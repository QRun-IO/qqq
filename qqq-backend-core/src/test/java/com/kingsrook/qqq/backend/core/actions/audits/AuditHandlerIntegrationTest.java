/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.audits;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.audits.DMLAuditHandlerInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.ProcessedAuditHandlerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditHandlerType;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditHandlerMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Integration tests for audit handler system.
 ** Tests the full flow from DML operations through to handler invocation.
 *******************************************************************************/
class AuditHandlerIntegrationTest extends BaseTest
{
   private static List<DMLAuditHandlerInput>       capturedDMLInputs       = new ArrayList<>();
   private static List<ProcessedAuditHandlerInput> capturedProcessedInputs = new ArrayList<>();
   private static CountDownLatch                   asyncLatch              = null;



   /*******************************************************************************
    ** Reset captured data before each test
    *******************************************************************************/
   @BeforeEach
   void setUp() throws QException
   {
      capturedDMLInputs.clear();
      capturedProcessedInputs.clear();
      asyncLatch = null;

      QInstance qInstance = QContext.getQInstance();
      new AuditsMetaDataProvider().defineAll(qInstance, TestUtils.MEMORY_BACKEND_NAME, null);

      qInstance.getTable(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .setAuditRules(new QAuditRules().withAuditLevel(AuditLevel.FIELD));
   }



   /*******************************************************************************
    ** Clean up memory store after each test
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      MemoryRecordStore.getInstance().reset();
   }



   /*******************************************************************************
    ** Test that DML handler is called when performing an INSERT operation.
    *******************************************************************************/
   @Test
   void testDMLHandlerCalledOnInsert() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("testDMLHandler")
         .withHandlerCode(new QCodeReference(CapturingDMLAuditHandler.class))
         .withHandlerType(AuditHandlerType.DML)
         .withIsAsync(false)
         .withEnabled(true));

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "John").withValue("lastName", "Doe")
      ));

      InsertOutput insertOutput = new InsertAction().execute(insertInput);

      assertEquals(1, capturedDMLInputs.size());
      DMLAuditHandlerInput captured = capturedDMLInputs.get(0);
      assertEquals(TestUtils.TABLE_NAME_PERSON_MEMORY, captured.getTableName());
      assertEquals(DMLAuditHandlerInput.DMLType.INSERT, captured.getDmlType());
      assertNotNull(captured.getNewRecords());
      assertEquals(1, captured.getNewRecords().size());
      assertEquals("John", captured.getNewRecords().get(0).getValueString("firstName"));
      assertNotNull(captured.getTimestamp());
      assertNotNull(captured.getSession());
   }



   /*******************************************************************************
    ** Test that DML handler is called when performing an UPDATE operation.
    *******************************************************************************/
   @Test
   void testDMLHandlerCalledOnUpdate() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("testDMLHandler")
         .withHandlerCode(new QCodeReference(CapturingDMLAuditHandler.class))
         .withHandlerType(AuditHandlerType.DML)
         .withIsAsync(false)
         .withEnabled(true));

      /////////////////////////
      // first insert a record
      /////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "John").withValue("lastName", "Doe")
      ));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      Integer insertedId = insertOutput.getRecords().get(0).getValueInteger("id");

      capturedDMLInputs.clear();

      /////////////////////////
      // now update the record
      /////////////////////////
      UpdateInput updateInput = new UpdateInput();
      updateInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      updateInput.setRecords(List.of(
         new QRecord().withValue("id", insertedId).withValue("firstName", "Jane")
      ));

      new UpdateAction().execute(updateInput);

      assertEquals(1, capturedDMLInputs.size());
      DMLAuditHandlerInput captured = capturedDMLInputs.get(0);
      assertEquals(TestUtils.TABLE_NAME_PERSON_MEMORY, captured.getTableName());
      assertEquals(DMLAuditHandlerInput.DMLType.UPDATE, captured.getDmlType());
      assertNotNull(captured.getNewRecords());
      assertEquals(1, captured.getNewRecords().size());
      assertEquals("Jane", captured.getNewRecords().get(0).getValueString("firstName"));

      /////////////////////////////////////////////////////////////////////////
      // old records should be populated for updates (fetched by UpdateAction)
      /////////////////////////////////////////////////////////////////////////
      assertNotNull(captured.getOldRecords());
      assertEquals(1, captured.getOldRecords().size());
   }



   /*******************************************************************************
    ** Test that DML handler is called when performing a DELETE operation.
    *******************************************************************************/
   @Test
   void testDMLHandlerCalledOnDelete() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("testDMLHandler")
         .withHandlerCode(new QCodeReference(CapturingDMLAuditHandler.class))
         .withHandlerType(AuditHandlerType.DML)
         .withIsAsync(false)
         .withEnabled(true));

      /////////////////////////
      // first insert a record
      /////////////////////////
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "John").withValue("lastName", "Doe")
      ));
      InsertOutput insertOutput = new InsertAction().execute(insertInput);
      Integer insertedId = insertOutput.getRecords().get(0).getValueInteger("id");

      capturedDMLInputs.clear();

      /////////////////////////
      // now delete the record
      /////////////////////////
      DeleteInput deleteInput = new DeleteInput();
      deleteInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      deleteInput.setPrimaryKeys(List.of(insertedId));

      new DeleteAction().execute(deleteInput);

      assertEquals(1, capturedDMLInputs.size());
      DMLAuditHandlerInput captured = capturedDMLInputs.get(0);
      assertEquals(TestUtils.TABLE_NAME_PERSON_MEMORY, captured.getTableName());
      assertEquals(DMLAuditHandlerInput.DMLType.DELETE, captured.getDmlType());
      assertNotNull(captured.getNewRecords());
      assertEquals(1, captured.getNewRecords().size());
   }



   /*******************************************************************************
    ** Test that processed handler is called after audit records are inserted.
    *******************************************************************************/
   @Test
   void testProcessedHandlerCalledAfterAuditInsert() throws QException
   {
      QInstance qInstance = QContext.getQInstance();
      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("testProcessedHandler")
         .withHandlerCode(new QCodeReference(CapturingProcessedAuditHandler.class))
         .withHandlerType(AuditHandlerType.PROCESSED)
         .withIsAsync(false)
         .withEnabled(true));

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "John").withValue("lastName", "Doe")
      ));

      new InsertAction().execute(insertInput);

      assertEquals(1, capturedProcessedInputs.size());
      ProcessedAuditHandlerInput captured = capturedProcessedInputs.get(0);
      assertNotNull(captured.getAuditSingleInputs());
      assertThat(captured.getAuditSingleInputs()).isNotEmpty();
      assertNotNull(captured.getTimestamp());
      assertNotNull(captured.getSession());
   }



   /*******************************************************************************
    ** Test that async DML handler is called asynchronously.
    *******************************************************************************/
   @Test
   void testAsyncDMLHandler() throws Exception
   {
      asyncLatch = new CountDownLatch(1);

      QInstance qInstance = QContext.getQInstance();
      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("testAsyncDMLHandler")
         .withHandlerCode(new QCodeReference(CapturingDMLAuditHandler.class))
         .withHandlerType(AuditHandlerType.DML)
         .withIsAsync(true)
         .withEnabled(true));

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "Async").withValue("lastName", "Test")
      ));

      new InsertAction().execute(insertInput);

      boolean completed = asyncLatch.await(5, TimeUnit.SECONDS);
      assertTrue(completed, "Async handler should complete within timeout");
      assertEquals(1, capturedDMLInputs.size());
      assertEquals("Async", capturedDMLInputs.get(0).getNewRecords().get(0).getValueString("firstName"));
   }



   /*******************************************************************************
    ** Test that multiple handlers are all called.
    *******************************************************************************/
   @Test
   void testMultipleHandlersCalled() throws QException
   {
      QInstance qInstance = QContext.getQInstance();

      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("dmlHandler")
         .withHandlerCode(new QCodeReference(CapturingDMLAuditHandler.class))
         .withHandlerType(AuditHandlerType.DML)
         .withIsAsync(false)
         .withEnabled(true));

      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("processedHandler")
         .withHandlerCode(new QCodeReference(CapturingProcessedAuditHandler.class))
         .withHandlerType(AuditHandlerType.PROCESSED)
         .withIsAsync(false)
         .withEnabled(true));

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "Multi").withValue("lastName", "Handler")
      ));

      new InsertAction().execute(insertInput);

      assertEquals(1, capturedDMLInputs.size());
      assertEquals(1, capturedProcessedInputs.size());
   }



   /*******************************************************************************
    ** Capturing DML audit handler for testing.
    *******************************************************************************/
   public static class CapturingDMLAuditHandler implements DMLAuditHandlerInterface
   {
      @Override
      public String getName()
      {
         return "capturingDMLHandler";
      }


      @Override
      public void handleDMLAudit(DMLAuditHandlerInput input) throws QException
      {
         capturedDMLInputs.add(input);

         if(asyncLatch != null)
         {
            asyncLatch.countDown();
         }
      }
   }



   /*******************************************************************************
    ** Capturing processed audit handler for testing.
    *******************************************************************************/
   public static class CapturingProcessedAuditHandler implements ProcessedAuditHandlerInterface
   {
      @Override
      public String getName()
      {
         return "capturingProcessedHandler";
      }


      @Override
      public void handleAudit(ProcessedAuditHandlerInput input) throws QException
      {
         capturedProcessedInputs.add(input);
      }
   }

}
