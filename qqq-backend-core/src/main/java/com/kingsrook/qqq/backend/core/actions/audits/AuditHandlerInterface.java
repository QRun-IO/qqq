/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
