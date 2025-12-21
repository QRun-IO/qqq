# CLAUDE.md - QQQ Framework Guide

## Project Overview

QQQ is a low-code application framework for engineers by Kingsrook, LLC. It uses metadata-driven architecture where applications are defined through configuration rather than code generation.

**Current Version:** 0.33.0-SNAPSHOT
**License:** AGPL-3.0
**Java Version:** 17+

## Repository Structure

```
qqq/
├── qqq-backend-core/           # Foundation: metadata models, actions, interfaces
├── qqq-backend-module-rdbms/   # JDBC adapter (parent for SQLite/PostgreSQL)
├── qqq-backend-module-sqlite/  # SQLite backend
├── qqq-backend-module-postgres/# PostgreSQL backend
├── qqq-backend-module-mongodb/ # MongoDB backend
├── qqq-backend-module-api/     # REST/API backend
├── qqq-backend-module-filesystem/ # Filesystem storage
├── qqq-middleware-javalin/     # HTTP server (SPA support, deep linking)
├── qqq-middleware-picocli/     # CLI support
├── qqq-middleware-lambda/      # AWS Lambda adapter
├── qqq-middleware-slack/       # Slack integration
├── qqq-middleware-api/         # Base API middleware
├── qqq-middleware-health/      # Health checks
├── qqq-language-support-javascript/ # JS scripting
├── qqq-openapi/                # OpenAPI/Swagger docs
├── qqq-utility-lambdas/        # Lambda utilities
├── qqq-bom/                    # Bill of Materials
├── qqq-dev-tools/              # Developer tooling
└── qqq-sample-project/         # Example application
```

## Build Commands

```bash
mvn clean install                              # Full build + tests
mvn clean compile -DskipTests                  # Compile only
mvn clean verify                               # Build + tests + coverage
mvn clean verify -Dcoverage.haltOnFailure=false # Ignore coverage failures
mvn checkstyle:check                           # Validate code style
mvn test -Dtest=ClassName                      # Run specific test
mvn test -Dtest=ClassName#methodName           # Run specific test method
```

## Code Style (Strictly Enforced)

### Formatting
- **Indentation:** 3 spaces (NO tabs)
- **Braces:** Opening brace on NEXT line
- **Imports:** NO wildcards, lexicographical order
- **Wrapper types:** Always use Integer/Long/Boolean over int/long/boolean

### Naming
- **Fields/variables:** `lowerCaseFirstCamelStyle` (e.g., `firstName`)
- **MetaDataProducer NAME:** `lowerCaseFirstCamelStyle` (e.g., `"orderTable"`)
- **Classes:** `CapitalCamelCase`
- **MetaDataProducer classes:** `{Name}{Type}MetaDataProducer`

### Comments (Javadoc Flower Box)
```java
/*******************************************************************************
 ** Description of the class or method.
 *******************************************************************************/
public class MyClass
{
   // ...
}
```

### Inline Comments (Box Style)
```java
/////////////////////////////////////////////////////////////////////////
// Explanation of complex logic goes here                              //
/////////////////////////////////////////////////////////////////////////
```

### Fluent Style (Required)
```java
// CORRECT
Order order = new Order()
   .withCustomerId(id)
   .withStatus("PENDING");

// WRONG
Order order = new Order();
order.setCustomerId(id);
order.setStatus("PENDING");
```

### Logging
```java
private static final QLogger LOG = QLogger.getLogger(MyClass.class);
LOG.info("Message", logPair("key", value));
// NEVER use System.out, System.err, or printStackTrace()
```

## Core Patterns

### MetaDataProducerInterface
All metadata defined via producer classes:
```java
public class OrderTableMetaDataProducer implements MetaDataProducerInterface<QTableMetaData>
{
   public static final String NAME = "order";

   @Override
   public QTableMetaData produce(QInstance qInstance)
   {
      return new QTableMetaData()
         .withName(NAME)
         .withPrimaryKeyField("id")
         // ...
   }
}
```

### RecordEntity
Preferred for entity beans (use `@QMetaDataProducingEntity`):
```java
@QMetaDataProducingEntity
public class Order extends QRecord
{
   public static final String TABLE_NAME = "order";

   private Integer id;
   private Integer customerId;

   public Order withId(Integer id) { this.id = id; return this; }
   public Integer getId() { return id; }
}
```

### QContext
Thread-local context for session/backend references. Use `CapturedContext` for async operations.

## Testing

- **Framework:** JUnit 5 + AssertJ
- **Coverage:** 80% instructions, 95% classes (enforced)
- **Naming:** `test{Method}_{scenario}_{expected}`
- **Pattern:** Arrange-Act-Assert

```java
@Test
void testProcessOrder_validOrder_succeeds()
{
   // Arrange
   Order order = new Order().withStatus("PENDING");

   // Act
   Order result = service.processOrder(order);

   // Assert
   assertThat(result.getStatus()).isEqualTo("PROCESSED");
}
```

## SPA Support (qqq-middleware-javalin)

### IsolatedSpaRouteProvider
Serves SPAs with deep linking:
```java
new IsolatedSpaRouteProvider()
   .withSpaPath("/app")
   .withStaticFilesPath("/spa-files")
   .withDeepLinking(true)
```

### Base Href Behavior
- Root paths (`/`, `/app/`): NO `<base href>` tag
- Deep links (`/app/table/record`): Injects `<base href="/app/">`

## Key Classes Reference

| Class | Purpose |
|-------|---------|
| `QInstance` | Root metadata container |
| `QTableMetaData` | Table definitions |
| `QFieldMetaData` | Field definitions |
| `QProcessMetaData` | Workflow definitions |
| `QRecord` | Dynamic record (key-value) |
| `AbstractQActionFunction` | Base for all actions |
| `QContext` | Thread-local context |
| `QLogger` | Logging wrapper (log4j2) |

## Git Conventions

- **Commits:** Conventional format (`feat:`, `fix:`, `docs:`, etc.)
- **Branches:** Feature branches from `develop`
- **PRs:** Against `develop` branch
- **Signing:** GPG signing required

## Common Gotchas

1. **Integer comparison:** Use `.equals()` or `Objects.equals()`, NOT `==`
2. **Null handling:** Database values can be null; always use wrapper types
3. **Checkstyle:** Builds fail on style violations
4. **Coverage:** Builds fail below thresholds (use `-Dcoverage.haltOnFailure=false` to override)
5. **Import order:** Must be lexicographical (Checkstyle enforces)

## Files Reference

- `CODE_STYLE.md` - Detailed coding standards
- `CONTRIBUTING.md` - Contribution guidelines
- `checkstyle/config.xml` - Checkstyle rules
- `qqq-middleware-javalin/ISOLATED_SPA_ROUTE_PROVIDER.md` - SPA documentation
