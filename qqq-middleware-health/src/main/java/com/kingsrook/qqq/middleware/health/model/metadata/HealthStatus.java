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

