# UI API / published model API parity

**API = admin parity:** every visible admin table, field, process and table
capability must also be available in each published API/version, or have an
explicit exception with a non-blank reason. A metadata exclusion flag alone is
not a reason. New UI metadata must fail the application build until published or
deliberately excluded.

`com.kingsrook.qqq.api.actions.UiApiModelApiParityChecker` accepts a validated,
enriched `QInstance`, an API name, a supported version string, and
`ParityExclusions`. It returns a sorted `List<String>` of drift, including stale
exclusions. It uses `MetaDataAction`, `TableMetaDataAction` and
`ProcessMetaDataAction`, the core actions behind `/qqq/v1`, with a
`QSystemUserSession`. It restores the caller's context and does not run CRUD or
process bodies. Application metadata and permission customizers still run.

The checks cover:

- Table and process publication metadata and inclusive version ranges.
- Visible physical and virtual fields, resolved with `GetTableApiFieldsAction`,
  their API aliases, duplicate API names and matching `QFieldType`. Exact type
  equality is intentional: a string representation does not make an integer or
  date field interchangeable with a string field.
- Effective permission level, base name, denied clients, table permission names
  and permission decisions through `PermissionsHelper`. Both APIs use the same
  core rules; there is no separate API permission-rules supplement. The checker
  compares the built-in `next-ui` and `api` channels, catching channel-specific
  denials even with all permissions. This is a metadata contract test, not an
  authentication, credential-scope or record-security integration test.
- `TABLE_QUERY`, `TABLE_GET`, `TABLE_COUNT`, `TABLE_INSERT`, `TABLE_UPDATE` and
  `TABLE_DELETE`, using the same instance-then-table operation enablement and
  backend capabilities as model API execution/OpenAPI generation. Count is
  supplied by the query response, so it requires query enablement; there is no
  separate model API count operation.

Objects hidden by the UI metadata actions or marked hidden are outside this
check. All remaining processes require publication or an exception, including
processes without any API supplement; removing a supplement cannot silently
remove a process from the parity contract.

## Add the guard to an application test

Use application metadata built through its normal validation/enrichment path.
Keep a **fixed**, reviewed exceptions list per API/version. Never generate
exceptions from whatever happens to be missing in the current metadata.

```java
QInstance instance = buildApplicationInstance();
new QInstanceValidator().validate(instance);

ParityExclusions exclusions = new ParityExclusions()
   .withTable("internalJob", "Operational state is managed only through the admin UI")
   .withProcess("rebuildIndex", "Operator-only maintenance procedure")
   .withField("order", "cost", "Internal cost is not part of the customer contract")
   .withCapability("order", Capability.TABLE_DELETE, "Published orders are retained");

List<String> drift = new UiApiModelApiParityChecker()
   .check(instance, "customer-api", "2026.Q3", exclusions);
assertTrue(drift.isEmpty(), String.join("\n", drift));
```

Repeat for each API in `ApiInstanceMetaDataContainer.of(instance).getApis()` and
each of its `getSupportedVersions()`. An unknown API or unsupported version is
an input error. Custom authentication modules must accept the framework's system
session for these metadata actions.

Exclusion names are **core metadata names**, not API aliases. A whole-table
exception covers its fields, permissions and capabilities. Field and capability
exceptions leave the rest of the table checked. Entries that no longer match a
checked UI surface (including redundant children of an excluded table) produce
`stale exclusion` drift. Reasons are available from `getReasons()`; the checker
does not mutate that object. An exception still matches an existing UI surface
even if it has since been published, so review reasons when expanding contracts.

See `UiApiModelApiParityCheckerTest` for the committed `test-api` and `person-api`
exceptions and independent drift mutations. No API runtime behavior is changed.
