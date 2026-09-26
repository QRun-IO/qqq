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

package com.kingsrook.qqq.esb.conformance;


import com.kingsrook.qqq.esb.model.EsbProviderType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;


/*******************************************************************************
 * RabbitMQ 4 JMS and HTTP management conformance.
 ******************************************************************************/
class RabbitMQConformanceIT extends AbstractEsbConformanceTest
{
   private static final String USERNAME = "esbtest";
   private static final String PASSWORD = "esbtest";

   private static final GenericContainer<?> BROKER = new GenericContainer<>(DockerImageName.parse("rabbitmq:4-management"))
      .withEnv("RABBITMQ_DEFAULT_USER", USERNAME)
      .withEnv("RABBITMQ_DEFAULT_PASS", PASSWORD)
      .withExposedPorts(5672, 15672)
      .waitingFor(Wait.forHttp("/api/overview").forPort(15672).withBasicCredentials(USERNAME, PASSWORD));



   /** Start the broker container once for the conformance class. */
   @BeforeAll
   static void startContainer()
   {
      BROKER.start();
   }



   /** Stop the broker container after all conformance cases. */
   @AfterAll
   static void stopContainer()
   {
      BROKER.stop();
   }



   /** The running Testcontainers broker. */
   @Override
   protected GenericContainer<?> broker()
   {
      return (BROKER);
   }



   /** The provider type for this broker. */
   @Override
   protected EsbProviderType providerType()
   {
      return (EsbProviderType.RABBITMQ);
   }



   /** The mapped JMS or AMQP connection URL. */
   @Override
   protected String brokerUrl()
   {
      return ("amqp://" + BROKER.getHost() + ":" + BROKER.getMappedPort(5672) + "/%2F");
   }



   /** The mapped broker management URL. */
   @Override
   protected String managementUrl()
   {
      return ("http://" + BROKER.getHost() + ":" + BROKER.getMappedPort(15672));
   }



   /** The broker test username. */
   @Override
   protected String brokerUsername()
   {
      return (USERNAME);
   }



   /** The broker test password. */
   @Override
   protected String brokerPassword()
   {
      return (PASSWORD);
   }

}
