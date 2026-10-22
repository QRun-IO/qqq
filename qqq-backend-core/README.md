# QQQ Backend Core

Core module of the QQQ framework. Defines metadata models, standard actions, and interfaces for backend modules.

## What It Provides

- **Metadata System** - QInstance, QTableMetaData, QFieldMetaData, QProcessMetaData
- **Action Framework** - QueryAction, InsertAction, UpdateAction, DeleteAction
- **Backend Interfaces** - Contracts for storage backends to implement
- **Validation** - QInstanceValidator for metadata validation
- **Context Management** - QContext for thread-local state

## Usage

```java
QInstance instance = new QInstance();

// Define a table
QTableMetaData table = new QTableMetaData()
   .withName("orders")
   .withBackendName("main-db")
   .withPrimaryKeyField("id")
   .withField(new QFieldMetaData("id", QFieldType.INTEGER))
   .withField(new QFieldMetaData("customerId", QFieldType.INTEGER))
   .withField(new QFieldMetaData("total", QFieldType.DECIMAL));

instance.addTable(table);
```

## Derived-value writes

Trusted system jobs can use `withOmitRecordChangeEvents(true)` on `UpdateInput`,
`InsertInput` and `DeleteInput` when writing derived counters or denormalized totals
that are not business events. The default is false. Required integrity listeners
still run; record locks, field security, validation and permissions are unchanged.
This option does not change audit semantics or require restore mode. See the
[UpdateInput options](../docs/actions/UpdateAction.adoc) for the separate automation,
audit and modify-date controls.

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
