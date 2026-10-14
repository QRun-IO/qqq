# Contingent possible values

A contingent field offers choices determined by other fields. For example, an
address's country controls its state choices, and the state controls its city
choices. Existing fields without dependencies keep their existing behavior.

## Declaring a TABLE source

Register the lookup table and its possible value source, then attach a filter to
the dependent field:

```java
instance.addPossibleValueSource(QPossibleValueSource.newForTable("countryState"));

new QFieldMetaData("stateId", QFieldType.INTEGER)
   .withLabel("State / province")
   .withPossibleValueSourceName("countryState")
   .withPossibleValueSourceFilter(new QQueryFilter(
      new QFilterCriteria("countryId", QCriteriaOperator.EQUALS, "${input.countryId}")));
```

The criterion's `countryId` is a column on the lookup table. The variable's
`countryId` is the name of another field in the calling form. The dependency is
inferred automatically. `ContingentPossibleValueHelper.getDependencyFieldNames`
returns an ordered union of explicit dependencies followed by references in every
criterion value and nested sub-filter. Duplicate names are removed.

Explicit `possibleValueSourceDependsOn` names on table fields must exist on that
table. Inferred input names need not be table fields: existing applications can
supply additional contextual inputs.

## Declaring a CUSTOM source

A custom provider has no filter to parse. Declare all input fields it reads as
dependencies. This example offers field names for the selected table:

```java
instance.addPossibleValueSource(new QPossibleValueSource()
   .withName("fieldNames")
   .withType(QPossibleValueSourceType.CUSTOM)
   .withCustomCodeReference(new QCodeReference(FieldNameProvider.class)));

new QFieldMetaData("fieldName", QFieldType.STRING)
   .withPossibleValueSourceName("fieldNames")
   .withPossibleValueSourceDependsOn(List.of("tableName"));

public class FieldNameProvider implements QCustomPossibleValueProvider<String>
{
   public QPossibleValue<String> getPossibleValue(Serializable id)
   {
      return new QPossibleValue<>(String.valueOf(id), String.valueOf(id));
   }

   public List<QPossibleValue<String>> search(SearchPossibleValueSourceInput input)
   {
      Serializable tableName = input.getOtherValues().get("tableName");
      List<QPossibleValue<String>> choices = new ArrayList<>();
      for(QTableMetaData table : QContext.getQInstance().getTables().values())
      {
         if(tableName == null || table.getName().equals(String.valueOf(tableName)))
         {
            for(QFieldMetaData field : table.getFields().values())
            {
               choices.add(new QPossibleValue<>(field.getName(), field.getLabel()));
            }
         }
      }
      return completeCustomPVSSearch(input, choices);
   }
}
```

The provider must honor `idList` as well as `otherValues`; write validation uses
that search contract. With no dependency selected, providers should implement
unrestricted search where appropriate. The UI endpoints block missing explicit
dependencies in FORM mode with the default behavior before invoking the provider.
If a provider uses additional contextual inputs, declare those dependencies too:
validation groups requests by these declared values.

## Missing dependencies and behavior

`PossibleValueDependencyBehavior` has two values:

- `DISABLE_UNTIL_SET` (also the meaning of null): disable the form field and show a
  hint until every dependency has a value. Missing filter inputs make FORM
  searches return no matches. Missing explicit dependencies also block options.
- `SHOW_ALL_UNTIL_SET`: keep the field enabled. FORM searches omit criteria whose
  inputs are missing, using FILTER interpretation. Criteria with supplied inputs
  still narrow the result; this does not discard the entire filter.

```java
field.withPossibleValueSourceDependencyBehavior(
   PossibleValueDependencyBehavior.SHOW_ALL_UNTIL_SET);
```

The `filter` use case always omits criteria with missing inputs, regardless of
this setting. Table, process and legacy endpoints accept use-case strings
case-insensitively. Omitted or unrecognized use cases default to FORM.

## Filter variables and fallbacks

- `${input.name}` reads the request's `values.name`.
- `${processValues.name}` reads process values supplied to the process endpoint;
  legacy process requests can recover these from their `processUUID`.
- `${input.name}??${processValues.possibleValueFilterValueName}` uses the first
  available value. Generated bulk-edit processes add these process-value fallbacks
  to top-level criteria.
- `${currentUser}` retains the existing current-user expression behavior.

All `${input.*}` references, including both sides of a fallback, are dependencies.
Process-value and current-user references are not field dependencies. Clone a
metadata filter before calling `interpretValues`; interpretation mutates criteria.

The existing generated bulk-edit fallback enricher rebuilds the top-level filter;
it does not preserve sub-filters or its OR grouping. That existing implementation
is unchanged here. Dependency discovery, bulk preparation and direct table/process
searches support nested filters, but generated bulk-edit form searches should use
top-level AND criteria until that separate enrichment limitation is addressed.
The table's original filter is still used for server-side write validation.

## Frontend contract and contexts

UI API v1 (unreleased 1.2.0) exposes two optional field properties on contingent
fields only, including legacy `QFrontendFieldMetaData`:

```json
{
  "possibleValueSourceDependsOn": ["countryId"],
  "possibleValueSourceDependencyBehavior": "DISABLE_UNTIL_SET"
}
```

The behavior is explicitly filled with its default in frontend metadata. Ordinary
fields omit both properties. The write-validation opt-out is backend metadata and
is not a frontend property.

Search a table field using `POST /qqq/v1/table/addressBook/possibleValues/stateId`:

```json
{"values":{"countryId":1},"useCase":"form"}
```

For process fields use
`POST /qqq/v1/processes/addressBookPickPlace/possibleValues/stateId` with the same
body. Process requests can additionally supply `processValues`.

| Context | Values and responsibilities |
|---|---|
| Record create/edit | Send current form values, including unsaved edits, with FORM. |
| Bulk edit | The generated process discovers explicit, multi-criterion and nested dependencies; provide current inputs and generated process fallback values. |
| Process screens | Send current form values and applicable process values with FORM. |
| Filter builder | Send FILTER, so missing form dependencies do not hide all options. |
| Related-record picker | Send the form values; picker filters already interpolate these inputs. |
| Quick-create | Send the new record's current values, preserving the same dependency behavior as full create. |

Each frontend must observe the metadata, re-query when any dependency changes,
clear a dependent selection that no longer fits, and apply the disable/hint rule.
Ignore stale search responses that arrive after a newer dependency change. Apply
these rules transitively: changing country may clear state, which must then clear
city. These backend changes do not implement frontend state management.

## Server-side write validation

`InsertAction.performValidations` and `UpdateAction.performValidations` validate
contingent fields beside required fields. Validation runs only when the input
source requests required-field validation and is not `QInputSource.SYSTEM`.
SYSTEM already validates required fields in this codebase; it is explicitly
excluded from this additional validation without changing that existing contract.
This covers user-sourced form, bulk-edit, quick-create and API table writes.

On insert, every nonempty dependent value is checked. On update, a field is checked
only if that field or a dependency appears in the submitted values. Missing update
values are obtained from the old record; if unavailable, a batched query fetches
only the needed records and columns. Those values do not get added to the update.

Validation interprets filters with REMOVE_FROM_FILTER behavior: an empty country
does not by itself invalidate a state. A supplied country does restrict it. Each
invalid value adds a `BadInputStatusMessage` to its record, for example:

> State [1] is not a valid choice for the selected Country

Multiple dependency labels are joined with `and`. Existing write flows skip
records with errors. UI API table insert returns its existing validation-error
response. No source means no check; ENUM sources without filters are skipped.
To opt a field out explicitly:

```java
field.withValidatePossibleValueSourceOnWrite(false);
```

Null means validation is enabled. This validates declared relationships, not an
entire database foreign-key graph. A concurrent change to a lookup record after
the search is not prevented by this application-level validation.

### Process-step limitation

Process possible-value searches and metadata support dependencies. Generic
frontend-step submissions do not gain automatic contingent write validation.
`RunProcessAction.validateUserInputValues` checks server-owned inputs before
restoring process state; it is not a merged form-values validation hook.
`RunBackendStepAction.ensureInputFieldsAreInRequest` supplies defaults and uses a
callback for missing backend inputs, rather than validating submitted frontend
forms. No new process validation lifecycle is introduced here. A process that
writes through a user-sourced InsertAction/UpdateAction gets table validation;
other processes must validate their inputs in their backend step as needed.

## Multi-value, batching and caching

Collection-valued dependent fields validate every nonempty element. A collection
substituted for `${input.a}` is flattened into criterion values, allowing
`IN ${input.a}` and `NOT_IN ${input.a}` to receive the intended list.

Validation groups records by field and resolved dependency values, unions the
selected ids and performs one possible-value search per group. Validation removes
the interactive 250-result limit so a large valid batch is not truncated. Custom
providers receive the group's representative complete record values and must
base contingent decisions on their declared dependencies.

There is no possible-value result caching today. Frontends that introduce a cache
must key it by source, dependency values, use case and relevant user/process
context, and invalidate dependent choices when those inputs change.

## Sample application

The H2 sample server and CLI load `prime-test-database.sql`. Existing sample
tables are unchanged. The new **Places** navigation section contains:

| Name | Fields / behavior |
|---|---|
| `country` | `id`, `name`; TABLE PVS `country` |
| `countryState` | `id`, `countryId`, `name`; TABLE PVS `countryState` |
| `stateCity` | `id`, `stateId`, `name`; TABLE PVS `stateCity` |
| `addressBook` | `id`, `label`, `countryId`, `stateId`, `cityId`; country â†’ state â†’ city cascade |
| `addressBookPickPlace` | `pickPlace` frontend step with `countryId`, `stateId`, `cityId`; `echoCity` backend step; `result` displays `chosenCityName` |

Countries: USA = 1, Canada = 2. States/provinces: Illinois = 1, Missouri = 2,
Ontario = 3, Quebec = 4. Cities: Chester = 1, Springfield = 2, St. Louis = 3,
Kansas City = 4, Toronto = 5, Ottawa = 6, Montreal = 7, Quebec City = 8.
Addresses: row 1 is `(countryId=1, stateId=1, cityId=1)`; row 2 is `(2,3,5)`.
These names and ids are stable fixtures for the Next acceptance spec.

## Rollout audit

| Repo | Table / process | Field A -> Field B | Mechanism | Status |
|---|---|---|---|---|
| KofTwentyTwo/Website-Backend | menu_item | menuId -> parentId | filter `menuId` | PR #167 (draft) |
| KofTwentyTwo/Website-Backend | page | siteId -> parentPageId, featuredMediaId | filter `siteId` | PR #167 (draft) |
| KofTwentyTwo/Website-Backend | project | siteId -> categoryId, featuredMediaId | filter `siteId` | PR #167 (draft) |
| KofTwentyTwo/Website-Backend | project_category | siteId -> featuredMediaId | filter `siteId` | PR #167 (draft) |
| KofTwentyTwo/Website-Backend | photo_album | projectId -> projectUpdateId | filter `projectId` | PR #167 (draft) |
| KofTwentyTwo/Website-Backend | photo_album, photo_album_folder | siteId -> projectId, photoAlbumFolderId, parentId | filter + SHOW_ALL_UNTIL_SET (siteId optional) | after this RC |
| KofTwentyTwo/Website-Backend | photo_album | id -> coverPhotoId | filter `photoAlbumId` | after this RC |
| KofTwentyTwo/Website-Backend | blog_post_tag_int, blog_post, project_update | blogPostId -> blogTagId; blogId / projectId -> featuredMediaId | custom provider + dependsOn | after this RC |
| QRun-IO/qbit-crm | crmDeal | pipelineId -> pipelineStageId | filter | PR #6 |
| QRun-IO/qbit-crm | crmActivity | activityTypeId -> outcomeId | filter | PR #6 |
| QRun-IO/qbit-crm | crmEmailListMember | contactId -> formSubmissionId | filter | PR #6 |
| QRun-IO/qbit-crm | crmActivity | companyId -> contactId, dealId | - | not applied (company optional) |
| QRun-IO/qbit-easypost-tracking | easypost_tracked_table | sourceTableName -> trackingCode/carrier/notifyUser field names | custom provider reads otherValues | PR #17 |
| QRun-IO/qbit-marketing | campaign, claim, finding, metric, outcome_event | productId -> product-scoped FKs (11) | filter `productId` | PR #10 |
| QRun-IO/qbit-wms | 21 tables + CycleCount form | warehouseId / clientId / itemId / activityTypeId scoping (30 filters) | filter | PR #5 |
| QRun-IO/qbit-assistant | assistant_provider | providerType -> model, kind | custom provider + dependsOn | requested from the assistant agent |
| QRun-IO/qbit-data-analysis | watch | table_name -> saved_id | filter | requested from the data-analysis agent |
| QRun-IO/qbit-collaboration | comment; feature setting | threadId -> parentCommentId; scopeType -> scopeName | filter; custom provider | requested from the collaboration agent |
| QRun-IO/qbit-secured-data, qbit-email, qbit-file-gateway, qbit-edi | (no PVS fields on develop yet) | table -> field; provider -> from address; connection -> bucket; partner -> mapping | filter / custom provider | rule sent to the owning agents |
| QRun-IO/qbit-alerts | alerts_mute, alerts_subscription | scopeType -> scopeValue | custom provider + dependsOn (scopeValue is free text today) | follow-up after this RC |
| QRun-IO/qbit-backups | backup_plan_scope | selectorType -> selectorValue | custom provider + dependsOn | follow-up after this RC |
| QRun-IO/qbit-web-performance | budget | scope -> scopeValue | custom provider + dependsOn | follow-up after this RC |
| QRun-IO/qbit-cdn | cdn_table_tag_rule | tableName -> publishedField | `tables` PVS + field provider | follow-up after this RC |
| QRun-IO/qbit-webhooks | webhooks_subscription | eventTypeName -> include/excludeFields; apiName -> apiVersion | field provider; core apiVersion provider | follow-up |
| QRun-IO/qbit-workflows | workflow_revision, workflow_test_scenario | apiName -> apiVersion | core apiVersion provider | follow-up (core) |
| QRun-IO/qbit-workflows | tableTrigger, RunRecordWorkflow, TestAssertion, UpdateInputRecordFieldStep | table -> workflow / field | filter / custom | already contingent |
| QRun-IO/qbit-quick-search | config and process field PVSs | table -> field | custom provider | already contingent |
| QRun-IO/qbit-customizable-table-views | customizable_table, view field, view widget | tableName / tableViewId -> view, field, widget | filter / custom | already contingent |
| QRun-IO/qbit-object-storage, sql-workbench, authentik, others | - | - | - | no editable pairs (audited 2026-10-13) |
