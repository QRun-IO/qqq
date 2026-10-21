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


package com.kingsrook.qqq.backend.core.jobs.effects;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;


/*******************************************************************************
 ** The deterministic idempotency key of an effect, and the forms targets need
 ** (spec section 2.13): jobId:chunkSeq:rowKey:effectName. A rerun chunk derives
 ** the same keys, so the outbox, the provider and the target all dedupe.
 *******************************************************************************/
public final class EffectKeys
{
   /*******************************************************************************
    ** Static helpers only.
    *******************************************************************************/
   private EffectKeys()
   {
   }



   /*******************************************************************************
    ** jobId:chunkSeq:rowKey:effectName.
    *******************************************************************************/
   public static String key(Integer jobId, Integer chunkSeq, String rowKey, String effectName)
   {
      return (jobId + ":" + chunkSeq + ":" + Objects.requireNonNullElse(rowKey, "") + ":" + effectName);
   }



   /*******************************************************************************
    ** A short, URL- and filename-safe digest of a key (SHA-256, hex, 32 chars):
    ** for object names, header values and Message-IDs whatever the row key holds.
    *******************************************************************************/
   public static String digest(String key)
   {
      try
      {
         byte[] hash = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
         return (HexFormat.of().formatHex(hash).substring(0, 32));
      }
      catch(NoSuchAlgorithmException e)
      {
         throw (new IllegalStateException("SHA-256 is not available", e));
      }
   }



   /*******************************************************************************
    ** The provider Idempotency-Key header value for an effect.
    *******************************************************************************/
   public static String idempotencyKeyHeader(String key)
   {
      return ("qqq-" + digest(key));
   }



   /*******************************************************************************
    ** A deterministic object name under a prefix: a repeated PUT overwrites the
    ** same object instead of creating another.
    *******************************************************************************/
   public static String objectName(String prefix, String key, String extension)
   {
      String base = (prefix == null || prefix.isEmpty() ? "" : (prefix.endsWith("/") ? prefix : prefix + "/")) + digest(key);
      return (extension == null || extension.isEmpty() ? base : base + "." + extension);
   }



   /*******************************************************************************
    ** A fixed RFC 5322 Message-ID for an email effect, so a resend carries the
    ** same id and receiving systems (and reconcile lookups) recognize it.
    *******************************************************************************/
   public static String messageId(String key, String domain)
   {
      return ("<" + digest(key) + "@" + domain + ">");
   }
}
