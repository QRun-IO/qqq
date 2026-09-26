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


import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import jakarta.jms.ConnectionFactory;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;


/*******************************************************************************
 * Connection factory builder for ActiveMQ Artemis (Jakarta client).
 *
 * The provider url is an Artemis url, e.g., tcp://host:61616.  The client's own
 * reconnect is left off (its default), so a lost connection reaches the JMS
 * ExceptionListener, and EsbConnectionManager reconnects.
 *******************************************************************************/
public class ArtemisConnectionFactoryBuilder implements EsbConnectionFactoryBuilder
{

   /*******************************************************************************
    ** Build an ActiveMQConnectionFactory with a 5 s connect timeout - unless the
    ** url sets its own connect-timeout-millis.
    *******************************************************************************/
   @Override
   public ConnectionFactory buildConnectionFactory(QEsbProviderMetaData provider)
   {
      ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(provider.getUrl(), provider.getUsername(), provider.getPassword());

      TransportConfiguration[] connectors = connectionFactory.getStaticConnectors();
      if(connectors != null)
      {
         for(TransportConfiguration connector : connectors)
         {
            connector.getParams().putIfAbsent(TransportConstants.NETTY_CONNECT_TIMEOUT, CONNECT_TIMEOUT_MS);
         }
      }

      return (connectionFactory);
   }

}
