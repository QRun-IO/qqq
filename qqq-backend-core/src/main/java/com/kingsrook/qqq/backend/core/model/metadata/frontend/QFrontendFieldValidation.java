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

package com.kingsrook.qqq.backend.core.model.metadata.frontend;


import java.io.Serializable;
import java.math.BigDecimal;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValuePatternBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueRangeBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** The validation rules of a field, in one place, as the backend enforces them
 ** on insert and update - so a client (web, CLI, TUI, native) can check a value
 ** before saving.  The server stays the authority: a rule not stated here is
 ** never assumed by a client.
 **
 ** - required: a value is required (isRequired).
 ** - maxLength + tooLongBehavior: ERROR refuses a longer value; TRUNCATE and
 **   TRUNCATE_ELLIPSIS trim it on save; PASS_THROUGH (or absent) does neither.
 ** - minValue / maxValue (+ minInclusive / maxInclusive): a value outside is
 **   refused (only ranges whose behavior is ERROR; CLIP ranges are not listed).
 ** - pattern (+ patternMessage): a non-empty value must match the whole
 **   regular expression (ValuePatternBehavior).
 *******************************************************************************/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QFrontendFieldValidation implements Serializable
{
   private Boolean required;
   private Integer maxLength;
   private String  tooLongBehavior;
   private String  minValue;
   private Boolean minInclusive;
   private String  maxValue;
   private Boolean maxInclusive;
   private String  pattern;
   private String  patternMessage;



   /*******************************************************************************
    ** The rules of a field, or null when it has none.
    *******************************************************************************/
   public static QFrontendFieldValidation of(QFieldMetaData field)
   {
      if(field == null)
      {
         return (null);
      }

      QFrontendFieldValidation rs = new QFrontendFieldValidation();
      boolean                  any = false;

      if(field.getIsRequired())
      {
         rs.required = true;
         any = true;
      }

      if(field.getMaxLength() != null && field.getType() != null && field.getType().isStringLike())
      {
         rs.maxLength = field.getMaxLength();
         ValueTooLongBehavior tooLong = field.getBehaviorOnlyIfSet(ValueTooLongBehavior.class);
         rs.tooLongBehavior = (tooLong == null ? ValueTooLongBehavior.PASS_THROUGH : tooLong).name();
         any = true;
      }

      ValueRangeBehavior range = field.getBehaviorOnlyIfSet(ValueRangeBehavior.class);
      if(range != null)
      {
         if(range.getMinValue() != null && range.getMinBehavior() == ValueRangeBehavior.Behavior.ERROR)
         {
            rs.minValue = plain(range.getMinValue());
            rs.minInclusive = range.getMinAllowEqualTo();
            any = true;
         }
         if(range.getMaxValue() != null && range.getMaxBehavior() == ValueRangeBehavior.Behavior.ERROR)
         {
            rs.maxValue = plain(range.getMaxValue());
            rs.maxInclusive = range.getMaxAllowEqualTo();
            any = true;
         }
      }

      ValuePatternBehavior pattern = field.getBehaviorOnlyIfSet(ValuePatternBehavior.class);
      if(pattern != null && pattern.getPattern() != null)
      {
         rs.pattern = pattern.getPattern();
         rs.patternMessage = pattern.getMessageFor(field);
         any = true;
      }

      return (any ? rs : null);
   }



   /*******************************************************************************
    ** a bound as plain digits (a string, so a long or decimal keeps its exact value)
    *******************************************************************************/
   private static String plain(Number number)
   {
      return (new BigDecimal(number.toString()).toPlainString());
   }



   /*******************************************************************************
    ** Getter for required
    *******************************************************************************/
   public Boolean getRequired()
   {
      return (this.required);
   }



   /*******************************************************************************
    ** Getter for maxLength
    *******************************************************************************/
   public Integer getMaxLength()
   {
      return (this.maxLength);
   }



   /*******************************************************************************
    ** Getter for tooLongBehavior
    *******************************************************************************/
   public String getTooLongBehavior()
   {
      return (this.tooLongBehavior);
   }



   /*******************************************************************************
    ** Getter for minValue
    *******************************************************************************/
   public String getMinValue()
   {
      return (this.minValue);
   }



   /*******************************************************************************
    ** Getter for minInclusive
    *******************************************************************************/
   public Boolean getMinInclusive()
   {
      return (this.minInclusive);
   }



   /*******************************************************************************
    ** Getter for maxValue
    *******************************************************************************/
   public String getMaxValue()
   {
      return (this.maxValue);
   }



   /*******************************************************************************
    ** Getter for maxInclusive
    *******************************************************************************/
   public Boolean getMaxInclusive()
   {
      return (this.maxInclusive);
   }



   /*******************************************************************************
    ** Getter for pattern
    *******************************************************************************/
   public String getPattern()
   {
      return (this.pattern);
   }



   /*******************************************************************************
    ** Getter for patternMessage
    *******************************************************************************/
   public String getPatternMessage()
   {
      return (this.patternMessage);
   }
}
