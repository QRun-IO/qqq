# Central health monitoring: Design Spec

Status: **Owner decisions recorded 2026-10-14 (all recommendations accepted). Building.** · drafted 2026-10-14 · Author: James Maes (drafted with Claude)
Targets: QQQ 4.1.x core (`qqq-backend-core`, `qqq-middleware-javalin`, `qqq-middleware-health`), one registration PR per qbit, and kof22 (`KofTwentyTwo/Website-Backend`) as the first adopter.

> Owner, 2026-10-14: "there needs to be a way for services inside of the qqq to all monitor and
> report on their health centrally etc, instead of each of them needing to do it one off. like all
> of the assistants and its models and connections, and quick search, and esb and jobs etc."

## Owner decisions (2026-10-14)

**All recommendations accepted (standing approval).** The owner approved every open design on
2026-10-12 ("they are approved. we will adjust as needed once we can view them working"); that
standing approval covers this spec. Every open question below is decided as recommended:

| # | Decision | Section |
|---|---|---|
| H1 | **One SPI in core** (`HealthCheck`), registered through metadata. Qbits register their checks from their producers; core registers its own. No qbit depends on another qbit to be monitored. | §3 |
| H2 | **The existing `qqq-middleware-health` module is kept and absorbed**, not replaced: its `HealthIndicator`s become `HealthCheck`s through an adapter, and `/health` keeps its URL and JSON shape. | §3.6, §7.2 |
| H3 | **Results are standard QQQ tables** (`health_check`, `health_check_result`, `health_component`), visible and editable with full record screens, secured by permissions. History has a retention sweep. | §5 |
| H4 | **One scheduled runner, leased per check per interval slot across pods.** The lease is a unique-key slot claim on `health_check_result`, not a durable job (see §4.2 for why). | §4 |
| H5 | **Transitions raise alerts through core `QAlerts`** (DOWN, DEGRADED, recovered), with flap suppression. They reach the bell, email and Slack through qbit-alerts when installed, and the log otherwise. | §6 |
| H6 | **`/qqq/v1/health` is a spec'd UI API extension** (summary, components, history, run now). `/health` and `/ready` stay plain Kubernetes probe routes outside `/qqq/v1`. Prometheus `/metrics` is optional and off by default. | §7 |
| H7 | **One "System Health" app home** built from generic widgets. It is the single dashboard for health; other apps link to it. One-off health widgets are deleted. | §8 |
| H8 | **Secrets and PHI never enter details.** Details are a typed allow-list map; values pass a redactor; exception messages are reduced to class name plus a redacted message. A test enforces it. | §3.4 |
| H9 | **Migration ports every one-off check to the SPI and deletes the duplicate.** | §9 |

## 0. Summary

Today every QQQ service checks itself in its own way: quick search has a status API, the assistant
probes providers lazily when a turn fails, email has a readiness object, object storage has a
"test connection" action, backups, CDN and webhooks each show their own status card, kof22 has an
Admin → Health area and a Data Health dashboard, and core has a `/health` endpoint whose checks
are configured by hand. Nobody sees all of it in one place, nothing is recorded over time, and a
provider that goes down at 3am is noticed when a user hits it.

This spec adds:

1. **A `HealthCheck` SPI in core.** A check has a name, component, category, kind
   (`LIVENESS`, `READINESS`, `DEPENDENCY`), interval, timeout, severity and a `run()` that returns
   `UP`, `DEGRADED`, `DOWN` or `UNKNOWN` with details and latency.
2. **A registry** (`HealthChecksMetaData`, supplemental instance metadata). Qbits add checks from
   their producers; core adds checks for its own subsystems.
3. **A runner** on every pod that claims each due check for its interval slot (one run per slot
   across all pods), runs it with jitter and a hard timeout, and records the result.
4. **Standard tables**: definitions (enable, interval and thresholds are editable), result history
   with retention, and a per-component rollup.
5. **Alerts on transitions** through `QAlerts`, with flap suppression.
6. **Endpoints**: `/qqq/v1/health/...` for UIs, `/health` and `/ready` for Kubernetes.
7. **A System Health app home** and a "Run check now" record action.

## 1. Inventory: what exists today (2026-10-14)

Read from `origin/develop` of each repo on 2026-10-14.

| Area | What exists | How it runs | Persisted | Exposed |
|---|---|---|---|---|
| Core `/health` | `qqq-middleware-health`: `HealthIndicator` (`getName`, `check(QInstance)`), `HealthCheckExecutor` (concurrent, ≤10, timeout includes queue time), indicators BasicAlive, Database (RDBMS `SELECT 1`), Memory, DiskSpace. App subclasses `HealthMetaDataProducer`. | Per request | No | Raw `GET /health` (unauthenticated, outside `/qqq/v1`); 503 on DOWN. No liveness/readiness split. |
| ESB (`qqq-esb`) | `EsbConnectionManager.isConnected(provider)`, `QEsbRuntime.isRunning()`, `EsbBrokerAdapter.getQueueInfo` (depth, consumers), `EsbStats`. | On demand | No | `/qqq/v1/esb/overview` (extension `esb`). No health indicator. |
| Durable jobs (qqq#1178–#1195, open stack) | `DurableJobStore.reapExpired`, stall detection, `jobs_worker`, alert types `jobs.job_failed`, `jobs.job_stalled`, heartbeat `jobs.reaper`. | Runtime per pod | `jobs_*` tables | Standard tables. Not on develop yet. |
| Scheduler | `QScheduleManager` (Quartz or simple). | n/a | Quartz JDBC store | Nothing generic. kof22 has a "Scheduled Jobs" widget and Data Health rules on Quartz triggers. |
| Session store | `QSessionStoreProviderInterface` (no ping); providers in `qbit-session-store`. | n/a | n/a | Nothing. |
| Alerts SPI (core) | `QAlerts.raise/resolve/heartbeat/notify`, `QAlertTypeMetaData`, `AlertSinkInterface`, `AlertSeverity` INFO/WARNING/ERROR/CRITICAL. qbit-alerts supplies the sink and monitors (`MonitorEvaluator`, HEARTBEAT monitors). | n/a | `alerts_*` (qbit-alerts) | Bell, email, Slack. |
| qbit-quick-search | `QuickSearchStatus` (OK/DEGRADED/DOWN/DISABLED; OpenSearch reachability and colour, nodes, shards, paused, per-index counts and last sync, failed-event backlog, backoff) from `StatusDiagnostics`, cached 5s. | Lazy, rate-limited | No (reads persisted index rows) | Extension `GET /qbit/quick-search/status` (#51, #54); diagnostics widget; developer panel. |
| qbit-assistant | `AssistantProviderType.checkHealth` (OpenAI-compatible family only: models list; LOCAL for Ollama, LM Studio, vLLM, llama.cpp). `ProviderHealth` lazy circuit breaker (LOCAL probe ≤ every 60s, DOWN 60s on network errors). `ConnectionTest` (one 32-token completion). | Lazy and on demand | `assistant_provider.healthState/healthCheckedAt/downUntil`, `lastTest*`; usage purpose TEST | Processes `assistantProviderTestConnection`, `assistantModelTestConnection`; home "Provider health" view. |
| qbit-email | `EmailApi.readiness(...)` → `EmailApiReadiness` (config preflight, no I/O). `EmailDispatcher` passive `email_provider.healthStatus` from real sends; `email.provider.unhealthy` via `QAlerts`. | Preflight / passive | `email_provider` | Java API; home "Provider health" card. |
| qbit-object-storage | `CapabilityProbe` (read-only probes, optional write probes under `.qqq-probe/`, auth/TLS/DNS/clock problems). | On demand | `object_storage_connection.lastTested*`, capability tables | Record actions `objectStorageTestConnection`(+`WithWriteTests`). |
| qbit-backups | `BackupsHealthIndicator` (the only qbit on `qqq-middleware-health`; DOWN if a critical plan is LATE/FAILING/MISSING_IN_STORAGE). Scheduled `backupCheckSla`. `DestinationProbe` on demand. Own alert types through an adapter to `QAlerts`. | Scheduled SLA, on-demand probe | `backups_plan.slaStatus` | `/health` indicator; record action `backupTestDestination`. |
| qbit-cdn | `CdnOperations.connectionTest`; scheduled daily `ScheduledDriftStep` (drift plus connection test). `cdnHealthCard` HTML widget for the host's Health page. No `QAlerts`. | Daily and on demand | `cdn_provider.lastHealth*` | Process `cdnTestConnection`; HTML card. |
| qbit-webhooks | `EndpointHealth` passive state machine (HEALTHY → UNHEALTHY → PROBATION → DISABLED); `webhooks.endpoint.unhealthy` via `QAlerts`. | Passive | `webhooks_endpoint.health*` | "Send Test Event"; home summary. |
| qbit-authentik | `AuthentikDriftReport` (7 checks: server version, blueprints, service account and its single API token, break-glass, last admin, mapper attributes, lockout); token-expiry warning; server version fetch. Token broker has no probe. | Lazy per page load | No | `driftReport` widget, Identity home card. No `QAlerts`. |
| Local LLM hosts | Only via the assistant's OpenAI-compatible LOCAL probe. qqq-mcp-rag and qbit-middleware-mcp have none. | Lazy | see assistant | — |
| kof22 (Website-Backend) | `/health` with BasicAlive, Database, Memory, `BuildInfoHealthIndicator`. Admin → **Health** app (`adminHealth`): HTML "System Status" tiles (build, uptime, database, ESB, search, scheduler) from `LiveAdminHomeProbes`, HTML ESB tiles, Data Health table, Scheduled Jobs table, CDN card; an Admin home roll-up card. **Data Health** app: `DataHealthEvaluator` rule sets on qbit-alerts monitors (900s), `dataHealthRunChecks` process. | Per page load; Data Health monitors every 900s | Data Health via `alerts_*` only | Admin → Health, Admin → Health → Data Health. Probes in cluster-gitops: liveness `tcpSocket`, readiness `GET /health`. |

**Conclusions.** Five different status vocabularies (OK/DEGRADED/DOWN/DISABLED, UP/DOWN, HEALTHY/UNHEALTHY/PROBATION, OK/FAILED, CapabilityStatus). Only backups uses the core health SPI. Only quick search has a spec'd API. Most checks are lazy, so nothing notices an outage until a user hits it, and nothing keeps history. Assistant, email, object storage, webhooks and Authentik have no presence on kof22's Health page at all. Core already has the right primitives (`HealthIndicator`, `QAlerts`, the extension point); what is missing is a registry, a scheduler with a cross-pod lease, tables and one home.

## 2. Goals and non-goals

**Goals**
- Any module or qbit adds a check in a few lines, and gets scheduling, history, alerting, the
  dashboard, probes and the API for free.
- One place to see the health of a whole QQQ application, with history.
- Kubernetes probes stay fast and never call external providers.
- Nothing a check records can leak a secret or PHI.

**Non-goals**
- Replacing Prometheus/Alertmanager for infrastructure metrics. The optional `/metrics` export
  feeds them; it does not compete with them.
- Synthetic end-user monitoring (page loads, journeys). The web-performance qbit covers that.
- Deep data-quality rules. Data Health "issues" in kof22 stay where they are (§9.3); only their
  liveness ("is the scanner running") becomes a check.

## 3. Core contract

Package `com.kingsrook.qqq.backend.core.health` in `qqq-backend-core`, so every qbit can register
checks with only its existing core dependency.

### 3.1 `HealthCheck`

```java
public interface HealthCheck
{
   /** stable, unique, lowercase dotted name, e.g. "assistant.provider.openai" */
   String getName();

   /** the run itself.  Must honor input.getDeadline(); must not throw for an expected failure. */
   HealthCheckOutcome run(HealthCheckInput input) throws QException;
}
```

Its descriptive attributes are metadata, not methods, so they can be overridden from the
`health_check` table without code:

```java
new HealthCheckMetaData()
   .withName("assistant.provider.openai")
   .withLabel("OpenAI provider")
   .withComponent("assistant")                // the rollup key and the grid tile
   .withCategory(HealthCategory.AI)           // CORE, DATABASE, MESSAGING, JOBS, SEARCH, AI, EMAIL,
                                              // STORAGE, IDENTITY, CDN, INTEGRATION, OTHER
   .withKind(HealthCheckKind.DEPENDENCY)      // LIVENESS | READINESS | DEPENDENCY
   .withIntervalSeconds(300)
   .withTimeoutMs(10_000)
   .withSeverity(AlertSeverity.WARNING)       // what DOWN means (core AlertSeverity): CRITICAL | ERROR | WARNING | INFO
   .withFailureThreshold(2)                   // consecutive non-UP results before a transition
   .withRecoveryThreshold(1)                  // consecutive UP results before "recovered"
   .withCheck(new QCodeReference(OpenAiProviderHealthCheck.class))   // or .withCheckInstance(lambda)
```

- **Kind.** `LIVENESS` checks are in-process and cheap (the pod can serve at all). `READINESS`
  checks gate traffic (the pod's database is reachable). `DEPENDENCY` checks are everything
  external (providers, OpenSearch, S3, SMTP, Authentik). Only `LIVENESS` and selected
  `READINESS` checks ever run inside a probe (§7.2).
- **Severity** is core `AlertSeverity`: what a `DOWN` means for the application. `DEGRADED`
  alerts are always one level lower.
- **Dynamic sets.** A `HealthCheckSupplier` returns checks computed from data, e.g. one per
  enabled assistant model or object-storage connection. The registry re-resolves suppliers when
  the runner starts and every 5 minutes, so adding a connection record adds its check without a
  restart. A supplier's checks are named `<prefix>.<stable id>`.

### 3.2 `HealthCheckInput` and `HealthCheckOutcome`

- Input: `QInstance`, `Instant deadline`, `boolean onDemand` (Run check now), `Integer attempt`.
  The check runs under the system's automated session (`health_runner` user) with the
  `health_check` security keys only.
- Outcome: `HealthStatus status` (the existing enum, moved to core and re-exported from
  `qqq-middleware-health`), `String message` (one short line), `HealthDetails details`,
  `Long latencyMs` (set by the runner if the check leaves it null), and optional
  `List<HealthMetric>` (name, value, unit) for numeric gauges such as queue lag.
- A check that throws, or passes its deadline, is recorded as `DOWN` with message
  `"check failed: <ExceptionClass>"` or `"timed out after Nms"`. `UNKNOWN` is for "cannot tell"
  (not configured, disabled upstream), never for errors.

### 3.3 Status aggregation

`HealthStatus.aggregate` (existing rules) rolls checks up into a component and components into the
application, but a check's severity caps its effect: an `INFO` check that is `DOWN` makes its
component `DEGRADED`, not `DOWN`, and does not move the application status.

### 3.4 Details, secrets and PHI (H8)

- `HealthDetails` is a small typed map: keys are `[a-zA-Z][a-zA-Z0-9_.]*`, values are strings
  (max 500 chars), numbers, booleans or instants. No nested objects, no records, no free-form
  payloads.
- Every string value and every message passes `HealthRedactor` before it is stored or served:
  - URL userinfo and query strings are stripped (`https://u:p@h/x?token=..` → `https://h/x`);
  - values that look like secrets are masked: bearer/basic credentials, `key=`/`secret=`/
    `password=`/`token=` pairs, AWS key ids, JWTs, PEM blocks, long base64/hex runs (≥ 32);
  - email addresses, phone numbers and digit runs that look like identifiers are masked, so a
    provider error that echoes a recipient or a patient identifier cannot leak.
- Detail **keys** containing `password`, `secret`, `token`, `key`, `credential` or `authorization`
  are rejected (the value is replaced by `"[redacted]"` and a warning is logged once per check).
- Exception messages are never copied verbatim: the runner records the exception class and the
  redacted first line, and logs the full stack trace server-side only.
- **Test:** `HealthDetailsNoSecretsTest` runs every registered check in the core test instance and
  every qbit's test instance with poisoned configuration (credentials set to a known sentinel)
  and fails if the sentinel, any configured secret value, or a PII pattern appears in any stored
  result, API response or alert text. Each qbit registration PR runs the same test helper.

### 3.5 Registry

`HealthChecksMetaData implements QSupplementalInstanceMetaData` (name `healthChecks`):

```java
HealthChecksMetaData.ofOrWithNew(qInstance)
   .withCheck(new HealthCheckMetaData()...)
   .withSupplier(new HealthCheckSupplierMetaData()
      .withNamePrefix("object_storage.connection")
      .withComponent("object_storage")
      .withSupplier(new QCodeReference(ObjectStorageConnectionChecks.class)));
```

QInstance validation fails on: a duplicate name, a name not matching
`[a-z0-9]+([._-][a-z0-9]+)*`, a missing component, interval < 10s, timeout ≥ interval, or a
`LIVENESS` check with timeout > 1000ms.

### 3.6 `qqq-middleware-health` compatibility

- `HealthIndicator` stays. `HealthIndicatorCheck` adapts one into a `HealthCheck`
  (name `core.<indicator name>`, component `core`, kind `READINESS` for `database`, `LIVENESS`
  for `memory`, `alive` and `diskSpace`).
- `HealthMetaDataProducer.buildHealthCheckMetaData` keeps working: its indicators are registered
  in `HealthChecksMetaData` too, so an app that already configured `/health` gets them on the
  dashboard with no change.
- `HealthStatus` moves to `com.kingsrook.qqq.backend.core.health`; the old class stays as a
  deprecated copy with a converter until 4.2 (no binary break in 4.1).

### 3.7 Module placement

| Module | Holds |
|---|---|
| `qqq-backend-core` (`core.health`) | SPI, metadata, registry, validation, `HealthStatus`, redactor, tables, runner, transitions, retention, the Run-check-now process, the System Health app and widgets, and the core checks that need only core (scheduler, session store, durable jobs). |
| `qqq-backend-module-rdbms` (and other backend modules) | `QBackendModuleInterface` gains a default `getHealthCheck(QBackendMetaData)` returning null. RDBMS implements it (`SELECT 1`), and core registers `core.database.<backend>` for every backend that returns one, so core never imports RDBMS classes. |
| `qqq-esb` | Registers `core.esb.*` from its existing metadata producer. |
| `qqq-middleware-javalin` | Starts and stops the runner with `QApplicationJavalinServer`; the `health` UI API extension specs. |
| `qqq-middleware-health` | `/health` (unchanged URL), the new `/ready`, the `HealthIndicator` adapter, optional `/metrics`. |

## 4. Runner

### 4.1 Schedule

`HealthRunner` starts with `QApplicationJavalinServer` (and from `QScheduleManager` for non-HTTP
apps) on every pod whose `HealthMonitoringMetaData.runnerEnabled` is true (default true). Every
`tickSeconds` (default 10) it:

1. lists enabled `health_check` rows whose `next_due_at <= now`;
2. **claims** each due check for its slot (§4.2);
3. runs the claimed checks on a bounded pool (`maxConcurrentChecks`, default 4; never the
   AsyncJobManager pool and never the durable-jobs workers), each with its own hard deadline;
4. records the result, updates the rollup, evaluates transitions (§6);
5. sends `QAlerts.heartbeat("health.runner")` once a minute, so the alerts qbit's dead-man monitor
   raises if no pod is running checks at all.

`next_due_at` is `slot_start + interval + jitter`, with jitter uniformly 0–10% of the interval
(max 30s), so checks with the same interval do not all fire together.

### 4.2 One run per slot across pods (the lease)

The slot is `floor(epochSeconds / interval)`. A pod claims a check by **inserting** its
`health_check_result` row with status `RUNNING`, `slot = <slot>`, `runner_pod = <pod id>` and
`lease_expires_at = now + timeout + 5s`. `health_check_result` has a unique key
`(health_check_id, slot, on_demand_request_id)`, so exactly one insert wins; the others get a
unique-key violation and move on. The winner updates the same row with the outcome.

- A pod that dies mid-check leaves a `RUNNING` row. The next tick on any pod finds `RUNNING` rows
  past `lease_expires_at` and closes them as `UNKNOWN` with message
  `"abandoned by pod <id>"`; they do not count toward transitions. The next slot runs normally.
- "Run check now" inserts with `on_demand_request_id` set, so it never collides with the
  scheduled slot.

**Why not a durable job** (the recommendation said "as a durable job"): the runner must keep
working when the jobs subsystem is what is broken, because "durable jobs lag" and "stuck leases"
are among its own checks. A runner built on durable jobs goes silent exactly when it is most
needed. The slot claim gives the same guarantee (one run per interval across pods, leased,
crash-safe) using only a unique key, which every backend already enforces, and it does not wait
on the open durable-jobs PRs (qqq#1178 onward). The durable-jobs kernel is monitored *by* this
runner (§10.1) instead.

### 4.3 Retention

A daily sweep (part of the runner, slot-claimed the same way) deletes `health_check_result` rows
older than `resultRetentionDays` (default 30), keeping the last transition row per check
regardless of age. Deletes are batched (1,000 rows) so a large table never holds one long lock.

### 4.4 Failure modes

| Situation | Behavior |
|---|---|
| Health tables unreachable (database down) | Runner logs once per minute, keeps the last results in memory for `/ready`, and records nothing. `core.database` is DOWN in `/ready` from the in-memory probe. |
| Check hangs past its timeout | Thread interrupted; result `DOWN` "timed out". A check that ignores interrupts holds one pool slot until it returns; the pool is bounded so it cannot starve the pod. |
| All pods' runners disabled | `health.runner` heartbeat stops; the alerts qbit raises "Health runner not running". |
| Clock skew between pods | Slots use the database clock when the backend exposes one (`ConditionalUpdate` backends, after qqq#1178), else the pod clock; a skew below the interval just shifts a run. |

## 5. Tables (H3)

All three are standard QQQ tables in the application's main backend (`HealthMonitoringMetaData
.backendName`), snake_case columns, `health_` prefix, published as ESB table events and searchable
in Quick Search (definitions only). Permissions use core's standard table permission base name `health`: `health.read` (read all three),
`health.write` (edit definitions, run now). Records carry no security key beyond the permission.

### 5.1 `health_check` (definitions; the main nav table)

| Column | Notes |
|---|---|
| `id`, `name` (unique), `label`, `component`, `category`, `kind`, `severity` | Synced from metadata at startup (insert new, update labels; never delete, set `registered=false` instead). |
| `enabled` | **Editable.** Off stops scheduling; the check still runs on "Run check now". |
| `interval_seconds`, `timeout_ms`, `failure_threshold`, `recovery_threshold` | **Editable overrides**; null means the metadata default (shown as placeholder). |
| `registered` | False when the code no longer registers the check (kept for history). |
| `current_status`, `current_message`, `current_since`, `last_run_at`, `last_latency_ms`, `consecutive_failures`, `consecutive_successes` | Read-only, written by the runner. |
| `next_due_at` | Read-only. |

Record screen: sections Identity, Schedule, Current state, and child lists "Recent results"
(`health_check_result`, newest first) and a small line chart of latency.
Record action: **Run check now** (§8.2).

### 5.2 `health_check_result` (history; child of `health_check`)

`id`, `health_check_id`, `slot`, `on_demand_request_id`, `status` (`RUNNING`, `UP`,
`DEGRADED`, `DOWN`, `UNKNOWN`), `message`, `details_json` (redacted, max 4 KB), `metrics_json`,
`latency_ms`, `started_at`, `finished_at`, `runner_pod`, `lease_expires_at`, `is_transition`,
`triggered_by` (`SCHEDULE`, `ON_DEMAND`, `STARTUP`). Unique key
`(health_check_id, slot, on_demand_request_id)`. Indexed on `(health_check_id, started_at)`.
Read-only in the UI.

### 5.3 `health_component` (rollup; child list on the home)

`id`, `name` (unique), `label`, `status`, `since`, `checks_up`, `checks_degraded`,
`checks_down`, `checks_unknown`, `worst_check_id`, `updated_at`. Recomputed after every result.
Read-only.

## 6. Transitions and alerts (H5)

Per check, the runner keeps `current_status` and consecutive counters:

- A non-UP result increments `consecutive_failures`; the status **changes** only when the counter
  reaches `failure_threshold` (default 2; 1 for `CRITICAL` readiness checks).
- An UP result increments `consecutive_successes`; a non-UP check **recovers** only after
  `recovery_threshold` (default 1) UP results.
- **Flap suppression:** if a check has changed state ≥ 4 times in the last 30 minutes, it is marked
  `flapping` (`health_check.flapping = true`), one alert "flapping" is raised, and individual
  transitions stop alerting until it has been stable for 30 minutes.

Each transition writes `is_transition = true` on the result and calls core `QAlerts`:

| Transition | Call |
|---|---|
| → DOWN | `QAlerts.raise(new AlertSignal("health.check.down").withDedupKey(checkName).withSeverity(fromCheckSeverity))` with title "`<label>` is down", the redacted message, a record ref to the `health_check` row and a link to the System Health home. |
| → DEGRADED | Same with `health.check.degraded`, one severity lower. |
| → UP | `QAlerts.resolve("health.check.down", checkName, "Recovered after <duration>")` and the same for degraded. |
| UNKNOWN | No alert (an unknown check is shown, not paged). |

Alert types `health.check.down`, `health.check.degraded`, `health.check.flapping` and the
heartbeat `health.runner` are registered as `QAlertTypeMetaData` by core, so with qbit-alerts
installed they route to the bell, email and Slack by the user's existing alert preferences;
without it they go to `LoggingAlertSink`.

## 7. Endpoints (H6)

### 7.1 UI API extension `health` (`/qqq/v1/health/...`)

Registered by core as a `MiddlewareSpecExtension` named `health`, semver `1.0.0`, with `/health`
added to `ESTABLISHED_PREFIXES` (core-owned, like `/esb`). All require `health.read`; run-now
requires `health.write`.

| Method, path | Returns |
|---|---|
| `GET /health/summary` | `{status, since, counts{up,degraded,down,unknown}, components:[{name,label,status,since,checksDown,…}], runner{lastBeatAt, pods}}` |
| `GET /health/components/{name}` | The component and its checks with current state. |
| `GET /health/checks/{name}/history?from&to&limit` | Results (redacted), newest first, max 500. |
| `POST /health/checks/{name}/run` | Runs now (same claim path, `on_demand_request_id`), waits up to the check's timeout, returns the outcome. |

Schemas are prefixed `Health…V1`. Next UI needs nothing new for these: the System Health home is
built from generic widgets (§8), which use the standard widget API. The extension exists for
non-web clients (CLI, TUI, iOS) and for the "status card" links on other app homes.

### 7.2 Kubernetes probes (not UI API)

- `GET /health` (existing URL and JSON shape, kept): **liveness**. Runs only `LIVENESS` checks,
  in-process, with a 1s total budget. 200 unless one is DOWN.
- `GET /ready` (new): **readiness**. Runs `LIVENESS` checks plus `READINESS` checks flagged
  `includeInReadiness` (default: core database backends only), each answered from the runner's
  in-memory last result if it is younger than 2× its interval, else run inline with a 2s budget.
  503 when any included check is DOWN. Never runs a `DEPENDENCY` check, so a provider outage can
  never take pods out of service.
- Both are unauthenticated, return only names and statuses (no details, no messages), and are
  served outside `/qqq/v1`.

### 7.3 Prometheus (optional, off by default)

`HealthMonitoringMetaData.prometheusEnabled` adds `GET /metrics` (text exposition) with
`qqq_health_check_status{check,component,kind}` (0 up, 1 degraded, 2 down, 3 unknown),
`qqq_health_check_latency_ms`, and any `HealthMetric` gauges. No details are exported.

## 8. UI (H7)

### 8.1 System Health app

A core app `systemHealth` ("System Health", icon `monitor_heart`), placed by the host (kof22:
Admin → System Health). Its home is the only health dashboard:

1. **Summary** (`MULTI_STATISTICS`): overall status, components down, degraded, checks run in the
   last hour, runner pods alive.
2. **Components** (`TABLE`): one row per `health_component`, status chip, since, worst check, link
   to the component's filtered `health_check` list. Full width, wraps at narrow widths.
3. **Problems** (`TABLE`): checks currently not UP, worst first, with message and since.
4. **Recent transitions** (`TABLE`): last 20 `is_transition` results.
5. **Latency** (`SMALL_LINE_CHART` per component, 24h, from `health_check_result`).

Nav lists only `health_check`; results and components are child lists and widgets. Every widget
reads real data only; an empty install shows "No checks have run yet".

### 8.2 "Run check now"

A standard streamed-ETL record action on `health_check` (single or multiple selected records),
label "Run check now", with the standard Review ("3 checks will run") and Result
("2 UP, 1 DOWN") summary lines. Not in the nav.

### 8.3 Status cards elsewhere

Other app homes (Quick Search, Assistant, CDN, ESB, Jobs, Backups) may show **one** status line
for their component, linking to System Health filtered to it. They must not duplicate history,
charts or check lists.

## 9. Migration (H9)

The rule: **a check reuses the qbit's existing probe code; the one-off scheduling, status vocabulary and status UI go.** Per-entity domain state that real traffic produces (webhook endpoint health, email provider health, the assistant circuit breaker) is not health-check history and stays; the checks read it. Configuration-time diagnostics that take input or write to the target (write probes, "Send Test Event") stay as record actions.

### 9.1 Core

| Today | Becomes | Deleted |
|---|---|---|
| `HealthIndicator`s configured in `HealthMetaDataProducer` | Adapted automatically (§3.6). | Nothing in 4.1; the old `HealthStatus` copy goes in 4.2. |
| ESB: no check | `core.esb.connection.<provider>`, `core.esb.dead_letters` (§10.1). | — |
| Durable jobs signals | `core.jobs.*` checks (§10.1) beside the existing `jobs.*` alerts. Those stay: they are per job, the checks are per system. | — |

### 9.2 Qbits (one PR each)

| Qbit | Check source | Kept | Deleted or changed |
|---|---|---|---|
| quick-search | `StatusDiagnostics.read()` mapped OK→UP, DEGRADED→DEGRADED, DOWN→DOWN, DISABLED→UNKNOWN. | `GET /qbit/quick-search/status` (the index-level detail view the Quick Search home uses). | The home's own status tile becomes the §8.3 one-line card. |
| assistant | `checkHealth` per enabled provider; per enabled model a minimal call through `ConnectionTest` at a long interval (default 6h, counted in usage with purpose `HEALTH` and subject to the cost cap). | Circuit breaker (`ProviderHealth`), Test connection actions, `lastTest*`. | Home "Provider health" view becomes the one-line card plus links. The breaker reads the check's last result before its own lazy LOCAL probe, so local hosts are not probed twice. |
| email | Per provider: passive `healthStatus` plus a transport handshake (SMTP `NOOP` or an API auth ping; never a send). One `email.readiness` check from `EmailApi.readiness` per category. | Passive `healthStatus`, `email.provider.unhealthy`. | "Provider health" card becomes the one-line card. |
| object-storage | Per connection: `CapabilityProbe` in read-only mode. | Test connection actions (write tests stay on demand only). | — |
| backups | The `BackupsHealthIndicator` rule as `backups.sla`; a read-only (head) destination probe per active plan, daily. | `backupCheckSla` (it computes SLA state; the check reads it). | `BackupsHealthIndicator` and the optional `qqq-middleware-health` dependency. |
| cdn | `CdnOperations.connectionTest` per provider. | The daily drift step (drift is configuration, not health) and `cdnTestConnection`. | The connection test inside `ScheduledDriftStep`; the `cdnHealthCard` HTML widget (also a G5 violation). |
| webhooks | One check per enabled endpoint reading `EndpointHealth` (UNHEALTHY or PROBATION → DEGRADED, auto-DISABLED → DOWN), plus `webhooks.dispatcher` (oldest undelivered age). | The state machine, alerts, Send Test Event. | Home health summary becomes the one-line card. |
| authentik | `authentik.api` (reachable, version), `authentik.api_token` (present; expiry under 14 days → DEGRADED), `authentik.mapper` (drift checks 6 and 7), `authentik.token_broker` (client-credentials grant when configured, else UNKNOWN). | The `driftReport` widget (drift is configuration review). | The Identity home "health" card becomes the one-line card. |
| session store | Done in core, not per qbit: `core.session_store` round-trips a probe key (store, load, remove, 60s TTL) through whatever `QSessionStoreProviderInterface` is registered. | — | — |

### 9.3 kof22 (draft PR)

| Today | Becomes |
|---|---|
| Admin → Health home: HTML System Status tiles, HTML ESB tiles, the CDN card, and the Admin home roll-up card | The core System Health home, placed at Admin → Health. The tiles, the build/database/ESB/search/scheduler probes in `LiveAdminHomeProbes`, and the CDN card are deleted. The Admin home roll-up reads `health_component`. |
| `BuildInfoHealthIndicator` | Kept, adapted as `LIVENESS` check `core.buildInfo` (always UP; details are the commit and build time). |
| Data Health app (rule sets on alerts monitors) | **Kept.** It is data quality, not service health, and already runs on qbit-alerts. A `kof22.data_health` check reports DEGRADED while any ERROR rule has an open alert, so it shows on the grid with a link. |
| Scheduled Jobs widget | Kept as an operations record view; `core.scheduler` covers its health. |
| cluster-gitops probes: liveness `tcpSocket`, readiness `GET /health` | Liveness `GET /health`, readiness `GET /ready`, and a `startupProbe` on `/health`. Listed in the kof22 PR for the coordinator; GitOps is not changed by this program's PRs. |
| Liquibase | A dated changeset `changesets/features/2026-10-1x-health.sql` (kof22 moved from numbered files to dated ones after 0041). |

## 10. Checks registered (initial set)

Defaults are shown; every value can be overridden per row in `health_check`.

### 10.1 Core

| Name | Kind | Severity | What |
|---|---|---|---|
| `core.alive`, `core.memory` | LIVENESS | CRITICAL / WARNING | The existing indicators (30s). |
| `core.database.<backend>` | READINESS, in `/ready` | CRITICAL | `SELECT 1` per RDBMS backend (30s, 3s timeout); other backends run a one-row query. |
| `core.scheduler` | READINESS | ERROR | Scheduler started; for Quartz, no ERROR triggers and none overdue by more than 2 intervals (60s). |
| `core.esb.connection.<provider>` | DEPENDENCY | ERROR | `EsbConnectionManager.isConnected` and the ESB runtime running (30s). |
| `core.esb.dead_letters` | DEPENDENCY | WARNING | Dead-letter depth above 0 → DEGRADED (5m), through `EsbBrokerAdapter` when management is enabled. |
| `core.jobs.workers` | DEPENDENCY | ERROR | At least one `jobs_worker` heartbeat newer than 2× TTL while jobs are enabled (60s). |
| `core.jobs.lag` | DEPENDENCY | WARNING | Oldest PENDING chunk older than 5m → DEGRADED, older than 30m → DOWN. |
| `core.jobs.stuck_leases` | DEPENDENCY | WARNING | Chunks whose lease expired and were not reaped within 2 reaper intervals. |
| `core.session_store` | READINESS | ERROR | Round-trip of a probe key through the registered `QSessionStoreProviderInterface` (30s); absent when no provider is registered. |

The `core.jobs.*` checks register only when `DurableJobsMetaData` is present, so P1 does not wait for the durable-jobs PRs; they land with the kernel or right after it merges.

### 10.2 Qbits

| Names | Per | Kind | Interval |
|---|---|---|---|
| `quick_search.opensearch`, `quick_search.index.<name>`, `quick_search.backlog` | cluster, index | DEPENDENCY | 60s |
| `assistant.provider.<id>` | enabled provider | DEPENDENCY | 5m (local hosts 60s) |
| `assistant.model.<id>` | enabled model | DEPENDENCY | 6h |
| `email.provider.<id>`, `email.readiness` | provider | DEPENDENCY | 5m |
| `object_storage.connection.<id>` | connection | DEPENDENCY | 5m |
| `backups.sla`, `backups.destination.<planId>` | plan | DEPENDENCY | 15m / 24h |
| `cdn.provider.<id>` | provider | DEPENDENCY | 15m |
| `webhooks.endpoint.<id>`, `webhooks.dispatcher` | endpoint | DEPENDENCY | 5m |
| `authentik.api`, `authentik.api_token`, `authentik.mapper`, `authentik.token_broker` | — | DEPENDENCY | 5m / 6h |

## 11. Build plan

| Phase | Repo / PR | Contents |
|---|---|---|
| P0 | qqq: this spec | `docs/specs/HEALTH.md`. |
| P1 | qqq: core kernel | SPI, metadata, registry, validation, redactor, tables, runner with slot claims, transitions and `QAlerts`, retention, `qqq-middleware-health` adapter, `/ready`, core checks (database backends, scheduler, session store, ESB, durable jobs when present), the `health` UI API extension, the System Health app and the Run-check-now action. Tests below. |
| P2 | qbit PRs, one per qbit | Registrations in §10, plus deletion of each qbit's one-off check surface where §9 says so. |
| P3 | qqq-frontend-next | Only if a generic widget cannot render the home acceptably (none expected). |
| P4 | Website-Backend (draft) | Adopt: enable the runner, add the System Health app under Admin, remove the Health area's one-off cards (§9.3), Liquibase for the three tables, probes in cluster-gitops point at `/health` and `/ready`. |

## 12. Acceptance tests (P1)

1. **Flip and recover:** a fake check returns DOWN twice → one `health.check.down` alert raised
   (captured by a test `AlertSinkInterface`), `health_check.current_status = DOWN`,
   component DOWN; then UP once → alert resolved, component UP, both transitions recorded.
2. **Flap suppression:** alternating results 5 times in 30 minutes (fake clock) → one flapping
   alert, no further transition alerts.
3. **Leased across 2 pods:** two runners on one H2/Postgres database with the same check due →
   exactly one result row per slot, both pods tick; killing the winner mid-check leaves a
   `RUNNING` row that the other pod closes as UNKNOWN "abandoned" after the lease, and the next
   slot runs on the survivor.
4. **No secrets in details:** sentinel credentials in a check's config and a check that throws an
   exception whose message contains a bearer token, a URL with userinfo, an email and a JWT →
   none appear in `health_check_result`, the API responses, `/health`, `/ready` or alert text.
5. Probes: `/health` runs no `DEPENDENCY` check; `/ready` returns 503 when the database check is
   DOWN and 200 when only a dependency is DOWN.
6. Timeouts: a hanging check is recorded DOWN "timed out" and does not block other checks.
7. Retention: rows past retention are deleted in batches; the last transition per check is kept.
8. Validation: duplicate names, bad names, timeout ≥ interval fail the instance.
9. UI API contract: the `health` extension appears in `openapi.yaml` with `Health…V1` schemas.

## 13. As built (P1)

- **Permissions** are core's standard table permissions with base name `health`
  (`health.read`, `health.write`), not new `view`/`admin` suffixes.
- **Indexes and NOT NULL.** Core has no portable index declaration, so host migrations create
  the unique keys and the `(health_check_id, started_at)` index. `on_demand_request_id` is
  `NOT NULL DEFAULT ''`: scheduled runs store `""`, on-demand runs a UUID.
- **Claim path.** A pre-check plus the insert run under an in-JVM lock, which gives the memory
  backend database-like uniqueness. Across pods, the database unique key decides.
- **Bounded execution.** A tick skips claims when every worker is busy. An on-demand run with no
  free worker returns UNKNOWN "busy" instead of queueing.
- **Runner pods alive** on the home counts distinct `runner_pod` values in results from the last
  two minutes. There is no separate pod heartbeat table.
- **Scheduler check.** `QScheduleManager` gains `isStarted()`, so the check does not look into
  Quartz internals.
- **`/health` and legacy indicators.** Legacy `HealthIndicator`s keep `durationMs` and their
  details (now redacted) on `/health`, because deploy checks read `buildInfo.details.commitShaShort`
  there. Registry checks and everything on `/ready` are status-only. Behavior changes for existing
  apps: the `database` indicator moves from `/health` to `/ready`, and a throwing or timed-out
  indicator is DOWN (503) instead of UNKNOWN (200). Apps whose readiness probe uses `/health`
  should switch it to `/ready` when they upgrade (kof22: part of the adoption PR).
- **Registry names are lowercase** (`diskSpace` registers as `core.diskspace`); `/health` keeps
  the legacy display keys.
- **`/qqq/v1/health`** is wired by `QApplicationJavalinServer` when `HealthMonitoringMetaData` is
  present. The runner starts as the `healthRuntime` runtime service, and is guarded against a
  second start in the same JVM.
