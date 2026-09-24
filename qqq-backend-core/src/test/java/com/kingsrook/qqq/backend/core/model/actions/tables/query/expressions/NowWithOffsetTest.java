/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions;


import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;


/*******************************************************************************
 ** Unit test for NowWithOffset
 *******************************************************************************/
class NowWithOffsetTest extends BaseTest
{
   private static final long DAY_IN_MILLIS = 24 * 60 * 60 * 1000;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws Exception
   {
      long now = System.currentTimeMillis();

      QFieldMetaData dateTimeField = new QFieldMetaData("myDateTime", QFieldType.DATE_TIME);
      QFieldMetaData dateField     = new QFieldMetaData("myDate", QFieldType.DATE);

      {
         Offset<Long> allowedDiff            = Offset.offset(100L);
         Offset<Long> allowedDiffPlusOneDay  = Offset.offset(100L + DAY_IN_MILLIS);
         Offset<Long> allowedDiffPlusTwoDays = Offset.offset(100L + 2 * DAY_IN_MILLIS);

         long oneWeekAgoMillis = ((Instant) NowWithOffset.minus(1, ChronoUnit.WEEKS).evaluate(dateTimeField)).toEpochMilli();
         assertThat(oneWeekAgoMillis).isCloseTo(now - (7 * DAY_IN_MILLIS), allowedDiff);

         long twoWeeksFromNowMillis = ((Instant) NowWithOffset.plus(2, ChronoUnit.WEEKS).evaluate(dateTimeField)).toEpochMilli();
         assertThat(twoWeeksFromNowMillis).isCloseTo(now + (14 * DAY_IN_MILLIS), allowedDiff);

         long oneMonthAgoMillis = ((Instant) NowWithOffset.minus(1, ChronoUnit.MONTHS).evaluate(dateTimeField)).toEpochMilli();
         assertThat(oneMonthAgoMillis).isCloseTo(now - (30 * DAY_IN_MILLIS), allowedDiffPlusTwoDays); // two days, to work on 3/1...

         long twoMonthsFromNowMillis = ((Instant) NowWithOffset.plus(2, ChronoUnit.MONTHS).evaluate(dateTimeField)).toEpochMilli();
         assertThat(twoMonthsFromNowMillis).isCloseTo(now + (60 * DAY_IN_MILLIS), allowedDiffPlusTwoDays);

         long oneYearAgoMillis = ((Instant) NowWithOffset.minus(1, ChronoUnit.YEARS).evaluate(dateTimeField)).toEpochMilli();
         assertThat(oneYearAgoMillis).isCloseTo(now - (365 * DAY_IN_MILLIS), allowedDiffPlusOneDay);

         long twoYearsFromNowMillis = ((Instant) NowWithOffset.plus(2, ChronoUnit.YEARS).evaluate(dateTimeField)).toEpochMilli();
         assertThat(twoYearsFromNowMillis).isCloseTo(now + (730 * DAY_IN_MILLIS), allowedDiffPlusTwoDays);
      }

      {
         assertThat(NowWithOffset.minus(1, ChronoUnit.WEEKS).evaluate(dateField)).isInstanceOf(LocalDate.class);

         assertEquals(LocalDate.now().minusDays(1), NowWithOffset.minus(1, ChronoUnit.DAYS).evaluate(dateField));
         assertEquals(LocalDate.now().minusDays(7), NowWithOffset.minus(1, ChronoUnit.WEEKS).evaluate(dateField));
      }
   }



   /*******************************************************************************
    ** A null field argument should default to DATE_TIME and return an Instant.
    *******************************************************************************/
   @Test
   void testEvaluate_nullField_defaultsToDateTime() throws QException
   {
      // Act
      Object result = NowWithOffset.minus(1, ChronoUnit.DAYS).evaluate(null);

      // Assert — null field must fall back to DATE_TIME → Instant
      assertInstanceOf(Instant.class, result);
   }



   /*******************************************************************************
    ** Evaluating against an unsupported field type (e.g. INTEGER) should throw QException.
    *******************************************************************************/
   @Test
   void testEvaluate_unsupportedFieldType_throws()
   {
      QFieldMetaData intField = new QFieldMetaData("n", QFieldType.INTEGER);
      assertThatThrownBy(() -> NowWithOffset.minus(1, ChronoUnit.DAYS).evaluate(intField))
         .isInstanceOf(QException.class)
         .hasMessageContaining("Unsupported");
   }



   /*******************************************************************************
    ** DATE field with a PLUS offset should move forward, not backward.
    *******************************************************************************/
   @Test
   void testEvaluate_plusOperator_dateField_movesForward() throws QException
   {
      // Act
      LocalDate result = (LocalDate) NowWithOffset.plus(3, ChronoUnit.DAYS).evaluate(
         new QFieldMetaData("d", QFieldType.DATE));

      // Assert
      assertEquals(LocalDate.now().plusDays(3), result);
   }



   /*******************************************************************************
    ** DATE_TIME field with DAYS unit should return an Instant roughly 24 h away.
    *******************************************************************************/
   @Test
   void testEvaluate_daysUnit_dateTimeField_returnsInstant() throws QException
   {
      long now    = System.currentTimeMillis();
      long result = ((Instant) NowWithOffset.minus(1, ChronoUnit.DAYS).evaluate(
         new QFieldMetaData("dt", QFieldType.DATE_TIME))).toEpochMilli();

      assertThat(result).isCloseTo(now - DAY_IN_MILLIS, Offset.offset(100L));
   }



   /*******************************************************************************
    ** factory getters should round-trip the values set in the factory method.
    *******************************************************************************/
   @Test
   void testFactoryGetters_roundTrip()
   {
      NowWithOffset expr = NowWithOffset.plus(5, ChronoUnit.HOURS);
      assertEquals(NowWithOffset.Operator.PLUS, expr.getOperator());
      assertEquals(5, expr.getAmount());
      assertEquals(ChronoUnit.HOURS, expr.getTimeUnit());
   }

}
