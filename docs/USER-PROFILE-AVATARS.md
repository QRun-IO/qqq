# User profile and avatars

Status: implemented in core and UI API 1.7.0 (QQQ 4.1.0-RC.16). Builds on
[USER-PREFERENCES.md](USER-PREFERENCES.md).

## 0. Summary

A signed-in user sets a photo (avatar) and an optional display name on the **Profile** section of
the Preferences page. Profile is an ordinary preferences section (`profile`) registered by core
through the same SPI every qbit uses, so every client renders it generically. Photos are uploaded
with the existing `POST /qqq/v1/uploads`, validated and re-encoded on the server, stored as
objects in the application's storage table (S3/MinIO; never the local filesystem), and served to
any signed-in user by `GET /qqq/v1/avatars/{userId}`. A user without a photo gets an initials
avatar drawn by the client. Gravatar is never called unless the application allows it **and** the
user turns it on.

## 1. Audit (2026-10-15): what already existed

- The preferences SPI (`UserPreferencesMetaData`, sections, lists, actions, providers,
  `/qqq/v1/preferences`) shipped in UI API 1.3.0. Next renders every section generically; only
  its device-only *Display* section is client-side.
- On the SPI already: alerts (`notifications`), slack (`slack`), file-gateway (`file-transfer`),
  qa-capture (`qa-capture`). In flight: assistant (`assistant`, qbit-assistant#21), authentik
  personal access tokens (`access-tokens`, qbit-authentik#25). Collaboration, secured-data,
  worm-audit and object-storage have no per-user settings (collaboration's follow flags are per
  record, not preferences).
- Avatars: none in core. Next drew initials, or a Gravatar for **every** user whenever the app's
  branding set `gravatarDefault` (an MD5 of each email sent to a third party, with no opt-in).
  Presence showed initials; comments, mentions, activity and the assistant showed no picture.

## 2. Metadata

```java
new UserProfileMetaDataProvider()
   .withGravatarOptIn(false)                 // default false: no Gravatar choice at all
   .defineAll(qInstance, "rdbms", "avatarStorage");
```

- Table `user_profile` (label "User Profiles", FIELD audits): `id`, `userId` (unique),
  `displayName`, `avatarKey`, `avatarUpdatedAt`, `useGravatar`, `gravatarHash`, `createDate`,
  `modifyDate`. Applications create it with their own migration (DDL in the CHANGELOG). No foreign
  keys. Administrators see and correct rows like any other table.
- `avatarStorageTableName`: a storage (filesystem/S3) table the application defines. Objects are
  written at `avatars/{avatarKey}/{size}.png`.
- Section `profile` (label "Profile", icon `account_circle`, order 1, no permission, no app):
  - `avatar` - STRING with a `FILE_UPLOAD` adornment: `valueIsUploadRef=true`,
    `accept=image/png,image/jpeg,image/webp,image/gif`, `maxBytes=5242880`, `imageShape=circle`.
    Read value: the photo URL (or null). Write value: a ref from `POST /uploads`, or null to remove.
  - `displayName` - STRING, max 100. Blank means "use my sign-in name".
  - `useGravatar` - BOOLEAN, only when the app opts in.

`valueIsUploadRef` is a generic contract: any section may declare an upload field this way, and a
client that sees it uploads the file first and sends the ref.

## 3. Image handling (server)

All inline and bounded (5 MB, 40 megapixels), so no durable job is needed.

1. The ref must belong to the signed-in user (`UploadRefs.get`, owner-bound, not expired).
2. Type by **magic bytes**, never by name or declared type: PNG, JPEG, GIF (first frame), WebP.
   Everything else (SVG, HTML, PDF, HEIC, ...) is refused: "Use a PNG, JPEG, WebP or GIF image."
3. Dimensions are read from the header before decoding (decompression-bomb guard).
4. Decode, apply the JPEG EXIF orientation, centre-crop square, resize to 32, 64, 128 and 256 px,
   and **re-encode as PNG**. Re-encoding drops every byte of metadata (EXIF, GPS, XMP, ICC
   comments). PNG at 256 px is small; there is no pure-Java WebP encoder, so WebP output is not
   offered.
5. `avatarKey` = first 32 hex of SHA-256(userId, PNG bytes): content-addressed per user. Replacing
   or removing deletes the previous objects after the row is updated.

## 4. UI API 1.7.0

| Operation | Notes |
|---|---|
| `GET /avatars/{userId}?size=32\|64\|128\|256&v=` | Any signed-in user. 200 `image/png`; 302 to Gravatar (`d=404`) only for a user who turned it on; 404 when none (clients draw initials). With `v` equal to the current key: `Cache-Control: private, max-age=31536000, immutable`; otherwise `private, max-age=300` with an `ETag` (304 on match). `X-Content-Type-Options: nosniff`. |
| `POST /manageSession` | `values.user.avatarUrl` (when the user has a photo or Gravatar) and `values.user.displayName` (when set). |
| `GET/PATCH /preferences/profile` | The section above. |
| `FieldAdornment.values` | New known keys `valueIsUploadRef`, `accept`, `maxBytes`, `imageShape` (FILE_UPLOAD). |
| `GET /version` | Feature `avatars`. |

Authorization: avatars are visible to every signed-in user of the instance (they appear next to
comments, presence and mentions). They are not public: the operation is secured like any other.
Only the owner changes their own photo; there is no user parameter on the write path.

## 5. Clients

- One shared avatar component everywhere a person appears: header/user menu, presence, comments,
  mentions, activity, assistant chat. It shows the photo, else initials; a failed photo falls back
  to initials and is not retried for the session.
- The Preferences page renders `profile` generically; a `FILE_UPLOAD` field with
  `valueIsUploadRef` gets an image uploader (drag and drop, square crop, preview, remove).
- Next no longer calls Gravatar from branding alone; it shows a Gravatar only through
  `/avatars/{userId}` for users who turned it on.

## 6. Security tests

- Upload refs of another user are refused (404 wording, never says whose).
- SVG, HTML and polyglot files are refused by magic bytes; a JPEG with GPS EXIF comes back with
  no EXIF; an oversized image or dimension is refused.
- `GET /avatars/{userId}` needs a session; a PATCH carrying `userId` is rejected (existing rule).
