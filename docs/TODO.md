# TODO

## Completed (2026-01-14)

### SpotBugs + PMD Implementation
- [x] Create implementation plan (`docs/PLAN-spotbugs-pmd.md`)
- [x] Add SpotBugs Maven plugin to parent pom.xml
- [x] Create SpotBugs exclusion filter (`spotbugs/exclude-filter.xml`)
- [x] Add PMD Maven plugin to parent pom.xml
- [x] Create PMD ruleset (`pmd/ruleset.xml`)
- [x] Test local Maven execution
- [x] Add static analysis commands to qqq-orb
- [x] Add static analysis job to qqq-orb
- [x] Update CircleCI config with pipeline parameter

---

## Pending Work

### Orb Release Required
- [ ] Bump qqq-orb version to 0.6.0
- [ ] Publish qqq-orb to CircleCI registry
- [ ] Uncomment static_analysis workflow in qqq `.circleci/config.yml`

### Future: Tune Static Analysis
- [ ] Review SpotBugs findings and add targeted exclusions
- [ ] Review PMD findings and tune ruleset
- [ ] Consider enabling `failOnError` for specific high-priority rules

### Background: License Migration (Paused)
See `docs/PLAN-license-migration.md` for details.

---

## Quick Reference

### Local Static Analysis Commands
```bash
# SpotBugs only
mvn spotbugs:check -DskipTests

# PMD only
mvn pmd:check -DskipTests

# Both with failure on issues
mvn verify -Dspotbugs.failOnError=true -Dpmd.failOnViolation=true

# Single module
mvn spotbugs:check pmd:check -pl qqq-backend-core -DskipTests

# View SpotBugs GUI
mvn spotbugs:gui -pl qqq-backend-core
```

### CircleCI Trigger (after orb release)
```bash
# Via tag
git tag static-analysis/$(date +%Y%m%d) && git push origin --tags

# Via API
curl -X POST https://circleci.com/api/v2/project/gh/Kingsrook/qqq/pipeline \
  -H "Circle-Token: $CIRCLE_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"parameters": {"run_static_analysis": true}}'
```
