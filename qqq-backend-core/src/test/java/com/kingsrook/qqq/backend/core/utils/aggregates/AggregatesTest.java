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

package com.kingsrook.qqq.backend.core.utils.aggregates;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit test for Aggregates
 *******************************************************************************/
class AggregatesTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInteger()
   {
      IntegerAggregates aggregates = new IntegerAggregates();

      assertEquals(0, aggregates.getCount());
      assertNull(aggregates.getMin());
      assertNull(aggregates.getMax());
      assertNull(aggregates.getSum());
      assertNull(aggregates.getAverage());

      aggregates.add(5);
      assertEquals(1, aggregates.getCount());
      assertEquals(5, aggregates.getMin());
      assertEquals(5, aggregates.getMax());
      assertEquals(5, aggregates.getSum());
      assertThat(aggregates.getAverage()).isCloseTo(new BigDecimal("5"), Offset.offset(BigDecimal.ZERO));

      aggregates.add(10);
      assertEquals(2, aggregates.getCount());
      assertEquals(5, aggregates.getMin());
      assertEquals(10, aggregates.getMax());
      assertEquals(15, aggregates.getSum());
      assertThat(aggregates.getAverage()).isCloseTo(new BigDecimal("7.5"), Offset.offset(BigDecimal.ZERO));

      aggregates.add(15);
      assertEquals(3, aggregates.getCount());
      assertEquals(5, aggregates.getMin());
      assertEquals(15, aggregates.getMax());
      assertEquals(30, aggregates.getSum());
      assertThat(aggregates.getAverage()).isCloseTo(new BigDecimal("10"), Offset.offset(BigDecimal.ZERO));

      aggregates.add(null);
      assertEquals(3, aggregates.getCount());
      assertEquals(5, aggregates.getMin());
      assertEquals(15, aggregates.getMax());
      assertEquals(30, aggregates.getSum());
      assertThat(aggregates.getAverage()).isCloseTo(new BigDecimal("10"), Offset.offset(BigDecimal.ZERO));

      assertEquals(new BigDecimal("750"), aggregates.getProduct());
      assertEquals(new BigDecimal("25.0000"), aggregates.getVariance());
      assertEquals(new BigDecimal("5.0000"), aggregates.getStandardDeviation());
      assertThat(aggregates.getVarP()).isCloseTo(new BigDecimal("16.6667"), Offset.offset(new BigDecimal(".0001")));
      assertThat(aggregates.getStdDevP()).isCloseTo(new BigDecimal("4.0824"), Offset.offset(new BigDecimal(".0001")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLong()
   {
      LongAggregates aggregates = new LongAggregates();

      assertEquals(0, aggregates.getCount());
      assertNull(aggregates.getMin());
      assertNull(aggregates.getMax());
      assertNull(aggregates.getSum());
      assertNull(aggregates.getAverage());

      aggregates.add(5L);
      assertEquals(1, aggregates.getCount());
      assertEquals(5, aggregates.getMin());
      assertEquals(5, aggregates.getMax());
      assertEquals(5, aggregates.getSum());
      assertThat(aggregates.getAverage()).isCloseTo(new BigDecimal("5"), Offset.offset(BigDecimal.ZERO));

      aggregates.add(10L);
      assertEquals(2, aggregates.getCount());
      assertEquals(5, aggregates.getMin());
      assertEquals(10, aggregates.getMax());
      assertEquals(15, aggregates.getSum());
      assertThat(aggregates.getAverage()).isCloseTo(new BigDecimal("7.5"), Offset.offset(BigDecimal.ZERO));

      aggregates.add(15L);
      assertEquals(3, aggregates.getCount());
      assertEquals(5, aggregates.getMin());
      assertEquals(15, aggregates.getMax());
      assertEquals(30, aggregates.getSum());
      assertThat(aggregates.getAverage()).isCloseTo(new BigDecimal("10"), Offset.offset(BigDecimal.ZERO));

      aggregates.add(null);
      assertEquals(3, aggregates.getCount());
      assertEquals(5, aggregates.getMin());
      assertEquals(15, aggregates.getMax());
      assertEquals(30, aggregates.getSum());
      assertThat(aggregates.getAverage()).isCloseTo(new BigDecimal("10"), Offset.offset(BigDecimal.ZERO));

      assertEquals(new BigDecimal("750"), aggregates.getProduct());
      assertEquals(new BigDecimal("25.0000"), aggregates.getVariance());
      assertEquals(new BigDecimal("5.0000"), aggregates.getStandardDeviation());
      assertThat(aggregates.getVarP()).isCloseTo(new BigDecimal("16.6667"), Offset.offset(new BigDecimal(".0001")));
      assertThat(aggregates.getStdDevP()).isCloseTo(new BigDecimal("4.0824"), Offset.offset(new BigDecimal(".0001")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBigDecimal()
   {
      BigDecimalAggregates aggregates = new BigDecimalAggregates();
      aggregates.add(null);

      assertEquals(0, aggregates.getCount());
      assertNull(aggregates.getMin());
      assertNull(aggregates.getMax());
      assertNull(aggregates.getSum());
      assertNull(aggregates.getAverage());

      BigDecimal bd51 = new BigDecimal("5.1");
      aggregates.add(bd51);
      assertEquals(1, aggregates.getCount());
      assertEquals(bd51, aggregates.getMin());
      assertEquals(bd51, aggregates.getMax());
      assertEquals(bd51, aggregates.getSum());
      assertThat(aggregates.getAverage()).isCloseTo(bd51, Offset.offset(BigDecimal.ZERO));

      BigDecimal bd101 = new BigDecimal("10.1");
      aggregates.add(new BigDecimal("10.1"));
      assertEquals(2, aggregates.getCount());
      assertEquals(bd51, aggregates.getMin());
      assertEquals(bd101, aggregates.getMax());
      assertEquals(new BigDecimal("15.2"), aggregates.getSum());
      assertThat(aggregates.getAverage()).isCloseTo(new BigDecimal("7.6"), Offset.offset(BigDecimal.ZERO));

      BigDecimal bd148 = new BigDecimal("14.8");
      aggregates.add(bd148);
      assertEquals(3, aggregates.getCount());
      assertEquals(bd51, aggregates.getMin());
      assertEquals(bd148, aggregates.getMax());
      assertEquals(new BigDecimal("30.0"), aggregates.getSum());
      assertThat(aggregates.getAverage()).isCloseTo(new BigDecimal("10.0"), Offset.offset(BigDecimal.ZERO));

      assertEquals(new BigDecimal("762.348"), aggregates.getProduct());
      assertEquals(new BigDecimal("23.5300"), aggregates.getVariance());
      assertEquals(new BigDecimal("4.8508"), aggregates.getStandardDeviation());
      assertThat(aggregates.getVarP()).isCloseTo(new BigDecimal("15.6867"), Offset.offset(new BigDecimal(".0001")));
      assertThat(aggregates.getStdDevP()).isCloseTo(new BigDecimal("3.9606"), Offset.offset(new BigDecimal(".0001")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInstant()
   {
      InstantAggregates aggregates = new InstantAggregates();

      assertEquals(0, aggregates.getCount());
      assertNull(aggregates.getMin());
      assertNull(aggregates.getMax());
      assertNull(aggregates.getSum());
      assertNull(aggregates.getAverage());

      Instant i1970 = Instant.parse("1970-01-01T00:00:00Z");
      aggregates.add(i1970);
      assertEquals(1, aggregates.getCount());
      assertEquals(i1970, aggregates.getMin());
      assertEquals(i1970, aggregates.getMax());
      assertNull(aggregates.getSum());
      assertEquals(i1970, aggregates.getAverage());

      Instant i1980 = Instant.parse("1980-01-01T00:00:00Z");
      aggregates.add(i1980);
      assertEquals(2, aggregates.getCount());
      assertEquals(i1970, aggregates.getMin());
      assertEquals(i1980, aggregates.getMax());
      assertNull(aggregates.getSum());
      assertEquals(Instant.parse("1975-01-01T00:00:00Z"), aggregates.getAverage());

      Instant i1990 = Instant.parse("1990-01-01T00:00:00Z");
      aggregates.add(i1990);
      assertEquals(3, aggregates.getCount());
      assertEquals(i1970, aggregates.getMin());
      assertEquals(i1990, aggregates.getMax());
      assertNull(aggregates.getSum());
      assertEquals(Instant.parse("1980-01-01T08:00:00Z"), aggregates.getAverage()); // a leap day throws this off by 8 hours :)

      /////////////////////////////////////////////////////////////////////
      // assert we gracefully return null for these ops we don't support //
      /////////////////////////////////////////////////////////////////////
      assertNull(aggregates.getProduct());
      assertNull(aggregates.getVariance());
      assertNull(aggregates.getStandardDeviation());
      assertNull(aggregates.getVarP());
      assertNull(aggregates.getStdDevP());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testLocalDate()
   {
      LocalDateAggregates aggregates = new LocalDateAggregates();

      assertEquals(0, aggregates.getCount());
      assertNull(aggregates.getMin());
      assertNull(aggregates.getMax());
      assertNull(aggregates.getSum());
      assertNull(aggregates.getAverage());

      LocalDate ld1970 = LocalDate.of(1970, Month.JANUARY, 1);
      aggregates.add(ld1970);
      assertEquals(1, aggregates.getCount());
      assertEquals(ld1970, aggregates.getMin());
      assertEquals(ld1970, aggregates.getMax());
      assertNull(aggregates.getSum());
      assertEquals(ld1970, aggregates.getAverage());

      LocalDate ld1980 = LocalDate.of(1980, Month.JANUARY, 1);
      aggregates.add(ld1980);
      assertEquals(2, aggregates.getCount());
      assertEquals(ld1970, aggregates.getMin());
      assertEquals(ld1980, aggregates.getMax());
      assertNull(aggregates.getSum());
      assertEquals(LocalDate.of(1975, Month.JANUARY, 1), aggregates.getAverage());

      LocalDate ld1990 = LocalDate.of(1990, Month.JANUARY, 1);
      aggregates.add(ld1990);
      assertEquals(3, aggregates.getCount());
      assertEquals(ld1970, aggregates.getMin());
      assertEquals(ld1990, aggregates.getMax());
      assertNull(aggregates.getSum());
      assertEquals(ld1980, aggregates.getAverage());

      /////////////////////////////////////////////////////////////////////
      // assert we gracefully return null for these ops we don't support //
      /////////////////////////////////////////////////////////////////////
      assertNull(aggregates.getProduct());
      assertNull(aggregates.getVariance());
      assertNull(aggregates.getStandardDeviation());
      assertNull(aggregates.getVarP());
      assertNull(aggregates.getStdDevP());
   }

}