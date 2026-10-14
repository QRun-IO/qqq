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

package com.kingsrook.qqq.backend.core.utils;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.JsonPathStorage;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Runtime custom field metadata and storage.
 *******************************************************************************/
public class JsonFieldUtils
{
   /*******************************************************************************
    ** Read a typed logical value from a physical JSON map, including joined records.
    *******************************************************************************/
   public static Serializable read(QRecord record, QFieldMetaData field, String prefix)
   {
      if(field.getBackendStorage() instanceof JsonPathStorage storage)
      {
         Object raw = record.getValue(prefix + storage.getColumnName());
         if(raw instanceof Map<?, ?> map)
         {
            return (ValueUtils.getValueAsFieldType(field.getType(), map.get(storage.getKey())));
         }
      }
      return (record.getValue(prefix + field.getName()));
   }



   /*******************************************************************************
    ** Merge one logical value; null removes the key. Always copy the stored map.
    *******************************************************************************/
   public static void write(QRecord record, QFieldMetaData field, Serializable value)
   {
      if(field.getBackendStorage() instanceof JsonPathStorage storage)
      {
         LinkedHashMap<String, Serializable> map = new LinkedHashMap<>();
         if(record.getValue(storage.getColumnName()) instanceof Map<?, ?> previous)
         {
            previous.forEach((key, entry) -> map.put((String) key, (Serializable) entry));
         }
         if(value == null)
         {
            map.remove(storage.getKey());
         }
         else
         {
            map.put(storage.getKey(), ValueUtils.getValueAsFieldType(field.getType(), value));
         }
         record.setValue(storage.getColumnName(), map);
      }
      else
      {
         record.setValue(field.getName(), value);
      }
   }



   /*******************************************************************************
    ** Project logical fields without exposing the backing JSON column.
    *******************************************************************************/
   public static void project(QRecord record, QTableMetaData table, String prefix)
   {
      Map<String, Serializable> projected = new LinkedHashMap<>();
      for(QFieldMetaData field : table.getFields().values())
      {
         if(field.getBackendStorage() instanceof JsonPathStorage)
         {
            projected.put(prefix + field.getName(), read(record, field, prefix));
         }
      }
      for(QFieldMetaData field : table.getFields().values())
      {
         if(field.getBackendStorage() instanceof JsonPathStorage storage)
         {
            record.getValues().remove(prefix + storage.getColumnName());
         }
      }
      record.getValues().putAll(projected);
   }
}
