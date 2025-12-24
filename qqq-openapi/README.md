# QQQ OpenAPI

OpenAPI/Swagger specification generator for QQQ applications.

## Features

- Automatic OpenAPI spec generation from QQQ metadata
- Swagger UI integration
- API documentation export

## Usage

```java
QInstance instance = new QInstance();
// ... configure instance ...

OpenApiSpecGenerator generator = new OpenApiSpecGenerator(instance);
String openApiSpec = generator.generate();
```

## Output

Generates OpenAPI 3.0 specification including:
- Table CRUD endpoints
- Process endpoints
- Request/response schemas
- Authentication requirements

## License

GNU Affero General Public License v3.0
