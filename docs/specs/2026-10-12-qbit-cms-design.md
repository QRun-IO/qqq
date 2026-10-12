# QQQ CMS application qbit: Assessment and Design Spec

Status: **Approved** · drafted 2026-10-12, owner decisions 2026-10-12 · Author: James Maes (drafted with Claude)
Targets: QQQ 4.1.x (4.1.0-RC.4 or later), QQQ Next UI 1.0.x (`v1.0.0-RC.14` or later), proposed new repo `QRun-IO/qbit-cms`
Source assessed: `KofTwentyTwo/Website-Backend` `origin/main` @ `ec5bb65` (2026-10-12, PR #109) and `KofTwentyTwo/Website-Frontend` `origin/main` @ `d670cc1` (PR #34)

Scope: this document is **assessment and design only**. It changes no code, no schema and no live system. The owner decided all open questions on 2026-10-12 (table below, log in §10).

Path conventions used below:

- `WB:` is the Website-Backend repo root. `J/` is `WB:src/main/java/com/kof22/website/`, `T/` is `WB:src/test/java/com/kof22/website/`, `LB/` is `WB:src/main/resources/db/liquibase/`.
- `WF:` is the Website-Frontend repo root.
- `qqq:` is this repo (`origin/develop`). Other qbits are named by repo.

## Owner decisions (2026-10-12)

The owner decided all fifteen questions on 2026-10-12. These decisions override anything below that contradicts them, and the sections they touch have been updated. §10 is now the decision log.

| # | Decision | Effect on the design |
|---|---|---|
| D1 | **Packaging (A):** one `qbit-cms` artifact with `CmsFeature` flags, plus a separate `qbit-cms-site-api` artifact. Analytics stays its own qbit, `qbit-cms-analytics` (#98). | §2.3 |
| D2 | **License (A):** the extracted CMS code is relicensed **Apache-2.0** and developed in public at `QRun-IO/qbit-cms`. kof22-specific code stays proprietary in Website-Backend. | Clears the license blocker before any code moves (§8). |
| D3 | **kof22 table names (B): rename to the `cms_` prefix at cutover.** This overrides the original recommendation to keep the names. | §3.6 is redesigned around a reversible, transactional rename migration, rehearsed on a production snapshot, run in a booked maintenance window, with compatibility views and a reverse migration for rollback. Phase 4 grows from 1.5 to 3 weeks. |
| D4 | **Default prefix (A):** `cms_`, the same as kof22 after D3, so kof22 runs the standard schema with no legacy mappings. | §2.4 |
| D5 | **Schema ownership (A):** the qbit ships a prefix-parameterized changelog that the host includes and runs, plus generated reference DDL. | §3.2 |
| D6 | **Site API in the qbit (A):** a separate `qbit-cms-site-api` artifact with configurable name, path, hosts and auth. The v1 contract is frozen and kof22 keeps `api.kof22.com` unchanged. | §2.7. Public resource names are pinned (R14), so the D3 rename cannot change `/v1`. |
| D7 | **Multi-site (A):** `FixedSiteResolver` by default; host-based resolution and admin site scoping arrive in phase 6. | §2.6 |
| D8 | **Frontend (A):** headless contract now; `@qrun-io/cms-next` package and starter in phase 5, with Website-Frontend as the first consumer. | §4.1 |
| D9 | **Preview and revalidation (A):** signed one-record preview tokens and HMAC revalidation webhooks, both opt-in. kof22 stays `force-dynamic`. | §4.2, §4.3 |
| D10 | **Data Health (A):** CMS rules ship in the qbit behind a rule-provider interface; the framework stays in the host. | §1.2 |
| D11 | **Authoring, contact and app association (A):** opt-in features of the API artifact. | §2.7 |
| D12 | **Publisher token auth (A):** a generic, config-driven verifier in the publishing feature. | §2.4, §3.6 (the publisher CLI is updated for the renamed tables and processes) |
| D13 | **Deprecated `/public-api/` (A):** not migrated. It stays in the host until its 2027-01-31 sunset, with its API table names pinned to the old names so the rename does not change it. | §3.6 |
| D14 | **Sequencing (A):** #107 (needs RC.4) and #98 finish first, then phase 0. | §8 |
| D15 | **Acceptance target (A):** a blank `qqq-app-starter` app in CI (phase 1), plus a real second adopter in phase 6. | §8 |

## 0. Summary

The kof22 CMS is a substantial, working QQQ application: about 38 CMS tables, 2 transactional publishing processes, 4 media/photo processes, a curated public **Site API v1** with an authoring API for an iOS app, ESB change events, Quick Search indexing, record-security-lock based public visibility, 126 CMS and publishing Data Health rules, a CMS home with seven sub-app homes, and a privacy-first analytics module. It is all built *inside* the host app: tables are unprefixed (`site`, `page`, `media`, `menu`, `redirect`, `project`...), registered by package scan against a hard-coded backend name, and wired together by a fixed, order-sensitive `defineInstance()` (`J/metadata/WebsiteMetaDataProvider.java:85-232`).

One module, CMS analytics (`J/cms/analytics/**`), has already been reshaped into a qbit: a `QBitMetaDataProducer<CmsAnalyticsConfig>`, a prefix-parameterized Liquibase changelog, and SPIs for host-specific behaviour (`docs/design/cms-analytics-module.md`, Website-Backend#98). **This design generalizes that proven pattern to the whole CMS.**

What it takes, in one paragraph: about **3.5 weeks of in-repo refactoring** in Website-Backend to remove kof22 constants and raw table names behind a config object and five SPIs (§1.4), then **~10 weeks** to stand up `QRun-IO/qbit-cms` (core, media, blog, pages, menus, projects, photos, publishing) and `qbit-cms-site-api`, a **~3 week cutover** of kof22 that renames its tables to `cms_` in one reversible, rehearsed transaction with no data copies (§3.6, D3), and **~5 weeks** for the optional frontend package, preview and revalidation, and multi-site host resolution. Total about **21.5 engineer-weeks** (one senior engineer with AI assistance), sequenced after #107 (core permission enforcement) and #98 (analytics extraction).

Design rules (these restate the owner's standing rules and the qbit conventions found in §2.1):

1. **qbit data is normal QQQ data.** Every CMS table is a standard, visible QQQ table in the host's backend, secured by QQQ permission rules and record security locks. Nothing is hidden, and there is no side-channel store.
2. **Processes are never in the left nav.** Every CMS process is a table or record action (`withTableName` plus a `QMenu` slot), as `J/metadata/menus/WebsiteProcessActions.java` already enforces for kof22.
3. **One landing page, no duplicated widgets.** The qbit produces one CMS app with one home and one home per sub-app. It never sets the instance `homeAppName`; the host owns its landing page.
4. **Readable at every width; real data only.** Widgets follow the readable-widget rules (`qqq:docs/adoption-checklist.adoc` §6). Production installs get no seeded or sample content; demo data is a separate opt-in Liquibase context.
5. **The host owns the database.** The qbit ships a prefix-parameterized changelog that the host includes and runs (the QQQ qbit convention). It never runs DDL at startup.
6. **Install, upgrade and uninstall never lose data.** The qbit never recreates existing tables (`MARK_RAN` preconditions), brings an existing schema to the standard prefix by renaming in place rather than copying, appends changesets only, and has no automatic drop.
7. **Headless first.** The Site API is the product contract (semver, `/v1`). A Next.js package and starter are optional conveniences on top of it.

---

## 1. Inventory

### 1.1 What exists today (counts)

| Area | Where (Website-Backend) | Size |
|---|---|---|
| CMS entities + enums | `J/model/cms/` (33 files), `J/model/publishing/` (4) | ~38 tables (§1.2) |
| CMS app, sub-apps | `J/metadata/autoload/apps/CMSAppMetaDataProvider.java` (app `Content`, label `CMS`, sub-apps `cmsSites`, `cmsPages`, `cmsBlog`, `cmsNavigation`, `cmsMedia`, `cmsPhotography`, `cmsProjects`, L74-88) | 1 app, 7 sub-apps |
| CMS home + sub-app homes | `J/metadata/cmshome/` (10 files: `CmsDashboardMetaData`, `CmsHomeWidgets`, `CmsSectionWidgets`, `CmsLibraryWidgets`, `DashboardWidget`, `DashboardHtml`, `RecordLabels`, `ContentInventory`...) | ~40 widgets |
| Publishing | `J/workflows/publishing/` (`PublishBlogPost` 751 lines, `PublishProjectContent` 974 lines, `RelatedVersions`, `ContentUrlMigration`), `J/metadata/publishing/WebsitePublishingMetaData.java` | 2 processes, 4 bookkeeping tables |
| Media + photos | `J/workflows/media/` (4: `ProcessMediaMetadataETL`, `UploadCmsImage`, `CmsImageUpload`, `CmsMediaLinks`), `J/workflows/photography/` (1), `J/utils/` (3 image processors), `J/model/cms/CmsFilePreviews.java` | 4 processes, renditions, FILE_PREVIEW |
| Site API v1 | `J/siteapi/**` (25 files incl. `http/SiteApiRouteProvider.java`, 912 lines), `J/metadata/autoload/siteapi/SiteApiMetaDataProvider.java` | ~20 resources, 10 custom routes, 8 authoring endpoints |
| Public visibility | `J/metadata/PublicContentPersonalizer.java`, `J/siteapi/SiteApiTables.java` | 4 security key type pairs |
| Content stream | `J/metadata/ContentStreamMetaData.java`, SQL view `LB/changesets/0009-content-stream.sql`, `0020-gallery-stream-scope.sql` | 1 view-backed table |
| Machine publisher auth | `J/metadata/autoload/auth/Publisher*` (3 files) | 1 auth module |
| ESB | `J/metadata/esb/WebsiteEsbMetaData.java` | 3 destinations, 16 tables, 1 trigger |
| Quick Search | `J/metadata/search/WebsiteQuickSearchMetaData.java` + `@QuickSearchable` on 14 entities | 1 index |
| Data Health (CMS rules) | `J/datahealth/rules/Cms*Rules.java` (7) + `PublishingRules` | 126 rules (9+18+24+21+10+17+15+12) |
| Analytics | `J/cms/analytics/**` (42 files) | already qbit-shaped, #98 |
| Schema | `LB/changelog.yaml`, `LB/changesets/` (22 files, 152 changesets), `LB/datasets/` (26), `LB/cms-analytics/` (13) | all host-owned |
| Deprecated public API | `J/metadata/autoload/api/PublicApiMetaDataProvider.java` (`/public-api/`, sunset 2027-01-31) | not migrated (D13) |

Stack: QQQ BOM `4.1.0-RC.3`, Next `1.0.0-RC.14` (`WB:pom.xml:47-48`), `qbit-quick-search 1.1.0-RC.1`, `qbit-sql-workbench 1.0.0-RC.1` (both qbits on Maven Central), `qqq-esb` on Artemis, Liquibase 4.33, Java 21. 217 main and 79 test Java files. License: proprietary, "All Rights Reserved" (`WB:LICENSE`), with only `cms/analytics/**` relicensed Apache-2.0 for extraction (`docs/design/cms-analytics-module.md` §"License decision (2026-10-12)").

### 1.2 Classification

Legend: **(a)** generic CMS, moves into the qbit · **(b)** kof22-specific, stays in the host as config or customization · **(c)** shared infrastructure, belongs in QQQ core, Next, or another qbit.

#### Tables

| Group | Tables (entity) | Class | Notes |
|---|---|---|---|
| Sites | `site`, `domain`, `redirect`, `redirect_type` | (a) | `domain.fqdn`/`isPrimary` becomes the multi-site host map (§2.6). `redirect_type` values (CMS301 etc.) are reference data seeded by the qbit. |
| Pages | `page`, `page_type`, `page_section`, `page_section_type`, `snippet` | (a) | Section-type *rows* that are kof22-specific (`HARDWARE_TABLE`, `GITHUB_INFO`, `BUILD_COMMANDS`, `WORKFLOW`, `ARCHITECTURE`, `RELEASE_HISTORY`, `TECH_STACK`) are **(b) data**: they stay in the host's datasets. The qbit seeds only generic types. |
| Blog | `blog`, `blog_type`, `blog_post`, `blog_post_tag_int`, `blog_tag`, `author`, `blog_post_media` | (a) | `blog_post_media` is defined in `J/siteapi/SiteApiTables.java:54`; it moves to the blog feature. |
| Revisions | `blog_post_version` (`ContentChannel`, `ContentFormat`, `ContentVersionStatus`) | (a) | One table holds versions for posts, projects and updates (three nullable FKs, three UKs). Keep it as is; it is the "related versions" model. |
| Menus | `menu`, `menu_item` | (a) | Self-join exposed join is commented out with a TODO (`J/model/cms/MenuItem.java`); fix on the way. |
| Media | `media`, `media_type` | (a) | Rendition URL columns (`thumbnailUrl`, `galleryUrl`, `slideshowUrl`) stay. |
| Photos | `photo`, `photo_album`, `photo_album_folder`, `photo_tag`, `photo_tag_int` | (a) | EXIF fields generic. Photo "scores" UI is frontend-only (b). |
| Projects | `project`, `project_category`, `project_update` | (a) | `ProjectSection` enum (`SOFTWARE`/`PERSONAL`) is kof22 IA: generalize to a free `section` code with a host-seeded PVS (refactor R12). |
| Stream | `content_stream` (SQL view) | (a) | The view computes public URLs in SQL with kof22 URL shapes (`LB/changesets/0009-content-stream.sql:75-121`); see R6. |
| Publishing | `blog_publication`, `blog_publication_receipt`, `project_publication`, `project_publication_receipt` | (a) | Built as explicit `QTableMetaData` (`J/model/publishing/*`). |
| Site API support | `site_api_idempotency`, `site_api_upload`, `contact_message` | (a) | Site API artifact (D6). |
| Analytics | `analytics_{salt,session,pageview,daily_stat,hourly_stat}` | (a), separate qbit | `qbit-cms-analytics` per #98. |
| Audit, Quartz, OAuth sessions, quick-search, sql-workbench | `audit*`, `qrtz_*`, `qqq_user_session*`, `quick_search_*`, `sql_workbench_*` | (c) | Owned by QQQ core and other qbits; already prefixed. Stay host-owned. |

#### Code

| Component | Path | Class | Becomes |
|---|---|---|---|
| Entities and table customizers | `J/model/cms/*`, `J/model/publishing/*` | (a) | `com.kingsrook.qbits.cms.<feature>.model` |
| `CmsFilePreviews` (FILE_PREVIEW adornments) | `J/model/cms/CmsFilePreviews.java` | (a) | uses core FILE_PREVIEW (4.1.0-RC.3) |
| `CmsMediaLinks` (featuredMediaId → URL pre-insert/update customizers on 7 tables) | `J/workflows/media/CmsMediaLinks.java:47-63` | (a) | media feature |
| `CMSAppMetaDataProvider`, CMS homes | `J/metadata/autoload/apps/CMSAppMetaDataProvider.java`, `J/metadata/cmshome/*` | (a) | `CmsAppMetaDataProducer`, `home/` |
| `DashboardHtml`, `WidgetHtml` readable-widget helpers | `J/metadata/cmshome/DashboardHtml.java`, `J/cms/analytics/widgets/WidgetHtml.java` | (c) | upstream to `qqq-backend-core` (a small `widgets.html` helper) or Next widget types; used by ~20 host classes today (#98 blocker 2) |
| `DashboardWidget.Context` JDBC helper (currently backed by `AnalyticsRepository`) | `J/metadata/cmshome/DashboardWidget.java` | (c) | a tiny core or qbit-local JDBC helper; #98 prep |
| Publishing processes, `RelatedVersions`, `ContentUrlMigration` | `J/workflows/publishing/*` | (a) | publishing feature |
| Media and photo processes, image processors | `J/workflows/media/*`, `J/workflows/photography/*`, `J/utils/*` | (a) | media and photos features behind a `CmsStorage` SPI (R7) |
| `PublicContentPersonalizer` (public visibility locks) | `J/metadata/PublicContentPersonalizer.java` | (a) | `CmsVisibility` (locks driven by config + site resolution) |
| Site API (resources, routes, OpenAPI shaper, docs, authoring, contact) | `J/siteapi/**`, `J/metadata/autoload/siteapi/*` | (a) | `qbit-cms-site-api` artifact |
| `ApiProblem` error envelope, `Idempotency-Key`, semver/`versions.json`, OpenAPI shaping | `J/siteapi/http/ApiProblem.java`, `SiteApiOpenApiShaper.java` | (c) candidates | keep in the site-api artifact for v1; propose upstreaming to `qqq-middleware-api` later |
| Machine publisher bearer auth | `J/metadata/autoload/auth/PublisherTokenVerifier.java`, `PublisherTokenRouteProvider.java`, `PublisherOAuth2AuthenticationModule.java` | (a) generalized | publishing feature, config-driven issuer/audience/groups/allow-list |
| ESB publications + media trigger | `J/metadata/esb/WebsiteEsbMetaData.java` | (a) the CMS part; (b) provider/broker | qbit declares publications and the trigger against host-named destinations |
| Quick Search annotations | `@QuickSearchable` on 14 entities | (a) | stay on entities; registration is the host's (optional dependency) |
| CMS Data Health rules | `J/datahealth/rules/Cms*Rules.java`, `PublishingRules.java` | (a) rules, (c) framework | the Data Health *framework* (`J/datahealth/*`) is host code today; see D10 |
| Analytics module | `J/cms/analytics/**` | (a) | `qbit-cms-analytics` (#98) |
| `Kof22Analytics`, `ContentPathResolver` | `J/metadata/analytics/*` | (b) glue, (a) resolver | `qbit-cms` provides `CmsContentResolver`/`CmsSiteResolver` implementations of the analytics SPIs |
| Bootstrap, backends, branding | `J/Server.java`, `J/metadata/WebsiteMetaDataProvider.java`, `J/metadata/branding/*` | (b) | stays |
| Landing Dashboard, Admin homes | `J/home/*`, `J/metadata/adminhome/*` | (b) | stays (host owns landing page, rule 3) |
| Authentik customizer, admin OAuth | `J/metadata/autoload/auth/AuthentikAuthenticationCustomizer.java`, `AdminSiteAuthMetaDataProvider.java` | (b) | stays; calls the qbit's `CmsSecurity.applySessionPermissions(...)` helper (§6.3) |
| `ProjectCodeRouteProvider` (GitHub code view) | `J/projects/*` | (b) | stays |
| Seed tools | `J/tools/ImportSeed*Main.java` | (b) | stay |
| SQL workbench, scheduler, quick-search glue | `J/metadata/{sqlworkbench,scheduler,search}/*` | (b) | stay |
| `WebsiteTableMenus` (Next RC.11 menu workaround) | `J/metadata/menus/WebsiteTableMenus.java:30-41` | (c) | delete once Next ≥ RC.14 is confirmed to show Developer/Audit without it |
| `WebsiteProcessActions` (no processes in nav) | `J/metadata/menus/WebsiteProcessActions.java` | (c) | the qbit follows the rule itself; a core "processes are actions only" instance flag is a candidate |
| `LiquibaseRunner` | `J/startup/LiquibaseRunner.java` | (b) dead | no caller in main (verified); migrations run from `docker/Dockerfile.migrations` |
| Deprecated public API | `J/metadata/autoload/api/PublicApiMetaDataProvider.java` | (b) | stays in host until sunset, then deleted (D13) |

### 1.3 Coupling hotspots (verified at `ec5bb65`)

| Kind | Where | What |
|---|---|---|
| Backend name | every entity customizer → `WebsiteMetaDataProvider.RDBMS_BACKEND_NAME` | hard host reference from model classes |
| Registration order | `J/metadata/WebsiteMetaDataProvider.java:85-232` | package scans of `com.kof22.website.model`, `.workflows`, `.apps` (missing package), then explicit configure calls in a fixed order |
| Raw table names in Java | `J/workflows/media/CmsMediaLinks.java:49`, `J/metadata/PublicContentPersonalizer.java:119-153`, `J/metadata/esb/WebsiteEsbMetaData.java:66-69`, `J/metadata/autoload/auth/PublisherTokenRouteProvider.java:91-99`, `J/home/HomeWidgets.java:105`, `J/siteapi/SiteApiResources.java` | breaks with any prefix |
| Raw table names in SQL | `J/datahealth/rules/*` (126 rules), `J/metadata/analytics/Kof22Analytics.java:64-69` (`SLUG_QUERIES`, no site filter), `J/metadata/cmshome/CmsSectionWidgets.java:230` (`FROM analytics_daily_stat`), the `content_stream` view | breaks with any prefix |
| Site identity | `PUBLIC_WEBSITE_SITE_ID` default `1` (`J/metadata/PublicContentPersonalizer.java:95-101`, `MediaUploadService.java:215`); `SITE_ID=1L` in tools | one site per deployment; no per-request resolution |
| Hosts and URLs | `J/siteapi/SiteApi.java:40` (`kof22SiteApi`), `:43` (`/site-api/`), `:52` (semver `1.1.0`), `:61` (`https://api.kof22.com`), `:64` (`https://www.kof22.com`), `:67` (authoring blog `quick-bits`); `SiteApiRouteProvider.java:619` (CORS origins); `SiteApiOpenApiShaper.java:225` (`auth.kof22.com`); `J/siteapi/docs/SiteApiDocs.java:45-257` | kof22 constants with env overrides |
| Auth names | `kof22-admins` (`AuthentikAuthenticationCustomizer.java:49`); `kof-website-publishers`, `kof-website-drafters` (`PublisherTokenVerifier.java:45-46`); scope `kof22:publish`, claim `kof22_publish`, issuer `auth.kof22.com/application/o/kof22-post-ios/`, audience `kof22-post-ios` (`SiteApiTokenVerifier.java:66-67,111-114`) | IdP-specific names in code |
| Storage | `kof22-website-incoming`, `kof22-website-files`, `https://s3.kof22.com` (`J/siteapi/authoring/MediaStorage.java:106-138`); key layouts `media/<slug>/...` (`J/utils/MediaImageProcessor.java:85-108`), `photos/<album>/<photo>/...` (`J/utils/PhotoImageProcessor.java:285-324`), `siteId/slug/checksum` (`J/workflows/media/CmsImageUpload.java:202-213`); direct `AmazonS3` clients built from env (`PhotoImageProcessor.java:198-217`) | no QQQ table uses the `s3` backend; three key layouts |
| Infra names | ESB `kof22Artemis`, `kof22.cms.tables`, `kof22.cms.processes`, `kof22.media.ingest` (`WebsiteEsbMetaData.java:57-60`), instance `kof22-website` (`:117`); scheduler `kof22Scheduler` (`J/metadata/scheduler/WebsiteSchedulerMetaData.java:46-48`); index `kof22-content` (`WebsiteQuickSearchMetaData.java:69`) | host infra names inside CMS wiring |
| Frontend URL shapes | `/blogs/<blog>/<post>`, `[/personal/<cat>]/projects/<p>[/updates/<u>]`, `/personal/photography/<album>`, `/stream` in `J/metadata/analytics/ContentPathResolver.java:50-56`, `J/workflows/publishing/ContentUrlMigration.java:289,358-367`, `J/siteapi/content/PublicContentService.java:320-412` (sitemap, feeds), and the stream view SQL | public URL scheme is code, not config |
| Mail | `smtp.galaxy.lan` default (`J/siteapi/contact/Mailer.java:45`), from/subject (`ContactService.java:104,195-196`) | host infra |
| Time zone | `America/Chicago` (`Kof22Analytics.java:53`) | host config (already) |

### 1.4 Refactor list (in Website-Backend, before extraction)

Each item is a small behaviour-preserving PR against Website-Backend, the same approach as #98's "Prep in this repo". Exit criterion for the whole list: `CmsModuleBoundaryTest` (ArchUnit, modelled on `CmsAnalyticsModuleTest.testModuleHasNoHostDependencies`) passes for a new `com.kof22.website.cms.core` package tree with no `com.kof22.website.*` imports outside it.

| # | Refactor | Hotspots removed | Est. |
|---|---|---|---|
| R1 | Introduce `CmsConfig` (a `QBitConfig`) and a `CmsQBitProducer` *in the host*, producing today's metadata from today's code. Entity customizers take the backend from `QBitConfig.getDefaultBackendNameForTables()` instead of `WebsiteMetaDataProvider.RDBMS_BACKEND_NAME`. Replace the package scans with the producer. | backend name, registration order | 3 d |
| R2 | `CmsTables`: one registry of logical table names → physical names (`config.applyPrefix(...)`), used by every Java reference. | raw names in Java (6 sites) | 2 d |
| R3 | `CmsSql`: `{blog_post}`-style placeholders in all raw SQL, resolved through R2 (same approach as `AnalyticsSchema`). Data Health rules, `SLUG_QUERIES`, `CmsSectionWidgets:230`. | raw names in SQL | 2 d |
| R4 | `CmsSiteApiConfig`: API name, internal path, public base URL, website base URL, contact, default authoring blog, CORS origins, auth issuer/audience/scope/claim/groups, iOS app ids, SMTP. Env lookups move to the host's config builder (`withEnvironment(env)` as in `CmsAnalyticsConfig`). | hosts/URLs, auth names, mail | 2 d |
| R5 | `CmsSiteResolver` SPI replacing `PUBLIC_WEBSITE_SITE_ID` (§2.6); `FixedSiteResolver(1)` keeps today's behaviour. | site identity | 2 d |
| R6 | `CmsUrlScheme`: URL patterns per content kind (`post: /blogs/{blog}/{slug}`, ...). Used by content-path resolution, URL migration, sitemap and feeds. The `content_stream` view keeps its columns but takes URLs from a `url_path` computed in Java at publish time and stored on the row, or the view is regenerated from the scheme by a changeset (decide in phase 2; storing is simpler and indexable). | URL shapes | 4 d |
| R7 | `CmsStorage` SPI (`put`, `presignPut`, `publicUrl`, `delete`) with `S3CmsStorage` and `FilesystemCmsStorage`; buckets, key layouts and rendition widths in `CmsMediaConfig`. Replaces the three direct `AmazonS3` clients. | storage | 3 d |
| R8 | `CmsRoleMapping` + `CmsSecurity.applySessionPermissions(session, groups)`; the host's Authentik customizer calls it. Publisher verifier reads groups/issuer/audience from config. | auth names | 2 d |
| R9 | ESB destination names, provider name, scheduler name and search index name injected from config. | infra names | 1 d |
| R10 | Land #107 (core permission enforcement) and fold `CmsTablePermissionChecker` into the producer (§6). | permissions | (in #107) |
| R11 | Finish #98 prep: `WidgetHtml`/`DashboardHtml` out of the analytics module, host JDBC helper, `CmsSectionWidgets:230` via analytics runtime API, rollup process name via runtime. | reverse coupling | (in #98) |
| R12 | `ProjectSection` enum → seeded PVS table or code field; `kof.content-config.v1` → namespaced `cms.content-config.v1` with the old name accepted as an alias. | kof22 IA | 1 d |
| R13 | Test fixture split: `FreshCmsInstance` (no host classes) vs host integration tests. | tests | 2 d |
| R14 | Pin public API names: every Site API resource and nested schema gets an explicit public name (today a null `apiName` defaults to the table name, `J/siteapi/SiteApiResources.java:73`), and the deprecated `/public-api/` gets explicit API table names, so the D3 rename cannot change either API. | API names coupled to table names | 1 d |
| R15 | Names in config and data: generate the publisher route allow-list from `CmsTables`; the publisher CLI reads table and process names from `publishing.yaml`; a `CmsRenamePlan` lists every table, constraint, index, sequence, view and name-as-data column (§3.6.1) that the rename changelog generator uses. | rename preparation | 1.5 d |

Incidental findings to fix on the way (not blockers): `versions.json` "released" reads the oldest changelog entry (`J/siteapi/http/SiteApiRouteProvider.java:674` uses `CHANGELOG.get(size-1)` while the list is newest-first); `docs/publisher-authentication.md` lists a narrower bearer allow-list than `PublisherTokenRouteProvider.java:91-99`; `docs/configuration.md` says `AUTH_MODE` defaults to `dev_local` but `J/Server.java:80` defaults to `dev`; `docs/database/liquibase-startup-migrations.md` says `Server.main()` runs migrations but nothing calls `LiquibaseRunner`; the implementation plan names `ESB_ARTEMIS_MGMT_URL` but code reads `ESB_ARTEMIS_MANAGEMENT_URL`.

---

## 2. Architecture

### 2.1 What QQQ already provides (research summary)

| Building block | Where | Used for |
|---|---|---|
| `QBitMetaDataProducer<C>`, `QBitConfig.validate()`, `QBitComponentMetaDataProducer`, `QBitProductionContext`, `SourceQBitAware` | `qqq:qqq-backend-core/.../model/metadata/qbits/` | the producer, config validation, sub-producers, `sourceQBitName` stamping |
| QBit identity `groupId:artifactId[:namespace]`; `QInstance.addQBit()` rejects duplicates | same | two installs need two namespaces (§3.3) |
| Table customizer from config, default backend for tables | `QBitConfig.getTableMetaDataCustomizer()`, `getDefaultBackendNameForTables()` | snake_case mapping and backend injection |
| No core table-prefix field | (each qbit implements `applyPrefix`) | `CmsConfig.tableNamePrefix` |
| Core permission enforcement, `PermissionEnforcement.ENFORCE`, `CorePermissionEnforcer.runTrusted(reason, ...)` | `qqq:docs/misc/CorePermissionEnforcement.adoc` (4.1.0-RC.4, #1061/#1062) | §6 |
| Record security locks, join locks, `MultiRecordSecurityLock` | core | public visibility, site scoping |
| `FILE_PREVIEW` adornment, `homeAppName` | 4.1.0-RC.3, Next RC.13+ | media thumbnails; the host's landing page |
| Developer panel providers | `qqq:docs/misc/DeveloperPanels.adoc`, PR #1076 | §2.9 |
| ESB: table/process publications, `EsbTrigger`, CloudEvents, no outbox | `qqq:docs/specs/2026-09-25-qqq-esb-design.md`, `qqq-esb` | §2.8 |
| Record search SPI; `qbit-quick-search` `@QuickSearchable` | `RecordSearchAction`, `RecordSearchProviderInterface` | §5 |
| API middleware (`ApiInstanceMetaData`, OpenAPI) | `qqq-middleware-api` | Site API |
| Schema assessment | `qqq-backend-module-rdbms/.../RDBMSBackendAssessor.java` | install doctor (§3.4) |
| Prefix-aware changelog generation | `qbit-template-data/.../liquibase/ExampleLiquibaseGenerator.java` | fallback if Liquibase property substitution is insufficient (§3.2) |

Conventions from the reference qbits (`qbit-sql-workbench`, `qbit-quick-search`, `qbit-authentik`, `qbit-assistant`): group `com.kingsrook.qbits`, artifact `qbit-<name>`, parent `com.kingsrook:qbit-build-parent`, `${revision}` versions, package `com.kingsrook.qbits.<name>`, top-level `<X>QBitProducer` / `<X>QBitConfig` / `<X>Runtime`, `isEnabled()` = config present, namespace becomes the prefix when none is set, Javalin routes registered only when `QJavalinMetaData` is on the classpath, host re-parents the qbit's top app, README sections Install / Configuration reference / Permissions / Operational tables / Upgrading, CircleCI `qqq-orb`, gitflow develop/release/main, Apache-2.0. DDL is host-run everywhere: quick-search "ships no DDL", sql-workbench ships `docs/ddl/postgres.sql`, assistant ships `docs/schema-postgres.sql`, analytics ships a host-included prefix-parameterized changelog.

Prior art: `QRun-IO/qbit-template-application/qbit-platform-content.md` is an early, unimplemented CMS sketch (`cms_` prefix, UUID keys, Keycloak). This spec supersedes it; its draft → published → archived states map onto `ContentVersionStatus`.

### 2.2 What "application qbit" means here

Per `qbit-template/QBIT_TYPE_TAXONOMY.md`, an *application* qbit installs a complete functional area: tables, possible values, processes, widgets, apps, permissions and routes, on the host's backend. For the CMS, `CmsQBitProducer.produce(qInstance)` contributes:

| Contribution | Detail |
|---|---|
| Tables | ~38 tables (§1.2), prefixed, on `config.backendName`; field audit `FIELD` on content tables (today `J/metadata/audit/WebsiteAuditMetaData.java:105`); unique keys as today |
| Possible values | `redirect_type`, `blog_type`, `page_type`, `page_section_type`, `media_type`, enums (`ContentChannel`, `ContentFormat`, `ContentVersionStatus`) |
| Security key types | `<prefix>Site`, `<prefix>Publication`, `<prefix>NullParent`, `<prefix>Listed`, each with an all-access key (`J/metadata/PublicContentPersonalizer.java:40-45`, `J/siteapi/SiteApiTables.java:51-52`) |
| Processes | `PublishBlogPost`, `PublishProjectContent`, `ExtractMediaMetadata`, `UploadCmsImage`, `UploadCmsImageFile`, `GeneratePhotoRenditions`, `PublishScheduledPosts`; all `withTableName` + record/table menus, none in nav |
| Apps | one top app (`cms`, label `CMS`) with sub-apps sites, pages, blog, navigation, media, photography, projects; the host re-parents or keeps it top-level |
| Widgets | CMS home and sub-app homes (§1.1), honest empty states, readable at all widths |
| Permissions | per-table rules + a `cms.*` permission set (§6) |
| Schedules | scheduled publish (60 s), optional orphan sweep for media metadata (when no ESB) |
| ESB | table publications for content tables, a `PublishBlogPost`/`PublishProjectContent` process publication, the media-insert trigger (only when `qqq-esb` is present and `esbDestinationPrefix` is set) |
| Search | `searchFields` on content tables (core search works without quick-search); `@QuickSearchable` annotations picked up when the host lists the qbit's entities |
| Developer panels | "Content" panel on content records (§2.9) |
| Routes | machine publisher token routes (publishing feature); the Site API is the separate `qbit-cms-site-api` artifact (§2.7) |
| Schema | `qbit-cms/db/changelog/qbit-cms.changelog.yaml`, host-included (§3.2) |
| Runtime | `CmsRuntime` (storage client, URL scheme, site resolver), transient on the config, rebuilt in `postProduceActions` |
| SPI implementations for other qbits | `CmsSiteResolver`/`CmsContentResolver` adapters for `qbit-cms-analytics` |

### 2.3 Packaging (D1: option A decided)

| Option | Shape | For | Against |
|---|---|---|---|
| **A (decided, D1)** | Repo `QRun-IO/qbit-cms`, Maven multi-module: `qbit-cms` (core + media + blog + pages + menus + projects + photos + publishing, switched by `CmsFeature` flags), `qbit-cms-site-api` (API, authoring, contact; brings `qqq-middleware-api`, `nimbus-jose-jwt`), `qbit-cms-test-support`. Analytics stays its own repo `qbit-cms-analytics` (#98). | The features are entangled at the schema level: `featuredMediaId` on 7 tables, `blog_post_version` FKs to post/project/update, `photo_album` FKs to project/update, the stream view across all kinds. One artifact keeps FKs, PVS references and the changelog coherent. Flags (the `AuthentikFeature` pattern) still let a host install only pages + media. Heavy optional deps are isolated in the API artifact. | One release train for all content features. |
| B | Eight sub-qbits (`cms-core`, `cms-media`, `cms-blog`, `cms-projects`, `cms-photos`, `cms-publishing`, `cms-site-api`, `cms-analytics`) | Fine-grained install | Cross-qbit FKs and PVS references need inter-qbit config plumbing; eight changelogs whose order the host must get right; eight version matrices. High cost for little real benefit. |
| C | One jar including the Site API | Simplest | Forces API middleware and JWT libraries on hosts that only want admin CMS. |

`CmsFeature` set (option A): `CORE` (always: site, domain, redirect, page, section, snippet, menu), `MEDIA`, `BLOG` (requires MEDIA), `PROJECTS` (requires MEDIA), `PHOTOS` (requires MEDIA), `PUBLISHING` (requires BLOG or PROJECTS), `STREAM` (requires BLOG), `DATA_HEALTH_RULES` (D10). A disabled feature produces no metadata, and its changesets are skipped by a Liquibase `context` or precondition on a `cms.features` property. `validate()` enforces the dependencies.

### 2.4 Configuration (`CmsQBitConfig`)

| Setting | Default | Meaning |
|---|---|---|
| `backendName` | required | RDBMS backend for all CMS tables (one backend: the stream view and joins need one database) |
| `tableNamePrefix` | `cms_` (D4) | SQL and QQQ table name prefix. A namespace becomes the prefix if unset. Must equal the changelog property `cms.tablePrefix`. kof22 uses `cms_` after the D3 rename. |
| `namePrefix` | `cms` | Prefix for process, widget, security-key-type and permission names (`cmsPublishBlogPost`, `cms.<table>.edit`) |
| `adoptExistingSchema` | `false` | Install over existing tables that already match the qbit baseline exactly (baseline changesets `MARK_RAN`). kof22 doesn't need it: its rename (§3.6) produces the baseline shape. |
| `fieldNameMapping` | `SNAKE_CASE` | Column mapping. The D3 rename also renames kof22's `createdate`/`modifydate` to `create_date`/`modify_date`, so there is no legacy mapping. |
| `features` | all | `Set<CmsFeature>` (§2.3) |
| `appName`, `appLabel`, `parentAppName` | `cms`, `CMS`, none | The top app and where it nests |
| `permissionBaseName` | `cms` | Permission names `<base>.<table>.read/insert/edit/delete`, `<base>.publish`, `<base>.admin` (§6) |
| `openReads` | `false` | When true, CMS table reads need no permission (kof22 today, #107). Writes always need permissions. |
| `startupMode` | `FAIL_FAST` | `FAIL_FAST` or `DEGRADED` when the schema check fails (§3.4) |
| `roleMapping` | empty | IdP group → `CmsRole` (`ADMIN`, `EDITOR`, `PUBLISHER`, `DRAFTER`, `VIEWER`) |
| `siteResolver` | `FixedSiteResolver(1)` | §2.6 |
| `adminSiteScoping` | `false` | When true, admin sessions only see sites granted to them (multi-tenant hosts, §6.4) |
| `urlScheme` | generic defaults (`/blog/{blog}/{slug}`, `/{page-path}`, `/projects/{slug}`, ...) | Public URL patterns per kind (R6) |
| `publicWebsiteBaseUrl` | required when STREAM or PUBLISHING | Absolute URLs in feeds, sitemap, receipts |
| `media.storage` | required when MEDIA | `CmsStorage` instance (S3/MinIO or filesystem) |
| `media.publicBaseUrl`, `media.keyLayout`, `media.renditions` | none, `{site}/{slug}/{checksum}`, `thumbnail 400 / gallery 1600 / slideshow 2400` | Rendition widths today default to 400/1600/2400 (`J/workflows/media/CmsImageUpload.java:202-213`) |
| `media.maxUploadBytes`, `media.allowedTypes` | 5 MB, png/jpeg | from `CmsImageUpload` |
| `schedulerName` | none | Scheduled publish; without it the process exists but is unscheduled |
| `esbDestinationPrefix`, `esbProviderName` | none | e.g. `kof22.cms` → `kof22.cms.tables`, `kof22.cms.processes`, `kof22.cms.media.ingest`. Unset: no ESB metadata. |
| `publisher` | none | Machine publisher token: issuer, audience, introspection URL/credentials, publisher and drafter groups, route allow-list (generated from enabled features) |
| `revalidation` | none | Webhook URL(s) + HMAC secret for ISR hooks (§4.3) |
| `previewTokenSecret`, `previewTokenTtl` | none, 15 min | Signed draft preview (§4.2) |
| `seedReferenceData` | `true` | Generic PVS rows (redirect types, generic section types). Never content. |
| `environment` | `System.getenv()` | `${env.X}` interpolation, as in `CmsAnalyticsConfig.withEnvironment` |

`validate()` checks: backend exists and is RDBMS; prefix rules; feature dependencies; no name collisions (§3.3); storage present when MEDIA; base URLs absolute; secrets present when their feature is on. `toString()` redacts secrets (sql-workbench pattern).

Host registration (kof22, after cutover):

```java
new CmsQBitProducer()
   .withQBitConfig(new CmsQBitConfig()
      .withBackendName("postgres")
      .withTableNamePrefix("cms_")                                  // D3 + D4: renamed at cutover (§3.6)
      .withAppName("Content").withAppLabel("CMS")
      .withSiteResolver(new FixedSiteResolver(env("PUBLIC_WEBSITE_SITE_ID", 1)))
      .withUrlScheme(Kof22Cms.URL_SCHEME)                          // /blogs/{blog}/{slug}, /personal/...
      .withPublicWebsiteBaseUrl("https://www.kof22.com")
      .withMedia(new CmsMediaConfig().withStorage(S3CmsStorage.fromEnv(env)) ...)
      .withSchedulerName("kof22Scheduler")
      .withEsbProviderName("kof22Artemis").withEsbDestinationPrefix("kof22")
      .withPublisher(Kof22Cms.publisherConfig(env))
      .withOpenReads(true)                                          // keep #107 behaviour
      .withRoleMapping(Map.of("kof22-admins", ADMIN, "kof-website-publishers", PUBLISHER, "kof-website-drafters", DRAFTER))
      .withEnvironment(env))
   .produce(qInstance);
new CmsSiteApiQBitProducer().withQBitConfig(Kof22Cms.siteApiConfig(env)).produce(qInstance);
```

### 2.5 Package layout (`com.kingsrook.qbits.cms`)

```
CmsQBitProducer, CmsQBitConfig, CmsFeature, CmsRuntime, CmsTables, CmsSql
core/        site, domain, redirect, page, section, snippet, menu (model + metadata)
media/       media, media_type, CmsStorage SPI, S3/filesystem storage, renditions, metadata ETL, upload processes
blog/        blog, post, tag, author, post-media, versions
projects/    project, category, update
photos/      photo, album, folder, tag, rendition process
publishing/  publications, receipts, PublishBlogPost, PublishProjectContent, RelatedVersions, ContentUrlMigration, publisher token auth
stream/      content_stream
visibility/  CmsVisibility (security keys + locks), CmsSiteResolver SPI
security/    CmsPermissions, CmsTablePermissionChecker, CmsRole, CmsSecurity
home/        CMS home + sub-app homes, widget helpers
developer/   CmsDeveloperPanels
analytics/   adapters implementing qbit-cms-analytics SPIs (compiled against its API, optional)
health/      Data Health rule definitions (D10)
resources/qbit-cms/db/changelog/...   prefix-parameterized changelog
```

`qbit-cms-site-api`: `com.kingsrook.qbits.cms.siteapi` with `CmsSiteApiQBitProducer`, `CmsSiteApiConfig`, `resources/`, `http/`, `authoring/`, `contact/`, `docs/`.

### 2.6 Site and tenant model

Today a site is fixed per deployment (`PUBLIC_WEBSITE_SITE_ID`, `J/metadata/PublicContentPersonalizer.java:95-101`) and the `domain` table is only used by analytics (`J/metadata/analytics/Kof22Analytics.java` `DomainSiteResolver`). The Site API has no host-to-site resolution, and neither does the frontend.

Design: `site` is the unit of scoping. Every content table has, or reaches by join, a `siteId`. `CmsSiteResolver` (SPI) returns the site id(s) for a request:

| Strategy | Use |
|---|---|
| `FixedSiteResolver(id)` (default) | One site per deployment. Exactly today's behaviour. |
| `HostSiteResolver` | Looks up the request host (`X-Forwarded-Host` from a trusted proxy, else `Host`) in `domain.fqdn` (cached). One Site API serves many sites. |
| `HeaderSiteResolver` | `X-Cms-Site: <site key>` from a trusted server-side caller (a frontend that already knows its site). |
| custom | Host code |

The resolved site becomes the session's `<prefix>Site` security key; record locks (`siteId` READ lock and join locks, as today) do the filtering. Admin sessions get the all-access key unless `adminSiteScoping` is on (§6.4).

### 2.7 Site API v1 inside the qbit (`qbit-cms-site-api`)

The Site API is already a real QQQ `ApiInstanceMetaData` (`J/metadata/autoload/siteapi/SiteApiMetaDataProvider.java`) with an allow-list of resources and fields (`J/siteapi/SiteApiResources.java`), plus a custom route provider for non-table routes (`J/siteapi/http/SiteApiRouteProvider.java`). It moves almost unchanged; only the constants become `CmsSiteApiConfig`:

| Setting | kof22 value today | Default for new installs |
|---|---|---|
| `apiName` | `kof22SiteApi` (`SiteApi.java:40`) | `<prefix>SiteApi` |
| `internalPath` | `/site-api/` (`:43`) | `/site-api/` |
| `publicBaseUrl` | `https://api.kof22.com` (`:61`) | required |
| `publicPathPrefixStripped` | yes (Traefik adds `/site-api`) | configurable |
| `allowedOrigins` | `www.kof22.com`, `kof22.com` + `SITE_API_EXTRA_ORIGINS` (`SiteApiRouteProvider.java:593-621`) | required for browser use |
| `features` | resources, search, sitemap, feeds, contact, authoring, AASA | `RESOURCES`, `SITEMAP`, `FEEDS` on; `SEARCH` on when a search provider exists; `CONTACT`, `AUTHORING`, `IOS_ASSOCIATION` opt-in (D11) |
| `authoring.*` | issuer `auth.kof22.com/...`, audience `kof22-post-ios`, scope `kof22:publish`, groups `kof22-admins,kof-website-publishers`, default blog `quick-bits` | required when AUTHORING |
| `contact.*` | SMTP `smtp.galaxy.lan`, to/from, salt, hourly caps | host `CmsMailer` SPI |
| `docs.title/contact` | kof22 strings (`SiteApiDocs.java`) | from config |

**Contract.** The `/v1` resource set, field names, error envelope (`J/siteapi/http/ApiProblem.java`), cache headers (`public, max-age=60` for anonymous GETs; `no-store` on `/v1/me/*`), `X-API-Version`, `versions.json` and the OpenAPI document are the qbit's public contract under semver. A breaking change is `/v2`. The qbit owns `info.version`; the host can append its own resources only under a host-chosen extension prefix (`x-host-...` in OpenAPI), so a host addition cannot collide with a future qbit minor.

**kof22 compatibility.** The kof22 instance keeps `apiName=kof22SiteApi`, `/site-api/`, `api.kof22.com` and the iOS app's OpenAPI contract (`src/main/resources/site-api/openapi-extensions.yaml`, the swift-openapi-generator input) byte-compatible. Contract tests run the current golden responses against the qbit build (§7). Resource names are pinned explicitly (R14), so the D3 table rename changes nothing under `/v1`.

### 2.8 Events, search and schedules

- **ESB (optional).** When `qqq-esb` is on the classpath and `esbDestinationPrefix` is set, the qbit adds `EsbTableMetaData.publications` (INSERT/UPDATE/DELETE) to the content tables (today's 16, `WebsiteEsbMetaData.java:66-69`), process publications for both publish processes (today only `PublishBlogPost` publishes; add `PublishProjectContent` for consistency), and the media-insert `EsbTrigger` (concurrency 1, max attempts 5, today `:150-162`). The host declares the provider and destinations, because broker config is infrastructure.
- **Without ESB**, media metadata extraction falls back to a scheduled sweep (`media where metadataExtractedAt is null`, every 5 min) or a post-insert customizer when `media.extractSynchronously=true`. Nothing silently stops working; the CMS home shows "metadata pending" counts.
- **Search.** Content tables declare `searchFields` so core `RecordSearchAction` works with no provider. `@QuickSearchable` annotations stay on the entities. A host with `qbit-quick-search` adds `CmsEntities.searchable(config)` to `searchableEntityClasses`. Site API `/v1/search` uses Quick Search when present (`J/siteapi/content/PublicContentService.java:27-29`), else a bounded SQL `ILIKE` over published titles/excerpts, with the result re-read through locks in both cases.
- **Schedules.** `PublishScheduledPosts` every 60 s (today `siteApiPublishScheduledPosts`, `J/siteapi/authoring/ScheduledPublishProcess`), on `schedulerName`. Analytics rollups belong to `qbit-cms-analytics`.

### 2.9 Developer panel

A `CmsDeveloperPanels` provider (registered only when `DeveloperPanelProviderInterface` is on the classpath, as `qbit-assistant` does) adds a "Content" panel to the record Developer view for content tables: publication state and dates, the public URL(s) from `urlScheme`, the Site API URL for the record, latest publication receipt (request id, digest, outcome), version count by channel, ESB publication status, Quick Search index status when installed, and an `ACTIONS` block (Publish, Preview link). It reads only through the user's session; `isAvailable` is false for non-content tables.

---

## 3. Installing into any running QQQ system

### 3.1 Requirements

| Requirement | Minimum |
|---|---|
| QQQ | 4.1.0-RC.4 (core permission enforcement). FILE_PREVIEW (RC.3) and developer panels (#1076) are used when present. |
| UI | Next 1.0.0-RC.14+. **Material is not supported** (it ignores FILE_PREVIEW and custom home widgets, and has no ESB UI; `qqq:docs/migration/4.1.adoc`). Tables still work generically in Material, with no guarantees. |
| Database | PostgreSQL 14+ (production); H2 for tests. The `content_stream` view and the rollup lock are PostgreSQL-specific with H2 twins (pattern from `LB/cms-analytics/`). |
| Object storage | S3-compatible (MinIO, AWS S3) or filesystem, when MEDIA is on |
| Scheduler | any QQQ scheduler, optional |

### 3.2 Schema ownership: the host runs it, the qbit ships it (D5)

The qbit ships `qbit-cms/db/changelog/qbit-cms.changelog.yaml` in its jar:

- Every changeset uses `logicalFilePath: qbit-cms/changelog` and author `qbit-cms`, so ids are stable no matter where the host includes the file from (the `logicalFilePath` problem #98 found for analytics).
- Table names are `${cms.tablePrefix}<name>`; the host sets the property next to the include, as `LB/changelog.yaml` already does for `cms.analytics.tablePrefix`.
- Feature sections are gated by a precondition on a `cms.features` property, so disabled features create nothing.
- **Baseline changesets** (`1.0.0-baseline-*`) create the v1 schema with `preConditions onFail: MARK_RAN` + `not tableExists`. On a fresh database they create tables; on kof22 they are marked as run (§3.6).
- Later versions only **append** changesets (`1.1.0-*`, `1.2.0-*`). No edits to released changesets, ever. The CHANGELOG has a "Schema changes" section per release.
- A reference `docs/ddl/postgres.sql` is generated in CI from the changelog for hosts that don't use Liquibase (sql-workbench style).

Host include:

```yaml
  - property: { name: cms.tablePrefix, value: cms_ }
  - property: { name: cms.features,    value: CORE,MEDIA,BLOG,PUBLISHING,STREAM }
  - include:  { file: qbit-cms/db/changelog/qbit-cms.changelog.yaml }   # from the qbit jar on the migrations classpath
```

kof22 runs migrations in a separate image (`WB:docker/Dockerfile.migrations`, Liquibase CLI), so the qbit jar (or an extracted copy of its changelog directory, as sql-workbench hosts do) must be on that image's classpath. The rename changelog (§3.6) takes the source prefix as a property. Phase 1 verifies that an empty source prefix (kof22) substitutes cleanly. If it does not, the fallback is a generated changelog (the `qbit-template-data` `ExampleLiquibaseGenerator` approach).

Why not let the qbit run DDL at startup: every QQQ host already has its own migration pipeline and backup discipline (kof22: migrations job + CNPG backups), and multiple app replicas racing DDL is a known foot-gun. Startup only **checks**.

### 3.3 Name collisions, namespaces, multiple installs, multiple backends

- **QQQ names.** Tables, PVS, processes, apps, widgets, security key types and permission names are all prefixed (Next routes are a flat `/app/{name}` namespace, so this is required, not cosmetic). `validate()` checks every name it will produce against the instance before producing and fails with a list of collisions and the setting to change.
- **Database names.** In non-adopt mode, a baseline precondition that finds an existing table without the expected `databasechangelog` row **halts** with "table `<x>` exists but was not created by qbit-cms; set a different `cms.tablePrefix`, use adoptExistingSchema (exact baseline shape only), or rename with the `adopt/rename-to-prefix` changelog". It never overwrites.
- **Two installs in one instance** (e.g. two independent CMSs) need two namespaces (`QInstance.addQBit()` rejects a duplicate `groupId:artifactId`), which become two prefixes, two apps and two permission bases. Supported but not a design goal; multi-site within one install is the normal way to run several sites.
- **Multiple backends.** All CMS tables live in one backend (`backendName`); FKs, join locks and the stream view require it. Analytics can live elsewhere. A separate PostgreSQL schema is possible by giving the CMS its own RDBMS backend whose connection sets `currentSchema`; the RDBMS module has no per-table schema setting today.

### 3.4 Startup check ("install doctor")

On `postProduceActions`, `CmsRuntime` runs a cheap schema check: the latest expected changeset id is present in `databasechangelog`, and `RDBMSBackendAssessor` finds every CMS table and column. Behaviour by `startupMode` (the quick-search pattern): `FAIL_FAST` (default in production) refuses to start with the missing changesets listed; `DEGRADED` starts, disables CMS writes and shows the problem on the CMS home and in Data Health.

### 3.5 Upgrade and uninstall

- **Upgrade.** Bump the dependency, run migrations (new appended changesets), deploy. Semver: patch = no schema change; minor = additive schema (new nullable columns, tables, indexes; `CREATE INDEX CONCURRENTLY` with H2 twins); major = anything destructive, with a migration guide (`docs/MIGRATION-<n>.x.md`, quick-search style) and, when needed, a two-step expand/contract over two minors.
- **Version skew.** The app refuses (FAIL_FAST) to run against a schema older than its baseline + required changesets; it tolerates a *newer* schema (additive) so rolling deploys work.
- **Uninstall.** Removing the producer call removes all metadata; data stays. The qbit ships an opt-in `qbit-cms-uninstall.changelog.yaml` (drops, behind a context the host must name explicitly), documented with a backup step. Never automatic.

### 3.6 kof22 migration: in-app CMS → qbit, renamed to `cms_` (D3)

D3 decided that kof22's CMS tables are renamed to the standard `cms_` prefix at cutover. Afterwards kof22 runs exactly the schema every other install runs, with no adoption or legacy mappings. Principles:

- **No data copies and no object-store moves.** Every change is a metadata rename or a small `UPDATE` of a column that stores a table name.
- **One transaction.** PostgreSQL DDL is transactional, so the whole forward migration commits or rolls back as a unit.
- **Every step has a written reverse**, and the reverse is rehearsed.
- **The Site API does not change.**

#### 3.6.1 What the rename touches

| Object | Forward change | Reverse | Notes |
|---|---|---|---|
| CMS tables (~38: every (a) table in §1.2 except analytics) | Liquibase `renameTable` `<name>` → `cms_<name>` | rename back | Metadata-only in PostgreSQL. Each needs a brief `ACCESS EXCLUSIVE` lock (milliseconds). Foreign keys, indexes, constraints, grants, identity columns and dependent views follow by OID. |
| Timestamp columns | `createdate`/`modifydate` → `create_date`/`modify_date` (`renameColumn`) | rename back | Aligns kof22 with the qbit's standard snake_case mapping, so no `LEGACY_KOF22` mapping is needed. kof22 maps them explicitly today. |
| Constraint and index names | `pk_<t>`, `uq_*`, `fk_*`, `idx_*` → `cms_` forms (about 117 named constraints and 75 indexes across `LB/changesets/0001`-`0020`) via generated `ALTER TABLE ... RENAME CONSTRAINT` / `ALTER INDEX ... RENAME` | rename back | Cosmetic to PostgreSQL, but required so the renamed schema equals the qbit baseline exactly (§3.6.2 step 1), and so later qbit changesets can reference names predictably. |
| Identity sequences | `<t>_id_seq` → `cms_<t>_id_seq` (`ALTER SEQUENCE ... RENAME`) | rename back | Values are unchanged. The generic resync in `LB/changesets/0016-sequence-resync.sql` uses `pg_get_serial_sequence` and keeps working. |
| `content_stream` view | drop it, then create `cms_content_stream` from the qbit's changeset | drop the new view, recreate the old one from `0020-gallery-stream-scope.sql` | PostgreSQL would keep the old view working through OIDs, but its name and definition must match the qbit. |
| Columns that store table names as data | `UPDATE ... SET x = 'cms_' \|\| x WHERE x IN (<renamed tables>)` on `audit_table.name` (`LB/changesets/0008-audits.sql`), `quick_search_index.table_name`, `quick_search_failed_event.table_name` (`0007-quick-search.sql:7,52`), `analytics_pageview.content_table`, `analytics_daily_stat.content_table` (`0010-analytics.sql:75,99`) | `UPDATE` stripping the prefix, limited to the same set | Audit history stays attached: audits reference `audit_table` by id, and only the name changes. Analytics raw pageviews are the largest set (400-day retention), so the rehearsal times this step and batches it if needed. |
| QQQ metadata | Table names become `cms_<name>`, equal to their backend names (SNAKE_CASE mapping, no `backendDetails` override). Security key types, permission names (`cms.<table>.*`), join names and record-lock join paths all come from `CmsTables` (R2). | previous image | No permissions, keys or locks are stored as data: they are computed per session from groups, so nothing persisted needs migrating. |
| Process names | Processes take the qbit's name prefix (`cmsPublishBlogPost`, `cmsPublishScheduledPosts`, ...). Scheduled jobs register under the new names at startup. The migration deletes the old Quartz job and trigger rows (`qrtz_*` for `siteApiPublishScheduledPosts`). | the previous image recreates its jobs at startup | Avoids collisions in other hosts. The publisher CLI is updated (next row). |
| Publisher CLI | The machine-token route allow-list (today `PublisherTokenRouteProvider.java:91-99`) is generated from `CmsTables`. A new CLI release reads table and process names from `publishing.yaml`, which is updated for the new names. | previous CLI and config | Publishing is frozen during the window. Both CLI versions are kept until the watch period ends. |
| Next admin deep links | `/app/blog_post/123` → `/app/cms_blog_post/123`. App and sub-app names don't change (`appName=Content`, `cmsSites`, ...). The admin Traefik ingress gets a temporary redirect middleware for the old table paths, kept for 90 days. | remove the middleware | Bookmarks and links in notes keep working. |
| Quick Search | Documents are keyed by table name, so a full reindex of `kof22-content` runs right after deploy. | full reindex | Admin search falls back to core search until the reindex completes (minutes at kof22 volume, measured in rehearsal). |
| ESB | CloudEvent types change (`qqq.table.blog_post.updated` → `qqq.table.cms_blog_post.updated`). Destination names (`kof22.cms.tables`, ...) don't change. The in-app media trigger is metadata and updates itself. Drain `kof22.media.ingest` before the window (queued messages carry the old names). | previous image | No external consumers are known (open question in the 4.1 implementation plan, L333). The change is announced in the CHANGELOG anyway. |
| Data Health rules | SQL is written with `CmsSql` placeholders (R3), so it resolves to the new names with no data change. Cached results are cleared and a run is triggered after deploy. | previous image | |
| Analytics SQL | `SLUG_QUERIES` (`Kof22Analytics.java:64-69`), `CmsSectionWidgets.java:230` and the content-path resolver's table list come from `CmsTables` (R2, R3, #98), plus the `content_table` data update above. | previous image | |
| Site API v1 | **No change.** Every resource's public name is pinned explicitly (R14), because today a null `apiName` defaults to the table name (`J/siteapi/SiteApiResources.java:73`). | n/a | Proven byte-identical by the golden-response and OpenAPI diffs. The iOS app is unaffected. |
| Deprecated `/public-api/` | Its API table names are pinned to the old names (D13), because it auto-exposes tables by table name (`PublicApiMetaDataProvider.java`). | n/a | Deleted at the 2027-01-31 sunset. |
| SQL Workbench notebooks | Not rewritten automatically. A pre-window inventory query (`sql_workbench_cell` text matching old table names) lists the notebooks, which are fixed by hand. The compatibility views keep them running meanwhile. | n/a | |
| Compatibility views | In the same transaction, create one view per renamed table under the **old** name: `CREATE VIEW blog_post AS SELECT ..., create_date AS createdate, modify_date AS modifydate FROM cms_blog_post`. Single-table views are automatically updatable in PostgreSQL. | dropped by the reverse | They let the previous image and existing notebooks keep working against the renamed schema, which shrinks public downtime and makes rollback a redeploy. They are dropped by a follow-up changeset after the watch period. **To verify in rehearsal:** inserts through the views apply the base tables' identity defaults, and QQQ's `RETURNING id` works through them. If not, the views are read-only for the old image, and the plan uses the downtime fallback in §3.6.3. |

The rename ships **in the qbit** as a generic, reusable changelog, `qbit-cms/db/changelog/adopt/rename-to-prefix.yaml`, parameterized by the source prefix (`""` for kof22), the target prefix and the feature set. It includes explicit `rollback` blocks for every change: Liquibase auto-generates rollbacks for renames, but not for `UPDATE`s. The host runs it once, before the qbit changelog. The qbit's baseline changesets then find the `cms_` tables and are `MARK_RAN`.

**Permissions at cutover.** The role mapping reproduces today's effective access: admins get everything, the publisher/drafter token semantics are unchanged, and iOS authors get #107's `SITE_API_AUTHOR_PERMISSIONS`. kof22 sets `openReads=true`. The permission red-team suite (§7) runs against every rehearsal database.

#### 3.6.2 Runbook

| Step | What | Verification |
|---|---|---|
| 0 | **Prep refactors** R1-R15 (§1.4) ship in Website-Backend as normal releases, reading every name from `CmsTables` with prefix `""`. Behaviour is identical. | Full test suite. Site API golden responses unchanged. One week of production soak. |
| 1 | **Rename changelog + round trip in CI.** Take a schema-only dump of production CMS tables, apply the forward rename, assert it equals the qbit's `cms_` baseline (normalized `pg_dump --schema-only` diff = 0), apply the reverse, and assert it equals the original. | Diff = 0 in both directions |
| 2 | **Rehearsal on a production snapshot, at least twice.** Clone production into a scratch CNPG cluster. Before: record per-table row counts and `md5(string_agg(row::text, '\|' ORDER BY id))` checksums (name-as-data columns compared after mapping). Run forward + qbit changelog + the new image. | Counts and checksums equal. Name-as-data columns mapped exactly. `RDBMSBackendAssessor` clean. Baseline `MARK_RAN` with **zero DDL** beyond the rename (reviewed `updateSQL`). Site API golden-response suite and OpenAPI diff: **0 diffs**. Publisher CLI (new release) `PLAN`, `SAVE_DRAFT` and `PUBLISH` against the rehearsal. iOS authoring `GET /v1/me`. Media upload → ESB → metadata ETL. Quick Search full reindex (timed). Data Health run with no new failures. Old deep link redirects. Compatibility-view write test (§3.6.1). Forward-migration duration recorded. |
| 2r | **Rollback rehearsal** on the same clone: reverse migration + previous image. | Counts and checksums equal to the "before" values. Previous image's smoke tests pass. |
| 3 | **Pre-window (T-1 day).** Announce. Stage the notebook fixes. Merge the Traefik redirect middleware (inactive). Confirm CNPG continuous backup and the PITR window are healthy. Confirm versioning is on for `kof22-website-files` and `kof22-website-incoming`. | Checklist signed off |
| 4 | **Window** (§3.6.3). | Smoke checklist in §3.6.3 |
| 5 | **Watch, 7 days.** Data Health daily, ESB dead letters, Quick Search status, analytics ingestion, publisher receipts. | No regressions |
| 6 | **Cleanup.** A follow-up changeset drops the compatibility views (after 7 days). The redirect middleware is removed after 90 days. The old CLI version is retired. | |

#### 3.6.3 Maintenance window

| T | Action | Duration (est.) |
|---|---|---|
| T-60 min | Freeze publishing: disable the publisher token client in Authentik, and enable the iOS authoring maintenance flag (`503` with `Retry-After`). Drain `kof22.media.ingest`. | 5 min |
| T-55 min | CNPG on-demand backup, plus `pg_dump --format=custom` of the CMS tables and the name-as-data tables. Test-restore the dump into scratch. | 15-20 min |
| T0 | Run the migrations job: the rename changelog (one transaction, timed in rehearsal; expected seconds, or up to a few minutes if the analytics `UPDATE` is large), then the qbit changelog (`MARK_RAN`). | 1-5 min |
| T+5 | Rolling deploy of the new image (old pods keep serving through the compatibility views). Activate the redirect middleware. | 3-5 min |
| T+10 | Start the Quick Search full reindex. Trigger a Data Health run. | runs in background |
| T+10 | Smoke: CMS home and each sub-app home; edit and publish a draft through the new CLI; `GET /v1/me`; `/v1/post/query`, `/v1/page/query`, sitemap and feeds compared with golden responses; media upload → renditions → metadata ETL; old deep link redirects; analytics Views widget; Quick Search once the reindex finishes. | 15 min |
| T+25 | Unfreeze publishing. | 2 min |

- **Window to book:** 60 minutes after the freeze. **Expected:** 30-45 minutes end to end.
- **Public-site impact:** the target is **none** (the old image serves through the compatibility views until it is replaced). If rehearsal shows the views can't take the old image's queries, the fallback is to scale the backend to 0 for T0 to T+5, giving about **10-15 minutes** of public downtime. The frontend is `force-dynamic` and has no stale cache to fall back on.
- **Admin writes and publishing:** frozen for about 45 minutes.
- **Site search:** degraded to core search until the reindex finishes.

#### 3.6.4 Rollback

| When | How | Data impact |
|---|---|---|
| During T0 (migration fails) | Automatic: the transaction rolls back. Unfreeze, investigate. | none |
| Within the watch period, app-level problem | Redeploy the previous image. It runs against the compatibility views. Remove the redirect middleware and reindex Quick Search. | none |
| Within the watch period, schema-level problem | Scale down. Run the reverse migration (one transaction: drop the compatibility views, reverse renames and `UPDATE`s, recreate the old `content_stream`, delete the qbit's `databasechangelog` rows by its `logicalFilePath`). Deploy the previous image, reindex, then restore the previous CLI and config. | none (writes made after cutover are kept, since rows are renamed, not copied) |
| Last resort | CNPG point-in-time restore to the T-55 backup | loses writes made after the backup (publishing was frozen, so only edits made after the unfreeze) |

### 3.7 Compatibility matrix

| | QQQ 4.1.0-RC.4+ | QQQ 4.2 (enforcement on by default) | Next ≥ 1.0.0-RC.14 | Material |
|---|---|---|---|---|
| qbit-cms 1.0 | supported; the producer turns on table permission rules regardless of the instance enforcement flag | supported (designed for enforcement on) | supported | not supported (tables usable, homes/previews not) |
| qbit-cms-site-api 1.0 | supported | supported | n/a | n/a |

---

## 4. Frontend

### 4.1 Recommendation: headless contract first, then a package and a starter (D8)

The Site API v1 is the product. Any frontend (Next.js, Astro, an iOS app, a static generator) consumes it. On top, the qbit project publishes:

1. **`@qrun-io/cms-next`** (npm, Apache-2.0), extracted from `WF:`:
   - typed client generated from the qbit's OpenAPI (replacing `WF:src/api/apis/*`, `src/api/models/*`, which are stale and kof-branded), with the thin generic query layer from `WF:src/api/SiteApi.ts` (`query`/`get`, `IN`/`EMPTY` mapping);
   - cached fetchers (`WF:src/lib/api.ts`, minus kof22 bridges/hubs);
   - a **section registry** replacing the hard-coded 23-case `switch` in `WF:src/components/sections/SectionRenderer.tsx`: `registerSection(code, Component)` with a fallback, and headless default sections for TEXT, MARKDOWN, HERO, BLOG_LIST, FEATURED_POSTS, GALLERY, IMAGE_CAROUSEL, SOCIAL_LINKS, CONTACT_FORM;
   - route-handler factories: sitemap proxy, robots, feeds alternates, stream RSS, CMS redirects middleware (`WF:src/lib/cms-redirects.ts`, EXACT/PREFIX, 30 s cache, 2 s timeout, loop guard), analytics beacon + forwarder (`WF:src/lib/analytics/*`, with a configurable client-IP strategy instead of "last X-Forwarded-For entry, assumes Traefik"), draft-mode enable/disable, revalidate endpoint;
   - config: `siteApiBaseUrl`, `siteUrl`, `siteKey` (multi-site), `previewSecret`, `revalidateSecret`. No kof22 defaults (today `https://www.kof22.com` is a fallback in ~10 files and `https://api.kof22.com` in `WF:src/lib/site-api-links.ts`).
2. **`create-qqq-cms-site`** starter (template repo or `npm create`): App Router, Tailwind, the package, plain neutral styles, mock Site API fixtures (from `WF:src/test-fixtures/mock/*`) for tests.
3. **Website-Frontend becomes the first consumer**, keeping its terminal theme, `/personal/*` IA, project hubs and code view, and kof22-only sections.

Not recommended: extracting Website-Frontend itself as the template (its IA and visual chrome are kof22's), or shipping a renderer inside the Java qbit.

### 4.2 Preview and drafts

Today there is no preview: drafts are private rows, the API is published-only, and `/preview` in `WF:` is a dev-only local-file renderer. Design (opt-in, `previewTokenSecret` set):

1. A "Preview" record action on post/page/project/update (and the developer panel link) mints a short-lived token: HS256 JWT `{site, table, id, versionKey?, exp ≤ 15 min, sub: user}` signed with `previewTokenSecret`. Minting requires read permission on the record.
2. The frontend's `/api/draft?token=...` verifies the signature, enables Next `draftMode()`, and redirects to the record's URL from `urlScheme`.
3. In draft mode, fetchers send `Authorization: Preview <token>` to the Site API. The API verifies it and grants the session, for that request only, the publication and listed all-access keys **scoped to the one record** (implemented as an extra lock clause on `id = token.id`, not a global key), plus its parents. Responses are `Cache-Control: no-store, private`.
4. Tokens are not refreshable; the action re-mints.

### 4.3 ISR and revalidation

Today `WF:src/app/layout.tsx` sets `dynamic = 'force-dynamic'` and the backend has no revalidation hook (confirmed in `docs/design/qqq-4.1-next-esb-quicksearch-implementation-plan.md:200-203`). Design (opt-in, `revalidation` configured):

- After commit of a content change or a publish, `CmsRevalidationNotifier` POSTs `{"site": "...", "tags": ["post:123", "blog:quick-bits", "stream", "sitemap"], "paths": ["/blogs/quick-bits/hello"]}` to each configured URL, with `X-Cms-Signature: sha256=<HMAC(body)>` and a timestamp header (5-minute replay window). Tags are computed from the record and its parents; paths from `urlScheme`.
- Delivery: a core after-commit record-change listener (works without ESB), at-least-once with a small retry (3 attempts, backoff), failures logged and counted on the CMS home. With ESB, the notifier can instead be an ESB consumer for durable retry.
- `@qrun-io/cms-next` tags its fetches (`next: { tags }`) and exposes `revalidateRoute(secret)` calling `revalidateTag`/`revalidatePath`.
- kof22 can stay `force-dynamic` (it is cheap at kof22's traffic); the hook is for adopters that cache.

---

## 5. Dependencies on other qbits and modules

| Dependency | Required? | Used for | Without it |
|---|---|---|---|
| `qqq-backend-core`, `qqq-backend-module-rdbms` (+ postgres) | required | everything | n/a |
| `qqq-middleware-javalin` | required for publisher routes; optional otherwise | publisher token routes | publishing via the UI only |
| `qqq-middleware-api` | required by `qbit-cms-site-api` only | Site API | no public API (admin CMS still works) |
| S3 SDK | required when MEDIA uses `S3CmsStorage` | storage | filesystem storage |
| `qqq-esb` | optional | change events, async media ETL, durable revalidation | scheduled sweep / synchronous ETL; after-commit revalidation |
| `qbit-quick-search` | optional | full-text search in admin and `/v1/search` | core `searchFields` search; SQL fallback in the API |
| `qbit-cms-analytics` | optional | Views widgets, top content, traffic tiles on the CMS home | those widgets are not produced (not shown empty) |
| a QQQ scheduler | optional | scheduled publish, sweeps | processes exist, unscheduled; CMS home warns |
| developer panel contract (core, #1076) | optional | Content panel | no panel |
| `qbit-authentik` | not used | IdP groups arrive in the session; `roleMapping` is IdP-neutral | n/a |
| `qbit-assistant` | not used | the assistant sees CMS tables as normal tables | n/a |
| Data Health framework | optional (D10) | CMS rules | rules not registered |

Degradation rule: an absent optional dependency produces no metadata for the dependent feature and no error at runtime, and the CMS home says so in one line where it matters (for example, "Search: core search (Quick Search not installed)"). Nothing renders as zero or empty because a dependency is missing (real-data rule).

---

## 6. Security and permissions

### 6.1 Enforcement baseline

The qbit assumes **core permission enforcement** (`qqq:docs/misc/CorePermissionEnforcement.adoc`, 4.1.0-RC.4; default on in 4.2). Today CMS tables have **no permission rules on main**; #107 (draft, needs RC.4) adds `CmsTablePermissionChecker` with open reads and `<table>.insert/.edit/.delete` for writes, plus a kill switch `KOF22_ENFORCE_CORE_PERMISSIONS=false`. The qbit makes that the default and drops the kill switch (a host can still turn enforcement off at the instance level, at its own risk).

### 6.2 Permission model

- Every CMS table has `READ_INSERT_EDIT_DELETE_PERMISSIONS` rules: `<base>.<table>.read`, `.insert`, `.edit`, `.delete`. Reads are **not** open by default for a generic install (kof22 sets `openReads=true` to keep #107's behaviour).
- Process permissions: `<base>.publish` (Publish actions), `<base>.media.upload`, `<base>.admin` (reference tables, publication receipts, uninstall-level actions).
- `CmsRole` bundles: `ADMIN` (all), `EDITOR` (read/write content, upload media, no publish), `PUBLISHER` (EDITOR + publish), `DRAFTER` (draft-only paths of the publish process, already modelled as `DRAFTER_GROUP`, `PublisherTokenVerifier.java:45-46`), `VIEWER` (read). `CmsSecurity.applySessionPermissions(session, groups, config)` is called from the host's auth customizer, the same integration point as `AssistantSecurity.applySessionKeys`.
- Bookkeeping tables (publications, receipts, idempotency, uploads, contact messages) are read-only in the UI (insert/update/delete capabilities removed), visible to admins, as normal QQQ tables.
- Processes re-check permissions in their first step (the authentik spec's §5.3 rule: don't trust the middleware alone). `PUBLISH` keeps its `reviewedDigest` check (`J/workflows/publishing/PublishBlogPost.java:239`).

### 6.3 Public access

The Site API session is a scoped `FULLY_ANONYMOUS` provider (today "site-api-anonymous") with **read permission only** on the curated tables, and security keys: publication = published, listed = listed, site = the resolved site. Locks do the filtering (§2.6), so even a bug in the resource allow-list cannot return drafts. Authoring (`/v1/me/*`) runs as the token's user with `PUBLISHER`/author permissions. Preview tokens add a one-record lock clause (§4.2). Trusted internal reads that must bypass table permissions (feeds, sitemap generation from cached data) use `CorePermissionEnforcer.runTrusted("cms: public sitemap of published content", ...)`, never a blanket bypass.

### 6.4 Multi-site and multi-tenant

- **Public:** strictly per resolved site via the site key.
- **Admin, single organization (default):** admins see all sites.
- **Admin, multi-tenant host** (`adminSiteScoping=true`): sessions get site keys for the sites granted to them (from a host `CmsSiteGrantSupplier`, e.g. groups `cms-site-<key>-editors`), and all-access only for `ADMIN`. Every content table is site-locked directly or by join lock. Reference tables (types) are global and read-only for non-admins.
- **Tenancy above sites** (a host's own tenant key): the host adds its tenant lock to `site` via `getTableMetaDataCustomizer()`; content tables inherit through join locks. The qbit doesn't model tenants itself.
- **Storage isolation:** key layout begins with `{site}/`; presigned upload URLs are per site and per user, 15 min, size-capped (today 25 MB in `MediaStorage`).
- **Audit:** field-level audit on all content tables (today's `AuditLevel.FIELD`), receipts for every publish, and ESB events for downstream audit if configured.

---

## 7. Testing, docs and adoption

### 7.1 Testing strategy

| Layer | How | Gate |
|---|---|---|
| Unit | H2 (PostgreSQL mode) `FreshCmsInstance` with every feature combination that `validate()` allows | PR |
| Architecture | ArchUnit: no host imports; no raw table-name literals outside `CmsTables`/`CmsSql`; processes have `withTableName` and are not in any app's nav; every table has permission rules | PR |
| Schema | Testcontainers PostgreSQL: (1) fresh install with `cms_`; (2) fresh install with a namespace; (3) two namespaces in one database; (4) rename round trip: a kof22-shaped schema → `adopt/rename-to-prefix` → equals the `cms_` baseline → reverse → equals the original, plus compatibility-view reads and writes; (5) upgrade from every released version's schema to head; (6) collision halts; `RDBMSBackendAssessor` clean after each | PR |
| Storage | Testcontainers MinIO for `S3CmsStorage` (today S3 is only faked in Website-Backend tests): upload, presign, renditions, metadata extraction | PR |
| Search | Testcontainers OpenSearch with `qbit-quick-search` (optional profile), plus the no-provider fallback | nightly |
| ESB | embedded Artemis: publications, media trigger, idempotent redelivery | nightly |
| Site API contract | (1) OpenAPI snapshot diff: a breaking diff fails unless the major version changes; (2) golden JSON responses (curated fields, envelope, headers, CORS, cache headers) migrated from `T/siteapi/SiteApiPublicIntegrationTest.java` (13), `SiteApiAuthoringIntegrationTest` (9), `SiteApiUnitTest` (12); (3) consumer contract: `@qrun-io/cms-next` fixtures replayed against the API | PR |
| Permissions red team | with enforcement ENFORCE: anonymous cannot read drafts/unlisted/other sites; editor cannot publish; drafter cannot `PUBLISH`; preview token reads only its record and expires; site-scoped admin cannot cross sites; publisher token cannot call routes outside its allow-list or mix cookies + bearer | release |
| Next UI | Playwright: CMS home and each sub-app home at 360/768/1280 widths (readability rules), FILE_PREVIEW thumbnails, actions present, no processes in nav | release |
| Migration rehearsal | the §3.6.2 steps 2 and 2r (forward and rollback) against a fresh production snapshot, scripted, before kof22 cutover and before every qbit major | per cutover |
| Publisher CLI | the opt-in `-Dkof.publisher.jar` contract tests (`T/metadata/BlogPrivacyIntegrationTest.java:342-490`) become a generic `-Dcms.publisher.jar` profile | release |

### 7.2 Docs (in `QRun-IO/qbit-cms`)

README (Install, Configuration reference, Features, Permissions, Operational tables, Upgrading), `docs/DESIGN.md` (this spec, trimmed), `docs/ddl/postgres.sql` (generated), `docs/site-api/` (contract, changelog, versioning policy), `docs/MIGRATION-*.md`, `docs/adopting-existing-schema.md`, CHANGELOG with "Schema changes" and "Site API changes" sections, plus `CLAUDE.md`/`AGENTS.md` per qbit convention.

### 7.3 Adoption checklist (consumer-neutral; to add to `qqq:docs/adoption-checklist.adoc` when 1.0.0-RC.1 ships)

> **qbit-cms**
>
> 1. **Versions.** QQQ ≥ 4.1.0-RC.4, Next ≥ 1.0.0-RC.14, `qbit-cms` and `qbit-cms-site-api` at the same version. Material is not supported.
> 2. **Decide names.** Choose `tableNamePrefix` (default `cms_`) and app placement (`parentAppName`). Check for collisions: `validate()` lists them at startup.
> 3. **Migrations.** Put the qbit jar (or its `qbit-cms/db/changelog/` directory) on your Liquibase classpath. Add the `cms.tablePrefix` and `cms.features` properties and the include. Take a backup, run, and confirm the baseline changesets ran (fresh) or were `MARK_RAN` (adopting).
> 4. **Register.** Call `CmsQBitProducer` after your backends, scheduler and (optional) ESB provider are defined. Register `qbit-cms-site-api` only if you serve a public site.
> 5. **Storage.** Configure an S3-compatible bucket (versioning on) or filesystem storage, a public base URL, and rendition widths.
> 6. **Permissions.** Turn on core permission enforcement. Map your IdP groups to `CmsRole`s in `roleMapping`, and call `CmsSecurity.applySessionPermissions` from your authentication customizer. Grant `ADMIN` to administrators only.
> 7. **Sites.** Create your site and domains. Pick a site resolver (fixed for one site; host-based for several).
> 8. **Public API.** Set the public base URL, allowed origins, and the features you want (contact, authoring and app association are off by default). Route your public host to the internal path.
> 9. **Optional integrations.** Quick Search (add the CMS entities to `searchableEntityClasses`), ESB (set `esbDestinationPrefix`), CMS analytics, preview tokens, revalidation webhooks.
> 10. **Verify.** CMS home loads with real counts, a draft is invisible to the public API, publish makes it visible, sitemap and feeds include it, uploads produce renditions.
> 11. **On upgrade.** Read the qbit CHANGELOG "Schema changes" and "Site API changes" sections before deploying, run migrations before the app.

---

## 8. Delivery plan

Estimates assume one senior engineer with AI assistance, plus about 25% of a frontend engineer from phase 5, in calendar weeks. Each phase is PR-sized slices; nothing merges without review.

| Phase | Scope | Where | Exit criteria | Est. |
|---|---|---|---|---|
| **Pre** | #107 (core permission enforcement, needs QQQ 4.1.0-RC.4) and #98 (analytics extraction) complete | Website-Backend, `qbit-cms-analytics` | both merged and deployed | (existing work) |
| **0. Prep refactors** | R1-R15 (§1.4) inside Website-Backend, behaviour-identical; incidental doc/bug fixes | Website-Backend | `CmsModuleBoundaryTest` green; Site API golden responses unchanged; production soak 1 week | 3.5 wk |
| **1. qbit-cms core** | Repo from the qbit template (multi-module); `CmsQBitProducer`/`Config`/`Runtime`/`CmsTables`; CORE + MEDIA + BLOG features; storage SPI with S3/filesystem; permissions + roles; CMS home + sub-app homes; prefix-parameterized changelog + baseline; install doctor; Testcontainers Postgres/MinIO matrix | `qbit-cms` | 1.0.0-RC.1 published; installs cleanly into a blank `qqq-app-starter` app (the "any running QQQ system" acceptance test) with `cms_` | 4 wk |
| **2. Remaining features** | PROJECTS, PHOTOS, PUBLISHING (processes, receipts, related versions, URL migration, publisher token auth), STREAM (view + URL scheme), ESB publications + media trigger + no-ESB fallback, developer panel | `qbit-cms` | RC.2; publisher CLI contract profile green; ESB nightly green | 3 wk |
| **3. Site API artifact** | `qbit-cms-site-api`: resources, routes, OpenAPI shaping, docs page, feeds, sitemap, search, contact, authoring, AASA, `CmsSiteApiConfig`; contract test suite | `qbit-cms` (module) | kof22 golden responses replay with 0 diffs; OpenAPI snapshot gate in CI | 3 wk |
| **4. kof22 cutover (rename to `cms_`, D3)** | `adopt/rename-to-prefix` changelog with explicit rollbacks and compatibility views; CI round trip; publisher CLI release and config; admin Traefik redirect middleware; notebook inventory; two production-snapshot rehearsals plus a rollback rehearsal; the maintenance window (§3.6.3); 7-day watch; cleanup changeset; delete the moved code from Website-Backend | `qbit-cms`, Website-Backend, publisher CLI, cluster-gitops (redirect middleware) | Rehearsals green (counts and checksums equal, 0 Site API diffs, CLI publish works, rollback restores the original); window completed inside the booked 60 min; no Data Health regressions after 7 days | 3 wk |
| **5. Frontend package + preview + ISR** | `@qrun-io/cms-next` (client, fetchers, section registry, route factories), `create-qqq-cms-site` starter, preview tokens (backend + package), revalidation notifier; Website-Frontend adopts the package | new npm package repo, `qbit-cms`, Website-Frontend | starter renders a fresh install end to end; Website-Frontend on the package with no visual change | 3 wk |
| **6. Multi-site + second adopter** | `HostSiteResolver`, `adminSiteScoping`, site grants, multi-site tests; adoption checklist into qqq docs; a second consumer (blank starter app or another QQQ app) adopts | `qbit-cms`, `qqq` docs | two sites served from one install with isolation red team green | 2 wk |

Total: about **21.5 weeks** after the prerequisites, of which phases 0-4 (16.5 weeks) deliver "kof22 runs on the qbit and a blank QQQ app can install it".

**Release coordination.** The release captain owns QQQ RCs; this work needs no QQQ core change beyond what RC.4 ships (optional upstreaming of widget helpers and API error envelopes would be separate, later QQQ PRs). `qbit-cms` uses the qbit release-branch flow. The npm package has its own semver.

### 8.1 Risks

| Risk | Mitigation |
|---|---|
| The baseline drifts from kof22's real schema (hand edits, out-of-order changesets: two `0006` and two `0009` files in `LB/changesets/`) | Generate the baseline from a schema-only dump of production, diff in CI, rehearse on a snapshot |
| The rename misses an object (a constraint, a sequence, a column that stores a table name, raw SQL in a notebook) | `CmsRenamePlan` generated from the catalog, not by hand; CI round trip asserts schema equality both ways; two production-snapshot rehearsals; compatibility views for anything still using old names |
| The rename takes too long (large analytics `UPDATE`) or blocks on locks | Timed in rehearsal; batch the analytics update before the window if needed; `lock_timeout` on the migration so it fails fast instead of queueing behind long transactions; publishing frozen |
| Compatibility views can't serve the old image's inserts (identity defaults, `RETURNING id`) | Verified in rehearsal; fallback is 10-15 min of planned public downtime (§3.6.3) |
| Old admin links, notebooks or external ESB consumers depend on old names | 90-day redirect middleware; notebook inventory and fixes before the window; CHANGELOG notice of the new CloudEvent types |
| Liquibase empty source-prefix substitution in the rename changelog misbehaves | Verify in phase 1; fallback: a generated changelog |
| Raw SQL in 126 Data Health rules and the stream view resists prefixing | `CmsSql` placeholders + tests that run every rule against a `cms_`-prefixed schema |
| The iOS app's OpenAPI contract breaks | Byte-compare `openapi-extensions.yaml` output; contract tests; semver gate |
| Permission regressions when rules become non-open for generic installs | kof22 keeps `openReads=true`; red-team suite on the rehearsal database |
| Scope creep into a general page builder | Non-goals: no WYSIWYG page builder, no multi-language content in 1.0 (the early sketch's `default_language` is deferred), no renderer in Java |
| Next has no plugin system for custom widgets | CMS homes use generic HTML/`block` widgets only; no custom widget types in 1.0 |
| Licensing of extracted code | Decided (D2): Apache-2.0; proprietary headers are replaced as files move, as for analytics |
| Release-captain contention on QQQ | No QQQ core changes required; spec PR is docs-only |

### 8.2 Non-goals (1.0)

WYSIWYG or block editor beyond today's section JSON; localization/multi-language content; comments; e-commerce; a server-side renderer in Java; Material UI support; migrating the deprecated `/public-api/`.

---

## 9. One-page summary

- **What:** turn the kof22 CMS into `QRun-IO/qbit-cms` (+ `qbit-cms-site-api`), installable into any QQQ 4.1 app with Next, with `qbit-cms-analytics` (already in flight) as an optional companion.
- **How:** generalize the analytics module's proven pattern: one typed config, a QBit producer, prefixed standard QQQ tables, a host-included prefix-parameterized Liquibase changelog, and SPIs for site resolution, storage, URL scheme, role mapping and mail.
- **Biggest work items:** 15 in-repo refactors to remove kof22 constants and raw table names (3.5 wk); the qbit itself (7 wk: core 4 + remaining features 3); the Site API artifact (3 wk); the kof22 cutover with a reversible rename to `cms_` (3 wk); optional frontend package, preview and ISR (3 wk); multi-site (2 wk).
- **kof22 data:** never copied. Tables, constraints and sequences are renamed to `cms_` in one transaction, with name-as-data columns updated and compatibility views under the old names. It is rehearsed twice on a production snapshot (counts, checksums, golden Site API diffs, publisher CLI). The window is 60 minutes booked and 30-45 expected, with the public-site impact target being none. Rollback is the previous image (via the compatibility views) or the reverse migration. The Site API doesn't change.
- **Security:** core permission enforcement on; per-table rules; IdP-neutral role mapping; public reads filtered by record locks per resolved site; preview tokens scoped to one record.
- **Frontend:** headless Site API contract first; `@qrun-io/cms-next` package + starter later; Website-Frontend becomes the first consumer.
- **Gate:** owner decisions made 2026-10-12 (§10); #107 and #98, then phase 0.

## 10. Owner decisions (decision log, 2026-10-12)

The owner chose the recommended option **A** for every question except **D3**, where the owner chose **B** (rename). Options are kept for the record.

1. **D2 License and visibility** (blocks any code move)
   - **A (recommended, chosen):** relicense the extracted CMS code Apache-2.0 and develop in public `QRun-IO/qbit-cms`, as already decided for analytics. kof22-specific code stays proprietary in Website-Backend.
   - B: private `KofTwentyTwo/qbit-cms`, proprietary, private Maven registry. Fewer adopters; QRun-IO docs can't reference it.
   - C: dual license (AGPL/commercial).
2. **D1 Packaging**
   - **A (recommended, chosen):** one repo, `qbit-cms` with `CmsFeature` flags + separate `qbit-cms-site-api` artifact; analytics stays `qbit-cms-analytics`.
   - B: eight sub-qbits.
   - C: one jar including the API.
3. **D3 kof22 table names**
   - A (recommended, declined): keep today's unprefixed names permanently (adoption mode, zero DDL).
   - **B (chosen by the owner, overriding the recommendation):** rename to `cms_` during the cutover window (§3.6).
   - C: keep now, rename later in a separate window.
4. **D14 Sequencing**
   - **A (recommended, chosen):** finish #107 (RC.4) and #98 first, then phase 0.
   - B: run phase 0 in parallel with #98 (more merge conflicts in `cmshome/` and `WidgetHtml`).
5. **D5 Schema ownership**
   - **A (recommended, chosen):** the qbit ships a prefix-parameterized changelog that the host includes and runs; generated reference DDL for non-Liquibase hosts.
   - B: reference DDL only (sql-workbench style); hosts write their own changesets.
   - C: the qbit runs DDL at startup.
6. **D6 Site API in the qbit**
   - **A (recommended, chosen):** yes, as `qbit-cms-site-api` with configurable name, path, hosts and auth; v1 contract frozen; kof22 keeps `api.kof22.com` unchanged.
   - B: the Site API stays in the host; the qbit is admin-only.
7. **D4 Default prefix for new installs**
   - **A (recommended, chosen):** `cms_`.
   - B: no prefix.
8. **D11 iOS authoring, contact form, app association**
   - **A (recommended, chosen):** in the API artifact as opt-in features, IdP-neutral config.
   - B: host-only extensions.
9. **D12 Machine publisher token auth**
   - **A (recommended, chosen):** generic config-driven verifier in the publishing feature (issuer, audience, introspection, groups, generated route allow-list).
   - B: host-only; the qbit exposes processes and the host secures them.
10. **D10 Data Health rules**
    - **A (recommended, chosen):** ship the CMS rules as data in the qbit behind a small rule-provider interface, registered only when the host has a Data Health framework; the framework itself stays in Website-Backend until a `qbit-data-health` (or the assistant spec's `DataIssueProvider` SPI) exists.
    - B: build `qbit-data-health` first.
    - C: rules stay in the host.
11. **D8 Frontend**
    - **A (recommended, chosen):** headless contract now; `@qrun-io/cms-next` package + starter in phase 5; Website-Frontend first consumer.
    - B: headless only, no package.
    - C: extract Website-Frontend as a template repo.
12. **D9 Preview and revalidation**
    - **A (recommended, chosen):** signed one-record preview tokens + HMAC revalidation webhooks, both opt-in; kof22 stays `force-dynamic` until it wants caching.
    - B: defer both past 1.0.
13. **D7 Multi-site default**
    - **A (recommended, chosen):** `FixedSiteResolver` default; `HostSiteResolver` and admin site scoping in phase 6.
    - B: host-based resolution required from 1.0.
14. **D13 Deprecated `/public-api/`**
    - **A (recommended, chosen):** not migrated; stays in the host until its 2027-01-31 sunset, then deleted.
    - B: delete before cutover.
15. **D15 "Any running QQQ system" acceptance target**
    - **A (recommended, chosen):** a blank `qqq-app-starter` app in CI (phase 1), plus a real second adopter in phase 6.
    - B: a specific second product first.

---

## Appendix A: Research references

- Website-Backend `origin/main` @ `ec5bb65`: `docs/design/cms-analytics-module.md` (extraction + license decision), `docs/site-api.md`, `docs/publisher-authentication.md`, `docs/publishing-content-extensions.md`, `docs/public-stream.md`, `docs/cms-media.md`, `docs/admin-dashboards.md`, `docs/configuration.md`; issues/PRs #98 (analytics extraction), #107 (core permission enforcement, draft).
- Website-Frontend `origin/main` @ `d670cc1`: `src/api/SiteApi.ts`, `src/lib/api.ts`, `src/components/sections/SectionRenderer.tsx`, `src/lib/cms-redirects.ts`, `src/proxy.ts`, `src/lib/analytics/*`, `src/app/layout.tsx`.
- QQQ: `docs/migration/4.1.adoc`, `docs/adoption-checklist.adoc`, `docs/misc/CorePermissionEnforcement.adoc`, `docs/misc/DeveloperPanels.adoc` (#1076), `docs/specs/2026-09-25-qqq-esb-design.md`, `docs/specs/2026-10-11-qqq-assistant-qbit-design.md`, PR #1065 (Authentik qbit spec, draft).
- qbits: `qbit-sql-workbench` (`SqlWorkbenchQBitProducer`, `docs/ddl/postgres.sql`), `qbit-quick-search` (`QuickSearchQBitConfig`, `docs/MIGRATION-1.x.md`), `qbit-authentik` (`AuthentikFeature`, `AuthentikGuard`), `qbit-assistant` (developer panel registration, schema doc, ArchUnit), `qbit-template` (`QBIT_TYPE_TAXONOMY.md`), `qbit-template-data` (`ExampleLiquibaseGenerator`), `qbit-template-application/qbit-platform-content.md` (superseded sketch).
