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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert;


import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreInsertCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryAssert;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendFieldMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfileField;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for full bulk insert process
 *******************************************************************************/
public class BulkInsertFullProcessTest extends BaseTest
{
   private static final String defaultEmail = "noone@kingsrook.com";



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
   public static String getPersonCsvRow1()
   {
      return ("""
         "0","2021-10-26 14:39:37","2021-10-26 14:39:37","John","Doe","1980-01-01","john@doe.com","Missouri",42
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getPersonCsvRow2()
   {
      return ("""
         "0","2021-10-26 14:39:37","2021-10-26 14:39:37","Jane","Doe","1981-01-01","john@doe.com","Illinois",
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getPersonCsvHeaderUsingLabels()
   {
      return ("""
         "Id","Create Date","Modify Date","First Name","Last Name","Birth Date","Email","Home State",noOfShoes
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test() throws Exception
   {
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY)).isEmpty();

      /////////////////////////////////////////////////////////
      // start the process - expect to go to the upload step //
      /////////////////////////////////////////////////////////
      RunProcessInput  runProcessInput  = new RunProcessInput();
      RunProcessOutput runProcessOutput = startProcess(runProcessInput);
      String           processUUID      = runProcessOutput.getProcessUUID();
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("upload");

      //////////////////////////
      // continue post-upload //
      //////////////////////////
      runProcessOutput = continueProcessPostUpload(runProcessInput, processUUID, simulateFileUpload(2));
      assertEquals(List.of("Id", "Create Date", "Modify Date", "First Name", "Last Name", "Birth Date", "Email", "Home State", "noOfShoes"), runProcessOutput.getValue("headerValues"));
      assertEquals(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I"), runProcessOutput.getValue("headerLetters"));

      //////////////////////////////////////////////////////
      // assert about the suggested mapping that was done //
      //////////////////////////////////////////////////////
      Serializable bulkLoadProfile = runProcessOutput.getValue("bulkLoadProfile");
      assertThat(bulkLoadProfile).isInstanceOf(BulkLoadProfile.class);
      assertThat(((BulkLoadProfile) bulkLoadProfile).getFieldList()).hasSizeGreaterThan(5);
      assertEquals("firstName", ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(0).getFieldName());
      assertEquals(3, ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(0).getColumnIndex());
      assertEquals("lastName", ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(1).getFieldName());
      assertEquals(4, ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(1).getColumnIndex());
      assertEquals("birthDate", ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(2).getFieldName());
      assertEquals(5, ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(2).getColumnIndex());

      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("fileMapping");

      ////////////////////////////////
      // continue post file-mapping //
      ////////////////////////////////
      runProcessOutput = continueProcessPostFileMapping(runProcessInput);
      Serializable valueMappingField = runProcessOutput.getValue("valueMappingField");
      assertThat(valueMappingField).isInstanceOf(QFrontendFieldMetaData.class);
      assertEquals("homeStateId", ((QFrontendFieldMetaData) valueMappingField).getName());
      assertEquals(List.of("Missouri", "Illinois"), runProcessOutput.getValue("fileValues"));
      assertEquals(List.of("homeStateId"), runProcessOutput.getValue("fieldNamesToDoValueMapping"));
      assertEquals(Map.of(1, "IL"), runProcessOutput.getValue("mappedValueLabels"));
      assertEquals(0, runProcessOutput.getValue("valueMappingFieldIndex"));
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("valueMapping");

      /////////////////////////////////
      // continue post value-mapping //
      /////////////////////////////////
      runProcessOutput = continueProcessPostValueMapping(runProcessInput);
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("review");

      /////////////////////////////////
      // continue post review screen //
      /////////////////////////////////
      runProcessOutput = continueProcessPostReviewScreen(runProcessInput);
      assertThat(runProcessOutput.getRecords()).hasSize(2);
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("result");
      assertThat(runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY)).isNotNull().isInstanceOf(List.class);
      assertThat(runProcessOutput.getException()).isEmpty();

      ProcessSummaryLineInterface okLine = ProcessSummaryAssert.assertThat(runProcessOutput)
         .hasLineWithMessageContaining("Person Memory records were inserted")
         .hasStatus(Status.OK)
         .hasCount(2)
         .getLine();
      assertEquals(List.of(1, 2), ((ProcessSummaryLine) okLine).getPrimaryKeys());

      ProcessSummaryAssert.assertThat(runProcessOutput).hasLineWithMessageContaining("records were processed from the file").hasStatus(Status.INFO);
      ProcessSummaryAssert.assertThat(runProcessOutput).hasLineWithMessageContaining("Inserted Id values between 1 and 2").hasStatus(Status.INFO);

      ////////////////////////////////////
      // query for the inserted records //
      ////////////////////////////////////
      List<QRecord> records = TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      assertEquals("John", records.get(0).getValueString("firstName"));
      assertEquals("Jane", records.get(1).getValueString("firstName"));

      assertNotNull(records.get(0).getValue("id"));
      assertNotNull(records.get(1).getValue("id"));

      assertEquals(2, records.get(0).getValueInteger("homeStateId"));
      assertEquals(1, records.get(1).getValueInteger("homeStateId"));

      assertEquals(defaultEmail, records.get(0).getValueString("email"));
      assertEquals(defaultEmail, records.get(1).getValueString("email"));

      assertEquals(42, records.get(0).getValueInteger("noOfShoes"));
      assertNull(records.get(1).getValue("noOfShoes"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testSummaryLinePrimaryKeys() throws Exception
   {
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY)).isEmpty();
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(PersonWarnOrErrorCustomizer.class));

      /////////////////////////////////////////////////////////
      // start the process - expect to go to the upload step //
      /////////////////////////////////////////////////////////
      RunProcessInput  runProcessInput  = new RunProcessInput();
      RunProcessOutput runProcessOutput = startProcess(runProcessInput);
      String           processUUID      = runProcessOutput.getProcessUUID();

      continueProcessPostUpload(runProcessInput, processUUID, simulateFileUploadForWarningCase());
      continueProcessPostFileMapping(runProcessInput);
      continueProcessPostValueMapping(runProcessInput);
      runProcessOutput = continueProcessPostReviewScreen(runProcessInput);

      ProcessSummaryLineInterface okLine = ProcessSummaryAssert.assertThat(runProcessOutput)
         .hasLineWithMessageContaining("Person Memory record was inserted")
         .hasStatus(Status.OK)
         .hasCount(1)
         .getLine();
      assertEquals(List.of(1), ((ProcessSummaryLine) okLine).getPrimaryKeys());

      ProcessSummaryLineInterface warnTornadoLine = ProcessSummaryAssert.assertThat(runProcessOutput)
         .hasLineWithMessageContaining("records were inserted, but had a warning: Tornado warning")
         .hasStatus(Status.WARNING)
         .hasCount(2)
         .getLine();
      assertEquals(List.of(2, 3), ((ProcessSummaryLine) warnTornadoLine).getPrimaryKeys());

      ProcessSummaryAssert.assertThat(runProcessOutput).hasLineWithMessageContaining("record was inserted, but had a warning: Hurricane warning").hasStatus(Status.WARNING).hasCount(1);
      ProcessSummaryAssert.assertThat(runProcessOutput).hasLineWithMessageContaining("records were processed from the file").hasStatus(Status.INFO).hasCount(4);
      ProcessSummaryAssert.assertThat(runProcessOutput).hasLineWithMessageContaining("Inserted Id values between 1 and 4").hasStatus(Status.INFO);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testSummaryLineErrors() throws Exception
   {
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY)).isEmpty();
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(PersonWarnOrErrorCustomizer.class));

      /////////////////////////////////////////////////////////
      // start the process - expect to go to the upload step //
      /////////////////////////////////////////////////////////
      RunProcessInput  runProcessInput  = new RunProcessInput();
      RunProcessOutput runProcessOutput = startProcess(runProcessInput);
      String           processUUID      = runProcessOutput.getProcessUUID();

      continueProcessPostUpload(runProcessInput, processUUID, simulateFileUploadForErrorCase());
      continueProcessPostFileMapping(runProcessInput);
      continueProcessPostValueMapping(runProcessInput);
      runProcessOutput = continueProcessPostReviewScreen(runProcessInput);

      ProcessSummaryAssert.assertThat(runProcessOutput).hasLineWithMessageContaining("Person Memory record was inserted.").hasStatus(Status.OK).hasCount(1);

      ProcessSummaryAssert.assertThat(runProcessOutput)
         .hasLineWithMessageContaining("plane")
         .hasStatus(Status.ERROR)
         .hasCount(1);

      ProcessSummaryAssert.assertThat(runProcessOutput)
         .hasLineWithMessageContaining("purifier")
         .hasStatus(Status.ERROR)
         .hasCount(1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testOneRow() throws Exception
   {
      ///////////////////////////////////////
      // make sure table is empty to start //
      ///////////////////////////////////////
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY)).isEmpty();

      RunProcessInput  runProcessInput  = new RunProcessInput();
      RunProcessOutput runProcessOutput = startProcess(runProcessInput);
      String           processUUID      = runProcessOutput.getProcessUUID();

      continueProcessPostUpload(runProcessInput, processUUID, simulateFileUpload(1));
      continueProcessPostFileMapping(runProcessInput);
      continueProcessPostValueMapping(runProcessInput);
      runProcessOutput = continueProcessPostReviewScreen(runProcessInput);

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // all that just so we can make sure this message is right (because it was wrong when we first wrote it, lol) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      ProcessSummaryAssert.assertThat(runProcessOutput).hasLineWithMessageContaining("Inserted Id 1");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static RunProcessOutput continueProcessPostReviewScreen(RunProcessInput runProcessInput) throws QException
   {
      RunProcessOutput runProcessOutput;
      runProcessInput.setStartAfterStep("review");
      addProfileToRunProcessInput(runProcessInput);
      runProcessOutput = new RunProcessAction().execute(runProcessInput);
      return runProcessOutput;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static RunProcessOutput continueProcessPostValueMapping(RunProcessInput runProcessInput) throws QException
   {
      runProcessInput.setStartAfterStep("valueMapping");
      runProcessInput.addValue("mappedValuesJSON", JsonUtils.toJson(Map.of("Illinois", 1, "Missouri", 2)));
      addProfileToRunProcessInput(runProcessInput);
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
      return (runProcessOutput);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static RunProcessOutput continueProcessPostFileMapping(RunProcessInput runProcessInput) throws QException
   {
      RunProcessOutput runProcessOutput;
      runProcessInput.setStartAfterStep("fileMapping");
      addProfileToRunProcessInput(runProcessInput);
      runProcessOutput = new RunProcessAction().execute(runProcessInput);
      return runProcessOutput;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static RunProcessOutput continueProcessPostUpload(RunProcessInput runProcessInput, String processUUID, StorageInput storageInput) throws QException
   {
      runProcessInput.setProcessUUID(processUUID);
      runProcessInput.setStartAfterStep("upload");
      runProcessInput.addValue("theFile", new ArrayList<>(List.of(storageInput)));
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
      return (runProcessOutput);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static StorageInput simulateFileUpload(int noOfRows) throws Exception
   {
      String       storageReference = UUID.randomUUID() + ".csv";
      StorageInput storageInput     = new StorageInput(TestUtils.TABLE_NAME_MEMORY_STORAGE).withReference(storageReference);
      try(OutputStream outputStream = new StorageAction().createOutputStream(storageInput))
      {
         outputStream.write((getPersonCsvHeaderUsingLabels() + getPersonCsvRow1() + (noOfRows == 2 ? getPersonCsvRow2() : "")).getBytes());
      }
      return storageInput;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static StorageInput simulateFileUploadForWarningCase() throws Exception
   {
      String       storageReference = UUID.randomUUID() + ".csv";
      StorageInput storageInput     = new StorageInput(TestUtils.TABLE_NAME_MEMORY_STORAGE).withReference(storageReference);
      try(OutputStream outputStream = new StorageAction().createOutputStream(storageInput))
      {
         outputStream.write((getPersonCsvHeaderUsingLabels() + """
            "0","2021-10-26 14:39:37","2021-10-26 14:39:37","John","Doe","1980-01-01","john@doe.com","Missouri",42
            "0","2021-10-26 14:39:37","2021-10-26 14:39:37","Tornado warning","Doe","1980-01-01","john@doe.com","Missouri",42
            "0","2021-10-26 14:39:37","2021-10-26 14:39:37","Tornado warning","Doey","1980-01-01","john@doe.com","Missouri",42
            "0","2021-10-26 14:39:37","2021-10-26 14:39:37","Hurricane warning","Doe","1980-01-01","john@doe.com","Missouri",42
            """).getBytes());
      }
      return storageInput;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static StorageInput simulateFileUploadForErrorCase() throws Exception
   {
      String       storageReference = UUID.randomUUID() + ".csv";
      StorageInput storageInput     = new StorageInput(TestUtils.TABLE_NAME_MEMORY_STORAGE).withReference(storageReference);
      try(OutputStream outputStream = new StorageAction().createOutputStream(storageInput))
      {
         outputStream.write((getPersonCsvHeaderUsingLabels() + """
            "0","2021-10-26 14:39:37","2021-10-26 14:39:37","John","Doe","1980-01-01","john@doe.com","Missouri",42
            "0","2021-10-26 14:39:37","2021-10-26 14:39:37","not-pre-Error plane","Doe","1980-01-01","john@doe.com","Missouri",42
            "0","2021-10-26 14:39:37","2021-10-26 14:39:37","Error purifier","Doe","1980-01-01","john@doe.com","Missouri",42
            """).getBytes());
      }
      return storageInput;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static RunProcessOutput startProcess(RunProcessInput runProcessInput) throws QException
   {
      runProcessInput.setProcessName(TestUtils.TABLE_NAME_PERSON_MEMORY + ".bulkInsert");
      runProcessInput.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
      return runProcessOutput;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void addProfileToRunProcessInput(RunProcessInput input)
   {
      input.addValue("version", "v1");
      input.addValue("layout", "FLAT");
      input.addValue("hasHeaderRow", "true");
      input.addValue("fieldListJSON", JsonUtils.toJson(List.of(
         new BulkLoadProfileField().withFieldName("firstName").withColumnIndex(3),
         new BulkLoadProfileField().withFieldName("lastName").withColumnIndex(4),
         new BulkLoadProfileField().withFieldName("email").withDefaultValue(defaultEmail),
         new BulkLoadProfileField().withFieldName("homeStateId").withColumnIndex(7).withDoValueMapping(true).withValueMappings(Map.of("Illinois", 1)),
         new BulkLoadProfileField().withFieldName("noOfShoes").withColumnIndex(8)
      )));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public static class PersonWarnOrErrorCustomizer implements TableCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public AbstractPreInsertCustomizer.WhenToRun whenToRunPreInsert(InsertInput insertInput, boolean isPreview)
      {
         return AbstractPreInsertCustomizer.WhenToRun.BEFORE_ALL_VALIDATIONS;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
      {
         for(QRecord record : records)
         {
            if(record.getValueString("firstName").toLowerCase().contains("warn"))
            {
               record.addWarning(new QWarningMessage(record.getValueString("firstName")));
            }
            else if(record.getValueString("firstName").toLowerCase().contains("error"))
            {
               if(isPreview && record.getValueString("firstName").toLowerCase().contains("not-pre-error"))
               {
                  continue;
               }

               record.addError(new BadInputStatusMessage(record.getValueString("firstName")));
            }
         }
         return records;
      }
   }
}
