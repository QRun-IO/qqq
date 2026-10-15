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
 ** Times one statement for {@link StatementExecutionListeners}. Backend modules
 ** call start() just before executing, succeeded(rows) after it returns, and
 ** finish(sql) in a finally block; a statement never marked as succeeded is
 ** reported as failed.
 **
 ** <pre>
 ** StatementTimer timer = StatementExecutionListeners.start(backend, table, StatementOperation.QUERY);
 ** try { ...execute...; timer.succeeded(rowCount); }
 ** finally { timer.finish(sql); }
 ** </pre>
 *******************************************************************************/
public class StatementTimer
{
   /** The shared timer used while nobody listens; every method is a no-op. */
   public static final StatementTimer NOOP = new StatementTimer(null, null, null, 0L);

   private final String             backendName;
   private final String             tableName;
   private final StatementOperation operation;
   private final long               startNanos;

   private boolean succeeded = false;
   private Integer rowCount;
   private boolean finished  = false;



   /*******************************************************************************
    ** Constructor (see StatementExecutionListeners.start).
    *******************************************************************************/
   StatementTimer(String backendName, String tableName, StatementOperation operation, long startNanos)
   {
      this.backendName = backendName;
      this.tableName = tableName;
      this.operation = operation;
      this.startNanos = startNanos;
   }



   /*******************************************************************************
    ** The statement completed; rows read or affected when known.
    *******************************************************************************/
   public StatementTimer succeeded(Integer rowCount)
   {
      if(this != NOOP)
      {
         this.succeeded = true;
         this.rowCount = rowCount;
      }
      return (this);
   }



   /*******************************************************************************
    ** Report the statement (once). Never throws.
    *******************************************************************************/
   public void finish(CharSequence sql)
   {
      if(this == NOOP || finished)
      {
         return;
      }
      finished = true;
      try
      {
         long elapsed = System.nanoTime() - startNanos;
         StatementExecutionListeners.fire(new StatementExecution(backendName, tableName, operation == null ? StatementOperation.OTHER : operation,
            sql == null ? null : sql.toString(), elapsed, rowCount, succeeded, UnitsOfWork.current(), StatementExecutionListeners.currentProcessName()));
      }
      catch(Throwable t)
      {
         //////////////////////////////////////////////////////////////
         // instrumentation must never fail the statement it timed   //
         //////////////////////////////////////////////////////////////
      }
   }
}
