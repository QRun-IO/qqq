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

package com.kingsrook.qqq.backend.core.actions.topics;


import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.AbstractQActionFunction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.topics.PostToTopicInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.PostToTopicOutput;
import com.kingsrook.qqq.backend.core.model.actions.topics.PostToTopicSingleOutput;
import com.kingsrook.qqq.backend.core.model.actions.topics.TopicMessage;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicProviderMetaData;
import com.kingsrook.qqq.backend.core.modules.topics.QTopicProviderDispatcher;
import com.kingsrook.qqq.backend.core.modules.topics.TopicProviderInterface;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * action to post messages to topics
 *******************************************************************************/
public class PostToTopicAction extends AbstractQActionFunction<PostToTopicInput, PostToTopicOutput>
{
   private static final QLogger LOG = QLogger.getLogger(PostToTopicAction.class);



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public PostToTopicOutput execute(PostToTopicInput input) throws QException
   {
      if(input == null)
      {
         return null;
      }

      /////////////////////////////////////
      // split up the inputs by provider //
      /////////////////////////////////////
      QInstance                         qInstance        = QContext.getQInstance();
      ListingHash<String, TopicMessage> inputPerProvider = new ListingHash<>();
      PostToTopicOutput.Builder         outputBuilder    = new PostToTopicOutput.Builder(input);

      for(TopicMessage singleInput : input.getTopicMessageList())
      {
         String         topicName = singleInput.getTopicName();
         QTopicMetaData topic     = qInstance.getTopic(topicName);

         if(topic == null)
         {
            outputBuilder.add(new PostToTopicSingleOutput(singleInput).withHadError(true).withErrorMessage("Unrecognized topic name: " + topicName));
            continue;
         }

         String                 providerName = topic.getProviderName();
         QTopicProviderMetaData provider     = qInstance.getTopicProvider(providerName);
         if(provider == null)
         {
            outputBuilder.add(new PostToTopicSingleOutput(singleInput).withHadError(true).withErrorMessage("Unrecognized topic name: " + topicName));
            continue;
         }

         inputPerProvider.add(providerName, singleInput);
      }

      ///////////////////////////
      // process each provider //
      ///////////////////////////
      for(Map.Entry<String, List<TopicMessage>> entry : inputPerProvider.entrySet())
      {
         String             providerName = entry.getKey();
         List<TopicMessage> subInputList = entry.getValue();

         try
         {
            QTopicProviderMetaData provider               = qInstance.getTopicProvider(providerName);
            TopicProviderInterface topicProviderInterface = new QTopicProviderDispatcher().getTopicProviderInterface(provider);

            PostToTopicInput  subInput  = new PostToTopicInput().withTopicMessageList(subInputList);
            PostToTopicOutput subOutput = topicProviderInterface.post(subInput);

            for(PostToTopicSingleOutput postToTopicSingleOutput : subOutput.getPostToTopicSingleOutputList())
            {
               outputBuilder.add(postToTopicSingleOutput);
            }
         }
         catch(QException e)
         {
            LOG.warn("Error posting to topic provider", e, logPair("providerName", providerName));
            for(TopicMessage singleInput : subInputList)
            {
               outputBuilder.add(new PostToTopicSingleOutput(singleInput).withHadError(true).withErrorMessage("Error posting to topic provider: " + e.getMessage()));
            }
         }
      }

      return outputBuilder.build();
   }

}
