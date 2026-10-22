/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2026.  Kingsrook, LLC
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
import com.kingsrook.qqq.api.javalin.QJavalinApiHandler;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import io.javalin.Javalin;


/*******************************************************************************
 ** Owned loopback transport shared by the OpenAPI acceptance fixtures.
 *******************************************************************************/
class SampleOpenApiHttpFixture implements AutoCloseable
{
   private final QInstance previousInstance;
   private final Javalin server;
   private final HttpClient client;



   /*******************************************************************************
    ** Register the real product routes and bind an ephemeral loopback port.
    *******************************************************************************/
   SampleOpenApiHttpFixture(QInstance instance)
   {
      previousInstance = QJavalinImplementation.getQInstance();
      Javalin startedServer = null;
      try
      {
         QJavalinImplementation.setQInstance(instance);
         startedServer = Javalin.create(config ->
         {
            config.routes.apiBuilder(new QJavalinApiHandler(instance).getRoutes());
            config.routes.after(context -> QContext.clear());
         });
         startedServer.start("127.0.0.1", 0);
         client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
         server = startedServer;
      }
      catch(RuntimeException e)
      {
         try
         {
            if(startedServer != null)
            {
               startedServer.stop();
            }
         }
         finally
         {
            QJavalinImplementation.setQInstance(previousInstance);
            QContext.clear();
         }
         throw e;
      }
   }



   /*******************************************************************************
    ** Bound each real HTTP request; all data and authentication are fixture-owned.
    *******************************************************************************/
   HttpResponse<String> get(String path) throws Exception
   {
      return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
         .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
   }



   /*******************************************************************************
    ** Release the client, server and calling-thread context after each test.
    *******************************************************************************/
   @Override
   public void close()
   {
      try
      {
         client.close();
      }
      finally
      {
         try
         {
            server.stop();
         }
         finally
         {
            QJavalinImplementation.setQInstance(previousInstance);
            QContext.clear();
         }
      }
   }
}
