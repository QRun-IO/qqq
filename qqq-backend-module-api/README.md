# QQQ Backend Module - API

Backend module for consuming external web service APIs as QQQ data sources.

## Features

- HTTP/HTTPS API integration
- REST endpoint mapping to QQQ tables
- Authentication support (API keys, OAuth, etc.)
- Request/response transformation

## Usage

```java
APIBackendMetaData backend = new APIBackendMetaData()
   .withName("external-api")
   .withBaseUrl("https://api.example.com/v1");

QTableMetaData table = new QTableMetaData()
   .withName("customers")
   .withBackendName("external-api")
   .withBackendDetails(new APITableBackendDetails()
      .withTablePath("/customers"));
```

## License

GNU Affero General Public License v3.0
