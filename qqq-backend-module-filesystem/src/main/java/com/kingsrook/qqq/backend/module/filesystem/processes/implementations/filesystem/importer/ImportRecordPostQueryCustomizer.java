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

package com.kingsrook.qqq.backend.module.filesystem.processes.implementations.filesystem.importer;


import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPostQueryCustomizer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.SystemErrorStatusMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import org.json.JSONObject;


/*******************************************************************************
 ** combine all unstructured fields of the record into a JSON blob in the "values" field.
 *******************************************************************************/
public class ImportRecordPostQueryCustomizer extends AbstractPostQueryCustomizer
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> apply(List<QRecord> records)
   {

      if(CollectionUtils.nullSafeHasContents(records))
      {
         QTableMetaData table = null;
         if(StringUtils.hasContent(records.get(0).getTableName()))
         {
            table = QContext.getQInstance().getTable(records.get(0).getTableName());
         }

         for(QRecord record : records)
         {
            try
            {
               if(record.getValues().containsKey("values"))
               {
                  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // if the record has a json blob of "values", copy the values out of there, and put them directly in the record's values map. //
                  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  JSONObject jsonObject = JsonUtils.toJSONObject(record.getValueString("values"));
                  for(String key : jsonObject.keySet())
                  {
                     if(!record.getValues().containsKey(key))
                     {
                        record.setValue(key, jsonObject.get(key));
                     }
                  }
               }
               else
               {
                  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  // remove known values from a clone of the values map - then only put the un-structured values in a JSON document in the values field //
                  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                  Map<String, Serializable> values = record.getValues();
                  if(table != null)
                  {
                     values = new HashMap<>(values);
                     for(String fieldName : table.getFields().keySet())
                     {
                        values.remove(fieldName);
                     }
                  }

                  String valuesJson = JsonUtils.toJson(values);
                  record.setValue("values", valuesJson);
               }
            }
            catch(Exception e)
            {
               record.addError(new SystemErrorStatusMessage("Error processing unstructured values in record: " + e.getMessage()));
            }
         }
      }

      return (records);
   }

}
