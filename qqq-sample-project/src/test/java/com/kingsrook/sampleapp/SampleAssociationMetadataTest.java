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

package com.kingsrook.sampleapp;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Canonical sample associations are metadata identities, not exposed query joins.
 *******************************************************************************/
public class SampleAssociationMetadataTest
{
   private static boolean allowPetRead;
   private SampleJavalinServer server;
   private HttpClient client;
   private URI base;
   private QInstance instance;



   /*******************************************************************************
    ** Keep the bundled Person association and sections exactly as authored.
    *******************************************************************************/
   private void start(boolean childRead) throws Exception
   {
      allowPetRead = childRead;
      instance = SampleMetaDataProvider.defineTestInstance();
      for(String tableName : List.of("person", "pet"))
      {
         instance.getTable(tableName).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      }
      instance.getAuthentication().setCustomizer(new QCodeReference(MetadataPermissions.class));
      server = new SampleJavalinServer(new SampleMetaDataProvider()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            return instance;
         }
      });
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.withPort(0).withMiddlewareVersionList(List.of(new MiddlewareVersionV1())).withJavalinConfigurationCustomizer(service::set);
      server.start();
      client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
      base = URI.create("http://localhost:" + service.get().port());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void stop()
   {
      if(client != null)
      {
         client.close();
      }
      if(server != null)
      {
         server.stop();
      }
      allowPetRead = false;
      QContext.clear();
   }



   /*******************************************************************************
    ** The canonical pets group has data or a successful empty list under that name.
    *******************************************************************************/
   @Test
   void testCanonicalPetsMetadataAndSuccessfulGroups() throws Exception
   {
      start(true);
      assertCanonicalMetadata();
      JSONObject parent = get("/data/person/1", 200);
      assertFalse(parent.has("associatedRecords"));
      JSONObject populated = get("/data/person/1?includeAssociations=true", 200).getJSONObject("associatedRecords");
      assertEquals(4, populated.getJSONArray("pets").length());
      assertTrue(populated.getJSONArray("pets").toList().stream().anyMatch(value ->
         "Charlie".equals(((Map<?, ?>) ((Map<?, ?>) value).get("values")).get("name"))));
      assertFalse(populated.has("pet"));
      JSONObject empty = get("/data/person/4?includeAssociations=true", 200).getJSONObject("associatedRecords");
      assertTrue(empty.getJSONArray("pets").isEmpty());
   }



   /*******************************************************************************
    ** Child metadata and INSERT remain available while related reads stay denied.
    *******************************************************************************/
   @Test
   void testDeniedChildReadDoesNotRemoveAssociationMetadata() throws Exception
   {
      start(false);
      assertCanonicalMetadata();
      for(String prefix : List.of("", "/qqq/v1"))
      {
         JSONObject child = get(prefix + "/metaData/table/pet", 200);
         child = prefix.isEmpty() ? child.getJSONObject("table") : child;
         assertFalse(child.getBoolean("readPermission"));
         assertTrue(child.getBoolean("insertPermission"));
         assertTrue(child.getJSONObject("fields").has("personId"));
      }
      get("/data/pet/1", 403);
      JSONObject denied = get("/data/person/1?includeAssociations=true", 403);
      assertFalse(denied.has("associatedRecords"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertCanonicalMetadata() throws Exception
   {
      assertTrue(CollectionUtils.nullSafeIsEmpty(instance.getTable("person").getExposedJoins()));
      assertEquals(1, instance.getTable("person").getAssociations().size());
      for(String prefix : List.of("", "/qqq/v1"))
      {
         JSONObject table = get(prefix + "/metaData/table/person", 200);
         table = prefix.isEmpty() ? table.getJSONObject("table") : table;
         JSONArray associations = table.getJSONArray("associations");
         assertEquals(1, associations.length());
         JSONObject association = associations.getJSONObject(0);
         assertEquals("pets", association.getString("name"));
         assertEquals("pet", association.getString("associatedTableName"));
         JSONObject join = association.getJSONObject("join");
         assertEquals("personJoinPet", join.getString("name"));
         assertEquals("person", join.getString("leftTable"));
         assertEquals("pet", join.getString("rightTable"));
         assertEquals("id", join.getJSONArray("joinOns").getJSONObject(0).getString("leftField"));
         assertEquals("personId", join.getJSONArray("joinOns").getJSONObject(0).getString("rightField"));
         assertTrue(table.isNull("exposedJoins") || table.getJSONArray("exposedJoins").isEmpty());
         assertTrue(get(prefix + "/metaData", 200).getJSONObject("tables").getJSONObject("person").isNull("associations"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject get(String path, int status) throws Exception
   {
      HttpResponse<String> response = client.send(HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(status, response.statusCode(), response.body());
      return new JSONObject(response.body());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class MetadataPermissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read", "pet.insert");
         if(allowPetRead)
         {
            session.withPermission("pet.read");
         }
      }
   }
}
