/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.async;


import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kingsrook.qqq.backend.core.actions.processes.ProcessStateAccess;


/*******************************************************************************
 ** Object to track current status of an async job - e.g., its state, and some
 ** messages from the backend like "x of y"
 *******************************************************************************/
public class AsyncJobStatus implements Serializable
{
   private transient ProcessStateAccess stateAccess;

   private String        jobName;
   private AsyncJobState state;
   private String        message;
   private Integer       current;
   private Integer       total;
   private Exception     caughtException;

   private boolean cancelRequested;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return "AsyncJobStatus{"
         + "state=" + state
         + ", message='" + message + '\''
         + ", current=" + current
         + ", total=" + total
         + ", caughtException=" + caughtException
         + '}';
   }



   /*******************************************************************************
    ** Getter for state
    **
    *******************************************************************************/
   public AsyncJobState getState()
   {
      return state;
   }



   /*******************************************************************************
    ** Setter for state
    **
    *******************************************************************************/
   public void setState(AsyncJobState state)
   {
      this.state = state;
   }



   /*******************************************************************************
    ** Getter for message
    **
    *******************************************************************************/
   public String getMessage()
   {
      return message;
   }



   /*******************************************************************************
    ** Setter for message
    **
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    ** Getter for current
    **
    *******************************************************************************/
   public Integer getCurrent()
   {
      return current;
   }



   /*******************************************************************************
    ** Setter for current
    **
    *******************************************************************************/
   public void setCurrent(Integer current)
   {
      this.current = current;
   }



   /*******************************************************************************
    ** Getter for total
    **
    *******************************************************************************/
   public Integer getTotal()
   {
      return total;
   }



   /*******************************************************************************
    ** Setter for total
    **
    *******************************************************************************/
   public void setTotal(Integer total)
   {
      this.total = total;
   }



   /*******************************************************************************
    ** Getter for caughtException
    **
    *******************************************************************************/
   public Exception getCaughtException()
   {
      return caughtException;
   }



   /*******************************************************************************
    ** Setter for caughtException
    **
    *******************************************************************************/
   public void setCaughtException(Exception caughtException)
   {
      this.caughtException = caughtException;
   }



   /*******************************************************************************
    ** Getter for cancelRequested
    **
    *******************************************************************************/
   public boolean getCancelRequested()
   {
      return cancelRequested;
   }



   /*******************************************************************************
    ** Setter for cancelRequested
    **
    *******************************************************************************/
   public void setCancelRequested(boolean cancelRequested)
   {
      this.cancelRequested = cancelRequested;
   }



   /*******************************************************************************
    ** Getter for jobName
    *******************************************************************************/
   public String getJobName()
   {
      return (this.jobName);
   }



   /*******************************************************************************
    ** Setter for jobName
    *******************************************************************************/
   public void setJobName(String jobName)
   {
      this.jobName = jobName;
   }



   /*******************************************************************************
    ** Fluent setter for jobName
    *******************************************************************************/
   public AsyncJobStatus withJobName(String jobName)
   {
      this.jobName = jobName;
      return (this);
   }


   /*******************************************************************************
    ** Server-owned authorization, never part of an HTTP state response.
    *******************************************************************************/
   @JsonIgnore
   public ProcessStateAccess getStateAccess()
   {
      return stateAccess;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void setStateAccess(ProcessStateAccess stateAccess)
   {
      this.stateAccess = stateAccess;
   }
}
