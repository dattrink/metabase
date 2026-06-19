(ns metabase.sso.providers.wso2
  "WSO2 Identity Server OIDC authentication provider.
   Derives from the base OIDC provider and adds WSO2-specific
   configuration from Metabase settings."
  (:require
   [metabase.auth-identity.core :as auth-identity]
   [metabase.sso.settings :as sso-settings]
   [methodical.core :as methodical]))

(set! *warn-on-reflection* true)

;;; -------------------------------------------------- Provider Registration --------------------------------------------------

(derive :provider/wso2 :provider/oidc)

;;; -------------------------------------------------- Configuration --------------------------------------------------

(defn- build-wso2-oidc-config
  "Build OIDC configuration map for the WSO2 provider."
  [request]
  (when (and (sso-settings/wso2-client-id)
             (sso-settings/wso2-client-secret)
             (sso-settings/wso2-issuer-uri))
    {:client-id (sso-settings/wso2-client-id)
     :client-secret (sso-settings/unobfuscated-wso2-client-secret)
     :issuer-uri (sso-settings/wso2-issuer-uri)
     :attribute-email (sso-settings/wso2-attribute-email)
     :attribute-firstname (sso-settings/wso2-attribute-firstname)
     :attribute-lastname (sso-settings/wso2-attribute-lastname)
     :scopes ["openid" "email" "profile"]
     :redirect-uri (:redirect-uri request)}))

;;; -------------------------------------------------- Authentication --------------------------------------------------

(methodical/defmethod auth-identity/authenticate :provider/wso2
  [_provider request]
  (cond
    (not (sso-settings/wso2-enabled))
    {:success? false
     :error :wso2-not-enabled
     :message "WSO2 authentication is not enabled"}

    (not (sso-settings/wso2-configured))
    {:success? false
     :error :wso2-not-configured
     :message "WSO2 Identity Server is not configured"}

    :else
    (let [oidc-config (build-wso2-oidc-config request)]
      (if-not oidc-config
        {:success? false
         :error :configuration-error
         :message "Failed to build WSO2 OIDC configuration"}
        (let [auth-result (next-method _provider (assoc request :oidc-config oidc-config))]
          (if (and (:success? auth-result)
                   (:user-data auth-result))
            (assoc-in auth-result [:user-data :sso_source] :wso2)
            auth-result))))))

;;; -------------------------------------------------- Login --------------------------------------------------

(methodical/defmethod auth-identity/login! :provider/wso2
  [provider request]
  (next-method provider request))
