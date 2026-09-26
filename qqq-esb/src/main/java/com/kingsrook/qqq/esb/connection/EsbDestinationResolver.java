/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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
