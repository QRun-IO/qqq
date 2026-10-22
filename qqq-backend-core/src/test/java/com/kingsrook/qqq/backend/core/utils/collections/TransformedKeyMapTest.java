/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils.collections;


import java.math.BigDecimal;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for TransformedKeyMap 
 *******************************************************************************/
@SuppressWarnings({ "RedundantCollectionOperation", "RedundantOperationOnEmptyContainer" })
class TransformedKeyMapTest extends BaseTest
{
   private static final BigDecimal BIG_DECIMAL_TWO   = BigDecimal.valueOf(2);
   private static final BigDecimal BIG_DECIMAL_THREE = BigDecimal.valueOf(3);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCaseInsensitiveKeyMap()
   {
      TransformedKeyMap<String, String, Integer> caseInsensitiveKeys = new TransformedKeyMap<>(key -> key.toLowerCase());
      caseInsensitiveKeys.put("One", 1);
      caseInsensitiveKeys.put("one", 1);
      caseInsensitiveKeys.put("ONE", 1);
      assertEquals(1, caseInsensitiveKeys.get("one"));
      assertEquals(1, caseInsensitiveKeys.get("One"));
      assertEquals(1, caseInsensitiveKeys.get("oNe"));
      assertEquals(1, caseInsensitiveKeys.size());

      //////////////////////////////////////////////////
      // get back the first way it was put in the map //
      //////////////////////////////////////////////////
      assertEquals("One", caseInsensitiveKeys.entrySet().iterator().next().getKey());
      assertEquals("One", caseInsensitiveKeys.keySet().iterator().next());

      assertEquals(1, caseInsensitiveKeys.entrySet().size());
      assertEquals(1, caseInsensitiveKeys.keySet().size());

      for(String key : caseInsensitiveKeys.keySet())
      {
         assertEquals(1, caseInsensitiveKeys.get(key));
      }

      for(Map.Entry<String, Integer> entry : caseInsensitiveKeys.entrySet())
      {
         assertEquals("One", entry.getKey());
         assertEquals(1, entry.getValue());
      }

      /////////////////////////////
      // add a second unique key //
      /////////////////////////////
      caseInsensitiveKeys.put("Two", 2);
      assertEquals(2, caseInsensitiveKeys.size());
      assertEquals(2, caseInsensitiveKeys.entrySet().size());
      assertEquals(2, caseInsensitiveKeys.keySet().size());

      ////////////////////////////////////////
      // make sure remove works as expected //
      ////////////////////////////////////////
      caseInsensitiveKeys.remove("TWO");
      assertNull(caseInsensitiveKeys.get("Two"));
      assertNull(caseInsensitiveKeys.get("two"));
      assertEquals(1, caseInsensitiveKeys.size());
      assertEquals(1, caseInsensitiveKeys.keySet().size());
      assertEquals(1, caseInsensitiveKeys.entrySet().size());

      ///////////////////////////////////////
      // make sure clear works as expected //
      ///////////////////////////////////////
      caseInsensitiveKeys.clear();
      assertNull(caseInsensitiveKeys.get("one"));
      assertEquals(0, caseInsensitiveKeys.size());
      assertEquals(0, caseInsensitiveKeys.keySet().size());
      assertEquals(0, caseInsensitiveKeys.entrySet().size());

      /////////////////////////////////////////
      // make sure put-all works as expected //
      /////////////////////////////////////////
      caseInsensitiveKeys.putAll(Map.of("One", 1, "one", 1, "ONE", 1, "TwO", 2, "tWo", 2, "three", 3));
      assertEquals(1, caseInsensitiveKeys.get("oNe"));
      assertEquals(2, caseInsensitiveKeys.get("two"));
      assertEquals(3, caseInsensitiveKeys.get("Three"));
      assertEquals(3, caseInsensitiveKeys.size());
      assertEquals(3, caseInsensitiveKeys.entrySet().size());
      assertEquals(3, caseInsensitiveKeys.keySet().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStringToNumberMap()
   {
      TransformedKeyMap<String, Integer, BigDecimal> multiLingualWordToNumber = new TransformedKeyMap<>(key -> switch(key.toLowerCase())
      {
         case "one", "uno", "eins" -> 1;
         case "two", "dos", "zwei" -> 2;
         case "three", "tres", "drei" -> 3;
         default -> null;
      });
      multiLingualWordToNumber.put("One", BigDecimal.ONE);
      multiLingualWordToNumber.put("uno", BigDecimal.ONE);
      assertEquals(BigDecimal.ONE, multiLingualWordToNumber.get("one"));
      assertEquals(BigDecimal.ONE, multiLingualWordToNumber.get("uno"));
      assertEquals(BigDecimal.ONE, multiLingualWordToNumber.get("eins"));
      assertEquals(1, multiLingualWordToNumber.size());

      //////////////////////////////////////////////////
      // get back the first way it was put in the map //
      //////////////////////////////////////////////////
      assertEquals("One", multiLingualWordToNumber.entrySet().iterator().next().getKey());
      assertEquals("One", multiLingualWordToNumber.keySet().iterator().next());

      assertEquals(1, multiLingualWordToNumber.entrySet().size());
      assertEquals(1, multiLingualWordToNumber.keySet().size());

      for(String key : multiLingualWordToNumber.keySet())
      {
         assertEquals(BigDecimal.ONE, multiLingualWordToNumber.get(key));
      }

      for(Map.Entry<String, BigDecimal> entry : multiLingualWordToNumber.entrySet())
      {
         assertEquals("One", entry.getKey());
         assertEquals(BigDecimal.ONE, entry.getValue());
      }

      /////////////////////////////
      // add a second unique key //
      /////////////////////////////
      multiLingualWordToNumber.put("Two", BIG_DECIMAL_TWO);
      assertEquals(BIG_DECIMAL_TWO, multiLingualWordToNumber.get("Dos"));
      assertEquals(2, multiLingualWordToNumber.size());
      assertEquals(2, multiLingualWordToNumber.entrySet().size());
      assertEquals(2, multiLingualWordToNumber.keySet().size());

      ////////////////////////////////////////
      // make sure remove works as expected //
      ////////////////////////////////////////
      multiLingualWordToNumber.remove("ZWEI");
      assertNull(multiLingualWordToNumber.get("Two"));
      assertNull(multiLingualWordToNumber.get("Dos"));
      assertEquals(1, multiLingualWordToNumber.size());
      assertEquals(1, multiLingualWordToNumber.keySet().size());
      assertEquals(1, multiLingualWordToNumber.entrySet().size());

      ///////////////////////////////////////
      // make sure clear works as expected //
      ///////////////////////////////////////
      multiLingualWordToNumber.clear();
      assertNull(multiLingualWordToNumber.get("eins"));
      assertNull(multiLingualWordToNumber.get("One"));
      assertEquals(0, multiLingualWordToNumber.size());
      assertEquals(0, multiLingualWordToNumber.keySet().size());
      assertEquals(0, multiLingualWordToNumber.entrySet().size());

      /////////////////////////////////////////
      // make sure put-all works as expected //
      /////////////////////////////////////////
      multiLingualWordToNumber.putAll(Map.of("One", BigDecimal.ONE, "Uno", BigDecimal.ONE, "EINS", BigDecimal.ONE, "dos", BIG_DECIMAL_TWO, "zwei", BIG_DECIMAL_TWO, "tres", BIG_DECIMAL_THREE));
      assertEquals(BigDecimal.ONE, multiLingualWordToNumber.get("oNe"));
      assertEquals(BIG_DECIMAL_TWO, multiLingualWordToNumber.get("dos"));
      assertEquals(BIG_DECIMAL_THREE, multiLingualWordToNumber.get("drei"));
      assertEquals(3, multiLingualWordToNumber.size());
      assertEquals(3, multiLingualWordToNumber.entrySet().size());
      assertEquals(3, multiLingualWordToNumber.keySet().size());
   }

}