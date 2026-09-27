/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.reporting;


import java.io.ByteArrayOutputStream;
import java.util.List;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportOutput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSActionTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Test some harder exports, using RDBMS backend.
 *******************************************************************************/
public class ExportActionWithinRDBMSTest extends RDBMSActionTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   public void beforeEach() throws Exception
   {
      super.primeTestDatabase();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIncludingFieldsFromExposedJoinTableWithTwoJoinsToMainTable() throws QException
   {
      QContext.setQSession(new QSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1));

      ByteArrayOutputStream baos        = new ByteArrayOutputStream();
      ExportInput           exportInput = new ExportInput();
      exportInput.setTableName(TestUtils.TABLE_NAME_ORDER);

      exportInput.setReportDestination(new ReportDestination()
         .withReportFormat(ReportFormat.CSV)
         .withReportOutputStream(baos));
      exportInput.setQueryFilter(new QQueryFilter());
      exportInput.setFieldNames(List.of("id", "storeId", "billToPersonId", "currentOrderInstructionsId", TestUtils.TABLE_NAME_ORDER_INSTRUCTIONS + ".id", TestUtils.TABLE_NAME_ORDER_INSTRUCTIONS + ".instructions"));
      ExportOutput exportOutput = new ExportAction().execute(exportInput);

      assertNotNull(exportOutput);

      ///////////////////////////////////////////////////////////////////////////
      // if there was an exception running the query, we get back 0 records... //
      ///////////////////////////////////////////////////////////////////////////
      assertEquals(3, exportOutput.getRecordCount());
   }

}
