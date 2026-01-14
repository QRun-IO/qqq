# TODO

## Completed (2026-01-14)

### SpotBugs + PMD Implementation - DONE
- [x] Create implementation plan (`docs/PLAN-spotbugs-pmd.md`)
- [x] Add SpotBugs Maven plugin to parent pom.xml
- [x] Create SpotBugs exclusion filter (`spotbugs/exclude-filter.xml`)
- [x] Add PMD Maven plugin to parent pom.xml
- [x] Create PMD ruleset (`pmd/ruleset.xml`)
- [x] Test local Maven execution
- [x] Add static analysis commands to qqq-orb
- [x] Add static analysis job to qqq-orb
- [x] Fix orb to use `mvn install` for multi-module dependency resolution
- [x] Publish qqq-orb@0.6.0
- [x] Update CircleCI config to use @0.6.0
- [x] Merge PR #369 to develop
- [x] Generate `spotbugs-summary.csv` with full analysis breakdown

---

## Pending Work

### Future: Address SpotBugs Findings
Priority order for tackling issues:

**Immediate (Security)**
- [ ] `HARD_CODE_PASSWORD` (1) - Find and remove hardcoded password
- [ ] `SQL_INJECTION_JDBC` (13) - Verify all use parameterized queries

**High Value (Concurrency)**
- [ ] `ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD` (23) - Fix static field mutations
- [ ] `SING_SINGLETON_GETTER_NOT_SYNCHRONIZED` (12) - Fix race conditions

**Easy Wins (Code Quality)**
- [ ] `DLS_DEAD_LOCAL_STORE` (17) - Remove unused local assignments
- [ ] `WMI_WRONG_MAP_ITERATOR` (14) - Use entrySet() instead of keySet()+get()
- [ ] `DM_DEFAULT_ENCODING` (22) - Specify UTF-8 explicitly

**Low Priority (QQQ Patterns - Mostly False Positives)**
- [ ] `EI_EXPOSE_REP` (554) - Intentional for fluent builders
- [ ] `CT_CONSTRUCTOR_THROW` (52) - Review case by case
- [ ] `SE_BAD_FIELD` (45) - QQQ rarely serializes

### Future: Tune Static Analysis
- [ ] Review remaining false positives and add exclusions
- [ ] Consider enabling `failOnError` for security rules only

### Background: License Migration (Paused)
See `docs/PLAN-license-migration.md` for details.

---

## Quick Reference

### Local Static Analysis Commands
```bash
# SpotBugs only
mvn spotbugs:check -DskipTests

# PMD only
mvn pmd:check -DskipTests

# Both with failure on issues
mvn verify -Dspotbugs.failOnError=true -Dpmd.failOnViolation=true

# Single module
mvn spotbugs:check pmd:check -pl qqq-backend-core -DskipTests

# View SpotBugs GUI
mvn spotbugs:gui -pl qqq-backend-core
```

### SpotBugs Summary
See `spotbugs-summary.csv` for full breakdown of 1,626 findings by bug type.
