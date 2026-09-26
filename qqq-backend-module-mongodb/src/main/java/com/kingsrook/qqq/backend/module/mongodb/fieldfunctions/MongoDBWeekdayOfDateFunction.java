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

package com.kingsrook.qqq.backend.module.mongodb.fieldfunctions;


import java.util.List;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateFunction;
import org.apache.commons.lang3.BooleanUtils;
import org.bson.Document;


/*******************************************************************************
 ** MongoDB adapter for WeekdayOfDateFunction.
 ** MongoDB $dayOfWeek returns Sun=1..Sat=7.
 ** ISO-8601 needs Mon=1..Sun=7.
 ** Conversion: ((mongoDow + 5) % 7) + 1
 **
 ** For ORDER BY with sortSundayFirst=true: isoValue % 7
 ** so Sunday(7) becomes 0 and sorts before Monday(1).
 *******************************************************************************/
public class MongoDBWeekdayOfDateFunction implements MongoDBFieldFunctionAdapterInterface
{

   @Override
   public Object getExpression(String fieldReference, FieldFunction fieldFunction, Function<String, String> fieldNameToFieldReference)
   {
      return buildIsoExpression(fieldReference);
   }



   @Override
   public Object getExpressionForOrderBy(String fieldReference, FieldFunction fieldFunction, Function<String, String> fieldNameToFieldReference)
   {
      Boolean sundayFirst = fieldFunction.getArgumentValueOrDefault(Boolean.class, WeekdayOfDateFunction.PARAM_SORT_SUNDAY_FIRST);
      Object  isoExpr     = buildIsoExpression(fieldReference);

      if(BooleanUtils.isTrue(sundayFirst))
      {
         // isoValue % 7: Sunday(7)->0, Monday(1)->1, ..., Saturday(6)->6
         return new Document("$mod", List.of(isoExpr, 7));
      }

      return isoExpr;
   }



   /*******************************************************************************
    ** Build the ISO-8601 weekday expression from a MongoDB $dayOfWeek value.
    ** MongoDB: Sun=1, Mon=2, ..., Sat=7
    ** ISO:     Mon=1, Tue=2, ..., Sun=7
    ** Formula: ((mongoDow + 5) % 7) + 1
    *******************************************************************************/
   protected Object buildIsoExpression(String fieldReference)
   {
      Object mongoDow = new Document("$dayOfWeek", fieldReference);
      Object plus5    = new Document("$add", List.of(mongoDow, 5));
      Object mod7     = new Document("$mod", List.of(plus5, 7));
      return new Document("$add", List.of(mod7, 1));
   }

}
