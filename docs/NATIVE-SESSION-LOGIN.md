# Native OAuth2 cookie-session login

UI API 1.9.0 supports an explicit browser-based OAuth2 login that creates the same
QQQ session used by the web client. It is optional and applies to `OAUTH2` metadata.
TABLE_BASED, Auth0, existing web login, bearer login and session resume retain their
existing contracts.

## Configuration

The host configures `OAuth2AuthenticationMetaData.nativeSessionLogin` with
`OAuth2NativeSessionLoginMetaData`:

```java
oauth2.withNativeSessionLogin(new OAuth2NativeSessionLoginMetaData()
   .withIssuer("https://login.example.test/application/o/warehouse/")
   .withDiscoveryUrl("https://login.example.test/application/o/warehouse/.well-known/openid-configuration")
   .withRedirectUris(List.of("com.example.warehouse:/oauth2/callback")));
```

These values are public. Issuer and discovery URL must be explicit HTTPS URLs,
without credentials, query or fragment. The issuer is not inferred from `baseUrl`:
providers such as Authentik can use different issuer paths, and backend discovery
may use an internal URL. Operators must provide browser-reachable public URLs that
describe the same provider/client used by the backend.

Callbacks are exact HTTPS app links or private-use reverse-domain schemes, with a
path and without credentials, wildcards, query or fragment. Register the identical
callback with the provider for the existing `clientId`. The server never guesses
an application package, domain or callback. The client secret remains server-side.
The separate `nativeClientId`/bearer configuration does not enable this session flow.
Configure that provider client to require S256 PKCE. QQQ can validate the verifier
syntax before exchange; the provider owns the code-to-challenge binding and the
challenge method used by the original authorization request.

## Discovery and exchange

Only a valid configured policy advertises `nativeSessionLogin` in `GET /version`
and the following typed object in `GET /metaData/authentication`:

```json
{
  "flow": "AUTHORIZATION_CODE_PKCE_SESSION",
  "codeChallengeMethod": "S256",
  "prompt": "login",
  "issuer": "https://login.example.test/application/o/warehouse/",
  "discoveryUrl": "https://login.example.test/application/o/warehouse/.well-known/openid-configuration",
  "redirectUris": ["com.example.warehouse:/oauth2/callback"]
}
```

1. Require API >=1.9 <2, the feature, this descriptor, and the known flow/method/prompt.
   Use the outer `values.clientId` and space-separated `values.scopes`.
2. Fetch the explicit discovery document over HTTPS and require its issuer to
   equal the descriptor issuer exactly. Validate public HTTPS authorization
   endpoints. Select only an allowed callback actually owned by this installed app.
3. Open the system browser with `prompt=login`, a fresh cryptographically random state, a fresh
   RFC7636 verifier and its S256 challenge. Preserve the pending request safely
   across lifecycle changes; reject stale, duplicate or mismatched callbacks.
4. After validating state and the exact callback, send the following fields to
   the same QQQ server's existing `POST /qqq/v1/manageSession`:

```json
{
  "authenticationFlow": "AUTHORIZATION_CODE_PKCE_SESSION",
  "code": "<authorization code>",
  "codeVerifier": "<43-128 ASCII unreserved characters>",
  "redirectUri": "com.example.warehouse:/oauth2/callback"
}
```

The native marker requires configured metadata, a nonempty code, a verifier of
43–128 ASCII letters/digits/`-._~`, and an exact listed callback. Unknown markers,
unconfigured flow, wrong authentication types and mixed `accessToken`, `sessionUUID`,
`uuid` or `Authorization` are rejected with `400` before invoking the authentication
provider. A stale cookie may accompany a fresh code exchange; it is not used to
resume that request. Provider exchange still validates the authorization code,
callback and PKCE binding. Invalid provider credentials follow existing `401` behavior.

Keep the resulting QQQ session cookie scoped to that server, then load metadata
through the ordinary semantic API. Do not log the code, verifier, state, cookie,
provider tokens or server secret. No new login endpoint or token-storage protocol
is introduced. This contract does not add ID-token nonce validation; clients must
not claim that capability.

The fixed prompt requests fresh authentication so a shared browser session does not
silently select the previous handheld operator. It does not perform provider-global
SSO logout or establish a cryptographic `auth_time` guarantee. Deployment acceptance
must verify that the configured provider flow lets the next operator identify themselves.

Marker-free requests preserve the existing web protocol. The explicit marker is
a protocol selection and validation boundary, not a claim that legacy web clients
have been migrated to the native callback policy.

## Verification scope

`NativeSessionLoginSpecV1Test` exercises real HTTP version/authentication metadata,
typed descriptor shape and session requests with a scoped counting provider double.
Invalid inputs must never reach that provider. It also verifies a valid native
request and unchanged marker-free web forwarding. The provider double is test-only;
it does not establish a real identity-provider/browser integration. Existing
OAuth2 provider, web/session, TABLE_BASED and bearer tests remain required.

See [Authentication session logging](SESSION-LOGGING.md) for credential-safe diagnostic fields and the session correlation format.
