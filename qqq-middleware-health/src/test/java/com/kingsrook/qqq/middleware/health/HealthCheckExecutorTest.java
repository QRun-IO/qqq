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

