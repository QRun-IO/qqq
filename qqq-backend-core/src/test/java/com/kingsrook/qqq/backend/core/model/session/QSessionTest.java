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

package com.kingsrook.qqq.backend.core.model.session;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for QSession
 *******************************************************************************/
class QSessionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSecurityKeys()
   {
      QSession session = new QSession().withSecurityKeyValues(Map.of(
         "clientId", List.of(42, 47),
         "warehouseId", List.of(1701)
      ));
      assertEquals(List.of(42, 47), session.getSecurityKeyValues("clientId"));
      assertEquals(List.of(1701), session.getSecurityKeyValues("warehouseId"));
      assertEquals(List.of(), session.getSecurityKeyValues("tenantId"));

      session.withSecurityKeyValue("clientId", 256);
      session.withSecurityKeyValue("clientId", 512);
      for(int i : List.of(42, 47, 256, 512))
      {
         assertTrue(session.hasSecurityKeyValue("clientId", i), "Should contain: " + i);
         assertTrue(session.hasSecurityKeyValue("clientId", String.valueOf(i), QFieldType.INTEGER), "Should contain: " + i);
      }

      session.clearSecurityKeyValues();
      for(int i : List.of(42, 47, 256, 512))
      {
         assertFalse(session.hasSecurityKeyValue("clientId", i), "Should no longer contain: " + i);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMixedValueTypes()
   {
      QSession session = new QSession().withSecurityKeyValues(Map.of(
         "storeId", List.of("100", "200", 300, "four-hundred")
      ));

      for(int i : List.of(100, 200, 300))
      {
         assertTrue(session.hasSecurityKeyValue("storeId", i, QFieldType.INTEGER), "Should contain: " + i);
         assertTrue(session.hasSecurityKeyValue("storeId", String.valueOf(i), QFieldType.INTEGER), "Should contain: " + i);
         assertTrue(session.hasSecurityKeyValue("storeId", i, QFieldType.STRING), "Should contain: " + i);
         assertTrue(session.hasSecurityKeyValue("storeId", String.valueOf(i), QFieldType.STRING), "Should contain: " + i);
      }

      ////////////////////////////////////////////////////////////////////////////
      // next two blocks - used to throw exceptions - now, gracefully be false. //
      ////////////////////////////////////////////////////////////////////////////
      int i = 400;
      assertFalse(session.hasSecurityKeyValue("storeId", i, QFieldType.INTEGER), "Should not contain: " + i);
      assertFalse(session.hasSecurityKeyValue("storeId", String.valueOf(i), QFieldType.INTEGER), "Should not contain: " + i);
      assertFalse(session.hasSecurityKeyValue("storeId", i, QFieldType.STRING), "Should not contain: " + i);
      assertFalse(session.hasSecurityKeyValue("storeId", String.valueOf(i), QFieldType.STRING), "Should not contain: " + i);

      assertFalse(session.hasSecurityKeyValue("storeId", "one-hundred", QFieldType.INTEGER), "Should not contain: " + i);
      assertFalse(session.hasSecurityKeyValue("storeId", "one-hundred", QFieldType.STRING), "Should not contain: " + i);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNullSafety()
   {
      QSession session = new QSession();
      assertFalse(session.hasSecurityKeyValue("any", 1));
      assertFalse(session.hasSecurityKeyValue("other", 1));
      assertFalse(session.hasSecurityKeyValue("other", 1, QFieldType.STRING));
      assertEquals(List.of(), session.getSecurityKeyValues("any"));

      session.withSecurityKeyValue("any", 1);
      assertTrue(session.hasSecurityKeyValue("any", 1));
      assertTrue(session.hasSecurityKeyValue("any", 1, QFieldType.STRING));
      assertFalse(session.hasSecurityKeyValue("any", 2));
      assertFalse(session.hasSecurityKeyValue("other", 1));
      assertFalse(session.hasSecurityKeyValue("other", 1, QFieldType.STRING));
      assertEquals(List.of(), session.getSecurityKeyValues("other"));
   }

}