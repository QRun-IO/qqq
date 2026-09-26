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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionParameter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionTypeIdentifier;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/***************************************************************************
 * Follows ISO-8601 Standard, with Monday=1, Sunday=7.
 *
 * <p>Note that this matches Java DayOfWeek (but differs from Calendar and Date,
 * as well as javascript)</p>
 ***************************************************************************/
public class WeekdayOfDateTimeFunction implements FieldFunctionType
{
   public static final FieldFunctionTypeIdentifier IDENTIFIER = () -> "WeekdayOfDateTime";

   public static final String PARAM_SORT_SUNDAY_FIRST       = "sortSundayFirst";
   public static final String PARAM_TIME_ZONE_ID            = "timeZoneId";
   public static final String PARAM_ZONE_ID_FROM_FIELD_NAME = "zoneIdFromFieldName";
   public static final String PARAM_USE_SESSION_ZONE_ID     = "useSessionZoneId";



   /***************************************************************************
    * Returns this function's identifier (WeekdayOfDateTime).
    ***************************************************************************/
   @Override
   public FieldFunctionTypeIdentifier getIdentifier()
   {
      return (IDENTIFIER);
   }



   /***************************************************************************
    * Returns the set of DATE_TIME field types that this function may be applied to.
    ***************************************************************************/
   @Override
   public Set<QFieldType> getAllowedFieldTypes()
   {
      return Set.of(QFieldType.DATE_TIME);
   }



   /***************************************************************************
    * Returns the parameter definitions: timeZoneId (optional override), sortSundayFirst (locale-aware default), and useSessionZoneId (default true).
    ***************************************************************************/
   @Override
   public List<FieldFunctionParameter> getParameters()
   {
      DayOfWeek firstDay = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();

      return List.of(
         new FieldFunctionParameter().withName(PARAM_TIME_ZONE_ID).withType(QFieldType.STRING).withIsRequired(false),
         new FieldFunctionParameter().withName(PARAM_ZONE_ID_FROM_FIELD_NAME).withType(QFieldType.STRING).withIsRequired(false),
         new FieldFunctionParameter().withName(PARAM_SORT_SUNDAY_FIRST).withType(QFieldType.BOOLEAN).withIsRequired(false).withDefaultValue(DayOfWeek.SUNDAY.equals(firstDay)),
         new FieldFunctionParameter().withName(PARAM_USE_SESSION_ZONE_ID).withType(QFieldType.BOOLEAN).withIsRequired(false).withDefaultValue(true)
      );
   }



   /***************************************************************************
    * Returns INTEGER — the ISO-8601 day-of-week number (Monday=1 … Sunday=7) in the resolved time zone.
    ***************************************************************************/
   @Override
   public QFieldType getReturnType()
   {
      return QFieldType.INTEGER;
   }



   /***************************************************************************
    * Converts the datetime field value to the specified time zone and returns
    * the ISO-8601 day-of-week value, or null if the value is null.
    ***************************************************************************/
   @Override
   public Integer apply(FieldFunction fieldFunction, QRecord record) throws QValueException
   {
      Instant sourceValue = record.getValueInstant(fieldFunction.getFieldName());
      if(sourceValue == null)
      {
         return (null);
      }

      /////////////////////////////////////////////////////////////////////////////////////////////
      // start by assuming we'll use either the instance default time zone or else the session's //
      /////////////////////////////////////////////////////////////////////////////////////////////
      Boolean useSessionZoneId = fieldFunction.getArgumentValueOrDefault(Boolean.class, WeekdayOfDateTimeFunction.PARAM_USE_SESSION_ZONE_ID);
      String  timeZoneId;
      if(useSessionZoneId)
      {
         timeZoneId = ObjectUtils.tryElse(() -> ValueUtils.getSessionOrInstanceZoneId().toString(), ZoneId.systemDefault().getId());
      }
      else
      {
         timeZoneId = QContext.getQInstance().getDefaultTimeZoneId();
      }

      ////////////////////////////////////////////////////////////////
      // but - if a param of time zone id is present, then use that //
      ////////////////////////////////////////////////////////////////
      String timeZoneIdArg = fieldFunction.getArgumentValueOrDefault(String.class, PARAM_TIME_ZONE_ID);
      if(StringUtils.hasContent(timeZoneIdArg))
      {
         timeZoneId = ValueUtils.getValueAsString(timeZoneIdArg);
      }

      /////////////////////////////////////////////////////////////////////////////////////////////
      // but (again) - if a zoneIdFromFieldName is listed, and such a value exist, then use THAT //
      /////////////////////////////////////////////////////////////////////////////////////////////
      String zoneIdFromFieldName = fieldFunction.getArgumentValueOrDefault(String.class, PARAM_ZONE_ID_FROM_FIELD_NAME);
      if(StringUtils.hasContent(zoneIdFromFieldName))
      {
         String fieldValue = record.getValueString(zoneIdFromFieldName);
         if(StringUtils.hasContent(fieldValue))
         {
            timeZoneId = fieldValue;
         }
      }

      ZonedDateTime zonedDateTime = sourceValue.atZone(ZoneId.of(timeZoneId));

      return zonedDateTime.getDayOfWeek().getValue();
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
