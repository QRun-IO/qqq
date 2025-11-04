/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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