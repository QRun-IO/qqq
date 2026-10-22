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


package com.kingsrook.qqq.backend.core.jobs.progress;


import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/*******************************************************************************
 ** Conservative default redaction for credentials and common personal identifiers.
 ** Callers must report counts and names, never row values.
 *******************************************************************************/
public class DefaultJobLogRedactor implements JobLogRedactor
{
   private static final Pattern BLOB = Pattern.compile("(?<![A-Za-z0-9_/+=-])[A-Za-z0-9/+_-]{40,}={0,2}(?![A-Za-z0-9_/+=-])");

   private static final List<Pattern> PATTERNS = List.of(
      Pattern.compile("(?i)\\bBearer\\s+[^\\s,;]+"),
      Pattern.compile("(?i)(?:password|passwd|secret|token|api[_-]?key|authorization|credential|private[_-]?key)[\"']?\\s*[:=]\\s*(?:\"[^\"]*\"|'[^']*'|[^\\s,;]+)"),
      Pattern.compile("\\bAKIA[A-Z0-9]{12,}\\b"),
      Pattern.compile("\\beyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\b"),
      Pattern.compile("(?<=://)[^\\s/@]+(?::[^\\s/@]*)?@"),
      Pattern.compile("\\b[A-Fa-f0-9]{32,}\\b"),
      Pattern.compile("[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+"),
      Pattern.compile("(?<!\\d)\\d{3}-\\d{2}-\\d{4}(?!\\d)"),
      Pattern.compile("(?<!\\d)(?:\\+?1[-. ]?)?(?:\\(\\d{3}\\)[-. ]?|\\d{3}[-. ])\\d{3}[-. ]\\d{4}(?!\\d)"));



   /*******************************************************************************
    ** Redact before truncating so cutting a credential never evades a rule.
    *******************************************************************************/
   @Override
   public String redact(String message)
   {
      if(message == null)
      {
         return (null);
      }
      String text = message.replaceAll("[\\p{Cc}\\p{Cf}]", "");
      for(Pattern pattern : PATTERNS)
      {
         text = pattern.matcher(text).replaceAll("[redacted]");
      }
      text = BLOB.matcher(text).replaceAll(match -> looksLikeSecret(match.group()) ? "[redacted]" : Matcher.quoteReplacement(match.group()));
      return (text.substring(0, Math.min(1000, text.length())));
   }



   /*******************************************************************************
    ** A long token is a secret when it mixes upper case, lower case and digits
    ** with few separators. Storage keys and paths (many '/', '_' or '-') and
    ** table names (lower case) are left alone.
    *******************************************************************************/
   static boolean looksLikeSecret(String token)
   {
      boolean upper      = false;
      boolean lower      = false;
      boolean digit      = false;
      int     separators = 0;
      for(char c : token.toCharArray())
      {
         if(Character.isUpperCase(c))
         {
            upper = true;
         }
         else if(Character.isLowerCase(c))
         {
            lower = true;
         }
         else if(Character.isDigit(c))
         {
            digit = true;
         }
         else if(c == '/' || c == '_' || c == '-')
         {
            separators++;
         }
      }
      return (upper && lower && digit && separators * 20 <= token.length());
   }
}
