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


import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordAssociatedScriptLogsInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordAssociatedScriptLogsOutputInterface;
import com.kingsrook.qqq.middleware.javalin.executors.utils.RecordDeveloperModeUtils;


/*******************************************************************************
 ** Get the logs of a record's associated script revision - requiring that the
 ** record be readable, as the legacy developer route does.
 *******************************************************************************/
public class RecordAssociatedScriptLogsExecutor extends AbstractMiddlewareExecutor<RecordAssociatedScriptLogsInput, RecordAssociatedScriptLogsOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(RecordAssociatedScriptLogsInput input, RecordAssociatedScriptLogsOutputInterface output) throws QException
   {
      RecordDeveloperModeUtils.checkRecordIsReadable(input.getTableName(), input.getPrimaryKey(), null);
      output.setScriptLogRecords(RecordDeveloperModeUtils.getScriptLogRecords(new QueryInput(), input.getScriptRevisionId()));
   }

}
