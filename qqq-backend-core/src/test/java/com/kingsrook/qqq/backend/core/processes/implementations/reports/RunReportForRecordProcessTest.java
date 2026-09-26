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

package com.kingsrook.qqq.backend.core.processes.implementations.reports;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;


/*******************************************************************************
 ** Unit test for BasicRunReportProcess
 *******************************************************************************/
class RunReportForRecordProcessTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRunReport() throws QException
   {
      QInstance instance = QContext.getQInstance();
      TestUtils.insertDefaultShapes(instance);

      RunProcessInput runProcessInput = new RunProcessInput();
      runProcessInput.setProcessName(TestUtils.PROCESS_NAME_RUN_SHAPES_PERSON_REPORT);
      runProcessInput.addValue(BasicRunReportProcess.FIELD_REPORT_NAME, TestUtils.REPORT_NAME_SHAPES_PERSON);
      runProcessInput.addValue("recordsParam", "recordIds");
      runProcessInput.addValue("recordIds", "1");

      RunProcessOutput runProcessOutput = new RunProcessAction().execute(runProcessInput);

      // runProcessOutput = new RunProcessAction().execute(runProcessInput);
      // assertThat(runProcessOutput.getProcessState().getNextStepName()).isPresent().get().isEqualTo(BasicRunReportProcess.STEP_NAME_ACCESS);
      // assertThat(runProcessOutput.getValues()).containsKeys("downloadFileName", "serverFilePath");
   }

}