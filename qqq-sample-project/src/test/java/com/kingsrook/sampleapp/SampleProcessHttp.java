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


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** The /qqq/v1 process protocol (init, step, status) against one node, as one
 ** signed-in user (mock auth resumes a session by its sessionId cookie, so the
 ** same user can be served by any node).
 *******************************************************************************/
class SampleProcessHttp
{
   private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
   private final URI        base;
   private final String     sessionId;



   /*******************************************************************************
    **
    *******************************************************************************/
   SampleProcessHttp(URI base, String sessionId)
   {
      this.base = base;
      this.sessionId = sessionId;
   }



   /*******************************************************************************
    ** The same user on another node.
    *******************************************************************************/
   SampleProcessHttp on(URI otherBase)
   {
      return (new SampleProcessHttp(otherBase, sessionId));
   }



   /*******************************************************************************
    ** Start a process; values are a JSON object of process values.
    *******************************************************************************/
   JSONObject init(String process, String valuesJson, long stepTimeoutMillis) throws Exception
   {
      Map<String, String> fields = new LinkedHashMap<>();
      fields.put("stepTimeoutMillis", Long.toString(stepTimeoutMillis));
      if(valuesJson != null)
      {
         fields.put("values", valuesJson);
      }
      return (ok(post("/qqq/v1/processes/" + process + "/init", fields)));
   }



   /*******************************************************************************
    ** Continue a process at a step.
    *******************************************************************************/
   JSONObject step(String process, String processUUID, String step, String valuesJson) throws Exception
   {
      Map<String, String> fields = new LinkedHashMap<>();
      fields.put("stepTimeoutMillis", "10000");
      if(valuesJson != null)
      {
         fields.put("values", valuesJson);
      }
      return (ok(post("/qqq/v1/processes/" + process + "/" + processUUID + "/step/" + step, fields)));
   }



   /*******************************************************************************
    ** One status poll of an async step (any type, including ERROR).
    *******************************************************************************/
   JSONObject status(String process, String processUUID, String jobUUID) throws Exception
   {
      HttpResponse<String> response = get("/qqq/v1/processes/" + process + "/" + processUUID + "/status/" + jobUUID);
      assertEquals(200, response.statusCode(), response.body());
      return (new JSONObject(response.body()));
   }



   /*******************************************************************************
    ** Poll an async step until it is no longer RUNNING.
    *******************************************************************************/
   JSONObject awaitDone(String process, String processUUID, String jobUUID, Duration timeout) throws Exception
   {
      long       deadline = System.nanoTime() + timeout.toNanos();
      JSONObject result;
      do
      {
         result = status(process, processUUID, jobUUID);
         if(!"RUNNING".equals(result.optString("type")))
         {
            return (result);
         }
         Thread.sleep(100);
      }
      while(System.nanoTime() < deadline);
      return (fail("Step still running after " + timeout + ": " + result));
   }



   /*******************************************************************************
    ** Run a short process synchronously and return its result.
    *******************************************************************************/
   JSONObject run(String process, String valuesJson) throws Exception
   {
      JSONObject result = init(process, valuesJson, 15_000);
      assertEquals("COMPLETE", result.optString("type"), result.toString());
      return (result);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   HttpResponse<String> get(String path) throws Exception
   {
      return (client.send(builder(path).GET().build(), HttpResponse.BodyHandlers.ofString()));
   }



   /*******************************************************************************
    ** A multipart form post.
    *******************************************************************************/
   HttpResponse<String> post(String path, Map<String, String> fields) throws Exception
   {
      String        boundary = "SampleProcess" + UUID.randomUUID();
      StringBuilder body     = new StringBuilder();
      for(Map.Entry<String, String> field : fields.entrySet())
      {
         body.append("--").append(boundary).append("\r\nContent-Disposition: form-data; name=\"").append(field.getKey()).append("\"\r\n\r\n").append(field.getValue()).append("\r\n");
      }
      body.append("--").append(boundary).append("--\r\n");
      return (client.send(builder(path).header("Content-Type", "multipart/form-data; boundary=" + boundary)
         .POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofString()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private HttpRequest.Builder builder(String path)
   {
      return (HttpRequest.newBuilder(base.resolve(path)).header("Cookie", "sessionId=" + sessionId).timeout(Duration.ofSeconds(20)));
   }



   /*******************************************************************************
    ** A 200 process response that is not an error.
    *******************************************************************************/
   private static JSONObject ok(HttpResponse<String> response)
   {
      assertEquals(200, response.statusCode(), response.body());
      JSONObject result = new JSONObject(response.body());
      if(result.has("error") || "ERROR".equals(result.optString("type")))
      {
         fail("Process error: " + result);
      }
      return (result);
   }
}
