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

package com.kingsrook.qqq.backend.core.crypto;


import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import com.google.crypto.tink.Aead;
import com.google.crypto.tink.BinaryKeysetReader;
import com.google.crypto.tink.BinaryKeysetWriter;
import com.google.crypto.tink.KeyTemplates;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.RegistryConfiguration;
import com.google.crypto.tink.StreamingAead;
import com.google.crypto.tink.streamingaead.StreamingAeadConfig;
import com.google.crypto.tink.subtle.AesGcmJce;
import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Streaming authenticated encryption of large artifacts (e.g., backup files),
 ** built on Google Tink's StreamingAead (AES256_GCM_HKDF_1MB), which
 ** authenticates every segment and detects truncation.
 **
 ** Each artifact gets a FRESH data key.  That data key is wrapped (AES-256-GCM)
 ** by the key-encryption key (KEK) from a QKeyProviderInterface, and the wrapped
 ** data key, the KEK's id and its version are written in a header at the start
 ** of the stream:
 **
 ** <pre>
 **   "QSC1" | UTF keyId | UTF keyVersion | int wrappedLength | wrappedDataKey | tink stream...
 ** </pre>
 **
 ** The whole header is also bound into the stream as associated data, so any
 ** change to it makes decryption fail.  Decrypting reads the key id and version
 ** from the header and asks the provider for exactly that version, so data
 ** written under an old key version stays readable after rotation.
 **
 ** Key material is never logged and never put in an exception message.
 *******************************************************************************/
public class StreamingArtifactCipher
{
   private static final byte[] MAGIC                  = "QSC1".getBytes(StandardCharsets.US_ASCII);
   private static final String KEY_TEMPLATE           = "AES256_GCM_HKDF_1MB";
   private static final int    MAX_WRAPPED_KEY_LENGTH = 4096;

   static
   {
      try
      {
         StreamingAeadConfig.register();
      }
      catch(GeneralSecurityException e)
      {
         throw (new ExceptionInInitializerError("Unable to register the streaming AEAD primitives"));
      }
   }

   private final QKeyProviderInterface keyProvider;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public StreamingArtifactCipher(QKeyProviderInterface keyProvider)
   {
      this.keyProvider = keyProvider;
   }



   /*******************************************************************************
    ** Wrap an output stream so that what is written to it is encrypted (under a
    ** new data key, wrapped by the CURRENT version of keyRef) into the
    ** destination.  The header is written immediately.  The returned stream MUST
    ** be closed to finish the encryption (an unclosed stream is a truncated one,
    ** and will fail to decrypt); closing it also closes the destination.
    *******************************************************************************/
   public OutputStream newEncryptingStream(String keyRef, OutputStream destination) throws QException
   {
      QKeyMaterial kek = keyProvider.getKey(keyRef);
      try
      {
         KeysetHandle dataKey = KeysetHandle.generateNew(KeyTemplates.get(KEY_TEMPLATE));

         ByteArrayOutputStream wrapped = new ByteArrayOutputStream();
         dataKey.write(BinaryKeysetWriter.withOutputStream(wrapped), aeadFor(kek));

         byte[] header = buildHeader(kek.getKeyId(), kek.getVersion(), wrapped.toByteArray());
         StreamingAead streamingAead = dataKey.getPrimitive(RegistryConfiguration.get(), StreamingAead.class);

         destination.write(header);
         return (streamingAead.newEncryptingStream(destination, header));
      }
      catch(GeneralSecurityException | IOException e)
      {
         throw (new QException("Error starting encryption of stream for key [" + kek.getKeyId() + "] version [" + kek.getVersion() + "]", e));
      }
   }



   /*******************************************************************************
    ** Wrap an input stream of encrypted data (as written by newEncryptingStream)
    ** so that reading from it yields the plaintext.  Reads the header now, and
    ** gets the named key version from the provider.  Throws QException if the
    ** header is malformed, the key is unavailable, or the data key cannot be
    ** unwrapped (wrong key).  Reading the returned stream throws IOException if
    ** the content has been altered or truncated.
    *******************************************************************************/
   public InputStream newDecryptingStream(InputStream source) throws QException
   {
      try
      {
         DataInputStream in = new DataInputStream(source);

         byte[] magic = new byte[MAGIC.length];
         in.readFully(magic);
         if(!Arrays.equals(magic, MAGIC))
         {
            throw (new QException("Stream is not in the expected encrypted format"));
         }

         String keyId   = in.readUTF();
         String version = in.readUTF();
         int    length  = in.readInt();
         if(length <= 0 || length > MAX_WRAPPED_KEY_LENGTH)
         {
            throw (new QException("Encrypted stream header is malformed"));
         }

         byte[] wrapped = new byte[length];
         in.readFully(wrapped);
         byte[] header = buildHeader(keyId, version, wrapped);

         QKeyMaterial kek = keyProvider.getKey(keyId, version);

         KeysetHandle dataKey;
         try
         {
            dataKey = KeysetHandle.read(BinaryKeysetReader.withBytes(wrapped), aeadFor(kek));
         }
         catch(GeneralSecurityException e)
         {
            throw (new QException("Unable to unwrap the data key for key [" + keyId + "] version [" + version + "] (wrong key, or corrupted header)"));
         }

         StreamingAead streamingAead = dataKey.getPrimitive(RegistryConfiguration.get(), StreamingAead.class);
         return (new SanitizingInputStream(streamingAead.newDecryptingStream(in, header)));
      }
      catch(EOFException e)
      {
         throw (new QException("Encrypted stream is truncated (incomplete header)"));
      }
      catch(IOException | GeneralSecurityException e)
      {
         throw (new QException("Error starting decryption of stream", e));
      }
   }



   /*******************************************************************************
    ** Read just the key id and version named in the header of an encrypted
    ** stream (consumes the header from the stream).  Useful for finding which
    ** runs still need an old key version.
    *******************************************************************************/
   public static String[] readKeyIdAndVersion(InputStream source) throws QException
   {
      try
      {
         DataInputStream in    = new DataInputStream(source);
         byte[]          magic = new byte[MAGIC.length];
         in.readFully(magic);
         if(!Arrays.equals(magic, MAGIC))
         {
            throw (new QException("Stream is not in the expected encrypted format"));
         }
         return (new String[] { in.readUTF(), in.readUTF() });
      }
      catch(IOException e)
      {
         throw (new QException("Encrypted stream header is truncated or malformed"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static byte[] buildHeader(String keyId, String version, byte[] wrappedDataKey) throws IOException
   {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      DataOutputStream      out   = new DataOutputStream(bytes);
      out.write(MAGIC);
      out.writeUTF(keyId);
      out.writeUTF(version);
      out.writeInt(wrappedDataKey.length);
      out.write(wrappedDataKey);
      out.flush();
      return (bytes.toByteArray());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Aead aeadFor(QKeyMaterial kek) throws GeneralSecurityException
   {
      return (new AesGcmJce(kek.getKeyBytes()));
   }



   /*******************************************************************************
    ** Re-throws failures from Tink with a fixed message, so no detail from the
    ** underlying library can leak into callers' logs.
    *******************************************************************************/
   private static class SanitizingInputStream extends FilterInputStream
   {

      /*******************************************************************************
       **
       *******************************************************************************/
      SanitizingInputStream(InputStream in)
      {
         super(in);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public int read() throws IOException
      {
         try
         {
            return (super.read());
         }
         catch(IOException e)
         {
            throw (sanitize(e));
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public int read(byte[] b, int off, int len) throws IOException
      {
         try
         {
            return (super.read(b, off, len));
         }
         catch(IOException e)
         {
            throw (sanitize(e));
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private static IOException sanitize(IOException e)
      {
         return (new IOException("Decryption failed: the data is corrupt, truncated, or was not encrypted with this key", e));
      }
   }

}
