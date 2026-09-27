/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.queues;


/*******************************************************************************
 ** settings that can be applied to either an SQSQueue or an SQSQueueProvider,
 ** to control what the SQSQueuePoller does when it receives from AWS.
 *******************************************************************************/
public class SQSPollerSettings
{
   private Integer maxNumberOfMessages;
   private Integer waitTimeSeconds;
   private Integer maxLoops;



   /*******************************************************************************
    ** Getter for maxNumberOfMessages
    *******************************************************************************/
   public Integer getMaxNumberOfMessages()
   {
      return (this.maxNumberOfMessages);
   }



   /*******************************************************************************
    ** Setter for maxNumberOfMessages
    *******************************************************************************/
   public void setMaxNumberOfMessages(Integer maxNumberOfMessages)
   {
      this.maxNumberOfMessages = maxNumberOfMessages;
   }



   /*******************************************************************************
    ** Fluent setter for maxNumberOfMessages
    *******************************************************************************/
   public SQSPollerSettings withMaxNumberOfMessages(Integer maxNumberOfMessages)
   {
      this.maxNumberOfMessages = maxNumberOfMessages;
      return (this);
   }



   /*******************************************************************************
    ** Getter for waitTimeSeconds
    *******************************************************************************/
   public Integer getWaitTimeSeconds()
   {
      return (this.waitTimeSeconds);
   }



   /*******************************************************************************
    ** Setter for waitTimeSeconds
    *******************************************************************************/
   public void setWaitTimeSeconds(Integer waitTimeSeconds)
   {
      this.waitTimeSeconds = waitTimeSeconds;
   }



   /*******************************************************************************
    ** Fluent setter for waitTimeSeconds
    *******************************************************************************/
   public SQSPollerSettings withWaitTimeSeconds(Integer waitTimeSeconds)
   {
      this.waitTimeSeconds = waitTimeSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxLoops
    *******************************************************************************/
   public Integer getMaxLoops()
   {
      return (this.maxLoops);
   }



   /*******************************************************************************
    ** Setter for maxLoops
    *******************************************************************************/
   public void setMaxLoops(Integer maxLoops)
   {
      this.maxLoops = maxLoops;
   }



   /*******************************************************************************
    ** Fluent setter for maxLoops
    *******************************************************************************/
   public SQSPollerSettings withMaxLoops(Integer maxLoops)
   {
      this.maxLoops = maxLoops;
      return (this);
   }

}
