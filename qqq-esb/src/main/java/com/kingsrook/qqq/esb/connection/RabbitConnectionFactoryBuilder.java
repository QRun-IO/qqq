/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kingsrook.qqq.esb.connection;


import java.util.Map;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import com.rabbitmq.jms.admin.RMQDestination;
import com.rabbitmq.jms.client.RMQSession;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Session;


/*******************************************************************************
 * Connection factory builder for RabbitMQ (4.x), via the RabbitMQ JMS client.
 *
 * The provider url is an AMQP URI (host, port, and vhost); username and
 * password, when given, override any in the URI.
 *
 * Queues, and the queues behind durable topic subscriptions, are declared as
 * quorum queues - they are what gives RabbitMQ messages a delivery count.
 *
 * The AMQP client's own automatic recovery is turned off, so a lost connection
 * reaches the JMS ExceptionListener, and EsbConnectionManager reconnects (the
 * JMS sessions and consumers would not survive an AMQP-level recovery anyway).
 *******************************************************************************/
public class RabbitConnectionFactoryBuilder implements EsbConnectionFactoryBuilder
{
   public static final Map<String, Object> QUORUM_QUEUE_ARGUMENTS = Map.of("x-queue-type", "quorum");



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ConnectionFactory buildConnectionFactory(QEsbProviderMetaData provider) throws JMSException
   {
      RMQConnectionFactory connectionFactory = new RMQConnectionFactory();
      connectionFactory.setUri(provider.getUrl());

      if(StringUtils.hasContent(provider.getUsername()))
      {
         connectionFactory.setUsername(provider.getUsername());
      }

      if(StringUtils.hasContent(provider.getPassword()))
      {
         connectionFactory.setPassword(provider.getPassword());
      }

      connectionFactory.setAmqpConnectionFactoryPostProcessor(amqpConnectionFactory ->
      {
         amqpConnectionFactory.setConnectionTimeout(CONNECT_TIMEOUT_MS);
         amqpConnectionFactory.setAutomaticRecoveryEnabled(false);
      });

      return (connectionFactory);
   }



   /*******************************************************************************
    ** Declare the queues behind this session's durable subscriptions as quorum
    ** queues.
    *******************************************************************************/
   @Override
   public void configureSession(Session session)
   {
      ((RMQSession) session).setQueueDeclareArguments(QUORUM_QUEUE_ARGUMENTS);
   }



   /*******************************************************************************
    ** A (durable, quorum) queue.
    *******************************************************************************/
   @Override
   public Queue createQueue(Session session, String brokerQueueName)
   {
      return (new RMQDestination(brokerQueueName, true, false, QUORUM_QUEUE_ARGUMENTS));
   }

}
