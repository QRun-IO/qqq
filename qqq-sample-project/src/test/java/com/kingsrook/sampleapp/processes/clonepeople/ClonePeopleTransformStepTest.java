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

package com.kingsrook.sampleapp.processes.clonepeople;


import java.util.ArrayList;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
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

}