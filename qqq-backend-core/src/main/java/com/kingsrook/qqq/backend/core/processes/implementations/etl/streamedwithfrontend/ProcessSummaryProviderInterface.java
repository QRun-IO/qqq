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
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;


/*******************************************************************************
 ** Interface for a class that can provide a ProcessSummary - a list of Process Summary Lines
 *******************************************************************************/
public interface ProcessSummaryProviderInterface
{

   /*******************************************************************************
    ** Note - object needs to be serializable, and List isn't... so, use ArrayList?
    *******************************************************************************/
   ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen);


   /*******************************************************************************
    ** not meant to be overridden - meant to be called by framework - to make sure that
    ** all lines have their proper message picked (e.g., if they have singular/plural
    ** and past/future variants).
    *******************************************************************************/
   default ArrayList<ProcessSummaryLineInterface> doGetProcessSummary(RunBackendStepOutput runBackendStepOutput, boolean isForResultScreen)
   {
      ArrayList<ProcessSummaryLineInterface> processSummary = getProcessSummary(runBackendStepOutput, isForResultScreen);
      if(processSummary == null)
      {
         return (null);
      }

      for(ProcessSummaryLineInterface processSummaryLine : processSummary)
      {
         processSummaryLine.prepareForFrontend(isForResultScreen);
      }

      return (processSummary);
   }

}
