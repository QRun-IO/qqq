/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.audits;


import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditHandlerFailurePolicy;


/*******************************************************************************
 ** Base interface for audit handlers that can receive audit events.
 ** Handlers can be registered globally or per-table via QAuditHandlerMetaData.
 **
 ** Implementations should extend either:
 ** - {@link DMLAuditHandlerInterface} for raw DML events with full record snapshots
 ** - {@link ProcessedAuditHandlerInterface} for processed audit messages
 *******************************************************************************/
public interface AuditHandlerInterface
{
   /***************************************************************************
    ** Unique name for this handler (used for registration and logging).
    ***************************************************************************/
   String getName();


   /***************************************************************************
    ** Whether this handler should execute synchronously or asynchronously.
    ** Async handlers run in a thread pool after the transaction commits.
    ** Default is false (synchronous).
    ***************************************************************************/
   default boolean isAsync()
   {
      return false;
   }


   /***************************************************************************
    ** Policy for handling failures in this handler.
    ** Default is LOG_AND_CONTINUE.
    ** Note: FAIL_OPERATION is only supported for synchronous handlers.
    ***************************************************************************/
   default AuditHandlerFailurePolicy getFailurePolicy()
   {
      return AuditHandlerFailurePolicy.LOG_AND_CONTINUE;
   }

}
