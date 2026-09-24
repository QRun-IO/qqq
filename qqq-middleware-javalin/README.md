# QQQ Middleware - Javalin

HTTP middleware and application server for QQQ. `QApplicationJavalinServer` starts an `AbstractQQQApplication`, configures middleware routes and can serve the Material Dashboard. Its `withPort(...)` method sets the port before `start()`. See the [sample application](../qqq-sample-project/README.md) for a complete server, metadata and authentication setup. The versioned specifications define the actual endpoint paths and payloads.

QQQ 4.0 requires Java 21. See the [release and build instructions](../README.md) and [4.0 migration guide](../docs/migration/4.0.adoc).

## Source and examples

- [QApplicationJavalinServer](src/main/java/com/kingsrook/qqq/middleware/javalin/QApplicationJavalinServer.java)
- [QJavalinMetaData](src/main/java/com/kingsrook/qqq/middleware/javalin/QJavalinMetaData.java)
- [TableMetaDataSpecV1](src/main/java/com/kingsrook/qqq/middleware/javalin/specs/v1/TableMetaDataSpecV1.java)
- [ProcessMetaDataSpecV1](src/main/java/com/kingsrook/qqq/middleware/javalin/specs/v1/ProcessMetaDataSpecV1.java)
- [Module tests](src/test/java/)

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
