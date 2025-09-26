/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.topics;


import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicSubscriberMetaData;


/*******************************************************************************
 * Input wrapper for the action to subscribe to a topic
 * ({@link com.kingsrook.qqq.backend.core.actions.topics.SubscribeToTopicAction}.
 * wrapper for a list of multiple individual messages (e.g., to run in bulk by
 * default).
 *******************************************************************************/
public class SubscribeToTopicInput extends AbstractActionInput
{
   private QTopicSubscriberMetaData subscriberMetaData;
   private String                   topicName;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public SubscribeToTopicInput()
   {
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public SubscribeToTopicInput(String topicName, QTopicSubscriberMetaData subscriberMetaData)
   {
      this.topicName = topicName;
      this.subscriberMetaData = subscriberMetaData;
   }



   /*******************************************************************************
    * Getter for subscriberMetaData
    * @see #withSubscriberMetaData(QTopicSubscriberMetaData)
    *******************************************************************************/
   public QTopicSubscriberMetaData getSubscriberMetaData()
   {
      return (this.subscriberMetaData);
   }



   /*******************************************************************************
    * Setter for subscriberMetaData
    * @see #withSubscriberMetaData(QTopicSubscriberMetaData)
    *******************************************************************************/
   public void setSubscriberMetaData(QTopicSubscriberMetaData subscriberMetaData)
   {
      this.subscriberMetaData = subscriberMetaData;
   }



   /*******************************************************************************
    * Fluent setter for subscriberMetaData
    *
    * @param subscriberMetaData
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public SubscribeToTopicInput withSubscriberMetaData(QTopicSubscriberMetaData subscriberMetaData)
   {
      this.subscriberMetaData = subscriberMetaData;
      return (this);
   }



   /*******************************************************************************
    * Getter for topicName
    * @see #withTopicName(String)
    *******************************************************************************/
   public String getTopicName()
   {
      return (this.topicName);
   }



   /*******************************************************************************
    * Setter for topicName
    * @see #withTopicName(String)
    *******************************************************************************/
   public void setTopicName(String topicName)
   {
      this.topicName = topicName;
   }



   /*******************************************************************************
    * Fluent setter for topicName
    *
    * @param topicName
    * TODO document this property
    *
    * @return this
    *******************************************************************************/
   public SubscribeToTopicInput withTopicName(String topicName)
   {
      this.topicName = topicName;
      return (this);
   }

}
