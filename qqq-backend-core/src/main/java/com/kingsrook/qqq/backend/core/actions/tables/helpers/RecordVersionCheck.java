/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.RecordVersionConflictError;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Best-effort optimistic preflight using the already permission-filtered old rows.
 *******************************************************************************/
public class RecordVersionCheck
{
   /*******************************************************************************
    ** Compare typed versions before defaults change the caller supplied values.
    *******************************************************************************/
   public static void check(UpdateInput input, List<QRecord> oldRecords)
   {
      QTableMetaData table = input.getTable();
      String versionField = table.getRecordVersionField();
      if(input.getExpectedVersions() == null || versionField == null || versionField.isEmpty())
      {
         return;
      }
      Map<String, Serializable> expected = new LinkedHashMap<>();
      input.getExpectedVersions().forEach((key, value) -> expected.put(ValueUtils.getValueAsString(key), value));
      Map<String, QRecord> oldByKey = new LinkedHashMap<>();
      oldRecords.forEach(record -> oldByKey.put(ValueUtils.getValueAsString(record.getValue(table.getPrimaryKeyField())), record));
      for(QRecord record : input.getRecords())
      {
         String key = ValueUtils.getValueAsString(record.getValue(table.getPrimaryKeyField()));
         QRecord old = oldByKey.get(key);
         if(old == null || !expected.containsKey(key))
         {
            continue;
         }
         Serializable current = old.getValue(versionField);
         QFieldMetaData field = table.getField(versionField);
         Serializable requested = field == null ? expected.get(key) : ValueUtils.getValueAsFieldType(table, field, expected.get(key));
         if(!Objects.equals(current, requested))
         {
            List<String> changed = record.getValues().keySet().stream()
               .filter(name -> !name.equals(table.getPrimaryKeyField()) && !Objects.deepEquals(old.getValue(name), record.getValue(name)))
               .toList();
            Serializable modifiedBy = table.getFields().containsKey("modifiedByUserId") ? old.getValue("modifiedByUserId") : old.getValue("modifiedBy");
            String modifyDate = table.getFields().values().stream()
               .filter(candidate -> DynamicDefaultValueBehavior.MODIFY_DATE.equals(candidate.getBehaviorOnlyIfSet(DynamicDefaultValueBehavior.class)))
               .map(QFieldMetaData::getName).findFirst().orElse(null);
            record.addError(new RecordVersionConflictError(current, modifiedBy, modifyDate == null ? null : old.getValue(modifyDate), changed));
         }
      }
   }
}

