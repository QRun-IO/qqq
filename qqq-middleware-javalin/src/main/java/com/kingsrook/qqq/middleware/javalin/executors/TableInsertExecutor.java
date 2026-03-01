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


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.QStatusMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableInsertInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableInsertOutputInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableInsertExecutor extends AbstractMiddlewareExecutor<TableInsertInput, TableInsertOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(TableInsertExecutor.class);



   /***************************************************************************
    **
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

         QRecord record = new QRecord();
         record.setTableName(input.getTableName());

         Map<String, Serializable> recordValues = input.getRecordValues();
         if(recordValues != null)
         {
            for(Map.Entry<String, Serializable> entry : recordValues.entrySet())
            {
               record.setValue(entry.getKey(), entry.getValue());
            }
         }

         List<QRecord> recordList = new ArrayList<>();
         recordList.add(record);
         insertInput.setRecords(recordList);

         InsertOutput insertOutput = new InsertAction().execute(insertInput);
         QRecord      outputRecord = insertOutput.getRecords().get(0);

         if(CollectionUtils.nullSafeHasContents(outputRecord.getErrors()))
         {
            QTableMetaData table = QContext.getQInstance().getTable(input.getTableName());
            String tableLabel = table != null ? table.getLabel() : input.getTableName();
            throw (new QUserFacingException("Error inserting " + tableLabel + ": "
               + StringUtils.joinWithCommasAndAnd(outputRecord.getErrors().stream().map(QStatusMessage::getMessage).toList())));
         }

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
