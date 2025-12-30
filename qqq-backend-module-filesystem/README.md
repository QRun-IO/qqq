# QQQ Backend Module - Filesystem

Backend module for file-based data sources. Supports local disk, SFTP, and AWS S3.

## Features

- Local filesystem access
- SFTP remote file operations
- AWS S3 integration
- CSV, JSON, and other file format parsing

## Usage

```java
// Local filesystem
FilesystemBackendMetaData localBackend = new FilesystemBackendMetaData()
   .withName("local-files")
   .withBasePath("/data/files");

// S3
S3BackendMetaData s3Backend = new S3BackendMetaData()
   .withName("s3-files")
   .withBucketName("my-bucket")
   .withBasePath("data/");
```

## License

GNU Affero General Public License v3.0
