/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.processes;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for QQQProcessTableManager 
 *******************************************************************************/
class QQQProcessTableManagerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessesGetInsertedUponRequest() throws QException
   {
      new QQQProcessesMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);

      Integer greetPeopleProcessId = QQQProcessTableManager.getQQQProcessId(QContext.getQInstance(), TestUtils.PROCESS_NAME_GREET_PEOPLE);
      assertEquals(1, greetPeopleProcessId);

      assertEquals(1, QueryAction.execute(QQQProcessesMetaDataProvider.QQQ_PROCESS_CACHE_TABLE_NAME, new QQueryFilter()).size());
      assertEquals(1, QueryAction.execute(QQQProcess.TABLE_NAME, new QQueryFilter()).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExistingProcessComesBack() throws QException
   {
      new QQQProcessesMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);

      new InsertAction().execute(new InsertInput(QQQProcess.TABLE_NAME).withRecordEntity(new QQQProcess().withName(TestUtils.PROCESS_NAME_GREET_PEOPLE)));
      new InsertAction().execute(new InsertInput(QQQProcess.TABLE_NAME).withRecordEntity(new QQQProcess().withName(TestUtils.PROCESS_NAME_ADD_TO_PEOPLES_AGE)));

      assertEquals(0, QueryAction.execute(QQQProcessesMetaDataProvider.QQQ_PROCESS_CACHE_TABLE_NAME, new QQueryFilter()).size());
      assertEquals(2, QueryAction.execute(QQQProcess.TABLE_NAME, new QQueryFilter()).size());

      assertEquals(2, QQQProcessTableManager.getQQQProcessId(QContext.getQInstance(), TestUtils.PROCESS_NAME_ADD_TO_PEOPLES_AGE));

      assertEquals(1, QueryAction.execute(QQQProcessesMetaDataProvider.QQQ_PROCESS_CACHE_TABLE_NAME, new QQueryFilter()).size());
      assertEquals(2, QueryAction.execute(QQQProcess.TABLE_NAME, new QQueryFilter()).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBogusProcessName() throws QException
   {
      new QQQProcessesMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);
      assertNull(QQQProcessTableManager.getQQQProcessId(QContext.getQInstance(), "not a process"));
      assertEquals(0, QueryAction.execute(QQQProcessesMetaDataProvider.QQQ_PROCESS_CACHE_TABLE_NAME, new QQueryFilter()).size());
      assertEquals(0, QueryAction.execute(QQQProcess.TABLE_NAME, new QQueryFilter()).size());
   }

}