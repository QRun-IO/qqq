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
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ParentWidgetRenderer;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.CompositeWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.QWidgetData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RawHTML;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.bignumberblock.BigNumberBlockData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.button.ButtonBlockData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.button.ButtonValues;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.inputfield.InputFieldBlockData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.inputfield.InputFieldValues;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReferenceLambda;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.ParentWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.WidgetDropdownData;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.WidgetDropdownType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.CollapsibleMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QComponentType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendComponentMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.sampleapp.SampleJavalinServer;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Browser controls use native widget requests and a real downloaded CSV file.
 *******************************************************************************/
class SampleWidgetControlsBrowserIT
{
   private static final AtomicInteger calls = new AtomicInteger();
   private static final AtomicReference<String> submittedMessage = new AtomicReference<>();
   private static final AtomicReference<String> submittedAction = new AtomicReference<>();
   private static volatile boolean malformedBlock;
   private static volatile boolean enterBlock;
   private static volatile boolean emptyExport;
   private static volatile boolean fail;
   private QInstance instance;
   private SampleJavalinServer server;
   private ChromeDriver driver;
   private WebDriverWait wait;
   private String baseUrl;
   private Path downloads;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      calls.set(0);
      submittedMessage.set(null);
      submittedAction.set(null);
      malformedBlock = false;
      enterBlock = false;
      emptyExport = false;
      fail = false;
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.addPossibleValueSource(new QPossibleValueSource().withName("ownedChoice").withType(QPossibleValueSourceType.ENUM)
         .withEnumValues(List.of(new QPossibleValue<>("alpha", "Alpha"), new QPossibleValue<>("beta", "Beta"))));
      instance.addWidget(new QWidgetMetaData().withName("ownedControlValues").withType("html").withLabel("Selected Values")
         .withCodeReference(new QCodeReference(ControlsRenderer.class)));
      instance.addWidget(new ParentWidgetMetaData().withChildWidgetNameList(List.of("ownedControlValues"))
         .withName("ownedControls").withType("parentWidget").withLabel("Owned Controls")
         .withIsCard(true).withGridColumns(12).withTooltip("Owned widget help")
         .withShowReloadButton(true).withShowExportButton(true).withStoreDropdownSelections(true)
         .withCollapsible(CollapsibleMetaData.INITIALLY_OPEN)
         .withDropdown(new WidgetDropdownData().withPossibleValueSourceName("ownedChoice").withLabel("Choice").withIsRequired(true))
         .withDropdown(new WidgetDropdownData().withName("ownedDate").withLabel("Day").withType(WidgetDropdownType.DATE_PICKER).withIsRequired(true))
         .withCodeReference(new QCodeReference(ControlsRenderer.class)));
      instance.getApp("SampleWidgetsDashboard").setWidgets(List.of("ownedControls", "SampleStatisticsWidget"));
      instance.addWidget(new QWidgetMetaData().withName("ownedBlockWidget").withType("composite").withLabel("Owned Blocks")
         .withCodeReference(new QCodeReference(BlocksRenderer.class)));
      QFieldMetaData message = new QFieldMetaData("ownedMessage", QFieldType.STRING).withLabel("Owned message").withIsRequired(true);
      QProcessMetaData process = new QProcessMetaData().withName("ownedBlockProcess").withLabel("Owned Block Process")
         .withStep(new QFrontendStepMetaData().withName("input").withFormField(message)
            .withComponent(new QFrontendComponentMetaData().withType(QComponentType.WIDGET).withValue("widgetName", "ownedBlockWidget")))
         .withStep(new QBackendStepMetaData().withName("capture")
            .withInputData(new QFunctionInputMetaData().withField(new QFieldMetaData("ownedMessage", QFieldType.STRING)).withField(new QFieldMetaData("actionCode", QFieldType.STRING)))
            .withCode(new QCodeReferenceLambda<BackendStep>((in, out) ->
            {
               submittedAction.set(in.getValueString("actionCode"));
               submittedMessage.set(in.getValueString("ownedMessage"));
            })));
      instance.addProcess(process);
      instance.getApp("SampleWidgetsDashboard").withChild(process);
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
      baseUrl = "http://localhost:" + service.get().port();
      downloads = Files.createTempDirectory(Path.of("target").toAbsolutePath(), "widget-downloads-");
      ChromeOptions options = new ChromeOptions();
      options.addArguments("--headless=new", "--window-size=1600,1200");
      options.setExperimentalOption("prefs", Map.of("download.default_directory", downloads.toString(), "download.prompt_for_download", false));
      driver = new ChromeDriver(options);
      driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
      wait = new WebDriverWait(driver, Duration.ofSeconds(15));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void stop(TestInfo testInfo) throws Exception
   {
      try
      {
         if(driver != null)
         {
            Path output = Path.of("target", "widget-controls");
            Files.createDirectories(output);
            String name = testInfo.getTestMethod().orElseThrow().getName();
            Files.write(output.resolve(name + ".png"), driver.getScreenshotAs(OutputType.BYTES));
            Files.writeString(output.resolve(name + ".html"), driver.getPageSource());
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
    ** Selections reach the renderer and survive refresh; CSV bytes match values.
    *******************************************************************************/
   @Test
   void testSelectionsPersistenceCsvAndDeferredCollapse() throws Exception
   {
      open();
      wait.until(browser -> body().contains("Please select a Choice"));
      driver.findElement(By.cssSelector("#ownedControls input[role=combobox]")).click();
      wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@role='option' and text()='Beta']"))).click();
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#ownedControls button[aria-label^='Choose date']"))).click();
      wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[text()='Today']"))).click();
      wait.until(browser -> driver.executeScript("return localStorage.getItem('qqq.widgets.dropdownData.ownedControls.ownedDate');") != null && body().contains("choice=beta; day="));
      String selectedDate = new JSONObject((String) driver.executeScript("return localStorage.getItem('qqq.widgets.dropdownData.ownedControls.ownedDate');")).getString("id");
      assertFalse(selectedDate.isBlank());
      assertTrue(body().contains("day=" + selectedDate));
      assertEquals("beta", new JSONObject((String) driver.executeScript("return localStorage.getItem('qqq.widgets.dropdownData.ownedControls.ownedChoice');")).getString("id"));
      int before = calls.get();
      icon("refresh").click();
      wait.until(browser -> calls.get() > before && body().contains("day=" + selectedDate));
      icon("save_alt").click();
      Path csv = wait.until(browser ->
      {
         try(var files = Files.list(downloads))
         {
            return files.filter(path -> path.toString().endsWith(".csv")).findFirst().orElse(null);
         }
         catch(Exception e)
         {
            throw new IllegalStateException(e);
         }
      });
      assertEquals("\"Label\",\"Value\"\n\"A,\"\"B\"\"\",7\n\"Beta\",\"0\"\n", Files.readString(csv));
      driver.navigate().refresh();
      wait.until(browser -> body().contains("choice=beta; day=" + selectedDate));
      WebElement heading = driver.findElement(By.xpath("//*[@id='ownedControls']//h6[contains(.,'Owned Controls')]"));
      new Actions(driver).moveToElement(heading).perform();
      wait.until(browser -> body().contains("Owned widget help"));
      assertTrue(instance.getWidget("ownedControls").getCollapsible().getIsCollapsible());
      assertNull(driver.executeAsyncScript("const done = arguments[arguments.length - 1]; fetch('/metaData').then(r => r.json()).then(data => done(data.widgets.ownedControls.collapsible ?? null));"));
      heading.click();
      driver.executeAsyncScript("const done = arguments[arguments.length - 1]; requestAnimationFrame(() => requestAnimationFrame(done));");
      assertTrue(body().contains("choice=beta"));
      assertNull(driver.executeScript("return localStorage.getItem('qqq.widget.collapsibleOpenState.ownedControls');"));
      driver.navigate().refresh();
      wait.until(browser -> body().contains("choice=beta; day=" + selectedDate));
   }



   /*******************************************************************************
    ** A stale saved choice and absent export data retain the neighboring widget.
    *******************************************************************************/
   @Test
   void testInvalidSavedChoiceAndEmptyExport() throws Exception
   {
      open();
      driver.executeScript("localStorage.setItem('qqq.widgets.dropdownData.ownedControls.ownedChoice', JSON.stringify({id:'removed',label:'Old choice'}));");
      emptyExport = true;
      driver.navigate().refresh();
      wait.until(browser -> body().contains("98.5%") && !driver.findElements(By.cssSelector("#ownedControls input[role=combobox]")).isEmpty());
      assertEquals("", driver.findElement(By.cssSelector("#ownedControls input[role=combobox]")).getAttribute("value"));
      icon("save_alt").click();
      assertEquals("There is no data available to export.", wait.until(ExpectedConditions.alertIsPresent()).getText());
      driver.switchTo().alert().accept();
      assertTrue(body().contains("98.5%"));
   }



   /*******************************************************************************
    ** Failed and denied widgets preserve the healthy dashboard content.
    *******************************************************************************/
   @Test
   void testFailedAndDeniedControls() throws Exception
   {
      fail = true;
      instance.getWidget("ownedControls").setType("html");
      open();
      wait.until(browser -> body().contains("An error occurred loading widget content."));
      assertTrue(body().contains("98.5%"));
      int before = calls.get();
      instance.getWidget("ownedControls").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      driver.navigate().refresh();
      wait.until(browser -> body().contains("98.5%"));
      assertTrue(driver.findElements(By.id("ownedControls")).isEmpty());
      assertEquals(before, calls.get());
   }



   /*******************************************************************************
    ** Non-parent storage is a recorded limitation, not persistence acceptance.
    *******************************************************************************/
   @Test
   void testDeferredNonParentSelectionPersistence() throws Exception
   {
      instance.getWidget("ownedControls").setType("html");
      open();
      wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#ownedControls input[role=combobox]"))).click();
      wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@role='option' and text()='Beta']"))).click();
      wait.until(browser -> driver.findElement(By.cssSelector("#ownedControls input[role=combobox]")).getAttribute("value").equals("Beta"));
      assertNull(driver.executeScript("return localStorage.getItem('qqq.widgets.dropdownData.ownedControls.ownedChoice');"));
      driver.navigate().refresh();
      wait.until(browser -> body().contains("98.5%") && !driver.findElements(By.cssSelector("#ownedControls input[role=combobox]")).isEmpty());
      assertEquals("", driver.findElement(By.cssSelector("#ownedControls input[role=combobox]")).getAttribute("value"));
   }



   /*******************************************************************************
    ** Button and Enter callbacks submit actual values to a native backend step.
    *******************************************************************************/
   @Test
   void testBlockCallbacks() throws Exception
   {
      for(boolean enter : List.of(false, true))
      {
         submittedMessage.set(null);
         submittedAction.set(null);
         enterBlock = enter;
         driver.get(baseUrl + "/SampleWidgetsDashboard/ownedBlockProcess");
         if(enter)
         {
            WebElement field = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("input[name=ownedMessage]")));
            Files.createDirectories(Path.of("target", "widget-controls"));
            Files.writeString(Path.of("target", "widget-controls", "input-block-label.html"), driver.getPageSource());
            field.sendKeys(Keys.ENTER);
            driver.executeAsyncScript("const done = arguments[arguments.length - 1]; requestAnimationFrame(() => requestAnimationFrame(done));");
            assertNull(submittedMessage.get());
            field.sendKeys("By enter", Keys.ENTER);
            wait.until(browser -> "By enter".equals(submittedMessage.get()));
            assertNull(submittedAction.get());
         }
         else
         {
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[contains(.,'Submit owned')]"))).click();
            wait.until(browser -> "owned-submit".equals(submittedAction.get()));
            assertNull(submittedMessage.get());
         }
      }
   }



   /*******************************************************************************
    ** An unsupported block warns in its card without losing neighboring content.
    *******************************************************************************/
   @Test
   void testMalformedBlockType() throws Exception
   {
      malformedBlock = true;
      instance.getApp("SampleWidgetsDashboard").setWidgets(List.of("ownedBlockWidget", "SampleStatisticsWidget"));
      open();
      wait.until(browser -> body().contains("Unsupported block type: OWNED_UNKNOWN"));
      assertTrue(body().contains("98.5%"));
   }



   /*******************************************************************************
    ** Native parent layouts and live tab selection; saved-tab restoration is deferred.
    *******************************************************************************/
   @Test
   void testParentGridTabsAndStoredSelection() throws Exception
   {
      ParentWidgetMetaData parent = (ParentWidgetMetaData) instance.getWidget("ownedControls");
      parent.setDropdowns(List.of());
      instance.addWidget(new QWidgetMetaData().withName("ownedSecondChild").withType("html").withLabel("Second Child")
         .withDefaultValue("ownedChoice", "second").withCodeReference(new QCodeReference(ControlsRenderer.class)));
      parent.setChildWidgetNameList(List.of("ownedControlValues", "ownedSecondChild"));
      open();
      wait.until(browser -> body().contains("choice=second"));
      assertTrue(driver.findElement(By.id("ownedControlValues")).isDisplayed());
      assertTrue(driver.findElement(By.id("ownedSecondChild")).isDisplayed());
      parent.setLayoutType(ParentWidgetMetaData.LayoutType.TABS);
      driver.navigate().refresh();
      WebElement second = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@role='tab' and text()='Second Child']")));
      assertTrue(driver.findElements(By.id("ownedSecondChild")).stream().noneMatch(WebElement::isDisplayed));
      second.click();
      wait.until(browser -> body().contains("choice=second"));
      assertEquals("1", driver.executeScript("return localStorage.getItem('qqq.widgets.selectedTabs.ownedControls');"));
      driver.navigate().refresh();
      wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//*[@role='tab' and text()='Second Child']"))).click();
      wait.until(browser -> body().contains("choice=second"));
      assertEquals("1", driver.executeScript("return localStorage.getItem('qqq.widgets.selectedTabs.ownedControls');"));
      assertEquals("true", driver.findElement(By.xpath("//*[@role='tab' and text()='Second Child']")).getAttribute("aria-selected"));
   }



   /*******************************************************************************
    ** Empty and failed parents retain a healthy neighboring widget.
    *******************************************************************************/
   @Test
   void testParentEmptyAndFailedStates() throws Exception
   {
      ParentWidgetMetaData parent = (ParentWidgetMetaData) instance.getWidget("ownedControls");
      parent.setDropdowns(List.of());
      parent.setChildWidgetNameList(List.of());
      open();
      wait.until(browser -> !driver.findElements(By.id("ownedControls")).isEmpty());
      assertTrue(driver.findElements(By.id("ownedControlValues")).isEmpty());
      parent.setChildWidgetNameList(null);
      driver.navigate().refresh();
      wait.until(browser -> body().contains("98.5%"));
      assertTrue(driver.findElements(By.id("ownedControlValues")).isEmpty());
      parent.setDropdowns(List.of(new WidgetDropdownData().withPossibleValueSourceName("ownedMissingPvs")));
      driver.navigate().refresh();
      wait.until(browser -> body().contains("An error occurred loading widget content."));
      assertTrue(body().contains("98.5%"));
   }



   /*******************************************************************************
    ** Hidden or removed child metadata must not break an allowed parent dashboard.
    *******************************************************************************/
   @Test
   void testParentDeniedAndMissingChildren() throws Exception
   {
      ParentWidgetMetaData parent = (ParentWidgetMetaData) instance.getWidget("ownedControls");
      parent.setDropdowns(List.of());
      instance.getWidget("ownedControlValues").setPermissionRules(QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      instance.addWidget(new QWidgetMetaData().withName("ownedAllowedChild").withType("html").withLabel("Allowed Child")
         .withDefaultValue("ownedChoice", "allowed").withCodeReference(new QCodeReference(ControlsRenderer.class)));
      parent.setChildWidgetNameList(List.of("ownedControlValues", "ownedAllowedChild"));
      for(ParentWidgetMetaData.LayoutType layout : ParentWidgetMetaData.LayoutType.values())
      {
         parent.setLayoutType(layout);
         driver.get(baseUrl + "/SampleWidgetsDashboard");
         String metadata = (String) driver.executeAsyncScript("const done = arguments[arguments.length - 1]; fetch('/metaData').then(r => r.text()).then(done);");
         assertFalse(new JSONObject(metadata).getJSONObject("widgets").has("ownedControlValues"));
         assertEquals(403L, driver.executeAsyncScript("const done = arguments[arguments.length - 1]; fetch('/widget/ownedControlValues').then(r => done(r.status));"));
         wait.until(browser -> body().contains("98.5%") && body().contains("choice=allowed"));
         assertTrue(driver.findElements(By.id("ownedControlValues")).isEmpty());
      }
      parent.setChildWidgetNameList(List.of("ownedMissingChild", "ownedAllowedChild"));
      driver.navigate().refresh();
      wait.until(browser -> body().contains("98.5%") && body().contains("choice=allowed"));
      parent.setChildWidgetNameList(List.of("ownedControlValues"));
      driver.navigate().refresh();
      wait.until(browser -> body().contains("98.5%"));
      assertTrue(driver.findElements(By.id("ownedControlValues")).isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void open()
   {
      driver.get(baseUrl + "/SampleWidgetsDashboard");
      wait.until(browser -> body().contains("98.5%"));
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
   private WebElement icon(String name)
   {
      return driver.findElement(By.xpath("//*[@id='ownedControls']//button[.//*[text()='" + name + "']]"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class ControlsRenderer extends AbstractWidgetRenderer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public RenderWidgetOutput render(RenderWidgetInput input) throws QException
      {
         calls.incrementAndGet();
         if(fail && !"parentWidget".equals(input.getWidgetMetaData().getType()))
         {
            throw new QException("Owned controls failure");
         }
         QWidgetData data;
         if("parentWidget".equals(input.getWidgetMetaData().getType()))
         {
            data = new ParentWidgetRenderer().render(input).getWidgetData();
         }
         else
         {
            data = new RawHTML("Owned Controls", "choice=" + input.getQueryParams().getOrDefault("ownedChoice", "") + "; day=" + input.getQueryParams().getOrDefault("ownedDate", ""));
            setupDropdowns(input, (QWidgetMetaData) input.getWidgetMetaData(), data);
         }
         if(!emptyExport)
         {
            data.setCsvData(List.of(List.of("Label", "Value"), List.of("A,\"B\"", 7), List.of("Beta", 0)));
         }
         return new RenderWidgetOutput(data);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class BlocksRenderer extends AbstractWidgetRenderer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public RenderWidgetOutput render(RenderWidgetInput input) throws QException
      {
         CompositeWidgetData data = new CompositeWidgetData();
         data.setLayout(CompositeWidgetData.Layout.FLEX_ROW_WRAPPED);
         if(malformedBlock)
         {
            data.addBlock(new BigNumberBlockData()
            {
               /*******************************************************************************
                **
                *******************************************************************************/
               @Override
               public String getBlockTypeName()
               {
                  return "OWNED_UNKNOWN";
               }
            });
         }
         else if(enterBlock)
         {
            data.addBlock(new InputFieldBlockData().withValues(new InputFieldValues()
               .withFieldMetaData(new QFieldMetaData("ownedMessage", QFieldType.STRING).withLabel("Owned message").withIsRequired(true))
               .withSubmitOnEnter(true)));
         }
         else
         {
            data.addBlock(new ButtonBlockData().withValues(new ButtonValues().withLabel("Submit owned").withActionCode("owned-submit")));
         }
         return new RenderWidgetOutput(data);
      }
   }
}
