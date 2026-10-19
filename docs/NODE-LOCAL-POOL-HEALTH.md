# Node-local pool health adoption

Issue #1273 separates database connectivity from the pool inside each JVM. Connectivity failures still become DOWN immediately. Pool gauges remain available in results. A waiter snapshot begins a 30-second observation window; continued waiters at or after that window become DEGRADED. A sample with no waiters resets the window. New checkout timeouts and current suspected leaks become DEGRADED immediately. An unchanged cumulative timeout counter does not keep a warning active. Missing gauges produce UNKNOWN.

Node-local execution is **off by default**. While it is off, the shared database check continues reporting pool pressure, including the improved transient-waiter handling. Cross-pod state independence requires activation after the upgrade below.

## Rolling upgrade

1. Apply the additive schema migration to every health storage database before deploying this version. Adapt identifiers and datetime type to the application's normal migration conventions:

   ```sql
   ALTER TABLE health_check ADD COLUMN runner_pod VARCHAR(255) NULL;
   ALTER TABLE health_check ADD COLUMN runner_lease_expires_at TIMESTAMP NULL;
   ```

   No new unique constraint is needed: existing unique `health_check.name` and result `(health_check_id, slot, on_demand_request_id)` keys remain authoritative. Existing rows have null ownership and remain shared.

2. Roll out the new library to **every health runner sharing these tables**, including web, worker and gateway processes. Keep node-local checks disabled. Shared checks continue running; simultaneous pod downtime is unnecessary.
3. Verify no old health-runner binary remains. Old synchronization code cannot recognize ownership and will unregister another pod's local rows. The new default prevents those rows from being created during the mixed-version phase.
4. Enable `HealthChecksMetaData.ofOrWithNew(instance).withNodeLocalChecksEnabled(true)` in each runner's application configuration, before starting its `HealthRunner`. This is an explicit application opt-in, not a production change made by this patch. Use a unique pod/process-lifetime runner ID; do not reuse one ID across live replicas.
5. Verify one shared `core.database.<backend>` check plus a `core.pool.<backend>.pod.<stable-hash>` row per pooled runner. The ownership column and result details identify the pod. Each local row has independent claims, state, alert deduplication and recovery. A healthy pod does not clear another pod's alert. The database component aggregates all registered local rows.

All participating runners must support conditional table updates (the existing memory and RDBMS backends do). Custom connection providers exposing pool gauges can participate; an uninitialized configured provider reports UNKNOWN. The default unpooled provider does not register a local pool check.

## Node lifecycle

Every runner tick renews its own local rows' lease for at least 60 seconds, or three definition intervals, whichever is longer. A peer observes an expired lease as UNKNOWN, not a successful recovery. After a further hour the row is unregistered but retained as history, and its alerts are resolved with an explicit retirement reason. A returning owner registers its same identity and supplies its own sample. Cleanup compares the observed lease before writing, so a renewed lease wins against a stale cleanup attempt.

New synchronization preserves peer-owned rows even when that runner has only shared definitions. It does not make an old binary safe after activation. Before rolling back to an old binary, disable local checks everywhere, allow stale local rows to retire, and verify there are no registered local rows. Keep the additive columns for rollback compatibility. Shared pool monitoring resumes while activation is off; do not drop schema columns during a rolling rollback.

The observation window uses monotonic JVM time and is covered by deterministic tests. It is sampled pressure, not proof that a waiter remained continuously blocked between samples. No application-wide failure threshold or deployment setting is increased.
