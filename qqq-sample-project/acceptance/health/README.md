# Health source acceptance (#599, #600)

`SampleHealthAcceptanceTest` uses the sample's metadata and server. Its
`healthfixture.SampleHealthMetaDataProducer` is discovered through the real
metadata package scanner and registers the existing health route provider.
The fixture package is outside the normal application's metadata scan. The
health module dependency is test-scoped; no runtime or authentication behavior
is changed.

All listeners use ephemeral ports. HTTP binds to loopback. The database test
creates its own H2 TCP service and memory database, inserts and preserves an
owned row, verifies probe connections are closed, then stops that same service
to prove the unavailable-database response. Memory tests change thresholds
without exhausting the heap. Disk tests use a JUnit temporary directory and
thresholds around its reported free space without filling the disk.

| Requirement | Sample evidence |
| --- | --- |
| Autoload and default route | `testAutoloadedDefaultEndpoint` |
| Configured path; disabled route truly absent | `testConfiguredEndpointPath`, `testDisabledEndpointIsAbsent` |
| UP/DEGRADED HTTP 200; DOWN HTTP 503; custom details and duration | `testAggregateStatusCustomDetailsAndDuration` |
| Throwing/failing indicator; DOWN dominates UNKNOWN | `testThrowingIndicatorAndDownAggregationReleaseWorkers` |
| Parallel checks within one request | `testIndicatorsWithinOneRequestRunConcurrently` — **fails** |
| Concurrent HTTP requests | `testConcurrentHttpRequestsReleaseTheirWorkers` |
| Timeout, interruption, worker and request-context cleanup | `testTimeoutCancelsIndicatorAndCleansUpRequest`; all tracked requests also check worker termination and request context |
| Healthy and unreachable owned database | `testOwnedDatabaseConnectivityAndUnavailableService` |
| Heap thresholds and invalid configuration | `testMemoryThresholdsAndInvalidConfiguration` |
| Disk UP/DEGRADED/DOWN thresholds and invalid/missing paths | `testDiskThresholdsAndInvalidPaths` |
| Missing/nonexistent/non-RDBMS backend configuration | `testInvalidDatabaseConfiguration` |

## Remaining product defect

At base `62f382dbd`, `HealthCheckExecutor.execute()` loops over indicators and
calls `executeWithTimeout()` for each. That method submits one future and waits
for it before the next indicator is submitted. Thus indicators within a single
request run serially despite the documented concurrent-check requirement.

The regression gives two indicators a bounded rendezvous: both must enter before
either can succeed. Current execution returns UNKNOWN for the first and UP for
the second, instead of two UP checks and an overall UP response. This is a
required, enabled regression, not a skipped or expected-failure test.
`health.endpoint` remains pending, with the failing method mapped in the ledger.
`health.indicators` is verified only for its listed passing source cases.

A separate runtime fix must submit checks concurrently while retaining bounded
timeouts, interruption, shutdown, response aggregation, and request cleanup.
That change is outside this acceptance slice and needs its own review. Current
UNKNOWN results return HTTP 200; the tests preserve that existing contract.
QQQ's HTTP serializer emits the timestamp as numeric epoch seconds, rather than
the ISO string shown in the health module README example. The cleanup tests use
cooperatively interruptible indicators; they do not claim Java can forcibly
terminate arbitrary uninterruptible custom code.

## Reproduce

Run from the repository root with Java 21 and Maven. Use a new disposable cache,
never another agent's cache:

```sh
health_maven_repo=$(mktemp -d /tmp/qqq-599-maven.XXXXXX)
mvn -B -nsu -Dmaven.repo.local="$health_maven_repo" \
  -Drevision=0.0.0-health-acceptance -DskipTests \
  -Dspotbugs.skip=true -Dpmd.skip=true \
  -pl qqq-bom,qqq-middleware-health,qqq-middleware-api,qqq-middleware-picocli,qqq-backend-module-filesystem,qqq-backend-module-mongodb,qqq-backend-module-sqlite,qqq-backend-module-postgres \
  -am install
mvn -B -nsu -Dmaven.repo.local="$health_maven_repo" \
  -Drevision=0.0.0-health-acceptance -Dqqq.version=0.0.0-health-acceptance \
  -f qqq-sample-project/pom.xml -Dtest=SampleHealthAcceptanceTest test
mvn -B -nsu -Dmaven.repo.local="$health_maven_repo" \
  -Drevision=0.0.0-health-acceptance -pl qqq-middleware-health verify
mvn -B -nsu -Dmaven.repo.local="$health_maven_repo" \
  -Drevision=0.0.0-health-acceptance -Dqqq.version=0.0.0-health-acceptance \
  -f qqq-sample-project/pom.xml verify
python3 qqq-sample-project/test_feature_coverage.py
python3 qqq-sample-project/verify-feature-coverage.py --stage source --report-only
```

The focused run and full sample `verify` currently exit nonzero on the required
concurrency regression. On 2026-09-26, using the unique cache
`/private/tmp/qqq-599-maven.ctF7WM`, the focused run had 12 tests, 1 failure,
0 errors, 0 skips. Full sample verification had 688 tests, the same single
failure, 0 errors, 0 skips, and stopped before packaging and later verify goals.
Health module `verify` passed 33 tests and its existing quality gates; its
report-only PMD warnings remain. Sample Checkstyle and compilation passed.
The source coverage report stays `stage_passed=false` and `complete=false`;
these two rows do not certify the rest of the release inventory or browser tests.
