/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.interfaces.QueryInterface;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ActionTimeoutHelper;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.AssociatedRecordDiscovery;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.UniqueKeyLookup;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.JoinsContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.Pair;
import com.kingsrook.qqq.backend.module.rdbms.fieldfunctions.RDBMSFieldFunctionAdapterInterface;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 **
 *******************************************************************************/
public class RDBMSQueryAction extends AbstractRDBMSAction implements QueryInterface
{
   private static final QLogger LOG = QLogger.getLogger(RDBMSQueryAction.class);

   private ActionTimeoutHelper actionTimeoutHelper;

   private static boolean mysqlResultSetOptimizationEnabled = false;

   static
   {
      try
      {
         mysqlResultSetOptimizationEnabled = new QMetaDataVariableInterpreter().getBooleanFromPropertyOrEnvironment("qqq.rdbms.mysql.resultSetOptimizationEnabled", "QQQ_RDBMS_MYSQL_RESULT_SET_OPTIMIZATION_ENABLED", false);
      }
      catch(Exception e)
      {
         LOG.warn("Error reading property/env for mysqlResultSetOptimizationEnabled", e);
      }
   }

   /*******************************************************************************
    **
    *******************************************************************************/
   public QueryOutput execute(QueryInput queryInput) throws QException
   {
      return execute(queryInput, false);
   }



   /*******************************************************************************
    ** Native declared-key lookups retain database equality and caller transactions.
    *******************************************************************************/
   @Override
   public List<QRecord> lookupUniqueKey(UniqueKeyLookup.Input input) throws QException
   {
      return execute(input.newQueryInput(), true).getRecords();
   }



   /*******************************************************************************
    ** Private parent tuples retain physical values and the caller's transaction.
    *******************************************************************************/
   @Override
   public List<QRecord> readAssociationValues(AssociatedRecordDiscovery.StoredValuesInput input) throws QException
   {
      return execute(input.newQueryInput(), true).getRecords();
   }



   /*******************************************************************************
    ** Structural membership ignores row locks without changing query input or context.
    *******************************************************************************/
   @Override
   public List<Serializable> findAssociatedPrimaryKeys(AssociatedRecordDiscovery.Input input) throws QException
   {
      QueryInput queryInput = input.newQueryInput();
      return execute(queryInput, true).getRecords().stream()
         .map(record -> record.getValue(queryInput.getTable().getPrimaryKeyField())).toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QueryOutput execute(QueryInput queryInput, boolean omitSecurity) throws QException
   {
      try
      {
         QTableMetaData table     = queryInput.getTable();
         String         tableName = queryInput.getTableName();
         setBackendMetaData(queryInput.getBackend());

         List<Serializable> params    = new ArrayList<>();
         Selection          selection = makeSelection(queryInput);
         CollectionUtils.addAllIfNotNull(params, selection.paramsForSelection());

         StringBuilder sql = makeSQL(queryInput, selection, tableName, params, table, omitSecurity);

         Connection connection;
         boolean    needToCloseConnection = false;
         if(queryInput.getTransaction() != null && queryInput.getTransaction() instanceof RDBMSTransaction rdbmsTransaction)
         {
            connection = rdbmsTransaction.getConnection();
         }
         else
         {
            connection = getConnection(queryInput);
            needToCloseConnection = true;
         }

         Long mark = System.currentTimeMillis();

         try
         {
            statement = createStatement(connection, sql.toString(), queryInput);

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // set up & start an actionTimeoutHelper (note, internally it'll deal with the time being null or negative as meaning not to timeout) //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            actionTimeoutHelper = new ActionTimeoutHelper(queryInput.getTimeoutSeconds(), TimeUnit.SECONDS, new StatementTimeoutCanceller(statement, sql));
            actionTimeoutHelper.start();

            ///////////////////////////////////////////////////////////////////////////////////////////////////
            // to avoid counting time spent acquiring a connection, re-set the queryStat startTimestamp here //
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            if(queryStat != null)
            {
               queryStat.setStartTimestamp(Instant.now());
            }

            //////////////////////////////////////////////
            // execute the query - iterate over results //
            //////////////////////////////////////////////
            QueryOutput queryOutput = new QueryOutput(queryInput);

            getActionStrategy().executeStatement(statement, sql, ((ResultSet resultSet) ->
            {
               /////////////////////////////////////////////////////////////////////////
               // once we've started getting results, go ahead and cancel the timeout //
               /////////////////////////////////////////////////////////////////////////
               actionTimeoutHelper.cancel();

               ResultSetMetaData metaData = resultSet.getMetaData();
               while(resultSet.next())
               {
                  setQueryStatFirstResultTime();

                  QRecord record = new QRecord();
                  record.setTableName(table.getName());
                  LinkedHashMap<String, Serializable> values = new LinkedHashMap<>();
                  record.setValues(values);

                  for(int i = 1; i <= metaData.getColumnCount(); i++)
                  {
                     QFieldMetaData field = selection.fields().get(i - 1);

                     if(!queryInput.getShouldFetchHeavyFields() && field.getIsHeavy())
                     {
                        ///////////////////////////////////////////////////////////////////////////////////
                        // if this is a non-fetched heavy field (e.g., we just fetched its length), then //
                        // get the value here as an INTEGER, not a BLOB or whatever the field would be   //
                        ///////////////////////////////////////////////////////////////////////////////////
                        Serializable fieldLength = getFieldValueFromResultSet(QFieldType.INTEGER, resultSet, i);
                        setHeavyFieldLengthInRecordBackendDetails(record, field, fieldLength);
                     }
                     else
                     {
                        Serializable value = getFieldValueFromResultSet(field, resultSet, i);
                        values.put(field.getName(), value);
                     }
                  }

                  queryOutput.addRecord(record);

                  if(queryInput.getAsyncJobCallback().wasCancelRequested())
                  {
                     LOG.info("Breaking query job, as requested.");
                     break;
                  }
               }

               /////////////////////////////////////////////////////////////////
               // in case there were no results, set the firstResultTime here //
               /////////////////////////////////////////////////////////////////
               setQueryStatFirstResultTime();

            }), params);

            return queryOutput;
         }
         finally
         {
            logSQL(sql, params, mark);

            if(actionTimeoutHelper != null)
            {
               /////////////////////////////////////////
               // make sure the timeout got cancelled //
               /////////////////////////////////////////
               actionTimeoutHelper.cancel();
            }

            if(needToCloseConnection)
            {
               connection.close();
            }
         }
      }
      catch(Exception e)
      {
         if(actionTimeoutHelper != null && actionTimeoutHelper.getDidTimeout())
         {
            setQueryStatFirstResultTime();
            throw (new QUserFacingException("Query timed out."));
         }

         if(isCancelled)
         {
            throw (new QUserFacingException("Query was cancelled."));
         }

         LOG.warn("Error executing query", e, logPair("tableName", queryInput.getTableName()), logPair("filter", queryInput.getFilter()));
         throw new QException("Error executing query", e);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private StringBuilder makeSQL(QueryInput queryInput, Selection selection, String tableName, List<Serializable> params, QTableMetaData table, boolean omitSecurity) throws QException
   {
      QQueryFilter filter = clonedOrNewFilter(queryInput.getFilter());
      JoinsContext joinsContext = omitSecurity ? new JoinsContext(tableName, filter, true)
         : new JoinsContext(QContext.getQInstance(), queryInput, filter);

      StringBuilder sql = new StringBuilder();

      if(filter != null && filter.getSubFilterSetOperator() != null && CollectionUtils.nullSafeHasContents(filter.getSubFilters()))
      {
         boolean isFirst = true;
         for(QQueryFilter subFilter : filter.getSubFilters())
         {
            if(!sql.isEmpty())
            {
               sql.append(" ").append(filter.getSubFilterSetOperator().name().replace('_', ' ')).append(" ");
            }

            ////////////////////////////////////////////////////////////////////////////////////////////////////
            // add selection params for each sub-query after the first (the first set was added by the caller //
            // before this method was invoked).                                                               //
            ////////////////////////////////////////////////////////////////////////////////////////////////////
            if(!isFirst)
            {
               CollectionUtils.addAllIfNotNull(params, selection.paramsForSelection());
            }
            isFirst = false;

            sql.append(" (");
            sql.append(selection.selectClause());
            sql.append(" FROM ").append(makeFromClause(QContext.getQInstance(), tableName, joinsContext, params));
            sql.append(" WHERE ").append(makeWhereClause(joinsContext, subFilter, params));
            sql.append(") ");
         }

         if(CollectionUtils.nullSafeHasContents(filter.getOrderBys()))
         {
            /////////////////////////////////////////////////////////////////////////////////////////////////////////
            // the base version of makeOrderByClause uses `table`.`column` style references - which don't work for //
            // these kinds of queries... so, use this version, which does index-based ones (maybe we could/should  //
            // switch to always use those?                                                                         //
            // the best here might be, to alias all columns, and then use those aliases in both versions...        //
            /////////////////////////////////////////////////////////////////////////////////////////////////////////
            sql.append(" ORDER BY ").append(makeOrderByClauseForSubFilterSetOperationQuery(table, filter.getOrderBys(), joinsContext, selection));
         }
      }
      else
      {
         sql.append(selection.selectClause());
         sql.append(" FROM ").append(makeFromClause(QContext.getQInstance(), tableName, joinsContext, params));
         sql.append(" WHERE ").append(makeWhereClause(joinsContext, filter, params));

         if(filter != null && CollectionUtils.nullSafeHasContents(filter.getOrderBys()))
         {
            sql.append(" ORDER BY ").append(makeOrderByClause(table, filter.getOrderBys(), joinsContext, params));
         }
      }

      if(filter != null && filter.getLimit() != null)
      {
         sql.append(" LIMIT ").append(filter.getLimit());

         if(filter.getSkip() != null)
         {
            // todo - other sql grammars?
            sql.append(" OFFSET ").append(filter.getSkip());
         }
      }

      // todo sql customization - can edit sql and/or param list

      setSqlAndJoinsInQueryStat(sql, joinsContext);
      return sql;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private String makeOrderByClauseForSubFilterSetOperationQuery(QTableMetaData table, List<QFilterOrderBy> orderBys, JoinsContext joinsContext, Selection selection)
   {
      List<String> clauses = new ArrayList<>();

      for(QFilterOrderBy orderBy : orderBys)
      {
         String                                ascOrDesc                     = orderBy.getIsAscending() ? "ASC" : "DESC";
         JoinsContext.FieldAndTableNameOrAlias otherFieldAndTableNameOrAlias = joinsContext.getFieldAndTableNameOrAlias(orderBy.getFieldName());

         QFieldMetaData field  = otherFieldAndTableNameOrAlias.field();
         String         column = getColumnName(field);

         String qualifiedColumn = escapeIdentifier(otherFieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(column);
         String columnNo        = String.valueOf(selection.qualifiedColumns.indexOf(qualifiedColumn) + 1);
         clauses.add(columnNo + " " + ascOrDesc);

      }
      return (String.join(", ", clauses));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private static void setHeavyFieldLengthInRecordBackendDetails(QRecord record, QFieldMetaData field, Serializable fieldLength)
   {
      if(record.getBackendDetails() == null)
      {
         record.setBackendDetails(new HashMap<>());
      }

      if(record.getBackendDetail(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS) == null)
      {
         record.addBackendDetail(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS, new HashMap<>());
      }

      ((Map<String, Serializable>) record.getBackendDetail(QRecord.BACKEND_DETAILS_TYPE_HEAVY_FIELD_LENGTHS)).put(field.getName(), fieldLength);
   }



   /***************************************************************************
    ** output wrapper for makeSelection method.
    ** - selectClause is everything from SELECT up to (but not including) FROM
    ** - qualifiedColumns is a list of the `table`.`column` strings
    ** - fields are those being selected, in the same order, and with mutated
    ** names for join fields.
    ** - any parameters required for functions in the SELECT clause
    ***************************************************************************/
   private record Selection(String selectClause, List<String> qualifiedColumns, List<QFieldMetaData> fields, List<Serializable> paramsForSelection)
   {

   }



   /*******************************************************************************
    ** For a given queryInput, determine what fields are being selected - returning
    ** a record containing the SELECT clause, as well as a List of QFieldMetaData
    ** representing those fields - where - note - the names for fields from join
    ** tables will be prefixed by the join table nameOrAlias.
    *******************************************************************************/
   private Selection makeSelection(QueryInput queryInput) throws QException
   {
      QInstance       instance   = QContext.getQInstance();
      String          tableName  = queryInput.getTableName();
      List<QueryJoin> queryJoins = queryInput.getQueryJoins();
      QTableMetaData  table      = queryInput.getTable();

      Set<String> fieldNamesToInclude = queryInput.getFieldNamesToInclude();

      ///////////////////////////////////////////////////////////////////////////////////////////////
      // start with the main table's fields, optionally filtered by the set of fieldNamesToInclude //
      ///////////////////////////////////////////////////////////////////////////////////////////////
      List<QFieldMetaData> fieldList = table.getFields().values()
         .stream().filter(field -> fieldNamesToInclude == null || fieldNamesToInclude.contains(field.getName()))
         .toList();

      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      // map those field names to columns, joined with ", ".                                                              //
      // if a field is heavy, and heavy fields aren't being selected, then replace that field name with a LENGTH function //
      //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
      List<String> qualifiedColumns = new ArrayList<>(fieldList.stream()
         .map(field -> Pair.of(field, escapeIdentifier(tableName) + "." + escapeIdentifier(getColumnName(field))))
         .map(pair -> wrapHeavyFieldsWithLengthFunctionIfNeeded(pair, queryInput.getShouldFetchHeavyFields()))
         .toList());

      StringBuilder        columns            = new StringBuilder(String.join(", ", qualifiedColumns));
      List<QFieldMetaData> selectionFieldList = new ArrayList<>(fieldList);
      List<Serializable>   params             = new ArrayList<>();

      /////////////////////////////////////////
      // add virtual fields if there are any //
      /////////////////////////////////////////
      addVirtualFieldsToSelection(table, tableName, fieldNamesToInclude, columns, params, selectionFieldList, false);

      ///////////////////////////////////////////////////////////////////////////////////////////////////////////
      // figure out if distinct is being used.  then start building the select clause with the table's columns //
      ///////////////////////////////////////////////////////////////////////////////////////////////////////////
      boolean              requiresDistinct   = queryInput.getSelectDistinct() || doesSelectClauseRequireDistinct(table);
      StringBuilder        selectClause       = new StringBuilder((requiresDistinct) ? "SELECT DISTINCT " : "SELECT ").append(columns);

      boolean needCommaBeforeJoinFields = !columns.isEmpty();

      ///////////////////////////////////
      // add any 'selected' queryJoins //
      ///////////////////////////////////
      for(QueryJoin queryJoin : CollectionUtils.nonNullList(queryJoins))
      {
         if(queryJoin.getSelect())
         {
            QTableMetaData joinTable        = instance.getTable(queryJoin.getJoinTable());
            String         tableNameOrAlias = queryJoin.getJoinTableOrItsAlias();
            if(joinTable == null)
            {
               throw new QException("Requested join table [" + queryJoin.getJoinTable() + "] is not a defined table.");
            }

            ///////////////////////////////////////////////////////////////////////////
            // make sure personalization is applied to the join table, if applicable //
            ///////////////////////////////////////////////////////////////////////////
            joinTable = TableMetaDataPersonalizerAction.execute(new TableMetaDataPersonalizerInput().withTableMetaData(joinTable).withInputSource(queryInput.getInputSource()));
            if(joinTable == null)
            {
               throw (new QException("Query join table is not available"));
            }

            ///////////////////////////////////
            // filter by fieldNamesToInclude //
            ///////////////////////////////////
            List<QFieldMetaData> joinFieldList = joinTable.getFields().values()
               .stream().filter(field -> fieldNamesToInclude == null || fieldNamesToInclude.contains(tableNameOrAlias + "." + field.getName()))
               .toList();
            if(joinFieldList.isEmpty())
            {
               continue;
            }

            /////////////////////////////////////////////////////
            // map to columns, wrapping heavy fields as needed //
            /////////////////////////////////////////////////////
            List<String> qualifiedJoinColumns = joinFieldList.stream()
               .map(field -> Pair.of(field, escapeIdentifier(tableNameOrAlias) + "." + escapeIdentifier(getColumnName(field))))
               .map(pair -> wrapHeavyFieldsWithLengthFunctionIfNeeded(pair, queryInput.getShouldFetchHeavyFields()))
               .toList();

            qualifiedColumns.addAll(qualifiedJoinColumns);
            StringBuilder joinColumns = new StringBuilder(String.join(", ", qualifiedJoinColumns));
            selectionFieldList.addAll(joinFieldList.stream().map(field -> field.clone().withName(tableNameOrAlias + "." + field.getName())).toList());

            /////////////////////////////////////////
            // add virtual fields if there are any //
            /////////////////////////////////////////
            addVirtualFieldsToSelection(joinTable, tableNameOrAlias, fieldNamesToInclude, joinColumns, params, selectionFieldList, true);

            ////////////////////////////////////////////////////////////////////////////////////////////////
            // append to output objects.                                                                  //
            // note that fields are cloned, since we are changing their names to have table/alias prefix. //
            ////////////////////////////////////////////////////////////////////////////////////////////////
            if(needCommaBeforeJoinFields)
            {
               selectClause.append(", ");
            }
            selectClause.append(joinColumns);
            needCommaBeforeJoinFields = true;
         }
      }

      return (new Selection(selectClause.toString(), qualifiedColumns, selectionFieldList, params));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   private void addVirtualFieldsToSelection(QTableMetaData table, String tableNameOrAlias, Set<String> fieldNamesToInclude, StringBuilder columns, List<Serializable> params, List<QFieldMetaData> selectionFieldList, boolean isJoinField)
   {
      for(QVirtualFieldMetaData virtualField : CollectionUtils.nonNullMap(table.getVirtualFields()).values())
      {
         if(virtualField.getIsQuerySelectable())
         {
            FieldFunction fieldFunction = virtualField.getFieldFunction();
            if(fieldFunction == null)
            {
               ////////////////////////////////////////////////////////////////////////////////////////////////
               // if there's no field function, then we can't get the virtual field's value from the select. //
               // this means the value will come from a table customizer                                     //
               ////////////////////////////////////////////////////////////////////////////////////////////////
               continue;
            }

            if(fieldNamesToInclude != null && !fieldNamesToInclude.contains(virtualField.getName()))
            {
               continue;
            }

            RDBMSFieldFunctionAdapterInterface fieldFunctionAdapter = backendMetaData.getFieldFunctionAdapter(fieldFunction.getFunctionTypeIdentifier());
            requireFieldFunctionAdapterNotNull(fieldFunctionAdapter, fieldFunction);

            QFieldMetaData sourceField       = table.getField(fieldFunction.getFieldName());
            String         columnName        = escapeIdentifier(tableNameOrAlias) + "." + escapeIdentifier(getColumnName(sourceField));
            String         wrappedColumnName = fieldFunctionAdapter.wrapColumnName(columnName, fieldFunction, makeFieldNameToColumnReferenceFunction(tableNameOrAlias, table));

            CollectionUtils.addAllIfNotNull(params, fieldFunctionAdapter.getParams(fieldFunction));

            if(!columns.isEmpty())
            {
               columns.append(", ");
            }

            columns.append(wrappedColumnName);
            if(isJoinField)
            {
               selectionFieldList.add(virtualField.clone().withName(tableNameOrAlias + "." + virtualField.getName()));
            }
            else
            {
               selectionFieldList.add(virtualField);
            }
         }
      }
   }



   /*******************************************************************************
    ** if we're not fetching heavy fields, instead just get their length.  this
    ** method wraps the field 'sql name' (e.g., column_name or table_name.column_name)
    ** with the LENGTH() function, if needed.
    *******************************************************************************/
   private String wrapHeavyFieldsWithLengthFunctionIfNeeded(Pair<QFieldMetaData, String> fieldAndSqlName, boolean shouldFetchHeavyFields)
   {
      QFieldMetaData field   = fieldAndSqlName.getA();
      String         sqlName = fieldAndSqlName.getB();
      if(!shouldFetchHeavyFields && field.getIsHeavy())
      {
         return ("LENGTH(" + sqlName + ")");
      }
      return (sqlName);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private PreparedStatement createStatement(Connection connection, String sql, QueryInput queryInput) throws SQLException
   {
      /////////////////////////////////////////////////////////////////////////
      // if we're allowed to use the mysqlResultSetOptimization, and we have //
      // the query hint of "potentially large no of results", then check if  //
      // our backend is indeed mysql, and if so, then apply those settings.  //
      /////////////////////////////////////////////////////////////////////////
      if(mysqlResultSetOptimizationEnabled && queryInput.hasQueryHint(QueryHint.POTENTIALLY_LARGE_NUMBER_OF_RESULTS))
      {
         RDBMSBackendMetaData rdbmsBackendMetaData = (RDBMSBackendMetaData) queryInput.getBackend();

         if(RDBMSBackendMetaData.VENDOR_MYSQL.equals(rdbmsBackendMetaData.getVendor()) || RDBMSBackendMetaData.VENDOR_AURORA_MYSQL.equals(rdbmsBackendMetaData.getVendor()))
         {
            //////////////////////////////////////////////////////////////////////////////////////////////////////
            // mysql "optimization", presumably here - from Result Set section of                               //
            // https://dev.mysql.com/doc/connector-j/en/connector-j-reference-implementation-notes.html without //
            // this change, we saw ~10 seconds of "wait" time, before results would start to stream out of a    //
            // large query (e.g., > 1,000,000 rows).                                                            //
            // with this change, we start to get results immediately, and the total runtime also seems lower... //
            // perhaps more importantly, without this change, the whole result set goes into memory - but with  //
            // this change, it is streamed.                                                                     //
            //////////////////////////////////////////////////////////////////////////////////////////////////////
            PreparedStatement statement = connection.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            statement.setFetchSize(Integer.MIN_VALUE);
            return (statement);
         }
      }

      return (connection.prepareStatement(sql));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void cancelAction()
   {
      doCancelQuery();
   }
}
