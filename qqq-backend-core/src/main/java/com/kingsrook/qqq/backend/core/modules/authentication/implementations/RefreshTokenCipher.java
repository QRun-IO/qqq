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

package com.kingsrook.qqq.backend.core.modules.authentication.implementations;


import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Authenticated encryption (AES-256-GCM) of OAuth2 refresh tokens at rest
 ** (#1048), so a copy of the refresh-token table (a backup, a replica, an
 ** over-broad DB role) does not hand out live credentials.
 **
 ** Stored values look like:  v1:<keyId>:<base64(nonce | ciphertext | tag)>
 ** with a random 96-bit nonce per value.  The associated data binds each value
 ** to its session (uuid + userId), so a ciphertext copied to another row will
 ** not decrypt.  The keyId names the key that encrypted the value, so keys can
 ** be rotated: values under a previous key still decrypt, and are re-encrypted
 ** under the current key the next time they are written.
 **
 ** Keys come from configuration (OAuth2AuthenticationMetaData), never from the
 ** database; they may be given as ${env.X} / ${prop.X} references.  Messages
 ** from this class never include key or token material.
 *******************************************************************************/
public class RefreshTokenCipher
{
   public static final String VERSION        = "v1";
   public static final String DEFAULT_KEY_ID = "default";

   private static final String  TRANSFORMATION      = "AES/GCM/NoPadding";
   private static final int     KEY_LENGTH_BYTES    = 32;
   private static final int     NONCE_LENGTH_BYTES  = 12;
   private static final int     TAG_LENGTH_BITS     = 128;
   private static final Pattern KEY_ID_PATTERN      = Pattern.compile("[A-Za-z0-9_.-]{1,64}");
   private static final String  AAD_CONTEXT_PREFIX  = "qqq.oauth2.refreshToken." + VERSION;

   private static final SecureRandom SECURE_RANDOM = new SecureRandom();

   private final String                 currentKeyId;
   private final Map<String, SecretKey> keysById;



   /*******************************************************************************
    ** Constructor - use fromMetaData.
    *******************************************************************************/
   private RefreshTokenCipher(String currentKeyId, Map<String, SecretKey> keysById)
   {
      this.currentKeyId = currentKeyId;
      this.keysById = keysById;
   }



   /*******************************************************************************
    ** Build a cipher from the OAuth2 meta-data's key settings.  Returns null if
    ** no current key is configured (refreshTokenEncryptionKey is unset).  Throws
    ** if any configured key or key id is invalid (the message names the setting,
    ** never the key).
    *******************************************************************************/
   public static RefreshTokenCipher fromMetaData(OAuth2AuthenticationMetaData metaData) throws QException
   {
      if(metaData == null || !StringUtils.hasContent(metaData.getRefreshTokenEncryptionKey()))
      {
         if(metaData != null && metaData.getPreviousRefreshTokenEncryptionKeys() != null && !metaData.getPreviousRefreshTokenEncryptionKeys().isEmpty())
         {
            throw (new QException("previousRefreshTokenEncryptionKeys is set, but refreshTokenEncryptionKey is not"));
         }
         return (null);
      }

      QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();

      String currentKeyId = Objects.requireNonNullElse(metaData.getRefreshTokenEncryptionKeyId(), DEFAULT_KEY_ID);
      validateKeyId(currentKeyId, "refreshTokenEncryptionKeyId");

      Map<String, SecretKey> keysById = new HashMap<>();
      keysById.put(currentKeyId, decodeKey(interpreter.interpret(metaData.getRefreshTokenEncryptionKey()), "refreshTokenEncryptionKey"));

      if(metaData.getPreviousRefreshTokenEncryptionKeys() != null)
      {
         for(Map.Entry<String, String> entry : metaData.getPreviousRefreshTokenEncryptionKeys().entrySet())
         {
            String keyId = entry.getKey();
            validateKeyId(keyId, "previousRefreshTokenEncryptionKeys key id");
            if(keysById.containsKey(keyId))
            {
               throw (new QException("previousRefreshTokenEncryptionKeys may not reuse the current key id [" + keyId + "]"));
            }
            keysById.put(keyId, decodeKey(interpreter.interpret(entry.getValue()), "previousRefreshTokenEncryptionKeys[" + keyId + "]"));
         }
      }

      return (new RefreshTokenCipher(currentKeyId, keysById));
   }



   /*******************************************************************************
    ** Build the associated data that binds a stored value to its session.
    *******************************************************************************/
   public static String buildAssociatedData(String sessionUuid, String userId)
   {
      return (AAD_CONTEXT_PREFIX + "|" + Objects.requireNonNullElse(sessionUuid, "") + "|" + Objects.requireNonNullElse(userId, ""));
   }



   /*******************************************************************************
    ** Encrypt a refresh token under the current key.
    *******************************************************************************/
   public String encrypt(String plaintext, String associatedData) throws QException
   {
      try
      {
         byte[] nonce = new byte[NONCE_LENGTH_BYTES];
         SECURE_RANDOM.nextBytes(nonce);

         Cipher cipher = Cipher.getInstance(TRANSFORMATION);
         cipher.init(Cipher.ENCRYPT_MODE, keysById.get(currentKeyId), new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
         cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
         byte[] ciphertextAndTag = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

         byte[] payload = ByteBuffer.allocate(nonce.length + ciphertextAndTag.length).put(nonce).put(ciphertextAndTag).array();
         return (VERSION + ":" + currentKeyId + ":" + Base64.getEncoder().encodeToString(payload));
      }
      catch(GeneralSecurityException e)
      {
         throw (new QException("Error encrypting refresh token", e));
      }
   }



   /*******************************************************************************
    ** Decrypt a stored value.  Throws (with a short, token-free reason as the
    ** message) if the value is malformed, names an unknown key, or fails
    ** authentication (wrong key, tampered, or bound to a different session).
    *******************************************************************************/
   public String decrypt(String storedValue, String associatedData) throws QException
   {
      if(storedValue == null)
      {
         throw (new QException("missingValue"));
      }

      String[] parts = storedValue.split(":", 3);
      if(parts.length != 3 || !VERSION.equals(parts[0]))
      {
         throw (new QException("unrecognizedFormat"));
      }

      SecretKey key = keysById.get(parts[1]);
      if(key == null)
      {
         throw (new QException("unknownKeyId"));
      }

      byte[] payload;
      try
      {
         payload = Base64.getDecoder().decode(parts[2]);
      }
      catch(IllegalArgumentException e)
      {
         throw (new QException("malformedPayload"));
      }

      if(payload.length < NONCE_LENGTH_BYTES + TAG_LENGTH_BITS / 8)
      {
         throw (new QException("malformedPayload"));
      }

      try
      {
         Cipher cipher = Cipher.getInstance(TRANSFORMATION);
         cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, payload, 0, NONCE_LENGTH_BYTES));
         cipher.updateAAD(associatedData.getBytes(StandardCharsets.UTF_8));
         byte[] plaintext = cipher.doFinal(payload, NONCE_LENGTH_BYTES, payload.length - NONCE_LENGTH_BYTES);
         return (new String(plaintext, StandardCharsets.UTF_8));
      }
      catch(GeneralSecurityException e)
      {
         throw (new QException("authenticationFailed"));
      }
   }



   /*******************************************************************************
    ** Whether a stored value should be re-written under the current key (i.e.,
    ** it was not encrypted with the current key id).
    *******************************************************************************/
   public boolean needsReEncryption(String storedValue)
   {
      return (storedValue == null || !storedValue.startsWith(VERSION + ":" + currentKeyId + ":"));
   }



   /*******************************************************************************
    ** Getter for currentKeyId
    *******************************************************************************/
   public String getCurrentKeyId()
   {
      return (this.currentKeyId);
   }



   /*******************************************************************************
    ** Make sure a key id can be embedded in the stored format.
    *******************************************************************************/
   private static void validateKeyId(String keyId, String settingName) throws QException
   {
      if(keyId == null || !KEY_ID_PATTERN.matcher(keyId).matches())
      {
         throw (new QException(settingName + " must be 1-64 characters of letters, digits, '_', '.' or '-'"));
      }
   }



   /*******************************************************************************
    ** Decode a base64 key (after variable interpretation), requiring 32 bytes.
    *******************************************************************************/
   private static SecretKey decodeKey(String base64Key, String settingName) throws QException
   {
      if(!StringUtils.hasContent(base64Key))
      {
         throw (new QException(settingName + " is not set (or its variable did not resolve)"));
      }

      byte[] keyBytes;
      try
      {
         keyBytes = Base64.getDecoder().decode(base64Key.trim());
      }
      catch(IllegalArgumentException e)
      {
         throw (new QException(settingName + " is not valid base64"));
      }

      if(keyBytes.length != KEY_LENGTH_BYTES)
      {
         throw (new QException(settingName + " must decode to " + KEY_LENGTH_BYTES + " bytes (e.g., from: openssl rand -base64 32)"));
      }

      return (new SecretKeySpec(keyBytes, "AES"));
   }

}
