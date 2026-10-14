# Output delivery: destinations for process results, jobs, reports and exports

**Status:** approved design. Owner decisions (2026-10-14): all recommendations accepted (standing approval).
**Target:** QQQ core 4.1.x (`qqq-backend-core`, `qqq-middleware-javalin`), additive. UI API minor bump.
**Related:**

- qbit-slack `docs/DESIGN.md` section 9, which is the first external destination;
- the durable jobs effect outbox (#1191, `jobs_effect`);
- core messaging (#1194);
- scheduled reports (`ScheduledReport`, `RunScheduledReportExecuteStep`);
- secured data and classification (`docs/SECURED-DATA.md`).

## Owner requirement (verbatim, 2026-10-14)

> we should be able to send messages to channels etc as output from scheduled jobs and scheduled reports etc
> and from running things, etc or even as exports from the system (download is an option, but so is slack,
> etc). Slack should be tied to the user logged in, as with some deployments, we have users from MANY
> different companies logged in at once

## 0. Summary

Anything that produces output can deliver it to one or more **destinations**:

- process results;
- durable jobs;
- scheduled reports and scheduled jobs;
- exports and saved views;
- assistant scheduled tasks.

Core owns a thin, generic SPI, one action, two small tables and two UI API operations. The destinations
themselves are code that the installed qbits register:

| Destination | Provider |
|---|---|
| Download | Core (today's behaviour) |
| Email | Core, through `SendMessageAction`; qbit-email may register a richer one |
| Slack DM or channel | qbit-slack |
| Object storage, SFTP | Later (qbit-object-storage, qbit-file-gateway) |

Core never imports a qbit (G1).

**Principles:**

1. A delivery always runs **as its owner** (a QQQ user), with the owner's permissions and security keys.
   Scheduled runs rebuild the owner's session each time, the same way `RunScheduledReportExecuteStep` does.
2. A destination only offers targets that the owner can reach. Pickers are contingent dropdowns served by
   the destination, as the current user.
3. Classified content never goes to a destination that can't hold it. Core enforces this before calling
   the destination, and the user sees which fields and why.
4. Delivery is exactly once for durable jobs, through the `jobs_effect` outbox. Elsewhere, an idempotency
   key and a log row give the same result.
5. There is no filesystem. Attachments are storage references, streamed.

## 1. SPI (package `com.kingsrook.qqq.backend.core.delivery`)

```java
public interface DeliveryDestinationInterface
{
   /** Next contingent target field for the current session (e.g. workspace, then channel). */
   DeliveryTargetStep describeTargets(DeliveryDestinationContext context, Map<String, Serializable> chosen) throws QException;

   /** Domain validation of a complete target for the owner; messages are user-facing. */
   default List<String> validate(DeliveryRequest request) throws QException { return List.of(); }

   /** Deliver or queue. Must be idempotent on request.getIdempotencyKey(). */
   DeliveryResult deliver(DeliveryRequest request) throws QException;

   /** Look up the outcome of an earlier key (jobs_effect reconcile). Empty = unknown to the destination. */
   default Optional<DeliveryResult> lookup(String idempotencyKey) throws QException { return Optional.empty(); }
}
```

### 1.1 `QDeliveryDestinationMetaData`

The destinations are held in the supplemental `DeliveryDestinationsMetaData`, named `deliveryDestinations`.

| Field | Meaning |
|---|---|
| `name`, `label`, `icon`, `order` | |
| `codeReference` | The `DeliveryDestinationInterface` |
| `permissionName` | Optional. Users without it never see the destination |
| `maxClassification` | The highest `DataClassification` it may receive. Download: PHI. Email: as the provider declares. Slack: CONFIDENTIAL |
| `maxAttachmentBytes` | A per-destination setting (Email defaults to 10 MB, Slack to 50 MB). Beyond it an attachment becomes a link (section 2). Null means no attachments |
| `allowBearerLinks` | May receive presigned storage URLs, which only apply below PII |
| `aboveMaxClassification` | What happens to content above `maxClassification`: `REFUSE` (the default, and Slack's choice) or `LINK_ONLY`. With `LINK_ONLY` the destination receives only the title and an authenticated app link, and no data. qbit-email uses this with its `email_secure_message` template |

### 1.2 Contingent pickers

`DeliveryTargetStep` holds one field: a name, a label, and options (value, label, optional description), or
free text when the destination allows it. It also has a `complete` flag and a `summary` ("Slack: #ops in Acme").
The client sends back the values chosen so far, and the destination answers with the next field. Slack's
fields are the workspace from the user's own connections, then the channel the user is a member of, or "DM me".

### 1.3 `DeliveryRequest` and `DeliveryResult`

`DeliveryRequest`:

- `destinationName`, `targetValues` (the chosen map), `ownerUserId`;
- `sourceType` (PROCESS / JOB / SCHEDULED_REPORT / SCHEDULED_JOB / EXPORT / SAVED_VIEW / ASSISTANT_TASK)
  and `sourceKey`, for example `job:42` or `scheduledReport:7:2026-10-14T06:00Z`;
- `title`, `summary` (Markdown), `link` (an app link to the result);
- `attachments`: a list of `DeliveryAttachment` (file name, MIME type, size, storage table name and
  reference). Content is streamed through the storage action and is never staged on disk;
- `contentClassification` (the highest field classification in the payload, computed by the producer) and
  `restrictedFieldLabels`;
- `leaveOutRestricted`;
- `idempotencyKey`.

`DeliveryResult`:

- `status`: DELIVERED, QUEUED, REFUSED, FAILED_RETRYABLE, FAILED or UNAUTHORIZED;
- `externalReference`, for example a Slack message id or an email outbox id;
- `reasons`, which are user-facing.

## 2. `DeliverOutputAction`

`new DeliverOutputAction().execute(request)` does the following:

1. **Owner session.** The current session user must be the owner. Otherwise the action must be in a system
   or job context, where it builds the owner's session with `createAutomatedSessionForUser`, intersected
   with the captured security keys for jobs (jobs spec 2.10). If the session can't be built, the result is
   UNAUTHORIZED.
2. **Visibility.** The destination's `permissionName` is checked.
3. **Classification gate.** If `contentClassification > maxClassification` and the destination is
   `LINK_ONLY`, the attachments and summary are dropped. The destination receives the title and the
   authenticated app link (step 4), and the log records it as delivered as a link. Otherwise the result is
   REFUSED with:
   "This output includes restricted fields (Patient name, Date of birth). <Destination> can't receive them.
   Leave them out, or choose another destination."
   - When `leaveOutRestricted` is true, the producer must already have rendered without those fields, and
     `contentClassification` reflects that.
   - Core helper `DeliveryClassification.of(table, fieldNames)` computes the classification and the labels
     from field metadata and the field-access resolver, so exports and reports use one rule.
4. **Size.** Over `maxAttachmentBytes`, an attachment is replaced by a link:
   - Below PII, and when `allowBearerLinks` is set: a presigned GET from the storage backend, with
     `linkTtlHours` (default 72).
   - Otherwise: an authenticated app link to the stored output (`/qqq/v1/delivery/output/{logId}`). It
     streams the file only to a signed-in user who may see it: the owner, or anyone with the source's
     permission and record access.
5. **Log.** It writes or reads `delivery_log` (section 4), keyed by `idempotencyKey`. A row that's already
   final returns its result without calling the destination.
6. **Deliver.** It calls `validate`, then `deliver`, and records the result.
   - On FAILED_RETRYABLE, the log row waits for its retry (`retryMinutes` 1, 5, 15, 60, 240). Retries come
     from the core scheduled process `deliveryRetry`, or from the jobs_effect dispatcher for jobs.
   - On UNAUTHORIZED, the owner is notified (`QAlerts.notify`, category SYSTEM, a link to the source), and
     the alert `delivery.unauthorized` is raised, deduplicated per owner and destination per day. **There is
     no fallback to another destination, user or credential.**

**Durable jobs.** A job declares the effect `deliver` as `IDEMPOTENT_EXTERNAL`. `DeliveryEffectTarget
implements EffectTarget` maps:

- `send` to `DeliverOutputAction`, with the effect key as the idempotency key;
- `reconcile` to the destination's `lookup`.

So a crash between "Slack accepted" and "marked SENT" is reconciled, not resent. Core provides this target
once #1191 has merged; until then the action is called directly at job completion.

## 3. Saved deliveries: `output_delivery`

This is a standard table with a record screen, owner-locked on `ownerUserId`. Admins see all rows with
`output_delivery.admin`.

| Field | Notes |
|---|---|
| `ownerUserId` | Set on insert; it can't be changed |
| `sourceTableName`, `sourceRecordId` | For example `scheduledReport` 7, `scheduledJob` 3, or a saved view |
| `destinationName` | |
| `targetJson` | The chosen values. **Never a credential**: destinations store ids, such as Slack workspace and channel ids |
| `targetSummary` | From the last `DeliveryTargetStep` |
| `leaveOutRestricted` | |
| `enabled` | |
| `lastDeliveredAt`, `lastStatus`, `lastError` | |

It is a child list on scheduled reports, scheduled jobs and saved views. The scheduled report's existing
`toAddresses` keep working: they're treated as an implicit EMAIL delivery, with no migration.

## 4. Log: `delivery_log`

A standard table, owner-locked, and a child list on its source.

The fields are:

- `ownerUserId`, `sourceType`, `sourceKey`, `outputDeliveryId`;
- `destinationName`, `targetSummary`;
- `idempotencyKey` (unique), `status`, `attemptCount`, `nextAttemptAt`;
- `externalReference`;
- `contentClassification`, `attachmentName`, `attachmentBytes`, `deliveredAsLink`, `storageReference`
  (for the app link);
- `lastError` (user-facing, never credentials);
- `createdAt`, `deliveredAt`.

Retention: `logRetentionDays`, which defaults to 90.

Both tables come from `DeliveryMetaDataProvider.defineAll(instance, backendName, enricher)`, the same pattern
as saved reports. A host without them can still deliver directly, but has no retries and no saved deliveries.

## 5. UI API (minor bump)

| Operation | Purpose |
|---|---|
| `GET /qqq/v1/delivery/destinations` | The destinations the current user may use, with label, icon and limits |
| `POST /qqq/v1/delivery/destinations/{name}/targets` | Body `{chosen: {...}}`. Returns the next `DeliveryTargetStep` |
| `GET /qqq/v1/delivery/output/{logId}` | The authenticated download behind over-size links |

Process integration:

- **Frontend.** A process may add the standard **Deliver to** frontend step
  (`DeliveryStepMetaData.addTo(process)`): a destination list plus the contingent target fields, rendered
  generically by any client.
- **Backend.** The matching backend step calls `DeliverOutputAction` for each chosen destination, with the
  process's output, such as an export file or a report.
- **Exports.** The export dialog offers destinations next to Download through the same operations.
- **Scheduled reports and jobs** get an "Add delivery" record action that creates `output_delivery` rows.

## 6. Built-in destinations

- **DOWNLOAD:** the output stays in storage, and the result is DELIVERED with the app link. This is today's
  behaviour, now logged.
- **EMAIL:** through `SendMessageAction` with the configured provider (`emailProviderName`).
  `maxClassification` is CONFIDENTIAL and `aboveMaxClassification` is `LINK_ONLY`. qbit-email will register
  its own destination under the same name: `deliver` calls `EmailApi.sendOnce` with the delivery key, and
  `lookup` calls `getMessageByIdempotencyKey`. It may raise `maxClassification` to PHI only by an explicit
  setting, validated against `EmailApi.readiness(instance, category, true)`: the template must be
  phi_approved, the provider phi_allowed, and the body key set. The targets are
  "me" (the owner's email) and, with `delivery.email.others`, a free list of addresses. Attachments are
  attached up to `maxAttachmentBytes` (10 MB), and become links beyond that. qbit-email may register its
  own richer destination under the same name.

## 7. Tests (acceptance)

- **Owner identity:** a scheduled delivery runs as the owner, and an owner who lost permission gives
  UNAUTHORIZED, an owner notification and no delivery.
- **Classification:** a PHI export to a CONFIDENTIAL destination is REFUSED and names the fields. With
  `leaveOutRestricted` it's delivered without them.
- **Size:** an over-size attachment becomes a presigned link only below PII; otherwise it's an app link that
  requires sign-in and access.
- **Idempotency:** a repeated key returns the first result without calling the destination twice.
- **Durable jobs:** a crash after the destination accepted reconciles through `lookup`, giving one delivery.
- **Pickers:** destinations see only the current user's session. A fake two-tenant destination proves that
  user A never receives tenant B's options.
- **UI API:** contract tests for the three operations; the spec version bumped; a changelog entry.

## 8. Phases

1. SPI, the action, the tables, DOWNLOAD and EMAIL, the UI API, and tests (one PR).
2. The process **Deliver to** step, the export dialog, and scheduled report and job deliveries (one PR).
   Next UI renders them in a separate PR to qqq-frontend-next.
3. `DeliveryEffectTarget` once #1191 has merged.
