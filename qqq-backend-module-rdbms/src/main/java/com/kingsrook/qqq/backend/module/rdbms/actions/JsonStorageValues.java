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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.JsonPathStorage;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Runtime custom field metadata and storage.
 *******************************************************************************/
public class JsonStorageValues
{
   /*******************************************************************************
    ** One representative per physical column, preserving declared order.
    *******************************************************************************/
   public static List<QFieldMetaData> columns(List<QFieldMetaData> fields)
   {
      Map<String, QFieldMetaData> columns = new LinkedHashMap<>();
      for(QFieldMetaData field : fields)
      {
         columns.putIfAbsent(AbstractRDBMSAction.getColumnName(field), field);
      }
      return (new ArrayList<>(columns.values()));
   }



   /*******************************************************************************
    ** Serialize typed scalar values; temporal values use their ISO representation.
    *******************************************************************************/
   public static Object jsonValue(QFieldMetaData field, Serializable value)
   {
      Object typed = ValueUtils.getValueAsFieldType(field.getType(), value);
      return (typed instanceof java.time.temporal.TemporalAccessor ? typed.toString() : typed);
   }



   /*******************************************************************************
    ** Non-null values to insert or merge for one JSON column.
    *******************************************************************************/
   public static String object(QRecord record, List<QFieldMetaData> fields, String column)
   {
      Map<String, Object> values = new LinkedHashMap<>();
      for(QFieldMetaData field : fields)
      {
         if(field.getBackendStorage() instanceof JsonPathStorage storage && storage.getColumnName().equals(column) && record.getValue(field.getName()) != null)
         {
            values.put(storage.getKey(), jsonValue(field, record.getValue(field.getName())));
         }
      }
      return (JsonUtils.toJson(values));
   }



   /*******************************************************************************
    ** PostgreSQL text-array literal. Validated keys contain only letters, digits and underscores.
    *******************************************************************************/
   public static String removedKeys(QRecord record, List<QFieldMetaData> fields, String column)
   {
      List<String> removed = new ArrayList<>();
      for(QFieldMetaData field : fields)
      {
         if(field.getBackendStorage() instanceof JsonPathStorage storage && storage.getColumnName().equals(column) && record.getValue(field.getName()) == null)
         {
            if(storage.getKey() == null || !storage.getKey().matches("[a-z][a-z0-9_]{0,40}"))
            {
               throw (new IllegalArgumentException("Invalid JSON storage key"));
            }
            removed.add(storage.getKey());
         }
      }
      return ("{" + String.join(",", removed) + "}");
   }
}
