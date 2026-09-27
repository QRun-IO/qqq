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

package com.kingsrook.qqq.middleware.javalin.executors;


import com.kingsrook.qqq.backend.core.actions.tables.RecordSearchAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchOutput;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordSearchMiddlewareInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordSearchOutputInterface;


/*******************************************************************************
 ** Executes record search across the tables that declare search fields, as
 ** the session's user (tables it may not read are skipped; record security
 ** locks apply).
 *******************************************************************************/
public class RecordSearchExecutor extends AbstractMiddlewareExecutor<RecordSearchMiddlewareInput, RecordSearchOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(RecordSearchMiddlewareInput input, RecordSearchOutputInterface output) throws QException
   {
      try
      {
         RecordSearchOutput searchOutput = new RecordSearchAction().execute(new RecordSearchInput()
            .withSearchTerm(input.getSearchTerm())
            .withTableNames(input.getTableNames())
            .withLimitPerTable(input.getLimitPerTable())
            .withInputSource(QInputSource.USER));

         output.setResults(searchOutput.getResults());
      }
      catch(QException e)
      {
         QUserFacingException userFacingException = ExceptionUtils.findClassInRootChain(e, QUserFacingException.class);
         if(userFacingException != null)
         {
            throw userFacingException;
         }

         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException("Unexpected error occurred while executing record search: " + e.getMessage(), e));
      }
   }

}
