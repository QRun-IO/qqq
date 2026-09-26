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

package com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.filehandling;


import java.util.Iterator;
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadFileRow;


/*******************************************************************************
 **
 *******************************************************************************/
public abstract class AbstractIteratorBasedFileToRows<E> implements FileToRowsInterface
{
   private Iterator<E> iterator;

   private boolean         useLast = false;
   private BulkLoadFileRow last;

   int rowNo = 0;


   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public boolean hasNext()
   {
      if(iterator == null)
      {
         throw new IllegalStateException("Object was not init'ed");
      }

      if(useLast)
      {
         return true;
      }

      return iterator.hasNext();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public BulkLoadFileRow next()
   {
      rowNo++;
      if(iterator == null)
      {
         throw new IllegalStateException("Object was not init'ed");
      }

      if(useLast)
      {
         useLast = false;
         return (this.last);
      }

      E e = iterator.next();

      BulkLoadFileRow row = makeRow(e);

      this.last = row;
      return (this.last);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public abstract BulkLoadFileRow makeRow(E e);



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void unNext()
   {
      rowNo--;
      useLast = true;
   }



   /*******************************************************************************
    ** Getter for iterator
    *******************************************************************************/
   public Iterator<E> getIterator()
   {
      return (this.iterator);
   }



   /*******************************************************************************
    ** Setter for iterator
    *******************************************************************************/
   public void setIterator(Iterator<E> iterator)
   {
      this.iterator = iterator;
   }



   /*******************************************************************************
    ** Getter for rowNo
    **
    *******************************************************************************/
   @Override
   public int getRowNo()
   {
      return rowNo;
   }
}
