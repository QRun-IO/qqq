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
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Exercise the sample's configured process route through the public server.
 *******************************************************************************/
class SampleDynamicRouteTest
{
   /*******************************************************************************
    ** The example must consume the current route-to-process input contract.
    *******************************************************************************/
   @Test
   void testCanonicalDynamicRoute() throws Exception
   {
      SampleJavalinServer server = new SampleJavalinServer(new SampleMetaDataProvider()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public QInstance defineQInstance() throws QException
         {
            return SampleMetaDataProvider.defineTestInstance();
         }
      });
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      try
      {
         server.start();
         try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
         {
            HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + service.get().port() + "/dynamic-site/details"))
               .timeout(Duration.ofSeconds(5)).build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());
            assertEquals("You requested: /dynamic-site/details(at path-param: details)", response.body());
            assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("text/plain"), "The example returns plain text, not JSON");
         }
      }
      finally
      {
         server.stop();
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }
}
