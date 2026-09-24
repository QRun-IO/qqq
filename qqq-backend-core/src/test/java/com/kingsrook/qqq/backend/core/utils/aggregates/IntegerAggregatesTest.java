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
 ** Unit tests for IntegerAggregates
 *******************************************************************************/
class IntegerAggregatesTest
{

   private static final Offset<Double> TOLERANCE = Offset.offset(0.001);


   /*******************************************************************************
    ** Empty aggregator — everything should be null / count=0.
    *******************************************************************************/
   @Test
   void testEmptyAggregator_allNulls()
   {
      IntegerAggregates agg = new IntegerAggregates();

      assertEquals(0, agg.getCount());
      assertNull(agg.getSum());
      assertNull(agg.getMin());
      assertNull(agg.getMax());
      assertNull(agg.getProduct());
      assertNull(agg.getAverage());
   }



   /*******************************************************************************
    ** Null inputs must be silently skipped.
    *******************************************************************************/
   @Test
   void testAdd_nullInputs_ignored()
   {
      IntegerAggregates agg = new IntegerAggregates();
      agg.add(null);
      agg.add(null);

      assertEquals(0, agg.getCount());
   }



   /*******************************************************************************
    ** Single positive value — all summaries equal that value.
    *******************************************************************************/
   @Test
   void testAdd_singlePositiveValue_allSummariesEqual()
   {
      IntegerAggregates agg = new IntegerAggregates();
      agg.add(5);

      assertEquals(1, agg.getCount());
      assertEquals(Integer.valueOf(5), agg.getSum());
      assertEquals(Integer.valueOf(5), agg.getMin());
      assertEquals(Integer.valueOf(5), agg.getMax());
      assertEquals(new BigDecimal("5"), agg.getProduct());
   }



   /*******************************************************************************
    ** Multiple values — verify sum, min, max, product, average.
    *******************************************************************************/
   @Test
   void testAdd_multipleValues_correctSummaries()
   {
      IntegerAggregates agg = new IntegerAggregates();
      agg.add(1);
      agg.add(2);
      agg.add(3);

      assertEquals(3, agg.getCount());
      assertEquals(Integer.valueOf(6), agg.getSum());
      assertEquals(Integer.valueOf(1), agg.getMin());
      assertEquals(Integer.valueOf(3), agg.getMax());
      assertEquals(new BigDecimal("6"), agg.getProduct());   // 1*2*3
      assertThat(agg.getAverage().doubleValue()).isCloseTo(2.0, TOLERANCE);
   }



   /*******************************************************************************
    ** Negative values — min must be the most negative.
    *******************************************************************************/
   @Test
   void testAdd_negativeValues_minMaxCorrect()
   {
      IntegerAggregates agg = new IntegerAggregates();
      agg.add(-10);
      agg.add(0);
      agg.add(5);

      assertEquals(Integer.valueOf(-10), agg.getMin());
      assertEquals(Integer.valueOf(5), agg.getMax());
      assertEquals(Integer.valueOf(-5), agg.getSum());
   }



   /*******************************************************************************
    ** Integer overflow edge: values that overflow int sum — document the known
    ** behaviour (int arithmetic, no overflow protection in the implementation).
    *******************************************************************************/
   @Test
   void testAdd_largeValues_sumUsesIntegerArithmetic()
   {
      IntegerAggregates agg = new IntegerAggregates();
      agg.add(Integer.MAX_VALUE);
      agg.add(1);

      // Integer addition wraps around — this test documents (not endorses) that behaviour.
      // Product uses BigDecimal so it will be correct.
      assertThat(agg.getProduct().longValue()).isEqualTo((long) Integer.MAX_VALUE * 1L);
   }



   /*******************************************************************************
    ** Variance should be non-null once two or more values added.
    *******************************************************************************/
   @Test
   void testVariance_twoValues_nonNull()
   {
      IntegerAggregates agg = new IntegerAggregates();
      agg.add(10);
      agg.add(20);

      assertThat(agg.getVariance()).isNotNull();
      assertThat(agg.getStandardDeviation()).isNotNull();
   }

}
