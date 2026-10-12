# QQQ Backups qbit: Design Spec

Status: **Draft for owner review. Q7 decided by the owner 2026-10-12; the other questions are open.** · drafted 2026-10-12 · Author: James Maes (drafted with Claude)
Targets: QQQ 4.1.x (a small set of backend-agnostic core hooks, §5) and a new repo, `QRun-IO/qbit-backups` (Apache-2.0). It writes through **`qbit-object-storage`** (#1089, approved) and raises alerts through the **Alerts qbit** (in progress) when that is installed. The first consumer is kof22's Website-Backend: Postgres on CloudNativePG.
Related specs: object storage (#1089), alerts (in progress), CMS (#1083), CDN (#1084), Authentik (#1065).

> This spec is a design only. Nothing has been built, no repository has been created, and no storage has been provisioned. The owner questions are in §16. Storage goes through the approved object-storage qbit (QRun-IO/qqq#1089, decisions D1–D14), and §6.3 lists five small follow-ups it needs. Alerts go through the Alerts qbit's producer SPI (`docs/specs/2026-10-12-qbit-alerts-design.md`, in progress). §10.4 is reconciled with it once it is published.

## Owner decisions (2026-10-12)

These decisions override anything below that contradicts them. The sections they affect have been updated.

| # | Decision | Effect on the design |
|---|---|---|
| D1 (Q7) | **Files referenced by records are part of backups and exports.** Media, photos and any other S3 objects that records point at are copied into the content-addressed, deduplicated store by default. Reference-only stays available as an option. Restore brings files back, and verification checks file checksums. | `fileObjectPolicy` defaults to `COPY` for every scope (§4.1). File copy is a first-class part of the run (§8.5). There is a visible `backups_file_object` table (§4.8). Restore restores files by default (§9.3). Every verify level checks files (§10.1). File copy moves into P1–P3 (§14). |

## 0. Summary

`qbit-backups` gives any QQQ application **application-level, logical backups** of the data that QQQ manages. A backup can cover one table, a set of tables, an app's or a qbit's tables, or the whole instance. It is written as portable files to any S3-compatible object store, and it can be restored, verified and pruned from inside the app.

1. **Backup plans are records.** A plan says what to back up (tables, apps or qbits, with exclusions and optional filters), how (format, compression, encryption, sensitive-field policy, consistency), where (a storage connection from the object-storage qbit, plus a prefix), when (cron, run by the QQQ scheduler), and how long to keep it (keep N daily, weekly and monthly, with object lock where the provider supports it).
2. **Runs stream every table, and the files its records reference,** in primary-key order, using keyset paging inside a consistent read snapshot where the backend has one. Each table is written as **JSON Lines** (the default, lossless and restorable), **CSV** (for people and spreadsheets), or later **Parquet**. Files are gzip- or zstd-compressed and optionally encrypted on the client. A **manifest**, written last, records the schema fingerprint of every table, row counts, SHA-256 checksums, content hashes, the QQQ and app versions, and the consistency level actually achieved. If a run has no manifest, it did not complete.
3. **Restore** is a standard streamed ETL process with a real **Review** step. Review shows per table how many records would be inserted, updated, left unchanged or deleted, any conflicts and any schema mismatches, plus a sample field-level diff. Restore can target a whole backup, selected tables or selected records, in the same instance or table or a different one. The modes are **insert-new**, **upsert** or **replace**, with optional **new-ID mapping**. Sequences are resynced afterwards, and a pre-restore safety backup is taken by default.
4. **Verification is built in.** *Verify backup* re-reads every object and checks sizes, checksums and row counts. A scheduled **test restore** loads the backup into a scratch backend (for example a throwaway Postgres schema or database), compares counts and content hashes, and drops the scratch copy.
5. **Missed, failed and unverified backups are loud.** When the **Alerts qbit** is installed, backups raises alerts through its producer SPI, including **heartbeat monitors** that fire when a backup *didn't run*. A bucket-aware SLA checker runs independently of the backup job and compares the catalog with what is really in the bucket. Without the Alerts qbit, the same conditions show on the **Admin → Backups** home and in a `backups` health indicator. Prometheus gauges are an optional, secondary export.

This complements physical database backups and does not replace them. §1.3 explains the difference.

**Effort:** about **7–9 weeks** for one engineer working with Claude: P0 core hooks, then P1–P4 to a verified, restorable, alerting v1 (§14). P5 adds optional formats and incrementals.

---

## 1. Context

### 1.1 Goals

- Back up "all the data QQQ manages" without writing per-app scripts, whatever the backend: RDBMS, MongoDB, memory, filesystem or API.
- Use portable formats that any tool can read in ten years without QQQ: JSONL or CSV, plus a self-describing manifest.
- Make restore a first-class, previewed, permission-checked, audited operation, not a runbook.
- Prove that backups are restorable, continuously, and alert when they are missing, failed, stale or unverified.
- Follow the owner's qbit rules:
  - qbit data lives in standard, visible QQQ tables with full record screens, secured by permissions and record locks
  - processes run as table or record actions, never in the nav, with plain labels
  - actions use the standard streamed ETL Review/Result flow
  - one home per app owns its data, with no duplicate dashboards
  - only real data is shown
  - the UI is readable at every width
  - permissions follow the logged-in user.

### 1.2 Non-goals (v1)

- **Point-in-time recovery.** That is the job of physical backups and WAL archiving (§1.3).
- **Backing up the database server itself:** roles, extensions, tablespaces, non-QQQ tables, functions or triggers. An optional native `pg_dump` artifact (§8.4) covers this where it matters, but its restore is a DBA runbook, not a QQQ action.
- **Schema migration during restore.** Restore maps fields by name and reports mismatches. It does not run Liquibase or Flyway.
- **Being a general ETL or replication tool.** TableSync and the ESB already do that.
- **A built-in S3 client.** All object I/O goes through the object-storage qbit.

### 1.3 Logical (this qbit) vs physical backups

| | **Physical** (for example CloudNativePG barman-cloud base backups plus WAL archiving, `pgBackRest`, volume snapshots) | **Logical, application-level** (`qbit-backups`) |
|---|---|---|
| What it copies | The database cluster's files and WAL: every database, index, role and byte | The rows of QQQ tables, as QQQ sees them (field names, types, values), plus a metadata snapshot |
| Granularity of restore | The whole cluster, to any point in time within the WAL window | A whole backup, a table, a filtered set of records, or a single record |
| Restore target | A new cluster with the same major version | The same or another instance or table, another backend type, even another database engine |
| Portability | Engine- and version-specific | JSONL or CSV that any tool can read. Parquet and native dumps are optional. |
| Recovery point | Seconds (continuous WAL) | The last run (for example nightly) |
| Recovery time for a full disaster | Usually faster: replay files | Slower: insert row by row. Fine for small and medium apps. |
| Protects against | Losing the cluster, the volumes or the node | Bad deploys or migrations, an admin's mistaken bulk edit or delete, a logical corruption that physical backups faithfully copied, vendor or engine moves, legal exports, seeding staging with real (optionally scrubbed) data, a failed or unverified physical backup chain |
| Non-QQQ data, backends that aren't SQL | No and no | No and yes (MongoDB, memory, filesystem tables, readable API tables) |

**When to use which:**
- **Run both** for any production app.
- **Physical** is the first line for "the database is gone". Use it to restore to 14:32 yesterday.
- **Logical** is the first line for "someone deleted 300 blog posts last Tuesday", "restore just this table", "move this app to a new cluster or provider" and "prove we can restore". It is also the independent second copy when the physical chain is broken.
- Store them with **different providers and different credentials**, so that one compromised key or one provider incident cannot take out both.
- Physical backup tooling can silently skip or lose runs. For example, a catalog-maintenance bug in a backup plugin has been seen to delete completed backup objects after a partial listing, which made healthy backups look missing. The checks in this qbit look at **what is actually in the bucket**, not only at job records (§10.2).

---

## 2. Research summary (QQQ 4.1.x, `develop` at 56171e849)

These facts drive the core hooks in §5.

| Area | What exists | Gap for backups |
|---|---|---|
| Backend SPI | `QBackendModuleInterface` with Query/Count/Insert/Update/Delete/Aggregate/Storage interfaces. Modules: RDBMS (with Postgres, MySQL and SQLite strategies), MongoDB, API, filesystem (local, S3, SFTP), memory. `Capability` (`TABLE_QUERY` … `TABLE_EXPORT`) applies per backend and per table. | No consistent-snapshot API and no sequence API |
| Streaming | `RecordPipe` (a bounded queue with backpressure), `QueryHint.POTENTIALLY_LARGE_NUMBER_OF_RESULTS`, `MAY_USE_READ_ONLY_BACKEND` | Only MySQL actually streams (`fetchSize=MIN_VALUE`). **The Postgres JDBC driver buffers the whole result set**, because no fetch size is set and queries outside a transaction run in autocommit. There is no keyset or cursor paging anywhere; `skip`/`limit` is offset only. |
| Transactions | `QBackendTransaction`, `RDBMSTransaction` (`autoCommit=false`), shared across inputs through `withTransaction` | No isolation-level or read-only API |
| Query output | `QueryInput.shouldOmitHiddenFields` and `shouldMaskPasswords` both default to **true**, and `shouldFetchHeavyFields` defaults to false | A backup must turn all three off explicitly, under a permission check |
| Export | `ExportAction` and `ReportFormat` (CSV/TSV/JSON/XLSX). `includeSensitiveFields` defaults to false. | It always skips heavy (BLOB) fields. JSON keys are *labels*, not field names, and values are translated. It is not a restore format, so the qbit writes its own lossless JSONL writer. |
| Streamed ETL | `StreamedETLWithFrontendProcess`, with preview, review, validate, execute and result steps; `ProcessSummaryLine`; `transactionLevel` set to `page` or `process` | A good fit for backup, restore, verify and prune |
| Insert semantics | `InsertAction` honors a supplied primary key. `ReplaceAction` upserts by `UniqueKey`, with optional deletes scoped by a filter. `LoadViaInsertOrUpdateStep` splits on PK null or not. | Insert **always** overwrites create and modify dates (`DynamicDefaultValueBehavior`), always sets the automation status to pending, and always runs customizers. A faithful restore needs an opt-in flag (§5.3). |
| Sequences | IDs come only from generated keys or `RETURNING` | Nothing resyncs Postgres sequences after explicit-ID inserts (§5.4) |
| Metadata | `QInstanceAdapter.qInstanceToJson`, `QFrontendTableMetaData` | No stable schema fingerprint or diff (§5.5) |
| Scheduling | Quartz (clustered JDBC store, `@DisallowConcurrentExecution`) or the simple scheduler. `ScheduledJob` records. `AbstractRecordSyncToScheduledJobProcess` keeps a record in sync with a scheduled job (used by scheduled reports). | Scheduled processes run with `withPermissionBypass` as the system session. There are no per-job timeouts and no core run-log table. |
| Security | `PermissionLevel`, `QPermissionRules.permissionBaseName`, `CustomPermissionChecker`. `CorePermissionEnforcer` (#1061) can enforce permissions in core actions. `RecordSecurityLock` (with `LockScope.WRITE`), `FieldSecurityLock`, `isSensitive`, `isHidden`, and `PASSWORD`/`REVEAL` | Has what this needs |
| Audit | `AuditAction.execute(table, id, keys, message[, details])`, with record- or field-level DML audits | Has what this needs |
| Health and metrics | `qqq-middleware-health` `HealthIndicator`. ESB lifecycle events (`EsbProcessPublication`). | **No metrics registry in core** (no Micrometer or Prometheus) |
| Secrets and crypto | `${env.X}`, `SecretsManagerUtils` (AWS), `RefreshTokenCipher` (AES-256-GCM, versioned key ids) | No general key provider or streaming cipher |

---

## 3. Architecture

```
                         ┌──────────────────────────── qbit-backups (inside each QQQ app) ───────────────────────────────┐
 Scheduler (Quartz) ───► │ backupRunPlan (ETL)                                                                            │
 "Run backup now" ─────► │   ScopeResolver (plans, apps, qbits, table roles, filters) ─► ordered table list               │
                         │   ConsistencyCoordinator ─► core: openConsistentReadTransaction (§5.1)                         │
                         │   per table: KeysetPager (§5.2) ─► RowCodec (JSONL/CSV) ─► Compressor ─► [Encryptor]           │
                         │            ─► HashingOutputStream (sha256, rowsHash) ─► StorageService.openWrite  ────────┐    │
                         │   FileCopier (referenced S3 objects, content-addressed, server-side copy when possible)  │    │
                         │   [NativeDumpRunner: pg_dump --snapshot=<same snapshot>] ────────────────────────────────┤    │
                         │   ManifestWriter (written LAST = commit marker) ─────────────────────────────────────────┤    │
                         │                                                                                          │    │
 "Restore…" ───────────► │ backupRestore (ETL): read manifest ─► Review diff ─► load (insert-new/upsert/replace)    │    │
                         │   ─► core: restore-mode insert (§5.3) ─► core: resyncKeySequences (§5.4)                 │    │
 "Verify backup" ──────► │ backupVerify (ETL): STORAGE / CONTENT / TEST_RESTORE into scratch backend ───────────────┤    │
 "Prune per retention" ► │ backupPrune (ETL): GFS keep-set ─► delete (respects object lock) ────────────────────────┤    │
 SLA checker (5 min) ──► │ backupCheckSla: catalog ⟷ bucket listing ─► SLA status ─► Alerts qbit SPI / health / ESB │    │
 Admin → Backups home ◄─ │ backups_plan, backups_run, backups_artifact, backups_verification, …                     │    │
                         └──────────────────────────────────────────────────────────────────────────────────────────┼────┘
                                                                                                                    ▼
                                     object-storage qbit: StorageService (connections, capabilities, object lock, presign)
                                                                                                                    ▼
                                              any S3-compatible provider: AWS S3, MinIO, R2, B2, Wasabi, Spaces, Ceph RGW, Garage
```

### 3.1 Where code lives

| Piece | Location |
|---|---|
| Snapshot reads, keyset paging, restore-mode writes, sequence resync, schema fingerprint, the Postgres fetch-size fix | **QQQ core 4.1.x** (`qqq-backend-core`, `qqq-backend-module-rdbms`, `-postgres`, `-mongodb`). These are generic and useful beyond backups (exports, TableSync, data migration). Details in §5. |
| Tables, processes, codecs, manifest, scope resolver, crypto, scheduler sync, SLA checker, metrics endpoint, Admin → Backups app and widgets | New repo **`QRun-IO/qbit-backups`**, package `com.kingsrook.qbits.backups`, Apache-2.0. It follows the `qbit-template-application` / `qbit-quick-search` pattern: `BackupsQBitConfig implements QBitConfig`, `BackupsQBitProducer`, and table producers as `QBitComponentMetaDataProducer`. |
| Object I/O, connections, credentials, provider capabilities, Admin → Storage | **`qbit-object-storage`** (#1089). This qbit depends on it and never holds S3 credentials (§6). |
| Alert delivery, subscriptions, channels, heartbeat clocks | **Alerts qbit** (optional). Backups is a producer through `BackupAlertSink` (§10.4). |
| Optional Parquet writer | A separate module, `qbit-backups-parquet`, so the main artifact stays small (§8.3) |
| Offline tools | `qbit-backups-cli` (picocli): `verify`, `decrypt`, `to-csv` and `restore --dry-run` against a manifest, **with no running app**. This is the "read it in ten years" guarantee. P4. |
| Optional Prometheus rules | Example `PrometheusRule` YAML in the qbit's `docs/`, for the `absent()` backstop (§10.4). Each consumer's GitOps repo owns the real rules. |

### 3.2 Table roles (what "everything" means)

"The entire database" is ambiguous in a QQQ instance. Some tables are rebuildable caches, some are secrets, and some belong to the scheduler. Each table gets a **backup role**, declared as supplemental table metadata (`BackupTableMetaData`). Where a table doesn't declare one, the role is inferred by the rules below. Plans select by role.

| Role | Meaning | Default in a "whole instance" plan | How it is inferred when not declared |
|---|---|---|---|
| `DATA` | Business data | **Included** | Default |
| `AUDIT` | `audit`, `auditTable`, `auditUser` and `auditDetail`, and audit-like history | Included (configurable, §16 Q6) | Core audit table names |
| `DERIVED` | Rebuildable from other data: search indexes, rollups, caches (`QTableMetaData.cacheOf`) | Excluded | `cacheOf` set, or qbit-declared |
| `EPHEMERAL` | Sessions, idempotency keys, OAuth state, scheduler internals (`qrtz_*`), async job state | Excluded | Declared by core and qbit producers |
| `SECRET` | Refresh tokens, salts, credentials | **Never** backed up, even when explicitly selected | Declared, or a table that is hidden with every field `PASSWORD`/sensitive |
| `BACKUPS` | This qbit's own tables | Separate: the catalog is rebuilt from manifests (§9.6) | Self |
| `EXTERNAL` | API-backed tables (data owned by another system) | Excluded; can be selected | API backend |

Core and first-party qbits (sessions, Quartz, Quick Search, SQL Workbench, ESB, Assistant) should declare roles for their own tables. That is part of P0.

---

## 4. Data model (standard QQQ tables)

All tables:
- use the `backups_` prefix: lowercase snake_case, with the table name equal to the backend name (see **Naming** below)
- have `id`, `createDate` and `modifyDate`
- use **field-level audit**
- are in the app's configured backend (`getDefaultBackendNameForTables`)
- are role `BACKUPS`.

**Naming (owner rule, 2026-10-12).** "table names should always be lowercase and use _, and qbits should always be prefixed with their name, ie, cdn_ etc". Every table in this qbit is `backups_<name>` in lowercase snake_case, including the restore tables (`backups_restore_run`, `backups_restore_run_table`); the QQQ table name and the backend (SQL) name are identical, and possible-value sources take the table name. Other qbits' tables are referenced by their prefixed names (`object_storage_*`, `alerts_*`, `cdn_*`). Permissions use the full qbit name (`backups.*`, already compliant), and storage permissions are referenced as `object_storage.*` (owner, 2026-10-12). Field, process and Java class names are unchanged.

Process-written tables are read-only in the UI. Multi-tenant apps add the tenant key and a `RecordSecurityLock` to every table (§11.4). **No table ever stores a key, password or credential.** Tables hold only references, such as a key name or a storage connection name.

### 4.1 `backups_plan`

| Field | Type | Notes |
|---|---|---|
| `name`, `description` | STRING, TEXT | `name` unique |
| `planKey` | STRING | Slug used in object keys (unique, immutable after the first run) |
| `isActive` | BOOLEAN | Inactive plans are not scheduled and are excluded from SLA checks |
| `scopeType` | STRING (PVS) | `INSTANCE`, `APPS`, `QBITS`, `TABLES` |
| `includeRoles` | STRING (multi PVS) | Default `DATA,AUDIT` |
| `storageBucketAlias` | STRING (PVS over `object_storage_bucket` rows with role `BACKUP`) | Where the backup goes (§6.1) |
| `pruneBucketAlias` | STRING, optional | The same bucket through a delete-capable connection, used only by Prune (§11.5) |
| `prefix` | STRING | Defaults to `qqq-backups/<instanceName>` |
| `critical` | BOOLEAN | Critical plans raise higher-severity alerts and drive the health indicator |
| `format` | PVS | `JSONL` (default), `CSV`, `JSONL_AND_CSV`, `PARQUET` (P5) |
| `compression` | PVS | `GZIP` (default), `ZSTD`, `NONE` |
| `encryptionMode` | PVS | `NONE`, `CLIENT_AES256_GCM` |
| `encryptionKeyRef` | STRING | The *name* of a key in a key provider (§11.2), never the key itself |
| `sensitiveFieldPolicy` | PVS | `EXCLUDE` (default), `INCLUDE_ENCRYPTED_ONLY`, `INCLUDE` (§11.3) |
| `heavyFieldPolicy` | PVS | `INLINE` (default; base64 in JSONL), `SEPARATE_OBJECTS`, `EXCLUDE` |
| `fileObjectPolicy` | PVS | **`COPY` (default for every scope, including ad-hoc exports; owner decision D1)**, or `REFERENCE_ONLY` (§8.5) |
| `fileMissingPolicy` | PVS | `WARN` (default: the run ends `SUCCEEDED_WITH_WARNINGS` and raises an alert), or `FAIL` |
| `consistency` | PVS | `SNAPSHOT_REQUIRED`, `SNAPSHOT_PREFERRED` (default), `PER_TABLE` (§7) |
| `useReadReplica` | BOOLEAN | Use `MAY_USE_READ_ONLY_BACKEND` where configured |
| `incrementalMode` | PVS | `FULL` (default). `CHANGED_SINCE_LAST` is P5 (§8.6). |
| `nativeDump` | PVS | `NONE` (default), `PG_DUMP_CUSTOM` (§8.4) |
| `cronExpression`, `cronTimeZoneId` | STRING | Synced to a `scheduledJob` (§9.1). An empty value means a manual-only plan. |
| `scheduledJobId` | INTEGER | Read-only. Set by the sync. |
| `slaHours` | INTEGER | Maximum age of the last *successful* run before alerting. Default 26 for a nightly plan. |
| `verifySlaDays` | INTEGER | Maximum age of the last *passed* test restore. Default 8. |
| `verifyCronExpression`, `verifyLevel` | STRING, PVS | Scheduled verification, for example weekly `TEST_RESTORE` |
| `retainDaily`, `retainWeekly`, `retainMonthly`, `retainYearly`, `retainMinimum` | INTEGER | GFS retention (§10.3). Defaults 7/4/12/0/3. |
| `objectLockMode`, `objectLockDays` | PVS, INTEGER | `NONE`, `GOVERNANCE`, `COMPLIANCE`. Validated against the connection's capabilities. |
| `maxDurationMinutes` | INTEGER | Run timeout (§9.3). Default 240. |
| `pageSize`, `targetPartBytes` | INTEGER | Default 5,000 rows and 256 MiB |
| `ownerUserId` | STRING | Who the plan runs as for permission validation (§11.1) |
| `lastRunId`, `lastSuccessRunId`, `lastSuccessDate`, `lastVerifiedRunId`, `lastVerifiedDate`, `slaStatus` | — | Denormalized by processes for the home and for list screens. Read-only. |

Indexes: unique `name`; unique `planKey`; `(isActive, slaStatus)`.

### 4.2 `backups_plan_scope` (child of plan, association `scopes`)

One row per inclusion or exclusion, so the scope is visible and auditable as ordinary records.

| Field | Notes |
|---|---|
| `backupPlanId` | FK |
| `action` | `INCLUDE`, `EXCLUDE` |
| `selectorType` | `TABLE`, `APP`, `QBIT`, `ROLE` |
| `selectorValue` | A table name, app name, qbit name or role. A PVS lists the instance's tables, apps and qbits. |
| `filterJson` | An optional `QQueryFilter` (JSON), TABLE only. For example "only `status = PUBLISHED`", or `modifyDate >= NOW-90d` for an archive export. |
| `includeAssociatedChildren` | BOOLEAN (default true): also include tables reachable through the table's `associations` |
| `fieldExclusions` | Comma-separated field names to leave out (for example free-text PII in a scrubbed export) |

Resolution: start from `scopeType` and `includeRoles`, apply the INCLUDE rows, then the EXCLUDE rows. Then add associated children, and drop `SECRET` tables. Order the tables parents first, using the association and join graph (§9.5). The **Review** step of *Run backup now* lists the resolved tables. The resolved list is also stored on every run, so drift is visible.

### 4.3 `backups_run`

| Field | Notes |
|---|---|
| `backupPlanId` | FK, nullable (ad-hoc exports from a table action have no plan) |
| `runKey` | `20261012T020000Z-<shortId>`; used in object keys |
| `trigger` | `SCHEDULED`, `MANUAL`, `PRE_RESTORE`, `RESUME` |
| `requestedByUserId` | The logged-in user, or `system:scheduler` |
| `status` | `QUEUED`, `RUNNING`, `SUCCEEDED`, `SUCCEEDED_WITH_WARNINGS`, `FAILED`, `TIMED_OUT`, `CANCELLED`, `ABANDONED` (heartbeat lost) |
| `startDate`, `endDate`, `durationMs`, `heartbeatDate` | `heartbeatDate` is updated every page |
| `tablesPlanned`, `tablesDone`, `rowsTotal`, `bytesRaw`, `bytesStored` | |
| `filesReferenced`, `filesCopied` (new to the store), `filesDeduplicated` (already present), `filesMissing` (referenced but not found at the source), `fileBytesCopied`, `fileBytesReferenced` | File counts for the run (§8.5) |
| `consistencyAchieved` | `INSTANCE_SNAPSHOT`, `BACKEND_SNAPSHOT`, `PER_TABLE`, `BEST_EFFORT` |
| `snapshotId` | For example the Postgres exported snapshot id. Diagnostic only. |
| `storageBucketAlias`, `runPrefix` | Copied from the plan at run time; they are immutable |
| `manifestKey`, `manifestSha256` | Set when the manifest is written. **No manifest means the run is not usable.** |
| `qqqVersion`, `appVersion`, `instanceName`, `schemaFingerprint` | `schemaFingerprint` is a hash of all table fingerprints |
| `encryptionKeyRef`, `encryptionKeyFingerprint` | The fingerprint is the first 16 hex characters of SHA-256 of the key-encryption key id plus its version. Never the key. |
| `retentionClass` | `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`, `NONE` (computed at prune time) |
| `lockedUntil` | Object-lock retain-until date, if any |
| `prunedDate`, `prunedByUserId` | Set when the objects are deleted. The row is kept as history. |
| `verificationStatus` | `UNVERIFIED`, `STORAGE_OK`, `CONTENT_OK`, `TEST_RESTORE_OK`, `FAILED` |
| `warningCount`, `errorMessage`, `errorDetail` | The detail is a stack trace with values scrubbed |

Indexes: `(backupPlanId, startDate desc)`, `(status, heartbeatDate)`, unique `runKey`.

### 4.4 `backups_artifact` (child of run, association `artifacts`)

One row per stored object or part.

| Field | Notes |
|---|---|
| `backupRunId` | FK |
| `artifactType` | `TABLE_DATA`, `TABLE_BLOBS`, `FILE_OBJECTS_INDEX`, `NATIVE_DUMP`, `MANIFEST`, `CHECKSUMS` |
| `tableName`, `partNumber` | |
| `objectKey` | Relative to `runPrefix` |
| `format`, `compression`, `encrypted` | |
| `rowCount`, `bytesRaw`, `bytesStored` | |
| `sha256Stored` | Of the bytes in the bucket (after compression and encryption) |
| `sha256Raw` | Of the uncompressed, unencrypted content |
| `rowsHash` | The canonical content hash of the table's rows in PK order (§8.2). Set on the last part only. |
| `firstKey`, `lastKey` | Keyset bounds of the part; used for resume |
| `tableFingerprint` | §5.5 |
| `status` | `WRITING`, `COMPLETE`, `FAILED` |
| `storageVersionId` | If the bucket is versioned |

Indexes: `(backupRunId, tableName, partNumber)` unique; `(tableName)`.

### 4.5 `backups_verification`

| Field | Notes |
|---|---|
| `backupRunId` | FK |
| `level` | `STORAGE` (objects exist, sizes and checksums match), `CONTENT` (decode, re-hash and recount every part), `TEST_RESTORE` (§10) |
| `trigger`, `requestedByUserId`, `startDate`, `endDate` | |
| `status` | `PASSED`, `PASSED_WITH_WARNINGS`, `FAILED`, `RUNNING`, `ERROR` |
| `tablesChecked`, `rowsChecked`, `mismatches` | |
| `scratchTarget` | For example `postgres-scratch:schema=qqq_verify_<run>`, dropped afterwards |
| `detailJson` | Per-table expected and actual counts and hashes (also shown as a child list widget) |

### 4.6 `backups_restore_run`

| Field | Notes |
|---|---|
| `sourceBackupRunId` | FK. For backups imported from another instance, this is the imported catalog row (§9.6). |
| `requestedByUserId`, `startDate`, `endDate`, `status` | Same statuses as `backups_run`, plus `PREVIEWED` (the user stopped at Review) |
| `mode` | `INSERT_NEW`, `UPSERT`, `REPLACE` (§9.3) |
| `idStrategy` | `PRESERVE` (default), `REMAP_NEW_IDS` |
| `matchOn` | `PRIMARY_KEY` (default), or a named `UniqueKey` |
| `targetInstanceName` | Always the instance this is running in (restore runs where it writes) |
| `scopeSummary` | The selected tables and filters |
| `preRestoreBackupRunId` | The safety backup taken first (§9.4) |
| `rowsInserted`, `rowsUpdated`, `rowsUnchanged`, `rowsDeleted`, `rowsSkipped`, `rowsErrored` | |
| `filesRestored`, `filesAlreadyPresent`, `filesConflicting`, `filesMissingFromBackup`, `fileBytesRestored` | §9.3 |
| `sequencesResynced` | Count |
| `automationsSuppressed`, `esbSuppressed` | BOOLEAN options |
| `confirmationText` | What the user typed to confirm a REPLACE |
| `errorMessage` | |

### 4.7 `backups_restore_run_table` (child of backups_restore_run)

Per-table results: `tableName`, `targetTableName`, the counts above, `schemaStatus` (`MATCH`, `COMPATIBLE`, `MISMATCH`), `unmappedFields`, `sequenceResynced`, `status`.

### 4.8 `backups_file_object` (the content-addressed file store)

One row per stored file in `_files/sha256/…`, shared by all runs of the instance. This makes the file store a normal, visible table: searchable by hash, size or type, and linkable from runs and verification results.

| Field | Notes |
|---|---|
| `sha256` | Unique. It is also the object key (`_files/sha256/<ab>/<cd>/<sha256>`). |
| `sizeBytes`, `contentType` | From the source object |
| `storageBucketAlias`, `objectKey` | Where the copy lives |
| `encrypted`, `encryptionKeyFingerprint` | Set when the plan encrypts (§8.5) |
| `firstRunId`, `lastReferencedRunId`, `referenceCount` | `referenceCount` is the number of retained runs whose file index lists the hash. Prune maintains it. |
| `lastVerifiedDate`, `lastVerifyStatus` | From CONTENT and TEST_RESTORE verification |
| `lockedUntil`, `deletedDate` | Object lock, and when prune removed it |

Indexes: unique `sha256`; `(referenceCount, deletedDate)` for the prune sweep; `(lastVerifiedDate)`.

The **per-run file index** (`files/index-*.jsonl.gz`, an artifact of type `FILE_OBJECTS_INDEX`) holds one line per reference: table, record pk, field, source bucket alias and key, sha256, size and status (`COPIED`, `DEDUPLICATED`, `MISSING`, `REFERENCE_ONLY`). Index lines are not table rows, because a nightly run can reference hundreds of thousands of files.

### 4.9 `backups_id_map` (transient)

This is used only for `REMAP_NEW_IDS`. Fields: `restoreRunId`, `tableName`, `oldId`, `newId`. Index `(restoreRunId, tableName, oldId)`. Rows are deleted by the restore's final step, or garbage-collected after 7 days by `GarbageCollectorProcessMetaDataProducer`. They are visible while they exist, like every other qbit table.

---

## 5. Core hooks (QQQ 4.1.x)

These land in core because they are backend-specific, and other features need them too: exports, TableSync and data migrations. Each is opt-in and leaves existing behavior unchanged. **They land after the current release-candidate gate**, in the next 4.1.x.

### 5.1 Consistent read transactions

```java
// QBackendModuleInterface (default: unsupported)
default Optional<QBackendTransaction> openConsistentReadTransaction(ConsistentReadInput input)
// ConsistentReadInput: backendName, ConsistencyScope {BACKEND, TABLE}, readOnly=true, useReadReplica, Optional<String> importSnapshotId
// The returned transaction exposes Optional<String> getExportedSnapshotId()
```

| Backend | Implementation | Guarantee |
|---|---|---|
| **PostgreSQL** | `BEGIN ISOLATION LEVEL REPEATABLE READ, READ ONLY`, then `SELECT pg_export_snapshot()`. Parallel workers (and `pg_dump --snapshot`) attach with `SET TRANSACTION SNAPSHOT '<id>'`. | **Every table in the backend reflects one instant.** Caveat: a long snapshot holds back `xmin`, which delays vacuum and grows bloat. On a hot standby it can be cancelled by recovery conflicts unless `hot_standby_feedback` is on. Document it and use `maxDurationMinutes`. |
| **MySQL/MariaDB (InnoDB)** | `START TRANSACTION WITH CONSISTENT SNAPSHOT, READ ONLY` | One instant, for InnoDB tables. MyISAM tables are reported as `BEST_EFFORT`. |
| **SQLite** | `BEGIN DEFERRED` plus the first read, in WAL mode | One instant |
| **H2** (tests) | Same as RDBMS, with `REPEATABLE READ` | One instant |
| **MongoDB** | A session with `readConcern: snapshot`. It is bounded by `minSnapshotHistoryWindowInSeconds` (300 s by default). | One instant only if the whole run finishes inside the window. Otherwise it falls back to per-collection consistency and records `PER_TABLE`. |
| **Memory** | Copy-on-read under the module's lock | One instant |
| **Filesystem, S3, SFTP tables; API** | Not supported | `BEST_EFFORT`. Files are listed, then read, and API pages may shift. Recorded in the manifest. |

Across **different backends** there is never a shared instant. The manifest records the snapshot time per backend.

### 5.2 Keyset paging and true streaming

- **`KeysetPager`**, a core utility. It runs `ORDER BY <pk> ASC`, with `WHERE pk > :lastKey AND <filter>`, `LIMIT :pageSize`, over any backend that supports criteria and ordering on the primary key.
  - It is resumable from `lastKey`.
  - Its cost stays flat with depth, unlike `skip`.
  - It works inside the snapshot transaction from §5.1.
  - Composite keys are out of scope, because QQQ uses one PK field.
  - Tables whose PK is not orderable (rare) fall back to one streamed query.
- **Postgres fetch size.** When a `QueryInput` carries an RDBMS transaction (so autocommit is off) and the `POTENTIALLY_LARGE_NUMBER_OF_RESULTS` hint, `PostgreSQLRDBMSActionStrategy` sets `setFetchSize(n)` (default 1,000). This fixes whole-result buffering for **every** large export, not only backups. It is a small, separately shippable fix.
- **`QueryInput.withRawRead(RawReadReason)`** is a single opt-in that:
  - sets `shouldOmitHiddenFields=false`, `shouldMaskPasswords=false` and `shouldFetchHeavyFields=true`
  - skips possible-value translation and display values.

  It requires a permission check (`backups.run` or the caller's own `CustomPermissionChecker`) and is audited. Record locks and field locks **still apply** unless the session holds the all-access keys (§11.1).

### 5.3 Restore-mode writes

A new `ActionFlag`, **`RestoreWriteFlag`**, on `InsertInput`, `UpdateInput` and `ReplaceInput`. Only processes whose permission checker grants `backups.restore` may set it. `CorePermissionEnforcer` rejects it otherwise. When it is set:
- Supplied `createDate` and `modifyDate` values are **kept**: `DynamicDefaultValueBehavior` only fills nulls.
- The automation status is left as supplied, or set to `OK` instead of `PENDING_INSERT_AUTOMATIONS` (option `suppressAutomations`, default true).
- Pre- and post-insert customizers run only if `runCustomizers=true` (default false; restore is a data operation, not a business event).
- DML audit is **replaced** by one summary audit per table and restore, unless `perRecordAudit=true`.
- ESB record-change publication is skipped unless `publishEvents=true`.
- **These still run and cannot be turned off:**
  - record-security validation (`ValidateRecordSecurityLockHelper`)
  - field-security write checks (`FieldSecurityLockHelper`)
  - unique-key checks (except when the mode deliberately upserts)
  - required-field and type validation.

### 5.4 Sequence resync

```java
// QBackendModuleInterface (default: no-op)
default List<SequenceResyncResult> resyncKeySequences(QBackendMetaData backend, Collection<QTableMetaData> tables)
```

- **Postgres:**
  ```sql
  SELECT setval(pg_get_serial_sequence('<table>', '<pk col>'), COALESCE(MAX(<pk>), 1), MAX(<pk>) IS NOT NULL) FROM <table>
  ```
  This covers both `serial` and `GENERATED … AS IDENTITY` columns. When `pg_get_serial_sequence` returns null, the table has no sequence and is reported as "not applicable".
- **H2:** `ALTER TABLE … ALTER COLUMN … RESTART WITH max+1`.
- **MySQL and SQLite** advance on their own when explicit IDs are inserted, so they report "not needed".
- **MongoDB:** not applicable.

Restore calls this for every table it wrote with `PRESERVE` IDs, inside the restore transaction where the backend allows it.

### 5.5 Schema fingerprint

`TableSchemaFingerprint.of(QTableMetaData)` is a stable SHA-256 over:
- the table name, PK field and backend type
- the sorted fields: name, type, `isRequired`, `maxLength`, PVS name, `isHeavy`, `isSensitive`
- the unique keys and associations.

Labels, adornments and help text are excluded, so cosmetic changes don't count. `SchemaDiff` compares two fingerprints' underlying descriptors and returns added, removed and changed fields. Restore and verify use it, and anyone can use it for migration checks.

### 5.6 Streaming cipher and key provider (core `crypto` package)

- Generalize `RefreshTokenCipher`'s key handling into `QKeyProviderInterface`, with `getKey(keyRef)` returning key bytes plus a key id and version. The providers are:
  - `EnvKeyProvider`: base64 in an env var, which is how Infisical-via-ESO and Kubernetes secrets arrive
  - `AwsSecretsManagerKeyProvider`, through the existing `SecretsManagerUtils`
  - later, `AwsKmsKeyProvider`, for envelope encryption with KMS `GenerateDataKey`.
- The **streaming** format is Google **Tink** `StreamingAead` (`AES256_GCM_HKDF_1MB`, Apache-2.0). Each artifact gets a fresh data key, wrapped by the key-encryption key, and the wrapped key and key id are written in the object header and the manifest. **Keys never go into tables.** Tables hold only `encryptionKeyRef` and the fingerprint.
- Tink is chosen over a hand-rolled chunked GCM because segment authentication and truncation detection are easy to get subtly wrong.

### 5.7 Size

- §5.1: about 2 days
- §5.2: about 2 days (the Postgres fetch-size fix is half a day and can ship first)
- §5.3: about 2 days
- §5.4: about 1 day
- §5.5: about 1 day
- §5.6: about 2 days

**About 10 days total, including tests on Postgres, MySQL, SQLite, H2, MongoDB and memory.**

---

## 6. Storage: writing through the object-storage qbit (#1089)

The backups qbit never builds an S3 client. Every byte goes through `qbit-object-storage`, whose decisions D1–D14 are accepted (QRun-IO/qqq#1089). This section maps what backups needs onto that spec's service API (its §4) and lists the small additions the storage qbit needs.

### 6.1 Configuration: backup buckets are ordinary storage buckets

Connections and buckets come from **code config only** (storage D2). A plan therefore references a **bucket alias**, never an endpoint or a key:

```java
new ObjectStorageQBitConfig()
   .withConnection(new StorageConnectionConfig("backupWriter")      // put/get/list only (§11.5)
      .withProviderType(ProviderType.AWS_S3).withRegion("${env.BACKUP_S3_REGION}")
      .withAccessKeyRef("${env.STORAGE_BACKUP_WRITER_KEY}").withSecretKeyRef("${env.STORAGE_BACKUP_WRITER_SECRET}"))
   .withConnection(new StorageConnectionConfig("backupPruner")      // adds delete; used only by Prune
      .withProviderType(ProviderType.AWS_S3).withRegion("${env.BACKUP_S3_REGION}")
      .withAccessKeyRef("${env.STORAGE_BACKUP_PRUNER_KEY}").withSecretKeyRef("${env.STORAGE_BACKUP_PRUNER_SECRET}"))
   .withBucket(new StorageBucketConfig("backups").withConnection("backupWriter")
      .withBucketName("${env.BACKUP_BUCKET}").withRole(BucketRole.BACKUP).withIndexed(false))
   .withBucket(new StorageBucketConfig("backupsPrune").withConnection("backupPruner")
      .withBucketName("${env.BACKUP_BUCKET}").withRole(BucketRole.BACKUP).withIndexed(false));
```

`backups_plan.storageBucketAlias` is a PVS over `object_storage_bucket` rows with role `BACKUP`. `backups_plan.pruneBucketAlias` is optional; when it is empty, Prune uses the main alias and the plan shows the "one key can write and delete" warning (§11.5).

### 6.2 Operation mapping

| Backups needs | Storage API (#1089 §4.1) | Notes |
|---|---|---|
| Stream a part of unknown length | `bucket.put(PutRequest.of(key, stream).withContentType(…).withMetadata(…))` | Multipart above 16 MB, inside the connection's rate limit and concurrency (storage §7.5). Parts default to 256 MiB, well inside the 10,000-part limit. |
| Read a part (restore, verify) | `bucket.get(key, Range.all())` | Ranged reads are used to resume a verify mid-part |
| Existence and size (STORAGE verify, SLA check) | `bucket.head(key)` | |
| Listing (SLA checker, catalog import, debris) | `bucket.list(ListRequest.prefix(…).delimiter("/").pageSize(1000).after(token))` | Backup buckets are `indexed(false)`. The backups catalog is the index, so the storage object index does not have to hold millions of part keys. |
| Copy referenced files into the CAS store | `bucket.copy(CopyRequest.of(src, dst))` | Server-side when both aliases share a connection. Otherwise backups streams `get` into `put`. |
| Prune | `delete(key)` on the prune alias | §6.4 covers soft delete |
| Download | `bucket.presignGet(PresignGet.of(key).ttl(15 min).downloadAs(name))` | Signed with the storage qbit's least-privilege presign key (storage D14) |
| Capabilities | `object_storage_connection_capability` / `object_storage_bucket_capability` rows from **Test connection** | `OBJECT_LOCK`, `VERSIONING`, `LIFECYCLE`, `MULTIPART`, `CONDITIONAL_WRITE`. Plan save validates against these: an `objectLockMode` on a bucket whose `OBJECT_LOCK` isn't `SUPPORTED` is refused. |
| Errors | `StorageException.reason` | `THROTTLED` is retried with backoff. `ACCESS_DENIED` and `PROVIDER_ERROR` fail the part, with the provider request id on the run. |
| Write log | `object_storage_operation` | Every backup write, copy and delete appears there automatically, with the backups run id as the process run id |

**Integrity does not rely on the provider.** The storage qbit defaults `checksumMode` to `WHEN_REQUIRED` everywhere except AWS (storage §3.4), because many stores mishandle SDK checksums. Backups therefore computes its **own SHA-256** of every part while streaming, and the manifest and `backups_artifact` rows hold it. Verification re-hashes what it reads. Provider checksums are an extra check where they exist, never the only one.

### 6.3 Additions the storage qbit needs (small follow-ups to #1089)

| # | Addition | Why |
|---|---|---|
| SF1 | `BucketRole.BACKUP`: private; never public; excluded from header policy, orphan and missing scans | Backup objects aren't app media. They must not show as "orphaned" or get Cache-Control rewritten. |
| SF2 | `PutRequest.withObjectLock(mode, retainUntil)` and `withStorageClass(…)` | Object lock per object (§10.3). It is only offered when `OBJECT_LOCK` is `SUPPORTED`. |
| SF3 | A permanent-delete call for system callers, such as `delete(DeleteRequest.of(key).permanent())`, that deletes every version, gated by the caller's own permission check (here `backups.admin`) | Prune must free space. A delete marker in a versioned bucket would not (§6.4). |
| SF4 | Write probes skip `BACKUP` buckets with object lock on, or probe a `.qqq-probe/` prefix that is excluded from lock rules | A probe object in a locked bucket can't be deleted |
| SF5 | A system-caller context: `ObjectStorage.asSystem("backups", runId)` | Storage calls happen in a system context tagged with the backups run (§6.5) |

### 6.4 Soft delete and prune

Storage D4 soft-deletes by default: a delete marker when the bucket is versioned, and a copy to trash otherwise. For backups that would double storage and keep "deleted" backup data around. So:
- **Prune** uses the permanent delete (SF3), on the prune alias.
- **Object-locked** objects return a typed refusal. Prune reports "kept: locked until …" and retries next time.
- If the bucket is versioned for ransomware protection (recommended where object lock isn't available), a lifecycle rule expiring noncurrent versions after the lock window is the backstop. It is IaC-owned (storage D1).

### 6.5 Permissions across the two qbits

- **Backups permissions gate backups actions** (§11.4). Backups then calls storage in a **system context tagged with the run** (SF5), so a backups operator does not also need `object_storage.write` on the backup bucket, and `object_storage_operation` shows "system: backups run 1234 by user X".
- **Backup buckets are not browsable by ordinary storage users.** The storage qbit's per-bucket `RecordSecurityLock` (storage D7) gives the `BACKUP` bucket rows a key only backups admins hold. Otherwise anyone with `object_storage.view` could download full data dumps from Admin → Storage.
- A **storage protection rule** (`NO_DELETE` on the backups prefix, storage §6.7) blocks deletes from Admin → Storage. Only the backups Prune action, through SF3, removes backup objects.
- **Admin → Storage owns** bucket size, usage trend, connection health and capabilities for the backup bucket. Admin → Backups links to it and does not repeat those numbers (§12).

### 6.6 Object lock by provider

The storage qbit's probe is authoritative. Its seed table (storage §3.3) gives:
- **Supported:** AWS S3, MinIO (bucket created with lock), Wasabi, Ceph RGW
- **Unsupported:** R2 through the S3 API, Garage
- **Probe decides:** B2, DO Spaces.

Cloudflare R2's **bucket locks** are configured through the Cloudflare API or wrangler, outside S3. When a plan targets R2, the plan record says "retention enforced by bucket rule (IaC)" and backups sets no per-object retention.

---

## 7. Consistency guarantees

The plan's `consistency` setting combines with what the backend supports to give the run's `consistencyAchieved` value, and every table's level is recorded in the manifest.

| Setting | Behavior |
|---|---|
| `SNAPSHOT_REQUIRED` | Fail before writing anything if any backend in scope cannot open a consistent read (§5.1), or if a MongoDB run outlives its snapshot window |
| `SNAPSHOT_PREFERRED` (default) | Use a snapshot per backend where possible. Otherwise fall back per table and record a warning (`SUCCEEDED_WITH_WARNINGS`). |
| `PER_TABLE` | Each table is read in its own short read-only transaction. This avoids long snapshots on busy OLTP databases, but cross-table references can be torn (a child row pointing to a parent written after the parent's table was read). Restore Review flags orphans. |

On Postgres the whole run normally takes one exported snapshot. Tables can then be exported **in parallel** (`parallelism`, default 2), on separate connections attached with `SET TRANSACTION SNAPSHOT`. An optional `pg_dump` (§8.4) uses the **same** snapshot, so the logical files and the native dump are consistent with each other.

---

## 8. Formats and layout

### 8.1 Object layout

```
<prefix>/<instanceName>/<planKey>/<yyyy>/<mm>/<dd>/<runKey>/
    manifest.json                      ← written LAST; the commit marker
    checksums.sha256                   ← "sha256  relative/key" for every object (sha256sum -c compatible)
    tables/<tableName>/part-00001.jsonl.gz[.tink]
    tables/<tableName>/part-00001.csv.gz           (if CSV is selected)
    tables/<tableName>/blobs/<pk>/<field>.bin      (heavyFieldPolicy=SEPARATE_OBJECTS)
    files/index-00001.jsonl.gz         ← table, record pk, field, source connection/bucket/key, sha256, size, cas key
    native/pg_dump.custom[.tink]       (optional)
<prefix>/<instanceName>/_files/sha256/<ab>/<cd>/<sha256>   ← content-addressed copies of referenced files (shared across runs)
```

- **Date partitioning** keeps listings cheap and makes provider lifecycle rules readable.
- Parts are cut at `targetPartBytes` (256 MiB by default) or 1M rows. That bounds memory for restore and allows parallel verify.
- Object user metadata includes `qqq-backup-run`, `qqq-backup-table`, `qqq-backup-sha256-raw`. Objects describe themselves even without the catalog.

### 8.2 JSON Lines (default, restorable)

- One JSON object per line, ordered by primary key. Keys are **field names** (not labels), and only raw values are written (no display values or possible-value labels).
- The type encoding is canonical:

| `QFieldType` | JSON encoding |
|---|---|
| STRING, TEXT, HTML, PASSWORD | string |
| INTEGER, LONG | number |
| DECIMAL | **string**, `BigDecimal.toPlainString()`, so no float rounding |
| BOOLEAN | `true` or `false` |
| DATE | `"yyyy-MM-dd"` |
| TIME | `"HH:mm:ss[.SSSSSS]"` |
| DATE_TIME | ISO-8601 instant in UTC, `"2026-10-12T02:00:00.123456Z"` |
| BLOB | base64 string, or `{"$ref":"blobs/…","sha256":…,"size":…}` |
| null | `null`. Absent fields mean excluded by policy and are listed in the manifest. |

- **The `rowsHash` content hash** is SHA-256 over the canonical lines, in PK order, with sorted keys and a `\n` after each line. A verifier recomputes it from the bucket (CONTENT) or from a scratch restore (TEST_RESTORE). If two hashes match, the data is the same, whatever the compression, encryption or part boundaries.
- Associations are **not nested**. Each associated table is its own file set, which keeps restore streaming and order-independent within a table. The manifest records the association and join graph used for restore ordering.

### 8.3 Other formats

- **CSV** (optional, alongside or instead of JSONL):
  - RFC 4180, UTF-8 without a BOM, a header of field names, and the same canonical value strings.
  - BLOBs are always written as references, never inline.
  - CSV is for people and spreadsheets. Restore accepts it, but JSONL is the format guaranteed to be lossless (CSV can't tell null from an empty string unless the `\N` convention is on; it is on by default and stated in the manifest).
- **Parquet** (P5, the `qbit-backups-parquet` module):
  - `parquet-java` with its non-Hadoop `OutputFile` API, Snappy or Zstd, with types mapped from `QFieldType` (DECIMAL as a logical decimal with the field's precision, if declared).
  - It makes backups queryable in DuckDB, Athena or Spark.
  - It is optional because of its dependency weight.
- **Compression:** gzip (JDK built-in) by default. zstd (`zstd-jni`) is optional and is faster with better ratios for large plans.

### 8.4 Native `pg_dump` (optional, Postgres only)

- `nativeDump=PG_DUMP_CUSTOM` runs `pg_dump --format=custom --snapshot=<exported id> --no-owner --no-privileges` as a subprocess and streams stdout into `openWrite`. The client binary must exist in the image and match the server's major version. That is checked at plan save and run start, and the plan fails with a clear error otherwise.
- The dump captures what QQQ doesn't model: other schemas, functions, extensions, constraints and indexes.
- **Restore of a native dump is not a QQQ action.** It is a documented `pg_restore` runbook. *Verify* at level `CONTENT` runs `pg_restore --list` to prove the archive is readable.
- Off by default. Apps on CloudNativePG already have physical backups, and keeping two Postgres-native formats rarely pays off.

### 8.5 Heavy fields and referenced files

- **Heavy or BLOB fields:**
  - `INLINE` writes base64 in the JSONL (simple, fine below about 1 MB per value).
  - `SEPARATE_OBJECTS` writes each value as its own object, referenced from the row.
  - `EXCLUDE` drops it, and the manifest says so.
- **Files referenced from records:** QQQ has no "S3 key" field type. Backups reuses the **storage qbit's `StorageReferenceProvider` registrations** (#1089 §6.4, for example `FieldReferenceProvider.keyPrefix("files", "media", "storageKey")`), so an app declares its file references once, for orphan detection, delete protection and backups alike. Storage-backed tables (filesystem or S3 backend tables) are discovered automatically. Only references from tables in the plan's scope (and its filters) are followed.

  **Files are part of the backup by default (owner decision D1).** With `fileObjectPolicy`:
  - **`COPY` (default, every scope):** after a table's rows are written, the run walks the references from that table's rows (in the same snapshot) and makes sure each file is in the **content-addressed store** `_files/sha256/…`:
    1. It looks the source object up with `head`. Where the reference provider already knows the hash (for example a content-addressed key, or a stored checksum column), that is used. Otherwise the object is streamed once and hashed.
    2. If a `backups_file_object` row with that SHA-256 exists, the file is **deduplicated**. Only the index line is written. So a nightly run copies only new or changed media, and a 10,000-photo library costs one copy, not one per night.
    3. If not, it copies the object: server-side (`copy`) when the source and the backup bucket share a connection and the plan doesn't encrypt; otherwise it streams `get` into `put`, through the encryptor when the plan encrypts. It re-hashes while streaming and records the hash it actually observed.
    4. Copies run with bounded concurrency (default 4) within the storage connection's rate limits.
  - **`REFERENCE_ONLY` (option):** records the key, size and ETag from a `head` in the file index, without copying. Restore can then say which files are missing, but can't bring them back. It suits apps whose files are already protected elsewhere (for example a versioned, replicated, object-locked bucket).
  - **Missing files** (referenced by a row but gone at the source) are listed in the index with status `MISSING` and counted in `filesMissing`. Under `fileMissingPolicy=WARN` the run ends `SUCCEEDED_WITH_WARNINGS` and raises `backups.files.missing`. They are often a real data bug worth knowing about.
  - **Consistency:** source files are read after the rows that reference them, so a file replaced between the row read and the copy is captured as it is at copy time. Its hash in the index is what was actually stored. Content-addressed source keys, which most media pipelines use, make this moot.
  - **Encryption:** when the plan encrypts, file copies are encrypted too, and dedup still works because the store is keyed by the plaintext hash. The hash of each file is therefore visible in the bucket. This is accepted, and documented, because it only proves that a known file is present.
  - **Prune:** the file store is mark-and-sweep. Prune recomputes `referenceCount` from the indexes of retained runs and permanently deletes (SF3) unreferenced, unlocked objects. So a deleted photo stays restorable for as long as any retained backup references it.
- **Exports:** the ad-hoc **Back up these records** export and CSV plans copy referenced files the same way, so an export of a table of photos includes the photos.

### 8.6 Incremental (P5)

- `CHANGED_SINCE_LAST` exports rows whose `modifyDate` is at or after the previous successful run's snapshot time, minus a 5-minute overlap.
- **Deletes are not visible to a `modifyDate` filter.** An incremental run therefore also writes the **full PK list** of each table (cheap: one key column), so restore can rebuild the exact row set.
- Restoring an incremental needs its chain: the last full run plus each incremental after it. The manifest records `baseRunKey` and `previousRunKey`, and prune never breaks a retained chain.
- Tables without a modify-date field are always exported in full.
- The v1 recommendation is **full only**. Most QQQ apps are small enough that a full nightly run is cheaper than the complexity (§16 Q5).

### 8.7 Manifest (`manifest.json`)

```jsonc
{
  "manifestVersion": 1,
  "runKey": "20261012T020000Z-7f3a",
  "plan": { "planKey": "nightly", "name": "Nightly full", "settings": { /* format, compression, policies, consistency */ } },
  "instance": { "name": "…", "appVersion": "…", "qqqVersion": "4.1.x", "qbits": [ { "name": "…", "version": "…" } ] },
  "startedAt": "…", "finishedAt": "…", "requestedBy": "system:scheduler",
  "consistency": { "achieved": "INSTANCE_SNAPSHOT", "perBackend": [ { "backend": "postgres", "type": "postgres", "level": "SNAPSHOT", "snapshotTime": "…" } ] },
  "encryption": { "mode": "CLIENT_AES256_GCM", "scheme": "tink-streaming-aead-AES256_GCM_HKDF_1MB", "keyRef": "backups-kek", "keyFingerprint": "…" },
  "tables": [ {
      "name": "blogPost", "role": "DATA", "backend": "postgres", "primaryKey": "id",
      "fingerprint": "sha256:…", "schema": { /* fields with name, type, required, maxLength, pvs, sensitive, heavy; unique keys; associations; joins */ },
      "filter": null, "excludedFields": [ { "name": "secretNote", "reason": "SENSITIVE_POLICY" } ],
      "rowCount": 1234, "rowsHash": "sha256:…", "keyRange": { "min": 1, "max": 1400 },
      "parts": [ { "key": "tables/blogPost/part-00001.jsonl.gz", "rows": 1234, "bytesStored": 456789, "sha256Stored": "…", "sha256Raw": "…" } ],
      "consistency": "SNAPSHOT"
  } ],
  "restoreOrder": [ "author", "blog", "blogPost", "blogPostTag" ],
  "files": { "policy": "COPY", "indexKeys": [ "files/index-00001.jsonl.gz" ], "objects": 396, "bytes": 812345678, "missing": 0 },
  "nativeDump": null,
  "warnings": [ ],
  "totals": { "tables": 42, "rows": 98765, "bytesStored": 123456789 }
}
```

Writing the manifest is the run's commit. If a run dies before then, the SLA checker and prune treat its prefix as debris (§10.3). The manifest is also small enough to encrypt or not, independently of the data (`encryptManifest`, default false). Table names and counts are rarely sensitive, and leaving them readable keeps the catalog import and offline tooling simple.

---

## 9. Processes (table and record actions only)

All of these are standard `StreamedETLWithFrontendProcess` flows with standard Review and Result summary lines. None is in the nav. Labels are plain.

| Process | Label | Where | Permission | Review shows | Result shows |
|---|---|---|---|---|---|
| `backupRunPlan` | **Run backup now** | Record action on `backups_plan`; also run by the scheduler | `backups.run` | Resolved tables (count; sample list), estimated rows from `count`, excluded tables by reason, the sensitive-field policy and encryption, destination, the consistency the backends can give, and warnings (for example "pg_dump client not found") | Tables, rows, bytes, duration, consistency achieved, warnings; links to the run and its artifacts |
| `backupExportTable` | **Back up these records** | Table and bulk action on any table the user can read, when the qbit is enabled for the table | `backups.run` plus read on the table | The selected records or the current filter, format, destination | An ad-hoc `backups_run` (no plan) restorable like any other |
| `backupRestore` | **Restore…** | Record action on `backups_run`; **Restore this table** on `backups_artifact` (table parts); **Restore these records** from a filtered list of a table's backup rows (§9.3) | `backups.restore` plus insert/edit (and delete for REPLACE) on each target table | §9.3 | Per-table counts, sequences resynced, the pre-restore backup link, errors with record links |
| `backupVerify` | **Verify backup** | Record action on `backups_run`; also scheduled per plan | `backups.run` | Level, objects and bytes to read (and the egress estimate), the scratch target for TEST_RESTORE | Passed or failed per table, mismatches |
| `backupDownload` | **Download** | Record action on `backups_artifact` and `backups_run` (the manifest plus `checksums.sha256` plus a presigned link list) | `backups.restore` (§16 Q13) | What will be downloaded, whether it is encrypted, link TTL (15 min) | Presigned links. Each issue is audited. |
| `backupPrune` | **Prune per retention** | Record action on `backups_plan`; table action on `backups_run`; scheduled daily | `backups.admin` | The runs that will be deleted, kept by class (daily, weekly, monthly), kept because locked until a date, CAS objects to sweep, bytes freed | Deleted runs and objects, lock-skipped objects |
| `backupCancelRun` | **Cancel** | Record action on a RUNNING `backups_run` or `backups_restore_run` | `backups.run` or `backups.restore` | — | Cooperative cancel at the next page (`AsyncJobCallback.wasCancelRequested`) |
| `backupResumeRun` | **Resume** | Record action on a FAILED or TIMED_OUT `backups_run` with `PER_TABLE` consistency | `backups.run` | Tables and parts already complete; where each table resumes (`lastKey`) | As for a run. Snapshot runs cannot resume across a new snapshot; they say so and offer a fresh run. |
| `backupImportCatalog` | **Import backups from storage** | Table action on `backups_run` | `backups.admin` | The manifests found under a backup bucket alias and prefix that aren't in the catalog yet | `backups_run` and `backups_artifact` rows created from the manifests (§9.6) |
| `backupTestDestination` | **Test destination** | Record action on `backups_plan` | `backups.admin` | — | Write, read, list and delete of a probe object (head only on locked buckets). Capability report (object lock, versioning, checksums). |
| `backupCheckSla` | — (scheduled only, every 5 min) | — | system | — | Updates `slaStatus`, metrics, health, events (§10.2) |
| `backupSyncPlanSchedules` | — (table customizer) | — | — | — | Keeps `scheduledJob` rows in sync with plan cron fields (§9.1) |

### 9.1 Scheduling

- **Plans own their schedule.** A `BackupPlanSyncToScheduledJobProcess` extends core `AbstractRecordSyncToScheduledJobProcess`, the scheduled-reports pattern. It creates, updates or deletes one `scheduledJob` per active plan (`foreignKeyType=backups_plan`), and a second job for `verifyCronExpression`. Plans stay the single editable source, and the scheduled job shows up in the standard scheduled-jobs screens.
- **The scheduler is QQQ's.** In clustered Quartz, `@DisallowConcurrentExecution` plus the JDBC job store mean one node runs a trigger.
- **There is one run per plan at a time.** Before starting, the run inserts a `backups_run` in `QUEUED`, then flips it to `RUNNING` with a conditional update (`WHERE planId=? AND NOT EXISTS RUNNING with heartbeat newer than 10 min`). That is a database-level lease, independent of the scheduler. A second trigger while one is running records a `SKIPPED_CONCURRENT` warning event; it is not silent.
- **Instance-wide concurrency:** `BackupsQBitConfig.maxConcurrentRuns` (default 1) queues extra runs.
- **Timeout:** `maxDurationMinutes` is checked every page, and the run ends `TIMED_OUT`. A run whose heartbeat is more than 10 minutes old is marked `ABANDONED` by the SLA checker (pod killed or OOM).
- **Scheduled runs and permissions:** see §11.1.

### 9.2 Large tables

- Keyset pages (§5.2) of `pageSize` rows go through a bounded `RecordPipe`, so memory is O(page), not O(table).
- Writes are multipart uploads through `openWrite`, so the run never touches a temp disk.
- Parts rotate at `targetPartBytes`.
- Tables run in parallel on Postgres with the shared snapshot (§7).
- Progress is reported through `AsyncJobCallback.updateStatus("blogPost: 120,000 / 1,400,000", current, total)`, and the run row is updated every page.

### 9.3 Restore

**Inputs (standard process fields, first screen):**
- **Source:** the run (from the record action), plus a table multi-select limited to the tables in its manifest. Optionally a filter per table, for selected records.
- **Target:**
  - The same table (default).
  - Another table with the same or a compatible fingerprint. Fields map by name, unmapped fields are listed, and incompatible type changes block the restore.
- **Mode:**
  - `INSERT_NEW`: insert rows whose match key does not exist; skip the others. **The safest; the default.** Use it to recover deleted records.
  - `UPSERT`: insert or update by match key (`ReplaceAction` without deletes). Use it to roll back edits.
  - `REPLACE`: within each selected table **and filter**, delete target rows that aren't in the backup, then upsert. The user must type the table name (or `REPLACE <n> TABLES`) to confirm. It requires `backups.admin` as well as `backups.restore`.
- **Match on:** the primary key (default) or a declared unique key, for restores into a different instance whose IDs differ.
- **IDs:**
  - `PRESERVE` (default): insert with the backup's IDs, then resync sequences (§5.4).
  - `REMAP_NEW_IDS`: let the target assign IDs and write `backups_id_map`. Foreign-key fields in later tables (from QQQ joins and PVS references: a field whose PVS or join points at a remapped table) are rewritten through the map, in restore order. Fields that can't be traced through metadata are listed in Review as "unmapped references" so a human can decide.
- **Options:**
  - `suppressAutomations` (default on), `publishEvents` (default off), `runCustomizers` (default off), `perRecordAudit` (default off)
  - **`restoreFiles` (default on when the backup copied files).** Target: the original bucket alias and key (default), or another bucket alias and prefix.
  - **`preRestoreBackup` (default on)**

**Review (the preview, with no writes):** For each table the qbit streams the backup rows and the matching target rows (by key, in pages), and computes standard summary lines:
- "1,234 records will be inserted"
- "56 will be updated"
- "7,890 are unchanged"
- "12 will be deleted (REPLACE)"
- "3 conflict on unique key `slug`"
- "2 reference a parent that is missing from the backup and the target"
- "schema: 1 field in backup not in target (`legacyFlag`) will be skipped"
- "sequences will be resynced on 9 tables"

A **sample diff** widget shows up to 20 changed records per table, field by field (old → new), with sensitive values masked even here. Full validation (`doFullValidation`) is on by default, so Review covers every row, not a sample. For very large tables the user can switch to sampled Review, and the Result then shows the real counts.

**Files in Review:** for each referenced file of the selected rows: "N files will be restored", "N already present with the same checksum (skipped)", "N exist at the target with **different** content" (conflicts: kept unless the mode is `REPLACE`, and listed with record links), and "N are missing from the backup" (they were `MISSING` or `REFERENCE_ONLY` at backup time).

**Execute:**
1. Pre-restore backup of the target scope (including its files) (a `backups_run` with `trigger=PRE_RESTORE`, linked on the restore).
2. Tables in **restore order**, parents first: the manifest's topological order over associations and joins. Cycles are broken by inserting with nullable references unset, then updating them in a second pass.
3. Restore-mode writes (§5.3) in `page` transactions, so a huge restore doesn't hold one transaction for hours. `REPLACE` deletes and its upserts for a table share one transaction where the backend supports it, so a table is never left half-empty.
4. Sequence resync.
5. File restore, if selected. Each file goes from the store back to its target key through the storage API (in a system context tagged with the restore; the storage `RecordSecurityLock` and protection rules still apply to the *target* bucket). It is decrypted if needed, and **re-hashed on write and compared with the SHA-256 from the index**; a mismatch fails that file and is listed in the Result. The storage qbit's `StorageChangeListener` fires as for any write, so for example the CDN qbit purges restored media.
6. One summary audit per table, plus the `backups_restore_run` and `backups_restore_run_table` rows.

**Into a different instance:** the target instance installs the qbit, adds the same storage connection (read-only credentials are fine), and runs **Import backups from storage** (§9.6). It then restores from the imported run. Restore always runs *in* the instance it writes to, so the target's permissions, locks and validation apply.

### 9.4 Safety rails

- Restore refuses to run while a backup of the same tables is RUNNING, and the other way round.
- Plans with `REPLACE` scopes over a whole instance require `backups.admin` and a second confirmation that names the instance.
- The pre-restore backup is on by default, and turning it off is recorded on the `backups_restore_run`.
- Restores never touch `SECRET` or `BACKUPS` tables, or `EPHEMERAL` tables (unless selected explicitly with a warning).

### 9.5 Restore ordering

The ordering comes from:
- `Association` (parent before child)
- `QJoinMetaData` with a many-to-one type (the "one" side first)
- PVS fields whose PVS is table-backed.

The resulting DAG is stored in the manifest. When a database-level FK exists that QQQ doesn't know about and the insert fails, the error names the table and the qbit retries that table after the others (one extra pass) before reporting.

### 9.6 Catalog rebuild ("the database with the catalog is gone")

The catalog tables live in the database being backed up. After a total loss, the catalog is gone with it. Every manifest is therefore self-describing. **Import backups from storage** lists `<prefix>/<instanceName>/*/…/manifest.json`, validates each manifest's checksum, and recreates `backups_run` and `backups_artifact` rows (`trigger=IMPORTED`). The flow after a disaster is:
1. Restore the database physically, or create an empty schema with migrations.
2. Start the app.
3. Import the catalog.
4. Restore.

The offline CLI (§3.1) can do step 4 without the app if it has to.

### 9.7 Download

- The action issues **presigned GET URLs** (15-minute TTL) through the storage qbit for the selected artifacts, plus the manifest and checksums.
- Encrypted artifacts download as ciphertext. The Result screen links to the CLI's `decrypt` instructions. The app never decrypts to a browser download, because that would put plaintext full-table data on a laptop with one click.
- Each URL issue is audited, with the user, the artifact and the expiry.

---

## 10. Verification, SLA and alerting

### 10.1 Verify levels

| Level | Reads | Proves | Default schedule |
|---|---|---|---|
| `STORAGE` | `head` on every object, **including every file-store object the run's file index references** | Every object exists with the expected size. Where the provider stores SHA-256 checksums, they match the manifest. | After every run (cheap) |
| `CONTENT` | Every byte | Decrypts, decompresses and parses; row counts, `sha256Stored`, `sha256Raw` and `rowsHash` match; `pg_restore --list` succeeds for native dumps. **Every referenced file-store object is decrypted and re-hashed, and must equal its SHA-256 key.** Files already verified in the last `fileReverifyDays` (default 30) are skipped, so the cost stays bounded as the store grows; `backups_file_object.lastVerifiedDate` tracks this. | Weekly, or after every run for small plans |
| `TEST_RESTORE` | Every byte, plus writes to scratch | Restores every table (`PRESERVE` IDs, `INSERT_NEW`) into a **scratch target**, re-reads it in PK order, and compares counts and `rowsHash` per table. Sequences resync. **Restores the run's files into a scratch prefix** (`qqq-verify/<runKey>/` in the backup bucket, or a configured scratch bucket alias), re-hashes each one, and checks every scratch row's reference resolves to a restored file. `verifyFileSamplePercent` (default 100) can limit this for very large media stores. Then drops the scratch and deletes the scratch files. | Weekly (`verifySlaDays=8`) |

**Scratch targets** (`BackupsQBitConfig.scratchTargets`):
- **`RDBMS_SCHEMA`:** a configured scratch backend whose dedicated database user may `CREATE SCHEMA`/`DROP SCHEMA` only for `qqq_verify_*`. The qbit:
  1. creates `qqq_verify_<runKey>`
  2. builds each table there, generating a DDL-only copy of the source tables from QQQ metadata (columns by type, PK, unique keys), or, if a native dump exists, from `pg_restore --schema-only`
  3. restores into it through a temporary in-memory `QTableMetaData` clone pointing at the scratch schema
  4. drops the schema.
- **`SEPARATE_DATABASE`:** the same flow against a separate database or cluster. This is the strongest isolation, and it is recommended for production (§16 Q11).
- **`MEMORY`:** QQQ's memory backend. It suits small apps and CI. It proves the files and codecs, not the database.

**Results** go to `backups_verification` and update `backups_run.verificationStatus` and `backups_plan.lastVerified*`. A failure is an alert, not a log line.

### 10.2 SLA checker (independent of the job)

`backupCheckSla` runs every 5 minutes. It is a scheduled process, separate from the backup trigger, so a broken trigger cannot also silence the check. For each active plan it computes `slaStatus`:
- `OK`: the last success is younger than `slaHours`, its manifest **exists in the bucket** (a `head` on `manifestKey`, cached for 1 h), and verification is within `verifySlaDays`
- `LATE`: no success within `slaHours` (covers skipped triggers, scheduler down, runs stuck in QUEUED)
- `FAILING`: the last N=2 runs failed
- `MISSING_IN_STORAGE`: the catalog says success but the manifest is gone. That means someone or something deleted it, the credentials or bucket changed, or a lifecycle rule misfired.
- `UNVERIFIED`: the last passed verification is older than `verifySlaDays`
- `ABANDONED`: there is a RUNNING run with a stale heartbeat.

The checker also lists the plan's prefix once a day. It reports **orphans** (run prefixes in the bucket that aren't in the catalog) and **debris** (prefixes with no manifest older than 24 h). This is the "look at the bucket, not the job" lesson from physical backup tooling.

### 10.3 Retention and prune (GFS)

- **Retention rules:**
  - Keep the newest `retainDaily` successful runs, one per calendar day.
  - Also keep the newest run of each of the last `retainWeekly` ISO weeks, the last `retainMonthly` months and the last `retainYearly` years.
  - Always keep at least `retainMinimum` successful runs, **always keep the newest run that passed verification**, and never delete a run referenced by a retained incremental chain or a `backups_restore_run` from the last 30 days.
  - Debris older than 7 days is always deleted.
- **Object lock:** when the plan sets `objectLockMode` and the connection supports it, every object gets `retain-until = now + objectLockDays`. `objectLockDays` should equal the daily window, for example 7–14 days. Long-term runs (weekly and monthly) could be extended with `PutObjectRetention` when they are classed (P3, only if the provider supports extension). Prune skips locked objects, reports "kept: locked until …", and retries on the next prune.
- **Provider lifecycle rules** (expire noncurrent versions, abort incomplete multipart uploads after 2 days) are recommended as a backstop. They are configured through the storage qbit or IaC, never relied on as the only retention.

### 10.4 Signals and alerting

**The primary alert path is the Alerts / Manage-by-Exception qbit** (`docs/specs/2026-10-12-qbit-alerts-design.md`, in progress), when it is installed. Backups is one of its **producers**. Subscriptions, channels (in-app, email, webhooks, Slack optional), routing, acknowledgement and escalation all belong to the Alerts qbit; backups only produces.

- **Binding.** At startup, backups detects the Alerts qbit (class and metadata presence) and binds a small `BackupAlertSink` adapter to its producer SPI. Without the Alerts qbit the sink is a no-op. Every condition stays visible on the Admin → Backups home, in the tables and in the health indicator, and nothing depends on alerts being delivered. This is the same pattern the storage qbit uses (#1089 §9.3).
- **Heartbeat monitors ("the backup didn't run").** For each active plan, backups registers a **heartbeat monitor** with the Alerts qbit: expected every cron interval, with a grace of `slaHours`. Every successful run **checks in**, but only after the manifest is written and its `head` succeeds. If the trigger never fires, the scheduler is down or the pod never starts, the *Alerts qbit's* clock raises `backups.plan.missed`, not code inside the backup job. If the Alerts SPI has no heartbeat primitive in its final form, the SLA checker (§10.2) raises `backups.plan.missed` itself. Either way the check is independent of the backup trigger.
- **Alerts raised.** Each one has a stable dedupe key (type + plan, or type + run), counts, and a link to the plan, run or filtered table. Never values, keys or presigned URLs.

| Alert | Raised when | Resolves when | Default severity |
|---|---|---|---|
| `backups.plan.missed` | No successful run within `slaHours` (heartbeat overdue) | The next successful run checks in | error (critical plans: critical) |
| `backups.run.failed` | A run ends FAILED, TIMED_OUT or ABANDONED | The next successful run of the plan | warning; error after 2 in a row |
| `backups.run.missing_in_storage` | The catalog says success, but the manifest is gone from the bucket | A later run succeeds and its manifest is present, or an admin acknowledges | critical |
| `backups.files.missing` | A run found referenced files missing at the source | A later run finds none | warning |
| `backups.verify.failed` | A verification (any level) fails, including any file checksum mismatch | A later verification of a run of the same plan passes | error |
| `backups.verify.overdue` | No passed verification within `verifySlaDays` | A verification passes | warning |
| `backups.plan.owner_invalid` | The plan owner no longer exists or lost `backups.admin` (§11.1) | The owner is fixed | warning |
| `backups.storage.debris` | Prefixes without a manifest older than 24 h, or orphan run prefixes | Prune or import clears them | info |
| `backups.restore.completed` | A restore finished (any status). An info record, so restores are never silent. | Auto, after 7 days | info (error if it failed) |

- **Thresholds** (`slaHours`, `verifySlaDays`, `critical`) live on `backups_plan` rows, as ordinary audited data.

**Other signals (secondary):**

| Signal | Mechanism |
|---|---|
| **Admin → Backups home** | The tables (§12). Always present. |
| **Health** | A `BackupsHealthIndicator` (`qqq-middleware-health`). It reports `DOWN` if any plan marked `critical` is `LATE`, `FAILING` or `MISSING_IN_STORAGE`, and `UP` with details otherwise. It is off the liveness path by default (it is reported, but does not fail `/health`), so a backup problem never restarts pods. Admin → Health shows one status card that links to the Backups home. |
| **ESB** | When the ESB is present: `qqq.backups.run.succeeded`, `.failed`, `.restore.completed`. For integrations, not alerting. Fire-and-forget, so a failed publish never fails a backup. |
| **Prometheus (optional, off by default)** | Core has no metrics registry (§2). For operators who already run Alertmanager, `withPrometheusEndpoint(true)` exposes `GET /metrics/backups` (text format, computed from the tables, cached 60 s). The gauges, all labeled `{instance_name, plan}`: `qqq_backup_last_success_timestamp_seconds`, `qqq_backup_last_run_failed`, `qqq_backup_sla_seconds`, `qqq_backup_last_verify_success_timestamp_seconds`, `qqq_backup_last_success_bytes`. Example rules ship in `docs/`. **The rule worth having even alongside the Alerts qbit** is `absent_over_time(qqq_backup_last_success_timestamp_seconds[30m])`. It is the one signal that still fires when the whole app, and so the Alerts qbit with it, is down. It is a cluster-level backstop, not the primary path. |

---

## 11. Security

### 11.1 Who a backup runs as

- **Manual actions follow the logged-in user.** *Run backup now*, *Back up these records*, *Restore*, *Verify* and *Download* run in the user's session.
  - Core enforcement (#1061) checks `backups.*` plus the user's **read** permission on every table in scope (for backup) or **write** permission on every target (for restore).
  - Record locks and field locks apply. A user without all-access keys gets a backup **of what they can see**, and the manifest marks it `securityScope: USER` with the user id.
  - A `USER`-scoped run is flagged as **not a full backup**, and it does not satisfy a plan's SLA.
- **Scheduled runs** need a full-instance read, so they run as the scheduler's system session. Scheduled processes already use `withPermissionBypass`.
  - To keep "permissions follow the user" meaningful, the plan stores `ownerUserId`. **Saving or activating a plan re-checks** that the saving user has `backups.admin` and read access to every table the plan resolves to.
  - The SLA checker re-validates daily that the owner still exists and still has `backups.admin`. If not, the plan's status becomes `OWNER_INVALID` and it alerts. It doesn't silently keep running under a departed admin.
  - The manifest records `securityScope: FULL`, `runAs: system:scheduler` and `planOwner`.
- **Restore never runs scheduled**, except as a TEST_RESTORE into scratch.

### 11.2 Encryption

- **In transit:** TLS, enforced by the storage qbit. Plain HTTP endpoints are refused unless the connection is marked `allowInsecureForLocalTesting`.
- **At rest at the provider:** SSE (SSE-S3 or the provider default) is recommended and set by the storage connection.
- **Client-side (`CLIENT_AES256_GCM`):**
  - Tink streaming AEAD, with a fresh data key per object, wrapped by a key-encryption key from `QKeyProviderInterface` (§5.6).
  - The key comes from a secret manager (Infisical through ESO into an env var, AWS Secrets Manager, later KMS). **Key material never appears in a table, a log, a manifest or an audit.** Only `keyRef`, the key version and the fingerprint do.
  - Rotation:
    1. Add a new key version and make it current for writes.
    2. Keep old versions readable until every run that uses them has been pruned.
    3. The home shows "runs still needing key version N".
  - **Losing the key loses the backups.** The plan screen says so, and *Test destination* verifies that the current key decrypts a probe.

### 11.3 Sensitive and secret data

| Data | Policy |
|---|---|
| `isSensitive` fields | `sensitiveFieldPolicy`:<br>`EXCLUDE` (default): the field is left out and the manifest records it.<br>`INCLUDE_ENCRYPTED_ONLY`: the plan can't be saved without `CLIENT_AES256_GCM`.<br>`INCLUDE`: requires `backups.admin` plus a typed acknowledgement, recorded in audit.<br>Ad-hoc table backups (`backupExportTable`) use `EXCLUDE` unless the user has `backups.admin`. |
| `PASSWORD` fields | Treated like sensitive fields. Never unmasked unless the policy includes sensitive data. |
| `FieldSecurityLock` fields | Read through the session's keys. With a `USER` scope, fields the user can't see are absent, and the manifest records that. |
| `SECRET`-role tables | **Never** exported (tokens, salts, credentials) |
| Review and diff screens | Sensitive values are always masked, even for admins. Restore writes them, but never shows them. |
| Logs and errors | Values are never logged. Errors cite table, pk and field only. |

### 11.4 Permissions and locks

| Permission | Allows |
|---|---|
| `backups.view` | The Admin → Backups app, every backups table (read-only), the home |
| `backups.run` | Run backup now, Back up these records, Verify, Cancel and Resume a backup |
| `backups.restore` | Restore (INSERT_NEW and UPSERT), Download, Cancel restore |
| `backups.admin` | Edit plans (scope, destination, encryption, sensitive policy, retention, lock), Prune, REPLACE restores, Import catalog, Test destination |

- The permissions are implemented as `QPermissionRules` with `permissionBaseName` `backups` and a `CustomPermissionChecker` per process, the same pattern as `quickSearchAdmin` and `esbOperate`.
- `backups_run`, `backups_artifact`, `backups_verification`, `backups_restore_run`, `backups_restore_run_table` and `backups_id_map` are written only by processes. Their fields are read-only in the UI, and delete is not offered (prune marks them instead).
- **Multi-tenant apps** add the tenant key and a `RecordSecurityLock` to every backups table, and a plan's scope is limited by the tenant key, so tenant admins see and restore only their tenant's backups.

### 11.5 Ransomware and blast radius

- **The writer credential** for the backup connection should be **put, get and list only**. Prune uses a separate **delete** credential, or relies on lifecycle rules plus object lock. Where the storage qbit supports credential roles, the plan validates this split and warns if a single all-powerful key is used.
- **Use a different provider or account from the physical backups**, so one leaked key or one provider incident can't remove both copies.
- **Object lock in GOVERNANCE mode** on the daily window (§16 Q14).

### 11.6 Audit

These all write audit entries through `AuditAction`, with the user, the plan or run, and a message:
- plan create, update and delete (field level)
- run start and finish
- every restore (a summary per table, the confirmation text, the pre-restore backup id)
- every verify
- every download link issued
- every prune (the runs and object counts deleted)
- import catalog
- each key-version change seen.

---

## 12. Admin → Backups app

A sub-app under Admin, like Quick Search, ESB and CDN. It exists only when the qbit is installed and enabled, and it needs `backups.view`. **Its home is the single owner of backup data.** Admin → Health shows only a status card (worst `slaStatus` and one line) that links here. **Storage-level data, such as bucket sizes, usage trend, connection health and provider capabilities, belongs to Admin → Storage** (#1089 §10.2). This home links to the plan's backup bucket record there and never repeats its numbers. **Alert history, acknowledgement and subscriptions belong to the Alerts qbit.** The Needs-attention card links to the open `backups.*` alerts when that qbit is installed.

### 12.1 Home cards (all real data; every card has an honest empty state)

| Card | Shows | Source | Empty state |
|---|---|---|---|
| **Plan status** | One row per active plan: last successful backup, **age against SLA** (a green, amber or red chip, for example "6 h of 26 h"), last run status, next scheduled run, verification age against `verifySlaDays`, lock mode. Each row links to the plan. | `backups_plan` (denormalized fields), `scheduledJob` | "No backup plans yet. Create one from Backup plans → New." |
| **Needs attention** | Plans that are LATE, FAILING, MISSING_IN_STORAGE, UNVERIFIED, ABANDONED or OWNER_INVALID; failed runs in the last 7 days; orphans and debris found in storage. Each item links to a filtered list. | `backups_plan`, `backups_run`, SLA checker | "Nothing needs attention." (shown only when real checks have run; otherwise "The SLA checker hasn't run yet.") |
| **Latest test restore** | For each plan: the last TEST_RESTORE result, tables and rows compared, mismatches, duration, the scratch target | `backups_verification` | "No test restore yet. Run Verify backup → Test restore on a run, or set a verify schedule on the plan." |
| **Size and duration trend** | 30 and 90 days per plan: bytes stored, rows, duration (line charts, with the numbers next to them) | `backups_run` | "Trends appear after the first successful run." |
| **Recent activity** | The last 10 backup, verify, restore and prune runs, with status and user | `backups_run`, `backups_verification`, `backups_restore_run` | — |
| **Retention** | For each plan: runs kept by class (daily, weekly, monthly, yearly), the oldest restorable point, the next prune, objects still locked | `backups_run` | "Retention starts once runs exist." |

### 12.2 Sections (standard tables with full record screens)

- **Plans:** `backups_plan` (with the `backups_plan_scope` child list), and links to the scheduled job and the storage connection
- **Runs:** `backups_run` (with the artifacts, verifications and restores as child lists), `backups_artifact`
- **Restores:** `backups_restore_run` (with `backups_restore_run_table`)
- **Verification:** `backups_verification`

Actions are the record and table actions from §9. **None of them is in the nav.**

### 12.3 UI rules

- Readable at every width. Cards stack to one column below 900 px, tables use full width, and long table names wrap.
- No inner horizontal scroll and no double scrollbars. Diff widgets wrap values and never scroll sideways.
- Screenshots at 6 widths are part of the P1 exit criteria.

---

## 13. Testing

- **Core hooks:** Testcontainers on Postgres 16 and 17, MySQL 8 and MariaDB, SQLite, H2, and MongoDB 7 (replica set).
  - The snapshot holds while a concurrent writer runs (a row inserted mid-run is absent from every table).
  - Keyset paging is equivalent to a full scan.
  - The Postgres fetch size keeps the heap bounded (1M rows under a 256 MB heap).
  - Restore-mode keeps dates.
  - The sequence resync means the next insert gets `max+1`.
- **Codecs:** round-trip property tests for every `QFieldType`, including DECIMAL precision, timezone instants, BLOB base64, null vs empty in CSV, and unicode. `rowsHash` is stable across part sizes and compression.
- **Storage:** a contract suite against **MinIO** (object lock on), **Garage** (no lock), and LocalStack S3. It covers multipart, checksum headers, locked-delete errors, presign and server-side copy. It uses the storage qbit's test harness.
- **End-to-end** on the sample app:
  1. Back up.
  2. Mutate (delete 10 rows, edit 10, add 10).
  3. Run Review for each mode and assert the summary lines.
  4. Restore.
  5. Assert the counts and hashes, and that the sequence is advanced.
  6. Run a TEST_RESTORE into a scratch schema.
  7. Prune with GFS fixtures (a synthetic clock).
  8. Import the catalog into a fresh instance and restore there.
- **Failure injection:** kill the pod mid-run (ABANDONED, then resume on PER_TABLE), a storage 503 during a part (retry, then fail), a deleted manifest (MISSING_IN_STORAGE alert), a wrong key (verify fails clearly), no `pg_dump` binary (plan validation error).
- **Security:** a user without `backups.restore` can't see Restore; a USER-scoped backup excludes locked rows; sensitive fields are absent under `EXCLUDE`; REPLACE without `backups.admin` is refused by the core enforcer; no secret appears in logs, which a log-scrape assertion checks.
- **UI:** screenshots at 6 widths; empty states for every card.

---

## 14. Packaging and phase plan

**Repo:** `QRun-IO/qbit-backups`, Apache-2.0, Maven `com.kingsrook.qbits:qbit-backups`. Optional modules `qbit-backups-parquet` and `qbit-backups-cli`. It depends on QQQ core 4.1.x (with §5) and on the object-storage qbit.

Estimates assume one engineer working with Claude.

| Phase | Scope | Estimate | Exit criteria |
|---|---|---|---|
| **P0: core hooks** (QQQ 4.1.x) | §5.1–5.6. The Postgres fetch-size fix ships first, on its own. Table-role declarations for core and first-party qbit tables. | 10–12 d | Merged to `develop` after the RC gate, with the cross-backend Testcontainers suite green |
| **P1: backup (rows and files)** | Repo from `qbit-template-application`. Tables §4.1–4.5. Scope resolver and roles. JSONL and CSV codecs, gzip. Manifest and checksums. Snapshot and keyset streaming. **Referenced-file copy into the deduplicated store, the file index and `backups_file_object`.** Plan → scheduledJob sync. Run backup now, Back up these records, Cancel. STORAGE verify. **Admin → Backups shell, with the home and honest empty states.** | 13–15 d | A sample app and kof22 staging run nightly backups, with referenced media copied and deduplicated, to MinIO. The home is readable at 6 widths. |
| **P2: restore** | Restore with Review (all three modes, PRESERVE and REMAP IDs, match on unique key), restore ordering, sequence resync, pre-restore backup, **file restore with checksum check**, catalog import, Download, restore tables §4.6, §4.7 and §4.9 | 10–12 d | The E2E restore suite is green. A deleted-records restore works on kof22 staging. |
| **P3: verify, SLA, alerts, retention** | CONTENT and TEST_RESTORE verify (`RDBMS_SCHEMA`, `SEPARATE_DATABASE`, `MEMORY`), the SLA checker (bucket-aware), **`BackupAlertSink` for the Alerts qbit (alerts plus per-plan heartbeat monitors)**, the health indicator, ESB events, GFS prune with object lock and permanent delete (storage SF3), the file-store mark-and-sweep, **file checksums at every verify level**. Optional `/metrics/backups` and example rules. | 8–10 d | Alerts qbit alerts fire in staging for missed, failed, missing-in-storage and unverified, and the home shows the same without the Alerts qbit. A weekly test restore passes. |
| **P4: encryption, CLI** | Tink encryption (data and file-store objects) plus key providers and rotation, sensitive-field policies, `qbit-backups-cli` (verify incl. files, decrypt, to-csv) | 5–7 d | An encrypted nightly backup with copied files is verified offline by the CLI |
| **P5: optional** | Parquet module, native `pg_dump`, `CHANGED_SINCE_LAST` incrementals with PK lists, zstd, parallel table export | 6–9 d | As needed |

**Total:** about **7–9 weeks** for P0–P4, plus 1.5–2 weeks for P5. P0's fetch-size fix and P1 can overlap once the snapshot and keyset APIs are agreed.

### 14.1 First consumer: kof22

kof22 is a Postgres-on-CloudNativePG QQQ app that already has physical backups: barman-cloud WAL archiving plus scheduled base backups to object storage. Adoption, with the details kept in the kof22 repositories:

1. **Annotate table roles.** Content, media metadata, users and authors are `DATA`. Audit tables are `AUDIT`. Search indexes and analytics rollups are `DERIVED`. Sessions, idempotency keys, Quartz and upload staging are `EPHEMERAL`. Tokens and salts are `SECRET`.
2. **Destination.** A **dedicated bucket at a different provider and account** from the CNPG backups, with object lock in GOVERNANCE mode for 14 days, a write-only writer credential, and a separate prune credential.
3. **Plan "Nightly full".** `INSTANCE` scope, roles `DATA,AUDIT`, JSONL with gzip, `CLIENT_AES256_GCM` (key from the existing secret manager through ESO), `sensitiveFieldPolicy=INCLUDE_ENCRYPTED_ONLY`, `fileObjectPolicy=COPY` (the default, per D1) for every referenced media and photo object, which also gives media an off-site, deduplicated copy, `SNAPSHOT_REQUIRED`, at 02:00 local time (well clear of the physical base backup window), `slaHours=26`, retention 14 daily, 8 weekly, 12 monthly.
4. **Weekly TEST_RESTORE** into a scratch database (§16 Q11). A plain `INSTANCE` restore drill once a quarter, into a staging namespace.
5. **Alerts.** Install the Alerts qbit and subscribe the owner to the `backups.*` alerts. Heartbeat monitors cover "the nightly backup didn't run". As a cluster-level backstop, optionally enable `/metrics/backups` with one `absent_over_time` rule, and add the physical backups' own age alerts there too. Physical backups have no alert rule today.
6. **Keep the existing physical backups** unchanged. Use the logical backups for table- and record-level recovery and as the independent second copy.

Size at kof22's scale (tens of tables, low millions of rows at most, media in the hundreds of MB): a nightly run should take minutes, and storage should cost cents to a few dollars a month.

---

## 15. Risks

| # | Risk | Mitigation |
|---|---|---|
| R1 | Long snapshots bloat Postgres or get cancelled on replicas | Parallel export shortens runs. `maxDurationMinutes`. `PER_TABLE` option. Read on the primary by default. Document `hot_standby_feedback` for replica reads. |
| R2 | Restore overwrites good data | INSERT_NEW is the default. Full Review with counts and diffs. A pre-restore backup by default. REPLACE needs `backups.admin` and a typed confirmation. Audit. |
| R3 | Backups contain secrets or PII and leak | SECRET tables are never exported. Sensitive fields are excluded by default or encrypted. Client-side encryption. Download only as ciphertext via short presigned URLs. Writer and prune credential split. |
| R4 | Encryption key lost, so backups are unreadable | Keys in a secret manager with its own backup. *Test destination* decrypt probe. Key-version usage on the home. The CLI decrypts with only the key and the bucket. |
| R5 | Silent misses (the scheduler is down, a trigger is lost, objects are deleted) | Heartbeat monitors in the Alerts qbit. An independent SLA checker that checks the bucket, not only the catalog. Health indicator. Orphan and debris reports. The optional `absent_over_time` rule for a dead app. |
| R6 | Restore order or FK surprises in databases with constraints QQQ doesn't know about | Topological order from metadata, plus a retry pass, plus clear errors. Test restore catches it weekly. |
| R7 | Schema drift between backup and target | Fingerprints and `SchemaDiff` in Review. Mapping by name. Incompatible types block the restore. |
| R8 | Restore triggers business side effects (emails, webhooks, ESB events) | Customizers, automations and ESB are suppressed by default (§5.3). Turning them on is explicit and recorded. |
| R9 | Large BLOBs or large media libraries blow up memory, time or cost | `SEPARATE_OBJECTS` policy, streaming base64, part rotation. Files are streamed, never buffered. The deduplicated store copies each file once. Bounded re-verification (`fileReverifyDays`) and the TEST_RESTORE sample setting. |
| R10 | Provider differences (object lock, checksums, copy) | Everything goes through the storage qbit's capability probe. The contract suite runs on MinIO, Garage and LocalStack. |
| R11 | Release interference | Core hooks land after the RC gate, behind opt-in flags. The fetch-size fix ships on its own. |
| R12 | The storage follow-ups (§6.3 SF1–SF5) or the Alerts SPI land late, or differ from this spec | SF1–SF5 are small and can land in storage P1–P2. Without them, backups uses role `PRIVATE` plus a protection rule, and lifecycle-based expiry for prune. Without the Alerts qbit, the home and health indicator still show every condition. |

---

## 16. Owner questions (with recommendations)

| # | Question | Options and recommendation |
|---|---|---|
| Q1 | **Repo and package** | **(a)** `QRun-IO/qbit-backups`, `com.kingsrook.qbits.backups`, Apache-2.0, plus optional `-parquet` and `-cli` modules. **Recommended.**<br>(b) Fold into the object-storage qbit. Rejected: different lifecycle and permissions. |
| Q2 | **Who scheduled runs run as** | **(a)** The system session, with plan-owner validation on save and a daily re-check (§11.1). **Recommended.**<br>(b) Run as the plan owner's real session. That honors locks, but makes "full backup" depend on one person's grants and breaks when they leave. |
| Q3 | **Default for `isSensitive` and `PASSWORD` fields** | **(a)** `EXCLUDE` by default. `INCLUDE_ENCRYPTED_ONLY` is available, and is required for any full-fidelity plan. **Recommended.**<br>(b) Include by default (full fidelity), relying on encryption. |
| Q4 | **Client-side encryption: required or optional?** | **(a)** Optional in general, but **required** whenever sensitive fields, audit tables or user tables are included, and on by default for new plans. Tink streaming AEAD; keys from the secret manager. **Recommended.**<br>(b) Always required.<br>(c) Rely on provider SSE only. |
| Q5 | **Incremental backups in v1?** | **(a)** No. Full nightly in v1; `CHANGED_SINCE_LAST` with PK lists in P5. **Recommended.**<br>(b) Ship incrementals in P1. |
| Q6 | **Audit tables in the nightly plan** | **(a)** Include them (they are the history, and restoring data without them loses provenance). If they dominate size, split them into a second weekly plan. **Recommended.**<br>(b) Exclude by default. |
| Q7 | **Referenced S3 files** | **Decided (owner, 2026-10-12), D1:** files referenced by records are included in backups and exports. `COPY` into the content-addressed, deduplicated store is the default for every scope (kof22 included). `REFERENCE_ONLY` stays as an option. Restore brings files back, and verification checks file checksums. |
| Q8 | **Native `pg_dump`** | **(a)** An optional P5 feature, off by default (CNPG apps already have physical backups). **Recommended.**<br>(b) On by default for Postgres. |
| Q9 | **Parquet** | **(a)** Defer to P5, as a separate module. **Recommended.**<br>(b) Include in P1. |
| Q10 | **Restore approval** | **(a)** Single user with `backups.restore`, a full Review, a pre-restore backup, and a typed confirmation for REPLACE (which also needs `backups.admin`). **Recommended for v1.**<br>(b) Two-person approval (request, then approve) for REPLACE and whole-instance restores, as a later add-on. |
| Q11 | **Test-restore target** | **(a)** A separate scratch database (same cluster is fine) with a dedicated role limited to `qqq_verify_*` schemas. **Recommended.**<br>(b) A scratch schema inside the production database. Simpler, but load and privileges sit on prod.<br>(c) The memory backend only. Proves the files, not the database. |
| Q12 | **Alerting path** | **(a)** The Alerts qbit is primary (producer SPI plus per-plan heartbeat monitors). Without it: home plus health indicator. An optional `/metrics/backups` endpoint exists only as an `absent()` backstop for "the whole app is down". **Recommended.**<br>(b) Also ship Prometheus gauges on by default.<br>(c) No Prometheus at all. |
| Q13 | **Download permission** | **(a)** `backups.restore` covers Download: full-data egress has the same sensitivity as restore. **Recommended.**<br>(b) A fifth permission, `backups.download`. |
| Q14 | **Object lock mode and window** | **(a)** GOVERNANCE, 14 days, where the provider supports it; R2 bucket locks where applicable. **Recommended.**<br>(b) COMPLIANCE: cannot be shortened, even by the root account.<br>(c) None. |
| Q15 | **Credential split** | **(a)** The writer has put, get and list only; prune uses a separate delete credential (or lifecycle rules). Warn when one key does both. **Recommended.**<br>(b) One credential. |
| Q16 | **Default retention** | **(a)** 7 daily, 4 weekly, 12 monthly, minimum 3, always keeping the newest verified run. kof22: 14, 8 and 12. **Recommended.**<br>(b) Days only (for example 30 days). |
| Q17 | **Core hook timing** | **(a)** Land §5 in the next 4.1.x after the RC gate. Ship the Postgres fetch-size fix first, because it helps every large export today. **Recommended.**<br>(b) Wait for 4.2. |
| Q18 | **kof22 destination provider** | Must differ from the CNPG physical backup provider and account. See the owner's report for the concrete options and recommendation. They are kept out of this public spec. |
| Q19 | **Storage follow-ups SF1–SF5 (§6.3) in `qbit-object-storage`** | **(a)** Add them to the storage spec as a short follow-up edit and land them in storage P1–P2: `BACKUP` bucket role, object lock on put, permanent delete for system callers, probe handling for locked buckets, system-caller context. **Recommended.**<br>(b) Backups works around them (role `PRIVATE`, lifecycle-only prune). |
| Q20 | **A "whole instance" plan that includes `EXTERNAL` (API-backed) tables** | **(a)** Excluded by default. Selectable, with a `BEST_EFFORT` warning. Restore into API tables is disabled unless the table is writable. **Recommended.**<br>(b) Never allowed. |

---

## 17. Sources

Researched 2026-10-12.

- QQQ source at `develop` 56171e849:
  - `QBackendModuleInterface`, `QueryInput`, `RecordPipe`
  - `RDBMSQueryAction.createStatement`, `RDBMSTransaction`, `PostgreSQLRDBMSActionStrategy`
  - `ExportAction`, `JsonExportStreamer`
  - `StreamedETLWithFrontendProcess`, `ReplaceAction`, `InsertAction`, `DynamicDefaultValueBehavior`
  - `TableSyncProcess`, `AbstractRecordSyncToScheduledJobProcess`
  - `QuartzJobRunner`, `SchedulerUtils`
  - `CorePermissionEnforcer`, `RecordSecurityLock`, `FieldSecurityLock`
  - `AuditAction`, `RefreshTokenCipher`, `QInstanceAdapter`, `qqq-middleware-health`
  - `qqq-esb` (`EsbProcessPublication`)
- PostgreSQL snapshot export and `SET TRANSACTION SNAPSHOT`: https://www.postgresql.org/docs/current/functions-admin.html#FUNCTIONS-SNAPSHOT-SYNCHRONIZATION , https://www.postgresql.org/docs/current/sql-set-transaction.html
- `pg_dump --snapshot`: https://www.postgresql.org/docs/current/app-pgdump.html
- Hot-standby conflicts: https://www.postgresql.org/docs/current/hot-standby.html#HOT-STANDBY-CONFLICT
- pgJDBC cursor-based fetching (requires autocommit off): https://jdbc.postgresql.org/documentation/query/#getting-results-based-on-a-cursor
- MySQL consistent snapshot: https://dev.mysql.com/doc/refman/8.0/en/commit.html
- MongoDB snapshot read concern and `minSnapshotHistoryWindowInSeconds`: https://www.mongodb.com/docs/manual/reference/read-concern-snapshot/
- S3 Object Lock: https://docs.aws.amazon.com/AmazonS3/latest/userguide/object-lock.html
- Additional checksums: https://docs.aws.amazon.com/AmazonS3/latest/userguide/checking-object-integrity.html
- Cloudflare R2 bucket locks: https://developers.cloudflare.com/r2/buckets/bucket-locks/
- Backblaze B2 Object Lock: https://www.backblaze.com/docs/cloud-storage-object-lock
- MinIO object locking: https://min.io/docs/minio/linux/administration/object-management/object-retention.html
- Google Tink streaming AEAD: https://developers.google.com/tink/streaming-aead
- JSON Lines: https://jsonlines.org/
- RFC 4180 (CSV): https://www.rfc-editor.org/rfc/rfc4180
- Apache Parquet Java: https://github.com/apache/parquet-java
- CloudNativePG backup and the barman-cloud plugin: https://cloudnative-pg.io/documentation/current/backup/ , https://cloudnative-pg.io/plugin-barman-cloud/
