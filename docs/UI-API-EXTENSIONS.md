# UI API extension point (`/qqq/v1`)

Status: design accepted 2026-10-13 (owner decisions 2026-10-12: all recommendations accepted).
Applies from QQQ 4.1.0-RC.6 (UI API 1.1.0).

The UI/UX API is `/qqq/v1` (`qqq-middleware-javalin`, package
`com.kingsrook.qqq.middleware.javalin.specs.v1`). It is the only API that drives a UI: Next UI
today, and a CLI, TUI and native iOS client later. Every route under `/qqq/v1` must therefore be
described by an `AbstractEndpointSpec` so that it appears, with full request and response schemas,
in the OpenAPI document that a non-web client is generated from.

Before this change, the only way for a module or qbit to add a `/qqq/v1` route was a raw Javalin
handler in a `QJavalinRouteProviderInterface` (alerts, assistant, sql-workbench, ESB). Those routes
were invisible to the OpenAPI document. This document defines the replacement.

## 1. Registering specs: `MiddlewareSpecExtension`

A module or qbit declares an **extension**: a namespace plus the spec classes it serves.

```java
MiddlewareSpecExtensionsMetaData.ofOrWithNew(qInstance)
   .withExtension(new MiddlewareSpecExtension()
      .withName("alerts")                      // namespace; lowercase, a-z 0-9 and '-'
      .withSemanticVersion("1.0.0")            // the extension's own semver
      .withDescription("Alerts inbox and record panel")
      .withSpec(AlertsInboxSummarySpecV1.class)
      .withSpec(AlertsInboxSpecV1.class));
```

- Classes: `com.kingsrook.qqq.middleware.javalin.specs.extensions.MiddlewareSpecExtension` and
  `MiddlewareSpecExtensionsMetaData` (a `QSupplementalInstanceMetaData`, name
  `middlewareSpecExtensions`). Producers call this from the same place they register metadata
  today (qbit producer, or a `MetaDataProducer`).
- A spec is an ordinary `AbstractEndpointSpec<INPUT, OUTPUT, EXECUTOR>` with its own executor,
  input and output classes, exactly like the core specs. Its `defineBasicOperation().getPath()` is
  relative to the version base, e.g. `/qbit/alerts/inbox/summary`.
- Response and request body classes implement `ToSchema` and are published through
  `defineComponentSchemas()`. Component schema names must be prefixed with the extension's
  PascalCase name (`AlertsInboxResponseV1`) so they cannot collide with core schemas.
- `MiddlewareVersionV1` (any `AbstractMiddlewareVersion`) instantiates the extension specs from the
  QInstance when the server calls `setQInstance`, binds their routes after the core routes, and
  includes them in the OpenAPI it serves at `/qqq/v1/openapi.yaml` and `.json`.

### Writing a spec: `AbstractJsonEndpointSpec`
Most module and qbit endpoints take path/query parameters and an optional JSON object body, and
return JSON. `AbstractJsonEndpointSpec` (same package) does the plumbing. A subclass defines:

- `defineBasicOperation()` (path, method, summary), `defineRequestParameters()`, and
  `defineComponentSchemas()` (the response schema and any it references);
- `getResponseSchemaName()`, and `defineRequestBodySchema()` (or null for no body);
- `handle(JsonInput)`, which does the work with the user's session in `QContext` and returns the
  body. The body is serialized to JSON with nulls kept.

`JsonInput` gives `getPathParam`, `getQueryParam`, `getQueryParamInteger` (400 on bad input) and
`getBody()`. A body that is not JSON is rejected with 400. Endpoints with other shapes (files,
streams) extend `AbstractEndpointSpec` directly.

**Streaming (SSE).** Declare the 200 response with content type `text/event-stream`
(`BasicResponse(contentType, ...)`), with an event schema in `defineComponentSchemas()`, and write
the stream in `handleOutput`. Document each event type as a schema, so non-web clients can parse it.

**Non-UI routes.** Machine-to-machine endpoints, such as Alertmanager's webhook, are not part of
the UI API. Register them outside `/qqq/v1`, e.g. `/qqq/hooks/{qbit}/...`, through a route provider.

### Namespaces
- **Default: `/qqq/v1/qbit/{qbitName}/...`.** New qbit endpoints use this (the extension's
  `pathPrefix` defaults to `/qbit/{name}`).
- **Established module paths are kept stable.** An extension may declare an explicit `pathPrefix`
  when its URLs already ship to clients: `/esb` (qqq-esb), `/alerts` (qbit-alerts),
  `/assistant` (qbit-assistant) and `/sql-workbench` (qbit-sql-workbench). These are recorded in
  `MiddlewareSpecExtensionsMetaData.ESTABLISHED_PREFIXES`; any other explicit prefix fails
  validation. Moving those four to `/qbit/...` would be a breaking URL change and is deferred to
  `/qqq/v2`.
- Reserved by core (never available to extensions): `/metaData`, `/table`, `/processes`,
  `/possibleValues`, `/widget`, `/reports`, `/download`, `/manageSession`, `/logout`, `/oidc`,
  `/search`, `/version`, `/preferences`, `/openapi.*`.

### QInstance validation (fails the instance)
For each extension:
1. name is present and matches `[a-z0-9][a-z0-9-]*`;
2. semantic version is valid `MAJOR.MINOR.PATCH`;
3. path prefix is `/qbit/{name}` or listed in `ESTABLISHED_PREFIXES`; it does not overlap another
   extension's prefix or a core-reserved root;
4. every spec reference loads, extends `AbstractEndpointSpec`, has a public no-arg constructor, and
   its path starts with the prefix;
5. no two specs (core or extension) define the same method + path.

## 2. Raw routes under `/qqq/v1`: strict by default

Raw Javalin handlers are registered in code by route providers, so `QInstanceValidator` cannot see
them. The check therefore runs where the routes exist: when `QApplicationJavalinServer` has built
the Javalin service, `UiApiRouteAuditor` lists every HTTP endpoint (Javalin's internal router)
under each middleware version's base path and compares it with the spec'd operations plus the
documentation routes (`/qqq/v1/`, `/openapi.yaml|json|html`).

- **Default (strict): startup fails** when any route under a version's base path has no spec,
  naming each method and path and pointing to this document.
- **Opt out, temporarily** (`QApplicationJavalinServer.withStrictUiApiRoutes(false)`, or system
  property / env `qqq.uiApi.strictRoutes` / `QQQ_UI_API_STRICT_ROUTES=false`): one WARN per
  unspec'd route instead. Use it only while a qbit or module moves its routes onto specs.

Strict became the default in UI API 1.3.0 (QRun-IO/qqq#1159), once the alerts, assistant and
sql-workbench qbits moved their routes onto extension specs. Before that the default was to warn,
so that upgrading core could not break a deployment whose qbits still served raw routes. An
application on an older alerts, assistant or sql-workbench qbit release must upgrade it, or opt
out, when it upgrades core.

## 3. Versioning
- The URL major (`v1`) only changes for breaking changes (`/qqq/v2`).
- `info.version` in the OpenAPI document is semver `1.MINOR.PATCH` (`AbstractMiddlewareVersion
  .getSemanticVersion()`); rules in `UI-API-CHANGELOG.md`.
- `GET /qqq/v1/version` (unsecured) returns the API semver, base path, supported majors, core
  feature names and each registered extension with its own semver. Clients check compatibility
  from it before signing in.
- Each extension carries its own semver; a breaking change to an extension's operations also needs
  `/qqq/v2` because the URL space is shared.

## 4. Contract tests
- Core: `UiApiContractTest` regenerates the core document and fails if it differs from the
  committed `openapi/v1/openapi.yaml`, then compares the committed file with the release baseline
  `openapi/v1/baseline.yaml` using `OpenApiCompatibilityChecker`. Any breaking difference fails;
  any non-breaking difference requires `info.version` > baseline.
- Extensions: `UiApiContractAssertions.assertExtensionMatchesCommitted(extension, path)` lets a
  qbit commit its own fragment (`src/main/resources/openapi/qqq-v1/{name}.yaml`) and run the
  same two checks in its own build.

## 5. In-process invocation with the user's session (assistant)

The owner wants the AI assistant to read and write data only through `/qqq/v1`, with the signed-in
user's own session, so it is held to exactly the same security boundaries as the UI.

**Choice: loopback HTTP through the running server, not a re-implementation.** A spec's
`buildInput(Context)`, session setup (`ExecutorSessionUtils.setupSession`), the version's
`preExecute`, Javalin before/after filters, access logging, permission checks and error mapping
all hang off the Javalin request pipeline. Calling executors directly would skip some of them and
drift over time. Building a synthetic Javalin `Context` is internal API and fragile. A request to
the server's own port runs the identical pipeline by construction.

API (`com.kingsrook.qqq.middleware.javalin.specs.invoke`):

```java
UiApiInvoker invoker = UiApiInvoker.forServer(qApplicationJavalinServer); // or .forBaseUrl("http://127.0.0.1:8000")
UiApiResponse response = invoker.invoke(new UiApiRequest()
   .withMethod(HttpMethod.POST)
   .withPath("/table/{tableName}/query")          // the spec template, as in openapi.yaml
   .withPathParam("tableName", "person")
   .withQueryParam("limit", "20")
   .withJsonBody(Map.of("filter", filter))
   .withCredentials(callerCredentials)            // UiApiCredentials.fromContext(ctx) of the user's request; required
   .withInvokedBy("assistant"));                  // X-QQQ-Invoked-By, written to the access log
response.getStatus(); response.getJson(); response.getHeaders();
```

- **Only spec'd operations can be invoked.** The invoker resolves `method + path template`
  against the version's spec list (core plus extensions). Anything else is refused before any
  request is made, so the assistant cannot reach raw or legacy routes.
- **The session is the user's.** The invoker forwards the caller's credentials exactly as their
  request carried them (`UiApiCredentials.fromContext`: the sessionUUID/sessionId cookies, the
  Authorization header, x-api-key), so the call authenticates as the same user under any module
  (OAuth2/OIDC and Auth0 resume by sessionUUID, table-based by sessionId, API keys by header, MOCK
  by either). `withSessionUUID(uuid)` is the shorthand when only the session UUID is known. There
  is no service session and no way to elevate; a 401/403 comes back exactly as the UI would see it.
- **`listOperations()`** returns each invocable operation (method, path template, summary,
  parameters, request/response schema refs) from the same OpenAPI model, so the assistant can build
  its tool list from the spec instead of hand-written tool definitions.
- The loopback target is `127.0.0.1:{port}` of the `QApplicationJavalinServer` that is running
  (`getPort()`); the invoker sends the user's timezone header when given, and a per-call timeout
  (default 60 s). Use `invokeForStream` for binary downloads or streaming responses.
- Recursion guard: the invoker counts nested invocations on the calling thread and refuses more
  than 8 (`UiApiInvoker.MAX_DEPTH`).

### In-process sessions, streams and multipart

Components that authenticate a user by other means (for example an SFTP gateway accepting
an SSH key) can mint a `QSession` with
`QAuthenticationModuleInterface.createAutomatedSessionForUser`, then call
`UiApiInProcessSessions.register(session, ttl)` and pass the returned token through
`UiApiCredentials.ofInProcessSession(token)`. TTL must be positive and at most 24 hours.
Tokens exist only in this JVM; use `revoke(token)` when the gateway session ends. Registration
and resolution copy the session (`QSession.clone()`, which also carries a credential scope),
to isolate request state.

The invoker sends only `X-QQQ-In-Process-Session` when these credentials are supplied.
Both session setup paths accept it **only from a loopback socket peer**, and an invalid,
expired or revoked token returns 401 without falling back to other credentials.
`UiApiCredentials.fromContext(ctx)` deliberately ignores this header.
The forwarding headers (`X-Forwarded-For`, `X-Real-IP`, `Forwarded`) are never trusted: only
the socket peer counts. If a reverse proxy runs on the same host, every proxied request arrives
from loopback, so the 256-bit token held in JVM memory is then the whole protection. The token is
never written to logs, exception messages or the 401 response, and must not be logged by callers.

`invokeForStream(request)` returns an `AutoCloseable UiApiStreamResponse` with status,
headers (`getHeader(name)` is case-insensitive), `isSuccess()` and `getInputStream()`.
Use try-with-resources; `readBodyAsString()` consumes an error body as UTF-8. It shares
`invoke`'s operation, credential and depth checks; depth is released when the call returns.

Add files with `request.withMultipartFile(fieldName, fileName, contentType, supplier)`;
repeat the call for multiple files. Each supplier must return a fresh `InputStream` for
an HTTP attempt, and the publisher closes it. Files are streamed with a random boundary;
`withFormParam` values become text parts. JSON and multipart bodies are mutually exclusive.

### AI tool kernel data
`UiApiAiDataGateway` (in `specs.invoke`) is the kernel's `AiDataGateway` over this invoker. Every
data read the model can cause becomes a spec'd operation, made with the user's credentials:
- `POST /table/{t}/query` and `/count`
- `GET /table/{t}/{pk}`
- `POST /search`
- `POST /widget/{name}`

Set it per turn with `new AiToolKernelSettings().withDataGateway(new UiApiAiDataGateway(UiApiInvoker.forServer(server, "assistant"), UiApiCredentials.fromContext(ctx), turnId))`.

## 6. What a client sees
The served `/qqq/v1/openapi.yaml` is core plus every registered extension, so a generated client
for a given deployment covers everything that deployment's UI can call. The core document
committed in qqq covers core only; each extension's fragment is committed in its own repo and
vendored by Next UI next to the core document.
