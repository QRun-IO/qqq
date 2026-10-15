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

package com.kingsrook.qqq.backend.core.instrumentation;


/*******************************************************************************
 ** Opens and closes {@link UnitOfWork}s on the current thread.
 **
 ** Units nest: the outermost unit owns every statement run while it is open
 ** (a process run inside an HTTP request belongs to that request; a scheduled
 ** job's process belongs to the job). Only the outermost unit's end is
 ** reported to listeners. Scopes do nothing (and return a shared no-op) while
 ** no statement listener is registered.
 **
 ** <pre>
 ** try(UnitsOfWork.Scope scope = UnitsOfWork.begin(UnitOfWorkKind.SCHEDULED_JOB, processName)) { ... }
 ** </pre>
 **
 ** Units are per thread: work handed to another thread is not part of the unit
 ** (statements there are still attributed by process through the action stack).
 *******************************************************************************/
public final class UnitsOfWork
{
   private static final ThreadLocal<UnitOfWork> OUTERMOST = new ThreadLocal<>();
   private static final ThreadLocal<int[]>      DEPTH     = ThreadLocal.withInitial(() -> new int[1]);

   private static final Scope NOOP_SCOPE = new Scope(null, false);



   /*******************************************************************************
    ** Static helpers only.
    *******************************************************************************/
   private UnitsOfWork()
   {
   }



   /*******************************************************************************
    ** Begin a unit (or a nested scope inside the open one). Close the returned
    ** scope exactly once, in a finally block or try-with-resources.
    *******************************************************************************/
   public static Scope begin(UnitOfWorkKind kind, String name)
   {
      if(!StatementExecutionListeners.isActive())
      {
         return (NOOP_SCOPE);
      }

      int[] depth = DEPTH.get();
      if(depth[0] > 0 && OUTERMOST.get() != null)
      {
         depth[0]++;
         return (new Scope(OUTERMOST.get(), false));
      }

      UnitOfWork unit = new UnitOfWork(kind, name);
      OUTERMOST.set(unit);
      depth[0] = 1;
      return (new Scope(unit, true));
   }



   /*******************************************************************************
    ** The outermost open unit on this thread, or null.
    *******************************************************************************/
   public static UnitOfWork current()
   {
      return (OUTERMOST.get());
   }



   /*******************************************************************************
    ** Drop any unit left open on this thread (for thread pools, before a new
    ** request), without reporting it.
    *******************************************************************************/
   public static void reset()
   {
      OUTERMOST.remove();
      DEPTH.get()[0] = 0;
   }



   /*******************************************************************************
    ** A begun unit or nested scope.
    *******************************************************************************/
   public static final class Scope implements AutoCloseable
   {
      private final UnitOfWork unit;
      private final boolean    outermost;
      private boolean          closed;



      /*******************************************************************************
       ** Constructor.
       *******************************************************************************/
      private Scope(UnitOfWork unit, boolean outermost)
      {
         this.unit = unit;
         this.outermost = outermost;
      }



      /*******************************************************************************
       ** The unit this scope belongs to (null for the no-op scope).
       *******************************************************************************/
      public UnitOfWork getUnitOfWork()
      {
         return (unit);
      }



      /*******************************************************************************
       ** End the scope; ending the outermost one reports the unit to listeners.
       *******************************************************************************/
      @Override
      public void close()
      {
         if(unit == null || closed)
         {
            return;
         }
         closed = true;

         int[] depth = DEPTH.get();
         depth[0] = Math.max(0, depth[0] - 1);
         if(outermost)
         {
            OUTERMOST.remove();
            depth[0] = 0;
            StatementExecutionListeners.fireUnitOfWorkEnd(unit);
         }
      }
   }
}
