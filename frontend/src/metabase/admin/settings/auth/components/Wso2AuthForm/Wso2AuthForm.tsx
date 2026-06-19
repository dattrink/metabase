import { useMemo } from "react";
import { jt, t } from "ttag";

import {
  SettingsPageWrapper,
  SettingsSection,
} from "metabase/admin/components/SettingsSection";
import {
  useGetAdminSettingsDetailsQuery,
  useGetSettingsQuery,
  useUpdateWso2AuthMutation,
} from "metabase/api";
import { useSetting } from "metabase/common/hooks";
import {
  Form,
  FormErrorMessage,
  FormProvider,
  FormSubmitButton,
  FormTextInput,
} from "metabase/forms";
import { Flex, Stack, Text, Title } from "metabase/ui";
import type { SettingDefinition, Settings } from "metabase-types/api";

type Wso2AuthSettings = Pick<
  Settings,
  | "wso2-issuer-uri"
  | "wso2-client-id"
  | "wso2-client-secret"
  | "wso2-attribute-email"
  | "wso2-attribute-firstname"
  | "wso2-attribute-lastname"
>;

export const Wso2AuthForm = (): JSX.Element => {
  const { data: settingValues } = useGetSettingsQuery();
  const { data: settingDetails } = useGetAdminSettingsDetailsQuery();
  const [updateWso2AuthSettings] = useUpdateWso2AuthMutation();

  const initialValues = useMemo(() => {
    return {
      "wso2-issuer-uri": settingValues?.["wso2-issuer-uri"] ?? "",
      "wso2-client-id": settingValues?.["wso2-client-id"] ?? "",
      "wso2-client-secret": "",
      "wso2-attribute-email":
        settingValues?.["wso2-attribute-email"] ?? "email",
      "wso2-attribute-firstname":
        settingValues?.["wso2-attribute-firstname"] ?? "given_name",
      "wso2-attribute-lastname":
        settingValues?.["wso2-attribute-lastname"] ?? "family_name",
    };
  }, [settingValues]);

  const isWso2AuthEnabled = useSetting("wso2-enabled");

  const onSubmit = (values: Wso2AuthSettings) => {
    const payload = {
      ...values,
      "wso2-enabled": true,
    };
    return updateWso2AuthSettings(payload).unwrap();
  };

  return (
    <SettingsPageWrapper title={t`WSO2 auth`}>
      <SettingsSection>
        <FormProvider
          initialValues={initialValues}
          enableReinitialize
          onSubmit={onSubmit}
        >
          {({ dirty }) => (
            <Form disabled={!dirty}>
              <Stack gap="md">
                <Title order={2}>{t`Sign in with WSO2`}</Title>
                <Text c="text-secondary">
                  {t`Allows users with existing Metabase accounts to login with a WSO2 Identity Server account that matches their email address.`}
                </Text>
                <Text c="text-secondary">
                  {jt`Configure your WSO2 Identity Server application settings below.`}
                </Text>
                <FormTextInput
                  name="wso2-issuer-uri"
                  label={t`Issuer URI`}
                  placeholder="https://idp.example.com:9443/oauth2/oidcdiscovery"
                  {...getEnvSettingProps(
                    settingDetails?.["wso2-issuer-uri"],
                  )}
                />
                <FormTextInput
                  name="wso2-client-id"
                  label={t`Client ID`}
                  placeholder={t`Your WSO2 OAuth2 client ID`}
                  {...getEnvSettingProps(
                    settingDetails?.["wso2-client-id"],
                  )}
                />
                <FormTextInput
                  name="wso2-client-secret"
                  label={t`Client Secret`}
                  type="password"
                  placeholder={t`Your WSO2 OAuth2 client secret`}
                  {...getEnvSettingProps(
                    settingDetails?.["wso2-client-secret"],
                  )}
                />
                <FormTextInput
                  name="wso2-attribute-email"
                  label={t`Email Attribute`}
                  description={t`OIDC claim to use for the user's email address.`}
                  {...getEnvSettingProps(
                    settingDetails?.["wso2-attribute-email"],
                  )}
                />
                <FormTextInput
                  name="wso2-attribute-firstname"
                  label={t`First Name Attribute`}
                  {...getEnvSettingProps(
                    settingDetails?.["wso2-attribute-firstname"],
                  )}
                />
                <FormTextInput
                  name="wso2-attribute-lastname"
                  label={t`Last Name Attribute`}
                  {...getEnvSettingProps(
                    settingDetails?.["wso2-attribute-lastname"],
                  )}
                />
                <Flex justify="end">
                  <FormSubmitButton
                    label={
                      isWso2AuthEnabled
                        ? t`Save changes`
                        : t`Save and enable`
                    }
                    variant="filled"
                    disabled={!dirty}
                  />
                </Flex>
                <FormErrorMessage />
              </Stack>
            </Form>
          )}
        </FormProvider>
      </SettingsSection>
    </SettingsPageWrapper>
  );
};

const getEnvSettingProps = (setting?: SettingDefinition) => {
  if (setting?.is_env_setting) {
    return {
      description: t`Using ${setting.env_name}`,
      readOnly: true,
    };
  }
  return {};
};
