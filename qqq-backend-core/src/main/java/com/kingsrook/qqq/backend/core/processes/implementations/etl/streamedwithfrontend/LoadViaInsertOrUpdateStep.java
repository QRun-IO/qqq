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

package com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Generic implementation of a LoadStep - that runs Insert and/or Update
 ** actions for the destination table - where the presence or absence of the
 ** record's primaryKey field is the indicator for which to do.  e.g., it assumes
 ** auto-generated ids, to be populated upon insert.
 *******************************************************************************/
public class LoadViaInsertOrUpdateStep extends AbstractLoadStep
{
   public static final String FIELD_DESTINATION_TABLE     = "destinationTable";
   public static final String FIELD_SKIP_UNIQUE_KEY_CHECK = "skipUniqueKeyCheck";

   protected List<QRecord> recordsToInsert = null;
   protected List<QRecord> recordsToUpdate = null;



   /*******************************************************************************
    **
    *******************************************************************************/
   protected InputSource getInputSource()
   {
      return (QInputSource.SYSTEM);
   }



   /*******************************************************************************
    ** Execute the backend step - using the request as input, and the result as output.
    **
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      evaluateRecords(runBackendStepInput);
      insertAndUpdateRecords(runBackendStepInput, runBackendStepOutput);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void insertAndUpdateRecords(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      QTableMetaData tableMetaData = QContext.getQInstance().getTable(runBackendStepInput.getValueString(FIELD_DESTINATION_TABLE));

      if(CollectionUtils.nullSafeHasContents(recordsToInsert))
      {
         InsertInput insertInput = new InsertInput();
         insertInput.setInputSource(getInputSource());
         insertInput.setTableName(tableMetaData.getName());
         insertInput.setRecords(recordsToInsert);
         getTransaction().ifPresent(insertInput::setTransaction);
         insertInput.setAsyncJobCallback(runBackendStepInput.getAsyncJobCallback());

         if(runBackendStepInput.getValuePrimitiveBoolean(FIELD_SKIP_UNIQUE_KEY_CHECK))
         {
            insertInput.setSkipUniqueKeyCheck(true);
         }

         InsertOutput insertOutput = new InsertAction().execute(insertInput);
         runBackendStepOutput.getRecords().addAll(insertOutput.getRecords());
      }

      if(CollectionUtils.nullSafeHasContents(recordsToUpdate))
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setInputSource(getInputSource());
         updateInput.setTableName(tableMetaData.getName());
         updateInput.setRecords(recordsToUpdate);
         getTransaction().ifPresent(updateInput::setTransaction);
         updateInput.setAsyncJobCallback(runBackendStepInput.getAsyncJobCallback());
         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         runBackendStepOutput.getRecords().addAll(updateOutput.getRecords());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Optional<QBackendTransaction> openTransaction(RunBackendStepInput runBackendStepInput) throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(runBackendStepInput.getValueString(FIELD_DESTINATION_TABLE));
      return (Optional.of(QBackendTransaction.openFor(insertInput)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void evaluateRecords(RunBackendStepInput runBackendStepInput) throws QException
   {
      QTableMetaData tableMetaData = QContext.getQInstance().getTable(runBackendStepInput.getValueString(FIELD_DESTINATION_TABLE));
      recordsToInsert = new ArrayList<>();
      recordsToUpdate = new ArrayList<>();

      splitRecordsForInsertOrUpdate(runBackendStepInput.getRecords(), tableMetaData.getPrimaryKeyField(), recordsToInsert, recordsToUpdate);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void splitRecordsForInsertOrUpdate(List<QRecord> inputList, String primaryKeyFieldName, List<QRecord> insertList, List<QRecord> updateList)
   {
      for(QRecord record : inputList)
      {
         if(record.getValue(primaryKeyFieldName) == null)
         {
            insertList.add(record);
         }
         else
         {
            updateList.add(record);
         }
      }
   }
}
