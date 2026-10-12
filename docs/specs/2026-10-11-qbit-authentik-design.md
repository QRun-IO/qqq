# Authentik qbit: Design Spec

Status: Approved, revision 4 (phase 0 findings applied; review fixes; owner decisions of 2026-10-12) · 2026-10-12 · Author: James Maes (drafted with Claude)
Targets: QQQ 4.1.x, QQQ Next UI 1.0.x, Authentik 2026.8.x (researched against the live `auth.kof22.com` schema, `info.version: 2026.8.3`), new repo `QRun-IO/qbit-authentik`

## 0. Summary

`qbit-authentik` lets an operator (first of all James) **add users, create groups, manage memberships and grant permissions directly in a QQQ app**. The same changes can still be made in the Authentik admin UI, and both stay consistent. The goal is to never need a Git commit or an Authentik detour for routine identity work, while keeping Authentik as the one place that actually stores identities.

The first consumer is the kof22 Admin (`KofTwentyTwo/Website-Backend`). The second is Me.Health Portal (MHP), which replaces its hand-written `AuthentikAdminApiClient`.

Seven design rules shape everything below:

1. **Authentik's database is the source of truth** for users, groups, memberships, roles and permissions.
   - The qbit reads Authentik live and writes through the Authentik API.
   - The Authentik admin UI writes to the same database.
   - QQQ keeps no competing copy, only a small refreshable mirror for joins and widgets.
2. **Blueprints shrink to infrastructure and bootstrap.**
   - Flows, stages, providers, applications, policies, scope mappings and branding stay in Git as `state: present`.
   - Bootstrap identity objects use `state: created`, which creates them if missing and never overwrites them. These are the qbit's service account, a break-glass admin and seed groups.
   - Blueprints contain no member lists at all.
   - Blueprint re-applies therefore can never undo an edit made in QQQ or the UI. This was verified against the 2026.8.3 importer source (§2.2).
3. **QQQ permissions are data, managed in QQQ.**
   - A group's QQQ permissions live in the group attribute `qqq.io/permissions`.
   - The reusable `AuthentikGroupPermissionMapper` turns a user's groups, including inherited parents, into QQQ session permissions.
   - Who can operate the qbit is decided by those same permissions (`authentik.*`). No operator group is hard-coded.
4. **The qbit checks permissions itself.** QQQ core actions do not enforce permissions (QRun-IO/qqq#1061), and `FieldSecurityLock` is not enforced (#1062). An `AuthentikGuard` in the backend action layer checks every read and write.
5. **No escalation.**
   - Operators can only grant permissions they hold themselves, whether through memberships or permission attributes.
   - Operators cannot edit themselves.
   - Protected accounts are untouchable: break-glass, the service account, Authentik superusers.
   - The last holder of the identity-admin permissions cannot be removed.
   - Authentik also enforces a hard floor: the service account cannot touch superuser groups, token keys or impersonation.
6. **Credentials never pass through QQQ.**
   - New users and password resets go through Authentik's own emails (enrollment or recovery).
   - QQQ never sets passwords, never shows recovery links and never displays token keys.
7. **Every change is audited on both sides.** QQQ writes a local append-only `authentik_admin_action` row. Authentik receives a `custom_qqq_admin_action` event naming the human operator.

### 0.1 Owner decisions (2026-10-11)

**Owner decisions (2026-10-12): all recommendations accepted.** Every open question in this spec (§11.2, and the options marked *recommended* anywhere) is decided as the spec recommends, and the design is approved to build in phase order; adjustments follow once it can be seen working. The earlier decisions are below.

| # | Decision |
|---|---|
| 1 | **Source of truth.** Authentik's database. Users, groups, memberships, roles and permissions can be edited from **both** QQQ and the Authentik admin UI. Blueprints cover infrastructure plus `state: created` bootstrap objects only, with no member lists. A nightly read-only export to Git is optional (for audit and DR, not authoritative). |
| 2 | **v1 scope.** Full user lifecycle: phases 0–3 of §11. Authentik RBAC role management is phase 4. |
| 3 | **Who operates.** Decided by permissions, not a fixed group. Fine-grained `authentik.*` QQQ permissions are granted through Authentik groups and the mapper. James holds all of them at bootstrap. All safety guards are kept. |
| 4 | **Users.** Direct create is allowed, alongside invitations. The user sets their own password through an Authentik email. No hard delete; deactivate only. |
| 5–16 | The recommendations in the first revision are accepted: recovery email only, MFA view and reset, typed confirmation plus reason with two-person approval for role changes, read-through plus webhook→ESB plus mirror, Authentik-specific with neutral names, the mapper, list-and-revoke for service accounts, new repo, cluster-gitops housekeeping, no Enterprise features, and the retention defaults. Decision 11 (attribute-driven permissions) moves **into v1**, because decision 3 depends on it. |
| §11.2 | All six smaller questions take the recommended answer: snapshot repo `KofTwentyTwo/identity-snapshots` (private); two-person approval **on** for `authentik.*` grants, with the Authentik UI as break-glass while James is the only identity admin; a new `kof22-breakglass` superuser; all four seed groups; service accounts listed but only deactivate/revoke; `kof-website-drafters` created with **no** app access until the owner decides (one attribute later). |

### 0.2 Phase 0 findings (2026-10-11, verified on Authentik 2026.8.3)

The four open points of revision 2 were resolved against a disposable Authentik 2026.8.3 (Testcontainers contract tests in `QRun-IO/qbit-authentik`). Six further facts changed the design; each is pinned by a contract test, so an Authentik upgrade that changes it fails CI.

| # | Finding | Design consequence | Test |
|---|---|---|---|
| i | **Global role permissions are declarative**: a blueprint `authentik_rbac.role` entry accepts `attrs.permissions: [app.codename, …]` (a blueprint-only `RoleSerializer` field). Create and update both call `role.assign_perms(...)`, so with `state: present` the list is re-asserted **additively** on every apply. Matching is by codename, so `view_group` also grants Django's unused `auth.view_group` (harmless). | The qbit's role is `present` with `permissions` only (§4.3); the lint allows exactly that (§4.5). No runbook `assign` call. | `BootstrapBlueprintIT`, `BlueprintReapplyIT` |
| ii | **Every route into superuser ancestry is refused** without `enable_group_superuser`: `add_user` to a superuser group or any descendant (403); `parents` set to a superuser group or a descendant, creating a group under one, user `groups` PATCH, group `users` PATCH, `is_superuser` true or false (400). | Escalation floor confirmed; no residual risk on that side. | `SuperuserFloorIT` |
| ii-b | **The lockout direction is NOT refused**: with `remove_user_from_group` / `change_user` / `change_group` the service account can remove members from `authentik Admins`, deactivate a superuser (break-glass), strip `qqq.io/protected`, empty a superuser's groups and rename the superuser group. | Documented residual risk (§5.1); the guard is the only control; recovery is cluster access (`ak shell`). | `SuperuserFloorIT` |
| iii | **`check_access?for_user=` works only for superusers.** For anyone else it silently evaluates the **caller** (no error). `POST /policies/all/{uuid}/test/` fails with HTTP 500 (`WrongAppError`) for non-superusers on expression policies. The application **list** shows a non-superuser only the apps it can itself access (`superuser_full_list` is superuser-only); GET by slug works with `view_application`. | The qbit computes app access itself (§5.1, §8.2): `qqq.io/app-access` (the attribute-driven sign-in policy) plus the app's group/user bindings; other expression policies show "not evaluated". `applicationsInScope` must be explicit slugs. `view_user_applications` dropped. | `AccessCheckIT`, `SignInPolicyIT` |
| iv | **`metaapplyblueprint` ordering works on an empty instance**: discovery creates an instance per file synchronously, then applies each asynchronously; the bootstrap's first entry applies the infrastructure blueprint inside its own transaction, so its `created` stage entry finds the stage. If the infra instance does not exist yet, `required: true` fails **only** the bootstrap blueprint (separate file), which retries on the next discovery. Applies (including `POST …/apply/`) are asynchronous: wait for `last_applied` to change. | §4.3 note made definitive. | `BootstrapBlueprintIT` (h) |
| v | **Any user can mint API tokens for itself and read their keys** (`POST /core/tokens/` without `add_token`, the `user` field is forced to the caller; `view_key` on that token returns 200). | Service account tokens beyond the expected one are flagged by the drift report; rotation checklist checks the token list (§9.1). | `SuperuserFloorIT` |
| vi | API details: multi-value filters are **repeated plain names** (`type=a&type=b`); the bracketed form `type[]=a` is **silently ignored** (unfiltered results). The user detail `GET /core/users/{id}/` takes no `include_*` parameters (it always returns `groups_obj` and `roles_obj`). **Groups have no `last_updated`.** Listing tokens without `view_token` returns the caller's own tokens. Java's HttpClient h2c upgrade on plain http is rejected ("400 Invalid HTTP request received"); use HTTP/1.1. | FilterMapper and schema drift test (§6.1, §10); group stale-check and reconcile changed (§5.4, §7.2). | `SchemaDriftTest`, `QbitReadContractIT` |

---

## How it works (plain-language overview)

This section is written for the owner. The rest of the spec has the detail.

**One database, two front doors.** Every user, group and membership lives in Authentik's Postgres database. There are two ways to change it:
- the **Identity** screens in the QQQ admin (this qbit)
- Authentik's own admin UI at `auth.kof22.com/if/admin/`

Both call the same Authentik code, which applies the same validation. QQQ does not keep its own list of users that could disagree with Authentik.

**How QQQ talks to Authentik.**
- The qbit holds an API token for a dedicated Authentik **service account**, `qqq-kof22-admin-idadmin`.
- When you open *Users*, QQQ asks Authentik for the list right then. When you click *Add to groups*, QQQ calls Authentik's `add_user` endpoint.
- The service account's Authentik permissions are the outer fence. It can manage ordinary users and groups, but Authentik refuses if it tries to make anyone a superuser, read token secrets or impersonate.

**How the two front doors stay in sync.**
- QQQ reads live, so a change made in the Authentik UI shows up in QQQ on the next screen load (allowing for a 30-second cache).
- For things QQQ keeps a small copy of (the Web Users join and home-page counts), Authentik sends a **webhook** to QQQ whenever a user or group changes. QQQ refreshes just that record through the ESB.
- A 15-minute background check catches anything a webhook missed.

**Where Git still fits.** Git (cluster-gitops blueprints) keeps the **plumbing** that should be reviewed and versioned:
- the login, recovery and enrollment flows
- the OAuth2 provider and the admin application
- the sign-in policy
- branding

Git also *creates* a few starting objects if they don't exist yet:
- the service account
- the break-glass admin
- the `identity-admins` group

These starting objects use `state: created`. Authentik's blueprint importer skips an object that already exists, so **a blueprint re-apply never changes or reverts it**. Blueprints never list group members, so a re-apply can never remove someone you added. Re-applies happen hourly and on every Git change.

**How permissions work.**
- Each group can carry a list of QQQ permissions, for example `kof-website-publishers` → `cms.publish`, or `identity-admins` → `authentik.*`. The list is stored on the group in Authentik as the attribute `qqq.io/permissions`, and you edit it in QQQ on the group's page.
- When someone signs in, QQQ looks at their groups, including parent groups, and gives them the union of those permissions.
- To give someone a permission, add them to a group that has it. To give a whole group a new permission, edit the group.
- **Who may use the Identity screens is itself just a permission** (`authentik.users.manage`, `authentik.memberships.manage`, …). At bootstrap you hold all of them through `identity-admins`.

**Who can sign in to the admin at all.** The Authentik sign-in policy, kept in Git, changes from "is in `kof22-admins` or `kof-website-publishers`" to "is in any group whose `qqq.io/app-access` attribute lists this app". Access then becomes data you can manage in QQQ: a new group can be given admin sign-in without a Git change.

**Safety rails you'll notice.**
- You can't grant a permission you don't hold. You can't edit your own account from the bulk tools.
- Break-glass and superuser accounts show a lock and can't be changed from QQQ.
- QQQ refuses to remove the last person who can administer identities.
- Destructive actions (deactivate, reset MFA) need a reason and typed confirmation.
- Everything is logged in QQQ's *Audit*, and Authentik's event log shows "via QQQ by james@kof22.com".

**Adding a user, end to end.**
1. In *Users*, choose *Create user*. Enter name, email and groups, then confirm.
2. QQQ creates the user in Authentik, adds the memberships, and has Authentik email a set-your-password link. QQQ never sees the password.
3. The webhook updates QQQ's copy in seconds. The new user signs in, and QQQ gives them their groups' permissions.

**If QQQ is down, or for emergencies.** Use the Authentik admin UI. It is the same data, and QQQ picks up the changes when it's back. The break-glass admin is a superuser that QQQ can't touch.

**Backups and history.**
- The authoritative backup is the CNPG Postgres backup of Authentik's database.
- An optional nightly job writes a read-only YAML snapshot of users, groups and memberships to a Git repo, giving a diffable "who had access when". **It is never applied back.**

---

## 1. Goals, non-goals, personas, scenarios

### 1.1 Goals

- One reusable qbit for any QQQ app with an Authentik IdP. It is turned on with configuration: base URL, token reference, features, read-only mode and user scope.
- **Manage identities in QQQ**:
  - create, invite, edit, deactivate and reactivate users
  - create groups (with parents) and edit them
  - add and remove memberships (bulk)
  - edit the QQQ permissions and app access a group grants
  - send password-reset email, revoke sessions, reset MFA, revoke app passwords
  - later: Authentik RBAC roles
- **See** who has access and why:
  - groups (hierarchy), effective QQQ permissions per user, roles
  - application access (computed by the qbit from `qqq.io/app-access` and the app's bindings, §0.2 iii) and policy bindings
  - sessions, MFA devices (metadata only), tokens (metadata only), invitations, events
- **Work alongside the Authentik admin UI**, with no lost updates and no "wrong" place to make a change.
- **Integrate with the host app.**
  - Web Users shows groups, last login and lockout state.
  - Admin-home widgets.
  - The reusable `AuthentikGroupPermissionMapper` replaces hand-written group→permission customizers.

### 1.2 Non-goals (v1)

- **No editing of flows, stages, providers, applications, policies, scope mappings or brands.** These remain infrastructure in Git; QQQ shows them read-only.
- **No impersonation.** It is session-cookie based and too risky to proxy.
- **No setting passwords, showing recovery links or showing token keys.**
- **No hard delete of users.** Deactivate only.
- **No Enterprise-only features**: Lifecycle/offboarding, account lockdown, access requests, CSV export.
- **No Authentik RBAC role management in v1.** That is phase 4. QQQ permissions are attribute-driven and available in v1.
- **No generic multi-IdP SPI yet.** Names are neutral where cheap.
- **No Material Dashboard UI.**

### 1.3 Personas

| Persona | kof22 example | Typical grants |
|---|---|---|
| Identity admin | James | All `authentik.*` (bootstrap) |
| Delegated manager | A future editor-in-chief | `authentik.users.read`, `authentik.memberships.manage`. Can only add people to groups whose permissions they already hold. |
| Auditor | A read-only admin | `authentik.users.read`, `authentik.audit.read` |
| Platform owner | Whoever runs cluster-gitops | Owns the infrastructure blueprints; not involved in routine identity changes |

### 1.4 Scenarios (kof22)

**A. "Add Sam as a publisher."**
- James opens Users → *Create user* and enters name, email and group `kof-website-publishers`.
- The preview shows the effective QQQ permissions Sam will get (`cms.publish`, …) and the computed access result for `kof22-website-prod` ("will be able to sign in: yes", from `qqq.io/app-access` and the app's bindings; not Authentik's `check_access`, §0.2 iii).
- James confirms. QQQ calls `POST /core/users/` and `add_user`, then `recovery_email`, so Sam sets his own password.
- The local audit row and the Authentik event are written, and the webhook refreshes the mirror.

**B. "Make publishers able to see analytics."** On the group `kof-website-publishers`, James opens *Edit permissions* and adds `kof22.analytics`.
- The guard checks that James holds `kof22.analytics` himself.
- The preview lists the 4 members who gain the permission.
- The change applies at each member's next session refresh (§5.6).

**C. "Lock out a contractor now."** James opens the contractor's user → *Deactivate*.
- The preview shows live sessions and app passwords.
- James types the username to confirm. QQQ sends `PATCH is_active=false`, which in 2026.8 also deletes the user's sessions, and optionally revokes app passwords.
- The guard refuses this against James himself, the break-glass admin and the service account.

**D. "Someone changed groups in the Authentik UI."** An admin adds a user to `kof22-admins` directly in Authentik.
- Authentik fires `model_updated`, and the webhook makes QQQ refresh that user.
- QQQ's audit view shows the change from Authentik's event log, with the actor and "via Authentik UI". Nothing is reverted, because both doors are legitimate.

---
## 2. Research summary

### 2.1 Authentik 2026.8.3 API surface used

Facts come from the live OpenAPI schema at `https://auth.kof22.com/api/v3/schema/` (`info.version: 2026.8.3`, fetched without authentication), from source at tag `version/2026.8.3`, and from the docs (Appendix B). The base path is `/api/v3`. Every list endpoint takes `page`, `page_size`, `ordering` and `search`.

| Area | Endpoints | Notes that matter for the design |
|---|---|---|
| Users | `GET/POST /core/users/`, `GET/PUT/PATCH/DELETE /core/users/{id}/` (int pk) | Filters: `username`, `email`, `name`, `uuid`, `is_active`, `is_superuser`, `groups_by_name`, `groups_by_pk`, `roles_by_*`, `type` (multi-value filters are **repeated plain names**; the `[]` form is silently ignored, §0.2 vi), `path`, `path_startswith`, `last_login__gt/__lt/__isnull`, `last_updated__gt`, `date_joined__*`, `attributes` (JSON), `include_groups`, `include_roles`. `search` also accepts a query language (`QLSearch`). `is_superuser` is derived and read-only. Types: `internal`, `external`, `service_account`, `internal_service_account`. |
| User actions | `POST /core/users/{id}/set_password/`, `/set_password_hash/`, `/recovery/` (returns a link), `/recovery_email/` (needs `email_stage` uuid), `/impersonate/`, `GET /core/users/impersonate_end/`, `POST /core/users/service_account/` (returns a token), `GET /core/users/me/`, `GET /core/users/paths/` | Password actions need `authentik_core.reset_user_password`. Adding a user to a group whose **ancestry** includes a superuser group needs `enable_group_superuser`. Assigning roles needs `authentik_rbac.change_role`. |
| Sessions | `GET /core/authenticated_sessions/` (filter `user__username`), `DELETE …/{uuid}/`, `DELETE …/bulk_delete/?user_pks=` | Fields: `last_ip`, `last_user_agent`, `last_used`, `expires`, `geo_ip`, `asn`. Deactivating a user deletes their sessions (2026.8). |
| MFA devices | `GET /authenticators/admin/all/?user=<pk>`. Per type, CRUD on `/authenticators/admin/{totp,webauthn,static,duo,sms,email}/{id}/` | Device metadata only: name, type, confirmed, created, last_used. |
| Tokens | `GET/POST /core/tokens/`, `GET/PUT/PATCH/DELETE /core/tokens/{identifier}/`, `GET …/view_key/`, `POST …/set_key/` | Intents: `api`, `app_password`, `recovery`, `verification`. Keyed by identifier. `view_token_key` is a separate permission, and the qbit must never hold it. **Any user may create tokens for itself and read their keys** without `add_token` (§0.2 v). |
| Groups | `GET/POST /core/groups/`, `GET/PUT/PATCH/DELETE /core/groups/{uuid}/`, `POST …/add_user/` and `…/remove_user/` (`{pk}`) | **`parents` is many-to-many since 2025.12.** Groups inherit roles **and superuser status** from every ancestor. Names are unique (2025.12). Filters: `members_by_username` (repeat the plain name for several values), `include_users`, `include_children`, `include_parents`, `include_inherited_roles`, `is_superuser`, `attributes`. Custom permissions: `add_user_to_group`, `remove_user_from_group`, `enable_group_superuser`, `disable_group_superuser`. |
| RBAC | `GET/POST /rbac/roles/`, `…/{uuid}/add_user/` and `…/remove_user/`, `GET /rbac/permissions/` (catalogue), `GET /rbac/permissions/assigned_by_roles/?model=&object_pk=`, `POST …/assigned_by_roles/{uuid}/assign/`, `PATCH …/unassign/`, `GET /rbac/permissions/roles/?uuid=`, `/rbac/initial_permissions/` | **RBAC has been fully role-based since 2025.12.** Roles are assigned to users directly or through groups; there are no direct user permissions. `assign` with `object_pk` grants an object permission; without it, a global one. Initial permissions automatically grant a role permissions on objects that its members create. |
| Apps and policies | `GET /core/applications/{slug}/`, `GET /core/applications/{slug}/check_access/?for_user=`, `GET/POST /policies/bindings/` (target = `pbm_uuid`, plus exactly one of policy/group/user), `/policies/all/`, `POST /policies/all/{uuid}/test/` | **`for_user` is honoured only for superusers** (silently ignored otherwise), the policy test endpoint returns 500 for non-superusers, and the app list shows a non-superuser only its own apps (§0.2 iii). Bindings can expire (2026.8). |
| Events | `GET /events/events/` (filters `action`, `actions` (repeated plain name), `username`, `client_ip`, `context_model_app`, `context_model_name`, `context_model_pk`, `context_authorized_app`), `POST /events/events/` (custom `custom_*` actions), `/actions/`, `/volume/`, `/top_per_user/` | Relevant actions: `login`, `login_failed`, `logout`, `password_set`, `model_created/updated/deleted`, `impersonation_*`, `secret_view`, `invitation_used`, `user_write`, `suspicious_request`, `custom_`. |
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
- **Many-to-many fields are replaced, not merged.** If a `present` entry lists `users:` on a group, or `groups:` on a user, the serializer *replaces* the whole set on every apply. Any membership added through the API is wiped within an hour. The same is true of `parents` and `roles`. This follows from the importer building a `partial=True` serializer on the existing instance and saving it for `present` entries (verified in source, §4.2). Contract test (d) in §10 proves it.
- **Blueprint entries can carry `permissions:`** (object permissions, 2024.8+). The bootstrap blueprint uses this mechanism to grant the qbit's role its object permissions (§4.3).
- **`state: created` never re-saves an existing object, but it still re-applies the entry's `permissions:`, additively** (verified in `importer.py`, §4.2). That is the basis of the bootstrap pattern.
- **No built-in per-object marker.** Objects created by a blueprint carry no "owned by blueprint X" marker that the API can read. That doesn't matter once blueprints stop owning identity data (§4).

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
│ AUTHENTIK_ACCESS_MATRIX, AUTHENTIK_HEALTH widgets (+ admin-home widgets)  │
└──────────────┬───────────────────────────────────────────────────────────┘
               │ /qqq/v1/... (normal QQQ routes)   /qqq/v1/authentik/* (widgets)
┌──────────────▼──────────────── qbit-authentik (Java) ────────────────────┐
│ AuthentikQBitProducer / AuthentikQBitConfig                              │
│  ├─ API backend "authentikApi" ── AuthentikActionUtil (BaseAPIActionUtil) │
│  │     ├─ AuthentikGuard   (QQQ permission, escalation, protection)       │
│  │     ├─ FilterMapper     (QQueryFilter → Authentik query params)        │
│  │     └─ Throttle         (token bucket, bulk pacing)                    │
│  ├─ EffectivePermissionResolver (groups + ancestors → qqq.io/permissions) │
│  ├─ processes (table/record actions; preview→review→execute)              │
│  ├─ AdminActionAuditor     (local audit row + Authentik custom event)     │
│  ├─ Mirror tables (RDBMS) + MirrorSync (webhook→ESB + reconcile)          │
│  ├─ AuthentikWebhookRouteProvider (/qqq/v1/authentik/webhook)             │
│  ├─ IdentitySnapshotExport (optional nightly read-only YAML → git)        │
│  └─ AuthentikGroupPermissionMapper (auth customizer; uses the resolver)   │
└──────────────┬───────────────────────────────────────────────────────────┘
               │ Bearer token of service account "qqq-<appKey>-idadmin"
┌──────────────▼──────────────── Authentik (source of truth) ──────────────┐
│ DB: users, groups, memberships, attributes, roles   ◄── Authentik admin UI│
│ role qqq-<appKey>-idadmin (bootstrap blueprint, present, perms only)      │
│ notification rule + event_matcher policy + webhook transport → QQQ        │
│ blueprints: flows/stages/providers/apps/policies/branding (present)       │
└───────────────────────────────────────────────────────────────────────────┘
```

**Packaging.** Repo `QRun-IO/qbit-authentik`; Maven `com.kingsrook.qbits:qbit-authentik`; package `com.kingsrook.qbits.authentik`; QBit identity `com.kingsrook.qbits:authentik`; parent `qbit-build-parent` 2.0.0. Next UI widgets ship in `qqq-frontend-next` (§8.4). There is one module.

### 3.2 `AuthentikQBitConfig`

| Setting | Default | Meaning |
|---|---|---|
| `baseUrl` | required | Authentik host root, e.g. `https://auth.kof22.com`. `/api/v3` is appended. This is separate from the OAuth2 issuer URL, as MHP learned. |
| `apiToken` | required | Usually `${env.AUTHENTIK_API_TOKEN}`. Interpolated by the qbit (because `APIBackendMetaData.enrich()` is disabled) and redacted in `toString()` |
| `tableNamePrefix` / namespace | `authentik` | Prefix for tables (joined with `_`: `authentik_user`), processes and widgets (camel-cased: `authentikResyncMirror`) |
| `features` | `{READ}` | A subset of `READ`, `USERS` (create, invite, edit, deactivate), `GROUPS` (create, edit, permissions and app-access attributes), `MEMBERSHIP`, `CREDENTIALS` (recovery email, MFA reset, app-password revoke), `SESSIONS`, `RBAC` (phase 4), `ACCESS_MATRIX`, `MIRROR`, `WEBHOOK`, `SNAPSHOT_EXPORT`. A feature that is off produces no metadata. |
| `readOnly` | `false` | Hard kill switch. Every write is refused. |
| `appKey` | required | Identifies this QQQ app in attributes, audit and the app-access attribute, e.g. `kof22-admin` |
| `userScope` | `internal` + `external` users | Optional `path_startswith` and/or `groups_by_name` filter, to partition a shared Authentik between apps. Out-of-scope users are invisible and cannot be written. |
| `groupScope` | all non-superuser groups | Optional name prefix or attribute filter (`qqq.io/app: <appKey>`) for multi-app Authentiks |
| `protectedUsernames` | `[]` | Added to the built-in protected set: the service account, users with `qqq.io/protected: true`, and superusers (§5.4) |
| `bootstrapOperators` | `${env.AUTHENTIK_QBIT_BOOTSTRAP_OPERATORS}` (empty) | Emails that receive all `authentik.*` permissions even if the group attributes are wrong. A lockout escape hatch that logs a warning while in use; intended to be emptied after go-live. |
| `permissionCatalog` | derived | The QQQ permission names that can be granted through `qqq.io/permissions`, taken from `PermissionsHelper.getAllAvailablePermissions` plus the `authentik.*` names. Unknown names are rejected. |
| `recoveryEmailStage` | required if `CREDENTIALS` or `USERS` | Email stage slug or uuid for `recovery_email`, e.g. `kof22-recovery-email` |
| `invitationFlow` | required for invites | Enrollment flow slug, e.g. `kof22-enrollment` |
| `newUserPath` | `users` | Authentik `path` for users QQQ creates |
| `applicationsInScope` | all visible | Application slugs shown in the access matrix and the app-access editor |
| `confirmations` | see §6.3 | Per-action confirmation level: `CONFIRM`, `TYPED`, `TWO_PERSON` |
| `throttle` | 10 req/s, burst 20; bulk pacing 100 ms | Self-imposed; authenticated calls are not throttled server-side |
| `cacheTtl` | 30 s lists, 60 s group attributes, 5 min computed app access | Memoization TTLs |
| `mirror` | backend name, 15-minute reconcile, 30-day inbox retention | Used if `MIRROR` is on |
| `webhookSecret` | `${env.AUTHENTIK_WEBHOOK_SECRET}` | Shared secret in a header set by the transport's header mapping |
| `esbDestinationName` | optional | If set, webhook events go through the ESB; otherwise they are processed inline |
| `snapshotExport` | off | Git remote, branch, path and schedule for the optional nightly export (§7.3) |
| `expectedServerVersion` | `2026.8` | A major or minor mismatch logs a warning at startup and in the health widget |

`validate()` checks:
- the URL is well formed and the token is non-blank
- feature dependencies are present: `USERS` and `CREDENTIALS` need `recoveryEmailStage`, `MIRROR` needs an RDBMS backend, `WEBHOOK` needs a secret, `SNAPSHOT_EXPORT` needs a remote
- the prefix rules hold

---

## 4. Source of truth, blueprints and GitOps

### 4.1 The split

| Concern | Lives in | Edited from | Blueprint state |
|---|---|---|---|
| Users, groups, memberships, group parents, group attributes (`qqq.io/permissions`, `qqq.io/app-access`), user attributes | **Authentik DB** | QQQ qbit **and** Authentik admin UI | none (data); bootstrap seeds only, as `created` |
| Authentik RBAC roles and their assignments | Authentik DB | Authentik UI (v1); QQQ (phase 4) | the qbit's own role: `present`, `permissions` only (§4.3) |
| Flows, stages, prompts, email templates | Git | cluster-gitops PR | `present` |
| OAuth2/other providers, applications, scope mappings | Git | cluster-gitops PR | `present` |
| Policies and policy bindings (incl. the app sign-in policy) | Git | cluster-gitops PR | `present` |
| Brands, branding | Git | cluster-gitops PR | `present` |
| Notification transport, rule and event matcher policies for the qbit webhook | Git | cluster-gitops PR | `present` |
| API tokens and secrets | Authentik DB + Infisical | Authentik UI (manual, once), Infisical | never in blueprints |

The rule of thumb is that **anything you'd change as part of running the business is data; anything that changes how login works is infrastructure.**

### 4.2 Why `state: created` is safe (verified in source)

The 2026.8.3 importer, `authentik/blueprints/v1/importer.py` at tag `version/2026.8.3`:

- **Existing objects are skipped.** `_validate_single` looks the object up by `identifiers`. For an existing object and any state other than `must_created`, it builds a *partial* serializer on that instance. Then `_apply_models` does this:
  ```python
  if instance and not instance._state.adding and state == BlueprintEntryDesiredState.CREATED:
      self.logger.debug("Instance exists, skipping", ...)
  else:
      instance = self._save_with_retry(serializer, entry, raise_errors)
  ...
  self._apply_permissions(instance, entry)
  ```
  So for `created`, an existing object is **never saved**. Attributes, members, parents and names edited in QQQ or the UI are left alone.
- **Permissions are re-applied every run, additively.** `_apply_permissions` runs for existing `created` objects too, and only calls `role.assign_perms(...)` / `assign_perms_to_managed_role(...)`. It never removes permissions. This is what we want for the service account's role: if someone strips a permission in the UI, the next apply puts it back. It never takes away permissions granted outside the blueprint.
- **Validation still runs on existing objects.** `serializer.is_valid()` runs before the skip, and the whole blueprint is applied inside one `atomic()` transaction. An invalid bootstrap entry, such as a `!Find` that returns nothing, therefore **rolls back the entire blueprint**, infrastructure included. Mitigations:
  - Bootstrap entries live in **their own blueprint file** (`kof22-identity-bootstrap.yaml`), separate from `kof22-oauth2-apps.yaml`.
  - Bootstrap entries keep `attrs` minimal.
  - The qbit's health widget shows blueprint instance `status`.
- **Removing an entry deletes nothing.** Only `state: absent` deletes. Dropping a user or group entry from a blueprint leaves the object in place, as plain data.
- **When re-applies happen** (docs): on blueprint file change (the directory is watched), on the 60-minute `blueprints_discovery` schedule, and on manual `apply`.
- **Why `present` is unsafe for identity data.** A `present` entry re-saves its `attrs` with `partial=True` on every apply. Many-to-many attrs (`users:` on a group, `groups:` on a user, `parents`, `roles`) are *replaced*, not merged. So a `present` group listing `users:` wipes API-added members at least hourly. A `present` entry with `attributes:` replaces the whole attributes dict, wiping `qqq.io/permissions` edits. This is the MHP APPS-1462 failure mode.

### 4.3 Bootstrap blueprint (kof22)

This goes in a new ConfigMap key, `kof22-identity-bootstrap.yaml`. Every identity entry is `created`, and no entry lists members. This is the exact file applied by the contract tests (`QRun-IO/qbit-authentik` `src/test/resources/blueprints/kof22-identity-bootstrap.yaml`) and proposed in KofTwentyTwo/cluster-gitops#72; phase 0/1 holds read permissions only, later phases append to the role's list.

```yaml
# Test fixture: the kof22 identity bootstrap blueprint (DESIGN.md section 4.3),
# in the shape proposed for cluster-gitops. Identity objects are
# `state: created`: created when missing, never updated, never given members.
version: 1
metadata:
  name: kof22-identity-bootstrap
  labels:
    blueprints.goauthentik.io/instantiate: "true"
entries:
  # Apply the infrastructure blueprint first (it owns the recovery email stage
  # referenced below). On an empty instance this is what makes the bootstrap
  # apply cleanly (contract test h).
  - model: authentik_blueprints.metaapplyblueprint
    attrs:
      identifiers:
        name: kof22-oauth2-apps
      required: true

  # The role the qbit service account runs as. It is the qbit's own
  # infrastructure, not identity data, so it is `present`: attrs carry ONLY
  # `permissions` (global permissions; a blueprint-only RoleSerializer field),
  # which are assigned additively on every apply, so a permission removed in
  # the UI comes back within the hour and nothing granted elsewhere is taken
  # away. No users, no groups. Phase 0/1 = read-only; later phases append here.
  # Note: matching is by codename, so view_group also grants Django's unused
  # auth.view_group (harmless).
  - model: authentik_rbac.role
    state: present
    id: qqq-role
    identifiers:
      name: qqq-kof22-admin-idadmin
    attrs:
      permissions:
        - authentik_core.view_user
        - authentik_core.view_group
        - authentik_core.view_application
        - authentik_core.view_authenticatedsession
        - authentik_rbac.view_role
        - authentik_policies.view_policybinding
        - authentik_events.view_event
        - authentik_events.add_event
        - authentik_blueprints.view_blueprintinstance

  # The qbit service account. Its API token is minted once by hand (never here).
  - model: authentik_core.user
    state: created
    id: qqq-sa
    identifiers:
      username: qqq-kof22-admin-idadmin
    attrs:
      name: QQQ kof22 Admin identity service
      type: service_account
      path: service-accounts/qqq
      roles:
        - !KeyOf qqq-role
      attributes:
        qqq.io/protected: true

  # Object permission on a stage the infrastructure blueprint owns: the bare
  # `created` entry is skipped (the stage exists), but its permissions are
  # still applied, additively, on every run.
  - model: authentik_stages_email.emailstage
    state: created
    identifiers:
      name: kof22-recovery-email
    permissions:
      - role: !KeyOf qqq-role
        permission: authentik_stages_email.view_emailstage

  # Seed groups: created once, then plain data. No users, no member lists.
  - model: authentik_core.group
    state: created
    identifiers:
      name: identity-admins
    attrs:
      attributes:
        qqq.io/permissions: ["authentik.*"]
        qqq.io/app-access: ["kof22-website-prod"]
  - model: authentik_core.group
    state: created
    identifiers:
      name: kof22-admins
  - model: authentik_core.group
    state: created
    identifiers:
      name: kof-website-publishers
  # No qqq.io/app-access yet: drafters sign in to the admin only once the owner
  # decides to set that one attribute (DESIGN.md section 11.2 item 6).
  - model: authentik_core.group
    state: created
    identifiers:
      name: kof-website-drafters

  # Break-glass admin: a superuser the qbit cannot touch. Password set once by
  # hand (1Password); recovery through `ak create_recovery_key`.
  - model: authentik_core.user
    state: created
    identifiers:
      username: kof22-breakglass
    attrs:
      name: Break-glass admin
      type: internal
      path: users/breakglass
      groups:
        - !Find [authentik_core.group, [name, "authentik Admins"]]
      attributes:
        qqq.io/protected: true
```

Notes (resolved in phase 0, §0.2):
- **Global permissions** are the role's `attrs.permissions` (finding i). The role is `present` with nothing but `permissions`, so it is re-asserted additively on every apply and carries no users or groups. `roles:` on the `created` service account applies on first create only; the role's later permission changes still reach it.
- **Ordering on a fresh install** (finding iv): the leading `metaapplyblueprint` entry applies `kof22-oauth2-apps` first, inside the bootstrap's transaction. Verified on an empty 2026.8.3. If the infra instance did not exist yet, only the bootstrap fails, and it retries on the next discovery.
- **Object permission** on `kof22-recovery-email` (needed by `recovery_email`, phase 3) is applied through the bare `created` stage entry's `permissions:` on every run.
- **Seeding James's membership.** `groups:` on a `created` user only applies when the user is first created. James's account already exists, so his `identity-admins` membership is a one-time manual step in the Authentik UI (owner checklist item 10). `bootstrapOperators` covers the window before it's done.
- **Break-glass password.** The break-glass admin has no usable password from the blueprint. Its password is set once by hand and stored in 1Password. Its recovery path is `ak create_recovery_key` on the worker pod.

### 4.4 Migration from today's blueprint

Today `kof22-oauth2-apps.yaml` has `present` entries for group `kof22-admins` and user `james.maes` (with `groups: [authentik Admins, kof22-admins]`). Everything else in it is infrastructure. The migration runs in this order, so no re-apply can wipe data in between:

1. **Freeze.** Note the current live memberships: `GET /core/users/?include_groups=true`, or the Authentik UI. Optionally run the first snapshot export (§7.3).
2. **Split the file** in one cluster-gitops PR:
   - **Remove** the `authentik_core.user james.maes` entry and the `authentik_core.group kof22-admins` entry from `kof22-oauth2-apps.yaml`. Per §4.2, removing an entry deletes nothing, so James's account, his memberships and the group all stay.
   - **Add** `kof22-identity-bootstrap.yaml` (§4.3) to the ConfigMap, with `kof22-admins` re-declared there as `created` (no attrs). It is then skipped on every apply.
   - **Change the app sign-in policy** `kof22-require-admins-group` from a name list to attribute-driven access, keeping the old names as a transition fallback:
     ```python
     appkey = "kof22-website-prod"
     for g in request.user.all_groups():
         if appkey in g.attributes.get("qqq.io/app-access", []):
             return True
     return ak_is_group_member(request.user, name__in=["kof22-admins", "kof-website-publishers"])  # remove after step 5
     ```
   - **Add** the webhook transport, body/header mappings, rule and event-matcher policies (§7.1) as `present` infrastructure, **inert** until phase 2: the rule has no destination (`destination_event_user: false`, no group), so nothing is created or sent (Authentik's notification engine also never fires a rule without policies: `empty_result = False`). The ExternalSecret key for the webhook secret ships with phase 2, so a missing Infisical key cannot put `authentik-env` into sync error.
   - Add a CI lint to cluster-gitops (§4.5).
3. **Apply.** ArgoCD syncs the ConfigMap, the worker sees the file change and applies both blueprints. Then check:
   - Both instances have `status: successful`.
   - James still holds `authentik Admins` and `kof22-admins` (unchanged, because the entries were removed, not changed).
   - The break-glass admin, service account, role and seed groups exist.
4. **Mint the token and seed.**
   - Mint the service-account API token in the Authentik UI (90-day expiry) and store it in Infisical.
   - Add James to `identity-admins`, and set `qqq.io/app-access` on `kof22-admins` and `kof-website-publishers`.
   - Set `qqq.io/permissions` on `kof22-admins` to match today's hard-coded `ADMIN_PERMISSIONS`.
5. **Cut over the backend.** Deploy Website-Backend with the qbit and the mapper replacing `AuthentikAuthenticationCustomizer`. Then remove the name fallback from the sign-in policy in a follow-up PR.
6. **Prove it.** Add a test member through the API and run `POST /managed/blueprints/{uuid}/apply/` on both instances. The member must survive. This is also contract test (c) in §10.

**Rollback.** Re-adding the old `present` entries restores today's behaviour, and would re-pin James's groups. The sign-in policy fallback keeps logins working throughout.

### 4.5 Guarding against regressions

- **cluster-gitops CI lint.** A small script fails the PR if any blueprint has:
  - (R1) `state: present` (or a missing state, which defaults to present) on `authentik_core.user` or `authentik_core.group`
  - (R2) a member list (`users:`) on any group entry. `groups:`/`roles:` on a `created` user stay allowed: they seed only on first create (break-glass, the service account)
  - (R3) an `authentik_rbac.role` entry that is `present` with anything besides `permissions` (and `name`)
  - (R4) a literal `password`, `password_hash`, `key`, `token` or `client_secret` (use `!Env`), or any `authentik_core.token` entry
  - Implemented as `scripts/lint-authentik-blueprints.py` (with `--self-test`) plus a GitHub workflow in cluster-gitops#72. On today's `main` it flags exactly the two entries the migration removes.
- **qbit health widget.** It shows:
  - blueprint instance status and `last_applied`
  - a **reversion detector**: an Authentik `model_updated` event on a user or group whose actor is the blueprint system task within 2 minutes of a QQQ write to the same object is flagged "possibly reverted by blueprint"
  - unmanaged infrastructure the qbit relies on (recovery stage, enrollment flow) that is missing

### 4.6 Rejected alternatives

- **Blueprints own identities, and QQQ opens PRs.** Membership changes would wait on review and an ArgoCD sync, which conflicts with decision 1.
- **A QQQ database as the master, pushed to Authentik.** That creates two sources of truth and makes Authentik UI edits second-class, which conflicts with decision 1.
- **Ownership markers with refusal of blueprint-owned objects** (revision 1). This is no longer needed, because blueprints no longer own identity data.

---

## 5. Permission and safety model

### 5.1 Authentik side: the service account's role

The role `qqq-<appKey>-idadmin` is assigned directly to the service account (bootstrap blueprint). The service account is in **no groups**, so it has no superuser ancestry and no app sign-in.

| Permission (global unless noted) | Why | Phase |
|---|---|---|
| `authentik_core.view_user`, `view_group`, `view_application`, `view_authenticatedsession`, `authentik_rbac.view_role`, `authentik_policies.view_policybinding`, `authentik_events.view_event`, `authentik_blueprints.view_blueprintinstance` | Read screens, access computation, drift report (the phase 0/1 set in §4.3) | 1 |
| `authentik_core.view_token`, `authentik_stages_invitation.view_invitation`, device `view_*` | Token, invitation and MFA lists | 3 |
| (none) for the access matrix: `check_access?for_user=` cannot be used by a non-superuser (§0.2 iii); the qbit evaluates `qqq.io/app-access` and the app's bindings itself | Access matrix | 1 |
| `authentik_events.add_event` | `custom_qqq_admin_action` audit events | 1 |
| `authentik_core.add_user`, `change_user` | Create, edit, deactivate, reactivate | 3 |
| `authentik_core.reset_user_password` | `recovery_email` (also allows `set_password`/`recovery`, which the qbit never calls, see §5.5) | 3 |
| `view_emailstage` (object, on `kof22-recovery-email`) | Needed by `recovery_email` | 3 |
| `authentik_core.add_group`, `change_group` | Create groups, edit parents and attributes | 3 |
| `authentik_core.add_user_to_group`, `remove_user_from_group` | Membership | 2 |
| `authentik_core.delete_authenticatedsession` | Revoke sessions | 3 |
| `authentik_core.delete_token` | Revoke app passwords | 3 |
| Device `delete_*` for TOTP, WebAuthn, static, email, SMS | MFA reset | 3 |
| `authentik_stages_invitation.add_invitation`, `change_invitation`, `delete_invitation` | Invites | 3 |
| `authentik_rbac.change_role`, `assign` (object, on non-qbit roles) | Role management | 4 |

**Never granted:**
- `enable_group_superuser` / `disable_group_superuser`. Authentik then refuses every route into superuser ancestry: membership (direct, bulk, by user or by group), `parents` under a superuser group or any descendant, creating a group under one, and flipping `is_superuser` (verified, §0.2 ii).
- `view_token_key`, `set_token_key`, `add_token` (MHP APPS-1462)
- `impersonate`, `delete_user`
- any `change_*` on flows, stages, providers, applications, policies, bindings, brands or blueprints
- any change on its own role. Role permissions are object permissions excluding `qqq-*` roles, and the guard also refuses.

**What Authentik still enforces if QQQ is fully compromised** (verified by contract tests). The attacker could edit ordinary users and groups, deactivate users and send recovery emails to users' *own* addresses. They could not:
- become or create a superuser, or put anyone under superuser ancestry
- read other users' token keys, or create tokens for anyone but the service account
- impersonate anyone or obtain recovery links
- change login flows or policies
- delete anyone

**What Authentik does NOT enforce (residual risk, §0.2 ii-b and v).** With the phase 2/3 permissions the service account can also *lock out* administrators: remove members from `authentik Admins`, deactivate `kof22-breakglass`, strip `qqq.io/protected`, empty a superuser's groups, rename the superuser group. It can also mint additional API tokens for itself that survive a rotation. Controls:
- the guard refuses every write that targets a protected account or a superuser group (§5.4); the contract tests pin that Authentik does not
- phase 2 option: grant `remove_user_from_group` (and `add_user_to_group`) as **object** permissions on the in-scope groups (blueprint `permissions:` plus `initial_permissions` for groups the qbit creates) instead of globally, which removes the "remove from `authentik Admins`" path at the cost of a step for groups created in the UI
- the drift report flags superuser changes made by the service account (event `model_updated` on a superuser user/group with the service account as actor) and service-account tokens beyond the expected one
- recovery from a lockout is cluster access: `kubectl exec` into the Authentik worker and `ak shell` (reactivate, re-add), or `ak create_recovery_key`

This is weaker than revision 1's per-group object permissions. That is the cost of "manage all identities from QQQ" (decision 1). It is partly offset by `groupScope`/`userScope` for multi-app setups, and by the qbit-side guards.

### 5.2 QQQ side: `authentik.*` permissions

| Permission | Grants |
|---|---|
| `authentik.users.read` | Identity app; users, groups, memberships, effective permissions, applications, bindings, blueprints (read) |
| `authentik.users.manage` | Create, invite, edit (name, email, path, type), deactivate, reactivate users; cancel invitations |
| `authentik.groups.manage` | Create groups, rename them, set parents (within `groupScope`), edit non-reserved attributes |
| `authentik.memberships.manage` | Add and remove members (subject to the escalation rule) |
| `authentik.permissions.manage` | Edit a group's `qqq.io/permissions` and `qqq.io/app-access` (subject to the escalation rule) |
| `authentik.roles.manage` | Assign and unassign Authentik RBAC roles (phase 4) |
| `authentik.credentials.reset` | Send recovery email, reset MFA, revoke app passwords |
| `authentik.sessions.revoke` | Revoke sessions |
| `authentik.audit.read` | Local audit log, Authentik events, sessions (IPs, user agents), MFA device lists, user attributes |
| `authentik.approve` | Approve two-person requests |
| `authentik.admin` | Health and drift views, resync mirror, snapshot export, view config |

`authentik.*` in an attribute expands to all of the above. Permissions are granted only through `qqq.io/permissions` on groups, read by the mapper (§5.6). Nothing is hard-coded per app. **Bootstrap:** the `identity-admins` group carries `authentik.*`, and James is its first member (§4.3).

### 5.3 Enforcement point (re: #1061, #1062)

- `AuthentikActionUtil.doQuery/doGet/doInsert/doUpdate/doDelete` call `AuthentikGuard.check(table, op, session)` first. The guard checks the QQQ permission, the feature flag, `readOnly` and scope.
  - A session with no user is refused, except `MirrorSync`'s internal read-only session.
- Every qbit process's first backend step re-checks `checkProcessPermissionThrowing` and the per-record rules (§5.4). It does not trust that the middleware ran.
- Field-level protection does not use `FieldSecurityLock` (#1062):
  - Sensitive columns are removed in `jsonObjectToRecord` without `authentik.audit.read`. These are `attributes` (except the `qqq.io/*` keys, shown under `users.read`), `last_ip`, `user_agent` and `geo_ip`.
  - Filters on those fields are rejected for that session.
- Tables also declare `QPermissionRules`, so the UI hides what the user can't use.
- An ArchUnit test asserts that every write path goes through the guard.

### 5.4 Guardrails (checked at preview and again at execute)

1. **Protected accounts.**
   - Covered: the service account, users with `qqq.io/protected: true` (break-glass), `protectedUsernames`, and users with `is_superuser=true`, including superuser status inherited through `parents`.
   - No action can target them. They show as "skipped: protected (reason)".
   - QQQ cannot set or clear `qqq.io/protected`; only the Authentik UI or a blueprint can.
2. **Protected groups.** Superuser groups and all their descendants cannot be managed. A group's `parents` can't be set to a superuser group or any of its descendants. Any of these would make members superusers (`User.all_groups()` includes ancestors).
3. **No self-edits.** Operators cannot change their own memberships, attributes or active state, or revoke their own sessions.
4. **No escalation (the subset rule).**
   - Every grant is checked against **P(operator)**: the operator's live effective QQQ permissions, resolved from their *current* Authentik groups (cached 30 s), not the session claim.
   - **Adding user U to group G** requires `P(G) ⊆ P(operator)`. P(G) is the permissions G confers, including inherited parents.
   - **Editing G's `qqq.io/permissions`** requires every added permission to be in P(operator). Removing a permission requires only `authentik.permissions.manage`.
   - **Setting G's parents to H** requires `P(H) ⊆ P(operator)`.
   - **App access**: adding an app key to `qqq.io/app-access` requires the operator to have access to that app.
   - Holders of `authentik.*` pass naturally. Delegated managers can only hand out what they hold.
5. **Last-admin protection.** The qbit refuses any change after which no active, non-protected human holds `authentik.memberships.manage` **and** `authentik.permissions.manage`. That covers removing a membership, deactivating, editing permissions and re-parenting. Break-glass doesn't count, because it can't use QQQ.
6. **Bulk caps.** At most 100 targets per run, `TYPED` confirmation above 25, paced by the throttle. A partial failure stops the run with per-record results.
7. **Stale checks.** Each target's state is re-read at execute: users by `last_updated`; groups (which have no `last_updated` in 2026.8, §0.2 vi) by a fingerprint of name, parents, `qqq.io/*` attributes and member count. A record changed since the preview, for example in the Authentik UI, fails with "changed since preview; review again". This is how the two front doors avoid lost updates: QQQ never blind-overwrites a concurrent UI edit. Attribute edits send a merge-patch of only the changed `qqq.io/*` keys, re-read immediately before the write.

### 5.5 Credentials policy

| Action | Allowed | Mechanism |
|---|---|---|
| Create user | Yes (`USERS`) | `POST /core/users/` with **no password** (`is_active=true`, `path=newUserPath`, `attributes.qqq.io/created-by`), then memberships, then `recovery_email` so the user sets their own password |
| Invite user | Yes (`USERS`) | Invitation (`fixed_data` email and name, `single_use`, expires in 7 days) + `send_email`, through the `kof22-enrollment` flow. Groups are applied by QQQ when the `invitation_used` webhook arrives. |
| Send password-reset email | Yes (`CREDENTIALS`) | `recovery_email` with `recoveryEmailStage`, `token_duration` 30 minutes |
| Show or copy recovery link, set password, set hash | **No** | Never called. A link given to the operator amounts to account takeover. |
| View or set token keys; create tokens | **No** | Not permitted for the service account; not exposed |
| Revoke sessions | Yes (`SESSIONS`) | `bulk_delete?user_pks=` / per-session `DELETE` |
| Reset MFA | Yes (`CREDENTIALS`) | Device list, then per-type `DELETE`, with typed confirmation and a reason |
| Revoke app passwords | Yes (`CREDENTIALS`) | `DELETE /core/tokens/{identifier}/`, intent `app_password` only |

### 5.6 `AuthentikGroupPermissionMapper` and `EffectivePermissionResolver`

The mapper is a `QAuthenticationModuleCustomizerInterface` component. It replaces kof22's `AuthentikAuthenticationCustomizer` and generalizes MHP's IAM-style group-attribute policies (MHP ADR 0001).

1. It takes the user's group names from the `groups` claim. The current kof22 `profile` mapping emits **direct** groups only.
2. It resolves each group through `EffectivePermissionResolver`: group attributes plus all ancestors via `include_parents`, from the mirror or the API, cached 60 s. The result is the union of `qqq.io/permissions`, with `authentik.*` and other wildcard prefixes expanded against `permissionCatalog`.
3. It adds static fallbacks if configured, through `withGroupPermissions(...)` (useful in tests and dev), plus `bootstrapOperators`.
4. It sets QQQ session permissions, user, email and full name.

**Freshness.** Permissions are computed at session creation. The QQQ session is re-validated on each access-token refresh (1 hour in kof22), and the mapper runs again then. A webhook on a group or membership change also marks affected sessions in `qqq_user_session` for re-evaluation on their next request (phase 2). Phase 0 checks this is possible through the OAuth2 module's session store. If it isn't, the staleness is up to 1 hour, which is documented.

**Fallback.** If Authentik is unreachable at sign-in, the mapper uses the mirror. If the mirror is empty, it grants only what the static config provides. It never grants everything.

---
## 6. Data model

**Naming (owner rule, 2026-10-12):** "table names should always be lowercase and use _, and qbits should always be prefixed with their name, ie, cdn_ etc". Every table, API-backed or local, is lowercase snake_case with the `authentik_` prefix (`authentik_user`, `authentik_admin_action`). Where QQQ separates the table name from the backend name, both are identical; possible-value sources use the table name. Permissions follow the same rule (owner, 2026-10-12): the former `identity.*` permissions are now `authentik.*` (`authentik.users.read`, `authentik.memberships.manage`, ...); the shorthand `users.read` etc. in the action tables means the `authentik.` name. Java classes stay CamelCase; field, process and widget names are unchanged. The one name that is not a table is the API backend, a connection rather than a table: it is `authentikApi` everywhere (diagram, README, code), camel-cased like the process and widget names. No version of qbit-authentik has been released yet, so this is a rename in the design and in the unreleased branch code (whose `applyPrefix` camel-joins `authentik` + name today), not a migration.

### 6.1 API-backed tables (backend `authentikApi`, `AuthentikActionUtil`)

| Table | Source | Key | Capabilities | Notes |
|---|---|---|---|---|
| `authentik_user` | `/core/users/` | `pk` (int) | Query, get. Insert via *Create user*/*Invite*; update via processes only. | Computed: `isProtected`, `isSuperuser` (inherited), `createdByQqq`, `effectivePermissions` (detail), `mfaDeviceCount` (detail). `attributes` beyond `qqq.io/*` is sensitive. |
| `authentik_group` | `/core/groups/` | `pk` (uuid) | Query, get. Insert via *Create group*; update via *Edit group* / *Edit permissions*. | `parents` as display names; `isSuperuser` (inherited, locked); `permissions` (own `qqq.io/permissions`) and `effectivePermissions` (with ancestors); `appAccess`; member count. |
| `authentik_group_member` | Virtual: `GET /core/users/?groups_by_pk=` / `GET /core/groups/{uuid}/?include_users=true` | `(groupPk, userPk)` composite string | Query, insert → `add_user`, delete → `remove_user` | The membership association. It supports the generic child-record UI on both user and group. Insert and delete go through the guard, with processes as the preferred UI. |
| `authentik_role` | `/rbac/roles/` | uuid | Query, get | `managed` roles (`ak-managed-role--*`, `ak-migrated-role--*`) are hidden by default |
| `authentik_role_assignment` | Virtual over `GET /rbac/roles/?users=` / `?groups=` (`inherited=true` for effective) | composite | Query; insert/delete in phase 4 | `assignedVia`: DIRECT or a group name |
| `authentik_permission` | `/rbac/permissions/` + `assigned_by_roles` | id | Query | Catalogue and per-role view (read only) |
| `authentik_application` | `/core/applications/` | slug | Query, get | Read only. Links to bindings. |
| `authentik_policy_binding` | `/policies/bindings/?target=` | uuid | Query | Read only. Shows the policy, group or user, plus negate, order and expiry. |
| `authentik_session` | `/core/authenticated_sessions/` | uuid | Query, delete (revoke) | Sensitive |
| `authentik_mfa_device` | `/authenticators/admin/all/?user=` | `type:id` | Query, delete (reset) | Metadata only. Query requires a `user` criterion. |
| `authentik_token` | `/core/tokens/?user__username=` | identifier | Query, delete (revoke) | **No key field.** Only `intent`, `expires`, `description`. |
| `authentik_event` | `/events/events/` | uuid | Query, get | Sensitive. Ordered `-created`. `context` is rendered as JSON. |
| `authentik_invitation` | `/stages/invitation/invitations/` | uuid | Query; insert via *Invite*; delete (cancel) | |
| `authentik_blueprint` | `/managed/blueprints/` | uuid | Query | Read only. Used by the drift report. |

**`FilterMapper`** maps QQQ criteria to Authentik query params:
- `EQUALS` → `field=`
- `IN` → the plain name repeated once per value, e.g. `type=a&type=b` (`groups_by_name`, `type`, `actions`). **Never the bracketed `field[]=a` form: Authentik silently ignores it and returns unfiltered results (§0.2 vi).**
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
| `authentik_admin_action` | **Append-only audit and outbox** of every write the qbit attempts. Columns: id, correlationId, actionName, operator (QQQ user + Authentik username), reason, targetType, targetKey, targetLabel, beforeJson, afterJson, status (`PENDING_APPROVAL`, `RUNNING`, `SUCCEEDED`, `FAILED`, `REFUSED`), errorMessage, authentikEventPk, approver, timestamps. The row is inserted *before* the API call, following the sql-workbench `ExecutionAuditor` pattern; if that insert fails, the call is not made. Insert, update and delete capabilities are removed for everyone. |
| `authentik_approval_request` | Two-person requests: action payload, requester, approver, 24 h expiry, approver ≠ requester. Used for RBAC (phase 4) and, by default, for permission edits that add `authentik.*` (§6.3). |
| `authentik_user_mirror` | `MIRROR` feature: pk, uuid, username, email, name, type, isActive, lastLogin, isSuperuser, isProtected, groupNames, effectivePermissions (text), mfaDeviceCount, failedLogins24h, refreshedAt. Unique on username and email. Web Users joins here (`qqq_user_session.user_id` = email in kof22). The mapper falls back to it. |
| `authentik_group_mirror` | pk, name, parentPks, attributes `qqq.io/*` only, memberCount, isSuperuser (inherited), refreshedAt. Feeds the resolver and the tree widget. |
| `authentik_membership_mirror` | userPk, groupPk, groupName, refreshedAt |
| `authentik_webhook_inbox` | Received webhook payloads, deduplicated by event pk, with processing status. Retained 30 days. |

### 6.3 Processes (table and record actions only, never left nav)

Each process:
- is bound with `withTableName(...)` and placed in `QMenu` slots (`isHidden(true)` for navigation)
- uses `StreamedETLWithFrontendProcess` with steps: **input** (targets and values; **reason** is required for writes), **preview** (server-computed per-target before → after, effective-permission deltas, computed-access deltas, guard results, and skipped targets with reasons), **confirm** (per `confirmations`), **execute** (audit row → stale check → API call → Authentik custom event → cache and mirror invalidation), and **result** (per-record outcome with links to audit rows)

| Process | Table / slot | Records | Permission | Default confirmation | Authentik calls |
|---|---|---|---|---|---|
| Create user | `authentik_user` query action | 0 | `authentik.users.manage` (+ `memberships.manage` if groups given) | CONFIRM | `POST /core/users/` (no password) → `add_user` per group → `recovery_email` |
| Invite user | same | 0 | `authentik.users.manage` | CONFIRM | `POST /stages/invitation/invitations/` + `send_email`; groups applied on `invitation_used` |
| Edit user | `authentik_user` view action | 1 | `authentik.users.manage` | CONFIRM | `PATCH /core/users/{id}/` (name, email, path, type internal/external) |
| Deactivate | `authentik_user` | 1–25 | `authentik.users.manage` | TYPED | `PATCH is_active=false` (deletes sessions) + optional app-password revoke |
| Reactivate | same | 1–25 | `authentik.users.manage` | CONFIRM | `PATCH is_active=true` |
| Add to groups / Remove from groups | `authentik_user` query + view | 1–100 | `authentik.memberships.manage` | CONFIRM (TYPED > 25) | `POST /core/groups/{g}/add_user/` / `remove_user/` |
| Add members / Remove members | `authentik_group` view | 1 group + picker | same | same | same |
| Create group | `authentik_group` query action | 0 | `authentik.groups.manage` (+ `permissions.manage` if permissions given) | CONFIRM | `POST /core/groups/` (name, parents, attributes) |
| Edit group | `authentik_group` view | 1 | `authentik.groups.manage` | CONFIRM | `PATCH` name, parents and non-reserved attributes (merge-patch) |
| Edit permissions and app access | `authentik_group` view | 1 | `authentik.permissions.manage` | CONFIRM; **TWO_PERSON** when adding any `authentik.*` permission | `PATCH attributes` (`qqq.io/permissions`, `qqq.io/app-access`), re-read first |
| Check access / Effective permissions | `authentik_user` view | 1 | `authentik.users.read` | none | computed access per in-scope app (§5.1, not `check_access`); the resolver |
| Send password reset email | `authentik_user` | 1–25 | `authentik.credentials.reset` | CONFIRM | `recovery_email` |
| Reset MFA | `authentik_user` | 1 | `authentik.credentials.reset` | TYPED | per-device `DELETE` |
| Revoke app passwords | `authentik_user`, `authentik_token` | 1–25 | `authentik.credentials.reset` | TYPED | `DELETE /core/tokens/{identifier}/` |
| Revoke sessions | `authentik_user`, `authentik_session` | 1–100 | `authentik.sessions.revoke` | CONFIRM | `bulk_delete` / per-session `DELETE` |
| Cancel invitation | `authentik_invitation` | 1–25 | `authentik.users.manage` | CONFIRM | `DELETE` |
| Assign / unassign role | user / group | 1–25 | `authentik.roles.manage` | TWO_PERSON | `/rbac/roles/{r}/add_user/` or group `roles` (phase 4) |
| Resync mirror | `authentik_user_mirror` query | 0 | `authentik.admin` | CONFIRM | full reconcile |
| Export snapshot now | `authentik_user_mirror` query | 0 | `authentik.admin` | CONFIRM | reads only (§7.3) |
| View audit trail | every table: view action → filtered `authentik_admin_action` + `authentik_event?context_model_pk=` | 1 | `authentik.audit.read` | none | events query |

**Concurrent edits.** If the Authentik UI changes a user or group after a QQQ preview was built, the stale check fails that record and asks for a re-review. Edits made in the Authentik UI are never overwritten blindly.

---

## 7. Sync

| Mode | Used for | Freshness |
|---|---|---|
| **Read-through** (API + 30 s memoization) | All Authentik tables and record screens | Live |
| **Mirror** (RDBMS) | Web Users joins, widgets, counts, the resolver fallback, the tree widget | Webhook ~seconds; reconcile every 15 min |

### 7.1 Webhook path (Authentik → QQQ)

These objects are infrastructure in Git (`present`):
1. **Event matcher policies** `qqq-<appKey>-sync-*`: `model_created`, `model_updated`, `model_deleted` for `authentik_core.user` and `authentik_core.group`, plus `login`, `login_failed`, `password_set`, `invitation_used`, `user_write`. They are bound to one rule (ANY).
2. **Webhook transport** `qqq-<appKey>`:
   - `webhook_url` `https://admin.kof22.com/qqq/v1/authentik/webhook`
   - a **body mapping** returning `{eventPk, action, created, username, model: context.model, app}`. The default body has no event id or model.
   - a **header mapping** adding `X-QQQ-Webhook-Secret` from the worker env
   - `send_once: true`
3. **Notification rule** with `destination_event_user: true`. Without a destination, nothing is sent.

`AuthentikWebhookRouteProvider` does the following:
1. checks the secret with a constant-time compare
2. inserts into `authentik_webhook_inbox`, deduplicating on `eventPk`
3. publishes to the ESB destination `authentik.events`, or processes inline
4. returns 204

An `EsbTrigger` (BATCH 50 / 2 s, concurrency 1, maxAttempts 5) runs `AuthentikApplyChangeEvents`, which:
- invalidates memoization
- re-reads the touched users and groups into the mirror
- recomputes effective permissions for the affected members (group edits fan out to members)
- marks their QQQ sessions for re-evaluation (§5.6)

Changes made by QQQ itself come back as webhooks too. They are harmless, because each one only triggers a re-read.

### 7.2 Reconcile

Webhooks are hints: delivery is at least once, unordered, and lost if no destination resolves. So:
- **Every 15 minutes**: `GET /core/users/?last_updated__gt=<watermark>`, a full group sweep (groups have no `last_updated` in 2026.8, and are few), plus `login_failed` events since the watermark.
- **Nightly**: a full user, group and membership sweep (about 20 requests per 1,000 users), which removes deleted rows.

### 7.3 Optional nightly snapshot export (read-only, not authoritative)

`IdentitySnapshotExport` (feature `SNAPSHOT_EXPORT`, scheduled nightly) writes `identity/<appKey>/users.yaml`, `groups.yaml` and `memberships.yaml` into a configured Git repo and commits only if something changed. The proposal is a private `KofTwentyTwo/identity-snapshots` repo, kept separate from cluster-gitops so ArgoCD can never apply it.

- **Contents.** Username, name, email, type, active, path, groups, group parents and `qqq.io/*` attributes. **No** IPs, sessions, devices or tokens.
- **Purpose.** A diffable "who had access when" history for audits, and a reference list for disaster recovery.
- **Not authoritative.** Nothing ever applies it back. The DR source of truth is the CNPG backup of Authentik's database.
- **Credentials.** A deploy key or a fine-grained token, write access to that repo only, held in Infisical.

### 7.4 Web Users integration (kof22)

- *Web Users* gains Users, Groups and Audit (`authentik_user`, `authentik_group`, `authentik_admin_action`).
- `qqq_user_session` gets a join to `authentik_user_mirror` on `user_id = email`, adding groups, active, last login, MFA and effective permissions.
- The `adminHomeWebUsers` widget replaces its footer link with real counts.

---

## 8. UI (Next UI)

### 8.1 App placement

The qbit produces an app `authentikAdmin` (label **"Identity"**, icon `badge`). kof22 nests it under **Admin → Web Users** with plain labels: "Users", "Groups", "Sessions", "Audit". Processes appear only as record and table actions.

### 8.2 Screens

| Screen | Built from | Contents |
|---|---|---|
| Users list | generic query on `authentik_user` | Username, name, email, type, active, last login, groups, protected lock. Saved views: "Inactive 90d", "No MFA", "Service accounts", "Superusers", "Created by QQQ". Bulk actions: Add to groups, Remove from groups, Deactivate, Revoke sessions. Table action: Create user, Invite user. |
| User detail | generic view + child widgets | Badges (protected, superuser, service account). Tabs: Groups, **Effective permissions** (each with the granting group), Access (computed per app from `qqq.io/app-access` and the app's bindings; other expression policies shown as "not evaluated", §0.2 iii), Sessions, MFA devices, App passwords, History (QQQ audit + Authentik events, including UI-made changes with their actor) |
| Groups | `AUTHENTIK_GROUP_TREE` widget + generic list | Tree over `parents` (many-to-many; shared children marked). Badges: superuser (red, locked), has-permissions, app-access. Member counts. |
| Group detail | generic view + child widgets | Members (Add/Remove); **Permissions** (own + inherited, with an editor that offers only permissions the operator holds); **App access**; parents and children; roles (read); other attributes (`authentik.audit.read`) |
| Access matrix | `AUTHENTIK_ACCESS_MATRIX` widget | Users in scope × `applicationsInScope`, cells computed by the qbit (not `check_access`, §0.2 iii; cached 5 min, 25 rows per page) |
| Health | `AUTHENTIK_HEALTH` widget | API reachability and version, token expiry, blueprint instance status, webhook lag, last reconcile, possible blueprint reversions, `bootstrapOperators` in use |
| Audit | generic query on `authentik_admin_action` + `authentik_event` | Filter by operator, target, action, status, and source (QQQ vs Authentik UI) |

### 8.3 Admin-home widgets (standard widget types, from the mirror)

- Statistics: active users, inactive users, service accounts, users without MFA, failed logins in 24 h.
- Table: recent auth events.
- Table: "attention" items: admins without MFA, tokens expiring within 14 days (including the qbit's own token), blueprint status not `successful`, `bootstrapOperators` still set.

### 8.4 Frontend delivery

- Custom widget types `AUTHENTIK_GROUP_TREE`, `AUTHENTIK_ACCESS_MATRIX` and `AUTHENTIK_HEALTH` are added to `WidgetRenderer.tsx` as `React.lazy` cases, following the `SQL_WORKBENCH` and `ESB_OVERVIEW` pattern.
- `src/lib/api/authentik.ts` calls `/qqq/v1/authentik/{tree,matrix,health,permission-catalog}`, all guarded.
- A permission-picker field component for the group permissions editor.
- Mocks in `src/mocks/fixtures/authentik.ts`; e2e in `tests/e2e/authentik.spec.ts`.
- Everything else uses the generic UI.

---

## 9. Security

### 9.1 Secrets

- **API token.**
  - Infisical `/kof22-website-authentik-api/token` → ExternalSecret `kof22-website-authentik-api` (store `infisical`, refresh 1 h) → env `AUTHENTIK_API_TOKEN` on the backend Deployment. This is the same pattern as `kof22-website-oauth2`, and MHP uses the same shape.
  - Minted by hand in the Authentik UI for the service account (`intent: api`, 90-day expiry). Never in blueprints.
- **Rotation.**
  - The health widget warns 14 days before expiry.
  - Rotate in the Authentik UI, update Infisical, and let ESO refresh. Then check the service account has exactly one token (it can mint its own, §0.2 v).
  - The qbit re-reads the env-backed value on a 401 and retries once.
- **Other secrets.** The webhook secret and the snapshot-export Git credential are separate Infisical keys.
- **Redaction.**
  - `toString()` and `OutboundAPILog` redact `Authorization`.
  - Bodies are never logged for `recovery_email`, user create, token endpoints, or any response containing `token`/`key`/`link`.

### 9.2 Audit (both sides)

- **QQQ.** The append-only `authentik_admin_action` (§6.2), plus RECORD-level `QAuditRules` on the mirror tables.
- **Authentik.**
  - Model events carry `user = qqq-<appKey>-idadmin`.
  - The qbit also posts `custom_qqq_admin_action` with `{correlationId, operator, operatorEmail, reason, action, targets, appKey}` and stores the returned event pk.
  - Since 2026.8, admin changes also appear in the affected user's event history.
- **Changes made in the Authentik UI** show up in QQQ's Audit view through the events table, with their real actor.

### 9.3 Rate limiting and load

Authenticated API calls are not throttled by Authentik, and kof22's Authentik shares the website's Postgres. The qbit therefore:
- uses a token bucket (10 req/s, burst 20)
- paces bulk operations
- caps the access matrix
- serves widgets and the mapper fallback from the mirror

429 and 5xx responses use `BaseAPIActionUtil` backoff.

### 9.4 PII and retention

- **What is PII.** Emails, names, IPs, user agents, geo/ASN and event contexts.
- **Who sees it.** IPs, user agents, geo and event contexts need `authentik.audit.read`. Exports respect the same rule.
- **What is stored where.** The mirror stores no IPs or user agents. The snapshot export holds only directory data (§7.3).
- **Retention.** Webhook inbox 30 days; admin-action audit 2 years.

### 9.5 Threats considered

| Threat | Control |
|---|---|
| A compromised QQQ backend escalates in Authentik | The service account can't create superusers, touch superuser groups, read or mint tokens, impersonate, delete users, or change flows and policies (§5.1) |
| A QQQ operator escalates themselves or a friend | Subset rule, no self-edits, two-person approval for `authentik.*` grants, protected accounts |
| Lockout (last admin removed or deactivated) | Last-admin guard; break-glass superuser outside QQQ; `bootstrapOperators` |
| A blueprint re-apply reverts QQQ edits | No `present` identity entries, no member lists (CI lint); bootstrap as `created` (verified, §4.2); reversion detector |
| Lost update between QQQ and the Authentik UI | Stale check at execute; attribute merge-patch after a fresh read |
| Account takeover through a recovery link | Links are never shown; email only |
| A non-HTTP caller bypasses permissions (#1061) | Guard in the backend util, plus ArchUnit |
| A field leaks because `FieldSecurityLock` isn't enforced (#1062) | Sensitive fields stripped in the util |
| A forged webhook | Shared secret, constant-time compare; it only triggers re-reads |
| Stale session permissions after a demotion | Re-evaluation on token refresh plus webhook-triggered marking (§5.6); live groups used for guard checks |
| An Authentik upgrade breaks the API | Pinned-schema contract test, version check |

---

## 10. Testing

- **Unit.**
  - `AuthentikActionUtil` with queued mock responses, using **recorded fixtures**. The fixtures are captured from the Testcontainers run, scrubbed, and stored under `src/test/resources/fixtures/2026.8.3/`.
  - Covered: FilterMapper, pagination, id types, field stripping, redaction, and the resolver (ancestors, wildcards, unknown names).
- **Guard and safety matrix** (release gate). Every process × permission set × target class (protected, superuser by inheritance, self, last admin, out of scope, ordinary), plus subset-rule cases.
- **Contract tests** (`@Testcontainers`, skipped without Docker via `RequiresDockerCondition`).
  - Setup: Authentik `2026.8.3` (server + worker + Postgres + Mailpit), bootstrapped with `AUTHENTIK_BOOTSTRAP_TOKEN`, plus the §4.3 bootstrap blueprint and an infrastructure blueprint shaped like kof22's.
  - Tests run with the **service-account token**.
  - Key assertions:
    - (a) Create user → add to group → `recovery_email` arrives in Mailpit.
    - (b) Adding to a superuser group, or re-parenting under one, is refused, and by which layer.
    - (c) **API-made members and attribute edits survive `POST /managed/blueprints/{uuid}/apply/`** on both blueprints. The `created` entries are skipped, and the role's permissions are re-asserted after being removed in the UI.
    - (d) A `present` entry with `users:` *does* wipe members. This documents why the CI lint exists.
    - (e) Webhook delivery reaches a test route with the custom body.
    - (f) `PATCH is_active=false` deletes sessions.
    - (g) ~~`check_access` works with the granted permissions~~ replaced: `check_access?for_user` is ignored for non-superusers and the policy test endpoint fails (pinned in `AccessCheckIT`); the attribute-driven sign-in policy is verified with the admin's `check_access` (`SignInPolicyIT`).
    - (h) The bootstrap blueprint on an empty instance, ordered by `metaapplyblueprint`, applies cleanly.
  - **Phase 0 status:** (b), (c), (d), (h) green, plus the escalation and lockout floor, token minting, access-check behaviour, the sign-in policy, inert webhook plumbing, and the qbit's own read path (`QbitReadContractIT`), all on 2026.8.3.
- **cluster-gitops lint tests.** Fixtures with good and bad blueprints.
- **Schema drift.** A pinned `schema.yml` test, plus a scheduled CI diff against new releases.
- **Frontend.** Mock fixtures and a Playwright e2e for create user, edit permissions, the tree and the matrix.
- **Staging rehearsal.** Run the §4.4 migration against a disposable Authentik restored from a production snapshot before doing it for real.

---

## 11. Delivery plan

The estimates assume one senior engineer with AI assistance, plus about 20% of a frontend engineer from phase 1. They are calendar weeks. **v1 = phases 0–3** (decision 2).

| Phase | Scope | Exit criteria | Est. |
|---|---|---|---|
| **0. Foundations and migration** | See the task list below | Contract tests (b), (c), (d), (h) green; the kof22 migration done in staging; read-only token wired | 2 wk |
| **1. Read-only + permissions model** | Tables (users, groups, members, roles, applications, bindings, blueprints; sensitive tables behind `audit.read`). `EffectivePermissionResolver` + `AuthentikGroupPermissionMapper` (kof22 cut over, sign-in policy fallback removed). Group tree, Effective permissions and Access tabs. Health widget. Mirror with scheduled reconcile. Admin-home widgets. | kof22 Web Users shows groups, last login and MFA; kof22 permissions come from `qqq.io/permissions`; guard matrix green for reads | 2.5 wk |
| **2. Memberships and permission editing** | Add/remove (both sides, bulk), the subset rule, the last-admin guard, the group permissions and app-access editor with two-person approval for `authentik.*`, `authentik_admin_action` + Authentik custom events, webhook route + ESB trigger + inbox, session re-evaluation marking | Scenarios B and D end to end; safety matrix 100%; webhook latency < 10 s | 2.5 wk |
| **3. Full user lifecycle** | Create user, invite (+ enrollment flow in Git), edit, deactivate/reactivate, recovery email, revoke sessions, reset MFA, revoke app passwords, create and edit groups, cancel invitations; optional snapshot export | Scenarios A and C end to end; contract tests (a), (e), (f) green; access computation pinned by `AccessCheckIT` and `SignInPolicyIT` (§10 g) | 3 wk |
| **4. RBAC (post-v1)** | Authentik role assignment with two-person approval, access-matrix polish, MHP migration off `AuthentikAdminApiClient` | MHP running on the qbit | 3 wk |

**v1 (phases 0–3) is about 10 weeks**, and phase 4 adds about 3 more.

### 11.0 Phase 0 task list

**Blueprint and Authentik changes need the owner.** Edits to Authentik blueprints in cluster-gitops have been blocked for agents by the permission classifier (it flags them as a "Permission Grant"). So items 4–8 and 10 must be **applied by James, or explicitly allowed by him**. An agent can prepare the diffs and the runbook.

| # | Task | Who |
|---|---|---|
| 1 | Create repo `QRun-IO/qbit-authentik` from `qbit-template-extension`; copy this spec to `docs/DESIGN.md`; add `AGENTS.md`/`CLAUDE.md` | agent: **done** (the repo is public) |
| 2 | `AuthentikQBitConfig` + producer skeleton; `AuthentikActionUtil` (bearer auth, page/page_size paging, FilterMapper, throttle, redaction); pinned `schema.yml` + drift test | agent |
| 3 | Testcontainers harness (Authentik 2026.8.3 + Postgres + Mailpit), fixture recorder; contract tests (b), (c), (d), (h) | agent |
| 4 | Verify on the container: (i) how to grant *global* role permissions declaratively in 2026.8.3; (ii) whether a `parents` change under a superuser group is refused without `enable_group_superuser`; (iii) the permission `check_access` needs; (iv) the `metaapplyblueprint` ordering. Then fix §4.3 and §5.1. | agent: **done**, §0.2 |
| 5 | Prepare the cluster-gitops PR: add `kof22-identity-bootstrap.yaml`; remove the `present` `james.maes` user and `kof22-admins` group entries; make the sign-in policy attribute-driven with the name fallback; add webhook transport, rule and policies; add the CI lint | agent drafts; **James reviews and merges** |
| 6 | Rehearse the migration (§4.4 steps 1–6) on a staging Authentik restored from a production snapshot | agent with **James's go-ahead** |
| 7 | Apply the migration in production: merge the PR, check that both blueprint instances are `successful` and that memberships are unchanged | **James** |
| 8 | Mint the service-account API token (90 days) in the Authentik UI and store it in Infisical `/kof22-website-authentik-api/token`; create the webhook secret | **James** |
| 9 | Add ExternalSecret `kof22-website-authentik-api` and env wiring to the Website-Backend Deployment (no secret values in Git) | agent drafts; James merges |
| 10 | Seed data in the Authentik UI: add James to `identity-admins`; set `qqq.io/app-access` on `kof22-admins` and `kof-website-publishers`; set `qqq.io/permissions` on `kof22-admins` to today's `ADMIN_PERMISSIONS`; set the break-glass password in 1Password | **James** |
| 11 | Wire the qbit into Website-Backend in **read-only mode** on staging; confirm the health widget is green | agent |

### 11.1 Risks

| Risk | Mitigation |
|---|---|
| The service account holds global user/group change rights (decision 1), so its blast radius is wider than revision 1's | Hard Authentik floor (no superuser, tokens, impersonation or delete); `userScope`/`groupScope`; guard; audit on both sides; 90-day token |
| Someone reintroduces a `present` identity entry, and members get wiped | CI lint; contract test (d); reversion detector |
| A bootstrap entry error rolls back the infrastructure blueprint | Separate bootstrap file; `metaapplyblueprint` ordering; minimal attrs; health widget |
| A lockout through bad permission edits | Last-admin guard, break-glass, `bootstrapOperators`, two-person approval for `authentik.*` |
| Session permissions go stale after a demotion | Token-refresh re-evaluation, webhook marking, live checks in the guard |
| The service account can lock administrators out (remove members from `authentik Admins`, deactivate break-glass, strip `qqq.io/protected`, §0.2 ii-b). Re-parenting under a superuser group **is** refused by Authentik (§0.2 ii) | Guard refusal of every write that targets a protected account or a superuser group; drift report; recovery through cluster access |
| Load on the shared Postgres | Throttle, caps, mirror |
| Webhook loss | Inbox, 15-minute reconcile, nightly sweep |
| QQQ core gaps (#1061, #1062) | Guard in the backend util, ArchUnit, field stripping |
| Token leakage | Redaction tests; no body logging on sensitive paths |
| An Authentik upgrade changes fields | Pinned schema, fixtures per version, version check |

### 11.2 Owner decisions and remaining questions

The decisions taken on 2026-10-11 are in §0.1. The remaining questions are smaller and default to the recommendation (marked) if not answered:

1. **Snapshot export target.**
   - **(a) New private `KofTwentyTwo/identity-snapshots` repo.** *(recommended)*
   - (b) A folder in cluster-gitops (risk of being applied by mistake).
   - (c) Skip.
2. **Two-person approval for granting `authentik.*`.**
   - **(a) On**, with a second identity admin required, or break-glass via the Authentik UI while only James exists. *(recommended)*
   - (b) Off while James is the only admin, with a `TYPED` confirmation instead.
3. **Break-glass admin.**
   - **(a) A new `kof22-breakglass` superuser, its password in 1Password, protected from QQQ.** *(recommended)*
   - (b) Use James's own account as break-glass, which means QQQ can never edit James.
4. **Seed groups.**
   - **(a) Seed `identity-admins`, `kof22-admins`, `kof-website-publishers`, `kof-website-drafters` as `created`.** *(recommended)*
   - (b) Seed only `identity-admins`.
5. **Service accounts in scope.**
   - **(a) QQQ lists service accounts but can only deactivate them and revoke app passwords; creating them stays in the Authentik UI.** *(recommended)*
   - (b) Hide them from QQQ.
6. **Drafters.** `kof-website-drafters` is accepted by the backend but has no app access.
   - **(a) Give it `qqq.io/app-access` only if drafters should sign in to the admin; otherwise leave it for bearer-token publishing only.** *(recommended; needs your call on intent)*

---

## Appendix A: Example API mapping (`authentik_user`)

| QQQ | Authentik |
|---|---|
| `QueryInput(authentik_user, filter: isActive EQUALS false AND lastLogin LESS_THAN 2026-07-13, orderBy username, limit 50)` | `GET /api/v3/core/users/?is_active=false&last_login__lt=2026-07-13T00:00:00Z&ordering=username&include_groups=true&include_roles=true&type=internal&type=external&type=service_account&page=1&page_size=50` |
| `GetInput(authentik_user, 42)` | `GET /api/v3/core/users/42/` (the detail endpoint takes no `include_*` parameters and always returns `groups_obj` and `roles_obj`) |
| `InsertInput(authentik_group_member, {groupPk: <uuid>, userPk: 42})` | Guard checks, then `POST /api/v3/core/groups/<uuid>/add_user/` with body `{"pk": 42}`, which returns 204 |
| `DeleteInput(authentik_session, <uuid>)` | Guard checks, then `DELETE /api/v3/core/authenticated_sessions/<uuid>/` |
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
