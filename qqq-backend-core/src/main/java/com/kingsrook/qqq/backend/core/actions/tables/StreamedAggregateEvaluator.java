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

package com.kingsrook.qqq.backend.core.actions.tables;


import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.JoinsContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.backend.core.utils.aggregates.AggregateAccumulator;
import com.kingsrook.qqq.backend.core.utils.aggregates.AggregateAnalysis;


/*******************************************************************************
 ** Bounded, permission-aware reference evaluator for aggregate requests.
 *******************************************************************************/
public class StreamedAggregateEvaluator
{
   private static final int PAGE_SIZE = 1000;



   /*******************************************************************************
    ** Execute the fallback with the configured row budget.
    *******************************************************************************/
   public AggregateOutput execute(AggregateInput input) throws QException
   {
      return (execute(input, input.getStreamedRowLimit(), false));
   }



   /*******************************************************************************
    ** Paged reads retain only group accumulators, plus percentile/distinct values up to the row budget.
    *******************************************************************************/
   public AggregateOutput execute(AggregateInput input, int rowLimit, boolean samplingSupported) throws QException
   {
      AggregateAnalysis.validate(input);
      QQueryFilter filter = input.getFilter() == null ? new QQueryFilter() : input.getFilter().clone();
      JoinsContext joins = new JoinsContext(QContext.getQInstance(), input, filter);
      QueryInput query = new QueryInput();
      query.setCommonParamsFrom(input);
      query.setInputSource(input.getInputSource());
      query.setTableMetaData(input.getTable());
      query.setShouldFetchHeavyFields(true);
      query.setShouldOmitHiddenFields(false);
      query.setShouldMaskPasswords(false);
      if(input.getPermissionEnforcement() == PermissionEnforcement.SKIP)
      {
         query.withPermissionBypass(input.getPermissionBypassReason());
      }
      else
      {
         query.setPermissionEnforcement(input.getPermissionEnforcement());
      }
      query.setQueryJoins(joins.getQueryJoins().stream().map(join -> join.clone().withSelect(true)).toList());
      Set<String> fields = new HashSet<>();
      List<Aggregate> aggregates = AggregateAnalysis.aggregatesIncludingOrderBy(input);
      List<GroupBy> groups = CollectionUtils.nonNullList(input.getGroupBys());
      for(Aggregate aggregate : aggregates)
      {
         fields.add(aggregate.getFieldName());
         CollectionUtils.addIfNotNull(fields, aggregate.getOtherFieldName());
      }
      groups.forEach(group -> fields.add(group.getFieldName()));
      String primaryKey = input.getTable().getPrimaryKeyField();
      fields.add(primaryKey);
      List<QFilterOrderBy> order = new ArrayList<>();
      order.add(new QFilterOrderBy(primaryKey));
      for(QueryJoin join : query.getQueryJoins())
      {
         String key = join.getJoinTableOrItsAlias() + "." + joins.getTable(join.getJoinTable()).getPrimaryKeyField();
         fields.add(key);
         order.add(new QFilterOrderBy(key));
      }
      query.setFieldNamesToInclude(fields);
      filter.setOrderBys(order);
      query.setFilter(filter);
      Map<List<Serializable>, List<AggregateAccumulator>> bins = new LinkedHashMap<>();
      if(groups.isEmpty())
      {
         bins.put(List.of(), accumulators(aggregates, joins));
      }
      boolean sampled = samplingSupported && input.getSamplePercent() != null;
      long step = sampled ? Math.max(1, BigDecimal.valueOf(100).divide(input.getSamplePercent(), 0, RoundingMode.HALF_UP).min(BigDecimal.valueOf(Long.MAX_VALUE)).longValue()) : 1;
      long scanned = 0;
      long mainRow = -1;
      Serializable previousKey = null;
      ZoneId zone = ValueUtils.getSessionOrInstanceZoneId();
      while(scanned < rowLimit)
      {
         int size = (int) Math.min(PAGE_SIZE, rowLimit - scanned);
         filter.setSkip((int) scanned);
         filter.setLimit(size);
         List<QRecord> records = new QueryAction().execute(query).getRecords();
         for(QRecord record : records)
         {
            scanned++;
            Serializable key = record.getValue(primaryKey);
            if(mainRow < 0 || !Objects.equals(previousKey, key))
            {
               mainRow++;
               previousKey = key;
            }
            if(mainRow % step != 0)
            {
               continue;
            }
            List<Serializable> keys = new ArrayList<>();
            for(GroupBy group : groups)
            {
               Serializable value = record.getValue(group.getFieldName());
               if(group.getBucket() != null)
               {
                  value = AggregateAnalysis.bucket(value, group.getBucket(), zone);
               }
               else if(StringUtils.hasContent(group.getFormatString()))
               {
                  value = AggregateAnalysis.applyFormatString(value, group);
               }
               else if(group.getType() != null)
               {
                  value = ValueUtils.getValueAsFieldType(group.getType(), value);
               }
               keys.add(value);
            }
            List<AggregateAccumulator> states = bins.computeIfAbsent(keys, ignored -> accumulators(aggregates, joins));
            states.forEach(state -> state.accept(record));
         }
         if(records.size() < size)
         {
            break;
         }
      }
      List<AggregateResult> results = new ArrayList<>();
      bins.forEach((keys, states) ->
      {
         AggregateResult result = new AggregateResult();
         for(int i = 0; i < groups.size(); i++)
         {
            result.withGroupByValue(groups.get(i), keys.get(i));
         }
         for(int i = 0; i < aggregates.size(); i++)
         {
            result.withAggregateValue(aggregates.get(i), states.get(i).finish());
         }
         results.add(result);
      });
      for(QFilterOrderBy sort : CollectionUtils.nonNullList(input.getFilter() == null ? null : input.getFilter().getOrderBys()).reversed())
      {
         results.sort((left, right) ->
         {
            Serializable l = sort instanceof QFilterOrderByAggregate aggregate ? left.getAggregateValue(aggregate.getAggregate())
               : left.getGroupByValue(((QFilterOrderByGroupBy) sort).getGroupBy());
            Serializable r = sort instanceof QFilterOrderByAggregate aggregate ? right.getAggregateValue(aggregate.getAggregate())
               : right.getGroupByValue(((QFilterOrderByGroupBy) sort).getGroupBy());
            return (AggregateAccumulator.compare(l, r) * (sort.getIsAscending() ? 1 : -1));
         });
      }
      if(input.getLimit() != null && input.getLimit() < results.size())
      {
         results.subList(input.getLimit(), results.size()).clear();
      }
      for(AggregateResult result : results)
      {
         result.getAggregateValues().keySet().retainAll(CollectionUtils.nonNullList(input.getAggregates()));
      }
      AggregateOutput output = new AggregateOutput().withRowsScanned(scanned).withTruncated(scanned == rowLimit)
         .withSampled(sampled).withSamplePercent(sampled ? input.getSamplePercent() : null);
      output.setResults(results);
      return (output);
   }



   /*******************************************************************************
    ** Allocate one state per requested aggregate per group.
    *******************************************************************************/
   private static List<AggregateAccumulator> accumulators(List<Aggregate> aggregates, JoinsContext joins)
   {
      return (aggregates.stream().map(aggregate -> new AggregateAccumulator(aggregate,
         joins.getFieldAndTableNameOrAlias(aggregate.getFieldName(), true).field().getType())).toList());
   }
}

