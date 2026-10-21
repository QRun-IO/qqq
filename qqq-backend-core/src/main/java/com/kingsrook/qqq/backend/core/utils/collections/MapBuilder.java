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
import java.util.function.Supplier;


/*******************************************************************************
 ** Map.of is "great", but annoying because it makes unmodifiable maps, and it
 ** NPE's on nulls...  So, replace it with this, which returns HashMaps (or maps
 ** of the type you choose).
 **
 ** Can use it 2 ways:
 ** MapBuilder.of(key, value, key2, value2, ...) => Map (a HashMap)
 ** MapBuilder.of(() -> new SomeMap<SomeKeyType, SomeValueType>()).with(key, value).with(key2, value2)...build() => SomeMap (the type you specify)
 *******************************************************************************/
public class MapBuilder<K, V, M extends Map<K, V>>
{
   private M map;



   /*******************************************************************************
    **
    *******************************************************************************/
   private MapBuilder(M map)
   {
      this.map = map;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <K, V, M extends Map<K, V>> MapBuilder<K, V, M> of(Supplier<M> mapSupplier)
   {
      return (new MapBuilder<>(mapSupplier.get()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public MapBuilder<K, V, M> with(K key, V value)
   {
      map.put(key, value);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public M build()
   {
      return (this.map);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <K, V> Map<K, V> of(
      K k1, V v1
   )
   {
      Map<K, V> rs = new HashMap<>();
      rs.put(k1, v1);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <K, V> Map<K, V> of(
      K k1, V v1,
      K k2, V v2
   )
   {
      Map<K, V> rs = new HashMap<>();
      rs.put(k1, v1);
      rs.put(k2, v2);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <K, V> Map<K, V> of(
      K k1, V v1,
      K k2, V v2,
      K k3, V v3
   )
   {
      Map<K, V> rs = new HashMap<>();
      rs.put(k1, v1);
      rs.put(k2, v2);
      rs.put(k3, v3);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <K, V> Map<K, V> of(
      K k1, V v1,
      K k2, V v2,
      K k3, V v3,
      K k4, V v4
   )
   {
      Map<K, V> rs = new HashMap<>();
      rs.put(k1, v1);
      rs.put(k2, v2);
      rs.put(k3, v3);
      rs.put(k4, v4);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <K, V> Map<K, V> of(
      K k1, V v1,
      K k2, V v2,
      K k3, V v3,
      K k4, V v4,
      K k5, V v5
   )
   {
      Map<K, V> rs = new HashMap<>();
      rs.put(k1, v1);
      rs.put(k2, v2);
      rs.put(k3, v3);
      rs.put(k4, v4);
      rs.put(k5, v5);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <K, V> Map<K, V> of(
      K k1, V v1,
      K k2, V v2,
      K k3, V v3,
      K k4, V v4,
      K k5, V v5,
      K k6, V v6
   )
   {
      Map<K, V> rs = new HashMap<>();
      rs.put(k1, v1);
      rs.put(k2, v2);
      rs.put(k3, v3);
      rs.put(k4, v4);
      rs.put(k5, v5);
      rs.put(k6, v6);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <K, V> Map<K, V> of(
      K k1, V v1,
      K k2, V v2,
      K k3, V v3,
      K k4, V v4,
      K k5, V v5,
      K k6, V v6,
      K k7, V v7
   )
   {
      Map<K, V> rs = new HashMap<>();
      rs.put(k1, v1);
      rs.put(k2, v2);
      rs.put(k3, v3);
      rs.put(k4, v4);
      rs.put(k5, v5);
      rs.put(k6, v6);
      rs.put(k7, v7);
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <K, V> Map<K, V> of(
      K k1, V v1,
      K k2, V v2,
      K k3, V v3,
      K k4, V v4,
      K k5, V v5,
      K k6, V v6,
      K k7, V v7,
      K k8, V v8
   )
   {
      Map<K, V> rs = new HashMap<>();
      rs.put(k1, v1);
      rs.put(k2, v2);
      rs.put(k3, v3);
      rs.put(k4, v4);
      rs.put(k5, v5);
      rs.put(k6, v6);
      rs.put(k7, v7);
      rs.put(k8, v8);
      return (rs);
   }

}
