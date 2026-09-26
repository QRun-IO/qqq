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

package com.kingsrook.qqq.backend.core.state;


import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.PrefixedDefaultThreadFactory;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Singleton class that provides a (non-persistent!!) in-memory state provider.
 *******************************************************************************/
public class InMemoryStateProvider implements StateProviderInterface
{
   private static final QLogger LOG = QLogger.getLogger(InMemoryStateProvider.class);

   private static InMemoryStateProvider instance;

   private final Map<AbstractStateKey, Object> map;

   private static int jobPeriodSeconds = 60 * 30; // 30 minutes
   private static int cleanHours       = 5;
   private static int jobInitialDelay  = 60 * 60 * cleanHours;



   /*******************************************************************************
    ** Private constructor for singleton.
    *******************************************************************************/
   private InMemoryStateProvider()
   {
      this.map = Collections.synchronizedMap(new HashMap<>());

      ///////////////////////////////////////////////////////////
      // Start a single thread executor to handle the cleaning //
      ///////////////////////////////////////////////////////////
      ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(new PrefixedDefaultThreadFactory(this));
      executorService.scheduleAtFixedRate(new InMemoryStateProvider.InMemoryStateProviderCleanJob(), jobInitialDelay, jobPeriodSeconds, TimeUnit.SECONDS);
   }



   /*******************************************************************************
    ** Runnable that gets scheduled to periodically clean the InMemoryStateProvider
    *******************************************************************************/
   private static class InMemoryStateProviderCleanJob implements Runnable
   {
      private static final QLogger LOG = QLogger.getLogger(InMemoryStateProvider.InMemoryStateProviderCleanJob.class);



      /*******************************************************************************
       ** run
       *******************************************************************************/
      @Override
      public void run()
      {
         try
         {
            Instant cleanTime = Instant.now().minus(cleanHours, ChronoUnit.HOURS);
            getInstance().clean(cleanTime);
         }
         catch(Exception e)
         {
            LOG.warn("Error cleaning InMemoryStateProvider entries.", e);
         }
      }
   }



   /*******************************************************************************
    ** Singleton accessor
    *******************************************************************************/
   public static InMemoryStateProvider getInstance()
   {
      if(instance == null)
      {
         instance = new InMemoryStateProvider();
      }
      return instance;
   }



   /*******************************************************************************
    ** Put a block of data, under a key, into the state store.
    *******************************************************************************/
   @Override
   public <T extends Serializable> void put(AbstractStateKey key, T data)
   {
      map.put(key, data);
   }



   /*******************************************************************************
    ** Get a block of data, under a key, from the state store.
    *******************************************************************************/
   @Override
   public <T extends Serializable> Optional<T> get(Class<? extends T> type, AbstractStateKey key)
   {
      try
      {
         return Optional.ofNullable(type.cast(map.get(key)));
      }
      catch(ClassCastException cce)
      {
         throw new RuntimeException("Stored state value could not be cast to desired type", cce);
      }
   }



   /*******************************************************************************
    ** Remove a block of data, under a key, from the state store.
    *******************************************************************************/
   @Override
   public void remove(AbstractStateKey key)
   {
      map.remove(key);
   }



   /*******************************************************************************
    ** Get the current status
    *
    *******************************************************************************/
   @Override
   public String status()
   {
      return ("InMemoryStateProvider map size: " + map.size());
   }



   /*******************************************************************************
    ** Clean entries that started before the given Instant
    *
    *******************************************************************************/
   @Override
   public void clean(Instant cleanBeforeInstant)
   {
      long    jobStartTime = System.currentTimeMillis();
      Integer beforeSize   = map.size();
      LOG.info("Starting clean for InMemoryStateProvider.", logPair("beforeSize", beforeSize));

      map.entrySet().removeIf(e -> e.getKey().getStartTime().isBefore(cleanBeforeInstant));

      Integer afterSize = map.size();
      long    endTime   = System.currentTimeMillis();
      LOG.info("Completed clean for InMemoryStateProvider.", logPair("beforeSize", beforeSize), logPair("afterSize", afterSize), logPair("amountCleaned", (beforeSize - afterSize)), logPair("runTimeMillis", (endTime - jobStartTime)));
   }

}
