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


import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for RefreshTokenCipher (#1048)
 *******************************************************************************/
class RefreshTokenCipherTest extends BaseTest
{
   private static final String KEY_A = newKey();
   private static final String KEY_B = newKey();
   private static final String AAD   = RefreshTokenCipher.buildAssociatedData("session-uuid-1", "user-1");



   /*******************************************************************************
    ** Values round-trip, in the documented format, with a fresh nonce each time.
    *******************************************************************************/
   @Test
   void testRoundTripAndFormat() throws QException
   {
      RefreshTokenCipher cipher = cipher(KEY_A, null, null);
      String             token  = "a-refresh-token-value";

      String first  = cipher.encrypt(token, AAD);
      String second = cipher.encrypt(token, AAD);
      assertThat(first).startsWith("v1:default:").doesNotContain(token);
      assertNotEquals(first, second);
      assertEquals(token, cipher.decrypt(first, AAD));
      assertEquals(token, cipher.decrypt(second, AAD));
      assertEquals(RefreshTokenCipher.DEFAULT_KEY_ID, cipher.getCurrentKeyId());

      ////////////////////////////////////////////////////////////
      // nonce (12) + ciphertext (same length as token) + tag (16) //
      ////////////////////////////////////////////////////////////
      byte[] payload = Base64.getDecoder().decode(first.split(":", 3)[2]);
      assertEquals(12 + token.length() + 16, payload.length);
   }



   /*******************************************************************************
    ** Wrong key, wrong associated data, tampering, unknown key id and malformed
    ** values all fail, with a token-free reason.
    *******************************************************************************/
   @Test
   void testDecryptFailures() throws QException
   {
      RefreshTokenCipher cipher = cipher(KEY_A, null, null);
      String             stored = cipher.encrypt("secret-token", AAD);

      assertReason("authenticationFailed", () -> cipher(KEY_B, null, null).decrypt(stored, AAD));
      assertReason("authenticationFailed", () -> cipher.decrypt(stored, RefreshTokenCipher.buildAssociatedData("session-uuid-2", "user-1")));
      assertReason("authenticationFailed", () -> cipher.decrypt(stored, RefreshTokenCipher.buildAssociatedData("session-uuid-1", "user-2")));

      String[] parts   = stored.split(":", 3);
      byte[]   payload = Base64.getDecoder().decode(parts[2]);
      payload[14] ^= 0x01;
      assertReason("authenticationFailed", () -> cipher.decrypt(parts[0] + ":" + parts[1] + ":" + Base64.getEncoder().encodeToString(payload), AAD));

      assertReason("unknownKeyId", () -> cipher.decrypt("v1:other:" + parts[2], AAD));
      assertReason("unrecognizedFormat", () -> cipher.decrypt("secret-token", AAD));
      assertReason("unrecognizedFormat", () -> cipher.decrypt("v2:default:" + parts[2], AAD));
      assertReason("malformedPayload", () -> cipher.decrypt("v1:default:%%%", AAD));
      assertReason("malformedPayload", () -> cipher.decrypt("v1:default:AAAA", AAD));
      assertReason("missingValue", () -> cipher.decrypt(null, AAD));
   }



   /*******************************************************************************
    ** Rotation: previous keys decrypt by key id; only the current key encrypts.
    *******************************************************************************/
   @Test
   void testRotation() throws QException
   {
      RefreshTokenCipher oldCipher = cipher(KEY_A, "k1", null);
      String             oldValue  = oldCipher.encrypt("rotating-token", AAD);
      assertFalse(oldCipher.needsReEncryption(oldValue));

      RefreshTokenCipher newCipher = cipher(KEY_B, "k2", Map.of("k1", KEY_A));
      assertEquals("rotating-token", newCipher.decrypt(oldValue, AAD));
      assertTrue(newCipher.needsReEncryption(oldValue));
      assertTrue(newCipher.needsReEncryption("plaintext"));
      assertTrue(newCipher.needsReEncryption(null));

      String newValue = newCipher.encrypt("rotating-token", AAD);
      assertThat(newValue).startsWith("v1:k2:");
      assertFalse(newCipher.needsReEncryption(newValue));
      assertReason("unknownKeyId", () -> oldCipher.decrypt(newValue, AAD));
   }



   /*******************************************************************************
    ** Key configuration: none -> null; invalid settings throw, never echoing keys.
    *******************************************************************************/
   @Test
   void testFromMetaData() throws QException
   {
      assertNull(RefreshTokenCipher.fromMetaData(null));
      assertNull(RefreshTokenCipher.fromMetaData(new OAuth2AuthenticationMetaData()));
      assertNull(RefreshTokenCipher.fromMetaData(new OAuth2AuthenticationMetaData().withRefreshTokenEncryptionKey("").withPreviousRefreshTokenEncryptionKeys(Map.of())));

      String shortKey = Base64.getEncoder().encodeToString(new byte[31]);
      assertConfigError("must decode to 32 bytes", new OAuth2AuthenticationMetaData().withRefreshTokenEncryptionKey(shortKey));
      assertConfigError("not valid base64", new OAuth2AuthenticationMetaData().withRefreshTokenEncryptionKey("@@not-base64@@"));
      assertConfigError("did not resolve", new OAuth2AuthenticationMetaData().withRefreshTokenEncryptionKey("${env.QQQ_UNSET_VARIABLE_FOR_TEST_1048}"));
      assertConfigError("refreshTokenEncryptionKeyId", new OAuth2AuthenticationMetaData().withRefreshTokenEncryptionKey(KEY_A).withRefreshTokenEncryptionKeyId(""));
      assertConfigError("key id", new OAuth2AuthenticationMetaData().withRefreshTokenEncryptionKey(KEY_A).withPreviousRefreshTokenEncryptionKeys(Map.of("bad id", KEY_B)));
      assertConfigError("may not reuse", new OAuth2AuthenticationMetaData().withRefreshTokenEncryptionKey(KEY_A).withRefreshTokenEncryptionKeyId("same").withPreviousRefreshTokenEncryptionKeys(Map.of("same", KEY_B)));
      assertConfigError("previousRefreshTokenEncryptionKeys is set", new OAuth2AuthenticationMetaData().withPreviousRefreshTokenEncryptionKeys(Map.of("old", KEY_B)));
   }



   /*******************************************************************************
    ** Associated data includes both session uuid and user id, and handles nulls.
    *******************************************************************************/
   @Test
   void testBuildAssociatedData()
   {
      assertNotEquals(RefreshTokenCipher.buildAssociatedData("s1", "u1"), RefreshTokenCipher.buildAssociatedData("s1", "u2"));
      assertNotEquals(RefreshTokenCipher.buildAssociatedData("s1", "u1"), RefreshTokenCipher.buildAssociatedData("s2", "u1"));
      assertEquals(RefreshTokenCipher.buildAssociatedData("s1", null), RefreshTokenCipher.buildAssociatedData("s1", ""));
      assertThat(RefreshTokenCipher.buildAssociatedData(null, null)).isNotNull();
   }



   /*******************************************************************************
    ** Build a cipher from key settings
    *******************************************************************************/
   private static RefreshTokenCipher cipher(String key, String keyId, Map<String, String> previousKeys) throws QException
   {
      return (RefreshTokenCipher.fromMetaData(new OAuth2AuthenticationMetaData()
         .withRefreshTokenEncryptionKey(key)
         .withRefreshTokenEncryptionKeyId(keyId)
         .withPreviousRefreshTokenEncryptionKeys(previousKeys)));
   }



   /*******************************************************************************
    ** Assert that a decrypt call fails with the given reason
    *******************************************************************************/
   private static void assertReason(String reason, ThrowingSupplier supplier)
   {
      QException exception = assertThrows(QException.class, supplier::get);
      assertEquals(reason, exception.getMessage());
   }



   /*******************************************************************************
    ** Assert that building a cipher fails with a message containing the text,
    ** and not containing either test key.
    *******************************************************************************/
   private static void assertConfigError(String expectedText, OAuth2AuthenticationMetaData metaData)
   {
      QException exception = assertThrows(QException.class, () -> RefreshTokenCipher.fromMetaData(metaData));
      assertThat(exception.getMessage()).contains(expectedText).doesNotContain(KEY_A).doesNotContain(KEY_B);
   }



   /*******************************************************************************
    ** Generate a random base64 32-byte key
    *******************************************************************************/
   private static String newKey()
   {
      byte[] bytes = new byte[32];
      new SecureRandom().nextBytes(bytes);
      return (Base64.getEncoder().encodeToString(bytes));
   }



   /*******************************************************************************
    ** Supplier that may throw a QException
    *******************************************************************************/
   @FunctionalInterface
   private interface ThrowingSupplier
   {
      /*******************************************************************************
       ** Get the value
       *******************************************************************************/
      String get() throws QException;
   }

}
