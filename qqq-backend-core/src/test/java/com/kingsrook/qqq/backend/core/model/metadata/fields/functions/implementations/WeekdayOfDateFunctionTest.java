/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations;


import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for {@link WeekdayOfDateFunction}
 *******************************************************************************/
class WeekdayOfDateFunctionTest extends BaseTest
{
   private final WeekdayOfDateFunction function = new WeekdayOfDateFunction();



   /***************************************************************************
    ** Verify all 7 days return ISO-8601 values: Monday=1 through Sunday=7
    ***************************************************************************/
   @Test
   void testAllDaysOfWeek() throws Exception
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("date");

      ///////////////////////////////////////////////////////////////
      // 2026-02-23 is a Monday, iterate through the week to Sunday //
      ///////////////////////////////////////////////////////////////
      LocalDate monday = LocalDate.of(2026, 2, 23);
      assertEquals(DayOfWeek.MONDAY, monday.getDayOfWeek());

      for(int i = 0; i < 7; i++)
      {
         LocalDate date = monday.plusDays(i);
         QRecord  record = new QRecord().withValue("date", date);
         int      expected = i + 1; // Monday=1, Tuesday=2, ... Sunday=7
         assertEquals(expected, function.apply(ff, record), "Day " + date.getDayOfWeek());
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testNullSource() throws Exception
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("date");

      assertNull(function.apply(ff, new QRecord().withValue("date", null)));
   }



   /***************************************************************************
    ** With sundayFirst=true, applyForSorting should return value % 7,
    ** mapping Sunday(7) -> 0, Monday(1) -> 1, etc.
    ***************************************************************************/
   @Test
   void testApplyForSortingSundayFirst() throws Exception
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("date")
         .withArguments(Map.of(WeekdayOfDateFunction.PARAM_SORT_SUNDAY_FIRST, true));

      //////////////////////////////////////////////
      // Sunday(7) % 7 = 0, should sort first     //
      // Monday(1) % 7 = 1, should sort second    //
      //////////////////////////////////////////////
      LocalDate sunday = LocalDate.of(2026, 3, 1);
      assertEquals(DayOfWeek.SUNDAY, sunday.getDayOfWeek());
      assertEquals(0, function.applyForSorting(ff, new QRecord().withValue("date", sunday)));

      LocalDate monday = LocalDate.of(2026, 2, 23);
      assertEquals(DayOfWeek.MONDAY, monday.getDayOfWeek());
      assertEquals(1, function.applyForSorting(ff, new QRecord().withValue("date", monday)));

      LocalDate saturday = LocalDate.of(2026, 2, 28);
      assertEquals(DayOfWeek.SATURDAY, saturday.getDayOfWeek());
      assertEquals(6, function.applyForSorting(ff, new QRecord().withValue("date", saturday)));
   }



   /***************************************************************************
    ** With sundayFirst=false, applyForSorting should return the raw value
    ** (same as apply), so Monday(1) sorts first naturally.
    ***************************************************************************/
   @Test
   void testApplyForSortingMondayFirst() throws Exception
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("date")
         .withArguments(Map.of(WeekdayOfDateFunction.PARAM_SORT_SUNDAY_FIRST, false));

      LocalDate monday = LocalDate.of(2026, 2, 23);
      assertEquals(1, function.applyForSorting(ff, new QRecord().withValue("date", monday)));

      LocalDate sunday = LocalDate.of(2026, 3, 1);
      assertEquals(7, function.applyForSorting(ff, new QRecord().withValue("date", sunday)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testApplyForSortingNullSource() throws Exception
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateFunction.IDENTIFIER)
         .withFieldName("date")
         .withArguments(Map.of(WeekdayOfDateFunction.PARAM_SORT_SUNDAY_FIRST, true));

      assertNull(function.applyForSorting(ff, new QRecord().withValue("date", null)));
   }

}
