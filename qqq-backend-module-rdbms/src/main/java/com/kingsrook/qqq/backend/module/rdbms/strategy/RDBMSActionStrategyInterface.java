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

package com.kingsrook.qqq.backend.module.rdbms.strategy;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;


/*******************************************************************************
 **
 *******************************************************************************/
public interface RDBMSActionStrategyInterface
{

   /***************************************************************************
    * modifies the clause StringBuilder (appending to it)
    * returning the number of expected number of params to bind
    ***************************************************************************/
   Integer appendCriterionToWhereClause(QFilterCriteria criterion, StringBuilder clause, String column, List<Serializable> values, QFieldMetaData field);

   /***************************************************************************
    *
    ***************************************************************************/
   Serializable getFieldValueFromResultSet(QFieldType type, ResultSet resultSet, int i) throws SQLException;


   /***************************************************************************
    *
    ***************************************************************************/
   PreparedStatement executeUpdate(Connection connection, String sql, List<Object> params) throws SQLException;


   /***************************************************************************
    *
    ***************************************************************************/
   void executeBatchUpdate(Connection connection, String updateSQL, List<List<Serializable>> values) throws SQLException;


   /***************************************************************************
    *
    ***************************************************************************/
   List<Serializable> executeInsertForGeneratedIds(Connection connection, String sql, List<Object> params, QFieldMetaData primaryKeyField) throws SQLException;


   /***************************************************************************
    *
    ***************************************************************************/
   Integer executeUpdateForRowCount(Connection connection, String sql, Object... params) throws SQLException;


   /***************************************************************************
    *
    ***************************************************************************/
   void executeStatement(PreparedStatement statement, CharSequence sql, ResultSetProcessor processor, Object... params) throws SQLException, QException;


   /***************************************************************************
    *
    ***************************************************************************/
   Integer getPageSize(AbstractActionInput actionInput);


   /***************************************************************************
    ** Get the string to use for quoting identifiers (table names, column names).
    ** Default is backtick for MySQL/H2/SQLite. PostgreSQL should return empty string
    ** or double-quote.
    ***************************************************************************/
   default String getIdentifierQuoteString()
   {
      return "`";
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   @FunctionalInterface
   interface ResultSetProcessor
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      void processResultSet(ResultSet rs) throws SQLException, QException;
   }
}
