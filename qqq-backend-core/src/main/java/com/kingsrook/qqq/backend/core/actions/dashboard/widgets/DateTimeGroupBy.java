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


import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Enum to define various "levels" of group-by for on dashboards that want to
 ** group records by, e.g., year, or month, or week, or day, or hour.
 *******************************************************************************/
public enum DateTimeGroupBy
{
   /////////////////////////////////////////////////////////////////////////////////////////////////////////////
   // note - double %'s on the time format strings here, because this is a java-format string, which will get //
   // its '%s' replaced with a column name, and so then those %'s for the date_format need escaped as %%.     //
   // See https://www.w3schools.com/sql/func_mysql_date_format.asp for DATE_FORMAT args                       //
   /////////////////////////////////////////////////////////////////////////////////////////////////////////////
   YEAR("%%Y", MillisPer.YEAR, 1, ChronoUnit.YEARS, DateTimeFormatter.ofPattern("yyyy"), DateTimeFormatter.ofPattern("yyyy")),
   MONTH("%%Y-%%m", 2 * MillisPer.MONTH, 1, ChronoUnit.MONTHS, DateTimeFormatter.ofPattern("yyyy-MM"), DateTimeFormatter.ofPattern("MMM'.' yyyy")),
   WEEK("%%XW%%V", 35 * MillisPer.DAY, 7, ChronoUnit.DAYS, DateTimeFormatter.ofPattern("YYYY'W'ww"), DateTimeFormatter.ofPattern("YYYY'W'w")),
   DAY("%%Y-%%m-%%d", 36 * MillisPer.HOUR, 1, ChronoUnit.DAYS, DateTimeFormatter.ofPattern("yyyy-MM-dd"), DateTimeFormatter.ofPattern("EEE'.' M'/'d")),
   HOUR("%%Y-%%m-%%dT%%H", 0, 1, ChronoUnit.HOURS, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH"), DateTimeFormatter.ofPattern("h a"));



   /*******************************************************************************
    **
    *******************************************************************************/
   public interface MillisPer
   {
      long HOUR  = 60 * 60 * 1000;
      long DAY   = 24 * HOUR;
      long WEEK  = 7 * DAY;
      long MONTH = 30 * DAY;
      long YEAR  = 365 * DAY;
   }



   private final String            sqlDateFormat;
   private final long              millisThreshold;
   private final int               noOfChronoUnitsToAdd;
   private final ChronoUnit        chronoUnitToAdd;
   private final DateTimeFormatter selectedStringFormatter;
   private final DateTimeFormatter humanStringFormatter;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   DateTimeGroupBy(String sqlDateFormat, long millisThreshold, int noOfChronoUnitsToAdd, ChronoUnit chronoUnitToAdd, DateTimeFormatter selectedStringFormatter, DateTimeFormatter humanStringFormatter)
   {
      this.sqlDateFormat = sqlDateFormat;
      this.millisThreshold = millisThreshold;
      this.noOfChronoUnitsToAdd = noOfChronoUnitsToAdd;
      this.chronoUnitToAdd = chronoUnitToAdd;
      this.selectedStringFormatter = selectedStringFormatter;
      this.humanStringFormatter = humanStringFormatter;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getSqlExpression()
   {
      ZoneId sessionOrInstanceZoneId = ValueUtils.getSessionOrInstanceZoneId();
      return (getSqlExpression(sessionOrInstanceZoneId));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getSqlExpression(ZoneId targetZoneId)
   {
      String targetTimezone = targetZoneId.toString();

      if("Z".equals(targetTimezone) || !StringUtils.hasContent(targetTimezone))
      {
         targetTimezone = "UTC";
      }

      //////////////////////////////////////////////////////////////////////////////////////////////////////
      // if we only had a timezone offset (not a zone name/id), then the zoneId's toString will look like //
      // UTC-05:00.  MySQL doesn't want that, so, strip away the leading UTC, to just get -05:00          //
      //////////////////////////////////////////////////////////////////////////////////////////////////////
      if((targetTimezone.startsWith("UTC-") || targetTimezone.startsWith("UTC+")) && targetTimezone.length() > 5)
      {
         targetTimezone = targetTimezone.substring(3);
      }

      return "DATE_FORMAT(CONVERT_TZ(%s, 'UTC', '" + targetTimezone + "'), '" + sqlDateFormat + "')";

      /*
      if(this == WEEK)
      {
         return "YEARWEEK(CONVERT_TZ(%s, 'UTC', '" + targetTimezone + "'), 6)";
      }
      else
      {
         return "DATE_FORMAT(CONVERT_TZ(%s, 'UTC', '" + targetTimezone + "'), '" + sqlDateFormat + "')";
      }
      */
   }



   /*******************************************************************************
    ** get an instance of this enum, based on start & end instants - look at the #
    ** of millis between them, and return the first enum value w/ a millisThreshold
    ** under that difference.  Default to HOUR.
    *******************************************************************************/
   public static DateTimeGroupBy selectFromStartAndEndTimes(Instant start, Instant end)
   {
      long millisBetween = end.toEpochMilli() - start.toEpochMilli();
      for(DateTimeGroupBy value : DateTimeGroupBy.values())
      {
         if(millisBetween > value.millisThreshold)
         {
            return (value);
         }
      }

      return (HOUR);
   }



   /*******************************************************************************
    ** Make an Instant into a string that will match what came out of the database's
    ** DATE_FORMAT() function
    *******************************************************************************/
   public String makeSelectedString(Instant time)
   {
      return (makeSelectedString(time, ValueUtils.getSessionOrInstanceZoneId()));
   }



   /*******************************************************************************
    ** Make an Instant into a string that will match what came out of the database's
    ** DATE_FORMAT() function
    *******************************************************************************/
   public String makeSelectedString(Instant time, ZoneId zoneId)
   {
      ZonedDateTime zoned = time.atZone(zoneId);

      if(this == WEEK)
      {
         ////////////////////////////////////////////////////////////////////////////////////////////////////
         // so, it seems like database is returning, e.g., W00-W52, but java is doing W1-W53...            //
         // which, apparently we can compensate for by adding a week?  not sure, but results seemed right. //
         ////////////////////////////////////////////////////////////////////////////////////////////////////
         zoned = zoned.plusDays(7);
         int weekYear = zoned.get(IsoFields.WEEK_BASED_YEAR);
         int week     = zoned.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
         return (String.format("%04dW%02d", weekYear, week));
      }

      return (selectedStringFormatter.format(zoned));
   }



   /*******************************************************************************
    ** Make a string to show to a user
    *******************************************************************************/
   public String makeHumanString(Instant instant)
   {
      return (makeHumanString(instant, ValueUtils.getSessionOrInstanceZoneId()));
   }



   /*******************************************************************************
    ** Make a string to show to a user
    *******************************************************************************/
   public String makeHumanString(Instant instant, ZoneId zoneId)
   {
      ZonedDateTime zoned = instant.atZone(zoneId);
      if(this.equals(WEEK))
      {
         DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("M'/'d");

         while(zoned.get(ChronoField.DAY_OF_WEEK) != DayOfWeek.SUNDAY.getValue())
         {
            ////////////////////////////////////////
            // go backwards until sunday is found //
            ////////////////////////////////////////
            zoned = zoned.minus(1, ChronoUnit.DAYS);
         }

         return (dateTimeFormatter.format(zoned) + "-" + dateTimeFormatter.format(zoned.plusDays(6)));

         /*
         int               weekOfYear        = zoned.get(ChronoField.ALIGNED_WEEK_OF_YEAR);
         ZonedDateTime     sunday            = zoned.with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, weekOfYear).with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
         ZonedDateTime     saturday          = sunday.with(TemporalAdjusters.next(DayOfWeek.SATURDAY));
         DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("M'/'d");

         return (dateTimeFormatter.format(sunday) + "-" + dateTimeFormatter.format(saturday));
         */
      }

      return (humanStringFormatter.format(zoned));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Instant roundDown(Instant instant)
   {
      return roundDown(instant, ValueUtils.getSessionOrInstanceZoneId());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Instant roundDown(Instant instant, ZoneId zoneId)
   {
      ZonedDateTime zoned = instant.atZone(zoneId);
      return switch(this)
      {
         case YEAR -> zoned.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS).toInstant();
         case MONTH -> zoned.with(TemporalAdjusters.firstDayOfMonth()).truncatedTo(ChronoUnit.DAYS).toInstant();
         case WEEK ->
         {
            while(zoned.get(ChronoField.DAY_OF_WEEK) != DayOfWeek.SUNDAY.getValue())
            {
               zoned = zoned.minusDays(1);
            }
            yield (zoned.truncatedTo(ChronoUnit.DAYS).toInstant());
         }
         case DAY -> zoned.truncatedTo(ChronoUnit.DAYS).toInstant();
         case HOUR -> zoned.truncatedTo(ChronoUnit.HOURS).toInstant();
      };
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Instant increment(Instant instant)
   {
      return (increment(instant, ValueUtils.getSessionOrInstanceZoneId()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Instant increment(Instant instant, ZoneId zoneId)
   {
      ZonedDateTime zoned = instant.atZone(zoneId);
      return (zoned.plus(noOfChronoUnitsToAdd, chronoUnitToAdd).toInstant());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static DateTimeFormatter sqlDateFormatToSelectedDateTimeFormatter(String sqlDateFormat)
   {
      for(DateTimeGroupBy value : values())
      {
         if(value.sqlDateFormat.equals(sqlDateFormat))
         {
            return (value.selectedStringFormatter);
         }
      }
      return null;
   }
}
