(ns metabase.sso.providers.wso2-test
  (:require
   [clojure.test :refer :all]
   [metabase.auth-identity.core :as auth-identity]
   [metabase.sso.settings :as sso-settings]
   [metabase.test :as mt]
   [metabase.test.fixtures :as fixtures]))

(set! *warn-on-reflection* true)

(use-fixtures :once (fixtures/initialize :db))

(use-fixtures :each (fn [thunk]
                      (mt/with-temporary-setting-values [wso2-enabled false
                                                          wso2-client-id nil
                                                          wso2-client-secret nil
                                                          wso2-issuer-uri nil]
                        (thunk))))

(deftest ^:parallel authenticate-when-not-enabled-test
  (testing "Returns error when WSO2 is not enabled"
    (mt/with-temporary-setting-values [wso2-enabled false]
      (let [result (auth-identity/authenticate :provider/wso2 {})]
        (is (false? (:success? result)))
        (is (= :wso2-not-enabled (:error result)))
        (is (some? (:message result)))))))

(deftest ^:parallel authenticate-when-not-configured-test
  (testing "Returns error when WSO2 is not configured"
    (mt/with-temporary-setting-values [wso2-enabled true
                                        wso2-client-id nil]
      (let [result (auth-identity/authenticate :provider/wso2 {})]
        (is (false? (:success? result)))
        (is (= :wso2-not-configured (:error result)))
        (is (some? (:message result)))))))

(deftest ^:parallel authenticate-initiates-redirect-test
  (testing "Initiates OIDC redirect when configured and enabled"
    (mt/with-temporary-setting-values [wso2-enabled true
                                        wso2-client-id "test-client-id"
                                        wso2-client-secret "test-secret"
                                        wso2-issuer-uri "https://accounts.google.com"]
      (let [result (auth-identity/authenticate :provider/wso2
                                               {:redirect-uri "https://metabase.example.com/auth/sso/wso2/callback"})]
        (is (= :redirect (:success? result)))
        (is (string? (:redirect-url result)))
        (is (string? (:state result)))
        (is (string? (:nonce result)))))))
