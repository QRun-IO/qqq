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
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QRuntimeException;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class ThisOrLastPeriod extends AbstractFilterExpression<Serializable>
{
   private Operator   operator;
   private ChronoUnit timeUnit;




   /***************************************************************************
    **
    ***************************************************************************/
   public enum Operator
   {THIS, LAST}



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ThisOrLastPeriod()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   private ThisOrLastPeriod(Operator operator, ChronoUnit timeUnit)
   {
      this.operator = operator;
      this.timeUnit = timeUnit;
   }



   /*******************************************************************************
    ** Factory
    **
    *******************************************************************************/
   public static ThisOrLastPeriod this_(ChronoUnit timeUnit)
   {
      return (new ThisOrLastPeriod(Operator.THIS, timeUnit));
   }



   /*******************************************************************************
    ** Factory
    **
    *******************************************************************************/
   public static ThisOrLastPeriod last(ChronoUnit timeUnit)
   {
      return (new ThisOrLastPeriod(Operator.LAST, timeUnit));
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
   Serializable evaluate(QFieldMetaData field, Instant now) throws QException
   {
      QFieldType type = field == null ? QFieldType.DATE_TIME : field.getType();

      if(type.equals(QFieldType.DATE_TIME))
      {
         return (evaluateForDateTime(now));
      }
      else if(type.equals(QFieldType.DATE))
      {
         return (evaluateForDate(now));
      }
      else
      {
         throw (new QException("Unsupported field type [" + type + "]"));
      }
   }



   /***************************************************************************
    ** The start of this (or the last) period, as an Instant - computed on the
    ** local calendar, and only then turned into an Instant, so that the offset
    ** used is the one in effect at that moment (not today's) across a daylight-
    ** saving change.
    ***************************************************************************/
   private Instant evaluateForDateTime(Instant now)
   {
      ZoneId zoneId = ValueUtils.getSessionOrInstanceZoneId();

      if(timeUnit == ChronoUnit.HOURS)
      {
         Instant thisHour = now.truncatedTo(ChronoUnit.HOURS);
         return operator.equals(Operator.THIS) ? thisHour : now.minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
      }

      return (evaluateForDate(now).atStartOfDay(zoneId).toInstant());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public LocalDate evaluateForDate()
   {
      return (evaluateForDate(Instant.now()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private LocalDate evaluateForDate(Instant now)
   {
      ZoneId    zoneId = ValueUtils.getSessionOrInstanceZoneId();
      LocalDate today  = now.atZone(zoneId).toLocalDate();
      int       last   = operator.equals(Operator.THIS) ? 0 : 1;

      switch(timeUnit)
      {
         case DAYS ->
         {
            return today.minusDays(last);
         }
         case WEEKS ->
         {
            return today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY)).minusWeeks(last);
         }
         case MONTHS ->
         {
            return today.withDayOfMonth(1).minusMonths(last);
         }
         case YEARS ->
         {
            return today.withDayOfYear(1).minusYears(last);
         }
         default -> throw (new QRuntimeException("Unsupported unit: " + timeUnit));
      }
   }



   /*******************************************************************************
    ** Getter for operator
    **
    *******************************************************************************/
   public Operator getOperator()
   {
      return operator;
   }



   /*******************************************************************************
    ** Getter for timeUnit
    **
    *******************************************************************************/
   public ChronoUnit getTimeUnit()
   {
      return timeUnit;
   }
}
