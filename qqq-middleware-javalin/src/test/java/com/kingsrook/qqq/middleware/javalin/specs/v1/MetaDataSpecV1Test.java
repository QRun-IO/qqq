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


import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for MetaDataSpecV1 
 *******************************************************************************/
class MetaDataSpecV1Test extends SpecTestBase
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new MetaDataSpecV1();
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
   @Test
   void test()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/metaData").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertThat(jsonObject.getJSONObject("tables").length()).isGreaterThanOrEqualTo(1);
      assertThat(jsonObject.getJSONObject("processes").length()).isGreaterThanOrEqualTo(1);
      assertThat(jsonObject.getJSONObject("apps").length()).isGreaterThanOrEqualTo(1);
      assertThat(jsonObject.getJSONArray("appTree").length()).isGreaterThanOrEqualTo(1);

      ////////////////////////////////////////////////////////////////////////
      // widgets carry their full frontend meta-data (including permission) //
      // and reports are listed with what a frontend needs to run them      //
      ////////////////////////////////////////////////////////////////////////
      JSONObject timezoneWidget = jsonObject.getJSONObject("widgets").getJSONObject("timezoneWidget");
      assertTrue(timezoneWidget.getBoolean("hasPermission"));
      assertTrue(timezoneWidget.has("isCard"));
      JSONObject personsReport = jsonObject.getJSONObject("reports").getJSONObject("personsReport");
      assertEquals("personsReport", personsReport.getString("name"));
      assertTrue(personsReport.getBoolean("hasPermission"));
   }



   /*******************************************************************************
    ** Tables advertise their search fields, only to sessions that may read them.
    *******************************************************************************/
   @Test
   void testSearchFields()
   {
      serverQInstance.getTable(TestUtils.TABLE_NAME_PERSON).withSearchFields("firstName", "lastName");

      JSONObject tables = JsonUtils.toJSONObject(Unirest.get(getBaseUrlAndPath() + "/metaData").asString().getBody()).getJSONObject("tables");
      assertThat(tables.getJSONObject(TestUtils.TABLE_NAME_PERSON).getJSONArray("searchFields").toList()).containsExactly("firstName", "lastName");
      assertThat(tables.getJSONObject(TestUtils.TABLE_NAME_PET).has("searchFields")).isFalse();

      serverQInstance.getTable(TestUtils.TABLE_NAME_PERSON).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      tables = JsonUtils.toJSONObject(Unirest.get(getBaseUrlAndPath() + "/metaData").asString().getBody()).getJSONObject("tables");
      assertThat(tables.optJSONObject(TestUtils.TABLE_NAME_PERSON) == null || !tables.getJSONObject(TestUtils.TABLE_NAME_PERSON).has("searchFields")).isTrue();
   }

}
