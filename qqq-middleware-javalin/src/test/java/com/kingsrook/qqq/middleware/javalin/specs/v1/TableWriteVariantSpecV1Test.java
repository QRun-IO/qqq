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
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import io.javalin.http.ContentType;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


/*******************************************************************************
 ** The v1 insert, update and delete routes honor a tableVariant query
 ** parameter (as the query, count and get routes do) - #1351.
 *******************************************************************************/
class TableWriteVariantSpecV1Test extends SpecTestBase
{
   private static final String VARIANT_PEOPLE  = "{\"type\":\"" + TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS + "\",\"id\":1}";
   private static final String VARIANT_PLANETS = "{\"type\":\"" + TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS + "\",\"id\":\"2\"}";



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new TableInsertSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return List.of(new TableUpdateSpecV1(), new TableDeleteSpecV1());
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
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Insert, update and delete each act on the variant named by the query
    ** parameter, and leave the other variant alone.
    *******************************************************************************/
   @Test
   void testInsertUpdateDeleteUseVariant() throws QException
   {
      insertVariantOptions();
      String tableUrl = getBaseUrlAndPath() + "/table/" + TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA;

      HttpResponse<String> insert = Unirest.post(tableUrl)
         .queryString("tableVariant", VARIANT_PLANETS)
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("name", "Venus")))
         .asString();
      assertEquals(200, insert.getStatus(), insert.getBody());
      int id = JsonUtils.toJSONObject(insert.getBody()).getJSONObject("record").getJSONObject("values").getInt("id");
      assertEquals(List.of("Venus"), namesInVariant(2));
      assertEquals(List.of(), namesInVariant(1));

      HttpResponse<String> update = Unirest.patch(tableUrl + "/" + id)
         .queryString("tableVariant", VARIANT_PLANETS)
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("name", "Mercury")))
         .asString();
      assertEquals(200, update.getStatus(), update.getBody());
      assertEquals(List.of("Mercury"), namesInVariant(2));

      ///////////////////////////////////////////////////////////////
      // a delete against the other variant does not touch planets //
      ///////////////////////////////////////////////////////////////
      Unirest.delete(tableUrl + "/" + id).queryString("tableVariant", VARIANT_PEOPLE).asString();
      assertEquals(List.of("Mercury"), namesInVariant(2));

      HttpResponse<String> delete = Unirest.delete(tableUrl + "/" + id)
         .queryString("tableVariant", VARIANT_PLANETS)
         .asString();
      assertEquals(200, delete.getStatus(), delete.getBody());
      assertEquals(List.of(), namesInVariant(2));
   }



   /*******************************************************************************
    ** Without a variant the write fails (as a read would); a tableVariant that is
    ** not a JSON object is a bad request.
    *******************************************************************************/
   @Test
   void testMissingOrMalformedVariant() throws QException
   {
      insertVariantOptions();
      String tableUrl = getBaseUrlAndPath() + "/table/" + TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA;

      HttpResponse<String> noVariant = Unirest.post(tableUrl)
         .contentType(ContentType.APPLICATION_JSON.getMimeType())
         .body(JsonUtils.toJson(Map.of("name", "Venus")))
         .asString();
      assertNotEquals(200, noVariant.getStatus(), noVariant.getBody());

      HttpResponse<String> malformed = Unirest.delete(tableUrl + "/1")
         .queryString("tableVariant", "not-json")
         .asString();
      assertEquals(400, malformed.getStatus(), malformed.getBody());
      assertThat(malformed.getBody()).contains("tableVariant");
   }



   /*******************************************************************************
    ** Two variant options: people (1) and planets (2).
    *******************************************************************************/
   private void insertVariantOptions() throws QException
   {
      QContext.init(serverQInstance, new QSystemUserSession());
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS).withRecords(List.of(
         new QRecord().withValue("id", 1).withValue("name", "People"),
         new QRecord().withValue("id", 2).withValue("name", "Planets"))));
      QContext.clear();
   }



   /*******************************************************************************
    ** The names of the variant-backed records in one variant.
    *******************************************************************************/
   private List<String> namesInVariant(Integer variantId) throws QException
   {
      QContext.init(serverQInstance, new QSystemUserSession());
      try
      {
         QContext.getQSession().setBackendVariants(Map.of(TestUtils.TABLE_NAME_MEMORY_VARIANT_OPTIONS, variantId));
         return (new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_MEMORY_VARIANT_DATA)).getRecords().stream()
            .map(record -> record.getValueString("name"))
            .toList());
      }
      finally
      {
         QContext.clear();
      }
   }

}
