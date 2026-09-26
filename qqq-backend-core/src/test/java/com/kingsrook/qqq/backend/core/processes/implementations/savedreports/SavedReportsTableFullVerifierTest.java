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

package com.kingsrook.qqq.backend.core.processes.implementations.savedreports;


import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.reporting.GenerateReportActionTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.savedreports.ReportColumns;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


/*******************************************************************************
 ** Unit test for SavedReportsTableFullVerifier 
 *******************************************************************************/
class SavedReportsTableFullVerifierTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      new SavedReportsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);
      GenerateReportActionTest.insertPersonRecords(QContext.getQInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      ReportColumns reportColumns = new ReportColumns();
      reportColumns.withColumn("id");

      //////////////////////////////////
      // insert a saved report record //
      //////////////////////////////////
      SavedReport savedReport = new SavedReport();
      savedReport.setTableName(TestUtils.TABLE_NAME_PERSON_MEMORY);
      savedReport.setLabel("Test");
      savedReport.setColumnsJson(JsonUtils.toJson(reportColumns));
      savedReport.setQueryFilterJson(JsonUtils.toJson(new QQueryFilter()));
      List<QRecord> reportRecordList = new InsertAction().execute(new InsertInput(SavedReport.TABLE_NAME).withRecordEntity(savedReport)).getRecords();

      SavedReportsTableFullVerifier savedReportsTableFullVerifier = new SavedReportsTableFullVerifier();
      savedReportsTableFullVerifier.verify(reportRecordList, SavedReportsMetaDataProvider.REPORT_STORAGE_TABLE_NAME);
   }

}