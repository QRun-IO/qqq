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

package com.kingsrook.qqq.backend.core.processes.implementations.mock;


import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;


/*******************************************************************************
 ** Mock implementation of a FunctionBody.
 **
 ** Basically just passes data from the request to the response.
 *******************************************************************************/
public class MockBackendStep implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(MockBackendStep.class);

   public static final String FIELD_GREETING_PREFIX = "greetingPrefix";
   public static final String FIELD_GREETING_SUFFIX = "greetingSuffix";
   public static final String FIELD_MOCK_VALUE      = "mockValue";
   public static final String MOCK_VALUE            = "You so silly";



   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      /////////////////////////////////
      // mock the "greet" process... //
      /////////////////////////////////
      runBackendStepOutput.addValue("outputMessage", runBackendStepInput.getValueString(FIELD_GREETING_PREFIX) + " X " + runBackendStepInput.getValueString(FIELD_GREETING_SUFFIX));

      runBackendStepInput.getRecords().forEach(r ->
      {
         LOG.info("We are mocking " + r.getValueString("firstName") + ": " + r.getValue(FIELD_MOCK_VALUE));
         r.setValue(FIELD_MOCK_VALUE, "Ha ha!");
         r.setValue("greetingMessage", runBackendStepInput.getValueString(FIELD_GREETING_PREFIX) + " " + r.getValueString("firstName") + " " + runBackendStepInput.getValueString(FIELD_GREETING_SUFFIX));
      });

      runBackendStepOutput.setValues(runBackendStepInput.getValues());
      runBackendStepOutput.addValue(FIELD_MOCK_VALUE, MOCK_VALUE);
      runBackendStepOutput.addValue("noOfPeopleGreeted", runBackendStepInput.getRecords().size());
      runBackendStepOutput.addValue(RunProcessAction.BASEPULL_DID_QUERY_USING_TIMESTAMP_FIELD, true);
      runBackendStepOutput.addValue(RunProcessAction.BASEPULL_READY_TO_UPDATE_TIMESTAMP_FIELD, true);

      if("there".equalsIgnoreCase(runBackendStepInput.getValueString(FIELD_GREETING_SUFFIX)))
      {
         throw (new QException("You said Hello There, didn't you..."));
      }
   }
}
