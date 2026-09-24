/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Actual legacy form writes must authorize their child operations before DML.
 *******************************************************************************/
class AssociatedWriteHttpTest extends QJavalinTestBase
{
   private static List<String> permissions = List.of();
   private UnirestInstance client;



   /*******************************************************************************
    ** Each test owns its HTTP pool because the fixture restarts the server.
    *******************************************************************************/
   @AfterEach
   void closeClient()
   {
      if(client != null)
      {
         client.close();
      }
   }



   /*******************************************************************************
    ** Denied child INSERT must not leave the already-inserted parent behind.
    *******************************************************************************/
   @Test
   void testInsertDeniedBeforeParentMutation() throws Exception
   {
      startWithPermissions("person.insert", "pet.read");
      String before = snapshot();
      HttpResponse<String> response = client.post(BASE_URL + "/data/person")
         .field("firstName", "New").field("lastName", "Parent").field("email", "parent@example.test")
         .field("associations", "{\"pets\":[{\"name\":\"New child\",\"species\":\"dog\"}]}").asString();
      assertDeniedAndUnchanged(response, before);
   }



   /*******************************************************************************
    ** Keeping both existing children requires EDIT, but no DELETE.
    *******************************************************************************/
   @Test
   void testUpdateDeniedBeforeParentMutation() throws Exception
   {
      startWithPermissions("person.edit", "pet.read");
      String before = snapshot();
      assertDeniedAndUnchanged(update("[{\"id\":1,\"name\":\"Changed\"},{\"id\":2}]"), before);
   }



   /*******************************************************************************
    ** A mixed replacement set must also authorize its new child INSERT.
    *******************************************************************************/
   @Test
   void testUpdateInsertDeniedBeforeParentMutation() throws Exception
   {
      startWithPermissions("person.edit", "pet.edit", "pet.delete");
      String before = snapshot();
      assertDeniedAndUnchanged(update("[{\"id\":1},{\"id\":2},{\"name\":\"New child\",\"species\":\"dog\"}]"), before);
   }



   /*******************************************************************************
    ** Omitted rows and an empty replacement set both perform real DELETEs.
    *******************************************************************************/
   @Test
   void testUpdateDeleteDeniedBeforeParentMutation() throws Exception
   {
      startWithPermissions("person.edit", "pet.edit", "pet.insert");
      String before = snapshot();
      assertDeniedAndUnchanged(update("[{\"id\":1}]"), before);
      assertDeniedAndUnchanged(update("[]"), before);
   }



   /*******************************************************************************
    ** Parent DELETE may not cascade into a denied child table.
    *******************************************************************************/
   @Test
   void testDeleteDeniedBeforeParentMutation() throws Exception
   {
      startWithPermissions("person.delete", "pet.read");
      String before = snapshot();
      assertDeniedAndUnchanged(client.delete(BASE_URL + "/data/person/1").asString(), before);
   }



   /*******************************************************************************
    ** Prospective parent changes can make previously hidden children deletable.
    *******************************************************************************/
   @Test
   void testParentUpdateCannotRevealUncheckedDeletes() throws Exception
   {
      startWithPermissions("person.edit");
      QInstance instance = TestUtils.defineInstance();
      for(String table : List.of("person", "pet"))
      {
         instance.getTable(table).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      }
      instance.addSecurityKeyType(new QSecurityKeyType().withName("ownerName"));
      instance.getTable("pet").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("ownerName")
         .withFieldName("person.firstName").withJoinNameChain(List.of("personJoinPet")));
      instance.getAuthentication().setCustomizer(new QCodeReference(WritePermissions.class));
      restartServerWithInstance(instance);
      String before = snapshot();
      assertDeniedAndUnchanged(update("[]"), before);
   }



   /*******************************************************************************
    ** Discovery needs no READ grant; retaining every child needs no DELETE grant.
    *******************************************************************************/
   @Test
   void testUpdateWithoutChildReadOrDelete() throws Exception
   {
      startWithPermissions("person.edit", "pet.edit");
      assertEquals(403, client.get(BASE_URL + "/data/pet").asString().getStatus());
      HttpResponse<String> response = update("[{\"id\":1,\"name\":\"Changed\"},{\"id\":2}]");
      assertEquals(200, response.getStatus(), response.getBody());
      assertEquals("Changed", scalar("SELECT name FROM pet WHERE id=1"));
      assertEquals("2", scalar("SELECT COUNT(*) FROM pet"));
   }



   /*******************************************************************************
    ** A missing association key does not request any child operation.
    *******************************************************************************/
   @Test
   void testParentOnlyUpdate() throws Exception
   {
      startWithPermissions("person.edit");
      HttpResponse<String> response = client.patch(BASE_URL + "/data/person/1").field("firstName", "Changed").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      assertEquals("Changed", scalar("SELECT first_name FROM person WHERE id=1"));
      assertEquals("2", scalar("SELECT COUNT(*) FROM pet"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void startWithPermissions(String... granted) throws Exception
   {
      permissions = List.of(granted);
      QInstance instance = TestUtils.defineInstance();
      for(String table : List.of("person", "pet"))
      {
         instance.getTable(table).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      }
      instance.getAuthentication().setCustomizer(new QCodeReference(WritePermissions.class));
      restartServerWithInstance(instance);
      client = Unirest.spawnInstance();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> update(String pets)
   {
      return client.patch(BASE_URL + "/data/person/1").field("firstName", "Changed")
         .field("associations", "{\"pets\":" + pets + "}").asString();
   }



   /*******************************************************************************
    ** Check storage independently of the HTTP status, so partial writes surface.
    *******************************************************************************/
   private void assertDeniedAndUnchanged(HttpResponse<String> response, String before) throws Exception
   {
      String after = snapshot();
      assertAll(() -> assertEquals(403, response.getStatus(), response.getBody()),
         () -> assertEquals("Permission denied.", JsonUtils.toJSONObject(response.getBody()).optString("error")),
         () -> assertEquals(before, after));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String snapshot() throws Exception
   {
      return rows("SELECT id, first_name, last_name FROM person ORDER BY id") + rows("SELECT id, name, species, owner_person_id FROM pet ORDER BY id");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String rows(String sql) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineDefaultH2Backend());
         Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               row.add(result.getString(column));
            }
            rows.add(row);
         }
      }
      return rows.toString();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String scalar(String sql) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(TestUtils.defineDefaultH2Backend());
         Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         result.next();
         return result.getString(1);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class WritePermissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions(permissions.toArray(String[]::new));
         session.withSecurityKeyValue("ownerName", "Changed");
      }
   }
}
