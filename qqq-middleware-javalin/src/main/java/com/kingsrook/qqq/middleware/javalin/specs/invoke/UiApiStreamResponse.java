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


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 ** A streaming UI API response. Close after consuming the body, including errors.
 *******************************************************************************/
public class UiApiStreamResponse implements AutoCloseable
{
   private final int status;
   private final Map<String, List<String>> headers;
   private final InputStream inputStream;



   /*******************************************************************************
    ** Construct a response owning the supplied body stream.
    *******************************************************************************/
   public UiApiStreamResponse(int status, Map<String, List<String>> headers, InputStream inputStream)
   {
      this.status = status;
      this.headers = headers;
      this.inputStream = inputStream;
   }



   /*******************************************************************************
    ** Getter for status.
    *******************************************************************************/
   public int getStatus()
   {
      return (status);
   }



   /*******************************************************************************
    ** Getter for headers.
    *******************************************************************************/
   public Map<String, List<String>> getHeaders()
   {
      return (headers);
   }



   /*******************************************************************************
    ** First header value, matched without regard to case; null when absent.
    *******************************************************************************/
   public String getHeader(String name)
   {
      return (headers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(name))
         .flatMap(entry -> entry.getValue().stream()).findFirst().orElse(null));
   }



   /*******************************************************************************
    ** Getter for the caller-owned response stream.
    *******************************************************************************/
   public InputStream getInputStream()
   {
      return (inputStream);
   }



   /*******************************************************************************
    ** Whether the HTTP status is successful.
    *******************************************************************************/
   public boolean isSuccess()
   {
      return (status >= 200 && status < 300);
   }



   /*******************************************************************************
    ** Consume the remaining body as UTF-8, useful for error responses.
    *******************************************************************************/
   public String readBodyAsString() throws IOException
   {
      return (new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
   }



   /*******************************************************************************
    ** Release the response stream and its connection resources.
    *******************************************************************************/
   @Override
   public void close() throws IOException
   {
      inputStream.close();
   }
}
