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


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.esb.model.EsbProviderType;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.Topic;


/*******************************************************************************
 * Broker-specific JMS setup for one EsbProviderType: builds the provider's
 * ConnectionFactory, and makes the broker's flavor of sessions and queues.
 *
 * Each app adds only its own broker's client jar, so implementations are
 * loaded by class name (see forProviderType): a missing client jar fails only
 * the providers of that type, with a message naming the missing class.  The
 * broker client classes are referenced only from the implementations, never
 * from this interface or the connection manager.
 *******************************************************************************/
public interface EsbConnectionFactoryBuilder
{
   /*******************************************************************************
    ** How long to wait for a TCP connection to a broker, in milliseconds.
    *******************************************************************************/
   Integer CONNECT_TIMEOUT_MS = 5000;



   /*******************************************************************************
    ** Build a ConnectionFactory for a provider, from its url and credentials.
    *******************************************************************************/
   ConnectionFactory buildConnectionFactory(QEsbProviderMetaData provider) throws JMSException;



   /*******************************************************************************
    ** Apply broker-specific settings to a newly opened session.
    *******************************************************************************/
   default void configureSession(Session session) throws JMSException
   {
      /////////////////////////////////////
      // nothing to do, for most brokers //
      /////////////////////////////////////
   }



   /*******************************************************************************
    ** Make a Queue for a broker-side queue name.
    *******************************************************************************/
   default Queue createQueue(Session session, String brokerQueueName) throws JMSException
   {
      return (session.createQueue(brokerQueueName));
   }



   /*******************************************************************************
    ** Make a Topic for a broker-side topic name.
    *******************************************************************************/
   default Topic createTopic(Session session, String brokerTopicName) throws JMSException
   {
      return (session.createTopic(brokerTopicName));
   }



   /*******************************************************************************
    ** Load the builder for a provider type, through the given class loader.
    **
    ** Throws QException "Broker client for TYPE not on classpath: CLASS" if the
    ** type's client jar is missing.
    *******************************************************************************/
   static EsbConnectionFactoryBuilder forProviderType(EsbProviderType providerType, ClassLoader classLoader) throws QException
   {
      return switch(providerType)
      {
         case ACTIVEMQ_ARTEMIS -> load(providerType, "org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory", "com.kingsrook.qqq.esb.connection.ArtemisConnectionFactoryBuilder", classLoader);
         case RABBITMQ -> load(providerType, "com.rabbitmq.jms.admin.RMQConnectionFactory", "com.kingsrook.qqq.esb.connection.RabbitConnectionFactoryBuilder", classLoader);
      };
   }



   /*******************************************************************************
    ** Check that the broker client class is there, then instantiate the builder.
    *******************************************************************************/
   private static EsbConnectionFactoryBuilder load(EsbProviderType providerType, String clientClassName, String builderClassName, ClassLoader classLoader) throws QException
   {
      try
      {
         Class.forName(clientClassName, false, classLoader);
      }
      catch(ClassNotFoundException | LinkageError e)
      {
         throw (new QException("Broker client for " + providerType + " not on classpath: " + clientClassName, e));
      }

      try
      {
         return ((EsbConnectionFactoryBuilder) Class.forName(builderClassName, true, classLoader).getConstructor().newInstance());
      }
      catch(Exception | LinkageError e)
      {
         throw (new QException("Could not load the ESB connection factory builder for " + providerType + ": " + builderClassName, e));
      }
   }

}
