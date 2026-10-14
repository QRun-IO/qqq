/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.esb.realtime;


import java.time.Duration;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMessage;
import com.kingsrook.qqq.esb.EsbTestBase;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;


/*******************************************************************************
 ** Embedded Artemis tests the same non-shared topic semantics used by separate pods.
 *******************************************************************************/
class EsbRealtimeBusTest extends EsbTestBase
{
   /*******************************************************************************
    ** Every live pod receives the hint; a later pod has no durable backlog.
    *******************************************************************************/
   @Test
   void broadcastsToEveryPodWithoutDurableReplay() throws Exception
   {
      try(EsbRealtimeBus first = new EsbRealtimeBus(QContext.getQInstance());
         EsbRealtimeBus second = new EsbRealtimeBus(QContext.getQInstance()))
      {
         LinkedBlockingQueue<RealtimeMessage> firstEvents = new LinkedBlockingQueue<>();
         LinkedBlockingQueue<RealtimeMessage> secondEvents = new LinkedBlockingQueue<>();
         first.subscribe(firstEvents::add);
         second.subscribe(secondEvents::add);
         assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
         {
            while(!first.isListening() || !second.isListening())
            {
               Thread.sleep(20);
            }
         });
         first.publish(new RealtimeMessage().withChannel("instance").withEvent("test.changed"));
         assertEquals("test.changed", firstEvents.poll(5, TimeUnit.SECONDS).getEvent());
         assertEquals("test.changed", secondEvents.poll(5, TimeUnit.SECONDS).getEvent());
      }
      try(EsbRealtimeBus later = new EsbRealtimeBus(QContext.getQInstance()))
      {
         LinkedBlockingQueue<RealtimeMessage> events = new LinkedBlockingQueue<>();
         later.subscribe(events::add);
         assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
         {
            while(!later.isListening())
            {
               Thread.sleep(20);
            }
         });
         assertNull(events.poll(200, TimeUnit.MILLISECONDS));
      }
   }



   /*******************************************************************************
    ** Broker unavailability never propagates back to a record writer.
    *******************************************************************************/
   @Test
   void publishFailureIsBestEffort() throws Exception
   {
      try(EsbRealtimeBus bus = new EsbRealtimeBus(QContext.getQInstance()))
      {
         assertTimeoutPreemptively(Duration.ofSeconds(10), () ->
         {
            while(!bus.isListening())
            {
               Thread.sleep(20);
            }
         });
         stopEmbeddedBroker();
         assertDoesNotThrow(() -> bus.publish(new RealtimeMessage().withChannel("instance").withEvent("offline")));
      }
      finally
      {
         com.kingsrook.qqq.esb.connection.EsbConnectionManager.getInstance().closeAll();
         startEmbeddedBroker();
      }
   }
}

