# WSO2 Identity Server SSO Integration Design

**Date:** 2026-06-19
**Status:** Draft
**SSO Type:** OAuth2 / OpenID Connect (Authorization Code flow)
**Edition:** OSS
**Identity Provider:** WSO2 Identity Server

---

## Overview

Integrate WSO2 Identity Server as a new SSO provider in Metabase OSS, leveraging the existing OIDC base infrastructure. Users can sign in via WSO2 IS using the standard OAuth2 Authorization Code flow with OpenID Connect Discovery.

## Architecture

```
User clicks "Sign in with WSO2"
  → GET /auth/sso/wso2
  → Redirect to WSO2 IS /authorize endpoint
  → User authenticates on WSO2 IS
  → WSO2 IS redirects to Metabase /auth/sso/wso2/callback?code=...
  → Backend exchanges authorization code for tokens
  → Backend verifies ID token (JWK signature validation)
  → Backend extracts user info from ID token claims
  → Creates/updates Metabase user and session
  → Redirects user to home page
```

## Components

### 1. Backend Provider (`src/metabase/sso/providers/wso2.clj`) - NEW

Derives from `:provider/oidc` to reuse the existing OIDC infrastructure:
- OIDC Discovery (`.well-known/openid-configuration`)
- Authorization code → token exchange
- ID token verification (JWK lookup, signature check)
- State/nonce cookie management
- User data extraction from claims

Registration:
```clojure
(derive :provider/wso2 :metabase.auth-identity.provider/provider)
(derive :provider/wso2 :metabase.auth-identity.provider/create-user-if-not-exists)
(derive :provider/wso2 :provider/oidc)
```

The provider reuses `:provider/oidc` authenticate, passing WSO2-specific config built from settings.

### 2. Settings (`src/metabase/sso/settings.clj`) - MODIFY

| Setting Key | Type | Description |
|-------------|------|-------------|
| `wso2-issuer-uri` | string | WSO2 IS issuer URL (e.g., `https://idp.example.com:9443/oauth2/oidcdiscovery`) |
| `wso2-client-id` | string | OAuth2 client ID registered in WSO2 IS |
| `wso2-client-secret` | string | OAuth2 client secret (stored encrypted) |
| `wso2-enabled` | boolean | Enable/disable WSO2 SSO |
| `wso2-configured` | boolean (computed) | True when issuer-uri, client-id, and client-secret are all set |
| `wso2-attribute-email` | string (optional) | ID token claim for email (default: `email`) |
| `wso2-attribute-firstname` | string (optional) | ID token claim for first name (default: `given_name`) |
| `wso2-attribute-lastname` | string (optional) | ID token claim for last name (default: `family_name`) |

### 3. API Routes

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/auth/sso/wso2` | Initiate OAuth2 flow; redirect to WSO2 IS authorize endpoint |
| GET | `/auth/sso/wso2/callback` | OAuth2 callback; exchange code, verify tokens, create session |

Routes are added to the SSO routing namespace. They follow the same pattern as existing OIDC routes in the enterprise edition.

### 4. Frontend

#### Login Button (`frontend/src/metabase/auth/components/Wso2Button/`) - NEW

- Shown on login page when `wso2-enabled` is true
- Follows existing pattern of external auth buttons (Google, SSO)
- Click initiates redirect to `/auth/sso/wso2`

#### Admin Settings Form (`frontend/src/metabase/admin/settings/auth/components/Wso2AuthForm/`) - NEW

Form fields:
- Issuer URI (text input)
- Client ID (text input)
- Client Secret (password input)
- Email Attribute (text input, default: email)
- First Name Attribute (text input, default: given_name)
- Last Name Attribute (text input, default: family_name)
- Enabled (toggle)

#### Plugin Registration (`frontend/src/metabase/plugins/oss/auth.ts`) - MODIFY

Register WSO2 as an external auth provider in `PLUGIN_AUTH_PROVIDERS`.

#### Types (`frontend/src/metabase-types/api/settings.ts`) - MODIFY

Add TypeScript types for all new WSO2 settings.

#### Routes (`frontend/src/metabase/admin/settingsRoutes.tsx`) - MODIFY

Add route `/admin/settings/authentication/wso2` pointing to the settings form.

### 5. SSO Init (`src/metabase/sso/init.clj`) - MODIFY

Require the new WSO2 provider namespace so the derive macros run at boot time.

## Files Summary

| Action | File |
|--------|------|
| CREATE | `src/metabase/sso/providers/wso2.clj` |
| CREATE | `frontend/src/metabase/auth/components/Wso2Button/Wso2Button.tsx` |
| CREATE | `frontend/src/metabase/admin/settings/auth/components/Wso2AuthForm/Wso2AuthForm.tsx` |
| MODIFY | `src/metabase/sso/settings.clj` |
| MODIFY | `src/metabase/sso/init.clj` |
| MODIFY | `src/metabase/sso/api/routes.clj` |
| MODIFY | `frontend/src/metabase/plugins/oss/auth.ts` |
| MODIFY | `frontend/src/metabase-types/api/settings.ts` |
| MODIFY | `frontend/src/metabase/admin/settingsRoutes.tsx` |

## Error Handling

- Discovery failure: show error to admin during configuration save
- Token exchange failure: redirect to login with error message
- ID token verification failure: redirect to login with error message
- User not found and provisioning disabled: error message on login page
- Network errors during OIDC flow: redirect to login with error message

## Testing

- Unit tests for the provider namespace in `test/metabase/sso/providers/wso2_test.clj`
- Test OIDC discovery with mock HTTP responses
- Test token exchange and verification
- Test user creation and update flows
- Frontend component tests for Wso2Button and Wso2AuthForm
