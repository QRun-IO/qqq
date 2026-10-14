# Job progress, job log and the Jobs tray: design

Status: proposed (2026-10-14). Extends `DURABLE-JOBS-AND-ASSISTED-BULK-LOAD.md` (R1-R6) and
section 2.11 of it. UI API: the open 1.4.0 entry.

## 0. The ask

The owner, shown "Run backup now" displaying only "Working / Executing Process / 1 of 1 / Cancel":

> a) real time progress of what it is done (logs etc?), b) ability to leave the job have it keep
> running (a jobs icon or menu up top like notifications, to see all running jobs or recent jobs that
> finished or failed). I should be able to click on one of them to bring them back up. The animations
> for running jobs should look really nice and smooth.

Defaults (owner can override):
- Every eligible process offers **Run in background**. Long processes (`alwaysDurable`: backups,
  bulk loads, imports) move to the background automatically about 5 s after they start, unless the
  user chooses to stay.
- The header tray shows the **user's own** jobs. Holders of `jobs_admin` also see everyone's jobs on
  the full Jobs page (Admin > Operations > Jobs).

## 1. What is wrong today

- "Run backup now" (`backupRunPlan`) is a streamed ETL over one plan record. One record is under
  `durableThresholdRows`, so it runs inline through `AsyncJobManager`, and the only progress is the
  record count ("1 of 1"). The real work (tables, bytes, media, upload, verify) is invisible.
- Durable jobs have counts (`rows_*`, `chunks_*`) and a throttled `job.progress` event, but no
  phases, no bytes, no message and no log.
- Nothing lists a user's running jobs, so leaving the screen loses the job.

## 2. Model (core, `com.kingsrook.qqq.backend.core.jobs.progress`)

### 2.1 Structured progress
A `JobProgressSnapshot` (JSON-serializable, counts and text only, never row values):

| Field | Meaning |
|---|---|
| `phases[]` | `{name, label, status}`; status is `PENDING`, `RUNNING`, `DONE`, `SKIPPED`, `FAILED` |
| `currentPhase` | the running phase's name |
| `unitLabel` | what `done/total` count ("tables", "rows", "files"); default "rows" |
| `done`, `total` | counts in the current phase (total may be null: indeterminate) |
| `bytesDone`, `bytesTotal` | optional byte progress |
| `percent` | 0-100, derived (see below), never decreases within a job attempt |
| `etaSeconds` | derived from the rate over a sliding window; null when unknown |
| `message` | one short line, e.g. "Backing up kof22_blog_post (12 of 48)" |
| `updatedAt` | server time of the last write |

**Percent.** If phases declare `weight`s, percent is the weighted sum of finished phases plus the
current phase's fraction (`bytes` when both byte values are known, else `done/total`). Without
phases, it is `done/total`. With neither, it falls back to the job's committed counts
(`rowsDone/rowsTotal`, then `chunksDone/chunksTotal`), so every existing durable job gets a percent
with no code change. A null percent means indeterminate; the UI then shows an indeterminate bar.

**Storage.** On `jobs_job`, as reportable columns: `current_phase`, `percent_done`,
`progress_message`, `bytes_done`, `bytes_total`, `eta_seconds`, plus `progress_json` (the full
snapshot) and `log_seq` (the newest log line's id). All additive and nullable.

### 2.2 Job log
New standard table **`jobs_log`**: `id`, `job_id`, `chunk_id`, `attempt`, `level` (`DEBUG`, `INFO`,
`WARN`, `ERROR`), `logged_at`, `phase`, `message` (max 1000 chars). Append-only: no update or delete
permission for anyone; retention deletes it with its job (qbit-jobs, `retentionDays`). It inherits
the job's record security lock through `job_id` and is a child list on the job record.

- **Capped.** At most `logMaxLines` per job (default 2000, in `DurableJobsMetaData`). Over the cap,
  the oldest INFO/DEBUG lines are pruned in batches, WARN/ERROR lines are kept, and
  `jobs_job.log_dropped` counts what was dropped (the UI says "N earlier lines not kept").
- **Redacted.** Every message goes through `JobLogRedactor` before it is written: values after
  `password|passwd|secret|token|api[_-]?key|authorization|credential|private[_-]?key` keys, bearer
  tokens, `AKIA...` keys, JWTs, URL userinfo, and long hex/base64 blobs become `[redacted]`; email
  addresses and phone/SSN-shaped numbers become `[redacted]` (no PHI). Messages are truncated and
  control characters stripped. The redactor is an SPI (`withLogRedactor`) so an app can add rules.
- **Throttled.** Lines are buffered per running chunk and flushed in one insert at most once a
  second, at a phase change, at WARN/ERROR, and at chunk end.
- **Cursor.** The log cursor is `jobs_log.id` (a sequence). Lines are written by one flusher per
  running chunk; parallel chunks (opt-in, rare) may commit slightly out of id order, so the poll
  response re-reads a short overlap window and the client dedupes by id.

### 2.3 Where writes go, and fencing
- Progress and log writes run in **their own short transaction**, never in a chunk's
  `SAME_TRANSACTION`, so they are visible live while the chunk's data is not yet committed.
- Every flush is **fenced**: it first renews the chunk lease with the chunk's `lease_token`
  (the existing heartbeat CAS). If the lease is gone (reclaimed; a zombie pod), the flush is dropped
  and the step is interrupted at its next page check, exactly like a lost heartbeat.
- On **reclaim** the reaper appends `WARN "Attempt N was interrupted (the server stopped); retrying"`
  (or "needs review"), and clears `current_phase`/`progress_message`. The next attempt re-reports
  its phases. `percent_done` is reset to the committed fraction, so a retried job never shows more
  done than is committed. Log lines from the interrupted attempt stay (they describe what happened)
  and carry their `attempt`.
- On finish, `JobFinalizer` sets `percent_done = 100` for `COMPLETED`, marks the running phase
  `DONE`/`FAILED`, and appends a final INFO or ERROR line with the summary.

### 2.4 Reporting API for processes and steps
```java
JobProgress progress = JobProgress.of(runBackendStepInput);   // never null
progress.phases(JobPhase.of("tables", "Back up tables", 70), JobPhase.of("media", "Copy media", 20),
                JobPhase.of("upload", "Upload manifest", 5), JobPhase.of("verify", "Verify", 5));
progress.phase("tables").total(48, "tables").bytesTotal(estimate);
progress.done(12).bytesDone(n).message("Backing up kof22_blog_post");
progress.info("Wrote kof22_blog_post: 1,204 rows, 2.1 MB");
progress.warn("Skipped 3 missing media files");
```
- **In a durable job** (a `JobChunkScope` is open) it writes the job's progress and log as above.
- **Outside a job** (inline or async process) it writes to the step's `AsyncJobCallback`:
  `message`, `current/total`, plus the new optional `AsyncJobStatus.progress` (the same snapshot)
  and an in-memory ring of the last 200 log lines. Inline runs therefore get the same rich view.
- **Adapters (existing code keeps working).** In a durable job the step's `AsyncJobCallback` is a
  `JobProgressAsyncJobCallback`: `updateStatus(message, current, total)` and `incrementCurrent` map
  to `message`/`done`/`total`. The streamed ETL execute step's per-page record counts already feed
  `rows_done`, which drives the fallback percent.

## 3. Detach and auto-background
- **Before start.** "Run in background" calls the existing `POST /processes/{name}/jobs`.
- **`alwaysDurable` processes** return `durableJobId` from the execute step at once (as built).
  The client shows the live view for that job; after ~5 s it moves the job to the tray ("Still
  running in the background - you can keep working") unless the user clicks Stay. Nothing on the
  server changes when the user leaves: the job is durable.
- **An inline async process that is already running** (not durable): `POST /jobs/track` with the
  `processName`, `processUUID` and `jobUUID`. Core creates a **tracking** `jobs_job` row
  (`source_type = INLINE`, status `RUNNING`, owner = caller, `lease_owner` = this pod). A per-pod
  `InlineJobTracker` mirrors the `AsyncJobStatus` (message, counts, progress, log ring) into the job
  every second and finalizes it from the process result. If the pod dies, the reaper finds the
  tracking row's pod gone and marks it `FAILED` with "The server restarted while this ran in the
  foreground; it was not a durable job, so it could not be resumed. Run it again." This is honest
  rather than crash-proof; processes that must survive a crash declare `alwaysDurable`.
- `qbit-backups` sets `alwaysDurable` on `backupRunPlan` (and restore and verify), so backups are
  crash-proof jobs and never inline.

## 4. UI API 1.4.0 (`/qqq/v1`)
All operations require a session, check `jobs_job.read` and apply the record lock (own jobs, or all
with `jobs_admin`). Errors: 404 for a job the caller cannot see (no existence leak), 409 for an
invalid transition.

| Operation | Purpose |
|---|---|
| `GET /jobs?scope=mine\|all&state=active\|recent\|all&limit=&processName=` | Tray and Jobs page list: `JobListResponseV1 {jobs: JobSummaryV1[], activeCount, generatedAt}`. `scope=all` needs `jobs_admin`. Recent = finished in the last 7 days, newest first. |
| `GET /jobs/{jobId}` | `JobDetailV1`: summary + `progress` (`JobProgressV1`) + summary lines + allowed `actions[]`. |
| `GET /jobs/{jobId}/progress?afterLogId=&logLimit=` | Poll: `JobProgressPollV1 {job: JobSummaryV1, progress, log: JobLogLineV1[], nextAfterLogId, logDropped, terminal}`. Cheap (two indexed reads). |
| `POST /jobs/{jobId}/cancel` | Cancel (the existing control path). |
| `POST /jobs/{jobId}/retry` | Retry failed chunks when there are any, else run again (a new job); returns the job id to open. |
| `POST /jobs/track` | Detach a running inline process into a tracking job: `{processName, processUUID, jobUUID}` -> `{jobId}`. |

**Live updates.** `GET /realtime/stream` already carries `job.status` and `job.progress` on
`user:{id}` and `record:jobs_job:{id}`. `job.progress` gains optional `currentPhase`,
`percent`, `message`, `bytesDone`, `bytesTotal` and `logSeq` (additive). Clients subscribe to
`user:me` for the tray, and on a `job.progress` whose `logSeq` advanced they fetch the log with
`afterLogId`. Without realtime they poll `/progress` (1 s while visible and running, 10 s for the
tray, paused while the tab is hidden). Process metadata already has `durableJobEligible` and
`durableJobAlways`; the process status response gains optional `progress` and `logTail` for inline
async runs.

## 5. Next UI (qqq-frontend-next)
- **Header Jobs button** next to the alerts bell: an icon with an active count badge; while jobs run
  the icon shows a slow, subtle ring animation (transform/opacity only; static under
  `prefers-reduced-motion`). The popover lists running jobs (label, phase, animated determinate
  mini bar, percent, ETA) and the recent finished/failed ones (status chip, duration, relative time);
  clicking one opens it. Footer: "All jobs".
- **Live job view** (replaces "Working / Executing Process"): title and status; a phase stepper
  (done ticks, the running phase pulsing gently); a determinate bar that eases between updates
  (CSS transition on `transform: scaleX`, never a jump); `percent`, `done/total unit`, bytes, ETA
  and elapsed; the message line; a collapsible live log (monospace, level colours, auto-scroll that
  pauses when the user scrolls up, Copy). Actions: Run in background, Cancel, and on finish Retry
  or Open result.
- **Auto-background** for `durableJobAlways` after ~5 s, with a toast and Stay.
- **Jobs page** `/app/jobs`: tabs Active / Recent / All (admin), the same rows, full width,
  readable 390-1440, no inner horizontal scroll, light and dark.
- Only `/qqq/v1` calls; Vitest, mocked e2e, screenshots at 390 and 1440 in light and dark.

## 6. qbit-backups
Phases: `prepare` (resolve scope), `tables` (N/M tables, bytes written), `media` (N/M files,
copied/deduplicated), `upload` (catalog, checksums, manifest), `verify` (when the plan verifies after
run). Log lines: one per table (name, rows, bytes, duration), media totals, upload keys (no bucket
credentials), verification result, and every warning. No row values; table names and counts only.

## 7. qbit-jobs and kof22
qbit-jobs registers `jobs_log` in its app (child of the job), adds the Jobs app to the ADMIN group
(Admin > Operations > Jobs), and its record panel uses the same progress view data. kof22 adopts the
RCs in Website-Backend#186 (DDL for the new columns and `jobs_log`), and the header tray appears for
every user with `jobs_job.read`.

## 8. Tests
- Unit: percent derivation (phases, weights, bytes, fallbacks, monotonic), ETA, redactor (every rule,
  no false positives on table names), log cap and pruning keeps WARN/ERROR, throttling.
- Adapters: `AsyncJobCallback` in a job writes progress; outside a job, `JobProgress` writes the
  async status.
- API: permission (own vs `jobs_admin`, 404 for others), poll cursor, cancel/retry transitions,
  track; contract test with the 1.4.0 bump.
- **Kill -9 and reclaim (Postgres, in CI):** a job writing phases and logs is killed mid-chunk; after
  reclaim and completion the log has attempt-1 lines, one reclaim line and attempt-2 lines, ids
  strictly increasing with no duplicates; the zombie's late flush is rejected; final percent is 100
  and counts equal the committed totals.

## 9. Phasing (one PR each)
1. qqq: this spec. 2. qqq: model, API, adapters, tests. 3. qbit-backups: phases and logs.
4. qqq-frontend-next: tray, live view, Jobs page. 5. qbit-jobs: `jobs_log` and admin placement.
6. kof22: adopt.
