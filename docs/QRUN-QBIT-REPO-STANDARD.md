# QRun qbit repository standard

The audit checklist for every `QRun-IO/qbit-*` repository. It was derived on 2026-10-12 from the
reference qbits (`qbit-quick-search`, `qbit-sql-workbench`, `qbit-assistant`, `qbit-authentik`),
the org community files in `QRun-IO/.github`, the qqq wiki (Branching and Versioning, QBit
Development) and the KofTwentyTwo standards. Where the references disagree, the most complete
setting wins and the reason is noted. `qbit-template` is archived and is not a reference.

An item is **pass**, **fail**, or **owner-action** (it needs the owner's UI or credentials).

## 1. Files

| # | Item | Standard |
|---|---|---|
| F1 | `LICENSE` | Apache-2.0 full text, appendix line `Copyright 2024 Kingsrook, LLC` (copy from `qbit-sql-workbench`; blob `4bb040e`). |
| F2 | `NOTICE` | `QBit <Name>` / `Copyright <year> Kingsrook, LLC` / "This product includes software developed at Kingsrook, LLC (https://kingsrook.com/)." / "Licensed under the Apache License, Version 2.0." The name must be this qbit's. |
| F3 | `README.md` | `# QBit <Name>`, a one-paragraph summary, the design link (`QRun-IO/qqq` spec or PR), and the sections Requirements, Install, (domain sections: tables, permissions, configuration), Adopting in another app, Build, License. License and Java badges are optional (quick-search carries them). |
| F4 | `CONTRIBUTING.md`, `SECURITY.md`, `CODE_OF_CONDUCT.md`, `CODE_STYLE.md` | Short pointer files to the same file in `QRun-IO/qqq` (`develop`). New files link `github.com/QRun-IO/qqq`; older ones still link `Kingsrook/qqq`, which redirects. |
| F5 | `CHANGELOG.md` | Keep a Changelog format, semantic versions, an `Unreleased` section. |
| F6 | `CLAUDE.md` and `AGENTS.md` | Same content (agent header differs): overview, build commands (always `./mvnw`), architecture, invariants, naming rule, code style, dependencies, release flow. |
| F7 | `.github/CODEOWNERS` | `* @KofTwentyTwo` |
| F8 | `.github/dependabot.yml` | Maven and GitHub Actions, weekly, `target-branch: develop`, `open-pull-requests-limit: 3`, minor and patch grouped (the `qqq-frontend-next` pattern of 2026-10-12). |
| F9 | Issue and PR templates | **Not** per repo: inherited from `QRun-IO/.github` (`ISSUE_TEMPLATE/`, `PULL_REQUEST_TEMPLATE.md`). A per-repo copy is a fail unless it differs on purpose. |
| F10 | `.gitignore` | The reference list (`target/`, `.env`, `.idea`, `*.iml`, build archives, `.flattened-pom.xml`, `.claude/settings.local.json`, ...). |
| F11 | `.gitattributes` | `* text=auto eol=lf`, `*.cmd text eol=crlf`, `mvnw text eol=lf` (from `qbit-assistant`). |
| F12 | `.editorconfig` | UTF-8, LF, final newline, 3-space indent (Kingsrook layout), YAML 2, `*.cmd` CRLF. |
| F13 | Maven wrapper | `mvnw` (mode **100755**), `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` pinning Maven 3.9.11. Maven 3.10 cannot read the parent POM, so builds always use `./mvnw`. |
| F14 | `checkstyle/config.xml`, `checkstyle/license.txt` | The Kingsrook config (blob `385d653`). Wiring it into `verify` is optional (`qbit-cdn` does). |
| F15 | `docs/` | At least `docs/DESIGN.md` (the approved spec, or a pointer to it in `QRun-IO/qqq`). Optional: DDL (`docs/ddl/`), status, audits, migration notes. |
| F16 | Workflows | None required (CI is CircleCI). Repo-specific GitHub Actions are allowed (e.g. `qbit-authentik` schema drift). |
| F17 | No secrets, no build output | No `target/`, jars, or credentials committed. |

## 2. Build (`pom.xml`)

| # | Item | Standard |
|---|---|---|
| B1 | Parent | `com.kingsrook:qbit-build-parent:2.0.0` (holds the `release` profile: sources, javadoc, GPG signing, `central-publishing-maven-plugin`). |
| B2 | Coordinates | `groupId` `com.kingsrook.qbits`, `artifactId` = repository name, `<version>${revision}</version>` with `<revision>X.Y.Z-SNAPSHOT</revision>` in `<properties>`. |
| B3 | Metadata | `name` `QBit <Name>`, `description`, `url`, Apache-2.0 `licenses`, `developers` (Kingsrook Team), `scm` pointing at `QRun-IO/<repo>`. |
| B4 | Java | `maven.compiler.source/target` 21. |
| B5 | QQQ version | Default-active `qqq-snapshot` profile importing `qqq-bom-pom` (currently `4.1.0-RC.4`) ahead of the parent's 4.0.0 BOM, until the parent moves to 4.1 (ADR-0007). |
| B6 | Coverage | JaCoCo 0.8.15 `prepare-agent` / `report` / `check`, gate at 70% instructions and 90% classes (bundle). |
| B7 | Static analysis | Checkstyle config present (F14). SpotBugs and PMD are not used by any reference qbit. |
| B8 | Green build | `./mvnw verify` passes on a clean checkout with Java 21. |
| B9 | Package | `com.kingsrook.qbits.<name>`, entry point `<Name>QBitProducer implements QBitMetaDataProducer<<Name>QBitConfig>`. |
| B10 | Naming (owner rule, 2026-10-12) | Tables lowercase snake_case prefixed with the full qbit name (`cdn_`, `web_performance_`, `object_storage_`, `alerts_`, `backups_`, `marketing_`); backend name equals the table name. Permissions use the full name (`object_storage.view`). |

## 3. CI/CD (CircleCI)

| # | Item | Standard |
|---|---|---|
| C1 | `.circleci/config.yml` | Identical in every reference: `kingsrook/qqq-orb@0.6.8`, the `configure-package-install` pre-step, context `qqq-maven-registry-credentials`, workflows `test_only` (all branches except develop, release/*, hotfix/*), `publish_snapshot` (develop), `publish_release_candidate` (release/*), `publish_release` (`v*` tags), `publish_hotfix_release` (hotfix/*). |
| C2 | Project followed | The repository is a CircleCI project (`gh/QRun-IO/<repo>`), `oss: true`, `set-github-status`, fork PRs not built. |
| C3 | Checkout key | A **GitHub user key** (preferred) so the orb can push version bumps and tags; a deploy key alone is not enough for releases. **owner-action** (Project Settings, SSH Keys, Add User Key). |
| C4 | Publishing | Only through the orb: SNAPSHOTs from `develop`, RCs from `release/*`, releases from `v*` tags, to Maven Central. Nobody publishes by hand. |

## 4. Branches and flow

| # | Item | Standard |
|---|---|---|
| G1 | Branches | `main` and `develop` exist; default branch **`develop`** (GitFlow, as `qbit-quick-search` and `qbit-sql-workbench`). |
| G2 | Flow | `feature/*` from `develop`, PR to `develop`; `release/X.Y` cut from `develop` publishes RCs; a `vX.Y.Z` tag publishes the release; `hotfix/*` for fixes; `main` holds reviewed final release source. |
| G3 | Commits | Signed, Conventional Commits. |

## 5. Repository settings

| # | Item | Standard |
|---|---|---|
| S1 | Description | `QQQ QBit: <what it does>`. Homepage empty (no published docs site per qbit). |
| S2 | Topics | `java`, `qbit`, `qbit-extension`, `qqq`, `qqq-plugin` plus domain topics. |
| S3 | Features | Issues on, Projects on, Discussions off, **Wiki off** (docs live in the repo; `qbit-authentik`, K22-REPO-40). |
| S4 | Merges | Merge commits, squash and rebase all allowed (identical in every reference); auto-merge off; **auto-delete head branches off**, so merged `release/*` branches survive. |
| S5 | Security | Dependabot alerts and security updates on, secret scanning and push protection on, private vulnerability reporting on (the `qbit-authentik` set, K22-REPO-30). |
| S6 | Labels | The org default set (bug, dependencies, documentation, duplicate, enhancement, good first issue, help wanted, invalid, question, secuurity, wontfix). The `secuurity` typo is in the org defaults. |

## 6. Branch protection (classic rules; org rulesets need GitHub Team)

| # | Branch | Standard |
|---|---|---|
| P1 | `main` | PR required (0 approvals, stale approvals dismissed), strict status checks (no named context), **signed commits**, **linear history**, conversation resolution, no force push, no deletion, pushes restricted to admins, admins not enforced. |
| P2 | `develop` | PR required (0 approvals, stale approvals dismissed), strict required check **`test_only`**, signed commits, conversation resolution, no force push, no deletion, admins not enforced. |
| P3 | `release/*` | Unprotected in every reference, because the orb pushes version-bump commits (unsigned) to them. Protecting them needs an orb change first. |

## Open questions for the owner

- `main` requires linear history while the flow merges with merge commits. In practice releases are tagged from `release/*` and `main` is not updated; decide whether `main` should track releases (then drop linear history, or merge releases with a squash) or stay as-is.
- `release/*` and `v*` tags have no protection (P3). Consider a tag protection rule once the orb signs its commits.
