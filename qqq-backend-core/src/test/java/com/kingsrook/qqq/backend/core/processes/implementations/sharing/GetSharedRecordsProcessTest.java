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


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
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
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for GetSharedRecordsProcess 
 *******************************************************************************/
class GetSharedRecordsProcessTest extends BaseTest
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
         .withColumnsJson(JsonUtils.toJson(new ReportColumns().withColumn("id")))));

      new InsertAction().execute(new InsertInput(SharedSavedReport.TABLE_NAME).withRecordEntity(new SharedSavedReport()
         .withScope(ShareScope.READ_WRITE.getPossibleValueId())
         .withUserId("007")
         .withSavedReportId(1)
      ));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      RunBackendStepInput     input       = new RunBackendStepInput();
      RunBackendStepOutput    output      = new RunBackendStepOutput();
      GetSharedRecordsProcess processStep = new GetSharedRecordsProcess();

      input.addValue("tableName", SavedReport.TABLE_NAME);
      input.addValue("recordId", 1);
      processStep.run(input, output);

      @SuppressWarnings("unchecked")
      List<QRecord> resultList = (List<QRecord>) output.getValue("resultList");
      assertEquals(1, resultList.size());

      QRecord outputRecord = resultList.get(0);
      assertEquals(1, outputRecord.getValueInteger("shareId"));
      assertEquals(ShareScope.READ_WRITE.getPossibleValueId(), outputRecord.getValueString("scopeId"));
      assertEquals("user", outputRecord.getValueString("audienceType"));
      assertEquals("007", outputRecord.getValueString("audienceId"));
      assertEquals("user 007", outputRecord.getValueString("audienceLabel"));
   }

}