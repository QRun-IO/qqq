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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.Aggregate2DTableWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.NoCodeWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.AbstractWidgetValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.HtmlWrapper;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.QNoCodeWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetAdHocValue;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetAggregate;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetCalculation;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetCount;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetHtmlLine;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.nocode.WidgetQueryField;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Configured widget results cross the real HTTP and JSON boundary against H2.
 *******************************************************************************/
class SampleAggregateWidgetCallerTest
{
   private QInstance instance;
   private SampleJavalinServer server;
   private HttpClient client;
   private URI baseUri;
   private Map<String, List<List<String>>> before;
   private Map<String, String> metadataBefore;
   private String mockAuthenticationBefore;



   /*******************************************************************************
    ** HTTP authority must not come from query or form parameters.
    *******************************************************************************/
   @Test
   void testNoCodeHttpReadSubsetAndSourceOverrides() throws Exception
   {
      start(ReadPolicy.class);
      int userCount = scalar("SELECT COUNT(id) FROM person WHERE id=1");
      int systemCount = scalar("SELECT COUNT(id) FROM person");
      assertTrue(systemCount > userCount);
      assertAll(
         () -> assertHtml(get("personCount"), userCount),
         () -> assertHtml(get("personWithDogCount"), scalar("SELECT COUNT(DISTINCT p.id) FROM person p JOIN pet a ON p.id=a.person_id WHERE p.id=1 AND a.species_id=1")),
         () -> assertHtml(get("personCount?inputSource=SYSTEM"), userCount),
         () -> assertHtml(post("personCount", "inputSource=SYSTEM"), userCount),
         () -> assertHtml(direct("personCount"), systemCount));
   }



   /*******************************************************************************
    ** Native cells and presentation labels must use the same active USER table.
    *******************************************************************************/
   @Test
   void testTwoDimensionalHttpReadSubsetLabelsAndSourceOverrides() throws Exception
   {
      start(ReadPolicy.class);
      String userSql = "SELECT name,species_id,COUNT(id) FROM pet WHERE person_id=1 GROUP BY name,species_id ORDER BY name,species_id";
      String systemSql = "SELECT name,species_id,COUNT(id) FROM pet GROUP BY name,species_id ORDER BY name,species_id";
      assertTrue(scalar("SELECT COUNT(*) FROM pet") > scalar("SELECT COUNT(*) FROM pet WHERE person_id=1"));
      assertAll(
         () -> assertGrid(get("petGrid"), userSql, "Permitted pet"),
         () -> assertGrid(get("petGrid?inputSource=SYSTEM"), userSql, "Permitted pet"),
         () -> assertGrid(post("petGrid", "inputSource=SYSTEM"), userSql, "Permitted pet"),
         () -> assertGrid(direct("petGrid"), systemSql, instance.getTable("pet").getField("name").getLabel()));
   }



   /*******************************************************************************
    ** A configured aggregate cannot use an operand removed from active metadata.
    *******************************************************************************/
   @Test
   void testNoCodeRemovedOperandAndSystemControl() throws Exception
   {
      start(RemovedOperand.class);
      assertAll(
         () -> assertEmptyHtml(get("salaryCount")),
         () -> assertHtml(direct("salaryCount"), scalar("SELECT COUNT(annual_salary) FROM person")));
   }



   /*******************************************************************************
    ** Missing active row and column fields must fail before native grouping.
    *******************************************************************************/
   @Test
   void testTwoDimensionalRemovedRowAndSystemControl() throws Exception
   {
      start(RemovedRow.class);
      assertAll(
         () -> assertError(request("GET", "petGrid", null), 500),
         () -> assertGrid(direct("petGrid"), "SELECT name,species_id,COUNT(id) FROM pet GROUP BY name,species_id ORDER BY name,species_id", instance.getTable("pet").getField("name").getLabel()));
   }



   /*******************************************************************************
    ** Row-field checking alone must not leave a removed column field usable.
    *******************************************************************************/
   @Test
   void testTwoDimensionalRemovedColumnAndSystemControl() throws Exception
   {
      start(RemovedColumn.class);
      assertAll(
         () -> assertError(request("GET", "petGrid", null), 500),
         () -> assertGrid(direct("petGrid"), "SELECT name,species_id,COUNT(id) FROM pet GROUP BY name,species_id ORDER BY name,species_id", instance.getTable("pet").getField("name").getLabel()));
   }



   /*******************************************************************************
    ** WRITE permission cannot authorize an aggregate read over HTTP.
    *******************************************************************************/
   @Test
   void testNoCodeReadPermissionAndSystemControl() throws Exception
   {
      start(ReadPolicy.class);
      assertAll(
         () -> assertEmptyHtml(get("fieldLabCount")),
         () -> assertEmptyHtml(get("fieldLabCount?inputSource=SYSTEM")),
         () -> assertEmptyHtml(post("fieldLabCount", "inputSource=SYSTEM")),
         () -> assertHtml(direct("fieldLabCount"), scalar("SELECT COUNT(id) FROM field_lab")));
   }



   /*******************************************************************************
    ** The table renderer also enforces READ without changing direct SYSTEM calls.
    *******************************************************************************/
   @Test
   void testTwoDimensionalReadPermissionAndSystemControl() throws Exception
   {
      start(ReadPolicy.class);
      assertAll(
         () -> assertError(request("GET", "fieldLabGrid", null), 403),
         () -> assertError(request("GET", "fieldLabGrid?inputSource=SYSTEM", null), 403),
         () -> assertError(request("POST", "fieldLabGrid", "inputSource=SYSTEM"), 403),
         () -> assertGrid(direct("fieldLabGrid"), "SELECT name,long_value,COUNT(id) FROM field_lab GROUP BY name,long_value ORDER BY name,long_value", instance.getTable("fieldLab").getField("name").getLabel()));
   }



   /*******************************************************************************
    ** A USER-removed table must not fall back to canonical metadata.
    *******************************************************************************/
   @Test
   void testRemovedTablesAndSystemControls() throws Exception
   {
      start(RemovedTables.class);
      assertAll(
         () -> assertEmptyHtml(get("personCount")),
         () -> assertError(request("GET", "petGrid", null), 500),
         () -> assertHtml(direct("personCount"), scalar("SELECT COUNT(id) FROM person")),
         () -> assertGrid(direct("petGrid"), "SELECT name,species_id,COUNT(id) FROM pet GROUP BY name,species_id ORDER BY name,species_id", instance.getTable("pet").getField("name").getLabel()));
   }



   /*******************************************************************************
    ** A filter can read a joined table even when its values are not returned.
    *******************************************************************************/
   @Test
   void testNoCodeJoinedFilterReadPermissionAndSystemControl() throws Exception
   {
      start(ReadPolicy.class, PersonOnlyPermissions.class);
      assertAll(
         () -> assertEmptyHtml(get("personWithDogCount")),
         () -> assertEmptyHtml(get("personWithDogCount?inputSource=SYSTEM")),
         () -> assertEmptyHtml(post("personWithDogCount", "inputSource=SYSTEM")),
         () -> assertHtml(direct("personWithDogCount"), scalar("SELECT COUNT(DISTINCT p.id) FROM person p JOIN pet a ON p.id=a.person_id WHERE a.species_id=1")));
   }



   /*******************************************************************************
    ** Query and count values must keep the same caller scope as aggregates.
    *******************************************************************************/
   @Test
   void testNoCodeQueryAndCountCallerScope() throws Exception
   {
      start(ReadPolicy.class);
      addValueWidget("ownedCount", new WidgetCount().withName("metric").withTableName("person"));
      addValueWidget("ownedQuery", new WidgetQueryField().withName("metric").withTableName("person").withSelectFieldName("id")
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 2))));
      assertAll(
         () -> assertHtml(get("ownedCount"), scalar("SELECT COUNT(id) FROM person WHERE id=1")),
         () -> assertHtml(post("ownedCount", "inputSource=SYSTEM"), 1),
         () -> assertEmptyHtml(get("ownedQuery")),
         () -> assertEmptyHtml(get("ownedQuery?inputSource=SYSTEM")),
         () -> assertEmptyHtml(post("ownedQuery", "inputSource=SYSTEM")),
         () -> assertHtml(direct("ownedCount"), scalar("SELECT COUNT(id) FROM person")),
         () -> assertHtml(direct("ownedQuery"), 2));
   }



   /*******************************************************************************
    ** A write grant must not expose a no-code count or field from that table.
    *******************************************************************************/
   @Test
   void testNoCodeQueryAndCountReadPermission() throws Exception
   {
      start(ReadPolicy.class);
      addValueWidget("ownedCount", new WidgetCount().withName("metric").withTableName("fieldLab"));
      addValueWidget("ownedQuery", new WidgetQueryField().withName("metric").withTableName("fieldLab").withSelectFieldName("id")
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))));
      assertAll(
         () -> assertEmptyHtml(get("ownedCount")),
         () -> assertEmptyHtml(post("ownedCount", "inputSource=SYSTEM")),
         () -> assertEmptyHtml(get("ownedQuery")),
         () -> assertEmptyHtml(post("ownedQuery", "inputSource=SYSTEM")),
         () -> assertHtml(direct("ownedCount"), 3),
         () -> assertHtml(direct("ownedQuery"), 1));
   }



   /*******************************************************************************
    ** Joined filter sources need READ even when no joined field is returned.
    *******************************************************************************/
   @Test
   void testNoCodeQueryAndCountJoinedReadPermission() throws Exception
   {
      start(ReadPolicy.class, PersonOnlyPermissions.class);
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("pet.speciesId", QCriteriaOperator.EQUALS, 1));
      addValueWidget("ownedCount", new WidgetCount().withName("metric").withTableName("person").withFilter(filter));
      addValueWidget("ownedQuery", new WidgetQueryField().withName("metric").withTableName("person").withSelectFieldName("id")
         .withFilter(new QQueryFilter(new QFilterCriteria("pet.speciesId", QCriteriaOperator.EQUALS, 1), new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))));
      assertAll(
         () -> assertEmptyHtml(get("ownedCount")),
         () -> assertEmptyHtml(get("ownedQuery")),
         () -> assertHtml(direct("ownedCount"), scalar("SELECT COUNT(*) FROM person p JOIN pet a ON p.id=a.person_id WHERE a.species_id=1")),
         () -> assertHtml(direct("ownedQuery"), 1));
   }



   /*******************************************************************************
    ** Removed tables must stay unavailable to the HTTP renderer.
    *******************************************************************************/
   @Test
   void testNoCodeQueryAndCountRemovedTable() throws Exception
   {
      start(RemovedTables.class);
      addValueWidget("ownedCount", new WidgetCount().withName("metric").withTableName("person"));
      addValueWidget("ownedQuery", new WidgetQueryField().withName("metric").withTableName("person").withSelectFieldName("id")
         .withFilter(new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 1))));
      assertAll(
         () -> assertEmptyHtml(get("ownedCount")),
         () -> assertEmptyHtml(get("ownedQuery")),
         () -> assertHtml(direct("ownedCount"), scalar("SELECT COUNT(id) FROM person")),
         () -> assertHtml(direct("ownedQuery"), 1));
   }



   /*******************************************************************************
    ** Declarative values, all calculation operators, conditional output and wrappers.
    *******************************************************************************/
   @Test
   void testNoCodeValueSourcesAndCalculations() throws Exception
   {
      start(ReadPolicy.class);
      QNoCodeWidgetMetaData widget = new QNoCodeWidgetMetaData()
         .withValue(new WidgetCount().withName("count").withTableName("person"))
         .withValue(new WidgetQueryField().withName("id").withTableName("person").withSelectFieldName("id"))
         .withValue(new WidgetAggregate().withName("aggregate").withTableName("person").withAggregate(new Aggregate("id", AggregateOperator.COUNT)))
         .withValue(new WidgetAdHocValue().withName("constant").withInputValues(Map.of("then", Instant.now().minusSeconds(120)))
            .withCodeReference(new QCodeReference(OwnedConstant.class)))
         .withValue(new WidgetCalculation().withName("sum").withOperator(WidgetCalculation.Operator.SUM_INTEGERS).withValues(List.of("count", "aggregate", "constant")))
         .withValue(new WidgetCalculation().withName("change").withOperator(WidgetCalculation.Operator.PERCENT_CHANGE).withValues(List.of("current", "previous")))
         .withValue(new WidgetCalculation().withName("minutes").withOperator(WidgetCalculation.Operator.AGE_MINUTES).withValues(List.of("then")))
         .withValue(new WidgetCalculation().withName("seconds").withOperator(WidgetCalculation.Operator.AGE_SECONDS).withValues(List.of("then")))
         .withOutput(new WidgetHtmlLine().withVelocityTemplate("$count|$id|$aggregate|$constant|$sum|$change|$minutes|$seconds"))
         .withOutput(new WidgetHtmlLine().withVelocityTemplate("visible").withWrapper(HtmlWrapper.SUBHEADER)
            .withCondition(new QFilterCriteria("count", QCriteriaOperator.EQUALS, 1)))
         .withOutput(new WidgetHtmlLine().withVelocityTemplate("hidden").withCondition(new QFilterCriteria("count", QCriteriaOperator.EQUALS, 999)));
      addNoCodeWidget("ownedValues", widget);
      String html = post("ownedValues", "current=200&previous=100").getString("html");
      assertTrue(html.startsWith("1|1|1|7|9|100|2|"), html);
      assertTrue(html.endsWith("<h4>\nvisible\n</h4>\n"), html);
      int seconds = Integer.parseInt(html.substring("1|1|1|7|9|100|2|".length(), html.indexOf("<h4>")));
      assertTrue(seconds >= 120 && seconds < 150, html);
      assertFalse(html.contains("hidden"));
   }



   /*******************************************************************************
    ** Empty sources and invalid values retain healthy output; bad templates fail.
    *******************************************************************************/
   @Test
   void testNoCodeEmptyInvalidSourcesAndTemplate() throws Exception
   {
      start(ReadPolicy.class);
      QQueryFilter empty = new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, -1));
      QNoCodeWidgetMetaData widget = new QNoCodeWidgetMetaData()
         .withValue(new WidgetCount().withName("count").withTableName("person").withFilter(empty))
         .withValue(new WidgetQueryField().withName("query").withTableName("person").withSelectFieldName("id").withFilter(empty))
         .withValue(new WidgetCount().withName("missing").withTableName("ownedMissingTable"))
         .withValue(new WidgetCalculation().withName("invalidCalculation").withValues(List.of("count")))
         .withValue(new WidgetAdHocValue().withName("invalidReference").withCodeReference(new QCodeReference(String.class)))
         .withOutput(new WidgetHtmlLine().withVelocityTemplate("healthy:$count|$!query|$!missing|$!invalidCalculation|$!invalidReference"));
      addNoCodeWidget("ownedValues", widget);
      assertEquals("healthy:0||||", get("ownedValues").getString("html"));
      widget.setOutputs(List.of(new WidgetHtmlLine().withVelocityTemplate("#if(")));
      metadataBefore = metadata();
      assertError(request("GET", "ownedValues", null), 500);
   }



   /*******************************************************************************
    ** Raw HTML templates explicitly escape untrusted text in an ad-hoc value.
    *******************************************************************************/
   @Test
   void testNoCodeEscapedUserText() throws Exception
   {
      start(ReadPolicy.class);
      QNoCodeWidgetMetaData widget = new QNoCodeWidgetMetaData()
         .withValue(new WidgetAdHocValue().withName("safeText")
            .withCodeReference(new QCodeReference(OwnedEscapedText.class)))
         .withOutput(new WidgetHtmlLine().withVelocityTemplate("<p>$safeText</p>"));
      addNoCodeWidget("ownedValues", widget);
      assertEquals("<p>&lt;img src=x onerror=owned()&gt;&amp;hello</p>",
         post("ownedValues", "text=%3Cimg+src%3Dx+onerror%3Downed()%3E%26hello").getString("html"));
   }



   /*******************************************************************************
    ** Add only this case's native value source to the already running sample.
    *******************************************************************************/
   private void addValueWidget(String name, AbstractWidgetValueSource value)
   {
      QNoCodeWidgetMetaData widget = new QNoCodeWidgetMetaData().withValue(value)
         .withOutput(new WidgetHtmlLine().withVelocityTemplate("<span>aggregate=[$!{metric}]</span>"));
      addNoCodeWidget(name, widget);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addNoCodeWidget(String name, QNoCodeWidgetMetaData widget)
   {
      widget.withName(name).withLabel(name).withType(WidgetType.HTML.getType()).withCodeReference(new QCodeReference(NoCodeWidgetRenderer.class));
      instance.addWidget(widget);
      metadataBefore = metadata();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void start(Class<?> personalizer) throws Exception
   {
      start(personalizer, WidgetPermissions.class);
   }



   /*******************************************************************************
    ** Use the same injected-instance server fixture as sample HTTP acceptance.
    *******************************************************************************/
   private void start(Class<?> personalizer, Class<?> permissions) throws Exception
   {
      mockAuthenticationBefore = System.getProperty("qqq.sample.mockAuthentication");
      ConnectionManager.resetConnectionProviders();
      instance = SampleMetaDataProvider.defineTestInstance();
      assertEquals(QAuthenticationType.MOCK, instance.getAuthentication().getType());
      instance.getAuthentication().setCustomizer(new QCodeReference(permissions));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("widgetOwner"));
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(personalizer));
      for(String tableName : List.of("person", "pet", "fieldLab"))
      {
         instance.getTable(tableName).setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS));
      }
      instance.addWidget(scalarWidget("personCount", "person", "id"));
      QNoCodeWidgetMetaData joinedWidget = (QNoCodeWidgetMetaData) scalarWidget("personWithDogCount", "person", "id");
      WidgetAggregate joinedAggregate = (WidgetAggregate) joinedWidget.getValues().get(0);
      joinedAggregate.setAggregate(new Aggregate("id", AggregateOperator.COUNT_DISTINCT));
      joinedAggregate.setFilter(new QQueryFilter(new QFilterCriteria("pet.speciesId", QCriteriaOperator.EQUALS, 1)));
      instance.addWidget(joinedWidget);
      instance.addWidget(scalarWidget("salaryCount", "person", "annualSalary"));
      instance.addWidget(scalarWidget("fieldLabCount", "fieldLab", "id"));
      instance.addWidget(gridWidget("petGrid", "pet", "name", "speciesId"));
      instance.addWidget(gridWidget("fieldLabGrid", "fieldLab", "name", "longValue"));
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
      client = HttpClient.newHttpClient();
      baseUri = URI.create("http://localhost:" + service.get().port());
      QContext.init(instance, new QSession().withPermissions("person.read", "pet.read", "fieldLab.write").withSecurityKeyValue("widgetOwner", 1));
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(3, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value) VALUES(1,'Alpha',10),(2,'Beta',20),(3,'Gamma',30)"));
      }
      before = snapshot();
      metadataBefore = metadata();
   }



   /*******************************************************************************
    ** Read probes preserve all canonical fixture rows and canonical table metadata.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(before != null)
         {
            assertAll(
               () -> assertEquals(before, snapshot()),
               () -> assertEquals(metadataBefore, metadata()),
               () -> assertEquals(mockAuthenticationBefore, System.getProperty("qqq.sample.mockAuthentication")));
         }
      }
      finally
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
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** No-code errors leave the quiet reference empty instead of returning a metric.
    *******************************************************************************/
   private QWidgetMetaData scalarWidget(String name, String table, String field)
   {
      QNoCodeWidgetMetaData widget = new QNoCodeWidgetMetaData();
      widget.withName(name).withLabel(name).withType(WidgetType.HTML.getType()).withCodeReference(new QCodeReference(NoCodeWidgetRenderer.class));
      widget.withValue(new WidgetAggregate().withName("metric").withTableName(table).withAggregate(new Aggregate(field, AggregateOperator.COUNT)));
      widget.withOutput(new WidgetHtmlLine().withVelocityTemplate("<span>aggregate=[$!{metric}]</span>"));
      return widget;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QWidgetMetaData gridWidget(String name, String table, String row, String column)
   {
      return new QWidgetMetaData().withName(name).withLabel(name).withType(WidgetType.TABLE.getType())
         .withCodeReference(new QCodeReference(Aggregate2DTableWidgetRenderer.class))
         .withDefaultValue("tableName", table).withDefaultValue("valueField", "id")
         .withDefaultValue("rowField", row).withDefaultValue("columnField", column).withDefaultValue("orderBys", "row,column");
   }



   /*******************************************************************************
    ** RenderWidgetInput deliberately uses only APIs available before source support.
    *******************************************************************************/
   private JSONObject direct(String widgetName) throws Exception
   {
      RenderWidgetInput input = new RenderWidgetInput().withWidgetMetaData(instance.getWidget(widgetName));
      input.addQueryParam("inputSource", "USER");
      return JsonUtils.toJSONObject(JsonUtils.toJson(new RenderWidgetAction().execute(input).getWidgetData()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> request(String method, String widgetName, String body) throws Exception
   {
      URI uri = baseUri.resolve("/widget/" + widgetName);
      assertEquals(baseUri.getAuthority(), uri.getAuthority());
      HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30))
         .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
      if(body != null)
      {
         request.header("Content-Type", "application/x-www-form-urlencoded");
      }
      return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject get(String widgetName) throws Exception
   {
      return success(request("GET", widgetName, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject post(String widgetName, String body) throws Exception
   {
      return success(request("POST", widgetName, body));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONObject success(HttpResponse<String> response)
   {
      assertEquals(200, response.statusCode(), response.body());
      JSONObject json = JsonUtils.toJSONObject(response.body());
      assertFalse(json.has("error"), response.body());
      return json;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertError(HttpResponse<String> response, int status)
   {
      assertEquals(status, response.statusCode(), response.body());
      JSONObject json = JsonUtils.toJSONObject(response.body());
      assertTrue(json.has("error"), response.body());
      assertFalse(json.has("rows"), response.body());
      assertFalse(json.has("html"), response.body());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertHtml(JSONObject json, int expected)
   {
      assertEquals("<span>aggregate=[" + expected + "]</span>", json.getString("html"), json.toString());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertEmptyHtml(JSONObject json)
   {
      assertEquals("<span>aggregate=[]</span>", json.getString("html"), json.toString());
   }



   /*******************************************************************************
    ** SQL derives every native cell, zero-filled column, row total and grand total.
    *******************************************************************************/
   private void assertGrid(JSONObject json, String sql, String expectedLabel) throws Exception
   {
      Map<String, Map<String, Object>> expectedByRow = new LinkedHashMap<>();
      Set<String> columns = new LinkedHashSet<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         while(result.next())
         {
            String row = result.getString(1);
            String column = result.getString(2);
            columns.add(column);
            expectedByRow.computeIfAbsent(row, ignored -> new LinkedHashMap<>()).put(column, result.getInt(3));
         }
      }
      Map<String, Object> total = new LinkedHashMap<>();
      total.put("_row", "Total");
      total.put("_total", 0);
      List<Map<String, Object>> expectedRows = new ArrayList<>();
      for(Map.Entry<String, Map<String, Object>> entry : expectedByRow.entrySet())
      {
         Map<String, Object> row = entry.getValue();
         int sum = 0;
         for(String column : columns)
         {
            int value = (Integer) row.getOrDefault(column, 0);
            row.put(column, value);
            sum += value;
            total.put(column, (Integer) total.getOrDefault(column, 0) + value);
         }
         row.put("_row", entry.getKey());
         row.put("_total", sum);
         total.put("_total", (Integer) total.get("_total") + sum);
         expectedRows.add(row);
      }
      expectedRows.add(total);
      assertEquals(expectedRows, json.getJSONArray("rows").toList(), json.toString());
      JSONArray actualColumns = json.getJSONArray("columns");
      assertEquals(expectedLabel, actualColumns.getJSONObject(0).getString("header"));
      List<String> expectedAccessors = new ArrayList<>(List.of("_row"));
      expectedAccessors.addAll(columns);
      expectedAccessors.add("_total");
      List<String> actualAccessors = new ArrayList<>();
      for(int index = 0; index < actualColumns.length(); index++)
      {
         actualAccessors.add(actualColumns.getJSONObject(index).getString("accessor"));
      }
      assertEquals(expectedAccessors, actualAccessors);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private int scalar(String sql) throws Exception
   {
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         assertTrue(result.next());
         int value = result.getInt(1);
         assertFalse(result.next());
         return value;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Connection connection() throws Exception
   {
      Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
      assertEquals("jdbc:h2:mem:test_database", connection.getMetaData().getURL());
      return connection;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, List<List<String>>> snapshot() throws Exception
   {
      Map<String, List<List<String>>> tables = new LinkedHashMap<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         for(String table : List.of("person", "pet", "pet_note", "field_lab"))
         {
            List<List<String>> rows = new ArrayList<>();
            try(ResultSet result = statement.executeQuery("SELECT * FROM " + table + " ORDER BY id"))
            {
               while(result.next())
               {
                  List<String> row = new ArrayList<>();
                  for(int index = 1; index <= result.getMetaData().getColumnCount(); index++)
                  {
                     row.add(result.getString(index));
                  }
                  rows.add(row);
               }
            }
            tables.put(table, rows);
         }
      }
      return tables;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, String> metadata()
   {
      Map<String, String> result = new LinkedHashMap<>();
      for(String table : List.of("person", "pet", "petNote", "fieldLab"))
      {
         result.put(table, JsonUtils.toJson(instance.getTable(table)));
      }
      result.put("joins", JsonUtils.toJson(instance.getJoins()));
      result.put("widgets", JsonUtils.toJson(instance.getWidgets()));
      return result;
   }



   /*******************************************************************************
    ** Named Java functions are the no-code ad-hoc loader contract.
    *******************************************************************************/
   public static class OwnedConstant implements Function<Map<String, Object>, Object>
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Object apply(Map<String, Object> context)
      {
         return 7;
      }
   }



   /*******************************************************************************
    ** Escape a text node before putting its content into a raw HTML template.
    *******************************************************************************/
   public static class OwnedEscapedText implements Function<Map<String, Object>, Object>
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Object apply(Map<String, Object> context)
      {
         return context.get("text").toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class WidgetPermissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read", "pet.read", "fieldLab.write");
         session.withSecurityKeyValue("widgetOwner", 1);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PersonOnlyPermissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         session.withPermissions("person.read", "pet.write", "fieldLab.write");
         session.withSecurityKeyValue("widgetOwner", 1);
      }
   }



   /*******************************************************************************
    ** Only USER receives these locks and labels; SYSTEM uses canonical metadata.
    *******************************************************************************/
   public static class ReadPolicy implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() != QInputSource.USER || !Set.of("person", "pet").contains(input.getTableName()))
         {
            return input.getTable();
         }
         QTableMetaData table = input.getTable().clone();
         table.withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("widgetOwner")
            .withFieldName("person".equals(input.getTableName()) ? "id" : "personId")
            .withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
         if("pet".equals(input.getTableName()))
         {
            table.getField("name").setLabel("Permitted pet");
         }
         return table;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RemovedOperand extends ReadPolicy
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         QTableMetaData table = super.execute(input);
         if(input.getInputSource() == QInputSource.USER && "person".equals(input.getTableName()))
         {
            table.getFields().remove("annualSalary");
         }
         return table;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RemovedRow extends ReadPolicy
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         QTableMetaData table = super.execute(input);
         if(input.getInputSource() == QInputSource.USER && "pet".equals(input.getTableName()))
         {
            table.getFields().remove("name");
         }
         return table;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RemovedColumn extends ReadPolicy
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         QTableMetaData table = super.execute(input);
         if(input.getInputSource() == QInputSource.USER && "pet".equals(input.getTableName()))
         {
            table.getFields().remove("speciesId");
         }
         return table;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RemovedTables extends ReadPolicy
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         return input.getInputSource() == QInputSource.USER && Set.of("person", "pet").contains(input.getTableName()) ? null : input.getTable();
      }
   }
}
