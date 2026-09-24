/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.utils.aggregates;


import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for StringAggregates
 **
 ** Note: sum and average return null by design (semantically undefined for strings).
 *******************************************************************************/
class StringAggregatesTest
{

   /*******************************************************************************
    ** Empty aggregator — count=0 and all accessors return null.
    *******************************************************************************/
   @Test
   void testEmptyAggregator_allNulls()
   {
      StringAggregates agg = new StringAggregates();

      assertEquals(0, agg.getCount());
      assertNull(agg.getMin());
      assertNull(agg.getMax());
      assertNull(agg.getSum());     // by design: undefined for strings
      assertNull(agg.getAverage()); // by design: undefined for strings
   }



   /*******************************************************************************
    ** Null inputs must be silently skipped (count stays 0).
    *******************************************************************************/
   @Test
   void testAdd_nullInputs_ignored()
   {
      StringAggregates agg = new StringAggregates();
      agg.add(null);
      agg.add(null);

      assertEquals(0, agg.getCount());
      assertNull(agg.getMin());
   }



   /*******************************************************************************
    ** Single value — min and max both equal that value.
    *******************************************************************************/
   @Test
   void testAdd_singleValue_minMaxEqualValue()
   {
      StringAggregates agg = new StringAggregates();
      agg.add("hello");

      assertEquals(1, agg.getCount());
      assertEquals("hello", agg.getMin());
      assertEquals("hello", agg.getMax());
   }



   /*******************************************************************************
    ** Lexicographic ordering — min is the earliest string alphabetically.
    *******************************************************************************/
   @Test
   void testAdd_multipleValues_correctMinMax()
   {
      StringAggregates agg = new StringAggregates();
      agg.add("banana");
      agg.add("apple");
      agg.add("cherry");

      assertEquals(3, agg.getCount());
      assertEquals("apple", agg.getMin());
      assertEquals("cherry", agg.getMax());
   }



   /*******************************************************************************
    ** Mixed nulls and non-null values — nulls must not corrupt count or min/max.
    *******************************************************************************/
   @Test
   void testAdd_mixedNullsAndValues_nullsIgnored()
   {
      StringAggregates agg = new StringAggregates();
      agg.add(null);
      agg.add("zebra");
      agg.add(null);
      agg.add("ant");

      assertEquals(2, agg.getCount());
      assertEquals("ant", agg.getMin());
      assertEquals("zebra", agg.getMax());
   }



   /*******************************************************************************
    ** Empty string is a valid value and is less than any non-empty string.
    *******************************************************************************/
   @Test
   void testAdd_emptyString_treatedAsLowest()
   {
      StringAggregates agg = new StringAggregates();
      agg.add("alpha");
      agg.add("");

      assertEquals(2, agg.getCount());
      assertEquals("", agg.getMin());
      assertEquals("alpha", agg.getMax());
   }



   /*******************************************************************************
    ** Sum is always null — defined in the interface contract for strings.
    *******************************************************************************/
   @Test
   void testGetSum_afterAdding_alwaysNull()
   {
      StringAggregates agg = new StringAggregates();
      agg.add("a");
      agg.add("b");

      assertNull(agg.getSum());
   }



   /*******************************************************************************
    ** Average is always null — defined in the interface contract for strings.
    *******************************************************************************/
   @Test
   void testGetAverage_afterAdding_alwaysNull()
   {
      StringAggregates agg = new StringAggregates();
      agg.add("hello");

      assertNull(agg.getAverage());
   }



   /*******************************************************************************
    ** Case-sensitive: uppercase letters sort before lowercase in standard
    ** Java String.compareTo (ASCII/Unicode order).
    *******************************************************************************/
   @Test
   void testAdd_caseSensitiveOrdering_uppercaseFirst()
   {
      StringAggregates agg = new StringAggregates();
      agg.add("Banana");
      agg.add("apple");

      assertThat(agg.getMin()).isEqualTo("Banana"); // 'B' < 'a' in Unicode
      assertThat(agg.getMax()).isEqualTo("apple");
   }

}
