/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.helpers;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for ActionTimeoutHelper 
 *******************************************************************************/
class ActionTimeoutHelperTest extends BaseTest
{
   private static AtomicInteger cancelCount;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach()
   {
      cancelCount = new AtomicInteger(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTimesOut()
   {
      ActionTimeoutHelper actionTimeoutHelper = new ActionTimeoutHelper(10, TimeUnit.MILLISECONDS, () -> doCancel());
      actionTimeoutHelper.start();
      SleepUtils.sleep(50, TimeUnit.MILLISECONDS);
      assertEquals(1, cancelCount.get());
      assertTrue(actionTimeoutHelper.getDidTimeout());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testGetsCancelled()
   {
      ActionTimeoutHelper actionTimeoutHelper = new ActionTimeoutHelper(100, TimeUnit.MILLISECONDS, () -> doCancel());
      actionTimeoutHelper.start();
      SleepUtils.sleep(10, TimeUnit.MILLISECONDS);
      actionTimeoutHelper.cancel();
      assertEquals(0, cancelCount.get());
      SleepUtils.sleep(200, TimeUnit.MILLISECONDS);
      assertEquals(0, cancelCount.get());
      assertFalse(actionTimeoutHelper.getDidTimeout());
   }



   /*******************************************************************************
    ** goal here is - confirm that we can have more threads running at same time
    ** than we have threads allocated to the ActionTimeoutHelper's thread pool,
    ** and they should all still get cancelled.
    *******************************************************************************/
   @Test
   void testManyThreads() throws InterruptedException, ExecutionException
   {
      int N = 50;

      ExecutorService executorService = Executors.newCachedThreadPool();
      List<Future<?>> futureList      = new ArrayList<>();

      for(int i = 0; i < N; i++)
      {
         System.out.println("Submitting: " + i);
         futureList.add(executorService.submit(() ->
         {
            ActionTimeoutHelper actionTimeoutHelper = new ActionTimeoutHelper(10, TimeUnit.MILLISECONDS, () -> doCancel());
            actionTimeoutHelper.start();
            SleepUtils.sleep(1, TimeUnit.SECONDS);
         }));
      }

      for(Future<?> future : futureList)
      {
         future.get();
      }

      assertEquals(N, cancelCount.get());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void doCancel()
   {
      cancelCount.getAndIncrement();
   }

}