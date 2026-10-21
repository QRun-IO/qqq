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


/*******************************************************************************
 ** How much of an alert an external channel (email, chat, paging, webhook) may
 ** include.  A channel has a level; an alert type has a ceiling, and the lower
 ** of the two applies.
 *******************************************************************************/
public enum DetailLevel
{
   /** severity, type label, group, title, age and one link back to the alert. */
   MINIMAL,

   /** MINIMAL plus the summary and the counts. */
   STANDARD,

   /** STANDARD plus details and record labels (admin opt-in). */
   FULL;



   /*******************************************************************************
    ** The lower (less revealing) of two levels.  Null means "no limit".
    *******************************************************************************/
   public static DetailLevel lowerOf(DetailLevel a, DetailLevel b)
   {
      if(a == null)
      {
         return (b);
      }
      if(b == null)
      {
         return (a);
      }
      return (a.compareTo(b) <= 0 ? a : b);
   }
}
