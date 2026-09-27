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

package com.kingsrook.qqq.backend.core.actions.scripts;


import java.util.List;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for com.kingsrook.qqq.backend.core.actions.scripts.ScriptApi
 *******************************************************************************/
class QqqScriptUtilsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      QqqScriptUtils api = new QqqScriptUtils();
      assertThat(api.newFilterCriteria()).isInstanceOf(QFilterCriteria.class);
      assertThat(api.newFilterOrderBy()).isInstanceOf(QFilterOrderBy.class);
      assertThat(api.newQueryFilter()).isInstanceOf(QQueryFilter.class);
      assertThat(api.newRecord()).isInstanceOf(QRecord.class);
      assertThat(api.newQueryInput()).isInstanceOf(QueryInput.class);

      String tableName = TestUtils.TABLE_NAME_PERSON_MEMORY;
      String uuid      = UUID.randomUUID().toString();
      api.insert(tableName, new QRecord().withValue("firstName", uuid));
      List<QRecord> queryResult = api.query(api.newQueryInput()
         .withTableName(tableName)
         .withFilter(api.newQueryFilter()
            .withCriteria(api.newFilterCriteria()
               .withFieldName("firstName")
               .withOperator("EQUALS")
               .withValues(List.of(uuid)))));
      assertEquals(1, queryResult.size());
      assertEquals(uuid, queryResult.get(0).getValueString("firstName"));

      String newUUID = UUID.randomUUID().toString();
      api.update(tableName, api.newRecord().withValue("id", queryResult.get(0).getValue("id")).withValue("lastName", newUUID));
      QQueryFilter filter = api.newQueryFilter()
         .withCriteria(api.newFilterCriteria()
            .withFieldName("lastName")
            .withOperator("EQUALS")
            .withValues(List.of(newUUID)));
      queryResult = api.query(tableName, filter);
      assertEquals(1, queryResult.size());
      assertEquals(newUUID, queryResult.get(0).getValueString("lastName"));

      api.delete(tableName, queryResult.get(0).getValue("id"));
      queryResult = api.query(tableName, filter);
      assertEquals(0, queryResult.size());

      api.delete(tableName, filter);
   }

}