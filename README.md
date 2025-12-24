# qqq

Metadata-driven application framework for building business software in Java.

**For:** Engineers building internal tools, admin panels, data management apps, or CRUD-heavy systems  
**Status:** Stable (v0.27)

## Why This Exists

Building business applications means writing the same patterns repeatedly: table views, forms, CRUD operations, user permissions, reports, scheduled jobs. Most frameworks make you implement these from scratch.

QQQ takes a different approach. You define your data model and business rules through metadata, and QQQ generates the working application - complete with API, dashboard, and backend logic.

This isn't a no-code tool. You write Java when you need custom behavior. But the boilerplate - the 80% that's the same across every app - is handled for you.

## Features

- **Metadata-driven tables** - Define entities once, get API + UI + validation
- **Backend modules** - RDBMS, filesystem, MongoDB, S3 out of the box
- **Business processes** - Multi-step workflows with state management
- **React dashboard** - Material-UI admin interface, zero frontend code required
- **Multiple interfaces** - REST API, CLI, Lambda handlers from same codebase
- **Extensible** - Custom actions, widgets, and integrations when needed

## Quick Start

**Prerequisites:** Java 17+, Maven 3.8+

```xml
<dependency>
    <groupId>com.kingsrook.qqq</groupId>
    <artifactId>qqq-backend-core</artifactId>
    <version>0.27.0</version>
</dependency>

<dependency>
    <groupId>com.kingsrook.qqq</groupId>
    <artifactId>qqq-backend-module-rdbms</artifactId>
    <version>0.27.0</version>
</dependency>
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

QQQ generates: REST endpoints, dashboard screens, query capabilities, and validation.

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

**Maturity:** Stable, used in production systems  
**Breaking changes:** Major versions may break API; see release notes  

**Roadmap:**
- Java 21 migration
- Improved widget system
- Enhanced process tracing

## Contributing

```bash
git clone git@github.com:QRun-IO/qqq.git
cd qqq
mvn clean install
```

See [Developer Onboarding](https://github.com/QRun-IO/qqq/wiki/Developer-Onboarding) and [Contribution Guidelines](https://github.com/QRun-IO/qqq/wiki/Contribution-Guidelines).

## Documentation

Full documentation: [QQQ Wiki](https://github.com/QRun-IO/qqq/wiki)

## License

AGPL-3.0
