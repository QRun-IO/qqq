/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
    ** Insert one row using only native defaults. The default syntax is MySQL/H2.
    *******************************************************************************/
   default String getInsertDefaultValuesClause()
   {
      return "() VALUES ()";
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
