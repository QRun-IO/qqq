# TODO

## Active Work: Issue #334 - OAuth2 Session Security Keys

### Completed
- [x] Add customizer fields to OAuth2AuthenticationModule
- [x] Add `getCustomizer()` method with memoization
- [x] Add `finalCustomizeSession()` helper method
- [x] Modify `createSessionFromToken()` to call `customizeSession()`
- [x] Add `finalCustomizeSession()` calls in `createSession()` flows
- [x] Add unit tests for customizer support
- [x] Add WireMock dependency for integration testing
- [x] Add integration tests with mock OIDC server
- [x] Create PR #337
- [x] Create GitHub issue #336 for Phase 2 QBit

### Pending
- [ ] PR #337 review and merge
- [ ] (Future) Implement #336 QSessionStoreInterface QBit

---

## Background: License Migration (Paused)

See `docs/PLAN-license-migration.md` for details.

### Completed
- [x] Push LICENSE, NOTICE, README to all 25 repos
- [x] Delete old LICENSE.txt files from qqq and qqq-android
- [x] Update `checkstyle/license.txt` with new simplified header

### Pending
- [ ] Update Java source file headers (~5,747 files)
- [ ] Update pom.xml `<licenses>` sections (~20 files)
- [ ] Update README.md AGPL references (~15 files)
- [ ] Update package.json license field (qqq-frontend-core)
- [ ] Run checkstyle to verify headers

---

## Notes

### OAuth2 Customizer Usage
```java
// In your customizer implementation
public void customizeSession(QInstance qInstance, QSession qSession, Map<String, Object> context)
{
   JSONObject jwtPayload = (JSONObject) context.get("jwtPayloadJsonObject");
   // Extract claims and set security keys
   qSession.withSecurityKeyValue("myKey", jwtPayload.getString("customClaim"));
}
```

### Related Issues/PRs
- Issue #334 - Session Security Keys not persisted (FIXED)
- PR #337 - OAuth2 customizer support (OPEN)
- Issue #336 - QSessionStoreInterface QBit (FUTURE)
