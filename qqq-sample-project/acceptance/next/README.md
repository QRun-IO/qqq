# Next successor evidence import (#798/#790)

The seven IDs in `../../next-acceptance-crosswalk.json` retain their original contracts and historical Material provenance. James's explicit Next-only/EOL direction changes the active acceptance target to Next, not the required behavior. The ledger remains authoritative and unchanged. The first five widget positives and block rendering/layout positives have exact source-backed selectors; all composite negative requirements and the full dashboard mapping remain pending. Shared widget error tests are not automatically proof for every widget type. This importer does not resolve #849/#820 or claim that Next tested Material.

The existing checker reads native Playwright `report.json` and Next `gate.json`; it never generates JUnit. It requires first-attempt passes, exact mapped file/title/matrix ID in each of Chromium, Firefox, WebKit, mobile and tablet, complete required matrix rows (respecting the pinned Next `desktopOnly` reasons), matching native gate results/totals, and no partial, failed, skipped or flaky run. Pending mappings and pending ledger review still block the seven rows. Historical Material JUnit mappings are retained but missing historical reports do not certify or block successor coverage; an actual supplied failing compatibility result still blocks. Other rows retain their existing Maven gate.

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
  "qqq_sha": "<exact current QQQ HEAD tested by all three jobs>",
  "mode": "javalin",
  "run_url": "https://github.com/QRun-IO/qqq-frontend-next/actions/runs/<run-id>",
  "jobs": [
    {
      "name": "acceptance-chromium-touch",
      "report": {"path": "acceptance-chromium-touch/report.json", "sha256": "<64-hex>"},
      "gate": {"path": "acceptance-chromium-touch/gate.json", "sha256": "<64-hex>"},
      "checkout_evidence": {"path": "acceptance-chromium-touch/checkout.txt", "sha256": "<64-hex>"},
      "artifacts": {
        "sample": {"path": "acceptance-chromium-touch/sample.jar", "sha256": "<64-hex>"},
        "api": {"path": "acceptance-chromium-touch/api.jar", "sha256": "<64-hex>"},
        "next": {"path": "acceptance-chromium-touch/next.jar", "sha256": "<64-hex>"}
      }
    }
  ]
}
```

The illustration is intentionally incomplete and cannot pass: all three jobs are mandatory, named `acceptance-chromium-touch`, `acceptance-firefox`, and `acceptance-webkit`. Their project sets are respectively `chromium/mobile/tablet`, `firefox`, and `webkit`. Include actual artifact bytes and sanitized checkout/build evidence for **each job**. Preserve the original native JSON bytes; the importer checks the hashes, native `config.metadata.ci.commitHash`, `gitCommit.hash` and `ci.buildHref`. A PR head and its GitHub test merge are different SHAs: the reviewer must reconcile the actual tested commit and update the source crosswalk deliberately, not relabel a receipt. Mutable `develop`/dispatch references alone are insufficient.

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
