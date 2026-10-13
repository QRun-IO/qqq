# Related-Record Picker

Status: spec, 2026-10-13. Phases (a) to (d) are below.

A metadata-driven modal for choosing **related records** of any table, generalising the
media/record picker of Next UI RC.16 (`RecordPickerDialog`, QQQ `QPossibleValuePicker`,
`docs/misc/RecordPicker.adoc`). It covers three relationships:

| Mode | Relationship | Example | Result of confirming |
|---|---|---|---|
| `SINGLE` | a foreign-key field on this table | `sales_order.customerId` → `customer` | the field's value is set (in a create or edit form) |
| `MULTI_CHILD` | an association to a child table whose rows point at the target | `sales_order` → `sales_order_line.skuId` → `sku` | one new child row per picked target, after a review step |
| `LINK` | an association to a join table (many-to-many) | `sales_order` → `sales_order_tag.tagId` → `tag` | join rows are added (link) or deleted (unlink) |

Each picker offers the target table's own filter builder, saved views and columns, a search box,
sorting, a preview, and (when the user may insert into the target) quick-create of a new target
record. Every operation is a v1 endpoint of the QQQ UI API (`/qqq/v1`) with typed schemas in the
generated OpenAPI, so a CLI, TUI or iOS client can implement the same picker.

## Owner request (2026-10-13)

> we also need a more generic 'related item' picker that works the same - but works for any type of
> other record related. ie, if I am on an order table and I want to pick and add the skus to it etc.
> or search and find a customer record, or create a customer record on the fly for it. it should be a
> modal popup like that that allows you to do a qqq filter view of the sub tables, etc or optionally
> create a new record. it should all be configured and managed by the meta data like everything else.
> and it needs to work with all of the same security rules etc etc.

## Owner decisions (2026-10-13)

1. **Adding children** (e.g. SKUs onto an order): multi-select target records in the modal, then a
   short review step shows one new child row per pick (e.g. an order line). The user fills the child's
   own fields, such as quantity and price, before saving. The fields come from the child table's
   metadata.
2. **Quick-create:** a table can declare a quick-create section (the fields to show). Without one, the
   required fields are shown. A link opens the full create screen. Same validation, permissions,
   record locks and audit as a normal create.
3. **It is all part of the QQQ UI/UX API (`/qqq/v1`)**, not the model API. Every operation is a spec'd
   v1 endpoint, or an existing one, documented in the generated OpenAPI with typed schemas. Hints are
   semantic, so a CLI, TUI or iOS client can implement the same picker.

Owner decisions (2026-10-12): all recommendations accepted. Where this spec recommends, that is the
decision; adjust once it can be seen working.

## 1. Metadata model

### 1.1 `QRelatedRecordPicker` (on the table that owns the relationship)

Declared on `QTableMetaData` with `withRelatedRecordPicker(...)`, keyed by `name`, managed like any
other table metadata (producers, enrichment, validation, frontend publication).

```java
new QTableMetaData().withName("sales_order")
   // ...fields, sections, associations...

   // SINGLE: pick (or quick-create) the order's customer
   .withRelatedRecordPicker(new QRelatedRecordPicker()
      .withName("customer")
      .withMode(RelatedRecordPickerMode.SINGLE)
      .withFieldName("customerId")                         // FK on this table; the target is its PVS table
      .withFilter(new QQueryFilter(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true)))
      .withColumns(List.of("name", "email", "region"))
      .withPreviewFields(List.of("name", "email", "phone", "region")))

   // MULTI_CHILD: add SKUs as order lines
   .withRelatedRecordPicker(new QRelatedRecordPicker()
      .withName("addSkus")
      .withLabel("Add SKUs")
      .withMode(RelatedRecordPickerMode.MULTI_CHILD)
      .withAssociationName("orderLines")                   // sales_order -> sales_order_line
      .withChildTargetFieldName("skuId")                   // sales_order_line.skuId -> sku
      .withFilter(new QQueryFilter(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true)))
      .withDefaultFilter(new QQueryFilter(new QFilterCriteria("region", QCriteriaOperator.EQUALS, "${input.region}")))
      .withColumns(List.of("code", "name", "price", "region"))
      .withPreviewFields(List.of("code", "name", "description", "price"))
      .withChildFieldDefaults(Map.of("quantity", 1))
      .withChildValuesFromTarget(Map.of("unitPrice", "price"))   // child field <- target field
      .withReviewFieldNames(List.of("quantity", "unitPrice")))

   // LINK: tag the order (many-to-many through sales_order_tag)
   .withRelatedRecordPicker(new QRelatedRecordPicker()
      .withName("tags")
      .withMode(RelatedRecordPickerMode.LINK)
      .withAssociationName("orderTags")                    // sales_order -> sales_order_tag
      .withChildTargetFieldName("tagId"));                 // sales_order_tag.tagId -> tag
```

| Attribute | Modes | Meaning | Default |
|---|---|---|---|
| `name` | all | unique per table; the path segment of the endpoints | required |
| `label` | all | dialog title and the child-list button | `Choose {field label}` / `Add {target label}` / `Link {target label}` |
| `mode` | all | `SINGLE`, `MULTI_CHILD` or `LINK` | required |
| `fieldName` | SINGLE | the FK field on this table | required for SINGLE |
| `associationName` | MULTI_CHILD, LINK | an `Association` of this table (to the child or join table) | required |
| `childTargetFieldName` | MULTI_CHILD, LINK | the child/join table's field that holds the target's key | required |
| `targetTableName` | all | the target table | the table of the FK field's possible value source |
| `filter` | all | always applied (AND) to every picker query; `${input.x}` reads the parent's values | none |
| `defaultFilter` | all | the initial, user-editable filter in the dialog; `${input.x}` is resolved server-side before it is published to the dialog | none |
| `columns` | all | result columns (target fields) | the target's record-label fields, then T1 section fields, up to 5 |
| `previewFields` | all | preview pane fields | the target's T1 section fields |
| `thumbnailField` | all | an image URL field for a grid/thumbnail view | none (list view) |
| `allowQuickCreate` | SINGLE, MULTI_CHILD, LINK | offer quick-create of a target record | `true` (still needs INSERT on the target) |
| `childFieldDefaults` | MULTI_CHILD, LINK | values for each new child row | none |
| `childValuesFromTarget` | MULTI_CHILD, LINK | child field ← target field, copied per pick (e.g. price) | none |
| `reviewFieldNames` | MULTI_CHILD | the child fields shown and editable in the review step | the child's required, editable fields other than the parent join fields and `childTargetFieldName`; an empty list skips the review |
| `maxSelections` | MULTI_CHILD, LINK | cap per confirm | 200 |

`${input.x}` uses the same interpreter as a field's `possibleValueSourceFilter`
(`QQueryFilter.interpretValues`). The values are the parent record (MULTI_CHILD, LINK; read by
primary key under the user's own security) or the form's current values (SINGLE, as the
possible-values routes already accept). A criterion whose variable has no value is dropped
(`FilterUseCase.DEFAULT`).

### 1.2 Quick-create section (on the target table)

`QTableMetaData.withQuickCreateSectionName("quickCreate")` names one of the table's
`QFieldSection`s. Quick-create shows that section's editable fields. Without one it shows the
table's required, editable fields (`isRequired && isEditable`, no default value needed by the
backend). The section can be hidden (`isHidden`) so it only serves quick-create.

### 1.3 Validation (`QInstanceValidator`)

- name present and unique; mode present; mode-specific attributes present, others absent
  (`fieldName` only for SINGLE; `associationName`/`childTargetFieldName` not for SINGLE).
- SINGLE: `fieldName` is a field of the table; the target resolves (explicit `targetTableName`, or
  the field's possible value source is a TABLE source).
- MULTI_CHILD/LINK: the association exists on the table; `childTargetFieldName` is a field of the
  associated table; the target resolves (explicit, or that field's TABLE possible value source).
- the target table exists; `columns`, `previewFields`, `thumbnailField` are target fields; every
  criterion and order-by of `filter`/`defaultFilter` names a target field.
- `childFieldDefaults`, `childValuesFromTarget` keys and `reviewFieldNames` are child fields,
  `childValuesFromTarget` values are target fields, and none of them is a parent join field or
  `childTargetFieldName`.
- `maxSelections` is 1 to 1000.
- `quickCreateSectionName` names a section of its table.

## 2. API: QQQ UI API v1

All routes are under `/qqq/v1`, specified with `AbstractEndpointSpec` like the other v1 routes,
registered in `MiddlewareVersionV1`, and published in `openapi/v1/openapi.yaml` (regenerated by
`PublishAPI`, checked by `ValidateAPIVersions`). They follow the existing conventions (tag `Tables`,
`OutputRecord` for records, `QueryFilter` for filters, `BasicErrorResponseV1` for errors).
When the UI API audit (`docs/UI-API-AUDIT.md`) lands, these routes follow its versioning and
contract-test rules; they are additive, a 1.x minor change.

### 2.1 Metadata (existing route, new properties)

`GET /qqq/v1/metaData/table/{tableName}` adds:

- `quickCreateSectionName` (string, nullable) on the table.
- `relatedRecordPickers`: a list of `RelatedRecordPicker` objects. A picker is published only when
  the user may READ its target table.

```json
{
  "name": "addSkus", "label": "Add SKUs", "mode": "MULTI_CHILD",
  "fieldName": null, "associationName": "orderLines",
  "targetTableName": "sku", "childTableName": "sales_order_line", "childTargetFieldName": "skuId",
  "filter": { "criteria": [ { "fieldName": "isActive", "operator": "EQUALS", "values": [true] } ] },
  "defaultFilter": { "criteria": [ { "fieldName": "region", "operator": "EQUALS", "values": ["${input.region}"] } ] },
  "columns": ["code", "name", "price", "region"],
  "previewFields": ["code", "name", "description", "price"],
  "thumbnailField": null,
  "reviewFieldNames": ["quantity", "unitPrice"],
  "maxSelections": 200,
  "canCreateTarget": true,
  "canAddChildren": true,
  "canRemoveChildren": false
}
```

`canCreateTarget` = `allowQuickCreate` and INSERT on the target. `canAddChildren` = INSERT on the
child/join table. `canRemoveChildren` = LINK mode and DELETE on the join table. The flags are hints
for the client; every route re-checks them.

### 2.2 Query targets

`POST /qqq/v1/table/{tableName}/relatedRecordPicker/{pickerName}/query`

```json
{
  "primaryKey": 1001,
  "values": { "region": "West" },
  "searchTerm": "widget",
  "filter": { "criteria": [ ... ], "orderBys": [ { "fieldName": "name", "isAscending": true } ] },
  "skip": 0,
  "limit": 50
}
```

- `primaryKey`: the parent record (MULTI_CHILD, LINK; also SINGLE on an edit form). Read with
  `GetAction` under the user's security; not found or not visible is `404`.
- `values`: form values for SINGLE on a create form (used when `primaryKey` is absent).
- `filter`: the user's filter on the target table (the dialog's filter builder or a saved view).
  The picker's `filter` (interpolated) is ANDed with it, and with `searchTerm`.
- `searchTerm`: case-insensitive CONTAINS over the target's `searchFields` (else its record-label
  fields), ORed.
- `skip`/`limit`: offset paging; `limit` 1 to 200, default 50. The route reads `limit + 1` rows for
  `hasMore` and never counts.

Response `RelatedRecordPickerQueryResponseV1`:

```json
{
  "records": [ { "tableName": "sku", "recordLabel": "WID-1 Widget", "values": { ... }, "displayValues": { ... } } ],
  "hasMore": true,
  "linkedIds": [ 17, 23 ],
  "resolvedDefaultFilter": { "criteria": [ { "fieldName": "region", "operator": "EQUALS", "values": ["West"] } ] }
}
```

- `linkedIds` (MULTI_CHILD, LINK with `primaryKey`): which of the returned targets the parent already
  has a child/join row for. LINK shows them as selected (unchecking unlinks); MULTI_CHILD marks them
  "already on this order" but still allows adding another line.
- `resolvedDefaultFilter`: the picker's `defaultFilter` with `${input.x}` resolved, for the client to
  start from (sent only when `skip` is 0).

Security: READ on the target table (`403` otherwise). The query runs as the user, so table
permissions, record security locks and field security locks apply exactly as on
`/table/{target}/query`. The picker filter narrows, never widens.

### 2.3 Prepare child rows (MULTI_CHILD, LINK)

`POST /qqq/v1/table/{tableName}/relatedRecordPicker/{pickerName}/prepare`

```json
{ "primaryKey": 1001, "targetIds": [17, 23] }
```

Response `RelatedRecordPickerPrepareResponseV1`:

```json
{
  "records": [
    { "tableName": "sales_order_line", "values": { "orderId": 1001, "skuId": 17, "quantity": 1, "unitPrice": 9.5 },
      "displayValues": { "skuId": "WID-1 Widget" } }
  ],
  "reviewFieldNames": ["quantity", "unitPrice"]
}
```

One draft per target, in `targetIds` order, with: `childFieldDefaults`, then
`childValuesFromTarget`, then the parent join values and `childTargetFieldName` (always last, so
they cannot be overridden). Nothing is written. Each target must pass the picker query (filter and
security) for this parent, else `400` naming the unavailable ids. Needs INSERT on the child table.

### 2.4 Add child rows (MULTI_CHILD, LINK)

`POST /qqq/v1/table/{tableName}/relatedRecordPicker/{pickerName}/add`

```json
{ "primaryKey": 1001,
  "records": [ { "values": { "skuId": 17, "quantity": 3, "unitPrice": 9.5 } },
               { "values": { "skuId": 23, "quantity": 1, "unitPrice": 4.0 } } ] }
```

or, without a review step (LINK, or MULTI_CHILD with no review fields), `{ "primaryKey": 1001, "targetIds": [17, 23] }`.

- The server builds each row like `prepare` and then applies the client's values for
  `reviewFieldNames` only; other client values are ignored. Parent join fields and
  `childTargetFieldName` are always the server's.
- Each target must pass the picker query for this parent (`400` otherwise).
- All rows are inserted in one `InsertAction` in one transaction: INSERT permission on the child
  table, record security lock checks on the new rows, field validation and value behaviors,
  pre/post-insert customizers, automations, and audit, exactly as a normal create. Any row error
  rolls back all rows; the response lists the errors per row (`400`).

Response `RelatedRecordPickerAddResponseV1`: `{ "records": [OutputRecord], "warnings": [string] }`.

### 2.5 Remove links (LINK)

`POST /qqq/v1/table/{tableName}/relatedRecordPicker/{pickerName}/remove`

```json
{ "primaryKey": 1001, "targetIds": [23] }
```

Deletes the join rows of this parent for these targets with `DeleteAction` (DELETE permission on the
join table, record locks, audit, customizers). Response `RelatedRecordPickerRemoveResponseV1`:
`{ "deletedCount": 1 }`. `400` for other modes (child rows of MULTI_CHILD are deleted with the normal
delete route, from the child list).

### 2.6 Existing routes the picker uses

| Need | Route |
|---|---|
| target table metadata (fields, sections, quick-create section, permissions) | `GET /qqq/v1/metaData/table/{target}` |
| saved views of the target | the existing saved-view process routes |
| quick-create | `POST /qqq/v1/table/{target}` (insert) |
| full create screen | the client's own create route for `{target}` (a link, not an API call) |
| preview of one record | `GET /qqq/v1/table/{target}/{primaryKey}` |
| SINGLE selection's label | the field's possible-values route with `ids` |

### 2.7 Errors

`400` bad body, unknown field in `filter`, picker of another mode, unavailable targets, row
validation errors; `403` missing permission; `404` unknown table, picker or parent. Bodies are
`BasicErrorResponseV1` (`error` message), with per-row messages in `errors` for `add`.

## 3. Security summary

| Operation | Checks |
|---|---|
| see a picker | READ on the target (else not published) |
| query | READ on the target; parent visible to the user (when `primaryKey`); record and field locks on the target via `QueryAction` |
| prepare | as query, plus INSERT on the child table |
| add | INSERT on the child table; every target passes the picker query; `InsertAction` (record locks on the new child rows, validation, audit) in one transaction |
| remove | LINK only; DELETE on the join table; `DeleteAction` (locks, audit) |
| quick-create | INSERT on the target via the normal insert route |

The parent join fields and the target key of a new row always come from the server.

## 4. UX flows (Next UI)

One component family: a `PickerDialogFrame` (dialog chrome: title, header actions, close, focus
trap, full screen on phones, footer) shared by the media `RecordPickerDialog` and the new
`RelatedRecordPickerDialog`. Both are `import()`ed on demand. The media picker keeps its behaviour
and tests; it becomes the PVS-picker configuration of the family.

- **SINGLE.** The FK field's input shows a Browse button next to the typeahead when the field has a
  SINGLE picker. The dialog lists target records in a table with the picker's columns, the target's
  filter builder (starting from `resolvedDefaultFilter`) and saved views, search, sortable column
  headers, Load more, a preview pane (lg and up; a Preview toggle on small screens). Enter, double
  click or Select confirms and fills the field. **New {target}** opens quick-create inside the
  dialog: the quick-create section's fields (or the required fields) in the standard form controls;
  Save inserts through the insert route, then selects the new record. "Open full create screen"
  opens the target's create route in a new tab.
- **MULTI_CHILD.** The parent's child list (record view) gets an **Add {target}** button
  (`canAddChildren`). The dialog allows multiple selection (checkboxes, Space, a selection count,
  already-present targets marked). **Next** calls `prepare` and shows the review step: one row per
  pick with the target's label and the review fields as form controls (types, required, possible
  values from the child table's metadata). **Add** calls `add`; errors show per row and nothing is
  saved until all rows pass. Quick-create of a target is available as in SINGLE.
- **LINK.** The parent's child list gets **Link {target}** (`canAddChildren` or
  `canRemoveChildren`). Linked targets show checked; checking links, unchecking unlinks; **Save**
  sends `add` (targetIds) then `remove` and reports both counts.
- Keyboard: arrows move through rows, Space toggles, Enter confirms, Escape closes (and leaves the
  review step first). Results are a `listbox` (multi-selectable where it applies); the review step
  is a labelled form. Responsive from 320 px (full screen under `sm`), light and dark tokens only.

## 5. CLI / TUI / iOS rendering notes

The metadata carries everything a non-web client needs: the mode says which verbs exist
(`SINGLE`: choose; `MULTI_CHILD`: add with review; `LINK`: link/unlink), `columns` say what to print
per row, `previewFields` what to show for the highlighted row, `reviewFieldNames` with the child
table's field metadata what to prompt for, and the `can*` flags which commands to offer.
A CLI could run:

```
qqq picker sales_order 1001 addSkus --search widget          # query: a table of `columns`
qqq picker sales_order 1001 addSkus --add 17,23              # prepare, prompt review fields, add
qqq picker sales_order 1001 tags --link 4 --unlink 9         # add + remove
```

A TUI renders the query as a scrolling list with a preview panel, the review step as a form per row.
Filters use the same `QueryFilter` JSON as the table query route. No client needs table-specific code.

## 6. Test plan

- **Core (JUnit):** validator cases for each rule in 1.3; frontend metadata publication and
  permission flags; query (picker filter AND user filter AND search, `${input}` from parent and from
  values, record locks hide rows, `hasMore`, `linkedIds`); prepare (order of defaults, copied target
  values, forced join values, unavailable targets); add (review values only, forced values, insert
  permission, record-lock violation, validation error rolls back all rows, audit rows written);
  remove (delete permission, only this parent's rows, mode check).
- **Javalin v1 (HTTP):** each route's status codes and bodies; OpenAPI regenerated and
  `ValidateAPIVersions` green.
- **Sample project:** `customer`, `sku`, `sales_order`, `sales_order_line`, `tag`,
  `sales_order_tag` with the three pickers above and a customer quick-create section.
- **Next UI:** unit tests for the dialog (search, filter, select, keyboard, quick-create, review,
  link/unlink), the media picker's existing tests unchanged; Playwright mocked e2e for the three
  modes; real-backend acceptance against the sample on Chromium, Firefox, WebKit and mobile;
  screenshots light and dark at 390 and 1440 px.

## 7. Phases

| Phase | Repo | Content |
|---|---|---|
| spec | qqq | this document |
| (a) | qqq | metadata, validation, frontend metadata, the four v1 routes, OpenAPI, sample tables, tests |
| (b) | qqq-frontend-next | `PickerDialogFrame`, `RelatedRecordPickerDialog` SINGLE with quick-create |
| (c) | qqq-frontend-next | MULTI_CHILD with the review step, child-list button |
| (d) | qqq-frontend-next | LINK link/unlink |
