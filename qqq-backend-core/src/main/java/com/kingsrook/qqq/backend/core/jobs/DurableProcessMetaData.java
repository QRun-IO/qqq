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


import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.jobs.model.JobIdempotency;
import com.kingsrook.qqq.backend.core.jobs.model.JobSemantics;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QSupplementalProcessMetaData;


/*******************************************************************************
 ** Per-process durable-job settings (supplemental process meta data "durable").
 ** Optional: with none, the kernel decides eligibility by the kind of process
 ** (core bulk processes and partitionable streamed ETL are eligible), and an app
 ** can make any process eligible by name in DurableJobsMetaData.
 *******************************************************************************/
public class DurableProcessMetaData extends QSupplementalProcessMetaData
{
   public static final String TYPE = "durable";

   private Boolean eligible;
   private Boolean alwaysDurable = false;
   private Integer durableThresholdRows;
   private Integer chunkSize;
   private JobIdempotency idempotency;
   private JobSemantics semantics;
   private Integer maxParallelChunks = 1;
   private Boolean chunkParallelSafe = false;
   private Integer maxConcurrentJobs;



   /*******************************************************************************
    ** The durable settings on a process, or null.
    *******************************************************************************/
   public static DurableProcessMetaData of(QProcessMetaData process)
   {
      if(process == null || process.getSupplementalMetaData() == null)
      {
         return (null);
      }
      return ((DurableProcessMetaData) process.getSupplementalMetaData().get(TYPE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getType()
   {
      return (TYPE);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, QProcessMetaData process, QInstanceValidator validator)
   {
      validator.assertCondition(maxParallelChunks == null || maxParallelChunks >= 1, "Process " + process.getName() + " durable maxParallelChunks must be at least 1.");
      validator.assertCondition(maxParallelChunks == null || maxParallelChunks == 1 || Boolean.TRUE.equals(chunkParallelSafe), "Process " + process.getName() + " durable maxParallelChunks over 1 requires chunkParallelSafe.");
      validator.assertCondition(chunkSize == null || chunkSize >= 1, "Process " + process.getName() + " durable chunkSize must be at least 1.");
   }



   /*******************************************************************************
    ** Getter for eligible: whether the process may run as a durable job (null means the kernel default for its kind).
    *******************************************************************************/
   public Boolean getEligible()
   {
      return (this.eligible);
   }



   /*******************************************************************************
    ** Fluent setter for eligible
    *******************************************************************************/
   public DurableProcessMetaData withEligible(Boolean eligible)
   {
      this.eligible = eligible;
      return (this);
   }


   /*******************************************************************************
    ** Getter for alwaysDurable: the process is too large to run synchronously; it always runs as a job.
    *******************************************************************************/
   public Boolean getAlwaysDurable()
   {
      return (this.alwaysDurable);
   }



   /*******************************************************************************
    ** Fluent setter for alwaysDurable
    *******************************************************************************/
   public DurableProcessMetaData withAlwaysDurable(Boolean alwaysDurable)
   {
      this.alwaysDurable = alwaysDurable;
      return (this);
   }


   /*******************************************************************************
    ** Getter for durableThresholdRows: rows above which it runs as a job (null means the instance default).
    *******************************************************************************/
   public Integer getDurableThresholdRows()
   {
      return (this.durableThresholdRows);
   }



   /*******************************************************************************
    ** Fluent setter for durableThresholdRows
    *******************************************************************************/
   public DurableProcessMetaData withDurableThresholdRows(Integer durableThresholdRows)
   {
      this.durableThresholdRows = durableThresholdRows;
      return (this);
   }


   /*******************************************************************************
    ** Getter for chunkSize: rows per chunk (null means the instance default).
    *******************************************************************************/
   public Integer getChunkSize()
   {
      return (this.chunkSize);
   }



   /*******************************************************************************
    ** Fluent setter for chunkSize
    *******************************************************************************/
   public DurableProcessMetaData withChunkSize(Integer chunkSize)
   {
      this.chunkSize = chunkSize;
      return (this);
   }


   /*******************************************************************************
    ** Getter for idempotency: how safe re-running a chunk is (null means chosen by the kernel).
    *******************************************************************************/
   public JobIdempotency getIdempotency()
   {
      return (this.idempotency);
   }



   /*******************************************************************************
    ** Fluent setter for idempotency
    *******************************************************************************/
   public DurableProcessMetaData withIdempotency(JobIdempotency idempotency)
   {
      this.idempotency = idempotency;
      return (this);
   }


   /*******************************************************************************
    ** Getter for semantics: CHUNKED or ATOMIC (null means CHUNKED).
    *******************************************************************************/
   public JobSemantics getSemantics()
   {
      return (this.semantics);
   }



   /*******************************************************************************
    ** Fluent setter for semantics
    *******************************************************************************/
   public DurableProcessMetaData withSemantics(JobSemantics semantics)
   {
      this.semantics = semantics;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxParallelChunks: chunks of one job that may run at once (more needs chunkParallelSafe).
    *******************************************************************************/
   public Integer getMaxParallelChunks()
   {
      return (this.maxParallelChunks);
   }



   /*******************************************************************************
    ** Fluent setter for maxParallelChunks
    *******************************************************************************/
   public DurableProcessMetaData withMaxParallelChunks(Integer maxParallelChunks)
   {
      this.maxParallelChunks = maxParallelChunks;
      return (this);
   }


   /*******************************************************************************
    ** Getter for chunkParallelSafe: chunks of one job may run in parallel.
    *******************************************************************************/
   public Boolean getChunkParallelSafe()
   {
      return (this.chunkParallelSafe);
   }



   /*******************************************************************************
    ** Fluent setter for chunkParallelSafe
    *******************************************************************************/
   public DurableProcessMetaData withChunkParallelSafe(Boolean chunkParallelSafe)
   {
      this.chunkParallelSafe = chunkParallelSafe;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxConcurrentJobs: running jobs of this process at once (null means the instance default).
    *******************************************************************************/
   public Integer getMaxConcurrentJobs()
   {
      return (this.maxConcurrentJobs);
   }



   /*******************************************************************************
    ** Fluent setter for maxConcurrentJobs
    *******************************************************************************/
   public DurableProcessMetaData withMaxConcurrentJobs(Integer maxConcurrentJobs)
   {
      this.maxConcurrentJobs = maxConcurrentJobs;
      return (this);
   }

}
