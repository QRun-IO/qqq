# QQQ Utility Lambdas

Lightweight AWS Lambda utilities for QQQ applications. Simple functions that integrate with QQQ without running the full framework.

## Features

- S3 event handlers
- SQS message processors
- Scheduled task utilities
- Lightweight QQQ integrations

## Usage

```java
public class S3EventHandler implements RequestHandler<S3Event, String>
{
   @Override
   public String handleRequest(S3Event event, Context context)
   {
      // Process S3 events and integrate with QQQ
      return "OK";
   }
}
```

## License

GNU Affero General Public License v3.0
