/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
      Optional<ProcessState> optionalProcessState = RunProcessAction.getState(input.getProcessUUID());
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
