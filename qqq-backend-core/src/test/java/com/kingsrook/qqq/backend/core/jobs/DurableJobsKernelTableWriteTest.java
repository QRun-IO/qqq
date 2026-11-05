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
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.jobs.model.JobAction;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** The kernel job tables are written by the jobs kernel only. An ordinary user
 ** (who is held to their own rows by the owner record lock, and for whom field
 ** editability is only a UI hint) must not be able to create or rewrite a job
 ** through the generic table actions - that would skip launch and control checks.
 ** Reads, launch and control (which write as the kernel) are unchanged.
 *******************************************************************************/
class DurableJobsKernelTableWriteTest extends DurableJobsTestBase
{
   private static final List<String> KERNEL_TABLES = List.of(DurableJobsTables.TABLE_JOB, DurableJobsTables.TABLE_CHUNK, DurableJobsTables.TABLE_ROW_ERROR, DurableJobsTables.TABLE_EVENT,
      DurableJobsTables.TABLE_WORKER, DurableJobsTables.TABLE_UPLOAD, DurableJobsTables.TABLE_EFFECT, DurableJobsTables.TABLE_LOG);



   /*******************************************************************************
    ** A user cannot insert, update or delete job records, in any kernel table.
    *******************************************************************************/
   @Test
   void userCannotWriteKernelTables() throws Exception
   {
      Integer mine = launch();
      QContext.getQInstance().setEnforcePermissionsInCoreActions(true);
      DurableJobsSecurity.applyToSession(QContext.getQSession());
      String me = QContext.getQSession().getUser().getIdReference();

      QRecord forged = new QRecord().withValue("processName", DurableJobsTestSupport.PROCESS_THREE_STEPS).withValue("userId", me).withValue("status", "QUEUED");
      assertThatThrownBy(() -> new InsertAction().execute(new InsertInput(DurableJobsTables.TABLE_JOB).withRecord(forged))).isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new UpdateAction().execute(new UpdateInput(DurableJobsTables.TABLE_JOB).withRecord(new QRecord().withValue("id", mine).withValue("processName", DurableJobsTestSupport.PROCESS_NOT_IDEMPOTENT)))).isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new DeleteAction().execute(new DeleteInput(DurableJobsTables.TABLE_JOB).withPrimaryKey(mine))).isInstanceOf(QPermissionDeniedException.class);

      for(String table : KERNEL_TABLES)
      {
         assertThatThrownBy(() -> new InsertAction().execute(new InsertInput(table).withRecord(new QRecord()))).as(table).isInstanceOf(QPermissionDeniedException.class);
      }

      assertThat(store.getJob(mine).getValueString("processName")).isEqualTo(DurableJobsTestSupport.PROCESS_THREE_STEPS);
      assertThat(store.getJob(mine).getValueString("status")).isEqualTo("QUEUED");
      assertThat(new QueryAction().execute(new QueryInput(DurableJobsTables.TABLE_JOB)).getRecords()).hasSize(1);
   }



   /*******************************************************************************
    ** Positive controls: with enforcement on, an ordinary user can still read their
    ** own jobs and launch and control them; the kernel writes as itself.
    *******************************************************************************/
   @Test
   void userStillReadsLaunchesAndControlsTheirJobs() throws Exception
   {
      QContext.getQInstance().setEnforcePermissionsInCoreActions(true);
      DurableJobsSecurity.applyToSession(QContext.getQSession());
      Integer jobId = launch();
      assertThat(new QueryAction().execute(new QueryInput(DurableJobsTables.TABLE_JOB)).getRecords()).extracting(record -> record.getValueInteger("id")).contains(jobId);
      DurableJobs.control(jobId, JobAction.PAUSE);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("PAUSED");
      DurableJobs.control(jobId, JobAction.CANCEL);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("CANCELLED");
   }
}
