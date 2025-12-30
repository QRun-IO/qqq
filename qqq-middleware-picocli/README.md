# QQQ Middleware - PicoCLI

CLI middleware using [PicoCLI](https://picocli.info). Build command-line applications with QQQ.

## Features

- Command-line interface for QQQ operations
- Table queries and data manipulation via CLI
- Process execution from command line
- Scripting and automation support

## Usage

```java
QInstance instance = new QInstance();
// ... configure instance ...

QPicocliImplementation cli = new QPicocliImplementation(instance);
cli.runCli(args);
```

## Commands

```bash
# Query a table
myapp query users --filter "status=active"

# Run a process
myapp process run-report --input file.csv

# Insert records
myapp insert orders --data '{"customer_id": 123}'
```

## License

GNU Affero General Public License v3.0
