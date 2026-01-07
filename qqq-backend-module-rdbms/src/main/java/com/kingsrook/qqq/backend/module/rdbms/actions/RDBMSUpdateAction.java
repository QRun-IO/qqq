/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.interfaces.UpdateInterface;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UpdateActionRecordSplitHelper;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ListingHash;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Only the fields which exist in the record's values map will be updated.
 ** Note the difference between a field being in the value map, with a null value,
 ** vs. not being in the map.  If the field (its key) is in the value map, with a
 ** null value, then the field will be updated to NULL.  But if it's not in the
 ** map, then it'll be ignored.  This would be to do a PATCH type operation, vs a
 ** PUT.  See https://rapidapi.com/blog/put-vs-patch/
 *******************************************************************************/
public class RDBMSUpdateAction extends AbstractRDBMSAction implements UpdateInterface
{
   private static final QLogger LOG = QLogger.getLogger(RDBMSUpdateAction.class);

   private int statusCounter = 0;



   /*******************************************************************************
    **
    *******************************************************************************/
   public UpdateOutput execute(UpdateInput updateInput) throws QException
   {
      QTableMetaData table = updateInput.getTable();
      setBackendMetaData(updateInput.getBackend());

      UpdateActionRecordSplitHelper updateActionRecordSplitHelper = new UpdateActionRecordSplitHelper();
      updateActionRecordSplitHelper.init(updateInput);

      UpdateOutput rs = new UpdateOutput();
      rs.setRecords(updateActionRecordSplitHelper.getOutputRecords());

      if(!updateActionRecordSplitHelper.getHaveAnyWithoutErrors())
      {
         LOG.info("Exiting early - all records have some error.");
         return (rs);
      }

      try
      {
         Connection connection;
         boolean    needToCloseConnection = false;
         if(updateInput.getTransaction() != null && updateInput.getTransaction() instanceof RDBMSTransaction rdbmsTransaction)
         {
            connection = rdbmsTransaction.getConnection();
         }
         else
         {
            connection = getConnection(updateInput);
            needToCloseConnection = true;
         }

         try
         {
            /////////////////////////////////////////////////////////////////////////////////////////////
            // process each distinct list of fields being updated (e.g., each different SQL statement) //
            /////////////////////////////////////////////////////////////////////////////////////////////
            ListingHash<List<String>, QRecord> recordsByFieldBeingUpdated = updateActionRecordSplitHelper.getRecordsByFieldBeingUpdated();
            for(Map.Entry<List<String>, List<QRecord>> entry : recordsByFieldBeingUpdated.entrySet())
            {
               updateRecordsWithMatchingListOfFields(updateInput, connection, table, entry.getValue(), entry.getKey());
            }
         }
         finally
         {
            if(needToCloseConnection)
            {
               connection.close();
            }
         }

         return rs;
      }
      catch(Exception e)
      {
         // todo - how to communicate errors??? outputRecord.setErrors(new ArrayList<>(List.of(e)));
         throw new QException("Error executing update: " + e.getMessage(), e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void updateRecordsWithMatchingListOfFields(UpdateInput updateInput, Connection connection, QTableMetaData table, List<QRecord> recordList, List<String> fieldsBeingUpdated) throws SQLException
   {
      ////////////////////////////////////////////////////////////////////////////////
      // check for an optimization - if all of the records have the same values for //
      // all fields being updated, just do 1 update, with an IN list on the ids.    //
      ////////////////////////////////////////////////////////////////////////////////
      boolean allAreTheSame = UpdateActionRecordSplitHelper.areAllValuesBeingUpdatedTheSame(updateInput, recordList, fieldsBeingUpdated);
      if(allAreTheSame)
      {
         updateRecordsWithMatchingValuesAndFields(updateInput, connection, table, recordList, fieldsBeingUpdated);
         return;
      }

      String sql = writeUpdateSQLPrefix(table, fieldsBeingUpdated) + " = ?";

      // todo sql customization? - let each table have custom sql and/or param list

      ////////////////////////////////////////////////////////
      // build the list of list of values, from the records //
      ////////////////////////////////////////////////////////
      List<List<Serializable>> values = new ArrayList<>();
      for(QRecord record : recordList)
      {
         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            continue;
         }

         List<Serializable> rowValues = new ArrayList<>();
         values.add(rowValues);

         for(String fieldName : fieldsBeingUpdated)
         {
            Serializable value = record.getValue(fieldName);
            value = scrubValue(table.getField(fieldName), value);
            rowValues.add(value);
         }
         Serializable idValue = record.getValue(table.getPrimaryKeyField());
         idValue = scrubValue(table.getField(table.getPrimaryKeyField()), idValue);
         rowValues.add(idValue);
      }

      if(values.isEmpty())
      {
         ////////////////////////////////////////////////////////////////////////////////
         // if all records had errors, so we didn't push any values, then return early //
         ////////////////////////////////////////////////////////////////////////////////
         return;
      }

      Long mark = System.currentTimeMillis();

      ////////////////////////////////////////////////////////////////////////////////
      // let query manager do the batch updates - note that it will internally page //
      ////////////////////////////////////////////////////////////////////////////////
      try
      {
         getActionStrategy().executeBatchUpdate(connection, sql, values);
         incrementStatus(updateInput, recordList.size());
      }
      finally
      {
         logSQL(sql, values, mark);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String writeUpdateSQLPrefix(QTableMetaData table, List<String> fieldsBeingUpdated)
   {
      String columns = fieldsBeingUpdated.stream()
         .map(f -> escapeIdentifier(this.getColumnName(table.getField(f))) + " = ?")
         .collect(Collectors.joining(", "));

      String tableName = escapeIdentifier(getTableName(table));
      return ("UPDATE " + tableName
         + " SET " + columns
         + " WHERE " + escapeIdentifier(getColumnName(table.getField(table.getPrimaryKeyField()))) + " ");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void updateRecordsWithMatchingValuesAndFields(UpdateInput updateInput, Connection connection, QTableMetaData table, List<QRecord> recordList, List<String> fieldsBeingUpdated) throws SQLException
   {
      for(List<QRecord> page : CollectionUtils.getPages(recordList, getActionStrategy().getPageSize(updateInput)))
      {
         //////////////////////////////
         // skip records with errors //
         //////////////////////////////
         page = page.stream().filter(r -> CollectionUtils.nullSafeIsEmpty(r.getErrors())).collect(Collectors.toList());
         if(page.isEmpty())
         {
            continue;
         }

         String sql = writeUpdateSQLPrefix(table, fieldsBeingUpdated) + " IN (" + StringUtils.join(",", Collections.nCopies(page.size(), "?")) + ")";

         // todo sql customization? - let each table have custom sql and/or param list

         ////////////////////////////////////////////////////////////////
         // values in the update clause can come from the first record //
         ////////////////////////////////////////////////////////////////
         QRecord      record0 = page.get(0);
         List<Object> params  = new ArrayList<>();
         for(String fieldName : fieldsBeingUpdated)
         {
            Serializable value = record0.getValue(fieldName);
            value = scrubValue(table.getField(fieldName), value);
            params.add(value);
         }

         //////////////////////////////////////////////////////////////////////
         // values in the where clause (in list) are the id from each record //
         //////////////////////////////////////////////////////////////////////
         for(QRecord record : page)
         {
            Serializable idValue = record.getValue(table.getPrimaryKeyField());
            idValue = scrubValue(table.getField(table.getPrimaryKeyField()), idValue);
            params.add(idValue);
         }

         Long mark = System.currentTimeMillis();

         /////////////////////////////////////
         // let query manager do the update //
         /////////////////////////////////////
         try
         {
            getActionStrategy().executeUpdate(connection, sql, params);
            incrementStatus(updateInput, page.size());
         }
         finally
         {
            logSQL(sql, params, mark);
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void incrementStatus(UpdateInput updateInput, int amount)
   {
      statusCounter += amount;
      updateInput.getAsyncJobCallback().updateStatusOnlyUpwards(statusCounter, updateInput.getRecords().size());
   }

}
