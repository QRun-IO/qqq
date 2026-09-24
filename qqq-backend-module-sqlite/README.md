# QQQ Backend Module - SQLite

SQLite backend built on the shared RDBMS actions. Configure a file path with `SQLiteBackendMetaData.withPath`; the backend builds a `jdbc:sqlite:` connection string. SQLite SQL behavior is supplied by `SQLiteRDBMSActionStrategy`.

QQQ 4.0 requires Java 21. See the [release and build instructions](../README.md) and [4.0 migration guide](../docs/migration/4.0.adoc).

## Source and examples

- [SQLiteBackendMetaData](src/main/java/com/kingsrook/qqq/backend/module/sqlite/model/metadata/SQLiteBackendMetaData.java)
- [SQLiteRDBMSActionStrategy](src/main/java/com/kingsrook/qqq/backend/module/sqlite/strategy/SQLiteRDBMSActionStrategy.java)
- [Module tests](src/test/java/)

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
