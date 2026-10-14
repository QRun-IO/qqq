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

package com.kingsrook.qqq.backend.core.health;


import java.util.ArrayList;
import java.util.List;


/*******************************************************************************
 ** HealthCheckOutcome
 *******************************************************************************/
public class HealthCheckOutcome
{
   private HealthStatus status = HealthStatus.UNKNOWN;
   private String message;
   private HealthDetails details = new HealthDetails();
   private Long latencyMs;
   private List<HealthMetric> metrics = new ArrayList<>();



   /*******************************************************************************
    ** Create a up outcome.
    *******************************************************************************/
   public static HealthCheckOutcome up(String message)
   {
      return (new HealthCheckOutcome().withStatus(HealthStatus.UP).withMessage(message));
   }



   /*******************************************************************************
    ** Create a degraded outcome.
    *******************************************************************************/
   public static HealthCheckOutcome degraded(String message)
   {
      return (new HealthCheckOutcome().withStatus(HealthStatus.DEGRADED).withMessage(message));
   }



   /*******************************************************************************
    ** Create a down outcome.
    *******************************************************************************/
   public static HealthCheckOutcome down(String message)
   {
      return (new HealthCheckOutcome().withStatus(HealthStatus.DOWN).withMessage(message));
   }



   /*******************************************************************************
    ** Create a unknown outcome.
    *******************************************************************************/
   public static HealthCheckOutcome unknown(String message)
   {
      return (new HealthCheckOutcome().withStatus(HealthStatus.UNKNOWN).withMessage(message));
   }



   /*******************************************************************************
    ** Getter for status
    *******************************************************************************/
   public HealthStatus getStatus()
   {
      return (status);
   }



   /*******************************************************************************
    ** Setter for status
    *******************************************************************************/
   public void setStatus(HealthStatus status)
   {
      this.status = status;
   }



   /*******************************************************************************
    ** Fluent setter for status
    *******************************************************************************/
   public HealthCheckOutcome withStatus(HealthStatus status)
   {
      setStatus(status);
      return (this);
   }



   /*******************************************************************************
    ** Getter for message
    *******************************************************************************/
   public String getMessage()
   {
      return (message);
   }



   /*******************************************************************************
    ** Setter for message
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    ** Fluent setter for message
    *******************************************************************************/
   public HealthCheckOutcome withMessage(String message)
   {
      setMessage(message);
      return (this);
   }



   /*******************************************************************************
    ** Getter for details
    *******************************************************************************/
   public HealthDetails getDetails()
   {
      return (details);
   }



   /*******************************************************************************
    ** Setter for details
    *******************************************************************************/
   public void setDetails(HealthDetails details)
   {
      this.details = details;
   }



   /*******************************************************************************
    ** Fluent setter for details
    *******************************************************************************/
   public HealthCheckOutcome withDetails(HealthDetails details)
   {
      setDetails(details);
      return (this);
   }



   /*******************************************************************************
    ** Getter for latencyMs
    *******************************************************************************/
   public Long getLatencyMs()
   {
      return (latencyMs);
   }



   /*******************************************************************************
    ** Setter for latencyMs
    *******************************************************************************/
   public void setLatencyMs(Long latencyMs)
   {
      this.latencyMs = latencyMs;
   }



   /*******************************************************************************
    ** Fluent setter for latencyMs
    *******************************************************************************/
   public HealthCheckOutcome withLatencyMs(Long latencyMs)
   {
      setLatencyMs(latencyMs);
      return (this);
   }



   /*******************************************************************************
    ** Getter for metrics
    *******************************************************************************/
   public List<HealthMetric> getMetrics()
   {
      return (metrics);
   }



   /*******************************************************************************
    ** Setter for metrics
    *******************************************************************************/
   public void setMetrics(List<HealthMetric> metrics)
   {
      this.metrics = metrics;
   }



   /*******************************************************************************
    ** Fluent setter for metrics
    *******************************************************************************/
   public HealthCheckOutcome withMetrics(List<HealthMetric> metrics)
   {
      setMetrics(metrics);
      return (this);
   }
}
