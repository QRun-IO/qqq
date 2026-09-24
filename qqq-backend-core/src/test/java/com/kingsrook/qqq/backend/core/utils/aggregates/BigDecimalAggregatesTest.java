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
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for BigDecimalAggregates
 *******************************************************************************/
class BigDecimalAggregatesTest
{

   /*******************************************************************************
    ** Empty aggregator should have count=0 and all summary values null.
    *******************************************************************************/
   @Test
   void testEmptyAggregator_allNulls()
   {
      BigDecimalAggregates agg = new BigDecimalAggregates();

      assertEquals(0, agg.getCount());
      assertNull(agg.getSum());
      assertNull(agg.getMin());
      assertNull(agg.getMax());
      assertNull(agg.getProduct());
      assertNull(agg.getAverage());
      // variance requires at least 2 values
      assertNull(agg.getVariance());
      assertNull(agg.getVarP());
      assertNull(agg.getStandardDeviation());
      assertNull(agg.getStdDevP());
   }



   /*******************************************************************************
    ** Null inputs should be silently skipped (count stays 0).
    *******************************************************************************/
   @Test
   void testAdd_nullInput_ignored()
   {
      BigDecimalAggregates agg = new BigDecimalAggregates();
      agg.add(null);
      agg.add(null);

      assertEquals(0, agg.getCount());
      assertNull(agg.getSum());
   }



   /*******************************************************************************
    ** Single value — count, sum, min, max, product all equal that value.
    *******************************************************************************/
   @Test
   void testAdd_singleValue_allSummariesEqualValue()
   {
      BigDecimalAggregates agg = new BigDecimalAggregates();
      agg.add(new BigDecimal("7.5"));

      assertEquals(1, agg.getCount());
      assertEquals(new BigDecimal("7.5"), agg.getSum());
      assertEquals(new BigDecimal("7.5"), agg.getMin());
      assertEquals(new BigDecimal("7.5"), agg.getMax());
      assertEquals(new BigDecimal("7.5"), agg.getProduct());
      // variance undefined for single value
      assertNull(agg.getVariance());
   }



   /*******************************************************************************
    ** Multiple values — verify sum, min, max, product, and average.
    *******************************************************************************/
   @Test
   void testAdd_multipleValues_correctSummaries()
   {
      BigDecimalAggregates agg = new BigDecimalAggregates();
      agg.add(new BigDecimal("2"));
      agg.add(new BigDecimal("4"));
      agg.add(new BigDecimal("6"));

      assertEquals(3, agg.getCount());
      assertEquals(new BigDecimal("12"), agg.getSum());
      assertEquals(new BigDecimal("2"), agg.getMin());
      assertEquals(new BigDecimal("6"), agg.getMax());
      assertEquals(new BigDecimal("48"), agg.getProduct());  // 2*4*6

      // average = 12/3 = 4.0
      assertThat(agg.getAverage().doubleValue()).isCloseTo(4.0, org.assertj.core.data.Offset.offset(0.001));
   }



   /*******************************************************************************
    ** Mixed nulls and values — nulls must not corrupt count or summaries.
    *******************************************************************************/
   @Test
   void testAdd_mixedNullsAndValues_nullsIgnored()
   {
      BigDecimalAggregates agg = new BigDecimalAggregates();
      agg.add(null);
      agg.add(new BigDecimal("10"));
      agg.add(null);
      agg.add(new BigDecimal("20"));

      assertEquals(2, agg.getCount());
      assertEquals(new BigDecimal("30"), agg.getSum());
      assertEquals(new BigDecimal("10"), agg.getMin());
      assertEquals(new BigDecimal("20"), agg.getMax());
   }



   /*******************************************************************************
    ** Variance should be non-null once two or more values have been added.
    *******************************************************************************/
   @Test
   void testVariance_twoValues_nonNull()
   {
      BigDecimalAggregates agg = new BigDecimalAggregates();
      agg.add(new BigDecimal("2"));
      agg.add(new BigDecimal("4"));

      assertThat(agg.getVariance()).isNotNull();
      assertThat(agg.getVarP()).isNotNull();
      assertThat(agg.getStandardDeviation()).isNotNull();
      assertThat(agg.getStdDevP()).isNotNull();
   }



   /*******************************************************************************
    ** Variance of identical values should be zero (no spread).
    *******************************************************************************/
   @Test
   void testVariance_identicalValues_zero()
   {
      BigDecimalAggregates agg = new BigDecimalAggregates();
      agg.add(new BigDecimal("5"));
      agg.add(new BigDecimal("5"));
      agg.add(new BigDecimal("5"));

      assertThat(agg.getVariance().doubleValue()).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.0001));
      assertThat(agg.getStandardDeviation().doubleValue()).isCloseTo(0.0, org.assertj.core.data.Offset.offset(0.0001));
   }



   /*******************************************************************************
    ** Negative values — min/max must still be correct.
    *******************************************************************************/
   @Test
   void testAdd_negativeValues_minMaxCorrect()
   {
      BigDecimalAggregates agg = new BigDecimalAggregates();
      agg.add(new BigDecimal("-3"));
      agg.add(new BigDecimal("-1"));
      agg.add(new BigDecimal("2"));

      assertEquals(new BigDecimal("-3"), agg.getMin());
      assertEquals(new BigDecimal("2"), agg.getMax());
      // sum = -3 + -1 + 2 = -2
      assertEquals(new BigDecimal("-2"), agg.getSum());
   }

}
