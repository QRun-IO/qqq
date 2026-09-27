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

package com.kingsrook.qqq.backend.core.adapters;


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.actions.shared.mapping.AbstractQFieldMapping;
import com.kingsrook.qqq.backend.core.model.actions.shared.mapping.QIndexBasedFieldMapping;
import com.kingsrook.qqq.backend.core.model.actions.shared.mapping.QKeyBasedFieldMapping;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for JsonToQFieldMappingAdapter
 **
 *******************************************************************************/
class JsonToQFieldMappingAdapterTest extends BaseTest
{
   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildMappingFromJson_nullInput()
   {
      testExpectedToThrow(null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildMappingFromJson_emptyStringInput()
   {
      testExpectedToThrow("");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildMappingFromJson_malformedJsonInput()
   {
      testExpectedToThrow("{foo=bar}");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildMappingFromJson_validKeyBasedInput()
   {
      JsonToQFieldMappingAdapter jsonToQFieldMappingAdapter = new JsonToQFieldMappingAdapter();
      AbstractQFieldMapping<String> mapping = (QKeyBasedFieldMapping) jsonToQFieldMappingAdapter.buildMappingFromJson("""
         {
            "Field1": "source1",
            "Field2": "source2",
         }
         """);
      System.out.println(mapping);
      assertNotNull(mapping);

      assertEquals("source1", mapping.getFieldSource("Field1"));
      assertEquals("source2", mapping.getFieldSource("Field2"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildMappingFromJson_validIndexBasedInput()
   {
      JsonToQFieldMappingAdapter jsonToQFieldMappingAdapter = new JsonToQFieldMappingAdapter();
      AbstractQFieldMapping<Integer> mapping = (QIndexBasedFieldMapping) jsonToQFieldMappingAdapter.buildMappingFromJson("""
         {
            "Field1": 1,
            "Field2": 2,
         }
         """);
      System.out.println(mapping);
      assertNotNull(mapping);

      assertEquals(1, mapping.getFieldSource("Field1"));
      assertEquals(2, mapping.getFieldSource("Field2"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildMappingFromJson_unsupportedTypeForSource()
   {
      testExpectedToThrow("""
         {
            "Field1": [1, 2],
            "Field2": {"A": "B"}
         }
         """);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_deserializeSerializedQKeyBasedFieldMapping()
   {
      QIndexBasedFieldMapping original = new QIndexBasedFieldMapping()
         .withMapping("foo", 0)
         .withMapping("bar", 1);
      String                   json         = JsonUtils.toJson(original);
      AbstractQFieldMapping<?> deserialized = new JsonToQFieldMappingAdapter().buildMappingFromJson(json);
      Assertions.assertThat(deserialized).usingRecursiveComparison().isEqualTo(original);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_deserializeSerializedQIndexBasedFieldMapping()
   {
      QKeyBasedFieldMapping original = new QKeyBasedFieldMapping()
         .withMapping("foo", "Fu")
         .withMapping("bar", "Bahr");
      String                   json         = JsonUtils.toJson(original);
      AbstractQFieldMapping<?> deserialized = new JsonToQFieldMappingAdapter().buildMappingFromJson(json);
      Assertions.assertThat(deserialized).usingRecursiveComparison().isEqualTo(original);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildMappingFromJson_emptyMapping()
   {
      testExpectedToThrow("{}");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void test_buildMappingFromJson_inputJsonList()
   {
      testExpectedToThrow("[]");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void testExpectedToThrow(String json)
   {
      try
      {
         JsonToQFieldMappingAdapter jsonToQFieldMappingAdapter = new JsonToQFieldMappingAdapter();
         AbstractQFieldMapping<?>   mapping                    = jsonToQFieldMappingAdapter.buildMappingFromJson(json);
         System.out.println(mapping);
      }
      catch(IllegalArgumentException iae)
      {
         System.out.println("Threw expected exception");
         return;
      }

      fail("Didn't throw expected exception");
   }

}
