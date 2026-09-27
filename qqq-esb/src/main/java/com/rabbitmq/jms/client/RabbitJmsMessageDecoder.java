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

package com.rabbitmq.jms.client;


import java.util.List;
import jakarta.jms.JMSException;
import jakarta.jms.Message;


/**
 * Bridge to the RabbitMQ JMS 3.9 wire decoder, which is package-private.
 * Selected dead-letter replay uses native AMQP acknowledgements because this
 * JMS client does not support queue selectors.
 */
public final class RabbitJmsMessageDecoder
{
   /** No instances. */
   private RabbitJmsMessageDecoder()
   {
   }


   /** Decode the RabbitMQ JMS wire body as a JMS message. */
   public static Message decode(byte[] body) throws JMSException
   {
      return (RMQMessage.fromMessage(body, List.of("com.rabbitmq.jms.admin", "java.util")));
   }
}
