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
