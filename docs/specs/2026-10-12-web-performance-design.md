# Web performance ("PageSpeed") for QQQ CMS sites: Design Spec

Status: **Approved** · drafted 2026-10-12, owner decisions 2026-10-12 · Author: James Maes (drafted with Claude)
Targets: QQQ 4.1.x, QQQ Next UI 1.0.x, proposed new repo `QRun-IO/qbit-web-performance`; first host `KofTwentyTwo/Website-Backend` + `KofTwentyTwo/Website-Frontend` (www.kof22.com)
Related: [`2026-10-12-qbit-cms-design.md`](https://github.com/QRun-IO/qqq/pull/1083) (#1083), [`2026-10-12-qbit-cdn-design.md`](https://github.com/QRun-IO/qqq/pull/1084) (#1084), [`2026-10-11-qqq-assistant-qbit-design.md`](./2026-10-11-qqq-assistant-qbit-design.md) (#1054), [`2026-09-25-qqq-esb-design.md`](./2026-09-25-qqq-esb-design.md)

Scope: **design only.** Nothing was built or deployed, and no cloud resource or API key was created.

## Owner decisions (2026-10-12)

The owner accepted every recommendation on 2026-10-12 ("accept all pagespeed recommendations"). D1–D13 are decided as recommended; §12 is now the decision log. In short:

| # | Decided | Effect |
|---|---|---|
| D1 | PSI lab + own RUM + CrUX recorded honestly | §2.4 |
| D2 | PSI API runner behind a `LabRunner` SPI; self-hosted Lighthouse is optional P6 | §2.2, §11 |
| D3 | New `qbit-web-performance`, built as a qbit from day one (Apache-2.0). Creating the public `QRun-IO` repo still needs the owner's explicit go-ahead. | §9 |
| D4 | RUM vitals stored in the analytics module (`analytics_vital*`) | §7 |
| D5 | Performance home is a CMS sub-app | §5 |
| D6 | Daily KEY + weekly full + on publish + after deploy, cap 500/day | §6.2 |
| D7 | Build-id probe (`/api/build`, backend version at startup) | §6.3 |
| D8 | Keep all Lighthouse reports forever in PostgreSQL | §4.3 |
| D9 | **Amended by the owner (2026-10-12): Slack is optional.** Regressions are raised through the generic Alerts qbit's producer SPI (`docs/specs/2026-10-12-qbit-alerts-design.md`) when it is installed, and its channels (in-app, email, Slack, webhook, ...) deliver them. Without it they still show on the Performance home, the Dashboard line and in Data Health. Prometheus metrics are an optional extra, not the alerting design. | §8.3 |
| D10 | One key for PSI + CrUX, Infisical write-only seed with `-target`; the owner creates the key and names the 1Password item | §8.2 |
| D11 | RUM attributed to the landing URL (no soft navigations) | §7.1 |
| D12 | Seed Google's "good" thresholds as budgets | §4.2 |
| D13 | All four categories on every run | §6 |

Work that does not need the API key (RUM collection, the `analytics_vital*` tables and p75 rollup, `/api/build`, a local qbit skeleton) starts now; the key, the Infisical seed and P1's PSI runner wait for the owner.

Path conventions: `WB:` is Website-Backend (`origin/main` @ `7760c80`), `WF:` is Website-Frontend (`origin/main` @ `199e8f8`), `GO:` is `KofTwentyTwo/cluster-gitops`, `PR:` is `KofTwentyTwo/Praetor`. `J/` is `WB:src/main/java/com/kof22/website/`.

---

## 0. Summary

The owner asked: *"could we design and integrate a Google PageSpeed like app or dashboard or metric or something like that into the CMS?"*

**Recommendation in one paragraph.** Measure three things and keep all of them as ordinary QQQ records:

1. **Lab data** from the **PageSpeed Insights (PSI) API v5**. That is Lighthouse 13 run by Google, from outside our network, so it measures what a real visitor gets (and later the CDN). Every run covers mobile and desktop and all four categories: Performance, Accessibility, Best Practices and SEO.
2. **Real-user data** (RUM): Core Web Vitals (LCP, INP, CLS, FCP, TTFB) measured in visitors' browsers by the `web-vitals` v6 attribution build. They go through the **existing** consent-gated analytics beacon (`/api/e` → `/analytics/v1/collect`) into analytics tables, with p75 computed the way CrUX computes it.
3. **Google field data (CrUX)**, recorded honestly. kof22 gets about 44 pageviews a day, so it is almost certainly below CrUX's popularity threshold. The CrUX check is still scheduled, and its result is stored as **"no CrUX data"** until Google publishes some. Our own RUM is the field truth in the meantime.

Package it as a separate, reusable **`qbit-web-performance`** (prefix `webperf_`). It installs into any QQQ site, and the CMS plugs in through two small SPIs (URL source, content resolver). Its single home is a **Performance** sub-app under the CMS. Everything else (the CMS home, the Dashboard, the record screens, the Health home) only shows a compact summary or a per-record panel that links there. Tests run on a schedule (daily for key pages, weekly for the whole sitemap), on publish (via `kof22.cms.*` ESB events), after deploys (a build-id probe) and around CDN stage changes. Regressions are confirmed with extra runs before they are raised as alerts through the generic Alerts qbit (when installed), whose subscriptions and channels (in-app, email, Slack, webhook, ...) decide who hears about them. Without that qbit they still show on the Performance home and in Data Health.

**Cost:** $0 a month in API fees. PSI and CrUX are free; the only resource is one Google Cloud API key stored in Infisical. Storage is about 2–4 GB a year if full Lighthouse reports are kept forever.
**Effort:** about **4–5 engineer-weeks** (one engineer with Claude) over six phases. Lab data, the first phase that delivers value, is about 1.5 weeks after the API key exists (§11).

**Finding from research.** A keyless PSI call from this network on 2026-10-12 returned `429 RESOURCE_EXHAUSTED` with `quota_limit_value: "0"` on the shared anonymous project. **Keyless use is not viable; an API key is required** (§8.2).

---

## 1. Rules this design follows

These restate the owner's standing rules. Every later section was checked against them.

| Rule | How this design follows it |
|---|---|
| 100% real data, nothing mocked | Every figure is a stored PSI or CrUX response or a RUM measurement from a real browser. Empty states say why (never tested, draft, CrUX has no data, too few samples). No estimated or interpolated values, and no "sample" scores. Lab and field values are always labelled as such. |
| Qbit data lives in standard, visible QQQ tables | All 11 tables (§4) are normal QQQ tables in the host's RDBMS, including the full Lighthouse report. They are secured by `webperf.*` permissions and, on multi-site hosts, a site record lock. No side store. |
| Processes are table or record actions, never in the nav | Every process is a table or record action (§6). Scheduled jobs are hidden processes. Labels are plain ("Test page speed", "Run site audit"), with no "ETL". |
| Record actions use the standard streamed ETL flow | Every action is `StreamedETLWithFrontendProcess`: record input → standard Review summary lines → Result summary lines. There are no custom dry-run forms. |
| One landing per app, no duplicated data | The Performance home owns every performance widget. The CMS home, Dashboard and Health only get summaries that use the owner's loader and link to it (§5). This is registered in `WB:docs/admin-dashboards.md` and enforced by `LandingPagesTest`. |
| Readable at all widths, no double scrollbars | Readable-widget rules (`WB:docs/admin-dashboards.md` "Readability at all sizes"): tables of 3+ columns are full width, no declared column widths, no `fixedHeight`, at most 4 columns, "View all" links instead of inner scroll. The record panel stacks into one column below 640 px. |
| Planned downtime fine; no data loss; no hacks | Schema is additive. The #1083 cutover rename script also rewrites stored table names (§4.4). |
| Analytics captures everything possible, consent-gated, retained forever | RUM vitals become analytics events under the same gate and are kept forever (§7). |

---

## 2. Data sources (research, 2026-10-12)

### 2.1 What each source is

| Source | What it measures | Access and limits | Fit for kof22 |
|---|---|---|---|
| **PSI API v5** `GET https://www.googleapis.com/pagespeedonline/v5/runPagespeed` | One Lighthouse lab run from Google's infrastructure. Params: `url`, `strategy` (`MOBILE` / `DESKTOP`), `category` (repeatable: `PERFORMANCE`, `ACCESSIBILITY`, `BEST_PRACTICES`, `SEO`; only Performance if omitted), `locale`. The response has `lighthouseResult` (scores, audits, `runtimeError`, `runWarnings`, `environment`) **and** `loadingExperience` / `originLoadingExperience`: the CrUX field data for that URL and origin, when it exists. | Free. Key passed as `key=`. Default quota **25,000 queries a day and 400 per 100 seconds**. A call takes about 10–40 s. **Since 2025-10-20 it runs Lighthouse 13.0** (PSI release notes). | **Primary lab engine.** 87 sitemap URLs × 2 strategies = 174 calls for a full audit, under 1% of the daily quota. |
| **Lighthouse 13 specifics** | Lighthouse 13 replaced most legacy performance audits with *insight* audits, which changes the audit ids in the JSON. For example, `render-blocking-insight` replaced `render-blocking-resources`, `image-delivery-insight` replaced `uses-optimized-images` and three others, `lcp-phases-insight` replaced `largest-contentful-paint-element`, and `document-latency-insight` replaced `server-response-time`, `redirects` and `uses-text-compression`. Some audits were removed (`offscreen-images`, `uses-rel-preload`, `font-size`, ...). Self-hosting needs Node ≥ 22.19. Score weights (since Lighthouse 10): TBT 30%, LCP 25%, CLS 25%, FCP 10%, Speed Index 10%. Bands: 0–49 poor, 50–89 needs improvement, 90–100 good. | n/a | Findings store `audit_id` together with `lighthouse_version`, so trends never compare audit ids across a major version (§6.4). |
| **Self-hosted Lighthouse** (Lighthouse CLI or Lighthouse CI in a k8s Job, headless Chrome) | The same Lighthouse, run by us. LHCI defaults to 3 runs and keeps the median; its docs recommend 5. LHCI Server has its own UI and database (SQLite, Postgres or MySQL), and its `psiCollectCron` can even schedule PSI. | No quota. Needs a Chrome image (~1–2 GB RAM per concurrent run) and Node ≥ 22.19 for Lighthouse 13. | **Not first.** From inside the cluster it measures the LAN path (hairpinned through UniFi, no WAN latency, bypassing a future CloudFront). CPU contention with other workloads adds variance (visible in `benchmarkIndex`). LHCI Server would be a second UI and store, against the "visible QQQ tables, one home" rules. It stays possible as a `LabRunner` adapter (§3.2, phase P6). |
| **CrUX API** `POST https://chromeuxreport.googleapis.com/v1/records:queryRecord` | Google's field data: real Chrome users, **p75 over a rolling 28 days, updated daily (~04:00 UTC)**. Metrics include `largest_contentful_paint`, `interaction_to_next_paint`, `cumulative_layout_shift`, `first_contentful_paint`, `experimental_time_to_first_byte`, the LCP subparts and `round_trip_time`. Form factors `PHONE` / `TABLET` / `DESKTOP`. | Free, key required, **150 queries a minute per Cloud project**. **CrUX History API**: weekly collection periods, up to **40** (25 by default), updated Mondays. | **A page or origin is in CrUX only if it is publicly discoverable and "sufficiently popular"** (the threshold is not published). At about 44 pageviews a day, kof22 is almost certainly below it. The API answers "not found" and PSI omits `loadingExperience`. We store that as **NO_DATA**; we never hide it or substitute anything (§2.3). |
| **Our RUM** (`web-vitals` **v6.2.3**, attribution build) | `onLCP`, `onINP`, `onCLS`, `onFCP`, `onTTFB` (`onFID` is gone). Each report carries `value`, `rating` (thresholds LCP 2500/4000 ms, INP 200/500 ms, CLS 0.1/0.25, FCP 1800/3000 ms, TTFB 800/1800 ms), `navigationType`, and attribution. LCP: `target`, `url`, `timeToFirstByte`, `resourceLoadDelay`, `resourceLoadDuration`, `elementRenderDelay`. INP: `interactionTarget`, `interactionType`, `inputDelay`, `processingDuration`, `presentationDelay`, `loadState`, long-animation-frame script data. CLS: `largestShiftTarget`, `largestShiftValue`, `largestShiftTime`, `loadState`. The library recommends queueing metrics and flushing on `visibilitychange` → hidden. | Ours. Works at any traffic level. | **The field truth for kof22.** We control the beacon, consent gate, storage and aggregation already (`J/cms/analytics/**`). |

### 2.2 Trade-offs

| Question | PSI | Self-hosted Lighthouse | CrUX | Our RUM |
|---|---|---|---|---|
| Measures what visitors get over the internet (and later the CDN) | Yes (Google's network) | No (LAN path) | Yes | Yes |
| Works at kof22's traffic | Yes | Yes | **No** (ineligible) | Yes. The site-wide p75 is meaningful; per page only for the busiest pages (§7.4) |
| Accessibility, Best Practices, SEO | Yes | Yes | No | No |
| Opportunities and diagnostics ("what to fix") | Yes | Yes | No | Partly: attribution names the LCP element, the slow interaction and the shifting element |
| Run-to-run variance | Single run per call; mitigated by confirmation runs (§6.4) | Median of N runs | None (28-day aggregate) | Needs sample counts (shown) |
| Infra and ops | One API key | Chrome image, a Job, a Node 22.19 toolchain | One API key (the same project) | Frontend change plus analytics tables |
| Matches what Google uses for Search | Field part only | No | **Yes** (when available) | Same metrics and method, different population |

### 2.3 Handling "no CrUX data" honestly

- Each daily CrUX check writes or updates a `webperf_field_snapshot` row with `status = NO_DATA` and `last_checked_at`. Unchanged NO_DATA results update one row instead of adding a row per day.
- Everywhere CrUX would appear, the UI says: *"Google field data (CrUX): not available. Google publishes data only for pages and sites with enough Chrome traffic. Last checked 2026-10-12. Real-user figures below are from this site's own measurements."*
- When PSI returns `loadingExperience` with `origin_fallback: true`, the field data belongs to the origin, not the URL. It is stored with `scope = ORIGIN` and labelled that way.
- When data appears, the History API is used once to backfill up to 40 weekly periods (P4).

### 2.4 Recommended combination

**PSI (lab, all four categories, mobile and desktop) + our RUM (field) + CrUX (recorded, usually NO_DATA).** Self-hosted Lighthouse is deferred behind a `LabRunner` SPI. Every place that shows a number says which source it came from: Lab (PSI, Lighthouse version, date), Field (this site, n samples, 28 days), or Field (Google CrUX, collection period).

---

## 3. Architecture

### 3.1 Shape

```
                      ┌────────────────────── qbit-web-performance (in the backend JVM) ───────────────────────┐
 sitemap.xml ──────►  │ webperfSyncTargets ─► webperf_target                                                     │
 (UrlSource SPI)      │                                                                                          │
 schedules ────────►  │ enqueue ─► webperf_lab_run (QUEUED) ─► webperfRunQueuedTests worker ─► PSI API v5         │
 ESB kof22.cms.* ──►  │   (dedupe, not_before)                    │  (LabRunner SPI; concurrency 2, daily cap)     │
 deploy probe ─────►  │                                           ├─► webperf_lab_run (scores, metrics)           │
 qbit-cdn stage ───►  │ webperf_change_event                      ├─► webperf_lab_finding (opportunities, ...)    │
 user actions ─────►  │                                           ├─► webperf_lab_report (full LHR, gzip)         │
                      │                                           └─► evaluate budgets ─► webperf_regression       │
 CrUX API ─────────►  │ webperfRefreshFieldData ─► webperf_field_snapshot                                        │
                      │ RumSource SPI ◄── analytics_vital_stat (p75) ◄── analytics_vital ◄── /analytics/v1/collect │
                      │ widgets: Performance home, record "Performance" panel, summaries; Micrometer gauges      │
                      └───────────────────────────────────────────────────────────────────────────────────────────┘
 www.kof22.com (Next) ── web-vitals v6 ─► same-origin /api/e ─► backend collect (existing forwarder)
```

### 3.2 `WebPerfQBitConfig` (main settings)

| Setting | Default | Meaning |
|---|---|---|
| `backendName` | required | RDBMS backend for all tables |
| `tableNamePrefix` / `namePrefix` | `webperf_` / `webperf` | SQL and table prefix, and the prefix for process, widget and permission names |
| `appName`, `appLabel`, `parentAppName` | `webPerformance`, `Performance`, none | kof22 sets `parentAppName = "Content"` (the CMS app) |
| `siteBaseUrl` | required | Public origin under test, e.g. `https://www.kof22.com` |
| `urlSource` | `SitemapUrlSource(siteBaseUrl + "/sitemap.xml")` | SPI: the URLs to test |
| `contentResolver` | none | SPI: URL path → `(tableName, id)`. kof22 adapts the existing analytics `ContentPathResolver`, so one mapping serves both. |
| `contentTables` | none | Tables that get the record "Performance" panel and the "Test page speed" action |
| `labRunner` | `PsiLabRunner` | SPI. Later `LocalLighthouseLabRunner` (P6). |
| `googleApiKey` | `${env.WEBPERF_GOOGLE_API_KEY}` | One key for PSI and CrUX. Redacted in `toString()`. |
| `strategies`, `categories` | both; all four | |
| `concurrency`, `maxRunsPerDay`, `minSpacingMillis` | 2, 500, 2000 | Below the PSI limits of 25,000 a day and 400 per 100 s |
| `keyTargets` | home, top-level indexes, newest 3 posts | The tier tested daily and after deploys (`webperf_target.tier = KEY`) |
| `schedulerName` | none | Without it, the processes exist but are unscheduled |
| `esbTopics` | none | For kof22, `kof22.cms.processes` and `kof22.cms.tables` |
| `publishSettleSeconds`, `deploySettleSeconds` | 180, 300 | `not_before` delays, so caches, revalidation and later CDN invalidation can settle first |
| `deployProbes` | none | SPI list. kof22: `HttpBuildIdProbe(www/api/build)` plus the backend's own version (§6.3). |
| `rumSource` | auto when `qbit-cms-analytics` is present | SPI over the analytics vitals tables (§7) |
| `cruxEnabled` | true | |
| `reportRetentionDays` | 0 (keep forever) | §4.3 |
| `timeZone` | `UTC` | kof22: `America/Chicago` |

`validate()` checks the backend, an absolute `siteBaseUrl`, that the key is present when PSI or CrUX is on, the caps, and that every `contentTables` entry exists. A missing API key does not fail startup. The Performance home says *"No Google API key configured; lab and CrUX tests are off"*, the actions explain it on their Review screen, and RUM keeps working.

### 3.3 SPIs

| SPI | kof22 implementation | Generic default |
|---|---|---|
| `UrlSource` | `SitemapUrlSource` (`WF:src/app/sitemap.xml/route.ts`, 87 URLs today) | sitemap, or a static list |
| `ContentResolver` | Adapter over `J/metadata/analytics/ContentPathResolver` | none: targets have no record link |
| `LabRunner` | `PsiLabRunner` | `PsiLabRunner` |
| `DeployProbe` | `HttpBuildIdProbe` (frontend), `SelfVersionProbe` (backend) | none |
| `RumSource` | `AnalyticsRumSource` (`qbit-cms-analytics`) | none: the RUM sections say "install qbit-cms-analytics" |
| `ChangeEventSource` | `CdnStageChangeSource` when `qbit-cdn` is present | none |
| Alerts producer (consumed, not defined here) | The Alerts qbit's producer SPI (`raise` / `resolve` with a dedup key, severity and record links) | Not installed: regressions stay in-app and in Data Health |

---

## 4. QQQ tables

### 4.1 Naming (with #1083 and #1084 in mind)

The SQL prefix is `webperf_` and the QQQ table names equal the SQL names, following the `analytics_` precedent. The prefix stays separate from `cms_`, so this qbit is not tied to the CMS's prefix: a site without the CMS can install it, and the #1083 rename of kof22's CMS tables to `cms_` does not touch these tables. Every table has `id`, `create_date` and `modify_date`, and audit is on for the tables people edit (targets, budgets, regressions). Measurement tables are written only by processes, and their fields are read-only in the UI.

### 4.2 Tables

| # | Table (label) | Purpose | Key fields | Indexes |
|---|---|---|---|---|
| 1 | `webperf_target` (Tested Pages) | One public URL under test | `site_key`, `url`, `path`, `source` (SITEMAP / MANUAL / CONTENT), `content_table`, `content_id`, `label`, `tier` (KEY / STANDARD), `enabled`, `in_sitemap`, `last_seen_in_sitemap_at`, plus the denormalized latest values for list sorting: `latest_mobile_performance`, `latest_desktop_performance`, `latest_accessibility`, `latest_tested_at`, `open_regression_count` | unique (`site_key`, `url`); (`content_table`, `content_id`); (`enabled`, `tier`) |
| 2 | `webperf_lab_run` (Lab Runs) | One Lighthouse run of one URL and strategy | `target_id`, `url`, `final_url`, `strategy`, `runner` (PSI / LOCAL), `trigger` (SCHEDULE / PUBLISH / DEPLOY / MANUAL / SITE_AUDIT / CONFIRM / CHANGE_COMPARE), `trigger_ref`, `batch_id`, `status` (QUEUED / RUNNING / SUCCEEDED / RUNTIME_ERROR / FAILED / SKIPPED_QUOTA / MERGED), `not_before`, `queued_at`, `started_at`, `completed_at`, `attempts`, `http_status`, `error_message`, `lighthouse_version`, `chrome_user_agent`, `benchmark_index`, `score_performance`, `score_accessibility`, `score_best_practices`, `score_seo` (0–100, null when the category was not run), `fcp_ms`, `lcp_ms`, `tbt_ms`, `cls`, `speed_index_ms`, `lcp_element` (selector), `run_warnings`, `deployment_id` (frontend build at run time), `cdn_stage` | (`target_id`, `strategy`, `completed_at`); (`status`, `not_before`); (`batch_id`); (`completed_at`) |
| 3 | `webperf_lab_finding` (Findings) | Each non-passing or informative audit of a run | `run_id`, `target_id`, `strategy`, `category`, `audit_id`, `lighthouse_version`, `kind` (INSIGHT / OPPORTUNITY / DIAGNOSTIC / FAILED_AUDIT), `title`, `score`, `display_value`, `savings_ms`, `savings_bytes`, `metric_savings` (JSON: LCP / FCP / CLS / TBT / INP), `top_items` (JSON, first 5 URLs or selectors, truncated) | (`run_id`); (`audit_id`, `completed_date`); (`target_id`, `audit_id`) |
| 4 | `webperf_lab_report` (Lighthouse Reports) | The complete PSI response, gzip-compressed, one row per run | `run_id` (unique), `content` (BLOB, gzip JSON), `bytes`, `sha256`. File-download adornment: "Download report (.json)", which opens in the Lighthouse Viewer. | unique (`run_id`) |
| 5 | `webperf_field_snapshot` (Google Field Data) | A CrUX or PSI `loadingExperience` result | `source` (CRUX_API / CRUX_HISTORY / PSI), `scope` (URL / ORIGIN), `scope_key`, `form_factor` (ALL / PHONE / DESKTOP / TABLET), `status` (OK / NO_DATA / ERROR), `period_start`, `period_end`, `p75_lcp_ms`, `p75_inp_ms`, `p75_cls`, `p75_fcp_ms`, `p75_ttfb_ms`, `distributions` (JSON good / NI / poor per metric), `last_checked_at` | unique (`source`, `scope`, `scope_key`, `form_factor`, `period_end`) |
| 6 | `webperf_budget` (Budgets) | Thresholds that define a regression | `name`, `scope` (SITE / TIER / TARGET / CONTENT_TABLE / PATH_PREFIX), `scope_value`, `source` (LAB / RUM / CRUX), `strategy`, `metric` (PERFORMANCE_SCORE, ACCESSIBILITY_SCORE, BEST_PRACTICES_SCORE, SEO_SCORE, LCP, INP, CLS, FCP, TTFB, TBT), `comparator`, `threshold`, `severity` (WARNING / CRITICAL), `enabled`, `seeded` | (`enabled`, `source`) |
| 7 | `webperf_regression` (Regressions) | A detected drop or budget breach, kept as a record | `target_id`, `content_table`, `content_id`, `source`, `strategy` or `form_factor`, `metric`, `baseline_value`, `baseline_ref` (run ids or window), `current_value`, `current_ref`, `delta`, `budget_id`, `severity`, `status` (SUSPECTED / CONFIRMED / NOT_CONFIRMED / RESOLVED / DISMISSED), `cause_change_event_id`, `detected_at`, `confirmed_at`, `resolved_at`, `resolution_note` | (`status`, `severity`); (`target_id`, `metric`, `status`) |
| 8 | `webperf_change_event` (Site Changes) | Things that may move performance, drawn as markers on trends | `kind` (FRONTEND_DEPLOY / BACKEND_DEPLOY / CDN_STAGE / LIGHTHOUSE_VERSION / MANUAL), `component`, `from_value`, `to_value` (build id, stage or version), `observed_at`, `source` (PROBE / WEBHOOK / QBIT_CDN / USER), `note` | (`observed_at`); (`kind`, `observed_at`) |
| 9 | `webperf_comparison` (Before and After) | The result of comparing around a change event | `change_event_id`, `scope` (KEY tier / TARGET / SITE), `source` (LAB / RUM), `strategy` or `form_factor`, `metric`, `segment` (ALL or a country / device), `before_value`, `before_n`, `before_window`, `after_value`, `after_n`, `after_window`, `delta`, `delta_pct` | (`change_event_id`) |
| 10 | `analytics_vital` (Web Vitals), **in the analytics module** | One RUM metric report | §7.2 | §7.2 |
| 11 | `analytics_vital_stat` (Web Vitals Daily), **in the analytics module** | p50, p75 and p95 rollups, both per day and over a trailing 28 days | §7.3 | §7.3 |

**Seeded reference rows** (budgets only, not data): Google's "good" thresholds for each Core Web Vital, for both lab and RUM (LCP ≤ 2500 ms, INP ≤ 200 ms, CLS ≤ 0.1, FCP ≤ 1800 ms, TTFB ≤ 800 ms); every category score ≥ 90 as a WARNING on KEY pages; Performance < 50 as CRITICAL on KEY pages. These are `seeded = true`, editable and audited.

### 4.3 Retention and size

- **Forever** for runs, findings, field snapshots, regressions, change events, comparisons and RUM (the owner's analytics rule).
- **Lighthouse reports:** a full PSI response is about 0.5–1.5 MB of JSON, roughly 100–300 KB gzipped. The expected volume is about 15–20 k runs a year (§6.2), which is **2–4 GB a year** in PostgreSQL (TOAST, backed up with the CNPG cluster). The default is to keep them forever (`reportRetentionDays = 0`). A retention option exists in case the database grows (decision D8).
- **RUM:** ~44 pageviews a day × up to 5 metrics is about 80 k rows a year. That is negligible, and it follows the analytics indexing guidance (revisit at ~50 M rows).

### 4.4 Interaction with the #1083 `cms_` rename

`webperf_target.content_table`, `webperf_regression.content_table` and `analytics_vital.content_table` store CMS **table names**, which the #1083 cutover renames (for example `blog_post` → `cms_blog_post`). The same is already true of `analytics_daily_stat.content_table`. **Add one statement per table to the cutover rename script** (`WB:ops/cms-qbit-cutover/rename-to-cms.sql`, #1083 §3.6.1):

```sql
UPDATE webperf_target SET content_table = 'cms_' || content_table WHERE content_table IN (/* renamed tables */);
```

The same statement applies to `webperf_regression` and `analytics_vital`. It runs inside the planned outage, is covered by the row-count and checksum verification, and loses no data. If this feature ships after the cutover, nothing is needed.

---

## 5. Where it shows up (single-dashboard rule)

### 5.1 Ownership (to add to `WB:docs/admin-dashboards.md`)

| Page | What it gets | Owner? |
|---|---|---|
| **Performance home** `/app/webPerformance` (CMS → Performance) | Everything below in §5.2. | **Owner of all performance widgets** |
| Record screen of each content table | A "Performance" section (T2, like the analytics "Views" section) for **that record only** (§5.3) | Owner of per-record detail (the same pattern as "Views") |
| CMS home | One compact `cmsHomePerformance` summary: 4 tiles (mobile Performance median of KEY pages, site LCP p75 (RUM, 28 d), open regressions, last full audit), each linking to the Performance home. **No chart and no list.** | Summary only, using the owner's loader |
| Dashboard | One "Needs Attention" line: *"2 confirmed performance regressions"* → the Regressions list. Shown only when there is at least one. | Summary only |
| Health home | Nothing new as a widget. Data Health rules (`operations.webperf-regressions`, `operations.webperf-runner-stale`, `operations.webperf-quota`, `operations.webperf-errors`) flow into the existing Health alerts and pills. | n/a |
| Alerts qbit (when installed) | Confirmed regressions and operational problems are raised and resolved through its producer SPI (§8.3), and its own inbox and channels present them. They are alerts that link back here, not a copy of the Performance widgets. | Owner of alert delivery |
| Analytics app | Nothing. Traffic stays there; vitals are presented only on the Performance home, even though they are stored in analytics tables (§7). | n/a |

The trend chart and the regressions list deliberately stay only on the Performance home. The CMS home's summary links to them. That is the rule, so the summary is not a second copy.

### 5.2 Performance home (owner)

Widgets in top-to-bottom order. All are real data with honest empty states.

| Widget | Type, width | Content |
|---|---|---|
| **Site Scores** | HTML, 12 | For each strategy: the median of the latest KEY-page runs for each category (Performance, Accessibility, Best Practices, SEO) as colored score rings in the 0–49 / 50–89 / 90–100 bands, plus the Lighthouse version and "tested 3 h ago". Below 640 px: label and value rows. |
| **Core Web Vitals** | HTML, 12 | One row per metric (LCP, INP, CLS, FCP, TTFB): **Field (this site)** p75 over 28 d with a good / NI / poor bar and *n*; **Field (Google CrUX)** p75 or "not available"; **Lab (mobile)** median. The source label is on every value. |
| **Trend** | LINE_CHART, 12 | Weekly medians of mobile and desktop Performance (lab) and RUM LCP p75 (field), with `webperf_change_event` markers (deploys, CDN stages, Lighthouse upgrades) listed under the chart with dates. The chart starts at the first recorded week and draws nothing before it. |
| **Open Regressions** | TABLE, 12, ≤ 4 columns | Page (link to the record, or the URL), what changed (*"Mobile Performance 94 → 71 (confirmed, 3 runs)"*), since (with the suspected cause, e.g. "frontend deploy 3f2a…"), severity. "View all" goes to the Regressions table. |
| **Slowest Pages** | TABLE, 12, 3 columns | The worst 10 pages by mobile Performance, with LCP lab and the top finding. |
| **Top Opportunities** | TABLE, 12, 3 columns | Findings grouped by `audit_id` across the latest run of each page: the title, how many pages, total estimated savings. Links to filtered Findings. |
| **Field Data Status** | HTML, 12 | CrUX eligibility (NO_DATA with last checked, or periods held), RUM sample counts, and the data-quality notes (for example "visits behind the load-balancer address have no country", from the analytics geo status). |
| **Recent Runs** | TABLE, 12, 4 columns | The newest 5 runs with status, scores and trigger. "View all" goes to Lab Runs. |

The app's Data section lists the tables from §4.2 in this order: Tested Pages, Lab Runs, Findings, Regressions, Budgets, Site Changes, Before and After, Google Field Data, Lighthouse Reports.

### 5.3 Record "Performance" panel (page, blog post, project, project update, ...)

One HTML widget in a T2 section named "Performance", following the `ContentViewsRenderer` pattern (`J/cms/analytics/AnalyticsMetaData.java:237-246`).

- **States:** *Draft: no public URL, not tested* · *Never tested. Use "Test page speed"* · *Test queued 14:02 (mobile, desktop)* · the results.
- **Latest scores:** for mobile and desktop, four category scores, each linking to its run, and the run date.
- **Vitals for this page:** lab (LCP, CLS, TBT, FCP), **field (this site, 28 d)** p75 with *n*, or *"12 samples. Need 30 for a reliable p75"*, and CrUX URL-level data or "not available".
- **What to fix:** the top 5 findings by estimated savings (title, savings, the element or URL), linking to the full Findings list for this run.
- **Trend:** an inline SVG sparkline of the last 12 mobile Performance scores, with deploy markers. It is drawn on the server, with no external script and no inner scroll.
- **Open regressions** for this page, with links.

The "Test page speed" action is in the record's action menu (§6.1), not inside the widget.

---

## 6. What gets measured, when, and the actions

### 6.1 Actions (all `StreamedETLWithFrontendProcess`)

| Process (label) | Where | Permission | Review summary lines (examples) | Result |
|---|---|---|---|---|
| `webperfTestPageSpeed` **Test page speed** | Record and bulk action on every `contentTables` table and on `webperf_target` | `webperf.test` | "3 pages will be tested on mobile and desktop (6 runs)" · "1 record is a draft with no public URL and will be skipped" · "1 page was tested less than 10 minutes ago and is already queued (merged)" · "Today's runs: 42 of 500 (Google allows 25,000)" | "Queued 6 runs. Results usually take 1–2 minutes", linking to the filtered Lab Runs |
| `webperfRunSiteAudit` **Run site audit** | Table action on `webperf_target` | `webperf.test` | "87 pages × 2 = 174 runs; about 45 minutes at 2 at a time" · quota line | "Queued 174 runs as audit #12", with a link |
| `webperfSyncTargets` **Sync pages from sitemap** | Table action on `webperf_target`; also scheduled daily | `webperf.admin` | "4 new pages, 83 unchanged, 1 no longer in the sitemap (kept, marked not in sitemap, history kept)" | Counts with links |
| `webperfRetryRuns` **Retry** | Record and bulk action on `webperf_lab_run` (FAILED, RUNTIME_ERROR, SKIPPED_QUOTA) | `webperf.test` | "2 runs will be queued again" | |
| `webperfRefreshFieldData` **Refresh Google field data** | Table action on `webperf_field_snapshot`; also scheduled daily at 06:00 UTC | `webperf.admin` | "Will check the origin and 87 URLs × 3 form factors (~262 CrUX queries, under 150 a minute)" | "Origin: no CrUX data. 0 URLs with data." |
| `webperfCompareChange` **Compare before and after** | Record action on `webperf_change_event` | `webperf.view` | "Lab: 9 KEY pages, 3 runs each before and after already recorded (or N to queue)" · "Field: 28 days before vs since (412 vs 96 LCP samples)" | Rows in `webperf_comparison`, with a link |
| `webperfDismissRegressions` **Dismiss** / `webperfResolveRegressions` **Mark resolved** | Record and bulk action on `webperf_regression` | `webperf.manage` | "2 regressions will be dismissed" (a note is required as a process input field) | |

Hidden scheduled processes (never in the nav): `webperfRunQueuedTests` (every minute; the worker), `webperfScheduleKeyTests`, `webperfScheduleSiteAudit`, `webperfProbeDeploys`, `webperfEvaluateRum`. The RUM rollup extends the analytics module's existing rollup and its "Recompute selected days" table action. There is no new action for it.

### 6.2 Which URLs and when

**URLs:** every URL in the sitemap. Today that is 87: the home page, about, contact, blog and stream indexes, 19 posts, 55 project pages, and 6 personal pages. `webperfSyncTargets` keeps the list current, and `ContentResolver` links each URL to its CMS record. Admin, preview and draft paths are never tested. Pages that drop out of the sitemap stay, marked `in_sitemap = false`, with their history kept.

| Trigger | What runs | Expected runs |
|---|---|---|
| **Daily**, 03:30 local | KEY tier (~8 URLs) × 2 strategies | ~16 a day |
| **Weekly full audit**, Sunday 04:00 | every enabled target × 2 | ~174 a week |
| **On publish** (§6.3) | The published record's URL(s) plus the hub pages it changes (home, its index), × 2 | ~6 per publish |
| **After a deploy** (§6.3) | KEY tier × 2 | ~16 per deploy |
| **Confirmation** (§6.4) | Up to 2 extra runs for each suspected regression | small |
| **CDN stage change** (§6.5) | KEY tier × 2 × 3 runs before and after | ~100 per stage change |
| **Manual** | Whatever the owner selects | |

The total is about 50–70 runs on an average day, under 0.3% of the PSI quota. A worker with concurrency 2 and 2 s spacing finishes a full audit in about 45 minutes. `maxRunsPerDay` (500) is a safety cap: runs past it are `SKIPPED_QUOTA`, visible and retryable. They are never silently dropped.

**Queue semantics.** Every trigger inserts `webperf_lab_run` rows with `status = QUEUED` and a `not_before`. A second request for the same target and strategy while one is QUEUED becomes `MERGED` into it, and the trigger is noted on the surviving row. The worker claims rows with `SELECT … FOR UPDATE SKIP LOCKED`, so any number of backend replicas never run a test twice. On HTTP 429 or 5xx the run is retried with backoff (max 3 attempts). A Lighthouse `runtimeError` (for example `NO_FCP`, or a page that failed to load) is stored as `RUNTIME_ERROR` with the message. It is a real result, not a missing one.

### 6.3 Re-test on publish and after deploys

**On publish (ESB).** An `EsbTrigger` subscribes the hidden `webperfEnqueueFromCmsEvents` to:

- `kof22.cms.processes`: `PublishBlogPost` / `PublishProjectContent` COMPLETED. These are the clearest "it went live" signals. #1083 §2.8 adds a `PublishProjectContent` publication.
- `kof22.cms.tables`: UPDATE events on content tables where publish-relevant fields changed (status, published date, slug, body). This catches pages, which have no publish process.

It enqueues the record's URL(s), and its hub pages, with `not_before = now + publishSettleSeconds` (180 s). That gives Next revalidation time today, and gives CloudFront invalidation time later (#1084 targets under ~2 min). Delivery is at least once, and the merge rule makes duplicates harmless. **Without ESB**, a scheduled sweep (every 10 min) enqueues targets whose content `modify_date` is later than their `latest_tested_at`, so nothing silently stops working.

**After deploys (recommended: probes, no infra).**

- **Frontend:** `WF:next.config.ts` already pins `deploymentId` to the commit SHA (CDN P0, `WF@65c2a60`). Add a no-store `GET /api/build` route returning `{ "deploymentId": "<sha>" }`, which the backend polls in-cluster every 5 min. A change writes a FRONTEND_DEPLOY `webperf_change_event` and enqueues the KEY tier with `not_before = +300 s`.
- **Backend:** at startup the qbit compares the running version with the last BACKEND_DEPLOY event and records a new one if it differs.
- **Alternative (D7):** enable ArgoCD notifications (`GO:platform/argocd/values.yaml` has `notifications.enabled: false`) with an `on-deployed` webhook to an HMAC-protected backend route. That is more moving parts, and it is specific to this cluster. A Munitor post-deploy call is a third option; it would tie the qbit to the CI tool.

Each lab run also records the frontend `deployment_id` it saw, so every score is attributable to a build.

### 6.4 Regression detection (lab noise handled honestly)

PSI performs one Lighthouse run per call, and Lighthouse documents that scores move with network routing and host conditions. So:

1. **Baseline:** for each target and strategy, the median of the last 5 successful SCHEDULE / CONFIRM / SITE_AUDIT runs within 30 days, **on the same Lighthouse major version**. A Lighthouse version change writes a LIGHTHOUSE_VERSION change event and starts a new baseline. Audit-level trends never span major versions, because Lighthouse 13 renamed the audits.
2. **Suspect** when, against the baseline: Performance drops ≥ 10 points, or LCP rises ≥ 25% and ≥ 500 ms, or CLS rises ≥ 0.05, or TBT rises ≥ 200 ms, or a seeded or owner budget is breached.
3. **Confirm:** queue 2 CONFIRM runs and take the **median of 3**. If it still meets the rule, the regression becomes CONFIRMED and links the nearest preceding change event as the likely cause. Otherwise it becomes NOT_CONFIRMED and stays visible.
4. **Accessibility, Best Practices and SEO** results are close to deterministic. A drop of ≥ 5 points, or a newly failing audit, is CONFIRMED immediately and listed with the failing audit ids.
5. **RUM drift** (`webperfEvaluateRum`, daily): the trailing 7-day p75 is compared with the previous 28-day p75 for the site and for pages with ≥ `minSamples` (30) in both windows. Only a move into a worse rating band (good → NI → poor) opens a WARNING. Small samples never alert.
6. **Resolve:** a CONFIRMED regression resolves automatically when the median of the next 3 runs is back within the rule, and the record keeps its history.

### 6.5 CDN before and after (with `qbit-cdn`, #1084)

- When `qbit-cdn` is installed, `CdnStageChangeSource` observes `cdnHostname.stage` changes (OFF → HEADERS_ONLY → LIVE → LOCKED, #1084 §8.4) and writes CDN_STAGE change events.
- **Before:** setting a hostname to HEADERS_ONLY queues the KEY tier, 3 runs per strategy, as the lab baseline. RUM needs nothing extra; it already has the history.
- **After:** the switch to LIVE queues the same set at +1 h and +24 h. `webperfCompareChange` then writes `webperf_comparison` rows:
  - lab medians (TTFB from the LCP phases, LCP, FCP, Performance)
  - RUM p75 for 28 days before against the days since. **TTFB and LCP are segmented by country**, because the CDN mainly helps distant visitors.
- **Caveat:** country segmentation needs a real client IP. Today visits arrive as `proxy_ip` (SNAT, `WB:docs/design/cms-analytics-module.md`). #1084 P3 fixes it with `X-Real-IP` from CloudFront. Until then the comparison shows site-wide figures only and says why.
- PSI runs over the public internet, so lab runs measure through CloudFront exactly as visitors do, which a self-hosted in-cluster runner could not do (§2.2).

---

## 7. Real-user monitoring (RUM)

### 7.1 Frontend (Website-Frontend)

- Add `web-vitals@6` and use the **attribution build**. Next's built-in `useReportWebVitals` is not used, because it does not expose attribution.
- Load it in the existing `AnalyticsBeacon` client component through a dynamic `import()` after `load` / `requestIdleCallback`, so measurement does not slow the page it measures. The attribution build is a few KB gzipped.
- **The same gate as pageviews:** `mayTrack()` (DNT, GPC, `navigator.webdriver`), `isOptedOut()` (`analytics:ignore`) and the ignored prefixes (`/admin`, `/preview`, `/draft`, `/api`) in `WF:src/lib/analytics/beacon.ts`. If those say no, the library is never loaded.
- Report final values only (no `reportAllChanges`), and queue them. On `visibilitychange` → hidden, flush once with `navigator.sendBeacon('/api/e', …)` as a `text/plain` body (no preflight):

```json
{ "type": "vitals", "hostname": "www.kof22.com", "path": "/blogs/quick-bits/some-post",
  "navigationType": "navigate", "deploymentId": "3f2a9c1",
  "connection": { "effectiveType": "4g", "rtt": 50, "saveData": false },
  "metrics": [
    { "name": "LCP", "value": 1840, "rating": "good",
      "attribution": { "target": "main>article>img.hero", "url": "/_next/image?url=…&w=1200",
        "timeToFirstByte": 210, "resourceLoadDelay": 90, "resourceLoadDuration": 640, "elementRenderDelay": 900 } },
    { "name": "INP", "value": 96, "rating": "good",
      "attribution": { "interactionTarget": "nav>button.menu", "interactionType": "pointer",
        "inputDelay": 8, "processingDuration": 40, "presentationDelay": 48, "loadState": "complete" } },
    { "name": "CLS", "value": 0.02, "rating": "good",
      "attribution": { "largestShiftTarget": "footer", "largestShiftValue": 0.02, "loadState": "complete" } } ] }
```

- The forwarder (`WF:src/lib/analytics/forward.ts`) passes the new type through unchanged. It already adds the client IP, User-Agent, DNT and GPC headers, and enforces the 4 KB limit. Attribution strings are truncated in the browser (selectors to 200 characters, URLs to their path, query strings dropped except `w` on `/_next/image`, and third-party URLs to their origin), so a batch stays within 4 KB.
- **SPA navigations:** like CrUX, metrics belong to the **hard navigation's landing URL**. INP and CLS cover the whole page lifetime, including client-side route changes. The panel says so. `reportSoftNavs` (Chrome's experimental Soft Navigations API) stays off until it is stable (D11).

### 7.2 Backend: `analytics_vital` (analytics module, then `qbit-cms-analytics`)

The collector accepts `type: "vitals"` and runs the same pipeline as a pageview: validation, bot and automation drop, DNT and GPC drop, ignored host or path, rate limit. It links the report to **today's open visit** of the same daily-salted visitor hash, the lookup engagement pings already use. With no open visit, the batch is dropped as `DROPPED_NO_SESSION`, the same as an engagement ping without a visit. Nothing new identifies a visitor. Device, browser, OS, country and screen width come from the visit row and are not collected again.

| Field | Notes |
|---|---|
| `id`, `received_at`, `view_date`, `site_key`, `session_id` | `session_id` is the analytics visit (daily, unlinkable across days) |
| `path`, `content_table`, `content_id` | Resolved through the existing `ContentResolver` |
| `metric` (LCP / INP / CLS / FCP / TTFB), `value`, `rating` | CLS is unitless; the others are ms |
| `navigation_type` | navigate / reload / back-forward / back-forward-cache / prerender / restore |
| `attr_target` | LCP element, INP interaction target, or CLS largest-shift target |
| `attr_url` | LCP resource (normalized) |
| `attr_ttfb_ms`, `attr_load_delay_ms`, `attr_load_duration_ms`, `attr_render_delay_ms` | LCP subparts |
| `attr_interaction_type`, `attr_input_delay_ms`, `attr_processing_ms`, `attr_presentation_ms`, `attr_load_state` | INP |
| `attr_shift_value`, `attr_shift_time_ms` | CLS |
| `connection_type`, `rtt_ms`, `save_data` | From the Network Information API (Chromium only; null elsewhere) |
| `deployment_id` | The frontend build |

Indexes: (`view_date`, `metric`); (`content_table`, `content_id`, `metric`, `view_date`); (`path`, `metric`, `view_date`); (`session_id`). They are built `CONCURRENTLY` with H2 twins, as in the analytics changelog, and the changelog stays additive.

### 7.3 Backend: `analytics_vital_stat` (rollup)

The existing analytics rollup (every 300 s for today and yesterday, under an advisory lock, idempotent) also computes:

- `stat_date`, `site_key`, `window_days` (1 or 28), `metric`, `dimension` (ALL / PATH / CONTENT / DEVICE / COUNTRY / CONNECTION / DEPLOYMENT), `dimension_value`
- `samples`, `p50`, `p75`, `p95` (`percentile_cont` on PostgreSQL), and `good` / `ni` / `poor` counts

**The 28-day row is computed from raw rows**, not by averaging daily percentiles (they cannot be combined), so "p75 over 28 days" means the same thing it means in CrUX. "Recompute selected days" on the analytics Daily Stats table rebuilds the vitals for those days too.

### 7.4 Low-traffic honesty

- Every RUM figure shows *n*.
- A p75 is shown only when *n* ≥ `minSamples` (30, configurable). Below that the UI shows *"n = 12. Not enough samples for a reliable p75"*, plus the individual values when there are 10 or fewer.
- INP exists only for visits with an interaction, so its *n* is smaller than LCP's, and the panel says so.

### 7.5 Privacy and consent

- **The gate does not change:** no DNT or GPC signal, not automated, and not opted out. It is enforced in the browser and again on the server, exactly as for pageviews. If kof22 ever adds a consent banner, vitals follow the same `mayTrack()` switch with no separate path.
- **No new identifiers.** No cookies and no storage writes. Vitals join the existing daily-salted visit. The IP and User-Agent are used only in memory by the existing collector.
- **Data minimization:** attribution holds element selectors and same-site resource paths only, never text content, form values or query strings, and third-party URLs are cut to their origin.
- **Privacy page:** add one sentence to kof22's privacy page saying that page-speed measurements (load and responsiveness timings) are collected under the same rules as the existing anonymous analytics.
- Retention: forever, per the owner's analytics rule. The data is anonymous by design.
- **Lab tests** load public pages from Google's servers. They involve no visitor data, and they are filtered out of analytics by the existing bot and automation rules (Lighthouse is a headless or automated Chrome).

---

## 8. Permissions, secrets and infrastructure

### 8.1 Permissions

- `webperf.view` (read the app and tables), `webperf.test` (run tests and audits), `webperf.manage` (budgets, dismiss or resolve), `webperf.admin` (sync, field-data refresh, settings).
- kof22 maps `kof22-admins` to all of them.
- Measurement tables are insert-only by processes, with every field read-only in the UI.
- Multi-site hosts add `site_key` with a record security lock (the CMS qbit's `<prefix>Site` key type), so each site sees only its own targets.
- Core permission enforcement (#1061) applies to every action. Scheduled work runs under `CorePermissionEnforcer.runTrusted("webperf scheduler", …)`.

### 8.2 The Google API key (owner action; nothing has been created)

- **One key** in a Google Cloud project the owner chooses, with the *PageSpeed Insights API* and the *Chrome UX Report API* enabled and the key restricted to those two APIs. Both are free; no billing account is needed for the default quota.
- **Why a key is required:** on 2026-10-12, a keyless PSI request from this network got `429 … quota_limit_value: "0" … defaultPerDayPerProject` on the shared anonymous project.
- **Storage:** Infisical, seeded write-only from Praetor in exactly the pattern used for the kof22 Anthropic key (`PR:terraform/infisical-config/main.tf`, commit `23c9faf`):
  - folder `infisical_secret_folder.kof22_website_pagespeed` (`/kof22-website-pagespeed`) and `infisical_secret.kof22_website_pagespeed_api_key` (`api-key`)
  - `value_wo` from an ephemeral, sensitive `TF_VAR_kof22_pagespeed_api_key` read with `op read` from 1Password, plus `value_wo_version` for rotation and the same "never push an empty key" precondition
  - applied **only** with `tofu apply -target=infisical_secret_folder.kof22_website_pagespeed -target=infisical_secret.kof22_website_pagespeed_api_key`. The key is never in tfvars, state or a plan.
- **Cluster:** a `GO:tenants/kof22/website/externalsecret-pagespeed.yaml` ExternalSecret maps it to `WEBPERF_GOOGLE_API_KEY` in the backend Deployment. The config reads `${env.WEBPERF_GOOGLE_API_KEY}`, and `toString()` redacts it. No table stores the key.
- **Rotation:** bump `kof22_pagespeed_api_key_version` and re-apply with the same `-target` set. ESO picks it up on refresh.

### 8.3 Alerts (through the Alerts qbit; Slack optional)

**Owner direction (D9, amended 2026-10-12):** Slack is optional. Alerting goes through the generic Alerts / Manage-by-Exception qbit (`docs/specs/2026-10-12-qbit-alerts-design.md`, PR pending). That qbit provides monitors, alerts, subscriptions and pluggable channels (in-app, email, Slack, webhook, ...). This qbit only *produces* alerts and never chooses a channel.

**Producer integration (when the Alerts qbit is installed).** The regression engine (§6.4) and the operational checks call the Alerts qbit's producer SPI:

| Event | Call | Dedup key | Severity | Record links |
|---|---|---|---|---|
| A regression is CONFIRMED (lab, after the median of 3), or a category score drops | `raise` | `webperf:regression:<target_id>:<source>:<strategy>:<metric>` | The regression's `severity`: WARNING, or CRITICAL for KEY pages below their critical budget | The `webperf_regression` record, the target, the content record (`content_table` / `content_id`), the confirming runs |
| RUM drifts into a worse band (§6.4 step 5) | `raise` | `webperf:rum:<scope>:<metric>` | WARNING | The regression record, and the content record when the drift is page-level |
| A regression is RESOLVED automatically, or the user runs Dismiss or Mark resolved | `resolve` | The same key | n/a | The regression record (with its resolution note) |
| The runner is stale (no successful run in 48 h) | `raise` / `resolve` | `webperf:runner-stale` | WARNING | The Lab Runs list |
| A spike of run errors or SKIPPED_QUOTA (more than 10 in 6 h) | `raise` / `resolve` | `webperf:run-errors` | WARNING | The filtered Lab Runs list |

Raises are keyed. A regression that stays open is one alert, not one alert per evaluation, and resolving the regression clears its alert. Who is notified, by which channel, and any quiet hours or escalation are all configured in the Alerts qbit's subscriptions and channels, not here. For example, a kof22 subscription that sends WARNING and CRITICAL web-performance alerts to Slack is one possible channel choice, and it is made in the Alerts qbit.

**Without the Alerts qbit, nothing is lost.**
- Regressions remain `webperf_regression` records.
- The Performance home's Open Regressions widget and the Dashboard "Needs Attention" line show them.
- The Data Health rules (`operations.webperf-regressions`, `operations.webperf-runner-stale`, `operations.webperf-quota`, `operations.webperf-errors`) feed the Health home.

The producer integration is compiled against the Alerts qbit's API and activated only when that qbit is on the classpath. This is the same optional-dependency pattern as `qbit-cdn`.

**Optional: Prometheus metrics (feature `ALERT_METRICS`, off by default).** Some hosts want Grafana graphs or cluster-level alert rules. For them, the backend's existing `/metrics` (`J/metadata/autoload/metrics/JavalinMetricsRouteProvider.java`) can expose these gauges, with bounded cardinality:
- `webperf_open_regressions{severity,source}`
- `webperf_lab_score{tier="KEY",strategy,category}`
- `webperf_rum_p75{metric}`
- `webperf_lab_runs_total{status}`
- `webperf_last_success_timestamp_seconds`

kof22's backend is not scraped today. Enabling scraping means adding the `monitored-app` component (`GO:platform/_components/monitored-app/`) to `tenants/kof22/website`. PrometheusRules routed by Alertmanager are then one more optional channel, and alerting does not need them.

---

## 9. Packaging

| Option | Shape | For | Against |
|---|---|---|---|
| **A (recommended)** | A new repo `QRun-IO/qbit-web-performance` (`com.kingsrook.qbits.webperf`, Apache-2.0, qbit-template conventions: `WebPerfQBitProducer`, `WebPerfQBitConfig`, a host-included prefix-parameterized Liquibase changelog). The CMS plugs in through `UrlSource` and `ContentResolver`; `qbit-cms` ships the adapters in an optional package, the way #1083 §2.2 ships analytics adapters. RUM storage is in `qbit-cms-analytics`; this qbit reads it through `RumSource`. | Any QQQ site with public URLs can install it, CMS or not. It is consistent with #1083 D1, which kept analytics separate. It has its own release train. Lab tests have nothing to do with content modelling. Build it as a qbit from day one, so there is no second extraction like #98. | One more repo. Three qbits cooperate through SPIs. |
| B | `CmsFeature.PERFORMANCE` inside `qbit-cms` | One install for CMS sites | Ties a generic web capability to the CMS. It joins the CMS release train (21.5 weeks of extraction work), and non-CMS QQQ sites cannot use it. |
| C | Build it inside Website-Backend first, then extract (the analytics path) | Fastest first demo | Repeats the #98 extraction (license headers, host-coupling audit), which the owner has already paid for once. |
| D | Put it all in `qbit-cms-analytics` | RUM and lab together | That qbit is about traffic. PSI scheduling, Lighthouse reports and budgets would bloat it, and lab tests need no analytics. |

**Where RUM lives (D4).** Vitals are analytics *events*: same beacon, same gate, same visit, same retention, same rollup machinery. Storing them in the analytics module means one collection pipeline and one privacy story. The Performance app *presents* them, so presentation stays in one home. Until #98 extracts `qbit-cms-analytics`, the vitals changes land in `J/cms/analytics/**` in Website-Backend and move with the module.

**Feature flags (following #1083).** `WebPerfFeature`: `LAB` (PSI), `FIELD_CRUX`, `RUM_VIEW` (needs a `RumSource`), `TRIGGERS_ESB`, `DEPLOY_PROBES`, `CDN_COMPARE` (needs `qbit-cdn`), `ALERTS` (raises alerts through the Alerts qbit when it is installed), `ALERT_METRICS` (optional Prometheus gauges, off by default). A disabled feature produces no metadata, and its changesets are skipped by context.

---

## 10. Testing

- **PSI and CrUX contract fixtures:** real responses captured once with the owner's key, for a kof22 page, a 404, a page with a `runtimeError`, and a CrUX NOT_FOUND. They are checked in under `src/test/resources`, so parsing tests track Lighthouse 13's shape. A weekly CI smoke test against the live API runs only when a CI key exists.
- **Queue:** merge, `SKIP LOCKED` across two simulated workers, backoff on 429, the daily cap leading to SKIPPED_QUOTA.
- **Regression engine:** baseline, suspect, confirm with median of 3, and the Lighthouse major-version reset.
- **RUM:** the collector accepts `vitals`, drops on DNT/GPC/bot/no-session, truncation and the 4 KB limit, and `percentile_cont` p75 checked against a hand-computed set. H2 and PostgreSQL (Testcontainers), as in `RollupScenario`.
- **UI rules:** `LandingPagesTest` (no widget on two apps), `WidgetReadabilityGuardTest` for the new widgets, and the six-width screenshot pass (360 to 3200 px, sidebar open and closed).
- **Frontend:** Jest for gating (no import when `mayTrack()` is false), the payload builder and truncation, and flush on `visibilitychange`.

---

## 11. Cost, effort and phases

**Running cost:** **$0 a month.** PSI and CrUX are free at this volume (~50–70 PSI calls a day against 25,000, and ~260 CrUX calls a day against 150 a minute). There is no new compute; it runs in the existing backend pods. Storage is about 2–4 GB a year of Lighthouse reports in CNPG (backed up to B2 with the cluster), and RUM rows are negligible. The optional self-hosted runner (P6) would need about 1–2 GB of RAM while a Job runs.

| Phase | Scope | Estimate | Exit criteria |
|---|---|---|---|
| **P0: key and secret** (owner plus 0.5 d) | The owner creates the Cloud project key (PSI and CrUX only). Praetor write-only Infisical seed with `-target`. ExternalSecret and env var. One manual PSI and CrUX call to confirm, including the **real** CrUX answer for kof22. | 0.5–1 d | The backend pod has `WEBPERF_GOOGLE_API_KEY`; a manual call succeeds. |
| **P1: lab core** | `QRun-IO/qbit-web-performance` from qbit-template. Tables 1–4 and 6–7. PSI runner, queue worker, sitemap sync, daily and weekly schedules. Actions: Test page speed, Run site audit, Sync, Retry. Performance home (Site Scores, Slowest Pages, Top Opportunities, Recent Runs, Trend). Record Performance panel (lab part). kof22 wiring (`ContentResolver` adapter, `contentTables`). Seeded budgets. | 7–9 d | A full audit of 87 pages is recorded; record panels show real scores. |
| **P2: triggers, regressions, alerts** | ESB publish trigger plus the fallback sweep. `/api/build` and the deploy probes, and `webperf_change_event`. Regression engine with confirmation. Regressions widget, CMS home summary, Dashboard line, Data Health rules. Alerts qbit producer integration (raise and resolve with dedup keys) once that qbit is available. Optional Prometheus gauges. | 4–5 d | A publish leads to a re-test within ~4 min. A deploy leads to KEY re-tests. A forced regression appears on the Performance home and in Data Health. With the Alerts qbit installed, it is raised once and resolved when it clears. |
| **P3: RUM** | The analytics `vitals` event, `analytics_vital` and `analytics_vital_stat`, the rollup extension. Frontend `web-vitals` v6 beacon and privacy-page sentence. Core Web Vitals widget, panel field section, RUM drift. | 5–7 d | Real vitals with *n* on the home and per page; DNT/GPC verified to send nothing. |
| **P4: CrUX** | `webperf_field_snapshot`, daily refresh, PSI `loadingExperience` capture, the honest NO_DATA path, History backfill if eligible. | 1.5–2 d | The Field Data Status widget shows the real CrUX status. |
| **P5: CDN before and after** (after #1084 P2/P3) | `CdnStageChangeSource`, `webperfCompareChange`, `webperf_comparison`, country-segmented RUM. | 2–3 d | The `s3` / `www` cutovers each have a recorded comparison. |
| **P6 (optional): self-hosted runner** | `LocalLighthouseLabRunner` as a k8s Job (Lighthouse 13, Node 22.19), median of 5. Useful for pre-production URLs PSI cannot reach. | 3–4 d | The same tables, `runner = LOCAL`. |

**Total P0–P5: about 20–27 engineer-days (≈ 4–5 weeks)**, or 3–4 more days with P6. **Sequencing:**

- P0–P2 do not depend on #1083 or #98.
- P3 touches the analytics module, so do it before #98's extraction or right after it, not during.
- P5 waits for #1084 P2/P3.
- **The P2 alert integration depends on the Alerts qbit** (`docs/specs/2026-10-12-qbit-alerts-design.md`) shipping its producer SPI. P2 can ship before then: regressions show in-app and in Data Health only until the Alerts qbit is installed, which switches on the producer calls with no schema change.
- If the #1083 cutover happens after P1, its rename script gains the §4.4 statements.

### 11.1 Risks

| Risk | Mitigation |
|---|---|
| PSI changes its response shape (Lighthouse 12 → 13 removed and renamed audits) | Findings are keyed by `(lighthouse_version, audit_id)`; the full response is kept; contract fixtures; the version change is recorded as a change event. |
| Lab noise causes false alerts | Confirmation (median of 3), thresholds that need both a relative and an absolute change, and severity bands. |
| CrUX never covers kof22 | Designed for: RUM is the field truth, and NO_DATA is stated. |
| RUM samples too small per page | Show *n*; minimum samples before any p75 or alert. |
| The quota changes or the key is revoked | Daily cap, SKIPPED_QUOTA rows, the `WebPerfRunErrors` alert, and the Health rule. |
| The beacon slows the page | Dynamic import after load or on idle; a single `sendBeacon` on hide. Measured by this very system (its own lab runs). |

---

## 12. Decision log (owner, 2026-10-12: all recommendations accepted)

| # | Decision | Options | Decided (as recommended) |
|---|---|---|---|
| **D1** | Data sources | A) PSI lab + own RUM + CrUX recorded honestly · B) PSI only · C) self-hosted Lighthouse + RUM | **A.** It is the only combination that gives "what to fix" (lab, four categories), real field truth at kof22's traffic (RUM) and Google's view when it exists (CrUX), at $0. |
| **D2** | Lab runner | A) PSI API v5 · B) self-hosted Lighthouse / LHCI in-cluster · C) both now | **A, with a `LabRunner` SPI** and B as optional phase P6. PSI measures over the internet (and later through CloudFront), is free at 174 runs per audit, and needs no Chrome pods. In-cluster runs measure the LAN path, and LHCI Server would be a second UI and store. |
| **D3** | Packaging | A) new `qbit-web-performance`, built as a qbit from day one · B) `CmsFeature.PERFORMANCE` in `qbit-cms` · C) build in Website-Backend, extract later · D) inside `qbit-cms-analytics` | **A** (Apache-2.0, `QRun-IO`). It is reusable by any QQQ site, keeps the CMS train lean, and avoids repeating the #98 extraction. |
| **D4** | Where RUM vitals are stored | A) analytics module (`analytics_vital*`), read through `RumSource` · B) the webperf qbit's own ingest endpoint and tables | **A.** One beacon, one consent gate, one visit model, one retention rule; presentation still has a single home. |
| **D5** | Where the Performance home sits | A) CMS → Performance sub-app · B) Admin → Performance · C) a top-level app | **A**, next to Analytics (also a CMS child). It is about published pages, and editors see it where they work. |
| **D6** | Schedule and run budget | A) daily KEY tier + weekly full audit + on publish + after deploy, cap 500 a day · B) daily full audit (~174 a day) · C) on demand only | **A.** About 50–70 runs a day; fresh where it matters, complete weekly. B is affordable too if the owner wants daily per-page trends. |
| **D7** | How deploys are detected | A) build-id probe (`/api/build` on the frontend, the backend version at startup) · B) enable ArgoCD notifications with a webhook · C) Munitor post-deploy call | **A.** No infra change, works on any host, and records the build on every run. ArgoCD notifications are disabled today. |
| **D8** | Lighthouse report retention | A) keep everything forever in PostgreSQL (~2–4 GB a year) · B) full reports 400 days, summaries and findings forever · C) reports in MinIO | **A**, which fits "retain forever" and keeps reports as a visible, downloadable QQQ table. Revisit B if the database size becomes a concern. |
| **D9** | Alerts | A) Prometheus to the existing Slack routes, plus Data Health · B) in-app only (the Regressions list and Dashboard line) · C) raise and resolve through the generic Alerts qbit's producer SPI when it is installed, and in-app plus Data Health otherwise; Slack and Prometheus are optional channels | **C (owner amendment, 2026-10-12: Slack must be optional).** A was the original recommendation; the owner replaced it with the Alerts qbit approach (§8.3). |
| **D10** | Google API key | A) one key, restricted to the PSI and CrUX APIs, in a Cloud project the owner picks; Infisical write-only seed with `-target` (the Anthropic-key pattern) · B) separate keys per API | **A.** The owner names the Cloud project and the 1Password item. Nothing has been created. |
| **D11** | RUM on client-side navigations | A) landing URL only (CrUX parity, stable) · B) experimental soft-navigation reporting (`reportSoftNavs`) | **A** now; revisit B when Chrome ships Soft Navigations as stable. |
| **D12** | Default budgets | A) seed Google's "good" thresholds (LCP 2.5 s, INP 200 ms, CLS 0.1, FCP 1.8 s, TTFB 0.8 s) and category scores ≥ 90 (WARNING), Performance < 50 on KEY pages (CRITICAL) · B) no seeded budgets; owner-defined only | **A.** These are editable records, so the owner can tighten them after seeing real baselines. |
| **D13** | Lighthouse categories per run | A) all four on every run · B) Performance only, the others weekly | **A.** It costs nothing extra (one call), and accessibility and SEO regressions are often caused by content edits, which publish-triggered runs catch. |

---

## Appendix A: Sources (fetched 2026-10-12)

- PSI API v5, get started and `runPagespeed` reference: https://developers.google.com/speed/docs/insights/v5/get-started, https://developers.google.com/speed/docs/insights/rest/v5/pagespeedapi/runpagespeed
- PSI release notes (Lighthouse 13.0 since 2025-10-20; Lighthouse 12.0 since 2024-05-10, which removed PWA): https://developers.google.com/speed/docs/insights/release_notes
- PSI quota of 25,000 a day and 400 per 100 s: widely documented, e.g. https://dev.to/addyosmani/monitoring-performance-with-the-pagespeed-insights-api-33k7. Confirm in the Cloud console quota page at P0.
- What's new in Lighthouse 13 (insight audits, removed audits, Node 22.19): https://developer.chrome.com/blog/lighthouse-13-0
- Lighthouse performance scoring (weights, bands, variability): https://developer.chrome.com/docs/lighthouse/performance/performance-scoring
- CrUX API (150 queries a minute, metrics, form factors, daily update): https://developer.chrome.com/docs/crux/api
- CrUX History API (40 weekly periods, Monday updates): https://developer.chrome.com/docs/crux/history-api
- CrUX methodology (eligibility: publicly discoverable and sufficiently popular): https://developer.chrome.com/docs/crux/methodology
- `web-vitals` (v6.2.3, attribution, `reportSoftNavs`, thresholds, flush on `visibilitychange`): https://github.com/GoogleChrome/web-vitals
- Lighthouse CI configuration and server (median of runs, `psiCollectCron`, storage): https://googlechrome.github.io/lighthouse-ci/docs/configuration.html, https://github.com/GoogleChrome/lighthouse-ci/blob/main/docs/server.md
- Repository facts: `WB:docs/design/cms-analytics-module.md`, `WB:docs/admin-dashboards.md`, `J/metadata/esb/WebsiteEsbMetaData.java`, `J/cms/analytics/AnalyticsMetaData.java`, `WF:src/lib/analytics/beacon.ts`, `WF:src/lib/analytics/forward.ts`, `WF:next.config.ts`, `GO:platform/argocd/values.yaml`, `GO:platform/_components/monitored-app/`, `PR:terraform/infisical-config/main.tf`.
