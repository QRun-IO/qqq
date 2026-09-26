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


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.utils.PrefixedDefaultThreadFactory;


/*******************************************************************************
 ** For actions that may want to set a timeout, and cancel themselves if they run
 ** too long - this class helps.
 **
 ** Construct with the timeout (delay & timeUnit), and a runnable that takes care
 ** of doing the cancel (e.g., cancelling a JDBC statement).
 **
 ** Call start() to make a future get scheduled (note, if delay was null or <= 0,
 ** then it doesn't get scheduled at all).
 **
 ** Call cancel() if the action got far enough/completed, to cancel the future.
 **
 ** You can check didTimeout (getDidTimeout()) to know if the timeout did occur.
 *******************************************************************************/
public class ActionTimeoutHelper
{
   private final Integer            delay;
   private final TimeUnit           timeUnit;
   private final Runnable           runnable;
   private       ScheduledFuture<?> future;

   private volatile boolean didTimeout = false;

   private static Integer                  CORE_THREADS             = 10;
   private static ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(CORE_THREADS, new PrefixedDefaultThreadFactory(ActionTimeoutHelper.class));



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ActionTimeoutHelper(Integer delay, TimeUnit timeUnit, Runnable runnable)
   {
      this.delay = delay;
      this.timeUnit = timeUnit;
      this.runnable = runnable;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void start()
   {
      if(delay == null || delay <= 0)
      {
         return;
      }

      future = scheduledExecutorService.schedule(() ->
      {
         didTimeout = true;
         runnable.run();
      }, delay, timeUnit);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void cancel()
   {
      if(future != null)
      {
         future.cancel(true);
      }
   }



   /*******************************************************************************
    ** Getter for didTimeout
    **
    *******************************************************************************/
   public boolean getDidTimeout()
   {
      return didTimeout;
   }

}
