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


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditSingleInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.DMLAuditHandlerInput;
import com.kingsrook.qqq.backend.core.model.actions.audits.ProcessedAuditHandlerInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditHandlerFailurePolicy;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditHandlerType;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditHandlerMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for AuditHandlerExecutor
 *******************************************************************************/
class AuditHandlerExecutorTest extends BaseTest
{
   private static AtomicInteger   dmlHandlerCallCount       = new AtomicInteger(0);
   private static AtomicInteger   processedHandlerCallCount = new AtomicInteger(0);
   private static AtomicBoolean   shouldThrow               = new AtomicBoolean(false);
   private static CountDownLatch  asyncLatch                = null;



   /*******************************************************************************
    ** Reset test counters before each test
    *******************************************************************************/
   private void resetCounters()
   {
      dmlHandlerCallCount.set(0);
      processedHandlerCallCount.set(0);
      shouldThrow.set(false);
      asyncLatch = null;
   }



   /*******************************************************************************
    ** Test DML handler is called synchronously
    *******************************************************************************/
   @Test
   void testExecuteDMLHandlers_sync() throws QException
   {
      resetCounters();

      QInstance qInstance = QContext.getQInstance();
      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("testDMLHandler")
         .withHandlerCode(new QCodeReference(TestDMLAuditHandler.class))
         .withHandlerType(AuditHandlerType.DML)
         .withIsAsync(false)
         .withEnabled(true));

      DMLAuditHandlerInput input = new DMLAuditHandlerInput()
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withDmlType(DMLAuditHandlerInput.DMLType.INSERT)
         .withNewRecords(List.of(new QRecord().withValue("id", 1)))
         .withTimestamp(Instant.now())
         .withSession(QContext.getQSession());

      new AuditHandlerExecutor().executeDMLHandlers(TestUtils.TABLE_NAME_PERSON_MEMORY, input);

      assertEquals(1, dmlHandlerCallCount.get());
   }



   /*******************************************************************************
    ** Test DML handler is called asynchronously
    *******************************************************************************/
   @Test
   void testExecuteDMLHandlers_async() throws Exception
   {
      resetCounters();
      asyncLatch = new CountDownLatch(1);

      QInstance qInstance = QContext.getQInstance();
      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("testAsyncDMLHandler")
         .withHandlerCode(new QCodeReference(TestDMLAuditHandler.class))
         .withHandlerType(AuditHandlerType.DML)
         .withIsAsync(true)
         .withEnabled(true));

      DMLAuditHandlerInput input = new DMLAuditHandlerInput()
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withDmlType(DMLAuditHandlerInput.DMLType.UPDATE)
         .withNewRecords(List.of(new QRecord().withValue("id", 1)))
         .withOldRecords(List.of(new QRecord().withValue("id", 1)))
         .withTimestamp(Instant.now())
         .withSession(QContext.getQSession());

      new AuditHandlerExecutor().executeDMLHandlers(TestUtils.TABLE_NAME_PERSON_MEMORY, input);

      boolean completed = asyncLatch.await(5, TimeUnit.SECONDS);
      assertTrue(completed, "Async handler should complete within timeout");
      assertEquals(1, dmlHandlerCallCount.get());
   }



   /*******************************************************************************
    ** Test processed handler is called
    *******************************************************************************/
   @Test
   void testExecuteProcessedHandlers() throws QException
   {
      resetCounters();

      QInstance qInstance = QContext.getQInstance();
      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("testProcessedHandler")
         .withHandlerCode(new QCodeReference(TestProcessedAuditHandler.class))
         .withHandlerType(AuditHandlerType.PROCESSED)
         .withIsAsync(false)
         .withEnabled(true));

      List<AuditSingleInput> auditInputs = new ArrayList<>();
      auditInputs.add(new AuditSingleInput()
         .withAuditTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withRecordId(1)
         .withMessage("Test audit"));

      ProcessedAuditHandlerInput input = new ProcessedAuditHandlerInput()
         .withAuditSingleInputs(auditInputs)
         .withTimestamp(Instant.now())
         .withSession(QContext.getQSession())
         .withSourceType("test");

      new AuditHandlerExecutor().executeProcessedHandlers(TestUtils.TABLE_NAME_PERSON_MEMORY, input);

      assertEquals(1, processedHandlerCallCount.get());
   }



   /*******************************************************************************
    ** Test table-specific handler filtering
    *******************************************************************************/
   @Test
   void testTableSpecificHandlerFiltering() throws QException
   {
      resetCounters();

      QInstance qInstance = QContext.getQInstance();

      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("tableSpecificHandler")
         .withHandlerCode(new QCodeReference(TestDMLAuditHandler.class))
         .withHandlerType(AuditHandlerType.DML)
         .withTableNames(Set.of("someOtherTable"))
         .withIsAsync(false)
         .withEnabled(true));

      DMLAuditHandlerInput input = new DMLAuditHandlerInput()
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withDmlType(DMLAuditHandlerInput.DMLType.INSERT)
         .withNewRecords(List.of(new QRecord().withValue("id", 1)))
         .withTimestamp(Instant.now())
         .withSession(QContext.getQSession());

      new AuditHandlerExecutor().executeDMLHandlers(TestUtils.TABLE_NAME_PERSON_MEMORY, input);

      assertEquals(0, dmlHandlerCallCount.get(), "Handler should not be called for non-matching table");
   }



   /*******************************************************************************
    ** Test LOG_AND_CONTINUE failure policy
    *******************************************************************************/
   @Test
   void testFailurePolicy_logAndContinue() throws QException
   {
      resetCounters();
      shouldThrow.set(true);

      QInstance qInstance = QContext.getQInstance();
      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("failingHandler")
         .withHandlerCode(new QCodeReference(TestDMLAuditHandler.class))
         .withHandlerType(AuditHandlerType.DML)
         .withIsAsync(false)
         .withFailurePolicy(AuditHandlerFailurePolicy.LOG_AND_CONTINUE)
         .withEnabled(true));

      DMLAuditHandlerInput input = new DMLAuditHandlerInput()
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withDmlType(DMLAuditHandlerInput.DMLType.INSERT)
         .withNewRecords(List.of(new QRecord().withValue("id", 1)))
         .withTimestamp(Instant.now())
         .withSession(QContext.getQSession());

      new AuditHandlerExecutor().executeDMLHandlers(TestUtils.TABLE_NAME_PERSON_MEMORY, input);

      assertEquals(1, dmlHandlerCallCount.get());
   }



   /*******************************************************************************
    ** Test FAIL_OPERATION failure policy
    *******************************************************************************/
   @Test
   void testFailurePolicy_failOperation() throws QException
   {
      resetCounters();
      shouldThrow.set(true);

      QInstance qInstance = QContext.getQInstance();
      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("failingHandler")
         .withHandlerCode(new QCodeReference(TestDMLAuditHandler.class))
         .withHandlerType(AuditHandlerType.DML)
         .withIsAsync(false)
         .withFailurePolicy(AuditHandlerFailurePolicy.FAIL_OPERATION)
         .withEnabled(true));

      DMLAuditHandlerInput input = new DMLAuditHandlerInput()
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withDmlType(DMLAuditHandlerInput.DMLType.INSERT)
         .withNewRecords(List.of(new QRecord().withValue("id", 1)))
         .withTimestamp(Instant.now())
         .withSession(QContext.getQSession());

      assertThatThrownBy(() -> new AuditHandlerExecutor().executeDMLHandlers(TestUtils.TABLE_NAME_PERSON_MEMORY, input))
         .isInstanceOf(QException.class)
         .hasMessageContaining("failingHandler");

      assertEquals(1, dmlHandlerCallCount.get());
   }



   /*******************************************************************************
    ** Test disabled handler is not called
    *******************************************************************************/
   @Test
   void testDisabledHandlerNotCalled() throws QException
   {
      resetCounters();

      QInstance qInstance = QContext.getQInstance();
      qInstance.addAuditHandler(new QAuditHandlerMetaData()
         .withName("disabledHandler")
         .withHandlerCode(new QCodeReference(TestDMLAuditHandler.class))
         .withHandlerType(AuditHandlerType.DML)
         .withIsAsync(false)
         .withEnabled(false));

      DMLAuditHandlerInput input = new DMLAuditHandlerInput()
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withDmlType(DMLAuditHandlerInput.DMLType.INSERT)
         .withNewRecords(List.of(new QRecord().withValue("id", 1)))
         .withTimestamp(Instant.now())
         .withSession(QContext.getQSession());

      new AuditHandlerExecutor().executeDMLHandlers(TestUtils.TABLE_NAME_PERSON_MEMORY, input);

      assertEquals(0, dmlHandlerCallCount.get(), "Disabled handler should not be called");
   }



   /*******************************************************************************
    ** Test DML audit handler implementation for testing
    *******************************************************************************/
   public static class TestDMLAuditHandler implements DMLAuditHandlerInterface
   {
      @Override
      public String getName()
      {
         return "testDMLHandler";
      }


      @Override
      public void handleDMLAudit(DMLAuditHandlerInput input) throws QException
      {
         dmlHandlerCallCount.incrementAndGet();

         if(asyncLatch != null)
         {
            asyncLatch.countDown();
         }

         if(shouldThrow.get())
         {
            throw new QException("Test exception");
         }
      }
   }



   /*******************************************************************************
    ** Test processed audit handler implementation for testing
    *******************************************************************************/
   public static class TestProcessedAuditHandler implements ProcessedAuditHandlerInterface
   {
      @Override
      public String getName()
      {
         return "testProcessedHandler";
      }


      @Override
      public void handleAudit(ProcessedAuditHandlerInput input) throws QException
      {
         processedHandlerCallCount.incrementAndGet();

         if(shouldThrow.get())
         {
            throw new QException("Test exception");
         }
      }
   }

}
