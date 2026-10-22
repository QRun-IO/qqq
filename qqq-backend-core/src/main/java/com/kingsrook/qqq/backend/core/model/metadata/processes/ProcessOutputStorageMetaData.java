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

package com.kingsrook.qqq.backend.core.model.metadata.processes;


import com.kingsrook.qqq.backend.core.actions.interfaces.QStorageInterface;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.QBackendModuleDispatcher;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryStorageAction;


/*******************************************************************************
 ** Storage table for process report downloads. Register with withSupplementalMetaData.
 *******************************************************************************/
public class ProcessOutputStorageMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "processOutputStorage";

   private String tableName;



   /*******************************************************************************
    ** Return the configured output storage, or null for legacy local output.
    *******************************************************************************/
   public static ProcessOutputStorageMetaData of(QInstance qInstance)
   {
      return QSupplementalInstanceMetaData.of(qInstance, NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return NAME;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public String getTableName()
   {
      return tableName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public ProcessOutputStorageMetaData withTableName(String tableName)
   {
      setTableName(tableName);
      return this;
   }



   /*******************************************************************************
    ** Validate existence and backend storage support, including memory table shape.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      QTableMetaData table = qInstance.getTable(tableName);
      validator.assertCondition(table != null, "Process output storage table [" + tableName + "] does not exist");
      if(table == null)
      {
         return;
      }

      try
      {
         QStorageInterface storage = new QBackendModuleDispatcher().getQBackendModule(qInstance.getBackend(table.getBackendName())).getStorageInterface();
         validator.assertCondition(storage != null, "Process output table [" + tableName + "] must support storage");
         if(storage instanceof MemoryStorageAction)
         {
            validator.assertCondition(table.getField(table.getPrimaryKeyField()) != null && QFieldType.STRING.equals(table.getField(table.getPrimaryKeyField()).getType())
               && table.getFields().values().stream().anyMatch(field -> QFieldType.BLOB.equals(field.getType())),
               "Process output memory storage table [" + tableName + "] must have a STRING primary key and a BLOB field");
         }
      }
      catch(Exception e)
      {
         validator.assertCondition(false, "Process output table [" + tableName + "] must support storage: " + e.getMessage());
      }
   }
}
