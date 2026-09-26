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


import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.esb.model.EsbProviderType;


/*******************************************************************************
 * The broker-side names of queues that brokers make for QQQ - so they can be
 * browsed and managed like any other queue.
 *******************************************************************************/
public final class EsbBrokerNames
{
   /////////////////////////////////////////////////////////////////////
   // between the address and the queue in an Artemis fully qualified //
   // queue name (FQQN), e.g., orderEvents::syncOrder\.orderEvents     //
   /////////////////////////////////////////////////////////////////////
   public static final String ARTEMIS_FQQN_SEPARATOR = "::";



   /*******************************************************************************
    ** Utility class - static methods only.
    *******************************************************************************/
   private EsbBrokerNames()
   {
   }



   /*******************************************************************************
    ** The broker-side name of the queue a broker keeps a shared durable topic
    ** subscription's messages on - to browse it (EsbMessageBrowser) or manage it
    ** (EsbBrokerAdapter) like any other queue:
    **
    ** - Artemis: the fully qualified queue name TOPIC::QUEUE, where QUEUE is the
    **   subscription name with each backslash and dot escaped by a backslash
    **   (e.g., orderEvents::syncOrder\.orderEvents).  The queue is on the
    **   topic's address, and a JMS queue named without its address is taken as
    **   a queue on an address of the same name - so browsing needs the full
    **   name.
    ** - RabbitMQ (JMS client): the subscription name, as is (topicBrokerName
    **   isn't used).
    **
    ** On both brokers the queue itself is named after the subscription alone,
    ** given no JMS client id (the ESB never sets one on its connections; a
    ** clientID in an Artemis url would prefix it) - which is why effective
    ** subscription names must be unique per provider.
    **
    ** Throws IllegalArgumentException without a provider type or subscription
    ** name, or, for Artemis, a topic name.
    *******************************************************************************/
   public static String subscriptionQueue(EsbProviderType providerType, String topicBrokerName, String subscriptionName)
   {
      if(providerType == null)
      {
         throw (new IllegalArgumentException("A provider type is required, to name a subscription queue"));
      }

      if(!StringUtils.hasContent(subscriptionName))
      {
         throw (new IllegalArgumentException("A subscription name is required, to name a subscription queue"));
      }

      return switch(providerType)
      {
         case ACTIVEMQ_ARTEMIS -> artemisSubscriptionQueue(topicBrokerName, subscriptionName);
         case RABBITMQ -> subscriptionName;
      };
   }



   /*******************************************************************************
    ** TOPIC::QUEUE, escaping the subscription name as Artemis's JMS client does
    ** for a shared durable subscription with no client id.
    *******************************************************************************/
   private static String artemisSubscriptionQueue(String topicBrokerName, String subscriptionName)
   {
      if(!StringUtils.hasContent(topicBrokerName))
      {
         throw (new IllegalArgumentException("A topic name is required, to name an Artemis subscription queue"));
      }

      return (topicBrokerName + ARTEMIS_FQQN_SEPARATOR + subscriptionName.replace("\\", "\\\\").replace(".", "\\."));
   }

}
