import { useCallback } from "react";
import { t } from "ttag";

import { useSetting } from "metabase/common/hooks";
import { AuthButton } from "../AuthButton";

interface Wso2ButtonProps {
  isCard?: boolean;
  redirectUrl?: string;
}

export const Wso2Button = ({ isCard, redirectUrl }: Wso2ButtonProps) => {
  const isEnabled = useSetting("wso2-enabled");

  const getUrl = useCallback(() => {
    const params = new URLSearchParams();
    if (redirectUrl) {
      params.set("redirect", redirectUrl);
    }
    const qs = params.toString();
    return `/auth/sso/wso2${qs ? `?${qs}` : ""}`;
  }, [redirectUrl]);

  if (!isEnabled) {
    return null;
  }

  return (
    <AuthButton link={getUrl()} isCard={isCard}>
      {t`Sign in with WSO2`}
    </AuthButton>
  );
};
