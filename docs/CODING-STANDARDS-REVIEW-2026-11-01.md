# Native authentication standards review

Scope: the 28 changed/new Java sources for API 1.10 native nonce validation and optional provider logout, plus their generated contracts. This is not a new review of every unchanged framework source file.

The review preserves the existing Kingsrook Apache license notices and adds SPDX identifiers after each full notice, matching the repository's fixed Checkstyle header. Scoped public methods/types and test overrides have the required banner comments. Sanitized authentication failures deliberately omit provider exception causes because those causes may contain tokens; this is a narrow credential-protection requirement, not permission to discard ordinary diagnostics.

Verification: all seven modules in the middleware-api dependency reactor pass enabled Checkstyle validation with zero violations. The earlier functional candidate passes 159 focused authentication tests, three generated-contract checks and 78 coherent scanner/demo/files/browser consumer cases. Subsequent scoped source edits are comments/whitespace; generated YAML is copied from that qualified candidate. Full-reactor offline validation additionally passed through middleware-health before the unrelated ESB module required an uncached Failsafe plugin. A fresh full hosted core gate remains required after publication.

The disposable real Authentik fixture now runs the qualified API 1.10 runtime. Actual negative nonce and retained-browser A → company logout → B acceptance remain separate runtime gates. The normal scanner server is unchanged. No production release, signing decision or exhaustive whole-framework security clearance is implied.
