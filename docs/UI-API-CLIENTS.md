# Building a UI API client (web, CLI, TUI, iOS)

For a jobs tray, list `GET /qqq/v1/jobs?scope=mine&state=active` and use `activeCount` for the badge.
Open `/jobs/{jobId}` for phases, progress, summary, and available actions. A null percent means
indeterminate progress. Subscribe to `user:me` for `job.progress` and `job.status`; when `logSeq`
advances, fetch `/jobs/{jobId}/progress?afterLogId=...`. Poll responses include a twenty-id overlap:
deduplicate by line id, retain the greatest `nextAfterLogId`, and display `logDropped` as omitted
earlier lines. Without realtime, poll each second while visible and running, every ten seconds for
the tray, and pause polling in hidden tabs. Stop active polling when `terminal` is true.

Foreground async status can carry the same `progress` and a redacted 200-line `logTail`.
`POST /jobs/track` detaches its display using `processName`, `processUUID`, and `jobUUID`; the resulting
INLINE job remains dependent on that server pod. Durable processes use the existing process jobs
launch operation and survive a restart. Use the cancel/retry operations and open the returned job id.
Only show an all-users list to holders of `jobs_admin`.

Every QQQ UI is a client of the UI API, `/qqq/v1`. Next UI is the web client. A CLI, TUI or native
iOS client works from the same OpenAPI document: `GET /qqq/v1/openapi.json` on a server, or the
committed `qqq-middleware-javalin/src/main/resources/openapi/v1/openapi.yaml` plus each extension's
`openapi/qqq-v1/{name}.yaml`. This page explains the semantic payloads: what each type *means* and
how each kind of client renders it. It never prescribes React or HTML structure.

Related docs: `UI-API-EXTENSIONS.md` covers the extension point and in-process invocation,
`UI-API-CHANGELOG.md` covers versioning, and `UI-API-AUDIT.md` covers the current gaps.

## 1. Lifecycle

1. `GET /version` (no session). Check `apiVersion` against the range the client supports, and stop
   with a clear message if it is outside it. A 404 means a server before 1.1.0, which serves the
   1.0.0 surface. Use `features` and `extensions` to decide which areas to show.
2. `GET /metaData/authentication` returns the provider type. Web clients sign in with it, then call
   `POST /manageSession`; the session is the `sessionUUID` cookie. CLI, TUI and native clients sign
   in as in section 7 and send `Authorization: Bearer <token>` on every call.
3. `GET /metaData` returns the apps, tables, processes, reports and widgets the user may use. Build
   the navigation from apps (`children`, `sections`) only. Never hard-code table or process names.
4. Use the table, process, widget and report operations as the user moves around.

General rules:
- **Tolerate additions.** Ignore unknown properties. Treat unknown enum values as "other": render the
  item's label or plain value, never crash.
- **Text comes from the server.** Use `label` for display, never `name`. Any HTML the server sends
  (`footerHTML`, `html`, help content in HTML format) becomes plain text in non-web clients: strip
  the tags and keep the links as text.
- **Permissions are enforced by the server.** Hide what metadata says the user cannot do, but always
  handle 403 (show the server's `error` message).
- **Every call is a spec operation.** A client never calls a path that is not in the document.
  Name calls by `operationId`: it is stable for the life of `/qqq/v1`.
- **Errors.** Every error is a `BasicErrorResponseV1`: show `message`, branch on `code`
  (`UNAUTHENTICATED`: sign in again or refresh the token; `PERMISSION_DENIED`; `NOT_FOUND`;
  `CONFLICT`; `RATE_LIMITED`: back off; `BAD_REQUEST`: show `fieldErrors` by field), and print
  `traceId` with any failure the user may report. Treat an unknown `code` by the HTTP status.
- **Client channel.** Send `X-QQQ-Client: cli` (or `tui`, `ios`, ... optionally `/<detail>`, e.g.
  `cli/qqq-cli 0.1.0`) so audits and the client-channel permissions attribute your calls. A
  credential-scoped token is always attributed to `token:<id>` instead.

## 2. Widgets (`POST /widget/{name}`)

The response is the widget data itself. It is one of the `WidgetData` shapes, chosen by `type`
(the OpenAPI `discriminator`). Every shape has `label`, `sublabel`, `footerHTML`,
`hasPermission`, the dropdown lists and `csvData`. Widget inputs, such as the record id and dropdown
selections, go in the JSON body; query parameters are also accepted.

| type | Meaning | Web | CLI | TUI | iOS |
|---|---|---|---|---|---|
| statistics | one headline number (`count`), with an optional comparison (`percentageAmount`, `increaseIsGood`) | stat tile | `label: count (±x%)` | boxed number | stat cell |
| multiStatistics | several statistics groups | tiles row | one line per stat | grid of boxes | grouped cells |
| table, multiTable | columns plus rows; a column of type `hidden` is not shown | data table | aligned text table, or CSV with `--csv` | scrollable table | list with detail rows |
| chart, barChart, horizontalBarChart, stackedBarChart, lineChart, smallLineChart, pieChart | `chartData.labels` plus `chartData.datasets[]` (label and data) | chart | table of labels × datasets | sparkline or bar chart in characters | Swift Charts |
| html | `html` content | rendered HTML | text (tags stripped) | text | attributed string |
| alert | `alertType` (severity) plus `html` or `bulletList` | banner | prefixed line (`[WARN] ...`) | coloured line | inline banner |
| divider | a visual break | rule | blank line | rule | separator |
| fieldValueList | label/value pairs of a `record` | definition list | `label: value` lines | two-column list | form section |
| stepper | `steps[]` with `activeStep` | stepper | `[x] [>] [ ]` list | step list | progress list |
| location, usaMap | a place or map markers | map | address / coordinates text | text list | MapKit |
| quickSightChart | an embedded external dashboard URL | iframe | print the URL | print the URL | open in web view |
| process | `processMetaData` to run inline | embedded process | run it as a process (section 3) | same | same |
| parentWidget, composite | children (`childWidgetNameList`, or `blocks` with `layout`) | layout of children | children in order | children stacked | stacked sections |
| childRecordList | `queryOutput` of a child table, plus add/edit/delete capabilities | child grid | child table | child table | child list |
| dynamicForm, cronUI, rowBuilder, filterAndColumnsSetup, pivotTableSetup | editors for a record field's value (`fieldList`, cron, rows, filter) | rich editor | prompt for each field; edit the raw value | form | form |
| dataBagViewer, scriptViewer, customComponent, generic | developer views; customComponent is web-only JavaScript | component | show the label; offer the raw record | same | same |

When `hasPermission` is false, show the label and "You do not have permission to view this data."
If `dropdownNameList` is present, the widget is a filtered view: offer one selector per dropdown
(options in `dropdownDataList`) and send the choices back as inputs.

## 3. Processes (`/processes/...`)

`init`, then `step` until `noMoreSteps`. Each frontend step lists `components`. Render them in order:

| Component type | Meaning | Non-web rendering |
|---|---|---|
| HELP_TEXT, HTML | explanatory text | text (HTML stripped) |
| EDIT_FORM | fields for the step's `inputFieldList` (the `includeFieldNames` subset) | prompts or a form, with field types and possible values from metadata |
| VIEW_FORM | read-only values | `label: value` |
| RECORD_LIST | the records the process is working on (`GET .../records`) | table, paged |
| VALIDATION_REVIEW_SCREEN | a preview (`previewMessage`, `recordCount`), with the choice to validate fully (`supportsFullValidation`) | show the counts and ask *Validate* or *Skip* |
| PROCESS_SUMMARY_RESULTS | `processResults[]` lines: `status` (OK/WARNING/ERROR/INFO), `count`, `message`, optional link to records | one line per result, with the status as a prefix |
| DOWNLOAD_FORM | a file to download: `GET /download/{file}` | save the file and print its path |
| GOOGLE_DRIVE_SELECT_FOLDER | choose a Google Drive folder | not available outside the web: show a message |
| BULK_EDIT_FORM | fields with "change this field" toggles | per field: keep or set a value |
| BULK_LOAD_FILE_MAPPING_FORM, BULK_LOAD_VALUE_MAPPING_FORM, BULK_LOAD_PROFILE_FORM | map a file's columns and values to fields; save the mapping as a profile | interactive mapping prompts, or a profile chosen by name |
| WIDGET | an ad-hoc widget (`widgetName`) | as in section 2 |

Long steps return a `jobUUID`. Poll `GET .../status/{jobUUID}`, which may include progress, and
offer `POST .../cancel`.

## 4. Field adornments

`fieldMetaData.adornments[]` gives each field's semantic display hints. The `values` keys are
documented in the `FieldAdornment` schema.

| Type | Meaning | Non-web rendering |
|---|---|---|
| LINK | the value links to a record (`toRecordFromTable`) or a URL | CLI prints the target; TUI/iOS navigate |
| CHIP | the value is a status; `color.{value}` gives a semantic colour (success, warning, error, info...) | coloured or bracketed text: `[Active]` |
| SIZE | `width` is a semantic width (xsmall ... xlarge) | map it to columns or characters |
| CODE_EDITOR | the value is code (`languageMode`) | monospaced; open `$EDITOR` to edit |
| RENDER_HTML | the value is HTML | strip the tags |
| REVEAL | the value is secret until revealed | mask it and reveal on request |
| FILE_DOWNLOAD | the value is a file: `GET /table/{t}/{pk}/{field}/{fileName}` | download on request |
| FILE_UPLOAD | the field takes a file (accepted `format`) | file path prompt or picker |
| FILE_PREVIEW | an image, video or PDF, with thumbnail/full-size/mime fields | iOS: QuickLook; CLI: print the URL |
| TOOLTIP | help text for the value (`staticText`, or per record via the display value `{field}:tooltipDynamic`) | `?` help or detail line |
| WIDGET | a widget renders the field (`widgetName`) | as in section 2 |
| ERROR | the value is an error message | prefix it with an error marker |

Per-record dynamic values use display-value keys of the form `{fieldName}:{key}`, e.g.
`email:tooltipDynamic`.

## 5. Metadata hints

Hints describe meaning, never markup:
- **Sections.** `tier` T1 is the identity and primary section; T2 and T3 are secondary. A small screen
  shows T1 first and collapses the rest.
- **Field behaviours.** For example, `TO_UPPER_CASE` and `TO_LOWER_CASE` normalise input. The server
  applies them on save, so a client may preview them.
- **Help content.** Each item has `roles` (where to show it: field, section, screen), `format`
  (TEXT/HTML/MARKDOWN) and `content`.
- **Possible values and the record picker.** A field with a possible value source shows a picker.
  The `picker` hints say how to present it as a list or a media grid, and which subtitle and
  thumbnail fields to use. Search with `POST /table/{t}/possibleValues/{field}`.
- **App home.** Apps list `widgets` (the home dashboard) and `sections` (groups of tables, processes
  and reports). Render the widgets first, then the sections.
- **Table capabilities.** `clientCapabilities` on table metadata says what to offer beyond CRUD, for
  this user, and how: `savedViews` (run `queryProcessName` with `tableName`; store and delete only
  when their process names are present), `audits` (run `processName`, or query `tableName` joined to
  `detailTableName`), `sharing` (the four sharing processes) and the bulk processes
  (`bulkInsert`, `bulkEdit`, `bulkEditWithFile`, `bulkDelete`: run `processName` with the selected
  records). A capability that is absent or `available: false` is not offered. Never hard-code these
  process names.
- **Validation.** `fields.{name}.validation` lists the rules the server enforces: `required`,
  `maxLength` (`tooLongBehavior` ERROR refuses, TRUNCATE trims), `minValue`/`maxValue` (with
  `minInclusive`/`maxInclusive`) and `pattern` (a whole-value regular expression, with
  `patternMessage`). Check them before saving; still show the server's record errors, since the
  server is the authority.

## 7. Authentication for CLI, TUI and native clients

Read `GET /qqq/v1/metaData/authentication`. For `type: OAUTH2`, `values` gives the issuer
(`baseUrl`), the scopes, and, when the app has one, `nativeClientId`: a **public** OAuth2 client
(no secret) for native clients, whose issuer is `nativeClientBaseUrl` (or `baseUrl`). Fetch the
issuer's discovery document, `{issuer}.well-known/openid-configuration`, for its endpoints.

**Device-authorization grant (RFC 8628): best for a CLI or TUI, and for SSH sessions.**
1. `POST {device_authorization_endpoint}` with `client_id={nativeClientId}` and
   `scope=openid profile email offline_access`.
2. Show the user `verification_uri_complete` (or `verification_uri` and `user_code`), and poll
   `POST {token_endpoint}` with `grant_type=urn:ietf:params:oauth:grant-type:device_code`,
   `client_id` and `device_code` every `interval` seconds while the answer is
   `authorization_pending` (add 5 s on `slow_down`; stop on `expired_token` or `access_denied`).
3. Keep the `access_token` and `refresh_token` in the OS keychain, or in a file only the user can
   read (mode 0600). Never log them.

**Authorization code + PKCE: best for a desktop or mobile app with a browser.**
Use `nativeClientId` with a redirect URI registered at the provider: a loopback URI
(`http://127.0.0.1:{port}/callback`, any free port, RFC 8252) or a custom scheme
(`com.example.app://oauth/callback`). Exchange the code at the token endpoint with the
`code_verifier`. Every redirect URI must be registered on the provider (the kof22 Authentik
`kof22-admin-cli` provider, for example, allows `http://127.0.0.1:<port>/callback` and
`http://localhost:<port>/callback`).

**Calling the API.** Send `Authorization: Bearer {access_token}`. The server verifies the JWT
against the provider's keys (issuer, audience = the app's client or `nativeClientId`, expiry,
signature; `alg: none` and HMAC are refused) and maps it to the same user, permissions and token
scope as a web sign-in. Bearer calls are stateless: no cookie is set.

**Refreshing.** Access tokens are short-lived (minutes). On `401` (`code: UNAUTHENTICATED`), or
shortly before `exp`, refresh at the provider: `POST {token_endpoint}` with
`grant_type=refresh_token`, `client_id={nativeClientId}` and the refresh token. Store the new pair
(providers may rotate refresh tokens). When refreshing fails, sign in again.

**Other bearer tokens.**
- *QQQ access tokens* (personal access tokens, e.g. qbit-authentik app passwords): exchange the
  token for a JWT at the provider (for Authentik, `grant_type=client_credentials` with the username
  and the token as password), then send the JWT as above. The token's scope still applies.
- *Session UUID:* a native app that used `POST /manageSession` (auth code + PKCE with the app's own
  client) may send `Authorization: Bearer {sessionUUID}` instead of the cookie; the server keeps
  that session's tokens refreshed.

## 8. Several servers (replicas)

Through QQQ 4.1.0-RC.10, process `init`/`step`/`status`/`records`, and async job status, read
replica-local state. A client must keep all calls for one `processUUID`/`jobUUID` on the same
backend replica (for example with a sticky cookie), or the calls return "Could not find status" or
permission denied. A CLI or TUI must therefore keep a cookie jar for the load balancer's affinity
cookie even when it authenticates with a bearer token. Realtime (SSE)
events reach only the subscribers connected to the replica that published them, unless the
deployment configures a cross-node realtime bus (`RealtimeMetaData.bus`, e.g. qqq-esb
`EsbRealtimeBus`).

From the release that adds shared process state (the cluster/HA work, the next 4.1 RC), an app that
configures the table-backed state provider needs no affinity: any replica serves any process or job
call, and a step whose replica died reports a clear error instead of hanging. Durable process jobs
(`POST /processes/{processName}/jobs`, `/jobs/...`) are database-backed and already work from any
replica.

## 6. Known gaps (tracked in UI-API-AUDIT.md)

These still need typed schemas or v1 operations before a non-web client can match the web UI:
- the alerts, assistant and sql-workbench payloads, until those qbits ship as extensions;
- the `materialDashboard` supplemental settings: go-to fields, quick filters, sidebar, form adjusters;
- form adjusters (a legacy route today).

Until they are typed, a non-web client shows these areas as read-only or omits them.
