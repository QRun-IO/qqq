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


package com.kingsrook.qqq.backend.core.jobs.processes;


import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobs;
import com.kingsrook.qqq.backend.core.jobs.model.JobAction;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import static com.kingsrook.qqq.backend.core.jobs.DurableJobsTables.TABLE_JOB;


/*******************************************************************************
 ** The job control actions as record actions on jobs_job (spec section 2.7):
 ** standard streamed-ETL processes with the usual Review and Result summary
 ** lines, so every client runs them through the standard process API.
 **
 ** jobsStart, jobsPause, jobsResume, jobsCancel, jobsRetryFailed, jobsRestart,
 ** jobsRunAgain. Each record goes through DurableJobs.control, which enforces
 ** ownership (or jobs_admin) and the assistant confirmation gate.
 *******************************************************************************/
public final class JobControlProcesses
{

   /*******************************************************************************
    ** not instantiable
    *******************************************************************************/
   private JobControlProcesses()
   {
   }



   /*******************************************************************************
    ** The process name for an action.
    *******************************************************************************/
   public static String processName(JobAction action)
   {
      return (switch(action)
      {
         case START -> "jobsStart";
         case PAUSE -> "jobsPause";
         case RESUME -> "jobsResume";
         case CANCEL -> "jobsCancel";
         case RETRY_FAILED -> "jobsRetryFailed";
         case RESTART -> "jobsRestart";
         case RUN_AGAIN -> "jobsRunAgain";
      });
   }



   /*******************************************************************************
    ** The action a process name runs.
    *******************************************************************************/
   static JobAction action(String processName) throws QException
   {
      for(JobAction action : JobAction.values())
      {
         if(processName(action).equals(processName))
         {
            return (action);
         }
      }
      throw (new QException("Not a job control process: " + processName));
   }



   /*******************************************************************************
    ** The statuses an action applies to (others are skipped on Review).
    *******************************************************************************/
   static Set<JobStatus> appliesTo(JobAction action)
   {
      return (switch(action)
      {
         case START -> EnumSet.of(JobStatus.AWAITING_CONFIRMATION);
         case PAUSE -> EnumSet.of(JobStatus.AWAITING_CONFIRMATION, JobStatus.QUEUED, JobStatus.PLANNING, JobStatus.RUNNING);
         case RESUME -> EnumSet.of(JobStatus.PAUSED, JobStatus.PAUSING);
         case CANCEL -> EnumSet.of(JobStatus.AWAITING_CONFIRMATION, JobStatus.QUEUED, JobStatus.PLANNING, JobStatus.RUNNING, JobStatus.PAUSING, JobStatus.PAUSED, JobStatus.NEEDS_REVIEW);
         case RETRY_FAILED -> EnumSet.of(JobStatus.FAILED, JobStatus.NEEDS_REVIEW, JobStatus.COMPLETED_WITH_ERRORS);
         case RESTART -> EnumSet.of(JobStatus.CANCELLED, JobStatus.COMPLETED_WITH_ERRORS, JobStatus.FAILED, JobStatus.PAUSED, JobStatus.NEEDS_REVIEW);
         case RUN_AGAIN -> EnumSet.of(JobStatus.CANCELLED, JobStatus.COMPLETED, JobStatus.COMPLETED_WITH_ERRORS, JobStatus.FAILED);
      });
   }



   /*******************************************************************************
    ** All seven processes.
    *******************************************************************************/
   public static List<QProcessMetaData> defineAll()
   {
      List<QProcessMetaData> processes = new ArrayList<>();
      for(JobAction action : JobAction.values())
      {
         processes.add(StreamedETLWithFrontendProcess.processMetaDataBuilder()
            .withName(processName(action))
            .withLabel(action.getLabel())
            .withIcon(new QIcon(icon(action)))
            .withTableName(TABLE_JOB)
            .withSourceTable(TABLE_JOB)
            .withDestinationTable(TABLE_JOB)
            .withExtractStepClass(ExtractViaQueryStep.class)
            .withTransformStepClass(Transform.class)
            .withLoadStepClass(Load.class)
            .withSupportsFullValidation(true)
            .withDoFullValidation(true)
            .withMinInputRecords(1)
            .withPreviewMessage("This is what " + action.getLabel().toLowerCase() + " will do.")
            .getProcessMetaData());
      }
      return (processes);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String icon(JobAction action)
   {
      return (switch(action)
      {
         case START -> "play_arrow";
         case PAUSE -> "pause";
         case RESUME -> "play_circle";
         case CANCEL -> "cancel";
         case RETRY_FAILED -> "replay";
         case RESTART -> "restart_alt";
         case RUN_AGAIN -> "repeat";
      });
   }



   /*******************************************************************************
    ** Review: which jobs the action applies to, and which are skipped.
    *******************************************************************************/
   public static class Transform extends AbstractTransformStep implements ProcessSummaryProviderInterface
   {
      private ProcessSummaryLine will;
      private ProcessSummaryLine skipped;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         JobAction action = action(input.getProcessName());
         if(will == null)
         {
            will = new ProcessSummaryLine(Status.OK).withSingularFutureMessage("job will be " + action.getPastTense()).withPluralFutureMessage("jobs will be " + action.getPastTense())
               .withSingularPastMessage("job was " + action.getPastTense()).withPluralPastMessage("jobs were " + action.getPastTense());
            skipped = new ProcessSummaryLine(Status.INFO).withSingularFutureMessage("job will be skipped").withPluralFutureMessage("jobs will be skipped")
               .withSingularPastMessage("job was skipped").withPluralPastMessage("jobs were skipped");
         }

         for(QRecord record : input.getRecords())
         {
            JobStatus status = JobStatus.valueOf(record.getValueString("status"));
            if(appliesTo(action).contains(status))
            {
               will.incrementCountAndAddPrimaryKey(record.getValue("id"));
               output.addRecord(record);
            }
            else
            {
               skipped.incrementCountAndAddPrimaryKey(record.getValue("id"));
               skipped.setMessage("(" + status.getPossibleValueLabel().toLowerCase() + ": cannot " + action.getLabel().toLowerCase() + ")");
            }
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         if(will != null)
         {
            will.addSelfToListIfAnyCount(lines);
            skipped.addSelfToListIfAnyCount(lines);
         }
         return (lines);
      }
   }



   /*******************************************************************************
    ** Result: apply the action to each job.
    *******************************************************************************/
   public static class Load extends AbstractLoadStep implements ProcessSummaryProviderInterface
   {
      private ProcessSummaryLine       done;
      private ProcessSummaryLine       failed;
      private final List<ProcessSummaryLine> extra = new ArrayList<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         JobAction action = action(input.getProcessName());
         if(done == null)
         {
            done = new ProcessSummaryLine(Status.OK).withSingularFutureMessage("job will be " + action.getPastTense()).withPluralFutureMessage("jobs will be " + action.getPastTense())
               .withSingularPastMessage("job was " + action.getPastTense()).withPluralPastMessage("jobs were " + action.getPastTense());
            failed = new ProcessSummaryLine(Status.ERROR).withSingularFutureMessage("job will fail").withPluralFutureMessage("jobs will fail")
               .withSingularPastMessage("job could not be changed").withPluralPastMessage("jobs could not be changed");
         }

         for(QRecord record : input.getRecords())
         {
            try
            {
               String result = DurableJobs.control(record.getValueInteger("id"), action);
               done.incrementCountAndAddPrimaryKey(record.getValue("id"));
               if(action == JobAction.RUN_AGAIN || action == JobAction.CANCEL)
               {
                  extra.add(new ProcessSummaryLine(Status.INFO, null, result + (action == JobAction.CANCEL ? " Finished chunks stay applied; cancelling does not undo them." : "")));
               }
            }
            catch(QUserFacingException | QPermissionDeniedException e)
            {
               failed.incrementCountAndAddPrimaryKey(record.getValue("id"));
               failed.setMessage(e.getMessage());
            }
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         if(done != null)
         {
            done.addSelfToListIfAnyCount(lines);
            failed.addSelfToListIfAnyCount(lines);
         }

         ////////////////////////////////////////////////////////////////////
         // the Result screen shows only these lines: repeat the jobs the //
         // Review skipped (wrong status), so the result accounts for all //
         ////////////////////////////////////////////////////////////////////
         if(getTransformStep() instanceof Transform transform && transform.skipped != null)
         {
            transform.skipped.addSelfToListIfAnyCount(lines);
         }
         lines.addAll(extra);
         return (lines);
      }
   }

}
