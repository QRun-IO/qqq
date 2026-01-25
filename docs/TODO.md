# TODO

## Completed (2026-01-25)

### Interface + Registry Refactoring - DONE
- [x] Create `QSessionStoreProviderInterface` in core
- [x] Create `QSessionStoreRegistry` in core
- [x] Refactor `QSessionStoreHelper` to use registry
- [x] Add `loadAndTouchSession()` combined operation
- [x] Update `OAuth2AuthenticationModule`
- [x] Update tests
- [x] qbit: extend core interface
- [x] qbit: register with core on startup
- [x] qbit: add `getDefaultTtl()` to providers
- [x] qbit: add optimized `loadAndTouch()` to providers
- [x] Both CI pipelines passing
- [x] Post blog draft to discussions #382
- [x] Reply to PR #381 review comments

---

## Pending Work

### PR #381 Follow-up
- [ ] Await final review approval
- [ ] Merge when approved

### Open PRs
| PR | Description | Status |
|----|-------------|--------|
| #381 | QSessionStore Integration (Interface+Registry) | Review addressed, CI passing |
| #373 | OAuth2 customizer tokens, scopes API | Awaiting review |
| #356 | Pluggable audit handler system | Awaiting review |

### Future: Address SpotBugs Findings
See `spotbugs-summary.csv` for full breakdown.

**Security (Immediate)**
- [ ] `HARD_CODE_PASSWORD` (1)
- [ ] `SQL_INJECTION_JDBC` (13)

**Concurrency (High Value)**
- [ ] `ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD` (23)
- [ ] `SING_SINGLETON_GETTER_NOT_SYNCHRONIZED` (12)

**Code Quality (Easy Wins)**
- [ ] `DLS_DEAD_LOCAL_STORE` (17)
- [ ] `WMI_WRONG_MAP_ITERATOR` (14)

## Quick Reference

```bash
# Local Static Analysis
mvn spotbugs:check -DskipTests
mvn pmd:check -DskipTests

# Run specific test
mvn test -Dtest=QSessionStoreHelperTest
```
