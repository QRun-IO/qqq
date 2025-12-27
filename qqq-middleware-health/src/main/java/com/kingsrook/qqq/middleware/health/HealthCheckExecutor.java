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

package com.kingsrook.qqq.middleware.health;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
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
 ** - Running multiple indicators (potentially in parallel)
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

      ///////////////////////////////////////////
      // Execute each indicator (with timeout) //
      ///////////////////////////////////////////
      for(HealthIndicator indicator : indicators)
      {
         String indicatorName = indicator.getName();

         try
         {
            HealthCheckResult result = executeWithTimeout(indicator);
            checkResults.put(indicatorName, result);
         }
         catch(Exception e)
         {
            LOG.warn("Health indicator failed", logPair("indicator", indicatorName), e);
            checkResults.put(indicatorName, new HealthCheckResult()
               .withStatus(HealthStatus.UNKNOWN)
               .withDetail("error", e.getMessage())
               .withDetail("exceptionType", e.getClass().getSimpleName()));
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
    ** Execute a single indicator with timeout.
    **
    ** @param indicator the indicator to execute
    ** @return health check result
    ** @throws Exception if execution fails or times out
    *******************************************************************************/
   private HealthCheckResult executeWithTimeout(HealthIndicator indicator) throws Exception
   {
      Integer timeoutMs = config.getTimeoutMs();
      if(timeoutMs == null)
      {
         timeoutMs = DEFAULT_TIMEOUT_MS; // Default 5 second timeout
      }

      Callable<HealthCheckResult> task = () ->
      {
         long              startTime = System.currentTimeMillis();
         HealthCheckResult result    = indicator.check(qInstance);

         //////////////////////////////////////////////////
         // Set duration if not already set by indicator //
         //////////////////////////////////////////////////
         if(result.getDurationMs() == null)
         {
            result.withDurationMs(System.currentTimeMillis() - startTime);
         }

         return result;
      };

      Future<HealthCheckResult> future = executorService.submit(task);

      try
      {
         return future.get(timeoutMs, TimeUnit.MILLISECONDS);
      }
      catch(TimeoutException e)
      {
         future.cancel(true);
         LOG.warn("Health indicator timed out",
            logPair("indicator", indicator.getName()),
            logPair("timeoutMs", timeoutMs));

         return new HealthCheckResult()
            .withStatus(HealthStatus.UNKNOWN)
            .withDurationMs((long) timeoutMs)
            .withDetail("error", "Health check timed out")
            .withDetail("timeoutMs", timeoutMs);
      }
      catch(ExecutionException e)
      {
         throw new Exception("Indicator execution failed", e.getCause());
      }
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
