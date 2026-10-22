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


import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for MapBuilder
 *******************************************************************************/
class MapBuilderTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSameAsMapOf()
   {
      assertEquals(Map.of("1", 1), MapBuilder.of("1", 1));
      assertEquals(Map.of("1", 1, "2", 2), MapBuilder.of("1", 1, "2", 2));
      assertEquals(Map.of("1", 1, "2", 2, "3", 3), MapBuilder.of("1", 1, "2", 2, "3", 3));
      assertEquals(Map.of("1", 1, "2", 2, "3", 3, "4", 4), MapBuilder.of("1", 1, "2", 2, "3", 3, "4", 4));
      assertEquals(Map.of("1", 1, "2", 2, "3", 3, "4", 4, "5", 5), MapBuilder.of("1", 1, "2", 2, "3", 3, "4", 4, "5", 5));
      assertEquals(Map.of("1", 1, "2", 2, "3", 3, "4", 4, "5", 5, "6", 6), MapBuilder.of("1", 1, "2", 2, "3", 3, "4", 4, "5", 5, "6", 6));
      assertEquals(Map.of("1", 1, "2", 2, "3", 3, "4", 4, "5", 5, "6", 6, "7", 7), MapBuilder.of("1", 1, "2", 2, "3", 3, "4", 4, "5", 5, "6", 6, "7", 7));
      assertEquals(Map.of("1", 1, "2", 2, "3", 3, "4", 4, "5", 5, "6", 6, "7", 7, "8", 8), MapBuilder.of("1", 1, "2", 2, "3", 3, "4", 4, "5", 5, "6", 6, "7", 7, "8", 8));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBetterThanMapOf()
   {
      ///////////////////////////////
      // assert this doesn't throw //
      ///////////////////////////////
      Map<String, Object> map = MapBuilder.of("1", null);

      ///////////////////////////////////////
      // this too, doesn't freaking throw. //
      ///////////////////////////////////////
      map.put("2", null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTypeYouRequest()
   {
      TreeMap<String, Integer> myTreeMap = MapBuilder.of(() -> new TreeMap<String, Integer>()).with("1", 1).with("2", 2).build();
      assertTrue(myTreeMap instanceof TreeMap);
   }

}