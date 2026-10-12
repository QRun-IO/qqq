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

package com.kingsrook.qqq.backend.core.alerts;


import java.util.Locale;


/*******************************************************************************
 ** How urgent an alert is.  Ordered from least to most severe, so
 ** isAtLeast / compareTo can be used for "minimum severity" checks.
 **
 ** An alert means "a person should do something".  INFO is for worth-a-glance
 ** items that never page anyone; external channels ignore it by default.
 *******************************************************************************/
public enum AlertSeverity
{
   INFO,
   WARNING,
   ERROR,
   CRITICAL;



   /*******************************************************************************
    ** True when this severity is the same as, or more severe than, the other.
    ** A null other is treated as INFO (so everything is at least it).
    *******************************************************************************/
   public boolean isAtLeast(AlertSeverity other)
   {
      return (other == null || this.compareTo(other) >= 0);
   }



   /*******************************************************************************
    ** Leniently map a severity label from another system (Prometheus, a
    ** producer's own enum, etc) to an AlertSeverity.  Case-insensitive; accepts
    ** common synonyms ("warn", "err", "fatal", "page", "info"/"informational").
    ** Returns the fallback for null, blank or unknown values.
    *******************************************************************************/
   public static AlertSeverity fromLabel(String label, AlertSeverity fallback)
   {
      if(label == null || label.isBlank())
      {
         return (fallback);
      }

      return switch(label.trim().toLowerCase(Locale.ROOT))
      {
         case "info", "informational", "notice", "low" -> INFO;
         case "warning", "warn", "medium" -> WARNING;
         case "error", "err", "high" -> ERROR;
         case "critical", "crit", "fatal", "page", "emergency" -> CRITICAL;
         default -> fallback;
      };
   }
}
