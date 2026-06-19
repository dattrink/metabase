(ns metabase.sso.integrations.wso2
  "Implementation of the WSO2 Identity Server backend for SSO.
   Uses OIDC (OpenID Connect) for authentication.

   Flow:
   1. User accesses GET /auth/sso/wso2
   2. Metabase redirects to WSO2 authorization endpoint
   3. User authenticates with WSO2
   4. WSO2 redirects back to GET /auth/sso/wso2/callback?code=...&state=...
   5. Metabase exchanges code for tokens and creates session"
  (:require
   [java-time.api :as t]
   [metabase.api.common :as api]
   [metabase.auth-identity.core :as auth-identity]
   [metabase.request.core :as request]
   [metabase.sso.core :as sso]
   [metabase.sso.settings :as sso-settings]
   [metabase.system.core :as system]
   [metabase.util.i18n :refer [tru]]
   [metabase.util.log :as log]
   [ring.util.response :as response]))

(set! *warn-on-reflection* true)

(defn- wso2-redirect-uri
  "Generate the redirect URI for WSO2 OIDC callback."
  []
  (str (system/site-url) "/auth/sso/wso2/callback"))

(defn- check-wso2-prereqs!
  "Check that WSO2 is available and enabled. Throws on failure."
  []
  (when-not (sso-settings/wso2-enabled)
    (throw (ex-info (tru "WSO2 SSO is not enabled")
                    {:status-code 400}))))

(defn sso-initiate
  "Initiate WSO2 SSO flow. Redirects to WSO2 authorization endpoint."
  [request]
  (check-wso2-prereqs!)
  (let [{:keys [redirect]} (:params request)
        redirect-url (if redirect
                       (try
                         (let [uri (java.net.URI. redirect)]
                           (when-let [host (.getHost uri)]
                             (when-let [our-host (some-> (system/site-url) java.net.URI. .getHost)]
                               (when-not (= host our-host)
                                 (throw (ex-info (tru "Invalid redirect URL")
                                                 {:status-code 400})))))
                           redirect)
                         (catch clojure.lang.ExceptionInfo e (throw e))
                         (catch Exception _
                           (throw (ex-info (tru "Invalid redirect URL")
                                           {:status-code 400}))))
                       "/")
        auth-result (auth-identity/authenticate :provider/wso2
                                                (assoc request
                                                       :redirect-uri (wso2-redirect-uri)
                                                       :final-redirect redirect-url))]
    (if (= :redirect (:success? auth-result))
      (sso/wrap-oidc-redirect auth-result
                              request
                              :wso2
                              redirect-url
                              {:browser-id (:browser-id request)})
      (throw (ex-info (or (:message auth-result) (tru "Failed to initiate WSO2 authentication"))
                      {:status-code 500})))))

(defn sso-callback
  "Handle WSO2 OIDC callback with authorization code."
  [request]
  (check-wso2-prereqs!)
  (let [{:keys [code state]} (:params request)
        login-result (auth-identity/login! :provider/wso2
                                           (assoc request
                                                  :code code
                                                  :state state
                                                  :oidc-provider :wso2
                                                  :redirect-uri (wso2-redirect-uri)
                                                  :device-info (request/device-info request)))]
    (if (:success? login-result)
      (let [final-redirect (or (:redirect-url login-result) "/")
            base-response (-> (response/redirect final-redirect)
                              (sso/clear-oidc-state-cookie))]
        (log/infof "WSO2 authentication successful for user %s"
                   (get-in login-result [:user-data :email]))
        (if-let [session (:session login-result)]
          (request/set-session-cookies request
                                       base-response
                                       session
                                       (t/zoned-date-time (t/zone-id "GMT")))
          base-response))
      (let [error-msg (or (:message login-result) (tru "WSO2 authentication failed"))]
        (log/errorf "WSO2 authentication failed: %s" error-msg)
        (throw (ex-info error-msg {:status-code 401}))))))
