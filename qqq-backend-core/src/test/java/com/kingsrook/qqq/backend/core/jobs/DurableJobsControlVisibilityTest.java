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
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.jobs.model.JobAction;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** A job the caller cannot read (the jobs table hides other owners' rows) is not
 ** found by control either, exactly as status() reports it - control must not tell
 ** a hidden job from an absent one. A visible job the caller may not do an action
 ** to (the assistant) stays a permission refusal.
 *******************************************************************************/
class DurableJobsControlVisibilityTest extends DurableJobsTestBase
{
   /*******************************************************************************
    ** Another owner's job answers like an absent one, and is left untouched.
    *******************************************************************************/
   @Test
   void foreignJobControlIsNotFoundLikeAbsent() throws Exception
   {
      Integer jobId = launch();
      QContext.init(instance, newSession("B"));
      DurableJobsSecurity.applyToSession(QContext.getQSession());

      Throwable absent = null;
      try
      {
         DurableJobs.control(987654, JobAction.CANCEL);
      }
      catch(QNotFoundException e)
      {
         absent = e;
      }
      assertThat(absent).isNotNull();

      assertThatThrownBy(() -> DurableJobs.control(jobId, JobAction.CANCEL)).isInstanceOf(QNotFoundException.class).hasMessage(absent.getMessage().replace("987654", String.valueOf(jobId)));
      assertThatThrownBy(() -> DurableJobs.retryFailedRows(jobId, null, null)).isInstanceOf(QNotFoundException.class);
      assertThatThrownBy(() -> DurableJobs.downloadFailedRows(jobId)).isInstanceOf(QNotFoundException.class);
      assertThatThrownBy(() -> DurableJobs.status(jobId)).isInstanceOf(QNotFoundException.class);
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("QUEUED");
   }



   /*******************************************************************************
    ** Positive controls: the owner and an admin can control; the assistant is refused
    ** with a permission error on a job it can see.
    *******************************************************************************/
   @Test
   void visibleJobsKeepTheirOutcomes() throws Exception
   {
      Integer jobId = launch();
      QContext.init(instance, newSession("ADMIN").withPermission(DurableJobsTables.PERMISSION_ADMIN));
      DurableJobsSecurity.applyToSession(QContext.getQSession());
      assertThat(DurableJobs.control(jobId, JobAction.PAUSE)).contains("");
      assertThat(store.getJob(jobId).getValueString("status")).isEqualTo("PAUSED");

      QContext.init(instance, newSession());
      DurableJobsSecurity.applyToSession(QContext.getQSession());
      QContext.getQSession().setClientChannel("assistant/turn-1");
      assertThatThrownBy(() -> DurableJobs.control(jobId, JobAction.RESTART)).isInstanceOf(QPermissionDeniedException.class);
   }
}
