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

package com.kingsrook.qqq.backend.core.processes.implementations.sharing;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.savedreports.ReportColumns;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedreports.SharedSavedReport;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for DeleteSharedRecordProcess
 *******************************************************************************/
class DeleteSharedRecordProcessTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      new SavedReportsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);

      new InsertAction().execute(new InsertInput(SavedReport.TABLE_NAME).withRecordEntity(new SavedReport()
         .withTableName(TestUtils.TABLE_NAME_PERSON_MEMORY)
         .withLabel("Test")
         .withColumnsJson(JsonUtils.toJson(new ReportColumns().withColumn("id")))
      ));

      new InsertAction().execute(new InsertInput(SharedSavedReport.TABLE_NAME).withRecordEntity(new SharedSavedReport()
         .withSavedReportId(1)
         .withUserId(BaseTest.DEFAULT_USER_ID)
         .withScope(ShareScope.READ_WRITE.getPossibleValueId())
      ));

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFailCases() throws QException
   {
      RunBackendStepInput       input       = new RunBackendStepInput();
      RunBackendStepOutput      output      = new RunBackendStepOutput();
      DeleteSharedRecordProcess processStep = new DeleteSharedRecordProcess();

      assertThatThrownBy(() -> processStep.run(input, output)).hasMessageContaining("Missing required input: tableName");
      input.addValue("tableName", SavedReport.TABLE_NAME);
      assertThatThrownBy(() -> processStep.run(input, output)).hasMessageContaining("Missing required input: recordId");
      input.addValue("recordId", 1);
      assertThatThrownBy(() -> processStep.run(input, output)).hasMessageContaining("Missing required input: shareId");
      input.addValue("shareId", 3);

      ///////////////////////////////////////////////////
      // fail because the requested record isn't found //
      ///////////////////////////////////////////////////
      assertThatThrownBy(() -> processStep.run(input, output)).hasMessageContaining("Error deleting shared record: No record was found to delete");

      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      // now fail because a different user (than the owner, who did the initial delete) is trying to share //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////
      QContext.setQSession(newSession("not-" + DEFAULT_USER_ID));
      input.addValue("shareId", 1);
      assertThatThrownBy(() -> processStep.run(input, output)).hasMessageContaining("not the owner of this record");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSuccess() throws QException
   {
      RunBackendStepInput       input       = new RunBackendStepInput();
      RunBackendStepOutput      output      = new RunBackendStepOutput();
      DeleteSharedRecordProcess processStep = new DeleteSharedRecordProcess();

      input.addValue("tableName", SavedReport.TABLE_NAME);
      input.addValue("recordId", 1);
      input.addValue("shareId", 1);

      //////////////////////////////////////////
      // assert the shared record got deleted //
      //////////////////////////////////////////
      processStep.run(input, output);

      QRecord sharedSavedReportRecord = new GetAction().executeForRecord(new GetInput(SharedSavedReport.TABLE_NAME).withPrimaryKey(1));
      assertNull(sharedSavedReportRecord);
   }

}