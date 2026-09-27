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
ambiguous implicit provider inputs are rejected without data changes. One runtime
gap remains at source base `7bc761154`: an explicitly unknown provider name returns
`ok=true` and leaves pending rows unprocessed. A rejection regression reproduces
that false-success response in `/private/tmp/qqq-567-unknown-provider-red.log`;
the retained test characterizes it and proves a subsequent valid-provider call
consumes the same pending work. No production fix or policy change is included.
PRE_DELETE remains documented unsupported/excluded; scheduler timing, competing
workers, atomic rollback and exactly-once delivery are not claimed. The ledger
retains all 130 requirements and the `pending` source status and existing 4.0
deferral; neither independent review nor full sample/release gates are complete.

Focused reproduction with matching source artifacts in an isolated Maven cache:

```sh
mvn -o -f qqq-sample-project/pom.xml -Dmaven.repo.local="$AUTOMATION_ACCEPTANCE_M2" \
  -Dtest=SampleAutomationAcceptanceTest test
python3 -B -m unittest discover -s qqq-sample-project -p 'test_*.py'
```

The initial nine-case focused run passes with zero failures/errors/skips and zero
Checkstyle violations (`/private/tmp/qqq-567-focused-final.log`). Test-owned fault
injection was checked red-to-green for code and child-process failures; deliberately
reversing priority and changing batch size to three produces two native-oracle
failures (`/private/tmp/qqq-567-priority-batch-mutation.log`), then restoration passes.
No full sample run was started for this branch; it requires the coordinated full
verification slot (and any separately reviewed packaged-child port helper).
