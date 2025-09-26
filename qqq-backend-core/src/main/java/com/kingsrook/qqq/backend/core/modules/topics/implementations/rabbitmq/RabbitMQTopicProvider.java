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


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.kingsrook.qqq.backend.core.actions.topics.TopicReceiverUtils;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.topics.PostToTopicInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.PostToTopicOutput;
import com.kingsrook.qqq.backend.core.model.actions.topics.PostToTopicSingleOutput;
import com.kingsrook.qqq.backend.core.model.actions.topics.SubscribeToTopicInput;
import com.kingsrook.qqq.backend.core.model.actions.topics.SubscribeToTopicOutput;
import com.kingsrook.qqq.backend.core.model.actions.topics.TopicMessage;
import com.kingsrook.qqq.backend.core.model.metadata.topics.QTopicMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.topics.TopicType;
import com.kingsrook.qqq.backend.core.model.metadata.topics.implementations.rabbitmq.RabbitMQTopicProviderMetaData;
import com.kingsrook.qqq.backend.core.modules.topics.TopicProviderInterface;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * Topic module implementation for RabbitMQ.
 *******************************************************************************/
public class RabbitMQTopicProvider implements TopicProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(RabbitMQTopicProvider.class);

   private static Map<String, Connection> subscriberConnectionMap = new ConcurrentHashMap<>();
   private static Map<String, Channel>    subscriberChannelMap    = new ConcurrentHashMap<>();



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public TopicType getType()
   {
      return (TopicType.RABBIT_MQ);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private Connection openConnection(String providerName) throws Exception
   {
      RabbitMQTopicProviderMetaData rabbitMQTopicProviderMetaData = (RabbitMQTopicProviderMetaData) QContext.getQInstance().getTopicProvider(providerName);

      ConnectionFactory factory = new ConnectionFactory();
      factory.setHost(rabbitMQTopicProviderMetaData.getHost());
      factory.setPort(rabbitMQTopicProviderMetaData.getPort());
      factory.setUsername(rabbitMQTopicProviderMetaData.getUsername());
      factory.setPassword(rabbitMQTopicProviderMetaData.getPassword());

      return factory.newConnection();
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

      if(!CollectionUtils.nullSafeHasContents(input.getTopicMessageList()))
      {
         return new PostToTopicOutput();
      }

      PostToTopicOutput.Builder outputBuilder = new PostToTopicOutput.Builder(input);

      TopicMessage   firstTopicMessage = input.getTopicMessageList().get(0);
      QTopicMetaData topic             = QContext.getQInstance().getTopic(firstTopicMessage.getTopicName());
      try(
         Connection connection = openConnection(topic.getProviderName());
         Channel channel = connection.createChannel();
      )
      {
         for(TopicMessage topicMessage : input.getTopicMessageList())
         {
            String exchangeName = exchangeDeclare(topic, channel);
            channel.basicPublish(exchangeName, "", null, topicMessage.getMessage().getBytes());
            outputBuilder.add(new PostToTopicSingleOutput(topicMessage));
         }
      }
      catch(Exception e)
      {
         LOG.warn("Error posting topic", e, logPair("topicName", () -> topic.getName()));
      }

      return (outputBuilder.build());
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private static String exchangeDeclare(QTopicMetaData topic, Channel channel) throws IOException
   {
      String exchangeName = topic.getTopicName();
      channel.exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT, true);
      return exchangeName;
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public SubscribeToTopicOutput subscribe(SubscribeToTopicInput input) throws QException
   {
      QTopicMetaData topic = QContext.getQInstance().getTopic(input.getTopicName());

      try
      {
         Connection connection = openConnection(topic.getProviderName());
         Channel    channel    = connection.createChannel();

         subscriberConnectionMap.put(topic.getProviderName(), connection);
         subscriberChannelMap.put(topic.getTopicName(), channel);

         String exchangeName = exchangeDeclare(topic, channel);
         String queueName    = channel.queueDeclare().getQueue();
         channel.queueBind(queueName, exchangeName, "");

         DeliverCallback deliverCallback = (consumerTag, delivery) ->
         {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

            TopicMessage topicMessage = new TopicMessage().withMessage(message).withTopicName(topic.getTopicName());

            try
            {
               TopicReceiverUtils.processMessagesThroughSubscriber(input.getTopicName(), input.getSubscriberMetaData(), List.of(topicMessage));
            }
            catch(Exception e)
            {
               LOG.info("Exception processing topic message", e, logPair("topicName", () -> topic.getName()), logPair("subscriberName", () -> input.getSubscriberMetaData().getName()));
            }
         };
         channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});

         LOG.info("Subscribed to topic", logPair("topicName", () -> topic.getName()), logPair("subscriberName", () -> input.getSubscriberMetaData().getName()), logPair("queueName", queueName), logPair("exchangeName", exchangeName));

         return new SubscribeToTopicOutput();
      }
      catch(Exception e)
      {
         throw new QException("Error subscribing to topic " + topic.getName(), e);
      }
   }

}

