/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.utils;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 ** Utility methods for working with QRecords (and the values they contain)
 *******************************************************************************/
public class QRecordUtils
{

   /*******************************************************************************
    ** given 2 records, and a collection of fields, identify any fields that are
    ** not equals between the records.
    *******************************************************************************/
   public static List<QFieldMetaData> getChangedFields(QRecord a, QRecord b, Collection<QFieldMetaData> fields)
   {
      List<QFieldMetaData> changedFields = new ArrayList<>();
      for(QFieldMetaData field : CollectionUtils.nonNullCollection(fields))
      {
         Serializable valueA = ValueUtils.getValueAsFieldType(field.getType(), a == null ? null : a.getValue(field.getName()));
         Serializable valueB = ValueUtils.getValueAsFieldType(field.getType(), b == null ? null : b.getValue(field.getName()));
         if(!Objects.equals(valueA, valueB))
         {
            changedFields.add(field);
         }
      }

      return (changedFields);
   }

}
