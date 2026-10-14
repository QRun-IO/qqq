/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.interfaces.ConditionalUpdateInterface;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.JoinsContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.ConditionalUpdateInput;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;


/*******************************************************************************
 ** RDBMS conditional update: one {@code UPDATE t SET ... WHERE <filter>}
 ** statement, so the compare and the set are atomic in the database. Runs in the
 ** caller's transaction when one is given.
 *******************************************************************************/
public class RDBMSConditionalUpdateAction extends AbstractRDBMSAction implements ConditionalUpdateInterface
{
   private static final DateTimeFormatter SQL_UTC = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss[.SSSSSS][.SSS]");



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int execute(ConditionalUpdateInput input) throws QException
   {
      setBackendMetaData(input.getBackend());
      QTableMetaData table = input.getTable();

      List<Serializable> params = new ArrayList<>();
      StringBuilder      sets   = new StringBuilder();
      for(Map.Entry<String, Serializable> entry : input.getValues().entrySet())
      {
         QFieldMetaData field = table.getField(entry.getKey());
         if(!sets.isEmpty())
         {
            sets.append(", ");
         }
         sets.append(escapeIdentifier(getColumnName(field))).append(" = ?");
         params.add(scrubValue(field, entry.getValue()));
      }

      JoinsContext joinsContext = new JoinsContext(QContext.getQInstance(), table.getName(), new ArrayList<>(), input.getFilter());
      String       whereClause  = makeWhereClause(joinsContext, input.getFilter(), params);

      String sql = "UPDATE " + escapeIdentifier(getTableName(table)) + " AS " + escapeIdentifier(table.getName())
         + " SET " + sets
         + " WHERE " + whereClause;

      long mark = System.currentTimeMillis();
      try
      {
         if(input.getTransaction() instanceof RDBMSTransaction rdbmsTransaction)
         {
            return (getActionStrategy().executeUpdateForRowCount(rdbmsTransaction.getConnection(), sql, params.toArray()));
         }

         try(Connection connection = getConnection(input))
         {
            return (getActionStrategy().executeUpdateForRowCount(connection, sql, params.toArray()));
         }
      }
      catch(Exception e)
      {
         throw (new QException("Error executing conditional update: " + e.getMessage(), e));
      }
      finally
      {
         logSQL(sql, params, mark);
      }
   }



   /*******************************************************************************
    ** The database clock, read with one SELECT. Vendors differ in how they hand
    ** back the current time, so each is read as UTC explicitly.
    *******************************************************************************/
   @Override
   public Instant databaseNow(QBackendMetaData backend) throws QException
   {
      RDBMSBackendMetaData rdbmsBackend = (RDBMSBackendMetaData) backend;
      String               vendor       = rdbmsBackend.getVendor() == null ? "" : rdbmsBackend.getVendor();
      String sql = switch(vendor)
      {
         case RDBMSBackendMetaData.VENDOR_MYSQL, RDBMSBackendMetaData.VENDOR_AURORA_MYSQL -> "SELECT DATE_FORMAT(UTC_TIMESTAMP(6), '%Y-%m-%d %H:%i:%s.%f')";
         case "sqlite" -> "SELECT strftime('%Y-%m-%d %H:%M:%f', 'now')";
         default -> "SELECT CURRENT_TIMESTAMP";
      };

      try(Connection connection = ConnectionManager.getConnection(rdbmsBackend); PreparedStatement statement = connection.prepareStatement(sql); ResultSet resultSet = statement.executeQuery())
      {
         resultSet.next();
         if(sql.equals("SELECT CURRENT_TIMESTAMP"))
         {
            OffsetDateTime offsetDateTime = resultSet.getObject(1, OffsetDateTime.class);
            return (offsetDateTime.toInstant());
         }
         return (LocalDateTime.parse(resultSet.getString(1), SQL_UTC).toInstant(ZoneOffset.UTC));
      }
      catch(Exception e)
      {
         throw (new QException("Error reading the database clock: " + e.getMessage(), e));
      }
   }

}
