/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** Subclass of record pipe that ony allows through distinct records, based on
 ** the set of fields specified in the constructor as a uniqueKey.
 *******************************************************************************/
public class DistinctFilteringRecordPipe extends RecordPipe
{
   private UniqueKey         uniqueKey;
   private Set<Serializable> seenValues = new HashSet<>();



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public DistinctFilteringRecordPipe(UniqueKey uniqueKey)
   {
      this.uniqueKey = uniqueKey;
   }



   /*******************************************************************************
    ** Constructor that accepts pipe's overrideCapacity (allowed to be null)
    **
    *******************************************************************************/
   public DistinctFilteringRecordPipe(UniqueKey uniqueKey, Integer overrideCapacity)
   {
      super(overrideCapacity);
      this.uniqueKey = uniqueKey;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addRecords(List<QRecord> records) throws QException
   {
      List<QRecord> recordsToAdd = new ArrayList<>();
      for(QRecord record : records)
      {
         if(!seenBefore(record))
         {
            recordsToAdd.add(record);
         }
      }

      if(recordsToAdd.isEmpty())
      {
         return;
      }

      super.addRecords(recordsToAdd);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addRecord(QRecord record) throws QException
   {
      if(seenBefore(record))
      {
         return;
      }

      super.addRecord(record);
   }



   /*******************************************************************************
    ** return true if we've seen this record before (based on the unique key) -
    ** also - update the set of seen values!
    *******************************************************************************/
   private boolean seenBefore(QRecord record)
   {
      Serializable ukValues = extractUKValues(record);
      if(seenValues.contains(ukValues))
      {
         return true;
      }
      seenValues.add(ukValues);
      return false;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Serializable extractUKValues(QRecord record)
   {
      if(uniqueKey.getFieldNames().size() == 1)
      {
         return (record.getValue(uniqueKey.getFieldNames().get(0)));
      }
      else
      {
         ArrayList<Serializable> rs = new ArrayList<>();
         for(String fieldName : uniqueKey.getFieldNames())
         {
            rs.add(record.getValue(fieldName));
         }
         return (rs);
      }
   }
}
