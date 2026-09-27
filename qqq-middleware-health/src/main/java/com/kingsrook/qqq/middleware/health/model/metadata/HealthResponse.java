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

package com.kingsrook.qqq.middleware.health.model.metadata;


import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonInclude;


/*******************************************************************************
 ** Overall health check response containing the aggregate status and results
 ** from all individual health indicators.
 **
 ** This is the JSON response sent to clients hitting the /health endpoint.
 *******************************************************************************/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HealthResponse
{
   private HealthStatus                   status;
   private Instant                        timestamp;
   private Map<String, HealthCheckResult> checks;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public HealthResponse()
   {
      this.timestamp = Instant.now();
      this.checks = new HashMap<>();
   }



   /*******************************************************************************
    ** Fluent setter for status
    *******************************************************************************/
   public HealthResponse withStatus(HealthStatus status)
   {
      this.status = status;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for timestamp
    *******************************************************************************/
   public HealthResponse withTimestamp(Instant timestamp)
   {
      this.timestamp = timestamp;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for checks
    *******************************************************************************/
   public HealthResponse withChecks(Map<String, HealthCheckResult> checks)
   {
      this.checks = checks;
      return (this);
   }



   /*******************************************************************************
    ** Add a single check result
    *******************************************************************************/
   public HealthResponse withCheck(String name, HealthCheckResult result)
   {
      if(this.checks == null)
      {
         this.checks = new HashMap<>();
      }
      this.checks.put(name, result);
      return (this);
   }



   /*******************************************************************************
    ** Getter for status
    *******************************************************************************/
   public HealthStatus getStatus()
   {
      return (this.status);
   }



   /*******************************************************************************
    ** Setter for status
    *******************************************************************************/
   public void setStatus(HealthStatus status)
   {
      this.status = status;
   }



   /*******************************************************************************
    ** Getter for timestamp
    *******************************************************************************/
   public Instant getTimestamp()
   {
      return (this.timestamp);
   }



   /*******************************************************************************
    ** Setter for timestamp
    *******************************************************************************/
   public void setTimestamp(Instant timestamp)
   {
      this.timestamp = timestamp;
   }



   /*******************************************************************************
    ** Getter for checks
    *******************************************************************************/
   public Map<String, HealthCheckResult> getChecks()
   {
      return (this.checks);
   }



   /*******************************************************************************
    ** Setter for checks
    *******************************************************************************/
   public void setChecks(Map<String, HealthCheckResult> checks)
   {
      this.checks = checks;
   }
}
