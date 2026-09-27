/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.actions.async;


import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.state.InMemoryStateProvider;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for NonPersistedAsyncJobCallback.
 **
 ** The key behavioral difference from AsyncJobCallback is that storeUpdatedStatus()
 ** is intentionally a no-op, so updates never write to the state provider.
 *******************************************************************************/
@TestMethodOrder(MethodOrderer.MethodName.class)
class NonPersistedAsyncJobCallbackTest extends BaseTest
{
   private static final QLogger LOG = QLogger.getLogger(NonPersistedAsyncJobCallbackTest.class);



   /*******************************************************************************
    ** updateStatus must update the in-memory AsyncJobStatus object but must NOT
    ** write anything to the state provider.
    *******************************************************************************/
   @Test
   void testUpdateStatus_doesNotWriteToStateProvider()
   {
      UUID             jobUUID        = UUID.randomUUID();
      AsyncJobStatus   asyncJobStatus = new AsyncJobStatus();
      NonPersistedAsyncJobCallback callback = new NonPersistedAsyncJobCallback(jobUUID, asyncJobStatus);

      callback.updateStatus("processing", 5, 10);

      /////////////////////////////////////////////////////////////////////
      // The status object itself must be updated (in-memory is mutated). //
      /////////////////////////////////////////////////////////////////////
      assertThat(asyncJobStatus.getMessage()).isEqualTo("processing");
      assertThat(asyncJobStatus.getCurrent()).isEqualTo(5);
      assertThat(asyncJobStatus.getTotal()).isEqualTo(10);

      ///////////////////////////////////////////////////////////////////////
      // The state provider must not have been written to for this job id. //
      ///////////////////////////////////////////////////////////////////////
      UUIDAndTypeStateKey key   = new UUIDAndTypeStateKey(jobUUID, StateType.ASYNC_JOB_STATUS);
      assertThat(AsyncJobManager.getStateProvider().get(AsyncJobStatus.class, key)).isEmpty();
   }



   /*******************************************************************************
    ** incrementCurrent must increment the in-memory status without persisting.
    *******************************************************************************/
   @Test
   void testIncrementCurrent_updatesStatusWithoutPersisting()
   {
      UUID             jobUUID        = UUID.randomUUID();
      AsyncJobStatus   asyncJobStatus = new AsyncJobStatus();
      NonPersistedAsyncJobCallback callback = new NonPersistedAsyncJobCallback(jobUUID, asyncJobStatus);

      callback.updateStatus(0, 5);
      callback.incrementCurrent();
      callback.incrementCurrent();

      assertThat(asyncJobStatus.getCurrent()).isEqualTo(2);

      UUIDAndTypeStateKey key   = new UUIDAndTypeStateKey(jobUUID, StateType.ASYNC_JOB_STATUS);
      assertThat(InMemoryStateProvider.getInstance().get(AsyncJobStatus.class, key)).isEmpty();
   }



   /*******************************************************************************
    ** wasCancelRequested must delegate to the in-memory status flag.
    *******************************************************************************/
   @Test
   void testWasCancelRequested_reflectsInMemoryStatus()
   {
      UUID             jobUUID        = UUID.randomUUID();
      AsyncJobStatus   asyncJobStatus = new AsyncJobStatus();
      NonPersistedAsyncJobCallback callback = new NonPersistedAsyncJobCallback(jobUUID, asyncJobStatus);

      assertThat(callback.wasCancelRequested()).isFalse();

      asyncJobStatus.setCancelRequested(true);
      assertThat(callback.wasCancelRequested()).isTrue();
   }



   /*******************************************************************************
    ** clearCurrentAndTotal must wipe the counters without persisting.
    *******************************************************************************/
   @Test
   void testClearCurrentAndTotal_clearsInMemoryWithoutPersisting()
   {
      UUID             jobUUID        = UUID.randomUUID();
      AsyncJobStatus   asyncJobStatus = new AsyncJobStatus();
      NonPersistedAsyncJobCallback callback = new NonPersistedAsyncJobCallback(jobUUID, asyncJobStatus);

      callback.updateStatus(3, 10);
      callback.clearCurrentAndTotal();

      assertThat(asyncJobStatus.getCurrent()).isNull();
      assertThat(asyncJobStatus.getTotal()).isNull();

      UUIDAndTypeStateKey key   = new UUIDAndTypeStateKey(jobUUID, StateType.ASYNC_JOB_STATUS);
      assertThat(InMemoryStateProvider.getInstance().get(AsyncJobStatus.class, key)).isEmpty();
   }

}
