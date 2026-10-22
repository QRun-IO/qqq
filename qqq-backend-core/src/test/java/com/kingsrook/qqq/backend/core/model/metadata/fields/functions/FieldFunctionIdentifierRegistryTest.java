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

package com.kingsrook.qqq.backend.core.model.metadata.fields.functions;


import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;


/*******************************************************************************
 ** Unit test for FieldFunctionIdentifierRegistry
 *******************************************************************************/
class FieldFunctionIdentifierRegistryTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      FieldFunctionIdentifierRegistry.getInstance().unregister("CustomTestFunction");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSingletonInstance()
   {
      FieldFunctionIdentifierRegistry instance1 = FieldFunctionIdentifierRegistry.getInstance();
      FieldFunctionIdentifierRegistry instance2 = FieldFunctionIdentifierRegistry.getInstance();
      assertNotNull(instance1);
      assertSame(instance1, instance2);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAutoDiscoveryFindsBuiltInFunctions()
   {
      FieldFunctionIdentifierRegistry registry = FieldFunctionIdentifierRegistry.getInstance();

      FieldFunctionTypeIdentifier stringLength = registry.getFieldFunctionTypeIdentifier("StringLength");
      assertNotNull(stringLength);
      assertEquals("StringLength", stringLength.getName());

      FieldFunctionTypeIdentifier subString = registry.getFieldFunctionTypeIdentifier("SubString");
      assertNotNull(subString);
      assertEquals("SubString", subString.getName());

      FieldFunctionTypeIdentifier weekdayOfDate = registry.getFieldFunctionTypeIdentifier("WeekdayOfDate");
      assertNotNull(weekdayOfDate);

      FieldFunctionTypeIdentifier weekdayOfDateTime = registry.getFieldFunctionTypeIdentifier("WeekdayOfDateTime");
      assertNotNull(weekdayOfDateTime);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLookupUnknownReturnsNull()
   {
      FieldFunctionIdentifierRegistry registry = FieldFunctionIdentifierRegistry.getInstance();
      assertNull(registry.getFieldFunctionTypeIdentifier("BogusNonExistentFunction"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRegisterCustomIdentifier()
   {
      FieldFunctionIdentifierRegistry registry = FieldFunctionIdentifierRegistry.getInstance();
      FieldFunctionTypeIdentifier custom = () -> "CustomTestFunction";

      registry.register(custom);

      FieldFunctionTypeIdentifier result = registry.getFieldFunctionTypeIdentifier("CustomTestFunction");
      assertNotNull(result);
      assertEquals("CustomTestFunction", result.getName());
   }

}
