# In-app help content

QQQ help is data: rows in the `helpContent` table are applied to the running `QInstance` and
shipped to clients in the `/qqq/v1` metadata. This document records what core supports as of
develop `a3ae4903b` (2026-10-13), what is missing, and the plan to make help complete.

## 1. Audit: what exists today

### Objects that can carry help

| Object | Key format (`helpContent.key`) | Held on | In `/qqq/v1` metadata | Next UI renders |
|---|---|---|---|---|
| Instance | `instanceLevel:true;slot:{s}` | `QInstance.helpContent` (map by slot) | `MetaDataResponseV1.helpContents` (untyped object) | Only the query screen's filter-paster slots |
| App | `app:{a};slot:{s}` | `QAppMetaData.helpContent` | **No.** `AppMetaData` (v1) does not expose it | No |
| Table | `table:{t};slot:{s}` | `QTableMetaData.helpContent` | `TableMetaData(Light).helpContents` (untyped object) | No (no table header help) |
| Table section | `table:{t};section:{s}` | `QFieldSection.helpContents` | `TableSection.helpContents` (untyped array) | Edit forms and view screen section headers |
| Table field | `table:{t};field:{f}` | `QFieldMetaData.helpContents` | `FieldMetaData.helpContents` (untyped array) | Edit/insert form labels, view labels (`FieldLabel`), query column headers (`DataGrid`) |
| Process | none: `process:{p}` alone is logged as "unrecognized key format" | not supported | no | no |
| Process step | `process:{p};step:{s}` (`*suffix` wildcard on the process) | `QFrontendStepMetaData.helpContents` | `FrontendStep.helpContents` (untyped array) | Process step screen header |
| Process field | `process:{p};field:{f}` | the input/output `QFieldMetaData` | `FieldMetaData.helpContents` | Process forms, bulk-load mapping |
| Report | none | not supported | no | no |
| Widget | `widget:{w};slot:{s}` (slot `label` or a table column) | `QWidgetMetaData.helpContent` | `WidgetMetaData.helpContent` (untyped object) | Widget title tooltip, table-widget column headers |
| Possible-value source | none | not supported | no | no |
| Record / table actions | none (a process key is the closest) | not supported | no | no |
| Anything else | other keys go to `QHelpContentPlugin` supplemental metadata | plugin-defined | plugin-defined | no |

Field help on joined/exposed-join fields comes from the joined table's field.

### Roles (where help shows)

`QHelpRole`: `ALL_SCREENS`, `READ_SCREENS`, `WRITE_SCREENS`, `QUERY_SCREEN`, `VIEW_SCREEN`,
`EDIT_SCREEN`, `INSERT_SCREEN`, `PROCESS_SCREEN`, `APP_SCREEN`, `TABLE_ACTION_MENU`.
`HelpRole` is an interface so applications can add roles in code.

- The table's role possible-value source (`HelpContentRole`) lists only the first eight;
  `APP_SCREEN` and `TABLE_ACTION_MENU` cannot be stored as data.
- One role per row; the unique key is `(key, role)`. A row's roles replace an existing entry with the
  same role set on the target object.
- Next picks, per screen, the most specific role first (`help-utils.ts`), falling back to an entry
  with no roles.

### Storage and management

- Table `helpContent` (`HelpContentMetaDataProvider`, opt-in per application): `id`, `key`
  (250 chars), `role`, `format`, `content`, `createDate`, `modifyDate`. Standard record screens.
- `loadHelpContent(qInstance)` reads every row; applications call it at startup. Insert, update
  and delete customizers apply the change to the live instance, so edits take effect without a
  deploy on the node that made them. Other nodes see it only after their next restart or reload.
- No authoring UI beyond the generic record screens. Material's `?helpHelp` mode (and Next's
  `helpHelpActive`) appends each slot's key to its tooltip so authors can see where help can go.
- No coverage report: nothing lists objects that lack help.

### Format

`HelpFormat`: `TEXT`, `HTML`, `MARKDOWN`. Markdown is rendered server-side with commonmark into
`contentAsHtml`, with **raw HTML passed through**. Next sanitizes HTML and rendered markdown with
DOMPurify before rendering, so it is safe in Next. Other clients get unsanitized HTML.

### Targeting

- By screen role only. There is no targeting by permission, user, client channel or tenant.
  Help is visible to anyone who can see the object.
- No i18n: one content per key and role.
- No versioning beyond `modifyDate`. Audit is not enabled on the table by default.

### `/qqq/v1` UI API (spec 1.2.0)

- Help is only in the metadata responses, typed as a bare `array` or `object` with no item schema.
  Clients cannot generate types for it; Next hand-writes `QHelpContent`.
- No help search endpoint. The assistant's `search_help` tool (`SearchHelpTool`, `AiHelpText`)
  searches app, table, field, process and widget help in-process, filtered by visibility.

### Next UI

Renders help in the places marked above with an info-icon `HoverTooltip` (keyboard-focusable,
`aria-describedby`). Missing: app and table headers, process and report launch points, record
action menus, a long-help panel, and help search.

## 2. Decision

**Owner decisions (2026-10-12): all recommendations accepted.**

- Storage, application and enforcement stay in core: the existing `helpContent` table, extended.
  The table keeps its name `helpContent`: a pre-standard exception to the lowercase snake_case
  rule (approved 2026-10-13). It predates the standard, applications reference it, and renaming
  it is a breaking change for every adopter.
- A thin `qbit-help` holds only the authoring experience: the Help app, its coverage dashboard,
  in-context "Add help", preview, bulk edit and import/export. It reads core through the
  `/qqq/v1` API and core classes only.
- Markdown is the default format, rendered to a safe subset: raw HTML is escaped and unsafe URLs
  are dropped. `HTML` stays for compatibility; it is sanitized by clients.

## 3. Plan (built: core QRun-IO/qqq#1177, Next QRun-IO/qqq-frontend-next#127, authoring QRun-IO/qbit-help#1)

### Core

1. **Every object.** Add keys for `process:{p};slot:{s}`, `report:{r};slot:{s}` and
   `possibleValueSource:{pvs};slot:{s}`. Record and table actions are processes; their help is
   process help with role `TABLE_ACTION_MENU`. Add `APP_SCREEN` and `TABLE_ACTION_MENU` to the
   role possible values.
2. **Summary plus long help.** New column `summary` (short plain text for tooltips). `content`
   is the long help. When `summary` is empty, clients derive a tooltip from `content`.
3. **Targeting.** New columns `permissionName` (shown only to users who hold that permission) and
   `clientChannel` (shown only to that `X-QQQ-Client` channel). Filtering happens when the
   per-user frontend metadata is built.
4. **Typed payloads.** A `HelpContent` schema (`content`, `summary`, `format`, `contentAsHtml`,
   `roles`) referenced from every help property, and help added to `AppMetaData`,
   `ProcessMetaData(Light)` and `ReportMetaData`. Listed under the open UI API version (1.2.0 while QQQ 4.1.0-RC.6 is uncut; 1.3.0 after).
5. **Search.** `GET /qqq/v1/help/search?q=` (`HelpSearchSpecV1`) returning ranked, permission-
   filtered results with links, shared with the assistant's `search_help` tool.
6. **Coverage.** `GET /qqq/v1/help/coverage` (`HelpCoverageSpecV1`), for users who may insert
   `helpContent`: every helpable object, its key, and whether it has a summary and long help.
7. **Reload.** `QInstanceHelpContentManager.reloadHelpContent` clears and reapplies all rows, so a
   bulk import or another node's edit can be applied without a restart.

### Next UI

Info icons and tooltips on app and table headers, list column headers, section headers, field
labels (edit and view), widgets, process steps and record actions; a "?" side panel with the long
help; help results in the command palette; "Add help" on hover for authors.

### Authoring (`qbit-help`)

Help app home with the coverage dashboard, in-context editing and preview.

## 4. Writing help

### Keys

| Target | Key | Notes |
|---|---|---|
| App | `app:{app};slot:help` | Shown on the app home header (`APP_SCREEN`). |
| Table | `table:{table};slot:help` | Query, view, edit and create headers. |
| Table section | `table:{table};section:{section}` | |
| Table field | `table:{table};field:{field}` | Labels on edit and view, list column headers. |
| Process | `process:{process};slot:help` | Process header (`PROCESS_SCREEN`); its action-menu item (`TABLE_ACTION_MENU`). `*suffix` matches every process ending in `suffix`. |
| Process step | `process:{process};step:{step}` | |
| Process field | `process:{process};field:{field}` | |
| Report | `report:{report};slot:help` | |
| Widget | `widget:{widget};slot:label` | Column help uses the column name as the slot. |
| Possible-value source | `possibleValueSource:{pvs};slot:help` | Search and coverage. |
| Instance | `instanceLevel:true;slot:{slot}` | |

### Rows

- `summary`: one plain sentence, at most about 120 characters. It is the tooltip.
- `content`: the long help, in Markdown (`format` = `MARKDOWN`). Raw HTML is escaped and unsafe links are dropped.
- `role`: usually `ALL_SCREENS`. Use a more specific role only when one screen needs different words.
- `permissionName` and `clientChannel`: leave both empty unless the help is for a narrower audience.

### Coverage report

`GET /qqq/v1/help/coverage?missingOnly=true` lists every helpable object that has no help yet.
You can narrow it with `kind=` (`app`, `table`, `section`, `field`, `process`, `step`,
`processField`, `report`, `widget`, `possibleValueSource`, `instance`) and `parentKey=` (for example
`table:person`). It needs insert or edit permission on `helpContent`. The Help app home in
`qbit-help` shows the same report with "Add help" links. In Next, add `?helpHelp` to any URL to
see each slot's key in place.

### Applying changes

Inserts, edits and deletes apply immediately on the node that makes them. Other nodes pick them up
within 30 seconds. After a bulk import straight into the database, run "Apply help changes now"
from `qbit-help`, or call `QInstanceHelpContentManager.reloadHelpContent`.

## 5. Keeping help current (owner decision 2026-10-14)

Seeding with insert-if-missing SQL leaves the text frozen at whatever it said when it was first
written. Instead, help ships with the code that it describes, and a sync keeps the database in step
with it without overwriting anything a person has edited.

### 5.1 Bundles

- A **help bundle** is a classpath resource at `help/{bundleId}.yaml` (it is read from the jar,
  never from disk):

  ```yaml
  bundle: qbit-alerts          # unique id
  version: 0.3.0               # the owning artifact's version
  layer: QBIT                  # CORE | QBIT | HOST
  entries:
    - key: table:alerts_alert;slot:help
      role: ALL_SCREENS        # optional, defaults to ALL_SCREENS
      summary: One problem that needs attention, with its status and owner.
      content: |
        An alert is raised by a monitor ... default {{default:alerts_alert.severity}} ...
    - key: table:alerts_alert;field:legacyCode
      retired: true            # explicit removal
  ```
- Core, each qbit and the host register their bundles with `HelpBundles.register(qInstance, "help/x.yaml")`.
  Qbits register theirs from their producer.
- Layering: **CORE < QBIT < HOST < USER**. For a given `(key, role)` the highest bundle layer
  supplies the text the sync seeds. An edit made in the UI (USER) always wins over any bundle.

### 5.2 Provenance on each row

`helpContent` gains these columns:
- `source`: CORE, QBIT, HOST or USER.
- `bundleId` and `bundleVersion`.
- `lastSeededHash`: a SHA-256 of the summary, content and format the sync last wrote.
- `upstreamStatus`: CURRENT, UPSTREAM_CHANGED or RETIRED.
- `upstreamSummary` and `upstreamContent`: the bundle's newer text, kept while the status is UPSTREAM_CHANGED.
- `modifiedBy`.

A row with no `bundleId` is USER-created.

### 5.3 Sync (startup, and on demand)

The sync runs at startup, in the background. It runs as a durable job once the durable job kernel
(qqq#1178) is in the instance, and as an async process otherwise. Admins can also run it from the
Help app as "Sync help from bundles". For each winning bundle entry:

| Database row | Action |
|---|---|
| none | insert: source is the entry's layer, hash is set, status CURRENT |
| hash of the current text equals `lastSeededHash` (nobody edited it) | update to the bundle text and refresh the hash; source stays as is |
| hash differs (someone edited it) and the bundle text changed since `lastSeededHash` | keep the edited text, set `UPSTREAM_CHANGED`, store the upstream text, and raise one alert, `help.upstream.changed` (deduplicated per run) |
| hash differs and the bundle text is unchanged | nothing |

For keys that are no longer in any bundle (or are marked `retired`):
- a row nobody edited is deleted;
- an edited row is marked RETIRED, never deleted.

The run writes a summary (inserted, updated, upstream-changed, retired) to the process result and to
the log. In the Help app, admins accept the upstream text or keep theirs. Either way the status
goes back to CURRENT and the hash is set to the new upstream text.

### 5.4 Live values instead of copied ones

Help text may reference metadata. The references are rendered when the text is read (frontend
metadata, search, assistant), so help never repeats a default or label that might later change:

| Reference | Renders |
|---|---|
| `{{label:table}}`, `{{label:table.field}}`, `{{label:process}}`, `{{label:app}}` | the current label |
| `{{default:table.field}}` | the field's default value |
| `{{maxLength:table.field}}` | the field's maximum length |
| `{{values:possibleValueSource}}` | the enum's labels, comma-separated |
| `{{setting:name}}` | a value from a registered `HelpValueProvider` (hosts and qbits register them; for example `alerts.digestHour.default`) |

An unknown reference renders as an empty string and is logged once. The coverage gate (5.5)
fails on references that cannot be resolved.

### 5.5 Coverage gate

Core ships `HelpBundleAssertions` (in `qqq-backend-core`, under `testing`), which every qbit and
the host call from one test:

```java
HelpBundleAssertions.forInstance(qInstance)
   .bundles("help/qbit-alerts.yaml")
   .owning(t -> t.getName().startsWith("alerts_"))   // the objects this bundle must cover
   .excluding("table:alerts_alert;field:id", ...)    // explicit exclusions
   .assertComplete();
```

It fails when any of these is true:
- an owned app, table, process, record action or non-trivial field has no entry. Non-trivial means every field except the primary key, `createDate`/`modifyDate` and hidden fields, unless the field is excluded.
- an entry's key does not resolve.
- a summary is longer than 120 characters, or contains markup.
- a `{{...}}` reference does not resolve.

As a result, new metadata cannot merge without help.

### 5.6 Review surface (`qbit-help`)

The Help app home shows coverage, the "Upstream changed" rows (with a side-by-side diff and
Accept upstream / Keep mine actions), retired rows and keys that no longer resolve.

### 5.7 Migration

KofTwentyTwo/Website-Backend#196's 3,394 seeded rows become:
- `help/kof22.yaml` (HOST), which keeps the kof22-specific text;
- the generic parts, moved into each qbit's `help/{qbit}.yaml`.

Quoted defaults and labels become `{{...}}` references. The SQL seed changesets are dropped
before #196 deploys; only the provenance columns migration remains.
