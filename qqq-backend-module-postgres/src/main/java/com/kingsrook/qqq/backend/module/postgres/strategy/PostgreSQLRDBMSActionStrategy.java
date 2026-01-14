/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.postgres.strategy;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.module.rdbms.strategy.BaseRDBMSActionStrategy;


/*******************************************************************************
 ** PostgreSQL specialization of the default RDBMS/JDBC action strategy.
 ** 
 ** This class provides PostgreSQL-specific implementations for database operations,
 ** including optimized insert operations using RETURNING clause, proper handling
 ** of temporal data types, and PostgreSQL-specific parameter binding behavior.
 *******************************************************************************/
public class PostgreSQLRDBMSActionStrategy extends BaseRDBMSActionStrategy
{

   /***************************************************************************
    ** PostgreSQL uses RETURNING clause for getting generated IDs.
    ** This is more efficient than the default JDBC approach.
    **
    ** @param connection the database connection
    ** @param sql the INSERT SQL statement
    ** @param params the query parameters
    ** @param primaryKeyField the primary key field metadata
    ** @return list of generated IDs
    ** @throws SQLException if a database error occurs
    ***************************************************************************/
   @Override
   public List<Serializable> executeInsertForGeneratedIds(
      Connection connection, String sql, List<Object> params,
      QFieldMetaData primaryKeyField) throws SQLException
   {
      String quoteString = getIdentifierQuoteString();
      sql = sql + " RETURNING " + quoteString + getColumnName(primaryKeyField) + quoteString;

      try(PreparedStatement statement = connection.prepareStatement(sql))
      {
         bindParams(params.toArray(), statement);
         incrementStatistic(STAT_QUERIES_RAN);
         statement.execute();

         ResultSet          resultSet = statement.getResultSet();
         List<Serializable> rs        = new ArrayList<>();
         while(resultSet.next())
         {
            rs.add(getFieldValueFromResultSet(
               primaryKeyField.getType(), resultSet, 1));
         }
         return (rs);
      }
   }



   /***************************************************************************
    ** Override parameter binding for PostgreSQL-specific handling.
    ** PostgreSQL JDBC driver requires explicit handling of temporal types
    ** and is stricter about type matching than other databases.
    **
    ** For null values, PostgreSQL needs Types.OTHER (not Types.CHAR) so it
    ** can infer the correct type from the column definition.
    **
    ** @param statement the prepared statement
    ** @param index the parameter index
    ** @param value the parameter value
    ** @return the number of parameters bound (always 1)
    ** @throws SQLException if a database error occurs
    ***************************************************************************/
   @Override
   protected int bindParamObject(PreparedStatement statement, int index,
      Object value) throws SQLException
   {
      if(value == null)
      {
         ////////////////////////////////////////////////////////////////////
         // PostgreSQL can infer the correct type from the column when     //
         // using Types.OTHER. This is better than Types.CHAR which causes //
         // type mismatch errors.                                          //
         ////////////////////////////////////////////////////////////////////
         statement.setNull(index, Types.OTHER);
         return 1;
      }
      else if(value instanceof Integer i)
      {
         statement.setInt(index, i);
         return 1;
      }
      else if(value instanceof Short s)
      {
         statement.setShort(index, s);
         return 1;
      }
      else if(value instanceof Long l)
      {
         statement.setLong(index, l);
         return 1;
      }
      else if(value instanceof Double d)
      {
         statement.setDouble(index, d);
         return 1;
      }
      else if(value instanceof Float f)
      {
         statement.setFloat(index, f);
         return 1;
      }
      else if(value instanceof Instant instant)
      {
         LocalDateTime localDateTime = LocalDateTime.ofInstant(instant,
            ZoneOffset.UTC);
         statement.setObject(index, localDateTime, Types.TIMESTAMP);
         return 1;
      }
      else if(value instanceof java.time.LocalDate localDate)
      {
         statement.setObject(index, localDate, Types.DATE);
         return 1;
      }
      else if(value instanceof LocalTime localTime)
      {
         statement.setObject(index, localTime, Types.TIME);
         return 1;
      }
      else if(value instanceof LocalDateTime localDateTime)
      {
         statement.setObject(index, localDateTime, Types.TIMESTAMP);
         return 1;
      }
      else
      {
         return super.bindParamObject(statement, index, value);
      }
   }



   /***************************************************************************
    ** Override bindParam for Integer to use Types.OTHER for nulls.
    ** This allows PostgreSQL to properly infer types in all contexts.
    **
    ** @param statement the prepared statement
    ** @param index the parameter index
    ** @param value the integer value (may be null)
    ** @throws SQLException if a database error occurs
    ***************************************************************************/
   @Override
   protected void bindParam(PreparedStatement statement, int index, Integer value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.OTHER);
      }
      else
      {
         statement.setInt(index, value);
      }
   }



   /***************************************************************************
    ** Override bindParam for Long to use Types.OTHER for nulls.
    ** This allows PostgreSQL to properly infer types in all contexts.
    **
    ** @param statement the prepared statement
    ** @param index the parameter index
    ** @param value the long value (may be null)
    ** @throws SQLException if a database error occurs
    ***************************************************************************/
   @Override
   protected void bindParam(PreparedStatement statement, int index, Long value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.OTHER);
      }
      else
      {
         statement.setLong(index, value);
      }
   }



   /***************************************************************************
    ** Override bindParam for Double to use Types.OTHER for nulls.
    ** This allows PostgreSQL to properly infer types in all contexts.
    **
    ** @param statement the prepared statement
    ** @param index the parameter index
    ** @param value the double value (may be null)
    ** @throws SQLException if a database error occurs
    ***************************************************************************/
   @Override
   protected void bindParam(PreparedStatement statement, int index, Double value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.OTHER);
      }
      else
      {
         statement.setDouble(index, value);
      }
   }



   /***************************************************************************
    ** Override bindParam for String to use Types.OTHER for nulls.
    ** This allows PostgreSQL to properly infer types in all contexts.
    **
    ** @param statement the prepared statement
    ** @param index the parameter index
    ** @param value the string value (may be null)
    ** @throws SQLException if a database error occurs
    ***************************************************************************/
   @Override
   protected void bindParam(PreparedStatement statement, int index, String value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.OTHER);
      }
      else
      {
         statement.setString(index, value);
      }
   }



   /***************************************************************************
    ** PostgreSQL uses double quotes for identifier quoting.
    **
    ** @return double quote string for identifier quoting
    ***************************************************************************/
   @Override
   public String getIdentifierQuoteString()
   {
      return "\"";
   }

}
