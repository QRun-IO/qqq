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

package com.kingsrook.qqq.backend.core.actions.permissions.fieldaccess;


import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;


/*******************************************************************************
 ** Masks values of fields whose exposure is MASKED:
 ** - email addresses keep their first character and their domain (j***@example.com)
 ** - numbers, and strings of digits (and separators), keep their last 4 digits (***1234)
 ** - other strings keep their first and last character (a***z), or are fully
 **   masked (***) when 4 or fewer characters long.
 *******************************************************************************/
public class FieldValueMasker
{
   public static final String MASK = "***";



   /*******************************************************************************
    ** Mask a value (of a field of the given type - which may be null).
    *******************************************************************************/
   public static String mask(String value, QFieldType type, FieldMaskStyle style)
   {
      if(value == null)
      {
         return (null);
      }

      style = style == null ? FieldMaskStyle.AUTO : style;
      if(style == FieldMaskStyle.FULL)
      {
         return (MASK);
      }

      String trimmed = value.trim();
      int    atIndex = trimmed.indexOf('@');
      if((style == FieldMaskStyle.AUTO || style == FieldMaskStyle.EMAIL) && atIndex > 0 && atIndex < trimmed.length() - 1 && trimmed.indexOf('@', atIndex + 1) < 0)
      {
         return (trimmed.charAt(0) + MASK + trimmed.substring(atIndex));
      }

      if(style == FieldMaskStyle.EMAIL)
      {
         return (MASK);
      }

      String digits = trimmed.replaceAll("[^0-9]", "");
      if((style == FieldMaskStyle.AUTO || style == FieldMaskStyle.LAST_4) && ((type != null && type.isNumeric()) || (!digits.isEmpty() && trimmed.matches("[0-9 ().+/-]+"))))
      {
         return (digits.length() > 4 ? MASK + digits.substring(digits.length() - 4) : MASK);
      }

      if(style == FieldMaskStyle.LAST_4 || trimmed.length() <= 4)
      {
         return (MASK);
      }

      return (trimmed.charAt(0) + MASK + trimmed.charAt(trimmed.length() - 1));
   }
}
