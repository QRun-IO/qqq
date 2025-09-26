/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.qqq.backend.core.model.metadata.topics;


import com.kingsrook.qqq.backend.core.model.metadata.QMetaDataObject;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;


/*******************************************************************************
 ** MetaData to define a subscriber to a topic.
 **
 ** The name attribute is a globally unique name within the QInstance
 ** The providerName is the connection to the topic system.
 ** The topicName uniquely identifies the topic within the context of the provider.
 ** The processName is the code that runs for messages found on the topic.
 ** The handlerCodeReference is an alternative to a process name for handling messages.
 ** The schedule may not be used by all provider types, but defines when the topic is polled.
 *******************************************************************************/
public class QTopicSubscriberMetaData implements QMetaDataObject
{
   private String name;

   private String         processName;
   private QCodeReference receiverCodeReference;

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
   public QTopicSubscriberMetaData withName(String name)
   {
      this.name = name;
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
   public QTopicSubscriberMetaData withProcessName(String processName)
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
   public QTopicSubscriberMetaData withSchedule(QScheduleMetaData schedule)
   {
      this.schedule = schedule;
      return (this);
   }



   /*******************************************************************************
    * Getter for receiverCodeReference
    * @see #withReceiverCodeReference(QCodeReference)
    *******************************************************************************/
   public QCodeReference getReceiverCodeReference()
   {
      return (this.receiverCodeReference);
   }



   /*******************************************************************************
    * Setter for receiverCodeReference
    * @see #withReceiverCodeReference(QCodeReference)
    *******************************************************************************/
   public void setReceiverCodeReference(QCodeReference receiverCodeReference)
   {
      this.receiverCodeReference = receiverCodeReference;
   }



   /*******************************************************************************
    * Fluent setter for receiverCodeReference
    *
    * @param receiverCodeReference
    * CodeReference to class that receives topic messages.  Must implement
    * {@link com.kingsrook.qqq.backend.core.actions.topics.ReceiveFromTopicInterface}
    * @return this
    *******************************************************************************/
   public QTopicSubscriberMetaData withReceiverCodeReference(QCodeReference receiverCodeReference)
   {
      this.receiverCodeReference = receiverCodeReference;
      return (this);
   }

}
