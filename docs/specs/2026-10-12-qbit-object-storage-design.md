# QQQ Object Storage qbit: Design Spec

Status: **Approved** · drafted 2026-10-12, owner decisions 2026-10-12 · Author: James Maes (drafted with Claude)
Targets: QQQ 4.1.x (`qqq-backend-module-filesystem` S3 upgrade) and a new repo, **`QRun-IO/qbit-object-storage`** (Apache-2.0).
Related specs: the CMS qbit (#1083), the CDN qbit (#1084) and the backups qbit (#1091) consume this one (§9). When the Alerts qbit is installed, storage problems are raised through it (§9.3).

> Phase P0 (the core S3 upgrade, §8.1) is in progress in QQQ core. The qbit repository has not been created yet; that needs the owner's explicit approval. The first consumer's (kof22) private migration notes live in that app's own repository, not here.

## Owner decisions (2026-10-12)

The owner accepted every recommendation in §13. These decisions override anything below that contradicts them.

| # | Decision |
|---|---|
| D1 | Bucket configuration is **IaC-owned by default** (read-only plus drift check). UI edits only for buckets marked `managedBy = UI`, with `s3.admin`. |
| D2 | **Connections come from code config only.** Secret references must match the allow-list. The UI edits operational fields only. |
| D3 | The **Postgres object index** is the primary browse table (write-through plus scans, live HEAD on the record). A bounded "Live objects" table only for non-indexed buckets. |
| D4 | **Soft delete**: versioning with 30-day noncurrent expiry where supported; a private trash bucket with 30-day expiry where not. Permanent deletes need `s3.admin`. |
| D5 | **Referenced objects are blocked** from delete and move in v1, with no override. |
| D6 | The S3 backend moves to **AWS SDK v2 in 4.1.x**, with a release note for the `S3Utils` public type changes. |
| D7 | **Per-bucket scoping** with a record security lock ships in v1. |
| D8 | v1 uploads go **through the app server**, 100 MB per file by default. Direct browser uploads come in P5. |
| D9 | Usage statistics are **index-derived** in v1. Admin-API plug-ins come later and are opt-in. |
| D10 | CMS keeps `CmsStorage` and this qbit provides the S3 adapter. CDN drops `cdnBackfillObjectHeaders` and uses Apply header policy, `PublicUrlResolver` and `StorageChangeListener`. #1083 and #1084 get follow-up edits. |
| D11 | **Orphaned and missing objects belong to the Storage home.** Data Health gets linking rules only. |
| D12 | Names: `QRun-IO/qbit-object-storage`, `com.kingsrook.qbits.objectstorage`, app label "Storage", table prefix `object_storage_`, permissions `s3.view`, `s3.write`, `s3.delete`, `s3.admin`. **Amended 2026-10-12 (owner):** the table prefix was `storage` (camelCase names such as `storageObject`); the owner naming rule (§5.1, Naming) makes every table lowercase snake_case prefixed with the full qbit name, so `object_storage_object` etc. Permission names are not table names and are unchanged. |
| D13 | Conformance: **MinIO and Garage in CI on every PR**; AWS S3, R2 and B2 in an optional owner-run workflow; others community-reported. |
| D14 | A **separate least-privilege presign key** is required when presigned URLs leave the server. |

## 0. Summary

`qbit-object-storage` gives any QQQ application **S3-compatible object storage as a capability, and an admin app to run it**. It has two jobs:

1. **Provide storage to the app (the service).** Configured connections and buckets, and one Java API for put, get, head, delete, copy, list, metadata and Cache-Control changes, and presigned upload and download URLs. It also builds keys and public URLs consistently (including through a CDN later). Application code and other qbits (CMS, CDN) use this API instead of each building its own S3 client.
2. **Administer storage in the portal (the Storage app).** An **Admin → Storage** sub-app: browse buckets and folders, search objects, preview, download, upload, copy, move, rename, change headers and metadata, generate presigned links, and delete with review. Every write is permission-checked and logged. The home shows bucket sizes, recent changes, orphaned and missing objects, and cache-header coverage.

It works with **any S3-compatible provider**: AWS S3, MinIO, Cloudflare R2, Backblaze B2, Wasabi, DigitalOcean Spaces, Ceph RGW, Garage and others. Each connection carries its own endpoint, addressing, checksum and TLS settings. A **capability model**, filled in by a "Test connection" probe, decides which features the app shows, and the UI says plainly when a provider can't do something.

Design rules:

1. **Data lives in standard, visible QQQ tables** with full record screens, secured by permissions and record security locks. Nothing is hidden.
2. **Processes are table and record actions**, never nav items, with plain labels. Every write action is a standard streamed ETL: record input from selected/filter/all, the standard **Review** step with summary lines, and the standard **Result** step. There are no custom dry-run forms.
3. **The logged-in user's permissions govern every write.** Credentials never reach the browser, and no table stores a secret value.
4. **Kernel pieces go to QQQ core** (the S3 client upgrade in `qqq-backend-module-filesystem`). Everything else, including the UI, lives in the qbit.
5. **Real data only.** Every number on the home comes from the bucket index or the operation log, and empty states say what is missing and why.

---

## 1. Context

### 1.1 Goals

- One storage API and one configuration model for every QQQ app and qbit that stores files.
- An admin app that lets an operator configure, browse, inspect and change objects directly, safely, with an audit trail.
- Provider-neutral: the same code against MinIO in a homelab, Garage on a NAS, R2, B2, or AWS S3.
- A clean migration path for apps that hand-roll S3 code today, with identical behavior and no data moves.

### 1.2 Non-goals (v1)

- Creating or deleting **buckets** from the UI. Bucket lifecycle stays in IaC (§6.3, Q1).
- User and key management on the provider (MinIO users, IAM, R2 tokens). Those belong to the provider's own tooling or IaC.
- Replication, cross-region setup and storage-class transitions.
- Edge caching and invalidation. That is the CDN qbit (#1084); this qbit exposes the hooks it needs (§9.2).
- A general file manager for end users. This is an operator tool.

### 1.3 Current state (researched 2026-10-12)

**QQQ's S3 backend, `qqq-backend-module-filesystem` (`s3/` package):**

| Finding | Effect |
|---|---|
| Built on **AWS SDK for Java v1** (`com.amazonaws:aws-java-sdk-s3:1.12.797`). SDK v1 reached **end of support on 2025-12-31**. | Must move to SDK v2 (§8.1). |
| `S3BackendMetaData` has `bucketName`, `accessKey`, `secretKey`, `region` only. There is **no endpoint override, path-style flag, checksum mode or custom CA**. The default client is hard-wired to `us-east-1`. | The module can only talk to AWS S3. It cannot reach MinIO, R2, Garage or B2 without app-side workarounds. |
| Credentials are plain strings on backend metadata (usually filled from `${env.X}`). **No backend-variant support for S3** (the SFTP backend has it via `SFTPBackendVariantSetting`). | One bucket per backend definition. No per-connection credentials from table rows. |
| `S3Utils.listObjectsInBucketMatchingGlob` lists **every page** under the prefix into memory, then filters, sorts, skips and limits in memory (`AbstractBaseFilesystemAction`). | Fine for hundreds of objects. Not usable as the browse table for large buckets (§5.3). |
| Record fields: path, base name, size, create/modify date, contents (heavy, lazy). **No content type, ETag, Cache-Control, user metadata or tags.** | Too thin for an admin view without extra HEAD calls. |
| `S3StorageAction.makePublic` uses `setObjectAcl(PublicRead)`. | Fails on R2 (no ACLs), on AWS buckets with ACLs disabled (the default since April 2023), and is ignored or partial on several other stores. |
| `getDownloadURL` returns an unsigned `getUrl(...)`. | Not usable for private buckets. |
| Tests use LocalStack and Testcontainers. | Good base for a conformance suite (§11). |

**The first consumer (a QQQ 4.1 website backend on in-cluster MinIO):**

- Two buckets: a **public** media bucket (anonymous download policy, served through a public hostname, ~400 objects today) and a **private incoming** bucket for presigned uploads from a mobile app (lifecycle expiry after 1 day, also aborting incomplete multipart uploads after 1 day).
- Buckets, the public-read policy and the lifecycle rule are created by an **IaC job** in the GitOps repo (`mc mb`, `mc anonymous set`, `mc ilm import`), not by the app.
- The app builds S3 clients in **three places** from environment variables, with path-style addressing forced on. One of them is a second client for presigning against a public hostname. Seven classes touch S3 directly: upload, rendition publishing, photo processing, a presigned-upload service, a Cache-Control backfill, and two import tools.
- Cache-Control is chosen per key by a small policy (immutable for content-addressed keys, short-lived otherwise), and a backfill process rewrites headers in place with copy-object REPLACE.
- No QQQ table uses the S3 backend. There are no Testcontainers-MinIO tests; S3 is faked in unit tests.

### 1.4 Prior art in QRun-IO

- `qbit-sftp-data-integration` puts connection rows in a table and feeds them to the filesystem module through **backend variants**. It stores the SFTP password in a table column. This qbit deliberately does **not** do that (§7.2).
- `qbit-cdn` (#1084) defines `cdnBackfillObjectHeaders`, which rewrites object Cache-Control on the storage origin. That job moves here (§9.2).
- `qbit-cms` (#1083) defines a narrow `CmsStorage` SPI (R7: `put`, `presignPut`, `publicUrl`, `delete`) with S3 and filesystem implementations. This qbit provides the S3 implementation (§9.1).

---

## 2. Architecture

```
            ┌──────────────────────────── QQQ application ────────────────────────────┐
            │                                                                         │
 app code ──┤  ObjectStorage API (bucket alias → typed operations)                    │
 CMS qbit ──┤     │  key builder · header policy · public URL resolver · ref providers  │
 CDN qbit ──┤     │  rate limiter · operation log · index write-through · listeners     │
            │     ▼                                                                   │
            │  S3ClientFactory (QQQ core, filesystem module)                          │
            │     │  SDK v2 S3Client / S3Presigner per connection, cached             │
            │     ▼                                                                   │
            │  Admin → Storage app: storage* tables, record/table actions (ETL), home │
            └─────┼───────────────────────────────────────────────────────────────────┘
                  ▼
     any S3-compatible endpoint (AWS · MinIO · R2 · B2 · Wasabi · Spaces · Ceph · Garage)
```

### 2.1 Where code lives

| Piece | Location |
|---|---|
| SDK v2 migration, `S3ConnectionSettings`, `S3ClientFactory`, endpoint/addressing/checksum/TLS settings, S3 backend variants, bounded listing | QQQ core, `qqq-backend-module-filesystem`, next 4.1.x (§8.1) |
| Optional: `${file.PATH}` secret references in `QMetaDataVariableInterpreter` | QQQ core, `qqq-backend-core` (§8.2) |
| Optional: a `BYTES` display format (1.2 MB) | QQQ core plus Next UI (§8.3) |
| Service API, tables, processes, capability probe, Storage app and home, conformance suite | New repo **`QRun-IO/qbit-object-storage`**, package `com.kingsrook.qbits.objectstorage`, following the `qbit-template` pattern: `ObjectStorageQBitConfig implements QBitConfig`, `ObjectStorageQBitProducer`, table producers as `QBitComponentMetaDataProducer`. Apache-2.0. |
| Preview of video, audio, text and PDF in the `FILE_PREVIEW` lightbox (if not already supported) | Next UI (§10.3) |

One repo, one artifact. The admin app is switched on by config (`withAdminApp(true)`), so a headless consumer (for example the CMS qbit in a small app) gets the service and tables without the sub-app.

### 2.2 Configuration (code is the source of truth)

```java
new ObjectStorageQBitConfig()
   .withTableNamePrefix("object_storage_")
   .withBackendName("rdbms")                       // where storage* tables live
   .withConnection(new StorageConnectionConfig("primary")
      .withProviderType(ProviderType.MINIO)
      .withEndpoint("${env.S3_ENDPOINT}")
      .withPresignEndpoint("${env.S3_PRESIGN_ENDPOINT}")   // optional public host for presigned URLs
      .withRegion("${env.S3_REGION}")
      .withAddressingStyle(AddressingStyle.PATH)
      .withAccessKeyRef("${env.S3_ACCESS_KEY}")
      .withSecretKeyRef("${env.S3_SECRET_KEY}"))
   .withBucket(new StorageBucketConfig("files")            // alias used by code
      .withConnection("primary")
      .withBucketName("${env.S3_BUCKET_NAME}")
      .withRole(BucketRole.PUBLIC)
      .withPublicBaseUrl("${env.PUBLIC_BASE_URL}")
      .withIndexed(true))
   .withBucket(new StorageBucketConfig("incoming")
      .withConnection("primary").withBucketName("${env.INCOMING_BUCKET}")
      .withRole(BucketRole.INCOMING).withIndexed(false))
   .withHeaderPolicy(new QCodeReference(MyHeaderPolicy.class))      // optional, §4.4
   .withReferenceProvider(FieldReferenceProvider.keyPrefix("files", "media", "storageKey"))  // §6.4
   .withAdminApp(true);
```

At startup the producer **upserts** one `object_storage_connection` row and one `object_storage_bucket` row per configured entry (keyed by name/alias), so the tables always show the real configuration. Config-owned rows are marked `source = CONFIG`; their identity and secret-reference fields are read-only in the UI (§7.2, Q2).

---

## 3. Provider neutrality

### 3.1 Per-connection settings

| Field | Values | Notes |
|---|---|---|
| `providerType` | `AWS_S3`, `MINIO`, `CLOUDFLARE_R2`, `BACKBLAZE_B2`, `WASABI`, `DO_SPACES`, `CEPH_RGW`, `GARAGE`, `OTHER` | Seeds default capabilities (§3.3) and sensible defaults below. It never hard-codes behavior the probe can check. |
| `endpoint` | URL | Internal endpoint the server uses. Empty means the AWS regional endpoint. |
| `presignEndpoint` | URL, optional | Host to sign presigned URLs for, when clients reach the store by a different name than the server does. SigV4 signs the `Host` header, so this **must** be the exact host the client will use. |
| `region` | string | AWS region; `auto` for R2; whatever the store expects otherwise (Garage defaults to `garage`, MinIO accepts `us-east-1`). |
| `addressingStyle` | `PATH`, `VIRTUAL_HOST` | Default `PATH` for self-hosted stores, `VIRTUAL_HOST` for AWS, R2, Spaces, Wasabi and B2. |
| `signatureVersion` | `V4` | SigV4 only. SDK v2 has no SigV2 for S3, and every listed provider accepts SigV4. The field exists so the choice is explicit and visible. |
| `checksumMode` | `WHEN_REQUIRED`, `WHEN_SUPPORTED` | Maps to the SDK v2 `requestChecksumCalculation` and `responseChecksumValidation`. **Default `WHEN_REQUIRED` for every provider except `AWS_S3`** (§3.4). |
| `payloadSigning` | `SIGNED`, `UNSIGNED_OVER_TLS` | `UNSIGNED_OVER_TLS` skips payload hashing on HTTPS endpoints for large uploads. Default `SIGNED`. |
| `tlsVerify` | boolean | Default true. False is allowed only for `http://` dev endpoints or with an explicit warning on the record. |
| `caBundleRef` | secret reference, optional | PEM bundle for stores behind a private CA. Builds a custom trust store for the HTTP client. |
| `credentialsMode` | `STATIC_REFS`, `DEFAULT_CHAIN`, `WEB_IDENTITY` | `DEFAULT_CHAIN` and `WEB_IDENTITY` use the SDK provider chain (IAM role, IRSA, pod identity) and need no stored reference. |
| `accessKeyRef`, `secretKeyRef`, `sessionTokenRef` | secret references (§7.2) | Never values. |
| `presignAccessKeyRef`, `presignSecretKeyRef` | secret references, optional | A separate least-privilege key for presigning (§7.3). |
| `adminApiType` | `NONE`, `MINIO_ADMIN`, `GARAGE_ADMIN` | Non-S3 admin APIs for usage and quotas (§3.5). Off by default. |
| `maxOpsPerSecond`, `maxConcurrency` | integers | Rate limits (§7.5). |
| `httpTimeouts` | connect/read ms | Defaults 5 s / 60 s. |

`S3ClientFactory` (core, §8.1) turns these settings into one cached SDK v2 `S3Client` and `S3Presigner` per connection. Clients rebuild when the connection row's settings hash changes.

### 3.2 Capability model

Capabilities are rows in `object_storage_connection_capability` (and per bucket where it varies, in `object_storage_bucket_capability`): `capability`, `status`, `detail`, `source` (`DEFAULT`, `PROBE`, `OVERRIDE`), `probedAt`.

| Capability | Scope | Probe (read-only unless noted) |
|---|---|---|
| `LIST_V2` | connection | `ListObjectsV2` with `max-keys=1` |
| `PUT_GET_DELETE` | bucket | write probe: put, head, get, delete of `.qqq-probe/<uuid>` (only when the user asks and the bucket is writable, §3.6) |
| `COPY_REPLACE_METADATA` | bucket | write probe: copy-in-place with `MetadataDirective=REPLACE`, then head |
| `BATCH_DELETE` | bucket | write probe: `DeleteObjects` with the probe key |
| `MULTIPART` | bucket | write probe: two-part upload at the minimum part size, then abort or complete |
| `PRESIGNED_GET`, `PRESIGNED_PUT` | connection | write probe: presign, then a plain HTTP request to the URL |
| `PRESIGNED_POST` | connection | write probe: POST policy upload |
| `CONDITIONAL_WRITE` | bucket | write probe: `If-None-Match: *` on an existing key must fail with 412 |
| `VERSIONING` | bucket | `GetBucketVersioning` |
| `OBJECT_LOCK` | bucket | `GetObjectLockConfiguration` |
| `LIFECYCLE` | bucket | `GetBucketLifecycleConfiguration` |
| `BUCKET_POLICY` | bucket | `GetBucketPolicy` |
| `CORS` | bucket | `GetBucketCors` |
| `OBJECT_TAGGING` | bucket | `GetObjectTagging` on a listed key |
| `ACL` | bucket | `GetBucketAcl` |
| `USAGE_ADMIN_API`, `QUOTA_ADMIN_API` | connection | Admin API info call, only when `adminApiType` is set |

**Status values** and how a probe response maps to them:

| Status | When | UI effect |
|---|---|---|
| `SUPPORTED` | 200, or the documented "not configured" error (e.g. `NoSuchLifecycleConfiguration`, `NoSuchCORSConfiguration`, `NoSuchBucketPolicy`) | Feature shown |
| `UNSUPPORTED` | 501 `NotImplemented`, 400/405 with a "not implemented"/"not supported" code, or a stub the provider documents (e.g. versioning always "not enabled") | Feature hidden; a one-line note on the bucket record says the provider does not support it |
| `NOT_PERMITTED` | 403 `AccessDenied` | Feature shown read-only with "the configured key may not do this" |
| `PARTIAL` | supported with documented limits (lifecycle subset, canned ACLs only) | Feature shown; the Review step lists the limits |
| `UNKNOWN` | never probed, or an unexpected error (kept in `detail`) | Treated as unsupported for writes, shown with "untested" |

The probe never changes bucket configuration. It only uses `Get*` calls for bucket settings, and object writes go to a single `.qqq-probe/` key that is always deleted afterwards.

### 3.3 Known provider differences (seed defaults; the probe overrides them)

Legend: ✅ supported · ❌ not supported · ◐ partial · ? not documented clearly, probe decides. Sources are in §15; where a provider's documentation is silent, the cell is `?` rather than a guess.

| | AWS S3 | MinIO | R2 | B2 | Wasabi | DO Spaces | Ceph RGW | Garage |
|---|---|---|---|---|---|---|---|---|
| Default addressing | virtual | path | virtual (path works) | virtual | virtual | virtual | path | path (vhost with `root_domain`) |
| Region | real region | any (`us-east-1`) | `auto` | from endpoint | from endpoint | from endpoint | any | configured (`garage` default) |
| Versioning | ✅ | ✅ | ❌ | ◐ native B2 versions; S3 API ? | ✅ | ✅ | ✅ | ❌ (stub reports "not enabled") |
| Object lock | ✅ | ✅ (bucket created with lock) | ❌ (S3 API) | ? | ✅ | ? | ✅ | ❌ |
| Lifecycle | ✅ | ✅ | ◐ `*LifecycleConfiguration` only | ? | ? | ◐ | ✅ | ◐ `Expiration` and `AbortIncompleteMultipartUpload` only |
| Bucket policy | ✅ | ✅ (subset) | ❌ | ? | ✅ | ◐ | ✅ | ❌ |
| CORS via S3 API | ✅ | ? (historically a server-wide setting) | ✅ | ? | ✅ | ✅ | ✅ | ✅ |
| ACLs | ◐ disabled by default on new buckets (Object Ownership = BucketOwnerEnforced) | ◐ | ❌ | ◐ bucket private/public-read only; no object ACLs | ◐ | ◐ canned | ✅ | ❌ |
| Object tagging | ✅ | ✅ | ❌ | ❌ (Get returns empty, Put header rejected) | ✅ | ? | ✅ | ❌ |
| Presigned POST | ✅ | ✅ | ? | ❌ | ? | ? | ✅ | ✅ |
| Non-S3 admin API | (IAM, CloudWatch) | MinIO Admin API (`madmin`): usage, quotas, users, policies | (Cloudflare API) | (B2 native API) | (Wasabi API) | (DO API) | RGW Admin Ops API | Garage Admin API: buckets, keys, quotas |
| Notable | ACLs off and Block Public Access on by default | Admin features are not S3 (see below) | Many checksum headers unimplemented; no ACL headers on Put/Copy | Object-level ACL headers ignored | **Minimum storage duration billing**: deleted objects are still billed for the minimum period | | Broadest compatibility of the self-hosted stores | Single-region, simple; many bucket-config APIs missing |

**Operational notes the design must respect:**

- **MinIO admin APIs are not S3.** Bucket quotas, usage totals, users and policies come from the MinIO Admin API (`mc admin ...`), which needs admin credentials. v1 does not call it (§3.5, Q9).
- **MinIO's community edition changed during 2025** (admin features removed from the bundled web console, and changes to how community builds are distributed). Operators pinning an older release should plan an exit. Garage and Ceph are the self-hosted alternatives this qbit already targets. Verify the current state before relying on it.
- **AWS changed defaults in April 2023.** New buckets have ACLs disabled and Block Public Access on, so "make public" must be a bucket policy (IaC), never an object ACL.
- **Wasabi bills deletes.** The Review step for deletes on a Wasabi connection says so.

### 3.4 The SDK v2 checksum trap

AWS SDK for Java v2 **2.30.0 (January 2025)** turned on default data-integrity protection: `requestChecksumCalculation` and `responseChecksumValidation` default to `WHEN_SUPPORTED`, so `PutObject` and `UploadPart` send CRC32 checksums, often as aws-chunked **trailing** checksums. Many S3-compatible stores reject or mishandle these (unsupported `x-amz-checksum-*` or `x-amz-trailer` headers, or chunked encoding), and the failure looks like a signature or 400 error, not a checksum error. R2's compatibility table, for instance, lists most checksum headers as unimplemented.

The design therefore:
- defaults `checksumMode = WHEN_REQUIRED` for every provider except `AWS_S3`;
- covers both modes in the conformance suite (§11), so a store that starts supporting checksums can be switched to `WHEN_SUPPORTED` with evidence;
- shows the mode on the connection record, with a note explaining it.

### 3.5 Usage statistics without admin APIs

Bucket size and object count come from the **object index** (§5), which is exact for indexed buckets and costs only `ListObjectsV2` calls. `object_storage_usage_daily` snapshots those totals once a day. Admin-API-based usage (MinIO, Garage, Ceph) is a later, optional plug-in (P5), because it needs admin credentials that the app should not hold by default.

### 3.6 Test connection

A record action on `object_storage_connection`, **"Test connection"**, as a standard ETL over the connection's buckets:
- **Review** lists what will be probed: read-only bucket-setting probes always; write probes only when the user ticks "Include write tests" and the bucket is not protected.
- **Result** writes the capability rows, sets `lastTestedAt` and `lastTestStatus`, and shows one summary line per bucket ("8 supported, 3 unsupported, 1 not permitted").
- It also reports clock skew (from the `Date` response header), because SigV4 fails beyond 15 minutes of skew, and whether the presign host resolves.

---

## 4. The service API (job 1)

### 4.1 Shape

```java
StorageBucket files = ObjectStorage.bucket("files");          // alias from config

PutResult  r = files.put(PutRequest.of("media/42/hero/" + sha + "/original.jpg", bytesOrStream)
                         .withContentType("image/jpeg")
                         .withContentLength(len)               // stream path: multipart above 16 MB
                         .withMetadata(Map.of("alt", "...")));  // headers from the policy unless given
Optional<StoredObject> o = files.head(key);
try(ObjectStream s = files.get(key, Range.all())) { ... }
files.copy(CopyRequest.of(src, dst).withMetadata(MetadataDirective.COPY));
files.updateHeaders(key, ObjectHeaders.cacheControl("public, max-age=31536000, immutable"));
files.delete(key);                                            // honors versioning/trash, §6.5
Page<ObjectSummary> p = files.list(ListRequest.prefix("media/42/").delimiter("/").pageSize(1000).after(token));
URL up   = files.presignPut(PresignPut.of(key).contentType("image/jpeg").contentLength(len).ttl(Duration.ofMinutes(15)));
URL down = files.presignGet(PresignGet.of(key).ttl(Duration.ofMinutes(5)).downloadAs("photo.jpg"));
String url = files.publicUrl(key);                            // via PublicUrlResolver, §4.5
```

- All operations are synchronous, use the connection's cached client, and go through the rate limiter (§7.5).
- `put` with a stream and unknown length uses multipart above 16 MB. Part size and parallelism are per-connection settings within the provider's limits (5 MiB minimum part, 10,000 parts).
- `presignPut` can sign `Content-Length` and `Content-Type`, which pins the upload to that exact size and type. A presigned PUT cannot express a *maximum* size. Where `PRESIGNED_POST` is supported, `presignPost` with a `content-length-range` policy does, and the API exposes it with an honest fallback ("not supported on this provider; verify size after upload").
- Every call returns typed results and throws `StorageException` with a stable `reason` (`NOT_FOUND`, `ACCESS_DENIED`, `UNSUPPORTED`, `PRECONDITION_FAILED`, `THROTTLED`, `PROVIDER_ERROR`) and the provider's request ID. No raw SDK exception leaks out.

### 4.2 Who may call it

The API runs in-process with the current `QContext` session.
- **Admin-app actions** check `s3.*` permissions and bucket locks before calling the API (§7.1).
- **Application code** (an upload endpoint, a publishing job) calls it as trusted server code, like any QQQ action. When QQQ core's opt-in `PermissionEnforcement.ENFORCE` (#1061) is on, the API applies the same `s3.*` checks to the session's user. A system session (scheduled jobs) is allowed and logged as such.
- Every write, from any caller, lands in `object_storage_operation` (§6.6).

### 4.3 Keys

`StorageKeys` builds and validates keys:
- normalizes `//`, strips a leading `/`, rejects `..` segments, control characters and keys over 1,024 bytes;
- offers a content-addressed template helper (`{scope}/{slug}/{sha256}/{name}`) and a safe-filename helper;
- percent-encodes correctly when building URLs (`+`, spaces, unicode), the most common source of "works on MinIO, 403 on AWS" bugs.

Apps keep their own key layouts. The helpers only make them consistent and safe.

### 4.4 Header policy

`ObjectHeaderPolicy` decides the headers for a new or rewritten object:

```java
public interface ObjectHeaderPolicy
{
   ObjectHeaders headersFor(StorageBucketConfig bucket, String key, String contentType);
}
```

- The default implementation reads the **`object_storage_header_rule`** table: bucket, key glob or regex, content-type pattern, Cache-Control, Content-Disposition, priority. It is ordinary data, editable with `s3.admin`, and audited.
- An app can plug in its own policy in code, for example one driven by per-media-type settings it already keeps. The qbit does not own those settings.
- `put` applies the policy unless the caller passes explicit headers. The **"Apply header policy"** action (§6.2) rewrites existing objects to match it, which replaces app-specific backfills and the CDN qbit's `cdnBackfillObjectHeaders`.
- **Coverage** (home, §10.2) is the share of indexed objects whose observed Cache-Control equals the policy's answer.

### 4.5 Public URLs

`PublicUrlResolver` maps (bucket, key) to the URL the world uses. The default is `publicBaseUrl + "/" + encodedKey`. The CDN qbit supplies a resolver that returns the CDN hostname per its rules, so switching media to a CDN is a configuration change, not a code change. `parse(url)` is the reverse mapping, used by reference providers that store URLs instead of keys (§6.4).

### 4.6 Change listeners

`StorageChangeListener` is called after every successful write (put, copy, header change, delete, restore) with the bucket, key and operation. The CDN qbit registers one to plan purges. Listeners run after the write and never fail it. Their errors are logged on the `object_storage_operation` row.

---

## 5. Mapping to QQQ tables

### 5.1 Tables

All tables are standard QQQ tables in the app's backend (RDBMS), prefixed `object_storage_` by default (lowercase snake_case; QQQ table name = backend name), with full record screens.

**Naming (owner rule, 2026-10-12).** "table names should always be lowercase and use _, and qbits should always be prefixed with their name, ie, cdn_ etc". Every table here is lowercase snake_case with the full qbit name as prefix (`object_storage_`, not `storage` or `s3`). The QQQ table name and the backend (DB) table name are identical, and possible-value sources take the table name. Java class names stay CamelCase.

| Table | What a row is | Written by | Notes |
|---|---|---|---|
| `object_storage_connection` | an S3 endpoint and its settings (§3.1) | config upsert; `s3.admin` for non-identity fields | No secret values. Shows "credentials resolved: yes/no" as a computed field. |
| `object_storage_connection_capability` | one capability's status for a connection | Test connection | Child of connection. |
| `object_storage_bucket` | a bucket alias and its config (role, public base URL, indexed, trash, managed-by, limits) plus observed state (versioning, lifecycle, policy, CORS summaries; object count, bytes, last scan) | config upsert; scan; "Check configuration" | Bucket record is the folder-tree root (§5.4). |
| `object_storage_bucket_capability` | per-bucket capability | Test connection | Child of bucket. |
| `object_storage_folder` | a prefix in an indexed bucket, with object count, bytes and newest change, rolled up | scan and write-through | `parentFolderId` gives the tree. |
| `object_storage_object` | one object in an indexed bucket: key, folder, name, extension, size, content type, ETag, last modified, storage class, Cache-Control, Content-Disposition, user metadata (JSON), tags (JSON), version ID, `referenceCount`, `protection`, `headersKnown` | scan, HEAD refresh, write-through | **The object table.** Its fields mirror S3 and are read-only in the edit screen. Changes go through actions (§6.2). |
| `object_storage_object_version` | a non-current version or delete marker | on demand ("Show versions") | Only for versioned buckets. |
| `object_storage_object_reference` | a record that points at a key or prefix | reference scan (§6.4) | `objectId` is null when the referenced object does not exist: a **missing** object. |
| `object_storage_header_rule` | a header policy rule (§4.4) | `s3.admin` | Audited. |
| `object_storage_protection_rule` | a protected bucket/prefix (§6.3) | `s3.admin` | Audited. |
| `object_storage_operation` | one write against storage: who, when, action, bucket, key, destination key, bytes, result, error, provider request ID, process run | the service, always | Read-only for everyone. The storage audit log. |
| `object_storage_usage_daily` | per bucket per day: object count, bytes, by top-level prefix | nightly job | Feeds the usage trend. |

### 5.2 Why an index table rather than a live S3-backed table

S3 listing is a forward-only cursor: `ListObjectsV2` returns at most 1,000 keys per call, sorted by key, filterable by prefix only. It has no server-side sort, no filter on size, date or content type, no count, and no random-access page N. Content type and Cache-Control are not in the listing at all; each needs a HEAD.

A QQQ table needs filters, sorts, counts, paging and joins (for orphans and missing objects). So the primary table is an **index**: a real QQQ table kept in step with the bucket.

- **Full scan**: "Rescan bucket" action and a nightly schedule. Streams `ListObjectsV2` pages, upserts rows in batches, marks each row with the scan generation, and at the end removes rows not seen (with a safety stop if more than a configurable share would be removed, which usually means a wrong prefix or credentials). The cursor is saved on the bucket row so a large scan resumes after a restart.
- **HEAD backlog**: objects whose headers are unknown get HEADed in the background under the rate limit, oldest first. The home shows the backlog honestly ("headers known for 82% of objects").
- **Write-through**: every write through the service updates the index immediately. An index failure never fails the write; the next scan repairs it.
- **Drift**: writes made outside QQQ (CLI, provider console, presigned uploads) appear at the next scan. Each object record shows "indexed at" and has **"Refresh from bucket"**. Provider event notifications (MinIO webhooks, S3 to SQS) are a later option (P5).
- **Scale**: Postgres handles millions of rows comfortably. Buckets above a configurable cap (default 5 million keys) index only listed prefixes. Short-lived buckets (for example an incoming-uploads bucket with 1-day expiry) are not indexed at all.

The S3 backend table from `qqq-backend-module-filesystem` remains available once upgraded (§8.1). The qbit uses it for an optional **"Live objects"** table on non-indexed buckets: a bounded, prefix-scoped, key-ordered listing for debugging, with no count and no arbitrary filters, labeled as such.

### 5.3 Paging, search and filters

All on the index, with standard QQQ query screens: key contains or starts with, folder, extension, content type, size range, last-modified range, Cache-Control present or missing, referenced or not, protected or not. Indexes: `(bucketId, key)` unique, `(bucketId, folderId)`, `(bucketId, lastModified)`, `(bucketId, size)`, `(bucketId, contentType)`. If Quick Search is installed, `object_storage_object` registers as a searchable table (key and name) so objects show up in global search. No separate search UI.

### 5.4 Folders without a custom browser

The tree uses standard record screens:
- The **bucket** record shows its top-level folders and top-level objects as child-record lists.
- A **folder** record shows a breadcrumb (its parent chain as links), its subfolders and its objects as child-record lists, with counts and sizes.
- Object rows show a thumbnail via `FILE_PREVIEW` (§10.3), size, type, last modified and Cache-Control.

That gives "click into a folder, see what's there, go up" with no custom component. A dedicated two-pane file browser is not planned unless the standard screens prove insufficient in use.

---

## 6. Actions and safety

### 6.1 Permissions and locks

| Permission | Grants |
|---|---|
| `s3.view` | Read all storage tables; preview, download, presign GET |
| `s3.write` | Upload, copy, move/rename, change headers and metadata, edit tags, apply header policy, presign PUT, restore |
| `s3.delete` | Delete objects (soft where possible, §6.5) |
| `s3.admin` | Edit connections (non-identity fields), header rules, protection rules; Test connection with write tests; bucket configuration changes where allowed (§6.3); permanent deletes; runs over the bulk cap |

Per-bucket scoping uses a **`RecordSecurityLock`** on `bucketId` (and `connectionId` on connection tables), with a security key type `object_storage_bucket`. A user without the key value for a bucket sees none of its rows and cannot run actions on them. Apps with one administrator grant all-access.

### 6.2 Actions

Every write is a **standard streamed ETL** (`StreamedETLWithFrontendProcess`): input from the selected rows, the current filter, or all rows; a transform step that produces the **Review** summary lines in validate mode; a load step that does the work; and the standard **Result** with per-line counts and links to the affected rows and `object_storage_operation` entries. Labels are plain.

| Action (label) | On | Permission | Review shows |
|---|---|---|---|
| **Upload files** | `object_storage_object` table, `object_storage_folder` record | `s3.write` | files and sizes, destination prefix, overwrites, headers the policy will apply |
| **Copy objects** | `object_storage_object` | `s3.write` | count, bytes, destination bucket/prefix, overwrites, objects over 5 GB (multipart copy) |
| **Move or rename objects** | `object_storage_object`, `object_storage_folder` (rename folder) | `s3.write` + `s3.delete` | count; that a move is copy then delete, not atomic; referenced objects that will be **skipped** (§6.4) |
| **Change headers and metadata** | `object_storage_object` | `s3.write` | Cache-Control, Content-Type, Content-Disposition, user metadata before → after; that copy-in-place changes Last-Modified and can change the ETag |
| **Apply header policy** | `object_storage_object`, `object_storage_bucket` | `s3.write` | already correct / will change / failed-to-read counts |
| **Edit tags** | `object_storage_object` | `s3.write` | shown only when `OBJECT_TAGGING` is supported |
| **Create presigned link** | `object_storage_object` record | `s3.view` (GET), `s3.write` (PUT) | method, TTL (capped by bucket max), who it is for; the Result shows the URL once |
| **Delete objects** | `object_storage_object`, `object_storage_folder` | `s3.delete` | will delete (soft) / will delete permanently / skipped: referenced / skipped: protected / skipped: object lock until date; on Wasabi, the minimum-duration billing note |
| **Restore objects** | `object_storage_object` (deleted), `object_storage_object_version` | `s3.write` | delete markers to remove, or trash copies to move back |
| **Refresh from bucket** | `object_storage_object` | `s3.view` | count to HEAD |
| **Rescan bucket** | `object_storage_bucket` | `s3.view` | prefix, last scan, expected list calls |
| **Check configuration** | `object_storage_bucket` | `s3.view` | observed versioning, lifecycle, policy, CORS; drift against the expected values in config |
| **Test connection** | `object_storage_connection` | `s3.view` (read probes), `s3.admin` (write probes) | §3.6 |
| **Download** | object record | `s3.view` | not a process: a `FILE_DOWNLOAD` link to a permission-checked backend route that redirects to a 5-minute presigned GET |

Bulk runs are capped at 10,000 objects per run (configurable). Above the cap needs `s3.admin`. Presigned URLs are logged in `object_storage_operation` **without the signature**: key, method, expiry and issuer only, because the URL itself is a bearer token.

### 6.3 Bucket configuration: observe by default, change only when the bucket is UI-managed

Each bucket has `managedBy`: `IAC` (default) or `UI`.
- **`IAC`**: versioning, lifecycle, policy and CORS are shown read-only from "Check configuration", with a drift flag against the expected values the app's config declares. The record says where the source of truth is.
- **`UI`**: `s3.admin` can run **Change versioning**, **Edit lifecycle rules**, **Edit CORS rules** and **Edit bucket policy**, each a standard ETL whose Review shows the current and new JSON side by side, and each only when the capability is `SUPPORTED` or `PARTIAL`.
- Bucket creation and deletion stay out of scope (§1.2).

### 6.4 References: orphans, missing objects and referenced-object protection

`StorageReferenceProvider` lets an app declare which records point at storage:

```java
public interface StorageReferenceProvider
{
   Stream<StorageReference> references(String bucketAlias) throws QException;
   // StorageReference(bucketAlias, keyOrPrefix, matchMode KEY|PREFIX, tableName, recordId, fieldName, label)
}
```

- Most apps need no code: **`FieldReferenceProvider`** reads a table field holding a key, a key prefix (a rendition set), or a public URL (parsed back with `PublicUrlResolver.parse`).
- The **reference scan** runs after each bucket scan and rebuilds `object_storage_object_reference`, then sets `object_storage_object.referenceCount`.
- **Orphaned** = indexed object in a reference-tracked bucket with `referenceCount = 0`, outside exempt prefixes.
- **Missing** = reference row with no matching object (no key match, or a prefix with zero objects).
- **Protection**: referenced objects are **skipped** by Delete and Move, and the Review lists the referencing records with links. Header and metadata changes are allowed. There is no override in v1 (Q5): fix or delete the referencing record first.

### 6.5 Soft delete

| Bucket state | What Delete does | Undo |
|---|---|---|
| Versioning enabled | Creates a delete marker | **Restore** removes the marker |
| Not versioned, trash configured | Copies to the trash location, then deletes | **Restore** copies back |
| Neither | Deletes permanently, needs `s3.admin`, and the Review says "permanent" on its own line | none |

- The **trash location** must be a private bucket, or a prefix in a private bucket. A trash prefix inside a public bucket would leave "deleted" files publicly readable, so the config validator rejects it.
- The trash is expired by a lifecycle rule where `LIFECYCLE` is supported, otherwise by a nightly purge job (default 30 days).
- **Permanently delete versions** is a separate `s3.admin` action on `object_storage_object_version`.
- Object-lock retention and legal holds are shown read-only, and the Review explains why locked objects are skipped.

### 6.6 Audit

- `object_storage_operation` records every storage write from every caller (UI actions, app code, scheduled jobs): user or system, action, keys, bytes, outcome, provider request ID, and the process run ID when there is one.
- Config tables (`object_storage_connection`, `object_storage_bucket`, `object_storage_header_rule`, `object_storage_protection_rule`) use standard QQQ record audit, so changes show in Admin → Audit.
- The Storage home shows recent changes from `object_storage_operation` and links to the full table. It does not duplicate the Audit app.

### 6.7 Protection rules

`object_storage_protection_rule`: bucket, prefix glob, mode (`READ_ONLY`, `NO_DELETE`, `WARN`), reason, and optional `allowedSystemCallers` (for example `backups`, whose system-context permanent delete may pass a `NO_DELETE` rule, §9.4). Actions skip matching objects (or warn, for `WARN`) and say why in the Review. Changing a rule needs `s3.admin` and is audited. Typical use: an IaC-owned prefix, or legal/retention material.

---

## 7. Security

### 7.1 Enforcement

- Process permissions gate each action, and `RecordSecurityLock` on `bucketId` gates rows.
- The load step re-checks the permission and the bucket lock per record before each provider call (defense in depth, the same pattern the Assistant kernel uses).
- The backend download and preview route checks `s3.view` and the bucket lock, then redirects to a 5-minute presigned GET. Public-bucket objects can link straight to the public URL.

### 7.2 Secrets are references, never values

- Connection rows store **references** like `${env.S3_SECRET_KEY}` (and `${file./var/run/secrets/...}` once §8.2 lands). Resolved values live only in the in-memory client and are never written to a record, a log line, or a process value.
- `toString()` on all config classes redacts references and values.
- **Reference allow-list.** A user who could edit a connection's endpoint and point its secret reference at any environment variable could send that value to a server they control (the access key ID travels in clear in the `Authorization` header). So references must match `allowedSecretRefPatterns` from code config (default: `env.S3_*`, `env.STORAGE_*`, `file./var/run/secrets/storage/*`), and changing `endpoint` or `presignEndpoint` on a connection needs `s3.admin`, is audited, and invalidates the cached client.
- Whether connections can be **created** in the UI at all is Q2. The recommendation is no: config only.

### 7.3 Presigned URLs

- A presigned URL contains the **access key ID** (`X-Amz-Credential`), the expiry and the signature. Use a dedicated least-privilege key for presigning (`presignAccessKeyRef`), allowed only `PutObject` on the upload bucket or `GetObject` on the read bucket, never an administrator or root key.
- Maximum TTLs are per bucket (defaults: GET 1 hour, PUT 15 minutes; SigV4 allows at most 7 days).
- Per-user caps on presigns per hour (default 100), configurable.

### 7.4 Credentials in the browser

The browser never receives credentials. It receives presigned URLs (time-limited, single-object) and public URLs only.

### 7.5 Rate limits

- Per connection: `maxOpsPerSecond` (default 20) and `maxConcurrency` (default 4), a token bucket in each app instance. Multi-replica apps get per-replica limits, which is acceptable for an operator tool.
- Provider throttling (`SlowDown`, 503) triggers exponential backoff with jitter (SDK v2 adaptive retry), and the Result reports throttled retries.
- Per user: bulk-run cap (§6.2), presign cap (§7.3), and at most one running scan per bucket.

---

## 8. QQQ core changes (4.1.x)

### 8.1 `qqq-backend-module-filesystem`: S3 upgrade (kernel)

1. **Move to AWS SDK for Java v2** (`software.amazon.awssdk:s3`, plus `apache-client`; no CRT native dependency). SDK v1 is out of support. This changes the public types on `S3Utils.getAmazonS3()/setAmazonS3(...)`, so it is called out in the release notes (Q6).
2. **`S3ConnectionSettings`** (endpoint, presign endpoint, region, addressing, checksum mode, payload signing, TLS verify, CA bundle, credentials mode and references, timeouts) and **`S3ClientFactory`** (cached `S3Client`/`S3Presigner` per settings hash). `S3BackendMetaData` gains these fields; existing apps that set only bucket/key/secret/region keep working.
3. **Backend variants for S3** (`S3BackendVariantSetting`: endpoint, region, bucket, access key, secret key, addressing), mirroring SFTP, so a table can target a connection row.
4. **Bounded listing.** Pass `prefix`, `startAfter` and `maxKeys` down for queries that are key-ordered with a limit. Fail clearly above `maxListedObjects` (default 100,000) instead of loading a whole bucket into memory.
5. **Optional extra record fields**: content type, ETag and Cache-Control, filled from HEAD on single-record GETs only.
6. **`makePublic`** stops using object ACLs; it throws a clear `UNSUPPORTED` error that points to bucket policy.
7. **`getDownloadURL`** returns a presigned GET with a configurable TTL for private buckets.

Estimated effort: 6–8 days including tests.

### 8.2 `${file.PATH}` secret references (small, optional)

`QMetaDataVariableInterpreter` supports `${env.X}`, `${prop.X}` and `${literal.X}`. Adding `${file.PATH}` (read a mounted Kubernetes secret file, trimmed, cached) lets apps avoid putting secrets in environment variables. About 1 day. Useful to every qbit with credentials (CDN, Authentik, Assistant).

### 8.3 `BYTES` display format (small, optional)

A `DisplayFormat.BYTES` that renders `1234567` as `1.2 MB`, implemented in core and the Next UI. Used for sizes everywhere in this app. About 1 day across both.

No other core changes are needed. Permissions, locks, record audit, streamed ETL, `FILE_PREVIEW`, `FILE_DOWNLOAD` and child-record widgets already exist.

---

## 9. Consumers

### 9.1 CMS qbit (#1083)

The CMS spec defines `CmsStorage` (R7: `put`, `presignPut`, `publicUrl`, `delete`) with S3 and filesystem implementations. Recommendation (Q10):
- `CmsStorage` stays as the CMS's narrow SPI, so a CMS install without S3 still works with the filesystem implementation.
- `qbit-object-storage` ships `ObjectStorageCmsStorage`, a small adapter, so the CMS's S3 path is this qbit rather than a fourth hand-rolled client.
- The CMS registers `FieldReferenceProvider`s for its media and photo tables, which gives orphan, missing and referenced-object protection for free.

### 9.2 CDN qbit (#1084)

- **Drop `cdnBackfillObjectHeaders`.** The generic **Apply header policy** action (§6.2) does the same job. The CDN app home links to it.
- The CDN qbit provides a `PublicUrlResolver` (CDN hostnames) and a `StorageChangeListener` (purge planning on object change).
- Cache-header **policy** values stay wherever the app keeps them (§4.4). The CDN app owns edge cache policy. This qbit only shows object-header coverage and links to the CDN app.

Both specs need a short follow-up edit once this design is accepted.

### 9.3 Alerts qbit (optional, `docs/specs/2026-10-12-qbit-alerts-design.md`, in progress)

When the Alerts / Manage-by-Exception qbit is installed, this qbit raises alerts through its **producer SPI**. It detects the qbit at startup (class and metadata presence) and binds a small `StorageAlertSink` adapter. Without the Alerts qbit the sink is a no-op, and every condition below is still visible on the Storage home and in the tables. Nothing depends on alerts being delivered.

| Alert | Raised when | Resolves when | Default severity |
|---|---|---|---|
| `storage.objects.missing` | a reference scan finds referenced keys with no object (per bucket) | a later scan finds none | error |
| `storage.objects.orphaned` | orphaned objects in a reference-tracked bucket exceed a threshold (count or bytes, configurable) | below the threshold | warning |
| `storage.operation.failed` | a storage write fails after retries (app code or action), or a bulk run ends with failures | auto-resolves after 24 h with no new failures | warning (error for repeated failures on one key) |
| `storage.quota.near` | bucket bytes or object count cross a configured share of a quota (config value, or admin-API quota where available) | back under the threshold | warning at 80%, error at 95% |
| `storage.connection.failed` | Test connection, a scan, or N consecutive calls fail with auth, TLS, DNS or clock-skew errors | the next successful call | error |
| `storage.scan.stale` | an indexed bucket has not completed a scan in 48 h, or a scan hit the mass-removal safety stop | the next completed scan | warning |

- Each alert carries a stable dedupe key (alert type + connection/bucket), counts and a link to the filtered storage table or record, never secret values or presigned URLs.
- Subscriptions, channels (Slack optional) and routing belong to the Alerts qbit. This qbit only produces.
- The thresholds live on `object_storage_bucket` and `object_storage_connection` rows, so they are ordinary audited data.

### 9.4 Backups qbit (#1091): storage follow-ups SF1–SF5

The backups qbit writes backup sets through this service (#1091 §6, Q19 option (a)). It needs five small additions. They land in **storage P1–P2**, so backups never needs a workaround.

| # | Addition | Lands in | Notes |
|---|---|---|---|
| SF1 | **`BucketRole.BACKUP`**: always private, never public; excluded from the header policy, Apply header policy, and orphan and missing scans | P1 | The config validator rejects a `BACKUP` bucket with a public base URL or a public policy. Backup objects are not app media, so they are never "orphaned" and never get Cache-Control rewritten. |
| SF2 | **`PutRequest.withObjectLock(mode, retainUntil)`** and **`withStorageClass(...)`** | P1 | Object lock is offered only when the bucket's `OBJECT_LOCK` capability is `SUPPORTED`. Otherwise the call fails with `UNSUPPORTED`, so a lock is never silently dropped. |
| SF3 | **Permanent delete for system callers**: `delete(DeleteRequest.of(key).permanent())` deletes every version and delete marker of the key | P2 | It exists only in a system-caller context (SF5). The calling qbit's own permission check gates it (backups: `backups.admin`), not `s3.delete`. Objects under object-lock retention are skipped, and the result says so. It bypasses the `NO_DELETE` protection rule only for the context named in that rule's `allowedSystemCallers`. |
| SF4 | **Probes and locked buckets**: write probes (§3.6) skip `BACKUP` buckets that have object lock on. Alternatively they use a `.qqq-probe/` prefix that the bucket's default retention excludes. | P1 | A probe object in a locked bucket could not be deleted. The probe result records "write probes skipped: object lock". |
| SF5 | **System-caller context**: `ObjectStorage.asSystem("backups", runId).bucket(alias)` | P2 | The calls run in a system context tagged with the caller and run. `object_storage_operation` records "system: backups run 1234 (started by user X)". User `s3.*` permissions don't apply, so a backups operator doesn't also need `s3.write` on the backup bucket. The caller qbit must have checked its own permission first. |

Until these land, backups can fall back to role `PRIVATE` plus a `NO_DELETE` protection rule, and prune by lifecycle expiry (#1091 R12).

### 9.5 Migrating an app's hand-rolled S3 code

The pattern, with **identical behavior and no data moves**:

1. Install the qbit with connections and buckets whose settings reproduce today's environment variables exactly (same endpoint, region, addressing, bucket names, public base URL, presign host).
2. Replace each hand-built client with `ObjectStorage.bucket(alias)`. Key layouts stay byte-for-byte the same.
3. Move the app's Cache-Control rules into `object_storage_header_rule` rows or a code `ObjectHeaderPolicy` that returns the same values.
4. Replace app-specific header backfills with **Apply header policy**.
5. Register `FieldReferenceProvider`s for tables that hold keys or URLs.
6. Prove equivalence with a Testcontainers MinIO test per call site: same keys, same headers, same presigned URL host and signed headers.
7. Remove the old client builders.

Downtime during cutover is acceptable for the first consumer; no zero-downtime machinery is planned. The first consumer's file-level plan lives in its own repository.

---

## 10. The Storage app

### 10.1 Placement

Under **Admin → Storage**, alongside the existing Admin sub-apps, shown only when the qbit is installed with `withAdminApp(true)`. Nav entries are the home and the tables (Buckets, Objects, Folders, Connections, Operations, Header rules, Protection rules). No processes in nav.

### 10.2 Home (the single place for storage data)

Full-width sections, stacked, readable at every width with no inner horizontal scroll:

1. **Buckets.** One row per bucket: name, connection, role, objects, size, last scan, versioning, capability chips (for example "no versioning", "lifecycle partial"). Each row links to the bucket record.
2. **Data health.** Orphaned objects (count and bytes) and missing objects (count), per bucket, linking to filtered `object_storage_object` and `object_storage_object_reference` views. If the app has a Data Health area, it gets two rules that point here. It does not get a second copy.
3. **Recent changes.** The latest 20 `object_storage_operation` rows (who, what, when, outcome), with a link to the full table.
4. **Cache headers.** One line per bucket: "N of M objects match the header policy; headers known for X%". It links to **Apply header policy** and, when the CDN qbit is installed, to the CDN app. It does not repeat CDN data.
5. **Usage trend.** Bytes and object count per bucket over 30 days from `object_storage_usage_daily`. Until there are two days of snapshots, it says "collecting since <date>" rather than drawing a line.
6. **Connections.** Last test time and result per connection, with a nudge if never tested or tested more than 30 days ago.

### 10.3 Object record screen

- Header: key, size, type, last modified, ETag, referenced-by count, protection.
- **Preview** via `FILE_PREVIEW`, whose URL field points at the permission-checked preview route (§7.1). Images show inline; the lightbox should also handle **video, audio, PDF and text** (the first 256 KB). If the Next lightbox only handles images today, that is the one Next UI change this app needs.
- Sections: headers and user metadata, tags (when supported), referencing records (child list), versions (when versioned), recent operations on this key.
- Record actions as in §6.2.

### 10.4 Custom UI

None in v1. Later (P5): a direct-to-bucket upload widget (presigned multipart with progress) for files too large to send through the app server. It needs bucket CORS, so it is only offered where `CORS` is supported and configured.

---

## 11. Conformance test suite

An abstract JUnit 5 suite, `ObjectStorageConformanceSuite`, parameterized by a `ConformanceTarget` (connection settings, a scratch bucket, and the **expected** capability set). It runs the service API end to end:

- put, head, get (full and range), delete; 0-byte, 5 MiB, 20 MiB (multipart) objects;
- keys with spaces, `+`, `%`, unicode, `//` and trailing dots;
- list paging over 2,500 keys (three pages), delimiter listing, start-after;
- copy and copy-in-place with REPLACE metadata; Cache-Control round trip;
- batch delete, and the single-delete fallback where batch is unsupported;
- presigned GET and PUT via a plain HTTP client, including signed Content-Type and Content-Length, and expiry;
- presigned POST where expected;
- conditional writes where expected;
- both checksum modes, asserting `WHEN_REQUIRED` always works;
- the capability probe: its results must equal the target's expected set, and it must never crash or modify bucket config;
- soft delete (versioned and trash paths) and restore.

| Target | Where | When |
|---|---|---|
| MinIO (pinned image, Testcontainers) | CI, every PR | required |
| Garage (pinned image, Testcontainers) | CI, every PR | required; the most different store, with no versioning, tagging or policy |
| LocalStack | CI | kept for the core module's existing tests |
| AWS S3, Cloudflare R2, Backblaze B2 | GitHub Actions `workflow_dispatch` plus weekly, with credentials in a protected environment | optional; each uses a dedicated scratch bucket with a 1-day lifecycle |
| Wasabi, DigitalOcean Spaces, Ceph RGW | same workflow, when someone supplies credentials | community-reported results are recorded in the README matrix |

The README publishes the latest result per provider, so §3.3's `?` cells turn into evidence over time.

---

## 12. Phase plan and effort

| Phase | Scope | Effort |
|---|---|---|
| **P0 — core** | §8.1 SDK v2 and connection settings in `qqq-backend-module-filesystem`; §8.2 `${file.}` refs; conformance suite skeleton on MinIO and Garage | 7–9 days |
| **P1 — service** | Repo from `qbit-template`; config and config upsert; connection, bucket, capability, operation, header-rule tables; `ObjectStorage` API, keys, header policy, public URL resolver, listeners, rate limiter; Test connection; full conformance suite; backups follow-ups SF1, SF2, SF4 (§9.4) | 8–10 days |
| **P2 — first consumer migration** | §9.5 in the first consumer, behavior-identical, plus equivalence tests; backups follow-ups SF3, SF5 (§9.4) | 4–5 days |
| **P3 — Storage app, read side** | Index, scan, HEAD backlog, folders, object record with preview and download route, search, references (orphans and missing), usage snapshots, home, alert producer (§9.3, when the Alerts qbit is installed) | 8–10 days |
| **P4 — Storage app, write side** | Upload, copy, move/rename, change headers, apply header policy, tags, presign, delete and restore with versioning and trash, protection rules, bulk caps | 8–10 days |
| **P5 — later** | Bucket-config edits for UI-managed buckets, admin-API usage plug-ins (MinIO, Garage, Ceph), provider event notifications, direct browser uploads, `BYTES` display format if not already done | 6–8 days |

Total for P0–P4: about 35–44 days. P2 can ship right after P1, so the first consumer stops hand-rolling clients before the admin app exists. CMS and CDN can build on P1.

---

## 13. Owner questions (decided 2026-10-12: all recommendations accepted, see D1–D14)

1. **Bucket configuration from the UI, or IaC only?** Policy, versioning, lifecycle, CORS and quotas.
   *Recommendation:* IaC-owned by default (`managedBy = IAC`): shown read-only, with a drift check. UI edits only for buckets explicitly marked `UI`, with `s3.admin`. The first consumer's buckets are created by a GitOps job, so they stay IaC.
2. **May connections be created or re-pointed in the UI?**
   *Recommendation:* No. Connections come from code config (secret references to environment or mounted files, from an allow-list). The UI may edit operational fields (rate limits, timeouts, presign TTLs) with `s3.admin`. This closes the "point a secret at my server" hole in §7.2.
3. **Object index as the primary browse table?**
   *Recommendation:* Yes. A Postgres index kept current by write-through plus a nightly and on-demand scan, with live HEAD on the record screen. A bounded "Live objects" table (the upgraded S3 backend) only for non-indexed buckets.
4. **Soft delete where the provider has no versioning.**
   *Recommendation:* Enable versioning, with a 30-day noncurrent-version expiry, on public media buckets where the provider supports it (an IaC change). Use a **private** trash bucket with 30-day expiry where it does not (R2, Garage). Permanent deletes only with `s3.admin`.
5. **Referenced objects: block, or allow an admin override?**
   *Recommendation:* Block delete and move of referenced objects in v1, with no override. The Review links to the referencing records. Header changes stay allowed.
6. **SDK v2 migration of the S3 backend in 4.1.x or 4.2?** It changes the public `AmazonS3` types on `S3Utils`.
   *Recommendation:* 4.1.x, because SDK v1 is already out of support. Keep method names, change the types, and say so in the release notes.
7. **Per-bucket permission scoping in v1?**
   *Recommendation:* Yes. The lock is cheap to build now and painful to retrofit. Single-admin apps grant all-access.
8. **Upload path and size.**
   *Recommendation:* v1 uploads go through the app server, up to 100 MB per file by default (streamed multipart to the store). Direct-to-bucket browser uploads come in P5.
9. **Usage statistics source.**
   *Recommendation:* Index-derived in v1. It is exact for indexed buckets and needs no admin credentials. MinIO, Garage and Ceph admin-API plug-ins come later and are opt-in.
10. **How CMS and CDN depend on this qbit.**
    *Recommendation:* CMS keeps its `CmsStorage` SPI and this qbit provides the S3 adapter. CDN drops its own header backfill and uses **Apply header policy**, a `PublicUrlResolver` and a `StorageChangeListener`. Edit #1083 and #1084 accordingly.
11. **Orphans and missing objects: Storage home or Data Health?**
    *Recommendation:* The Storage home owns them. Data Health gets two rules that link there, with no second dashboard.
12. **Repo, package and permission names.**
    *Recommendation:* `QRun-IO/qbit-object-storage`, `com.kingsrook.qbits.objectstorage`, app label "Storage", tables prefixed `object_storage_` (amended 2026-10-12 by the owner naming rule; was `storage`), permissions `s3.view`, `s3.write`, `s3.delete`, `s3.admin`.
13. **Conformance targets.**
    *Recommendation:* MinIO and Garage in CI on every PR. AWS S3, R2 and B2 as an optional owner-run workflow with scratch buckets. Wasabi, Spaces and Ceph are community-reported.
14. **Presigning key.**
    *Recommendation:* Require a separate least-privilege presign key whenever presigned URLs leave the server, since its access key ID is visible in every URL. Warn on the connection record when the main key is used for presigning.

---

## 14. Risks

| Risk | Mitigation |
|---|---|
| The index drifts from the bucket | Write-through, nightly scan, "indexed at" on every row, Refresh from bucket, mass-removal safety stop |
| SDK v2 defaults break a store | `WHEN_REQUIRED` default off AWS; conformance runs both modes |
| A provider claims support but misbehaves | Probe-based capabilities; conformance suite; the UI treats `UNKNOWN` as unsupported for writes |
| An operator deletes live media | Referenced-object blocking, protection rules, soft delete, Review step, audit log |
| Secret exfiltration via connection edits | Config-only connections, reference allow-list, `s3.admin` on endpoint changes |
| Large buckets overload the app | Index caps, prefix-scoped indexing, bounded live listing, rate limits, resumable scans |
| MinIO community-edition changes | Provider-neutral design; Garage in CI as a ready alternative |

## 15. Sources

- AWS SDK for Java v2 data integrity protections (`requestChecksumCalculation`, `responseChecksumValidation`, default since 2.30.0): https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/s3-checksums.html
- AWS SDK for Java v1 end-of-support announcement: https://aws.amazon.com/blogs/developer/announcing-end-of-support-for-aws-sdk-for-java-v1-x-on-december-31-2025/
- Amazon S3 Object Ownership and ACLs disabled by default (April 2023): https://docs.aws.amazon.com/AmazonS3/latest/userguide/about-object-ownership.html
- Cloudflare R2 S3 API compatibility: https://developers.cloudflare.com/r2/api/s3/api/
- Garage S3 compatibility status: https://garagehq.deuxfleurs.fr/documentation/reference-manual/s3-compatibility/
- Backblaze B2 S3-compatible API: https://www.backblaze.com/docs/cloud-storage-s3-compatible-api
- MinIO Admin API and `mc admin`: https://min.io/docs/minio/linux/reference/minio-mc-admin.html
- Wasabi minimum storage duration policy: https://docs.wasabi.com/docs/how-does-wasabis-minimum-storage-duration-policy-work
- QQQ code: `qqq-backend-module-filesystem` `s3/` package, `QStorageInterface`, `AdornmentType.FILE_PREVIEW`, `QMetaDataVariableInterpreter`, `StreamedETLWithFrontendProcess` (develop @ 56171e849).
