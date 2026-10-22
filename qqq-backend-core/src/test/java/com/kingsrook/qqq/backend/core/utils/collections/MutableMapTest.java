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

package com.kingsrook.qqq.backend.core.utils.collections;


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for com.kingsrook.qqq.backend.core.utils.collections.MutableMap
 *******************************************************************************/
class MutableMapTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      MutableMap<String, Integer> map = new MutableMap<>(Map.of("a", 1));
      map.clear();
      map.put("b", 2);

      map = new MutableMap<>(Map.of("a", 1));
      map.remove("a");
      map.putAll(Map.of("c", 3, "d", 4));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNullInput()
   {
      Map<Integer, String> map = new MutableMap<>(null);
      map.put(1, "one");
      assertEquals(1, map.size());

      MutableMap<Integer, String> mutableMap = new MutableMap<>(null, LinkedHashMap::new);
      mutableMap.put(1, "uno");
      assertEquals(1, mutableMap.size());
      assertEquals(LinkedHashMap.class, mutableMap.getUnderlyingMap().getClass());
   }

}