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


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;


/*******************************************************************************
 * Input wrapper for the action to post to a topic
 * ({@link com.kingsrook.qqq.backend.core.actions.topics.PostToTopicAction}.
 * wrapper for a list of multiple individual messages (e.g., to run in bulk by
 * default).
 *******************************************************************************/
public class PostToTopicInput extends AbstractActionInput
{
   private List<TopicMessage> topicMessageList = new ArrayList<>();



   /***************************************************************************
    * fluently add a single input object to this action input.
    ***************************************************************************/
   public PostToTopicInput withTopicMessage(TopicMessage topicMessage)
   {
      if(this.topicMessageList == null)
      {
         this.topicMessageList = new ArrayList<>();
      }
      this.topicMessageList.add(topicMessage);
      return (this);
   }



   /*******************************************************************************
    * Getter for topicMessageList
    * @see #withTopicMessageList(List)
    *******************************************************************************/
   public List<TopicMessage> getTopicMessageList()
   {
      return (this.topicMessageList);
   }



   /*******************************************************************************
    * Setter for topicMessageList
    * @see #withTopicMessageList(List)
    *******************************************************************************/
   public void setTopicMessageList(List<TopicMessage> topicMessageList)
   {
      this.topicMessageList = topicMessageList;
   }



   /*******************************************************************************
    * Fluent setter for topicMessageList
    *
    * @param topicMessageList
    * list of individual messages to post
    * @return this
    *******************************************************************************/
   public PostToTopicInput withTopicMessageList(List<TopicMessage> topicMessageList)
   {
      this.topicMessageList = topicMessageList;
      return (this);
   }

}
