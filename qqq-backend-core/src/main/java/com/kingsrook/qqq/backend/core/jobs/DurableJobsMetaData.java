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


import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.ConditionalUpdateAction;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.jobs.progress.JobLogRedactor;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** Instance-level settings for durable jobs (QInstance supplemental meta data
 ** named "durableJobs"). Its presence turns durable jobs on: startup validation
 ** then requires the jobs tables, a backend with CONDITIONAL_UPDATE, and every
 ** alwaysDurable process to be eligible.
 *******************************************************************************/
public class DurableJobsMetaData implements QSupplementalInstanceMetaData
{
   public static final String NAME = "durableJobs";

   private Integer logMaxLines = 2000;
   private JobLogRedactor logRedactor;
   private String storageTableName;
   private Integer workerThreads = 2;
   private Integer leaseTtlSeconds = 60;
   private Integer reaperIntervalSeconds = 15;
   private Integer pollIntervalMillis = 2000;
   private Integer durableThresholdRows = 1000;
   private Integer defaultChunkSize = 500;
   private Integer defaultMaxConcurrentJobsPerProcess = 2;
   private Integer maxActiveJobsPerUser = 5;
   private Integer maxQueuedJobsPerUser = 50;
   private Integer stallMinutes = 15;
   private Integer queueStallMinutes = 60;
   private Integer assistantAutoStartMaxRows = 0;
   private Integer assistantMaxActiveJobs = 2;
   private Boolean scheduledRunsAsJobs = true;
   private String scheduledJobsUserId;
   private List<QCodeReference> completionEffectProviders = new ArrayList<>();
   private Set<String> eligibleProcessNames = new HashSet<>();
   private String podId;
   private String uploadsStorageTableName;
   private Integer uploadExpiryDays = 7;
   private Long maxUploadBytes = 1073741824L;
   private Map<String, QCodeReference> effectTargets = new LinkedHashMap<>();
   private Integer effectMaxAttempts = 5;
   private Integer effectReconcileAttempts = 3;
   private Integer effectLeaseTtlSeconds = 60;
   private Integer effectRetryBaseSeconds = 5;
   private Integer effectBatchSize = 20;
   private Integer effectDispatcherThreads = 1;
   private Boolean useSkipLockedClaims = true;



   /*******************************************************************************
    ** The settings on an instance, or null when durable jobs are off.
    *******************************************************************************/
   public static DurableJobsMetaData of(QInstance qInstance)
   {
      return (qInstance == null ? null : QSupplementalInstanceMetaData.of(qInstance, NAME));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Fluent adder for one eligible process name.
    *******************************************************************************/
   public DurableJobsMetaData withEligibleProcessName(String processName)
   {
      this.eligibleProcessNames.add(processName);
      return (this);
   }



   /*******************************************************************************
    ** The storage table uploads go to.
    *******************************************************************************/
   public String getEffectiveUploadsStorageTableName()
   {
      return (uploadsStorageTableName != null ? uploadsStorageTableName : storageTableName);
   }



   /*******************************************************************************
    ** Startup validation (spec section 4).
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      for(String tableName : List.of(DurableJobsTables.TABLE_JOB, DurableJobsTables.TABLE_CHUNK, DurableJobsTables.TABLE_ROW_ERROR, DurableJobsTables.TABLE_EVENT, DurableJobsTables.TABLE_WORKER, DurableJobsTables.TABLE_UPLOAD, DurableJobsTables.TABLE_EFFECT, DurableJobsTables.TABLE_LOG))
      {
         validator.assertCondition(qInstance.getTable(tableName) != null, "Durable jobs are enabled but table " + tableName + " is not defined (use DurableJobsMetaDataProvider.defineAll).");
      }

      if(qInstance.getTable(DurableJobsTables.TABLE_JOB) != null)
      {
         QBackendMetaData backend = qInstance.getBackendForTable(DurableJobsTables.TABLE_JOB);
         validator.assertCondition(backend != null && ConditionalUpdateAction.isSupported(backend), "Durable jobs require a backend with CONDITIONAL_UPDATE for the jobs tables.");
      }

      for(Map.Entry<String, QCodeReference> target : effectTargets.entrySet())
      {
         validator.assertCondition(target.getValue() != null, "Durable jobs effect target " + target.getKey() + " has no code reference.");
      }
      validator.assertCondition(logMaxLines != null && logMaxLines >= 1, "Durable jobs logMaxLines must be at least 1.");
      validator.assertCondition(effectMaxAttempts != null && effectMaxAttempts >= 1, "Durable jobs effectMaxAttempts must be at least 1.");
      validator.assertCondition(effectLeaseTtlSeconds != null && effectLeaseTtlSeconds >= 1, "Durable jobs effectLeaseTtlSeconds must be at least 1.");

      if(storageTableName != null)
      {
         validator.assertCondition(qInstance.getTable(storageTableName) != null, "Durable jobs storage table " + storageTableName + " is not defined.");
      }
      if(uploadsStorageTableName != null)
      {
         validator.assertCondition(qInstance.getTable(uploadsStorageTableName) != null, "Durable jobs uploads storage table " + uploadsStorageTableName + " is not defined.");
      }

      for(String processName : eligibleProcessNames)
      {
         validator.assertCondition(qInstance.getProcess(processName) != null, "Durable jobs eligible process " + processName + " is not defined.");
      }

      for(QProcessMetaData process : qInstance.getProcesses().values())
      {
         DurableProcessMetaData durable = DurableProcessMetaData.of(process);
         if(durable != null && Boolean.TRUE.equals(durable.getAlwaysDurable()))
         {
            validator.assertCondition(!Boolean.FALSE.equals(durable.getEligible()), "Process " + process.getName() + " is alwaysDurable but not eligible for durable jobs.");
         }
      }

      validator.assertCondition(workerThreads == null || workerThreads >= 0, "Durable jobs workerThreads may not be negative.");
      validator.assertCondition(leaseTtlSeconds != null && leaseTtlSeconds >= 3, "Durable jobs leaseTtlSeconds must be at least 3.");
   }



   /*******************************************************************************
    ** Getter for storageTableName: the storage table (should be object storage) for chunk inputs, error CSVs and outputs.
    *******************************************************************************/
   public String getStorageTableName()
   {
      return (this.storageTableName);
   }



   /*******************************************************************************
    ** Fluent setter for storageTableName
    *******************************************************************************/
   public DurableJobsMetaData withStorageTableName(String storageTableName)
   {
      this.storageTableName = storageTableName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for workerThreads: chunks this pod runs at once; 0 means this pod runs no jobs.
    *******************************************************************************/
   public Integer getWorkerThreads()
   {
      return (this.workerThreads);
   }



   /*******************************************************************************
    ** Fluent setter for workerThreads
    *******************************************************************************/
   public DurableJobsMetaData withWorkerThreads(Integer workerThreads)
   {
      this.workerThreads = workerThreads;
      return (this);
   }


   /*******************************************************************************
    ** Getter for leaseTtlSeconds: how long a chunk lease lasts without a heartbeat.
    *******************************************************************************/
   public Integer getLeaseTtlSeconds()
   {
      return (this.leaseTtlSeconds);
   }



   /*******************************************************************************
    ** Fluent setter for leaseTtlSeconds
    *******************************************************************************/
   public DurableJobsMetaData withLeaseTtlSeconds(Integer leaseTtlSeconds)
   {
      this.leaseTtlSeconds = leaseTtlSeconds;
      return (this);
   }


   /*******************************************************************************
    ** Getter for reaperIntervalSeconds: how often each pod looks for expired leases.
    *******************************************************************************/
   public Integer getReaperIntervalSeconds()
   {
      return (this.reaperIntervalSeconds);
   }



   /*******************************************************************************
    ** Fluent setter for reaperIntervalSeconds
    *******************************************************************************/
   public DurableJobsMetaData withReaperIntervalSeconds(Integer reaperIntervalSeconds)
   {
      this.reaperIntervalSeconds = reaperIntervalSeconds;
      return (this);
   }


   /*******************************************************************************
    ** Getter for pollIntervalMillis: how often an idle worker looks for work (jittered).
    *******************************************************************************/
   public Integer getPollIntervalMillis()
   {
      return (this.pollIntervalMillis);
   }



   /*******************************************************************************
    ** Fluent setter for pollIntervalMillis
    *******************************************************************************/
   public DurableJobsMetaData withPollIntervalMillis(Integer pollIntervalMillis)
   {
      this.pollIntervalMillis = pollIntervalMillis;
      return (this);
   }


   /*******************************************************************************
    ** Getter for durableThresholdRows: row count above which an eligible process runs as a job.
    *******************************************************************************/
   public Integer getDurableThresholdRows()
   {
      return (this.durableThresholdRows);
   }



   /*******************************************************************************
    ** Fluent setter for durableThresholdRows
    *******************************************************************************/
   public DurableJobsMetaData withDurableThresholdRows(Integer durableThresholdRows)
   {
      this.durableThresholdRows = durableThresholdRows;
      return (this);
   }


   /*******************************************************************************
    ** Getter for defaultChunkSize: rows per chunk unless the process says otherwise.
    *******************************************************************************/
   public Integer getDefaultChunkSize()
   {
      return (this.defaultChunkSize);
   }



   /*******************************************************************************
    ** Fluent setter for defaultChunkSize
    *******************************************************************************/
   public DurableJobsMetaData withDefaultChunkSize(Integer defaultChunkSize)
   {
      this.defaultChunkSize = defaultChunkSize;
      return (this);
   }


   /*******************************************************************************
    ** Getter for defaultMaxConcurrentJobsPerProcess: running jobs per process.
    *******************************************************************************/
   public Integer getDefaultMaxConcurrentJobsPerProcess()
   {
      return (this.defaultMaxConcurrentJobsPerProcess);
   }



   /*******************************************************************************
    ** Fluent setter for defaultMaxConcurrentJobsPerProcess
    *******************************************************************************/
   public DurableJobsMetaData withDefaultMaxConcurrentJobsPerProcess(Integer defaultMaxConcurrentJobsPerProcess)
   {
      this.defaultMaxConcurrentJobsPerProcess = defaultMaxConcurrentJobsPerProcess;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxActiveJobsPerUser: running jobs per user; more stay QUEUED.
    *******************************************************************************/
   public Integer getMaxActiveJobsPerUser()
   {
      return (this.maxActiveJobsPerUser);
   }



   /*******************************************************************************
    ** Fluent setter for maxActiveJobsPerUser
    *******************************************************************************/
   public DurableJobsMetaData withMaxActiveJobsPerUser(Integer maxActiveJobsPerUser)
   {
      this.maxActiveJobsPerUser = maxActiveJobsPerUser;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxQueuedJobsPerUser: queued jobs per user before a launch is refused (429).
    *******************************************************************************/
   public Integer getMaxQueuedJobsPerUser()
   {
      return (this.maxQueuedJobsPerUser);
   }



   /*******************************************************************************
    ** Fluent setter for maxQueuedJobsPerUser
    *******************************************************************************/
   public DurableJobsMetaData withMaxQueuedJobsPerUser(Integer maxQueuedJobsPerUser)
   {
      this.maxQueuedJobsPerUser = maxQueuedJobsPerUser;
      return (this);
   }


   /*******************************************************************************
    ** Getter for stallMinutes: RUNNING with no progress this long raises jobs.job-stalled.
    *******************************************************************************/
   public Integer getStallMinutes()
   {
      return (this.stallMinutes);
   }



   /*******************************************************************************
    ** Fluent setter for stallMinutes
    *******************************************************************************/
   public DurableJobsMetaData withStallMinutes(Integer stallMinutes)
   {
      this.stallMinutes = stallMinutes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for queueStallMinutes: QUEUED this long raises jobs.job-stalled.
    *******************************************************************************/
   public Integer getQueueStallMinutes()
   {
      return (this.queueStallMinutes);
   }



   /*******************************************************************************
    ** Fluent setter for queueStallMinutes
    *******************************************************************************/
   public DurableJobsMetaData withQueueStallMinutes(Integer queueStallMinutes)
   {
      this.queueStallMinutes = queueStallMinutes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for assistantAutoStartMaxRows: assistant jobs at or under this many rows start without confirmation; 0 means never.
    *******************************************************************************/
   public Integer getAssistantAutoStartMaxRows()
   {
      return (this.assistantAutoStartMaxRows);
   }



   /*******************************************************************************
    ** Fluent setter for assistantAutoStartMaxRows
    *******************************************************************************/
   public DurableJobsMetaData withAssistantAutoStartMaxRows(Integer assistantAutoStartMaxRows)
   {
      this.assistantAutoStartMaxRows = assistantAutoStartMaxRows;
      return (this);
   }


   /*******************************************************************************
    ** Getter for assistantMaxActiveJobs: active jobs the assistant channel may hold per user.
    *******************************************************************************/
   public Integer getAssistantMaxActiveJobs()
   {
      return (this.assistantMaxActiveJobs);
   }



   /*******************************************************************************
    ** Fluent setter for assistantMaxActiveJobs
    *******************************************************************************/
   public DurableJobsMetaData withAssistantMaxActiveJobs(Integer assistantMaxActiveJobs)
   {
      this.assistantMaxActiveJobs = assistantMaxActiveJobs;
      return (this);
   }


   /*******************************************************************************
    ** Getter for scheduledRunsAsJobs: eligible scheduled processes run as jobs.
    *******************************************************************************/
   public Boolean getScheduledRunsAsJobs()
   {
      return (this.scheduledRunsAsJobs);
   }



   /*******************************************************************************
    ** Fluent setter for scheduledRunsAsJobs
    *******************************************************************************/
   public DurableJobsMetaData withScheduledRunsAsJobs(Boolean scheduledRunsAsJobs)
   {
      this.scheduledRunsAsJobs = scheduledRunsAsJobs;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scheduledJobsUserId: the user that owns jobs launched by the
    ** scheduler when the scheduler runs as the system user. The app's
    ** authentication module must be able to create an automated session for it.
    ** Without it, such scheduled runs stay inline (as before durable jobs).
    *******************************************************************************/
   public String getScheduledJobsUserId()
   {
      return (this.scheduledJobsUserId);
   }



   /*******************************************************************************
    ** Fluent setter for scheduledJobsUserId
    *******************************************************************************/
   public DurableJobsMetaData withScheduledJobsUserId(String scheduledJobsUserId)
   {
      this.scheduledJobsUserId = scheduledJobsUserId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for eligibleProcessNames: processes made eligible by name, with no code.
    *******************************************************************************/
   public Set<String> getEligibleProcessNames()
   {
      return (this.eligibleProcessNames);
   }



   /*******************************************************************************
    ** Fluent setter for eligibleProcessNames
    *******************************************************************************/
   public DurableJobsMetaData withEligibleProcessNames(Set<String> eligibleProcessNames)
   {
      this.eligibleProcessNames = eligibleProcessNames;
      return (this);
   }


   /*******************************************************************************
    ** Getter for podId: this pod's id (defaults to the host name plus a random suffix).
    *******************************************************************************/
   public String getPodId()
   {
      return (this.podId);
   }



   /*******************************************************************************
    ** Fluent setter for podId
    *******************************************************************************/
   public DurableJobsMetaData withPodId(String podId)
   {
      this.podId = podId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for uploadsStorageTableName: the storage table POST /uploads streams files into (defaults to storageTableName).
    *******************************************************************************/
   public String getUploadsStorageTableName()
   {
      return (this.uploadsStorageTableName);
   }



   /*******************************************************************************
    ** Fluent setter for uploadsStorageTableName
    *******************************************************************************/
   public DurableJobsMetaData withUploadsStorageTableName(String uploadsStorageTableName)
   {
      this.uploadsStorageTableName = uploadsStorageTableName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for uploadExpiryDays: days an upload ref stays usable (a job that uses it extends it).
    *******************************************************************************/
   public Integer getUploadExpiryDays()
   {
      return (this.uploadExpiryDays);
   }



   /*******************************************************************************
    ** Fluent setter for uploadExpiryDays
    *******************************************************************************/
   public DurableJobsMetaData withUploadExpiryDays(Integer uploadExpiryDays)
   {
      this.uploadExpiryDays = uploadExpiryDays;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxUploadBytes: the largest file POST /uploads accepts.
    *******************************************************************************/
   public Long getMaxUploadBytes()
   {
      return (this.maxUploadBytes);
   }



   /*******************************************************************************
    ** Fluent setter for maxUploadBytes
    *******************************************************************************/
   public DurableJobsMetaData withMaxUploadBytes(Long maxUploadBytes)
   {
      this.maxUploadBytes = maxUploadBytes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for effectTargets: side-effect targets by name (EffectTarget code references); the outbox delivers to these
    *******************************************************************************/
   public Map<String, QCodeReference> getEffectTargets()
   {
      return (this.effectTargets);
   }



   /*******************************************************************************
    ** Fluent setter for effectTargets
    *******************************************************************************/
   public DurableJobsMetaData withEffectTargets(Map<String, QCodeReference> effectTargets)
   {
      this.effectTargets = effectTargets;
      return (this);
   }


   /*******************************************************************************
    ** Getter for effectMaxAttempts: delivery attempts before an effect is FAILED
    *******************************************************************************/
   public Integer getEffectMaxAttempts()
   {
      return (this.effectMaxAttempts);
   }



   /*******************************************************************************
    ** Fluent setter for effectMaxAttempts
    *******************************************************************************/
   public DurableJobsMetaData withEffectMaxAttempts(Integer effectMaxAttempts)
   {
      this.effectMaxAttempts = effectMaxAttempts;
      return (this);
   }


   /*******************************************************************************
    ** Getter for effectReconcileAttempts: reconcile tries for an UNKNOWN effect before it is queued for review
    *******************************************************************************/
   public Integer getEffectReconcileAttempts()
   {
      return (this.effectReconcileAttempts);
   }



   /*******************************************************************************
    ** Fluent setter for effectReconcileAttempts
    *******************************************************************************/
   public DurableJobsMetaData withEffectReconcileAttempts(Integer effectReconcileAttempts)
   {
      this.effectReconcileAttempts = effectReconcileAttempts;
      return (this);
   }


   /*******************************************************************************
    ** Getter for effectLeaseTtlSeconds: how long a dispatcher holds an effect while delivering it; an expired lease means the outcome is unknown
    *******************************************************************************/
   public Integer getEffectLeaseTtlSeconds()
   {
      return (this.effectLeaseTtlSeconds);
   }



   /*******************************************************************************
    ** Fluent setter for effectLeaseTtlSeconds
    *******************************************************************************/
   public DurableJobsMetaData withEffectLeaseTtlSeconds(Integer effectLeaseTtlSeconds)
   {
      this.effectLeaseTtlSeconds = effectLeaseTtlSeconds;
      return (this);
   }


   /*******************************************************************************
    ** Getter for effectRetryBaseSeconds: first retry delay for an effect; doubles per attempt (capped at 1 hour)
    *******************************************************************************/
   public Integer getEffectRetryBaseSeconds()
   {
      return (this.effectRetryBaseSeconds);
   }



   /*******************************************************************************
    ** Fluent setter for effectRetryBaseSeconds
    *******************************************************************************/
   public DurableJobsMetaData withEffectRetryBaseSeconds(Integer effectRetryBaseSeconds)
   {
      this.effectRetryBaseSeconds = effectRetryBaseSeconds;
      return (this);
   }


   /*******************************************************************************
    ** Getter for effectBatchSize: effects one dispatcher pass claims
    *******************************************************************************/
   public Integer getEffectBatchSize()
   {
      return (this.effectBatchSize);
   }



   /*******************************************************************************
    ** Fluent setter for effectBatchSize
    *******************************************************************************/
   public DurableJobsMetaData withEffectBatchSize(Integer effectBatchSize)
   {
      this.effectBatchSize = effectBatchSize;
      return (this);
   }


   /*******************************************************************************
    ** Getter for effectDispatcherThreads: dispatcher threads per pod (0 = this pod delivers no effects)
    *******************************************************************************/
   public Integer getEffectDispatcherThreads()
   {
      return (this.effectDispatcherThreads);
   }



   /*******************************************************************************
    ** Fluent setter for effectDispatcherThreads
    *******************************************************************************/
   public DurableJobsMetaData withEffectDispatcherThreads(Integer effectDispatcherThreads)
   {
      this.effectDispatcherThreads = effectDispatcherThreads;
      return (this);
   }


   /*******************************************************************************
    ** Getter for useSkipLockedClaims: claim chunks and effects with SELECT ... FOR UPDATE SKIP LOCKED where the backend supports it (else conditional updates)
    *******************************************************************************/
   public Boolean getUseSkipLockedClaims()
   {
      return (this.useSkipLockedClaims);
   }



   /*******************************************************************************
    ** Fluent setter for useSkipLockedClaims
    *******************************************************************************/
   public DurableJobsMetaData withUseSkipLockedClaims(Boolean useSkipLockedClaims)
   {
      this.useSkipLockedClaims = useSkipLockedClaims;
      return (this);
   }



   /*******************************************************************************
    ** Register a side-effect target (an EffectTarget implementation) by name.
    *******************************************************************************/
   public DurableJobsMetaData withEffectTarget(String name, QCodeReference codeReference)
   {
      this.effectTargets.put(name, codeReference);
      return (this);
   }


   /*******************************************************************************
    ** Getter for completionEffectProviders: JobCompletionEffects providers asked for job-level effects (e.g., output delivery) when a job is about to complete
    *******************************************************************************/
   public List<QCodeReference> getCompletionEffectProviders()
   {
      return (this.completionEffectProviders);
   }



   /*******************************************************************************
    ** Fluent setter for completionEffectProviders
    *******************************************************************************/
   public DurableJobsMetaData withCompletionEffectProviders(List<QCodeReference> completionEffectProviders)
   {
      this.completionEffectProviders = completionEffectProviders;
      return (this);
   }



   /*******************************************************************************
    ** Register a JobCompletionEffects provider (job-level effects, such as
    ** delivering a job's outputs, written when a job is about to complete).
    *******************************************************************************/
   public DurableJobsMetaData withCompletionEffectProvider(QCodeReference codeReference)
   {
      this.completionEffectProviders.add(codeReference);
      return (this);
   }



   /*******************************************************************************
    ** Getter for logMaxLines
    *******************************************************************************/
   public Integer getLogMaxLines()
   {
      return (this.logMaxLines);
   }



   /*******************************************************************************
    ** Setter for logMaxLines
    *******************************************************************************/
   public void setLogMaxLines(Integer logMaxLines)
   {
      this.logMaxLines = logMaxLines;
   }



   /*******************************************************************************
    ** Fluent setter for logMaxLines
    *******************************************************************************/
   public DurableJobsMetaData withLogMaxLines(Integer logMaxLines)
   {
      this.logMaxLines = logMaxLines;
      return (this);
   }



   /*******************************************************************************
    ** Getter for logRedactor
    *******************************************************************************/
   public JobLogRedactor getLogRedactor()
   {
      return (this.logRedactor);
   }



   /*******************************************************************************
    ** Setter for logRedactor
    *******************************************************************************/
   public void setLogRedactor(JobLogRedactor logRedactor)
   {
      this.logRedactor = logRedactor;
   }



   /*******************************************************************************
    ** Fluent setter for logRedactor
    *******************************************************************************/
   public DurableJobsMetaData withLogRedactor(JobLogRedactor logRedactor)
   {
      this.logRedactor = logRedactor;
      return (this);
   }

}
