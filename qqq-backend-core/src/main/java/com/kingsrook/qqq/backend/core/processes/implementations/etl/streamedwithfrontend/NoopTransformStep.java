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
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Implementation of a TransformStep - it does nothing other than take input records
 ** and sets them in the output
 *******************************************************************************/
public class NoopTransformStep extends AbstractTransformStep
{
   private final String             okSummarySuffix = " successfully processed.";
   private final ProcessSummaryLine okSummary       = new ProcessSummaryLine(Status.OK)
      .withSingularFutureMessage("can be" + okSummarySuffix)
      .withPluralFutureMessage("can be" + okSummarySuffix)
      .withSingularPastMessage("has been" + okSummarySuffix)
      .withPluralPastMessage("have been" + okSummarySuffix);



   /*******************************************************************************
    ** getProcessSummary
    *
    *******************************************************************************/
   @Override
   public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      ArrayList<ProcessSummaryLineInterface> rs = new ArrayList<>();
      okSummary.addSelfToListIfAnyCount(rs);
      return (rs);
   }



   /*******************************************************************************
    ** run
    *
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      ////////////////////////////////
      // return if no input records //
      ////////////////////////////////
      if(CollectionUtils.nullSafeIsEmpty(runBackendStepInput.getRecords()))
      {
         return;
      }

      for(QRecord qRecord : runBackendStepInput.getRecords())
      {
         okSummary.incrementCountAndAddPrimaryKey(qRecord.getValue(runBackendStepInput.getTable().getPrimaryKeyField()));
         runBackendStepOutput.getRecords().add(qRecord);
      }
   }

}
