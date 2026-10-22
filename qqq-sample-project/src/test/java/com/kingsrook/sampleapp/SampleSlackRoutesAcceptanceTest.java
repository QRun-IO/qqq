/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


import java.io.InputStreamReader;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.TableBasedAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.TableBasedAuthenticationModule;
import com.kingsrook.qqq.backend.core.state.SimpleStateKey;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.qqq.esb.model.EsbInstanceMetaData;
import com.kingsrook.qqq.esb.model.EsbProcessMetaData;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import com.kingsrook.qqq.middleware.javalin.QJavalinImplementation;
import com.kingsrook.qqq.slack.QSlackImplementation;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import io.javalin.Javalin;
import org.h2.tools.RunScript;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Native Slack form routes use owned HTTP/H2 and QQQ session-cookie auth.
 ** This is not live Slack identity, request-signature or token verification.
 *******************************************************************************/
class SampleSlackRoutesAcceptanceTest
{
   private final String sessionId = UUID.randomUUID().toString();
   private final String sentinel = "SlackOwned" + UUID.randomUUID();
   private CapturedContext previousContext;
   private Map<String, Serializable> previousObjects;
   private QInstance previousJavalinInstance;
   private Object previousSlackInstance;
   private Field slackInstanceField;
   private Connection anchor;
   private QInstance instance;
   private Javalin server;
   private HttpClient client;
   private URI route;



   /*******************************************************************************
    ** Reuse canonical sample records; change one value to prove the actual database.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousContext = QContext.capture();
      previousObjects = QContext.getObjects();
      previousJavalinInstance = QJavalinImplementation.getQInstance();
      slackInstanceField = QSlackImplementation.class.getDeclaredField("qInstance");
      slackInstanceField.setAccessible(true);
      previousSlackInstance = slackInstanceField.get(null);
      QContext.setObjects(new LinkedHashMap<>());
      ConnectionManager.resetConnectionProviders();
      String jdbcUrl = "jdbc:h2:mem:slack_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=0";
      anchor = DriverManager.getConnection(jdbcUrl, "sa", "");
      try(InputStreamReader reader = new InputStreamReader(SampleMetaDataProvider.class.getResourceAsStream("/prime-test-database.sql"), StandardCharsets.UTF_8))
      {
         RunScript.execute(anchor, reader);
      }
      try(PreparedStatement statement = anchor.prepareStatement("UPDATE person SET first_name=? WHERE id=1"))
      {
         statement.setString(1, sentinel);
         assertEquals(1, statement.executeUpdate());
      }
      try(Statement statement = anchor.createStatement())
      {
         statement.execute("CREATE TABLE slack_user (id INT PRIMARY KEY, create_date TIMESTAMP DEFAULT now(), modify_date TIMESTAMP DEFAULT now(), username VARCHAR(100), password_hash VARCHAR(200), full_name VARCHAR(100))");
         statement.execute("CREATE TABLE slack_session (id VARCHAR(40) PRIMARY KEY, create_date TIMESTAMP DEFAULT now(), modify_date TIMESTAMP DEFAULT now(), user_id INT, access_timestamp TIMESTAMP DEFAULT now())");
         statement.execute("INSERT INTO slack_user (id,username,full_name) VALUES (1,'sample-slack','Sample Slack User')");
      }
      try(PreparedStatement statement = anchor.prepareStatement("INSERT INTO slack_session (id,user_id,access_timestamp) VALUES (?,1,?)"))
      {
         statement.setString(1, sessionId);
         statement.setObject(2, LocalDateTime.now(ZoneOffset.UTC));
         statement.executeUpdate();
      }
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.getSupplementalMetaData().remove(EsbInstanceMetaData.NAME);
      instance.getTables().values().forEach(table ->
      {
         if(table.getSupplementalMetaData() != null)
         {
            table.getSupplementalMetaData().remove(EsbTableMetaData.TYPE);
         }
      });
      instance.getProcesses().values().forEach(process ->
      {
         if(process.getSupplementalMetaData() != null)
         {
            process.getSupplementalMetaData().remove(EsbProcessMetaData.TYPE);
         }
      });
      RDBMSBackendMetaData backend = SampleMetaDataProvider.defineRdbmsBackend().withName("slackOwnedDatabase").withJdbcUrl(jdbcUrl);
      instance.addBackend(backend);
      for(String name : List.of("person", "pet", "petNote"))
      {
         instance.getTable(name).setBackendName(backend.getName());
      }
      TableBasedAuthenticationMetaData authentication = new TableBasedAuthenticationMetaData().withUserTableName("slackUser").withSessionTableName("slackSession");
      authentication.setName("slackOwnedAuthentication");
      instance.addTable(authentication.defineStandardUserTable(backend.getName()));
      instance.addTable(authentication.defineStandardSessionTable(backend.getName()));
      for(String name : List.of("slackUser", "slackSession"))
      {
         QTableMetaData table = instance.getTable(name);
         table.withBackendDetails(new RDBMSTableBackendDetails().withTableName(name.equals("slackUser") ? "slack_user" : "slack_session"));
         for(QFieldMetaData field : table.getFields().values())
         {
            field.setBackendName(field.getName().replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT));
         }
      }
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), authentication);
      QContext.init(instance, new QSession());
      new QInstanceValidator().revalidate(instance);
      QJavalinImplementation.setQInstance(instance);
      QSlackImplementation slack = new QSlackImplementation(instance);
      server = Javalin.create(config ->
      {
         config.jetty.host = "127.0.0.1";
         config.routes.apiBuilder(slack.getRoutes());
         config.routes.after(QJavalinImplementation::clearQContext);
      }).start(0);
      route = URI.create("http://127.0.0.1:" + server.port() + "/slack/");
      client = HttpClient.newHttpClient();
      try(Connection actual = ConnectionManager.getConnection(backend))
      {
         assertEquals(anchor.getMetaData().getURL(), actual.getMetaData().getURL());
      }
   }



   /*******************************************************************************
    ** Restore caller globals and objects; shut down only the owned database.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(client != null)
         {
            client.close();
         }
         if(server != null)
         {
            server.stop();
         }
         TableBasedAuthenticationModule.getStateProvider().remove(new SimpleStateKey<>(sessionId));
         TableBasedAuthenticationModule.getStateProvider().remove(new SimpleStateKey<>("tableBasedAuthActivity:" + sessionId));
      }
      finally
      {
         try
         {
            if(anchor != null)
            {
               try(Statement statement = anchor.createStatement())
               {
                  statement.execute("SHUTDOWN");
               }
               finally
               {
                  anchor.close();
               }
            }
         }
         finally
         {
            slackInstanceField.set(null, previousSlackInstance);
            QJavalinImplementation.setQInstance(previousJavalinInstance);
            ConnectionManager.resetConnectionProviders();
            QContext.clear();
            QContext.init(previousContext);
            QContext.setObjects(previousObjects);
         }
      }
   }



   /*******************************************************************************
    ** Wrong storage or missing query/get dispatch loses the unique SQL sentinel.
    *******************************************************************************/
   @Test
   void testNativeQueryAndGetResponses() throws Exception
   {
      String query = message(post(form("table person query"), sessionId));
      assertTrue(query.contains("Query on table [person] results"), query);
      assertTrue(query.contains("id='1'"));
      assertTrue(query.contains("id='2'"));
      String get = message(post(form("table person get 1"), sessionId));
      assertTrue(get.contains("Get [1] on table [person] results"));
      assertTrue(get.contains(sentinel));
      assertFalse(get.contains("Blair"));
      assertTrue(message(post(form("table person get 999999"), sessionId)).contains("Record was not found"));
   }



   /*******************************************************************************
    ** Metadata includes supported processes; Slack does not execute processes.
    *******************************************************************************/
   @Test
   void testMetadataListsProcessesAndRejectsProcessCommand() throws Exception
   {
      JSONObject metadata = post(form(""), sessionId);
      assertEquals(4, metadata.getJSONArray("blocks").length(), message(metadata));
      String text = message(metadata);
      assertTrue(text.contains("*Tables*"));
      assertTrue(text.contains("*Processes*"));
      assertTrue(text.contains("Clone People [clonePeople]"));
      assertTrue(message(post(form("process greetInteractive"), sessionId)).contains("Invalid command was entered"));
   }



   /*******************************************************************************
    ** Missing and unknown native sessions cannot disclose canonical records.
    *******************************************************************************/
   @Test
   void testMissingAndInvalidSession() throws Exception
   {
      for(String cookie : new String[] { null, "unknown-synthetic-session" })
      {
         assertError(post(form("table person query"), cookie));
         assertError(post(form("table person get 1"), cookie));
      }
      assertTrue(message(post(form("table person get 1"), sessionId)).contains(sentinel));
   }



   /*******************************************************************************
    ** Form identity/token are not credentials; document the native auth boundary.
    *******************************************************************************/
   @Test
   void testSlackFormIdentityAndTokenDoNotAuthenticate() throws Exception
   {
      String request = form("table person get 1") + "&user_id=synthetic-user&token=synthetic-invalid-token";
      assertError(post(request, null));
      assertError(post(request, "unknown-synthetic-session"));
      assertTrue(message(post(request, sessionId)).contains(sentinel));
   }



   /*******************************************************************************
    ** A table permission denial stays an error while another native table works.
    *******************************************************************************/
   @Test
   void testDeniedReadWithAllowedControl() throws Exception
   {
      instance.getTable("person").setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      assertAll(
         () -> assertError(post(form("table person query"), sessionId)),
         () -> assertError(post(form("table person get 1"), sessionId)));
      assertTrue(message(post(form("table pet query"), sessionId)).contains("Query on table [pet] results"));
   }



   /*******************************************************************************
    ** Malformed commands return existing Slack error blocks, without side effects.
    *******************************************************************************/
   @Test
   void testMalformedAndUnknownCommands() throws Exception
   {
      for(String text : List.of("table", "table unknown query", "table person wrong", "table person get", "nonsense"))
      {
         assertError(post(form(text), sessionId));
      }
      assertError(post("text=table+person+query", sessionId));
      assertError(post("command=%ZZ", sessionId));
      try(Statement statement = anchor.createStatement(); var rows = statement.executeQuery("SELECT count(*) FROM person"))
      {
         assertTrue(rows.next());
         assertEquals(5, rows.getInt(1));
      }
   }



   /*******************************************************************************
    ** A real form request; exports are deliberately never invoked by this fixture.
    *******************************************************************************/
   private JSONObject post(String body, String cookie) throws Exception
   {
      HttpRequest.Builder request = HttpRequest.newBuilder(route).timeout(Duration.ofSeconds(10))
         .header("Content-Type", "application/x-www-form-urlencoded")
         .POST(HttpRequest.BodyPublishers.ofString(body));
      if(cookie != null)
      {
         request.header("Cookie", "sessionId=" + cookie);
      }
      HttpResponse<String> response = client.send(request.build(), HttpResponse.BodyHandlers.ofString());
      assertEquals(200, response.statusCode());
      JSONObject json = new JSONObject(response.body());
      assertEquals("in_channel", json.getString("response_type"));
      return json;
   }



   /*******************************************************************************
    ** URL-encode command text rather than relying on the framework parser.
    *******************************************************************************/
   private String form(String text)
   {
      return "command=%2Fqqq&text=" + URLEncoder.encode(text, StandardCharsets.UTF_8);
   }



   /*******************************************************************************
    ** Inspect the actual Slack section/mrkdwn envelope.
    *******************************************************************************/
   private String message(JSONObject response)
   {
      StringBuilder result = new StringBuilder();
      for(Object value : response.getJSONArray("blocks"))
      {
         JSONObject block = (JSONObject) value;
         assertEquals("section", block.getString("type"));
         assertEquals("mrkdwn", block.getJSONObject("text").getString("type"));
         result.append(block.getJSONObject("text").getString("text"));
      }
      return result.toString();
   }



   /*******************************************************************************
    ** Existing transport errors use a200 Slack Error block, not an HTTP4xx claim.
    *******************************************************************************/
   private void assertError(JSONObject response)
   {
      String text = message(response);
      assertTrue(text.contains("*Error*"), text);
      assertFalse(text.contains(sentinel));
      assertFalse(text.contains("Blair"));
   }
}
