/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.fields;


import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.values.ValueBehaviorApplier;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;


/*******************************************************************************
 ** Validate the min & max value for numeric fields.
 **
 ** For each min & max, there are 4 possible settings:
 ** - value - the number that is compared.
 ** - allowEqualTo - defaults to true.  controls if < (>) or ≤ (≥)
 ** - behavior - defaults to ERROR.  optionally can be "CLIP" instead.
 ** - clipAmount - if clipping, and not allowing equalTo, how much off the limit
 **   value should be added or subtracted.  Defaults to 1.
 **
 ** Convenient `withMin()` and `withMax()` methods exist for setting all 4
 ** properties for each of min or max.  Else, fluent-setters are recommended.
 *******************************************************************************/
public class ValueRangeBehavior implements FieldBehavior<ValueRangeBehavior>
{
   /***************************************************************************
    **
    ***************************************************************************/
   public enum Behavior
   {
      ERROR,
      CLIP
   }



   private Number     minValue;
   private boolean    minAllowEqualTo = true;
   private Behavior   minBehavior     = Behavior.ERROR;
   private BigDecimal minClipAmount   = BigDecimal.ONE;

   private Number     maxValue;
   private boolean    maxAllowEqualTo = true;
   private Behavior   maxBehavior     = Behavior.ERROR;
   private BigDecimal maxClipAmount   = BigDecimal.ONE;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ValueRangeBehavior()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public ValueRangeBehavior getDefault()
   {
      return null;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void apply(ValueBehaviorApplier.Action action, List<QRecord> recordList, QInstance instance, QTableMetaData table, QFieldMetaData field)
   {
      BigDecimal minLimitBigDecimal = minValue == null ? null : new BigDecimal(minValue.toString());
      String     minLimitString     = minValue == null ? null : minValue.toString();

      BigDecimal maxLimitBigDecimal = maxValue == null ? null : new BigDecimal(maxValue.toString());
      String     maxLimitString     = maxValue == null ? null : maxValue.toString();

      for(QRecord record : recordList)
      {
         BigDecimal recordValue = record.getValueBigDecimal(field.getName());
         if(recordValue != null)
         {
            if(minLimitBigDecimal != null)
            {
               int compare = recordValue.compareTo(minLimitBigDecimal);
               if(compare < 0 || (compare == 0 && !minAllowEqualTo))
               {
                  if(this.minBehavior == Behavior.ERROR)
                  {
                     String operator = minAllowEqualTo ? "" : "greater than ";
                     record.addError(new BadInputStatusMessage("The value for " + field.getLabel() + " is too small (minimum allowed value is " + operator + minLimitString + ")"));
                  }
                  else if(this.minBehavior == Behavior.CLIP)
                  {
                     if(minAllowEqualTo)
                     {
                        record.setValue(field.getName(), minLimitBigDecimal);
                     }
                     else
                     {
                        record.setValue(field.getName(), minLimitBigDecimal.add(minClipAmount));
                     }
                  }
               }
            }

            if(maxLimitBigDecimal != null)
            {
               int compare = recordValue.compareTo(maxLimitBigDecimal);
               if(compare > 0 || (compare == 0 && !maxAllowEqualTo))
               {
                  if(this.maxBehavior == Behavior.ERROR)
                  {
                     String operator = maxAllowEqualTo ? "" : "less than ";
                     record.addError(new BadInputStatusMessage("The value for " + field.getLabel() + " is too large (maximum allowed value is " + operator + maxLimitString + ")"));
                  }
                  else if(this.maxBehavior == Behavior.CLIP)
                  {
                     if(maxAllowEqualTo)
                     {
                        record.setValue(field.getName(), maxLimitBigDecimal);
                     }
                     else
                     {
                        record.setValue(field.getName(), maxLimitBigDecimal.subtract(maxClipAmount));
                     }
                  }
               }
            }

         }
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public boolean allowMultipleBehaviorsOfThisType()
   {
      return (false);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public List<String> validateBehaviorConfiguration(QTableMetaData tableMetaData, QFieldMetaData fieldMetaData)
   {
      List<String> errors = new ArrayList<>();

      if(minValue == null && maxValue == null)
      {
         errors.add("Either minValue or maxValue (or both) must be set.");
      }

      if(minValue != null && maxValue != null && new BigDecimal(minValue.toString()).compareTo(new BigDecimal(maxValue.toString())) > 0)
      {
         errors.add("minValue must be >= maxValue.");
      }

      if(fieldMetaData != null && fieldMetaData.getType() != null && !fieldMetaData.getType().isNumeric())
      {
         errors.add("can only be applied to a numeric type field.");
      }

      return (errors);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public ValueRangeBehavior withMin(Number value, boolean allowEqualTo, Behavior behavior, BigDecimal clipAmount)
   {
      setMinValue(value);
      setMinAllowEqualTo(allowEqualTo);
      setMinBehavior(behavior);
      setMinClipAmount(clipAmount);
      return (this);
   }


   /***************************************************************************
    **
    ***************************************************************************/
   public ValueRangeBehavior withMax(Number value, boolean allowEqualTo, Behavior behavior, BigDecimal clipAmount)
   {
      setMaxValue(value);
      setMaxAllowEqualTo(allowEqualTo);
      setMaxBehavior(behavior);
      setMaxClipAmount(clipAmount);
      return (this);
   }



   /*******************************************************************************
    ** Getter for minValue
    *******************************************************************************/
   public Number getMinValue()
   {
      return (this.minValue);
   }



   /*******************************************************************************
    ** Setter for minValue
    *******************************************************************************/
   public void setMinValue(Number minValue)
   {
      this.minValue = minValue;
   }



   /*******************************************************************************
    ** Fluent setter for minValue
    *******************************************************************************/
   public ValueRangeBehavior withMinValue(Number minValue)
   {
      this.minValue = minValue;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxValue
    *******************************************************************************/
   public Number getMaxValue()
   {
      return (this.maxValue);
   }



   /*******************************************************************************
    ** Setter for maxValue
    *******************************************************************************/
   public void setMaxValue(Number maxValue)
   {
      this.maxValue = maxValue;
   }



   /*******************************************************************************
    ** Fluent setter for maxValue
    *******************************************************************************/
   public ValueRangeBehavior withMaxValue(Number maxValue)
   {
      this.maxValue = maxValue;
      return (this);
   }



   /*******************************************************************************
    ** Getter for minAllowEqualTo
    *******************************************************************************/
   public boolean getMinAllowEqualTo()
   {
      return (this.minAllowEqualTo);
   }



   /*******************************************************************************
    ** Setter for minAllowEqualTo
    *******************************************************************************/
   public void setMinAllowEqualTo(boolean minAllowEqualTo)
   {
      this.minAllowEqualTo = minAllowEqualTo;
   }



   /*******************************************************************************
    ** Fluent setter for minAllowEqualTo
    *******************************************************************************/
   public ValueRangeBehavior withMinAllowEqualTo(boolean minAllowEqualTo)
   {
      this.minAllowEqualTo = minAllowEqualTo;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxAllowEqualTo
    *******************************************************************************/
   public boolean getMaxAllowEqualTo()
   {
      return (this.maxAllowEqualTo);
   }



   /*******************************************************************************
    ** Setter for maxAllowEqualTo
    *******************************************************************************/
   public void setMaxAllowEqualTo(boolean maxAllowEqualTo)
   {
      this.maxAllowEqualTo = maxAllowEqualTo;
   }



   /*******************************************************************************
    ** Fluent setter for maxAllowEqualTo
    *******************************************************************************/
   public ValueRangeBehavior withMaxAllowEqualTo(boolean maxAllowEqualTo)
   {
      this.maxAllowEqualTo = maxAllowEqualTo;
      return (this);
   }



   /*******************************************************************************
    ** Getter for minBehavior
    *******************************************************************************/
   public Behavior getMinBehavior()
   {
      return (this.minBehavior);
   }



   /*******************************************************************************
    ** Setter for minBehavior
    *******************************************************************************/
   public void setMinBehavior(Behavior minBehavior)
   {
      this.minBehavior = minBehavior;
   }



   /*******************************************************************************
    ** Fluent setter for minBehavior
    *******************************************************************************/
   public ValueRangeBehavior withMinBehavior(Behavior minBehavior)
   {
      this.minBehavior = minBehavior;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxBehavior
    *******************************************************************************/
   public Behavior getMaxBehavior()
   {
      return (this.maxBehavior);
   }



   /*******************************************************************************
    ** Setter for maxBehavior
    *******************************************************************************/
   public void setMaxBehavior(Behavior maxBehavior)
   {
      this.maxBehavior = maxBehavior;
   }



   /*******************************************************************************
    ** Fluent setter for maxBehavior
    *******************************************************************************/
   public ValueRangeBehavior withMaxBehavior(Behavior maxBehavior)
   {
      this.maxBehavior = maxBehavior;
      return (this);
   }



   /*******************************************************************************
    ** Getter for minClipAmount
    *******************************************************************************/
   public BigDecimal getMinClipAmount()
   {
      return (this.minClipAmount);
   }



   /*******************************************************************************
    ** Setter for minClipAmount
    *******************************************************************************/
   public void setMinClipAmount(BigDecimal minClipAmount)
   {
      this.minClipAmount = minClipAmount;
   }



   /*******************************************************************************
    ** Fluent setter for minClipAmount
    *******************************************************************************/
   public ValueRangeBehavior withMinClipAmount(BigDecimal minClipAmount)
   {
      this.minClipAmount = minClipAmount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxClipAmount
    *******************************************************************************/
   public BigDecimal getMaxClipAmount()
   {
      return (this.maxClipAmount);
   }



   /*******************************************************************************
    ** Setter for maxClipAmount
    *******************************************************************************/
   public void setMaxClipAmount(BigDecimal maxClipAmount)
   {
      this.maxClipAmount = maxClipAmount;
   }



   /*******************************************************************************
    ** Fluent setter for maxClipAmount
    *******************************************************************************/
   public ValueRangeBehavior withMaxClipAmount(BigDecimal maxClipAmount)
   {
      this.maxClipAmount = maxClipAmount;
      return (this);
   }

}
