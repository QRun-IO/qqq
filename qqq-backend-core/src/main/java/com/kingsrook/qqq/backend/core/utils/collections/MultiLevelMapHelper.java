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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;


/*******************************************************************************
 ** Help you use a multi-level map, such as:
 ** Map[String, Map[String, Integer]] countryStateCountMap = new HashMap[]();
 **
 ** Where you always want to put new maps at the lower-level if they aren't there,
 ** and similarly, you want to start with a 0 for the value under each (final) key.
 ** So instead of like:
 **
 ** countryStateCountMap.computeIfAbsent("US", () -> new HashMap());
 ** Map stateCountMap = countryStateCountMap.get("US");
 ** stateCountMap.putIfAbsent("MO", 0);
 ** stateCountMap.put(stateCountMap.get("MO") + count);
 **
 ** You can just do:
 ** MultiLevelMapHelper.getOrPutNextLevel(countryStateCountMap, "US",
 **    stateMap -> MultiLevelMapHelper.getOrPutAndIncrement(stateMap, "MO", count));
 **
 ** Or for a bigger map, such as:
 ** Map[Integer, Map[String, Map[Integer, Map[String, Integer]]]] bigOleMap = new HashMap[]();
 *
 ** getOrPutNextLevel(bigOleMap, clientId,
 **    map -> getOrPutNextLevel(map, sku,
 **       map2 -> getOrPutNextLevel(map2, warehouseId,
 **          map3 -> getOrPutAndIncrement(map3, state))));
 **
 *******************************************************************************/
public class MultiLevelMapHelper
{

   /*******************************************************************************
    ** For the given map,
    ** If the key is not found, run the supplier and put its result under that key
    ** Then get the value under that key and pass it to the Consumer next
    *******************************************************************************/
   public static <K, V> void getOrPutNextLevel(Map<K, V> map, K key, Supplier<V> notFoundSupplier, Consumer<V> next)
   {
      map.computeIfAbsent(key, k -> notFoundSupplier.get());
      V v = map.get(key);
      next.accept(v);
   }



   /*******************************************************************************
    ** For the given map,
    ** If the key is not found, make a new map[1] and put its result under that key
    ** Then get the value under that key (the next level map) and pass it to the Consumer next.
    **
    ** [1] - the new map will be the same type as the input map, if possible - else
    ** will be a new HashMap.  To control the map type, see the overload that takes
    ** a Supplier notFoundSupplier.
    *******************************************************************************/
   public static <K, K2, V> void getOrPutNextLevel(Map<K, Map<K2, V>> map, K key, Consumer<Map<K2, V>> next)
   {
      map.computeIfAbsent(key, k ->
      {
         try
         {
            @SuppressWarnings("unchecked")
            Map<K2, V> map1 = map.getClass().getConstructor().newInstance();
            return map1;
         }
         catch(Exception e)
         {
            return (new HashMap<>());
         }
      });
      Map<K2, V> v = map.get(key);
      next.accept(v);
   }



   /*******************************************************************************
    ** For the given map,
    ** If the key is not found, run the supplier and put its result under that key
    ** Then apply the Function next to that value, replacing the value under the key.
    *******************************************************************************/
   public static <K, V> void getOrPutFinalLevel(Map<K, V> map, K key, Supplier<V> notFoundSupplier, Function<V, V> next)
   {
      map.computeIfAbsent(key, k -> notFoundSupplier.get());
      V v = map.get(key);
      map.put(key, next.apply(v));
   }



   /*******************************************************************************
    ** For the given map,
    ** If the key is not found, put a 1 under it.
    ** else increment the value under the key.
    *******************************************************************************/
   public static <K> void getOrPutAndIncrement(Map<K, Integer> map, K key)
   {
      getOrPutAndIncrement(map, key, 1);
   }



   /*******************************************************************************
    ** For the given map,
    ** If the key is not found, put the Integer amount under it.
    ** else increment the value under the key by the Integer amount.
    *******************************************************************************/
   public static <K> void getOrPutAndIncrement(Map<K, Integer> map, K key, Integer amount)
   {
      map.putIfAbsent(key, 0);
      Integer v = map.get(key);
      map.put(key, v + amount);
   }

}
