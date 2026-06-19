import type { Settings } from "metabase-types/api";

import { Api } from "./api";
import { invalidateTags, tag } from "./tags";

type Wso2AuthSettings = Pick<
  Settings,
  | "wso2-enabled"
  | "wso2-issuer-uri"
  | "wso2-client-id"
  | "wso2-client-secret"
  | "wso2-attribute-email"
  | "wso2-attribute-firstname"
  | "wso2-attribute-lastname"
>;

export const wso2Api = Api.injectEndpoints({
  endpoints: (builder) => ({
    updateWso2Auth: builder.mutation<void, Wso2AuthSettings>({
      query: (settings) => ({
        method: "PUT",
        url: `/api/wso2/settings`,
        body: settings,
      }),
      invalidatesTags: (_, error) =>
        invalidateTags(error, [tag("session-properties")]),
    }),
  }),
});

export const { useUpdateWso2AuthMutation } = wso2Api;
