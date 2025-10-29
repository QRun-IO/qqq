# IsolatedSpaRouteProvider

The `IsolatedSpaRouteProvider` enables QQQ applications to serve multiple Single Page Applications (SPAs) simultaneously, each with complete isolation and independent routing capabilities.

## Key Features

- **Multiple SPAs**: Serve multiple SPAs at different paths (e.g., `/`, `/admin`, `/customer`)
- **Deep Linking Support**: Each SPA handles its own 404s and client-side routing
- **Complete Isolation**: SPAs don't interfere with each other or with API endpoints
- **Authentication Support**: Each SPA can have its own authentication requirements
- **API Preservation**: All existing API endpoints continue to work normally
- **Flexible Configuration**: Support for both filesystem and JAR-based static file serving

## Basic Usage

### Programmatic Configuration

```java
QApplicationJavalinServer server = new QApplicationJavalinServer(application)
   .withPort(8080)
   .withServeLegacyUnversionedMiddlewareAPI(true)  // Keep APIs enabled
   
   // Root SPA
   .withIsolatedSpaRouteProvider("/", "public-site/", "public-site/index.html")
   
   // Admin SPA with authentication
   .withAdditionalRouteProvider(
      new IsolatedSpaRouteProvider("/admin", "admin-spa/dist/", "admin-spa/dist/index.html")
         .withAuthenticator(new QCodeReference(AdminAuthenticator.class)))
   
   // Customer SPA
   .withIsolatedSpaRouteProvider("/customer", "customer-spa/build/", "customer-spa/build/index.html");
```

### Metadata Configuration

You can also configure SPAs through QInstance metadata:

```java
JavalinRouteProviderMetaData spaConfig = new JavalinRouteProviderMetaData()
   .withName("admin-spa")
   .withSpaPath("/admin")
   .withStaticFilesPath("admin-spa/dist/")
   .withSpaIndexFile("admin-spa/dist/index.html")
   .withExcludedPaths(List.of("/api", "/customer"))
   .withEnableDeepLinking(true)
   .withLoadFromJar(true)
   .withRouteAuthenticator(new QCodeReference(AdminAuthenticator.class));
```

## Configuration Options

### Constructor Parameters

- `spaPath`: The URL path where the SPA is hosted (e.g., `/`, `/admin`, `/customer`)
- `staticFilesPath`: Path to static files (classpath or filesystem)

### Fluent Configuration Methods

- `withSpaIndexFile(String)`: Set the SPA's index.html file
- `withAuthenticator(QCodeReference)`: Set authentication for this SPA
- `withExcludedPaths(List<String>)`: Exclude paths from root SPA (only for `/` path)
- `withDeepLinking(boolean)`: Enable/disable deep linking support (default: true)
- `withLoadFromJar(boolean)`: Load static files from JAR vs filesystem
- `withBeforeHandler(Handler)`: Add custom before handler
- `withAfterHandler(Handler)`: Add custom after handler

## Architecture

### SPA Isolation

Each `IsolatedSpaRouteProvider` creates a completely isolated environment:

1. **Static File Serving**: Each SPA serves its own static files from its configured path
2. **404 Handling**: Each SPA handles its own 404s via the `SpaNotFoundHandlerRegistry`
3. **Authentication**: Each SPA can have its own authentication requirements
4. **Middleware**: Each SPA can have custom before/after handlers

### 404 Handling

The `SpaNotFoundHandlerRegistry` manages 404 handling for multiple SPAs:

- **Global Handler**: Single 404 handler registered with Javalin
- **Path Matching**: Routes 404s to the appropriate SPA based on path prefix
- **Priority**: More specific paths take precedence over less specific ones
- **Fallback**: Root path (`/`) serves as the fallback for unmatched paths

### API Preservation

All existing API endpoints continue to work normally:

- Legacy unversioned middleware API (`/metaData`, `/data`, `/processes`, etc.)
- Versioned middleware API (`/qqq-api/v1/...`)
- Application-specific APIs
- Static file serving for non-SPA content

## Examples

### Multiple SPAs with Different Configurations

```java
QApplicationJavalinServer server = new QApplicationJavalinServer(application)
   .withPort(8080)
   
   // Public website at root
   .withAdditionalRouteProvider(
      new IsolatedSpaRouteProvider("/", "public-site/", "public-site/index.html")
         .withExcludedPaths(List.of("/admin", "/customer", "/api"))
         .withDeepLinking(true))
   
   // Admin dashboard with authentication
   .withAdditionalRouteProvider(
      new IsolatedSpaRouteProvider("/admin", "admin-spa/dist/", "admin-spa/dist/index.html")
         .withAuthenticator(new QCodeReference(AdminAuthenticator.class))
         .withLoadFromJar(true))
   
   // Customer portal
   .withAdditionalRouteProvider(
      new IsolatedSpaRouteProvider("/customer", "customer-spa/build/", "customer-spa/build/index.html")
         .withDeepLinking(true)
         .withLoadFromJar(false));
```

### Root SPA with Exclusions

When using a root SPA (`/`), you must exclude other SPA paths and API paths:

```java
new IsolatedSpaRouteProvider("/", "public-site/", "public-site/index.html")
   .withExcludedPaths(List.of(
      "/admin",           // Other SPA
      "/customer",        // Other SPA
      "/api",             // API endpoints
      "/qqq-api",         // Versioned API
      "/metaData",        // Legacy API
      "/data",            // Legacy API
      "/processes",       // Legacy API
      "/reports",         // Legacy API
      "/download"         // Legacy API
   ))
```

## Testing

The implementation includes comprehensive tests that verify:

- Multiple SPAs can run simultaneously
- Each SPA handles its own deep linking correctly
- SPAs don't interfere with each other
- API endpoints continue to work normally
- Static assets 404 naturally (not caught by SPAs)
- Authentication works per SPA
- 404 handling works correctly for each SPA

## Migration from SimpleFileSystemDirectoryRouter

If you're currently using `SimpleFileSystemDirectoryRouter` for SPA serving, migration to `IsolatedSpaRouteProvider` is straightforward:

### Before (SimpleFileSystemDirectoryRouter)
```java
.withAdditionalRouteProvider(
   new SimpleFileSystemDirectoryRouter("/app", "spa-files/")
      .withSpaRootPath("/app")
      .withSpaRootFile("spa-files/index.html"))
```

### After (IsolatedSpaRouteProvider)
```java
.withAdditionalRouteProvider(
   new IsolatedSpaRouteProvider("/app", "spa-files/", "spa-files/index.html"))
```

## Best Practices

1. **Use Exclusions for Root SPAs**: Always exclude other SPA paths and API paths when using a root SPA
2. **Consistent Naming**: Use consistent naming conventions for SPA paths and static file directories
3. **Authentication**: Apply authentication at the SPA level, not globally
4. **Testing**: Test each SPA independently and together to ensure proper isolation
5. **Performance**: Consider using JAR-based serving for production deployments

## Troubleshooting

### SPA Not Loading
- Check that the `staticFilesPath` and `spaIndexFile` are correct
- Verify that static files exist in the expected location
- Check the `loadFromJar` setting matches your deployment method

### 404s Not Handled Correctly
- Ensure `withDeepLinking(true)` is set
- For root SPAs, verify exclusions are properly configured
- Check that the `SpaNotFoundHandlerRegistry` is working correctly

### API Endpoints Not Working
- Ensure `withServeLegacyUnversionedMiddlewareAPI(true)` is set
- Check that API paths are excluded from root SPAs
- Verify that API routes are registered before SPA routes

### Cross-SPA Interference
- Each SPA should only handle requests under its own path
- Use exclusions for root SPAs to prevent interference
- Check that the `SpaNotFoundHandlerRegistry` is routing correctly