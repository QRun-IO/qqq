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

package com.kingsrook.qqq.backend.module.rdbms.model.metadata;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for RDBMSTableMetaDataBuilder 
 *******************************************************************************/
class RDBMSTableMetaDataBuilderTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      TestUtils.primeTestDatabase("prime-test-database.sql");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QBackendMetaData backend = QContext.getQInstance().getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      QTableMetaData   table   = new RDBMSTableMetaDataBuilder().buildTableMetaData((RDBMSBackendMetaData) backend, "order");

      assertNotNull(table);
      assertEquals("order", table.getName());
      assertEquals("id", table.getPrimaryKeyField());
      assertEquals(QFieldType.INTEGER, table.getField(table.getPrimaryKeyField()).getType());
      assertNotNull(table.getField("storeId"));
   }

}