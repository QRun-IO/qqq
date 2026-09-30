# Generated infrastructure extension acceptance (#610)

This fixture copies tracked files from the first-party `qbit-template-extension`
checkout into a disposable directory, renames the QBit, and fills the template's
documented customization hooks with consumer-owned `PRE_INSERT_RECORD` behavior.
It leaves the template and QQQ source checkouts unchanged. A separate Maven host
depends on the generated artifact and executes real memory-backend inserts and
reads. Three JUnit cases check the configured target, disabled configuration,
missing target and field prerequisites, and unrelated table isolation. The runner
also verifies that the host cannot compile without the generated dependency.

From a clean QQQ source worktree, with a clean first-party template
checkout, Java 21, and Maven access to public release dependencies:

```sh
python3 qqq-sample-project/acceptance/extension/run_extension.py \
  --qqq-source . \
  --template-source /path/to/qbit-template-extension \
  --maven-repo /path/to/disposable-maven-repository \
  --report-dir qqq-sample-project/target/surefire-reports
```

The runner derives the candidate version from the source root POM's `revision`.
Pass `--candidate-version 0.0.0-sample-acceptance` to use CI's isolated Maven
version instead. It installs the checked-out QQQ BOM and core at that exact
version before building the generated extension and host against the same version.
The runner permits downloads of public template plugins and dependencies, while
Maven's `-nsu` prevents remote snapshot refresh from replacing the local
candidate. It prints both source commits and the candidate version, and removes
its generated projects on success or failure. The optional `--report-dir` copies
the host JUnit report only after all acceptance checks pass; CI uses it for the
sample feature ledger. Omit `--maven-repo` to use the default local repository.
CI passes its existing Maven settings with `--maven-settings` so the core build
uses the same configured repositories while public template plugins can be
downloaded. A cache populated only by the QQQ core build is sufficient.
This is source-stage acceptance; it does not prove
registry publication or the template's unfinished example extension out of the
box.

The new Java fixture sources use the Apache-2.0 header from QQQ #797. This
branch is based on develop before #797, whose Checkstyle license template still
expects AGPL. Full-tree license/Checkstyle validation therefore needs the #797
head combined; the standalone host fixture does not run that root gate.
