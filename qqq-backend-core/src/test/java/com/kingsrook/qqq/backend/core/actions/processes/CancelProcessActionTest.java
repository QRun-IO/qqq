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


import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


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

         ///////////////////////////////////////
         // add a cancel step to this process //
         ///////////////////////////////////////
         QContext.getQInstance().getProcess(TestUtils.PROCESS_NAME_GREET_PEOPLE_INTERACTIVE)
            .setCancelStep(new QBackendStepMetaData().withCode(new QCodeReference(CancelStep.class)));

         new CancelProcessAction().execute(input);
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