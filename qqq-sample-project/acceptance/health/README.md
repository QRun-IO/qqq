# Health source acceptance (#599, #600)

`SampleHealthAcceptanceTest` uses the sample's metadata and server. Its
`healthfixture.SampleHealthMetaDataProducer` is discovered through the real
metadata package scanner and registers the existing health route provider.
The fixture package is outside the normal application's metadata scan. The
health module dependency is test-scoped. The separate executor fix makes
checks concurrent; no authentication behavior is changed.

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
| Parallel checks within one request | `testIndicatorsWithinOneRequestRunConcurrently` |
| Concurrent HTTP requests | `testConcurrentHttpRequestsReleaseTheirWorkers` |
| Timeout, interruption, worker and request-context cleanup | `testTimeoutCancelsIndicatorAndCleansUpRequest`; all tracked requests also check worker termination and request context |
| Healthy and unreachable owned database | `testOwnedDatabaseConnectivityAndUnavailableService` |
| Heap thresholds and invalid configuration | `testMemoryThresholdsAndInvalidConfiguration` |
| Disk UP/DEGRADED/DOWN thresholds and invalid/missing paths | `testDiskThresholdsAndInvalidPaths` |
| Missing/nonexistent/non-RDBMS backend configuration | `testInvalidDatabaseConfiguration` |

## Executor timeout and cleanup semantics

At base `62f382dbd`, checks were submitted and awaited one at a time. The
retained sample rendezvous failed because its first check could not meet the
second. The runtime fix submits every configured check before collecting any
result, using the existing fixed pool of at most ten workers.

Each check gets the configured timeout (default 5,000 ms) measured with a
monotonic clock from submission, **including queue time**. Collecting a result
uses only the remaining budget, so N timed-out checks do not cost N full waits.
Checks whose queue budget expires do not invoke their indicator. A result
completed before its deadline remains valid when collected later; completion
after the deadline is a timeout. Successful duration measures execution time
unless supplied by the indicator; timeout duration remains the configured
budget. Timeout responses retain UNKNOWN, error and timeout details.

Unfinished futures are canceled on exit, including caller interruption; the
caller retains its interrupt flag. Shutdown remains the caller's responsibility
(the HTTP route uses its existing finally block). The focused executor suite
covers twelve successful checks crossing the ten-worker limit, twenty-four
simultaneous/queued timeouts and cancellation, interrupted callers, throwing and
null results, aggregation, and preserved explicit/measured duration. The sample
checks worker termination and request-context cleanup over actual HTTP.

Current UNKNOWN results still return HTTP 200. QQQ's HTTP serializer emits the
timestamp as numeric epoch seconds, rather than the ISO string shown in the
health module README example. Cleanup tests use cooperatively interruptible
indicators; Java cannot forcibly terminate arbitrary uninterruptible custom code.

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
mvn -B -o -Dmaven.repo.local="$health_maven_repo" \
  -Drevision=0.0.0-health-acceptance -pl qqq-middleware-health \
  -Dtest=HealthCheckExecutorConcurrencyTest,HealthCheckExecutorTest test
mvn -B -nsu -Dmaven.repo.local="$health_maven_repo" \
  -Drevision=0.0.0-health-acceptance -pl qqq-middleware-health install
mvn -B -nsu -Dmaven.repo.local="$health_maven_repo" \
  -Drevision=0.0.0-health-acceptance -Dqqq.version=0.0.0-health-acceptance \
  -f qqq-sample-project/pom.xml -Dtest=SampleHealthAcceptanceTest test
mvn -B -nsu -Dmaven.repo.local="$health_maven_repo" \
  -Drevision=0.0.0-health-acceptance -Dqqq.version=0.0.0-health-acceptance \
  -f qqq-sample-project/pom.xml -Pacceptance-tests clean verify
python3 qqq-sample-project/test_feature_coverage.py
python3 qqq-sample-project/verify-feature-coverage.py --stage source --report-only
```

## Validation (2026-09-26)

Runtime fix: signed local commit `27aad627e`, separate from the retained
sample regression commit `8c246ae43`.

All runs used the task-owned cache `/private/tmp/qqq-599-maven.ctF7WM` and exact
source candidate `0.0.0-health-acceptance`. The executor-only TDD command added
`-Dtest=HealthCheckExecutorConcurrencyTest` to module `test`: before the fix,
5 cases had 3 failures (serial rendezvous, 9.76 seconds for twenty-four 400 ms
budgets, and caller interruption). After the fix, the new five plus four existing
executor cases passed. The module `install` lifecycle then ran full `verify`
and installed that same source JAR for the sample: **38 tests, zero failures,
errors or skips**, with Checkstyle and coverage gates passing. Its configured
report-only analysis still reports 8 SpotBugs findings and 47 PMD warnings.

The focused sample command passed **12/12** health tests. The full
`-Pacceptance-tests clean verify` profile passed **688 unit tests + 61 acceptance
tests**, zero failures/errors/skips, with class coverage **38/40 (95.00%)**.
This includes the existing browser, packaged-process and database suites.
The initial default-profile run and three-test packaged diagnostic passed their
tests but failed coverage at 90% and 92%; those incomplete runs omitted the
acceptance profile that supplies the required SPA/sharing coverage. They are
not an outstanding coverage blocker after the full-profile success. No test,
threshold, exclusion, or failure flag was relaxed.

CI also builds Material dashboard pin
`8ec1be7ad0e1836801e33c503f4417e47f332066` using the steps in
[the sample acceptance job](../../../.circleci/config.yml). That exact pin was
built independently in `/private/tmp/qqq-599-material.owVViS`, rebinding only
its POM's QQQ version to `0.0.0-health-acceptance`, with 49 JavaScript and 22 Java
tests passing. Its JAR was installed into the same task-owned Maven cache;
installed health and Material JAR bytes match their local source build outputs.
No existing UI worktree or implementation was edited. The matching sample run
adds `-Dqqq.frontend.material-dashboard.version=0.0.0-health-acceptance` to the
full-profile command above and also passed **688 + 61 tests**, zero failures,
errors or skips, and **95.00%** class coverage.

The ten Python ledger tests pass. Both health rows have all their named source
cases passing; the overall source report remains `stage_passed=false` and
`complete=false` for unrelated gaps. These rows do not certify the rest of the
release inventory or published artifacts. Independent review is required before
pushing this branch.
