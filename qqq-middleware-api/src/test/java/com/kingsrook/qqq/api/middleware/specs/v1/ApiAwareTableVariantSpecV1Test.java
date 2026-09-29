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

package com.kingsrook.qqq.api.middleware.specs.v1;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.api.TestUtils;
import com.kingsrook.qqq.api.middleware.specs.ApiAwareSpecTestBase;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import io.javalin.http.ContentType;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** API-versioned query and count must use the selected backend partition.
 *******************************************************************************/
class ApiAwareTableVariantSpecV1Test extends ApiAwareSpecTestBase
{
   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ApiAwareTableQuerySpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return List.of(new ApiAwareTableCountSpecV1());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = super.defineQInstance();
      instance.getTable(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA).withSupplementalMetaData(new ApiTableMetaDataContainer()
         .withApiTableMetaData(TestUtils.API_NAME, new ApiTableMetaData().withInitialVersion(TestUtils.V2023_Q1)));
      return instance;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected void primeTestData(QInstance instance) throws Exception
   {
      QContext.withTemporaryContext(new CapturedContext(instance, new QSystemUserSession()), () ->
      {
         new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS).withRecords(List.of(
            new QRecord().withValue("id", 1).withValue("name", "North"),
            new QRecord().withValue("id", 2).withValue("name", "South"))));
      });
      for(int variant : List.of(1, 2))
      {
         var session = new QSystemUserSession().withBackendVariants(Map.of(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, variant));
         QContext.withTemporaryContext(new CapturedContext(instance, session), () ->
         {
            List<QRecord> records = variant == 1
               ? List.of(new QRecord().withValue("id", 1).withValue("name", "North apple"), new QRecord().withValue("id", 2).withValue("name", "North pear"))
               : List.of(new QRecord().withValue("id", 1).withValue("name", "South kiwi"));
            new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA).withRecords(records));
         });
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testQueryVariantSelectionAndMissingVariant()
   {
      assertVariantSelectionAndMissingVariant("query");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testCountVariantSelectionAndMissingVariant()
   {
      assertVariantSelectionAndMissingVariant("count");
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private void assertVariantSelectionAndMissingVariant(String operation)
   {
      String url = getBaseUrlAndPath(TestUtils.API_PATH, TestUtils.V2023_Q1) + "/table/" + TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA + "/" + operation;
      for(String variant : List.of("1", "2", "1"))
      {
         var response = Unirest.post(url).contentType(ContentType.APPLICATION_JSON.getMimeType())
            .body(JsonUtils.toJson(Map.of("filter", Map.of(), "tableVariant", Map.of("type", TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, "id", variant))))
            .asString();
         assertEquals(200, response.getStatus(), response.getBody());
         JSONObject body = new JSONObject(response.getBody());
         int expectedCount = variant.equals("1") ? 2 : 1;
         assertEquals(expectedCount, operation.equals("count") ? body.getInt("count") : body.getJSONArray("records").length());
         if(operation.equals("query"))
         {
            for(Object record : body.getJSONArray("records"))
            {
               assertTrue(((JSONObject) record).getJSONObject("values").getString("name").startsWith(variant.equals("1") ? "North" : "South"));
            }
         }
      }
      var missing = Unirest.post(url).contentType(ContentType.APPLICATION_JSON.getMimeType()).body("{}").asString();
      assertEquals(500, missing.getStatus());
      assertTrue(missing.getBody().contains("Could not find Backend Variant information"));
   }
}
