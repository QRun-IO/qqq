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
 ** One statement a backend module ran on behalf of a table action.
 **
 ** @param backendName  the QQQ backend's name
 ** @param tableName    the QQQ table the action was on (may be null)
 ** @param operation    the action's kind
 ** @param sql          the statement as sent, with bind placeholders; parameter
 **                     values are never included
 ** @param elapsedNanos time from just before execution to completion (including
 **                     reading results)
 ** @param rowCount     rows read or affected when known, else null
 ** @param succeeded    false when the statement threw
 ** @param unitOfWork   the outermost open unit of work on the thread, or null
 ** @param processName  the outermost process on the thread's action stack, or null
 *******************************************************************************/
public record StatementExecution(String backendName, String tableName, StatementOperation operation, String sql, long elapsedNanos, Integer rowCount,
                                 boolean succeeded, UnitOfWork unitOfWork, String processName)
{
}
