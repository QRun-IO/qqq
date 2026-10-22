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

package com.kingsrook.qqq.backend.core.model.actions.tables.aggregate;


import java.math.BigDecimal;
import java.util.Objects;


/*******************************************************************************
 ** Aggregate analysis metadata.
 *******************************************************************************/
public final class NumericBucket implements GroupByBucket
{
   private BigDecimal min;
   private BigDecimal max;
   private int count;



   /*******************************************************************************
    ** Constructor.
    *******************************************************************************/
   public NumericBucket(BigDecimal min, BigDecimal max, int count)
   {
      this.min = min;
      this.max = max;
      this.count = count;
   }



   /*******************************************************************************
    ** Getter for min
    *******************************************************************************/
   public BigDecimal getMin()
   {
      return (this.min);
   }



   /*******************************************************************************
    ** Setter for min
    *******************************************************************************/
   public void setMin(BigDecimal min)
   {
      this.min = min;
   }



   /*******************************************************************************
    ** Fluent setter for min
    *******************************************************************************/
   public NumericBucket withMin(BigDecimal min)
   {
      setMin(min);
      return (this);
   }



   /*******************************************************************************
    ** Getter for max
    *******************************************************************************/
   public BigDecimal getMax()
   {
      return (this.max);
   }



   /*******************************************************************************
    ** Setter for max
    *******************************************************************************/
   public void setMax(BigDecimal max)
   {
      this.max = max;
   }



   /*******************************************************************************
    ** Fluent setter for max
    *******************************************************************************/
   public NumericBucket withMax(BigDecimal max)
   {
      setMax(max);
      return (this);
   }



   /*******************************************************************************
    ** Getter for count
    *******************************************************************************/
   public int getCount()
   {
      return (this.count);
   }



   /*******************************************************************************
    ** Setter for count
    *******************************************************************************/
   public void setCount(int count)
   {
      this.count = count;
   }



   /*******************************************************************************
    ** Fluent setter for count
    *******************************************************************************/
   public NumericBucket withCount(int count)
   {
      setCount(count);
      return (this);
   }



   /*******************************************************************************
    ** Value equality for result keys.
    *******************************************************************************/
   @Override
   public boolean equals(Object other)
   {
      return (other instanceof NumericBucket that && Objects.equals(min, that.min) && Objects.equals(max, that.max) && Objects.equals(count, that.count));
   }



   /*******************************************************************************
    ** Hash for result keys.
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return (Objects.hash(min, max, count));
   }
}
