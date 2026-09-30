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

package com.kingsrook.qqq.backend.core.utils;


import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for CountingHash
 *******************************************************************************/
class CountingHashTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      CountingHash<String> countingHash = new CountingHash<>();

      assertNull(countingHash.get("a"));

      countingHash.add("a");
      assertEquals(1, countingHash.get("a"));

      countingHash.add("a");
      assertEquals(2, countingHash.get("a"));

      countingHash.add("a", 2);
      assertEquals(4, countingHash.get("a"));

      countingHash.add("b", 5);
      assertEquals(5, countingHash.get("b"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAlwaysMutable()
   {
      CountingHash<String> alwaysMutable = new CountingHash<>(Map.of("A", 5));
      alwaysMutable.add("A");
      alwaysMutable.add("B");
      assertEquals(6, alwaysMutable.get("A"));
      assertEquals(1, alwaysMutable.get("B"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPut()
   {
      CountingHash<String> alwaysMutable = new CountingHash<>(Map.of("A", 5));
      alwaysMutable.put("A", 25);
      assertEquals(25, alwaysMutable.get("A"));
      alwaysMutable.put("A");
      assertEquals(26, alwaysMutable.get("A"));
   }

}
