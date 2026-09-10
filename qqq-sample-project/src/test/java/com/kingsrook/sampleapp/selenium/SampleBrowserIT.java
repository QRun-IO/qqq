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
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.sampleapp.SampleJavalinServer;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Platform;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Browser acceptance uses a fresh server, synthetic database and browser profile.
 *******************************************************************************/
class SampleBrowserIT
{
   private SampleJavalinServer server;
   private ChromeDriver driver;
   private String baseUrl;
   private String originalMockAuthentication;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      originalMockAuthentication = System.getProperty("qqq.sample.mockAuthentication");
      System.setProperty("qqq.sample.mockAuthentication", "true");
      server = new SampleJavalinServer();
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
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
   void stop(TestInfo testInfo) throws Exception
   {
      try
      {
         if(driver != null)
         {
            try
            {
               Path evidence = Path.of("target", "browser-acceptance");
               Files.createDirectories(evidence);
               Files.write(evidence.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".png"), driver.getScreenshotAs(OutputType.BYTES));
               Files.writeString(evidence.resolve(testInfo.getTestMethod().orElseThrow().getName() + ".html"), driver.getPageSource());
            }
            finally
            {
               driver.quit();
            }
         }
      }
      finally
      {
         if(server != null)
         {
            server.stop();
         }
         QContext.clear();
         if(originalMockAuthentication == null)
         {
            System.clearProperty("qqq.sample.mockAuthentication");
         }
         else
         {
            System.setProperty("qqq.sample.mockAuthentication", originalMockAuthentication);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCreateEditAndProcessPerson() throws Exception
   {
      driver.get(baseUrl + "/public/");
      assertEquals("Welcome to the public site", driver.findElement(By.tagName("h1")).getText());
      driver.get(baseUrl + "/peopleApp/greetingsApp/person");
      new WebDriverWait(driver, Duration.ofSeconds(30)).until(browser -> browser.findElement(By.tagName("body")).getText().contains("Person"));
      assertTrue(driver.findElement(By.tagName("body")).getText().contains("Person"));
      driver.findElement(By.cssSelector("[data-qqq-id='create-new']")).click();
      new WebDriverWait(driver, Duration.ofSeconds(30)).until(browser -> !browser.findElements(By.cssSelector("form input")).isEmpty());
      driver.findElement(By.cssSelector("[data-qqq-id='save']")).click();
      new WebDriverWait(driver, Duration.ofSeconds(30)).until(browser -> browser.findElement(By.tagName("body")).getText().contains("First Name is required."));
      assertTrue(driver.findElement(By.tagName("body")).getText().contains("Last Name is required."));
      assertTrue(driver.findElement(By.tagName("body")).getText().contains("Email is required."));
      driver.findElement(By.id("firstName")).sendKeys("Browser");
      driver.findElement(By.id("lastName")).sendKeys("Acceptance");
      driver.findElement(By.id("email")).sendKeys("browser@example.invalid");
      driver.findElement(By.cssSelector("[data-qqq-id='save']")).click();
      new WebDriverWait(driver, Duration.ofSeconds(30)).until(browser -> !browser.getCurrentUrl().endsWith("/create") && browser.findElement(By.tagName("body")).getText().contains("Browser Acceptance"));
      driver.findElement(By.cssSelector("a[href$='/edit']")).click();
      new WebDriverWait(driver, Duration.ofSeconds(30)).until(browser -> !browser.findElements(By.id("firstName")).isEmpty());
      Keys selectAllModifier = Platform.MAC.is(driver.getCapabilities().getPlatformName()) ? Keys.COMMAND : Keys.CONTROL;
      driver.findElement(By.id("firstName")).sendKeys(Keys.chord(selectAllModifier, "a"), "Updated");
      assertEquals("Updated", driver.findElement(By.id("firstName")).getAttribute("value"));
      driver.findElement(By.cssSelector("[data-qqq-id='save']")).click();
      new WebDriverWait(driver, Duration.ofSeconds(30)).until(browser -> !browser.getCurrentUrl().endsWith("/edit") && browser.findElement(By.tagName("body")).getText().contains("Updated Acceptance"));
      driver.navigate().refresh();
      new WebDriverWait(driver, Duration.ofSeconds(30)).until(browser -> browser.findElement(By.tagName("body")).getText().contains("Updated Acceptance"));
      driver.findElement(By.cssSelector("[data-qqq-id='actions-menu']")).click();
      new WebDriverWait(driver, Duration.ofSeconds(30)).until(browser -> !browser.findElements(By.cssSelector("[role='menuitem']")).isEmpty());
      driver.findElement(By.cssSelector("[data-qqq-id='menu-item-greetinteractive']")).click();
      new WebDriverWait(driver, Duration.ofSeconds(30)).until(browser -> !browser.findElements(By.id("greetingPrefix")).isEmpty());
      driver.findElement(By.id("greetingPrefix")).sendKeys("Hello");
      driver.findElement(By.id("greetingSuffix")).sendKeys("QQQ");
      driver.findElement(By.cssSelector("[data-qqq-id='submit']")).click();
      new WebDriverWait(driver, Duration.ofSeconds(30)).until(browser -> browser.findElement(By.tagName("body")).getText().contains("Hello Updated QQQ"));
      assertTrue(driver.findElement(By.tagName("body")).getText().contains("Hello X QQQ"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFieldLabFormAndNormalization() throws Exception
   {
      driver.get(baseUrl + "/miscellaneous/fieldLab/create");
      new WebDriverWait(driver, Duration.ofSeconds(30)).until(browser -> !browser.findElements(By.id("name")).isEmpty());
      driver.findElement(By.id("name")).sendKeys("Browser Field Lab");
      driver.findElement(By.id("upperValue")).sendKeys("mixed case");
      driver.findElement(By.id("lowerValue")).sendKeys("MIXED CASE");
      driver.findElement(By.id("trimValue")).sendKeys("  trim me  ");
      driver.findElement(By.id("boundedValue")).sendKeys("42");
      driver.findElement(By.id("passwordValue")).sendKeys("synthetic-browser-value");
      assertEquals("password", driver.findElement(By.id("passwordValue")).getAttribute("type"));
      driver.findElement(By.cssSelector("[data-qqq-id='save']")).click();
      new WebDriverWait(driver, Duration.ofSeconds(30)).until(browser -> !browser.getCurrentUrl().endsWith("/create") && browser.findElement(By.tagName("body")).getText().contains("Browser Field Lab"));
      driver.navigate().refresh();
      new WebDriverWait(driver, Duration.ofSeconds(30)).until(browser -> browser.findElement(By.tagName("body")).getText().contains("MIXED CASE"));
      assertTrue(driver.findElement(By.tagName("body")).getText().contains("mixed case"));
      assertTrue(driver.findElement(By.tagName("body")).getText().contains("trim me"));
      assertTrue(!driver.findElement(By.tagName("body")).getText().contains("synthetic-browser-value"));
   }
}
