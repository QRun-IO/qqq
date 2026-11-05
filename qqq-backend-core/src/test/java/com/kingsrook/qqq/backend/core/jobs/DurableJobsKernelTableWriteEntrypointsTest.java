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
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.ReplaceAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.RestoreWriteFlag;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Every standard native write entry point that a user session can reach - insert,
 ** update, delete, and replace (all three halves), with and without the restore
 ** flag and its permission, and the explicit table-permission check the HTTP
 ** executors make even when core enforcement is off - is refused for the kernel
 ** job tables. The kernel itself (a system scope) still writes. No generic bulk
 ** table process exists for these tables. ConditionalUpdateAction is internal
 ** (called only by kernel and health code, never by an executor or process) and
 ** performs no permission check on any table; it is not a user entry point.
 *******************************************************************************/
class DurableJobsKernelTableWriteEntrypointsTest extends DurableJobsTestBase
{
   /*******************************************************************************
    ** Replace (insert, update and delete halves) and the restore flag cannot reach
    ** the kernel tables, even for a session that holds backups.restore.
    *******************************************************************************/
   @Test
   void replaceAndRestoreFlagCannotBypass() throws Exception
   {
      Integer mine = launch();
      QContext.getQInstance().setEnforcePermissionsInCoreActions(true);
      DurableJobsSecurity.applyToSession(QContext.getQSession());
      QContext.getQSession().withPermission(RestoreWriteFlag.REQUIRED_PERMISSION);
      String me = QContext.getQSession().getUser().getIdReference();

      for(boolean flagged : new boolean[] { false, true })
      {
         ReplaceInput insertHalf = new ReplaceInput();
         insertHalf.setTableName(DurableJobsTables.TABLE_JOB);
         insertHalf.setKey(new UniqueKey("label"));
         insertHalf.setPerformDeletes(false);
         insertHalf.setRecords(List.of(new QRecord().withValue("label", "forged").withValue("processName", DurableJobsTestSupport.PROCESS_THREE_STEPS).withValue("userId", me).withValue("status", "QUEUED")));
         ReplaceInput updateHalf = new ReplaceInput();
         updateHalf.setTableName(DurableJobsTables.TABLE_JOB);
         updateHalf.setKey(new UniqueKey("id"));
         updateHalf.setPerformDeletes(false);
         updateHalf.setRecords(List.of(new QRecord().withValue("id", mine).withValue("processName", DurableJobsTestSupport.PROCESS_NOT_IDEMPOTENT)));
         ReplaceInput deleteHalf = new ReplaceInput();
         deleteHalf.setTableName(DurableJobsTables.TABLE_JOB);
         deleteHalf.setKey(new UniqueKey("label"));
         deleteHalf.setPerformDeletes(true);
         deleteHalf.setRecords(List.of());
         if(flagged)
         {
            insertHalf.withFlag(new RestoreWriteFlag());
            updateHalf.withFlag(new RestoreWriteFlag());
            deleteHalf.withFlag(new RestoreWriteFlag());
         }
         for(var entry : java.util.Map.of("insertHalf", insertHalf, "updateHalf", updateHalf, "deleteHalf", deleteHalf).entrySet())
         {
            String outcome;
            try
            {
               new ReplaceAction().execute(entry.getValue());
               outcome = "NO EXCEPTION";
            }
            catch(Throwable t)
            {
               outcome = t.getClass().getSimpleName();
            }
            assertThat(outcome).as("replace " + entry.getKey() + " flagged=" + flagged).isEqualTo("QPermissionDeniedException");

         }

         InsertInput insert = new InsertInput(DurableJobsTables.TABLE_JOB).withRecord(new QRecord().withValue("processName", DurableJobsTestSupport.PROCESS_THREE_STEPS).withValue("userId", me));
         UpdateInput update = new UpdateInput(DurableJobsTables.TABLE_JOB).withRecord(new QRecord().withValue("id", mine).withValue("status", "COMPLETED"));
         DeleteInput delete = new DeleteInput(DurableJobsTables.TABLE_JOB).withPrimaryKey(mine);
         if(flagged)
         {
            insert.withFlag(new RestoreWriteFlag());
            update.withFlag(new RestoreWriteFlag());
            delete.withFlag(new RestoreWriteFlag());
         }
         assertThatThrownBy(() -> new InsertAction().execute(insert)).as("insert flagged=" + flagged).isInstanceOf(QPermissionDeniedException.class);
         assertThatThrownBy(() -> new UpdateAction().execute(update)).as("update flagged=" + flagged).isInstanceOf(QPermissionDeniedException.class);
         assertThatThrownBy(() -> new DeleteAction().execute(delete)).as("delete flagged=" + flagged).isInstanceOf(QPermissionDeniedException.class);
      }

      assertThat(store.getJob(mine).getValueString("processName")).isEqualTo(DurableJobsTestSupport.PROCESS_THREE_STEPS);
      assertThat(store.getJob(mine).getValueString("status")).isEqualTo("QUEUED");
   }



   /*******************************************************************************
    ** The explicit table-permission check the HTTP executors make (independent of
    ** the core enforcement flag) refuses insert, edit and delete and allows read.
    ** Bulk table processes are not defined for these tables.
    *******************************************************************************/
   @Test
   void apiLayerCheckAndNoBulkProcesses() throws Exception
   {
      QContext.getQInstance().setEnforcePermissionsInCoreActions(false);
      for(String table : List.of(DurableJobsTables.TABLE_JOB, DurableJobsTables.TABLE_CHUNK, DurableJobsTables.TABLE_EFFECT, DurableJobsTables.TABLE_EVENT, DurableJobsTables.TABLE_ROW_ERROR, DurableJobsTables.TABLE_WORKER, DurableJobsTables.TABLE_UPLOAD))
      {
         for(TablePermissionSubType type : List.of(TablePermissionSubType.INSERT, TablePermissionSubType.EDIT, TablePermissionSubType.DELETE))
         {
            assertThatThrownBy(() -> PermissionsHelper.checkTablePermissionThrowing((type == TablePermissionSubType.INSERT ? new InsertInput(table) : type == TablePermissionSubType.EDIT ? new UpdateInput(table) : new DeleteInput(table)), type)).as(table + " " + type).isInstanceOf(QPermissionDeniedException.class);
         }
         PermissionsHelper.checkTablePermissionThrowing(new com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput(table), TablePermissionSubType.READ);
      }
   }



   /*******************************************************************************
    ** Positive control: the kernel (a system scope) still writes the same table with
    ** the same standard actions while enforcement is on.
    *******************************************************************************/
   @Test
   void kernelScopeStillWrites() throws Exception
   {
      Integer mine = launch();
      QContext.getQInstance().setEnforcePermissionsInCoreActions(true);
      SystemScope.call(() -> new UpdateAction().execute(new UpdateInput(DurableJobsTables.TABLE_JOB).withRecord(new QRecord().withValue("id", mine).withValue("statusReason", "kernel wrote"))));
      assertThat(store.getJob(mine).getValueString("statusReason")).isEqualTo("kernel wrote");
   }


   /*******************************************************************************
    ** The standard bulk table processes (bulkInsert, bulkEdit, bulkEditWithFile,
    ** bulkDelete) exist for these tables, so the permission gate in front of them
    ** must refuse a user too - with core enforcement off, as the HTTP path runs it.
    ** (The process gate used to hand the table checker an untyped input that a
    ** checker keyed on the write input type could not recognize.)
    *******************************************************************************/
   @Test
   void bulkTableProcessesAreRefused() throws Exception
   {
      QContext.getQInstance().setEnforcePermissionsInCoreActions(false);
      for(String table : List.of(DurableJobsTables.TABLE_JOB, DurableJobsTables.TABLE_CHUNK, DurableJobsTables.TABLE_EFFECT, DurableJobsTables.TABLE_EVENT, DurableJobsTables.TABLE_ROW_ERROR, DurableJobsTables.TABLE_WORKER, DurableJobsTables.TABLE_UPLOAD))
      {
         for(String action : List.of("bulkInsert", "bulkEdit", "bulkEditWithFile", "bulkDelete"))
         {
            String process = table + "." + action;
            if(QContext.getQInstance().getProcess(process) != null)
            {
               assertThatThrownBy(() -> PermissionsHelper.checkProcessPermissionThrowing(new com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput(), process)).as(process).isInstanceOf(QPermissionDeniedException.class);
            }
         }
      }
      assertThat(QContext.getQInstance().getProcess("jobs_job.bulkInsert")).as("the standard bulk processes are present, so the gate matters").isNotNull();
   }

}
