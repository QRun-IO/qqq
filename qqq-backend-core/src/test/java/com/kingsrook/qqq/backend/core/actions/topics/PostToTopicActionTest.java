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
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.topics.PollFromTopicTopicInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.PostToTopicInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.ReceiveFromTopicInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.ReceiveFromTopicOutput;
import com.kingsrook.qqq.backend.core.model.actions.topics.TopicMessage;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicSubscriberMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.topics.implementations.memory.MemoryCallbackTopicProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.topics.implementations.memory.MemoryPollingTopicProviderMetaData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;


/*******************************************************************************
 ** Unit test for PostToTopicAction 
 *******************************************************************************/
class PostToTopicActionTest extends BaseTest
{
   private static final String MEMORY_CALLBACK_TOPIC_PROVIDER_NAME = "memoryCallbackTopicProvider";
   private static final String MEMORY_POLLING_TOPIC_PROVIDER_NAME  = "memoryPollingTopicProvider";

   private static final String ORDER_CREATED_TOPIC_NAME   = "orderCreated";
   private static final String ORDER_SHIPPED_TOPIC_NAME   = "orderShipped";
   private static final String SESSION_UPDATED_TOPIC_NAME = "sessionUpdated";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      addTopicMetaData(QContext.getQInstance());
      CustomerNotifierTopicReceiver.receivedMessages.clear();
      WarehouseSystemTopicReceiver.receivedOrderIds.clear();
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private void addTopicMetaData(QInstance qInstance)
   {
      qInstance.addTopicProvider(new MemoryCallbackTopicProviderMetaData().withName(MEMORY_CALLBACK_TOPIC_PROVIDER_NAME));
      qInstance.addTopicProvider(new MemoryPollingTopicProviderMetaData().withName(MEMORY_POLLING_TOPIC_PROVIDER_NAME));

      qInstance.addTopic(new QTopicMetaData()
         .withName(ORDER_CREATED_TOPIC_NAME)
         .withProviderName(MEMORY_CALLBACK_TOPIC_PROVIDER_NAME)
         .withTopicName("order/created"));

      qInstance.addTopic(new QTopicMetaData()
         .withName(ORDER_SHIPPED_TOPIC_NAME)
         .withProviderName(MEMORY_CALLBACK_TOPIC_PROVIDER_NAME)
         .withTopicName("order/shipped"));

      qInstance.addTopic(new QTopicMetaData()
         .withName(SESSION_UPDATED_TOPIC_NAME)
         .withProviderName(MEMORY_POLLING_TOPIC_PROVIDER_NAME)
         .withTopicName("session/updated"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCallbacks() throws QException
   {
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // should safely noop if a message is posted before any subscribers exist (and no one will get that message) //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      new PostToTopicAction().execute(new PostToTopicInput()
         .withTopicMessage(new TopicMessage().withTopicName(ORDER_CREATED_TOPIC_NAME).withMessage("0")));

      ////////////////////////////////////////////////////////////////////////////////////////////
      // now make 2 subscriptions for CustomerNotifierTopicReceiver to orders created & shipped //
      // and subscribe the WarehouseSystem to order created                                     //
      ////////////////////////////////////////////////////////////////////////////////////////////
      QContext.getQInstance().getTopic(ORDER_CREATED_TOPIC_NAME).addSubscriber(new QTopicSubscriberMetaData()
         .withName("customerNotifier")
         .withReceiverCodeReference(new QCodeReference(CustomerNotifierTopicReceiver.class)));

      QContext.getQInstance().getTopic(ORDER_SHIPPED_TOPIC_NAME).addSubscriber(new QTopicSubscriberMetaData()
         .withName("customerNotifier")
         .withReceiverCodeReference(new QCodeReference(CustomerNotifierTopicReceiver.class)));

      QContext.getQInstance().getTopic(ORDER_CREATED_TOPIC_NAME).addSubscriber(new QTopicSubscriberMetaData()
         .withName("warehouseSystem")
         .withReceiverCodeReference(new QCodeReference(WarehouseSystemTopicReceiver.class)));

      ////////////////////////////////////////////////////////
      // post creation of order 1, then shipping of order 2 //
      ////////////////////////////////////////////////////////
      new PostToTopicAction().execute(new PostToTopicInput()
         .withTopicMessage(new TopicMessage().withTopicName(ORDER_CREATED_TOPIC_NAME).withMessage("1")));

      new PostToTopicAction().execute(new PostToTopicInput()
         .withTopicMessage(new TopicMessage().withTopicName(ORDER_SHIPPED_TOPIC_NAME).withMessage("2")));

      //////////////////////////////////////////////////////////////////////////////////////
      // assert CustomerNotifier received both messages (with topic name as part of them) //
      // and WarehouseSystem only saw the order created                                   //
      //////////////////////////////////////////////////////////////////////////////////////
      assertEquals(List.of(ORDER_CREATED_TOPIC_NAME + ":1", ORDER_SHIPPED_TOPIC_NAME + ":2"), CustomerNotifierTopicReceiver.receivedMessages);
      assertEquals(List.of("1"), WarehouseSystemTopicReceiver.receivedOrderIds);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPolling() throws QException
   {
      ///////////////////////
      // set up subscriber //
      ///////////////////////
      QContext.getQInstance().getTopic(SESSION_UPDATED_TOPIC_NAME).addSubscriber(new QTopicSubscriberMetaData()
         .withName("sessionCache")
         .withReceiverCodeReference(new QCodeReference(SessionUpdatedTopicReceiver.class)));

      ////////////////////////////////////////////////////
      // post a message, then poll, and assert delivery //
      ////////////////////////////////////////////////////
      new PostToTopicAction().execute(new PostToTopicInput().withTopicMessage(new TopicMessage().withTopicName(SESSION_UPDATED_TOPIC_NAME).withMessage("0")));
      new PollFromTopicAction().execute(new PollFromTopicTopicInput().withTopicName(SESSION_UPDATED_TOPIC_NAME));
      assertEquals(List.of("0"), SessionUpdatedTopicReceiver.receivedSessionIds);

      //////////////////////////
      // post 2 more messages //
      //////////////////////////
      new PostToTopicAction().execute(new PostToTopicInput().withTopicMessage(new TopicMessage().withTopicName(SESSION_UPDATED_TOPIC_NAME).withMessage("1")));
      new PostToTopicAction().execute(new PostToTopicInput().withTopicMessage(new TopicMessage().withTopicName(SESSION_UPDATED_TOPIC_NAME).withMessage("2")));

      //////////////////////////////////////////////////
      // confirm not received until poller runs again //
      //////////////////////////////////////////////////
      assertEquals(List.of("0"), SessionUpdatedTopicReceiver.receivedSessionIds);

      /////////////////////////////////////////////////////////////////////
      // re-run poller and assert messages are delivered                 //
      // this demonstrates basic ability to hold messages in topic queue //
      /////////////////////////////////////////////////////////////////////
      new PollFromTopicAction().execute(new PollFromTopicTopicInput().withTopicName(SESSION_UPDATED_TOPIC_NAME));
      assertEquals(List.of("0", "1", "2"), SessionUpdatedTopicReceiver.receivedSessionIds);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static class CustomerNotifierTopicReceiver implements ReceiveFromTopicInterface
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



   /***************************************************************************
    *
    ***************************************************************************/
   public static class WarehouseSystemTopicReceiver implements ReceiveFromTopicInterface
   {
      static List<String> receivedOrderIds = new ArrayList<>();



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public ReceiveFromTopicOutput receiveTopicMessage(ReceiveFromTopicInput input)
      {
         for(TopicMessage topicMessage : input.getTopicMessageList())
         {
            receivedOrderIds.add(topicMessage.getMessage());
         }
         return new ReceiveFromTopicOutput();
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   public static class SessionUpdatedTopicReceiver implements ReceiveFromTopicInterface
   {
      static List<String> receivedSessionIds = new ArrayList<>();



      /***************************************************************************
       *
       ***************************************************************************/
      @Override
      public ReceiveFromTopicOutput receiveTopicMessage(ReceiveFromTopicInput input)
      {
         for(TopicMessage topicMessage : input.getTopicMessageList())
         {
            receivedSessionIds.add(topicMessage.getMessage());
         }
         return new ReceiveFromTopicOutput();
      }
   }

}