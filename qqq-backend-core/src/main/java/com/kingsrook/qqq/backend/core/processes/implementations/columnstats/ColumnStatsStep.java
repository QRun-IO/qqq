/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.columnstats;


import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.DateTimeGroupBy;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerAction;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionsHelper;
import com.kingsrook.qqq.backend.core.actions.permissions.TablePermissionSubType;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.values.QPossibleValueTranslator;
import com.kingsrook.qqq.backend.core.actions.values.QValueFormatter;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryHint;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByGroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DisplayFormat;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFunctionInputMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.ExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 * This is a single-step process used to provide Column Statistics.  These include
 * counts per-value for a field, plus things like total count, min, max, avg, based
 * on the field type.
 *
 * See {@link ColumnStatsTableConfig} for some configuration options for the process.
 *******************************************************************************/
public class ColumnStatsStep implements BackendStep
{
   private static final QLogger LOG = QLogger.getLogger(ColumnStatsStep.class);

   private static ColumnStatsTableConfig defaultColumnStatsTableConfig = new ColumnStatsTableConfig();



   /*******************************************************************************
    **
    *******************************************************************************/
   public static QProcessMetaData getProcessMetaData()
   {
      return (new QProcessMetaData()
         .withName("columnStats")
         .withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.NOT_PROTECTED))
         .withStepList(List.of(new QBackendStepMetaData()
            .withName("step")
            .withCode(new QCodeReference(ColumnStatsStep.class))
            .withInputData(new QFunctionInputMetaData()
               .withField(new QFieldMetaData("ColumnStatsTableConfig", QFieldType.STRING)))
         )));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void run(RunBackendStepInput runBackendStepInput, RunBackendStepOutput runBackendStepOutput) throws QException
   {
      try
      {
         String tableName  = runBackendStepInput.getValueString("tableName");
         String fieldName  = runBackendStepInput.getValueString("fieldName");
         String orderBy    = runBackendStepInput.getValueString("orderBy");
         String filterJSON = runBackendStepInput.getValueString("filterJSON");

         /////////////////////////////////////////
         // make sure user may query this table //
         /////////////////////////////////////////
         PermissionsHelper.checkTablePermissionThrowing(new QueryInput().withTableName(tableName), TablePermissionSubType.READ);

         QQueryFilter filter = null;
         if(StringUtils.hasContent(filterJSON))
         {
            filter = JsonUtils.toObject(filterJSON, QQueryFilter.class);

            ///////////////////////////////////////////////////////////////
            // ... remove any order-by that may have been in that filter //
            ///////////////////////////////////////////////////////////////
            filter.setOrderBys(new ArrayList<>());
         }
         else
         {
            filter = new QQueryFilter();
         }

         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
         // apply user personalization to the table - e.g., so user can't request a column they aren't allowed to see //
         ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
         QTableMetaData table = TableMetaDataPersonalizerAction.execute(new TableMetaDataPersonalizerInput().withTableName(tableName).withInputSource(QInputSource.USER));

         FieldAndQueryJoin fieldAndQueryJoin = getFieldAndQueryJoin(table, fieldName);
         QFieldMetaData    field             = fieldAndQueryJoin.field();
         QueryJoin         queryJoin         = fieldAndQueryJoin.queryJoin();

         if(field == null)
         {
            throw (new QException("Could not find field by name: " + fieldName));
         }

         if(field.getType().equals(QFieldType.BLOB))
         {
            throw (new QException("Column stats are not supported for this field's data type."));
         }

         ///////////////////////////////////////////////////////////////////////////////////////////
         // get the config for this table, if there is one -                                      //
         // then (if the config says so) check if the filter's count is over the configured limit //
         ///////////////////////////////////////////////////////////////////////////////////////////
         ColumnStatsTableConfig columnStatsTableConfig;
         if(runBackendStepInput.getValue("ColumnStatsTableConfig") != null)
         {
            columnStatsTableConfig = (ColumnStatsTableConfig) runBackendStepInput.getValue("ColumnStatsTableConfig");
         }
         else
         {
            columnStatsTableConfig = Objects.requireNonNullElse(ColumnStatsTableConfig.of(table), defaultColumnStatsTableConfig);
         }
         checkIfCountIsOverLimit(columnStatsTableConfig, table, filter);

         ////////////////////////////////////////////
         // do a count query grouped by this field //
         ////////////////////////////////////////////
         Aggregate aggregate = new Aggregate(table.getPrimaryKeyField(), AggregateOperator.COUNT).withFieldType(QFieldType.DECIMAL);
         GroupBy   groupBy   = new GroupBy(field.getType(), fieldName);

         // todo - something here about an input param to specify how you want dates & date-times grouped
         if(field.getType().equals(QFieldType.DATE_TIME))
         {
            String sqlExpression = DateTimeGroupBy.HOUR.getSqlExpression(ZoneId.systemDefault());
            groupBy = new GroupBy(QFieldType.STRING, fieldName, sqlExpression);
         }

         if(StringUtils.hasContent(orderBy))
         {
            if(orderBy.equalsIgnoreCase("count.asc"))
            {
               filter.withOrderBy(new QFilterOrderByAggregate(aggregate, true));
            }
            else if(orderBy.equalsIgnoreCase("count.desc"))
            {
               filter.withOrderBy(new QFilterOrderByAggregate(aggregate, false));
            }
            else if(orderBy.equalsIgnoreCase(fieldName + ".asc"))
            {
               filter.withOrderBy(new QFilterOrderByGroupBy(groupBy, true));
            }
            else if(orderBy.equalsIgnoreCase(fieldName + ".desc"))
            {
               filter.withOrderBy(new QFilterOrderByGroupBy(groupBy, false));
            }
            else
            {
               LOG.info("Unrecognized orderBy: " + orderBy);
            }
         }

         ///////////////////////////////////////////////////////////////////////////////////////////////////
         // always add order by to break ties.  these will be the default too, if input didn't supply one //
         ///////////////////////////////////////////////////////////////////////////////////////////////////
         filter.withOrderBy(new QFilterOrderByAggregate(aggregate, false));
         filter.withOrderBy(new QFilterOrderByGroupBy(groupBy));

         Integer        limit          = 1000;
         AggregateInput aggregateInput = new AggregateInput();
         aggregateInput.withAggregate(aggregate);
         aggregateInput.withGroupBy(groupBy);
         aggregateInput.setTableName(tableName);
         aggregateInput.setFilter(filter);
         aggregateInput.setLimit(limit);

         if(queryJoin != null)
         {
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            // re-construct this queryJoin object - just because, the JoinsContext edits the previous one, so we can make some failing-joins otherwise... //
            ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
            fieldAndQueryJoin = getFieldAndQueryJoin(table, fieldName);
            aggregateInput.withQueryJoin(fieldAndQueryJoin.queryJoin());
         }

         aggregateInput.setTimeoutSeconds(columnStatsTableConfig.getQueryTimeoutSeconds());
         aggregateInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);

         AggregateOutput aggregateOutput = new AggregateAction().execute(aggregateInput);

         ArrayList<QRecord> valueCounts = new ArrayList<>();
         for(AggregateResult result : aggregateOutput.getResults())
         {
            Serializable value = result.getGroupByValue(groupBy);

            if(field.getType().equals(QFieldType.DATE_TIME) && value != null)
            {
               value = Instant.parse(value + ":00:00Z");
            }

            Integer count = ValueUtils.getValueAsInteger(result.getAggregateValue(aggregate));
            valueCounts.add(new QRecord().withValue(fieldName, value).withValue("count", count));
         }

         //////////////////////////////////////////////////////////////////////////////////////////////////
         // so... our json serialization causes both "" and null values to go to the frontend as null... //
         // so we get 2 rows, but they look the same to the frontend.                                    //
         // turns out, users (probably?) don't care about the difference, so let's merge "" and null!    //
         //////////////////////////////////////////////////////////////////////////////////////////////////
         Integer rowsWithAValueToDecrease = mergeEmptyStringAndNull(field, fieldName, valueCounts, orderBy);

         QFieldMetaData countField = new QFieldMetaData("count", QFieldType.INTEGER).withDisplayFormat(DisplayFormat.COMMAS).withLabel("Count");

         QPossibleValueTranslator qPossibleValueTranslator = new QPossibleValueTranslator();
         qPossibleValueTranslator.translatePossibleValuesInRecords(table, valueCounts, queryJoin == null ? null : List.of(queryJoin), null);

         /////////////////////////////////////////////////////////////////////////////////////////////////
         // todo - be aware of possible name collisions here!! (e.g., a table w/ a field named `count`) //
         /////////////////////////////////////////////////////////////////////////////////////////////////
         QValueFormatter.setDisplayValuesInRecords(table, Map.of(fieldName, field, "count", countField), valueCounts);

         runBackendStepOutput.addValue("valueCounts", valueCounts);

         /////////////////////////////////////////////////////
         // now do individual statistics as a pseudo-record //
         /////////////////////////////////////////////////////
         QFieldMetaData countNonNullField  = new QFieldMetaData("count", QFieldType.INTEGER).withLabel("Rows with a value").withDisplayFormat(DisplayFormat.COMMAS);
         QFieldMetaData countDistinctField = new QFieldMetaData("countDistinct", QFieldType.INTEGER).withLabel("Distinct values").withDisplayFormat(DisplayFormat.COMMAS);
         QFieldMetaData sumField           = new QFieldMetaData("sum", QFieldType.DECIMAL).withDisplayFormat(field.getDisplayFormat());
         QFieldMetaData avgField           = new QFieldMetaData("average", QFieldType.DECIMAL).withDisplayFormat(DisplayFormat.DECIMAL2_COMMAS);
         QFieldMetaData minField           = new QFieldMetaData("min", field.getType()).withDisplayFormat(field.getDisplayFormat());
         QFieldMetaData maxField           = new QFieldMetaData("max", field.getType()).withDisplayFormat(field.getDisplayFormat());

         boolean doCountDistinct = true;
         boolean doSum           = true;
         boolean doAvg           = true;
         boolean doMin           = true;
         boolean doMax           = true;
         if(field.getType().isStringLike())
         {
            doSum = false;
            doAvg = false;
         }
         if(field.getType().equals(QFieldType.BOOLEAN))
         {
            doSum = false;
            doAvg = false;
            doMin = false;
            doMax = false;
         }
         if(field.getType().equals(QFieldType.DATE) || field.getType().equals(QFieldType.DATE_TIME))
         {
            doSum = false;
            doAvg = false; // could this be done?
         }
         if(StringUtils.hasContent(field.getPossibleValueSourceName()))
         {
            doSum = false;
            doAvg = false;
            doMin = false;
            doMax = false;
         }

         if(field.getName().equals(table.getPrimaryKeyField()))
         {
            doSum = false;
         }

         ArrayList<QFieldMetaData> fields = new ArrayList<>();
         fields.add(countNonNullField);
         fields.add(countDistinctField);
         if(doSum)
         {
            fields.add(sumField);
         }
         if(doAvg)
         {
            fields.add(avgField);
         }
         if(doMin)
         {
            fields.add(minField);
         }
         if(doMax)
         {
            fields.add(maxField);
         }

         QRecord statsRecord = new QRecord();

         if(valueCounts.size() < limit)
         {
            statsRecord.setValue(countDistinctField.getName(), valueCounts.size());
            doCountDistinct = false;
         }

         /////////////////////////////////////////////////////////////////////////////////
         // just in case any of these don't fit in an integer, use decimal for them all //
         /////////////////////////////////////////////////////////////////////////////////
         Aggregate countTotalRowsAggregate = new Aggregate(table.getPrimaryKeyField(), AggregateOperator.COUNT).withFieldType(QFieldType.DECIMAL);
         Aggregate countNonNullAggregate   = new Aggregate(fieldName, AggregateOperator.COUNT).withFieldType(QFieldType.DECIMAL);
         Aggregate countDistinctAggregate  = new Aggregate(fieldName, AggregateOperator.COUNT_DISTINCT).withFieldType(QFieldType.DECIMAL);
         Aggregate sumAggregate            = new Aggregate(fieldName, AggregateOperator.SUM).withFieldType(QFieldType.DECIMAL);
         Aggregate avgAggregate            = new Aggregate(fieldName, AggregateOperator.AVG).withFieldType(QFieldType.DECIMAL);
         Aggregate minAggregate            = new Aggregate(fieldName, AggregateOperator.MIN);
         Aggregate maxAggregate            = new Aggregate(fieldName, AggregateOperator.MAX);

         AggregateInput statsAggregateInput = new AggregateInput();
         statsAggregateInput.withAggregate(countTotalRowsAggregate);
         statsAggregateInput.withAggregate(countNonNullAggregate);

         if(doCountDistinct)
         {
            statsAggregateInput.withAggregate(countDistinctAggregate);
         }
         if(doSum)
         {
            statsAggregateInput.withAggregate(sumAggregate);
         }
         if(doAvg)
         {
            statsAggregateInput.withAggregate(avgAggregate);
         }
         if(doMin)
         {
            statsAggregateInput.withAggregate(minAggregate);
         }
         if(doMax)
         {
            statsAggregateInput.withAggregate(maxAggregate);
         }

         BigDecimal totalRows = null;
         if(CollectionUtils.nullSafeHasContents(statsAggregateInput.getAggregates()))
         {
            statsAggregateInput.setTableName(tableName);
            filter.setOrderBys(new ArrayList<>());
            statsAggregateInput.setFilter(filter);
            if(queryJoin != null)
            {
               statsAggregateInput.withQueryJoin(queryJoin);
            }

            statsAggregateInput.setTimeoutSeconds(columnStatsTableConfig.getQueryTimeoutSeconds());
            statsAggregateInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);

            AggregateOutput statsAggregateOutput = new AggregateAction().execute(statsAggregateInput);
            if(CollectionUtils.nullSafeHasContents(statsAggregateOutput.getResults()))
            {
               AggregateResult statsAggregateResult = statsAggregateOutput.getResults().get(0);

               totalRows = ValueUtils.getValueAsBigDecimal(statsAggregateResult.getAggregateValue(countTotalRowsAggregate));

               statsRecord.setValue(countNonNullField.getName(), statsAggregateResult.getAggregateValue(countNonNullAggregate));
               if(doCountDistinct)
               {
                  statsRecord.setValue(countDistinctField.getName(), statsAggregateResult.getAggregateValue(countDistinctAggregate));
               }
               if(doSum)
               {
                  statsRecord.setValue(sumField.getName(), statsAggregateResult.getAggregateValue(sumAggregate));
               }
               if(doAvg)
               {
                  statsRecord.setValue(avgField.getName(), statsAggregateResult.getAggregateValue(avgAggregate));
               }
               if(doMin)
               {
                  statsRecord.setValue(minField.getName(), statsAggregateResult.getAggregateValue(minAggregate));
               }
               if(doMax)
               {
                  statsRecord.setValue(maxField.getName(), statsAggregateResult.getAggregateValue(maxAggregate));
               }

               if(rowsWithAValueToDecrease != null)
               {
                  ///////////////////////////////////////////////////////////////////////////////////////////////
                  // this is in case we merged any "" and null values -                                        //
                  // we need to take away however many ""'s there were from countNonNull (treat those as null) //
                  // and decrease unique values by 1                                                           //
                  ///////////////////////////////////////////////////////////////////////////////////////////////
                  try
                  {
                     statsRecord.setValue(countNonNullField.getName(), statsRecord.getValueInteger(countNonNullField.getName()) - rowsWithAValueToDecrease);
                     statsRecord.setValue(countDistinctField.getName(), statsRecord.getValueInteger(countDistinctField.getName()) - 1);
                  }
                  catch(Exception e)
                  {
                     LOG.warn("Error decreasing by non-null empty string count", e, logPair("fieldName", fieldName), logPair("tableName", tableName));
                  }
               }
            }
         }

         /////////////////////
         // figure count%'s //
         /////////////////////
         if(totalRows == null)
         {
            totalRows = new BigDecimal(valueCounts.stream().mapToInt(r -> r.getValueInteger("count")).sum());
         }

         if(totalRows != null && totalRows.compareTo(BigDecimal.ZERO) > 0)
         {
            BigDecimal oneHundred = new BigDecimal(100);
            for(QRecord valueCount : valueCounts)
            {
               BigDecimal percent = new BigDecimal(Objects.requireNonNullElse(valueCount.getValueInteger("count"), 0)).divide(totalRows, 4, RoundingMode.HALF_UP).multiply(oneHundred).setScale(2, RoundingMode.HALF_UP);
               valueCount.setValue("percent", percent);
            }

            QFieldMetaData percentField = new QFieldMetaData("percent", QFieldType.DECIMAL).withDisplayFormat(DisplayFormat.PERCENT_POINT2).withLabel("Percent");
            QValueFormatter.setDisplayValuesInRecords(table, Map.of(fieldName, field, "percent", percentField), valueCounts);
         }

         QInstanceEnricher qInstanceEnricher = new QInstanceEnricher(null);
         fields.forEach(qInstanceEnricher::enrichField);

         QValueFormatter.setDisplayValuesInRecord(table, fields.stream().collect(Collectors.toMap(f -> f.getName(), f -> f)), statsRecord);

         runBackendStepOutput.addValue("statsFields", fields);
         runBackendStepOutput.addValue("statsRecord", statsRecord);
      }
      catch(Exception e)
      {
         throw new QException("Error calculating column stats", e);
      }
   }



   /***************************************************************************
    * if a {@link ColumnStatsTableConfig#withFailIfCountOverLimit(Integer)}
    * value is set, do a count on the table with the input filter, and throw
    * an error if the coutn is over the limit.
    *
    * @param columnStatsTableConfig the config object to check for a limit.
    * if null, or limit is null, method returns with noop.
    * @param table the table being worked on - note - may
    ***************************************************************************/
   private void checkIfCountIsOverLimit(ColumnStatsTableConfig columnStatsTableConfig, QTableMetaData table, QQueryFilter filter) throws QException
   {
      if(columnStatsTableConfig == null || columnStatsTableConfig.getFailIfCountOverLimit() == null)
      {
         return;
      }

      CountInput countInput = new CountInput();
      countInput.setTableMetaData(table);
      countInput.setFilter(filter == null ? null : filter.clone()); // clone filter, in case it gets modified (e.g., for security)
      countInput.withQueryHint(QueryHint.MAY_USE_READ_ONLY_BACKEND);
      countInput.withTimeoutSeconds(columnStatsTableConfig.getQueryTimeoutSeconds());
      CountOutput countOutput = new CountAction().execute(countInput);
      Integer     count       = countOutput.getCount();
      if(count > columnStatsTableConfig.getFailIfCountOverLimit())
      {
         throw (new QUserFacingException("Unable to calculate Column Statistics.  Your filter matches too many rows (" + String.format("%,d", count) + "). The limit for this table is " + String.format("%,d", columnStatsTableConfig.getFailIfCountOverLimit()) + "."));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer mergeEmptyStringAndNull(QFieldMetaData field, String fieldName, ArrayList<QRecord> valueCounts, String orderBy)
   {
      if(field.getType().isStringLike())
      {
         Integer nullCount        = null;
         Integer emptyStringCount = null;
         for(QRecord record : valueCounts)
         {
            if("".equals(record.getValue(fieldName)))
            {
               emptyStringCount = record.getValueInteger("count");
            }
            else if(record.getValue(fieldName) == null)
            {
               nullCount = record.getValueInteger("count");
            }
         }

         if(nullCount != null && emptyStringCount != null)
         {
            Iterator<QRecord> iterator = valueCounts.iterator();
            while(iterator.hasNext())
            {
               QRecord record = iterator.next();
               if("".equals(record.getValue(fieldName)))
               {
                  iterator.remove();
               }
               else if(record.getValue(fieldName) == null)
               {
                  record.setValue("count", nullCount + emptyStringCount);
               }
            }

            ///////////////////////////////////////////////////
            // re-sort the records, as the counts may change //
            ///////////////////////////////////////////////////
            if(StringUtils.hasContent(orderBy))
            {
               if(orderBy.toLowerCase().startsWith("count."))
               {
                  valueCounts.sort(Comparator.comparing(r -> r.getValueInteger("count")));
               }
               else
               {
                  valueCounts.sort(Comparator.comparing(r -> Objects.requireNonNullElse(r.getValueString(fieldName), "")));
               }

               if(orderBy.toLowerCase().endsWith(".desc"))
               {
                  Collections.reverse(valueCounts);
               }
            }

            return (emptyStringCount);
         }
      }

      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private FieldAndQueryJoin getFieldAndQueryJoin(QTableMetaData table, String fieldName)
   {
      QFieldMetaData field     = null;
      QueryJoin      queryJoin = null;
      if(fieldName.contains("."))
      {
         String[] parts = fieldName.split("\\.", 2);
         for(ExposedJoin exposedJoin : CollectionUtils.nonNullList(table.getExposedJoins()))
         {
            if(exposedJoin.getJoinTable().equals(parts[0]))
            {
               field = QContext.getQInstance().getTable(exposedJoin.getJoinTable()).getField(parts[1]);
               queryJoin = new QueryJoin()
                  .withJoinTable(exposedJoin.getJoinTable())
                  .withSelect(true)
                  .withType(QueryJoin.Type.INNER);
               break;
            }
         }
      }
      else
      {
         field = table.getField(fieldName);
      }

      return (new FieldAndQueryJoin(field, queryJoin));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private record FieldAndQueryJoin(QFieldMetaData field, QueryJoin queryJoin) {}
}
