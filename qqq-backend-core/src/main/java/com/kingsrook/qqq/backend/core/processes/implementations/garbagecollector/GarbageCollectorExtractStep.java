/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.garbagecollector;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.collections.ListBuilder;


/*******************************************************************************
 **
 *******************************************************************************/
public class GarbageCollectorExtractStep extends ExtractViaQueryStep
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected QQueryFilter getQueryFilter(RunBackendStepInput runBackendStepInput) throws QException
   {
      //////////////////////////////////////////////////////////////////////////////////////////
      // in case the process was executed via a frontend, and the user specified a limitDate, //
      // then put that date in the defaultQueryFilter, rather than the default                //
      //////////////////////////////////////////////////////////////////////////////////////////
      Instant limitDate = ValueUtils.getValueAsInstant(runBackendStepInput.getValue("limitDate"));
      if(limitDate != null)
      {
         QQueryFilter defaultQueryFilter = (QQueryFilter) runBackendStepInput.getValue("defaultQueryFilter");
         defaultQueryFilter.getCriteria().get(0).setValues(ListBuilder.of(limitDate));
      }

      return super.getQueryFilter(runBackendStepInput);
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   protected void customizeInputPreQuery(RunBackendStepInput runBackendStepInput, QueryInput queryInput)
   {
      queryInput.withQueryHint(QueryHint.POTENTIALLY_LARGE_NUMBER_OF_RESULTS);
   }

}
