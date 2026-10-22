/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import java.util.ArrayList;
import java.util.List;
import kong.unirest.HttpResponse;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Collect whole response bodies and headers and detect even partial sentinel leaks.
 *******************************************************************************/
public class SentinelResponseScanner
{
   private final List<String> responses = new ArrayList<>();



   /*******************************************************************************
    ** Capture every body and header, including errors and downloads.
    *******************************************************************************/
   public void collect(HttpResponse<String> response)
   {
      responses.add(String.valueOf(response.getBody()));
      response.getHeaders().all().forEach(header -> collect(header.getName(), header.getValue()));
   }



   /*******************************************************************************
    ** Also accepts in-process invoker responses.
    *******************************************************************************/
   public void collect(String body, String headers)
   {
      responses.add(String.valueOf(body));
      responses.add(String.valueOf(headers));
   }



   /*******************************************************************************
    ** Every longer clear substring contains a five-character clear substring.
    *******************************************************************************/
   public void assertSafe(String... sentinels)
   {
      for(String sentinel : sentinels)
      {
         for(int i = 0; i <= sentinel.length() - 5; i++)
         {
            String fragment = sentinel.substring(i, i + 5);
            for(String response : responses)
            {
               assertFalse(response.contains(fragment), "Response leaked sentinel fragment: " + fragment);
            }
         }
      }
   }
}
