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
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.interfaces.AggregateInterface;
import com.kingsrook.qqq.backend.core.actions.tables.helpers.ActionTimeoutHelper;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.JoinsContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.functions.FieldFunction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.module.rdbms.fieldfunctions.RDBMSFieldFunctionAdapterInterface;


/*******************************************************************************
 **
 *******************************************************************************/
public class RDBMSAggregateAction extends AbstractRDBMSAction implements AggregateInterface
{
   private static final QLogger LOG = QLogger.getLogger(RDBMSAggregateAction.class);

   private ActionTimeoutHelper actionTimeoutHelper;



   /*******************************************************************************
    **
    *******************************************************************************/
   public AggregateOutput execute(AggregateInput aggregateInput) throws QException
   {
      try
      {
         QTableMetaData table = aggregateInput.getTable();
         setBackendMetaData(aggregateInput.getBackend());

         QQueryFilter filter       = clonedOrNewFilter(aggregateInput.getFilter());
         JoinsContext joinsContext = new JoinsContext(QContext.getQInstance(), aggregateInput, filter);

         List<Serializable> selectParams = new ArrayList<>();
         List<String>       selectClauses = buildSelectClauses(aggregateInput, joinsContext, selectParams);

         List<String>          extraGroupByPositions = new ArrayList<>();
         Map<GroupBy, Integer> orderByPositionMap    = new HashMap<>();
         populateStructuresForComplexGroupBys(filter, joinsContext, selectClauses, selectParams, extraGroupByPositions, orderByPositionMap);

         List<Serializable> params = new ArrayList<>();
         params.addAll(selectParams);

         String fromClause = makeFromClause(QContext.getQInstance(), table.getName(), joinsContext, params);

         String sql = "SELECT " + StringUtils.join(", ", selectClauses)
            + " FROM " + fromClause
            + " WHERE " + makeWhereClause(joinsContext, filter, params);

         if(CollectionUtils.nullSafeHasContents(aggregateInput.getGroupBys()))
         {
            List<Serializable> groupByParams = new ArrayList<>();
            String groupByClause = makeGroupByClause(aggregateInput, joinsContext, groupByParams);
            if(!extraGroupByPositions.isEmpty())
            {
               groupByClause += ", " + StringUtils.join(", ", extraGroupByPositions);
            }
            sql += " GROUP BY " + groupByClause;
            params.addAll(groupByParams);
         }

         if(filter != null && CollectionUtils.nullSafeHasContents(filter.getOrderBys()))
         {
            sql += " ORDER BY " + makeAggregateOrderByClause(table, filter.getOrderBys(), joinsContext, params, orderByPositionMap);
         }

         if(aggregateInput.getLimit() != null)
         {
            sql += " LIMIT " + aggregateInput.getLimit();
         }

         setSqlAndJoinsInQueryStat(sql, joinsContext);

         AggregateOutput       rs      = new AggregateOutput();
         List<AggregateResult> results = new ArrayList<>();
         rs.setResults(results);

         Long mark = System.currentTimeMillis();

         Connection connection;
         boolean    needToCloseConnection = false;
         if(aggregateInput.getTransaction() != null && aggregateInput.getTransaction() instanceof RDBMSTransaction rdbmsTransaction)
         {
            connection = rdbmsTransaction.getConnection();
         }
         else
         {
            connection = getConnection(aggregateInput);
            needToCloseConnection = true;
         }

         try
         {
            statement = connection.prepareStatement(sql);

            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // set up & start an actionTimeoutHelper (note, internally it'll deal with the time being null or negative as meaning not to timeout) //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            actionTimeoutHelper = new ActionTimeoutHelper(aggregateInput.getTimeoutSeconds(), TimeUnit.SECONDS, new StatementTimeoutCanceller(statement, sql));
            actionTimeoutHelper.start();

            ///////////////////////////////////////////////////////////////////////////////////////////////////
            // to avoid counting time spent acquiring a connection, re-set the queryStat startTimestamp here //
            ///////////////////////////////////////////////////////////////////////////////////////////////////
            if(queryStat != null)
            {
               queryStat.setStartTimestamp(Instant.now());
            }

            getActionStrategy().executeStatement(statement, sql, ((ResultSet resultSet) ->
            {
               /////////////////////////////////////////////////////////////////////////
               // once we've started getting results, go ahead and cancel the timeout //
               /////////////////////////////////////////////////////////////////////////
               actionTimeoutHelper.cancel();

               while(resultSet.next())
               {
                  setQueryStatFirstResultTime();

                  AggregateResult result = new AggregateResult();
                  results.add(result);

                  int selectionIndex = 1;
                  for(GroupBy groupBy : CollectionUtils.nonNullList(aggregateInput.getGroupBys()))
                  {
                     Serializable value = getFieldValueFromResultSet(groupBy.getType(), resultSet, selectionIndex++);
                     result.withGroupByValue(groupBy, value);
                  }

                  for(Aggregate aggregate : aggregateInput.getAggregates())
                  {
                     JoinsContext.FieldAndTableNameOrAlias fieldAndTableNameOrAlias = joinsContext.getFieldAndTableNameOrAlias(aggregate.getFieldName(), true /* allowVirtualFields */);
                     QFieldMetaData                        field                    = fieldAndTableNameOrAlias.field();

                     QFieldType fieldType = aggregate.getFieldType();
                     if(fieldType == null)
                     {
                        if((field.getType().equals(QFieldType.INTEGER) || field.getType().equals(QFieldType.LONG)) && (aggregate.getOperator().equals(AggregateOperator.AVG)))
                        {
                           fieldType = QFieldType.DECIMAL;
                        }
                        else if(field.getType().isTemporal() && (aggregate.getOperator().equals(AggregateOperator.COUNT)) || aggregate.getOperator().equals(AggregateOperator.COUNT_DISTINCT))
                        {
                           fieldType = QFieldType.INTEGER;
                        }
                     }

                     if(fieldType != null)
                     {
                        field = new QFieldMetaData().withType(fieldType);
                     }

                     Serializable value = getFieldValueFromResultSet(field, resultSet, selectionIndex++);
                     result.withAggregateValue(aggregate, value);
                  }
               }

               /////////////////////////////////////////////////////////////////
               // in case there were no results, set the firstResultTime here //
               /////////////////////////////////////////////////////////////////
               setQueryStatFirstResultTime();

            }), params);
         }
         finally
         {
            logSQL(sql, params, mark);

            if(needToCloseConnection)
            {
               connection.close();
            }
         }

         return rs;
      }
      catch(Exception e)
      {
         if(actionTimeoutHelper != null && actionTimeoutHelper.getDidTimeout())
         {
            setQueryStatFirstResultTime();
            throw (new QUserFacingException("Aggregate query timed out."));
         }

         if(isCancelled)
         {
            throw (new QUserFacingException("Aggregate query was cancelled."));
         }

         LOG.warn("Error executing aggregate", e);
         throw new QException("Error executing aggregate", e);
      }
      finally
      {
         if(actionTimeoutHelper != null)
         {
            /////////////////////////////////////////
            // make sure the timeout got cancelled //
            /////////////////////////////////////////
            actionTimeoutHelper.cancel();
         }
      }
   }



   /***************************************************************************
    * for ORDER BY entries that use a different expression than the GROUP BY
    * (e.g., a field function with a different wrapColumnNameForOrderBy), add
    * the ORDER BY expression as an extra SELECT clause and track its position
    * for GROUP BY and ORDER BY by position.
    *
    * <p>This is added to support MySQL's ONLY_FULL_GROUP_BY mode, but should be
    * compatible with all</p>
    ***************************************************************************/
   private void populateStructuresForComplexGroupBys(QQueryFilter filter, JoinsContext joinsContext, List<String> selectClauses, List<Serializable> selectParams, List<String> extraGroupByPositions, Map<GroupBy, Integer> orderByPositionMap)
   {
      if(filter != null && CollectionUtils.nullSafeHasContents(filter.getOrderBys()))
      {
         for(QFilterOrderBy orderBy : filter.getOrderBys())
         {
            if(orderBy instanceof QFilterOrderByGroupBy orderByGroupBy)
            {
               GroupBy                               groupBy                  = orderByGroupBy.getGroupBy();
               JoinsContext.FieldAndTableNameOrAlias fieldAndTableNameOrAlias = joinsContext.getFieldAndTableNameOrAlias(groupBy.getFieldName(), true);
               QFieldMetaData                        field                    = fieldAndTableNameOrAlias.field();

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

                  String selectExpr  = fieldFunctionAdapter.wrapColumnName(columnName, fieldFunction, fieldNameToColumnReference);
                  String orderByExpr = fieldFunctionAdapter.wrapColumnNameForOrderBy(columnName, fieldFunction, fieldNameToColumnReference);

                  if(!selectExpr.equals(orderByExpr))
                  {
                     selectClauses.add(orderByExpr);
                     CollectionUtils.addAllIfNotNull(selectParams, fieldFunctionAdapter.getParams(fieldFunction));
                     int position = selectClauses.size(); // 1-based position
                     extraGroupByPositions.add(String.valueOf(position));
                     orderByPositionMap.put(groupBy, position);
                  }
               }
            }
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> buildSelectClauses(AggregateInput aggregateInput, JoinsContext joinsContext, List<Serializable> params)
   {
      List<String> rs = new ArrayList<>();

      for(GroupBy groupBy : CollectionUtils.nonNullList(aggregateInput.getGroupBys()))
      {
         rs.add(getSingleGroupByClause(groupBy, joinsContext, params, false));
      }

      for(Aggregate aggregate : aggregateInput.getAggregates())
      {
         JoinsContext.FieldAndTableNameOrAlias fieldAndTableNameOrAlias = joinsContext.getFieldAndTableNameOrAlias(aggregate.getFieldName(), true /* allowVirtualFields */);
         QFieldMetaData                        field                    = fieldAndTableNameOrAlias.field();

         String columnExpression;
         if(field instanceof QVirtualFieldMetaData virtualField)
         {
            String         fieldTableName = joinsContext.resolveTableNameOrAliasToTableName(fieldAndTableNameOrAlias.tableNameOrAlias());
            String         realFieldName  = virtualField.getFieldFunction().getFieldName();
            QFieldMetaData realField      = joinsContext.getTable(fieldTableName).getField(realFieldName);
            String         columnName     = escapeIdentifier(fieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(getColumnName(realField));

            QTableMetaData                     fieldTable           = joinsContext.getTable(fieldTableName);
            FieldFunction                      fieldFunction        = virtualField.getFieldFunction();
            RDBMSFieldFunctionAdapterInterface fieldFunctionAdapter = backendMetaData.getFieldFunctionAdapter(fieldFunction.getFunctionTypeIdentifier());
            requireFieldFunctionAdapterNotNull(fieldFunctionAdapter, fieldFunction);

            columnExpression = fieldFunctionAdapter.wrapColumnName(columnName, fieldFunction, makeFieldNameToColumnReferenceFunction(fieldAndTableNameOrAlias.tableNameOrAlias(), fieldTable));
            CollectionUtils.addAllIfNotNull(params, fieldFunctionAdapter.getParams(fieldFunction));
         }
         else
         {
            columnExpression = escapeIdentifier(fieldAndTableNameOrAlias.tableNameOrAlias()) + "." + escapeIdentifier(getColumnName(field));
         }

         rs.add(aggregate.getOperator().getSqlPrefix() + columnExpression + ")");
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String makeGroupByClause(AggregateInput aggregateInput, JoinsContext joinsContext, List<Serializable> params)
   {
      List<String> columns = new ArrayList<>();
      for(GroupBy groupBy : CollectionUtils.nonNullList(aggregateInput.getGroupBys()))
      {
         columns.add(getSingleGroupByClause(groupBy, joinsContext, params, false));
      }

      return (StringUtils.join(",", columns));
   }



   /*******************************************************************************
    ** Build an ORDER BY clause for aggregate queries, using positional references
    ** for group-by entries whose ORDER BY expression differs from their SELECT
    ** expression.
    *******************************************************************************/
   private String makeAggregateOrderByClause(QTableMetaData table, List<QFilterOrderBy> orderBys, JoinsContext joinsContext, List<Serializable> params, Map<GroupBy, Integer> orderByPositionMap)
   {
      List<String> clauses = new ArrayList<>();

      for(QFilterOrderBy orderBy : orderBys)
      {
         String ascOrDesc = orderBy.getIsAscending() ? "ASC" : "DESC";

         if(orderBy instanceof QFilterOrderByGroupBy orderByGroupBy && orderByPositionMap.containsKey(orderByGroupBy.getGroupBy()))
         {
            clauses.add(orderByPositionMap.get(orderByGroupBy.getGroupBy()) + " " + ascOrDesc);
         }
         else
         {
            /////////////////////////////////////////////////////////////////////////////
            // for entries without a positional mapping, delegate to the standard logic //
            /////////////////////////////////////////////////////////////////////////////
            clauses.add(makeOrderByClause(table, List.of(orderBy), joinsContext, params));
         }
      }

      return (StringUtils.join(", ", clauses));
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
