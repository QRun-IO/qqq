# Filesystem portability qualification

This change corrects local filesystem and remote path handling on Windows while
preserving native local path semantics. S3 and SFTP retain POSIX separators and
case-sensitive NIO glob matching on every host. Literal backend prefixes no
longer become regular expressions or glob syntax. Local storage operations check
the resolved table boundary, including existing ancestors of new targets.

Untrusted local storage references containing `..` are rejected consistently.
Administrator-configured paths retain native symlink and parent-segment
resolution. The host must prevent untrusted concurrent directory mutation;
validation followed by I/O does not provide atomic protection against a local
OS actor replacing symlinks.

## Verification performed

Qualification used JDK 21 on Windows and an isolated Maven repository based on
core source `ecf0f550a0357cec6387c2b54efca128a405a629`. The modified filesystem
artifact had a distinct source-hash version; it was not represented as unchanged
core bytes.

- Full module `clean verify`: **142 discovered, 138 passed, four existing skips,
  zero failures or errors**. Local, S3 and SFTP tests ran. Checkstyle, JaCoCo
  checks and SpotBugs passed. PMD completed under its existing nonblocking policy.
- Regressions cover native local separators, literal regex/glob characters in
  prefixes, remote case sensitivity and NIO glob alternatives/classes/escapes,
  allowed configured symlink roots, metadata `symlink/..` resolution, and denied
  outside-table reads, writes, new targets and deletions. Dangling links and
  untrusted parent-segment references are rejected.
- An independent review identified premature lexical normalization of configured
  `symlink/..` metadata. A probe reproduced the regression against the prior JAR;
  the subsequent full gate passed with the actual-native-destination regression.
- A later cleanup changed exactly two comment lines without changing line
  counts. Fresh compilation, Checkstyle and PMD passed, and **all 70 production
  class files matched the fully tested JAR byte-for-byte**. The cleanup removed
  two introduced PMD warnings; 352 warnings remain under the existing policy.
  Tests were not rerun for this comment-only delta.
- The optional mobile Record Files consumer passed **18 tests**: 16 integration
  executions and two widget-contract tests. The integration executions include
  12 operation-guard concurrency cases plus identity substitution, session and
  approval changes, parent authorization, attachment policy, and persistent
  upload association. Provenance recorded **214 dependency JARs, 3,163 unique
  QQQ/QBit classes and no duplicates**.

The Windows module invocation disabled only the existing cosmetic shell coverage
summary (`exec.skip`); it retained the tests and configured coverage/static
analysis checks. Production dependency resolution selected Guava **33.7.2-jre**
and Jimfs **1.3.2**. The optional consumer selected H2 **2.2.224** and a consistent
JUnit API/engine/params **6.0.2**; parameterized tests require the explicit,
BOM-managed `junit-jupiter-params` test dependency in that consumer.

## Evidence identities and limits

The fully tested source manifest SHA-256 is
`6ba69744254c3ec9f2e39349651148cdd5ca645b364cd31c40ee00e1fca66f3e`.
Its JAR SHA-256 is
`57815f69178e13320560279372ac64b8e59e8b0d05a422b92d918c7d2a721def`.
The subsequent comment-cleanup source manifest SHA-256 is
`6945a2ffe5e87b34f900393ce0edd9fbcb0e0d21c9b68d52e6b62ac23c9a47b3`.
These manifests describe the isolated qualification source copies before this
documentation was added; they are not Git commit hashes or release coordinates.

The consumer proof combined scanner source
`10416938724c9663d66f7848ff2b1a63fe635794` with frozen Record Files and collaboration
artifacts and the explicitly distinct filesystem artifact. It does not replace
the owners' complete release suites, migration qualification, or publication of
immutable dependencies. No optional default tuple, shared Maven installation,
operator database or handheld was changed by these tests.

Before adopting this as one release tuple, build the required core/BOM/filesystem
reactor from the final signed source revision, requalify scanner/demo/browser and
optional consumer gates against that revision, and obtain the files owner's
complete matching immutable artifact/source handoff. Preserve the old frozen
bundles and their evidence when making the explicit adoption change.

## Malformed-reference follow-up

Hosted sample acceptance at the initial signed portability revision found that a NUL-containing reference escaped as unchecked InvalidPathException. The correction translates only invalid untrusted reference parsing into the existing fixed-message QBadRequestException, without retaining the reference or cause. Metadata parsing and containment remain unchanged. The existing invalid-reference test now covers NUL for reads, writes, URL generation and deletion.

The corrected module passes fresh clean verify:142 discovered,138 passed,four existing skips,zero failures/errors, with coverage and configured static checks retained. A direct StorageAction probe reproduces unchecked InvalidPathException in all four operations against the initial signed JAR and verifies the safe QQQ error against the corrected JAR. Corrected qualification manifest SHA256:789cb83dd4326179ce1a903bc8cd88e1a8ab4d4856e2117d4e724eab5fcde2e6; JAR SHA256:a52738a1807a07284dbcc404d98309a052eddcf4838356f8cbd15693c3d6be0e. The earlier probe setup failed metadata validation; it was corrected and its failed logs were retained separately.

The initial signed revision also passed the coherent core/BOM/filesystem closure (4,649 discovered,4,629 passed,20 existing skips) and the matching scanner39/demo6/RecordFiles18/browser15 suites. Those source-bound results remain separate from this follow-up; fresh hosted sample acceptance and final coherent dependency adoption are still required for the corrected revision. No default dependencies or operator runtime changed.
