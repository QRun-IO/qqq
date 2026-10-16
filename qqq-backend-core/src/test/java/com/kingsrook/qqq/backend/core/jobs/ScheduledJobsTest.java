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
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerUtils;
import com.kingsrook.qqq.backend.core.serviceidentity.QServicePrincipalMetaData;
import com.kingsrook.qqq.backend.core.serviceidentity.QServiceSession;
import com.kingsrook.qqq.backend.core.serviceidentity.ServiceIdentitiesMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


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



   /*******************************************************************************
    ** With scheduledJobsServicePrincipal, the principal owns the job and the
    ** worker re-mints the principal's session at each claim.
    *******************************************************************************/
   @Test
   void servicePrincipalOwnsScheduledJobs() throws Exception
   {
      ServiceIdentitiesMetaData.declare(instance, new QServicePrincipalMetaData().withName("scheduler"));
      settings.withScheduledJobsServicePrincipal("scheduler").withScheduledJobsUserId("scheduler-bot");
      QProcessMetaData process = instance.getProcess(DurableJobsTestSupport.PROCESS_THREE_STEPS);

      SchedulerUtils.runProcess(instance, () -> new QSystemUserSession(instance), process, null, Map.of("seed", "nightly"));
      QContext.init(instance, new QSystemUserSession(instance));

      List<QRecord> jobs = SystemScope.call(() -> new QueryAction().execute(new QueryInput(DurableJobsTables.TABLE_JOB)).getRecords());
      assertThat(jobs).singleElement().satisfies(job -> assertThat(job.getValueString("userId")).isEqualTo("service:scheduler"));

      QSession claimSession = JobSessions.forJob(instance, jobs.get(0));
      assertThat(claimSession).isInstanceOf(QServiceSession.class);
      assertThat(claimSession.getUser().getIdReference()).isEqualTo("service:scheduler");
      assertThat(JobSessions.isTokenSession(claimSession)).isFalse();

      DurableJobsRuntime.start(instance);
      await(jobs.get(0).getValueInteger("id"), JobStatus.COMPLETED);
      assertThat(DurableJobsTestSupport.runs("step3")).isEqualTo(1);
   }



   /*******************************************************************************
    ** Unfinished scheduler jobs of the old scheduledJobsUserId move to the
    ** principal (once per node); finished ones and other owners are untouched.
    *******************************************************************************/
   @Test
   void legacyScheduledJobsMoveToThePrincipal() throws Exception
   {
      ServiceIdentitiesMetaData.declare(instance, new QServicePrincipalMetaData().withName("scheduler"));
      settings.withScheduledJobsUserId("scheduler-bot");
      QProcessMetaData process = instance.getProcess(DurableJobsTestSupport.PROCESS_THREE_STEPS);
      SchedulerUtils.runProcess(instance, () -> new QSystemUserSession(instance), process, null, Map.of("seed", "a"));
      QContext.init(instance, new QSystemUserSession(instance));
      Integer legacyId = SystemScope.call(() -> new QueryAction().execute(new QueryInput(DurableJobsTables.TABLE_JOB)).getRecords()).get(0).getValueInteger("id");
      Integer doneId = SystemScope.call(() -> new InsertAction().execute(new InsertInput(DurableJobsTables.TABLE_JOB).withRecord(new QRecord()
         .withValue("userId", "scheduler-bot").withValue("clientChannel", ScheduledJobs.CHANNEL_SCHEDULER).withValue("status", JobStatus.COMPLETED.getPossibleValueId())
         .withValue("processName", DurableJobsTestSupport.PROCESS_THREE_STEPS))).getRecords().get(0).getValueInteger("id"));

      settings.withScheduledJobsServicePrincipal("scheduler");
      ScheduledJobs.ownerSession(instance);

      assertThat(SystemScope.call(() -> DurableJobs.status(legacyId)).getValueString("userId")).isEqualTo("service:scheduler");
      assertThat(SystemScope.call(() -> DurableJobs.status(doneId)).getValueString("userId")).isEqualTo("scheduler-bot");
   }



   /*******************************************************************************
    ** A configured but undeclared/unavailable principal fails the tick: never a
    ** fallback to the user or the system user.
    *******************************************************************************/
   @Test
   void unavailablePrincipalFailsClosed()
   {
      settings.withScheduledJobsServicePrincipal("scheduler").withScheduledJobsUserId("scheduler-bot");
      QContext.setQSession(new QSystemUserSession(instance));
      assertThatThrownBy(() -> ScheduledJobs.ownerSession(instance)).isInstanceOf(QAuthenticationException.class);
   }



   /*******************************************************************************
    ** Review H2: only a principal's own minted session may start a job owned by
    ** a principal; a human whose id merely starts with service: cannot.
    *******************************************************************************/
   @Test
   void humanCannotLaunchAPrincipalOwnedJob()
   {
      ServiceIdentitiesMetaData.declare(instance, new QServicePrincipalMetaData().withName("scheduler"));
      QContext.setQSession(new QSession().withUser(new com.kingsrook.qqq.backend.core.model.session.QUser().withIdReference("service:scheduler")));
      assertThatThrownBy(() -> DurableJobs.launch(new com.kingsrook.qqq.backend.core.jobs.model.LaunchRequest(DurableJobsTestSupport.PROCESS_THREE_STEPS).withValues(Map.of("seed", "x"))))
         .isInstanceOf(com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** A user-owned job with no grant runs on that user's behalf through the
    ** scheduler principal - the user's full access, attributed to both - and
    ** completes (owner decision 2026-10-16).
    *******************************************************************************/
   @Test
   void userOwnedJobRunsWithTheUsersAccessViaTheScheduler() throws Exception
   {
      ServiceIdentitiesMetaData.declare(instance, new QServicePrincipalMetaData().withName("scheduler").withMayActOnBehalfOfUsers(true));
      QProcessMetaData process = instance.getProcess(DurableJobsTestSupport.PROCESS_THREE_STEPS);
      Integer jobId = ScheduledJobs.launchIfDurable(process, Map.of("seed", "mine"));
      QRecord job = DurableJobs.status(jobId);
      assertThat(job.getValueString("userId")).isEqualTo(DEFAULT_USER_ID);

      QSession claim = JobSessions.forJob(instance, job);
      assertThat(claim).isInstanceOf(com.kingsrook.qqq.backend.core.serviceidentity.QDelegatedSession.class);
      assertThat(claim.getUser().getIdReference()).isEqualTo(DEFAULT_USER_ID);
      assertThat(com.kingsrook.qqq.backend.core.serviceidentity.ServiceIdentities.attributionName(claim)).endsWith("via service:scheduler");

      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.COMPLETED);
      assertThat(DurableJobsTestSupport.runs("step3")).isEqualTo(1);
   }
}
