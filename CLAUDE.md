# CLAUDE.md - QQQ Framework Guide

## Project Overview

QQQ is a low-code application framework for engineers by QRun-IO, LLC (formerly Kingsrook). It uses metadata-driven architecture where applications are defined through configuration rather than code generation.

**Current Version:** 0.40.0-SNAPSHOT
**License:** Apache-2.0
**Java Version:** 17+

## Session Continuity

To continue from a previous session, say **"continue from last session"** and Claude will:
1. Read `docs/SESSION.md` for current context
2. Read `docs/TODO.md` for pending tasks
3. Resume work from last checkpoint

**Important:** Session state is kept in `./docs/` directory, NOT in `~/.claude/`.

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

## Static Analysis (SpotBugs + PMD)

```bash
mvn spotbugs:check -DskipTests                 # Run SpotBugs (report only)
mvn pmd:check -DskipTests                      # Run PMD (report only)
mvn spotbugs:gui -pl qqq-backend-core          # SpotBugs GUI for single module
```

**Configuration files:**
- `spotbugs/exclude-filter.xml` - Exclusions for QQQ patterns (fluent builders, singletons)
- `pmd/ruleset.xml` - Custom ruleset tuned for QQQ conventions

**CI Integration:** Static analysis runs in parallel with tests on feature branches via `qqq-orb/static_analysis` job.

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

## Key Dependencies

| Library | Version | Notes |
|---------|---------|-------|
| Log4j | 2.25.3 | Logging (security patched) |
| Jackson | 2.20.1 | JSON serialization |
| JaCoCo | 0.8.14 | Code coverage |
| Testcontainers | 2.0.3 | Integration testing |
| MongoDB Driver | 5.5.1 | Use mongo:6.0+ for tests |
| AssertJ | 3.27.6 | Test assertions |

## Files Reference

- `CODE_STYLE.md` - Detailed coding standards
- `CONTRIBUTING.md` - Contribution guidelines
- `checkstyle/config.xml` - Checkstyle rules
- `qqq-middleware-javalin/ISOLATED_SPA_ROUTE_PROVIDER.md` - SPA documentation
- `docs/SESSION.md` - Current session state for continuity
- `docs/TODO.md` - Active task tracking

## Recent Learnings

### RDBMS Identifier Quoting
- PostgreSQL lowercases unquoted identifiers (e.g., `mhOrderId` becomes `mhorderid`)
- MySQL/H2/SQLite are case-insensitive by default
- Always use `escapeIdentifier()` for column/table names in SQL generation
- The `escapeIdentifier()` method uses dialect-specific quote characters (`"` for PostgreSQL, backtick for MySQL)

### Audit System
- `AuditsMetaDataProvider.withRecordIdType(QFieldType)` configures audit table PK type
- Default is INTEGER (backwards compatible)
- STRING mode allows auditing tables with UUID/string PKs

### OAuth2 Authentication Customizer Support
- `OAuth2AuthenticationModule` now supports `QAuthenticationModuleCustomizerInterface`
- Customizers are called on both initial login AND session resume
- JWT payload is passed to `customizeSession()` via `context.get("jwtPayloadJsonObject")`
- `finalCustomizeSession()` is called after session creation for final adjustments
- This enables apps to set security keys that persist across requests
- See issue #334 and PR #337 for details
- Future: #336 proposes `QSessionStoreInterface` QBit for session persistence

### Virtual Fields (PR #280)
- `QVirtualFieldMetaData` - computed fields that don't exist in backend storage
- Defined via `QTableMetaData.withVirtualField()` or `withVirtualFields()`
- Appear in table views, record screens, and exports alongside real fields
- Computation happens at render time, not storage time

### Alternative Sections (PR #280)
- `QFieldSection.withAlternative()` - context-specific field layouts
- `QFieldSectionAlternativeType` enum for standard contexts (e.g., MOBILE)
- Custom types via `QFieldSectionAlternativeTypeInterface`
- Enables different layouts for desktop vs mobile, embedded widgets, etc.

### Bulk Edit Query Optimization (PR #320)
- Three-tier fallback prevents memory exhaustion on large datasets
- Tier 1: IN lists for small batches
- Tier 2: OR queries in pages for medium batches
- Tier 3: Record-by-record for huge batches
- `TryAnotherWayException` signals automatic fallback between tiers

### PostgreSQL Fixes (PR #335)
- `QueryManager.getInstant()` now handles TIMESTAMPTZ with timezone offsets
- Tries `OffsetDateTime.parse()` first, falls back to `LocalDateTime` parsing
- Identifier quoting fixed - column names properly escaped for case sensitivity

### Static Analysis - SpotBugs + PMD (PR #369)
- **SpotBugs 4.8.6.6** with FindSecBugs plugin for security analysis
- **PMD 7.9.0** with custom ruleset for code quality
- Report-only by default (use `-Dspotbugs.failOnError=true` to fail builds)
- CI runs `static_analysis` job in parallel with tests on feature branches
- Key exclusions: `EI_EXPOSE_REP` for fluent builders, singleton patterns
- Summary: ~70 High, ~1,550 Medium findings (many false positives for QQQ patterns)
- See `spotbugs-summary.csv` for full breakdown by bug type

### QQQ-Orb @0.6.0
- Added `static_analysis` job for SpotBugs + PMD
- Jobs run in parallel with test jobs
- Reports stored as CircleCI artifacts
- Orb repo: `/Users/james.maes/Git.Local/QRun-IO/qqq-orb/`
