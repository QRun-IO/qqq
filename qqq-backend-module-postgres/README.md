# QQQ Backend Module - PostgreSQL

PostgreSQL backend module for the QQQ framework.

## Features

- Full CRUD operations support
- Connection pooling via C3P0
- Transaction management
- Batch operations
- Generated key retrieval using RETURNING clause
- PostgreSQL 12+ support

## Usage

```java
// Define backend
PostgreSQLBackendMetaData backend = new PostgreSQLBackendMetaData()
   .withName("postgres-main")
   .withHostName("localhost")
   .withPort(5432)
   .withDatabaseName("myapp")
   .withUsername("user")
   .withPassword("password");

// Add to QInstance
QInstance instance = new QInstance();
instance.addBackend(backend);

// Define table
QTableMetaData table = new QTableMetaData()
   .withName("users")
   .withBackendName("postgres-main")
   .withBackendDetails(new PostgreSQLTableBackendDetails()
      .withTableName("users"))
   .withPrimaryKeyField("id")
   .withField(new QFieldMetaData("id", QFieldType.INTEGER))
   .withField(new QFieldMetaData("email", QFieldType.STRING));

instance.addTable(table);
```

## Connection Pooling

```java
PostgreSQLBackendMetaData backend = new PostgreSQLBackendMetaData()
   .withName("postgres-main")
   .withHostName("localhost")
   .withDatabaseName("myapp")
   .withUsername("user")
   .withPassword("password")
   .withConnectionProvider(new QCodeReference(C3P0PooledConnectionProvider.class))
   .withConnectionPoolSettings(new ConnectionPoolSettings()
      .withMinPoolSize(5)
      .withMaxPoolSize(20));
```

## Requirements

- PostgreSQL 12+
- Java 17+

## License

GNU Affero General Public License v3.0
