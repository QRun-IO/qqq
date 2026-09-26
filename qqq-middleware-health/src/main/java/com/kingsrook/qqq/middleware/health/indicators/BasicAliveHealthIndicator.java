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


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.middleware.health.HealthIndicator;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthStatus;


/*******************************************************************************
 ** Basic health indicator that just reports UP always.
 **
 ** This indicator:
 ** - Basic health check that always returns UP
 *******************************************************************************/
public class BasicAliveHealthIndicator implements HealthIndicator
{


   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public BasicAliveHealthIndicator()
   {
   }



   /*******************************************************************************
    ** Get the name of this health indicator
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("basicAlive");
   }



   /*******************************************************************************
    ** Execute the memory health check
    *******************************************************************************/
   @Override
   public HealthCheckResult check(QInstance qInstance) throws QException
   {
      long startTime = System.currentTimeMillis();

      /////////////////////////////////////////
      // Determine status based on threshold //
      /////////////////////////////////////////
      HealthStatus status = HealthStatus.UP;

      return new HealthCheckResult().withStatus(status).withDurationMs(System.currentTimeMillis() - startTime);
   }
}
