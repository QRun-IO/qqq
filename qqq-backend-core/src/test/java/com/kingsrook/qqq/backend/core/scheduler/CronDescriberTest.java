/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.scheduler;


import java.text.ParseException;
import java.util.List;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.scheduler.CronDescriber.Field;
import com.kingsrook.qqq.backend.core.scheduler.CronDescriber.Range;
import com.kingsrook.qqq.backend.core.scheduler.CronDescriber.Scalar;
import com.kingsrook.qqq.backend.core.scheduler.CronDescriber.Step;
import org.junit.jupiter.api.Test;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for CronDescriber 
 *******************************************************************************/
class CronDescriberTest extends BaseTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws ParseException
   {
      ////////////////////////////////////////////////////
      // quartz: sec min hour | date month weekday year //
      ////////////////////////////////////////////////////
      assertDescription("Every day, every second", "* * * * * ?");
      assertDescription("Every day, every second", "* * * ? * *");
      assertDescription("Every day, every minute", "0 * * * * ?");
      assertDescription("Every day, every hour, at minute 00", "0 0 * * * ?");
      assertDescription("Every day, every hour, at minutes 00 and 30", "0 0,30 * * * ?");
      assertDescription("Every day, at 12:00 am", "0 0 0 * * ?");
      assertDescription("Every day, at 1:00 am", "0 0 1 * * ?");
      assertDescription("Every day, at 11:00 am", "0 0 11 * * ?");
      assertDescription("Every day, at 12:00 pm", "0 0 12 * * ?");
      assertDescription("Every day, at 1:00 pm", "0 0 13 * * ?");
      assertDescription("Every day, at 11:00 pm", "0 0 23 * * ?");
      assertDescription("Every day, at 11:00 pm", "0 0 23, * * ?");
      assertDescription("Every month, on the 10th, at 12:00 am", "0 0 0 10 * ?");
      assertDescription("Every month, on the 10th and 20th, at 12:00 am", "0 0 0 10,20 * ?");
      assertDescription("Every month, every day between the 10th and 15th, at 12:00 am", "0 0 0 10-15 * ?");
      assertDescription("Every day, at 12:00 am, every second between 10 and 15", "10-15 0 0 * * ?");
      assertDescription("Every day, every hour between 8am and 4pm, at minute 30, at second 30", "30 30 8-16 * * ?");
      assertDescription("Every day, at 12:00 am, every 5 seconds between 00 and 59", "0/5 0 0 * * ?");
      assertDescription("Every day, at 12am, every 30 minutes between 03 and 59", "0 3/30 0 * * ?");
      assertDescription("Every week, on Monday, Wednesday, and Friday, at 12:00 am", "0 0 0 ? * MON,WED,FRI");
      assertDescription("Every week, every day between Monday and Friday, at 12:00 am", "0 0 0 ? * MON-FRI");
      assertDescription("Every week, on Sunday and Saturday, at 12:00 am", "0 0 0 ? * 1,7");
      assertDescription("Every day, at 2:05 am, 6:05 am, 12:05 pm, 4:05 pm, and 8:05 pm", "0 5 2,6,12,16,20 * * ?");
      assertDescription("Every day, at 2:15:30 am and 6:15:30 am", "30 15 2,6 * * ?");
      assertDescription("Every year between 2002 and 2010, in January, March, and September, every day between Monday and Friday, every hour, at minutes 14, 18, every minute between 03 and 39, and at minute 52, every 5 seconds between 00 and 59", "0/5 14,18,3-39,52 * ? JAN,MAR,SEP MON-FRI 2002-2010");
      assertDescription("Every month between January and June, every second", "* * * ? 1-6 *");
      assertDescription("In March and every 3 months between January and July, every second", "* * * ? 3,1-7/3 *");
      assertDescription("Every month, on the 1st, 3rd, and 5th, every second", "* * * 1,3,5 * ?");
      assertDescription("Every month, on the 1st, 3rd, 5th, and every day between the 7th and 9th, every second", "* * * 1,3,5,7-9 * ?");
      assertDescription("Every month, on the 1st, 3rd, and 5th, every hour between 2am and 4am and at 3pm, every second", "* * 2-4,15 1,3,5 * ?");
      assertDescription("In January, on the 1st, at 12:00 am", "0 0 0 1 1 ?");
      // todo might be good for single-date expressions: assertDescription("On January 1st, at 12:00 am", "0 0 0 1 1 ?");

      // 0 5,10 6 ? * Thu:
      // is: Every week, on Thursday, at 6am, at minutes 05 and 10
      // could be: Every week, on Thursday, at 6:05 am and 6:10 am

      String bothDayFieldsStar = "0 0 0 * * *";
      assertThatThrownBy(() -> CronScheduleBuilder.cronScheduleNonvalidatedExpression(bothDayFieldsStar)).hasMessageContaining("both");
      assertThatThrownBy(() -> CronDescriber.getDescription(bothDayFieldsStar)).hasMessageContaining("both").hasMessageContaining("one must be");

      String bothDayFields1 = "0 0 0 1 * 1";
      assertThatThrownBy(() -> CronScheduleBuilder.cronScheduleNonvalidatedExpression(bothDayFields1)).hasMessageContaining("both");
      assertThatThrownBy(() -> CronDescriber.getDescription(bothDayFields1)).hasMessageContaining("both").hasMessageContaining("one must be");

      String bothDayFieldsQuestion = "0 0 0 ? * ?";
      assertThatThrownBy(() -> CronScheduleBuilder.cronScheduleNonvalidatedExpression(bothDayFieldsQuestion)).hasMessageContaining("can only be");
      assertThatThrownBy(() -> CronDescriber.getDescription(bothDayFieldsQuestion)).hasMessageContaining("both").hasMessageContaining("only one can be");
   }



   /***************************************************************************
    *
    ***************************************************************************/
   void assertDescription(String expected, String expression) throws ParseException
   {
      assertEquals(expected, CronDescriber.getDescription(expression));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTimePhrases() throws ParseException
   {
      assertEquals(List.of("every second"), CronDescriber.buildTimePhrases("*", "*", "*"));
      assertEquals(List.of("every minute"), CronDescriber.buildTimePhrases("0", "*", "*"));
      assertEquals(List.of("every hour", "at minute 00"), CronDescriber.buildTimePhrases("0", "0", "*"));
      assertEquals(List.of("every hour", "at minute 00", "at second 05"), CronDescriber.buildTimePhrases("05", "0", "*"));
      assertEquals(List.of("every hour", "every minute between 00 and 15", "at second 05"), CronDescriber.buildTimePhrases("05", "0-15", "*"));
      assertEquals(List.of("every hour", "every minute between 00 and 15", "every 5 seconds between 05 and 55"), CronDescriber.buildTimePhrases("05-55/5", "0-15", "*"));
      assertEquals(List.of("every hour", "at minute 00", "every 3 seconds between 00 and 10, at seconds 15, 17, 24, and every second between 40 and 57"), CronDescriber.buildTimePhrases("0-10/3,15,17,24,40-57", "0", "*"));
      assertEquals(List.of("every hour between 8am and 4pm", "at minute 00"), CronDescriber.buildTimePhrases("0", "0", "8-16"));
      assertEquals(List.of("at 12:00 am"), CronDescriber.buildTimePhrases("0", "0", "0"));
      assertEquals(List.of("at 11:59:59 pm"), CronDescriber.buildTimePhrases("59", "59", "23"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDayPhrases() throws ParseException
   {
      assertEquals(List.of("every day"), CronDescriber.buildDayPhrases("*", "*", "?", "*"));
      assertEquals(List.of("every day"), CronDescriber.buildDayPhrases("?", "*", "*", "*"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testYearPhrase() throws ParseException
   {
      assertEquals("in the year 2000", CronDescriber.buildYearPhrase("2000"));
      assertEquals("every year between 2000 and 2005", CronDescriber.buildYearPhrase("2000-2005"));
      assertEquals("in the years 2000, 2004, and 2008", CronDescriber.buildYearPhrase("2000,2004,2008"));
      assertEquals("every 4 years between 2000 and 2100", CronDescriber.buildYearPhrase("2000-2100/4"));
      assertEquals("every year between 2000 and 2100", CronDescriber.buildYearPhrase("2000-2100/1"));
      assertEquals("in the year 2000", CronDescriber.buildYearPhrase("2000-2000/5"));

      String complexYears = "2000,2010-2020,2030-2100/10";
      assertEquals("in the year 2000, every year between 2010 and 2020, and every 10 years between 2030 and 2100", CronDescriber.buildYearPhrase(complexYears));
      assertThat(new CronExpression("0 0 0 1 1 ? " + complexYears).getExpressionSummary()).contains("2000,2010,2011,2012,2013,2014,2015,2016,2017,2018,2019,2020,2030,2040,2050,2060,2070,2080,2090,2100");
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Test
   void testMonthPhrase() throws ParseException
   {
      assertEquals("in January", CronDescriber.buildMonthPhrase("Jan"));
      assertEquals("every month between January and March", CronDescriber.buildMonthPhrase("Jan-Mar"));
      assertEquals("in January, March, and May", CronDescriber.buildMonthPhrase("1,3,5"));
      assertEquals("every 2 months between January and September", CronDescriber.buildMonthPhrase("Jan-Sep/2"));
      assertEquals("every month between January and November", CronDescriber.buildMonthPhrase("Jan-Nov/1"));
      assertEquals("every month between May and December", CronDescriber.buildMonthPhrase("May/1"));
      assertEquals("in May", CronDescriber.buildMonthPhrase("May-May/1"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWeekdayPhrase() throws ParseException
   {
      assertEquals("on Monday", CronDescriber.buildWeekdayPhrase("Mon"));
      assertEquals("every day between Monday and Friday", CronDescriber.buildWeekdayPhrase("MON-FRI"));
      assertEquals("on Monday, Thursday, and Saturday", CronDescriber.buildWeekdayPhrase("2,5,7"));
      assertEquals("every 2 days between Monday and Saturday", CronDescriber.buildWeekdayPhrase("Mon-Sat/2"));
      assertEquals("every day between Monday and Friday", CronDescriber.buildWeekdayPhrase("Mon-Fri/1"));
      assertEquals("every day between Wednesday and Saturday", CronDescriber.buildWeekdayPhrase("4/1"));
      assertEquals("on Thursday", CronDescriber.buildWeekdayPhrase("5-5/1"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testDayOfMonthPhrase() throws ParseException
   {
      assertEquals("on the 1st", CronDescriber.buildDayOfMonthPhrase("1"));
      assertEquals("every day between the 2nd and 5th", CronDescriber.buildDayOfMonthPhrase("2-5"));
      assertEquals("on the 3rd, 4th, and 7th", CronDescriber.buildDayOfMonthPhrase("3,4,7"));
      assertEquals("every 2 days between the 6th and 21st", CronDescriber.buildDayOfMonthPhrase("6-21/2"));
      assertEquals("every day between the 7th and 22nd", CronDescriber.buildDayOfMonthPhrase("7-22/1"));
      assertEquals("every day between the 21st and 31st", CronDescriber.buildDayOfMonthPhrase("21/1"));
      assertEquals("on the 11th", CronDescriber.buildDayOfMonthPhrase("11-11/1"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTokenize() throws ParseException
   {
      assertEquals(List.of(new Scalar(30)), CronDescriber.tokenize("30", Field.MINUTES));
      assertEquals(List.of(new Scalar(15), new Scalar(30), new Scalar(45)), CronDescriber.tokenize("15,30,45", Field.MINUTES));
      assertEquals(List.of(new Range(1, 3)), CronDescriber.tokenize("1-3", Field.MINUTES));
      assertEquals(List.of(new Range(1, 3), new Scalar(5), new Range(7, 9)), CronDescriber.tokenize("1-3,5,7-9", Field.MINUTES));

      ////////////////////////////////////////////
      // multiple commas are gracefully ignored //
      ////////////////////////////////////////////
      String oneCommaComma = "2,,";
      CronScheduleBuilder.cronScheduleNonvalidatedExpression("0 0 0 ? " + oneCommaComma + " *"); // asserting no exception
      assertThat(new CronExpression("0 0 0 ? " + oneCommaComma + " *").getExpressionSummary()).contains("months: 2");
      assertEquals(List.of(new Scalar(2)), CronDescriber.tokenize(oneCommaComma, Field.MONTH));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testStepOf() throws ParseException
   {
      assertEquals(new Step(new Range(1, 10), 2), Step.of("1-10/2", Field.HOURS));
      assertEquals(new Step(new Range(0, 59), 3), Step.of("*/3", Field.MINUTES));
      assertEquals(new Step(new Range(3, 59), 4), Step.of("3/4", Field.MINUTES));
      assertEquals(new Scalar(5), Step.of("5-5/5", Field.MINUTES));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testRangeOf() throws ParseException
   {
      assertEquals(new Range(0, 59), Range.of("*", Field.SECONDS));
      assertEquals(new Range(1, 31), Range.of("*", Field.DAY_OF_MONTH));

      assertEquals(new Range(1, 58), Range.of("1-58", Field.MINUTES));

      assertEquals(new Range(2, 11), Range.of("Feb-Nov", Field.MONTH));
      assertEquals(new Range(1, 4), Range.of("SUN-wed", Field.DAY_OF_WEEK));

      assertEquals(new Range(5, 12), Range.of("5", Field.MONTH)); // useful for a step expression without a - (e.g., 5/2 for 5,7,9,...)

      ///////////////////////////////////////////////////////////////////////
      // special case that return a range of 1, which we convert to scalar //
      ///////////////////////////////////////////////////////////////////////
      assertEquals(new Scalar(5), Range.of("5-5", Field.MINUTES));

      /////////////////
      // error cases //
      /////////////////
      assertThatThrownBy(() -> Range.of("A-B", Field.SECONDS)).isInstanceOf(ParseException.class).hasMessageContaining("Invalid number: [A]");
      assertThatThrownBy(() -> Range.of("1-2-3", Field.SECONDS)).isInstanceOf(ParseException.class).hasMessageContaining("Invalid number: [2-3]");
      assertThatThrownBy(() -> Range.of("1-", Field.SECONDS)).isInstanceOf(ParseException.class).hasMessageContaining("Incomplete range expression: [1-]");
      assertThatThrownBy(() -> Range.of("-1", Field.SECONDS)).isInstanceOf(ParseException.class).hasMessageContaining("Incomplete range expression: [-1]");
      assertThatThrownBy(() -> Range.of("-", Field.SECONDS)).isInstanceOf(ParseException.class).hasMessageContaining("Incomplete range expression: [-]");

      ///////////////////////////////////////////////////////////
      // use cases I didn't expect, but trying to match quartz //
      // number-*: raises an exception (makes sense)           //
      ///////////////////////////////////////////////////////////
      String numberDashStar = "3-*";
      assertThatThrownBy(() -> CronScheduleBuilder.cronScheduleNonvalidatedExpression("0 0 0 ? 1 " + numberDashStar)).hasMessageContaining("For input string: \"*\"");
      assertThatThrownBy(() -> Range.of(numberDashStar, Field.DAY_OF_WEEK)).hasMessageContaining("Invalid day of week: [*]");
      assertThatThrownBy(() -> CronScheduleBuilder.cronScheduleNonvalidatedExpression("0 0 " + numberDashStar + " ? 1 1")).hasMessageContaining("For input string: \"*\"");
      assertThatThrownBy(() -> Range.of(numberDashStar, Field.HOURS)).hasMessageContaining("Invalid number: [*]");

      /////////////////////////////////////////////////////
      // *-number: interprets as "all" (not expected...) //
      /////////////////////////////////////////////////////
      String starDashNumber = "*-3";
      CronScheduleBuilder.cronScheduleNonvalidatedExpression("0 0 0 ? 1 " + starDashNumber); // asserting no exception
      assertThat(new CronExpression("0 0 0 ? 1 " + starDashNumber).getExpressionSummary()).contains("daysOfWeek: 1,2,3,4,5,6,7");
      assertEquals(new Range(1, 7), Range.of(starDashNumber, Field.DAY_OF_WEEK));

      ////////////////////////////////////////////////////////
      // wraparounds (e.g., high-low (instead of low-high)) //
      ////////////////////////////////////////////////////////
      String wrapAround = "6-2";
      CronScheduleBuilder.cronScheduleNonvalidatedExpression("0 0 0 ? 1 " + wrapAround); // asserting no exception
      assertThat(new CronExpression("0 0 0 ? 1 " + wrapAround).getExpressionSummary()).contains("daysOfWeek: 1,2,6,7");
      assertEquals(new Range(6, 2), Range.of(wrapAround, Field.DAY_OF_WEEK));

      //////////////////////////////////////////
      // can't mix number-word or word-number //
      //////////////////////////////////////////
      String numberDashWord = "2-THU";
      assertThatThrownBy(() -> CronScheduleBuilder.cronScheduleNonvalidatedExpression("0 0 0 ? 1 " + numberDashWord)).hasMessageContaining("For input string: \"T\"");
      assertThatThrownBy(() -> Range.of(numberDashWord, Field.DAY_OF_WEEK)).hasMessageContaining("Range may not mix words and numbers: [2-THU]");

      String wordDashNumber = "MON-4";
      assertThatThrownBy(() -> CronScheduleBuilder.cronScheduleNonvalidatedExpression("0 0 0 ? 1 " + wordDashNumber)).hasMessageContaining("StringIndexOutOfBounds");
      assertThatThrownBy(() -> Range.of(wordDashNumber, Field.DAY_OF_WEEK)).hasMessageContaining("Range may not mix words and numbers: [MON-4]");

      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // despite not being documented, full names seem to work for the second part of a range, because it does substr(3) apparently //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      String monTues = "Mon-Tues";
      CronScheduleBuilder.cronScheduleNonvalidatedExpression("0 0 0 ? 1 " + monTues); // asserting no exception
      assertThat(new CronExpression("0 0 0 ? 1 " + monTues).getExpressionSummary()).contains("daysOfWeek: 2,3");
      assertEquals(new Range(2, 3), Range.of(monTues, Field.DAY_OF_WEEK));

      String tuesWed = "Tues-Wed";
      CronScheduleBuilder.cronScheduleNonvalidatedExpression("0 0 0 ? 1 " + tuesWed); // asserting no exception
      assertThat(new CronExpression("0 0 0 ? 1 " + tuesWed).getExpressionSummary()).contains("daysOfWeek: 3");
      assertEquals(new Scalar(3), Range.of(tuesWed, Field.DAY_OF_WEEK));

      ///////////////////////////////////////////////////////////////////
      // test bed - useful to leave here working through new use cases //
      ///////////////////////////////////////////////////////////////////
      /*
      String second         = "0";
      String minute         = "0";
      String hour           = "0";
      String dayOfMonth     = "*";
      String month          = "April-June";
      String dayOfWeek      = "*";
      String cronExpression = second + " " + minute + " " + hour + " " + dayOfMonth + " " + month + " " + dayOfWeek;
      CronScheduleBuilder.cronScheduleNonvalidatedExpression(cronExpression);
      System.out.println(new CronExpression(cronExpression).getExpressionSummary());
      */
   }

}
