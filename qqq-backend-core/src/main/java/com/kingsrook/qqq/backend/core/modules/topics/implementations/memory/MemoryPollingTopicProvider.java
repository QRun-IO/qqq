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
import com.kingsrook.qqq.backend.core.modules.topics.PollingTopicProviderInterface;
import com.kingsrook.qqq.backend.core.modules.topics.TopicProviderInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class MemoryPollingTopicProvider implements TopicProviderInterface, PollingTopicProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(MemoryPollingTopicProvider.class);



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public TopicType getType()
   {
      return (TopicType.IN_MEMORY_POLLING);
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

      PostToTopicOutput output = new PostToTopicOutput();
      for(TopicMessage singleInput : input.getTopicMessageList())
      {
         PostToTopicSingleOutput singleOutput = new PostToTopicSingleOutput(singleInput);
         output.withSingleOutput(singleOutput);

         postSingle(singleInput, singleOutput);
      }

      return (output);
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



   /***************************************************************************
    *
    ***************************************************************************/
   private void postSingle(TopicMessage singleInput, PostToTopicSingleOutput singleOutput)
   {
      try
      {
         MemoryTopicDataStore.getInstance().post(singleInput.getTopicName(), singleInput.getMessage());
         singleOutput.setHadError(false);
      }
      catch(Exception e)
      {
         LOG.warn("Error while posting single input to topic", e, logPair("topicName", singleInput.getTopicName()));
         singleOutput.setHadError(true);
         singleOutput.setErrorMessage(e.getMessage());
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void poll(String topicName)
   {
      try
      {
         List<TopicMessage> messageList = MemoryTopicDataStore.getInstance().poll(topicName, null);

         if(CollectionUtils.nullSafeHasContents(messageList))
         {
            TopicReceiverUtils.processMessagesThroughSubscribers(topicName, messageList);
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error polling from topic", e, logPair("topicName", topicName));
      }
   }


}

