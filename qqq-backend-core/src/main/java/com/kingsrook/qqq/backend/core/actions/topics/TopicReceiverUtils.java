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


import java.util.ArrayList;
import java.util.List;
import com.google.gson.reflect.TypeToken;
import com.kingsrook.qqq.backend.core.actions.customizers.QCodeLoader;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.ReceiveFromTopicInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.TopicMessage;
import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicSubscriberMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class TopicReceiverUtils
{
   private static final QLogger LOG = QLogger.getLogger(TopicReceiverUtils.class);



   /***************************************************************************
    *
    ***************************************************************************/
   public static void processMessagesThroughSubscribers(String topicName, List<TopicMessage> messageList) throws QException
   {
      QTopicMetaData topic = QContext.getQInstance().getTopic(topicName);
      for(QTopicSubscriberMetaData subscriber : CollectionUtils.nonNullList(topic.getSubscribers()))
      {
         processMessagesThroughSubscriber(topicName, subscriber, messageList);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static void processMessagesThroughSubscriber(String topicName, QTopicSubscriberMetaData subscriber, List<TopicMessage> messageList) throws QException
   {
      if(subscriber.getReceiverCodeReference() != null)
      {
         ReceiveFromTopicInput input = new ReceiveFromTopicInput().withTopicMessageList(messageList);

         ReceiveFromTopicInterface receiveFromTopic = QCodeLoader.getAdHoc(ReceiveFromTopicInterface.class, subscriber.getReceiverCodeReference());
         receiveFromTopic.receiveTopicMessage(input);
      }
      else if(subscriber.getProcessName() != null)
      {
         ArrayList<TopicMessage> messageArrayList = CollectionUtils.useOrWrap(messageList, new TypeToken<>() {});
         RunProcessInput         input            = new RunProcessInput();
         input.setProcessName(subscriber.getProcessName());
         input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
         input.withValue("messageList", messageArrayList);
         new RunProcessAction().execute(input);
      }
      else
      {
         LOG.warn("Subscriber missing receiverCodeReference and processName", logPair("topicName", topicName), logPair("subscriber", subscriber.getName()));
      }
   }

}
