# QQQ Backend Module - MongoDB

Backend module for MongoDB document databases.

## Features

- Native MongoDB driver integration
- Document-to-record mapping
- Aggregation pipeline support
- Index management

## Usage

```java
MongoDBBackendMetaData backend = new MongoDBBackendMetaData()
   .withName("mongo-main")
   .withConnectionString("mongodb://localhost:27017")
   .withDatabaseName("myapp");

QTableMetaData table = new QTableMetaData()
   .withName("users")
   .withBackendName("mongo-main")
   .withBackendDetails(new MongoDBTableBackendDetails()
      .withCollectionName("users"));
```

## Requirements

- MongoDB 4.4+
- Java 17+

## License

GNU Affero General Public License v3.0
