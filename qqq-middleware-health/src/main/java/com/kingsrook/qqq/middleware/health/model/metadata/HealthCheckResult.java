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
