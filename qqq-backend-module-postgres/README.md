# QQQ Backend Module - PostgreSQL

PostgreSQL backend module for the QQQ framework.

## Features

- Full CRUD operations support
- Connection pooling via C3P0
- Transaction management
- Batch operations
- Generated key retrieval using RETURNING clause

## Usage

```java
// Define backend
PostgreSQLBackendMetaData backend = new PostgreSQLBackendMetaData();
backend
   .withName("postgres-main")
   .withHostName("localhost")
   .withPort(5432)
   .withDatabaseName("myapp")
   .withUsername(System.getenv("DB_USERNAME"))
   .withPassword(System.getenv("DB_PASSWORD"));

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
PostgreSQLBackendMetaData backend = new PostgreSQLBackendMetaData();
backend
   .withName("postgres-main")
   .withHostName("localhost")
   .withDatabaseName("myapp")
   .withUsername(System.getenv("DB_USERNAME"))
   .withPassword(System.getenv("DB_PASSWORD"))
   .withConnectionProvider(new QCodeReference(C3P0PooledConnectionProvider.class))
   .withConnectionPoolSettings(new ConnectionPoolSettings()
      .withMinPoolSize(5)
      .withMaxPoolSize(20));
```

## Requirements

- A configured PostgreSQL database; validate your server version with your application
- Java 21

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
