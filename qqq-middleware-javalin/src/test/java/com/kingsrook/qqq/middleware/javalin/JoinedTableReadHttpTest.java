/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin;


import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Joined values require the same READ permission as direct table access.
 *******************************************************************************/
class JoinedTableReadHttpTest extends QJavalinTestBase
{
   /*******************************************************************************
    ** Getting an allowed parent must not expose an unreadable child's fields.
    *******************************************************************************/
   @Test
   void testGetChecksJoinedReadPermission() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      instance.getTable("person").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getTable("pet").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getAuthentication().setCustomizer(new QCodeReference(PersonReadPermission.class));
      restartServerWithInstance(instance);

      assertEquals(200, Unirest.get(BASE_URL + "/data/person/1").asString().getStatus());
      assertEquals(403, Unirest.get(BASE_URL + "/data/pet/1").asString().getStatus());
      String joins = "[{\"joinTable\":\"pet\",\"alias\":\"animal\",\"select\":true,\"type\":\"LEFT\"}]";
      HttpResponse<String> denied = Unirest.get(BASE_URL + "/data/person/1").queryString("queryJoins", joins).asString();
      assertEquals(403, denied.getStatus(), denied.getBody());
      JSONObject body = JsonUtils.toJSONObject(denied.getBody());
      assertEquals("Permission denied.", body.getString("error"));
      assertFalse(body.has("values"));
      HttpResponse<String> deniedAssociations = Unirest.get(BASE_URL + "/data/person/1").queryString("includeAssociations", true).asString();
      assertEquals(403, deniedAssociations.getStatus(), deniedAssociations.getBody());
      assertFalse(JsonUtils.toJSONObject(deniedAssociations.getBody()).has("associatedRecords"));

      instance.getAuthentication().setCustomizer(new QCodeReference(PersonAndPetReadPermission.class));
      restartServerWithInstance(instance);
      HttpResponse<String> allowed = Unirest.get(BASE_URL + "/data/person/1").queryString("queryJoins", joins).asString();
      assertEquals(200, allowed.getStatus(), allowed.getBody());
      assertEquals("dog", JsonUtils.toJSONObject(allowed.getBody()).getJSONObject("values").getString("animal.species"));
      HttpResponse<String> allowedAssociations = Unirest.get(BASE_URL + "/data/person/1").queryString("includeAssociations", true).asString();
      assertEquals(200, allowedAssociations.getStatus(), allowedAssociations.getBody());
      assertEquals(2, JsonUtils.toJSONObject(allowedAssociations.getBody()).getJSONObject("associatedRecords").getJSONArray("pets").length());
   }



   /*******************************************************************************
    ** Legacy HTTP Count and Query use the same USER personalization boundary as V1.
    *******************************************************************************/
   @Test
   void testLegacyReadsUseUserPersonalization() throws Exception
   {
      QInstance instance = TestUtils.defineInstance();
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE,
         new QCodeReference(HideBirthDateForUser.class));
      restartServerWithInstance(instance);
      HttpResponse<String> query = Unirest.get(BASE_URL + "/data/person").asString();
      assertEquals(200, query.getStatus(), query.getBody());
      JSONObject firstRecord = JsonUtils.toJSONObject(query.getBody()).getJSONArray("records").getJSONObject(0);
      assertFalse(firstRecord.getJSONObject("values").has("birthDate"));

      HttpResponse<String> filteredCount = Unirest.get(BASE_URL + "/data/person/count")
         .queryString("filter", "{\"criteria\":[{\"fieldName\":\"birthDate\",\"operator\":\"IS_NOT_BLANK\"}]}").asString();
      assertEquals(500, filteredCount.getStatus(), filteredCount.getBody());
      assertFalse(JsonUtils.toJSONObject(filteredCount.getBody()).has("count"));
      assertEquals(6, JsonUtils.toJSONObject(Unirest.get(BASE_URL + "/data/person/count").asString().getBody()).getInt("count"));
   }



   /*******************************************************************************
    ** Write permission deliberately does not grant READ.
    *******************************************************************************/
   public static class PersonReadPermission implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read", "pet.write");
      }
   }



   /*******************************************************************************
    ** Grant both tables for the allowed-path control.
    *******************************************************************************/
   public static class PersonAndPetReadPermission implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read", "pet.read");
      }
   }



   /*******************************************************************************
    ** Preserve the field for trusted code while removing it from USER reads.
    *******************************************************************************/
   public static class HideBirthDateForUser implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         QTableMetaData table = input.getTable();
         if("person".equals(table.getName()) && QInputSource.USER.equals(input.getInputSource()))
         {
            table = table.clone();
            table.getFields().remove("birthDate");
         }
         return table;
      }
   }
}
