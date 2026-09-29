# QQQ Sample Project

QRun-owned reference application for QQQ 4.1: tables, related records, processes, widgets, the default Next dashboard, the optional Material Dashboard 0.41.0, and a PicoCLI entry point. The default database is an in-memory H2 database populated with sample data at server startup.

## Quickstart with Next

Requires JDK 21+, Git, curl and unzip, using Bash on macOS, Linux, or Windows WSL. No Maven, Node.js or Docker installation is needed. From a directory where you want the editable sample checkout:

```bash
curl -fsSLo quickstart.sh https://raw.githubusercontent.com/QRun-IO/qqq/quickstart-4.1.0/quickstart.sh
bash quickstart.sh
```

The script checks prerequisites and the port, clones into `qqq-sample`, compiles only this application against released QQQ 4.1.0, and opens <http://localhost:8000/app/person>. The sample depends on the `qqq-frontend-next` jar, so one Java process serves the API and the Next dashboard on port 8000. Local mock authentication and seeded H2 records require no provider account or database setup. Use this sample with local synthetic data.

Create a Person, edit and refresh it, then choose **Actions → Greet Interactive** from the record. Enter a greeting prefix and suffix and advance through the process. The Next dashboard's feature coverage is certified by the real-backend acceptance matrix in [qqq-frontend-next](https://github.com/QRun-IO/qqq-frontend-next/tree/main/docs/acceptance) ([#649](https://github.com/QRun-IO/qqq/issues/649)).

Press Ctrl+C to stop the application. Edit Java files under `qqq-sample/qqq-sample-project/src/main/java`, then run `./quickstart.sh` from `qqq-sample` to recompile and restart. Seeded data resets on each launch. Build and application output is in `qqq-sample/quickstart.log`. Use `bash quickstart.sh my-directory` for another destination; an existing destination is never overwritten. The script prints prerequisite installation links and reports an occupied port before downloading or starting anything. Run `QQQ_FRONTEND=material bash quickstart.sh` to open the Material Dashboard instead.

## Framework development

### Candidate bootstrap acceptance (#605)

Run the source stage from this checkout after installing JDK 21 and Maven:

```bash
python3 qqq-sample-project/bootstrap_acceptance.py --stage source
```

It creates a fresh disposable cache under the OS temporary directory, installs the root
framework there, then separately verifies the packaged sample. The bootstrap
reports require seeded HTTP startup, a real write, a second clean startup
with the original five rows, missing/duplicate configuration failures, and
three unskipped test suites. A separate BOM consumer resolves every reviewed
QQQ library at the same source revision. This is **source-install evidence**;
the cache contains locally built artifacts. The focused bootstrap profile
records its JaCoCo report but disables the full-suite coverage threshold;
normal sample `verify` and CI remain responsible for that threshold.
CircleCI runs this proof in the dedicated `sample_bootstrap_source` job on
feature, develop, release, tag and hotfix workflows. Publishing jobs require
it to pass. The job retains the runner report, Maven logs and JUnit reports,
then checks the report's commit and negative boundaries through the ledger.
Locally, copy the runner's `acceptance.json` to
`qqq-sample-project/target/bootstrap-acceptance.json`, then run:

```bash
python3 qqq-sample-project/verify-feature-coverage.py --stage source --require-feature sample.bootstrap
```

That focused check does not claim the full feature inventory is complete.

After the exact 4.1 release candidate is publicly available, set
`QQQ_CANDIDATE_VERSION` to its published literal version and run:

```bash
python3 qqq-sample-project/bootstrap_acceptance.py --stage published --version "$QQQ_CANDIDATE_VERSION"
```

Use the candidate version actually published. This stage exports committed
`HEAD`, creates an empty Maven cache and settings, imports the candidate
`qqq-bom-pom` in a separate versionless consumer, and verifies each library
JAR before building and starting the sample against public artifacts. Both
stages reject missing BOM configuration, a mismatched candidate, incomplete
test reports, and reuse of an explicit `--workdir`; evidence stays inside
the new fixture. The prior published set contained 16 libraries; the live
4.1 BOM adds `qqq-esb`, so this check requires all 17 and fails on BOM drift.
Browser-specific behavior, native provider contracts, and the full feature
ledger have their own gates; this bootstrap run certifies none of them.
`sample.bootstrap` records source-installed evidence only.
`sample.bootstrap.published` remains pending until the published stage passes
at the reviewed public candidate and its report is mapped into the ledger.

Requires Java 21 and Maven 3.8 or later. This separate path builds the framework and runs its acceptance tests.

From the QQQ repository root, install the framework modules, then build this separate sample:

```bash
mvn clean install
mvn -f qqq-sample-project/pom.xml -Pacceptance-tests clean verify
mvn -f qqq-sample-project/pom.xml exec:java \
  -Dexec.mainClass=com.kingsrook.sampleapp.SampleJavalinServer \
  -Dqqq.sample.mockAuthentication=true
```

Open <http://localhost:8000/> for the Next dashboard. Add `-Dqqq.javalin.frontend=material` to the `exec:java` command to serve the Material Dashboard instead; the Material browser suites (`-Pacceptance-tests`) select it that way. Expand **People App**, open **Greetings App**, and select **Person**. Open a record and choose **Actions → Greet Interactive** to try the local mock process. Stop the server with Ctrl+C; restarting it recreates the sample database. The explicit `qqq.sample.mockAuthentication` option selects bundled mock authentication for local exploration. Without it, configure `OAUTH2_BASE_URL`, `OAUTH2_CLIENT_ID`, `OAUTH2_CLIENT_SECRET`, and `OAUTH2_SCOPES` in the process environment or a `.env` file. Mock authentication is for local sample data only.

The runnable association graph is **Person → pets → Pet → notes → Pet Note**. **Greetings App → Pet Note** lists the seeded notes for Charlie and Toby, and lets you add a note using its Pet selector. An expanded read such as `curl 'http://localhost:8000/data/person/1?includeAssociations=true'` uses the exact group names `pets` and `notes`, including `notes: []` for pets with no notes.

Open **Miscellaneous → Field Lab** to create records with every declared field type and try length limits, case/whitespace normalization, and numeric bounds. Password values are synthetic demonstration data: read masking is not encryption or authentication storage. The sample configures H2 connections for UTC timestamp storage.

Field Lab also demonstrates DATE and DATE_TIME creation/modification defaults, an explicit `NONE` opt-out, and user defaults on writes. DATE defaults use the server's local calendar date; DATE_TIME defaults use the current instant. Reads do not invent a user ID for stored blanks. Case normalization can run on writes, reads or filters; read normalization does not rewrite stored data. The [field reference](../docs/metaData/Fields.adoc) describes these contracts and configuration boundaries.

To inspect the command-line interface:

```bash
mvn -f qqq-sample-project/pom.xml exec:java \
  -Dexec.mainClass=com.kingsrook.sampleapp.SampleCli -Dexec.args="--help" \
  -Dqqq.sample.mockAuthentication=true
```

Open **Miscellaneous → Pet Species** to inspect the same Dog/Cat enum used by pets as a read-only table. Try `curl 'http://localhost:8000/data/person/count'` for the seeded total of five; the [Count reference](../docs/actions/CountAction.adoc) explains filters, joined/distinct counts, transactions, permissions and backend-specific timeout/cancellation. Count acceptance uses the existing Person/Pet data plus explicit owned metadata/database variants; these variants are identified in `SampleCountContractTest` and do not change the default app's security policy.

The [Get reference](../docs/actions/GetAction.adoc) covers primary and declared unique keys, read options, associations and caller-owned transactions. `SampleGetContractTest` uses the same Person/Pet/Note, Carrier and Field Lab tables with isolated metadata variants and independent SQL checks. Get retains primary routing when given a read-only hint ([#513](https://github.com/QRun-IO/qqq/issues/513)); caller-owned transactions remain supported. The separate Query invocation fixture verifies replica routing against its own disposable H2 store.

The [Insert reference](../docs/actions/InsertAction.adoc) covers single and batch results, required/default values, uniqueness and caller-owned transactions. `SampleInsertContractTest` uses Person and Field Lab to check exact stored values, generated identities, mixed success/error results, record security, native failures and explicit commit/rollback. Fractional text for INTEGER/LONG fields is rejected; exact integral text such as `1,000.00` remains valid. The database tests also distinguish blank numeric keys, empty String natural keys, and MySQL's configured zero-key behavior. Inspect per-record errors and roll back a supported transaction when the complete operation must succeed together; a native failure can follow earlier successful writes.

Person marks its identity and creation/modification timestamps non-editable. Material and Next create/edit forms honor that metadata; the Material keyboard regression checks native SQL to ensure a creation timestamp cannot be changed through a disabled form control ([#537](https://github.com/QRun-IO/qqq/issues/537)). `isEditable` controls form editing; use the existing permission/personalization mechanisms for server-side write restrictions. `SampleUniqueKeyTest` and `SampleStorageConstraintTest` exercise declared and native composite-key insert/update conflicts, sparse and batch updates, null semantics, and independent SQL readback.

The People App → Greetings App hierarchy supplies the navigation/presentation example. `SamplePresentationMetadataTest` checks both metadata APIs, sections and record labels, hidden/disabled permissions, per-session field/order customization, and rejection of invalid parents/cycles; browser checks exercise both dashboards. Next nested-app links use the existing flat app route ([#540](https://github.com/QRun-IO/qqq/issues/540)). Its v1 branding ([#539](https://github.com/QRun-IO/qqq/issues/539)) and declared app/section icons ([#538](https://github.com/QRun-IO/qqq/issues/538)) remain deferred limitations. A successful Next delete can also leave a misleading not-found notification ([#541](https://github.com/QRun-IO/qqq/issues/541)); native deletion/readback passes. The owned Next navigation fixture excludes the external QuickSight widget and does not certify widget/provider coverage.

## Local sharing fixture and known limitations

The sample retains an opt-in sharing fixture (`qqq.sample.sharing=true`, requiring `qqq.sample.mockAuthentication=true`) with synthetic Alice, Bob and Casey users, H2 directory/assets/grants, memory schedules/history and filesystem output. Mock authentication is for local synthetic data only. The fixture exercises the existing generic sharing configuration; it does not establish production authentication or an archive-retention policy.

The dashboard walkthrough is not certified for 4.0: colon-containing recipient IDs ([#444](https://github.com/QRun-IO/qqq/issues/444)), missing-column report preview ([#445](https://github.com/QRun-IO/qqq/issues/445)) and mock identity/display behavior ([#371](https://github.com/QRun-IO/qqq/issues/371), [#332](https://github.com/QRun-IO/qqq/issues/332)) are deferred Medium/Low issues. The local browser fixes and their dependent reproduction tests are preserved for a future release; their earlier passing results do not apply to the 4.0 candidate. Do not use a previously built local dashboard JAR as evidence that these limitations are fixed.

Backend sample fixtures continue to exercise native saved-report/view ownership, sharing, revocation, rendering and download boundaries. All remaining acceptance gaps stay visible in `feature-coverage.json`.


## Acceptance coverage

`SampleHealthAcceptanceTest` loads a sample-owned health metadata producer and
exercises real local HTTP, an owned H2 TCP database, thresholds, failures, and
worker/request cleanup. Both health rows (#599/#600) have passing source
evidence, including the retained within-request concurrency regression after
the bounded executor fix. See [health acceptance evidence](acceptance/health/README.md)
for timeout semantics and full-profile verification results, including the
required sample class-coverage gate. No health requirement is deferred or removed by this fixture.

`SampleWidgetContractTest` checks the eleven canonical dashboard payloads, protected-widget GET/POST denial before renderer execution, allowed controls, unknown widgets, empty HTML and actual renderer errors. `SampleBrowserIT#testCanonicalWidgets` checks their visible Material output, plotted marks and pie-chart reload. `SampleWidgetStatesBrowserIT` checks empty/error isolation for all eleven types and explicitly records malformed-payload limitations ([#553](https://github.com/QRun-IO/qqq/issues/553)); its passing limitation probe does not certify malformed-data robustness. All eleven canonical types now have denied/allowed GET/POST and invalid renderer-reference checks; existing native NoCode/2D source-permission tests are retained. `SampleWidgetControlsBrowserIT` covers parent dropdown/date persistence, CSV downloads, reload/tooltips, failed/denied widgets and native button/Enter callbacks. Non-parent selection persistence ([#554](https://github.com/QRun-IO/qqq/issues/554)), collapse metadata ([#555](https://github.com/QRun-IO/qqq/issues/555)) and input-block label association ([#556](https://github.com/QRun-IO/qqq/issues/556)) remain explicit deferrals; additional widget/block/provider cases remain in the inventory. Next supports the tested statistics/HTML/bar/line/pie path but retains the explicitly deferred widget compatibility gaps in [#550](https://github.com/QRun-IO/qqq/issues/550).

`SampleInteractiveProcessTest` exercises the versioned greeting workflow, selected records, form/component metadata, rendered HTML and the sample widget payload, async progress/status, invalid inputs, lost sessions/state and legacy back navigation. Matching saved-report tests verify generated download bytes and ownership. The legacy process route supports tested multipart upload/archive/download; **v1 init/step do not deliver uploaded files despite advertising a file field** ([#543](https://github.com/QRun-IO/qqq/issues/543), Medium, deferred). A successful v1 response does not prove file delivery. The separate limitation probe records that behavior without certifying it. Unknown linear resume targets retain the existing no-op contract. Source protocol checks do not certify complete dashboard rendering.

`SampleProcessFlowTest` exercises linear and state-machine execution against owned Person data: selected records, required values, ordering/overrides, optional states, back navigation, rejected code/state targets, bounded cycles and caller context after failures. Cancellation checks cover the existing application hook/no-hook and ownership behavior; terminal cleanup and failed-run lifecycle guarantees remain deferred under [#478](https://github.com/QRun-IO/qqq/issues/478) and related inventory entries. `SampleTableSyncTest` uses a disposable native H2 destination to exercise preview/confirmation, insert/update/skip, repeated synchronization, lookup/filter hooks, disabled operations and mixed/source errors. Skipped-operation summary wording is reversed ([#542](https://github.com/QRun-IO/qqq/issues/542)); the underlying writes pass. Bulk source workflows are covered below; dashboard controls remain separate acceptance.

`SampleEtlCancellationContractTest` verifies Basic/Streamed mapping, bounded pages, preview/full validation without writes, mixed record summaries, insert/update/delete loads and native transaction outcomes after actual extractor/transformer/loader exceptions. Auto-commit retains completed writes; page/process modes roll back the failed transaction. Cancellation and broader pipe lifecycle limitations remain deferred under [#480](https://github.com/QRun-IO/qqq/issues/480), [#491](https://github.com/QRun-IO/qqq/issues/491) and [#492](https://github.com/QRun-IO/qqq/issues/492). `SampleBasepullTest` verifies native persisted watermarks, initial lookback, independent variants, failure/selection without watermark advance, timestamp equality/overlap and empty/reversed windows.

`SampleBulkProcessTest` exercises the built-in insert/edit/delete processes on owned Person/Pet/PetNote data. Native SQL verifies CSV/XLSX field and PVS mapping, preview/full validation without writes, confirmed loads, saved-profile persistence/reuse/delete, selected and query-matched edits, partial row errors, row-lock exclusions, associated deletion and rollback after an actual loader exception. Malformed files, missing mappings/selections, duplicate keys and middleware permission denial are checked. Existing cancellation limitation [#480](https://github.com/QRun-IO/qqq/issues/480) remains deferred. Legacy multipart byte-transfer evidence is retained; versioned upload limitation [#543](https://github.com/QRun-IO/qqq/issues/543) and dashboard rendering are not certified by these source tests.

Reporting source scenarios are reconciled in `feature-coverage.json`: all export formats, configured privacy/ownership/sharing, Quartz/SMTP delivery, report views, formulas/totals, variance sources and custom record transforms have sample evidence. Known Medium/Low limitations remain explicitly deferred, including query windows, dependent cleanup and the dashboard sharing walkthrough. This is source acceptance with named limitations; full dashboard and published-artifact acceptance remain open.

`SampleMetadataContextTest` exercises canonical metadata enrichment and rejected references, duplicate unique keys and invalid code references. Its worker examples show explicit context capture, cleanup, isolated users on a reused thread, and restoration after temporary-context failure. These fixtures use the sample's own H2 data.

`SampleMetadataProductionTest` uses explicit `fixturemetadata` variants with disposable memory tables to exercise scanned annotated entities/enums, generated child joins/widgets, multi-output producers, and serialized JSON metadata loaded back into usable tables. It also covers typed records, display/backend values, entity/association conversion, changed-field updates, nulls and invalid mappings. The fixture package is outside the running application’s metadata scan; existing SQL insert/update cases cover warnings versus rejecting errors. Child joins use the child field’s parent possible-value-source annotation; the explicit `ChildTable.joinFieldName` option is unused and deferred in [#535](https://github.com/QRun-IO/qqq/issues/535).

`SampleVirtualFieldTest` reads canonical Field Lab SQL rows and an owned Memory variant. It covers string length, both substring forms, date/datetime weekdays, explicit timezone and Sunday-first ordering, invalid arguments, custom function registration and refusal by a backend without an adapter. Null calculations pass; sorting mixed null/non-null weekday values in Memory fails under deferred [#536](https://github.com/QRun-IO/qqq/issues/536), so the working sort example filters out null dates. The failed reproduction is retained in release evidence; it is not a passing scenario.

Field Lab now also checks that malformed DATE, TIME and DATE_TIME inserts/updates leave every stored column unchanged. Its HTTP scenario verifies all twelve field types, including large numeric values, exact binary bytes and masked passwords. Existing datetime display checks cover timezone and daylight-saving behavior; the remaining dashboard/provider-specific scenarios stay visible in the inventory.

`SampleDynamicRouteTest` exercises the configured text response at `/dynamic-site/details`. `SampleCustomRouteTest` uses explicit variants of the same metadata for all five configurable HTTP verbs, decoded inputs, custom context handlers, binary/redirect/storage responses, private-file authentication, traversal rejection and failure cleanup. Authentication uses the existing local demonstration credentials; streaming uses the sample City filesystem table in a disposable directory. These variants are test fixtures, while the running sample keeps its configured GET route.

`SampleQueryPrivacyTest` exercises direct reads and bounded streams against Person/Pet/Note and Field Lab, including replacement customizers, exact projections, selected joins, virtual fields, private labels and explicit visibility overrides. Direct and buffered empty/expanded-result controls remain; their plain single-record assertions are archived under [#491](https://github.com/QRun-IO/qqq/issues/491). The new live termination/accounting assertions are archived under [#492](https://github.com/QRun-IO/qqq/issues/492), retaining an ordinary native two-row/no-mutation control. HTTP tests retain joined privacy through both Query API versions and legacy Get. Synthetic data and native SQL snapshots support these checks. Current common Get/Query evidence has been reconciled against matching frozen source and reports. Known pipe, routing, pagination, resource and qualified virtual-projection limitations remain explicitly deferred; this does not certify full provider coverage or published artifacts.

`SampleAggregateWidgetCallerTest` exercises no-code query/count/aggregate values, named Java ad-hoc functions, all four calculation operators, conditional/wrapped HTML, invalid and empty sources, template failures, and explicitly escaped user text. Real HTTP/H2 checks enforce source-table and joined-table READ rules, personalized row limits and removed metadata while preserving trusted SYSTEM controls ([#557](https://github.com/QRun-IO/qqq/issues/557)). Run it with `mvn -f qqq-sample-project/pom.xml -Dtest=SampleAggregateWidgetCallerTest test`. No-code templates emit raw HTML; the application must escape untrusted values before inserting them into that HTML.

Parent-widget acceptance uses `SampleWidgetContractTest` and the `testParent*` methods in `SampleWidgetControlsBrowserIT`: GRID/TABS, visible child values, empty and failed parents, invalid renderer references, parent/child permissions, missing children and a healthy neighboring widget. [#558](https://github.com/QRun-IO/qqq/issues/558) fixes permission-denied children blanking Material. Reliable saved-tab restoration remains deferred under [#559](https://github.com/QRun-IO/qqq/issues/559); acceptance verifies explicit tab switching and stored selections without requiring the deferred restoration defect to occur. The existing native chart-payload checks cover `ChartData`; `CHART` is not advertised as a standalone frontend renderer.

This application is the first-party release acceptance target. It uses synthetic H2 data and explicit local mock authentication; no downstream application, account or sign-off is involved. Field Lab exposes every field type, length/case/whitespace/range policy examples, dynamic defaults, and fixed/record-specific timezone display. Its tests cover daylight-saving boundaries and invalid/missing-zone fallback as well as persistence. `SampleJavalinServerTest` exercises the real dashboard bundle, metadata, HTTP CRUD and query, independent JDBC readback, required-field rejection without persistence, and greeting-process output.

`SampleDisplayWidgetsBrowserIT` exercises alerts (including hidden output), dividers, field-value lists, horizontal bars and multiple tables through the native sample server and real Material browser. It checks typed values, widget permissions, invalid renderers, empty/error states and the backend-only `LocationData` payload. Deliberately malformed alert, field-value-list and multi-table responses reproduce the deferred [#553](https://github.com/QRun-IO/qqq/issues/553) limitation; those probes do not certify robust rendering. Run with `mvn -Pacceptance-tests -Dit.test=SampleDisplayWidgetsBrowserIT -f qqq-sample-project/pom.xml initialize test-compile failsafe:integration-test failsafe:verify`.

The same browser class also exercises a process widget round trip and permission denial, a first-party dynamically loaded component and missing-bundle error, the map's deferred fixed-marker limitation ([#560](https://github.com/QRun-IO/qqq/issues/560)), and the real QuickSight renderer/SDK against a local protocol service. The QuickSight fixture uses synthetic credentials and the SDK's [endpoint override](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/endpoint-config.html); it verifies outgoing request identity, iframe content and denied/empty provider responses without an AWS account. This is owned protocol acceptance, not live AWS acceptance.

Child-record widget coverage combines `SampleWidgetContractTest` with `SampleDisplayWidgetsBrowserIT#testChildRecordGridAndStates`: actual person/pet rows, limited totals, record links, empty/error/malformed states, invalid references, source/join READ denial, personalized counts and the trusted internal path. The local Material grid's missing-license watermark is recorded separately as deferred Low [#562](https://github.com/QRun-IO/qqq/issues/562).

The required goal is coverage of every supported QQQ feature and use case. The [feature inventory](feature-coverage.json) records the remaining scenario/browser/integration gaps; the current sample does **not** yet meet that goal. Core unit-test coverage alone does not establish sample feature coverage. A verified feature needs a reviewed list of public contracts, runnable examples, and assertions for their successful and rejected outcomes. Passing one example does not certify the entire family: filtered tenant counts, distinct counts, update normalization, filter normalization, DATE defaults and DATE_TIME defaults are separate cases.

`SampleAssociatedWriteTest` uses that canonical three-level graph for recursive HTTP INSERT/UPDATE, generated IDs, omitted versus empty note groups, legacy flat-form compatibility, and denied/malformed requests with independent SQL snapshots. The recursive form format requires `X-QQQ-Association-Format: record-v1` and one `associations` field containing named arrays of `{ "values": { ... }, "associatedRecords": { ... } }` records. Permission/parse failures are checked before mutation; this does not promise automatic rollback after a later native or business-validation failure. `SampleDatabaseIT` exercises the same graph/schema in disposable MySQL/PostgreSQL containers, plus explicit native key-generation fixture variations. No additional application or backend is used for the runnable model.

Field Lab is part of the running application. Tenant security currently uses programmatic test fixtures; it is not yet a navigable sample mode. MySQL/PostgreSQL and MongoDB integration tests use explicitly disposable containers. These forms of evidence remain distinct so an integration test is not mistaken for a runnable application example.

Run acceptance with Java 21, Docker, and a locally installed Chrome (or Chrome for Testing):

```bash
mvn -f qqq-sample-project/pom.xml -Pacceptance-tests clean verify
python3 qqq-sample-project/verify-feature-coverage.py --report-only
```

The profile runs browser workflows, packaged configuration and SPA launchers from empty working directories, CLI commands, Field Lab temporal readback against disposable MySQL 8.4/PostgreSQL 17 containers, and the MongoDB fixture described below. It pulls the database images when needed, requires no shared database or provider account, and fails if Docker is unavailable. The browser uses a new disposable profile and the test owns/stops its sample server and database containers. Screenshots and test reports are under `qqq-sample-project/target/`. Use the acceptance profile for this module's `verify` or `install`: its coverage gate includes the packaged launchers. For lightweight checks, `mvn -f qqq-sample-project/pom.xml test` runs ordinary unit tests without Chrome or Docker. The coverage checker without `--report-only` fails for pending, unmapped, skipped, missing or failed feature checks. `verified_tests` binds current evidence; signed Git history preserves the initial audit. A reviewed enum-only declaration is listed separately as unsupported; implemented backend-only contracts still require tests.

`SampleCliContractTest` runs the generated CLI over canonical Field Lab and verifies native persistence, typed arguments, JSON/CSV mapping, CSV/XLSX exports, process results, invalid input, permission denials and context restoration. The packaged CLI is also checked from an empty directory. [#547](https://github.com/QRun-IO/qqq/issues/547) adds the missing CLI permission enforcement. Multiword criteria remain limited by [#548](https://github.com/QRun-IO/qqq/issues/548); use primary-key selection for affected mutations. Run the focused workflow with `mvn -f qqq-sample-project/pom.xml -Dtest=SampleCliContractTest test`.

`SampleBackendCapabilityTest` covers backend dispatch, capability defaults/overrides, isolated variants and invalid variant configuration. `SamplePermissionMatrixTest` covers permission levels, hidden/disabled metadata, custom permission checks, direct HTTP CRUD/process denials and native database readback. Without `TABLE_COUNT` or `TABLE_EXPORT`, the count and export routes refuse requests even on an unprotected table; other capability flags do not deny direct HTTP operations, so configure permission rules for access control. Broader runtime capability enforcement is deferred in [#546](https://github.com/QRun-IO/qqq/issues/546). Browser controls remain separate dashboard acceptance work.

`SampleSQLiteTest` runs canonical Field Lab against a temporary SQLite file using the existing SQLite module as a test-scoped dependency. Native SQL checks cover CRUD, single/batch/default generated keys, scalar/temporal/binary types, filters/paging, counts/aggregates, joins, record locks, rejected input, transaction rollback and recovery, native timeout/cancellation, and simple/C3P0 connection ownership. The fixture closes its connections and pool; it needs no Docker or provider account. Run it with `mvn -f qqq-sample-project/pom.xml -Dtest=SampleSQLiteTest test`. The running application continues to use H2. Existing common limitations remain: Get replica hints (#513), pipe postprocessing/termination (#491/#492), qualified virtual-only projection (#531), statement cleanup (#516) and cancellation delegate publication (#523). Connection lifecycle checks do not certify statement closure or unsynchronized race freedom.

`SampleMongoDatabaseIT` uses canonical Field Lab metadata in an isolated `mongo:7.0` standalone container with fixture-only credentials and a published-port readiness check. The test owns and closes its client/container; each case resets its generated database and verifies an unrelated collection is untouched. Native BSON readback checks selected CRUD, generated and supplied keys, type round trips, paging/sort/filter, count/aggregate, computed predicates, and READ policies. New boundaries cover a missing collection, disconnected endpoint, malformed filter, ordered duplicate-key batch, reused client session, client close/reopen, and the standalone rollback limit. A standalone rollback cannot undo an already durable write; declaring transaction support against that server fails without writing.

`SampleMongoReplicaSetIT` owns a separate single-node `mongo:7.0` replica set and native oracle client. It verifies that a public-action batch becomes visible only after commit, rollback discards writes and permits reuse, and a duplicate-key failure rolls back the entire batch. A targeted MongoDB `failCommand` on `aggregate` proves QQQ's timed-out query reached the server and returned its timeout error; disabling the failpoint lets a fresh public query recover. Closing a transaction with an uncommitted write releases its client, aborts the write, and leaves the server available to a fresh action. These tests use fixture-only credentials and random published ports; the replica-set database is dropped after each case.

These are bounded provider checks, so `backend.mongodb.data` and `backend.mongodb.lifecycle` remain pending. MongoDB write lookups require native ObjectId keys exposed as hexadecimal Strings; selected native BSON String keys or changed decoded primary keys are refused, while complete native String-key CRUD and ordinary lookup disambiguation remain unproven. A temporary source probe passed `new QRecord().withValue("integerValue", "not-an-integer")` to `InsertAction`: it returned no errors, wrote BSON String to `integer_value`, and public `GetAction` returned that String despite INTEGER metadata. The probe was removed after capturing its focused test result; malformed-type rejection needs a separately reviewed validation policy ([#820](https://github.com/QRun-IO/qqq/issues/820)). MongoDB aggregate joins may be ignored rather than rejected ([#524](https://github.com/QRun-IO/qqq/issues/524)); do not use this fixture as join acceptance. The timeout fixture proves server failpoint entry, client-visible timeout and fresh-query recovery; it does not inspect `currentOp` or prove server-operation cancellation. Native owned/borrowed close and reuse, one transaction-owned session, and injected initialization/close failure cleanup are covered by [#821](https://github.com/QRun-IO/qqq/issues/821). Broader resource accounting and combined-source review remain pending; QQQ defines no shared Mongo pool policy. #525/#526 and the documented deferrals are unchanged. Ordinary `mvn -f qqq-sample-project/pom.xml test` does not run these Failsafe classes. MongoDB reports are written to `qqq-sample-project/target/failsafe-reports/`.

To validate a separately built dashboard candidate, add `-Dqqq.frontend.material-dashboard.version=<candidate-version>` to the acceptance command. Install that candidate under its own version with its freshly built assets and resolved QQQ dependency versions, and verify that the assembled sample contains the same assets. An overridden local run is source compatibility evidence; public-artifact acceptance must use the versions actually published.

The checker pins the reviewed set of feature IDs and allows only `train.bom` and `sample.bootstrap.published` to wait for published artifacts. Removing/renaming a feature or changing that boundary requires a source review and an explicit checker change. This prevents accidental scope shrinkage; it does not prove the inventory is exhaustive. Always run a clean Maven verification immediately before checking the ledger, because XML test reports alone do not identify the source revision they exercised.

CI runs the complete sample test suite for feature changes and before publication. For 4.0, the maintainer deferred comprehensive feature coverage under [#534](https://github.com/QRun-IO/qqq/issues/534): CI reports source-stage gaps without making them publication blockers. Public-artifact acceptance still requires a clean-cache test run against the published versions. Run `python3 qqq-sample-project/test_feature_coverage.py` to check the ledger verifier itself.

After committing the sample and publishing the release, validate those public artifacts using Python 3.12 or later:

For a published BOM candidate or GA release, first merge the **Published BOM consumer** workflow into default `develop` and carry it into `release/4.1` and the GA tag. After Central sync, dispatch it on `release/4.1` with the exact `4.1.0-RC.N` version. For GA, dispatch the workflow through GitHub CLI or API with tag ref `v4.1.0` and version input `4.1.0`; the web selector only lists branches. Require a successful RC run and retain its attached evidence before GA promotion; verify the GA artifact after it is public. The workflow checks that the selected source revision matches the requested version and runs `python3 qqq-sample-project/verify-published-bom.py 4.1.0-RC.1` (substitute the literal published version). The checker imports the public BOM through Maven Central with empty settings and a fresh cache, compares every normalized managed coordinate, and resolves every managed QQQ jar plus the pinned Next dashboard. It checks direct dependency scopes and compares each cached BOM/jar byte-for-byte with a separate HTTPS fetch from Central. It fails on absent, wrong-version, or snapshot dependencies and keeps `pom.xml`, `settings.xml`, `maven.log`, `dependency-tree.json`, the isolated cache, and `evidence.json` under `qqq-sample-project/target/published-bom-*`. Local runs can archive that directory; the workflow attaches the report, consumer POM, Maven log, and dependency tree. This checks the published BOM consumer contract, while the sample acceptance command below checks runtime behavior.

```bash
python3 qqq-sample-project/verify-published.py 4.1.0 --material-version 0.41.0 --next-version 0.2.1
```

Supply the core and dashboard versions actually published; the command above selects final core 4.1.0, Next dashboard 0.2.1 and Material Dashboard 0.41.0. This exports committed `HEAD`, resolves the literal parent and dashboard from Central with empty user/global settings and a new cache, runs the complete sample acceptance profile, and reports the feature ledger. It does not install local framework artifacts. It retains `maven.log` and `acceptance.json` under `target/published-*`, including separate test-acceptance and feature-coverage results. Add `--require-complete-coverage` when the deferred comprehensive feature gate is required; without it, a successful run does not certify the deferred scenarios.

## Source entry points

All paths below are under `src/main/java/com/kingsrook/sampleapp/`:

- `metadata/SampleMetaDataProvider.java`: database, tables, processes, and navigation.
- `metadata/FieldLabTableMetaDataProducer.java`: field types and behavior examples.
- `SampleJavalinServer.java`: HTTP server and sample database initialization.
- `SampleCli.java`: command-line entry point.
- `ConfigFileBasedSampleJavalinServer.java`: the complete sample plus optional metadata files.
- `IsolatedSpaServer.java`: bundled public and authenticated private SPAs.

Every launcher shares the canonical application, bundled `metadata/personTable.yaml`, and guarded in-memory H2 bootstrap. `ConfigFileBasedSampleJavalinServer` accepts zero or one argument: an optional directory of additional YAML/JSON metadata. That directory must contain only metadata files. It uses each metadata type's registration rules: duplicate tables fail, while singleton branding or authentication at an existing scope can be replaced. Missing directories, invalid properties and duplicate table names fail startup with a nonzero exit. It does not create database tables for added metadata.

To run either HTTP example from the assembled JAR, set `sample_jar` to the absolute path of `qqq-sample-project/target/qqq-sample-project-<version>-jar-with-dependencies.jar`:

```bash
java -Dqqq.sample.mockAuthentication=true -Dqqq.sample.port=8000 \
  -cp "$sample_jar" com.kingsrook.sampleapp.ConfigFileBasedSampleJavalinServer
java -Dqqq.sample.mockAuthentication=true -Dqqq.sample.port=8000 \
  -cp "$sample_jar" com.kingsrook.sampleapp.IsolatedSpaServer
```

Run one server at a time. The SPA example serves a public application at `/` and a private application at `/private/`. Its HTTP Basic demonstration credentials are `sample` / `sample-only`; they are public synthetic values for exercising access rejection locally. Missing, malformed or incorrect credentials receive 401, including requests for private assets and deep links. Valid deep links load their SPA, while missing assets and API paths retain their errors. The sample's QQQ APIs use the separately selected mock authentication. Real applications must configure their own authentication provider before serving real data.

The unused parallel Liquibase bootstrap was removed because its schema and paths had drifted from the running application; framework Liquibase generation still requires its own acceptance scenario.

This sample is built from the repository and is not published to Maven Central. See the [4.0 migration guide](../docs/migration/4.0.adoc) for API changes.

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.

`SampleDownloadContractTest` exercises real HTTP download and direct-report routes using owned temporary files. Its explicit metadata fixture adds the built-in report process over canonical Pet Species and a trusted storage-producing step over canonical City storage. It verifies exact CSV/storage bytes, GET/POST report downloads, UTF-8 attachment names, separate-session denial, tampering, forged output values, refusal of arbitrary storage references even with table READ, and removal of temporary output after report generation fails. Named-report checks verify allowed report access without raw-table READ, report-permission denial through direct, browser-process and application API entry points, effective API input/default/customizer selection, exactly-once custom checks, early standard denial, preservation of application-customized query input source, invalid-format controls and successful complete JSON/XLSX responses. Later-view no-partial-response policy is deferred in [#497](https://github.com/QRun-IO/qqq/issues/497). These focused checks do not certify multipart upload/archive behavior or the complete saved-report feature; `http.uploads` remains pending.

`SampleUploadContractTest` adds nine focused native multipart checks using canonical City storage and an explicit upload-consuming process fixture. They cover denied/unknown processes, traversal and unsafe filenames, validation before the first write, missing archive configuration, native request-size limits including chunked requests, and exact multi-file archive/process/download bytes under a configured limit. Archive contents are independently read back from owned temporary files. Later storage/process-failure cleanup, complete saved-report flow and final integrated acceptance remain open; these bindings do not promote `http.uploads` to accepted.

`SampleSavedReportContractTest` exercises saved reports, table exports and report views through the real sample HTTP server, trusted Java calls and two canonical instances. Its fixture installs `SavedReportsMetaDataProvider` over memory metadata and owned filesystem storage, with canonical H2 people/pets, Pet Species and the tables possible-value source.

Its focused contracts cover:

- Browser, v1 and application API report bytes, process permissions and trusted configuration. API provenance and archive-input policy changes are deferred in [#451](https://github.com/QRun-IO/qqq/issues/451) and [#453](https://github.com/QRun-IO/qqq/issues/453). Extra rendering-ownership policy is deferred in [#451](https://github.com/QRun-IO/qqq/issues/451)/[#446](https://github.com/QRun-IO/qqq/issues/446).
- Asynchronous results and process-state isolation across sessions, applications and backend variants; owner identity and permission rechecks.
- Schedule creation/edit/delete, manual execution and automated-owner execution. Caller-session restoration remains deferred in [#450](https://github.com/QRun-IO/qqq/issues/450). Schedules remain inactive; the email-error fixture fails sender validation before invoking a provider.
- Trusted report source behavior and application-customized query inputs. Explicit USER propagation and forced post-customizer policy are deferred in [#448](https://github.com/QRun-IO/qqq/issues/448). Processed-row counts, action-reuse reset and changed query preparation are deferred in [#464](https://github.com/QRun-IO/qqq/issues/464); their new assertions are preserved outside the active build. Automatic saved-report source provenance is deferred in [#448](https://github.com/QRun-IO/qqq/issues/448); its new policy assertions are archived outside the active build.
- Generation before archive opening, preservation of prior bytes on generation failure, ordinary delivery-failure history, temporary-output cleanup and readable workbook output. New producer-failure/cleanup assertions are preserved for future work under [#473](https://github.com/QRun-IO/qqq/issues/473) and [#474](https://github.com/QRun-IO/qqq/issues/474). New exporter cleanup/ownership/bounds assertions are preserved for future work under [#465–469](https://github.com/QRun-IO/qqq/issues/465).
- JSON column labels with the existing missing-label fallback. Cross-view label and blank-label changes are archived under [#487](https://github.com/QRun-IO/qqq/issues/487). Five new summary-isolation/total assertions are preserved outside the active build under [#475](https://github.com/QRun-IO/qqq/issues/475), [#476](https://github.com/QRun-IO/qqq/issues/476) and [#477](https://github.com/QRun-IO/qqq/issues/477); the documented existing limitations remain.

`SampleTableExportContractTest` retains canonical H2 controls for ordinary/empty exports, header-free XLSX/JSON/CSV/TSV, zero limits, established field-value encoding, selected rows, possible-value labels and joined-row SQL readback. USER joined-table READ and output-privacy checks, trusted SYSTEM/REVEAL and ordinary column-capacity controls remain. Core/private-predicate policy, hidden headers and personalized preflight assertions are preserved outside the candidate under [#494](https://github.com/QRun-IO/qqq/issues/494), [#495](https://github.com/QRun-IO/qqq/issues/495) and [#496](https://github.com/QRun-IO/qqq/issues/496). Window/immutability and earlier negative-preflight assertions are archived under [#488](https://github.com/QRun-IO/qqq/issues/488); title/header capacity under [#465](https://github.com/QRun-IO/qqq/issues/465). Joined-table authorization runs at preflight and execution; generic action-reuse revalidation is deferred under #488. Header-enabled `LIST_OF_MAPS` remains a static test utility; only its header-free branch is archived under [#489](https://github.com/QRun-IO/qqq/issues/489). Internal-format HTTP/parser assertions are archived under [#490](https://github.com/QRun-IO/qqq/issues/490), preserving invalid-name and successful file-format controls. Run release-scoped export/query/download checks against the selected source and public artifacts; the deferred scenarios remain tracked in [#534](https://github.com/QRun-IO/qqq/issues/534).

`SampleReportViewContractTest` retains ten canonical H2 controls for static suppliers, input filters, formulas, zero denominators, empty sources, bad formulas and unknown pivot-source lookup. Ordinary/empty native pivot checks inspect workbook source ranges, grouping indexes and refresh metadata; CSV, TSV and JSON omit native pivots. These verify workbook structure, not spreadsheet-application recalculation. Eleven new customization, title/XML/SUMMARY pivot and early-validation assertions are archived with deferred [#482](https://github.com/QRun-IO/qqq/issues/482), [#483](https://github.com/QRun-IO/qqq/issues/483), [#484](https://github.com/QRun-IO/qqq/issues/484) and [#485](https://github.com/QRun-IO/qqq/issues/485).

`SampleSummaryReportContractTest` retains three controls for three-level totals, flat ordering without subtotals and formulas over subtotal aggregates, including independent native H2 SQL checks. Four new hierarchical-ordering assertions are archived under [#486](https://github.com/QRun-IO/qqq/issues/486). Existing flat-sort limitations remain. The saved-report unlabeled-formula control retains a displayed total and does not depend on deferred hidden-total behavior. Earlier larger checkpoints belong to their recorded source. Selected-source and public-artifact acceptance are separate from the comprehensive coverage deferred under [#534](https://github.com/QRun-IO/qqq/issues/534).

The five new `SampleExportPresentationContractTest` methods and their patches are preserved outside the active build: CSV/TSV title/header escaping is deferred in [#471](https://github.com/QRun-IO/qqq/issues/471), and streamed POI numeric styling/report-column overrides in [#472](https://github.com/QRun-IO/qqq/issues/472). Existing output-format, ordinary value escaping, template and pivot controls remain active.

The earlier table-export source checkpoint exercised the inventory requirements: all five formats, explicit selected IDs and row windows, selected/private fields and row locks, PVS/display translation, empty results, unsupported HTTP formats, native producer failure, and XLSX preflight. Physical streaming guards and exactly-once destination-close expectations are deferred in #465/#466. Earlier 36-method passing evidence belongs to its recorded source snapshot; producer-failure/cleanup assertions are now archived under #473/#474. Focused checks do not replace the selected-source and public-artifact release checks; comprehensive coverage remains deferred under [#534](https://github.com/QRun-IO/qqq/issues/534).

`SampleProcessCancellationContractTest` retains native ownership/owner-resume and unknown/missing-ID controls. `SampleEtlCancellationContractTest` retains two ordinary native commit controls and adds six ETL workflow/exception checks with independent SQL readback, as described above. The new lifecycle/report/ETL cancellation suites and producer-failure assertions are preserved outside active acceptance under [#473](https://github.com/QRun-IO/qqq/issues/473), [#474](https://github.com/QRun-IO/qqq/issues/474), [#478](https://github.com/QRun-IO/qqq/issues/478), [#479](https://github.com/QRun-IO/qqq/issues/479), [#480](https://github.com/QRun-IO/qqq/issues/480) and [#481](https://github.com/QRun-IO/qqq/issues/481). HTTP controls retain outsider rejection, unchanged state, owner cancellation and unknown-ID rejection. Original framework cancellation/ETL tests remain. The former 40-method checkpoint belongs to its historical source. Release-scoped checks retain the selected behavior; the additional scenarios remain deferred under [#534](https://github.com/QRun-IO/qqq/issues/534).

`SampleScheduledReportContractTest` retains isolated automated-owner sessions, rejected identities, ordinary Quartz/local SMTP delivery and preserved archive/history controls. New Quartz failure-notification and malformed OAuth return-shape expectations are archived under [#502](https://github.com/QRun-IO/qqq/issues/502) and [#505](https://github.com/QRun-IO/qqq/issues/505); this does not weaken the retained owner isolation checks.

`SampleScheduledReportLifecycleTest` retains explicitly configured lifecycle, real persisted-job dispatch and invalid-edit preservation controls. Eight new automatic-selection/failure-warning methods are archived under [#503](https://github.com/QRun-IO/qqq/issues/503). No database/scheduler atomicity or restart-recovery guarantee is claimed.

`SampleSavedViewSharingContractTest` adds ten native H2 and HTTP sharing cases: ordinary owner insert/edit/revoke, nonowner rejection, missing and mismatched asset/share IDs across and within owners, hidden foreign keys, and presentation customizers. It verifies stored share state and completed process responses. The fixture uses the saved-view provider without its optional record lock; it proves generic process ownership and asset binding, not recipient read confidentiality, scope enforcement, saved-view CRUD ownership or atomic concurrent reassignment. Those are outside this fixture and covered separately below where applicable.

`SampleSavedAssetOwnershipContractTest` adds 25 native H2/HTTP methods for stored-owner updates/deletes, sparse and mixed-owner writes, hidden/presented fields, actual PATCH routes, process errors and table-write permissions. Native SQL verifies ownership and report-content preservation; a caller-owned transaction changes ownership before an edit, proves the private read sees its uncommitted state, and rolls back without affecting an outside connection. Unowned-record maintenance and report ownership defaults remain compatible. The fixture does not certify configured recipient visibility, READ_WRITE shared editing, quick-view behavior, child cleanup or concurrency against external owner changes.

`SampleSavedViewAccessContractTest` retains 18 configured owner/recipient access, sharing, revocation, private-field and caller-transaction checks. Ten new dependent-cleanup methods and the associated audit fallback are archived under [#504](https://github.com/QRun-IO/qqq/issues/504). Ordinary saved-asset ownership checks remain independent.

`SampleSavedReportAccessContractTest` retains configured sharing/read/render and recipient mutation-denial controls with native H2 records and owned filesystem output. Its new dependent-share cleanup method is archived under [#504](https://github.com/QRun-IO/qqq/issues/504). These focused controls are separate from the integrated source and public-artifact release checks.

Exact method bindings and remaining acceptance gaps live in [feature-coverage.json](feature-coverage.json). Storage evidence includes native local/memory replacement and embedded SFTP failure handling; S3 transport remains synthetic. Optional FastExcel internal cleanup, live S3 and remaining saved-asset/scheduling coverage remain open under [#534](https://github.com/QRun-IO/qqq/issues/534). Complete feature-group coverage still requires every mapped scenario, but the deferred groups do not block 4.0 publication. Integrated source and public-artifact acceptance remain separate release gates.

Known Medium issues [#449](https://github.com/QRun-IO/qqq/issues/449) (API process cache across QInstances) and [#450](https://github.com/QRun-IO/qqq/issues/450) (caller session after direct automated execution) are deferred. Their new regression assertions and patches are preserved outside the active release build. The feature ledger records the selected reporting checks and deferred coverage separately from integrated release acceptance.

Scheduled owner-only management/invocation and additional nested permission requirements ([#452](https://github.com/QRun-IO/qqq/issues/452)) are deferred; their eight new policy tests are preserved outside the active build. Ordinary schedule creation/default-owner/edit/delete, actual Quartz delivery, authentication checks and lifecycle/failure behavior remain exercised. Selected authentication and isolation protections remain; the deferred policy changes are tracked in [#534](https://github.com/QRun-IO/qqq/issues/534).

Saved-report generation finishes in a temporary file before opening archive storage. The existing stream API remains supported; failed destination copy/close is not atomic. The stronger storage contract, history validation changes and rendering-only COMPLETE status are deferred in [#459](https://github.com/QRun-IO/qqq/issues/459), [#460](https://github.com/QRun-IO/qqq/issues/460) and [#461](https://github.com/QRun-IO/qqq/issues/461). Their new API/tests are preserved outside the active release source. Retained delivery checks use the existing FAILED lifecycle; new cancellation boundaries are deferred under #479.

The sharing fixture explicitly configures saved-view generic sharing metadata; automatic provider registration is deferred (#527). New literal-JSON-null, skip-only/negative-window, BSON-normalization and qualified virtual-projection refinements are deferred under #528–#531. Their archived assertions are not current acceptance evidence. Existing bounded pagination, native storage, public scalar/password and Instant controls remain.

`SampleCustomizerContractTest` exercises every table hook with native SQL checks, rejected/throwing writes and invalid customizer metadata. `SampleSecurityBackendTest` covers tenant read/export/DML isolation, multiple/all-access keys, null-policy overrides, Memory CRUD/key failures/reset isolation, and orphan/duplicate join paths. Retained joined-lock, write-only, Replace and Memory aggregate tests supply the remaining mapped cases. `SampleLookupAndJoinTest` adds native one-to-one and many-to-many SQL comparisons; the original join source cases now reuse matching association/HTTP/DML evidence.


`SampleLookupAndJoinTest` also covers TABLE/ENUM/CUSTOM possible values, selected lookup, labels, fallback and denied foreign rows, plus native RecordLookupHelper and CacheOf population/expiry/record-lock behavior. Alternate-ID search/translation work, but selected TABLE-PVS lookup still uses the primary key ([#544](https://github.com/QRun-IO/qqq/issues/544)). CacheOf currently implements only unique-key-to-unique-key source loading; its two primary-key modes and source-miss caching remain unfinished ([#545](https://github.com/QRun-IO/qqq/issues/545)). Both are Medium deferrals. Tests named for these limitations document them; they do not certify the missing capability. RecordLookupHelper objects are scoped to their caller/run, and cache/source access rules are configured explicitly.

`SampleDisplayWidgetsBrowserIT` also exercises cron and dynamic-form widgets on an owned memory record: typed values, zero, human-readable schedule, actual editor changes, native save and refresh, empty/error/malformed responses, invalid renderer references, widget/source permission denial and cron record personalization. Widget sections omit `fieldNames`; their fields are declared in a separate hidden section. Wrong-shaped cron descriptions can still blank the record page ([Medium #553](https://github.com/QRun-IO/qqq/issues/553)); the test explicitly records that deferred limitation. The native cron READ/context fix is tracked by [#563](https://github.com/QRun-IO/qqq/issues/563).

On 2026-09-24 the maintainer deferred the remaining48 comprehensive coverage groups beyond4.0. The inventory remains79/127 and continues to report gaps; it no longer blocks publication. Existing tests and required release/main-workflow checks still apply. [#534](https://github.com/QRun-IO/qqq/issues/534) links each remaining coverage group and the known future work.


### Template rendering source acceptance (#570)

`SampleTemplateRenderingTest` covers `RenderTemplateAction.execute`, `render`, and `renderVelocity` with inline strings and owned UTF-8 classpath resources. Ten JUnit methods assert literal outputs for ordered/empty loops, both condition branches, reference/literal-block escaping, caller-encoded versus raw HTML, context values containing unevaluated template syntax, missing context/keys, missing/empty code, missing resources/type, and syntax failures followed by a valid render. Run from the repository root after installing the matching source dependencies into your own Maven cache: `mvn -Dmaven.repo.local="$TEMPLATE_M2" -f qqq-sample-project/pom.xml -Dtest=SampleTemplateRenderingTest clean test`. The test runs in the sample application's QContext without a database or server and is also discovered by normal sample `test`/`verify`.

The renderer accepts strings; the consumer loads resources with the JDK and rejects missing resources before rendering. Context is optional: ordinary missing references stay literal, quiet references become empty, and missing values are false in conditions. Null code currently throws `NullPointerException`, while empty code is valid; malformed Velocity throws `ParseErrorException`. HTML encoding is explicit (the fixture uses the existing core dependency's Jsoup `Entities.escape`); raw values pass through and already encoded values are not double-encoded. This proves source rendering behavior, not automatic sanitization, template sandboxing, PDF/browser behavior, or published-candidate resolution. Only `core.templates.render` is mapped by this slice.

### PostgreSQL source acceptance (#581/#582)

`SamplePostgresAcceptanceIT` uses the existing `postgres:17-alpine` Testcontainer and PostgreSQL module, a fresh synthetic schema per test, dynamically allocated host ports, and the sample application's QContext. Fourteen methods invoke actual QQQ actions and compare results with independent JDBC connections; five existing PostgreSQL methods in `SampleDatabaseIT` add temporal, native-constraint, association/key and blocked-query evidence. Docker is required: unavailable Docker fails these tests rather than skipping them. After installing matching source dependencies into a task-owned cache, run from the repository root: `mvn -Dmaven.repo.local="$POSTGRES_M2" -f qqq-sample-project/pom.xml -Pacceptance-tests '-Dit.test=SamplePostgresAcceptanceIT,SampleDatabaseIT#testPostgres*' clean test-compile failsafe:integration-test failsafe:verify`. Normal `-Pacceptance-tests verify` also discovers these integration tests.

| Requirement | Runnable evidence and oracle |
| --- | --- |
| CRUD, types, nulls, generated keys | `testCrudTypesGeneratedKeyAndNullRoundTrip`, `testBatchWritesAndGeneratedIdentityCorrelation`; raw Java types, JDBC values, returned-key correlation, delete count and absent-row readback. Existing temporal/association tests cover UTC/host-zone behavior, identity/sequence and default-values inserts. |
| Count, aggregates, paging, sort, filters, joins | SQL comparisons in `testCountFiltersSortAndPagingAgainstSql`, `testScalarAndGroupedAggregatesAgainstSql`, `testInnerAndLeftJoinsAgainstSql`; all six aggregate operators, grouping/nulls, INNER/LEFT joins, empty pages and literal filter values. |
| Invalid inputs, denied rows, connection failure | `testDuplicateMissingTableAndMalformedInputs`, `testDeniedRowsAcrossReadAggregateAndWrites`, `testConnectionFailureAndRecovery`; exact SQLSTATEs, unchanged native rows, allowed controls and recovery. `testUnsupportedStorageFailsExplicitly` checks the backend's unsupported storage interface. |
| Commit, rollback, batch failure | `testTransactionCommitVisibilityAndConnectionClose`, `testExplicitRollbackRestoresAllWrites`, both mid-batch tests; transaction-local versus independent visibility, native aborted-transaction state, persisted partial autocommit success, explicit rollback and subsequent commit. |
| Reuse, timeout/cancellation, cleanup | `testPoolReuseCheckoutTimeoutAndOwnedCleanup` verifies physical PostgreSQL PID reuse, bounded pool exhaustion, rollback/reset when an unfinished pooled transaction closes, and native session disappearance. Existing Count/Query tests observe actual blocked PostgreSQL requests before timeout/cancellation, verify their removal and prove recovery. |

Boundaries: autocommit batches can retain earlier successful statements; callers must supply and roll back a transaction for atomic row changes. PostgreSQL sequence allocation is not rolled back. `ConnectionManager.resetConnectionProviders()` clears the registry without closing pools: the fixture explicitly destroys only its uniquely named C3P0 pool and verifies server-side cleanup. PostgreSQL has relational CRUD/aggregate/join support but no file-storage interface. These two ledger rows record source evidence only; no authentication/UI behavior, full release verification, or published-candidate resolution is claimed.

### External API mapping acceptance (#585)

`SampleApiMappingAcceptanceTest` adds an API-backed table to the sample QInstance and
runs native QQQ Get, Query, Count, Insert, Update and Delete actions against an owned
`127.0.0.1` HTTP server on an ephemeral port. The scripted provider captures the actual
method, URI, JSON body and content type; expected requests are independent literals.
All data is synthetic. Each fixture stops its server, releases withheld responses and
joins its executor threads, and fails if expected requests never arrive.

| Contract | Evidence |
|---|---|
| Six actions and translation | GET key path; nested/renamed fields; UTF-8 wrapped POST; returned generated key; wrapped PUT array; DELETE key path, count and subsequent 404. |
| Pagination and count | Five ordered records over three pages; exact offsets; full final page followed by empty page; explicit limit/skip; escaped equality filter; provider total 17 despite a one-record count response page. |
| HTTP failures | 400 and 503 for all six actions; native bounded GET/query 503 retries; each failed mutation sends exactly one request. |
| Timeouts | Server consumes then withholds each action's response; configured 300 ms socket timeout completes within 3 seconds; mutations are not retried. |
| Missing/malformed/wrong bodies | GET and count reject unusable resource/count responses; query rejects malformed JSON and scalar wrapper/list entries; insert returns record errors for bad responses or missing generated key and sends POST only once. |
| Missing results | GET 404 returns null; native query empty body, JSON null and empty arrays return no records; no extra pagination request. |
| Unsupported provider mappings | Unsupported criteria and multi-key delete fail before HTTP. |

The test provider customizes URL/filter/count and JSON envelope hooks documented in
`docs/metaData/Backends.adoc`. It retains native HTTP execution, pagination and retry
behavior. This evidence covers the scripted provider's schema and single-key delete
contract; generic strict field-type validation, other providers, OAuth/security and
published-candidate acceptance have separate scope. PUT/DELETE use valid bodyless 204
responses. Scripted readback verifies response mapping; it does not claim remote service
persistence or transaction semantics.

After installing this checkout's source modules into a **new task-specific Maven cache**,
run from the repository root:

```sh
mvn -B -ntp -Dmaven.repo.local="$API_ACCEPTANCE_M2" -f qqq-sample-project/pom.xml \
  -Dtest=SampleApiMappingAcceptanceTest test
mvn -B -ntp -Dmaven.repo.local="$API_ACCEPTANCE_M2" -f qqq-sample-project/pom.xml -Pacceptance-tests clean verify
python3 -m unittest discover -s qqq-sample-project -p test_feature_coverage.py
python3 qqq-sample-project/verify-feature-coverage.py --stage source --report-only
```

Only `backend.api.mapping` receives these method bindings. Source evidence uses the
source-built dependencies from the reviewed #803 base `9a526e330`; published artifacts
and the combined release integration remain separate gates.

Full `-Pacceptance-tests clean verify` passes on this base: 692 unit tests (including
all 11 new API contracts) and 61 integration/browser tests, with zero failures,
errors or skips. Checkstyle reports zero violations and class coverage is 38/40
(95%), meeting the unchanged gate. This run uses the task-owned Maven cache,
source-built QQQ artifacts and the POM-default Material 0.41.0 dependency. The earlier
plain `verify` run omitted this required profile; its 90% class coverage was not a
new production gap. The source inventory report remains incomplete for other rows;
combined release and published-candidate validation remain separate.

### Local filesystem acceptance (#587 / #590)

`SampleLocalFilesystemAcceptanceTest` uses the sample QInstance, real QQQ record and
storage actions, and JUnit-owned temporary directories. Independent `Files` reads
check stored bytes, preserved source files and failed-write effects. Its Apache-2.0
fixture adds no production dependencies. Source evidence is limited to the two local
filesystem rows; combined release and published-candidate validation remain separate.

| Requirement | Evidence and boundary |
|---|---|
| ONE insert/query/count/delete | Real files, nested names, byte contents, file size/base name, heavy-field selection and native deletion readback. |
| CSV/JSON and cardinality | MANY parses CSV and JSON objects/arrays, Unicode/quotes/newlines, CSV empty/missing cells and omitted JSON values; glob selection and count are checked. ONE retains whole bytes for either format and does not invoke the MANY-only post-read hook. Explicit JSON null remains a null field. |
| Update and MANY mutation | All filesystem updates and MANY insert/delete are explicitly unimplemented; tests require the native refusal and unchanged files. |
| Storage | Binary streams, shorter replacement, nested Unicode/space paths, file URL, missing input, canonical traversal/symlink refusal and native byte readback. Record query/insert/delete confinement also rejects outside-table targets; an in-table symlink remains usable. |
| Parsing failures | Malformed/truncated CSV/JSON and wrong JSON element type fail without source changes. Duplicate CSV headers are suffixed (`name`, `name 2`); a missing cell is null. CSV/JSON adapters preserve source numeric values; invalid numeric text raises `QValueException` on typed access, with no claim of eager whole-file type validation. |
| Customizers | MANY post-read transformation affects returned records while preserving file bytes; thrown customizer exceptions abort query/count. |
| OS failures | Owned files have POSIX permissions removed, independently confirmed unreadable/unwritable, then restored before cleanup. Run as an unprivileged user; elevated/root execution cannot certify OS denial. |
| Partial failures | A middle ONE insert fails against a file-as-parent while earlier/later writes persist. A failing source copied through the actual storage output stream leaves its prefix and truncates old contents. Storage is non-atomic; existing [#459](https://github.com/QRun-IO/qqq/issues/459) remains unchanged. |

Local files have no transport credential or connection-timeout contract. SFTP/S3
transport acceptance belongs to its separate inventory rows. No network failure is
simulated to fill those inapplicable local cases.

The initial source-built `d1dac171a` run reproduced seven failing assertions across
three defects. Minimal corrections accompany the acceptance fixture:

- `JsonToQRecordAdapter` translates explicit JSON null before the Serializable cast.
- `SharedFilesystemBackendModuleUtils` treats filenames as filesystem paths rather
  than concatenating unescaped URI text; space and Unicode queries now pass.
- `AbstractFilesystemAction` checks canonical table confinement for local listing,
  writes and deletes. Traversal insert/delete and outside-target symlink read/write
  regressions preserve their sentinels, with a successful in-table symlink control.
  All probes stay inside the owned outer temporary directory.

Review regression: low-level `deleteFile` retains its idempotent missing-file no-op
before resolving context or checking confinement. The new test repeats deletion of
an absent outside path and dangling symlink, then creates that target and requires
both deletion attempts to fail while preserving the target and link. The original
`FilesystemBackendModuleTest` assertions remain intact; its existing-file test now
initializes/clears QContext for backend resolution. The S3 module fixture uses the
existing Localstack random-port option to avoid another run's fixed ports.

Unsupported mutations and non-atomic streams keep their existing behavior. File
customizers remain MANY-only, and parsed numeric type conversion remains lazy. The
adapter's documented collision limitation for pre-suffixed CSV headers remains; the
fixture certifies identical duplicate headers (`name`, `name`) and missing cells.

With the checkout's source dependencies installed in a new task-owned Maven cache:

```sh
mvn -B -ntp -Dmaven.repo.local="$FILESYSTEM_ACCEPTANCE_M2" \
  -pl qqq-backend-module-filesystem clean verify
mvn -B -ntp -Dmaven.repo.local="$FILESYSTEM_ACCEPTANCE_M2" \
  -pl qqq-backend-core -Dtest=JsonToQRecordAdapterTest test
mvn -B -ntp -Dmaven.repo.local="$FILESYSTEM_ACCEPTANCE_M2" -f qqq-sample-project/pom.xml \
  -Dtest=SampleLocalFilesystemAcceptanceTest test
mvn -B -ntp -Dmaven.repo.local="$FILESYSTEM_ACCEPTANCE_M2" -f qqq-sample-project/pom.xml \
  -Djdk.httpclient.HttpClient.log=errors,channel -Pacceptance-tests clean verify
python3 -m unittest discover -s qqq-sample-project -p test_feature_coverage.py
python3 qqq-sample-project/verify-feature-coverage.py --stage source --report-only
```

The full-profile command is required; plain `verify` omits the packaged/browser
evidence needed for the sample coverage gate. No test or coverage threshold is lowered.

Review-fix verification: all 23 filesystem methods and 9 JSON-adapter tests pass.
The entire filesystem module `clean verify` succeeds: 110 discovered tests, 106
passed and four existing disabled JSON-serialization tests in the local/S3 metadata
classes. Checkstyle and coverage checks pass unchanged. The module's existing
non-blocking static-analysis configuration reports 38 SpotBugs and 329 PMD findings;
these checks were not disabled or made less strict. Python ledger tests pass 23/23.

Final exclusive verification at `9b3bc4cea` (the reviewed #823 commit `fcf3b0d10`
cherry-picked onto `ff0e98890`) passes **743 unit + 78 integration/browser tests**
with zero failures, errors or skips. Checkstyle has zero violations, and class
coverage is 39/41 (95.12%), meeting the unchanged threshold. The command above was
run once with the sample/broker slot explicitly reserved and bounded HTTP
`errors,channel` diagnostics enabled. No tests were retried, skipped or relaxed.
The source report verifies 89/127 features; both owned filesystem rows have no gaps,
while the overall source gate remains incomplete for other rows.

The earlier `ff0e98890` attempt ran 741 unit tests with one HTTP header EOF in
`SampleInteractiveProcessTest.testLegacyBackAndUnknownResumeDoNotRunWork`; Failsafe
was not reached. Its HTTP wildcard port was 49202 and Artemis loopback port was
61616. Those logs remain preserved: this successful exclusive run does not establish
the earlier EOF's cause or claim resolution of #824.

Local review logs are retained under `/private/tmp/qqq-587-acceptance.fB6Bs0/`:
`backend-module-red.log`, `missing-delete-sample-red.log`,
`filesystem-full-verify.log`, `core-adapter-rereview.log`,
`sample-filesystem-rereview.log`, `sample-full-rereview.log`,
`http-error-port-evidence.txt` and `listeners-at-sample-error.log`. Final evidence is
`sample-exclusive-823.log`, `exclusive-run-command.txt`,
`python-exclusive-823.log` and `source-report-exclusive-823.log`, with copied JUnit
reports under `exclusive-823-reports/`.
Only `backend.filesystem.local` and `backend.filesystem.formats` receive these source
bindings; original requirements and supported-case assertions are preserved.

### S3 source acceptance (#588)

`SampleS3AcceptanceIT` runs real QQQ actions against an owned
`localstack/localstack:1.4` container with dynamically mapped ports, a unique bucket
and a separate namespace per case. It reuses the sample QInstance and Field Lab
metadata. A test-only backend subtype injects the emulator endpoint through the
existing `S3Utils` setter; calls use the actual production action implementations
and AWS SDK. A separate SDK client reads native objects, keys, metadata and multipart
uploads. Cleanup aborts owned uploads, deletes and verifies empty namespaces, deletes
the owned bucket, closes both clients and stops only the owned container. No real
AWS credentials or shared service are used.

| Contract | Source evidence and boundary |
|---|---|
| ONE record APIs | Insert/query/count/delete, whole CSV/JSON bytes, nested Unicode/space names, size/base name, heavy-field selection and exact native readback. Update refuses with the provider's `NotImplementedException` and leaves native objects unchanged. |
| MANY record APIs | CSV rows and JSON arrays/objects, quoted multiline Unicode CSV, long values, explicit JSON null, count, post-read transformation and customizer failure. MANY insert/delete refuse without changing source objects. |
| Raw storage | Binary round trip, shorter replacement, content type, URL mapping and native ACL metadata. Emulator ACL changes do not establish public AWS access or IAM enforcement. |
| Multipart | A 6 MiB + 17 byte write has an in-progress native upload and no published object before close; completion yields exact bytes and no remaining upload. |
| Missing/malformed | Missing prefix is empty; missing bucket/key retain native diagnostics; malformed CSV/JSON and invalid glob fail without mutation. Invalid glob is a configuration check, not comprehensive S3 key validation. |
| Failures | Owned HTTP fixtures return `AccessDenied`, `InvalidAccessKeyId` and `SignatureDoesNotMatch` for read/write and hold a received request for a bounded 500 ms socket-read timeout. These verify SDK/provider propagation, not actual IAM, signing enforcement, or connection-establishment timeout. |
| Partial write | An input source fails after six bytes; closing the real output stream publishes that prefix over the old object. This documents non-atomic storage (#459); it does not claim rollback, failed-multipart abort, or cancellation. |

The row `backend.filesystem.s3` stays **pending**. The active native regression
`rawStorageOffsetWritesExactSlice` first failed against the old production stream:
writing UTF-8 `0123456789` with `write(bytes, 2, 5)` and closing stored `234`, not
`23456`. [#829](https://github.com/QRun-IO/qqq/issues/829) corrects the final copy to
use the remaining byte count and validates the entire slice with
`Objects.checkFromIndexSize` before copying or uploading. Module regressions cover
nonzero offsets (including offset greater than length), zero-length slices,
null/negative/out-of-range/overflowing bounds, and multipart crossings from both
empty and accumulated buffers. Invalid large slices previously started an upload;
the regression now requires no upload and unchanged buffered contents. The obsolete
reproduction patch is removed in favor of these executable tests.

At the #829 checkpoint, invalid S3 key/reference cases and failed-multipart cleanup
remained unverified; the #838 evidence and original-scope reassessment below update
those gaps. Slice validation introduces no key policy. Live AWS and published-candidate
acceptance remain separate; existing provider and release deferrals are unchanged.

The reviewed [#826](https://github.com/QRun-IO/qqq/pull/826) dependency is merged for
its JSON-null fix; its local-filesystem evidence remains separate. After installing
matching source artifacts into an isolated task Maven cache, the focused Docker
fixture can run without the shared embedded Artemis port:

```sh
mvn -B -o -nsu -Dmaven.repo.local="$S3_ACCEPTANCE_M2" -f qqq-sample-project/pom.xml \
  -Pacceptance-tests -Dit.test=SampleS3AcceptanceIT \
  clean test-compile failsafe:integration-test failsafe:verify
```

Normal `-Pacceptance-tests clean verify` discovers the same IT class. Full sample
runs must be scheduled exclusively because other sample classes share embedded
Artemis port 61616. Focused verification does not claim clean-cache public-artifact
acceptance.

After #829, the focused acceptance Failsafe run passes 13/13 S3 cases, including
the previously failing native slice regression, with zero failures, errors or skips.
The full filesystem module `clean install` (including verify) passes 110 tests with
four existing skips, zero Checkstyle violations and all coverage checks satisfied.
Existing non-blocking analysis still reports 38 SpotBugs and 329 PMD findings; no
gates were relaxed. The module JAR and the isolated-cache JAR used by the sample
match byte-for-byte. Requiring the S3 row explicitly still fails the feature gate,
as intended while its remaining gaps are open.

Full verification at signed source `8fe813138794cc6d891145a95197195fa315acff` passes
**760 unit + 102 integration/browser tests (862 total)**, with zero failures, errors
or skips, including all 13 S3 cases. The single reserved run completed in 9:14:

```sh
mvn -B -o -nsu -Dmaven.repo.local="$S3_ACCEPTANCE_M2" \
  -Djdk.httpclient.HttpClient.log=errors,channel \
  -f qqq-sample-project/pom.xml -Pacceptance-tests,data-qbit-acceptance clean verify
```

The data QBit fixture was rebuilt from the same checkout into the isolated cache
before verification. Checkstyle reports zero violations; JaCoCo class coverage is
39/41 (95.12%), satisfying the unchanged gate. Line coverage is 877/969 (90.51%).
All 46 top-level Python checks pass; the current source ledger reports 91/127
verified features while S3 remains pending for the boundaries above. The broker
reservation was released after exit 0, with no retry or test suppression. The local
full log is `/private/tmp/qqq-829-sample-full-verify.log`; the pre-fix native failure
is `/private/tmp/qqq-829-native-offset-red.log`. These are source results, not
published-artifact or live AWS acceptance.


#### Explicit multipart failure cleanup (#838)

Three native regressions exercise explicit SDK failures during the second part of a
write, the final part on close, and completion. A narrow JDK proxy delegates all other
calls to the real SDK client. Before throwing, the independent oracle requires the
same real upload ID and native part sizes: one 5 MiB part for a part failure, or
5 MiB plus 1,048,593 bytes before completion. The write case supplies 10 MiB; the
close/completion cases supply 6 MiB + 17 bytes. The caller uses the real
`StorageAction` output stream in try-with-resources, so close is attempted even when
write fails. Existing-object controls compare every native key and byte with the
pre-write snapshot; the final-part case also verifies no new object is published.

Against the published `1a94158` implementation, all three cleanup assertions failed:
each upload ID and its uploaded parts remained after the exception and close attempt.
The write failure was retried by close and still left the first part. The original
red run is `/private/tmp/qqq-588-multipart-cleanup-red.log` (three failures, zero
errors/skips), with an unchanged 13/13 passing baseline. The expanded red runs are
`/private/tmp/qqq-838-module-red.log` and
`/private/tmp/qqq-838-native-abort-red.log`.

The minimal [#838](https://github.com/QRun-IO/qqq/issues/838) correction makes the
stream terminal on SDK failure and attempts to abort only its initiated upload.
The original exception is rethrown; an abort exception is attached as suppressed.
Repeated close is a no-op and later writes reject reuse without any further SDK
calls. Native regressions require no remaining upload after successful abort and
unchanged native object state. A fourth native case injects abort failure: it checks
the original exception identity, supplemental abort error and honestly retained
upload/parts. Independent test teardown then aborts that residual upload and verifies
empty namespaces before deleting the owned bucket/container. Module regressions
also cover terminal behavior, successful content metadata and redundant close.

This is distinct from #459: the explicit failed upload/completion calls do not publish
a partial replacement in these probes; the existing source-copy partial-publication
assertion remains unchanged. Injected faults do not prove AWS IAM behavior or rollback
when completion succeeds server-side but its response is lost. Actual invalid
key/reference cases, metadata-to-SDK credential propagation and connection-establishment
timeout remain acceptance gaps; S3 stays pending. Ambiguous completion and public-artifact
acceptance are separate unverified limits, not newly imposed atomicity requirements. No new storage SPI,
mandatory atomicity contract or provider retry policy is introduced.


Focused #838 verification passes all **17 native S3 cases**, retaining all 13 original
cases and adding the four failure/cleanup cases. Full filesystem `clean install`
(including verify) passes 114 tests with four existing skips and no failures/errors;
Checkstyle reports zero violations and coverage gates pass. SpotBugs reports 38 and
PMD 331 non-blocking findings; the two new PMD warnings concern the narrowly scoped
runtime-exception cleanup catch and reference identity guard against self-suppression.
No rules were suppressed or gates changed. All 46 top-level Python checks pass.

The combined reviewed base is `4da03460068dd9cabea50caf65bf5405754d716f`; changed
source dependencies were rebuilt into the task-owned cache, and the installed
filesystem JAR matches the tested module JAR byte-for-byte. Green logs are
`/private/tmp/qqq-838-filesystem-verify-install.log` and
`/private/tmp/qqq-838-native-green.log`. The full sample verification below certifies
this cleanup delta separately from the earlier #829 run.


#### Remaining original #588 scope

The original issue asks for invalid-path, permission/credential-failure and connection-
timeout evidence alongside supported record/storage behavior. It does not require a
new key-rejection policy or general AWS IAM certification. Existing methods cover the
record/cardinality/format contracts and their unsupported-operation boundaries,
missing and malformed input, source-copy partial publication (#459), and now explicit
multipart cleanup (#838). The row remains pending while the following work is reviewed
and proved; these are proposed checks, not passing-test claims:

- **Keys/references:** [AWS defines flat UTF-8 keys](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html),
  with a 1,024-byte limit including prefixes. Valid examples include `folder/./file.txt`
  and `folder/../file.txt`; some excess parent segments are invalid. Add native SDK
  controls at 1,024 and 1,025 bytes (including multibyte text and the complete QQQ
  prefix), plus a key whose parent-segment count exceeds all preceding normal segments.
  Compare SDK rejection with public QQQ behavior and unchanged object/upload state;
  document emulator differences instead of relabeling successful LocalStack writes as
  AWS validation. Exercise valid dot segments as exact opaque keys, with distinct
  normalized-key sentinels to detect aliasing on read/write/delete. Inspect missing or
  empty QQQ references separately; do not assume a filesystem traversal rule applies.
- **Credentials/permission:** current owned HTTP 403 cases prove SDK error propagation,
  not selection of credentials from QQQ metadata or AWS permission enforcement. Use
  synthetic metadata credentials through the production client-construction path and
  verify their outgoing signing identity locally without exposing secrets. An allowed
  control and denied operation should prove propagation and no writes on denial. Only
  an actual provider contract unavailable locally calls for a QRun-controlled sandbox;
  broad IAM certification is not a new acceptance requirement.
- **Connection timeout:** the current fixture receives the request and delays its
  response, so it proves a socket-read timeout. Establish a bounded owned network
  fixture that actually stalls connection establishment and assert the precise SDK
  cause plus native no-mutation/recovery. Connection refusal or an injected exception
  alone must not be labeled a real connection timeout.

No requirements, existing negative cases or pending status were removed by this
reassessment; no blanket dot-segment rejection or new provider policy was added.


#### Full sample verification of #838

At independently reviewed runtime commit `ecaccd04645430262c566424535438dc59bb31e0`,
one exclusive full run passed **760 regular tests plus 107 integration/browser tests**
(**867 total, zero failures/errors/skips**). This includes all 17 native S3 cases and
the data-QBit profile. Maven completed `clean verify` with **BUILD SUCCESS in 9:11**;
Checkstyle reported zero violations, and JaCoCo gates passed with 39/41 classes
(95.12%) and 877/969 lines (90.51%) covered.

```sh
env -u JAVA_TOOL_OPTIONS mvn -B -o -nsu \
  -Dmaven.repo.local="$S3_ACCEPTANCE_M2" \
  -Djdk.httpclient.HttpClient.log=errors,channel \
  -f qqq-sample-project/pom.xml \
  -Pacceptance-tests,data-qbit-acceptance clean verify
```

The task-owned cache contained matching source artifacts, including the rebuilt
filesystem and Mongo modules; this was not a fresh empty-cache or published-artifact
check. Run handle `44447` reached exit 0 without restart; the broker reservation was
then released to MAIN/Sartre. The log is `/private/tmp/qqq-838-sample-full-verify.log`,
with counts/coverage in `/private/tmp/qqq-838-full-acceptance-summary.json` and copied
reports in `/private/tmp/qqq-838-full-reports`. These are source-verification results.
The S3 row remains pending for the original-scope gaps listed above; this full run
does not turn provider limitations or proposed checks into passing evidence.


### SFTP source acceptance (#589)

`SampleSftpAcceptanceTest` starts the existing Apache SSHD 2.16.0 server on
`127.0.0.1:0` with a fresh JUnit temporary virtual root and synthetic credentials.
Actual QQQ record/storage actions run in the sample QInstance; independent native
file reads check bytes, deletion, denied writes and partial persistence. Cleanup
stops only the owned listener and checks connection refusal; JUnit removes its root
and generated host key. No HTTP server, broker, external credentials or new
dependencies are used by this focused fixture.

| Scope | Evidence and boundary |
|---|---|
| Record operations | ONE insert/query/count/delete with native bytes, sizes/names, Unicode paths and nested exact selection. Broad listing is nonrecursive; remote parent directories must already exist. |
| Formats and hooks | CSV/JSON MANY preserves Unicode, quoted/newline content, missing fields and JSON null; customizers alter returned values only, and customizer errors propagate. ONE keeps whole bytes and bypasses MANY-only hooks. |
| Storage/failure effects | Binary stream round trip and shorter replacement; missing paths, malformed/truncated data and wrong JSON element type fail explicitly. Invalid path/batch tests retain earlier/later successful writes; a failed source copy leaves the remote prefix. This is not atomic publication ([#459](https://github.com/QRun-IO/qqq/issues/459)). |
| Credentials/permissions | Real SSH rejects bad password and username; native OS permissions deny reads/writes, protect native contents, and allowed controls pass after restoration. Run unprivileged; tests do not skip OS denial. |
| Timeout | A real TCP peer accepts the client's SSH banner but never replies. The existing SSHD `org.apache.sshd.config.auth-timeout` is temporarily lowered to 500 ms and restored; failure must return within five seconds and the peer observes client closure. QQQ exposes the resulting `SshException: Session is being closed`, not a dedicated timeout exception. Successful reconnection is checked. |
| Unsupported operations | Updates, MANY insert/delete, public URLs and make-public fail explicitly without file changes. No atomic write, recursive directory listing or application-level timeout policy is claimed. [#463](https://github.com/QRun-IO/qqq/issues/463) action reuse and [#632](https://github.com/QRun-IO/qqq/issues/632) permission-exception test fragility remain separate. |

After building matching branch artifacts into a fresh task-owned Maven cache, run:

```sh
mvn -B -ntp -Dmaven.repo.local="$SFTP_ACCEPTANCE_M2" -f qqq-sample-project/pom.xml \
  -Dtest=SampleSftpAcceptanceTest test
mvn -B -ntp -Dmaven.repo.local="$SFTP_ACCEPTANCE_M2" -pl qqq-backend-module-filesystem \
  '-Dtest=SFTP*Test' test
mvn -B -ntp -Dmaven.repo.local="$SFTP_ACCEPTANCE_M2" -pl qqq-backend-module-filesystem clean verify
```

The original 19-case run at `df9a5abae` had five failures, retained in
`nineteen-contracts-red.log`: ignored globs and outside-table record/storage reads,
writes and record deletion. Fixes [#830](https://github.com/QRun-IO/qqq/issues/830)
and [#831](https://github.com/QRun-IO/qqq/issues/831) apply the existing Java NIO glob
syntax to table-relative names and resolve remote paths/link targets before I/O.
New files validate their existing parent; deletion keeps the original alias path.
The boundary is the configured table root, including a configured root alias, and
retains each API's existing leading-slash convention. This is a pre-operation check,
not an atomic guarantee against concurrent remote filesystem changes.

Focused sample evidence now passes **23/23**, zero failures/errors/skips. It includes
native sentinels, nested direct glob selection, safe leaf/parent links, configured
relative/leading-slash roots, and all original failing cases. Missing SFTP deletion
still returns zero deletions and one record error (unlike local missing-file no-op).
A separate raw SSHD probe confirms that this fixture's server cannot delete through
a symlinked parent (`SSH_FX_NO_SUCH_FILE`); that existing limitation is asserted with
unchanged target bytes. Leaf-link deletion succeeds and retains its target. No
account-root escape or unauthorized application access is claimed.

Logs and copied JUnit evidence are under `/private/tmp/qqq-589-acceptance.FrwS1r/`:
`nineteen-contracts-red.log`, `glob-module-red.log`, `links-root-red.log`,
`sftp-23-sample.log`, `native-provider-delete-probe.log` and
`storage-permission-recovery.log`. Full filesystem module `clean verify` passes:
108 executed tests, zero failures/errors, plus four pre-existing disabled metadata
tests (112 total); zero
Checkstyle violations, 61/61 classes covered and the unchanged coverage gates pass.
`filesystem-module-verify-final.log` retains that result and nonfatal SpotBugs/PMD
advisories. The ledger verifier's Python suite also passes 23/23.
At source head `cbbddae72`, the exclusive full run passes **772 unit tests**
(766 ordinary tests plus six source-only data-QBit host tests) and **78 integration/
browser tests**, all executed with zero failures, errors or skips. Class coverage is
39/41 (95.12%); Checkstyle and unchanged coverage gates pass. The data-QBit JAR was
built from the same source into the owned cache before enabling both profiles.
Only `backend.filesystem.sftp` receives these source bindings; original requirements
remain, and no published-candidate or complete release-gate claim is made.

The first full run at the same head failed one exact-output child-process assertion:
`JAVA_TOOL_OPTIONS` added a 76-character JVM startup banner to the captured output.
A separate child-process comparison reproduced the exact difference. The unchanged
method passed with diagnostics supplied as a Maven system property, then the full
run passed with that invocation correction. No source assertion, timeout or gate
was changed. Original and corrected reports remain archived under the task log
root; final evidence is `sample-exclusive-cbbddae72-corrected.log` and
`exclusive-cbbddae72-corrected-reports/`. The exclusive broker slot was released
when the corrected run terminated.

After installing matching framework sources into the owned cache and obtaining an
exclusive sample/broker slot, reproduce with:

```sh
mvn -B -ntp -Dmaven.repo.local="$SFTP_ACCEPTANCE_M2" -Drevision=4.1.0-SNAPSHOT \
  -f qqq-sample-data-qbit/pom.xml clean install
env -u JAVA_TOOL_OPTIONS mvn -B -ntp -Dmaven.repo.local="$SFTP_ACCEPTANCE_M2" \
  -Drevision=4.1.0-SNAPSHOT -Djdk.httpclient.HttpClient.log=errors,channel \
  -f qqq-sample-project/pom.xml -Pacceptance-tests,data-qbit-acceptance clean verify
```


### PDF source acceptance (#571, verified)

`SamplePdfAcceptanceTest` renders a first-party HTML report in real sample metadata
context and reopens the serialized bytes with the existing transitive PDFBox parser.
Four tests assert exact text on two pages, a local generated PNG embedded in the PDF,
malformed HTML repaired without losing text, and missing-image tolerance with a
present-image control. Direct OpenHTMLToPDF controls show that its strict XML input
rejects the malformed fixture and that it also tolerates the absent image. An owned
output stream fails after 64 bytes: the real renderer reaches that sink, QQQ wraps the
same `IOException` in `QException`, and a fresh conversion succeeds. This is failure
propagation and recovery evidence, not atomic-output or rollback evidence. Tests use
30-second bounds, close PDF documents/streams, restore QContext, and let JUnit remove
per-test assets; no server, broker, external/private assets or new dependencies.

Focused source tests pass 4/4, including Checkstyle with zero violations. Temporarily
removing the report's CSS page break produces the expected 2-versus-1-page failure;
the original fixture is restored. The isolated Maven cache is
`/private/tmp/qqq-571-pdf-4da-m2`; all sample reactor dependencies were rebuilt from
`4da03460068dd9cabea50caf65bf5405754d716f` (dependency setup skips tests, not a full gate).
Logs are `/private/tmp/qqq-571-focused-final.log`,
`/private/tmp/qqq-571-pagination-mutation.log` and
`/private/tmp/qqq-571-dependencies.log`. Reproduce the focused check from repository root:

```sh
mvn -o -f qqq-sample-project/pom.xml \
  -Dmaven.repo.local=/private/tmp/qqq-571-pdf-4da-m2 \
  -Dtest=SamplePdfAcceptanceTest test
```

All 46 top-level Python checks pass (`/private/tmp/qqq-571-python.log`). The ledger
report resolves all four fully qualified test bindings; its required-feature gate
initially exited 1 solely because scenario review was pending. Independent review
and the combined full sample run now pass: 817 regular plus 110 integration tests,
zero failures/errors/skips, at `fd7743b3773e02cb0ee59e570191ede2e48b40f2`.
The source ledger is verified; its existing post-4.0 disposition is unchanged and
this grants no new release approval. PDFBox is also used by the renderer internally, so parsing is an independent
output assertion, not a second rendering engine or visual-fidelity certification.
Custom-font behavior, remote assets and published-candidate acceptance are not claimed.


### Maintenance sample acceptance (#574)

`SampleMaintenanceAcceptanceTest` invokes the existing registered garbage-collector and column-statistics processes in the sample application's QContext. Each test owns a unique in-memory H2 database initialized with the existing sample schema; native JDBC seeds and full-column readback independently verify persisted results. The fixture shuts down that database, removes its process state and restores ordinary and named caller context. No server, broker, provider account, runtime dependency or application schema is added.

| Original scenario | Runnable evidence |
| --- | --- |
| Configured expired-record cleanup | `testConfiguredExpiryAndRepeatPreserveNonexpiredRecords` checks the configured 30-day threshold on both sides, null/nonexpired preservation and a repeat run; `testExplicitCutoffPreservesEqualAndLaterDates` checks the documented override and strict cutoff. |
| Column statistics match fixture values | Existing `SampleAggregateColumnStatsContractTest.testRegisteredProcessPublishesNativeCountsAndStats` compares real process output and serialization with independent SQL groups, counts, percentages, sum, average and extrema. Its scoped READ and denied joined-table tests are reused without changing or duplicating their implementation. |
| Nonexpired/denied records untouched | Separate READ- and WRITE-lock tests compare every persisted column, delete an allowed control in the same invocation, retain null-owner rows and then delete the other owner's expired row only after granting its existing security key. |
| Empty table | `testEmptyCleanupAndStatistics` repeats empty cleanup and compares all empty statistics with native SQL, including null sum/average/extrema. |
| Invalid configuration | Missing cleanup field, missing statistics field and malformed supplemental statistics configuration fail without changing any persisted row; a corrected statistics request succeeds. |

The garbage-collector producer has **no default schedule**. These tests call its process synchronously and do not certify timer behavior or descendant-table cascading (`joinedTablesToAlsoDelete` is null). Cleanup's existing table actions use SYSTEM input; the denial cases exercise configured record-security locks, not a new USER table-permission policy. Column statistics retain their existing USER READ checks. Native evidence here is H2 source acceptance, not a published-candidate or external database claim.

After installing matching framework source artifacts into a task-owned cache, run:

```sh
mvn -B -ntp -Dmaven.repo.local="$MAINTENANCE_M2" -f qqq-sample-project/pom.xml \
  -Dtest=SampleMaintenanceAcceptanceTest,SampleAggregateColumnStatsContractTest clean test
python3 qqq-sample-project/test_feature_coverage.py
```

On source base `4da034600`, the focused clean run passed **8 maintenance +9 reused column-statistics tests**, with zero failures/errors/skips and Checkstyle zero. No product defect was confirmed. Independent review and the combined full acceptance/data-QBit run now pass at `fd7743b3773e02cb0ee59e570191ede2e48b40f2`: **817 regular +110 integration tests**, zero failures/errors/skips. `core.maintenance` is verified at source stage. The packaged launcher tests also verify propagation of the run's isolated broker port. When also enabling `data-qbit-acceptance`, first build the matching `qqq-sample-data-qbit` source fixture into that same cache; its six additional host tests are source-only evidence.


## Query statistics: bounded source evidence (#564)

`SampleQueryStatisticsAcceptanceTest` and
`SampleQueryStatisticsAcceptanceRegressionTest` use canonical sample `person`/`pet`
metadata, unique H2 databases, the real manager and public consumer interface, and
`QueryStatMetaDataProvider`/`QQQTablesMetaDataProvider`. Explicit fixture SQL creates
the statistics schema; native JDBC independently checks persisted values. These
fixtures use no persistence mocks, network listeners, broker, new dependencies or
launched application changes. They restore manager settings, stop/join workers,
clear context and close owned databases. New Java files carry Apache-2.0 headers.

Twenty-two sample methods cover direct, plain and association-buffered counts (including
3/100/105-row batches and tails); exact UTC timing/session/SQL and criterion/order/join
records; backend/table opt-outs; disabled startup; thresholds and throwing consumers;
actual native query/storage failures; preterminated plain pipes; explicit and
scheduled insertion; and lifecycle/context isolation. The joined query verifies
actual descending child-name delivery, not just generated SQL. An explicit flush
preserves the caller's real backend transaction and action stack as well as its
instance and session. Separate native databases prove that a foreign instance
reaches neither consumers nor the configured statistics store, including after a
restart switches the configured instance.

The seven former failing probes were reproduced against the original matching
source, then passed against the corrected core before being moved from the explicit
`DefectProbe` class into ordinary `RegressionTest` discovery:

| Issue | Correction and evidence |
| --- | --- |
| #833 | Buffered counts include forwarded records and pending tails; three association cases retain actual parent/child delivery and match native counts. Core pipe tests also cover zero rows and standalone buffered pipes before/after final flush. |
| #834 | Flush captures/restores all caller context fields on success, empty/disabled exits and supplier/storage failure. A context-free worker remains context-free. |
| #835 | Foreign contexts are rejected before consumers or enqueueing. Both native databases retain only their own source-session statistics across replacement. |
| #836 | Start retires the previous scheduler; start/stop serialize with a running flush. Jobs capture their configuration/generation, and a consumer finishing after restart cannot enqueue into the new batch. A controlled native flush/restart checks the previous worker terminates and only the replacement supplier is used afterward. The bounded child JVM checks two starts/one stop leave zero workers. |
| #837 | Flush-scoped suppression prevents its own work reaching consumers or storage, even when statistics-table collection is enabled. Repeated explicit/scheduled flushes stay stable; legitimate application queries of the statistics table remain observable. Supplier/storage failure restores collection. The original observation was one extra self-generated row per flush, not demonstrated infinite recursion. |

Statistics measure completed **backend actions**, not whole-request success. The
positive `testPostQueryRejectionRetainsCompletedBackendMeasurement` verifies both
the propagated post-query customizer error and the legitimate backend measurement
in consumers/native storage. It replaces the withdrawn suppress-stat probe. Native
backend failure produces no completed measurement. A preterminated plain pipe
records zero delivered rows; concurrent query cancellation is not certified.
Counts are consumer-only fields, not persisted columns. Structured order records
retain field names, not direction; join records retain table IDs, not join type.
SQL text supplies additional detail. Failed storage batches are still dropped,
without retry; the test proves loss and recovery on the next fresh query.
Lifecycle calls can wait for an in-flight storage operation; no new timeout or
storage-failure policy is introduced.

`core.observability.query_statistics` remains **pending** for independent review,
combined full-sample verification and remaining concurrent query termination scope.
All existing requirement/negative/stage/deferral fields, including #525/#526 and
#527–#531 boundaries, remain unchanged. The required-feature gate must remain red;
passing mapped tests alone do not certify the entire feature.

Reproduce with Java 21 and a dedicated matching-source cache:

```sh
QUERY_STATS_M2=/private/tmp/qqq-564-e74cb40e-m2
# Initial matching-source artifact preparation (tests run separately below).
mvn -nsu -Dmaven.repo.local="$QUERY_STATS_M2" -DskipTests install
# After core edits: full core verification/install, with normal quality gates.
mvn -nsu -Dmaven.repo.local="$QUERY_STATS_M2" -pl qqq-backend-core install
mvn -nsu -Dmaven.repo.local="$QUERY_STATS_M2" -f qqq-sample-project/pom.xml \
  -Dtest=SampleQueryStatisticsAcceptanceTest,SampleQueryStatisticsAcceptanceRegressionTest,SampleQueryContractTest test
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover \
  -s qqq-sample-project -p test_feature_coverage.py
# Expected nonzero: feature remains pending, even with --report-only.
PYTHONDONTWRITEBYTECODE=1 python3 qqq-sample-project/verify-feature-coverage.py \
  --stage source --report-only --require-feature core.observability.query_statistics
```

Local 2026-09-27 focused evidence: the seven original probes failed on assertions
with zero errors/skips (`/private/tmp/qqq-564-seven-red.log`), then all seven passed
alongside twelve sample methods and three query-contract controls
(`/private/tmp/qqq-564-seven-green.log`). Expanded native evidence passed twenty
sample methods plus the same three controls with zero errors/skips
(`/private/tmp/qqq-564-expanded-green.log`). A final clean focused run against
the fully verified/installed core also passed all 23 with zero errors/skips
(`/private/tmp/qqq-564-final-focused.log`); its installed core jar SHA-256 matches
the built jar. The 23 Python ledger tests passed
(`/private/tmp/qqq-564-ledger-fixed.log`). Focused core red/green logs are
`/private/tmp/qqq-{833,834,835,836,837}-core-{red,green}.log`; the additional delayed
consumer generation case passes in `/private/tmp/qqq-836-generation-green.log`.
The unique cache copied dependency downloads, removed copied QQQ
`4.1.0-SNAPSHOT` artifacts, then rebuilt the matching source reactor. Later
artifact-only core installation used `-DskipTests -Djacoco.skip=true` because
focused coverage data cannot meet the full-module threshold; that preparation is
not claimed as full verification. At `95ad06c28`, full core `clean install` passed with normal gates: 2,069 tests, zero
failures/errors, eleven existing skips, 527/542 classes covered (97.23%) and zero
Checkstyle violations (`/private/tmp/qqq-564-full-core-install.log`). PMD reports
non-blocking warnings under the unchanged repository configuration. The first
sandboxed full-core attempt had seven socket/Docker setup errors and was rerun
with access to the required fixtures; it is not counted as a passing run.
This earlier focused checkpoint preceded the full combined verification recorded below.

Review follow-up for #834: `CapturedContext` omits named objects. Flush now saves
that map separately, detaches it before calling the supplier, and restores the
exact original map after clearing its own objects. Core regressions cover null,
empty, populated and object-only caller contexts on empty/disabled exits and
supplier failure. Native regressions preserve the caller's mutable payload and
`AuditDetailAccumulator` through real successful and failed SQL inserts, prove
supplier-created objects are available only within storage, and check that
flush-created objects do not leak. The scheduled fixture checks named-object
cleanup between jobs. No public context API or storage-failure policy changed.

The new core cases first failed 3/3 and native cases failed 2/2 on assertions
without errors/skips (`/private/tmp/qqq-834-objects-core-red.log` and
`/private/tmp/qqq-834-objects-native-red.log`). Focused green runs passed 32 core
manager/context/audit/pipe tests and 25 sample/query-contract tests, all without
failures/errors/skips (`/private/tmp/qqq-834-objects-core-green.log` and
`/private/tmp/qqq-834-objects-native-green.log`). Artifact-only installation into
the isolated cache used `-DskipTests -Djacoco.skip=true`; built/installed jar
SHA-256 hashes match. Independent re-review and composition review passed. On the
combined source, full core/RDBMS installation passed 2,072/274 tests with 11/5
existing skips and no failures/errors; both installed JARs match the built artifacts.
All 25 focused query cases and the full sample acceptance/data-QBit profiles passed
at `fd7743b3773e02cb0ee59e570191ede2e48b40f2`: **817 regular +110 integration tests**,
zero failures/errors/skips, and all 41 sample classes covered. The 46 Python checks
also pass. Logs are `/private/tmp/qqq-798-wave8-{core-rdbms,query-focused,sample,python}.log`;
archived reports are `/private/tmp/qqq-798-wave8-final-reports`. The query-statistics
row at that checkpoint remained pending for concurrent query cancellation; the
sidecar and combined run below complete that source requirement. No requirement,
release disposition or threshold is relaxed.


#### Provider-contract follow-up for original #588

The provider-contract branch retains all 17 reviewed S3 methods and adds seven
focused cases. It introduces no production changes, dependencies, provider policy
or public API. These results extend the source evidence; the earlier full #838 run
does not certify this new delta; its completed combined verification is recorded below.

| Original requirement | Added evidence and remaining boundary |
| --- | --- |
| Path/file-name behavior | At exactly 1,024 UTF-8 bytes, independent native writes and QQQ read/replace/delete preserve exact keys and bytes. Controls cover ASCII and multibyte names under both ASCII and multibyte table prefixes; the limit calculation includes the entire backend/table prefix. |
| Invalid path/key | Native LocalStack 1.4 accepts 1,025-byte keys. The explicit limitation test records native and QQQ acceptance/readback/deletion, not successful AWS validation. Excess parent segments in the complete key fail in both paths with LocalStack HTTP 500, with the entire owned bucket unchanged; this is not an AWS error-code claim. |
| Valid opaque keys | The real SDK and QQQ PUT/GET/DELETE send `folder/./dot.txt`, `folder/../parent.txt`, and `./leading.txt` unchanged to an owned HTTP recorder. Native LocalStack writes instead alias normalized keys, overwrite their sentinel values, and delete those aliases. Exact wire behavior is proved, but native preservation of these valid AWS keys is not proved by this emulator. |
| Credential/permission failure | Production `preAction` constructs the SDK client from two distinct synthetic metadata key/secret pairs and a selected region. Local presigning plus independent JDK HMAC verification proves selection of both access key and secret. Signed URLs/signatures are never logged; no generated URL is requested. Existing owned 403 tests prove error propagation; neither layer certifies AWS IAM enforcement. |
| Connection timeout | A network-isolated Linux child JVM fills an owned loopback accept queue with real connected sockets. A native connect then times out, followed by a public QQQ `StorageAction` failure whose cause chain includes Apache `ConnectTimeoutException` caused by `SocketTimeoutException`. Draining that same listener restores a successful native connect. The parent verifies native S3 state unchanged and normal QQQ access still works. |

[AWS's key rules](https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-keys.html)
allow the tested period segments; they are not filesystem traversal to reject by
blanket policy. The initial native probes kept that expected contract and failed
before QQQ was involved: two assertion failures, zero errors/skips in
`/private/tmp/qqq-588-provider-key-first.log`. The SDK-only wire control then passed,
separating the provider/emulator path from QQQ. The final limitation tests name the
observed emulator behavior explicitly; they do not remove the original acceptance
gap or turn the red AWS expectations into conformance claims.

The Linux helper uses the existing `maven:3.9-eclipse-temurin-21` image, Testcontainers
copy-to-container runtime artifacts, and `network=none`; there are no host bind
mounts, exposed ports, host firewall/route changes, or external AWS calls. Temporary
artifact directories, sockets, SDK clients and the child container are owned and
closed. Its 500 ms fixture connect deadline and disabled retries do not change
production defaults. Native queue saturation was first probed separately: macOS
returned a reset rather than a timeout, while three Linux trials timed out at
501–510 ms. The required acceptance case therefore always uses the Linux fixture;
there is no platform skip or fallback that accepts reset/read-timeout errors.

S3 remains **pending** for full verification and independent review of the original
requirement mapping below. Native AWS overlong-key rejection and literal period-key
preservation remain unproved provider-conformance limits; they are not automatically
new live-AWS acceptance gates. The original invalid-path scenario has a real native
failure/no-mutation case plus missing-object-reference and glob checks. The reviewer must
assess those checks against the original path/file-name requirement before changing
status. Metadata signing and real connection timeout now have runnable evidence;
source-copy partial publication remains the documented #459 boundary.


Focused provider validation passes **24/24 cases, zero failures/errors/skips**, with
zero Checkstyle violations (`/private/tmp/qqq-588-provider-matching-native.log`). All
46 top-level Python checks pass (`/private/tmp/qqq-588-provider-bindings-python46.log`).
The isolated cache is `/private/tmp/qqq-588-s3-provider-m2`, copied independently from
the matching #838 cache; production sources remain identical to reviewed `71cc83528`.
The native Linux backlog research log is
`/private/tmp/qqq-588-native-backlog-linux-probe.log`; the earlier macOS reset is kept
in `/private/tmp/qqq-588-native-backlog-probe.log`. No full-sample or Javalin-server
run was started for this delta, and no shared broker reservation was used.


#### Original #588 requirement-to-evidence map

All method names below belong to `SampleS3AcceptanceIT`; the timeout method also
launches `S3ConnectTimeoutFixture`. Each original scenario is mapped explicitly.
Unsupported operations remain tested refusals, not invented implementations.

| Original scenario | Actual methods | Independent oracle / result |
| --- | --- | --- |
| Query | `oneRecordRoundTripAndUnsupportedUpdate`, `oneJsonNamesHeavySelectionAndReplacement`, `manyCsvJsonAndPostReadHaveNativeOracles` | Native objects seeded/read by a separate SDK client; public query fields, heavy bytes, filters and cardinalities match known bytes/rows. |
| Count | `oneRecordRoundTripAndUnsupportedUpdate`, `oneJsonNamesHeavySelectionAndReplacement`, `manyCsvJsonAndPostReadHaveNativeOracles`, `missingBucketAndInvalidGlobDoNotMutate` | Known native ONE/MANY objects yield counts 1/2; an empty native prefix yields 0. |
| Insert | `oneRecordRoundTripAndUnsupportedUpdate`, `oneJsonNamesHeavySelectionAndReplacement`, `manyCsvJsonAndPostReadHaveNativeOracles` | ONE writes exact CSV/JSON bytes under expected keys; MANY insert is explicitly unsupported and preserves native snapshots. |
| Update | `oneRecordRoundTripAndUnsupportedUpdate` | Actual `UpdateAction` throws the adapter's `NotImplementedException`; all native keys/bytes remain unchanged. No supported update is claimed. |
| Delete | `oneRecordRoundTripAndUnsupportedUpdate`, `manyCsvJsonAndPostReadHaveNativeOracles`, `exactUtf8KeyLengthBoundaryPreservesNativeBytes` | ONE deletion count/readback proves native absence and preserves controls; MANY delete refuses without mutation. |
| Raw storage contracts | `rawStorageRoundTripAndMissingRead`, `storageUrlAndAclHaveNativeMetadataOracles`, `multipartPublishesOnCloseAndReleasesUpload`, `rawStorageOffsetWritesExactSlice` | Independent SDK verifies exact binary bytes, content type, URL target, emulator ACL, multipart parts/invisibility before close, publication afterward and absence of residual upload; offset output is exactly `23456`. |
| CSV/JSON | `oneRecordRoundTripAndUnsupportedUpdate`, `oneJsonNamesHeavySelectionAndReplacement`, `manyCsvJsonAndPostReadHaveNativeOracles`, `manyScalarValuesNullsAndFailedCustomizer` | Known native CSV/JSON bytes exercise opaque ONE contents and parsed MANY rows, Unicode, quoted commas/newlines, long values and JSON null. |
| ONE/MANY cardinality | Same four methods as CSV/JSON | ONE maps each object to one record; MANY parses file rows and counts them. MANY insert/delete and adapter update retain explicit unsupported boundaries. |
| Path/file-name behavior | `oneJsonNamesHeavySelectionAndReplacement`, `exactUtf8KeyLengthBoundaryPreservesNativeBytes`, `sdkAndQqqSendOpaquePeriodSegmentsWithoutNormalization`, `localstackNormalizesValidPeriodSegments` | Native nested/space/Unicode names and complete 1,024-byte keys round-trip; raw wire paths preserve valid period segments. Native normalization is separately observed with distinct normalized-key sentinels, not described as AWS preservation. |
| Post-read handling | `manyCsvJsonAndPostReadHaveNativeOracles`, `manyScalarValuesNullsAndFailedCustomizer`, `oneJsonNamesHeavySelectionAndReplacement` | Configured MANY transformation changes parsed values while native bytes stay unchanged; failed MANY customizer surfaces on query/count; ONE bypasses it as its current contract. |
| Missing file | `rawStorageRoundTripAndMissingRead`, `missingBucketAndInvalidGlobDoNotMutate` | Native `NoSuchKey`/`NoSuchBucket` errors propagate; empty prefix has zero rows/count; native snapshots are unchanged. |
| Malformed CSV/JSON | `malformedCsvAndJsonRefuseWithoutMutation` | SDK seeds invalid CSV and JSON; actual QQQ parsing fails and independent native snapshots remain unchanged. |
| Permission/credential failure | `permissionAndCredentialResponsesPropagateWithoutNativeWrites`, `metadataCredentialsReachSdkSigner` | Real SDK receives owned HTTP `AccessDenied`, `InvalidAccessKeyId`, `SignatureDoesNotMatch` responses on query/write with no native mutation. Independent HMAC proves production metadata key/secret selection. This is QQQ propagation/signing evidence, not AWS IAM enforcement. |
| Connection timeout | `connectionEstablishmentTimeoutHasNativeCauseAndRecovery`; separate `readTimeoutPropagatesWithoutNativeMutation` | Actual native Linux connect and QQQ SDK cause chain distinguish establishment timeout from socket-read timeout/refusal; same listener recovers, native S3 snapshot is unchanged and normal QQQ access works. |
| Invalid path | `excessParentSegmentsDoNotMutateNativeObjects`, `missingBucketAndInvalidGlobDoNotMutate`, `rawStorageRoundTripAndMissingRead`; boundary control `localstackDoesNotEnforceAwsKeyByteLimit` | Invalid full native key errors through both SDK/QQQ and leaves the entire owned bucket unchanged; invalid glob/missing-object-reference errors also have no mutation. Emulator HTTP500 and accepted overlong keys remain explicit provider limits, not AWS validation claims. |
| Partial write | `failedCopyPublishesPrefixAndPreservesOtherKeys`, `failedMultipartWriteAbortsUploadAndPreservesExistingObject`, `failedMultipartFinalPartAbortsUploadWithoutPublishing`, `failedMultipartCompletionAbortsUploadAndPreservesExistingObject`, `failedMultipartAbortPreservesOriginalAndResidualParts` | Native bytes expose #459's source-copy partial publication. Real multipart IDs/parts precede SDK faults; native upload/object readback proves successful abort or honest residual state after abort failure, terminal stream behavior and independent teardown. |

No original scenario label is omitted from the runnable mapping. The concrete
unproved native contracts are AWS's 1,025-byte rejection and literal storage of valid
period-segment keys; their relevance to the original path/file-name requirement is
left visible as provider limits. Independent review accepted all 16 original scenario
mappings without adding live-AWS, general IAM, blanket strict-key or mandatory
atomic-rollback requirements. Full combined verification of this delta also passed.


All 24 ledger bindings use exact fully qualified JUnit report IDs (including the
injected `Path` parameter in the timeout case). The original 17 method bodies are
unchanged. Independent input and composition reviews passed. At
`9b687321ed5a515414b9cb4c42f69b1d9e2991f3`, full acceptance and data-QBit profiles passed
**817 regular +117 integration tests (934 total)** with zero failures/errors/skips
and all 41 sample classes covered. All 24 native S3 methods passed against the
current runtime, as did the 46 Python checks. Logs are
`/private/tmp/qqq-798-wave9-{s3-focused,sample,python}.log`; reports are archived in
`/private/tmp/qqq-798-wave9-final-reports`. S3 is verified at source stage; this does
not certify unrelated rows, public release artifacts or AWS-specific enforcement.


### Query statistics: concurrent cancellation sidecar (#564)

This test-only increment starts from reviewed `d78bce1c1` in a separate worktree.
`SampleQueryStatisticsAcceptanceCancellationTest` uses the existing owned H2
fixture, `QueryAction.cancel()`, a real `NonPersistedAsyncJobCallback`, and
`RecordPipe.terminate()`. No production behavior, dependency or schema contract
changes. Test-only H2 alias/view objects pause actual statement execution through
latches; a normal table customizer separately pauses real row delivery. Neither
substitutes a backend, connection or persistence operation.

| Passing method | Exact outcome |
| --- | --- |
| `testNativeStatementCancellationDoesNotPublishCompletedStatistic` | Cancellation after entry into real JDBC execution raises `Query was cancelled.`; neither the consumer nor native `query_stat` contains a completed measurement. |
| `testAsyncCancellationDuringDeliveryRetainsExactPartialMeasurement` | A concurrent callback cancellation request stops the backend after its current row. Exactly the first native row reaches the plain pipe; the normal backend return retains count 1 and persists its SQL/session metadata. |
| `testConcurrentPlainPipeTerminationRetainsZeroDeliveryMeasurement` | Termination while SQL is active, before delivery starts, discards all delivery. SQL still completes and retains count 0. An independent native count proves the gated view has 20,480 rows; terminating a pipe is not statement cancellation. |

Each case verifies native connection cleanup (only the owned oracle remains),
unchanged native person count, session ownership, cleared context on reuse of the
query worker, bounded worker shutdown, and a fresh five-row recovery read and
statistic. The inherited fixture stops and joins the statistics scheduler.
Latches establish ordering rather than elapsed sleeps. Backend counts remain
consumer-only; no request-success/outcome column is invented. The existing
post-query rejection regression continues to retain completed backend SQL stats.

The row is **verified at source stage** after the combined run recorded below.
This evidence covers the three named controlled H2 paths, not arbitrary thread interruption or termination of an already blocked,
full or association-buffered pipe. It does not claim exhaustive race detection or
other JDBC drivers. Independent requirement and composition reviews passed; the
combined full run is recorded below. The sidecar itself starts no HTTP fixture or
broker. No runtime defect was demonstrated in these three paths.

Reproduce from this worktree using Java 21 and matching-source artifacts:

```sh
QUERY_STATS_CANCEL_M2=/private/tmp/qqq-564-cancellation-d78-m2
# Prepare a fresh dedicated cache from this source if not already prepared.
mvn -nsu -Dmaven.repo.local="$QUERY_STATS_CANCEL_M2" -DskipTests install
mvn -nsu -Dmaven.repo.local="$QUERY_STATS_CANCEL_M2" -f qqq-sample-project/pom.xml \
  -Dtest=SampleQueryStatisticsAcceptanceTest,SampleQueryStatisticsAcceptanceRegressionTest,SampleQueryStatisticsAcceptanceCancellationTest,SampleQueryContractTest test
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover \
  -s qqq-sample-project -p test_feature_coverage.py
# Expected nonzero: required pending feature cannot be certified by report-only.
PYTHONDONTWRITEBYTECODE=1 python3 qqq-sample-project/verify-feature-coverage.py \
  --stage source --report-only --require-feature core.observability.query_statistics
```

Local 2026-09-27 evidence: the baseline passed 25 tests. The sidecar cache was an
isolated copy of the existing matching d78 source cache; this is not a clean-cache
bootstrap or published-artifact proof. Initial fixture setup had two null backend
metadata errors, corrected before outcome validation. All three new cases then
passed. Temporarily removing the three cancellation/termination calls made all
three assertions fail, with zero errors/skips: cancelled SQL returned normally,
terminated delivery became 20,480 rows, and callback cancellation delivered all
five rows (`/private/tmp/qqq-564-cancellation-negative-controls.log`). Restoring
those calls and running the final focused set passed **28 tests, zero
failures/errors/skips**, with zero Checkstyle violations in 11.045 seconds
(`/private/tmp/qqq-564-cancellation-focused.log`). No production edits were needed.
At that focused checkpoint, the 23 Python ledger tests passed; the required-feature
command exited 1 because scenario review was still pending. All 130 IDs and all
requirement, negative-case, stage and deferral fields remain unchanged. The new
Java file has an Apache-2.0 header.


### Combined query verification and timestamp precision (#564, #846)

The matching combined run at `64fd0466b4911cff66e20b34fb4b5e7425662f90` passed **821 regular +117 integration tests (938 total)** with zero failures/errors/skips and all41 sample classes covered. All29 focused query cases and46 Python checks passed. Original-requirement review confirms the direct/plain/association-buffered reads, persistence, lifecycle, failures and controlled cancellation outcomes cover the source contract; it does not require an exhaustive cross-product of every interruption mode. Query statistics is now source-verified. Consumer-only counts, dropped failed storage batches, backend-measurement semantics and #525/#526/#527–531 dispositions remain unchanged.

Linux CI exposed nanosecond Instant values being compared with rounded H2 TIMESTAMP values. The #846 fixture-only correction uses native JDBC casting as the precision oracle; fixed nanosecond inputs independently prove rounding down, up and across a second boundary through real manager persistence. Its deterministic assertion failed before the correction and passed afterward. No production schema, precision, tolerance, retry or timeout changed. Evidence: `/private/tmp/qqq-846-timestamp-{red,green}.log`, `/private/tmp/qqq-798-wave11-{query-focused,sample,python}.log` and archived `/private/tmp/qqq-798-wave11-final-reports`. Public-candidate and other release gates remain separate.

### Merge duplicate Person acceptance (#568 / #843)

`SampleMergeAcceptanceTest` registers the actual `MergeDuplicatesProcess` against
sample Person/Pet/Pet Note metadata in a uniquely named, owned H2 database. Its
application transform groups people by email, accepts an explicit survivor ID,
combines worked days and a chosen first name, and queues existing pets for
reassignment. Duplicate discovery, preview/resume, writes, deletes and transaction
ownership remain framework operations. Independent JDBC connections inspect
committed rows; failure cases compare every column of all three native tables.
This is a Java process acceptance fixture, not browser interaction or a new
framework rule for choosing survivors.

Original [#568](https://github.com/QRun-IO/qqq/issues/568) requirements map to these
methods in `com.kingsrook.sampleapp.SampleMergeAcceptanceTest`:

| Original scenario | Runnable methods and native oracle |
| --- | --- |
| Select duplicates and choose survivor | `selectedDuplicateMergesFieldsAndPreservesAssociations` selects only Person 1 and discovers Person 2 by email; `choosingSecondSurvivorPreservesAllPetsAndGrandchildren` keeps Person 2 instead. JDBC observes exactly the selected survivor and combined fields. |
| Merge fields/associations and verify persistence | Both survivor tests preserve all six pets and both nested notes; unrelated people are unchanged. `selectedSurvivorAndFieldsPersistWithoutAssociationMoves` verifies a field-only merge and repeat no-op. `previewDoesNotWriteAndConfirmationPersistsMerge` snapshots all tables before confirmation and checks persistence after resuming the same process. |
| Unrelated records | `unrelatedSelectionAndInvalidSurvivorDoNotMutate` proves distinct email groups and a survivor outside the duplicate group leave all native rows unchanged. |
| Denied record | `readDeniedDuplicateIsNotMerged` excludes a denied duplicate even when explicitly selected. `writeDeniedSurvivorDoesNotConsumeDuplicate`, `deniedAssociationReassignmentRollsBackMerge` and `deniedDuplicateDeletionRollsBackMerge` assert an error and unchanged native tables; reassignment includes both allowed and denied child updates and exercises process/page transactions. |
| Unique conflict | `uniqueConflictDoesNotConsumeDuplicate` installs a fixture-only native unique constraint and corresponding QQQ key; a conflicting survivor field fails without consuming the duplicate or its children. |
| Failed merge rollback | `thrownLoadFailureRollsBackAlreadyUpdatedSurvivor` observes the survivor and reparented pet inside the live native transaction, then throws from the existing pre-delete customizer; independent JDBC proves rollback. The denied-write cases also test error-bearing output records, not only thrown exceptions. |

Replacement regressions exercise the supported application transform's alternative
policy of deleting existing pets and inserting a replacement attached to the survivor.
`processReplacementByFilter` / `pageReplacementByFilter` and their `ById` counterparts
prove the replacement survives while old children and their notes are removed.
`processUniqueReplacementByFilter` / `pageUniqueReplacementByFilter` and their `ById`
counterparts reuse an old pet's unique name under both a native constraint and QQQ
metadata. All eight compare unrelated native rows. `replacementExceptionRollsBackDeletes`
and `replacementRecordErrorRollsBackDeletes` observe old children/notes already
removed inside the transaction before rejecting the insert; both process/page modes
restore every original row. These preserve delete-before-replacement semantics while
deferring only duplicate-parent ID deletion until reassignment succeeds.

Additional boundary tests distinguish the existing transaction modes:
`autocommitFailureStopsDeletionWithoutClaimingRollback` proves that an earlier
survivor update remains committed under autocommit, while the duplicate, children
and notes survive the failed reassignment. No cross-backend, distributed or
whole-job-across-pages atomicity is claimed. `cleanupFailuresDoNotMaskThrownMergeFailure`
and `cleanupFailuresDoNotMaskErrorBearingReassignment` use the existing load
extension point with a native H2 transaction that performs real rollback/close
before raising controlled cleanup errors. Both process/page paths retain the exact
primary exception and suppressed rollback/close diagnostics.
`repeatedPrimaryInstanceIsNotSuppressedOntoItself` guards self-suppression;
`successfulMergeSurfacesCloseFailure` and `successfulPageMergeSurfacesCloseFailure`
prove close failures remain visible after successful commits, with committed native
rows rather than a false rollback claim.

The original compiling red cases exposed persisted association loss and continued
deletions after rejected writes. [#843](https://github.com/QRun-IO/qqq/issues/843)
tracks the correction: the merge load step keeps application-selected child ID/filter
deletes before replacement inserts, then stores/reassigns related records before
deleting duplicate parents. It rejects record-level action errors and relies on the existing
ETL transaction to roll back. The ETL owner now retains primary failures when cleanup
also fails. No dependencies, public APIs, shared sample schema or provider policies
are added. Focused validation is **27 native cases, zero failures/errors/skips**;
the full core `clean install` also passes **2,063 tests, zero failures/errors and
11 existing skips**, with configured quality checks unchanged. Independent input
and composition review passed; the ledger row is now **verified at source stage**
after the combined full sample run recorded below.

Run the focused fixture after installing matching source artifacts into an isolated
Maven repository:

```sh
mvn -B -o -nsu -Dmaven.repo.local=/path/to/owned-cache \
  -f qqq-sample-project/pom.xml -Dtest=SampleMergeAcceptanceTest test
```

The fixture starts no HTTP server, broker or container. It removes its own process
state, shuts down its unique H2 database, resets connection providers and restores
caller context. The full `-Pacceptance-tests,data-qbit-acceptance clean verify` gate
remains separately scheduled with the shared sample broker owner.


### Automation source acceptance (#567)

`SampleAutomationAcceptanceTest` runs nine source-only cases on the real sample
Person/Pet metadata, with test-only evidence columns in a UUID-named H2 database.
Public Insert/Update and registered manual automation/recovery processes drive the
scenario; independent native SQL checks pending/running/final statuses, persisted
handler effects, filtered associations, reverse-declared priorities and child
process batches of 2/2/1. A failed filtered action marks its whole batch failed;
later actions still execute, partial writes remain, and explicit recovery can
repeat those writes. Completed rows are not processed again by another poll.
Recovery preview is read-only; failed/stale running states are requeued while
recent running, OK, unknown and null states remain untouched. Fixtures close their
database, remove owned parent/child process states and restore QContext. They do
not start a scheduler, Javalin or Artemis; Person ESB publication is disabled only
on the private fixture instance.

Invalid provider linkage/status-field metadata and missing/unknown table or
ambiguous implicit provider inputs are rejected without data changes.
The correction for [#845](https://github.com/QRun-IO/qqq/issues/845) fixes the false-success
response reproduced at source base `7bc761154`: an explicit unknown provider now
throws the existing `QException` before work or status changes. The native sample
regression compares all Person rows before/after rejection, then verifies that a
valid-provider retry changes that row to OK with one persisted handler invocation.
Null/blank provider selection still chooses the sole configured provider and rejects zero
or multiple providers; a known provider with no applicable work remains a no-op.
No new API, provider or runtime policy is introduced.
PRE_DELETE remains documented unsupported/excluded; scheduler timing, competing
workers, atomic rollback and exactly-once delivery are not claimed. The ledger
retains all 130 requirements and the existing 4.0 deferral. Independent requirement
and composition review passed, and this row is now **verified at source stage**
after the combined full run below. Publication gates remain separate.

Focused reproduction with matching source artifacts in an isolated Maven cache:

```sh
mvn -o -f qqq-sample-project/pom.xml -Dmaven.repo.local="$AUTOMATION_ACCEPTANCE_M2" \
  -Dtest=SampleAutomationAcceptanceTest test
python3 -B -m unittest discover -s qqq-sample-project -p 'test_*.py'
```

The #845 rejection regressions fail against the original implementation in both
core and sample (`/private/tmp/qqq-845-{core,sample}-red.log`), then pass after the
provider check: core 7/7 and sample 9/9, with zero Checkstyle violations. Root core
`clean install` through the unchanged verify gates passes 2,067 total tests (2,056
executed and 11 existing skips), zero failures/errors, 97.23% class coverage and
82.32% instruction coverage (`/private/tmp/qqq-845-core-full-install.log`). SpotBugs
and PMD run under the existing report-only defaults and retain advisory findings;
this is not a zero-warning claim. The sample uses the identical built/installed
core JAR in the owned cache (`/private/tmp/qqq-845-sample-green.log`); 46 Python
checks also pass (`/private/tmp/qqq-845-python.log`).

The original fixture's deliberately reversed priority and changed batch size
produced two native-oracle failures before restoration
(`/private/tmp/qqq-567-priority-batch-mutation.log`). No full sample run was started
on the original isolated branch. Its reviewed changes are now included in the
combined verification below, including the packaged-child port helper.

The reviewed integration at `2770fbf5d` passed **858 regular +117 integration tests
(975 total)** with zero failures/errors/skips and all 41 sample classes covered.
Matching-source core/RDBMS gates passed 2,076/274 tests with 11/5 existing skips;
46 focused SQLite/merge/automation tests and 46 Python checks passed. Merge and
automation are source-verified. The test-only #848 correction asserts logical
connection closure and native reuse of a one-slot SQLite pool; a latch-controlled
case proves asynchronous check-in without changing production pool settings.
Required CI, public-artifact consumption, remaining ESB requirements and separately
owned Next acceptance still govern release; this checkpoint is not release certification.


## JavaScript executor: bounded sample evidence (#591)

`SampleJavaScriptAcceptanceTest` registers an owned Java pre-update customizer on
canonical sample `person` metadata. That customizer passes the original inline
`QCodeReference` to `ExecuteCodeAction`, which loads the existing Nashorn
`QCodeExecutor`. Four owned scripts live under `SampleJavaScriptAcceptance/` in
test resources. This does not add a script-capable metadata loader: the current
`QCodeLoader` accepts Java customizers, and the sample Java adapter invokes the
existing script API explicitly.

Six methods cover typed Java bean/decimal/integer context, deployment context,
the real execution logger, returned record identity and a persisted last-name
mutation; syntax and runtime diagnostics; absent reference and absent inline
source; and invalid application result type. Every negative snapshots all native
person columns before and after the attempted update. Runtime and invalid-result
scripts mutate their in-memory record first, then fail before persistence.
Successful DML changes only the intended name and the normal modification time,
whose native value must lie within the write interval. Other rows/columns remain
unchanged. Each case checks native connection cleanup and caller session/context;
fixture cleanup restores prior ordinary and named context and closes its H2 DB.

Nashorn may validly return a string. The **sample customizer's** QRecord result
contract rejects that string before DML; the execution log correctly records
successful script execution. Missing code reference currently produces a wrapped
null-reference exception; missing inline source reports the existing inline-only
limitation. These tests do not promise script rollback, sandboxing of trusted
scripts, new engine behavior, or isolation from arbitrary script side effects.
The logger is the existing in-memory `BuildScriptLogAndScriptLogLineExecutionLogger`;
no persisted script-log behavior is claimed.

The sample now declares the existing `qqq-language-support-javascript` module at
`${revision}` in **test scope**, with no separate engine version override. The
customizer and JavaScript resources in this increment exist only under
`src/test`; this dependency does not supply production JavaScript execution.
No script-execution references were found in the sample's production metadata or
code. Independent review and combined verification remain required;
`javascript.executor` stays pending.

Local diagnostic evidence, Java 21, 2026-09-27:

- Isolated worktree from `c65c70dfc`, cache
  `/private/tmp/qqq-591-javascript-c65-m2`, cloned from main's stable source cache.
  Core, RDBMS and JavaScript module jar SHA-256 hashes match main's cache. This is
  not empty-cache or public-artifact verification.
- Baseline query controls: 3 passed (`/private/tmp/qqq-591-baseline.log`).
- Ordinary six-case run: 1 passed, 4 failures and 1 error, no skips; five cases
  blocked by missing executor class (`/private/tmp/qqq-591-classpath-red.log`).
- The existing JavaScript module's runtime classpath was resolved offline with
  `dependency:build-classpath`. For diagnosis only, its jar, Nashorn 15.7 and its
  declared ASM jars were supplied using Surefire's
  `-Dmaven.test.additionalClasspath`. No dependencies were installed or changed.
  An initial native oracle used the wrong physical-column index and overlooked
  the normal modify timestamp; the final oracle uses column names and explicitly
  bounds that timestamp.
- The six script cases plus three query controls passed, zero failures/errors/
  skips, zero Checkstyle violations (`/private/tmp/qqq-591-explicit-runtime-green.log`).
  This diagnostic classpath is not a substitute for ordinary CI integration.
- No production code, full sample, HTTP/broker fixture or shared worktree changed.

After the test-scoped module reference was authorized, ordinary Maven execution
passed all six JavaScript cases and three query controls: **9 tests, zero
failures/errors/skips**, zero Checkstyle violations, in 12.751 seconds
(`/private/tmp/qqq-591-ordinary-focused.log`). No additional-classpath property or
diagnostic jar override was used. The original classpath-red and diagnostic logs
remain intact. Reproduce with matching-source artifacts and Java 21:

```sh
mvn -nsu -Dmaven.repo.local=/private/tmp/qqq-591-javascript-c65-m2 \
  -f qqq-sample-project/pom.xml \
  -Dtest=SampleJavaScriptAcceptanceTest,SampleQueryContractTest clean test
PYTHONDONTWRITEBYTECODE=1 python3 -m unittest discover \
  -s qqq-sample-project -p test_feature_coverage.py
# Expected nonzero until independent review and combined verification:
PYTHONDONTWRITEBYTECODE=1 python3 qqq-sample-project/verify-feature-coverage.py \
  --stage source --report-only --require-feature javascript.executor
```
The 23 Python ledger tests also passed. The required-feature command exits 1
with this row blocked only by pending scenario review. All 130 IDs, acceptance
requirements, negative cases, statuses, stages and deferrals remain unchanged.

### Scheduling acceptance (#569, #847, #853, #855)

`SampleSchedulingAcceptanceTest` owns a unique H2 database, RAM Quartz scheduler,
Simple scheduler and registered application processes. Actual QQQ actions update
sample Person records; independent JDBC connections verify committed results and
unrelated rows. The real application launcher uses an owned ephemeral HTTP port; no
shared sample broker is started. A separate owned H2 database exercises Quartz's
actual JDBC job store. Before enrichment, every fixture instance removes only ESB
instance, table and process supplemental metadata, preventing unrelated broker
services, publications and triggers from registering. The real launcher asserts
that only HTTP and scheduling start, with no runtime services or record/process
listeners registered. The legacy queue
runner uses its real SDK against a disposable loopback, in-memory protocol fixture
with synthetic credentials, not an SQS account or a claim about AWS delivery/IAM.

Original [#569](https://github.com/QRun-IO/qqq/issues/569) coverage maps as follows
(methods are in `com.kingsrook.sampleapp.SampleSchedulingAcceptanceTest`):

| Original requirement | Method and independent observation |
| --- | --- |
| Interval / initial delay / process runner | `simpleIntervalAndInitialDelayPersistProcessWrites` observes delayed, repeated writes and stopped executor state. `quartzHonorsInitialDelay` reads the native trigger date; `quartzDelayPrecedesActualProcessWrite` observes delayed dispatch and persisted data. Core `QuartzSchedulerTest#nativeTriggerHonorsComputedInitialDelay` also checks milliseconds, seconds, explicit zero, the existing default and precedence. |
| Cron | `cronScheduleDispatchesAndUnscheduleAllRemovesIt` checks native expression/time zone, actual process persistence and removal of all jobs. |
| Schedule / reschedule / duplicate identity / pause / resume / unschedule | `quartzIntervalPauseResumeRescheduleAndUnschedule` observes one native job after replacement, updated interval, retained paused state, resumed execution and removed identity. |
| Automation runner | `automationRunnerPersistsHandlerResultAndStatus` runs the actual polling automation runner and handler; native Person data and status transition from pending to OK, with unrelated rows unchanged. |
| Queue runner | `queueRunnerPersistsBodyBeforeAcknowledging` checks the received body drives persisted data and the actual SDK sends the expected receipt acknowledgement. `queueRunnerDoesNotAcknowledgeFailedProcess` checks no acknowledgement and unchanged native rows after a real application exception. |
| Variant serial / parallel | `serialVariantsPersistDistinctRowsWithoutOverlap` and `parallelVariantsOverlapWithIsolatedSessionsAndRows` use latches to prove one-at-a-time versus concurrent execution, native job counts, per-variant sessions and distinct persisted rows. |
| Invalid cron | `invalidCronPreservesNativeScheduleAndData` proves invalid new/replacement definitions do not replace native jobs or mutate data. Existing setup logs rejection; this is not a promise of caller-visible exceptions. Simple metadata correctly declares cron unsupported. |
| Job exception | `failedProcessDoesNotPreventNextInterval` verifies the runner's actual logged diagnostic and later successful persistence. The existing process runner catches errors; native Quartz exception propagation is not claimed (see existing #502). |
| Timeout / shutdown cleanup | `SampleSchedulingTimeoutAcceptanceIT#actualSimpleStopTimeoutRetainsWorkThenCleansUp` holds a real registered Simple job on an owned latch and invokes the actual `StandardScheduledExecutor.stop()`. After its existing 300-second wait, `stop()` returns `false` with `STOPPING` while the worker remains alive and native data unchanged. Release permits one QQQ write, exact prior worker context restoration and actual worker termination. The API retains `STOPPING` afterward; no forced cancellation or application deadline is claimed. `observerTimeoutDoesNotCancelAndShutdownWaitsForOwnedWork` separately covers orderly Quartz shutdown waiting, not this timeout path. |
| Context isolation | `quartzContextSuccessIsolatedAndRestored`, `quartzContextFailureRestoresAndNextRunRecovers`, and the corresponding `simpleContext...` methods prove a job cannot inherit or mutate the prior named-object map. They verify exact map/instance/session identity, user/thread restoration, no mutation on failed work, and native persistence on the next run. Quartz uses native listener hooks; Simple wraps its unchanged registered runnable in a real `StandardScheduledExecutor` to seed/observe worker context. |

Additional controls preserve existing boundaries. `disabledManagerDoesNotRegisterOrDispatch`
checks the global disable switch without changing its previous setting.
`launcherStartupDispatchesFreshQuartzJob` exercises the documented application
launcher without a second registration call, with native H2 writes and Quartz/HTTP
shutdown assertions. `startupDispatchesMixedSimpleAndQuartzJobs` proves both
scheduler types dispatch from one manager start. `startupRegistersPersistedDynamicJob`
loads job/parameter rows inserted through native SQL and verifies dispatch without
changing those rows. `startupPreservesExistingPausedRamJob` retains the original
trigger, paused state and unrelated native job. `persistentQuartzStartupRetainsGuard`
uses a real H2-backed Quartz JDBC store: existing paused state remains unchanged and
missing jobs stay absent, preserving the historical persistent-store startup guard.
This is a local JDBC control, not multi-node cluster certification.

[#855](https://github.com/QRun-IO/qqq/issues/855) bootstraps only missing jobs in
nonpersistent stores. Startup never reconciles or replaces existing jobs; Quartz's
non-replacing insert also protects a job registered concurrently. Explicit management
after startup retains its existing pause/reschedule/unschedule behavior.

[#847](https://github.com/QRun-IO/qqq/issues/847) applies the already-computed delay
through Quartz's existing trigger API. [#853](https://github.com/QRun-IO/qqq/issues/853)
confines named-object ownership to the existing Quartz/Simple wrappers: save the
nullable worker map, start the job with an empty map, and restore the exact prior
map in finally. Global `CapturedContext` and public APIs remain unchanged.

The scheduling row remains **pending**: independent review and full combined sample
verification are outstanding. The actual Simple stop-timeout IT complements the
short observer-wait case; neither promises cancellation or an application execution
deadline. The queue fixture
does not certify external SQS behavior or SDK-client shutdown ownership. Requirements,
release dispositions and existing deferrals are unchanged.

Run focused acceptance after installing matching source artifacts into an isolated
Maven cache:

```sh
mvn -B -o -nsu -Dmaven.repo.local=/path/to/owned-cache \
  -f qqq-sample-project/pom.xml -Dtest=SampleSchedulingAcceptanceTest test
```

Fixture teardown releases owned blocked work, stops schedulers and its protocol
server, removes only its process-state UUIDs, shuts down owned H2 and restores the
caller's context. Full sample profiles remain separately coordinated with their
shared broker owner.

Focused validation passes **19 native cases, zero failures/errors/skips**. Matching
core `clean install` passes **2,073 tests, zero failures/errors and 11 existing skips**,
with normal quality gates, and all **46 Python checks** pass. No combined full sample
run is claimed for this branch.

The #855 source follow-up passed 23 focused scheduling cases with zero
failures/errors/skips, plus the full core `clean install`: 2,074 tests, zero
failures/errors, 11 existing skips, normal Checkstyle/JaCoCo/analysis gates.
This is focused source evidence; it does not substitute for the combined full
sample gate. The separate acceptance-only stop-timeout case exercises the real
five-minute boundary and is excluded from ordinary Surefire tests. Run it with:

```sh
mvn -B -o -nsu -Dmaven.repo.local=/path/to/owned-cache \
  -f qqq-sample-project/pom.xml -Pacceptance-tests \
  -Dit.test=SampleSchedulingTimeoutAcceptanceIT \
  initialize test-compile failsafe:integration-test failsafe:verify
```

Its 330-second observer bound is only a diagnostic guard: reaching that bound fails
the test. Passing requires the Boolean returned by the real stop call after at least
300 seconds, followed by retained-work and native cleanup assertions. Normal full
`-Pacceptance-tests verify` discovers the IT automatically.

The test-only #569 follow-up passed the real timeout IT (1 case, 301.7 seconds),
then all 23 ordinary scheduling cases and 46 Python checks, with zero
failures/errors/skips in the scheduling cases. The isolated IT produced no ESB
connection attempt. Production is unchanged from the full-core-gated #855 input;
combined full sample verification and independent review remain pending.

The ESB-isolation correction removes instance, table and process ESB metadata
before enrichment, including the worker-context fixture instances. The corrected
fixture passed all 23 ordinary cases and the actual stop-timeout IT (1 case,
301.666 seconds), with zero failures/errors/skips, plus all 46 Python checks.
The real-launcher assertions confirm exactly HTTP and scheduling services, with
no runtime services or record/process listeners registered. Neither Maven XML
report contains an ESB runtime, trigger or connection-manager entry. This is
fixture-only evidence; the scheduling row remains pending review and combined
full sample verification.

## Associated and ad-hoc script acceptance (#572)

`SampleAssociatedScriptAcceptanceTest` runs the existing Store/Test/Run actions
inside `SampleMetaDataProvider.defineTestInstance()`, with an owned UUID-named H2
schema and real JavaScript executor. Its SQL resource creates the existing script
entities locally; it does not add application metadata, production tables or APIs.
Independent JDBC queries check persisted Person values, revision/file history,
current-revision pointers and success/error logs. No server or broker starts.

| Test method | Verified boundary |
| --- | --- |
| `testStoreRevisionsAndRunCurrentAssociatedScript` | Real Store action creates and advances revisions, retains exact historical code/author/commit metadata, and a fresh Run action resolves the current code with supplied inputs. |
| `testAssociatedDraftReturnsOutputWithoutPersistingRevisionOrLogs` | Registered Test process returns draft output and in-memory log lines; saved code, revisions and persisted logs remain unchanged. |
| `testRecordDraftLocalMutationDoesNotPersist` | Native record tester executes and logs a local record mutation without implicitly saving it; this does **not** prove isolation of explicit write APIs. |
| `testAdHocRecordInputsOutputsAndExplicitPersistence` | Primary-key record inputs and caller values produce exact output; local mutation is transient, while an explicit `qqq.update` in Run persists only the selected record. |
| `testRunRecordProcessPersistsSelectedRecordsAndSummarizesBatches` | Public Run process selects three records, uses stored batch size two, persists script-requested updates, and reports two successful log links; untouched rows remain unchanged. |
| `testInvalidDraftCodeParametersAndAssociatedReference` | Invalid draft syntax, missing/nonexistent record keys and a non-associated field fail; no draft revision/log or Person change is stored. |
| `testRecordScriptPermissionDeniedThenGrantedWrite` | A caller with read permission cannot perform the script's write; granting edit to the same caller allows it. Native data and error/success logs distinguish both outcomes. |
| `testStoreRevisionDeniedByRecordLockThenAuthorized` | A script WRITE security lock denies revision storage without advancing the pointer; granting its key allows the next revision. |
| `testStoredSyntaxAndRuntimeFailuresThenValidRevisionRecovery` | Store retains source text without compiling it; Run rejects invalid syntax and a runtime exception, records failures, then executes a valid replacement revision. |
| `testRunRecordProcessReportsScriptFailuresWithoutImplicitWrites` | Runtime failures produce ERROR script-log links and no implicit record writes. The process's attempted-record count is not treated as success evidence. |
| `testAdHocPinnedRevisionAndInvalidReferences` | Explicit revision ID runs historical code while script ID runs current code; missing and nonexistent revision references return errors. |

Each fixture closes its private database, restores the prior QContext/session and
named objects, and removes only its own process UUID state. Script IDs are unique
within the test class to avoid reuse through the existing shared script-ID cache.
These are unversioned scripts exercising the existing QRecord fallback; the API
adapter emits a warning when no script API version is specified. Versioned API
record conversion, browser editing and concurrent revision storage are not covered.

**Open non-persistence gap:** [#849](https://github.com/QRun-IO/qqq/issues/849)
records a failing native control: calling the registered `testScript` process with
`qqq.update('person', qqq.newRecord().withValue('id', 1).withValue('firstName',
'Draft write'))` leaves that update committed. The independent SQL assertion
expected `Avery` and observed `Draft write`. Test does not save its draft revision
or logs, but it is not a write-isolation sandbox. The passing local-mutation test
above must not be read as proving otherwise. No production correction or new
runtime policy is included; the intended explicit-write boundary needs resolution.
The original #572 requirement remains unchanged and the ledger row stays pending
for this gap, independent review and combined full verification. Its existing
post-4.0 disposition is not a 4.1 release approval.

**Composition prerequisite:** #591 signed head `65ad000b4` owns the sample's
test-scoped JavaScript provider dependency. This #572 branch adds no dependencies
or POM changes. After composing #591, the normal focused command is:

```sh
mvn -o -f qqq-sample-project/pom.xml \
  -Dmaven.repo.local=/path/to/matching-isolated-cache \
  -Dtest=SampleAssociatedScriptAcceptanceTest test
python3 -B -m unittest discover -s qqq-sample-project -p 'test_*.py'
# Expected nonzero while this feature remains pending.
python3 -B qqq-sample-project/verify-feature-coverage.py \
  --stage source --report-only --require-feature core.scripts.associated
```

Focused local execution on base `fe3f63afd` uses an independent clone of the
stable c65 Maven cache, with matching embedded core/RDBMS/API/Javalin/JavaScript
sources verified. Because that base lacks the #591 dependency, Surefire's
`-Dmaven.test.additionalClasspath` supplies the existing provider, Nashorn 15.7
and its four ASM 7.3.1 jars from the isolated cache. This is focused source evidence,
not the normal combined/full or published-artifact gate. No missing-provider skip
or mock executor is used. Detailed commands and reports are retained locally in
`/private/tmp/qqq-572-evidence`; the Test-mode write red report is
`/private/tmp/qqq-572-test-write-red.log`.

Local final focused evidence: **11 tests passed, zero failures/errors/skips and
zero Checkstyle violations** (`/private/tmp/qqq-572-final-focused.log`). Both owned
fixture negative controls failed as expected (extra edit grant; missing third
selected row) before restoration. All **46 Python checks passed**. The required
feature report exits 1 because scenario review remains pending; all 11 method
bindings resolve. All 130 original IDs, requirements, statuses, stages and release
dispositions are preserved; only this row's gap and passing evidence changed.


## Application API versioning source acceptance (#595)

`SampleApplicationApiVersioningAcceptanceTest` exercises `ApiImplementation.get`,
`update` and `runProcess` against the native sample Person/Pet/Pet Note schema in a
unique owned H2 database. It checks opt-in tables/processes, explicitly selected
fields, initial/final version boundaries, historical and external field names,
bidirectional custom mapping, nested records, duplicate-name rejection and
permission/record-security denials. JDBC independently checks persisted values and
unchanged denied writes. Fixtures restore QContext, close H2, clear table/field API
caches and remove their own process state; unique API names isolate process lookup
caching. No new dependencies, external provider, HTTP server or browser is needed.

**Validation and limits:** [#868](https://github.com/QRun-IO/qqq/issues/868)
corrects the missing-replacement-target gap demonstrated by this fixture.
`QInstanceValidator` now rejects `replacedByFieldName="missingTarget"` before the API
can return a misleading null. Replacement targets must exist in current table
metadata, but need not be exposed in the historical API version. The module's
`ApiReplacementFieldValidationTest` preserves direct mapping semantics and custom
computed fields, rejects unsupported removed-to-removed aliases, and checks
revalidation after field maps are cached. Normal table-version inheritance for
unconfigured fields is unchanged; the fixture explicitly excludes private fields
and separately proves that default. The ledger remains `pending` until independent
review and the main full gate; full HTTP (#596), OpenAPI (#597), published artifacts
and release acceptance remain separate.

After installing this source revision into an owned Maven cache, run from the
repository root:

```sh
mvn -B -o -nsu -Dmaven.repo.local=/path/to/owned-cache \
  -f qqq-sample-project/pom.xml \
  -Dtest=SampleApplicationApiVersioningAcceptanceTest test
python3 -B -m unittest discover -s qqq-sample-project -p 'test_*.py'
```


## Legacy OpenAPI model acceptance (#598)

`SampleOpenApiModelsAcceptanceTest` constructs a legacy document with a typed path
parameter, component schema/reference, response content and bearer security. It
compares the complete parsed JSON tree and literal wire values for all four `In`
and six `Type` constants through both setters and fluent methods. Four paired JDK
compiler controls accept typed calls and reject each removed String overload of
`Parameter.setIn/withIn` and `Schema.setType/withType` at the expected source line
with an incompatible-type diagnostic; compilation uses the sample's resolved
classpath. No new dependency, HTTP server, browser or external provider is needed.

```sh
mvn -f qqq-sample-project/pom.xml \
  -Dtest=SampleOpenApiModelsAcceptanceTest,SampleMigration40ContractTest test
python3 -B -m unittest discover -s qqq-sample-project -p 'test_*.py'
python3 -B qqq-sample-project/verify-feature-coverage.py --stage source --report-only
```

The positive consumer explicitly uses
`JsonUtils.toJsonCustomized(value, builder -> builder.serializationInclusion(JsonInclude.Include.NON_NULL))`.
Default `JsonUtils.toJson` uses `NON_EMPTY` and reproducibly changes
`{"security":[{"BearerAuth":[]}]}` into `{"security":[{}]}`. A separate characterization
asserts this loss and the existing customization's preservation; it does **not**
claim the default serializer is fixed. The related security-description defect is
tracked separately in [#869](https://github.com/QRun-IO/qqq/issues/869). `GenerateOpenApiSpecAction` uses these
legacy models and `JsonUtils.toPrettyJson`; the JSON spec route returns that rendered
string directly, without the handler's separate `ALWAYS` customization. A bounded
generator diagnostic found non-empty permission scopes for an HTTP bearer scheme,
not the empty-scope loss on that endpoint. Both behaviors are described in the
separate issue; no production serializer change is included here.

DTOs serialize missing `info`/`paths`,
empty info, a path parameter without a name and with `required=false`, an operation
without responses, and an array schema without items. Those negative fixtures
characterize the existing permissive DTO boundary; they do not claim invalid-shape
rejection. Null `In` throws `NullPointerException`; null `Type` clears the schema type.
No documented required-shape validator contract was found in these DTOs, and
[#598](https://github.com/QRun-IO/qqq/issues/598) does not require adding one.

The focused source run at base `6215d2b1e` passed 6 acceptance and 7 existing migration
tests, with zero failures/errors/skips and zero Checkstyle violations. It used a
private copy of the verified source cache; all 27 OpenAPI and 1,033 core embedded
Java sources matched that base. `openapi.models` stays **pending** for main integration
and acceptance of the reviewed change. The separate defect is not described as
fixed; no new validation or publication gate is introduced. Existing contracts and
release deferrals remain unchanged.


## Configured messaging source acceptance (#573)

`SampleMessagingAcceptanceTest` uses the normal `SendMessageAction` → `EMAIL`
provider in the sample instance. An owned standard-library SMTP receiver binds
only `127.0.0.1` on an ephemeral port and never relays. Every address is synthetic
under `example.invalid`; there are no credentials, environment-derived endpoints,
live AWS/SMTP/Slack/SMS/email accounts or recipients, added dependencies, or
runtime changes in the original #573 slice. Source inspection found EMAIL
host/port configuration and SES region/credentials configuration; the existing SES fixture injects a client below
normal dispatch, so this acceptance slice uses EMAIL. With the bounded #870
Reply-To correction, the ten methods cover:

| Method | Observed boundary |
| --- | --- |
| `testConfiguredMessageEnvelopeAndBody` | Exact MAIL FROM and To/Cc/Bcc RCPT envelope, decoded sender/subject/UTF-8 text and HTML, Bcc hidden in MIME, unselected provider untouched. |
| `testExplicitReplyToAfterFrom` | Actual MIME has the requested reply address/label, excluding the implicit sender fallback. |
| `testMultipleExplicitReplyToAfterFrom` | All explicit reply addresses survive in order; SMTP delivery recipients remain unchanged. |
| `testMultipleExplicitReplyToAroundFrom` | A From party between reply parties does not overwrite or truncate the explicit list. |
| `testSubsequentMessageDoesNotReuseRecipients` | Reconfigured owned receiver gets only the new recipient, without old Cc/Bcc. |
| `testMissingAndUnknownProviderConfiguration` | Null/empty/blank and unknown provider names fail without a receiver connection. |
| `testUnsupportedProviderType` | Unsupported configured type fails without fallback transmission. |
| `testInvalidReceiverRejectedBeforeTransmission` | Invalid recipient role and malformed address, even after a valid party, fail before any connection. |
| `testReceiverRefusalDoesNotTransmitBody` | Local SMTP 550 rejection propagates; no DATA, acceptance, or unrelated-provider connection. |
| `testDeliveryFailureAfterBodyDoesNotFallback` | Local SMTP 554 after DATA propagates; captured body is not reported as accepted and no fallback occurs. |

```sh
mvn -B -o -nsu -Dmaven.repo.local=/path/to/owned-matching-cache \
  -f qqq-sample-project/pom.xml -Dtest=SampleMessagingAcceptanceTest test
python3 -B -m unittest discover -s qqq-sample-project -p 'test_*.py'
# Expected exit 1 while independent review/full composition remain pending.
python3 -B qqq-sample-project/verify-feature-coverage.py \
  --stage source --report-only --require-feature core.messaging
```

The `core.messaging` row remains **pending**; these are focused source checks,
not full composition, independent review, live provider, native/browser or
published-artifact acceptance. [#870](https://github.com/QRun-IO/qqq/issues/870)
tracks the provisionally Medium Reply-To defect discovered in #573: the wire
reproduction expected `reply@example.invalid` but observed `sender@example.invalid`.
This branch corrects `SendEmailAction.addSender()` to inspect only an explicit
Reply-To header and append each supplied reply party. Three retained wire
regressions cover single/multiple addresses after From and multiple addresses
around From; the ordinary From case also preserves its absent-header fallback.
Reply parties are never added to the SMTP envelope. The original #573 candidate
remains at `f9c4a9b91e7c`; its evidence is in `qqq-573-messaging-evidence`.
The fix's red/green reports, rebuilt-core provenance and focused logs are in
`qqq-870-email-reply-to-evidence`. Run these added tests against the rebuilt core
in an owned cache; the original unpatched cache fails all three regressions.
All 130 ledger contracts, stages, statuses, original issue links and release
dispositions are preserved; this is not release approval.


## Public OpenAPI security requirements (#869)

`SampleOpenApiSecurityAcceptanceTest` exercises the real JSON and YAML spec routes
using owned memory metadata and an ephemeral loopback HTTP server. Explicit HTTP
bearer and API-key requirements retain their named empty scope arrays; OAuth
permission scopes remain non-empty, and anonymous operations retain `security: []`.
An unsupported version returns the existing error response rather than a spec.
These checks cover security descriptions, not authentication enforcement. The
generator's document rendering selects `NON_NULL` through existing JSON/YAML hooks;
shared serialization defaults remain unchanged, as the #598 characterization shows.

Run `mvn -pl qqq-middleware-api install`, then
`mvn -f qqq-sample-project/pom.xml -Dtest=SampleOpenApiSecurityAcceptanceTest,SampleOpenApiModelsAcceptanceTest,SampleMigration40ContractTest test`
against the same Maven repository. Native local sockets are required. The focused
checks exercise no external auth provider and introduce no release deferral;
[#869](https://github.com/QRun-IO/qqq/issues/869) records the original defect.


## V1 metadata/query/count source acceptance (#592)

`SampleV1TableHttpAcceptanceTest` serves sample metadata through `QApplicationJavalinServer` against a unique H2 database seeded with the canonical synthetic Person/Pet rows. It avoids the demo launcher's default database priming and broker startup. A unique first-name sentinel must appear over HTTP, and the actual connection URL must match the direct JDBC oracle; teardown restores the caller's context objects. It asserts V1 instance/table/process metadata, filtered/joined/sorted query and count with independent JDBC identities, empty responses, default limits and explicit paging. The configured two-row default bounds requests without a limit; count remains independent. Missing/null filters retain unfiltered behavior. Wrong non-null filter types return400 after [#871](https://github.com/QRun-IO/qqq/issues/871); malformed JSON/operators and unknown tables cannot return successful data.

Permission controls require403 for Person query/count while Pet remains readable. The existing mock authentication provider's `Bearer Deny` token exercises actual401 responses on all five endpoint families, each with an allowed control. This proves local middleware rejection, not an external identity-provider integration. Existing presentation and joined-privacy tests run alongside the seven new cases. No Next UI change or browser/public-artifact acceptance is included.

Run with a source-matching private cache after installing the corrected Javalin module:

```sh
mvn -B -o -Dmaven.repo.local=/path/to/owned-cache \
  -f qqq-sample-project/pom.xml \
  -Dtest=SampleV1TableHttpAcceptanceTest,SamplePresentationMetadataTest,SampleJavalinServerTest test
python3 -m unittest discover -s qqq-sample-project -p 'test_*.py'
```

`http.metadata.tables` remains pending independent review and full combined acceptance. All original feature contracts, stages, statuses and release dispositions remain unchanged.


## Application API HTTP source acceptance (#596)

`SampleApplicationApiHttpAcceptanceTest` configures real `QJavalinApiHandler`
routes on an owned `127.0.0.1` ephemeral port, native table-based session-cookie
authentication, and the sample Person/Pet schema in private H2. Nine scenarios
exercise get/query (including query counts), insert/patch/delete, mixed bulk
outcomes, five custom process methods and their configured 405s, deterministic
async status, process body/status/Content-Type transformation, CORS headers and
HTML error negotiation. Negatives cover malformed JSON, invalid types/filters,
missing/unknown records, missing/invalid sessions, denied processes and server
errors. JDBC checks persisted and unchanged rows. Fixtures disable unrelated ESB
metadata, restore context/server state and remove their owned process/session
state; no external service, browser or additional dependency is required.

[#872](https://github.com/QRun-IO/qqq/issues/872) records the native red proof that
a malformed numeric field produced 500. The API input adapter now rejects invalid
directly mapped values as 400 before table actions, using the destination field's
native type without rewriting raw values. Custom mappers retain control of their
input syntax; unrelated process errors remain 500. Module regressions cover
current, renamed and historical fields, valid coercion/null/ignored-field behavior
and custom mapping. This is source evidence only: the ledger stays `pending` for
independent review and the main full gate. No standalone count endpoint is assumed;
OpenAPI acceptance (#597), security generation (#869), published artifacts and
release acceptance remain separate. After installing this revision into an owned
Maven cache, run from the repository root:

```sh
mvn -B -o -nsu -Dmaven.repo.local=/path/to/owned-cache \
  -f qqq-sample-project/pom.xml \
  -Dtest=SampleApplicationApiHttpAcceptanceTest test
python3 -B -m unittest discover -s qqq-sample-project -p 'test_*.py'
```


## SQS utility source acceptance (#604)

[`SampleSqsUtilityAcceptanceTest`](../qqq-utility-lambdas/src/test/java/com/kingsrook/sampleapp/SampleSqsUtilityAcceptanceTest.java) lives in the utility module and calls the real stream handler/default AWS SDK client. Fresh child JVMs use cleared environments, isolated credentials/home, disabled instance metadata, and synthetic identities. An owned loopback SQS JSON protocol listener independently checks exact QueueUrl and MessageBody. This is separate from the new ESB and does not change its providers or replace existing SQS integration.

Eight cases cover UTF-8/raw malformed text, empty-message/service rejection, credential failure without send, absent/blank queue configuration, and no input body in captured success/failure Lambda logs. [#878](https://github.com/QRun-IO/qqq/issues/878) removes unconditional body logging; [#879](https://github.com/QRun-IO/qqq/issues/879) validates queue configuration before credentials. Error diagnostics still include service-provided text; this does not certify arbitrary provider-message sanitization, live AWS/IAM, deployed Lambda behavior or published artifacts.

The sample does not automatically run the utility module. Run its normal source gate and explicitly import the fresh report for the existing ledger verifier:

```sh
mvn -B -o -Dmaven.repo.local="$OWNED_M2" -pl qqq-utility-lambdas clean verify
mkdir -p qqq-sample-project/target/surefire-reports
cp qqq-utility-lambdas/target/surefire-reports/TEST-com.kingsrook.sampleapp.SampleSqsUtilityAcceptanceTest.xml qqq-sample-project/target/surefire-reports/
python3 -B -m unittest discover -s qqq-sample-project -p 'test_*.py'
python3 -B qqq-sample-project/verify-feature-coverage.py --stage source --report-only --require-feature utility-lambda.sqs
```

The last command still reports pending scenario review until independent and combined acceptance are complete. All130 original contracts/statuses/stages and release dispositions are preserved; no release deferral is added.


## Metadata-driven OpenAPI acceptance (#597)

`SampleOpenApiAcceptanceTest` uses the shared `SampleOpenApiHttpFixture` to parse real loopback JSON/YAML specs for two configured versions, compare external field names and schemas with served records, execute documented string/number query examples, and check HTTP security declarations. It asserts excluded, unconfigured and future endpoints are absent from both the documents and served API. Missing API metadata/name/version and a missing table primary key exercise existing generator errors; these checks do not introduce DTO validation. Every local reference is resolved, and deleting the referenced table schema from a document copy proves the dangling-reference oracle fails. [#873](https://github.com/QRun-IO/qqq/issues/873) fixes the reproduced malformed example-pointer prefix.

Run `mvn -f qqq-sample-project/pom.xml -Dtest=SampleOpenApiAcceptanceTest,SampleOpenApiSecurityAcceptanceTest test` against matching framework sources. [#874](https://github.com/QRun-IO/qqq/issues/874) corrects the reproduced renamed-filter HTTP 500: `testRenamedQueryFieldMatchesDocumentedAlias` now requires successful filtering through the documented alias for both configured versions. Additional HTTP checks cover historical replacement aliases and reject wrong-version/API, excluded, internal and unknown names; malformed criteria keep the public alias in their error. Native `ApiRenamedQueryTest` retains replacement/custom-mapper precedence and covers negation, ordering and no-match results. The owned MOCK authentication fixture proves document/route behavior, not live OAuth authentication or permission enforcement. This is focused evidence, not full OpenAPI schema validation or exhaustive API acceptance; the ledger remains pending main review/integration and retains its existing release disposition.

The #874 correction maps only direct API aliases to native criterion names after existing API/version field selection. It does not change filter operators, values, permission checks or custom mappers. [#877](https://github.com/QRun-IO/qqq/issues/877) additionally converts directly mapped and replacement DATE criteria with the existing native date parser at the application API boundary. `SampleApiDateQueryAcceptanceTest` proves real HTTP equality through current/historical aliases, supported date formats, IN/BETWEEN, no-match and blank results, invalid-date 400 responses, STRING controls and raw-input custom mapper precedence. Native `ApiDateQueryTest` contrasts typed native equality with unchanged raw-string memory behavior and verifies replacement native-type precedence. Run `mvn -f qqq-sample-project/pom.xml -Dtest=SampleApiDateQueryAcceptanceTest test` against matching framework sources. DATE pattern operators and other field types retain their existing behavior; this does not claim stricter calendar validation, exhaustive backend parity or runtime authentication enforcement.


## Lambda dispatch source acceptance (#601)

[`SampleLambdaDispatchAcceptanceTest`](../qqq-middleware-lambda/src/test/java/com/kingsrook/sampleapp/SampleLambdaDispatchAcceptanceTest.java)
exercises real Lambda stream/custom handlers and `/processes/{name}/init` locally.
It lives in the Lambda module's test tree to use existing dependencies; the sample
POM is unchanged. Events, memory metadata and identities are owned synthetic
fixtures. The application adapter supplies already-resolved sessions using the
existing MOCK provider and clears `QContext` in `finally`; this is explicit
application behavior, not authentication or automatic cleanup supplied by
`QStandardLambdaHandler`. Its default `setupSession()` is empty, and
`setQInstance()` alone is insufficient.

| Method | Observed boundary |
| --- | --- |
| `testCustomStreamRequestResponse` | Stream parsing, UTF-8 custom values, path/query/header mapping and distinct request IDs. |
| `testMalformedCustomRequestsNeverDispatch` | Malformed event/body, unsupported content type and missing request context produce errors without calling custom logic. |
| `testCustomExceptionsPreserveRequestIdentity` | User-facing and internal custom exceptions produce correlated error bodies. |
| `testConfiguredProcessInitAndRequestIsolation` | Actual process steps receive request bodies and the configured instance/session; successive callers have separate outputs and process UUIDs. |
| `testMissingAndInvalidSessionCannotRunProcess` | Core rejects missing/invalid MOCK sessions before executing a step; a valid-session control executes. |
| `testUnconfiguredStandardHandlerDoesNotSupplyContext` | Setting the handler instance without application context fails before process execution. |
| `testProcessFailuresAndRecoveryCleanContext` | User-facing/internal process errors remain body errors; application cleanup precedes a successful recovery request. |
| `testUnknownPathAndProcessDoNotExecute` | Unknown paths and process names cannot invoke the configured process. |
| `testUnimplementedStandardRoutesStayUnavailable` | Metadata/query/count/export/get/update/delete/possible-values/widget stubs return errors, not supported CRUD. |

The stream writes `QLambdaResponse.Body`, not the Java status/header envelope;
deployed HTTP semantics require application event mapping. Process callers must
inspect `body.error`/`body.userFacingError`. All nine methods assert caller context
is clear immediately after invocation, before teardown. No AWS account, endpoint,
credentials, broker, browser or new dependency is used. Long-running async
completion and deployed/published-artifact behavior are not covered.

Run from the repository root with a source-matching owned cache:

```sh
mvn -B -o -nsu -Dmaven.repo.local="$OWNED_M2" -pl qqq-middleware-lambda clean verify
python3 -B -m unittest discover -s qqq-sample-project -p 'test_*.py'
mkdir -p qqq-sample-project/target/surefire-reports
cp qqq-middleware-lambda/target/surefire-reports/TEST-com.kingsrook.sampleapp.SampleLambdaDispatchAcceptanceTest.xml qqq-sample-project/target/surefire-reports/
python3 -B qqq-sample-project/verify-feature-coverage.py --stage source --report-only --require-feature lambda.dispatch
```

The explicit copy imports the freshly generated module report for the sample
ledger verifier; it is not a sample-module test run. The required-feature command
continues to return 1 while `lambda.dispatch` remains **pending** independent
review and combined acceptance. All 130 contracts, stages, statuses, issue links,
release dispositions and deferrals are preserved. Focused evidence lives in the
workspace's `qqq-601-lambda-dispatch-evidence`; this is not release approval.

## Upload, download and saved-report source acceptance (#594)

For [#594](https://github.com/QRun-IO/qqq/issues/594), the retained upload/download/saved-report tests now boot `QApplicationJavalinServer` on owned loopback ports, with a private H2 backend and filesystem roots, avoiding demo auto-priming and broker startup. Direct JDBC marker changes identify the database actually used by HTTP processing and saved-report generation. Added cases check V1 init/step upload archive/process/download bytes, session/reference grants, missing and malformed uploads, saved-report archive/history and API stream bytes, and later storage/process failures. The existing denial, size-limit, traversal, forged-download and saved-report cleanup cases remain. Archive retention after a later upload failure is documented behavior: the fixture removes only its retained file and proves an existing sibling survives; it does not claim transactional rollback.

The focused set contains 63 native cases (14 upload, 20 download, 29 saved-report); `http.uploads` maps 39 relevant methods while retaining all prior 29 bindings. The [#876](https://github.com/QRun-IO/qqq/issues/876) follow-up requires 400 for malformed multipart on legacy run and V1 init/step, without archive writes or execution, and verifies a valid retry still consumes and archives exact bytes. The narrow correction maps only wrapped Jetty 400 errors from multipart form-parameter reading to the existing bad-request contract; generic exception handling is unchanged. Acceptance remains pending independent review and the main full gate. Existing deferrals #488–492/#497/#465 and retained #493/#454–458 remain; archived #448/#494/#495/#496/#497 source-policy assertions are not restored.

## Slack route and message source acceptance (#602 / #603)

`SampleSlackRoutesAcceptanceTest` sends real encoded form POSTs to the native
`QSlackImplementation` routes on an owned `127.0.0.1` ephemeral listener. It uses
canonical sample Person/Pet records in UUID H2, verifies the provider URL, and
checks a unique SQL value in the get response. Query output is the existing
record-identity text, not a full record projection. Metadata lists tables and
processes; a process execution command is unsupported. Missing/unknown native
sessions, denied Person READ with a readable Pet control, and malformed commands
produce the existing HTTP200 Slack Error-block envelope. The fixture restores
context objects/static instances, removes owned session state and closes H2/HTTP.

Authentication is QQQ's real table-based session-cookie module with synthetic
local identities. Slack form `user_id` and `token` do not authenticate requests
and are not verified: a valid QQQ cookie remains authoritative. This is not proof
of Slack signing, live token validation, external identity providers, or browser
cookie behavior. [#880](https://github.com/QRun-IO/qqq/issues/880) corrects session
setup ordering before table input setters; [#881](https://github.com/QRun-IO/qqq/issues/881)
handles untyped widgets in metadata; [#883](https://github.com/QRun-IO/qqq/issues/883)
applies existing USER/READ checks to query/get/export. Export transport itself is
not exercised by these sample cases. No process-execution feature is added.

`SampleSlackMessagesAcceptanceTest` converts statistics/chart/line data and
inspects actual SDK HTTP payloads at an owned local receiver. Each outbound call
uses a separate JVM with an empty inherited environment, a fresh temporary cwd,
and only synthetic token/channel values. The production helper hardcodes the
SDK's immutable singleton, so test-only reflection redirects its endpoint inside
that child before any call; endpoint equality is asserted. Native request
construction remains intact. The SDK's `auth.test` lookup and `chat.postMessage`
are both local, and the captured channel/text/markdown/authentication are checked.
The child explicitly exits after SDK close; natural whole-JVM termination and
parallel shared-server isolation are not claimed.

Empty/unsupported widget messages and API/HTTP rejection are asserted. The void
adapter logs service rejection and does not return delivery confirmation.
[#882](https://github.com/QRun-IO/qqq/issues/882) prevents dispatch when the token
or channel is absent. No live Slack service, account, recipient, export upload or
additional third-party library is used. The sample's only dependency wiring adds
the existing reactor-version Slack module in test scope.

After installing source-matching modules into an owned cache:

```sh
mvn -B -o -nsu -Dmaven.repo.local=/path/to/owned-cache \
  -pl qqq-middleware-slack clean install
mvn -B -o -nsu -Dmaven.repo.local=/path/to/owned-cache \
  -f qqq-sample-project/pom.xml \
  -Dtest=SampleSlackRoutesAcceptanceTest,SampleSlackMessagesAcceptanceTest,SamplePresentationMetadataTest,SampleMetaDataProviderTest test
python3 -B -m unittest discover -s qqq-sample-project -p 'test_*.py'
```

The Slack module currently has no module-local tests; its normal build runs
Checkstyle/static checks, while these native sample tests supply the regressions.
No module coverage ratio is claimed. Both ledger rows stay `pending` for
independent review and the main full gate. All130 original contracts, stages,
statuses and release dispositions remain unchanged; no Next or held OAuth work is
included, and no full sample/published/release acceptance is claimed.
