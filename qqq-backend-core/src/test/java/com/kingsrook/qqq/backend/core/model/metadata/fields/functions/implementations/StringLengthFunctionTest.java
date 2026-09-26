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

package com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for {@link StringLengthFunction}
 *******************************************************************************/
class StringLengthFunctionTest extends BaseTest
{
   private final StringLengthFunction function = new StringLengthFunction();



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testBasicLength()
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
         .withFieldName("value");

      assertEquals(5, function.apply(ff, new QRecord().withValue("value", "Hello")));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testEmptyString()
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
         .withFieldName("value");

      assertEquals(0, function.apply(ff, new QRecord().withValue("value", "")));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testNullSource()
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
         .withFieldName("value");

      assertNull(function.apply(ff, new QRecord().withValue("value", null)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testLongerString()
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(StringLengthFunction.IDENTIFIER)
         .withFieldName("value");

      assertEquals(11, function.apply(ff, new QRecord().withValue("value", "Hello World")));
   }

}
