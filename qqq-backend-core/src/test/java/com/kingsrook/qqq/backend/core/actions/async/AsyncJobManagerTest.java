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


import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;


/*******************************************************************************
 ** Unit test for AsyncJobManager
 *******************************************************************************/
class AsyncJobManagerTest extends BaseTest
{
   public static final int ANSWER = 42;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testCompletesInTime() throws JobGoingAsyncException, QException
   {
      AsyncJobManager asyncJobManager = new AsyncJobManager();
      Integer answer = asyncJobManager.startJob(5, TimeUnit.SECONDS, (callback) ->
      {
         return (ANSWER);
      });
      assertEquals(ANSWER, answer);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testJobGoesAsync()
   {
      assertThrows(JobGoingAsyncException.class, () ->
      {
         AsyncJobManager asyncJobManager = new AsyncJobManager();
         asyncJobManager.startJob(1, TimeUnit.MICROSECONDS, (callback) ->
         {
            Thread.sleep(1_000);
            return (ANSWER);
         });
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testJobThatThrowsBeforeTimeout()
   {
      assertThrows(QException.class, () ->
      {
         AsyncJobManager asyncJobManager = new AsyncJobManager();
         asyncJobManager.startJob(1, TimeUnit.SECONDS, (callback) ->
         {
            throw (new IllegalArgumentException("I must throw."));
         });
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testJobThatThrowsAfterTimeout() throws QException, InterruptedException
   {
      String          message         = "I must throw.";
      AsyncJobManager asyncJobManager = new AsyncJobManager();
      try
      {
         asyncJobManager.startJob(1, TimeUnit.MILLISECONDS, (callback) ->
         {
            Thread.sleep(50);
            throw (new IllegalArgumentException(message));
         });
         fail("We should catch a JobGoingAsyncException");
      }
      catch(JobGoingAsyncException jgae)
      {
         Thread.sleep(100);
         Optional<AsyncJobStatus> jobStatus = asyncJobManager.getJobStatus(jgae.getJobUUID());
         assertEquals(AsyncJobState.ERROR, jobStatus.get().getState());
         assertNotNull(jobStatus.get().getCaughtException());
         assertEquals(message, jobStatus.get().getCaughtException().getMessage());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   public void testGettingStatusOfAsyncJob() throws InterruptedException, QException
   {
      AsyncJobManager asyncJobManager = new AsyncJobManager();
      String          preMessage      = "Going to sleep";
      String          postMessage     = "Waking up";
      try
      {
         asyncJobManager.startJob(50, TimeUnit.MILLISECONDS, (callback) ->
         {
            callback.updateStatus(preMessage);
            callback.updateStatus(0, 1);
            Thread.sleep(100);
            callback.updateStatus(postMessage, 1, 1);
            return (ANSWER);
         });
         fail("We should catch a JobGoingAsyncException");
      }
      catch(JobGoingAsyncException jgae)
      {
         assertNotNull(jgae.getJobUUID());
         Optional<AsyncJobStatus> jobStatus = asyncJobManager.getJobStatus(jgae.getJobUUID());

         assertEquals(AsyncJobState.RUNNING, jobStatus.get().getState());
         assertEquals(preMessage, jobStatus.get().getMessage());
         assertEquals(0, jobStatus.get().getCurrent());
         assertEquals(1, jobStatus.get().getTotal());

         Thread.sleep(200);
         jobStatus = asyncJobManager.getJobStatus(jgae.getJobUUID());
         assertEquals(AsyncJobState.COMPLETE, jobStatus.get().getState());
         assertEquals(postMessage, jobStatus.get().getMessage());
         assertEquals(1, jobStatus.get().getCurrent());
         assertEquals(1, jobStatus.get().getTotal());
      }
   }

}