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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;
import io.javalin.Javalin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StarterApplicationLivePermissionTest
{
   private static volatile boolean allow;

   @Test
   void deniedInsertHasAllowedControlAndNoDatabaseWrite() throws Exception
   {
      QInstance instance = new StarterAppMetaDataProvider().defineQInstance();
      instance.withInstanceDefaultAuthentication(new QAuthenticationMetaData()
         .withName("mock").withType(QAuthenticationType.MOCK)
         .withCustomizer(new QCodeReference(Grants.class)));
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
      String marker = UUID.randomUUID().toString();
      String allowedName = "Allowed-" + marker;
      String deniedName = "Denied-" + marker;
      try
      {
         server.start();
         URI uri = URI.create("http://localhost:" + service.get().port() + "/qqq/v1/table/orderDeskEntity");
         try(HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build())
         {
            allow = true;
            assertEquals(200, insert(client, uri, allowedName).statusCode(), "allowed control must write");
            assertEquals(1, count(allowedName), "allowed control must persist");
            allow = false;
            HttpResponse<String> denied = insert(client, uri, deniedName);
            assertEquals(403, denied.statusCode(), denied.body());
            assertEquals(0, count(deniedName), "denied operation must not write");
            assertEquals(1, count(allowedName), "denial must not undo the allowed control");
         }
      }
      finally
      {
         allow = false;
         server.stop();
         QContext.clear();
         try(Connection connection = ConnectionManager.getConnection((RDBMSBackendMetaData) StarterAppMetaDataProvider.defineRDBMSBackend());
            PreparedStatement delete = connection.prepareStatement("DELETE FROM orderDeskEntity WHERE name=?"))
         {
            delete.setString(1, allowedName);
            delete.executeUpdate();
         }
      }
   }

   private HttpResponse<String> insert(HttpClient client, URI uri, String name) throws Exception
   {
      HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5))
         .header("Content-Type", "application/json")
         .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"" + name + "\"}"))
         .build();
      return client.send(request, HttpResponse.BodyHandlers.ofString());
   }

   private int count(String name) throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection((RDBMSBackendMetaData) StarterAppMetaDataProvider.defineRDBMSBackend());
         PreparedStatement query = connection.prepareStatement("SELECT COUNT(*) FROM orderDeskEntity WHERE name=?"))
      {
         query.setString(1, name);
         try(ResultSet result = query.executeQuery())
         {
            result.next();
            return result.getInt(1);
         }
      }
   }

   public static class Grants implements QAuthenticationModuleCustomizerInterface
   {
      @Override
      public void customizeSession(QInstance instance, QSession session, Map<String, Object> context)
      {
         if(allow)
         {
            session.withPermissions("orderDeskEntity.hasAccess");
         }
      }
   }
}
