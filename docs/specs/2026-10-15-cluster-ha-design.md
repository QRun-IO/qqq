# QQQ on N nodes: cluster, HA, DR and scaling

Status: P1 in progress (2026-10-15). Owner: James Maes. Test bed: kof22 website.

## Goal

A QQQ application runs as one unit on N nodes. For example, 3 web nodes serve
requests and 3 processing nodes run jobs, the scheduler and ESB triggers. Any
node can serve any user, and a node can be added, drained or lost without
losing a user's work. Core stays k8s-agnostic. Kubernetes awareness lives in
the new qbit `qbit-cluster`.

## What breaks today (audit verified against develop 8919c7313)

| # | Problem | Where |
|---|---|---|
| 1 | Process and async job state are hard-coded to `InMemoryStateProvider`. A step, status poll or cancel on another node returns "Could not find status" or permission denied. kof22 uses a sticky cookie to work around it. | `RunProcessAction.getStateProvider`, `AsyncJobManager.getStateProvider`; `ProcessState.stateAccess` and `AsyncJobStatus.stateAccess` are `transient`; `ProcessState` holds `Optional` fields, which are not `Serializable`; `AsyncJobManager.cancelJob` mutates the object in place and never stores it; `OneTimeSecretHandoff` is a static map. |
| 2 | The realtime bus defaults to in-memory, so SSE events reach only the node that produced them. | `RealtimeBus.get` (already pluggable via `RealtimeMetaData.bus`; `EsbRealtimeBus` exists in qqq-esb). |
| 3 | There are no node roles. Every node runs HTTP, Quartz, ESB triggers and durable-job workers. | `QApplicationJavalinServer.start`, the app `Server`; `QQQ_SCHEDULE_MANAGER_ENABLED` exists but nothing else does. |
| 4 | There is no cluster invalidation channel: logout clears only the local token memo, and static memos are per node. | OAuth2 module memo, `RuntimeMetaDataOverlay`, ProcessLockUtils, SlaChecker, QuickSearchRuntime, object-storage ConfigSync. |
| 5 | Artemis runs as a single replica, and ESB publish is fire-and-forget ("Sending never fails the write"). | `EsbRecordChangeListener`, cluster-gitops `artemis-statefulset.yaml`. |
| 6 | Durable-job workers cannot rebuild a user session under OAuth2, so kof22 disables background launches. | `JobSessions.forJob` and `createAutomatedSessionForUser`. Needs **owner sign-off** (P3). |
| 7 | Shutdown is not graceful: there is no readiness flip and no HTTP drain, and the jobs drain (25 s) runs close to the 30 s grace period. | `QApplicationJavalinServer.stop` is not hooked to SIGTERM. |
| 8 | Rate limiters are per pod. | QbitAlertsSink, the assistant ProviderService, DiagnosticsRateLimiter, StorageBucket. |
| + | Quartz jobs do not request recovery, so a job running on a node that dies is not re-fired. | the Quartz scheduler setup |

## Design

### A. Node roles (core, `com.kingsrook.qqq.backend.core.cluster.QNodeRoles`)

Roles: `web`, `worker`, `scheduler`, `esb`. They are read once at startup from
`QQQ_NODE_ROLES` (system property `qqq.node.roles`), a comma list or `all`. The
default is `all`, so a single-node app behaves exactly as today. Explicit
per-flag variables override a role:

| Flag | Env / property | Effect when off |
|---|---|---|
| web | `QQQ_HTTP_ENABLED` / `qqq.http.enabled` | Javalin still starts, because probes and `/metrics` need it. Every other route answers 503 `node does not serve requests`, so a misrouted Service is obvious. |
| scheduler | `QQQ_SCHEDULE_MANAGER_ENABLED` (existing) | Quartz is initialized but never started (standby). The health check reports UP "standby". |
| esb | `QQQ_ESB_TRIGGERS_ENABLED` / `qqq.esb.triggers.enabled` | `QEsbRuntime.start` is a no-op, so no trigger consumers run. Publishing still works on every node. The health check reports UP "standby". |
| worker | `QQQ_JOBS_WORKER_THREADS` / `qqq.jobs.workerThreads` | Durable-job worker threads drop to 0 (API-only: launch, track, reap). An explicit number wins. |

`QNodeRoles.nodeId()` comes from `QQQ_NODE_ID`, else `HOSTNAME`, else the local
host name. It is used for job ownership and the registry.

### B. Shared process and async state (core)

* `StateProviderMetaData`, an instance supplemental, names the provider:
  `withProviderCode(QCodeReference)` or `withTableName("qqq_state")` for the built-in
  `TableStateProvider`. Without it, behavior is unchanged (in-memory).
  `QStateProviders.forInstance()` replaces both hard-coded `getStateProvider()` methods.
* **`TableStateProvider`** works on any QQQ backend, using QQQ actions run in a
  system scope. Table `qqq_state`:

  | column | type | notes |
  |---|---|---|
  | `state_key` | varchar(100) PK | `PROCESS_STATUS:<uuid>`, `ASYNC_JOB_STATUS:<uuid>`, `ONE_TIME_SECRETS:<uuid>` |
  | `state_type` | varchar(40) | |
  | `payload` | bytea | Java serialization, gzip, then AES-GCM when a key is configured |
  | `node_id` | varchar(250) | the writer |
  | `created_at`, `updated_at`, `expires_at` | timestamp | TTL, 12 h by default |

  The table has no FKs and no WORM table. The DDL ships in the host app's
  migrations (kof22 Liquibase), runnable by the app owner role.
* **Serialization:** `ProcessState` gets `writeObject`/`readObject` for its
  `Optional`s, and `stateAccess` becomes serialized. `ProcessMetaDataAdjustment`
  becomes `Serializable`. `AsyncJobStatus` serializes `stateAccess`, and a caught
  exception that cannot be serialized is replaced by a `QException` carrying its
  message (user-facing exceptions are kept). Reads use an `ObjectInputFilter`
  allow-list: `java.*`, `com.kingsrook.*`, plus prefixes the app adds, with depth
  and size limits.
* **Encryption:** with `QQQ_STATE_ENCRYPTION_KEY` (base64 AES-256), payloads are
  encrypted. Without the key, one-time secrets stay on the producing node
  (in-memory, today's behavior) and are never written to the database. With
  the key, they go through the shared store, so any node can hand them off.
* **Ownership across nodes:** `ProcessStateAccess` keeps its session hash and
  backend variants. The same-JVM `QInstance` identity check applies only when
  the access was captured in this JVM, and a deserialized access is checked by
  session hash, variants, process name and UUID, and current permissions. This
  is no weaker than today: the session check is the same.
* **Write volume:** `AsyncJobCallback` writes progress at most once a second per job
  (configurable). State changes (start, complete, error) are written
  immediately. Each callback write first re-reads the stored `cancelRequested`,
  so a cancel from another node is honored. `cancelJob` now stores the flag.
* **Lost nodes:** each running async job records its `nodeId`. A heartbeat
  re-writes running jobs every 10 s, and that write also flushes throttled
  progress. A reader that sees RUNNING with a heartbeat older than 60 s reports
  ERROR: "The server running this step stopped before it finished. Please run
  it again." Without this, the UI would spin forever.
* **Cleanup:** every node runs a sweeper (every 15 min, idempotent) that deletes
  rows past `expires_at`, and reads ignore expired rows.
* **Test mode:** `qqq.state.roundTrip=true` makes the in-memory provider serialize
  on put and deserialize on get. Core's and the app's test suites run with it,
  to catch non-serializable state and code that relies on mutating a stored
  object by reference.

### C. Cross-node realtime (P1 via app config)

Apps with qqq-esb set `RealtimeMetaData.bus = EsbRealtimeBus`, an Artemis topic
with NON_PERSISTENT, 120 s TTL hints, which every node subscribes to. The Jobs
tray, notifications, presence, record events and logout fan out to every node.
A broker-free `PostgresNotifyRealtimeBus` (LISTEN/NOTIFY) is planned for apps
without ESB (P2).

### D. Graceful drain (core)

`QNodeLifecycle` holds a `draining` flag. On SIGTERM, a hook registered by
`QApplicationJavalinServer` runs:

1. `draining = true`. `/ready` returns 503 (`"draining"`), so the endpoints
   controller removes the pod. The k8s `preStop` sleep (5 s) covers propagation.
2. It waits `QQQ_DRAIN_DELAY_SECONDS` (default 5) for in-flight load balancer
   routing to settle.
3. It stops Jetty gracefully: no new connections, and in-flight requests finish
   (Jetty `stopTimeout`, default 20 s).
4. It drains `AsyncJobManager` (in-flight inline steps finish, up to the remaining
   budget) and `DurableJobsRuntime.stop` (leases released, so another worker
   picks them up). The ESB runtime and scheduler stop through their existing hooks.

For kof22, `terminationGracePeriodSeconds` goes to 60. Budget: preStop 5 s, drain
5 s, HTTP 20 s, jobs 25 s.

### E. qbit-cluster (new public repo QRun-IO/qbit-cluster, Apache-2.0)

Tables (prefix `cluster_`, standard QQQ tables under Admin → Cluster):

* `cluster_node` has one row per running node: node id, pod, k8s node, zone (from
  the Downward API env `QQQ_POD_NAME`, `QQQ_K8S_NODE_NAME`, `QQQ_ZONE`), deployment
  (`QQQ_DEPLOYMENT`), roles, version, started at, heartbeat at, status (UP, DRAINING,
  LOST, STOPPED), worker threads, scheduler and ESB active, JVM memory, and
  requests in flight.
* `cluster_lease` holds lease-based leadership for non-Quartz singletons:
  name, holder, expires at, version. It uses compare-and-set updates.
* `cluster_setting` holds defaults: heartbeat 15 s, lost after 60 s, keep
  stopped nodes for 7 days. Seeded.

Runtime: heartbeat writer, lost-node detection (marks LOST, raises an alert via
qbit-alerts through the core alerts SPI when present), and a core health check
("cluster": this node's heartbeat is current). On SIGTERM it marks the node
DRAINING and then STOPPED. Record actions: **Drain node** (sets a drain request
that the node polls; the node flips readiness and stops claiming work), and
in P3 **Scale deployment**. The Admin home widget shows nodes by role. Help
text is included.

P3: an optional k8s client (ServiceAccount with get/patch on `deployments/scale`
in its own namespace only) reads replicas and readiness, and scales web or
worker Deployments, or exports metrics for HPA/KEDA (queue depth, in-flight).

### F. kof22 target shape (cluster-gitops)

* `website-backend` (web): `QQQ_NODE_ROLES=web`, 2 replicas (3 supported). It
  keeps the migrations init containers. Service `website-backend` selects
  `role: web`.
* `website-backend-worker`: `QQQ_NODE_ROLES=worker,scheduler,esb`, 2 replicas,
  the same image (the kustomize `images:` override covers both), the run-migrations
  init container (Liquibase lock, idempotent), and no Service traffic.
* PDBs (`minAvailable: 1` each), anti-affinity by hostname, preStop sleep 5 s,
  grace 60 s.
* Sticky affinity is removed from `ingressroute-admin.yaml` once B and C are live.
* Postgres connections: max_connections is 200. Each pod's pool size is
  multiplied by 4 pods, plus Quartz and Liquibase. The current pool and the new
  totals are checked before scaling, and the pool is lowered for workers if needed.

## Phases

* **P1 (this release):** A, B, C (ESB bus config), D, the qbit-cluster registry,
  roles, lease, drain and Admin UI, the kof22 split, and removal of sticky affinity.
  Quartz `requestsRecovery` (a job on a dead node re-fires on another node).
* **P2:** 4 (ClusterBus invalidation SPI over the realtime bus: token memo,
  overlay, static memos), 5 (transactional ESB outbox table with a relay worker),
  8 (DB-backed shared rate-limiter SPI), and the Postgres NOTIFY realtime bus.
* **P3:** 6 (worker-side session reconstruction; the design goes to the owner
  first, see below), Artemis primary/backup replication, and k8s scaling in
  qbit-cluster. MinIO and OpenSearch single replicas are noted for P3+.

## Item 6: design direction (for owner sign-off, not implemented)

A worker must run a job as the launching user, with no more access than at
launch and with **current** permissions, without a tokenless super-session.
Proposal:

1. At launch, the web node stores a **job credential** in the job row: the user id,
   the captured security-key scope (it does this today), and a server-signed
   (HMAC, `QQQ_JOB_CREDENTIAL_KEY`) grant binding job id, user id, scope hash and
   an expiry.
2. On claim, the worker verifies the signature and expiry. It rebuilds the user's
   **current** permissions through the auth module (for OAuth2, the refresh token
   the platform already stores encrypted per session (qqq#1041/#1048), or an
   Authentik admin lookup of group membership). It intersects permissions and
   keys with the captured scope, as `JobSessions.intersect` does today.
3. If current permissions cannot be established (the refresh token is revoked or the user
   is disabled), the job fails with "owner's access could not be confirmed".
   Nothing escalates.

No change to `QAutomatedSession` authorization happens until the owner approves.

## Verification on kof22 (v1 acceptance)

1. Kill a web pod in the middle of a process wizard: the next step runs on the other
   web pod with its state intact.
2. Kill a worker in the middle of a Quartz job (`requestsRecovery`): the other worker
   re-runs it. Note: user durable jobs stay disabled until P3 (item 6).
3. A step that ran on a killed pod shows the "server stopped" error within about 60 s
   instead of hanging.
4. Jobs tray and notification events produced on a worker reach a user connected to
   a web pod.
5. Admin → Cluster shows 4 nodes with the right roles and heartbeats.
6. A rollout under load (`hey`, or a k6 loop against admin and API) shows zero failed
   requests.
