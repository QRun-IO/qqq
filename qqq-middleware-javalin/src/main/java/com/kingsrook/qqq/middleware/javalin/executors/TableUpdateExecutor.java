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
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.QStatusMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ExceptionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableUpdateInput;
import com.kingsrook.qqq.middleware.javalin.executors.io.TableUpdateOutputInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableUpdateExecutor extends AbstractMiddlewareExecutor<TableUpdateInput, TableUpdateOutputInterface>
{
   private static final QLogger LOG = QLogger.getLogger(TableUpdateExecutor.class);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void execute(TableUpdateInput input, TableUpdateOutputInterface output) throws QException
   {
      try
      {
         UpdateInput updateInput = new UpdateInput();
         updateInput.setTableName(input.getTableName());
         updateInput.setInputSource(QInputSource.USER);

         PermissionsHelper.checkTablePermissionThrowing(updateInput, TablePermissionSubType.EDIT);

         QTableMetaData tableMetaData = QContext.getQInstance().getTable(input.getTableName());

         QRecord record = new QRecord();
         record.setTableName(input.getTableName());
         record.setValue(tableMetaData.getPrimaryKeyField(), input.getPrimaryKey());

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
         updateInput.setRecords(recordList);

         UpdateOutput updateOutput = new UpdateAction().execute(updateInput);
         QRecord      outputRecord = updateOutput.getRecords().get(0);

         if(CollectionUtils.nullSafeHasContents(outputRecord.getErrors()))
         {
            String tableLabel = tableMetaData != null ? tableMetaData.getLabel() : input.getTableName();
            throw (new QUserFacingException("Error updating " + tableLabel + ": "
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
         throw (new QException("Unexpected error occurred while executing update: " + e.getMessage(), e));
      }
   }

}
