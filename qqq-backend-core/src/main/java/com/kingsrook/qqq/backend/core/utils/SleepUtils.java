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

package com.kingsrook.qqq.backend.core.utils;


import java.time.Duration;
import java.util.concurrent.TimeUnit;


/*******************************************************************************
 ** Utility methods to help with sleeping!
 *******************************************************************************/
public class SleepUtils
{

   /*******************************************************************************
    ** Sleep for as close as we can to the specified amount of time (ignoring
    ** InterruptedException - continuing to sleep more).
    *******************************************************************************/
   public static void sleep(long duration, TimeUnit timeUnit)
   {
      long millis = timeUnit.toMillis(duration);
      long start  = System.currentTimeMillis();
      long end    = start + millis;

      while(System.currentTimeMillis() < end)
      {
         try
         {
            long millisToSleep = end - System.currentTimeMillis();
            Thread.sleep(Math.max(0, millisToSleep)); // avoid negative sleep, which fails.
         }
         catch(InterruptedException e)
         {
            // sleep more.
         }
      }
   }



   /*******************************************************************************
    ** overload for sleep that takes duration object
    *******************************************************************************/
   public static void sleep(Duration sleepDuration)
   {
      sleep(sleepDuration.toMillis(), TimeUnit.MILLISECONDS);
   }

}
