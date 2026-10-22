/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions;


import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.StartOfPeriod.Unit;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for StartOfPeriod
 *******************************************************************************/
class StartOfPeriodTest extends BaseTest
{
   private static final QFieldMetaData DATE_TIME_FIELD = new QFieldMetaData("when", QFieldType.DATE_TIME);
   private static final QFieldMetaData DATE_FIELD      = new QFieldMetaData("day", QFieldType.DATE);

   private static final String CHICAGO = "America/Chicago";

   ////////////////////////////////////////////////////////////////////////////
   // Wed Oct 14 2026, 10:30 in Chicago (CDT, UTC-5) - 15:30Z                  //
   ////////////////////////////////////////////////////////////////////////////
   private static final Instant OCT_14_2026 = Instant.parse("2026-10-14T15:30:00Z");



   /***************************************************************************
    **
    ***************************************************************************/
   private Object at(StartOfPeriod expression, QFieldMetaData field, Instant now) throws QException
   {
      return (expression.evaluate(field, now));
   }



   /*******************************************************************************
    ** Day, week, month, quarter, year - this, last, next - as a DATE_TIME (the
    ** Instant of local midnight, in the expression's zone).
    *******************************************************************************/
   @Test
   void testDateTimeInChicago() throws QException
   {
      ///////////////////////////////////////////////////////////////////////////////
      // Oct 14 is CDT (-5): local midnight is 05:00Z.  Nov 1 is CST (-6): 06:00Z.  //
      ///////////////////////////////////////////////////////////////////////////////
      assertEquals(Instant.parse("2026-10-14T05:00:00Z"), at(StartOfPeriod.of(Unit.DAYS, 0).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));
      assertEquals(Instant.parse("2026-10-13T05:00:00Z"), at(StartOfPeriod.of(Unit.DAYS, -1).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));
      assertEquals(Instant.parse("2026-10-15T05:00:00Z"), at(StartOfPeriod.of(Unit.DAYS, 1).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));

      // Sunday Oct 11 (default week start)
      assertEquals(Instant.parse("2026-10-11T05:00:00Z"), at(StartOfPeriod.of(Unit.WEEKS, 0).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));
      assertEquals(Instant.parse("2026-10-04T05:00:00Z"), at(StartOfPeriod.of(Unit.WEEKS, -1).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));

      assertEquals(Instant.parse("2026-10-01T05:00:00Z"), at(StartOfPeriod.of(Unit.MONTHS, 0).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));
      assertEquals(Instant.parse("2026-09-01T05:00:00Z"), at(StartOfPeriod.of(Unit.MONTHS, -1).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));

      //////////////////////////////////////////////////////////////////////////////
      // next month starts Nov 1 - local midnight is after the Nov 1 2:00 fall-back //
      // so, at CST (-6).  and last month/this month, from October, stay CDT.       //
      //////////////////////////////////////////////////////////////////////////////
      assertEquals(Instant.parse("2026-11-01T05:00:00Z"), at(StartOfPeriod.of(Unit.MONTHS, 1).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));
      assertEquals(Instant.parse("2026-12-01T06:00:00Z"), at(StartOfPeriod.of(Unit.MONTHS, 2).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));

      assertEquals(Instant.parse("2026-10-01T05:00:00Z"), at(StartOfPeriod.of(Unit.QUARTERS, 0).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));
      assertEquals(Instant.parse("2026-07-01T05:00:00Z"), at(StartOfPeriod.of(Unit.QUARTERS, -1).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));
      assertEquals(Instant.parse("2027-01-01T06:00:00Z"), at(StartOfPeriod.of(Unit.QUARTERS, 1).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));

      assertEquals(Instant.parse("2026-01-01T06:00:00Z"), at(StartOfPeriod.of(Unit.YEARS, 0).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));
      assertEquals(Instant.parse("2025-01-01T06:00:00Z"), at(StartOfPeriod.of(Unit.YEARS, -1).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, OCT_14_2026));
   }



   /*******************************************************************************
    ** The time zone decides which day it is - just after local midnight, Chicago
    ** is still "yesterday" in UTC terms, and a late-evening moment is already
    ** "tomorrow" in UTC.
    *******************************************************************************/
   @Test
   void testZoneDecidesTheDay() throws QException
   {
      Instant lateEveningChicago = Instant.parse("2026-10-15T03:30:00Z"); // Oct 14, 10:30 PM CDT
      assertEquals(LocalDate.of(2026, 10, 14), at(StartOfPeriod.of(Unit.DAYS, 0).withTimeZoneId(CHICAGO), DATE_FIELD, lateEveningChicago));
      assertEquals(LocalDate.of(2026, 10, 15), at(StartOfPeriod.of(Unit.DAYS, 0).withTimeZoneId("UTC"), DATE_FIELD, lateEveningChicago));
      assertEquals(LocalDate.of(2026, 10, 15), at(StartOfPeriod.of(Unit.DAYS, 0).withTimeZoneId("Asia/Tokyo"), DATE_FIELD, lateEveningChicago));
   }



   /*******************************************************************************
    ** A day that has a daylight-saving change is 23 or 25 hours long - the range
    ** [start of day, start of next day) still covers it exactly.
    *******************************************************************************/
   @Test
   void testDaylightSavingDayLengths() throws QException
   {
      // spring forward: Sunday March 8 2026 (2:00 CST -> 3:00 CDT) - 23 hours
      Instant march8 = Instant.parse("2026-03-08T18:00:00Z");
      Instant start  = (Instant) at(StartOfPeriod.of(Unit.DAYS, 0).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, march8);
      Instant end    = (Instant) at(StartOfPeriod.of(Unit.DAYS, 1).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, march8);
      assertEquals(Instant.parse("2026-03-08T06:00:00Z"), start);
      assertEquals(Instant.parse("2026-03-09T05:00:00Z"), end);
      assertEquals(Duration.ofHours(23), Duration.between(start, end));

      // fall back: Sunday November 1 2026 (2:00 CDT -> 1:00 CST) - 25 hours
      Instant nov1  = Instant.parse("2026-11-01T18:00:00Z");
      Instant start2 = (Instant) at(StartOfPeriod.of(Unit.DAYS, 0).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, nov1);
      Instant end2   = (Instant) at(StartOfPeriod.of(Unit.DAYS, 1).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, nov1);
      assertEquals(Instant.parse("2026-11-01T05:00:00Z"), start2);
      assertEquals(Instant.parse("2026-11-02T06:00:00Z"), end2);
      assertEquals(Duration.ofHours(25), Duration.between(start2, end2));

      // a week that contains the change
      assertEquals(Duration.ofHours(7 * 24 + 1), Duration.between(
         (Instant) at(StartOfPeriod.of(Unit.WEEKS, 0).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, nov1),
         (Instant) at(StartOfPeriod.of(Unit.WEEKS, 1).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, nov1)));

      // a month that contains the change: November 2026 is 30 days + 1 hour
      assertEquals(Duration.ofHours(30 * 24 + 1), Duration.between(
         (Instant) at(StartOfPeriod.of(Unit.MONTHS, 0).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, nov1),
         (Instant) at(StartOfPeriod.of(Unit.MONTHS, 1).withTimeZoneId(CHICAGO), DATE_TIME_FIELD, nov1)));
   }



   /*******************************************************************************
    ** In a zone where the daylight-saving change skips local midnight (Sao Paulo
    ** used to; Havana still does), the start of the day is the first valid moment.
    *******************************************************************************/
   @Test
   void testMidnightSkippedByDaylightSaving() throws QException
   {
      // Havana springs forward at 00:00 -> 01:00 on the second Sunday of March (2026-03-08)
      Instant instant = Instant.parse("2026-03-08T12:00:00Z");
      Instant start   = (Instant) at(StartOfPeriod.of(Unit.DAYS, 0).withTimeZoneId("America/Havana"), DATE_TIME_FIELD, instant);
      assertEquals(Instant.parse("2026-03-08T05:00:00Z"), start); // 01:00 CDT (-4)
   }



   /*******************************************************************************
    ** Week start: default Sunday; Monday when asked.  And quarters/leap years.
    *******************************************************************************/
   @Test
   void testWeekStartAndCalendarEdges() throws QException
   {
      // Wed 2026-10-14
      assertEquals(LocalDate.of(2026, 10, 11), at(StartOfPeriod.of(Unit.WEEKS, 0).withTimeZoneId("UTC"), DATE_FIELD, OCT_14_2026));
      assertEquals(LocalDate.of(2026, 10, 12), at(StartOfPeriod.of(Unit.WEEKS, 0).withTimeZoneId("UTC").withWeekStart(DayOfWeek.MONDAY), DATE_FIELD, OCT_14_2026));
      assertEquals(LocalDate.of(2026, 10, 5), at(StartOfPeriod.of(Unit.WEEKS, -1).withTimeZoneId("UTC").withWeekStart(DayOfWeek.MONDAY), DATE_FIELD, OCT_14_2026));
      assertEquals(LocalDate.of(2026, 10, 10), at(StartOfPeriod.of(Unit.WEEKS, 0).withTimeZoneId("UTC").withWeekStart(DayOfWeek.SATURDAY), DATE_FIELD, OCT_14_2026));

      // when "today" is itself the first day of the week
      assertEquals(LocalDate.of(2026, 10, 12), at(StartOfPeriod.of(Unit.WEEKS, 0).withTimeZoneId("UTC").withWeekStart(DayOfWeek.MONDAY), DATE_FIELD, Instant.parse("2026-10-12T00:00:00Z")));

      // quarters, across a year boundary
      Instant feb = Instant.parse("2027-02-10T12:00:00Z");
      assertEquals(LocalDate.of(2027, 1, 1), at(StartOfPeriod.of(Unit.QUARTERS, 0).withTimeZoneId("UTC"), DATE_FIELD, feb));
      assertEquals(LocalDate.of(2026, 10, 1), at(StartOfPeriod.of(Unit.QUARTERS, -1).withTimeZoneId("UTC"), DATE_FIELD, feb));
      assertEquals(LocalDate.of(2027, 4, 1), at(StartOfPeriod.of(Unit.QUARTERS, 1).withTimeZoneId("UTC"), DATE_FIELD, feb));

      // months, years, from a 31st / a leap day
      assertEquals(LocalDate.of(2026, 2, 1), at(StartOfPeriod.of(Unit.MONTHS, -1).withTimeZoneId("UTC"), DATE_FIELD, Instant.parse("2026-03-31T12:00:00Z")));
      assertEquals(LocalDate.of(2027, 1, 1), at(StartOfPeriod.of(Unit.YEARS, 1).withTimeZoneId("UTC"), DATE_FIELD, Instant.parse("2026-12-31T23:59:59Z")));
      assertEquals(LocalDate.of(2028, 1, 1), at(StartOfPeriod.of(Unit.YEARS, 0).withTimeZoneId("UTC"), DATE_FIELD, Instant.parse("2028-02-29T12:00:00Z")));
   }



   /*******************************************************************************
    ** The zone comes from the session (X-QQQ-UserTimezone) when the expression
    ** doesn't say, then from the instance default; the expression's own zone wins.
    *******************************************************************************/
   @Test
   void testZoneFromSessionThenInstance() throws QException
   {
      Instant lateEveningChicago = Instant.parse("2026-10-15T03:30:00Z"); // Oct 14, 10:30 PM CDT; Oct 15 in UTC

      StartOfPeriod today = StartOfPeriod.of(Unit.DAYS, 0);
      QContext.getQInstance().setDefaultTimeZoneId("UTC");
      assertEquals(LocalDate.of(2026, 10, 15), at(today, DATE_FIELD, lateEveningChicago));

      QContext.getQSession().setValue(QSession.VALUE_KEY_USER_TIMEZONE, CHICAGO);
      assertEquals(LocalDate.of(2026, 10, 14), at(today, DATE_FIELD, lateEveningChicago));
      assertEquals(Instant.parse("2026-10-14T05:00:00Z"), at(today, DATE_TIME_FIELD, lateEveningChicago));

      assertEquals(LocalDate.of(2026, 10, 15), at(StartOfPeriod.of(Unit.DAYS, 0).withTimeZoneId("Asia/Tokyo"), DATE_FIELD, lateEveningChicago));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testErrors()
   {
      assertThatThrownBy(() -> at(StartOfPeriod.of(Unit.DAYS, 0).withTimeZoneId("Not/AZone"), DATE_FIELD, OCT_14_2026))
         .hasMessageContaining("Unrecognized time zone");
      assertThatThrownBy(() -> at(StartOfPeriod.of(Unit.DAYS, 0), new QFieldMetaData("n", QFieldType.INTEGER), OCT_14_2026))
         .hasMessageContaining("Unsupported field type");
      assertThatThrownBy(() -> at(StartOfPeriod.of(null, 0).withTimeZoneId("UTC"), DATE_FIELD, OCT_14_2026))
         .hasMessageContaining("requires a unit");
   }



   /*******************************************************************************
    ** A null field evaluates as a DATE_TIME (like the sibling expressions).
    *******************************************************************************/
   @Test
   void testNullFieldIsDateTime() throws QException
   {
      assertThat(StartOfPeriod.of(Unit.DAYS, 0).evaluate(null)).isInstanceOf(Instant.class);
   }



   /*******************************************************************************
    ** The JSON shape the UI sends - and how it round-trips.
    *******************************************************************************/
   @Test
   void testJsonRoundTrip() throws Exception
   {
      QFilterCriteria criteria = JsonUtils.toObject("""
         {"fieldName": "createDate", "operator": "GREATER_THAN_OR_EQUALS", "values":
            [{"type": "StartOfPeriod", "unit": "WEEKS", "offset": -1, "weekStart": "MONDAY", "timeZoneId": "America/Chicago"}]
         }
         """, QFilterCriteria.class);
      StartOfPeriod expression = (StartOfPeriod) criteria.getValues().get(0);
      assertEquals(Unit.WEEKS, expression.getUnit());
      assertEquals(-1, expression.getOffset());
      assertEquals(DayOfWeek.MONDAY, expression.getWeekStart());
      assertEquals("America/Chicago", expression.getTimeZoneId());

      QFilterCriteria minimal = JsonUtils.toObject("""
         {"fieldName": "createDate", "operator": "LESS_THAN", "values": [{"type": "StartOfPeriod", "unit": "MONTHS"}]}
         """, QFilterCriteria.class);
      StartOfPeriod minimalExpression = (StartOfPeriod) minimal.getValues().get(0);
      assertEquals(Unit.MONTHS, minimalExpression.getUnit());
      assertEquals(0, minimalExpression.getOffset());

      String json = JsonUtils.toJson(expression);
      StartOfPeriod again = JsonUtils.toObject(json, StartOfPeriod.class);
      assertEquals(Unit.WEEKS, again.getUnit());
      assertEquals("StartOfPeriod", again.getType());
   }



   /*******************************************************************************
    ** The whole point: ">= start of Oct 1" and "< start of Oct 13" (the exclusive
    ** end of "Oct 1 to Oct 12 inclusive"), against real records, in Chicago - the
    ** 23:59:59.999 record on Oct 12 is in, the 00:00:00.000 record on Oct 13 is out.
    *******************************************************************************/
   @Test
   void testHalfOpenRangeQuery() throws QException
   {
      QContext.getQSession().setValue(QSession.VALUE_KEY_USER_TIMEZONE, CHICAGO);

      InsertInput insertInput = new InsertInput(TestUtils.TABLE_NAME_PERSON_MEMORY);
      insertInput.setRecords(List.of(
         new QRecord().withValue("firstName", "before").withValue("lastName", "x").withValue("timestamp", Instant.parse("2026-10-01T04:59:59.999Z")), // Sep 30, 11:59:59.999 PM
         new QRecord().withValue("firstName", "first").withValue("lastName", "x").withValue("timestamp", Instant.parse("2026-10-01T05:00:00Z")), // Oct 1, 00:00:00
         new QRecord().withValue("firstName", "last").withValue("lastName", "x").withValue("timestamp", Instant.parse("2026-10-13T04:59:59.999Z")), // Oct 12, 11:59:59.999 PM
         new QRecord().withValue("firstName", "after").withValue("lastName", "x").withValue("timestamp", Instant.parse("2026-10-13T05:00:00Z")))); // Oct 13, 00:00:00
      new InsertAction().execute(insertInput);

      QueryInput queryInput = new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withFilter(new QQueryFilter()
         .withCriteria(new QFilterCriteria("timestamp", QCriteriaOperator.GREATER_THAN_OR_EQUALS, Instant.parse("2026-10-01T05:00:00Z")))
         .withCriteria(new QFilterCriteria("timestamp", QCriteriaOperator.LESS_THAN, Instant.parse("2026-10-13T05:00:00Z"))));
      List<String> names = new QueryAction().execute(queryInput).getRecords().stream().map(r -> r.getValueString("firstName")).collect(Collectors.toList());
      assertThat(names).containsExactlyInAnyOrder("first", "last");

      /////////////////////////////////////////////////////////////////////////////////////////////
      // the same, as expressions: "today" on Oct 12 (evaluated by the backend, in the session's //
      // zone) - but with the clock fixed, by evaluating the bounds here and querying with them. //
      /////////////////////////////////////////////////////////////////////////////////////////////
      Instant oct12 = Instant.parse("2026-10-12T20:00:00Z");
      QQueryFilter expressionFilter = new QQueryFilter()
         .withCriteria(new QFilterCriteria("timestamp", QCriteriaOperator.GREATER_THAN_OR_EQUALS, StartOfPeriod.of(Unit.DAYS, 0).evaluate(null, oct12)))
         .withCriteria(new QFilterCriteria("timestamp", QCriteriaOperator.LESS_THAN, StartOfPeriod.of(Unit.DAYS, 1).evaluate(null, oct12)));
      List<String> today = new QueryAction().execute(new QueryInput(TestUtils.TABLE_NAME_PERSON_MEMORY).withFilter(expressionFilter)).getRecords().stream().map(r -> r.getValueString("firstName")).collect(Collectors.toList());
      assertThat(today).containsExactly("last"); // only the 23:59:59.999 record is on Oct 12 (Chicago)
   }

}
