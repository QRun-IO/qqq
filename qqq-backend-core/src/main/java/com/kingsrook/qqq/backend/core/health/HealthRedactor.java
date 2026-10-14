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

package com.kingsrook.qqq.backend.core.health;


import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*******************************************************************************
 ** Conservative sanitization at the boundary between probes and health records.
 *******************************************************************************/
public final class HealthRedactor
{
   public static final String REDACTED = "[redacted]";

   private static final Pattern URL = Pattern.compile("(?i)\\b[a-z][a-z0-9+.-]*://[^\\s<>\\\"']+");
   private static final List<Pattern> SECRETS = List.of(
      Pattern.compile("(?s)-----BEGIN [^-]+-----.*?(?:-----END [^-]+-----|$)"),
      Pattern.compile("(?i)\\b(?:bearer|basic)\\s+[a-z0-9+/_.=~-]+"),
      Pattern.compile("(?i)\\b[\\w.-]*(?:key|secret|password|token|credential|authorization)[\\w.-]*\\s*[:=]\\s*(?:\\\"[^\\\"]*\\\"|'[^']*'|[^\\s,;&]+)"),
      Pattern.compile("\\b(?:AKIA|ASIA)[A-Z0-9]{16}\\b"),
      Pattern.compile("\\beyJ[a-zA-Z0-9_-]*\\.[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+"),
      Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"),
      Pattern.compile("(?<!\\w)\\+?(?:\\d[ .()-]*){10,15}(?!\\w)"),
      Pattern.compile("\\d{9,}"),
      Pattern.compile("[a-zA-Z0-9+/_=-]{32,}"),
      Pattern.compile("(?i)\\b[a-z0-9_.-]*(?:secret|password|credential)[a-z0-9_.-]*\\b"));



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private HealthRedactor()
   {
   }



   /*******************************************************************************
    ** Strip URL authentication and queries, then mask secrets and identifiers.
    *******************************************************************************/
   public static String redact(String value)
   {
      if(value == null)
      {
         return (null);
      }
      Matcher matcher = URL.matcher(value);
      StringBuilder urls = new StringBuilder();
      while(matcher.find())
      {
         String url = matcher.group();
         int query = url.indexOf('?');
         int fragment = url.indexOf('#');
         int end = query < 0 ? url.length() : query;
         if(fragment >= 0)
         {
            end = Math.min(end, fragment);
         }
         url = url.substring(0, end);
         int authorityStart = url.indexOf("://") + 3;
         int pathStart = url.indexOf('/', authorityStart);
         int authorityEnd = pathStart < 0 ? url.length() : pathStart;
         int at = url.lastIndexOf('@', authorityEnd);
         if(at >= authorityStart)
         {
            url = url.substring(0, authorityStart) + url.substring(at + 1);
         }
         matcher.appendReplacement(urls, Matcher.quoteReplacement(url));
      }
      matcher.appendTail(urls);
      String result = urls.toString();
      for(Pattern pattern : SECRETS)
      {
         result = pattern.matcher(result).replaceAll(REDACTED);
      }
      return (result);
   }



   /*******************************************************************************
    ** Never persist a stack trace or unredacted multiline exception message.
    *******************************************************************************/
   public static String redactThrowable(Throwable throwable)
   {
      String message = redact(throwable.getMessage());
      String firstLine = message == null ? "" : message.split("\\R", 2)[0];
      return (throwable.getClass().getSimpleName() + ": " + firstLine);
   }
}
