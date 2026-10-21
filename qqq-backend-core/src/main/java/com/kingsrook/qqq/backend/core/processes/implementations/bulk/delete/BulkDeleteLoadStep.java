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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.delete;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaDeleteStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.general.ProcessSummaryWarningsAndErrorsRollup;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Load step for bulk-delete.
 *******************************************************************************/
public class BulkDeleteLoadStep extends LoadViaDeleteStep implements ProcessSummaryProviderInterface
{
   private ProcessSummaryLine       okSummary     = new ProcessSummaryLine(Status.OK);
   private List<ProcessSummaryLine> infoSummaries = new ArrayList<>();

   private ProcessSummaryWarningsAndErrorsRollup processSummaryWarningsAndErrorsRollup = ProcessSummaryWarningsAndErrorsRollup.build("deleted");

   private String tableLabel;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected InputSource getInputSource()
   {
      return (QInputSource.USER);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void preRun(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      super.preRun(runBackendStepInput, runBackendStepOutput);

      QTableMetaData table = QContext.getQInstance().getTable(runBackendStepInput.getTableName());
      if(table != null)
      {
         tableLabel = table.getLabel();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      ArrayList<ProcessSummaryLineInterface> rs = new ArrayList<>();

      String noWarningsSuffix = processSummaryWarningsAndErrorsRollup.countWarnings() == 0 ? "" : " with no warnings";

      okSummary.setSingularPastMessage(tableLabel + " record was deleted" + noWarningsSuffix + ".");
      okSummary.setPluralPastMessage(tableLabel + " records were deleted" + noWarningsSuffix + ".");
      okSummary.pickMessage(isForResultScreen);
      okSummary.addSelfToListIfAnyCount(rs);

      processSummaryWarningsAndErrorsRollup.addToList(rs);
      rs.addAll(infoSummaries);
      return (rs);
   }



   /*******************************************************************************
    ** Execute the backend step - using the request as input, and the result as output.
    **
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ////////////////////////////
      // have base class delete //
      ////////////////////////////
      super.runOnePage(runBackendStepInput, runBackendStepOutput);

      QTableMetaData             table               = QContext.getQInstance().getTable(runBackendStepInput.getTableName());
      String                     primaryKeyFieldName = table.getPrimaryKeyField();
      Map<Serializable, QRecord> outputRecordMap     = runBackendStepOutput.getRecords().stream().collect(Collectors.toMap(r -> r.getValue(primaryKeyFieldName), r -> r, (a, b) -> a));

      ///////////////////////////////////////////////////////////////////////////////////////////////////
      // roll up the results, based on the input list, but looking for error/warnings from output list //
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      for(QRecord record : runBackendStepInput.getRecords())
      {
         Serializable recordPrimaryKey = record.getValue(primaryKeyFieldName);
         QRecord      outputRecord     = outputRecordMap.get(recordPrimaryKey);

         if(outputRecord != null && CollectionUtils.nullSafeHasContents(outputRecord.getErrors()))
         {
            String message = outputRecord.getErrors().get(0).getMessage();
            processSummaryWarningsAndErrorsRollup.addError(message, recordPrimaryKey);
         }
         else if(outputRecord != null && CollectionUtils.nullSafeHasContents(outputRecord.getWarnings()))
         {
            String message = outputRecord.getWarnings().get(0).getMessage();
            processSummaryWarningsAndErrorsRollup.addWarning(message, recordPrimaryKey);
         }
         else
         {
            okSummary.incrementCountAndAddPrimaryKey(recordPrimaryKey);
         }
      }
   }

}
