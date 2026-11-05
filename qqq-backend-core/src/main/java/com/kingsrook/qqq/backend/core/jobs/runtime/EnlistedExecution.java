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


package com.kingsrook.qqq.backend.core.jobs.runtime;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobs;
import com.kingsrook.qqq.backend.core.jobs.JobGrants;
import com.kingsrook.qqq.backend.core.jobs.JobSessions;
import com.kingsrook.qqq.backend.core.jobs.model.JobLease;
import com.kingsrook.qqq.backend.core.jobs.plans.ChunkContext;
import com.kingsrook.qqq.backend.core.jobs.plans.JobChunkScope;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.BoundedIntentData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** Native-runtime-only evidence of an authenticated enlisted chunk. A generic
 ** JobChunkScope or a directly launched process cannot create this authority.
 *******************************************************************************/
public final class EnlistedExecution implements AutoCloseable
{
   private static final ThreadLocal<EnlistedExecution> CURRENT = new ThreadLocal<>();
   private final QInstance instance;
   private final QSession session;
   private final ChunkContext context;
   private final DurableJobStore store;
   private final String identity;
   private final String authority;
   private final EnlistedExecution previous;



   /*******************************************************************************
    ** Immutable dispatch data only; source field read authority is not implied.
    *******************************************************************************/
   public record Verified(Integer jobId, Integer chunkId, String actorId, String processName,
      String processRevision, String sourceTable, String eventType, String eventId, Map<String, Serializable> parameters)
   {
   }



   /*******************************************************************************
    ** Called only by the native worker after successful JobSessions restoration.
    *******************************************************************************/
   static EnlistedExecution open(ChunkContext context, DurableJobStore store) throws QException
   {
      EnlistedExecution execution = new EnlistedExecution(context, store);
      CURRENT.set(execution);
      return (execution);
   }



   /*******************************************************************************
    ** Capture exact runtime identity and immutable persisted-intent identity.
    *******************************************************************************/
   private EnlistedExecution(ChunkContext context, DurableJobStore store) throws QException
   {
      this.instance = QContext.getQInstance();
      this.session = QContext.getQSession();
      this.context = context;
      this.store = store;
      this.identity = intentIdentity(context.getJob());
      this.authority = authority(session);
      this.previous = CURRENT.get();
   }



   /*******************************************************************************
    ** Recheck current actor, process, signed grant and database lease before
    ** returning bounded persisted parameters. Never trust process input values.
    *******************************************************************************/
   public static Verified require(String processName, String revision) throws QException
   {
      EnlistedExecution execution = CURRENT.get();
      if(execution == null)
      {
         throw (denied());
      }
      return (execution.verify(processName, revision));
   }



   /*******************************************************************************
    ** No cached job status or supplied chunk identity substitutes for live checks.
    *******************************************************************************/
   private Verified verify(String processName, String revision) throws QException
   {
      JobChunkScope scope = JobChunkScope.current().orElseThrow(EnlistedExecution::denied);
      if(instance != QContext.getQInstance() || session != QContext.getQSession() || session instanceof QSystemUserSession
         || session == null || session.getUser() == null || JobSessions.isTokenSession(session)
         || !authority.equals(authority(session)) || !Objects.equals(scope.getIdentity(), context.identity()))
      {
         throw (denied());
      }
      context.check();
      JobLease lease = context.getLease();
      QRecord job = store.getJob(lease.jobId());
      QRecord chunk = store.getChunk(lease.chunkId());
      if(job == null || chunk == null || job.getValueString("enlistmentKey") == null
         || !identity.equals(intentIdentity(job)) || !Objects.equals(processName, job.getValueString("processName"))
         || !Objects.equals(revision, job.getValueString("processRevision")) || !"RUNNING".equals(job.getValueString("status"))
         || !Objects.equals(session.getUser().getIdReference(), job.getValueString("userId"))
         || !"RUNNING".equals(chunk.getValueString("status")) || !Objects.equals(lease.jobId(), chunk.getValueInteger("jobId"))
         || !Objects.equals(lease.seq(), chunk.getValueInteger("seq")) || !Objects.equals(lease.attempt(), chunk.getValueInteger("attempt"))
         || !Objects.equals(lease.token(), chunk.getValueString("leaseToken")) || !Objects.equals(lease.podId(), chunk.getValueString("leaseOwner"))
         || chunk.getValueInstant("leaseExpiresAt") == null || !chunk.getValueInstant("leaseExpiresAt").isAfter(store.now()))
      {
         throw (denied());
      }
      Map<String, List<Serializable>> launchScope = readJson(job.getValueString("credentialScopeJson"), new TypeReference<>() {});
      List<Serializable> grants = launchScope == null ? null : launchScope.get(JobGrants.SCOPE_GRANT);
      if(grants == null || grants.size() != 1 || !(grants.get(0) instanceof String grant))
      {
         throw (denied());
      }
      JobGrants.verify(instance, lease.jobId(), job.getValueString("userId"), grant, launchScope);
      QProcessMetaData process = instance.getProcess(processName);
      DurableJobs.requireEnlistedProcessRevision(job, process);
      Map<String, Serializable> parameters = BoundedIntentData.copyParameters(readJson(job.getValueString("paramsJson"), new TypeReference<Map<String, Serializable>>() {}));
      Map<String, Serializable> source = BoundedIntentData.copyParameters(readJson(job.getValueString("sourceJson"), new TypeReference<Map<String, Serializable>>() {}));
      Object eventType = source.get("recordEvent");
      Object eventId = source.get("eventId");
      if(!(eventType instanceof String event) || !List.of("INSERT", "UPDATE", "DELETE").contains(event)
         || !(eventId instanceof String id) || !id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"))
      {
         throw (denied());
      }
      PermissionsHelper.checkProcessPermissionThrowing(new RunProcessInput().withProcessName(processName), processName, parameters);
      if(instance != QContext.getQInstance() || session != QContext.getQSession() || !authority.equals(authority(session)))
      {
         throw (denied());
      }
      return (new Verified(lease.jobId(), lease.chunkId(), job.getValueString("userId"), processName, revision,
         job.getValueString("tableName"), event, id, parameters));
   }



   /*******************************************************************************
    ** Private transient digest; no record values or grant strings are logged.
    *******************************************************************************/
   private static String intentIdentity(QRecord job) throws QException
   {
      Map<String, Serializable> values = new java.util.LinkedHashMap<>();
      for(String field : List.of("id", "userId", "processName", "processRevision", "tableName", "enlistmentKey",
         "intentFingerprint", "sourceJson", "paramsJson", "credentialScopeJson", "clientChannel"))
      {
         if(List.of("sourceJson", "paramsJson", "credentialScopeJson").contains(field))
         {
            values.put(field, (Serializable) BoundedIntentData.copyParameters(readJson(job.getValueString(field), new TypeReference<Map<String, Serializable>>() {})));
         }
         else
         {
            values.put(field, job.getValue(field));
         }
      }
      return (BoundedIntentData.fingerprint(values));
   }



   /*******************************************************************************
    ** Prevent same-object mutation from broadening the restored worker authority.
    *******************************************************************************/
   private static String authority(QSession session) throws QException
   {
      Map<String, Object> values = new java.util.LinkedHashMap<>();
      values.put("actor", session == null || session.getUser() == null ? null : session.getUser().getIdReference());
      values.put("session", session == null ? null : session.getUuid());
      values.put("channel", session == null ? null : session.getClientChannel());
      values.put("scope", JobSessions.captureScope(session));
      values.put("permissions", session == null || session.getPermissions() == null ? null : session.getPermissions().stream().sorted().toList());
      return (BoundedIntentData.fingerprint(values));
   }



   /*******************************************************************************
    ** Bound serialized data before parsing; invalid owner rows expose no payload.
    *******************************************************************************/
   private static <T> T readJson(String value, TypeReference<T> type) throws QException
   {
      if(value == null || value.length() > 8388608)
      {
         throw (denied());
      }
      try
      {
         return (JsonUtils.toObject(value, type));
      }
      catch(java.io.IOException | RuntimeException error)
      {
         throw (denied());
      }
   }



   /*******************************************************************************
    ** Uniform refusal does not expose job existence or stored intent data.
    *******************************************************************************/
   private static QPermissionDeniedException denied()
   {
      return (new QPermissionDeniedException("An active authenticated enlisted execution is required"));
   }



   /*******************************************************************************
    ** Restore outer runtime scope even when the process refuses or throws.
    *******************************************************************************/
   @Override
   public void close()
   {
      if(previous == null)
      {
         CURRENT.remove();
      }
      else
      {
         CURRENT.set(previous);
      }
   }
}
