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


import java.time.ZoneId;
import java.util.List;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.implementations.WeekdayOfDateTimeFunction;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.bson.Document;


/*******************************************************************************
 ** MongoDB adapter for WeekdayOfDateTimeFunction.
 ** Same ISO conversion as WeekdayOfDate, but uses timezone-aware
 ** form of $dayOfWeek: {$dayOfWeek: {date: "$field", timezone: "tz"}}.
 **
 ** Timezone resolution follows the same priority as the RDBMS version
 ** and the core WeekdayOfDateTimeFunction:
 **   1. Per-row field value (zoneIdFromFieldName), falling back to static tz via $ifNull
 **   2. Explicit timeZoneId argument
 **   3. Session zone ID (if useSessionZoneId is true)
 **   4. Instance default time zone
 *******************************************************************************/
public class MongoDBWeekdayOfDateTimeFunction implements MongoDBFieldFunctionAdapterInterface
{

   @Override
   public Object getExpression(String fieldReference, FieldFunction fieldFunction, Function<String, String> fieldNameToFieldReference)
   {
      return buildIsoExpression(fieldReference, fieldFunction, fieldNameToFieldReference);
   }



   @Override
   public Object getExpressionForOrderBy(String fieldReference, FieldFunction fieldFunction, Function<String, String> fieldNameToFieldReference)
   {
      Boolean sundayFirst = fieldFunction.getArgumentValueOrDefault(Boolean.class, WeekdayOfDateTimeFunction.PARAM_SORT_SUNDAY_FIRST);
      Object  isoExpr     = buildIsoExpression(fieldReference, fieldFunction, fieldNameToFieldReference);

      if(BooleanUtils.isTrue(sundayFirst))
      {
         return new Document("$mod", List.of(isoExpr, 7));
      }

      return isoExpr;
   }



   /*******************************************************************************
    ** Build the ISO-8601 weekday expression with timezone awareness.
    *******************************************************************************/
   private Object buildIsoExpression(String fieldReference, FieldFunction fieldFunction, Function<String, String> fieldNameToFieldReference)
   {
      Object timezone = resolveTimezone(fieldFunction, fieldNameToFieldReference);

      Object mongoDow = new Document("$dayOfWeek", new Document("date", fieldReference).append("timezone", timezone));
      Object plus5    = new Document("$add", List.of(mongoDow, 5));
      Object mod7     = new Document("$mod", List.of(plus5, 7));
      return new Document("$add", List.of(mod7, 1));
   }



   /*******************************************************************************
    ** Resolve the timezone value using the same priority as the RDBMS adapter
    ** and the core WeekdayOfDateTimeFunction.
    **
    ** Returns either a String (static timezone) or a Document containing an
    ** $ifNull expression (when zoneIdFromFieldName is set, to try the per-document
    ** field value first, falling back to the static timezone).
    *******************************************************************************/
   private Object resolveTimezone(FieldFunction fieldFunction, Function<String, String> fieldNameToFieldReference)
   {
      String staticTimeZoneId = resolveStaticTimeZoneId(fieldFunction);

      /////////////////////////////////////////////////////////////////////////////////////////////
      // if a zoneIdFromFieldName is specified, use $ifNull to try the per-document field value  //
      // first, falling back to the static timezone when the field is null.                      //
      /////////////////////////////////////////////////////////////////////////////////////////////
      String zoneIdFromFieldName = fieldFunction.getArgumentValueOrDefault(String.class, WeekdayOfDateTimeFunction.PARAM_ZONE_ID_FROM_FIELD_NAME);
      if(StringUtils.hasContent(zoneIdFromFieldName) && fieldNameToFieldReference != null)
      {
         String fieldRef = fieldNameToFieldReference.apply(zoneIdFromFieldName);
         return new Document("$ifNull", List.of(fieldRef, staticTimeZoneId));
      }

      return staticTimeZoneId;
   }



   /*******************************************************************************
    ** Resolve the static timezone ID from arguments and context (session/instance).
    *******************************************************************************/
   private String resolveStaticTimeZoneId(FieldFunction fieldFunction)
   {
      Boolean useSessionZoneId = fieldFunction.getArgumentValueOrDefault(Boolean.class, WeekdayOfDateTimeFunction.PARAM_USE_SESSION_ZONE_ID);
      String  timeZoneId;

      if(BooleanUtils.isTrue(useSessionZoneId))
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
         timeZoneId = timeZoneIdArg;
      }

      return timeZoneId;
   }

}
