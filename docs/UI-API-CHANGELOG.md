# UI API changelog (`/qqq/v1`)

The UI/UX API is `/qqq/v1` (`qqq-middleware-javalin`, `specs/v1`). It is the only API that drives a
UI (Next today; CLI, TUI and native iOS clients later), and it is semantically versioned
independently of QQQ's own release version.

## Rules

- **Version format.** `info.version` in `openapi/v1/openapi.yaml`, `GET /qqq/v1/version`
  (`apiVersion`) and `MiddlewareVersionV1.SEMANTIC_VERSION` are all `MAJOR.MINOR.PATCH`.
  MAJOR always equals the URL version: every `1.x.y` is served at `/qqq/v1`.
- **MINOR: additive changes.** New operations, new optional parameters, new response properties,
  new schemas, new enum values, and new extensions. A client built against `1.x` keeps working with
  any server `1.y` where `y >= x`.
- **PATCH: fixes.** Behaviour fixes and corrections to the document that change no structure, such
  as descriptions or examples. A PATCH bump is optional for documentation-only changes, which the
  contract test does not count as a change.
- **Breaking changes need a new URL version (`/qqq/v2`).** These include removing or renaming an
  operation, parameter, property, schema, enum value or response code; changing a type or format;
  and making something required that was optional. v1 and v2 are then served side by side, and v1
  gets a sunset date here.
- **Clients must tolerate additions.** Ignore unknown properties, and treat unknown enum values as
  "other" (see `UI-API-CLIENTS.md`).
- **Extensions.** Each extension (module or qbit, registered through
  `MiddlewareSpecExtensionsMetaData`) carries its own semver under the same rules. Its document
  is committed in its own repo at `src/main/resources/openapi/qqq-v1/{name}.yaml` and checked with
  `UiApiContract.check(...)`. `GET /qqq/v1/version` lists every registered extension with its
  version.

## Enforcement

`UiApiContractTest` (qqq-middleware-javalin) runs in every build. It fails when:
1. the committed `openapi.yaml` differs from the document generated from the code (regenerate with
   `PublishAPI -r <repo root>`);
2. any change from `openapi/v1/baseline.yaml` (the document as of the last release) to
   `openapi.yaml` is breaking (`OpenApiCompatibilityChecker`);
3. the document changed structurally since the baseline but `info.version` was not raised above
   the baseline's;
4. `info.version` is not semver, or its MAJOR is not 1.

When cutting an RC or GA, the release captain refreshes the baseline with
`PublishAPI -r <repo root> --baseline`. Several additive PRs between releases can share one MINOR
bump.

Raw routes under `/qqq/v1` that have no spec are logged at startup (`UiApiRouteAuditor`).
Strict mode (`withStrictUiApiRoutes(true)` or `QQQ_UI_API_STRICT_ROUTES=true`) fails startup
instead. Strict becomes the default once the alerts, assistant and sql-workbench qbits ship their
spec'd routes.

## Legacy unversioned routes (deprecated)

The unversioned `QJavalinImplementation` routes (`/metaData`, `/data/...`, `/processes/...`,
`/widget/...`, `/possibleValues/...`, `/download/...`, `/reports/...`, `/serverInfo`,
`/manageSession`) are the Material Dashboard's API. They are **deprecated as of 2026-10-13**.
Next UI makes no calls to them (see `UI-API-AUDIT.md`).

- Every response from them carries `Deprecation: @1760313600` (RFC 9745) and
  `Link: </qqq/v1/openapi.html>; rel="successor-version"`.
- The first call to each route family is logged at INFO.
- They stay served while the Material Dashboard uses them, because
  `serveLegacyUnversionedMiddlewareAPI` defaults to true.
- **Sunset:** they are removed together with the Material Dashboard. No date is set yet; it will be
  announced here.

## Versions

### 1.1.0 (QQQ 4.1.0-RC.6, unreleased)
- Added `GET /version` (unsecured): the API semver, base path, supported majors, core features
  and registered extensions.
- Added the extension point: modules and qbits register endpoint specs
  (`MiddlewareSpecExtensionsMetaData`); they are served under `/qqq/v1/qbit/{name}/...` (or an
  established prefix: `/esb`, `/alerts`, `/assistant`, `/sql-workbench`) and included in the served
  OpenAPI document.
- `info.version` is now semver (it was `v1`).

### 1.0.0 (baseline)
- The 32 operations of QQQ 4.1.0-RC.5: authentication, session, metaData, tables (query, count,
  get, insert, update, delete, variants, export, possible values, developer mode), record search,
  widgets, processes (init, step, status, records, cancel, possible values), reports and
  downloads.
