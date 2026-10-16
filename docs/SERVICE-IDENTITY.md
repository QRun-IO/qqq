# Service identity for background work

Status: APPROVED by the owner 2026-10-16 (all recommendations in section 9).

## 1. Problem

QQQ runs work with no signed-in user: scheduled reports, alert routing and
delivery, data-analysis schedules and watches, Authentik maintenance (run-once
policy changes), the health runner, release-notes detection, quick-search
indexing, durable-job bookkeeping and many qbit internals. Today that work runs
under one of three session shapes:

| Shape | What it is | Under OAuth2 today |
|---|---|---|
| `QSystemUserSession` | `hasPermission()` is always true and it holds every all-access security key | Accepted (`isSessionValid` short-circuits). Works, but it is an unbounded, unattributed super-session. |
| `createAutomatedSessionForUser(userId)` | A `QSession` carrying only a user id, then the customizer's `customizeAutomatedSessionForUser` | Rejected: no stored access token, so `isSessionValid` is false. Scheduled reports, `DeliverySessions.forOwner`, alert audience checks, data-analysis per-recipient runs, Slack owner sessions and file-gateway users all fail. |
| Hand-made `QSession` (`AuthentikMaintenance.MaintenanceSession`, data-analysis `data-analysis-scheduler`) | A plain session with a self-assigned name and permissions | Rejected (`Invalid session in request`). Seen in kof22 prod: `data_analysis_run_schedules` fails every run; the `identityAdminsFullAccess` run-once fails at its audit insert. |

Making token-less sessions valid under OAuth2 was refused (it would let any code
mint authority the identity provider never granted). Job grants (RC.14) cover
work a signed-in user launched; they do not cover work nobody launched.

## 2. Goals and non-goals

Goals:
- Background work runs as a named **service principal** whose authority is
  granted explicitly by the owner, is least-privilege per role, is revocable, and
  is enforced by the normal permission and record-lock checks.
- Under OAuth2 the principal holds a **real access token** from the identity
  provider and its session is built by the same bearer path as any API caller
  (RC.15 `OAuth2BearerTokenVerifier` + the customizer). No new way for OAuth2 to
  accept a session.
- Work done "for" a user (a scheduled report, an alert delivered to a recipient)
  runs with **that user's current access**, attributed to the service principal
  on behalf of the user.
- Single-node and non-Authentik deployments (TABLE_BASED, Mock, anonymous)
  keep working through a local principal with explicit permissions that OAuth2
  deployments never accept.
- HIPAA-friendly audit: every row a principal writes names the principal (and
  the user it acted for); secured-data/PHI is not visible to a principal unless
  explicitly granted.
- Works in cluster mode: tokens cached per node, refreshed before expiry, never
  shared over ClusterBus.

Non-goals (phase 1):
- Removing `QSystemUserSession`. It stays for framework plumbing that touches no
  business data on anyone's behalf (section 6.3). Business work moves off it.
- Per-tenant principals for multi-tenant hosts.

## 3. Findings that shape the design

1. **The bearer path already accepts machine tokens.** `createSessionFromBearerToken`
   verifies issuer, signature, audience, lifetime and token type, falls back to
   `preferred_username` for client_credentials tokens without `sub`, runs the
   customizer (`AuthentikPolicyClaimMapper` turns the token's policy claim into
   permissions) and marks the session as a bearer session, which
   `isSessionValid` accepts until `exp`. A service session is therefore just a
   bearer session for a token QQQ obtained itself.
2. **Authentik M2M uses username + app-password, not client_id/secret alone.**
   With `grant_type=client_credentials`, `client_id=<the QQQ provider>`,
   `username=<service account>`, `password=<app-password token>`, Authentik
   issues a signed JWT for that user, audience = the QQQ client id, with claims
   from the requested scope mappings (so the same policy claim). Service accounts
   are ordinary Authentik users of type `service_account`, can be in groups,
   can have several app-password tokens with expiries, and can be deactivated.
   Application policy bindings are evaluated for this grant, so the app can
   restrict client_credentials to a `qqq-service-accounts` group.
   (Passing only the provider's client secret makes Authentik auto-create one
   `ak-<provider>-client_credentials` account per provider: one identity for
   everything, which is not least-privilege. Not used.)
3. **The kof22 token-broker 400s are a probe bug, not an outage.** Authentik logs
   `Invalid grant_type for provider` / `grant_type: client_credentials` /
   `provider: qqq-kof22-token-broker` every 5 minutes. The broker provider is
   configured for token exchange (RFC 8693), which is what
   `AuthentikTokenBroker.exchange` uses for Identity writes; the health check
   `token_broker` probes `client_credentials`, which that provider does not
   allow. Authentik 2026.8 checks the grant type before the client secret
   (`token/base.py`), so the probe should send the grant the broker actually
   uses (token exchange) with a dummy subject: `invalid_client` means bad
   credentials (DOWN), and any later refusal means the credentials passed (UP).
   Introspection cannot be used, because it answers `{"active":false}` either way. The broker is not the service-identity mechanism: it
   exchanges a *user's* token to call the Authentik API as that user.
4. **Service sessions can call the Authentik API as themselves.** Identity writes
   exchange the current session's access token (`OAuth2SubjectTokenSource`). For
   a service bearer session that token is the service account's own JWT, so
   Authentik RBAC on the service account (a role in the blueprint) decides what
   it may change in Authentik, and Authentik's event log names it.
5. **Scheduled processes** run under the scheduler's session supplier (kof22:
   `QSystemUserSession`); `ScheduledJobs.ownerSession` needs
   `scheduledJobsUserId` plus `createAutomatedSessionForUser`, which fails under
   OAuth2.

## 4. Design

### 4.1 Metadata: service principals

```java
qInstance.addServicePrincipal(new QServicePrincipalMetaData()
   .withName("scheduler")                     // stable id, used in audit as service:scheduler
   .withLabel("Scheduler")
   .withDescription("Runs scheduled reports and scheduled processes")
   .withMayActOnBehalfOfUsers(true)           // only principals that need it
   .withLocalPermissions(Set.of(...)));       // used ONLY by the local provider (4.4)
```

Qbits declare the principals they need (alerts declares `alerts`,
data-analysis declares `data-analysis`, quick-search declares `search-indexer`,
release-notes declares `release-notes`, qbit-authentik declares `maintenance`);
hosts may rename/merge them through qbit config. Validation fails startup if a
process or qbit names an undeclared principal.

### 4.2 API

```java
ServiceIdentities.run("scheduler", () -> ...);                 // as the principal
ServiceIdentities.runOnBehalfOf("scheduler", userId, () -> ...);// as user, attributed to principal
QSession s = ServiceIdentities.session("alerts");               // for code that manages its own context
```

`QProcessMetaData.withServicePrincipal("scheduler")`: the scheduler runs a
scheduled process inside `ServiceIdentities.run(...)`; scheduled durable jobs
are owned by the principal (job user id `service:<name>`), and a worker
re-mints the principal's session at every claim (no job grant needed, current
authority applies, a revoked principal fails the job cleanly). The per-claim
QContext, including the `JobLogWriter` context capture, gets the minted
session and never `QSystemUserSession` (the same bug qbit-assistant #59 fixed).
Job grants (RC.14) are unchanged: they still gate the durable jobs a user
launches, and launches from token sessions stay refused (#1235). Only the
`ScheduledJobs.ownerSession` / `scheduledJobsUserId` path is replaced. A
migration re-owns existing scheduled-job rows (pending, claimed, or running)
from `scheduledJobsUserId` to the process's principal, so their in-flight
claims do not fail after the upgrade.

Resolution goes through an SPI chosen by the instance's authentication module:

```java
public interface QServiceIdentityProviderInterface
{
   QSession sessionFor(QInstance instance, QServicePrincipalMetaData principal) throws QAuthenticationException;
   QSession sessionOnBehalfOf(QInstance instance, QSession serviceSession, String userId) throws QAuthenticationException;
}
```

### 4.3 OAuth2 provider (client_credentials)

- Credentials per principal come from configuration (env/secret), never the DB
  or filesystem: `QQQ_SERVICE_<NAME>_USERNAME`, `QQQ_SERVICE_<NAME>_SECRET`
  (Authentik: service account username + app-password token). Missing
  credentials = the principal is unavailable: its work fails loudly with an
  alert and a health check, it never falls back to the system user.
- Token acquisition: `POST <token endpoint>` with `grant_type=client_credentials`,
  `client_id=<app clientId>`, `username`, `password`, `scope=<the app's scopes
  incl. the policy scope>`. The JWT then goes through the **existing**
  `createSession(Authorization: Bearer ...)` path: verifier + customizer +
  bearer session. QQQ adds no acceptance rule.
- Cache: per node, per principal, in memory only (`ClusterCaches.nodeLocal`);
  single-flight acquisition; refreshed when within `max(60s, 20% of lifetime)`
  of `exp`; a session is never handed out with less than 60 s left. Long work
  calls `ServiceIdentities.run` per unit (per report, per delivery, per chunk),
  so it always holds a fresh session. Nothing about tokens crosses ClusterBus;
  each node authenticates for itself. The cache is registered with
  `ClusterCaches` (the ClusterCacheAudit test stays green). Revocation and
  rotation reach every node: a cached token is kept for at most 5 minutes
  whatever its `exp` says, and a key-only invalidate (`principal name`, no token
  material) on `qqq.cluster.control` drops it everywhere at once (sent by the
  Identity Deactivate action and on any node's `invalid_grant`).
- Verifier constraints (RC.15, unchanged): the deployment needs
  `bearerTokensEnabled=true`; `iss` must be the configured issuer (baseUrl or
  externalBaseUrl); `aud` must be the clientId (or additionalBearerAudiences);
  the token must be an access token (no `nonce`/`at_hash`; has `scope`, `azp`,
  `client_id` or typ `at+jwt`); exp - iat within `bearerTokenMaxLifetimeSeconds`
  (default 1 day; the service grant uses 5 minutes). Startup validation fails if
  principals are declared but bearer tokens are disabled.
- Service tokens are handed to `createSession` in-process and never sent over
  HTTP. So route gates such as kof22's `PublisherTokenRouteProvider` (WB#276:
  a non-credential bearer on /qqq/v1 gets 401) never see them, and they get **no**
  allow entry there. A service token presented over HTTP stays refused.
- Failures: `invalid_grant`/`invalid_client` (revoked, deactivated, rotated) ->
  cached negative result with backoff (30 s doubling to 5 min), alert
  `service_identity.unavailable` (dedup per principal), health check DOWN.
  Revocation takes effect at the next mint, i.e. within the provider's access
  token lifetime (recommend 5 minutes for the service grant).
- Rotation: the secret is replaced in Infisical (Praetor `infisical-config`,
  targeted apply), ESO refreshes the env, pods roll; Authentik keeps the old
  token valid until its expiry so a rollout has no gap. Health check warns 14
  days before the app-password token expires (read through the Identity API).

### 4.4 Local provider (TABLE_BASED, Mock, anonymous, no auth module)

- Mints a `QServiceSession` (a new subclass of `QSession`, **not**
  `QSystemUserSession`): permissions = the principal's `localPermissions`, plus
  any grants/revocations in the standard `service_principal_grant` table
  (DB-defined in code, editable in the admin UI by holders of
  `service_principals.manage`); security keys = only those explicitly listed;
  no all-access keys unless listed.
- Validity: `TableBasedAuthenticationModule`, `MockAuthenticationModule` and
  `FullyAnonymousAuthenticationModule` accept a `QServiceSession` only when it
  was minted by the local provider in this JVM (registered by identity in a
  weak set, unforgeable from requests: it is never serialized to the session
  store or a cookie). **`OAuth2AuthenticationModule` and `Auth0AuthenticationModule`
  never accept a `QServiceSession`**: on those modules the only service session
  is a bearer session from 4.3.

### 4.5 On behalf of a user

Used where the product promise is "you receive what you could see":
scheduled reports, output deliveries, alert audience checks and deliveries,
data-analysis per-recipient runs, Slack owner actions, file-gateway workers.

- Requires the principal to be declared `mayActOnBehalfOfUsers` (reviewed
  code). Implementation note: a separate `qqq.service.actOnBehalf` permission
  was dropped, because core has no registry for named permissions and Authentik
  policies only grant catalogued names. Because of the intersection (D2), the
  principal must hold every permission the work uses anyway, so delegation can
  only narrow access, and revoking the principal's grants in Authentik revokes
  the delegated work too.
- The user's current access comes from the auth module's existing
  `createAutomatedSessionForUser` plus the customizer, called under the
  principal's session:
  - qbit-authentik: reads the user and its groups with the service session
    (Authentik API, as the service account), refuses inactive/missing users, and
    computes permissions with the same `EffectivePermissionResolver` the
    Identity screens use; security keys via the host customizer's
    `customizeAutomatedSessionForUser`.
  - TABLE_BASED / local: the existing `createAutomatedSessionForUser` path.
- The delegated session (`QDelegatedSession`) has user = the user, permissions =
  **user's current permissions ∩ the principal's permissions** (decision D2),
  and holds a reference to its parent service session. Validity: the parent is
  valid (an unexpired bearer session under OAuth2) and the user still resolves.
  It is never serialized and never reachable from a request.
- Audit: `auditUser` = the user's name with `via service:<principal>`; the
  audit row's client channel is `service` (see 4.6).

### 4.6 Attribution and audit

- Service session user: `idReference = service:<name>` (from the token's
  `preferred_username` mapped through config to the principal),
  `fullName = "<Label> (service)"`, client channel `service`, session values
  `qqq.service.principal=<name>` and, for delegation, `qqq.service.onBehalfOf=<userId>`.
- `AuditAction` records `service:<name>` as the user, or
  `<user> via service:<name>` for delegated writes; durable-job audits keep the
  job channel. Logs carry `servicePrincipal` and `onBehalfOf`.
- Secured-data access logs (qbit-secured-data) record the principal and the
  on-behalf-of user, so HIPAA access reports show who saw PHI and through what.

### 4.7 Identity UI (qbit-authentik, via /qqq/v1)

A **Service accounts** table in the Identity app (standard QQQ table over the
Authentik API, filtered to `type=service_account` users in
`qqq-service-accounts`): principal name, Authentik user, active, groups,
effective permissions (same resolver as users), app-password token expiry, last
authentication. Record actions (standard ETL review/result): Deactivate,
Reactivate, Edit policy (the existing policy editor on the account's group).
Creating accounts and issuing secrets stays declarative (blueprint + Praetor) for
kof22 (decision D4). UI API: one new table, no new endpoints; minor bump of the
qbit's UI API extension.

## 5. Threat model

| Threat | Mitigation |
|---|---|
| Code mints authority with a hand-made session | OAuth2/Auth0 accept only provider-signed tokens; `QServiceSession` and `QDelegatedSession` are never accepted by OAuth2 except the delegated one whose parent is a valid bearer session; ArchUnit rule: no `new QSession()`/`QSystemUserSession` in business packages without an allow-list entry. |
| Stolen service secret | Secret only in Infisical + pod env; per-principal secrets (blast radius = that role); Authentik application policy limits client_credentials to `qqq-service-accounts`; tokens short-lived; deactivate the account in Authentik to revoke; Authentik logs every mint. |
| Over-privileged principal | Each principal is its own Authentik account with its own group policy, granted explicitly by the owner; default policy is empty; no all-access keys; PHI only via an explicit secured-data grant. |
| Confused deputy (principal reads data for a user who should not see it) | Delegation intersects with the user's current access, resolved fresh per unit of work; inactive or removed users fail closed. |
| Escalation through Identity writes | Service accounts call Authentik as themselves (token exchange of their own token); Authentik RBAC on the account plus qbit-authentik's guard (protected groups, second-person approval) still apply; a service account cannot edit its own group's policy (guard rule added). |
| Token leakage across nodes | Tokens never leave the node's memory; not in session store, ClusterBus, logs (redacted), or DB. |
| Provider outage | Principal work fails and retries per durable-job policy; no fallback to system user; alert + health DOWN. Plumbing on `QSystemUserSession` is unaffected, so audits and job bookkeeping keep working. |
| Replay of a captured service token against the API | Same as any bearer token: audience-bound, short-lived; it carries only the principal's authority. |

## 6. Call-site migration

### 6.1 Business work: move to service principals (phase 1)

| Call site | Today | Becomes |
|---|---|---|
| core `RunScheduledReportExecuteStep.updateSessionForUser` | automated session for owner (fails under OAuth2) | `runOnBehalfOf("scheduler", owner)` |
| core `DeliverySessions.forOwner` | automated session for owner | `sessionOnBehalfOf("scheduler", owner)` |
| core `ScheduledJobs.ownerSession` / `scheduledJobsUserId` | automated session for a configured user | job owned by the process's principal; worker re-mints per claim |
| core scheduler supplier (kof22 `WebsiteSchedulerMetaData`) | `QSystemUserSession` | scheduler shell keeps system for bookkeeping; each process runs as its declared principal (default `scheduler`) |
| qbit-authentik `AuthentikMaintenance.applyPolicy` (`identityAdminsFullAccess` run-once) | self-made `MaintenanceSession` | `run("maintenance", ...)`; operator recorded as `service:maintenance` with the run-once name as reason prefix; `MaintenanceSession` deleted |
| qbit-authentik health `token_broker` | probes client_credentials (400) | introspection probe; plus a `service_identity` check per principal |
| qbit-alerts `AlertsSecurity.systemSession` (router, sink, monitors) | system + all alerts keys | `run("alerts", ...)` with the alerts keys granted to that principal |
| qbit-alerts `AlertRouter.automatedSession`, `AlertsAudienceResolver` | automated session per recipient | `sessionOnBehalfOf("alerts", recipient)` |
| qbit-data-analysis `ScheduleRunner.runDue`, `WatchEvaluator` | self-made `data-analysis-scheduler` session (fails in prod) | `run("data-analysis", ...)` |
| qbit-data-analysis `ScheduleRunner.session`, `WatchEvaluator` per owner/recipient | automated session | `sessionOnBehalfOf("data-analysis", user)` |
| qbit-data-analysis `EffectiveConfigResolver` | system | `run("data-analysis", ...)` |
| qbit-release-notes `ReleaseNotesRuntime.asSystem` | system | `run("release-notes", ...)` |
| qbit-quick-search `QuickSearchRuntime`, `IndexValidator`, `RecordReindexer` | system (all keys, may index PHI) | `run("search-indexer", ...)`; PHI fields not indexed unless granted (D5) |
| kof22 `RunOnceRunner` | system | each run-once declares its principal (`maintenance` for identity changes) |
| kof22 `DataHealthEvaluator` | system | `run("scheduler", ...)` (reads business tables) |

### 6.2 Phase 2 (same mechanism, after phase 1 is proven)

qbit-slack `SlackOwnerSession`, qbit-file-gateway `WorkerUserSession` /
`GatewaySession` (on behalf of), qbit-crm webhook, qbit-edi / easypost
`SystemWrites`, qbit-marketing `MarketingStore`, qbit-collaboration followers,
kof22 EDI and share-audience lookups.

### 6.3 Stays on `QSystemUserSession` (framework plumbing)

Durable-jobs kernel (`SystemScope`, `DurableJobsRuntime`, `JobLogWriter`),
`ClusterBus` handlers, `TableStateProvider`, session store, OAuth2/Auth0
internals (token/refresh tables, back-channel logout), `QInstance` overlays,
help content/bundles, health runner and probes, `HealthMetrics`, route
providers' static assets, ESB outbox relay, secured-data/WORM audit writers
(they record who did something; they must not depend on the IdP), qbit-jobs
purge, qbit-cluster, object-storage internals, sql-workbench auditor,
db-performance sampling, backups health. Each gets an allow-list entry with a
one-line reason; new uses fail the ArchUnit rule.

## 7. Configuration and kof22 rollout

- Authentik (cluster-gitops blueprint, `!Env` secrets): group
  `qqq-service-accounts`; service accounts `svc-kof22-scheduler`,
  `svc-kof22-alerts`, `svc-kof22-data-analysis`, `svc-kof22-maintenance`,
  `svc-kof22-release-notes`, `svc-kof22-search-indexer`, each in
  `qqq-service-accounts` and its own `service:kof22-<role>` group carrying its
  policy; app-password tokens with keys from env; the kof22 QQQ provider allows
  `client_credentials` with a 5-minute access-token validity for that grant; an
  application policy binding limits client_credentials to
  `qqq-service-accounts`. The maintenance account gets an Authentik role allowing
  group attribute changes (for the run-once).
- Praetor `terraform/infisical-config`: `random_password` per principal seeded
  write-only to `/kof22-website-service-identity/<role>` (read by both the
  Authentik worker for the blueprint and the backend ExternalSecret). PR, owner
  applies (targeted).
- Website-Backend: declare principals, map env, set processes' principals,
  migrate the kof22 call sites in 6.1.

## 8. Tests

Token issuance and refresh (stub token endpoint, near-expiry refresh,
single-flight, backoff on failure); revocation (invalid_grant -> fails closed,
alert, no system fallback); least privilege (principal without a permission is
denied; no all-access keys; PHI field hidden); delegation (intersection, inactive
user fails, parent expiry invalidates child); audit attribution (`service:x`,
`user via service:x`); OAuth2 never accepts `QServiceSession` or a hand-made
session; local provider on TABLE_BASED and with no auth module; cluster (two
nodes each mint their own token; nothing on ClusterBus); ArchUnit allow-list.

## 8a. Acceptance (verified in kof22 prod, with no user signed in)

1. The `identityAdminsFullAccess` run-once applies (or reports no change) to
   `kof22-admins`. Its audit row says `service:maintenance`, and Authentik's
   event log names `svc-kof22-maintenance`.
2. A scheduled report runs and is delivered with the owner's current access.
   The audit says `<owner> via service:scheduler`.
3. Alert routing and delivery run as `service:alerts`, and recipients only get
   what they can see.
4. `data_analysis_run_schedules` and watches run without `Invalid session in
   request`.
5. The `token_broker` health check is UP, Authentik logs no more 400s from it,
   and a per-principal `service_identity` check is UP.
6. Deactivating a service account in Authentik stops its work on every node
   within 5 minutes (immediately with the cluster invalidate), and raises an
   alert. Its work never falls back to the system user.

## 9. Owner decisions (signed off 2026-10-16: all recommendations)

- **D1 (decided: per role)** One Authentik service account per role (recommended; six for kof22) vs one per app.
- **D2 (decided: intersection)** On-behalf-of = user's current access **∩** the principal's policy (recommended, least privilege; the scheduler's policy must then cover what reports read) vs user's access alone.
- **D3 (decided: maintenance account holds full access for the run-once, then narrowed; owner: "do it")** Bootstrap of `kof22-admins` full access: give `svc-kof22-maintenance` the grants it must hand out (recommended: full access in its policy, narrowed after the run-once) vs move that policy into the Authentik blueprint and retire the run-once.
- **D4 (decided: declarative only)** Secrets: issued and rotated only declaratively (blueprint + Praetor/Infisical, recommended for kof22) vs also from the Identity UI (one-time display).
- **D5 (decided: no PHI unless granted)** Quick-search indexer: stop indexing secured-data/PHI fields unless the indexer principal is granted them (recommended; behavior change from today's system indexer).
- **D6 (decided: phase 1 = 6.1, allow-list for 6.3)** Scope: phase 1 = section 6.1 only; `QSystemUserSession` remains for plumbing in 6.3 behind an allow-list (recommended) vs migrating everything now.

## 10. Security review (2026-10-16) and resolutions

An independent review found the following issues; all are fixed in the
implementation PRs except where marked as accepted.

- **H1, fixed.** A scheduled report's `userId` was user-editable, so with the
  scheduler principal anyone who could edit a schedule could render reports
  with another user's access. The owner is now stamped from the session on
  insert, and it cannot be changed afterwards.
- **H2, fixed.** Any session whose id started with `service:` could own and
  launch principal jobs. The `service:` namespace is now refused for every
  session that is not a minted principal session (`ActionHelper`). Only a
  minted principal may launch a principal-owned job, and a principal-owned job
  that carries a user grant is refused.
- **H3, fixed in deployment.** Principal policies are seeded with least
  privilege, not full access; maintenance keeps full access for the D3
  run-once only. A principal cannot change service principal role groups.
- **M1, fixed.** Service tokens carry no access-token (`qqq_token`) claim, so
  kof22 refuses them over HTTP; they are used only in-process.
- **M2, accepted.** The provider's access-token lifetime is 1 hour. New work
  stops within 5 minutes of deactivation (the cache cap), and a leaked token
  is refused over HTTP (M1). A unit of work that is already running finishes.
- **M3, fixed.** With `QQQ_SERVICE_IDENTITIES_REQUIRED=true`, kof22 declares
  every principal: a missing credential makes the principal unavailable
  instead of falling back to its previous authority.
- **M4, fixed.** Service sessions bypass the session store, and every call
  gets a fresh object.
- **M5, accepted.** Delegation intersects permissions, and drops all-access
  keys the principal lacks. Ordinary record-lock keys are the user's, because
  the work is the user's view. Without the table permission, those keys give
  no access.
- **M6, fixed.** The key script only accepts 48-128 alphanumeric secrets.
- **M7, fixed.** Token requests use 5 s connect and 10 s read timeouts, and
  follow no redirects.
- **L1, fixed.** The token's `preferred_username` must match the configured
  account.
- **L6, fixed.** The raw-token accessor is package-private.
- **L4, L5, open (low).** The migration guard is per JVM, and local minted
  sessions never expire.

