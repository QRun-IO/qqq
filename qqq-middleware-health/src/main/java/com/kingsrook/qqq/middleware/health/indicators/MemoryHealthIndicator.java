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

package com.kingsrook.qqq.middleware.health.indicators;


import java.util.HashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.health.HealthIndicator;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;


/*******************************************************************************
 ** Health indicator that checks JVM memory usage.
 **
 ** This indicator:
 ** - Checks heap memory usage percentage
 ** - Returns UP if below threshold
 ** - Returns DEGRADED if above threshold (but not critical)
 ** - Configurable threshold (default 90%)
 *******************************************************************************/
public class MemoryHealthIndicator implements HealthIndicator
{
   private Integer thresholdPercent = 90;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public MemoryHealthIndicator()
   {
   }



   /*******************************************************************************
    ** Get the name of this health indicator
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("memory");
   }



   /*******************************************************************************
    ** Execute the memory health check
    *******************************************************************************/
   @Override
   public HealthCheckResult check(QInstance qInstance) throws QException
   {
      long startTime = System.currentTimeMillis();

      Runtime runtime = Runtime.getRuntime();

      long maxMemory   = runtime.maxMemory();
      long totalMemory = runtime.totalMemory();
      long freeMemory  = runtime.freeMemory();
      long usedMemory  = totalMemory - freeMemory;

      ///////////////////////////////////////
      // Calculate memory usage percentage //
      ///////////////////////////////////////
      double usedPercent = (double) usedMemory / maxMemory * 100.0;

      Map<String, Object> details = new HashMap<>();
      details.put("maxBytes", maxMemory);
      details.put("usedBytes", usedMemory);
      details.put("freeBytes", maxMemory - usedMemory);
      details.put("usedPercent", String.format("%.2f", usedPercent));
      details.put("thresholdPercent", thresholdPercent);

      /////////////////////////////////////////
      // Determine status based on threshold //
      /////////////////////////////////////////
      HealthStatus status;
      if(usedPercent >= thresholdPercent)
      {
         status = HealthStatus.DEGRADED;
         details.put("warning", "Memory usage exceeds threshold");
      }
      else
      {
         status = HealthStatus.UP;
      }

      return new HealthCheckResult()
         .withStatus(status)
         .withDurationMs(System.currentTimeMillis() - startTime)
         .withDetails(details);
   }



   /*******************************************************************************
    ** Fluent setter for thresholdPercent
    *******************************************************************************/
   public MemoryHealthIndicator withThreshold(Integer thresholdPercent)
   {
      this.thresholdPercent = thresholdPercent;
      return (this);
   }



   /*******************************************************************************
    ** Getter for thresholdPercent
    *******************************************************************************/
   public Integer getThresholdPercent()
   {
      return (this.thresholdPercent);
   }



   /*******************************************************************************
    ** Setter for thresholdPercent
    *******************************************************************************/
   public void setThresholdPercent(Integer thresholdPercent)
   {
      this.thresholdPercent = thresholdPercent;
   }
}
