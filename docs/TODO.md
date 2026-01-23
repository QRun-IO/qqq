# TODO

## Completed (2026-01-23)

### QSessionStore QBit Integration (PR #381)
- [x] Create `QSessionStoreHelper.java` with reflection-based QBit bridge
- [x] Add `sessionStoreEnabled` to `QAuthenticationMetaData`
- [x] Create `QSessionStoreHelperTest.java` for non-QBit classpath tests
- [x] Fix OAuth2 test pollution (add `clearOIDCProviderMetadataCache()`)
- [x] Create PR #381 and pass CI

---

## Completed (2026-01-21)

### PR Reviews - Multi-App Tables & Custom Menus
- [x] Review and merge PR #377 (backend multi-app tables)
- [x] Review and merge PR #378 (backend custom menus)
- [x] Review and merge PR #37 (frontend custom menus)
- [x] Review and merge PR #38 (frontend multi-app tables)

All merged to `release/0.36.0` on 2026-01-21.

---

## Completed (2026-01-14)

### SpotBugs + PMD Implementation
- [x] Add SpotBugs and PMD to Maven build
- [x] Create exclusion filters for QQQ patterns
- [x] Add CI job via qqq-orb@0.6.0
- [x] Merge PR #369

---

## In Progress

### QSessionStore QBit (Separate Repo)
- [x] Create `qbit-session-store` repository
- [x] Implement InMemory provider
- [x] Implement TableBased provider
- [x] Implement Redis provider
- [x] Set up CircleCI with qqq-orb
- [ ] Integration tests with OAuth2AuthenticationModule + session store on classpath
- [ ] Documentation and examples

---

## Future Work

### Address SpotBugs Findings

**Security (Immediate)**
- [ ] `HARD_CODE_PASSWORD` (1) - Find and remove
- [ ] `SQL_INJECTION_JDBC` (13) - Verify parameterized queries

**Concurrency (High Value)**
- [ ] `ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD` (23)
- [ ] `SING_SINGLETON_GETTER_NOT_SYNCHRONIZED` (12)

**Code Quality (Easy Wins)**
- [ ] `DLS_DEAD_LOCAL_STORE` (17) - Remove unused assignments
- [ ] `WMI_WRONG_MAP_ITERATOR` (14) - Use entrySet()
- [ ] `DM_DEFAULT_ENCODING` (22) - Specify UTF-8

### Tune Static Analysis
- [ ] Review false positives and add exclusions
- [ ] Consider enabling `failOnError` for security rules only

---

## Quick Reference

```bash
# SpotBugs
mvn spotbugs:check -DskipTests

# PMD
mvn pmd:check -DskipTests

# Single module with GUI
mvn spotbugs:gui -pl qqq-backend-core
```
