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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import com.google.gson.reflect.TypeToken;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for CollectionUtils
 **
 *******************************************************************************/
class CollectionUtilsTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_nullSafeIsEmpty_Map()
   {
      assertTrue(CollectionUtils.nullSafeIsEmpty(Collections.emptyMap()));
      assertTrue(CollectionUtils.nullSafeIsEmpty(new HashMap<>()));
      assertTrue(CollectionUtils.nullSafeIsEmpty((Map<?, ?>) null));
      Map<String, Integer> myMap = new HashMap<>();
      assertTrue(CollectionUtils.nullSafeIsEmpty(myMap));
      myMap.put("A", 1);
      assertFalse(CollectionUtils.nullSafeIsEmpty(myMap));
      myMap.clear();
      assertTrue(CollectionUtils.nullSafeIsEmpty(myMap));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_nullSafeIsEmpty_Collection()
   {
      assertTrue(CollectionUtils.nullSafeIsEmpty(Collections.emptyList()));
      assertTrue(CollectionUtils.nullSafeIsEmpty(new ArrayList<>()));
      assertTrue(CollectionUtils.nullSafeIsEmpty((List<?>) null));
      List<String> myList = new ArrayList<>();
      assertTrue(CollectionUtils.nullSafeIsEmpty(myList));
      myList.add("A");
      assertFalse(CollectionUtils.nullSafeIsEmpty(myList));
      myList.clear();
      assertTrue(CollectionUtils.nullSafeIsEmpty(myList));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_nullSafeHasContents_Map()
   {
      assertFalse(CollectionUtils.nullSafeHasContents(Collections.emptyMap()));
      assertFalse(CollectionUtils.nullSafeHasContents(new HashMap<>()));
      assertFalse(CollectionUtils.nullSafeHasContents((Map<?, ?>) null));
      Map<String, Integer> myMap = new HashMap<>();
      assertFalse(CollectionUtils.nullSafeHasContents(myMap));
      myMap.put("A", 1);
      assertTrue(CollectionUtils.nullSafeHasContents(myMap));
      myMap.clear();
      assertFalse(CollectionUtils.nullSafeHasContents(myMap));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_nullSafeHasContents_Collection()
   {
      assertFalse(CollectionUtils.nullSafeHasContents(Collections.emptyList()));
      assertFalse(CollectionUtils.nullSafeHasContents(new ArrayList<>()));
      assertFalse(CollectionUtils.nullSafeHasContents((List<?>) null));
      List<String> myList = new ArrayList<>();
      assertFalse(CollectionUtils.nullSafeHasContents(myList));
      myList.add("A");
      assertTrue(CollectionUtils.nullSafeHasContents(myList));
      myList.clear();
      assertFalse(CollectionUtils.nullSafeHasContents(myList));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_nullSafeSize_Map()
   {
      assertEquals(0, CollectionUtils.nullSafeSize(Collections.emptyMap()));
      assertEquals(0, CollectionUtils.nullSafeSize(new HashMap<>()));
      assertEquals(0, CollectionUtils.nullSafeSize((Map<?, ?>) null));
      Map<String, Integer> myMap = new HashMap<>();
      assertEquals(0, CollectionUtils.nullSafeSize(myMap));
      myMap.put("A", 1);
      assertEquals(1, CollectionUtils.nullSafeSize(myMap));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_nullSafeSize_Collection()
   {
      assertEquals(0, CollectionUtils.nullSafeSize(Collections.emptyList()));
      assertEquals(0, CollectionUtils.nullSafeSize(new ArrayList<>()));
      assertEquals(0, CollectionUtils.nullSafeSize((List<?>) null));
      List<String> myList = new ArrayList<>();
      assertEquals(0, CollectionUtils.nullSafeSize(myList));
      myList.add("A");
      assertEquals(1, CollectionUtils.nullSafeSize(myList));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("ConstantConditions")
   @Test
   void test_addAllToMap()
   {
      Map<String, Integer> to   = new HashMap<>();
      Map<String, Integer> from = new HashMap<>();

      assertThrows(NullPointerException.class, () -> CollectionUtils.addAllToMap(null, null));
      assertThrows(NullPointerException.class, () -> CollectionUtils.addAllToMap(to, null));

      // this case does not currently throw - capture that fact here in case we change it.
      CollectionUtils.addAllToMap(null, from);

      CollectionUtils.addAllToMap(to, from);
      assertEquals(0, to.size());

      from.put("A", 1);
      CollectionUtils.addAllToMap(to, from);
      assertEquals(1, to.size());
      assertEquals(1, to.get("A"));

      from.put("B", 2);
      CollectionUtils.addAllToMap(to, from);
      assertEquals(2, to.size());
      assertEquals(1, to.get("A"));
      assertEquals(2, to.get("B"));

      from.put("B", 3);
      CollectionUtils.addAllToMap(to, from);
      assertEquals(2, to.size());
      assertEquals(1, to.get("A"));
      assertEquals(3, to.get("B"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_listToMap()
   {
      assertNull(CollectionUtils.listToMap(null, null));

      List<String> myList = new ArrayList<>();
      myList.add("Apple");
      myList.add("Banana");
      assertThrows(NullPointerException.class, () -> CollectionUtils.listToMap(myList, null));

      Map<String, String> myMap = CollectionUtils.listToMap(myList, first());
      assertEquals(2, myMap.size());
      assertEquals("Apple", myMap.get("A"));
      assertEquals("Banana", myMap.get("B"));

      // confirm what a clobbered key does
      myList.add("Airplane");
      myMap = CollectionUtils.listToMap(myList, first());
      assertEquals(2, myMap.size());
      assertEquals("Airplane", myMap.get("A"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_listToMap_valueFunction()
   {
      assertNull(CollectionUtils.listToMap(null, null, null));

      List<String> myList = new ArrayList<>();
      myList.add("Apple");
      myList.add("Banana");
      assertThrows(NullPointerException.class, () -> CollectionUtils.listToMap(myList, null, null));

      Map<String, String> myMap = CollectionUtils.listToMap(myList, first(), rest());
      assertEquals(2, myMap.size());
      assertEquals("pple", myMap.get("A"));
      assertEquals("anana", myMap.get("B"));

      // confirm what a clobbered key does
      myList.add("Airplane");
      myMap = CollectionUtils.listToMap(myList, first(), rest());
      assertEquals(2, myMap.size());
      assertEquals("irplane", myMap.get("A"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_listToListingHash()
   {
      assertNull(CollectionUtils.listToListingHash(null, null));

      List<String> myList = new ArrayList<>();
      myList.add("Apple");
      myList.add("Banana");
      assertThrows(NullPointerException.class, () -> CollectionUtils.listToListingHash(myList, null));

      ListingHash<String, String> myListingHash = CollectionUtils.listToListingHash(myList, first());
      assertEquals(2, myListingHash.size());
      assertEquals(1, myListingHash.get("A").size());
      assertEquals("Apple", myListingHash.get("A").get(0));
      assertEquals(1, myListingHash.get("B").size());
      assertEquals("Banana", myListingHash.get("B").get(0));

      myList.add("Airplane");
      myListingHash = CollectionUtils.listToListingHash(myList, first());
      assertEquals(2, myListingHash.size());
      assertEquals(2, myListingHash.get("A").size());
      assertEquals("Apple", myListingHash.get("A").get(0));
      assertEquals("Airplane", myListingHash.get("A").get(1));
      assertEquals(1, myListingHash.get("B").size());
      assertEquals("Banana", myListingHash.get("B").get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_listToListingHash_valueFunction()
   {
      assertNull(CollectionUtils.listToListingHash(null, null, null));

      List<String> myList = new ArrayList<>();
      myList.add("Apple");
      myList.add("Banana");
      assertThrows(NullPointerException.class, () -> CollectionUtils.listToListingHash(myList, null, null));

      ListingHash<String, String> myListingHash = CollectionUtils.listToListingHash(myList, first(), rest());
      assertEquals(2, myListingHash.size());
      assertEquals(1, myListingHash.get("A").size());
      assertEquals("pple", myListingHash.get("A").get(0));
      assertEquals(1, myListingHash.get("B").size());
      assertEquals("anana", myListingHash.get("B").get(0));

      myList.add("Airplane");
      myListingHash = CollectionUtils.listToListingHash(myList, first(), rest());
      assertEquals(2, myListingHash.size());
      assertEquals(2, myListingHash.get("A").size());
      assertEquals("pple", myListingHash.get("A").get(0));
      assertEquals("irplane", myListingHash.get("A").get(1));
      assertEquals(1, myListingHash.get("B").size());
      assertEquals("anana", myListingHash.get("B").get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_listTo2LevelMap()
   {
      assertNull(CollectionUtils.listTo2LevelMap(null, null, null));

      List<String> myList = new ArrayList<>();
      myList.add("Apple");
      myList.add("Banana");
      myList.add("Airplane");
      assertThrows(NullPointerException.class, () -> CollectionUtils.listTo2LevelMap(myList, null, null));
      assertThrows(NullPointerException.class, () -> CollectionUtils.listTo2LevelMap(myList, first(), null));
      assertThrows(NullPointerException.class, () -> CollectionUtils.listTo2LevelMap(myList, null, second()));

      Map<String, Map<String, String>> myMap = CollectionUtils.listTo2LevelMap(myList, first(), second());
      assertEquals(2, myMap.size());
      assertEquals(2, myMap.get("A").size());
      assertEquals("Apple", myMap.get("A").get("p"));
      assertEquals("Airplane", myMap.get("A").get("i"));
      assertEquals(1, myMap.get("B").size());
      assertEquals("Banana", myMap.get("B").get("a"));

      // demonstrate clobbering behavior
      myList.add("Ape");
      myMap = CollectionUtils.listTo2LevelMap(myList, first(), second());
      assertEquals(2, myMap.get("A").size());
      assertEquals("Ape", myMap.get("A").get("p"));
   }



   /*******************************************************************************
    ** helper method to get rest of string (unsafely)
    *******************************************************************************/
   private Function<String, String> rest()
   {
      return s -> s.substring(1);
   }



   /*******************************************************************************
    ** helper method to get first char of string (unsafely)
    *******************************************************************************/
   private Function<String, String> first()
   {
      return s -> s.substring(0, 1);
   }



   /*******************************************************************************
    ** helper method to get second char of string (unsafely)
    *******************************************************************************/
   private Function<String, String> second()
   {
      return s -> s.substring(1, 2);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_getPages()
   {
      List<List<Integer>> pages = CollectionUtils.getPages(null, 5);
      assertEquals(0, pages.size());

      pages = CollectionUtils.getPages(Collections.emptyList(), 5);
      assertEquals(0, pages.size());

      pages = CollectionUtils.getPages(List.of(1, 2, 3), 5);
      assertEquals(1, pages.size());
      assertEquals(3, pages.get(0).size());

      pages = CollectionUtils.getPages(List.of(1, 2, 3, 4, 5), 5);
      assertEquals(1, pages.size());
      assertEquals(5, pages.get(0).size());

      pages = CollectionUtils.getPages(List.of(1, 2, 3, 4, 5, 6, 7), 5);
      assertEquals(2, pages.size());
      assertEquals(5, pages.get(0).size());
      assertEquals(2, pages.get(1).size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_getQuestionMarks()
   {
      assertEquals("", CollectionUtils.getQuestionMarks(null));
      assertEquals("", CollectionUtils.getQuestionMarks(Collections.emptyList()));
      assertEquals("?", CollectionUtils.getQuestionMarks(List.of(1)));
      assertEquals("?,?,?", CollectionUtils.getQuestionMarks(List.of(1, 2, 3)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_safelyGetPage()
   {
      List<Integer> empty = Collections.emptyList();
      List<Integer> list  = List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

      /////////////////////
      // null list input //
      /////////////////////
      assertNull(CollectionUtils.safelyGetPage(null, null, null));
      assertNull(CollectionUtils.safelyGetPage(null, 1, 1));
      assertNull(CollectionUtils.safelyGetPage(null, null, 1));
      assertNull(CollectionUtils.safelyGetPage(null, 1, null));

      //////////////////////
      // empty list input //
      //////////////////////
      assertEquals(empty, CollectionUtils.safelyGetPage(empty, null, null));
      assertEquals(empty, CollectionUtils.safelyGetPage(empty, 1, 1));
      assertEquals(empty, CollectionUtils.safelyGetPage(empty, null, 1));
      assertEquals(empty, CollectionUtils.safelyGetPage(empty, 1, null));

      ////////////////////////////////////////
      // cases that give back the full list //
      ////////////////////////////////////////
      assertEquals(list, CollectionUtils.safelyGetPage(list, null, null));
      assertEquals(list, CollectionUtils.safelyGetPage(list, 0, null));

      ///////////////////
      // empty outputs //
      ///////////////////
      assertEquals(empty, CollectionUtils.safelyGetPage(list, 0, 0));
      assertEquals(empty, CollectionUtils.safelyGetPage(list, 10, 1));
      assertEquals(empty, CollectionUtils.safelyGetPage(list, 20, 10));

      ///////////////////////
      // illegal arguments //
      ///////////////////////
      assertThrows(IllegalArgumentException.class, () -> CollectionUtils.safelyGetPage(list, -1, 1));
      assertThrows(IllegalArgumentException.class, () -> CollectionUtils.safelyGetPage(list, 1, -1));
      assertThrows(IllegalArgumentException.class, () -> CollectionUtils.safelyGetPage(list, -1, -1));
      assertThrows(IllegalArgumentException.class, () -> CollectionUtils.safelyGetPage(null, -1, -1));

      /////////////////////////////
      // normal kinds of outputs //
      /////////////////////////////
      assertEquals(List.of(1), CollectionUtils.safelyGetPage(list, null, 1));
      assertEquals(List.of(1), CollectionUtils.safelyGetPage(list, 0, 1));
      assertEquals(List.of(2), CollectionUtils.safelyGetPage(list, 1, 1));
      assertEquals(List.of(2, 3), CollectionUtils.safelyGetPage(list, 1, 2));
      assertEquals(List.of(2, 3, 4), CollectionUtils.safelyGetPage(list, 1, 3));
      assertEquals(List.of(9), CollectionUtils.safelyGetPage(list, 8, 1));
      assertEquals(List.of(9, 10), CollectionUtils.safelyGetPage(list, 8, 2));
      assertEquals(List.of(9, 10), CollectionUtils.safelyGetPage(list, 8, 10));
      assertEquals(List.of(10), CollectionUtils.safelyGetPage(list, 9, 1));
      assertEquals(List.of(10), CollectionUtils.safelyGetPage(list, 9, 2));
      assertEquals(List.of(10), CollectionUtils.safelyGetPage(list, 9, 10));

      /////////////////////////////////////////////////////////
      // make sure scrolling through pages works as expected //
      /////////////////////////////////////////////////////////
      int           skip        = 0;
      int           limit       = 3;
      int           pageCount   = 0;
      List<Integer> accumulator = new ArrayList<>();
      while(true)
      {
         List<Integer> nextPage = CollectionUtils.safelyGetPage(list, skip, limit);
         if(nextPage.isEmpty())
         {
            break;
         }
         accumulator.addAll(nextPage);
         skip += limit;
         pageCount++;
      }
      assertEquals(4, pageCount);
      assertEquals(list, accumulator);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNonNullArray()
   {
      assertArrayEquals(new Integer[] { }, CollectionUtils.nonNullArray(null));
      assertArrayEquals(new Integer[] { }, CollectionUtils.nonNullArray(new Integer[] { }));
      assertArrayEquals(new Integer[] { 1, 2, 3 }, CollectionUtils.nonNullArray(new Integer[] { 1, 2, 3 }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNonNullList()
   {
      assertEquals(List.of(), CollectionUtils.nonNullList(null));
      assertEquals(List.of(), CollectionUtils.nonNullList(List.of()));
      assertEquals(List.of(1, 2, 3), CollectionUtils.nonNullList(List.of(1, 2, 3)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMergeLists()
   {
      assertEquals(List.of(), CollectionUtils.mergeLists());
      assertEquals(List.of(), CollectionUtils.mergeLists((List<Object>) null));
      assertEquals(List.of(), CollectionUtils.mergeLists((List<Object>[]) null));
      assertEquals(List.of(), CollectionUtils.mergeLists(List.of()));
      assertEquals(List.of(1, 2, 3), CollectionUtils.mergeLists(List.of(1, 2, 3)));
      assertEquals(List.of(1, 2, 3), CollectionUtils.mergeLists(List.of(1, 2), List.of(3)));
      assertEquals(List.of(1, 2, 3), CollectionUtils.mergeLists(List.of(1, 2), null, List.of(3)));
      assertEquals(List.of(1, 2, 3), CollectionUtils.mergeLists(null, List.of(1, 2, 3), null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   @Test
   void testUseOrWrap()
   {
      assertNull(CollectionUtils.useOrWrap((Collection<?>) null, TypeToken.get(ArrayList.class)));
      assertNull(CollectionUtils.useOrWrap((Map<?, ?>) null, TypeToken.get(HashMap.class)));

      {
         List<String>      originalList    = new ArrayList<>(List.of("A", "B", "C"));
         ArrayList<String> reallyArrayList = CollectionUtils.useOrWrap(originalList, new TypeToken<>() {});
         assertSame(originalList, reallyArrayList);
      }

      {
         List<String>      originalList    = new LinkedList<>(List.of("A", "B", "C"));
         ArrayList<String> reallyArrayList = CollectionUtils.useOrWrap(originalList, new TypeToken<>() {});
         assertNotSame(originalList, reallyArrayList);
         assertEquals(ArrayList.class, reallyArrayList.getClass());
      }

      assertEquals(ArrayList.class, CollectionUtils.useOrWrap(new LinkedList<>(), TypeToken.get(ArrayList.class)).getClass());

      {
         Map<String, Integer>     originalMap   = new HashMap<>(Map.of("A", 1, "B", 2));
         HashMap<String, Integer> reallyHashMap = CollectionUtils.useOrWrap(originalMap, new TypeToken<>() {});
         assertSame(originalMap, reallyHashMap);
      }

      {
         Map<String, Integer>     originalMap   = new TreeMap<>(Map.of("A", 1, "B", 2));
         HashMap<String, Integer> reallyHashMap = CollectionUtils.useOrWrap(originalMap, new TypeToken<>() {});
         assertNotSame(originalMap, reallyHashMap);
         assertEquals(HashMap.class, reallyHashMap.getClass());
      }

      assertEquals(TreeMap.class, CollectionUtils.useOrWrap(new Hashtable<>(), TypeToken.get(TreeMap.class)).getClass());

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSwapMultiLevelMapKeys()
   {
      Map<String, Map<Integer, String>> input = MapBuilder.of(
         "A", Map.of(1, "A1", 2, "A2", 3, "A3"),
         "B", Map.of(1, "B1", 4, "B4"),
         "C", null);

      Map<Integer, Map<String, String>> output = CollectionUtils.swapMultiLevelMapKeys(input);

      assertEquals(MapBuilder.of(
         1, Map.of("A", "A1", "B", "B1"),
         2, Map.of("A", "A2"),
         3, Map.of("A", "A3"),
         4, Map.of("B", "B4")), output);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAddIfNotNull()
   {
      HashSet<String> s = new HashSet<>();
      CollectionUtils.addIfNotNull(s, null);
      assertEquals(Set.of(), s);

      CollectionUtils.addIfNotNull(s, "");
      assertEquals(Set.of(""), s);

      CollectionUtils.addIfNotNull(s, "1");
      assertEquals(Set.of("", "1"), s);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAddAllIfNotNull()
   {
      BiFunction<Collection<Integer>, Collection<Integer>, Collection<Integer>> doAddAllIfNotNull = (Collection<Integer> destination, Collection<Integer> source) ->
      {
         CollectionUtils.addAllIfNotNull(destination, source);
         return (destination);
      };

      assertThatThrownBy(() -> doAddAllIfNotNull.apply(null, null)).hasMessage("destination may not be null");
      assertThatThrownBy(() -> doAddAllIfNotNull.apply(null, Collections.emptyList())).hasMessage("destination may not be null");
      assertThatThrownBy(() -> doAddAllIfNotNull.apply(null, List.of(1))).hasMessage("destination may not be null");

      assertEquals(List.of(), doAddAllIfNotNull.apply(new ArrayList<>(), null));
      assertEquals(List.of(), doAddAllIfNotNull.apply(new ArrayList<>(), Collections.emptyList()));
      assertEquals(List.of(1), doAddAllIfNotNull.apply(new ArrayList<>(), List.of(1)));

      assertEquals(List.of(1), doAddAllIfNotNull.apply(ListBuilder.of(1), null));
      assertEquals(List.of(1, 2), doAddAllIfNotNull.apply(ListBuilder.of(1), ListBuilder.of(2)));
      assertEquals(List.of(1, 2, 3), doAddAllIfNotNull.apply(ListBuilder.of(1), ListBuilder.of(2, 3)));

      assertEquals(Set.of(1), doAddAllIfNotNull.apply(new HashSet<>(List.of(1)), null));
      assertEquals(Set.of(1, 2), doAddAllIfNotNull.apply(new HashSet<>(List.of(1)), List.of(2)));
      assertEquals(Set.of(1, 2, 3), doAddAllIfNotNull.apply(new HashSet<>(List.of(1)), List.of(2, 3)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testAddAfter()
   {
      ///////////////////////////////////
      // insert after a middle element //
      ///////////////////////////////////
      ArrayList<String> list = new ArrayList<>(List.of("a", "b", "c"));
      CollectionUtils.addAfter(list, "b", "x");
      assertEquals(List.of("a", "b", "x", "c"), list);

      ///////////////////////////////////
      // insert after the last element //
      ///////////////////////////////////
      list = new ArrayList<>(List.of("a", "b", "c"));
      CollectionUtils.addAfter(list, "c", "z");
      assertEquals(List.of("a", "b", "c", "z"), list);

      ////////////////////////////////////
      // insert after the first element //
      ////////////////////////////////////
      list = new ArrayList<>(List.of("a", "b", "c"));
      CollectionUtils.addAfter(list, "a", "y");
      assertEquals(List.of("a", "y", "b", "c"), list);

      ///////////////////////////////////////////////
      // target not found throws IllegalArgumentException //
      ///////////////////////////////////////////////
      ArrayList<String> list2 = new ArrayList<>(List.of("a", "b"));
      assertThatThrownBy(() -> CollectionUtils.addAfter(list2, "x", "y"))
         .isInstanceOf(IllegalArgumentException.class)
         .hasMessage("Target element not found");
   }

}
