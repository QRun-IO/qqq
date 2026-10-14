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

package com.kingsrook.qqq.backend.core.model.helpcontent;


/*******************************************************************************
 ** Scoped per-thread marker distinguishing sync writes from user edits.
 *******************************************************************************/
public class HelpContentSyncContext implements AutoCloseable
{
   private static final ThreadLocal<Boolean> active = ThreadLocal.withInitial(() -> false);
   private final boolean previous;


   /*******************************************************************************
    ** Begin a scope, preserving nested callers.
    *******************************************************************************/
   public HelpContentSyncContext()
   {
      previous = active.get();
      active.set(true);
   }



   /*******************************************************************************
    ** Whether this write belongs to bundle synchronization.
    *******************************************************************************/
   public static boolean isActive()
   {
      return (active.get());
   }



   /*******************************************************************************
    ** Restore the caller's scope even when a table action fails.
    *******************************************************************************/
   @Override
   public void close()
   {
      if(previous)
      {
         active.set(true);
      }
      else
      {
         active.remove();
      }
   }
}
