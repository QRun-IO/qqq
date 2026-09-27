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

package com.kingsrook.qqq.backend.core.utils;


import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.reflect.TypeToken;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Utility class for working with Collections.
 **
 *******************************************************************************/
public class CollectionUtils
{
   /*******************************************************************************
    ** true if c is null or it's empty
    **
    *******************************************************************************/
   public static boolean nullSafeIsEmpty(Collection<?> c)
   {
      if(c == null || c.isEmpty())
      {
         return (true);
      }

      return (false);
   }



   /*******************************************************************************
    ** true if c is null or it's empty
    **
    *******************************************************************************/
   public static boolean nullSafeIsEmpty(Map<?, ?> c)
   {
      if(c == null || c.isEmpty())
      {
         return (true);
      }

      return (false);
   }



   /*******************************************************************************
    ** true if c is NOT null and it's not empty
    **
    *******************************************************************************/
   public static boolean nullSafeHasContents(Collection<?> c)
   {
      return (!nullSafeIsEmpty(c));
   }



   /*******************************************************************************
    ** true if c is NOT null and it's not empty
    **
    *******************************************************************************/
   public static boolean nullSafeHasContents(Map<?, ?> c)
   {
      return (!nullSafeIsEmpty(c));
   }



   /*******************************************************************************
    ** 0 if c is empty, otherwise, its size.
    **
    *******************************************************************************/
   public static int nullSafeSize(Collection<?> c)
   {
      if(c == null)
      {
         return (0);
      }

      return (c.size());
   }



   /*******************************************************************************
    ** 0 if c is empty, otherwise, its size.
    **
    *******************************************************************************/
   public static int nullSafeSize(Map<?, ?> c)
   {
      if(c == null)
      {
         return (0);
      }

      return (c.size());
   }



   /*******************************************************************************
    ** add all values from one map to another.
    **
    *******************************************************************************/
   public static <K, V> void addAllToMap(Map<K, V> addingTo, Map<K, V> addingFrom)
   {
      for(K key : addingFrom.keySet())
      {
         addingTo.put(key, addingFrom.get(key));
      }
   }



   /*******************************************************************************
    ** Build a map from a list, supplying a function that extracts keys from the
    ** objects in the list (the objects in the list become the values in the map).
    **
    *******************************************************************************/
   public static <K, V> Map<K, V> listToMap(List<V> values, Function<V, ? extends K> keyFunction)
   {
      if(values == null)
      {
         return (null);
      }

      Map<K, V> rs = new HashMap<>();
      for(V value : values)
      {
         rs.put(keyFunction.apply(value), value);
      }

      return (rs);
   }



   /*******************************************************************************
    ** Build a map from a list, supplying a function that extracts keys from the
    ** objects in the list, and another function that extracts the values from the
    ** objects in the list.
    **
    *******************************************************************************/
   public static <E, K, V> Map<K, V> listToMap(List<E> elements, Function<E, ? extends K> keyFunction, Function<E, ? extends V> valueFunction)
   {
      if(elements == null)
      {
         return (null);
      }

      Map<K, V> rs = new HashMap<>();
      for(E element : elements)
      {
         rs.put(keyFunction.apply(element), valueFunction.apply(element));
      }

      return (rs);
   }



   /*******************************************************************************
    ** Build a listingHash from a list, supplying a function that extracts keys from the
    ** objects in the list (the objects in the list become the values in the map).
    **
    *******************************************************************************/
   public static <K, V> ListingHash<K, V> listToListingHash(List<V> values, Function<V, ? extends K> keyFunction)
   {
      if(values == null)
      {
         return (null);
      }

      ListingHash<K, V> rs = new ListingHash<>();
      for(V value : values)
      {
         rs.add(keyFunction.apply(value), value);
      }

      return (rs);
   }



   /*******************************************************************************
    * <p>Take a list of objects, and build a listing hash, using 2 lambdas to control
    * how keys in the listing hash are created (from the objects), and how values
    * are created.</p>
    *
    * <p>For example, given a list of Work records, if we want a Listing hash with
    * workStatusId as keys, and workId a values, we would call:</p>
    *
    * <code>listToListingHash(workList, Work::getWorkStatusId, Work::getId)</code>
    *
    * @param elements list of objects to be mapped
    * @param keyFunction function to map an object from elements list into keys
    *                    for the listing hash
    * @param valueFunction function to map an object from elements list into values
    *                       for the listing hash
    *
    * @return ListingHash that might look like:
    *     1 -> [ 73, 75, 68]
    *     2 -> [ 74 ]
    *     4 -> [ 76, 77, 79, 80, 81]
    *
    * end
    *******************************************************************************/
   public static <E, K, V> ListingHash<K, V> listToListingHash(List<E> elements, Function<E, ? extends K> keyFunction, Function<E, ? extends V> valueFunction)
   {
      if(elements == null)
      {
         return (null);
      }

      ListingHash<K, V> rs = new ListingHash<>();
      for(E element : elements)
      {
         rs.add(keyFunction.apply(element), valueFunction.apply(element));
      }

      return (rs);
   }



   /*******************************************************************************
    ** Convert a list to a 2-level Map (ie., Map of Map of Key,Value), where 2
    ** lambdas are provided for extract the two levels of keys from the objects in
    ** the list, and the values in the map are the values in the list.
    **
    *******************************************************************************/
   public static <K1, K2, V> Map<K1, Map<K2, V>> listTo2LevelMap(List<V> values, Function<V, ? extends K1> keyFunction1, Function<V, ? extends K2> keyFunction2)
   {
      if(values == null)
      {
         return (null);
      }

      Map<K1, Map<K2, V>> rs = new HashMap<>();
      for(V value : values)
      {
         K1 k1 = keyFunction1.apply(value);
         if(!rs.containsKey(k1))
         {
            rs.put(k1, new HashMap<>());
         }
         rs.get(k1).put(keyFunction2.apply(value), value);
      }

      return (rs);
   }



   /*******************************************************************************
    ** split a large collection into lists of lists, with specified pageSize
    **
    *******************************************************************************/
   public static <T> List<List<T>> getPages(Collection<T> values, int pageSize)
   {
      List<List<T>> rs = new LinkedList<>();

      if(values == null || values.isEmpty())
      {
         //////////////////////////////////////////////////////////////////
         // if there are no input values, return an empty list of lists. //
         //////////////////////////////////////////////////////////////////
         return (rs);
      }

      List<T> currentPage = new LinkedList<>();
      rs.add(currentPage);

      for(T value : values)
      {
         if(currentPage.size() >= pageSize)
         {
            currentPage = new LinkedList<>();
            rs.add(currentPage);
         }

         currentPage.add(value);
      }

      return (rs);
   }



   /*******************************************************************************
    ** build comma-delimited string of question marks from a collection (e.g., for
    ** an sql string)
    **
    *******************************************************************************/
   public static String getQuestionMarks(Collection<?> c)
   {
      if(CollectionUtils.nullSafeIsEmpty(c))
      {
         return ("");
      }

      StringBuilder rs = new StringBuilder();
      for(int i = 0; i < c.size(); i++)
      {
         rs.append(i > 0 ? "," : "").append("?");
      }

      return (rs.toString());
   }



   /*******************************************************************************
    ** Get a sub-list, safely - i.e., w/o worrying if your indexes are too big.
    *******************************************************************************/
   public static <T> List<T> safelyGetPage(List<T> list, Integer skip, Integer limit)
   {
      //////////////////////////////////////////////////////////
      // throw if the user requested a negative skip or limit //
      //////////////////////////////////////////////////////////
      if(skip != null && skip < 0)
      {
         throw (new IllegalArgumentException("Skip may not be negative (was " + skip + ")"));
      }
      if(limit != null && limit < 0)
      {
         throw (new IllegalArgumentException("Limit may not be negative (was " + limit + ")"));
      }

      ///////////////////////////////////
      // null input list = null output //
      ///////////////////////////////////
      if(list == null)
      {
         return (null);
      }

      int startAt;
      if(skip == null)
      {
         ///////////////////////////////////////////
         // treat null skip is the same as 0 skip //
         ///////////////////////////////////////////
         startAt = 0;
      }
      else if(skip > list.size())
      {
         ////////////////////////////////////////
         // if skip is too large, return empty //
         ////////////////////////////////////////
         return (new ArrayList<>());
      }
      else
      {
         startAt = skip;
      }

      int endAt;
      if(limit == null)
      {
         ////////////////////////////////////////////////////////
         // treat null limit as request for all after the skip //
         ////////////////////////////////////////////////////////
         endAt = list.size();
      }
      else if(limit == 0)
      {
         /////////////////////////////////////////////
         // treat limit of zero as request for none //
         /////////////////////////////////////////////
         return (new ArrayList<>());
      }
      else
      {
         endAt = startAt + limit;
         if(endAt > list.size())
         {
            endAt = list.size();
         }
      }

      return list.subList(startAt, endAt);
   }



   /*******************************************************************************
    ** Returns the input array, unless it was null - in which case a new (empty) array is returned.
    **
    ** Meant to help avoid null checks on foreach loops.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public static <T> T[] nonNullArray(T[] array)
   {
      if(array == null)
      {
         return (T[]) new ArrayList<T>().toArray();
      }
      return (array);
   }



   /*******************************************************************************
    ** Returns the input list, unless it was null - in which case a new array list is returned.
    **
    ** Meant to help avoid null checks on foreach loops.
    *******************************************************************************/
   public static <T> List<T> nonNullList(List<T> list)
   {
      if(list == null)
      {
         return (new ArrayList<>());
      }
      return (list);
   }



   /*******************************************************************************
    ** Returns the input collection, unless it was null - in which case a new array list is returned.
    **
    ** Meant to help avoid null checks on foreach loops.
    *******************************************************************************/
   public static <T> Collection<T> nonNullCollection(Collection<T> c)
   {
      if(c == null)
      {
         return (new ArrayList<>());
      }
      return (c);
   }



   /*******************************************************************************
    ** Returns the input map, unless it was null - in which case a new HashMap is returned.
    **
    ** Meant to help avoid null checks on foreach loops.
    *******************************************************************************/
   public static <K, V> Map<K, V> nonNullMap(Map<K, V> map)
   {
      if(map == null)
      {
         return (new HashMap<>());
      }
      return (map);
   }



   /*******************************************************************************
    ** Convert a collection of QRecords to a map, from one field's values out of
    ** those records, to another field's value from those records
    *******************************************************************************/
   public static Map<Serializable, Serializable> recordsToMap(Collection<QRecord> records, String keyFieldName, String valueFieldName)
   {
      Map<Serializable, Serializable> rs = new HashMap<>();

      for(QRecord record : nonNullCollection(records))
      {
         rs.put(record.getValue(keyFieldName), record.getValue(valueFieldName));
      }

      return (rs);
   }



   /*******************************************************************************
    ** Convert a collection of QRecords to a map, from one field's values out of
    ** those records, to the records themselves.
    *******************************************************************************/
   public static Map<Serializable, QRecord> recordsToMap(Collection<QRecord> records, String keyFieldName)
   {
      Map<Serializable, QRecord> rs = new HashMap<>();

      for(QRecord record : nonNullCollection(records))
      {
         rs.put(record.getValue(keyFieldName), record);
      }

      return (rs);
   }



   /*******************************************************************************
    ** Convert a collection of QRecords to a map, from one field's values out of
    ** those records, to the records themselves.
    *******************************************************************************/
   public static <T extends Serializable> Map<T, QRecord> recordsToMap(Collection<QRecord> records, String keyFieldName, Class<T> type)
   {
      Map<T, QRecord> rs = new HashMap<>();

      for(QRecord record : nonNullCollection(records))
      {
         rs.put(ValueUtils.getValueAsType(type, record.getValue(keyFieldName)), record);
      }

      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static Map<?, ?> objectToMap(Object o)
   {
      ObjectMapper mapper = new ObjectMapper()
         .registerModule(new JavaTimeModule())
         .setSerializationInclusion(JsonInclude.Include.NON_NULL)
         .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
         .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

      return (mapper.convertValue(o, Map.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SafeVarargs
   public static <T> List<T> mergeLists(List<T>... lists)
   {
      List<T> rs = new ArrayList<>();
      if(lists != null)
      {
         for(List<T> list : lists)
         {
            if(list != null)
            {
               rs.addAll(list);
            }
         }
      }
      return (rs);
   }



   /*******************************************************************************
    ** For cases where you have a Collection (of an unknown type), and you know you
    ** want/need it in a specific concrete type (say, ArrayList), but you don't want
    ** to just blindly copy it (e.g., as that may be expensive), call this method.
    **
    ** ArrayList[String] myStrings = CollectionUtils.useOrWrap(yourStrings, new TypeToken<>() {});
    ** CollectionUtils.useOrWrap(yourStrings, TypeToken.get(ArrayList.class));
    **
    ** Note that you may always just pass `new TypeToken() {}` as the 2nd arg - then
    ** the compiler will infer the type (T) based on the variable you're assigning into.
    *******************************************************************************/
   public static <E, T extends Collection<E>> T useOrWrap(Collection<E> collection, TypeToken<T> typeToken)
   {
      try
      {
         if(collection == null)
         {
            return (null);
         }

         @SuppressWarnings("unchecked")
         Class<T> targetClass = (Class<T>) typeToken.getRawType();
         if(targetClass.isInstance(collection))
         {
            return (targetClass.cast(collection));
         }

         Constructor<T> constructor = targetClass.getConstructor(Collection.class);
         return (constructor.newInstance(collection));
      }
      catch(Exception e)
      {
         throw (new QRuntimeException("Error wrapping collection", e));
      }
   }



   /*******************************************************************************
    ** For cases where you have a Collection (of an unknown type), and you know you
    ** want/need it in a specific concrete type (say, ArrayList), but you don't want
    ** to just blindly copy it (e.g., as that may be expensive), call this method.
    **
    ** HashMap[String,Integer] myMap = CollectionUtils.useOrWrap(yourMap, new TypeToken<>() {});
    ** CollectionUtils.useOrWrap(yourMap, TypeToken.get(HashMap.class));
    **
    ** Note that you may always just pass `new TypeToken() {}` as the 2nd arg - then
    ** the compiler will infer the type (T) based on the variable you're assigning into.
    *******************************************************************************/
   public static <K, V, T extends Map<K, V>> T useOrWrap(Map<K, V> collection, TypeToken<T> typeToken)
   {
      try
      {
         if(collection == null)
         {
            return (null);
         }

         @SuppressWarnings("unchecked")
         Class<T> targetClass = (Class<T>) typeToken.getRawType();
         if(targetClass.isInstance(collection))
         {
            return (targetClass.cast(collection));
         }

         Constructor<T> constructor = targetClass.getConstructor(Map.class);
         return (constructor.newInstance(collection));
      }
      catch(Exception e)
      {
         throw (new QRuntimeException("Error wrapping collection", e));
      }
   }



   /*******************************************************************************
    ** Take a multi-level map, e.g., Map{String, Map{Integer, BigDecimal}}
    ** and invert it - e.g., to Map{Integer, Map{String, BigDecimal}}
    *******************************************************************************/
   public static <K1, K2, V> Map<K2, Map<K1, V>> swapMultiLevelMapKeys(Map<K1, Map<K2, V>> input)
   {
      if(input == null)
      {
         return (null);
      }

      Map<K2, Map<K1, V>> output = new HashMap<>();

      for(Map.Entry<K1, Map<K2, V>> entry : input.entrySet())
      {
         K1         key1 = entry.getKey();
         Map<K2, V> map1 = entry.getValue();

         if(map1 != null)
         {
            for(Map.Entry<K2, V> entry2 : map1.entrySet())
            {
               K2 key2  = entry2.getKey();
               V  value = entry2.getValue();

               output.computeIfAbsent(key2, (k) -> new HashMap<>());
               output.get(key2).put(key1, value);
            }
         }
      }

      return (output);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static <K> boolean containsKeyWithNonNullValue(Map<K, ?> map, K key)
   {
      if(map == null)
      {
         return (false);
      }

      return (map.containsKey(key) && map.get(key) != null);
   }



   /***************************************************************************
    ** add an element to a collection, but, only if the element isn't null
    ***************************************************************************/
   public static <T, E extends T> void addIfNotNull(Collection<T> c, E element)
   {
      if(element != null)
      {
         c.add(element);
      }
   }



   /***************************************************************************
    * add all objects in a source collection to a destination collection, in a
    * null-safe manner with regard to the source.
    *
    * @param destination collection to put objects into.  May NOT be null.
    *                    if it's immutable, and source is not null, that will
    *                    fail (as you'd expect) too.
    * @param source collection to get objects from.  if null, is ignored.
    * @throws NullPointerException if destination is null.
    ***************************************************************************/
   public static <T> void addAllIfNotNull(Collection<T> destination, Collection<T> source)
   {
      Objects.requireNonNull(destination, "destination may not be null");

      if(source != null)
      {
         destination.addAll(source);
      }
   }



   /***************************************************************************
    * Add an object to a list after another object already in the list
    ***************************************************************************/
   public static <T> void addAfter(List<T> list, T target, T elementToInsert)
   {
      int idx = list.indexOf(target);
      if(idx == -1)
      {
         throw new IllegalArgumentException("Target element not found");
      }
      list.add(idx + 1, elementToInsert);
   }

}
