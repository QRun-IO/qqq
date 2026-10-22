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

package com.kingsrook.sampleapp.processes.clonepeople;


import java.io.Serializable;
import java.util.ArrayList;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class ClonePeopleTransformStep extends AbstractTransformStep implements ProcessSummaryProviderInterface
{
   private ProcessSummaryLine okSummary            = new ProcessSummaryLine(Status.OK, 0, "can be cloned with no issues.");
   private ProcessSummaryLine warningCloneSummary  = new ProcessSummaryLine(Status.WARNING, 0, "can be cloned, but because are already a clone, their clone cannot be cloned in the future.");
   private ProcessSummaryLine refuseCloningSummary = new ProcessSummaryLine(Status.ERROR, 0, "declined cloning in this sample scenario");
   private ProcessSummaryLine nestedCloneSummary   = new ProcessSummaryLine(Status.ERROR, 0, "are already a clone of a clone, so they can't be cloned again.");



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      if(isForResultScreen)
      {
         okSummary.setMessage("were cloned");
         warningCloneSummary.setMessage("were already a clone, so they were cloned again now, but their clones cannot be cloned after this.");
         nestedCloneSummary.setMessage("are already a clone of a clone, so they weren't cloned again.");
      }

      ArrayList<ProcessSummaryLineInterface> rs = new ArrayList<>();
      okSummary.addSelfToListIfAnyCount(rs);
      warningCloneSummary.addSelfToListIfAnyCount(rs);
      refuseCloningSummary.addSelfToListIfAnyCount(rs);
      nestedCloneSummary.addSelfToListIfAnyCount(rs);
      return (rs);
   }



   /*******************************************************************************
    ** Execute the backend step - using the request as input, and the result as output.
    **
    *******************************************************************************/
   @Override
   public void runOnePage(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      for(QRecord inputPerson : runBackendStepInput.getRecords())
      {
         Serializable id = inputPerson.getValue("id");
         if("Morgan".equals(inputPerson.getValueString("firstName")))
         {
            refuseCloningSummary.incrementCountAndAddPrimaryKey(id);
         }
         else if(inputPerson.getValueString("firstName").matches("Clone of.*Clone of.*"))
         {
            nestedCloneSummary.incrementCountAndAddPrimaryKey(id);
         }
         else
         {
            QRecord outputPerson = new QRecord(inputPerson);
            outputPerson.setValue("id", null);
            outputPerson.setValue("firstName", "Clone of: " + inputPerson.getValueString("firstName"));
            runBackendStepOutput.getRecords().add(outputPerson);

            if(inputPerson.getValueString("firstName").matches("Clone of.*"))
            {
               warningCloneSummary.incrementCountAndAddPrimaryKey(id);
            }
            else
            {
               okSummary.incrementCountAndAddPrimaryKey(id);
            }
         }
      }
   }

}
