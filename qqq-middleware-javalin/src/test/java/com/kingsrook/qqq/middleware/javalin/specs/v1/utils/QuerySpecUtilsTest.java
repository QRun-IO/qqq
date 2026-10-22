/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1.utils;


import java.io.IOException;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QBadRequestException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Supplied filters must match the documented JSON object shape.
 *******************************************************************************/
class QuerySpecUtilsTest
{
   /*******************************************************************************
    ** Missing/null filters retain the existing unfiltered request contract.
    *******************************************************************************/
   @Test
   void testAbsentAndNullFilters() throws IOException
   {
      assertNull(QuerySpecUtils.getFilterFromRequestBody(new JSONObject()));
      assertNull(QuerySpecUtils.getFilterFromRequestBody(new JSONObject("{\"filter\":null}")));
   }



   /*******************************************************************************
    ** Structured filters preserve selection and paging values.
    *******************************************************************************/
   @Test
   void testStructuredFilter() throws IOException
   {
      QQueryFilter filter = QuerySpecUtils.getFilterFromRequestBody(new JSONObject("""
         {"filter":{"limit":3,"skip":1,"criteria":[{"fieldName":"id","operator":"EQUALS","values":[7]}]}}
         """));
      assertEquals(3, filter.getLimit());
      assertEquals(1, filter.getSkip());
      assertEquals("id", filter.getCriteria().getFirst().getFieldName());
      assertEquals(List.of(7), filter.getCriteria().getFirst().getValues());
   }



   /*******************************************************************************
    ** Wrong JSON types must fail before becoming an unfiltered backend action.
    *******************************************************************************/
   @Test
   void testNonObjectFiltersFail()
   {
      for(String json : List.of("{\"filter\":\"id=1\"}", "{\"filter\":[]}", "{\"filter\":42}", "{\"filter\":true}"))
      {
         IOException failure = assertThrows(IOException.class,
            () -> QuerySpecUtils.getFilterFromRequestBody(new JSONObject(json)), json);
         assertTrue(assertInstanceOf(QBadRequestException.class, failure.getCause()).getMessage().contains("filter"));
      }
   }
}
