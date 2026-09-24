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


import java.math.BigDecimal;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for LongAggregates
 *******************************************************************************/
class LongAggregatesTest
{

   private static final Offset<Double> TOLERANCE = Offset.offset(0.001);


   /*******************************************************************************
    ** Empty aggregator — everything should be null / count=0.
    *******************************************************************************/
   @Test
   void testEmptyAggregator_allNulls()
   {
      LongAggregates agg = new LongAggregates();

      assertEquals(0, agg.getCount());
      assertNull(agg.getSum());
      assertNull(agg.getMin());
      assertNull(agg.getMax());
      assertNull(agg.getProduct());
      assertNull(agg.getAverage());
      assertNull(agg.getVariance());
      assertNull(agg.getVarP());
      assertNull(agg.getStandardDeviation());
      assertNull(agg.getStdDevP());
   }



   /*******************************************************************************
    ** Null inputs must be silently skipped (count stays 0).
    *******************************************************************************/
   @Test
   void testAdd_nullInputs_ignored()
   {
      LongAggregates agg = new LongAggregates();
      agg.add(null);
      agg.add(null);

      assertEquals(0, agg.getCount());
      assertNull(agg.getSum());
   }



   /*******************************************************************************
    ** Single positive value — count, sum, min, max, product all equal that value.
    *******************************************************************************/
   @Test
   void testAdd_singleValue_allSummariesEqualValue()
   {
      LongAggregates agg = new LongAggregates();
      agg.add(10L);

      assertEquals(1, agg.getCount());
      assertEquals(Long.valueOf(10L), agg.getSum());
      assertEquals(Long.valueOf(10L), agg.getMin());
      assertEquals(Long.valueOf(10L), agg.getMax());
      assertEquals(new BigDecimal("10"), agg.getProduct());
      assertNull(agg.getVariance());
   }



   /*******************************************************************************
    ** Multiple values — verify sum, min, max, product, average.
    *******************************************************************************/
   @Test
   void testAdd_multipleValues_correctSummaries()
   {
      LongAggregates agg = new LongAggregates();
      agg.add(2L);
      agg.add(4L);
      agg.add(6L);

      assertEquals(3, agg.getCount());
      assertEquals(Long.valueOf(12L), agg.getSum());
      assertEquals(Long.valueOf(2L), agg.getMin());
      assertEquals(Long.valueOf(6L), agg.getMax());
      assertEquals(new BigDecimal("48"), agg.getProduct()); // 2*4*6
      assertThat(agg.getAverage().doubleValue()).isCloseTo(4.0, TOLERANCE);
   }



   /*******************************************************************************
    ** Mixed nulls and values — nulls must not corrupt count or summaries.
    *******************************************************************************/
   @Test
   void testAdd_mixedNullsAndValues_nullsIgnored()
   {
      LongAggregates agg = new LongAggregates();
      agg.add(null);
      agg.add(100L);
      agg.add(null);
      agg.add(200L);

      assertEquals(2, agg.getCount());
      assertEquals(Long.valueOf(300L), agg.getSum());
      assertEquals(Long.valueOf(100L), agg.getMin());
      assertEquals(Long.valueOf(200L), agg.getMax());
   }



   /*******************************************************************************
    ** Negative values — min/max must still be correct.
    *******************************************************************************/
   @Test
   void testAdd_negativeValues_minMaxCorrect()
   {
      LongAggregates agg = new LongAggregates();
      agg.add(-30L);
      agg.add(-10L);
      agg.add(20L);

      assertEquals(Long.valueOf(-30L), agg.getMin());
      assertEquals(Long.valueOf(20L), agg.getMax());
      assertEquals(Long.valueOf(-20L), agg.getSum());
   }



   /*******************************************************************************
    ** Variance should be non-null once two or more values have been added.
    *******************************************************************************/
   @Test
   void testVariance_twoValues_nonNull()
   {
      LongAggregates agg = new LongAggregates();
      agg.add(10L);
      agg.add(20L);

      assertThat(agg.getVariance()).isNotNull();
      assertThat(agg.getVarP()).isNotNull();
      assertThat(agg.getStandardDeviation()).isNotNull();
      assertThat(agg.getStdDevP()).isNotNull();
   }



   /*******************************************************************************
    ** Variance of identical values should be zero.
    *******************************************************************************/
   @Test
   void testVariance_identicalValues_zero()
   {
      LongAggregates agg = new LongAggregates();
      agg.add(5L);
      agg.add(5L);
      agg.add(5L);

      assertThat(agg.getVariance().doubleValue()).isCloseTo(0.0, Offset.offset(0.0001));
      assertThat(agg.getStandardDeviation().doubleValue()).isCloseTo(0.0, Offset.offset(0.0001));
   }



   /*******************************************************************************
    ** Product uses BigDecimal, so large-value products remain exact.
    *******************************************************************************/
   @Test
   void testProduct_largeValues_bigDecimalExact()
   {
      LongAggregates agg = new LongAggregates();
      agg.add(Long.MAX_VALUE);
      agg.add(1L);

      assertThat(agg.getProduct()).isNotNull();
      assertThat(agg.getProduct().longValue()).isEqualTo(Long.MAX_VALUE);
   }

}
