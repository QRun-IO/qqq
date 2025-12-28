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

package com.kingsrook.qqq.middleware.health.indicators;


import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Unit test for MemoryHealthIndicator
 *******************************************************************************/
class MemoryHealthIndicatorTest
{

   /*******************************************************************************
    ** Test that memory indicator returns UP when below threshold
    *******************************************************************************/
   @Test
   void testCheck_belowThreshold_returnsUp() throws Exception
   {
      QInstance qInstance = new QInstance();
      
      MemoryHealthIndicator indicator = new MemoryHealthIndicator()
         .withThreshold(99); // Set very high threshold so we're definitely below it

      HealthCheckResult result = indicator.check(qInstance);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(HealthStatus.UP);
      assertThat(result.getDurationMs()).isNotNull();
      assertThat(result.getDetails()).isNotNull();
      assertThat(result.getDetails()).containsKey("usedPercent");
      assertThat(result.getDetails()).containsKey("maxBytes");
      assertThat(result.getDetails()).containsKey("usedBytes");
      assertThat(result.getDetails()).containsKey("freeBytes");
   }



   /*******************************************************************************
    ** Test that memory indicator returns DEGRADED when above threshold
    *******************************************************************************/
   @Test
   void testCheck_aboveThreshold_returnsDegraded() throws Exception
   {
      QInstance qInstance = new QInstance();
      
      MemoryHealthIndicator indicator = new MemoryHealthIndicator()
         .withThreshold(0); // Set threshold to 0 so any memory usage is above it

      HealthCheckResult result = indicator.check(qInstance);

      assertThat(result).isNotNull();
      assertThat(result.getStatus()).isEqualTo(HealthStatus.DEGRADED);
      assertThat(result.getDetails()).containsKey("warning");
   }



   /*******************************************************************************
    ** Test that indicator name is correct
    *******************************************************************************/
   @Test
   void testGetName_returnsMemory()
   {
      MemoryHealthIndicator indicator = new MemoryHealthIndicator();
      assertThat(indicator.getName()).isEqualTo("memory");
   }



   /*******************************************************************************
    ** Test fluent setter for threshold
    *******************************************************************************/
   @Test
   void testFluentSetters()
   {
      MemoryHealthIndicator indicator = new MemoryHealthIndicator()
         .withThreshold(75);

      assertThat(indicator.getThresholdPercent()).isEqualTo(75);
   }
}

