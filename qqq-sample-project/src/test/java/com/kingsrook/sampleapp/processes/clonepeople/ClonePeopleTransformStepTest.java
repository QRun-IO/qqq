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

package com.kingsrook.sampleapp.processes.clonepeople;


import java.util.ArrayList;
import com.kingsrook.qqq.backend.core.actions.metadata.ProcessMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.ProcessMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for ClonePeopleTransformStep
 *******************************************************************************/
class ClonePeopleTransformStepTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeAll
   static void beforeAll() throws Exception
   {
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterAll
   static void afterAll()
   {
      QContext.clear();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessStep() throws QException
   {
      QInstance qInstance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(qInstance, new QSession());

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(SampleMetaDataProvider.TABLE_NAME_PERSON);
      QueryOutput queryOutput = new QueryAction().execute(queryInput);

      RunBackendStepInput      input                    = new RunBackendStepInput();
      RunBackendStepOutput     output                   = new RunBackendStepOutput();
      ClonePeopleTransformStep clonePeopleTransformStep = new ClonePeopleTransformStep();

      input.setRecords(queryOutput.getRecords());
      clonePeopleTransformStep.runOnePage(input, output);

      ArrayList<ProcessSummaryLineInterface> processSummary = clonePeopleTransformStep.getProcessSummary(output, true);

      assertThat(processSummary)
         .usingRecursiveFieldByFieldElementComparatorOnFields("status", "count")
         .contains(new ProcessSummaryLine(Status.OK, 4, null))
         .contains(new ProcessSummaryLine(Status.ERROR, 1, null));
   }



   /*******************************************************************************
    ** The extract step refuses a run without records, so the process metadata a
    ** client reads must say at least one record is needed (QRun-IO/qqq#1341).
    *******************************************************************************/
   @Test
   void testMetaDataDeclaresMinInputRecords() throws QException
   {
      QInstance qInstance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(qInstance, new QSession());

      assertEquals(1, qInstance.getProcess(SampleMetaDataProvider.PROCESS_NAME_CLONE_PEOPLE).getMinInputRecords());

      ProcessMetaDataInput input = new ProcessMetaDataInput();
      input.setProcessName(SampleMetaDataProvider.PROCESS_NAME_CLONE_PEOPLE);
      assertEquals(1, new ProcessMetaDataAction().execute(input).getProcess().getMinInputRecords());
   }

}
