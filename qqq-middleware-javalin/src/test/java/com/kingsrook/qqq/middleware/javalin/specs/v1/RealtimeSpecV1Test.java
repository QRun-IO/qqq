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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.realtime.RealtimeBus;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Real authenticated HTTP streams and subscriptions through the existing spec harness.
 *******************************************************************************/
class RealtimeSpecV1Test extends SpecTestBase
{
   private final RealtimeStreamSpecV1 stream = new RealtimeStreamSpecV1();



   /*******************************************************************************
    ** Stream route under test.
    *******************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (stream);
   }



   /*******************************************************************************
    ** Subscription and update routes share the stream hub.
    *******************************************************************************/
   @Override
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new RealtimeSubscriptionsSpecV1(stream::getHub), new TableUpdateSpecV1(), new TableGetSpecV1(), new LogoutSpecV1()));
   }



   /*******************************************************************************
    ** Use the ordinary v1 route prefix.
    *******************************************************************************/
   @Override
   protected String getVersion()
   {
      return ("v1");
   }



   /*******************************************************************************
    ** Enable only the feature gate needed for change hints.
    *******************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = super.defineQInstance();
      RecordFeaturesMetaData.ofOrWithNew(instance).withEnabled(RecordFeature.FOLLOW, true);
      new QInstanceEnricher(instance).enrich();
      return (instance);
   }



   /*******************************************************************************
    ** Release streaming resources before the harness stops Jetty.
    *******************************************************************************/
   @AfterEach
   void closeStream()
   {
      stream.close();
      RealtimeBus.close(serverQInstance);
   }



   /*******************************************************************************
    ** Read a bounded number of SSE lines with a socket read timeout.
    *******************************************************************************/
   private JSONObject event(BufferedReader reader) throws Exception
   {
      JSONObject event = new JSONObject();
      for(int lineCount = 0; lineCount < 100; lineCount++)
      {
         String line = reader.readLine();
         if(line == null)
         {
            throw (new AssertionError("Stream ended before an event"));
         }
         if(line.isEmpty() && event.has("event"))
         {
            return (event);
         }
         if(line.startsWith("event:"))
         {
            event.put("event", line.substring(6).trim());
         }
         else if(line.startsWith("id:"))
         {
            event.put("id", line.substring(3).trim());
         }
         else if(line.startsWith("data:"))
         {
            event.put("data", new JSONObject(line.substring(5).trim()));
         }
      }
      throw (new AssertionError("No event in first 100 SSE lines"));
   }



   /*******************************************************************************
    ** Hello, allowed/refused subscriptions, ownership isolation and update-to-stream delivery.
    *******************************************************************************/
   @Test
   void streamSubscriptionsAndCommittedChange() throws Exception
   {
      HttpURLConnection connection = (HttpURLConnection) URI.create(getBaseUrlAndPath() + "/realtime/stream").toURL().openConnection();
      connection.setRequestProperty("Cookie", "sessionId=realtime-owner");
      connection.setReadTimeout(4000);
      connection.setConnectTimeout(4000);
      try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)))
      {
         assertTrue(connection.getContentType().startsWith("text/event-stream"));
         JSONObject hello = event(reader);
         assertEquals("hello", hello.getString("event"));
         String id = hello.getJSONObject("data").getString("connectionId");
         assertTrue(hello.getString("id").startsWith(id + ":"));
         var subscribed = Unirest.post(getBaseUrlAndPath() + "/realtime/subscriptions")
            .header("Cookie", "sessionId=realtime-owner").contentType("application/json")
            .body(JsonUtils.toJson(Map.of("connectionId", id, "add", List.of("record:person:1", "record:person:999999"))))
            .asString();
         assertEquals(200, subscribed.getStatus(), subscribed.getBody());
         JSONObject subscriptions = new JSONObject(subscribed.getBody());
         assertEquals(List.of("record:person:1"), subscriptions.getJSONArray("active").toList());
         assertEquals(List.of("record:person:999999"), subscriptions.getJSONArray("refused").toList());
         var foreign = Unirest.post(getBaseUrlAndPath() + "/realtime/subscriptions")
            .header("Cookie", "sessionId=someone-else").contentType("application/json")
            .body(JsonUtils.toJson(Map.of("connectionId", id, "add", List.of("instance")))).asString();
         assertEquals(404, foreign.getStatus(), foreign.getBody());
         var update = Unirest.patch(getBaseUrlAndPath() + "/table/person/1").header("Cookie", "sessionId=realtime-owner")
            .contentType("application/json").body(JsonUtils.toJson(Map.of("firstName", "Changed"))).asString();
         assertEquals(200, update.getStatus(), update.getBody());
         JSONObject changed = event(reader);
         assertEquals("record.changed", changed.getString("event"));
         assertEquals("person", changed.getJSONObject("data").getString("table"));
         assertTrue(changed.getJSONObject("data").getJSONArray("fields").toList().contains("firstName"));
         assertFalse(changed.getJSONObject("data").has("values"));
         serverQInstance.addSecurityKeyType(new QSecurityKeyType().withName("lastName"));
         serverQInstance.getTable("person").withRecordSecurityLock(new RecordSecurityLock().withFieldName("lastName").withSecurityKeyType("lastName"));
         var invisible = Unirest.post(getBaseUrlAndPath() + "/realtime/subscriptions")
            .header("Cookie", "sessionId=realtime-owner").contentType("application/json")
            .body(JsonUtils.toJson(Map.of("connectionId", id, "add", List.of("record:person:1"), "remove", List.of("record:person:1")))).asString();
         assertEquals(200, invisible.getStatus(), invisible.getBody());
         assertEquals(List.of("record:person:1"), new JSONObject(invisible.getBody()).getJSONArray("refused").toList());
         var logout = Unirest.post(getBaseUrlAndPath() + "/logout").header("Cookie", "sessionId=realtime-owner").asString();
         assertEquals(200, logout.getStatus(), logout.getBody());
         assertEquals("session-ended", event(reader).getString("event"));
      }
      finally
      {
         connection.disconnect();
      }
   }



   /*******************************************************************************
    ** Conflicts return 409 details; omitted versions preserve ordinary updates.
    *******************************************************************************/
   @Test
   void versionConflictAndUnconditionalUpdate()
   {
      serverQInstance.getTable("person").setRecordVersionField("firstName");
      var before = Unirest.get(getBaseUrlAndPath() + "/table/person/1").asString();
      String current = new JSONObject(before.getBody()).getJSONObject("record").getJSONObject("values").getString("firstName");
      var stale = Unirest.patch(getBaseUrlAndPath() + "/table/person/1").queryString("expectedVersion", "stale")
         .contentType("application/json").body(JsonUtils.toJson(Map.of("firstName", "Rejected"))).asString();
      assertEquals(409, stale.getStatus(), stale.getBody());
      JSONObject conflict = new JSONObject(stale.getBody());
      assertEquals(current, conflict.getString("currentVersion"));
      assertTrue(conflict.getJSONArray("changedFields").toList().contains("firstName"));
      var matched = Unirest.patch(getBaseUrlAndPath() + "/table/person/1").queryString("expectedVersion", current)
         .contentType("application/json").body(JsonUtils.toJson(Map.of("firstName", "Matched"))).asString();
      assertEquals(200, matched.getStatus(), matched.getBody());
      var ordinary = Unirest.patch(getBaseUrlAndPath() + "/table/person/1")
         .contentType("application/json").body(JsonUtils.toJson(Map.of("firstName", "Unconditional"))).asString();
      assertEquals(200, ordinary.getStatus(), ordinary.getBody());
   }
}

