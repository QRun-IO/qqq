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


import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.tables.ConditionalUpdateAction;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
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
   private Set<String> eligibleProcessNames = new HashSet<>();
   private String podId;



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
    ** Startup validation (spec section 4).
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      for(String tableName : List.of(DurableJobsTables.TABLE_JOB, DurableJobsTables.TABLE_CHUNK, DurableJobsTables.TABLE_ROW_ERROR, DurableJobsTables.TABLE_EVENT, DurableJobsTables.TABLE_WORKER))
      {
         validator.assertCondition(qInstance.getTable(tableName) != null, "Durable jobs are enabled but table " + tableName + " is not defined (use DurableJobsMetaDataProvider.defineAll).");
      }

      if(qInstance.getTable(DurableJobsTables.TABLE_JOB) != null)
      {
         QBackendMetaData backend = qInstance.getBackendForTable(DurableJobsTables.TABLE_JOB);
         validator.assertCondition(backend != null && ConditionalUpdateAction.isSupported(backend), "Durable jobs require a backend with CONDITIONAL_UPDATE for the jobs tables.");
      }

      if(storageTableName != null)
      {
         validator.assertCondition(qInstance.getTable(storageTableName) != null, "Durable jobs storage table " + storageTableName + " is not defined.");
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

}
