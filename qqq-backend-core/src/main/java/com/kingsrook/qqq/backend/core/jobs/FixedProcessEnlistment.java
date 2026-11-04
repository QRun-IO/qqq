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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.TransactionalRecordChangeScope;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.jobs.model.JobEventType;
import com.kingsrook.qqq.backend.core.jobs.store.DurableJobStore;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.BoundedIntentData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** Required fixed-process intent on the native record transaction. Only declared
 ** JSON dispatch inputs are retained; raw source event data contributes to the
 ** fingerprint, and is never automatically copied into readable job parameters.
 *******************************************************************************/
final class FixedProcessEnlistment
{
   /*******************************************************************************
    ** Static Jobs-owned boundary.
    *******************************************************************************/
   private FixedProcessEnlistment() {}



   /*******************************************************************************
    ** Return the durable job identity, visible to workers only after source commit.
    ** Any refusal poisons the active source transaction, including caught errors.
    *******************************************************************************/
   static Integer enlist(RecordChangeEvent event, FixedProcessRequest request) throws QException
   {
      TransactionalRecordChangeScope.AdmittedEvent admitted = TransactionalRecordChangeScope.admittedEvent(event);
      try
      {
         DurableJobsMetaData settings = DurableJobs.settings();
         String namespace = settings.getTransactionalNamespace();
         if(request == null || namespace == null || !namespace.matches("[A-Za-z0-9._:-]{1,128}"))
         {
            throw (new QException("Transactional Jobs needs its host namespace and fixed request"));
         }
         QSession session = QContext.getQSession();
         String actor = DurableJobsSecurity.userId(session);
         if(session == null || session instanceof QSystemUserSession || actor == null || actor.isBlank() || JobSessions.isTokenSession(session)
            || com.kingsrook.qqq.backend.core.serviceidentity.ServiceIdentities.principalNameFromId(actor) != null
            || session.getUuid() == null || session.getUuid().isBlank())
         {
            throw (new QPermissionDeniedException("Transactional Jobs requires an interactive identity with a restorable signed grant"));
         }
         QProcessMetaData process = QContext.getQInstance().getProcess(request.getProcessName());
         String revision = requireProcess(process);
         PermissionsHelper.checkProcessPermissionThrowing(new RunProcessInput().withProcessName(process.getName()), process.getName(), request.getValues());
         Map<String, List<Serializable>> scope = JobSessions.captureScope(session);
         String authority = authority(session, actor, scope);
         String backend = JobAdmission.lock(admitted.transaction(), admitted.tableName());
         Map<String, Object> identity = new LinkedHashMap<>();
         identity.put("namespace", namespace);
         identity.put("backend", backend);
         identity.put("actor", actor);
         identity.put("table", admitted.tableName());
         identity.put("event", admitted.type().name());
         identity.put("process", process.getName());
         identity.put("operation", request.getOperationKey());
         String key = BoundedIntentData.fingerprint(identity);
         identity.put("scope", scope);
         identity.put("recordFingerprint", admitted.fingerprint());
         identity.put("revision", revision);
         identity.put("parameters", request.getValues());
         identity.put("channel", session.getClientChannel());
         String fingerprint = JobGrants.intentFingerprint(QContext.getQInstance(), BoundedIntentData.fingerprint(identity));
         DurableJobStore store = new DurableJobStore(settings, admitted.transaction());
         List<QRecord> prior = store.query(DurableJobsTables.TABLE_JOB,
            new QQueryFilter(new QFilterCriteria("enlistmentKey", QCriteriaOperator.EQUALS, key)).withLimit(2));
         if(!prior.isEmpty())
         {
            QRecord saved = prior.get(0);
            if(prior.size() != 1 || !fingerprint.equals(saved.getValueString("intentFingerprint"))
               || !actor.equals(saved.getValueString("userId")) || !revision.equals(saved.getValueString("processRevision")))
            {
               throw (new QException("Transactional operation key has a different persisted intent"));
            }
            requireCurrentAuthority(session, actor, authority, request, revision);
            TransactionalRecordChangeScope.admittedEvent(event);
            return (saved.getValueInteger("id"));
         }
         Instant now = store.now();
         boolean assistant = DurableJobs.isAssistant(session);
         QRecord job = new QRecord().withValue("label", Objects.requireNonNullElse(process.getLabel(), process.getName()))
            .withValue("processName", process.getName()).withValue("processRevision", revision).withValue("tableName", admitted.tableName())
            .withValue("enlistmentKey", key).withValue("intentFingerprint", fingerprint)
            .withValue("mode", "EXECUTE").withValue("status", assistant ? "AWAITING_CONFIRMATION" : "QUEUED")
            .withValue("priority", DurableJobsTables.DEFAULT_PRIORITY).withValue("userId", actor).withValue("clientChannel", session.getClientChannel())
            .withValue("sourceType", "STEPS").withValue("sourceJson", JsonUtils.toJson(Map.of("recordEvent", admitted.type().name(), "eventId", admitted.eventId())))
            .withValue("paramsJson", JsonUtils.toJson(request.getValues())).withValue("idempotency", "NONE").withValue("semantics", "ATOMIC")
            .withValue("chunkSize", 1).withValue("maxParallelChunks", 1).withValue("chunksTotal", 1)
            .withValue("rowsDone", 0).withValue("rowsOk", 0).withValue("rowsFailed", 0).withValue("chunksDone", 0).withValue("chunksFailed", 0)
            .withValue("notifyEmail", false).withValue("queuedAt", now).withValue("leaseVersion", 0).withValue("createDate", now).withValue("modifyDate", now);
         Integer id = store.insertJob(job, admitted.transaction());
         Map<String, List<Serializable>> signed = new LinkedHashMap<>(scope);
         signed.put(JobGrants.SCOPE_GRANT, List.of(JobGrants.issue(QContext.getQInstance(), id, actor, session.getUuid(), scope)));
         store.updateRequiredJob(id, Map.of("credentialScopeJson", JsonUtils.toJson(signed)));
         store.insertChunks(List.of(new QRecord().withValue("jobId", id).withValue("seq", 0).withValue("rangeJson", "{\"plan\":\"ATOMIC\"}")
            .withValue("status", "PENDING").withValue("attempt", 0).withValue("maxAttempts", 1).withValue("leaseVersion", 0)
            .withValue("createDate", now).withValue("modifyDate", now)), admitted.transaction());
         store.recordRequiredEvent(id, null, JobEventType.LAUNCHED, actor, session.getClientChannel(), null, "Enlisted with source transaction");
         requireCurrentAuthority(session, actor, authority, request, revision);
         TransactionalRecordChangeScope.admittedEvent(event);
         return (id);
      }
      catch(QException | RuntimeException | LinkageError error)
      {
         admitted.transaction().markRollbackOnly();
         throw (error);
      }
   }



   /*******************************************************************************
    ** Detect changed initiating authority during trusted native bookkeeping hooks.
    *******************************************************************************/
   private static void requireCurrentAuthority(QSession session, String actor, String expected, FixedProcessRequest request, String revision) throws QException
   {
      if(session != QContext.getQSession() || !actor.equals(DurableJobsSecurity.userId(session)) || JobSessions.isTokenSession(session)
         || !expected.equals(authority(session, actor, JobSessions.captureScope(session))))
      {
         throw (new QPermissionDeniedException("Initiating Jobs authority changed during enlistment"));
      }
      QProcessMetaData process = QContext.getQInstance().getProcess(request.getProcessName());
      if(!revision.equals(requireProcess(process)))
      {
         throw (new QException("Fixed process revision changed during enlistment"));
      }
      PermissionsHelper.checkProcessPermissionThrowing(new RunProcessInput().withProcessName(process.getName()), process.getName(), request.getValues());
   }



   /*******************************************************************************
    ** Hash current identity/channel/scope without retaining raw credentials.
    *******************************************************************************/
   private static String authority(QSession session, String actor, Map<String, List<Serializable>> scope) throws QException
   {
      Map<String, Object> values = new LinkedHashMap<>();
      values.put("actor", actor);
      values.put("session", session.getUuid());
      values.put("channel", session.getClientChannel());
      values.put("scope", scope);
      return (BoundedIntentData.fingerprint(values));
   }



   /*******************************************************************************
    ** Explicitly supported fixed host process shape, checked again by the worker.
    *******************************************************************************/
   static String requireProcess(QProcessMetaData process) throws QException
   {
      DurableProcessMetaData metadata = DurableProcessMetaData.of(process);
      if(process == null || !DurableJobs.isEligible(process) || metadata == null || metadata.getTransactionalRevision() == null
         || !metadata.getTransactionalRevision().matches("[A-Za-z0-9._:-]{1,128}") || !"LINEAR".equals(process.getStepFlow().name())
         || process.getStepList() == null || process.getStepList().isEmpty() || process.getStepList().size() > 16
         || process.getStepList().stream().anyMatch(step -> !(step instanceof QBackendStepMetaData backend)
            || backend.getCode() == null || backend.getCode().getCodeType() != QCodeType.JAVA))
      {
         throw (new QException("Transactional Jobs requires an opted-in fixed Java backend process revision"));
      }
      return (metadata.getTransactionalRevision());
   }
}
