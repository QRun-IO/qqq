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

package com.kingsrook.qqq.backend.core.model.tables;


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
 ** Unit test for QQQTableTableManager 
 *******************************************************************************/
class QQQTableTableManagerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTablesGetInsertedUponRequest() throws QException
   {
      new QQQTablesMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);

      Integer personMemoryTableId = QQQTableTableManager.getQQQTableId(QContext.getQInstance(), TestUtils.TABLE_NAME_PERSON_MEMORY);
      assertEquals(1, personMemoryTableId);

      assertEquals(1, QueryAction.execute(QQQTablesMetaDataProvider.QQQ_TABLE_CACHE_TABLE_NAME, new QQueryFilter()).size());
      assertEquals(1, QueryAction.execute(QQQTable.TABLE_NAME, new QQueryFilter()).size());
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExistingTableComesBack() throws QException
   {
      new QQQTablesMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);

      new InsertAction().execute(new InsertInput(QQQTable.TABLE_NAME).withRecordEntity(new QQQTable().withName(TestUtils.TABLE_NAME_SHAPE)));
      new InsertAction().execute(new InsertInput(QQQTable.TABLE_NAME).withRecordEntity(new QQQTable().withName(TestUtils.TABLE_NAME_ID_AND_NAME_ONLY)));

      assertEquals(0, QueryAction.execute(QQQTablesMetaDataProvider.QQQ_TABLE_CACHE_TABLE_NAME, new QQueryFilter()).size());
      assertEquals(2, QueryAction.execute(QQQTable.TABLE_NAME, new QQueryFilter()).size());

      assertEquals(2, QQQTableTableManager.getQQQTableId(QContext.getQInstance(), TestUtils.TABLE_NAME_ID_AND_NAME_ONLY));

      assertEquals(1, QueryAction.execute(QQQTablesMetaDataProvider.QQQ_TABLE_CACHE_TABLE_NAME, new QQueryFilter()).size());
      assertEquals(2, QueryAction.execute(QQQTable.TABLE_NAME, new QQueryFilter()).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBogusTableName() throws QException
   {
      new QQQTablesMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);
      assertNull(QQQTableTableManager.getQQQTableId(QContext.getQInstance(), "not a table"));
      assertEquals(0, QueryAction.execute(QQQTablesMetaDataProvider.QQQ_TABLE_CACHE_TABLE_NAME, new QQueryFilter()).size());
      assertEquals(0, QueryAction.execute(QQQTable.TABLE_NAME, new QQueryFilter()).size());
   }

}