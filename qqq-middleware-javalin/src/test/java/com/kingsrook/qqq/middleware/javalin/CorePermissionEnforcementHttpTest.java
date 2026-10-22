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

package com.kingsrook.qqq.middleware.javalin;


import java.util.HashSet;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.FieldSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** HTTP-level checks of core permission enforcement (issues #1061 / #1062)
 ** through the legacy Javalin routes, with QInstance.enforcePermissionsInCoreActions
 ** on: allowed users see no change (other than locked fields being omitted),
 ** denied users get the same 403 as before, possible-value lookups and exports
 ** keep working, and filters on a locked field are 403s.
 *******************************************************************************/
class CorePermissionEnforcementHttpTest extends QJavalinTestBase
{

   /*******************************************************************************
    ** put the default server back for other test classes.
    *******************************************************************************/
   @AfterAll
   static void restoreDefaultServer() throws Exception
   {
      restartServerWithInstance(TestUtils.defineInstance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QInstance defineEnforcingInstance()
   {
      QInstance instance = TestUtils.defineInstance();
      instance.setEnforcePermissionsInCoreActions(true);
      instance.addSecurityKeyType(new QSecurityKeyType().withName("clearance"));
      instance.getTable("person").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getTable("person").getField("email").setFieldSecurityLock(new FieldSecurityLock()
         .withSecurityKeyType("clearance")
         .withDefaultBehavior(FieldSecurityLock.Behavior.DENY)
         .withOverrideValues(List.of("pii")));
      return (instance);
   }



   /*******************************************************************************
    ** reader: query/get/count/possible-values work; email is omitted; filtering on
    ** email is denied; writes are denied (as before, by the middleware).
    *******************************************************************************/
   @Test
   void testReaderWithLockedField() throws Exception
   {
      QInstance instance = defineEnforcingInstance();
      instance.getAuthentication().setCustomizer(new QCodeReference(PersonReader.class));
      restartServerWithInstance(instance);

      HttpResponse<String> query = Unirest.get(BASE_URL + "/data/person").asString();
      assertEquals(200, query.getStatus(), query.getBody());
      JSONObject firstRecord = JsonUtils.toJSONObject(query.getBody()).getJSONArray("records").getJSONObject(0);
      assertTrue(firstRecord.getJSONObject("values").has("firstName"));
      assertFalse(firstRecord.getJSONObject("values").has("email"));

      HttpResponse<String> get = Unirest.get(BASE_URL + "/data/person/1").asString();
      assertEquals(200, get.getStatus(), get.getBody());
      assertFalse(JsonUtils.toJSONObject(get.getBody()).getJSONObject("values").has("email"));

      HttpResponse<String> count = Unirest.get(BASE_URL + "/data/person/count").asString();
      assertEquals(200, count.getStatus(), count.getBody());

      HttpResponse<String> filteredOnLockedField = Unirest.get(BASE_URL + "/data/person/count")
         .queryString("filter", "{\"criteria\":[{\"fieldName\":\"email\",\"operator\":\"IS_NOT_BLANK\"}]}").asString();
      assertEquals(403, filteredOnLockedField.getStatus(), filteredOnLockedField.getBody());

      HttpResponse<String> possibleValues = Unirest.get(BASE_URL + "/data/person/possibleValues/partnerPersonId").asString();
      assertEquals(200, possibleValues.getStatus(), possibleValues.getBody());

      HttpResponse<String> export = Unirest.get(BASE_URL + "/data/person/export/people.csv").asString();
      assertEquals(200, export.getStatus(), export.getBody());
      assertThat(export.getBody()).contains("Darin").doesNotContain("Email").doesNotContain("@");

      HttpResponse<String> metaData = Unirest.get(BASE_URL + "/metaData/table/person").asString();
      assertEquals(200, metaData.getStatus(), metaData.getBody());
      assertFalse(JsonUtils.toJSONObject(metaData.getBody()).getJSONObject("table").getJSONObject("fields").has("email"));

      HttpResponse<String> insert = Unirest.post(BASE_URL + "/data/person").body(JsonUtils.toJson(Map.of("firstName", "No", "lastName", "Write"))).asString();
      assertEquals(403, insert.getStatus(), insert.getBody());
   }



   /*******************************************************************************
    ** a user with the override value sees the locked field, and other tables
    ** (without permission rules) are unaffected.
    *******************************************************************************/
   @Test
   void testOverrideValueAndUnprotectedTables() throws Exception
   {
      QInstance instance = defineEnforcingInstance();
      instance.getAuthentication().setCustomizer(new QCodeReference(PersonReaderWithPii.class));
      restartServerWithInstance(instance);

      HttpResponse<String> get = Unirest.get(BASE_URL + "/data/person/1").asString();
      assertEquals(200, get.getStatus(), get.getBody());
      assertTrue(JsonUtils.toJSONObject(get.getBody()).getJSONObject("values").has("email"));

      HttpResponse<String> pets = Unirest.get(BASE_URL + "/data/pet").asString();
      assertEquals(200, pets.getStatus(), pets.getBody());
   }



   /*******************************************************************************
    ** a user without READ gets the same 403 as without core enforcement.
    *******************************************************************************/
   @Test
   void testNoReadIsDenied() throws Exception
   {
      QInstance instance = defineEnforcingInstance();
      instance.getAuthentication().setCustomizer(new QCodeReference(NoPermissions.class));
      restartServerWithInstance(instance);

      assertEquals(403, Unirest.get(BASE_URL + "/data/person").asString().getStatus());
      assertEquals(403, Unirest.get(BASE_URL + "/data/person/1").asString().getStatus());
      assertEquals(403, Unirest.get(BASE_URL + "/data/person/count").asString().getStatus());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PersonReader implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PersonReaderWithPii implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read");
         session.withSecurityKeyValue("clearance", "pii");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class NoPermissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         //////////////////////////////////
         // grant no permissions at all //
         //////////////////////////////////
         session.setPermissions(new HashSet<>());
      }
   }

}
