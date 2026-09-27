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

package com.kingsrook.qqq.backend.module.api.actions;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.api.BaseTest;
import com.kingsrook.qqq.backend.module.api.TestUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Unit test for APIRecordUtils 
 *******************************************************************************/
class APIRecordUtilsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      QTableMetaData table = QContext.getQInstance().getTable(TestUtils.MOCK_TABLE_NAME);
      table.withField(new QFieldMetaData("myField", QFieldType.INTEGER).withBackendName("myBackendName"));

      QRecord record = new QRecord()
         .withValue("foo", 1)
         .withValue("bar.baz", 2)
         .withValue("list[0].a", 3)
         .withValue("list[1].a", 4)
         .withValue("list[1].b", 5)
         .withValue("myField", 6);

      JSONObject jsonObject = APIRecordUtils.jsonQueryStyleQRecordToJSONObject(table, record, true);
      assertEquals(1, jsonObject.getInt("foo"));
      assertEquals(2, jsonObject.getJSONObject("bar").getInt("baz"));
      assertEquals(3, jsonObject.getJSONArray("list").getJSONObject(0).getInt("a"));
      assertEquals(4, jsonObject.getJSONArray("list").getJSONObject(1).getInt("a"));
      assertEquals(5, jsonObject.getJSONArray("list").getJSONObject(1).getInt("b"));
      assertEquals(6, jsonObject.getInt("myBackendName"));
      assertFalse(jsonObject.has("myField"));

      ///////////////////////////////////////////////////////
      // if we say "false" for includeNonTableFields, then //
      // we should only get the myField field              //
      ///////////////////////////////////////////////////////
      jsonObject = APIRecordUtils.jsonQueryStyleQRecordToJSONObject(table, record, false);
      assertFalse(jsonObject.has("foo"));
      assertEquals(6, jsonObject.getInt("myBackendName"));
   }

}