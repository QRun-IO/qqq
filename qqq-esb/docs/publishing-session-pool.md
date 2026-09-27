# Publishing sessions (#850)

Implements the pooling requirement in approved ESB design section 5 and
[issue #850](https://github.com/QRun-IO/qqq/issues/850).

Each provider lazily owns at most eight publishing sessions. A publication leases
one transacted JMS session exclusively for its whole batch, closes its producer,
and commits once. Only a successful commit makes the session reusable. A failed
or abandoned lease rolls back and closes the session; it cannot contaminate a
later publication. If closing a discarded session itself fails, the manager
retires that connection so capacity cannot grow around an unclosed resource.
Messages remain persistent.

Eight slots bound broker resources while allowing independent callers to publish
concurrently. Exhaustion fails immediately through the existing unsuccessful
`EsbPublishOutput` and failure counters. There is no unbounded waiting queue,
retry, or new configuration surface. Existing broker-client connection settings are unchanged; the pool adds no
new network-operation timeout.

The pool belongs to its provider connection generation. Connection loss removes
idle and borrowed sessions from reuse; the failed connection is closed by the
existing reconnect worker. A late return from the old generation is discarded.
`closeAll()` invalidates the pool and closes the connection, including borrowed
sessions. This is a reset, not a permanent shutdown: runtime owners must stop
workers before calling it so they cannot create fresh providers afterward. A
subsequent publication uses a new provider owner. Consumer callers of
`openSession` still receive independent sessions that they must close themselves.

## Evidence and reproduction

| Requirement | Dedicated evidence |
| --- | --- |
| Reuse across public calls, persistent delivery, producer cleanup | `EsbPublishingPoolTest.repeatedPublicationsReuseOneSession`: 12 calls, one broker-observed session; exact message IDs; zero retained producers. |
| Exclusive leases and bounded per-provider capacity | `EsbPublishingLeaseTest.concurrentLeasesAreExclusiveAndCapacityIsBounded`: prewarmed pool, eight concurrent held leases, ninth rejected; separate-provider and consumer controls. Public exhaustion output and counters have a native test. |
| Transaction isolation after failure | Broker-side send rejection and commit failure: failed batch absent, next message independently received, different broker session. Failed commit retry, abandoned lease, rollback failure and failed close have deterministic tests. |
| Connection generation and shutdown | Idle and borrowed invalidation, successful late return, in-flight commit failure after reconnect, configuration-time loss, native broker restart and manager reset. |
| Both supported brokers | `PublishingPoolIT`: Artemis 2.57.0 and RabbitMQ 4; repeated public calls, malformed second event rolls back first, eight concurrent pending transactions invisible before commit, exact eight payloads after commit. |

The tests use owned fake JMS objects, embedded Artemis on a random port, and owned
Docker containers. They require no external broker credentials. Consumer
`openSession` behavior is also covered by the unchanged connection-manager and
broker conformance suites. This is not a throughput benchmark or evidence that
all other ESB release requirements are complete.

From the repository root, with a separate local repository containing dependencies
built from this checkout:

```sh
mvn -B -Dmaven.repo.local=/private/tmp/qqq-850-evidence/m2 \
  -pl qqq-esb -am -DskipTests install
mvn -B -Dmaven.repo.local=/private/tmp/qqq-850-evidence/m2 \
  -pl qqq-esb -Desb.conformance.skip=false clean verify
```

The first command prepares source artifacts; it is not test evidence. The second
runs all module unit tests, both shared broker conformance suites, the dedicated
publishing-pool broker cases, Checkstyle, coverage, and static analysis. Docker is
required. No dependency, image version, coverage threshold, or shared conformance
fixture is changed by #850.

Verified on 2026-09-27: **263 unit tests + 34 integration tests**, zero failures,
errors, or skips (17 new cases: ten deterministic, five embedded-broker, two
container-broker). Checkstyle and coverage gates passed: 85/85 classes (100%) and
11,605/12,231 instructions (94.88%). Static-analysis tasks completed under their
existing warn-only policy: 23 SpotBugs / 186 PMD findings versus 22 / 181 at the
base. The extra SpotBugs finding is the intentional borrowed JMS session getter;
its exclusive ownership and lifetime are the lease contract.

Regression sensitivity: the original publisher used twelve broker sessions for
twelve calls; the new test failed before pooling. Failed-commit retry and failed
session cleanup also failed before their guards. Four deliberate mutations each
caused an assertion failure: shared checkout, excess capacity, stale-generation
return, and failed-session reuse. All mutations were restored before final gates.

One earlier full run timed out in the unchanged Artemis
`reconnectsAfterBrokerRestart` test, before its first publication. The unchanged
single-case diagnostic and instrumented full sequence subsequently passed. The
original timeout's cause remains unproved; this change does not claim to fix it.
No timeout, assertion, or gate was weakened. Retained local evidence is under
`/private/tmp/qqq-850-evidence/`: `final-module-and-brokers.log` and
`final-full-failed/` preserve that failure; `full-diagnostic/maven.log`,
`final-green/`, and `verification.json` hold the successful final gate. This is
#850 evidence, not a declaration of complete ESB release readiness.
