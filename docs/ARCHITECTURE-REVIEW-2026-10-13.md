# Architecture review: UI API only, coupling and cohesion (2026-10-13)

Owner question: "Are we continuing to ensure that all of this works via the UI/UX API only? Also,
keep good design, with cohesion and coupling concerns front of mind."

**Short answer.** The direction is right, but it is not finished:

- Core now has the pieces: the extension point (#1128), client channels (#1141), `UiApiInvoker`,
  the route auditor and contract tests. Next UI has a contract test with an off-spec ratchet, and
  the new qbits (collaboration, data-analysis) build spec'd extensions from day one.
- Two older qbits have no migration PR: alerts and sql-workbench. Between them they hold 27 of
  the 43 off-spec calls.
- Almost every qbit app home is server-rendered HTML, which a CLI, TUI or iOS client cannot use.
- Several stand-ins are already overdue against RC.5.
- Two qbits depend on another qbit's internals.

Sources: QRun-IO/qqq `develop` @ 272a682d9 (RC.5 tag `v4.1.0-RC.5`); qqq-frontend-next `main` @
455d8a5; qbit `develop` heads (alerts 110f15d, assistant 73159e5, authentik 7fb96d7, collaboration
003384a, data-analysis 1b9f502, secured-data 4e74994, easypost-tracking a0e45d5, sql-workbench
316a5d6, quick-search e6f6b66, webhooks ad235f6, cdn 59fc98c, object-storage 4411376, backups
506741b, web-performance 31a33dc, marketing e91c09d, cms c0405ac); KofTwentyTwo/Website-Backend
`develop`; and the open PRs listed in section 6. File references are `repo:path:line` on those
heads.

## 1. Findings by severity

| ID | Sev | Finding | Owner |
|---|---|---|---|
| F1 | High | Qbit app homes are server-rendered HTML, which is not frontend-agnostic | core (typed widgets) + every qbit |
| F2 | High | alerts and sql-workbench still serve raw `/qqq/v1` routes, with no migration PR | qbit-alerts, qbit-sql-workbench |
| F3 | High | Stand-ins are overdue: the core API already shipped in RC.5 | alerts, backups, assistant, object-storage |
| F4 | High | easypost-tracking compiles against qbit-alerts internals (its stand-in SPI) | qbit-easypost-tracking |
| F5 | High | marketing depends on qbit-assistant internals (model provider and pricing) | qbit-marketing, qbit-assistant |
| F6 | High | The Next off-spec ratchet grew on the day it landed | qqq-frontend-next |
| F7 | Med | The alertmanager webhook (a non-UI route) sits under `/qqq/v1` | qbit-alerts |
| F8 | Med | The legacy unversioned API is on by default, and kof22 forces it on | core, kof22 |
| F9 | Med | kof22 gates the UI API with a raw global `before` filter (publisher tokens) | kof22, core |
| F10 | Med | kof22 reaches into qbit-quick-search internals and duplicates its dashboard | kof22, qbit-quick-search |
| F11 | Med | There are four Standard-Webhooks / HMAC signers | webhooks, alerts, cdn |
| F12 | Med | Notifications and follow have three paths and two "watch" concepts | core #1125, alerts, collaboration, easypost |
| F13 | Med | Preferences: assistant adds its own endpoints next to the core contract | qbit-assistant |
| F14 | Med | Per-field exposure has several deciders: AI exposure, secured-data tiers, channel denies and a classification copy | core (secured-data), qbit-assistant |
| F15 | Med | Core `AiToolKernel` data tools are a second, non-UI-API data path | core |
| F16 | Low | Generic core contracts live in packages named after one qbit (`collaboration`, `ai`) | core |
| F17 | Low | The core client-channel default wall hard-codes qbit table prefixes | core |
| F18 | Low | Cross-cutting contracts have no standalone docs | core |
| F19 | Low | Mixed and unpinned core baselines (SNAPSHOT, RC.2, RC.4) | secured-data, authentik, RC.4 qbits |
| F20 | Low | Next hard-codes qbit names (alerts, assistant probe) | qqq-frontend-next, qbit-alerts |

### F1 (High): qbit homes are server-rendered HTML
**Evidence.** Each of these returns `RawHTML` or declares `WidgetType.HTML`, often with inline
styles:
- qbit-alerts `home/AlertsHome.java:119,133,162`;
- qbit-backups `app/BackupsApp.java:64,66,110`;
- qbit-cdn `app/CdnWidgets.java:65,67,132`;
- qbit-web-performance `metadata/WebPerfMetaData.java:113-116`;
- qbit-assistant `home/AssistantAccessWidget.java:75,158,173`;
- qbit-collaboration `app/CollaborationHome.java:98,246`;
- qbit-data-analysis `app/DataAnalysisHome.java:69,101`.

The same pattern is in authentik (`app/AuthentikHomeWidget`, `policy/PolicyCutoverWidgets`),
easypost (`app/TrackingHtml`), sql-workbench (`home/HomeHtml`), quick-search
(`diagnostics/DiagnosticsHtml`), webhooks (`app/WebhooksHomeView`), object-storage
(`app/StorageWidgets`) and marketing (`widgets/MarketingWidgets`). That is 14 of the 16 qbits.

**Why it matters.** `/qqq/v1` is spec'd, but what it carries is web markup. A CLI, TUI or iOS
client gets an HTML string it cannot render. This is the largest remaining gap against the
"frontend-agnostic" goal. It also bypasses Next's theming: the inline `opacity` and `font-size`
styles are why some homes look off in dark mode.

**Recommendation.**
- Core (UI-API-AUDIT phase 5): publish discriminated `widgetData` schemas for the data widgets
  (statistics, multiStatistics, table, fieldValueList, quickLinks, chart, childRecordList).
- Each qbit: rebuild its home from those types.
- Ban new `WidgetType.HTML` / `RawHTML` in qbits (guardrail G5). HTML stays only for content that
  really is HTML, such as CMS bodies and email previews, through an allow-list.

### F2 (High): raw `/qqq/v1` routes with no migration PR
**Evidence.**
- qbit-alerts `api/AlertsRouteProvider.java:57,59,100-107`: 7 UI routes plus the ingest webhook.
  No open PR.
- qbit-sql-workbench `api/SqlWorkbenchRouteProvider.java:57`: 21 routes. No open PR.
- In flight: qbit-assistant #19 (draft, needs RC.6 for `UiApiInvoker`) and qqq #1129 (ESB, open).
- Next lists all of them as off-spec: `src/lib/api/openapi/ui-api-calls.ts:38-60`.

**Recommendation.** Open the alerts and sql-workbench extension PRs now: specs, a committed
fragment and a contract test, following collaboration #8 and data-analysis #5. Merge #1129 and
release assistant #19 with RC.6. These four are the whole blocker list for the strict-mode flip
(section 5).

### F3 (High): stand-ins overdue against RC.5
`QAlerts` (#1108), `WebhookSigner` (#1109) and `S3ClientFactory` (#1095) are in the
`v4.1.0-RC.5` tag. These still carry copies or reflective bindings:

| Repo | Stand-in | Core replacement | Due |
|---|---|---|---|
| qbit-alerts | `spi/` package (`package-info.java:19-25`, `AlertsInstanceMetaData.java:39` "alertsQBitStandIn", `NotificationSignal`/`Severity`/`Category`/`Audience`) | `core.alerts` (RC.5) + `QAlerts.notify` (#1125) | now / #1125 |
| qbit-alerts | `delivery/channels/StandardWebhookSigner.java:31-36` | `core.utils.webhooks.WebhookSigner` (RC.5) | now |
| qbit-backups | `alerts/AlertsQBitAlertSink.java:36-47` (reflective) | `QAlerts` (RC.5) | now |
| qbit-assistant | `engine/BudgetAlerts.java:31-33` (reflective) | `QAlerts` (RC.5) | now |
| qbit-object-storage | `internal/ConnectionRuntime.java:67-69` | `S3ClientFactory` (RC.5) | now |
| qbit-backups | `manifest/TableFingerprints.java:36`, `run/TableReader.java:42`, `restore/StandardRestoreWriter.java:50` | `TableSchemaFingerprint`, `KeysetPager` (develop) | RC.6 |
| qbit-collaboration | `security/ParentRecordAccess.java:42`, `UserSessionSupplier.java:29`, `OtherUserAccess.java:37`, `comments/CommentsService.java:360`, `model/CollaborationTables.java:56` | `actions.collaboration.*`, `TableFeaturePermissionsMetaData` (develop) | RC.6 |
| qbit-collaboration | `notify/Notifications.java:36`, `CollaborationNotifier.java:26` | `QAlerts.notify` (#1125, open) | #1125 RC |
| qbit-data-analysis | `engine/StandInAnalysisBackend.java:63`, `permissions/DataAnalysisPermissions.java:54` | aggregate operators (#1139), `TableFeaturePermissionsMetaData` (develop) | RC.6 (PR #5) |
| qbit-assistant | `uiapi/LoopbackUiApiGateway.java:45`, `classification/DataClassification.java:28` | `UiApiInvoker` (#1128), `actions.ai.DataClassification` (#1142) | RC.6 (PR #19) |
| qbit-object-storage | `internal/SecretRefs.java:39` (`${file.PATH}`) | `QMetaDataVariableInterpreter` (no core PR found) | open a core PR |

The root cause is the baseline: alerts, assistant, backups and object-storage still build on
4.1.0-RC.4. **Recommendation:** move them to RC.5 and delete the "now" rows this week. Do the
RC.6 rows in the same PR that bumps to RC.6. Guardrail G4 then stops this from recurring.

### F4 (High): easypost-tracking compiles against qbit-alerts internals
**Evidence.**
- qbit-easypost-tracking `support/EasyPostNotifications.java:107-138` uses
  `com.kingsrook.qbits.alerts.AlertsRuntime` and `com.kingsrook.qbits.alerts.spi.*`, the stand-in
  package from F3.
- `pom.xml` declares `qbit-alerts 0.1.0-RC.2`, optional.

When alerts deletes its stand-in package, easypost breaks. A qbit should never need another qbit's
runtime class to send a notification.

**Recommendation.** Call core `QAlerts.notify` (#1125) only, and drop the qbit-alerts dependency.

### F5 (High): marketing depends on qbit-assistant internals
**Evidence.** qbit-marketing compiles against `qbits.assistant.engine.ModelPricing` and
`qbits.assistant.provider.*`:
- `MarketingQBitConfig.java:24-25`
- `agent/MarketingAgentService.java:28-39`
- `agent/provider/OpenAiCompatibleModelProvider.java:33-37`

The `qbit-assistant` dependency is compile scope (`pom.xml`). So installing marketing installs the
whole assistant, and marketing carries its own OpenAI-compatible provider next to
`qbit-assistant-provider-openai`.

**Recommendation.**
- Split the provider SPI (`provider.*`, `ModelPricing`) into a small published module,
  `qbit-assistant-provider-api`, that holds no runtime, tables or routes.
- Marketing depends on that module only and reuses the published OpenAI provider.
- Longer term, the provider SPI belongs next to core's `actions.ai`.

### F6 (High): the Next off-spec ratchet grew on day one
**Evidence.**
- `ui-api-calls.ts:17-23` says the list "can only shrink".
- Commit 248bca9, the same day, added `GET /assistant/actions/{}` and the `confirm` and `reject`
  calls for the assistant write mode (qbit-assistant #21).
- Next #105 (favorites) fails unit tests because it calls collaboration operations whose fragment
  is not vendored yet (collaboration #8 is a draft).

**Recommendation.**
- The contract test asserts a fixed ceiling (`OFF_SPEC_CALLS.length <= 43`), lowered as owners
  migrate.
- Add a CODEOWNERS rule on `ui-api-calls.ts`.
- A feature PR in Next lands only after its qbit's extension fragment is vendored. Merge order:
  qbit extension PR, then fragment vendored into Next, then the UI PR.

### F7 (Med): a machine webhook inside the UI API
**Evidence.** qbit-alerts `AlertsRouteProvider.java:107` registers
`POST /qqq/v1/alerts/ingest/alertmanager`.

easypost does this correctly: `EasyPostTrackingQBitConfig.java:54,95-98` uses
`/qqq/hooks/easypost` and rejects any `/qqq/v1` path.

**Recommendation.** Move the webhook to `/qqq/hooks/alerts/alertmanager`, keeping the old path as
a deprecated alias for one minor release (UI-API-AUDIT section 4).

### F8 (Med): the legacy unversioned API is on by default
**Evidence.**
- `QApplicationJavalinServer.java:97`: `serveLegacyUnversionedMiddlewareAPI = true`.
- kof22 `Server.java:100` sets it to true explicitly.
- #1130 adds deprecation headers but sets no date.
- Next still needs two legacy families:
  - form-adjuster `/material-dashboard-backend/form-adjuster`, which has no v1 spec on develop;
  - API discovery `/apis.json` and `versions.json`.

The legacy routes are a second, unspec'd way in to the same data.

**Recommendation.**
- Spec `POST /table/{t}/formAdjuster/{id}/{event}` and the discovery links (audit phase 4).
- Default the legacy API to `false` in the release after that.
- kof22 sets `false` as soon as Next stops calling form-adjuster.

### F9 (Med): kof22 gates the UI API with a raw global filter
**Evidence.** kof22 `metadata/autoload/auth/PublisherTokenRouteProvider.java:54-77` installs
`config.routes.before`/`after` on every route:
- It 403s any `Authorization` request that is not on a hard-coded list of `/qqq/v1` paths (`allows`
  at `:87`), or that also carries a session cookie.
- It strips `Set-Cookie`.

This is application auth policy applied to `/qqq/v1` outside the spec. A future iOS or CLI client
using bearer tokens would get 403, and so would any caller that forwards both a cookie and an
`Authorization` header.

**Recommendation.** Add a core concept of a **scoped credential**: an API key or token whose
allowed operations are a list of spec `operationId`s, checked in the version's `preExecute`. Move
kof22's list into metadata. Until then, keep the filter but name its paths by spec template, and
cover it with a test that fails when a listed path leaves the spec.

### F10 (Med): kof22 reaches into qbit-quick-search internals
**Evidence.**
- kof22 `metadata/adminhome/LiveAdminHomeProbes.java:36,396` uses `QuickSearchOpenSearchClient`
  from the runtime.
- `metadata/adminhome/QuickSearchHomeWidgets.java` and `AdminOperationsWidgets.java` rebuild the
  quick-search health view that the qbit already owns
  (`diagnostics/QuickSearchDiagnosticsWidgetRenderer`).
- Reading the qbit's entity classes (`quicksearch.model.*`) is fine: those are its published tables.

**Recommendation.**
- The qbit publishes a status API (`QuickSearchStatus`, or a widget the host links to).
- kof22 deletes its copies and links to the Quick Search home ("one landing, no duplicate
  dashboards").

### F11 (Med): four webhook signers
**Evidence.**
- core `utils/webhooks/WebhookSigner` (RC.5);
- qbit-webhooks `shared/signing/WebhookSigner.java`, plus its `WebhookVerifier` and
  `WebhookSecret`;
- qbit-alerts `StandardWebhookSigner.java` (the stand-in);
- qbit-cdn `purge/RevalidationHookRunner.java:195-196` (ad-hoc HMAC-SHA256).

**Recommendation.** All four use core's signer and verifier, so there is one format, one rotation
rule and one SSRF-safe client. qbit-webhooks keeps endpoints, secrets and delivery; signing moves
to core.

### F12 (Med): notifications and follow
**Evidence.** There are three notification paths today:
- qbit-alerts `spi.NotificationSignal` (stand-in);
- collaboration `notify/Notifications.java:36` (stand-in);
- core `QAlerts.notify` (#1125, open).

There are also two "interest in a record" concepts:
- alerts `POST /alerts/record-panel/watch` (`AlertsRouteProvider.java:106`, `alerts_subscription`);
- `collaboration_follow` (COLLABORATION.md:283), with reasons MANUAL, MENTIONED, ASSIGNED and so
  on.

**Recommendation, single owners:**
- **Core `QAlerts.notify`** is the only producer API for user notifications. qbit-alerts
  implements delivery: inbox, channels, quiet hours and preferences.
- **qbit-collaboration** owns *following a record* and mentions.
- **qbit-alerts** owns *subscriptions to alert types and scopes*.

The alerts record panel shows the alerts on a record. Its "watch" button writes a collaboration
follow when collaboration is installed, and is hidden otherwise. It does not keep a second follow
table.

### F13 (Med): preferences
**Evidence.**
- The core contract is in progress: `docs/USER-PREFERENCES.md`, uncommitted in `qqq-wt-prefs`.
  Sections, a provider SPI and `GET/PATCH /qqq/v1/preferences`.
- qbit-alerts already fits: `alerts_user_preference` and `alerts_notification_preference` become
  provider-backed sections.
- qbit-assistant #21 adds `/assistant/preferences` endpoints and an `assistant_user_preference`
  table.

**Recommendation.**
- The assistant registers a preferences section with a provider over its table, and drops its own
  endpoints.
- Rule: per-user settings are a preferences section; per-qbit admin settings stay as the qbit's
  config table.
- Land the core preferences PR before #21.

### F14 (Med): per-field exposure has several deciders
**Evidence.**
- core `actions/ai/exposure/AiExposurePolicy`, `AiValueMasker`, `AiFieldAccess`;
- `FieldSecurityLock` / `FieldSecurityLockHelper`;
- the secured-data tiers (SECURED-DATA.md: `FieldAccessHelper`, MASK/REVEAL behaviours,
  per-channel policy rows);
- client-channel denies (whole objects only);
- qbit-assistant's own `classification/DataClassification.java` copy.

SECURED-DATA.md already intends to unify these but keeps `AiValueMasker` separate.

**Recommendation.**
- `FieldAccessHelper` (from #1147) is the **one** per-field decision point for every client,
  including the assistant.
- Inputs: lock, classification, role tier, client channel.
- Outputs: FULL, REVEALABLE, MASKED or HIDDEN.
- `AiExposure` becomes a default policy keyed on channel `assistant`, an input to that decision
  rather than a second masker.
- `ClientChannels` stays object-level only.
- Delete the assistant's `DataClassification` copy (F3).

### F15 (Med): `AiToolKernel` is a second data path
**Evidence.** Core `actions/ai/tools/QueryRecordsTool.java:158` calls `QueryAction` directly, and
so do `GetRecordTool`, `CountRecordsTool` and `RenderWidgetTool`.

qbit-assistant uses the kernel only for `navigate` (`engine/ContextBuilder.java:57`); its data
tools go through `UiApiGateway`. But the kernel's data tools ship in core, and any future AI
consumer (MCP, marketing) could use them and bypass `/qqq/v1`.

**Recommendation.** Either reimplement the kernel data tools on `UiApiInvoker`, or deprecate them
in RC.6 and remove them before GA. Then "AI only through `/qqq/v1`" holds by construction.

### F16 (Low): generic contracts in qbit-named core packages
**Evidence.**
- `core/actions/collaboration/` holds `RecordAccessCheckerInterface`,
  `RecordReferenceSecurityHelper`, `UserSessionProviderInterface` and `RecordFeature*`. These are
  used beyond collaboration (alerts recipient checks, data-analysis, secured-data).
- `DataClassification` lives in `actions/ai`, but secured-data uses it for every client.

**Recommendation.** Before 4.1.0 GA, while moving them is still cheap:
- move the first group to `actions.records.references` and `actions.records.features`;
- move `DataClassification` to `model.metadata.security`.

### F17 (Low): the core default wall names qbit prefixes
**Evidence.** UI-API-CLIENT-CHANNELS.md:229-239 builds `assistant_*`, `authentik_*` and
`webhooks_*` into core.

**Trade-off.** This is fail-closed, which is good. But core now knows qbit names.

**Recommendation.**
- Keep the generic patterns in core (`*secret*`, `*permission*`, `audit*` and so on).
- Each qbit registers its own denies in its producer.
- A validator warns when an installed qbit has tables matching `*_policy`, `*_secret` or
  `*_config` and registers no channel rule.

### F18 (Low): contract docs
`TableFeaturePermissions`, record references and features, and the notifications SPI are
documented only inside COLLABORATION.md and DATA-ANALYSIS.md. See the section 2 table.
**Recommendation:** one `docs/CONTRACTS.md` index that links one doc per contract.

### F19 (Low): baselines
qbit-secured-data `develop` builds on `4.1.0-SNAPSHOT`, which is not reproducible. qbit-authentik
is on RC.2. Ten qbits are on RC.4. **Recommendation:** forbid SNAPSHOT on `develop`, and bump
every qbit to the latest RC within a week of the RC (guardrail G7).

### F20 (Low): Next hard-codes qbit names
Next hard-codes:
- the `alerts` app, the `alertsAcknowledge` and `alertsSnooze` processes, `alerts_alert` and
  `/app/notifications` (`lib/api/alerts.ts:31`, `components/alerts/*`);
- the assistant presence probe (`lib/api/assistant-probe.ts:38`).

Already listed in UI-API-AUDIT section 3. **Recommendation:** the alerts extension payloads carry
actions and hrefs, and presence comes from `GET /version` extensions.

## 2. Shared contracts in core

| Contract | Where | One owner? | Doc | Overlap |
|---|---|---|---|---|
| UI API extensions | `middleware/javalin/specs/extensions` | core UI API | UI-API-EXTENSIONS.md | none |
| `UiApiInvoker` | `specs/invoke` | core UI API | UI-API-EXTENSIONS.md section 5 | `AiToolKernel` data tools (F15) |
| Client channels | `ClientChannelsMetaData`, `PermissionsHelper` | core | UI-API-CLIENT-CHANNELS.md | qbit names in the default wall (F17) |
| `TableFeaturePermissions` | `model/metadata/permissions` | core | none of its own (COLLABORATION, DATA-ANALYSIS) | none; `RecordFeaturePermissionsRegistrar` correctly adapts onto it |
| Record reference and features | `actions/collaboration/*` | core | COLLABORATION.md | package name (F16) |
| Field access tiers | `FieldAccessHelper` (#1147 spec) | core | SECURED-DATA.md | `AiExposurePolicy`/`AiValueMasker`, `FieldSecurityLock` (F14) |
| `DataClassification` | `actions/ai` | core | SECURED-DATA.md, #1142 | assistant copy (F3), package (F16) |
| Preferences | `actions/preferences` (in progress) | core | USER-PREFERENCES.md (unmerged) | assistant #21 endpoints (F13) |
| Alerts producer (`QAlerts`) | `core/alerts` (RC.5) | core; qbit-alerts implements | changelog + alerts spec | stand-ins (F3) |
| Notifications (`QAlerts.notify`) | #1125 (open) | core; qbit-alerts implements | none yet | alerts and collaboration stand-ins (F12) |
| Webhook signing | `utils/webhooks` (RC.5) | core | webhook signing doc | qbit-webhooks, alerts, cdn copies (F11) |
| Object storage | `qbits.objectstorage.api` | qbit-object-storage | its README | none. This is the right shape for a qbit-published SPI |

## 3. Cohesion: one owner per concern

| Concern | Owner | Others |
|---|---|---|
| User notifications (produce) | core `QAlerts.notify` | every qbit calls it; none depends on qbit-alerts |
| Notification delivery, inbox and channels | qbit-alerts | |
| Following a record, mentions | qbit-collaboration | alerts record panel links to it |
| Alert-type subscriptions | qbit-alerts | |
| AI budget limits | qbit-assistant (enforces) | raises budget alerts via `QAlerts` only |
| Per-user settings | core preferences (sections + providers) | qbits provide sections |
| Per-qbit admin settings | the qbit's config table | |
| Search (find a record) | qbit-quick-search via core `/search` provider | |
| Profiles and statistics | qbit-data-analysis | no overlap with search found; data-analysis must not build its own index |
| Field visibility for every client | core `FieldAccessHelper` | secured-data supplies policy; AI exposure is a policy input |
| Object-level client denies | core client channels | |
| Webhook signing | core | qbit-webhooks owns endpoints and delivery |
| Object storage | qbit-object-storage `api` | backups and collaboration consume it (fine) |

## 4. Dependency graph (qbit to qbit, `develop`)

```
easypost-tracking --(optional, INTERNALS)--> alerts        (F4: remove)
marketing --------(compile, INTERNALS)----> assistant      (F5: narrow to a provider-api module)
backups ----------(optional, api pkg)-----> object-storage (OK)
collaboration ----(compile, api pkg)------> object-storage (OK)
```

There are no cycles. No other qbit references another qbit's packages
(`git grep com.kingsrook.qbits.<other>`). Core has no qbit dependency.

## 5. Guardrails to add

| # | Check | Where | When |
|---|---|---|---|
| G1 | **Dependency rule.** `maven-enforcer` `bannedDependencies` bans `com.kingsrook.qbits:*`, except an allow-list of published API artifacts (`qbit-object-storage`, a future `qbit-assistant-provider-api`). An ArchUnit rule: `com.kingsrook.qbits.<x>..` may depend only on itself, `com.kingsrook.qqq..`, the JDK and libraries, and allow-listed `com.kingsrook.qbits.<y>.api..` packages. Also no `Class.forName("com.kingsrook.qbits.` of another qbit. | `qbit-build-parent` (shared rule + test-jar) | now |
| G2 | **Strict raw routes.** Every qbit's server test starts `QApplicationJavalinServer` with `withStrictUiApiRoutes(true)`. Core flips the default to strict in **4.1.0-RC.7, target 2026-10-27**, once alerts, sql-workbench, assistant #19 and ESB #1129 are released. kof22 sets `QQQ_UI_API_STRICT_ROUTES=true` in CI now. | qbits, core, kof22 | CI now; default RC.7 |
| G3 | **Extension contract test.** Every qbit that registers a `MiddlewareSpecExtension` commits `src/main/resources/openapi/qqq-v1/{name}.yaml` and runs `UiApiContractAssertions.assertExtensionMatchesCommitted`. A parent-POM test fails when an extension is registered with no committed fragment. | `qbit-build-parent`, each qbit | now |
| G4 | **No stand-ins after RC.N.** Mark each stand-in `STAND-IN(core=QRun-IO/qqq#NNNN, removeBy=4.1.0-RC.N)`. A build check (enforcer or exec script) fails when `qqq.version >= removeBy`. A stand-in without a `removeBy` fails. | `qbit-build-parent` | now |
| G5 | **No HTML widgets in qbits.** ArchUnit: no use of `RawHTML` or `WidgetType.HTML` outside an allow-list (CMS content, email preview). | `qbit-build-parent` | after the core typed widgets (F1) |
| G6 | **Next ratchet ceiling.** `ui-api-contract.test.ts` asserts `OFF_SPEC_CALLS.length <= N`, with N lowered as owners migrate. CODEOWNERS on `ui-api-calls.ts`. | qqq-frontend-next | now |
| G7 | **Pinned baseline.** Enforcer `requireReleaseDeps` for `com.kingsrook.qqq` on `develop` (no SNAPSHOT); a monthly check that the core RC is current. | `qbit-build-parent` | now |
| G8 | **Legacy off.** `serveLegacyUnversionedMiddlewareAPI` defaults to false in the release after the form-adjuster and discovery specs (F8). | core | RC.7 or RC.8 |

## 6. Open PRs reviewed

| Repo | PRs |
|---|---|
| qqq | #1148, #1144, #1143, #1130, #1129, #1125, #1085 (#1147 merged during the review) |
| qqq-frontend-next | #105, #104, #103 |
| qbit-assistant | #21, #19 |
| qbit-collaboration | #9, #8 |
| qbit-data-analysis | #6, #5 |
| qbit-quick-search | #40 |
| qbit-cms | #3 |
| kof22 | #160, #141, #139, #138, #137, #136, #135, #134 |

The new extension PRs (collaboration #8, data-analysis #5, assistant #19) follow the pattern
correctly: specs, a committed fragment and a contract test. No open PR adds a new raw `/qqq/v1`
route provider.
