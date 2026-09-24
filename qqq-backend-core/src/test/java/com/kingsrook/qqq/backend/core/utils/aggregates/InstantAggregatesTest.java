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


import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for InstantAggregates
 **
 ** Note: sum() returns null by design (semantically undefined for timestamps).
 ** Average is computed as the mean of epoch-millisecond values.
 *******************************************************************************/
class InstantAggregatesTest
{

   private static final Instant T1 = Instant.parse("2024-01-01T00:00:00Z");
   private static final Instant T2 = Instant.parse("2024-06-01T00:00:00Z");
   private static final Instant T3 = Instant.parse("2024-12-31T00:00:00Z");


   /*******************************************************************************
    ** Empty aggregator — count=0 and all accessors return null.
    *******************************************************************************/
   @Test
   void testEmptyAggregator_allNulls()
   {
      InstantAggregates agg = new InstantAggregates();

      assertEquals(0, agg.getCount());
      assertNull(agg.getMin());
      assertNull(agg.getMax());
      assertNull(agg.getSum());     // by design: undefined for timestamps
      assertNull(agg.getAverage()); // returns null when count=0
   }



   /*******************************************************************************
    ** Null inputs must be silently skipped (count stays 0).
    *******************************************************************************/
   @Test
   void testAdd_nullInputs_ignored()
   {
      InstantAggregates agg = new InstantAggregates();
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
      InstantAggregates agg = new InstantAggregates();
      agg.add(T1);

      assertEquals(1, agg.getCount());
      assertEquals(T1, agg.getMin());
      assertEquals(T1, agg.getMax());
   }



   /*******************************************************************************
    ** Multiple values — correct min, max, count, and average.
    *******************************************************************************/
   @Test
   void testAdd_multipleValues_correctMinMaxAverage()
   {
      InstantAggregates agg = new InstantAggregates();
      agg.add(T1);
      agg.add(T2);
      agg.add(T3);

      assertEquals(3, agg.getCount());
      assertEquals(T1, agg.getMin());
      assertEquals(T3, agg.getMax());

      Instant avg = agg.getAverage();
      assertThat(avg).isNotNull();
      // Average must lie between min and max
      assertThat(avg).isAfter(T1).isBefore(T3);
   }



   /*******************************************************************************
    ** Mixed nulls and values — nulls must not corrupt count or min/max.
    *******************************************************************************/
   @Test
   void testAdd_mixedNullsAndValues_nullsIgnored()
   {
      InstantAggregates agg = new InstantAggregates();
      agg.add(null);
      agg.add(T1);
      agg.add(null);
      agg.add(T3);

      assertEquals(2, agg.getCount());
      assertEquals(T1, agg.getMin());
      assertEquals(T3, agg.getMax());
   }



   /*******************************************************************************
    ** Two identical instants — average equals both.
    *******************************************************************************/
   @Test
   void testAverage_identicalValues_equalsValue()
   {
      InstantAggregates agg = new InstantAggregates();
      agg.add(T2);
      agg.add(T2);

      assertThat(agg.getAverage()).isEqualTo(T2);
   }



   /*******************************************************************************
    ** Sum is always null — defined in the interface contract for timestamps.
    *******************************************************************************/
   @Test
   void testGetSum_afterAdding_alwaysNull()
   {
      InstantAggregates agg = new InstantAggregates();
      agg.add(T1);

      assertNull(agg.getSum());
   }



   /*******************************************************************************
    ** Epoch-zero instant is a valid value — treated as the minimum possible.
    *******************************************************************************/
   @Test
   void testAdd_epochZero_validValue()
   {
      InstantAggregates agg = new InstantAggregates();
      agg.add(Instant.EPOCH);
      agg.add(T1);

      assertEquals(2, agg.getCount());
      assertEquals(Instant.EPOCH, agg.getMin());
      assertEquals(T1, agg.getMax());
   }

}
