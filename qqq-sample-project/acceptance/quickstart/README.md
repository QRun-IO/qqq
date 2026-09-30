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

## Real launcher lifecycle runner

`run-public.mjs` uses an existing project's installed `@playwright/test` and browser; these are acceptance tooling, not application prerequisites. It downloads the exact public launcher, verifies the cloned commit, and launches the sample in an owned directory with spaces. It checks seeded Next records and associations, create/edit/readback, browser refresh, SIGINT/SIGTERM cleanup of HTTP and embedded-broker ports, warm restart and data reset, a Java branding edit/recompile retained across starts, and explicit Material asset selection. Logs, screenshots and per-phase results stay in the new evidence directory; the original checkout is not edited.

```sh
node qqq-sample-project/acceptance/quickstart/run-public.mjs \
  --playwright-project /path/to/installed-playwright-project \
  --output /new/quickstart-evidence \
  --version 4.1.0-RC.1 --expected-sha FULL_QUICKSTART_TAG_COMMIT
```

Use `--browser firefox` or `--browser webkit` for those engines, or `--channel chrome` for installed Chrome. Add `--cold` only in a fresh environment with no Maven cache/settings, Git credentials or build/version overrides. The cold timer includes launcher/source/dependency downloads and ends when seeded Person data is visible; it requires at most 90 seconds. Browser tooling and system prerequisites are installed before timing. Record the reference environment separately. Warm runs do not establish cold timing.

For harness development only, replace `--expected-sha` with `--rehearsal-source /path/to/qqq`; this copies tracked source, uses locally installed `4.1.0-SNAPSHOT` dependencies and labels the result **SOURCE REHEARSAL**. It cannot be combined with `--cold` and is never public-release evidence. A successful lifecycle result does not close #650: the full screen/action/platform matrix, additional download/build/runtime recovery cases, public BOM provenance and Material browser feature compatibility remain separate required evidence. `releaseAcceptanceComplete` deliberately stays false.

The **Public quickstart lifecycle** GitHub workflow runs this public command in Chromium, Firefox and WebKit on Linux and macOS, retaining each result even when another matrix entry fails. Push the immutable `quickstart-4.1.0-RC.1` tag only after its Maven dependencies are public; the tag triggers acceptance against its own checked-out commit. Manual dispatch is also available once the workflow exists on the repository's default branch; supply the public candidate version and tag's full reviewed commit. It does not use `--cold`: hosted-runner timings are functional evidence, not the declared reference benchmark. Windows WSL and the broader feature/action acceptance remain explicit outstanding coverage until exercised; this workflow does not certify them.
