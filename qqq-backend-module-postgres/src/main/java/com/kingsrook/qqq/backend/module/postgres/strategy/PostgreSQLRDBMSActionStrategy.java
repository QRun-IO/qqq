/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.module.postgres.strategy;


import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyGuarantee;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyScope;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistentReadInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateCapability;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucketUnit;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.NumericBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.metadata.fields.JsonPathStorage;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.module.rdbms.actions.JsonStorageValues;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.strategy.BaseRDBMSActionStrategy;
import com.kingsrook.qqq.backend.module.rdbms.strategy.ConsistentReadStart;


/*******************************************************************************
 ** PostgreSQL specialization of the default RDBMS/JDBC action strategy.
 ** 
 ** This class provides PostgreSQL-specific implementations for database operations,
 ** including optimized insert operations using RETURNING clause, proper handling
 ** of temporal data types, and PostgreSQL-specific parameter binding behavior.
 *******************************************************************************/
public class PostgreSQLRDBMSActionStrategy extends BaseRDBMSActionStrategy
{
   public static final int DEFAULT_LARGE_RESULT_FETCH_SIZE = 1_000;

   private int largeResultFetchSize = DEFAULT_LARGE_RESULT_FETCH_SIZE;



   /***************************************************************************
    ** The pgjdbc driver buffers the entire result set in memory unless the
    ** connection is in a transaction (autocommit off) and a fetch size is set
    ** (it then uses a server-side cursor).  So, for queries hinted as potentially
    ** large, when we are inside a transaction, set a fetch size.  Outside a
    ** transaction (autocommit on), a fetch size has no effect, so it is left alone.
    ***************************************************************************/
   @Override
   public void configureStatementForLargeResults(Connection connection, PreparedStatement statement) throws SQLException
   {
      if(largeResultFetchSize > 0 && !connection.getAutoCommit())
      {
         statement.setFetchSize(largeResultFetchSize);
      }
   }



   /*******************************************************************************
    ** Setter for largeResultFetchSize (0 disables)
    *******************************************************************************/
   public void setLargeResultFetchSize(int largeResultFetchSize)
   {
      this.largeResultFetchSize = largeResultFetchSize;
   }



   /*******************************************************************************
    ** Getter for largeResultFetchSize
    *******************************************************************************/
   public int getLargeResultFetchSize()
   {
      return largeResultFetchSize;
   }



   /*******************************************************************************
    ** PostgreSQL requires DEFAULT VALUES when no columns are supplied.
    *******************************************************************************/
   @Override
   public String getInsertDefaultValuesClause()
   {
      return "DEFAULT VALUES";
   }



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

   /*******************************************************************************
    ** PostgreSQL: REPEATABLE READ (READ ONLY, if so requested) - so every table
    ** reflects the one snapshot the transaction takes at its first statement.
    **
    ** For BACKEND scope, the snapshot is exported (pg_export_snapshot), and the id
    ** returned, so that other connections (parallel workers, pg_dump --snapshot)
    ** can attach to it with SET TRANSACTION SNAPSHOT (via importSnapshotId on the
    ** input).  The exporting transaction must stay open for as long as others may
    ** import.  Not done on a hot standby, which can't export snapshots.
    **
    ** Caveat:  a long-lived snapshot holds back vacuum (xmin), and, on a standby,
    ** can be cancelled by recovery conflicts (unless hot_standby_feedback is on).
    *******************************************************************************/
   @Override
   public ConsistentReadStart beginConsistentRead(Connection connection, ConsistentReadInput input) throws SQLException
   {
      try(Statement statement = connection.createStatement())
      {
         statement.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ" + (input.getReadOnly() ? " READ ONLY" : ""));

         if(input.getImportSnapshotId().isPresent())
         {
            ////////////////////////////////////////////////////////////////////////
            // SET can't take bind parameters - so be strict about what's in the  //
            // id (postgres snapshot ids look like 00000003-0000001B-1)           //
            ////////////////////////////////////////////////////////////////////////
            String snapshotId = input.getImportSnapshotId().get();
            if(!snapshotId.matches("[0-9A-Fa-f-]{1,64}"))
            {
               throw (new SQLException("Invalid snapshot id: " + snapshotId));
            }

            statement.execute("SET TRANSACTION SNAPSHOT '" + snapshotId + "'");
            return (new ConsistentReadStart(Optional.empty(), ConsistencyGuarantee.ONE_INSTANT));
         }

         boolean inRecovery = false;
         try(ResultSet resultSet = statement.executeQuery("SELECT pg_is_in_recovery()"))
         {
            inRecovery = resultSet.next() && resultSet.getBoolean(1);
         }

         if(input.getScope() == ConsistencyScope.BACKEND && !inRecovery)
         {
            try(ResultSet resultSet = statement.executeQuery("SELECT pg_export_snapshot()"))
            {
               if(resultSet.next())
               {
                  return (new ConsistentReadStart(Optional.of(resultSet.getString(1)), ConsistencyGuarantee.ONE_INSTANT));
               }
            }
         }

         return (new ConsistentReadStart(Optional.empty(), ConsistencyGuarantee.ONE_INSTANT));
      }
   }



   /*******************************************************************************
    ** Extract JSON scalars using the declared QQQ type.
    *******************************************************************************/
   @Override
   public String jsonPathExpression(String column, JsonPathStorage storage, QFieldType type)
   {
      if(storage.getKey() == null || !storage.getKey().matches("[a-z][a-z0-9_]{0,40}"))
      {
         throw (new IllegalArgumentException("Invalid JSON storage key"));
      }
      String cast = switch(type)
      {
         case STRING, TEXT -> "";
         case DECIMAL -> "::numeric";
         case LONG -> "::bigint";
         case INTEGER -> "::integer";
         case BOOLEAN -> "::boolean";
         case DATE -> "::date";
         case DATE_TIME -> "::timestamptz";
         default -> throw new IllegalArgumentException("Unsupported JSON field type: " + type);
      };
      return ("(" + column + "->>'" + storage.getKey() + "')" + cast);
   }



   /*******************************************************************************
    ** A JSON document is bound as text and explicitly cast to JSONB.
    *******************************************************************************/
   @Override
   public String jsonObjectPlaceholder()
   {
      return ("?::jsonb");
   }



   /*******************************************************************************
    ** Merge at the row lock; updates of independent keys cannot overwrite each other.
    *******************************************************************************/
   @Override
   public String jsonMergeAssignment(String column)
   {
      return (column + " = (COALESCE(" + column + ",'{}'::jsonb) || ?::jsonb) - ?::text[]");
   }



   /*******************************************************************************
    ** Equality and IN use GIN-compatible containment, one document per alternative.
    *******************************************************************************/
   @Override
   public String jsonPathPredicate(String column, QFieldMetaData field, QFilterCriteria criterion, List<Serializable> values)
   {
      if((criterion.getOperator() != QCriteriaOperator.EQUALS && criterion.getOperator() != QCriteriaOperator.IN)
         || values.isEmpty() || values.stream().anyMatch(java.util.Objects::isNull))
      {
         return (null);
      }
      JsonPathStorage storage = (JsonPathStorage) field.getBackendStorage();
      List<String> clauses = new ArrayList<>();
      for(int i = 0; i < values.size(); i++)
      {
         values.set(i, JsonUtils.toJson(Map.of(storage.getKey(), JsonStorageValues.jsonValue(field, values.get(i)))));
         clauses.add(column + " @> ?::jsonb");
      }
      return (String.join(" OR ", clauses));
   }



   /*******************************************************************************
    ** PostgreSQL implements every aggregate extension.
    *******************************************************************************/
   @Override
   public Set<AggregateCapability> getSupportedAggregateCapabilities(AggregateInput input)
   {
      return (EnumSet.allOf(AggregateCapability.class));
   }



   /*******************************************************************************
    ** Block sampling belongs to the main relation, after its alias and before joins.
    *******************************************************************************/
   @Override
   public String getSampledTableExpression(String table, String alias, BigDecimal percent, List<Serializable> params)
   {
      if(percent == null)
      {
         return (table + " AS " + alias);
      }
      params.add(percent);
      return (table + " AS " + alias + " TABLESAMPLE SYSTEM (?)");
   }



   /*******************************************************************************
    ** PostgreSQL date_trunc and width_bucket with an explicitly closed final numeric bin.
    *******************************************************************************/
   @Override
   public String getGroupByBucketExpression(GroupBy group, String column, QFieldType sourceType, RDBMSBackendMetaData backend, List<Serializable> params)
   {
      if(group.getBucket() instanceof NumericBucket numeric)
      {
         params.addAll(List.of(numeric.getMax(), numeric.getCount(), numeric.getMin(), numeric.getMax(), numeric.getCount()));
         return ("CASE WHEN " + column + " = ? THEN ? ELSE width_bucket(" + column + ", ?, ?, ?) END");
      }
      if(sourceType == QFieldType.DATE)
      {
         column = "CAST(" + column + " AS TIMESTAMP)";
      }
      DateBucketUnit unit = ((DateBucket) group.getBucket()).getUnit();
      String zone = getBucketTimeZone(ValueUtils.getSessionOrInstanceZoneId());
      // PostgreSQL AT TIME ZONE accepts an interval with the ISO offset sign.
      // Text offsets (including UTC-05:00) use the opposite POSIX sign.
      // See https://www.postgresql.org/docs/current/functions-datetime.html#FUNCTIONS-DATETIME-ZONECONVERT
      String zoneParameter = ValueUtils.getSessionOrInstanceZoneId().normalized() instanceof ZoneOffset ? "CAST(? AS INTERVAL)" : "?";
      if(sourceType == QFieldType.DATE_TIME)
      {
         ///////////////////////////////////////////////////////////////////////////////
         // QQQ stores DATE_TIME as a zoneless TIMESTAMP holding UTC (it binds Instants //
         // as UTC LocalDateTimes), so: UTC wall time -> absolute -> session wall time  //
         ///////////////////////////////////////////////////////////////////////////////
         params.add(zone);
         column = "((" + column + " AT TIME ZONE 'UTC') AT TIME ZONE " + zoneParameter + ")";
      }
      if(unit == DateBucketUnit.DAY_OF_WEEK || unit == DateBucketUnit.HOUR_OF_DAY)
      {
         return ("EXTRACT(" + (unit == DateBucketUnit.DAY_OF_WEEK ? "ISODOW" : "HOUR") + " FROM " + column + ")");
      }
      String expression = "date_trunc('" + unit.name().toLowerCase(Locale.ROOT) + "', " + column + ")";
      if(unit == DateBucketUnit.HOUR)
      {
         //////////////////////////////////////////////////////////////////////////////
         // back to an absolute time, then to a zoneless UTC wall time: QQQ reads    //
         // DATE_TIME results as UTC (the convention it binds Instants with), so the //
         // value must not depend on the connection's TimeZone setting.             //
         //////////////////////////////////////////////////////////////////////////////
         params.add(zone);
         return ("((" + expression + " AT TIME ZONE " + zoneParameter + ") AT TIME ZONE 'UTC')");
      }
      return ("CAST(" + expression + " AS DATE)");
   }
}
