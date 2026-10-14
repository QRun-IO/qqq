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
- **One bump per QQQ release, not per PR.** All additive changes between two releases share one
  MINOR. The contract test compares against the last *released* document (`baseline.yaml`), so
  the version only has to be above the released one. Later PRs in the same release add their notes
  to the open entry below.
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

## Extensions

| Extension | Prefix | Version | Owner |
|---|---|---|---|
| esb | `/esb` | 1.0.0 | qqq-esb (`EsbJavalinMetaDataProducer`) |

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

### 1.2.0 (QQQ 4.1.0-RC.6, unreleased)
1.1.0 was never released; its additions ship here.
- Added typed `recordFeatures.ownership` and `builtInViews` to table metadata for
  ASSIGNMENT-enabled tables. The "My records" filter preserves `CurrentUser`
  expression objects (`type: CurrentUser`, displayed as "Me"). Core also resolves
  `${currentUser}` filter values per session; a missing user matches no records.
- Added `GET /version` (unsecured): the API semver, base path, supported majors, core features
  and registered extensions.
- Added the extension point: modules and qbits register endpoint specs
  (`MiddlewareSpecExtensionsMetaData`, `AbstractJsonEndpointSpec`); they are served under
  `/qqq/v1/qbit/{name}/...` (or an established prefix: `/esb`, `/alerts`, `/assistant`,
  `/sql-workbench`) and included in the served OpenAPI document. `UiApiInvoker` invokes spec'd
  operations in-process with the caller's credentials.
- `info.version` is now semver (it was `v1`).
- `POST /widget/{name}`: its response is now typed as `WidgetData`, a discriminated union (by
  `type`) of one schema per widget type, built from the backend's widget data classes. The request
  body is documented as string inputs, which may also be sent as query parameters.
  - This corrects the document to what the server always returned: the data itself, not a
    `{widgetData}` wrapper.
  - The contract test accepts the `$ref` change as a correction (`accepted-breaking-changes.txt`).
- `FieldAdornment.type` now lists every adornment type (adds FILE_UPLOAD and TOOLTIP).
  `FieldAdornment.values` documents its known keys.
- `FrontendComponent.type` now lists every component type (adds the three BULK_LOAD forms).
- Client channels (`docs/UI-API-CLIENT-CHANNELS.md`):
  - the `X-QQQ-Client: <channel>[/<detail>]` request header, checked against
    `ClientChannelsMetaData` (an unknown channel gets 400);
  - `GET /version` lists the accepted channels in `clientChannels`;
  - the channel is stored on the session and in audits (`audit.clientChannel`, opt-in);
  - channel deny rules: `QPermissionRules.withDeniedClients`, plus denied objects and patterns, with a
    default wall that keeps the assistant away from its own configuration, identity, permissions,
    audits, API keys, credentials and secrets.
- Added `docs/UI-API-CLIENTS.md`: how web, CLI, TUI and iOS clients render each type.
- Record collaboration (docs/COLLABORATION.md):
  - Table metadata gains `recordFeatures` (`RecordFeaturesV1`), and the metadata responses gain
    `metaDataGeneration`.
  - Added `GET /table/{tableName}/{primaryKey}/features` (`RecordFeatureSummaryV1`).
  - Added `GET /realtime/stream` (Server-Sent Events), with one schema per event type, and
    `POST /realtime/subscriptions`.
  - `PATCH /table/{tableName}/{primaryKey}` accepts an optional `expectedVersion`. A stale
    version returns 409 `RecordConflictV1`.
  - Core features `recordFeatures` and `realtime` are listed by `GET /version`.
- Added related-record pickers (`docs/RELATED-RECORD-PICKER.md`): the operations
  `POST /table/{tableName}/relatedRecordPicker/{pickerName}/query|prepare|add|remove` with the
  `RelatedRecordPicker{Query,Prepare,Add,Remove}ResponseV1` schemas, and the table metadata
  properties `relatedRecordPickers` (`RelatedRecordPicker` schema) and `quickCreateSectionName`.

**Accepted corrections.** A change the checker reports as breaking can be accepted only when it
corrects the document to the server's actual, unchanged behaviour. List it, with the reason, in
`openapi/v1/accepted-breaking-changes.txt`. The list is cleared when the baseline is refreshed.

### 1.0.0 (baseline)
- The 32 operations of QQQ 4.1.0-RC.5: authentication, session, metaData, tables (query, count,
  get, insert, update, delete, variants, export, possible values, developer mode), record search,
  widgets, processes (init, step, status, records, cancel, possible values), reports and
  downloads.
