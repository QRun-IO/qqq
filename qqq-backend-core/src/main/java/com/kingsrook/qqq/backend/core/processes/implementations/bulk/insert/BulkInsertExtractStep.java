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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert;


import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.tables.StorageAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.storage.StorageInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling.FileToRowsInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping.RowsToRecordInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkInsertMapping;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractExtractStep;


/*******************************************************************************
 ** Extract step for generic table bulk-insert ETL process
 **
 ** This step does a little bit of transforming, actually - taking rows from
 ** an uploaded file, and potentially merging them (for child-table use-cases)
 ** and applying the "Mapping" - to put fully built records into the pipe for the
 ** Transform step.
 *******************************************************************************/
public class BulkInsertExtractStep extends AbstractExtractStep
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      int rowsAdded     = 0;
      int originalLimit = Objects.requireNonNullElse(getLimit(), Integer.MAX_VALUE);

      StorageInput          storageInput      = BulkInsertStepUtils.getStorageInputForTheFile(runBackendStepInput);
      BulkInsertMapping     bulkInsertMapping = (BulkInsertMapping) runBackendStepOutput.getValue("bulkInsertMapping");
      RowsToRecordInterface rowsToRecord      = bulkInsertMapping.getLayout().newRowsToRecordInterface();

      try
         (
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////
            // open a stream to read from our file, and a FileToRows object, that knows how to read from that stream //
            ///////////////////////////////////////////////////////////////////////////////////////////////////////////
            InputStream inputStream = new StorageAction().getInputStream(storageInput);
            FileToRowsInterface fileToRowsInterface = FileToRowsInterface.forFile(storageInput.getReference(), inputStream);
         )
      {
         ///////////////////////////////////////////////////////////
         // read the header row (if this file & mapping uses one) //
         ///////////////////////////////////////////////////////////
         BulkLoadFileRow headerRow = bulkInsertMapping.getHasHeaderRow() ? fileToRowsInterface.next() : null;

         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         // while there are more rows in the file - and we're under the limit - get more records form the file //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////
         while(fileToRowsInterface.hasNext() && rowsAdded < originalLimit)
         {
            int remainingLimit = originalLimit - rowsAdded;

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // put a page-size limit on the rows-to-record class, so it won't be tempted to do whole file all at once //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////
            int           pageLimit = Math.min(remainingLimit, getMaxPageSize());
            List<QRecord> page      = rowsToRecord.nextPage(fileToRowsInterface, headerRow, bulkInsertMapping, pageLimit);

            if(page.size() > remainingLimit)
            {
               /////////////////////////////////////////////////////////////
               // in case we got back more than we asked for, sub-list it //
               /////////////////////////////////////////////////////////////
               page = page.subList(0, remainingLimit);
            }

            /////////////////////////////////////////////
            // send this page of records into the pipe //
            /////////////////////////////////////////////
            getRecordPipe().addRecords(page);
            rowsAdded += page.size();
         }
      }
      catch(QException qe)
      {
         throw qe;
      }
      catch(Exception e)
      {
         throw new QException("Unhandled error in bulk insert extract step", e);
      }

   }



   /***************************************************************************
    **
    ***************************************************************************/
   private int getMaxPageSize()
   {
      return (1000);
   }
}
