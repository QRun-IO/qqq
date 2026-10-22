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


package com.kingsrook.qqq.backend.core.jobs;


import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectStatus;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectStore;
import com.kingsrook.qqq.backend.core.jobs.effects.JobEffectType;
import com.kingsrook.qqq.backend.core.jobs.model.ChunkStatus;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.processes.JobEffectReviewProcesses;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Effect review through the same frontend-skipping record actions as job control.
 *******************************************************************************/
class JobEffectReviewProcessesTest extends DurableJobsTestBase
{
   /*******************************************************************************
    ** Each action resolves the effect, clears review and reopens the job for evaluation.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({ "jobsEffectMarkSent,SENT,Mark Effect Sent,marked sent", "jobsEffectResend,PENDING,Resend Effect,queued for resend", "jobsEffectSkip,FAILED,Skip Effect,skipped" })
   void resolvesEffect(String process, EffectStatus expected, String label, String past) throws Exception
   {
      Integer jobId    = launch();
      Integer effectId = insertEffect(jobId, EffectStatus.UNKNOWN);
      store.setJobStatus(jobId, EnumSet.of(JobStatus.QUEUED), JobStatus.NEEDS_REVIEW, null);
      List<ProcessSummaryLine> lines = runReview(process, effectId.toString());
      assertThat(lines).anySatisfy(line ->
      {
         assertThat(line.getCount()).isEqualTo(1);
         assertThat(line.getMessage()).isEqualTo("effect was " + past);
      });
      QRecord effect = new EffectStore(settings).get(effectId);
      assertThat(effect.getValueString("status")).isEqualTo(expected.name());
      assertThat(effect.getValueBoolean("needsReview")).isFalse();
      assertThat(effect.getValueString("lastError")).startsWith(label + " by Anonymous on ");
      assertThat(Instant.parse(effect.getValueString("lastError").split(" on ")[1])).isBeforeOrEqualTo(Instant.now());
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("RUNNING");
      assertThat(store.getJob(jobId).getValueString("statusReason")).isEqualTo("Effects reviewed");
      assertThat(instance.getProcess(process).getLabel()).isEqualTo(label);
   }



   /*******************************************************************************
    ** The last reviewed effect releases a finished job into its correct terminal state.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({ "jobsEffectMarkSent,COMPLETED", "jobsEffectSkip,COMPLETED_WITH_ERRORS", "jobsEffectResend,RUNNING" })
   void finalizesAfterLastReview(String process, String expected) throws Exception
   {
      Integer jobId    = launch();
      Integer effectId = insertEffect(jobId, EffectStatus.UNKNOWN);
      store.setChunkStatuses(jobId, EnumSet.of(ChunkStatus.PENDING), ChunkStatus.DONE, Map.of());
      DurableJobs.evaluate(jobId);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("NEEDS_REVIEW");
      runReview(process, effectId.toString());
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo(expected);
   }



   /*******************************************************************************
    ** All three actions explain why in-flight and delivered effects are left alone.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = { "jobsEffectMarkSent", "jobsEffectResend", "jobsEffectSkip" })
   void skipsSendingAndSent(String process) throws Exception
   {
      Integer jobId   = launch();
      Integer sending = insertEffect(jobId, EffectStatus.SENDING);
      Integer sent    = insertEffect(jobId, EffectStatus.SENT);
      Integer review  = insertEffect(jobId, EffectStatus.UNKNOWN);
      List<ProcessSummaryLine> lines = runReview(process, sending + "," + sent + "," + review);
      assertThat(lines).anySatisfy(line ->
      {
         assertThat(line.getCount()).isEqualTo(2);
         assertThat(line.getSingularPastMessage()).isEqualTo("effect was skipped");
         assertThat(line.getMessage()).contains("SENDING", "SENT", "delivered");
      });
      assertThat(new EffectStore(settings).get(sending).getValueString("status")).isEqualTo("SENDING");
      assertThat(new EffectStore(settings).get(sent).getValueString("status")).isEqualTo("SENT");
      assertThat(new EffectStore(settings).get(review).getValueBoolean("needsReview")).isFalse();
   }



   /*******************************************************************************
    ** Another user cannot select or resolve an owner's effect through a record action.
    *******************************************************************************/
   @ParameterizedTest
   @ValueSource(strings = { "jobsEffectMarkSent", "jobsEffectResend", "jobsEffectSkip" })
   void refusesAnotherUser(String process) throws Exception
   {
      Integer jobId    = launch();
      Integer effectId = insertEffect(jobId, EffectStatus.UNKNOWN);
      QContext.init(instance, newSession("someone-else"));
      assertThat(runReview(process, effectId.toString())).isEmpty();
      assertThat(new EffectStore(settings).get(effectId).getValueString("status")).isEqualTo("UNKNOWN");
      assertThat(new EffectStore(settings).get(effectId).getValueBoolean("needsReview")).isTrue();
   }



   /*******************************************************************************
    ** Load rechecks ownership even if a record was selected before the user changed.
    *******************************************************************************/
   @Test
   void loadRefusesStaleSelectionFromAnotherOwner() throws Exception
   {
      Integer jobId    = launch();
      Integer effectId = insertEffect(jobId, EffectStatus.UNKNOWN);
      QRecord effect   = new EffectStore(settings).get(effectId);
      QContext.init(instance, newSession("someone-else"));
      JobEffectReviewProcesses.Load load   = new JobEffectReviewProcesses.Load();
      RunBackendStepOutput          output = new RunBackendStepOutput();
      load.runOnePage(new RunBackendStepInput().withProcessName("jobsEffectMarkSent").withRecords(List.of(effect)), output);
      assertThat(load.getProcessSummary(output, true)).anySatisfy(line ->
      {
         ProcessSummaryLine summary = (ProcessSummaryLine) line;
         assertThat(summary.getStatus()).isEqualTo(Status.ERROR);
         assertThat(summary.getCount()).isEqualTo(1);
         assertThat(summary.getMessage()).contains("own jobs");
      });
      assertThat(new EffectStore(settings).get(effectId).getValueString("status")).isEqualTo("UNKNOWN");
   }



   /*******************************************************************************
    ** An administrator can resolve another user's effect and an absent name falls back to id.
    *******************************************************************************/
   @Test
   void administratorCanResolveWithIdInNote() throws Exception
   {
      Integer jobId    = launch();
      Integer effectId = insertEffect(jobId, EffectStatus.UNKNOWN);
      QContext.init(instance, newSession("review-admin").withPermission(DurableJobsTables.PERMISSION_ADMIN));
      QContext.getQSession().getUser().setFullName(null);
      runReview("jobsEffectMarkSent", effectId.toString());
      QRecord effect = new EffectStore(settings).get(effectId);
      assertThat(effect.getValueString("status")).isEqualTo("SENT");
      assertThat(effect.getValueString("lastError")).startsWith("Mark Effect Sent by review-admin on ");
   }



   /*******************************************************************************
    ** A remaining unresolved effect keeps the job in review after one is marked sent.
    *******************************************************************************/
   @Test
   void otherReviewEffectStillBlocksCompletion() throws Exception
   {
      Integer jobId    = launch();
      Integer effectId = insertEffect(jobId, EffectStatus.UNKNOWN);
      insertEffect(jobId, EffectStatus.UNKNOWN);
      store.setChunkStatuses(jobId, EnumSet.of(ChunkStatus.PENDING), ChunkStatus.DONE, Map.of());
      DurableJobs.evaluate(jobId);
      runReview("jobsEffectMarkSent", effectId.toString());
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("NEEDS_REVIEW");
   }



   /*******************************************************************************
    ** Insert a review fixture through the same system scope used by the effect store.
    *******************************************************************************/
   private Integer insertEffect(Integer jobId, EffectStatus status) throws Exception
   {
      return (SystemScope.call(() ->
      {
         QRecord record = new InsertAction().execute(new InsertInput(DurableJobsTables.TABLE_EFFECT).withRecord(new QRecord()
            .withValue("jobId", jobId).withValue("chunkId", chunk(jobId).getValue("id")).withValue("chunkSeq", 0)
            .withValue("effectName", "test").withValue("rowKey", "row").withValue("targetName", "test")
            .withValue("idempotencyKey", UUID.randomUUID().toString()).withValue("effectType", JobEffectType.NON_IDEMPOTENT_EXTERNAL.name())
            .withValue("status", status.name()).withValue("needsReview", true).withValue("leaseVersion", 0)
            .withValue("payloadJson", "{}"))).getRecords().get(0);
         assertThat(record.getErrors()).isNullOrEmpty();
         return (record.getValueInteger("id"));
      }));
   }



   /*******************************************************************************
    ** Execute a record action and read the standard Result summary.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private List<ProcessSummaryLine> runReview(String process, String ids) throws Exception
   {
      DurableJobsSecurity.applyToSession(QContext.getQSession());
      RunProcessInput input = new RunProcessInput();
      input.setProcessName(process);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      input.addValue("recordIds", ids);
      RunProcessOutput output = new RunProcessAction().execute(input);
      assertThat(output.getException()).isEmpty();
      assertThat(output.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY)).isInstanceOf(List.class);
      return ((List<ProcessSummaryLine>) output.getValues().get(StreamedETLWithFrontendProcess.FIELD_PROCESS_SUMMARY));
   }
}
