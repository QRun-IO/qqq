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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
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
      QFieldType type = field == null ? QFieldType.DATE_TIME : field.getType();

      if(type.equals(QFieldType.DATE_TIME))
      {
         return (evaluateForDateTime());
      }
      else if(type.equals(QFieldType.DATE))
      {
         // return (evaluateForDateTime());
         return (evaluateForDate());
      }
      else
      {
         throw (new QException("Unsupported field type [" + type + "]"));
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private Instant evaluateForDateTime()
   {
      ZoneId zoneId = ValueUtils.getSessionOrInstanceZoneId();

      switch(timeUnit)
      {
         case HOURS ->
         {
            if(operator.equals(Operator.THIS))
            {
               return Instant.now().truncatedTo(ChronoUnit.HOURS);
            }
            else
            {
               return Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.HOURS);
            }
         }
         case DAYS ->
         {
            Instant startOfToday = ValueUtils.getStartOfTodayInZoneId(zoneId.getId());
            return operator.equals(Operator.THIS) ? startOfToday : startOfToday.minus(1, ChronoUnit.DAYS);
         }
         case WEEKS ->
         {
            Instant       startOfToday       = ValueUtils.getStartOfTodayInZoneId(zoneId.getId());
            LocalDateTime startOfThisWeekLDT = LocalDateTime.ofInstant(startOfToday, zoneId);
            while(startOfThisWeekLDT.get(ChronoField.DAY_OF_WEEK) != DayOfWeek.SUNDAY.getValue())
            {
               ////////////////////////////////////////
               // go backwards until sunday is found //
               ////////////////////////////////////////
               startOfThisWeekLDT = startOfThisWeekLDT.minus(1, ChronoUnit.DAYS);
            }
            Instant startOfThisWeek = startOfThisWeekLDT.toInstant(zoneId.getRules().getOffset(startOfThisWeekLDT));

            return operator.equals(Operator.THIS) ? startOfThisWeek : startOfThisWeek.minus(7, ChronoUnit.DAYS);
         }
         case MONTHS ->
         {
            Instant       startOfThisMonth    = ValueUtils.getStartOfMonthInZoneId(zoneId.getId());
            LocalDateTime startOfThisMonthLDT = LocalDateTime.ofInstant(startOfThisMonth, ZoneId.of(zoneId.getId()));
            LocalDateTime startOfLastMonthLDT = startOfThisMonthLDT.minus(1, ChronoUnit.MONTHS);
            Instant       startOfLastMonth    = startOfLastMonthLDT.toInstant(ZoneId.of(zoneId.getId()).getRules().getOffset(Instant.now()));

            return operator.equals(Operator.THIS) ? startOfThisMonth : startOfLastMonth;
         }
         case YEARS ->
         {
            Instant       startOfThisYear    = ValueUtils.getStartOfYearInZoneId(zoneId.getId());
            LocalDateTime startOfThisYearLDT = LocalDateTime.ofInstant(startOfThisYear, zoneId);
            LocalDateTime startOfLastYearLDT = startOfThisYearLDT.minus(1, ChronoUnit.YEARS);
            Instant       startOfLastYear    = startOfLastYearLDT.toInstant(zoneId.getRules().getOffset(Instant.now()));

            return operator.equals(Operator.THIS) ? startOfThisYear : startOfLastYear;
         }
         default -> throw (new QRuntimeException("Unsupported unit: " + timeUnit));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public LocalDate evaluateForDate()
   {
      ZoneId    zoneId = ValueUtils.getSessionOrInstanceZoneId();
      LocalDate today  = Instant.now().atZone(zoneId).toLocalDate();

      switch(timeUnit)
      {
         case DAYS ->
         {
            return operator.equals(Operator.THIS) ? today : today.minusDays(1);
         }
         case WEEKS ->
         {
            LocalDate startOfThisWeek = today;
            while(startOfThisWeek.get(ChronoField.DAY_OF_WEEK) != DayOfWeek.SUNDAY.getValue())
            {
               ////////////////////////////////////////
               // go backwards until sunday is found //
               ////////////////////////////////////////
               startOfThisWeek = startOfThisWeek.minusDays(1);
            }
            return operator.equals(Operator.THIS) ? startOfThisWeek : startOfThisWeek.minusDays(7);
         }
         case MONTHS ->
         {
            Instant       startOfThisMonth    = ValueUtils.getStartOfMonthInZoneId(zoneId.getId());
            LocalDateTime startOfThisMonthLDT = LocalDateTime.ofInstant(startOfThisMonth, ZoneId.of(zoneId.getId()));
            LocalDateTime startOfLastMonthLDT = startOfThisMonthLDT.minusMonths(1);
            Instant       startOfLastMonth    = startOfLastMonthLDT.toInstant(ZoneId.of(zoneId.getId()).getRules().getOffset(Instant.now()));

            return (operator.equals(Operator.THIS) ? startOfThisMonth : startOfLastMonth).atZone(zoneId).toLocalDate();
         }
         case YEARS ->
         {
            Instant       startOfThisYear    = ValueUtils.getStartOfYearInZoneId(zoneId.getId());
            LocalDateTime startOfThisYearLDT = LocalDateTime.ofInstant(startOfThisYear, zoneId);
            LocalDateTime startOfLastYearLDT = startOfThisYearLDT.minusYears(1);
            Instant       startOfLastYear    = startOfLastYearLDT.toInstant(zoneId.getRules().getOffset(Instant.now()));

            return (operator.equals(Operator.THIS) ? startOfThisYear : startOfLastYear).atZone(zoneId).toLocalDate();
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
