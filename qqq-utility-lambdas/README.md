# QQQ Utility Lambdas

`QPostToSQSLambda` forwards the incoming Lambda request body to the SQS queue named by the `QUEUE_URL` environment variable, using the AWS default credential provider. Configure its execution role for that queue. Missing or blank `QUEUE_URL` is rejected before client creation or credential lookup. The utility forwards raw UTF-8 text without parsing application JSON and does not log the incoming body. Service and credential failures propagate as `IOException`; service error diagnostics are still logged, so provider error text remains subject to the application's log policy.

QQQ 4.0 requires Java 21. See the [release and build instructions](../README.md) and [4.0 migration guide](../docs/migration/4.0.adoc).

## Source and examples

- [QPostToSQSLambda](src/main/java/com/kingsrook/qqq/utilitylambdas/QPostToSQSLambda.java)

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.

## Local acceptance

`SampleSqsUtilityAcceptanceTest` invokes the production stream handler and default AWS SDK client in fresh JVMs. Each child has a cleared environment, an isolated credential file/home, disabled instance metadata, an explicit region, and only synthetic credentials when needed. An owned `127.0.0.1` SQS JSON protocol fixture captures the exact queue and body. No real AWS account, queue, credential or recipient is used.

Eight cases cover exact UTF-8 forwarding, malformed JSON forwarded as raw text, empty-body service rejection, access denial, missing credentials, absent/blank queue configuration, and absence of the synthetic body in success/failure Lambda logs. The fixture models a service response; it does not certify live SQS, IAM, deployment, or arbitrary provider error-content sanitization. Existing SDK retry and credential-chain behavior is unchanged.

Run the module's normal gate from the repository root:

```sh
mvn -B -o -Dmaven.repo.local="$OWNED_M2" -pl qqq-utility-lambdas clean verify
```

The module declares the repository's existing managed JUnit dependency in test scope. No production dependency or SDK version changes. Acceptance remains pending independent review and combined-source verification.
