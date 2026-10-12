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
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;


/*******************************************************************************
 ** A webhook signing secret (Standard Webhooks format).
 **
 ** <p>The text form is {@code whsec_} followed by the base64 of the key bytes.
 ** The HMAC key is the decoded bytes. A secret without the prefix is used as
 ** raw UTF-8 bytes, so a secret shared with an older receiver still works.</p>
 **
 ** <p>{@link #toString()} never reveals the key: it prints the fingerprint.</p>
 *******************************************************************************/
public final class WebhookSecret
{
   public static final String PREFIX = "whsec_";

   /** Key length for generated secrets, in bytes (256 bits). */
   public static final int GENERATED_KEY_BYTES = 32;

   /** Shortest key accepted, in bytes (128 bits). */
   public static final int MINIMUM_KEY_BYTES = 16;

   private static final SecureRandom RANDOM = new SecureRandom();

   private final byte[] key;



   /*******************************************************************************
    ** Constructor (keeps a copy of the key).
    *******************************************************************************/
   private WebhookSecret(byte[] key)
   {
      if(key == null || key.length < MINIMUM_KEY_BYTES)
      {
         throw (new IllegalArgumentException("A webhook signing secret must be at least " + MINIMUM_KEY_BYTES + " bytes"));
      }
      this.key = Arrays.copyOf(key, key.length);
   }



   /*******************************************************************************
    ** A new random secret.
    *******************************************************************************/
   public static WebhookSecret generate()
   {
      byte[] bytes = new byte[GENERATED_KEY_BYTES];
      RANDOM.nextBytes(bytes);
      return (new WebhookSecret(bytes));
   }



   /*******************************************************************************
    ** Parse the text form ({@code whsec_<base64>}, or raw text).
    *******************************************************************************/
   public static WebhookSecret parse(String text)
   {
      if(text == null || text.isBlank())
      {
         throw (new IllegalArgumentException("A webhook signing secret is required"));
      }
      String trimmed = text.trim();
      if(trimmed.startsWith(PREFIX))
      {
         try
         {
            return (new WebhookSecret(Base64.getDecoder().decode(trimmed.substring(PREFIX.length()))));
         }
         catch(IllegalArgumentException e)
         {
            throw (new IllegalArgumentException("A " + PREFIX + " secret must be base64 after the prefix", e));
         }
      }
      return (new WebhookSecret(trimmed.getBytes(StandardCharsets.UTF_8)));
   }



   /*******************************************************************************
    ** The text form to give a receiver ({@code whsec_<base64>}).
    *******************************************************************************/
   public String reveal()
   {
      return (PREFIX + Base64.getEncoder().encodeToString(key));
   }



   /*******************************************************************************
    ** The HMAC key (a copy).
    *******************************************************************************/
   public byte[] keyBytes()
   {
      return (Arrays.copyOf(key, key.length));
   }



   /*******************************************************************************
    ** A short, non-reversible id for the secret (first 12 hex chars of its
    ** SHA-256), safe to store and show, so people can tell secrets apart.
    *******************************************************************************/
   public String fingerprint()
   {
      try
      {
         byte[] digest = MessageDigest.getInstance("SHA-256").digest(key);
         return (HexFormat.of().formatHex(digest).substring(0, 12));
      }
      catch(NoSuchAlgorithmException e)
      {
         throw (new IllegalStateException("SHA-256 is unavailable", e));
      }
   }



   /*******************************************************************************
    ** Never prints the key.
    *******************************************************************************/
   @Override
   public String toString()
   {
      return ("WebhookSecret[" + fingerprint() + "]");
   }



   /*******************************************************************************
    ** Equal when the keys are equal (constant time).
    *******************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      return (o instanceof WebhookSecret other && MessageDigest.isEqual(key, other.key));
   }



   /*******************************************************************************
    ** Hash of the fingerprint.
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return (fingerprint().hashCode());
   }
}
