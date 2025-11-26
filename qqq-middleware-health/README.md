# QQQ Middleware - Health

Kubernetes-compatible health check endpoints for QQQ applications.

## Features

- `/health` endpoint for Kubernetes liveness/readiness probes
- Built-in indicators: Database, Memory, Disk Space
- Extensible health indicator interface
- Thread-safe concurrent execution
- Auto-registration via metadata (no manual setup required)

## Usage

Extend `HealthMetaDataProducer` to configure health checks:

```java
package com.myapp.metadata.autoload.health;

import com.kingsrook.qqq.middleware.health.HealthMetaDataProducer;

public class HealthMetaDataProducer extends com.kingsrook.qqq.middleware.health.HealthMetaDataProducer
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
               .withPath("/var/myapp")
               .withMinimumFreeBytes(1_000_000_000L)
         ))
         .withTimeoutMs(5000);
   }
}
```

Place this class in your `metadata.autoload` package tree. The health endpoint registers automatically - no code needed in `Server.java`.

## Response Format

```json
{
  "status": "UP",
  "timestamp": "2025-11-26T10:30:00Z",
  "checks": {
    "database": {
      "status": "UP",
      "durationMs": 45,
      "details": {
        "backendName": "rdbms",
        "vendor": "postgresql"
      }
    },
    "memory": {
      "status": "UP",
      "durationMs": 2,
      "details": {
        "usedPercent": "45.2",
        "thresholdPercent": 85
      }
    }
  }
}
```

HTTP status: 200 (UP/DEGRADED), 503 (DOWN)

## Built-in Indicators

- `DatabaseHealthIndicator` - Check database connectivity with simple query
- `MemoryHealthIndicator` - Check JVM heap usage percentage
- `DiskSpaceHealthIndicator` - Check available disk space

## Custom Indicators

Implement `HealthIndicator` interface:

```java
public class CustomHealthIndicator implements HealthIndicator
{
   @Override
   public String getName()
   {
      return "customCheck";
   }

   @Override
   public HealthCheckResult check(QInstance qInstance) throws QException
   {
      // Your check logic here
      return new HealthCheckResult()
         .withStatus(HealthStatus.UP)
         .withDurationMs(duration)
         .withDetail("key", "value");
   }
}
```

## Migration from Manual Registration

Replace deprecated manual registration:

```java
// OLD (deprecated):
.withAdditionalRouteProvider(new JavalinHealthRouteProvider())
```

With metadata-driven approach shown in Usage section above.

## License

AGPL 3.0

