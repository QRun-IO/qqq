# QQQ Middleware - API

Core middleware API interfaces and base classes.

## What It Provides

- Base classes for middleware implementations
- Request/response handling interfaces
- Session management contracts
- Authentication hooks

## Usage

This module is a dependency for other middleware modules (`qqq-middleware-javalin`, `qqq-middleware-lambda`, etc.) and provides shared interfaces.

```java
public interface QMiddlewareInterface
{
   void start(QInstance instance);
   void stop();
}
```

## License

GNU Affero General Public License v3.0
