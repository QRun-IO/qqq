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


import java.util.Locale;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQConnection;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;


/*******************************************************************************
 * Connection factory builder for ActiveMQ Artemis (Jakarta client).
 *
 * The provider url is an Artemis url, passed to the client untouched - so the
 * same setting runs either mode:
 *
 * - Single broker, e.g., tcp://host:61616.  The client's own reconnect is left
 *   off (its default), so a lost connection reaches the JMS ExceptionListener,
 *   and EsbConnectionManager reconnects (and the triggers re-subscribe).
 * - HA (a live/backup pair, or a cluster), e.g.,
 *   (tcp://a:61616,tcp://b:61616)?ha=true&reconnectAttempts=-1&retryInterval=1000
 *   - the client connects to the first broker that answers, and fails over
 *   (to the backup, or back to the restarted live) by itself, keeping the
 *   connection; a transacted commit cut by a failover throws, so the trigger
 *   rolls back, rebuilds its consumer, and the message is redelivered (and
 *   deduped, with EsbDedupeConfig).  If the client gives up (a finite
 *   reconnectAttempts), EsbConnectionManager takes over as for one broker.
 *
 * Defaults, unless the url sets them: a 5 s connect timeout on each connector,
 * and a 30 s callFailoverTimeout - the client's own default (-1) would block
 * a send or commit for as long as a failover takes, which with
 * reconnectAttempts=-1 and every broker down is forever.
 *******************************************************************************/
public class ArtemisConnectionFactoryBuilder implements EsbConnectionFactoryBuilder
{
   static final Long CALL_FAILOVER_TIMEOUT_MS = 30_000L;

   /*******************************************************************************
    ** Build an ActiveMQConnectionFactory from the url (single or HA), with the
    ** defaults in the class comment.
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

      String url = provider.getUrl() == null ? "" : provider.getUrl();
      if(!url.toLowerCase(Locale.ROOT).contains("callfailovertimeout"))
      {
         connectionFactory.setCallFailoverTimeout(CALL_FAILOVER_TIMEOUT_MS);
      }

      return (connectionFactory);
   }




   /*******************************************************************************
    ** The Artemis client reports a completed failover (HA mode) to the
    ** ExceptionListener with the error code FAILOVER.
    *******************************************************************************/
   @Override
   public boolean isFailover(JMSException exception)
   {
      return (exception != null && ActiveMQConnection.EXCEPTION_FAILOVER.equals(exception.getErrorCode()));
   }

}
