# qqq

Metadata-driven application framework for building business software in Java.

**For:** Engineers building internal tools, admin panels, data management apps, or CRUD-heavy systems
**QQQ 4.0.0** is the first release under the semver contract. See the [changelog](CHANGELOG.md) for breaking changes, fixes and explicitly deferred issues.

## Why This Exists

Building business applications means writing the same patterns repeatedly: table views, forms, CRUD operations, user permissions, reports, scheduled jobs. Most frameworks make you implement these from scratch.

QQQ takes a different approach. You define your data model and business rules through metadata, and QQQ generates the working application - complete with API, dashboard, and backend logic.

Write Java for custom behavior while sharing metadata across the configured backend, middleware, and dashboard modules.

## Features

- **Metadata-driven tables** - Define entities once, get API + UI + validation
- **Backend modules** - RDBMS, filesystem, MongoDB, S3 out of the box
- **Business processes** - Multi-step workflows with state management
- **Metadata-driven dashboard** - the Next dashboard is served by default; the Material Dashboard remains an explicit option
- **Multiple interfaces** - REST API, CLI, Lambda handlers from same codebase
- **Extensible** - Custom actions, widgets, and integrations when needed

## Quick Start — 4.1 RC1

This branch targets QQQ **4.1.0-RC.1** with Next **1.0.0-RC.11**. Confirm availability in the [RC1 release](https://github.com/QRun-IO/qqq/releases/tag/v4.1.0-RC.1) before installing. This is a prerelease with [known Next limitations](https://github.com/QRun-IO/qqq-frontend-next/releases/tag/v1.0.0-RC.11), including browser and feature parity gaps. For the stable release, use the [4.0.0 instructions](https://github.com/QRun-IO/qqq/blob/v4.0.0/README.md).

**Prerequisites:** JDK 21+, Git, curl and unzip; Bash on macOS, Linux, or Windows WSL. Maven is downloaded automatically by the checked-in Maven Wrapper. No Node.js or Docker is needed.

```bash
curl -fsSLo quickstart.sh https://raw.githubusercontent.com/QRun-IO/qqq/quickstart-4.1.0-RC.1/quickstart.sh
bash quickstart.sh
```

The script checks prerequisites, clones editable sample source into `qqq-sample`, compiles it against published QQQ 4.1.0-RC.1, and opens the Next dashboard at <http://localhost:8000/app/person>. One Java process serves both the API and the dashboard; port 8000 must be free. Seeded H2 data needs no external database or account. Press Ctrl+C to stop; edit the Java source and run `./quickstart.sh` inside the checkout to rebuild and restart. Data resets on restart. Run `QQQ_FRONTEND=material bash quickstart.sh` to open the Material Dashboard instead. See the [sample instructions](qqq-sample-project/README.md) for the walkthrough and logs.

Use the 4.1.0-RC.1 BOM below to align modules, including the default dashboard, in your own application. Migrating an existing application requires the [4.0 migration guide](docs/migration/4.0.adoc).

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.kingsrook.qqq</groupId>
            <artifactId>qqq-bom-pom</artifactId>
            <version>4.1.0-RC.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <dependency>
        <groupId>com.kingsrook.qqq</groupId>
        <artifactId>qqq-backend-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.kingsrook.qqq</groupId>
        <artifactId>qqq-backend-module-rdbms</artifactId>
    </dependency>
</dependencies>
```

Define a table:

```java
new QTableMetaData()
    .withName("order")
    .withBackendName("rdbms")
    .withPrimaryKeyField("id")
    .withField(new QFieldMetaData("id", QFieldType.INTEGER))
    .withField(new QFieldMetaData("customerId", QFieldType.INTEGER))
    .withField(new QFieldMetaData("status", QFieldType.STRING))
    .withField(new QFieldMetaData("total", QFieldType.DECIMAL));
```

Register table metadata in a configured application to expose query and validation behavior through its selected backend, middleware and dashboard.

## Usage

### Adding Backend Modules

```xml
<!-- PostgreSQL, MySQL, etc -->
<artifactId>qqq-backend-module-rdbms</artifactId>

<!-- Local/S3 file storage -->
<artifactId>qqq-backend-module-filesystem</artifactId>

<!-- MongoDB -->
<artifactId>qqq-backend-module-mongodb</artifactId>
```

### Adding Middleware

```xml
<!-- HTTP server with REST API -->
<artifactId>qqq-middleware-javalin</artifactId>

<!-- CLI commands -->
<artifactId>qqq-middleware-picocli</artifactId>

<!-- AWS Lambda -->
<artifactId>qqq-middleware-lambda</artifactId>
```

### Adding the Dashboard

Add the Next dashboard jar. `QApplicationJavalinServer` serves it at `/`, on the same port as the API:

```xml
<dependency>
    <groupId>com.kingsrook.qqq</groupId>
    <artifactId>qqq-frontend-next</artifactId>
</dependency>
```

The [Material Dashboard](https://github.com/QRun-IO/qqq-frontend-material-dashboard) remains available. Applications that depend only on its jar keep serving it, unchanged. With both jars present, choose Material with `withServeFrontendMaterialDashboard(true)` or `-Dqqq.javalin.frontend=material` (`next` and `none` are the other values). To serve both, host Material at another path with `withFrontendMaterialDashboardHostedPath("/material")`.

## Project Status

QQQ 4.0 establishes the semver contract. Major-version migration includes package renames and API removals; consult the [migration guide](docs/migration/4.0.adoc) and [release notes](CHANGELOG.md). QQQ 4.1 makes the [Next dashboard](https://github.com/QRun-IO/qqq-frontend-next) the default; the Material Dashboard (0.42.0-RC.2) remains supported as an explicit option. Comprehensive sample coverage and other deferred work are tracked in [#534](https://github.com/QRun-IO/qqq/issues/534).

## Contributing

```bash
git clone --branch main https://github.com/QRun-IO/qqq.git
cd qqq
mvn clean install
```

See [Developer Onboarding](https://github.com/QRun-IO/qqq/wiki/Developer-Onboarding) and [Contribution Guidelines](https://github.com/QRun-IO/qqq/wiki/Contribution-Guidelines).

### Verify packaged notices

From the reactor root, package the release-profile archives and check their
LICENSE/NOTICE entries before publication:

```bash
mvn -Prelease -DskipTests -Dgpg.skip=true clean package
python3 scripts/check-packaged-notices.py
```

The guard checks main, source and Javadoc JARs for every JAR-producing reactor
module, plus declared test JARs. Each must contain exactly one `META-INF/LICENSE`
and `META-INF/NOTICE`, byte-identical to the root files. Explicit archive paths
can also be supplied to check downloaded distributions. Missing archives,
missing or duplicate entries, and altered notice text fail the check.

Maven shared archive resources preserve the existing main/test resources. Shade
adds the exact root notices after its existing filters, without merging them
with dependency notices; inherited transformer lists append to child manifest
and service transformers. Missing root notice files fail `generate-resources`
before copying, even when stale generated files exist. Maven locates the root
through the checked-in `.mvn` directory, including root `-pl`, module `-f` and
module-directory invocations. The optional `buildShadedJar` profile uses the same
mapping. This explicit post-package guard does not run automatically in the
publishing workflow. These commands skip runtime tests and neither sign nor
publish; existing release checks still apply.

Run the lifecycle from the reactor root (`-pl` or `-f` may select a module).
The existing relative Checkstyle header path requires that working directory;
isolated resource-only probes from a module directory need `-Dcheckstyle.skip=true`.

## Documentation

Start with the [sample application](qqq-sample-project/README.md), [4.0 migration guide](docs/migration/4.0.adoc), [framework documentation](https://www.qrun.io/docs), and [QQQ Wiki](https://github.com/QRun-IO/qqq/wiki).

## License

See [LICENSE](LICENSE), [NOTICE](NOTICE), and the license headers in individual source files.
