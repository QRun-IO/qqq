/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
