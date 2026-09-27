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

package com.kingsrook.qqq.backend.core.model.metadata.queues;


import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;


/*******************************************************************************
 ** MetaData to define a message queue, which must exist within a QueueProvider.
 **
 ** The name attribute is a globally unique name within the QInstance
 ** The providerName is the connection to the queue system.
 ** The queueName uniquely identifies the queue within the context of the provider.
 ** The processName is the code that runs for messages found on the queue.
 ** The schedule may not be used by all provider types, but defines when the queue is polled.
 *******************************************************************************/
public class QQueueMetaData implements TopLevelMetaDataInterface
{
   private String name;
   private String providerName;
   private String queueName;
   private String processName;

   private QScheduleMetaData schedule;



   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    **
    *******************************************************************************/
   public QQueueMetaData withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Getter for providerName
    **
    *******************************************************************************/
   public String getProviderName()
   {
      return providerName;
   }



   /*******************************************************************************
    ** Setter for providerName
    **
    *******************************************************************************/
   public void setProviderName(String providerName)
   {
      this.providerName = providerName;
   }



   /*******************************************************************************
    ** Fluent setter for providerName
    **
    *******************************************************************************/
   public QQueueMetaData withProviderName(String providerName)
   {
      this.providerName = providerName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queueName
    **
    *******************************************************************************/
   public String getQueueName()
   {
      return queueName;
   }



   /*******************************************************************************
    ** Setter for queueName
    **
    *******************************************************************************/
   public void setQueueName(String queueName)
   {
      this.queueName = queueName;
   }



   /*******************************************************************************
    ** Fluent setter for queueName
    **
    *******************************************************************************/
   public QQueueMetaData withQueueName(String queueName)
   {
      this.queueName = queueName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for processName
    **
    *******************************************************************************/
   public String getProcessName()
   {
      return processName;
   }



   /*******************************************************************************
    ** Setter for processName
    **
    *******************************************************************************/
   public void setProcessName(String processName)
   {
      this.processName = processName;
   }



   /*******************************************************************************
    ** Fluent setter for processName
    **
    *******************************************************************************/
   public QQueueMetaData withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for schedule
    **
    *******************************************************************************/
   public QScheduleMetaData getSchedule()
   {
      return schedule;
   }



   /*******************************************************************************
    ** Setter for schedule
    **
    *******************************************************************************/
   public void setSchedule(QScheduleMetaData schedule)
   {
      this.schedule = schedule;
   }



   /*******************************************************************************
    ** Fluent setter for schedule
    **
    *******************************************************************************/
   public QQueueMetaData withSchedule(QScheduleMetaData schedule)
   {
      this.schedule = schedule;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      qInstance.addQueue(this);
   }

}
