/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.customizers;


import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.collections.TypeTolerantKeyMap;


/*******************************************************************************
 ** utility class to help table customizers working with the oldRecordList.
 ** Usage is just 2 lines:
 ** outside of loop-over-records:
 **   - OldRecordHelper oldRecordHelper = new OldRecordHelper(updateInput.getTableName(), oldRecordList);
 ** then inside the record loop:
 **   - Optional<QRecord> oldRecord = oldRecordHelper.getOldRecord(record);
 *******************************************************************************/
public class OldRecordHelper
{
   private String     primaryKeyField;
   private QFieldType primaryKeyType;

   private Optional<List<QRecord>>    oldRecordList;
   private Map<Serializable, QRecord> oldRecordMap;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public OldRecordHelper(String tableName, Optional<List<QRecord>> oldRecordList)
   {
      this.primaryKeyField = QContext.getQInstance().getTable(tableName).getPrimaryKeyField();
      this.primaryKeyType = QContext.getQInstance().getTable(tableName).getField(primaryKeyField).getType();

      this.oldRecordList = oldRecordList;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public Optional<QRecord> getOldRecord(QRecord record)
   {
      if(oldRecordMap == null)
      {
         if(oldRecordList.isPresent())
         {
            oldRecordMap = new TypeTolerantKeyMap<>(primaryKeyType);
            oldRecordList.get().forEach(r -> oldRecordMap.put(r.getValue(primaryKeyField), r));
         }
         else
         {
            oldRecordMap = Collections.emptyMap();
         }
      }

      return (Optional.ofNullable(oldRecordMap.get(record.getValue(primaryKeyField))));
   }
}
