# QQQ Sample Project

QRun-owned reference application for QQQ 4.0: tables, related records, processes, widgets, the Material Dashboard 0.40.0, and a PicoCLI entry point. Requires Java 21 and Maven 3.8 or later. The default database is an in-memory H2 database populated with sample data at server startup.

## Build and run

From the QQQ repository root, install the framework modules, then build this separate sample:

```bash
mvn clean install
mvn -f qqq-sample-project/pom.xml -Pacceptance-tests clean verify
mvn -f qqq-sample-project/pom.xml exec:java \
  -Dexec.mainClass=com.kingsrook.sampleapp.SampleJavalinServer \
  -Dqqq.sample.mockAuthentication=true
```

Open <http://localhost:8000/>. Expand **People App**, open **Greetings App**, and select **Person**. Open a record and choose **Actions → Greet Interactive** to try the local mock process. Stop the server with Ctrl+C; restarting it recreates the sample database. The explicit `qqq.sample.mockAuthentication` option selects bundled mock authentication for local exploration. Without it, configure `OAUTH2_BASE_URL`, `OAUTH2_CLIENT_ID`, `OAUTH2_CLIENT_SECRET`, and `OAUTH2_SCOPES` in the process environment or a `.env` file. Mock authentication is for local sample data only.

Open **Miscellaneous → Field Lab** to create records with every declared field type and try length limits, case/whitespace normalization, and numeric bounds. Password values are synthetic demonstration data: read masking is not encryption or authentication storage. The sample configures H2 connections for UTC timestamp storage.

Field Lab also demonstrates DATE and DATE_TIME creation/modification defaults, an explicit `NONE` opt-out, and user defaults on writes. DATE defaults use the server's local calendar date; DATE_TIME defaults use the current instant. Reads do not invent a user ID for stored blanks. Case normalization can run on writes, reads or filters; read normalization does not rewrite stored data. The [field reference](../docs/metaData/Fields.adoc) describes these contracts and configuration boundaries.

To inspect the command-line interface:

```bash
mvn -f qqq-sample-project/pom.xml exec:java \
  -Dexec.mainClass=com.kingsrook.sampleapp.SampleCli -Dexec.args="--help" \
  -Dqqq.sample.mockAuthentication=true
```

## Acceptance coverage

This application is the first-party release acceptance target. It uses synthetic H2 data and explicit local mock authentication; no downstream application, account or sign-off is involved. Field Lab exposes every field type, length/case/whitespace/range policy examples, dynamic defaults, and fixed/record-specific timezone display. Its tests cover daylight-saving boundaries and invalid/missing-zone fallback as well as persistence. `SampleJavalinServerTest` exercises the real dashboard bundle, metadata, HTTP CRUD and query, independent JDBC readback, required-field rejection without persistence, and greeting-process output.

The required goal is coverage of every supported QQQ feature and use case. The [feature inventory](feature-coverage.json) records the remaining scenario/browser/integration gaps; the current sample does **not** yet meet that goal. Core unit-test coverage alone does not establish sample feature coverage. A verified feature needs a reviewed list of public contracts, runnable examples, and assertions for their successful and rejected outcomes. Passing one example does not certify the entire family: filtered tenant counts, distinct counts, update normalization, filter normalization, DATE defaults and DATE_TIME defaults are separate cases.

Field Lab is part of the running application. Tenant security currently uses programmatic test fixtures; it is not yet a navigable sample mode. MySQL/PostgreSQL tests use explicitly disposable containers. These forms of evidence remain distinct so an integration test is not mistaken for a runnable application example.

Run acceptance with Java 21, Docker, and a locally installed Chrome (or Chrome for Testing):

```bash
mvn -f qqq-sample-project/pom.xml -Pacceptance-tests clean verify
python3 qqq-sample-project/verify-feature-coverage.py --report-only
```

The profile runs browser workflows, packaged configuration and SPA launchers from empty working directories, CLI commands, and Field Lab temporal readback against disposable MySQL 8.4 and PostgreSQL 17 containers. It pulls the database images when needed, requires no shared database or provider account, and fails if Docker is unavailable. The browser uses a new disposable profile and the test owns/stops its sample server and database containers. Screenshots and test reports are under `qqq-sample-project/target/`. Use the acceptance profile for this module's `verify` or `install`: its coverage gate includes the packaged launchers. For lightweight checks, `mvn -f qqq-sample-project/pom.xml test` runs ordinary unit tests without Chrome or Docker. The coverage checker without `--report-only` fails for pending, unmapped, skipped, missing or failed feature checks. `verified_tests` binds current evidence; signed Git history preserves the initial audit. A reviewed enum-only declaration is listed separately as unsupported; implemented backend-only contracts still require tests.

The checker pins the reviewed set of feature IDs and allows only `train.bom` to wait for published artifacts. Removing/renaming a feature or changing that boundary requires a source review and an explicit checker change. This prevents accidental scope shrinkage; it does not prove the inventory is exhaustive. Always run a clean Maven verification immediately before checking the ledger, because XML test reports alone do not identify the source revision they exercised.

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
