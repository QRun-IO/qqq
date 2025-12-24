# QQQ Language Support - JavaScript

JavaScript runtime integration for QQQ. Execute JavaScript code within QQQ processes.

## Features

- GraalJS runtime integration
- JavaScript-based process steps
- Scripting for transformations and validations
- Shared context between Java and JavaScript

## Usage

```java
// Define a JavaScript-based process step
QProcessMetaData process = new QProcessMetaData()
   .withName("transform-data")
   .withStep(new QCodeReference()
      .withCodeType(QCodeType.JAVASCRIPT)
      .withCode("record.total = record.quantity * record.price;"));
```

## Requirements

- Java 17+
- GraalJS runtime

## License

GNU Affero General Public License v3.0
