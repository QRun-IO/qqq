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

package com.kingsrook.qqq.esb.management;


import com.kingsrook.qqq.esb.model.EsbProviderType;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Unit test for EsbBrokerNames.  (EsbMessageBrowserTest also browses an
 ** Artemis subscription queue by its resolved name, on the embedded broker; the
 ** conformance suite checks both brokers for real.)
 *******************************************************************************/
class EsbBrokerNamesTest
{

   /*******************************************************************************
    ** Artemis names a shared durable subscription's queue after the subscription,
    ** with its dots and backslashes backslash-escaped, on the topic's address -
    ** so its fully qualified name is TOPIC::QUEUE.
    *******************************************************************************/
   @Test
   void testArtemisSubscriptionQueue()
   {
      assertThat(EsbBrokerNames.subscriptionQueue(EsbProviderType.ACTIVEMQ_ARTEMIS, "orderEvents", "syncOrder.orderEvents")).isEqualTo("orderEvents::syncOrder\\.orderEvents");
      assertThat(EsbBrokerNames.subscriptionQueue(EsbProviderType.ACTIVEMQ_ARTEMIS, "orderEvents", "back\\slash")).isEqualTo("orderEvents::back\\\\slash");
      assertThat(EsbBrokerNames.subscriptionQueue(EsbProviderType.ACTIVEMQ_ARTEMIS, "orderEvents", "plainName")).isEqualTo("orderEvents::plainName");
   }



   /*******************************************************************************
    ** RabbitMQ names a durable subscription's queue exactly after the
    ** subscription.
    *******************************************************************************/
   @Test
   void testRabbitSubscriptionQueue()
   {
      assertThat(EsbBrokerNames.subscriptionQueue(EsbProviderType.RABBITMQ, "orderEvents", "syncOrder.orderEvents")).isEqualTo("syncOrder.orderEvents");
      assertThat(EsbBrokerNames.subscriptionQueue(EsbProviderType.RABBITMQ, "orderEvents", "back\\slash")).isEqualTo("back\\slash");
      assertThat(EsbBrokerNames.subscriptionQueue(EsbProviderType.RABBITMQ, null, "syncOrder.orderEvents")).isEqualTo("syncOrder.orderEvents");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMissingArgumentsThrow()
   {
      assertThatThrownBy(() -> EsbBrokerNames.subscriptionQueue(null, "orderEvents", "syncOrder.orderEvents")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> EsbBrokerNames.subscriptionQueue(EsbProviderType.RABBITMQ, "orderEvents", " ")).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> EsbBrokerNames.subscriptionQueue(EsbProviderType.ACTIVEMQ_ARTEMIS, "orderEvents", null)).isInstanceOf(IllegalArgumentException.class);
      assertThatThrownBy(() -> EsbBrokerNames.subscriptionQueue(EsbProviderType.ACTIVEMQ_ARTEMIS, " ", "syncOrder.orderEvents")).isInstanceOf(IllegalArgumentException.class);
   }

}
