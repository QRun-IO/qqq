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

package com.kingsrook.qqq.backend.core.utils;


import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;


/*******************************************************************************
 ** ThreadFactory implementation that puts a common prefix on all threads.
 **
 ** Makes it so that, instead of having 100s of pool-x-thread-y names that are
 ** hard to tell apart, they can have a prefix:  MyService-pool-x-thread-y, vs
 ** YourThing-pool-x-thread-y.
 **
 ** You can put '-' at the end of your threadNamePrefix (constructor arg) or
 ** you can omit it, either way, we'll make it look like shown above.
 *******************************************************************************/
public class PrefixedDefaultThreadFactory implements ThreadFactory
{
   private final String        threadNamePrefix;
   private final ThreadFactory threadFactory;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public PrefixedDefaultThreadFactory(String threadNamePrefix)
   {
      if(StringUtils.hasContent(threadNamePrefix))
      {
         this.threadNamePrefix = threadNamePrefix.replaceAll("-+$", "") + "-";
      }
      else
      {
         this.threadNamePrefix = "";
      }

      threadFactory = Executors.defaultThreadFactory();
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public PrefixedDefaultThreadFactory(Class<?> callerClass)
   {
      this(callerClass != null ? callerClass.getSimpleName() : "");
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public PrefixedDefaultThreadFactory(Object caller)
   {
      this(caller != null ? caller.getClass() : null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Thread newThread(Runnable r)
   {
      Thread thread = threadFactory.newThread(r);
      thread.setName(threadNamePrefix + thread.getName());
      return (thread);
   }

}
