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


import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ListingHash
 **
 *******************************************************************************/
class ListingHashTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   private ListingHash<String, String> makeDefaultTestListingHash()
   {
      ListingHash<String, String> listingHash = new ListingHash<>();
      listingHash.add("A", "Apple");
      listingHash.add("A", "Acorn");
      listingHash.add("B", "Ball");
      return listingHash;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_add()
   {
      ListingHash<String, String> listingHash = makeDefaultTestListingHash();
      assertEquals(2, listingHash.size());
      assertEquals(2, listingHash.get("A").size());
      assertEquals(1, listingHash.get("B").size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_addAllCollectionKeyFunction()
   {
      ListingHash<String, String> listingHash = new ListingHash<>();
      listingHash.addAll(Set.of("Apple", "Acorn", "Ball"), (s -> s.substring(0, 1)));
      assertEquals(2, listingHash.size());
      assertEquals(2, listingHash.get("A").size());
      assertEquals(1, listingHash.get("B").size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_addAllListingHash()
   {
      ListingHash<String, String> source = makeDefaultTestListingHash();

      ListingHash<String, String> listingHash = new ListingHash<>();
      source.add("A", "Arrow");
      source.add("C", "Car");
      listingHash.addAll(source);

      assertEquals(3, listingHash.size());
      assertEquals(3, listingHash.get("A").size());
      assertEquals(1, listingHash.get("B").size());
      assertEquals(1, listingHash.get("C").size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_addAllKeyCollection()
   {
      ListingHash<String, String> listingHash = new ListingHash<>();
      listingHash.add("A", "Arrow");
      listingHash.addAll("A", Set.of("Apple", "Acorn"));
      listingHash.addAll("B", Set.of("Ball"));
      assertEquals(2, listingHash.size());
      assertEquals(3, listingHash.get("A").size());
      assertEquals(1, listingHash.get("B").size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_clear()
   {
      ListingHash<String, String> listingHash = makeDefaultTestListingHash();
      listingHash.clear();
      assertEquals(0, listingHash.size());
      assertFalse(listingHash.containsKey("A"));
      assertFalse(listingHash.containsKey("B"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_containsKey()
   {
      ListingHash<String, String> listingHash = makeDefaultTestListingHash();
      assertTrue(listingHash.containsKey("A"));
      assertTrue(listingHash.containsKey("B"));
      assertFalse(listingHash.containsKey("C"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_containsValue()
   {
      ListingHash<String, String> listingHash = makeDefaultTestListingHash();
      assertTrue(listingHash.containsValue(List.of("Apple", "Acorn")));
      assertFalse(listingHash.containsValue("Apple"));
      assertFalse(listingHash.containsValue(List.of("Apple")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_entrySet()
   {
      ListingHash<String, String>          listingHash = makeDefaultTestListingHash();
      Set<Map.Entry<String, List<String>>> entrySet    = listingHash.entrySet();
      assertEquals(2, entrySet.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings({ "SimplifiableAssertion", "EqualsWithItself" })
   @Test
   void test_equals()
   {
      ListingHash<String, String> listingHash1 = makeDefaultTestListingHash();
      ListingHash<String, String> listingHash2 = makeDefaultTestListingHash();
      assertTrue(listingHash1.equals(listingHash2));
      assertTrue(listingHash1.equals(listingHash1));
      assertFalse(listingHash1.equals(new ListingHash<>()));
      assertFalse(new ListingHash<>().equals(listingHash1));
      listingHash2.add("C", "Car");
      assertFalse(listingHash1.equals(listingHash2));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_get()
   {
      ListingHash<String, String> listingHash = makeDefaultTestListingHash();
      List<String>                aList       = listingHash.get("A");
      assertEquals(2, aList.size());
      assertEquals("Apple", aList.get(0));
      assertEquals("Acorn", aList.get(1));
      List<String> bList = listingHash.get("B");
      assertEquals(1, bList.size());
      assertEquals("Ball", bList.get(0));
      assertNull(listingHash.get("C"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_hashCode()
   {
      ListingHash<String, String> listingHash1 = makeDefaultTestListingHash();
      ListingHash<String, String> listingHash2 = makeDefaultTestListingHash();
      assertEquals(listingHash1.hashCode(), listingHash2.hashCode());
      assertEquals(listingHash1.hashCode(), listingHash1.hashCode());
      assertNotEquals(listingHash1.hashCode(), new ListingHash<>().hashCode());
      listingHash2.add("C", "Car");
      assertNotEquals(listingHash1.hashCode(), listingHash2.hashCode());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_isEmpty()
   {
      assertTrue(new ListingHash<>().isEmpty());
      ListingHash<String, String> listingHash = makeDefaultTestListingHash();
      assertFalse(listingHash.isEmpty());
      listingHash.clear();
      assertTrue(listingHash.isEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_keySet()
   {
      ListingHash<String, String> listingHash = makeDefaultTestListingHash();
      Set<String>                 keySet      = listingHash.keySet();
      assertEquals(2, keySet.size());
      assertTrue(keySet.contains("A"));
      assertTrue(keySet.contains("B"));
      assertFalse(keySet.contains("C"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_put()
   {
      ListingHash<String, String> listingHash = makeDefaultTestListingHash();
      listingHash.put("A", List.of("Android"));
      assertEquals(2, listingHash.size());
      assertEquals(1, listingHash.get("A").size());
      assertEquals("Android", listingHash.get("A").get(0));
      listingHash.put("C", List.of("Car"));
      assertEquals(3, listingHash.size());
      assertEquals(1, listingHash.get("C").size());
      assertEquals("Car", listingHash.get("C").get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_putAll()
   {
      ListingHash<String, String> listingHash = makeDefaultTestListingHash();
      listingHash.putAll(Map.of("C", List.of("Car"), "D", List.of("Dog", "Door")));
      assertEquals(4, listingHash.size());
      assertEquals(2, listingHash.get("A").size());
      assertEquals("Apple", listingHash.get("A").get(0));
      assertEquals("Acorn", listingHash.get("A").get(1));
      assertEquals(1, listingHash.get("C").size());
      assertEquals("Car", listingHash.get("C").get(0));
      assertEquals(2, listingHash.get("D").size());
      assertEquals("Dog", listingHash.get("D").get(0));
      assertEquals("Door", listingHash.get("D").get(1));

      listingHash.putAll(Map.of("A", List.of("Android")));
      assertEquals(4, listingHash.size());
      assertEquals(1, listingHash.get("A").size());
      assertEquals("Android", listingHash.get("A").get(0));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_remove()
   {
      ListingHash<String, String> listingHash = makeDefaultTestListingHash();
      listingHash.remove("A");
      assertEquals(1, listingHash.size());
      assertFalse(listingHash.containsKey("A"));
      listingHash.remove("B");
      assertEquals(0, listingHash.size());
      listingHash.remove("B");
      listingHash.remove("C");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_size()
   {
      ListingHash<String, String> listingHash = makeDefaultTestListingHash();
      assertEquals(2, listingHash.size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test_values()
   {
      ListingHash<String, String> listingHash = makeDefaultTestListingHash();
      Collection<List<String>>    values      = listingHash.values();
      assertEquals(2, values.size());
      assertTrue(values.contains(List.of("Apple", "Acorn")));
      assertTrue(values.contains(List.of("Ball")));
   }
}
