/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.modules.backend.implementations.memory;


import java.util.concurrent.Callable;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyGuarantee;


/*******************************************************************************
 ** Consistent-read transaction for the memory backend: holds a copy of all the
 ** data in the store, taken at one instant (under the store's lock), which the
 ** memory query, count and aggregate actions read from when this is the
 ** transaction on their input.
 **
 ** Note the memory store is one process-wide store - so the snapshot covers
 ** every memory backend, not just the one named on the input.
 *******************************************************************************/
public class MemoryConsistentReadTransaction extends QBackendTransaction
{
   private final MemoryRecordStore.Snapshot snapshot;



   /*******************************************************************************
    ** Constructor - takes the snapshot now.
    *******************************************************************************/
   public MemoryConsistentReadTransaction()
   {
      this.snapshot = MemoryRecordStore.getInstance().takeSnapshot();
   }



   /*******************************************************************************
    ** Run a read such that it sees this transaction's snapshot.
    *******************************************************************************/
   public <T> T callInSnapshot(Callable<T> callable) throws Exception
   {
      return (MemoryRecordStore.callWithSnapshot(snapshot, callable));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public ConsistencyGuarantee getConsistencyGuarantee()
   {
      return (ConsistencyGuarantee.ONE_INSTANT);
   }

}
