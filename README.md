# qqq

Metadata-driven application framework for building business software in Java.

**For:** Engineers building internal tools, admin panels, data management apps, or CRUD-heavy systems
**4.0 status:** Prerelease. Maven Central currently provides `4.0.0-RC.2`; this branch contains additional candidate fixes. `4.0.0-RC.3` and final `4.0.0` have not been published. See the [changelog](CHANGELOG.md) for changes and open release gates.

## Why This Exists

Building business applications means writing the same patterns repeatedly: table views, forms, CRUD operations, user permissions, reports, scheduled jobs. Most frameworks make you implement these from scratch.

QQQ takes a different approach. You define your data model and business rules through metadata, and QQQ generates the working application - complete with API, dashboard, and backend logic.

Write Java for custom behavior while sharing metadata across the configured backend, middleware, and dashboard modules.

## Features

- **Metadata-driven tables** - Define entities once, get API + UI + validation
- **Backend modules** - RDBMS, filesystem, MongoDB, S3 out of the box
- **Business processes** - Multi-step workflows with state management
- **React dashboard** - Material-UI admin interface, zero frontend code required
- **Multiple interfaces** - REST API, CLI, Lambda handlers from same codebase
- **Extensible** - Custom actions, widgets, and integrations when needed

## Quick Start

**Prerequisites:** Java 21+, Maven 3.8+

The published RC.2 is available for evaluation using the BOM below. To exercise the changes on this branch, build from source using the [sample instructions](qqq-sample-project/README.md); RC.2 does not include these unpublished fixes. Migrating an existing application requires the [4.0 migration guide](docs/migration/4.0.adoc).

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.kingsrook.qqq</groupId>
            <artifactId>qqq-bom-pom</artifactId>
            <version>4.0.0-RC.2</version>
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

See [qqq-frontend-material-dashboard](https://github.com/QRun-IO/qqq-frontend-material-dashboard) for the React admin UI.

## Project Status

QQQ 4.0 is undergoing release validation. Major-version migration includes package renames and API removals; consult the [migration guide](docs/migration/4.0.adoc) and [release notes](CHANGELOG.md). Final publication follows candidate verification and QRun-owned reference-application acceptance.

## Contributing

```bash
git clone --branch feature/qqq-4-polish https://github.com/QRun-IO/qqq.git
cd qqq
mvn clean install
```

See [Developer Onboarding](https://github.com/QRun-IO/qqq/wiki/Developer-Onboarding) and [Contribution Guidelines](https://github.com/QRun-IO/qqq/wiki/Contribution-Guidelines).

## Documentation

Start with the [sample application](qqq-sample-project/README.md), [4.0 migration guide](docs/migration/4.0.adoc), [framework documentation](https://www.qrun.io/docs), and [QQQ Wiki](https://github.com/QRun-IO/qqq/wiki).

## License

See [LICENSE](LICENSE), [NOTICE](NOTICE), and the license headers in individual source files.
