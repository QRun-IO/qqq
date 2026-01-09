# TODO

## Recently Completed (2026-01-09)

### PR Merge Task
- [x] Create merge plan (`docs/PLAN-pr-merge-order.md`)
- [x] Merge PR #321 - child-table personalization fix
- [x] Merge PR #320 - bulk edit query optimization
- [x] Merge PR #335 - PostgreSQL timestamp/identifier fixes
- [x] Merge PR #337 - OAuth2 customizer support
- [x] Merge PR #280 - virtual fields and alternative sections
- [x] Post-merge validation (189 tests)
- [x] Create Daily Build Log (Discussion #340)

### Issue #334 - OAuth2 Session Security Keys
- [x] Add customizer support to OAuth2AuthenticationModule
- [x] Add unit tests and integration tests
- [x] Create PR #337 (MERGED)
- [x] Create GitHub issue #336 for Phase 2 QBit

---

## Pending Work

### Future: Issue #336 - QSessionStoreInterface QBit
- [ ] Design pluggable session store interface
- [ ] Implement default in-memory store
- [ ] Add Redis/database store options
- [ ] Documentation

### Background: License Migration (Paused)
See `docs/PLAN-license-migration.md` for details.

- [x] Push LICENSE, NOTICE, README to all 25 repos
- [x] Delete old LICENSE.txt files
- [x] Update `checkstyle/license.txt` header
- [ ] Update Java source file headers (~5,747 files)
- [ ] Update pom.xml `<licenses>` sections (~20 files)
- [ ] Update README.md AGPL references (~15 files)
- [ ] Update package.json license field (qqq-frontend-core)
- [ ] Run checkstyle to verify headers

---

## Notes

### New Features Available (v0.36.0-SNAPSHOT)

**Virtual Fields:**
```java
new QTableMetaData()
   .withVirtualField(new QVirtualFieldMetaData("computed")
      .withType(QFieldType.STRING));
```

**Alternative Sections:**
```java
new QFieldSection()
   .withAlternative(QFieldSectionAlternativeType.MOBILE,
      new QFieldSection().withFields(List.of("name")));
```

**OAuth2 Customizer:**
```java
new OAuth2AuthenticationModule()
   .withCustomizer(new QCodeReference(MyCustomizer.class));
```

### Related Links
- [Discussion #340 - Daily Build Log](https://github.com/orgs/QRun-IO/discussions/340)
- [Issue #336 - QSessionStoreInterface](https://github.com/Kingsrook/qqq/issues/336)
- [PR #280](https://github.com/Kingsrook/qqq/pull/280), [#320](https://github.com/Kingsrook/qqq/pull/320), [#321](https://github.com/Kingsrook/qqq/pull/321), [#335](https://github.com/Kingsrook/qqq/pull/335), [#337](https://github.com/Kingsrook/qqq/pull/337)
