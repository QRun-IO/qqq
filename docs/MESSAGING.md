# Core messaging

Qbits should send through core's `SendMessageAction` with a messaging provider name,
without importing the qbit that implements the provider. For example, the host can
register qbit-email's provider type `QBIT_EMAIL` under the name `qbitEmail`; callers
depend only on core and use that configured name.

```java
SendMessageOutput output = new SendMessageAction().execute(new SendMessageInput()
   .withMessagingProviderName("qbitEmail")
   .withTo(new Party().withAddress("recipient@example.com"))
   .withTemplateName("order-confirmation")
   .withVariable("orderNumber", "ORD-42")
   .withIdempotencyKey("order-confirmation:42")
   .withCategory("TRANSACTIONAL")
   .withLinkedRecord("order", 42L)
   .withSenderName("qbit-orders")
   .withLocale("en-US"));
```

The example requires a provider declaring `TEMPLATES` and `IDEMPOTENCY`, and a
configured template and sender. Existing `subject`, `contentList` (including email
TEXT/HTML roles), `attachmentList`, `to`, and `from` remain available with their
existing provider-specific behavior. Both `to` and `from` are `Party` values; use
`MultiParty` for multiple recipients or sender roles such as FROM and REPLY_TO.

All new input fields are optional and default to null:

| Field | Meaning |
| --- | --- |
| `templateName` | Provider template name; requires `TEMPLATES`. |
| `variables` | `Map<String, Serializable>`; `withVariable(name, value)` adds or replaces one entry. |
| `idempotencyKey` | Provider deduplication key; requires `IDEMPOTENCY`. |
| `category` | Conventional values: `TRANSACTIONAL`, `NOTIFICATION`, `MARKETING`, `SECURITY`. Providers may refuse unknown values. |
| `linkedTableName`, `linkedRecordId` | Associated table and serializable record ID; set together with `withLinkedRecord(table, id)`. |
| `senderName` | Calling qbit or process name for audit, distinct from the `from` party. |
| `locale` | Provider rendering locale. |

Core passes the input unchanged to the provider. A non-null template name or
idempotency key without its required capability throws `QException` before sending:
`Messaging provider X does not support templates` or
`Messaging provider X does not support idempotency`.
Category, linked record, sender name, locale, and variables without a template are
advisory; providers that do not support them silently ignore them.

`SendMessageAction.isAvailable(name)` checks that metadata exists in the current
`QInstance` and its type resolves. It does not check credentials or transport health.
`SendMessageAction.capabilities(name)` returns the declared
`Set<MessagingProviderCapability>`, or an empty set for absent or unresolvable providers.
Use `isAvailable` to distinguish those from providers declaring no optional features.
Both helpers require the caller's normal initialized `QContext`.

Providers declare features by overriding `MessagingProviderInterface.getCapabilities()`;
the default is an empty set. The enum contains `TEMPLATES`, `IDEMPOTENCY`, `CATEGORIES`,
`LINKED_RECORDS`, `ASYNC_DELIVERY`, and `MESSAGE_LOOKUP`. These declarations describe
provider support; core does not implement an outbox, deduplication store, or lookup API.
Built-in `EMAIL` and `SES` declare no capabilities and retain their existing sending behavior.

`SendMessageOutput` has an optional serializable `messageId`, a `SendMessageStatus`,
and optional `List<String> reasons`:

| Status | Meaning |
| --- | --- |
| `SENT` | Handed to the transport synchronously; does not guarantee inbox delivery. |
| `QUEUED` | Accepted by an asynchronous provider (outbox). |
| `DUPLICATE` | Idempotency key already used; `messageId` identifies the earlier message. |
| `SUPPRESSED` | Not sent; `reasons` explains suppression. |
| `REFUSED` | Not sent; `reasons` explains refusal. |

A newly constructed output has null fields. Successful dispatch through core always
returns a status: core supplies `SENT` for legacy null outputs or null statuses and
preserves explicit provider statuses, IDs, and reasons. Built-in EMAIL and SES return
`SENT` directly after transport submission. Transport errors still throw `QException`.
