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

package com.kingsrook.qqq.backend.core.actions.permissions;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.ClassifiedValueKeySource;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.ClassifiedValueKeySourceMetaData;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.ClassifiedValueTokens;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Copy these UTF-8 HMAC-SHA256 vectors to other token consumers.
 *******************************************************************************/
class ClassifiedValueTokensTest extends BaseTest
{
   private static final String KEY = "AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=";
   private final QTableMetaData table = new QTableMetaData().withName("patient");
   private final QFieldMetaData field = new QFieldMetaData("systolic", QFieldType.DECIMAL);

   /*******************************************************************************
    ** Clear key.
    *******************************************************************************/
   @AfterEach
   void clearKey()
   {
      System.clearProperty("QQQ_AUDIT_HMAC_KEY");
      System.clearProperty("QQQ_AUDIT_HMAC_KEY_ID");
   }



   /*******************************************************************************
    ** Fixed vectors and verification.
    *******************************************************************************/
   @Test
   void fixedVectorsAndVerification()
   {
      System.setProperty("QQQ_AUDIT_HMAC_KEY", KEY);
      String recordToken = "hmac:v1:env1:YJ7YulFxAOci7Cm0pX0fOAZwmcEVTzBXaAQrmFD2fk8";
      assertEquals(recordToken, ClassifiedValueTokens.forRecordField(table, field, 42, new BigDecimal("120.00")));
      assertTrue(ClassifiedValueTokens.verifyRecordField(table, field, 42, 120, recordToken));
      assertFalse(ClassifiedValueTokens.verifyRecordField(table, field, 43, 120, recordToken));
      assertFalse(ClassifiedValueTokens.verifyRecordField(table, field, 42, 121, recordToken));
      assertFalse(ClassifiedValueTokens.verifyRecordField(table, field, 42, null, recordToken));
      assertFalse(ClassifiedValueTokens.verifyRecordField(table, field, 42, 120, recordToken.replace("env1", "old")));
      assertEquals("hmac:v1:env1:y5go1-Gynl0OjSDFoDaUiXNTWm6ci_3IHOn7HBxuc4k", ClassifiedValueTokens.forRecordField(table, field, 42, Instant.parse("2026-10-13T12:34:56Z")));
      List<String> parts = List.of("email", "welcome", "recipient", "message-7");
      String contextToken = "hmac:v1:env1:p1BggBbb8ytFgS5YeM6eBiriGgC1j3x0YGjics5qVeM";
      assertEquals(contextToken, ClassifiedValueTokens.forContext(parts, "Ada@example.com"));
      assertTrue(ClassifiedValueTokens.verifyContext(parts, "Ada@example.com", contextToken));
      assertFalse(ClassifiedValueTokens.verifyContext(List.of("webhook", "welcome", "recipient", "message-7"), "Ada@example.com", contextToken));
      assertFalse(ClassifiedValueTokens.verifyContext(parts, "Other", contextToken));
      assertFalse(ClassifiedValueTokens.verifyContext(parts, "Other", "changed:v1"));
      assertFalse(ClassifiedValueTokens.verifyContext(parts, "Other", "hmac:v2:env1:bogus"));
      System.setProperty("QQQ_AUDIT_HMAC_KEY_ID", "rotation2");
      assertEquals(recordToken.replace("env1", "rotation2"), ClassifiedValueTokens.forRecordField(table, field, 42, 120));
   }



   /*******************************************************************************
    ** Canonical forms and null.
    *******************************************************************************/
   @Test
   void canonicalFormsAndNull()
   {
      assertEquals("120", ClassifiedValueTokens.canonical(new BigDecimal("120.000")));
      assertEquals("100000000000000000000", ClassifiedValueTokens.canonical(new BigDecimal("1E+20")));
      assertEquals("0", ClassifiedValueTokens.canonical(new BigDecimal("0.000")));
      assertEquals("2026-10-13T12:34:56Z", ClassifiedValueTokens.canonical(Instant.parse("2026-10-13T12:34:56Z")));
      assertEquals("2026-10-13", ClassifiedValueTokens.canonical(LocalDate.of(2026, 10, 13)));
      assertEquals("true", ClassifiedValueTokens.canonical(true));
      assertEquals("false", ClassifiedValueTokens.canonical(false));
      assertEquals("text", ClassifiedValueTokens.canonical("text"));
      assertNull(ClassifiedValueTokens.canonical(null));
      assertNull(ClassifiedValueTokens.forRecordField(table, field, 42, null));
      assertNull(ClassifiedValueTokens.forContext(List.of("email"), null));
   }



   /*******************************************************************************
    ** Domain required.
    *******************************************************************************/
   @Test
   void domainRequired()
   {
      assertThrows(IllegalArgumentException.class, () -> ClassifiedValueTokens.forContext(null, "x"));
      assertThrows(IllegalArgumentException.class, () -> ClassifiedValueTokens.forContext(List.of(), "x"));
      assertThrows(IllegalArgumentException.class, () -> ClassifiedValueTokens.forContext(List.of(""), "x"));
      assertThrows(IllegalArgumentException.class, () -> ClassifiedValueTokens.verifyContext(Arrays.asList((String) null), "x", "changed:v1"));
      assertThrows(IllegalArgumentException.class, () -> ClassifiedValueTokens.forContext(List.of(" "), null));
   }



   /*******************************************************************************
    ** No key never verifies changed.
    *******************************************************************************/
   @Test
   void noKeyNeverVerifiesChanged()
   {
      System.setProperty("QQQ_AUDIT_HMAC_KEY", "");
      assertEquals("changed:v1", ClassifiedValueTokens.forRecordField(table, field, 42, 120));
      assertEquals("changed:v1", ClassifiedValueTokens.forContext(List.of("email"), "x"));
      assertFalse(ClassifiedValueTokens.verifyRecordField(table, field, 42, 120, "changed:v1"));
      assertFalse(ClassifiedValueTokens.verifyContext(List.of("email"), "x", "changed:v1"));
   }



   /*******************************************************************************
    ** Shred verification never creates keys.
    *******************************************************************************/
   @Test
   void shredVerificationNeverCreatesKeys()
   {
      AtomicBoolean destroyed = new AtomicBoolean();
      ClassifiedValueKeySourceMetaData.ofOrWithNew(QContext.getQInstance()).withKeySourceInstance(new ClassifiedValueKeySource()
      {
         @Override
         public Optional<KeyAndId> instanceKey()
         {
            return Optional.of(new KeyAndId(Base64.getDecoder().decode(KEY), "env1"));
         }

         @Override
         public Optional<KeyAndId> keyForRecordField(String tableName, String fieldName, Serializable pk, boolean create)
         {
            assertEquals("patient", tableName);
            assertEquals("systolic", fieldName);
            assertEquals(42, pk);
            if(destroyed.get())
            {
               assertFalse(create, "Verification must not recreate a shredded key");
               return Optional.empty();
            }
            return Optional.of(new KeyAndId(Base64.getDecoder().decode(KEY), "dek42"));
         }
      });
      String token = ClassifiedValueTokens.forRecordField(table, field, 42, 120);
      assertEquals("shred:v1:dek42:YJ7YulFxAOci7Cm0pX0fOAZwmcEVTzBXaAQrmFD2fk8", token);
      assertTrue(ClassifiedValueTokens.verifyRecordField(table, field, 42, 120, token));
      destroyed.set(true);
      for(int candidate = 60; candidate <= 250; candidate++)
      {
         assertFalse(ClassifiedValueTokens.verifyRecordField(table, field, 42, candidate, token));
      }
   }
}
