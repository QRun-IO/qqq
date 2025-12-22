# SPA Base Href Bug Fix - Review and Testing Guide

This document explains all changes made to fix the SPA `<base href>` bug and provides instructions for manual review and testing.

## Problem Summary

**Original Issue (Darin's Report):**
QFMD uses `"homepage": "."` in package.json, causing relative paths like `./static/js/main.js` to resolve incorrectly on deep-linked URLs.

**Example:**
- User navigates to `/someApp/someTable`
- Browser tries to load `./static/js/main.js`
- Incorrectly resolves to `/someApp/static/js/main.js` instead of `/static/js/main.js`

## Solution Implemented

Inject `<base href="/">` (or `<base href="/spaPath/">` for non-root SPAs) into index.html when serving deep-linked routes. This tells the browser the correct base URL for resolving relative paths.

---

## Files Changed

### New Files

| File | Purpose |
|------|---------|
| `qqq-middleware-javalin/src/main/java/.../routeproviders/SpaPathUtils.java` | Shared utility for path operations |

### Modified Files

| File | Changes |
|------|---------|
| `IsolatedSpaRouteProvider.java` | Base-href injection, uses shared utils, improved error handling |
| `SpaNotFoundHandlerRegistry.java` | Thread-safe list, per-instance tracking, uses shared utils |
| `QApplicationJavalinServer.java` | Uses IsolatedSpaRouteProvider for built-in Material Dashboard |
| `QApplicationJavalinServerTest.java` | Added comprehensive SPA tests, registry cleanup |
| `IsolatedSpaRouteProviderTest.java` | Updated error handling test expectations |
| `SpaNotFoundHandlerRegistryTest.java` | Updated to use SpaPathUtils |

---

## Detailed Change Descriptions

### 1. Base Href Injection (Core Fix)

**File:** `IsolatedSpaRouteProvider.java:560-588`

**Method:** `rewriteIndexHtmlPaths(String html, String basePath)`

**What it does:**
- Checks if `<base>` tag already exists in HTML
- If not, injects `<base href="/spaPath/">` after `<head>`
- If exists, updates the href attribute to the correct value

**Why:**
The HTML5 `<base>` tag tells the browser the base URL for all relative URLs in the document. This ensures `./static/js/main.js` resolves correctly regardless of the current route depth.

### 2. Built-in Material Dashboard Uses IsolatedSpaRouteProvider

**File:** `QApplicationJavalinServer.java`

**What changed:**
Previously used Javalin's `spaRoot.addFile()` which bypassed the base-href injection logic. Now uses `IsolatedSpaRouteProvider` for consistent behavior.

**Before:**
```java
config.spaRoot.addFile(frontendMaterialDashboardHostedPath, "material-dashboard/index.html");
```

**After:**
```java
IsolatedSpaRouteProvider materialDashboardProvider = new IsolatedSpaRouteProvider(
   frontendMaterialDashboardHostedPath,
   "material-dashboard"
)
   .withSpaIndexFile("material-dashboard/index.html")
   .withDeepLinking(true)
   .withLoadFromJar(true);

withAdditionalRouteProvider(materialDashboardProvider);
```

### 3. Path Exclusions Work for All SPAs (Bug Fix)

**File:** `IsolatedSpaRouteProvider.java:368`

**What changed:**
Non-root SPAs were passing `false` for `checkExclusions`, causing exclusions to be ignored.

**Before:**
```java
SpaNotFoundHandlerRegistry.getInstance().registerSpaHandler(spaPath, ctx -> handleNotFound(ctx, false));
```

**After:**
```java
SpaNotFoundHandlerRegistry.getInstance().registerSpaHandler(spaPath, ctx -> handleNotFound(ctx, true));
```

### 4. Thread Safety (Critical Fix)

**File:** `SpaNotFoundHandlerRegistry.java:73`

**What changed:**
Changed `handlers` from `ArrayList` to `CopyOnWriteArrayList` to prevent `ConcurrentModificationException` when handlers are registered while 404s are being processed.

### 5. Singleton Test Isolation (High Fix)

**File:** `SpaNotFoundHandlerRegistry.java:81, 112-121`

**What changed:**
- Replaced boolean `globalHandlerRegistered` with `registeredJavalinInstanceId` (int)
- Uses `System.identityHashCode()` to track which Javalin instance has the handler
- Automatically re-registers for new Javalin instances (important for tests)

### 6. Error Handling (High Fix)

**File:** `IsolatedSpaRouteProvider.java:486-522`

**What changed:**
- Uses try-with-resources for proper stream cleanup
- When index file not found: returns HTTP 500 with error message
- When IOException occurs: returns HTTP 500 with error message

### 7. Path Matching Consistency (Medium Fix)

**File:** `IsolatedSpaRouteProvider.java:463`

**What changed:**
Used simple `startsWith()` which could cause `/admin` SPA to match `/administrator` paths. Now uses `SpaPathUtils.isPathUnderPrefix()` for proper boundary checking.

### 8. Shared Utility Extraction (Medium Fix)

**File:** `SpaPathUtils.java` (new)

**What it provides:**
- `isPathUnderPrefix(requestPath, pathPrefix)` - Proper path prefix matching with boundary checking
- `normalizePath(path)` - Normalizes paths to start with `/` and not end with `/`

Both methods were duplicated in `IsolatedSpaRouteProvider` and `SpaNotFoundHandlerRegistry`. Now centralized in one place.

---

## Manual Review Checklist

### Code Review

- [ ] **SpaPathUtils.java** - Verify path matching logic handles all edge cases
- [ ] **IsolatedSpaRouteProvider.rewriteIndexHtmlPaths()** - Verify regex handles existing `<base>` tags correctly
- [ ] **SpaNotFoundHandlerRegistry** - Verify CopyOnWriteArrayList is appropriate for workload
- [ ] **QApplicationJavalinServer** - Verify Material Dashboard integration is correct

### Test Review

- [ ] **QApplicationJavalinServerTest** - Review comprehensive SPA test scenarios
- [ ] **IsolatedSpaRouteProviderTest** - Verify error handling tests
- [ ] **SpaNotFoundHandlerRegistryTest** - Verify path matching tests

---

## Manual Testing Instructions

### Prerequisites

1. Build the project: `mvn clean install -DskipTests`
2. Have a test application that uses `QApplicationJavalinServer`

### Test Scenarios

#### Scenario 1: Material Dashboard at Root ("/")

1. Configure Material Dashboard to serve at `/`
2. Start the application
3. Navigate to `/` - should see Material Dashboard
4. Navigate to `/someTable/123` (deep link) - should still see Material Dashboard
5. Open browser DevTools Network tab
6. Verify static assets (JS, CSS) load from `/static/...` NOT `/someTable/static/...`
7. View page source - verify `<base href="/">` is present in `<head>`

#### Scenario 2: Material Dashboard at "/dashboard"

1. Configure Material Dashboard to serve at `/dashboard`
2. Start the application
3. Navigate to `/dashboard` - should see Material Dashboard
4. Navigate to `/dashboard/users/123` (deep link) - should still see Material Dashboard
5. Open browser DevTools Network tab
6. Verify static assets load from `/dashboard/static/...` NOT `/dashboard/users/static/...`
7. View page source - verify `<base href="/dashboard/">` is present

#### Scenario 3: Two SPAs (MD at /dashboard, Custom at /)

1. Configure Material Dashboard at `/dashboard`
2. Configure custom SPA at `/` with exclusion for `/dashboard`
3. Navigate to `/` - should see custom SPA
4. Navigate to `/dashboard` - should see Material Dashboard
5. Navigate to `/some-route` - should see custom SPA (deep link)
6. Navigate to `/dashboard/users` - should see Material Dashboard (deep link)

#### Scenario 4: Static Asset 404s

1. Configure any SPA
2. Navigate to `/nonexistent.js` - should get 404 (not SPA HTML)
3. Navigate to `/nonexistent.css` - should get 404 (not SPA HTML)
4. Navigate to `/nonexistent.png` - should get 404 (not SPA HTML)
5. Navigate to `/nonexistent-route` (no extension) - should get SPA HTML

#### Scenario 5: Cross-SPA Boundary Isolation

1. Configure SPA at `/app`
2. Configure SPA at `/apps` (different SPA)
3. Navigate to `/app` - should see first SPA
4. Navigate to `/apps` - should see second SPA
5. Navigate to `/app/users` - should see first SPA (deep link)
6. Navigate to `/apps/data` - should see second SPA (deep link)
7. Verify `/app` SPA doesn't intercept `/apps` requests

#### Scenario 6: Path Exclusions

1. Configure SPA at `/` with exclusion `/api`
2. Navigate to `/api/users` - should get 404 (not SPA HTML)
3. Navigate to `/api-docs` - should get SPA HTML (not excluded - different path)
4. Configure SPA at `/portal` with exclusion `/portal/api`
5. Navigate to `/portal/api/data` - should get 404 (not SPA HTML)
6. Navigate to `/portal/other` - should get SPA HTML

---

## Automated Test Summary

Run all SPA-related tests:

```bash
mvn test -pl qqq-middleware-javalin -Dtest="QApplicationJavalinServerTest,IsolatedSpaRouteProviderTest,SpaNotFoundHandlerRegistryTest"
```

**Expected results:** All 362+ tests pass

### Key Test Classes

| Test Class | What It Tests |
|------------|---------------|
| `QApplicationJavalinServerTest` | Integration tests for full server with SPAs |
| `IsolatedSpaRouteProviderTest` | Unit tests for SPA provider logic |
| `SpaNotFoundHandlerRegistryTest` | Unit tests for 404 handler registry |

### Comprehensive Test Scenarios in QApplicationJavalinServerTest

| Test Method | Scenario |
|-------------|----------|
| `testComprehensive_BuiltInMaterialDashboardAtRoot` | MD at "/" |
| `testComprehensive_BuiltInMaterialDashboardAtDashboardPath` | MD at "/dashboard" |
| `testComprehensive_MaterialDashboardAtDashboardAndCustomSpaAtRoot` | Two SPAs |
| `testComprehensive_TwoNonRootSpasWithNothingAtRoot` | "/" returns 404 |
| `testComprehensive_CrossSpaBoundaryIsolation` | `/app` vs `/apps` |
| `testComprehensive_StaticAsset404ForAllFileTypes` | .js/.css/.png 404 |
| `testComprehensive_DeepPathsAndQueryStrings` | Deep paths work |
| `testComprehensive_DarinsRelativePathScenario` | Original bug scenario |
| `testComprehensive_SpaPathExclusions` | Exclusions work |

---

## Rollback Plan

If issues are discovered:

1. Revert commits on this branch
2. The original behavior used Javalin's built-in `spaRoot.addFile()` which doesn't inject `<base>` tags
3. SPAs with `"homepage": "."` will have broken deep linking (original bug)

---

## Related Links

- Original issue: Darin's report about QFMD relative paths
- Branch: `feature/spa-qfmd-base-bug-investigation`
- Files: See "Files Changed" section above
