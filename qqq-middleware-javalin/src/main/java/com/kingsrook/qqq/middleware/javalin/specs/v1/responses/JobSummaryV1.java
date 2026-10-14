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


package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;


/*******************************************************************************
 ** Jobs UI API v1 representation.
 *******************************************************************************/
public class JobSummaryV1 implements ToSchema
{
   private Integer jobId;
   private String label;
   private String processName;
   private String status;
   private String sourceType;
   private String userId;
   private String currentPhase;
   private Double percent;
   private String message;
   private Long bytesDone;
   private Long bytesTotal;
   private Long etaSeconds;
   private Integer rowsDone;
   private Integer rowsTotal;
   private Integer rowsFailed;
   private Integer chunksDone;
   private Integer chunksTotal;
   private String queuedAt;
   private String startedAt;
   private String finishedAt;
   private Long logSeq;



   /*******************************************************************************
    ** Getter for jobId
    *******************************************************************************/
   public Integer getJobId()
   {
      return (this.jobId);
   }



   /*******************************************************************************
    ** Setter for jobId
    *******************************************************************************/
   public void setJobId(Integer jobId)
   {
      this.jobId = jobId;
   }



   /*******************************************************************************
    ** Fluent setter for jobId
    *******************************************************************************/
   public JobSummaryV1 withJobId(Integer jobId)
   {
      this.jobId = jobId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (this.label);
   }



   /*******************************************************************************
    ** Setter for label
    *******************************************************************************/
   public void setLabel(String label)
   {
      this.label = label;
   }



   /*******************************************************************************
    ** Fluent setter for label
    *******************************************************************************/
   public JobSummaryV1 withLabel(String label)
   {
      this.label = label;
      return (this);
   }



   /*******************************************************************************
    ** Getter for processName
    *******************************************************************************/
   public String getProcessName()
   {
      return (this.processName);
   }



   /*******************************************************************************
    ** Setter for processName
    *******************************************************************************/
   public void setProcessName(String processName)
   {
      this.processName = processName;
   }



   /*******************************************************************************
    ** Fluent setter for processName
    *******************************************************************************/
   public JobSummaryV1 withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for status
    *******************************************************************************/
   public String getStatus()
   {
      return (this.status);
   }



   /*******************************************************************************
    ** Setter for status
    *******************************************************************************/
   public void setStatus(String status)
   {
      this.status = status;
   }



   /*******************************************************************************
    ** Fluent setter for status
    *******************************************************************************/
   public JobSummaryV1 withStatus(String status)
   {
      this.status = status;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sourceType
    *******************************************************************************/
   public String getSourceType()
   {
      return (this.sourceType);
   }



   /*******************************************************************************
    ** Setter for sourceType
    *******************************************************************************/
   public void setSourceType(String sourceType)
   {
      this.sourceType = sourceType;
   }



   /*******************************************************************************
    ** Fluent setter for sourceType
    *******************************************************************************/
   public JobSummaryV1 withSourceType(String sourceType)
   {
      this.sourceType = sourceType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for userId
    *******************************************************************************/
   public String getUserId()
   {
      return (this.userId);
   }



   /*******************************************************************************
    ** Setter for userId
    *******************************************************************************/
   public void setUserId(String userId)
   {
      this.userId = userId;
   }



   /*******************************************************************************
    ** Fluent setter for userId
    *******************************************************************************/
   public JobSummaryV1 withUserId(String userId)
   {
      this.userId = userId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for currentPhase
    *******************************************************************************/
   public String getCurrentPhase()
   {
      return (this.currentPhase);
   }



   /*******************************************************************************
    ** Setter for currentPhase
    *******************************************************************************/
   public void setCurrentPhase(String currentPhase)
   {
      this.currentPhase = currentPhase;
   }



   /*******************************************************************************
    ** Fluent setter for currentPhase
    *******************************************************************************/
   public JobSummaryV1 withCurrentPhase(String currentPhase)
   {
      this.currentPhase = currentPhase;
      return (this);
   }



   /*******************************************************************************
    ** Getter for percent
    *******************************************************************************/
   public Double getPercent()
   {
      return (this.percent);
   }



   /*******************************************************************************
    ** Setter for percent
    *******************************************************************************/
   public void setPercent(Double percent)
   {
      this.percent = percent;
   }



   /*******************************************************************************
    ** Fluent setter for percent
    *******************************************************************************/
   public JobSummaryV1 withPercent(Double percent)
   {
      this.percent = percent;
      return (this);
   }



   /*******************************************************************************
    ** Getter for message
    *******************************************************************************/
   public String getMessage()
   {
      return (this.message);
   }



   /*******************************************************************************
    ** Setter for message
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    ** Fluent setter for message
    *******************************************************************************/
   public JobSummaryV1 withMessage(String message)
   {
      this.message = message;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bytesDone
    *******************************************************************************/
   public Long getBytesDone()
   {
      return (this.bytesDone);
   }



   /*******************************************************************************
    ** Setter for bytesDone
    *******************************************************************************/
   public void setBytesDone(Long bytesDone)
   {
      this.bytesDone = bytesDone;
   }



   /*******************************************************************************
    ** Fluent setter for bytesDone
    *******************************************************************************/
   public JobSummaryV1 withBytesDone(Long bytesDone)
   {
      this.bytesDone = bytesDone;
      return (this);
   }



   /*******************************************************************************
    ** Getter for bytesTotal
    *******************************************************************************/
   public Long getBytesTotal()
   {
      return (this.bytesTotal);
   }



   /*******************************************************************************
    ** Setter for bytesTotal
    *******************************************************************************/
   public void setBytesTotal(Long bytesTotal)
   {
      this.bytesTotal = bytesTotal;
   }



   /*******************************************************************************
    ** Fluent setter for bytesTotal
    *******************************************************************************/
   public JobSummaryV1 withBytesTotal(Long bytesTotal)
   {
      this.bytesTotal = bytesTotal;
      return (this);
   }



   /*******************************************************************************
    ** Getter for etaSeconds
    *******************************************************************************/
   public Long getEtaSeconds()
   {
      return (this.etaSeconds);
   }



   /*******************************************************************************
    ** Setter for etaSeconds
    *******************************************************************************/
   public void setEtaSeconds(Long etaSeconds)
   {
      this.etaSeconds = etaSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for etaSeconds
    *******************************************************************************/
   public JobSummaryV1 withEtaSeconds(Long etaSeconds)
   {
      this.etaSeconds = etaSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rowsDone
    *******************************************************************************/
   public Integer getRowsDone()
   {
      return (this.rowsDone);
   }



   /*******************************************************************************
    ** Setter for rowsDone
    *******************************************************************************/
   public void setRowsDone(Integer rowsDone)
   {
      this.rowsDone = rowsDone;
   }



   /*******************************************************************************
    ** Fluent setter for rowsDone
    *******************************************************************************/
   public JobSummaryV1 withRowsDone(Integer rowsDone)
   {
      this.rowsDone = rowsDone;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rowsTotal
    *******************************************************************************/
   public Integer getRowsTotal()
   {
      return (this.rowsTotal);
   }



   /*******************************************************************************
    ** Setter for rowsTotal
    *******************************************************************************/
   public void setRowsTotal(Integer rowsTotal)
   {
      this.rowsTotal = rowsTotal;
   }



   /*******************************************************************************
    ** Fluent setter for rowsTotal
    *******************************************************************************/
   public JobSummaryV1 withRowsTotal(Integer rowsTotal)
   {
      this.rowsTotal = rowsTotal;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rowsFailed
    *******************************************************************************/
   public Integer getRowsFailed()
   {
      return (this.rowsFailed);
   }



   /*******************************************************************************
    ** Setter for rowsFailed
    *******************************************************************************/
   public void setRowsFailed(Integer rowsFailed)
   {
      this.rowsFailed = rowsFailed;
   }



   /*******************************************************************************
    ** Fluent setter for rowsFailed
    *******************************************************************************/
   public JobSummaryV1 withRowsFailed(Integer rowsFailed)
   {
      this.rowsFailed = rowsFailed;
      return (this);
   }



   /*******************************************************************************
    ** Getter for chunksDone
    *******************************************************************************/
   public Integer getChunksDone()
   {
      return (this.chunksDone);
   }



   /*******************************************************************************
    ** Setter for chunksDone
    *******************************************************************************/
   public void setChunksDone(Integer chunksDone)
   {
      this.chunksDone = chunksDone;
   }



   /*******************************************************************************
    ** Fluent setter for chunksDone
    *******************************************************************************/
   public JobSummaryV1 withChunksDone(Integer chunksDone)
   {
      this.chunksDone = chunksDone;
      return (this);
   }



   /*******************************************************************************
    ** Getter for chunksTotal
    *******************************************************************************/
   public Integer getChunksTotal()
   {
      return (this.chunksTotal);
   }



   /*******************************************************************************
    ** Setter for chunksTotal
    *******************************************************************************/
   public void setChunksTotal(Integer chunksTotal)
   {
      this.chunksTotal = chunksTotal;
   }



   /*******************************************************************************
    ** Fluent setter for chunksTotal
    *******************************************************************************/
   public JobSummaryV1 withChunksTotal(Integer chunksTotal)
   {
      this.chunksTotal = chunksTotal;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queuedAt
    *******************************************************************************/
   public String getQueuedAt()
   {
      return (this.queuedAt);
   }



   /*******************************************************************************
    ** Setter for queuedAt
    *******************************************************************************/
   public void setQueuedAt(String queuedAt)
   {
      this.queuedAt = queuedAt;
   }



   /*******************************************************************************
    ** Fluent setter for queuedAt
    *******************************************************************************/
   public JobSummaryV1 withQueuedAt(String queuedAt)
   {
      this.queuedAt = queuedAt;
      return (this);
   }



   /*******************************************************************************
    ** Getter for startedAt
    *******************************************************************************/
   public String getStartedAt()
   {
      return (this.startedAt);
   }



   /*******************************************************************************
    ** Setter for startedAt
    *******************************************************************************/
   public void setStartedAt(String startedAt)
   {
      this.startedAt = startedAt;
   }



   /*******************************************************************************
    ** Fluent setter for startedAt
    *******************************************************************************/
   public JobSummaryV1 withStartedAt(String startedAt)
   {
      this.startedAt = startedAt;
      return (this);
   }



   /*******************************************************************************
    ** Getter for finishedAt
    *******************************************************************************/
   public String getFinishedAt()
   {
      return (this.finishedAt);
   }



   /*******************************************************************************
    ** Setter for finishedAt
    *******************************************************************************/
   public void setFinishedAt(String finishedAt)
   {
      this.finishedAt = finishedAt;
   }



   /*******************************************************************************
    ** Fluent setter for finishedAt
    *******************************************************************************/
   public JobSummaryV1 withFinishedAt(String finishedAt)
   {
      this.finishedAt = finishedAt;
      return (this);
   }



   /*******************************************************************************
    ** Getter for logSeq
    *******************************************************************************/
   public Long getLogSeq()
   {
      return (this.logSeq);
   }



   /*******************************************************************************
    ** Setter for logSeq
    *******************************************************************************/
   public void setLogSeq(Long logSeq)
   {
      this.logSeq = logSeq;
   }



   /*******************************************************************************
    ** Fluent setter for logSeq
    *******************************************************************************/
   public JobSummaryV1 withLogSeq(Long logSeq)
   {
      this.logSeq = logSeq;
      return (this);
   }

}
