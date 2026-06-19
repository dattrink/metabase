(ns metabase.api.wso2
  "/api/wso2 endpoints"
  (:require
   [metabase.api.common :as api]
   [metabase.api.macros :as api.macros]
   [metabase.settings.core :as setting]
   [metabase.sso.settings :as sso-settings]))

#_{:clj-kondo/ignore [:metabase/validate-defendpoint-has-response-schema]}
(api.macros/defendpoint :put "/settings"
  "Update WSO2 Identity Server SSO settings. You must be a superuser to do this."
  [_route-params
   _query-params
   {:keys [wso2-client-id wso2-client-secret wso2-issuer-uri
           wso2-attribute-email wso2-attribute-firstname wso2-attribute-lastname
           wso2-enabled]
    :as body}
   :- [:map
       [:wso2-client-id {:optional true} [:maybe :string]]
       [:wso2-client-secret {:optional true} [:maybe :string]]
       [:wso2-issuer-uri {:optional true} [:maybe :string]]
       [:wso2-attribute-email {:optional true} [:maybe :string]]
       [:wso2-attribute-firstname {:optional true} [:maybe :string]]
       [:wso2-attribute-lastname {:optional true} [:maybe :string]]
       [:wso2-enabled {:optional true} [:maybe :boolean]]]]
  (api/check-superuser)
  (setting/set-many! body)
  (when (contains? body :wso2-enabled)
    (sso-settings/wso2-enabled! (:wso2-enabled body))))

(def ^{:arglists '([request respond raise])} routes
  "`/api/wso2/` routes."
  (api.macros/ns-handler *ns*))
