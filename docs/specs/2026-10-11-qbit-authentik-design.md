# Authentik qbit: Design Spec

Status: Draft for review · 2026-10-11 · Author: James Maes (drafted with Claude)
Targets: QQQ 4.1.x, QQQ Next UI 1.0.x, Authentik 2026.8.x (researched against the live `auth.kof22.com` schema, `info.version: 2026.8.3`), new repo `QRun-IO/qbit-authentik`

## 0. Summary

`qbit-authentik` lets the admins of a QQQ application manage Authentik users, groups, memberships and permissions from inside the QQQ app. It does this without opening the Authentik admin UI, and without letting QQQ become a second, unaudited path into the identity provider.

The first consumer is the kof22 Admin (`KofTwentyTwo/Website-Backend`). It already signs in through Authentik with QQQ's OAuth2 module. Its "Web Users" area cannot show groups today because the backend has no Authentik API access. The second consumer is Me.Health Portal (MHP). MHP already has a hand-written `AuthentikAdminApiClient`, and this qbit is meant to replace it.

Seven design rules shape everything below:

1. **Ownership is split, and each piece has one owner.** GitOps blueprints own the *structure*: groups, roles, applications, providers, policies, and *which groups QQQ may manage*. The qbit owns *membership of delegated groups* and *user lifecycle*. The qbit refuses to edit anything a blueprint will overwrite (§4).
2. **Least privilege is enforced in Authentik, not only in QQQ.** The qbit uses a dedicated service account. Its role has global *view* permissions plus *object* permissions on the specific groups a blueprint delegates. If QQQ is compromised, it still cannot add anyone to `authentik Admins`, because Authentik itself denies the call (§5.1).
3. **The qbit checks permissions itself.** QQQ core actions do not enforce table or process permissions (QRun-IO/qqq#1061), and `FieldSecurityLock` is not enforced (QRun-IO/qqq#1062). Every qbit read and write passes through an `AuthentikGuard` that checks the QQQ session's permissions in the backend action layer. It does not rely on the middleware (§5.3).
4. **No self-elevation, and no grants beyond your own.** An operator can only grant group memberships they hold themselves, unless they have an explicit `grantAny` permission. Operators cannot change their own memberships. Protected accounts cannot be modified at all: break-glass admins, superusers and the qbit's own service account (§5.4).
5. **Credentials never pass through QQQ.** Password resets go out by Authentik email (`recovery_email`). QQQ never shows recovery links, never sets passwords and never displays token keys (§5.5).
6. **Every change is audited on both sides.** QQQ writes a local `authentikAdminAction` row: operator, reason, before and after, result. Authentik also gets a `custom_qqq_admin_action` event, so Authentik's own log names the human and not just the service account (§9.2).
7. **Reads go through the API, with a small mirror for joins.** Authentik screens read the API live. A small RDBMS mirror (users, memberships) feeds the joins and widgets that need it, such as Web Users ↔ `qqq_user_session`. The mirror is refreshed by Authentik webhook → ESB events, plus a periodic reconcile (§7).

---

## 1. Goals, non-goals, personas, scenarios

### 1.1 Goals

- One reusable qbit that any QQQ app with an Authentik IdP can turn on with configuration:
  - base URL
  - token reference
  - features enabled
  - read-only mode
  - delegated-group scope
- **See** who has access and why:
  - users, groups (with hierarchy), memberships, roles, applications and their policy bindings
  - sessions, MFA devices (metadata only), app passwords and tokens (metadata only)
  - invitations and the event log
  - who can open which app: `check_access`
- **Manage**, within guardrails:
  - group memberships (bulk)
  - invite, deactivate and reactivate users
  - send password-reset email, revoke sessions, reset MFA
  - create delegated groups
  - later: role assignment and service-account housekeeping
- **Coexist with GitOps.** Ownership of every object is visible (Blueprint / QQQ / Unmanaged), editing is blocked where a blueprint would revert it, and a drift report is provided.
- **Integrate with the host app.** Web Users shows Authentik groups, last login and lockout state. Admin-home widgets cover counts, recent failures, stale accounts and accounts without MFA. The qbit also provides a reusable "Authentik groups → QQQ permissions" customizer.

### 1.2 Non-goals (v1)

- **No editing of flows, stages, providers, applications, policies or brands.** Those are blueprint territory, and QQQ shows them read-only. A full Authentik admin clone is explicitly out of scope.
- **No impersonation.** Authentik impersonation is session-cookie based and needs a reason. It cannot work cleanly from a bearer-token client, and it is too risky to proxy.
- **No password setting, recovery-link display or token-key display** (§5.5).
- **No hard delete of users.** Use deactivate (open question Q3).
- **No Enterprise-only features** in v1: Lifecycle/offboarding, account lockdown, access requests, CSV export. Design hooks are noted where they would fit (Q15).
- **No generic multi-IdP SPI yet.** Names are neutral where cheap, and an SPI is deferred until a second IdP is real (Q10).
- **No Material Dashboard UI.**

### 1.3 Personas

| Persona | kof22 example | Needs |
|---|---|---|
| Identity admin | James (`kof22-admins`) | Full lifecycle, drift report, audit, protected-account safety |
| Delegated manager | A future editor-in-chief | Add or remove publishers and drafters only. Cannot touch admins. |
| Auditor / read-only admin | Any `kof22-admins` member in read-only mode | Who has access to what, recent auth failures, history of changes |
| Platform owner | Whoever runs cluster-gitops | Blueprints remain the source of truth for structure. QQQ edits stay visible and reversible. |

### 1.4 Scenarios (kof22)

**A. "Give Sam publishing rights."** The admin opens Users → Sam → *Add to groups* and picks `kof-website-publishers`.
- The preview shows Sam's groups before and after, plus the result of `check_access` for app `kof22-website-prod` after the change ("will be able to sign in: yes, via policy `kof22-require-admins-group`").
- The admin confirms and gives a reason. The qbit calls `POST /core/groups/{uuid}/add_user/` and audits on both sides. The mirror refreshes on the webhook.

**B. "Lock out a contractor now."** The admin opens Users → contractor → *Deactivate*.
- The preview lists the contractor's live sessions, app passwords and group memberships.
- The admin types the username to confirm. The qbit sends `PATCH is_active=false`, which in 2026.8 also deletes the user's sessions. If selected, it also revokes app passwords.
- The protected-account rule blocks this action against `james.maes` and the service account.

**C. "Who can get into the admin, and why?"** The *Access* tab shows the delegated users × apps matrix.
- Each cell shows the `check_access` result plus the binding path, for example "member of `kof-website-publishers` → policy `kof22-require-admins-group` passes".
- It also flags `kof-website-drafters`. The backend accepts drafters, but the app policy does not let them sign in, which is a real gap found during this research (§2.3).

**D. "Did anyone change the admin group outside Git?"** The *Drift* report compares live Authentik with the blueprint markers and the delegation list.
- It shows unmanaged objects that should be adopted into a blueprint. In kof22 today that includes `kof-website-publishers`, `kof-publisher-prod` and the `kof22-post-ios` provider, all of which were created in the UI.
- It also shows memberships a blueprint will revert on its next apply.

---

## 2. Research summary

### 2.1 Authentik 2026.8.3 API surface used

Facts come from the live OpenAPI schema at `https://auth.kof22.com/api/v3/schema/` (`info.version: 2026.8.3`, fetched without authentication), from source at tag `version/2026.8.3`, and from the docs (Appendix B). The base path is `/api/v3`. Every list endpoint takes `page`, `page_size`, `ordering` and `search`.

| Area | Endpoints | Notes that matter for the design |
|---|---|---|
| Users | `GET/POST /core/users/`, `GET/PUT/PATCH/DELETE /core/users/{id}/` (int pk) | Filters: `username`, `email`, `name`, `uuid`, `is_active`, `is_superuser`, `groups_by_name[]`, `groups_by_pk[]`, `roles_by_*[]`, `type[]`, `path`, `path_startswith`, `last_login__gt/__lt/__isnull`, `last_updated__gt`, `date_joined__*`, `attributes` (JSON), `include_groups`, `include_roles`. `search` also accepts a query language (`QLSearch`). `is_superuser` is derived and read-only. Types: `internal`, `external`, `service_account`, `internal_service_account`. |
| User actions | `POST /core/users/{id}/set_password/`, `/set_password_hash/`, `/recovery/` (returns a link), `/recovery_email/` (needs `email_stage` uuid), `/impersonate/`, `GET /core/users/impersonate_end/`, `POST /core/users/service_account/` (returns a token), `GET /core/users/me/`, `GET /core/users/paths/` | Password actions need `authentik_core.reset_user_password`. Adding a user to a group whose **ancestry** includes a superuser group needs `enable_group_superuser`. Assigning roles needs `authentik_rbac.change_role`. |
| Sessions | `GET /core/authenticated_sessions/` (filter `user__username`), `DELETE …/{uuid}/`, `DELETE …/bulk_delete/?user_pks=` | Fields: `last_ip`, `last_user_agent`, `last_used`, `expires`, `geo_ip`, `asn`. Deactivating a user deletes their sessions (2026.8). |
| MFA devices | `GET /authenticators/admin/all/?user=<pk>`. Per type, CRUD on `/authenticators/admin/{totp,webauthn,static,duo,sms,email}/{id}/` | Device metadata only: name, type, confirmed, created, last_used. |
| Tokens | `GET/POST /core/tokens/`, `GET/PUT/PATCH/DELETE /core/tokens/{identifier}/`, `GET …/view_key/`, `POST …/set_key/` | Intents: `api`, `app_password`, `recovery`, `verification`. Keyed by identifier. `view_token_key` is a separate permission, and the qbit must never hold it. |
| Groups | `GET/POST /core/groups/`, `GET/PUT/PATCH/DELETE /core/groups/{uuid}/`, `POST …/add_user/` and `…/remove_user/` (`{pk}`) | **`parents` is many-to-many since 2025.12.** Groups inherit roles **and superuser status** from every ancestor. Names are unique (2025.12). Filters: `members_by_username[]`, `include_users`, `include_children`, `include_parents`, `include_inherited_roles`, `is_superuser`, `attributes`. Custom permissions: `add_user_to_group`, `remove_user_from_group`, `enable_group_superuser`, `disable_group_superuser`. |
| RBAC | `GET/POST /rbac/roles/`, `…/{uuid}/add_user/` and `…/remove_user/`, `GET /rbac/permissions/` (catalogue), `GET /rbac/permissions/assigned_by_roles/?model=&object_pk=`, `POST …/assigned_by_roles/{uuid}/assign/`, `PATCH …/unassign/`, `GET /rbac/permissions/roles/?uuid=`, `/rbac/initial_permissions/` | **RBAC has been fully role-based since 2025.12.** Roles are assigned to users directly or through groups; there are no direct user permissions. `assign` with `object_pk` grants an object permission; without it, a global one. Initial permissions automatically grant a role permissions on objects that its members create. |
| Apps and policies | `GET /core/applications/`, **`GET /core/applications/{slug}/check_access/?for_user=`** (returns `{passing, messages}`), `GET/POST /policies/bindings/` (target = `pbm_uuid`, plus exactly one of policy/group/user), `/policies/all/`, `POST /policies/all/{uuid}/test/` | `check_access` explains access per user and per app. Bindings can expire (2026.8). |
| Events | `GET /events/events/` (filters `action`, `actions[]`, `username`, `client_ip`, `context_model_app`, `context_model_name`, `context_model_pk`, `context_authorized_app`), `POST /events/events/` (custom `custom_*` actions), `/actions/`, `/volume/`, `/top_per_user/` | Relevant actions: `login`, `login_failed`, `logout`, `password_set`, `model_created/updated/deleted`, `impersonation_*`, `secret_view`, `invitation_used`, `user_write`, `suspicious_request`, `custom_`. |
| Notifications | `/events/transports/` (mode `webhook` with `webhook_mapping_body`/`_headers`), `/events/rules/` (needs `destination_group` or `destination_event_user`), `/policies/event_matcher/` (action, app, model, client_ip) | Delivery runs as Dramatiq tasks with 5 retries, at least once, with no idempotency key and no ordering guarantee. The default body has no event id or model, so a custom body mapping is required. |
| Invitations | `GET/POST /stages/invitation/invitations/` (`fixed_data`, `expires`, `single_use`, `flow`), `POST …/{uuid}/send_email/` | Needs an enrollment flow with an invitation stage, plus working email. |
| Blueprints | `GET /managed/blueprints/`, `POST …/{uuid}/apply/`, `GET …/available/` | Each instance has `status`, `last_applied`, `managed_models`. |

**API mechanics**

- **Auth.** Bearer tokens (`intent=api`) act with **exactly the owning user's permissions**. There are no per-token scopes. OAuth2 access tokens carrying scope `goauthentik.io/api` are also accepted (source `authentik/api/authentication.py`). Inactive users and expired tokens are rejected.
- **Pagination.** Responses are `{pagination: {next, previous, count, current, total_pages, start_index, end_index}, results, autocomplete}`. `page_size` defaults to 20 and is capped at **100** (tenant settings).
- **Rate limits.** Only anonymous requests are throttled (1000/s per process). **Authenticated API calls are not throttled at all**, so the qbit must throttle itself. kof22's Authentik shares the website's CNPG Postgres.
- **Versioning.** There is a single `/api/v3`, and its schema version equals the release version. Breaking changes are listed per release.

### 2.2 Blueprints and API edits (the critical interaction)

From the Authentik blueprint docs and `blueprints/v1/importer.py`:

- **When blueprints are re-applied.**
  - On any file change, because the blueprint directory is watched.
  - Every 60 minutes, through the `blueprints_discovery` schedule.
  - On a manual `apply`.
  - Each blueprint applies atomically and rolls back on error.
- **`state: present`** (the default) *"updates the fields specified in attrs … This will overwrite any manual changes to those fields, but fields not in attrs are left unchanged."*
- **`state: created`** creates the object once and never updates it, so manual changes survive. `must_created` fails if the object already exists. `absent` deletes it.
- **Many-to-many fields are replaced, not merged.** If a `present` entry lists `users:` on a group, or `groups:` on a user, the serializer *replaces* the whole set on every apply. Any membership added through the API is wiped within an hour. The same is true of `parents` and `roles`. This is an inference from the serializer behaviour; phase 0 has a contract test for it (§10).
- **Blueprint entries can carry `permissions:`** (object permissions, 2024.8+). This is the mechanism §5.1 uses to delegate specific groups to the qbit's service account *from Git*.
- **No built-in per-object marker.** Objects created by a blueprint carry no generic "owned by blueprint X" marker that the API can read. Only `managed` exists, and only on some models, for Authentik's own system objects. So ownership must be declared by convention (§4.1).

MHP learned this the hard way. Blueprint drift caused incident APPS-1462, which led to the split in `GG/DevOps/HANDOFF-authentik-split.md`. MHP's least-privilege "portal-provisioner" account deliberately has **no token permissions** as a result.

### 2.3 Current kof22 state (cluster-gitops `tenants/kof22/website-authentik/`, Website-Backend origin/main)

- **Deployment.** Authentik chart `2026.8.3`, delivered by ArgoCD. Blueprints come from ConfigMap `kof22-authentik-blueprints`, mounted at `/blueprints/custom`, as a single file `kof22-oauth2-apps.yaml`. **Every entry is `state: present`.** Secrets come from an ExternalSecret `authentik-env` that reads the Infisical `ClusterSecretStore` named `infisical`.
- **What the blueprint defines.**
  - Flows (authentication, recovery, invalidation, authorization) and their stages. Recovery email uses stage `kof22-recovery-email`.
  - OAuth2 provider `kof22-website-prod` and application `kof22-website-prod` (`policy_engine_mode: any`).
  - Expression policy `kof22-require-admins-group`, which allows members of `kof22-admins` or `kof-website-publishers`, bound to the application.
  - Group `kof22-admins`, with no attributes, not superuser, and no `users` attr.
  - User `james.maes`, with `groups: [authentik Admins, kof22-admins]`. So **james's memberships are blueprint-owned and would be reverted**.
  - The groups claim comes from an override of the managed `profile` scope mapping, not from a `groups` scope.
- **Created only in the UI, so not in Git.** Group `kof-website-publishers`, service account `kof-publisher-prod` (expiring app password), and the `kof22-post-ios` provider.
  - The backend also recognizes `kof-website-drafters`, but that group doesn't exist, and the app policy wouldn't admit drafters anyway.
- **How the backend uses groups.** `AuthentikAuthenticationCustomizer` maps exactly one group, `kof22-admins`, to a fixed permission set (ESB, analytics, quick-search admin, data health). Everything else is ungated.
  - Web Users (`adminWebUsers`) lists `qqq_user_session` and the redirect state.
  - The `adminHomeWebUsers` widget's footer says groups and lockouts are managed in the IdP and links to `/if/admin/#/identity/users`.
  - `docs/admin-home.md` notes: "the backend has no Authentik API access".
  - There is no `AUTHENTIK_API_*` configuration.
- **MHP prior art.**
  - `health.me.apps.support.services.authentik.AuthentikAdminApiClient` calls groups, users, `add_user`, `recovery` and `recovery_email`.
  - Configuration: `AUTHENTIK_API_URL` (host root) and `AUTHENTIK_API_TOKEN`, from an ESO secret.
  - It maps IAM-style policy documents, stored in group attributes, to a `policies` claim (ADR 0001).

### 2.4 Prior art: clients and other admin platforms

- **Official generated clients** exist for Go (`goauthentik.io/api/v3`), Python (`authentik-client`), TypeScript (`@goauthentik/api`) and Rust. Their versions track the server.
  - **There is no official Java client**: Maven Central has nothing under `io.goauthentik`, and `client-java` returns 404.
  - `client-kotlin` is archived at 2023.10.
  - The Terraform provider `goauthentik/authentik` is at 2026.8.0.
- **Decision: no generated client.** The qbit uses roughly 30 operations. A hand-mapped `BaseAPIActionUtil` subclass is smaller than an openapi-generator client of more than 140 models. It also gets QQQ's paging, retry, 429 backoff and outbound logging for free. The pinned schema is kept in test resources, and a contract test (§10) diffs the operations the qbit uses against it.
- **Delegated IdP admin elsewhere** (general background, not verified this session):
  - Keycloak uses realm-management roles (`manage-users`, `view-users`, `query-groups`) plus fine-grained admin permissions, granted to a service-account client.
  - Okta uses scoped admin roles and resource sets, and `okta.*` OAuth scopes.
  - The common pattern, which this design copies, is a *machine identity whose IdP-side permissions are the real limit*, with the app's own UI permissions layered on top as a second, narrower filter.

### 2.5 QQQ building blocks reused

| Need | QQQ asset |
|---|---|
| QBit packaging | `QBitMetaDataProducer<C>` + `QBitConfig.validate(QInstance, List<String>)` + `QBitComponentMetaDataProducer`. The pattern follows `qbit-sql-workbench` and `qbit-quick-search`. |
| API-backed tables | `qqq-backend-module-api`: `APIBackendMetaData` (`AuthorizationType.API_TOKEN`), a `BaseAPIActionUtil` subclass (`buildQueryStringForGet`, `processGetResponse`, `jsonObjectToRecord`, `buildUrlSuffixForSingleRecordGet`, `doQuery`/`doCount` overrides, 429 `RateLimitException` backoff, 5xx retry, `getMillisToSleepAfterEveryCall`, `OutboundAPILog`) |
| Actions on tables and records | Table-bound processes (`withTableName`, `min/maxInputRecords`), `StreamedETLWithFrontendProcess` preview → review → execute, and `QMenu` slots (`VIEW_SCREEN_ACTIONS`, `QUERY_SCREEN_ACTIONS`). **Never left nav.** |
| Permissions | `QPermissionRules`, `PermissionsHelper.check*Throwing`, `session.hasPermission`. Available permissions are derived from metadata. |
| Audit | `AuditAction`, `RunBackendStepOutput.addAuditSingleInput`, and the sql-workbench append-only execution-row pattern |
| Inbound webhooks | A `QJavalinRouteProviderInterface`, modeled on `qbit-easypost-tracking` `EasyPostWebhookRouteProvider` (shared-secret check), or `ProcessBasedRouter` |
| Async refresh | `qqq-esb`: a destination plus an `EsbTrigger` that runs a process with `esbMessages`, at-least-once |
| Caching | `Memoization<K,V>` (TTL plus max size); `CacheOf` (unique-key caches) |
| Secrets | `QMetaDataVariableInterpreter` (`${env.X}`). **`APIBackendMetaData.enrich()` is commented out**, so the qbit must interpolate its own secrets. |
| Next UI | Generic table, record and process screens, plus custom widget types switched in `WidgetRenderer.tsx` (the `SQL_WORKBENCH` and `ESB_OVERVIEW` pattern), with `src/lib/api/<feature>.ts` |
| Tests | Testcontainers (`RequiresDockerCondition` skip, as in sql-workbench), `MockApiActionUtils` queued responses |

---

## 3. Architecture

### 3.1 Components

```
┌──────────────────────────── QQQ Next UI ─────────────────────────────────┐
│ generic table/record/process screens  +  AUTHENTIK_GROUP_TREE,            │
│ AUTHENTIK_ACCESS_MATRIX, AUTHENTIK_DRIFT widgets  (+ admin-home widgets)  │
└──────────────┬───────────────────────────────────────────────────────────┘
               │ /qqq/v1/... (normal QQQ routes)   /qqq/v1/authentik/* (widgets)
┌──────────────▼──────────────── qbit-authentik (Java) ────────────────────┐
│ AuthentikQBitProducer / AuthentikQBitConfig                              │
│  ├─ API backend "authentik"  ── AuthentikActionUtil (BaseAPIActionUtil)   │
│  │     ├─ AuthentikGuard   (QQQ permission + ownership + protection)      │
│  │     ├─ FilterMapper     (QQueryFilter → Authentik query params)        │
│  │     └─ Throttle         (token bucket, bulk pacing)                    │
│  ├─ OwnershipService       (Blueprint / QQQ / Unmanaged; delegation set)  │
│  ├─ processes (table/record actions; preview→review→execute)              │
│  ├─ AdminActionAuditor     (local audit row + Authentik custom event)     │
│  ├─ Mirror tables (RDBMS) + MirrorSync (webhook→ESB + reconcile)          │
│  ├─ AuthentikWebhookRouteProvider (/qqq/v1/authentik/webhook)             │
│  └─ AuthentikGroupPermissionMapper (reusable auth customizer component)   │
└──────────────┬───────────────────────────────────────────────────────────┘
               │ Bearer token of service account "qqq-<app>-idadmin"
┌──────────────▼──────────────── Authentik ────────────────────────────────┐
│ role qqq-<app>-idadmin: global view_*; object perms on delegated groups;  │
│ initial_permissions on users it creates   (all declared in a blueprint)   │
│ notification rule + event_matcher policy + webhook transport → QQQ        │
└───────────────────────────────────────────────────────────────────────────┘
```

**Packaging.** Repo `QRun-IO/qbit-authentik`; Maven `com.kingsrook.qbits:qbit-authentik`; package `com.kingsrook.qbits.authentik`; QBit identity `com.kingsrook.qbits:authentik`; parent `qbit-build-parent` 2.0.0. The Next UI widgets ship in `qqq-frontend-next` behind a probe (§8.4). There is one module; a separate client module would be premature.

### 3.2 `AuthentikQBitConfig`

| Setting | Default | Meaning |
|---|---|---|
| `baseUrl` | required | Authentik host root, e.g. `https://auth.kof22.com`. `/api/v3` is appended. This is separate from the OAuth2 issuer URL, as MHP learned. |
| `apiToken` | required | Usually `${env.AUTHENTIK_API_TOKEN}`. Interpolated by the qbit, redacted in `toString()` |
| `tableNamePrefix` / namespace | `authentik` | Prefix for tables, processes and widgets |
| `features` | `{READ}` | A subset of `READ`, `MEMBERSHIP`, `USER_LIFECYCLE`, `CREDENTIALS` (recovery email, revoke sessions, MFA reset, token revoke), `GROUP_CREATE`, `RBAC`, `ACCESS_MATRIX`, `MIRROR`, `WEBHOOK`. A feature that is off produces no metadata. |
| `readOnly` | `false` | Hard kill switch. Every write is refused, whatever the features and permissions. |
| `delegation` | `ATTRIBUTE` | How the qbit decides which groups it may manage: `ATTRIBUTE` (groups whose attributes include `qqq.io/delegated-to: <appKey>`), `NAME_PREFIX` (e.g. `kof-website-`), `EXPLICIT` (a list), or a combination. The service account's object permissions remain the real limit (§5.1). |
| `appKey` | required | Identifies this QQQ app in attributes and audit, e.g. `kof22-admin` |
| `userScope` | all `internal` + `external` users | Optional `path_startswith` and/or `groups_by_name` filter, so a shared Authentik can be partitioned between apps |
| `protectedUsernames` | `[]` + the service account + all superusers | Accounts no operator can change (§5.4) |
| `protectedGroups` | all `is_superuser` groups and their descendants | Groups that can never be managed |
| `recoveryEmailStage` | required if `CREDENTIALS` | The email-stage slug or uuid for `recovery_email`, e.g. `kof22-recovery-email` |
| `invitationFlow` | required for invites | The enrollment flow slug |
| `applicationsInScope` | all visible | Application slugs shown in the access matrix |
| `confirmations` | see §6.3 | Per-action confirmation level: `CONFIRM`, `TYPED`, `TWO_PERSON` |
| `throttle` | 10 req/s, burst 20; bulk pacing 100 ms | Self-imposed, because authenticated calls are unthrottled server-side |
| `cacheTtl` | 30 s for lists, 5 min for `check_access` and the catalogue | Memoization TTLs |
| `mirror` | backend name + 15-minute reconcile + 30-day event retention | Only used if `MIRROR` is on |
| `webhookSecret` | `${env.AUTHENTIK_WEBHOOK_SECRET}` | Shared secret, carried in a header set by the transport's header mapping |
| `esbDestinationName` | optional | If set, webhook events go to the ESB. Otherwise they are processed in-request. |
| `permissionBaseName` | `authentik` | Base for the QQQ permission names in §5.2 |
| `expectedServerVersion` | `2026.8` | A major or minor mismatch with `/api/v3/root/config` (or the `info.version` header) logs a warning at startup and in the health widget |

`validate()` checks:
- the URL is well formed and the token is non-blank
- each feature's dependencies are present: `CREDENTIALS` needs `recoveryEmailStage`, `MIRROR` needs an RDBMS backend, `WEBHOOK` needs a secret
- the prefix rules hold
- `readOnly` and write features are not both set (that combination is a warning, not an error)

---

## 4. Blueprint / GitOps coexistence

### 4.1 Ownership model

Every group and user the qbit shows carries a computed **`ownership`** and a **`membershipOwner`**:

| ownership | How it is detected | Allowed in QQQ |
|---|---|---|
| `BLUEPRINT` | The object has attribute `qqq.io/owner: blueprint` (convention, set in the blueprint `attrs`), **or** appears in the qbit's `blueprintObjects` hint list | Read only. Editing is refused with "Managed by blueprint `<name>`; change it in Git", and a link to the cluster-gitops path is shown if configured. |
| `QQQ` | Attribute `qqq.io/owner: <appKey>` (stamped by the qbit when it creates the object) | Full management within the enabled features |
| `UNMANAGED` | Neither | Read only by default. The *Adopt* action (identity admin only, typed confirmation) stamps `qqq.io/owner: <appKey>`. The drift report recommends either adopting it or moving it into a blueprint. |

The membership owner is tracked separately from the object owner. A group can be blueprint-defined while QQQ manages its members:

| membershipOwner | Rule |
|---|---|
| `BLUEPRINT` | The blueprint entry sets `users:` on the group, or the user's own blueprint entry sets `groups:`. Detected by the marker `qqq.io/membership: blueprint`. **Membership changes are refused**, because they would be reverted within 60 minutes. |
| `QQQ` | The group is delegated (§3.2 `delegation`) and the blueprint does not list members. Membership is managed in QQQ. |

The **marker convention** is the core of the design, because Authentik exposes no per-object blueprint provenance (§2.2). It costs one attribute per blueprint entry. The qbit ships a sample blueprint snippet and a cluster-gitops doc change.

### 4.2 Recommended blueprint pattern (kof22)

```yaml
# Structure stays in Git; membership is delegated to QQQ.
- model: authentik_core.group
  state: present
  identifiers: { name: kof-website-publishers }
  attrs:
    attributes:
      qqq.io/owner: blueprint
      qqq.io/delegated-to: kof22-admin      # the qbit may manage members
  # no "users:" here → membership is not reverted
  permissions:                              # Authentik-side delegation
    - role: !Find [authentik_rbac.role, [name, qqq-kof22-admin-idadmin]]
      permission: add_user_to_group
    - role: !Find [authentik_rbac.role, [name, qqq-kof22-admin-idadmin]]
      permission: remove_user_from_group
    - role: !Find [authentik_rbac.role, [name, qqq-kof22-admin-idadmin]]
      permission: view_group
```

`kof22-admins` keeps `qqq.io/owner: blueprint` and has **no** `qqq.io/delegated-to` (Q5). Its membership is managed only in Git, so QQQ shows it but cannot change it. The `james.maes` user entry already pins his groups, so the qbit marks him `membershipOwner=BLUEPRINT` and lists him in `protectedUsernames`.

### 4.3 Drift report

`authentikDriftReport` is a widget plus a process that can be exported. It lists:

1. **Unmanaged objects**: groups, users, providers and service accounts with no owner marker. kof22 today: `kof-website-publishers`, `kof-publisher-prod`, the `kof22-post-ios` provider. Suggested fix: add the object to a blueprint with `state: created` or `present`, or adopt it.
2. **Delegation mismatch.** A group is marked `qqq.io/delegated-to` but the service account lacks `add_user_to_group` on it, found with a `check` call against `GET /rbac/permissions/assigned_by_roles/?model=authentik_core.group&object_pk=`. The reverse case is a permission granted with no marker.
3. **Doomed edits.** QQQ memberships on groups whose membership owner is `BLUEPRINT`, which can only happen if the marker changed after the edit.
4. **Blueprint health.** `GET /managed/blueprints/` instances whose `status` is not `successful`, or whose `last_applied` is older than 2 hours.
5. **Policy gaps.** Groups that the host app's permission mapper recognizes but that no application binding admits. In kof22, `kof-website-drafters` is the live example.
6. **Superuser exposure.** Users who are superusers through inherited parents. This is informational.

### 4.4 Rejected alternatives

- **Blueprints for everything, with QQQ opening PRs to cluster-gitops.** This is the purest GitOps, but membership changes would wait on PR review and an ArgoCD sync, which is too slow for "lock out a contractor now". It could come back later as an *export to blueprint* button for promoting unmanaged objects (phase 4+).
- **QQQ owns everything, with blueprints only for bootstrap (`state: created`).** This loses review and history for structural changes, and repeats MHP's APPS-1462 drift in the other direction.

---

## 5. Permission and safety model

### 5.1 Authentik side: least-privilege service account

All of the following is declared in a blueprint, in the cluster-gitops PR that follows this spec:

- **User `qqq-<appKey>-idadmin`**, `type: service_account`, path `service-accounts/qqq`.
  - It is never placed in any group: no superuser ancestry, and no app access.
  - Its API token (`intent: api`, expiring 90 days) is created once in the Authentik UI, or with `POST /core/users/service_account/` by an admin, and stored in Infisical. Blueprints should not mint secrets.
- **Role `qqq-<appKey>-idadmin`**, assigned directly to that user with `/rbac/roles/{uuid}/add_user/`. Its permissions:

| Permission | Scope | Why | Phase |
|---|---|---|---|
| `authentik_core.view_user`, `view_group`, `view_application`, `view_authenticatedsession`, `authentik_rbac.view_role`, `authentik_policies.view_policybinding`, `authentik_events.view_event`, `authentik_blueprints.view_blueprintinstance`, `authentik_stages_invitation.view_invitation` | global | Read screens | 1 |
| `authentik_core.view_user_applications` + `view_application` (exact permission `check_access` requires is a phase 0 item to verify) | global | Access matrix | 1 |
| `authentik_events.add_event` | global | Write `custom_qqq_admin_action` audit events | 1 |
| `add_user_to_group`, `remove_user_from_group`, `view_group` | **object**, per delegated group (blueprint `permissions:`) | Membership | 2 |
| `authentik_core.add_user` + an initial-permissions rule granting `change_user`, `reset_user_password`, `delete_authenticatedsession` on **users it creates** | global add; object for the rest | Lifecycle limited to users QQQ created or adopted | 3 |
| `authentik_core.change_user` on adopted users (object, granted by *Adopt* through a blueprint entry or by an admin) | object | Deactivate or reactivate pre-existing users | 3 |
| `authentik_stages_invitation.add_invitation` + `view_emailstage` on the recovery email stage | global / object | Invites, recovery email | 3 |
| Device delete on `authentik_stages_authenticator_*` | object via initial permissions | MFA reset | 3 |
| `authentik_core.delete_token` (app passwords of managed users only) | object | Revoke app passwords | 3 |
| `authentik_rbac.change_role` / `assign` on **delegated roles** (object) | object | Role assignment | 4 |

The account is **never** granted:
- `enable_group_superuser` / `disable_group_superuser`
- `view_token_key` / `set_token_key` (the MHP APPS-1462 lesson)
- `impersonate`
- global `reset_user_password`
- any `change_*` on flows, stages, providers, applications, policies or brands
- any write on its own role

**Effect.** Even a fully compromised QQQ backend can only change memberships of the delegated groups and manage users that QQQ created or that were explicitly adopted. Authentik enforces this; QQQ code does not. An open item for phase 0: confirm that `add_user`/`remove_user` honour the *object* permissions `add_user_to_group`/`remove_user_from_group` rather than requiring global `change_group`. If they do not, the fallback is object `change_group` on the delegated groups, with the blueprint markers still protecting structure.

### 5.2 QQQ side: permissions

Base name `authentik`, configurable:

| QQQ permission | Grants |
|---|---|
| `authentik.hasAccess` | The Identity app and read-only tables: users, groups, memberships, roles, applications, bindings, blueprints |
| `authentik.sensitive.read` | Sessions (IPs, user agents), events, MFA devices, tokens metadata, user `attributes` |
| `authentik.membership.write` | Add and remove members of delegated groups, limited to groups the operator holds (§5.4) |
| `authentik.membership.grantAny` | Add members to any delegated group, even ones the operator doesn't hold |
| `authentik.user.lifecycle` | Invite, deactivate, reactivate, adopt |
| `authentik.user.credentials` | Send recovery email, revoke sessions, reset MFA, revoke app passwords |
| `authentik.group.write` | Create delegated groups and edit their attributes |
| `authentik.rbac.write` | Assign delegated roles (phase 4) |
| `authentik.approve` | Approve two-person requests |
| `authentik.admin` | Drift report actions (adopt) and the qbit's config/health view |

Recommended mapping for kof22 (Q5):
- A new blueprint group `kof22-identity-admins`, a child of nothing and not superuser, gets everything except `grantAny`.
- `kof22-admins` gets `hasAccess` + `sensitive.read`.

The mapping goes through the reusable `AuthentikGroupPermissionMapper` (§5.6).

### 5.3 Enforcement point (re: #1061, #1062)

The HTTP middleware checks table and process permissions, but nothing else does (#1061). The qbit therefore enforces permissions in **its own backend layer**, so a direct `InsertAction`/`UpdateAction`/`DeleteAction`/`RunProcessAction` call cannot bypass them. Such calls can come from a script, the assistant (#1054), MCP (qbit-middleware-mcp#2) or ESB triggers.

- `AuthentikActionUtil.doQuery/doGet/doInsert/doUpdate/doDelete` call `AuthentikGuard.check(table, op, session)` first. The guard checks the QQQ permission (§5.2), the feature flag and `readOnly`. A session with no user is refused, except the qbit's own internal `MirrorSync` session, which is read-only.
- Every qbit process's first backend step re-checks its permission (`checkProcessPermissionThrowing`) plus the per-record guard rules (§5.4). It does not trust that the middleware ran.
- **Field-level protection does not use `FieldSecurityLock`** (#1062). Sensitive columns (`attributes`, `last_ip`, `user_agent`, `geo_ip`) are removed in `jsonObjectToRecord` when the session lacks `authentik.sensitive.read`. Filters on those fields are rejected for that session.
- Tables are declared with `QPermissionRules` too, so the generic UI hides what the user can't use. That is defense in depth, not the primary control.
- An ArchUnit test asserts that every write path in the qbit goes through `AuthentikGuard`.

### 5.4 Guardrails (checked in the preview step and again at execute)

1. **Protected accounts.**
   - Covered: `protectedUsernames`, the service account itself, every user with `is_superuser=true` (including superuser status inherited through `parents`), and every user whose membership owner is `BLUEPRINT`.
   - No lifecycle, credential or membership action can target them.
   - These accounts appear in previews as "skipped: protected (reason)".
2. **Protected groups.** Superuser groups and **all their descendants** cannot be managed. Membership in a child of a superuser group makes the user a superuser (source `core/models.py`, `all_groups()`). The guard also refuses to set `parents` on a new group to anything outside the delegated tree.
3. **No self-service elevation or lockout.**
   - Operators cannot add or remove *themselves*, deactivate themselves, or revoke their own sessions from the bulk tools.
   - The operator is matched by Authentik username or email from the OAuth2 claims, which is `sub_mode: user_email` in kof22.
4. **No grants beyond your own.** Without `grantAny`, an operator can only add members to groups the operator currently belongs to. The guard checks this against the operator's **live** Authentik groups (`GET /core/users/?username=…&include_groups=true`, cached 30 s), not the session's `groups` claim. The claim can be up to one hour old (the access-token lifetime), so a just-demoted operator could otherwise still grant.
5. **Last-member protection.** The qbit refuses to remove the final active member of a group listed in `config.minimumMembers` (default: groups that gate an application's policy). This prevents locking everyone out of the admin.
6. **Bulk caps.**
   - At most 100 targets per run.
   - Anything over 25 needs `TYPED` confirmation.
   - Runs are paced by the throttle.
   - A partial failure stops the run and reports per-record results. There are no silent skips.
7. **Stale checks.** At execute time the qbit re-reads each target's `last_updated`. If it changed after the preview, that record fails with "changed since preview" and is not overwritten.

### 5.5 Credentials policy

| Action | Allowed | Mechanism |
|---|---|---|
| Send password-reset email | Yes (`CREDENTIALS`) | `POST /core/users/{id}/recovery_email/` with the configured email stage and `token_duration` (default 30 minutes, matching `kof22-recovery-email`) |
| Show or copy a recovery link | **No** (Q6) | A link given to the operator amounts to account takeover. `/recovery/` is never called. |
| Set a password / set a hash | **No** | Never called. Not exposed. |
| View or set token keys | **No** | The service account lacks the permission, and the qbit never calls `view_key`/`set_key`. |
| Create a service account or app password | Phase 4, optional (Q12) | If enabled, the token is shown **once** in the result screen, never stored or logged, and the token's `expires` is set. The default is "create in a blueprint or the UI". |
| Revoke sessions | Yes | `DELETE /core/authenticated_sessions/bulk_delete/?user_pks=` |
| Reset MFA | Yes (Q7) | `GET /authenticators/admin/all/?user=`, then a per-type `DELETE`. Typed confirmation and a reason are required. |

### 5.6 `AuthentikGroupPermissionMapper` (reusable customizer)

This is a `QAuthenticationModuleCustomizerInterface` component that apps can use instead of hand-written customizers such as kof22's `AuthentikAuthenticationCustomizer`:

- `withGroupPermissions("kof22-identity-admins", Set.of(...))`, i.e. a static map from group to permissions.
- An optional `withAttributePermissions(true)`. Permissions are read from a `qqq_permissions` claim, emitted by a scope mapping from the group attribute `qqq.io/permissions: [...]` that the blueprint manages. This generalizes MHP's IAM-style approach (ADR 0001).
- It sets `QSession` permissions, user, email and full name, and keeps the raw groups for guard rule 4.

---

## 6. Data model

### 6.1 API-backed tables (backend `authentik`, `AuthentikActionUtil`)

| Table | Source | Key | Capabilities | Notes |
|---|---|---|---|---|
| `authentikUser` | `/core/users/` | `pk` (int) | Query, get. Insert only via the *Invite* or *Create user* processes; update via processes only. | Computed fields: `ownership`, `membershipOwner`, `isProtected`, `isSuperuser`, `mfaDeviceCount` (detail only). `attributes` is sensitive. |
| `authentikGroup` | `/core/groups/` | `pk` (uuid) | Query, get. Insert via *Create group* (phase 3). | `parents` shown as display names. `isSuperuser` is inherited. `delegated`. `ownership`. Child list widget. |
| `authentikGroupMember` | Virtual: `GET /core/users/?groups_by_pk=` / `GET /core/groups/{uuid}/?include_users=true` | `(groupPk, userPk)` composite string | Query, insert → `add_user`, delete → `remove_user` | The membership association. It supports the generic child-record UI on both user and group. Insert and delete go through the guard, with processes as the preferred UI. |
| `authentikRole` | `/rbac/roles/` | uuid | Query, get | `managed` roles (`ak-managed-role--*`, `ak-migrated-role--*`) are hidden by default |
| `authentikRoleAssignment` | Virtual over `GET /rbac/roles/?users=` / `?groups=` (`inherited=true` for effective) | composite | Query; insert/delete in phase 4 | `assignedVia`: DIRECT or a group name |
| `authentikPermission` | `/rbac/permissions/` + `assigned_by_roles` | id | Query | Catalogue and per-role view (read only) |
| `authentikApplication` | `/core/applications/` | slug | Query, get | Read only. Links to bindings. |
| `authentikPolicyBinding` | `/policies/bindings/?target=` | uuid | Query | Read only. Shows the policy, group or user, plus negate, order and expiry. |
| `authentikSession` | `/core/authenticated_sessions/` | uuid | Query, delete (revoke) | Sensitive |
| `authentikMfaDevice` | `/authenticators/admin/all/?user=` | `type:id` | Query, delete (reset) | Metadata only. Query requires a `user` criterion. |
| `authentikToken` | `/core/tokens/?user__username=` | identifier | Query, delete (revoke) | **No key field.** Only `intent`, `expires`, `description`. |
| `authentikEvent` | `/events/events/` | uuid | Query, get | Sensitive. Ordered `-created`. `context` is rendered as JSON. |
| `authentikInvitation` | `/stages/invitation/invitations/` | uuid | Query; insert via *Invite*; delete (cancel) | |
| `authentikBlueprint` | `/managed/blueprints/` | uuid | Query | Read only. Used by the drift report. |

**`FilterMapper`** maps QQQ criteria to Authentik query params:
- `EQUALS` → `field=`
- `IN` → repeated `field[]=` where supported (`groups_by_name`, `type`, `actions`)
- `GREATER_THAN` / `LESS_THAN` on date fields → `__gt` / `__lt`
- `IS_BLANK` → `__isnull=true`
- `CONTAINS` / `STARTS_WITH` on text fields → `search=` (or `path_startswith`)
- `orderBy` → `ordering`

Unsupported criteria (OR across fields, `NOT_EQUALS`, nested filters) are **rejected** with a clear message. The alternative, client-side filtering across up to 100-row pages, would silently truncate results.

**Pagination.** `doQuery` is overridden to use `page`/`page_size=100`. It reads the count from `pagination.count`, so `doCount` costs one request with `page_size=1`, and it stops at `total_pages`.

**Identifier types.** Ids are mixed (int user pk, uuid group pk, slug, identifier), so each table declares its own `buildUrlSuffixForSingleRecordGet`.

**Caching.**
- `Memoization` with TTLs from config, keyed by session-independent request URL.
- Results are permission-filtered *after* the cache, because the API call itself always runs as the service account.
- Every successful write invalidates the touched keys, and webhook events also invalidate (§7).

### 6.2 Local tables (host RDBMS backend)

| Table | Purpose |
|---|---|
| `authentikAdminAction` | **Append-only audit and outbox** of every write the qbit attempts. Columns: id, correlationId, actionName, operator (QQQ user + Authentik username), reason, targetType, targetKey, targetLabel, beforeJson, afterJson, status (`PENDING_APPROVAL`, `RUNNING`, `SUCCEEDED`, `FAILED`, `REFUSED`), errorMessage, authentikEventPk, approver, timestamps. The row is inserted *before* the API call, as in the sql-workbench `ExecutionAuditor` pattern. If that insert fails, the call is not made. Insert, update and delete capabilities are removed for everyone, so the table is read-only in the UI. |
| `authentikApprovalRequest` | Two-person requests (phase 4): requested action payload, requester, approver, expiry (24 h), and the rule that the approver cannot be the requester |
| `authentikUserMirror` | `MIRROR` feature: pk, uuid, username, email, name, type, isActive, lastLogin, isSuperuser, ownership, groupNames (denormalized text), mfaDeviceCount, lastFailedLogin, failedLogins24h, refreshedAt. Unique on username and email. This is what Web Users joins to (`qqq_user_session.user_id` = email in kof22). |
| `authentikMembershipMirror` | userPk, groupPk, groupName, refreshedAt. Used for widgets and "members of X" counts without fan-out calls. |
| `authentikWebhookInbox` | Received webhook payloads (deduplicated by event pk) with processing status. Retained 30 days. |

### 6.3 Processes (table and record actions only, never left nav)

Each process:
- is bound with `withTableName(...)` and placed in `QMenu` slots, with `isHidden(true)` for navigation
- uses `StreamedETLWithFrontendProcess`, with steps: **input** (targets, values, **reason** required for writes), **preview** (server-computed: per-target before → after, guard results, skipped targets with reasons, and `check_access` deltas where relevant), **confirm** (per `confirmations` level), **execute** (audit row → API call → Authentik custom event → cache/mirror invalidation), and **result** (per-record outcome with a link to the audit rows)

| Process | Table / slot | Records | Feature | Default confirmation | Authentik calls |
|---|---|---|---|---|---|
| Add to groups | `authentikUser` query + view actions | 1–100 | MEMBERSHIP | CONFIRM (TYPED > 25) | `POST /core/groups/{g}/add_user/` per pair |
| Remove from groups | same | 1–100 | MEMBERSHIP | CONFIRM (TYPED > 25) | `remove_user` |
| Add members / Remove members | `authentikGroup` view action | 1 group, picker for users | MEMBERSHIP | same | same |
| Check access | `authentikUser` view action | 1 | READ | none | `check_access` per in-scope app |
| Invite user | `authentikUser` query action | 0 | USER_LIFECYCLE | CONFIRM | `POST /stages/invitation/invitations/` (`fixed_data`: email, name, the initial groups, re-validated at use by an expression in the enrollment flow; `single_use`; expires 7 days) + `send_email` |
| Create user (no invite) | same | 0 | USER_LIFECYCLE (Q3) | CONFIRM | `POST /core/users/` (`is_active=true`, attribute `qqq.io/owner`), then recovery email. The user sets their own password. |
| Deactivate | `authentikUser` | 1–25 | USER_LIFECYCLE | TYPED | `PATCH is_active=false` (deletes sessions in 2026.8), plus optional app-password revoke |
| Reactivate | same | 1–25 | USER_LIFECYCLE | CONFIRM | `PATCH is_active=true` |
| Adopt user / Adopt group | user / group view action | 1 | `authentik.admin` | TYPED | `PATCH attributes` (stamp owner). Object permissions come from the blueprint or an admin, and the preview shows whether they exist. |
| Send password reset email | `authentikUser` | 1–25 | CREDENTIALS | CONFIRM | `recovery_email` |
| Revoke sessions | `authentikUser`, `authentikSession` | 1–100 | CREDENTIALS | CONFIRM | `bulk_delete?user_pks=` / `DELETE` per session |
| Reset MFA | `authentikUser` | 1 | CREDENTIALS | TYPED | per-device `DELETE` |
| Revoke app passwords | `authentikUser`, `authentikToken` | 1–25 | CREDENTIALS | TYPED | `DELETE /core/tokens/{identifier}/` (intent `app_password` only) |
| Create group | `authentikGroup` query action | 0 | GROUP_CREATE | CONFIRM | `POST /core/groups/` with owner and delegation attributes. Parents must be delegated groups. Needs a blueprint `initial_permissions` rule so the role can manage the new group. |
| Edit group attributes | `authentikGroup` | 1 | GROUP_CREATE | CONFIRM | `PATCH attributes` (QQQ-owned only; `qqq.io/*` keys are reserved) |
| Assign role / Unassign role | user / group | 1–25 | RBAC | TWO_PERSON | `/rbac/roles/{r}/add_user/` or `PATCH group roles` (delegated roles only) |
| Cancel invitation | `authentikInvitation` | 1–25 | USER_LIFECYCLE | CONFIRM | `DELETE` |
| Drift report | `authentikGroup` / `authentikUser` query action + widget | 0 | READ | none | reads only |
| Resync mirror | `authentikUserMirror` query action | 0 | `authentik.admin` | CONFIRM | full reconcile |
| View audit trail | every table: view action → filtered `authentikAdminAction` + `authentikEvent?context_model_pk=` | 1 | READ (+ sensitive for events) | none | events query |

---

## 7. Sync

| Mode | Used for | Freshness |
|---|---|---|
| **Read-through** (API + 30 s memoization) | All Authentik tables and record screens | Live |
| **Mirror** (RDBMS) | Web Users joins, admin-home widgets, counts, "last login" columns | Webhook ~seconds; reconcile every 15 min |

**Webhook path.** These objects are defined in the same cluster-gitops blueprint:
1. An **event matcher policy** `qqq-<appKey>-sync`, one per action of interest: `model_created`, `model_updated`, `model_deleted` with app `authentik.core` (users, groups), plus `login`, `login_failed`, `password_set`, `user_write`. A rule can bind several policies in ANY mode.
2. A **webhook transport** `qqq-<appKey>` with:
   - `webhook_url: https://admin.kof22.com/qqq/v1/authentik/webhook`
   - a **body mapping** returning `{eventPk, action, created, username, model: context.model, app}`. The default body has no event id or model (§2.1).
   - a **header mapping** adding `X-QQQ-Webhook-Secret` from an environment variable.
   - `send_once: true`
3. A **notification rule** binding the policies to the transport with `destination_event_user: true`. Without a destination, nothing is sent.

`AuthentikWebhookRouteProvider` then:
1. checks the shared secret with a constant-time compare
2. inserts into `authentikWebhookInbox`, deduplicating on `eventPk`
3. publishes to the ESB destination (`authentik.events`), or processes inline if there is no ESB
4. returns 204 quickly

An `EsbTrigger` (BATCH, 50 / 2 s, concurrency 1, maxAttempts 5) runs `AuthentikApplyChangeEvents`. It invalidates memoization and re-reads only the touched users and groups into the mirror.

**Reconcile.** Delivery is at least once, with no ordering, and nothing is sent at all if a rule has no destination, so webhooks are *hints*. Every 15 minutes a scheduled process does the following:
- `GET /core/users/?last_updated__gt=<watermark>`
- `GET /events/events/?actions=login_failed&…` back to the watermark
- refreshes memberships of changed users

Nightly it does a full user and group sweep (about 20 requests for 1,000 users) and removes deleted rows.

**Web Users integration (kof22).**
- *Web Users* gains `authentikUser` (label "Identity users"), `authentikGroup` and `authentikAdminAction` sections.
- `qqq_user_session` gets an exposed join to `authentikUserMirror` on `user_id = email`, adding columns for groups, active, last login and MFA.
- The `adminHomeWebUsers` widget swaps its footer link for real numbers.
- The *Data Health* area can add rules such as "admin without MFA" and "publisher inactive > 90 days".

---

## 8. UI (Next UI)

### 8.1 App placement

- The qbit produces an app `authentikAdmin` (label **"Identity"**, icon `badge`) containing the tables in §6.1. Hosts can nest it.
- kof22 places it under **Admin → Web Users** and keeps plain labels: "Users", "Groups", "Sessions", "Audit".
- Processes appear only as record and table actions (repo rule: no processes in nav).

### 8.2 Screens

| Screen | Built from | Contents |
|---|---|---|
| Users list | generic query screen on `authentikUser` | Columns: username, name, email, type, active, last login, groups, ownership badge, protected icon. Saved views: "Inactive 90d", "No MFA", "Service accounts", "Superusers". Bulk actions: Add to groups, Remove from groups, Deactivate, Revoke sessions. |
| User detail | generic view screen + child widgets | Header badges (ownership, protected, superuser). Child lists: Groups (`authentikGroupMember`), Effective roles, Sessions, MFA devices, App passwords. "Access" tab (`check_access` per app). "History" tab (local audit + Authentik events for this user). Actions per §6.3. |
| Groups | `AUTHENTIK_GROUP_TREE` widget + generic list | A tree over `parents` (many-to-many, so a group can appear under several parents, marked as shared). Badges: blueprint, delegated, superuser (red). Member counts. Click through to group detail. |
| Group detail | generic view + child widgets | Members (with Add/Remove), parents and children, roles (direct and inherited), applications this group unlocks (bindings on the group, plus expression policies that reference it by name, marked best-effort), attributes (sensitive) |
| Access matrix | `AUTHENTIK_ACCESS_MATRIX` widget | Rows are users in scope (paged, filterable by group); columns are `applicationsInScope`; cells come from `check_access`, with a tooltip of messages. Cached 5 minutes. Calls are capped at 25 rows × N apps per page, with a "load more" control. |
| Drift | `AUTHENTIK_DRIFT` widget + exportable process | §4.3 sections, each with a count and a fix hint |
| Audit | generic query on `authentikAdminAction` + `authentikEvent` | Filter by operator, target, action, status. Each row links to the matching Authentik event. |

### 8.3 Admin-home widgets (standard widget types)

All of these come from the mirror, so there are no fan-out API calls on the home page:
- Statistics: active users, inactive users, service accounts, users without MFA, failed logins in 24 h.
- Table: recent auth events (last 20 `login`/`login_failed`).
- Table: "attention" items: locked or inactive admins, tokens expiring within 14 days, including the qbit's own token, and blueprint status not `successful`.

### 8.4 Frontend delivery

- The three custom widget types are added to `WidgetRenderer.tsx` as `React.lazy` cases, following the `SQL_WORKBENCH` and `ESB_OVERVIEW` pattern.
- An API client lives at `src/lib/api/authentik.ts` for the qbit routes `/qqq/v1/authentik/{tree,matrix,drift}`. All of them run the guard.
- Mocks go in `src/mocks/fixtures/authentik.ts`, and an e2e test in `tests/e2e/authentik.spec.ts`.
- Everything else is the generic UI. If the app isn't present, nothing renders, so the app itself acts as the feature flag.

---

## 9. Security

### 9.1 Secrets

- The **API token** is kept in Infisical at `/kof22-website-authentik-api/token`. An ExternalSecret `kof22-website-authentik-api` (store `infisical`, refresh 1 h) exposes it to the backend Deployment as env `AUTHENTIK_API_TOKEN`, following the same pattern as `kof22-website-oauth2`. MHP uses the same shape (`AUTHENTIK_API_URL`/`_TOKEN`).
- **Rotation.** The token expires after 90 days, and the qbit's health widget warns 14 days ahead.
  - Rotate it in the Authentik UI, update Infisical, and let ESO refresh the secret.
  - The qbit re-reads the env-backed value on a 401 and retries once. A restart is the fallback.
  - The runbook goes in the qbit README and the cluster-gitops docs.
- The **webhook secret** is a separate Infisical key, injected into the Authentik worker env (for the header mapping) and the backend env.
- **Redaction.**
  - `AuthentikQBitConfig.toString()` and `OutboundAPILog` redact `Authorization`.
  - Request and response bodies are **not logged** for `recovery_email`, `service_account`, token endpoints, or any response containing `token`/`key`/`link`. This is done by overriding `getAPIResponseLogLevel` and `redactOutboundCredentials` per path.

### 9.2 Audit (both sides)

- **QQQ.** An `authentikAdminAction` row is written before and after every write, as an append-only outbox (§6.2). Standard QQQ audit is also recorded on the mirror tables through `QAuditRules` at RECORD level.
- **Authentik.**
  - Authentik logs `model_updated` and similar events with `user = qqq-<appKey>-idadmin`.
  - To show the human, the qbit posts `POST /events/events/` with `action: custom_qqq_admin_action` and context `{correlationId, operator, operatorEmail, reason, action, targets, appKey}`.
  - The local row stores the returned event pk, so each side links to the other.
  - Since 2026.8, admin changes also appear in the affected user's event history.

### 9.3 Rate limiting and load

Authenticated API calls are not throttled by Authentik, and kof22's Authentik shares the website's Postgres. The qbit therefore:
- applies a token bucket (default 10 req/s, burst 20) across all callers in the JVM
- paces bulk operations
- caps the access matrix
- serves widgets from the mirror

If Authentik ever sits behind a 429-returning proxy, 429 and 5xx responses use `BaseAPIActionUtil` backoff.

### 9.4 PII and retention

- **What is PII.** Emails, names, IPs, user agents, geo/ASN and event contexts.
- **Who sees it.** Only holders of `authentik.sensitive.read`. Exports respect the same rule.
- **Mirror.** It stores no IPs or user agents, only counts and timestamps.
- **Retention.** The webhook inbox is kept 30 days. `authentikAdminAction` is kept 2 years (Q, defaults). Authentik's own event retention (`expires`) is unchanged.

### 9.5 Threats considered

| Threat | Control |
|---|---|
| A compromised QQQ backend escalates in Authentik | Authentik-side least privilege (§5.1): no superuser groups, no token keys, no global password reset |
| A QQQ operator escalates themselves | No self-edits, no grants beyond the operator's own groups, protected groups and accounts, two-person approval for RBAC |
| An operator takes over an account with a recovery link | Links are never shown; email only |
| A non-HTTP caller bypasses QQQ permissions (#1061) | Guard in the backend action util, plus ArchUnit |
| A field leaks because `FieldSecurityLock` is not enforced (#1062) | Sensitive fields stripped in the util; filters on them rejected |
| A forged webhook | Shared-secret header, constant-time compare. It only triggers re-reads, never writes. |
| Blueprint and API edits race | Ownership markers, refusal of blueprint-owned changes, drift report |
| Authentik API changes on upgrade | Pinned-schema contract test in CI; startup version check; `expectedServerVersion` |

---

## 10. Testing

- **Unit.**
  - `AuthentikActionUtil` with `MockApiActionUtils`-style queued responses, using **recorded fixtures**. The fixtures are JSON captured from the Testcontainers run, scrubbed, and checked into `src/test/resources/fixtures/2026.8.3/`.
  - Covered: FilterMapper (supported and rejected criteria), pagination, id-type handling, sensitive-field stripping, and redaction.
- **Guard and safety matrix.** For every process × permission set × target class (protected, superuser-by-inheritance, blueprint-owned, delegated, unmanaged, self), assert allow or refuse. This is a release gate.
- **Contract (integration, `@Testcontainers`, skipped without Docker via `RequiresDockerCondition`).**
  - Runs Authentik `2026.8.3`: server + worker + Postgres, bootstrapped with `AUTHENTIK_BOOTSTRAP_TOKEN`.
  - A test blueprint mirrors §4.2 and §5.1: the service account, its role, delegated and protected groups, and a superuser parent chain.
  - Tests run with the **service-account token**, not the bootstrap token, so Authentik's own denials are tested too.
  - Key assertions:
    - (a) `add_user` on a delegated group succeeds with object permissions only; on `kof22-admins` it fails with 403.
    - (b) Adding to a child of a superuser group fails.
    - (c) A membership added by the API to a `present` blueprint group that lists `users:` is reverted by `POST /managed/blueprints/{uuid}/apply/`. This proves §2.2's inference.
    - (d) Webhook transport delivery reaches a test route.
    - (e) `recovery_email` with the stage works (MailHog/Mailpit container).
    - (f) `PATCH is_active=false` deletes sessions.
- **Schema drift.** A test loads the pinned `schema.yml` and asserts that every operation and field the qbit uses exists. A scheduled CI job fetches the latest release schema and opens an issue on a diff.
- **Frontend.** Mock fixtures plus a Playwright e2e for the tree, matrix and drift widgets, and the add-to-groups flow.
- **Staging dry run.** Before kof22 production, run phase 1 and 2 against a disposable Authentik restored from a production DB snapshot, with read-only first.

---

## 11. Delivery plan

The estimates assume one senior engineer with AI assistance, plus about 20% of a frontend engineer from phase 1. They are calendar weeks.

| Phase | Scope | Exit criteria | Est. |
|---|---|---|---|
| **0. Foundations** | Repo from `qbit-template`. Config + producer. `AuthentikActionUtil` (auth, paging, FilterMapper, throttle, redaction). Testcontainers harness + fixtures recorder. **cluster-gitops PR:** service account, role and view permissions, ownership markers on existing blueprint objects, adopt `kof-website-publishers` into the blueprint. Infisical + ExternalSecret. Verify the §5.1 object-permission open item. | Contract tests (a)–(c) green; token wired in kof22 staging, read only | 1.5 wk |
| **1. MVP read-only** | Tables: users, groups, members, roles, role assignments, applications, bindings, blueprints; sensitive tables (sessions, MFA devices, tokens, events, invitations) behind `sensitive.read`. Ownership classification, drift report, `check_access` tab, group tree widget. `AuthentikGroupPermissionMapper`, with kof22 switched over. Admin-home widgets from a first mirror (scheduled reconcile only). | kof22 Web Users shows groups, last login and MFA; Scenario C and D work; guard matrix green for reads | 2.5 wk |
| **2. Membership management** | Add/remove (user and group sides, bulk), guardrails §5.4, `authentikAdminAction` + Authentik custom events, webhook route + ESB trigger + inbox, cache invalidation; blueprint delegation of `kof-website-publishers` / `kof-website-drafters` | Scenario A end to end; safety matrix 100%; webhook latency < 10 s | 2 wk |
| **3. User lifecycle and credentials** | Invite (+ enrollment-flow blueprint with an invitation stage), create (if Q3 allows), deactivate/reactivate, adopt, recovery email, revoke sessions, reset MFA, revoke app passwords, create group / edit attributes, initial-permissions blueprint rules | Scenario B end to end; contract tests (d)–(f) green | 3 wk |
| **4. RBAC and permissions** | Delegated roles, role assignment, two-person approvals (`authentikApprovalRequest`), access matrix widget, attribute-driven permission mapping (`qqq.io/permissions`), optional service-account/app-password creation (Q12), MHP migration off `AuthentikAdminApiClient` | MHP running on the qbit; approvals enforced for RBAC | 3 wk |

The total is about 12 weeks. The MVP (phases 0–1) takes about 4 weeks, and membership management lands at about 6 weeks.

### 11.1 Risks

| Risk | Mitigation |
|---|---|
| `add_user`/`remove_user` need global `change_group`, not object permissions, which would weaken §5.1 | Phase 0 contract test. Fallback: object `change_group` on delegated groups, plus markers and a guard. Document the residual risk. |
| Blueprint re-apply silently reverts QQQ edits | Ownership markers; refusal; drift report; contract test (c) |
| Superuser inheritance through `parents` (many-to-many) is missed | Compute ancestry server-side (`include_parents`), refuse in the guard, and Authentik also refuses without `enable_group_superuser` |
| Authentik upgrade breaks fields (2025.12 removed `assigned_by_users` and changed `parent` to `parents`) | Pinned schema test, version check, fixture per version |
| Load on the shared Postgres from the matrix or bulk operations | Throttle, caps, mirror-backed widgets |
| Webhook loss (no destination user, worker down) | Inbox + 15-minute reconcile + nightly full sweep |
| QQQ core permission gap (#1061) and field locks (#1062) | Guard in the backend util, ArchUnit, field stripping; adopt core enforcement once it lands |
| Token leakage through logs or outbound API log | Per-path body suppression, redaction tests |
| Operators confuse "deactivate" with "delete" | No delete in v1; clear labels; typed confirmation |
| Groups claim staleness (1-hour access token) lets a just-demoted operator still grant | Guard rule 4 (§5.4) re-reads the operator's live Authentik groups (cached 30 s) instead of trusting the claim |

### 11.2 Open questions for the owner

These are prioritized. Each lists the options, with the recommended one first.

1. **Source of truth.**
   - **(a) Split.** Blueprints own structure and *which* groups are delegated; QQQ owns membership of delegated groups and user lifecycle. *(recommended)*
   - (b) Blueprints own everything; QQQ is read-only and opens cluster-gitops PRs.
   - (c) QQQ owns everything; blueprints only bootstrap with `state: created`.
2. **v1 scope.**
   - **(a) Read-only plus membership management** (phases 0–2, about 6 weeks). *(recommended)*
   - (b) Read-only only (about 4 weeks).
   - (c) Through full lifecycle (about 9 weeks).
3. **May QQQ create or delete users?**
   - **(a) Invite (Authentik enrollment) plus deactivate/reactivate. No direct create, no hard delete.** *(recommended)*
   - (b) Also direct create with a recovery email.
   - (c) Full create and delete.
4. **Which apps first?**
   - **(a) kof22 first, built generic; MHP second, replacing its `AuthentikAdminApiClient` in phase 4.** *(recommended)*
   - (b) kof22 only.
   - (c) MHP's needs first.
5. **Who may operate it in kof22?**
   - **(a) New blueprint group `kof22-identity-admins` with full qbit rights; `kof22-admins` read-only; `kof22-admins` membership itself stays Git-only.** *(recommended)*
   - (b) All `kof22-admins` members get full rights.
   - (c) James only, through `protectedUsernames` inverted to an allowlist.
6. **Passwords and recovery.**
   - **(a) Recovery email only; never show links; never set passwords.** *(recommended)*
   - (b) Also allow copying a recovery link with typed confirmation and a two-person approval.
   - (c) Allow setting temporary passwords.
7. **MFA management.**
   - **(a) View devices and reset (delete) with typed confirmation and a reason.** *(recommended)*
   - (b) View only.
   - (c) Not at all.
   - Also: should "admin without MFA" become a Data Health rule or an alert?
8. **Destructive-operation approvals.**
   - **(a) Typed confirmation + reason for destructive operations; two-person only for RBAC and any change touching admin-gating groups (phase 4).** *(recommended)*
   - (b) Two-person for all destructive operations.
   - (c) Simple confirmation dialogs.
9. **Sync model.**
   - **(a) Read-through + webhook→ESB invalidation + 15-minute reconcile, with a small mirror for joins and widgets.** *(recommended)*
   - (b) Full mirror; all screens read the mirror.
   - (c) Read-through only, with no webhooks and no mirror (Web Users can't join).
10. **Generic IdP abstraction.**
    - **(a) Authentik-specific now, with neutral names; extract an SPI only when a second IdP is real.** *(recommended)*
    - (b) Build an `IdentityAdminProvider` SPI now.
11. **Permission mapping.**
    - **(a) Ship `AuthentikGroupPermissionMapper` (static map now, `qqq.io/permissions` group attributes in phase 4) and move kof22 to it.** *(recommended)*
    - (b) Keep per-app customizers.
12. **Service accounts and app passwords in QQQ.**
    - **(a) List and revoke only; creation stays in blueprint or UI.** *(recommended)*
    - (b) Full create with one-time token display.
13. **Where it lives.**
    - **(a) New repo `QRun-IO/qbit-authentik`, with this spec in `qqq/docs/specs` and copied to the repo's `docs/DESIGN.md` on creation.** *(recommended)*
    - (b) A module inside `qqq`.
14. **cluster-gitops housekeeping.** Bring the UI-created `kof-website-publishers`, `kof-website-drafters` (which also needs adding to the app policy, or the backend should stop accepting it), `kof-publisher-prod` and the `kof22-post-ios` provider into the blueprint, and add a real `groups` scope mapping?
    - **(a) Yes, in the phase 0 cluster-gitops PR, without `users:` attrs.** *(recommended)*
    - (b) Later.
    - (c) Leave them as UI-managed and adopt them through the qbit.
15. **Enterprise features.** Lifecycle/offboarding, account lockdown, access requests and exports are Enterprise.
    - **(a) Assume no license; design hooks only.** *(recommended)*
    - (b) We have or will get a license, so use Lifecycle offboarding for Deactivate.
16. **Retention defaults.** Local admin-action audit 2 years, webhook inbox 30 days, mirror without IPs. Are these acceptable?
    - **(a) Yes.** *(recommended)*
    - (b) Specify others.

---

## Appendix A: Example API mapping (`authentikUser`)

| QQQ | Authentik |
|---|---|
| `QueryInput(authentikUser, filter: isActive EQUALS false AND lastLogin LESS_THAN 2026-07-13, orderBy username, limit 50)` | `GET /api/v3/core/users/?is_active=false&last_login__lt=2026-07-13T00:00:00Z&ordering=username&page=1&page_size=50&include_groups=true&type=internal&type=external` |
| `GetInput(authentikUser, 42)` | `GET /api/v3/core/users/42/?include_groups=true&include_roles=true` |
| `InsertInput(authentikGroupMember, {groupPk: <uuid>, userPk: 42})` | Guard checks, then `POST /api/v3/core/groups/<uuid>/add_user/` with body `{"pk": 42}`, which returns 204 |
| `DeleteInput(authentikSession, <uuid>)` | Guard checks, then `DELETE /api/v3/core/authenticated_sessions/<uuid>/` |
| Count | `GET …&page_size=1`, reading `pagination.count` |

Response shape: `{"pagination": {"next": 2, "previous": 0, "count": 137, "current": 1, "total_pages": 3, "start_index": 1, "end_index": 50}, "results": [...], "autocomplete": {...}}`.

## Appendix B: Research references

- **Authentik live schema:** `https://auth.kof22.com/api/v3/schema/` (`info.version: 2026.8.3`, fetched unauthenticated 2026-10-11).
- **Authentik source at tag `version/2026.8.3`:** `authentik/api/authentication.py`, `authentik/api/pagination.py`, `authentik/api/throttle.py`, `authentik/lib/default.yml`, `authentik/core/models.py`, `authentik/core/api/users.py`, `authentik/events/models.py`, `authentik/events/tasks.py`, `authentik/blueprints/v1/importer.py`, `authentik/tenants/models.py`.
- **Authentik docs:**
  - https://docs.goauthentik.io/customize/blueprints/
  - https://docs.goauthentik.io/customize/blueprints/v1/structure/
  - https://docs.goauthentik.io/users-sources/access-control/
  - https://docs.goauthentik.io/users-sources/access-control/permissions/
  - https://docs.goauthentik.io/sys-mgmt/events/transports/
  - https://docs.goauthentik.io/sys-mgmt/events/notifications/
  - https://docs.goauthentik.io/releases/2025.12/
  - https://docs.goauthentik.io/releases/2026.8/
  - https://api.goauthentik.io/ (authentication, clients)
- **Clients:** `@goauthentik/api` (npm), `authentik-client` (PyPI), `goauthentik.io/api/v3` (Go proxy), `goauthentik/client-kotlin` (archived), Maven Central `io.goauthentik` (none), Terraform `goauthentik/authentik` 2026.8.0.
- **KofTwentyTwo/cluster-gitops@main:** `tenants/kof22/website-authentik/{blueprint-configmap,values,externalsecret-authentik-env}.yaml`, `clusters/k8s-galaxy/app-kof22-website-authentik-prod.yaml`, `tenants/kof22/website/externalsecret-oauth2.yaml`, `docs/PATTERN-tenant-web-and-dsm.md`.
- **KofTwentyTwo/Website-Backend@main:** `metadata/autoload/auth/AuthentikAuthenticationCustomizer.java`, `AdminSiteAuthMetaDataProvider.java`, `metadata/autoload/apps/AdminAppMetaDataProvider.java`, `metadata/adminhome/AdminHomeWidgets.java`, `AdminActivityWidgets.webUsers`, `docs/admin-home.md`, `docs/publisher-authentication.md`.
- **GG me-health-portal:** `services/authentik/AuthentikAdminApiClient.java`, `ProdAuthenticationCustomizer.java`, `docs/adr/0001-iam-style-policies-with-authentik.md`. Also `GG/DevOps/HANDOFF-authentik-split.md` (APPS-1462/1463) and `me-health-portal-cd/environments/prod/values.yaml`.
- **QRun-IO/qqq@develop:**
  - `qqq-backend-module-api` (`BaseAPIActionUtil`, `APIBackendMetaData`, `AuthorizationType`)
  - `model/metadata/qbits/*`, `PermissionsHelper`, `QAuthenticationModuleCustomizerInterface`, `OAuth2AuthenticationModule`, `AuditAction`, `Memoization`, `CacheOf`, `QMetaDataVariableInterpreter`
  - `qqq-esb` and `docs/specs/2026-09-25-qqq-esb-design.md`
  - issues #1061 and #1062; the spec format follows #1054
- **QRun-IO qbits:** `qbit-sql-workbench@develop` (`SqlWorkbenchQBitProducer`, `ExecutionAuditor`, `docs/DESIGN.md`), `qbit-quick-search`, `qbit-webhooks`, `qbit-easypost-tracking` (`EasyPostWebhookRouteProvider`), `qbit-middleware-mcp` (issue #2).
- **QRun-IO/qqq-frontend-next@main:** `src/components/widgets/WidgetRenderer.tsx`, `src/lib/api/sql-workbench.ts`, `src/components/esb/*`, `src/lib/utils/record-menu-utils.ts`.
