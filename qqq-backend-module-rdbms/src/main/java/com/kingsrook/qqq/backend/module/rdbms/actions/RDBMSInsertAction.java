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
import java.util.List;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.interfaces.InsertInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 **
 *******************************************************************************/
public class RDBMSInsertAction extends AbstractRDBMSAction implements InsertInterface
{
   private static final QLogger LOG = QLogger.getLogger(RDBMSInsertAction.class);



   /*******************************************************************************
    **
    *******************************************************************************/
   public InsertOutput execute(InsertInput insertInput) throws QException
   {
      InsertOutput   rs    = new InsertOutput();
      QTableMetaData table = insertInput.getTable();
      setBackendMetaData(insertInput.getBackend());

      Connection connection            = null;
      boolean    needToCloseConnection = false;

      StringBuilder sql    = null;
      List<Object>  params = null;
      Long          mark   = null;

      try
      {
         List<QFieldMetaData> insertableFields = table.getFields().values().stream()
            .filter(field -> !field.getName().equals("id")) // todo - intent here is to avoid non-insertable fields.
            .toList();

         String columns = insertableFields.stream()
            .map(f -> escapeIdentifier(getColumnName(f)))
            .collect(Collectors.joining(", "));
         String questionMarks = insertableFields.stream()
            .map(x -> "?")
            .collect(Collectors.joining(", "));

         List<QRecord> outputRecords = new ArrayList<>();
         rs.setRecords(outputRecords);

         if(insertInput.getTransaction() != null && insertInput.getTransaction() instanceof RDBMSTransaction rdbmsTransaction)
         {
            connection = rdbmsTransaction.getConnection();
         }
         else
         {
            connection = getConnection(insertInput);
            needToCloseConnection = true;
         }

         for(List<QRecord> page : CollectionUtils.getPages(insertInput.getRecords(), getActionStrategy().getPageSize(insertInput)))
         {
            String backendTableName = escapeIdentifier(getTableName(table));
            sql = new StringBuilder("INSERT INTO ").append(backendTableName).append("(").append(columns).append(") VALUES");
            params = new ArrayList<>();
            int recordIndex = 0;

            //////////////////////////////////////////////////////
            // for each record in the page:                     //
            // - if it has errors, skip it                      //
            // - else add a "(?,?,...,?)," clause to the INSERT //
            // - then add all fields into the params list       //
            //////////////////////////////////////////////////////
            for(QRecord record : page)
            {
               if(CollectionUtils.nullSafeHasContents(record.getErrors()))
               {
                  continue;
               }

               if(recordIndex++ > 0)
               {
                  sql.append(",");
               }
               sql.append("(").append(questionMarks).append(")");

               for(QFieldMetaData field : insertableFields)
               {
                  Serializable value = record.getValue(field.getName());
                  value = scrubValue(field, value);
                  params.add(value);
               }
            }

            ////////////////////////////////////////////////////////////////////////////////////////
            // if all records had errors, copy them to the output, and continue w/o running query //
            ////////////////////////////////////////////////////////////////////////////////////////
            if(recordIndex == 0)
            {
               for(QRecord record : page)
               {
                  QRecord outputRecord = new QRecord(record);
                  if(!StringUtils.hasContent(outputRecord.getTableName()))
                  {
                     outputRecord.setTableName(table.getName());
                  }
                  outputRecords.add(outputRecord);
               }
               continue;
            }

            mark = System.currentTimeMillis();

            ///////////////////////////////////////////////////////////
            // execute the insert, then foreach record in the input, //
            // add it to the output, and set its generated id too.   //
            ///////////////////////////////////////////////////////////
            // todo sql customization - can edit sql and/or param list
            // todo - non-serial-id style tables
            // todo - other generated values, e.g., createDate...  maybe need to re-select?
            List<Serializable> idList = getActionStrategy().executeInsertForGeneratedIds(connection, sql.toString(), params, table.getField(table.getPrimaryKeyField()));
            int                index  = 0;
            for(QRecord record : page)
            {
               QRecord outputRecord = new QRecord(record);
               if(!StringUtils.hasContent(outputRecord.getTableName()))
               {
                  outputRecord.setTableName(table.getName());
               }
               outputRecords.add(outputRecord);

               if(CollectionUtils.nullSafeIsEmpty(record.getErrors()))
               {
                  if(idList.size() > index)
                  {
                     Serializable id = idList.get(index++);
                     outputRecord.setValue(table.getPrimaryKeyField(), id);
                  }
               }
            }

            logSQL(sql, params, mark);
         }

         return rs;
      }
      catch(Exception e)
      {
         logSQL(sql, params, mark);
         throw new QException("Error executing insert: " + e.getMessage(), e);
      }
      finally
      {
         if(needToCloseConnection && connection != null)
         {
            try
            {
               connection.close();
            }
            catch(SQLException se)
            {
               LOG.error("Error closing database connection", se);
            }
         }
      }

   }

}
