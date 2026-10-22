# Operation-specific credential scopes

`QCredentialScope.operationPathParameters` adds an HTTP boundary allowlist. Keys are canonical UI operation names (`METHOD /template`, relative to `/qqq/v1`), and values map path-parameter names to allowed decoded values. These restrictions are additional to `operationIds`, ordinary user permissions, and existing global object scopes; they never grant access.

```java
session.withCredentialScope(new QCredentialScope()
   .withCredentialId("publisher-token")
   .withOperationPathParameters(Map.of(
      "POST /table/{tableName}/query", Map.of("tableName", Set.of("article", "author")),
      "POST /processes/{processName}/init", Map.of("processName", Set.of("PublishArticle")),
      "GET /processes/{processName}/{processUUID}/status/{jobUUID}", Map.of("processName", Set.of("PublishArticle"))
   )));
```

A null map preserves existing behavior. An empty map denies every operation. A non-null map denies operations absent from it. An empty parameter map permits that operation without additional object limits. Missing parameters and values outside a parameter's set fail closed. Keys use the canonical route, not an OpenAPI operation ID alias. Collections are copied defensively and exposed as immutable values; session cloning preserves independent scopes.

The operation/object check runs before the executor. It does not run inside backend actions invoked by an authorized process: that process still needs its ordinary table permissions, and any existing global `tables`, `processes`, or `readOnly` restrictions still apply. To allow publishing while denying direct writes, restrict the HTTP operations; do not mark that credential globally read-only or restrict its global tables to only the queryable tables. User permission revocation still denies access.

Attach scopes after verifying the credential, using the existing authentication metadata `customizer` (`QAuthenticationModuleCustomizerInterface.customizeSession`). OAuth supplies verified claims in `jwtPayloadJsonObject`; use the credential type or trusted claim mapping there. Do not derive grants from request path values or unverified JWT payloads. No new authentication-provider subclass is required just to attach this policy.

Requests with an `Authorization: Bearer` header and either `sessionId` or `sessionUUID` cookies return 403 before authentication selection. This policy covers HTTP authorization headers; the existing legacy opaque form-authorization behavior is unchanged. Scheme matching is case-insensitive and covers opaque tokens. Bearer authentication does not set or clear session cookies, including authentication failures and custom authentication modules. Session-creation endpoints refuse bearer headers with 401 and cannot convert operation-scoped sessions into browser credentials.

Legacy and application API handlers that have no canonical UI operation authorization reject credentials with non-null `operationIds` or `operationPathParameters`. Object-only scopes retain existing permission checks. Independently authenticated APIs continue using their own authentication metadata; there is no global ban on unrelated custom routes. Applications must continue protecting custom routes that bypass QQQ session setup. In particular, do not remove an application-wide publisher guard until all routes it protects use an equivalent declared boundary.

Custom endpoint specs overriding `serveRequest` must use `checkCredentialScopeOperation(context)` after setting up authentication and before executing actions. The legacy no-argument overload remains available, but deliberately denies new path-restricted scopes because it cannot inspect their route parameters.

This change builds on #1268's explicit application-instance authentication fix. Deploy that fix with this version; no production deployment or backend publisher-filter removal is included here.
