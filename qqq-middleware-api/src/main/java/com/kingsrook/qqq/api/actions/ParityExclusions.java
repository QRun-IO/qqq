/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.api.actions;


import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;


/*******************************************************************************
 ** Explicit, reasoned exceptions to UI/model API parity. Names are core metadata
 ** names, not API aliases. Keep a separate exclusions object for each API/version.
 *******************************************************************************/
public class ParityExclusions
{
   private final Map<String, String> reasons = new LinkedHashMap<>();



   /*******************************************************************************
    ** Exclude a whole table, including its fields, permissions and capabilities.
    *******************************************************************************/
   public ParityExclusions withTable(String tableName, String reason)
   {
      return (add("table [" + requireText(tableName) + "]", reason));
   }



   /*******************************************************************************
    ** Exclude a whole process.
    *******************************************************************************/
   public ParityExclusions withProcess(String processName, String reason)
   {
      return (add("process [" + requireText(processName) + "]", reason));
   }



   /*******************************************************************************
    ** Exclude a physical or virtual field without excluding the rest of its table.
    *******************************************************************************/
   public ParityExclusions withField(String tableName, String fieldName, String reason)
   {
      return (add("table [" + requireText(tableName) + "] field [" + requireText(fieldName) + "]", reason));
   }



   /*******************************************************************************
    ** Allow a deliberately unavailable operation without excluding the table.
    *******************************************************************************/
   public ParityExclusions withCapability(String tableName, Capability capability, String reason)
   {
      return (add("table [" + requireText(tableName) + "] capability [" + Objects.requireNonNull(capability) + "]", reason));
   }



   /*******************************************************************************
    ** Return an immutable copy so a running check cannot consume caller state.
    *******************************************************************************/
   public Map<String, String> getReasons()
   {
      return (Map.copyOf(reasons));
   }



   /*******************************************************************************
    ** Reject undocumented exceptions at construction time.
    *******************************************************************************/
   private ParityExclusions add(String key, String reason)
   {
      reasons.put(key, requireText(reason));
      return (this);
   }



   /*******************************************************************************
    ** Names and reasons must be meaningful, including for programmatic callers.
    *******************************************************************************/
   private static String requireText(String value)
   {
      if(value == null || value.isBlank())
      {
         throw (new IllegalArgumentException("Parity exclusion names and reasons must be non-blank"));
      }
      return (value);
   }
}
