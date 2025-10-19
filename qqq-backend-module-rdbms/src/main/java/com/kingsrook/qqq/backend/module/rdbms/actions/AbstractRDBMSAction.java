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
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QValueException;
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
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLockFilters;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.querystats.QueryStat;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.memoization.Memoization;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSFieldMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.strategy.RDBMSActionStrategyInterface;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Base class for all core actions in the RDBMS module.
 *******************************************************************************/
public abstract class AbstractRDBMSAction
{
   private static final QLogger LOG = QLogger.getLogger(AbstractRDBMSAction.class);

   protected QueryStat queryStat;

   protected PreparedStatement statement;
   protected boolean           isCancelled = false;

   private static Memoization<String, Boolean> doesSelectClauseRequireDistinctMemoization = new Memoization<String, Boolean>()
      .withTimeout(Duration.ofDays(365));

   private RDBMSBackendMetaData         backendMetaData;
   private RDBMSActionStrategyInterface actionStrategy;



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
    ** Handle obvious problems with values - like empty string for integer should be null,
    ** and type conversions that we can do "better" than jdbc...
    **
    *******************************************************************************/
   protected Serializable scrubValue(QFieldMetaData field, Serializable value)
   {
      if("".equals(value))
      {
         QFieldType type = field.getType();
         if(type.equals(QFieldType.INTEGER) || type.equals(QFieldType.LONG) || type.equals(QFieldType.DECIMAL) || type.equals(QFieldType.DATE) || type.equals(QFieldType.DATE_TIME) || type.equals(QFieldType.BOOLEAN))
         {
            value = null;
         }
      }

      //////////////////////////////////////////////////////////////////////////////
      // value utils is good at making values from strings - jdbc, not as much... //
      //////////////////////////////////////////////////////////////////////////////
      if(field.getType().equals(QFieldType.INTEGER) && value instanceof String)
      {
         value = ValueUtils.getValueAsInteger(value);
      }
      else if(field.getType().equals(QFieldType.LONG) && value instanceof String)
      {
         value = ValueUtils.getValueAsLong(value);
      }
      else if(field.getType().equals(QFieldType.DATE) && value instanceof String)
      {
         value = ValueUtils.getValueAsLocalDate(value);
      }
      else if(field.getType().equals(QFieldType.DATE_TIME) && value instanceof String)
      {
         value = ValueUtils.getValueAsInstant(value);
      }
      else if(field.getType().equals(QFieldType.DECIMAL) && value instanceof String)
      {
         value = ValueUtils.getValueAsBigDecimal(value);
      }
      else if(field.getType().equals(QFieldType.BOOLEAN) && value instanceof String)
      {
         value = ValueUtils.getValueAsBoolean(value);
      }

      return (value);
   }



   /*******************************************************************************
    ** If the table has a field with the given name, then set the given value in the
    ** given record.
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
    **
    *******************************************************************************/
   protected String makeFromClause(QInstance instance, String tableName, JoinsContext joinsContext, List<Serializable> params)
   {
      //////////////////////////////////////////////////////////////////////
      // start with the main table - un-aliased (well, aliased as itself) //
      //////////////////////////////////////////////////////////////////////
      StringBuilder rs = new StringBuilder(escapeIdentifier(getTableName(instance.getTable(tableName))) + " AS " + escapeIdentifier(tableName));

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
         QTableMetaData joinTable            = instance.getTable(queryJoin.getJoinTable());
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
            QTableMetaData leftTable  = instance.getTable(joinMetaData.getLeftTable());
            QTableMetaData rightTable = instance.getTable(joinMetaData.getRightTable());

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
    **
    ** @return optional sql where sub-clause, as in "x AND y"
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

         JoinsContext.FieldAndTableNameOrAlias fieldAndTableNameOrAlias = joinsContext.getFieldAndTableNameOrAlias(criterion.getFieldName());

         List<Serializable> values = criterion.getValues() == null ? new ArrayList<>() : new ArrayList<>(criterion.getValues());
         QFieldMetaData     field  = fieldAndTableNameOrAlias.field();
         String             column = escapeIdentifier(fieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(getColumnName(field));
         StringBuilder      clause = new StringBuilder();

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

         Integer expectedNoOfParams = actionStrategy.appendCriterionToWhereClause(criterion, clause, column, values, field);

         if(expectedNoOfParams != null)
         {
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
               throw new IllegalArgumentException("Incorrect number of values given for criteria [" + field.getName() + "] (expected " + expectedNoOfParams + ", received " + values.size() + ")");
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
                     valueListIterator.set(expression.evaluate(field));
                  }
                  catch(QException qe)
                  {
                     LOG.warn("Unexpected exception caught evaluating expression", qe);
                  }
               }
               else
               {
                  Serializable scrubbedValue = scrubValue(field, value);
                  valueListIterator.set(scrubbedValue);
               }
            }
         }

         clauses.add("(" + clause + ")");

         /* not ready for this at this time - would be needed when "parsing" of values (instead of expressions for relative date-times) is ready*/
         /*
         if(field.getType().equals(QFieldType.DATE_TIME))
         {
            values = evaluateDateTimeParamValues(values);
         }
         */

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



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Serializable> evaluateDateTimeParamValues(List<Serializable> values)
   {
      if(CollectionUtils.nullSafeIsEmpty(values))
      {
         return (values);
      }

      List<Serializable> rs = new ArrayList<>();
      for(Serializable value : values)
      {
         if(value instanceof Instant)
         {
            rs.add(value);
         }
         else
         {
            try
            {
               Instant valueAsInstant = ValueUtils.getValueAsInstant(value);
               rs.add(valueAsInstant);
            }
            catch(Exception e)
            {
               try
               {
                  LocalDate valueAsLocalDate = ValueUtils.getValueAsLocalDate(value);
                  rs.add(valueAsLocalDate);
                  /*
                  Optional<Instant> valueAsRelativeInstant = parseValueAsRelativeInstant(value);
                  rs.add(valueAsRelativeInstant.orElseThrow());
                  */
               }
               catch(Exception e2)
               {
                  throw (new QValueException("Parameter value [" + value + "] could not be evaluated as an absolute or relative Instant"));
               }
            }
         }
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Optional<Instant> parseValueAsRelativeInstant(Serializable value)
   {
      String valueString = ValueUtils.getValueAsString(value);
      if(valueString == null)
      {
         return (Optional.empty());
      }

      // todo - use parser!!
      return Optional.of(Instant.now());
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
         // Ignore - will use default
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
    **
    *******************************************************************************/
   protected Serializable getFieldValueFromResultSet(QFieldType type, ResultSet resultSet, int i) throws SQLException
   {
      return (actionStrategy.getFieldValueFromResultSet(type, resultSet, i));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected Serializable getFieldValueFromResultSet(QFieldMetaData qFieldMetaData, ResultSet resultSet, int i) throws SQLException
   {
      return (getFieldValueFromResultSet(qFieldMetaData.getType(), resultSet, i));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected String makeOrderByClause(QTableMetaData table, List<QFilterOrderBy> orderBys, JoinsContext joinsContext)
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
            clauses.add(getSingleGroupByClause(orderByGroupBy.getGroupBy(), joinsContext) + " " + ascOrDesc);
         }
         else
         {
            JoinsContext.FieldAndTableNameOrAlias otherFieldAndTableNameOrAlias = joinsContext.getFieldAndTableNameOrAlias(orderBy.getFieldName());

            QFieldMetaData field  = otherFieldAndTableNameOrAlias.field();
            String         column = getColumnName(field);
            clauses.add(escapeIdentifier(otherFieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(column) + " " + ascOrDesc);
         }
      }
      return (String.join(", ", clauses));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected String getSingleGroupByClause(GroupBy groupBy, JoinsContext joinsContext)
   {
      JoinsContext.FieldAndTableNameOrAlias fieldAndTableNameOrAlias = joinsContext.getFieldAndTableNameOrAlias(groupBy.getFieldName());
      String                                fullFieldName            = escapeIdentifier(fieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(getColumnName(fieldAndTableNameOrAlias.field()));
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
    **
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
    ** method that looks at security lock joins, and if a one-to-many is found where
    ** the specified field name is on the 'right side' of the join, then a distinct
    ** needs added to select clause.
    **
    ** Memoized because it's a lot of gyrations, and it never ever changes for a
    ** running server.
    *******************************************************************************/
   protected boolean doesSelectClauseRequireDistinct(QTableMetaData table)
   {
      if(table == null)
      {
         return (false);
      }

      return doesSelectClauseRequireDistinctMemoization.getResult(table.getName(), (name) ->
      {
         MultiRecordSecurityLock multiRecordSecurityLock = RecordSecurityLockFilters.filterForReadLockTree(CollectionUtils.nonNullList(table.getRecordSecurityLocks()));
         return doesMultiLockRequireDistinct(multiRecordSecurityLock, table);
      }).orElse(false);
   }



   /*******************************************************************************
    **
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
    **
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
    ** Getter for queryStat
    *******************************************************************************/
   public QueryStat getQueryStat()
   {
      return (this.queryStat);
   }



   /*******************************************************************************
    ** Setter for queryStat
    *******************************************************************************/
   public void setQueryStat(QueryStat queryStat)
   {
      this.queryStat = queryStat;
   }



   /*******************************************************************************
    **
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
    ** Setter for backendMetaData
    **
    *******************************************************************************/
   protected void setBackendMetaData(QBackendMetaData backendMetaData)
   {
      this.backendMetaData = (RDBMSBackendMetaData) backendMetaData;
      this.actionStrategy = this.backendMetaData.getActionStrategy();
   }



   /***************************************************************************
    *
    ***************************************************************************/
   protected RDBMSActionStrategyInterface getActionStrategy()
   {
      return (this.actionStrategy);
   }
}
