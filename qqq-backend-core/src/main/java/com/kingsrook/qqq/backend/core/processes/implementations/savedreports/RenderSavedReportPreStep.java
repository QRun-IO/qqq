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

package com.kingsrook.qqq.backend.core.processes.implementations.savedreports;


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** initial backend-step before rendering a saved report.  does some basic
 ** validations, and then (in future) will set up input fields (how??) for the
 ** input screen.
 *******************************************************************************/
public class RenderSavedReportPreStep implements BackendStep
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      String storageTableName = runBackendStepInput.getValueString(RenderSavedReportMetaDataProducer.FIELD_NAME_STORAGE_TABLE_NAME);
      if(!StringUtils.hasContent(storageTableName))
      {
         throw (new QUserFacingException("Process configuration error:  Missing value for storageTableName."));
      }

      if(QContext.getQInstance().getTable(storageTableName) == null)
      {
         throw (new QUserFacingException("Process configuration error:  Unrecognized value for storageTableName - no table named [" + storageTableName + "] was found in the instance."));
      }

      List<QRecord> records = runBackendStepInput.getRecords();
      if(!CollectionUtils.nullSafeHasContents(records))
      {
         throw (new QUserFacingException("No report was selected or found."));
      }

      if(records.size() > 1)
      {
         throw (new QUserFacingException("You may only run 1 report at a time."));
      }

      ///////////////////////////////////////////////////////////////////////////////////////
      // put the savedReportId in values - this'll get passed into the widget, so it knows //
      // what report we're working with, and thus what inputs to prompt for                //
      // also put a value in just to help it know we're running the process                //
      ///////////////////////////////////////////////////////////////////////////////////////
      SavedReport savedReport = new SavedReport(records.get(0));
      runBackendStepOutput.addValue("savedReportId", savedReport.getId());
      runBackendStepOutput.addValue("processName", runBackendStepInput.getProcessName());
   }

}
