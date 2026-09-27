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

package com.kingsrook.qqq.backend.core.processes.implementations.mergeduplicates;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.audits.AuditAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.audits.AuditInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaInsertOrUpdateStep;
import com.kingsrook.qqq.backend.core.utils.ListingHash;


/*******************************************************************************
 ** Generic ETL Load step for a merge duplicates process.
 **
 ** Uses otherTable* fields from the process state to do additional deletes, inserts
 ** and/or updates.
 *******************************************************************************/
public class MergeDuplicatesLoadStep extends LoadViaInsertOrUpdateStep
{
   private static final QLogger LOG = QLogger.getLogger(MergeDuplicatesLoadStep.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      super.runOnePage(runBackendStepInput, runBackendStepOutput);

      @SuppressWarnings("unchecked")
      ListingHash<String, Serializable> otherTableIdsToDelete = (ListingHash<String, Serializable>) runBackendStepInput.getValue("otherTableIdsToDelete");

      @SuppressWarnings("unchecked")
      ListingHash<String, QQueryFilter> otherTableFiltersToDelete = (ListingHash<String, QQueryFilter>) runBackendStepInput.getValue("otherTableFiltersToDelete");

      @SuppressWarnings("unchecked")
      ListingHash<String, QRecord> otherTableRecordsToStore = (ListingHash<String, QRecord>) runBackendStepInput.getValue("otherTableRecordsToStore");

      if(otherTableIdsToDelete != null)
      {
         for(String tableName : otherTableIdsToDelete.keySet())
         {
            DeleteInput deleteInput = new DeleteInput();
            deleteInput.setTableName(tableName);
            deleteInput.setPrimaryKeys(new ArrayList<>(otherTableIdsToDelete.get(tableName)));
            getTransaction().ifPresent(deleteInput::setTransaction);
            new DeleteAction().execute(deleteInput);
         }
      }

      if(otherTableFiltersToDelete != null)
      {
         for(String tableName : otherTableFiltersToDelete.keySet())
         {
            for(QQueryFilter filter : otherTableFiltersToDelete.get(tableName))
            {
               DeleteInput deleteInput = new DeleteInput();
               deleteInput.setTableName(tableName);
               deleteInput.setQueryFilter(filter);
               getTransaction().ifPresent(deleteInput::setTransaction);
               new DeleteAction().execute(deleteInput);
            }
         }
      }

      if(otherTableRecordsToStore != null)
      {
         for(String tableName : otherTableRecordsToStore.keySet())
         {
            QTableMetaData table = QContext.getQInstance().getTable(tableName);

            List<QRecord> recordsToInsert = new ArrayList<>();
            List<QRecord> recordsToUpdate = new ArrayList<>();

            splitRecordsForInsertOrUpdate(otherTableRecordsToStore.get(tableName), table.getPrimaryKeyField(), recordsToInsert, recordsToUpdate);

            InsertInput insertInput = new InsertInput();
            insertInput.setTableName(tableName);
            insertInput.setRecords(recordsToInsert);
            getTransaction().ifPresent(insertInput::setTransaction);
            new InsertAction().execute(insertInput);

            UpdateInput updateInput = new UpdateInput();
            updateInput.setTableName(tableName);
            updateInput.setRecords(recordsToUpdate);
            getTransaction().ifPresent(updateInput::setTransaction);
            new UpdateAction().execute(updateInput);
         }
      }

      AuditInput auditInput = (AuditInput) runBackendStepInput.getValue("auditInput");
      if(auditInput != null)
      {
         // todo exec async?
         new AuditAction().execute(auditInput);
         runBackendStepInput.addValue("auditInput", null);
      }
   }

}
