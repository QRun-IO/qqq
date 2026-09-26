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


import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.health.indicators.MemoryHealthIndicator;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckMetaData;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthResponse;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for HealthCheckExecutor
 *******************************************************************************/
class HealthCheckExecutorTest
{

   /*******************************************************************************
    ** Test that executor runs indicators and aggregates results
    *******************************************************************************/
   @Test
   void testExecute_withIndicators_returnsAggregatedResponse()
   {
      QInstance qInstance = new QInstance();

      HealthCheckMetaData config = new HealthCheckMetaData()
         .withEnabled(true)
         .withIndicators(List.of(
            new MemoryHealthIndicator().withThreshold(99)
         ));

      HealthCheckExecutor executor = new HealthCheckExecutor(qInstance, config);
      HealthResponse response = executor.execute();

      assertThat(response).isNotNull();
      assertThat(response.getStatus()).isNotNull();
      assertThat(response.getTimestamp()).isNotNull();
      assertThat(response.getChecks()).isNotNull();
      assertThat(response.getChecks()).containsKey("memory");
      
      executor.shutdown();
   }



   /*******************************************************************************
    ** Test that executor handles empty indicator list
    *******************************************************************************/
   @Test
   void testExecute_noIndicators_returnsUnknown()
   {
      QInstance qInstance = new QInstance();

      HealthCheckMetaData config = new HealthCheckMetaData()
         .withEnabled(true)
         .withIndicators(List.of());

      HealthCheckExecutor executor = new HealthCheckExecutor(qInstance, config);
      HealthResponse response = executor.execute();

      assertThat(response).isNotNull();
      assertThat(response.getStatus()).isEqualTo(HealthStatus.UNKNOWN);
      
      executor.shutdown();
   }



   /*******************************************************************************
    ** Test that executor handles indicator that throws exception
    *******************************************************************************/
   @Test
   void testExecute_indicatorThrowsException_returnsUnknownForThatIndicator()
   {
      QInstance qInstance = new QInstance();

      HealthIndicator failingIndicator = new HealthIndicator()
      {
         @Override
         public String getName()
         {
            return "failing";
         }

         @Override
         public HealthCheckResult check(QInstance qInstance)
         {
            throw new RuntimeException("Simulated failure");
         }
      };

      HealthCheckMetaData config = new HealthCheckMetaData()
         .withEnabled(true)
         .withIndicators(List.of(failingIndicator));

      HealthCheckExecutor executor = new HealthCheckExecutor(qInstance, config);
      HealthResponse response = executor.execute();

      assertThat(response).isNotNull();
      assertThat(response.getChecks()).containsKey("failing");
      assertThat(response.getChecks().get("failing").getStatus()).isEqualTo(HealthStatus.UNKNOWN);
      assertThat(response.getChecks().get("failing").getDetails()).containsKey("error");
      
      executor.shutdown();
   }



   /*******************************************************************************
    ** Test that executor respects timeout
    *******************************************************************************/
   @Test
   void testExecute_indicatorTimesOut_returnsUnknown()
   {
      QInstance qInstance = new QInstance();

      HealthIndicator slowIndicator = new HealthIndicator()
      {
         @Override
         public String getName()
         {
            return "slow";
         }

         @Override
         public HealthCheckResult check(QInstance qInstance)
         {
            try
            {
               Thread.sleep(10000); // Sleep for 10 seconds
               return new HealthCheckResult().withStatus(HealthStatus.UP);
            }
            catch(InterruptedException e)
            {
               Thread.currentThread().interrupt();
               return new HealthCheckResult().withStatus(HealthStatus.UNKNOWN);
            }
         }
      };

      HealthCheckMetaData config = new HealthCheckMetaData()
         .withEnabled(true)
         .withIndicators(List.of(slowIndicator))
         .withTimeoutMs(100); // Very short timeout

      HealthCheckExecutor executor = new HealthCheckExecutor(qInstance, config);
      HealthResponse response = executor.execute();

      assertThat(response).isNotNull();
      assertThat(response.getChecks()).containsKey("slow");
      assertThat(response.getChecks().get("slow").getStatus()).isEqualTo(HealthStatus.UNKNOWN);
      assertThat(response.getChecks().get("slow").getDetails()).containsKey("error");
      assertThat(response.getChecks().get("slow").getDetails().get("error").toString()).contains("timed out");
      
      executor.shutdown();
   }
}

