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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobSourceType;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRef;
import com.kingsrook.qqq.backend.core.jobs.uploads.UploadRefs;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaUpdateStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTestSupport.awaitStatus;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** The CHUNKED plans on the memory backend: key ranges over a table filter, and
 ** an uploaded file split into chunk objects that keep the file row numbers.
 *******************************************************************************/
class ChunkedJobsTest extends BaseTest
{
   static final String TABLE_ITEM     = "jobs_test_item";
   static final String PROCESS_DOUBLE = "jobsTestDoubleAmount";



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      DurableJobsRuntime.stop(QContext.getQInstance(), Duration.ofSeconds(5));
   }



   /*******************************************************************************
    ** An instance with the jobs tables, a test item table and a doubling process.
    *******************************************************************************/
   static QInstance defineInstance() throws QException
   {
      QInstance instance = DurableJobsTestSupport.defineInstance(DurableJobsTestSupport.fastSettings().withStorageTableName(TestUtils.TABLE_NAME_MEMORY_STORAGE));
      instance.addTable(new QTableMetaData()
         .withName(TABLE_ITEM)
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("code"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("code", QFieldType.STRING).withIsRequired(true))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true))
         .withField(new QFieldMetaData("amount", QFieldType.INTEGER)));
      instance.addProcess(StreamedETLWithFrontendProcess.processMetaDataBuilder()
         .withName(PROCESS_DOUBLE)
         .withTableName(TABLE_ITEM)
         .withSourceTable(TABLE_ITEM)
         .withDestinationTable(TABLE_ITEM)
         .withExtractStepClass(ExtractViaQueryStep.class)
         .withTransformStepClass(DoubleTransform.class)
         .withLoadStepClass(LoadViaUpdateStep.class)
         .getProcessMetaData());
      return (instance);
   }



   /*******************************************************************************
    ** A table job is split into key ranges of chunkSize rows; each chunk runs the
    ** process's own transform and load; a row the transform rejects is recorded
    ** in jobs_row_error by key; the job completes with errors.
    *******************************************************************************/
   @Test
   void testTableFilterChunks() throws QException
   {
      QInstance instance = defineInstance();
      QContext.init(instance, newSession());
      List<QRecord> items = new ArrayList<>();
      for(int i = 1; i <= 10; i++)
      {
         items.add(new QRecord().withValue("code", i == 7 ? "bad" : "c" + i).withValue("name", "Item " + i).withValue("amount", i));
      }
      new InsertAction().execute(new InsertInput(TABLE_ITEM).withRecords(items));

      Integer jobId = DurableJobs.launch(new LaunchRequest(PROCESS_DOUBLE).withFilter(new QQueryFilter()));
      DurableJobStore store = new DurableJobStore(DurableJobsMetaData.of(instance));
      QRecord launched = store.getJob(jobId);
      assertThat(launched.getValueString("sourceType")).isEqualTo(JobSourceType.TABLE_FILTER.name());
      assertThat(store.getChunks(jobId)).hasSize(1);

      DurableJobs.control(jobId, com.kingsrook.qqq.backend.core.jobs.model.JobAction.CANCEL);

      ////////////////////////////////////////////////////
      // a launch with chunkSize 3 (process meta data) //
      ////////////////////////////////////////////////////
      instance.getProcess(PROCESS_DOUBLE).withSupplementalMetaData(new DurableProcessMetaData().withChunkSize(3));
      Integer jobId2 = DurableJobs.launch(new LaunchRequest(PROCESS_DOUBLE).withFilter(new QQueryFilter()));
      assertThat(store.getChunks(jobId2)).hasSize(4);
      assertThat(store.getJob(jobId2).getValueInteger("rowsTotal")).isEqualTo(10);

      DurableJobsRuntime.start(instance);
      QRecord job = awaitStatus(jobId2, Duration.ofSeconds(20), JobStatus.COMPLETED, JobStatus.COMPLETED_WITH_ERRORS, JobStatus.FAILED);
      assertThat(job.getValueString("status")).isEqualTo(JobStatus.COMPLETED_WITH_ERRORS.name());
      assertThat(job.getValueInteger("rowsFailed")).isEqualTo(1);
      assertThat(job.getValueInteger("rowsOk")).isEqualTo(9);
      assertThat(store.getChunks(jobId2)).allMatch(c -> ChunkStatus.DONE.name().equals(c.getValueString("status")));

      List<QRecord> after = QueryAction.execute(TABLE_ITEM, new QQueryFilter().withOrderBy(new QFilterOrderBy("id")));
      assertThat(after.get(0).getValueInteger("amount")).isEqualTo(2);
      assertThat(after.get(6).getValueInteger("amount")).isEqualTo(7);
      assertThat(after.get(9).getValueInteger("amount")).isEqualTo(20);

      List<QRecord> rowErrors = store.query(DurableJobsTables.TABLE_ROW_ERROR, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId2)));
      assertThat(rowErrors).hasSize(1);
      assertThat(rowErrors.get(0).getValueString("recordPk")).isEqualTo(String.valueOf(after.get(6).getValueInteger("id")));
      assertThat(rowErrors.get(0).getValueString("message")).contains("bad code");
   }



   /*******************************************************************************
    ** An uploaded CSV is split into chunk objects; each chunk runs the bulk
    ** insert process headless; bad rows are recorded with their FILE row
    ** numbers; good rows load.
    *******************************************************************************/
   @Test
   void testFileChunks() throws QException
   {
      QInstance instance = defineInstance();
      QContext.init(instance, newSession());

      StringBuilder csv = new StringBuilder("code,name,amount\n");
      for(int i = 1; i <= 25; i++)
      {
         csv.append("k").append(i).append(",Name ").append(i).append(",").append(i == 5 || i == 18 ? "notANumber" : String.valueOf(i)).append("\n");
      }
      UploadRef ref = UploadRefs.store("items.csv", "text/csv", new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8)));
      assertThat(ref.sizeBytes()).isEqualTo(csv.length());

      String profile = """
         {"version":"v1","layout":"FLAT","hasHeaderRow":true,"fieldList":[
           {"fieldName":"code","columnIndex":0},{"fieldName":"name","columnIndex":1},{"fieldName":"amount","columnIndex":2}]}""";
      instance.getProcess(TABLE_ITEM + ".bulkInsert").withSupplementalMetaData(new DurableProcessMetaData().withChunkSize(10));
      Integer jobId = DurableJobs.launch(new LaunchRequest(TABLE_ITEM + ".bulkInsert").withUploadRefs(List.of(ref.ref())).withProfileJson(profile).withValue("tableName", TABLE_ITEM));

      DurableJobStore store = new DurableJobStore(DurableJobsMetaData.of(instance));
      assertThat(store.getJob(jobId).getValueString("sourceType")).isEqualTo(JobSourceType.STORAGE_FILE.name());
      assertThat(store.getChunks(jobId)).hasSize(3);
      assertThat(store.getJob(jobId).getValueInteger("rowsTotal")).isEqualTo(25);

      DurableJobsRuntime.start(instance);
      QRecord job = awaitStatus(jobId, Duration.ofSeconds(30), JobStatus.COMPLETED, JobStatus.COMPLETED_WITH_ERRORS, JobStatus.FAILED);
      assertThat(job.getValueString("status")).as(job.getValueString("errorMessage")).isEqualTo(JobStatus.COMPLETED_WITH_ERRORS.name());
      assertThat(QueryAction.execute(TABLE_ITEM, new QQueryFilter())).hasSize(23);

      List<QRecord> rowErrors = store.query(DurableJobsTables.TABLE_ROW_ERROR, new QQueryFilter(new QFilterCriteria("jobId", QCriteriaOperator.EQUALS, jobId)).withOrderBy(new QFilterOrderBy("rowNumber")));
      assertThat(rowErrors.stream().map(r -> r.getValueInteger("rowNumber")).toList()).containsExactly(6, 19);
      assertThat(job.getValueInteger("rowsFailed")).isEqualTo(2);
      assertThat(job.getValueInteger("rowsOk")).isEqualTo(23);
   }



   /*******************************************************************************
    ** Doubles each item's amount; rejects code "bad".
    *******************************************************************************/
   public static class DoubleTransform extends AbstractTransformStep
   {
      private final ProcessSummaryLine ok = new ProcessSummaryLine(Status.OK, 0, "doubled");



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         for(QRecord record : input.getRecords())
         {
            QRecord updated = new QRecord().withValue("id", record.getValue("id")).withValue("amount", record.getValueInteger("amount") * 2);
            if("bad".equals(record.getValueString("code")))
            {
               updated.addError(new BadInputStatusMessage("bad code"));
            }
            else
            {
               ok.incrementCountAndAddPrimaryKey(record.getValue("id"));
            }
            output.addRecord(updated);
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         return (new ArrayList<>(List.of(ok)));
      }
   }

}
