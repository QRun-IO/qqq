/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.tracing;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.logging.LogPair;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Implementation of ProcessTracerInterface that writes messages to the Logger.
 *******************************************************************************/
public class LoggingProcessTracer implements ProcessTracerInterface
{
   private static final QLogger LOG = QLogger.getLogger(LoggingProcessTracer.class);

   private long startMillis;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleProcessStart(RunProcessInput runProcessInput)
   {
      startMillis = System.currentTimeMillis();
      LOG.info("Starting process", logPair("name", runProcessInput.getProcessName()), logPair("uuid", runProcessInput.getProcessUUID()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleProcessResume(RunProcessInput runProcessInput)
   {
      String atOrAfter     = "after";
      String atOrAfterStep = runProcessInput.getStartAfterStep();
      if(StringUtils.hasContent(runProcessInput.getStartAtStep()))
      {
         atOrAfter = "at";
         atOrAfterStep = runProcessInput.getStartAtStep();
      }
      LOG.info("Resuming process", logPair("name", runProcessInput.getProcessName()), logPair("uuid", runProcessInput.getProcessUUID()), logPair(atOrAfter, atOrAfterStep));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleStepStart(RunBackendStepInput runBackendStepInput)
   {
      LOG.info("Starting process step", runBackendStepInput.getStepName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleMessage(RunBackendStepInput runBackendStepInput, ProcessTracerMessage message)
   {
      LOG.info("Message from process", logPair("message", message.getMessage()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleStepFinish(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput)
   {
      LOG.info("Finished process step", logPair("name", runBackendStepInput.getStepName()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleProcessBreak(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput, Exception processException)
   {
      LOG.info("Breaking process", logPair("name", runProcessInput.getProcessName()), logPair("uuid", runProcessInput.getProcessUUID()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void handleProcessFinish(RunProcessInput runProcessInput, RunProcessOutput runProcessOutput, Exception processException)
   {
      long finishMillis = System.currentTimeMillis();

      List<LogPair> summaryLogPairs = new ArrayList<>();
      Serializable  processSummary  = runProcessOutput.getValue(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY);
      if(processSummary instanceof List)
      {
         List<? extends ProcessSummaryLineInterface> processSummaryLines = (List<? extends ProcessSummaryLineInterface>) processSummary;
         for(ProcessSummaryLineInterface processSummaryLineInterface : processSummaryLines)
         {
            if(processSummaryLineInterface instanceof ProcessSummaryLine processSummaryLine)
            {
               summaryLogPairs.add(logPair(String.valueOf(summaryLogPairs.size()), logPair("status", processSummaryLine.getStatus()), logPair("count", processSummaryLine.getCount()), logPair("message", processSummaryLine.getMessage())));
            }
            else
            {
               summaryLogPairs.add(logPair(String.valueOf(summaryLogPairs.size()), logPair("message", processSummaryLineInterface.getMessage())));
            }
         }
      }

      LOG.info("Finished process", logPair("name", runProcessInput.getProcessName()), logPair("uuid", runProcessInput.getProcessUUID()), logPair("millis", finishMillis - startMillis), logPair("summary", summaryLogPairs));
   }
}
