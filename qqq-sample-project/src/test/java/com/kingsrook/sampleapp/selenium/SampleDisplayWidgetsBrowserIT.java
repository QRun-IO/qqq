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

package com.kingsrook.sampleapp.selenium;


import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.CronUIWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ProcessWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.QuickSightChartRenderer;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.AlertData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CronUISetupData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CronUIWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.DividerWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.DynamicFormWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.FieldValueListData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.LocationData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiTableData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ProcessWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.USMapWidgetData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QuickSightChartMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.sampleapp.SampleJavalinServer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Typed sample values cross native HTTP and actual Material display contracts.
 *******************************************************************************/
class SampleDisplayWidgetsBrowserIT
{
   private static final List<String> TYPES = List.of("alert", "divider", "fieldValueList", "horizontalBarChart", "multiTable");
   private static final List<String> EXTENSIONS = List.of("usaMap", "quickSightChart", "process", "customComponent");
   private static final AtomicReference<JSONObject> providerRequest = new AtomicReference<>();
   private static volatile String providerMode;
   private String previousEndpoint;
   private static final AtomicInteger calls = new AtomicInteger();
   private static volatile String mode;
   private static volatile String selected;
   private static volatile boolean allowed;
   private static volatile boolean sourceAllowed;
   private static final List<String> EDITORS = List.of("cronUI", "dynamicForm");
   private QInstance instance;
   private SampleJavalinServer server;
   private HttpClient client;
   private ChromeDriver driver;
   private WebDriverWait wait;
   private String baseUrl;
   private final Path output = Path.of("target", "display-widgets");



   /*******************************************************************************
    ** No external account or data is involved in these display-only fixtures.
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      providerMode = "populated";
      providerRequest.set(null);
      previousEndpoint = System.getProperty("aws.endpointUrl");
      assertTrue(System.getenv("AWS_ENDPOINT_URL") == null && System.getenv("AWS_ENDPOINT_URL_QUICKSIGHT") == null, "This fixture requires its own local AWS endpoint");
      mode = "populated";
      selected = null;
      allowed = true;
      sourceAllowed = true;
      MemoryRecordStore.fullReset();
      calls.set(0);
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.getAuthentication().setCustomizer(new QCodeReference(Permissions.class));
      for(String type : allTypes())
      {
         if("quickSightChart".equals(type))
         {
            continue;
         }
         instance.addWidget(new QWidgetMetaData().withName(name(type)).withType(type).withLabel("Owned " + type).withIsCard(true)
            .withCodeReference(new QCodeReference(Renderer.class))
            .withPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)));
      }
      instance.addWidget(new QuickSightChartMetaData().withName(name("quickSightChart"))
         .withAccessKey("owned-fixture-access").withSecretKey("owned-fixture-secret-not-a-credential")
         .withAccountId("000000000000").withDashboardId("owned-dashboard")
         .withUserArn("arn:aws:quicksight:us-east-1:000000000000:user/default/owned-user").withRegion("us-east-1")
         .withLabel("Owned QuickSight").withType("quickSightChart").withIsCard(true)
         .withCodeReference(new QCodeReference(Renderer.class))
         .withPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)));
      String childJoin = instance.getJoins().values().stream().filter(join -> "person".equals(join.getLeftTable()) && "pet".equals(join.getRightTable())).findFirst().orElseThrow().getName();
      instance.getWidget(name("childRecordList")).withDefaultValue(ChildRecordListRenderer.KEY_JOIN_NAME, childJoin)
         .withDefaultValue(ChildRecordListRenderer.KEY_MAX_ROWS, 2);
      instance.getTable("person").withSection(new QFieldSection().withName(name("childRecordList")).withLabel("Owned children")
         .withTier(Tier.T2).withWidgetName(name("childRecordList")));
      instance.getWidget(name("process")).withDefaultValue(ProcessWidgetRenderer.WIDGET_PROCESS_NAME, "greetInteractive");
      instance.getWidget(name("customComponent")).withDefaultValue("componentName", "OwnedComponent")
         .withDefaultValue("componentSourceUrl", "/owned-extension.js");
      QTableMetaData schedule = new QTableMetaData().withName("ownedSchedule").withLabel("Owned Schedule")
         .withBackendName("memory").withPrimaryKeyField("id").withRecordLabelFields("name")
         .withPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.READ_WRITE_PERMISSIONS))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("cronExpression", QFieldType.STRING).withLabel("Schedule Expression"))
         .withField(new QFieldMetaData("owner", QFieldType.STRING)).withField(new QFieldMetaData("zero", QFieldType.INTEGER))
         .withSection(new QFieldSection("identity", "Identity", null, Tier.T1, List.of("id", "name")))
         .withSection(new QFieldSection("editorFields", "Editor Fields", null, Tier.T2, List.of("cronExpression", "owner", "zero")).withIsHidden(true));
      instance.addTable(schedule);
      instance.addApp(new QAppMetaData().withName("ownedEditors").withLabel("Owned Editors").withChild(schedule));
      instance.getWidgets().put(name("cronUI"), CronUIWidgetRenderer.buildWidgetMetaData(name("cronUI"), "Owned schedule",
         new CronUISetupData().withTableName("ownedSchedule").withCronExpressionFieldName("cronExpression"))
         .withCodeReference(new QCodeReference(Renderer.class))
         .withPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)));
      for(String editor : EDITORS)
      {
         schedule.withSection(new QFieldSection().withName(name(editor)).withLabel("Owned " + editor).withTier(Tier.T2).withWidgetName(name(editor)));
      }
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
      server.withJavalinConfigCustomizer(config ->
      {
         config.routes.get("/owned-extension.js", context -> context.contentType("application/javascript").result(
            "window.OwnedComponent={OwnedComponent:({props})=>'Loaded component: '+props.widgetMetaData.label+' / '+String(props.widgetData.footerHTML ?? 'empty')};"));
         config.routes.get("/owned-embed.html", context -> context.contentType("text/html").result("<h1>Owned embedded chart</h1><p>42 units</p>"));
         config.routes.post("/accounts/{accountId}/embed-url/registered-user", context ->
         {
            providerRequest.set(new JSONObject(context.body()).put("fixtureAccountId", context.pathParam("accountId")));
            if("denied".equals(providerMode))
            {
               context.status(403).contentType("application/json").result("{\"Message\":\"Owned provider denial\",\"RequestId\":\"owned\"}");
            }
            else
            {
               context.contentType("application/json").result(new JSONObject().put("EmbedUrl", "empty".equals(providerMode) ? "" : baseUrl + "/owned-embed.html")
                  .put("Status", 200).put("RequestId", "owned").toString());
            }
         });
         config.routes.after(context ->
         {
            if("malformed".equals(mode) && context.path().equals("/widget/" + selected))
            {
               JSONObject body = new JSONObject(context.result());
               for(String key : List.of("html", "bulletList", "fields", "record", "chartData", "tableDataList", "mapMarkerList", "processMetaData", "url", "footerHTML", "queryOutput", "fieldList", "recordOfFieldValues", "cronDescription"))
               {
                  body.put(key, new JSONObject().put("invalidShape", true));
               }
               context.result(body.toString());
            }
         });
      });
      server.start();
      QContext.init(instance, new QSession());
      for(int id : List.of(1, 2, 3))
      {
         QRecord inserted = new InsertAction().executeForRecord(new InsertInput("ownedSchedule").withRecord(new QRecord()
            .withValue("id", id).withValue("name", "Owned schedule " + id).withValue("cronExpression", id == 3 ? "" : id == 1 ? "0 0 9 * * ?" : "0 0 12 * * ?")
            .withValue("owner", "Owned form value").withValue("zero", 0)));
         assertTrue(inserted.getErrors().isEmpty(), inserted.getErrorsAsString());
      }
      QContext.clear();
      baseUrl = "http://localhost:" + service.get().port();
      System.setProperty("aws.endpointUrl", baseUrl);
      client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
      ChromeOptions options = new ChromeOptions();
      options.addArguments("--headless=new", "--window-size=1600,2200");
      driver = new ChromeDriver(options);
      driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
      wait = new WebDriverWait(driver, Duration.ofSeconds(15));
      Files.createDirectories(output);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void stop()
   {
      try
      {
         if(driver != null)
         {
            driver.quit();
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
         if(previousEndpoint == null)
         {
            System.clearProperty("aws.endpointUrl");
         }
         else
         {
            System.setProperty("aws.endpointUrl", previousEndpoint);
         }
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** A widget grant must not grant read access to its source record.
    *******************************************************************************/
   @Test
   void testCronSourceReadDenial() throws Exception
   {
      assertTrue(json("/widget/ownedcronUI?id=1").getString("cronDescription").contains("9:00 am"));
      sourceAllowed = false;
      for(String type : EDITORS)
      {
         for(String method : List.of("GET", "POST"))
         {
            HttpResponse<String> response = request(method, "/widget/" + name(type) + "?id=1&inputSource=SYSTEM");
            assertEquals(403, response.statusCode(), response.body());
         }
      }
      assertTrue(json("/widget/ownedcronUI?cronExpression=0%200%209%20*%20*%20%3F").getString("cronDescription").contains("9:00 am"));
   }



   /*******************************************************************************
    ** Record widgets display the native typed values and cron description.
    *******************************************************************************/
   @Test
   void testRecordEditors() throws Exception
   {
      driver.get(baseUrl + "/ownedEditors/ownedSchedule/1");
      try
      {
         wait.until(browser -> body().contains("Owned form value") && body().contains("9:00 am") && body().contains("0 0 9 * * ?"));
         assertTrue(driver.findElement(By.id("owneddynamicForm")).getText().contains("Zero"));
         assertTrue(driver.findElement(By.id("owneddynamicForm")).getText().contains("0"));
      }
      finally
      {
         capture("record-editors");
      }
   }



   /*******************************************************************************
    ** User personalization must survive the renderer-to-table call boundary.
    *******************************************************************************/
   @Test
   void testCronPersonalizedVisibility() throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("ownedScheduleRow"));
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(ScheduleReadPolicy.class));
      assertTrue(json("/widget/ownedcronUI?id=1").getString("cronDescription").contains("9:00 am"));
      assertEquals(404, request("GET", "/widget/ownedcronUI?id=2").statusCode());
      assertEquals(404, request("GET", "/widget/ownedcronUI?id=999").statusCode());
      QContext.init(instance, new QSession());
      RenderWidgetInput input = new RenderWidgetInput().withWidgetMetaData(instance.getWidget(name("cronUI")));
      input.setQueryParams(new HashMap<>(Map.of("id", "2")));
      assertTrue(((CronUIWidgetData) new RenderWidgetAction().execute(input).getWidgetData()).getCronDescription().contains("12:00 pm"));
   }



   /*******************************************************************************
    ** Edits cross the real record form, native update, and a refreshed view.
    *******************************************************************************/
   @Test
   void testEditorSave() throws Exception
   {
      driver.get(baseUrl + "/ownedEditors/ownedSchedule/1/edit");
      try
      {
         wait.until(browser -> !browser.findElements(By.id("owner")).isEmpty());
         driver.findElement(By.id("owner")).sendKeys(Keys.chord(Keys.COMMAND, "a"), "Updated owned form");
         driver.findElement(By.xpath("//button[normalize-space()='Advanced']")).click();
         wait.until(browser -> !browser.findElements(By.name("cronExpression")).isEmpty());
         driver.findElement(By.name("cronExpression")).sendKeys(Keys.chord(Keys.COMMAND, "a"), "0 30 10 * * ?");
         wait.until(browser -> body().contains("10:30 am"));
         driver.findElement(By.cssSelector("[data-qqq-id='save']")).click();
         wait.until(browser -> !browser.getCurrentUrl().endsWith("/edit") && body().contains("Updated owned form") && body().contains("10:30 am"));
         driver.navigate().refresh();
         wait.until(browser -> body().contains("Updated owned form") && body().contains("0 30 10 * * ?"));
      }
      finally
      {
         capture("editors-saved");
      }
   }



   /*******************************************************************************
    ** Invalid cron syntax is a typed error; empty forms and failed loads are bounded.
    *******************************************************************************/
   @Test
   void testEditorBoundaryStates() throws Exception
   {
      assertFalse(json("/widget/ownedcronUI?cronExpression=invalid").getString("error").isBlank());
      assertFalse(json("/widget/ownedcronUI").has("cronDescription"));
      assertEquals(404, request("GET", "/widget/ownedcronUI?id=999").statusCode());
      driver.get(baseUrl + "/ownedEditors/ownedSchedule/3");
      wait.until(browser -> body().contains("Owned form value"));
      assertFalse(driver.findElement(By.id("ownedcronUI")).getText().contains("9:00 am"));
      capture("cron-empty");
      JSONArray results = new JSONArray();
      for(String type : EDITORS)
      {
         for(String state : List.of("error", "malformed", "empty"))
         {
            selected = name(type);
            mode = state;
            HttpResponse<String> response = request("GET", "/widget/" + selected + "?id=1");
            assertEquals("error".equals(state) ? 500 : 200, response.statusCode());
            int before = calls.get();
            driver.get(baseUrl + "/ownedEditors/ownedSchedule/1");
            wait.until(browser -> calls.get() >= before + 2);
            Thread.sleep(500);
            String text = body();
            capture(type + "-" + state);
            results.put(new JSONObject().put("type", type).put("state", state).put("status", response.statusCode())
               .put("recordVisible", text.contains("Owned schedule 1")).put("body", text));
            if("malformed".equals(state) && "cronUI".equals(type))
            {
               assertTrue(text.isBlank(), "Deferred #553 malformed cron response limitation");
            }
            else
            {
               assertTrue(text.contains("Owned schedule 1"), text);
            }
         }
      }
      Files.writeString(output.resolve("editor-states.json"), results.toString(2));
   }



   /*******************************************************************************
    ** Content assertions distinguish rendered data from empty card shells.
    *******************************************************************************/
   @Test
   void testPopulatedOutputAndHiddenAlert() throws Exception
   {
      List<String> widgets = new ArrayList<>(TYPES.stream().map(SampleDisplayWidgetsBrowserIT::name).toList());
      widgets.add("SampleStatisticsWidget");
      instance.getApp("SampleWidgetsDashboard").setWidgets(widgets);
      driver.get(baseUrl + "/SampleWidgetsDashboard");
      try
      {
         wait.until(browser -> body().contains("First owned row") && body().contains("Second owned row") && body().contains("98.5%"));
      }
      catch(RuntimeException e)
      {
         capture("populated-failure");
         throw e;
      }
      assertTrue(driver.findElement(By.cssSelector("#ownedalert [role=alert] strong")).getText().contains("Owned warning"));
      assertTrue(driver.findElement(By.cssSelector("#ownedalert li em")).getText().contains("Owned bullet"));
      assertTrue(driver.findElement(By.cssSelector("#owneddivider hr")).isDisplayed());
      String fields = driver.findElement(By.id("ownedfieldValueList")).getText();
      assertTrue(fields.contains("Owner:") && fields.contains("Alice") && fields.contains("Zero:") && fields.contains("0") && fields.contains("Owned choice"), fields);
      WebElement canvas = wait.until(browser -> driver.findElement(By.cssSelector("#ownedhorizontalBarChart canvas")));
      wait.until(browser -> Boolean.TRUE.equals(driver.executeScript("const c=arguments[0], a=c.getContext('2d').getImageData(0,0,c.width,c.height).data; let n=0; for(let i=0;i<a.length;i+=4){if(a[i+3]>180 && Math.max(a[i],a[i+1],a[i+2])-Math.min(a[i],a[i+1],a[i+2])>30){n++;}} return n>100;", canvas)));
      capture("populated");
      selected = name("alert");
      mode = "hidden";
      instance.getApp("SampleWidgetsDashboard").setWidgets(List.of(selected, "SampleStatisticsWidget"));
      openAndAwaitRequests();
      assertTrue(body().contains("98.5%"));
      assertTrue(driver.findElements(By.cssSelector("#ownedalert [role=alert]")).isEmpty());
      capture("hidden-alert");
   }



   /*******************************************************************************
    ** Map output records the fixed-marker limitation, while custom props render.
    *******************************************************************************/
   @Test
   void testMapLimitationAndDynamicComponent() throws Exception
   {
      JSONObject map = json("/widget/ownedusaMap");
      assertEquals("Owned Chicago", map.getJSONArray("mapMarkerList").getJSONObject(0).getString("name"));
      assertEquals(new BigDecimal("41.8781"), map.getJSONArray("mapMarkerList").getJSONObject(0).getBigDecimal("latitude"));
      instance.getApp("SampleWidgetsDashboard").setWidgets(List.of(name("usaMap"), name("customComponent"), "SampleStatisticsWidget"));
      driver.get(baseUrl + "/SampleWidgetsDashboard");
      wait.until(browser -> body().contains("Loaded component: Owned customComponent / Owned component value") && body().contains("98.5%"));
      wait.until(browser -> driver.findElements(By.cssSelector("#ownedusaMap .jvectormap-marker")).size() == 3);
      assertTrue(driver.findElements(By.cssSelector("#ownedusaMap .jvectormap-region")).size() >= 50);
      capture("extension-map-custom");
      selected = name("usaMap");
      mode = "empty";
      openAndAwaitRequests();
      assertFalse(json("/widget/ownedusaMap").has("mapMarkerList"), "Empty collections are omitted by native widget serialization");
      assertEquals(3, driver.findElements(By.cssSelector("#ownedusaMap .jvectormap-marker")).size(), "Deferred #560: supplied empty markers are ignored");
      instance.getWidget(name("customComponent")).withDefaultValue("componentSourceUrl", "/missing-owned-extension.js");
      driver.get(baseUrl + "/SampleWidgetsDashboard");
      wait.until(browser -> body().contains("Error loading OwnedComponent") && body().contains("98.5%"));
      capture("extension-missing-component");
   }



   /*******************************************************************************
    ** The real AWS SDK exchanges protocol messages with the owned local service.
    *******************************************************************************/
   @Test
   void testQuickSightProtocolAndEmbed() throws Exception
   {
      JSONObject data = json("/widget/ownedquickSightChart");
      assertEquals(baseUrl + "/owned-embed.html", data.getString("url"));
      JSONObject request = providerRequest.get();
      assertEquals("000000000000", request.getString("fixtureAccountId"));
      assertEquals("arn:aws:quicksight:us-east-1:000000000000:user/default/owned-user", request.getString("UserArn"));
      assertEquals("owned-dashboard", request.getJSONObject("ExperienceConfiguration").getJSONObject("Dashboard").getString("InitialDashboardId"));
      assertFalse(json("/metaData").toString().contains("owned-fixture-secret"));
      selected = name("quickSightChart");
      instance.getApp("SampleWidgetsDashboard").setWidgets(List.of(selected, "SampleStatisticsWidget"));
      openAndAwaitRequests();
      WebElement iframe = wait.until(browser -> driver.findElement(By.cssSelector("#ownedquickSightChart iframe")));
      wait.until(browser -> (baseUrl + "/owned-embed.html").equals(iframe.getAttribute("src")));
      driver.switchTo().frame(iframe);
      wait.until(browser -> body().contains("Owned embedded chart") && body().contains("42 units"));
      driver.switchTo().defaultContent();
      assertTrue(body().contains("98.5%"));
      capture("extension-quicksight");
      providerMode = "denied";
      assertEquals(500, request("GET", "/widget/ownedquickSightChart").statusCode());
      openAndAwaitRequests();
      assertTrue(body().contains("98.5%"));
      capture("extension-provider-denied");
      providerMode = "empty";
      assertEquals("", json("/widget/ownedquickSightChart").optString("url"));
      openAndAwaitRequests();
      assertTrue(body().contains("98.5%"));
      capture("extension-provider-empty");
   }



   /*******************************************************************************
    ** A widget starts the native sample process and observes its result and denial.
    *******************************************************************************/
   @Test
   void testProcessWidgetRoundTripAndDenial() throws Exception
   {
      JSONObject data = json("/widget/ownedprocess?greetingPrefix=Owned");
      assertEquals("greetInteractive", data.getJSONObject("processMetaData").getString("name"));
      assertEquals("Owned", data.getJSONObject("defaultValues").getString("greetingPrefix"));
      selected = name("process");
      instance.getApp("SampleWidgetsDashboard").setWidgets(List.of(selected, "SampleStatisticsWidget"));
      driver.get(baseUrl + "/SampleWidgetsDashboard?recordIds=1");
      wait.until(browser -> !driver.findElements(By.id("greetingPrefix")).isEmpty());
      driver.findElement(By.id("greetingPrefix")).sendKeys("Widget");
      driver.findElement(By.id("greetingSuffix")).sendKeys("checked");
      driver.findElement(By.cssSelector("#ownedprocess [data-qqq-id='submit']")).click();
      wait.until(browser -> body().contains("Widget Avery checked"));
      assertTrue(body().contains("98.5%"));
      capture("extension-process-result");
      instance.getProcess("greetInteractive").withPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      assertEquals(403, request("POST", "/processes/greetInteractive/init").statusCode());
      openAndAwaitRequests();
      wait.until(browser -> body().contains("98.5%"));
      assertTrue(driver.findElements(By.id("greetingPrefix")).isEmpty());
      capture("extension-process-denied");
      instance.getWidget(selected).withDefaultValue(ProcessWidgetRenderer.WIDGET_PROCESS_NAME, "missingOwnedProcess");
      assertFalse(json("/widget/ownedprocess").has("processMetaData"));
   }



   /*******************************************************************************
    ** External and extension failures preserve native widget access boundaries.
    *******************************************************************************/
   @Test
   void testExtensionEmptyErrorAndMalformedStates() throws Exception
   {
      JSONArray results = new JSONArray();
      for(String state : List.of("empty", "error", "malformed"))
      {
         mode = state;
         for(String type : EXTENSIONS)
         {
            selected = name(type);
            providerMode = "empty".equals(state) ? "empty" : "populated";
            instance.getApp("SampleWidgetsDashboard").setWidgets(List.of(selected, "SampleStatisticsWidget"));
            openAndAwaitRequests();
            JSONObject result = new JSONObject().put("type", type).put("state", state).put("healthy", body().contains("98.5%"));
            results.put(result);
            Files.writeString(output.resolve("extensions-states.json"), results.toString(2));
            capture("extension-" + state + "-" + type);
            assertEquals("error".equals(state) ? 500 : 200, request("GET", "/widget/" + selected).statusCode());
            assertTrue(result.getBoolean("healthy"), result.toString());
         }
      }
   }



   /*******************************************************************************
    ** Child records are rendered in their native parent-record context.
    *******************************************************************************/
   @Test
   void testChildRecordGridAndStates() throws Exception
   {
      selected = name("childRecordList");
      driver.get(baseUrl + "/peopleApp/greetingsApp/person/1");
      wait.until(browser -> driver.findElement(By.id(selected)).getText().contains("Charlie") && driver.findElement(By.id(selected)).getText().contains("Coco"));
      assertFalse(driver.findElement(By.id(selected)).getText().contains("Louie"));
      capture("children-grid");
      driver.findElement(By.xpath("//*[@id='ownedchildRecordList']//*[text()='Charlie']")).click();
      wait.until(browser -> driver.getCurrentUrl().endsWith("/pet/1") && body().contains("Charlie"));
      capture("children-record-link");
      driver.get(baseUrl + "/peopleApp/greetingsApp/person/4");
      wait.until(browser -> body().contains("Drew") && !driver.findElements(By.id(selected)).isEmpty());
      wait.until(browser -> Boolean.TRUE.equals(driver.executeScript("return performance.getEntriesByType('resource').some(e => e.name.includes('/widget/' + arguments[0]));", selected)));
      driver.executeAsyncScript("const done=arguments[arguments.length-1]; requestAnimationFrame(() => requestAnimationFrame(done));");
      assertFalse(driver.findElement(By.id(selected)).getText().contains("Charlie"));
      capture("children-empty");
      for(String state : List.of("error", "malformed"))
      {
         mode = state;
         driver.get(baseUrl + "/peopleApp/greetingsApp/person/1");
         wait.until(browser -> body().contains("Avery") && Boolean.TRUE.equals(driver.executeScript("return performance.getEntriesByType('resource').some(e => e.name.includes('/widget/' + arguments[0]));", selected)));
         driver.executeAsyncScript("const done=arguments[arguments.length-1]; requestAnimationFrame(() => requestAnimationFrame(done));");
         assertTrue(body().contains("Avery"));
         capture("children-" + state);
      }
   }



   /*******************************************************************************
    ** Native values and permission denial apply to each owned renderer contract.
    *******************************************************************************/
   @Test
   void testTypedPayloadsPermissionsAndReferences() throws Exception
   {
      assertEquals("WARNING", json("/widget/ownedalert").getString("alertType"));
      assertEquals("divider", json("/widget/owneddivider").getString("type"));
      JSONObject fields = json("/widget/ownedfieldValueList");
      assertEquals(0, fields.getJSONObject("record").getJSONObject("values").getInt("zero"));
      assertEquals("Owned choice", fields.getJSONObject("record").getJSONObject("displayValues").getString("choice"));
      JSONObject chart = json("/widget/ownedhorizontalBarChart").getJSONObject("chartData");
      assertEquals(List.of("First", "Zero", "Negative"), chart.getJSONArray("labels").toList());
      assertEquals(List.of(5, 0, -2), chart.getJSONArray("datasets").getJSONObject(0).getJSONArray("data").toList());
      assertEquals(2, json("/widget/ownedmultiTable").getJSONArray("tableDataList").length());
      JSONObject location = json("/widget/ownedlocation");
      assertEquals("Owned location", location.getString("title"));
      assertEquals("Owned address", location.getString("location"));
      assertEquals("Owned footer", location.getString("footerText"));
      assertEquals("/favicon.ico", location.getString("imageUrl"));
      for(String type : allTypes())
      {
         String widget = name(type);
         allowed = false;
         assertFalse(json("/metaData").getJSONObject("widgets").has(widget));
         int before = calls.get();
         for(String method : List.of("GET", "POST"))
         {
            assertEquals(403, request(method, "/widget/" + widget).statusCode());
         }
         assertEquals(before, calls.get());
         allowed = true;
         assertTrue(json("/metaData").getJSONObject("widgets").has(widget));
         assertEquals(200, request("POST", "/widget/" + widget).statusCode());
         for(QCodeReference reference : List.of(new QCodeReference("com.kingsrook.sampleapp.MissingDisplayRenderer", QCodeType.JAVA), new QCodeReference(String.class)))
         {
            instance.getWidget(widget).setCodeReference(reference);
            HttpResponse<String> response = request("GET", "/widget/" + widget);
            assertEquals(500, response.statusCode(), widget);
            assertFalse(new JSONObject(response.body()).optString("error").isBlank());
         }
         instance.getWidget(widget).setCodeReference(new QCodeReference(Renderer.class));
      }
   }



   /*******************************************************************************
    ** Empty values and actual renderer failures retain the healthy neighbor.
    *******************************************************************************/
   @Test
   void testEmptyAndFailedWidgets() throws Exception
   {
      for(String state : List.of("empty", "error"))
      {
         JSONArray results = exercise(state);
         for(int i = 0; i < results.length(); i++)
         {
            assertTrue(results.getJSONObject(i).getBoolean("healthy"), results.getJSONObject(i).toString());
         }
      }
   }



   /*******************************************************************************
    ** Known malformed-response limits are recorded, not robustness certification.
    *******************************************************************************/
   @Test
   void testDeferredMalformedWidgetLimitations() throws Exception
   {
      JSONArray results = exercise("malformed");
      for(int i = 0; i < results.length(); i++)
      {
         JSONObject result = results.getJSONObject(i);
         assertEquals(List.of("divider", "horizontalBarChart").contains(result.getString("type")), result.getBoolean("healthy"), result.toString());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JSONArray exercise(String state) throws Exception
   {
      mode = state;
      JSONArray results = new JSONArray();
      for(String type : TYPES)
      {
         selected = name(type);
         instance.getApp("SampleWidgetsDashboard").setWidgets(List.of(selected, "SampleStatisticsWidget"));
         openAndAwaitRequests();
         JSONObject result = new JSONObject().put("type", type).put("state", state).put("healthy", body().contains("98.5%"));
         result.put("status", driver.executeScript("return performance.getEntriesByType('resource').find(e => e.name.includes('/widget/' + arguments[0])).responseStatus;", selected));
         assertEquals("error".equals(state) ? 500 : 200, result.getInt("status"));
         result.put("text", body());
         results.put(result);
         capture(state + "-" + type);
         Files.writeString(output.resolve(state + ".json"), results.toString(2));
      }
      return results;
   }



   /*******************************************************************************
    ** Wait for actual native responses and the resulting React update.
    *******************************************************************************/
   private void openAndAwaitRequests()
   {
      driver.get(baseUrl + "/SampleWidgetsDashboard");
      wait.until(browser -> Boolean.TRUE.equals(driver.executeScript("return [arguments[0], 'SampleStatisticsWidget'].every(n => performance.getEntriesByType('resource').some(e => e.name.includes('/widget/' + n)));", selected)));
      driver.executeAsyncScript("const done=arguments[arguments.length-1]; requestAnimationFrame(() => requestAnimationFrame(done));");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void capture(String name) throws Exception
   {
      Files.write(output.resolve(name + ".png"), driver.getScreenshotAs(OutputType.BYTES));
      Files.writeString(output.resolve(name + ".html"), driver.getPageSource());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String body()
   {
      return driver.findElement(By.tagName("body")).getText();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpResponse<String> request(String method, String path) throws Exception
   {
      return client.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(15))
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
   private static String name(String type)
   {
      return "owned" + type;
   }



   /*******************************************************************************
    ** Location is a backend data contract, not a standalone Material renderer.
    *******************************************************************************/
   private static List<String> allTypes()
   {
      List<String> types = new ArrayList<>(TYPES);
      types.add("location");
      types.add("childRecordList");
      types.addAll(EXTENSIONS);
      types.addAll(EDITORS);
      return types;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class Permissions implements QAuthenticationModuleCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         if(allowed)
         {
            session.withPermissions(allTypes().stream().map(type -> name(type) + ".hasAccess").toArray(String[]::new));
            session.withPermission("ownedSchedule.write");
            session.withSecurityKeyValue("ownedScheduleRow", 1);
            if(sourceAllowed)
            {
               session.withPermission("ownedSchedule.read");
            }
         }
      }
   }



   /*******************************************************************************
    ** Only user reads are narrowed; trusted internal behavior remains observable.
    *******************************************************************************/
   public static class ScheduleReadPolicy implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(input.getInputSource() == QInputSource.USER && "ownedSchedule".equals(input.getTableName()))
         {
            return input.getTable().clone().withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("ownedScheduleRow")
               .withFieldName("id").withLockScope(RecordSecurityLock.LockScope.READ).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
         }
         return input.getTable();
      }
   }



   /*******************************************************************************
    ** Plain custom props are consumed by the first-party dynamically loaded module.
    *******************************************************************************/
   public static class OwnedComponentData extends QWidgetData
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getType()
      {
         return "customComponent";
      }
   }



   /*******************************************************************************
    ** Native data models deliberately use fixed, owned display values.
    *******************************************************************************/
   public static class Renderer extends AbstractWidgetRenderer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public RenderWidgetOutput render(RenderWidgetInput input) throws QException
      {
         calls.incrementAndGet();
         boolean selectedHere = input.getWidgetMetaData().getName().equals(selected);
         if(selectedHere && "error".equals(mode))
         {
            throw new QException("Owned display renderer failure");
         }
         boolean empty = selectedHere && "empty".equals(mode);
         QWidgetData data;
         switch(input.getWidgetMetaData().getType())
         {
            case "alert" -> data = empty ? new AlertData() : new AlertData(AlertData.AlertType.WARNING, "<strong>Owned warning</strong>")
               .withBulletList(List.of("<em>Owned bullet</em>")).withHideWidget(selectedHere && "hidden".equals(mode));
            case "divider" -> data = new DividerWidgetData();
            case "fieldValueList" ->
            {
               FieldValueListData fields = new FieldValueListData();
               if(!empty)
               {
                  fields.addFieldWithValue("owner", QFieldType.STRING, "Alice").withLabel("Owner");
                  fields.addFieldWithValue("zero", QFieldType.INTEGER, 0).withLabel("Zero");
                  fields.addFieldWithValue("choice", QFieldType.INTEGER, 1, "Owned choice").withLabel("Choice");
               }
               data = fields;
            }
            case "horizontalBarChart" -> data = new ChartData("Owned horizontal", "Owned data", "Owned series",
               empty ? List.of() : List.of("First", "Zero", "Negative"), empty ? List.of() : List.of(5, 0, -2)).withHeight(240);
            case "multiTable" -> data = new MultiTableData(empty ? List.of() : List.of(table("First", "First owned row"), table("Second", "Second owned row")));
            case "cronUI" ->
            {
               return new CronUIWidgetRenderer().render(input);
            }
            case "dynamicForm" ->
            {
               QRecord values = new QRecord().withValue("owner", "Owned form value").withValue("zero", 0);
               if(input.getQueryParams().containsKey("id"))
               {
                  GetInput get = new GetInput("ownedSchedule").withPrimaryKey(input.getQueryParams().get("id"));
                  get.setInputSource(input.getInputSource());
                  PermissionsHelper.checkTablePermissionThrowing(get, TablePermissionSubType.READ);
                  values = new GetAction().executeForRecord(get);
               }
               data = new DynamicFormWidgetData()
                  .withFieldList(empty ? List.of() : List.of(new QFieldMetaData("owner", QFieldType.STRING).withLabel("Owner"), new QFieldMetaData("zero", QFieldType.INTEGER).withLabel("Zero")))
                  .withRecordOfFieldValues(values).withNoFieldsMessage("No owned fields");
            }
            case "childRecordList" ->
            {
               return new ChildRecordListRenderer().render(input);
            }
            case "usaMap" -> data = new USMapWidgetData().withHeight("300px").withMapMarkerList(empty ? List.of() : List.of(
               new USMapWidgetData.MapMarker("Owned Chicago", new BigDecimal("41.8781"), new BigDecimal("-87.6298"))));
            case "quickSightChart" ->
            {
               return new QuickSightChartRenderer().render(input);
            }
            case "process" ->
            {
               return empty ? new RenderWidgetOutput(new ProcessWidgetData()) : new ProcessWidgetRenderer().render(input);
            }
            case "customComponent" -> data = new OwnedComponentData().withFooterHTML(empty ? "" : "Owned component value");
            case "location" -> data = new LocationData("/favicon.ico", "Owned location", "Owned description", "Owned address", "Owned footer");
            default -> throw new QException("Unexpected owned display type");
         }
         return new RenderWidgetOutput(data);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private TableData table(String label, String value)
      {
         return new TableData(label, List.of(new TableData.Column("default", "Name", "name", "100%", "left")), List.of(Map.of("name", value)));
      }
   }
}
