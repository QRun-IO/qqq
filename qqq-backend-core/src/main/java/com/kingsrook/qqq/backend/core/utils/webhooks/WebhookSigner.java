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


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/*******************************************************************************
 ** Signs webhook requests in the Standard Webhooks format
 ** (https://www.standardwebhooks.com/).
 **
 ** <p>The signed content is {@code <id>.<timestamp>.<body>}: the message id,
 ** a dot, the Unix timestamp in seconds, a dot, then the exact body bytes. The
 ** signature is HMAC-SHA256 with the secret's key, base64 encoded, and sent as
 ** {@code v1,<base64>}. During a secret rotation every active secret signs, and
 ** the signatures are separated by spaces, so a receiver holding either the old
 ** or the new secret accepts the request.</p>
 **
 ** <p>Headers: {@code webhook-id}, {@code webhook-timestamp},
 ** {@code webhook-signature}.</p>
 *******************************************************************************/
public final class WebhookSigner
{
   public static final String HEADER_ID        = "webhook-id";
   public static final String HEADER_TIMESTAMP = "webhook-timestamp";
   public static final String HEADER_SIGNATURE = "webhook-signature";

   /** The only signature scheme today: HMAC-SHA256. */
   public static final String VERSION_V1 = "v1";

   private static final String HMAC = "HmacSHA256";



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private WebhookSigner()
   {
   }



   /*******************************************************************************
    ** The three signature headers for one request.
    **
    ** @param messageId  the event id (stable across retries: it is the idempotency key)
    ** @param timestamp  when this attempt is sent
    ** @param body       the exact bytes that are sent
    ** @param secrets    the active secrets (one, or two during a rotation)
    *******************************************************************************/
   public static Map<String, String> headers(String messageId, Instant timestamp, byte[] body, List<WebhookSecret> secrets)
   {
      long seconds = timestamp.getEpochSecond();
      Map<String, String> headers = new LinkedHashMap<>();
      headers.put(HEADER_ID, messageId);
      headers.put(HEADER_TIMESTAMP, Long.toString(seconds));
      headers.put(HEADER_SIGNATURE, signatureHeader(messageId, seconds, body, secrets));
      return (headers);
   }



   /*******************************************************************************
    ** The {@code webhook-signature} value: {@code v1,<sig>} per secret, space
    ** separated.
    *******************************************************************************/
   public static String signatureHeader(String messageId, long timestampSeconds, byte[] body, List<WebhookSecret> secrets)
   {
      if(secrets == null || secrets.isEmpty())
      {
         throw (new IllegalArgumentException("At least one signing secret is required"));
      }
      List<String> parts = new ArrayList<>();
      for(WebhookSecret secret : secrets)
      {
         parts.add(VERSION_V1 + "," + sign(messageId, timestampSeconds, body, secret));
      }
      return (String.join(" ", parts));
   }



   /*******************************************************************************
    ** One base64 HMAC-SHA256 signature over {@code <id>.<timestamp>.<body>}.
    *******************************************************************************/
   public static String sign(String messageId, long timestampSeconds, byte[] body, WebhookSecret secret)
   {
      validateId(messageId);
      try
      {
         Mac mac = Mac.getInstance(HMAC);
         mac.init(new SecretKeySpec(secret.keyBytes(), HMAC));
         mac.update((messageId + "." + timestampSeconds + ".").getBytes(StandardCharsets.UTF_8));
         mac.update(body == null ? new byte[0] : body);
         return (Base64.getEncoder().encodeToString(mac.doFinal()));
      }
      catch(GeneralSecurityException e)
      {
         throw (new IllegalStateException("HMAC-SHA256 is unavailable", e));
      }
   }



   /*******************************************************************************
    ** A message id may not contain the separator or whitespace.
    *******************************************************************************/
   static void validateId(String messageId)
   {
      if(messageId == null || messageId.isEmpty() || messageId.contains(".") || messageId.chars().anyMatch(Character::isWhitespace))
      {
         throw (new IllegalArgumentException("A webhook message id is required and may not contain '.' or whitespace"));
      }
   }
}
