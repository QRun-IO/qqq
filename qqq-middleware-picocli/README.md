# QQQ Middleware - PicoCLI

Command-line interface built with picocli. `QPicoCliImplementation` takes a configured `QInstance`; `runCli(String name, String[] args)` returns the command exit code. `QCommandBuilder` defines the available commands and options. Use the [sample CLI](../qqq-sample-project/README.md) and its `--help` output for runnable invocation examples.

Each invocation initializes its own `QContext` and authenticates using that instance's configured provider. Commands use the resulting session, and the caller's context is restored afterward, including on failure. Embedded callers must configure the CLI's authentication provider; a pre-existing caller session does not authorize CLI operations.

Data and process commands enforce the authenticated session's permission rules. Criteria that reference joined tables also require read access to those tables. Denied exports leave their destination files untouched.

CLI criteria currently accept single-token values: spaces in a value are truncated ([#548](https://github.com/QRun-IO/qqq/issues/548)). Use primary-key selection for mutations involving such values until this deferred parser limitation is resolved.

QQQ 4.0 requires Java 21. See the [release and build instructions](../README.md) and [4.0 migration guide](../docs/migration/4.0.adoc).

## Source and examples

- [QPicoCliImplementation](src/main/java/com/kingsrook/qqq/middleware/picocli/QPicoCliImplementation.java)
- [QCommandBuilder](src/main/java/com/kingsrook/qqq/middleware/picocli/QCommandBuilder.java)
- [Module tests](src/test/java/)

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
