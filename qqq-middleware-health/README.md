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

import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.health.indicators.DatabaseHealthIndicator;
import com.kingsrook.qqq.middleware.health.indicators.MemoryHealthIndicator;
import com.kingsrook.qqq.middleware.health.indicators.DiskSpaceHealthIndicator;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData;

public class ApplicationHealthMetaDataProducer extends com.kingsrook.qqq.middleware.health.HealthMetaDataProducer
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
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.health.HealthIndicator;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;

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
      // Replace this constant result with the application check.
      return new HealthCheckResult()
         .withStatus(HealthStatus.UP)
         .withDurationMs(0L)
         .withDetail("key", "value");
   }
}
```

## Migration from Manual Registration

Use the metadata producer shown above in place of explicitly adding a `JavalinHealthRouteProvider` to the server's route providers. Ensure the application loads the producer's package.

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.

