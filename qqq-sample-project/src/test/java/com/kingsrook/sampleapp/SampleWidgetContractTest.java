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


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ParentWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChildRecordListData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RawHTML;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.ParentWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.WidgetDropdownData;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Canonical sample widget values and HTTP authorization use the real renderers.
 *******************************************************************************/
class SampleWidgetContractTest
{
   private static volatile Set<String> grants = Set.of();
   private static final AtomicInteger renderCalls = new AtomicInteger();
   private QInstance instance;
   private SampleJavalinServer server;
   private HttpClient client;
   private URI base;



   /*******************************************************************************
    ** Independent sessions make granted and denied controls explicit.
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      grants = Set.of();
      renderCalls.set(0);
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.getAuthentication().setCustomizer(new QCodeReference(OwnedPermissions.class));
      instance.addWidget(new QWidgetMetaData().withName("ownedWidget").withLabel("Owned widget").withType("html")
         .withPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION))
         .withCodeReference(new QCodeReference(OwnedRenderer.class)));
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
      client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
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
      grants = Set.of();
      QContext.clear();
   }



   /*******************************************************************************
    ** Assert native data, not merely a successful renderer call or card shell.
    *******************************************************************************/
   @Test
   void testCanonicalWidgetPayloads() throws Exception
   {
      Map<String, JSONObject> data = new HashMap<>();
      List<String> widgets = instance.getApp("SampleWidgetsDashboard").getWidgets();
      assertEquals(11, widgets.size());
      for(String name : widgets)
      {
         JSONObject value = json("/widget/" + name);
         assertFalse(value.getString("type").isBlank(), name);
         data.put(name, value);
      }
      JSONObject statistics = data.get("SampleStatisticsWidget");
      assertEquals("98.5%", statistics.getString("count"));
      assertEquals("of 481", statistics.getString("countContext"));
      assertEquals(-10, statistics.getInt("percentageAmount"));
      for(String name : List.of("SampleBarChartWidget", "SampleLineChartWidget", "SamplePieChartWidget", "SampleSmallLineChartWidget", "SampleStackedBarChartWidget"))
      {
         JSONObject chart = data.get(name).getJSONObject("chartData");
         assertFalse(chart.getJSONArray("labels").isEmpty(), name);
         assertEquals(chart.getJSONArray("labels").length(), chart.getJSONArray("datasets").getJSONObject(0).getJSONArray("data").length(), name);
      }
      JSONObject bar = data.get("SampleBarChartWidget").getJSONObject("chartData");
      assertEquals(List.of("Apple", "Orange", "Banana", "Lime", "Blueberry"), bar.getJSONArray("labels").toList());
      assertEquals(List.of(100, 150, 75, 100, 200), bar.getJSONArray("datasets").getJSONObject(0).getJSONArray("data").toList());
      assertEquals(List.of(1753, 1830, 920, 1543, 1804), data.get("SampleLineChartWidget").getJSONObject("chartData").getJSONArray("datasets").getJSONObject(0).getJSONArray("data").toList());
      JSONObject table = data.get("SampleTableWidget");
      assertEquals(3, table.getJSONArray("columns").length());
      assertEquals(4, table.getJSONArray("rows").length());
      assertEquals("Darin", table.getJSONArray("rows").getJSONObject(0).getString("name"));
      assertEquals("Total", table.getJSONArray("rows").getJSONObject(3).getString("name"));
      assertEquals(3, data.get("SampleStepperWidget").getJSONArray("steps").length());
      assertEquals(1, data.get("SampleStepperWidget").getInt("activeStep"));
      assertEquals(2, data.get("SampleMultiStatisticsWidget").getJSONArray("statisticsGroupData").length());
      assertEquals(3, data.get("SampleBigNumberBlocksWidget").getJSONArray("blocks").length());
      assertTrue(data.get("SampleBigNumberBlocksWidget").toString().contains("12,345"));
      assertTrue(data.get("SampleHTMLWidget").getString("html").contains("<i>User</i>"));
   }



   /*******************************************************************************
    ** Metadata visibility and direct GET/POST must agree before renderer effects.
    *******************************************************************************/
   @Test
   void testWidgetPermissionBeforeRendering() throws Exception
   {
      assertFalse(json("/metaData").getJSONObject("widgets").has("ownedWidget"));
      for(String method : List.of("GET", "POST"))
      {
         HttpResponse<String> denied = request(method, "/widget/ownedWidget");
         assertEquals(403, denied.statusCode(), denied.body());
         assertEquals(0, renderCalls.get());
      }
      grants = Set.of("ownedWidget.hasAccess");
      assertEquals("owned-sensitive-value", json("/widget/ownedWidget").getString("html"));
      assertEquals(1, renderCalls.get());
      assertEquals(200, request("POST", "/widget/ownedWidget").statusCode());
      assertEquals(2, renderCalls.get());
      assertEquals(403, request("GET", "/widget/missingWidget").statusCode());
   }



   /*******************************************************************************
    ** Widget authorization applies to every canonical renderer type.
    *******************************************************************************/
   @Test
   void testCanonicalWidgetPermissionMatrix() throws Exception
   {
      for(String name : instance.getApp("SampleWidgetsDashboard").getWidgets())
      {
         instance.getWidget(name).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
         grants = Set.of();
         assertFalse(json("/metaData").getJSONObject("widgets").has(name));
         for(String method : List.of("GET", "POST"))
         {
            assertEquals(403, request(method, "/widget/" + name).statusCode(), name + " " + method);
         }
         grants = Set.of(name + ".hasAccess");
         assertTrue(json("/metaData").getJSONObject("widgets").has(name));
         for(String method : List.of("GET", "POST"))
         {
            HttpResponse<String> allowed = request(method, "/widget/" + name);
            assertEquals(200, allowed.statusCode(), allowed.body());
            assertFalse(new JSONObject(allowed.body()).getString("type").isBlank(), name);
         }
      }
   }



   /*******************************************************************************
    ** Invalid renderer references fail without producing a successful payload.
    *******************************************************************************/
   @Test
   void testInvalidRendererReferences() throws Exception
   {
      for(String name : instance.getApp("SampleWidgetsDashboard").getWidgets())
      {
         for(QCodeReference reference : List.of(new QCodeReference("com.kingsrook.sampleapp.MissingWidgetRenderer", QCodeType.JAVA), new QCodeReference(String.class)))
         {
            instance.getWidget(name).setCodeReference(reference);
            for(String method : List.of("GET", "POST"))
            {
               HttpResponse<String> failed = request(method, "/widget/" + name);
               assertEquals(500, failed.statusCode(), name + " " + reference + " " + failed.body());
               assertFalse(new JSONObject(failed.body()).optString("error").isBlank(), failed.body());
            }
         }
      }
   }



   /*******************************************************************************
    ** Empty HTML is valid; an actual renderer failure remains a failed request.
    *******************************************************************************/
   @Test
   void testEmptyAndFailedRendering() throws Exception
   {
      grants = Set.of("ownedWidget.hasAccess");
      assertEquals("", json("/widget/ownedWidget?mode=empty").optString("html"));
      HttpResponse<String> failure = request("GET", "/widget/ownedWidget?mode=error");
      assertEquals(500, failure.statusCode(), failure.body());
      assertTrue(failure.body().contains("Owned renderer failure"));
      assertEquals(2, renderCalls.get());
   }



   /*******************************************************************************
    ** Parent payloads preserve layouts while child and parent grants stay separate.
    *******************************************************************************/
   @Test
   void testParentPayloadAndPermissions() throws Exception
   {
      ParentWidgetMetaData parent = new ParentWidgetMetaData().withChildWidgetNameList(List.of("ownedWidget"));
      parent.withName("ownedParent").withType("parentWidget").withLabel("Owned Parent")
         .withCodeReference(new QCodeReference(ParentWidgetRenderer.class))
         .withPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      instance.addWidget(parent);
      for(String method : List.of("GET", "POST"))
      {
         assertEquals(403, request(method, "/widget/ownedParent").statusCode());
      }
      grants = Set.of("ownedParent.hasAccess");
      assertFalse(json("/metaData").getJSONObject("widgets").has("ownedWidget"));
      for(ParentWidgetMetaData.LayoutType layout : ParentWidgetMetaData.LayoutType.values())
      {
         parent.setLayoutType(layout);
         JSONObject data = json("/widget/ownedParent");
         assertEquals(layout.name(), data.getString("layoutType"));
         assertEquals(List.of("ownedWidget"), data.getJSONArray("childWidgetNameList").toList());
         assertEquals(200, request("POST", "/widget/ownedParent").statusCode());
         assertEquals(403, request("GET", "/widget/ownedWidget").statusCode());
      }
      grants = Set.of("ownedParent.hasAccess", "ownedWidget.hasAccess");
      assertEquals("owned-sensitive-value", json("/widget/ownedWidget").getString("html"));
      parent.setChildWidgetNameList(List.of());
      JSONObject empty = json("/widget/ownedParent");
      assertTrue(empty.optJSONArray("childWidgetNameList") == null || empty.getJSONArray("childWidgetNameList").isEmpty());
      parent.setChildWidgetNameList(null);
      assertFalse(json("/widget/ownedParent").has("childWidgetNameList"));
   }



   /*******************************************************************************
    ** Invalid renderer references and missing dropdown sources fail as HTTP errors.
    *******************************************************************************/
   @Test
   void testParentRendererFailures() throws Exception
   {
      ParentWidgetMetaData parent = new ParentWidgetMetaData();
      parent.withName("ownedParent").withType("parentWidget").withLabel("Owned Parent");
      instance.addWidget(parent);
      for(QCodeReference reference : List.of(new QCodeReference("com.kingsrook.sampleapp.MissingParent", QCodeType.JAVA), new QCodeReference(String.class)))
      {
         parent.setCodeReference(reference);
         assertEquals(500, request("GET", "/widget/ownedParent").statusCode());
      }
      parent.setCodeReference(new QCodeReference(ParentWidgetRenderer.class));
      parent.setDropdowns(List.of(new WidgetDropdownData().withPossibleValueSourceName("ownedMissingPvs")));
      HttpResponse<String> response = request("GET", "/widget/ownedParent");
      assertEquals(500, response.statusCode(), response.body());
      assertFalse(new JSONObject(response.body()).optString("error").isBlank());
   }



   /*******************************************************************************
    ** Child rows, total counts and limited pages come from the sample's native join.
    *******************************************************************************/
   @Test
   void testChildRecordPayloadAndReferences() throws Exception
   {
      addChildWidget(2);
      JSONObject data = json("/widget/ownedChildren?id=1");
      assertEquals("pet", data.getJSONObject("childFrontendTableMetaData").getString("name"));
      assertEquals(2, data.getJSONObject("queryOutput").getJSONArray("records").length());
      assertEquals(4, data.getInt("totalRows"));
      assertEquals("Charlie", data.getJSONObject("queryOutput").getJSONArray("records").getJSONObject(0).getJSONObject("values").getString("name"));
      assertEquals(0, json("/widget/ownedChildren?id=4").getInt("totalRows"));
      assertEquals(0, json("/widget/ownedChildren").getInt("totalRows"));
      assertEquals(404, request("GET", "/widget/ownedChildren?id=999").statusCode());
      assertEquals(4, json("/widget/ownedChildren?id=1&joinName=missingOwnedJoin").getInt("totalRows"));
      String joinName = (String) instance.getWidget("ownedChildren").getDefaultValues().get(ChildRecordListRenderer.KEY_JOIN_NAME);
      instance.getWidget("ownedChildren").withDefaultValue(ChildRecordListRenderer.KEY_JOIN_NAME, "missingOwnedJoin");
      assertEquals(500, request("GET", "/widget/ownedChildren?id=1").statusCode());
      instance.getWidget("ownedChildren").withDefaultValue(ChildRecordListRenderer.KEY_JOIN_NAME, joinName);
      for(QCodeReference reference : List.of(new QCodeReference("com.kingsrook.sampleapp.MissingChildren", QCodeType.JAVA), new QCodeReference(String.class)))
      {
         instance.getWidget("ownedChildren").setCodeReference(reference);
         assertEquals(500, request("GET", "/widget/ownedChildren?id=1").statusCode());
      }
   }



   /*******************************************************************************
    ** Widget visibility cannot substitute for READ on either source table.
    *******************************************************************************/
   @Test
   void testChildRecordSourceReadDenial() throws Exception
   {
      addChildWidget(2);
      for(String table : List.of("person", "pet"))
      {
         instance.getTable(table).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      }
      assertAll(
         () ->
         {
            grants = Set.of("person.read");
            for(String method : List.of("GET", "POST"))
            {
               HttpResponse<String> response = request(method, "/widget/ownedChildren?id=1&inputSource=SYSTEM");
               assertEquals(403, response.statusCode(), response.body());
            }
         },
         () ->
         {
            grants = Set.of("pet.read");
            for(String method : List.of("GET", "POST"))
            {
               HttpResponse<String> response = request(method, "/widget/ownedChildren?id=1&inputSource=SYSTEM");
               assertEquals(403, response.statusCode(), response.body());
            }
         });
   }



   /*******************************************************************************
    ** The page and total must use the same personalized record visibility.
    *******************************************************************************/
   @Test
   void testChildRecordPersonalizedCount() throws Exception
   {
      addChildWidget(1);
      instance.addSecurityKeyType(new QSecurityKeyType().withName("ownedWidgetRow"));
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(ChildReadPolicy.class));
      instance.getAuthentication().setCustomizer(new QCodeReference(ChildPermissions.class));
      JSONObject data = json("/widget/ownedChildren?id=1");
      assertEquals(1, data.getJSONObject("queryOutput").getJSONArray("records").length());
      assertEquals(1, data.getInt("totalRows"), data.toString());
      assertEquals(404, request("GET", "/widget/ownedChildren?id=2").statusCode());
   }



   /*******************************************************************************
    ** Joined filters need READ; trusted calls retain the existing internal path.
    *******************************************************************************/
   @Test
   void testChildJoinedReadAndTrustedControl() throws Exception
   {
      addChildWidget(2);
      instance.getTable("petNote").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      instance.getWidget("ownedChildren")
         .withDefaultValue(ChildRecordListRenderer.KEY_QUERY_JOINS, new ArrayList<>(List.of(new QueryJoin("petNote"))))
         .withDefaultValue(ChildRecordListRenderer.KEY_BASE_FILTER, new QQueryFilter(new QFilterCriteria("petNote.note", QCriteriaOperator.EQUALS, "Target note")));
      assertEquals(403, request("GET", "/widget/ownedChildren?id=1").statusCode());
      grants = Set.of("petNote.read");
      assertEquals(1, json("/widget/ownedChildren?id=1").getInt("totalRows"));
      QContext.init(instance, new QSession());
      RenderWidgetInput input = new RenderWidgetInput().withWidgetMetaData(instance.getWidget("ownedChildren"));
      input.setQueryParams(new HashMap<>(Map.of("id", "1")));
      ChildRecordListData data = (ChildRecordListData) new RenderWidgetAction().execute(input).getWidgetData();
      assertEquals(1, data.getTotalRows());
      assertEquals("Charlie", data.getQueryOutput().getRecords().getFirst().getValueString("name"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addChildWidget(int maxRows)
   {
      QJoinMetaData join = instance.getJoins().values().stream().filter(value -> "person".equals(value.getLeftTable()) && "pet".equals(value.getRightTable())).findFirst().orElseThrow();
      instance.addWidget(ChildRecordListRenderer.widgetMetaDataBuilder(join).withName("ownedChildren").withLabel("Owned children")
         .withMaxRows(maxRows).getWidgetMetaData());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ChildReadPolicy implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() == QInputSource.USER && Set.of("person", "pet").contains(input.getTableName()))
         {
            return input.getTable().clone().withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("ownedWidgetRow")
               .withFieldName("id").withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
         }
         return input.getTable();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ChildPermissions extends OwnedPermissions
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         super.customizeSession(instance, session, context);
         session.withSecurityKeyValue("ownedWidgetRow", 1);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> request(String method, String path) throws Exception
   {
      return client.send(HttpRequest.newBuilder(base.resolve(path)).timeout(Duration.ofSeconds(10))
         .method(method, HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject json(String path) throws Exception
   {
      HttpResponse<String> response = request("GET", path);
      assertEquals(200, response.statusCode(), response.body());
      return new JSONObject(response.body());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class OwnedPermissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions(grants.toArray(String[]::new));
      }
   }



   /*******************************************************************************
    ** The counter proves denied requests never execute application widget code.
    *******************************************************************************/
   public static class OwnedRenderer extends AbstractWidgetRenderer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public RenderWidgetOutput render(RenderWidgetInput input) throws QException
      {
         renderCalls.incrementAndGet();
         if("error".equals(input.getQueryParams().get("mode")))
         {
            throw new QException("Owned renderer failure");
         }
         return new RenderWidgetOutput(new RawHTML("Owned", "empty".equals(input.getQueryParams().get("mode")) ? "" : "owned-sensitive-value"));
      }
   }
}
