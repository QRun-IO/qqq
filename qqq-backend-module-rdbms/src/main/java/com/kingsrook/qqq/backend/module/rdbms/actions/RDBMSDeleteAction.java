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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.interfaces.DeleteInterface;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.JoinsContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.SystemErrorStatusMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class RDBMSDeleteAction extends AbstractRDBMSAction implements DeleteInterface
{
   private static final QLogger LOG = QLogger.getLogger(RDBMSDeleteAction.class);

   private static Integer PAGE_SIZE = 1000;


   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean supportsQueryFilterInput()
   {
      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public DeleteOutput execute(DeleteInput deleteInput) throws QException
   {
      setBackendMetaData(deleteInput.getBackend());

      DeleteOutput deleteOutput = new DeleteOutput();
      deleteOutput.setRecordsWithErrors(new ArrayList<>());

      /////////////////////////////////////////////////////////////////////////////////
      // Our strategy is:                                                            //
      // - if there's a query filter, try to do a delete WHERE that filter.          //
      // - - if that has an error, or if there wasn't a query filter, then continue: //
      // - if there's only 1 pkey to delete, just run a delete where $pkey=? query   //
      // - else if there's a list, try to delete it, but upon error:                 //
      // - - do a single-delete for each entry in the list.                          //
      /////////////////////////////////////////////////////////////////////////////////
      try
      {
         Connection connection;
         boolean    needToCloseConnection = false;
         if(deleteInput.getTransaction() != null && deleteInput.getTransaction() instanceof RDBMSTransaction rdbmsTransaction)
         {
            connection = rdbmsTransaction.getConnection();
         }
         else
         {
            connection = getConnection(deleteInput);
            needToCloseConnection = true;
         }

         try
         {
            ///////////////////////////////////////////////////////////////////////////////////////////////
            // if there's a query filter, try to do a single-delete with that filter in the WHERE clause //
            ///////////////////////////////////////////////////////////////////////////////////////////////
            if(deleteInput.getQueryFilter() != null)
            {
               try
               {
                  deleteInput.getAsyncJobCallback().updateStatus("Running bulk delete via query filter.");
                  deleteQueryFilter(connection, deleteInput, deleteOutput);
                  return (deleteOutput);
               }
               catch(Exception e)
               {
                  deleteInput.getAsyncJobCallback().updateStatus("Error running bulk delete via filter.  Fetching keys for individual deletes.");
                  LOG.info("Exception trying to delete by filter query.  Moving on to deleting by id now.", e);
                  deleteInput.setPrimaryKeys(DeleteAction.getPrimaryKeysFromQueryFilter(deleteInput));
               }
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // at this point, there either wasn't a query filter, or there was an error executing it (in which case, the query should //
            // have been converted to a list of primary keys in the deleteInput). so, proceed now by deleting a list of pkeys.        //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            deleteList(connection, deleteInput, deleteOutput);

            return (deleteOutput);
         }
         finally
         {
            if(needToCloseConnection)
            {
               connection.close();
            }
         }
      }
      catch(Exception e)
      {
         throw new QException("Error executing delete: " + e.getMessage(), e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void deleteList(Connection connection, DeleteInput deleteInput, DeleteOutput deleteOutput)
   {
      List<Serializable> primaryKeys = deleteInput.getPrimaryKeys();
      if(primaryKeys.size() == 0)
      {
         /////////////////////////
         // noop - just return. //
         /////////////////////////
         return;
      }
      if(primaryKeys.size() == 1)
      {
         doDeleteOne(connection, deleteInput.getTable(), primaryKeys.get(0), deleteOutput);
      }
      else
      {
         // todo - page this?  or binary-tree it?
         try
         {
            deleteInput.getAsyncJobCallback().updateStatus("Running bulk delete via key list.");
            doDeleteList(connection, deleteInput.getTable(), primaryKeys, deleteOutput);
         }
         catch(Exception e)
         {
            deleteInput.getAsyncJobCallback().updateStatus("Error running bulk delete via key list.  Performing individual deletes.");
            LOG.info("Caught an error doing list-delete - going to single-deletes now", e);
            int current = 1;
            for(Serializable primaryKey : primaryKeys)
            {
               deleteInput.getAsyncJobCallback().updateStatus(current++, primaryKeys.size());
               doDeleteOne(connection, deleteInput.getTable(), primaryKey, deleteOutput);
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void doDeleteOne(Connection connection, QTableMetaData table, Serializable primaryKey, DeleteOutput deleteOutput)
   {
      String tableName      = getTableName(table);
      String primaryKeyName = getColumnName(table.getField(table.getPrimaryKeyField()));

      // todo sql customization - can edit sql and/or param list?
      String sql = "DELETE FROM "
         + escapeIdentifier(tableName)
         + " WHERE "
         + escapeIdentifier(primaryKeyName) + " = ?";

      Long mark = System.currentTimeMillis();

      try
      {
         int rowCount = getActionStrategy().executeUpdateForRowCount(connection, sql, primaryKey);
         deleteOutput.addToDeletedRecordCount(rowCount);

         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // it seems like maybe we shouldn't do the below - ids that aren't found will hit this condition,  //
         // but we (1) don't care and (2) can't detect this case when doing an in-list delete, so, let's    //
         // make the results match, and just avoid adding to the deleted count, not marking it as an error. //
         /////////////////////////////////////////////////////////////////////////////////////////////////////
         // if(rowCount == 1)
         // {
         //    deleteOutput.addToDeletedRecordCount(1);
         // }
         // else
         // {
         //    LOG.debug("rowCount 0 trying to delete [" + tableName + "][" + primaryKey + "]");
         //    deleteOutput.addRecordWithError(new QRecord(table, primaryKey).withError("Record was not deleted (but no error was given from the database)"));
         // }
      }
      catch(Exception e)
      {
         LOG.debug("Exception trying to delete [" + tableName + "][" + primaryKey + "]", e);
         deleteOutput.addRecordWithError(new QRecord(table, primaryKey).withError(new SystemErrorStatusMessage("Record was not deleted: " + e.getMessage())));
      }
      finally
      {
         logSQL(sql, List.of(primaryKey), mark);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void doDeleteList(Connection connection, QTableMetaData table, List<Serializable> primaryKeys, DeleteOutput deleteOutput) throws QException
   {
      try
      {
         String tableName      = getTableName(table);
         String primaryKeyName = getColumnName(table.getField(table.getPrimaryKeyField()));

         for(List<Serializable> page : CollectionUtils.getPages(primaryKeys, PAGE_SIZE))
         {
            long mark = System.currentTimeMillis();
            String sql = "DELETE FROM "
               + escapeIdentifier(tableName)
               + " WHERE "
               + escapeIdentifier(primaryKeyName)
               + " IN ("
               + page.stream().map(x -> "?").collect(Collectors.joining(","))
               + ")";

            try
            {
               Integer rowCount = getActionStrategy().executeUpdateForRowCount(connection, sql, page);
               deleteOutput.addToDeletedRecordCount(rowCount);
            }
            finally
            {
               logSQL(sql, page, mark);
            }
         }
      }
      catch(Exception e)
      {
         throw new QException("Error executing delete: " + e.getMessage(), e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void deleteQueryFilter(Connection connection, DeleteInput deleteInput, DeleteOutput deleteOutput) throws QException
   {
      QQueryFilter       filter = deleteInput.getQueryFilter();
      List<Serializable> params = new ArrayList<>();
      QTableMetaData     table  = deleteInput.getTable();

      String       tableName    = getTableName(table);
      JoinsContext joinsContext = new JoinsContext(QContext.getQInstance(), table.getName(), new ArrayList<>(), deleteInput.getQueryFilter());
      String       whereClause  = makeWhereClause(joinsContext, filter, params);

      // todo sql customization - can edit sql and/or param list?
      String sql = "DELETE FROM "
         + escapeIdentifier(tableName) + " AS " + escapeIdentifier(table.getName())
         + " WHERE "
         + whereClause;

      Long mark = System.currentTimeMillis();

      try
      {
         int rowCount = getActionStrategy().executeUpdateForRowCount(connection, sql, params);
         deleteOutput.setDeletedRecordCount(rowCount);
      }
      catch(Exception e)
      {
         throw new QException("Error executing delete with filter: " + e.getMessage(), e);
      }
      finally
      {
         logSQL(sql, params, mark);
      }
   }



   /***************************************************************************
    * Change the global page size used for delete in-lists.  Default is 1000.
    ***************************************************************************/
   public static void setPageSize(Integer pageSize)
   {
      PAGE_SIZE = pageSize;
   }



   /***************************************************************************
    * Get the global page size value for delete in-lists.
    ***************************************************************************/
   public static Integer getPageSize()
   {
      return PAGE_SIZE;
   }
}
