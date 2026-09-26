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

package com.kingsrook.qqq.backend.core.actions.dashboard.widgets;


import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.BiFunction;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for DateTimeGroupBy
 *******************************************************************************/
class DateTimeGroupByTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMillisPer()
   {
      assertEquals(3_600_000L, DateTimeGroupBy.MillisPer.HOUR);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRoundDown()
   {
      assertEquals(Instant.parse("2023-01-01T00:00:00Z"), DateTimeGroupBy.YEAR.roundDown(Instant.parse("2023-03-13T14:50:00Z")));
      assertEquals(Instant.parse("2023-03-01T00:00:00Z"), DateTimeGroupBy.MONTH.roundDown(Instant.parse("2023-03-13T14:50:00Z")));
      assertEquals(Instant.parse("2023-03-12T00:00:00Z"), DateTimeGroupBy.WEEK.roundDown(Instant.parse("2023-03-13T14:50:00Z")));
      assertEquals(Instant.parse("2023-03-13T00:00:00Z"), DateTimeGroupBy.DAY.roundDown(Instant.parse("2023-03-13T14:50:00Z")));
      assertEquals(Instant.parse("2023-03-13T14:00:00Z"), DateTimeGroupBy.HOUR.roundDown(Instant.parse("2023-03-13T14:50:00Z")));

      QContext.getQInstance().setDefaultTimeZoneId("US/Eastern");
      assertEquals(Instant.parse("2023-02-01T05:00:00Z"), DateTimeGroupBy.MONTH.roundDown(Instant.parse("2023-03-01T00:00:00Z")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testIncrement()
   {
      assertEquals(Instant.parse("2024-03-01T00:00:00Z"), DateTimeGroupBy.YEAR.increment(Instant.parse("2023-03-01T00:00:00Z")));
      assertEquals(Instant.parse("2024-01-01T00:00:00Z"), DateTimeGroupBy.MONTH.increment(Instant.parse("2023-12-01T00:00:00Z")));
      assertEquals(Instant.parse("2024-01-01T00:00:00Z"), DateTimeGroupBy.MONTH.increment(Instant.parse("2023-12-01T00:00:00Z")));
      assertEquals(Instant.parse("2023-03-08T00:00:00Z"), DateTimeGroupBy.WEEK.increment(Instant.parse("2023-03-01T00:00:00Z")));
      assertEquals(Instant.parse("2023-03-02T00:00:00Z"), DateTimeGroupBy.DAY.increment(Instant.parse("2023-03-01T00:00:00Z")));
      assertEquals(Instant.parse("2023-03-01T01:00:00Z"), DateTimeGroupBy.HOUR.increment(Instant.parse("2023-03-01T00:00:00Z")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSelectFromStartAndEndTimes()
   {
      ///////////////////////////////////////////////////////////////////
      // simple wrapper to call the function w/ a specified time range //
      ///////////////////////////////////////////////////////////////////
      BiFunction<Integer, ChronoUnit, DateTimeGroupBy> f = (amountToAdd, unit) ->
      {
         Instant start = Instant.parse("2021-01-01T00:00:00Z");
         Instant end   = start.plus(amountToAdd, unit);
         return (DateTimeGroupBy.selectFromStartAndEndTimes(start, end));
      };

      ///////////////////////////////////////////////////////////////////
      // choose YEAR if the timeframe is any amount larger than a year //
      ///////////////////////////////////////////////////////////////////
      assertEquals(DateTimeGroupBy.YEAR, f.apply(365 * 10, ChronoUnit.DAYS));
      assertEquals(DateTimeGroupBy.YEAR, f.apply(365 + 1, ChronoUnit.DAYS));

      /////////////////////////////////////////////////////////
      // choose month if equal to 1 year, or down to 60 days //
      /////////////////////////////////////////////////////////
      assertEquals(DateTimeGroupBy.MONTH, f.apply(365, ChronoUnit.DAYS));
      assertEquals(DateTimeGroupBy.MONTH, f.apply(61, ChronoUnit.DAYS));

      ///////////////////////////////////////
      // week between 60 days and 35 days //
      ///////////////////////////////////////
      assertEquals(DateTimeGroupBy.WEEK, f.apply(60, ChronoUnit.DAYS));
      assertEquals(DateTimeGroupBy.WEEK, f.apply(36, ChronoUnit.DAYS));

      //////////////////////////////////////
      // day between 35 days and 36 hours //
      //////////////////////////////////////
      assertEquals(DateTimeGroupBy.DAY, f.apply(35, ChronoUnit.DAYS));
      assertEquals(DateTimeGroupBy.DAY, f.apply(37, ChronoUnit.HOURS));

      //////////////////////////////////////////
      // hour under 36 hours (even negative!) //
      //////////////////////////////////////////
      assertEquals(DateTimeGroupBy.HOUR, f.apply(35, ChronoUnit.HOURS));
      assertEquals(DateTimeGroupBy.HOUR, f.apply(1, ChronoUnit.HOURS));
      assertEquals(DateTimeGroupBy.HOUR, f.apply(0, ChronoUnit.HOURS));
      assertEquals(DateTimeGroupBy.HOUR, f.apply(-1, ChronoUnit.HOURS));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetSqlExpression()
   {
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // note - double %'s on the time format strings here, because this is a java-format string, which will get //
      // its '%s' replaced with a column name, and so then those %'s for the date_format need escaped as %%.     //
      /////////////////////////////////////////////////////////////////////////////////////////////////////////////
      assertEquals("DATE_FORMAT(CONVERT_TZ(%s, 'UTC', 'UTC'), '%%Y')", DateTimeGroupBy.YEAR.getSqlExpression());
      assertEquals("DATE_FORMAT(CONVERT_TZ(%s, 'UTC', 'UTC'), '%%Y-%%m')", DateTimeGroupBy.MONTH.getSqlExpression());

      /////////////////////////////////////////////////////////////////////////////
      // if session has no zone info, but instance does, assert that it is used. //
      /////////////////////////////////////////////////////////////////////////////
      QContext.getQInstance().setDefaultTimeZoneId("US/Eastern");
      assertEquals("DATE_FORMAT(CONVERT_TZ(%s, 'UTC', 'US/Eastern'), '%%Y')", DateTimeGroupBy.YEAR.getSqlExpression());

      QContext.getQInstance().setDefaultTimeZoneId("US/Central");
      assertEquals("DATE_FORMAT(CONVERT_TZ(%s, 'UTC', 'US/Central'), '%%Y')", DateTimeGroupBy.YEAR.getSqlExpression());

      //////////////////////////////////////////////////////////////////
      // put a zone offset (but not name) in session - see it be used //
      //////////////////////////////////////////////////////////////////
      QContext.getQSession().setValue(QSession.VALUE_KEY_USER_TIMEZONE_OFFSET_MINUTES, "-300");
      assertEquals("DATE_FORMAT(CONVERT_TZ(%s, 'UTC', '-05:00'), '%%Y')", DateTimeGroupBy.YEAR.getSqlExpression());

      QContext.getQSession().setValue(QSession.VALUE_KEY_USER_TIMEZONE_OFFSET_MINUTES, "-420");
      assertEquals("DATE_FORMAT(CONVERT_TZ(%s, 'UTC', '-07:00'), '%%Y')", DateTimeGroupBy.YEAR.getSqlExpression());

      ///////////////////////////////////////////////////
      // put a zone (name) in session - see it be used //
      ///////////////////////////////////////////////////
      QContext.getQSession().setValue(QSession.VALUE_KEY_USER_TIMEZONE_OFFSET_MINUTES, null);
      QContext.getQSession().setValue(QSession.VALUE_KEY_USER_TIMEZONE, "US/Central");
      assertEquals("DATE_FORMAT(CONVERT_TZ(%s, 'UTC', 'US/Central'), '%%Y')", DateTimeGroupBy.YEAR.getSqlExpression());

      QContext.getQSession().setValue(QSession.VALUE_KEY_USER_TIMEZONE, "US/Eastern");
      assertEquals("DATE_FORMAT(CONVERT_TZ(%s, 'UTC', 'US/Eastern'), '%%Y')", DateTimeGroupBy.YEAR.getSqlExpression());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMakeSelectedString()
   {
      assertEquals("2021", DateTimeGroupBy.YEAR.makeSelectedString(Instant.parse("2021-01-01T00:00:00Z")));
      assertEquals("2021", DateTimeGroupBy.YEAR.makeSelectedString(Instant.parse("2021-12-31T11:59:59Z")));

      ///////////////////////////////////////////////
      // make sure a timezone does what's expected //
      ///////////////////////////////////////////////
      QContext.getQInstance().setDefaultTimeZoneId("US/Central");
      assertEquals("2020", DateTimeGroupBy.YEAR.makeSelectedString(Instant.parse("2021-01-01T00:00:00Z")));
      assertEquals("2021", DateTimeGroupBy.YEAR.makeSelectedString(Instant.parse("2021-01-01T06:00:00Z")));
      assertEquals("2021", DateTimeGroupBy.YEAR.makeSelectedString(Instant.parse("2021-12-31T11:59:59Z")));
      assertEquals("2021", DateTimeGroupBy.YEAR.makeSelectedString(Instant.parse("2022-01-01T03:00:00Z")));

      ///////////////////////////////////////////////
      // reset to UTC - test the other enum values //
      ///////////////////////////////////////////////
      QContext.getQInstance().setDefaultTimeZoneId("UTC");
      assertEquals("2021-01", DateTimeGroupBy.MONTH.makeSelectedString(Instant.parse("2021-01-01T00:00:00Z")));
      assertEquals("2021W01", DateTimeGroupBy.WEEK.makeSelectedString(Instant.parse("2021-01-01T00:00:00Z")));
      assertEquals("2021W01", DateTimeGroupBy.WEEK.makeSelectedString(Instant.parse("2020-12-31T00:00:00Z")));
      assertEquals("2021-01-01", DateTimeGroupBy.DAY.makeSelectedString(Instant.parse("2021-01-01T00:00:00Z")));
      assertEquals("2021-01-01T00", DateTimeGroupBy.HOUR.makeSelectedString(Instant.parse("2021-01-01T00:00:00Z")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMakeHumanString()
   {
      assertEquals("2021", DateTimeGroupBy.YEAR.makeHumanString(Instant.parse("2021-01-01T00:00:00Z")));
      assertEquals("2021", DateTimeGroupBy.YEAR.makeHumanString(Instant.parse("2021-12-31T11:59:59Z")));

      ///////////////////////////////////////////////
      // make sure a timezone does what's expected //
      ///////////////////////////////////////////////
      QContext.getQInstance().setDefaultTimeZoneId("US/Central");
      assertEquals("2020", DateTimeGroupBy.YEAR.makeHumanString(Instant.parse("2021-01-01T00:00:00Z")));
      assertEquals("2021", DateTimeGroupBy.YEAR.makeHumanString(Instant.parse("2021-01-01T06:00:00Z")));
      assertEquals("2021", DateTimeGroupBy.YEAR.makeHumanString(Instant.parse("2021-12-31T11:59:59Z")));
      assertEquals("2021", DateTimeGroupBy.YEAR.makeHumanString(Instant.parse("2022-01-01T03:00:00Z")));

      ///////////////////////////////////////////////
      // reset to UTC - test the other enum values //
      ///////////////////////////////////////////////
      QContext.getQInstance().setDefaultTimeZoneId("UTC");
      assertEquals("Jan. 2021", DateTimeGroupBy.MONTH.makeHumanString(Instant.parse("2021-01-01T00:00:00Z")));
      assertEquals("12/27-1/2", DateTimeGroupBy.WEEK.makeHumanString(Instant.parse("2021-01-01T00:00:00Z")));
      assertEquals("12/27-1/2", DateTimeGroupBy.WEEK.makeHumanString(Instant.parse("2020-12-31T00:00:00Z")));
      assertEquals("Fri. 1/1", DateTimeGroupBy.DAY.makeHumanString(Instant.parse("2021-01-01T00:00:00Z")));
      assertEquals("12 AM", DateTimeGroupBy.HOUR.makeHumanString(Instant.parse("2021-01-01T00:00:00Z")));
   }

}