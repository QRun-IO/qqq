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


/*******************************************************************************
 ** String version of data aggregator
 *******************************************************************************/
public class StringAggregates implements AggregatesInterface<String, String>
{
   private int count = 0;

   private String min;
   private String max;



   /*******************************************************************************
    ** Add a new value to this aggregate set
    *******************************************************************************/
   public void add(String input)
   {
      if(input == null)
      {
         return;
      }

      count++;

      if(min == null || input.compareTo(min) < 0)
      {
         min = input;
      }

      if(max == null || input.compareTo(max) > 0)
      {
         max = input;
      }
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
   public String getSum()
   {
      //////////////////////////////////////
      // sum of string doesn't make sense //
      //////////////////////////////////////
      return (null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getMin()
   {
      return (min);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getMax()
   {
      return (max);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getAverage()
   {
      ///////////////////////////////////////
      // average string doesn't make sense //
      ///////////////////////////////////////
      return (null);
   }

}
