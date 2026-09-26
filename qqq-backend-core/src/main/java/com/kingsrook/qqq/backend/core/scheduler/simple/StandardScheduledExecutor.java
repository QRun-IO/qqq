/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.scheduler.simple;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.PrefixedDefaultThreadFactory;


/*******************************************************************************
 ** Standard class ran by SimpleScheduler.  Takes a Runnable in its constructor -
 ** that's the code that actually executes.  Internally, this class will launch
 ** a newSingleThreadScheduledExecutor / ScheduledExecutorService to run the
 ** runnable on a repeating delay.
 **
 *******************************************************************************/
public class StandardScheduledExecutor
{
   private static final QLogger LOG = QLogger.getLogger(StandardScheduledExecutor.class);

   private Integer initialDelayMillis = 3000;
   private Integer delayMillis        = 1000;

   protected QInstance          qInstance;
   protected String             name;
   protected Supplier<QSession> sessionSupplier;

   private RunningState             runningState = RunningState.STOPPED;
   private ScheduledExecutorService service;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public StandardScheduledExecutor(Runnable runnable)
   {
      this.runnable = runnable;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Runnable runnable;



   /*******************************************************************************
    **
    *******************************************************************************/
   public Runnable getRunnable()
   {
      return (runnable);
   }



   /*******************************************************************************
    **
    ** @return true iff the schedule was started
    *******************************************************************************/
   public boolean start()
   {
      if(!runningState.equals(RunningState.STOPPED))
      {
         LOG.warn("Request to start from an invalid running state [" + runningState + "].  Must be STOPPED.");
         return (false);
      }

      LOG.info("Starting [" + name + "]");
      service = Executors.newSingleThreadScheduledExecutor(new PrefixedDefaultThreadFactory(this));
      service.scheduleWithFixedDelay(getRunnable(), initialDelayMillis, delayMillis, TimeUnit.MILLISECONDS);
      runningState = RunningState.RUNNING;
      return (true);
   }



   /*******************************************************************************
    ** Stop, and don't wait to check if it worked or anything
    *******************************************************************************/
   public void stopAsync()
   {
      new Thread(this::stop).start();
   }



   /*******************************************************************************
    ** Issue a stop, and wait (a while) for it to succeed.
    **
    ** @return true iff we see that the service fully stopped.
    *******************************************************************************/
   public boolean stop()
   {
      if(!runningState.equals(RunningState.RUNNING))
      {
         LOG.warn("Request to stop from an invalid running state [" + runningState + "].  Must be RUNNING.");
         return (false);
      }

      LOG.info("Stopping [" + name + "]");
      runningState = RunningState.STOPPING;
      service.shutdown();

      try
      {
         if(service.awaitTermination(300, TimeUnit.SECONDS))
         {
            LOG.info("Successfully stopped [" + name + "]");
            runningState = RunningState.STOPPED;
            return (true);
         }

         LOG.warn("Timed out waiting for service to fully terminate.  Will be left in STOPPING state.");
      }
      catch(InterruptedException ie)
      {
         ///////////////////////////////
         // what does this ever mean? //
         ///////////////////////////////
      }

      return (false);
   }



   /*******************************************************************************
    ** Getter for initialDelayMillis
    **
    *******************************************************************************/
   public Integer getInitialDelayMillis()
   {
      return initialDelayMillis;
   }



   /*******************************************************************************
    ** Setter for initialDelayMillis
    **
    *******************************************************************************/
   public void setInitialDelayMillis(Integer initialDelayMillis)
   {
      this.initialDelayMillis = initialDelayMillis;
   }



   /*******************************************************************************
    ** Getter for delayMillis
    **
    *******************************************************************************/
   public Integer getDelayMillis()
   {
      return delayMillis;
   }



   /*******************************************************************************
    ** Setter for delayMillis
    **
    *******************************************************************************/
   public void setDelayMillis(Integer delayMillis)
   {
      this.delayMillis = delayMillis;
   }



   /*******************************************************************************
    ** Setter for qInstance
    **
    *******************************************************************************/
   public void setQInstance(QInstance qInstance)
   {
      this.qInstance = qInstance;
   }



   /*******************************************************************************
    ** Setter for name
    **
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Setter for sessionSupplier
    **
    *******************************************************************************/
   public void setSessionSupplier(Supplier<QSession> sessionSupplier)
   {
      this.sessionSupplier = sessionSupplier;
   }



   /*******************************************************************************
    ** Getter for runningState
    **
    *******************************************************************************/
   public RunningState getRunningState()
   {
      return runningState;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public enum RunningState
   {
      STOPPED,
      RUNNING,
      STOPPING,
   }



   /*******************************************************************************
    ** Getter for name
    **
    *******************************************************************************/
   public String getName()
   {
      return name;
   }
}