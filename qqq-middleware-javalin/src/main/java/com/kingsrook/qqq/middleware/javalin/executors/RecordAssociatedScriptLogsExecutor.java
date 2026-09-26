/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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
