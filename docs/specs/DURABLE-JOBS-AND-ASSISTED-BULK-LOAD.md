# Durable jobs and assisted bulk load: design spec

Status: **Approved** (standing approval) · drafted 2026-10-13 · Author: James Maes (drafted with Claude)
Targets: QQQ 4.1.x core (`qqq-backend-core`, `qqq-backend-module-rdbms`, `qqq-middleware-javalin`), a new
**`QRun-IO/qbit-jobs`** (the Jobs app), `qbit-assistant` (the assistant tools), and one small
`qqq-frontend-next` change.

> **Owner decisions: all recommendations accepted (standing approval).** Every open question in
> section 9 is decided as recommended. The build follows the phases in section 7. This document is a
> design only; nothing was implemented or deployed with it.

Related: the architecture review (`docs/ARCHITECTURE-REVIEW-2026-10-13.md`, guardrails G1-G8), client
channels and the assistant wall (`docs/UI-API-CLIENT-CHANNELS.md`), UI API extensions
(`docs/UI-API-EXTENSIONS.md`), the realtime channel (#1144), `QAlerts.raise` and `QAlerts.notify`
(RC.5 and #1125), secured data (`docs/SECURED-DATA.md`), the alerts spec
(`2026-10-12-qbit-alerts-design.md`), the object storage spec, and the assistant spec
(`2026-10-11-qqq-assistant-qbit-design.md`).

---

## 0. The ask and the answer

The owner asked (2026-10-13):

> "with our bulk load system, do we have the ability to create and save profiles? to reuse and share?
> … can the assistant create the mapping profile for me? allow me to just upload a file to it, and it
> will setup the mapping profile and validate it and make sure it will work. same for asking the
> assistant to do something like run a process across 10,000 rows and ensure that they work and finish
> correctly etc (this implies some sort of job system for qqq — creating a long running batch job, and
> being able to monitor it, pause, cancel, restart etc) and ensure that a batch job will survive a
> server crashing etc"

He then added, as hard requirements:

> "any process or load or hard thing should be able to be ran as a async job, and jobs should be good
> hard core batch things that will survive, and be able to be reported on, work with emails and alerts
> etc"

**Short answers.**

- **Saved profiles: yes, they already exist.** You can save a bulk-load mapping as a profile, reuse
  it, and share it with named users as read-only or read-and-edit (section 1.1). Two things are
  missing: sharing with a group or role, and any way for the assistant to use profiles.
- **Durable jobs: no, not today.** Every background job and every multi-step process lives in one
  pod's memory. A pod restart loses it, cancelling it is only a hint, and pause and resume don't exist
  (section 1.2). This spec adds a **durable job kernel in core**, backed by persisted QQQ tables. Any
  process can run through it as a job, with no per-process rewrite (section 2). Jobs are chunked and
  checkpointed, leased with heartbeats, reclaimed when a pod dies, and reportable. They can be paused,
  resumed, cancelled, restarted and retried, and they alert and email on completion, failure and stall.
- **Assistant: it can do all of this, through the UI API only.** It can take an uploaded file, build
  and validate a mapping profile, save and share it, launch the load as a durable job and report the
  outcome. "Run process X over 10,000 rows" uses the same path (section 3). Permissions, write mode,
  confirmation and PHI routing are enforced by the server, not by the model.

**Where the code goes.** The kernel, tables, control processes and UI API operations go in **core**,
so core's own bulk processes, headless callers and every qbit can use them. The Jobs app goes in a new
**`qbit-jobs`**: its home, typed widgets, record-screen panels, retention and alert types. Next only
needs one generic change; it gets no jobs-specific pages (section 2.12).

---

## 1. Inventory: what exists today (develop @ b7bcbe863)

The coordinator's reading is checked below. Corrections and additions are in **bold**.

### 1.1 Saved bulk-load profiles: correct, with some detail
- `SavedBulkLoadProfile` (table `savedBulkLoadProfile`) has `label`, `tableName`, owner `userId`,
  `mappingJson` and `isBulkEdit`. `SharedSavedBulkLoadProfile` (table `sharedSavedBulkLoadProfile`)
  has `savedBulkLoadProfileId`, `userId` and `scope`. Both come from
  `SavedBulkLoadProfileMetaDataProvider`. **The table names are camelCase, from before the snake_case
  standard.** This spec does not rename them.
- The processes are `storeSavedBulkLoadProfile`, `querySavedBulkLoadProfile` and
  `deleteSavedBulkLoadProfile`. Sharing uses the generic sharing processes
  (`InsertSharedRecordProcess`, `EditSharedRecordProcess`, `DeleteSharedRecordProcess`,
  `GetSharedRecordsProcess`).
- **Sharing only works with individual users.** `ShareableTableMetaData` declares one audience type,
  `user`. The scopes are `READ_ONLY` and `READ_WRITE` (`ShareScope`). There is no group, role or
  "everyone" audience.
- The profile JSON is a `BulkLoadProfile` v1: `layout` (FLAT, TALL or WIDE), `hasHeaderRow`,
  `isBulkEdit`, `keyFields`, and a `fieldList` of `BulkLoadProfileField`. Each field has `fieldName`,
  `columnIndex`, `headerName`, `defaultValue`, `clearIfEmpty`, `doValueMapping` and `valueMappings`.
- **There is another consumer:** `qbit-sftp-data-integration` runs headless bulk loads
  (`ImportFileBulkLoad*`) driven by a profile.

### 1.2 `BulkLoadMappingSuggester`: correct
It works from the header row only. It matches massaged headers against field names, field labels and
"Table: Field" labels, and detects WIDE or TALL layouts for associations. It never looks at sample
values, infers no types, and never proposes value mappings.

### 1.3 `AsyncJobManager`: correct, and the gap is larger than stated
- `AsyncJobManager.getStateProvider()` returns `InMemoryStateProvider` (`TempFileStateProvider` is
  commented out). Its thread pool has 8 core threads, a maximum of 500, and a `SynchronousQueue`, so
  there is no queueing and no backpressure.
- `AsyncJobState` has only `RUNNING`, `COMPLETE` and `ERROR`. There is no cancelled, paused or queued
  state.
- **`cancelJob` only sets a flag (`cancelRequested`).** The flag is checked only by query loops (RDBMS,
  Mongo, the API module, the mock backend) and by `AsyncRecordPipeLoop` and `PrepareBulkEditStep`. It
  does not stop a transform or load that is already running.
- **`CancelProcessAction` and `/qqq/v1/processes/{p}/{uuid}/cancel` are not job cancel.** They run the
  process's optional *cancel step*, which is cleanup.
- **Process state is also only in memory.** `RunProcessAction.getStateProvider()` is in-memory, so a
  multi-step wizard such as bulk insert is tied to the pod that started it. A restart loses the wizard
  as well as the job.

### 1.4 Scheduler and process tracer: correct, plus one qbit the coordinator missed
- `QuartzScheduler` accepts Quartz `properties`, so an app can configure a clustered JDBC job store.
  That makes *triggers* durable, not running work. The `Pause`/`Resume Quartz jobs` processes pause
  triggers. `SimpleScheduler` is in memory.
- `ProcessTracerInterface` has two implementations in core, `LoggingProcessTracer` and
  `NoopProcessTracer`. **The persisted tracer is `qbit-standard-process-trace`.** It has process trace,
  summary line, key record and backend activity stats tables, plus a "recent traces" widget. Durable
  jobs keep calling the configured tracer for each chunk (section 2.10).

### 1.5 Uploads and storage (an addition)
- The v1 process step (`ProcessSpecUtilsV1`) and the legacy handler stream multipart uploads straight
  into the storage table named by `QJavalinMetaData.uploadedFileArchiveTableName`, under the key
  `date/process/uuid/filename`. The process then gets a `StorageInput`. **There is no temp file.**
  Whether this is object storage depends on the backend the app gives that table. If the table is not
  configured, uploads are refused. The local Website-Backend checkout does not set it.
- **Core still uses temp files** in `ExecuteReportStep` and `RenderSavedReportExecuteStep`
  (`createTempFile`). That breaks the no-filesystem rule for reports run as jobs; section 2.9 fixes it.
- Core cannot depend on `qbit-object-storage` (G1). Durable jobs therefore use core `StorageAction`
  against a configured storage table, which the app may back with S3, either through the filesystem
  module's S3 backend or through a table provided by qbit-object-storage.

### 1.6 The assistant: correct, with detail
- Core `AiToolKernel` tools are kinds `READ`, `NAVIGATION` and `WRITE_PROPOSAL`. The core tools are
  read-only. Their data tools call `QueryAction` directly, which bypasses `/qqq/v1` (F15, #1155). #1166,
  which moves them onto the UI API, is open.
- qbit-assistant `UiApiToolbox` runs its tools through the UI API gateway:
  - read tools: `list_tables`, `describe_table`, `query_records`, `count_records`, `get_record`,
    `search_records`, `render_widget`, `get_record_history`;
  - write tools: `create_record`, `update_record`, `delete_record`, which wait for confirmation as
    pending actions.

  The write mode is #21 (open).
- `DataAccessMode` per table or process is `NONE`, `READ` or `READ_WRITE` (processes: "run"). **But no
  tool runs a process yet, and there is no file or attachment tool.**
- `PhiRoutingGuard` and `ProviderService` enforce `hipaaEligible` per provider row under `hipaaMode`.
- `UiApiInvoker.forServer(server, "assistant")` fixes the channel. `audit.clientChannel` records
  `assistant/<turnId>`.

### 1.7 Other building blocks already on develop
- **Streamed ETL** (`StreamedETLWithFrontendProcess`): preview, validate (`supportsFullValidation`,
  `doFullValidation`) and execute steps; transaction levels `autoCommit`, `page` and `process`;
  `ProcessSummaryLine` results; `ExtractViaQueryStep`; `AsyncRecordPipeLoop`.
- **Bulk processes** are created for each table by `QInstanceEnricher`: `{table}.bulkInsert`,
  `.bulkEdit`, `.bulkEditWithFile` and `.bulkDelete`. They are checked by
  `BulkTableActionProcessPermissionChecker`.
- **Other pieces:**
  - `KeysetPager` (stable primary-key paging);
  - `UniqueKeyHelper`;
  - `QAuthenticationModuleInterface.createAutomatedSessionForUser`;
  - realtime (#1144, merged), with channels `record:`, `table:`, `user:me` and `instance`;
  - `QAlerts.raise/resolve/heartbeat/notify` (#1125, merged);
  - `UpdateInput.expectedVersions`, a version check that is documented as **not atomic**.
- **Related qbits:**
  - qbit-session-store has a table-backed session store;
  - qbit-email is a skeleton (scaffold and DESIGN.md only), so email is delivered through
    qbit-alerts' email channel (section 2.11).

---

## 2. Durable jobs (core kernel, store and contract)

### 2.1 Hard requirements
These restate the owner's requirements as testable rules.

- **R1, universal.** Any process can run as a durable job through one generic core path. That covers
  streamed ETL, bulk insert, edit and delete, reports and exports, record actions, scheduled processes
  and qbit processes. The UI ("Run in background"), the API and the assistant all use that path, and no
  process needs rewriting. A process can declare itself too large to run synchronously, and then it
  always runs as a job.
- **R2, hard-core batch.**
  - A job survives a pod crash or a redeploy.
  - It runs in checkpointed chunks, under leases with heartbeats, and orphaned work is reclaimed.
  - Delivery is at least once, with dedupe; it is exactly once when the job store and the destination
    share a transaction.
  - It can be paused, resumed, cancelled and restarted, and failed chunks or failed rows can be retried.
  - It has priorities, concurrency caps by job type and by user, and backpressure.
- **R3, reportable.**
  - Jobs, chunks and row errors are standard QQQ tables with full record screens. They work in reports,
    Data Analysis and Quick Search.
  - A Jobs app home shows live progress, throughput, failures and history.
  - Every job links to its audit trail.
- **R4, notifications.**
  - Completion, failure and stall alerts go through `QAlerts`: the in-app bell, honouring user
    preferences.
  - Email is opt-in, and its body has a summary and a link to the job.
- **R5, identity.**
  - A job runs with the submitting user's permissions and session scope, including the assistant
    channel and a scoped API token, and never with more.
  - Every run is audited under that user and channel.
- **R6, storage.** Only the database and object storage are used. There are no local files, and any
  buffering is bounded and always released.

### 2.2 Shape
```
core  com.kingsrook.qqq.backend.core.jobs
  DurableJobs (facade: launch, control, status)      DurableJobsMetaData (instance config)
  DurableJobStoreInterface  <- TableDurableJobStore (any backend with CONDITIONAL_UPDATE)
  JobWorker (per pod: claim -> run -> heartbeat -> complete)   LeaseReaper (per pod)
  JobPlanner + SourcePartitioner SPI:  StepCheckpointPlan | TableKeysetPartitioner | StorageFilePartitioner
  ChunkRunner: runs the process's own steps for one chunk under the job's session
  control processes: jobsPause, jobsResume, jobsCancel, jobsRetryFailed, jobsRestart, jobsRunAgain, jobsStart
core  rdbms module: CONDITIONAL_UPDATE capability (UPDATE ... WHERE pk=? AND lease_version=?)
core  javalin: POST /qqq/v1/processes/{processName}/jobs, job events on the realtime channel
qbit-jobs: Jobs app home (typed widgets), record panels, alert types, retention, Quick Search/Data Analysis registration
```
The package is generic. Per F16, it is not named after any qbit.

### 2.3 Tables (standard QQQ tables, prefix `jobs_`)
Core defines the tables with `DurableJobsMetaDataProvider.defineAll(instance, backendName, enricher)`,
the same pattern as `SavedBulkLoadProfileMetaDataProvider`. Without qbit-jobs, the kernel still works
and the tables still have standard screens. The `jobs_` prefix matches the qbit that owns the app.
Every table publishes ESB table events, as every main table does.

| Table | Key fields | Notes |
|---|---|---|
| `jobs_job` | `id`, `label`, `process_name`, `table_name`, `mode` (VALIDATE, EXECUTE), `status`, `priority` (0-9), `user_id` (owner), `client_channel`, `credential_scope_json`, `source_type` (STEPS, TABLE_FILTER, RECORD_IDS, STORAGE_FILE, ROW_SET), `source_json`, `params_json`, `idempotency` (SAME_TRANSACTION, NATURAL, NONE), `semantics` (CHUNKED, ATOMIC), `chunk_size`, `max_parallel_chunks`, `rows_total/done/ok/failed`, `chunks_total/done/failed`, `summary_json` (merged `ProcessSummaryLine`s), `output_storage_json` (report or export outputs), `parent_job_id` (retry or run-again of), `saved_bulk_load_profile_id`, `notify_email` (bool), `queued_at`, `started_at`, `finished_at`, `last_progress_at`, `error_message`, `lease_version` | The main table: nav entry, Quick Search, Data Analysis |
| `jobs_chunk` | `id`, `job_id`, `seq`, `status`, `range_json` (PK range, row range or step index), `input_storage_ref`, `attempt`, `max_attempts`, `lease_owner`, `lease_token`, `lease_expires_at`, `heartbeat_at`, `rows_in/ok/failed`, `summary_json`, `checkpoint_json` (process values after a step), `error_message`, `started_at`, `finished_at`, `lease_version` | Child list on the job |
| `jobs_row_error` | `id`, `job_id`, `chunk_id`, `row_number` (file row) or `record_pk`, `field_name`, `error_code`, `message` (classified CONFIDENTIAL), `retry_job_id`, `resolved` | Child list; reportable ("which rows failed and why") |
| `jobs_event` | `id`, `job_id`, `chunk_id`, `type` (LAUNCHED, CONFIRMED, PAUSED, RESUMED, CANCELLED, RECLAIMED, RETRIED, STALLED, COMPLETED, FAILED), `actor_user_id`, `client_channel`, `pod`, `detail` | The timeline. Human control actions are also audited normally. |
| `jobs_worker` | `pod_id`, `started_at`, `heartbeat_at`, `threads`, `draining`, `version` | Shows which pods run jobs. Leases never depend on it. |

**Status enums.**
- `jobs_job.status` is one of:
  - `AWAITING_CONFIRMATION`, `QUEUED`, `PLANNING`, `RUNNING`;
  - `PAUSING`, `PAUSED`, `CANCELLING`, `CANCELLED`;
  - `COMPLETED`, `COMPLETED_WITH_ERRORS`, `FAILED`, `NEEDS_REVIEW`.
- `jobs_chunk.status` is one of `PENDING`, `RUNNING`, `DONE`, `FAILED`, `RETRY`, `NEEDS_REVIEW` and
  `CANCELLED`.

**Security.**
- A record security lock on `user_id`: you see your own jobs, and the `jobs_admin` permission is the
  all-access key. Child tables inherit the lock through `job_id`.
- Permissions use the full names (`jobs_job.read` and so on).
- `params_json` and `jobs_row_error.message` are classified CONFIDENTIAL, so `FieldAccessHelper` masks
  them for the assistant channel and for roles without reveal.

### 2.4 Making any process a job (R1, no rewrite)
A process opts in with optional metadata, `QProcessMetaData.withDurability(DurableProcessMetaData)`:
- `eligible`: defaults to true for core bulk processes and for streamed ETL that uses
  `ExtractViaQueryStep` or `BulkInsertExtractStep`; false for everything else.
- `alwaysDurable`: the process is too large to run synchronously.
- `durableThresholdRows`: default 1,000.
- `chunkSize`: default 500.
- `idempotency` and `semantics`.
- `maxParallelChunks`: default 1.
- `maxConcurrentJobs`: per process.

An app can also turn on eligibility for any process by name in `DurableJobsMetaData`, with no code.

The kernel picks a **plan** for the process, generically:

1. **Streamed ETL with a partitionable extract (CHUNKED).** This covers bulk insert, edit and delete,
   record actions, and most data processes.
   - `TableKeysetPartitioner` (extract `ExtractViaQueryStep`): plans PK ranges over the frozen filter
     with `KeysetPager`. A chunk is the filter AND `pk BETWEEN lo AND hi`. Rows inserted later are not
     picked up, which matches today's semantics.
   - `StorageFilePartitioner` (extract `BulkInsertExtractStep`): streams the uploaded file once and
     writes chunk input objects of N rows as JSONL `BulkLoadFileRow`s, keeping the original row numbers,
     to the jobs storage table. CSV and XLSX both stream; there is no temp file. Each chunk then reads
     only its own object, so a restart never rescans the whole file.
   - The `ChunkRunner` runs the process's **own** transform and load step classes on each chunk, using
     the same `consumeRecordsFromPipe` path as `StreamedETLExecuteStep`. It opens one transaction for
     the chunk (page semantics) and merges the chunk's `ProcessSummaryLine`s into the job.
2. **ATOMIC.** A process whose transaction level is `process`, or that declares
   `semantics = ATOMIC`, runs as one chunk in one transaction. A crash rolls it back, and the reclaim
   reruns it from the start: all or nothing, but it survives.
3. **Any other process (STEPS plan).**
   - The job runs the process's backend steps from the launch step onward. Each backend step is a
     checkpoint: after a step finishes, the process values are saved to `jobs_chunk.checkpoint_json`
     (one chunk per step).
   - A reclaim resumes at the last finished step. A step that was running when its pod died is rerun
     only if it is `NATURAL` or `SAME_TRANSACTION`; otherwise the job goes to `NEEDS_REVIEW`.
   - Frontend steps are never part of a job. The UI, API or assistant collects their values before
     launch.
   - This covers reports and exports (section 2.9), scheduled processes and qbit processes.

**How a job starts.**
- **"Run in background".** At any eligible process's review or launch point, the client calls
  `POST /qqq/v1/processes/{processName}/jobs` with the values and either a records filter, a list of
  record ids, or upload refs. It returns `{jobId}`.
- **Automatically.** When the extract count is over `durableThresholdRows`, or the process is
  `alwaysDurable`, the normal process step returns its existing `jobUUID` async response, now bound to
  a durable job. The current Next async-status screen keeps working unchanged:
  `/status/{jobUUID}` reads `jobs_job` and adds `jobId` so the client can link to the job.
- **Scheduled.** A scheduled process that is eligible creates a job (`DurableJobsMetaData.scheduledRunsAsJobs`,
  default true for `alwaysDurable` processes).

### 2.5 Idempotency and exactly-once (R2)
- **SAME_TRANSACTION** is chosen automatically when the jobs store and the destination table share a
  backend that supports transactions, which is kof22's single Postgres. The chunk's load and its `DONE`
  update commit in one transaction, and the `DONE` update is fenced by `lease_token`. A rerun after a
  crash therefore never double-applies. This is exactly once.
- **NATURAL**: re-applying the chunk is harmless. Examples are bulk edit (set fields to values), bulk
  delete, and an upsert on unique keys. The chunk is retried freely.
- **NONE**, the default for unknown processes: it is at least once only by explicit choice.
  - A chunk reclaimed from a dead pod goes to `NEEDS_REVIEW`.
  - The user sees "this chunk may have partly run" and chooses Retry or Skip.
  - A chunk whose transaction rolled back (an exception before commit) is still retried automatically.
- **Dedupe for inserts across backends.** Bulk insert gets a `jobs_dedupe_key = jobId:rowNumber` when
  the destination has a unique key or a declared `jobRowKeyField`. Otherwise it uses `UniqueKeyHelper`
  on the table's unique keys. Rows that are already present on a rerun are counted as "already loaded".

### 2.6 Leases, heartbeats and reclaim (R2)
- **Claiming.** `JobWorker` claims chunks through the store's **atomic** conditional update (a new
  backend capability, `CONDITIONAL_UPDATE`). The RDBMS module runs
  `UPDATE jobs_chunk SET status='RUNNING', lease_owner=?, lease_token=?, lease_expires_at=now()+ttl,
  attempt=attempt+1, lease_version=lease_version+1 WHERE id=? AND lease_version=? AND status IN
  ('PENDING','RETRY')` and checks that one row was updated. The memory backend implements it for tests.
  The existing `expectedVersions` is not enough, because it is not atomic.
- **Clocks.** Lease times use the database clock (`now()`), never the pod clock, so clock skew does not
  matter.
- **Heartbeats.** A worker renews its lease every `ttl/3`. Defaults: a TTL of 60 seconds and a reaper
  pass every 15 seconds.
- **Reaping.** `LeaseReaper` runs on every pod.
  - A `RUNNING` chunk with an expired lease goes to `RETRY` and gets a `RECLAIMED` event. If it was at
    `max_attempts` (default 3), it goes to `FAILED` instead. If its idempotency is `NONE`, it goes to
    `NEEDS_REVIEW`.
  - The reaper also emits `QAlerts.heartbeat("jobs.reaper")`.
- **Fencing.** Completion, progress and checkpoint writes all require the current `lease_token`. A
  zombie pod (a long GC pause, or a network split) that comes back after reclaim fails that update and
  rolls back its chunk transaction.
- **Graceful drain on SIGTERM or redeploy.** The pod stops claiming and finishes the current page. It
  then commits a mid-chunk checkpoint where the plan allows one (keyset: the last PK done) or releases
  the lease (`PENDING`, no attempt increment). Nothing waits for the lease to expire.
- **Wake-up.** Workers poll every 2 seconds with jitter. When the ESB is present, a `qqq.jobs` wake-up
  message cuts the latency; it is optional, and correctness never depends on it.

### 2.7 Control (R2)
Control operations are **core processes registered as record actions on `jobs_job`**, so every client
runs them through the standard `/processes` API with no extra endpoints. The kernel facade backs them.

| Action | Effect |
|---|---|
| Start (confirm) | `AWAITING_CONFIRMATION` to `QUEUED`. Required for assistant launches (section 3.5). |
| Pause | No new claims. Running chunks stop at the next page boundary and checkpoint, or finish. Status `PAUSING`, then `PAUSED`. |
| Resume | `PAUSED` to `QUEUED`. |
| Cancel | No new claims. A running chunk stops at the next page boundary and rolls back its open transaction. Pending chunks go to `CANCELLED`. **Chunks already committed stay committed**: cancelling does not undo work, and the summary says so. |
| Retry failed chunks | `FAILED` and `NEEDS_REVIEW` chunks go back to `PENDING`, in the same job. |
| Retry failed rows | Creates a child job (`parent_job_id`, `source_type=ROW_SET`) over the failed rows only. For files, these are row numbers read from the chunk input objects; for tables, the PKs. It uses the same params, or a corrected bulk-load profile. Row errors get `retry_job_id`. |
| Download failed rows | Writes a CSV of the failed rows plus an error column to the jobs storage table, and returns a process download (`ProcessFileDownload`). The user can fix it in a spreadsheet and load it again. |
| Restart | Resets every chunk that is not `DONE` and reruns them, in the same job. |
| Run again | A new job with the same params, from the start. |

Pause and cancel come from a cooperative check in the `ChunkRunner` between pages. It reads the job
status, cached for 2 seconds. The flag in `AsyncJobStatus` is not used.

### 2.8 Concurrency, priority and backpressure (R2)
- **Per pod.** `workerThreads` (default 2; 0 means the pod runs no jobs, for API-only pods).
- **Per job.** `max_parallel_chunks` (default 1, which keeps today's ordering). A higher value needs
  `chunkParallelSafe` on the process.
- **Per process.** `maxConcurrentJobs` (default 2; 1 for bulk insert into a table that has unique keys).
- **Per user.** `maxActiveJobs` (default 5). Any job beyond a cap stays `QUEUED`, with a reason.
- **Claim order.** Highest `priority` first, then the oldest `queued_at`. Round-robin between users
  stops one user's 50 jobs from starving everyone else.
- **Backpressure.**
  - The kernel runs chunks on its own bounded pool, never `AsyncJobManager`'s 500-thread pool.
  - A launch is refused with 429 when the user has more than `maxQueuedJobs` (default 50) queued.
  - The record pipe capacity stays as each process sets it.

### 2.9 Storage with no filesystem (R6)
- Everything is stored in a configured `jobsStorageTableName`, an app storage table that should be
  object storage: chunk input objects, error CSVs, and report or export outputs.
- `ExecuteReportStep` and `RenderSavedReportExecuteStep` change to stream into `StorageAction`
  output streams instead of `createTempFile`. That change is part of phase 2. XLSX output needs a
  streaming writer with a bounded buffer.
- **Retention.** `qbit-jobs` purges finished jobs after `retentionDays` (default 30), together with
  their chunk objects. Chunk objects of a `COMPLETED` job with no errors are deleted after
  `inputRetentionDays` (default 7).

### 2.10 Identity, permissions and audit (R5)
- **At launch** the job captures:
  - `user_id`;
  - `client_channel`, the full value, for example `assistant/<turnId>` or `api/<tokenId>`;
  - `credential_scope_json`: the security key values from the launching session, plus the
    operation scope of a scoped token (F9).
- **At each claim** the worker builds a session with `createAutomatedSessionForUser(user_id)` and then:
  - **intersects** its security keys with the captured ones, so the job never has more access than at
    submit;
  - sets `withClientChannel(job.client_channel)`;
  - re-checks the process and table permissions, the channel wall, and whether the token was revoked.

  A user who lost access fails the job with a permission error. The job never runs with elevated
  access.
- **Audit.** Every record a chunk writes is audited under that user and channel (`audit.clientChannel`).
  The chunk also sets an audit detail of `job:<id>/chunk:<seq>`.
  - The job record screen links to "Changes made by this job": a saved filter on audit, by user,
    channel, time window and job id.
  - Control actions are audited on `jobs_job`, and also go to `jobs_event` with the actor and channel.
- **Process trace.** The configured `ProcessTracerInterface` is called for each chunk, with a key record
  of the job. When `qbit-standard-process-trace` is installed, every chunk run has a trace linked to
  the job.

### 2.11 Progress, alerts and email (R3, R4)
- **Realtime (#1144).**
  - The kernel publishes `job.progress` events, throttled to at most one per second per job:
    `{jobId, status, chunksDone, chunksTotal, rowsDone, rowsTotal, rowsFailed, rowsPerSecond,
    etaSeconds}`. It also publishes `job.status` on every change.
  - They go to `record:jobs_job:{id}` and to the owner's `user:me`.
  - The events carry counts only, never row values.
  - This adds typed event schemas to the UI API (a minor bump and a changelog entry). Polling the
    record is the fallback.
- **Alert types**, declared with `QAlertTypeMetaData`:
  - `jobs.job_failed`: raised on `FAILED` or `NEEDS_REVIEW`, deduped by job id, and resolved when a
    retry completes;
  - `jobs.job_stalled`: `RUNNING` with no progress for `stallMinutes` (default 15), or `QUEUED` past
    `queueStallMinutes`;
  - `jobs.reaper` heartbeat: no pod is reaping.
- **Notifications.** `QAlerts.notify` goes to the owner on `COMPLETED`, `COMPLETED_WITH_ERRORS` and
  `FAILED`, with a summary (rows ok and failed, duration, the top three error groups) and a link to the
  job.
  - Delivery honours the user's notification preferences in qbit-alerts.
  - Email is opt-in, either for each job (`notify_email`, the "Email me when done" checkbox) or as a
    standing preference. It is delivered by qbit-alerts' email channel, which uses qbit-email when that
    exists.
  - Core and qbit-jobs never depend on qbit-alerts or qbit-email (G1).

### 2.12 UI: qbit-jobs plus one small Next change
**Recommendation: a new `qbit-jobs` repo, not core Next pages.** Next stays generic, the home is built
from typed widgets (F1, G5), and the jobs tables already render with the standard screens.
- **Jobs app home**, which owns the job data:
  - running jobs with live progress bars (`job.progress`);
  - throughput (rows per minute, by process);
  - failures and `NEEDS_REVIEW` items;
  - the queue with its reasons;
  - workers and pods;
  - 30-day history.

  It is built from typed `widgetData` (statistics, table, chart) with no HTML. It is readable at every
  width, in light and dark mode.
- **Navigation.** The nav lists `jobs_job` only. Chunks, row errors and events are child lists on the
  job record.
- **Record panel.** A progress panel on the job record (a typed widget plus realtime). The record
  actions are those in section 2.7.
- **Search and analysis.** It registers `jobs_job` with Quick Search, and all four tables with Data
  Analysis, through their published contracts.
- **Next change (one PR).**
  - When a process response has `jobId`, show "Running as a background job" with a link to the job.
  - Offer "Run in background" when the process metadata says `durable.eligible`.
  - This is generic, and names no qbit (F20).

---

## 3. Assisted bulk load

### 3.1 Flow
1. **Attach.** The user attaches a file in the assistant panel.
   - Next uploads it through the UI API, as the user (channel `next-ui`), to `POST /qqq/v1/uploads`.
   - The file is streamed into the uploads storage table; nothing is written to disk.
   - The call returns an `uploadRef`. The model never sees bytes.
2. **Inspect and suggest.** The assistant asks the server for a mapping suggestion. The server runs
   `BulkLoadMappingSuggester`, then the model refines it with column profiles (section 3.3).
3. **Dry-run.** The assistant validates the profile. It reads the error summary, then changes the
   mapping, defaults or value mappings and validates again, until the errors are zero or are explained
   to the user.
4. **Save and share.** The assistant saves the profile and shares it per its scope.
5. **Launch.** The assistant launches the load as a durable job. A user confirmation is required
   (section 3.5).
6. **Report.** The assistant follows progress during the turn. If the turn ends first, the completion
   notification links to the job, and the assistant can report on it later with `job_status`.

### 3.2 Core UI API operations (spec'd, in core; any client can use them)
These are stateless and keyed by `uploadRef`, so they are not tied to a pod the way the bulk-insert
wizard's in-memory state is. They **reuse** the bulk-insert step classes: there is no second
validation path. Next's wizard can move onto them later.

| Operation | Purpose |
|---|---|
| `POST /qqq/v1/uploads` (multipart) | Streams the file to the uploads storage table. Returns `UploadRef {ref, fileName, sizeBytes, contentType, expiresAt}`. The `ref` is opaque, bound to the session user, and expires (default 7 days; a job that uses it extends it). |
| `GET /qqq/v1/bulkLoad/uploads/{ref}/profile?table=` | Headers and **column profiles**: inferred type, percent blank, distinct count, maximum length, pattern class (date-like, email-like, numeric, code-like), and up to N sample values **only where allowed** (section 3.3). |
| `POST /qqq/v1/bulkLoad/{table}/suggestMapping` | `{uploadRef, isBulkEdit}` returns a `BulkLoadProfile` v1 from the suggester, with unmapped columns, unmapped required fields, and possible-value columns whose values need mapping. |
| `POST /qqq/v1/bulkLoad/{table}/validate` | `{uploadRef, profile, mode: SAMPLE or FULL}` runs the bulk-insert mapping, value mapping and transform (validation and preview) with **no store**. It returns the standard `ProcessSummaryLine`s, counts and error groups, each with row numbers, `field_name`, `error_code` and a message masked per `FieldAccessHelper`. A FULL run over `durableThresholdRows` becomes a **VALIDATE-mode durable job** and returns `jobId`. |
| `POST /qqq/v1/processes/{table}.bulkInsert/jobs` | The generic launch (section 2.4), with `{uploadRef, savedBulkLoadProfileId or profile, values}`. |

Saving and sharing profiles uses the existing processes (`storeSavedBulkLoadProfile`,
`insertSharedRecord`), so there is no new API. **Addition:** an `everyone` audience and a role
audience on `ShareableTableMetaData` (phase 5), so a profile can be shared with a team and not only
with named users.

### 3.3 PHI rules for the model refinement
- **Classification follows the target field.** The suggester's mapping (and each later revision) maps
  a column to a field, and the column takes that field's classification (`DataClassification`,
  `FieldAccessHelper` for channel `assistant`).
- **Unmapped columns are treated as classified (fail closed).** The model can still see their headers,
  but not their values.
- **Sample values.**
  - They are sent only when the turn's provider is `hipaaEligible`, or when the column's field is
    unclassified and `FieldAccessHelper` gives FULL to the assistant.
  - In every other case the model sees only the header and the **column profile** (shape statistics),
    which is usually enough to map dates, emails, codes and names correctly.
  - The server decides this, in the profile and validate operations, from the provider routing that
    qbit-assistant passes (`X-QQQ-AI-Route: hipaa` or `standard`, set by the invoker and never by the
    model) and from `PhiRoutingGuard`.
- **Value mappings** for possible-value fields send the distinct source values only when the field is
  unclassified. Otherwise the server proposes exact and case-insensitive matches, and the user confirms
  the rest.
- **No header row.** If the file seems to have no header row (the suggester matches nothing), row 1 is
  treated as data under the same rules.
- **Logging.** Tool-call logs keep the profile and the counts, never the sample values.

### 3.4 Assistant tools (qbit-assistant, all through `UiApiInvoker`)
These names are aligned with the qbit-assistant bulk-profile work in progress, so there is one set.

| Tool | UI API call(s) | Kind |
|---|---|---|
| `bulk_profiles_list(tableName)` / `bulk_profile_describe(id)` | `querySavedBulkLoadProfile` process / table GET | read |
| `bulk_file_attach` | returns the `uploadRef` the panel already uploaded, plus `.../profile` (masked) | read |
| `bulk_mapping_suggest(tableName, uploadRef)` | `suggestMapping`, then model refinement over headers and permitted samples | read |
| `bulk_load_dry_run(tableName, uploadRef, profileJson)` | `validate` (SAMPLE, then FULL) | read (no store) |
| `bulk_profile_save(label, tableName, profileJson, share?)` | `storeSavedBulkLoadProfile`, `insertSharedRecord` | write (confirm) |
| `bulk_load_run(tableName, uploadRef, profileId)` | `POST /processes/{t}.bulkInsert/jobs` | write (job, `AWAITING_CONFIRMATION`) |
| `run_process_job(processName, table, filter or ids, values)` | `count_records`, a VALIDATE job when supported, then a `POST /processes/{p}/jobs` launch | write (job) |
| `job_status(jobId)` / `job_control(jobId, pause or cancel)` | table GET on `jobs_job`, `jobsPause` / `jobsCancel` | read / write |

In qbit-assistant, `bulk_load_run` goes through a `BulkLoadRunner` seam: `start(request)` returns
`RunHandle {status, jobId or processUUID, summary}`. Until phase 4 lands, the runner uses the existing
process endpoints synchronously, under the threshold. After that it always calls the jobs launch. The
generic pieces (suggester, dry-run, uploads) are **core UI API operations**, not `AiToolKernel` tools:
#1166 is moving the kernel's data tools onto the UI API, and new AI-only data paths are out (F15).

### 3.5 "Run process X over 10,000 rows"
The same path:
1. The assistant resolves the filter.
2. It calls `count_records` and shows the count.
3. Where the process supports full validation, it runs a **VALIDATE** job.
4. It reports the validation summary.
5. It launches an **EXECUTE** job.

**Enforced by the server, not by the model:**
- **Permissions.** Every call runs as the user (`UiApiInvoker.forServer(server, "assistant")`):
  - table and process permissions;
  - `BulkTableActionProcessPermissionChecker`;
  - the record locks;
  - the channel wall, where `jobs_*` is readable but `jobs_admin` is denied to `assistant`;
  - qbit-assistant's `DataAccessMode`, which must be `READ_WRITE` (the "run" mode) for the target
    table or process.
- **The confirmation gate (core).**
  - A job launched from channel `assistant` is always created as `AWAITING_CONFIRMATION`.
  - Core refuses Start, Resume, Retry, Restart and Run again from the `assistant` channel. Only a
    non-assistant channel can start it: the user's Confirm button in the panel, or the job record
    action.
  - The assistant may pause or cancel its own jobs.
  - An optional per-instance auto-start for small jobs (`assistantAutoStartMaxRows`, default 0, which
    means off) is the only exception.
- **Caps.**
  - The assistant cannot set `priority` above the default.
  - It counts against the user's caps, plus its own (`assistantMaxActiveJobs`, default 2).
- **Audit.** The job and all its writes carry `client_channel=assistant/<turnId>`. `jobs_event`
  records who confirmed and through which channel.

---

## 4. Contracts, briefly
- **`DurableJobs`.** `launch(LaunchRequest)` returns a `jobId`. `control(jobId, Action)`.
  `status(jobId)`.
  - `LaunchRequest`: `{processName, values, source (filter | ids | uploadRefs | steps), mode,
    priority, notifyEmail, label}`.
- **`DurableJobStoreInterface`.**
  - Jobs: `insertJob`, `planChunks` (batch).
  - Leases: `claim(podId, limits)`, `heartbeat(lease)`, `reapExpired(now)`.
  - Chunk writes, which need the lease token: `completeChunk(lease, outcome, tx)`,
    `checkpoint(lease, json)`, `recordRowErrors(lease, errors)` (batched).
  - Status: `setJobStatus(jobId, from, to)` (CAS).
- **`SourcePartitioner`.** `plan(job, session)` returns the chunk ranges.
  `openChunk(chunk)` returns an `AbstractExtractStep`, the same extract class restricted to the chunk.
- **`DurableProcessMetaData`.** See section 2.4. Instance-level settings are in `DurableJobsMetaData`
  (storage table, worker threads, TTLs, caps, thresholds, retention).
- **Validation at startup.** `QInstanceValidator` fails when durable jobs are enabled and the jobs
  backend lacks `CONDITIONAL_UPDATE`, when the storage table is missing, or when an `alwaysDurable`
  process is not eligible.

---

## 5. What does not change
- `AsyncJobManager` stays, for short interactive work under the threshold. It is not made durable.
- The bulk-insert wizard's step classes and summary lines stay. A durable job runs those same classes.
- The process API stays as it is. The jobs launch is a new operation next to it, and the
  async-status response gains an optional `jobId`.

---

## 6. Acceptance tests

### 6.1 Kill-the-pod (required, in CI)
**Setup.**
- Postgres (Testcontainers) and **two** QQQ server JVMs (A and B), with `workerThreads=1`, a 10-second
  lease TTL and a 3-second reaper interval.
- A table with a unique key, and a 10,000-row CSV with 20 deliberately bad rows. The CSV is uploaded
  through `POST /qqq/v1/uploads` and launched as `{t}.bulkInsert` with chunk size 500.

**Steps.**
1. Wait until A holds a `RUNNING` chunk and at least 3 chunks are `DONE`.
2. `kill -9` A, mid-chunk.

**Assert.**
- **Reclaim and finish.**
  - B reclaims the orphaned chunk within TTL plus the reaper interval, and writes a `RECLAIMED` event.
  - The job ends `COMPLETED_WITH_ERRORS`.
- **Exactly once.**
  - The table holds exactly 9,980 new rows: no duplicates and none missing.
  - The unique-key check passes, and the chunk that was running when A died has `attempt=2`.
- **Row errors.**
  - `jobs_row_error` has exactly 20 rows, with the right row numbers.
  - Retry failed rows, after the bad values are fixed in a corrected profile, loads those 20.
- **Audit.** There are 9,980 insert audits with the launching user and `client_channel`, plus the
  job detail.
- **Alerts.** A `job.progress` event arrived on `record:jobs_job:{id}`, and `QAlerts.notify` fired once.

### 6.2 Other tests
- **Graceful redeploy.** SIGTERM to A mid-job. A releases its lease with no attempt increment, B
  continues, and no chunk reruns.
- **Fencing.** Suspend A's worker thread past its lease, let B reclaim, then resume A. A's completion is
  refused and rolled back, and the row count is still exact.
- **Non-idempotent process.** A custom process with `idempotency=NONE` that is killed mid-chunk goes
  to `NEEDS_REVIEW`. "Retry chunk" completes it.
- **STEPS plan.** A three-backend-step process killed after step 2 resumes at step 3 with step 2's
  values. A report process run as a job writes its output to storage with no temp file.
- **Control.**
  - Pause stops new claims and reaches `PAUSED`.
  - Resume finishes the job.
  - Cancel leaves committed chunks committed, sets the rest `CANCELLED`, and the summary says how many
    were applied.
  - Restart and Run again behave as in section 2.7.
- **Caps and priority.**
  - A user's sixth job stays `QUEUED` with a reason.
  - A higher-priority job is claimed first.
  - Over `maxQueuedJobs`, the launch gets 429.
- **Identity.**
  - Remove the user's table-insert permission mid-job: the next chunk fails with a permission error,
    and nothing is written beyond that point.
  - A token-scoped launch cannot exceed the token's scope.
- **Assistant.**
  - A job launched from the `assistant` channel is `AWAITING_CONFIRMATION`.
  - A Start from the `assistant` channel gets 403; a Start from `next-ui` works.
  - Audits carry `assistant/<turnId>`.
- **PHI.**
  - With a non-`hipaaEligible` route, `.../profile` and `validate` return no values for classified or
    unmapped columns: the test asserts there are no sample values in the payload or in the tool-call log.
  - With a `hipaaEligible` route, the samples are present.
- **Mapping loop.** A scripted model provider drives attach, suggest, dry-run (finds 3 errors), fix,
  dry-run (0 errors), save, share (READ_ONLY to user B), and run. User B then sees the profile in the
  bulk-insert wizard.
- **Universal.** A qbit process with no durability metadata, enabled by name in `DurableJobsMetaData`,
  runs as a STEPS job.
- **Reportable.**
  - The job tables appear in a saved report and in Data Analysis.
  - `jobs_job` is found by Quick Search.
  - The Jobs home is screenshot light and dark at 390 and 1440 px.
- **Load.** 1,000,000 rows at chunk size 1,000 across two pods. The test reports throughput, and
  checks that memory stays flat (bounded buffers) and that no files are created (a read-only
  `java.io.tmpdir` in the test).

---

## 7. Phases

| Phase | Repo(s) | Delivers |
|---|---|---|
| 1. Kernel | core, rdbms | `CONDITIONAL_UPDATE` (rdbms and memory). Tables and `DurableJobsMetaDataProvider`. Store, worker, reaper, fencing, drain. STEPS and ATOMIC plans. Control processes. `POST /processes/{p}/jobs`. Async-status `jobId`. Kill-the-pod test with the STEPS plan. |
| 2. Chunked ETL | core | `TableKeysetPartitioner`, `StorageFilePartitioner` (chunk objects), `ChunkRunner`, SAME_TRANSACTION and dedupe, row errors, retry failed rows, download failed rows, VALIDATE mode, the threshold and `alwaysDurable`. Bulk insert, edit and delete, and streamed record actions, made eligible. Reports stream to storage (temp files removed). The full section 6.1 test. |
| 3. Observability | core, javalin, Next | `job.progress` and `job.status` realtime events (UI API minor), `QAlerts` types and notify, stall detection, audit detail and the audit link, the tracer per chunk. The Next "Running as a background job" link and "Run in background". |
| 4. qbit-jobs | new repo | The Jobs app home (typed widgets), record panels, retention purge, Quick Search and Data Analysis registration, permissions and the record lock, screenshots. |
| 5. Assisted bulk load | core, qbit-assistant | `POST /uploads`, the bulk-load profile, suggest and validate operations, with spec fragments and contract tests. PHI routing. Assistant tools (section 3.4) on `UiApiInvoker`. The core confirmation gate. `run_process_job`. Share audiences for everyone and roles. |
| 6. Hardening | all | The 1M-row load test, multi-pod chaos in CI nightly, scheduled processes as jobs, and kof22 adoption: a Liquibase changeset taking the next free number, and S3-backed upload and jobs storage tables. |

Phase 5's assistant tools can start before the kernel. The qbit-assistant `BulkLoadRunner` seam runs
small loads synchronously until phase 2 ships, then switches to the jobs launch. Dependencies are on
released RCs only. A qbit that needs this before an RC uses a stand-in tagged
`STAND-IN(core=#N, removeBy=RC.N)` (G4).

---

## 8. Risks

| Risk | Mitigation |
|---|---|
| Double execution on reclaim | SAME_TRANSACTION and fencing for exactly once; dedupe keys for inserts; `NEEDS_REVIEW` for `NONE` instead of a silent rerun |
| Permission drift during long jobs | A session per claim, current permissions intersected with the launch scope; fail closed |
| PHI leaks to the model through samples, error messages or logs | The server decides from classification and the provider route; unmapped columns fail closed; CONFIDENTIAL messages are masked; no values in tool logs |
| Database load from polling and leases | Polling with jitter, batch claims, indexes on `(status, priority, queued_at)` and `lease_expires_at`, the optional ESB wake-up |
| A large XLSX in memory | Streaming readers and writers; chunk objects bound the memory each chunk uses |
| Committed work under Cancel surprises users | The summary and the UI state "N rows were applied and are not undone"; offer ATOMIC where all-or-nothing matters |
| A runaway assistant job | The core confirmation gate, assistant caps, and pause or cancel from any channel |
| The wizard is still tied to one pod (in-memory process state) | Out of scope here. The new stateless bulk-load operations sidestep it for the assistant, and a database-backed `StateProviderInterface` is a follow-up issue |
| Storage cost and orphaned objects | Retention purge, input objects deleted after success, a nightly orphan sweep |
| Backends without transactions or CAS | Startup validation refuses them as the jobs store; such destinations get NATURAL or `NONE` semantics only |

---

## 9. Owner questions (each decided as recommended)
1. Should the UI be a new `qbit-jobs` or core Next pages? **A new qbit-jobs**, with typed widgets; Next
   stays generic.
2. Who should be notified when a job completes, by default? **The bell for the owner by default; email
   opt-in for each job or by preference.**
3. What should the default synchronous threshold be? **1,000 rows; a process can override it or
   declare `alwaysDurable`.**
4. Should the assistant be able to auto-start jobs? **No (0 rows) by default; the user always
   confirms.**
5. What should cancelling do with committed chunks? **Keep them, and say so; use ATOMIC semantics
   where all-or-nothing matters.**
6. How long should jobs be kept? **30 days for jobs; 7 days for input objects after a clean success.**
7. Should profiles be shareable with teams? **Yes: add `everyone` and role audiences (phase 5).**
