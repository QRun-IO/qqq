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

package com.kingsrook.qqq.backend.core.model.scripts;


/*******************************************************************************
 ** Where the source of a Script (project) may be executed.
 **
 ** The value is persisted (as its name) in the Script table's executionMode
 ** column, and is server-controlled and monotonic - see ScriptExecutionIsolation.
 **
 ** - LEGACY:  trusted, in-process execution (the historical behavior).  A Script
 **   with no stored value (every row that pre-dates the column) is LEGACY.
 ** - ISOLATED:  the source is fully untrusted, and may only be run by an isolated
 **   engine (e.g., qbit-scripting) - never by the in-process legacy executors.
 *******************************************************************************/
public enum ScriptExecutionMode
{
   LEGACY,
   ISOLATED;



   /*******************************************************************************
    ** Interpret a stored value.  No value means LEGACY (pre-existing, trusted,
    ** unmarked scripts); but any other value that isn't recognized fails closed
    ** as ISOLATED, so a corrupted or future marker never re-enables legacy
    ** execution.
    *******************************************************************************/
   public static ScriptExecutionMode fromValue(Object value)
   {
      if(value == null || value.toString().isBlank())
      {
         return (LEGACY);
      }

      for(ScriptExecutionMode mode : values())
      {
         if(mode.name().equals(value.toString().trim()))
         {
            return (mode);
         }
      }

      return (ISOLATED);
   }

}
