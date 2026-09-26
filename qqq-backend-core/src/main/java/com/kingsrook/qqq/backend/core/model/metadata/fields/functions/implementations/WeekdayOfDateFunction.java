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


import java.io.Serializable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionParameter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionTypeIdentifier;


/***************************************************************************
 * Follows ISO-8601 Standard, with Monday=1, Sunday=7.
 *
 * <p>Note that this matches Java DayOfWeek (but differs from Calendar and Date,
 * as well as javascript)</p>
 ***************************************************************************/
public class WeekdayOfDateFunction implements FieldFunctionType
{
   public static final FieldFunctionTypeIdentifier IDENTIFIER = () -> "WeekdayOfDate";

   public static final String PARAM_SORT_SUNDAY_FIRST = "sortSundayFirst";



   /***************************************************************************
    * Returns this function's identifier (WeekdayOfDate).
    ***************************************************************************/
   @Override
   public FieldFunctionTypeIdentifier getIdentifier()
   {
      return (IDENTIFIER);
   }



   /***************************************************************************
    * Returns the set of DATE field types that this function may be applied to.
    ***************************************************************************/
   @Override
   public Set<QFieldType> getAllowedFieldTypes()
   {
      return Set.of(QFieldType.DATE);
   }



   /***************************************************************************
    * Returns the parameter definitions, including sortSundayFirst with a locale-aware default.
    ***************************************************************************/
   @Override
   public List<FieldFunctionParameter> getParameters()
   {
      DayOfWeek firstDay = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();

      return List.of(
         new FieldFunctionParameter().withName(PARAM_SORT_SUNDAY_FIRST).withType(QFieldType.BOOLEAN).withIsRequired(false).withDefaultValue(DayOfWeek.SUNDAY.equals(firstDay))
      );
   }



   /***************************************************************************
    * Returns INTEGER — the ISO-8601 day-of-week number (Monday=1 … Sunday=7).
    ***************************************************************************/
   @Override
   public QFieldType getReturnType()
   {
      return QFieldType.INTEGER;
   }



   /***************************************************************************
    * Returns the ISO-8601 day-of-week value (Monday=1, Sunday=7) for the date
    * field's value, or null if the value is null.
    ***************************************************************************/
   @Override
   public Integer apply(FieldFunction fieldFunction, QRecord record) throws QValueException
   {
      LocalDate sourceValue = record.getValueLocalDate(fieldFunction.getFieldName());
      if(sourceValue == null)
      {
         return (null);
      }

      return sourceValue.getDayOfWeek().getValue();
   }



   /***************************************************************************
    * Returns the day-of-week value adjusted for sort ordering. When
    * sortSundayFirst is true, Sunday (value 7) is mapped to 0 via modulo so
    * it sorts before Monday.
    ***************************************************************************/
   @Override
   public Serializable applyForSorting(FieldFunction fieldFunction, QRecord record) throws QValueException, QException
   {
      Integer baseValue = apply(fieldFunction, record);
      if(baseValue == null)
      {
         return (null);
      }

      if(fieldFunction.getArgumentValueOrDefault(Boolean.class, PARAM_SORT_SUNDAY_FIRST))
      {
         return baseValue % 7;
      }
      else
      {
         return baseValue;
      }
   }
}
