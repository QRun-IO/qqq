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


package com.kingsrook.qqq.esb.cluster;


import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.cluster.ClusterEvent;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMessage;
import com.kingsrook.qqq.esb.EsbTestBase;
import com.kingsrook.qqq.esb.realtime.EsbRealtimeBus;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;


/*******************************************************************************
 ** Two "nodes" (a cluster bus and a realtime bus each) on the embedded broker,
 ** sharing one provider connection as the buses in a pod do.
 *******************************************************************************/
class EsbClusterBusTest extends EsbTestBase
{
   /*******************************************************************************
    ** A cluster event received on node B whose handler publishes a realtime hint
    ** (as RuntimeMetaDataOverlay does): the hint reaches node A, and the session
    ** the publish opened is closed. The handler used to run on the JMS listener
    ** thread, where Artemis refuses the session close (AMQ139006): the hint went
    ** out but every one leaked a broker session. It now also runs with the
    ** instance in QContext.
    *******************************************************************************/
   @Test
   void handlerOnAnotherNodeCanPublishARealtimeHint() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      try(EsbClusterBus clusterA = new EsbClusterBus(instance);
         EsbClusterBus clusterB = new EsbClusterBus(instance);
         EsbRealtimeBus realtimeA = new EsbRealtimeBus(instance);
         EsbRealtimeBus realtimeB = new EsbRealtimeBus(instance))
      {
         LinkedBlockingQueue<RealtimeMessage> hintsOnA = new LinkedBlockingQueue<>();
         realtimeA.subscribe(hintsOnA::add);
         AtomicReference<QInstance> handlerInstance = new AtomicReference<>();
         clusterB.subscribe(event ->
         {
            handlerInstance.set(QContext.getQInstance());
            realtimeB.publish(new RealtimeMessage().withChannel("instance").withEvent("metadata.changed"));
         });
         awaitListening(clusterA, clusterB, realtimeA, realtimeB);
         int sessionsBefore = getEmbeddedBrokerServer().getSessions().size();

         clusterA.publish(new ClusterEvent("cache", "overlay", "all"));

         RealtimeMessage hint = hintsOnA.poll(10, TimeUnit.SECONDS);
         assertNotNull(hint, "node B's handler published a hint that reached node A");
         assertEquals("metadata.changed", hint.getEvent());
         assertTimeoutPreemptively(Duration.ofSeconds(5), () ->
         {
            while(getEmbeddedBrokerServer().getSessions().size() > sessionsBefore)
            {
               Thread.sleep(20);
            }
         }, "the publishing sessions are closed");
         assertSame(instance, handlerInstance.get(), "handlers run with the instance in context");
      }
   }



   /*******************************************************************************
    ** Events arrive in the order they were published.
    *******************************************************************************/
   @Test
   void eventsAreDeliveredInOrder() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      try(EsbClusterBus sender = new EsbClusterBus(instance); EsbClusterBus receiver = new EsbClusterBus(instance))
      {
         LinkedBlockingQueue<String> keys = new LinkedBlockingQueue<>();
         receiver.subscribe(event -> keys.add(event.getKey()));
         awaitListening(sender, receiver);
         for(int i = 0; i < 50; i++)
         {
            sender.publish(new ClusterEvent("cache", "ordered", Integer.toString(i)));
         }
         for(int i = 0; i < 50; i++)
         {
            assertEquals(Integer.toString(i), keys.poll(10, TimeUnit.SECONDS));
         }
         assertNull(keys.poll(200, TimeUnit.MILLISECONDS));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void awaitListening(Object... buses)
   {
      assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
      {
         for(Object bus : buses)
         {
            while(!(bus instanceof EsbClusterBus cluster ? cluster.isListening() : ((EsbRealtimeBus) bus).isListening()))
            {
               Thread.sleep(20);
            }
         }
      });
   }
}
