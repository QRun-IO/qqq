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

package com.kingsrook.qqq.backend.core.actions.async;


import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.kingsrook.qqq.backend.core.BaseTest;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;


/*******************************************************************************
 ** Startup failures must not leave jobs counted as running during node drain.
 *******************************************************************************/
class AsyncJobStartupCleanupTest extends BaseTest
{
   /*******************************************************************************
    ** Failure to persist the initial status happens before a worker can clean up.
    *******************************************************************************/
   @Test
   void initialStatusFailureDoesNotLeaveRunningJob()
   {
      assertTrue(AsyncJobRegistry.awaitIdle(Duration.ofSeconds(5)), "Prior jobs must finish before measuring startup cleanup");
      UUID jobUUID = UUID.randomUUID();
      int initialRunningCount = AsyncJobRegistry.runningCount();
      AtomicBoolean bodyRan = new AtomicBoolean();
      IllegalStateException failure = new IllegalStateException("Initial status write failed");

      try(MockedStatic<AsyncJobRegistry> registry = mockStatic(AsyncJobRegistry.class, CALLS_REAL_METHODS))
      {
         registry.when(() -> AsyncJobRegistry.store(eq(jobUUID), any(AsyncJobStatus.class), eq(true))).thenThrow(failure);
         AsyncJobManager manager = new AsyncJobManager().withForcedJobUUID(jobUUID.toString());
         assertSame(failure, assertThrows(IllegalStateException.class, () -> manager.startJob(1, TimeUnit.SECONDS, callback ->
         {
            bodyRan.set(true);
            return (42);
         })));
         assertFalse(bodyRan.get());
         assertEquals(initialRunningCount, AsyncJobRegistry.runningCount(), "A job that never started must not block drain");
      }
      finally
      {
         AsyncJobRegistry.unregister(jobUUID);
      }
   }
}
