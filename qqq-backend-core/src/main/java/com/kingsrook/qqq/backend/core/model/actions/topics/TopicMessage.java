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


/*******************************************************************************
 * Object wrapper for a single message posted to a topic.
 *******************************************************************************/
public class TopicMessage
{
   private String topicName;
   private String message;



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
    * Name of the topic to post to.  must be a defined topic name in the QInstance.
    * @return this
    *******************************************************************************/
   public TopicMessage withTopicName(String topicName)
   {
      this.topicName = topicName;
      return (this);
   }



   /*******************************************************************************
    * Getter for message
    * @see #withMessage(String)
    *******************************************************************************/
   public String getMessage()
   {
      return (this.message);
   }



   /*******************************************************************************
    * Setter for message
    * @see #withMessage(String)
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    * Fluent setter for message
    *
    * @param message
    * String message to post to the topic.
    * @return this
    *******************************************************************************/
   public TopicMessage withMessage(String message)
   {
      this.message = message;
      return (this);
   }

}
