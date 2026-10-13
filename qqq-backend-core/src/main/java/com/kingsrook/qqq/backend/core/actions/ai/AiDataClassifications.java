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

package com.kingsrook.qqq.backend.core.actions.ai;


import java.util.Collection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Resolve AI data classifications without changing metadata or exposure policy.
 *******************************************************************************/
public final class AiDataClassifications
{
   /*******************************************************************************
    ** Static utility class.
    *******************************************************************************/
   private AiDataClassifications()
   {
   }



   /*******************************************************************************
    ** The table's own classification, or null if not classified.
    *******************************************************************************/
   public static DataClassification ofTable(QTableMetaData table)
   {
      AiTableMetaData metaData = AiTableMetaData.of(table);
      return (metaData == null ? null : metaData.getClassification());
   }



   /*******************************************************************************
    ** The field's classification, falling back to the table's.
    ** Null or unknown field names also fall back to the table.
    *******************************************************************************/
   public static DataClassification ofField(QTableMetaData table, String fieldName)
   {
      AiFieldMetaData metaData = table == null || table.getFields() == null || fieldName == null ? null : AiFieldMetaData.of(table.getFields().get(fieldName));
      return (metaData == null || metaData.getClassification() == null ? ofTable(table) : metaData.getClassification());
   }



   /*******************************************************************************
    ** The most sensitive classification on the table or any of its fields.
    *******************************************************************************/
   public static DataClassification highest(QTableMetaData table)
   {
      DataClassification classification = ofTable(table);
      if(table != null && table.getFields() != null)
      {
         for(String fieldName : table.getFields().keySet())
         {
            classification = DataClassification.max(classification, ofField(table, fieldName));
         }
      }
      return (classification);
   }



   /*******************************************************************************
    ** The most sensitive effective classification among the named fields.
    ** A null or empty collection falls back to the table's classification.
    *******************************************************************************/
   public static DataClassification highest(QTableMetaData table, Collection<String> fieldNames)
   {
      if(fieldNames == null || fieldNames.isEmpty())
      {
         return (ofTable(table));
      }

      DataClassification classification = null;
      for(String fieldName : fieldNames)
      {
         classification = DataClassification.max(classification, ofField(table, fieldName));
      }
      return (classification);
   }
}
