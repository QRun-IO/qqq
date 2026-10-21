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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit test for the key providers and StreamingArtifactCipher.
 *******************************************************************************/
class StreamingArtifactCipherTest
{
   private static final SecureRandom RANDOM = new SecureRandom();



   /*******************************************************************************
    ** Multi-megabyte stream (several 1MB segments) round trips.
    *******************************************************************************/
   @Test
   void testRoundTripMultiMegabyte() throws Exception
   {
      Map<String, String> env       = Map.of("BK_KEY", newKey());
      byte[]              plaintext = randomBytes(5 * 1024 * 1024 + 123);

      byte[] encrypted = encrypt(cipher(env), "BK_KEY", plaintext);
      assertThat(encrypted.length).isGreaterThan(plaintext.length);

      assertArrayEquals(plaintext, decrypt(cipher(env), encrypted));
   }



   /*******************************************************************************
    ** Empty and tiny streams round trip, and each artifact gets a fresh data key
    ** (so ciphertexts of the same plaintext differ).
    *******************************************************************************/
   @Test
   void testEmptyAndFreshDataKeys() throws Exception
   {
      Map<String, String>     env    = Map.of("BK_KEY", newKey());
      StreamingArtifactCipher cipher = cipher(env);

      assertArrayEquals(new byte[0], decrypt(cipher, encrypt(cipher, "BK_KEY", new byte[0])));

      byte[] plaintext = "hello".getBytes();
      byte[] one       = encrypt(cipher, "BK_KEY", plaintext);
      byte[] two       = encrypt(cipher, "BK_KEY", plaintext);
      assertThat(one).isNotEqualTo(two);
      assertArrayEquals(plaintext, decrypt(cipher, one));
   }



   /*******************************************************************************
    ** The header carries the key id and version.
    *******************************************************************************/
   @Test
   void testHeaderNamesKeyIdAndVersion() throws Exception
   {
      Map<String, String> env       = Map.of("BK_KEY", newKey(), "BK_KEY_VERSION", "7");
      byte[]              encrypted = encrypt(cipher(env), "BK_KEY", new byte[] { 1, 2, 3 });

      String[] idAndVersion = StreamingArtifactCipher.readKeyIdAndVersion(new ByteArrayInputStream(encrypted));
      assertEquals("BK_KEY", idAndVersion[0]);
      assertEquals("7", idAndVersion[1]);
   }



   /*******************************************************************************
    ** Chopping bytes off the end (at several points) is detected.
    *******************************************************************************/
   @Test
   void testTruncationDetected() throws Exception
   {
      Map<String, String>     env       = Map.of("BK_KEY", newKey());
      StreamingArtifactCipher cipher    = cipher(env);
      byte[]                  plaintext = randomBytes(3 * 1024 * 1024);
      byte[]                  encrypted = encrypt(cipher, "BK_KEY", plaintext);

      for(int chop : new int[] { 1, 100, 1024 * 1024, 2 * 1024 * 1024 })
      {
         byte[] truncated = Arrays.copyOf(encrypted, encrypted.length - chop);
         assertThrows(IOException.class, () -> decrypt(cipher, truncated), "chop " + chop);
      }

      ///////////////////////////////////////////////
      // truncated inside the header: a QException //
      ///////////////////////////////////////////////
      assertThrows(QException.class, () -> decrypt(cipher, Arrays.copyOf(encrypted, 10)));
   }



   /*******************************************************************************
    ** Flipping a byte in the body, or in the header, is detected.
    *******************************************************************************/
   @Test
   void testTamperDetected() throws Exception
   {
      Map<String, String>     env       = Map.of("BK_KEY", newKey());
      StreamingArtifactCipher cipher    = cipher(env);
      byte[]                  encrypted = encrypt(cipher, "BK_KEY", randomBytes(2 * 1024 * 1024));

      for(int position : new int[] { encrypted.length - 5, encrypted.length / 2, 1024 })
      {
         byte[] tampered = encrypted.clone();
         tampered[position] ^= 0x01;
         assertThrows(IOException.class, () -> decrypt(cipher, tampered), "body position " + position);
      }

      //////////////////////////////////////////////////////////////////////////////////////
      // header bytes (wrapped data key): fails either at unwrap or when reading the body //
      //////////////////////////////////////////////////////////////////////////////////////
      byte[] headerTampered = encrypted.clone();
      headerTampered[20] ^= 0x01;
      assertThrows(Exception.class, () -> decrypt(cipher, headerTampered));
   }



   /*******************************************************************************
    ** A different key cannot decrypt.
    *******************************************************************************/
   @Test
   void testWrongKeyFails() throws Exception
   {
      byte[] encrypted = encrypt(cipher(Map.of("BK_KEY", newKey())), "BK_KEY", randomBytes(1024));

      StreamingArtifactCipher other = cipher(Map.of("BK_KEY", newKey()));
      assertThrows(QException.class, () -> decrypt(other, encrypted));
   }



   /*******************************************************************************
    ** After rotation, new writes use the new version, and data written under the
    ** old version still decrypts (from NAME_V{old}).
    *******************************************************************************/
   @Test
   void testKeyVersionRotation() throws Exception
   {
      String keyV1  = newKey();
      String keyV2  = newKey();
      byte[] plain  = randomBytes(1_500_000);

      byte[] oldEnc = encrypt(cipher(Map.of("BK_KEY", keyV1)), "BK_KEY", plain);
      assertEquals("1", StreamingArtifactCipher.readKeyIdAndVersion(new ByteArrayInputStream(oldEnc))[1]);

      StreamingArtifactCipher rotated = cipher(Map.of("BK_KEY", keyV2, "BK_KEY_VERSION", "2", "BK_KEY_V1", keyV1));

      byte[] newEnc = encrypt(rotated, "BK_KEY", plain);
      assertEquals("2", StreamingArtifactCipher.readKeyIdAndVersion(new ByteArrayInputStream(newEnc))[1]);

      assertArrayEquals(plain, decrypt(rotated, oldEnc));
      assertArrayEquals(plain, decrypt(rotated, newEnc));

      //////////////////////////////////////////////////////////////
      // if the old version is no longer provided, old data fails //
      //////////////////////////////////////////////////////////////
      StreamingArtifactCipher noOld = cipher(Map.of("BK_KEY", keyV2, "BK_KEY_VERSION", "2"));
      assertThrows(QException.class, () -> decrypt(noOld, oldEnc));
      assertArrayEquals(plain, decrypt(noOld, newEnc));
   }



   /*******************************************************************************
    ** No exception message (or cause message) contains key material, and
    ** QKeyMaterial.toString redacts it.
    *******************************************************************************/
   @Test
   void testNoKeyMaterialInMessages() throws Exception
   {
      String              keyText   = newKey();
      String              otherText = newKey();
      Map<String, String> env       = Map.of("BK_KEY", keyText);
      byte[]              encrypted = encrypt(cipher(env), "BK_KEY", randomBytes(2 * 1024 * 1024));
      byte[]              tampered  = encrypted.clone();
      tampered[tampered.length - 5] ^= 0x01;

      Throwable[] failures = new Throwable[]
      {
         assertThrows(QException.class, () -> decrypt(cipher(Map.of("BK_KEY", otherText)), encrypted)),
         assertThrows(IOException.class, () -> decrypt(cipher(env), tampered)),
         assertThrows(QException.class, () -> cipher(Map.of("BK_KEY", "not base64 !!" + keyText)).newEncryptingStream("BK_KEY", new ByteArrayOutputStream())),
         assertThrows(QException.class, () -> cipher(Map.of("BK_KEY", Base64.getEncoder().encodeToString(new byte[5]))).newEncryptingStream("BK_KEY", new ByteArrayOutputStream())),
         assertThrows(QException.class, () -> cipher(Map.of()).newEncryptingStream("BK_KEY", new ByteArrayOutputStream())),
      };

      for(Throwable failure : failures)
      {
         for(Throwable t = failure; t != null; t = t.getCause())
         {
            String message = String.valueOf(t.getMessage());
            assertThat(message).doesNotContain(keyText).doesNotContain(otherText);
         }
      }

      QKeyMaterial material = new EnvKeyProvider(env::get).getKey("BK_KEY");
      assertThat(material.toString()).doesNotContain(keyText).contains("redacted");
   }



   /*******************************************************************************
    ** Provider lookups: current version, specific version, validation.
    *******************************************************************************/
   @Test
   void testEnvKeyProvider() throws QException
   {
      String         keyV1    = newKey();
      String         keyV3    = newKey();
      EnvKeyProvider provider = new EnvKeyProvider(Map.of("K", keyV3, "K_VERSION", "3", "K_V1", keyV1)::get);

      QKeyMaterial current = provider.getKey("K");
      assertEquals("K", current.getKeyId());
      assertEquals("3", current.getVersion());
      assertArrayEquals(Base64.getDecoder().decode(keyV3), current.getKeyBytes());

      assertArrayEquals(Base64.getDecoder().decode(keyV1), provider.getKey("K", "1").getKeyBytes());

      assertThrows(QException.class, () -> provider.getKey("K", "2"));
      assertThrows(QException.class, () -> provider.getKey("NOPE"));
      assertThrows(QException.class, () -> provider.getKey("bad key ref!"));
   }



   /*******************************************************************************
    ** The AWS provider does (path, name) lookups like SecretsManagerUtils, and
    ** shares the version convention.
    *******************************************************************************/
   @Test
   void testAwsSecretsManagerKeyProvider() throws QException
   {
      String              keyV1   = newKey();
      String              keyV2   = newKey();
      Map<String, String> secrets = Map.of("prod/backups/K", keyV2, "prod/backups/K_VERSION", "2", "prod/backups/K_V1", keyV1);

      AwsSecretsManagerKeyProvider provider = new AwsSecretsManagerKeyProvider("prod/backups/", (path, name) -> Optional.ofNullable(secrets.get(path + name)));

      assertEquals("2", provider.getKey("K").getVersion());
      assertArrayEquals(Base64.getDecoder().decode(keyV2), provider.getKey("K").getKeyBytes());
      assertArrayEquals(Base64.getDecoder().decode(keyV1), provider.getKey("K", "1").getKeyBytes());
      assertThrows(QException.class, () -> provider.getKey("MISSING"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static StreamingArtifactCipher cipher(Map<String, String> env)
   {
      return (new StreamingArtifactCipher(new EnvKeyProvider(env::get)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static byte[] encrypt(StreamingArtifactCipher cipher, String keyRef, byte[] plaintext) throws Exception
   {
      ByteArrayOutputStream sink = new ByteArrayOutputStream();
      try(OutputStream out = cipher.newEncryptingStream(keyRef, sink))
      {
         int offset = 0;
         while(offset < plaintext.length)
         {
            int n = Math.min(65_537, plaintext.length - offset);
            out.write(plaintext, offset, n);
            offset += n;
         }
      }
      return (sink.toByteArray());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static byte[] decrypt(StreamingArtifactCipher cipher, byte[] encrypted) throws Exception
   {
      try(InputStream in = cipher.newDecryptingStream(new ByteArrayInputStream(encrypted)))
      {
         return (in.readAllBytes());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String newKey()
   {
      return (Base64.getEncoder().encodeToString(randomBytes(32)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static byte[] randomBytes(int length)
   {
      byte[] bytes = new byte[length];
      RANDOM.nextBytes(bytes);
      return (bytes);
   }

}
