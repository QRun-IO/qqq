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


import java.io.Serializable;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Filter expression for the start of a calendar period (day, week, month,
 ** quarter or year), in the user's time zone.  The period is the one containing
 ** "today", moved by an offset of whole periods: offset 0 is this period, -1 the
 ** previous one, +1 the next one.
 **
 ** Two of these make a half-open range that has no 23:59:59 edge cases - e.g.,
 ** "last month" is {@code >= StartOfPeriod(MONTHS, -1)} and
 ** {@code < StartOfPeriod(MONTHS, 0)}.
 **
 ** For a DATE_TIME field, evaluates to the Instant of local midnight at the start
 ** of the period (so a daylight-saving change never shifts it).  For a DATE field,
 ** evaluates to the LocalDate of the first day of the period.
 **
 ** The time zone is, in order: the expression's timeZoneId, if given; else the
 ** session's (X-QQQ-UserTimezone), else the instance's default zone.  The first
 ** day of a week is the expression's weekStart, if given, else Sunday (as for
 ** {@link ThisOrLastPeriod}).
 *******************************************************************************/
public class StartOfPeriod extends AbstractFilterExpression<Serializable>
{
   private Unit      unit   = Unit.DAYS;
   private int       offset = 0;
   private DayOfWeek weekStart;
   private String    timeZoneId;



   /***************************************************************************
    **
    ***************************************************************************/
   public enum Unit
   {DAYS, WEEKS, MONTHS, QUARTERS, YEARS}



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public StartOfPeriod()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public StartOfPeriod(Unit unit, int offset)
   {
      this.unit = unit;
      this.offset = offset;
   }



   /*******************************************************************************
    ** Factory
    **
    *******************************************************************************/
   public static StartOfPeriod of(Unit unit, int offset)
   {
      return (new StartOfPeriod(unit, offset));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Serializable evaluate(QFieldMetaData field) throws QException
   {
      return (evaluate(field, Instant.now()));
   }



   /*******************************************************************************
    ** Evaluate, as of a given moment (so tests don't depend on the clock).
    *******************************************************************************/
   public Serializable evaluate(QFieldMetaData field, Instant now) throws QException
   {
      QFieldType type = field == null ? QFieldType.DATE_TIME : field.getType();
      if(!type.equals(QFieldType.DATE_TIME) && !type.equals(QFieldType.DATE))
      {
         throw (new QException("Unsupported field type [" + type + "]"));
      }

      ZoneId    zoneId = getZoneId();
      LocalDate start  = startOfPeriod(now.atZone(zoneId).toLocalDate());

      if(type.equals(QFieldType.DATE))
      {
         return (start);
      }

      /////////////////////////////////////////////////////////////////////////////////
      // atStartOfDay(zone) is the first valid moment of the day - so, in zones where //
      // a daylight-saving change skips midnight, it is the end of the gap.           //
      /////////////////////////////////////////////////////////////////////////////////
      return (start.atStartOfDay(zoneId).toInstant());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private LocalDate startOfPeriod(LocalDate today) throws QException
   {
      if(unit == null)
      {
         throw (new QException("A StartOfPeriod expression requires a unit."));
      }

      return switch(unit)
      {
         case DAYS -> today.plusDays(offset);
         case WEEKS -> today.with(TemporalAdjusters.previousOrSame(weekStart == null ? DayOfWeek.SUNDAY : weekStart)).plusWeeks(offset);
         case MONTHS -> today.withDayOfMonth(1).plusMonths(offset);
         case QUARTERS -> today.withDayOfMonth(1).withMonth(((today.getMonthValue() - 1) / 3) * 3 + 1).plusMonths(3L * offset);
         case YEARS -> today.withDayOfYear(1).plusYears(offset);
      };
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private ZoneId getZoneId() throws QException
   {
      if(StringUtils.hasContent(timeZoneId))
      {
         try
         {
            return (ZoneId.of(timeZoneId));
         }
         catch(DateTimeException e)
         {
            throw (new QUserFacingException("Unrecognized time zone: " + timeZoneId));
         }
      }

      return (ValueUtils.getSessionOrInstanceZoneId());
   }



   /*******************************************************************************
    ** Getter for unit
    *******************************************************************************/
   public Unit getUnit()
   {
      return (this.unit);
   }



   /*******************************************************************************
    ** Setter for unit
    *******************************************************************************/
   public void setUnit(Unit unit)
   {
      this.unit = unit;
   }



   /*******************************************************************************
    ** Getter for offset
    *******************************************************************************/
   public int getOffset()
   {
      return (this.offset);
   }



   /*******************************************************************************
    ** Setter for offset
    *******************************************************************************/
   public void setOffset(int offset)
   {
      this.offset = offset;
   }



   /*******************************************************************************
    ** Getter for weekStart
    *******************************************************************************/
   public DayOfWeek getWeekStart()
   {
      return (this.weekStart);
   }



   /*******************************************************************************
    ** Setter for weekStart
    *******************************************************************************/
   public void setWeekStart(DayOfWeek weekStart)
   {
      this.weekStart = weekStart;
   }



   /*******************************************************************************
    ** Fluent setter for weekStart
    *******************************************************************************/
   public StartOfPeriod withWeekStart(DayOfWeek weekStart)
   {
      this.weekStart = weekStart;
      return (this);
   }



   /*******************************************************************************
    ** Getter for timeZoneId
    *******************************************************************************/
   public String getTimeZoneId()
   {
      return (this.timeZoneId);
   }



   /*******************************************************************************
    ** Setter for timeZoneId
    *******************************************************************************/
   public void setTimeZoneId(String timeZoneId)
   {
      this.timeZoneId = timeZoneId;
   }



   /*******************************************************************************
    ** Fluent setter for timeZoneId
    *******************************************************************************/
   public StartOfPeriod withTimeZoneId(String timeZoneId)
   {
      this.timeZoneId = timeZoneId;
      return (this);
   }

}
