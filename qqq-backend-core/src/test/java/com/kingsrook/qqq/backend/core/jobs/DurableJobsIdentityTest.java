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


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.jobs.model.JobAction;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Regression coverage for DurableJobsIdentity.
 *******************************************************************************/
class DurableJobsIdentityTest extends DurableJobsTestBase
{

   /*******************************************************************************
    ** Current security keys are intersected with captured values and newly granted keys disappear.
    *******************************************************************************/
   @Test
   void intersectRetainsOnlyCapturedValues() throws Exception
   {
      QSession session = newSession().withSecurityKeyValue("tenant", 1).withSecurityKeyValue("tenant", 2)
         .withSecurityKeyValue("newGrant", "root").withSecurityKeyValue("lost", "current");
      Map<String, List<Serializable>> captured = Map.of("tenant", List.of("1", 3), "lost", List.of("previous"), "oldGrant", List.of("old"));
      JobSessions.intersect(session, captured);
      assertThat(session.getSecurityKeyValues()).containsOnlyKeys("tenant");
      assertThat(session.getSecurityKeyValues("tenant")).containsExactly(1);
   }



   /*******************************************************************************
    ** Authentication rejection fails the first claim without retrying any process step.
    *******************************************************************************/
   @Test
   void revokedOwnerFailsWithoutRetry() throws Exception
   {
      instance.getAuthentication().withCustomizer(new QCodeReference(DurableJobsTestSupport.RejectingCustomizer.class));
      Integer jobId = launch();
      store.updateJob(jobId, Map.of("userId", "revoked"));
      assertOwnerFailure(jobId);
   }



   /*******************************************************************************
    ** A missing owner session fails the first claim without retrying.
    *******************************************************************************/
   @Test
   void missingOwnerSessionFailsWithoutRetry() throws Exception
   {
      instance.getAuthentication().withModuleCodeReference(new QCodeReference(DurableJobsTestSupport.MissingOwnerAuthentication.class));
      Integer jobId = launch();
      store.updateJob(jobId, Map.of("userId", "missing"));
      assertOwnerFailure(jobId);
   }



   /*******************************************************************************
    ** A non-admin cannot control another owner's job.
    *******************************************************************************/
   @Test
   void differentUserCannotControlJob() throws Exception
   {
      Integer jobId = launch();
      QContext.init(instance, newSession("B"));
      assertThatThrownBy(() -> DurableJobs.control(jobId, JobAction.PAUSE)).isInstanceOf(QPermissionDeniedException.class);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("QUEUED");
   }



   /*******************************************************************************
    ** The jobs_admin permission authorizes another owner's control action.
    *******************************************************************************/
   @Test
   void adminCanControlOtherOwnersJob() throws Exception
   {
      Integer jobId = launch();
      QContext.init(instance, newSession("B").withPermission("jobs_admin"));
      DurableJobs.control(jobId, JobAction.PAUSE);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("PAUSED");
   }



   /*******************************************************************************
    ** Status lookup conceals another owner's job with not found.
    *******************************************************************************/
   @Test
   void differentUserCannotReadStatus() throws Exception
   {
      Integer jobId = launch();
      QContext.init(instance, newSession("B"));
      assertThatThrownBy(() -> DurableJobs.status(jobId)).isInstanceOf(QNotFoundException.class);
   }



   /*******************************************************************************
    ** Derived jobs keys contain the owner and grant all-access only to admins.
    *******************************************************************************/
   @Test
   void securityKeysFollowOwnerAndAdminPermission() throws Exception
   {
      QSession session = newSession("B");
      DurableJobsSecurity.applyToSession(session);
      assertThat(session.getSecurityKeyValues("jobs_user")).containsExactly("B");
      assertThat(session.getSecurityKeyValues("jobs_user_all_access")).isEmpty();
      session.withPermission("jobs_admin");
      DurableJobsSecurity.applyToSession(session);
      assertThat(session.getSecurityKeyValues("jobs_user")).containsExactly("B");
      assertThat(session.getSecurityKeyValues("jobs_user_all_access")).containsExactly(true);
      session.setPermissions(java.util.Set.of());
      DurableJobsSecurity.applyToSession(session);
      assertThat(session.getSecurityKeyValues("jobs_user_all_access")).isEmpty();
   }



   /*******************************************************************************
    ** QueryAction hides another owner's jobs while admins see all owners.
    *******************************************************************************/
   @Test
   void tableQueriesEnforceOwnerRecordLock() throws Exception
   {
      Integer first = launch();
      QContext.init(instance, newSession("B"));
      DurableJobsSecurity.applyToSession(QContext.getQSession());
      assertThat(new QueryAction().execute(new QueryInput(DurableJobsTables.TABLE_JOB)).getRecords()).isEmpty();
      Integer second = launch();
      assertThat(new QueryAction().execute(new QueryInput(DurableJobsTables.TABLE_JOB)).getRecords())
         .extracting(record -> record.getValueInteger("id")).containsExactly(second);
      QContext.getQSession().withPermission("jobs_admin");
      DurableJobsSecurity.applyToSession(QContext.getQSession());
      assertThat(new QueryAction().execute(new QueryInput(DurableJobsTables.TABLE_JOB)).getRecords())
         .extracting(record -> record.getValueInteger("id")).containsExactlyInAnyOrder(first, second);
   }



   /*******************************************************************************
    ** Owner authentication failures are terminal on the first attempt.
    *******************************************************************************/
   private void assertOwnerFailure(Integer jobId) throws Exception
   {
      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.FAILED);
      assertThat(chunk(jobId).getValueString("status")).isEqualTo("FAILED");
      assertThat(chunk(jobId).getValueInteger("attempt")).isEqualTo(1);
      assertThat(events(jobId)).doesNotContain("RETRIED");
      assertThat(DurableJobsTestSupport.runs("step1")).isZero();
   }
}

