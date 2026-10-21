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
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for BackendFieldFunctionAdapterRegistry
 *******************************************************************************/
class BackendFieldFunctionAdapterRegistryTest extends BaseTest
{

   /****************************************************************************
    * A minimal test adapter to verify registration works.
    ****************************************************************************/
   public static class TestAdapter implements BackendFieldFunctionAdapterInterface
   {
   }



   /****************************************************************************
    * A second test adapter to verify replacement registration.
    ****************************************************************************/
   public static class ReplacementAdapter implements BackendFieldFunctionAdapterInterface
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRegisterAndGetAdapter()
   {
      BackendFieldFunctionAdapterRegistry registry = new BackendFieldFunctionAdapterRegistry();
      registry.register(StringLengthFunction.IDENTIFIER, "testBackend", new QCodeReference(TestAdapter.class));

      BackendFieldFunctionAdapterInterface adapter = registry.getFieldFunctionAdapter(StringLengthFunction.IDENTIFIER);
      assertNotNull(adapter);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetUnregisteredReturnsNull()
   {
      BackendFieldFunctionAdapterRegistry registry = new BackendFieldFunctionAdapterRegistry();
      FieldFunctionTypeIdentifier bogus = () -> "NoSuchFunction";

      assertNull(registry.getFieldFunctionAdapter(bogus));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRegisterReplacesPreviousRegistration()
   {
      BackendFieldFunctionAdapterRegistry registry = new BackendFieldFunctionAdapterRegistry();
      registry.register(StringLengthFunction.IDENTIFIER, "testBackend", new QCodeReference(TestAdapter.class));
      registry.register(StringLengthFunction.IDENTIFIER, "testBackend", new QCodeReference(ReplacementAdapter.class));

      BackendFieldFunctionAdapterInterface adapter = registry.getFieldFunctionAdapter(StringLengthFunction.IDENTIFIER);
      assertNotNull(adapter);
      assertInstanceOf(ReplacementAdapter.class, adapter, "Should return the replacement adapter, not the original");
   }

}
