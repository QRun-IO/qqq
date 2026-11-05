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


import java.util.Base64;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** What a user-owned job runs as when its signed grant was not attached at launch
 ** (grants configured, but the launching session had nothing to bind to, or the
 ** attach failed): never the system user, never another user, and with the
 ** module's own automated-session authority only - the default one carries the
 ** user id and no permissions or keys, so a missing grant loses authority rather
 ** than gaining it. A token-style provider that refuses automated sessions makes
 ** such a job fail closed.
 *******************************************************************************/
class DurableJobsGrantFallbackBoundaryTest extends DurableJobsTestBase
{
   private static final String KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());



   /*******************************************************************************
    ** Launch with grants configured but nothing for the grant to bind to.
    *******************************************************************************/
   private Integer launchWithoutGrant() throws QException
   {
      settings.withJobGrantKey(KEY);
      QContext.getQSession().withPermission("launcher.only");
      String uuid = QContext.getQSession().getUuid();
      QContext.getQSession().setUuid("");
      try
      {
         return (launch());
      }
      finally
      {
         QContext.getQSession().setUuid(uuid);
      }
   }



   /*******************************************************************************
    ** The job has no grant and its worker session is the owner's automated session,
    ** not the system user and not the launcher's permissions.
    *******************************************************************************/
   @Test
   void missingGrantFallsBackToOwnerOnlyAutomatedSession() throws Exception
   {
      Integer jobId = launchWithoutGrant();
      assertThat(store.getJob(jobId).getValueString("credentialScopeJson")).doesNotContain(JobGrants.SCOPE_GRANT);

      QSession worker = JobSessions.forJob(instance, store.getJob(jobId));
      assertThat(worker).isNotInstanceOf(QSystemUserSession.class);
      assertThat(worker.getUser().getIdReference()).isEqualTo(store.getJob(jobId).getValueString("userId"));
      assertThat(worker.getPermissions() == null ? java.util.Set.of() : worker.getPermissions()).doesNotContain("launcher.only");
   }



   /*******************************************************************************
    ** A provider that refuses tokenless automated sessions fails the job closed.
    *******************************************************************************/
   @Test
   void missingGrantFailsClosedWhenProviderRefusesAutomatedSessions() throws Exception
   {
      Integer jobId = launchWithoutGrant();
      instance.getAuthentication().withModuleCodeReference(new QCodeReference(JobGrantsTest.SignInAuthentication.class));
      assertThatThrownBy(() -> JobSessions.forJob(instance, store.getJob(jobId))).isInstanceOf(QException.class);
   }
}
