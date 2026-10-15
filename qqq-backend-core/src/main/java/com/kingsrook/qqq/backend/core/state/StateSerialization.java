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

package com.kingsrook.qqq.backend.core.state;


import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import com.kingsrook.qqq.backend.core.crypto.KeyMaterialUtils;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Turns state values into the text a shared state provider stores, and back:
 ** Java serialization, gzip, then (with a key) AES-256-GCM bound to the state
 ** key, then base64. Reading goes through an ObjectInputFilter that allows only
 ** java.*, javax.*, com.kingsrook.* and the configured class prefixes, with
 ** depth and size limits.
 **
 ** Formats: "g1:" + base64(gzip) without a key; "e1:" + base64(nonce +
 ** ciphertext) with one. A value written in the other format reads as an error
 ** (callers treat it as absent).
 *******************************************************************************/
public final class StateSerialization
{
   private static final String PLAIN_PREFIX     = "g1:";
   private static final String ENCRYPTED_PREFIX = "e1:";
   private static final String TRANSFORMATION   = "AES/GCM/NoPadding";

   private static final int  NONCE_LENGTH_BYTES = 12;
   private static final int  TAG_LENGTH_BITS    = 128;
   private static final long MAX_BYTES          = 512L * 1024 * 1024;
   private static final long MAX_DEPTH          = 200;

   private static final SecureRandom SECURE_RANDOM = new SecureRandom();

   private static final List<String> DEFAULT_ALLOWED_PREFIXES = List.of("java.", "javax.", "com.kingsrook.");

   private final SecretKeySpec key;
   private final List<String>  allowedPrefixes;



   /*******************************************************************************
    ** Build from meta data (null meta data: no key, the default allow list).
    *******************************************************************************/
   public StateSerialization(StateProviderMetaData metaData) throws QException
   {
      allowedPrefixes = new ArrayList<>(DEFAULT_ALLOWED_PREFIXES);
      SecretKeySpec keySpec = null;
      if(metaData != null)
      {
         if(metaData.getAllowedClassPrefixes() != null)
         {
            allowedPrefixes.addAll(metaData.getAllowedClassPrefixes());
         }
         if(StringUtils.hasContent(metaData.getEncryptionKey()))
         {
            String keyValue = new QMetaDataVariableInterpreter().interpret(metaData.getEncryptionKey());
            if(StringUtils.hasContent(keyValue) && !keyValue.startsWith("${"))
            {
               byte[] keyBytes = KeyMaterialUtils.decodeBase64Key(keyValue.trim(), "stateProvider encryptionKey", KeyMaterialUtils.AES_256_KEY_LENGTH_BYTES);
               keySpec = new SecretKeySpec(keyBytes, "AES");
            }
         }
      }
      this.key = keySpec;
   }



   /*******************************************************************************
    ** Whether stored values are encrypted.
    *******************************************************************************/
   public boolean isEncrypted()
   {
      return (key != null);
   }



   /*******************************************************************************
    ** Serialize a value (bound to its state key when encrypting).
    *******************************************************************************/
   public String write(String stateKey, Serializable value) throws QException
   {
      byte[] bytes = toBytes(value);
      if(key == null)
      {
         return (PLAIN_PREFIX + Base64.getEncoder().encodeToString(bytes));
      }

      try
      {
         byte[] nonce = new byte[NONCE_LENGTH_BYTES];
         SECURE_RANDOM.nextBytes(nonce);
         Cipher cipher = Cipher.getInstance(TRANSFORMATION);
         cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
         cipher.updateAAD(stateKey.getBytes(StandardCharsets.UTF_8));
         byte[] encrypted = cipher.doFinal(bytes);
         byte[] payload   = ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array();
         return (ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(payload));
      }
      catch(Exception e)
      {
         throw (new QException("Error encrypting state", e));
      }
   }



   /*******************************************************************************
    ** Read a stored value back.
    *******************************************************************************/
   public Serializable read(String stateKey, String stored) throws QException
   {
      if(stored == null)
      {
         return (null);
      }

      byte[] bytes;
      if(stored.startsWith(PLAIN_PREFIX))
      {
         if(key != null)
         {
            throw (new QException("Stored state is not encrypted, but a state encryption key is configured"));
         }
         bytes = Base64.getDecoder().decode(stored.substring(PLAIN_PREFIX.length()));
      }
      else if(stored.startsWith(ENCRYPTED_PREFIX))
      {
         if(key == null)
         {
            throw (new QException("Stored state is encrypted, but no state encryption key is configured"));
         }
         bytes = decrypt(stateKey, stored.substring(ENCRYPTED_PREFIX.length()));
      }
      else
      {
         throw (new QException("Unrecognized stored state format"));
      }

      return (fromBytes(bytes));
   }



   /*******************************************************************************
    ** Decrypt a base64 nonce + ciphertext.
    *******************************************************************************/
   private byte[] decrypt(String stateKey, String base64) throws QException
   {
      try
      {
         ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(base64));
         byte[]     nonce  = new byte[NONCE_LENGTH_BYTES];
         buffer.get(nonce);
         byte[] encrypted = new byte[buffer.remaining()];
         buffer.get(encrypted);
         Cipher cipher = Cipher.getInstance(TRANSFORMATION);
         cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
         cipher.updateAAD(stateKey.getBytes(StandardCharsets.UTF_8));
         return (cipher.doFinal(encrypted));
      }
      catch(Exception e)
      {
         throw (new QException("Error decrypting state", e));
      }
   }



   /*******************************************************************************
    ** Java-serialize and gzip a value.
    *******************************************************************************/
   public static byte[] toBytes(Serializable value) throws QException
   {
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      try(ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(byteStream)))
      {
         out.writeObject(value);
      }
      catch(IOException | RuntimeException e)
      {
         throw (new QException("State value could not be serialized: " + e.getMessage(), e));
      }
      return (byteStream.toByteArray());
   }



   /*******************************************************************************
    ** Un-gzip and deserialize, through the allow-list filter.
    *******************************************************************************/
   public Serializable fromBytes(byte[] bytes) throws QException
   {
      try(ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes))))
      {
         in.setObjectInputFilter(this::filter);
         return ((Serializable) in.readObject());
      }
      catch(IOException | ClassNotFoundException | RuntimeException e)
      {
         throw (new QException("State value could not be read: " + e.getMessage(), e));
      }
   }



   /*******************************************************************************
    ** The allow-list filter.
    *******************************************************************************/
   ObjectInputFilter.Status filter(ObjectInputFilter.FilterInfo info)
   {
      if(info.depth() > MAX_DEPTH || info.streamBytes() > MAX_BYTES)
      {
         return (ObjectInputFilter.Status.REJECTED);
      }

      Class<?> clazz = info.serialClass();
      if(clazz == null)
      {
         return (ObjectInputFilter.Status.UNDECIDED);
      }
      while(clazz.isArray())
      {
         clazz = clazz.getComponentType();
      }
      if(clazz.isPrimitive())
      {
         return (ObjectInputFilter.Status.ALLOWED);
      }

      String name = clazz.getName();
      for(String prefix : allowedPrefixes)
      {
         if(name.startsWith(prefix))
         {
            return (ObjectInputFilter.Status.ALLOWED);
         }
      }
      return (ObjectInputFilter.Status.REJECTED);
   }
}
