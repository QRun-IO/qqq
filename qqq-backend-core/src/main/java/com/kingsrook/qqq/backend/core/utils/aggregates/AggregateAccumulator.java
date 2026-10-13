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
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Incremental aggregate state shared by memory and streamed execution.
 *******************************************************************************/
public class AggregateAccumulator
{
   private static final MathContext PRECISION = MathContext.DECIMAL128;
   private final Aggregate aggregate;
   private final QFieldType resultType;
   private final List<Serializable> values;
   private final Set<Serializable> distinct;
   private long count;
   private long nullCount;
   private long blankCount;
   private BigDecimal sum = BigDecimal.ZERO;
   private BigDecimal mean = BigDecimal.ZERO;
   private BigDecimal meanY = BigDecimal.ZERO;
   private BigDecimal m2 = BigDecimal.ZERO;
   private BigDecimal m2Y = BigDecimal.ZERO;
   private BigDecimal covariance = BigDecimal.ZERO;
   private Serializable min;
   private Serializable max;



   /*******************************************************************************
    ** Only percentiles and distinct counts retain input values.
    *******************************************************************************/
   public AggregateAccumulator(Aggregate aggregate, QFieldType sourceType)
   {
      this.aggregate = aggregate;
      resultType = AggregateAnalysis.resultType(aggregate, sourceType);
      values = aggregate.getOperator() == AggregateOperator.PERCENTILE_CONT || aggregate.getOperator() == AggregateOperator.PERCENTILE_DISC ? new ArrayList<>() : null;
      distinct = aggregate.getOperator() == AggregateOperator.COUNT_DISTINCT ? new HashSet<>() : null;
   }



   /*******************************************************************************
    ** Consume one row. Welford updates avoid retaining rows for variance and correlation.
    *******************************************************************************/
   public void accept(QRecord record)
   {
      Serializable value = record.getValue(aggregate.getFieldName());
      AggregateOperator operator = aggregate.getOperator();
      if(value == null)
      {
         nullCount++;
         blankCount++;
         return;
      }
      if(value instanceof String string && string.replace(" ", "").isEmpty())
      {
         blankCount++;
      }
      Serializable other = aggregate.getOtherFieldName() == null ? null : record.getValue(aggregate.getOtherFieldName());
      if(operator == AggregateOperator.CORR && other == null)
      {
         return;
      }
      count++;
      if(values != null)
      {
         values.add(value);
      }
      if(distinct != null)
      {
         distinct.add(value instanceof BigDecimal decimal ? decimal.stripTrailingZeros() : value);
      }
      if(operator == AggregateOperator.MIN_LENGTH || operator == AggregateOperator.MAX_LENGTH || operator == AggregateOperator.AVG_LENGTH)
      {
         String string = value.toString();
         value = string.codePointCount(0, string.length());
      }
      if((operator == AggregateOperator.MIN || operator == AggregateOperator.MIN_LENGTH) && (min == null || compare(value, min) < 0))
      {
         min = value;
      }
      if((operator == AggregateOperator.MAX || operator == AggregateOperator.MAX_LENGTH) && (max == null || compare(value, max) > 0))
      {
         max = value;
      }
      switch(operator)
      {
         case SUM, AVG, AVG_LENGTH, STDDEV_SAMP, STDDEV_POP, VAR_SAMP, VAR_POP, CORR ->
         {
            BigDecimal x = ValueUtils.getValueAsBigDecimal(value);
            sum = sum.add(x);
            BigDecimal delta = x.subtract(mean);
            mean = mean.add(delta.divide(BigDecimal.valueOf(count), PRECISION));
            m2 = m2.add(delta.multiply(x.subtract(mean)));
            if(operator == AggregateOperator.CORR)
            {
               BigDecimal y = ValueUtils.getValueAsBigDecimal(other);
               BigDecimal deltaY = y.subtract(meanY);
               meanY = meanY.add(deltaY.divide(BigDecimal.valueOf(count), PRECISION));
               m2Y = m2Y.add(deltaY.multiply(y.subtract(meanY)));
               covariance = covariance.add(delta.multiply(y.subtract(meanY)));
            }
         }
         default ->
         {
            // No numeric accumulator is needed.
         }
      }
   }



   /*******************************************************************************
    ** PostgreSQL percentile definitions over sorted non-null values.
    *******************************************************************************/
   private Serializable percentile()
   {
      if(values.isEmpty())
      {
         return (null);
      }
      values.sort(AggregateAccumulator::compare);
      BigDecimal p = aggregate.getPercentile();
      if(aggregate.getOperator() == AggregateOperator.PERCENTILE_DISC)
      {
         int index = Math.max(0, p.multiply(BigDecimal.valueOf(values.size())).setScale(0, RoundingMode.CEILING).intValue() - 1);
         return (values.get(index));
      }
      BigDecimal position = p.multiply(BigDecimal.valueOf(values.size() - 1));
      int lower = position.intValue();
      BigDecimal fraction = position.subtract(BigDecimal.valueOf(lower));
      BigDecimal lo = ValueUtils.getValueAsBigDecimal(values.get(lower));
      BigDecimal hi = ValueUtils.getValueAsBigDecimal(values.get(Math.min(lower + 1, values.size() - 1)));
      return (lo.add(hi.subtract(lo).multiply(fraction)));
   }



   /*******************************************************************************
    ** Finish one group.
    *******************************************************************************/
   public Serializable finish()
   {
      Serializable result = switch(aggregate.getOperator())
      {
         case COUNT -> count;
         case COUNT_DISTINCT -> distinct.size();
         case COUNT_NULL -> nullCount;
         case COUNT_BLANK -> blankCount;
         case MIN, MIN_LENGTH -> min;
         case MAX, MAX_LENGTH -> max;
         case SUM -> sum;
         case AVG, AVG_LENGTH -> count == 0 ? null : sum.divide(BigDecimal.valueOf(count), PRECISION);
         case VAR_SAMP -> count < 2 ? null : m2.divide(BigDecimal.valueOf(count - 1), PRECISION);
         case VAR_POP -> count == 0 ? null : m2.divide(BigDecimal.valueOf(count), PRECISION);
         case STDDEV_SAMP -> count < 2 ? null : m2.max(BigDecimal.ZERO).divide(BigDecimal.valueOf(count - 1), PRECISION).sqrt(PRECISION);
         case STDDEV_POP -> count == 0 ? null : m2.max(BigDecimal.ZERO).divide(BigDecimal.valueOf(count), PRECISION).sqrt(PRECISION);
         case CORR -> count < 2 || m2.signum() <= 0 || m2Y.signum() <= 0 ? null : covariance.divide(m2.multiply(m2Y).sqrt(PRECISION), PRECISION);
         case PERCENTILE_CONT, PERCENTILE_DISC -> percentile();
      };
      return (ValueUtils.getValueAsFieldType(resultType, result));
   }



   /*******************************************************************************
    ** Natural comparison with numeric normalization and nulls last.
    *******************************************************************************/
   @SuppressWarnings({ "rawtypes", "unchecked" })
   public static int compare(Serializable left, Serializable right)
   {
      if(left == null || right == null)
      {
         return (left == right ? 0 : left == null ? 1 : -1);
      }
      if(left instanceof Number && right instanceof Number)
      {
         return (ValueUtils.getValueAsBigDecimal(left).compareTo(ValueUtils.getValueAsBigDecimal(right)));
      }
      return (((Comparable) left).compareTo(right));
   }
}

