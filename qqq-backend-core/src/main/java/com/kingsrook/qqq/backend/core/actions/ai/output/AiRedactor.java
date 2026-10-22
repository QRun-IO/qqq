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


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Replaces secrets and PII in values sent to a model with [REDACTED:kind]:
 ** private keys, JWTs, API keys (sk-...), AWS access keys and (labelled) secret
 ** keys, US social security numbers, payment card numbers (Luhn-valid, 13-19
 ** digits) and any custom patterns (kind "custom").
 **
 ** Runs on every string value before truncation, so a long secret is never left
 ** half-redacted by a cut.
 *******************************************************************************/
public class AiRedactor
{
   public static final String KIND_PRIVATE_KEY = "private_key";
   public static final String KIND_JWT         = "jwt";
   public static final String KIND_API_KEY     = "api_key";
   public static final String KIND_AWS_KEY     = "aws_access_key";
   public static final String KIND_AWS_SECRET  = "aws_secret_key";
   public static final String KIND_SSN         = "ssn";
   public static final String KIND_CARD_NUMBER = "card_number";
   public static final String KIND_CUSTOM      = "custom";

   private static final Pattern PRIVATE_KEY_PATTERN = Pattern.compile("-----BEGIN [A-Z0-9 ]*PRIVATE KEY-----[\\s\\S]*?(?:-----END [A-Z0-9 ]*PRIVATE KEY-----|$)");
   private static final Pattern JWT_PATTERN         = Pattern.compile("\\beyJ[A-Za-z0-9_-]{5,}\\.eyJ[A-Za-z0-9_-]{5,}\\.[A-Za-z0-9_-]{5,}");
   private static final Pattern API_KEY_PATTERN     = Pattern.compile("\\bsk-[A-Za-z0-9_-]{16,}");
   private static final Pattern AWS_KEY_PATTERN     = Pattern.compile("\\b(?:AKIA|ASIA|AGPA|AIDA|AROA|ANPA)[0-9A-Z]{16}\\b");
   private static final Pattern AWS_SECRET_PATTERN  = Pattern.compile("(?i)(aws.{0,20}?secret.{0,20}?[:=]\\s*[\"']?)[A-Za-z0-9/+=]{40}(?![A-Za-z0-9/+=])");
   private static final Pattern SSN_PATTERN         = Pattern.compile("\\b(?!000|666|9\\d\\d)\\d{3}-(?!00)\\d{2}-(?!0000)\\d{4}\\b");
   private static final Pattern CARD_PATTERN        = Pattern.compile("(?<![0-9])(?:[0-9][ -]?){12,18}[0-9](?![0-9])");

   private final List<Pattern> customPatterns;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public AiRedactor(Collection<Pattern> customPatterns)
   {
      this.customPatterns = new ArrayList<>(CollectionUtils.nonNullCollection(customPatterns));
   }



   /*******************************************************************************
    ** Redact a value - adding the kind of each redaction made to redactedKinds
    ** (which may be null).
    *******************************************************************************/
   public String redact(String value, Set<String> redactedKinds)
   {
      if(value == null || value.isEmpty())
      {
         return (value);
      }

      String result = value;
      result = replaceAll(result, PRIVATE_KEY_PATTERN, KIND_PRIVATE_KEY, redactedKinds);
      result = replaceAll(result, JWT_PATTERN, KIND_JWT, redactedKinds);
      result = replaceAll(result, API_KEY_PATTERN, KIND_API_KEY, redactedKinds);
      result = replaceAll(result, AWS_KEY_PATTERN, KIND_AWS_KEY, redactedKinds);
      result = replaceAwsSecrets(result, redactedKinds);
      result = replaceAll(result, SSN_PATTERN, KIND_SSN, redactedKinds);
      result = replaceCardNumbers(result, redactedKinds);

      for(Pattern customPattern : customPatterns)
      {
         result = replaceAll(result, customPattern, KIND_CUSTOM, redactedKinds);
      }

      return (result);
   }



   /*******************************************************************************
    ** The replacement text for a redaction of the given kind.
    *******************************************************************************/
   public static String replacement(String kind)
   {
      return ("[REDACTED:" + kind + "]");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String replaceAll(String value, Pattern pattern, String kind, Set<String> redactedKinds)
   {
      Matcher matcher = pattern.matcher(value);
      if(!matcher.find())
      {
         return (value);
      }

      noteKind(redactedKinds, kind);
      return (matcher.replaceAll(Matcher.quoteReplacement(replacement(kind))));
   }



   /*******************************************************************************
    ** AWS secret keys are only recognized after an "aws ... secret ... =" style
    ** label (a bare 40-character string is too ambiguous) - the label is kept.
    *******************************************************************************/
   private static String replaceAwsSecrets(String value, Set<String> redactedKinds)
   {
      Matcher matcher = AWS_SECRET_PATTERN.matcher(value);
      if(!matcher.find())
      {
         return (value);
      }

      noteKind(redactedKinds, KIND_AWS_SECRET);
      return (matcher.replaceAll(m -> Matcher.quoteReplacement(m.group(1) + replacement(KIND_AWS_SECRET))));
   }



   /*******************************************************************************
    ** Replace digit runs that look like payment card numbers - 13 to 19 digits
    ** (optionally separated by spaces or dashes) that pass the Luhn check.
    *******************************************************************************/
   private static String replaceCardNumbers(String value, Set<String> redactedKinds)
   {
      Matcher       matcher = CARD_PATTERN.matcher(value);
      StringBuilder result  = new StringBuilder();
      boolean       any     = false;
      while(matcher.find())
      {
         String digits = matcher.group().replaceAll("[^0-9]", "");
         if(digits.length() >= 13 && digits.length() <= 19 && passesLuhn(digits))
         {
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement(KIND_CARD_NUMBER)));
            any = true;
         }
      }

      if(!any)
      {
         return (value);
      }

      matcher.appendTail(result);
      noteKind(redactedKinds, KIND_CARD_NUMBER);
      return (result.toString());
   }



   /*******************************************************************************
    ** The Luhn (mod 10) check, as used by payment card numbers.
    *******************************************************************************/
   static boolean passesLuhn(String digits)
   {
      int     sum          = 0;
      boolean shouldDouble = false;
      for(int i = digits.length() - 1; i >= 0; i--)
      {
         int digit = digits.charAt(i) - '0';
         if(shouldDouble)
         {
            digit *= 2;
            if(digit > 9)
            {
               digit -= 9;
            }
         }
         sum += digit;
         shouldDouble = !shouldDouble;
      }
      return (sum % 10 == 0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void noteKind(Set<String> redactedKinds, String kind)
   {
      if(redactedKinds != null)
      {
         redactedKinds.add(kind);
      }
   }
}
