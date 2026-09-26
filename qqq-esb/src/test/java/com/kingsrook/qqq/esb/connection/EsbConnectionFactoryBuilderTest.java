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


import java.util.Map;
import com.kingsrook.qqq.esb.model.QEsbDestinationMetaData;
import com.kingsrook.qqq.esb.model.QEsbProviderMetaData;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import com.rabbitmq.jms.admin.RMQDestination;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.InvalidDestinationException;
import jakarta.jms.Queue;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.jms.client.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for the connection factory builders and EsbDestinationResolver
 ** (no broker needed).
 *******************************************************************************/
class EsbConnectionFactoryBuilderTest
{

   /*******************************************************************************
    ** Artemis: url and credentials go to the factory, and the connect timeout is
    ** 5 s - unless the url sets its own.
    *******************************************************************************/
   @Test
   void testArtemisBuilder() throws Exception
   {
      ArtemisConnectionFactoryBuilder builder = new ArtemisConnectionFactoryBuilder();

      ConnectionFactory connectionFactory = builder.buildConnectionFactory(new QEsbProviderMetaData()
         .withUrl("tcp://broker.example:61617")
         .withUsername("esbUser")
         .withPassword("esbPassword"));
      try(ActiveMQConnectionFactory artemisConnectionFactory = (ActiveMQConnectionFactory) connectionFactory)
      {
         assertThat(artemisConnectionFactory.getUser()).isEqualTo("esbUser");
         assertThat(artemisConnectionFactory.getPassword()).isEqualTo("esbPassword");

         TransportConfiguration connector = artemisConnectionFactory.getStaticConnectors()[0];
         assertThat(connector.getParams()).containsEntry(TransportConstants.HOST_PROP_NAME, "broker.example");
         assertThat(connector.getParams()).containsEntry(TransportConstants.NETTY_CONNECT_TIMEOUT, EsbConnectionFactoryBuilder.CONNECT_TIMEOUT_MS);
      }

      ConnectionFactory ownTimeoutConnectionFactory = builder.buildConnectionFactory(new QEsbProviderMetaData()
         .withUrl("tcp://broker.example:61617?" + TransportConstants.NETTY_CONNECT_TIMEOUT + "=1234"));
      try(ActiveMQConnectionFactory artemisConnectionFactory = (ActiveMQConnectionFactory) ownTimeoutConnectionFactory)
      {
         assertThat(artemisConnectionFactory.getStaticConnectors()[0].getParams()).containsEntry(TransportConstants.NETTY_CONNECT_TIMEOUT, "1234");
      }
   }



   /*******************************************************************************
    ** RabbitMQ: the url is an AMQP URI; username and password, when given,
    ** override any in the URI.
    *******************************************************************************/
   @Test
   void testRabbitBuilderConnectionFactory() throws Exception
   {
      RabbitConnectionFactoryBuilder builder = new RabbitConnectionFactoryBuilder();

      RMQConnectionFactory connectionFactory = (RMQConnectionFactory) builder.buildConnectionFactory(new QEsbProviderMetaData()
         .withUrl("amqp://uriUser:uriPassword@broker.example:5673/esbVhost")
         .withUsername("esbUser")
         .withPassword("esbPassword"));
      assertThat(connectionFactory.getHost()).isEqualTo("broker.example");
      assertThat(connectionFactory.getPort()).isEqualTo(5673);
      assertThat(connectionFactory.getVirtualHost()).isEqualTo("esbVhost");
      assertThat(connectionFactory.getUsername()).isEqualTo("esbUser");
      assertThat(connectionFactory.getPassword()).isEqualTo("esbPassword");

      RMQConnectionFactory uriCredentialsConnectionFactory = (RMQConnectionFactory) builder.buildConnectionFactory(new QEsbProviderMetaData()
         .withUrl("amqp://uriUser:uriPassword@broker.example:5673/esbVhost"));
      assertThat(uriCredentialsConnectionFactory.getUsername()).isEqualTo("uriUser");
      assertThat(uriCredentialsConnectionFactory.getPassword()).isEqualTo("uriPassword");
   }



   /*******************************************************************************
    ** RabbitMQ queues are RMQDestinations declared as quorum queues.
    *******************************************************************************/
   @Test
   void testRabbitBuilderQueuesAreQuorumQueues() throws Exception
   {
      Queue queue = new RabbitConnectionFactoryBuilder().createQueue(null, "esb.test.orders");
      assertThat(queue).isInstanceOf(RMQDestination.class);
      assertThat(queue.getQueueName()).isEqualTo("esb.test.orders");

      RMQDestination rmqDestination = (RMQDestination) queue;
      assertThat(rmqDestination.isQueue()).isTrue();
      assertThat(rmqDestination.isTemporary()).isFalse();
      assertThat(rmqDestination.getQueueDeclareArguments()).isEqualTo(Map.of("x-queue-type", "quorum"));
   }



   /*******************************************************************************
    ** A destination with no type can't be resolved.
    *******************************************************************************/
   @Test
   void testResolverRejectsDestinationWithoutType()
   {
      EsbDestinationResolver resolver = new EsbDestinationResolver(new ArtemisConnectionFactoryBuilder());
      assertThatThrownBy(() -> resolver.resolve(null, new QEsbDestinationMetaData().withName("untyped")))
         .isInstanceOf(InvalidDestinationException.class)
         .hasMessageContaining("untyped");
   }

}
