# QQQ Utility Lambdas

`QPostToSQSLambda` forwards the incoming Lambda request body to the SQS queue named by the `QUEUE_URL` environment variable, using the AWS default credential provider. Configure its execution role for that queue. The current handler logs the incoming body; review payload sensitivity and log retention before deployment.

QQQ 4.0 requires Java 21. See the [release and build instructions](../README.md) and [4.0 migration guide](../docs/migration/4.0.adoc).

## Source and examples

- [QPostToSQSLambda](src/main/java/com/kingsrook/qqq/utilitylambdas/QPostToSQSLambda.java)

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
