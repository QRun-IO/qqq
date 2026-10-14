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

package com.kingsrook.qqq.backend.core.utils.webhooks;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/*******************************************************************************
 ** Verifies a signed webhook request (Standard Webhooks format, see
 ** {@link WebhookSigner}).
 **
 ** <p>Checks, in order: the three headers are present, the timestamp is within
 ** the tolerance of now (default five minutes, either direction, which stops
 ** replays of old requests), and at least one {@code v1} signature matches one
 ** of the receiver's secrets. Comparison is constant time. Unknown signature
 ** versions are ignored, so a future scheme can be added alongside v1.</p>
 **
 ** <p>Receivers should also remember recent {@code webhook-id}s and ignore a
 ** repeat: the same id is sent again on every retry of the same event.</p>
 *******************************************************************************/
public final class WebhookVerifier
{
   public static final Duration DEFAULT_TOLERANCE = Duration.ofMinutes(5);

   private final List<WebhookSecret> secrets;
   private final Duration            tolerance;
   private final Clock               clock;



   /*******************************************************************************
    ** Verifier with the default tolerance and the system clock.
    *******************************************************************************/
   public WebhookVerifier(List<WebhookSecret> secrets)
   {
      this(secrets, DEFAULT_TOLERANCE, Clock.systemUTC());
   }



   /*******************************************************************************
    ** Verifier.
    *******************************************************************************/
   public WebhookVerifier(List<WebhookSecret> secrets, Duration tolerance, Clock clock)
   {
      if(secrets == null || secrets.isEmpty())
      {
         throw (new IllegalArgumentException("At least one secret is required to verify webhooks"));
      }
      this.secrets = new ArrayList<>(secrets);
      this.tolerance = tolerance;
      this.clock = clock;
   }



   /*******************************************************************************
    ** Verify a request. Header names are matched case-insensitively.
    **
    ** @throws WebhookVerificationException with a reason safe to log
    *******************************************************************************/
   public void verify(Map<String, String> headers, byte[] body) throws WebhookVerificationException
   {
      String id        = header(headers, WebhookSigner.HEADER_ID);
      String timestamp = header(headers, WebhookSigner.HEADER_TIMESTAMP);
      String signature = header(headers, WebhookSigner.HEADER_SIGNATURE);
      if(id == null || timestamp == null || signature == null)
      {
         throw (new WebhookVerificationException(WebhookVerificationException.Reason.MISSING_HEADERS, "Missing webhook-id, webhook-timestamp or webhook-signature"));
      }

      long seconds;
      try
      {
         seconds = Long.parseLong(timestamp.trim());
      }
      catch(NumberFormatException e)
      {
         throw (new WebhookVerificationException(WebhookVerificationException.Reason.BAD_TIMESTAMP, "webhook-timestamp is not a Unix timestamp"));
      }
      long now = clock.instant().getEpochSecond();
      if(Math.abs(now - seconds) > tolerance.toSeconds())
      {
         throw (new WebhookVerificationException(WebhookVerificationException.Reason.TIMESTAMP_OUT_OF_TOLERANCE, "webhook-timestamp is outside the allowed window"));
      }

      try
      {
         WebhookSigner.validateId(id);
      }
      catch(IllegalArgumentException e)
      {
         throw (new WebhookVerificationException(WebhookVerificationException.Reason.MISSING_HEADERS, e.getMessage()));
      }

      for(String part : signature.trim().split(" +"))
      {
         int comma = part.indexOf(',');
         if(comma < 0 || !WebhookSigner.VERSION_V1.equals(part.substring(0, comma)))
         {
            continue;
         }
         byte[] offered = part.substring(comma + 1).getBytes(StandardCharsets.US_ASCII);
         for(WebhookSecret secret : secrets)
         {
            byte[] expected = WebhookSigner.sign(id, seconds, body, secret).getBytes(StandardCharsets.US_ASCII);
            if(MessageDigest.isEqual(expected, offered))
            {
               return;
            }
         }
      }
      throw (new WebhookVerificationException(WebhookVerificationException.Reason.NO_MATCHING_SIGNATURE, "No v1 signature matches a known secret"));
   }



   /*******************************************************************************
    ** Case-insensitive header lookup.
    *******************************************************************************/
   private static String header(Map<String, String> headers, String name)
   {
      if(headers == null)
      {
         return (null);
      }
      for(Map.Entry<String, String> entry : headers.entrySet())
      {
         if(entry.getKey() != null && entry.getKey().toLowerCase(Locale.ROOT).equals(name))
         {
            return (entry.getValue());
         }
      }
      return (null);
   }
}
