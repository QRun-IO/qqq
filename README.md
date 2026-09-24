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
- **Metadata-driven dashboards** - Next for the local quickstart, plus the existing Material UI dashboard
- **Multiple interfaces** - REST API, CLI, Lambda handlers from same codebase
- **Extensible** - Custom actions, widgets, and integrations when needed

## Quick Start

**Prerequisites:** JDK 21+, Git, curl, unzip, and Docker installed and running; Bash on macOS, Linux, or Windows WSL. Maven is downloaded automatically by the checked-in Maven Wrapper.

```bash
curl -fsSLo quickstart.sh https://raw.githubusercontent.com/QRun-IO/qqq/quickstart-4.0.0/quickstart.sh
bash quickstart.sh
```

The script checks prerequisites, clones editable sample source into `qqq-sample`, compiles it against published QQQ 4.0.0, and opens the Next dashboard at <http://localhost:3000/app/person>. Seeded H2 data needs no external database or account. Ports 8000 and 3000 must be free. Press Ctrl+C to stop; edit the Java source and run `./quickstart.sh` inside the checkout to rebuild and restart. Data resets on restart. See the [sample instructions](qqq-sample-project/README.md) for the walkthrough, logs, existing Material option, and current Next limitations.

Use the 4.0.0 BOM below to align modules in your own application. Migrating an existing application requires the [4.0 migration guide](docs/migration/4.0.adoc).

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.kingsrook.qqq</groupId>
            <artifactId>qqq-bom-pom</artifactId>
            <version>4.0.0</version>
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

QQQ 4.0 establishes the semver contract. Major-version migration includes package renames and API removals; consult the [migration guide](docs/migration/4.0.adoc) and [release notes](CHANGELOG.md). The standard Material dashboard release is 0.41.0; Next remains a compatible preview. Comprehensive sample coverage and other deferred work are tracked in [#534](https://github.com/QRun-IO/qqq/issues/534).

## Contributing

```bash
git clone --branch main https://github.com/QRun-IO/qqq.git
cd qqq
mvn clean install
```

See [Developer Onboarding](https://github.com/QRun-IO/qqq/wiki/Developer-Onboarding) and [Contribution Guidelines](https://github.com/QRun-IO/qqq/wiki/Contribution-Guidelines).

## Documentation

Start with the [sample application](qqq-sample-project/README.md), [4.0 migration guide](docs/migration/4.0.adoc), [framework documentation](https://www.qrun.io/docs), and [QQQ Wiki](https://github.com/QRun-IO/qqq/wiki).

## License

See [LICENSE](LICENSE), [NOTICE](NOTICE), and the license headers in individual source files.
