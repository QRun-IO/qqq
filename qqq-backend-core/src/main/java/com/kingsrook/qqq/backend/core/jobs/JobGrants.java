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

package com.kingsrook.qqq.backend.core.jobs;


import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import com.kingsrook.qqq.backend.core.crypto.KeyMaterialUtils;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Job grants (owner-approved design, spec 2026-10-15-cluster-ha-design item 6):
 ** how a worker node runs a user's durable job as that user - with the user's
 ** CURRENT permissions, never more than at launch, and never through a
 ** tokenless super-session.
 **
 ** At launch, the web node binds the job to the launching sign-in: it encrypts
 ** that session's id (AES-GCM, key derived from QQQ_JOB_GRANT_KEY, bound to the
 ** job and user) and signs job id + user id + encrypted session + launch scope
 ** + expiry (HMAC-SHA256, another derived key). The grant is kept in the job's
 ** credential scope.
 **
 ** At each claim, the worker verifies the signature and expiry (a tampered or
 ** expired grant fails the job), decrypts the session id, and asks the
 ** authentication module to rebuild that session the normal way - for OAuth2,
 ** from the stored (encrypted) access/refresh tokens, so the provider confirms
 ** the user is still signed in and their groups are current. JobSessions then
 ** intersects the result with the launch scope. A sign-in that has ended
 ** (logout, revoked or expired refresh token, max session lifetime) fails the
 ** job cleanly; nothing escalates.
 *******************************************************************************/
public final class JobGrants
{
   /** the credential scope key holding the grant */
   public static final String SCOPE_GRANT = "__grant";

   private static final String VERSION         = "g1";
   private static final int    NONCE_BYTES     = 12;
   private static final int    TAG_BITS        = 128;
   private static final String MAC_ALGORITHM   = "HmacSHA256";
   private static final String ENCRYPT_PURPOSE = "qqq.jobGrant.encrypt.v1";
   private static final String SIGN_PURPOSE    = "qqq.jobGrant.sign.v1";

   private static final SecureRandom SECURE_RANDOM = new SecureRandom();



   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private JobGrants()
   {
   }



   /*******************************************************************************
    ** The configured grant key bytes, or null when grants are off.
    *******************************************************************************/
   static byte[] key(QInstance instance) throws QException
   {
      DurableJobsMetaData settings = DurableJobsMetaData.of(instance);
      if(settings == null || !StringUtils.hasContent(settings.getJobGrantKey()))
      {
         return (null);
      }
      String value = new QMetaDataVariableInterpreter().interpret(settings.getJobGrantKey());
      if(!StringUtils.hasContent(value) || value.startsWith("${"))
      {
         return (null);
      }
      return (KeyMaterialUtils.decodeBase64Key(value.trim(), "durable jobs jobGrantKey", KeyMaterialUtils.AES_256_KEY_LENGTH_BYTES));
   }



   /*******************************************************************************
    ** Whether the instance issues job grants.
    *******************************************************************************/
   public static boolean isEnabled(QInstance instance)
   {
      try
      {
         return (key(instance) != null);
      }
      catch(QException e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    ** Issue a grant for a launched job: the value to keep in its credential
    ** scope (under SCOPE_GRANT).
    *******************************************************************************/
   public static String issue(QInstance instance, Integer jobId, String userId, String sessionUUID, Map<String, List<Serializable>> scope) throws QException
   {
      byte[] key = key(instance);
      if(key == null || jobId == null || !StringUtils.hasContent(userId) || !StringUtils.hasContent(sessionUUID))
      {
         throw (new QException("A job grant needs a key, a job, a user and a session."));
      }

      DurableJobsMetaData settings = DurableJobsMetaData.of(instance);
      long expires = Instant.now().plusSeconds(3600L * Objects.requireNonNullElse(settings.getJobGrantTtlHours(), 168)).getEpochSecond();
      String sealedSession = encrypt(derive(key, ENCRYPT_PURPOSE), sessionUUID, associatedData(jobId, userId));
      String signature = sign(derive(key, SIGN_PURPOSE), jobId, userId, sealedSession, scopeDigest(scope), expires);
      return (String.join(".", VERSION, String.valueOf(expires), sealedSession, signature));
   }



   /*******************************************************************************
    ** Verify a job's grant and return the launching session's id. Throws
    ** QPermissionDeniedException when the grant is missing, malformed, tampered
    ** with, for another job/user/scope, or expired.
    *******************************************************************************/
   public static String verify(QInstance instance, Integer jobId, String userId, String grant, Map<String, List<Serializable>> scope) throws QException
   {
      byte[] key = key(instance);
      if(key == null)
      {
         throw (new QPermissionDeniedException("Job grants are not configured on this node (QQQ_JOB_GRANT_KEY)."));
      }
      if(!StringUtils.hasContent(grant))
      {
         throw (new QPermissionDeniedException("The job has no grant."));
      }

      String[] parts = grant.split("\\.");
      if(parts.length != 4 || !VERSION.equals(parts[0]))
      {
         throw (new QPermissionDeniedException("The job's grant is not valid."));
      }

      long expires;
      try
      {
         expires = Long.parseLong(parts[1]);
      }
      catch(NumberFormatException e)
      {
         throw (new QPermissionDeniedException("The job's grant is not valid."));
      }

      String expected = sign(derive(key, SIGN_PURPOSE), jobId, userId, parts[2], scopeDigest(scope), expires);
      if(!MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), parts[3].getBytes(StandardCharsets.UTF_8)))
      {
         throw (new QPermissionDeniedException("The job's grant does not match the job (it was changed or belongs to another job)."));
      }
      if(Instant.now().getEpochSecond() > expires)
      {
         throw (new QPermissionDeniedException("The job's grant has expired; run the job again."));
      }

      return (decrypt(derive(key, ENCRYPT_PURPOSE), parts[2], associatedData(jobId, userId)));
   }



   /*******************************************************************************
    ** A stable digest of the launch scope (sorted keys and values), so the
    ** grant covers exactly the scope it was issued for.
    *******************************************************************************/
   static String scopeDigest(Map<String, List<Serializable>> scope) throws QException
   {
      TreeMap<String, List<String>> canonical = new TreeMap<>();
      if(scope != null)
      {
         scope.forEach((name, values) ->
         {
            if(!SCOPE_GRANT.equals(name))
            {
               List<String> strings = new ArrayList<>();
               for(Serializable value : Objects.requireNonNullElse(values, List.<Serializable>of()))
               {
                  strings.add(String.valueOf(value));
               }
               strings.sort(String::compareTo);
               canonical.put(name, strings);
            }
         });
      }
      try
      {
         byte[] digest = MessageDigest.getInstance("SHA-256").digest(JsonUtils.toJson(canonical).getBytes(StandardCharsets.UTF_8));
         return (Base64.getUrlEncoder().withoutPadding().encodeToString(digest));
      }
      catch(Exception e)
      {
         throw (new QException("Could not digest the job scope", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String associatedData(Integer jobId, String userId)
   {
      return ("qqq.jobGrant|" + jobId + "|" + userId);
   }



   /*******************************************************************************
    ** A purpose-specific key derived from the configured key.
    *******************************************************************************/
   private static byte[] derive(byte[] key, String purpose) throws QException
   {
      try
      {
         Mac mac = Mac.getInstance(MAC_ALGORITHM);
         mac.init(new SecretKeySpec(key, MAC_ALGORITHM));
         return (mac.doFinal(purpose.getBytes(StandardCharsets.UTF_8)));
      }
      catch(Exception e)
      {
         throw (new QException("Could not derive the job grant key", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String sign(byte[] key, Integer jobId, String userId, String sealedSession, String scopeDigest, long expires) throws QException
   {
      try
      {
         Mac mac = Mac.getInstance(MAC_ALGORITHM);
         mac.init(new SecretKeySpec(key, MAC_ALGORITHM));
         String payload = String.join("|", VERSION, String.valueOf(jobId), userId, sealedSession, scopeDigest, String.valueOf(expires));
         return (Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8))));
      }
      catch(Exception e)
      {
         throw (new QException("Could not sign the job grant", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String encrypt(byte[] key, String plaintext, String associatedData) throws QException
   {
      try
      {
         byte[] nonce = new byte[NONCE_BYTES];
         SECURE_RANDOM.nextBytes(nonce);
         Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
         cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
         cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
         byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
         return (Base64.getUrlEncoder().withoutPadding().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array()));
      }
      catch(Exception e)
      {
         throw (new QException("Could not seal the job grant", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String decrypt(byte[] key, String sealed, String associatedData) throws QException
   {
      try
      {
         ByteBuffer buffer = ByteBuffer.wrap(Base64.getUrlDecoder().decode(sealed));
         byte[]     nonce  = new byte[NONCE_BYTES];
         buffer.get(nonce);
         byte[] encrypted = new byte[buffer.remaining()];
         buffer.get(encrypted);
         Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
         cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, nonce));
         cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
         return (new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8));
      }
      catch(Exception e)
      {
         throw (new QPermissionDeniedException("The job's grant could not be opened."));
      }
   }
}
