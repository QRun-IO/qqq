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

package com.kingsrook.qqq.backend.core.actions.processes;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;


/*******************************************************************************
 ** When a process/function can't run because it's missing data, this interface
 ** defines how the core framework goes back to a middleware (and possibly to a
 ** frontend) to get the data.
 *******************************************************************************/
public interface QProcessCallback
{
   /*******************************************************************************
    ** Get the filter query for this callback.
    *******************************************************************************/
   default QQueryFilter getQueryFilter()
   {
      return (null);
   }

   /*******************************************************************************
    ** Get the field values for this callback.
    *******************************************************************************/
   default Map<String, Serializable> getFieldValues(List<QFieldMetaData> fields)
   {
      return (null);
   }

   /***************************************************************************
    * Allow a callback to modify the query input that a process uses to find its
    * initial records (e.g., to add a transaction or turn on heavy fields).
    ***************************************************************************/
   default void customizeInputPreQuery(RunBackendStepInput runBackendStepInput, QueryInput queryInput)
   {
      /////////////////////
      // noop by default //
      /////////////////////
   }
}
