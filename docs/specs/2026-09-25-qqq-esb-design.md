# QQQ ESB — Design Spec (4.1)

Status: Approved by James · 2026-09-25

## 1. Goal

An Enterprise Service Bus for QQQ. Tables and processes publish events to queues and topics, and processes are triggered by queues and topics, all configured in metadata. The broker does the work; QQQ stores nothing.

### Non-goals (4.1)
- No changes to existing SQS queues, table automations, or the scheduler.
- No new database tables. No message history, outbox, or retry storage in QQQ.
- No ordering guarantee (concurrency 1 gives strict order).
- No duplicate filtering. Delivery is at-least-once; processes must tolerate redelivery.
- No XA, no JMS message selectors, no Kafka, no Material Dashboard, no Prometheus export (#268).

## 2. Brokers

- One implementation on the Jakarta Messaging (JMS) 3.x API.
- Supported: ActiveMQ Artemis and RabbitMQ 4.x (via `com.rabbitmq.jms:rabbitmq-jms` 3.x).
- Not supported: ActiveMQ Classic — it has no JMS 2.0 shared durable subscriptions (`createSharedDurableConsumer` throws `UnsupportedOperationException`), which topic triggers need.
- The provider type selects the `ConnectionFactory` class, loaded reflectively. Each app adds its broker's client jar.
- Availability is the broker's job. Services that need HA run HA brokers.
- RabbitMQ: queues and durable subscription queues are declared as quorum queues (needed for delivery counts).
- No broker plugins or special broker settings are required. QQQ's `maxAttempts` must stay below the broker's own redelivery limit (defaults: Artemis 10, RabbitMQ quorum 20).

## 3. Metadata

All new metadata is validated at boot by `QInstanceValidator` (unknown provider, destination, or process fails startup).

**`QEsbProviderMetaData`** (top-level)
- `name`, `type` (`ACTIVEMQ_ARTEMIS` | `RABBITMQ`)
- `url` (or `host`/`port`/`virtualHost` for RabbitMQ), `username`, `password` — `${env.*}` interpolated
- `managementUrl`, `managementUsername`, `managementPassword` — optional; enables queue depth and consumer counts

**`QEsbDestinationMetaData`** (top-level)
- `name`, `type` (`QUEUE` | `TOPIC`), `providerName`
- `destinationName` — broker-side name; defaults to `name`

**`EsbTableMetaData`** (`QSupplementalTableMetaData`)
- `publications`: list of `{ destinationName, events: [INSERT, UPDATE, DELETE] }`

**`EsbProcessMetaData`** (`QSupplementalProcessMetaData`)
- `publications`: list of `{ destinationName, events: [STARTED, COMPLETED, FAILED] }`
- `triggers`: list of `EsbTrigger`:

| Field | Default | Meaning |
|---|---|---|
| `destinationName` | required | queue or topic to consume |
| `subscriptionName` | process name | topics only; shared durable subscription name |
| `mode` | `SINGLE` | `SINGLE` = one message per run; `BATCH` = up to `batchSize` messages or `batchWaitMs` |
| `batchSize`, `batchWaitMs` | 100, 1000 | batch mode only |
| `concurrency` | 1 | consumers per node |
| `maxAttempts` | 3 | attempts before dead-lettering |
| `retryDelayMs`, `retryMultiplier`, `retryMaxDelayMs` | 0, 2.0, 60000 | backoff; 0 = immediate |
| `onDeadLetter` | `DEAD_LETTER_QUEUE` | or `DISCARD` |
| `deadLetterDestinationName` | `<destination>.dlq` (queue) / `<destination>.<subscription>.dlq` (topic) | where dead letters go |
| `runAsSessionSupplier` | system user session | `QCodeReference` to a `Supplier<QSession>` |
| `timeoutMs` | none | per run |
| `startPaused` | false | start without consuming |

Supplemental ESB metadata is **not** included in frontend metadata; the UI reads it through the permission-checked ESB endpoints (section 7).

## 4. Message format

- JMS `TextMessage`; body is a CloudEvents 1.0 structured JSON event.
- `id` (UUID), `source` (`qqq://<instance>/table/<name>` or `.../process/<name>`), `type`, `time`, `subject` (record primary key, table events only), `datacontenttype: application/json`, `data`.
- Types: `qqq.table.<table>.inserted|updated|deleted`, `qqq.process.<process>.started|completed|failed`, or caller-supplied for explicit publishes.
- Table `data`: `{ record }` for insert, `{ record, oldRecord }` for update, `{ oldRecord }` for delete. Record values use the `/qqq/v1` record JSON form.
- JMS properties mirror `ce_id`, `ce_type`, `ce_source`.
- One message per record. A multi-record insert publishes all its messages on one JMS session.

## 5. Publishing

### Core hooks (`qqq-backend-core`, no ESB dependency, no-op when unused)
1. **Record change listener**: instance-level listeners called by `InsertAction`, `UpdateAction`, `DeleteAction` after a successful write, with table, event type, records, and old records.
2. **After-commit callback** on `QBackendTransaction`: `addAfterCommitCallback(Runnable)`; run after a successful commit, discarded on rollback. Implemented for the RDBMS and MongoDB transactions.
3. **Process lifecycle listener**: instance-level listeners called by `RunProcessAction` when a run starts, completes (final step finished), or fails. Separate from the single per-process tracer, so it does not conflict with `qbit-standard-process-trace`.

### Behavior
- Table events publish after commit. If the write ran inside a caller's transaction, publication waits for that transaction's commit; a rollback publishes nothing. With no transaction, publish right after the action.
- Process events publish from the lifecycle listener.
- Explicit publish: `EsbPublishAction` (`destinationName`, `type`, `data`) — callable from any step or code.
- A failed publish never fails the record write or the process. It is logged and counted as a publish failure.
- Connections and sessions are long-lived and pooled per provider; messages are sent `PERSISTENT`.

## 6. Consuming (triggers)

- `QEsbRuntime.start(qInstance)` / `stop()` — called by the app at startup and shutdown, like the scheduler. The sample app shows it.
- Per trigger: `concurrency` consumers on virtual threads, each with its own transacted JMS session. Queue triggers compete for messages; topic triggers use a shared durable subscription, so each message is processed once across all nodes and is kept while the app is down.
- Each run initializes `QContext` with the instance and the trigger's run-as session.
- Process input:
  - `esbMessages` — list of CloudEvents (size 1 in `SINGLE` mode).
  - If the process's `tableName` matches the event's table, a `QProcessCallback` filter on the event records' primary keys, so existing table-bound processes run on changed records unchanged.
- Success: commit (acknowledge).
- Failure (exception or timeout): the attempt number is the broker's `JMSXDeliveryCount`.
  - Below `maxAttempts`: the consumer waits the backoff delay, then rolls back, and the broker redelivers to the same queue or subscription. Other subscribers are never affected. While waiting, that consumer is busy (with `concurrency` 1 the queue pauses during backoff).
  - At `maxAttempts`: send to the dead-letter queue (or discard), then commit, in the same transacted session.
  - Crash redeliveries count the same way.
  - (Amended 2026-09-25: re-sending a copy would re-broadcast topic messages to every subscriber.)
- Dead letters carry the original event plus `qqqError`, `qqqFailedTrigger`, `qqqAttempts`, `qqqFailedAt` properties.
- Batch mode: one run per batch; commit or fail the batch as a unit.
- Pause/resume: sent over an internal non-durable control topic so every node applies it. Runtime pause state is not persisted; `startPaused` is the restart default.
- Broker down at startup or connection lost: log, retry connection with backoff, never block app boot.

## 7. Observability

Nothing is stored. Data comes from QQQ metadata, in-memory counters on each node, and the broker's management API when `managementUrl` is set.

- **Counters per destination and trigger (per node)**: published, publish failures, consumed, succeeded, failed, retried, dead-lettered, in flight, last activity, average and max processing time.
- **Broker data (optional)**: queue depth, consumer count, dead-letter depth — Artemis via Jolokia, RabbitMQ via its management HTTP API. Plain `java.net.http`, no extra dependencies.
- **Dead letters**: browsed with JMS `QueueBrowser`.

### Endpoints (`/qqq/v1/esb`, route provider in `qqq-esb`)
| Endpoint | Returns | Requires |
|---|---|---|
| `GET /overview` | all providers, destinations, publishers, triggers, counters, broker data | ESB app access |
| `GET /table/{table}` | that table's publications, their destinations, subscribing triggers, counters | table READ |
| `GET /process/{process}` | that process's publications and triggers, counters | process access |
| `GET /deadLetters/{trigger}` | browse dead letters (paged) | access to the trigger's process |
| `GET /messages/{destination}` | browse any queue (paged) | see section 8 |

Subscribing triggers on processes the user cannot access are omitted.

### Next dashboard (`qqq-frontend-next` only)
- **ESB app**: backend `QAppMetaData` named `esb` (`permissionBaseName = "esbView"`) with one widget of new type `ESB_OVERVIEW`; Next adds the renderer.
- **Table Developer view** (`/app/<table>/dev`): ESB section from `GET /table/{table}`; hidden on 403 or when the table has no ESB metadata.
- **Process Developer view** (`/app/<process>/dev`, new): process metadata plus the same ESB section.
- Management actions (section 8) appear wherever the queue or trigger is shown, only when permitted and supported by the broker. Destructive actions ask for confirmation.
- No record-level view.

## 8. Management

Applies to queues, dead-letter queues, and topic subscriptions (each is a queue on the broker).

### QQQ consumers (all brokers, no management API needed)
- Pause, resume, restart a trigger's consumers — on every node, via the control topic.
- Replay dead letters — consume selected dead letters and run the trigger's process directly (success removes them; failure leaves them). No re-broadcast to other subscribers.

### Broker queues (needs `managementUrl`)
Artemis via Jolokia; RabbitMQ via its management HTTP API.

| Action | Artemis | RabbitMQ |
|---|---|---|
| Browse messages | yes | yes (JMS `QueueBrowser`) |
| Pause / resume delivery to all consumers, including non-QQQ | yes | no |
| Purge all messages | yes | yes |
| Delete selected messages | yes | no |
| Delete messages older than a time | yes | no |
| Move messages to another queue | yes | no |

- Each adapter reports its capabilities; the UI shows only supported actions.
- RabbitMQ has no queue-level pause; pausing QQQ's consumers covers QQQ's side.

### Actions as QQQ processes
Standard permissions via shared `permissionBaseName`:
- `esbOperate`: `esbPauseTrigger`, `esbResumeTrigger`, `esbRestartTrigger`, `esbReplayDeadLetters`, `esbPauseQueue`, `esbResumeQueue`, `esbMoveMessages`.
- `esbDelete`: `esbPurgeQueue`, `esbDeleteMessages` (selected or older than a time).

Endpoint addition: `GET /messages/{destination}` — browse (paged). Requires `esbView`, or READ on a table that publishes to it, or access to a process it triggers.

## 9. Permissions

- Table section: table READ. Process section: process access.
- Service level: `esbView.hasAccess` (the ESB app).
- Pause, resume, restart, replay, move: `esbOperate.hasAccess`.
- Purge and delete messages: `esbDelete.hasAccess`.
- All ESB endpoints and processes check permissions server-side; the UI only hides what the server denies.

## 10. Modules

- `qqq-backend-core` — the three hooks in section 5. Nothing else.
- `qqq-esb` (new) — metadata, validation, publisher, runtime, counters, management adapters, endpoints, operate processes. Depends on `qqq-backend-core`, `qqq-middleware-javalin` (route provider, same pattern as `qqq-middleware-health`), and `jakarta.jms-api`. Added to `qqq-bom`.
- `qqq-frontend-next` — ESB app widget and Developer-view sections.
- `qqq-sample-project` — ESB example with embedded Artemis.
- The `feature/topics` branch is not merged; its naming and action shape are reused, its code is replaced.

## 11. Testing

- **Unit**: embedded Artemis in `qqq-esb` tests (no Docker).
- **Broker conformance suite**: one abstract suite, run against Artemis and RabbitMQ via Testcontainers in CI. A broker is supported only if it passes. Covers:
  - queue competing consumers and concurrency
  - topic shared durable subscription across two runtimes (once per subscription, kept while down)
  - retry, backoff, dead-letter, replay, delete
  - batch mode
  - trigger pause/resume/restart across two runtimes
  - reconnect after broker restart
  - each management action the broker's adapter reports as supported, and a clear error for unsupported ones
- **Core hooks**: table events on insert/update/delete; none on rollback; published after the caller's commit; process started/completed/failed events.
- **Permissions**: each endpoint and operate process denies without the required permission.
- **Next**: component tests for the ESB widget and sections; one Playwright acceptance test for the table Developer view section.

## 12. Acceptance criteria

1. A table configured with a publication emits one CloudEvent per inserted, updated, and deleted record, only after commit.
2. A process emits started/completed/failed events to its configured destinations.
3. A process with a queue trigger runs once per message with the configured concurrency, retries, and dead-lettering.
4. A process with a topic trigger receives each message once across two running app instances, including messages sent while it was stopped.
5. The conformance suite passes on Artemis and RabbitMQ.
6. A user with table READ sees that table's ESB section in the Developer view; without it, the section is absent and the endpoint returns 403.
7. A user with `esbView` sees the ESB app. With `esbOperate` they can pause, resume, and restart triggers, replay dead letters, and pause, resume, or move queue messages where the broker supports it. With `esbDelete` they can purge queues and delete selected or old messages where supported.
8. No new database tables; SQS, automations, and scheduler behavior unchanged (existing tests pass untouched).
