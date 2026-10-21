/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.actions.tables.picker;


import java.io.Serializable;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;


/*******************************************************************************
 ** Input for RelatedRecordPickerAction: the table that owns the picker
 ** (tableName), the picker, the parent record and the operation's arguments.
 ** See docs/RELATED-RECORD-PICKER.md.
 *******************************************************************************/
public class RelatedRecordPickerInput extends AbstractTableActionInput
{
   private String                          pickerName;
   private Serializable                    primaryKey;
   private Map<String, Serializable>       values;
   private String                          searchTerm;
   private QQueryFilter                    filter;
   private Integer                         skip;
   private Integer                         limit;
   private List<Serializable>              targetIds;
   private List<Map<String, Serializable>> records;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RelatedRecordPickerInput()
   {
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RelatedRecordPickerInput(String tableName, String pickerName)
   {
      setTableName(tableName);
      this.pickerName = pickerName;
   }


   /*******************************************************************************
    ** Getter for pickerName
    *******************************************************************************/
   public String getPickerName()
   {
      return (this.pickerName);
   }



   /*******************************************************************************
    ** Setter for pickerName
    *******************************************************************************/
   public void setPickerName(String pickerName)
   {
      this.pickerName = pickerName;
   }



   /*******************************************************************************
    ** Fluent setter for pickerName
    *******************************************************************************/
   public RelatedRecordPickerInput withPickerName(String pickerName)
   {
      this.pickerName = pickerName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for primaryKey
    *******************************************************************************/
   public Serializable getPrimaryKey()
   {
      return (this.primaryKey);
   }



   /*******************************************************************************
    ** Setter for primaryKey
    *******************************************************************************/
   public void setPrimaryKey(Serializable primaryKey)
   {
      this.primaryKey = primaryKey;
   }



   /*******************************************************************************
    ** Fluent setter for primaryKey
    *******************************************************************************/
   public RelatedRecordPickerInput withPrimaryKey(Serializable primaryKey)
   {
      this.primaryKey = primaryKey;
      return (this);
   }


   /*******************************************************************************
    ** Getter for values
    *******************************************************************************/
   public Map<String, Serializable> getValues()
   {
      return (this.values);
   }



   /*******************************************************************************
    ** Setter for values
    *******************************************************************************/
   public void setValues(Map<String, Serializable> values)
   {
      this.values = values;
   }



   /*******************************************************************************
    ** Fluent setter for values
    *******************************************************************************/
   public RelatedRecordPickerInput withValues(Map<String, Serializable> values)
   {
      this.values = values;
      return (this);
   }


   /*******************************************************************************
    ** Getter for searchTerm
    *******************************************************************************/
   public String getSearchTerm()
   {
      return (this.searchTerm);
   }



   /*******************************************************************************
    ** Setter for searchTerm
    *******************************************************************************/
   public void setSearchTerm(String searchTerm)
   {
      this.searchTerm = searchTerm;
   }



   /*******************************************************************************
    ** Fluent setter for searchTerm
    *******************************************************************************/
   public RelatedRecordPickerInput withSearchTerm(String searchTerm)
   {
      this.searchTerm = searchTerm;
      return (this);
   }


   /*******************************************************************************
    ** Getter for filter
    *******************************************************************************/
   public QQueryFilter getFilter()
   {
      return (this.filter);
   }



   /*******************************************************************************
    ** Setter for filter
    *******************************************************************************/
   public void setFilter(QQueryFilter filter)
   {
      this.filter = filter;
   }



   /*******************************************************************************
    ** Fluent setter for filter
    *******************************************************************************/
   public RelatedRecordPickerInput withFilter(QQueryFilter filter)
   {
      this.filter = filter;
      return (this);
   }


   /*******************************************************************************
    ** Getter for skip
    *******************************************************************************/
   public Integer getSkip()
   {
      return (this.skip);
   }



   /*******************************************************************************
    ** Setter for skip
    *******************************************************************************/
   public void setSkip(Integer skip)
   {
      this.skip = skip;
   }



   /*******************************************************************************
    ** Fluent setter for skip
    *******************************************************************************/
   public RelatedRecordPickerInput withSkip(Integer skip)
   {
      this.skip = skip;
      return (this);
   }


   /*******************************************************************************
    ** Getter for limit
    *******************************************************************************/
   public Integer getLimit()
   {
      return (this.limit);
   }



   /*******************************************************************************
    ** Setter for limit
    *******************************************************************************/
   public void setLimit(Integer limit)
   {
      this.limit = limit;
   }



   /*******************************************************************************
    ** Fluent setter for limit
    *******************************************************************************/
   public RelatedRecordPickerInput withLimit(Integer limit)
   {
      this.limit = limit;
      return (this);
   }


   /*******************************************************************************
    ** Getter for targetIds
    *******************************************************************************/
   public List<Serializable> getTargetIds()
   {
      return (this.targetIds);
   }



   /*******************************************************************************
    ** Setter for targetIds
    *******************************************************************************/
   public void setTargetIds(List<Serializable> targetIds)
   {
      this.targetIds = targetIds;
   }



   /*******************************************************************************
    ** Fluent setter for targetIds
    *******************************************************************************/
   public RelatedRecordPickerInput withTargetIds(List<Serializable> targetIds)
   {
      this.targetIds = targetIds;
      return (this);
   }


   /*******************************************************************************
    ** Getter for records
    *******************************************************************************/
   public List<Map<String, Serializable>> getRecords()
   {
      return (this.records);
   }



   /*******************************************************************************
    ** Setter for records
    *******************************************************************************/
   public void setRecords(List<Map<String, Serializable>> records)
   {
      this.records = records;
   }



   /*******************************************************************************
    ** Fluent setter for records
    *******************************************************************************/
   public RelatedRecordPickerInput withRecords(List<Map<String, Serializable>> records)
   {
      this.records = records;
      return (this);
   }
}
