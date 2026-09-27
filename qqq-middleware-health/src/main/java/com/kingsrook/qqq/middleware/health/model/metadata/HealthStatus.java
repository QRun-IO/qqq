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


/*******************************************************************************
 ** Enum representing the health status of a component or the overall system.
 **
 ** Status meanings:
 ** - UP: Component is functioning normally
 ** - DOWN: Component is not functioning (critical failure)
 ** - DEGRADED: Component is functioning but with reduced capability
 ** - UNKNOWN: Component status could not be determined
 *******************************************************************************/
public enum HealthStatus
{
   UP,
   DOWN,
   DEGRADED,
   UNKNOWN;



   /*******************************************************************************
    ** Determine the overall status from multiple component statuses.
    ** Rules:
    ** - If any component is DOWN, overall is DOWN
    ** - If all components are UP, overall is UP
    ** - If any component is DEGRADED (and none are DOWN), overall is DEGRADED
    ** - If any component is UNKNOWN (and none are DOWN/DEGRADED), overall is UNKNOWN
    *******************************************************************************/
   public static HealthStatus aggregate(HealthStatus... statuses)
   {
      if(statuses == null || statuses.length == 0)
      {
         return UNKNOWN;
      }

      boolean hasDown     = false;
      boolean hasDegraded = false;
      boolean hasUnknown  = false;

      for(HealthStatus status : statuses)
      {
         if(status == null)
         {
            hasUnknown = true;
            continue;
         }

         switch(status)
         {
            case DOWN -> hasDown = true;
            case DEGRADED -> hasDegraded = true;
            case UNKNOWN -> hasUnknown = true;
            case UP ->
            {
               // UP is good, continue
            }
            default ->
            {
               // Should never happen, but include default for Checkstyle
            }
         }
      }

      if(hasDown)
      {
         return DOWN;
      }
      if(hasDegraded)
      {
         return DEGRADED;
      }
      if(hasUnknown)
      {
         return UNKNOWN;
      }
      return UP;
   }
}

