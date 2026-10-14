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


package com.kingsrook.qqq.backend.core.jobs.progress;


import java.io.Serializable;
import java.time.Instant;


/*******************************************************************************
 ** Counts and display text for job progress. Never contains row values.
 *******************************************************************************/
public class JobLogLine implements Serializable
{
   private Long id;
   private Integer jobId;
   private Integer chunkId;
   private Integer attempt;
   private JobLogLevel level;
   private Instant loggedAt;
   private String phase;
   private String message;



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Long getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(Long id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public JobLogLine withId(Long id)
   {
      this.id = id;
      return (this);
   }



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
   public JobLogLine withJobId(Integer jobId)
   {
      this.jobId = jobId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for chunkId
    *******************************************************************************/
   public Integer getChunkId()
   {
      return (this.chunkId);
   }



   /*******************************************************************************
    ** Setter for chunkId
    *******************************************************************************/
   public void setChunkId(Integer chunkId)
   {
      this.chunkId = chunkId;
   }



   /*******************************************************************************
    ** Fluent setter for chunkId
    *******************************************************************************/
   public JobLogLine withChunkId(Integer chunkId)
   {
      this.chunkId = chunkId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for attempt
    *******************************************************************************/
   public Integer getAttempt()
   {
      return (this.attempt);
   }



   /*******************************************************************************
    ** Setter for attempt
    *******************************************************************************/
   public void setAttempt(Integer attempt)
   {
      this.attempt = attempt;
   }



   /*******************************************************************************
    ** Fluent setter for attempt
    *******************************************************************************/
   public JobLogLine withAttempt(Integer attempt)
   {
      this.attempt = attempt;
      return (this);
   }



   /*******************************************************************************
    ** Getter for level
    *******************************************************************************/
   public JobLogLevel getLevel()
   {
      return (this.level);
   }



   /*******************************************************************************
    ** Setter for level
    *******************************************************************************/
   public void setLevel(JobLogLevel level)
   {
      this.level = level;
   }



   /*******************************************************************************
    ** Fluent setter for level
    *******************************************************************************/
   public JobLogLine withLevel(JobLogLevel level)
   {
      this.level = level;
      return (this);
   }



   /*******************************************************************************
    ** Getter for loggedAt
    *******************************************************************************/
   public Instant getLoggedAt()
   {
      return (this.loggedAt);
   }



   /*******************************************************************************
    ** Setter for loggedAt
    *******************************************************************************/
   public void setLoggedAt(Instant loggedAt)
   {
      this.loggedAt = loggedAt;
   }



   /*******************************************************************************
    ** Fluent setter for loggedAt
    *******************************************************************************/
   public JobLogLine withLoggedAt(Instant loggedAt)
   {
      this.loggedAt = loggedAt;
      return (this);
   }



   /*******************************************************************************
    ** Getter for phase
    *******************************************************************************/
   public String getPhase()
   {
      return (this.phase);
   }



   /*******************************************************************************
    ** Setter for phase
    *******************************************************************************/
   public void setPhase(String phase)
   {
      this.phase = phase;
   }



   /*******************************************************************************
    ** Fluent setter for phase
    *******************************************************************************/
   public JobLogLine withPhase(String phase)
   {
      this.phase = phase;
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
   public JobLogLine withMessage(String message)
   {
      this.message = message;
      return (this);
   }

}
