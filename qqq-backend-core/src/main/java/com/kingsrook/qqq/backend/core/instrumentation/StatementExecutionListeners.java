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


import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** The JVM-wide registry of {@link StatementExecutionListener}s.
 **
 ** With no listener registered, {@link #isActive()} is one volatile read and
 ** backend modules skip all timing and event construction. Listeners are
 ** process-wide (a JVM normally serves one QInstance); register them once from
 ** application startup or a qbit producer, and unregister in tests.
 *******************************************************************************/
public final class StatementExecutionListeners
{
   private static final QLogger LOG = QLogger.getLogger(StatementExecutionListeners.class);

   private static final List<StatementExecutionListener> LISTENERS = new CopyOnWriteArrayList<>();
   private static final AtomicLong                       ERRORS    = new AtomicLong();

   private static volatile boolean active = false;



   /*******************************************************************************
    ** Static registry only.
    *******************************************************************************/
   private StatementExecutionListeners()
   {
   }



   /*******************************************************************************
    ** Add a listener (a listener already registered is not added twice).
    *******************************************************************************/
   public static void register(StatementExecutionListener listener)
   {
      if(listener != null && !LISTENERS.contains(listener))
      {
         LISTENERS.add(listener);
      }
      active = !LISTENERS.isEmpty();
   }



   /*******************************************************************************
    ** Remove a listener.
    *******************************************************************************/
   public static void unregister(StatementExecutionListener listener)
   {
      LISTENERS.remove(listener);
      active = !LISTENERS.isEmpty();
   }



   /*******************************************************************************
    ** Remove every listener (tests).
    *******************************************************************************/
   public static void clear()
   {
      LISTENERS.clear();
      active = false;
   }



   /*******************************************************************************
    ** Whether any listener is registered.
    *******************************************************************************/
   public static boolean isActive()
   {
      return (active);
   }



   /*******************************************************************************
    ** Start timing a statement: a timer whose finish() reports it, or a no-op
    ** timer (no allocation, no clock read) when nobody listens.
    *******************************************************************************/
   public static StatementTimer start(String backendName, String tableName, StatementOperation operation)
   {
      if(!active)
      {
         return (StatementTimer.NOOP);
      }
      return (new StatementTimer(backendName, tableName, operation, System.nanoTime()));
   }



   /*******************************************************************************
    ** Tell every listener about a statement. Never throws.
    *******************************************************************************/
   public static void fire(StatementExecution execution)
   {
      for(StatementExecutionListener listener : LISTENERS)
      {
         try
         {
            listener.onStatement(execution);
         }
         catch(Throwable t)
         {
            logListenerError(listener, t);
         }
      }
   }



   /*******************************************************************************
    ** Tell every listener that the outermost unit of work ended. Never throws.
    *******************************************************************************/
   static void fireUnitOfWorkEnd(UnitOfWork unitOfWork)
   {
      for(StatementExecutionListener listener : LISTENERS)
      {
         try
         {
            listener.onUnitOfWorkEnd(unitOfWork);
         }
         catch(Throwable t)
         {
            logListenerError(listener, t);
         }
      }
   }



   /*******************************************************************************
    ** The outermost process on this thread's action stack, or null.
    *******************************************************************************/
   static String currentProcessName()
   {
      try
      {
         if(QContext.getActionStack() != null)
         {
            for(AbstractActionInput action : QContext.getActionStack())
            {
               if(action instanceof RunProcessInput runProcessInput)
               {
                  return (runProcessInput.getProcessName());
               }
            }
         }
      }
      catch(Exception e)
      {
         //////////////////////////////////////////////////////////////////
         // a stack modified concurrently is not worth failing a query.  //
         //////////////////////////////////////////////////////////////////
      }
      return (null);
   }



   /*******************************************************************************
    ** Log the first error and then every 1000th, so a broken listener cannot
    ** flood the log.
    *******************************************************************************/
   private static void logListenerError(StatementExecutionListener listener, Throwable t)
   {
      long count = ERRORS.incrementAndGet();
      if(count == 1 || count % 1000 == 0)
      {
         LOG.warn("Statement execution listener failed (ignored)", t, logPair("listener", listener.getClass().getName()), logPair("errorCount", count));
      }
   }
}
