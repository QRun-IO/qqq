/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessState;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessRecordsInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.ProcessRecordsOutputInterface;


/*******************************************************************************
 ** Executor for fetching records from a process state.
 *******************************************************************************/
public class ProcessRecordsExecutor extends AbstractMiddlewareExecutor<ProcessRecordsInput, ProcessRecordsOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(ProcessRecordsInput input, ProcessRecordsOutputInterface output) throws QException
   {
      ExecutorSessionUtils.setTableVariantInSession(input.getTableVariant());

      ////////////////////////////////////////////////////////////////////////////
      // only the session that ran the process may read its records (as legacy) //
      ////////////////////////////////////////////////////////////////////////////
      Optional<ProcessState> optionalProcessState = RunProcessAction.getStateForUser(input.getProcessUUID(), input.getProcessName());
      if(optionalProcessState.isEmpty())
      {
         throw (new QException("Could not find process results."));
      }

      ProcessState  processState = optionalProcessState.get();
      List<QRecord> records      = processState.getRecords();

      if(records == null)
      {
         output.setRecords(new ArrayList<>());
         output.setTotalRecords(0);
      }
      else
      {
         List<QRecord> page = CollectionUtils.safelyGetPage(records, input.getSkip(), input.getLimit());
         output.setRecords(page);
         output.setTotalRecords(records.size());
      }
   }

}
