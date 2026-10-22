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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyGuarantee;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistentReadInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateCapability;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.metadata.fields.JsonPathStorage;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;


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
    ** Whether this database supports SELECT ... FOR UPDATE SKIP LOCKED (for
    ** QueryLockMode.FOR_UPDATE_SKIP_LOCKED). False by default.
    ***************************************************************************/
   default boolean supportsForUpdateSkipLocked()
   {
      return (false);
   }



   /***************************************************************************
    ** Get the string to use for quoting identifiers (table names, column names).
    ** Default is backtick for MySQL/H2/SQLite. PostgreSQL should return empty string
    ** or double-quote.
    ***************************************************************************/
   default String getIdentifierQuoteString()
   {
      return "`";
   }



   /***************************************************************************
    ** Hook for a vendor strategy to configure a prepared statement for a query that
    ** carries the POTENTIALLY_LARGE_NUMBER_OF_RESULTS hint, so that the driver streams
    ** the results rather than buffering them all in memory.  Default: no-op.
    ***************************************************************************/
   default void configureStatementForLargeResults(Connection connection, PreparedStatement statement) throws SQLException
   {
   }



   /*******************************************************************************
    ** Insert one row using only native defaults. The default syntax is MySQL/H2.
    *******************************************************************************/
   default String getInsertDefaultValuesClause()
   {
      return "() VALUES ()";
   }


   /*******************************************************************************
    ** Start a transaction on the (autocommit-off) connection, such that every
    ** read through it sees one snapshot - or, for a database with no better way,
    ** at least asks for REPEATABLE READ isolation, and reports BEST_EFFORT.
    ** Vendor-specific strategies override this with the vendor's snapshot syntax.
    **
    ** Called by RDBMSBackendModule.openConsistentReadTransaction, before any
    ** statement has run on the connection.
    *******************************************************************************/
   default ConsistentReadStart beginConsistentRead(Connection connection, ConsistentReadInput input) throws SQLException
   {
      if(input.getImportSnapshotId().isPresent())
      {
         throw (new SQLException("Importing a snapshot is not supported by this database"));
      }

      connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
      return (new ConsistentReadStart(Optional.empty(), ConsistencyGuarantee.BEST_EFFORT));
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


   /*******************************************************************************
    ** Render a typed JSON path. Unsupported dialects fail explicitly.
    *******************************************************************************/
   default String jsonPathExpression(String column, JsonPathStorage storage, QFieldType type)
   {
      throw (new UnsupportedOperationException("JsonPathStorage requires PostgreSQL or the memory backend"));
   }



   /*******************************************************************************
    ** Placeholder for a serialized JSON object.
    *******************************************************************************/
   default String jsonObjectPlaceholder()
   {
      throw (new UnsupportedOperationException("JsonPathStorage requires PostgreSQL or the memory backend"));
   }



   /*******************************************************************************
    ** Atomic merge assignment for non-null keys and removed keys.
    *******************************************************************************/
   default String jsonMergeAssignment(String column)
   {
      throw (new UnsupportedOperationException("JsonPathStorage requires PostgreSQL or the memory backend"));
   }



   /*******************************************************************************
    ** Optional indexed JSON predicate. Values are already typed; replace bind values when used.
    *******************************************************************************/
   default String jsonPathPredicate(String column, QFieldMetaData field, QFilterCriteria criterion, List<Serializable> values)
   {
      return (null);
   }



   /*******************************************************************************
    ** Optional extensions; third-party strategies remain source compatible.
    *******************************************************************************/
   default Set<AggregateCapability> getSupportedAggregateCapabilities(AggregateInput input)
   {
      return (Set.of());
   }



   /*******************************************************************************
    ** Render the complete aggregate expression. Operands are resolved SQL, never client field names.
    *******************************************************************************/
   default String getAggregateExpression(AggregateOperator operator, String columnExpression, String otherColumnExpression, Aggregate aggregate, RDBMSBackendMetaData backend, List<Serializable> params)
   {
      return (operator.getSqlPrefix() + columnExpression + ")");
   }



   /*******************************************************************************
    ** Render a bucket around an already resolved SQL column.
    *******************************************************************************/
   default String getGroupByBucketExpression(GroupBy group, String column, QFieldType sourceType, RDBMSBackendMetaData backend, List<Serializable> params)
   {
      throw (new UnsupportedOperationException("Bucketed grouping is not supported"));
   }



   /*******************************************************************************
    ** Sample only the main table; called before rendering any joins.
    *******************************************************************************/
   default String getSampledTableExpression(String table, String alias, BigDecimal percent, List<Serializable> params)
   {
      return (table + " AS " + alias);
   }
}
