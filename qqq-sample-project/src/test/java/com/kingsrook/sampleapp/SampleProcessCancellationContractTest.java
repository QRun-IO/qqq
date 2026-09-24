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

package com.kingsrook.sampleapp;


import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.CancelProcessAction;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native cancellation access, owner resume and unknown/missing-ID controls.
 *******************************************************************************/
class SampleProcessCancellationContractTest
{
   private QInstance instance;
   private QSession owner;
   private static AtomicInteger afterStepCalls;
   private static AtomicInteger cancelCalls;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.addProcess(new QProcessMetaData().withName("ownedPaused").withTableName("person")
         .withStep(step("read", ReadPeople.class)).withStep(new QFrontendStepMetaData().withName("wait"))
         .withStep(step("after", AfterStep.class)).withCancelStep(step("cancel", CancelHook.class)));
      owner = session();
      QContext.init(instance, owner);
      afterStepCalls = new AtomicInteger();
      cancelCalls = new AtomicInteger();
   }



   /*******************************************************************************
    ** Release the owned context and native connection provider.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testUnknownAndMissingProcessIdsAreRejected() throws Exception
   {
      RunProcessInput input = input("ownedPaused");
      assertThrows(QBadRequestException.class, () -> new CancelProcessAction().execute(input));
      input.setProcessUUID(null);
      assertThrows(QBadRequestException.class, () -> new CancelProcessAction().execute(input));
      assertEquals(0, cancelCalls.get());
   }



   /*******************************************************************************
    ** An unauthorized attempt leaves the owner's paused process resumable.
    *******************************************************************************/
   @Test
   void testOtherSessionCannotCancelProcess() throws Exception
   {
      RunProcessInput input = startPaused();
      QContext.init(instance, session());
      assertThrows(QPermissionDeniedException.class, () -> new CancelProcessAction().execute(input));
      assertEquals(0, cancelCalls.get());
      QContext.init(instance, owner);
      input.setStartAfterStep("wait");
      new RunProcessAction().execute(input);
      assertEquals(1, afterStepCalls.get());
   }



   /*******************************************************************************
    ** Cancel invokes the application hook; terminal lifecycle is a known deferral.
    *******************************************************************************/
   @Test
   void testOwnerCancelHookAndNoHookPreserveCaller() throws Exception
   {
      RunProcessInput input = startPaused();
      assertEquals("wait", new CancelProcessAction().execute(input).getProcessState().getNextStepName().orElseThrow());
      assertEquals(1, cancelCalls.get());
      assertEquals(0, afterStepCalls.get());
      instance.getProcess("ownedPaused").setCancelStep(null);
      new CancelProcessAction().execute(input);
      assertEquals(1, cancelCalls.get());
      assertSame(owner, QContext.getQSession());
      input.setStartAfterStep("wait");
      new RunProcessAction().execute(input);
      assertEquals(1, afterStepCalls.get());
      assertTrue(new CancelProcessAction().execute(input).getProcessState().getNextStepName().isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCancelHookFailurePreservesOwnerAndAllowsFreshRun() throws Exception
   {
      for(Boolean checked : List.of(true, false))
      {
         RunProcessInput input = startPaused();
         instance.getProcess("ownedPaused").setCancelStep(new QBackendStepMetaData().withName("throwingCancel")
            .withCode(new QCodeReferenceLambda<BackendStep>((in, out) ->
            {
               cancelCalls.incrementAndGet();
               if(checked)
               {
                  throw new QException("Owned cancellation failure");
               }
               throw new IllegalStateException("Owned unchecked cancellation failure");
            })));
         assertThrows(QException.class, () -> new CancelProcessAction().execute(input));
         assertSame(instance, QContext.getQInstance());
         assertSame(owner, QContext.getQSession());
         assertEquals(0, afterStepCalls.get());
      }
      assertEquals(2, cancelCalls.get());
      RunProcessInput input = startPaused();
      input.setStartAfterStep("wait");
      new RunProcessAction().execute(input);
      assertEquals(1, afterStepCalls.get());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunProcessInput startPaused() throws Exception
   {
      RunProcessInput input = input("ownedPaused");
      RunProcessOutput result = new RunProcessAction().execute(input);
      assertEquals("wait", result.getProcessState().getNextStepName().orElseThrow());
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunProcessInput input(String name)
   {
      return input(name, UUID.randomUUID().toString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunProcessInput input(String name, String processUUID)
   {
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(name);
      input.setProcessUUID(processUUID);
      input.setInputSource(QInputSource.USER);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QBackendStepMetaData step(String name, Class<? extends BackendStep> code)
   {
      return new QBackendStepMetaData().withName(name).withCode(new QCodeReference(code));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QSession session()
   {
      QSession session = new QSession();
      session.setUser(new QUser());
      session.setPermissions(Set.of());
      return session;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ReadPeople implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         QueryInput query = new QueryInput();
         query.setTableName("person");
         query.setInputSource(QInputSource.USER);
         assertEquals(5, new QueryAction().execute(query).getRecords().size());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class AfterStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output)
      {
         afterStepCalls.incrementAndGet();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class CancelHook implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         cancelCalls.incrementAndGet();

      }
   }
}
