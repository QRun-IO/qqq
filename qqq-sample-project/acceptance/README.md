# Starter and application QBit source acceptance (#607, #608)

By owner decision on 2026-10-13, the per-RC Next waiver approval gate was removed. Releases no longer require an owner comment on issue #798. Acceptance results for the seven Next-owned successor features are a non-blocking release report: missing receipts, pending mappings and pending review are recorded with reasons in `next_report_only_features`, excluded from verified counts, and keep `complete=false`. Reviewed receipts are evaluated as before and all native results remain in `next_acceptance`. Failed mapped tests and supplied receipts or bundles rejected as invalid or tampered still fail strict coverage. All non-Next strict requirements remain unchanged.

This fixture uses local checkouts of the first-party `qqq-app-starter` and
`qbit-template-application` repositories. It installs the selected QQQ source
revision into a dedicated Maven cache, makes disposable copies of both
consumers, follows the template's documented package/class/coordinate rename,
registers the resulting QBit in the copied starter, then runs JUnit against
the actual host. It does not edit either upstream repository.

```sh
export QQQ_STARTER_SOURCE=/path/to/qqq-app-starter
export QQQ_TEMPLATE_SOURCE=/path/to/qbit-template-application
export QQQ_SOURCE=/path/to/qqq
python3 -m unittest discover -s "$QQQ_SOURCE/qqq-sample-project/acceptance" -p 'test_starter_application.py' -v
python3 "$QQQ_SOURCE/qqq-sample-project/acceptance/run_starter_application.py" \
  --qqq-source "$QQQ_SOURCE" \
  --starter-source "$QQQ_STARTER_SOURCE" \
  --template-source "$QQQ_TEMPLATE_SOURCE" \
  --maven-repo /tmp/qqq-acceptance-m2 \
  --live-postgres \
  --junit-dir "$QQQ_SOURCE/qqq-sample-project/target/starter-application-junit"
```

Populate the dedicated Maven cache from a normal Maven build first, or pass
`--online` to resolve dependencies into a fresh dedicated cache. The runner
uses offline Maven by default. With `--online`, Maven uses `-nsu` so it can
resolve public dependencies without refreshing locally installed QQQ snapshots.
CI passes `--candidate-version 0.0.0-sample-acceptance`, matching the earlier
sample framework install. The runner installs the checked-out QQQ reactor at
that exact candidate version, checks that its installed JARs match the reactor
JARs, and builds both consumers against it. It prints all three source commit
IDs, the root revision, and the candidate version. It fails when the generated
QBit is not a valid live host integration; a successful compile alone is
insufficient.
The runner copies only tracked, non-secret-like source paths from the two
first-party checkouts. Its work directory is removed on exit, including on
failure. Supply a new path under the OS temporary directory with `--workdir`
only when inspecting a failed fixture; this explicitly retains generated
files and the starter server log. Never point `--workdir` at existing data.

The source gate pins starter `2fb62378fa76da1950dc9e67984c18fd0491ae3f`
and the public template fix PR #5 head
`70a72a54f4a3024e39d07a7d86c5acf738283286`. The latter is fetched from
`refs/pull/5/head` until that PR merges. CI rejects a different checkout head,
uses a fresh dedicated Maven repository, and runs the copied consumer against
the current QQQ source checkout. It records generated QBit and starter JUnit
reports plus a live-runner JUnit case under `target/starter-application-junit`.
The source release gate requires both inventory rows to pass from those reports.

`--live-postgres` requires Docker, the `postgres:17-alpine` image (the one the
PostgreSQL module and sample integration tests use), Java and free loopback
port 8000. Nothing is prepared by hand: the runner starts a disposable
PostgreSQL container with a random password and a Docker-assigned host port,
waits until it accepts connections, loads `starter-application-postgres.sql`,
packages and starts the copied starter, and performs real HTTP CRUD. Deletes
must report one deleted record and leave no readable or queryable row. A
separate live JUnit test proves an allowed insert persists and a denied insert
returns 403 without a DB write. The runner then probes a missing child table,
an unavailable DB, and absent DB environment variables. Every check is required
when `--live-postgres` is selected. The container is removed on success and on
failure, and a cleanup problem is only reported as a warning, never in place of
the failure being diagnosed. Without this switch, the runner covers compilation,
metadata and no-DB auth behavior only; that shorter run is not generated-consumer
acceptance. The #607/#608 source rows are mapped to the live reports
(`sourceGeneratedPostgresCrudAndNegativeProbes`). Published-artifact acceptance
remains pending. `--live-mysql`, `--mysql-host` and `--mysql-bind-address` no
longer exist and fail with a message naming the replacement.

The upstream starter's own RDBMS backend only builds MySQL and H2 connections, so
for this run the disposable copy of the starter (never the checkout) also gets
the `qqq-backend-module-postgres` dependency and builds a
`PostgreSQLBackendMetaData` when `RDBMS_VENDOR` is `postgres`. The runner installs
that module into the dedicated Maven cache with the other candidate modules.

The database endpoint is applied in two places that must agree: the packaged
server's process environment and the Surefire `environmentVariables` block of the
copied starter's pom (which overrides the Maven process environment and is what
the live permission test JVM reads). The runner rewrites and re-reads the copy's
six `RDBMS_*` values and fails if the pom does not declare one of them.

The address the acceptance process uses to reach the published database port is
chosen like Testcontainers does for the sample integration tests:
`TESTCONTAINERS_HOST_OVERRIDE`, then the host of a `tcp://` `DOCKER_HOST`, then
the default gateway when the process itself runs in a container, then
127.0.0.1. The runner checks that the published port accepts connections from
the acceptance process before building, and fails with the host and port when it
does not. Pass `--postgres-host` only when that discovery is wrong for a Docker
setup, and `--postgres-bind-address` only to change where the Docker host
publishes the port (default 127.0.0.1 for a loopback database host, otherwise
0.0.0.0, protected by the random password). The application still uses local
port 8000. Original source checkouts remain unchanged, and all live acceptance
checks are still required.
