/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.metadata;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for JavalinRouteProviderMetaData 
 *******************************************************************************/
class JavalinRouteProviderMetaDataTest
{

   /*******************************************************************************
    ** Test getters and setters for basic properties
    *******************************************************************************/
   @Test
   void testGettersAndSetters()
   {
      JavalinRouteProviderMetaData metaData = new JavalinRouteProviderMetaData();

      ////////////////////////////
      // Test name               //
      ////////////////////////////
      metaData.setName("test-route");
      assertEquals("test-route", metaData.getName());

      ////////////////////////////
      // Test hostedPath         //
      ////////////////////////////
      metaData.setHostedPath("/api");
      assertEquals("/api", metaData.getHostedPath());

      ////////////////////////////
      // Test fileSystemPath     //
      ////////////////////////////
      metaData.setFileSystemPath("/var/www");
      assertEquals("/var/www", metaData.getFileSystemPath());

      ////////////////////////////
      // Test spaRootPath        //
      ////////////////////////////
      metaData.setSpaRootPath("/app");
      assertEquals("/app", metaData.getSpaRootPath());

      ////////////////////////////
      // Test spaRootFile        //
      ////////////////////////////
      metaData.setSpaRootFile("index.html");
      assertEquals("index.html", metaData.getSpaRootFile());

      ////////////////////////////
      // Test processName        //
      ////////////////////////////
      metaData.setProcessName("testProcess");
      assertEquals("testProcess", metaData.getProcessName());

      ////////////////////////////
      // Test spaPath            //
      ////////////////////////////
      metaData.setSpaPath("/admin");
      assertEquals("/admin", metaData.getSpaPath());

      ////////////////////////////
      // Test staticFilesPath    //
      ////////////////////////////
      metaData.setStaticFilesPath("static-files/");
      assertEquals("static-files/", metaData.getStaticFilesPath());

      ////////////////////////////
      // Test spaIndexFile       //
      ////////////////////////////
      metaData.setSpaIndexFile("admin/index.html");
      assertEquals("admin/index.html", metaData.getSpaIndexFile());

      ////////////////////////////
      // Test excludedPaths      //
      ////////////////////////////
      List<String> excludedPaths = List.of("/api", "/auth");
      metaData.setExcludedPaths(excludedPaths);
      assertEquals(excludedPaths, metaData.getExcludedPaths());

      ////////////////////////////
      // Test enableDeepLinking  //
      ////////////////////////////
      metaData.setEnableDeepLinking(false);
      assertFalse(metaData.getEnableDeepLinking());

      ////////////////////////////
      // Test loadFromJar        //
      ////////////////////////////
      metaData.setLoadFromJar(true);
      assertTrue(metaData.getLoadFromJar());

      ////////////////////////////
      // Test methods            //
      ////////////////////////////
      List<String> methods = List.of("GET", "POST");
      metaData.setMethods(methods);
      assertEquals(methods, metaData.getMethods());
   }



   /*******************************************************************************
    ** Test all fluent setters work correctly and return this
    *******************************************************************************/
   @Test
   void testFluentSetters()
   {
      JavalinRouteProviderMetaData metaData = new JavalinRouteProviderMetaData();

      //////////////////////////////////////////////////////
      // Chain multiple fluent setters and verify result //
      //////////////////////////////////////////////////////
      JavalinRouteProviderMetaData result = metaData
         .withName("test-route")
         .withHostedPath("/api")
         .withFileSystemPath("/var/www")
         .withSpaRootPath("/app")
         .withSpaRootFile("index.html")
         .withProcessName("testProcess")
         .withSpaPath("/admin")
         .withStaticFilesPath("static-files/")
         .withSpaIndexFile("admin/index.html")
         .withExcludedPaths(List.of("/api", "/auth"))
         .withEnableDeepLinking(false)
         .withLoadFromJar(true)
         .withMethods(List.of("GET", "POST"));

      ////////////////////////////////////////////
      // Verify all returned the same instance  //
      ////////////////////////////////////////////
      assertEquals(metaData, result);

      //////////////////////////////
      // Verify values were set    //
      //////////////////////////////
      assertEquals("test-route", metaData.getName());
      assertEquals("/api", metaData.getHostedPath());
      assertEquals("/var/www", metaData.getFileSystemPath());
      assertEquals("/app", metaData.getSpaRootPath());
      assertEquals("index.html", metaData.getSpaRootFile());
      assertEquals("testProcess", metaData.getProcessName());
      assertEquals("/admin", metaData.getSpaPath());
      assertEquals("static-files/", metaData.getStaticFilesPath());
      assertEquals("admin/index.html", metaData.getSpaIndexFile());
      assertNotNull(metaData.getExcludedPaths());
      assertEquals(2, metaData.getExcludedPaths().size());
      assertFalse(metaData.getEnableDeepLinking());
      assertTrue(metaData.getLoadFromJar());
      assertNotNull(metaData.getMethods());
      assertEquals(2, metaData.getMethods().size());
   }



   /*******************************************************************************
    ** Test default values
    *******************************************************************************/
   @Test
   void testDefaultValues()
   {
      JavalinRouteProviderMetaData metaData = new JavalinRouteProviderMetaData();

      /////////////////////////////////////////////////////////////
      // Test default values                                    //
      /////////////////////////////////////////////////////////////
      assertTrue(metaData.getEnableDeepLinking(), "enableDeepLinking should default to true");
      assertFalse(metaData.getLoadFromJar(), "loadFromJar should default to false");
   }

}

