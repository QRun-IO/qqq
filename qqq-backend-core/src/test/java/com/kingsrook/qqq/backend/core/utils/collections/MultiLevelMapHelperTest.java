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


import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static com.kingsrook.qqq.backend.core.utils.collections.MultiLevelMapHelper.getOrPutAndIncrement;
import static com.kingsrook.qqq.backend.core.utils.collections.MultiLevelMapHelper.getOrPutNextLevel;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for MultiLevelMapHelper
 *******************************************************************************/
class MultiLevelMapHelperTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      Map<Integer, Map<String, Map<Integer, Map<String, Integer>>>> bigOleMap = new HashMap<>();

      Integer clientId    = 120;
      String  sku         = "BASIC1";
      Integer warehouseId = 10;
      String  state       = "MO";
      Integer quantity    = 5;

      getOrPutNextLevel(bigOleMap, clientId,
         map -> getOrPutNextLevel(map, sku,
            map2 -> getOrPutNextLevel(map2, warehouseId,
               map3 -> MultiLevelMapHelper.getOrPutFinalLevel(map3, state, () -> 0, v -> v + quantity))));
      assertEquals(5, bigOleMap.get(120).get("BASIC1").get(10).get("MO"));

      getOrPutNextLevel(bigOleMap, clientId,
         map -> getOrPutNextLevel(map, sku,
            map2 -> getOrPutNextLevel(map2, warehouseId,
               map3 -> getOrPutAndIncrement(map3, state))));
      assertEquals(6, bigOleMap.get(120).get("BASIC1").get(10).get("MO"));

      getOrPutNextLevel(bigOleMap, clientId,
         map -> getOrPutNextLevel(map, sku,
            map2 -> getOrPutNextLevel(map2, warehouseId,
               map3 -> getOrPutAndIncrement(map3, state, quantity))));
      assertEquals(11, bigOleMap.get(120).get("BASIC1").get(10).get("MO"));
   }

}