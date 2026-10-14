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
