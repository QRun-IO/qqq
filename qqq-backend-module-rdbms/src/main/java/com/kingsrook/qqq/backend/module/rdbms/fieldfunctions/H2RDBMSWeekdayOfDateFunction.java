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
 * H2 adapter for the {@link WeekdayOfDateFunction}:  H2 has no MySQL WEEKDAY(),
 * but its {@code ISO_DAY_OF_WEEK(col)} already returns QQQ's ISO-8601 values
 * (Monday=1 … Sunday=7), the same values as the MySQL adapter's
 * {@code WEEKDAY(col) + 1}.
 *******************************************************************************/
public class H2RDBMSWeekdayOfDateFunction implements RDBMSFieldFunctionAdapterInterface
{


   /***************************************************************************
    * Returns {@code ISO_DAY_OF_WEEK(col)}.
    ***************************************************************************/
   @Override
   public String wrapColumnName(String escapedColumnName, FieldFunction fieldFunction, Function<String, String> fieldNameToColumnReference)
   {
      return "ISO_DAY_OF_WEEK(" + escapedColumnName + ")";
   }



   /***************************************************************************
    * Returns the ORDER BY expression; when sortSundayFirst is true, applies
    * {@code MOD(..., 7)} so Sunday (value 7) sorts as 0 before Monday.
    ***************************************************************************/
   @Override
   public String wrapColumnNameForOrderBy(String escapedColumnName, FieldFunction fieldFunction, Function<String, String> fieldNameToColumnReference)
   {
      Boolean sundayFirst = fieldFunction.getArgumentValueOrDefault(Boolean.class, WeekdayOfDateFunction.PARAM_SORT_SUNDAY_FIRST);
      return (BooleanUtils.isTrue(sundayFirst) ? "MOD(ISO_DAY_OF_WEEK(" + escapedColumnName + "), 7)" : "ISO_DAY_OF_WEEK(" + escapedColumnName + ")");
   }
}
