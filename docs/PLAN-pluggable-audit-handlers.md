# Plan: Pluggable Audit Handler System

**Status:** Implemented (PR #356)
**Branch:** `feature/pluggable-audit-handlers`

## Goal

Enable multiple audit handlers to run alongside the default audit system, supporting use cases like WORM HIPAA-compliant storage.

## Requirements Summary

| Requirement | Decision |
|-------------|----------|
| Hook points | Both DML-level (full records) and Audit-level (processed) |
| Data richness | Full record snapshots (old/new QRecords) |
| Sync/Async | Configurable per handler |
| Failure policy | Configurable per handler |
| Scope | Global + per-table overrides |
| Transaction timing | After commit |
| Process audits | Yes, all audits go to handlers |

## Design

### New Interfaces

**AuditHandlerInterface** (base):
- `getName()` - unique handler name

**DMLAuditHandlerInterface** extends base:
- `handleDMLAudit(DMLAuditHandlerInput input)` - receives full old/new QRecords

**ProcessedAuditHandlerInterface** extends base:
- `handleAudit(ProcessedAuditHandlerInput input)` - receives AuditSingleInput list

### New Metadata

**QAuditHandlerMetaData**:
- `name`, `handlerCode` (QCodeReference)
- `handlerType` (DML or PROCESSED)
- `isAsync`, `failurePolicy`
- `tableNames` (null = global)
- `enabled`

**AuditHandlerType** enum: `DML`, `PROCESSED`

**AuditHandlerFailurePolicy** enum: `LOG_AND_CONTINUE`, `FAIL_OPERATION`

### New Input Classes

**DMLAuditHandlerInput**:
- `tableName`, `dmlType` (INSERT/UPDATE/DELETE)
- `newRecords`, `oldRecords` (full QRecords)
- `tableActionInput`, `timestamp`, `auditContext`, `session`

**ProcessedAuditHandlerInput**:
- `auditSingleInputs` (list)
- `timestamp`, `session`, `sourceType`

### Executor

**AuditHandlerExecutor**:
- `executeDMLHandlers(input)` - called from DMLAuditAction
- `executeProcessedHandlers(input)` - called from AuditAction
- Handles async (thread pool with CapturedContext)
- Respects failure policy per handler

## Files Created

```
qqq-backend-core/src/main/java/com/kingsrook/qqq/backend/core/
├── actions/audits/
│   ├── AuditHandlerExecutor.java
│   ├── AuditHandlerInterface.java
│   ├── DMLAuditHandlerInterface.java
│   └── ProcessedAuditHandlerInterface.java
├── model/actions/audits/
│   ├── DMLAuditHandlerInput.java
│   └── ProcessedAuditHandlerInput.java
└── model/metadata/audits/
    ├── QAuditHandlerMetaData.java
    ├── AuditHandlerType.java
    └── AuditHandlerFailurePolicy.java
```

## Files Modified

| File | Changes |
|------|---------|
| `QInstance.java` | Add `auditHandlers` map, `addAuditHandler()`, `getAuditHandlersForTable()` |
| `DMLAuditAction.java` | Call `AuditHandlerExecutor.executeDMLHandlers()` after processing |
| `AuditAction.java` | Call `AuditHandlerExecutor.executeProcessedHandlers()` after insert |

## Usage Example

```java
qInstance.addAuditHandler(new QAuditHandlerMetaData()
   .withName("wormHandler")
   .withHandlerCode(new QCodeReference(WormDMLAuditHandler.class))
   .withHandlerType(AuditHandlerType.DML)
   .withTableNames(Set.of("medicalRecord", "prescription"))
   .withIsAsync(false)
   .withFailurePolicy(AuditHandlerFailurePolicy.FAIL_OPERATION)
   .withEnabled(true));
```

## Tests

- `AuditHandlerExecutorTest.java` - 7 unit tests
- `AuditHandlerIntegrationTest.java` - 6 integration tests

## Future Work

WORM QBit reference implementation (separate repo: `qbit-worm-audit`):
- WormAuditQBitProducer
- WormAuditQBitConfig
- WormDMLAuditHandler

## Notes

- Existing `AuditActionCustomizerInterface` remains unchanged (modifies audit data)
- New handlers receive data but don't modify the default audit flow
- Async handlers use thread pool with CapturedContext for QContext propagation
- FAIL_OPERATION only works for sync handlers (async cannot roll back)
