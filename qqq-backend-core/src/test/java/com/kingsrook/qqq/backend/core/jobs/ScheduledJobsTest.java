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


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerUtils;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Scheduled processes as durable jobs (spec section 2.4).
 *******************************************************************************/
class ScheduledJobsTest extends DurableJobsTestBase
{

   /*******************************************************************************
    ** The scheduler (as the system user) launches an opted-in process as a job
    ** owned by scheduledJobsUserId, does not run it inline, and the runtime
    ** completes it.
    *******************************************************************************/
   @Test
   void systemSchedulerLaunchesJobOwnedByConfiguredUser() throws Exception
   {
      settings.withScheduledJobsUserId("scheduler-bot");
      QProcessMetaData process = instance.getProcess(DurableJobsTestSupport.PROCESS_THREE_STEPS);

      SchedulerUtils.runProcess(instance, () -> new QSystemUserSession(instance), process, null, Map.of("seed", "nightly"));
      QContext.init(instance, new QSystemUserSession(instance));

      assertThat(DurableJobsTestSupport.runs("step1")).as("not run inline").isZero();
      List<QRecord> jobs = SystemScope.call(() -> new QueryAction().execute(new QueryInput(DurableJobsTables.TABLE_JOB)).getRecords());
      assertThat(jobs).singleElement().satisfies(job ->
      {
         assertThat(job.getValueString("userId")).isEqualTo("scheduler-bot");
         assertThat(job.getValueString("clientChannel")).isEqualTo(ScheduledJobs.CHANNEL_SCHEDULER);
         assertThat(job.getValueString("processName")).isEqualTo(DurableJobsTestSupport.PROCESS_THREE_STEPS);
         assertThat(job.getValueString("paramsJson")).contains("nightly");
      });

      DurableJobsRuntime.start(instance);
      await(jobs.get(0).getValueInteger("id"), JobStatus.COMPLETED);
      assertThat(DurableJobsTestSupport.runs("step3")).isEqualTo(1);
   }



   /*******************************************************************************
    ** A scheduler running as a real user owns the job itself.
    *******************************************************************************/
   @Test
   void userSchedulerOwnsTheJob() throws Exception
   {
      QProcessMetaData process = instance.getProcess(DurableJobsTestSupport.PROCESS_THREE_STEPS);
      Integer          jobId   = ScheduledJobs.launchIfDurable(process, Map.of("seed", "x"));
      assertThat(jobId).isNotNull();
      QRecord job = DurableJobs.status(jobId);
      assertThat(job.getValueString("userId")).isEqualTo(DEFAULT_USER_ID);
      assertThat(job.getValueString("clientChannel")).isEqualTo(ScheduledJobs.CHANNEL_SCHEDULER);
   }



   /*******************************************************************************
    ** Inline (null) when: no owner for a system scheduler, the setting is off,
    ** the process did not opt in, or it opted out.
    *******************************************************************************/
   @Test
   void staysInlineWhenNotOptedInOrNoOwner() throws Exception
   {
      QProcessMetaData process = instance.getProcess(DurableJobsTestSupport.PROCESS_THREE_STEPS);

      QContext.setQSession(new QSystemUserSession(instance));
      assertThat(ScheduledJobs.runsAsJob(process)).isTrue();
      assertThat(ScheduledJobs.launchIfDurable(process, Map.of())).as("system user, no scheduledJobsUserId").isNull();

      QContext.setQSession(newSession());
      settings.withScheduledRunsAsJobs(false);
      assertThat(ScheduledJobs.launchIfDurable(process, Map.of())).as("setting off").isNull();
      settings.withScheduledRunsAsJobs(true);

      QProcessMetaData plain = new QProcessMetaData().withName("plainScheduled")
         .withStepList(List.of(new QBackendStepMetaData().withName("step1").withCode(new QCodeReference(DurableJobsTestSupport.Step1.class))));
      instance.addProcess(plain);
      assertThat(ScheduledJobs.runsAsJob(plain)).as("no durable meta data").isFalse();
      settings.getEligibleProcessNames().add("plainScheduled");
      assertThat(ScheduledJobs.runsAsJob(plain)).as("named eligible").isTrue();

      process.withSupplementalMetaData(new DurableProcessMetaData().withEligible(false));
      assertThat(ScheduledJobs.runsAsJob(process)).as("opted out").isFalse();
      assertThat(ScheduledJobs.runsAsJob(null)).isFalse();
   }



   /*******************************************************************************
    ** An unknown scheduledJobsUserId (no automated session) stays inline.
    *******************************************************************************/
   @Test
   void unusableOwnerStaysInline() throws Exception
   {
      settings.withScheduledJobsUserId("scheduler-bot");
      QContext.setQSession(new QSystemUserSession(instance));
      assertThat(ScheduledJobs.ownerSession(instance)).isNotNull();
      assertThat(ScheduledJobs.ownerSession(instance).getUser().getIdReference()).isEqualTo("scheduler-bot");
      settings.withScheduledJobsUserId(" ");
      assertThat(ScheduledJobs.ownerSession(instance)).isNull();
   }
}
