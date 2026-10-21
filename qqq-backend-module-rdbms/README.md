# QQQ Backend Module - RDBMS

Backend module for relational databases via JDBC. Base module for database-specific implementations.

## Features

- JDBC database connectivity
- Connection pooling (C3P0)
- Transaction management
- Query building and execution
- Batch operations

## Usage

```java
RDBMSBackendMetaData backend = new RDBMSBackendMetaData()
   .withName("main-db")
   .withJdbcUrl("jdbc:mysql://localhost:3306/myapp")
   .withUsername("user")
   .withPassword("password");

QTableMetaData table = new QTableMetaData()
   .withName("users")
   .withBackendName("main-db")
   .withPrimaryKeyField("id");
```

## Connection pooling

By default an RDBMS backend opens a new JDBC connection for each operation (`SimpleConnectionProvider`). That is fine for tests and small tools. For a server, use the C3P0 pool:

```java
backend
   .withConnectionProvider(new QCodeReference(C3P0PooledConnectionProvider.class))
   .withConnectionPoolSettings(new ConnectionPoolSettings()
      .withMinPoolSize(2)
      .withMaxPoolSize(15)                      // per process
      .withCheckoutTimeoutSeconds(30)           // the default when unset; C3P0 itself would wait forever
      .withMaxIdleTimeExcessConnectionsSeconds(300)
      .withMaxConnectionAgeSeconds(1800)
      .withLeakDetectionThresholdSeconds(300)   // log connections held longer, with the checkout stack trace
      .withTestConnectionOnCheckout(true));
```

Every setting is optional. With none set, C3P0's defaults apply (3 to 15 connections) plus the 30s checkout bound. That suits a single small node: one process uses at most 15 connections, well under PostgreSQL's default `max_connections` of 100. When checkouts time out, the `SQLException` names the backend and the pool state.

Connections are reset when they return to the pool. Uncommitted work is rolled back, and autoCommit, readOnly and isolation return to their defaults. QQQ only uses transaction-scoped server state (`SET TRANSACTION`, `SET LOCAL`).

The backend's health check reports the pool gauges. It is DEGRADED while threads wait for a checkout or a leak is suspected, and with `qqq-middleware-health` the gauges are exported on `/metrics` as `qqq_health_check_metric{metric="pool_*"}`. Call `ConnectionManager.closeConnectionProviders()` at the end of shutdown, after the work that uses the database has stopped.

### Behind PgBouncer (optional)

Several nodes can share a PgBouncer in **transaction** mode, which bounds server connections however many app processes run. Nothing in QQQ needs session state, so the only change is the JDBC URL. Add `prepareThreshold=0`, which PgBouncer 1.21+ with `max_prepared_statements` makes optional. An `ApplicationName` helps `pg_stat_activity` show who is connected:

```java
backend.withJdbcUrl("jdbc:postgresql://pgbouncer:5432/app?prepareThreshold=0&ApplicationName=app-web");
```

Keep migrations (Liquibase, `pg_dump`) and anything that uses session-level features (`pg_advisory_lock`, `LISTEN`, session `SET`, temp tables) on a direct connection. Use `pg_advisory_xact_lock` inside a transaction instead of session locks. Without a pooler, point the same settings straight at PostgreSQL; nothing else changes.

## Database-Specific Modules

- `qqq-backend-module-postgres` - PostgreSQL
- `qqq-backend-module-sqlite` - SQLite

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
