# TODO

## Completed (2026-01-18)

### PR #373: OAuth2 Enhancements
- [x] Issue #372: Pass accessToken and idToken to customizer
- [x] Issue #374: Expose OAuth2 scopes in API response
- [x] Issue #375: Logout endpoint and session identity validation
- [x] Fix: Prefer OIDC `sub` over `email` for user identity
- [x] Code quality fixes from GitHub recommendations
- [x] Update PR with full scope

**Status:** PR #373 ready for review/merge

---

## Pending Work

### PR #373 Follow-up
- [ ] Await PR review
- [ ] Merge when approved

### Future: Address SpotBugs Findings
See `spotbugs-summary.csv` for full breakdown.

**Immediate (Security)**
- [ ] `HARD_CODE_PASSWORD` (1) - Find and remove
- [ ] `SQL_INJECTION_JDBC` (13) - Verify parameterized queries

**High Value (Concurrency)**
- [ ] `ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD` (23)
- [ ] `SING_SINGLETON_GETTER_NOT_SYNCHRONIZED` (12)

**Easy Wins**
- [ ] `DLS_DEAD_LOCAL_STORE` (17)
- [ ] `WMI_WRONG_MAP_ITERATOR` (14)
