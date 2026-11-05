/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils;


import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Small adversarial data fixtures prove bounded immutable intent semantics.
 *******************************************************************************/
class BoundedIntentDataTest
{
   /*******************************************************************************
    ** Nested collections are copied and immutable after the caller changes them.
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void copiesNestedParameters() throws Exception
   {
      ArrayList<Serializable> values = new ArrayList<>(List.of("before"));
      Map<String, Serializable> input = new LinkedHashMap<>();
      input.put("items", values);
      Map<String, Serializable> copied = BoundedIntentData.copyParameters(input);
      values.set(0, "after");
      input.clear();
      assertEquals(List.of("before"), copied.get("items"));
      assertThrows(UnsupportedOperationException.class, () -> copied.put("new", "value"));
      assertThrows(UnsupportedOperationException.class, () -> ((List<Serializable>) copied.get("items")).add("value"));
   }



   /*******************************************************************************
    ** Canonical keys and decimal values are stable while native types stay distinct.
    *******************************************************************************/
   @Test
   void canonicalFingerprintPreservesTypes() throws Exception
   {
      Map<String, Serializable> first = new LinkedHashMap<>();
      first.put("b", 2);
      first.put("a", new BigDecimal("1.00"));
      assertEquals(BoundedIntentData.fingerprint(first), BoundedIntentData.fingerprint(Map.of("a", new BigDecimal("1.0"), "b", 2)));
      assertNotEquals(BoundedIntentData.fingerprint(1), BoundedIntentData.fingerprint(1L));
      assertNotEquals(BoundedIntentData.fingerprint(null), BoundedIntentData.fingerprint("null"));
      assertNotEquals(BoundedIntentData.fingerprint(Instant.EPOCH), BoundedIntentData.fingerprint(Instant.EPOCH.toString()));
      assertEquals(BoundedIntentData.fingerprint(new byte[] { 1, 2 }), BoundedIntentData.fingerprint(new byte[] { 1, 2 }));
      assertNotEquals(BoundedIntentData.fingerprint(new byte[] { 1, 2 }), BoundedIntentData.fingerprint(new byte[] { 1, 3 }));
   }



   /*******************************************************************************
    ** Cycles, unsupported objects, nonfinite numbers and oversized text fail safely.
    *******************************************************************************/
   @Test
   void rejectsUnsafeOrExcessiveValues()
   {
      Map<String, Serializable> cycle = new LinkedHashMap<>();
      cycle.put("self", (Serializable) cycle);
      assertThrows(QException.class, () -> BoundedIntentData.copyParameters(cycle));
      assertThrows(QException.class, () -> BoundedIntentData.copyParameters(Map.of("number", Double.NaN)));
      assertThrows(QException.class, () -> BoundedIntentData.copyParameters(Map.of("text", "x".repeat(65537))));
      assertThrows(QException.class, () -> BoundedIntentData.fingerprint(new Object()));
      assertThrows(QException.class, () -> BoundedIntentData.copyParameters(Map.of("binary", new byte[] { 1 })));
      assertThrows(QException.class, () -> BoundedIntentData.copyParameters(Map.of("number", new BigDecimal("1E+1000000"))));
   }
}
