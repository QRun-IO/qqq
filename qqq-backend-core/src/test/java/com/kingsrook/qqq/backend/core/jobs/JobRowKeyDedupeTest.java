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


import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobIdempotency;
import com.kingsrook.qqq.backend.core.jobs.model.JobSourceType;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.plans.JobChunkScope;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRef;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRefs;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist.BulkLoadUploadSources;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.BulkLoadRecordUtils;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTestSupport.awaitStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** File row dedupe without a natural unique key or a shared transaction.
 *******************************************************************************/
class JobRowKeyDedupeTest extends BaseTest
{
   private static final String TABLE   = "jobRowKeyItems";
   private static final String PROCESS = TABLE + ".bulkInsert";
   private static final String PROFILE = """
      {"version":"v1","layout":"FLAT","hasHeaderRow":true,"fieldList":[
        {"fieldName":"name","columnIndex":0},{"fieldName":"amount","columnIndex":1}]}""";

   private QInstance       instance;
   private QTableMetaData  table;
   private DurableJobStore store;



   /*******************************************************************************
    ** Jobs and destination use separate memory backends; only jobRowKey is unique.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws QException
   {
      instance = DurableJobsTestSupport.defineInstance(DurableJobsTestSupport.fastSettings().withStorageTableName(TestUtils.TABLE_NAME_MEMORY_STORAGE));
      instance.addBackend(new QBackendMetaData().withName("destinationMemory").withBackendType(MemoryBackendModule.class));
      table = new QTableMetaData().withName(TABLE).withBackendName("destinationMemory").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("amount", QFieldType.INTEGER))
         .withField(new QFieldMetaData("jobRowKey", QFieldType.STRING))
         .withUniqueKey(new UniqueKey("jobRowKey"))
         .withSupplementalMetaData(new DurableTableMetaData().withJobRowKeyField("jobRowKey"));
      instance.addTable(table);
      QContext.init(instance, newSession());
      instance.getProcess(PROCESS).withSupplementalMetaData(new DurableProcessMetaData().withChunkSize(2));
      store = new DurableJobStore(DurableJobsMetaData.of(instance));
   }



   /*******************************************************************************
    ** Stop worker threads before BaseTest clears the context and memory store.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      DurableJobsRuntime.stop(instance, Duration.ofSeconds(5));
   }



   /*******************************************************************************
    ** A reclaimed completed chunk skips its previous inserts with no row errors.
    *******************************************************************************/
   @Test
   void testFileChunkRerun() throws Exception
   {
      Integer jobId = launch("name,amount\nSame,1\nSame,1\nSame,1\nSame,1\nSame,1\n");
      assertThat(store.getJob(jobId).getValueString("sourceType")).isEqualTo(JobSourceType.STORAGE_FILE.name());
      assertThat(store.getJob(jobId).getValueString("idempotency")).isEqualTo(JobIdempotency.NATURAL.name());
      assertThat(store.getChunks(jobId)).hasSize(3);
      DurableJobsRuntime.start(instance);
      awaitCompleted(jobId);
      assertThat(QueryAction.execute(TABLE, new QQueryFilter())).extracting(r -> r.getValueString("jobRowKey"))
         .containsExactlyInAnyOrder(jobId + ":2", jobId + ":3", jobId + ":4", jobId + ":5", jobId + ":6");
      assertThat(rowErrors(jobId)).isEmpty();

      rerunFirstChunk(jobId);
      assertThat(QueryAction.execute(TABLE, new QQueryFilter())).hasSize(5);
      assertThat(rowErrors(jobId)).isEmpty();
      assertThat(store.getJob(jobId).getValueInteger("rowsOk")).isEqualTo(5);
      assertThat(store.getJob(jobId).getValueInteger("rowsFailed")).isZero();
   }



   /*******************************************************************************
    ** Failed-file ROW_SET retries retain original row numbers and dedupe on rerun.
    *******************************************************************************/
   @Test
   void testRowSetRetryRerun() throws Exception
   {
      Integer jobId = launch("name,amount\nSame,1\nSame,bad\nSame,3\n");
      DurableJobsRuntime.start(instance);
      QRecord failed = awaitStatus(jobId, Duration.ofSeconds(20), JobStatus.COMPLETED_WITH_ERRORS, JobStatus.FAILED);
      assertThat(failed.getValueString("status")).isEqualTo(JobStatus.COMPLETED_WITH_ERRORS.name());
      assertThat(rowErrors(jobId)).extracting(r -> r.getValueInteger("rowNumber")).containsExactly(3);

      String corrected = """
         {"version":"v1","layout":"FLAT","hasHeaderRow":true,"fieldList":[{"fieldName":"name","columnIndex":0}]}""";
      Integer retryId = DurableJobs.retryFailedRows(jobId, corrected, null);
      assertThat(store.getJob(retryId).getValueString("sourceType")).isEqualTo(JobSourceType.ROW_SET.name());
      awaitCompleted(retryId);
      assertThat(QueryAction.execute(TABLE, new QQueryFilter())).extracting(r -> r.getValueString("jobRowKey"))
         .containsExactlyInAnyOrder(jobId + ":2", jobId + ":4", retryId + ":3");

      rerunFirstChunk(retryId);
      assertThat(QueryAction.execute(TABLE, new QQueryFilter())).hasSize(3);
      assertThat(rowErrors(retryId)).isEmpty();
   }



   /*******************************************************************************
    ** A plain headless bulk insert leaves the declared field unset.
    *******************************************************************************/
   @Test
   void testOutsideJob() throws Exception
   {
      UploadRef       ref   = upload("name,amount\nSame,1\nSame,1\n");
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(PROCESS);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      input.addValue("isHeadless", true);
      input.addValue("tableName", TABLE);
      input.addValue("bulkLoadProfileJSON", PROFILE);
      input.addValue("theFile", new ArrayList<>(List.of(BulkLoadUploadSources.open(ref.ref()))));
      RunProcessOutput output = new RunProcessAction().execute(input);
      assertThat(output.getException()).isEmpty();
      assertThat(QueryAction.execute(TABLE, new QQueryFilter())).hasSize(2).allMatch(r -> r.getValue("jobRowKey") == null);
   }



   /*******************************************************************************
    ** A scope without identity and records without file rows do not invent keys.
    *******************************************************************************/
   @Test
   void testRowKeyScopeAndFirstFileRow() throws Exception
   {
      QRecord fileRecord = BulkLoadRecordUtils.addBackendDetailsAboutFileRows(new QRecord().withValue("name", "file"),
         new ArrayList<>(List.of(new BulkLoadFileRow(new Serializable[] { "file" }, 7), new BulkLoadFileRow(new Serializable[] { "file" }, 8))));
      try(JobChunkScope ignored = JobChunkScope.open(null))
      {
         transform(List.of(fileRecord));
         assertThat(fileRecord.getValue("jobRowKey")).isNull();
      }

      QRecord noFileRows = new QRecord().withValue("name", "no file");
      QRecord emptyRows  = BulkLoadRecordUtils.addBackendDetailsAboutFileRows(new QRecord().withValue("name", "empty"), new ArrayList<>());
      try(JobChunkScope scope = JobChunkScope.open(null, null, new JobChunkScope.ChunkIdentity(42, 1, 1, PROCESS)))
      {
         transform(List.of(fileRecord, noFileRows, emptyRows));
         assertThat(fileRecord.getValueString("jobRowKey")).isEqualTo("42:7");
         assertThat(noFileRows.getValue("jobRowKey")).isNull();
         assertThat(emptyRows.getValue("jobRowKey")).isNull();
         assertThat(scope.getRowErrors()).isEmpty();
      }
   }



   /*******************************************************************************
    ** The declared dedupe field defaults bulk insert to NATURAL.
    *******************************************************************************/
   @Test
   void testDefaultIdempotencyAndValidMetadata() throws Exception
   {
      assertThat(DurableJobs.defaultIdempotency(instance.getProcess(PROCESS), JobSourceType.STORAGE_FILE)).isEqualTo(JobIdempotency.NATURAL);
      assertThat(DurableJobs.defaultIdempotency(instance.getProcess(PROCESS), JobSourceType.ROW_SET)).isEqualTo(JobIdempotency.NATURAL);
      instance.setHasBeenValidated(null);
      assertThatCode(() -> new QInstanceValidator().validate(instance)).doesNotThrowAnyException();
      assertThat(DurableTableMetaData.of(table.clone()).getJobRowKeyField()).isEqualTo("jobRowKey");
      assertThat(DurableTableMetaData.rowKey(12, 34)).isEqualTo("12:34");
      assertThat(DurableTableMetaData.of(null)).isNull();
   }



   /*******************************************************************************
    ** A missing declared field produces a clear validator error.
    *******************************************************************************/
   @Test
   void testMissingField()
   {
      DurableTableMetaData.of(table).withJobRowKeyField("missing");
      assertInvalid("jobRowKeyField missing does not exist");
   }



   /*******************************************************************************
    ** Row keys are strings, not numeric fields.
    *******************************************************************************/
   @Test
   void testNonStringField()
   {
      table.getField("jobRowKey").setType(QFieldType.INTEGER);
      assertInvalid("jobRowKeyField jobRowKey must be STRING");
   }



   /*******************************************************************************
    ** The destination must enforce uniqueness on the declared field.
    *******************************************************************************/
   @Test
   void testMissingUniqueKey()
   {
      table.setUniqueKeys(null);
      assertInvalid("requires a unique key consisting of exactly that field");
   }



   /*******************************************************************************
    ** A composite unique key cannot protect jobRowKey by itself.
    *******************************************************************************/
   @Test
   void testCompositeUniqueKey()
   {
      table.setUniqueKeys(new ArrayList<>(List.of(new UniqueKey("jobRowKey", "name"))));
      assertInvalid("requires a unique key consisting of exactly that field");
   }



   /*******************************************************************************
    ** Validate through the instance hook, not just the supplemental directly.
    *******************************************************************************/
   private void assertInvalid(String message)
   {
      instance.setHasBeenValidated(null);
      assertThatThrownBy(() -> new QInstanceValidator().validate(instance))
         .isInstanceOf(QInstanceValidationException.class).hasMessageContaining(message);
   }



   /*******************************************************************************
    ** Run the transform with the same setup used by its existing unit tests.
    *******************************************************************************/
   private void transform(List<QRecord> records) throws QException
   {
      BulkInsertTransformStep step   = new BulkInsertTransformStep();
      RunBackendStepInput     input  = new RunBackendStepInput();
      RunBackendStepOutput    output = new RunBackendStepOutput();
      input.setTableName(TABLE);
      input.setStepName(StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE);
      input.setRecords(records);
      step.preRun(input, output);
      step.runOnePage(input, output);
      assertThat(output.getRecords()).hasSize(records.size());
   }



   /*******************************************************************************
    ** Store a CSV using the same upload path as the application.
    *******************************************************************************/
   private UploadRef upload(String csv) throws QException
   {
      return (UploadRefs.store("items.csv", "text/csv", new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8))));
   }



   /*******************************************************************************
    ** Launch a file bulk insert with its profile and upload reference.
    *******************************************************************************/
   private Integer launch(String csv) throws QException
   {
      UploadRef ref = upload(csv);
      return (DurableJobs.launch(new LaunchRequest(PROCESS).withUploadRefs(List.of(ref.ref())).withProfileJson(PROFILE).withValue("tableName", TABLE)));
   }



   /*******************************************************************************
    ** Wait for completion, retaining a useful failure message.
    *******************************************************************************/
   private void awaitCompleted(Integer jobId) throws QException
   {
      QRecord job = awaitStatus(jobId, Duration.ofSeconds(20), JobStatus.COMPLETED, JobStatus.COMPLETED_WITH_ERRORS, JobStatus.FAILED);
      assertThat(job.getValueString("status")).as(job.getValueString("errorMessage")).isEqualTo(JobStatus.COMPLETED.name());
   }



   /*******************************************************************************
    ** Simulate reclaim after destination writes: preserve attempt and bump fencing.
    *******************************************************************************/
   private void rerunFirstChunk(Integer jobId) throws QException
   {
      DurableJobsRuntime.stop(instance, Duration.ofSeconds(5));
      QRecord chunk = store.getChunks(jobId).get(0);
      assertThat(chunk.getValueString("status")).isEqualTo(ChunkStatus.DONE.name());
      SystemScope.call(() -> new UpdateAction().execute(new UpdateInput(DurableJobsTables.TABLE_CHUNK).withRecords(List.of(
         new QRecord().withValue("id", chunk.getValue("id")).withValue("status", ChunkStatus.PENDING.name())
            .withValue("leaseVersion", chunk.getValueInteger("leaseVersion") + 1)))));
      SystemScope.call(() -> new UpdateAction().execute(new UpdateInput(DurableJobsTables.TABLE_JOB).withRecords(List.of(
         new QRecord().withValue("id", jobId).withValue("status", JobStatus.RUNNING.name())))));
      DurableJobsRuntime.start(instance);
      awaitCompleted(jobId);
      QRecord rerun = store.getChunks(jobId).stream().filter(c -> c.getValue("id").equals(chunk.getValue("id"))).findFirst().orElseThrow();
      assertThat(rerun.getValueString("status")).isEqualTo(ChunkStatus.DONE.name());
      assertThat(rerun.getValueInteger("attempt")).isEqualTo(chunk.getValueInteger("attempt") + 1);
      assertThat(rerun.getValueString("summaryJson")).contains("rows already loaded by an earlier attempt of this job").doesNotContain("duplicate values in a unique key");
      assertThat(rerun.getValueInteger("rowsFailed")).isZero();
   }



   /*******************************************************************************
    ** Query the job's recorded row errors under the store's system scope.
    *******************************************************************************/
   private List<QRecord> rowErrors(Integer jobId) throws QException
   {
      return (store.query(DurableJobsTables.TABLE_ROW_ERROR, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId))));
   }
}
