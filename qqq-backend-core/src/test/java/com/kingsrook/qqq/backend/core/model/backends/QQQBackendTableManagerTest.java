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

package com.kingsrook.qqq.backend.core.model.backends;


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
 ** Unit test for QQQBackendTableManager 
 *******************************************************************************/
class QQQBackendTableManagerTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBackendsGetInsertedUponRequest() throws QException
   {
      new QQQBackendsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);

      Integer greetPeopleBackendId = QQQBackendTableManager.getQQQBackendId(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME);
      assertEquals(1, greetPeopleBackendId);

      assertEquals(1, QueryAction.execute(QQQBackendsMetaDataProvider.QQQ_BACKEND_CACHE_TABLE_NAME, new QQueryFilter()).size());
      assertEquals(1, QueryAction.execute(QQQBackend.TABLE_NAME, new QQueryFilter()).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testExistingBackendComesBack() throws QException
   {
      new QQQBackendsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);

      new InsertAction().execute(new InsertInput(QQQBackend.TABLE_NAME).withRecordEntity(new QQQBackend().withName(TestUtils.DEFAULT_BACKEND_NAME)));
      new InsertAction().execute(new InsertInput(QQQBackend.TABLE_NAME).withRecordEntity(new QQQBackend().withName(TestUtils.MEMORY_BACKEND_WITH_VARIANTS_NAME)));

      assertEquals(0, QueryAction.execute(QQQBackendsMetaDataProvider.QQQ_BACKEND_CACHE_TABLE_NAME, new QQueryFilter()).size());
      assertEquals(2, QueryAction.execute(QQQBackend.TABLE_NAME, new QQueryFilter()).size());

      assertEquals(2, QQQBackendTableManager.getQQQBackendId(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_WITH_VARIANTS_NAME));

      assertEquals(1, QueryAction.execute(QQQBackendsMetaDataProvider.QQQ_BACKEND_CACHE_TABLE_NAME, new QQueryFilter()).size());
      assertEquals(2, QueryAction.execute(QQQBackend.TABLE_NAME, new QQueryFilter()).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBogusBackendName() throws QException
   {
      new QQQBackendsMetaDataProvider().defineAll(QContext.getQInstance(), TestUtils.MEMORY_BACKEND_NAME, TestUtils.MEMORY_BACKEND_NAME, null);
      assertNull(QQQBackendTableManager.getQQQBackendId(QContext.getQInstance(), "not a backend"));
      assertEquals(0, QueryAction.execute(QQQBackendsMetaDataProvider.QQQ_BACKEND_CACHE_TABLE_NAME, new QQueryFilter()).size());
      assertEquals(0, QueryAction.execute(QQQBackend.TABLE_NAME, new QQueryFilter()).size());
   }
}