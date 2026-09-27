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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for TableVariantsSpecV1
 *******************************************************************************/
class TableVariantsSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableVariantsSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /*******************************************************************************
    ** A variant table lists its options (type and id to send back, and the label).
    *******************************************************************************/
   @Test
   void testVariants() throws QException
   {
      QContext.init(serverQInstance, new QSystemUserSession());
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "People"),
         new QRecord().withValue("id", 2).withValue("name", "Planets")
      )));
      QContext.clear();

      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/" + TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA + "/variants").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONArray variants = JsonUtils.toJSONObject(response.getBody()).getJSONArray("variants");
      assertEquals(2, variants.length(), response.getBody());
      assertEquals("1", variants.getJSONObject(0).getString("id"));
      assertEquals(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, variants.getJSONObject(0).getString("type"));
   }



   /*******************************************************************************
    ** A table without variants answers an empty list; an unknown table is refused.
    *******************************************************************************/
   @Test
   void testNoVariantsAndUnknownTable()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/table/person/variants").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      assertEquals(0, JsonUtils.toJSONObject(response.getBody()).getJSONArray("variants").length());

      assertEquals(403, Unirest.get(getBaseUrlAndPath() + "/table/notATable/variants").asString().getStatus());
   }

}
