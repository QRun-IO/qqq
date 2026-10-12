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

package com.kingsrook.qqq.backend.core.utils.http;


import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;


/*******************************************************************************
 ** The result of a {@link SafeHttpClient} request: the status, the headers
 ** (first value of each, names lower-cased), at most the policy's
 ** {@code maxResponseBytes} of the body, whether the body was cut off, the
 ** address actually connected to and the duration.
 *******************************************************************************/
public record SafeHttpResponse(int statusCode, Map<String, String> headers, byte[] body, boolean truncated, String remoteAddress, long durationMillis)
{
   /*******************************************************************************
    ** True for a 2xx status.
    *******************************************************************************/
   public boolean isSuccess()
   {
      return (statusCode >= 200 && statusCode < 300);
   }



   /*******************************************************************************
    ** A header value (case-insensitive), or null.
    *******************************************************************************/
   public String header(String name)
   {
      return (headers == null || name == null ? null : headers.get(name.toLowerCase(Locale.ROOT)));
   }



   /*******************************************************************************
    ** The body as UTF-8 text, cut to at most {@code maxChars}.
    *******************************************************************************/
   public String bodyExcerpt(int maxChars)
   {
      if(body == null || body.length == 0)
      {
         return ("");
      }
      String text = new String(body, StandardCharsets.UTF_8);
      return (text.length() > maxChars ? text.substring(0, maxChars) : text);
   }
}
