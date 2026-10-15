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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QAuthenticationException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.jobs.model.JobStatus;
import com.kingsrook.qqq.backend.core.jobs.runtime.DurableJobsRuntime;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.implementations.MockAuthenticationModule;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Job grants: a worker runs a user's job as that user, from the launching
 ** sign-in, with current permissions - and fails cleanly, never escalates.
 *******************************************************************************/
class JobGrantsTest extends DurableJobsTestBase
{
   private static final String KEY = Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes());

   /** the "provider": signed-in sessions (UUID to user) */
   static final Map<String, String> SIGNED_IN = new ConcurrentHashMap<>();

   /** the "provider": each user's current permissions */
   static final Map<String, Set<String>> PERMISSIONS = new ConcurrentHashMap<>();

   /** the "provider": each user's current security key values */
   static final Map<String, Map<String, List<Serializable>>> KEYS = new ConcurrentHashMap<>();



   /*******************************************************************************
    ** An auth module that rebuilds sessions only from a live sign-in (like
    ** OAuth2 with stored tokens) and refuses tokenless automated sessions.
    *******************************************************************************/
   public static class SignInAuthentication extends MockAuthenticationModule
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QSession createSession(QInstance qInstance, Map<String, String> context) throws QAuthenticationException
      {
         String uuid   = context.get("sessionUUID");
         String userId = uuid == null ? null : SIGNED_IN.get(uuid);
         if(userId == null)
         {
            throw (new QAuthenticationException("Session has ended"));
         }
         QSession session = new QSession();
         session.setUuid(uuid);
         session.setUser(new QUser().withIdReference(userId).withFullName("User " + userId));
         session.setPermissions(new HashSet<>(PERMISSIONS.getOrDefault(userId, Set.of())));
         KEYS.getOrDefault(userId, Map.of()).forEach((key, values) -> values.forEach(value -> session.withSecurityKeyValue(key, value)));
         return (session);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QSession createAutomatedSessionForUser(QInstance qInstance, Serializable userId) throws QAuthenticationException
      {
         throw (new QAuthenticationException("No tokenless sessions"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void enableGrants()
   {
      settings.withJobGrantKey(KEY);
      instance.getAuthentication().withModuleCodeReference(new QCodeReference(SignInAuthentication.class));
      String userId = QContext.getQSession().getUser().getIdReference();
      SIGNED_IN.put(QContext.getQSession().getUuid(), userId);
      PERMISSIONS.put(userId, QContext.getQSession().getPermissions() == null ? new HashSet<>() : new HashSet<>(QContext.getQSession().getPermissions()));
      KEYS.put(userId, new LinkedHashMap<>());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void clearProvider()
   {
      SIGNED_IN.clear();
      PERMISSIONS.clear();
      KEYS.clear();
   }



   /*******************************************************************************
    ** The job's stored credential scope.
    *******************************************************************************/
   private Map<String, List<Serializable>> scopeOf(Integer jobId) throws Exception
   {
      return (JsonUtils.toObject(store.getJob(jobId).getValueString("credentialScopeJson"), new TypeReference<>() {}));
   }



   /*******************************************************************************
    ** Overwrite the job's stored credential scope.
    *******************************************************************************/
   private void setScope(Integer jobId, Map<String, List<Serializable>> scope) throws QException
   {
      store.updateJob(jobId, Map.of("credentialScopeJson", JsonUtils.toJson(scope)));
   }



   /*******************************************************************************
    ** A granted job runs to completion on a worker, as its owner, on channel job.
    *******************************************************************************/
   @Test
   void grantedJobRunsAsItsOwner() throws Exception
   {
      Integer jobId = launch();
      assertThat(scopeOf(jobId)).containsKey(JobGrants.SCOPE_GRANT);
      assertThat(store.getJob(jobId).getValueString("credentialScopeJson")).doesNotContain(QContext.getQSession().getUuid());

      QSession session = JobSessions.forJob(instance, store.getJob(jobId));
      assertThat(session.getUser().getIdReference()).isEqualTo(QContext.getQSession().getUser().getIdReference());
      assertThat(session.getClientChannel()).startsWith(JobSessions.CHANNEL_JOB);

      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.COMPLETED);
   }



   /*******************************************************************************
    ** A changed grant (or one copied from another job) fails the job.
    *******************************************************************************/
   @Test
   void tamperedGrantFails() throws Exception
   {
      Integer jobId = launch();
      Map<String, List<Serializable>> scope = scopeOf(jobId);
      String grant = String.valueOf(scope.get(JobGrants.SCOPE_GRANT).get(0));
      scope.put(JobGrants.SCOPE_GRANT, new ArrayList<>(List.of(grant.substring(0, grant.length() - 2) + "xx")));
      setScope(jobId, scope);
      assertThatThrownBy(() -> JobSessions.forJob(instance, store.getJob(jobId))).isInstanceOf(QPermissionDeniedException.class);

      ///////////////////////////////////////////////////
      // another job's (valid) grant does not fit      //
      ///////////////////////////////////////////////////
      Integer other = launch();
      Map<String, List<Serializable>> otherScope = scopeOf(other);
      otherScope.put(JobGrants.SCOPE_GRANT, new ArrayList<>(List.of(grant)));
      setScope(other, otherScope);
      assertThatThrownBy(() -> JobSessions.forJob(instance, store.getJob(other))).isInstanceOf(QPermissionDeniedException.class);

      DurableJobsRuntime.start(instance);
      await(jobId, JobStatus.FAILED);
   }



   /*******************************************************************************
    ** Widening the stored launch scope breaks the grant.
    *******************************************************************************/
   @Test
   void widenedScopeFails() throws Exception
   {
      Integer jobId = launch();
      Map<String, List<Serializable>> scope = scopeOf(jobId);
      scope.put("tenant", new ArrayList<>(List.of("everyone")));
      setScope(jobId, scope);
      assertThatThrownBy(() -> JobSessions.forJob(instance, store.getJob(jobId))).isInstanceOf(QPermissionDeniedException.class);
   }



   /*******************************************************************************
    ** An expired grant fails.
    *******************************************************************************/
   @Test
   void expiredGrantFails() throws Exception
   {
      settings.withJobGrantTtlHours(-1);
      Integer jobId = launch();
      assertThatThrownBy(() -> JobSessions.forJob(instance, store.getJob(jobId)))
         .isInstanceOf(QPermissionDeniedException.class).hasMessageContaining("expired");
   }



   /*******************************************************************************
    ** A user who signed out (or whose refresh token was revoked) fails the job
    ** cleanly; there is no fallback to a tokenless session.
    *******************************************************************************/
   @Test
   void signedOutUserFailsCleanly() throws Exception
   {
      Integer jobId = launch();
      SIGNED_IN.clear();
      assertThatThrownBy(() -> JobSessions.forJob(instance, store.getJob(jobId)))
         .isInstanceOf(QPermissionDeniedException.class).hasMessageContaining("no longer signed in");

      DurableJobsRuntime.start(instance);
      QRecord failed = await(jobId, JobStatus.FAILED);
      assertThat(failed.getValueString("status")).isEqualTo("FAILED");
   }



   /*******************************************************************************
    ** A permission removed after launch is gone on the next rebuild.
    *******************************************************************************/
   @Test
   void permissionRemovedMidJobApplies() throws Exception
   {
      String userId = QContext.getQSession().getUser().getIdReference();
      PERMISSIONS.get(userId).add("reports.run");
      Integer jobId = launch();
      assertThat(JobSessions.forJob(instance, store.getJob(jobId)).hasPermission("reports.run")).isTrue();

      PERMISSIONS.get(userId).remove("reports.run");
      assertThat(JobSessions.forJob(instance, store.getJob(jobId)).hasPermission("reports.run")).isFalse();
   }



   /*******************************************************************************
    ** A launch scope narrower than the user's current keys limits the job.
    *******************************************************************************/
   @Test
   void launchScopeNarrowerThanUserLimitsTheJob() throws Exception
   {
      String userId = QContext.getQSession().getUser().getIdReference();
      QContext.getQSession().withSecurityKeyValue("tenant", 1);
      Integer jobId = launch();

      ///////////////////////////////////////////////////////////////
      // the user has since been given tenant 2 and a new key     //
      ///////////////////////////////////////////////////////////////
      KEYS.get(userId).put("tenant", List.of(1, 2));
      KEYS.get(userId).put("region", List.of("west"));

      QSession session = JobSessions.forJob(instance, store.getJob(jobId));
      assertThat(session.getSecurityKeyValues("tenant")).containsExactly(1);
      assertThat(session.getSecurityKeyValues()).doesNotContainKey("region");
   }



   /*******************************************************************************
    ** Grants are bound to job, user and scope; a direct verify with the wrong
    ** job id fails, and the right one yields the session.
    *******************************************************************************/
   @Test
   void grantBindsJobUserAndScope() throws Exception
   {
      Map<String, List<Serializable>> scope = new LinkedHashMap<>(Map.of("tenant", List.of(1)));
      String grant = JobGrants.issue(instance, 7, "u1", "session-7", scope);
      assertThat(JobGrants.verify(instance, 7, "u1", grant, Map.of("tenant", List.of("1")))).isEqualTo("session-7");
      assertThatThrownBy(() -> JobGrants.verify(instance, 8, "u1", grant, scope)).isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> JobGrants.verify(instance, 7, "u2", grant, scope)).isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> JobGrants.verify(instance, 7, "u1", grant, Map.of())).isInstanceOf(QPermissionDeniedException.class);
      assertThat(Instant.now()).isNotNull();
   }
}
