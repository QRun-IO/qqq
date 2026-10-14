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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import com.kingsrook.qqq.backend.core.logging.QLogger;


/*******************************************************************************
 ** Synchronous fan-out on virtual threads; a failed listener never fails a write.
 *******************************************************************************/
public class InMemoryRealtimeBus implements RealtimeBusInterface, AutoCloseable
{
   private static final QLogger LOG = QLogger.getLogger(InMemoryRealtimeBus.class);
   private final List<Consumer<RealtimeMessage>> listeners = new CopyOnWriteArrayList<>();
   private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();



   /*******************************************************************************
    ** Run each listener independently and await this publication.
    *******************************************************************************/
   @Override
   public void publish(RealtimeMessage message)
   {
      if(executor.isShutdown())
      {
         return;
      }
      List<Future<?>> deliveries = new ArrayList<>();
      for(Consumer<RealtimeMessage> listener : listeners)
      {
         try
         {
            deliveries.add(executor.submit(() ->
            {
               try
               {
                  listener.accept(message);
               }
               catch(Exception | LinkageError e)
               {
                  LOG.warn("Realtime listener failed", e);
               }
            }));
         }
         catch(RuntimeException e)
         {
            LOG.warn("Could not schedule realtime delivery", e);
         }
      }
      for(Future<?> delivery : deliveries)
      {
         try
         {
            delivery.get();
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            return;
         }
         catch(Exception e)
         {
            LOG.warn("Realtime delivery failed", e);
         }
      }
   }



   /*******************************************************************************
    ** The subscription handle removes only this listener.
    *******************************************************************************/
   @Override
   public AutoCloseable subscribe(Consumer<RealtimeMessage> listener)
   {
      listeners.add(listener);
      return (() -> listeners.remove(listener));
   }



   /*******************************************************************************
    ** Release the executor and listeners.
    *******************************************************************************/
   @Override
   public void close()
   {
      listeners.clear();
      executor.shutdown();
   }
}

