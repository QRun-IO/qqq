# User preferences (`/qqq/v1/preferences`)

Status: implemented in core and the v1 UI API. Owner decisions (2026-10-12): all recommendations accepted.
Applies from the QQQ 4.1.0 RC that ships it (UI API 1.2.0, additive).

## 0. Summary

A user sees and changes their own settings in one place: the Preferences page (user menu), or a
shortcut from a feature (for example the gear in the notification bell). Settings come from
**sections** that core, qbits and applications register. Each section is a typed field list built
from ordinary `QFieldMetaData`, plus optional row lists (for example one row per notification
category). Clients render sections generically from the metadata, so Next UI, a CLI, a TUI and the
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
3. A section may have row lists (fixed rows supplied by the provider; no add or remove through
   preferences). Per-row read-only fields carry a reason ("Security notices can't be muted").
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
  permissionName, fields, lists, provider (`QCodeReference` to a
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
}
```

- `UserPreferenceRequestContext`: the section, the session and `getUserId()`. The user id is always
  `session.getUser().getIdReference()`; nothing from the request can set it.
- `UserPreferenceValues`: scalar `values` (missing means the field default) and `lists`
  (list name to rows).
- `UserPreferenceRow`: `key`, `label`, `description`, `values`, `readOnlyFieldNames`,
  `readOnlyReason`.
- `UserPreferenceChange`: changed scalar `values`, and per list the changed rows (`key` plus the
  changed `values`, represented by `UserPreferenceRowChange`). Values are already coerced to the
  field's Java type when `store` sees them. A duplicate row key within one patch is rejected.

Providers write through ordinary table actions, so table audit rules record the change, the user
and the client channel.

## 4. Core action and store

`UserPreferencesAction` (`com.kingsrook.qqq.backend.core.actions.preferences`):

- `listSections()` returns the visible sections with their values, ordered.
- `getSection(name)` returns one, or throws `QNotFoundException` when unknown or hidden.
- `patchSection(name, change)` validates, calls the provider's `validate`, then `store` inside
  `runTrusted`, and returns the reloaded section. Errors throw `QBadRequestException` with every
  message.

The result model is `UserPreferenceSectionResult` (`section` plus `values`). The action uses
the current `QContext` session. Its raw-body `Map<String, ?>` overload preserves unknown properties
for validation; its `UserPreferenceChange` overload supports typed in-process callers.
Provider loads also run in the trusted scope, so reading and reloading preferences do not require
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

`PreferenceSectionV1`: `name`, `label`, `icon` (`Icon`), `description`, `order`, `appName`,
`fields` (`FieldMetaData[]`), `values` (object), `lists` (`PreferenceListV1[]`: `name`, `label`,
`description`, `rowLabel`, `fields`, `rows`), and `possibleValues` (source name to
`PossibleValueOption[]`, for enum sources used by the section). `PreferenceRowV1`: `key`, `label`,
`description`, `values`, `readOnlyFields`, `readOnlyReason`.

PATCH body `PreferenceSectionPatchRequestV1`: `{ "values": {...}, "lists": { "<list>": [ { "key":
"...", "values": {...} } ] } }`. Any other top-level property, including `userId`, is a 400.

`/preferences` is reserved by core; `preferences` is listed in the version's features.

## 6. Clients

- **Preferences page** (Next: `/app/preferences`, from the user menu). Lists every section.
  Next's device-only display settings appear as a "Display" section on the same page.
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
