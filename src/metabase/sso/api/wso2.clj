(ns metabase.sso.api.wso2
  "API routes for WSO2 Identity Server SSO authentication."
  (:require
   [metabase.api.macros :as api.macros]
   [metabase.sso.integrations.wso2 :as wso2-integration]
   [metabase.util.log :as log]))

;; GET /auth/sso/wso2
;;
#_{:clj-kondo/ignore [:metabase/validate-defendpoint-has-response-schema]}
(api.macros/defendpoint :get "/"
  "Initiate WSO2 SSO flow."
  [_route-params _query-params _body request]
  (try
    (wso2-integration/sso-initiate request)
    (catch Throwable e
      (log/error e "Error initiating WSO2 SSO")
      (throw e))))

;; GET /auth/sso/wso2/callback
;;
#_{:clj-kondo/ignore [:metabase/validate-defendpoint-has-response-schema]}
(api.macros/defendpoint :get "/callback"
  "WSO2 OIDC callback."
  [_route-params _query-params _body request]
  (try
    (wso2-integration/sso-callback request)
    (catch Throwable e
      (log/error e "Error handling WSO2 callback")
      (throw e))))

(def ^{:arglists '([request respond raise])} routes
  "`/auth/sso/wso2` routes."
  (api.macros/ns-handler *ns*))
