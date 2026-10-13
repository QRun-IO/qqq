# Data Analysis (statistical profiles, pivots, compare, anomalies)

Status: **Approved** (owner, 2026-10-13) · drafted 2026-10-13 · Author: James Maes (drafted with Claude)

Targets:
- **QQQ core 4.1.x**: aggregate extensions in `qqq-backend-core`, pushed down to SQL by
  `qqq-backend-module-rdbms`, with a streamed fallback for other backends (§3).
- **`QRun-IO/qbit-data-analysis`** (Apache-2.0): the analysis engine, tables, permissions, UI API
  extension, alerts, saved and scheduled analyses (§4–§9).
- **QQQ Next UI** (`QRun-IO/qqq-frontend-next`): the column-header action, the profile panel, the
  pivot explorer, compare, the Analyses app home and widgets (§10).

Related work, which this design uses and does not duplicate:
- UI API program: extension point (#1128), typed payloads (#1132), the `/qqq/v1` audit (#1127).
  Every analysis operation is a spec'd `/qqq/v1/qbit/data-analysis/...` operation (§8).
- Collaboration program (`docs/COLLABORATION.md`, #1136): sharing and favorites of saved analyses
  use its contracts once they land (§7.3). Its per-table feature permissions and ours share one
  core hook (§3.6).
- Alerts producer SPI (#1090, `QAlerts.raise` / `resolve`, #1125 `QAlerts.notify`) for anomaly
  watches and scheduled deliveries (§6).
- Related-record picker (#1131): the pivot explorer's "field from a related table" chooser reuses
  its join-path metadata rather than inventing its own (§5.2).
- qbit-sql-workbench: "Open in SQL Workbench" (§9.3), qbit-assistant: tool list from OpenAPI (§8.4).

> **Owner decisions (2026-10-12): all recommendations accepted.** Every open question in §13 is
> decided as recommended ("they are approved. we will adjust as needed once we can view them
> working").
>
> **Owner decisions (2026-10-13)**, from the request below:
> - **Compute is pushed down to the database.** Core `AggregateAction` is extended so RDBMS
>   backends compute distinct counts, top-N, percentiles (`percentile_cont`), histogram bins
>   (`width_bucket`), date bucketing, standard deviation and correlation (`corr`) in SQL.
>   Non-SQL backends fall back to streamed computation with row limits. Large tables get a fast
>   sampled estimate first, an exact run as an async job on demand, and caching per table,
>   filter and data version.
> - **v1 scope is all four areas**: (1) profiles plus drill-through, (2) the pivot / group-by
>   explorer, (3) compare plus relationships, (4) anomalies plus saved and scheduled analyses.
> - **Privacy:** a configurable minimum group size per table hides or merges groups smaller than
>   N. It defaults off for admin-only tables and is set for personal-data tables.

## Owner request (2026-10-13, verbatim)

"a statistical analysis qbit that would allow you to do data dives on any table or filter or
filter set in the system. things like the ability to select all unique values from a column, see
the distribution etc."

## Mandatory standards that shape this design

| Standard | How this design follows it |
|---|---|
| Data in standard visible tables | Configuration, saved analyses, schedules, watches and exact-run results are ordinary `data_analysis_*` tables with full record screens (§7). Nothing lives in a side store; the result cache is a table (`data_analysis_run`). |
| Naming | Tables lowercase snake_case, prefix `data_analysis_`; backend name equals table name; permissions use the full name (`data_analysis.saved.read`, `<table>.analyze`). |
| Processes are actions, never in nav | "Profile this table", "Run exact", "Run now", "Evaluate watch", "Copy analysis" are table or record actions using the standard streamed-ETL Review and Result summary lines. Scheduled evaluation is a hidden process. |
| One app home | The **Analyses** app home owns the saved-analysis, schedule and watch data. Other homes get link-only widgets. Analysis widgets placed on other dashboards render one saved analysis and link back (§7.4). |
| Readable at all widths, light and dark | Panels are single column below 900 px; tables have at most 4 visible columns and no inner horizontal scroll; charts resize to their container; colours come from theme tokens. Screenshots at 6 widths in both themes are an exit criterion of every UI phase. |
| UI only through `/qqq/v1` | Every screen calls spec'd operations of the `data-analysis` extension (§8). Payloads are typed and semantic so a CLI or TUI can render them. |
| Security is the user's | Every query runs as the user's session through `AggregateAction` / `QueryAction` with `PermissionEnforcement.ENFORCE`: table permissions, record locks and field security locks all apply. A field the user cannot read is never profiled, grouped or charted (§2). |
| Alerts through the SPI | Anomaly watches and failed schedules raise through `QAlerts` (#1090) when the Alerts qbit is present; without it they are recorded on the watch row only. |
| Real data only | No sample numbers. Estimates are labelled "Estimate (sample of N rows)"; empty states say why. |

---

## 1. Architecture

```
 Next UI (column header ▸ Analyze, profile panel, pivot, compare, Analyses home, widgets)
        │  /qqq/v1/qbit/data-analysis/...  (spec'd, OpenAPI, semver 1.x)
        ▼
 qbit-data-analysis
   AnalysisService ── AnalysisPlanner ── AnalysisCache (data_analysis_run)
        │                 │
        │      ┌──────────┴──────────────┐
        │  PushdownExecutor          StreamedExecutor (non-SQL backends, row-limited)
        │  (core AggregateAction       (core QueryAction, pipe, bounded memory)
        │   with the §3 extensions)
        ▼
 qqq-backend-core AggregateAction ─► RDBMS (Postgres / MySQL / H2 / SQLite) │ Memory │ Mongo …
```

- **Core (§3)** adds only generic, reusable aggregate capabilities. It knows nothing about
  profiles, pivots or anomalies.
- **The qbit** turns a request ("profile column `amount` of `order` under filter F") into a plan of
  core aggregate calls, runs them as the user, applies the privacy guard (§2.3), caches, and
  returns a typed result with a drill-through filter for every number (§4.4).
- **Next UI** renders the typed results. It contains no statistics code.

## 2. Security

### 2.1 Same path as the UI
- All reads go through core `AggregateAction` and `QueryAction` with the user's `QSession` and
  `PermissionEnforcement.ENFORCE`. `CorePermissionEnforcer.checkReadCriteria` already covers the
  aggregate, group-by and filter field names, and `TableMetaDataPersonalizerAction` removes
  fields the user may not see. Record security locks apply as a filter, exactly as a query.
- The qbit never uses a system session for user requests. Scheduled runs and watches run **as the
  owner of the saved analysis** (a session built for that user by the host's
  `QSessionSupplier`/scheduler user resolution), and are re-checked per recipient (§6.3).

### 2.2 Permissions
| Permission | Grants |
|---|---|
| `<table>.analyze` (per table, using the table's permission base name) | Use any analysis operation on that table. The user must also hold the table's READ permission. |
| `data_analysis.saved.read` / `.insert` / `.edit` / `.delete` | The standard table permissions of `data_analysis_saved` (plus its schedule and star child tables). |
| `data_analysis.watch.read` / `.insert` / `.edit` / `.delete` | `data_analysis_watch`. |
| `data_analysis.settings.read` / `.insert` / `.edit` / `.delete` | `data_analysis_table_config` (Analysis Settings). |
| `data_analysis.run.read` / `.insert` / `.edit` / `.delete` | `data_analysis_run` (Analysis Runs), still subject to the user lock. |
| `data_analysis.admin` | The all-access key of the `data_analysis_user` record locks: see and edit others' saved analyses, stars and runs. |

All qbit tables use standard permission rules (no custom checker), so every name appears in the core catalog and the qbit-authentik policy editor.

- `<table>.analyze` is registered through the core **table feature permission** hook (§3.6), so
  it appears in `PermissionsHelper.getAllAvailablePermissions` and in the qbit-authentik policy
  editor catalog under the table, labelled "Analyze".
- Without `<table>.analyze` every operation for that table returns 403 and the column-header
  action is not shown (`GET .../tables/{table}/capabilities` tells the UI).
- Saved analyses are record-locked on `owner_user_id` (READ and WRITE) with a `shared` flag that
  opens READ to holders of `data_analysis.saved.read` (§7.3). Opening a shared analysis still
  re-runs it as the viewer, so a viewer never sees data they could not query themselves.

### 2.3 Minimum group size (privacy guard)
- `min_group_size` per table (§7.1). Default: **off (0)** for tables whose permission level is
  admin-only (`data_analysis.admin` or the table requires a `*.admin` permission); otherwise the
  code default is **0** and hosts set it for personal-data tables (kof22: 5 for visitor tables).
- When set to N, any group, bucket, top value, crosstab cell or distinct-value listing with
  count < N is merged into one "Other (k groups below N)" row with its summed count; no value,
  min, max or percentile is shown for it. Drill-through on the merged row is disabled.
- Min, max and percentiles of a whole column are suppressed when the filtered row count is below N.
  Outlier listings are suppressed entirely when N is set (they identify individuals).
- The guard runs in the qbit after the query and before caching, so the cache never holds
  un-merged groups for a guarded table.

## 3. Core aggregate extensions (qqq-backend-core, qqq-backend-module-rdbms)

All additions are backwards compatible: new enum values, new optional fields, new interfaces with
default methods.

### 3.1 New aggregate operators
`AggregateOperator` gains:

| Operator | Parameters | Postgres | MySQL 8 | H2 / SQLite | Memory and streamed |
|---|---|---|---|---|---|
| `STDDEV_SAMP`, `STDDEV_POP` | — | `STDDEV_SAMP(x)` | `STDDEV_SAMP(x)` | `STDDEV_SAMP` (H2); SQLite unsupported | `VarianceCalculator` (exists) |
| `VAR_SAMP`, `VAR_POP` | — | `VAR_SAMP(x)` | `VAR_SAMP(x)` | H2 yes | same |
| `PERCENTILE_CONT` | `percentile` (0..1) | `percentile_cont(p) WITHIN GROUP (ORDER BY x)` | unsupported → fallback | H2 `PERCENTILE_CONT(p) WITHIN GROUP` | exact on sorted values (linear interpolation, same definition) |
| `PERCENTILE_DISC` | `percentile` | `percentile_disc(p) WITHIN GROUP (ORDER BY x)` | fallback | H2 yes | exact |
| `CORR` | `otherFieldName` | `corr(y, x)` | fallback | H2 `CORR` | Pearson, two-pass |
| `COUNT_NULL` | — | `COUNT(*) - COUNT(x)` | same | same | count |
| `COUNT_BLANK` | — (string fields) | `SUM(CASE WHEN x IS NULL OR TRIM(x) = '' THEN 1 ELSE 0 END)` | same | same | count |
| `MIN_LENGTH`, `MAX_LENGTH`, `AVG_LENGTH` | — (string fields) | `MIN(CHAR_LENGTH(x))` … | `CHAR_LENGTH` | `LENGTH` | per value |

- `Aggregate` gains optional `percentile` (BigDecimal) and `otherFieldName` (String); `equals` /
  `hashCode` include them so two percentiles of one field are distinct result keys.
- Result types: STDDEV/VAR/PERCENTILE_CONT/CORR return DECIMAL; COUNT_* INTEGER (LONG when the
  backend returns it); PERCENTILE_DISC returns the field's type.

### 3.2 Bucketed group-bys
`GroupBy` gains an optional `bucket` (`GroupByBucket`, sealed):
- `DateBucket(unit)`: `YEAR`, `QUARTER`, `MONTH`, `WEEK` (ISO, Monday), `DAY`, `HOUR`, plus the
  cyclic `DAY_OF_WEEK` (1 = Monday … 7) and `HOUR_OF_DAY` (0–23), evaluated **in the session's
  time zone** for DATE_TIME fields (Postgres `date_trunc(unit, x AT TIME ZONE tz)`, MySQL
  `CONVERT_TZ` + `DATE_FORMAT`, H2 `DATE_TRUNC`). Result: the bucket start as a LocalDate
  (date units) or an Integer (cyclic units).
- `NumericBucket(min, max, count)`: equal-width bins; Postgres/H2 `width_bucket(x, min, max,
  count)`, MySQL `FLOOR((x - min) / ((max - min) / count)) + 1` clamped to `0..count+1`. Result:
  the bucket number (0 = below min, count+1 = above max). The qbit converts bucket numbers to
  ranges.
- `formatString` keeps working and is mutually exclusive with `bucket` (validation error if both).

### 3.3 Top-N and order by aggregate
Already possible (`QFilterOrderByAggregate` + `limit`). Core adds a test and documentation that a
top-N of distinct values is `GroupBy(field)` + `COUNT` + order by count desc + `limit`, and that
the total distinct count comes from `COUNT_DISTINCT` in a separate call. No new API.

### 3.4 Capabilities and fallback
- `AggregateInterface` gains `default Set<AggregateCapability> getSupportedCapabilities(
  AggregateInput)` with values `STDDEV`, `VARIANCE`, `PERCENTILE`, `CORRELATION`, `NULL_BLANK_COUNTS`,
  `STRING_LENGTH`, `DATE_BUCKET`, `NUMERIC_BUCKET`, `SAMPLING`. The default (for unknown modules)
  is the empty set.
- `AggregateAction` checks the requested operators and buckets against the backend's set. With
  `AggregateInput.withAllowStreamedFallback(true)` (default **false**, so existing callers are
  unchanged) unsupported parts are computed by core's **`StreamedAggregateEvaluator`**: one
  `QueryAction` with a record pipe over the same filter, joins and security, honouring
  `AggregateInput.streamedRowLimit` (default 1,000,000). If the limit is hit the output is marked
  `AggregateOutput.truncated = true` with `rowsScanned`; callers must label results "partial".
- The memory backend implements every operator and bucket natively (it is the reference
  implementation for tests). RDBMS implements per dialect as in the tables above; a dialect
  without an operator reports it unsupported and the fallback runs.

### 3.5 Sampling
- `AggregateInput.withSamplePercent(BigDecimal)` (0 < p ≤ 100). Postgres: `TABLESAMPLE SYSTEM (p)`
  on the main table (`BERNOULLI` when the table is under 100k pages is not worth the special
  case); MySQL and H2: `WHERE RAND() < p/100` appended to the where clause; memory: deterministic
  every-k-th record. Backends without `SAMPLING` ignore the percentage and set
  `AggregateOutput.sampled = false`, so callers know the result is exact.
- `AggregateOutput` gains `sampled` (Boolean) and `samplePercent`. Sampling never applies to
  joined tables, only the main table.

### 3.6 Table feature permissions (shared with collaboration)
- New supplemental metadata `TableFeaturePermissionsMetaData` (name `tableFeaturePermissions`)
  holding `TableFeaturePermission(suffix, label, appliesTo: Predicate<QTableMetaData>)`.
- `PermissionsHelper.getAllAvailablePermissions` adds `<baseName>.<suffix>` for each applicable
  table, with `permissionType` = label (e.g. "Analyze"). `PermissionsHelper
  .hasTableFeaturePermission(table, suffix)` is the check.
- The collaboration program's per-table feature permissions (`.comment`, `.attach`, …) can
  register through the same hook instead of a hard-coded loop; this is coordinated through the
  coordinator, not done by this program.
- qbit-authentik's `PolicyCatalog` adds these as a generic `FEATURE` action per table (a small
  qbit-authentik PR, §11).

### 3.7 Data version (cache key)
`TableDataVersion.of(table, filter)` in the qbit, not core: `COUNT(*)` plus `MAX(modifyDate)` (the
table's `modifyDate` field if it has one, else `MAX(primaryKey)`) under the filter. Cheap on an
index; good enough to invalidate the cache on inserts and updates. Deletes on tables without a
modify date are caught by the count. Not a correctness guarantee: cached results show "as of
<time>" and a refresh button.

## 4. Profiles and drill-through (phase b)

### 4.1 Column profile
For one field of a table under a filter (or a saved view), the result `ColumnProfile` has:

- **Counts:** rows, non-null, nulls, blanks (strings), distinct (`COUNT_DISTINCT`), and
  uniqueness = distinct / non-null.
- **Top values:** top 20 by count, with the share of rows. For **possible-value (PVS) and foreign
  key fields** each value carries its display label (resolved through core `QPossibleValueTranslator`
  as the user), and the drill-through filters on the id. "All distinct values" pages through the
  full group-by (limit 500 per page) for the "select all unique values" use case.
- **Numeric** (INTEGER, LONG, DECIMAL): min, max, mean, stddev, sum, p1/p5/p25/p50/p75/p95/p99,
  a 20-bin histogram between p1 and p99 (plus "below" and "above" bins), zeros and negatives
  counts, and outliers by the Tukey rule (outside [Q1 − 1.5·IQR, Q3 + 1.5·IQR]) as a count with
  drill-through, and the 10 most extreme values when the privacy guard is off.
- **Dates** (DATE, DATE_TIME): min, max, range; volume over time with an auto bucket (DAY for
  ≤ 92 days, WEEK ≤ 2 years, MONTH ≤ 10 years, else YEAR); day-of-week and hour-of-day patterns
  (DATE_TIME only); **gaps**: buckets with zero rows inside the range, listed as date ranges.
- **Text** (STRING, TEXT, HTML): min, max and average length, a length histogram, and **patterns**:
  each value reduced to a shape (`A` letter, `9` digit, other characters kept, runs collapsed:
  `AAA-999` → `A-9`), top 10 shapes. Pattern reduction runs in the streamed executor over a
  bounded sample (10,000 rows) and is labelled as such.
- **Boolean:** true, false, null counts.
- **Chart:** a recommended chart kind (§10.3) plus the alternatives that make sense for the type.

### 4.2 Table profile
For a table under a filter: row count, and per readable field: completeness (non-null and
non-blank share), distinct count and uniqueness, a "constant" flag (one distinct value) and an
"empty" flag (all null/blank). Derived:
- **Completeness heatmap**: fields × (completeness), and optionally × a date bucket of the table's
  create date to show completeness over time.
- **Candidate keys**: fields (and pairs among the 8 most unique fields) with distinct = row count
  and no nulls.
- **Duplicates**: for a chosen field set, groups with count > 1 (top 50) with drill-through.

Field-level work is batched: one aggregate call returns COUNT, COUNT_NULL, COUNT_BLANK and
COUNT_DISTINCT for up to 20 fields at a time.

### 4.3 Large tables: estimate first, exact on demand
- If the filtered row count (one `COUNT`) exceeds the table's `sample_threshold_rows` (default
  1,000,000), the first response is computed with `samplePercent` chosen to read about 100,000
  rows, and is marked `estimate: true` with the sample size.
- "Run exact" starts an async job (core `AsyncJobManager`), records a `data_analysis_run` row
  (status RUNNING → COMPLETE/FAILED), and returns its id. The UI polls
  `GET .../runs/{id}` and swaps in the exact result.
- Exact results are cached in `data_analysis_run` keyed by table, normalized filter hash,
  operation, parameters hash and data version, and reused until the data version changes or the
  row is older than `cache_ttl_minutes` (default 60).

### 4.4 Drill-through
- Every number in every result carries a `drill` object: `{ "tableName": "...", "filter": { …
  QQueryFilter JSON … } }`. It is the analysis filter AND the condition that produced the number
  (value equals, IS_BLANK, between bucket bounds, date bucket range in the session time zone,
  NOT_BETWEEN Tukey fences, etc.).
- **Equivalence invariant (tested):** `COUNT(drill.filter)` equals the number shown, for every
  number that is a count, on both the memory backend and Postgres, unless the result is an
  estimate. Bucket bounds are half-open `[lo, hi)` with the last bucket closed to make this hold.
- The UI opens the drill filter in the standard table query screen (`/t/{table}?filter=…`), so
  the user lands on real, editable records.

## 5. Pivot / group-by explorer (phase c)

### 5.1 Model
`PivotRequest`: table, filter, `rows` (1–3 group-by fields), `columns` (0–2), `values` (1–6
aggregates: COUNT, SUM, AVG, MIN, MAX, COUNT_DISTINCT, PERCENTILE p), optional date bucket per
date group-by, optional numeric bucket per numeric group-by, `showTotals`, `limitGroups` (default
50 per dimension; the rest merged into "Other").

`PivotResult`: row header tuples, column header tuples, a cell matrix of typed values, row and
column totals, each cell with `drill`. The privacy guard merges small cells.

### 5.2 Fields from related tables
Group-bys and aggregates may name `joinTable.field` through the table's exposed joins and the
related-record picker's join-path metadata (#1131). The qbit adds the `QueryJoin`s; security on
joined tables applies through core as usual. A join path the user cannot read is not offered.

### 5.3 Execution
One aggregate call for the cell matrix, one per totals level (cheap, same filter), pushed down.
Column dimensions beyond the column limit are merged into "Other" by a first top-N call per
dimension.

## 6. Compare and relationships (phase d)

### 6.1 Compare A vs B
Two filters on one table (or one filter vs the whole table, or two time windows of one date
field). Per chosen field: side-by-side counts, shares, means, medians, deltas and percent
deltas. **Significance where valid:**
- two proportions (a category share): two-proportion z-test;
- two means (numeric): Welch's t-test from count, mean and variance (all pushed down);
- category distributions: chi-square test of homogeneity over the top categories (others merged,
  cells with expected count < 5 merged; if still invalid the test is reported "not applicable").
p-values are shown with a plain sentence ("A and B differ (p = 0.003)" / "No significant
difference"); nothing is called significant at p ≥ 0.05. Statistics code lives in the qbit
(`stats` package, pure Java, unit-tested against reference values).

### 6.2 Distribution shift over time
For a field and a date field: the category shares (or numeric quantiles) per time bucket, plus
the population stability index between the first and last bucket.

### 6.3 Correlation matrix
Up to 12 numeric fields: Pearson `CORR` pairwise, pushed down (66 aggregates in one call on
Postgres). Cells drill to a scatter (sampled 2,000 points as the user) with a least-squares trend
line.

### 6.4 Crosstabs with chi-square
Two categorical fields: counts, row and column shares, chi-square test of independence and
Cramér's V, with the same validity rules as 6.1.

## 7. Tables, saved, scheduled and widget analyses (phase f), anomalies (phase e)

### 7.1 Tables
| Table | Purpose | Key fields |
|---|---|---|
| `data_analysis_table_config` (Analysis Settings) | Per-table runtime overrides of the code defaults | `table_name` (unique), `enabled`, `max_rows` (streamed limit), `sample_threshold_rows`, `sample_rows`, `min_group_size`, `analyzable_fields` (comma list; blank = all readable), `excluded_fields`, `cache_ttl_minutes` |
| `data_analysis_saved` (Saved Analyses) | A saved analysis | `name`, `description`, `table_name`, `filter_json`, `kind` (COLUMN_PROFILE, TABLE_PROFILE, PIVOT, COMPARE, CORRELATION, CROSSTAB, SHIFT), `definition_json` (the typed request), `chart_kind`, `owner_user_id`, `shared`, timestamps |
| `data_analysis_saved_star` | A user's star on a saved analysis (child) | `saved_id`, `user_id`; locked on `user_id`. Replaced by `collaboration_favorite` (target type SAVED_ANALYSIS) when collaboration ships; kept as a view-compatible table until then. |
| `data_analysis_schedule` (Schedules, child of saved) | "Email me every Monday" | `saved_id`, `cron` (or `frequency` DAILY/WEEKLY/MONTHLY + `day` + `time` + `time_zone`), `format` (HTML_SUMMARY, CSV, XLSX), `recipients` (users), `enabled`, `last_run_at`, `last_status`, `next_run_at` |
| `data_analysis_watch` (Watches) | "Watch this metric" | `saved_id` or (`table_name`, `filter_json`, `metric_json`), `detector` (SPIKE, DROP, OUT_OF_RANGE, NEW_CATEGORY, NULL_RATE_CREEP), `parameters_json`, `bucket`, `severity`, `enabled`, `last_evaluated_at`, `state` (OK / FIRING), `alert_key` |
| `data_analysis_run` (Analysis Runs) | Exact runs, the cache and run history | `table_name`, `operation`, `request_hash`, `filter_hash`, `data_version`, `status`, `started_at`, `finished_at`, `rows_scanned`, `sampled`, `result_json`, `error`, `user_id`; locked on `user_id` |

Main tables in the nav: Saved Analyses, Watches, Analysis Settings, Analysis Runs. Schedules and
stars are child lists of Saved Analyses. Saved Analyses and Watches publish ESB table events and
are Quick Search-able.

### 7.2 Configuration
Code defaults (`DataAnalysisQBitConfig`): enabled for all tables, `max_rows` 1,000,000,
`sample_threshold_rows` 1,000,000, `sample_rows` 100,000, `min_group_size` 0, `cache_ttl_minutes`
60, table include and exclude lists. `data_analysis_table_config` rows override per table at
runtime (read through a 60-second memoized lookup).

### 7.3 Sharing and stars
`shared = true` opens READ to `data_analysis.saved.read` holders. When collaboration's sharing
contract (#1136) ships, "Share" uses `collaboration_share` (with its access-request flow) and stars
become `collaboration_favorite` rows; until then the local `shared` flag and star table are used.

### 7.4 Widgets and app home
- **Analyses app home:** starred analyses, my recent runs, firing watches, schedules due, each
  with "View all" to the table.
- **Analysis widget:** one saved analysis rendered as its chart and headline numbers; usable on
  any dashboard and app home (`DATA_ANALYSIS_SAVED` widget, `savedId` default value). It re-runs
  as the viewer and links back to the analysis.

### 7.5 Scheduled delivery
A hidden scheduled process (every 5 minutes) finds due schedules, re-runs the analysis as the
owner, renders HTML (headline numbers and a chart PNG rendered server-side from the same typed
result) plus the requested attachment, and sends to each recipient **only if that recipient can
read the table and has `<table>.analyze`** (re-run as the recipient when recipients differ from
the owner). Delivery uses core messaging (`SendEmailAction`); failures raise an alert through the
SPI.

### 7.6 Anomalies and watches (phase e)
- The evaluator is a hidden scheduled process that, per enabled watch, computes the metric per
  bucket (COUNT, SUM, AVG, NULL share, or category set) over a trailing window as the watch owner.
- Detectors:
  - **SPIKE / DROP**: the latest complete bucket vs a rolling baseline (median and MAD of the last
    k buckets; fire when |robust z| > threshold, default 3.5, and direction matches);
  - **OUT_OF_RANGE**: value outside configured `[min, max]`;
  - **NEW_CATEGORY**: values present in the latest bucket and absent in the baseline window;
  - **NULL_RATE_CREEP**: the null share rose by more than `delta` (default 5 points) versus the
    baseline, or has risen for `n` consecutive buckets.
- On fire: `QAlerts.raise(type "data_analysis.anomaly", key watch id, severity, message, link to
  the drill-through)`; on recovery `QAlerts.resolve`. The watch declares `QAlertTypeMetaData` so
  alert subscriptions work. Without the Alerts qbit the watch row records state only.

## 8. UI API (`/qqq/v1/qbit/data-analysis`, extension `data-analysis` 1.0.0)

### 8.1 Operations
| Method + path | Purpose |
|---|---|
| `GET /qbit/data-analysis/tables/{table}/capabilities` | Can the user analyze it; analyzable fields; config in effect (thresholds, guard); backend capabilities |
| `POST /qbit/data-analysis/tables/{table}/column-profile` | Body `{field, filter, exact?}` → `DataAnalysisColumnProfileV1` |
| `POST /qbit/data-analysis/tables/{table}/distinct-values` | Body `{field, filter, page, pageSize, search?}` → paged values with counts and labels |
| `POST /qbit/data-analysis/tables/{table}/table-profile` | Body `{filter, fields?}` → `DataAnalysisTableProfileV1` |
| `POST /qbit/data-analysis/tables/{table}/duplicates` | Body `{fields, filter}` |
| `POST /qbit/data-analysis/tables/{table}/pivot` | `PivotRequest` → `DataAnalysisPivotV1` |
| `POST /qbit/data-analysis/tables/{table}/compare` | `{filterA, filterB, fields}` → `DataAnalysisCompareV1` |
| `POST /qbit/data-analysis/tables/{table}/correlation` | `{fields, filter}` |
| `POST /qbit/data-analysis/tables/{table}/crosstab` | `{rowField, columnField, filter}` |
| `POST /qbit/data-analysis/tables/{table}/shift` | `{field, dateField, bucket, filter}` |
| `POST /qbit/data-analysis/tables/{table}/sql` | Equivalent SQL for a request (only when qbit-sql-workbench is installed and the user holds its permission) |
| `POST /qbit/data-analysis/runs` | Start an exact run → `{runId}` |
| `GET /qbit/data-analysis/runs/{runId}` | Status and result |
| `POST /qbit/data-analysis/saved/{id}/run` | Run a saved analysis as the viewer |
| `GET /qbit/data-analysis/saved/{id}/export?format=csv|xlsx` | Result as a file |

Saved analyses, schedules and watches themselves are CRUD through the standard core table
operations (`/qqq/v1/table/data_analysis_saved/...`), not new endpoints.

### 8.2 Payloads
Typed, semantic, frontend-agnostic. Every metric is `{ "key": "nulls", "label": "Nulls",
"value": 12, "valueType": "COUNT|DECIMAL|PERCENT|DATE|DATE_TIME|DURATION|TEXT", "formatted":
"12", "drill": {…} | null, "suppressed": false }`. Charts are data, not pictures:
`{ "kind": "HISTOGRAM|BOX|BAR|PARETO|TIME_SERIES|SCATTER|HEATMAP", "alternatives": [...], "x":
{field, label, valueType}, "y": {...}, "series": [ {name, points: [{x, y, label, drill}]} ],
"annotations": [...] }`. A CLI can print the metrics as a table and the series as text; a TUI can
draw a sparkline. All schemas are published as `DataAnalysis…V1` component schemas.

### 8.3 Contract
The qbit commits `src/main/resources/openapi/qqq-v1/data-analysis.yaml` and runs
`UiApiContractAssertions.assertExtensionMatchesCommitted` in its build (#1128 §4). Next vendors
the fragment.

### 8.4 Assistant
qbit-assistant builds tools from `UiApiInvoker.listOperations()`; every operation above has a
clear `summary` and `description`, and request schemas with field descriptions, so "what's the
distribution of order totals last month" maps to `column-profile`. The assistant calls them as
the user through the invoker; no assistant-specific code is needed in this qbit.

## 9. Charts and export

### 9.1 Chart choice
| Field / result | Default | Alternatives |
|---|---|---|
| numeric column | HISTOGRAM | BOX |
| category / PVS / FK / boolean | BAR (top values) | PARETO |
| date volume | TIME_SERIES with 7-bucket moving average | BAR |
| two numerics (correlation drill) | SCATTER with trend line | — |
| table completeness | HEATMAP | BAR |

### 9.2 Rendering (Next)
Next already ships **recharts** for widgets; the analysis panels use it through the same theme
tokens, in a lazily loaded chunk (`next/dynamic`) so the table screen's first load does not grow.
Box plot and heatmap are composed from recharts primitives. Charts have an accessible data-table
toggle.

### 9.3 Export
- CSV and XLSX of any result table (server-side, through core `GenerateReportAction` formats) via
  the export operation.
- Chart PNG and PDF client-side from the rendered SVG (no new dependency; PDF via the browser's
  print of a single-chart page).
- "Open in SQL Workbench": shown only when qbit-sql-workbench is installed and the user may use it;
  the `sql` operation returns the SQL the RDBMS backend would run (via core's existing
  `QueryStat`/SQL-preview path with the user's security filters inlined as a comment), and the UI
  opens the workbench with it as a draft.

## 10. Next UI (phases b–f)
- **Column header ▸ Analyze** on the table query grid (shown when capabilities say so) opens the
  **profile panel** as a right-side panel (full screen below 900 px): headline metrics, chart with
  kind switcher, top values with "show all", and every number clickable to drill through.
- Table toolbar ▸ **Analyze table** opens the table profile; **Pivot** and **Compare** open the
  explorer pages `/analysis/{table}/pivot` and `/compare`, reached from the toolbar, never the nav.
- "Save analysis" stores the current request as a `data_analysis_saved` record.
- Estimate badges, "Run exact" with progress, "as of" timestamps, and suppressed-value hints
  ("Hidden: fewer than 5 records").
- Acceptance tests on chromium, firefox, webkit and mobile-chrome; screenshots light and dark at
  360, 390, 768, 1024, 1440 and 1920 px.

## 11. Phases
| Phase | Repo(s) | Deliverable |
|---|---|---|
| spec | qqq | this document |
| repo | qbit-data-analysis | repo standard, scaffold, producer, config, tables, permissions |
| a | qqq | §3: operators, buckets, capabilities, streamed fallback, sampling, feature-permission hook; memory + RDBMS (Postgres via Testcontainers, H2) |
| b | qbit, Next, (qbit-authentik) | §4 profiles + drill-through + runs/cache + §8 endpoints for them; column-header action and profile panel; authentik catalog FEATURE action |
| c | qbit, Next | §5 pivot explorer |
| d | qbit, Next | §6 compare, shift, correlation, crosstab |
| e | qbit | §7.6 watches and alerts |
| f | qbit, Next | §7 saved, stars, schedules, widgets, Analyses home, export |

Each phase is a green PR per repo; Next PRs pass acceptance on all four browser projects. Until
core phase a ships in an RC, the qbit computes the missing operators with its own streamed
stand-in (`StandInAggregates`, marked for replacement) so its phases are not blocked; endpoints
need the extension point (#1128, RC.6).

## 12. Tests (acceptance)
- **Pushdown correctness:** every new operator and bucket against Postgres (Testcontainers) and
  H2 and the memory backend give the same result on the same fixture (percentiles to 1e-9).
- **Fallback:** a backend without the capability gives the same result through the streamed
  evaluator; the row limit sets `truncated`.
- **Security:** a user without table READ gets 403; without `<table>.analyze` gets 403; a field
  behind a field security lock is absent from capabilities and a request naming it is refused;
  record locks restrict counts exactly as a query does.
- **Privacy guard:** groups below N merged; extremes and percentiles suppressed below N; drill
  disabled on merged rows; cache holds merged results.
- **Drill-through equivalence:** for every count in a column profile, table profile and pivot,
  `COUNT(drill.filter)` equals the shown number (memory and Postgres).
- **UI API contract:** committed fragment matches; schemas are prefixed `DataAnalysis`.
- **Statistics:** z, Welch t, chi-square, Cramér's V, PSI, robust z against reference values.

## 13. Questions put to the owner (all decided as recommended)
1. Default `min_group_size` for non-admin tables: **0 (off) in code, set per table by the host**.
2. Store exact results in a table (visible, admin-readable) rather than an in-memory cache:
   **table** (`data_analysis_run`), with result rows locked to their user.
3. Scheduled runs run as the analysis owner and are re-checked per recipient: **yes**.
4. Outlier value listings are hidden whenever a guard is set: **yes**.
5. Stars and shares move to collaboration contracts when they land: **yes**, with a migration of
   `data_analysis_saved_star` rows into `collaboration_favorite`.
