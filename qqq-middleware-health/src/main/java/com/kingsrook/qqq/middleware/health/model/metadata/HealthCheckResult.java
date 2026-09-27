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


import java.util.HashMap;
import java.util.Map;


/*******************************************************************************
 ** Result of executing a single health indicator check.
 **
 ** Contains the status, execution duration, and optional details about the
 ** component being checked.
 *******************************************************************************/
public class HealthCheckResult
{
   private HealthStatus        status;
   private Long                durationMs;
   private Map<String, Object> details;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public HealthCheckResult()
   {
   }



   /*******************************************************************************
    ** Fluent setter for status
    *******************************************************************************/
   public HealthCheckResult withStatus(HealthStatus status)
   {
      this.status = status;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for durationMs
    *******************************************************************************/
   public HealthCheckResult withDurationMs(Long durationMs)
   {
      this.durationMs = durationMs;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for details
    *******************************************************************************/
   public HealthCheckResult withDetails(Map<String, Object> details)
   {
      this.details = details;
      return (this);
   }



   /*******************************************************************************
    ** Add a single detail entry
    *******************************************************************************/
   public HealthCheckResult withDetail(String key, Object value)
   {
      if(this.details == null)
      {
         this.details = new HashMap<>();
      }
      this.details.put(key, value);
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
    ** Getter for durationMs
    *******************************************************************************/
   public Long getDurationMs()
   {
      return (this.durationMs);
   }



   /*******************************************************************************
    ** Setter for durationMs
    *******************************************************************************/
   public void setDurationMs(Long durationMs)
   {
      this.durationMs = durationMs;
   }



   /*******************************************************************************
    ** Getter for details
    *******************************************************************************/
   public Map<String, Object> getDetails()
   {
      return (this.details);
   }



   /*******************************************************************************
    ** Setter for details
    *******************************************************************************/
   public void setDetails(Map<String, Object> details)
   {
      this.details = details;
   }
}
