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

package com.kingsrook.sampleapp;


import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.metadata.TableMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.DenyBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Presentation metadata uses the canonical sample and owned HTTP routes.
 *******************************************************************************/
class SamplePresentationMetadataTest
{
   private QInstance instance;
   private SampleJavalinServer server;
   private HttpClient client;
   private URI base;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
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
    ** Both metadata routes carry the same canonical hierarchy and presentation.
    *******************************************************************************/
   @Test
   void testCanonicalNavigationBrandingAndSections() throws Exception
   {
      start();
      for(String prefix : List.of("", "/qqq/v1"))
      {
         JSONObject metadata = json(prefix + "/metaData");
         if(prefix.isEmpty())
         {
            assertEquals("QQQ Sample", metadata.getJSONObject("branding").getString("appName"));
            assertEquals("/samples-logo.png", metadata.getJSONObject("branding").getString("logo"));
         }
         JSONObject people = named(metadata.getJSONArray("appTree"), "peopleApp");
         assertEquals("People App", people.getString("label"));
         assertEquals("person", people.getJSONObject("icon").getString("name"));
         JSONObject greetings = named(people.getJSONArray("children"), "greetingsApp");
         assertEquals("Greetings App", greetings.getString("label"));
         assertEquals("person", named(greetings.getJSONArray("children"), "person").getString("name"));
         JSONObject table = table(prefix, "person");
         JSONObject identity = named(table.getJSONArray("sections"), "identity");
         assertEquals("Identity", identity.getString("label"));
         assertEquals("badge", identity.getJSONObject("icon").getString("name"));
         assertEquals(List.of("id", "firstName", "lastName"), identity.getJSONArray("fieldNames").toList());
      }
      JSONObject record = json("/data/person/1");
      JSONObject values = record.getJSONObject("values");
      assertEquals(values.getString("firstName") + " " + values.getString("lastName"), record.getString("recordLabel"));
      assertEquals(200, request("/samples-logo.png").statusCode());
      assertEquals(200, request("/kr-icon.png").statusCode());
   }



   /*******************************************************************************
    ** Hidden objects disappear; disabled objects expose no usable CRUD operation.
    *******************************************************************************/
   @Test
   void testDeniedAndHiddenMetadata() throws Exception
   {
      instance.getTable("carrier").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      instance.getProcess("greet").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      instance.getApp("miscellaneous").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      instance.getTable("pet").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS).withDenyBehavior(DenyBehavior.DISABLED));
      instance.getTable("person").getField("daysWorked").setIsHidden(true);
      instance.getTable("person").getSections().forEach(section -> section.setFieldNames(section.getFieldNames().stream()
         .filter(name -> !name.equals("daysWorked")).toList()));
      start();
      for(String prefix : List.of("", "/qqq/v1"))
      {
         JSONObject metadata = json(prefix + "/metaData");
         assertFalse(metadata.getJSONObject("tables").has("carrier"));
         assertFalse(metadata.getJSONObject("processes").has("greet"));
         assertFalse(metadata.getJSONObject("apps").has("miscellaneous"));
         assertFalse(metadata.getJSONArray("appTree").toString().contains("\"miscellaneous\""));
         assertFalse(named(named(metadata.getJSONArray("appTree"), "peopleApp").getJSONArray("children"), "greetingsApp")
            .getJSONArray("children").toString().contains("\"greet\""));
         assertEquals(404, request(prefix + "/metaData/table/carrier").statusCode());
         JSONObject disabled = table(prefix, "pet");
         for(String permission : List.of("readPermission", "insertPermission", "editPermission", "deletePermission"))
         {
            assertFalse(disabled.getBoolean(permission));
         }
         assertFalse(table(prefix, "person").getJSONObject("fields").has("daysWorked"));
      }
      assertEquals(403, request("/data/pet/1").statusCode());
      assertEquals(403, request("/data/carrier/1").statusCode());
   }



   /*******************************************************************************
    ** A personalized section order must not leak into another session or SYSTEM.
    *******************************************************************************/
   @Test
   void testPersonalizedFieldOrderIsSessionScoped() throws Exception
   {
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(PresentationPersonalizer.class));
      new QInstanceValidator().validate(instance);
      QContext.init(instance, new QSession().withPermissions("sample.personalized"));
      TableMetaDataInput input = new TableMetaDataInput();
      input.setTableName("person");
      input.setInputSource(QInputSource.USER);
      QFrontendTableMetaData table = new TableMetaDataAction().execute(input).getTable();
      assertFalse(table.getFields().containsKey("email"));
      assertEquals(List.of("lastName", "firstName", "id"), table.getSections().getFirst().getFieldNames());
      assertEquals("Personal Details", table.getSections().getFirst().getLabel());
      input.setInputSource(QInputSource.SYSTEM);
      assertTrue(new TableMetaDataAction().execute(input).getTable().getFields().containsKey("email"));
      QContext.init(instance, new QSession().withPermissions());
      input.setInputSource(QInputSource.USER);
      table = new TableMetaDataAction().execute(input).getTable();
      assertTrue(table.getFields().containsKey("email"));
      assertEquals(List.of("id", "firstName", "lastName"), table.getSections().getFirst().getFieldNames());
      assertTrue(instance.getTable("person").getFields().containsKey("email"));
      assertEquals("Identity", instance.getTable("person").getSections().getFirst().getLabel());
   }



   /*******************************************************************************
    ** Invalid hierarchies fail validation before a server can publish navigation.
    *******************************************************************************/
   @Test
   void testInvalidParentAndCyclesAreRejected() throws Exception
   {
      instance.getApp("greetingsApp").setParentAppName("missingParent");
      assertTrue(assertThrows(QInstanceValidationException.class, () -> new QInstanceValidator().validate(instance)).getMessage().contains("Unrecognized parent app"));
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.getApp("greetingsApp").withChild(instance.getApp("peopleApp"));
      assertTrue(assertThrows(QInstanceValidationException.class, () -> new QInstanceValidator().validate(instance)).getMessage().contains("Circular app reference"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void start() throws Exception
   {
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
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      server.start();
      base = URI.create("http://localhost:" + service.get().port());
      client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> request(String path) throws Exception
   {
      URI target = base.resolve(path);
      assertEquals(base.getAuthority(), target.getAuthority());
      return client.send(HttpRequest.newBuilder(target).timeout(Duration.ofSeconds(20)).GET().build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject json(String path) throws Exception
   {
      HttpResponse<String> response = request(path);
      assertEquals(200, response.statusCode(), response.body());
      return new JSONObject(response.body());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject named(JSONArray objects, String name)
   {
      return objects.toList().stream().map(value -> new JSONObject((java.util.Map<?, ?>) value))
         .filter(value -> name.equals(value.optString("name"))).findFirst().orElseThrow();
   }



   /*******************************************************************************
    ** Legacy table metadata has an envelope; v1 returns the table directly.
    *******************************************************************************/
   private JSONObject table(String prefix, String name) throws Exception
   {
      JSONObject response = json(prefix + "/metaData/table/" + name);
      return prefix.isEmpty() ? response.getJSONObject("table") : response;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PresentationPersonalizer implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(!"person".equals(input.getTableName()) || input.getInputSource() != QInputSource.USER
            || !QContext.getQSession().getPermissions().contains("sample.personalized"))
         {
            return input.getTable();
         }
         QTableMetaData personalized = input.getTable().clone();
         personalized.getFields().remove("email");
         personalized.setSections(List.of(new QFieldSection("identity", "Personal Details", new QIcon("badge"), Tier.T1, List.of("lastName", "firstName", "id"))));
         return personalized;
      }
   }
}
