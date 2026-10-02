# Next successor evidence import (#798/#790)

The seven IDs in `../../next-acceptance-crosswalk.json` retain their original contracts and historical Material provenance. James's explicit Next-only/EOL direction changes the active acceptance target to Next, not the required behavior. The ledger remains authoritative and unchanged. The first five widget positives and block rendering/layout positives have exact source-backed selectors; all composite negative requirements and the full dashboard mapping remain pending. Shared widget error tests are not automatically proof for every widget type. This importer does not resolve #849/#820 or claim that Next tested Material.

The existing checker reads native Playwright `report.json` and Next `gate.json`; it never generates JUnit. It requires first-attempt passes, exact mapped file/title/matrix ID in each of Chromium, Firefox, WebKit, mobile and tablet, complete required matrix rows (respecting the pinned Next `desktopOnly` reasons), matching native gate results/totals, and no partial, failed, skipped or flaky run. Pending mappings and pending ledger review still block the seven rows. Historical Material JUnit mappings are retained but missing historical reports do not certify or block successor coverage; an actual supplied failing compatibility result still blocks. Other rows retain their existing Maven gate.

## Approved published Next RC10 limitations

For QQQ `4.1.0-RC.1` only, James [approved the coordinated RC with retained Next limitations](https://github.com/QRun-IO/qqq/issues/798#issuecomment-5943457468). `release-deferrals.json` records the exact public source/JAR identity, seven successor contract IDs, rationale and follow-up target. The candidate workflow explicitly supplies `QQQ_ACCEPTED_NEXT_CANDIDATE=4.1.0-RC.1`; local coverage can use `--candidate-version 4.1.0-RC.1`. Both the sample and BOM must select that exact Next version. Before publication, CI also requires source revision `4.1.0-SNAPSHOT` on `release/4.1` or `release/4.1.0`, the inputs that the pinned orb turns into RC1; an existing RC revision would increment and is rejected. Changing the candidate version, artifact identity, approval or feature set rejects the exception. GA, hotfix and ordinary feature checks do not enable it. Update the candidate workflow and reviewed policy deliberately for the next pair; do not reuse RC10 acceptance for a newer Next artifact.

Accepted rows remain pending in the inventory, are listed separately in `accepted_next_features`, are excluded from verified counts, and keep `complete=false`. The native report and its missing/failed evidence remain visible. No passing receipt is fabricated or required to justify the maintainer's exception. Actual supplied failing Material compatibility tests and every other source/public gate still block. This policy identifies the selected public artifact; it does not verify downloaded bytes or replace the separately required paired runtime, public-consumer and quickstart acceptance. Existing issues #575–#580/#606 and the release-note parity/browser/visual follow-ups stay open.

## Reviewed handoff

Place the receipt bundle under `qqq-sample-project/target/next-acceptance/`. Supply the independently reviewed receipt digest at invocation, rather than committing a run-specific receipt hash into its own tested QQQ revision:

```sh
python3 -B qqq-sample-project/verify-feature-coverage.py --stage source \
  --next-receipt-sha256 "$REVIEWED_NEXT_RECEIPT_SHA256" \
  --require-feature core.widget.data_bag_viewer
python3 -B -m unittest discover -s qqq-sample-project -p 'test_*.py'
```

`--require-feature` remains repeatable. Without a reviewed digest, native evidence remains pending even if receipt files exist. `--report-only` still cannot certify completion or override a required feature. No ledger status changes happen during import.

`receipt.json` is a **QQQ-side import manifest**, not a field added to Next's native schema:

```json
{
  "schema_version": 1,
  "next_sha": "<exact tested 40-hex Git SHA matching the reviewed crosswalk>",
  "qqq_sha": "<exact current QQQ HEAD tested by every job>",
  "mode": "javalin",
  "run_url": "https://github.com/QRun-IO/qqq-frontend-next/actions/runs/<run-id>",
  "jobs": [
    {
      "name": "acceptance-chromium",
      "report": {"path": "acceptance-chromium/report.json", "sha256": "<64-hex>"},
      "gate": {"path": "acceptance-chromium/gate.json", "sha256": "<64-hex>"},
      "checkout_evidence": {"path": "acceptance-chromium/checkout.txt", "sha256": "<64-hex>"},
      "artifacts": {
        "sample": {"path": "acceptance-chromium/sample.jar", "sha256": "<64-hex>"},
        "api": {"path": "acceptance-chromium/api.jar", "sha256": "<64-hex>"},
        "next": {"path": "acceptance-chromium/next.jar", "sha256": "<64-hex>"}
      }
    }
  ]
}
```

The illustration is intentionally incomplete and cannot pass. Current Next receipts require four jobs: `acceptance-chromium` (`chromium`), `acceptance-touch` (`mobile/tablet`), `acceptance-firefox` (`firefox`), and `acceptance-webkit` (`webkit`). The historical three-job layout remains supported: `acceptance-chromium-touch` (`chromium/mobile/tablet`), `acceptance-firefox` (`firefox`), and `acceptance-webkit` (`webkit`). Use one complete layout matching the actual run; mixed layouts, missing or duplicate jobs, and a job with the wrong project set are rejected. Both layouts require all five projects. Include actual artifact bytes and sanitized checkout/build evidence for **each job**. Preserve the original native JSON bytes; the importer checks the hashes, native `config.metadata.ci.commitHash`, `gitCommit.hash` and `ci.buildHref`. A PR head and its GitHub test merge are different SHAs: the reviewer must reconcile the actual tested commit and update the source crosswalk deliberately, not relabel a receipt. Mutable `develop`/dispatch references alone are insufficient.

Copy each source file listed in the crosswalk to `sources/<repository-relative-path>` within the bundle. Its SHA256 must match the reviewed exact Next source. This includes the matrix, mapped specs, gate, browser config and workflow. No path may escape the bundle, including through symlinks. The three pinned Next real-service exclusions are not waivers for the seven retained QQQ requirements.

The receipt hash is an **integrity anchor, not a cryptographic build attestation**. Independent review must establish that the recorded QQQ/Next revisions and sample/API/static-export artifacts actually ran together, using the per-job checkout/build evidence and original artifact provenance. The importer can verify bytes and native metadata; it cannot infer which backend ran from Playwright JSON alone. Never fill missing provenance with the intended revision, a locally rebuilt artifact, or a guessed hash. Missing source/runtime proof remains pending. Full main composition/CI, public-consumer/broker gates and separately owned default-entry-point requirements remain independent release gates.

## Outstanding behavior mapping

| Retained issue | Source-backed entry points | Still required before promotion |
| --- | --- | --- |
| #575 | WID-028, data bag version selection/content and empty state | Applicable malformed data, invalid reference, renderer failure and denied-source cases |
| #576 | WID-029, saved pivot definition and non-pivot state | Applicable malformed/reference/failure/denial cases |
| #577 | WID-030, saved filters/sort/columns and no-filter state | Applicable malformed/reference/failure/denial cases; do not inflate this contract into all editor parity |
| #578 | WID-031, typed read-only row rendering | Empty/malformed/reference/failure/denial cases |
| #579 | WID-032, current revision/file content and switching | Empty/malformed/reference/failure/denial cases; viewer proof does not resolve script execution scope |
| #580 | WID-057/058, block values/assets/styles/input and nested layouts/links/tooltips | Missing asset, invalid slot/value, empty children, action denial, unsafe content; preserve #556 disposition |
| #606 | Candidate NAV/QRY/REC/PRC/WID/RPT/SEC/INT areas | Exact original navigation/query/CRUD/associations/bulk/process/widget/responsive and negative-case mapping; real-provider vs fixture auth claims remain distinct |

The aggregate negative strings remain intact in the crosswalk. Partial empty-state evidence does not close a compound requirement. Complete each requirement with applicable exact test selectors and source hashes, with independent semantic review; a matrix label or passing count cannot prove the underlying behavior. The fixtures in `test_next_acceptance.py` are explicitly synthetic receipt/parser regressions, not release evidence or new browser passes.

## CircleCI receipt handoff

`sample_acceptance` stages the reviewed bundle **after** Maven clean/verify and the bootstrap workspace attachment. It runs the complete `test_*.py` discovery suite, including the importer and CI staging regressions. Existing bootstrap, starter/application, broker, static-analysis and publication dependencies remain intact. The staging diagnostic is stored as `target/next-acceptance-stage.json`; it contains hashes/status only, never the download URL or credentials. Report-only feature builds still list gaps and cannot certify completion. Candidate, release-tag and hotfix strict checks require valid staging and still require every existing source-stage acceptance gate.

The operator supplies these string [CircleCI pipeline parameters](https://circleci.com/docs/guides/orchestrate/pipeline-variables/) when triggering the exact candidate branch, release tag or hotfix pipeline:

| Parameter | Required value |
| --- | --- |
| `next_acceptance_bundle_url` | HTTPS location of the reviewed bundle ZIP, reachable by the runner; use only a nonsecret URL here |
| `next_acceptance_bundle_sha256` | Independently reviewed SHA256 of the exact ZIP bytes |
| `next_acceptance_receipt_sha256` | Independently reviewed SHA256 of its root `receipt.json` |

For a private signed URL, leave the URL parameter empty and supply `QQQ_NEXT_BUNDLE_URL` through an existing secure project/context environment configuration. **Pipeline parameters are not secret storage.** This change does not create a context, publish artifacts, choose a hosting service, or add bearer/basic authentication. The supplied HTTPS location must already be accessible, including any signed query needed for access. URL credentials/userinfo and HTTP downgrade redirects are rejected; neither URL nor exception text is logged. The URL is read as environment data, never interpolated into shell commands. Both digest parameters remain explicit reviewed inputs; the job does not derive its own trust anchor from the download.

Package the documented receipt bundle with `receipt.json`, `sources/`, and its referenced per-job files at ZIP root, **not** inside a surrounding directory. Include actual sample/API/Next runtime JAR bytes and the reviewed checkout/build evidence, not locally reconstructed substitutes. Use regular files/directories only: no symlinks, devices, duplicate names, encrypted members or escaping paths. The transport bounds are 2 GiB compressed, 4 GiB expanded and 10,000 entries; the minimal receipt bundle need not contain the full HTML/video/trace archive. Archive integrity is verified before manual extraction into a fresh owned staging directory. The old target bundle is removed first, so a missing, expired, corrupt or stale input cannot reuse yesterday's result. All required native jobs, actual test/project outcomes and source/runtime hashes are subsequently checked by the existing importer.

Run-specific digests stay in pipeline inputs, **not in the tested commit**. First finish the intended code/version/tag commit, obtain independently reviewed Next evidence testing that exact QQQ SHA, then trigger CircleCI for that same immutable commit via its candidate/hotfix ref or release tag. The stager checks the receipt against `git rev-parse HEAD` and, when provided, `CIRCLE_SHA1`; the exact tested Next commit must also match the reviewed crosswalk. A prior main SHA, PR head substituted for a merge SHA, moved branch/tag, version-changing commit or different runtime bundle is not interchangeable. Except for the explicitly accepted QQQ RC1 / Next RC10 pair above, automatic release pipelines without inputs deliberately remain blocked; re-trigger with matching reviewed inputs rather than relaxing the gate. If the intended source changes, obtain matching evidence and new reviewed inputs. This mechanism neither fills missing behavior mappings nor grants a release waiver.

Local staging/coverage commands are `python3 -B qqq-sample-project/next_acceptance_ci.py stage`, then `report` or `strict`. Set `QQQ_NEXT_CI_BUNDLE_URL` (or the secure fallback), `QQQ_NEXT_BUNDLE_SHA256` and `QQQ_NEXT_RECEIPT_SHA256` in the environment. Staging records pending/invalid input and allows diagnostic reporting to continue; `strict` cannot succeed unless staging matches the current inputs and the existing checker passes. No real receipt is shipped in this repository.
