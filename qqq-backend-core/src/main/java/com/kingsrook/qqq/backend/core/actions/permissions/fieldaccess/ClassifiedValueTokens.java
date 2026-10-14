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

package com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess;


import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess.ClassifiedValueKeySource.KeyAndId;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Shared, versioned audit/email value tokens. The MAC input is UTF-8.
 *******************************************************************************/
public final class ClassifiedValueTokens
{
   private static final AtomicBoolean warned = new AtomicBoolean();

   /*******************************************************************************
    ** Static utility.
    *******************************************************************************/
   private ClassifiedValueTokens()
   {
   }



   /*******************************************************************************
    ** Bind a value to the table, field and primary key, preferring a shreddable key.
    *******************************************************************************/
   public static String forRecordField(QTableMetaData table, QFieldMetaData field, Serializable pk, Serializable value)
   {
      if(value == null)
      {
         return null;
      }
      ClassifiedValueKeySource source = source();
      Optional<KeyAndId> key = source.keyForRecordField(table.getName(), field.getName(), pk, true);
      return key.isPresent() ? token("shred", key.get(), binding(table, field, pk), value)
         : instanceToken(source, binding(table, field, pk), value);
   }



   /*******************************************************************************
    ** Non-record bindings must start with a nonempty domain.
    *******************************************************************************/
   public static String forContext(List<String> bindingParts, Serializable value)
   {
      validateDomain(bindingParts);
      return value == null ? null : instanceToken(source(), bindingParts, value);
   }



   /*******************************************************************************
    ** No derivative of the original value.
    *******************************************************************************/
   public static String changed()
   {
      return "changed:v1";
   }



   /*******************************************************************************
    ** A key-destruction event, never a derivative of the value.
    *******************************************************************************/
   public static String shreddedToken(String ruleId)
   {
      if(ruleId == null || !ruleId.matches("[A-Za-z0-9._-]{1,180}"))
      {
         throw new IllegalArgumentException("Invalid shred rule identifier");
      }
      return "shredded:v1:" + ruleId;
   }



   /*******************************************************************************
    ** Verify only keyed tokens, using existing keys and constant-time MAC comparison.
    *******************************************************************************/
   public static boolean verifyRecordField(QTableMetaData table, QFieldMetaData field, Serializable pk, Serializable candidate, String token)
   {
      if(candidate == null || token == null)
      {
         return false;
      }
      ClassifiedValueKeySource source = source();
      if(token.startsWith("shred:v1:"))
      {
         return verify(source.keyForRecordField(table.getName(), field.getName(), pk, false), "shred", binding(table, field, pk), candidate, token);
      }
      return verify(source.instanceKey(), "hmac", binding(table, field, pk), candidate, token);
   }



   /*******************************************************************************
    ** Verify a non-record token without creating a key.
    *******************************************************************************/
   public static boolean verifyContext(List<String> bindingParts, Serializable candidate, String token)
   {
      validateDomain(bindingParts);
      return candidate != null && token != null && verify(source().instanceKey(), "hmac", bindingParts, candidate, token);
   }



   /*******************************************************************************
    ** Canonical ValueUtils strings, with scale-independent plain decimal notation.
    *******************************************************************************/
   public static String canonical(Serializable value)
   {
      return value instanceof BigDecimal decimal ? decimal.stripTrailingZeros().toPlainString() : ValueUtils.getValueAsString(value);
   }



   /*******************************************************************************
    ** Validate the context even for absent values.
    *******************************************************************************/
   private static void validateDomain(List<String> parts)
   {
      if(parts == null || parts.isEmpty() || parts.get(0) == null || parts.get(0).isBlank() || parts.stream().anyMatch(p -> p == null || p.indexOf('\u001F') >= 0))
      {
         throw new IllegalArgumentException("A nonempty domain and unambiguous binding parts are required");
      }
   }



   /*******************************************************************************
    ** The agreed record binding does not prepend an additional domain.
    *******************************************************************************/
   private static List<String> binding(QTableMetaData table, QFieldMetaData field, Serializable pk)
   {
      return List.of(table.getName(), field.getName(), String.valueOf(pk));
   }



   /*******************************************************************************
    ** Emit a keyed token or a value-free fallback.
    *******************************************************************************/
   private static String instanceToken(ClassifiedValueKeySource source, List<String> parts, Serializable value)
   {
      return source.instanceKey().map(key -> token("hmac", key, parts, value)).orElseGet(ClassifiedValueTokens::changed);
   }



   /*******************************************************************************
    ** Compare full tokens, including the key identifier and version.
    *******************************************************************************/
   private static boolean verify(Optional<KeyAndId> key, String kind, List<String> parts, Serializable value, String supplied)
   {
      return key.map(k -> MessageDigest.isEqual(token(kind, k, parts, value).getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8))).orElse(false);
   }



   /*******************************************************************************
    ** Encode the HMAC with URL-safe base64 and no padding.
    *******************************************************************************/
   private static String token(String kind, KeyAndId key, List<String> parts, Serializable value)
   {
      try
      {
         Mac mac = Mac.getInstance("HmacSHA256");
         mac.init(new SecretKeySpec(key.key(), "HmacSHA256"));
         byte[] digest = mac.doFinal((String.join("\u001F", parts) + "\u001F" + canonical(value)).getBytes(StandardCharsets.UTF_8));
         return kind + ":v1:" + key.id() + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
      }
      catch(GeneralSecurityException e)
      {
         throw new IllegalStateException("Unable to compute classified-value token", e);
      }
   }



   /*******************************************************************************
    ** Use instance metadata, falling back to environment/property configuration.
    *******************************************************************************/
   private static ClassifiedValueKeySource source()
   {
      ClassifiedValueKeySourceMetaData metadata = ClassifiedValueKeySourceMetaData.of(QContext.getQInstance());
      ClassifiedValueKeySource configured = metadata == null ? null : metadata.getKeySourceInstance();
      return configured == null ? new EnvironmentKeySource() : configured;
   }



   /*******************************************************************************
    ** Reads keys without the convenience property getter, which logs secret values.
    *******************************************************************************/
   private static class EnvironmentKeySource implements ClassifiedValueKeySource
   {
      @Override
      public Optional<KeyAndId> instanceKey()
      {
         QMetaDataVariableInterpreter interpreter = new QMetaDataVariableInterpreter();
         String encoded = interpreter.interpret("${prop.QQQ_AUDIT_HMAC_KEY}??${env.QQQ_AUDIT_HMAC_KEY}");
         if(encoded == null || encoded.isBlank())
         {
            if(warned.compareAndSet(false, true))
            {
               QLogger.getLogger(ClassifiedValueTokens.class).warn("No audit HMAC key configured; classified audit values use changed:v1");
            }
            return Optional.empty();
         }
         String id = interpreter.interpret("${prop.QQQ_AUDIT_HMAC_KEY_ID}??${env.QQQ_AUDIT_HMAC_KEY_ID}");
         try
         {
            return Optional.of(new KeyAndId(Base64.getDecoder().decode(encoded), id == null || id.isBlank() ? "env1" : id));
         }
         catch(IllegalArgumentException e)
         {
            throw new IllegalStateException("Invalid audit HMAC key configuration");
         }
      }

      @Override
      public Optional<KeyAndId> keyForRecordField(String table, String field, Serializable pk, boolean create)
      {
         return Optional.empty();
      }
   }
}
