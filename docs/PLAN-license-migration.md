# PLAN: Apache 2.0 License Migration

## Goal

Migrate all QRun-IO open-source repositories from AGPL-3.0 to Apache-2.0.

## Approach

1. Update LICENSE files in all repos (DONE)
2. Update source file headers to simplified format pointing to LICENSE
3. Update pom.xml license declarations
4. Update README badges and license text

## New License Header

```java
/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025 QRun-IO, LLC
 *
 * See the LICENSE file in the repository root for details.
 */
```

## Files Affected

### Phase 1: LICENSE/NOTICE files (COMPLETE)
- 24/25 repos updated via GitHub MCP
- qbit-template not found

### Phase 2: Source File Headers (IN PROGRESS)
- ~5,747 Java files with AGPL headers
- 9 checkstyle/license.txt template files

### Phase 3: Config Files (PENDING)
- ~20 pom.xml files with `<licenses>` section
- ~15 README.md files with AGPL mentions
- 1 package.json (qqq-frontend-core)

## Steps

### Update checkstyle/license.txt templates
1. [x] qqq/checkstyle/license.txt - DONE
2. [ ] Other 8 repos - copy same template

### Update Java source files
Option A: Write script to bulk-replace old header with new
Option B: Run `mvn checkstyle:check` to identify violations, fix manually
Option C: Add spotless plugin to auto-format

Recommended: Script approach for bulk update, then checkstyle to verify

### Update pom.xml files
Replace:
```xml
<licenses>
  <license>
    <name>GNU Affero General Public License v3.0</name>
    <url>https://www.gnu.org/licenses/agpl-3.0-standalone.html</url>
  </license>
</licenses>
```

With:
```xml
<licenses>
  <license>
    <name>Apache License, Version 2.0</name>
    <url>https://www.apache.org/licenses/LICENSE-2.0</url>
  </license>
</licenses>
```

## Open Questions

- Should we update copyright year range in all files (2021-2022 -> 2021-2025)?
- Should qbit repos have their own copyright line or reference QRun-IO?
