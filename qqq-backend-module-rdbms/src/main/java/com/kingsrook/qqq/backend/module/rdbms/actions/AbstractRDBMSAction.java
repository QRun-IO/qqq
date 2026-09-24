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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.JoinsContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.expressions.AbstractFilterExpression;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunctionTypeRegistry;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLockFilters;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.ObjectUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.module.rdbms.fieldfunctions.RDBMSFieldFunctionAdapterInterface;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSFieldMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.strategy.RDBMSActionStrategyInterface;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Base class for all core actions in the RDBMS module.
 **
 ** Provides common functionality for query construction, SQL generation, result
 ** set processing, and connection management across all RDBMS backend operations.
 ** Handles database-agnostic SQL building with vendor-specific strategies,
 ** security lock application, join processing, and filter criteria translation.
 **
 ** Key responsibilities:
 ** - SQL clause generation (SELECT, FROM, WHERE, ORDER BY)
 ** - PreparedStatement parameter management
 ** - Join context resolution and FROM clause construction
 ** - Security criteria integration via RecordSecurityLocks
 ** - Result set field extraction with type conversion
 ** - Database connection acquisition from ConnectionManager
 ** - Query cancellation support for long-running operations
 **
 ** All concrete RDBMS action implementations (Query, Count, Insert, Update, Delete)
 ** extend this class to leverage shared SQL building and data access patterns.
 *******************************************************************************/
public abstract class AbstractRDBMSAction
{
   private static final QLogger LOG = QLogger.getLogger(AbstractRDBMSAction.class);
   protected QueryStat queryStat;
   protected volatile PreparedStatement statement;
   protected volatile boolean           isCancelled = false;

   protected RDBMSBackendMetaData         backendMetaData;
   protected RDBMSActionStrategyInterface actionStrategy;



   /*******************************************************************************
    ** Get the table name to use in the RDBMS from a QTableMetaData.
    **
    ** That is, table.backendDetails.tableName if set -- else, table.name
    *******************************************************************************/
   public static String getTableName(QTableMetaData table)
   {
      if(table.getBackendDetails() instanceof RDBMSTableBackendDetails details)
      {
         if(StringUtils.hasContent(details.getTableName()))
         {
            return (details.getTableName());
         }
      }
      return (table.getName());
   }



   /*******************************************************************************
    ** Get the column name to use for a field in the RDBMS, from the fieldMetaData.
    **
    ** That is, field.backendName if set -- else, field.name
    *******************************************************************************/
   public static String getColumnName(QFieldMetaData field)
   {
      if(field.getBackendName() != null)
      {
         return (field.getBackendName());
      }
      return (field.getName());
   }



   /*******************************************************************************
    ** Get a database connection, per the backend in the request.
    **
    ** Note that it may be a connection to a read-only backend, per query-hints,
    ** and backend settings.
    *******************************************************************************/
   public static Connection getConnection(AbstractTableActionInput tableActionInput) throws SQLException
   {
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) tableActionInput.getBackend();

      boolean useReadOnly = false;
      if(tableActionInput instanceof QueryInput queryInput)
      {
         useReadOnly = queryInput.hasQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);
      }
      else if(tableActionInput instanceof CountInput countInput)
      {
         useReadOnly = countInput.hasQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);
      }
      else if(tableActionInput instanceof GetInput getInput)
      {
         useReadOnly = getInput.hasQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);
      }
      else if(tableActionInput instanceof AggregateInput aggregateInput)
      {
         useReadOnly = aggregateInput.hasQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);
      }

      if(useReadOnly && backend.getReadOnlyBackendMetaData() != null)
      {
         return ConnectionManager.getConnection(backend.getReadOnlyBackendMetaData());
      }

      return ConnectionManager.getConnection(backend);
   }



   /*******************************************************************************
    ** Make it easy (e.g., for tests) to turn on logging of SQL
    *******************************************************************************/
   public static void setLogSQL(boolean on, boolean doReformat, String loggerOrSystemOut)
   {
      setLogSQL(on);
      setLogSQLOutput(loggerOrSystemOut);
      setLogSQLReformat(doReformat);
   }



   /*******************************************************************************
    ** Make it easy (e.g., for tests) to turn on logging of SQL
    *******************************************************************************/
   public static void setLogSQL(boolean on)
   {
      System.setProperty("qqq.rdbms.logSQL", String.valueOf(on));
   }



   /*******************************************************************************
    ** Make it easy (e.g., for tests) to turn on logging of SQL
    *******************************************************************************/
   public static void setLogSQLOutput(String loggerOrSystemOut)
   {
      System.setProperty("qqq.rdbms.logSQL.output", loggerOrSystemOut);
   }



   /*******************************************************************************
    ** Make it easy (e.g., for tests) to turn on poor-man's formatting of SQL
    *******************************************************************************/
   public static void setLogSQLReformat(boolean doReformat)
   {
      System.setProperty("qqq.rdbms.logSQL.reformat", String.valueOf(doReformat));
   }



   /*******************************************************************************
    ** Handle obvious problems with values - like empty string for integer should be null,
    ** and type conversions that we can do "better" than jdbc.
    **
    ** Performs type coercion and normalization of values before they are set in
    ** PreparedStatements. Converts empty strings to null for typed fields, and
    ** uses ValueUtils for string-to-type conversions that are more forgiving than
    ** JDBC's default behavior.
    **
    ** @param field the field metadata defining expected type
    ** @param value the raw value to scrub/convert
    ** @return the scrubbed value, properly typed or null
    *******************************************************************************/
   protected Serializable scrubValue(QFieldMetaData field, Serializable value)
   {
      return scrubValue(field.getType(), value);
   }



   /*******************************************************************************
    ** Handle obvious problems with values - like empty string for integer should be null,
    ** and type conversions that we can do "better" than jdbc.
    **
    ** Performs type coercion and normalization of values before they are set in
    ** PreparedStatements. Converts empty strings to null for typed fields, and
    ** uses ValueUtils for string-to-type conversions that are more forgiving than
    ** JDBC's default behavior.
    **
    ** @param type the expected type of the value
    ** @param value the raw value to scrub/convert
    ** @return the scrubbed value, properly typed or null
    *******************************************************************************/
   protected Serializable scrubValue(QFieldType type, Serializable value)
   {
      if("".equals(value))
      {
         if(type.equals(QFieldType.INTEGER) || type.equals(QFieldType.LONG) || type.equals(QFieldType.DECIMAL) || type.equals(QFieldType.DATE) || type.equals(QFieldType.DATE_TIME) || type.equals(QFieldType.BOOLEAN))
         {
            value = null;
         }
      }

      //////////////////////////////////////////////////////////////////////////////
      // value utils is good at making values from strings - jdbc, not as much... //
      //////////////////////////////////////////////////////////////////////////////
      if(type.equals(QFieldType.INTEGER) && value instanceof String)
      {
         value = ValueUtils.getValueAsInteger(value);
      }
      else if(type.equals(QFieldType.LONG) && value instanceof String)
      {
         value = ValueUtils.getValueAsLong(value);
      }
      else if(type.equals(QFieldType.DATE) && value instanceof String)
      {
         value = ValueUtils.getValueAsLocalDate(value);
      }
      else if(type.equals(QFieldType.DATE_TIME) && value instanceof String)
      {
         value = ValueUtils.getValueAsInstant(value);
      }
      else if(type.equals(QFieldType.DECIMAL) && value instanceof String)
      {
         value = ValueUtils.getValueAsBigDecimal(value);
      }
      else if(type.equals(QFieldType.BOOLEAN) && value instanceof String)
      {
         value = ValueUtils.getValueAsBoolean(value);
      }

      return (value);
   }



   /*******************************************************************************
    ** If the table has a field with the given name, then set the given value in the
    ** given record.
    **
    ** Safely attempts to set a field value, gracefully handling the case where the
    ** field does not exist in the table metadata. Used when populating records from
    ** result sets where columns may not map to all defined fields.
    **
    ** @param record the record to populate
    ** @param table the table metadata to check for field existence
    ** @param fieldName the name of the field to set
    ** @param value the value to set in the field
    *******************************************************************************/
   protected void setValueIfTableHasField(QRecord record, QTableMetaData table, String fieldName, Serializable value)
   {
      try
      {
         if(table.getFields().containsKey(fieldName))
         {
            record.setValue(fieldName, value);
         }
      }
      catch(Exception e)
      {
         /////////////////////////////////////////////////
         // this means field doesn't exist, so, ignore. //
         /////////////////////////////////////////////////
      }
   }



   /*******************************************************************************
    ** Build the FROM clause for a SQL query, including all JOIN clauses.
    **
    ** Constructs the full FROM clause starting with the main table, then adds all
    ** joins from the JoinsContext in proper order. Handles table aliasing, join
    ** type specification (INNER, LEFT, etc.), and ON clause generation from join
    ** metadata. Also integrates security criteria into join ON clauses where
    ** applicable.
    **
    ** @param instance the QInstance containing table and join metadata
    ** @param tableName the name of the main table
    ** @param joinsContext context containing all joins to include in the query
    ** @param params list to populate with parameter values from security criteria
    ** @return the complete FROM clause including all joins
    *******************************************************************************/
   protected String makeFromClause(QInstance instance, String tableName, JoinsContext joinsContext, List<Serializable> params)
   {
      //////////////////////////////////////////////////////////////////////
      // start with the main table - un-aliased (well, aliased as itself) //
      //////////////////////////////////////////////////////////////////////
      StringBuilder rs = new StringBuilder(escapeIdentifier(getTableName(joinsContext.getTable(tableName))) + " AS " + escapeIdentifier(tableName));

      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      // sort the query joins from the main table "outward"...                                              //
      // this might not be perfect, e.g., for cases where what we actually might need is a tree of joins... //
      ////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<QueryJoin> queryJoins = sortQueryJoinsForFromClause(tableName, joinsContext.getQueryJoins());

      ////////////////////////////////////////////////////////
      // iterate over joins, adding to the from clause (rs) //
      ////////////////////////////////////////////////////////
      for(QueryJoin queryJoin : queryJoins)
      {
         QTableMetaData joinTable            = joinsContext.getTable(queryJoin.getJoinTable());
         String         joinTableNameOrAlias = queryJoin.getJoinTableOrItsAlias();

         ////////////////////////////////////////////////////////
         // add the `<type> JOIN table AS alias` bit to the rs //
         ////////////////////////////////////////////////////////
         rs.append(" ").append(queryJoin.getType()).append(" JOIN ")
            .append(escapeIdentifier(getTableName(joinTable)))
            .append(" AS ").append(escapeIdentifier(joinTableNameOrAlias));

         ////////////////////////////////////////////////////////////////////////////////
         // find the join in the instance, for building the ON clause                  //
         // append each sub-clause (condition) into a list, for later joining with AND //
         ////////////////////////////////////////////////////////////////////////////////
         List<String>  joinClauseList = new ArrayList<>();
         String        baseTableName  = Objects.requireNonNullElse(joinsContext.resolveTableNameOrAliasToTableName(queryJoin.getBaseTableOrAlias()), tableName);
         QJoinMetaData joinMetaData   = Objects.requireNonNull(queryJoin.getJoinMetaData(), () -> "Could not find a join between tables [" + baseTableName + "][" + queryJoin.getJoinTable() + "]");

         //////////////////////////////////////////////////
         // loop over join-ons (e.g., multi-column join) //
         //////////////////////////////////////////////////
         for(JoinOn joinOn : joinMetaData.getJoinOns())
         {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            // figure out if the join needs flipped.  We want its left table to equal the queryJoin's base table. //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////
            QTableMetaData leftTable  = joinsContext.getTable(joinMetaData.getLeftTable());
            QTableMetaData rightTable = joinsContext.getTable(joinMetaData.getRightTable());

            if(!joinMetaData.getLeftTable().equals(baseTableName))
            {
               joinOn = joinOn.flip();
               QTableMetaData tmpTable = leftTable;
               leftTable = rightTable;
               rightTable = tmpTable;
            }

            ////////////////////////////////////////////////////////////
            // get the table-names-or-aliases to use in the ON clause //
            ////////////////////////////////////////////////////////////
            String baseTableOrAlias = queryJoin.getBaseTableOrAlias();
            String joinTableOrAlias = queryJoin.getJoinTableOrItsAlias();
            if(baseTableOrAlias == null)
            {
               baseTableOrAlias = leftTable.getName();
               if(!joinsContext.hasAliasOrTable(baseTableOrAlias))
               {
                  throw (new RuntimeException("Could not find a table or alias [" + baseTableOrAlias + "] in query.  May need to be more specific setting up QueryJoins."));
               }
            }

            joinClauseList.add(escapeIdentifier(baseTableOrAlias)
               + "." + escapeIdentifier(getColumnName(leftTable.getField(joinOn.getLeftField())))
               + " = " + escapeIdentifier(joinTableOrAlias)
               + "." + escapeIdentifier(getColumnName((rightTable.getField(joinOn.getRightField())))));
         }

         if(CollectionUtils.nullSafeHasContents(queryJoin.getSecurityCriteria()))
         {
            Optional<String> securityOnClause = getSqlWhereStringAndPopulateParamsListFromNonNestedFilter(joinsContext, queryJoin.getSecurityCriteria(), QQueryFilter.BooleanOperator.AND, params);
            if(securityOnClause.isPresent())
            {
               LOG.debug("Wrote securityOnClause", logPair("clause", securityOnClause));
               joinClauseList.add(securityOnClause.get());
            }
         }

         rs.append(" ON ").append(StringUtils.join(" AND ", joinClauseList));
      }

      return (rs.toString());
   }



   /*******************************************************************************
    ** We've seen some SQL dialects (mysql, but not h2...) be unhappy if we have
    ** the from/join-ons out of order.  This method resorts the joins, to start with
    ** main table, then any tables attached to it, then fanning out from there.
    *******************************************************************************/
   private List<QueryJoin> sortQueryJoinsForFromClause(String mainTableName, List<QueryJoin> queryJoins)
   {
      List<QueryJoin> rs = new ArrayList<>();

      ////////////////////////////////////////////////////////////////////////////////
      // make a copy of the input list that we can feel safe removing elements from //
      ////////////////////////////////////////////////////////////////////////////////
      List<QueryJoin> inputListCopy = new ArrayList<>(queryJoins);

      ///////////////////////////////////////////////////////////////////////////////////////////////////
      // keep track of the tables (or aliases) that we've seen - that's what we'll "grow" outward from //
      ///////////////////////////////////////////////////////////////////////////////////////////////////
      Set<String> seenTablesOrAliases = new HashSet<>();
      seenTablesOrAliases.add(mainTableName);

      ////////////////////////////////////////////////////////////////////////////////////
      // loop as long as there are more tables in the inputList, and the keepGoing flag //
      // is set (e.g., indicating that we added something in the last iteration)        //
      ////////////////////////////////////////////////////////////////////////////////////
      boolean keepGoing = true;
      while(!inputListCopy.isEmpty() && keepGoing)
      {
         keepGoing = false;

         Iterator<QueryJoin> iterator = inputListCopy.iterator();
         while(iterator.hasNext())
         {
            QueryJoin nextQueryJoin = iterator.next();

            //////////////////////////////////////////////////////////////////////////
            // get the baseTableOrAlias from this join - and if it isn't set in the //
            // QueryJoin, then get it from the left-side of the join's metaData     //
            //////////////////////////////////////////////////////////////////////////
            String baseTableOrAlias = nextQueryJoin.getBaseTableOrAlias();
            if(baseTableOrAlias == null && nextQueryJoin.getJoinMetaData() != null)
            {
               baseTableOrAlias = nextQueryJoin.getJoinMetaData().getLeftTable();
            }

            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // if we have a baseTableOrAlias (would we ever not?), and we've seen it before - OR - we've seen this query join's joinTableOrAlias,   //
            // then we can add this pair of namesOrAliases to our seen-set, remove this queryJoin from the inputListCopy (iterator), and keep going //
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            if((StringUtils.hasContent(baseTableOrAlias) && seenTablesOrAliases.contains(baseTableOrAlias)) || seenTablesOrAliases.contains(nextQueryJoin.getJoinTableOrItsAlias()))
            {
               rs.add(nextQueryJoin);
               if(StringUtils.hasContent(baseTableOrAlias))
               {
                  seenTablesOrAliases.add(baseTableOrAlias);
               }

               seenTablesOrAliases.add(nextQueryJoin.getJoinTableOrItsAlias());
               iterator.remove();
               keepGoing = true;
            }
         }
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // in case any are left, add them all here - does this ever happen?                                          //
      // the only time a conditional breakpoint here fires in the RDBMS test suite, is in query designed to throw. //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
      rs.addAll(inputListCopy);

      return (rs);
   }



   /*******************************************************************************
    ** Method to make a full WHERE clause.
    **
    ** Note that criteria for security are assumed to have been added to the filter
    ** during the construction of the JoinsContext.
    *******************************************************************************/
   protected String makeWhereClause(JoinsContext joinsContext, QQueryFilter filter, List<Serializable> params) throws IllegalArgumentException
   {
      if(filter == null || !filter.hasAnyCriteria())
      {
         return ("1 = 1");
      }

      Optional<String> clause = getSqlWhereStringAndPopulateParamsListFromNonNestedFilter(joinsContext, filter.getCriteria(), filter.getBooleanOperator(), params);
      if(!CollectionUtils.nullSafeHasContents(filter.getSubFilters()))
      {
         ///////////////////////////////////////////////////////////////
         // if there are no sub-clauses, then just return this clause //
         // and if there's no clause, use the default 1 = 1           //
         ///////////////////////////////////////////////////////////////
         return (clause.orElse("1 = 1"));
      }

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // else, build a list of clauses - recursively expanding the sub-filters into clauses, then return them joined with our operator //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<String> clauses = new ArrayList<>();
      if(clause.isPresent() && StringUtils.hasContent(clause.get()))
      {
         clauses.add("(" + clause.get() + ")");
      }

      for(QQueryFilter subFilter : filter.getSubFilters())
      {
         String subClause = makeWhereClause(joinsContext, subFilter, params);
         if(StringUtils.hasContent(subClause))
         {
            clauses.add("(" + subClause + ")");
         }
      }

      return (String.join(" " + filter.getBooleanOperator().toString() + " ", clauses));
   }



   /*******************************************************************************
    ** Convert a non-nested list of filter criteria into a SQL WHERE sub-clause.
    **
    ** Iterates through criteria, converting each to SQL using the appropriate action
    ** strategy. Handles field-to-field comparisons, expression evaluation, value
    ** scrubbing/type conversion, and parameter list population. Joins all clauses
    ** with the specified boolean operator (AND/OR).
    **
    ** @param joinsContext context for resolving field names to table aliases
    ** @param criteria list of filter criteria to convert to SQL
    ** @param booleanOperator operator (AND/OR) to join the criteria clauses
    ** @param params list to populate with parameter values for PreparedStatement
    ** @return optional SQL where sub-clause, as in "x AND y", or empty if no valid criteria
    ** @throws IllegalArgumentException if criteria have incorrect number of values
    *******************************************************************************/
   private Optional<String> getSqlWhereStringAndPopulateParamsListFromNonNestedFilter(JoinsContext joinsContext, List<QFilterCriteria> criteria, QQueryFilter.BooleanOperator booleanOperator, List<Serializable> params) throws IllegalArgumentException
   {
      List<String> clauses = new ArrayList<>();
      for(QFilterCriteria criterion : criteria)
      {
         if(criterion.getFieldName() == null)
         {
            LOG.info("QFilter criteria is missing a fieldName - will not be included in query.");
            continue;
         }

         if(criterion.getOperator() == null)
         {
            LOG.info("QFilter criteria is missing a operator - will not be included in query.", logPair("fieldName", criterion.getFieldName()));
            continue;
         }

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // from the criterion's field name (e.g., lineItem.sku), figure out what field we're actually working with //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////
         JoinsContext.FieldAndTableNameOrAlias fieldAndTableNameOrAlias = joinsContext.getFieldAndTableNameOrAlias(criterion.getFieldName(), true);
         QFieldMetaData                        field                    = fieldAndTableNameOrAlias.field();
         QVirtualFieldMetaData                 virtualField             = null;

         ////////////////////////////////////////////////////////////////////////////////////////
         // if the field is virtual, then capture that virtualField in its own variable, and   //
         // replace the field variable with the real field that the virtual field is based on. //
         ////////////////////////////////////////////////////////////////////////////////////////
         if(field instanceof QVirtualFieldMetaData v && v.getFieldFunction() != null)
         {
            virtualField = v;
            String fieldTableName = joinsContext.resolveTableNameOrAliasToTableName(fieldAndTableNameOrAlias.tableNameOrAlias());
            String realFieldName  = virtualField.getFieldFunction().getFieldName();
            field = joinsContext.getTable(fieldTableName).getField(realFieldName);
         }

         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if the field specifies an action strategy, then use it (to overwrite the one that comes from the backend). //
         ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         RDBMSActionStrategyInterface actionStrategy = getActionStrategy();
         RDBMSFieldMetaData rdbmsFieldMetaData = RDBMSFieldMetaData.of(field);
         if(rdbmsFieldMetaData != null)
         {
            RDBMSActionStrategyInterface fieldActionStrategy = rdbmsFieldMetaData.getActionStrategy();
            if(fieldActionStrategy != null)
            {
               actionStrategy = fieldActionStrategy;
            }
         }

         /////////////////////////////////////////////////////////////////
         // figure out if a field function is being used.               //
         // if one is specified on the criterion, then use it.          //
         // else, if the field is virtual, then use ITS field function. //
         /////////////////////////////////////////////////////////////////
         FieldFunction fieldFunction = criterion.getFieldFunction();
         if(fieldFunction == null && virtualField != null)
         {
            fieldFunction = virtualField.getFieldFunction();
         }

         //////////////////////////////////////////////////////////////////////////////////////////////
         // if a field function is being used, then get the adapter for it from the action strategy. //
         // that's to help accommodate different DB vendors using different function names.          //
         //////////////////////////////////////////////////////////////////////////////////////////////
         RDBMSFieldFunctionAdapterInterface fieldFunctionAdapter = null;
         if(fieldFunction != null)
         {
            fieldFunctionAdapter = backendMetaData.getFieldFunctionAdapter(fieldFunction.getFunctionTypeIdentifier());
            requireFieldFunctionAdapterNotNull(fieldFunctionAdapter, fieldFunction);
         }

         //////////////////////////////////////////////////////////////////
         // figure out the column name for the table/alias + field name. //
         // wrap that string in a function if we have a function adapter //
         //////////////////////////////////////////////////////////////////
         String column = escapeIdentifier(fieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(getColumnName(field));
         if(fieldFunctionAdapter != null)
         {
            String         tableNameOrAlias = fieldAndTableNameOrAlias.tableNameOrAlias();
            String         fieldTableName   = joinsContext.resolveTableNameOrAliasToTableName(tableNameOrAlias);
            QTableMetaData fieldTable       = joinsContext.getTable(fieldTableName);
            column = fieldFunctionAdapter.wrapColumnName(column, fieldFunction, makeFieldNameToColumnReferenceFunction(tableNameOrAlias, fieldTable));
         }

         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // have the action strategy build the SQL string that goes in the where clause, and tell us out how many bind-params are needed. //
         // it takes the values list so that it can do things like:                                                                       //
         // - build an always-true or always-false clause for an IN with 0 parameters                                                     //
         // - edit the string for string contains, starts-with, etc, to work as SQL LIKE                                                  //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         StringBuilder      clause = new StringBuilder();
         List<Serializable> values = criterion.getValues() == null ? new ArrayList<>() : new ArrayList<>(criterion.getValues());
         Integer expectedNoOfParams = actionStrategy.appendCriterionToWhereClause(criterion, clause, column, values, field);

         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // if there's a field function adapter being used, see if we need to add any bind params as part of applying the function. //
         // for example, specifying a time zone, or a length to a substr function, etc.                                             //
         /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
         if(fieldFunctionAdapter != null)
         {
            List<Serializable> functionParams = fieldFunctionAdapter.getParams(fieldFunction);
            if(CollectionUtils.nullSafeHasContents(functionParams))
            {
               /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               // because some criteria duplicate the field name in the where clause, we need to add the params for each copy //
               // for examples: (x IS NULL or x IN ...) - or - (x IS NULL OR x = '')                                          //
               /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
               int copiesOfParamsNeeded = StringUtils.countSubstringInstances(clause.toString(), column);
               for(int i = 0; i < copiesOfParamsNeeded; i++)
               {
                  params.addAll(functionParams);
               }
            }
         }

         if(expectedNoOfParams != null)
         {
            ////////////////////////////////////////////////////////////////////////////
            // deal with an 'otherFieldName' being specified instead of a values list //
            ////////////////////////////////////////////////////////////////////////////
            if(expectedNoOfParams.equals(1) && StringUtils.hasContent(criterion.getOtherFieldName()))
            {
               JoinsContext.FieldAndTableNameOrAlias otherFieldAndTableNameOrAlias = joinsContext.getFieldAndTableNameOrAlias(criterion.getOtherFieldName());

               String otherColumn = escapeIdentifier(otherFieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(getColumnName(otherFieldAndTableNameOrAlias.field()));
               clause = new StringBuilder(clause.toString().replace("?", otherColumn));

               /////////////////////////////////////////////////////////////////////
               // make sure we don't add any values in this case, just in case... //
               /////////////////////////////////////////////////////////////////////
               values = Collections.emptyList();
            }
            else if(!expectedNoOfParams.equals(values.size()))
            {
               throw new IllegalArgumentException("Incorrect number of values given for criteria [" + fieldAndTableNameOrAlias.field().getName() + "] (expected " + expectedNoOfParams + ", received " + values.size() + ")");
            }

            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // replace any expression-type values with their evaluation                                                                         //
            // also, "scrub" non-expression values, which type-converts them (e.g., strings in various supported date formats become LocalDate) //
            //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            ListIterator<Serializable> valueListIterator = values.listIterator();
            while(valueListIterator.hasNext())
            {
               Serializable value = valueListIterator.next();
               if(value instanceof AbstractFilterExpression<?> expression)
               {
                  try
                  {
                     valueListIterator.set(expression.evaluate(Objects.requireNonNullElse(virtualField, field)));
                  }
                  catch(QException qe)
                  {
                     LOG.warn("Unexpected exception caught evaluating expression", qe);
                  }
               }
               else
               {
                  Serializable scrubbedValue;
                  if(fieldFunction != null)
                  {
                     FieldFunctionType fieldFunctionType = FieldFunctionTypeRegistry.ofOrWithNew(QContext.getQInstance()).getFieldFunctionType(fieldFunction.getFunctionTypeIdentifier());
                     scrubbedValue = scrubValue(fieldFunctionType.getReturnType(), value);
                  }
                  else
                  {
                     scrubbedValue = scrubValue(field, value);
                  }
                  valueListIterator.set(scrubbedValue);
               }
            }
         }

         /////////////////////////////////////////////////////////////////////////////////
         // finally, wrap the whole clause in ()'s and append it to the list of clauses //
         /////////////////////////////////////////////////////////////////////////////////
         clauses.add("(" + clause + ")");

         params.addAll(values);
      }

      //////////////////////////////////////////////////////////////////////////////
      // since we're skipping criteria w/o a field or operator in the loop -      //
      // we can get to the end here without any clauses... so, return a null here //
      //////////////////////////////////////////////////////////////////////////////
      if(clauses.isEmpty())
      {
         return (Optional.empty());
      }

      return (Optional.of(String.join(" " + booleanOperator.toString() + " ", clauses)));
   }



   /***************************************************************************
    * do an {@code Objects.requireNonNull} check on a fieldFunctionAdapter -
    * throwing {@code NullPointerException} with a detailed message if it is null.
    ***************************************************************************/
   protected void requireFieldFunctionAdapterNotNull(RDBMSFieldFunctionAdapterInterface fieldFunctionAdapter, FieldFunction fieldFunction)
   {
      Objects.requireNonNull(fieldFunctionAdapter, "Missing field function adapter for function ["
         + ObjectUtils.tryElse(() -> fieldFunction.getFunctionTypeIdentifier().getName(), "unknown")
         + "] in backend ["
         + ObjectUtils.tryElse(() -> backendMetaData.getName(), "unknown") + "]");
   }



   /***************************************************************************
    * Build a function that maps a QQQ field name to its fully-qualified,
    * escaped SQL column reference (e.g., {@code "timeZone"} →
    * {@code `t1`.`time_zone`}).  Useful when a field-function adapter needs
    * to reference additional columns on the same row.
    ***************************************************************************/
   protected Function<String, String> makeFieldNameToColumnReferenceFunction(String tableNameOrAlias, QTableMetaData table)
   {
      return fieldName -> escapeIdentifier(tableNameOrAlias) + "." + escapeIdentifier(getColumnName(table.getField(fieldName)));
   }



   /*******************************************************************************
    ** Escape an identifier (table name, column name) with the appropriate quote
    ** character for the database vendor.
    **
    ** Uses the action strategy's getIdentifierQuoteString() method to determine
    ** the appropriate quote character. Falls back to backtick if strategy not available.
    *******************************************************************************/
   protected String escapeIdentifier(String id)
   {
      String quoteString = "`"; // Default for MySQL/H2/SQLite

      try
      {
         /////////////////////////////////////////////////////////////
         // Primary: Try to get from action strategy if initialized //
         /////////////////////////////////////////////////////////////
         RDBMSActionStrategyInterface strategy = getActionStrategy();
         if(strategy != null)
         {
            quoteString = strategy.getIdentifierQuoteString();
         }
         ////////////////////////////////////////////////////////////////////
         // Fallback: If strategy is null, get it from backend in QContext //
         ////////////////////////////////////////////////////////////////////
         else if(QContext.getQInstance() != null && QContext.getQInstance().getBackends() != null)
         {
            for(QBackendMetaData backend : QContext.getQInstance().getBackends().values())
            {
               if(backend instanceof RDBMSBackendMetaData rdbmsBackend)
               {
                  RDBMSActionStrategyInterface backendStrategy = rdbmsBackend.getActionStrategy();
                  if(backendStrategy != null)
                  {
                     quoteString = backendStrategy.getIdentifierQuoteString();
                     break; // Use first RDBMS backend found
                  }
               }
            }
         }
      }
      catch(Exception e)
      {
         ///////////////////////////////
         // Ignore - will use default //
         ///////////////////////////////
      }

      ////////////////////////////////////////////////
      // Return identifier with appropriate quoting //
      ////////////////////////////////////////////////
      if(quoteString == null || quoteString.isEmpty())
      {
         return id;
      }
      else
      {
         return quoteString + id + quoteString;
      }
   }



   /*******************************************************************************
    ** Extract a field value from a ResultSet using the action strategy.
    **
    ** Delegates to the configured RDBMSActionStrategyInterface to handle vendor-
    ** specific type mappings and conversions when reading values from result sets.
    **
    ** @param type the QQQ field type to extract
    ** @param resultSet the JDBC ResultSet to read from
    ** @param i the 1-based column index in the result set
    ** @return the extracted value as a Serializable
    ** @throws SQLException if the value cannot be read from the result set
    *******************************************************************************/
   protected Serializable getFieldValueFromResultSet(QFieldType type, ResultSet resultSet, int i) throws SQLException
   {
      return (actionStrategy.getFieldValueFromResultSet(type, resultSet, i));
   }



   /*******************************************************************************
    ** Extract a field value from a ResultSet using field metadata.
    **
    ** Convenience method that extracts the type from field metadata and delegates
    ** to the type-based extraction method.
    **
    ** @param qFieldMetaData the field metadata containing the type information
    ** @param resultSet the JDBC ResultSet to read from
    ** @param i the 1-based column index in the result set
    ** @return the extracted value as a Serializable
    ** @throws SQLException if the value cannot be read from the result set
    *******************************************************************************/
   protected Serializable getFieldValueFromResultSet(QFieldMetaData qFieldMetaData, ResultSet resultSet, int i) throws SQLException
   {
      return (getFieldValueFromResultSet(qFieldMetaData.getType(), resultSet, i));
   }



   /*******************************************************************************
    ** Build the ORDER BY clause for a SQL query.
    **
    ** Constructs ORDER BY clause from a list of QFilterOrderBy specifications.
    ** Handles regular field ordering, aggregate function ordering, and group-by
    ** ordering. Resolves field names through the joins context to ensure proper
    ** table aliasing. Applies ASC/DESC direction per each order-by specification.
    **
    ** @param table the main table metadata (used for aggregate field resolution)
    ** @param orderBys list of order-by specifications to convert to SQL
    ** @param joinsContext context for resolving field names to table aliases
    ** @param params bind-params for the query - which may get appended to in here
    *                for fieldFunctions (e.g., substr(x, ?, ?))
    * @return comma-separated ORDER BY clause string
    *******************************************************************************/
   protected String makeOrderByClause(QTableMetaData table, List<QFilterOrderBy> orderBys, JoinsContext joinsContext, List<Serializable> params)
   {
      List<String> clauses = new ArrayList<>();

      for(QFilterOrderBy orderBy : orderBys)
      {
         String ascOrDesc = orderBy.getIsAscending() ? "ASC" : "DESC";
         if(orderBy instanceof QFilterOrderByAggregate orderByAggregate)
         {
            Aggregate aggregate = orderByAggregate.getAggregate();
            String    clause    = (aggregate.getOperator() + "(" + escapeIdentifier(getColumnName(table.getField(aggregate.getFieldName()))) + ")");
            clauses.add(clause + " " + ascOrDesc);
         }
         else if(orderBy instanceof QFilterOrderByGroupBy orderByGroupBy)
         {
            clauses.add(getSingleGroupByClause(orderByGroupBy.getGroupBy(), joinsContext, params, true) + " " + ascOrDesc);
         }
         else
         {
            JoinsContext.FieldAndTableNameOrAlias fieldAndTableNameOrAlias = joinsContext.getFieldAndTableNameOrAlias(orderBy.getFieldName(), true);
            QFieldMetaData                        field                    = fieldAndTableNameOrAlias.field();

            String column         = getColumnName(field);
            String tableDotColumn = escapeIdentifier(fieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(column);

            if(field instanceof QVirtualFieldMetaData virtualField && virtualField.getFieldFunction() != null)
            {
               String         fieldTableName = joinsContext.resolveTableNameOrAliasToTableName(fieldAndTableNameOrAlias.tableNameOrAlias());
               String         realFieldName  = virtualField.getFieldFunction().getFieldName();
               QFieldMetaData realField      = joinsContext.getTable(fieldTableName).getField(realFieldName);
               tableDotColumn = escapeIdentifier(fieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(getColumnName(realField));

               QTableMetaData                     fieldTable           = joinsContext.getTable(fieldTableName);
               FieldFunction                      fieldFunction        = virtualField.getFieldFunction();
               RDBMSFieldFunctionAdapterInterface fieldFunctionAdapter = backendMetaData.getFieldFunctionAdapter(fieldFunction.getFunctionTypeIdentifier());
               requireFieldFunctionAdapterNotNull(fieldFunctionAdapter, fieldFunction);

               tableDotColumn = fieldFunctionAdapter.wrapColumnNameForOrderBy(tableDotColumn, fieldFunction, makeFieldNameToColumnReferenceFunction(fieldAndTableNameOrAlias.tableNameOrAlias(), fieldTable));

               CollectionUtils.addAllIfNotNull(params, fieldFunctionAdapter.getParams(fieldFunction));
            }

            clauses.add(tableDotColumn + " " + ascOrDesc);
         }
      }

      return (String.join(", ", clauses));
   }



   /*******************************************************************************
    * Build a single GROUP BY clause element with optional formatting.
    *
    * Constructs a GROUP BY clause for a single field, resolving the field name
    * through the joins context and applying any format string (e.g., for date
    * truncation functions like DATE_TRUNC).
    *
    * @param groupBy the group-by specification containing field name and optional format
    * @param joinsContext context for resolving field names to table aliases
    * @param params bind-params for the query - which may get appended to in here (for fieldFunctions)
    * @param isForOrderBy whether this is for an ORDER BY clause
    * @return the GROUP BY clause element (e.g., "table.field" or "DATE(table.field)")
    *******************************************************************************/
   protected String getSingleGroupByClause(GroupBy groupBy, JoinsContext joinsContext, List<Serializable> params, boolean isForOrderBy)
   {
      JoinsContext.FieldAndTableNameOrAlias fieldAndTableNameOrAlias = joinsContext.getFieldAndTableNameOrAlias(groupBy.getFieldName(), true /* allowVirtualFields */);
      QFieldMetaData                        field                    = fieldAndTableNameOrAlias.field();

      String fullFieldName;
      if(field instanceof QVirtualFieldMetaData virtualField && virtualField.getFieldFunction() != null)
      {
         String         fieldTableName = joinsContext.resolveTableNameOrAliasToTableName(fieldAndTableNameOrAlias.tableNameOrAlias());
         String         realFieldName  = virtualField.getFieldFunction().getFieldName();
         QFieldMetaData realField      = joinsContext.getTable(fieldTableName).getField(realFieldName);
         String         columnName     = escapeIdentifier(fieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(getColumnName(realField));

         FieldFunction                      fieldFunction              = virtualField.getFieldFunction();
         RDBMSFieldFunctionAdapterInterface fieldFunctionAdapter       = backendMetaData.getFieldFunctionAdapter(fieldFunction.getFunctionTypeIdentifier());
         QTableMetaData                     fieldTable                 = joinsContext.getTable(fieldTableName);
         Function<String, String>           fieldNameToColumnReference = makeFieldNameToColumnReferenceFunction(fieldAndTableNameOrAlias.tableNameOrAlias(), fieldTable);
         requireFieldFunctionAdapterNotNull(fieldFunctionAdapter, fieldFunction);

         fullFieldName = isForOrderBy
            ? fieldFunctionAdapter.wrapColumnNameForOrderBy(columnName, fieldFunction, fieldNameToColumnReference)
            : fieldFunctionAdapter.wrapColumnName(columnName, fieldFunction, fieldNameToColumnReference);

         CollectionUtils.addAllIfNotNull(params, fieldFunctionAdapter.getParams(fieldFunction));
      }
      else
      {
         fullFieldName = escapeIdentifier(fieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(getColumnName(field));
      }

      if(groupBy.getFormatString() == null)
      {
         return (fullFieldName);
      }
      else
      {
         return (String.format(groupBy.getFormatString(), fullFieldName));
      }
   }



   /*******************************************************************************
    ** Log SQL statements and parameters when debug logging is enabled.
    **
    ** Conditionally logs SQL statements, parameter values, and execution timing
    ** based on system properties (qqq.rdbms.logSQL, qqq.rdbms.logSQL.output,
    ** qqq.rdbms.logSQL.reformat). Supports both logger-based output and System.out
    ** for test scenarios. Applies basic formatting to improve SQL readability when
    ** reformat flag is enabled.
    **
    ** @param sql the SQL statement to log
    ** @param params list of parameter values (limited to first 100 for performance)
    ** @param mark timestamp for calculating execution duration, or null if not tracking
    *******************************************************************************/
   protected void logSQL(CharSequence sql, List<?> params, Long mark)
   {
      if(System.getProperty("qqq.rdbms.logSQL", "false").equals("true"))
      {
         try
         {
            params = Objects.requireNonNullElse(params, Collections.emptyList());
            params = params.size() <= 100 ? params : params.subList(0, 99);

            /////////////////////////////////////////////////////////////////////////////
            // (very very) poor man's version of sql formatting... if property is true //
            /////////////////////////////////////////////////////////////////////////////
            if(System.getProperty("qqq.rdbms.logSQL.reformat", "false").equalsIgnoreCase("true"))
            {
               sql = Objects.requireNonNullElse(sql, "").toString()
                  .replaceAll("FROM ", "\nFROM\n   ")
                  .replaceAll("UNION ", "\nUNION\n   ")
                  .replaceAll("INTERSECT ", "\nINTERSECT\n   ")
                  .replaceAll("EXCEPT ", "\nEXCEPT\n   ")
                  .replaceAll("INNER", "\n   INNER")
                  .replaceAll("LEFT", "\n   LEFT")
                  .replaceAll("RIGHT", "\n   RIGHT")
                  .replaceAll("WHERE", "\nWHERE\n   ")
                  .replaceAll("ORDER BY", "\nORDER BY\n   ")
                  .replaceAll("GROUP BY", "\nGROUP BY\n   ");
            }

            if(System.getProperty("qqq.rdbms.logSQL.output", "logger").equalsIgnoreCase("system.out"))
            {
               System.out.println("SQL: " + sql);
               System.out.println("PARAMS: " + params);

               if(mark != null)
               {
                  System.out.println("SQL Took [" + QValueFormatter.formatValue(DisplayFormat.COMMAS, (System.currentTimeMillis() - mark)) + "] ms");
               }
            }
            else
            {
               LOG.debug("Running SQL", logPair("sql", sql), logPair("params", params));

               if(mark != null)
               {
                  LOG.debug("SQL Took [" + QValueFormatter.formatValue(DisplayFormat.COMMAS, (System.currentTimeMillis() - mark)) + "] ms");
               }
            }
         }
         catch(Exception e)
         {
            LOG.debug("Error logging sql...", e);
         }
      }
   }



   /*******************************************************************************
    ** Determine if a SELECT clause requires DISTINCT due to security lock joins.
    **
    ** Analyzes the table's RecordSecurityLocks to detect one-to-many joins where
    ** the base table is on the "left" side. Such joins can produce duplicate rows
    ** that must be eliminated with SELECT DISTINCT.
    **
    ** Use the current metadata because different instances and personalized tables
    ** can have different security policies despite sharing the same table name.
    **
    ** @param table the table metadata to analyze for DISTINCT requirement
    ** @return true if SELECT DISTINCT is required, false otherwise
    *******************************************************************************/
   protected boolean doesSelectClauseRequireDistinct(QTableMetaData table)
   {
      if(table == null)
      {
         return (false);
      }

      MultiRecordSecurityLock multiRecordSecurityLock = RecordSecurityLockFilters.filterForReadLockTree(CollectionUtils.nonNullList(table.getRecordSecurityLocks()));
      return doesMultiLockRequireDistinct(multiRecordSecurityLock, table);
   }



   /*******************************************************************************
    ** Recursively check if a multi-lock structure requires DISTINCT.
    **
    ** Examines a MultiRecordSecurityLock and all its child locks, looking for
    ** one-to-many or many-to-one joins where the query table is not the expected
    ** side. Such configurations require DISTINCT to eliminate duplicate rows
    ** caused by security join fan-out.
    **
    ** @param multiRecordSecurityLock the multi-lock structure to analyze
    ** @param table the base table being queried
    ** @return true if any lock in the structure requires DISTINCT
    *******************************************************************************/
   private boolean doesMultiLockRequireDistinct(MultiRecordSecurityLock multiRecordSecurityLock, QTableMetaData table)
   {
      for(RecordSecurityLock recordSecurityLock : multiRecordSecurityLock.getLocks())
      {
         if(recordSecurityLock instanceof MultiRecordSecurityLock childMultiLock)
         {
            if(doesMultiLockRequireDistinct(childMultiLock, table))
            {
               return (true);
            }
         }

         for(String joinName : CollectionUtils.nonNullList(recordSecurityLock.getJoinNameChain()))
         {
            QJoinMetaData joinMetaData = QContext.getQInstance().getJoin(joinName);
            if(JoinType.ONE_TO_MANY.equals(joinMetaData.getType()) && !joinMetaData.getRightTable().equals(table.getName()))
            {
               return (true);
            }
            else if(JoinType.MANY_TO_ONE.equals(joinMetaData.getType()) && !joinMetaData.getLeftTable().equals(table.getName()))
            {
               return (true);
            }
         }
      }

      return (false);
   }



   /*******************************************************************************
    ** Record SQL query text and join table names in the QueryStat for metrics.
    **
    ** Updates the QueryStat (if present) with the generated SQL text and the set
    ** of table names involved in joins. Used for query performance tracking,
    ** logging, and analytics.
    **
    ** @param sql the generated SQL query text to record
    ** @param joinsContext context containing join information, or null if no joins
    *******************************************************************************/
   protected void setSqlAndJoinsInQueryStat(CharSequence sql, JoinsContext joinsContext)
   {
      if(queryStat != null)
      {
         queryStat.setQueryText(sql.toString());

         if(joinsContext != null && CollectionUtils.nullSafeHasContents(joinsContext.getQueryJoins()))
         {
            Set<String> joinTableNames = new HashSet<>();
            for(QueryJoin queryJoin : joinsContext.getQueryJoins())
            {
               joinTableNames.add(queryJoin.getJoinTable());
            }
            queryStat.setJoinTableNames(joinTableNames);
         }
      }
   }



   /*******************************************************************************
    ** Getter for queryStat.
    **
    ** @return the current QueryStat instance used for performance tracking
    *******************************************************************************/
   public QueryStat getQueryStat()
   {
      return (this.queryStat);
   }



   /*******************************************************************************
    ** Setter for queryStat.
    **
    ** @param queryStat the QueryStat instance to use for performance tracking
    *******************************************************************************/
   public void setQueryStat(QueryStat queryStat)
   {
      this.queryStat = queryStat;
   }



   /*******************************************************************************
    ** Cancel an in-progress SQL query.
    **
    ** Sets the cancellation flag and attempts to cancel the current PreparedStatement.
    ** Used to support user-initiated query cancellation or timeout enforcement for
    ** long-running operations. Gracefully handles cases where no statement is active.
    *******************************************************************************/
   protected void doCancelQuery()
   {
      isCancelled = true;
      if(statement == null)
      {
         LOG.warn("Statement was null when requested to cancel query");
         return;
      }

      try
      {
         statement.cancel();
      }
      catch(SQLException e)
      {
         LOG.warn("Error trying to cancel query (statement)", e);
      }
   }



   /*******************************************************************************
    ** Either clone the input filter (so we can change it safely), or return a new blank filter.
    **
    ** Ensures immutability of input filters by creating a deep copy before applying
    ** security criteria or other modifications. Returns new empty filter if input is null.
    **
    ** @param filter the filter to clone, or null
    ** @return cloned filter if input was non-null, otherwise a new empty filter
    *******************************************************************************/
   protected QQueryFilter clonedOrNewFilter(QQueryFilter filter)
   {
      if(filter == null)
      {
         return (new QQueryFilter());
      }
      else
      {
         return (filter.clone());
      }
   }



   /*******************************************************************************
    ** Setter for backendMetaData.
    **
    ** Initializes the backend metadata and associated action strategy. The action
    ** strategy provides database vendor-specific SQL generation and type handling.
    **
    ** @param backendMetaData the backend metadata (must be RDBMSBackendMetaData)
    *******************************************************************************/
   protected void setBackendMetaData(QBackendMetaData backendMetaData)
   {
      this.backendMetaData = (RDBMSBackendMetaData) backendMetaData;
      this.actionStrategy = this.backendMetaData.getActionStrategy();
   }



   /*******************************************************************************
    ** Getter for the action strategy.
    **
    ** Returns the database vendor-specific strategy implementation used for SQL
    ** generation, type conversions, and dialect-specific behavior.
    **
    ** @return the configured RDBMSActionStrategyInterface instance
    *******************************************************************************/
   protected RDBMSActionStrategyInterface getActionStrategy()
   {
      return (this.actionStrategy);
   }
}
