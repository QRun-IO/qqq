# QQQ Sample Project

Example QQQ application demonstrating framework usage.

## What's Included

- Sample table definitions
- Javalin HTTP server setup
- PicoCLI command-line interface
- Basic authentication example

## Running

```bash
# Start HTTP server
mvn exec:java -Dexec.mainClass="com.example.SampleServer"

# Run CLI
mvn exec:java -Dexec.mainClass="com.example.SampleCli" -Dexec.args="query users"
```

## Project Structure

```
src/main/java/
├── SampleMetaDataProvider.java  # Table and backend definitions
├── SampleServer.java            # Javalin server entry point
└── SampleCli.java               # PicoCLI entry point
```

## License

GNU Affero General Public License v3.0
