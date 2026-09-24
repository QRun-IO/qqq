# QQQ Middleware - Application API

Application-facing API support with instance, table, field and process API metadata. `QJavalinApiHandler` serves these APIs, while `GenerateOpenApiSpecAction` generates their OpenAPI descriptions. Configure the API metadata and version for your application; adding the dependency alone does not expose every table.

QQQ 4.0 requires Java 21. See the [release and build instructions](../README.md) and [4.0 migration guide](../docs/migration/4.0.adoc).

## Source and examples

- [ApiInstanceMetaData](src/main/java/com/kingsrook/qqq/api/model/metadata/ApiInstanceMetaData.java)
- [ApiTableMetaData](src/main/java/com/kingsrook/qqq/api/model/metadata/tables/ApiTableMetaData.java)
- [QJavalinApiHandler](src/main/java/com/kingsrook/qqq/api/javalin/QJavalinApiHandler.java)
- [GenerateOpenApiSpecAction](src/main/java/com/kingsrook/qqq/api/actions/GenerateOpenApiSpecAction.java)
- [Module tests](src/test/java/)

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
