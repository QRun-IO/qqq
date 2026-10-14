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


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.progress.JobLogPermissions;
import com.kingsrook.qqq.backend.core.jobs.runtime.InlineJobTracker;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobClaimer;
import com.kingsrook.qqq.backend.core.jobs.runtime.JobFinalizer;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Inline jobs are never chunk work; a dead foreground pod cannot be resumed.
 *******************************************************************************/
class InlineJobTrackerTest extends DurableJobsTestBase
{
   /*******************************************************************************
    ** The missing-pod transition and final line happen once.
    *******************************************************************************/
   @Test
   void deadPodFailsInlineJobOnce() throws Exception
   {
      Integer id = store.insertJob(new QRecord().withValue("sourceType", "INLINE").withValue("status", "RUNNING")
         .withValue("leaseOwner", "gone").withValue("processName", DurableJobsTestSupport.PROCESS_THREE_STEPS)
         .withValue("userId", DurableJobsSecurity.userId(QContext.getQSession())).withValue("queuedAt", store.now()), null);
      assertThat(new JobClaimer(store, "live").claimNext()).isEmpty();
      assertThat(new JobFinalizer(store).evaluate(id)).isEqualTo(JobStatus.RUNNING);
      InlineJobTracker tracker = new InlineJobTracker(store, "live");
      tracker.reap();
      tracker.reap();
      assertThat(store.getJob(id).getValueString("status")).isEqualTo("FAILED");
      assertThat(store.getJob(id).getValueString("statusReason")).isEqualTo(InlineJobTracker.RESTART_MESSAGE);
      assertThat(store.query(DurableJobsTables.TABLE_LOG, new QQueryFilter())).hasSize(1);
   }



   /*******************************************************************************
    ** Job logs reject writes from owners and administrators.
    *******************************************************************************/
   @Test
   void logMutationPermissions() throws Exception
   {
      QContext.getQSession().withPermissions("jobs_admin", "jobs_job.read", "jobs_log.insert", "jobs_log.edit", "jobs_log.delete");
      JobLogPermissions permissions = new JobLogPermissions();
      var table = instance.getTable(DurableJobsTables.TABLE_LOG);
      permissions.checkPermissionsThrowing(new QueryInput(DurableJobsTables.TABLE_LOG), table);
      assertThatThrownBy(() -> permissions.checkPermissionsThrowing(new InsertInput(DurableJobsTables.TABLE_LOG), table)).isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> permissions.checkPermissionsThrowing(new UpdateInput(DurableJobsTables.TABLE_LOG), table)).isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> permissions.checkPermissionsThrowing(new DeleteInput(DurableJobsTables.TABLE_LOG), table)).isInstanceOf(QPermissionDeniedException.class);
   }
}
