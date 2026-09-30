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

package com.kingsrook.qqq.backend.module.rdbms.fieldfunctions;


import java.util.function.Function;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateFunction;
import org.apache.commons.lang3.BooleanUtils;


/*******************************************************************************
 * RDBMS adapter for the {@link WeekdayOfDateFunction} that generates
 * MySQL-compatible {@code WEEKDAY(col) + 1} SQL expressions.
 *
 * <p>MySQL's WEEKDAY() returns Monday=0 … Sunday=6, so adding 1 maps it to
 * QQQ's ISO-8601 standard of Monday=1 … Sunday=7.</p>
 *******************************************************************************/
public class RDBMSWeekdayOfDateFunction implements RDBMSFieldFunctionAdapterInterface
{


   /***************************************************************************
    * Returns {@code WEEKDAY(col) + 1} — shifting MySQL's 0-based Monday to
    * ISO-8601's 1-based Monday.
    ***************************************************************************/
   @Override
   public String wrapColumnName(String escapedColumnName, FieldFunction fieldFunction, Function<String, String> fieldNameToColumnReference)
   {
      ////////////////////////////////////////////////////////////////////
      // Mysql WEEKDAY returns values Monday=0, Sunday=6.               //
      // QQQ attempts to standardize on ISO-8601 (Monday=1, Sunday=7).  //
      // simplest way to map this is just adding 1 to WEEKDAY's output. //
      // this works for both a SELECT clause or a WHERE clause          //
      ////////////////////////////////////////////////////////////////////
      return "WEEKDAY(" + escapedColumnName + ") + 1";
   }



   /***************************************************************************
    * Returns the ORDER BY expression; when sortSundayFirst is true, applies
    * {@code % 7} so Sunday (value 7) sorts as 0 before Monday.
    ***************************************************************************/
   @Override
   public String wrapColumnNameForOrderBy(String escapedColumnName, FieldFunction fieldFunction, Function<String, String> fieldNameToColumnReference)
   {
      //////////////////////////////////////////////////////////////////////////////////////////////
      // to sort Sunday first, do a % 7, which puts Sunday(7) = 0, and leaves Monday(1) = 1, etc. //
      //////////////////////////////////////////////////////////////////////////////////////////////
      Boolean sundayFirst = fieldFunction.getArgumentValueOrDefault(Boolean.class, WeekdayOfDateFunction.PARAM_SORT_SUNDAY_FIRST);
      return "(WEEKDAY(" + escapedColumnName + ") + 1)" + (BooleanUtils.isTrue(sundayFirst) ? " % 7" : "");
   }
}
