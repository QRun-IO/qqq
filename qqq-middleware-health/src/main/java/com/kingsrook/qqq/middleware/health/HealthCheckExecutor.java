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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthResponse;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Executor that runs health indicators and aggregates results.
 **
 ** This class handles:
 ** - Running indicators concurrently on at most ten workers
 ** - Enforcing timeouts on individual indicators
 ** - Aggregating results into overall health status
 ** - Handling exceptions gracefully
 *******************************************************************************/
public class HealthCheckExecutor
{
   public static final int DEFAULT_TIMEOUT_MS = 5000;
   private static final QLogger LOG = QLogger.getLogger(HealthCheckExecutor.class);

   private final QInstance           qInstance;
   private final HealthCheckMetaData config;
   private final ExecutorService     executorService;



   /*******************************************************************************
    ** Constructor
    **
    ** @param qInstance the QInstance
    ** @param config health check configuration
    *******************************************************************************/
   public HealthCheckExecutor(QInstance qInstance, HealthCheckMetaData config)
   {
      this.qInstance = qInstance;
      this.config = config;

      ////////////////////////////////////////////////////////////////////////
      // Create thread pool for running indicators concurrently            //
      // Size based on number of indicators (min 1, max 10)                //
      ////////////////////////////////////////////////////////////////////////
      int poolSize = Math.min(Math.max(1, config.getIndicators().size()), 10);
      this.executorService = Executors.newFixedThreadPool(poolSize);
   }



   /*******************************************************************************
    ** Execute all configured health indicators and return aggregated response.
    **
    ** @return health response with overall status and individual check results
    *******************************************************************************/
   public HealthResponse execute()
   {
      HealthResponse                 response     = new HealthResponse();
      Map<String, HealthCheckResult> checkResults = new HashMap<>();

      List<HealthIndicator> indicators = config.getIndicators();
      if(indicators == null || indicators.isEmpty())
      {
         LOG.warn("No health indicators configured");
         return response
            .withStatus(HealthStatus.UNKNOWN)
            .withCheck("configuration", new HealthCheckResult()
               .withStatus(HealthStatus.UNKNOWN)
               .withDetail("message", "No health indicators configured"));
      }

      int timeoutMs = config.getTimeoutMs() == null ? DEFAULT_TIMEOUT_MS : config.getTimeoutMs();
      List<SubmittedCheck> submittedChecks = new ArrayList<>();
      try
      {
         /////////////////////////////////////////////////////////////////
         // Submit all checks before waiting; queue time counts toward  //
         // each check's budget, rather than starting a fresh wait per  //
         // result. Use a monotonic clock for deadlines and durations. //
         /////////////////////////////////////////////////////////////////
         for(HealthIndicator indicator : indicators)
         {
            String indicatorName = indicator.getName();
            try
            {
               long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
               Future<HealthCheckResult> future = executorService.submit(() -> runIndicator(indicator, deadline));
               submittedChecks.add(new SubmittedCheck(indicatorName, deadline, future));
            }
            catch(Exception e)
            {
               checkResults.put(indicatorName, failureResult(indicatorName, e));
            }
         }

         for(SubmittedCheck check : submittedChecks)
         {
            try
            {
               checkResults.put(check.name(), awaitResult(check, timeoutMs));
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
               checkResults.put(check.name(), failureResult(check.name(), e));
            }
            catch(Exception e)
            {
               checkResults.put(check.name(), failureResult(check.name(), e));
            }
         }
      }
      finally
      {
         for(SubmittedCheck check : submittedChecks)
         {
            if(!check.future().isDone())
            {
               check.future().cancel(true);
            }
         }
      }

      ///////////////////////////////////////////
      // Aggregate results into overall status //
      ///////////////////////////////////////////
      HealthStatus[] statuses = checkResults.values().stream()
         .map(HealthCheckResult::getStatus)
         .toArray(HealthStatus[]::new);

      HealthStatus overallStatus = HealthStatus.aggregate(statuses);

      return response
         .withStatus(overallStatus)
         .withChecks(checkResults);
   }



   /*******************************************************************************
    ** A check and its submission deadline, including time spent in the queue.
    *******************************************************************************/
   private record SubmittedCheck(String name, long deadline, Future<HealthCheckResult> future)
   {
   }



   /*******************************************************************************
    ** Run one check, recording its duration and rejecting late completion even
    ** if its future is already complete when the collecting thread reaches it.
    *******************************************************************************/
   private HealthCheckResult runIndicator(HealthIndicator indicator, long deadline) throws TimeoutException
   {
      long startTime = System.nanoTime();
      if(startTime - deadline >= 0)
      {
         throw new TimeoutException();
      }

      HealthCheckResult result;
      try
      {
         result = indicator.check(qInstance);
         if(result.getDurationMs() == null)
         {
            result.withDurationMs(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
         }
      }
      catch(Exception e)
      {
         result = failureResult(indicator.getName(), new Exception("Indicator execution failed", e));
      }

      if(System.nanoTime() - deadline >= 0)
      {
         throw new TimeoutException();
      }
      return result;
   }



   /*******************************************************************************
    ** Collect a submitted check using only its remaining budget. A result that
    ** finished within budget remains valid even if collected after the deadline.
    *******************************************************************************/
   private HealthCheckResult awaitResult(SubmittedCheck check, int timeoutMs) throws Exception
   {
      try
      {
         return check.future().get(Math.max(0, check.deadline() - System.nanoTime()), TimeUnit.NANOSECONDS);
      }
      catch(ExecutionException e)
      {
         if(!(e.getCause() instanceof TimeoutException))
         {
            throw new Exception("Indicator execution failed", e.getCause());
         }
      }
      catch(TimeoutException e)
      {
         check.future().cancel(true);
      }

      LOG.warn("Health indicator timed out", logPair("indicator", check.name()), logPair("timeoutMs", timeoutMs));
      return new HealthCheckResult()
         .withStatus(HealthStatus.UNKNOWN)
         .withDurationMs((long) timeoutMs)
         .withDetail("error", "Health check timed out")
         .withDetail("timeoutMs", timeoutMs);
   }



   /*******************************************************************************
    ** Preserve the existing UNKNOWN response and exception details for failures.
    *******************************************************************************/
   private HealthCheckResult failureResult(String indicatorName, Exception e)
   {
      LOG.warn("Health indicator failed", logPair("indicator", indicatorName), e);
      return new HealthCheckResult()
         .withStatus(HealthStatus.UNKNOWN)
         .withDetail("error", e.getMessage())
         .withDetail("exceptionType", e.getClass().getSimpleName());
   }



   /*******************************************************************************
    ** Shutdown the executor service.
    ** Call this when done with the executor to clean up threads.
    *******************************************************************************/
   public void shutdown()
   {
      if(executorService != null && !executorService.isShutdown())
      {
         executorService.shutdown();
         try
         {
            if(!executorService.awaitTermination(1, TimeUnit.SECONDS))
            {
               executorService.shutdownNow();
            }
         }
         catch(InterruptedException e)
         {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
         }
      }
   }
}
