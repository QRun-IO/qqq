/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.tables.listeners;


import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** Instance-level listener that InsertAction, UpdateAction and DeleteAction call
 ** with the records they wrote successfully.  Register one with
 ** QInstance.withRecordChangeListener(new QCodeReference(YourListener.class)).
 **
 ** Listeners run after the backend write and before the action returns.  When
 ** the caller passed a transaction, that transaction has not committed yet, so
 ** work that must only happen for committed data has to wait for the commit.
 **
 ** A new instance is loaded (via QCodeLoader) for each call, so keep any shared
 ** state outside the instance.  The event's records are the action's own record
 ** objects: do not change them, and copy anything kept beyond the call.
 ** Exceptions from a listener are logged and never fail the write.
 *******************************************************************************/
public interface RecordChangeListenerInterface
{
   /*******************************************************************************
    ** Whether this listener wants events for this table and change type.  Actions
    ** ask before they fetch old records, so returning false for what a listener
    ** does not need keeps them from running extra queries.
    *******************************************************************************/
   boolean appliesTo(String tableName, RecordChangeType type);

   /*******************************************************************************
    ** Called once per action with the records that were written without errors.
    *******************************************************************************/
   void onRecordsChanged(RecordChangeEvent event) throws QException;

}
