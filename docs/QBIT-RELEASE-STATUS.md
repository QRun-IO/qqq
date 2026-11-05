# QQQ Core — QBit integration status

Updated: 2026-11-05. Release order is Core, Collaboration, then Next UI.

PRs #1341 and #1342 are merged into `develop` at `5570df527ba84e14d7b0856c25dda693ded43aed`. Bootstrap passes in release run 4337. Publication remains incomplete: that run exposed a missing-context cleanup exception, a sample script schema omission, stale Replace snapshot fixtures, and an expiry fixture racing native job recovery. CI run 4338 has the same sample failures; its reactor verification passed with 5,717 executed tests. Native run 4339 passed.

The follow-up guards cleanup when no QInstance is available, adds `script.execution_mode` to the sample acceptance schema, disables inferred version advancement in fixtures that deliberately freeze timestamps, and explicitly completes expired-lease recovery after verifier refusal. All complete-row and permission assertions remain in place. The expiry fixture requires `NEEDS_REVIEW` with cleared ownership; the other verifier mutations still require `FAILED`. Focused verification passed: 38 Core/RDBMS tests and all 106 affected sample tests across focused runs, with no remaining failures or skips. The additional admission fix was followed by fresh Core/RDBMS verification and all 38 CRUD sample tests; the unchanged 14 script and 54 Replace cases already passed. Checkstyle and staged secret scanning passed. Full CI and immutable publication remain outstanding.

## Implemented contracts

This branch integrates server-controlled script isolation, early record hooks, transactional durable-job enlistment, source guards, atomic expected-version checks and confirmed JDBC rollback callbacks. It preserves the existing standard QQQ action and UI API boundaries. Core does not execute untrusted TypeScript itself.

The resume pass corrected rollback-only handling so a failed required operation retains resource compensation until confirmed rollback. A regression test demonstrated the failure before the fix. A second test covers a lost acknowledgement after a successful physical commit: cleanup must not run in that uncertain case. Existing tests were reconciled with advancing record versions and concealed job ownership. Static HTTP fixtures now use LF line endings on all platforms. Downstream browser verification also found that the dashboard policy blocked reading local, receipt-verified PDF blobs; `connect-src` now explicitly permits `blob:` while the preview loader still validates the same-origin source. The full Javalin module passed all 853 tests and style/coverage gates again after this correction.

## Earlier local verification

- Backend Core: 3,288 tests, zero failures/errors, 11 existing skips; Checkstyle and coverage gate passed.
- RDBMS: 494 tests, zero failures/errors, 5 existing skips; includes 29 MySQL, 43 PostgreSQL and 11 transaction callback tests. Checkstyle and coverage gate passed.
- JavaScript support: 12 tests passed.
- OpenAPI: module build passed.
- Javalin: 853 tests, zero failures/errors/skips; Checkstyle and coverage gate passed.
- Filesystem: 144 tests, zero failures/errors, 4 existing skips; Checkstyle and coverage gate passed.
- Native PostgreSQL: 152 tests, zero failures/errors, 2 existing skips; includes worker-kill, bulk-load recovery and external-effect fault tests. Checkstyle and coverage gate passed. The disposable Jobs fixture was updated with the three additive columns.

The affected dependency set passed 4,943 reported tests with 22 existing skips and no failures/errors before the current release follow-up. These results do not supersede the current CI failures above or prove publication.

The build uses Java 21, disposable database services and normal `4.1.0-SNAPSHOT` local Maven coordinates. The Windows invocation omits only the optional shell coverage-summary execution (`-Dexec.skip=true`); it does not skip tests or coverage enforcement. SpotBugs/PMD are advisory under the repository's existing configuration; a successful build does not mean all historical static-analysis findings are resolved.

## Adoption

Read [upgrade requirements](QBIT-INTEGRATION-UPGRADE.md) before changing an application. The standard script and Jobs metadata require additive schema preparation. See [remaining limitations](QBIT-KNOWN-ISSUES.md) for the boundary between this source verification and release/deployment.

The reconciled source started from canonical develop 9bf3df6ab10eb1f37d23944656c19a631943dcfc and reviewed Website-Backend b6fb23825a87412397dec703b9617148ae7eb601. Earlier private-cohort results do not replace the current verification above.
