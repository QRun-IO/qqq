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


import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Runs work received by a JMS MessageListener on its own thread, in arrival
 ** order, with the instance in QContext.
 **
 ** A JMS listener runs on the client library's thread for the shared provider
 ** connection; Artemis refuses (AMQ139006) to close any session of that
 ** connection from that thread, so anything reached from a listener that opens
 ** and closes its own session (publishing a cluster event or a realtime hint,
 ** restarting a trigger) fails there. That thread also has no QContext, so a
 ** system session built on it has no instance and no all-access keys. Listeners
 ** in this module only parse the message and hand the rest to this.
 **
 ** The queue is bounded: when it is full the work is dropped and logged, which
 ** is safe for the best-effort hints and events delivered this way.
 *******************************************************************************/
public final class EsbListenerDispatcher implements AutoCloseable
{
   private static final QLogger LOG = QLogger.getLogger(EsbListenerDispatcher.class);

   /** how much undelivered work one listener may hold */
   static final int QUEUE_CAPACITY = 10_000;

   private final String             name;
   private final QInstance          instance;
   private final ThreadPoolExecutor executor;



   /*******************************************************************************
    ** One ordered virtual-thread worker named for the listener.
    *******************************************************************************/
   public EsbListenerDispatcher(String name, QInstance instance)
   {
      this.name = name;
      this.instance = instance;
      this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(QUEUE_CAPACITY),
         Thread.ofVirtual().name(name).factory());
   }



   /*******************************************************************************
    ** Queue work; never throws to the JMS listener. Failures are logged.
    *******************************************************************************/
   public void dispatch(Runnable work)
   {
      try
      {
         executor.execute(() -> run(work));
      }
      catch(RejectedExecutionException e)
      {
         if(executor.isShutdown())
         {
            LOG.debug("Dropping ESB listener work after close", logPair("listener", name));
         }
         else
         {
            LOG.warn("Dropping ESB listener work: the queue is full", logPair("listener", name), logPair("capacity", QUEUE_CAPACITY));
         }
      }
   }



   /*******************************************************************************
    ** Run one item in the instance's context, isolated from the next.
    *******************************************************************************/
   private void run(Runnable work)
   {
      try
      {
         QContext.withTemporaryContext(new CapturedContext(instance, null), work::run);
      }
      catch(Throwable t)
      {
         LOG.warn("ESB listener work failed", t, logPair("listener", name));
      }
   }



   /*******************************************************************************
    ** Stop taking work and let queued work finish briefly.
    *******************************************************************************/
   @Override
   public void close()
   {
      executor.shutdown();
      try
      {
         if(!executor.awaitTermination(2, TimeUnit.SECONDS))
         {
            executor.shutdownNow();
         }
      }
      catch(InterruptedException e)
      {
         executor.shutdownNow();
         Thread.currentThread().interrupt();
      }
   }
}
