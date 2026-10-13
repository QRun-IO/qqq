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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistencyGuarantee;
import com.kingsrook.qqq.backend.core.model.actions.consistentread.ConsistentReadInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateCapability;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucketUnit;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.NumericBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Note that much of this class came from the old (old) QueryManager class.
 *******************************************************************************/
public class BaseRDBMSActionStrategy implements RDBMSActionStrategyInterface
{
   private static final QLogger LOG = QLogger.getLogger(BaseRDBMSActionStrategy.class);

   private static final int MILLIS_PER_SECOND = 1000;

   public static final int DEFAULT_PAGE_SIZE = 2000;
   public static       int PAGE_SIZE         = DEFAULT_PAGE_SIZE;

   private       boolean              collectStatistics = false;
   private final Map<String, Integer> statistics        = Collections.synchronizedMap(new HashMap<>());

   public static final String STAT_QUERIES_RAN = "queriesRan";
   public static final String STAT_BATCHES_RAN = "batchesRan";



   /***************************************************************************
    *
    ***************************************************************************/
   public Integer appendCriterionToWhereClause(QFilterCriteria criterion, StringBuilder clause, String column, List<Serializable> values, QFieldMetaData field)
   {
      clause.append(column);

      /////////////////////////////////////////////////////////////////////////////////////////////
      // for some operators, we append the column name a second time, e.g., to do an 'OR' of two //
      // conditions (NOT_EQUALS_OR_IS_NULL becomes col !=? or col IS NULL).                      //
      // It's important in these cases to have any bind-params after the duplicated column name  //
      // - for scenarios where the column name has been replaced by a function, and the function //
      // has bind-params of its own (so they can be bound all before the 'value' param(s).       //
      // So we actually make NOT_EQUALS_OR_IS_NULL become col IS NULL or col != ?                //
      /////////////////////////////////////////////////////////////////////////////////////////////

      switch(criterion.getOperator())
      {
         case EQUALS ->
         {
            clause.append(" = ?");
            return (1);
         }
         case NOT_EQUALS ->
         {
            clause.append(" != ?");
            return (1);
         }
         case NOT_EQUALS_OR_IS_NULL ->
         {
            clause.append(" IS NULL OR ").append(column).append(" != ? ");
            return (1);
         }
         case IN ->
         {
            if(values.isEmpty())
            {
               ///////////////////////////////////////////////////////
               // if there are no values, then we want a false here //
               ///////////////////////////////////////////////////////
               clause.delete(0, clause.length());
               clause.append(" 0 = 1 ");
               return (0);
            }
            else
            {
               clause.append(" IN (").append(values.stream().map(x -> "?").collect(Collectors.joining(","))).append(")");
               return (values.size());
            }
         }
         case IS_NULL_OR_IN ->
         {
            clause.append(" IS NULL ");

            if(!values.isEmpty())
            {
               clause.append(" OR ").append(column).append(" IN (").append(values.stream().map(x -> "?").collect(Collectors.joining(","))).append(")");
               return (values.size());
            }
            else
            {
               return (0);
            }
         }
         case NOT_IN ->
         {
            if(values.isEmpty())
            {
               //////////////////////////////////////////////////////
               // if there are no values, then we want a true here //
               //////////////////////////////////////////////////////
               clause.delete(0, clause.length());
               clause.append(" 1 = 1 ");
               return (0);
            }
            else
            {
               clause.append(" NOT IN (").append(values.stream().map(x -> "?").collect(Collectors.joining(","))).append(")");
               return (values.size());
            }
         }
         case LIKE ->
         {
            clause.append(" LIKE ?");
            return (1);
         }
         case NOT_LIKE ->
         {
            clause.append(" NOT LIKE ?");
            return (1);
         }
         case STARTS_WITH ->
         {
            clause.append(" LIKE ?");
            ActionHelper.editFirstValue(values, (s -> s + "%"));
            return (1);
         }
         case ENDS_WITH ->
         {
            clause.append(" LIKE ?");
            ActionHelper.editFirstValue(values, (s -> "%" + s));
            return (1);
         }
         case CONTAINS ->
         {
            clause.append(" LIKE ?");
            ActionHelper.editFirstValue(values, (s -> "%" + s + "%"));
            return (1);
         }
         case NOT_STARTS_WITH ->
         {
            clause.append(" NOT LIKE ?");
            ActionHelper.editFirstValue(values, (s -> s + "%"));
            return (1);
         }
         case NOT_ENDS_WITH ->
         {
            clause.append(" NOT LIKE ?");
            ActionHelper.editFirstValue(values, (s -> "%" + s));
            return (1);
         }
         case NOT_CONTAINS ->
         {
            clause.append(" NOT LIKE ?");
            ActionHelper.editFirstValue(values, (s -> "%" + s + "%"));
            return (1);
         }
         case LESS_THAN ->
         {
            clause.append(" < ?");
            return (1);
         }
         case LESS_THAN_OR_EQUALS ->
         {
            clause.append(" <= ?");
            return (1);
         }
         case GREATER_THAN ->
         {
            clause.append(" > ?");
            return (1);
         }
         case GREATER_THAN_OR_EQUALS ->
         {
            clause.append(" >= ?");
            return (1);
         }
         case IS_BLANK ->
         {
            clause.append(" IS NULL");
            if(field.getType().isStringLike())
            {
               clause.append(" OR ").append(column).append(" = ''");
            }
            return (0);
         }
         case IS_NOT_BLANK ->
         {
            clause.append(" IS NOT NULL");
            if(field.getType().isStringLike())
            {
               clause.append(" AND ").append(column).append(" != ''");
            }
            return (0);
         }
         case BETWEEN ->
         {
            clause.append(" BETWEEN ? AND ?");
            return (2);
         }
         case NOT_BETWEEN ->
         {
            clause.append(" NOT BETWEEN ? AND ?");
            return (2);
         }
         case TRUE ->
         {
            clause.delete(0, clause.length());
            clause.append(" 1 = 1 ");
            return (0);
         }
         case FALSE ->
         {
            clause.delete(0, clause.length());
            clause.append(" 0 = 1 ");
            return (0);
         }
         default -> throw new IllegalStateException("Unexpected operator: " + criterion.getOperator());
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Serializable getFieldValueFromResultSet(QFieldType type, ResultSet resultSet, int i) throws SQLException
   {
      return switch(type)
      {
         case STRING, TEXT, HTML, PASSWORD -> (QueryManager.getString(resultSet, i));
         case INTEGER -> (QueryManager.getInteger(resultSet, i));
         case LONG -> (QueryManager.getLong(resultSet, i));
         case DECIMAL -> (QueryManager.getBigDecimal(resultSet, i));
         case DATE -> (resultSet.getObject(i, LocalDate.class));
         case TIME -> (QueryManager.getLocalTime(resultSet, i));
         case DATE_TIME -> (QueryManager.getInstant(resultSet, i));
         case BOOLEAN -> (QueryManager.getBoolean(resultSet, i));
         case BLOB -> (QueryManager.getByteArray(resultSet, i));
         default -> throw new IllegalStateException("Unexpected field type: " + type);
      };
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public PreparedStatement executeUpdate(Connection connection, String sql, List<Object> params) throws SQLException
   {
      try(PreparedStatement statement = prepareStatementAndBindParams(connection, sql, new List[] { params }))
      {
         incrementStatistic(STAT_QUERIES_RAN);
         statement.executeUpdate();
         return (statement);
      }
      catch(SQLException e)
      {
         LOG.warn("SQLException", e, logPair("sql", sql));
         throw (e);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void executeBatchUpdate(Connection connection, String updateSQL, List<List<Serializable>> values) throws SQLException
   {
      for(List<List<Serializable>> page : CollectionUtils.getPages(values, PAGE_SIZE))
      {
         PreparedStatement updatePS = connection.prepareStatement(updateSQL);
         for(List<Serializable> row : page)
         {
            Object[] params = new Object[row.size()];
            for(int i = 0; i < row.size(); i++)
            {
               params[i] = row.get(i);
            }

            bindParams(params, updatePS);
            updatePS.addBatch();
         }
         incrementStatistic(STAT_BATCHES_RAN);
         updatePS.executeBatch();
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public List<Serializable> executeInsertForGeneratedIds(Connection connection, String sql, List<Object> params, QFieldMetaData primaryKeyField) throws SQLException
   {
      try(PreparedStatement statement = connection.prepareStatement(sql, new String[] { getColumnName(primaryKeyField) }))
      {
         bindParams(params.toArray(), statement);
         incrementStatistic(STAT_QUERIES_RAN);
         statement.executeUpdate();

         ResultSet          generatedKeys = statement.getGeneratedKeys();
         List<Serializable> rs            = new ArrayList<>();
         while(generatedKeys.next())
         {
            rs.add(getFieldValueFromResultSet(primaryKeyField.getType(), generatedKeys, 1));
         }
         return (rs);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public Integer executeUpdateForRowCount(Connection connection, String sql, Object... params) throws SQLException
   {
      try(PreparedStatement statement = prepareStatementAndBindParams(connection, sql, params))
      {
         incrementStatistic(STAT_QUERIES_RAN);
         int rowCount = statement.executeUpdate();
         return (rowCount);
      }
      catch(SQLException e)
      {
         LOG.warn("SQLException", e, logPair("sql", sql));
         throw (e);
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void executeStatement(PreparedStatement statement, CharSequence sql, ResultSetProcessor processor, Object... params) throws SQLException, QException
   {
      ResultSet resultSet = null;

      try
      {
         bindParams(params, statement);
         incrementStatistic(STAT_QUERIES_RAN);
         statement.execute();
         resultSet = statement.getResultSet();

         if(processor != null)
         {
            processor.processResultSet(resultSet);
         }
      }
      catch(SQLException e)
      {
         if(sql != null)
         {
            LOG.warn("SQLException", e, logPair("sql", sql));
         }
         throw (e);
      }
      finally
      {
         if(resultSet != null)
         {
            resultSet.close();
         }
      }
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public Integer getPageSize(AbstractActionInput actionInput)
   {
      return PAGE_SIZE;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected PreparedStatement prepareStatementAndBindParams(Connection connection, String sql, Object[] params) throws SQLException
   {
      PreparedStatement statement = connection.prepareStatement(sql);
      bindParams(params, statement);
      return statement;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void bindParams(Object[] params, PreparedStatement statement) throws SQLException
   {
      int paramIndex = 0;
      if(params != null)
      {
         for(Object param : params)
         {
            int paramsBound = bindParamObject(statement, (paramIndex + 1), param);
            paramIndex += paramsBound;
         }
      }
   }



   /*******************************************************************************
    * index is 1-based!!
    *******************************************************************************/
   protected int bindParamObject(PreparedStatement statement, int index, Object value) throws SQLException
   {
      if(value instanceof Integer i)
      {
         bindParam(statement, index, i);
         return (1);
      }
      else if(value instanceof Short s)
      {
         bindParam(statement, index, s.intValue());
         return (1);
      }
      else if(value instanceof Long l)
      {
         bindParam(statement, index, l);
         return (1);
      }
      else if(value instanceof Double d)
      {
         bindParam(statement, index, d);
         return (1);
      }
      else if(value instanceof String s)
      {
         bindParam(statement, index, s);
         return (1);
      }
      else if(value instanceof Boolean b)
      {
         bindParam(statement, index, b);
         return (1);
      }
      else if(value instanceof Timestamp ts)
      {
         bindParam(statement, index, ts);
         return (1);
      }
      else if(value instanceof Date)
      {
         bindParam(statement, index, (Date) value);
         return (1);
      }
      else if(value instanceof Calendar c)
      {
         bindParam(statement, index, c);
         return (1);
      }
      else if(value instanceof BigDecimal bd)
      {
         bindParam(statement, index, bd);
         return (1);
      }
      else if(value == null)
      {
         statement.setNull(index, Types.CHAR);
         return (1);
      }
      else if(value instanceof Collection<?> c)
      {
         int paramsBound = 0;
         for(Object o : c)
         {
            paramsBound += bindParamObject(statement, (index + paramsBound), o);
         }
         return (paramsBound);
      }
      else if(value instanceof byte[] ba)
      {
         statement.setBytes(index, ba);
         return (1);
      }
      else if(value instanceof Instant i)
      {
         ///////////////////////////////////////////////////////////////////////
         // QQQ stores zone-less SQL timestamps in UTC. Some JDBC drivers bind //
         // Instant using the host timezone, so provide the UTC wall clock.    //
         ///////////////////////////////////////////////////////////////////////
         statement.setObject(index, LocalDateTime.ofInstant(i, ZoneOffset.UTC));
         return (1);
      }
      else if(value instanceof LocalDate ld)
      {
         statement.setObject(index, ld);
         return (1);
      }
      else if(value instanceof LocalTime lt)
      {
         statement.setObject(index, lt);
         return (1);
      }
      else if(value instanceof OffsetDateTime odt)
      {
         long      epochMillis = odt.toEpochSecond() * MILLIS_PER_SECOND;
         Timestamp timestamp   = new Timestamp(epochMillis);
         statement.setTimestamp(index, timestamp);
         return (1);
      }
      else if(value instanceof LocalDateTime ldt)
      {
         ZoneOffset offset      = OffsetDateTime.now().getOffset();
         long       epochMillis = ldt.toEpochSecond(offset) * MILLIS_PER_SECOND;
         Timestamp  timestamp   = new Timestamp(epochMillis);
         statement.setTimestamp(index, timestamp);
         return (1);
      }
      else if(value instanceof PossibleValueEnum<?> pve)
      {
         return (bindParamObject(statement, index, pve.getPossibleValueId()));
      }
      else
      {
         throw (new SQLException("Unexpected value type [" + value.getClass().getSimpleName() + "] in bindParamObject."));
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   protected void bindParam(PreparedStatement statement, int index, Integer value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.INTEGER);
      }
      else
      {
         statement.setInt(index, value);
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   protected void bindParam(PreparedStatement statement, int index, Long value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.INTEGER);
      }
      else
      {
         statement.setLong(index, value);
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   protected void bindParam(PreparedStatement statement, int index, Double value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.DOUBLE);
      }
      else
      {
         statement.setDouble(index, value);
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   protected void bindParam(PreparedStatement statement, int index, String value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.CHAR);
      }
      else
      {
         statement.setString(index, value);
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   protected void bindParam(PreparedStatement statement, int index, Boolean value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.BOOLEAN);
      }
      else
      {
         statement.setBoolean(index, value);
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   protected void bindParam(PreparedStatement statement, int index, Date value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.DATE);
      }
      else
      {
         statement.setDate(index, new Date(value.getTime()));
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   protected void bindParam(PreparedStatement statement, int index, Timestamp value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.TIMESTAMP);
      }
      else
      {
         statement.setTimestamp(index, value);
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   protected void bindParam(PreparedStatement statement, int index, Calendar value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.DATE);
      }
      else
      {
         statement.setTimestamp(index, new Timestamp(value.getTimeInMillis()));
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   protected void bindParam(PreparedStatement statement, int index, LocalDate value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.DATE);
      }
      else
      {
         LocalDateTime localDateTime = value.atTime(0, 0);
         Timestamp     timestamp     = new Timestamp(localDateTime.atZone(ZoneId.systemDefault()).toEpochSecond() * MILLIS_PER_SECOND); // TimeStamp expects millis, not seconds, after epoch
         statement.setTimestamp(index, timestamp);
      }
   }



   /*******************************************************************************
    *
    *******************************************************************************/
   protected void bindParam(PreparedStatement statement, int index, LocalDateTime value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.TIMESTAMP);
      }
      else
      {
         Timestamp timestamp = new Timestamp(value.atZone(ZoneId.systemDefault()).toEpochSecond() * MILLIS_PER_SECOND); // TimeStamp expects millis, not seconds, after epoch
         statement.setTimestamp(index, timestamp);
      }
   }



   /*******************************************************************************
    **
    **
    *******************************************************************************/
   protected void bindParam(PreparedStatement statement, int index, BigDecimal value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.DECIMAL);
      }
      else
      {
         statement.setBigDecimal(index, value);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected void bindParam(PreparedStatement statement, int index, byte[] value) throws SQLException
   {
      if(value == null)
      {
         statement.setNull(index, Types.ARRAY);
      }
      else
      {
         statement.setBytes(index, value);
      }
   }



   /*******************************************************************************
    **
    **
    *******************************************************************************/
   protected void bindParamNull(PreparedStatement statement, int index) throws SQLException
   {
      statement.setNull(index, Types.NULL);
   }



   /*******************************************************************************
    ** Increment a statistic
    **
    *******************************************************************************/
   protected void incrementStatistic(String statName)
   {
      if(collectStatistics)
      {
         statistics.putIfAbsent(statName, 0);
         statistics.put(statName, statistics.get(statName) + 1);
      }
   }



   /*******************************************************************************
    ** Setter for collectStatistics
    **
    *******************************************************************************/
   public void setCollectStatistics(boolean collectStatistics)
   {
      this.collectStatistics = collectStatistics;
   }



   /*******************************************************************************
    ** clear the map of statistics
    **
    *******************************************************************************/
   public void resetStatistics()
   {
      statistics.clear();
   }



   /*******************************************************************************
    ** Getter for statistics
    **
    *******************************************************************************/
   public Map<String, Integer> getStatistics()
   {
      return statistics;
   }



   /*******************************************************************************
    ** Setter for pageSize
    **
    *******************************************************************************/
   public void setPageSize(int pageSize)
   {
      BaseRDBMSActionStrategy.PAGE_SIZE = pageSize;
   }

   /*******************************************************************************
    ** Get the column name to use for a field in the RDBMS, from the fieldMetaData.
    **
    ** That is, field.backendName if set -- else, field.name
    *******************************************************************************/
   protected String getColumnName(QFieldMetaData field)
   {
      if(field.getBackendName() != null)
      {
         return (field.getBackendName());
      }
      return (field.getName());
   }


   /*******************************************************************************
    ** MySQL/MariaDB (InnoDB): START TRANSACTION WITH CONSISTENT SNAPSHOT (READ
    ** ONLY).  Tables on other engines (e.g., MyISAM) aren't covered by the
    ** snapshot, and are reported as BEST_EFFORT.  H2: REPEATABLE READ (MVCC), pinned by a first read.
    ** Other databases: as the interface default.
    *******************************************************************************/
   @Override
   public ConsistentReadStart beginConsistentRead(Connection connection, ConsistentReadInput input) throws SQLException
   {
      String product = connection.getMetaData().getDatabaseProductName();
      String vendor  = product == null ? "" : product.toLowerCase();

      if(vendor.contains("mysql") || vendor.contains("mariadb"))
      {
         if(input.getImportSnapshotId().isPresent())
         {
            throw (new SQLException("Importing a snapshot is not supported by " + product));
         }

         try(Statement statement = connection.createStatement())
         {
            statement.execute("SET TRANSACTION ISOLATION LEVEL REPEATABLE READ");
            statement.execute("START TRANSACTION WITH CONSISTENT SNAPSHOT" + (input.getReadOnly() ? ", READ ONLY" : ""));
         }

         ConsistencyGuarantee guarantee = ConsistencyGuarantee.ONE_INSTANT;
         try(Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE' AND engine <> 'InnoDB'"))
         {
            if(resultSet.next() && resultSet.getInt(1) > 0)
            {
               guarantee = ConsistencyGuarantee.BEST_EFFORT;
            }
         }

         return (new ConsistentReadStart(Optional.empty(), guarantee));
      }

      if(vendor.startsWith("h2"))
      {
         if(input.getImportSnapshotId().isPresent())
         {
            throw (new SQLException("Importing a snapshot is not supported by " + product));
         }

         connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);

         /////////////////////////////////////////////////////////////////////////////
         // h2 takes a table's snapshot at the transaction's first read of that     //
         // table - so read from each table now, rather than at the caller's first  //
         // read (which may be later, after other writers have committed).  (h2 is  //
         // mostly for tests - so a read per table at open time is fine.)           //
         /////////////////////////////////////////////////////////////////////////////
         List<String> tableNames = new ArrayList<>();
         try(ResultSet tables = connection.getMetaData().getTables(null, connection.getSchema(), null, new String[] { "BASE TABLE", "TABLE" }))
         {
            while(tables.next())
            {
               tableNames.add(tables.getString("TABLE_NAME"));
            }
         }

         String quote = getIdentifierQuoteString();
         for(String tableName : tableNames)
         {
            try(Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT 1 FROM " + quote + tableName + quote + " LIMIT 1"))
            {
               resultSet.next();
            }
         }

         return (new ConsistentReadStart(Optional.empty(), ConsistencyGuarantee.ONE_INSTANT));
      }

      return (RDBMSActionStrategyInterface.super.beginConsistentRead(connection, input));
   }




   /*******************************************************************************
    ** H2 supports ordered-set aggregates; MySQL supports the remaining extensions.
    *******************************************************************************/
   @Override
   public Set<AggregateCapability> getSupportedAggregateCapabilities(AggregateInput input)
   {
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) input.getBackend();
      if(!isH2(backend) && !RDBMSBackendMetaData.VENDOR_MYSQL.equals(backend.getVendor()) && !RDBMSBackendMetaData.VENDOR_AURORA_MYSQL.equals(backend.getVendor()))
      {
         return (Set.of());
      }
      Set<AggregateCapability> capabilities = EnumSet.allOf(AggregateCapability.class);
      if(!isH2(backend))
      {
         capabilities.remove(AggregateCapability.PERCENTILE);
         capabilities.remove(AggregateCapability.CORRELATION);
      }
      return (capabilities);
   }



   /*******************************************************************************
    ** Use explicit backend metadata to identify H2.
    *******************************************************************************/
   protected boolean isH2(RDBMSBackendMetaData backend)
   {
      return (RDBMSBackendMetaData.VENDOR_H2.equals(backend.getVendor()));
   }



   /*******************************************************************************
    ** ANSI aggregate functions used by H2 and MySQL where supported.
    *******************************************************************************/
   @Override
   public String getAggregateExpression(AggregateOperator operator, String column, String otherColumn, Aggregate aggregate, RDBMSBackendMetaData backend, List<Serializable> params)
   {
      // H2 CHAR_LENGTH counts UTF-16 units. Its Unicode-aware regex counts code
      // points, matching CHAR_LENGTH on PostgreSQL/MySQL and the memory evaluator.
      String length = isH2(backend) ? "LENGTH(REGEXP_REPLACE(" + column + ", '(?s).', 'x'))" : "CHAR_LENGTH(" + column + ")";
      return (switch(operator)
      {
         case STDDEV_SAMP, STDDEV_POP, VAR_SAMP, VAR_POP -> operator.name() + "(" + column + ")";
         case PERCENTILE_CONT, PERCENTILE_DISC ->
         {
            params.add(aggregate.getPercentile());
            yield operator.name() + "(?) WITHIN GROUP (ORDER BY " + column + ")";
         }
         case CORR -> "CORR(" + otherColumn + ", " + column + ")";
         case COUNT_NULL -> "(COUNT(*) - COUNT(" + column + "))";
         case COUNT_BLANK -> "COALESCE(SUM(CASE WHEN " + column + " IS NULL OR TRIM(" + column + ") = '' THEN 1 ELSE 0 END), 0)";
         case MIN_LENGTH -> "MIN(" + length + ")";
         case MAX_LENGTH -> "MAX(" + length + ")";
         case AVG_LENGTH -> "AVG(" + length + ")";
         default -> operator.getSqlPrefix() + column + ")";
      });
   }



   /*******************************************************************************
    ** Sample the main relation before joining. RAND is supported by H2 and MySQL.
    *******************************************************************************/
   @Override
   public String getSampledTableExpression(String table, String alias, BigDecimal percent, List<Serializable> params)
   {
      if(percent == null)
      {
         return (table + " AS " + alias);
      }
      params.add(percent.movePointLeft(2));
      return ("(SELECT * FROM " + table + " WHERE RAND() < ?) AS " + alias);
   }



   /*******************************************************************************
    ** Equal-width bins with max included in the last bin, and session-local date buckets.
    *******************************************************************************/
   @Override
   public String getGroupByBucketExpression(GroupBy group, String column, QFieldType sourceType, RDBMSBackendMetaData backend, List<Serializable> params)
   {
      if(group.getBucket() instanceof NumericBucket numeric)
      {
         params.addAll(List.of(numeric.getMin(), numeric.getMax(), numeric.getCount() + 1, numeric.getMax(), numeric.getCount(),
            numeric.getMin(), numeric.getMax(), numeric.getMin(), numeric.getCount()));
         return ("CASE WHEN " + column + " < ? THEN 0 WHEN " + column + " > ? THEN ? WHEN " + column
            + " = ? THEN ? ELSE FLOOR((" + column + " - ?) / ((? - ?) / ?)) + 1 END");
      }
      DateBucketUnit unit = ((DateBucket) group.getBucket()).getUnit();
      String zone = ValueUtils.getSessionOrInstanceZoneId().getId();
      if(isH2(backend))
      {
         if(sourceType == QFieldType.DATE_TIME)
         {
            params.add(zone);
            // H2 interprets timezone-free timestamps in the JVM zone on AT TIME ZONE.
            // Attach UTC explicitly, then retain the offset while bucketing.
            column = "(CAST(CASE WHEN " + column + " IS NULL THEN NULL ELSE CONCAT(" + column + ", ' UTC') END AS TIMESTAMP WITH TIME ZONE) AT TIME ZONE ?)";
         }
         else if(unit == DateBucketUnit.HOUR)
         {
            params.add(zone);
            column = "CAST(CASE WHEN " + column + " IS NULL THEN NULL ELSE CONCAT(" + column + ", ' 00:00:00 ', ?) END AS TIMESTAMP WITH TIME ZONE)";
         }
         else
         {
            column = "CAST(" + column + " AS TIMESTAMP)";
         }
         if(unit == DateBucketUnit.DAY_OF_WEEK || unit == DateBucketUnit.HOUR_OF_DAY)
         {
            return ("EXTRACT(" + (unit == DateBucketUnit.DAY_OF_WEEK ? "ISO_DAY_OF_WEEK" : "HOUR") + " FROM " + column + ")");
         }
         String expression = "DATE_TRUNC('" + (unit == DateBucketUnit.WEEK ? "ISO_WEEK" : unit.name()) + "', " + column + ")";
         if(unit == DateBucketUnit.HOUR)
         {
            return (expression);
         }
         return ("CAST(FORMATDATETIME(" + expression + ", 'yyyy-MM-dd') AS DATE)");
      }
      if(sourceType == QFieldType.DATE_TIME)
      {
         params.add(zone);
         column = "CONVERT_TZ(" + column + ", '+00:00', ?)";
      }
      return (mysqlDateBucket(unit, column, zone, params));
   }



   /*******************************************************************************
    ** MySQL date expressions use named-zone tables for non-UTC session zones.
    *******************************************************************************/
   private String mysqlDateBucket(DateBucketUnit unit, String column, String zone, List<Serializable> params)
   {
      return (switch(unit)
      {
         case DAY_OF_WEEK -> "WEEKDAY(" + column + ") + 1";
         case HOUR_OF_DAY -> "HOUR(" + column + ")";
         case YEAR -> "CAST(DATE_FORMAT(" + column + ", '%Y-01-01') AS DATE)";
         case MONTH -> "CAST(DATE_FORMAT(" + column + ", '%Y-%m-01') AS DATE)";
         case DAY -> "DATE(" + column + ")";
         case WEEK -> "STR_TO_DATE(DATE_FORMAT(" + column + ", '%x%v Monday'), '%x%v %W')";
         case QUARTER -> "TIMESTAMPADD(QUARTER, TIMESTAMPDIFF(QUARTER, '1000-01-01', " + column + "), DATE('1000-01-01'))";
         case HOUR ->
         {
            params.add(zone);
            yield "CONVERT_TZ(CAST(DATE_FORMAT(" + column + ", '%Y-%m-%d %H:00:00') AS DATETIME), ?, '+00:00')";
         }
      });
   }
}
