# QQQ Dashboard Standard

Status: **Approved** (owner, 2026-10-16) · Author: James Maes (drafted with Claude)

Scope: the single top-level **Dashboard** (the landing page), every **app and app-group home**, and the **Admin home** in a QQQ application rendered through the UI API (`/qqq/v1`). The rules are written for multi-user, multi-tenant business apps built on QQQ and its qbits. Any QQQ deployment can be used as a test bed.

Rubric items marked **Auto** or **Partial** in section 7 are checked in CI (section 7.4).

Words used here: **must** is required to pass the rubric. **Should** is the default; you can depart from it if you write down why. A **zone** is a horizontal band of the page with one job. A **business user** is anyone who uses the content apps for their work. A **system admin** is someone who runs the system itself.

---

## 1. Principles and owner decisions

### 1.1 First principle: content first

> For the main app, the focus is the content applications, not the system itself. For 99% of the people, they will never see the system; they only access and use the application content for work-related things. Only system admins care about the system itself.
> (Owner, 2026-10-16)

What this means in practice:

- **P0.1** The landing Dashboard and every app home serve business users doing their work in the content apps (for example CMS, CRM, Marketing, EDI, Analytics).
- **P0.2** No system counts, system health, job or worker status, queue depths, integration health or platform statistics on the landing Dashboard, for any role, including system admins.
- **P0.3** System status belongs only to the **Admin home** and the system app homes under Admin, which only system admins can see.
- **P0.4** A business app home shows the state of the *business work* in that app. When something in the system stops that work (for example, email sending is paused), the app says so in business terms ("Campaign sends are paused") with a link to an admin, not with a health widget.
- **P0.5** Every page is scored first from the view of a non-admin business user, then from the view of a system admin (section 7.1).

### 1.2 Owner decisions

Decided 2026-10-16. Each one overrides any other wording in this document.

| # | Decision |
|---|---|
| D1 | **No user layout personalization.** Users cannot add, remove, reorder, pin, hide or collapse-and-persist widgets. Each role gets one well-chosen default layout, set in metadata. Role-based content stays. |
| D2 | **Generic standard.** It applies to all QQQ business apps (multi-user, multi-tenant). A single deployment is only a test bed. |
| D3 | **Location.** The standard lives in QQQ `docs/`, and rubric items are checked in CI wherever possible. |
| D4 | **Product messaging.** The landing may show one small, dismissible "What's new" card, and nothing louder. |
| D5 | **Favorites are kept.** Users may favorite records and saved views. Favorites are bookmarks (data), not layout. |
| D6 | **Widget filter choices are not remembered per user.** Page scope lives in the URL. `storeDropdownSelections` is off for home widgets. |
| D7 | **No tenant-admin override of role layouts in v1.** Layouts come from metadata only. |
| D8 | **Landing headlines are allowed:** at most one link tile per app, each with a single meaningful KPI, at most 6 tiles. |
| D9 | **One core attention API.** "Needs attention" is fed by any qbit through one core attention API. qbit-alerts is one source among others. |
| D10 | **Roles come from qbit-user-role-permissions.** The standard defines no new role list. |
| D11 | **Maximum content width is 2400 px.** Tables and trends stay full width within it. |
| D12 | **What's new shows product release notes only.** |
| D13 | **`homeAppName` is kept** as an opt-in instance setting. |
| D14 | **Refresh.** The attention zone updates live. Other widgets refresh every 60 s while the page is visible. |
| D15 | **No Assistant prompt box on the landing.** The header Ask button is enough. |
| D16 | **5-second test proxy.** On single-owner deployments an auditor agent may stand in for the 5-second test. |
| D17 | **CI home and gating.** Metadata checks live in QQQ core; page checks (Playwright, axe, Lighthouse) live in qqq-frontend-next. They report only for 2 releases, then block merges. |

---

## 2. Research summary

### 2.1 What a dashboard is (and is not)

- **Few** defines a dashboard as a visual display of the most important information needed to reach one or more objectives, arranged on a single screen so it can be monitored at a glance. Dashboards are not reports, exploration tools or portals. His common pitfalls include going past one screen, giving numbers no context (no comparison or trend), too much precision, and the wrong display medium. Sources: [Few, *Common Pitfalls in Dashboard Design* (PDF, 2006)](https://support.itrsgroup.com/hc/en-us/article_attachments/115004392393), [UXmatters review of *Information Dashboard Design*](https://uxmatters.com/mt/archives/2007/04/book-review-information-dashboard-design.php), [Tableau, "What the heck is a dashboard"](https://www.tableau.com/sites/default/files/webinars/what_the_heck_is_a_dashboard_anyways.pdf).
- **NN/g** describes dashboards as single-page, at-a-glance views consumed "with a minimum of interaction or cognitive processing". It separates **operational** dashboards (time-sensitive, "what needs me now") from **analytical** ones (trends, decisions). Charts should encode values as **length and 2-D position**, and strong signals such as saturated colour should be saved for the few values that matter. Sources: [NN/g, Dashboards: preattentive processing](https://www.nngroup.com/articles/dashboards-preattentive/), [NN/g, Data visualizations for dashboards](https://www.nngroup.com/videos/data-visualizations-dashboards/).
- **Carbon (IBM)** separates *presentation* dashboards (KPI status) from *exploration* dashboards: the most important metric goes top-left with the most visual weight, the layout follows an F-pattern, secondary detail is on demand, one colour per data series across the dashboard, and white space groups items. Source: [Carbon, Dashboards](https://v10.carbondesignsystem.com/data-visualization/dashboards).
- **PatternFly**: each card holds one metric or one tightly related group, with card filters at the top right and actions as links in the footer. Source: [PatternFly dashboard design guidelines](https://patternfly.org/patterns/dashboard/design-guidelines).

**For QQQ:** the landing Dashboard is an *operational* page about me and my work now. App homes are *presentation* pages with a short way into analysis. Neither page is a report, and neither is about the platform.

### 2.2 Patterns from strong product dashboards

| Product | What it does well | Lesson for QQQ |
|---|---|---|
| **Stripe** | Home holds a small set of business charts plus "recent actions needed" (disputes, verifications). Home charts are labelled as *estimates*, with Reports for exact figures. ([docs](https://docs.stripe.com/dashboard), [support](https://support.stripe.com/questions/dashboard-home-page-charts-for-business-insights)) | Put actions next to numbers. Be honest about how precise and how fresh a number is. |
| **Linear** | "My issues" and Inbox are the centre of the app. A command menu (Cmd/Ctrl+K) reaches everything, `?` lists shortcuts, and the inbox can be triaged with J/K, E and H (snooze). ([Inbox](https://linear.app/docs/inbox), [Navigating Linear](https://linear.app/enablement/guides/navigating-linear)) | Personal work queues come first. Keyboard use is fully supported; snooze and dismiss are standard verbs. |
| **GitHub** | The 2025 home moved from an activity feed to **My Work**: short lists of your PRs, issues and agent tasks. The social feed moved to its own page. ([Aug 2025](https://github.blog/changelog/2025-08-28-improvements-to-the-home-dashboard-available-in-public-preview/), [Oct 2025](https://github.blog/changelog/2025-10-28-home-dashboard-update-in-public-preview)) | Activity feeds move *off* the landing in favour of "my work". |
| **Vercel** | The team overview sorts activity by *your own* activity first. Navigation tabs look the same at team and project level. ([team overview](https://vercel.com/changelog/improved-team-overview-page), [nav redesign](https://vercel.com/changelog/dashboard-navigation-redesign-rollout)) | Relevance to the viewer decides the sort order. Navigation looks the same at every level. |
| **Datadog** | Reusable widget groups with permissions. Template variables apply one scope to a whole board. ([Powerpacks best practices](https://docs.datadoghq.com/dashboards/guide/powerpacks-best-practices)) | Widgets are reusable units defined in metadata. One page-level scope (time range, filters) is shared by every widget. |
| **GA4** | Home holds "Insights & recommendations" for anomalies; cards can be dismissed. New properties show this section empty, which is normal. ([GA4 Home](https://support.google.com/analytics/answer/11197963)) | Exceptions are generated, not hand-picked. Every recommendation can be dismissed. An empty insight zone is valid and must be explained. |
| **HubSpot** | Per-user default dashboards; users have asked for years for defaults by role. ([manage dashboards](https://knowledge.hubspot.com/dashboards/manage-your-dashboards), [idea thread](https://community.hubspot.com/t5/HubSpot-Ideas/Change-Default-Dashboard/idi-p/28637)) | Ship role-based defaults out of the box. |
| **Salesforce Home** | The Assistant lists items to act on in a fixed, documented priority order, and items age out. The performance chart shows Closed against Goal. Admins assign Home pages by profile. ([Trailhead](https://trailhead.salesforce.com/content/learn/modules/lex_migration_whatsnew/lex_migration_whatsnew_more)) | The attention list follows a *documented* priority rule and drops stale items. KPIs are shown against a target. Home pages are assigned by role. |
| **Notion Home** | Recently visited, My tasks across every task database, Upcoming events. Guests do not get Home. ([Notion Home](https://www.notion.com/help/home-and-my-tasks)) | "My tasks" works because the platform knows which field holds the assignee. Some roles need a different landing. |

### 2.3 Messages, alerts and announcements

- **Atlassian** assigns a channel to each job: **banners** only for critical system-level warnings about lost data or functionality, **flags** for confirmations, **section messages** above the area they refer to, and a separate "discovery" type for new things. ([Messages pattern](https://atlassian.design/patterns/messages/), [Flag](https://atlassian.design/components/flag))
- **Carbon**: notifications interrupt, so use them sparingly, keep them short and always give a next step. ([Notification pattern](https://carbondesignsystem.com/patterns/notification-pattern))
- **GOV.UK** uses the notification banner only for something the user needs to know that has *changed*; overused banners get ignored. ([notification banner](https://design-system.service.gov.uk/components/notification-banner/), [DfE design history](https://becoming-a-teacher.design-history.education.gov.uk/register-of-training-providers/how-we-show-success-error-and-notification-banners/))
- **NN/g** separates **indicators**, **validations** and **notifications**. ([NN/g](https://www.nngroup.com/articles/indicators-validations-notifications/))

**For QQQ:** "What's new" is *discovery*. It is never a banner or a modal and never sits above work or exceptions. A banner is reserved for loss of data or functionality.

### 2.4 Friendliness, onboarding, empty states and help

- **NN/g on empty states:** a blank widget lowers confidence. An empty state says what the system is doing, teaches the feature and gives a direct path to the key task. ([NN/g](https://www.nngroup.com/articles/empty-state-interface-design/))
- **Polaris / Shopify:** an empty state explains *why* there is nothing and *what to do*, with at most two actions, and distinguishes "nothing yet", "filtered out" and "not configured". ([app-home empty state](https://shopify.dev/docs/api/app-home/patterns/compositions/empty-state), [Polaris EmptyState](https://polaris-react.shopify.com/components/layout-and-structure/empty-state))
- **NN/g on help:** context-free "push" tutorials do worse than "pull" help tied to the user's current goal. ([NN/g help and documentation](https://www.nngroup.com/articles/help-and-documentation/))

**For QQQ:** no product tours on login. First run means a short *setup checklist* built from real state. Help sits next to the thing it explains.

### 2.5 Personalization, customization and roles

- **NN/g:** *customization* means the user decides; *personalization* means the system guesses. Customization tools must be easy to find, easy to undo and clearly worth the effort. ([NN/g, personalization is overrated](https://www.nngroup.com/articles/personalization-is-over-rated/), [NN/g video](https://www.nngroup.com/videos/personalization-customization/))
- Salesforce and HubSpot show that **role-based curated defaults** are what businesses need, more than a free-form canvas.

**For QQQ (D1, D5, D7):** one curated default layout per role, no user layout customization and no tenant override in v1. *Content* is still personal by rule: my records, my recents, my favorites and items routed to me come from data and ownership fields.

### 2.6 Speed, loading and freshness

- **Response-time limits** (Nielsen): 0.1 s feels instant, 1 s keeps the user's train of thought, 10 s loses attention. ([NN/g response times](https://www.nngroup.com/articles/response-times-3-important-limits/))
- **Skeletons** must match the final layout so they cause no shift, and are only used for waits under about 10 s. ([NN/g, skeleton screens](https://www.nngroup.com/articles/skeleton-screens/), [Atlassian Skeleton](https://atlassian.design/components/skeleton))
- **Core Web Vitals** "good" at p75: LCP ≤ 2.5 s, INP ≤ 200 ms, CLS ≤ 0.1. ([web.dev](https://web.dev/articles/vitals))
- **Being honest about freshness and precision is part of the design.**

### 2.7 Accessibility and responsive layout

- **WCAG 2.2** criteria that matter for dashboards: **2.4.11 Focus Not Obscured (AA)**, **2.5.8 Target Size (AA)** (at least 24×24 CSS px), **2.5.7 Dragging Movements (AA)**, **3.2.6 Consistent Help (A)** and **3.3.7 Redundant Entry**. ([W3C, What's new in 2.2](https://www.w3.org/WAI/standards-guidelines/wcag/new-in-22/))
- **1.4.10 Reflow:** content works at 320 CSS px with no two-way scrolling. ([W3C Understanding Reflow](https://www.w3.org/WAI/WCAG22/Understanding/reflow.html))
- **Window size classes:** compact < 600, medium 600–839, expanded 840–1199, large 1200–1599, extra-large ≥ 1600. ([Android window size classes](https://developer.android.com/develop/ui/compose/layouts/adaptive/window-size-classes)) The six check widths fall as follows: 390 compact, 768 medium, 1024 expanded, 1440 large, 1920 and 3200 extra-large.

### 2.8 How to test it

- **5-second test and first-click test** ([NN/g](https://www.nngroup.com/videos/validate-visual-design/)): show the page for 5 s, hide it, then ask *What is this page for? What needs your attention? Where would you click to deal with it?* On single-owner deployments an auditor agent may answer in place of users (D16; see 5.1).

---

## 3. Page profiles

| Profile | Audience | Question it answers | Rubric |
|---|---|---|---|
| **Landing Dashboard** | Everyone | What needs *me* in my work, across all content apps? | L1–L10 (7.2) |
| **App home** (content app) | Business users of that app | How is the work in *this area* going, and what needs doing here? | A1–A10 (7.3) |
| **App group home** | Business users of the child apps | How are the child apps going? (Links into each child; no re-rendered child widgets.) | A1–A10 (7.3) |
| **Admin home** and system app homes | System admins only | Is the *system* healthy, and what needs an admin? | A1–A10 with the Admin profile (6, 7.4) |

---

## 4. Standard: the landing Dashboard

### 4.1 Principles

1. **Content first (P0).** The landing is about the user's work in the content apps. It never shows the system.
2. **About me and now.** It answers, in this order: *Is anything wrong in my work? What is mine to do? Where was I?*
3. **One landing page; apps own their data.** The landing *routes* to app homes. It never re-renders an app's charts or tables. A number may appear only as a headline link tile (rule L-4).
4. **Manage by exception.** When nothing is wrong, the attention zone shrinks to one line. Calm is a valid state.
5. **Curated by role, not customizable.** Each role has one default layout defined in metadata (D1, D7).
6. **Real, permitted and timestamped.** Every value is real, permitted for this user and tenant, and shows its as-of time.
7. **Headless by design.** Everything on the page comes from `/qqq/v1` as typed data, so a CLI, TUI or native client can render the same landing as a list.

### 4.2 Required zones, in priority order

| # | Zone | Required? | Contents | Rules |
|---|---|---|---|---|
| 1 | **Header** | Must | Greeting with first name; tenant or org name (with a switcher if the user belongs to more than one); one-line status summary ("2 items need you · updated 09:41"); global search entry; the header Ask button when the Assistant is installed. | One line at ≥ 1024 px, at most two on phones. No hero image. Greeting is time-of-day in the *user's* time zone. No Assistant prompt box on the page (D15). |
| 2 | **Setup** (first run only) | Conditional | Checklist of 3–6 real *business* steps for this role ("Import contacts", "Create your first post", "Invite a teammate"), each ticked from real data. | Shown to new users and to admins of a new tenant until complete or dismissed; dismissal stored server-side. Zones 3 and 4 still render. Infrastructure setup (credentials, providers) belongs to the Admin home. |
| 3 | **Needs attention** | Must | Business exceptions *that involve me*, from the core attention API (D9): approvals waiting on me, overdue items assigned to me, records needing my review, alerts routed to me about business data, my campaign that failed to send. | Sort by severity, then age. At most 5 with "View all (n)". Each item has a verb ("Review", "Retry send", "Open") that runs a table or record action or deep-links to the record. Snooze and dismiss where the source allows. Updates live (D14). When empty: "All clear · checked 09:41". **No system items** (P0.2): failed scheduler jobs, worker or integration health go to the Admin home. |
| 4 | **My work** | Must (if any table has an owner or assignee semantic) | Records assigned to, owned by or awaiting me, grouped by app, due-date first. | Full-width list. At most 10 rows, then "View all" opens the owning table with a saved "Mine" view. |
| 5 | **Continue** | Must | **Recent**: the last 8 records and saved views I opened. **Favorites**: records and saved views I favorited (D5). | Both served by the UI API and stored server-side per user and tenant. Not in localStorage. Favorites are bookmarks, not layout: they never add, move or hide widgets. |
| 6 | **Headlines** | Should | At most 6 link tiles, one per content app at most, each the app's single most meaningful KPI: value, unit, period, comparison and as-of time (D8). | No charts. No system metrics (P0.2). The tile shows the same widget output as the app home (L-4). Clicking opens the app home. The tile set is chosen by role. |
| 7 | **Apps** | Must | The user's content apps as a list or grid: icon, name, one-line purpose, and an attention badge count of *business* items. | Order follows the sidebar. Only permitted apps. Admin appears for system admins as a plain link with no status badge. |
| 8 | **What's new** | Should | One small card: the latest unread product release note (title, 1–2 line summary, "Read more", "Dismiss") (D12). | Always below zones 3–5. Dismissal stored server-side. Never a modal, banner, toast, badge-pulse, carousel or autoplay video (D4). |
| 9 | **Help footer** | Must | Consistent help entry (docs, keyboard shortcuts `?`, contact or support) in the *same place on every page* (WCAG 3.2.6). | One line. |

**Above the fold.** At 1440×900, zones 1, 3 and the first rows of 4 are visible without scrolling. At 390×844, zones 1 and 3 are.

### 4.3 Rules

- **L-0 Content first.** No system counts or health on the landing for any role: no job, worker, scheduler, queue, cache, index, backup, certificate, integration-health or component-status widgets, and no "System" zone. A system admin's system view is the Admin home.
- **L-1 No vanity metrics.** Counts of tables, processes, apps, widgets, users or records-in-the-database are about the platform, not the business, and are forbidden.
- **L-2 No processes as destinations.** The landing never lists processes as navigation targets. Quick actions are *table actions* ("New post", "New contact"); record-level fixes are *record actions*.
- **L-3 Deep links everywhere.** Every row, tile and badge opens a specific URL (record, filtered table, or app home `#widget`).
- **L-4 Headline equals link.** A headline tile shows only a number the owning app already renders as a KPI, computed by the same widget code path.
- **L-5 Permissions are total.** A widget the user may not see is *omitted*: no locked shell, no "you don't have access" card, no count that leaks the size of hidden data. Every aggregate respects row-level security and tenant scope.
- **L-6 Freshness is shown.** Every zone shows a relative time ("updated 2 min ago") with the absolute time on hover or focus. Values older than their `staleAfter` get a stale indicator and a refresh control. Attention is live; other zones refresh every 60 s while the page is visible (D14).
- **L-7 Isolate failures.** One failing widget shows its own error state with a retry; the rest of the page renders. No global spinner.
- **L-8 No fake data.** No sample rows, lorem ipsum or demo charts. Empty zones use a guided empty state (5.3).
- **L-9 Messaging budget.** At most one small, dismissible what's-new card and zero promotional items per page view (D4). A banner is only for loss of functionality that affects the user's work, worded in business terms ("Email sending is paused; your scheduled campaigns will not go out. An admin has been notified.").
- **L-10 Fixed layout.** Zone order is fixed and widgets come from the role's default layout. No user controls to add, remove, reorder, pin, hide or persistently collapse widgets (D1). Snoozing or dismissing an *attention item* and favoriting a record are acting on data, not layout, and are allowed.

### 4.4 Anti-patterns (instant fail in review)

- Any system status on the landing: health summaries, failed-job counts, worker or queue status, "System" cards (L-0).
- Platform statistics ("42 tables, 17 processes").
- A chart or table copied from an app home, or a second "dashboard" anywhere in navigation.
- An activity firehose ("Alice updated Post 12") as primary content.
- A marketing hero, carousel, auto-opening "What's new" modal, or a product tour on login.
- An Assistant prompt box on the page (D15).
- Two-column card grids that squeeze lists under 560 px, and inner horizontal scrollbars.
- "No data" with no reason and no action.
- Recents or favorites held only in browser storage.
- "Customize dashboard", "Add widget", drag handles or widget pin icons (D1).
- Numbers without a period, unit or comparison.
- Red and green as the only signal of good or bad.

---

## 5. Standard: app and app-group homes

### 5.1 How it differs from the landing

| | Landing Dashboard | App home |
|---|---|---|
| Question it answers | What needs *me*, across all content apps? | How is the work in *this area* going, and what needs doing here? |
| Type (NN/g / Carbon) | Operational, personal | Presentation, plus a way into analysis |
| Data ownership | Owns none; only routes | Owns all of the app's widgets and data |
| Charts | None | One primary trend plus supporting charts |
| Tables | One list (My work) | Full-width work queues and tables |
| Scope controls | None (always "now") | Page-level time range and filters, in the URL |
| Navigation content | The user's apps | The app's own tables and saved views, grouped as the app declares |
| Empty state | Per zone | Per widget and per table, with setup guidance |
| App group home | n/a | One KPI strip and attention list across its children, and a link into each child. No re-rendered child widgets. |

### 5.2 Principles

1. **Content first (P0).** A content app home is about the business work in the app. It shows no system health (P0.4).
2. **One home per app, owning its data.** If a number about this app appears anywhere else, it is a link back here.
3. **Exceptions, then numbers, then trends, then work, then structure.** Carbon's F-pattern and Few's single screen in practice.
4. **Every number drills through** to the records behind it, as a filtered table.
5. **Every table is reachable and alive.** Each table is either populated and used, or shows a guided empty state explaining how it gets filled.
6. **Scope is explicit.** The time range and filters are visible, shared by all widgets and kept in the URL (D6).

### 5.3 Required zones, in priority order

| # | Zone | Required? | Contents | Rules |
|---|---|---|---|---|
| 1 | **App header** | Must | App name, a one-sentence purpose, 1–2 primary *table actions* ("New campaign"), page scope (time range, plus site or other filters when relevant), freshness ("updated 09:41") and a help link for the app. | Scope defaults per role from metadata and is written to the URL, so links are shareable. |
| 2 | **Attention (this app)** | Must | This app's business exceptions from the core attention API: failed sends, records needing review, overdue items, rejected documents. | Same contract as landing zone 3. When empty: "No issues · checked 09:41". When a system fault blocks the app's work, one business-worded item ("Sends are paused") routes the user to an admin; the fault's detail lives on the Admin home. |
| 3 | **KPI strip** | Must (operational and business apps) | 3–5 stat tiles: value, unit, period, comparison (previous period or target), direction (whether up is good) and as-of time. | Each tile drills to a filtered table. Show the precision the decision needs ("12.4k", not "12,431.27"). Never more than 5. Business measures only. |
| 4 | **Primary trend** | Should | One full-width time-series chart of the app's most important measure, with the comparison overlaid. | Line or bar only. No pie over 5 slices, gauges or 3-D. Series colours consistent across the app. |
| 5 | **Work queues** | Must if the app has workflow | Full-width tables: "Assigned to me", "Needs review", "Recently changed", each a saved view of a real table. | Full width at every size. At ≤ 768 px rows reflow to stacked label/value cards; never an inner horizontal scroll. |
| 6 | **Supporting widgets** | Optional | Breakdowns, top-N lists, maps. | Each at least 560 px wide or full width, ordered by declared priority. Whether a widget starts collapsed is set per role in metadata; a session-only expand/collapse toggle is fine (D1). |
| 7 | **Sections** | Must | The app's tables, grouped as the app declares, each with a record count and last-changed time. | Only tables the user may see. No processes; they run as actions on those tables. |
| 8 | **Learn** | Should | Docs links for this app. | At the bottom. The only release-note card is the landing's (D12). |

### 5.4 Rules

- **A-0 Content first.** No health, worker, queue, scheduler, provider-configuration or component-status widgets on a content app home for business users. Configuration gaps appear as a "not configured" empty state that names who can fix it ("Ask an admin to connect an email provider").
- **A-1** Widget layout comes from metadata (`app.widgets`, sections, role, priority). The front end never hard-codes app home content.
- **A-2** Every widget declares a **role** (section 8). The renderer orders zones by role, not by list position alone.
- **A-3** Every KPI tile and chart segment deep-links to a filtered table query, opened in the app's own table view.
- **A-4** Every table in the app's sections has rows *or* a guided empty state that says what fills it (an import, an integration sync, a user action, a scheduled job) and offers that action when the user may run it.
- **A-5** The time range and filters live in the URL; reloading or sharing the link reproduces the view, permissions allowing.
- **A-6** Each widget shows loading (a skeleton the shape of the result), empty (with reason), error (with message and retry) and stale states independently.
- **A-7** An app group home aggregates its children's attention and KPIs by *linking* (one tile per child app) and never re-renders child widgets.
- **A-8** No 2-column layouts below 1024 px. At ≥ 1440 px two widgets may sit side by side only if each keeps at least 560 px. Tables are always full width.
- **A-9** The page scope has a role default from metadata and lives in the URL. It is not remembered per user; `storeDropdownSelections` is off for home widgets (D6).
- **A-10** Help lives next to its widget (an "i" opening the widget's help content), not in a tour.

### 5.5 Anti-patterns

- A system or health widget on a content app home (A-0).
- A grid of equally weighted tiles with no hierarchy (the "wall of numbers").
- KPI tiles without comparison or period; percentages without a base count.
- Charts that cannot drill through, or that use colour as the only encoding.
- Every widget fetching its own time range, so numbers on one page disagree.
- An app home that is just a list of links, or one that duplicates another app's widgets.
- User layout controls (add, remove, reorder, pin, hide) (D1).
- Processes in the section lists or the sidebar.
- A table squeezed into a half-width card with an inner horizontal scrollbar.
- "Coming soon" or placeholder widgets.

---

## 6. Profile: the Admin home (and system app homes)

The Admin home is an app home whose audience is system admins. It is the one place for system status (P0.3). System app homes under Admin (for example Jobs, Backups, Alerts administration, Webhooks, Email, Storage, Search indexes, Audit, Users) follow the same profile.

### 6.1 Audience and visibility

- Only users with system-admin permission can see the Admin home, its child app homes, and the Admin entry in navigation. For anyone else they are *omitted* (L-5), not locked.
- The Admin home never appears as a landing for business users. `homeAppName` (D13) may point at it only on deployments whose every user is a system admin.

### 6.2 Zones

| # | Zone | Required? | Contents |
|---|---|---|---|
| 1 | **Header** | Must | "Admin", one-line purpose, freshness, help link. No primary table action is required. |
| 2 | **System attention** | Must | System exceptions from the core attention API: failed jobs, integration and delivery failures, expiring certificates or credentials, backup SLA misses, unhealthy components. Severity then age; each item actionable; live (D14). |
| 3 | **Health strip** | Must | 3–5 tiles of overall system status (for example: components healthy, jobs failed in 24 h, backups within SLA, deliveries failing), each with as-of and a drill link. |
| 4 | **Areas** | Must | The admin groups (Data, Features, Integrations, Operations, Users & Security) as links to their system app homes, each with a status badge. The Admin home does not re-render those apps' widgets (A-7). |
| 5 | **Supporting detail** | Optional | Recent transitions, scheduled jobs, component list: full width, collapsed by default per role if long. |

### 6.3 How the A-rubric applies to the Admin profile

| Item | Reading for the Admin profile |
|---|---|
| A1 | Purpose line required; a primary table action is *not* required. Business-user view: pass only if the page and its nav entry are omitted for non-admins. |
| A2 | System attention, first after the header. |
| A3 | The health strip is the KPI strip; "comparison" may be a threshold or SLA instead of a previous period. |
| A4 | A trend is optional; if present it follows A4. |
| A5 | Queues are lists of failing or pending system work (failed jobs, dead letters), full width, backed by saved views. |
| A6–A10 | As written. |

---

## 7. Scoring

### 7.1 How to score

- Score each item 0, 1 or 2. **Pass: ≥ 16/20 with no item at 0.** The target for every page is 20/20.
- **Two views, business user first (P0.5).** Score each page as a non-admin business user in the page's role, then as a system admin. The page's score is the **lower** of the two. For the Admin profile the business-user view is a single gate (the page and its nav entry are omitted for non-admins); if the gate fails, A1 and A8 score 0.
- Roles come from qbit-user-role-permissions (D10). Score once per role that can see the page, since each role has one layout (D1).
- Evidence for every score: screenshots at 390, 768, 1024, 1440, 1920 and 3200 px per role; a Lighthouse or RUM report; an axe run and a keyboard walkthrough.
- The 5-second test may be answered by an auditor agent on single-owner deployments (D16): it gets each screenshot for 5 s of context (one image, no DOM), answers the three questions, and its answers are compared with the page's declared purpose and current attention items.

**CI column:**

- **Auto**: fully machine-checkable, through metadata validation (`QInstanceValidator` plugin in core), UI API contract tests, Playwright at the 6 widths with seeded role users, axe-core and Lighthouse CI.
- **Partial**: CI checks the structural part; a human or the auditor agent checks the judgement part.
- **Manual**: human audit only.

### 7.2 Landing Dashboard rubric

| # | Item | CI | 0 | 1 | 2 |
|---|---|---|---|---|---|
| L1 | **Content first, single landing, no duplication** | **Auto**: nav metadata has exactly one dashboard; validator rejects landing widgets with role `trend`, `queue`, `breakdown` or any system role (`systemHealth`, `systemAttention`); every landing item carries a link | Any system count or health on the landing; or more than one dashboard in navigation; or the landing re-renders app charts or tables | One landing with no system content, but ≥ 1 widget duplicates an app widget or doesn't link to its owner | One "Dashboard"; only content-app material; every number and row links to its owning app or record; nothing copied from app homes |
| L2 | **Attention first** | **Auto**: Playwright checks DOM order (attention is the first zone after the header) and above-fold position at 1440×900; contract test checks sort order, action refs and that every item comes from the attention API with a non-system source | No business exception zone, or exceptions below the fold at 1440×900 | Present, but not first after the header, not sorted by severity then age, items lack an action, or system items mixed in | First content zone; severity-then-age; ≤ 5 items plus "View all"; each item has an action verb; live updates; empty state "All clear · checked HH:MM" |
| L3 | **Personal content by rule** | **Auto**: two seeded users in the same role get different My work, Recent and Favorites; recents read back from a second browser context; no `localStorage` writes for recents or favorites | Same content for every user | My work, Recent or Favorites missing, or held in browser storage | My work, Recent and Favorites present, user- and tenant-scoped, served by the UI API, derived from data |
| L4 | **KPI discipline / 5-second test** | **Partial**: validator rejects vanity and system sources and headline tiles missing unit, period, comparison or `asOf`; caps headlines at 6 and one per app; the 5-second test is manual or auditor-agent | Any vanity or system metric, or under 50 % correct on the 5-second test | Headlines lack unit, period, comparison or as-of; or 50–79 % correct | ≤ 6 headlines, one per app, each with unit, period, comparison, as-of and owner link; ≥ 80 % correct |
| L5 | **Permissions and role layouts** | **Auto**: per-role Playwright snapshot of the rendered widget set against role layout metadata; API test that unpermitted widgets are absent from the response; seeded hidden rows don't change counts | Any unpermitted widget, count or row visible; or locked placeholders | Permissions correct, but every role sees the same layout, or a no-access user gets a blank page | Correct for every role (no leaks in counts); each role gets its own curated default; a no-access user sees a guided message; no user layout controls (D1) |
| L6 | **Freshness and resilient states** | **Auto**: contract test that every widget response has `asOf`; fault injection fails one widget and checks the rest render; CLS during load ≤ 0.1; refresh cadence matches D14 | A global spinner, or a widget failure blanks or breaks the page | Some widgets lack as-of, skeleton or error-with-retry | Every widget: skeleton matching its final shape, as-of time, stale flag, isolated error with retry; attention live, others every 60 s while visible |
| L7 | **Performance budget** | **Auto**: Lighthouse CI and RUM thresholds; per-widget server timing p95 | LCP > 4 s or CLS > 0.25 at p75 | "Needs improvement" (LCP ≤ 4 s, CLS ≤ 0.25, INP ≤ 500 ms) | LCP ≤ 2.5 s, INP ≤ 200 ms, CLS ≤ 0.1 at p75; above-fold data ≤ 1.5 s; no request waterfall |
| L8 | **First run, empty states, messaging restraint** | **Partial**: validator requires `emptyState` on every widget; Playwright with an empty tenant checks no widget is blank and no sample data appears; DOM check finds no modal, banner or carousel from the what's-new source, at most 1 what's-new card below zone 5, dismissible, and no Assistant prompt box. Copy quality is manual. | Fake or sample data, a blank widget, or product messaging louder than one card | Generic "No data"; or the what's-new card isn't dismissible or sits above attention or my work | Business setup checklist driven by real state; every empty zone states a reason and one action; at most one small dismissible product release-note card below zone 5, dismissal server-side |
| L9 | **Discoverability and keyboard** | **Auto**: Playwright presses Cmd/Ctrl+K, `?`, `/` and Tab through every zone; checks every item has an `href`; nav metadata has no process entries | No search reachable from the landing; zones unreachable by keyboard | Search present but no shortcut; or no shortcut list; or items not deep-linkable | Cmd/Ctrl+K command menu (records, views, actions); `?` shortcut list; J/K/Enter in lists; skip link to attention; every item has a URL; no processes as destinations |
| L10 | **Responsive and accessible** | **Partial**: Playwright at the 6 widths asserts `scrollWidth ≤ clientWidth` for the page and every widget container, widget width ≥ its size-class minimum, and content width ≤ 2400 px; axe-core zero serious or critical; target-size script ≥ 24 px. Manual: focus obscured, chart text alternatives, reading order. | Horizontal page scroll or inner horizontal scroll at any width; or any axe serious violation | Passes the widths, but has a cramped widget (< 560 px) or minor WCAG 2.2 AA gaps | Clean at all 6 widths; content ≤ 2400 px; zero axe serious or critical; manual checks pass for 2.4.11, 2.5.8, 1.4.1 and 3.2.6; charts have text alternatives |

### 7.3 App / app-group home rubric

| # | Item | CI | 0 | 1 | 2 |
|---|---|---|---|---|---|
| A1 | **Ownership, purpose and content focus** | **Auto**: validator requires an app `purpose` and ≥ 1 primary table action (not for the Admin profile), rejects a widget referenced by more than one app home, rejects processes in app sections or nav, and rejects system-role widgets on a content app home | No header or purpose; app widgets duplicated on another page; processes in nav or sections; or a system or health widget shown to business users | Header present but no purpose line or primary action | Name, one-sentence purpose, 1–2 primary *table actions*; every widget unique to this app; no processes; content app homes show business state only |
| A2 | **Attention for this app** | **Partial**: DOM order and contract in CI; whether the app's failure modes are covered is manual | No exception zone, though the app has failure modes (sends, syncs, reviews) | Exceptions exist but not first after the header, or lack actions | First zone after the header; severity-ordered; each item actionable; live; "No issues · checked HH:MM" when clear |
| A3 | **KPI strip with drill-through** | **Auto**: validator checks 3–5 `kpi` widgets, each with unit, period, comparison, `increaseIsGood` and a drill URL; Playwright clicks each tile and expects a filtered table | No KPIs, or more than 8 equally weighted tiles | 3–5 tiles, but some lack comparison, period or unit, or don't drill through | 3–5 tiles, most important top-left; each with value, unit, period, comparison, direction and as-of; each click opens the filtered records |
| A4 | **Chart choice and hierarchy** | **Partial**: validator bans gauge and 3-D types and pie charts over 5 series, and requires one `trend` widget with size `full` and a text alternative; colour consistency is manual | Pie > 5 slices, gauge, 3-D, or colour-only encoding | Suitable chart types, but no single primary trend, or inconsistent series colours | One full-width primary trend with comparison; bar or line only; consistent colours; text alternative per chart |
| A5 | **Work queues** | **Auto**: `queue` widgets reference a saved view and use size `full`; a "mine" view is required when the table declares an owner field; Playwright checks no inner horizontal scroll | Workflow app with no queues; or tables in half-width cards with inner horizontal scroll | Queues present but not saved views, or no "mine" default where ownership exists | Full-width queues backed by saved views; row → record; "View all" → the table with the same view |
| A6 | **Every table alive** | **Auto**: walks every table in the app's sections against a seeded and an empty tenant: rows, or an `emptyState` with reason and action; a sample-data detector flags lorem ipsum and "example" content | Any section table empty with no guidance, or sample data present | All tables reachable, but some empty tables show generic "No records" | Every table shows a count and last-changed time, or a guided empty state naming what fills it plus the action |
| A7 | **Scope, freshness and states** | **Auto**: changing the URL scope changes every widget request; contract checks `asOf`; fault injection per widget; refresh cadence (D14); Lighthouse budgets | Widgets use differing implicit time ranges; global spinner; silent failures | A shared scope control exists but isn't in the URL; or some widgets lack as-of or error states | One page scope with a role default, in the URL, applied to all widgets; every widget has skeleton, empty-with-reason, error-with-retry, as-of and stale indicator; budgets from 9.4 met |
| A8 | **Permissions and role layouts** | **Auto**: per-role rendered widget set against role layout metadata; row-security test on aggregates | Unpermitted widget, table or row visible, or a locked shell shown | Correct visibility, but no role emphasis (same layout for every role) | Verified for every role: hidden widgets omitted, aggregates row-secured, role-specific defaults (collapsed state, headline, scope) |
| A9 | **Power-user support without customization** | **Auto**: `#widget` and scope deep links round-trip; keyboard reachability; no add, remove, reorder, pin or hide controls and no per-user layout writes (D1) | No keyboard path or no deep links; or user layout customization present | Deep links to the app work, but not to widgets or scope | `#widget` deep links, scope in the URL, full keyboard reach, Cmd/Ctrl+K reaches this app's tables, views and actions; layout identical for all users in a role |
| A10 | **Responsive and accessible** | **Partial**: as L10 | Horizontal or inner horizontal scroll at any width, or an axe serious violation | Clean widths but a sub-560 px side-by-side widget, or minor 2.2 AA gaps | Clean at all 6 widths; content ≤ 2400 px; side-by-side only at ≥ 1440 px with ≥ 560 px each; tables full width and reflowing to cards ≤ 768 px; WCAG 2.2 AA manual checks pass |

**App group homes** use the same rubric. A3 applies to the cross-child KPI strip, and A1 also requires that no child widget is re-rendered. The **Admin home** uses the readings in 6.3.

### 7.4 CI home and gating (D17)

- **Metadata checks** (L1, L4 structure, L5 metadata, L8 `emptyState`, A1, A3, A4 structure, A5, A6 metadata) live in QQQ core as a `QInstanceValidator` plugin, plus UI API contract tests in the middleware.
- **Page checks** (L2, L3, L6, L7, L9, L10, A7, A9, A10 and the rendered parts of L5 and A8) live in qqq-frontend-next as Playwright, axe-core and Lighthouse CI jobs against seeded role users at the 6 widths.
- Both **report only for 2 releases** after they land (results in the job summary and as a PR comment), then **block merges**. An application (for example a deployment repository) can run the same suites against its own instance.
- **CI summary:** Auto: L1, L2, L3, L5, L6, L7, L9, A1, A3, A5, A6, A7, A8, A9. Partial: L4, L8, L10, A2, A4, A10. No item is manual-only. The judgement parts are the 5-second test, copy quality, colour consistency and the manual WCAG checks.

---

## 8. Cross-cutting requirements

### 8.1 KPI selection and the 5-second test

A KPI belongs on a page only if it passes **all** of these checks:

1. **Decision.** Someone in the target role would act differently if it changed.
2. **Context.** It has a comparison: the previous period, a target, or a threshold.
3. **Ownership.** It belongs to exactly one app.
4. **Drill-through.** It links to the records behind it.
5. **Honesty.** Its unit, period, as-of time and whether it is estimated are stated.
6. **Audience.** On the landing and content app homes it measures business work, not the system (P0).

**Hierarchy:** the top-left tile is the single most important number. Saturated colour is spent only on out-of-threshold states.

**The 5-second test.** Show the page for 5 seconds to 3 people in the target role, using real data, or to the auditor agent (D16). Each must answer: (a) what this page is for, (b) whether anything needs attention, and (c) where they would click first. Score the share of correct answers.

### 8.2 Attention and exceptions (D9)

- One core **attention API** collects items from any qbit or application through a provider SPI. qbit-alerts is one provider; others (for example workflow approvals, overdue tasks, failed sends, EDI rejections) register their own.
- Every item carries: source app, `audience` (`business` or `system`), record link, severity (**critical**, **warning**, **info**), first-seen and last-seen times, owner or role, an available action, and snooze or dismiss permissions.
- `business` items appear on the landing and the owning app home for the users they are routed to. `system` items appear only on the Admin home and system app homes, for system admins.
- Critical items also go out through core messaging (email or Slack, as the user chooses); the dashboard is never the only channel.
- Items clear automatically when the condition clears; stale items (past `expiresAfter`) drop off.
- The attention zone updates live over the realtime stream (D14). An empty attention zone always says when it last checked.

### 8.3 Empty, loading, error and stale states

| State | Must show | Must not |
|---|---|---|
| Loading | A skeleton shaped like the final widget (no layout shift); after 10 s, a "still loading" message and a cancel option | A page-wide spinner, or a flash of "No data" |
| Empty: never had data | Why it's empty, what fills it, and one primary action ("Import contacts", "Create your first post") | Sample or fake data |
| Empty: filtered out | "No results for *last 7 days*", plus "Clear filters" | The first-run copy |
| Empty: not configured | What setup is needed and who can do it ("Ask an admin" if the user can't) | An action the user isn't allowed to run, or a system health readout |
| Error | A plain-language message, a correlation id, and Retry | A stack trace, or a silently empty widget |
| Stale | An "Updated 3 h ago" warning and a Refresh control | Hiding the age |

### 8.4 Accessibility (WCAG 2.2 AA)

- Landmarks: one `h1`, one `h2` per zone, and widgets as `section` elements with accessible names.
- Every chart has a text alternative: a summary sentence plus a data table or "View as table".
- Status uses icon plus text plus colour, never colour alone (1.4.1). Text contrast at least 4.5:1; chart marks and focus rings at least 3:1.
- Keyboard: every zone and widget can be reached. A skip link goes to "Needs attention". Focus is never hidden behind sticky headers or toasts (2.4.11).
- Targets at least 24×24 px (2.5.8). No drag-to-reorder (D1); any other drag interaction has a single-pointer alternative (2.5.7).
- Live updates in the attention zone use a polite live region, rate-limited.
- `prefers-reduced-motion` turns off animated counters and skeleton shimmer.

### 8.5 Responsive layout

| Width | Layout |
|---|---|
| 390 | One column. Header, attention, my work, continue, apps. Headlines 2-up *only* if each tile is at least 160 px and holds a single number; otherwise one per row. Tables reflow to stacked cards. |
| 768 | One column. KPI tiles may sit 2-up. Lists and tables stay full width. |
| 1024 | One main column. KPI strip 3–5 across. No side-by-side lists. |
| 1440 | KPI strip on one row. Two supporting widgets may sit side by side if each is at least 560 px. Tables full width. |
| 1920 | As 1440, with up to 3 supporting widgets per row (each at least 560 px). |
| 3200 | Content is centred with a **maximum width of 2400 px** (D11). Tables and the primary trend use the full content width. Tiles stop growing at about 480 px and more fit per row. Body text is limited to about 80 characters per line. |

At every width: no horizontal page scroll and no inner horizontal scroll (stricter than the WCAG table exception). Audits include screenshots at all six widths.

### 8.6 Layout and personal content contract (D1, D5, D7)

| Layer | Who decides | What | Where it's defined |
|---|---|---|---|
| Curated default | Product (qbit and app metadata) | Zones, widgets, roles, priorities, size classes | App and widget metadata |
| Role layout | Product, per role from qbit-user-role-permissions (D10). No tenant override in v1 (D7). | Which headline tiles and optional widgets each role sees, which start collapsed, default page scope | Role layout metadata |
| Personal *content* | System, by rule | My work, recents, attention routed to me | Derived from data and ownership fields |
| Favorites | User | Records and saved views bookmarked by the user (D5) | Per-user data via the UI API |
| What's-new dismissal | User | Hide the current card | Per-user read state via the UI API |
| Remembered widget filters | **Not allowed** (D6) | Scope is in the URL | — |
| User layout customization | **Not allowed** | No add, remove, reorder, pin, hide, persisted collapse or density preference | — |
| Inferred personalization | **Not allowed** | No "Suggested for you" or ML ranking | — |

Because every user in a role sees the same layout, layouts can be tested, screenshotted and supported per role, and the rubric can run in CI per role.

### 8.7 Role-based content

- Visibility is permission-driven (`permissionRules`); emphasis is role-driven (role layout). Roles are the ones defined through qbit-user-role-permissions (D10); this standard adds no role list.
- Applications should define at least: a system-admin role (sees Admin), and one or more business roles per content app. The rubric is scored for each role that can see the page.
- A user with no permitted apps sees a friendly empty landing: "You don't have access to any apps yet. Ask {admin name}." Never a blank page.

### 8.8 Discoverability and power users

- A global search and command menu (Cmd/Ctrl+K) searches records, tables, saved views and *actions*, on every page. `/` focuses search and `?` lists shortcuts. G-chords (`g d` for Dashboard) are optional.
- Lists use J/K to move, Enter to open, E to dismiss and H to snooze in attention and My work.
- Density is fixed by the design system for each size class. No per-user density toggle (D1).
- Deep links: every widget is addressable (`/app/{app}#{widget}`), and the time range and filters are in the URL.
- Suggested actions come only from real state ("3 drafts haven't been published"), never generic tips.
- The Assistant is reached from the header Ask button (D15).

---

## 9. What the UI API and widget metadata need

Status key: **Exists** means present in QQQ develop on 2026-10-16. **Partial** means something close exists. **New** means not found. **Verify** means likely but not confirmed.

| # | Capability | Purpose | Status | Notes |
|---|---|---|---|---|
| 1 | `permissionRules` on widget and app; `hasPermission` in metadata | L-5 | **Exists** | `QWidgetMetaData.permissionRules`; `QWidgetData.hasPermission`. Row-secured aggregates: **Verify**. |
| 2 | `homeAppName` (instance landing redirect) | Landing routing | **Exists** | Kept as opt-in (D13). |
| 3 | `app.widgets`, `gridColumns`, `isCard`, `collapsible` | Layout | **Exists** | `gridColumns` is in twelfths and does not encode a minimum readable width (see row 8). |
| 4 | `helpContent` on widget and app | Inline help | **Exists** | |
| 5 | `storeDropdownSelections`, `showReloadButton` | Refresh; dropdown memory | **Exists** | Home widgets default `storeDropdownSelections=false` (D6); the validator flags it on home widgets. |
| 6 | `widget.role` enum: `attention`, `myWork`, `recent`, `favorites`, `headline`, `kpi`, `trend`, `queue`, `breakdown`, `sections`, `setup`, `help`, plus system roles `systemAttention`, `systemHealth` | Zone order, landing contribution, content-first checks, headless rendering | **New** | The core of this standard. System roles are only valid on the Admin profile. |
| 7 | `widget.priority` (int) and `aboveFold` hint | Fetch order, layout | **New** | Today the order is list position only. |
| 8 | `widget.sizeClass` (`tile`, `standard` = 560 px min, `full`) | A-8, 8.5 | **New** | Replaces guessing from `gridColumns`. |
| 9 | `landingHeadline` flag on one KPI per app | Landing zone 6, L-4, D8 | **New** | The landing reads the *same* widget output. |
| 10 | Widget data envelope: `asOf`, `staleAfter`, `source`, `isEstimate`, `refreshIntervalSeconds` (default 60, D14) | L-6 | **New** | `QWidgetData` has no timestamp today. |
| 11 | `emptyState { reason: noData, filtered, notConfigured, noPermission; title; body; primaryAction (table action ref); docsLink; whoCanFix }` | 8.3 | **Partial** | Next UI has an `EmptyState` component and `helpContent` exists, but there is no metadata contract. |
| 12 | Widget error envelope `{ code, message, retryable, correlationId }` | L-7 | **Verify** | |
| 13 | KPI semantics: value, unit, period label, comparison value and label, target, `increaseIsGood`, drill URL | 8.1 | **Partial** | `StatisticsData` has `count`, `countURL`, `countContext`, `percentageAmount`, `percentageLabel`, `increaseIsGood`, `isCurrency`. Missing: unit, period, target, `asOf`. |
| 14 | Page scope contract: time range and filter variables shared by all widgets on an app home, serialized to the URL, with a role default | A-5, A-9 | **New** | |
| 15 | **Attention API**: provider SPI in core; `/qqq/v1` endpoint returning user-scoped items with `audience`, `severity`, `firstSeen`, `lastSeen`, `expiresAfter`, record link, action ref, snooze and dismiss; live updates over the realtime stream | Landing zone 3, app zone 2, Admin zone 2, D9, D14 | **New** | qbit-alerts becomes one provider. `RealtimeStreamSpecV1` exists for live updates. |
| 16 | Ownership semantic on tables: `ownerField` / `assigneeField` / `awaitingUserField` | My work | **Partial** | qbit-collaboration has assignments and My work services; a generic table-level declaration in core: **Verify**. |
| 17 | Server-side **recents** and **favorites** per user and tenant through the UI API | Landing zone 5, D5 | **Partial** | qbit-collaboration has favorites and recents services and tables; Next UI recents still come from `localStorage`. |
| 18 | Saved views (incl. a "Mine" view) | Drill-through, queues | **Exists** | `SavedView`, `SharedSavedView`, `QuickSavedView`. |
| 19 | **What's new** feed: latest product release note, `publishedAt`, `expiresAt`, per-user dismissal (D12) | Landing zone 8 | **Partial / Verify** | qbit-release-notes exists; confirm per-user dismissal through the UI API. |
| 20 | **Setup checklist** contract: steps with `isComplete` computed from data, `action`, `role`, `audience` | Landing zone 2 | **New** | |
| 21 | **Role layout** metadata keyed by roles from qbit-user-role-permissions: widgets shown, collapsed state, headline choice, default scope. No per-user or per-tenant layout record (D1, D7). | 8.6, 8.7 | **New** | |
| 22 | Command and action registry via UI API (searchable table and record actions) | 8.8 | **Verify** | Next UI has a command menu; qbit-quick-search exists. |
| 23 | Headless text form for every widget role (label, value, `asOf`, link) | Native client parity | **Partial** | `csvData` exists on `QWidgetData`. |
| 24 | Per-widget timing (`Server-Timing` or a field) | 9.4 budgets | **New** | |
| 25 | User time zone and first name in session | Greeting, timestamps | **Verify** | |
| 26 | Tenant context and switcher in session metadata | Header | **Verify** | |
| 27 | App `purpose` text and `audience` (`content` or `system`) | A1, content-first checks, Admin profile | **New** | `audience=system` places an app under Admin and enables the Admin profile. |

### 9.4 Performance budgets

Measured at p75 on the real deployment.

| Metric | Budget |
|---|---|
| LCP (landing and app home) | ≤ 2.5 s |
| INP | ≤ 200 ms |
| CLS | ≤ 0.1 (skeletons reserve the space) |
| Header and zone skeletons painted | ≤ 1.0 s |
| Above-the-fold widget data (zones 1–3) | ≤ 1.5 s |
| Per-widget server render | p95 ≤ 500 ms. At 5 s: time out, show the error state, offer retry. |
| UI API calls before first meaningful paint | 1 metadata call plus parallel widget calls in priority order. No waterfall on a below-the-fold widget. |
| JS bundle | Report-only; record the change per release. |
