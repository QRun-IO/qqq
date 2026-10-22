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

package com.kingsrook.qqq.middleware.health;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthResponse;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Concurrency contracts through the public executor and owned indicators.
 *******************************************************************************/
class HealthCheckExecutorConcurrencyTest
{
   /*******************************************************************************
    ** More than one pool of checks runs, with the first ten meeting concurrently.
    *******************************************************************************/
   @Test
   void testConcurrentChecksAndMoreThanPoolSize()
   {
      CountDownLatch entered = new CountDownLatch(10);
      Set<Thread> workers = ConcurrentHashMap.newKeySet();
      List<HealthIndicator> indicators = new ArrayList<>();
      for(int i = 0; i < 12; i++)
      {
         indicators.add(indicator("check" + i, () ->
         {
            workers.add(Thread.currentThread());
            entered.countDown();
            if(!entered.await(1, TimeUnit.SECONDS))
            {
               throw new IllegalStateException("Checks did not run concurrently");
            }
            return new HealthCheckResult().withStatus(HealthStatus.UP);
         }));
      }
      HealthCheckExecutor executor = executor(3000, indicators);
      try
      {
         HealthResponse response = executor.execute();
         assertThat(response.getChecks()).hasSize(12);
         assertThat(response.getStatus()).isEqualTo(HealthStatus.UP);
         assertThat(workers).hasSize(10);
      }
      finally
      {
         executor.shutdown();
      }
      assertStopped(workers);
   }



   /*******************************************************************************
    ** Queue time counts toward each budget; collection must not cost N timeouts.
    *******************************************************************************/
   @Test
   void testMultipleTimeoutsIncludeQueueTimeAndCancelWorkers()
   {
      Set<Thread> workers = ConcurrentHashMap.newKeySet();
      AtomicInteger active = new AtomicInteger();
      AtomicInteger maximum = new AtomicInteger();
      AtomicInteger interrupted = new AtomicInteger();
      List<HealthIndicator> indicators = new ArrayList<>();
      for(int i = 0; i < 24; i++)
      {
         indicators.add(indicator("slow" + i, () ->
         {
            workers.add(Thread.currentThread());
            maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
            try
            {
               new CountDownLatch(1).await();
               return new HealthCheckResult().withStatus(HealthStatus.UP);
            }
            catch(InterruptedException e)
            {
               interrupted.incrementAndGet();
               throw e;
            }
            finally
            {
               active.decrementAndGet();
            }
         }));
      }
      HealthCheckExecutor executor = executor(400, indicators);
      try
      {
         long start = System.nanoTime();
         HealthResponse response = executor.execute();
         assertThat(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)).isLessThan(2000);
         assertThat(response.getChecks()).hasSize(24);
         assertThat(response.getChecks().values()).allSatisfy(result ->
         {
            assertThat(result.getStatus()).isEqualTo(HealthStatus.UNKNOWN);
            assertThat(result.getDurationMs()).isEqualTo(400L);
            assertThat(result.getDetails()).containsEntry("error", "Health check timed out").containsEntry("timeoutMs", 400);
         });
      }
      finally
      {
         executor.shutdown();
      }
      assertThat(maximum.get()).isEqualTo(10);
      assertThat(active.get()).isZero();
      assertThat(interrupted.get()).isGreaterThanOrEqualTo(10);
      assertStopped(workers);
   }



   /*******************************************************************************
    ** Caller interruption is preserved and cancels every outstanding check.
    *******************************************************************************/
   @Test
   void testInterruptedCallerCancelsOutstandingChecks() throws Exception
   {
      CountDownLatch entered = new CountDownLatch(2);
      Set<Thread> workers = ConcurrentHashMap.newKeySet();
      Callable<HealthCheckResult> blocking = () ->
      {
         workers.add(Thread.currentThread());
         entered.countDown();
         new CountDownLatch(1).await();
         return new HealthCheckResult().withStatus(HealthStatus.UP);
      };
      HealthCheckExecutor executor = executor(5000, List.of(indicator("first", blocking), indicator("second", blocking)));
      AtomicBoolean interrupted = new AtomicBoolean();
      AtomicReference<HealthResponse> response = new AtomicReference<>();
      Thread caller = new Thread(() ->
      {
         try
         {
            response.set(executor.execute());
            interrupted.set(Thread.currentThread().isInterrupted());
         }
         finally
         {
            executor.shutdown();
         }
      });
      try
      {
         caller.start();
         assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
         caller.interrupt();
         caller.join(2000);
         assertThat(caller.isAlive()).isFalse();
         assertThat(interrupted.get()).isTrue();
         assertThat(response.get().getChecks()).hasSize(2);
         assertThat(response.get().getChecks().values()).allSatisfy(result ->
         {
            assertThat(result.getStatus()).isEqualTo(HealthStatus.UNKNOWN);
            assertThat(result.getDetails()).containsEntry("exceptionType", "InterruptedException");
         });
      }
      finally
      {
         caller.interrupt();
         caller.join(2000);
         executor.shutdown();
      }
      assertStopped(workers);
   }



   /*******************************************************************************
    ** Failures and null results do not discard healthy or DOWN siblings.
    *******************************************************************************/
   @Test
   void testThrowingAndNullResultsPreserveAggregationAndDuration()
   {
      HealthCheckExecutor executor = executor(2000, List.of(
         indicator("throwing", () ->
         {
            throw new IllegalStateException("owned failure");
         }),
         indicator("null", () -> null),
         indicator("healthy", () -> new HealthCheckResult().withStatus(HealthStatus.UP).withDurationMs(7L)),
         indicator("down", () -> new HealthCheckResult().withStatus(HealthStatus.DOWN))));
      try
      {
         HealthResponse response = executor.execute();
         assertThat(response.getStatus()).isEqualTo(HealthStatus.DOWN);
         assertThat(response.getChecks()).hasSize(4);
         for(String name : List.of("throwing", "null"))
         {
            assertThat(response.getChecks().get(name).getStatus()).isEqualTo(HealthStatus.UNKNOWN);
            assertThat(response.getChecks().get(name).getDetails()).containsEntry("error", "Indicator execution failed").containsEntry("exceptionType", "Exception");
         }
         assertThat(response.getChecks().get("healthy").getDurationMs()).isEqualTo(7L);
         assertThat(response.getChecks().get("down").getDurationMs()).isGreaterThanOrEqualTo(0L);
      }
      finally
      {
         executor.shutdown();
      }
   }



   /*******************************************************************************
    ** A completed sibling keeps its result even when collected after its budget.
    *******************************************************************************/
   @Test
   void testCompletedSiblingSurvivesEarlierTimeout()
   {
      CountDownLatch completed = new CountDownLatch(1);
      HealthCheckExecutor executor = executor(300, List.of(
         indicator("slow", () ->
         {
            completed.await();
            new CountDownLatch(1).await();
            return new HealthCheckResult().withStatus(HealthStatus.UP);
         }),
         indicator("fast", () ->
         {
            completed.countDown();
            return new HealthCheckResult().withStatus(HealthStatus.DOWN).withDurationMs(7L);
         })));
      try
      {
         HealthResponse response = executor.execute();
         assertThat(response.getStatus()).isEqualTo(HealthStatus.DOWN);
         assertThat(response.getChecks().get("fast").getDurationMs()).isEqualTo(7L);
         assertThat(response.getChecks().get("slow").getDetails()).containsEntry("error", "Health check timed out");
      }
      finally
      {
         executor.shutdown();
      }
   }



   /*******************************************************************************
    ** Construct an executor with a bounded per-check budget.
    *******************************************************************************/
   private HealthCheckExecutor executor(int timeoutMs, List<HealthIndicator> indicators)
   {
      return new HealthCheckExecutor(new QInstance(), new HealthCheckMetaData().withTimeoutMs(timeoutMs).withIndicators(indicators));
   }



   /*******************************************************************************
    ** Adapt owned behavior to the existing indicator interface.
    *******************************************************************************/
   private HealthIndicator indicator(String name, Callable<HealthCheckResult> check)
   {
      return new HealthIndicator()
      {
         /************************************************************************
          ** Return the fixture name.
          ************************************************************************/
         @Override
         public String getName()
         {
            return name;
         }


         /************************************************************************
          ** Execute owned behavior, preserving its cause.
          ************************************************************************/
         @Override
         public HealthCheckResult check(QInstance qInstance) throws QException
         {
            try
            {
               return check.call();
            }
            catch(Exception e)
            {
               throw new QException("Owned indicator failed", e);
            }
         }
      };
   }



   /*******************************************************************************
    ** Join only this test's workers; no global thread-count assumptions.
    *******************************************************************************/
   private void assertStopped(Set<Thread> workers)
   {
      for(Thread worker : workers)
      {
         try
         {
            worker.join(2000);
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
         }
         assertThat(worker.isAlive()).isFalse();
      }
   }
}
