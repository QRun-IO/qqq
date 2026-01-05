/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.editwithfile;


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
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
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
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendFieldMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.BulkInsertFullProcessTest;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfileField;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
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
class BulkEditWithFileFullProcessTest extends BaseTest
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
   public static String getPersonCsvRow1(boolean includeId)
   {
      String idPrefix = includeId ? """
         "1",""" : "";

      return (idPrefix + """
         "2021-10-26 14:39:37","2021-10-26 14:39:37","Jehn","Doe","1980-01-01","john@doe.com","Missouri",24
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getPersonCsvRow1ForUniqueKeyCase(boolean includeId)
   {
      String idPrefix = includeId ? """
         "1",""" : "";

      return (idPrefix + """
         "2021-10-26 14:39:37","2021-10-26 14:39:37","John","Doe","1980-01-01","johnny@doe.com","Missouri",24
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getPersonCsvRow2(boolean includeId)
   {
      String idPrefix = includeId ? """
         "2",""" : "";

      return (idPrefix + """
         "2021-10-26 14:39:37","2021-10-26 14:39:37","Jyne","Doe","1981-01-01","john@doe.com","Illinois",
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getPersonCsvRow2ForUniqueKeyCase(boolean includeId)
   {
      String idPrefix = includeId ? """
         "2",""" : "";

      return (idPrefix + """
         "2021-10-26 14:39:37","2021-10-26 14:39:37","Jane","Doe","1981-01-01","janey@doe.com","Illinois",
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static String getPersonCsvHeaderUsingLabels(boolean includeId)
   {
      String idPrefix = includeId ? """
         "Id",""" : "";

      return (idPrefix + """
         "Create Date","Modify Date","First Name","Last Name","Birth Date","Email","Home State",noOfShoes
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test() throws Exception
   {
      /////////////////////////////////////////////
      // use the bulk insert test to insert data //
      /////////////////////////////////////////////
      new BulkInsertFullProcessTest().test();
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY)).isNotEmpty();

      /////////////////////////////////////////////////////////
      // start the process - expect to go to the upload step //
      /////////////////////////////////////////////////////////
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.addValue("keyFields", "id");
      runProcessInput.addValue("isBulkEdit", "true");
      RunProcessOutput runProcessOutput = startProcess(runProcessInput);
      String           processUUID      = runProcessOutput.getProcessUUID();
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("upload");

      //////////////////////////
      // continue post-upload //
      //////////////////////////
      runProcessOutput = continueProcessPostUpload(runProcessInput, processUUID, simulateFileUpload(2, true));
      assertEquals(List.of("Id", "Create Date", "Modify Date", "First Name", "Last Name", "Birth Date", "Email", "Home State", "noOfShoes"), runProcessOutput.getValue("headerValues"));
      assertEquals(List.of("A", "B", "C", "D", "E", "F", "G", "H", "I"), runProcessOutput.getValue("headerLetters"));

      //////////////////////////////////////////////////////
      // assert about the suggested mapping that was done //
      //////////////////////////////////////////////////////
      Serializable bulkLoadProfile = runProcessOutput.getValue("bulkLoadProfile");
      assertThat(bulkLoadProfile).isInstanceOf(BulkLoadProfile.class);
      assertThat(((BulkLoadProfile) bulkLoadProfile).getFieldList()).hasSizeGreaterThan(5);
      assertEquals("id", ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(0).getFieldName());
      assertEquals(0, ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(0).getColumnIndex());
      assertEquals("firstName", ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(1).getFieldName());
      assertEquals(3, ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(1).getColumnIndex());
      assertEquals("lastName", ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(2).getFieldName());
      assertEquals(4, ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(2).getColumnIndex());
      assertEquals("birthDate", ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(3).getFieldName());
      assertEquals(5, ((BulkLoadProfile) bulkLoadProfile).getFieldList().get(3).getColumnIndex());

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
         .hasLineWithMessageContaining("Person Memory records were edited")
         .hasStatus(Status.OK)
         .hasCount(2)
         .getLine();
      assertEquals(List.of(1, 2), ((ProcessSummaryLine) okLine).getPrimaryKeys());

      ////////////////////////////////////
      // query for the inserted records //
      ////////////////////////////////////
      List<QRecord> records = TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      assertEquals("Jehn", records.get(0).getValueString("firstName"));
      assertEquals("Jyne", records.get(1).getValueString("firstName"));

      assertNotNull(records.get(0).getValue("id"));
      assertNotNull(records.get(1).getValue("id"));
      assertEquals(1, records.get(0).getValue("id"));
      assertEquals(2, records.get(1).getValue("id"));

      assertEquals(2, records.get(0).getValueInteger("homeStateId"));
      assertEquals(1, records.get(1).getValueInteger("homeStateId"));

      assertEquals(defaultEmail, records.get(0).getValueString("email"));
      assertEquals(defaultEmail, records.get(1).getValueString("email"));

      assertEquals(24, records.get(0).getValueInteger("noOfShoes"));
      assertNull(records.get(1).getValue("noOfShoes"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUsingUniqueKey() throws Exception
   {
      /////////////////////////////////////////////
      // use the bulk insert test to insert data //
      /////////////////////////////////////////////
      new BulkInsertFullProcessTest().test();
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY)).isNotEmpty();

      RunProcessOutput runProcessOutput = runProcessUsingUniqueKey(null);

      /////////////////////////////////////
      // verify the ids that were edited //
      /////////////////////////////////////
      ProcessSummaryLineInterface okLine = ProcessSummaryAssert.assertThat(runProcessOutput)
         .hasLineWithStatus(Status.OK)
         .getLine();
      assertEquals(List.of(1, 2), ((ProcessSummaryLine) okLine).getPrimaryKeys());

      ////////////////////////////////////
      // query for the inserted records //
      ////////////////////////////////////
      List<QRecord> records = TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      assertEquals("John", records.get(0).getValueString("firstName"));
      assertEquals("Jane", records.get(1).getValueString("firstName"));

      assertEquals(1, records.get(0).getValue("id"));
      assertEquals(2, records.get(1).getValue("id"));

      assertEquals("johnny@doe.com", records.get(0).getValueString("email"));
      assertEquals("janey@doe.com", records.get(1).getValueString("email"));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private RunProcessOutput runProcessUsingUniqueKey(Map<String, Serializable> processInputValues) throws Exception
   {
      /////////////////////////////////////////////////////////
      // start the process - expect to go to the upload step //
      /////////////////////////////////////////////////////////
      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.addValue("keyFields", "firstName|lastName");
      runProcessInput.addValue("isBulkEdit", "true");

      for(Map.Entry<String, Serializable> entry : CollectionUtils.nonNullMap(processInputValues).entrySet())
      {
         runProcessInput.addValue(entry.getKey(), entry.getValue());
      }

      RunProcessOutput runProcessOutput = startProcess(runProcessInput);
      String           processUUID      = runProcessOutput.getProcessUUID();
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("upload");

      //////////////////////////
      // continue post-upload //
      //////////////////////////
      runProcessOutput = continueProcessPostUpload(runProcessInput, processUUID, simulateFileUploadForUniqueKeyCase(2, false));
      assertEquals(List.of("Create Date", "Modify Date", "First Name", "Last Name", "Birth Date", "Email", "Home State", "noOfShoes"), runProcessOutput.getValue("headerValues"));
      assertEquals(List.of("A", "B", "C", "D", "E", "F", "G", "H"), runProcessOutput.getValue("headerLetters"));

      //////////////////////////////////////////////////////
      // assert about the suggested mapping that was done //
      //////////////////////////////////////////////////////
      Serializable bulkLoadProfileSerializable = runProcessOutput.getValue("bulkLoadProfile");
      assertThat(bulkLoadProfileSerializable).isInstanceOf(BulkLoadProfile.class);
      BulkLoadProfile bulkLoadProfile = (BulkLoadProfile) bulkLoadProfileSerializable;
      assertThat(bulkLoadProfile.getFieldList()).hasSizeGreaterThan(4);
      assertEquals("firstName", bulkLoadProfile.getFieldList().get(0).getFieldName());
      assertEquals(2, bulkLoadProfile.getFieldList().get(0).getColumnIndex());
      assertEquals("lastName", bulkLoadProfile.getFieldList().get(1).getFieldName());
      assertEquals(3, bulkLoadProfile.getFieldList().get(1).getColumnIndex());
      assertEquals("email", bulkLoadProfile.getFieldList().get(3).getFieldName());
      assertEquals(5, bulkLoadProfile.getFieldList().get(3).getColumnIndex());

      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("fileMapping");

      ////////////////////////////////
      // continue post file-mapping //
      ////////////////////////////////
      runProcessOutput = continueProcessPostFileMapping(runProcessInput);
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("review");

      /////////////////////////////////
      // continue post review screen //
      /////////////////////////////////
      runProcessOutput = continueProcessPostReviewScreen(runProcessInput);
      assertThat(runProcessOutput.getRecords()).hasSize(2);
      assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo("result");
      assertThat(runProcessOutput.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY)).isNotNull().isInstanceOf(List.class);
      assertThat(runProcessOutput.getException()).isEmpty();

      ProcessSummaryAssert.assertThat(runProcessOutput)
         .hasLineWithMessageContaining("Person Memory records were edited")
         .hasStatus(Status.OK)
         .hasCount(2);

      return (runProcessOutput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testUniqueKeyDoesntLoadMillionsOfRecords() throws Exception
   {
      //////////////////////////////////////////////////////////////////
      // insert lots of records that won't match, and a few that will //
      //////////////////////////////////////////////////////////////////
      List<QRecord> recordsToInsert = new ArrayList<>();
      for(int i = 0; i < 100_000; i++)
      {
         recordsToInsert.add(new QRecord().withValue("firstName", "Homer" + i).withValue("lastName", "Doe"));
      }
      recordsToInsert.add(new QRecord().withValue("firstName", "John").withValue("lastName", "Doe"));
      recordsToInsert.add(new QRecord().withValue("firstName", "Jane").withValue("lastName", "Doe"));
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withRecords(recordsToInsert));

      RunProcessOutput runProcessOutput = runProcessUsingUniqueKey(null);

      //////////////////////////////////////
      // assert about the updated records //
      //////////////////////////////////////
      List<QRecord> records = QueryAction.execute(TestUtils.TABLE_NAME_PERSON_MEMORY, new QQueryFilter(new QFilterCriteria("firstName", QCriteriaOperator.IN, "John", "Jane")).withOrderBy(new QFilterOrderBy("id")));

      assertEquals("John", records.get(0).getValueString("firstName"));
      assertEquals("Jane", records.get(1).getValueString("firstName"));

      assertEquals("johnny@doe.com", records.get(0).getValueString("email"));
      assertEquals("janey@doe.com", records.get(1).getValueString("email"));

      /////////////////////////////////////////////////////////////////
      // make sure we didn't select (way) too many potential records //
      /////////////////////////////////////////////////////////////////
      assertThat(runProcessOutput.getValueInteger("potentialRecordsLoaded")).isLessThanOrEqualTo(100);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testHittingLoadPotentialRecordsByUniqueKeyRecordByRecord() throws Exception
   {
      /////////////////////////////////////////////
      // use the bulk insert test to insert data //
      /////////////////////////////////////////////
      new BulkInsertFullProcessTest().test();
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY)).isNotEmpty();

      ////////////////////////////////////////////////////////////////////////////////////////////////
      // run the process, with 2 values set that should force the method we're interested in to run //
      ////////////////////////////////////////////////////////////////////////////////////////////////
      RunProcessOutput runProcessOutput = runProcessUsingUniqueKey(Map.of("ByPerFieldInListsQueryFailed", true, "ByOrQueriesFailed", true));

      /////////////////////////////////////
      // verify the ids that were edited //
      /////////////////////////////////////
      ProcessSummaryLineInterface okLine = ProcessSummaryAssert.assertThat(runProcessOutput)
         .hasLineWithStatus(Status.OK)
         .getLine();
      assertEquals(List.of(1, 2), ((ProcessSummaryLine) okLine).getPrimaryKeys());

      ////////////////////////////////////
      // query for the inserted records //
      ////////////////////////////////////
      List<QRecord> records = TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY);

      assertEquals("John", records.get(0).getValueString("firstName"));
      assertEquals("Jane", records.get(1).getValueString("firstName"));

      assertEquals(1, records.get(0).getValue("id"));
      assertEquals(2, records.get(1).getValue("id"));

      assertEquals("johnny@doe.com", records.get(0).getValueString("email"));
      assertEquals("janey@doe.com", records.get(1).getValueString("email"));
   }





   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSummaryLinePrimaryKeys() throws Exception
   {
      /////////////////////////////////////////////
      // use the bulk insert test to insert data //
      /////////////////////////////////////////////
      new BulkInsertFullProcessTest().testSummaryLinePrimaryKeys();
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY)).isNotEmpty();

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(PersonWarnOrErrorCustomizer.class));

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
         .hasLineWithMessageContaining("Person Memory records were edited")
         .hasStatus(Status.OK)
         .hasCount(4)
         .getLine();
      assertEquals(List.of(1, 2, 3, 4), ((ProcessSummaryLine) okLine).getPrimaryKeys());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSummaryLineErrors() throws Exception
   {
      /////////////////////////////////////////////
      // use the bulk insert test to insert data //
      /////////////////////////////////////////////
      new BulkInsertFullProcessTest().testSummaryLineErrors();
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY)).isNotEmpty();

      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(PersonWarnOrErrorCustomizer.class));

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

      ProcessSummaryAssert.assertThat(runProcessOutput).hasLineWithMessageContaining("Person Memory record was edited.").hasStatus(Status.OK).hasCount(1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testOneRow() throws Exception
   {
      /////////////////////////////////////////////
      // use the bulk insert test to insert data //
      /////////////////////////////////////////////
      new BulkInsertFullProcessTest().testSummaryLineErrors();
      assertThat(TestUtils.queryTable(TestUtils.TABLE_NAME_PERSON_MEMORY)).isNotEmpty();

      RunProcessInput  runProcessInput  = new RunProcessInput();
      RunProcessOutput runProcessOutput = startProcess(runProcessInput);
      String           processUUID      = runProcessOutput.getProcessUUID();

      continueProcessPostUpload(runProcessInput, processUUID, simulateFileUpload(1, true));
      continueProcessPostFileMapping(runProcessInput);
      continueProcessPostValueMapping(runProcessInput);
      runProcessOutput = continueProcessPostReviewScreen(runProcessInput);

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // all that just so we can make sure this message is right (because it was wrong when we first wrote it, lol) //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      ProcessSummaryAssert.assertThat(runProcessOutput).hasLineWithMessageContaining("Person Memory record was edited.").hasStatus(Status.OK).hasCount(1);
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
   private static StorageInput simulateFileUpload(int noOfRows, boolean includeId) throws Exception
   {
      String       storageReference = UUID.randomUUID() + ".csv";
      StorageInput storageInput     = new StorageInput(TestUtils.TABLE_NAME_MEMORY_STORAGE).withReference(storageReference);
      try(OutputStream outputStream = new StorageAction().createOutputStream(storageInput))
      {
         String fileContents = getPersonCsvHeaderUsingLabels(includeId) + getPersonCsvRow1(includeId) + (noOfRows == 2 ? getPersonCsvRow2(includeId) : "");
         System.out.println(fileContents);
         outputStream.write(fileContents.getBytes());
      }
      return storageInput;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static StorageInput simulateFileUploadForUniqueKeyCase(int noOfRows, boolean includeId) throws Exception
   {
      String       storageReference = UUID.randomUUID() + ".csv";
      StorageInput storageInput     = new StorageInput(TestUtils.TABLE_NAME_MEMORY_STORAGE).withReference(storageReference);
      try(OutputStream outputStream = new StorageAction().createOutputStream(storageInput))
      {
         String fileContents = getPersonCsvHeaderUsingLabels(includeId) + getPersonCsvRow1ForUniqueKeyCase(includeId) + (noOfRows == 2 ? getPersonCsvRow2ForUniqueKeyCase(includeId) : "");
         System.out.println(fileContents);
         outputStream.write(fileContents.getBytes());
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
         outputStream.write((getPersonCsvHeaderUsingLabels(true) + """
            "1","2021-10-26 14:39:37","2021-10-26 14:39:37","John","Doe","1980-01-01","john@doe.com","Missouri",42
            "2","2021-10-26 14:39:37","2021-10-26 14:39:37","Tornado warning","Doe","1980-01-01","john@doe.com","Missouri",42
            "3","2021-10-26 14:39:37","2021-10-26 14:39:37","Tornado warning","Doey","1980-01-01","john@doe.com","Missouri",42
            "4","2021-10-26 14:39:37","2021-10-26 14:39:37","Hurricane warning","Doe","1980-01-01","john@doe.com","Missouri",42
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
         outputStream.write((getPersonCsvHeaderUsingLabels(true) + """
            "1","2021-10-26 14:39:37","2021-10-26 14:39:37","John","Doe","1980-01-01","john@doe.com","Missouri",42
            "2","2021-10-26 14:39:37","2021-10-26 14:39:37","not-pre-Error plane","Doe","1980-01-01","john@doe.com","Missouri",42
            "3","2021-10-26 14:39:37","2021-10-26 14:39:37","Error purifier","Doe","1980-01-01","john@doe.com","Missouri",42
            """).getBytes());
      }
      return storageInput;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static RunProcessOutput startProcess(RunProcessInput runProcessInput) throws QException
   {
      runProcessInput.setProcessName(TestUtils.TABLE_NAME_PERSON_MEMORY + ".bulkEditWithFile");
      runProcessInput.addValue("tableName", TestUtils.TABLE_NAME_PERSON_MEMORY);
      runProcessInput.addValue("isBulkEdit", "true");
      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);
      return runProcessOutput;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static void addProfileToRunProcessInput(RunProcessInput input)
   {
      if("firstName|lastName".equals(input.getValue("keyFields")))
      {
         addProfileForUniqueKeyCaseToRunProcessInput(input);
         return;
      }

      input.addValue("version", "v1");
      input.addValue("layout", "FLAT");
      input.addValue("isBulkEdit", "true");
      input.addValue("keyFields", "id");
      input.addValue("hasHeaderRow", "true");
      input.addValue("fieldListJSON", JsonUtils.toJson(List.of(
         new BulkLoadProfileField().withFieldName("id").withColumnIndex(0),
         new BulkLoadProfileField().withFieldName("firstName").withColumnIndex(3),
         new BulkLoadProfileField().withFieldName("lastName").withColumnIndex(4),
         new BulkLoadProfileField().withFieldName("email").withDefaultValue(defaultEmail),
         new BulkLoadProfileField().withFieldName("homeStateId").withColumnIndex(7).withDoValueMapping(true).withValueMappings(Map.of("Illinois", 1)),
         new BulkLoadProfileField().withFieldName("noOfShoes").withColumnIndex(8)
      )));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static void addProfileForUniqueKeyCaseToRunProcessInput(RunProcessInput input)
   {
      input.addValue("version", "v1");
      input.addValue("layout", "FLAT");
      input.addValue("isBulkEdit", "true");
      input.addValue("keyFields", "firstName|lastName");
      input.addValue("hasHeaderRow", "true");
      input.addValue("fieldListJSON", JsonUtils.toJson(List.of(
         new BulkLoadProfileField().withFieldName("firstName").withColumnIndex(2),
         new BulkLoadProfileField().withFieldName("lastName").withColumnIndex(3),
         new BulkLoadProfileField().withFieldName("email").withColumnIndex(5)
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
