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
