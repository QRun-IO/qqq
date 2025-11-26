# QQQ Middleware - Health

Kubernetes-compatible health check endpoints for QQQ applications.

## Features

- `/health` endpoint for Kubernetes liveness and readiness probes
- Built-in health indicators: Database, Memory, Disk Space
- Extensible health indicator system
- Thread-safe concurrent health checks
- Configurable timeouts and thresholds
- JSON response format

## Usage

```java
QInstance qInstance = new QInstance()
   .withSupplementalMetaData(new HealthCheckMetaData()
      .withEnabled(true)
      .withEndpointPath("/health")
      .withIndicators(List.of(
         new DatabaseHealthIndicator().withBackendName("rdbms"),
         new MemoryHealthIndicator().withThreshold(85)
      )));
```

## License

AGPL 3.0 - See LICENSE file

