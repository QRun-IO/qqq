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
   .withField(new QFieldMetaData("customer_id", QFieldType.INTEGER))
   .withField(new QFieldMetaData("total", QFieldType.DECIMAL));

instance.addTable(table);
```

## License

GNU Affero General Public License v3.0
