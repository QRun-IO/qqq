# QQQ Sample Project

QRun-owned reference application for QQQ 4.0: tables, related records, processes, widgets, the Material Dashboard 0.40.0, and a PicoCLI entry point. Requires Java 21 and Maven 3.8 or later. The default database is an in-memory H2 database populated with sample data at server startup.

## Build and run

From the QQQ repository root, install the framework modules, then build this separate sample:

```bash
mvn clean install
mvn -f qqq-sample-project/pom.xml clean verify
mvn -f qqq-sample-project/pom.xml exec:java \
  -Dexec.mainClass=com.kingsrook.sampleapp.SampleJavalinServer \
  -Dqqq.sample.mockAuthentication=true
```

Open <http://localhost:8000/>. Expand **People App**, open **Greetings App**, and select **Person**. Open a record and choose **Actions → Greet Interactive** to try the local mock process. Stop the server with Ctrl+C; restarting it recreates the sample database. The explicit `qqq.sample.mockAuthentication` option selects bundled mock authentication for local exploration. Without it, configure `OAUTH2_BASE_URL`, `OAUTH2_CLIENT_ID`, `OAUTH2_CLIENT_SECRET`, and `OAUTH2_SCOPES` in the process environment or a `.env` file. Mock authentication is for local sample data only.

Open **Miscellaneous → Field Lab** to create records with every declared field type and try length limits, case/whitespace normalization, and numeric bounds. Password values are synthetic demonstration data: read masking is not encryption or authentication storage. The sample configures H2 connections for UTC timestamp storage.

To inspect the command-line interface:

```bash
mvn -f qqq-sample-project/pom.xml exec:java \
  -Dexec.mainClass=com.kingsrook.sampleapp.SampleCli -Dexec.args="--help" \
  -Dqqq.sample.mockAuthentication=true
```

## Acceptance coverage

This application is the first-party release acceptance target. It uses synthetic H2 data and explicit local mock authentication; no downstream application, account or sign-off is involved. Field Lab exposes every field type, length/case/whitespace/range policy examples, dynamic defaults, and fixed/record-specific timezone display. Its tests cover daylight-saving boundaries and invalid/missing-zone fallback as well as persistence. `SampleJavalinServerTest` exercises the real dashboard bundle, metadata, HTTP CRUD and query, independent JDBC readback, required-field rejection without persistence, and greeting-process output.

The required goal is coverage of every supported QQQ feature and use case. The [feature inventory](feature-coverage.json) records the remaining scenario/browser/integration gaps; the current sample does **not** yet meet that goal. Core unit-test coverage alone does not establish sample feature coverage.

Run acceptance with Java 21, Docker, and a locally installed Chrome (or Chrome for Testing):

```bash
mvn -f qqq-sample-project/pom.xml -Pacceptance-tests clean verify
python3 qqq-sample-project/verify-feature-coverage.py --report-only
```

The profile runs browser workflows, packaged static/SPA routes, and Field Lab temporal readback against disposable MySQL 8.4 and PostgreSQL 17 containers. It pulls the database images when needed, requires no shared database or provider account, and fails if Docker is unavailable. The browser uses a new disposable profile and the test owns/stops its sample server and database containers. Screenshots and test reports are under `qqq-sample-project/target/`. These integration tests are mandatory for release acceptance; ordinary unit tests remain available without Chrome or Docker. The coverage checker without `--report-only` fails for pending, unmapped, skipped, missing or failed feature checks. `baseline_sample` in the inventory preserves the initial audit; `verified_tests` binds current evidence. A reviewed enum-only declaration is listed separately as unsupported; implemented backend-only contracts still require tests.

CI runs the sample for feature changes and before publication. RC/final/hotfix publication requires `--stage source` to pass; public-artifact resolution is checked after publication by the default `published` stage. A source-stage pass explicitly leaves those artifact checks deferred and never claims complete release acceptance. Run `python3 qqq-sample-project/test_feature_coverage.py` to check the gate itself.

After committing the sample and publishing the candidate, validate those public artifacts using Python 3.12 or later:

```bash
python3 qqq-sample-project/verify-published.py 4.0.0-RC.3
```

Supply the version actually published. This exports committed `HEAD`, resolves the literal parent from Central with empty user/global settings and a new cache, runs the complete sample acceptance profile, and checks the feature ledger. It does not install local framework artifacts. It retains `maven.log` and `acceptance.json` under `target/published-*`; the complete feature gate remains open until every supported scenario is verified.

## Source entry points

All paths below are under `src/main/java/com/kingsrook/sampleapp/`:

- `metadata/SampleMetaDataProvider.java`: database, tables, processes, and navigation.
- `metadata/FieldLabTableMetaDataProducer.java`: field types and behavior examples.
- `SampleJavalinServer.java`: HTTP server and sample database initialization.
- `SampleCli.java`: command-line entry point.

This sample is built from the repository and is not published to Maven Central. See the [4.0 migration guide](../docs/migration/4.0.adoc) for API changes.

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
