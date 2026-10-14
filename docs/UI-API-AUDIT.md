# UI API audit (`/qqq/v1`)

Date: 2026-10-13. Sources: QRun-IO/qqq `develop` @ ed94d5ccf (committed
`qqq-middleware-javalin/src/main/resources/openapi/v1/openapi.yaml`, 32 operations),
QRun-IO/qqq-frontend-next `main` @ c7fe584 (RC.17), and the qbit checkouts listed in section 4.
Scope: Next UI `src/` only (no tests, `src/mocks/` or stories).

Goal (owner): 100% of the UI/UX goes through the versioned UI API, which must be frontend-agnostic
(web, CLI, TUI, native iOS) and semantically versioned. Plan: `docs/UI-API-EXTENSIONS.md`.

## 0. Summary

| Class | Distinct method+path templates called by Next | Notes |
|---|---|---|
| SPEC (in `openapi.yaml`) | **31** of 32 | the one not called is `POST /oidc/backchannel-logout` (server-to-server) |
| OFF-SPEC under `/qqq/v1` | **43** | alerts 5, assistant 10, esb 5, sql-workbench 20, API-versioned 3 |
| LEGACY / outside `/qqq/v1` | **6** | form-adjuster 1, API-middleware discovery 5 |
| EXTERNAL | 10 | IdP, analytics, Google, Gravatar, custom-component JS |

So 31 of 80 UI-API calls (39%) are described by the spec today. Correction to the starting
assumptions: Next does **not** call the unversioned `QJavalinImplementation` routes. Its axios
base URL is `/qqq/v1`, so `apiClient.get('/metaData')`, `/processes/{p}/init` and
`/processes/{p}/possibleValues/{f}` are the v1 routes. `/data/{t}/{pk}/{f}/{file}` only appears as a
legacy value that the backend still emits in record values and Next rewrites to the v1 field route
(`lib/utils/adornment-utils.ts:26-39`); it is never requested. The real legacy calls are listed
in section 1.3.

Spec/client mismatch found: `POST /widget/{name}`. The spec takes params in the JSON body and
wraps the response as `{widgetData:{...}}` (`WidgetResponseV1`); Next sends params as query
string and reads `type` at the top level (`lib/api/widgets.ts:61-71`). Fix in phase 5 (spec the
real shape; the server answers both today).

## 1. Call map (Next `src/`)

### 1.1 SPEC (31)
| Method | Path (under `/qqq/v1`) | Next src |
|---|---|---|
| GET | /metaData/authentication | lib/api/auth.ts:76 |
| POST | /manageSession | lib/api/auth.ts:125,154,170,181 |
| POST | /logout | lib/api/auth.ts:192 |
| GET | /metaData | lib/api/metadata.ts:38; app/(dashboard)/app/developer/page.tsx:192 |
| GET | /metaData/table/{t} | lib/api/metadata.ts:81 |
| GET | /metaData/process/{p} | lib/api/metadata.ts:100 |
| POST | /table/{t}/query | lib/api/tables.ts:157; lib/api/audits.ts:94 |
| POST | /table/{t}/count | lib/api/tables.ts:187 |
| POST | /search | lib/api/record-search.ts:265 |
| GET | /table/{t}/variants | lib/api/tables.ts:72 |
| GET / PATCH / DELETE | /table/{t}/{pk} | lib/api/tables.ts:224, 356, 375 |
| POST | /table/{t} | lib/api/tables.ts:332 |
| GET | /table/{t}/{pk}/developer | lib/api/developer.ts:341 |
| GET | /table/{t}/{pk}/developer/panels | lib/api/developer-panels.ts:203 |
| POST | /table/{t}/{pk}/developer/associatedScript/{f} | lib/api/developer.ts:382 |
| GET | /table/{t}/{pk}/developer/associatedScript/{f}/{rev}/logs | lib/api/developer.ts:411 |
| POST | /table/{t}/possibleValues/{f} | lib/api/possible-values.ts:130 |
| POST | /processes/{p}/possibleValues/{f} | lib/api/possible-values.ts:130 |
| POST | /possibleValues/{pvs} | lib/api/possible-values.ts:130 |
| POST | /widget/{w} | lib/api/widgets.ts:61 (shape mismatch, see summary) |
| POST | /processes/{p}/init | lib/api/processes.ts:307; lib/api/audits.ts:78 |
| POST | /processes/{p}/{uuid}/step/{s} | lib/api/processes.ts:336 |
| GET | /processes/{p}/{uuid}/status/{job} | lib/api/processes.ts:374 |
| GET | /processes/{p}/{uuid}/records | lib/api/processes.ts:400 |
| POST | /processes/{p}/{uuid}/cancel | lib/api/processes.ts:427 |
| POST | /table/{t}/export | lib/api/tables.ts:103 |
| GET (href/img) | /table/{t}/{pk}/{field}/{filename} | lib/utils/adornment-utils.ts:39; components/preview/FilePreviewLightbox.tsx; lib/utils/record-download.ts:54,80 |
| GET (href) | /download/{file} | lib/api/processes.ts:511; components/process/DownloadFormComponent.tsx:44; components/reports/ReportRun.tsx:71 |
| GET (href) | /reports/{report} | lib/api/reports.ts:58; components/reports/ReportRun.tsx:208 |

### 1.2 OFF-SPEC under `/qqq/v1` (43)
| Owner (registration) | Method + path (under `/qqq/v1`) | Next src |
|---|---|---|
| qbit-alerts (`AlertsRouteProvider`) | GET /alerts/inbox/summary; GET /alerts/inbox; POST /alerts/inbox/{read,unread,dismiss}; GET /alerts/record-panel; POST /alerts/record-panel/watch | lib/api/alerts.ts:104-180 |
| qbit-assistant (`AssistantRouteProvider`) | GET /assistant/config; GET+POST /assistant/conversations; GET+PATCH+DELETE /assistant/conversations/{id}; POST /assistant/conversations/{id}/turns (SSE response); GET /assistant/turns/{turnId}/events (SSE); POST /assistant/turns/{turnId}/cancel; POST /assistant/messages/{id}/feedback | lib/api/assistant.ts:86-313; lib/api/assistant-probe.ts:38,55 |
| qqq-esb (`EsbRouteProvider`) | GET /esb/overview; /esb/table/{t}; /esb/process/{p}; /esb/messages/{dest}; /esb/deadLetters/{trigger} | lib/api/esb.ts:54-122 |
| qbit-sql-workbench (`SqlWorkbenchRouteProvider`) | GET capabilities; GET schema/{backend}; POST execute; POST explain; GET executions/{uuid}/rows; POST executions/{uuid}/cancel; GET executions/{uuid}/export (href); GET history; GET+POST notebooks; GET+PUT+DELETE notebooks/{id}; POST notebooks/{id}/publish; POST notebooks/{id}/duplicate; GET notebooks/{id}/versions; POST notebooks/{id}/versions/{n}/restore; GET+POST notebooks/{id}/snapshots; DELETE notebooks/{id}/snapshots/{sid} (all under /sql-workbench) | lib/api/sql-workbench.ts:55-329 |
| qqq-middleware-api (`ApiAwareMiddlewareVersionV1`) | GET /{apiPath}/{apiVersion}/metaData/table/{t}; POST /{apiPath}/{apiVersion}/table/{t}/query; POST .../count | lib/api/api-versioned.ts:67-104 |

The API-versioned three are spec classes (ApiAware variants of the core specs) but are only bound
when an application passes `ApiAwareMiddlewareVersionV1` to the server, and they are not in the
committed document. Phase 5 adds them to the document as an optional path family.

### 1.3 LEGACY / outside `/qqq/v1` (6)
| Method | Path | Next src | Replacement (phase 4) |
|---|---|---|---|
| POST | /material-dashboard-backend/form-adjuster/{id}/{onChange,onLoad} | lib/api/form-adjuster.ts:114 | v1 spec `POST /table/{t}/formAdjuster/{id}/{event}` added in 1.3.0 (#1152); Next moves once it ships in an RC |
| GET | /apis.json?tableName= | lib/api/developer.ts:282 | v1 `GET /table/{t}/apis` added in 1.3.0 (#1152) |
| GET | {apiPath}versions.json | lib/api/developer.ts:301 | same (versions and table spec paths included) |
| GET (+href) | {apiPath}{ver}/{table}/openapi.json | lib/api/developer.ts:318,330; components/developer/RecordApiAccess.tsx:239 | links in the same response (documentation links stay hrefs) |
| GET (href) | {apiPath}{ver}/ and {apiPath}{ver}/openapi.json | components/developer/RecordApiAccess.tsx:237-238 | links in the same response |

### 1.4 EXTERNAL (not UI API; unchanged)
OIDC discovery and token (lib/auth/oidc.ts:146,214) and IdP navigations; Google Tag Manager,
PostHog and plugin scripts (lib/analytics/*); Google Picker scripts (lib/google-drive-picker.ts);
Gravatar (lib/utils/gravatar.ts:97); `customComponent` widget `componentSourceUrl`
(components/widgets/QqqContainerWidgets.tsx:226, web-only by nature).

## 2. Payload fields and enums Next reads that the spec does not describe

"partial" = the key exists in the spec but as an untyped object/array or free string.

### 2.1 Widgets (spec: `type` is a free string; `widgetData` is an untyped object)
- **Type enum** (no): statistics, multiStatistics, table, multiTable, stepper, html, alert, divider,
  fieldValueList, location, usaMap, quickSightChart, generic, process, parentWidget, composite,
  block, parent, childRecordList, customComponent, cronUI, dynamicForm, dataBagViewer,
  pivotTableSetup, filterAndColumnsSetup, rowBuilder, scriptViewer, ESB_OVERVIEW, SQL_WORKBENCH,
  recordGrid, quickLinks, processSummary, barChart, horizontalBarChart, stackedBarChart, lineChart,
  smallLineChart, pieChart, chart (components/widgets/WidgetRenderer.tsx:89-239).
- **Per-type data** (no): chart (chartData{labels,datasets}, chartSubheaderData, isCurrency,
  isYAxisCurrency, height, chartType); statistics (count, countFontSize, countURL, countContext,
  percentageAmount, percentageLabel, percentageURL, increaseIsGood, isCurrency, title;
  statisticsGroupData); table (tableDataList, columns, rows, column type `hidden`, csvData);
  stepper (steps, activeStep); html; alert (alertType, bulletList); fieldValueList (fields,
  fieldIndentLevels, fieldLabelPrefixIconNames/Colors, record); usaMap (mapMarkerList);
  quickSightChart (url, height); parentWidget (childWidgetNameList, layoutType); process
  (processMetaData); customComponent (componentName, componentSourceUrl); childRecordList
  (queryOutput, childTableMetaData, childFrontendTableMetaData, totalRows, viewAllLink,
  canAddChildRecord, defaultValuesForNewChildRecords*, disabledFieldsForNewChildRecords,
  omitFieldNames, onlyIncludeFieldNames, includeExposedJoinTables, disableRowClick, isInProcess);
  dynamicForm (fieldList, recordOfFieldValues, mergedDynamicFormValuesIntoFieldName,
  noFieldsMessage); cronUI (cronDescription, defaultValues.*FieldName); filterAndColumnsSetup
  (tableName, isApiVersioned, hide*, filter*FieldName(s), column(s)FieldName, omitExposedJoins,
  overrideIsEditable); rowBuilder (records, defaultValuesForNewRecords, hiddenValues, modalTitle);
  pivotTableSetup; dataBagViewer; scriptViewer; ESB overview; recordGrid; quickLinks;
  processSummary.
- **Block widget** (partial): blocks[], blockTypeName, layout.
- **Widget metadata** (no/partial): collapsible{isCollapsible,initiallyOpen}; dropdowns[] items
  (name, label, possibleValueSourceName, type, labelForNullValue, width, startIconName,
  allowBackAndForth, backAndForthInverted, disableClearable) and response dropdownNameList,
  dropdownLabelList, dropdownDataList, dropdownDefaultValueList; defaultValues keys;
  icons/helpContent by role.

### 2.2 Process frontend
- FrontendComponent.type missing BULK_LOAD_FILE_MAPPING_FORM, BULK_LOAD_VALUE_MAPPING_FORM,
  BULK_LOAD_PROFILE_FORM (components/process/ProcessComponent.tsx:67-69).
- Values per component (no): bulk load (bulkLoadProfile, suggestedBulkLoadProfile,
  tableStructure, headerValues, headerLetters, bodyValuesPreview, fileBaseName,
  savedBulkLoadProfileRecord, valueMapping*, fieldNamesToDoValueMapping, fileValues,
  mappedValueLabels, savedBulkLoadProfileList); VALIDATION_REVIEW_SCREEN (validationSummary,
  previewMessage, recordCount, supportsFullValidation, formatPreviewRecordUsingTableLayout);
  PROCESS_SUMMARY_RESULTS (processResults[] {status, count, message, primaryKeys, recordId,
  tableName, filter, linkPreText, linkText, linkPostText, bulletsOfText}); DOWNLOAD_FORM
  (downloadFileName, serverFilePath, storageTableName, storageReference); BULK_EDIT_FORM
  (nonDistinctPVSFields, bulkEditEnabledFields); GOOGLE_DRIVE_SELECT_FOLDER
  (googleDriveAccessToken, FolderId, FolderName); step inputFieldList, noMoreSteps; block-input
  autoFocus, placeholder, submitOnEnter.

### 2.3 Field adornments and behaviours
- Adornment type enum missing FILE_UPLOAD, TOOLTIP.
- Adornment values (partial): LINK (toRecordFromTable, toRecordFromTableDynamic, target); CHIP
  (`color.{value}`, `icon.{value}`, colour enum); SIZE (width xsmall..xlarge); FILE_DOWNLOAD
  (fileNameField, downloadUrlDynamic, defaultMimeType); FILE_PREVIEW (thumbnailUrlField,
  fullSizeUrlField, mimeTypeField, fileNameField, thumbnailMaxSize, previewable); TOOLTIP
  (staticText, tooltipDynamic); CODE_EDITOR (languageMode); WIDGET (widgetName).
- displayValues key convention `<field>:tooltipDynamic`, `:toRecordFromTableDynamic`,
  `:downloadUrlDynamic` (no).
- Field behaviours TO_UPPER_CASE / TO_LOWER_CASE / TRIM (partial, free strings).
- Picker sorts item shape {label, fieldName, isAscending} (partial).
- Help content {title, content, format, contentAsHtml, links, roles} (partial).

### 2.4 Metadata hints and settings
- Table `supplementalMetaData.materialDashboard`: gotoFieldNames, defaultQuickFilterFieldNames,
  showRecordSidebar, recordViewActionsPlacement, onLoadFormAdjuster, fieldRules (partial,
  untyped). Field: formAdjusterIdentifier, onChangeFormAdjuster, onLoadFormAdjuster,
  fieldsToDisableWhileRunningAdjusters (partial).
- App home: materialDashboard.showAppLabelOnHomeScreen, includeTableCountsOnHomeScreen,
  dashboardLayout.maxContentWidth (partial).
- Instance: materialDashboard.queryScreenCopyFullQueryColumnValuesLimit (no); `theme`,
  `redirects` (no); branding.customCss, branding.gravatarDefault (no); materialDashboardTheme
  ~120 keys (partial, additionalProperties).
- Sections: iconName, isHidden; tier values `basic`/`advanced` read alongside T1/T2/T3 (no).
- Reports: tableName, isHidden (no).
- environmentValues keys (ANALYTICS_*, GOOGLE_*, POSTHOG_*) (partial, free map).
- Table menu item types/slots/BUILT_IN options (partial, prose only).
- Developer panel block types KEY_VALUES/TEXT/CODE/TABLE/LINKS/ACTIONS and item fields (partial).
- Notifications/alerts, assistant, ESB, SQL Workbench, form-adjuster and sharing payloads: no
  schema at all (off-spec routes).

## 3. Hard-coded behaviour a non-web client could not reproduce from the API

| Next src | Hard-coded | Should come from |
|---|---|---|
| components/widgets/WidgetRenderer.tsx:89-264 | widget type to renderer switch, chart-variant map, shape sniffing (`'count' in data`, `blocks`, `message`, `blockTypeName`) | widget type enum + discriminated `widgetData` schema per type |
| components/process/ProcessComponent.tsx:57-70 | component type to renderer switch (incl. 3 BULK_LOAD types) | FrontendComponent type enum + per-type values schema |
| lib/api/audits.ts:28-106 | `GetAuditsForRecord` process, `audit` table, `auditDetail` join, fallback rule | table capability/link for audits |
| lib/api/developer.ts:35-610; components/scripts/ScriptEditorDialog.tsx:52; ScriptViewerWidget.tsx:40; DataBagViewerWidget.tsx:47 | script/dataBag table and process names (scriptRevision, scriptLog, storeScriptRevision, testScript, storeDataBagVersion, ...) | developer-mode v1 operations or names in the developer response / widget data |
| lib/hooks/use-saved-views.ts:35 | querySavedView / storeSavedView / deleteSavedView | saved-view capability in table metadata |
| lib/api/processes.ts:451-488 | saved bulk-load profile processes and output key | bulk-load profile metadata |
| lib/api/sharing.ts:32-138 | sharing processes and scope names | shareableTableMetaData naming them |
| components/query/ColumnStatsDialog.tsx:38 | `columnStats` process | QUERY_STATS capability naming the operation |
| components/query/ProcessLauncherMenu.tsx:39-90; RecordQueryBulkBar.tsx:54 | `{table}.bulkInsert/bulkEdit/bulkEditWithFile/bulkDelete` convention, labels, icons | table metadata listing bulk processes with roles |
| lib/utils/process-utils.ts:92-105 | `runRecordScript` fallback | always publish processNamesToAddToAllQueryAndViewScreens |
| lib/api/reports.ts:40-46 | `reportFormat` field, default XLSX | report metadata formats + default |
| components/alerts/*; lib/api/alerts.ts:31 | app `alerts`, processes alertsAcknowledge/alertsSnooze, table `alerts_alert`, `/app/notifications` | alerts payload carries actions/hrefs (phase 4 spec) |
| lib/utils/goto-utils.ts:26 and peers | `materialDashboard` supplemental key and sub-keys | typed, frontend-neutral table/field/app settings |
| lib/api/form-adjuster.ts:66-118 | `/material-dashboard-backend/form-adjuster` at server origin | v1 form-adjuster operation |
| lib/api/developer.ts:193 | API-middleware base by stripping `/qqq/v1`; `/apis.json` etc. | discovery links in v1 |
| lib/utils/adornment-utils.ts:26-206 | legacy `/data` URL rewrite; CHIP colour whitelist; `<field>:xxxDynamic` convention; SIZE width table (60..600 px) | backend emits v1 URLs; typed adornment values; semantic sizes |
| lib/utils/file-preview.ts:220-253 | MIME sniffing from file names | mimeType from backend |
| lib/utils/help-utils.ts:25-53 and form components | help role sets per screen | help role enum + screen/role mapping in spec |
| components/records/RecordView.tsx:308; RecordHoverCard.tsx:103; search/RecordPreview.tsx:61; quick-filter-utils.ts:62 | section tier semantics; preview/quick-filter fall back to T1 fields | tier enum + explicit preview / quick-filter field lists |
| lib/utils/record-layout-utils.ts:30,198; record-menu-utils.ts:28-230 | `RECORD_VIEW` key, IN_IDENTITY_SECTION default; menu slots and BUILT_IN mapping | enums + defaults in spec |
| lib/utils/filter-utils.ts:128-164, 672; form-layout.ts:100 | WeekdayOfDate functions; client-applied case behaviours | field function enum; behaviour enum |
| lib/utils/field-validation.ts; zod-from-metadata.ts; ProcessStepScreen.tsx:227-271 | client-side validation and coercion | validation rules in metadata + server validation errors |
| lib/utils/cron-utils.ts:504 | cron parsing/description | cronUI widget data |
| lib/analytics/*; GoogleDriveFolderComponent.tsx:49 | environmentValues magic keys, PostHog default host | typed analytics/integration settings |
| components/assistant/AssistantProvider.tsx:73; lib/api/assistant-probe.ts:38 | presence via supplemental key then probing `/assistant/config` | `GET /version` extensions list + spec'd assistant operations |
| components/widgets/table-widget-utils.ts:44,116; widget-utils.ts:188 | column type `hidden`; `timeframe`/`custom` dropdown names | widget data schema |
| components/widgets/PivotTableSetupWidget.tsx:59-71 | pivotTableJson/columnsJson/tableName/usePivotTable field names | widget defaultValues |

## 4. Routes registered by modules and qbits

Mechanism today: a `QJavalinRouteProviderInterface` registered with
`QJavalinMetaData.withAdditionalRouteProviderReference(...)`, whose `acceptJavalinConfig`
adds raw handlers with a hard-coded `BASE_PATH`. None has an OpenAPI description.

| Repo (commit read) | Routes | Under /qqq/v1? |
|---|---|---|
| qbit-alerts (aede554) | the 7 in 1.2 + `POST /alerts/ingest/alertmanager` (machine webhook, own Authorization check) | yes |
| qbit-assistant (16c8500) | the 10 in 1.2 + `GET /assistant/conversations/{id}` | yes |
| qbit-sql-workbench (7f21e46) | the 20 in 1.2 + `GET /notebooks/{id}/versions/{n}` | yes |
| qqq-esb (qqq ed94d5ccf) | the 5 in 1.2 | yes |
| qqq-middleware-health | `GET /health` (path configurable) | no (ops endpoint, not UI) |
| qqq-middleware-api | `/apis.json`, per-API docs, the published model APIs | no (model API) |
| qqq-middleware-javalin | 32 spec'd v1 operations; docs routes `/qqq/`, `/qqq/versions.json`, `/qqq/v1/openapi.{yaml,json,html}`; `/qqq/branding/{role}`; SPA and static providers; legacy `QJavalinImplementation` (`/metaData`, `/data/...`, `/processes/...`, `/widget/...`, `/possibleValues/...`, `/download/...`, `/reports/...`, `/serverInfo`, `/manageSession`) | v1 + legacy |
| quick-search, cdn, marketing, authentik, web-performance, webhooks, cms, object-storage, backups | none in `src/main` | — |

The `/alerts/ingest/alertmanager` webhook is not a UI operation. In phase 4 it moves to
`/qqq/hooks/alerts/alertmanager` (outside the UI API) with the old path kept as a deprecated
alias for one minor release.

## 5. Targets

| Metric | Before | After phase 4 target |
|---|---|---|
| spec'd calls | 31 | 80 (all) |
| off-spec `/qqq/v1` calls | 43 | 0 |
| legacy / outside-v1 calls | 6 | 0 (documentation links stay plain hrefs) |
