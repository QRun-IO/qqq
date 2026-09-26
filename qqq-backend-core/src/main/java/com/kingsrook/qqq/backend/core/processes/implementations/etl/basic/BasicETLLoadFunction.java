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

package com.kingsrook.qqq.backend.core.processes.implementations.etl.basic;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Function body for performing the Load step of a basic ETL process.
 *******************************************************************************/
public class BasicETLLoadFunction implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(BasicETLLoadFunction.class);

   private QBackendTransaction transaction;
   private boolean             returnStoredRecords = true;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      //////////////////////////////////////////////////////
      // exit early with no-op if no records made it here //
      //////////////////////////////////////////////////////
      List<QRecord> inputRecords = runBackendStepInput.getRecords();
      if(CollectionUtils.nullSafeIsEmpty(inputRecords))
      {
         runBackendStepOutput.addValue(BasicETLProcess.FIELD_RECORD_COUNT, 0);
         return;
      }

      LOG.info("Received [" + inputRecords.size() + "] records to load");

      //////////////////////////////////////////////////////////////////
      // put the destination table name in all records being inserted //
      //////////////////////////////////////////////////////////////////
      String table = runBackendStepInput.getValueString(BasicETLProcess.FIELD_DESTINATION_TABLE);
      for(QRecord record : inputRecords)
      {
         record.setTableName(table);
      }

      //////////////////////////////////////////
      // run an insert request on the records //
      //////////////////////////////////////////
      int           recordsInserted = 0;
      List<QRecord> outputRecords   = new ArrayList<>();
      int           pageSize        = 1000; // todo - make this a field?

      for(List<QRecord> page : CollectionUtils.getPages(inputRecords, pageSize))
      {
         LOG.info("Inserting a page of [" + page.size() + "] records. Progress:  " + recordsInserted + " loaded out of " + inputRecords.size() + " total");
         runBackendStepInput.getAsyncJobCallback().updateStatus("Inserting records", recordsInserted, inputRecords.size());

         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(table);
         insertInput.setRecords(page);
         insertInput.setTransaction(transaction);

         InsertAction insertAction = new InsertAction();
         InsertOutput insertOutput = insertAction.execute(insertInput);

         if(returnStoredRecords)
         {
            outputRecords.addAll(insertOutput.getRecords());
         }

         recordsInserted += insertOutput.getRecords().size();
      }
      runBackendStepOutput.setRecords(outputRecords);
      runBackendStepOutput.addValue(BasicETLProcess.FIELD_RECORD_COUNT, recordsInserted);
   }



   /*******************************************************************************
    ** Setter for transaction
    **
    *******************************************************************************/
   public void setTransaction(QBackendTransaction transaction)
   {
      this.transaction = transaction;
   }



   /*******************************************************************************
    ** Setter for returnStoredRecords
    **
    *******************************************************************************/
   public void setReturnStoredRecords(boolean returnStoredRecords)
   {
      this.returnStoredRecords = returnStoredRecords;
   }
}
