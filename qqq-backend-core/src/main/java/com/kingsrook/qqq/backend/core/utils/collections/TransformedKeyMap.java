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


import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/*******************************************************************************
 ** Version of a map that uses a transformation function on keys.  The original
 ** idea being, e.g., to support case-insensitive keys via a toLowerCase transform.
 ** e.g., map.put("One", 1); map.get("ONE") == 1.
 **
 ** But, implemented generically to support any transformation function.
 **
 ** keySet() and entries() should give only the first version of a key that overlapped.
 ** e.g., map.put("One", 1); map.put("one", 1); map.keySet() == Set.of("One");
 *******************************************************************************/
public class TransformedKeyMap<OK, TK, V> implements Map<OK, V>
{
   private Function<OK, TK> keyTransformer;
   private Map<TK, V>       wrappedMap;

   private Map<TK, OK> originalKeys = new HashMap<>();



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TransformedKeyMap(Function<OK, TK> keyTransformer)
   {
      this.keyTransformer = keyTransformer;
      this.wrappedMap = new HashMap<>();
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public TransformedKeyMap(Function<OK, TK> keyTransformer, Supplier<Map<TK, V>> supplier)
   {
      this.keyTransformer = keyTransformer;
      this.wrappedMap = supplier.get();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public int size()
   {
      return (wrappedMap.size());
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public boolean isEmpty()
   {
      return (wrappedMap.isEmpty());
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public boolean containsKey(Object key)
   {
      try
      {
         TK transformed = keyTransformer.apply((OK) key);
         return wrappedMap.containsKey(transformed);
      }
      catch(Exception e)
      {
         return (false);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public boolean containsValue(Object value)
   {
      return (wrappedMap.containsValue(value));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public V get(Object key)
   {
      try
      {
         TK transformed = keyTransformer.apply((OK) key);
         return wrappedMap.get(transformed);
      }
      catch(Exception e)
      {
         return (null);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public @Nullable V put(OK key, V value)
   {
      TK transformed = keyTransformer.apply(key);
      originalKeys.putIfAbsent(transformed, key);
      return wrappedMap.put(transformed, value);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public V remove(Object key)
   {
      try
      {
         TK transformed = keyTransformer.apply((OK) key);
         originalKeys.remove(transformed);
         return wrappedMap.remove(transformed);
      }
      catch(Exception e)
      {
         return (null);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void putAll(@NotNull Map<? extends OK, ? extends V> m)
   {
      for(Entry<? extends OK, ? extends V> entry : m.entrySet())
      {
         put(entry.getKey(), entry.getValue());
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void clear()
   {
      wrappedMap.clear();
      originalKeys.clear();
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public @NotNull Set<OK> keySet()
   {
      return new HashSet<>(originalKeys.values());
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public @NotNull Collection<V> values()
   {
      return wrappedMap.values();
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public @NotNull Set<Entry<OK, V>> entrySet()
   {
      Set<Entry<TK, V>> wrappedEntries = wrappedMap.entrySet();
      Set<Entry<OK, V>> originalEntries;
      try
      {
         originalEntries = wrappedEntries.getClass().getConstructor().newInstance();
      }
      catch(Exception e)
      {
         originalEntries = new HashSet<>();
      }

      for(Entry<TK, V> wrappedEntry : wrappedEntries)
      {
         OK originalKey = originalKeys.get(wrappedEntry.getKey());
         originalEntries.add(new TransformedKeyMapEntry<>(originalKey, wrappedEntry.getValue()));
      }

      return (originalEntries);
   }

   // methods with a default implementation below here //



   /*
   @Override
   public V getOrDefault(Object key, V defaultValue)
   {
      return Map.super.getOrDefault(key, defaultValue);
   }



   @Override
   public void forEach(BiConsumer<? super OK, ? super V> action)
   {
      Map.super.forEach(action);
   }



   @Override
   public void replaceAll(BiFunction<? super OK, ? super V, ? extends V> function)
   {
      Map.super.replaceAll(function);
   }



   @Override
   public @Nullable V putIfAbsent(OK key, V value)
   {
      return Map.super.putIfAbsent(key, value);
   }



   @Override
   public boolean remove(Object key, Object value)
   {
      return Map.super.remove(key, value);
   }



   @Override
   public boolean replace(OK key, V oldValue, V newValue)
   {
      return Map.super.replace(key, oldValue, newValue);
   }



   @Override
   public @Nullable V replace(OK key, V value)
   {
      return Map.super.replace(key, value);
   }



   @Override
   public V computeIfAbsent(OK key, @NotNull Function<? super OK, ? extends V> mappingFunction)
   {
      return Map.super.computeIfAbsent(key, mappingFunction);
   }



   @Override
   public V computeIfPresent(OK key, @NotNull BiFunction<? super OK, ? super V, ? extends V> remappingFunction)
   {
      return Map.super.computeIfPresent(key, remappingFunction);
   }



   @Override
   public V compute(OK key, @NotNull BiFunction<? super OK, ? super @Nullable V, ? extends V> remappingFunction)
   {
      return Map.super.compute(key, remappingFunction);
   }



   @Override
   public V merge(OK key, @NotNull V value, @NotNull BiFunction<? super V, ? super V, ? extends V> remappingFunction)
   {
      return Map.super.merge(key, value, remappingFunction);
   }
   */



   /***************************************************************************
    *
    ***************************************************************************/
   public static class TransformedKeyMapEntry<EK, EV> implements Map.Entry<EK, EV>
   {
      private final EK key;
      private       EV value;



      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public TransformedKeyMapEntry(EK key, EV value)
      {
         this.key = key;
         this.value = value;
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public EK getKey()
      {
         return (key);
      }



      @Override
      public EV getValue()
      {
         return (value);
      }



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public EV setValue(EV value)
      {
         throw (new UnsupportedOperationException("Setting value in an entry of a TransformedKeyMap is not supported."));
      }
   }
}
