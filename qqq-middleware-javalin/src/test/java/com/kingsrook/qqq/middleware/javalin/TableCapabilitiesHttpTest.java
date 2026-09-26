/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin;


import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Count and export honor the table's declared capabilities over HTTP.
 *******************************************************************************/
class TableCapabilitiesHttpTest extends QJavalinTestBase
{

   /*******************************************************************************
    ** A table without TABLE_COUNT / TABLE_EXPORT refuses both; its queries still work.
    *******************************************************************************/
   @Test
   void testDisabledCountAndExportAreRefused() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      instance.getTable(TestUtils.TABLE_NAME_PERSON).withoutCapability(Capability.TABLE_COUNT).withoutCapability(Capability.TABLE_EXPORT);
      restartServerWithInstance(instance);

      HttpResponse<String> count = Unirest.get(BASE_URL + "/data/person/count").asString();
      assertEquals(403, count.getStatus(), count.getBody());
      assertFalse(JsonUtils.toJSONObject(count.getBody()).has("count"));

      HttpResponse<String> export = Unirest.post(BASE_URL + "/data/person/export/people.csv").field("fields", "id,firstName").asString();
      assertEquals(403, export.getStatus(), export.getBody());
      assertFalse(export.getBody().contains("firstName"));

      HttpResponse<String> query = Unirest.get(BASE_URL + "/data/person").asString();
      assertEquals(200, query.getStatus(), query.getBody());
   }



   /*******************************************************************************
    ** With the capabilities (the default), count and export work.
    *******************************************************************************/
   @Test
   void testEnabledCountAndExportWork() throws Exception
   {
      restartServerWithInstance(TestUtils.defineInstance());

      HttpResponse<String> count = Unirest.get(BASE_URL + "/data/person/count").asString();
      assertEquals(200, count.getStatus(), count.getBody());
      assertTrue(JsonUtils.toJSONObject(count.getBody()).getInt("count") > 0);

      HttpResponse<String> export = Unirest.post(BASE_URL + "/data/person/export/people.csv").field("fields", "id,firstName").asString();
      assertEquals(200, export.getStatus(), export.getBody());
      assertTrue(export.getBody().startsWith("\"Id\",\"First Name\""), export.getBody());
   }
}
