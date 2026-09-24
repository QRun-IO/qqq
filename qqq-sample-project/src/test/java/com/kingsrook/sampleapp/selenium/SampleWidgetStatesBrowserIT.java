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


import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.ChartData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CompositeWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.MultiStatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RawHTML;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.StatisticsData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.StepperData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.sampleapp.SampleJavalinServer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Owned empty/error fixtures retain the canonical metadata and renderer types.
 *******************************************************************************/
class SampleWidgetStatesBrowserIT
{
   private static final Map<String, QCodeReference> originalRenderers = new HashMap<>();
   private static volatile String selectedWidget;
   private static volatile String mode;
   private QInstance instance;
   private List<String> widgets;
   private SampleJavalinServer server;
   private ChromeDriver driver;
   private String baseUrl;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      originalRenderers.clear();
      selectedWidget = "";
      mode = "";
      instance = SampleMetaDataProvider.defineTestInstance();
      widgets = List.copyOf(instance.getApp("SampleWidgetsDashboard").getWidgets());
      for(String name : widgets)
      {
         originalRenderers.put(name, instance.getWidget(name).getCodeReference());
         instance.getWidget(name).setCodeReference(new QCodeReference(StateRenderer.class));
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
      server.withJavalinConfigCustomizer(config -> config.routes.after(context ->
      {
         if("malformed".equals(mode) && context.path().equals("/widget/" + selectedWidget))
         {
            JSONObject body = new JSONObject(context.result());
            for(String key : List.of("chartData", "statisticsGroupData", "rows", "columns", "steps", "blocks", "html", "count"))
            {
               body.put(key, new JSONObject().put("invalidShape", true));
            }
            context.result(body.toString());
         }
      }));
      server.start();
      baseUrl = "http://localhost:" + service.get().port();
      ChromeOptions options = new ChromeOptions();
      options.addArguments("--headless=new", "--window-size=1440,1000");
      driver = new ChromeDriver(options);
      driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
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
         if(server != null)
         {
            server.stop();
         }
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** Empty typed values must not take down the neighbouring healthy widget.
    *******************************************************************************/
   @Test
   void testEmptyWidgetStates() throws Exception
   {
      JSONArray results = exercise("empty");
      for(int i = 0; i < results.length(); i++)
      {
         JSONObject result = results.getJSONObject(i);
         assertTrue(result.getBoolean("healthyControlVisible"), result.toString());
      }
   }



   /*******************************************************************************
    ** A renderer exception is a real failed widget request, not fake browser data.
    *******************************************************************************/
   @Test
   void testFailedWidgetStates() throws Exception
   {
      JSONArray results = exercise("error");
      for(int i = 0; i < results.length(); i++)
      {
         JSONObject result = results.getJSONObject(i);
         assertTrue(result.getBoolean("healthyControlVisible"), result.toString());
      }
   }



   /*******************************************************************************
    ** Deliberately malformed wire shapes isolate client contract handling.
    *******************************************************************************/
   @Test
   void testDeferredMalformedWidgetLimitations() throws Exception
   {
      JSONArray results = exercise("malformed");
      assertEquals(11, results.length());
      for(int i = 0; i < results.length(); i++)
      {
         JSONObject result = results.getJSONObject(i);
         Boolean expectedVisible = !List.of("SampleBigNumberBlocksWidget", "SampleMultiStatisticsWidget", "SampleTableWidget", "SampleStepperWidget", "SampleHTMLWidget").contains(result.getString("widget"));
         assertEquals(expectedVisible, result.getBoolean("healthyControlVisible"), result.toString());
      }
   }



   /*******************************************************************************
    ** Each navigation isolates one changed widget beside a canonical control.
    *******************************************************************************/
   private JSONArray exercise(String state) throws Exception
   {
      mode = state;
      JSONArray results = new JSONArray();
      Path output = Path.of("target", "widget-states");
      Files.createDirectories(output);
      for(String name : widgets)
      {
         selectedWidget = name;
         String control = name.equals("SampleHTMLWidget") ? "SampleStatisticsWidget" : "SampleHTMLWidget";
         String controlText = name.equals("SampleHTMLWidget") ? "98.5%" : "Purely Custom";
         instance.getApp("SampleWidgetsDashboard").setWidgets(List.of(name, control));
         driver.get(baseUrl + "/SampleWidgetsDashboard");
         new WebDriverWait(driver, Duration.ofSeconds(15)).until(browser -> Boolean.TRUE.equals(driver.executeScript(
            "return [arguments[0], arguments[1]].every(name => performance.getEntriesByType('resource').some(e => e.name.includes('/widget/' + name)));", name, control)));
         driver.executeAsyncScript("const done = arguments[arguments.length - 1]; requestAnimationFrame(() => requestAnimationFrame(done));");
         JSONObject result = new JSONObject();
         result.put("widget", name);
         result.put("state", state);
         result.put("httpStatus", driver.executeScript("return performance.getEntriesByType('resource').find(e => e.name.includes('/widget/' + arguments[0])).responseStatus;", name));
         assertEquals(state.equals("error") ? 500 : 200, result.getInt("httpStatus"), name);
         result.put("healthyControlVisible", driver.findElement(By.tagName("body")).getText().contains(controlText));
         result.put("text", driver.executeScript("return document.getElementById(arguments[0])?.innerText || '';", name));
         result.put("skeletons", driver.executeScript("return document.getElementById(arguments[0])?.querySelectorAll('.MuiSkeleton-root').length || 0;", name));
         result.put("alerts", driver.executeScript("return document.getElementById(arguments[0])?.querySelectorAll('[role=alert]').length || 0;", name));
         result.put("canvasCount", driver.executeScript("return document.getElementById(arguments[0])?.querySelectorAll('canvas').length || 0;", name));
         results.put(result);
         Files.writeString(output.resolve(state + ".json"), results.toString(2));
         if(!result.getBoolean("healthyControlVisible") || name.equals("SampleHTMLWidget"))
         {
            Files.write(output.resolve(state + "-" + name + ".png"), driver.getScreenshotAs(OutputType.BYTES));
         }
      }
      return results;
   }



   /*******************************************************************************
    ** Delegate to the unchanged sample renderer, then provide valid empty values.
    *******************************************************************************/
   public static class StateRenderer extends AbstractWidgetRenderer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public RenderWidgetOutput render(RenderWidgetInput input) throws QException
      {
         String name = input.getWidgetMetaData().getName();
         if(name.equals(selectedWidget) && "error".equals(mode))
         {
            throw new QException("Owned widget renderer failure");
         }
         RenderWidgetOutput output = QCodeLoader.getAdHoc(AbstractWidgetRenderer.class, originalRenderers.get(name)).render(input);
         if(!name.equals(selectedWidget) || !"empty".equals(mode))
         {
            return output;
         }
         QWidgetData data = output.getWidgetData();
         if(data instanceof ChartData chart)
         {
            chart.setChartData(new ChartData.Data().withLabels(List.of()).withDatasets(List.of()));
         }
         else if(data instanceof StatisticsData statistics)
         {
            statistics.setCount(0);
            statistics.setPercentageAmount(0);
         }
         else if(data instanceof RawHTML html)
         {
            html.setHtml("");
         }
         else if(data instanceof TableData table)
         {
            table.setRows(List.of());
         }
         else if(data instanceof StepperData stepper)
         {
            stepper.setSteps(List.of());
         }
         else if(data instanceof MultiStatisticsData statistics)
         {
            statistics.setStatisticsGroupData(List.of());
         }
         else if(data instanceof CompositeWidgetData composite)
         {
            composite.setBlocks(List.of());
         }
         return output;
      }
   }
}
