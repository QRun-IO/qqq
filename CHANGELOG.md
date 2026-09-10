# Changelog

All notable changes to QQQ will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [4.0.0] - Unreleased

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
- **BREAK-04-03** — `qqq-middleware-javalin` annotation-processor compiler block retained at Java 11
  (documented; not a version drift issue).
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

- Align Jackson modules on 2.21.5 and Netty modules on 4.1.137.Final through their BOMs.
- Update Log4j to 2.25.5, jsoup to 1.23.1, PostgreSQL JDBC to 42.7.12,
  c3p0 to 0.14.0, mchange-commons to 0.6.0, and Plexus Utils to 4.0.3.
- Update affected test dependencies: AssertJ 3.27.7, HttpClient 5.6.3,
  HttpCore 5.4.3, and Handlebars 4.5.2.
- Jetty 11 advisory remediation remains an open release gate while the Javalin 7 / Jetty 12
  migration is being evaluated. These dependency updates do not resolve those Jetty advisories.

### Known Issues / Deferred Bugs

No bug/defect-labeled issues remained open in the September 9 release review. The resolved bugs
are listed above. AWS SDK v1/v2 consolidation remains deferred (BREAK-04-05); existing SDK usage
is unchanged in this release.

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
