# QQQ Alerts qbit (Manage by Exception): Design Spec

Status: **Approved** (owner, 2026-10-12) · drafted 2026-10-12 · Author: James Maes (drafted with Claude)
Targets: QQQ 4.1.x (a small producer SPI in `qqq-backend-core`), QQQ Next UI 1.0.x (a bell and a record panel), and a proposed new repo, **`QRun-IO/qbit-alerts`** (Apache-2.0).
Related specs, all of which plug into this one (§9):
- web performance [#1088](https://github.com/QRun-IO/qqq/pull/1088)
- CDN qbit [#1084](https://github.com/QRun-IO/qqq/pull/1084), §4.9.1
- object storage qbit [#1089](https://github.com/QRun-IO/qqq/pull/1089), §9.3
- backups qbit (`docs/specs/2026-10-12-qbit-backups-design.md`, still being drafted when this was written)
- CMS qbit [#1083](https://github.com/QRun-IO/qqq/pull/1083)
- Authentik qbit [#1065](https://github.com/QRun-IO/qqq/pull/1065)
- QQQ Assistant [#1054](./2026-10-11-qqq-assistant-qbit-design.md)
- ESB ([`2026-09-25-qqq-esb-design.md`](./2026-09-25-qqq-esb-design.md))

> **Owner decisions (2026-10-12): all recommendations accepted.** Every question in §13 is decided as recommended ("they are approved. we will adjust as needed once we can view them working"). The build follows the phases in §11.2.
>
> This was written as a design only; no production system was touched. §13 lists the questions that were put to the owner, each with its recommendation (now the decision). The first consumer's (kof22) private adoption notes belong in that app's own repository, not here; §10 only describes the adoption at the level a public reader needs.

---

## 0. Summary

The owner asked for *"an 'Alerts or Manage By Exceptions' qbit that all other systems can use to create groups and alerts, and monitors and alarms etc that people can subscribe to etc, or connect them to slack rooms or email lists etc. it should be a generic qbit that all other systems can use and know about."* Slack must be optional: one channel among several, never required.

**The recommendation in one paragraph.** Split the work into two parts:

1. **A tiny producer SPI in QQQ core (4.1.x).** Any qbit or app can call `QAlerts.raise(...)`, `QAlerts.resolve(...)` and `QAlerts.heartbeat(...)`. It can also declare the alert types it can produce as metadata (`QAlertTypeMetaData`). With no alerts implementation installed, these calls are cheap no-ops that log, so every producer works with or without the qbit and needs no compile dependency beyond `qqq-backend-core`.
2. **`qbit-alerts`, the implementation.** It stores alerts as ordinary QQQ records:
   - alert types (synced from metadata), monitors, alerts with their records and timeline, groups, subscriptions, user preferences, teams, channels, mute windows and a notification log
   - a state machine with dedup, reopen, flapping control and auto-resolve
   - an evaluator for table-query, aggregate-threshold, heartbeat, process-failure, ESB-event and code-registered monitors
   - a delivery worker with a pluggable **channel SPI**: in-app inbox (the bell), email (individuals and lists), and optionally Slack, Microsoft Teams, generic webhook, PagerDuty and Prometheus Alertmanager (both as a sink and as a source). SMS comes later.

   Its **Alerts home** is the single owner of open-alert data. Every other screen (Admin home "Needs Attention", the CDN or Storage homes, record screens) shows a link-only summary or a per-record panel that links back.

**Why this split.** It is the shape the dependent specs already asked for:
- CDN §4.9.1 asks for "no compile-time requirement on the Alerts qbit beyond its SPI module".
- Object storage §9.3 binds a no-op sink when the qbit is absent.
- Web performance D9 routes through Prometheus "+ Data Health" today, for lack of anything better.

A core SPI makes all three trivial and removes the reflection-based detection each spec was about to invent.

**What it replaces.** The first host, kof22, has four "exception" features today: Data Health (about 140 rules), the ESB health views, Quick Search validation runs, and the Admin home "Needs Attention" list. None of them stores findings or supports acknowledge, snooze, resolve or notifications. Everything is computed live, per replica, as the system user. They become producers (§10). Data Health's rule framework becomes the qbit's monitor engine instead of a separate `qbit-data-health`.

**Effort:** about **14 engineer-weeks** (one engineer with Claude) for phases P0–P5, plus 2–3 weeks for the optional P6. The core SPI (P0, about 1.5 weeks) is first and unblocks every other qbit immediately (§11).
**Cost:** $0. Email uses the host's existing SMTP relay. Every paid integration (PagerDuty, Slack plans) is optional and the customer's own.

---

## 1. Rules this design follows

| Rule | How this design follows it |
|---|---|
| Qbit data lives in standard, visible QQQ tables | Every concept (§4) is a normal QQQ table with full record screens: alert types, monitors, alerts, their records and events, subscriptions, channels, mutes and notifications. Access is controlled by `alerts.*` permissions and record security locks. Nothing is hidden in a side store, and channel secrets are references (§7.3). |
| Processes are table or record actions, never in the nav | Acknowledge, Snooze, Resolve, Assign, Subscribe, Run check now, Send test notification and Retry delivery are all table or record actions (§6). Scheduled jobs (evaluate, deliver, digest) are hidden processes. Labels are plain, with no "ETL". |
| Actions use the standard streamed ETL flow | Every action is `StreamedETLWithFrontendProcess`: record input, then standard Review summary lines, then Result summary lines. "Test monitor" is the standard Review step of "Run check now", not a custom dry-run form. |
| One home per app, no duplicate dashboards | The Alerts home owns every open-alert widget. The Admin home "Needs Attention", the Dashboard and other qbit homes get link-only summaries that use the owner's loader (§6.5). Every app home stays useful on its own. |
| Readable at all widths, no double scrollbars | Tables of 3+ columns are full width with at most 4 columns. There are no declared column widths and no inner scroll; "View all" links are used instead. Cards stack below 900 px. The bell popover and record panel are single-column below 640 px. Screenshots at 6 widths are a P3 exit criterion. |
| Real data only | Every count, trend and age comes from `alerts_alert` and `alerts_event` rows. Empty states say why ("No monitors yet", "No open alerts"). There are no sample alerts and no placeholder numbers. A "Send test notification" goes through the real pipeline and is labelled TEST. |
| Permissions follow the logged-in user | Lists, counts, the bell and notifications are all filtered by what the *recipient* can see (§7.2). Monitors are evaluated by a trusted runner, but nothing they find is shown or sent to a user who cannot read the underlying record. |
| Kernel pieces in core 4.1.x, UI in qbits | Core gets only the SPI, the type metadata and the validator (§3). Everything else is in `qbit-alerts` and Next UI. |
| Public Apache-2.0 under QRun-IO; host internals stay private | Nothing here depends on, or names, a host's private hostnames, channels, groups or credentials. |
| Planned downtime fine; no data loss; no hacks | The schema is additive. Producers keep working (as no-ops) while the qbit is absent or down. |

**Manage by exception, as a design rule.** An alert means *a person should do something*. Informational events belong in audit, process trace or the ESB, not here. The INFO severity exists for "worth a glance" items that never page anyone, and channels ignore it by default (§5.4).

---

## 2. Concepts at a glance

```
              producers                                         qbit-alerts                                       people
 ┌────────────────────────────────┐          ┌───────────────────────────────────────────────────────┐    ┌─────────────────────┐
 │ any qbit / app code            │ raise /  │  alerts_type ◄─ sync from QAlertTypeMetaData (startup)│    │ Alerts home         │
 │   QAlerts.raise/resolve/beat ──┼─resolve─►│      │                                                │    │ bell (inbox)        │
 │ (core SPI, no-op w/o the qbit) │          │      ▼                                                │    │ record panels       │
 │                                │          │  alerts_alert ──► alerts_record_ref, alerts_event     │───►│ email / Slack /     │
 │ monitors (alerts_monitor data) │ evaluate │   ▲  dedup · reopen · flapping · auto-resolve         │    │ Teams / webhook /   │
 │   table query · aggregate ·    ├─────────►│   │                                                   │    │ PagerDuty /         │
 │   heartbeat · process failed · │          │   │  subscriptions + mutes + quiet hours + digests    │    │ Alertmanager        │
 │   ESB event · code check       │          │   │          │                                        │    └─────────────────────┘
 │                                │          │   │          ▼                                        │
 │ Alertmanager webhook (ingest) ─┼─────────►│   │   alerts_notification ──► channel SPI ─► send       │
 └────────────────────────────────┘          └───────────────────────────────────────────────────────┘
```

| Concept | What it is | Table |
|---|---|---|
| **Alert type** | A kind of problem a source can report, e.g. `cdn.purge.dead`. It is declared in metadata by the qbit that produces it and synced into a table so admins can see and tune every type that every installed qbit can raise. | `alerts_type` |
| **Monitor** | A configured check that raises alerts of a type: table query, aggregate threshold, heartbeat, process failure, ESB event pattern, or code check. Some monitors are seeded by qbits and some are created by admins. | `alerts_monitor` |
| **Alert** | One open (or closed) problem, identified by `(alerts_type, dedupKey)`. Repeats update it; they never duplicate it. | `alerts_alert` |
| **Alert record ref** | The records the alert is about (zero to many). These drive the record panel and visibility. | `alerts_record_ref` |
| **Alert event** | The alert's timeline: raised, repeated, acknowledged, snoozed, resolved, reopened, notified, comment and so on. It also feeds the trend charts. | `alerts_event` |
| **Group** | A category such as "Backups", "CDN" or "Content quality", optionally nested. Every type belongs to one. | `alerts_group` |
| **Subscription** | Who hears about what: a user, a team or a channel target, scoped to all alerts, a group, a type, a monitor, a table or one record, with a minimum severity and a delivery mode. | `alerts_subscription` |
| **Preference** | Per-user quiet hours, digest time, time zone and default destinations | `alerts_user_preference` |
| **Team** | A named set of users (optionally resolved from roles or IdP groups) | `alerts_team`, `alerts_team_member` |
| **Channel** | A configured destination: an email list, a Slack webhook, a PagerDuty service and so on. It holds settings and a *reference* to its secret. | `alerts_channel` |
| **Mute** | A maintenance window or mute rule that suppresses notifications (and optionally new alerts) for a scope and a time | `alerts_mute` |
| **Notification** | One delivery attempt record: who, which channel, status, retries and error | `alerts_notification` |
| **Inbox item** | One alert in one user's bell | `alerts_inbox_item` |
| **Escalation policy** (later phase) | Steps such as "after 15 min unacknowledged, notify team X" | `alerts_escalation_policy`, `alerts_escalation_step` |

---

## 3. QQQ core: the producer SPI (4.1.x)

Everything in this section lives in `qqq-backend-core`, package `com.kingsrook.qqq.backend.core.alerts`. It has no tables, no threads and no new dependencies, and with nothing installed it costs one map lookup and a rate-limited log line.

### 3.1 Calling it

```java
// Raise (or refresh) an alert. Idempotent on (type, dedupKey).
QAlerts.raise(new AlertSignal("cdn.purge.dead")
   .withDedupKey("provider:" + providerId)               // stable; repeats update one open alert
   .withSeverity(AlertSeverity.ERROR)                     // optional; defaults to the type's severity
   .withTitle("3 CDN purges failed after retries")        // external-safe text (§7.4)
   .withSummary("Content will correct itself when the edge TTL expires.") // external-safe
   .withDetails(detailsText)                              // in-app only, never sent to external channels
   .withRecordRefs(AlertRecordRef.of("cdn_purge_request", ids))  // the offending records
   .withLink(AlertLink.toTable("cdn_purge_request", filter, "Failed purges"))
   .withAttribute("provider", providerName));             // routing labels, not displayed by default

// Resolve it (no-op if it is not open).
QAlerts.resolve("cdn.purge.dead", "provider:" + providerId, "A later purge succeeded");

// Dead-man's switch: "I ran". A HEARTBEAT monitor raises when beats stop (§5.3).
QAlerts.heartbeat("backups.job.nightly-db");
```

| Type | Fields |
|---|---|
| `AlertSignal` | `alertTypeName` (required), `dedupKey` (default: the type name, which gives one alert per type), `severity`, `title`, `summary`, `details`, `recordRefs`, `links`, `attributes` (`Map<String,String>`, bounded at 20 keys), `occurredAt` (default now), `count` (for aggregate alerts, e.g. "37 records") |
| `AlertSeverity` | `INFO`, `WARNING`, `ERROR`, `CRITICAL`. The mappings: web performance WARNING/CRITICAL, CDN and storage warning/error, Data Health INFO/WARNING/ERROR, and Prometheus `info`/`warning`/`critical` (plus `error` where used). |
| `AlertRecordRef` | `tableName`, `primaryKey`, optional `label` (in-app only). At most 100 refs per signal; beyond that the signal carries `count` and a filter link. |
| `AlertLink` | `label` plus one of: a record, a table with a `QQueryFilter`, an app or widget path, or an absolute URL (allowed only for links the type declares) |

**Behavior guarantees** (the same promises the ESB makes for publishing):
- **Never throws into the caller and never blocks it.** A failure is logged and counted (`qqq_alerts_producer_failures_total`).
- **Transaction-aware.** If called while a `QBackendTransaction` is open, the signal is queued with `addAfterCommitCallback` (already in core) and is dropped on rollback, so an alert never describes a write that did not happen.
- **Validated cheaply.** An unknown `alertTypeName` is accepted, but recorded as type `unregistered` with a warning, so a typo never loses a signal. The validator (§3.3) catches it at boot for declared producers.

### 3.2 The sink

```java
public interface AlertSinkInterface
{
   void raise(AlertSignal signal);            // called after commit, on the caller's thread; must be fast
   void resolve(AlertResolution resolution);
   void heartbeat(String heartbeatKey, Instant at);
}
```

- It is registered through a supplemental instance metadata, `AlertsInstanceMetaData.ofOrWithNew(qInstance).withSink(new QCodeReference(...))`, following the `DeveloperPanelsMetaData` pattern.
- **The default is `LoggingAlertSink`.** It logs at INFO (WARNING and above at WARN), rate-limited per `(type, dedupKey)`. Production code paths are then identical with and without the qbit.
- `qbit-alerts` registers `QbitAlertsSink`. It writes the signal to an in-memory bounded queue drained by a virtual thread into the `alerts_*` tables (§5.1), so the caller never waits on the database. If the queue overflows, the signal is dropped and counted, and `alerts.sink.overflow` is raised once.

### 3.3 Declaring alert types (so the system "knows about" every source)

```java
qInstance.add(new QAlertTypeMetaData()
   .withName("cdn.purge.dead")
   .withLabel("CDN purge failed")
   .withSourceName("qbit-cdn")                         // shown as "Source"; set automatically from SourceQBitAware
   .withGroupName("cdn")                               // default group; created if missing
   .withDefaultSeverity(AlertSeverity.ERROR)
   .withDescription("A purge failed after its last retry or was marked DEAD.")
   .withHowToFix("Open the failed purge and choose Retry purge. Content corrects itself when the TTL expires.")
   .withSubjectTableName("cdn_purge_request")            // optional; drives visibility (§7.2) and the record panel
   .withVisibilityPermission("cdn.view")               // optional extra gate
   .withResolveMode(ResolveMode.PRODUCER)              // PRODUCER | MONITOR | QUIET_PERIOD(Duration) | MANUAL
   .withDefaultMonitor(...)                            // optional seeded monitor (§5.3), e.g. a heartbeat
   .withExternalDetailCeiling(DetailLevel.STANDARD));  // the most a channel may ever include (§7.4)
```

- **Where it lives.** It is a supplemental instance metadata (`AlertsInstanceMetaData.alertTypes`), not a new top-level `QInstance` collection, so core's public surface barely changes. A `QAlertTypeMetaData.of(qInstance, name)` helper reads it.
- **Validation** (`AlertsInstanceMetaData.validate`): unique names matching `^[a-z0-9]+(\.[a-z0-9-]+)+$`, the subject table exists, the visibility permission is non-blank, and a heartbeat default monitor has an interval.
- **Process failures without code.** A new `AlertProcessMetaData` (a `QSupplementalProcessMetaData`) has `alertOnFailure` (a severity) and `expectedEvery` (a `Duration`, for a heartbeat). Core's existing `ProcessLifecycleListenerInterface` feeds both. That gives any process "tell me when this fails" and "tell me when this didn't run" by metadata alone (§5.3).
- **Discovery.** At startup, `qbit-alerts` upserts every declared type into the `alerts_type` table (`installed = true`, metadata fields refreshed) and marks types no longer declared as `installed = false`. They are never deleted, so history keeps its labels. An admin therefore sees, in an ordinary table, every alert type every installed qbit can produce, with its source, description, how-to-fix, default severity and seeded monitors.

### 3.4 Other core changes (small)

| Change | Why | Size |
|---|---|---|
| `RecordAccessHelper.visibleKeys(table, keys, session)`: one `QueryAction` under the given session with `pk IN (...)`, returning the visible subset (record locks and #1075 enforcement apply) | Recipient and viewer filtering (§7.2) without each caller rebuilding it | 1 d |
| Generalize #1076 developer panels to **record panels**: `RecordPanelProviderInterface` (the same shape as `DeveloperPanelProviderInterface`) with a `placement` of `RECORD_VIEW` or `DEVELOPER`, and `GET /qqq/v1/table/{t}/{pk}/panels?placement=` | The "Open alerts" panel on every table with alerts (§6.3). The CMS, web performance and storage record panels can use the same mechanism instead of each one adding T2 widgets per table. | 2–3 d |
| Instance frontend metadata `topBarItems` (type + endpoint + permission), included only when the user has the permission | The Next UI bell appears only when `qbit-alerts` is installed and the user has `alerts.view` | 1 d |

Total core work is about 1.5 engineer-weeks with tests (§11).

---

## 4. Tables

Default table prefix `alerts_` (namespace `alerts`), configurable with `QBitConfig.namespace` (the quick-search pattern). Table names are lowercase snake_case and the backend (SQL) names are identical; possible-value sources take the table's name.

**Naming (owner rule, 2026-10-12).** "table names should always be lowercase and use _, and qbits should always be prefixed with their name, ie, cdn_ etc". So every table is `alerts_<name>` (the main table is `alerts_alert`), never `alert` or `alertType`; process and widget names use the full `alerts` prefix too (for example `alertsEvaluateMonitors`). Java class names stay CamelCase.

Every table has `id`, `createDate` and `modifyDate`. Audit is on (FIELD level) for tables people edit: types, monitors, alerts, subscriptions, channels, mutes, teams and policies. `alerts_event`, `alerts_notification` and `alerts_inbox_item` are written by the qbit and are read-only in the UI.

| # | Table (label) | Key fields | Indexes / notes |
|---|---|---|---|
| 1 | `alerts_type` (Alert Types) | `name` (unique), `label`, `sourceName`, `groupId`, `defaultSeverity`, `severityOverride`, `enabled`, `installed`, `description`, `howToFix`, `subjectTableName`, `visibilityPermission`, `resolveMode`, `quietPeriodMinutes`, `externalDetailCeiling`, `notify` (admin switch: alerts are recorded but never sent), `lastRaisedAt`, `openCount` (denormalized) | Metadata fields are refreshed at boot. Admin fields (`enabled`, `severityOverride`, `groupId`, `notify`) are kept. |
| 2 | `alerts_group` (Alert Groups) | `name` (unique), `label`, `parentGroupId`, `icon`, `sortOrder`, `description`, `ownerTeamId` | Groups are seeded by qbits from `groupName` and editable. |
| 3 | `alerts_monitor` (Monitors) | `name`, `alertTypeId`, `kind` (TABLE_QUERY / AGGREGATE / HEARTBEAT / PROCESS_FAILED / ESB_EVENT / CODE / EXTERNAL), `enabled`, `seeded`, `sourceName`, `definition` (JSON, by kind, §5.3), `perRecord` (one alert per matching record, or one aggregate alert), `severity`, `scheduleSeconds` or `cronExpression`, `nextRunAt`, `claimedBy`, `claimedUntil`, `lastRunAt`, `lastStatus` (OK / FIRING / CHECK_FAILED / SKIPPED_MUTED), `lastValue`, `lastError`, `lastElapsedMs`, `consecutiveFailures`, `forMinutes` (must hold this long before it fires) | (`enabled`, `nextRunAt`). Seeded monitors may be disabled or have thresholds tuned, but not deleted. |
| 4 | `alerts_alert` (Alerts) | `alertTypeId`, `monitorId`, `groupId`, `dedupKey`, `openDedupKey` (= type + key while not RESOLVED, null after; **unique**), `status` (OPEN / ACKNOWLEDGED / SNOOZED / RESOLVED), `severity`, `title`, `summary`, `details`, `subjectTableName`, `subjectRecordId`, `recordRefCount`, `itemCount`, `links` (JSON), `attributes` (JSON), `sourceName`, `firstRaisedAt`, `lastRaisedAt`, `occurrenceCount`, `reopenCount`, `flapping`, `acknowledgedByUserId`, `acknowledgedAt`, `snoozedUntil`, `assignedUserId`, `assignedTeamId`, `resolvedAt`, `resolvedByUserId`, `resolution` (PRODUCER / MONITOR / QUIET_PERIOD / MANUAL / EXPIRED), `resolutionNote`, `muted`, `lastNotifiedAt`, `escalationLevel`, plus the mirrored lock columns (§7.2) | unique (`openDedupKey`); (`status`, `severity`); (`alertTypeId`, `status`); (`subjectTableName`, `subjectRecordId`); (`assignedUserId`, `status`); (`lastRaisedAt`) |
| 5 | `alerts_record_ref` (Alert Records) | `alertId`, `tableName`, `recordId`, `label`, `active` (still part of the current occurrence) | (`tableName`, `recordId`, `active`); (`alertId`) |
| 6 | `alerts_event` (Alert Timeline) | `alertId`, `eventType` (RAISED / REPEATED / SEVERITY_CHANGED / ACKNOWLEDGED / SNOOZED / UNSNOOZED / ASSIGNED / COMMENTED / ESCALATED / NOTIFIED / MUTED / FLAPPING_STARTED / FLAPPING_ENDED / RESOLVED / REOPENED), `at`, `userId` (null for system), `fromValue`, `toValue`, `note` | (`alertId`, `at`); (`eventType`, `at`). It feeds the trend charts and MTTA/MTTR. |
| 7 | `alerts_subscription` (Subscriptions) | `subscriberType` (USER / TEAM / CHANNEL), `userId`, `teamId`, `channelId`, `scopeType` (ALL / GROUP / ALERT_TYPE / MONITOR / TABLE / RECORD / ATTRIBUTE), `scopeValue`, `scopeRecordId`, `minSeverity`, `events` (RAISED, ESCALATED, REMINDER, RESOLVED), `deliveryMode` (IMMEDIATE / DIGEST_HOURLY / DIGEST_DAILY), `channelOverrideIds` (the user's destinations for this subscription; default: their preferences), `enabled`, `createdByUserId` | Record-locked on `userId` for USER subscriptions (§7.1). Unique (subscriber, scope). |
| 8 | `alerts_user_preference` (My Alert Settings) | `userId` (unique), `timeZone`, `quietHoursStart`, `quietHoursEnd`, `quietDays`, `quietBypassSeverity` (default CRITICAL), `digestHour`, `defaultChannelIds`, `emailEnabled`, `inAppEnabled`, `pausedUntil` (vacation) | Record-locked on `userId`. Created lazily with defaults. |
| 9 | `alerts_team` / `alerts_team_member` (Teams) | team: `name`, `label`, `memberSource` (MANUAL / ROLE / IDP_GROUP), `memberSourceValue`; member: `teamId`, `userId`, `role` (MEMBER / LEAD) | Members come from the table, or are resolved at send time by `AlertAudienceResolver` (§5.6). |
| 10 | `alerts_channel` (Channels) | `name`, `label`, `channelType` (IN_APP / EMAIL / EMAIL_LIST / SLACK / TEAMS / WEBHOOK / PAGERDUTY / ALERTMANAGER / OPSGENIE / SMS), `enabled`, `settings` (JSON, non-secret), `secretRef` (**the name** of an env var or secret key, never the value), `detailLevel` (MINIMAL / STANDARD / FULL), `minSeverity`, `rateLimitPerHour`, `healthStatus` (HEALTHY / PROBATION / UNHEALTHY, the qbit-webhooks model), `lastSuccessAt`, `lastFailureAt`, `consecutiveFailures` | Admin only (§7.1). |
| 11 | `alerts_mute` (Mutes and Maintenance) | `name`, `reason`, `scopeType` and `scopeValue` (as subscriptions), `matchAttributes` (JSON), `startsAt`, `endsAt`, `recurrenceCron`, `recurrenceDurationMinutes`, `effect` (SILENCE_NOTIFICATIONS / SUPPRESS_ALERTS), `createdByUserId` | (`startsAt`, `endsAt`) |
| 12 | `alerts_notification` (Notification Log) | `alertId`, `alertEventId`, `subscriptionId`, `channelId`, `recipientUserId`, `recipientLabel` (masked, e.g. `j***@example.com`), `kind` (RAISED / ESCALATED / REMINDER / RESOLVED / DIGEST / TEST), `digestKey`, `status` (PENDING / SENDING / SENT / RETRYING / FAILED / DEAD / SUPPRESSED), `suppressedReason` (QUIET_HOURS / MUTED / FLAPPING / RATE_LIMITED / NOT_VISIBLE / CHANNEL_DISABLED / DUPLICATE / USER_PAUSED), `attempts`, `nextAttemptAt`, `claimedUntil`, `sentAt`, `providerMessageId`, `lastError` (truncated and redacted), `payloadHash` | (`status`, `nextAttemptAt`); (`alertId`); (`channelId`, `status`) |
| 13 | `alerts_inbox_item` (My Alerts Inbox) | `userId`, `alertId`, `notificationId`, `deliveredAt`, `readAt`, `archivedAt` | Record-locked on `userId`. (`userId`, `readAt`) |
| 14 | `alerts_escalation_policy` / `alerts_escalation_step` (P6) | policy: `name`, `repeatAfterMinutes`, `maxRepeats`; step: `policyId`, `afterMinutes`, `targetType` (USER / TEAM / CHANNEL), `targetId`, `onlyIfStatus` (OPEN) | Attached to groups or types (`escalationPolicyId`) |

**Retention defaults** (configurable): alerts, record refs and events are kept forever (a few thousand rows a year for a typical app). Notifications are kept 365 days and inbox items 90 days after they are read or archived. A daily hidden `alertsPurge` process enforces this.

**Liquibase.** One prefix-parameterized changelog, included by the host (the quick-search and webperf pattern), plus a `startupMode` schema check (`FAIL_FAST` / `WARN`).

---

## 5. The engine

### 5.1 Alert state machine

```
              raise (new key)                ack                    snooze(until)
  (none) ───────────────────────► OPEN ─────────────► ACKNOWLEDGED ─────────────► SNOOZED
                                   ▲  ▲                    │                         │ until passes, or a
                                   │  └──── unsnooze ──────┼─────────────────────────┘ severity increase
            reopen (raised again   │                       │
            within reopenWindow)   │      resolve (producer / monitor clear / quiet period / manual)
                                   └────────── RESOLVED ◄──┴──────────────────────────────── (from any open state)
```

- **Raise on an open key** updates `lastRaisedAt`, increments `occurrenceCount` and merges record refs. A severity *increase* records SEVERITY_CHANGED, unsnoozes, and notifies again. A decrease only records the change.
- **Raise on a key resolved less than `reopenWindow` ago** (default 30 min) reopens the same alert (`reopenCount++`, REOPENED) instead of creating a new one. Later raises create a new alert, and the history stays on the old one.
- **Flapping.** If an alert changes between open and resolved 5 or more times in 60 minutes, `flapping = true` (FLAPPING_STARTED). Subscribers get one "flapping" notice, and further open/resolve notifications are suppressed (`suppressedReason = FLAPPING`) until it has been stable for 60 minutes. All thresholds are configurable per type.
- **`forMinutes` on monitors** (default 0): a condition must hold across evaluations for that long before an alert opens. This is the Alertmanager `for:` idea and stops one bad sample from paging.
- **Auto-resolve modes** come from the type: `PRODUCER` (a `resolve` call), `MONITOR` (the next evaluation is clean), `QUIET_PERIOD` (no raise for N minutes, e.g. storage's "24 h with no new failures"), or `MANUAL`. Any mode also allows a manual Resolve with a note.
- **Acknowledge** stops reminders and escalation, but the alert stays visible until resolved. **Snooze** hides it from "needs action" lists and stops notifications until a time; it returns early if the severity rises.
- **Concurrency.** The unique `openDedupKey` makes "insert, or update the open one" safe across replicas: on a duplicate-key error, re-read and update. Every transition writes its `alerts_event` in the same transaction.

### 5.2 Grouping

There are two kinds of grouping, kept apart on purpose:
- **Categories** (`alerts_group`) are for people: the home groups by them, subscriptions target them, and the Needs Attention summary counts by them.
- **Aggregation** is for noise. A monitor in aggregate mode (`perRecord = false`) raises *one* alert ("37 published pages have no excerpt") with up to 100 record refs, an `itemCount` and a filter link. Per-record mode (one alert per record) is for problems that people fix one record at a time, such as "backup job X failed". The default is aggregate, and per-record mode is capped (default 50 open alerts per monitor; beyond the cap it falls back to one aggregate alert and says so).
- **Notification batching:** IMMEDIATE notifications that arrive within a 60-second window for the same recipient and channel are sent as one message ("3 new alerts in CDN"). Digests (§5.5) do this hourly or daily.

### 5.3 Monitors

All monitors run in one hidden scheduled process, `alertsEvaluateMonitors` (every 30 s). It claims due monitors with a conditional update (`claimedUntil < now`), so any number of replicas share the work with no Quartz clustering required. Each evaluation has a timeout (default 15 s) and a row-scan cap. A monitor that throws gets `lastStatus = CHECK_FAILED`, and after 3 consecutive failures it raises the meta-alert `alerts.monitor.failing`. A broken check is itself an exception to manage.

| Kind | Definition (JSON in `alerts_monitor.definition`) | Fires when | Typical use |
|---|---|---|---|
| `TABLE_QUERY` | `tableName`, `filter` (a standard `QQueryFilter`), `threshold` (default > 0), `labelField` | The count of matching rows passes the threshold. In per-record mode, each matching row is an alert. | Data Health `FilterCheck` rules ("published posts without an excerpt"), "orders stuck in PENDING > 2 h" |
| `AGGREGATE` | `tableName`, `filter`, `aggregate` (COUNT / SUM / AVG / MIN / MAX over a field, optional group-by field), `comparator`, `warningThreshold`, `errorThreshold`, `criticalThreshold` | The value crosses a band. Group-by gives one alert per group value. | Invalidation budget, storage bytes against a quota, failed logins in 24 h |
| `HEARTBEAT` | `heartbeatKey` **or** `processName` (completed successfully) **or** `tableName` + `dateField` + `filter` (the newest row's age), plus `expectedEverySeconds` and `graceSeconds` | Nothing was seen within interval + grace | "Nightly backup did not run", "the web performance runner is stale", "no Quick Search validation in 26 h" |
| `PROCESS_FAILED` | `processName` (or a name pattern), `minConsecutive` (default 1), `severity` | A process run fails (from the core lifecycle listener, no ESB needed). Resolves on the next successful run. | Any scheduled process; also seeded from `AlertProcessMetaData.alertOnFailure` |
| `ESB_EVENT` | `destinationName`, `typePattern` (glob over the CloudEvent `type`), `predicate` (a small field test on `data`: equals, in, exists), `dedupKeyTemplate` (e.g. `{subject}`), `resolveTypePattern` | A matching event arrives. A matching resolve event closes it. Runs as an ESB topic trigger with its own shared durable subscription. | `qqq.process.*.failed` across services; Authentik webhook events relayed through the ESB |
| `CODE` | `codeReference` to an `AlertCheckInterface`, `params` | The check returns signals. The engine resolves open alerts of that monitor whose keys the check did not return (a full-state diff). | Data Health `SqlCheck` / `EsbCounterCheck` / Quartz rules, ESB dead letters, Quick Search validation, anything a qbit wants to compute |
| `EXTERNAL` | `source` (ALERTMANAGER / WEBHOOK), `matchLabels` | Ingested (§5.7) | Infrastructure alerts from Prometheus |

```java
public interface AlertCheckInterface
{
   /** Return every currently-failing condition. Keys not returned are resolved. */
   List<AlertSignal> check(AlertCheckContext context) throws QException;
}
```

- **Who the checks run as.** Monitors run trusted (`CorePermissionEnforcer.runTrusted("alerts monitor <name>", ...)`), because a monitor must see everything to be correct. What a user is then *shown* or *sent* is filtered by that user's permissions (§7.2). This answers the assistant spec's re-scoping concern (#1054 §6.3) in one place, instead of in every adapter.
- **Seeded monitors.** A qbit can ship monitors through `QAlertTypeMetaData.withDefaultMonitor(...)`. They are inserted once (`seeded = true`) and then belong to the admin: thresholds are editable and the monitor can be disabled, but it cannot be deleted while the type is installed.
- **Monitors are data, so "Test monitor" is the normal flow.** The "Run check now" record action runs the evaluation in its Review step without writing alerts ("Would open 1 alert: 37 records. Would resolve 0."). Confirming applies it, and the Result screen shows what changed.

### 5.4 Routing and fan-out

When an alert event is notifiable (RAISED, REOPENED, SEVERITY_CHANGED upward, ESCALATED, REMINDER, RESOLVED), the router:
1. **Collects matching subscriptions:** scope ALL, GROUP (including parents), ALERT_TYPE, MONITOR, TABLE (equals `subjectTableName`), RECORD (any active record ref), or ATTRIBUTE (`key=value`), with `minSeverity ≤ severity` and the event in `events`.
2. **Expands** TEAM subscriptions to users (through `AlertAudienceResolver`), and keeps CHANNEL subscriptions (e.g. "everything ERROR+ in Backups goes to the ops email list") as they are.
3. **Filters each user recipient** for visibility (§7.2). A recipient who cannot see the alert gets nothing, logged as `SUPPRESSED / NOT_VISIBLE`. Channel subscriptions are not per-person, so they are gated by the channel's `detailLevel` (§7.4) and by the subscription creator's own visibility at creation time (only `alerts.admin` can create CHANNEL subscriptions).
4. **Applies, in order:** the type's `notify` switch, mutes, flapping, the user's `pausedUntil`, quiet hours (CRITICAL breaks through by default), the channel's `minSeverity` and rate limit, and dedup (same alert, channel, recipient and kind within 10 min). Every suppression is still written as a `SUPPRESSED` row with its reason, so "why didn't I get paged?" is answered by a table.
5. **Writes `alerts_notification` rows** (PENDING), plus an `alerts_inbox_item` for every in-app recipient.

**Defaults that keep it calm:** with no subscriptions configured, a fresh install notifies nobody. Every alert still appears on the Alerts home and in the bell of users subscribed by the bootstrap rule. The bootstrap rule is `alerts.admin` holders, ERROR and above, in-app only. INFO is never sent to external channels unless a subscription asks for it.

### 5.5 Delivery, retries, digests

- **`alertsDeliverNotifications`** (hidden, every 15 s) claims PENDING and due RETRYING rows, groups them by (recipient, channel) for the 60-second batch window, renders them (§7.4) and calls the channel.
- **Retries:** 1 min, 5 min, 15 min, 1 h, 4 h, then DEAD (6 attempts, configurable per channel). A 4xx that the channel reports as permanent (bad webhook URL, unknown address) goes straight to FAILED.
- **Channel health** follows the qbit-webhooks model (HEALTHY / PROBATION / UNHEALTHY). Three consecutive DEAD deliveries mark a channel UNHEALTHY and raise `alerts.channel.failing`. That alert is routed **in-app only**, to `alerts.admin`, so a broken channel can never loop.
- **Digests:** `alertsSendDigests` (hidden, hourly) groups DIGEST_* notifications per user into one message: open alerts by severity, new since the last digest and resolved since the last digest, each with a link. An empty digest is not sent.
- **Reminders:** an OPEN alert that has not been acknowledged sends a REMINDER every `reminderIntervalMinutes` (per type, default off; 60 for CRITICAL).
- **ESB (optional):** if `esbDestinationName` is set, alert lifecycle changes are also published as CloudEvents `qqq.alert.raised|acknowledged|resolved|reopened|escalated`, so other systems can react. It is off by default.

### 5.6 Channel SPI

```java
public interface AlertChannelInterface
{
   String getType();                                                  // "SLACK", "EMAIL", ...
   AlertChannelCapabilities getCapabilities();                        // threading, update-in-place, ack-back, batching, max size
   List<String> validate(AlertChannel channel, SecretResolver secrets);
   DeliveryResult deliver(AlertMessage message, AlertChannel channel, SecretResolver secrets);  // SENT / RETRY(after) / FAILED(permanent)
}
```

- Channel types are registered in `AlertsQBitConfig.channelTypes` as code references, so another qbit can add one (an SMS qbit, an internal chat system) without touching `qbit-alerts`.
- `AlertMessage` is already rendered and redacted for the channel's detail level (§7.4). Channels never see raw `details`.
- `AlertAudienceResolver` (an SPI) resolves TEAM members from `ROLE` (qbit-user-role-permissions) or `IDP_GROUP` (the Authentik qbit's group mirror) as well as `MANUAL`.

| Channel | Phase | How | Notes |
|---|---|---|---|
| **In-app (bell)** | P1 | `alerts_inbox_item` rows | Always available. Required, and cannot be disabled globally. |
| **Email** (to a user) | P1 | Core `SendMessageAction` through a configured `QMessagingProviderMetaData`: `EmailMessagingProviderMetaData` (SMTP) or SES | Plain-text plus simple HTML. `Message-ID`/`In-Reply-To` are derived from the alert id, so RESOLVED threads under RAISED in mail clients. `List-Id` and a footer link to the user's subscription record ("Change or stop these emails"). |
| **Email list** | P1 | Same, to a fixed address list in `settings` | For distribution lists. Addresses are masked in the log. |
| **Generic webhook** | P4 | HTTPS POST of a documented JSON body, signed with HMAC-SHA256 over an id, a timestamp and the body, using the shared signing format proposed in the qbit-webhooks assessment ([#1105](https://github.com/QRun-IO/qqq/pull/1105), §9; the shared signer lives in core per its D2). qbit-webhooks itself has no signing scheme today. 10 s timeout | When qbit-webhooks is installed, this channel may delegate to it (optional; decided in P4). |
| **Slack** (optional) | P4 | v1: Incoming Webhook URL (one channel per URL, the URL is the secret). v2 (optional): bot token with `chat.postMessage` / `chat.update`, so a resolve edits the original message and replies in its thread | Never required. No Slack SDK dependency; plain `java.net.http` and Block Kit JSON. The legacy `qqq-middleware-slack` bot is unrelated and is not reused. |
| **Microsoft Teams** | P4 | A Teams Workflows (Power Automate) "post to a channel when a webhook request is received" URL, with an Adaptive Card body | The classic Office 365 connectors are retired by Microsoft, so Workflows is the supported path. |
| **PagerDuty** | P4 | Events API v2: `trigger` / `acknowledge` / `resolve` with `dedup_key` = alert type + key. The routing key is the secret. | The natural choice for real paging and on-call. PagerDuty acknowledgements can come back through a signed webhook (P6). |
| **Prometheus Alertmanager (sink)** | P4 | `POST /api/v2/alerts` with labels `alertname`, `severity`, `qqq_type`, `qqq_group`, `qqq_origin="qbit-alerts"` and the annotations `summary` and `qqq_url`. It is re-posted while open and given `endsAt` on resolve. | Lets a host keep its existing Alertmanager routes (including any Slack routes) unchanged |
| **Opsgenie** | not planned | Alert API (`alias` = dedup key) | Atlassian ended Opsgenie sales on 2025-06-04 and ends support on 2027-04-05. Use PagerDuty, Jira Service Management or a webhook instead. |
| **SMS** | later | A channel qbit (e.g. Twilio or SNS) through the SPI | Out of scope for v1. It needs phone numbers, which are PII. |

### 5.7 Prometheus and Alertmanager as a source

- **Ingest endpoint:** `POST /qqq/v1/alerts/ingest/alertmanager` (a route provider in the qbit). It accepts Alertmanager's webhook payload (version 4) and authenticates it with a bearer token from `secretRef` (Alertmanager `http_config.authorization.credentials_file`).
- **Mapping:**
  - the alert's `fingerprint` becomes the `dedupKey`
  - `status` `firing`/`resolved` becomes raise/resolve
  - the `severity` label becomes the severity (unknown values map to WARNING)
  - the annotations `summary`/`description` become title/summary, after redaction
  - the type is `prometheus.<alertname>`, auto-registered as an EXTERNAL type in group `infrastructure`
  - labels are copied to `attributes` (bounded)
- **Loop guard.** Alerts carrying `qqq_origin="qbit-alerts"` are ignored on ingest. A host must never route an alert both ways.
- **Notifications default off for ingested types** (`notify = false`). Alertmanager is already notifying people. The value of ingest is one place to see and acknowledge everything; turning on QQQ notifications for these types is a per-type choice.
- **Watching the watcher.** The qbit exports Micrometer metrics when the app has a registry:
  - `qqq_alerts_open{severity,group}`
  - `qqq_alerts_evaluator_last_success_timestamp_seconds`
  - `qqq_alerts_notifications_total{channel_type,status}`
  - `qqq_alerts_producer_failures_total`
  
  Hosts with Prometheus should add one rule, "the alert evaluator has not run in 10 min", because an alerting system cannot report its own death.

---

## 6. Manage-by-exception UX

### 6.1 Where it lives

- **A top-level app, "Alerts"** (`alerts`, permission `alerts.view`), not an Admin sub-app. Every user with `alerts.view` has "my alerts", an inbox and subscriptions, and admins manage monitors and channels in the same place.
- **Data section, in this order:** Alerts, Monitors, Alert Types, Groups, Subscriptions, My Alert Settings, Teams, Channels, Mutes and Maintenance, Notification Log, Alert Timeline, Alert Records. Admin-only tables are hidden by permission (denyBehavior HIDDEN), not by moving them.

### 6.2 The Alerts home (the single owner)

Top to bottom. Every widget is full width, uses real data, and has an honest empty state.

| Widget | Type, width | Content |
|---|---|---|
| **Open by severity** | HTML, 12 | Four tiles: Critical, Error, Warning and Info open counts, each with the unacknowledged count and the oldest age ("2 Critical · 1 unacknowledged · oldest 3 h"). Each tile links to the filtered Alerts list. Below 640 px they become label/value rows. |
| **My alerts** | TABLE, 12, 4 columns | Alerts assigned to me or matching my subscriptions, unacknowledged first: Alert (title, linked), Severity, Age, Status. "View all" opens the "My alerts" saved view. Empty: "Nothing needs you. You are subscribed to 3 groups. [Manage subscriptions]". |
| **Needs action** | TABLE, 12, 4 columns | All visible OPEN, unacknowledged and unsnoozed alerts, ordered by severity then age: Alert, Severity, Age, Owner (assignee, team or "Unassigned"). The 10 newest; "View all". |
| **By group** | TABLE, 12, 4 columns | Group, Open, Oldest, Trend (7-day sparkline of opened alerts), each linking to the group's filtered list. This is the manage-by-exception overview. |
| **Trend** | LINE_CHART, 12 | 30 days of alerts opened and resolved per day, with mean time to acknowledge and mean time to resolve shown as numbers beside the chart (from `alerts_event`). The chart starts at the first recorded day. |
| **Noisy monitors** | TABLE, 12, 4 columns | The top 10 types or monitors in the last 7 days by occurrences + reopens + flapping: Monitor, Opened, Reopened, Flapping. A row links to the monitor, where "Mute", "Edit threshold" and "Disable" are record actions. |
| **Delivery health** (`alerts.admin`) | TABLE, 12, 4 columns | Channels that are not HEALTHY, plus DEAD/FAILED counts for the last 24 h, linking to the Notification Log. Shown only when there is something to show; otherwise one line: "All 4 channels delivered normally in the last 24 h." |
| **Recently resolved** | TABLE, 12, 3 columns | The newest 5: Alert, Resolved (how and by whom), Duration |

### 6.3 Record panel: "Open alerts" on any record

- **What it is.** A `RecordPanelProviderInterface` (§3.4) registered by the qbit for every table that is a `subjectTableName` of some type, or that has `alerts_record_ref` rows (cached).
- **What it shows** on the normal record view (not the Developer view):
  - each open, visible alert that references this record: severity dot, title (linked), age, status
  - the actions "Acknowledge" and "Snooze", as links to the record actions on the alert
  - a "Watch alerts for this record" action, which creates a RECORD-scope subscription for the current user
  - "N resolved in the last 30 days", linking to the filtered list
- **When it shows.** By default it is hidden when the record has no open alerts and nothing resolved in 30 days (recommendation, Q7), so most records show nothing.
- **Layout.** One column below 640 px, no inner scroll, at most 5 rows plus "View all".

### 6.4 The bell (Next UI)

- **Where.** `Header.tsx` already renders a placeholder bell (`data-qqq-id="button-notifications"`, count fixed at 0). It becomes `AlertsBell`, rendered only when instance metadata advertises the `alertsBell` top-bar item (§3.4) for this user.
- **Data.** `GET /qqq/v1/alerts/inbox/summary` returns the unread count, the highest unread severity and the latest 10 items. It is polled every 60 s while the tab is visible and paused while it is hidden. Server-sent events are a later option (Q12).
- **Popover.** One column: a severity dot, the title, the source group and a relative age. Clicking an item opens the alert record and marks it read. The footer has "Mark all read" and "Open Alerts". A CRITICAL unread item makes the badge red.
- **Accessibility.** The button's label is "Alerts, 3 unread". The badge is announced politely, and the popover is focus-trapped and closes on Escape.

### 6.5 Link-only summaries elsewhere (no duplicate dashboards)

| Page | Gets | Owner? |
|---|---|---|
| **Alerts home** | Everything in §6.2 | **Owner** |
| Instance Dashboard / Admin home **"Needs Attention"** | One line per group with open ERROR+ alerts the viewer can see ("CDN · 1 critical, 2 errors"), each linking to the group's filtered Alerts list, plus "All alerts" | Summary only, through `AlertSummaryLoader` (the owner's loader) |
| Other qbit homes (CDN, Storage, Performance, ESB, Backups) | Optionally, a one-line "2 open alerts" link filtered to the qbit's group | Summary only |
| Record screens | The "Open alerts" panel (§6.3) | Owner of per-record detail |

`AlertSummaryLoader.summarize(session, scope)` is public API, so a host's own landing widgets reuse the owner's numbers and permission filtering, and never query alert tables themselves.

### 6.6 Actions (all `StreamedETLWithFrontendProcess`, all table or record actions)

| Action (label) | On | Permission | Review → Result |
|---|---|---|---|
| Acknowledge | `alerts_alert` (bulk) | `alerts.manage` | "Will acknowledge 4 alerts (1 already acknowledged, skipped)" → done |
| Snooze | `alerts_alert` (bulk) | `alerts.manage` | Input: until (a date/time, or 1 h / 4 h / tomorrow 09:00). Review lists the alerts. |
| Resolve | `alerts_alert` (bulk) | `alerts.manage` | A note is required. Review warns when the monitor is still firing ("will reopen at the next check"). |
| Reopen | `alerts_alert` | `alerts.manage` | |
| Assign | `alerts_alert` (bulk) | `alerts.manage` | Input: a user or team |
| Add comment | `alerts_alert` | `alerts.view` (only on alerts the user can see) | Writes a COMMENTED event |
| Subscribe me | `alerts_type`, `alerts_group`, `alerts_monitor` (records); "Watch alerts for this record" from the panel | `alerts.view` | Input: minimum severity and delivery mode. Creates the user's own subscription. |
| Run check now | `alerts_monitor` | `alerts.admin` | Review is the evaluation preview (would open / update / resolve). Result applies it. |
| Enable / Disable monitor | `alerts_monitor` (bulk) | `alerts.admin` | |
| Mute | `alerts_type`, `alerts_group`, `alerts_monitor`, `alerts_alert` | `alerts.manage` | Input: duration and reason. Creates an `alerts_mute`. |
| Send test notification | `alerts_channel` | `alerts.admin` | Sends a TEST message through the real pipeline and logs it |
| Retry delivery | `alerts_notification` (bulk, FAILED/DEAD) | `alerts.admin` | Requeues them |
| Create monitor from type | `alerts_type` | `alerts.admin` | Prefilled from the type's defaults |

---

## 7. Security

### 7.1 Permissions

| Permission | Grants |
|---|---|
| `alerts.view` | The Alerts app, home and bell. Read the alerts the user may see (§7.2), comment, subscribe themselves, edit their own preferences and inbox. |
| `alerts.manage` | Acknowledge, snooze, resolve, reopen, assign, mute; manage team subscriptions for teams they lead |
| `alerts.admin` | Monitors, types (overrides), groups, teams, channels, escalation, every subscription, the full notification log, channel subscriptions, retries |

- **They follow the session** (the #1061/#1075 rules). Every action re-checks permissions in its first step. API endpoints check server-side, and the UI only hides what the server denies.
- **Record locks on personal rows.** `alerts_subscription` (USER rows), `alerts_user_preference` and `alerts_inbox_item` have a record security lock on `userId`, keyed on the session user, so a user only sees and edits their own. `alerts.admin` gets an all-access key.
- **The host maps its roles.** Nothing is granted by default.

### 7.2 Alert visibility follows the underlying record

A user sees an alert only if **all** of these hold:
1. they have `alerts.view`
2. they have the type's `visibilityPermission`, if one is set (e.g. `cdn.view`)
3. they can READ `subjectTableName`, if one is set
4. if the alert has a subject record, that record is visible to them under the table's record security locks

**How each check is enforced:**
- **Checks 1–3 run in the query.** A pre-query customizer on `alerts_alert` (and on `alerts_record_ref`, `alerts_event`, `alerts_inbox_item` and `alerts_notification` through their joins) adds `visibilityPermission IN (<the session's permissions>) OR NULL` and `subjectTableName IN (<tables the session can READ>) OR NULL`. Counts, paging and widgets are therefore correct.
- **Check 4: mirrored lock values.** At raise time the engine reads the subject record's values for each `RecordSecurityLock` on its table and copies them into mirrored lock columns on `alerts_alert` (one nullable column per security key type listed in `AlertsQBitConfig.mirroredSecurityKeyTypes`, e.g. a multi-site host's site key). The `alerts_alert` table then declares ordinary `RecordSecurityLock`s on those columns with `nullValueBehavior = ALLOW`, so QQQ enforces them natively in every query and count. Single-tenant hosts configure none.
- **Defense in depth at render and at send.** Record refs shown on screens, and every user recipient at fan-out (§5.4), are re-checked with `RecordAccessHelper.visibleKeys`. Refs the user cannot see are simply not shown. There is no "3 hidden" hint, because that would leak that they exist. Aggregate alerts show the count the *monitor* found only when the type does not declare a `subjectTableName`. Otherwise the panel shows the visible refs and the title, and titles are written to be count-safe (§7.4).
- **System-evaluated alerts** (CODE checks over SQL, broker counters) carry no subject records and are gated by checks 1–2 only. This matches the assistant spec's "system-evaluated, admin only" rule.

### 7.3 Secrets

- **Channel secrets are references.** `alerts_channel.secretRef` holds a *name* resolved by `QMetaDataVariableInterpreter` (`${env.NAME}`, which also covers Kubernetes secrets injected by External Secrets / Infisical / Vault, `.env`, or AWS Secrets Manager through `SecretsManagerUtils`). The SMTP password, Slack webhook URL, PagerDuty routing key, Teams Workflows URL, webhook HMAC key and the Alertmanager ingest token are never stored in a table, a log, an audit or an error message.
- **Display.** `toString()` redacts them, and the channel record shows "Secret: present / missing", never the value.
- **Gap noted.** Core has no general secret-provider SPI today (only env, prop, `.env` and AWS Secrets Manager). The env-var path is enough for v1. If the object-storage spec's `${file.PATH}` reference lands (its §8.2), mounted-file secrets work too.
- **Webhook URLs are secrets.** Slack and Teams URLs grant posting rights, so they are treated as secrets, not settings.

### 7.4 No PII leaks to Slack or email by default

- **Three text fields with different reach.** `title` and `summary` are *external-safe*: a producer writes them without personal data ("3 CDN purges failed", not "Purge of /people/jane-doe failed"). `details` and record-ref `label`s are *internal*: shown in-app to users who pass §7.2, never sent externally.
- **Channel detail levels:**
  - `MINIMAL` (the default for every external channel): severity, type label, group, title, age and one link to the alert in QQQ
  - `STANDARD`: adds the summary and the counts
  - `FULL` (admin opt-in, with a warning on save): adds details and record labels
  
  The type's `externalDetailCeiling` caps this, so a security-sensitive type can never go out at FULL.
- **Redaction pass.** Every outbound string goes through `AlertRedactor`, which masks emails, phone numbers, IPv4/IPv6 addresses, JWTs, bearer tokens, long hex or base64 strings (≥ 32 characters), `key=`/`secret=`/`password=` values and URLs with credentials. The redactor has its own test corpus, and the email footer says the message was redacted.
- **Templates.** Messages are rendered from fixed per-channel templates (a small Mustache-like renderer with no logic and HTML escaping on). Producers supply only data.
- **Email to individuals goes only to users** who pass visibility (§7.2). Email lists and team channels are channel subscriptions and get MINIMAL unless an admin raises the level.

### 7.5 Abuse and safety

- Rate limits per channel (`rateLimitPerHour`, default 60) and per alert type (at most 1 notification per (alert, recipient, kind) per 10 min).
- Producer flood protection: at most 1,000 signals per minute per type are processed. Beyond that, signals are counted, not stored, and `alerts.producer.flood` is raised.
- Ingest endpoints require their token, have a body size limit (1 MB) and are rate-limited.

---

## 8. Configuration (`AlertsQBitConfig`)

| Setting | Default | Meaning |
|---|---|---|
| `backendName` | required | RDBMS backend for all tables |
| `namespace` | `alerts` | Table (`alerts_`), process, widget and permission prefix |
| `appName`, `appLabel`, `parentAppName` | `alerts`, `Alerts`, none | Top-level by default (Q4) |
| `schedulerName` | required for monitors | Without it, producer signals still work but monitors and delivery do not run, and the home says so |
| `messagingProviderName` | none | A `QMessagingProviderMetaData` for email. Without it, email channels are disabled with an explanation. |
| `fromAddress`, `publicBaseUrl` | none | Email From address, and the base URL for links in messages |
| `channelTypes` | built-ins | Extra `AlertChannelInterface` code references |
| `mirroredSecurityKeyTypes` | none | §7.2 |
| `audienceResolvers` | MANUAL | ROLE and IDP_GROUP resolvers |
| `esbDestinationName` | none | Publish alert lifecycle CloudEvents |
| `alertmanagerIngest` | off | `{ enabled, tokenSecretRef }` |
| `bootstrapSubscription` | `alerts.admin`, ERROR+, in-app | §5.4 |
| `reopenWindowMinutes`, `flapThreshold`, `flapWindowMinutes` | 30, 5, 60 | §5.1 |
| `retention` | alerts forever, notifications 365 d, inbox 90 d | §4 |
| `features` | all on | `MONITORS`, `EMAIL`, `EXTERNAL_CHANNELS`, `DIGESTS`, `ESCALATION` (P6), `INGEST`, `BELL`, `RECORD_PANEL`. A disabled feature produces no metadata. |

`validate()` checks the backend, the scheduler when monitors are on, the messaging provider when email is on, unique channel type names and resolvable secret refs. A missing secret disables that channel and does not fail startup.

---

## 9. How each dependent spec plugs in

Every producer depends **only on `qqq-backend-core`** (the SPI, §3). Each declares its alert types in metadata, and with no `qbit-alerts` installed its signals go to the logging sink. Each keeps its own home as the owner of its data, and alerts link back to it.

| Spec | Alert types (declared by the producing qbit) | Monitor kind | Resolves | Links to |
|---|---|---|---|---|
| **Web performance #1088** | `webperf.regression.confirmed` (per target/strategy/metric; WARNING or CRITICAL from the budget), `webperf.runner.stale`, `webperf.runs.failing` (quota or errors), `webperf.rum.band-worse` | push for regressions; HEARTBEAT on `webperfRunQueuedTests`; AGGREGATE over `webperf_lab_run` failures | regression auto-resolve rule (its §6.4 step 6) or dismiss → `resolve`; next successful run | the `webperf_regression` record and the Performance home |
| **CDN #1084** | `cdn.purge.dead`, `cdn.budget.near` / `cdn.budget.over`, `cdn.connection.failed`, `cdn.config.drift`, `cdn.secret.rotation-due` (its §4.9.1 table, unchanged) | push from the dispatcher; AGGREGATE for the budget; CODE for rotation age | as in its §4.9.1 | `cdn_purge_request`, `cdnProvider`, `cdnDriftFinding` |
| **Object storage #1089** | `storage.objects.missing`, `storage.objects.orphaned`, `storage.operation.failed` (QUIET_PERIOD 24 h), `storage.quota.near`, `storage.connection.failed`, `storage.scan.stale` (its §9.3 table, unchanged) | push from scans and operations; HEARTBEAT for scans | as in its §9.3 | filtered `storageObject` / `storageObjectReference`, `storageBucket` |
| **Backups** (spec in progress) | `backups.job.missed` (HEARTBEAT per job: expected interval + grace), `backups.job.failed` (ERROR), `backups.verify.failed` (CRITICAL: a restore test failed or a checksum mismatched), `backups.offsite.lagging`, `backups.retention.violated`, `backups.target.near-full` | HEARTBEAT, PROCESS_FAILED, AGGREGATE | next successful run or verify | the backup job / run record |
| **CMS #1083** | Its Data Health rules (D10) ship as **monitors** through `QAlertTypeMetaData.withDefaultMonitor` (TABLE_QUERY for filter rules, CODE for SQL rules), group `content`; plus `cms.publish.failed`, `cms.revalidation.failing` (the revalidation notifier's 3 attempts failed), `cms.schedule.missed` (a scheduled publish time passed without a publish) | TABLE_QUERY, CODE, push | MONITOR (the next clean check) or the next success | the content record(s) |
| **Authentik #1065** | `identity.authentik.unreachable` (sign-in fell back to the mirror), `identity.mirror.reconcile-failed`, `identity.blueprint.reverted` (its reversion detector), `identity.superuser.changed-by-service-account` (CRITICAL; ceiling MINIMAL), `identity.service-tokens.unexpected`, `identity.approval.pending` (two-person approvals waiting; audience = the approvers) | push, ESB_EVENT on the webhook relay, HEARTBEAT on reconcile | the next success, or the approval being decided | the user/group/approval record |
| **Assistant #1054** | (a) **consumer:** its `DataIssueProvider` gets a `qbit-alerts` implementation, which is already permission-filtered (§7.2), with the read tools `list_alerts` and `explain_alert`; acknowledge or resolve from the assistant goes through its approval card like any write. (b) **producer:** `assistant.provider.failing`, `assistant.budget.near`, `assistant.eval.regressed`. (c) The assistant can *draft* a monitor as a proposal. It never sends notifications itself. | push | | the assistant turn / eval record |
| **ESB** (shipped) | `esb.dead-letters.present` (per trigger), `esb.publish.failures`, `esb.broker.unreachable`, `esb.trigger.no-consumers` | CODE over `EsbStats` and the management adapter, as a small optional adapter in `qqq-esb` | MONITOR | the ESB app (its home stays the owner) |
| **Quick Search** | `search.validation.failed` (per index, from `quickSearchValidationRun` FAIL/ERROR), `search.validation.missed` (HEARTBEAT on `quickSearchValidateIndex`), `search.index.failed-events` | push from `ValidateIndexStep`; HEARTBEAT; TABLE_QUERY on the failed-event table | the next PASS | the validation run record |
| **Any process** (core) | `process.<name>.failed`, `process.<name>.missed` via `AlertProcessMetaData` | PROCESS_FAILED, HEARTBEAT | the next success | `qbit-standard-process-trace` rows when installed |

**Relationship to the Prometheus rules in #1088 and #1084.** Those specs route app conditions through PrometheusRules and Alertmanager because nothing else existed. Once `qbit-alerts` is installed, the recommendation (Q8) is:
- **App conditions** (regressions, purge failures, budgets) go through the Alerts qbit only, so people are not told twice.
- **Liveness and infrastructure** conditions stay in Prometheus: the backend is down, the alert evaluator is stale, the origin guard is down. Alerts cannot report the death of the process they run in.

---

## 10. Migration

### 10.1 Existing features become producers

| Feature (first host) | Today | Becomes |
|---|---|---|
| **Data Health** (~140 rules: `FilterCheck`, `SqlCheck`, `JsonReferenceCheck`, `ParentCycleCheck`, `InvalidRegexCheck`, `EsbCounterCheck`, Quartz rules; computed on demand as the system user, cached 5 min per replica, no storage, no ack) | A rule list in host code | **Step 1 (adapter):** `DataHealthMonitorProvider` registers each rule as an alert type `datahealth.<ruleId>` (group = its area, severity mapped, `whyItMatters` → description, `howToFix` → how-to-fix) with a CODE monitor that calls the existing check. The drill-down filter becomes the link, and the sample ids become record refs. **Step 2 (native):** `FilterCheck` rules become TABLE_QUERY monitors (plain data an admin can edit). The other check kinds stay CODE. **Step 3:** the 5-minute in-memory cache and the per-area widgets are retired; the evaluated state lives in `alerts_alert`. The CMS rules then ship inside `qbit-cms` as seeded monitors (CMS D10's rule-provider interface *is* this mechanism), so no `qbit-data-health` is needed. |
| **ESB health views** (the qqq-esb overview, the host's broker probes, two `EsbCounterCheck` rules) | Live widgets | The ESB app keeps its home (the owner of ESB data). The ESB alert adapter (§9) raises dead letters, publish failures and broker reachability, and the host's ESB rules are dropped in favor of it. |
| **Quick Search validation runs** (`quickSearchValidationRun` rows, scheduled daily) | Stored runs, no notification | `ValidateIndexStep` raises or resolves `search.validation.failed`, plus a HEARTBEAT. The runs table stays the evidence. |
| **Admin home "Needs Attention"** (the top 5 Data Health problems plus content inventory lines) and the **Health home `healthAlerts`** subsystem pills | Host widgets that query Data Health directly | Become link-only summaries through `AlertSummaryLoader` (§6.5). The content-inventory lines ("drafts", "going live this week") are *work lists*, not exceptions, so they stay as they are. |

### 10.2 How the first host (kof22) adopts it

Each step is reversible:
1. Upgrade to the QQQ 4.1.x release with the SPI (P0). Nothing changes in behavior: the logging sink is used.
2. Add `qbit-alerts`, add its changelog, map the host's admin role to `alerts.*`, and configure the scheduler, email (the host's existing SMTP relay through a QQQ `EmailMessagingProviderMetaData`) and the bootstrap subscription. Slack is **not** configured.
3. Turn on the Data Health adapter (step 1 above). Compare its open alerts with the old Data Health screen for a week, then switch "Needs Attention" and the Health pills to the summary loader.
4. Enable the ESB and Quick Search adapters. Then the CDN, Storage, web-performance and backups producers start reporting as each of those qbits lands, with no further host work.
5. Optional: Alertmanager ingest (so infrastructure alerts are visible and can be acknowledged in one place, with notifications off), and the Alertmanager sink or Slack channel if the owner wants app alerts in the same place as infrastructure alerts.

The host-specific details (role names, relay host, Alertmanager receiver config, ExternalSecrets) belong in the host's private repositories.

### 10.3 Future qbits

The checklist for a new qbit is:
- declare `QAlertTypeMetaData` for each exceptional condition, with external-safe titles and a subject table
- call `QAlerts.raise` / `resolve`, or ship a seeded monitor
- add `AlertProcessMetaData` to its scheduled processes
- keep its own home as the owner, and link to the Alerts list filtered by its group

This goes into the qbit template docs.

---

## 11. Packaging, phases and dependency order

### 11.1 Packaging

| Option | Shape | For | Against |
|---|---|---|---|
| **A (recommended)** | SPI in `qqq-backend-core` 4.1.x + new repo `QRun-IO/qbit-alerts` (`com.kingsrook.qbits.alerts`, Apache-2.0, `QBitMetaDataProducer<AlertsQBitConfig>` like qbit-quick-search) + Next UI bell/panel | Producers need no extra dependency, and the qbit is optional | A small core API to keep stable |
| B | A separate `qbit-alerts-api` jar for the SPI | Core untouched | Every producer adds a dependency and a version to align. The CDN and storage specs would still need reflection to stay optional. |
| C | Alerts inside an existing module (e.g. `qqq-middleware-health`) | One fewer repo | Health is for Kubernetes probes. It would mix concerns and still need tables and UI. |
| D | Build in the first host, extract later | Fastest demo | Repeats the extraction work the owner has already paid for with analytics and Data Health |

### 11.2 Phases

| Phase | Scope | Effort |
|---|---|---|
| **P0: core SPI** (QQQ 4.1.x) | `QAlerts`, `AlertSignal` and friends, `AlertSinkInterface`, `LoggingAlertSink`, `QAlertTypeMetaData` + `AlertsInstanceMetaData` + validation, `AlertProcessMetaData`, after-commit deferral, `RecordAccessHelper.visibleKeys`, record panel placement (§3.4), `topBarItems`. Docs page `docs/misc/Alerts.adoc`. | 1.5 wk |
| **P1: qbit core** | Repo, tables, changelog, type sync, state machine (dedup, reopen, flapping, auto-resolve), sink, permissions and visibility (§7.2), router, delivery worker, in-app inbox, email and email-list channels, notification log, alert actions, the Alerts home, `AlertSummaryLoader` | 4 wk |
| **P2: monitors** | Evaluator with claims and timeouts; TABLE_QUERY, AGGREGATE, HEARTBEAT, PROCESS_FAILED, CODE; seeded monitors; Run check now; mutes and maintenance windows | 2.5 wk |
| **P3: Next UI** | Bell, record "Open alerts" panel renderer, widget polish, six-width screenshots, a Playwright acceptance test | 1.5 wk |
| **P4: external channels** | Generic webhook, Slack (webhook), Teams (Workflows), PagerDuty, Alertmanager sink and ingest, Micrometer metrics, digests, quiet hours, reminders, redactor corpus | 2.5 wk |
| **P5: first-host migration** | Data Health adapter, then native TABLE_QUERY conversion; ESB and Quick Search adapters; Needs Attention and Health summaries. Other qbits' producers land with those qbits. | 2 wk |
| **P6: later** | Escalation policies and steps; PagerDuty ack-back; ESB_EVENT monitors; Slack bot-token threading; SSE for the bell; SMS through a channel qbit | 2–3 wk |

The total for P0–P5 is about **14 engineer-weeks**. P6 is optional.

### 11.3 Dependency order

```
QQQ 4.1.x core SPI (P0) ──┬──► qbit-alerts P1–P4 ──► first-host migration (P5)
                          │
                          ├──► qbit-cdn, qbit-object-storage, qbit-web-performance, qbit-backups,
                          │    qbit-cms, qbit-authentik, qqq-esb adapter, qbit-quick-search
                          │    (each declares types and calls QAlerts; works with no qbit-alerts installed)
                          │
Next UI (P3) ◄────────────┘ (bell + record panel; both hidden when the qbit is absent)
```

- **P0 goes first and is small**, so producers code against the real API from day one. CDN P1, storage P3 and webperf can all land before `qbit-alerts` exists.
- **No producer may require `qbit-alerts`.** Each must keep its conditions visible on its own home and in its tables (they already do). Alerts add notification and one place to triage, not correctness.
- If a producer ships before P0 is released, it uses a 20-line local shim with the same method names, removed on upgrade. This replaces the reflection detection sketched in the CDN and storage specs.

---

## 12. Testing

- **Core SPI:** no-op behavior with no sink; after-commit deferral (nothing on rollback); never throws; validator cases; lifecycle listener → `AlertProcessMetaData`.
- **State machine:** property tests over random raise/resolve/ack/snooze sequences. Invariants:
  - at most one open alert per key
  - every transition has an event
  - reopen within the window, and a new alert after it
  - flapping on and off
  - `forMinutes`
  - two simulated replicas racing on one key
- **Monitors:** each kind against H2 and PostgreSQL (Testcontainers); claim fairness across two evaluators; timeout; CHECK_FAILED meta-alert; the CODE full-state diff.
- **Visibility:**
  - a user without READ on the subject table sees no alert, no count, no bell item and no notification
  - record locks through mirrored columns
  - render-time re-check hides individual refs
  - the visible count equals the list count for the same user
- **Routing:** each suppression reason; quiet hours with CRITICAL bypass; digests; rate limits; channel health transitions; the loop guard on the Alertmanager sink and ingest.
- **Channels:** contract tests against WireMock for Slack, Teams, PagerDuty, Alertmanager and webhooks (HMAC verified); GreenMail for SMTP (threading headers, footer link); permanent-versus-retryable classification.
- **Redaction:** a corpus of PII and secret strings must never appear in any rendered external message at any detail level.
- **UI:** `LandingPagesTest`-style "no widget on two apps", readability guard, six widths (360–3200 px), bell keyboard and screen-reader checks.

---

## 13. Owner questions (with recommendations)

1. **Name and shape.** Repo `QRun-IO/qbit-alerts`, package `com.kingsrook.qbits.alerts`, table prefix `alerts_`, app label "Alerts"?
   *Recommendation:* yes, with the table prefix `alerts_` (was `alert`), per the owner naming rule of 2026-10-12 (§4): tables are lowercase snake_case and prefixed with the qbit's full name. "Manage by exception" is the principle, and "Alerts" is the label people understand. Creating the public repo still needs your explicit go-ahead.
2. **Where the producer SPI lives.** In `qqq-backend-core` 4.1.x (option A), or in a separate `qbit-alerts-api` jar (B)?
   *Recommendation:* A. It is about 10 small classes with no dependencies, and it is what makes every other qbit's integration optional without reflection.
3. **Data Health's future.** Absorb its framework into `qbit-alerts` as monitor kinds, or build a separate `qbit-data-health` that produces alerts?
   *Recommendation:* absorb. A Data Health rule *is* a monitor. CMS D10's "rule-provider interface" becomes seeded monitors, and the assistant's `DataIssueProvider` reads alerts. That makes one engine, one place to triage and one permission model.
4. **Top-level app or Admin sub-app?**
   *Recommendation:* top-level "Alerts", because every user with `alerts.view` has "my alerts", an inbox and subscriptions. In the first host, **retire Admin → Health as a separate dashboard**: its subsystem pills become the Alerts home's "By group" view, and Data Health's per-area widgets go away. Admin → ESB, CDN, Storage and the rest stay as the owners of their own data.
5. **Default channels.**
   *Recommendation:* in-app and email only. Slack, Teams, PagerDuty, webhooks and Alertmanager are opt-in channels configured by an admin. Nothing ships pre-wired to Slack.
6. **Severity scale.**
   *Recommendation:* four levels, INFO / WARNING / ERROR / CRITICAL, with the mappings in §3.1. INFO is never sent externally by default.
7. **Record panel when a record has no alerts.**
   *Recommendation:* hidden when nothing is open and nothing was resolved in 30 days. Otherwise most records would carry an empty box.
8. **Prometheus and Alertmanager.** How should it relate to the existing Alertmanager routes?
   *Recommendation:* (a) ingest Alertmanager into the Alerts home with QQQ notifications **off** for those types, for one triage view; (b) once `qbit-alerts` is live, move *app* conditions in #1084 and #1088 off PrometheusRules so nobody is told twice; (c) keep liveness and infrastructure in Prometheus, plus one rule for "alert evaluator stale"; (d) offer the Alertmanager **sink** only if you want app alerts to follow the existing routes instead of QQQ subscriptions.
9. **Record-level visibility for multi-tenant hosts.** Mirrored lock columns, or a render-time check only?
   *Recommendation:* mirrored lock columns for the key types listed in config, plus the render-time re-check. This keeps counts and paging correct, and single-tenant hosts configure nothing.
10. **External detail level.**
    *Recommendation:* MINIMAL by default for every external channel (title, severity, group, age, link). STANDARD is per-channel opt-in, and FULL needs `alerts.admin` and shows a warning. Producers must write titles without personal data. This is a review checklist item for every producer.
11. **Teams and audiences.**
    *Recommendation:* the qbit's own `alerts_team` table (MANUAL members) in P1. ROLE and IDP_GROUP resolvers (user-role-permissions, the Authentik mirror) come in P4, so "notify the publishers group" tracks group membership automatically.
12. **Bell transport.**
    *Recommendation:* polling every 60 s while the tab is visible in v1. Server-sent events are a P6 option if the latency matters.
13. **Escalation and on-call.**
    *Recommendation:* defer escalation policies to P6, and don't build rotations. For real paging, connect PagerDuty (or Jira Service Management through a webhook). Skip Opsgenie: support ends on 2027-04-05.
14. **Retention.**
    *Recommendation:* alerts, record refs and timelines kept forever (small volumes, and the history is valuable for "has this happened before?"). The notification log is kept 365 days and inbox items 90 days after they are read.
15. **Who may create monitors.**
    *Recommendation:* `alerts.admin` only. `alerts.manage` can acknowledge, snooze, resolve, assign and mute. Any `alerts.view` user can subscribe themselves.
16. **Muting semantics.**
    *Recommendation:* the default mute *silences notifications only*, so alerts are still recorded and shown with a "muted" badge. Suppressing the alert itself (SUPPRESS_ALERTS) is available, but only for maintenance windows created by an admin.
17. **Alert lifecycle on the ESB.**
    *Recommendation:* supported, and off by default. Turn it on when a second system needs to react to alerts.
18. **Quiet hours and digests.**
    *Recommendation:* in P4. Quiet hours are off by default, and CRITICAL bypasses them. Digests are per subscription (hourly or daily at the user's `digestHour`).
19. **Generic webhook channel.** Delegate to `qbit-webhooks` or build a small sender?
    *Recommendation:* build the small sender (about 1 day) and sign with the shared format from the webhooks assessment (#1105 §9, signer in core per its D2), so receivers verify both the same way. (An earlier draft said qbit-webhooks already had a signing scheme; it does not.) That avoids making qbit-webhooks a dependency.
20. **Phasing.**
    *Recommendation:* approve P0 now (it unblocks CDN, Storage, web performance and backups), then P1–P3 as one milestone ("alerts you can see and triage, with email"). P4 is next, and the first-host migration (P5) follows.

---

## Appendix A: Existing code this design builds on

All of these are on `QRun-IO/qqq` `develop` unless noted.

| Area | Existing piece | Used for |
|---|---|---|
| After-commit | `QBackendTransaction.addAfterCommitCallback` | Transaction-aware `raise` |
| Listeners | `RecordChangeListenerInterface`, `ProcessLifecycleListenerInterface`, `QRuntimeServiceInterface` | PROCESS_FAILED, heartbeats, the sink drain thread |
| Scheduler | `QScheduleMetaData`, Quartz/simple schedulers, schedule-after-start (#1044) | Evaluate, deliver, digest and purge processes |
| ESB | `qqq-esb`: `EsbTrigger`, `EsbPublishAction`, CloudEvents `EsbEvent`, `EsbStats` | ESB_EVENT monitors, lifecycle publishing, the ESB alert adapter |
| Permissions | `QPermissionRules`, `PermissionsHelper`, `RecordSecurityLock`, `CorePermissionEnforcer.runTrusted`, #1061/#1075 enforcement | §7 |
| Messaging | `QMessagingProviderMetaData`, `EmailMessagingProviderMetaData`, `SESMessagingProviderMetaData`, `SendMessageAction` | Email channels |
| Secrets | `QMetaDataVariableInterpreter`, `SecretsManagerUtils` | `secretRef` resolution |
| Supplemental metadata | `QSupplementalInstanceMetaData` / `QSupplementalProcessMetaData` (as `EsbInstanceMetaData`, `DeveloperPanelsMetaData`) | `AlertsInstanceMetaData`, `AlertProcessMetaData` |
| Developer panels (#1076) | `DeveloperPanelProviderInterface`, `DeveloperPanelsAction`, `/developer/panels` | Generalized to record panels (§3.4) |
| Home app (#1059) | `QInstance.homeAppName` | The landing page that shows the summary |
| Widgets | `WidgetType` TABLE, HTML, MULTI_STATISTICS, LINE_CHART | The Alerts home |
| Streamed ETL | `StreamedETLWithFrontendProcess` | Every action |
| Qbit pattern | `QBitMetaDataProducer`, `QBitConfig`, qbit-quick-search's runtime service and listeners | Packaging |
| qbit-webhooks | Send log and health model (HEALTHY / PROBATION / UNHEALTHY). It has no signing; the signature format comes from the #1105 assessment §9 | Channel health |
| Next UI | `Header.tsx` placeholder bell (`button-notifications`) | `AlertsBell` |

## Appendix B: External references (checked 2026-10-12)

- Prometheus Alertmanager: webhook receiver payload (version 4) and `POST /api/v2/alerts`.
- PagerDuty Events API v2 (`trigger` / `acknowledge` / `resolve`, `dedup_key`).
- Slack Incoming Webhooks and `chat.postMessage` / `chat.update` (Block Kit).
- Microsoft Teams: Workflows webhooks replace the retired Office 365 connectors.
- Atlassian: Opsgenie end of sale 2025-06-04, end of support 2027-04-05 (atlassian.com/software/opsgenie/migration).
- CloudEvents 1.0 (as used by the QQQ ESB).
