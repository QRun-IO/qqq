# QQQ Backend Module - SQLite

Backend module for SQLite embedded databases.

## Features

- Embedded database support
- File-based or in-memory databases
- No external database server required
- Good for testing and small applications

## Usage

```java
SQLiteBackendMetaData backend = new SQLiteBackendMetaData()
   .withName("local-db")
   .withDatabasePath("/data/app.db");

// In-memory database
SQLiteBackendMetaData memoryBackend = new SQLiteBackendMetaData()
   .withName("test-db")
   .withInMemory(true);
```

## Requirements

- Java 17+

## License

GNU Affero General Public License v3.0
