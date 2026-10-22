/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.kingsrook.qqq.backend.core.jobs;


import java.util.List;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectStatus;
import com.kingsrook.qqq.backend.core.jobs.effects.JobEffectType;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobEventType;
import com.kingsrook.qqq.backend.core.jobs.model.JobIdempotency;
import com.kingsrook.qqq.backend.core.jobs.model.JobMode;
import com.kingsrook.qqq.backend.core.jobs.model.JobSemantics;
import com.kingsrook.qqq.backend.core.jobs.model.JobSourceType;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.processes.JobControlProcesses;
import com.kingsrook.qqq.backend.core.jobs.processes.JobEffectReviewProcesses;
import com.kingsrook.qqq.backend.core.jobs.progress.JobLogPermissions;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRefs;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist.BulkLoadUploadSources;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.DEFAULT_MAX_ATTEMPTS;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.DEFAULT_PRIORITY;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.SECURITY_KEY_USER;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.SECURITY_KEY_USER_ALL_ACCESS;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_CHUNK;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_EFFECT;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_EVENT;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_JOB;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_LOG;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_ROW_ERROR;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_UPLOAD;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_WORKER;


/*******************************************************************************
 ** Defines the durable-jobs tables (jobs_job, jobs_chunk, jobs_row_error,
 ** jobs_event, jobs_worker), their possible-value sources, joins, the record
 ** lock, and the job control processes. Same pattern as
 ** SavedBulkLoadProfileMetaDataProvider:
 **
 ** <pre>new DurableJobsMetaDataProvider().defineAll(instance, "rdbms", enricher);</pre>
 **
 ** Without the qbit-jobs app the tables still get standard screens.
 **
 ** Security: a record lock on jobs_job.userId (key type jobs_user) - users see
 ** their own jobs; the jobs_user_all_access key (granted for the jobs_admin
 ** permission by DurableJobsSecurity.applyToSession) sees all. Child tables are
 ** locked through their join to jobs_job.
 *******************************************************************************/
public class DurableJobsMetaDataProvider
{

   /*******************************************************************************
    ** Add every durable-jobs table, join, possible-value source, security key
    ** type and control process to the instance.
    *******************************************************************************/
   public void defineAll(QInstance instance, String backendName, Consumer<QTableMetaData> backendDetailEnricher) throws QException
   {
      if(instance.getSecurityKeyType(SECURITY_KEY_USER) == null)
      {
         instance.addSecurityKeyType(new QSecurityKeyType().withName(SECURITY_KEY_USER).withAllAccessKeyName(SECURITY_KEY_USER_ALL_ACCESS));
      }

      addEnumPvs(instance, JobStatus.NAME, JobStatus.values());
      addEnumPvs(instance, ChunkStatus.NAME, ChunkStatus.values());
      addEnumPvs(instance, JobEventType.NAME, JobEventType.values());
      addEnumPvs(instance, JobMode.NAME, JobMode.values());
      addEnumPvs(instance, JobSourceType.NAME, JobSourceType.values());
      addEnumPvs(instance, JobIdempotency.NAME, JobIdempotency.values());
      addEnumPvs(instance, JobSemantics.NAME, JobSemantics.values());
      addEnumPvs(instance, JobEffectType.NAME, JobEffectType.values());
      addEnumPvs(instance, EffectStatus.NAME, EffectStatus.values());

      instance.addTable(enrich(defineJobTable(backendName), backendDetailEnricher));
      instance.addTable(enrich(defineChunkTable(backendName), backendDetailEnricher));
      instance.addTable(enrich(defineRowErrorTable(backendName), backendDetailEnricher));
      instance.addTable(enrich(defineEventTable(backendName), backendDetailEnricher));
      instance.addTable(enrich(defineLogTable(backendName), backendDetailEnricher));
      instance.addTable(enrich(defineWorkerTable(backendName), backendDetailEnricher));
      instance.addTable(enrich(defineUploadTable(backendName), backendDetailEnricher));
      instance.addTable(enrich(defineEffectTable(backendName), backendDetailEnricher));

      ////////////////////////////////////////////////////////////////////////
      // assisted bulk load (profile, suggestMapping, validate) reads files //
      // by the same opaque upload refs that jobs take                      //
      ////////////////////////////////////////////////////////////////////////
      BulkLoadUploadSources.register(UploadRefs::open);

      instance.addPossibleValueSource(QPossibleValueSource.newForTable(TABLE_JOB));
      instance.addPossibleValueSource(QPossibleValueSource.newForTable(TABLE_CHUNK));

      instance.addJoin(childJoin(TABLE_CHUNK));
      instance.addJoin(childJoin(TABLE_ROW_ERROR));
      instance.addJoin(childJoin(TABLE_EVENT));
      instance.addJoin(childJoin(TABLE_LOG));
      instance.addJoin(childJoin(TABLE_EFFECT));

      for(var process : JobControlProcesses.defineAll())
      {
         instance.addProcess(process);
      }
      JobEffectReviewProcesses.defineAll().forEach(instance::addProcess);
   }



   /*******************************************************************************
    ** Names of the main tables a host may wire into its ESB (each main table
    ** publishes table events). Core does not depend on qqq-esb.
    *******************************************************************************/
   public static List<String> esbTableNames()
   {
      return (List.of(TABLE_JOB));
   }



   /*******************************************************************************
    ** A backend-detail enricher that maps every field to a snake_case column
    ** (rowsDone -> rows_done), for databases whose schema uses snake_case. Pass it
    ** as defineAll's enricher, or compose it with your own.
    *******************************************************************************/
   public static Consumer<QTableMetaData> snakeCaseColumnNames()
   {
      return (table -> table.getFields().values().forEach(field ->
      {
         if(field.getBackendName() == null)
         {
            field.setBackendName(field.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase());
         }
      }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static <T extends PossibleValueEnum<String>> void addEnumPvs(QInstance instance, String name, T[] values)
   {
      if(instance.getPossibleValueSource(name) == null)
      {
         instance.addPossibleValueSource(QPossibleValueSource.newForEnum(name, values));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QTableMetaData enrich(QTableMetaData table, Consumer<QTableMetaData> backendDetailEnricher)
   {
      if(backendDetailEnricher != null)
      {
         backendDetailEnricher.accept(table);
      }
      return (table);
   }



   /*******************************************************************************
    ** jobs_job to a child table, one-to-many on jobId.
    *******************************************************************************/
   private static QJoinMetaData childJoin(String childTable)
   {
      return (new QJoinMetaData()
         .withLeftTable(TABLE_JOB)
         .withRightTable(childTable)
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", "jobId"))
         .withInferredName());
   }



   /*******************************************************************************
    ** Name of the join from jobs_job to a child table.
    *******************************************************************************/
   public static String childJoinName(String childTable)
   {
      return (QJoinMetaData.makeInferredJoinName(TABLE_JOB, childTable));
   }



   /*******************************************************************************
    ** The owner lock: the session's jobs_user key must equal the job's userId.
    *******************************************************************************/
   private static RecordSecurityLock ownerLock(String fieldName, List<String> joinNameChain)
   {
      RecordSecurityLock lock = new RecordSecurityLock()
         .withSecurityKeyType(SECURITY_KEY_USER)
         .withFieldName(fieldName)
         .withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE);
      if(joinNameChain != null)
      {
         lock.withJoinNameChain(joinNameChain);
      }
      return (lock);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QFieldMetaData confidential(QFieldMetaData field)
   {
      return (field.withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.CONFIDENTIAL)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QFieldMetaData field(String name, QFieldType type)
   {
      return (new QFieldMetaData(name, type));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QFieldMetaData readOnly(String name, QFieldType type)
   {
      return (new QFieldMetaData(name, type).withIsEditable(false));
   }



   /*******************************************************************************
    ** jobs_job: one row per durable job.
    *******************************************************************************/
   public QTableMetaData defineJobTable(String backendName)
   {
      return (new QTableMetaData()
         .withName(TABLE_JOB)
         .withAssociation(new Association().withName(TABLE_LOG).withAssociatedTableName(TABLE_LOG).withJoinName(childJoinName(TABLE_LOG)))
         .withExposedJoin(new ExposedJoin().withJoinTable(TABLE_LOG).withJoinPath(List.of(childJoinName(TABLE_LOG))).withLabel("Log"))
         .withLabel("Job")
         .withIcon(new QIcon("pending_actions"))
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("label")
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.RECORD))
         .withRecordSecurityLock(ownerLock("userId", null))
         .withField(readOnly("id", QFieldType.INTEGER))
         .withField(field("label", QFieldType.STRING).withMaxLength(250))
         .withField(readOnly("processName", QFieldType.STRING).withLabel("Process").withMaxLength(250))
         .withField(readOnly("tableName", QFieldType.STRING).withLabel("Table").withMaxLength(250))
         .withField(readOnly("mode", QFieldType.STRING).withPossibleValueSourceName(JobMode.NAME).withMaxLength(20))
         .withField(readOnly("status", QFieldType.STRING).withPossibleValueSourceName(JobStatus.NAME).withMaxLength(30))
         .withField(readOnly("statusReason", QFieldType.STRING).withMaxLength(500))
         .withField(field("priority", QFieldType.INTEGER).withDefaultValue(DEFAULT_PRIORITY))
         .withField(readOnly("userId", QFieldType.STRING).withLabel("Owner").withMaxLength(250))
         .withField(readOnly("clientChannel", QFieldType.STRING).withMaxLength(250))
         .withField(readOnly("credentialScopeJson", QFieldType.TEXT).withLabel("Credential Scope"))
         .withField(readOnly("sourceType", QFieldType.STRING).withPossibleValueSourceName(JobSourceType.NAME).withMaxLength(30))
         .withField(readOnly("sourceJson", QFieldType.TEXT).withLabel("Source"))
         .withField(confidential(readOnly("paramsJson", QFieldType.TEXT).withLabel("Parameters")))
         .withField(readOnly("idempotency", QFieldType.STRING).withPossibleValueSourceName(JobIdempotency.NAME).withMaxLength(30))
         .withField(readOnly("semantics", QFieldType.STRING).withPossibleValueSourceName(JobSemantics.NAME).withMaxLength(20))
         .withField(readOnly("chunkSize", QFieldType.INTEGER))
         .withField(readOnly("maxParallelChunks", QFieldType.INTEGER))
         .withField(readOnly("currentPhase", QFieldType.STRING).withMaxLength(250))
         .withField(readOnly("percentDone", QFieldType.INTEGER))
         .withField(readOnly("progressMessage", QFieldType.STRING).withMaxLength(1000))
         .withField(readOnly("bytesDone", QFieldType.LONG))
         .withField(readOnly("bytesTotal", QFieldType.LONG))
         .withField(readOnly("etaSeconds", QFieldType.LONG))
         .withField(readOnly("progressJson", QFieldType.TEXT))
         .withField(readOnly("logSeq", QFieldType.LONG))
         .withField(readOnly("logDropped", QFieldType.INTEGER))
         .withField(readOnly("leaseOwner", QFieldType.STRING).withMaxLength(250))
         .withField(readOnly("rowsTotal", QFieldType.INTEGER).withDisplayFormat(DisplayFormat.COMMAS))
         .withField(readOnly("rowsDone", QFieldType.INTEGER).withDisplayFormat(DisplayFormat.COMMAS))
         .withField(readOnly("rowsOk", QFieldType.INTEGER).withLabel("Rows OK").withDisplayFormat(DisplayFormat.COMMAS))
         .withField(readOnly("rowsFailed", QFieldType.INTEGER).withDisplayFormat(DisplayFormat.COMMAS))
         .withField(readOnly("chunksTotal", QFieldType.INTEGER).withDisplayFormat(DisplayFormat.COMMAS))
         .withField(readOnly("chunksDone", QFieldType.INTEGER).withDisplayFormat(DisplayFormat.COMMAS))
         .withField(readOnly("chunksFailed", QFieldType.INTEGER).withDisplayFormat(DisplayFormat.COMMAS))
         .withField(readOnly("summaryJson", QFieldType.TEXT).withLabel("Summary"))
         .withField(readOnly("outputStorageJson", QFieldType.TEXT).withLabel("Outputs"))
         .withField(readOnly("parentJobId", QFieldType.INTEGER).withLabel("Parent Job").withPossibleValueSourceName(TABLE_JOB))
         .withField(readOnly("savedBulkLoadProfileId", QFieldType.INTEGER).withLabel("Bulk Load Profile"))
         .withField(field("notifyEmail", QFieldType.BOOLEAN).withLabel("Email Me When Done").withDefaultValue(false))
         .withField(readOnly("queuedAt", QFieldType.DATE_TIME))
         .withField(readOnly("startedAt", QFieldType.DATE_TIME))
         .withField(readOnly("finishedAt", QFieldType.DATE_TIME))
         .withField(readOnly("lastProgressAt", QFieldType.DATE_TIME))
         .withField(readOnly("errorMessage", QFieldType.TEXT))
         .withField(readOnly("leaseVersion", QFieldType.INTEGER).withDefaultValue(0))
         .withField(readOnly("createDate", QFieldType.DATE_TIME))
         .withField(readOnly("modifyDate", QFieldType.DATE_TIME))
         .withSection(new QFieldSection("identity", new QIcon("badge"), Tier.T1, List.of("id", "label", "status", "statusReason", "processName", "tableName", "mode", "userId")))
         .withSection(new QFieldSection("progress", "Progress", new QIcon("speed"), Tier.T2, List.of("currentPhase", "percentDone", "progressMessage", "bytesDone", "bytesTotal", "etaSeconds", "rowsTotal", "rowsDone", "rowsOk", "rowsFailed", "chunksTotal", "chunksDone", "chunksFailed", "lastProgressAt", "errorMessage")))
         .withSection(new QFieldSection("settings", "Settings", new QIcon("tune"), Tier.T2, List.of("priority", "notifyEmail", "sourceType", "idempotency", "semantics", "chunkSize", "maxParallelChunks", "parentJobId", "savedBulkLoadProfileId", "clientChannel")))
         .withSection(new QFieldSection("data", "Data", new QIcon("data_object"), Tier.T3, List.of("progressJson", "logSeq", "logDropped", "leaseOwner", "summaryJson", "outputStorageJson", "sourceJson", "paramsJson", "credentialScopeJson", "leaseVersion")))
         .withSection(new QFieldSection("dates", new QIcon("calendar_month"), Tier.T3, List.of("queuedAt", "startedAt", "finishedAt", "createDate", "modifyDate"))));
   }



   /*******************************************************************************
    ** jobs_chunk: the unit of work a worker leases.
    *******************************************************************************/
   public QTableMetaData defineChunkTable(String backendName)
   {
      return (new QTableMetaData()
         .withName(TABLE_CHUNK)
         .withLabel("Job Chunk")
         .withIcon(new QIcon("view_module"))
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withRecordLabelFormat("Chunk %s")
         .withRecordLabelFields("seq")
         .withUniqueKey(new UniqueKey("jobId", "seq"))
         .withRecordSecurityLock(ownerLock(TABLE_JOB + ".userId", List.of(childJoinName(TABLE_CHUNK))))
         .withField(readOnly("id", QFieldType.INTEGER))
         .withField(readOnly("jobId", QFieldType.INTEGER).withLabel("Job").withPossibleValueSourceName(TABLE_JOB))
         .withField(readOnly("seq", QFieldType.INTEGER).withLabel("Sequence"))
         .withField(readOnly("status", QFieldType.STRING).withPossibleValueSourceName(ChunkStatus.NAME).withMaxLength(30))
         .withField(readOnly("rangeJson", QFieldType.TEXT).withLabel("Range"))
         .withField(readOnly("inputStorageRef", QFieldType.STRING).withMaxLength(1000).withLabel("Input Object"))
         .withField(readOnly("attempt", QFieldType.INTEGER).withDefaultValue(0))
         .withField(readOnly("maxAttempts", QFieldType.INTEGER).withDefaultValue(DEFAULT_MAX_ATTEMPTS))
         .withField(readOnly("leaseOwner", QFieldType.STRING).withMaxLength(250))
         .withField(readOnly("leaseToken", QFieldType.STRING).withMaxLength(100))
         .withField(readOnly("leaseExpiresAt", QFieldType.DATE_TIME))
         .withField(readOnly("heartbeatAt", QFieldType.DATE_TIME))
         .withField(readOnly("rowsIn", QFieldType.INTEGER).withDisplayFormat(DisplayFormat.COMMAS))
         .withField(readOnly("rowsOk", QFieldType.INTEGER).withLabel("Rows OK").withDisplayFormat(DisplayFormat.COMMAS))
         .withField(readOnly("rowsFailed", QFieldType.INTEGER).withDisplayFormat(DisplayFormat.COMMAS))
         .withField(readOnly("summaryJson", QFieldType.TEXT).withLabel("Summary"))
         .withField(confidential(readOnly("checkpointJson", QFieldType.TEXT).withLabel("Checkpoint")))
         .withField(readOnly("errorMessage", QFieldType.TEXT))
         .withField(readOnly("startedAt", QFieldType.DATE_TIME))
         .withField(readOnly("finishedAt", QFieldType.DATE_TIME))
         .withField(readOnly("leaseVersion", QFieldType.INTEGER).withDefaultValue(0))
         .withField(readOnly("createDate", QFieldType.DATE_TIME))
         .withField(readOnly("modifyDate", QFieldType.DATE_TIME))
         .withSection(new QFieldSection("identity", new QIcon("badge"), Tier.T1, List.of("id", "jobId", "seq", "status", "attempt", "maxAttempts")))
         .withSection(new QFieldSection("progress", "Progress", new QIcon("speed"), Tier.T2, List.of("rowsIn", "rowsOk", "rowsFailed", "errorMessage", "rangeJson", "inputStorageRef")))
         .withSection(new QFieldSection("lease", "Lease", new QIcon("lock_clock"), Tier.T2, List.of("leaseOwner", "leaseToken", "leaseExpiresAt", "heartbeatAt", "leaseVersion")))
         .withSection(new QFieldSection("data", "Data", new QIcon("data_object"), Tier.T3, List.of("summaryJson", "checkpointJson")))
         .withSection(new QFieldSection("dates", new QIcon("calendar_month"), Tier.T3, List.of("startedAt", "finishedAt", "createDate", "modifyDate"))));
   }



   /*******************************************************************************
    ** jobs_row_error: which rows failed and why.
    *******************************************************************************/
   public QTableMetaData defineRowErrorTable(String backendName)
   {
      return (new QTableMetaData()
         .withName(TABLE_ROW_ERROR)
         .withLabel("Job Row Error")
         .withIcon(new QIcon("error_outline"))
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withRecordLabelFormat("Row %s")
         .withRecordLabelFields("rowNumber")
         .withRecordSecurityLock(ownerLock(TABLE_JOB + ".userId", List.of(childJoinName(TABLE_ROW_ERROR))))
         .withField(readOnly("id", QFieldType.INTEGER))
         .withField(readOnly("jobId", QFieldType.INTEGER).withLabel("Job").withPossibleValueSourceName(TABLE_JOB))
         .withField(readOnly("chunkId", QFieldType.INTEGER).withLabel("Chunk").withPossibleValueSourceName(TABLE_CHUNK))
         .withField(readOnly("rowNumber", QFieldType.INTEGER))
         .withField(readOnly("recordPk", QFieldType.STRING).withLabel("Record Key").withMaxLength(250))
         .withField(readOnly("fieldName", QFieldType.STRING).withMaxLength(250))
         .withField(readOnly("errorCode", QFieldType.STRING).withMaxLength(100))
         .withField(confidential(readOnly("message", QFieldType.TEXT)))
         .withField(readOnly("retryJobId", QFieldType.INTEGER).withLabel("Retry Job").withPossibleValueSourceName(TABLE_JOB))
         .withField(field("resolved", QFieldType.BOOLEAN).withDefaultValue(false))
         .withField(readOnly("createDate", QFieldType.DATE_TIME))
         .withSection(new QFieldSection("identity", new QIcon("badge"), Tier.T1, List.of("id", "jobId", "chunkId", "rowNumber", "recordPk")))
         .withSection(new QFieldSection("error", "Error", new QIcon("error_outline"), Tier.T2, List.of("fieldName", "errorCode", "message", "retryJobId", "resolved")))
         .withSection(new QFieldSection("dates", new QIcon("calendar_month"), Tier.T3, List.of("createDate"))));
   }



   /*******************************************************************************
    ** jobs_effect: the transactional outbox of side effects outside the job's
    ** database (spec section 2.13). Written in the chunk's transaction; delivered
    ** by the dispatcher; idempotencyKey is unique (jobId:chunkSeq:rowKey:effect).
    *******************************************************************************/
   public QTableMetaData defineEffectTable(String backendName)
   {
      return (new QTableMetaData()
         .withName(TABLE_EFFECT)
         .withLabel("Job Effect")
         .withIcon(new QIcon("outbox"))
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withRecordLabelFormat("%s %s")
         .withRecordLabelFields("effectName", "rowKey")
         .withUniqueKey(new UniqueKey("idempotencyKey"))
         .withRecordSecurityLock(ownerLock(TABLE_JOB + ".userId", List.of(childJoinName(TABLE_EFFECT))))
         .withField(readOnly("id", QFieldType.INTEGER))
         .withField(readOnly("jobId", QFieldType.INTEGER).withLabel("Job").withPossibleValueSourceName(TABLE_JOB))
         .withField(readOnly("effectName", QFieldType.STRING).withLabel("Effect").withMaxLength(100))
         .withField(readOnly("status", QFieldType.STRING).withPossibleValueSourceName(EffectStatus.NAME).withMaxLength(30))
         .withField(readOnly("needsReview", QFieldType.BOOLEAN).withDefaultValue(false))
         .withField(readOnly("rowKey", QFieldType.STRING).withLabel("Row Key").withMaxLength(250))
         .withField(readOnly("targetName", QFieldType.STRING).withLabel("Target").withMaxLength(100))
         .withField(readOnly("effectType", QFieldType.STRING).withPossibleValueSourceName(JobEffectType.NAME).withMaxLength(30))
         .withField(readOnly("chunkId", QFieldType.INTEGER).withLabel("Chunk").withPossibleValueSourceName(TABLE_CHUNK))
         .withField(readOnly("chunkSeq", QFieldType.INTEGER).withLabel("Chunk Sequence"))
         .withField(readOnly("idempotencyKey", QFieldType.STRING).withMaxLength(500))
         .withField(confidential(readOnly("payloadJson", QFieldType.TEXT).withLabel("Payload")))
         .withField(readOnly("payloadRef", QFieldType.STRING).withLabel("Payload Object").withMaxLength(1000))
         .withField(readOnly("attempt", QFieldType.INTEGER).withDefaultValue(0))
         .withField(readOnly("maxAttempts", QFieldType.INTEGER))
         .withField(readOnly("reconcileAttempts", QFieldType.INTEGER).withDefaultValue(0))
         .withField(readOnly("nextAttemptAt", QFieldType.DATE_TIME))
         .withField(readOnly("leaseOwner", QFieldType.STRING).withMaxLength(250))
         .withField(readOnly("leaseToken", QFieldType.STRING).withMaxLength(100))
         .withField(readOnly("leaseExpiresAt", QFieldType.DATE_TIME))
         .withField(readOnly("leaseVersion", QFieldType.INTEGER).withDefaultValue(0))
         .withField(readOnly("providerRef", QFieldType.STRING).withLabel("Provider Reference").withMaxLength(500))
         .withField(confidential(readOnly("lastError", QFieldType.TEXT)))
         .withField(readOnly("sentAt", QFieldType.DATE_TIME))
         .withField(readOnly("createDate", QFieldType.DATE_TIME))
         .withField(readOnly("modifyDate", QFieldType.DATE_TIME))
         .withSection(new QFieldSection("identity", new QIcon("badge"), Tier.T1, List.of("id", "jobId", "effectName", "status", "needsReview", "rowKey")))
         .withSection(new QFieldSection("delivery", "Delivery", new QIcon("outbox"), Tier.T2, List.of("targetName", "effectType", "attempt", "maxAttempts", "reconcileAttempts", "nextAttemptAt", "providerRef", "lastError", "sentAt")))
         .withSection(new QFieldSection("source", "Source", new QIcon("input"), Tier.T2, List.of("chunkId", "chunkSeq", "idempotencyKey", "payloadJson", "payloadRef")))
         .withSection(new QFieldSection("lease", "Lease", new QIcon("lock_clock"), Tier.T3, List.of("leaseOwner", "leaseToken", "leaseExpiresAt", "leaseVersion")))
         .withSection(new QFieldSection("dates", new QIcon("calendar_month"), Tier.T3, List.of("createDate", "modifyDate"))));
   }



   /*******************************************************************************
    ** jobs_event: the timeline of a job. Type leads the fields: most events are
    ** job-level, so a (usually empty) chunk column first would leave lists, such as
    ** the job's Events child list, without a readable first column.
    *******************************************************************************/
   public QTableMetaData defineEventTable(String backendName)
   {
      return (new QTableMetaData()
         .withName(TABLE_EVENT)
         .withLabel("Job Event")
         .withIcon(new QIcon("timeline"))
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("type")
         .withRecordSecurityLock(ownerLock(TABLE_JOB + ".userId", List.of(childJoinName(TABLE_EVENT))))
         .withField(readOnly("id", QFieldType.INTEGER))
         .withField(readOnly("jobId", QFieldType.INTEGER).withLabel("Job").withPossibleValueSourceName(TABLE_JOB))
         .withField(readOnly("type", QFieldType.STRING).withPossibleValueSourceName(JobEventType.NAME).withMaxLength(30))
         .withField(readOnly("createDate", QFieldType.DATE_TIME))
         .withField(readOnly("detail", QFieldType.TEXT))
         .withField(readOnly("chunkId", QFieldType.INTEGER).withLabel("Chunk").withPossibleValueSourceName(TABLE_CHUNK))
         .withField(readOnly("actorUserId", QFieldType.STRING).withLabel("Actor").withMaxLength(250))
         .withField(readOnly("clientChannel", QFieldType.STRING).withMaxLength(250))
         .withField(readOnly("pod", QFieldType.STRING).withMaxLength(250))
         .withSection(new QFieldSection("identity", new QIcon("badge"), Tier.T1, List.of("id", "jobId", "type", "createDate", "chunkId")))
         .withSection(new QFieldSection("details", "Details", new QIcon("text_snippet"), Tier.T2, List.of("actorUserId", "clientChannel", "pod", "detail"))));
   }



   /*******************************************************************************
    ** jobs_worker: which pods run jobs. Leases never depend on it.
    *******************************************************************************/
   public QTableMetaData defineWorkerTable(String backendName)
   {
      return (new QTableMetaData()
         .withName(TABLE_WORKER)
         .withLabel("Job Worker")
         .withIcon(new QIcon("dns"))
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("podId")
         .withUniqueKey(new UniqueKey("podId"))
         .withField(readOnly("id", QFieldType.INTEGER))
         .withField(readOnly("podId", QFieldType.STRING).withLabel("Pod").withMaxLength(250))
         .withField(readOnly("startedAt", QFieldType.DATE_TIME))
         .withField(readOnly("heartbeatAt", QFieldType.DATE_TIME))
         .withField(readOnly("threads", QFieldType.INTEGER))
         .withField(readOnly("draining", QFieldType.BOOLEAN))
         .withField(readOnly("version", QFieldType.STRING).withMaxLength(100))
         .withSection(new QFieldSection("identity", new QIcon("badge"), Tier.T1, List.of("id", "podId", "threads", "draining", "version", "startedAt", "heartbeatAt"))));
   }




   /*******************************************************************************
    ** jobs_upload: files uploaded through POST /uploads (the bytes live in the
    ** uploads storage table; this row is the opaque ref, its owner and expiry).
    *******************************************************************************/
   public QTableMetaData defineUploadTable(String backendName)
   {
      return (new QTableMetaData()
         .withName(TABLE_UPLOAD)
         .withLabel("Job Upload")
         .withIcon(new QIcon("upload_file"))
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("fileName")
         .withUniqueKey(new UniqueKey("ref"))
         .withRecordSecurityLock(ownerLock("userId", null))
         .withField(readOnly("id", QFieldType.INTEGER))
         .withField(readOnly("ref", QFieldType.STRING).withMaxLength(100).withLabel("Upload Ref"))
         .withField(readOnly("userId", QFieldType.STRING).withLabel("Owner").withMaxLength(250))
         .withField(readOnly("fileName", QFieldType.STRING).withMaxLength(250))
         .withField(readOnly("contentType", QFieldType.STRING).withMaxLength(250))
         .withField(readOnly("sizeBytes", QFieldType.LONG))
         .withField(readOnly("storageTableName", QFieldType.STRING).withMaxLength(250))
         .withField(readOnly("storageReference", QFieldType.STRING).withMaxLength(1000))
         .withField(readOnly("expiresAt", QFieldType.DATE_TIME))
         .withField(readOnly("jobId", QFieldType.INTEGER).withLabel("Last Job").withPossibleValueSourceName(TABLE_JOB))
         .withField(readOnly("createDate", QFieldType.DATE_TIME))
         .withSection(new QFieldSection("identity", new QIcon("badge"), Tier.T1, List.of("id", "fileName", "userId", "sizeBytes", "contentType", "expiresAt", "jobId")))
         .withSection(new QFieldSection("storage", "Storage", new QIcon("folder"), Tier.T2, List.of("ref", "storageTableName", "storageReference", "createDate"))));
   }



   /*******************************************************************************
    ** Append-only redacted log, secured through the parent job.
    *******************************************************************************/
   public QTableMetaData defineLogTable(String backendName)
   {
      return (new QTableMetaData()
         .withName(TABLE_LOG)
         .withLabel("Job Log")
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withRecordLabelFormat("%s")
         .withRecordLabelFields("message")
         .withPermissionRules(new QPermissionRules().withCustomPermissionChecker(new QCodeReference(JobLogPermissions.class)))
         .withRecordSecurityLock(ownerLock(TABLE_JOB + ".userId", List.of(childJoinName(TABLE_LOG))))
         .withField(readOnly("id", QFieldType.LONG))
         .withField(readOnly("jobId", QFieldType.INTEGER).withPossibleValueSourceName(TABLE_JOB))
         .withField(readOnly("chunkId", QFieldType.INTEGER).withPossibleValueSourceName(TABLE_CHUNK))
         .withField(readOnly("attempt", QFieldType.INTEGER))
         .withField(readOnly("level", QFieldType.STRING).withMaxLength(10))
         .withField(readOnly("loggedAt", QFieldType.DATE_TIME))
         .withField(readOnly("phase", QFieldType.STRING).withMaxLength(250))
         .withField(readOnly("message", QFieldType.STRING).withMaxLength(1000))
         .withSection(new QFieldSection("log", new QIcon("notes"), Tier.T1, List.of("id", "loggedAt", "level", "phase", "message", "jobId", "chunkId", "attempt"))));
   }
}
