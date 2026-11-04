# QBit integration remaining limitations

Updated: 2026-11-04.

- Core and its affected storage/middleware checks have passed. Collaboration and Next UI acceptance follow next. A coherent public Core/Collaboration/Scripting/Record Files release has not been published from this branch.
- Applications must migrate the script execution marker and additive Jobs columns before deploying the new metadata. See [upgrade requirements](QBIT-INTEGRATION-UPGRADE.md).
- The pre-existing optional shell coverage-summary command fails under native Windows Maven. Use `-Dexec.skip=true`; JaCoCo reports and enforcement remain enabled.
- Existing disabled tests and advisory SpotBugs/PMD findings remain. The integration verification is not a claim to have corrected every historical issue across all QQQ modules.
- Untrusted-script containment, renderer deployment, physical printing and production throughput belong to the corresponding QBits and configured workers. Core regression tests do not qualify those runtimes.
