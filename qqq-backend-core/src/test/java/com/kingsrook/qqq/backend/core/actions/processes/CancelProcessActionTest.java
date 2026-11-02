/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.util.List;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.secrets.OneTimeSecret;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for CancelProcessAction 
 *******************************************************************************/
public class CancelProcessActionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBadInputs()
   {
      RunProcessInput input = new RunProcessInput();
      assertThatThrownBy(() -> new CancelProcessAction().execute(input))
         .hasMessageContaining("Process [null] is not defined");

      input.setProcessName("foobar");
      assertThatThrownBy(() -> new CancelProcessAction().execute(input))
         .hasMessageContaining("Process [foobar] is not defined");

      input.setProcessName(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE);
      assertThatThrownBy(() -> new CancelProcessAction().execute(input))
         .hasMessageContaining("processUUID was not given");

      input.setProcessUUID("not-a-uuid");
      assertThatThrownBy(() -> new CancelProcessAction().execute(input))
         .isInstanceOf(QBadRequestException.class)
         .hasMessageContaining("is not a valid UUID");

      input.setProcessUUID(UUID.randomUUID().toString());
      assertThatThrownBy(() -> new CancelProcessAction().execute(input))
         .hasMessageContaining("State for process UUID")
         .hasMessageContaining("was not found");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      try
      {
         ///////////////////////////////////////////////////////////////
         // start up the process - having it break upon frontend step //
         ///////////////////////////////////////////////////////////////
         RunProcessInput input = new RunProcessInput();
         input.setProcessName(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE);
         input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);

         RunProcessOutput runProcessOutput = new RunProcessAction().execute(input);
         input.setProcessUUID(runProcessOutput.getProcessUUID());

         /////////////////////////////////////////////////////////////////////////////////
         // try to run the cancel action, but, with no cancel step, it should exit noop //
         /////////////////////////////////////////////////////////////////////////////////
         QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(CancelProcessAction.class);
         new CancelProcessAction().execute(input);
         assertThat(collectingLogger.getCollectedMessages())
            .anyMatch(m -> m.getMessage().contains("does not have a custom cancel step"));
         collectingLogger.clear();

         ///////////////////////////////////////////////////////////////////
         // cancelling is terminal: the cancelled run refuses more steps //
         ///////////////////////////////////////////////////////////////////
         input.setStartAfterStep("setup");
         assertThatThrownBy(() -> new RunProcessAction().execute(input))
            .isInstanceOf(QBadRequestException.class)
            .hasMessageContaining("process was cancelled");

         ////////////////////////////////////////////////////////////////
         // add a cancel step to this process, and cancel a fresh run //
         ////////////////////////////////////////////////////////////////
         QContext.getQInstance().getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE)
            .setCancelStep(new QBackendStepMetaData().withCode(new QCodeReference(CancelStep.class)));

         RunProcessInput freshInput = new RunProcessInput();
         freshInput.setProcessName(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE);
         freshInput.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
         freshInput.setProcessUUID(new RunProcessAction().execute(freshInput).getProcessUUID());
         new CancelProcessAction().execute(freshInput);
         assertThat(collectingLogger.getCollectedMessages())
            .noneMatch(m -> m.getMessage().contains("does not have a custom cancel step"))
            .anyMatch(m -> m.getMessage().contains("Running cancel step"));
         assertEquals(1, CancelStep.callCount);
      }
      finally
      {
         QLogger.deactivateCollectingLoggerForClass(CancelProcessAction.class);
      }
   }



   /*******************************************************************************
    ** A step that throws a QException must leave the run owned by its starter,
    ** who can still resume and cancel it - and nobody else can.
    *******************************************************************************/
   @Test
   void testFailedStepKeepsOwnerAbleToCancelAndResume() throws QException
   {
      QException thrown = new QException("step failed on purpose");
      addThrowingProcess("failsWithQException", thrown);

      QSession ownerSession = QContext.getQSession();
      String   processUUID  = startFailingProcess("failsWithQException", thrown);
      assertFailedStateOwnedAndClean(processUUID, "failsWithQException");

      //////////////////////////////////////////////////////////////////////////////
      // another session - a different user, or even the same user in a new       //
      // session - can neither cancel nor resume, and that does not alter the run //
      //////////////////////////////////////////////////////////////////////////////
      for(QSession otherSession : List.of(newSession("other-operator"), newSession()))
      {
         QContext.setQSession(otherSession);
         assertThatThrownBy(() -> new CancelProcessAction().execute(userInput("failsWithQException", processUUID)))
            .isInstanceOf(QPermissionDeniedException.class);
         assertThatThrownBy(() -> new RunProcessAction().execute(userInput("failsWithQException", processUUID).withStartAtStep("boom")))
            .isInstanceOf(QPermissionDeniedException.class);
      }

      ////////////////////////////////////////////////////////////////////////////////
      // the owner can resume (which hits the same failure - not a permission error) //
      ////////////////////////////////////////////////////////////////////////////////
      QContext.setQSession(ownerSession);
      assertThatThrownBy(() -> new RunProcessAction().execute(userInput("failsWithQException", processUUID).withStartAtStep("boom")))
         .isSameAs(thrown);

      ///////////////////////////////////////////////////////////////
      // and the owner can cancel, which is terminal for everybody //
      ///////////////////////////////////////////////////////////////
      RunProcessOutput cancelOutput = new CancelProcessAction().execute(userInput("failsWithQException", processUUID));
      assertTrue(cancelOutput.getProcessState().getIsCancelled());
      assertTrue(RunProcessAction.getStateForUser(processUUID, "failsWithQException").orElseThrow().getIsCancelled());
      assertThatThrownBy(() -> new RunProcessAction().execute(userInput("failsWithQException", processUUID).withStartAtStep("boom")))
         .isInstanceOf(QBadRequestException.class)
         .hasMessageContaining("process was cancelled");
   }



   /*******************************************************************************
    ** Same, for an ordinary runtime exception - which the process wraps, keeping
    ** the original as its cause.
    *******************************************************************************/
   @Test
   void testFailedStepRuntimeExceptionKeepsOwnerAbleToCancel() throws QException
   {
      IllegalStateException thrown = new IllegalStateException("runtime failure on purpose");
      addThrowingProcess("failsWithRuntimeException", thrown);

      QSession ownerSession = QContext.getQSession();
      String   processUUID  = UUID.randomUUID().toString();
      assertThatThrownBy(() -> new RunProcessAction().execute(userInput("failsWithRuntimeException", processUUID)))
         .isInstanceOf(QException.class)
         .hasCause(thrown);
      assertFailedStateOwnedAndClean(processUUID, "failsWithRuntimeException");

      QContext.setQSession(newSession());
      assertThatThrownBy(() -> new CancelProcessAction().execute(userInput("failsWithRuntimeException", processUUID)))
         .isInstanceOf(QPermissionDeniedException.class);

      QContext.setQSession(ownerSession);
      assertTrue(new CancelProcessAction().execute(userInput("failsWithRuntimeException", processUUID)).getProcessState().getIsCancelled());
   }



   /*******************************************************************************
    ** The (old, and still current) failure semantics: nothing a failed step set -
    ** values, records, one-time secrets - is kept or published.  Only ownership
    ** and the step list survive.
    *******************************************************************************/
   private void assertFailedStateOwnedAndClean(String processUUID, String processName) throws QException
   {
      ProcessState stored = RunProcessAction.getState(processUUID).orElseThrow();
      assertNotNull(stored.getStateAccess(), "A failed step must not strip the run's ownership");
      assertEquals(processUUID, stored.getStateAccess().requireCurrentProcessUUID(processName));
      assertEquals(List.of("boom"), stored.getStepList());
      assertThat(stored.getValues()).doesNotContainKeys("partial", RunProcessAction.ONE_TIME_SECRETS_MASKED);
      assertThat(stored.getRecords()).isEmpty();
      assertFalse(stored.getIsCancelled());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String startFailingProcess(String processName, QException thrown)
   {
      String processUUID = UUID.randomUUID().toString();
      assertThatThrownBy(() -> new RunProcessAction().execute(userInput(processName, processUUID))).isSameAs(thrown);
      return (processUUID);
   }



   /*******************************************************************************
    ** a process whose only step sets some output, then throws.
    *******************************************************************************/
   private void addThrowingProcess(String processName, Exception toThrow)
   {
      QContext.getQInstance().addProcess(new QProcessMetaData().withName(processName)
         .withStep(new QBackendStepMetaData().withName("boom").withCode(new QCodeReferenceLambda<BackendStep>((input, output) ->
         {
            output.addValue("partial", "should-not-be-kept");
            output.setRecords(List.of(new QRecord().withValue("id", 1)));
            output.addOneTimeSecret(new OneTimeSecret().withLabel("partial").withValue("partial-secret-value").withMasked("partial-masked"));
            if(toThrow instanceof QException qe)
            {
               throw qe;
            }
            throw (RuntimeException) toThrow;
         }))));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RunProcessInput userInput(String processName, String processUUID)
   {
      return new RunProcessInput().withProcessName(processName).withProcessUUID(processUUID).withInputSource(QInputSource.USER);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class CancelStep implements BackendStep
   {
      static int callCount = 0;



      @Override
      public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
      {
         callCount++;
      }
   }

}