/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

import java.time.Instant;
import java.util.Locale;
import com.kingsrook.qqq.backend.core.health.HealthCheck;
import com.kingsrook.qqq.backend.core.health.HealthCheckInput;
import com.kingsrook.qqq.backend.core.health.HealthCheckKind;
import com.kingsrook.qqq.backend.core.health.HealthCheckMetaData;
import com.kingsrook.qqq.backend.core.health.HealthCheckOutcome;
import com.kingsrook.qqq.backend.core.health.HealthDetails;
import com.kingsrook.qqq.backend.core.health.HealthRedactor;
import com.kingsrook.qqq.middleware.health.model.metadata.HealthCheckResult;


/*******************************************************************************
 ** Compatibility boundary for existing application indicators.
 *******************************************************************************/
public class HealthIndicatorCheck implements HealthCheck
{
   private final HealthIndicator indicator;



   /*******************************************************************************
    ** Keep the application's existing implementation.
    *******************************************************************************/
   public HealthIndicatorCheck(HealthIndicator indicator)
   {
      this.indicator = indicator;
   }



   /*******************************************************************************
    ** Registry identifiers are lower case; legacy probe display names stay intact.
    *******************************************************************************/
   public String getName()
   {
      return ("core." + indicator.getName().toLowerCase(Locale.ROOT));
   }



   /*******************************************************************************
    ** Database indicators gate readiness; all other legacy indicators are local.
    *******************************************************************************/
   public HealthCheckMetaData toMetaData()
   {
      boolean database = "database".equalsIgnoreCase(indicator.getName());
      return (new HealthCheckMetaData().withName(getName()).withLabel(indicator.getName()).withComponent("core")
         .withKind(database ? HealthCheckKind.READINESS : HealthCheckKind.LIVENESS)
         .withIncludeInReadiness(database).withIntervalSeconds(30).withTimeoutMs(database ? 2000 : 1000).withCheckInstance(this));
   }



   /*******************************************************************************
    ** Only supported scalar details cross the core allow-list and redactor.
    *******************************************************************************/
   @Override
   public HealthCheckOutcome run(HealthCheckInput input)
   {
      try
      {
         HealthCheckResult result = indicator.check(input.getQInstance());
         HealthDetails details = new HealthDetails();
         if(result.getDetails() != null)
         {
            result.getDetails().forEach((key, value) ->
            {
               if(value instanceof String || value instanceof Number || value instanceof Boolean || value instanceof Instant)
               {
                  details.withValue(key, value);
               }
            });
         }
         return (new HealthCheckOutcome().withStatus(result.getStatus().toCore()).withLatencyMs(result.getDurationMs()).withDetails(details));
      }
      catch(Exception e)
      {
         return (HealthCheckOutcome.down(HealthRedactor.redactThrowable(e)));
      }
   }
}
