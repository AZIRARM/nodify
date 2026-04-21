# Authentication Guide - Internal / OAuth2 / OpenID Connect

## Overview

The system supports three authentication modes:
- **Internal**: Built-in email/password authentication
- **OAuth2**: OAuth2 protocol authentication (e.g., Keycloak)
- **OpenID Connect**: OpenID Connect protocol authentication (e.g., Keycloak)

## Configuration

### 1. Internal Mode (Default)

```properties
AUTH_MODE=internal
```

No additional configuration required.

### 2. OAuth2 Mode

```properties
AUTH_MODE=oauth2

# OAuth2
OAUTH2_ENABLED=true
OAUTH2_AUTHORIZATION_URI=<your_authorization_url>
OAUTH2_TOKEN_URI=<your_token_url>
OAUTH2_USER_INFO_URI=<your_userinfo_url>
OAUTH2_CLIENT_ID=<your_client_id>
OAUTH2_CLIENT_SECRET=<your_client_secret>
OAUTH2_SCOPE=openid profile email
OAUTH2_ROLES_CLAIM=roles
OAUTH2_USER_NAME_CLAIM=preferred_username
OAUTH2_REDIRECT_URI=http://localhost:8080/authentication/oauth2/callback
OAUTH2_FRONTEND_TARGET_URL=http://localhost:4200/oauth2/callback
```

### 3. OpenID Connect Mode

```properties
AUTH_MODE=openid

# OpenID
OPENID_ENABLED=true
OPENID_ISSUER_URI=<your_issuer_url>
OPENID_CLIENT_ID=<your_client_id>
OPENID_CLIENT_SECRET=<your_client_secret>
OPENID_JWK_SET_URI=<your_jwks_url>
OPENID_USER_INFO_URI=<your_userinfo_url>
OPENID_AUTHORIZATION_URI=<your_authorization_url>
OPENID_TOKEN_URI=<your_token_url>
OPENID_LOGOUT_URI=<your_logout_url>
OPENID_ROLES_CLAIM=realm_access.roles
OPENID_USER_NAME_CLAIM=preferred_username
OPENID_REDIRECT_URI=http://localhost:8080/authentication/openid/callback
OPENID_FRONTEND_TARGET_URL=http://localhost:4200/openid/callback
```

## Environment Variables Summary

| Variable | Description | Required For |
|----------|-------------|--------------|
| `AUTH_MODE` | Authentication mode (`internal`, `oauth2`, `openid`) | All |
| `OAUTH2_ENABLED` | Enable OAuth2 | OAuth2 |
| `OAUTH2_AUTHORIZATION_URI` | OAuth2 authorization endpoint | OAuth2 |
| `OAUTH2_TOKEN_URI` | OAuth2 token endpoint | OAuth2 |
| `OAUTH2_USER_INFO_URI` | OAuth2 userinfo endpoint | OAuth2 |
| `OAUTH2_CLIENT_ID` | OAuth2 client identifier | OAuth2 |
| `OAUTH2_CLIENT_SECRET` | OAuth2 client secret | OAuth2 |
| `OAUTH2_SCOPE` | OAuth2 scopes (default: `openid profile email`) | OAuth2 |
| `OAUTH2_ROLES_CLAIM` | Claim name for roles (default: `roles`) | OAuth2 |
| `OAUTH2_USER_NAME_CLAIM` | Claim name for username (default: `preferred_username`) | OAuth2 |
| `OAUTH2_REDIRECT_URI` | Backend OAuth2 callback URI | OAuth2 |
| `OAUTH2_FRONTEND_TARGET_URL` | Frontend OAuth2 callback URI | OAuth2 |
| `OPENID_ENABLED` | Enable OpenID Connect | OpenID |
| `OPENID_ISSUER_URI` | OpenID issuer URI | OpenID |
| `OPENID_CLIENT_ID` | OpenID client identifier | OpenID |
| `OPENID_CLIENT_SECRET` | OpenID client secret | OpenID |
| `OPENID_JWK_SET_URI` | OpenID JWKS URI for token validation | OpenID |
| `OPENID_USER_INFO_URI` | OpenID userinfo endpoint | OpenID |
| `OPENID_AUTHORIZATION_URI` | OpenID authorization endpoint | OpenID |
| `OPENID_TOKEN_URI` | OpenID token endpoint | OpenID |
| `OPENID_LOGOUT_URI` | OpenID logout endpoint | OpenID |
| `OPENID_ROLES_CLAIM` | Claim path for roles (default: `realm_access.roles`) | OpenID |
| `OPENID_USER_NAME_CLAIM` | Claim name for username (default: `preferred_username`) | OpenID |
| `OPENID_REDIRECT_URI` | Backend OpenID callback URI | OpenID |
| `OPENID_FRONTEND_TARGET_URL` | Frontend OpenID callback URI | OpenID |

## Provider Configuration Example

### Keycloak Configuration

| Parameter | Keycloak Endpoint |
|-----------|-------------------|
| Authorization URI | `https://<server>/realms/<realm>/protocol/openid-connect/auth` |
| Token URI | `https://<server>/realms/<realm>/protocol/openid-connect/token` |
| UserInfo URI | `https://<server>/realms/<realm>/protocol/openid-connect/userinfo` |
| JWK Set URI | `https://<server>/realms/<realm>/protocol/openid-connect/certs` |
| Logout URI | `https://<server>/realms/<realm>/protocol/openid-connect/logout` |
| Issuer URI | `https://<server>/realms/<realm>` |

## Switching Between Modes

1. Update `AUTH_MODE` in your environment configuration
2. Set the corresponding `*_ENABLED` flag to `true`
3. Configure the required endpoints and credentials
4. Restart the application

## Frontend Behavior

- **Internal Mode**: Displays email/password login form
- **OAuth2 Mode**: Displays OAuth2 login button
- **OpenID Mode**: Displays OpenID Connect login button

## Notes

- The frontend automatically detects the active mode via the `/authentication/mode` endpoint
- Redirect URIs must match exactly between provider configuration and application settings
- For production, always use HTTPS endpoints