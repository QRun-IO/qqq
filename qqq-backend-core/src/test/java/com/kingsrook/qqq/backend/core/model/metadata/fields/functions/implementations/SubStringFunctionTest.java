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


import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Unit tests for {@link SubStringFunction}
 *******************************************************************************/
class SubStringFunctionTest extends BaseTest
{
   private final SubStringFunction function = new SubStringFunction();



   /***************************************************************************
    ** 1-based: SubString("Hello", 1) should return the full string
    ***************************************************************************/
   @Test
   void testFromIndex1() throws Exception
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("value")
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 1));

      QRecord record = new QRecord().withValue("value", "Hello");
      assertEquals("Hello", function.apply(ff, record));
   }



   /***************************************************************************
    ** 1-based: SubString("Hello", 2) -> "ello"
    ***************************************************************************/
   @Test
   void testFromIndexOnly() throws Exception
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("value")
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2));

      QRecord record = new QRecord().withValue("value", "Hello");
      assertEquals("ello", function.apply(ff, record));
   }



   /***************************************************************************
    ** SubString("Hello", 2, 3) -> "ell"
    ***************************************************************************/
   @Test
   void testFromIndexWithLength() throws Exception
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("value")
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 2, SubStringFunction.LENGTH_PARAM, 3));

      QRecord record = new QRecord().withValue("value", "Hello");
      assertEquals("ell", function.apply(ff, record));
   }



   /***************************************************************************
    ** fromIndex beyond string length -> ""
    ***************************************************************************/
   @Test
   void testFromIndexBeyondLength() throws Exception
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("value")
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 5));

      QRecord record = new QRecord().withValue("value", "Hi");
      assertEquals("", function.apply(ff, record));
   }



   /***************************************************************************
    ** SubString("Hello", 4, 10) -> "lo" (length clamped to string end)
    ***************************************************************************/
   @Test
   void testFromIndexWithLengthBeyondEnd() throws Exception
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("value")
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 4, SubStringFunction.LENGTH_PARAM, 10));

      QRecord record = new QRecord().withValue("value", "Hello");
      assertEquals("lo", function.apply(ff, record));
   }



   /***************************************************************************
    ** null source value -> null
    ***************************************************************************/
   @Test
   void testNullSource() throws Exception
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("value")
         .withArguments(Map.of(SubStringFunction.FROM_INDEX_PARAM, 1));

      QRecord record = new QRecord().withValue("value", null);
      assertNull(function.apply(ff, record));
   }



   /***************************************************************************
    ** missing fromIndex argument -> QException
    ***************************************************************************/
   @Test
   void testMissingFromIndex()
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(SubStringFunction.IDENTIFIER)
         .withFieldName("value")
         .withArguments(Map.of());

      QRecord record = new QRecord().withValue("value", "Hello");
      assertThrows(QException.class, () -> function.apply(ff, record));
   }

}
