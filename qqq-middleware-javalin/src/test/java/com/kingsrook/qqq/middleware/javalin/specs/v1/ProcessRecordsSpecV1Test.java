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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.List;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Unit test for ProcessRecordsSpecV1
 *******************************************************************************/
class ProcessRecordsSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new ProcessRecordsSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return List.of(new ProcessInitSpecV1());
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
    ** Test retrieving process records after running a process that produces records.
    *******************************************************************************/
   @Test
   void testProcessRecords()
   {
      ///////////////////////////////////////////////////////////////////////////
      // first, init a process that will produce records in its process state. //
      // the "greet" process operates on the person table.                     //
      ///////////////////////////////////////////////////////////////////////////
      HttpResponse<String> initResponse = Unirest.post(getBaseUrlAndPath() + "/processes/greet/init")
         .multiPartContent()
         .field("recordsParam", "recordIds")
         .field("recordIds", "1,2,3")
         .asString();

      assertEquals(200, initResponse.getStatus());
      JSONObject initJson    = JsonUtils.toJSONObject(initResponse.getBody());
      String     processUUID = initJson.getString("processUUID");
      assertNotNull(processUUID);

      /////////////////////////////////////////////////////////////////////////
      // now fetch the records from that process state via the records spec. //
      /////////////////////////////////////////////////////////////////////////
      HttpResponse<String> recordsResponse = Unirest.get(getBaseUrlAndPath() + "/processes/greet/" + processUUID + "/records")
         .asString();

      assertEquals(200, recordsResponse.getStatus());
      JSONObject recordsJson = JsonUtils.toJSONObject(recordsResponse.getBody());
      assertNotNull(recordsJson);

      JSONArray records = recordsJson.getJSONArray("records");
      assertThat(records.length()).isGreaterThanOrEqualTo(1);

      Integer totalRecords = recordsJson.getInt("totalRecords");
      assertThat(totalRecords).isGreaterThanOrEqualTo(1);
   }



   /*******************************************************************************
    ** Test that requesting records for a non-existent process UUID returns error.
    *******************************************************************************/
   @Test
   void testProcessNotFound()
   {
      String fakeUUID = UUID.randomUUID().toString();

      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/processes/greet/" + fakeUUID + "/records")
         .asString();

      assertEquals(500, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.getString("error")).contains("Could not find process results");
   }

}
