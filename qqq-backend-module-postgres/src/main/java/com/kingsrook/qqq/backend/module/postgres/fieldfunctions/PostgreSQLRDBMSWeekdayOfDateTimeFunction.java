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

package com.kingsrook.qqq.backend.module.postgres.fieldfunctions;


import java.io.Serializable;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateTimeFunction;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.module.rdbms.fieldfunctions.RDBMSFieldFunctionAdapterInterface;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 * PostgreSQL adapter for the {@link com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateTimeFunction}
 * that generates {@code EXTRACT(ISODOW FROM (col AT TIME ZONE 'UTC') AT TIME ZONE ?)} SQL expressions.
 *
 * <p>QQQ stores datetime values as UTC in TIMESTAMP (no-tz) columns.  PostgreSQL's
 * single-argument {@code AT TIME ZONE} on a TIMESTAMP interprets the stored value
 * <em>as if it were already in</em> the named zone, which would be wrong here.
 * The two-step form first declares the stored value to be UTC (producing a TIMESTAMPTZ),
 * then converts that TIMESTAMPTZ to the target zone.  This matches the behaviour of
 * {@code CONVERT_TZ} in MySQL.</p>
 *
 * <p>PostgreSQL's {@code ISODOW} matches ISO-8601 (Monday=1 … Sunday=7),
 * so no numeric offset is needed.</p>
 *******************************************************************************/
public class PostgreSQLRDBMSWeekdayOfDateTimeFunction implements RDBMSFieldFunctionAdapterInterface
{

   /***************************************************************************
    * Returns a {@code EXTRACT(ISODOW FROM (col AT TIME ZONE 'UTC') AT TIME ZONE ?)} expression.
    * The single bind parameter (target timezone) is supplied by {@link #getParams}.
    *
    * The two-step AT TIME ZONE is required because QQQ stores datetimes as UTC
    * in TIMESTAMP (without time zone) columns: the first step declares the
    * stored value to be UTC; the second step converts it to the target zone.
    ***************************************************************************/
   @Override
   public String wrapColumnName(String escapedColumnName, FieldFunction fieldFunction, Function<String, String> fieldNameToColumnReference)
   {
      String zoneIdFromFieldName = fieldFunction.getArgumentValueOrDefault(String.class, WeekdayOfDateTimeFunction.PARAM_ZONE_ID_FROM_FIELD_NAME);

      ///////////////////////////////////////////////////////////////////
      // QQQ attempts to standardize on ISO-8601 (Monday=1, Sunday=7). //
      // Postgres ISODOW matches this.                                 //
      ///////////////////////////////////////////////////////////////////
      if(StringUtils.hasContent(zoneIdFromFieldName))
      {
         String columnRef = fieldNameToColumnReference.apply(zoneIdFromFieldName);
         return "EXTRACT(ISODOW FROM (" + escapedColumnName + " AT TIME ZONE 'UTC') AT TIME ZONE COALESCE(" + columnRef + ", ?))";
      }
      else
      {
         return "EXTRACT(ISODOW FROM (" + escapedColumnName + " AT TIME ZONE 'UTC') AT TIME ZONE ?)";
      }
   }



   /***************************************************************************
    * Returns the single bind parameter: the resolved target timezone (session, instance default, or explicit override).
    ***************************************************************************/
   @Override
   public List<Serializable> getParams(FieldFunction fieldFunction)
   {
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

      String timeZoneIdArg = fieldFunction.getArgumentValueOrDefault(String.class, WeekdayOfDateTimeFunction.PARAM_TIME_ZONE_ID);
      if(StringUtils.hasContent(timeZoneIdArg))
      {
         timeZoneId = ValueUtils.getValueAsString(timeZoneIdArg);
      }

      return List.of(timeZoneId);
   }



   /***************************************************************************
    * Returns the ORDER BY expression using the same two-step AT TIME ZONE form.
    * When sortSundayFirst is true, appends {@code % 7} so Sunday (value 7) sorts as 0 before Monday.
    ***************************************************************************/
   @Override
   public String wrapColumnNameForOrderBy(String escapedColumnName, FieldFunction fieldFunction, Function<String, String> fieldNameToColumnReference)
   {
      String zoneIdFromFieldName = fieldFunction.getArgumentValueOrDefault(String.class, WeekdayOfDateTimeFunction.PARAM_ZONE_ID_FROM_FIELD_NAME);

      //////////////////////////////////////////////////////////////////////////////////////////////
      // to sort Sunday first, do a % 7, which puts Sunday(7) = 0, and leaves Monday(1) = 1, etc. //
      //////////////////////////////////////////////////////////////////////////////////////////////
      Boolean sundayFirst = fieldFunction.getArgumentValueOrDefault(Boolean.class, WeekdayOfDateTimeFunction.PARAM_SORT_SUNDAY_FIRST);

      if(StringUtils.hasContent(zoneIdFromFieldName))
      {
         String columnRef = fieldNameToColumnReference.apply(zoneIdFromFieldName);
         return "EXTRACT(ISODOW FROM (" + escapedColumnName + " AT TIME ZONE 'UTC') AT TIME ZONE COALESCE(" + columnRef + ", ?))" + (BooleanUtils.isTrue(sundayFirst) ? " % 7" : "");
      }
      else
      {
         return "EXTRACT(ISODOW FROM (" + escapedColumnName + " AT TIME ZONE 'UTC') AT TIME ZONE ?)" + (BooleanUtils.isTrue(sundayFirst) ? " % 7" : "");
      }
   }

}
