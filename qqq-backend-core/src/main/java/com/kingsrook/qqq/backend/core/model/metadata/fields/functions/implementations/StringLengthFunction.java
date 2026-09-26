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
import java.util.Collections;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionParameter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionTypeIdentifier;


/***************************************************************************
 * FieldFunctionType implementation that returns the character length of a
 * string field's value, equivalent to SQL CHAR_LENGTH().
 *
 * <p>Returns null when the source field value is null.</p>
 ***************************************************************************/
public class StringLengthFunction implements FieldFunctionType
{
   public static final FieldFunctionTypeIdentifier IDENTIFIER = () -> "StringLength";



   /***************************************************************************
    * Returns this function's identifier (StringLength).
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
    * Returns an empty list — StringLength takes no parameters.
    ***************************************************************************/
   @Override
   public List<FieldFunctionParameter> getParameters()
   {
      return Collections.emptyList();
   }



   /***************************************************************************
    * Returns INTEGER, the type of the character-count result.
    ***************************************************************************/
   @Override
   public QFieldType getReturnType()
   {
      return QFieldType.INTEGER;
   }



   /***************************************************************************
    * Returns the character length of the field's string value, or null if the value is null.
    ***************************************************************************/
   @Override
   public Serializable apply(FieldFunction fieldFunction, QRecord record)
   {
      String sourceValue = record.getValueString(fieldFunction.getFieldName());
      if(sourceValue == null)
      {
         return (null);
      }

      return sourceValue.length();
   }

}
