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

package com.kingsrook.qqq.backend.core.utils.aggregates;


import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.DateTimeGroupBy;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateCapability;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.DateBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.GroupByBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.NumericBucket;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.QFilterOrderByAggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.JoinsContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Shared validation, result types and bucket semantics for aggregate execution.
 *******************************************************************************/
public final class AggregateAnalysis
{
   /*******************************************************************************
    ** Capabilities required by operators and buckets; sampling is best effort.
    *******************************************************************************/
   public static Set<AggregateCapability> requiredCapabilities(AggregateInput input)
   {
      Set<AggregateCapability> result = EnumSet.noneOf(AggregateCapability.class);
      for(Aggregate aggregate : aggregatesIncludingOrderBy(input))
      {
         AggregateCapability capability = switch(aggregate.getOperator())
         {
            case STDDEV_SAMP, STDDEV_POP -> AggregateCapability.STDDEV;
            case VAR_SAMP, VAR_POP -> AggregateCapability.VARIANCE;
            case PERCENTILE_CONT, PERCENTILE_DISC -> AggregateCapability.PERCENTILE;
            case CORR -> AggregateCapability.CORRELATION;
            case COUNT_NULL, COUNT_BLANK -> AggregateCapability.NULL_BLANK_COUNTS;
            case MIN_LENGTH, MAX_LENGTH, AVG_LENGTH -> AggregateCapability.STRING_LENGTH;
            default -> null;
         };
         if(capability != null)
         {
            result.add(capability);
         }
      }
      for(GroupBy group : CollectionUtils.nonNullList(input.getGroupBys()))
      {
         if(group.getBucket() != null)
         {
            result.add(group.getBucket() instanceof DateBucket ? AggregateCapability.DATE_BUCKET : AggregateCapability.NUMERIC_BUCKET);
         }
      }
      return (result);
   }



   /*******************************************************************************
    ** Validate extension parameters before either execution path.
    *******************************************************************************/
   public static void validate(AggregateInput input) throws QException
   {
      if(input.getStreamedRowLimit() <= 0)
      {
         throw (new QException("streamedRowLimit must be positive"));
      }
      if(input.getSamplePercent() != null && (input.getSamplePercent().signum() <= 0 || input.getSamplePercent().compareTo(BigDecimal.valueOf(100)) > 0))
      {
         throw (new QException("samplePercent must be greater than zero and at most 100"));
      }
      JoinsContext joins = null;
      for(Aggregate aggregate : aggregatesIncludingOrderBy(input))
      {
         if(aggregate.getOperator() == AggregateOperator.COUNT_BLANK || aggregate.getOperator() == AggregateOperator.MIN_LENGTH
            || aggregate.getOperator() == AggregateOperator.MAX_LENGTH || aggregate.getOperator() == AggregateOperator.AVG_LENGTH)
         {
            if(joins == null)
            {
               joins = new JoinsContext(QContext.getQInstance(), input, input.getFilter() == null ? new QQueryFilter() : input.getFilter().clone());
            }
            QFieldType sourceType = joins.getFieldAndTableNameOrAlias(aggregate.getFieldName(), true).field().getType();
            if(sourceType != QFieldType.STRING && sourceType != QFieldType.TEXT && sourceType != QFieldType.HTML)
            {
               throw (new QException(aggregate.getOperator() + " requires a STRING, TEXT or HTML field: " + aggregate.getFieldName() + " is " + sourceType));
            }
         }
         if(aggregate.getOperator() == AggregateOperator.PERCENTILE_CONT || aggregate.getOperator() == AggregateOperator.PERCENTILE_DISC)
         {
            if(aggregate.getPercentile() == null || aggregate.getPercentile().signum() < 0 || aggregate.getPercentile().compareTo(BigDecimal.ONE) > 0)
            {
               throw (new QException("percentile must be between zero and one"));
            }
         }
         if(aggregate.getOperator() == AggregateOperator.CORR && !StringUtils.hasContent(aggregate.getOtherFieldName()))
         {
            throw (new QException("CORR requires otherFieldName"));
         }
      }
      for(GroupBy group : CollectionUtils.nonNullList(input.getGroupBys()))
      {
         if(group.getBucket() != null && group.getFormatString() != null)
         {
            throw (new QException("GroupBy formatString and bucket are mutually exclusive"));
         }
         if(group.getBucket() instanceof DateBucket date && date.getUnit() == null)
         {
            throw (new QException("DateBucket requires a unit"));
         }
         if(group.getBucket() instanceof NumericBucket numeric && (numeric.getMin() == null || numeric.getMax() == null
            || numeric.getMin().compareTo(numeric.getMax()) >= 0 || numeric.getCount() <= 0 || numeric.getCount() == Integer.MAX_VALUE))
         {
            throw (new QException("NumericBucket requires min < max and a positive count below Integer.MAX_VALUE"));
         }
      }
   }



   /*******************************************************************************
    ** Result type for the new operators, preserving legacy caller overrides.
    *******************************************************************************/
   public static QFieldType resultType(Aggregate aggregate, QFieldType sourceType)
   {
      if(aggregate.getFieldType() != null)
      {
         return (aggregate.getFieldType());
      }
      return (switch(aggregate.getOperator())
      {
         case STDDEV_SAMP, STDDEV_POP, VAR_SAMP, VAR_POP, PERCENTILE_CONT, CORR, AVG_LENGTH -> QFieldType.DECIMAL;
         case COUNT_NULL, COUNT_BLANK, MIN_LENGTH, MAX_LENGTH, COUNT, COUNT_DISTINCT -> QFieldType.INTEGER;
         case AVG -> sourceType == QFieldType.INTEGER || sourceType == QFieldType.LONG ? QFieldType.DECIMAL : sourceType;
         default -> sourceType;
      });
   }



   /*******************************************************************************
    ** Result type for bucket keys.
    *******************************************************************************/
   public static QFieldType bucketType(GroupBy group)
   {
      if(group.getBucket() instanceof DateBucket date)
      {
         return (switch(date.getUnit())
         {
            case HOUR -> QFieldType.DATE_TIME;
            case DAY_OF_WEEK, HOUR_OF_DAY -> QFieldType.INTEGER;
            default -> QFieldType.DATE;
         });
      }
      return (group.getBucket() == null ? group.getType() : QFieldType.INTEGER);
   }



   /*******************************************************************************
    ** Bucket a value in the session zone. The final numeric interval includes max.
    *******************************************************************************/
   public static Serializable bucket(Serializable value, GroupByBucket bucket, ZoneId zone)
   {
      if(value == null)
      {
         return (null);
      }
      if(bucket instanceof NumericBucket numeric)
      {
         BigDecimal x = ValueUtils.getValueAsBigDecimal(value);
         if(x.compareTo(numeric.getMin()) < 0)
         {
            return (0);
         }
         if(x.compareTo(numeric.getMax()) > 0)
         {
            return (numeric.getCount() + 1);
         }
         if(x.compareTo(numeric.getMax()) == 0)
         {
            return (numeric.getCount());
         }
         return (x.subtract(numeric.getMin()).multiply(BigDecimal.valueOf(numeric.getCount()))
            .divide(numeric.getMax().subtract(numeric.getMin()), 0, RoundingMode.FLOOR).intValue() + 1);
      }
      DateBucket date = (DateBucket) bucket;
      ZonedDateTime time = value instanceof LocalDate localDate ? localDate.atStartOfDay(zone) : ValueUtils.getValueAsInstant(value).atZone(zone);
      LocalDate day = time.toLocalDate();
      return (switch(date.getUnit())
      {
         case YEAR -> day.withDayOfYear(1);
         case QUARTER -> day.withDayOfMonth(1).withMonth(((day.getMonthValue() - 1) / 3) * 3 + 1);
         case MONTH -> day.withDayOfMonth(1);
         case WEEK -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
         case DAY -> day;
         case HOUR -> time.truncatedTo(ChronoUnit.HOURS).toInstant();
         case DAY_OF_WEEK -> day.getDayOfWeek().getValue();
         case HOUR_OF_DAY -> time.getHour();
      });
   }



   /*******************************************************************************
    ** Legacy format-string support shared with memory aggregation.
    *******************************************************************************/
   public static Serializable applyFormatString(Serializable value, GroupBy groupBy) throws QException
   {
      if(value == null)
      {
         return (null);
      }

      String formatString = groupBy.getFormatString();

      try
      {
         if(formatString.startsWith("DATE_FORMAT"))
         {
            /////////////////////////////////////////////////////////////////////////////
            // one known-use case we have here looks like this:                        //
            // DATE_FORMAT(CONVERT_TZ(%s, 'UTC', 'UTC'), '%%Y-%%m-%%dT%%H')            //
            // ... for now, let's just try to support the formatting bit at the end... //
            // todo - support the CONVERT_TZ bit too!                                  //
            /////////////////////////////////////////////////////////////////////////////
            String            sqlDateTimeFormat = formatString.replaceFirst(".*'%%", "%%").replaceFirst("'.*", "");
            DateTimeFormatter dateTimeFormatter = DateTimeGroupBy.sqlDateFormatToSelectedDateTimeFormatter(sqlDateTimeFormat);
            if(dateTimeFormatter == null)
            {
               throw (new QException("Unsupported sql dateTime format string [" + sqlDateTimeFormat + "] for MemoryRecordStore"));
            }

            String        valueAsString  = ValueUtils.getValueAsString(value);
            Instant       valueAsInstant = ValueUtils.getValueAsInstant(valueAsString);
            ZonedDateTime zonedDateTime  = valueAsInstant.atZone(ZoneId.systemDefault());
            return (dateTimeFormatter.format(zonedDateTime));
         }
         else
         {
            throw (new QException("Unsupported group-by format string [" + formatString + "] for MemoryRecordStore"));
         }
      }
      catch(QException qe)
      {
         throw (qe);
      }
      catch(Exception e)
      {
         throw (new QException("Error applying format string [" + formatString + "] to group by value [" + value + "]", e));
      }
   }




   /*******************************************************************************
    ** Order-only aggregates need capabilities, validation and accumulator state too.
    *******************************************************************************/
   public static List<Aggregate> aggregatesIncludingOrderBy(AggregateInput input)
   {
      List<Aggregate> aggregates = new ArrayList<>(CollectionUtils.nonNullList(input.getAggregates()));
      addOrderByAggregates(input.getFilter(), aggregates);
      return (aggregates);
   }



   /*******************************************************************************
    ** Collect aggregate sort descriptors without modifying the caller's selections.
    *******************************************************************************/
   private static void addOrderByAggregates(QQueryFilter filter, List<Aggregate> aggregates)
   {
      if(filter == null)
      {
         return;
      }
      for(QFilterOrderBy order : CollectionUtils.nonNullList(filter.getOrderBys()))
      {
         if(order instanceof QFilterOrderByAggregate sort && sort.getAggregate() != null && !aggregates.contains(sort.getAggregate()))
         {
            aggregates.add(sort.getAggregate());
         }
      }
      for(QQueryFilter subFilter : CollectionUtils.nonNullList(filter.getSubFilters()))
      {
         addOrderByAggregates(subFilter, aggregates);
      }
   }
}
