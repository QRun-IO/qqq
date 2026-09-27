/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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


import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for YamlUtils
 *******************************************************************************/
class YamlUtilsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws JsonProcessingException
   {
      String yaml = """
         orderNo: "B-9910"
         date: "2019-04-18"
         customerName: "Customer, Jane"
         orderLines:
         - item: "Copper Wire (200ft)"
           quantity: 1
           unitPrice: 50.67
         - item: "Washers (1/4\\")"
           quantity: 24
           unitPrice: 0.15
         """;

      Map<String, Object> map = YamlUtils.toMap(yaml);
      System.out.println(map);
   }



   /***************************************************************************
    * simple bean to use in customObjectMapper test
    * we'd use a map, but SORT_PROPERTIES_ALPHABETICALLY doesn't apply to maps...
    ***************************************************************************/
   private record SomeBean(String foo, Integer bar) {}



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCustomObjectMapper() throws JsonProcessingException
   {
      SomeBean someBean = new SomeBean("Hi", 47);

      ///////////////////////////////////////////////////////////////////
      // by default, the fields come out in the order they're declared //
      ///////////////////////////////////////////////////////////////////
      assertEquals("""
         foo: "Hi"
         bar: 47
         """, YamlUtils.toYaml(someBean));

      /////////////////////////////////////////////////////////////
      // customize the builder to sort properties alphabetically //
      // (to assert that doing a customization works)            //
      /////////////////////////////////////////////////////////////
      String outputYaml = YamlUtils.toYamlCustomized(someBean, yamlMapperBuilder ->
      {
         yamlMapperBuilder.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
      });

      assertEquals("""
         bar: 47
         foo: "Hi"
         """, outputYaml);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testQInstanceToYaml()
   {
      QInstance qInstance = TestUtils.defineInstance();
      String    yaml      = YamlUtils.toYaml(qInstance);
      System.out.println(yaml);
   }

}