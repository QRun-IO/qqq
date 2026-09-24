/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.List;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.processes.implementations.mock.MockBackendStep;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ProcessInitSpecV1 
 *******************************************************************************/
class ProcessInitSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ProcessInitSpecV1();
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
      QLogger.deactivateCollectingLoggerForClass(MockBackendStep.class);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetInitialRecordsFromRecordIdsParam()
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(MockBackendStep.class);

      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .multiPartContent()
         .field("recordsParam", "recordIds")
         .field("recordIds", "2,3")
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals("COMPLETE", jsonObject.getString("type"));
      assertEquals("null X null", jsonObject.getJSONObject("values").getString("outputMessage")); // these nulls are because we didn't pass values for some fields.

      assertThat(collectingLogger.getCollectedMessages())
         .filteredOn(clm -> clm.getMessage().contains("We are mocking"))
         .hasSize(2);
      // todo - also request records
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetInitialRecordsFromFilterParam()
   {
      QCollectingLogger collectingLogger = QLogger.activateCollectingLoggerForClass(MockBackendStep.class);

      QQueryFilter queryFilter = new QQueryFilter()
         .withCriteria(new QFilterCriteria()
            .withFieldName("id")
            .withOperator(QCriteriaOperator.IN)
            .withValues(List.of(3, 4, 5)));
      String filterJSON = JsonUtils.toJson(queryFilter);

      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .multiPartContent()
         .field("recordsParam", "filterJSON")
         .field("filterJSON", filterJSON)
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals("COMPLETE", jsonObject.getString("type"));
      assertEquals("null X null", jsonObject.getJSONObject("values").getString("outputMessage"));

      assertThat(collectingLogger.getCollectedMessages())
         .filteredOn(clm -> clm.getMessage().contains("We are mocking"))
         .hasSize(3);
      // todo - also request records
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRequiresRowsButNotSpecified()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .multiPartContent()
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals("ERROR", jsonObject.getString("type"));
      assertTrue(jsonObject.has("error"));
      assertTrue(jsonObject.getString("error").contains("Missing input records"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldValues()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .multiPartContent()
         .field("recordsParam", "recordIds")
         .field("recordIds", "2,3")
         .field("values", new JSONObject()
            .put("greetingPrefix", "Hey")
            .put("greetingSuffix", "Jude")
            .toString()
         )
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertEquals("COMPLETE", jsonObject.getString("type"));
      assertEquals("Hey X Jude", jsonObject.getJSONObject("values").getString("outputMessage"));
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInitGoingAsync()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/" + TestUtils.PROCESS_NAME_SIMPLE_SLEEP + "/init")
         .multiPartContent()
         .field("stepTimeoutMillis", "50")
         .field("values", new JSONObject()
            .put(TestUtils.SleeperStep.FIELD_SLEEP_MILLIS, 500)
            .toString()
         )
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      String     processUUID = jsonObject.getString("processUUID");
      String     jobUUID     = jsonObject.getString("jobUUID");
      assertNotNull(processUUID, "Process UUID should not be null.");
      assertNotNull(jobUUID, "Job UUID should not be null");

      // todo - in a higher-level test, resume test_processInitGoingAsync at the // request job status before sleep is done // line
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testProcessPutsNullKeyInMap()
   {
      HttpResponse<String> response = Unirest.post(getBaseUrlAndPath() + "/processes/" + TestUtils.PROCESS_NAME_PUTS_NULL_KEY_IN_MAP + "/init")
         .multiPartContent()
         .asString();

      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());

      JSONObject values = jsonObject.getJSONObject("values");
      JSONObject mapWithNullKey = values.getJSONObject("mapWithNullKey");
      assertTrue(mapWithNullKey.has("")); // null key currently set to become empty-string key...
      assertEquals("hadNullKey", mapWithNullKey.getString(""));
      assertTrue(mapWithNullKey.has("one"));
      assertEquals("1", mapWithNullKey.getString("one"));
   }

}