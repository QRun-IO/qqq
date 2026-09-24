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
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


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

         QFieldMetaData primaryKeyField = table.getField(table.getPrimaryKeyField());
         for(List<QRecord> page : insertPages(insertInput, primaryKeyField))
         {
            boolean suppliedKeys = page.stream().filter(record -> CollectionUtils.nullSafeIsEmpty(record.getErrors()))
               .anyMatch(record -> scrubValue(primaryKeyField, record.getValue(primaryKeyField.getName())) != null);
            boolean zeroKey = suppliedKeys && page.size() == 1 && isZeroPrimaryKey(primaryKeyField, scrubValue(primaryKeyField, page.get(0).getValue(primaryKeyField.getName())));
            List<QFieldMetaData> insertableFields = table.getFields().values().stream()
               .filter(field -> suppliedKeys || !field.getName().equals(table.getPrimaryKeyField())).toList();
            String columns = insertableFields.stream().map(field -> escapeIdentifier(getColumnName(field))).collect(Collectors.joining(", "));
            String backendTableName = escapeIdentifier(getTableName(table));
            sql = new StringBuilder("INSERT INTO ").append(backendTableName);
            if(insertableFields.isEmpty())
            {
               sql.append(" ").append(getActionStrategy().getInsertDefaultValuesClause());
            }
            else
            {
               sql.append("(").append(columns).append(") VALUES");
            }
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
               if(insertableFields.isEmpty())
               {
                  recordIndex++;
                  continue;
               }

               if(recordIndex++ > 0)
               {
                  sql.append(",");
               }
               sql.append("(");
               for(int fieldIndex = 0; fieldIndex < insertableFields.size(); fieldIndex++)
               {
                  QFieldMetaData field = insertableFields.get(fieldIndex);
                  if(fieldIndex > 0)
                  {
                     sql.append(",");
                  }
                  sql.append("?");
                  params.add(scrubValue(field, record.getValue(field.getName())));
               }
               sql.append(")");
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
            // todo - other generated values, e.g., createDate...  maybe need to re-select?
            List<Serializable> idList = List.of();
            if(suppliedKeys && !zeroKey)
            {
               getActionStrategy().executeUpdateForRowCount(connection, sql.toString(), params.toArray());
            }
            else if(insertableFields.isEmpty())
            {
               idList = new ArrayList<>();
               for(int record = 0; record < recordIndex; record++)
               {
                  idList.addAll(getActionStrategy().executeInsertForGeneratedIds(connection, sql.toString(), params, table.getField(table.getPrimaryKeyField())));
               }
            }
            else
            {
               idList = getActionStrategy().executeInsertForGeneratedIds(connection, sql.toString(), params, table.getField(table.getPrimaryKeyField()));
            }
            if(zeroKey && (idList.size() > 1 || (idList.size() == 1 && idList.get(0) == null)))
            {
               throw new QException("Backend returned an invalid generated-key result for one zero-key insert");
            }
            int index = 0;
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



   /*******************************************************************************
    ** Keep supplied and generated keys in separate statements, in input order.
    ** Drivers need not return supplied keys alongside generated keys in a batch.
    *******************************************************************************/
   private List<List<QRecord>> insertPages(InsertInput input, QFieldMetaData primaryKeyField)
   {
      List<List<QRecord>> result = new ArrayList<>();
      for(List<QRecord> page : CollectionUtils.getPages(input.getRecords(), getActionStrategy().getPageSize(input)))
      {
         int start = 0;
         Boolean suppliedKeys = null;
         for(int index = 0; index < page.size(); index++)
         {
            QRecord record = page.get(index);
            if(CollectionUtils.nullSafeHasContents(record.getErrors()))
            {
               continue;
            }
            Serializable keyValue = scrubValue(primaryKeyField, record.getValue(primaryKeyField.getName()));
            if(isZeroPrimaryKey(primaryKeyField, keyValue))
            {
               //////////////////////////////////////////////////////////////////////////////////////////////////////
               // Some identity columns generate a new key from zero. Isolate it so returned keys cannot misalign. //
               //////////////////////////////////////////////////////////////////////////////////////////////////////
               if(index > start)
               {
                  result.add(page.subList(start, index));
               }
               result.add(page.subList(index, index + 1));
               start = index + 1;
               suppliedKeys = null;
               continue;
            }
            boolean supplied = keyValue != null;
            if(suppliedKeys != null && suppliedKeys != supplied)
            {
               result.add(page.subList(start, index));
               start = index;
            }
            suppliedKeys = supplied;
         }
         if(start < page.size())
         {
            result.add(page.subList(start, page.size()));
         }
      }
      return result;
   }



   /*******************************************************************************
    ** String natural keys keep their literal value, including "0" and empty text.
    *******************************************************************************/
   private boolean isZeroPrimaryKey(QFieldMetaData field, Serializable value)
   {
      return field.getType().isNumeric() && value != null && ValueUtils.getValueAsBigDecimal(value).signum() == 0;
   }

}
