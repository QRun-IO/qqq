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
 ** Observes the statements backend modules run (for example to attribute
 ** database time to routes, processes and jobs, or to find N+1 patterns).
 **
 ** Both methods are called synchronously on the thread that ran the statement
 ** or ended the unit, so they must be cheap: aggregate in memory and write
 ** elsewhere. They must not run table actions themselves. Exceptions they
 ** throw are caught and logged (rate-limited) by core and never reach the
 ** caller.
 *******************************************************************************/
public interface StatementExecutionListener
{
   /*******************************************************************************
    ** A statement completed (successfully or not).
    *******************************************************************************/
   void onStatement(StatementExecution execution);


   /*******************************************************************************
    ** The outermost unit of work on this thread ended. Nested units do not call
    ** this; their statements belong to the outermost unit.
    *******************************************************************************/
   default void onUnitOfWorkEnd(UnitOfWork unitOfWork)
   {
   }
}
