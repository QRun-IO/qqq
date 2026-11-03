# Guava deserialization security update

The parent manages `com.google.guava:guava:33.7.2-jre`, fixing [CVE-2026-102554 / GHSA-xxph-c9ww-hj94](https://github.com/google/guava/security/advisories/GHSA-xxph-c9ww-hj94), published October 27, 2026. The advisory concerns excessive allocation while reading hostile Java-serialized Guava collections. Earlier candidate and operator classpaths contained affected `32.1.2-jre`; this does not by itself establish an exposed HTTP deserialization path.

`qqq-backend-core` now declares its existing Guava usage directly instead of relying on another library's transitive dependency. The version comes from parent dependency management. `qqq-bom-pom` inherits the same management, so BOM importers receive the constraint; the direct backend-core dependency also carries the patched version to consumers that do not import the BOM. An application's own explicit dependency management can override this, so verify the resolved consumer runtime rather than inferring it from these source declarations.

## Qualification and adoption

Before publishing/adopting the updated core:

- Run existing `TableStateProviderTest` for state serialization, encryption and class-filter behavior, and `FluentListTest`, `ClassPathUtilsTest`, `ProcessFileDownloadTest`, `RunProcessActionTest` and `CancelProcessActionTest` for the direct Guava call sites and process-state behavior. Keep the native-session/null-parser and OpenAPI contract gates when qualifying the scanner's core closure.
- Inspect the effective imported BOM and a fresh downstream consumer dependency tree. Require one selected `com.google.guava:guava:33.7.2-jre` runtime JAR, with no older Guava JAR or duplicate collection classes. Preserve the artifact hash in the scanner fixture provenance.
- Rebuild and test scanner base/demo against the new immutable core source/release coordinate in an isolated Maven repository. Never replace an existing published artifact or infer adoption from a source commit alone.

The dependency pin is not a serialization-policy change. StateSerialization's existing default class allowlist remains unchanged. No change to the running operator server, Maven publication, completed consumer qualification or general vulnerability clearance is implied by this source update.
