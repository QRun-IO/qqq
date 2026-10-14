/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.health;


import java.time.Clock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.permissions.UseTablePermissionCustomPermissionChecker;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;


/*******************************************************************************
 ** Standard streamed record action: preview counts only; load executes checks.
 *******************************************************************************/
public class HealthRunCheckNowProcess
{
   public static final String NAME = "healthRunCheckNow";



   /*******************************************************************************
    ** Auto-commit avoids holding a database transaction while a probe waits.
    *******************************************************************************/
   public static QProcessMetaData define()
   {
      return (StreamedETLWithFrontendProcess.defineProcessMetaData(ExtractViaQueryStep.class, Review.class, Run.class,
         Map.of(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, HealthMetaDataProvider.CHECK_TABLE,
            StreamedETLWithFrontendProcess.FIELD_SUPPORTS_FULL_VALIDATION, false,
            StreamedETLWithFrontendProcess.FIELD_TRANSACTION_LEVEL, StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_AUTO_COMMIT))
         .withName(NAME).withLabel("Run check now").withTableName(HealthMetaDataProvider.CHECK_TABLE)
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION)
            .withCustomPermissionChecker(UseTablePermissionCustomPermissionChecker.build(HealthMetaDataProvider.CHECK_TABLE, TablePermissionSubType.EDIT))));
   }



   /*******************************************************************************
    ** Preview and transform do not run checks; results accumulate during load.
    *******************************************************************************/
   public static class Review extends AbstractTransformStep
   {
      private int selected;
      private final Map<HealthStatus, Integer> counts = new EnumMap<>(HealthStatus.class);



      /*******************************************************************************
       ** Pass selected records to the load step.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output)
      {
         selected += input.getRecords().size();
         output.setRecords(input.getRecords());
      }



      /*******************************************************************************
       ** Review has a selection count; Result includes all four outcome counts.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         if(!isForResultScreen)
         {
            Integer total = output.getValueInteger(StreamedETLWithFrontendProcess.FIELD_RECORD_COUNT);
            int count = total == null ? selected : total;
            lines.add(new ProcessSummaryLine(Status.OK, count, count + " checks will run"));
         }
         else
         {
            for(HealthStatus status : HealthStatus.values())
            {
               int count = counts.getOrDefault(status, 0);
               lines.add(new ProcessSummaryLine(Status.OK, count, count + " " + status.name()));
            }
         }
         return (lines);
      }
   }



   /*******************************************************************************
    ** Execution only: a transient runner is closed even when a check fails.
    *******************************************************************************/
   public static class Run extends AbstractLoadStep
   {
      /*******************************************************************************
       ** Reuse the application runner where available.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         HealthRunner active = HealthRunner.getForInstance(QContext.getQInstance());
         boolean transientRunner = active == null;
         HealthRunner runner = transientRunner ? new HealthRunner(QContext.getQInstance(), "on-demand-" + UUID.randomUUID(), Clock.systemUTC()) : active;
         try
         {
            for(QRecord record : input.getRecords())
            {
               HealthCheckOutcome outcome = runner.runNow(record.getValueString("name"));
               ((Review) getTransformStep()).counts.merge(outcome.getStatus(), 1, Integer::sum);
            }
            output.setRecords(input.getRecords());
         }
         finally
         {
            if(transientRunner)
            {
               runner.close();
            }
         }
      }
   }
}
