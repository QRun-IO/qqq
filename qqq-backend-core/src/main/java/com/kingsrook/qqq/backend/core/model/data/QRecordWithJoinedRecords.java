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

package com.kingsrook.qqq.backend.core.model.data;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;


/*******************************************************************************
 ** Extension on QRecord, intended to be used where you've got records from
 ** multiple tables, and you want to combine them into a single "wide" joined
 ** record - but to do so without copying or modifying any of the individual
 ** records.
 **
 ** e.g., given:
 ** - Order (id, orderNo, orderDate) (main table)
 ** - LineItem (id, sku, quantity)
 ** - Extrinsic (id, key, value)
 **
 ** If set up in here as:
 ** - new QRecordWithJoinedRecords(order)
 **      .withJoinedRecordValues(lineItem)
 **      .withJoinedRecordValues(extrinsic)
 **
 ** Then we'd have the appearance of values in the object like:
 ** - id, orderNo, orderDate, lineItem.id, lineItem.sku, lineItem.quantity, extrinsic.id, extrinsic.key, extrinsic.value
 **
 ** Which, by the by, is how a query that returns joined records looks, and, is
 ** what BackendQueryFilterUtils can use to do filter.
 **
 ** This is done without copying or mutating any of the records (which, if you just use
 ** QRecord.withJoinedRecordValues, then those values are copied into the main record)
 ** - because this object is just storing references to the input records.
 **
 ** Note that this implies that, values changed in this record (e.g, calls to setValue)
 ** WILL impact the underlying records!
 *******************************************************************************/
public class QRecordWithJoinedRecords extends QRecord
{
   private QRecord              mainRecord;
   private Map<String, QRecord> components = new LinkedHashMap<>();


   /***************************************************************************
    **
    ***************************************************************************/
   public QRecordWithJoinedRecords(QRecord mainRecord)
   {
      this.mainRecord = mainRecord;
   }



   /*************************************************************************
    **
    ***************************************************************************/
   @Override
   public void addJoinedRecordValues(String joinTableName, QRecord joinedRecord)
   {
      components.put(joinTableName, joinedRecord);
   }



   /*************************************************************************
    **
    ***************************************************************************/
   public QRecordWithJoinedRecords withJoinedRecordValues(QRecord record, String joinTableName)
   {
      addJoinedRecordValues(joinTableName, record);
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Serializable getValue(String fieldName)
   {
      return performFunctionOnRecordBasedOnFieldName(fieldName, ((record, f) -> record.getValue(f)));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void setValue(String fieldName, Object value)
   {
      performFunctionOnRecordBasedOnFieldName(fieldName, ((record, f) ->
      {
         record.setValue(f, value);
         return (null);
      }));
   }



   /***************************************************************************
    *
    ***************************************************************************/
   @Override
   public void setValue(String fieldName, Serializable value)
   {
      performFunctionOnRecordBasedOnFieldName(fieldName, ((record, f) ->
      {
         record.setValue(f, value);
         return (null);
      }));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void removeValue(String fieldName)
   {
      performFunctionOnRecordBasedOnFieldName(fieldName, ((record, f) ->
      {
         record.removeValue(f);
         return (null);
      }));
   }



   /***************************************************************************
    ** avoid having this same block in all the functions that call it...
    ** given a fieldName, which may be a joinTable.fieldName, apply the function
    ** to the right entity.
    ***************************************************************************/
   private Serializable performFunctionOnRecordBasedOnFieldName(String fieldName, BiFunction<QRecord, String, Serializable> functionToPerform)
   {
      if(fieldName.contains("."))
      {
         String[] parts     = fieldName.split("\\.");
         QRecord  component = components.get(parts[0]);
         if(component != null)
         {
            return functionToPerform.apply(component, parts[1]);
         }
         else
         {
            return null;
         }
      }
      else
      {
         return functionToPerform.apply(mainRecord, fieldName);
      }
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, Serializable> getValues()
   {
      Map<String, Serializable> rs = new LinkedHashMap<>(mainRecord.getValues());
      for(Map.Entry<String, QRecord> componentEntry : components.entrySet())
      {
         String  joinTableName   = componentEntry.getKey();
         QRecord componentRecord = componentEntry.getValue();
         for(Map.Entry<String, Serializable> entry : componentRecord.getValues().entrySet())
         {
            rs.put(joinTableName + "." + entry.getKey(), entry.getValue());
         }
      }
      return (rs);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public Map<String, List<QRecord>> getAssociatedRecords()
   {
      return mainRecord.getAssociatedRecords();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QRecord withAssociatedRecord(String name, QRecord associatedRecord)
   {
      mainRecord.withAssociatedRecord(name, associatedRecord);
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QRecord withAssociatedRecords(Map<String, List<QRecord>> associatedRecords)
   {
      mainRecord.withAssociatedRecords(associatedRecords);
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public void setAssociatedRecords(Map<String, List<QRecord>> associatedRecords)
   {
      mainRecord.setAssociatedRecords(associatedRecords);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QRecord withAssociatedRecords(String name, List<QRecord> associatedRecords)
   {
      mainRecord.withAssociatedRecords(name, associatedRecords);
      return (this);
   }



   /***************************************************************************
    * Given an object of this type (`this`), add a list of join-records to it,
    * producing a new list, which is `this` × joinRecordList.
    * One may want to use this in a loop to build a larger cross product - more
    * to come here in that spirit.
    * @param joinTable name of the join table (e.g., to prefix the join fields)
    * @param joinRecordList list of join records.  may not be null.  may be 0+ size.
    * @return list of new QRecordWithJoinedRecords, based on `this` with each
    * joinRecord added (e.g., output list is same size as joinRecordList).
    * Note that does imply an 'inner' style join - where - if the joinRecordList
    * is empty, you'll get back an empty list!
    ***************************************************************************/
   public List<QRecordWithJoinedRecords> buildCrossProduct(String joinTable, List<QRecord> joinRecordList)
   {
      List<QRecordWithJoinedRecords> rs = new ArrayList<>();

      for(QRecord joinRecord : joinRecordList)
      {
         /////////////////////////////////////////////////////////////
         // essentially clone the existing QRecordWithJoinedRecords //
         /////////////////////////////////////////////////////////////
         QRecordWithJoinedRecords newRecord = new QRecordWithJoinedRecords(mainRecord);
         components.forEach((k, v) -> newRecord.addJoinedRecordValues(k, v));

         ///////////////////////////////////////////
         // now add the new join record to it too //
         ///////////////////////////////////////////
         newRecord.addJoinedRecordValues(joinTable, joinRecord);
         rs.add(newRecord);
      }

      return (rs);
   }
}
