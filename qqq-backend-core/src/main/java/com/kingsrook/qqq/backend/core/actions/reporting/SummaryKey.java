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

package com.kingsrook.qqq.backend.core.actions.reporting;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qqq.backend.core.utils.Pair;


/*******************************************************************************
 ** For a summary report, a list of field/value pairs that make up a "key".
 **
 ** For example, in a report doing summaries by State > City > ZipCode, a SummaryKey
 ** would look like:  [(state:MO),(city:St.Louis),(zipCode:63101)].
 *******************************************************************************/
public class SummaryKey implements Cloneable
{
   private List<Pair<String, Serializable>> keys = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   public SummaryKey()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return "PivotKey{keys=" + keys + '}';
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void add(String field, Serializable value)
   {
      keys.add(new Pair<>(field, value));
   }



   /*******************************************************************************
    ** Getter for keys
    **
    *******************************************************************************/
   public List<Pair<String, Serializable>> getKeys()
   {
      return keys;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean equals(Object o)
   {
      if(this == o)
      {
         return true;
      }
      if(o == null || getClass() != o.getClass())
      {
         return false;
      }
      SummaryKey summaryKey = (SummaryKey) o;
      return Objects.equals(keys, summaryKey.keys);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(keys);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public SummaryKey clone()
   {
      SummaryKey clone = new SummaryKey();

      for(Pair<String, Serializable> key : keys)
      {
         clone.add(key.getA(), key.getB());
      }

      return (clone);
   }
}
