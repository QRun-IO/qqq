# Data Graph (schema map, record graph, system map)

Status: **Draft for owner review** · drafted 2026-10-14 · Author: James Maes (drafted with Claude)
Design only. Nothing in this document is built.

Targets:
- **QQQ core 4.1.x** (`qqq-backend-core`): a small, additive relationship catalog over existing
  metadata, and a neighbour-expansion action that reuses `QueryAction` (§3).
- **`QRun-IO/qbit-data-graph`** (Apache-2.0, new): the graph services, UI API extension, saved
  views, expansion audit and caches (§4-§8).
- **QQQ Next UI** (`QRun-IO/qqq-frontend-next`): the Data Graph screens, WebGL renderer and list
  fallback (§9).

Related work, which this design uses and does not duplicate:
- UI API extension point (`UI-API-EXTENSIONS.md` §1): every operation is a spec'd
  `/qqq/v1/qbit/data-graph/...` operation, so a CLI, TUI, iOS client and the assistant can all use it.
- Secured data (`SECURED-DATA.md`): field access tiers and masking. The graph never reads a value
  except through core actions that already apply them (§5).
- Collaboration (`COLLABORATION.md`): the record timeline (§7.2 there), tags, followers, and
  `RecordAccessCheckerInterface` for reference-style relationships (§3.2 there).
- Data analysis (`DATA-ANALYSIS.md`): the user-access fingerprint used to key caches
  (`SECURED-DATA.md` §3.5).
- Related-record picker (`RELATED-RECORD-PICKER.md`): join-path metadata is already exposed there;
  the schema graph reuses the same walk.
- Assistant (`specs/2026-10-11-qqq-assistant-qbit-design.md`) and `UiApiInvoker`
  (`UI-API-EXTENSIONS.md` §5): "show me how X connects to Y" is a client of this API with the
  user's session (§8).

## Owner request (2026-10-14, paraphrased)

The owner showed an interactive force-directed coupling map of the Galaxy systems: systems as
nodes coloured by type, typed and coloured edges (direct SQL, webhook/API, PHI paths), counts in
header cards, filter toggles (PHI paths only, hide dormant, labels), "Focus <node>", a side table
of the most-coupled nodes with in / out / PHI counts, and a narrative. The question: "what would
it look like to have a data relationship visualization qbit like this that would allow someone to
see and navigate and fly through the data in the system?"

## Mandatory standards that shape this design

| Standard | How this design follows it |
|---|---|
| Data in standard visible tables | Saved views, pins and the expansion audit are ordinary `data_graph_*` tables with record screens, secured by record locks. The graph itself is derived on request and never persisted as a hidden structure. |
| Naming | Tables lowercase snake_case, prefix `data_graph_`; permissions `data_graph.*`. |
| Processes are actions, never in nav | "Save view", "Share view", "Refresh row counts" are table or record actions with standard review/result summaries. The nav lists one entry, **Data Graph**. |
| One landing, no duplicate dashboards | One Data Graph app home; other apps link to it ("Open in Data Graph" on a table or record) and never embed a second map. |
| Readable at all sizes | A list fallback is a first-class view, not an afterthought (§9.5). Screenshots at 6 widths, light and dark, are an exit criterion of every UI phase. |
| UI only through `/qqq/v1` | Every screen and the assistant call spec'd operations of the `data-graph` extension. Payloads are typed, so a TUI can render them as trees. |
| Security is the user's | Section 5. The graph is a *view over what the user may already read*, never a side channel. |
| No filesystem | Caches are in memory per node plus a table; nothing is written to local disk. |
| Alerts through the SPI | Probing and bulk-crawl anomalies raise through `QAlerts` when present (§5.6). |

## 0. Summary

`qbit-data-graph` lets a person see how data connects, and move through it. It has two views of
one model, plus an optional third:

1. **Schema map**: tables as nodes, relationships as typed edges. Answers "what is in this system
   and how does it hang together", sized by row count, with overlays for classification,
   permissions, grouping and API exposure.
2. **Record graph**: pick any record, expand its neighbours by relationship, hop by hop. Answers
   "what is connected to this one thing", with breadcrumbs, history, focus, N-hop expansion with
   hard limits, an audit/timeline overlay, and a link into the normal record screen from every node.
3. **System map** (optional, phase e): qbits, external providers, webhooks, ESB topics and jobs,
   like the owner's image. Same renderer, different catalog.

Six rules shape the rest:

1. **The graph is a projection of existing metadata and existing reads.** No new model of
   customers' data. Schema nodes and edges come from `QInstance` metadata; record nodes and edges
   come from `QueryAction` and `GetAction`.
2. **The caller's `QSession` runs every read, with `PermissionEnforcement.ENFORCE`.** A node or
   edge the user cannot read does not exist in the response, not even as a count (§5).
3. **Server-side limits are the product, not an afterthought.** Every expansion has a hard cap
   on nodes, edges, hops and wall time, enforced in the service and never trusted from the client (§6).
4. **A graph never reveals more than the table view.** A node label that would be masked in the
   table is cleared, exactly as core already does for record labels.
5. **The renderer is replaceable.** The API returns graph data (nodes, edges, paging cursors),
   never layout. Positions are stored only in a user's saved view.
6. **Every record expansion is audited** (§5.5).

## 1. Goals, non-goals, personas

### 1.1 Goals
- Understand an unfamiliar QQQ app in minutes: the schema map answers "what tables exist, how are
  they related, which hold PHI/PII, which can I touch, which are exposed over the API".
- Trace a real record's footprint: "this order: its customer, lines, shipments, tags, followers,
  and who changed it last week".
- Find the path between two records or two tables, within the user's permissions.
- Work for any QQQ app with no per-app code, driven by metadata; apps tune it with optional
  metadata (§3.4).
- Stay usable at scale: hundreds of tables in the schema map, hundreds of thousands of related
  rows behind one record.

### 1.2 Non-goals
- Not a data-lineage or ETL graph (column-level lineage across pipelines). The system map shows
  coarse integration coupling only.
- Not an editing surface. Nodes link to the normal record screens; no drag-to-relate or
  create-from-graph in v1.
- Not a general graph database: no Cypher/Gremlin, no stored traversals, no graph analytics beyond
  degree counts and shortest path.
- No joins beyond what QQQ already supports. A relationship across backends appears as an edge and
  expands only through that backend's normal `QueryAction`.

### 1.3 Personas
| Persona | Uses |
|---|---|
| Developer / integrator | Schema map: join and FK topology, API exposure, qbit grouping. |
| Data steward / compliance | Schema map with PHI/PII overlay; "every path from this table to a PHI table"; record graph for subject-access review. |
| Support / ops | Record graph: "everything connected to this order", with the audit overlay. |
| Assistant (acting as the user) | The same API (§8). |

### 1.4 Example flows
1. A steward opens Data Graph and toggles **PHI/PII only**: the map dims everything except
   classified tables and the edges between them; the most-coupled list shows `person` with in/out counts.
2. Support opens an order and chooses **Open in Data Graph**: the record graph starts on that order
   with relationship chips (lines, customer, tags). They expand "orders" on the customer node, see
   25 of 214 and a "+189 more" chip. **Open record** jumps to the normal customer screen.
3. A developer selects two tables and chooses **Path**: the map highlights the shortest join chain
   and lists it as `order -> order_line -> product`.

## 2. Concepts and the graph model

### 2.1 Node and edge kinds

Schema view:
- **Table node**: `kind: TABLE`, id `table:{name}`. Carries label, qbit/app grouping, backend,
  approximate row count, classification, API flags, and the user's capabilities.
- **Edge kinds** (each is a relationship in existing metadata):
  - `JOIN`: a `QJoinMetaData` (one-to-many, many-to-one, one-to-one) with its join-on fields.
  - `ASSOCIATION`: an `Association` on a table (children saved with the parent).
  - `FOREIGN_KEY`: a field whose possible-value source is a table. This covers FKs with no
    declared join.
  - `RECORD_REFERENCE`: a collaboration-style reference (`tableName` + `recordId`,
    `RecordReferenceTableMetaData`), shown as an edge to each referable table.
- Direction is child to parent for display; each edge also carries `cardinality`.
- Several relationships between the same pair are returned as separate edges; the client may
  bundle them for display.

Record view:
- **Record node**: `kind: RECORD`, id `rec:{table}:{pk}`. Carries table, primary key, a label (the
  table's record label, with the secured-data fallback) and optional `badges` (classification of
  the table, tag count, followed, has comments).
- **Edge**: `{ from, to, relationship, cardinality }`.
- **Group node** (virtual): when a relationship has more visible neighbours than one page, the
  server returns the first page plus `grp:{rec}:{relationship}` carrying the *visible* total
  (§5.3). The client draws it as a "+211 more" chip. It is a paging handle, not a record.

System view (phase e): nodes `QBIT`, `EXTERNAL_PROVIDER`, `WEBHOOK`, `ESB_TOPIC`, `JOB`, `TABLE`;
edges `READS`, `WRITES`, `PUBLISHES`, `SUBSCRIBES`, `CALLS`, `SCHEDULES`, each with a `flow`
(`SQL`, `WEBHOOK_API`, `MESSAGE`, `SCHEDULE`) and a `carriesClassification` (max classification of
the tables involved), the equivalent of the image's "PHI paths" toggle.

### 2.2 Overlays (schema map)
Overlays are per-node and per-edge attributes the server computes for this user; the client chooses
what to draw. Each is an optional request flag so a CLI can ask only for what it needs.

| Overlay | Attribute | Notes |
|---|---|---|
| Classification | `classification: PUBLIC / CONFIDENTIAL / PII / PHI` on table nodes; `carriesClassification` on edges (max of the two tables) | Core `DataClassification`, with qbit-secured-data override rows honoured because the catalog reads the effective classification. |
| Permissions | `access: { read, insert, edit, delete }`, `fieldCounts: { total, visible, masked }` | Tables the user cannot read are removed entirely (§5.2); the overlay exists only for visible tables. |
| Grouping | `group: { app, qbit }` | From app and qbit metadata. |
| API exposure | `api: { uiApi, apiMiddleware: [versions], assistantVisible }` | From table API metadata and `AiExposure`. Returned only with `data_graph.viewApiExposure` (default: admins). |
| Row count | `rowCount`, `rowCountKind: EXACT / ESTIMATE / UNKNOWN` | §6.4. |
| Dormancy | `lastWriteAt`, `dormant` | The image's "hide dormant": no write within N days (default 90), from cached max `modifyDate`. |

## 3. Core (qqq-backend-core)

A small, additive surface. Most of the work lives in the qbit; core supplies only what must see
internal metadata.

### 3.1 `RelationshipCatalog` (read-only)

```java
package com.kingsrook.qqq.backend.core.actions.metadata;

public class RelationshipCatalog
{
   /** All relationships of one table, both directions, from joins, associations,
    *  possible-value FKs and record-reference tables. Pure metadata; no data access. */
   public List<RelationshipDescriptor> forTable(QInstance qInstance, String tableName);

   /** Whole-instance edge list, deduplicated and direction-normalised. */
   public List<RelationshipDescriptor> all(QInstance qInstance);
}

public record RelationshipDescriptor(
   String id,                  // stable: "join:{joinName}", "assoc:{table}.{name}", "fk:{table}.{field}"
   Kind kind,                  // JOIN, ASSOCIATION, FOREIGN_KEY, RECORD_REFERENCE
   String childTable,
   String parentTable,
   Cardinality cardinality,    // ONE_TO_MANY, MANY_TO_ONE, ONE_TO_ONE
   List<FieldPair> on,         // childField -> parentField
   String label)
```

Derived on demand and cached per QInstance metadata version. The qbit filters it per user (§5.2);
core does not know about users here. Unit-tested on the sample project and on a fixture with a join,
an association, an FK and a record-reference table.

### 3.2 `NeighbourExpansionAction`

```java
public class NeighbourExpansionAction
{
   NeighbourExpansionOutput execute(NeighbourExpansionInput input);
   // input:  tableName, primaryKey(s), relationshipId, cursor, pageSize,
   //         optional QQueryFilter, PermissionEnforcement
   // output: List<QRecord> visibleNeighbours (masked per FieldAccessHelper),
   //         Integer visibleTotal (nullable, capped), String nextCursor
}
```

It builds a `QueryAction` on the neighbour table with the relationship's join criterion ANDed with
the caller's filter and runs it with `PermissionEnforcement.ENFORCE`, so **table permission, record
security locks, field security locks and field access tiers all apply exactly as in a normal
query**. It adds no permission logic of its own. A capped `COUNT` of the same query supplies
`visibleTotal`. For `RECORD_REFERENCE` it also consults `RecordAccessCheckerInterface` and
`RecordReferenceSecurityHelper`, so a reference to an unreadable record is dropped.

### 3.3 `RelationshipPathFinder`
A bounded breadth-first search over the catalog from table A to table B returning up to `k`
shortest join chains. Record-level paths (§4.4) are built by the qbit walking those chains with
`NeighbourExpansionAction`.

### 3.4 Optional metadata

```java
new QTableMetaData()
   .withSupplementalMetaData(new DataGraphTableMetaData()
      .withLabelFields(List.of("name"))            // validated at startup: must not be restricted fields
      .withHideInGraph(false)                      // exclude noisy tables
      .withRelationshipOverrides(...)              // hide or relabel one relationship
      .withDefaultExpandRelationships(List.of("orders")))
```

`hideInGraph` removes a table from the schema map for everyone and is separate from security.
Audit, session and `data_graph_*` tables are hidden by default; audit remains reachable through a
record's timeline overlay (§4.5).

## 4. UI API extension (`/qqq/v1/qbit/data-graph/...`)

Registered with `MiddlewareSpecExtension` (`name: data-graph`, semver `1.0.0`), default prefix
`/qbit/data-graph`, specs on `AbstractJsonEndpointSpec`, schema names prefixed `DataGraph...V1`.
Every operation runs as the user's session. All are read-only: GET, or POST when a filter body is
needed. None mutates customer data.

| Operation | Method and path | Purpose |
|---|---|---|
| Config | `GET /config` | Whether the qbit is enabled for this user; limits in force; which overlays and views are allowed. Next probes this to show or hide the entry. |
| Schema graph | `GET /schema` | The user's visible schema map (§4.1). |
| Table detail | `GET /schema/table/{table}` | One table's relationships and field-level overlay (side panel). |
| Schema path | `GET /schema/path?from=&to=&max=` | Up to `k` shortest table paths. |
| Record node | `GET /record/{table}/{pk}` | One node, its badges, and the relationships available to expand with visible counts (capped). |
| Neighbours | `POST /record/{table}/{pk}/neighbours` | Expand one relationship, paged (§4.2). |
| Expand N hops | `POST /record/{table}/{pk}/expand` | Breadth-first expansion within limits (§4.3). |
| Record path | `POST /path` | Path between two records (§4.4). |
| Timeline | `GET /record/{table}/{pk}/timeline`, `POST /timeline/batch` | Audit and activity overlay (§4.5). |
| System graph | `GET /system` | Phase e. |

Saved views are standard table records (§7), read and written through the normal table API, so they
need no custom endpoints.

### 4.1 Schema graph

`GET /schema?overlays=classification,permissions,group,api,rowCount&include=app:sales&hideDormant=false`

```json
{
  "version": "1.0.0",
  "generatedAt": "2026-10-14T15:00:00Z",
  "fingerprint": "sha256:...",
  "nodes": [
    { "id": "table:order", "kind": "TABLE", "label": "Order",
      "group": { "app": "sales", "qbit": null },
      "classification": "PII",
      "rowCount": 1480220, "rowCountKind": "ESTIMATE",
      "access": { "read": true, "insert": true, "edit": true, "delete": false },
      "fieldCounts": { "total": 24, "visible": 22, "masked": 2 },
      "degree": { "in": 3, "out": 4 } }
  ],
  "edges": [
    { "id": "join:orderLine", "kind": "JOIN", "from": "table:order_line", "to": "table:order",
      "cardinality": "MANY_TO_ONE", "label": "Order lines",
      "on": [{ "child": "orderId", "parent": "id" }],
      "carriesClassification": "PII" }
  ],
  "summary": { "tables": 143, "relationships": 311, "phiTables": 12, "piiTables": 31 }
}
```

The response never states how many tables were removed for this user (§5.2). `degree` counts
visible neighbours only. `summary` feeds the header cards. `fingerprint` is the user's access
fingerprint plus the metadata version (the cache key, §6.5). Ordering is stable (by id). Above
`maxSchemaNodes` the nodes are returned grouped by app, each group expandable with `?include=app:...`.

### 4.2 Neighbours (one relationship, paged)

`POST /record/{table}/{pk}/neighbours`
```json
{ "relationshipId": "join:orderLine", "direction": "OUT", "pageSize": 25, "cursor": null, "filter": null }
```
```json
{ "from": "rec:order:1001",
  "nodes": [ { "id": "rec:order_line:77", "kind": "RECORD", "table": "order_line", "pk": 77,
               "label": "Widget x2", "badges": { "classification": "PII" } } ],
  "edges": [ { "from": "rec:order:1001", "to": "rec:order_line:77",
               "relationship": "join:orderLine", "cardinality": "ONE_TO_MANY" } ],
  "page": { "returned": 25, "visibleTotal": 40, "totalIsCapped": false, "nextCursor": "..." },
  "limits": { "nodesRemaining": 475, "truncated": false } }
```

- `visibleTotal` counts records **this user can read**, never a raw count (§5.3).
- Paging is a keyset cursor `(sortField, pk)`, encoded and HMAC-signed with a per-session key so it
  cannot be forged or replayed by another user. Page size is clamped to `maxPageSize`.
- An optional `filter` is a standard `QQueryFilter` and passes through core criteria enforcement
  (`FieldAccessHelper.assertCriteriaAllowed`), so a filter on a MASKED or HIDDEN field returns 403
  as in the table API.

### 4.3 Expand N hops

`POST /record/{table}/{pk}/expand` with `{ "hops": 2, "relationships": ["*"], "perRelationshipLimit": 10, "maxNodes": 200 }`.

Breadth-first, level by level; each level is one `IN (pk list)` query per relationship through
`NeighbourExpansionAction`, not one query per node. It stops at the first of: `hops`, `maxNodes`,
`maxEdges`, `timeBudgetMs`, or an empty frontier. The response says why it stopped
(`"stoppedBy": "MAX_NODES"`) and lists each frontier node's remaining `expandable` relationships
with visible counts, so the client can offer "continue from here". Cycles are deduplicated by node
id. Requests above server caps are **clamped, not rejected**, and `limits` echoes the effective values.

**Hub guard.** A relationship whose visible total exceeds `hubThreshold` is never auto-expanded in
an N-hop call. It returns only the group node (a "+N" chip) with a bucketed count; the user expands
it explicitly, one page at a time. This stops a hop from fanning out through a `status` or `country`
row that thousands of records point at.

### 4.4 Path between two records

`POST /path` with `{ "from": {"table":"claim","pk":9}, "to": {"table":"provider","pk":4}, "maxHops": 4, "maxPaths": 3 }`.

1. Compute schema-level candidate chains (§3.3), shortest first.
2. Resolve them at record level with a **bidirectional bounded search**: expand from both ends one
   hop at a time, only through relationships on candidate chains, under the §4.3 limits, until
   `maxPaths` frontiers meet.
3. Return each path as ordered nodes and edges, plus their union as a mini-graph.

Result states: `FOUND`, `NONE_WITHIN_HOPS`, `LIMIT_REACHED` (a cap stopped the search; never
reported as "no path"). **`NONE_WITHIN_HOPS` means "no connection through data you can access"**,
and the UI says exactly that, because a path may exist through records the user cannot read (§5.4).

### 4.5 Timeline and audit overlay

`GET /record/{table}/{pk}/timeline?from=&to=&limit=&kinds=` returns the collaboration timeline
(core audit merged with activity, `COLLABORATION.md` §7.2) for one node, using the user's normal
audit-read permission. Values of fields the user cannot see are masked as on the audit screen. If
the user lacks audit permission on the table the endpoint returns `available: false` and no events.

`POST /timeline/batch` takes up to `maxNodes` node ids and a time window and returns
`{ nodeId: [{at, kind, actorLabel}] }`, capped at `maxEventsPerNode`. The client uses it to pulse
nodes changed in the window and to replay a window ("what changed around this order in the last 24
hours").

### 4.6 Errors
`400` bad input (unknown relationship id, hops < 1); `401`; `403` (no `data_graph.*` permission, or
a filter on a restricted field); `404` (table or record not visible to you, **identical to "does not
exist"**); `409` (stale cursor, or an unindexed relationship refused, §6.6); `429` (rate limit,
§5.6). A time budget overrun returns the partial result with `stoppedBy: TIME` rather than an error.

### 4.7 Compatibility
Additive and versioned as an extension (`UI-API-EXTENSIONS.md` §3). The fragment is committed in
the qbit at `src/main/resources/openapi/qqq-v1/data-graph.yaml`, checked with
`UiApiContractAssertions.assertExtensionMatchesCommitted`, and vendored by Next UI. Entry added to
`CONTRACTS.md` when core phase a lands.

## 5. Security

A relationship graph is a **new way to infer data**, so this is the central design problem. The rule
is absolute: **the graph reveals nothing the same user could not already read through the standard
table API, and a missing node is never distinguishable from one that does not exist.**

### 5.1 Principle
Every node, edge, count and label comes from a core action run in the caller's session with
`PermissionEnforcement.ENFORCE`. The service has no system session and no elevated path (the
assistant's stance). No code path builds a response from unenforced data and redacts it afterwards;
post-hoc redaction is a bug class avoided by construction.

### 5.2 Schema level
- A table the user cannot read is **removed** from `nodes`, along with every edge touching it. It is
  not greyed, counted or mentioned. `degree` counts visible neighbours only, and the response has no
  "hidden" count.
- If two visible tables are linked only through a hidden table, they show **no direct edge**. We
  never synthesise a shortcut that hints a hidden table exists.
- `fieldCounts.masked` is the user's own masked field count on a visible table and names none.
- A table's `classification` is the maximum over fields the user can see (tier not HIDDEN), so a
  hidden PHI field does not label the table PHI to someone who cannot see that field. A steward with
  full access sees the true maximum.
- The API exposure overlay needs `data_graph.viewApiExposure`.

### 5.3 Record level
- Neighbour queries use `QueryAction` with `ENFORCE`, so **record security locks filter neighbours in
  SQL**. Rows the user cannot see are never fetched, labelled or counted.
- `visibleTotal` counts the same enforced query. A raw table statistic is never used as a
  per-record count.
- Node labels follow the secured-data rule: a label built from a non-FULL field is cleared and the
  client shows the primary key.
- Badges (tag count, followers, comments) come from collaboration's per-record summary with its own
  feature permissions, so a user without tag access does not see "3 tags".
- A node card never shows field values: only label, table, PK, badges and an "Open record" link.
  Values appear only on the normal record screen, where masking, Reveal and break-glass already
  work. (A short field preview through `GetAction` with the same masking is a later, config-gated
  option.)

### 5.4 Edges, PHI and inference
- An edge exists only if **both endpoints are visible** and the relationship itself is permitted
  (join through readable tables; references through `RecordAccessCheckerInterface`).
- A `PHI`-carrying edge is shown only to a user who can read the key fields of both tables.
  REVEALABLE or MASKED users still see that a record link exists (the table API exposes the key
  too), but never PHI values.
- **No existence oracle.** Hidden and missing records return the same `404`. `NONE_WITHIN_HOPS` is
  worded "no connection through data you can access". Counts are always visible counts.
- **Exact-match probing.** A `/neighbours` filter on a REVEALABLE field follows EXACT_MATCH_ONLY and
  the enumeration guardrails (`SECURED-DATA.md` §7.6) because it goes through the same
  `assertCriteriaAllowed` path.
- **Hub inference.** Counts above `hubThreshold` are bucketed ("1,000+"), so the graph cannot be used
  to count rows precisely on large locked tables.
- Break-glass and Reveal are unaffected: the graph neither grants nor consumes them. During
  break-glass the graph shows what the table API returns, under the banner Next already shows.

### 5.5 Audit of every expansion
Each `neighbours`, `expand`, `path` and `timeline` call writes one row to `data_graph_access_log`
(standard table; CONFIDENTIAL; walled off from the assistant channel, like secured-data's log,
because it describes who looked at what):

`id, at, userId, channel, operation, startTable, startPk, relationshipId, hops, nodesReturned,
stoppedBy, classificationMax, phiTouched, durationMs, outcome`

- It records what was asked and a **summary** of what came back, never values or node lists.
- `classificationMax` and `phiTouched` make "who explored PHI paths this month" one filter. With
  qbit-secured-data installed, an expansion returning a node of a restricted PHI table also writes
  its `secured_data_access_log` row, so there is one trail per restricted-data touch.
- The schema map is logged coarsely (one row per session open, not per redraw).
- Retention and wipe follow secured-data (§13 there).
- If the log write fails, an expansion that touches classified tables **fails closed**. For
  unclassified results `auditFailureMode` may be `FAIL_OPEN_AND_ALERT`.

### 5.6 Abuse and anomaly controls
- Per-user rate limit (default 30 expansions/min, 600/hour) and a daily node cap on classified
  results (`maxNodesPerDay`, default 50,000).
- `QAlerts` anomalies, when Alerts is present: unusual PHI node volume, many `NONE_WITHIN_HOPS`
  path searches (path probing), and sequential `neighbours` across thousands of start records (bulk
  crawl). They go to the same destinations as secured-data anomalies.
- `data_graph.expand` and `data_graph.path` are separate permissions from `data_graph.use`, so an
  org can grant "see the schema" without "walk the records".

### 5.7 Assistant and other channels
The `ASSISTANT` channel runs through `UiApiInvoker` with the user's credentials (§8). The
client-channel deny wall (`UI-API-CLIENT-CHANNELS.md`) can deny `expand` for the assistant, or
restrict it to unclassified tables, independently of the user's own access.

### 5.8 Permissions
| Permission | Grants |
|---|---|
| `data_graph.use` | The Data Graph entry and the schema map (visible tables only). |
| `data_graph.expand` | Record graph: neighbours, expand, timeline overlay. |
| `data_graph.path` | Path search between records. |
| `data_graph.viewApiExposure` | The API exposure overlay. |
| `data_graph.systemMap` | System map (phase e). Admin default. |
| `data_graph.admin` | Limits, hidden-table list, access log. |

All are in addition to, never instead of, the table permissions the data itself needs.

## 6. Limits, performance, caching

### 6.1 The graph-explosion problem
The graph around a hub (a `country` row, a `status` row, the admin user) touches most of the
database. Every choice below keeps the first expansion cheap and keeps the user in control.

### 6.2 Principles
1. **Never auto-expand hubs** (§4.3).
2. **Ask for pages, not graphs.** The record view loads the focus node with relationships as
   collapsed chips showing visible counts; the user expands what they care about.
3. **Batch by relationship, not by record:** one `IN (pk list)` query per relationship per level on
   an indexed join key.
4. **Cap everything on the server** and return partial results with an explicit `stoppedBy`.
5. **Cheap counts only when cheap:** `visibleTotal` is a capped count (`LIMIT cap+1`); past the cap
   the response says `totalIsCapped: true` ("1,000+").

### 6.3 Limits
| Limit | Default | Ceiling |
|---|---|---|
| Hops per expansion | 3 | 5 |
| Nodes per response | 500 | 2,000 |
| Edges per response | 1,500 | 6,000 |
| Neighbours per page | 25 | 100 |
| Count cap | 1,000 | 10,000 |
| Time budget | 4 s | 15 s |
| Schema nodes before grouping | 1,500 | 5,000 |
| Hub threshold | 1,000 | 100,000 |

Admin-tunable in the standard table `data_graph_config`, validated against the ceilings. Requests
are clamped and the response's `limits` object states the values in force.

### 6.4 Row counts for node sizing
Node size in the schema map follows table size. We never run `COUNT(*)` on a huge table on page load:
1. `rowCountKind: ESTIMATE` from backend statistics (Postgres `reltuples`, MySQL
   `information_schema`) through an optional `RowCountEstimateInterface` on the backend module.
2. `EXACT` only under a small threshold, or from a cached exact count.
3. `UNKNOWN` for backends without statistics (drawn at a default size, with a legend entry).
4. **Tables with record security locks never use an unenforced estimate** (it would leak locked-row
   volume). They are sized from a bucketed enforced count when cheap, else `UNKNOWN`.
5. A background job refreshes unenforced counts hourly into `data_graph_table_stats`
   (`tableName, rowCount, kind, computedAt`), applied under rule 4.

### 6.5 Caching
- **Catalog:** per QInstance metadata version; invalidated on a metadata or
  `RuntimeMetaDataProvider` change.
- **Per-user schema response:** in memory, keyed by `(metadataVersion, accessFingerprint, overlays,
  include)`, where the fingerprint is the same user-access fingerprint as Data analysis and Secured
  data (`SECURED-DATA.md` §3.5); any permission, lock or tier change changes the key. TTL 5 minutes;
  `Cache-Control: private, max-age=60` and an `ETag` from the fingerprint, so a reload is a 304.
- **Record neighbours:** never cached across users. A 30 s per-session memo keyed by
  `(sessionUUID, table, pk, relationshipId, cursor, filterHash)` absorbs refetches on focus change.
  Record data is volatile and access-sensitive; correctness beats hit rate.
- **Row-count stats:** shared across users, applied under §6.4 rule 4.
- No cache holds field values: only ids, labels and counts already filtered for that access
  fingerprint. In memory per node plus the stats table; no local disk.

### 6.6 Large tables
- Every neighbour query must use an indexed join key. Where the backend can report indexes, the
  catalog marks each relationship `indexed: true/false/unknown`; the service **refuses** to expand an
  unindexed relationship on a table over `unindexedExpandMaxRows` (default 100,000) with an explicit
  "relationship not indexed" `409` instead of scanning. The admin config lists these relationships
  and the fix.
- Keyset pagination only, no `OFFSET`. Queries carry a statement timeout equal to the remaining
  time budget.
- Deep, wide questions ("every record within 3 hops of this person, as a CSV") are a durable job
  (#1171) with the same security as any export. They are not part of the interactive API.

## 7. Qbit tables

Standard visible tables, `data_graph_` prefix.

| Table | Purpose | Security |
|---|---|---|
| `data_graph_view` | Saved view: `name, kind (SCHEMA/RECORD/SYSTEM), startTable, startPk, relationships, overlays, filtersJson, layoutJson (positions, zoom), shared` | Owner lock on `userId`; sharing through collaboration. A record view stores the **start and expansion instructions, not the result**, and replays with the opener's permissions. |
| `data_graph_pin` | Pinned nodes and bookmarked paths per user | Owner lock |
| `data_graph_config` | Limits and hidden-table overrides | `data_graph.admin` |
| `data_graph_table_stats` | Row-count cache (§6.4) | `data_graph.admin` read |
| `data_graph_access_log` | Expansion audit (§5.5) | CONFIDENTIAL; `data_graph.admin` read; walled off from the assistant |

A saved view never stores node labels or values. Opening a shared view re-runs it as the opener;
where they lack access, nodes are simply absent, with a neutral "some items are not available to
you", never a placeholder naming them.

## 8. The assistant: "show me how X connects to Y"

The assistant reaches this feature only through `UiApiInvoker` with the user's credentials
(`UI-API-EXTENSIONS.md` §5) and `invokedBy: assistant`, so it has exactly the user's view and is
subject to the channel deny wall (§5.7). Its tool list is generated from the OpenAPI operations; no
hand-written tool is needed.

Flow for "how does claim 9 connect to provider 4?":
1. Resolve X and Y: the existing user-scoped `POST /search` when given names rather than ids.
2. `POST /qbit/data-graph/path` with both records. On `FOUND` the assistant narrates the chain from
   node and relationship labels ("claim 9, its patient, that patient's encounter, the encounter's
   provider Dr. Smith") and returns an **"Open in Data Graph" card** carrying a view descriptor
   (start, path, hops), not data, so the user lands in the full visual under their own session.
3. On `NONE_WITHIN_HOPS` it says "I could not find a connection within 4 steps through data you can
   access", the same wording as the UI, and does not speculate.
4. Table-level questions ("how does `order` relate to `product`") use `/schema/path`.

Rules: the assistant gets masked values and absent nodes like any client. Under the assistant's PHI
data-minimisation route (assistant spec §3.6) classified labels are not sent to the model, and it
narrates from relationship names and counts. Every assistant-driven expansion is logged with channel
`ASSISTANT` (§5.5). The assistant cannot raise limits.

## 9. Next UI

### 9.1 Placement
One nav entry, **Data Graph** (`data_graph.use`). "Open in Data Graph" appears in a table's action
menu (schema map focused on that table) and a record's action menu (record graph on that record,
with `data_graph.expand`). No dashboards are duplicated elsewhere.

### 9.2 Schema map screen
```
 +---------------------------------------------------------------------------+
 | Data Graph    [Schema | Record | System]      search...        [view v]    |
 +-----------+---------------------------------------------+-----------------+
 | Tables 143| overlay: (o) None ( ) Classification        |  Most coupled   |
 | Rels   311|          ( ) Permissions ( ) Group ( ) API  |  table  in out  |
 | PHI     12|  [PHI/PII only] [Hide dormant] [Labels]     |  person  9  4   |
 | PII     31|                                             |  order   3  4   |
 |           |        force-directed canvas                |  ...            |
 +-----------+---------------------------------------------+-----------------+
 | Selected: order . PII . 1.4M rows (est.) . sales app . read/edit            |
 | [Focus order]  [Open table]  [Path to...]  [Expand in Record graph]         |
 +---------------------------------------------------------------------------+
```
This mirrors the owner's image: header cards, filter toggles, **Focus <node>** (centre, fade
non-neighbours, show direct neighbours), a most-coupled table (sortable by in, out, classified
edges; click to focus). The narrative is composed client-side from response fields ("12 of 143
tables hold PHI; `person` is the most connected: 9 inbound, 4 outbound"), never free text from the
server, so it is accurate and translatable.

Style: colour by overlay from theme tokens; size by `log(rowCount)` with a legend for estimate and
unknown; a ring marks tables the user can edit; dashed edges for FOREIGN_KEY, solid for JOIN, thick
for ASSOCIATION; PHI edges use the PHI colour and draw on top.

### 9.3 Record graph screen
- Start from "Open in Data Graph", from a record search (`POST /search`), or a pasted record link.
- The focus node is centred; relationships appear as **chips around it** with visible counts
  ("orders 214", "tags 3"). A chip expands one page into the canvas, with a "+189 more" group node
  for the next page.
- **Breadcrumbs** record the focus nodes visited; click to return. **History** (back, forward, last
  20) is client state; "save view" stores it in `data_graph_view`.
- **Focus mode** hides everything beyond 1 or 2 hops of the selected node. "Expand 2 hops" calls
  `/expand`; stops show as "Stopped at 500 nodes. Continue from here."
- **Timeline overlay**: a time slider under the canvas. Nodes changed in the window pulse; hover
  shows actor, kind and summary; "Replay" steps the window forward (`/timeline/batch`).
- **Follow a link**: every node has "Open record" (normal record screen; new tab with the usual
  modifier) and "Open table filtered to related".
- Keyboard: arrows move between neighbours, Enter expands, Space selects, `F` focus, `B` back, `/`
  search, `Esc` clears focus.

### 9.4 Renderer and "fly through"
Recommendation (owner to confirm):

| Choice | Role |
|---|---|
| **sigma.js v3 (WebGL) + graphology** | Default 2D renderer for both views. WebGL scales to tens of thousands of nodes; ForceAtlas2 layout runs in a web worker; camera API for pan, zoom and animated focus. |
| **deck.gl** | Optional 3D "fly through", lazy-loaded as its own chunk, off by default, fed the same data. Depth encodes hop distance from focus (record graph) or app grouping (schema map). |
| cytoscape.js | Considered. Richer layouts and algorithms, but the canvas renderer scales worse and its WebGL renderer is newer. Kept as an alternative for the schema map only if hierarchical layouts are needed. |
| react-force-graph 3D | Rejected as primary: ties us to three.js and is harder to make accessible. |

With the §6 limits both views stay under roughly 2,000 visible nodes, so sigma.js has ample
headroom; we do not pick a renderer for scale the server does not allow. 3D is a different look at
the same payload: never required and never the only way to reach any information.

The renderer sits behind a `GraphRenderer` interface in Next so sigma and deck.gl share one adapter.
Route-level dynamic imports keep the base bundle unchanged; the bundle budget is report-only
(`enforce: false`) but the delta is measured and reported in the PR.

Layout: ForceAtlas2 default; initial positions seeded deterministically from ids so reloads look the
same; dragged positions are saved in the view's `layoutJson`; a "re-layout" button. `prefers-reduced-motion`
uses a precomputed static layout and no camera animation.

### 9.5 Mobile and the list fallback
- Below 900 px, when WebGL is unavailable, or by user choice, the canvas is replaced by an
  **outline** of the same payload: the focus node, then relationships as accordion sections with
  counts; each neighbour a row (label, table, chevron to re-focus); breadcrumbs stay. The schema map
  becomes a table of tables (name, group, classification, in, out, size) with the same filters and a
  per-table relationships drawer. Same API data, same actions.
- The most-coupled table is the primary content on small widths.
- Touch: pinch and pan where the canvas is shown; tap selects; no hover-only information anywhere.

### 9.6 Accessibility
- The canvas is never the only access path. "View as list" is always one action away and is a real
  semantic tree (`role="tree"`, `treeitem`, `aria-expanded`, `aria-level`).
- Colour never stands alone: classification uses a shape badge and text; edge types use stroke
  patterns. Contrast meets WCAG 2.2 AA in both themes; forced-colors is honoured.
- A focus ring marks the selected node; a live region announces results ("25 of 214 orders added;
  189 more available") and limit stops.
- Full keyboard operation (§9.3) with a shortcuts help, and text alternatives for the narrative.

### 9.7 Light and dark
Colours come from theme tokens (the existing classification and app palettes). Canvas background,
edge opacity and label halo are tokenised so the WebGL renderer re-skins on theme change without
refetching. Screenshots at 6 widths in both themes are part of each UI phase's exit.

## 10. Phases

| Phase | Scope | Exit criteria |
|---|---|---|
| **a. Core** | `RelationshipCatalog`, `NeighbourExpansionAction`, `RelationshipPathFinder`, `DataGraphTableMetaData`, `RowCountEstimateInterface` (rdbms), `CONTRACTS.md` row | Core tests (§11 rows 20, 21, 27, 29) |
| **b. Schema map** | qbit scaffold; `/config`, `/schema`, `/schema/table`, `/schema/path`; config and stats tables; per-user cache; schema screen and list fallback; overlays (classification, permissions, group, row count) | Rows 1-3, 19, 22-24, 33-36 |
| **c. Record graph** | `/record`, `/neighbours`, `/expand`, `/path`; audit log; rate limits; record graph screen, breadcrumbs, focus, saved views | Rows 4-18, 25-30 |
| **d. Overlays and assistant** | Timeline overlay, API exposure overlay, pins, shared views, assistant card and tools, anomalies through Alerts | Rows 31-32, plus 15 and 18 |
| **e. 3D and system map** | deck.gl fly-through (lazy); system map catalog (qbits, providers, webhooks, ESB topics, jobs) | Separate go/no-go; 3D must pass the same accessibility and security rows |

Each phase is independently releasable behind `data_graph.use`.

## 11. Acceptance matrix

Security (integration tests with several users and fixtures carrying locks and tiers):

| # | Scenario | Expected |
|---|---|---|
| 1 | User without read on table H; `GET /schema` | H absent; no edge touching H; no hidden count anywhere |
| 2 | A -> H -> B where H is hidden | A and B shown, no A-B edge |
| 3 | The table's only PHI field is HIDDEN for the user | Table not marked PHI for them; marked PHI for an admin |
| 4 | Two users with different record locks on one record | Each sees only their own neighbours; `visibleTotal` differs |
| 5 | Record the user cannot read | `404` identical to a nonexistent record |
| 6 | Label built from a MASKED field | Label cleared, PK shown |
| 7 | Neighbours filter on a MASKED field | `403` |
| 8 | REVEALABLE field exact-match filter | Allowed for EQUALS/IN only; secured-data log row written |
| 9 | No `data_graph.expand`, call `/neighbours` | `403`; schema map still works |
| 10 | Record-reference edge to an unreadable record | Edge dropped |
| 11 | `expand` through a hub over the threshold | Not auto-expanded; group node with bucketed count |
| 12 | Path exists only through unreadable data | Same response and wording as no path |
| 13 | Any neighbours, expand, path or timeline call | One `data_graph_access_log` row, no node values |
| 14 | Audit write fails on a PHI expansion | Expansion fails closed |
| 15 | Assistant channel denied `expand` by the channel wall | `403` for the assistant; the user's UI unaffected |
| 16 | User A's cursor used by user B | Rejected (signature) |
| 17 | Break-glass active | Output matches the table API under break-glass |
| 18 | Rate limit exceeded | `429`, and an alert when Alerts is present |
| 19 | User gains a permission | Next schema call reflects it (fingerprint changed), not stale |

Correctness and limits:

| # | Scenario | Expected |
|---|---|---|
| 20 | Catalog on the sample project | Every join, association and table FK appears once with correct cardinality |
| 21 | Two joins between the same tables | Two edges |
| 22 | Schema over `maxSchemaNodes` | Grouped by app, expandable |
| 23 | Row count on a locked table | Never an unenforced estimate |
| 24 | Unindexed relationship over the row threshold | Refused with a reason; no scan |
| 25 | `expand` with `hops=9` | Clamped to the ceiling; `limits` echoes effective values |
| 26 | `expand` reaching `maxNodes` | Partial result, `stoppedBy: MAX_NODES`, continuation handles |
| 27 | Cycle in the data | Each node once; all edges present |
| 28 | Time budget exceeded | Partial result, `stoppedBy: TIME` |
| 29 | Keyset paging over 10,000 neighbours | No duplicates or gaps across pages |
| 30 | N-hop expansion on 1M-row indexed tables | One query per relationship per level; p95 within the time budget |

Assistant, UI and accessibility:

| # | Scenario | Expected |
|---|---|---|
| 31 | "How does X connect to Y" | Uses `/path` through `UiApiInvoker`; same result as the UI; logged `ASSISTANT` |
| 32 | PHI-restricted route | No classified labels sent to the model |
| 33 | List fallback | Same nodes, counts and actions as the canvas; full keyboard use |
| 34 | WebGL unavailable | Automatic list fallback, no error |
| 35 | Axe and keyboard audit, both themes | No serious violations; AA contrast; reduced motion honoured |
| 36 | Screenshots at 6 widths, both themes | No horizontal scroll; no cramped panels |

Contract: the `data-graph` OpenAPI fragment is committed and compatible with its baseline
(`UI-API-EXTENSIONS.md` §4).

## 12. Risks

| Risk | Impact | Mitigation |
|---|---|---|
| **Graph explosion** through hubs (status, country, admin user) | Slow responses, unreadable canvas, database load | Hub guard (§4.3), pages not graphs, hard caps and ceilings, partial results with explicit continuation |
| **Performance on large tables** | Scans, timeouts | Indexed-join requirement and refusal, keyset paging, one `IN` query per relationship per level, statement timeouts, estimated counts, stats table (§6) |
| **Inference leaks** (counts, existence, shortcut edges, labels) | A new route to data the user cannot read | Enforced reads only; no post-hoc redaction; no hidden counts; identical 404; no synthesised edges; bucketed hub counts; signed cursors (§5) |
| **Audit volume** | Large log table | Rows are summaries; coarse schema logging; retention reuse; indexes on user, time and PHI flag |
| **Cache staleness against security** | Stale view after a permission change | Fingerprint in the cache key; record data never shared across users |
| **Label leakage** through record-label format | Masked values in labels | Reuse core label clearing; `labelFields` validated at startup |
| **WebGL support, low-end devices** | Blank canvas | Automatic list fallback; node ceiling; reduced motion |
| **Renderer lock-in** | Rework | API returns data, not layout; `GraphRenderer` interface; one adapter for sigma and deck.gl |
| **3D accessibility** | Exclusion | 3D optional and never the only path; same list fallback and tests |
| **Metadata gaps** (FKs without joins, polymorphic references) | Missing or wrong edges | Catalog covers joins, associations, possible-value FKs and record-reference tables; per-table overrides; an admin "relationships not in graph" report |
| **Scope creep** (editing, lineage, graph analytics) | Delay | Non-goals (§1.2); revisit after phase d |
| **Cross-backend traversal** | N+1 across backends | Batch per backend; schema map marks cross-backend edges; refuse cross-backend expansion above a small row bound |
| **Bundle size** | Slower load | Dynamic imports per route; deck.gl lazy; budget delta reported (report-only budget) |

## 13. Open questions (recommendation in bold)

1. Renderer: **sigma.js default with lazy deck.gl for 3D**, or cytoscape.js for the schema map.
2. Shared saved views replay with the opener's permissions (**yes**); snapshots would store data
   outside the lock model.
3. `data_graph.expand` granted to every role with table read (**no: explicit grant; schema map only
   by default**).
4. Audit fail-closed only for classified results (**yes**) or always.
5. System map sources: qbit, webhook, ESB topic and job metadata are in the QInstance; external
   providers need a small declarative registry (`DataGraphSystemMetaData`). Confirm that belongs in
   QQQ, or whether the Galaxy-wide infrastructure map (the owner's image) belongs in Praetor and
   this qbit covers only the in-instance system map.
6. Node field preview in cards (**off by default**, later, through `GetAction` with masking).
