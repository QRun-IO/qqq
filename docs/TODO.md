# TODO

## Active Work

### Pluggable Audit Handlers (PR #356)
- [x] Design pluggable audit handler system
- [x] Create handler interfaces (DML + Processed)
- [x] Create metadata and executor classes
- [x] Modify QInstance, DMLAuditAction, AuditAction
- [x] Add unit tests (7 tests)
- [x] Add integration tests (6 tests)
- [x] Rebase onto 0.40.0-SNAPSHOT
- [x] Create PR #356
- [ ] PR review and merge

### Post-Merge: WORM QBit
- [ ] Create `qbit-worm-audit` repository
- [ ] Implement WormAuditQBitProducer
- [ ] Implement WormDMLAuditHandler
- [ ] Add WORM backend integration

---

## Recently Completed (2026-01-10)

### Security Vulnerability Remediation
- [x] PR #343 - Jetty, WireMock, commons-lang fixes (Merged)
- [x] PR #344 - mysql-connector-j, protobuf-java fixes (Merged)

### Version Transition
- [x] Branch `release/0.36.0` created for RC
- [x] `develop` bumped to 0.40.0-SNAPSHOT

---

## Pending Work

### Remaining Security Alerts
- [ ] guava upgrade (4 LOW) - future PR
- [ ] jetty-http (5 MEDIUM) - deferred to Javalin 7.x

### Future: Issue #336 - QSessionStoreInterface QBit
- [ ] Design pluggable session store interface
- [ ] Implement default in-memory store
- [ ] Add Redis/database store options

### Background: License Migration (Paused)
- Transition from AGPL/Kingsrook to Apache 2.0/QRun-IO
- See `docs/PLAN-license-migration.md` for details

---

## Related Links

- [PR #356 - Audit Handlers](https://github.com/QRun-IO/qqq/pull/356)
- [PR #343](https://github.com/QRun-IO/qqq/pull/343)
- [PR #344](https://github.com/QRun-IO/qqq/pull/344)
- [Discussion #340 - 0.36.0 Summary](https://github.com/orgs/QRun-IO/discussions/340)
