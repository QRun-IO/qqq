# Quickstart lifecycle acceptance (#650)

Run `python3 -B qqq-sample-project/test_quickstart_launcher.py -v` from the repository root. The existing CircleCI sample acceptance job discovers this suite automatically. It executes the actual `quickstart.sh` with isolated external-command fixtures and an owned process on port 8000; keep that port free. These are launcher regression tests, not proof of Maven downloads, a real Java application, browser features, data reset or the 90-second target.

| Contract | Automated launcher evidence |
| --- | --- |
| Help and invalid arguments without prerequisites | `test_help_and_usage_need_no_installed_prerequisites` |
| Missing Git, Java, javac, curl or unzip | `test_missing_prerequisites_are_actionable_and_do_not_clone` |
| Old Java or invalid JAVA_HOME | `test_old_java_and_wrong_java_home_are_rejected` |
| Invalid dashboard selection | `test_invalid_frontend_is_rejected_before_downloading` |
| Preserve an occupied port | `test_occupied_port_is_preserved_and_reported` |
| Preserve an existing destination and unrelated contents | `test_existing_destination_and_its_contents_are_preserved` |
| Clone failure stops before building | `test_clone_failure_is_nonzero_and_does_not_start_a_build` |
| Compile failure retains logs and local edits | `test_compile_failure_keeps_diagnostics_and_source_for_retry` |
| SIGINT/SIGTERM, normal exit and application failure; restart; path with spaces; rejected concurrent launch; Next/Material choice; no orphan process/listener | `test_signal_cleanup_and_restart_preserve_edits_and_frontend_choice` |

Public acceptance remains open under [#650](https://github.com/QRun-IO/qqq/issues/650): use the exact published website command in a clean environment, then verify cold/warm startup, recovery from interrupted downloads/builds, Java edit/recompile, seeded screens and relationships, data reset, browser/platform coverage and timing. Reuse the Next feature matrix under [#649](https://github.com/QRun-IO/qqq/issues/649), accounting explicitly for approved RC limitations. Its specialized acceptance server is not the public launcher; results from that server alone cannot certify this journey. `bootstrap_acceptance.py --stage published` separately verifies public Maven/BOM consumption. Neither these fixtures nor historical 4.0 timings close the public acceptance gate.
