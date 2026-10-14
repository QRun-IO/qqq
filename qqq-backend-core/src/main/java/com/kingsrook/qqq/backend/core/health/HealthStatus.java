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


import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;


/*******************************************************************************
 ** HealthStatus
 *******************************************************************************/
public enum HealthStatus implements PossibleValueEnum<String>
{
   UP, DEGRADED, DOWN, UNKNOWN;

   public static final String NAME = "healthStatus";



   /*******************************************************************************
    ** Possible value id
    *******************************************************************************/
   @Override
   public String getPossibleValueId()
   {
      return (name());
   }



   /*******************************************************************************
    ** Possible value label
    *******************************************************************************/
   @Override
   public String getPossibleValueLabel()
   {
      return (name());
   }



   /*******************************************************************************
    ** DOWN dominates DEGRADED, then UNKNOWN, then UP. Empty means UNKNOWN.
    *******************************************************************************/
   public static HealthStatus aggregate(HealthStatus... statuses)
   {
      if(statuses == null || statuses.length == 0)
      {
         return (UNKNOWN);
      }
      HealthStatus result = UP;
      for(HealthStatus status : statuses)
      {
         if(status == DOWN)
         {
            return (DOWN);
         }
         if(status == DEGRADED)
         {
            result = DEGRADED;
         }
         else if((status == null || status == UNKNOWN) && result == UP)
         {
            result = UNKNOWN;
         }
      }
      return (result);
   }
}
