# Changelog

All notable changes to QQQ will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- **Next dashboard by default ([#649](https://github.com/QRun-IO/qqq/issues/649))** — `QApplicationJavalinServer`
  serves the `com.kingsrook.qqq:qqq-frontend-next` dashboard at `/` when its jar is on the classpath
  (`NextDashboardRouteProvider`; API routes and their own 404 responses are never shadowed). `qqq-bom-pom`
  manages `qqq-frontend-next`. New `withServeFrontendNext(boolean)` and JVM property
  `qqq.javalin.frontend` (`next`, `material` or `none`).
- **v1 branding ([#539](https://github.com/QRun-IO/qqq/issues/539))** — `GET /qqq/v1/metaData` includes the
  instance `branding` (names, logo, icon, accent colors, banners). The published v1 OpenAPI document was
  regenerated, which also brings in previously unpublished schema additions (associations, OAuth2
  authentication values, back-channel logout).
- **Fail-fast meta-data producers ([#763](https://github.com/QRun-IO/qqq/issues/763))** — with
  `QInstance.withFailOnMetaDataProducerError(true)` or JVM property `qqq.metaData.failOnProducerError=true`,
  `MetaDataProducerHelper` throws a `QException` when a class fails while being evaluated as a producer, or a
  producer fails to run, instead of logging a warning and skipping it. `QBitMetaDataProducer` honors the instance flag for its
  own producers. The default (warn and skip) is unchanged.

### Changed
- The sample application and quickstart serve the Next dashboard from the sample itself on port 8000;
  the quickstart no longer needs Docker. `QQQ_FRONTEND=material bash quickstart.sh` opens the Material
  Dashboard. The sample's branding icon now lives in its overlay folder.

### Compatibility
- Applications that depend only on `qqq-frontend-material-dashboard` keep serving it at its configured
  path, unchanged. With both dashboard jars present, Next is served unless Material is selected
  explicitly; `withServeFrontendMaterialDashboard(true)` alone keeps Material (Next is then not served
  at the same root). Hosting both requires Material at another path. Deep links differ: Material uses
  `/<app>/<table>/<id>`, Next uses `/app/<table>/<id>`.

## [4.0.0] - 2026-09-24

### Breaking Changes

**Package renames:**
- **BREAK-01 (qqq-middleware-javalin)** — `com.kingsrook.qqq.backend.javalin.*` renamed to
  `com.kingsrook.qqq.middleware.javalin.*`. All 16 classes (including `QJavalinImplementation`,
  `QJavalinMetaData`, `QJavalinProcessHandler`, `QJavalinScriptsHandler`, `QJavalinAccessLogger`,
  `QJavalinUtils`, etc.) now live under the new root package. Update all import statements.
  See [Migration Guide](docs/migration/4.0.adoc#javalin-imports).
- **BREAK-02 (qqq-middleware-picocli)** — `com.kingsrook.qqq.frontend.picocli.*` renamed to
  `com.kingsrook.qqq.middleware.picocli.*`. Affected classes: `QPicoCliImplementation`,
  `QCommandBuilder`, `PicoCliProcessCallback`. If your `pom.xml` has a `<mainClass>` referencing
  `frontend.picocli`, update that as well. See [Migration Guide](docs/migration/4.0.adoc#picocli-imports).

**Removed deprecated APIs (BREAK-03):**
- **BREAK-03-A** — `QBackendMetaData` deprecated elements removed: 2 private fields
  (`variantOptionsTableTypeField`, `variantOptionsTableTypeValue`), 20 deprecated
  `variantOptionsTable*` accessors, and the `LegacyBackendVariantSetting` enum.
  See [Migration Guide](docs/migration/4.0.adoc#removed-deprecated-apis).
- **BREAK-03-B** — Miscellaneous zero-caller deprecated elements removed from 5 files:
  `ExtractViaQueryStep.customizeInputPreQuery(QueryInput)` 1-arg overload,
  `QRecordListMetaData.addField()`, `QRecordEntity.toQRecordOnlyChangedFields()` 0-arg overload,
  `BaseAPIActionUtil.setSession()`, and `ApiFieldCustomValueMapper.customizeFilterCriteria()` vararg
  overload.
- **BREAK-03-C** — `qqq-openapi`: `Parameter.setIn(String)` / `withIn(String)` and
  `Schema.setType(String)` / `withType(String)` type-narrowed from `String` to enum.
  Use the `com.kingsrook.qqq.openapi.model.In` and `Type` enum overloads that already existed.

**API-shape cleanups (BREAK-04):**
- **BREAK-04-01** — `qqq-bom`: Added 5 previously-unmanaged modules
  (`qqq-backend-module-sqlite`, `qqq-backend-module-postgres`, `qqq-middleware-lambda`,
  `qqq-middleware-health`, `qqq-utility-lambdas`). Update your BOM import; remove any manual
  version pins for these modules.
- **BREAK-04-02** — `qqq-utility-lambdas` promoted from Java 11 to Java 21 compiler target.
- **BREAK-04-03** — `qqq-middleware-javalin` annotation-processor compiler block now inherits
  the Java 21 compiler target; the former Java 11 override is removed.
- **BREAK-04-04** — Qodana static analysis aligned from JDK 17 to JDK 21.
- **BREAK-04-06** — `JsonUtils.toJson(Object, Consumer<ObjectMapper>)` removed.
  Use `toJsonCustomized(Object, Consumer<JsonMapper.Builder>)` for builder-level config or
  `toJsonWithMapper(Object, Consumer<JsonMapper>)` for post-build mapper config.
- **BREAK-04-07** — `YamlUtils.toYaml(Object, Consumer<ObjectMapper>)` removed.
  Use `toYamlCustomized(Object, Consumer<YAMLMapper.Builder>)`.
- **BREAK-04-08** — `AbstractActionInput.getInstance()` and `getSession()` removed.
  Use `QContext.getQInstance()` and `QContext.getQSession()`.
- **BREAK-04-09** — `QQueryFilter.interpretValues(FilterUseCase, Map)` 2-arg overload removed.
  Use `interpretValues(Map, FilterUseCase)` with a map of named value maps.
- **BREAK-04-10** — `NowWithOffset.minus/plus(int, TimeUnit)` factories removed.
  Use `NowWithOffset.minus/plus(int, ChronoUnit)`.
- **BREAK-04-11** — `QInstance.setAuthentication(QAuthenticationMetaData)` removed.
  Use `qInstance.registerAuthenticationProvider(AuthScope.instanceDefault(), authMetaData)`.
  Add import: `com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope`.
- **BREAK-04-12** — `QInstance.metaDataFilter` property and accessors removed.
  Use `qInstance.setMetaDataActionCustomizer(codeRef)`.
- **BREAK-04-13** — `QBrandingMetaData.environmentBannerText` / `environmentBannerColor`
  fields and accessors removed. Use `withBanner(slot, new Banner().withMessageText(...))` with a frontend-supported `BannerSlot`.
- **BREAK-04-14** — `QProcessMetaData.addStep(QStepMetaData)` and `addStep(int, QStepMetaData)`
  removed. Use `withStep(step)` and `withStep(index, step)`.
- **BREAK-04-15** — `QProcessMetaData.addOptionalStep()` removed. Use `withOptionalStep(step)`.
- **BREAK-04-16** — `QFunctionInputMetaData.addField()` removed. Use `withField(field)`.
- **BREAK-04-17** — `AbstractHTMLWidgetRenderer` input-taking `linkRecordEdit`, `linkProcessForFilter`,
  and `linkProcessForRecord` overloads removed.
  Use the 2-arg forms without the `input` parameter.
- **BREAK-04-18** — `RecordCustomizerUtilityInterface.getValueFromRecordOrOldRecord()` removed.
  Use `getValueFromRecordElseFromOldRecord()` with `ValueUtils` for type conversion.
- **BREAK-04-19** — `RecordAutomationHandler` abstract class deleted.
  Implement `RecordAutomationHandlerInterface` directly.
- **BREAK-04-20** — `AllowAllMetaDataFilter` class and `MetaDataFilterInterface` deleted.
  Implement the `metaDataActionCustomizer` `CodeReference` pattern instead.
- **BREAK-04-21** — `RenderTemplateAction.renderVelocity(ActionInput, ...)` overloads removed.
  Use the overloads without the `actionInput` parameter.
- **BREAK-04-22** — `BaseAPIActionUtil.executeOAuthTokenRequest(CloseableHttpClient, HttpPost)`
  overload removed. Use the generic `HttpRequestBase` overload.
- **BREAK-04-23** — `AbstractBaseFilesystemAction.writeFile(backend, path, contents)` 3-arg
  overload removed. Use `writeFile(backend, table, record, path, contents)`.
- **BREAK-04-24** — `QApplicationJavalinServer.withAdditionalRouteProvider(instance)` removed.
  Use `withAdditionalRouteProviders(List.of(instance))`.
- **BREAK-04-25** — `ApiQueryFilterUtils.manageCriteriaFields(...)` deprecated 5-arg overload
  removed. Use the 6-arg form with `apiVersion`.
- **BREAK-04-26** — `GetTableApiFieldsAction.getTableApiFieldMap/getTableApiFieldList(ApiNameVersionAndTableName)`
  overloads removed along with the `ApiNameVersionAndTableName` inner record.
  Use `GetTableApiFieldsInput` directly.

### Fixed

- Material parent widgets skip child metadata hidden by permissions, preserving permitted children and the surrounding dashboard ([#558](https://github.com/QRun-IO/qqq/issues/558)).

- Application API custom process permission checks now see mapped/defaulted/customized inputs before execution. This closes a configured named-report authorization bypass ([#455](https://github.com/QRun-IO/qqq/issues/455)); standard denial remains before pre-run customization.

- Metadata directory loading now rejects missing directories, invalid properties and mapping errors instead of reporting partial success. Packaged sample launchers share the same guarded H2 bootstrap and bundled Person YAML.
- Isolated SPA authentication rejection stops endpoint execution, including requests to the bare protected path.
- `DynamicDefaultValueBehavior.USER_ID` applies only during insert/update; reading a stored null no longer fabricates the current reader's ID.
- PicoCLI now runs with its own authenticated session and restores the caller's context on completion or failure. Separate CLI instances no longer share static instance/session state.
- String truncation preserves supplementary Unicode characters at UTF-16 boundaries and handles ellipsis limits shorter than the suffix.
- The sample CLI initializes its disposable H2 data in a fresh process. Sample reset refuses any other database, including a cached connection provider with the same backend name; obsolete external MySQL reset configuration and unused Liquibase bootstrap/dependencies were removed.

- `QBackendMetaData.withoutCapabilities(Set<Capability>)` now disables the supplied capabilities, matching the varargs overload. Previously it enabled them, so tables could retain operations the backend configuration intended to exclude.

**Resolved bugs:**
- **#331** — `MockAuthenticationModule` now calls `customizeSession()` when a session customizer
  is configured, consistent with `Auth0AuthenticationModule` and `OAuth2AuthenticationModule`.
  Applications using MockAuth in dev mode to pin user IDs or set security keys now work correctly.
- **#357** — Child record list widgets now default to `NOT_PROTECTED` permission rules, fixing
  a visibility defect where child record list tabs were invisibly filtered out when the QInstance
  had restrictive default permission rules.

- Endpoint specifications with no tag now generate their OpenAPI method without a null-pointer
  exception. Tags remain optional; tagged endpoints retain their existing behavior.
- Invalid sharing scopes now identify the rejected input in the validation message instead of
  reporting `[null]`.

### Security

- Cron widgets now require source-table READ permission and preserve caller personalization when loading a record; expression-only descriptions and trusted internal calls retain their behavior ([#563](https://github.com/QRun-IO/qqq/issues/563)).

- Child-record widgets now enforce parent, child and joined-table READ rules for user requests and keep paginated totals within personalized record visibility ([#561](https://github.com/QRun-IO/qqq/issues/561)).

- No-code query/count widget values now preserve caller context and enforce source-table and joined-table READ rules ([#557](https://github.com/QRun-IO/qqq/issues/557)); trusted internal SYSTEM calls retain their existing behavior.

- Align Jackson modules on 2.21.5 and Netty modules on 4.1.137.Final through their BOMs.
- Update Log4j to 2.25.5, jsoup to 1.23.1, PostgreSQL JDBC to 42.7.12,
  c3p0 to 0.14.0, mchange-commons to 0.6.0, and Plexus Utils to 4.0.3.
- Update affected test dependencies: AssertJ 3.27.7, HttpClient 5.6.3,
  HttpCore 5.4.3, and Handlebars 4.5.2.
- Migrate the HTTP stack to Javalin 7.2.3 and Jetty 12.1.13, aligned through
  the Jetty core and EE10 BOMs.

### Known Issues / Deferred Bugs

- [#562](https://github.com/QRun-IO/qqq/issues/562): the current local Material sample grid displays a missing-license watermark (Low; deferred). Grid data and navigation work; public deployment license state is not established by this local observation.

- [#560](https://github.com/QRun-IO/qqq/issues/560): the Material US map displays fixed demo locations and ignores supplied markers (Medium; deferred).

- [#559](https://github.com/QRun-IO/qqq/issues/559): parent-widget tabs do not restore the saved selection after refresh (Low; deferred). Live tab switching works.

- [#550](https://github.com/QRun-IO/qqq/issues/550): Next dashboard lacks canonical multi-statistics, table, stacked-bar, stepper, small-line and composite-child widget compatibility (Medium; deferred). Core workflows and selected chart rendering do not establish full Material/Next parity.
- [#553](https://github.com/QRun-IO/qqq/issues/553): Material has incomplete empty/error indicators for some widget types, and deliberately malformed widget responses can blank the dashboard (Medium; deferred). The valid-empty stacked-bar crash is separately fixed under [#552](https://github.com/QRun-IO/qqq/issues/552).
- [#554](https://github.com/QRun-IO/qqq/issues/554): Material forwards dropdown/date selection persistence through parent widgets; ordinary HTML widgets do not save selections despite `storeDropdownSelections=true` (Medium; deferred).
- [#555](https://github.com/QRun-IO/qqq/issues/555): The native frontend widget metadata response omits `collapsible`, so declared widget collapse controls and persistence are unavailable through that response (Medium; deferred).
- [#556](https://github.com/QRun-IO/qqq/issues/556): Input-field blocks can render a visible label whose target does not match the input's generated ID (Low; deferred).

The September 23 triage defers Medium/Low issues to future releases:

- [#444](https://github.com/QRun-IO/qqq/issues/444): dashboard sharing truncates recipient IDs containing colons (Medium).
- [#445](https://github.com/QRun-IO/qqq/issues/445): report preview fails when a grid column has no field metadata (Medium).
- [#371](https://github.com/QRun-IO/qqq/issues/371) and [#332](https://github.com/QRun-IO/qqq/issues/332): remaining mock identity/display behavior (Medium/Low).
- [#447](https://github.com/QRun-IO/qqq/issues/447): saved-report deletion/retention policy investigation (Low; not a confirmed defect).

Candidate guard issue [#446](https://github.com/QRun-IO/qqq/issues/446) is also deferred (Medium): its newly introduced render policy requires clarification in [#451](https://github.com/QRun-IO/qqq/issues/451). Related contract investigations are [#448](https://github.com/QRun-IO/qqq/issues/448), [#452](https://github.com/QRun-IO/qqq/issues/452) and [#453](https://github.com/QRun-IO/qqq/issues/453). Confirmed Medium process-cache/session findings [#449](https://github.com/QRun-IO/qqq/issues/449) and [#450](https://github.com/QRun-IO/qqq/issues/450) are deferred. The additional rendering guard (#446 and part of #451), scheduled ownership/nested policy (#452), and Medium cache/session fixes (#449/#450) have been removed from the candidate and preserved for future work. API provenance (#451), protected storage inputs (#453) and automatic saved-source policy (#448) have also been removed from the candidate with their new dependent assertions preserved. Application API async state ownership remains explicit and independent of SYSTEM process provenance. The mandatory atomic storage API ([#459](https://github.com/QRun-IO/qqq/issues/459)), history validation/error-handling changes ([#460](https://github.com/QRun-IO/qqq/issues/460)) and rendering-only COMPLETE status ([#461](https://github.com/QRun-IO/qqq/issues/461)) are also deferred and extracted. Saved-report generation now completes before opening destination storage ([#458](https://github.com/QRun-IO/qqq/issues/458)); the existing streaming API and render-and-deliver failure status remain. The repeated-close memory storage fix ([#462](https://github.com/QRun-IO/qqq/issues/462)) and SFTP action-reuse investigation ([#463](https://github.com/QRun-IO/qqq/issues/463)) are Medium and deferred; their candidate changes and new memory tests are preserved outside the active release. Report count/reuse/query-preparation changes ([#464](https://github.com/QRun-IO/qqq/issues/464)) are Medium and extracted with their eight new assertions preserved for a future release. XLSX bounds, destination ownership, cleanup API, exception causes and unused allocation findings are tracked in [#465](https://github.com/QRun-IO/qqq/issues/465), [#466](https://github.com/QRun-IO/qqq/issues/466), [#467](https://github.com/QRun-IO/qqq/issues/467), [#468](https://github.com/QRun-IO/qqq/issues/468) and [#469](https://github.com/QRun-IO/qqq/issues/469); their candidate patches and dependent new assertions are extracted and preserved for future work; the established exporter interface and destination behavior remain. CSV/TSV title/header escaping ([#471](https://github.com/QRun-IO/qqq/issues/471), Medium) and streamed POI numeric styling/report-column overrides ([#472](https://github.com/QRun-IO/qqq/issues/472), Low) are extracted and deferred with their new tests preserved. Medium summary findings [#475](https://github.com/QRun-IO/qqq/issues/475), [#476](https://github.com/QRun-IO/qqq/issues/476) and [#477](https://github.com/QRun-IO/qqq/issues/477) are extracted with their new tests preserved; repeated/transformed summary inputs, shared total maps and hidden-total formula limitations remain. Medium producer findings [#473](https://github.com/QRun-IO/qqq/issues/473) and [#474](https://github.com/QRun-IO/qqq/issues/474) are extracted with the coupled process/report/ETL cancellation changes [#478](https://github.com/QRun-IO/qqq/issues/478), [#479](https://github.com/QRun-IO/qqq/issues/479) and [#480](https://github.com/QRun-IO/qqq/issues/480). Low malformed-UUID diagnostics [#481](https://github.com/QRun-IO/qqq/issues/481) are also deferred. Candidate patches/new assertions are preserved; original optional-hook, callback and transaction contracts remain alongside independent access protections. The selected deferrals and future work are tracked in [#534](https://github.com/QRun-IO/qqq/issues/534). AWS SDK v1/v2 consolidation remains deferred (BREAK-04-05); existing SDK usage is unchanged in this release.

Additional known Medium/Low findings are deferred to later versions under the same impact-based policy:

- [#470](https://github.com/QRun-IO/qqq/issues/470): Investigate intermittent empty HTTP response in saved-report sample test.
- [#482](https://github.com/QRun-IO/qqq/issues/482): Report view customization is TABLE-only and occurs after output preparation.
- [#483](https://github.com/QRun-IO/qqq/issues/483): Cloned report views share mutable QReportField objects.
- [#484](https://github.com/QRun-IO/qqq/issues/484): Native XLSX pivots have source-range, cache XML and SUMMARY-source limitations.
- [#485](https://github.com/QRun-IO/qqq/issues/485): Validate missing or incomplete runtime report views before output.
- [#486](https://github.com/QRun-IO/qqq/issues/486): Summary subtotals can become detached from their detail groups.
- [#487](https://github.com/QRun-IO/qqq/issues/487): JSON report keys reuse another view label for the same field name.
- [#488](https://github.com/QRun-IO/qqq/issues/488): Export preparation mutates caller filters and discards query-only limits.
- [#489](https://github.com/QRun-IO/qqq/issues/489): Header-free LIST_OF_MAPS export fails in the test-only collector.
- [#490](https://github.com/QRun-IO/qqq/issues/490): Internal LIST_OF_MAPS format returns HTTP500 through file-report routes.
- [#491](https://github.com/QRun-IO/qqq/issues/491): Single-record pipe postprocessing cannot remove or expand results correctly.
- [#492](https://github.com/QRun-IO/qqq/issues/492): RecordPipe termination can accept or count writes after cancellation.
- [#494](https://github.com/QRun-IO/qqq/issues/494): Clarify core export authorization and private-field predicate policies.
- [#495](https://github.com/QRun-IO/qqq/issues/495): Hidden USER export columns remain in spreadsheet headers.
- [#496](https://github.com/QRun-IO/qqq/issues/496): Spreadsheet export preflight counts rows outside USER personalization.
- [#497](https://github.com/QRun-IO/qqq/issues/497): Define failure delivery for partially streamed direct HTTP reports.
- [#502](https://github.com/QRun-IO/qqq/issues/502): Scheduled execution failures are swallowed instead of reaching Quartz listeners.
- [#503](https://github.com/QRun-IO/qqq/issues/503): Scheduled report synchronization lacks scheduler selection and per-record failure feedback.
- [#504](https://github.com/QRun-IO/qqq/issues/504): Saved asset deletion leaves dependent shares and quick-view preferences.
- [#505](https://github.com/QRun-IO/qqq/issues/505): OAuth session validation throws for absent or malformed token state.
- [#511](https://github.com/QRun-IO/qqq/issues/511): Define aggregate private-operand policy without changing trusted-source behavior implicitly.
- [#512](https://github.com/QRun-IO/qqq/issues/512): Define stricter Aggregate descriptor validation separately from existing execution contracts.
- [#513](https://github.com/QRun-IO/qqq/issues/513): Get-to-Query conversion drops read-only routing hints.
- [#514](https://github.com/QRun-IO/qqq/issues/514): Aggregate table widget column identities collide for null and reserved text values.
- [#515](https://github.com/QRun-IO/qqq/issues/515): RDBMS Aggregate COUNT decodes some operands using their source type.
- [#516](https://github.com/QRun-IO/qqq/issues/516): RDBMS Query and Aggregate leave statements open on caller-owned connections.
- [#517](https://github.com/QRun-IO/qqq/issues/517): RDBMS Aggregate group-only null lists and typed ordering have compatibility gaps.
- [#518](https://github.com/QRun-IO/qqq/issues/518): Memory Aggregate applies Query windows before grouping and differs on empty scalar results.
- [#519](https://github.com/QRun-IO/qqq/issues/519): Memory Aggregate decimal and binary equality and AVG precision differ from native SQL.
- [#520](https://github.com/QRun-IO/qqq/issues/520): MongoDB Aggregate result conversion mishandles BSON numeric/binary/civil-time values and AVG defaults.
- [#521](https://github.com/QRun-IO/qqq/issues/521): MongoDB Aggregate ignores result limits and has incomplete group/aggregate ordering.
- [#522](https://github.com/QRun-IO/qqq/issues/522): MongoDB Aggregate does not cancel its timeout task on empty or failed execution.
- [#523](https://github.com/QRun-IO/qqq/issues/523): Query and Aggregate cancellation delegates lack explicit cross-thread publication.
- [#524](https://github.com/QRun-IO/qqq/issues/524): MongoDB Aggregate can ignore unsupported joins or use missing virtual computations.
- [#527](https://github.com/QRun-IO/qqq/issues/527): Sharing-enabled saved-view providers omit generic sharing metadata.
- [#528](https://github.com/QRun-IO/qqq/issues/528): Typed JSON literal-null values raise ClassCastException.
- [#529](https://github.com/QRun-IO/qqq/issues/529): Query skip-only windows are ignored and negative-window handling varies by provider.
- [#530](https://github.com/QRun-IO/qqq/issues/530): Mongo Query exposes BSON wrappers and host-dependent civil value conversions.
- [#531](https://github.com/QRun-IO/qqq/issues/531): RDBMS qualified virtual-only projections can omit the requested values.
- [#532](https://github.com/QRun-IO/qqq/issues/532): Unknown middleware routes can fall through to the automatic dashboard SPA.
- [#535](https://github.com/QRun-IO/qqq/issues/535): ChildTable.joinFieldName is unused during child join metadata production.
- [#536](https://github.com/QRun-IO/qqq/issues/536): Memory Query fails sorting a weekday virtual field containing null values.
- [#538](https://github.com/QRun-IO/qqq/issues/538): Next omits declared app and section icons from live v1 metadata.
- [#539](https://github.com/QRun-IO/qqq/issues/539): v1 metadata omits application branding, leaving Next with default identity.
- [#541](https://github.com/QRun-IO/qqq/issues/541): Next can show Resource not found after a successful record deletion.
- [#542](https://github.com/QRun-IO/qqq/issues/542): TableSync skip summaries reverse insert and update labels.
- [#543](https://github.com/QRun-IO/qqq/issues/543): Versioned process init and step advertise uploads but do not deliver files.
- [#544](https://github.com/QRun-IO/qqq/issues/544): Table PVS selected-value lookup ignores overrideIdField.
- [#545](https://github.com/QRun-IO/qqq/issues/545): CacheOf advertises unimplemented key modes and source-miss caching.
- [#546](https://github.com/QRun-IO/qqq/issues/546): Disabled capability flags do not reject direct table HTTP operations.
- [#548](https://github.com/QRun-IO/qqq/issues/548): CLI criteria truncate values containing spaces.

The remaining 48 source-stage coverage groups, the published-stage BOM evidence group ([#627](https://github.com/QRun-IO/qqq/issues/627)), and other future work are tracked in [#534](https://github.com/QRun-IO/qqq/issues/534). These gaps remain open; the selected release acceptance does not claim complete feature coverage.

### Migration

See [docs/migration/4.0.adoc](docs/migration/4.0.adoc) for step-by-step migration guidance
covering all four breaking-change categories.

## [0.40.0] - 2026-03-29

### Breaking Changes
- **TableBasedAuthenticationModule** - Removed SHA1 backward compatibility for password hashing. Only SHA256 format (`sha256:iterations:salt:hash`) is now supported. Users with legacy SHA1-hashed passwords must reset their passwords.

### Added
- **Field Functions** - Virtual computed fields with backend-specific implementations (core, RDBMS, PostgreSQL, MongoDB). Supports `StringLength`, `WeekdayOfDate`, and more.
- **OAuth2 externalBaseUrl** - Split internal/external URL deployments for Kubernetes environments where pods cannot reach LoadBalancer VIPs
- **Collapsible Elements** - New `Collapsible` metadata class applied to `QFieldSection` and `QWidgetMetaData`
- **SpotBugs + PMD** - Static analysis integrated into CI pipeline via `qqq-orb/static_analysis` job
- **Branding** - `accentColorLight` and `gravatarDefault` fields added to branding metadata
- **Virtual Fields as PVS** - Virtual fields supported in possible value sources and `TableMetaData` API
- **Join Validation** - `JoinGraph` tracks flipped joins, `matchesJoinPath` considers flipped joins for matching
- **Saved Views** - `QuerySavedViewProcess` single-view mode adds quick-view attributes

### Fixed
- **JoinsContext** - Clone incoming `QueryJoin` objects to prevent shared-state mutation; prevent double-flip of multi-hop security join metadata; exclude implicit security lock joins from join cross product
- **MemoryRecordStore** - Clear stale display values on queried records; add virtual fields after stripping unrecognized fields; use `JoinsContext` query joins for cross product
- **RDBMS** - Paginate over primary keys in `doDeleteList` to avoid oversized queries; fix param binding in set-operation queries with virtual fields; fix order-by for grouped fields with field functions
- **Joins** - Normalize self-joins correctly by comparing fields; flipped joins respect `joinOn` fields
- **TableMetaDataAction** - Exclude deeply-nested exposed joins from response
- **MetaData Production** - Add class name tie-breaking for stable producer ordering

### Changed
- **Theme Refactor** - `QThemeMetaData` moved from `qqq-backend-core` to `qqq-frontend-material-dashboard` (frontend-specific)
- **License** - Migrated from AGPL-3.0 to Apache-2.0

### Security
- **Jetty 11.0.26** - Upgraded from 11.0.25 to fix HTTP/2 vulnerability (HIGH)
- **WireMock 3.13.2** - Upgraded from 3.13.0 to fix commons-fileupload vulnerability (HIGH)
- **commons-lang 2.x removed** - Migrated to commons-lang3 3.20.0 (no fix available for 2.x MEDIUM CVE)
- **iq80 snappy excluded** - Excluded vulnerable snappy from checkstyle plugin dependencies (MEDIUM)
- **mysql-connector-j 8.4.0** - Migrated from deprecated mysql:mysql-connector-java 8.0.30 (HIGH)
- **protobuf-java 3.25.5** - Override to fix DoS vulnerability (HIGH)
- **PasswordHasher SHA1 removed** - Eliminated weak cryptographic algorithm (CodeQL alert)
- **RapiDoc XSS hardened** - Strengthened URL validation with origin checking

### Notes
- commons-lang3 alert dismissed - already at 3.20.0
- commons-beanutils alerts dismissed - already at fix version 1.11.0
- jetty-http alerts dismissed - requires Jetty 12.x (Javalin 7.x)

## [0.35.0] - 2025-12-28

### Changed

#### Platform
- **Java 21 LTS** - Migrated from Java 17 to Java 21 LTS

#### Dependencies - Major Updates
- **JUnit Jupiter** 5.8.1 → 6.0.1 (major version upgrade)
- **Checkstyle** 10.16.0 → 12.2.0 (major version upgrade)
- **Mockito** 5.14.2 → 5.21.0
- **ByteBuddy** 1.15.4 → 1.18.3

#### Dependencies - Build Plugins
- maven-compiler-plugin 3.10.1 → 3.14.1
- maven-surefire-plugin 3.5.3 → 3.5.4
- maven-jar-plugin 3.4.2 → 3.5.0
- central-publishing-maven-plugin 0.8.0 → 0.9.0

#### Dependencies - Runtime
- MongoDB Driver 5.5.1 → 5.6.2
- SQLite JDBC 3.47.1.0 → 3.51.1.0
- AWS SDK BOM 2.40.13 → 2.40.16
- AWS Lambda Java Core 1.2.3 → 1.4.0
- AWS Lambda Java Events 3.14.0 → 3.16.1
- AWS Lambda Runtime Interface Client 2.6.0 → 2.8.7
- Commons Validator 1.9.0 → 1.10.1
- Jakarta Mail 2.0.1 → 2.0.2
- Angus Activation 2.0.2 → 2.0.3
- Nashorn Core 15.6 → 15.7
- SLF4J API 2.0.16 → 2.0.17
- Kotlin Stdlib 2.2.21 → 2.3.0

#### Dependencies - Test
- H2 Database 2.2.220 → 2.4.240
- Unirest Java 3.13.12 → 3.14.5

### Added
- Enhanced documentation wiki with comprehensive guides
- Improved GitHub templates and contribution process
- Better cross-references between documentation pages
- Health check middleware module (qqq-middleware-health)

## [0.27.0] - 2024-01-XX

### Added
- Comprehensive documentation wiki
- Enhanced development workflow guides
- Improved testing and code review standards
- Better contribution guidelines and templates

### Changed
- Streamlined documentation for QQQ framework developers
- Improved cross-references between wiki pages
- Enhanced GitHub project structure and templates

## [0.26.1] - 2024-01-XX

### Fixed
- Various bug fixes and improvements
- Enhanced stability and performance

## [0.26.0] - 2024-01-XX

### Added
- Core QQQ framework capabilities
- Backend modules for RDBMS, filesystem, MongoDB, SQLite
- Middleware support for Javalin, PicoCLI, Lambda, Slack
- React dashboard framework with Material-UI
- Comprehensive testing and quality standards

### Changed
- Initial public release of QQQ framework
- Established development workflow and standards
- Created modular architecture foundation

---

## 📚 For Detailed Information

**📖 [Complete Documentation Wiki](https://github.com/Kingsrook/qqq/wiki)** - Start here for comprehensive guides

- **[🏠 Home](https://github.com/Kingsrook/qqq/wiki/Home)** - Project overview and quick start
- **[🚀 Release Flow](https://github.com/Kingsrook/qqq/wiki/Release-Flow)** - Detailed release process
- **[🏷️ Changelog & Tagging](https://github.com/Kingsrook/qqq/wiki/Changelog-and-Tagging)** - Commit conventions and release notes
- **[🔧 Developer Onboarding](https://github.com/Kingsrook/qqq/wiki/Developer-Onboarding)** - Setup and contribution guide

## 🔄 Version Compatibility

QQQ follows semantic versioning:
- **MAJOR** versions may contain breaking changes
- **MINOR** versions add new functionality (backward compatible)
- **PATCH** versions contain bug fixes (backward compatible)

For detailed compatibility information, see [Compatibility Matrix](https://github.com/Kingsrook/qqq/wiki/Compatibility-Matrix).

---

**Thank you for using QQQ!** 🚀
