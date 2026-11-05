# QQQ Core — QBit integration status

Updated: 2026-11-05. Release order is Core, Collaboration, then Next UI.

The integration and original release corrections are merged into `develop` by PR #1343 (`7825261ab722bbf558cc52a7fa096917713dd50f`) and promoted to `release/4.1` by PR #1345 (`1bef44cf98b1d552872fcfd38a2e618475e41b48`). Full CI run 4482 passed before merging. Snapshot run 4515 published that develop source as `4.1.0-20261105.105935-4515`; immutable `4.1.0-RC.22` publication remains incomplete.

Those corrections guard cleanup when no QInstance is available, add `script.execution_mode` to the sample schema, disable inferred version advancement in frozen-timestamp fixtures, and explicitly complete expired-lease recovery. Full CI passed 5,717 reactor tests and the sample acceptance checks. The expiry fixture still requires `NEEDS_REVIEW` with cleared ownership; other verifier mutations require `FAILED`.

Release run 4629 passed its static, ESB, bootstrap, sample, multi-node, consumer and strict coverage checks. Its first verification attempt encountered a native MongoDB bind failure. After all seven unchanged Mongo lifecycle checks passed on fresh containers, a normal failed-job-only retry preserved the successful jobs. That retry passed all 122 Mongo tests but exposed a transactional verifier fixture timeout in the token-mutation case.

The focused correction keeps the file-backed H2 database open for the verifier worker's lifetime and closes the connection after runtime shutdown. It preserves synchronous writes, independent worker connections, every assertion, the existing timeouts, and the separate cold-reopen tests. The unchanged focused case took 13.77 seconds locally; the corrected case passed in 4.84 seconds. All 30 relevant tests passed: the full 28-case H2 class and the affected case on disposable MySQL and PostgreSQL, with zero failures, errors or skips. The configured Checkstyle validation reported zero violations. Normal PR and release checks remain required. No immutable release is claimed.

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
