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


/*******************************************************************************
 ** Classes that support doing data aggregations (e.g., count, sum, min, max, average).
 ** Sub-classes should supply the type parameter.
 **
 ** The AVG_T parameter describes the type used for the average getAverage method
 ** which, e.g, for date types, might be a date, vs. numbers, they'd probably be
 ** BigDecimal.
 *******************************************************************************/
public interface AggregatesInterface<T extends Serializable, AVG_T extends Serializable>
{
   /*******************************************************************************
    **
    *******************************************************************************/
   void add(T t);

   /*******************************************************************************
    **
    *******************************************************************************/
   int getCount();

   /*******************************************************************************
    **
    *******************************************************************************/
   T getSum();

   /*******************************************************************************
    **
    *******************************************************************************/
   T getMin();

   /*******************************************************************************
    **
    *******************************************************************************/
   T getMax();

   /*******************************************************************************
    **
    *******************************************************************************/
   AVG_T getAverage();


   /*******************************************************************************
    **
    *******************************************************************************/
   default BigDecimal getProduct()
   {
      return (null);
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   default BigDecimal getVariance()
   {
      return (null);
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   default BigDecimal getVarP()
   {
      return (null);
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   default BigDecimal getStandardDeviation()
   {
      return (null);
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   default BigDecimal getStdDevP()
   {
      return (null);
   }

}
