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


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordDeveloperModeInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordDeveloperModeOutputInterface;
import com.kingsrook.qqq.middleware.javalin.executors.utils.RecordDeveloperModeUtils;


/*******************************************************************************
 ** Get a record for developer mode, with its table's associated scripts - with
 ** the same permission checks and data as the legacy developer route.
 *******************************************************************************/
public class RecordDeveloperModeExecutor extends AbstractMiddlewareExecutor<RecordDeveloperModeInput, RecordDeveloperModeOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(RecordDeveloperModeInput input, RecordDeveloperModeOutputInterface output) throws QException
   {
      QRecord        record = RecordDeveloperModeUtils.getRecord(new GetInput(), input.getTableName(), input.getPrimaryKey());
      QTableMetaData table  = QContext.getQInstance().getTable(input.getTableName());

      output.setRecord(record);
      output.setAssociatedScripts(RecordDeveloperModeUtils.getAssociatedScripts(table, record, null));
   }

}
