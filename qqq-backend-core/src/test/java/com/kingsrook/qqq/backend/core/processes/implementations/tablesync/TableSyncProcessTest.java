/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.tablesync;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractExtractStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.utils.GeneralProcessUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for TableSyncProcess
 *******************************************************************************/
class TableSyncProcessTest extends BaseTest
{
   private static final String PROCESS_NAME           = "testSyncProcess";
   private static final String TABLE_NAME_PEOPLE_SYNC = "peopleSync";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      setupDataAndMetaData(ExtractViaQueryStep.class);

      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(PROCESS_NAME);
      runProcessInput.addValue("recordIds", "1,2,3,4,5");
      runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);

      RunProcessAction runProcessAction = new RunProcessAction();
      RunProcessOutput runProcessOutput = runProcessAction.execute(runProcessInput);

      @SuppressWarnings("unchecked")
      ArrayList<ProcessSummaryLineInterface> processResults = (ArrayList<ProcessSummaryLineInterface>) runProcessOutput.getValues().get("processResults");

      assertThat(processResults.get(0))
         .hasFieldOrPropertyWithValue("message", "were inserted")
         .hasFieldOrPropertyWithValue("count", 3);

      assertThat(processResults.get(1))
         .hasFieldOrPropertyWithValue("message", "were updated")
         .hasFieldOrPropertyWithValue("count", 2);

      List<QRecord> syncedRecords = TestUtils.queryTable(qInstance, TABLE_NAME_PEOPLE_SYNC);
      assertEquals(5, syncedRecords.size());

      /////////////////////////////////////////////////////////////////
      // make sure the record referencing 3 has had its name updated //
      // and the one referencing 5 stayed the same                   //
      /////////////////////////////////////////////////////////////////
      Map<Serializable, QRecord> syncPersonsBySourceId = GeneralProcessUtils.loadTableToMap(TABLE_NAME_PEOPLE_SYNC, "sourcePersonId");
      assertEquals("Tyler", syncPersonsBySourceId.get(3).getValueString("firstName"));
      assertEquals("Homer", syncPersonsBySourceId.get(5).getValueString("firstName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setupDataAndMetaData(Class<? extends AbstractExtractStep> extractStepClass) throws QException
   {
      QInstance      qInstance   = QContext.getQInstance();
      QTableMetaData personTable = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      qInstance.addTable(new QTableMetaData()
         .withName(TABLE_NAME_PEOPLE_SYNC)
         .withPrimaryKeyField("id")
         .withBackendName(TestUtils.MEMORY_BACKEND_NAME)
         .withFields(personTable.getFields())
         .withField(new QFieldMetaData("sourcePersonId", QFieldType.INTEGER)));

      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Darin"),
         new QRecord().withValue("id", 2).withValue("firstName", "Tim"),
         new QRecord().withValue("id", 3).withValue("firstName", "Tyler"),
         new QRecord().withValue("id", 4).withValue("firstName", "James"),
         new QRecord().withValue("id", 5).withValue("firstName", "Homer")
      ));

      TestUtils.insertRecords(qInstance.getTable(TABLE_NAME_PEOPLE_SYNC), List.of(
         new QRecord().withValue("sourcePersonId", 3).withValue("firstName", "Garret"),
         new QRecord().withValue("sourcePersonId", 5).withValue("firstName", "Homer")
      ));

      qInstance.addProcess(TableSyncProcess.processMetaDataBuilder(false)
         .withName(PROCESS_NAME)
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withSyncTransformStepClass(PersonTransformClass.class)
         .withExtractStepClass(extractStepClass)
         .getProcessMetaData());
   }



   /*******************************************************************************
    ** Handle a case where an extract step sends duplicate records (in the same page)
    ** into the transform & load steps.
    **
    ** Basically the same test as above - only we have a custom Extract step, that
    ** produces duplicates.
    *******************************************************************************/
   @Test
   void testDupesFromExtractStep() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      setupDataAndMetaData(TestExtractor.class);

      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(PROCESS_NAME);
      runProcessInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);

      RunProcessAction runProcessAction = new RunProcessAction();
      RunProcessOutput runProcessOutput = runProcessAction.execute(runProcessInput);

      @SuppressWarnings("unchecked")
      ArrayList<ProcessSummaryLineInterface> processResults = (ArrayList<ProcessSummaryLineInterface>) runProcessOutput.getValues().get("processResults");

      assertThat(processResults.get(0))
         .hasFieldOrPropertyWithValue("message", "were inserted")
         .hasFieldOrPropertyWithValue("count", 3);

      assertThat(processResults.get(1))
         .hasFieldOrPropertyWithValue("message", "were updated")
         .hasFieldOrPropertyWithValue("count", 2);

      List<QRecord> syncedRecords = TestUtils.queryTable(qInstance, TABLE_NAME_PEOPLE_SYNC);
      assertEquals(5, syncedRecords.size());

      /////////////////////////////////////////////////////////////////
      // make sure the record referencing 3 has had its name updated //
      // and the one referencing 5 stayed the same                   //
      /////////////////////////////////////////////////////////////////
      Map<Serializable, QRecord> syncPersonsBySourceId = GeneralProcessUtils.loadTableToMap(TABLE_NAME_PEOPLE_SYNC, "sourcePersonId");
      assertEquals("Tyler", syncPersonsBySourceId.get(3).getValueString("firstName"));
      assertEquals("Homer", syncPersonsBySourceId.get(5).getValueString("firstName"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PersonTransformClass extends AbstractTableSyncTransformStep
   {

      @Override
      public QRecord populateRecordToStore(RunBackendStepInput runBackendStepInput, QRecord destinationRecord, QRecord sourceRecord) throws QException
      {
         destinationRecord.setValue("sourcePersonId", sourceRecord.getValue("id"));
         destinationRecord.setValue("firstName", sourceRecord.getValue("firstName"));
         destinationRecord.setValue("lastName", sourceRecord.getValue("lastName"));
         return (destinationRecord);
      }



      @Override
      protected SyncProcessConfig getSyncProcessConfig()
      {
         SyncProcessConfig syncProcessConfig = new SyncProcessConfig(TestUtils.TABLE_NAME_PERSON_MEMORY, "id", "peopleSync", "sourcePersonId", true, true);
         syncProcessConfig.noop();
         return (syncProcessConfig);
      }

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class TestExtractor extends AbstractExtractStep
   {
      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         List<QRecord> qRecords = TestUtils.queryTable(QContext.getQInstance(), TestUtils.TABLE_NAME_PERSON_MEMORY);
         for(QRecord qRecord : qRecords)
         {
            getRecordPipe().addRecord(qRecord);
         }

         ////////////////////////////////////////
         // re-add records 1 and 5 to the pipe //
         ////////////////////////////////////////
         getRecordPipe().addRecord(qRecords.get(0));
         getRecordPipe().addRecord(qRecords.get(4));
      }
   }
}
