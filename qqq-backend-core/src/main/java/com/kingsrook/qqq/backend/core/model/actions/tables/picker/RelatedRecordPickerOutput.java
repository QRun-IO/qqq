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
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Output of RelatedRecordPickerAction.  Which members are set depends on the
 ** operation (query, prepare, add, remove).
 *******************************************************************************/
public class RelatedRecordPickerOutput extends AbstractActionOutput
{
   private List<QRecord>      records;
   private boolean            hasMore;
   private List<Serializable> linkedIds;
   private QQueryFilter       resolvedDefaultFilter;
   private List<String>       reviewFieldNames;
   private Integer            deletedCount;
   private List<String>       warnings;


   /*******************************************************************************
    ** Getter for records
    *******************************************************************************/
   public List<QRecord> getRecords()
   {
      return (this.records);
   }



   /*******************************************************************************
    ** Setter for records
    *******************************************************************************/
   public void setRecords(List<QRecord> records)
   {
      this.records = records;
   }



   /*******************************************************************************
    ** Fluent setter for records
    *******************************************************************************/
   public RelatedRecordPickerOutput withRecords(List<QRecord> records)
   {
      this.records = records;
      return (this);
   }


   /*******************************************************************************
    ** Getter for hasMore
    *******************************************************************************/
   public boolean getHasMore()
   {
      return (this.hasMore);
   }



   /*******************************************************************************
    ** Setter for hasMore
    *******************************************************************************/
   public void setHasMore(boolean hasMore)
   {
      this.hasMore = hasMore;
   }



   /*******************************************************************************
    ** Fluent setter for hasMore
    *******************************************************************************/
   public RelatedRecordPickerOutput withHasMore(boolean hasMore)
   {
      this.hasMore = hasMore;
      return (this);
   }


   /*******************************************************************************
    ** Getter for linkedIds
    *******************************************************************************/
   public List<Serializable> getLinkedIds()
   {
      return (this.linkedIds);
   }



   /*******************************************************************************
    ** Setter for linkedIds
    *******************************************************************************/
   public void setLinkedIds(List<Serializable> linkedIds)
   {
      this.linkedIds = linkedIds;
   }



   /*******************************************************************************
    ** Fluent setter for linkedIds
    *******************************************************************************/
   public RelatedRecordPickerOutput withLinkedIds(List<Serializable> linkedIds)
   {
      this.linkedIds = linkedIds;
      return (this);
   }


   /*******************************************************************************
    ** Getter for resolvedDefaultFilter
    *******************************************************************************/
   public QQueryFilter getResolvedDefaultFilter()
   {
      return (this.resolvedDefaultFilter);
   }



   /*******************************************************************************
    ** Setter for resolvedDefaultFilter
    *******************************************************************************/
   public void setResolvedDefaultFilter(QQueryFilter resolvedDefaultFilter)
   {
      this.resolvedDefaultFilter = resolvedDefaultFilter;
   }



   /*******************************************************************************
    ** Fluent setter for resolvedDefaultFilter
    *******************************************************************************/
   public RelatedRecordPickerOutput withResolvedDefaultFilter(QQueryFilter resolvedDefaultFilter)
   {
      this.resolvedDefaultFilter = resolvedDefaultFilter;
      return (this);
   }


   /*******************************************************************************
    ** Getter for reviewFieldNames
    *******************************************************************************/
   public List<String> getReviewFieldNames()
   {
      return (this.reviewFieldNames);
   }



   /*******************************************************************************
    ** Setter for reviewFieldNames
    *******************************************************************************/
   public void setReviewFieldNames(List<String> reviewFieldNames)
   {
      this.reviewFieldNames = reviewFieldNames;
   }



   /*******************************************************************************
    ** Fluent setter for reviewFieldNames
    *******************************************************************************/
   public RelatedRecordPickerOutput withReviewFieldNames(List<String> reviewFieldNames)
   {
      this.reviewFieldNames = reviewFieldNames;
      return (this);
   }


   /*******************************************************************************
    ** Getter for deletedCount
    *******************************************************************************/
   public Integer getDeletedCount()
   {
      return (this.deletedCount);
   }



   /*******************************************************************************
    ** Setter for deletedCount
    *******************************************************************************/
   public void setDeletedCount(Integer deletedCount)
   {
      this.deletedCount = deletedCount;
   }



   /*******************************************************************************
    ** Fluent setter for deletedCount
    *******************************************************************************/
   public RelatedRecordPickerOutput withDeletedCount(Integer deletedCount)
   {
      this.deletedCount = deletedCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for warnings
    *******************************************************************************/
   public List<String> getWarnings()
   {
      return (this.warnings);
   }



   /*******************************************************************************
    ** Setter for warnings
    *******************************************************************************/
   public void setWarnings(List<String> warnings)
   {
      this.warnings = warnings;
   }



   /*******************************************************************************
    ** Fluent setter for warnings
    *******************************************************************************/
   public RelatedRecordPickerOutput withWarnings(List<String> warnings)
   {
      this.warnings = warnings;
      return (this);
   }
}
