# PLAN: SpotBugs + PMD Code Quality Integration

## Goal

Add SpotBugs and PMD static analysis to the QQQ build for local execution and optional CI/CD.

## Approach

1. **Maven Integration** - Add plugins to parent pom.xml with sensible defaults
2. **Non-blocking by default** - Warnings reported but don't fail builds initially
3. **Optional CI workflow** - Manual trigger GitHub Action for on-demand analysis

## Configuration Strategy

| Tool | Phase | Default | Override Property |
|------|-------|---------|-------------------|
| SpotBugs | verify | report only | `-Dspotbugs.failOnError=true` |
| PMD | verify | report only | `-Dpmd.failOnViolation=true` |

## Files to Create/Modify

- `pom.xml` - Add plugin configurations
- `spotbugs/exclude-filter.xml` - Exclusions for known false positives
- `pmd/ruleset.xml` - Custom ruleset tuned for QQQ
- `.github/workflows/static-analysis.yml` - Optional CI workflow

## Local Commands

```bash
# Run SpotBugs only
mvn spotbugs:check

# Run PMD only
mvn pmd:check

# Run both during verify
mvn verify

# Fail on issues (CI mode)
mvn verify -Dspotbugs.failOnError=true -Dpmd.failOnViolation=true
```

## Plugin Versions

- SpotBugs Maven Plugin: 4.8.6.6
- PMD Maven Plugin: 3.26.0
- PMD: 7.9.0

## Exclusion Strategy

SpotBugs exclusions for QQQ patterns:
- `EI_EXPOSE_REP` / `EI_EXPOSE_REP2` - Fluent builders intentionally expose mutable state
- `NP_NULL_ON_SOME_PATH` - QContext patterns with known null handling

PMD suppressions:
- `AvoidFieldNameMatchingMethodName` - QQQ getter/setter pattern
- `TooManyMethods` - MetaData classes are large by design
