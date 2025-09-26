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

package com.kingsrook.qqq.backend.core.modules.topics.implementations.memory;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.topics.TopicReceiverUtils;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.topics.PostToTopicInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.PostToTopicOutput;
import com.kingsrook.qqq.backend.core.model.actions.topics.PostToTopicSingleOutput;
import com.kingsrook.qqq.backend.core.model.actions.topics.SubscribeToTopicInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.SubscribeToTopicOutput;
import com.kingsrook.qqq.backend.core.model.actions.topics.TopicMessage;
import com.kingsrook.qqq.backend.core.model.metadata.topics.TopicType;
import com.kingsrook.qqq.backend.core.modules.topics.TopicProviderInterface;
import com.kingsrook.qqq.backend.core.utils.ListingHash;


/*******************************************************************************
 **
 *******************************************************************************/
public class MemoryCallbackTopicProvider implements TopicProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(MemoryCallbackTopicProvider.class);



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public TopicType getType()
   {
      return (TopicType.IN_MEMORY_CALLBACK);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public PostToTopicOutput post(PostToTopicInput input)
   {
      if(input == null)
      {
         return null;
      }

      ListingHash<String, TopicMessage> messagesByTopic = new ListingHash<>();
      for(TopicMessage topicMessage : input.getTopicMessageList())
      {
         messagesByTopic.add(topicMessage.getTopicName(), topicMessage);
      }

      PostToTopicOutput.Builder outputBuilder = new PostToTopicOutput.Builder(input);
      for(Map.Entry<String, List<TopicMessage>> entry : messagesByTopic.entrySet())
      {
         String             topicName     = entry.getKey();
         List<TopicMessage> topicMessages = entry.getValue();

         try
         {
            TopicReceiverUtils.processMessagesThroughSubscribers(topicName, topicMessages);
            topicMessages.forEach(tm -> outputBuilder.add(new PostToTopicSingleOutput(tm).withHadError(false)));
         }
         catch(Exception e)
         {
            String errorMessage = "Error processing topic message: " + e.getMessage();
            topicMessages.forEach(tm -> outputBuilder.add(new PostToTopicSingleOutput(tm).withHadError(true).withErrorMessage(errorMessage)));
         }
      }

      return (outputBuilder.build());
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public SubscribeToTopicOutput subscribe(SubscribeToTopicInput input) throws QException
   {
      // todo wip
      return null;
   }

}

