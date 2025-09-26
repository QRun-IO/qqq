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

package com.kingsrook.qqq.backend.core.modules.topics.implementations.rabbitmq;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.topics.PostToTopicAction;
import com.kingsrook.qqq.backend.core.actions.topics.ReceiveFromTopicInterface;
import com.kingsrook.qqq.backend.core.actions.topics.SubscribeToTopicAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.topics.PostToTopicInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.ReceiveFromTopicInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.ReceiveFromTopicOutput;
import com.kingsrook.qqq.backend.core.model.actions.topics.SubscribeToTopicInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.TopicMessage;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicSubscriberMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.topics.implementations.rabbitmq.RabbitMQTopicProviderMetaData;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for RabbitMQTopicProvider 
 *******************************************************************************/
class RabbitMQTopicProviderTest extends BaseTest
{
   private static final String PROVIDER_NAME = "rabbitmq";
   private static final String TOPIC_NAME    = "logs";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      QInstance qInstance = QContext.getQInstance();

      qInstance.addTopicProvider(new RabbitMQTopicProviderMetaData()
         .withName(PROVIDER_NAME)
         .withHost("localhost")
         .withPort(5672)
         .withUsername("guest")
         .withPassword("guest"));

      qInstance.addTopic(new QTopicMetaData()
         .withName(TOPIC_NAME)
         .withProviderName(PROVIDER_NAME)
         .withTopicName("logs")); // this is being treated as a rabbitmq exchange name...

   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test() throws QException
   {
      new PostToTopicAction().execute(new PostToTopicInput()
         .withTopicMessage(new TopicMessage().withTopicName(TOPIC_NAME).withMessage("hi")));

      new SubscribeToTopicAction().execute(new SubscribeToTopicInput()
         .withTopicName(TOPIC_NAME)
         .withSubscriberMetaData(new QTopicSubscriberMetaData()
            .withName("LogSubscriber")
            .withReceiverCodeReference(new QCodeReference(LogTopicReceiver.class))
         ));

      new PostToTopicAction().execute(new PostToTopicInput()
         .withTopicMessage(new TopicMessage().withTopicName(TOPIC_NAME).withMessage("hey")));

      SleepUtils.sleep(1, TimeUnit.SECONDS);
      assertEquals(List.of("logs:hey"), LogTopicReceiver.receivedMessages);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static class LogTopicReceiver implements ReceiveFromTopicInterface
   {
      static List<String> receivedMessages = new ArrayList<>();



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public ReceiveFromTopicOutput receiveTopicMessage(ReceiveFromTopicInput input)
      {
         for(TopicMessage topicMessage : input.getTopicMessageList())
         {
            receivedMessages.add(topicMessage.getTopicName() + ":" + topicMessage.getMessage());
         }
         return new ReceiveFromTopicOutput();
      }
   }

}