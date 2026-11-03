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

## Path semantics

Local disk paths and globs use the host filesystem's separator and case rules.
S3 keys and SFTP paths always use `/` and case-sensitive NIO glob matching,
including `*`, `**`, `?`, character classes, alternatives, and escapes. Jimfs
provides these remote path semantics independently of the host OS; it does not
store the remote files. S3 base paths are literal prefixes, and globs apply to
the keys relative to that prefix. Empty or directory globs list immediate
children; a non-directory glob without metacharacters matches that file name.

Local storage references are relative file paths. Absolute/rooted references and
any `..` component are rejected with `QBadRequestException`, including parent
segments that would normalize back inside the table. This restriction applies to
untrusted storage references, not administrator-configured backend/table base
paths. Ordinary nested references remain supported. Existing symlinks are
resolved before checking the real table boundary; this also applies to ancestors
of new upload targets. A configured symlink base is allowed and defines its real
table as the boundary. Metadata parent segments retain native filesystem
resolution through symlinks; they are not lexically collapsed first. Dangling
links and other resolution errors fail closed.
The host must control directory mutation: validation and subsequent I/O are not
an atomic defense against a local OS actor replacing links concurrently.

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
