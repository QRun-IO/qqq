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


package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;


/*******************************************************************************
 ** job.progress data, sent on the owner and job record channels.
 *******************************************************************************/
public class RealtimeJobProgressEventV1 implements ToSchema
{
   private Integer jobId;
   private String status;
   private Integer chunksDone;
   private Integer chunksTotal;
   private Integer rowsDone;
   private Integer rowsTotal;
   private Integer rowsFailed;
   private Double rowsPerSecond;
   private Long etaSeconds;
   private String currentPhase;
   private Double percent;
   private String message;
   private Long bytesDone;
   private Long bytesTotal;
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
   public RealtimeJobProgressEventV1 withJobId(Integer jobId)
   {
      this.jobId = jobId;
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
   public RealtimeJobProgressEventV1 withStatus(String status)
   {
      this.status = status;
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
   public RealtimeJobProgressEventV1 withChunksDone(Integer chunksDone)
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
   public RealtimeJobProgressEventV1 withChunksTotal(Integer chunksTotal)
   {
      this.chunksTotal = chunksTotal;
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
   public RealtimeJobProgressEventV1 withRowsDone(Integer rowsDone)
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
   public RealtimeJobProgressEventV1 withRowsTotal(Integer rowsTotal)
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
   public RealtimeJobProgressEventV1 withRowsFailed(Integer rowsFailed)
   {
      this.rowsFailed = rowsFailed;
      return (this);
   }



   /*******************************************************************************
    ** Getter for rowsPerSecond
    *******************************************************************************/
   public Double getRowsPerSecond()
   {
      return (this.rowsPerSecond);
   }



   /*******************************************************************************
    ** Setter for rowsPerSecond
    *******************************************************************************/
   public void setRowsPerSecond(Double rowsPerSecond)
   {
      this.rowsPerSecond = rowsPerSecond;
   }



   /*******************************************************************************
    ** Fluent setter for rowsPerSecond
    *******************************************************************************/
   public RealtimeJobProgressEventV1 withRowsPerSecond(Double rowsPerSecond)
   {
      this.rowsPerSecond = rowsPerSecond;
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
   public RealtimeJobProgressEventV1 withEtaSeconds(Long etaSeconds)
   {
      this.etaSeconds = etaSeconds;
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
   public RealtimeJobProgressEventV1 withCurrentPhase(String currentPhase)
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
   public RealtimeJobProgressEventV1 withPercent(Double percent)
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
   public RealtimeJobProgressEventV1 withMessage(String message)
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
   public RealtimeJobProgressEventV1 withBytesDone(Long bytesDone)
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
   public RealtimeJobProgressEventV1 withBytesTotal(Long bytesTotal)
   {
      this.bytesTotal = bytesTotal;
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
   public RealtimeJobProgressEventV1 withLogSeq(Long logSeq)
   {
      this.logSeq = logSeq;
      return (this);
   }

}
