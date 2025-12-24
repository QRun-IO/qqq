# QQQ Middleware - Javalin

HTTP server middleware using [Javalin](https://javalin.io). Provides REST APIs and serves the QQQ dashboard.

## Features

- REST API endpoints for all QQQ tables and processes
- Static file serving for dashboard
- Authentication and session management
- WebSocket support

## Usage

```java
QInstance instance = new QInstance();
// ... configure instance ...

QJavalinImplementation javalin = new QJavalinImplementation(instance);
javalin.startJavalin(8080);
```

## Endpoints

| Path | Description |
|------|-------------|
| `/api/` | REST API root |
| `/api/{table}/query` | Query table records |
| `/api/{table}/insert` | Insert records |
| `/api/{table}/update` | Update records |
| `/api/{table}/delete` | Delete records |
| `/api/process/{name}/run` | Run a process |

## License

GNU Affero General Public License v3.0
