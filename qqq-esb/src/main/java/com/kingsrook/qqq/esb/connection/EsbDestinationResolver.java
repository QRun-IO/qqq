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


import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import jakarta.jms.Destination;
import jakarta.jms.InvalidDestinationException;
import jakarta.jms.JMSException;
import jakarta.jms.Queue;
import jakarta.jms.Session;


/*******************************************************************************
 * Turns ESB destination meta-data (or a broker-side queue name) into a JMS
 * Destination, in the flavor of one provider's broker.
 *
 * Destinations are named by their broker-side names (the meta-data's
 * getEffectiveDestinationName).  The broker-specific part is delegated to the
 * provider's EsbConnectionFactoryBuilder (e.g., RabbitMQ quorum queues).
 *******************************************************************************/
public class EsbDestinationResolver
{
   private final EsbConnectionFactoryBuilder connectionFactoryBuilder;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public EsbDestinationResolver(EsbConnectionFactoryBuilder connectionFactoryBuilder)
   {
      this.connectionFactoryBuilder = connectionFactoryBuilder;
   }



   /*******************************************************************************
    ** A Queue or Topic (per the destination's type) for a destination.
    *******************************************************************************/
   public Destination resolve(Session session, QEsbDestinationMetaData destination) throws JMSException
   {
      if(destination.getType() == null)
      {
         throw (new InvalidDestinationException("ESB destination " + destination.getName() + " has no type"));
      }

      String brokerName = destination.getEffectiveDestinationName();
      return switch(destination.getType())
      {
         case QUEUE -> connectionFactoryBuilder.createQueue(session, brokerName);
         case TOPIC -> connectionFactoryBuilder.createTopic(session, brokerName);
      };
   }



   /*******************************************************************************
    ** A Queue for a broker-side queue name - e.g., a dead-letter queue.
    *******************************************************************************/
   public Queue resolveQueue(Session session, String brokerQueueName) throws JMSException
   {
      return (connectionFactoryBuilder.createQueue(session, brokerQueueName));
   }

}
