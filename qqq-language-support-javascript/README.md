# QQQ Language Support - JavaScript

JavaScript execution through the standalone Nashorn engine. `QJavaScriptExecutor` implements the core `QCodeExecutor` contract for a `QCodeReference`, an input context and an execution logger. Use the script/code-reference contracts and module tests when integrating scripts into customizers or process steps.

QQQ 4.0 requires Java 21. See the [release and build instructions](../README.md) and [4.0 migration guide](../docs/migration/4.0.adoc).

## Source and examples

- [QJavaScriptExecutor](src/main/java/com/kingsrook/qqq/languages/javascript/QJavaScriptExecutor.java)
- [Module tests](src/test/java/)

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
