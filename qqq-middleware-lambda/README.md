# QQQ Middleware - Lambda

AWS Lambda stream-handler integration. `QAbstractLambdaHandler` parses requests and writes responses using `QLambdaRequest` and `QLambdaResponse`; `QStandardLambdaHandler` implements process-init dispatch (its other standard action branches are unfinished) and `QBaseCustomLambdaHandler` supports application handlers. Adapt the included example and configure the AWS event mapping, authentication, permissions and deployment for your application.

QQQ 4.0 requires Java 21. See the [release and build instructions](../README.md) and [4.0 migration guide](../docs/migration/4.0.adoc).

## Source and examples

- [QAbstractLambdaHandler](src/main/java/com/kingsrook/qqq/lambda/QAbstractLambdaHandler.java)
- [QStandardLambdaHandler](src/main/java/com/kingsrook/qqq/lambda/QStandardLambdaHandler.java)
- [QBaseCustomLambdaHandler](src/main/java/com/kingsrook/qqq/lambda/QBaseCustomLambdaHandler.java)
- [ExampleLambdaHandler](src/main/java/com/kingsrook/qqq/lambda/examples/ExampleLambdaHandler.java)
- [Module tests](src/test/java/)

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
