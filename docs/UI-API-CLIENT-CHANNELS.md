# Client channels and the assistant wall

Every change records **who** made it (the human user) and **through which client** (the
channel). Some objects are never available through some channels, whatever the user's
permissions. The main case is the AI assistant: it can never see or change its own
configuration, or the security-administration surfaces.

## The channel
- **Header.** A request names its client in `X-QQQ-Client: <channel>[/<detail>]`, for example
  `next-ui`, `cli`, or `assistant/<turnId>`.
- **Allow-list.** The channel must be on the allow-list in `ClientChannelsMetaData`, a supplemental
  QInstance metadata object. The defaults are `next-ui`, `material-dashboard`, `assistant`, `cli`,
  `tui`, `ios` and `api`. An unknown channel is refused with 400. `GET /qqq/v1/version` lists the
  accepted channels in `clientChannels`.
- **Session.** The server stores the channel on the session: `QSession.getClientChannel()` gives the
  full value and `getClientChannelName()` the part before the `/`. The channel is set on every
  request, and is null when the header is absent.
- **Attribution only.** The actor is always the signed-in human user. A channel never grants
  anything; it can only be denied things.
- **Next UI** sends `next-ui`.
- **Server-side code.** Code that acts for a user without going through HTTP, such as the
  assistant's direct core calls, sets the channel itself:
  `session.withClientChannel("assistant/<turnId>")`.

## Auditing
`AuditsMetaDataProvider.withClientChannelField(true)` adds `audit.clientChannel` (label "Client",
VARCHAR(100), nullable). It shows on the audit record screen and is filterable like any field.
`AuditAction` fills it from the session at the moment the change is made:
`AuditSingleInput.clientChannel` is captured when the input is built, so audits written later keep
it. Audit details belong to their audit record, so they carry its channel.

To enable it, an application adds the column first and then turns the field on. For kof22, add a
Liquibase feature changeset `changesets/features/YYYY-MM-DD-audit-client-channel.sql` with
`ALTER TABLE audit ADD COLUMN client_channel VARCHAR(100) NULL`, using the backend's column naming.

## Deny rules (the wall)
Deny rules can be set in three ways, for tables, processes, reports, widgets and apps:
- on the object: `new QPermissionRules()...withDeniedClients(Set.of("assistant"))`;
- by exact name: `ClientChannelsMetaData.ofOrWithNew(qInstance).withDeniedObject("assistant", "my_table")`;
- by name pattern: `...withDeniedPattern("assistant", "my_prefix_*")`, where `*` is a wildcard and
  matching is case-insensitive.

**Enforcement** happens in `PermissionsHelper`, the same place that checks table, process, report,
widget and app permissions. For a denied channel:
- the object is omitted from `metaData`, exactly as for a user without permission (DENY_HIDE);
- every operation on it is refused with 403 ("Permission denied.").

The check runs before anything that could allow access: unprotected objects, custom permission
checkers, the user's permissions.

**Default wall for `assistant`.** It applies even when an instance registers nothing, because
`ClientChannelsMetaData.of` falls back to the defaults. The default patterns are:
- `assistant_*`: its own configuration. That covers providers, models, prices, profiles, skills,
  policy, budgets, usage, tool-call logs and conversations.
- `authentik_*`: identity, permissions and policies.
- `audit*` is write-denied only: the assistant can read a record's history but never insert, edit or delete audits (`withDeniedWritePattern`).
- `*api_key*` and `*apikey*`.
- `*credential*`, `*secret*` and `*password*`.
- `*permission*` and the role tables: `role`, `roles`, `*_role`, `*_roles`, `*_role_*`, `user_role*`.
- `*user_session*` and `*usersession*`.
- `webhooks_*`: webhook endpoints and their signing secrets.
- `worm_audit_*`: the WORM audit chain, checkpoints, sealed exports and purges (qbit-worm-audit).

Modules add their own rules with `withDeniedObject` or `withDeniedPattern`. For example, the
assistant qbit registers its tables, and `assistant_*` is already covered.

## Fail-closed
- **The assistant cannot remove or change its label.** `UiApiInvoker.forServer(server, "assistant")`
  fixes the channel on the server side, and sets it on every call it makes. A request can only add a
  detail (`withClientDetail(turnId)`, which gives `assistant/<turnId>`). The invoker never forwards
  a client-supplied `X-QQQ-Client`.
- **The model only acts through the invoker.** The assistant's LLM never sends HTTP requests
  itself; its tool calls go through the invoker. So a missing or forged header from the model
  cannot happen.
- **A request labelled with another channel is only ever more restricted.** Any client may add the
  `assistant` label to restrict itself, but no channel grants access.
- **Only code or admin metadata can change the rules.** Every surface that edits permissions,
  identity or this configuration is itself behind the wall.

## Field access per channel

`FieldAccessHelper.getDecision(table, field)` combines the field security lock, the
instance's `FieldAccessResolverMetaData` resolver, and the session channel's policy,
using the most restrictive tier. Register a `FieldAccessChannelPolicyInterface`
instance or `QCodeReference` with
`ClientChannelsMetaData.ofOrWithNew(instance).withFieldAccessPolicy(channelName, policy)`;
`getFieldAccessPolicy(channelName)` retrieves it. The channel name comes from
`QSession.getClientChannelName()` (details after `/` do not affect policy).

The `assistant` channel defaults to `AiExposureChannelPolicy`, even without instance
configuration: NONE maps to HIDDEN, METADATA_ONLY and MASKED to MASKED, and VALUES
to FULL. Other channels have no default field ceiling. The policy uses the active
kernel's constructor settings during kernel calls; outside the kernel it uses
`AiToolKernelSettings` supplemental instance metadata, or default settings if absent.
The kernel sets `assistant` on a cloned session and restores the caller's context.
Its compatibility exposure API maps shared tiers back to AI exposure. It retains the
channel policy's METADATA_ONLY reason so descriptions can name those fields while
value outputs omit them. All masking uses `FieldValueMasker`.

Registering a resolver opts outermost USER inputs into field access enforcement even
when core table enforcement is disabled. It does not enable table, join, or record
permission checks. SKIP and authorized scopes retain their bypass behavior; without
a resolver, field enforcement continues to follow core permission enforcement.

## Scoped credentials (access tokens)
A session created from a scoped credential carries `QSession.credentialScope`, a
`QCredentialScope`. One example is an Authentik access token minted by qbit-authentik: its
authentication customizer reads the token's `qqq_token` claim and sets the scope. A scope has these
fields; each set is null when unrestricted:
- `credentialId`
- `readOnly`
- `apps`, `tables`, `processes`, `reports`, `widgets`
- `operationIds`, as `METHOD /template`, e.g. `POST /table/{tableName}/query`

Core enforces the scope. It only ever narrows the user's live permissions, so revoking a user
permission narrows the token too:
- `PermissionsHelper` denies objects outside the scope: they are hidden from metaData and refused.
  `readOnly` denies table inserts, edits and deletes, and any process the scope doesn't list.
- The UI API refuses operations outside `operationIds` (`AbstractEndpointSpec.getOperationId()`).
- The session's client channel is `token:<credentialId>`. The `X-QQQ-Client` header is ignored, so
  the request cannot relabel itself, and the channel is audited.
- The assistant wall and field-access rules apply as usual.

## Who owns the wall rules (#1157)
- **Core keeps the generic rules** (`ASSISTANT_GENERIC_DENIED_PATTERNS`). These cover API keys,
  credentials, secrets, passwords, permission and role tables, and user sessions, plus write-only
  access to `audit*`.
- **Each qbit owns its own rules.** Its producer registers them, for example
  `ClientChannelsMetaData.ofOrWithNew(qInstance).withDeniedPattern("assistant", "<qbit>_*")`,
  `withDeniedObject(...)`, or `QPermissionRules.withDeniedClients(...)` on the table.
- **The qbit prefixes still in core are transitional** (`ASSISTANT_TRANSITIONAL_QBIT_DENIED_PATTERNS`:
  `assistant_*`, `authentik_*`, `webhooks_*`, `worm_audit_*`). They stay until each qbit registers
  its own rule, so the wall never opens for an application still on an older qbit. They are removed
  at 4.1.0 GA.
- **The validator warns about uncovered sensitive tables.** For every table whose name looks
  sensitive (`*_policy`, `*_config`, `*_settings`, `*_secret*`, `*_token`, `*_key`, ...), it warns
  if no rule denies the table to the assistant. Fix it by walling the table in its qbit, or by
  recording a deliberate review with `withReviewedObject("assistant", name)`. A review grants
  nothing.
