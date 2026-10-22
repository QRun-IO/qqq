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

package com.kingsrook.qqq.middleware.javalin.specs.invoke;


import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/*******************************************************************************
 ** The result of an in-process UI API call: exactly what an HTTP client of the
 ** UI API would have received.
 *******************************************************************************/
public class UiApiResponse
{
   private static final ObjectMapper MAPPER = new ObjectMapper();

   private final int                       status;
   private final Map<String, List<String>> headers;
   private final String                    body;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public UiApiResponse(int status, Map<String, List<String>> headers, String body)
   {
      this.status = status;
      this.headers = headers;
      this.body = body;
   }



   /*******************************************************************************
    ** Whether the status is 2xx.
    *******************************************************************************/
   public boolean isSuccess()
   {
      return (status >= 200 && status < 300);
   }



   /*******************************************************************************
    ** The body parsed as JSON (null for an empty body).
    *******************************************************************************/
   public JsonNode getJson() throws com.fasterxml.jackson.core.JsonProcessingException
   {
      return (body == null || body.isBlank() ? null : MAPPER.readTree(body));
   }



   /*******************************************************************************
    ** Getter for status
    *******************************************************************************/
   public int getStatus()
   {
      return (this.status);
   }



   /*******************************************************************************
    ** Getter for headers
    *******************************************************************************/
   public Map<String, List<String>> getHeaders()
   {
      return (this.headers);
   }



   /*******************************************************************************
    ** Getter for body
    *******************************************************************************/
   public String getBody()
   {
      return (this.body);
   }
}
