# QQQ Middleware - Lambda

AWS Lambda middleware. Deploy QQQ applications as serverless functions.

## Features

- AWS Lambda request/response handling
- API Gateway integration
- Cold start optimization
- Environment-based configuration

## Usage

```java
public class MyLambdaHandler extends QqqLambdaHandler
{
   @Override
   protected QInstance defineQInstance()
   {
      QInstance instance = new QInstance();
      // ... configure instance ...
      return instance;
   }
}
```

## Deployment

```yaml
# serverless.yml
functions:
  api:
    handler: com.example.MyLambdaHandler
    events:
      - http:
          path: /{proxy+}
          method: any
```

## License

GNU Affero General Public License v3.0
