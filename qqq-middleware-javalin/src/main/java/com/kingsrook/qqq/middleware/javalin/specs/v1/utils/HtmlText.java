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

package com.kingsrook.qqq.middleware.javalin.specs.v1.utils;


import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;


/*******************************************************************************
 ** Convert display HTML to text without executing markup or fetching resources.
 *******************************************************************************/
public class HtmlText
{
   /*******************************************************************************
    ** Client-neutral representation for non-web clients.
    *******************************************************************************/
   public static String fromHtml(String html)
   {
      if(html == null)
      {
         return (null);
      }
      Document document = Jsoup.parse(html);
      document.select("script, style, template").remove();
      document.select("p, div, li, h1, h2, h3, h4, h5, h6, tr, blockquote, pre").after("\n");
      return (document.body().wholeText().replace('\u00a0', ' ').replaceAll("[ \t]+\n", "\n").replaceAll("\n{3,}", "\n\n").strip());
   }
}
