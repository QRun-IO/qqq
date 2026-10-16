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


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import org.json.JSONArray;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** A user's realtime stream (SSE /qqq/v1/realtime/stream) on one node,
 ** subscribed to some channels; events are read on a background thread.
 *******************************************************************************/
class SampleRealtimeStream implements AutoCloseable
{
   private final HttpURLConnection               connection;
   private final LinkedBlockingQueue<JSONObject> events = new LinkedBlockingQueue<>();
   private final Thread                          reader;



   /*******************************************************************************
    **
    *******************************************************************************/
   private SampleRealtimeStream(HttpURLConnection connection)
   {
      this.connection = connection;
      this.reader = Thread.ofVirtual().name("sample-realtime-stream").start(this::read);
   }



   /*******************************************************************************
    ** Open the stream as the node's user and subscribe to the channels.
    *******************************************************************************/
   static SampleRealtimeStream open(SampleProcessHttp node, String... channels) throws Exception
   {
      HttpURLConnection connection = (HttpURLConnection) node.base().resolve("/qqq/v1/realtime/stream").toURL().openConnection();
      connection.setRequestProperty("Cookie", "sessionId=" + node.sessionId());
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(0);
      SampleRealtimeStream stream = new SampleRealtimeStream(connection);
      try
      {
         JSONObject hello = stream.await("hello", event -> true, Duration.ofSeconds(10));
         String connectionId = hello.getJSONObject("data").getString("connectionId");
         HttpResponse<String> subscribed = node.postJson("/qqq/v1/realtime/subscriptions",
            new JSONObject().put("connectionId", connectionId).put("add", new JSONArray(List.of(channels))).toString());
         assertEquals(200, subscribed.statusCode(), subscribed.body());
         assertEquals(List.of(channels), new JSONObject(subscribed.body()).getJSONArray("active").toList());
         return (stream);
      }
      catch(Exception | AssertionError e)
      {
         stream.close();
         throw (e);
      }
   }



   /*******************************************************************************
    ** The first event with this name that matches, within the timeout.
    *******************************************************************************/
   JSONObject await(String eventName, Predicate<JSONObject> matches, Duration timeout) throws InterruptedException
   {
      long deadline = System.nanoTime() + timeout.toNanos();
      for(long remaining = timeout.toNanos(); remaining > 0; remaining = deadline - System.nanoTime())
      {
         JSONObject event = events.poll(remaining, TimeUnit.NANOSECONDS);
         if(event != null && eventName.equals(event.optString("event")) && matches.test(event))
         {
            return (event);
         }
      }
      throw (new AssertionError("No " + eventName + " event within " + timeout));
   }



   /*******************************************************************************
    ** Parse SSE frames (event, id, data lines; a blank line ends one).
    *******************************************************************************/
   private void read()
   {
      try(BufferedReader lines = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)))
      {
         JSONObject event = new JSONObject();
         for(String line = lines.readLine(); line != null; line = lines.readLine())
         {
            if(line.isEmpty())
            {
               if(event.has("event"))
               {
                  events.add(event);
               }
               event = new JSONObject();
            }
            else if(line.startsWith("event:"))
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
      }
      catch(Exception e)
      {
         ////////////////////////////////////////////
         // closed by the test, or the node ended //
         ////////////////////////////////////////////
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void close()
   {
      connection.disconnect();
      reader.interrupt();
   }
}
