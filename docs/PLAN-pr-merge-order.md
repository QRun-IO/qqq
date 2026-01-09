# PR Merge Plan

## Overview

Five PRs to review, approve, merge, and test in optimal order to minimize conflicts and risk.

**Status: COMPLETED** - All PRs merged on 2026-01-09

## PR Summary

| PR | Title | Type | Author | Status |
|----|-------|------|--------|--------|
| #321 | fix(personalization): child-table meta-data | Bug Fix | darinkelkhoff | MERGED |
| #320 | fix(bulkEditWithFile): avoid huge query results | Bug Fix | darinkelkhoff | MERGED |
| #335 | fix(rdbms): PostgreSQL timestamp/identifier fixes | Bug Fix | KofTwentyTwo | MERGED |
| #337 | feat(auth): OAuth2 customizer support | Feature | KofTwentyTwo | MERGED |
| #280 | feat: virtual fields and alternative sections | Feature | darinkelkhoff | MERGED |

## Merge Order (Completed)

### 1. PR #321 - Personalized Child Record List Metadata
- [x] Merged: `b2837708cd7194fca561b20f5dd3d4435dea0da9`

### 2. PR #320 - Bulk Edit Query Optimization
- [x] Approved and merged: `dea9c2a4761c6472c27189399798ba187eaf33a5`

### 3. PR #335 - PostgreSQL Timestamp and Identifier Fixes
- [x] Merged: `886f86f1dd007410d5a4cd62ba5432ed9777c09c`

### 4. PR #337 - OAuth2 Authentication Customizer
- [x] Merged: `53d6a5bc1fe0ae0c7d7fe0d79942a69282cc19a3`

### 5. PR #280 - Virtual Fields and Alternative Sections
- [x] Branch updated with develop
- [x] Approved and merged: `a9eb8d28231664286aee8e75e621b3810d89ec52`

## Post-Merge Validation

After all PRs merged:

```bash
# Full test suite
mvn clean verify

# Specific module tests if concerned
mvn test -pl qqq-backend-core
mvn test -pl qqq-backend-module-postgres
mvn test -pl qqq-middleware-javalin
```

## Risk Assessment

| PR | Risk | Mitigation |
|----|------|------------|
| #321 | Low | Small, targeted change |
| #320 | Medium | Well-tested with 100K row scenario |
| #335 | Low | PostgreSQL-specific, fallback preserves existing behavior |
| #337 | Medium | Mirrors proven Auth0 pattern, WireMock integration tests |
| #280 | High | Large feature; rebase and thorough testing required |

## Notes

- All PRs have passing CI (CircleCI)
- #321 is the only PR with existing approval
- #335 and #337 have requested reviewers (tim-chamberlain, t-samples)
- #280 is the oldest PR (Dec 11) and may need rebase conflict resolution
