# QQQ Middleware - Lambda

AWS Lambda stream-handler integration. `QAbstractLambdaHandler` parses requests and writes responses using `QLambdaRequest` and `QLambdaResponse`; `QBaseCustomLambdaHandler` dispatches JSON bodies to application handlers. `QStandardLambdaHandler` implements `/processes/{name}/init` with optional trailing slash. Its `setupSession()` is empty: an application must supply a valid `QContext` instance/session before process execution and clear request context in `finally`. `setQInstance()` alone does not establish that context or authenticate a caller. Adapt the included example and configure event mapping, authentication, permissions and deployment for your application.

The stream serializes `QLambdaResponse.Body` (`requestId`, optional `errorMessage` and application `body`), not the full response object or an API Gateway proxy envelope. Do not infer a deployed HTTP status from the Java response's status code. Process failures appear in `body.error` and, for user-facing failures, `body.userFacingError`; callers must inspect these fields. Request bodies are forwarded to the process as the raw `body` value. No claim is made here about deployed AWS behavior, long-running asynchronous completion or authentication supplied by the middleware.

Standard metadata, query, count, export, record get/update/delete, possible-values and widget branches are TODO stubs. They do not perform CRUD or return metadata/widgets. The current dispatcher returns an unrecognized-path error for the table/metadata branches and an internal-server-error body for the widget branch. These outcomes are unavailable-route boundaries, not passing CRUD features.

## Local acceptance

[`SampleLambdaDispatchAcceptanceTest`](src/test/java/com/kingsrook/sampleapp/SampleLambdaDispatchAcceptanceTest.java) uses synthetic in-memory streams and an owned memory-backed instance with the existing MOCK authentication provider. Its application subclass explicitly supplies already-resolved test sessions and owns cleanup; it is not an authentication implementation. Tests cover custom request/response and errors, actual process dispatch and caller isolation, missing/invalid sessions, process failure/recovery, unknown routes and the unavailable standard branches. Cleanup assertions run immediately after each invocation, before test teardown. The test lives in this module so the sample needs no new dependency; its methods are mapped by the sample's `lambda.dispatch` ledger row.

From the repository root, using an owned cache containing source-matching dependencies:

```sh
mvn -B -o -nsu -Dmaven.repo.local="$OWNED_M2" -pl qqq-middleware-lambda clean verify
python3 -B -m unittest discover -s qqq-sample-project -p 'test_*.py'
```

These local checks do not verify live AWS, deployment/event mapping, browser behavior or published artifacts. The sample row remains pending independent review and combined acceptance.

QQQ 4.0 requires Java 21. See the [release and build instructions](../README.md) and [4.0 migration guide](../docs/migration/4.0.adoc).

## Source and examples

- [QAbstractLambdaHandler](src/main/java/com/kingsrook/qqq/lambda/QAbstractLambdaHandler.java)
- [QStandardLambdaHandler](src/main/java/com/kingsrook/qqq/lambda/QStandardLambdaHandler.java)
- [QBaseCustomLambdaHandler](src/main/java/com/kingsrook/qqq/lambda/QBaseCustomLambdaHandler.java)
- [ExampleLambdaHandler](src/main/java/com/kingsrook/qqq/lambda/examples/ExampleLambdaHandler.java)
- [Module tests](src/test/java/)

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
