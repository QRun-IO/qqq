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

package com.kingsrook.qqq.middleware.javalin.executors;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.statusmessages.QStatusMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.middleware.javalin.AssociatedWritePermissions;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableInsertInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableInsertOutputInterface;
import com.kingsrook.qqq.middleware.javalin.executors.utils.TableWriteUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableInsertExecutor extends AbstractMiddlewareExecutor<TableInsertInput, TableInsertOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(TableInsertExecutor.class);



   /***************************************************************************
    ** Insert the record (with any associated records), checking the insert
    ** permission and the write permissions of every association, as the legacy
    ** insert route does.
    ***************************************************************************/
   @Override
   public void execute(TableInsertInput input, TableInsertOutputInterface output) throws QException
   {
      try
      {
         InsertInput insertInput = new InsertInput();
         insertInput.setTableName(input.getTableName());
         insertInput.setInputSource(QInputSource.USER);

         PermissionsHelper.checkTablePermissionThrowing(insertInput, TablePermissionSubType.INSERT);

         QRecord record = TableWriteUtils.recordToWrite(input.getTableName(), input.getRecord(), input.getRecordValues());
         insertInput.setRecords(new ArrayList<>(List.of(record)));
         AssociatedWritePermissions.check(insertInput);

         InsertOutput insertOutput = new InsertAction().execute(insertInput);
         QRecord      outputRecord = insertOutput.getRecords().get(0);
         TableWriteUtils.throwIfRecordErrors("inserting", input.getTableName(), outputRecord);

         if(CollectionUtils.nullSafeHasContents(outputRecord.getWarnings()))
         {
            output.setWarnings(outputRecord.getWarnings().stream().map(QStatusMessage::getMessage).toList());
         }

         output.setRecord(outputRecord);
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
         throw (new QException("Unexpected error occurred while executing insert: " + e.getMessage(), e));
      }
   }

}
