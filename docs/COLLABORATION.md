# Record Collaboration

Status: spec, 2026-10-13. Build phases (a) to (i) are in section 13.

This spec adds record collaboration to QQQ: attachments, tags, user-defined fields, comments with
mentions, presence, sharing and favorites, plus following, an activity timeline, recents, a command
palette, tasks and owner/assignee. All of it is metadata-driven and frontend-agnostic. Every
operation is a spec'd `/qqq/v1` UI API endpoint with typed schemas, so Next UI, a CLI, a TUI and a
native client can implement the same features.

## Owner request (2026-10-13, verbatim)

1. "the ability for any record to have arbitrary files attached directly to it of any type."
2. "the ability for any record to have tags created on it, either approved tags, or created on the
   fly, and for the filters and quicksearch to be able to search by the tags."
3. "the ability for an admin user to create and design user defined fields for any table or record
   type at runtime, and those fields to be in the filter and indexed and in quick search as needed."
4. "the ability for any record to have an ongoing notes or comments (like google docs) on it, where
   you can mention others etc."
5. "the ability to see others in the same table or field as you (ie, if two people have the same
   record open at the same time, see that, like you can see that in google docs etc)."
6. "the ability to share a record with someone (get the link or share it in system with a comment,
   etc)"
7. "the ability for you to star or favorite a table, app, or record and have it show up in your
   favorites"

## Owner decisions (2026-10-13)

- **Packaging.** Core contracts in QRun-IO/qqq plus one qbit, `qbit-collaboration`
  (QRun-IO/qbit-collaboration, public).
  - Core provides:
    - record-features metadata per instance, app and table, overridable at runtime;
    - a generic record reference (table + primary key), whose extension rows inherit the parent's
      permissions and record locks;
    - query, filter and quick-search hooks for tags and user-defined fields;
    - a runtime metadata layer that turns user-defined fields into normal `QFieldMetaData`;
    - a realtime UI API channel (SSE over the ESB, multi-pod safe);
    - all endpoints as semver'd `/qqq/v1` operations.
  - The qbit provides the features: attachments, tags, user-defined fields, comments, presence,
    sharing, favorites, and the extras below.
- **User-defined field storage.** One JSONB column per enabled table, with generated expression
  indexes for filterable or searchable fields. No DDL per field change. Postgres first; an EAV
  value table is the fallback for other backends.
- **Extras in scope:**
  - follow/watch a record;
  - a per-record activity timeline;
  - recents;
  - a command palette;
  - tasks/reminders on records;
  - a standard owner/assignee with a "My records" view.
- **Out of scope for now:** real-time co-editing of field contents, version restore, and
  full-text search of attachment contents.
- **Permissions.** Each table gets separate grantable permissions: `<table>.comment`, `.tag`,
  `.attach`, `.share`, `.task` and `.assign`. By default, anyone who can edit the record has
  them. Reading follows record read access. The permissions appear in the qbit-authentik policy
  editor's catalog.

Owner decisions (2026-10-12): all recommendations accepted. Where this spec recommends, that is the
decision; adjust once it can be seen working.

## Mandatory standards that shape this design

| Standard | How this spec meets it |
|---|---|
| Data in standard, visible QQQ tables | Every persisted concept is a `collaboration_*` table with full record screens, secured by permissions and record locks (§4). The only non-table state is presence, which is ephemeral by the owner's decision (§9). |
| Table prefix and full-name permissions | Tables are `collaboration_<name>`. Qbit permissions are `collaboration.<action>`. Per-table feature permissions use the host table's name (`order.comment`). |
| Processes are record actions, never in the nav | Bulk tag/untag, bulk follow, Publish field, Archive field, Reindex field, Purge orphans and Grant access request are table or record actions with plain labels and standard streamed-ETL Review/Result summaries. Scheduled work (reminders, recents trim, index build, orphan purge) is hidden processes. |
| One home that owns its data | The qbit has one app, **Collaboration**, under Admin. Its home shows real numbers for its own tables (comments this week, open tasks, storage used by attachments, tag vocabulary size, fields by status, open access requests). User-facing surfaces live on the record screen and in the header, not in a second dashboard. |
| Nav lists only main tables | The nav shows Comments, Attachments, Tags, Custom Fields, Tasks, Shares, Access Requests and Feature Settings. Child tables (comment revisions, mentions, record tags, field values, follows) appear as child lists on their parents. Favorites and recents are per-user and reached from the header and palette; they stay visible as tables for admins (`denyBehavior HIDDEN` for others). |
| API matches the admin | All tables are published through the QQQ API middleware like any other table. Main tables publish ESB table events and are searchable in Quick Search where it makes sense (comments, attachments, tags, tasks). |
| Readable at all widths, light and dark | All Next UI surfaces are specified at 390 px and 1440 px (§11). Panels stack under the record at narrow widths, and nothing has an inner horizontal scroll. |
| Alerts through the Alerts SPI | Mentions, shares, follows, task reminders and access requests are user notifications through `QAlerts.notify` (#1125). Without the Alerts qbit they are recorded in the timeline and shown in-panel, and no notification is sent. |
| Storage through qbit-object-storage | Attachment bytes go through `ObjectStorage.bucket(...)`. Previews and thumbnails reuse the media preview path (§6.1). |

## 1. Architecture

```
                        ┌──────────── QQQ core (qqq-backend-core, qqq-middleware-javalin) ────────────┐
 Next / CLI / TUI ──►   │ /qqq/v1 core specs:                                                          │
   /qqq/v1              │   GET  /metaData/table/{t}  (+ recordFeatures, + runtime fields)             │
                        │   GET  /table/{t}/{pk}/features   (summary via providers)                    │
                        │   GET  /realtime/stream  (SSE)   POST /realtime/subscriptions                │
                        │   PUT  /table/{t}/{pk} with expectedVersion  (409 conflict)                  │
                        │ Contracts: RecordFeatures metadata + resolver, RecordReference,              │
                        │   RecordReferenceSecurity (pre-query + mirrored locks + parent check),       │
                        │   VirtualFieldCriteriaExpander (filter + quick search), QSubQuery criteria,  │
                        │   RuntimeMetaDataOverlay (UDF fields, feature overrides), JsonPathStorage,   │
                        │   RealtimeBus (in-memory | ESB topic), FeaturePermission catalog,            │
                        │   CurrentUser filter expression, RecordVersion check                         │
                        └───────────────▲─────────────────────────────────────────▲────────────────────┘
                                        │ SPIs                                    │ ESB topic qqq.realtime
                        ┌───────────────┴──── qbit-collaboration ─────────────────┴────────────────────┐
 /qqq/v1/qbit/          │ tables collaboration_*  ·  extension specs  ·  record actions                │
   collaboration/...    │ favorites · recents · attachments · tags · UDF designer · comments · follow  │
                        │ timeline · presence · shares · access requests · tasks · assignments         │
                        └───────┬──────────────────────┬───────────────────────┬──────────────────────┘
                                │ ObjectStorage        │ QAlerts.notify        │ RecordSearchProvider
                        qbit-object-storage      qbit-alerts (optional)   qbit-quick-search (optional)
```

**Rule of the split.** Core holds every contract that a frontend or another qbit depends on and
that needs to change core behavior: query translation, security, metadata publication and the
realtime transport. The qbit holds the data, the business rules and the qbit endpoints. A host
without the qbit pays nothing: every core hook is a no-op until something registers.

## 2. Record features metadata (core)

### 2.1 Model

`com.kingsrook.qqq.backend.core.model.metadata.collaboration`:

```java
public enum RecordFeature { ATTACHMENTS, TAGS, CUSTOM_FIELDS, COMMENTS, PRESENCE, SHARING,
                            FAVORITES, FOLLOW, TIMELINE, RECENTS, TASKS, ASSIGNMENT }

new RecordFeaturesMetaData()                    // QSupplemental{Instance,App,Table}MetaData, name "recordFeatures"
   .withEnabled(RecordFeature.COMMENTS, true)
   .withEnabled(RecordFeature.ATTACHMENTS, true)
   .withSetting(RecordFeature.TAGS, "mode", "APPROVED_ONLY")   // feature-specific settings (typed accessors in the qbit)
   .withPermissionMode(RecordFeature.COMMENTS, FeaturePermissionMode.EDIT_OR_GRANTED)
   .withOwnership(new RecordOwnership().withOwnerFieldName("ownerUserId").withAssigneeFieldName("assigneeUserId"));
```

- It can be declared at three levels: the instance (`QInstance` supplemental), an app (applies to
  the tables in that app's sections) and a table. The most specific level wins, per feature and
  per setting.
- **Runtime overrides** come from `RecordFeatureOverrideProviderInterface` (an SPI; the qbit
  implements it over `collaboration_feature_setting`). They are applied last, so an admin can
  switch a feature on or off for an instance, app or table without a deploy. Overrides go through
  the runtime overlay (§5), so every pod sees the change within one overlay refresh.
- **Hard limits that overrides cannot lift:**
  - A table must support the operations a feature needs. For example, COMMENTS needs the table to
    support GET, and CUSTOM_FIELDS needs an RDBMS backend or the EAV fallback.
  - A table can declare `withFeatureLocked(feature, false)` to forbid a feature outright, for
    example attachments on an audit table.
  - The qbit's own `collaboration_*` tables never get record features, so there are no comments
    on comments.
- `RecordFeaturesResolver.resolve(QTableMetaData)` returns `ResolvedRecordFeatures`, a map from
  feature to enabled, settings and permission mode. It is cached per overlay generation.
- **Defaults.** Without the qbit, the resolver returns nothing enabled. With the qbit, its config
  says which features default on (recommended: FAVORITES, RECENTS and TIMELINE for every table
  with a primary key; everything else off until enabled per app or table).

### 2.2 Frontend publication

`QFrontendTableMetaData` gains `recordFeatures`, typed and published in the OpenAPI as
`RecordFeaturesV1`. It holds the enabled features, the public settings (for example the tag mode
and the max attachment size) and the session's capabilities for that table:

```json
"recordFeatures": {
  "features": ["COMMENTS", "ATTACHMENTS", "TAGS", "FOLLOW", "TIMELINE", "FAVORITES", "RECENTS"],
  "settings": { "TAGS": { "mode": "APPROVED_ONLY" }, "ATTACHMENTS": { "maxBytes": 104857600 } },
  "capabilities": { "comment": true, "attach": true, "tag": false, "share": true, "task": false, "assign": false, "createTag": false }
}
```

Capabilities are table-level ("may, if the record allows"). Record-level answers come from
`GET /table/{t}/{pk}/features` (§10.1), which applies the record's locks.

### 2.3 Feature permissions

- Each permission-bearing feature is registered through the core **table feature permission**
  hook, which it shares with qbit-data-analysis's `<table>.analyze` (DATA-ANALYSIS.md §3.6). The
  hook is `TableFeaturePermissionsMetaData`, holding `TableFeaturePermission`s, each with:
  - `suffix` and `label`;
  - `appliesTo`, a serializable `(QInstance, QTableMetaData) -> boolean`; here, "this feature is
    enabled for the table";
  - `impliedBy`, a serializable `(QInstance, QTableMetaData) -> TablePermissionSubType`; here,
    EDIT under `EDIT_OR_GRANTED` and null under `GRANTED_ONLY`.
- `PermissionsHelper.getAllAvailablePermissions` then lists `<table>.comment`, `<table>.attach`,
  `<table>.tag`, `<table>.share`, `<table>.task` and `<table>.assign`, using the table's
  permission base name. That list feeds the qbit-authentik policy editor catalog, where these
  permissions show under a generic FEATURE action per table.
- `PermissionsHelper.hasTableFeaturePermission(...)` is the table-level check. It is true when the
  user has table READ and either holds the explicit permission or holds the implied table
  permission.
- **Permission mode** is set per feature:
  - `EDIT_OR_GRANTED` is the default. A user may use the feature if they hold the feature
    permission, or if they can edit the record (table edit permission plus the parent's WRITE
    record locks).
  - `GRANTED_ONLY` requires the explicit feature permission.
  - In both modes the user must also be able to read the parent record.
- **Global qbit permissions:**
  - `collaboration.view` lets a user see the admin tables.
  - `collaboration.admin` covers feature settings, the tag vocabulary for all tables, moderation
    (editing or deleting others' comments), the UDF designer and access-request handling.
  - `collaboration.fields.manage` covers the UDF designer only.
  - `collaboration.tags.manage` covers approving and merging tags.
- `CorePermissionEnforcer` treats feature permissions like any other custom permission: a missing
  permission returns 403 from the UI API.

## 3. Record reference and security inheritance (core)

### 3.1 RecordReference

```java
public record RecordReference(String tableName, Serializable primaryKey) { String primaryKeyAsString(); }
```

Extension tables store `tableName` (VARCHAR 128) and `recordId` (VARCHAR 64, the primary key as a
string; composite keys are not supported, and the validator rejects features on such tables).
They are declared with:

```java
new QTableMetaData().withName("collaboration_comment")
   .withSupplementalMetaData(new RecordReferenceTableMetaData()
      .withTableNameField("tableName").withRecordIdField("recordId")
      .withFeature(RecordFeature.COMMENTS)          // which feature permission governs writes
      .withMirroredLocks(true));
```

### 3.2 How extension rows inherit security

A user can read an extension row only if they can read its parent record. They can write one only
if the feature permission rule (§2.3) passes for the parent. The checks below are enforced by core
for every table carrying `RecordReferenceTableMetaData`, through every path: UI API, model API,
processes and the assistant.

1. **Table-level check, in the query.** A pre-query hook adds
   `tableName IN (<tables the session can READ that have the feature enabled>)` to every query,
   count and aggregate. Counts, paging and widgets are therefore correct.
2. **Record locks, in the query (mirrored lock values).** This is the same mechanism as the Alerts
   spec §7.2.
   - At write time, core copies the parent record's values for each of its `RecordSecurityLock`s
     into mirrored lock columns on the extension row. There is one nullable column per security key
     type listed in `RecordReferenceSecurityConfig.mirroredSecurityKeyTypes`, for example
     `lockKey_clientId`.
   - The extension tables declare ordinary `RecordSecurityLock`s on those columns, with
     `nullValueBehavior = ALLOW`, so QQQ enforces them natively in every query.
   - Single-tenant hosts configure no key types and get no extra columns.
   - When a parent's lock field changes, for example an order moves to another client, a core
     `RecordChangeListener` updates the mirrored columns of its extension rows in the same
     transaction.
3. **The parent check, for pinned queries and every write.**
   - A query pinned to one parent (`tableName = X AND recordId = Y`, which is what every UI panel
     sends) first runs `GetAction` on the parent with the user's session. If the parent is not
     visible, the result is empty, or a 404 from the qbit endpoints. This covers locks that
     cannot be mirrored: multi-record locks through joins, and locks on key types not in the
     mirrored list.
   - Every insert, update and delete of an extension row checks the parent the same way, then
     applies the feature permission rule. A write to a parent that is not visible fails with the
     same "not found" as a missing record, so it does not reveal that the record exists.
4. **Unpinned queries over parents with unmirrored locks** (an admin browsing all comments) are
   post-filtered: the rows are grouped by parent table, and the parents are re-read in batches
   with the user's session. Paging refills from the next page until it is full or 5 batches have
   been read. When post-filtering applies, counts are reported as `approximate: true` in the UI
   API. Hosts that want exact counts list their key types in `mirroredSecurityKeyTypes`.
5. **Own-row rules** sit on top of inheritance and are declared per table with
   `RecordReferenceTableMetaData.withAuthorField("createdByUserId")`. Editing or deleting your own
   comment, attachment or task needs only the feature permission. Editing or deleting someone
   else's needs `collaboration.admin`. Comments can never be edited by anyone else; they can only
   be deleted, as moderation.

**Parent deleted.** A core `RecordChangeListener` (DELETE) calls
`RecordReferenceCleanupInterface` implementations after commit. The qbit deletes the parent's tags,
follows, favorites, recents, presence and tasks. It marks comments, shares and the activity rows
`parentDeleted = true`, which hides them from every non-admin query, and purges them after 30 days
(configurable). It deletes attachments through object storage, where buckets with versioning or
trash keep them restorable for the bucket's retention. There is no cascade inside the parent's own
transaction, so a slow storage call cannot fail a delete.

## 4. Data model (qbit `collaboration_*` tables)

Every table has `id`, `createDate` and `modifyDate`. Audit is on (FIELD level) for tables that
people edit. Extension tables carry `tableName` and `recordId`, plus the mirrored lock columns.
They are indexed on (`tableName`, `recordId`).

| # | Table (label) | Key fields | Notes |
|---|---|---|---|
| 1 | `collaboration_feature_setting` (Feature Settings) | `scopeType` (INSTANCE / APP / TABLE), `scopeName`, `feature`, `enabled`, `settings` (JSON), `permissionMode` | unique (`scopeType`, `scopeName`, `feature`). Runtime overrides (§2.1). Admin only. |
| 2 | `collaboration_user` (People) | `userId` (unique, the session's `QUser.idReference`), `displayName`, `email`, `avatarUrl`, `lastSeenAt`, `active` | A directory filled lazily on each user's first collaboration call, used for mention autocomplete and display names. Replaced or enriched by `UserDirectoryProviderInterface` (§8.3) when one is registered, e.g. by qbit-authentik. |
| 3 | `collaboration_favorite` (Favorites) | `userId`, `targetType` (TABLE / APP / RECORD / PROCESS / REPORT / SAVED_VIEW), `targetName`, `recordId`, `label` (cached), `sortOrder`, `folder` | unique (`userId`, `targetType`, `targetName`, `recordId`). Record-locked on `userId`. |
| 4 | `collaboration_recent` (Recently Viewed) | `userId`, `targetType`, `targetName`, `recordId`, `label`, `lastViewedAt`, `viewCount` | unique (`userId`, `targetType`, `targetName`, `recordId`). Record-locked on `userId`. Trimmed to 200 per user by a hidden daily job. |
| 5 | `collaboration_attachment` (Attachments) | `tableName`, `recordId`, `fileName`, `contentType`, `sizeBytes`, `sha256`, `bucketName`, `storageKey`, `thumbnailKey`, `description`, `createdByUserId`, `parentDeleted` | ESB events, Quick Search over `fileName` and `description`. |
| 6 | `collaboration_tag` (Tags) | `name` (normalized lowercase), `label`, `color`, `tableName` (null = global vocabulary), `approved`, `description`, `createdByUserId`, `usageCount` (denormalized), `mergedIntoTagId` | unique (`tableName`, `name`). Quick Search. |
| 7 | `collaboration_record_tag` (Record Tags) | `tableName`, `recordId`, `tagId`, `createdByUserId` | unique (`tableName`, `recordId`, `tagId`). Child of tag. |
| 8 | `collaboration_field_definition` (Custom Fields) | `tableName`, `name`, `label`, `type` (STRING / TEXT / INTEGER / DECIMAL / BOOLEAN / DATE / DATE_TIME / SELECT / MULTI_SELECT / USER / RECORD_LINK), `options` (JSON for SELECT), `linkTableName`, `required`, `defaultValue`, `minValue`, `maxValue`, `maxLength`, `helpText`, `sectionName`, `sortOrder`, `filterable`, `searchable`, `indexState` (NONE / BUILDING / READY / FAILED), `status` (DRAFT / ACTIVE / ARCHIVED), `storage` (JSONB / EAV), `createdByUserId` | unique (`tableName`, `name`). The designer (§6.3). |
| 9 | `collaboration_field_value` (Custom Field Values, EAV fallback) | `tableName`, `recordId`, `fieldName`, `valueText`, `valueNumber`, `valueDate`, `valueDateTime`, `valueBoolean` | unique (`tableName`, `recordId`, `fieldName`). Only used for tables whose backend has no JSON column support. |
| 10 | `collaboration_comment` (Comments) | `tableName`, `recordId`, `threadId` (root comment id; null on roots), `parentCommentId`, `body` (markdown), `fieldName` (optional anchor), `authorUserId`, `editedAt`, `resolvedAt`, `resolvedByUserId`, `deletedAt`, `deletedByUserId`, `parentDeleted` | (`tableName`, `recordId`, `createDate`). ESB events, Quick Search over `body`. |
| 11 | `collaboration_comment_revision` (Comment History) | `commentId`, `body`, `editedAt`, `editedByUserId` | Child of comment. Written on every edit. |
| 12 | `collaboration_mention` (Mentions) | `commentId`, `mentionedUserId`, `visible` (could the user read the record at send time), `notifiedAt` | Child of comment. |
| 13 | `collaboration_follow` (Followers) | `tableName`, `recordId`, `userId`, `reason` (MANUAL / AUTHOR / COMMENTED / MENTIONED / ASSIGNED / SHARED), `notifyOnChange`, `notifyOnComment`, `mutedUntil` | unique (`tableName`, `recordId`, `userId`). |
| 14 | `collaboration_activity` (Activity) | `tableName`, `recordId`, `at`, `actorUserId`, `type` (COMMENTED / COMMENT_EDITED / COMMENT_RESOLVED / FILE_ADDED / FILE_REMOVED / TAGGED / UNTAGGED / SHARED / FOLLOWED / TASK_CREATED / TASK_DONE / ASSIGNED / FIELD_CHANGED_CUSTOM), `summary`, `refTableName`, `refId`, `parentDeleted` | (`tableName`, `recordId`, `at`). The timeline merges this with core audit at read time (§7.2). |
| 15 | `collaboration_share` (Shares) | `tableName`, `recordId`, `fromUserId`, `toUserId`, `message`, `recipientCouldRead`, `sentAt`, `openedAt` | |
| 16 | `collaboration_access_request` (Access Requests) | `tableName`, `recordId`, `requesterUserId`, `shareId`, `message`, `status` (OPEN / GRANTED / DECLINED / WITHDRAWN), `handledByUserId`, `handledAt`, `note` | Not record-reference-secured (the requester cannot read the parent): visible to the requester (lock on `requesterUserId`) and to `collaboration.admin`. Shows table and record id, never record data. |
| 17 | `collaboration_task` (Tasks) | `tableName`, `recordId`, `title`, `notes` (markdown), `assigneeUserId`, `dueAt`, `remindAt`, `remindedAt`, `status` (OPEN / DONE / CANCELLED), `completedAt`, `completedByUserId`, `createdByUserId` | (`assigneeUserId`, `status`, `dueAt`). ESB events, Quick Search over `title`. |
| 18 | `collaboration_record_assignment` (Record Owners) | `tableName`, `recordId`, `ownerUserId`, `assigneeUserId` | unique (`tableName`, `recordId`). Only for tables without their own owner/assignee columns (§7.5). |

**Retention defaults** (configurable): recents keep 200 per user, activity is kept forever, and
rows whose parent was deleted are purged after 30 days.

## 5. Runtime metadata overlay (core)

User-defined fields and feature overrides change at runtime and must look exactly like code-defined
metadata to every consumer: frontend metadata, filters, query, API middleware, exports and the
assistant.

- **SPI.** `RuntimeMetaDataProviderInterface` (instance-level list) returns a
  `RuntimeMetaDataContribution`:
  - per table, fields to add (`QFieldMetaData` with backend storage, §6.3), section placement,
    search fields to add, and virtual fields to add (tags, owner/assignee);
  - feature overrides;
  - a `generation` (long) that changes whenever the contribution changes.
- **Apply.** `RuntimeMetaDataOverlay.refresh(qInstance)`:
  1. Collects the contributions.
  2. Builds new `QTableMetaData` clones for the affected tables (copy-on-write).
  3. Runs the field-level enricher and validator rules on them.
  4. Swaps them into the instance under the instance's write lock.
  - A contribution that fails validation is skipped and logged. The previous generation of that
    table stays in force, so a bad field can never take a table down.
  - Code-defined fields always win name clashes. The designer prevents clashes, and the validator
    is the backstop.
- **When it runs.** At startup, after producers and before the server starts. After a local change,
  when the qbit calls `refresh` in the after-commit of the change. On other pods, from the realtime
  bus message `qqq.metadata.changed` (§9), with a 60 s poll of the generations as the fallback
  when no bus is configured.
- **Frontend.** The `/metaData` responses carry `metaDataGeneration`. A UI that sees it change,
  or receives `metadata.changed` on the stream, refetches table metadata.
- **No DDL at refresh.** The overlay only changes metadata. Storage needs are met by the one-time
  JSONB column (§6.3).

## 6. Feature design: storage and query

### 6.1 Attachments

- **Upload.** `POST /qqq/v1/qbit/collaboration/records/{table}/{pk}/attachments` is multipart, one
  or more files, with an optional `description`. The server:
  1. Checks the parent and the `attach` permission.
  2. Streams the bytes into the configured bucket (default `collaboration`, role PRIVATE) at
     `attachments/{table}/{pk}/{uuid}/{sanitizedFileName}`.
  3. Computes `sha256`.
  4. Inserts the row.
  5. Writes FILE_ADDED activity.
  6. Publishes the realtime event.
  The size limit is per table (setting `maxBytes`, default 100 MB, capped by the bucket's
  `maxUploadBytes`). Any type is allowed. An optional `blockedExtensions` setting is empty by
  default.
- **Large files.** When the bucket supports presigned PUT,
  `POST .../attachments/upload-url` returns a presigned URL and a pending row. The client uploads
  directly, then calls `POST .../attachments/{id}/complete`, which HEADs the object and finalizes
  the row. The CLI uses the same flow.
- **Download and preview.** `GET .../attachments/{id}/download` always goes through the app
  server: it checks the parent, then redirects to a 5-minute presigned GET with
  `Content-Disposition: attachment`. The URL is never stored.
  `GET .../attachments/{id}/preview` does the same with `inline`, for the types the media preview
  supports (images, PDF, video, audio, text). Thumbnails for images are generated on upload with
  the object-storage media thumbnail helper and stored at `thumbnailKey`. Next reuses its media
  preview components. Other types show a file card.
- **Delete** is a record action, "Delete attachments", with a standard Review/Result. It deletes
  through object storage, soft where the bucket allows. The panel's delete icon runs the same
  process for one record.
- **Not in scope:** full-text search of attachment contents. File names and descriptions are
  searchable.

### 6.2 Tags

- **Vocabulary.** A tag is global (`tableName` null) or per table. A table's tag mode is a feature
  setting:
  - `FREE` (the default): anyone with `tag` may create a tag on the fly by typing a new name.
  - `APPROVED_ONLY`: only approved tags can be applied. New names become suggestions
    (`approved = false`) that a `collaboration.tags.manage` holder approves, merges or deletes
    from the Tags table with the record actions "Approve tags" and "Merge tags".
- **Normalization.** Names are trimmed, lowercased and collapsed to single spaces; the label keeps
  the case as typed. A colon-prefixed name such as `priority:high` is just a name; there are no
  hierarchies in v1.
- **Apply.** `PUT .../records/{t}/{pk}/tags` with `{add: [...], remove: [...]}`. The bulk record
  actions **Add tags** and **Remove tags** are added to every table with TAGS enabled. They are
  standard streamed ETL: pick tags, Review ("Add 2 tags to 340 orders; 12 already tagged"), then
  Result.
- **Filter.** Core virtual field `collaborationTags` (label "Tags", type STRING,
  `possibleValueSourceName = "collaboration_tag"` filtered to the table's vocabulary,
  `isQueryCriteria = true`, `isQuerySelectable = true`). It is added by the qbit through the
  overlay to every table with TAGS enabled.
  - Supported operators: IN, NOT_IN, IS_BLANK, IS_NOT_BLANK, plus "has all of" (new
    `QCriteriaOperator.CONTAINS_ALL`, defined for virtual fields only).
  - Its `VirtualFieldCriteriaExpander` (§6.5) rewrites `collaborationTags IN (a, b)` to
    `id IN (SELECT recordId FROM collaboration_record_tag WHERE tableName = 'order' AND tagId IN (a, b))`.
- **Column.** As a selectable virtual field, Tags can be added as a grid column. A POST_QUERY
  hook fills it with one batched query per page.
- **Quick search.**
  - The table's search fields gain `collaborationTags`. Through the same expander, a default
    search for `urgent` also matches records tagged with a tag whose label contains `urgent`.
  - A search term starting with `#` (`#urgent`) matches tags only, exactly. Core's `RecordSearchAction`
    recognizes the prefix and passes it to expanders as `SearchTermKind.TAG`.
  - With qbit-quick-search, the provider indexes `collaborationTags` like any search field. That
    is coordinated with that qbit and needs no core change beyond the field being a search field.

### 6.3 User-defined fields

- **Storage (Postgres).**
  - Each enabled table has one column, `qqq_custom_fields JSONB NOT NULL DEFAULT '{}'`, plus a
    GIN index `USING gin (qqq_custom_fields jsonb_path_ops)`.
  - The host adds both once, in its own migration. `CollaborationSchema.customFieldsDdl(table)`
    prints the statements. Enabling CUSTOM_FIELDS on a table without the column marks the feature
    unavailable, with a visible reason on the Feature Settings record and the Custom Fields home
    card. It never fails a query.
  - Field changes never run DDL on the table.
- **Field metadata.** An ACTIVE definition becomes a normal `QFieldMetaData` through the overlay,
  with:
  - the name `cf_<name>` (the prefix prevents clashes with code fields and future columns);
  - the type and label;
  - `isRequired`, display format and help content;
  - a possible value source: generated enum PVS for SELECT, the `collaboration_user` PVS for USER,
    the link table's PVS for RECORD_LINK;
  - `backendStorage = new JsonPathStorage("qqq_custom_fields", "<name>")`.
  The field is placed in the table section named by `sectionName`, or in a "Custom fields"
  section that the overlay adds after the last section.
- **Core: `JsonPathStorage`.** A new `QFieldMetaData.backendStorage`. The RDBMS module translates:
  - select: `(qqq_custom_fields->>'name')` cast to the field type (`::numeric`, `::date`,
    `::timestamptz`, `::boolean`);
  - where and order by: the same expression;
  - equality and IN: `qqq_custom_fields @> '{"name": value}'`, so the GIN index applies;
  - insert and update: set the merged `jsonb` with `qqq_custom_fields || jsonb_build_object(...)`
    for changed keys only, and `- 'name'` for nulls, so concurrent updates of different custom
    fields do not overwrite each other;
  - MULTI_SELECT: a JSON array, filtered with `?|`, or `@>` for all.
  The memory backend and MongoDB support `JsonPathStorage` natively, as a nested map.
- **Indexes.** Equality and IN use the GIN index. A field marked `filterable` with a range or
  sort use (number, date) or `searchable` gets a **generated expression index**:
  - The hidden job `collaborationBuildFieldIndexes` creates it with
    `CREATE INDEX CONCURRENTLY IF NOT EXISTS cf_<table>_<name> ON <table> (((qqq_custom_fields->>'<name>')::<type>))`,
    or `gin_trgm_ops` for searchable text when `pg_trgm` is present.
  - Progress shows in `indexState` (BUILDING / READY / FAILED, with the error on the record).
  - Archiving a field drops its index the same way. It is the only DDL the feature runs: it never
    locks the table for writes, and it is generated by the qbit, not hand-written per field.
  - Hosts that forbid runtime DDL set `withManagedIndexes(false)`. The job then only prints the
    statements on the field record, for a migration.
- **EAV fallback (other backends).** Values live in `collaboration_field_value`. The field gets
  `backendStorage = new ExternalValueStorage(...)`: a virtual field that is selectable (filled by a
  batched POST_QUERY hook) and filterable through a criteria expander, `id IN (SELECT recordId
  FROM collaboration_field_value WHERE ...)`. Writes go through a post-insert/update hook in the
  same transaction where the backends match, after commit otherwise. It is slower and documented
  as such.
- **Designer** (Admin -> Collaboration -> Custom Fields): a standard table with a full record
  screen. It has a "Preview" widget that renders the field as it will look in the form and the
  filter. It has record actions **Publish field** (DRAFT -> ACTIVE), **Archive field** (hides it;
  values stay in the JSON; restorable by Publish) and **Rebuild index**.
  - Only `collaboration.fields.manage` holders can edit definitions.
  - **Validation:** names are `[a-z][a-z0-9_]{0,40}`; type changes are only allowed in DRAFT; an
    ACTIVE field's options can be added and renamed by label, never removed while values use them.
    The Review step counts the values that would be affected.
  - **Values in forms:** custom fields are edited in the normal create and edit forms, are
    validated by core's field validation (required, min, max, length), and are audited like any
    field.
- **Filter and quick search.** A filterable field is a normal field in the filter builder. A
  searchable field is added to the table's search fields by the overlay.

### 6.4 Owner and assignee, "My records"

- Tables declare `RecordOwnership` (owner and assignee field names, both pointing to user ids) in
  their features metadata. For tables without such columns, the qbit provides the
  `collaborationOwner` and `collaborationAssignee` virtual fields, backed by
  `collaboration_record_assignment` and filterable through the expander.
- Core adds the filter expression `CurrentUser` (`${currentUser}` in saved filters; the
  frontend's value chip is "Me"). Any user-id field can then be filtered to the signed-in user.
- **"My records"** is a core, metadata-generated saved view on each table with ownership: owner =
  me OR assignee = me. The UI shows it in the table's view menu, and there is a cross-table
  "My work" page (§11) listing, per table, the records assigned to me plus my open tasks.
- **Assign** is a record and bulk action ("Assign to", "Take ownership") with the standard
  Review/Result. It needs `<table>.assign`, or the edit-based default. Assigning notifies the
  assignee and makes them a follower.

### 6.5 Core query hooks

- **`QSubQuery` criteria value.** `new QFilterCriteria("id", IN, new QSubQuery("collaboration_record_tag", "recordId", filter))`.
  - RDBMS emits `IN (SELECT ...)` when both tables are in the same backend.
  - Otherwise core materializes the sub-query to an id list, up to `QSubQuery.MAX_MATERIALIZED`
    (default 10,000). Above that it throws a `QUserFacingException`: "This filter matches too
    many records to combine across databases."
  - The memory backend evaluates it directly.
  - The sub-query runs with the same session, so its own security applies.
- **`VirtualFieldCriteriaExpanderInterface`**, set on `QVirtualFieldMetaData.withCriteriaExpander`:

  ```java
  QQueryFilter expand(QTableMetaData table, QFilterCriteria criteria, CriteriaExpansionContext ctx);
  QQueryFilter expandSearchTerm(QTableMetaData table, String term, SearchTermKind kind, CriteriaExpansionContext ctx); // null = no match
  ```

  `QueryAction`, `CountAction` and `AggregateAction` expand criteria on such fields before
  calling the backend. `RecordSearchAction`'s default search ORs `expandSearchTerm` into its
  contains filter.
- **`RecordVersionCheck`.** `QTableMetaData.withRecordVersionField("modifyDate")` is the default
  for tables with a modify date. `UpdateInput.withExpectedVersions(Map<pk, value>)` makes the
  update compare first. On a mismatch it fails that record with `RecordVersionConflictException`,
  which carries the current version, `modifiedByUserId` (when the table has one) and the changed
  field names from audit when available. The v1 update endpoint accepts `expectedVersion`; a
  conflict is a 409 with `RecordConflictV1`. Callers that do not send a version are unaffected.

## 7. Feature design: comments, follow, timeline, sharing, tasks

### 7.1 Comments and mentions

- **Threads.** Root comments sit on the record. They are optionally anchored to a `fieldName`, in
  which case they show by that field and in the panel with a field chip. Replies are one level
  deep, under the root.
- **Body.** The body is markdown (CommonMark plus GFM tables and task lists), rendered with
  sanitized output in every client. Mentions are written as `@[Display Name](user:<userId>)`. The
  composer inserts them from `GET .../mentions/candidates?table&pk&q` (people from the directory,
  ranked by those who already follow or commented on the record). Record links
  `[label](record:<table>/<pk>)` render as chips.
- **On post:**
  1. Insert the comment.
  2. Parse the mentions and insert `collaboration_mention` rows.
  3. For each mentioned user, check whether they can read the record (§8.3). Users who can are
     notified through `QAlerts.notify` (category SHARE, `sourceTableName` and `sourceRecordId` set,
     so the Alerts qbit's recipient visibility check also applies) and are made followers
     (MENTIONED). Users who cannot get no notification and no follow; the composer warned before
     sending (§11).
  4. Make the author a follower (COMMENTED).
  5. Notify the other followers who have `notifyOnComment`.
  6. Write COMMENTED activity.
  7. Publish the realtime event.
- **Edit.** Authors only. The previous body goes to `collaboration_comment_revision`, and
  `editedAt` is set. "Edited" links to the history, which anyone who can read the comment can see.
- **Delete.** Soft: `deletedAt` is set and the body is shown as "Comment deleted". Authors delete
  their own; `collaboration.admin` moderates. Hard deletion happens only with parent cleanup.
- **Resolve.** Resolving a thread collapses it. Anyone with `comment` can resolve or reopen.
- **Ordering.** Oldest first within a thread; threads newest activity first. The panel
  paginates 20 threads at a time.

### 7.2 Follow and timeline

- **Follow.** Users can follow and unfollow from the record header. Users are followed
  automatically when they author, comment, are mentioned, are assigned or are shared with.
  `notifyOnChange` defaults to on for MANUAL and ASSIGNED follows and off for the others.
  `notifyOnComment` defaults to on.
  - **Change notifications** come from a qbit `RecordChangeListener` on tables with FOLLOW.
    Changes are batched per record and follower over 5 minutes ("Order 1042: status, total changed
    by Sam"). The change itself is never sent back to the user who made it. Recipients are checked
    for read access at send time.
  - Bulk **Follow** and **Unfollow** are table actions.
- **Timeline.** `GET .../records/{t}/{pk}/timeline?before&limit&types` merges, newest first:
  - core audit rows for the record (through the audit tables, with the user's normal audit read
    permission; field values the user cannot see are masked as in the audit screen);
  - `collaboration_activity`.
  Each item is a typed `TimelineItemV1` (kind, at, actor, summary, details, ref). The endpoint
  pages by a `(at, kind, id)` cursor. Without TIMELINE enabled, the endpoint returns 404.

### 7.3 Sharing and access requests

- **Copy link.** `GET .../records/{t}/{pk}/link` returns the canonical app URL
  (`{baseUrl}/app/{table}/{pk}`, from the instance's configured frontend base URL) and a plain
  label. Clients may build it themselves. It is an endpoint so that CLI and TUI clients get the
  same URL. A link carries no token and no grant.
- **Share in app.** `POST .../records/{t}/{pk}/shares` with `{toUserIds, message}`. It needs
  `share` (default: anyone who can edit; owners can lower it to anyone who can read by setting
  GRANTED_ONLY and granting `.share` widely).
  - For each recipient, the server checks read access (§8.3).
    - **Recipients who can read** get a notification ("Sam shared Order 1042 with you: <message>")
      with the link, and are made followers (SHARED).
    - **Recipients who cannot read** are not sent the record. The sender sees the warning first,
      in the share dialog's preview step. If they confirm, the recipient gets a notification
      without record data: "Sam wants to share a record with you in Orders. Request access?" The
      link opens the access-request page.
  - Sharing never grants access.
- **Request access.** Opening a record link without access shows the standard not-found page
  with "Request access" when the table has SHARING enabled. The message never says whether the
  record exists. `POST /qqq/v1/qbit/collaboration/access-requests` with
  `{tableName, recordId, message}` creates the request.
  - It notifies holders of `collaboration.admin` and of the table's configured
    `accessApproverPermission` (a setting; default `collaboration.admin`). It also notifies the
    record's owner, if the record has one.
  - Requests are handled in the Access Requests table with **Mark granted** and **Decline**.
    Granting means the approver changed roles or locks in the identity system. The action records
    the outcome and notifies the requester; it does not change permissions itself.

### 7.4 Tasks and reminders

- A task is a record-attached to-do with a title, notes, an assignee (any directory user who can
  read the record; others are refused with a clear error), a due time and a remind time.
- `remindAt` defaults to `dueAt` minus 1 day, or the due time itself for due times less than a day
  away.
- The hidden job `collaborationSendTaskReminders` runs every minute. It notifies the assignee once
  at `remindAt` and again when the task becomes overdue.
- **Mark done** and **Reopen** are record actions and inline toggles.
- **"My tasks"** is a saved view on `collaboration_task`: assignee = me, status OPEN, ordered by
  due date. It is shown on the "My work" page and in the header's work menu.
- Creating or completing a task writes activity and notifies followers who have
  `notifyOnChange`.

### 7.5 Favorites and recents

- **Favorites** may be a table, an app, a process, a report, a saved view or a record.
  - `PUT /qbit/collaboration/favorites` with `{targetType, targetName, recordId?}` toggles on;
    `DELETE` toggles off; `GET` lists them in `sortOrder`. `PATCH .../favorites/order` reorders
    and sets folders.
  - Labels are cached when starred and refreshed on list (for records, through one batched query
    per table). Records the user can no longer see are listed as "Unavailable" with a remove
    button, never with their old label.
- **Recents.**
  - `POST /qbit/collaboration/recents` records a view `{targetType, targetName, recordId?}`. The
    record screen calls it once per open; table and app screens call it too.
  - The server upserts the row (`lastViewedAt`, `viewCount + 1`), throttled to once per minute per
    target.
  - `GET` returns the most recent 50, re-checked for visibility like favorites, with unavailable
    rows dropped.
  - Next's existing local recents (`recent-records`) are the fallback when the qbit is absent.
- **Command palette (Next).** The existing `CommandMenu` gains:
  - a **Favorites** group, shown first when the search is empty;
  - server recents in place of local recents when the qbit is present;
  - actions "Star this page" and "Unstar this page";
  - "Go to My work";
  - `#tag` record search.
  The palette is composed client-side from metadata, `/search`, favorites and recents, so no new
  "palette" endpoint is needed. CLI and TUI clients compose the same sources.

## 8. Security summary

### 8.1 Matrix

| Operation | Needs |
|---|---|
| See any feature panel | READ on the parent table and the parent visible under its locks |
| Comment, reply, resolve | the above, plus `comment` (§2.3) |
| Edit or delete own comment | `comment` |
| Delete others' comments | `collaboration.admin` |
| Upload, delete own attachment | `attach` |
| Delete others' attachments | `collaboration.admin` |
| Apply, remove tags | `tag` |
| Create a tag (FREE mode) | `tag` |
| Approve, merge tags; edit vocabulary | `collaboration.tags.manage` |
| Share | `share` |
| Create, edit own task; complete a task assigned to you | `task` |
| Assign owner or assignee | `assign` |
| Favorite, recents, follow | READ on the parent and the record visible |
| Design custom fields | `collaboration.fields.manage` |
| Feature settings, access requests | `collaboration.admin` |
| Edit custom field values | the normal table edit permission and locks (they are fields) |

### 8.2 Leak rules

- Responses never say whether a record that is not visible exists.
  - Not-found and no-access return the same 404 with the same body.
  - Access requests show only the table label and the id the requester typed or followed.
- Notifications are rendered for, and checked against, each recipient at send time.
- Realtime events carry ids and types, never record data. Clients refetch through permission-
  checked endpoints (§9).
- Favorites and recents of records that become invisible show "Unavailable".
- Mention candidates come from the user directory, which shows names and emails but not who can
  see what. The "can't see this record" warning is shown only to someone who can already read the
  record and is choosing to mention.

### 8.3 Checking another user's access

Notifications, mentions, shares and task assignees all need "can user U read record R".

- Core adds `RecordAccessCheckerInterface`: `canRead(userId, RecordReference) -> Optional<Boolean>`.
  `Optional.empty()` means unknown.
- The qbit's default implementation:
  - uses a registered `UserSessionProviderInterface` to build U's session (permissions and
    security keys) and runs the parent `GetAction` as U;
  - qbit-authentik provides such a provider. The default one, without an identity qbit, can
    build sessions only for the current user and returns unknown for others.
- **Unknown** is treated as "cannot read" for sending record data. The sender sees "We can't
  confirm Sam can open this record", and the recipient gets only the request-access style
  notification. The Alerts qbit's own recipient check still applies on top.

## 9. Realtime channel and presence

### 9.1 Transport (core)

- **Stream.** `GET /qqq/v1/realtime/stream` is Server-Sent Events.
  - One stream per browser tab. Next shares one stream across tabs with a `BroadcastChannel`
    leader.
  - The server sends `hello` (with `connectionId`, `heartbeatSeconds` = 25 and
    `metaDataGeneration`), then events.
  - A comment ping is sent every 25 s, so proxies keep the connection open.
  - Reconnects use `Last-Event-ID`. The server keeps a 2-minute per-connection replay buffer in
    memory. A reconnect outside it gets `resync`, and clients refetch what they show.
- **Subscriptions.** `POST /qqq/v1/realtime/subscriptions` with
  `{connectionId, add: [channel...], remove: [channel...]}`.
  - Channels:
    - `record:{table}:{pk}`: record events and presence;
    - `table:{table}`: table-level presence and "records changed" hints for list screens;
    - `user:me`: the server maps it to the session's user; notifications, mentions, shares and
      tasks;
    - `instance`: `metadata.changed`.
  - Every `add` is permission-checked: a record channel needs the record visible, a table channel
    needs table READ. Refused channels are returned in `refused`, without saying why.
  - The checks are repeated every 5 minutes and on the session's expiry or logout, when the server
    closes the stream with `event: session-ended`.
  - Limits: 200 channels per connection and 20 connections per session.
- **Events.** Typed schemas in the OpenAPI, one per `event:` name:
  - `record.changed {table, pk, version, changedBy, fields?}`, where `fields` lists names only and
    only the fields the subscriber can see;
  - `record.deleted`;
  - `presence.changed {table, pk?, viewers: [PresenceEntryV1]}`;
  - `comment.added | comment.updated | comment.deleted {table, pk, commentId, threadId}`;
  - `attachment.changed {table, pk}`, `tags.changed {table, pk}`, `task.changed {table, pk, taskId}`;
  - `notification.created {notificationId?}` (a hint for the bell);
  - `metadata.changed {generation}`;
  - `resync`, `session-ended`.
- **Multi-pod.** `RealtimeBusInterface` has `publish(RealtimeMessage)` and
  `subscribe(Consumer<RealtimeMessage>)`.
  - `InMemoryRealtimeBus` is the default for a single pod.
  - `EsbRealtimeBus` (in `qqq-esb`) publishes to the topic named by
    `QEsbInstanceMetaData.realtimeDestinationName` (default `qqq.realtime`). Each pod consumes
    with a **non-durable, non-shared** subscription, like the ESB's control topic, so every pod
    sees every message and delivers it to its own local connections.
  - Messages are small JSON (channel, event, data). Delivery is best-effort with no redelivery;
    clients refetch on `resync`.
  - Producers:
    - core's `RecordChangeListener` publishes `record.changed` and `record.deleted` after commit,
      only for tables with PRESENCE or FOLLOW enabled, so the bus stays quiet elsewhere;
    - the qbit publishes its own events;
    - the overlay publishes `metadata.changed`.
- **Middleware.** The stream is a spec'd `AbstractEndpointSpec` with `text/event-stream` (§1 of
  `UI-API-EXTENSIONS.md`). Javalin's SSE client runs on a virtual thread per connection. The
  connection registry is per pod.

### 9.2 Presence (qbit)

- **Heartbeat.** `POST /qqq/v1/qbit/collaboration/presence` with
  `{connectionId, table, pk?, mode: VIEWING|EDITING, fieldName?}`.
  - The client sends it on open, on mode and field change, and every 20 s.
  - Entries expire 45 s after the last heartbeat. `DELETE` removes one on close, with
    `navigator.sendBeacon` on unload.
  - The heartbeat re-checks access, so an entry for a record the user can no longer see is
    dropped.
- **Store.** Each pod keeps a presence map replicated over the realtime bus: each heartbeat is
  published as `presence.heartbeat`, and each pod applies it locally with the TTL. On a change it
  publishes `presence.changed` to `record:` and `table:` channels.
  - The map holds at most one entry per (user, connection, target) and is capped at 50,000
    entries per pod. Above the cap, new targets are refused with a logged warning.
  - Nothing is persisted. This is the one sanctioned exception to "data in tables", by the owner's
    decision ("ephemeral, heartbeats").
  - Admins can see live entries in a read-only **Who's here** widget on the Collaboration home.
- **What users see:**
  - On a record: avatars of the other viewers. Editors get a pencil badge, and the focused field
    shows a colored ring with the editor's name on the field label.
  - On a table list: avatars in the header ("3 people viewing Orders"). Rows that others are
    editing get a small dot.
  - A user's own other tabs are merged into one entry. `GET .../presence?table&pk` gives the same
    data without a stream, for clients without SSE.
- **Conflict warnings.**
  - When another user saves a record I am editing, `record.changed` arrives. The edit form shows
    "Sam saved changes to status, total. Your edits are not saved yet." with **Review changes**,
    which shows the new values next to mine.
  - On save, the form sends `expectedVersion`. A 409 shows the same dialog, with
    **Keep mine (overwrite)** (re-sends with the new version) and **Use theirs**.
  - Fields only I changed merge silently, because the update sends only changed fields.
  - This is a warning, not co-editing. Real-time co-editing is out of scope.

## 10. UI API

Core endpoints are plain `specs/v1` classes. Qbit endpoints use the extension point
(`MiddlewareSpecExtension` named `collaboration`, semver `1.0.0`, prefix
`/qqq/v1/qbit/collaboration`) with schema names prefixed `Collaboration`. Request and response
bodies are JSON unless noted. Every list endpoint pages with `limit` (default 20, max 100) and an
opaque `cursor`. All endpoints that take `{table}/{pk}` run the parent check (§3.2) first and
return 404 on failure.

### 10.1 Core (`/qqq/v1`, UI API minor bump to 1.3.0, after #1132's 1.2.0)

| Method and path | Purpose | Request | Response schema |
|---|---|---|---|
| `GET /metaData/table/{t}` (existing) | adds `recordFeatures`, runtime fields and `metaDataGeneration` | | `TableMetaDataResponseV1` + `RecordFeaturesV1` |
| `GET /metaData` (existing) | adds `metaDataGeneration` | | |
| `GET /table/{t}/{pk}/features` | record-level feature summary: enabled features, my capabilities on this record, counts (comments open, attachments, tags list, followers, open tasks), `isFavorite`, `isFollowing`, owner/assignee, presence snapshot | | `RecordFeatureSummaryV1` (filled by `RecordFeatureSummaryProviderInterface`s; core alone returns features and capabilities) |
| `PATCH /table/{t}/{pk}` (existing update) | accepts `expectedVersion` | `{values, expectedVersion?}` | 409 `RecordConflictV1 {currentVersion, changedFields, modifiedByUserId, modifiedAt}` |
| `GET /realtime/stream` | SSE | `Last-Event-ID` header | `text/event-stream`, events `Realtime*EventV1` |
| `POST /realtime/subscriptions` | add or remove channels | `RealtimeSubscriptionRequestV1` | `RealtimeSubscriptionResponseV1 {active, refused}` |

`GET /version` lists the `recordFeatures` and `realtime` core feature names when present.

### 10.2 Qbit (`/qqq/v1/qbit/collaboration`)

| Method and path | Purpose |
|---|---|
| `GET/PUT/DELETE /favorites`, `PATCH /favorites/order` | list, star, unstar, reorder |
| `GET/POST /recents` | list, record a view |
| `GET/POST /records/{t}/{pk}/attachments` | list, upload (multipart) |
| `POST /records/{t}/{pk}/attachments/upload-url`, `POST /attachments/{id}/complete` | direct upload |
| `GET /attachments/{id}/download`, `GET /attachments/{id}/preview`, `GET /attachments/{id}/thumbnail` | 302 to presigned URL |
| `PATCH/DELETE /attachments/{id}` | rename or describe; delete one |
| `GET /tags?table&q` | vocabulary for a table (approved, plus mine pending) |
| `POST /tags` | create (FREE) or suggest (APPROVED_ONLY) |
| `GET/PUT /records/{t}/{pk}/tags` | list; add/remove |
| `GET/POST /records/{t}/{pk}/comments` | threads (paged), post |
| `PATCH/DELETE /comments/{id}`, `POST /comments/{id}/resolve`, `POST /comments/{id}/reopen`, `GET /comments/{id}/history` | |
| `GET /mentions/candidates?table&pk&q` | people, with `canRead: true/false/unknown` |
| `GET/PUT/DELETE /records/{t}/{pk}/follow` | my follow state and settings |
| `GET /records/{t}/{pk}/followers` | list |
| `GET /records/{t}/{pk}/timeline` | merged timeline |
| `GET /records/{t}/{pk}/link` | canonical link |
| `POST /records/{t}/{pk}/shares/preview` | per-recipient `canRead` before sending |
| `POST /records/{t}/{pk}/shares` | send |
| `POST /access-requests`, `GET /access-requests/mine` | request, list mine |
| `GET/POST /records/{t}/{pk}/tasks`, `PATCH /tasks/{id}`, `GET /tasks/mine` | |
| `PUT /records/{t}/{pk}/assignment` | owner/assignee (for tables using the qbit's assignment table; tables with their own columns use the normal update) |
| `GET /people?q`, `GET /people/{userId}` | directory |
| `POST/DELETE /presence`, `GET /presence?table&pk` | heartbeat, leave, snapshot |
| `GET /my-work` | records assigned to me per table (counts and first page links), my open tasks, unread mentions |

Each operation's schemas are in the qbit's committed fragment
`src/main/resources/openapi/qqq-v1/collaboration.yaml`, checked by
`UiApiContractAssertions.assertExtensionMatchesCommitted`. Bulk operations (tag, untag, follow,
assign, delete attachments) are processes, run through the existing `/processes` endpoints.

## 11. UX (Next UI) and CLI/TUI rendering

### 11.1 Next UI

- **Record screen.**
  - The header gains: a star (favorite); a follow bell; avatars for presence; a share button
    (copy link, share with people); and tag chips under the title, with an "add tag" combobox.
  - A **Collaboration panel** has tabs Comments, Files, Tasks and Activity. At 1440 px it is a
    right-side column, 360 px wide and collapsible. At 1024 px and below it is a section under the
    record's sections, with the tabs as a segmented control, and there is still a single page
    scrollbar.
  - Counts show on the tabs. Field-anchored comments show a speech-bubble marker by the field.
- **Custom fields** show in their section like any field. In edit forms, presence rings show on
  focused fields.
- **Table screen.**
  - The Tags column and the Tags filter are available.
  - Presence avatars show in the header.
  - "My records" is in the view menu.
  - The bulk actions Add tags, Remove tags, Follow, Unfollow, Assign to and Take ownership are in
    the actions menu, with the standard process screens.
- **Header.**
  - A star menu lists favorites, grouped by folder, with "Manage" leading to the user's favorites
    table view.
  - The bell is the Alerts qbit's.
  - A "My work" link leads to the cross-table page.
- **Command palette.** As in §7.5.
- **Share dialog.**
  - A copy-link field.
  - A people picker that shows a "can't open this record" badge from `shares/preview`.
  - A message box.
  - On send, the result lists who was notified and who was offered access.
- **Comments composer.**
  - A markdown editor with toolbar, preview, `@` autocomplete and `#` record link autocomplete.
  - A warning chip appears for mentioned people who can't read the record ("Sam can't see this
    record and won't be notified").
- **Conflict dialog.** As in §9.2.
- **Widths and themes.**
  - Every surface is checked at 390 px and 1440 px, in light and dark, with Playwright
    screenshots in the PRs.
  - Chips wrap and never scroll sideways.
  - Panels have no fixed heights that would cause a second scrollbar.
- **Code splitting.** The comments editor and markdown renderer, the share dialog, the panel tabs
  and the presence layer are lazy chunks loaded only when the table has the feature. Each PR
  reports the bundle size change (report-only).
- **Acceptance.** Playwright on Chromium, Firefox, WebKit and mobile WebKit, against the
  qqq-sample-project demo (§12).

### 11.2 CLI and TUI notes

- Every feature is reachable from the endpoints above. There are no browser-only behaviors.
- **CLI:**
  - `qqq comment add order 1042 --body @file.md` and `qqq comment list order 1042`;
  - `qqq attach order 1042 ./invoice.pdf` uses `upload-url`, then `complete`;
  - `qqq tag order 1042 +urgent -later`;
  - `qqq share order 1042 --to sam --message "..."`;
  - `qqq favorites` and `qqq my-work`;
  - `qqq watch order 1042` subscribes to the SSE stream and prints events as lines.
- **TUI.** The record view has a side pane with the same tabs. Presence shows as initials in the
  status bar. Conflicts use the same 409 contract.
- **Markdown** is rendered as text in terminals (bold, lists, code). Mentions show as `@Name`.

## 12. Test plan and demo

- **Core unit tests:**
  - features resolution across levels and overrides;
  - permission catalog entries and both permission modes;
  - the pre-query table filter;
  - mirrored lock copy and update on a parent lock change;
  - the pinned-query parent check;
  - post-filtered paging with `approximate`;
  - `QSubQuery` on memory and RDBMS (H2 and Postgres through Testcontainers), and cross-backend
    materialization and its limit;
  - the virtual field expander in query, count, aggregate and search;
  - the `#tag` search;
  - `JsonPathStorage` select, where, order and partial-update merge (two concurrent updates of
    different keys both survive);
  - the overlay: refresh, a skipped invalid contribution and generation change;
  - `RecordVersionCheck` conflicts;
  - `CurrentUser` expression;
  - the realtime bus (in-memory, and ESB on Artemis through Testcontainers, two consumers both
    receive);
  - the SSE spec with replay, resync, refused channels and session end;
  - the UI API contract test for 1.3.0.
- **Qbit tests (H2, plus Postgres through Testcontainers for custom fields):**
  - each feature's endpoints for the happy path, no-permission (403), invisible parent (404) and
    own versus others' rows;
  - mentions to users who can and cannot read, with notifications captured by a test alert sink;
  - follow batching;
  - timeline merge and cursor;
  - share to a non-reader producing the access-request notification only;
  - access request visibility;
  - task reminders with a fixed clock;
  - recents throttling and trim;
  - favorites showing "Unavailable" after a lock change;
  - attachments against MinIO (Testcontainers) for upload, presigned flow, download redirect and
    delete;
  - UDF publish, archive, type rules, index job states and EAV fallback on the memory backend;
  - bulk actions through the streamed ETL with Review/Result lines;
  - the extension OpenAPI fragment check.
  - Coverage meets the repo standard's gates.
- **Next UI:**
  - unit tests per component;
  - Playwright acceptance for: star and unstar from the record and the palette; the palette
    favorites group; tag and filter by tag; `#tag` search; upload, preview and delete a file;
    comment with a mention, edit, resolve; follow; timeline; two browser contexts on the same
    record showing presence and the conflict dialog; share with the "can't open" warning; task
    create and complete; My work;
  - all of it on 4 browsers.
- **Demo.** `qqq-sample-project` enables all features on its sample tables (person, order) with
  seeded users and real records. Screenshots of the record screen, table screen, palette, share
  dialog, conflict dialog, Collaboration home and Custom Fields designer, light and dark, at
  390 px and 1440 px.

## 13. Phases

Each phase is one or more green PRs. Core goes to `QRun-IO/qqq` `develop` and ships in the next
4.1.x RC. Qbit work goes to `qbit-collaboration` `develop`. Next UI goes as separate PRs to
`qqq-frontend-next` `main`. Until a needed core change is released, the qbit uses a small internal
stand-in, marked for replacement, or waits for the RC; each PR says which.

| Phase | Core | Qbit | Next UI |
|---|---|---|---|
| a. Contracts | `RecordFeaturesMetaData` + resolver + frontend publication; `RecordReference`, `RecordReferenceTableMetaData` and the security rules of §3.2 (table filter, mirrored locks, parent check, post-filter, cleanup hook); feature permissions in the catalog; `RecordFeatureSummaryProviderInterface` and `GET /table/{t}/{pk}/features`; `RuntimeMetaDataProviderInterface` and overlay (generation only, no fields yet); `RecordAccessCheckerInterface`, `UserSessionProviderInterface` | repo scaffold, qbit config, `collaboration_feature_setting`, `collaboration_user`, override provider, home skeleton | types for `RecordFeaturesV1` |
| b. Favorites, recents, palette | | favorites, recents, endpoints, summary provider | star in record and table headers, favorites menu, palette favorites group, server recents |
| c. Attachments | | attachments with object storage, previews and thumbnails, delete action | Files tab, upload, preview |
| d. Tags | `QSubQuery`; virtual field criteria expander in query, count, aggregate and search; `CONTAINS_ALL`; `#tag` search | tags, vocabulary modes, bulk actions, filter field | tag chips, Tags filter and column, `#tag` in palette and search |
| e. Comments, follow, timeline | | comments, revisions, mentions, follows, activity, timeline merge with audit, notifications | Comments and Activity tabs, composer, follow bell |
| f. User-defined fields | `JsonPathStorage` in RDBMS, memory and MongoDB; overlay field contributions | definitions, designer, publish and archive, index job, EAV fallback | designer preview widget; fields render through normal metadata |
| g. Realtime and presence | `RealtimeBus` (in-memory, ESB), SSE spec, subscriptions, `record.changed` producer, `RecordVersionCheck` with 409 | presence endpoints and replicated map, events for its features | stream client, presence avatars and field rings, conflict dialog, live panel refresh |
| h. Sharing | | link, shares with preview, access requests | share dialog, request-access page |
| i. Tasks, owner/assignee | `CurrentUser` expression, ownership metadata, "My records" view | tasks, reminders, assignment table and virtual fields, assign actions, My work | Tasks tab, My work page, assign actions |

Releases: the coordinator is told "ready: QQQ RC" after a core phase merges, "ready:
qbit-collaboration release" once phases (a) to (c) are merged and green against a released RC, and
"ready: Next RC" after each Next phase.

## 14. Migration and backends

- **Schema.** `CollaborationSchema.postgresScript(config)` prints the DDL for the qbit's tables,
  generated from the metadata, for the host's Liquibase or Flyway. `customFieldsDdl(table)`
  prints the one-time JSONB column and GIN index per table. Mirrored lock columns are included
  when key types are configured. Adding a key type later is a host migration plus the hidden
  backfill process **Refresh mirrored locks**.
- **Backends.**
  - The qbit's tables can live in any QQQ backend that supports query, count and transactions.
    PostgreSQL is the intended store.
  - Custom fields: Postgres uses JSONB; MySQL 8 and H2 can use `JsonPathStorage` with their JSON
    functions (`->>`; H2 for tests only, without indexes); any other backend uses the EAV
    fallback.
  - Sub-queries are native within one RDBMS backend and materialized across backends (§6.5).
- **Upgrading hosts.** Nothing changes until the qbit is installed and a feature is enabled.
  Existing tables keep working with no columns added, except for CUSTOM_FIELDS, which needs the
  one-time column. kof22 adoption is a draft PR sequenced by the coordinator, taking the next free
  Liquibase number at that time.
- **Compatibility.** UI API 1.3.0 is additive. The `expectedVersion` 409 applies only to callers
  that send it.

## 15. Coordination

- **Related-record picker (#1131).** The RECORD_LINK custom field type and the share dialog's
  people picker reuse the picker's `SINGLE` mode and its v1 endpoints. This spec adds no picker
  endpoints.
- **UI API program (#1127 to #1132).** The core endpoints here follow #1128's spec conventions and
  version after #1132 (1.2.0 -> 1.3.0). The qbit uses the extension point from #1128.
- **Alerts (#1125, qbit-alerts).** All notifications go through `QAlerts.notify`, with category
  SHARE for mentions and shares and JOB for reminders.
- **qbit-authentik.** It provides `UserSessionProviderInterface` and `UserDirectoryProviderInterface`
  for accurate "can they read it" checks. The new permissions appear in its policy editor through
  the core catalog.
- **qbit-quick-search.** It indexes the virtual and custom search fields.
