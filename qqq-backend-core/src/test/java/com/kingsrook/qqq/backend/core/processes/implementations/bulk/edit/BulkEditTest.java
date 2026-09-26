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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.edit;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreUpdateCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryRecordLink;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for full bulk edit process
 *******************************************************************************/
class BulkEditTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   @AfterEach
   void beforeAndAfterEach()
   {
      MemoryRecordStore.getInstance().reset();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      //////////////////////////////
      // insert some test records //
      //////////////////////////////
      QInstance qInstance = QContext.getQInstance();
      TestUtils.insertRecords(qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY), List.of(
         new QRecord().withValue("id", 1).withValue("firstName", "Darin").withValue("lastName", "Kelkhoff").withValue("email", "darin.kelkhoff@kingsrook.com"),
         new QRecord().withValue("id", 2).withValue("firstName", "Tim").withValue("lastName", "Chamberlain").withValue("email", "tim.chamberlain@kingsrook.com"),
         new QRecord().withValue("id", 3).withValue("firstName", "James").withValue("lastName", "Maes").withValue("email", "james.maes@kingsrook.com")
      ));

      //////////////////////////////////
      // set up the run-process input //
      //////////////////////////////////
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(TestUtils.TABLE_NAME_PERSON_MEMORY + ".bulkEdit");
      runProcessInput.addValue(StreamedETLWithFrontendProcess.FIELD_DEFAULT_QUERY_FILTER,
         new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.IN, List.of(1, 2))));

      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
      String           processUUID      = runProcessOutput.getProcessUUID();

      runProcessInput.addValue(BulkEditTransformStep.FIELD_ENABLED_FIELDS, "firstName,email,birthDate");
      runProcessInput.addValue("firstName", "Johnny");
      runProcessInput.addValue("email", null);
      runProcessInput.addValue("birthDate", "1909-01-09");

      runProcessInput.setProcessUUID(processUUID);
      runProcessInput.setStartAfterStep("edit");
      runProcessOutput = new RunProcessAction().execute(runProcessInput);
      assertThat(runProcessOutput.getRecords()).hasSize(2);
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("review");

      runProcessInput.addValue(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true);
      runProcessInput.setStartAfterStep("review");
      runProcessOutput = new RunProcessAction().execute(runProcessInput);
      assertThat(runProcessOutput.getRecords()).hasSize(2);
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("review");
      assertThat(runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY)).isNotNull().isInstanceOf(List.class);

      runProcessInput.setStartAfterStep("review");
      runProcessOutput = new RunProcessAction().execute(runProcessInput);
      assertThat(runProcessOutput.getRecords()).hasSize(2);
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("result");
      assertThat(runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY)).isNotNull().isInstanceOf(List.class);
      assertThat(runProcessOutput.getException()).isEmpty();

      @SuppressWarnings("unchecked")
      List<ProcessSummaryLine> processSummaryLines = (List<ProcessSummaryLine>) runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY);
      assertThat(processSummaryLines).hasSize(4);
      assertThat(processSummaryLines.stream().filter(psl -> psl.getStatus().equals(Status.OK))).hasSize(1);
      List<ProcessSummaryLine> infoLines = processSummaryLines.stream().filter(psl -> psl.getStatus().equals(Status.INFO)).collect(Collectors.toList());
      assertThat(infoLines).hasSize(3);
      assertThat(infoLines.stream().map(ProcessSummaryLine::getMessage)).anyMatch(m -> m.matches("(?s).*First Name.*Johnny.*"));
      assertThat(infoLines.stream().map(ProcessSummaryLine::getMessage)).anyMatch(m -> m.matches("(?s).*Email was cleared.*"));
      assertThat(infoLines.stream().map(ProcessSummaryLine::getMessage)).anyMatch(m -> m.matches("(?s).*Birth Date.*1909-01-09.*"));

      /////////////////////////////////////////////////////////////////////////////////
      // query for the edited records - assert that id 1 & 2 were updated, 3 was not //
      /////////////////////////////////////////////////////////////////////////////////
      List<QRecord> records = TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertEquals("Johnny", records.get(0).getValueString("firstName"));
      assertEquals("Johnny", records.get(1).getValueString("firstName"));
      assertEquals("James", records.get(2).getValueString("firstName"));
      assertEquals("1909-01-09", records.get(0).getValueString("birthDate"));
      assertEquals("1909-01-09", records.get(1).getValueString("birthDate"));
      assertNull(records.get(2).getValueString("birthDate"));
      assertNull(records.get(0).getValue("email"));
      assertNull(records.get(1).getValue("email"));
      assertEquals("james.maes@kingsrook.com", records.get(2).getValue("email"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWarningsAndErrors() throws QException
   {
      //////////////////////////////
      // insert some test records //
      //////////////////////////////
      QInstance      qInstance     = QContext.getQInstance();
      QTableMetaData table         = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      List<QRecord>  personsToLoad = new ArrayList<>();
      for(int i = 0; i < 100; i++)
      {
         personsToLoad.add(new QRecord().withValue("id", i).withValue("firstName", "Darin" + i));
      }
      TestUtils.insertRecords(table, personsToLoad);

      table.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(PersonPreUpdateReusedMessages.class));

      //////////////////////////////////
      // set up the run-process input //
      //////////////////////////////////
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(TestUtils.TABLE_NAME_PERSON_MEMORY + ".bulkEdit");
      runProcessInput.addValue(StreamedETLWithFrontendProcess.FIELD_DEFAULT_QUERY_FILTER,
         new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.LESS_THAN_OR_EQUALS, 100)));

      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
      String           processUUID      = runProcessOutput.getProcessUUID();

      runProcessInput.addValue(BulkEditTransformStep.FIELD_ENABLED_FIELDS, "firstName");
      runProcessInput.addValue("firstName", "Johnny");

      runProcessInput.setProcessUUID(processUUID);
      runProcessInput.setStartAfterStep("edit");
      runProcessOutput = new RunProcessAction().execute(runProcessInput);
      assertThat(runProcessOutput.getRecords()).hasSize(0);
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("review");

      runProcessInput.addValue(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true);
      runProcessInput.setStartAfterStep("review");
      runProcessOutput = new RunProcessAction().execute(runProcessInput);
      assertThat(runProcessOutput.getRecords()).hasSize(20);
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("review");
      assertThat(runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_VALIDATION_SUMMARY)).isNotNull().isInstanceOf(List.class);

      runProcessInput.setStartAfterStep("review");
      runProcessOutput = new RunProcessAction().execute(runProcessInput);
      assertThat(runProcessOutput.getRecords()).hasSize(20);
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("result");
      assertThat(runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY)).isNotNull().isInstanceOf(List.class);
      assertThat(runProcessOutput.getException()).isEmpty();

      @SuppressWarnings("unchecked")
      List<ProcessSummaryLine> processSummaryLines = (List<ProcessSummaryLine>) runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY);
      assertThat(processSummaryLines).hasSize(4);

      assertThat(processSummaryLines.get(0))
         .hasFieldOrPropertyWithValue("status", Status.OK)
         .hasFieldOrPropertyWithValue("count", 10)
         .matches(psl -> psl.getMessage().contains("edited with no warnings"), "expected message");

      assertThat(processSummaryLines.get(1))
         .hasFieldOrPropertyWithValue("status", Status.ERROR)
         .hasFieldOrPropertyWithValue("count", 60)
         .matches(psl -> psl.getMessage().contains("Id less than 60 is error"), "expected message");

      assertThat(processSummaryLines.get(2))
         .hasFieldOrPropertyWithValue("status", Status.WARNING)
         .hasFieldOrPropertyWithValue("count", 30)
         .matches(psl -> psl.getMessage().contains("Id less than 90 is warning"), "expected message");

      List<ProcessSummaryLine> infoLines = processSummaryLines.stream().filter(psl -> psl.getStatus().equals(Status.INFO)).collect(Collectors.toList());
      assertThat(infoLines).hasSize(1);
      assertThat(infoLines.stream().map(ProcessSummaryLine::getMessage)).anyMatch(m -> m.matches("(?s).*First Name.*Johnny.*"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PersonPreUpdateReusedMessages extends AbstractPreUpdateCustomizer
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records) throws QException
      {
         for(QRecord record : records)
         {
            Integer id = record.getValueInteger("id");
            if(id < 60)
            {
               record.addError(new BadInputStatusMessage("Id less than 60 is error."));
            }
            else if(id < 90)
            {
               record.addWarning(new QWarningMessage("Id less than 90 is warning."));
            }
         }

         return (records);
      }

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUniqueWarningsAndErrors() throws QException
   {
      //////////////////////////////
      // insert some test records //
      //////////////////////////////
      QInstance      qInstance     = QContext.getQInstance();
      QTableMetaData table         = qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY);
      List<QRecord>  personsToLoad = new ArrayList<>();
      for(int i = 0; i < 100; i++)
      {
         personsToLoad.add(new QRecord().withValue("id", i).withValue("firstName", "Darin" + i));
      }
      TestUtils.insertRecords(table, personsToLoad);

      table.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(PersonPreUpdateUniqueMessages.class));

      //////////////////////////////////
      // set up the run-process input //
      //////////////////////////////////
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(TestUtils.TABLE_NAME_PERSON_MEMORY + ".bulkEdit");
      runProcessInput.addValue(StreamedETLWithFrontendProcess.FIELD_DEFAULT_QUERY_FILTER,
         new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.LESS_THAN_OR_EQUALS, 100)));

      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
      String           processUUID      = runProcessOutput.getProcessUUID();

      runProcessInput.addValue(BulkEditTransformStep.FIELD_ENABLED_FIELDS, "firstName");
      runProcessInput.addValue("firstName", "Johnny");

      runProcessInput.setProcessUUID(processUUID);
      runProcessInput.setStartAfterStep("edit");
      runProcessOutput = new RunProcessAction().execute(runProcessInput);

      runProcessInput.addValue(StreamedETLWithFrontendProcess.FIELD_DO_FULL_VALIDATION, true);
      runProcessInput.setStartAfterStep("review");
      runProcessOutput = new RunProcessAction().execute(runProcessInput);

      runProcessInput.setStartAfterStep("review");
      runProcessOutput = new RunProcessAction().execute(runProcessInput);

      @SuppressWarnings("unchecked")
      List<ProcessSummaryLineInterface> processSummaryLines = (List<ProcessSummaryLineInterface>) runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY);
      assertThat(processSummaryLines).hasSize(1 + 50 + 1 + 30 + 1);

      int index = 0;
      assertThat(processSummaryLines.get(index++))
         .hasFieldOrPropertyWithValue("status", Status.OK)
         .hasFieldOrPropertyWithValue("count", 10)
         .matches(psl -> psl.getMessage().contains("edited with no warnings"), "expected message");

      for(int i = 0; i < 50; i++)
      {
         assertThat(processSummaryLines.get(index++))
            .hasFieldOrPropertyWithValue("status", Status.ERROR)
            .isInstanceOf(ProcessSummaryRecordLink.class) // this is because it's a singleton, so we get rid of the "1 had an error" thing (doReplaceSingletonCountLinesWithSuffixOnly)
            .matches(psl -> psl.getMessage().contains("less than 60 is error"), "expected message");
      }

      assertThat(processSummaryLines.get(index++))
         .hasFieldOrPropertyWithValue("status", Status.ERROR)
         .hasFieldOrPropertyWithValue("count", 10)
         .matches(psl -> psl.getMessage().contains("had other errors"), "expected message");

      for(int i = 0; i < 30; i++)
      {
         assertThat(processSummaryLines.get(index++))
            .hasFieldOrPropertyWithValue("status", Status.WARNING)
            .isInstanceOf(ProcessSummaryRecordLink.class) // this is because it's a singleton, so we get rid of the "1 had an error" thing (doReplaceSingletonCountLinesWithSuffixOnly)
            .matches(psl -> psl.getMessage().contains("less than 90 is warning"), "expected message");
      }

      assertThat(processSummaryLines.get(index++))
         .hasFieldOrPropertyWithValue("status", Status.INFO)
         .matches(psl -> psl.getMessage().matches("(?s).*First Name.*Johnny.*"), "expected message");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PersonPreUpdateUniqueMessages extends AbstractPreUpdateCustomizer
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> apply(List<QRecord> records) throws QException
      {
         for(QRecord record : records)
         {
            Integer id = record.getValueInteger("id");
            if(id < 60)
            {
               record.addError(new BadInputStatusMessage("Id [" + id + "] less than 60 is error."));
            }
            else if(id < 90)
            {
               record.addWarning(new QWarningMessage("Id [" + id + "] less than 90 is warning."));
            }
         }

         return (records);
      }

   }

}