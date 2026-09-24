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
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.ActionHelper;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
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

}
