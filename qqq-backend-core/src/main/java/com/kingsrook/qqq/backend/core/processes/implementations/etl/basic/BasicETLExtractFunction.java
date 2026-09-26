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


import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.reporting.RecordPipe;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;


/*******************************************************************************
 ** Function body for performing the Extract step of a basic ETL process.
 *******************************************************************************/
public class BasicETLExtractFunction implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(BasicETLExtractFunction.class);

   private RecordPipe recordPipe = null;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      String tableName = runBackendStepInput.getValueString(BasicETLProcess.FIELD_SOURCE_TABLE);
      LOG.debug("Start query on table: " + tableName);

      QueryInput queryInput = new QueryInput();
      queryInput.setTableName(tableName);

      // queryRequest.setSkip(integerQueryParam(context, "skip"));
      // queryRequest.setLimit(integerQueryParam(context, "limit"));

      // todo? String filter = stringQueryParam(context, "filter");
      // if(filter != null)
      // {
      //    queryRequest.setFilter(JsonUtils.toObject(filter, QQueryFilter.class));
      // }

      //////////////////////////////////////////////////////////////////////
      // if the caller gave us a record pipe, pass it to the query action //
      //////////////////////////////////////////////////////////////////////
      if(recordPipe != null)
      {
         queryInput.setRecordPipe(recordPipe);
      }

      QueryAction queryAction = new QueryAction();
      QueryOutput queryOutput = queryAction.execute(queryInput);

      if(recordPipe == null)
      {
         ////////////////////////////////////////////////////////////////////////////
         // only return the records (and log about them) if there's no record pipe //
         ////////////////////////////////////////////////////////////////////////////
         runBackendStepOutput.setRecords(queryOutput.getRecords());
         LOG.info("Query on table " + tableName + " produced " + queryOutput.getRecords().size() + " records.");
      }
   }



   /*******************************************************************************
    ** Setter for recordPipe
    **
    *******************************************************************************/
   public void setRecordPipe(RecordPipe recordPipe)
   {
      this.recordPipe = recordPipe;
   }
}
