/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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


import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Function;
import java.util.function.Predicate;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PVSValueFormatAndFields;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;


/*******************************************************************************
 **
 *******************************************************************************/
public class TimeZonePossibleValueSourceMetaDataProvider
{
   public static final String NAME = "timeZones";



   /*******************************************************************************
    **
    *******************************************************************************/
   public QPossibleValueSource produce()
   {
      return (produce(null, null, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QPossibleValueSource produce(Predicate<String> filter, Function<String, String> labelMapper)
   {
      return (produce(filter, labelMapper, null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QPossibleValueSource produce(Predicate<String> filter, Function<String, String> labelMapper, Comparator<QPossibleValue<?>> comparator)
   {
      QPossibleValueSource possibleValueSource = new QPossibleValueSource()
         .withName("timeZones")
         .withType(QPossibleValueSourceType.ENUM)
         .withValueFormatAndFields(PVSValueFormatAndFields.LABEL_ONLY);

      List<QPossibleValue<?>> enumValues = new ArrayList<>();
      for(String availableID : TimeZone.getAvailableIDs())
      {
         if(filter == null || filter.test(availableID))
         {
            String label = labelMapper == null ? availableID : labelMapper.apply(availableID);
            enumValues.add(new QPossibleValue<>(availableID, label));
         }
      }

      if(comparator != null)
      {
         enumValues.sort(comparator);
      }

      possibleValueSource.withEnumValues(enumValues);
      return (possibleValueSource);
   }
}
