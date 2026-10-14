# User preferences (`/qqq/v1/preferences`)

Status: implemented in core and the v1 UI API. Owner decisions (2026-10-12): all recommendations accepted.
Applies from the QQQ 4.1.0 RC that ships it (UI API 1.2.0, additive).

## 0. Summary

A user sees and changes their own settings in one place: the Preferences page (user menu), or a
shortcut from a feature (for example the gear in the notification bell). Settings come from
**sections** that core, qbits and applications register. Each section is a typed field list built
from ordinary `QFieldMetaData`, plus optional row lists (for example one row per notification
category), and registered section, list and row actions. Clients render sections generically from the metadata, so Next UI, a CLI, a TUI and the
iOS client all show the same preferences.

Storage stays with the owner of the data. Each section names a **provider** (an SPI). The alerts
qbit keeps its `alerts_notification_preference` and `alerts_user_preference` tables; a simple
section with no provider uses core's key-value table `user_preference`.

The UI API only ever reads and writes the **signed-in user's** preferences. There is no user
parameter anywhere in it. Administrators change another user's settings through the normal tables,
which are standard, visible QQQ tables with full record screens, permissions and audits.

## 1. Owner decisions (2026-10-12)

All recommendations below were accepted:

1. Sections live in supplemental instance metadata (`UserPreferencesMetaData`), the same pattern as
   UI API extensions. No new top-level `QInstance` map.
2. Fields are ordinary `QFieldMetaData`, serialized with the existing v1 `FieldMetaData` schema, so
   clients reuse their field renderers. Enum possible values are returned inline with the section.
3. A section may have row lists supplied by the provider. PATCH edits existing rows; registered
   actions can create, rotate or remove provider-owned resources. Per-row read-only fields carry a reason ("Security notices can't be muted").
4. A section is visible only when it is registered (the qbit is installed), its app (if any) exists
   and the user may see the app, and the user has its permission (if any). A hidden section is
   404, exactly like an unknown one.
5. Writes run the provider inside `CorePermissionEnforcer.runTrusted(...)` with the user's own
   session: the user needs no table permission, and audits still name the user and the client
   channel (`audit.clientChannel`, qqq#1141).
6. PATCH is partial: only the fields and rows sent change. Unknown fields, unknown rows, read-only
   fields and any user identifier in the body are rejected with 400.
7. Core validates types, required, max length, enum membership and editability; the provider adds
   domain validation. All errors return together in one 400. Messages use display labels, for
   example `Digest hour: must be a whole number` or
   `Categories, Security: In-app: can't be changed: Security notices cannot be muted`.
   Unknown names say `isn't a setting here`; unexpected top-level keys say
   `Unexpected property: <name>`.

## 2. Metadata (qqq-backend-core)

Package `com.kingsrook.qqq.backend.core.model.metadata.preferences`.

```java
UserPreferencesMetaData.ofOrWithNew(qInstance)
   .withSection(new UserPreferenceSectionMetaData()
      .withName("notifications")                  // [a-z][a-zA-Z0-9_-]*, unique
      .withLabel("Notifications")
      .withIcon(new QIcon("notifications"))
      .withDescription("What reaches you in the app and by email.")
      .withOrder(20)                              // ascending, then label
      .withAppName("alerts")                      // optional scope
      .withPermissionName("alerts.view")          // optional
      .withField(new QFieldMetaData("pausedUntil", QFieldType.DATE_TIME).withLabel("Pause until"))
      .withList(new UserPreferenceListMetaData()
         .withName("categories").withLabel("Categories").withRowLabel("Category")
         .withField(new QFieldMetaData("inApp", QFieldType.BOOLEAN).withLabel("In-app")))
      .withProvider(new QCodeReference(AlertsNotificationPreferencesProvider.class)));
```

- `UserPreferencesMetaData` (`QSupplementalInstanceMetaData`, name `userPreferences`): the
  sections, plus `keyValueTableName` (default `user_preference`) for the core store.
- `UserPreferenceSectionMetaData`: name, label, icon, description, order (default 100), appName,
  permissionName, showInUserMenu (default false), fields, lists, actions, provider (`QCodeReference` to a
  `UserPreferenceProviderInterface`; null means the core key-value store, which supports scalar
  fields only).
- `UserPreferenceListMetaData`: name, label, description, rowLabel, fields.

Validation (`QInstanceValidator`, fails the instance): names present and well-formed and unique;
labels present; field names unique within a section and within each list; field types are
BOOLEAN, INTEGER, DECIMAL, STRING, TEXT, DATE, TIME or DATE_TIME; possible value sources exist;
`appName` exists; the provider loads and implements the interface; a section without a provider has
no lists, and the key-value table exists in the instance.

## 3. Provider SPI

```java
public interface UserPreferenceProviderInterface
{
   UserPreferenceValues load(UserPreferenceRequestContext context) throws QException;
   default List<String> validate(UserPreferenceRequestContext context, UserPreferenceChange change) throws QException { return List.of(); }
   void store(UserPreferenceRequestContext context, UserPreferenceChange change) throws QException;
   default UserPreferenceActionResult runAction(UserPreferenceRequestContext context, UserPreferenceActionRequest request) throws QException
   {
      throw new QBadRequestException("This action isn't available.");
   }
}
```

- `UserPreferenceRequestContext`: the section, the session and `getUserId()`. The user id is always
  `session.getUser().getIdReference()`; nothing from the request can set it.
- `UserPreferenceValues`: scalar `values` (missing means the field default) and `lists`
  (list name to rows).
- `UserPreferenceRow`: `key`, `label`, `description`, `values`, `readOnlyFieldNames`,
  `readOnlyReason`, `actionNames` (null means all ROW actions for this list; empty means none).
- `UserPreferenceChange`: changed scalar `values`, and per list the changed rows (`key` plus the
  changed `values`, represented by `UserPreferenceRowChange`). Values are already coerced to the
  field's Java type when `store` sees them. A duplicate row key within one patch is rejected.

Providers write through ordinary table actions, so table audit rules record the change, the user
and the client channel. Secret values must never be included in audit data.

## 4. Core action and store

`UserPreferencesAction` (`com.kingsrook.qqq.backend.core.actions.preferences`):

- `listSections()` returns the visible sections with their values, ordered.
- `getSection(name)` returns one, or throws `QNotFoundException` when unknown or hidden.
- `patchSection(name, change)` validates core fields, then calls the provider's `validate` and
  `store` inside the same `runTrusted` scope, and returns the reloaded section. Errors throw `QBadRequestException` with every
  message.

The result model is `UserPreferenceSectionResult` (`section` plus `values`). The action uses
the current `QContext` session. Its raw-body `Map<String, ?>` overload preserves unknown properties
for validation; its `UserPreferenceChange` overload supports typed in-process callers.
Provider validation can read its own tables without backing-table permissions. Provider loads
also run in the trusted scope, so reading and reloading preferences do not require
backing-table permissions. Visibility is checked before entering that scope, including the app's
client-channel deny rules. The original session is retained throughout.

Boolean inputs accept booleans and the strings `true`, `false`, `yes`, `no` (case-insensitive);
other inputs are rejected before `ValueUtils` coercion. Structured objects/arrays are not scalar
values. `userId` is reserved and cannot be registered as a scalar or row field. Explicit JSON null
is persisted and returned as null; only a missing value uses the field default.

`KeyValueUserPreferenceProvider` is the default provider. `UserPreferencesKeyValueStoreMetaDataProvider
.defineAll(qInstance, backendName, null)` defines the table `user_preference` (label "User
Preferences"): `id`, `userId`, `sectionName`, `preferenceName`, `value` (TEXT, JSON-encoded),
`createDate`, `modifyDate`, with a unique key on (`userId`, `sectionName`, `preferenceName`) and
FIELD-level audits. Applications create the table with their own migration.

## 5. UI API (qqq-middleware-javalin, core specs)

| Operation | Spec | Response |
|---|---|---|
| `GET /qqq/v1/preferences` | `PreferencesSpecV1` | `PreferencesResponseV1 { sections: PreferenceSectionV1[] }` |
| `GET /qqq/v1/preferences/{sectionName}` | `PreferenceSectionGetSpecV1` | `PreferenceSectionV1` |
| `PATCH /qqq/v1/preferences/{sectionName}` | `PreferenceSectionPatchSpecV1` | `PreferenceSectionV1` |
| `POST /qqq/v1/preferences/{sectionName}/actions/{actionName}` | `PreferenceSectionActionSpecV1` | `PreferenceActionResponseV1` |

`PreferenceSectionV1`: `name`, `label`, `icon` (`Icon`), `description`, `order`, `appName`, `showInUserMenu` (boolean, default false),
`fields` (`FieldMetaData[]`), `values` (object), `lists` (`PreferenceListV1[]`: `name`, `label`,
`description`, `rowLabel`, `fields`, `rows`), and `possibleValues` (source name to
`PossibleValueOption[]`, for enum sources used by the section). `PreferenceRowV1`: `key`, `label`,
`description`, `values`, `readOnlyFields`, `readOnlyReason`, `actionNames` (null means all ROW actions for this list; empty means none).
Sections also include `actions: PreferenceActionV1[]`; `possibleValues` includes ENUM sources
used by action input fields as well as scalar and list fields.

PATCH body `PreferenceSectionPatchRequestV1`: `{ "values": {...}, "lists": { "<list>": [ { "key":
"...", "values": {...} } ] } }`. Any other top-level property, including `userId`, is a 400.

`/preferences` is reserved by core; `preferences` is listed in the version's features.

## 6. Clients

- **Preferences page** (Next: `/app/preferences`, from the user menu). Lists every section.
  Next's device-only display settings appear as a "Display" section on the same page.
- **Section menu entries.** When `UserPreferenceSectionMetaData.showInUserMenu` is true,
  published as `PreferenceSectionV1.showInUserMenu`, the client adds a user-menu entry using
  the section label and icon that opens that section. The default is false.
- **Shortcuts.** A feature links to its section by name. The alerts inbox summary returns
  `preferencesSection: "notifications"`, and Next shows a gear in the bell popover and on
  `/app/notifications` that opens that section in a drawer, with "Manage all preferences".
- **Rendering.** Scalar fields use the client's normal field editors. Lists render one row per
  `key`; at narrow widths each row stacks as a card. Read-only cells show the reason.

## 7. Security

- No user parameter exists. The provider gets the user id from the session.
- A hidden section returns 404 for read and write.
- The client channel is recorded on audits (qqq#1141). The assistant channel follows the normal
  deny rules.
- Tests: two users each see only their own values; a PATCH carrying `userId` is rejected; another
  user's rows are unchanged after a write; a section behind a permission is 404 without it.

## 8. First sections

- **Notifications** (qbit-alerts): pause until; daily digest hour and time zone; alert email and
  in-app on/off; per category: in-app on/off, in-app minimum severity, email (off, immediate,
  daily digest), email minimum severity (security can't be muted); per own alert subscription:
  enabled, minimum severity, delivery mode.
- **Assistant** (qbit-assistant, follow-up): the write-mode default and other per-user settings,
  replacing the bespoke `/assistant/preferences` endpoint.


## Sample acceptance section

`qqq-sample-project` registers `regional` (Regional settings) in `SampleMetaDataProvider.defineInstance()`.
It uses the core KV provider with the sample's H2 RDBMS backend; `prime-test-database.sql`
creates `user_preference` with a unique user/section/preference key and snake-case columns.
The section has no app or permission scope. Its defaults are `America/Chicago`, `ISO`, and
`SUNDAY`; `sample_date_format` and `sample_week_start` supply inline enum choices.
`SamplePreferencesTest` exercises GET, PATCH and a persisted reload through `SampleJavalinServer`.

## Actions and one-time secrets

Register `UserPreferenceActionMetaData` on a section with `.withAction(...)`. `getActions()`
returns its registrations. The core and `PreferenceActionV1` contracts contain `name`, `label`,
`icon` (`QIcon` in core, `Icon` on the wire), `description`, `scope`, `listName`, `confirmationText`,
`isDestructive`, `inputFields` (`QFieldMetaData` / `FieldMetaData`) and `order` (default 100).
Clients display actions by order, then label.

| Scope (`UserPreferenceActionScope`) | Target | Request rowKey |
|---|---|---|
| `SECTION` | Entire section; listName must be null | Forbidden |
| `LIST` | Existing list named by listName | Forbidden |
| `ROW` | Existing list named by listName and one loaded row | Required |

Names must be valid and unique within the section. Startup validation checks labels, scope,
list references and every input field with the same rules as section/list fields, including
reserved `userId`, supported types, duplicate names and referenced possible-value sources.
Clients must show `confirmationText` and obtain confirmation before invoking an action when it
is set. `isDestructive` tells clients to use destructive-action styling.

`UserPreferencesAction.runAction(sectionName, actionName, Map<String, ?> body)` first resolves a
visible section and registered action. Unknown actions return `Action not found` (404); hidden
sections also return 404. Only `rowKey` and `values` are accepted in the body. Unknown properties
return `Unexpected property: <name>` (400). `values`, when supplied, must be an object; input
fields use the same type coercion, editability, explicit-null required checks, maximum lengths,
ENUM membership and label-based errors as PATCH. Omitted values remain omitted, as in PATCH;
providers validate domain requirements and any mandatory inputs that were omitted.

For ROW actions, core loads the current session user's rows and finds the key in the action's
list. The row's `actionNames` must either be null (every ROW action of that list), or contain
the action name. An empty set allows none. Missing rows, another user's row keys, and disallowed
row actions return 400 before the provider runs. Providers must filter `load` by `context.getUserId()`
and retain ownership checks when performing mutations, including concurrent changes.

The provider receives `UserPreferenceActionRequest { actionName, listName, rowKey, values }`;
`listName` comes from metadata and `values` are coerced. `runAction` executes inside
`CorePermissionEnforcer.runTrusted` with the original session and client channel. The default
SPI implementation throws `QBadRequestException("This action isn't available.")`.

Providers return `UserPreferenceActionResult { message, secrets, focusRowKey }`. Core attaches
the reloaded `UserPreferenceSectionResult` as `section`. The shared core type
`com.kingsrook.qqq.backend.core.model.secrets.OneTimeSecret` has `label`, `value`, `masked`
(for example `qqq_…7f3a`) and `usageText` (for example a curl command; may contain newlines).
It is neither Serializable nor a QRecordEntity; `toString()` prints only label and masked.
It is returned once to the requesting client, never stored, logged, audited or put in process state.
`PreferenceSecretV1` exposes the same four fields. The HTTP response is
`PreferenceActionResponseV1 { section, message, secrets: PreferenceSecretV1[], focusRowKey }`.
The POST operation sends `Cache-Control: no-store`.

Secrets are shown **once** in the action response. Clients must not store, log, cache, include in
analytics, or retain them in preference state. Show the secret in a temporary copy dialog and
clear it when dismissed. Core does not log or audit action results or secret values. Providers
must never return secrets from `load`, put them into scalar/list values, or include them in
exceptions, logs or audit details. Persist only the application's appropriate verifier (such as
a token hash), and audit non-secret lifecycle metadata. A subsequent GET cannot recover a secret;
rotation produces a new one. Providers own issuance and rotation semantics; the generic core
does not persist or replay one-time results.

### Example: personal access tokens

The `access-tokens` section is owned by **qbit-authentik**; no token storage implementation is
installed by core. This example uses `PersonalAccessTokensProvider` for that qbit's token service.

```java
new UserPreferenceSectionMetaData()
   .withName("access-tokens").withShowInUserMenu(true).withLabel("Personal access tokens").withIcon(new QIcon("key"))
   .withProvider(new QCodeReference(PersonalAccessTokensProvider.class))
   .withList(new UserPreferenceListMetaData().withName("tokens").withLabel("Tokens").withRowLabel("Token")
      .withField(new QFieldMetaData("name", QFieldType.STRING).withLabel("Name").withIsEditable(false))
      .withField(new QFieldMetaData("expiresAt", QFieldType.DATE_TIME).withLabel("Expires at").withIsEditable(false)))
   .withAction(new UserPreferenceActionMetaData().withName("create").withLabel("Create token")
      .withScope(UserPreferenceActionScope.LIST).withListName("tokens").withOrder(10)
      .withInputField(new QFieldMetaData("name", QFieldType.STRING).withLabel("Name").withIsRequired(true).withMaxLength(100))
      .withInputField(new QFieldMetaData("expiresAt", QFieldType.DATE_TIME).withLabel("Expires at").withIsRequired(true)))
   .withAction(new UserPreferenceActionMetaData().withName("rotate").withLabel("Rotate token")
      .withScope(UserPreferenceActionScope.ROW).withListName("tokens").withOrder(20))
   .withAction(new UserPreferenceActionMetaData().withName("revoke").withLabel("Revoke token")
      .withScope(UserPreferenceActionScope.ROW).withListName("tokens").withOrder(30)
      .withIsDestructive(true).withConfirmationText("Revoke this token? It will stop working immediately."));
```

Illustrative provider action method (`tokens` is an application-owned service, and `IssuedToken`
is its result type; every service call is scoped to the session user):

```java
@Override
public UserPreferenceActionResult runAction(UserPreferenceRequestContext context, UserPreferenceActionRequest request) throws QException
{
   String userId = context.getUserId();
   if("revoke".equals(request.getActionName()))
   {
      tokens.revoke(userId, request.getRowKey());
      return new UserPreferenceActionResult().withMessage("Token revoked.");
   }

   IssuedToken issued;
   if("create".equals(request.getActionName()))
   {
      String name = (String) request.getValues().get("name");
      Instant expiresAt = (Instant) request.getValues().get("expiresAt");
      if(name == null || name.isBlank() || expiresAt == null)
      {
         throw new QBadRequestException("Name and Expires at are required.");
      }
      issued = tokens.create(userId, name, expiresAt);
   }
   else if("rotate".equals(request.getActionName()))
   {
      issued = tokens.rotate(userId, request.getRowKey());
   }
   else
   {
      throw new QBadRequestException("This action isn't available.");
   }
   return new UserPreferenceActionResult().withMessage("Token ready.").withFocusRowKey(issued.key())
      .withSecrets(List.of(new OneTimeSecret().withLabel("Personal access token")
         .withValue(issued.secret()).withMasked(issued.masked())
         .withUsageText("Copy it now; it won't be shown again.")));
}
```

`load(context)` lists only that user's non-secret token metadata. Active rows can return
`.withActionNames(Set.of("rotate", "revoke"))`; revoked rows can return `.withActionNames(Set.of())`.
Creation and rotation return a secret only in the immediate action result; revocation returns a
message with an empty secrets list. Core reloads the list after each successful operation.

## Email address accessor

`UserPreferenceRequestContext.getUserEmailAddress()` reads
`session.getValuesForFrontend().get("user")` as a map and returns its nonblank string `email`.
OAuth2 populates that map with `session.withValueForFrontend("user", {name, email})`.
If absent, blank or not a string, the accessor returns the session user's `idReference` only
when it is a syntactically valid email address, using the existing Apache Commons email validator.
Otherwise it returns null. It never reads a request body, query parameter or header. This gives
email-suppression providers a session-derived address without confusing an opaque user id with
an email; providers handle a null address according to their domain rules.
