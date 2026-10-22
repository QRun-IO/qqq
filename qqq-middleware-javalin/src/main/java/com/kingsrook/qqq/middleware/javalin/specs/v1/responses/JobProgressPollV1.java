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


import java.util.List;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
 ** Jobs UI API v1 representation.
 *******************************************************************************/
public class JobProgressPollV1 implements ToSchema
{
   private JobSummaryV1 job;
   private JobProgressV1 progress;
   @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
   @OpenAPIListItems(value = JobLogLineV1.class, useRef = true)
   private List<JobLogLineV1> log;
   private Long nextAfterLogId;
   private Integer logDropped;
   private Boolean terminal;



   /*******************************************************************************
    ** Getter for job
    *******************************************************************************/
   public JobSummaryV1 getJob()
   {
      return (this.job);
   }



   /*******************************************************************************
    ** Setter for job
    *******************************************************************************/
   public void setJob(JobSummaryV1 job)
   {
      this.job = job;
   }



   /*******************************************************************************
    ** Fluent setter for job
    *******************************************************************************/
   public JobProgressPollV1 withJob(JobSummaryV1 job)
   {
      this.job = job;
      return (this);
   }



   /*******************************************************************************
    ** Getter for progress
    *******************************************************************************/
   public JobProgressV1 getProgress()
   {
      return (this.progress);
   }



   /*******************************************************************************
    ** Setter for progress
    *******************************************************************************/
   public void setProgress(JobProgressV1 progress)
   {
      this.progress = progress;
   }



   /*******************************************************************************
    ** Fluent setter for progress
    *******************************************************************************/
   public JobProgressPollV1 withProgress(JobProgressV1 progress)
   {
      this.progress = progress;
      return (this);
   }



   /*******************************************************************************
    ** Getter for log
    *******************************************************************************/
   public List<JobLogLineV1> getLog()
   {
      return (this.log);
   }



   /*******************************************************************************
    ** Setter for log
    *******************************************************************************/
   public void setLog(List<JobLogLineV1> log)
   {
      this.log = log;
   }



   /*******************************************************************************
    ** Fluent setter for log
    *******************************************************************************/
   public JobProgressPollV1 withLog(List<JobLogLineV1> log)
   {
      this.log = log;
      return (this);
   }



   /*******************************************************************************
    ** Getter for nextAfterLogId
    *******************************************************************************/
   public Long getNextAfterLogId()
   {
      return (this.nextAfterLogId);
   }



   /*******************************************************************************
    ** Setter for nextAfterLogId
    *******************************************************************************/
   public void setNextAfterLogId(Long nextAfterLogId)
   {
      this.nextAfterLogId = nextAfterLogId;
   }



   /*******************************************************************************
    ** Fluent setter for nextAfterLogId
    *******************************************************************************/
   public JobProgressPollV1 withNextAfterLogId(Long nextAfterLogId)
   {
      this.nextAfterLogId = nextAfterLogId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for logDropped
    *******************************************************************************/
   public Integer getLogDropped()
   {
      return (this.logDropped);
   }



   /*******************************************************************************
    ** Setter for logDropped
    *******************************************************************************/
   public void setLogDropped(Integer logDropped)
   {
      this.logDropped = logDropped;
   }



   /*******************************************************************************
    ** Fluent setter for logDropped
    *******************************************************************************/
   public JobProgressPollV1 withLogDropped(Integer logDropped)
   {
      this.logDropped = logDropped;
      return (this);
   }



   /*******************************************************************************
    ** Getter for terminal
    *******************************************************************************/
   public Boolean getTerminal()
   {
      return (this.terminal);
   }



   /*******************************************************************************
    ** Setter for terminal
    *******************************************************************************/
   public void setTerminal(Boolean terminal)
   {
      this.terminal = terminal;
   }



   /*******************************************************************************
    ** Fluent setter for terminal
    *******************************************************************************/
   public JobProgressPollV1 withTerminal(Boolean terminal)
   {
      this.terminal = terminal;
      return (this);
   }

}

