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


import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobs;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsSecurity;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsTables;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectStatus;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectStore;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobFinalizer;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobSignals;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Owner/admin record actions for resolving external effects through standard process tracing.
 *******************************************************************************/
public final class JobEffectReviewProcesses
{
   /*******************************************************************************
    ** Not instantiable.
    *******************************************************************************/
   private JobEffectReviewProcesses()
   {
   }



   /*******************************************************************************
    ** Record actions with the same Review and Result screens as job control processes.
    *******************************************************************************/
   public static List<QProcessMetaData> defineAll()
   {
      List<QProcessMetaData> processes = new ArrayList<>();
      for(String name : List.of("jobsEffectMarkSent", "jobsEffectResend", "jobsEffectSkip"))
      {
         processes.add(StreamedETLWithFrontendProcess.processMetaDataBuilder()
            .withName(name)
            .withLabel(label(name))
            .withIcon(new QIcon("fact_check"))
            .withTableName(DurableJobsTables.TABLE_EFFECT)
            .withSourceTable(DurableJobsTables.TABLE_EFFECT)
            .withDestinationTable(DurableJobsTables.TABLE_EFFECT)
            .withExtractStepClass(ExtractViaQueryStep.class)
            .withTransformStepClass(Transform.class)
            .withLoadStepClass(Load.class)
            .withSupportsFullValidation(true)
            .withDoFullValidation(true)
            .withMinInputRecords(1)
            .withPreviewMessage("This is what " + label(name).toLowerCase() + " will do.")
            .getProcessMetaData());
      }
      return (processes);
   }



   /*******************************************************************************
    ** The operator-visible action name, also included in the resolution note.
    *******************************************************************************/
   private static String label(String name)
   {
      return (switch(name)
      {
         case "jobsEffectMarkSent" -> "Mark Effect Sent";
         case "jobsEffectResend" -> "Resend Effect";
         case "jobsEffectSkip" -> "Skip Effect";
         default -> throw new IllegalArgumentException("Not an effect review process: " + name);
      });
   }



   /*******************************************************************************
    ** The disposition explicitly chosen by the operator.
    *******************************************************************************/
   private static EffectStatus destination(String name)
   {
      return (switch(name)
      {
         case "jobsEffectMarkSent" -> EffectStatus.SENT;
         case "jobsEffectResend" -> EffectStatus.PENDING;
         case "jobsEffectSkip" -> EffectStatus.FAILED;
         default -> throw new IllegalArgumentException("Not an effect review process: " + name);
      });
   }



   /*******************************************************************************
    ** Standard countable future/past summary for a successful action.
    *******************************************************************************/
   private static ProcessSummaryLine summary(String name)
   {
      String past = switch(destination(name))
      {
         case SENT -> "marked sent";
         case PENDING -> "queued for resend";
         default -> "skipped";
      };
      return (new ProcessSummaryLine(Status.OK)
         .withSingularFutureMessage("effect will be " + past).withPluralFutureMessage("effects will be " + past)
         .withSingularPastMessage("effect was " + past).withPluralPastMessage("effects were " + past));
   }



   /*******************************************************************************
    ** Skipped effects must remain visible in both Review and Result summaries.
    *******************************************************************************/
   private static ProcessSummaryLine skippedSummary()
   {
      return (new ProcessSummaryLine(Status.INFO)
         .withSingularFutureMessage("effect will be skipped").withPluralFutureMessage("effects will be skipped")
         .withSingularPastMessage("effect was skipped").withPluralPastMessage("effects were skipped"));
   }



   /*******************************************************************************
    ** Review only effects that are neither already sent nor held by a dispatcher.
    *******************************************************************************/
   public static class Transform extends AbstractTransformStep implements ProcessSummaryProviderInterface
   {
      private ProcessSummaryLine will;
      private ProcessSummaryLine skipped;



      /*******************************************************************************
       ** Summarize eligible effects and explain why protected statuses are skipped.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         if(will == null)
         {
            will = summary(input.getProcessName());
            skipped = skippedSummary();
         }
         for(QRecord record : input.getRecords())
         {
            EffectStatus status = EffectStatus.valueOf(record.getValueString("status"));
            if(status == EffectStatus.SENDING || status == EffectStatus.SENT)
            {
               skipped.incrementCountAndAddPrimaryKey(record.getValue("id"));
               skipped.setMessage("SENDING effects are being delivered; SENT effects have already been delivered.");
            }
            else
            {
               will.incrementCountAndAddPrimaryKey(record.getValue("id"));
               output.addRecord(record);
            }
         }
      }



      /*******************************************************************************
       ** Supply the standard Review summary lines.
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
    ** Resolve each effect after checking its job owner and re-reading its current state.
    *******************************************************************************/
   public static class Load extends AbstractLoadStep implements ProcessSummaryProviderInterface
   {
      private ProcessSummaryLine done;
      private ProcessSummaryLine failed;
      private ProcessSummaryLine skipped;



      /*******************************************************************************
       ** Apply the operator's decision, clear its alert, then re-evaluate job completion.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         String name = input.getProcessName();
         if(done == null)
         {
            done = summary(name);
            failed = new ProcessSummaryLine(Status.ERROR)
               .withSingularFutureMessage("effect will fail").withPluralFutureMessage("effects will fail")
               .withSingularPastMessage("effect could not be changed").withPluralPastMessage("effects could not be changed");
            skipped = skippedSummary();
         }
         EffectStore     effects = new EffectStore(DurableJobs.settings());
         DurableJobStore store   = new DurableJobStore(DurableJobs.settings());
         QSession        session = QContext.getQSession();
         String          actor   = DurableJobsSecurity.userId(session);
         String          user    = session.getUser() == null ? null : session.getUser().getFullName();
         if(!StringUtils.hasContent(user))
         {
            user = actor;
         }
         for(QRecord record : input.getRecords())
         {
            Integer effectId = record.getValueInteger("id");
            try
            {
               QRecord effect = effects.get(effectId);
               if(effect == null)
               {
                  skipped.incrementCountAndAddPrimaryKey(effectId);
                  skipped.setMessage("Effect no longer exists.");
                  continue;
               }
               Integer jobId = effect.getValueInteger("jobId");
               QRecord job   = store.getJob(jobId);
               if(job == null || !(session.hasPermission(DurableJobsTables.PERMISSION_ADMIN) || (actor != null && actor.equals(job.getValueString("userId")))))
               {
                  throw (new QPermissionDeniedException("You can only review effects for your own jobs."));
               }
               String note = label(name) + " by " + user + " on " + Instant.now();
               if(!effects.resolve(effectId, destination(name), note))
               {
                  skipped.incrementCountAndAddPrimaryKey(effectId);
                  skipped.setMessage("Effect is SENDING, SENT, or changed while it was being reviewed.");
                  continue;
               }
               JobSignals.effectResolved(effectId, note);
               store.setJobStatus(jobId, EnumSet.of(JobStatus.NEEDS_REVIEW), JobStatus.RUNNING, Map.of("statusReason", "Effects reviewed"));
               new JobFinalizer(store).evaluate(jobId);
               done.incrementCountAndAddPrimaryKey(effectId);
            }
            catch(QPermissionDeniedException e)
            {
               failed.incrementCountAndAddPrimaryKey(effectId);
               failed.setMessage(e.getMessage());
            }
         }
      }



      /*******************************************************************************
       ** Include effects skipped during Review so the Result accounts for every selection.
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         if(done != null)
         {
            done.addSelfToListIfAnyCount(lines);
            failed.addSelfToListIfAnyCount(lines);
            skipped.addSelfToListIfAnyCount(lines);
         }
         if(getTransformStep() instanceof Transform transform && transform.skipped != null)
         {
            transform.skipped.addSelfToListIfAnyCount(lines);
         }
         return (lines);
      }
   }
}
