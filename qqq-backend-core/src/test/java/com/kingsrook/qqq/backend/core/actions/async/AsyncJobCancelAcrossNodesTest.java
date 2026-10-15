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

package com.kingsrook.qqq.backend.core.actions.async;


import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.state.InMemoryStateProvider;
import com.kingsrook.qqq.backend.core.state.QStateProviders;
import com.kingsrook.qqq.backend.core.state.StateProviderMetaData;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.TableStateProvider;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** A cancel requested on one node must reach the job running on another, even
 ** when the running node's own status write races with (and overwrites) the
 ** cancel flag in the stored status.
 *******************************************************************************/
class AsyncJobCancelAcrossNodesTest extends BaseTest
{
   private static final String TABLE = StateProviderMetaData.DEFAULT_TABLE_NAME;



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QStateProviders.clearForTests();
   }



   /*******************************************************************************
    ** Share state through the memory backend.
    *******************************************************************************/
   private static void share()
   {
      QInstance instance = QContext.getQInstance();
      instance.addTable(TableStateProvider.defineTable(TABLE, TestUtils.MEMORY_BACKEND_NAME));
      instance.withSupplementalMetaData(new StateProviderMetaData().withTableName(TABLE).withEncryptionKey(null));
      QStateProviders.clearForTests();
   }



   /*******************************************************************************
    ** The marker survives a status write that lost the flag.
    *******************************************************************************/
   @Test
   void testCancelMarkerSurvivesALostStatusUpdate() throws Exception
   {
      share();
      String jobUUID = new AsyncJobManager().startJob("test", callback ->
      {
         long deadline = System.currentTimeMillis() + 10_000;
         while(!callback.wasCancelRequested() && System.currentTimeMillis() < deadline)
         {
            callback.updateStatus("working");
            Thread.sleep(50);
         }
         return (callback.wasCancelRequested());
      });
      UUID uuid = UUID.fromString(jobUUID);

      ///////////////////////////////////////////////////////////////////////
      // "another node" requested the cancel (marker), and then the running //
      // node's racing write stored a status without the flag              //
      ///////////////////////////////////////////////////////////////////////
      QStateProviders.get().put(AsyncJobRegistry.cancelKey(uuid), Boolean.TRUE);
      UUIDAndTypeStateKey key   = new UUIDAndTypeStateKey(uuid, StateType.ASYNC_JOB_STATUS);
      AsyncJobStatus      stale = QStateProviders.get().get(AsyncJobStatus.class, key).orElseThrow();
      stale.setCancelRequested(false);
      QStateProviders.get().put(key, stale);

      long deadline = System.currentTimeMillis() + 10_000;
      while(System.currentTimeMillis() < deadline && new AsyncJobManager().getJobStatus(jobUUID).orElseThrow().getState() == AsyncJobState.RUNNING)
      {
         Thread.sleep(100);
      }
      AsyncJobStatus finished = new AsyncJobManager().getJobStatus(jobUUID).orElseThrow();
      assertEquals(AsyncJobState.COMPLETE, finished.getState());
      assertTrue(finished.getCancelRequested(), "the running job saw the cancel");
   }



   /*******************************************************************************
    ** cancelJob writes the marker when state is shared.
    *******************************************************************************/
   @Test
   void testCancelJobWritesTheMarkerWhenShared() throws Exception
   {
      share();
      UUID           uuid   = UUID.randomUUID();
      AsyncJobStatus status = new AsyncJobStatus();
      status.setState(AsyncJobState.RUNNING);
      QStateProviders.get().put(new UUIDAndTypeStateKey(uuid, StateType.ASYNC_JOB_STATUS), status);

      new AsyncJobManager().cancelJob(uuid.toString());
      assertTrue(QStateProviders.get().get(Boolean.class, AsyncJobRegistry.cancelKey(uuid)).orElse(false));
   }



   /*******************************************************************************
    ** Single node (no StateProviderMetaData): no marker is written; the in-memory
    ** status carries the flag, as before.
    *******************************************************************************/
   @Test
   void testSingleNodeCancelWritesNoMarker() throws Exception
   {
      UUID           uuid   = UUID.randomUUID();
      AsyncJobStatus status = new AsyncJobStatus();
      status.setState(AsyncJobState.RUNNING);
      InMemoryStateProvider.getInstance().put(new UUIDAndTypeStateKey(uuid, StateType.ASYNC_JOB_STATUS), status);

      new AsyncJobManager().cancelJob(uuid.toString());
      assertFalse(InMemoryStateProvider.getInstance().get(Boolean.class, AsyncJobRegistry.cancelKey(uuid)).isPresent());
      assertTrue(new AsyncJobManager().getJobStatus(uuid.toString()).orElseThrow().getCancelRequested());
   }
}
