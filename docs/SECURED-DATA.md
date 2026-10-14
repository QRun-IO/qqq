# Secured Data (field tiers, masking, reveal, break-glass)

Status: **Approved** (owner, 2026-10-13) · drafted 2026-10-13 · Author: James Maes (drafted with Claude)

Targets:
- **QQQ core 4.1.x** (`qqq-backend-core`, `qqq-middleware-javalin`): the field-access tiers,
  server-side masking and filter/sort enforcement, the resolver SPI, and the field metadata that
  tells clients each field's tier (§2–§4). This part is the contract every client and qbit relies
  on.
- **`QRun-IO/qbit-secured-data`** (Apache-2.0): the classification overrides, the policy, reveal
  grants, break-glass sessions, the access log, the Reveal and break-glass operations, and the app
  home (§5–§8).
- **QQQ Next UI** (`QRun-IO/qqq-frontend-next`): masked-value rendering, the Reveal dialog, filter
  builders that offer only the allowed operators, and the break-glass banner (§9).

Related work, which this design uses and does not duplicate:
- `DataClassification` (PUBLIC < CONFIDENTIAL < PII < PHI) and `AiValueMasker` (#1142). The
  assistant's PHI routing uses the same classification.
- `FieldSecurityLock` / `FieldSecurityLockHelper` (#1062): DENY is today's HIDDEN.
- UI API extension point (`UI-API-EXTENSIONS.md`) and client channels (`UI-API-CLIENT-CHANNELS.md`,
  #1141): the access log records the channel; the assistant reaches data only through `/qqq/v1`,
  so it receives masked values like any client.
- Table feature permissions (`TableFeaturePermissionsMetaData`): the per-table grants appear in the
  qbit-authentik policy editor through its catalog.
- Data analysis (`DATA-ANALYSIS.md` §2.4, #1146): maps FULL → FULL, REVEALABLE → EXACT_MATCH_ONLY,
  MASKED and HIDDEN → NONE, and keys its caches by the access fingerprint (§3.5).
- Alerts producer SPI (#1090) for reveal anomalies and break-glass notifications, optional.

> **Owner decisions (2026-10-12): all recommendations accepted.** Every open question in §12 is
> decided as recommended ("they are approved. we will adjust as needed once we can view them
> working").
>
> **Owner decisions (2026-10-13)**, from the request below (retention, wipe and audit protection
> are in §13–§15):
> - **Name:** qbit-secured-data. Tables `secured_data_*`, permissions `secured_data.*` and
>   `secured_data_*` table features.
> - **Classification** uses core's `DataClassification` on tables and fields. Code sets the
>   defaults; admin rows in `secured_data_classification` override them at runtime.
> - **Four tiers per role × classification:** FULL, REVEALABLE, MASKED, HIDDEN (§2).
> - **Reveal:** a self-serve "Reveal" with a reason (configurable list plus free text) shows one
>   field of one record for N minutes (default 5). Audited, with an optional daily cap and an
>   anomaly alert.
> - **REVEALABLE filtering is exact-match only:** EQUALS and IN on a value the user already knows;
>   results stay masked; no range, contains or sort; every such search is audited.
> - **Break-glass:** a time-boxed FULL grant for emergencies. It needs a reason, is loudly
>   audited, notifies admins through the bell and alerts, and expires automatically.
> - **View audit:** `secured_data_access_log` records views and reveals of restricted fields: who,
>   which record and field, when, why, the client channel and the outcome.

## Owner request (2026-10-13, verbatim)

> a phi or 'filtered data' / 'secured data' qbit. one that would allow us to mark a table or field
> as 'phi' or 'secure' and then treat it different, based on user permissioned. ie, an admin might
> still be able to see them and filter on them etc, but a customer service admin would have them
> 'blurred' and not visible by default, but maybe could 'allow' seeing them one off (and audited
> etc), and a cs rep would have strictly blurred only - ie, never able to see or filter on them etc.

## Mandatory standards that shape this design
- Data lives in standard, visible QQQ tables with full record screens, secured by permissions.
  The qbit's tables are `secured_data_*`; its own tables are classified CONFIDENTIAL and walled off
  from the assistant channel (they describe who looked at PHI).
- Processes are table/record actions with plain labels, never in the nav. Record actions use the
  standard streamed-ETL Review/Result summary.
- One app home that owns the data. The nav lists only the main tables.
- The API matches the admin; masking applies to `/qqq/v1` and to the QQQ API middleware alike.
- Readable at all widths, light and dark.

## 1. Architecture

```
            client (Next UI, CLI, iOS, assistant via UiApiInvoker, QQQ API)
                                     │  /qqq/v1  (and /api)
                                     ▼
   core actions: Query / Get / Count / Aggregate / Export / Report / RecordSearch / widgets
        │  checkReadCriteria ──► FieldAccessHelper.assertCriteriaAllowed   (403 on a disallowed filter/sort)
        │  results           ──► FieldAccessHelper.applyToRecords          (mask / remove values)
        │  table metadata    ──► FieldAccessHelper.applyToTable            (drop HIDDEN, tier per field)
        ▼
   FieldAccessHelper ── effective tier = most restrictive of
        ├─ FieldSecurityLock on the field (ALLOW=FULL, REVEAL, MASK, DENY=HIDDEN; override values → FULL)
        └─ FieldAccessResolverInterface (registered by qbit-secured-data; none = FULL)
                 ├─ classification: secured_data_classification row ▸ code (AiFieldMetaData / AiTableMetaData)
                 ├─ break-glass session (FULL)
                 ├─ table feature grants  <table>.secured_data_full / .secured_data_reveal
                 ├─ secured_data_policy rows matched by the session's permissions
                 ├─ the qbit's code defaults per classification
                 └─ reveal grants (isRevealActive) and the access log (onAccess)
```

Masking happens in core, server side, before values leave. A client never receives a value the
user may not see, so no client can "un-blur" it, and the assistant (a UI API client) gets the
same masked values as the user it acts for.

## 2. The tiers

| Tier | Value in responses | Field metadata | Filter | Sort, group, aggregate | Quick search | Export | Write |
|---|---|---|---|---|---|---|---|
| **FULL** | real | present | any operator | yes | matched | real | as table permission |
| **REVEALABLE** | masked; real for an active reveal of that record+field | present, `accessTier: REVEALABLE` | **EQUALS / IN only** (exact match), audited | no | not matched | masked | refused |
| **MASKED** | masked | present, `accessTier: MASKED` | no (403) | no (403) | not matched | masked | refused |
| **HIDDEN** | absent | absent | no (403) | no (403) | not matched | absent | refused |

- A **masked value** replaces both the value and the display value, using the field's mask style
  (§3.3). It is always a string, whatever the field type; clients use the field's `accessTier`
  to render it as masked rather than parse it.
- **Writes need FULL.** A non-FULL field is reported with `isEditable: false`; an insert or update
  that carries a value for it is refused with 403 ("you may not write to the field [X]"), exactly
  like a DENY lock today. Clients must not echo masked values back. (Recommended; see §12 Q1.)
- **Record labels** built from a non-FULL field are cleared, as for DENY today, so a label never
  carries a masked value; clients fall back to the id.
- The **primary key** is never restricted (records must stay addressable).
- **Joined and associated records** get the same treatment for their own tables' fields.
- **Possible values** whose label fields are non-FULL show ids only.
- **System sessions** (`QSystemUserSession`) and calls that are not being enforced (nested inside
  an authorized scope, or `PermissionEnforcement.SKIP`) see FULL. That is today's rule for locks;
  egress points that can carry records from such nested calls are covered in §3.4.

## 3. Core (qqq-backend-core, qqq-middleware-javalin)

### 3.1 API

Package `com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess`:

```java
public enum FieldAccessTier
{
   FULL, REVEALABLE, MASKED, HIDDEN;

   boolean canReadValue();                         // FULL
   boolean canFilter(QCriteriaOperator operator);  // FULL: any; REVEALABLE: EQUALS, IN; else false
   boolean canSort();                              // FULL
   boolean canGroupOrAggregate();                  // FULL
   boolean canQuickSearch();                       // FULL
   boolean canWrite();                             // FULL
   boolean isInMetaData();                         // all but HIDDEN
   FieldAccessTier mostRestrictive(FieldAccessTier other);
   FieldAccessTier leastRestrictive(FieldAccessTier other);
}

public enum FieldMaskStyle { AUTO, EMAIL, LAST_4, FIRST_LAST, FULL }

public record FieldAccessDecision(FieldAccessTier tier, FieldMaskStyle maskStyle, String reason, boolean exactMatchAllowed)
{
   public FieldAccessDecision(FieldAccessTier tier, FieldMaskStyle maskStyle, String reason) { this(tier, maskStyle, reason, true); }
}

public interface FieldAccessResolverInterface
{
   /** The tier of a field for the session in QContext. Called often: must be cheap (cache per session). */
   FieldAccessDecision resolve(QTableMetaData table, QFieldMetaData field);

   /** Whether the session holds an active reveal of this record's field (REVEALABLE only). */
   default boolean isRevealActive(String tableName, String fieldName, Serializable primaryKey) { return false; }

   /** Stable string for the session's effective access (changes when tiers can change). */
   default String getAccessFingerprint() { return ""; }

   /** Told about access to restricted fields, after enforcement (for the access log). Must not throw. */
   default void onAccess(FieldAccessEvent event) { }

   /** Before an exact-match query on a REVEALABLE field runs: throw QPermissionDeniedException to refuse (rate limit). */
   default void checkExactMatch(FieldAccessEvent event) throws QPermissionDeniedException { }

   /** A one-way fingerprint of a searched value (the qbit uses a keyed HMAC). Values never leave core otherwise. */
   default String fingerprintSearchValue(Serializable value) { return null; }
}
```

- `FieldAccessResolverMetaData` (`QSupplementalInstanceMetaData`, name `fieldAccessResolver`)
  holds one resolver (`QCodeReference` or instance). There is at most one; registering a second
  fails validation.
- `FieldAccessEvent`: `type` (VIEW, EXACT_MATCH_FILTER, DENIED), `outcome` (MATCH, NO_MATCH,
  REFUSED; exact-match only), `searchValueFingerprints`, `tableName`, `fieldNames`,
  `primaryKeys` (for GET and small results; capped, see §6.2), `recordCount`, `actionName`
  (query, get, export, report, search, process, widget), and never the values or filter values.
- `FieldSecurityLock.Behavior` gains **`MASK`** and **`REVEAL`** next to ALLOW and DENY. A lock's
  tier: ALLOW → FULL, REVEAL → REVEALABLE, MASK → MASKED, DENY → HIDDEN; holding an override value
  (or the all-access key) → FULL. `FieldSecurityLock.withMaskStyle(FieldMaskStyle)` sets the mask.
  Existing ALLOW/DENY behaviour is unchanged.
- `FieldAccessHelper` (static) is the single entry point:
  - `FieldAccessTier getTier(QTableMetaData table, String fieldName)` and
    `Map<String, FieldAccessTier> getTiers(QTableMetaData table)`: the most restrictive of the lock
    and the resolver; FULL when neither exists. Results are cached per QContext call.
  - `String getAccessFingerprint()`: the resolver's fingerprint plus a hash of the lock-derived
    tiers. Data analysis and any other cache key on it.
  - `void assertCriteriaAllowed(String tableName, List<QueryJoin> joins, QQueryFilter filter,
    Collection<String> sortGroupAggregateFieldNames)` (§3.2).
  - `void applyToRecords(String tableName, List<QueryJoin> joins, List<QRecord> records, String actionName)` (§3.3).
  - `QTableMetaData applyToTable(QTableMetaData table)`: drops HIDDEN fields (as `removeDeniedFields`
    does), sets `isEditable=false` on non-FULL fields, and records each field's tier for the
    frontend metadata (§3.6).
  - `void assertWritesAllowed(QTableMetaData table, List<QRecord> records)`: replaces
    `assertWritesAccessible`, refusing non-FULL fields.
- `FieldSecurityLockHelper` keeps its public methods and delegates to `FieldAccessHelper`, so
  existing callers and tests keep working (DENY = HIDDEN).

### 3.2 Filter, sort, group and aggregate enforcement
`CorePermissionEnforcer.checkReadCriteria` calls `FieldAccessHelper.assertCriteriaAllowed`
(replacing the lock check), for every action that already calls it (query, count, aggregate,
get-by-unique-key, export, report source queries, possible-value searches):
- each criteria field (including `otherFieldName` comparisons and sub-filters, on base or joined
  tables) must satisfy `tier.canFilter(operator)`;
- each order-by, group-by and aggregate field must be FULL;
- otherwise `QPermissionDeniedException("Permission denied.")`, mapped to 403 by the UI API. The
  message does not name the field, so HIDDEN fields are not confirmed to exist.
- a REVEALABLE exact-match criterion is allowed and raises `onAccess(EXACT_MATCH_FILTER)` with the
  table and field names (never the searched value).
- `IS_BLANK` / `IS_NOT_BLANK` are not exact matches and are refused for REVEALABLE (they partition
  the table without a known value). `IN` is limited to 25 values for REVEALABLE so it cannot be
  used to enumerate.
- **Enumeration guardrails** (§7.6). Before an exact-match query runs, core calls the resolver's
  `checkExactMatch(FieldAccessEvent)`, which may refuse it (rate limit, low-entropy field) with a
  `QPermissionDeniedException` carrying a user-facing message. The resolver's decision can also
  say `exactMatchAllowed=false` for a field, which makes REVEALABLE behave like MASKED for
  filtering. After the query (or count), core reports the outcome: `onAccess(EXACT_MATCH_FILTER)`
  with `outcome` MATCH or NO_MATCH and `recordCount`. A refusal is reported with outcome REFUSED.
  The searched values never leave core: the event carries only `searchValueFingerprints`, from the
  resolver's `fingerprintSearchValue(Serializable)` (the qbit uses a keyed HMAC), so distinct-value
  counts can be kept without storing values.
- **Client channels** (#1141). The table-level channel wall in `PermissionsHelper` runs first; field
  tiers are evaluated after it in the same enforcement path, and the resolver sees the channel on
  the session (`QSession.getClientChannelName()`), so a policy can cap tiers per channel (§7.1).

### 3.3 Masking results
`QueryAction` (the path under get, export, reports, record-list widgets and record search) calls
`FieldAccessHelper.applyToRecords` where it calls `removeDeniedValues` today, after display values
are generated:
- HIDDEN: value and display value removed (today's behaviour);
- MASKED: value and display value replaced by the mask;
- REVEALABLE: masked unless `isRevealActive(table, field, primaryKey)`, in which case the real value
  stays;
- joined values (`alias.field`) and associated records, recursively;
- record labels that use a non-FULL field are cleared.

Mask styles reuse the `AiValueMasker` rules, moved to `FieldValueMasker` in the fieldaccess package
(`AiValueMasker` delegates):
- `AUTO` (default): emails keep the first character and the domain, digit strings keep the last
  4, other text keeps the first and last character, short values become `***`;
- `EMAIL`, `LAST_4`, `FIRST_LAST`: force one rule;
- `FULL`: always `***` (dates, PHI notes).

Then `onAccess(VIEW)` is called once per action with the restricted fields that were returned
(including FULL fields that are classified, for the FULL-user view audit; see §6.2).

### 3.4 Egress points outside QueryAction
Values can leave without a top-level enforced QueryAction (a process step or a widget reads as the
user inside an authorized scope). These are covered at the UI API boundary, where the table is
known:
- `ProcessRecordsExecutor` (`/processes/{p}/{uuid}/records`): `applyToRecords` on the step's table
  (the process's `tableName`, or the frontend step's record-list table);
- `ProcessInitOrStepExecutor` values that are `QRecord` lists of a known table: same;
- `WidgetDataExecutor`: record-list, child-record-list and table-row widgets apply it to their
  records (renderers know the table); custom widgets that build HTML from field values are the
  widget author's responsibility, and the spec lists this as a documented limit;
- `RecordSearchAction` (core quick search): non-FULL fields are never used for matching, and result
  labels built from them are cleared. qbit-quick-search's provider must do the same (its index
  never includes a field whose tier is not FULL for the searching user; it filters at query time
  by tier because indexes are shared). This is a separate qbit-quick-search PR.
- The QQQ API middleware (`qqq-middleware-api`) uses the same core actions with enforcement, so it
  masks; its OpenAPI field schemas gain `x-qqq-access-tier`.
- Exports and reports stream through `QueryAction`, so cells are masked; a MASKED or HIDDEN field
  in a report's group-by, pivot or sort is refused by §3.2.
- **Audit details** carry old and new values of another table's fields.
  `FieldAccessHelper.applyToAuditDetails(String tableName, List<QRecord> auditDetails)` drops rows
  for HIDDEN fields and masks old/new values for MASKED and REVEALABLE ones (reveals do not apply
  to history). The audit screens and the collaboration timeline call it.
- **Realtime** (`RealtimeHub`, #1144) sends field names only; names of HIDDEN fields are dropped
  per subscriber (`tier.isInMetaData()`).
- **Stored labels** (favorites, recents, notifications): code that persists or forwards a record
  label checks `FieldAccessHelper.isRecordLabelFullAccess(table)` and stores null otherwise.

### 3.5 Fingerprint and caching
Anything that caches per-user results (data analysis, quick-search result caches, widget caches)
must key by `FieldAccessHelper.getAccessFingerprint()`. The qbit's fingerprint changes when the
user's permissions, the policy version, the classification version, or break-glass state change;
reveal grants do not change it (they change values for one record, which are never cached).

### 3.6 UI API (additive, documented in `UI-API-CHANGELOG.md` under the open 1.2.0 entry)
- `FieldMetaData` gains `accessTier` (enum FULL, REVEALABLE, MASKED; omitted when no lock and no
  resolver applies, which means FULL), `allowedFilterOperators` (present only when restricted:
  `["EQUALS","IN"]` for REVEALABLE, `[]` for MASKED), and `isSortable` (false when restricted).
  `isEditable` is false for non-FULL fields.
- `TableMetaData` gains `hasRestrictedFields` (boolean) so clients can skip work on most tables.
- `QRecord` responses are unchanged in shape: masked values are strings.
- Clients that ignore the new properties still never see a masked value in the clear (server-side
  masking), they only lose the chip and Reveal affordance.
- 403 on a disallowed filter or sort keeps the standard error body.

## 4. Core tests (phase a)
- `FieldAccessTierTest`: operators, ordering, combination.
- `FieldAccessHelperTest`: lock-only, resolver-only, both (most restrictive), none; fingerprint.
- Query, get, count, aggregate, export (CSV/XLSX/JSON), report, record search, process records and
  record-list widget, each with FULL, REVEALABLE, MASKED and HIDDEN users:
  - values (masked, removed, revealed), display values, joins, associations, record labels;
  - filter operators allowed/refused (EQUALS/IN for REVEALABLE; IN > 25 refused; IS_BLANK refused);
  - sort, group-by and aggregate refused;
  - writes refused;
  - field metadata tier, `isEditable`, `allowedFilterOperators`.
- **Response scanner:** a test utility that seeds distinctive sentinel values in every restricted
  field, runs every scenario through the `/qqq/v1` Javalin layer (query, get, export, search,
  process records, widget, metaData, possible values, assistant channel via `UiApiInvoker`) as a
  MASKED and a HIDDEN user, and fails if any sentinel (or its unmasked substrings longer than 4
  characters) appears anywhere in a response body or header.
- Existing `FieldSecurityLock` tests keep passing unchanged.

## 5. Classification (qbit)

The effective classification of a field:
1. a `secured_data_classification` row for the table and field;
2. a row for the table with no field (applies to every field of the table);
3. the field's `AiFieldMetaData.classification` (code);
4. the table's `AiTableMetaData.classification` (code);
5. none (unclassified, always FULL).

Rows can raise or lower the code default (an admin can declassify a field the code marked PII,
which is logged). Rows may also set the field's `mask_style`. The qbit's producer can seed code
defaults for common names (`ssn`, `dob`/`date_of_birth`, `email`, `phone`, `diagnosis*`) through
`SecuredDataQBitConfig.withNameHeuristics(true)`; off by default.

## 6. Tables (qbit)

All tables use the backend the host supplies (RDBMS in kof22), audit on, ESB table events on for the
main tables, record screens with sections. Labels in brackets.

### 6.1 Configuration
- **`secured_data_classification`** [Classification]: `id`, `table_name` (PVS of tables),
  `field_name` (nullable; PVS filtered by table), `classification` (PUBLIC, CONFIDENTIAL, PII, PHI),
  `mask_style` (nullable), `low_cardinality` (nullable boolean: null = auto, §7.6), `notes`, `create_date`, `modify_date`, `modified_by`. Unique
  (table_name, field_name).
- **`secured_data_policy`** [Policy]: `id`, `name`, `permission_name` (the session permission this
  row applies to, e.g. `cs_admin`, or an authentik-generated permission), `classification`,
  `table_name` (nullable: all tables), `tier` (FULL, REVEALABLE, MASKED, HIDDEN),
  `reveal_minutes` (default 5, 1–60), `daily_reveal_cap` (nullable), `reason_options` (text, one
  per line), `allow_free_text_reason` (default true), `require_free_text` (default false),
  `client_channel` (nullable: all channels; a channel row only caps, see §7.1),
  `exact_match_per_hour` (default 20), `miss_alert_consecutive` (default 5),
  `distinct_value_alert` (default 15), `min_entropy_enabled` (default true) (§7.6),
  `is_active`, `create_date`, `modify_date`, `modified_by`.

### 6.2 Activity
- **`secured_data_reveal_grant`** [Reveal] (child of the access log app section, not in nav):
  `id`, `user_id`, `user_name`, `table_name`, `record_id`, `field_name`, `reason`, `reason_text`,
  `granted_at`, `expires_at`, `access_log_id`. Read-only for everyone (written by the Reveal
  operation as the system), so grants cannot be forged.
- **`secured_data_break_glass`** [Break-glass session]: `id`, `user_id`, `user_name`, `reason`,
  `reason_text`, `classification_ceiling` (default PHI), `table_name` (nullable: all), `started_at`,
  `expires_at` (default 60 min, max 8 h), `ended_at`, `ended_by`, `status` (ACTIVE, ENDED,
  EXPIRED). Read-only; written by the Start/End actions.
- **`secured_data_access_log`** [Access log]: `id`, `event_type` (VIEW, REVEAL, REVEAL_DENIED,
  EXACT_MATCH_FILTER, FILTER_DENIED, BREAK_GLASS_START, BREAK_GLASS_END, BREAK_GLASS_EXPIRED,
  POLICY_CHANGE, CLASSIFICATION_CHANGE), `user_id`, `user_name`, `table_name`, `record_id`
  (nullable), `field_name` (nullable; comma list for VIEW), `record_count`, `action_name`,
  `reason`, `reason_text`, `client_channel` (from `QSession.getClientChannel()` when present),
  `outcome` (ALLOWED, DENIED, CAPPED, MATCH, NO_MATCH, REFUSED), `anomaly_flag`, `break_glass_id`, `created_at`. Insert-only (no edit, no
  delete permission is ever granted; the table has no update/delete capability).
  - VIEW rows are written for restricted fields only (classification PII or PHI by default,
    configurable), one row per action per table, with up to 50 record ids; GETs log their one
    record. FULL users' views are logged too ("audited views").
  - Writes are asynchronous (a bounded in-memory queue drained as the system), so logging never
    blocks or fails a read; a full queue logs a WARN and counts the drop in the app home.

## 7. Resolution, Reveal and break-glass (qbit)

### 7.1 Tier resolution (per session, cached until the fingerprint changes)
For a field with effective classification C on table T:
1. Unclassified or PUBLIC → FULL.
2. An ACTIVE, unexpired break-glass session for the user covering T with ceiling ≥ C → FULL.
3. The table feature `T.secured_data_full` → FULL; `T.secured_data_reveal` → at least REVEALABLE.
4. Active `secured_data_policy` rows whose `permission_name` the session holds, whose
   classification is C and whose table is T or null → the least restrictive tier among them.
5. Policy rows whose `client_channel` equals the session's channel name (e.g. `assistant`) then
   cap the result: the most restrictive of the result and those rows' tiers.
6. Otherwise the code default for C from `SecuredDataQBitConfig` (defaults: CONFIDENTIAL FULL, PII
   MASKED, PHI MASKED).
7. Holders of `secured_data.admin` are FULL on the qbit's own tables only; they get no implicit
   access to classified data elsewhere (separation of duties).

The fingerprint is a hash of (user id, sorted permissions, policy version, classification version,
active break-glass id).

### 7.2 Reveal (UI API operation)
`POST /qqq/v1/qbit/secured-data/reveal` with `{tableName, primaryKey, fieldName, reason,
reasonText}`:
- the caller must have READ on the table and the record (the record is fetched as the user, so
  record locks apply) and the field's tier must be REVEALABLE;
- the reason must be one of the policy's `reason_options`, or free text when allowed (required
  when `require_free_text`); max 500 characters;
- the daily cap (per user, across tables, from the most permissive matching policy row) is
  checked; over the cap → 429-style refusal (`outcome=CAPPED`) and an alert;
- on success: a `secured_data_reveal_grant` (expires in `reveal_minutes`), an access-log REVEAL
  row, and the response `{value, displayValue, expiresAt, revealId}`. Until expiry, the same
  record's field comes back unmasked in GET/query for that user (`isRevealActive`).
- `GET /qqq/v1/qbit/secured-data/reveal/options?tableName=&fieldName=` returns the reason list,
  whether free text is allowed/required, the duration and the remaining daily allowance, so the
  dialog is driven by the server.
- Reveal is per user: another session of the same user shares it; other users never do.

### 7.3 Break-glass (table actions on `secured_data_break_glass`)
- **Start break-glass** (process `secured_data_break_glass_start`, permission: the process's own
  has-access permission, granted to few): reason (required, free text ≥ 20 chars), duration,
  optional table scope and ceiling. Creates the session, logs BREAK_GLASS_START, raises a
  `secured_data.break_glass` alert (severity HIGH) through the alerts SPI when present, and sends a
  bell notification to holders of `secured_data.admin`.
- **End break-glass** (record action, standard ETL Review/Result): ends the user's own session (or
  any, for admins), logs BREAK_GLASS_END, resolves the alert.
- **Expiry:** a scheduled job every minute marks overdue sessions EXPIRED and logs
  BREAK_GLASS_EXPIRED; resolution also checks `expires_at` on every call, so expiry is exact even
  if the job is late.
- `GET /qqq/v1/qbit/secured-data/status` returns the caller's active break-glass session (if any)
  for the banner, and the caller's reveal allowance.

### 7.4 Anomalies (phase e)
Evaluated when a REVEAL or EXACT_MATCH_FILTER row is written (cheap counters per user per day):
- reveals above the policy's `daily_reveal_cap` (CAPPED), or above 3× the user's 30-day daily
  median with at least 10 reveals;
- reveals across more than 20 distinct records in an hour;
- more than 50 exact-match searches on a restricted field in an hour;
- any break-glass session.
Each raises an alert (`secured_data.anomaly`, keyed by user and day) through the alerts SPI and
sets `anomaly_flag` on the log row. Without qbit-alerts, flags still show on the app home.

### 7.6 Enumeration guardrails for exact-match search
A CS admin with REVEALABLE access could guess and check low-variety values (birth year, state,
last 4 digits, a date of birth within a year) without ever revealing. Four controls, each a
policy setting on `secured_data_policy`, all ON by default:
1. **Rate limit** (`exact_match_per_hour`, default 20, per user per field): counted from
   `secured_data_access_log` EXACT_MATCH_FILTER rows in the last hour (authoritative across nodes).
   Over the limit the search is refused: "Exact-match search limit reached for [field]. Try again
   later or ask an administrator." (403, outcome REFUSED).
2. **Miss alerting** (`miss_alert_consecutive`, default 5; `distinct_value_alert`, default 15 per
   hour): N consecutive NO_MATCH searches on the same field by one user, or more than M distinct
   searched values (by HMAC fingerprint) in an hour, raise `secured_data.enumeration` through the
   alerts SPI and flag the log row.
3. **Minimum entropy** (`min_entropy_enabled`, default true): exact match is disabled
   (`exactMatchAllowed=false`) for fields marked low-cardinality on
   `secured_data_classification.low_cardinality` (true/false/null = auto). Auto marks: boolean and
   enum (possible-value-source) fields, date fields, integer fields named or labelled like a year,
   state/region/zip3 fields, and string fields whose `maxLength` is under 6. Admin rows override
   auto in either direction.
4. **Logging:** every exact-match search writes one access-log row with outcome MATCH, NO_MATCH
   or REFUSED (and the reason for a refusal); never the searched value.

These are **compensating controls** for the HIPAA minimum-necessary standard (45 CFR
164.514(d)): users whose role needs only to locate a record by an identifier they already hold get
that and nothing more. They also implement access monitoring (164.308(a)(1)(ii)(D), information
system activity review, and 164.312(b), audit controls): every restricted access is logged with
user, field, outcome and channel, and anomalies alert.

### 7.5 Permissions
- Table features registered through `TableFeaturePermissionsMetaData` for every table with a
  classified field: `secured_data_full` ("Secured data: full") and `secured_data_reveal`
  ("Secured data: reveal"). They show in the qbit-authentik policy editor catalog
  (`{"Action":["secured_data_reveal"],"Resource":["patient"]}`).
- `secured_data.admin`: manage classification and policy, see the access log and grants.
- The qbit's tables use standard table permissions; the access log, grants and break-glass
  tables are read-only to everyone (written as system).
- The assistant channel is denied `secured_data_*` (registered with
  `ClientChannelsMetaData.withDeniedPattern("assistant", "secured_data_*")`).

## 8. App home (qbit, phase e)
One app, "Secured Data", with one home dashboard that owns this data:
- KPIs: reveals today, active reveal grants, active break-glass sessions, anomaly flags (7 days),
  denied filters (7 days);
- recent reveals (table, full width: user, table, record, field, reason, when, channel);
- top revealers (30 days) and top reasons;
- break-glass events;
- policy and classification changes;
- anomaly flags.
Nav: Classification, Policy, Access log, Break-glass sessions. Reveal grants are a child list on
the access log and on users. No other dashboards.

## 9. Next UI (QRun-IO/qqq-frontend-next, separate PRs to main)
- **Masked values:** any field with `accessTier` MASKED or REVEALABLE renders its value as a
  muted, monospace mask with a lock chip ("Masked" / "Restricted"), in grids, record views,
  child lists and pickers. No client-side blur of real data (there is none to blur).
- **Reveal:** on REVEALABLE fields in the record view (and the grid cell menu), a Reveal button
  opens a dialog driven by `/reveal/options` (reason select, free text, duration, remaining
  allowance), calls `/reveal`, shows the value with a countdown, and re-masks at expiry.
- **Filters:** the filter builder and column header menu offer only `allowedFilterOperators`;
  MASKED fields are not offered in filter, sort or group pickers; REVEALABLE fields offer
  "equals"/"is any of" with a note "Exact match only, audited".
- **Forms:** non-FULL fields are read-only and never sent in update payloads.
- **Break-glass banner:** when `/status` reports an active session, a persistent red banner with
  the reason, a countdown and an End button.
- Code-split: the Reveal dialog and banner load on demand; the size change is reported.

## 10. Phases
- **(a) Core**: §3–§4 in qqq (PR to develop) plus `UI-API-CHANGELOG` notes. Until it ships in an
  RC, the qbit compiles against the qqq SNAPSHOT built locally and CI uses the RC once cut
  (stated in each PR).
- **(b) Qbit tables, classification, policy, resolver, access log** (§5–§6, §7.1, §7.5).
- **(c) Reveal and break-glass** (§7.2–§7.3) with the UI API extension `secured-data` 1.0.0.
- **(d) Next UI** (§9).
- **(e) App home and anomaly alerts** (§7.4, §8).
- **(f) Retention and wipe** (§13), with crypto-shredding keys (§14.3).
- **(g) Audit value protection and egress**: §14.1–§14.2 as a core PR; §15 in core, qqq-esb and
  the qbits listed, coordinated with their owners; the canary and wipe-proof tests.

## 11. Acceptance matrix
For each of FULL, REVEALABLE, MASKED, HIDDEN users over query, get, export, quick search, filter,
sort, aggregate, process records, widget, the QQQ API and the assistant channel:
- the value outcome matches §2 and the response scanner finds no sentinel for MASKED/HIDDEN;
- REVEALABLE: exact-match finds the record with the value still masked, the search is logged,
  reveal shows the value for N minutes then masks again, the cap refuses and alerts;
- break-glass grants FULL, notifies, and expires on time;
- enumeration guardrails: the 21st exact-match search in an hour is refused; 5 consecutive misses
  and 16 distinct values each alert; a low-cardinality field (boolean, enum, year, state, short
  digits, admin-marked) refuses exact match; each search logs MATCH, NO_MATCH or REFUSED;
- every reveal, exact-match search, denied filter and restricted view produces exactly one
  access-log row with user, table, field, record(s), reason, channel and outcome.

## 12. Open questions (decided as recommended, 2026-10-12)
1. **Writes to non-FULL fields.** Recommended: refused (a masked value must never be written back
   by accident). Write-only data entry for REVEALABLE can be added later as a tier option.
2. **IS_BLANK for REVEALABLE.** Recommended: refused (it is not an exact match).
3. **Admins and data.** Recommended: `secured_data.admin` manages policy but gets no implicit data
   access (separation of duties); admins get FULL through policy rows like anyone else.
4. **VIEW logging volume.** Recommended: one row per action per table, PII and PHI only, up to 50
   record ids; configurable.
5. **Default tiers.** Recommended: CONFIDENTIAL FULL, PII MASKED, PHI MASKED (fail closed once the
   qbit is installed; the policy opens access).
6. **Reveal scope.** Recommended: one field of one record, per user, all of the user's sessions.

## 13. Retention and wipe (qbit, phase f)

> **Owner request (2026-10-13, verbatim):** "remove or wipe or overwrite secure data after X days...
> we want to remove readings from a medical device after 30 days. we want the reading record to
> stay - but the one field with the data needs to be wiped... fully audited".
>
> **Owner decision:** the default action NULLs the field and writes a marker row, so screens show
> "Wiped per policy (30 days)".

### 13.1 Tables
- **`secured_data_retention_rule`** [Retention rule] (main table, in the nav):
  - Identity and target: `id`, `name`, `table_name`, and `field_names` (one or more fields of the
    table, as a comma list validated against the table).
  - Age: `age_field_name` (the date or date-time field the age is measured from, e.g.
    `reading_time`) and `age_days` (at least 1).
  - `action`: NULL_WITH_MARKER (the default), OVERWRITE_TEXT, KEYED_HASH, DELETE_RECORD or
    ARCHIVE_THEN_WIPE (§13.2).
  - Action options:
    - `overwrite_text`: default "[wiped per policy]"; text fields only.
    - `audit_treatment`: SHREDDABLE (the default) or CHANGED_ONLY (§14).
    - `archive_storage_name`: a qbit-object-storage store; used by ARCHIVE_THEN_WIPE only.
    - `batch_size`: default 500.
  - Status and housekeeping: `is_active`, `last_run_at`, `last_run_count`, `create_date`,
    `modify_date` and `modified_by`.
- **`secured_data_wipe_marker`** [Wipe marker]:
  - Columns: `id`, `table_name`, `record_id`, `field_name`, `rule_id`, `action`, `wiped_at` and
    `run_id`. Unique on (table_name, record_id, field_name).
  - It is a child of the rule. It also shows as a child list on any record screen, through a
    generic "Wiped fields" widget.
  - Core needs no change for this. The record GET response gains nothing; instead, the widget and
    a field adornment read the marker. Next shows "Wiped per policy (N days)" for a null field
    that has a marker.
- **`secured_data_legal_hold`** [Legal hold]:
  - Columns: `id`, `table_name`, `record_id` (nullable, meaning the whole table), `reason`,
    `placed_by`, `placed_at`, `released_by`, `released_at` and `status` (ACTIVE or RELEASED).
  - An active hold on a record, or on its table, pauses every rule for that record. A run counts
    held records separately.
- **`secured_data_wipe_run`** [Wipe run]:
  - Columns: `id`, `rule_id`, `started_at`, `finished_at`, `status`, `eligible_count`,
    `held_count`, `wiped_count`, `error_count`, `record_ids` and `message`. `record_ids` is bounded
    text; the full list is in the audit.
  - It is insert-only for users; only the process writes it.

### 13.2 Actions
| Action | Field | Record | Audit detail (§14) |
|---|---|---|---|
| NULL_WITH_MARKER (default) | set to null; a marker row is written | kept | `wiped:v1:<ruleId>` |
| OVERWRITE_TEXT | set to `overwrite_text` (STRING and TEXT fields only; validated) | kept | `wiped:v1:<ruleId>` |
| KEYED_HASH | set to `hmac:v1:<keyId>:<mac>` with the instance key, so a known value can be matched later. Refused for low-entropy fields (§7.6). | kept | `wiped:v1:<ruleId>` |
| DELETE_RECORD | — | deleted by a normal delete, with its audit; associations follow the table's own rules | the delete audit |
| ARCHIVE_THEN_WIPE | the values are first written to the rule's qbit-object-storage store as one encrypted JSON object per record (a locked WORM bucket where available), then handled as NULL_WITH_MARKER | kept | `wiped:v1:<ruleId>` plus the archive object key |

- **Every action** destroys the per-record-per-field audit keys of the wiped fields, in the same
  transaction as the field update (§14.3).
- **A record is eligible** when its `age_field_name` is older than `now - age_days`, the field is
  not already null, and it has no active legal hold.

### 13.3 The process
`secured_data_wipe` is a standard streamed-ETL process. It extracts eligible ids, transforms
them, and loads in batches of `batch_size`. It runs in two ways:
- **On a schedule**, daily by default (configurable), as the system, for every active rule.
- **As a table action**, "Run wipe now" on selected rules (permission `secured_data.retention`). It
  shows the standard **Review** (eligible, held and to-wipe counts per rule, never values) and
  **Result** lines.

Every batch writes the following, as the system:
- one `secured_data_wipe_run` row;
- `secured_data_access_log` rows of type WIPE: the rule, table and field, up to 50 record ids per
  row, the action and the outcome;
- the core audit of each update, with the `wiped:v1` detail.

**No values are ever read into logs, Review lines, messages or audits.** The process selects ids
only and updates by id.

### 13.4 Backups versus the wipe period
Wiped values remain in backups taken before the wipe. The rule screen shows a "Backups" panel
with:
- the backup retention it knows about:
  - qbit-backups policies, through its retention API when present;
  - an admin-entered CNPG/WAL retention (`SecuredDataQBitConfig.withBackupRetentionDays(...)`;
    unknown by default).
- a warning when any retention exceeds `age_days`, for example: "Backups keep data for 35 days,
  longer than this rule's 30 days. Wiped values remain recoverable from backups until they
  expire."

The app home shows the same warning for every rule it applies to.

## 14. Audit value protection (core hook + qbit)

> **Owner decision:** audit details for PHI and PII fields never store values. "Changed by whom,
> when" stays visible.
>
> **Design correction (2026-10-13):** an HMAC with a global key of a low-entropy value (such as a
> BP reading), sealed in WORM for six years, can be brute-forced by anyone who holds the key. That
> would defeat a 30-day wipe. Fields under a wipe rule, and low-entropy fields, therefore use
> crypto-shredding.

### 14.1 Core hook (additive core PR)
For every audited field whose effective classification is PII or PHI, `DMLAuditAction` asks an
`AuditValueProtectorInterface` for the values to store. The protector is registered through
`AuditValueProtectionMetaData`, a supplemental instance metadata. The classification comes from
`AiDataClassifications`, or from the protector's own classification source.

```java
public interface AuditValueProtectorInterface
{
   /** Never returns plain values for classified fields. */
   ProtectedAuditValues protect(QTableMetaData table, QFieldMetaData field, Serializable primaryKey,
                                Serializable oldValue, Serializable newValue, DMLType dmlType);
}
public record ProtectedAuditValues(String oldToken, String newToken, String message) {}
```

- **What the detail stores.** `oldValue` and `newValue` hold the tokens. `message` contains no
  value: "Changed Systolic", "Set Systolic" or "Removed Systolic". Display formatting (such as
  possible-value labels) is skipped for these fields.
- **Without a protector, core still never writes classified values.** The built-in default uses
  the instance HMAC key from the environment (`QQQ_AUDIT_HMAC_KEY`, base64, from Infisical) and
  writes `hmac:v1:<keyId>:<mac>`. With no key configured, it writes `changed:v1` (no derivative)
  and logs one WARN at startup.
- **Unclassified fields** are unchanged: their values are audited as today.
- **Verifying a known value.** `ClassifiedValueTokens.verify(...)` (§14.5) checks
  a value against an `hmac:` token, or against a `shred:` token while its key still exists. The
  qbit uses it for an admin-only "Verify a value" record action on audit details, and that action
  is itself access-logged.

### 14.2 Token formats (what qbit-worm-audit seals)
All tokens are ASCII, at most 200 characters, and are stored in `auditDetail.oldValue` and
`newValue`. The MAC input for both keyed tokens is
`m = table \x1f field \x1f pk \x1f canonical(value)`.

| Token | Meaning |
|---|---|
| `hmac:v1:<keyId>:<b64url(HMAC-SHA256(K_instance, m))>` | HMAC with the instance key: PII/PHI fields without a wipe rule |
| `shred:v1:<auditKeyId>:<b64url(HMAC-SHA256(DEK, m))>` | HMAC with a per-record-per-field key: fields under a wipe rule, or low-entropy fields |
| `changed:v1` | changed, with no derivative: the rule option "changed only", or no key configured |
| `wiped:v1:<ruleId>` | the wipe itself (as the new value) |
| `shredded:v1:<ruleId>` | a separate audit detail on the record, written when its keys are destroyed |

- **`canonical(value)`** is the field type's canonical string: `ValueUtils` as-string, decimals
  stripped of trailing zeros, and instants in ISO-8601 UTC. Null is never tokenised; it is simply
  an absent old or new value.
- **`keyId`** names the instance key version. Rotation keeps old keys for verification only.
- **Binding table, field and pk into `m`** stops anyone correlating equal values across records.

### 14.3 Crypto-shredding
- **`secured_data_audit_key`** [Audit key] (not in the nav; admins see metadata only):
  - Columns: `id`, `table_name`, `record_id`, `field_name`, `wrapped_key` and `created_at`.
    Unique on (table, record, field).
  - `wrapped_key` is a random 256-bit DEK, wrapped with AES-GCM under the KEK from
    `QQQ_SECURED_DATA_KEK`. The column is HIDDEN to every user, never exported, and never in ESB
    events.
- **Creating keys.** The first time a protected field is audited for a record, the qbit's
  protector creates the DEK and writes a `shred:` token.
- **Shredding.** The wipe deletes the record-field's key row in the same transaction as nulling
  the field, and writes `shredded:v1`. The sealed audit rows stay intact, so WORM holds, but they
  can no longer be verified or brute-forced: the MAC key is gone.
- **Which fields shred:**
  - any field named in an active retention rule with `audit_treatment = SHREDDABLE`;
  - any PII/PHI field that is low-entropy by the §7.6 rules.

  Other classified fields use the instance HMAC. The rule option CHANGED_ONLY writes `changed:v1`
  for its field and creates no key.
- **Keys are not WORM and get short backups.**
  - qbit-worm-audit never seals `secured_data_audit_key`, and WORM exports exclude it.
  - qbit-backups excludes it from long-term policies: its backup set is registered with a short
    retention (7 days by default).
- **Residual risk: CNPG physical backups and WAL archives cannot exclude a table.** A wiped
  value's key stays in them until they expire. Until then, someone who holds both such a backup
  and the KEK can recover the value. The rule screen's Backups panel (§13.4) states this window.
- **KEK:** without `QQQ_SECURED_DATA_KEK` (from Infisical, via the environment) the keys in the
  table are useless, so a database dump alone reveals nothing. KEK rotation re-wraps the rows in
  place.

### 14.5 Reusable token helper (core)
The token helper is not audit-specific: email outboxes, ESB, webhooks and logs use it too.
Package `com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess`:

```java
public final class ClassifiedValueTokens
{
   // record-bound value: m = table  field  pk  canonical(value); hmac:v1 or shred:v1 per the protector
   static String forRecordField(QTableMetaData table, QFieldMetaData field, Serializable pk, Serializable value);
   // non-record value: m = domain  part1  ...  canonical(value), e.g. ("email", template, variable, messageId); hmac:v1 only
   static String forContext(List<String> bindingParts, Serializable value);
   static String changed();                                   // "changed:v1"
   static boolean verifyRecordField(QTableMetaData table, QFieldMetaData field, Serializable pk, Serializable candidate, String token);
   static boolean verifyContext(List<String> bindingParts, Serializable candidate, String token);
   static String canonical(Serializable value);
}
public interface ClassifiedValueKeySource { Optional<KeyAndId> instanceKey(); Optional<KeyAndId> keyForRecordField(String table, String field, Serializable pk); }
public interface LogRedactorInterface { String redact(String message); }   // registered on QLogger; QRecord logPair values are masked by core
```

- The instance key is `QQQ_AUDIT_HMAC_KEY` (one instance key for every use). Without it, every
  method returns `changed:v1`.
- `forRecordField` uses the record-field DEK (shred) when the field shreds (§14.3), so a wipe
  also shreds tokens of that value held elsewhere (an email outbox snapshot of a record-bound
  variable).
- Non-record low-entropy values get `changed:v1`: there is no record key to shred.
- The non-record binding always starts with a domain (`email`, `esb`, `webhook`), so it can never
  collide with a record binding.

### 14.4 qbit-worm-audit
- qbit-worm-audit seals `audit` and `auditDetail` rows as written, including these tokens.
- It must never seal or export `secured_data_audit_key`.
- It treats a `shredded:v1` detail as a normal row.
- Verifying the chain never needs the DEKs.

## 15. No classified values in any egress (phase g)

> **Owner decision:** no PHI in audits or any egress.

Every path that copies record values out of the database must drop or tokenise PII/PHI fields.
The only exception is an interactive response to a user who is entitled to the value (FULL).

| Egress | Treatment | Where |
|---|---|---|
| Audit details | §14 tokens | core `DMLAuditAction` |
| Application logs and error messages | A `QLogger` redaction hook masks classified fields with `FieldValueMasker` in `logPair` values that are `QRecord`s, and in core messages that embed a field value (validation, unique-key violations, value-format errors). A `LogRedactorInterface` hook lets the qbit add patterns. | core |
| ESB table events | Classified field values are omitted from the payload, but their names stay in `changedFields`. A subscription can opt in with `includeClassified=true` only where the PHI routing allows it. | qqq-esb |
| Outbound webhooks | Built from ESB and table events, so the same omission applies. Webhook payload customizers cannot add the values back. | qbit-webhooks |
| Notifications and email | Rendered per recipient through masking (§3.4). Templates never include PHI fields by default. | core, qbit-alerts, collaboration |
| Quick-search indexes | PII/PHI fields are never indexed, for any user (decided by classification). | qbit-quick-search |
| Assistant transcripts | Values reach the model only as the assistant's PHI routing allows. Stored transcripts redact the PII/PHI values the model saw (coordinated with the assistant program). | qbit-assistant |
| Data-analysis caches | No cached value or group key from a PII/PHI field; counts only. | qbit-data-analysis |
| Exports and reports | Masked per tier (§3.3). Stored report files inherit the requester's tier and expire. | core |
| Collaboration timeline and comments | The timeline shows §14 tokens through audit details. Comment text is user-written and out of scope (documented). | qbit-collaboration |

**Canary PHI test.** It runs end to end in the qbit, with a unit variant in core:
1. Plant marker values (`CANARY-PHI-<random>`) in PHI and PII fields.
2. Exercise every egress above that is available on the test classpath:
   - CRUD with audits;
   - a failing validation and a duplicate-key error;
   - log capture;
   - an ESB event;
   - an export as a MASKED user;
   - a quick-search index build, if that qbit is present;
   - a wipe run.
3. Fail if any marker, or any substring of it longer than 4 characters, appears in any captured
   output, log line, audit row, event payload or file.

**Wipe-proof test.** Wipe a low-entropy field, such as a systolic reading in the range 60–250.
Then brute-force every plausible value against every audit token for that record, with every key
the test can reach: the instance HMAC key, the KEK and all remaining DEKs. Nothing may match.
When qbit-worm-audit is on the classpath, the WORM chain must still verify.
