# QQQ Core — QBit integration status

Updated: 2026-11-04. The user resumed work on Core, Collaboration and Next UI in that order.

Core PR #1341 is merged into `develop` at `ab7d4b5d62079febee78fd9d9c9ef33deb56b06d`. Release run 4273 failed in sample bootstrap verification; publication is not complete. The failing test assumed that same-named backends still shared a connection provider, but providers now include database connection settings in their cache identity. The follow-up regression verifies that the sample database is seeded while the other database retains exactly its original row. The runtime reset guard is unchanged. All eight targeted sample/bootstrap Java tests and 38 bootstrap/feature-evidence Python tests pass locally; the release pipeline must still pass on the follow-up commit.

## Implemented contracts

This branch integrates server-controlled script isolation, early record hooks, transactional durable-job enlistment, source guards, atomic expected-version checks and confirmed JDBC rollback callbacks. It preserves the existing standard QQQ action and UI API boundaries. Core does not execute untrusted TypeScript itself.

The resume pass corrected rollback-only handling so a failed required operation retains resource compensation until confirmed rollback. A regression test demonstrated the failure before the fix. A second test covers a lost acknowledgement after a successful physical commit: cleanup must not run in that uncertain case. Existing tests were reconciled with advancing record versions and concealed job ownership. Static HTTP fixtures now use LF line endings on all platforms. Downstream browser verification also found that the dashboard policy blocked reading local, receipt-verified PDF blobs; `connect-src` now explicitly permits `blob:` while the preview loader still validates the same-origin source. The full Javalin module passed all 853 tests and style/coverage gates again after this correction.

## Verification

- Backend Core: 3,288 tests, zero failures/errors, 11 existing skips; Checkstyle and coverage gate passed.
- RDBMS: 494 tests, zero failures/errors, 5 existing skips; includes 29 MySQL, 43 PostgreSQL and 11 transaction callback tests. Checkstyle and coverage gate passed.
- JavaScript support: 12 tests passed.
- OpenAPI: module build passed.
- Javalin: 853 tests, zero failures/errors/skips; Checkstyle and coverage gate passed.
- Filesystem: 144 tests, zero failures/errors, 4 existing skips; Checkstyle and coverage gate passed.
- Native PostgreSQL: 152 tests, zero failures/errors, 2 existing skips; includes worker-kill, bulk-load recovery and external-effect fault tests. Checkstyle and coverage gate passed. The disposable Jobs fixture was updated with the three additive columns.

The affected dependency set passed 4,943 reported tests with 22 existing skips and no failures/errors. Core implementation and local verification are complete for the next Collaboration integration step.

The build uses Java 21, disposable database services and normal `4.1.0-SNAPSHOT` local Maven coordinates. The Windows invocation omits only the optional shell coverage-summary execution (`-Dexec.skip=true`); it does not skip tests or coverage enforcement. SpotBugs/PMD are advisory under the repository's existing configuration; a successful build does not mean all historical static-analysis findings are resolved.

## Adoption

Read [upgrade requirements](QBIT-INTEGRATION-UPGRADE.md) before changing an application. The standard script and Jobs metadata require additive schema preparation. See [remaining limitations](QBIT-KNOWN-ISSUES.md) for the boundary between this source verification and release/deployment.

The reconciled source started from canonical develop 9bf3df6ab10eb1f37d23944656c19a631943dcfc and reviewed Website-Backend b6fb23825a87412397dec703b9617148ae7eb601. Earlier private-cohort results do not replace the current verification above.
