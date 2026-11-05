# QBit integration remaining limitations

Updated: 2026-11-05.

- Core's integration passed full CI and its snapshot is published. Immutable release run 4629 is blocked by a transactional verifier fixture timeout; the focused database-lifecycle correction and its qualification are described in [current status](QBIT-RELEASE-STATUS.md). Compatible immutable Core and Collaboration artifacts remain required before adoption by the other QBits.
- Applications must migrate the script execution marker and additive Jobs columns before deploying the new metadata. See [upgrade requirements](QBIT-INTEGRATION-UPGRADE.md).
- The pre-existing optional shell coverage-summary command fails under native Windows Maven. Use `-Dexec.skip=true`; JaCoCo reports and enforcement remain enabled.
- Existing disabled tests and advisory SpotBugs/PMD findings remain. The integration verification is not a claim to have corrected every historical issue across all QQQ modules.
- Untrusted-script containment, renderer deployment, physical printing and production throughput belong to the corresponding QBits and configured workers. Core regression tests do not qualify those runtimes.
