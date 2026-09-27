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

package com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for NoopTransformStep
 *******************************************************************************/
class NoopTransformStepTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      RunBackendStepInput input = new RunBackendStepInput();
      input.setTableName(TestUtils.TABLE_NAME_PERSON);
      input.setRecords(List.of(new QRecord().withValue("id", 47)));

      RunBackendStepOutput output = new RunBackendStepOutput();

      NoopTransformStep noopTransformStep = new NoopTransformStep();
      noopTransformStep.runOnePage(input, output);

      assertEquals(1, output.getRecords().size());
      assertEquals(47, output.getRecords().get(0).getValueInteger("id"));

      ArrayList<ProcessSummaryLineInterface> processSummary = noopTransformStep.getProcessSummary(output, false);
      assertEquals(1, processSummary.size());
      ProcessSummaryLineInterface processSummaryLineInterface = processSummary.get(0);
      assertEquals(Status.OK, processSummaryLineInterface.getStatus());
      if(processSummaryLineInterface instanceof ProcessSummaryLine processSummaryLine)
      {
         assertEquals(1, processSummaryLine.getCount());
         assertEquals(1, processSummaryLine.getPrimaryKeys().size());
         assertEquals(47, processSummaryLine.getPrimaryKeys().get(0));
      }
   }

}