# QQQ Backend Module - MongoDB

MongoDB storage backend for QQQ tables, including query, count, aggregate, insert, update and delete actions. Configure the connection and database with `MongoDBBackendMetaData`; map a table to a collection with `MongoDBTableBackendDetails.withTableName`. Database behavior and supported operations are covered by the module tests.

QQQ 4.0 requires Java 21. See the [release and build instructions](../README.md) and [4.0 migration guide](../docs/migration/4.0.adoc).

## Source and examples

- [MongoDBBackendMetaData](src/main/java/com/kingsrook/qqq/backend/module/mongodb/model/metadata/MongoDBBackendMetaData.java)
- [MongoDBTableBackendDetails](src/main/java/com/kingsrook/qqq/backend/module/mongodb/model/metadata/MongoDBTableBackendDetails.java)
- [MongoDBBackendModule](src/main/java/com/kingsrook/qqq/backend/module/mongodb/MongoDBBackendModule.java)
- [Module tests](src/test/java/)

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
