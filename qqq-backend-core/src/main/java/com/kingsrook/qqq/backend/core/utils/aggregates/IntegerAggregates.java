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


import java.math.BigDecimal;


/*******************************************************************************
 ** Integer version of data aggregator
 *******************************************************************************/
public class IntegerAggregates implements AggregatesInterface<Integer, BigDecimal>
{
   private int        count = 0;
   // private Integer countDistinct;
   private Integer    sum;
   private Integer    min;
   private Integer    max;
   private BigDecimal product;

   private VarianceCalculator varianceCalculator = new VarianceCalculator();



   /*******************************************************************************
    ** Add a new value to this aggregate set
    *******************************************************************************/
   public void add(Integer input)
   {
      if(input == null)
      {
         return;
      }

      BigDecimal inputBD = new BigDecimal(input);

      count++;

      if(sum == null)
      {
         sum = input;
      }
      else
      {
         sum = sum + input;
      }

      if(product == null)
      {
         product = inputBD;
      }
      else
      {
         product = product.multiply(inputBD);
      }

      if(min == null || input < min)
      {
         min = input;
      }

      if(max == null || input > max)
      {
         max = input;
      }

      varianceCalculator.updateVariance(inputBD);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public BigDecimal getVariance()
   {
      return (varianceCalculator.getVariance());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public BigDecimal getVarP()
   {
      return (varianceCalculator.getVarP());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public BigDecimal getStandardDeviation()
   {
      return (varianceCalculator.getStandardDeviation());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public BigDecimal getStdDevP()
   {
      return (varianceCalculator.getStdDevP());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int getCount()
   {
      return (count);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer getSum()
   {
      return (sum);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer getMin()
   {
      return (min);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer getMax()
   {
      return (max);
   }



   /*******************************************************************************
    ** Getter for product
    **
    *******************************************************************************/
   @Override
   public BigDecimal getProduct()
   {
      return product;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public BigDecimal getAverage()
   {
      if(this.count > 0)
      {
         return (BigDecimal.valueOf(this.sum.doubleValue() / (double) this.count));
      }
      else
      {
         return (null);
      }
   }

}
