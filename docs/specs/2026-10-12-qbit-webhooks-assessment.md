# qbit-webhooks assessment: adopt, enhance or rewrite

**Date:** 2026-10-12. **Status:** assessment only. No code was changed and no repository was touched.
**Question (owner):** is the existing webhooks qbit good enough to add and use for our webhooks feature, or does it need enhancing, or a new version altogether?

**Reviewed:** `QRun-IO/qbit-webhooks` at `develop` commit `58c5f91` (version `0.4.0-SNAPSHOT`; last release `0.3.0` on Maven Central, 2025-12-28). Every statement about the code below was checked against that source, not against the README.

Related: [Alerts qbit design (#1090)](./2026-10-12-qbit-alerts-design.md), [CDN qbit design (#1084)](./2026-10-12-qbit-cdn-design.md), [QQQ ESB design](./2026-09-25-qqq-esb-design.md), `docs/QRUN-QBIT-REPO-STANDARD.md`.

## 1. Verdict

**Enhance it, as a 1.0 release that is allowed to break compatibility. Do not adopt it as-is, and do not rewrite it.**

- **Not as-is.** The delivery engine is good, but it has no signing, no secret handling, no SSRF protection, no event id or idempotency key, no delete events, no ESB integration, and it breaks the owner's table-naming and app-layout rules. A webhook sender that sends unsigned POSTs to any URL, including `http://169.254.169.254/`, is not safe to put in front of real subscribers.
- **Not a rewrite.** The hard, easy-to-get-wrong parts exist and are tested: a transactional outbox (events are written in the same transaction as the source record), a per-webhook scheduled sender, retry with backoff, a health state machine (healthy, unhealthy, probation), rate-limit handling, an event type registry, API-versioned payloads, record-security-aware matching, and the standard review-and-result process flow with redeliver. The 32 tests pass against QQQ 4.1.0-RC.4. A rewrite would rebuild the same skeleton and cost more (roughly 45 to 55 working days against roughly 35 for the enhancement).
- **Breaking changes are cheap now.** No production consumer exists (section 8). The only dependents are the `qqq-all` demo app and, in documentation, two other qbit READMEs. After this window, every rename becomes a migration.

**The README is not evidence.** `README.md` documents `WebhooksQBit`, `QWebhookMetaData`, `X-Webhook-Signature`, a `webhook_log` table, `withMaxRetries(...)` and "Stable and production-ready". None of these exist in the source. `CLAUDE.md` in the repo already warns about this. The actual entry points are `WebhooksQBitProducer`, `WebhooksRegistry` and `WebhookEventType`.

**One design-spec correction is needed now.** The Alerts design (the channel table) says the webhook channel uses "HMAC-SHA256 signature header with a timestamp (qbit-webhooks' signing scheme)". **qbit-webhooks has no signing scheme.** The HEALTHY / PROBATION / UNHEALTHY health model it also cites is real. Alerts should define the signing format itself (section 7 proposes one) or depend on the shared primitive proposed in section 7.

## 2. The need (assumptions)

Inferred from the owner's request and the other qbit designs. Each is an assumption to confirm.

1. Outbound webhooks from QQQ apps (the first host, Voyage, future apps), configured by admins in the QQQ UI, with no code deploy to add a subscriber.
2. Event sources: (a) table events, including from the ESB (Artemis topics such as `<app>.cms.tables`); (b) process events; (c) custom domain events ("post published"); (d) alerts, through the Alerts qbit's webhook channel.
3. Signed, timestamped delivery with rotating secrets. Secrets stored as references, never as plaintext in a table.
4. At-least-once delivery with retries, backoff, dead-lettering, an idempotency key and a documented ordering contract.
5. Delivery logs, redeliver, test send, endpoint health with automatic pause, per-endpoint rate limits and timeouts.
6. Payload shaping (include or exclude fields), subscription filters, per-subscription permissions.
7. Inbound webhooks (verifying signed third-party callbacks) are in scope only as a later phase.
8. The qbit must follow the owner's qbit rules (`docs/QRUN-QBIT-REPO-STANDARD.md` B10, plus the table, nav, process, dashboard and API rules).

## 3. What the qbit is

A QBit (`com.kingsrook.qbits:qbit-webhooks`, Java 21, parent `qbit-build-parent:2.0.0`) with about 7,700 lines of main code (40 files, most of it getters, setters and headers) and 3,900 lines of tests (19 files, 32 tests).

**Data model (5 tables, all standard visible tables):** `webhook` (name, url, active status, health status), `webhookSubscription` (webhook, event type name, active status, API name and version; unique on event type plus webhook), `webhookEvent` (the outbox row: status NEW, SENDING, DELIVERED, AWAITING_RETRY, FAILED; next attempt time; source table and record id), `webhookEventContent` (the stored JSON post body, one per event), `webhookEventSendLog` (one row per attempt: http status, truncated error, start and end time).

**Flow.**
1. A host registers `WebhookEventType`s in `WebhooksRegistry`. Categories: insert, update, store (insert or update), each also "with field" and "with specific value", plus ad hoc.
2. The qbit registers instance-level `POST_INSERT_RECORD` and `POST_UPDATE_RECORD` customizers. On a match it builds one `webhookEvent` plus content per matching subscription, **inside the caller's transaction** (a transactional outbox).
3. Adding or activating a `webhook` creates a scheduled job (every 60 s by default) that runs the `SendWebhookEvent` process for that webhook: extract NEW events and those due for retry, transform (checks webhook and subscription are active and the webhook is not unhealthy), load (POST).
4. The sender POSTs, records a send log, and moves the event to DELIVERED, AWAITING_RETRY (backoff 1, 5, 15, 60, 240 minutes) or FAILED (5 attempts). A 429 sleeps and retries in-process (3 times, doubling).
5. `WebhookHealthManager` marks a webhook UNHEALTHY after 10 failures in a row, which stops sending. A table process `ManageWebhookHealth` moves long-unhealthy webhooks to PROBATION (one retry; success returns to healthy). Test send is a record action.
6. Payloads are `{ webhookEventDetails: {...}, record: {...} }`, with the record rendered through the QQQ API middleware at the subscription's API name and version (stable, versioned field names).

## 4. Capability matrix

Legend: **Has** (works as needed), **Partial** (exists, with a gap), **Missing**.

| Capability | Status | Evidence and gap |
|---|---|---|
| Table events: insert, update, store | **Has** | `FirePostInsertWebhookEvent`, `FirePostUpdateWebhookEvent`; with-field and with-value variants |
| Table events: delete | **Missing** | README promises it; no delete category. Instance-level customizers do not fire in `DeleteAction` (the 2026-10-04 audit records this), so it needs a core hook or a per-table `MultiCustomizer` |
| Table events from the ESB (Artemis topics) | **Missing** | No ESB or `EsbTrigger` code at all; sources are only the in-process customizers |
| Process events | **Missing** | Only table and ad hoc events |
| Custom domain events | **Has** | `FireAdHocWebhookEvent.fire(eventTypeName, tableName, keyRecord, transaction)` |
| Alerts channel | **Missing** | No notion of a non-table sender; the Alerts channel has to be built (section 7) |
| Signed delivery (HMAC, timestamp) | **Missing** | The only header set is `Content-Type`. No secret field exists on `webhook` |
| Rotating secrets, secret references | **Missing** | Nothing to rotate |
| Retries with backoff | **Has** | Configurable schedule `[1,5,15,60,240]` min, max attempts, 429 handling |
| Dead-lettering | **Partial** | `FAILED` is the terminal state and is visible and redeliverable; no separate DLQ, no alert on it |
| Idempotency key | **Missing** | The event id is not in the body or a header. A retry after a lost response looks like a new event to the receiver |
| Ordering | **Partial** | Events are extracted by id ascending per webhook, but a failing event backs off while later ones go through, so order is not preserved. Not documented |
| Delivery log | **Has** | `webhookEventSendLog`, but the error is truncated to 100 chars and there is no duration, response excerpt or request id |
| Replay and redeliver | **Has** | Same process, flags "already delivered, will be sent again" |
| Endpoint health, auto-pause | **Has** | HEALTHY / UNHEALTHY / PROBATION, thresholds configurable. Pause, not disable; no alert raised |
| Test send | **Has** | `SendTestEventToWebhook` record action |
| Per-endpoint rate limit | **Missing** | Only reacts to a receiver's 429. No outbound cap |
| Timeouts | **Partial** | Fixed 5 s in `buildHttpClient()`. The `httpTimeoutMillis` config property exists but is never read |
| Payload shaping | **Partial** | Per-event-type customizer hook (code only). No per-subscription field include or exclude from the UI |
| Subscription filters | **Missing** | A commented-out `queryFilterJson` todo. Only with-field and with-value types |
| Permissions per subscription | **Partial** | Record security locks and security fields copy to events and gate matching; table permissions use the generic table names |
| Retention | **Missing** | Events, bodies and logs grow without a purge |
| Inbound webhooks | **Missing** | The README tagline says "inbound and outbound"; only outbound exists |
| Multi-replica safety | **Partial** | See section 5 |

**Summary:** 7 Has, 6 Partial, 11 Missing (24 rows). The Has column is the delivery engine. The Missing column is the security and integration surface.

## 5. Architecture

**QQQ 4.1 compatibility: good.** The unmodified source builds and passes all 32 tests with `-Pqqq-snapshot -Dqqq.snapshot.version=4.1.0-RC.4` (Java 21, `./mvnw verify`). The default build still follows the parent (QQQ 4.0.0). The standard (B5) wants the 4.1 profile active by default; here it is opt-in.

**Durable queueing: good.** Delivery is queued in the database (a table as the queue) and written in the same transaction as the source change, so an event is never lost to a crash between commit and send and never describes a rolled-back write. That is better than the in-process queue the Alerts spec uses for signals. Cost: delivery latency is the scheduler interval (60 s default), and the table is polled once per active webhook.

**ESB integration: none.** It hooks table writes in-process only. It does not consume ESB topics, so any write made by another service is invisible to it, and a publisher that already emits CloudEvents (the ESB format, with a stable `id`) cannot be fanned out to subscribers. Section 7 describes the adapter.

**Multi-replica safety: unverified, probably unsafe under load.** The send step reads due events, then marks each SENDING with a "leaked after 10 minutes" timestamp. The mark is a plain update after the extract, not an atomic claim, so two replicas running the same webhook's job at once can both pick up an event. QQQ's scheduler normally runs a given scheduled job on one node, which is the only protection, and I did not confirm it for every scheduler type. Needs a conditional-update claim and a test with two runners.

**Other architecture notes.**
- Several settings are cached in static fields on first read (`WebhookEventSender`, memoized subscription lookups), so reconfiguring or running two instances in one JVM (tests) shares stale values. Subscription changes can take up to 5 minutes to be seen on other replicas (per-replica memoization, cleared only locally).
- A new HTTP client is built per send, with no pooling (the code carries a "needs scrutiny" comment).
- The 429 handler sleeps on the sending thread (up to about 7 s per event), blocking the webhook's whole batch.
- The producer hard-codes the QBit version string `0.1.4` and artifact id `webhooks`.
- It depends on `qqq-middleware-api` for payload versioning. That is a real strength for stable external contracts, and a heavier dependency than a bare webhook sender needs.

## 6. Security

| Area | Finding | Severity |
|---|---|---|
| Signing | None. A receiver cannot authenticate the sender or detect tampering or replay. README describes `X-Webhook-Signature`; it is not implemented | **Critical** |
| Secret storage | There is no secret at all. When added it must be a reference (`secretRef`, resolved at send time), per the Alerts spec rule | **Critical** (gap) |
| SSRF | The URL is `POST`ed as given, with no scheme check, no private, loopback, link-local or metadata (`169.254.169.254`) blocking, no DNS pinning (rebinding), and no limit on response size. Anyone who can create a `webhook` row can probe the internal network, and error text is stored in the send log (a blind-to-visible oracle). The Apache client default does not follow a redirected POST for 301 and 302, but 307 and 308 behavior and future changes are not pinned by any test | **High** |
| TLS | The default `HttpClientBuilder` verifies certificates and hostnames. No pinning, no minimum protocol setting | Acceptable |
| PII exposure | The body is the full API view of the record, to every subscriber, with no per-subscription field filter. The view obeys API field visibility, so exposure equals the API's | **Medium** |
| Log exposure | Error text is truncated to 100 chars and the response body is not stored. The URL and webhook id are logged at warn. No secret is logged (there is none) | Low |
| Permissions | Generic table names (`webhook`, ...), so a host's permission names are `webhook.*`, not `webhooks.*` | Medium (standards) |
| Inbound | Not implemented, so no replay-window or constant-time compare code exists to review | n/a |

Internal receivers are a real need (the CDN design's revalidation hooks call every Next pod over the cluster network). The SSRF policy therefore needs a deliberate, admin-only "allow internal targets" setting with an explicit allowlist, not a blanket ban.

## 7. Fit against the owner's standards

| Rule | Result | Detail |
|---|---|---|
| Data in standard visible tables | **Pass** | Five standard tables, including the stored body (`webhookEventContent`) and every attempt |
| Table names lowercase snake_case with the full qbit prefix; permissions use the full name | **Fail** | `webhook`, `webhookSubscription`, `webhookEvent`, `webhookEventContent`, `webhookEventSendLog` (camelCase, no `webhooks_` prefix) |
| Processes are table or record actions with the streamed-ETL review and result flow; none in the nav; no custom dry-run forms | **Pass** | `SendWebhookEvent`, `ManageWebhookHealth`, `SendTestEventToWebhook` are `StreamedETLWithFrontendProcess` bound to a table. Test send adds one small input form field (a checkbox), acceptable |
| A real app home dashboard | **Fail** | No app home or widgets; the app section just lists the tables |
| Nav shows only main tables, sub-tables as relations | **Fail** | `produceAppSection()` lists all five tables, including events, content and send log. Webhook has child relation widgets, but the section still exposes them |
| API that matches the admin | **Partial** | Subscriptions carry an API name and version, but there is no API surface for webhooks management and no test of parity |
| ESB and Quick Search on the main tables | **Fail** | None |
| Readable UI | **Partial** | Chips and a JSON code editor for the body are good; nothing was checked at the widths the owner uses |
| Repo standard (files) | **Partial** | See below |
| Repo standard (build) | **Partial** | See below |

**Repo standard checklist (against `QRUN-QBIT-REPO-STANDARD.md`).**
- **F1/F2 licence files:** LICENSE and NOTICE are Apache-2.0, but `pom.xml` `<licenses>`, source headers and `checkstyle/license.txt` still say AGPL-3.0 and the README footer says "Proprietary". Open PR #5 (`feature/GH-10-public-stack-prep`) aligns them and is unmerged.
- **F3 README:** wrong (describes an API that does not exist). **F5 CHANGELOG:** wrong (a copy of `qbit-user-role-permissions`, "QBit User Role Permissions"). **F6:** `CLAUDE.md` exists, no `AGENTS.md`. **F8 dependabot.yml, F11 .gitattributes, F12 .editorconfig, F13 Maven wrapper, F15 docs/DESIGN.md:** all absent.
- **B3:** `url` and `scm` point at `Kingsrook/qbit-webhooks`, not `QRun-IO`. **B4 Java 21:** yes (via parent). **B5:** profile opt-in, not default. **B6 JaCoCo:** absent, so no coverage number or gate. **B9:** package and producer pattern match.
- **C1 CircleCI:** present but on `qqq-orb@0.5.0` (standard is `0.6.8`; PR #5 has the bump). **G1 default branch `develop`:** yes. **G2:** `main` is not protected; `develop` requires a PR and signed commits, with no required check named `test_only` (the standard P2 asks for it).
- **S3:** issues are **disabled** (the standard wants them on), wiki is off (correct). No GitHub Release after `v0.3.0`.

## 8. Quality

- **Tests:** 32 test methods, all passing on 4.1.0-RC.4. Coverage is not measured (no JaCoCo). The sender (880 test lines for 418 source lines) and event firing are well covered; there is no test of replica claiming, redirects, large responses, or concurrency.
- **CI health:** a CircleCI config exists; the 2026-09 re-pin to the parent and the orb bump are merged or in PR #5. No required status check on `develop`.
- **Release history:** `0.2.0`, `0.2.1`, `0.2.2`, `0.3.0` on Maven Central (the last on 2025-12-28), plus a stray `v0.2.1` version string. The next release is blocked on PR #5.
- **Open work:** PR #5 (licence and CI alignment) and a Dependabot PR #3 (assertj bump). Issues are disabled, so there is no issue backlog.
- **Code health:** consistent Kingsrook style, small focused classes, good comments. The weak points are the static config caches, the hard-coded version string, and the unused `httpTimeoutMillis`.
- **Docs:** misleading (section 1). A knowledge-base dossier exists privately; the repo has no `docs/` folder.
- **Adoption:** 0 stars. Dependents found: `qqq-all` (aggregate BOM and demo app, `DemoQBits` registers an `orderStored` event type), and README mentions in `qbit-crm` and `qbit-wms`. No use found in the first host, Voyage or the website backend. Maven Central download counts were not available.

## 9. Fit with the other planned qbits

| Qbit | Should it use qbit-webhooks? | Reasoning |
|---|---|---|
| **Alerts (#1090)** | **Share the primitives, not the tables.** Optional dependency for the generic webhook channel, with the signing format defined once | Alerts owns its own notification table, retry ladder (1 min to 4 h, then DEAD) and channel health. Putting a second outbox behind it would duplicate the queue. What it needs is the signer, the SSRF-safe sender and the verification doc. If qbit-webhooks is installed, a "delegate" mode (create an event for a subscriber) is a nice option, as the Alerts spec already allows |
| **CDN (#1084)** | **Share the primitives only** | `cdn_revalidation_hook` is a fan-out to every pod, must finish before an edge invalidation, and targets internal addresses. That is a synchronous dependency step, not a subscription. It needs the signer and a trusted-internal mode of the safe client, not the event outbox |
| **Marketing** | **No for now** | Its transports (`MarketingTransport`, email through QQQ messaging) are message sends owned by the campaign engine. A webhook transport could be added later through the same primitives |
| **Authentik (#1065)** | **Inbound only (later)** | Authentik sends webhooks to QQQ; inbound verification is the relevant piece |

Recommendation: extract the two reusable pieces so no qbit re-implements them:
1. `WebhookSigner` and `WebhookVerifier` (HMAC-SHA256 over `id.timestamp.body`, 5 minute tolerance, constant-time compare, two active secrets) and
2. `SafeHttpClient` (URL policy, DNS pinning, no redirects, size and time caps).

Place them in `qqq-backend-core` utilities (4.1.x), so Alerts and CDN, which depend only on core, can use them without taking on qbit-webhooks. qbit-webhooks becomes their first and fullest consumer. If core is not the right home, a tiny `qbit-webhooks-spi` artifact works the same way.

**Proposed signing format** (modelled on the Standard Webhooks and Stripe conventions): headers `webhook-id` (the event id, also the idempotency key), `webhook-timestamp` (Unix seconds), `webhook-signature` (`v1,<base64 HMAC-SHA256 of "<id>.<timestamp>.<body>">`, space-separated when two secrets are active during rotation). Also `User-Agent`, and `webhook-attempt`.

## 10. Recommended design for the enhancement (qbit-webhooks 1.0)

**Keep:** the outbox tables and flow, per-webhook scheduled sender, health model, registry, ad hoc events, API-versioned payloads, record-security matching, the three table and record actions.

**Change:**
1. **Naming.** Tables `webhooks_endpoint`, `webhooks_subscription`, `webhooks_event`, `webhooks_event_content`, `webhooks_delivery` (backend name equals table name); permission names `webhooks_endpoint.*`. App `webhooks`. Fix the version string.
2. **Secrets.** `webhooks_endpoint.secretRef` (a name resolved through `QMetaDataVariableInterpreter`) plus an optional `previousSecretRef` for rotation. A secret value is never stored, logged or returned. A "Rotate secret" record action and an "expires" date.
3. **Delivery.** Signed with the section 9 format. Add the stable event id (UUID, unique per subscription and source event) to body and headers. Honor `httpTimeoutMillis`. Reuse one pooled, SSRF-safe client. Do not sleep in the batch on 429; reschedule with `Retry-After`. Atomic claim of events (conditional update to SENDING with a lease) so replicas cannot double-send. Document at-least-once, no global ordering, and an optional per-endpoint "serial" mode (one in flight).
4. **Sources.** (a) keep the in-process table customizers and add delete; (b) an **ESB adapter**: a process with an `EsbTrigger` on a topic such as `<app>.cms.tables`, mapping the CloudEvent `type` to an event type and using the CloudEvent `id` as the dedup key, with `qqq.process.<name>.completed|failed` as process events; (c) ad hoc events unchanged; (d) a small `WebhookTarget` SPI so other qbits can enqueue a payload for a named endpoint.
5. **Subscriptions.** Add a filter (`QQueryFilter` JSON), and per-subscription field include and exclude lists applied on top of the API view (PII control). Add a `detailLevel`.
6. **Operations.** Health pause plus an optional auto-disable after N probation failures, with an alert signal when Alerts is installed (`webhooks.endpoint.unhealthy`). Per-endpoint rate limit (events per minute). Retention process (purge delivered events, content and logs older than N days). Store duration, a response excerpt and the attempt number on each delivery row.
7. **UI and standards.** Real app home (delivery health, failing endpoints, last 24 h counts, links, no duplicated tables). Nav shows `webhooks_endpoint` and `webhooks_event`; subscriptions, content and deliveries are relations. ESB publications and Quick Search on the main tables. API parity check. JaCoCo gate (70% instructions, 90% classes). Standard repo files, default 4.1 profile, accurate README and `docs/DESIGN.md`.
8. **Inbound (phase 2).** A route provider and a `webhooks_inbound_source` table (name, `secretRef`, verification scheme: this signing format, GitHub `X-Hub-Signature-256`, Stripe `Stripe-Signature`, shared token), a received-events table with replay-window and duplicate rejection by id, and a hand-off to an ESB topic or a process. Modeled on the existing route-provider pattern the Authentik design cites.

## 11. Prioritized gap list and effort

Estimates are working days for one engineer who knows the codebase.

| # | Gap | Severity | Effort |
|---|---|---|---|
| 1 | Signing, secret references, rotation | Critical | 3 |
| 2 | SSRF-safe client (scheme, private ranges, DNS pinning, redirects, size, timeouts, internal allowlist) | High | 4 |
| 3 | Rename tables and permissions to `webhooks_*` snake_case, migration note | Standards, breaking | 3 |
| 4 | Event id and idempotency header, dedup key, attempt header | High | 2 |
| 5 | ESB source adapter and process events | High (needed for the first host's ESB topics) | 6 |
| 6 | Delete events (core hook or `MultiCustomizer` composition) | Medium | 2 |
| 7 | Atomic claim and lease, config caching fix, two-runner test | High (correctness) | 3 |
| 8 | Subscription filters, field include and exclude, detail level | Medium | 5 |
| 9 | App home dashboard, nav cleanup, API parity, ESB publications, Quick Search | Standards | 5 |
| 10 | Rate limit, 429 handling off the thread, health to alert, auto-disable | Medium | 3 |
| 11 | Retention process, richer delivery log | Medium | 2 |
| 12 | Repo standard (files, `AGENTS.md`, JaCoCo, mvnw, default 4.1 profile, README and CHANGELOG rewrite, protect `main`, enable issues, close PR #5 by merging) | Standards | 3 |
| 13 | Shared signer and safe client extraction (core or SPI artifact) and Alerts and CDN adoption | Cross-qbit | 3 |
| | **Outbound 1.0 total** | | **about 40** |
| 14 | Inbound webhooks (phase 2) | Feature | 8 |

Order: 1, 2, 4, 3 first (one release, `1.0.0-RC.1`, usable by the first host's CMS events through the in-process path), then 7, 5, 6, 8, 9, 10, 11, 12 (RC.2 and GA), then 13 and 14.

## 12. Breaking-change assessment

| Change | Who is affected | Mitigation |
|---|---|---|
| Table and permission renames | Any host that installed 0.x and has rows | Only `qqq-all` demo is known. A one-time SQL rename script in `docs/migration`. Pre-1.0 versions say breaking changes are allowed |
| Payload gains an `id` and headers | Receivers | Additive; envelope field names unchanged |
| Signing on | Receivers | Existing endpoints have no secret, so unsigned stays valid until `secretRef` is set; add a one-release warning |
| SSRF policy on by default | Subscribers on private addresses | Admin-only internal allowlist setting; documented |
| Config class changes (`withHttpTimeoutMillis` now read, new properties) | Host configs | Defaults preserve current behavior |
| Maven coordinates unchanged | None | Version moves `0.3.0` to `1.0.0` |

Known consumers: `qqq-all` (aggregate BOM and demo). No dependency found in the first host, Voyage or the website backend. Because the README documents an API that never existed, nobody can have integrated by following it.

## 13. Risks

1. Unsigned, unrestricted sending is the headline risk. Do not enable the qbit in any app, even internally, before gap 1 and 2.
2. Double-sending under several replicas (gap 7) is unverified, so it is a risk until a test proves it.
3. The Alerts spec's claim about qbit-webhooks' signing scheme is wrong and could spread to the CDN and marketing work.
4. The ESB adapter depends on `EsbTrigger` semantics (retry and dead-letter belong to the trigger); dedup by CloudEvent id is required because broker redelivery is at-least-once.
5. Dependence on `qqq-middleware-api` ties the payload shape to API versioning. This is a benefit for external contracts, but a host without the API module cannot use the qbit.

## 14. Decisions for the owner

| # | Decision | Recommendation |
|---|---|---|
| D1 | Adopt, enhance or rewrite | **Enhance as 1.0** |
| D2 | Where the shared signer and safe client live | **`qqq-backend-core` utilities** (4.1.x), so Alerts and CDN do not depend on a qbit |
| D3 | Break table names now | **Yes**, rename to `webhooks_*` before any real data exists |
| D4 | Internal-network targets | **Off by default**; admin-only allowlist per endpoint, needed by the CDN revalidation hooks |
| D5 | Inbound webhooks in 1.0 | **Phase 2**, after outbound GA, in the same qbit |
| D6 | Correct the Alerts spec (#1090) | **Yes**: replace the sentence about "qbit-webhooks' signing scheme" with a link to section 9 |
| D7 | Merge PR #5 (licence and CI alignment) | **Yes**, first, so the 1.0 work starts from a consistent repo |
| D8 | Event delivery order | **At-least-once, per-endpoint ordering only on request** (serial mode), documented |

## 15. Method and limits

Read the main source (all the behavior classes, the models, the processes and the config) and the tests in outline; built the repo unmodified with Java 21 against QQQ 4.1.0-RC.4 (32 tests, all passing; a scratch copy, nothing committed); inspected GitHub settings, releases, PRs and branch protection with `gh`; searched `QRun-IO` and `KofTwentyTwo` code and the local clones for dependents; read the Alerts, CDN and ESB designs and the repo standard. Not done: no runtime test of multi-replica behavior, no load test, no UI screenshots, no review of Maven Central download statistics. Items marked "unverified" are inferences from the code.
