/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model;


import java.io.Serializable;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;


/*******************************************************************************
 ** A row of values, e.g., from a file, for bulk-load
 *******************************************************************************/
public class BulkLoadFileRow implements Serializable
{
   private int            rowNo;
   private Serializable[] values;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BulkLoadFileRow(Serializable[] values, int rowNo)
   {
      this.values = values;
      this.rowNo = rowNo;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public int size()
   {
      if(values == null)
      {
         return (0);
      }

      return (values.length);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public boolean hasIndex(int i)
   {
      if(values == null)
      {
         return (false);
      }

      if(i >= values.length || i < 0)
      {
         return (false);
      }

      return (true);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public Serializable getValue(int i)
   {
      if(values == null)
      {
         throw new IllegalStateException("Row has no values");
      }

      if(i >= values.length || i < 0)
      {
         throw new IllegalArgumentException("Index out of bounds:  Requested index " + i + "; values.length: " + values.length);
      }

      return (values[i]);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public Serializable getValueElseNull(int i)
   {
      if(!hasIndex(i))
      {
         return (null);
      }

      return (values[i]);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String toString()
   {
      if(values == null)
      {
         return ("null");
      }

      return Arrays.stream(values).map(String::valueOf).collect(Collectors.joining(","));
   }



   /***************************************************************************
    **
    ***************************************************************************/
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

      BulkLoadFileRow that = (BulkLoadFileRow) o;
      return rowNo == that.rowNo && Objects.deepEquals(values, that.values);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public int hashCode()
   {
      return Objects.hash(rowNo, Arrays.hashCode(values));
   }



   /*******************************************************************************
    ** Getter for rowNo
    *******************************************************************************/
   public int getRowNo()
   {
      return (this.rowNo);
   }



   /*******************************************************************************
    ** Setter for rowNo
    *******************************************************************************/
   public void setRowNo(int rowNo)
   {
      this.rowNo = rowNo;
   }



   /*******************************************************************************
    ** Fluent setter for rowNo
    *******************************************************************************/
   public BulkLoadFileRow withRowNo(int rowNo)
   {
      this.rowNo = rowNo;
      return (this);
   }

}
