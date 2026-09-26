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
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionParameter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionTypeIdentifier;


/***************************************************************************
 * FieldFunctionType implementation that extracts a substring from a string
 * field's value, using 1-based indexing (matching SQL SUBSTRING, not Java's
 * 0-based {@link String#substring}).
 *
 * <p>Parameters: {@code fromIndex} (required, 1-based start position) and
 * {@code length} (optional number of characters to extract).  If {@code length}
 * is omitted the remainder of the string is returned.</p>
 ***************************************************************************/
public class SubStringFunction implements FieldFunctionType
{
   public static final FieldFunctionTypeIdentifier IDENTIFIER = () -> "SubString";

   public static final String FROM_INDEX_PARAM = "fromIndex";
   public static final String LENGTH_PARAM     = "length";



   /***************************************************************************
    * Returns this function's identifier (SubString).
    ***************************************************************************/
   @Override
   public FieldFunctionTypeIdentifier getIdentifier()
   {
      return (IDENTIFIER);
   }



   /***************************************************************************
    * Returns the set of string-like field types that this function may be applied to.
    ***************************************************************************/
   @Override
   public Set<QFieldType> getAllowedFieldTypes()
   {
      return QFieldType.STRING_LIKE_TYPES;
   }



   /***************************************************************************
    * Returns the parameter definitions: fromIndex (required) and length (optional).
    ***************************************************************************/
   @Override
   public List<FieldFunctionParameter> getParameters()
   {
      return List.of(
         new FieldFunctionParameter().withName(FROM_INDEX_PARAM).withType(QFieldType.INTEGER).withIsRequired(true),
         new FieldFunctionParameter().withName(LENGTH_PARAM).withType(QFieldType.INTEGER).withIsRequired(false)
      );
   }



   /***************************************************************************
    * Returns STRING, the type of the extracted substring.
    ***************************************************************************/
   @Override
   public QFieldType getReturnType()
   {
      return QFieldType.STRING;
   }



   /***************************************************************************
    * Extracts and returns the substring of the field value using 1-based fromIndex
    * and optional length; returns null if the field value is null, or an empty
    * string if fromIndex is beyond the string's length.
    ***************************************************************************/
   @Override
   public Serializable apply(FieldFunction fieldFunction, QRecord record) throws QException
   {
      String sourceValue = record.getValueString(fieldFunction.getFieldName());
      if(sourceValue == null)
      {
         return (null);
      }

      Integer fromIndex = fieldFunction.getArgumentValueOrDefault(Integer.class, FROM_INDEX_PARAM);
      Integer length    = fieldFunction.getArgumentValueOrDefault(Integer.class, LENGTH_PARAM);

      if(fromIndex == null)
      {
         throw (new QException("Missing required fromIndex arguments for SubString function"));
      }
      else
      {
         /////////////////////////////////////////////////////////////////////////////////////
         // java substring is 0 based, by SQL (and this function) are 1-based - so minus 1. //
         /////////////////////////////////////////////////////////////////////////////////////
         fromIndex--;
         if(fromIndex >= sourceValue.length())
         {
            return "";
         }

         if(length == null)
         {
            return sourceValue.substring(fromIndex);
         }
         else
         {
            Integer toIndex = Math.min(fromIndex + length, sourceValue.length());
            return sourceValue.substring(fromIndex, toIndex);
         }
      }
   }

}
