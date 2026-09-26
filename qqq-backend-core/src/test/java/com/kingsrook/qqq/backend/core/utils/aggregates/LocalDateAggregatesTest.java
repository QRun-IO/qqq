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

package com.kingsrook.qqq.backend.core.utils.aggregates;


import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for LocalDateAggregates
 **
 ** Note: sum() returns null by design (semantically undefined for dates).
 ** Average is computed as the mean of epoch-day values.
 *******************************************************************************/
class LocalDateAggregatesTest
{

   private static final LocalDate D1 = LocalDate.of(2024, 1, 1);
   private static final LocalDate D2 = LocalDate.of(2024, 7, 1);
   private static final LocalDate D3 = LocalDate.of(2024, 12, 31);


   /*******************************************************************************
    ** Empty aggregator — count=0 and all accessors return null.
    *******************************************************************************/
   @Test
   void testEmptyAggregator_allNulls()
   {
      LocalDateAggregates agg = new LocalDateAggregates();

      assertEquals(0, agg.getCount());
      assertNull(agg.getMin());
      assertNull(agg.getMax());
      assertNull(agg.getSum());     // by design: undefined for dates
      assertNull(agg.getAverage()); // returns null when count=0
   }



   /*******************************************************************************
    ** Null inputs must be silently skipped (count stays 0).
    *******************************************************************************/
   @Test
   void testAdd_nullInputs_ignored()
   {
      LocalDateAggregates agg = new LocalDateAggregates();
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
      LocalDateAggregates agg = new LocalDateAggregates();
      agg.add(D1);

      assertEquals(1, agg.getCount());
      assertEquals(D1, agg.getMin());
      assertEquals(D1, agg.getMax());
   }



   /*******************************************************************************
    ** Multiple values — correct min, max, count, and average within range.
    *******************************************************************************/
   @Test
   void testAdd_multipleValues_correctMinMaxAverage()
   {
      LocalDateAggregates agg = new LocalDateAggregates();
      agg.add(D1);
      agg.add(D2);
      agg.add(D3);

      assertEquals(3, agg.getCount());
      assertEquals(D1, agg.getMin());
      assertEquals(D3, agg.getMax());

      LocalDate avg = agg.getAverage();
      assertThat(avg).isNotNull();
      // Average must lie between min and max
      assertThat(avg).isAfterOrEqualTo(D1).isBeforeOrEqualTo(D3);
   }



   /*******************************************************************************
    ** Two identical dates — average equals both.
    *******************************************************************************/
   @Test
   void testAverage_identicalValues_equalsValue()
   {
      LocalDateAggregates agg = new LocalDateAggregates();
      agg.add(D2);
      agg.add(D2);

      assertThat(agg.getAverage()).isEqualTo(D2);
   }



   /*******************************************************************************
    ** Mixed nulls and values — nulls must not corrupt count or min/max.
    *******************************************************************************/
   @Test
   void testAdd_mixedNullsAndValues_nullsIgnored()
   {
      LocalDateAggregates agg = new LocalDateAggregates();
      agg.add(null);
      agg.add(D1);
      agg.add(null);
      agg.add(D3);

      assertEquals(2, agg.getCount());
      assertEquals(D1, agg.getMin());
      assertEquals(D3, agg.getMax());
   }



   /*******************************************************************************
    ** Sum is always null — defined in the interface contract for dates.
    *******************************************************************************/
   @Test
   void testGetSum_afterAdding_alwaysNull()
   {
      LocalDateAggregates agg = new LocalDateAggregates();
      agg.add(D1);

      assertNull(agg.getSum());
   }



   /*******************************************************************************
    ** Pre-epoch dates (negative epoch day) are valid values.
    *******************************************************************************/
   @Test
   void testAdd_preEpochDate_validValue()
   {
      LocalDate preEpoch = LocalDate.of(1960, 6, 15);

      LocalDateAggregates agg = new LocalDateAggregates();
      agg.add(preEpoch);
      agg.add(D1);

      assertEquals(2, agg.getCount());
      assertEquals(preEpoch, agg.getMin());
      assertEquals(D1, agg.getMax());
   }

}
