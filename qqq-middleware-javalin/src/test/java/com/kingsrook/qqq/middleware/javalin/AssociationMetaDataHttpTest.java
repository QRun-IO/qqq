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


import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.RowBuilderWidgetRenderer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Association;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.SchemaBuilder;
import com.kingsrook.qqq.middleware.javalin.specs.v1.MiddlewareVersionV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.WidgetMetaData;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.TableMetaData;
import io.javalin.Javalin;
import kong.unirest.Unirest;
import kong.unirest.UnirestInstance;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Full metadata describes named associations independently of child record access.
 *******************************************************************************/
class AssociationMetaDataHttpTest
{
   private QApplicationJavalinServer server;
   private UnirestInstance client;
   private String base;
   private QInstance instance;



   /*******************************************************************************
    ** One real server serves both legacy and V1 routes on its owned ephemeral port.
    *******************************************************************************/
   private void start() throws Exception
   {
      TestUtils.primeTestDatabase();
      instance = TestUtils.defineInstance();
      QTableMetaData person = instance.getTable("person");
      person.setExposedJoins(List.of());
      person.setAssociations(List.of(
         new Association().withName("dependents").withAssociatedTableName("pet").withJoinName("personJoinPet"),
         new Association().withName("emergencyContacts").withAssociatedTableName("pet").withJoinName("personJoinPet")));
      for(String tableName : List.of("person", "pet"))
      {
         instance.getTable(tableName).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      }
      instance.getAuthentication().setCustomizer(new QCodeReference(ParentReadChildInsert.class));
      instance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(instance.getJoin("personJoinPet"))
         .withName("householdWidget").withManageAssociationName("dependents").getWidgetMetaData());
      instance.addWidget(RowBuilderWidgetRenderer.widgetMetaDataBuilder("contactEditor")
         .withAssociationName("emergencyContacts").withParentTableName("person")
         .withIsForRecordViewAndEditScreen(true).withFields(List.of(instance.getTable("pet").getField("name"))).getWidgetMetaData());
      person.withSection(new QFieldSection().withName("identity").withLabel("Identity").withTier(Tier.T1).withFieldNames(List.copyOf(person.getFields().keySet())));
      person.withSection(new QFieldSection().withName("householdSection").withLabel("Household").withTier(Tier.T2).withWidgetName("householdWidget"));
      person.withSection(new QFieldSection().withName("contactsSection").withLabel("Contacts").withTier(Tier.T2).withWidgetName("contactEditor"));
      AtomicReference<Javalin> service = new AtomicReference<>();
      server = new QApplicationJavalinServer(new AbstractQQQApplication()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public QInstance defineQInstance()
         {
            return instance;
         }
      }).withPort(0).withServeFrontendMaterialDashboard(false)
         .withMiddlewareVersionList(List.of(new MiddlewareVersionV1())).withJavalinConfigurationCustomizer(service::set);
      server.start();
      base = "http://localhost:" + service.get().port();
      client = Unirest.spawnInstance();
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
      QContext.clear();
   }



   /*******************************************************************************
    ** Exact names survive even when both groups share a target and the same join.
    *******************************************************************************/
   @Test
   void testFullMetadataKeepsAliasesWithoutGrantingChildRead() throws Exception
   {
      start();
      String canonical = JsonUtils.toJson(instance.getTable("person").getAssociations());
      assertAll(() -> assertFullMetadata(""), () -> assertFullMetadata("/qqq/v1"));
      assertEquals(canonical, JsonUtils.toJson(instance.getTable("person").getAssociations()));
      assertEquals(403, client.get(base + "/data/pet/1").asString().getStatus());
      assertEquals(403, client.get(base + "/data/person/1").queryString("includeAssociations", true).asString().getStatus());
   }



   /*******************************************************************************
    ** Widget defaults preserve the existing explicit section-to-association binding.
    *******************************************************************************/
   @Test
   void testWidgetAssociationBindingsRemainInMetadata() throws Exception
   {
      start();
      assertAll(() -> assertWidgetBindings(""), () -> assertWidgetBindings("/qqq/v1"));
   }



   /*******************************************************************************
    ** Metadata follows the declared hide/show policy; neither response grants READ.
    *******************************************************************************/
   @Test
   void testDeniedMetadataFollowsExistingVisibilityPolicy() throws Exception
   {
      start();
      QPermissionRules rules = instance.getTable("pet").getPermissionRules();
      rules.setPermissionBaseName("deniedPet");
      rules.setDenyBehavior(DenyBehavior.HIDDEN);
      for(String prefix : List.of("", "/qqq/v1"))
      {
         var response = client.get(base + prefix + "/metaData/table/pet").asString();
         assertEquals(404, response.getStatus());
         assertEquals("Table [pet] was not found.", new JSONObject(response.getBody()).getString("error"));
         assertFalse(get(prefix + "/metaData").getJSONObject("tables").has("pet"));
      }

      rules.setDenyBehavior(DenyBehavior.DISABLED);
      for(String prefix : List.of("", "/qqq/v1"))
      {
         JSONObject pet = table(prefix, "pet");
         assertFalse(pet.getBoolean("readPermission"));
         assertFalse(pet.getBoolean("insertPermission"));
         assertFalse(pet.getBoolean("editPermission"));
         assertFalse(pet.getBoolean("deletePermission"));
      }
      assertEquals(403, client.get(base + "/data/pet/1").asString().getStatus());
   }



   /*******************************************************************************
    ** The published schema uses the same shallow descriptor and existing join shape.
    *******************************************************************************/
   @Test
   void testAssociationAndWidgetSchemas()
   {
      JSONObject tableSchema = new JSONObject(JsonUtils.toJson(new TableMetaData().toSchema()));
      assertEquals("#/components/schemas/Association", tableSchema.getJSONObject("properties")
         .getJSONObject("associations").getJSONObject("items").getString("$ref"));
      JSONObject association = new JSONObject(JsonUtils.toJson(new com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.Association().toSchema()));
      assertThat(association.getJSONObject("properties").keySet()).containsExactlyInAnyOrder("name", "associatedTableName", "join");
      assertTrue(association.getJSONObject("properties").getJSONObject("join").getJSONObject("properties").has("joinOns"));
      JSONObject widget = new JSONObject(JsonUtils.toJson(new SchemaBuilder().classToSchema(WidgetMetaData.class)));
      assertTrue(widget.getJSONObject("properties").getJSONObject("defaultValues").getBoolean("additionalProperties"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertFullMetadata(String prefix)
   {
      JSONObject person = table(prefix, "person");
      JSONArray associations = person.getJSONArray("associations");
      assertEquals(2, associations.length());
      for(int i = 0; i < associations.length(); i++)
      {
         JSONObject association = associations.getJSONObject(i);
         assertThat(association.keySet()).containsExactlyInAnyOrder("name", "associatedTableName", "join");
         assertEquals(List.of("dependents", "emergencyContacts").get(i), association.getString("name"));
         assertEquals("pet", association.getString("associatedTableName"));
         JSONObject join = association.getJSONObject("join");
         assertEquals("personJoinPet", join.getString("name"));
         assertEquals("person", join.getString("leftTable"));
         assertEquals("pet", join.getString("rightTable"));
         assertEquals("id", join.getJSONArray("joinOns").getJSONObject(0).getString("leftField"));
         assertEquals("ownerPersonId", join.getJSONArray("joinOns").getJSONObject(0).getString("rightField"));
      }
      assertTrue(person.isNull("exposedJoins") || person.getJSONArray("exposedJoins").isEmpty());
      JSONObject pet = table(prefix, "pet");
      assertFalse(pet.getBoolean("readPermission"));
      assertTrue(pet.getBoolean("insertPermission"));
      assertTrue(pet.getJSONObject("fields").has("name"));
      JSONObject lightPerson = get(prefix + "/metaData").getJSONObject("tables").getJSONObject("person");
      assertTrue(lightPerson.isNull("associations"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertWidgetBindings(String prefix)
   {
      JSONObject widgets = get(prefix + "/metaData").getJSONObject("widgets");
      JSONObject household = widgets.getJSONObject("householdWidget");
      assertEquals("dependents", household.getJSONObject("defaultValues").getString("manageAssociationName"));
      assertEquals("personJoinPet", household.getJSONObject("defaultValues").getString("joinName"));
      assertFalse(household.has("codeReference"));
      assertEquals("emergencyContacts", widgets.getJSONObject("contactEditor").getJSONObject("defaultValues").getString("associationName"));
      JSONArray sections = table(prefix, "person").getJSONArray("sections");
      assertThat(sections.toList()).anySatisfy(section ->
      {
         assertEquals("householdSection", ((Map<?, ?>) section).get("name"));
         assertEquals("householdWidget", ((Map<?, ?>) section).get("widgetName"));
      }).anySatisfy(section ->
      {
         assertEquals("contactsSection", ((Map<?, ?>) section).get("name"));
         assertEquals("contactEditor", ((Map<?, ?>) section).get("widgetName"));
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject table(String prefix, String name)
   {
      JSONObject body = get(prefix + "/metaData/table/" + name);
      return prefix.isEmpty() ? body.getJSONObject("table") : body;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject get(String path)
   {
      var response = client.get(base + path).asString();
      assertEquals(200, response.getStatus(), response.getBody());
      return new JSONObject(response.getBody());
   }



   /*******************************************************************************
    ** Metadata and INSERT capability remain available while child READ is denied.
    *******************************************************************************/
   public static class ParentReadChildInsert implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read", "pet.insert");
      }
   }
}
