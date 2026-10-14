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
2. `GET /metaData/authentication` returns the provider type. Sign in with it, then call
   `POST /manageSession`. The session is the `sessionUUID` cookie; native clients keep it in their
   cookie jar or keychain.
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

## 6. Known gaps (tracked in UI-API-AUDIT.md)

These still need typed schemas or v1 operations before a non-web client can match the web UI:
- the alerts, assistant and sql-workbench payloads, until those qbits ship as extensions;
- the `materialDashboard` supplemental settings: go-to fields, quick filters, sidebar, form adjusters;
- saved views, sharing, audits and bulk-process conventions, which are named by convention today;
- form adjusters (a legacy route today).

Until they are typed, a non-web client shows these areas as read-only or omits them.
