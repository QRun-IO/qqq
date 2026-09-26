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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.scheduler.QSchedulerInterface;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.SchedulableType;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.BasicSchedulableIdentity;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.SchedulableIdentity;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** QQQ Service (Singleton) that starts up repeating, scheduled jobs within QQQ.
 **
 ** These include:
 ** - Automation providers (which require polling)
 ** - Queue pollers
 ** - Scheduled processes.
 **
 ** All of these jobs run using a "system session" - as defined by the sessionSupplier.
 *******************************************************************************/
public class SimpleScheduler implements QSchedulerInterface
{
   private static final QLogger LOG = QLogger.getLogger(SimpleScheduler.class);

   private static SimpleScheduler simpleScheduler = null;
   private final  QInstance       qInstance;
   private        String          schedulerName;

   protected Supplier<QSession> sessionSupplier;

   /////////////////////////////////////////////////////////////////////////////////////
   // for jobs that don't define a delay index, auto-stagger them, using this counter //
   /////////////////////////////////////////////////////////////////////////////////////
   private int delayIndex = 0;

   private Map<SchedulableIdentity, StandardScheduledExecutor> executors = new LinkedHashMap<>();



   /*******************************************************************************
    ** Singleton constructor
    *******************************************************************************/
   private SimpleScheduler(QInstance qInstance)
   {
      this.qInstance = qInstance;
   }



   /*******************************************************************************
    ** Singleton accessor
    *******************************************************************************/
   public static SimpleScheduler getInstance(QInstance qInstance)
   {
      if(simpleScheduler == null)
      {
         simpleScheduler = new SimpleScheduler(qInstance);
      }
      return (simpleScheduler);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void start()
   {
      for(StandardScheduledExecutor executor : executors.values())
      {
         executor.start();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void stopAsync()
   {
      for(StandardScheduledExecutor scheduledExecutor : executors.values())
      {
         scheduledExecutor.stopAsync();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void stop()
   {
      for(StandardScheduledExecutor scheduledExecutor : executors.values())
      {
         scheduledExecutor.stop();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void setupSchedulable(SchedulableIdentity schedulableIdentity, SchedulableType schedulableType, Map<String, Serializable> parameters, QScheduleMetaData schedule, boolean allowedToStart)
   {
      if(!allowedToStart)
      {
         return;
      }

      SimpleJobRunner           simpleJobRunner = new SimpleJobRunner(qInstance, schedulableType, new HashMap<>(parameters));
      StandardScheduledExecutor executor        = new StandardScheduledExecutor(simpleJobRunner);
      executor.setName(schedulableIdentity.getIdentity());
      setScheduleInExecutor(schedule, executor);
      executors.put(schedulableIdentity, executor);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void setScheduleInExecutor(QScheduleMetaData schedule, StandardScheduledExecutor executor)
   {
      if(schedule.getRepeatMillis() != null)
      {
         executor.setDelayMillis(schedule.getRepeatMillis());
      }
      else
      {
         executor.setDelayMillis(1000 * schedule.getRepeatSeconds());
      }

      if(schedule.getInitialDelayMillis() != null)
      {
         executor.setInitialDelayMillis(schedule.getInitialDelayMillis());
      }
      else if(schedule.getInitialDelaySeconds() != null)
      {
         executor.setInitialDelayMillis(1000 * schedule.getInitialDelaySeconds());
      }
      else
      {
         executor.setInitialDelayMillis(1000 * ++delayIndex);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void unscheduleSchedulable(SchedulableIdentity schedulableIdentity, SchedulableType schedulableType)
   {
      StandardScheduledExecutor executor = executors.get(schedulableIdentity);
      if(executor != null)
      {
         LOG.info("Stopping job in simple scheduler", logPair("identity", schedulableIdentity));
         executors.remove(schedulableIdentity);
         executor.stop();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean isScheduled(BasicSchedulableIdentity schedulableIdentity, SchedulableType schedulableType)
   {
      return (executors.containsKey(schedulableIdentity));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void unscheduleAll() throws QException
   {
      for(Map.Entry<SchedulableIdentity, StandardScheduledExecutor> entry : new HashSet<>(executors.entrySet()))
      {
         StandardScheduledExecutor executor = executors.remove(entry.getKey());
         if(executor != null)
         {
            executor.stopAsync();
         }
      }
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
    ** Getter for sessionSupplier
    **
    *******************************************************************************/
   public Supplier<QSession> getSessionSupplier()
   {
      return sessionSupplier;
   }



   /*******************************************************************************
    ** Getter for managedExecutors
    **
    *******************************************************************************/
   public List<StandardScheduledExecutor> getExecutors()
   {
      return new ArrayList<>(executors.values());
   }



   /*******************************************************************************
    ** Getter for schedulerName
    *******************************************************************************/
   public String getSchedulerName()
   {
      return (this.schedulerName);
   }



   /*******************************************************************************
    ** Setter for schedulerName
    *******************************************************************************/
   public void setSchedulerName(String schedulerName)
   {
      this.schedulerName = schedulerName;
   }



   /*******************************************************************************
    ** Fluent setter for schedulerName
    *******************************************************************************/
   public SimpleScheduler withSchedulerName(String schedulerName)
   {
      this.schedulerName = schedulerName;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void unInit()
   {
      //////////////////////////////////////////////////
      // resetting the singleton should be sufficient //
      //////////////////////////////////////////////////
      simpleScheduler = null;
   }
}
