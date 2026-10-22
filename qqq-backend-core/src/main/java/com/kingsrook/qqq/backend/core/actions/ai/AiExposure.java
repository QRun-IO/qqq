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

package com.kingsrook.qqq.backend.core.actions.ai;


/*******************************************************************************
 ** How much of a table or field the AI tool kernel may expose to a model.
 ** Ordered from most to least restrictive:
 **
 ** NONE - invisible to the assistant: omitted from descriptions, and rejected
 **    if named in a filter, field list or sort.
 ** METADATA_ONLY - the assistant knows it exists and may filter (or sort) on it
 **    server-side, but its values are never sent to the model.
 ** MASKED - values are sent partially masked (e.g., j***@example.com, ***1234).
 ** VALUES - values are sent (after redaction and truncation).
 **
 ** A table's exposure caps the exposure of each of its fields.
 *******************************************************************************/
public enum AiExposure
{
   NONE,
   METADATA_ONLY,
   MASKED,
   VALUES;



   /*******************************************************************************
    ** The more restrictive of this exposure and another (null is ignored).
    *******************************************************************************/
   public AiExposure mostRestrictive(AiExposure other)
   {
      if(other == null)
      {
         return (this);
      }
      return (other.ordinal() < this.ordinal() ? other : this);
   }



   /*******************************************************************************
    ** Whether values at this exposure may be sent to a model (masked or not).
    *******************************************************************************/
   public boolean allowsValues()
   {
      return (this == MASKED || this == VALUES);
   }
}
