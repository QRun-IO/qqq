# Deployment modes

A QQQ application runs as one node (the default) or as several nodes that act
as one application (HA). Every cluster feature is opt-in: with nothing set, a
node behaves exactly as QQQ did before cluster support. The design and its
reasoning are in [the cluster/HA spec](specs/2026-10-15-cluster-ha-design.md).

CI proves both modes on every build (`sample_multinode_acceptance`):

* single node: `SampleSingleNodeDefaultsTest` in `qqq-sample-project`;
* several nodes: `SampleMultiNodeIT`, separate sample JVMs on one shared
  database and broker.

## 1. Single node (default)

**Set nothing.** `java -jar`, docker-compose, the quickstart and H2-backed
tests all run this way.

What you get:

| Area | Default behaviour |
|---|---|
| Roles | `QQQ_NODE_ROLES` unset means `all`: the node serves HTTP, runs the scheduler, runs ESB triggers (when the app uses qqq-esb) and runs durable-job workers. |
| Durable jobs | Workers use the instance's `DurableJobsMetaData.workerThreads`. |
| Process and async state | In memory (`InMemoryStateProvider`). No `qqq_state` table and no `QQQ_STATE_ENCRYPTION_KEY`. Wizards, status polls, cancels and one-time secrets work within the one JVM. |
| Cluster bus | In process (`InMemoryClusterBus`). Cache invalidations and session ends apply to this node. |
| ESB | Optional. Without `EsbInstanceMetaData`, nothing is published and no relay runs. The outbox and dedupe are off. |
| Probes | With `qqq-middleware-health`, `/health`, `/ready` and `/live` answer 200 with one replica. `/ready` answers 503 while the node drains. Under sustained memory pressure `/ready` only reports DEGRADED (still 200, so one replica never leaves its load balancer); `/live` fails after 2 minutes so the JVM is restarted. |
| Shutdown | On SIGTERM (Ctrl+C), the node flips `/ready` to 503, waits for running async steps (up to `QQQ_ASYNC_DRAIN_SECONDS`, 20) and in-flight requests (up to `QQQ_HTTP_DRAIN_SECONDS`, 20), then stops. `QQQ_DRAIN_DELAY_SECONDS` defaults to 0, so an idle node stops at once. |
| Database | No connection pooler required. A plain RDBMS backend opens a connection per operation; a server should use the C3P0 pool (see below). |

Nothing grows that did not grow before. The state sweeper, the async
heartbeat, the cancel markers and the ESB outbox relay only start when their
feature is configured.

## 2. Single node with ESB

Add `qqq-esb` and one provider with a single broker url. Nothing else changes:
the node keeps every role, so its triggers run.

```java
EsbInstanceMetaData.of(qInstance)
   .withInstanceName("my-app")
   .withProvider(new QEsbProviderMetaData().withName("broker")
      .withType(EsbProviderType.ACTIVEMQ_ARTEMIS)
      .withUrl("${env.ESB_URL}"))                  // tcp://broker:61616
   .withDestination(new QEsbDestinationMetaData().withName("orderEvents")
      .withType(EsbDestinationType.TOPIC).withProviderName("broker"));
```

When the broker is down, a direct publish is logged and counted, and the table
write still succeeds. `EsbConnectionManager` reconnects with backoff (1 s, up
to 30 s) and triggers re-subscribe.

The transactional outbox (`EsbOutboxConfig`) and dedupe (`EsbDedupeConfig`)
are optional on one node; see [ESB](esb/index.adoc). If you enable the outbox:

* While the broker is unreachable, events wait as `PENDING` rows. The outbox
  grows by the rows written during the outage, and nothing is dropped. The
  `core.esb.outbox` health check turns DEGRADED when the oldest `PENDING` row
  is older than `pendingAlertMs` (5 min). The rows drain when the broker is
  back.
* A row whose destination is no longer configured (renamed or removed between
  deploys) counts attempts and goes `DEAD` after `maxAttempts` (10). It does
  not block the outbox forever. `DEAD` rows are kept, and the health check is
  DOWN while any exist.
* `SENT` rows are purged after `retentionDays` (7).
* An outbox cannot be enabled without a provider: instance validation
  requires every destination to name a configured provider.

## 3. HA / multi-node

Run the same image as two deployments: web pods behind the Service, and worker
pods with no Service traffic. Every node reads the same database. With ESB, the
nodes also share the broker.

### Roles

| Deployment | Environment |
|---|---|
| web pods | `QQQ_NODE_ROLES=web` |
| worker pods | `QQQ_NODE_ROLES=worker,scheduler,esb` |
| every pod | `QQQ_NODE_ID` from the pod name (Downward API `metadata.name`); it falls back to `HOSTNAME`. |

A node without the web role still starts Javalin for probes and `/metrics`, and
answers 503 `This node does not serve requests (no web role).` on every other
route. A node without `scheduler` keeps Quartz in standby. A node without `esb`
runs no trigger consumers but still publishes. A node without `worker` runs 0
durable-job worker threads. Per-feature overrides: `QQQ_HTTP_ENABLED`,
`QQQ_SCHEDULE_MANAGER_ENABLED`, `QQQ_ESB_TRIGGERS_ENABLED`,
`QQQ_JOBS_WORKER_THREADS`.

### Shared process and async state

```java
qInstance.addTable(TableStateProvider.defineTable("qqq_state", "rdbms"));
qInstance.withSupplementalMetaData(new StateProviderMetaData().withTableName("qqq_state"));
```

Set `QQQ_STATE_ENCRYPTION_KEY` on every node, with the same value: base64 of 32
random bytes (`openssl rand -base64 32`). Startup fails without it once state
is shared. Create the table in your migrations:

```sql
CREATE TABLE qqq_state
(
   state_key   VARCHAR(100) PRIMARY KEY,
   state_type  VARCHAR(40),
   payload     TEXT,
   node_id     VARCHAR(250),
   create_date TIMESTAMP,
   modify_date TIMESTAMP,
   expires_at  TIMESTAMP
);
CREATE INDEX i_qqq_state_expires_at ON qqq_state (expires_at);
```

Any node can then serve any wizard step, status poll, records page or cancel,
so sticky sessions are optional. One-time secrets hand off once, through the
encrypted store. A step whose node died reads as "The server running this step
stopped before it finished" after `lostAfterSeconds` (60; heartbeat 10 s).

### Cluster bus and realtime over ESB

```java
qInstance.withSupplementalMetaData(new ClusterBusMetaData()
   .withBus(new QCodeReference(EsbClusterBus.class)));           // topic qqq.cluster.control
qInstance.withSupplementalMetaData(new RealtimeMetaData()
   .withBus(new QCodeReference(EsbRealtimeBus.class)));
EsbInstanceMetaData.of(qInstance).withRealtimeDestinationName("qqq.realtime");
```

With one ESB provider, `qqq.cluster.control` is declared for you. For the
realtime destination, see [Realtime](misc/Realtime.adoc). If the bus
class cannot load (for example, with no ESB provider configured), QQQ logs an
error and keeps events on the node. Short cache TTLs still apply.

### Broker HA, outbox and dedupe

* Provider url for an Artemis primary/backup pair or cluster:
  `(tcp://broker-a:61616,tcp://broker-b:61616)?ha=true&reconnectAttempts=-1&retryInterval=1000`.
  A single url still works; see "Single broker or HA" in [ESB](esb/index.adoc).
* Turn on the outbox, so events commit with their records, and dedupe, so a
  redelivery after a failover runs once:

```java
qInstance.addTable(EsbOutboxTable.defineTable("esb_outbox", "rdbms"));
qInstance.addTable(EsbProcessedMessageTable.defineTable("esb_processed_message", "rdbms"));
EsbInstanceMetaData.of(qInstance)
   .withOutbox(new EsbOutboxConfig().withEnabled(true))
   .withDedupe(new EsbDedupeConfig().withEnabled(true));
```

The PostgreSQL DDL for both tables is in [ESB](esb/index.adoc) ("Host DDL").
Run the relay on several nodes only on a backend with `SKIP LOCKED`
(PostgreSQL).

### Job grants

Set `QQQ_JOB_GRANT_KEY` (base64 of 32 bytes, the same on every node) so a
durable job launched from a user sign-in runs on a worker as that user, from
the same sign-in and with current permissions. Without it, jobs carry no grant
and use the automated-session path, which an OAuth2 worker cannot use to
rebuild the user's session (spec item 6). Scheduled and system jobs never need
a grant.

### Drain and Kubernetes

| Setting | Default | HA suggestion |
|---|---|---|
| `QQQ_DRAIN_DELAY_SECONDS` | 0 | 5: time for endpoints to drop the pod after `/ready` turns 503 |
| `QQQ_ASYNC_DRAIN_SECONDS` | 20 | 20 |
| `QQQ_HTTP_DRAIN_SECONDS` | 20 | 20 (Jetty graceful stop) |

Durable-job workers release their leases on stop (up to 20 s), and another
worker continues them. On Kubernetes:

```yaml
spec:
  terminationGracePeriodSeconds: 60      # preStop 5 + delay 5 + async/HTTP 20 + jobs 20, with headroom
  containers:
    - lifecycle:
        preStop:
          exec: { command: ["sleep", "5"] }
      readinessProbe:
        httpGet: { path: /ready, port: 8000 }
      livenessProbe:
        httpGet: { path: /live, port: 8000 }
```

### Memory pressure

`/ready` and `/live` include two core checks on this JVM's own heap
(`JvmMemoryPressure`): pressure is heap still at or above
`QQQ_HEALTH_HEAP_AFTER_GC_PERCENT` (85) after every collection in the last
`QQQ_HEALTH_MEMORY_WINDOW_SECONDS` (30), or GC pauses taking
`QQQ_HEALTH_GC_TIME_PERCENT` (30) of wall time. It ends when heap after GC
drops below `QQQ_HEALTH_HEAP_RECOVER_PERCENT` (75), so a JVM near the line
does not flap.

| Check | Probe | Effect |
|---|---|---|
| `core.jvm.memory_pressure` | `/ready` | DEGRADED while pressured. With `QQQ_HEALTH_MEMORY_READINESS_ACTION=down`, DOWN (503) after `QQQ_HEALTH_MEMORY_READINESS_AFTER_SECONDS` (15): the pod leaves the Service and the other replicas take its traffic. Set it only with two or more replicas. |
| `core.jvm.memory_pressure_sustained` | `/live` and `/ready` (not `/health`, whose shape is kept) | DOWN after `QQQ_HEALTH_MEMORY_LIVENESS_AFTER_SECONDS` (120) of pressure, so the orchestrator restarts a JVM that would otherwise crawl until OutOfMemoryError. |

`/live` runs only checks flagged `includeInLivenessProbe` (a restart can fix
them); a database or provider outage never fails it. Set
`QQQ_HEALTH_MEMORY_PRESSURE_ENABLED=false` to remove both checks.

### Pod restarts on System Health

Set `QQQ_HEALTH_POD_RESTARTS_SELECTOR` to the label selector of the app's pods
(for example `app in (website-backend,website-backend-worker)`), mount the
service account token, and grant it `get`/`list` on pods in its namespace.
The `core.kubernetes.pod_restarts` check (DEPENDENCY, every 60 s) is DEGRADED
when a container restarted in the last `QQQ_HEALTH_POD_RESTARTS_WINDOW_MINUTES`
(60), naming the cause (JVM out of memory is exit 3, OOMKilled), and DOWN in
CrashLoopBackOff; the health runner raises the standard `health.check.*`
alerts.

Add a PodDisruptionBudget (`minAvailable: 1`) per deployment, and spread pods
across hosts with anti-affinity.

### Connection pooling

Every node opens its own pool, so size it against the database's
`max_connections`: pool size times nodes, plus Quartz and migrations. Use
`C3P0PooledConnectionProvider` with `ConnectionPoolSettings`, and optionally
PgBouncer in transaction mode. See
[Connection pooling](../qqq-backend-module-rdbms/README.md#connection-pooling).

## 4. Going back from HA to one node

1. Scale to one deployment and remove `QQQ_NODE_ROLES` (or set it to `all`), so
   the node runs every role again.
2. Optionally remove `StateProviderMetaData`, `ClusterBusMetaData` and the ESB
   realtime bus. The node then uses in-memory state, and
   `QQQ_STATE_ENCRYPTION_KEY` is no longer needed. Wizards and async steps
   in flight during the switch are lost, so users rerun them. You can drop
   `qqq_state` afterwards.
3. Keeping the shared-state configuration on one node also works. It costs a
   database write per state change, and it survives restarts of that node.
4. ESB: a single broker url works with or without the outbox and dedupe.
   Before you disable the outbox, let `PENDING` rows drain.
5. `QQQ_JOB_GRANT_KEY` can stay. A single node with every role runs its own
   jobs.

## Trying it locally

The sample has an opt-in cluster mode (`SAMPLE_CLUSTER_MODE=true`) that adds
`qqq_state`, `StateProviderMetaData`, `EsbClusterBus`, probes and small demo
processes. Point every node at one database with `SAMPLE_JDBC_URL` (for
example an H2 TCP server). Let only one node prime it
(`SAMPLE_CLUSTER_PRIME_DATABASE=false` on the others), and have the others
use the first node's broker (`-Dqqq.sample.esb.embedded=false
-Dqqq.sample.esb.port=<port>`). `SampleMultiNodeIT` shows the full setup.
