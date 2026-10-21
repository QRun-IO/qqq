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

package com.kingsrook.qqq.backend.core.actions.ai.output;


import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*******************************************************************************
 ** Wraps tool output (JSON text) in the data envelope the assistant's system
 ** prompt declares as untrusted data:
 **
 **    <qqq_data source="query_records" table="blog_post" trust="untrusted">{...}</qqq_data>
 **
 ** Any opening or closing qqq_data tag inside the data (e.g., injected into a
 ** record's text by a user) is neutralized by replacing its "&lt;" with the JSON
 ** escape for that character - which keeps the JSON valid and equal in value,
 ** but means the text can no longer close (or open) an envelope.
 *******************************************************************************/
public class AiDataEnvelope
{
   public static final String TAG_NAME = "qqq_data";

   private static final Pattern TAG_PATTERN       = Pattern.compile("(?i)<(\\s*/?\\s*qqq_data)");
   private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("[^A-Za-z0-9_.:-]");

   ///////////////////////////////////////////////////////////////////
   // a JSON unicode escape for the less-than character (backslash, //
   // u, 003c) - built in two parts to keep it out of a literal     //
   ///////////////////////////////////////////////////////////////////
   private static final String LESS_THAN_JSON_ESCAPE = "\\" + "u003c";



   /*******************************************************************************
    ** Wrap JSON data from a tool (source) - about a table (which may be null).
    *******************************************************************************/
   public static String wrap(String source, String table, String json)
   {
      StringBuilder rs = new StringBuilder("<").append(TAG_NAME).append(" source=\"").append(attribute(source)).append('"');
      if(table != null)
      {
         rs.append(" table=\"").append(attribute(table)).append('"');
      }
      rs.append(" trust=\"untrusted\">");
      rs.append(neutralize(json));
      rs.append("</").append(TAG_NAME).append('>');
      return (rs.toString());
   }



   /*******************************************************************************
    ** Neutralize any qqq_data tags in JSON text.
    *******************************************************************************/
   public static String neutralize(String json)
   {
      if(json == null)
      {
         return ("");
      }
      return (TAG_PATTERN.matcher(json).replaceAll(m -> Matcher.quoteReplacement(LESS_THAN_JSON_ESCAPE + m.group(1))));
   }



   /*******************************************************************************
    ** An attribute value - with any characters outside a safe set removed.
    *******************************************************************************/
   static String attribute(String value)
   {
      if(value == null)
      {
         return ("");
      }
      return (ATTRIBUTE_PATTERN.matcher(value).replaceAll(""));
   }
}
