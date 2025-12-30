# QQQ Backend Module - RDBMS

Backend module for relational databases via JDBC. Base module for database-specific implementations.

## Features

- JDBC database connectivity
- Connection pooling (C3P0)
- Transaction management
- Query building and execution
- Batch operations

## Usage

```java
RDBMSBackendMetaData backend = new RDBMSBackendMetaData()
   .withName("main-db")
   .withJdbcUrl("jdbc:mysql://localhost:3306/myapp")
   .withUsername("user")
   .withPassword("password");

QTableMetaData table = new QTableMetaData()
   .withName("users")
   .withBackendName("main-db")
   .withPrimaryKeyField("id");
```

## Database-Specific Modules

- `qqq-backend-module-postgres` - PostgreSQL
- `qqq-backend-module-sqlite` - SQLite

## License

GNU Affero General Public License v3.0
