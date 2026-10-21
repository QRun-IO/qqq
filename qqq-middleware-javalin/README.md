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


## Public branding assets for a standalone Node UI (QQQ #1009)

`GET` and `HEAD /qqq/branding/logo` and `/qqq/branding/icon` resolve only the
corresponding current `QInstance.branding` field inside `material-dashboard-overlay`.
They work with `qqq.javalin.frontend=none`. They do not enable a Java dashboard or
mount the whole overlay directory; unlisted files and HTML/script resources remain
outside this endpoint's scope. The existing Next/Material overlay registration is
unchanged.

Eligible declarations are canonical root-relative image paths. Queries/fragments
are not classpath resource names. External/data URLs are not fetched. Image MIME
headers, `nosniff`, a restrictive SVG-safe policy, and `no-cache` accompany the
response; missing/invalid declarations return 404. `setQInstance` hot swaps apply
to subsequent requests. No caller-provided resource path or backend URL is accepted.

The matching Next standalone change internally aliases its two public branding
paths to these roles through its existing API rewrite. **These endpoints are not
present in published 4.1.0-RC.1.** The first numbered release containing #1009 is
not yet assigned; until then, qualification must identify the exact source build
and JAR hashes. The retained RC1 standalone result remains failed (5/43 passed),
and no claim of retroactive compatibility or completed browser qualification is
made. Older deployments can explicitly provide images in the Node `public` tree
or use an already-supported external image URL.
