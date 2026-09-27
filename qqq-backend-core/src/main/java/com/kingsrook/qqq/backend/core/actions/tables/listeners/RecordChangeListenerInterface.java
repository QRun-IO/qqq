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
 ** A new instance is loaded (via QCodeLoader) for each event - and for each
 ** appliesTo check - so keep any shared state outside the instance.
 **
 ** One RecordChangeEvent object is shared by every listener that applies to it
 ** (called in registration order), and its records are the action's own record
 ** objects: do not change the event or its records (a change would be seen by
 ** the listeners after yours, and by the action's caller), and copy anything
 ** kept beyond the call.
 **
 ** Exceptions (and LinkageErrors, such as a NoClassDefFoundError from an
 ** optional library that isn't on the classpath) from a listener are logged
 ** and never fail the write.
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
