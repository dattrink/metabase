(ns metabase.sso.integrations.wso2-test
  (:require
   [clojure.test :refer :all]
   [metabase.sso.integrations.wso2 :as wso2-integration]
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

(deftest ^:parallel sso-initiate-when-not-enabled-test
  (testing "Throws when WSO2 is not enabled"
    (is (thrown? clojure.lang.ExceptionInfo
                 (wso2-integration/sso-initiate {:params {}})))))

(deftest ^:parallel sso-callback-when-not-enabled-test
  (testing "Throws when WSO2 is not enabled"
    (is (thrown? clojure.lang.ExceptionInfo
                 (wso2-integration/sso-callback {:params {}})))))
