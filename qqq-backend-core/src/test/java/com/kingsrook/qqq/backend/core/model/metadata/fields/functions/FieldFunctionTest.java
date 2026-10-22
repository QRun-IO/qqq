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


import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.StringLengthFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.SubStringFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for FieldFunction
 *******************************************************************************/
class FieldFunctionTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testClone()
   {
      FieldFunction original = new FieldFunction()
         .withFieldName("firstName")
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2, SubStringFunction.LENGTH_PARAM, 3));

      FieldFunction clone = original.clone();
      assertNotSame(original, clone);
      assertNotSame(original.getArguments(), clone.getArguments());
      assertEquals("firstName", clone.getFieldName());
      assertEquals("SubString", clone.getFunctionTypeIdentifier().getName());
      assertEquals(2, clone.getArguments().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCloneWithNullArguments()
   {
      FieldFunction original = new FieldFunction()
         .withFieldName("test")
         .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER);

      FieldFunction clone = original.clone();
      assertNotSame(original, clone);
      assertEquals("test", clone.getFieldName());
      assertNull(clone.getArguments());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetArgumentValueOrDefaultWithExplicitArg()
   {
      FieldFunction ff = new FieldFunction()
         .withFieldName("name")
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 5));

      Integer fromIndex = ff.getArgumentValueOrDefault(Integer.class, SubStringFunction.FROM_INDEX_PARAM);
      assertEquals(5, fromIndex);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetArgumentValueOrDefaultFallsBackToDefault()
   {
      FieldFunction ff = new FieldFunction()
         .withFieldName("name")
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 1));

      /////////////////////////////////////////////////////////////////////////////////
      // length param is not in arguments, and is not required - should get null     //
      // (SubStringFunction.LENGTH_PARAM has no default value defined in parameters) //
      /////////////////////////////////////////////////////////////////////////////////
      Integer length = ff.getArgumentValueOrDefault(Integer.class, SubStringFunction.LENGTH_PARAM);
      assertNull(length);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetFunctionTypeIdentifierName()
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER);
      assertEquals("StringLength", ff.getFunctionTypeIdentifierName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetFunctionTypeIdentifierNameWhenNull()
   {
      FieldFunction ff = new FieldFunction();
      assertNull(ff.getFunctionTypeIdentifierName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testFluentSetters()
   {
      FieldFunction ff = new FieldFunction()
         .withFieldName("test")
         .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
         .withArguments(Map.of("key", "value"));

      assertEquals("test", ff.getFieldName());
      assertNotNull(ff.getFunctionTypeIdentifier());
      assertNotNull(ff.getArguments());
   }

}
