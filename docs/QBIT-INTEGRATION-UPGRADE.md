# Adopting the QBit integration APIs

These changes supply Core contracts used by Scripting and the Collaboration attachment lifecycle. Core does not embed an untrusted-code engine, file store or document renderer.

## Schema preparation

Before upgrading a SQL-backed application that installs the standard script tables, add a nullable `VARCHAR(32)` column for the `script.executionMode` field. With the default snake-case backend mapping this is `script.execution_mode`. Use the application's actual table/field mappings and migration mechanism. Existing null values retain trusted legacy execution behavior; untrusted source must be created through the Scripting isolation authority, never through the legacy script executor.

Applications that install the Jobs tables must also apply the three nullable Jobs columns described in [transactional Jobs](transactional-jobs/README.md), even if strict transactional admission remains disabled. The provider-specific additive scripts in that directory are migration examples, not startup DDL. Strict admission additionally needs the guard row, supported storage settings and a host-controlled process revision.

## Compatibility

- Existing table pre-update customizers retain their late validation phase. Opting into `BEFORE_FIELD_VALIDATIONS` allows scripts to provide values before native validation, while preserving authorized record identities and field permissions.
- Isolated scripts cannot execute through the legacy JVM script path. The marker is server-controlled, monotonic and checked across ordinary writes and restore paths.
- A required transactional listener poisons its transaction on failure. A caller cannot catch the failure and commit partial work. Ordinary post hooks are not after-commit hooks.
- Configured record versions advance on ordinary updates. Clients should use the returned version for the next expected-version guard. Realtime notifications include the changed version field. Timestamp versions can advance by one millisecond beyond the wall clock to distinguish successive writes.
- JDBC transactions expose rollback callbacks for resource cleanup. Marking a transaction rollback-only retains those callbacks until confirmed rollback. A failed physical commit discards them because its outcome may be uncertain; cleanup must not delete potentially committed files.
- Dashboard PDF previews need `blob:` in CSP `connect-src` to read locally verified bytes. The Javalin default and matching standalone Next UI policy supply it; hosts replacing the complete policy must preserve that source. This does not authorize arbitrary authenticated API destinations.
- Backends without confirmed rollback callbacks refuse that capability. Backend shutdown hooks run after application service cleanup.

## Local verification

Use Java 21, the Maven wrapper and Docker for the disposable MySQL/PostgreSQL fixtures:

```shell
./mvnw -pl qqq-backend-module-rdbms,qqq-middleware-javalin -am \
  -DmysqlTransactionalJobsTests=true -DpostgresTransactionalJobsTests=true install
```

On Windows, add `-Dexec.skip=true` to omit the optional shell-only coverage summary. This does not disable tests, Checkstyle or JaCoCo's coverage gate. `install` writes local Maven artifacts; it does not publish a release. Downstream verification must use these exact Core artifacts rather than the older public RC.21 APIs.
