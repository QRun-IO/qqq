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
concurrently. When all eight are leased, the caller waits interruptibly on the
provider monitor, releasing that monitor so lease returns, cleanup, connection
loss and shutdown can progress. A successful return or completed discard wakes
waiters. Every wake rechecks the original connection generation; loss or reset
fails that waiting call rather than borrowing from a replacement generation.
Interruption restores the thread flag and reports a failed publication.

Healthy saturation does not fail or drop a publication. There is no capacity
wait timeout, extra waiting queue, executor, retry, or new configuration surface.
The waiting caller retains its batch: this applies synchronous backpressure and
can delay completion while existing publications hold leases. It does not bound
upstream caller concurrency or promise a deadline if a broker operation hangs.
Existing broker-client connection settings are unchanged; the pool adds no new
network-operation timeout. Actual broker failures still follow section 5's
logged/counted failure contract, without failing the record write or process.

If session configuration fails and closing that untracked session also fails,
the connection is retired. The original configuration exception remains the
cause, with the cleanup exception suppressed on it; repeated attempts cannot
accumulate unaccounted sessions on that connection.

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
| Exclusive leases and bounded per-provider capacity | `EsbPublishingLeaseTest.concurrentLeasesAreExclusiveAndCapacityIsBounded`: prewarmed pool, eight concurrent held leases, ninth waiting; separate-provider and consumer controls. Embedded Artemis and both container brokers prove the ninth public call stays pending, then delivers its exact persistent event after release, with zero capacity failure counters. |
| Transaction isolation after failure | Broker-side send rejection and commit failure: failed batch absent, next message independently received, different broker session. Failed commit retry, abandoned lease, rollback failure and failed close have deterministic tests, including combined configuration/cleanup failure and fresh-connection recovery. |
| Connection generation and shutdown | Idle and borrowed invalidation, successful late return, in-flight commit failure after reconnect, configuration-time loss, native broker restart and manager reset. Waiting borrowers wake on shutdown/loss; interruption restores its flag without consuming capacity, and discard wakes only after cleanup. |
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

Verified on 2026-09-27 after both review corrections: **268 unit tests + 34
integration tests**, zero failures, errors, or skips (22 dedicated cases: fifteen
deterministic, five embedded-broker, two container-broker). The 20 focused
lease/embedded tests also passed. Checkstyle and coverage gates passed: 85/85
classes (100%) and 11,645/12,271 instructions (94.90%). Static analysis completed
under its existing warn-only policy: 23 SpotBugs / 188 PMD findings versus 22 /
181 at the base. The added SpotBugs finding is the intentional borrowed JMS
session getter; its exclusive ownership and lifetime are the lease contract.

Regression sensitivity: the original publisher used twelve broker sessions for
twelve calls; the new test failed before pooling. Failed-commit retry and failed
session cleanup also failed before their guards. Four deliberate mutations each
caused an assertion failure: shared checkout, excess capacity, stale-generation
return, and failed-session reuse. All mutations were restored before the parent
gates. For this correction, the public ninth-publisher test failed against immediate rejection and the combined
configuration/cleanup regression failed before connection retirement was added.

One earlier full run timed out in the unchanged Artemis
`reconnectsAfterBrokerRestart` test: after broker restart, awaiting `RUNNING` at
shared conformance line 383, before its first publication or pool checkout. The
unchanged single-case diagnostic and instrumented full sequence subsequently passed. The
original timeout's cause remains unproved; this change does not claim to fix it.
No timeout, assertion, or gate was weakened. Retained local evidence is under
`/private/tmp/qqq-850-evidence/`: `final-module-and-brokers.log` and
`final-full-failed/` preserve that failure; `full-diagnostic/maven.log`,
`final-green/`, and `verification.json` hold the successful **parent** gate.
The corrected gate is separately archived in `backpressure-full/`: `maven.log`,
`result.json`, `verification.json`, XML reports and coverage, plus per-capture UTC
metadata, owned-container logs/state and JVM thread snapshots. It finished at
2026-09-27 09:46:49 UTC. Neither later passing run establishes the original
failure's cause or attributes it to the environment or this change. This is #850
evidence, not a declaration of complete ESB release readiness.
