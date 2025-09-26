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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;


/*******************************************************************************
 ** MetaData to define a message topic, which must exist within a TopicProvider.
 **
 ** The name attribute is a globally unique name within the QInstance
 ** The providerName is the connection to the topic system.
 ** The topicName uniquely identifies the topic within the context of the provider.
 ** The processName is the code that runs for messages found on the topic.
 ** The handlerCodeReference is an alternative to a process name for handling messages.
 ** The schedule may not be used by all provider types, but defines when the topic is polled.
 *******************************************************************************/
public class QTopicMetaData implements TopLevelMetaDataInterface
{
   private String name;
   private String providerName;
   private String topicName;

   private List<QTopicSubscriberMetaData> subscribers; // todo wip - just track this for the memory ones via their subscribe methods.



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
   public QTopicMetaData withName(String name)
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
   public QTopicMetaData withProviderName(String providerName)
   {
      this.providerName = providerName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for topicName
    **
    *******************************************************************************/
   public String getTopicName()
   {
      return topicName;
   }



   /*******************************************************************************
    ** Setter for topicName
    **
    *******************************************************************************/
   public void setTopicName(String topicName)
   {
      this.topicName = topicName;
   }



   /*******************************************************************************
    ** Fluent setter for topicName
    **
    *******************************************************************************/
   public QTopicMetaData withTopicName(String topicName)
   {
      this.topicName = topicName;
      return (this);
   }



   /*******************************************************************************
    * Getter for subscribers
    * @see #withSubscribers(List)
    *******************************************************************************/
   public List<QTopicSubscriberMetaData> getSubscribers()
   {
      return (this.subscribers);
   }



   /*******************************************************************************
    * Setter for subscribers
    * @see #withSubscribers(List)
    *******************************************************************************/
   public void setSubscribers(List<QTopicSubscriberMetaData> subscribers)
   {
      this.subscribers = subscribers;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void addSubscriber(QTopicSubscriberMetaData subscriber)
   {
      if(this.subscribers == null)
      {
         this.subscribers = new ArrayList<>();
      }
      this.subscribers.add(subscriber);
   }



   /*******************************************************************************
    * Fluent setter for subscribers
    *
    * @param subscribers
    * List of who should be notified when messages are posted to this topic.
    * @return this
    *******************************************************************************/
   public QTopicMetaData withSubscribers(List<QTopicSubscriberMetaData> subscribers)
   {
      this.subscribers = subscribers;
      return (this);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      qInstance.addTopic(this);
   }

}
