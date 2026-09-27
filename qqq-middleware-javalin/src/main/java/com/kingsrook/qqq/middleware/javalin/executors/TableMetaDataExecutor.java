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


import com.kingsrook.qqq.backend.core.actions.metadata.TableMetaDataAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionCheckResult;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableMetaDataInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableMetaDataOutputInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableMetaDataExecutor extends AbstractMiddlewareExecutor<TableMetaDataInput, TableMetaDataOutputInterface>
{

   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(TableMetaDataInput input, TableMetaDataOutputInterface output) throws QException
   {
      com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput tableMetaDataInput = new com.kingsrook.qqq.backend.core.model.actions.metadata.TableMetaDataInput();

      String         tableName = input.getTableName();
      QTableMetaData table     = QContext.getQInstance().getTable(tableName);
      if(table == null)
      {
         throw (new QNotFoundException("Table [" + tableName + "] was not found."));
      }

      tableMetaDataInput.setTableName(tableName);
      tableMetaDataInput.setInputSource(QInputSource.USER);
      if(PermissionsHelper.getPermissionCheckResult(tableMetaDataInput, table).equals(PermissionCheckResult.DENY_HIDE))
      {
         throw (new QNotFoundException("Table [" + tableName + "] was not found."));
      }

      TableMetaDataAction tableMetaDataAction = new TableMetaDataAction();
      TableMetaDataOutput tableMetaDataOutput = tableMetaDataAction.execute(tableMetaDataInput);

      output.setTableMetaData(tableMetaDataOutput.getTable());
   }

}
