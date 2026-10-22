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

package com.kingsrook.qqq.backend.core.utils.collections;


import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.utils.lambdas.VoidVoidMethod;


/*******************************************************************************
 ** Object to wrap a Map, so that in case a caller provided an immutable Map,
 ** you can safely perform mutating operations on it (in which case, it'll get
 ** replaced by an actual mutable Map).
 *******************************************************************************/
public class MutableMap<K, V> implements Map<K, V>
{
   private Map<K, V>           sourceMap;
   private Supplier<Map<K, V>> supplierIfNeeded;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public MutableMap(Map<K, V> sourceMap)
   {
      this(sourceMap, HashMap::new);
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public MutableMap(Map<K, V> sourceMap, Supplier<Map<K, V>> supplierIfNeeded)
   {
      this.sourceMap = Objects.requireNonNullElseGet(sourceMap, supplierIfNeeded);
      this.supplierIfNeeded = supplierIfNeeded;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   Map<K, V> getUnderlyingMap()
   {
      return (sourceMap);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void replaceSourceMapWithMutableCopy()
   {
      try
      {
         Map<K, V> replacementMap = supplierIfNeeded.get();
         replacementMap.putAll(sourceMap);
         sourceMap = replacementMap;
      }
      catch(Exception e)
      {
         throw (new IllegalStateException("Error getting from the supplier provided for this MutableMap.", e));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private <T> T doMutableOperationForValue(Supplier<T> supplier)
   {
      try
      {
         return (supplier.get());
      }
      catch(UnsupportedOperationException uoe)
      {
         replaceSourceMapWithMutableCopy();
         return (supplier.get());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void doMutableOperationForVoid(VoidVoidMethod method)
   {
      try
      {
         method.run();
      }
      catch(UnsupportedOperationException uoe)
      {
         replaceSourceMapWithMutableCopy();
         method.run();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int size()
   {
      return (sourceMap.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean isEmpty()
   {
      return (sourceMap.isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean containsKey(Object key)
   {
      return (sourceMap.containsKey(key));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean containsValue(Object value)
   {
      return (sourceMap.containsValue(value));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public V get(Object key)
   {
      return (sourceMap.get(key));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public V put(K key, V value)
   {
      return (doMutableOperationForValue(() -> sourceMap.put(key, value)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public V remove(Object key)
   {
      return (doMutableOperationForValue(() -> sourceMap.remove(key)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void putAll(Map<? extends K, ? extends V> m)
   {
      doMutableOperationForVoid(() -> sourceMap.putAll(m));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void clear()
   {
      doMutableOperationForVoid(() -> sourceMap.clear());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Set<K> keySet()
   {
      return (sourceMap.keySet());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Collection<V> values()
   {
      return (sourceMap.values());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Set<Entry<K, V>> entrySet()
   {
      return (sourceMap.entrySet());
   }
}
