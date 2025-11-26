# QQQ Middleware - Health

Kubernetes-compatible health check endpoints for QQQ applications.

## Features

- `/health` endpoint for Kubernetes liveness and readiness probes
- Built-in health indicators: Database, Memory, Disk Space
- Extensible health indicator system
- Thread-safe concurrent health checks
- Configurable timeouts and thresholds
- JSON response format
- Auto-registration via metadata (no manual route provider setup)

## Usage

Health endpoints are configured purely through metadata. Create a `MetaDataProducer` that extends `HealthMetaDataProducer`:

```java
public class MyHealthMetaDataProducer extends HealthMetaDataProducer
{
   @Override
   protected HealthCheckMetaData buildHealthCheckMetaData(QInstance qInstance)
   {
      return new HealthCheckMetaData()
         .withEnabled(true)
         .withEndpointPath("/health")
         .withIndicators(List.of(
            new DatabaseHealthIndicator().withBackendName("rdbms"),
            new MemoryHealthIndicator().withThreshold(85),
            new DiskSpaceHealthIndicator()
               .withPath("/")
               .withMinimumFreeBytes(1_000_000_000L)
         ))
         .withTimeoutMs(5000);
   }
}
```

The health endpoint will be automatically registered when your application starts. No additional code is needed in `Server.java`.

## Migration from Manual Registration

If you're currently using manual route provider registration:

```java
// OLD (deprecated):
.withAdditionalRouteProvider(new JavalinHealthRouteProvider())
```

Migrate to the metadata-driven approach by creating a `HealthMetaDataProducer` as shown above. The manual registration method is deprecated and will be removed in a future release.

## License

AGPL 3.0 - See LICENSE file

