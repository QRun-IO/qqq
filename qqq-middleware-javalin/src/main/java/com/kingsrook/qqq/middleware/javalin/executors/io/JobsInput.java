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


package com.kingsrook.qqq.middleware.javalin.executors.io;


import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.JobTrackRequestV1;


/*******************************************************************************
 ** Parsed jobs operation parameters.
 *******************************************************************************/
public class JobsInput extends AbstractMiddlewareInput
{
   private String operation;
   private Integer jobId;
   private String scope;
   private String state;
   private Integer limit;
   private String processName;
   private Long afterLogId;
   private Integer logLimit;
   private JobTrackRequestV1 track;



   /*******************************************************************************
    ** Getter for operation
    *******************************************************************************/
   public String getOperation()
   {
      return (this.operation);
   }



   /*******************************************************************************
    ** Setter for operation
    *******************************************************************************/
   public void setOperation(String operation)
   {
      this.operation = operation;
   }



   /*******************************************************************************
    ** Fluent setter for operation
    *******************************************************************************/
   public JobsInput withOperation(String operation)
   {
      this.operation = operation;
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
   public JobsInput withJobId(Integer jobId)
   {
      this.jobId = jobId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scope
    *******************************************************************************/
   public String getScope()
   {
      return (this.scope);
   }



   /*******************************************************************************
    ** Setter for scope
    *******************************************************************************/
   public void setScope(String scope)
   {
      this.scope = scope;
   }



   /*******************************************************************************
    ** Fluent setter for scope
    *******************************************************************************/
   public JobsInput withScope(String scope)
   {
      this.scope = scope;
      return (this);
   }



   /*******************************************************************************
    ** Getter for state
    *******************************************************************************/
   public String getState()
   {
      return (this.state);
   }



   /*******************************************************************************
    ** Setter for state
    *******************************************************************************/
   public void setState(String state)
   {
      this.state = state;
   }



   /*******************************************************************************
    ** Fluent setter for state
    *******************************************************************************/
   public JobsInput withState(String state)
   {
      this.state = state;
      return (this);
   }



   /*******************************************************************************
    ** Getter for limit
    *******************************************************************************/
   public Integer getLimit()
   {
      return (this.limit);
   }



   /*******************************************************************************
    ** Setter for limit
    *******************************************************************************/
   public void setLimit(Integer limit)
   {
      this.limit = limit;
   }



   /*******************************************************************************
    ** Fluent setter for limit
    *******************************************************************************/
   public JobsInput withLimit(Integer limit)
   {
      this.limit = limit;
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
   public JobsInput withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for afterLogId
    *******************************************************************************/
   public Long getAfterLogId()
   {
      return (this.afterLogId);
   }



   /*******************************************************************************
    ** Setter for afterLogId
    *******************************************************************************/
   public void setAfterLogId(Long afterLogId)
   {
      this.afterLogId = afterLogId;
   }



   /*******************************************************************************
    ** Fluent setter for afterLogId
    *******************************************************************************/
   public JobsInput withAfterLogId(Long afterLogId)
   {
      this.afterLogId = afterLogId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for logLimit
    *******************************************************************************/
   public Integer getLogLimit()
   {
      return (this.logLimit);
   }



   /*******************************************************************************
    ** Setter for logLimit
    *******************************************************************************/
   public void setLogLimit(Integer logLimit)
   {
      this.logLimit = logLimit;
   }



   /*******************************************************************************
    ** Fluent setter for logLimit
    *******************************************************************************/
   public JobsInput withLogLimit(Integer logLimit)
   {
      this.logLimit = logLimit;
      return (this);
   }



   /*******************************************************************************
    ** Getter for track
    *******************************************************************************/
   public JobTrackRequestV1 getTrack()
   {
      return (this.track);
   }



   /*******************************************************************************
    ** Setter for track
    *******************************************************************************/
   public void setTrack(JobTrackRequestV1 track)
   {
      this.track = track;
   }



   /*******************************************************************************
    ** Fluent setter for track
    *******************************************************************************/
   public JobsInput withTrack(JobTrackRequestV1 track)
   {
      this.track = track;
      return (this);
   }

}
