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


import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


/*******************************************************************************
 ** Unit tests for {@link WeekdayOfDateTimeFunction}
 *******************************************************************************/
class WeekdayOfDateTimeFunctionTest extends BaseTest
{
   private final WeekdayOfDateTimeFunction function = new WeekdayOfDateTimeFunction();



   /***************************************************************************
    ** Basic test with a known timestamp in UTC
    ***************************************************************************/
   @Test
   void testBasic() throws Exception
   {
      /////////////////////////////////////////
      // 2026-02-23 12:00:00 UTC is a Monday //
      /////////////////////////////////////////
      Instant mondayNoonUtc = LocalDateTime.of(2026, 2, 23, 12, 0, 0)
         .toInstant(ZoneOffset.UTC);

      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("timestamp")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC"));

      assertEquals(1, function.apply(ff, new QRecord().withValue("timestamp", mondayNoonUtc)));
   }



   /***************************************************************************
    ** Test that timezone affects the weekday result.
    ** 23:00 UTC on Monday = 01:00 Tuesday in Europe/Athens (UTC+2)
    ***************************************************************************/
   @Test
   void testTimezoneBoundary() throws Exception
   {
      Instant mondayLateUtc = LocalDateTime.of(2026, 2, 23, 23, 0, 0)
         .toInstant(ZoneOffset.UTC);

      ////////////////////////////////
      // In UTC, this is Monday (1) //
      ////////////////////////////////
      FieldFunction ffUtc = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("timestamp")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC"));

      assertEquals(1, function.apply(ffUtc, new QRecord().withValue("timestamp", mondayLateUtc)));

      ///////////////////////////////////////////////////
      // In Europe/Athens (UTC+2), this is Tuesday (2) //
      ///////////////////////////////////////////////////
      FieldFunction ffAthens = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("timestamp")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "Europe/Athens"));

      assertEquals(2, function.apply(ffAthens, new QRecord().withValue("timestamp", mondayLateUtc)));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Test
   void testNullSource() throws Exception
   {
      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("timestamp")
         .withArguments(Map.of(WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC"));

      assertNull(function.apply(ff, new QRecord().withValue("timestamp", null)));
   }



   /***************************************************************************
    ** With sundayFirst=true, applyForSorting should map Sunday(7) -> 0
    ***************************************************************************/
   @Test
   void testApplyForSortingSundayFirst() throws Exception
   {
      //////////////////////////////////////
      // 2026-03-01 12:00 UTC is a Sunday //
      //////////////////////////////////////
      Instant sundayNoonUtc = LocalDateTime.of(2026, 3, 1, 12, 0, 0)
         .toInstant(ZoneOffset.UTC);

      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("timestamp")
         .withArguments(Map.of(
            WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC",
            WeekdayOfDateTimeFunction.PARAM_SORT_SUNDAY_FIRST, true
         ));

      assertEquals(0, function.applyForSorting(ff, new QRecord().withValue("timestamp", sundayNoonUtc)));

      /////////////////////////////////////////////////
      // Monday should sort as 1                      //
      /////////////////////////////////////////////////
      Instant mondayNoonUtc = LocalDateTime.of(2026, 2, 23, 12, 0, 0)
         .toInstant(ZoneOffset.UTC);

      assertEquals(1, function.applyForSorting(ff, new QRecord().withValue("timestamp", mondayNoonUtc)));
   }



   /***************************************************************************
    ** With sundayFirst=false, applyForSorting should return raw value
    ***************************************************************************/
   @Test
   void testApplyForSortingMondayFirst() throws Exception
   {
      Instant sundayNoonUtc = LocalDateTime.of(2026, 3, 1, 12, 0, 0)
         .toInstant(ZoneOffset.UTC);

      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("timestamp")
         .withArguments(Map.of(
            WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC",
            WeekdayOfDateTimeFunction.PARAM_SORT_SUNDAY_FIRST, false
         ));

      assertEquals(7, function.applyForSorting(ff, new QRecord().withValue("timestamp", sundayNoonUtc)));
   }



   /***************************************************************************
    ** Test that zoneIdFromFieldName reads the timezone from another field
    ** on the record, overriding the static timeZoneId argument.
    ***************************************************************************/
   @Test
   void testZoneIdFromFieldName() throws Exception
   {
      ///////////////////////////////////////////////////////////////////////////////
      // 23:00 UTC on Monday 2026-02-23 = 01:00 Tuesday in Europe/Athens (UTC+2) //
      ///////////////////////////////////////////////////////////////////////////////
      Instant mondayLateUtc = LocalDateTime.of(2026, 2, 23, 23, 0, 0)
         .toInstant(ZoneOffset.UTC);

      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("timestamp")
         .withArguments(Map.of(
            WeekdayOfDateTimeFunction.PARAM_ZONE_ID_FROM_FIELD_NAME, "timeZone",
            WeekdayOfDateTimeFunction.PARAM_USE_SESSION_ZONE_ID, false));

      ///////////////////////////////////////////////////////////////////////////
      // with timeZone field = "Europe/Athens", should cross into Tuesday (2) //
      ///////////////////////////////////////////////////////////////////////////
      QRecord athensRecord = new QRecord()
         .withValue("timestamp", mondayLateUtc)
         .withValue("timeZone", "Europe/Athens");
      assertEquals(2, function.apply(ff, athensRecord));

      ///////////////////////////////////////////////
      // with timeZone field = "UTC", stays Monday //
      ///////////////////////////////////////////////
      QRecord utcRecord = new QRecord()
         .withValue("timestamp", mondayLateUtc)
         .withValue("timeZone", "UTC");
      assertEquals(1, function.apply(ff, utcRecord));
   }



   /***************************************************************************
    ** When zoneIdFromFieldName is set but the field value on the record is
    ** null, should fall back to the resolved default timezone.
    ***************************************************************************/
   @Test
   void testZoneIdFromFieldNameFallsBackWhenNull() throws Exception
   {
      Instant mondayLateUtc = LocalDateTime.of(2026, 2, 23, 23, 0, 0)
         .toInstant(ZoneOffset.UTC);

      FieldFunction ff = new FieldFunction()
         .withFunctionTypeIdentifier(WeekdayOfDateTimeFunction.IDENTIFIER)
         .withFieldName("timestamp")
         .withArguments(Map.of(
            WeekdayOfDateTimeFunction.PARAM_ZONE_ID_FROM_FIELD_NAME, "timeZone",
            WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID, "UTC"));

      ///////////////////////////////////////////////////////////
      // null timeZone field — should fall back to "UTC" param //
      ///////////////////////////////////////////////////////////
      QRecord record = new QRecord()
         .withValue("timestamp", mondayLateUtc)
         .withValue("timeZone", null);
      assertEquals(1, function.apply(ff, record), "null timezone should fall back to UTC → Monday");
   }

}
