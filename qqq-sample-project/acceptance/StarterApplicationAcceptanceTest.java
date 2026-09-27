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

package com.kingsrook.qqq.starterapp;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import com.qrunio.acceptance.orderdesk.OrderDeskAppQBitConfig;
import com.qrunio.acceptance.orderdesk.OrderDeskAppQBitProducer;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StarterApplicationAcceptanceTest
{
   @Test
   void generatedQBitIsRegisteredInRealStarterHost() throws Exception
   {
      QInstance instance = new StarterAppMetaDataProvider().defineQInstance();
      assertEquals(1, instance.getQBits().size());
      assertNotNull(instance.getTable("orderDeskEntity"), instance.getTables().keySet().toString());
      assertNotNull(instance.getTable("orderDeskChildEntity"));
      assertEquals("orderDeskEntity", instance.getTable("orderDeskChildEntity")
         .getFields().get("orderDeskEntityId").getPossibleValueSourceName());
      assertNotNull(instance.getPossibleValueSource("orderDeskEntity"));
      assertNotNull(instance.getProcess("orderDeskProcess"));
      assertNotNull(instance.getWidget("orderDeskDashboardWidget"));
      assertNotNull(instance.getApp("orderDeskApp"));
      assertTrue(instance.getApp("orderDeskApp").getSections().stream()
         .anyMatch(section -> section.getTables().contains("orderDeskEntity")));
   }

   @Test
   void generatedNavigationMustBeValidForStarterHttp() throws Exception
   {
      QInstance instance = new StarterAppMetaDataProvider().defineQInstance();
      assertNotNull(instance.getApp("orderDeskApp").getChildren(),
         "QAppMetaData validation requires app children; template only calls withSection");
   }

   @Test
   void invalidConfigurationAndDuplicateRegistrationFail() throws Exception
   {
      QInstance empty = new QInstance();
      QException missingBackend = assertThrows(QException.class, () -> new OrderDeskAppQBitProducer()
         .withConfig(new OrderDeskAppQBitConfig().withBackendName("missing"))
         .produce(empty, "acceptance"));
      assertTrue(missingBackend.getMessage().contains("Backend not found"));

      QException blankBackend = assertThrows(QException.class, () -> new OrderDeskAppQBitProducer()
         .withConfig(new OrderDeskAppQBitConfig()).produce(empty, "acceptance"));
      assertTrue(blankBackend.getMessage().contains("backendName is required"));

      QInstance instance = new StarterAppMetaDataProvider().defineQInstance();
      IllegalArgumentException duplicate = assertThrows(IllegalArgumentException.class,
         () -> new OrderDeskAppQBitProducer()
            .withConfig(new OrderDeskAppQBitConfig().withBackendName(StarterAppMetaDataProvider.RDBMS_BACKEND_NAME))
            .produce(instance, "acceptance"));
      assertTrue(duplicate.getMessage().contains("second qBit"));
   }

   @Test
   void configuredMockAuthenticationDeniesUnpermittedInsert() throws Exception
   {
      QInstance instance = new StarterAppMetaDataProvider().defineQInstance();
      instance.withInstanceDefaultAuthentication(new QAuthenticationMetaData()
         .withName("mock").withType(QAuthenticationType.MOCK));
      instance.getTable("orderDeskEntity").setPermissionRules(
         QPermissionRules.defaultInstance().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      QApplicationJavalinServer server = new QApplicationJavalinServer(new AbstractQQQApplication()
      {
         @Override
         public QInstance defineQInstance()
         {
            return instance;
         }
      });
      AtomicReference<Javalin> service = new AtomicReference<>();
      server.setPort(0);
      server.withJavalinConfigurationCustomizer(service::set);
      try
      {
         server.start();
         URI uri = URI.create("http://localhost:" + service.get().port() + "/qqq/v1/table/orderDeskEntity");
         try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
         {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5))
               .header("Content-Type", "application/json")
               .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Denied\"}"))
               .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(403, response.statusCode(), response.body());
            assertFalse(response.body().contains("\"record\""));
         }
      }
      finally
      {
         server.stop();
         QContext.clear();
      }
   }
}
