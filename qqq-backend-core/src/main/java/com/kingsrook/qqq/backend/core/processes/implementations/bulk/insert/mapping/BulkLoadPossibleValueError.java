/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.mapping;


import java.io.Serializable;


/*******************************************************************************
 ** Specialized error for records, for bulk-load use-cases, where we want to
 ** report back info to the user about the field & value.
 *******************************************************************************/
public class BulkLoadPossibleValueError extends AbstractBulkLoadRollableValueError
{
   private final String       fieldLabel;
   private final Serializable value;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BulkLoadPossibleValueError(String fieldName, Serializable value, String fieldLabel)
   {
      super("Value [" + value + "] for field [" + fieldLabel + "] is not a valid option");
      this.value = value;
      this.fieldLabel = fieldLabel;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getMessageToUseAsProcessSummaryRollupKey()
   {
      return ("Unrecognized value for field [" + fieldLabel + "]");
   }



   /*******************************************************************************
    ** Getter for value
    **
    *******************************************************************************/
   @Override
   public Serializable getValue()
   {
      return value;
   }
}
