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

Raw routes under `/qqq/v1` that have no spec are detected at startup (`UiApiRouteAuditor`).
Since 1.3.0 (#1159) such routes fail startup by default (strict mode). `withStrictUiApiRoutes(false)`
or `QQQ_UI_API_STRICT_ROUTES=false` only logs them, as a temporary opt-out.

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

### 1.3.0 (unreleased)
- Added `UiApiRequest.withHeader(name, value)` for custom invocation headers; null values remove headers, and the invoker drops reserved credential, client-channel and request-metadata headers case-insensitively.
- Strict UI API routes are the default (#1159): startup fails when a route under `/qqq/v1` has no
  spec (core or extension). `QQQ_UI_API_STRICT_ROUTES=false` (or `withStrictUiApiRoutes(false)`)
  opts out temporarily. Applications must run the spec'd alerts, assistant and sql-workbench qbit
  releases.
- The assistant wall denies `audit*` (reads and writes) by default again; `withAssistantAuditReads()`
  is the host opt-in for reads. See the security record in UI-API-CLIENT-CHANNELS.md.
- Durable jobs (docs/specs/DURABLE-JOBS-AND-ASSISTED-BULK-LOAD.md):
  - Added `POST /processes/{processName}/jobs`: launch an eligible process as a durable background
    job (`ProcessJobLaunchResponseV1 {jobId, status}`). Over the per-user queue cap it returns
    429. From the `assistant` client channel the job is created `AWAITING_CONFIRMATION`.
  - Jobs are the standard tables `jobs_job`, `jobs_chunk`, `jobs_row_error`, `jobs_event` and
    `jobs_worker`, read with the table operations. Control actions are the record-action processes
    `jobsStart`, `jobsPause`, `jobsResume`, `jobsCancel`, `jobsRetryFailed`, `jobsRestart` and
    `jobsRunAgain`, run with the process operations.
  - The core feature `durableJobs` is listed by `GET /version`.
  - Added `POST /uploads` (multipart with one file, or the raw body plus a `fileName` query
    parameter): streams the file into the uploads storage table and returns `UploadRefResponseV1
    {ref, fileName, sizeBytes, contentType, expiresAt}`. The ref is opaque, bound to the caller, and
    expires (default 7 days); pass it in `uploadRefs` when launching a job.
  - Process metadata gains `durableJobEligible` and `durableJobAlways`, so clients can offer
    "Run in background".
  - A process run over `durableThresholdRows` (or `alwaysDurable`) no longer runs its execute step
    inline: the step returns at once with the value `durableJobId` and a summary line, and the job
    does the work.
- Process init/step responses now optionally include `oneTimeSecrets` using the shared
  `PreferenceSecretV1` schema, with `Cache-Control: no-store`. A synchronous step returns them
  in its own response; a step that went async hands them, once, to the first status poll that
  sees the job complete. Stored state exposes only `values.oneTimeSecretsMasked`.
  See `docs/misc/ProcessBackendSteps.adoc`.
- Extended preferences with `POST /preferences/{sectionName}/actions/{actionName}` and
  `PreferenceActionRequestV1`, `PreferenceActionResponseV1`, `PreferenceActionV1`, and
  `PreferenceSecretV1`. Section actions support SECTION/LIST/ROW scopes, typed inputs,
  confirmations and destructive styling; rows expose nullable `actionNames` permissions.
  Action input ENUM choices are included in section `possibleValues`. Responses include the
  reloaded section and optional one-time secrets with `Cache-Control: no-store`; clients must
  not store or log secrets. ROW actions can target only allowed rows loaded for the session user.
  Secrets use shared core `OneTimeSecret`; `PreferenceSecretV1` exposes `label`, `value`,
  `masked` and multiline `usageText` (replacing `help`). Sections publish `showInUserMenu`
  (default false) so clients can add a label/icon menu entry opening the section.
  Provider validation and storage share the trusted scope with the original session.
  The example `access-tokens` section is owned by qbit-authentik.
  Core also exposes the session-derived `getUserEmailAddress()` for preference providers.
- Added per-user preferences (`docs/USER-PREFERENCES.md`): `GET /preferences`,
  `GET /preferences/{sectionName}` and `PATCH /preferences/{sectionName}`. The schemas
  `PreferencesResponseV1`, `PreferenceSectionV1`, `PreferenceListV1`, `PreferenceRowV1`
  and `PreferenceSectionPatchRequestV1` describe typed fields, fixed rows, inline ENUM
  choices and partial updates scoped to the signed-in user. `preferences` is a core
  feature and `/preferences` is reserved by core.
- AI tool kernel data access goes through the UI API. `AiToolKernelSettings.withDataGateway(new UiApiAiDataGateway(...))` makes every query, count, get, search and widget render a `/qqq/v1` call, made as the user through the assistant channel. `CoreAiDataGateway` remains the default for other kernels. The assistant wall keeps `audit*` hidden by default; a host opts in to reads with `withAssistantAuditReads()`, and audits are never writable by the assistant (see UI-API-CLIENT-CHANNELS.md, security record).
- Scoped credentials: `QSession.credentialScope` (`QCredentialScope`) narrows a session to the apps, tables, processes, reports, widgets and operations it lists, optionally read-only.
  - It is intersected with the user's live permissions.
  - The session's client channel is `token:<id>`; "token" is a new default channel.
  - Access tokens from qbit-authentik set it.

### 1.2.0 (QQQ 4.1.0-RC.6)
1.1.0 was never released; its additions ship here.
- In-app help (docs/HELP-CONTENT.md):
  - Typed `HelpContentV1` entries and slot maps across all existing help properties; new `helpContents` maps on apps, processes and reports. Targeting fields remain server-only.
  - Added secured `GET /help/search?q=&limit=` with ranked results, canonical keys, summaries, long help and application-relative links (default 20, maximum 50).
  - Added `GET /help/coverage?kind=&missingOnly=&parentKey=` for users with INSERT or EDIT permission on `helpContent`; includes hidden targets and scoped totals before the missing-only filter.
  - Per-session permission and client-channel targeting, safe Markdown rendering and cross-node reload. `GET /version` advertises the `help` feature.
- Added JVM-local loopback-only session tokens, streamed responses and multipart uploads to `UiApiInvoker`.
- Added the assisted bulk-load operations (tag `Bulk Load`, design spec #1171 section 3.2):
  `GET /bulkLoad/uploads/{uploadRef}/profile`, `POST /bulkLoad/{tableName}/suggestMapping` and
  `POST /bulkLoad/{tableName}/validate` (a dry run through the table's bulk-insert process; nothing
  is stored). Uploads are read through `BulkLoadUploadSources` by their opaque ref, as the session
  user. The `X-QQQ-AI-Route: hipaa` request header allows sample values of classified and unmapped
  columns; without it they are left out.
- Added contingent field metadata: `possibleValueSourceDependsOn` lists explicit and
  inferred input dependencies; `possibleValueSourceDependencyBehavior` defaults to
  `DISABLE_UNTIL_SET`. Both properties are absent for non-contingent fields.
- Added `SHOW_ALL_UNTIL_SET`: FORM searches omit missing-input criteria while
  retaining criteria for supplied inputs. FILTER searches remain unrestricted by
  missing inputs; use-case parsing is consistent and case-insensitive.
- User-sourced table writes validate contingent choices in batches, including
  partial updates and custom providers; fields can opt out with
  `validatePossibleValueSourceOnWrite(false)`. SYSTEM writes are excluded.
  See [Contingent possible values](CONTINGENT-POSSIBLE-VALUES.md) for examples,
  frontend obligations, sample fixtures and the process-step validation limitation.
- Added typed `recordFeatures.ownership` and `builtInViews` to table metadata for
  ASSIGNMENT-enabled tables. The "My records" filter preserves `CurrentUser`
  expression objects (`type: CurrentUser`, displayed as "Me"). Core also resolves
  `${currentUser}` filter values per session; a missing user matches no records.
- Secured data (field access tiers, docs/SECURED-DATA.md):
  - Field policy enforcement is independent of table-permission opt-in, and the assistant uses the same field decision with its default channel policy.
  - Field metadata adds `accessTier` (FULL, REVEALABLE, MASKED, HIDDEN),
    `allowedFilterOperators`, and `isSortable`; table metadata adds `hasRestrictedFields`.
    HIDDEN fields are omitted and restricted fields are not editable.
  - Disallowed filters and sorting return 403 using the standard error body; exact-match
    policy refusals preserve the resolver's user-facing message.
  - Masked values and display values are strings, regardless of the underlying field type;
    masking applies to records, process results, structured widgets, exports, and audit details.
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
