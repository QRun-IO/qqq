# Optional native provider logout

The additive UI API 1.10 capability is disabled by default. It implements OIDC
RP-Initiated Logout for an explicitly authenticated native cookie session. The host
and client decide when to invoke it; ordinary QQQ logout does not sign out the
company identity provider.

Configure valid `nativeSessionLogin` with `requireNonceValidation: true`, then `OAuth2NativeSessionLogoutMetaData` with
exact `postLogoutRedirectUris`. Register those URIs with the provider **for logout**.
Give the OAuth2 provider an explicit nonblank metadata name; that name is part of
the encrypted session binding and capability eligibility.
Reusing the Android login callback does not imply provider logout registration.

For Authentik, choose the provider's invalidation flow to match the intended
behavior. Its default `default-provider-invalidation-flow` preserves the main
Authentik SSO session. Explicit company sign-out requires an invalidation flow
containing the User Logout stage, such as the built-in `default-invalidation-flow`.
This can also sign out other applications; ordinary QQQ local logout remains
separate. See [Authentik's provider logout settings](https://docs.goauthentik.io/add-secure-apps/providers/single-logout/).
Do not treat a successful RP logout callback as proof that the company session
ended: verify fresh credentials and the next operator's principal in the same
retained browser context.

Configure the existing refresh-token encryption key/key IDs for private session
material; a refresh token, refresh grant or refresh-token table is not required.

Before opting in, add a nullable `nativeLogoutContext` column to the host's physical
`userSession` table. It stores an encrypted envelope and needs at least 131072
characters (for example CLOB on H2, TEXT on PostgreSQL, MEDIUMTEXT on MySQL).
Apply this additive migration through the host's existing migration system using its
actual table/column name mapping. Then use
`new UserSessionMetaDataProducer(backend).withNativeLogoutContext().produce(instance)`.
The default producer does not add the field, preserving existing unmigrated hosts.
The feature is advertised only when metadata, explicit storage and usable encryption
key are present. Existing sessions without retained material still log out locally.

Only a successful strict native code exchange retains the original validated ID token.
Legacy native login with nonce validation disabled remains available but never
advertises this logout capability or retains a logout hint.
The AES-GCM envelope binds provider, session and stored user, and contains the
configured issuer/client/callback. Its AAD purpose differs from refresh tokens.
The table remains system-only, including for administrators. This field never goes
into QSession, profile values or session-store payloads.

The public `values.nativeSessionLogout` descriptor contains only
`flow: OIDC_RP_INITIATED_LOGOUT` and `postLogoutRedirectUris`. Clients must require
both the descriptor and the feature marker, then send:

```json
{"nativeLogout":{"postLogoutRedirectUri":"<exact advertised callback>"}}
```

The existing POST `/qqq/v1/logout` may return `nativeLogoutData` with
`issuer`, `endSessionEndpoint`, `postLogoutRedirectUri` and sensitive
`idTokenHint`. The response is no-store. The current cookie is authenticated and
matched to private retained material; bearer/mixed/body identities cannot retrieve
it. Discovery uses the explicit public HTTPS policy, checks issuer and endpoint,
and does not follow redirects. An unavailable hint/provider or rejected optional
request yields local-only logout, never a claim that company sign-out completed.

Session deletion and cache invalidation are independent of provider revocation or
discovery failures. Database deletion failure is an error, never a hint-bearing
success; cookies are expired either way. Cookie resume and logout share the existing
local refresh stripe lock to prevent a late resume cache write undoing logout.
Cached cookie resume also reads the authoritative session row and validates its
identity and access-token lifetime before accepting a cached principal. Cookie
resume and validity checks do not use access-token memoization. A stale cache write
after another node deletes the row therefore cannot recreate usable authentication.

Use AppAuth's end-session request and generated state. Compare response issuer,
endpoint and callback against bound metadata/discovery; keep login and logout
results distinct. Cancelled/failed provider sign-out never restores a QQQ session.
Do not log or deliberately persist tokens, complete URLs, SDK requests or responses.
The browser query and AppAuth's internal Intent/activity state carry the hint; this
is an explicit sensitive boundary, not a promise of zero persistence.

Implementation must pass focused core/HTTP/schema tests and real provider
A→logout→B acceptance before adoption. No global runtime, provider policy or
deployment changes are implied by this source addition.

The same API 1.10 adds optional strict native login validation. Enable
`nativeSessionLogin.requireNonceValidation` to advertise the conditional feature
`nativeSessionNonceValidation` and `values.nativeSessionLogin.nonceValidation:
REQUIRED`. Updated native clients require both and send the nonce retained from
their original authorization request as top-level `expectedNonce` (16–512 URL-safe
characters). They must not take the expected nonce from a callback.

Before host customization or session storage, the server checks the exact public
issuer, singleton exchange-client audience, optional matching authorized party,
expiration/issue time with 60-second clock skew, nonce and nonblank subject. The
ID-token subject must equal the access JWT subject used by QQQ; that access token
must also carry a valid expiration and any supplied not-before/issue time. Only signed-form
RS256 ID tokens returned directly by the normally trusted HTTPS token endpoint are
accepted; OIDC Core 3.1.3.7 item 6 permits this direct TLS response as the validation
trust boundary. This is not verification of an arbitrary client-supplied token.
Discovery and token transport reject redirects, use bounded timeouts and byte
limits, and retain normal certificate and hostname checks. A customizer that fetches
userinfo must independently require its `sub` to match this validated subject.
Disabled policy preserves legacy native/web behavior; strict policy never falls
back to claims decoding after validation failure.
