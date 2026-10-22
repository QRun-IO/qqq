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

package com.kingsrook.qqq.backend.core.realtime;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Publication is synchronous, virtual-thread based, and isolates listeners.
 *******************************************************************************/
class InMemoryRealtimeBusTest
{
   /*******************************************************************************
    ** A bad listener does not stop the next one; closing removes a subscription.
    *******************************************************************************/
   @Test
   void fanOutAndUnsubscribe() throws Exception
   {
      try(InMemoryRealtimeBus bus = new InMemoryRealtimeBus())
      {
         List<String> received = new ArrayList<>();
         AtomicBoolean virtual = new AtomicBoolean();
         bus.subscribe(message ->
         {
            throw new IllegalStateException("test listener");
         });
         AutoCloseable subscription = bus.subscribe(message ->
         {
            virtual.set(Thread.currentThread().isVirtual());
            received.add(message.getEvent());
         });
         bus.publish(new RealtimeMessage().withChannel("instance").withEvent("test"));
         assertEquals(List.of("test"), received);
         assertTrue(virtual.get());
         subscription.close();
         bus.publish(new RealtimeMessage().withChannel("instance").withEvent("ignored"));
         assertEquals(1, received.size());
      }
   }
}

