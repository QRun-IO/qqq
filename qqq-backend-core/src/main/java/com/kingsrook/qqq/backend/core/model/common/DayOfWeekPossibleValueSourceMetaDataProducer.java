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

package com.kingsrook.qqq.backend.core.model.common;


import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PVSValueFormatAndFields;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;


/*******************************************************************************
 ** Meta Data Producer for DayOfWeek.
 *******************************************************************************/
public class DayOfWeekPossibleValueSourceMetaDataProducer extends MetaDataProducer<QPossibleValueSource>
{
   public static final String NAME = "DayOfWeek";



   /*******************************************************************************
    ** Produces and returns a {@link QPossibleValueSource} containing the seven days
    * of the week as enum values, ordered starting from the locale's first day of the week.
    *******************************************************************************/
   @Override
   public QPossibleValueSource produce(QInstance qInstance)
   {
      DayOfWeek firstDay = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();

      List<QPossibleValue<?>> possibleValues = new ArrayList<>();
      DayOfWeek current = firstDay;
      for (int i = 0; i < 7; i++)
      {
         possibleValues.add(getPossibleValue(current));
         current = current.plus(1);
      }

      return (new QPossibleValueSource()
         .withType(QPossibleValueSourceType.ENUM)
         .withName(NAME)
         .withEnumValues(possibleValues)
         .withValueFormatAndFields(PVSValueFormatAndFields.LABEL_ONLY));
   }



   /*******************************************************************************
    ** Maps the given {@link java.time.DayOfWeek} to a {@link QPossibleValue} using
    * its ISO-8601 integer value and locale-formatted display name.
    *******************************************************************************/
   private QPossibleValue<Integer> getPossibleValue(DayOfWeek dayOfWeek)
   {
      return new QPossibleValue<>(dayOfWeek.getValue(), dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()));
   }

}
