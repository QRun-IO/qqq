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

package com.kingsrook.qqq.backend.core.model.actions.tables.search;


import java.io.Serializable;


/*******************************************************************************
 ** Summary of the record search of one table: how many results it returned,
 ** whether it has more, and what searched it.
 *******************************************************************************/
public class RecordSearchTableSummary implements Serializable
{
   private static final long serialVersionUID = 1L;

   private String             tableName;
   private String             tableLabel;
   private Integer            resultCount;
   private Boolean            hasMore;
   private Integer            totalCount;
   private RecordSearchSource source;



   /*******************************************************************************
    ** Getter for tableName
    *******************************************************************************/
   public String getTableName()
   {
      return (this.tableName);
   }



   /*******************************************************************************
    ** Setter for tableName
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public RecordSearchTableSummary withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableLabel
    *******************************************************************************/
   public String getTableLabel()
   {
      return (this.tableLabel);
   }



   /*******************************************************************************
    ** Setter for tableLabel
    *******************************************************************************/
   public void setTableLabel(String tableLabel)
   {
      this.tableLabel = tableLabel;
   }



   /*******************************************************************************
    ** Fluent setter for tableLabel
    *******************************************************************************/
   public RecordSearchTableSummary withTableLabel(String tableLabel)
   {
      this.tableLabel = tableLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for resultCount
    *******************************************************************************/
   public Integer getResultCount()
   {
      return (this.resultCount);
   }



   /*******************************************************************************
    ** Setter for resultCount
    *******************************************************************************/
   public void setResultCount(Integer resultCount)
   {
      this.resultCount = resultCount;
   }



   /*******************************************************************************
    ** Fluent setter for resultCount - number of results output for the table
    *******************************************************************************/
   public RecordSearchTableSummary withResultCount(Integer resultCount)
   {
      this.resultCount = resultCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for hasMore
    *******************************************************************************/
   public Boolean getHasMore()
   {
      return (this.hasMore);
   }



   /*******************************************************************************
    ** Setter for hasMore
    *******************************************************************************/
   public void setHasMore(Boolean hasMore)
   {
      this.hasMore = hasMore;
   }



   /*******************************************************************************
    ** Fluent setter for hasMore - whether more matches exist past this page
    *******************************************************************************/
   public RecordSearchTableSummary withHasMore(Boolean hasMore)
   {
      this.hasMore = hasMore;
      return (this);
   }



   /*******************************************************************************
    ** Getter for totalCount
    *******************************************************************************/
   public Integer getTotalCount()
   {
      return (this.totalCount);
   }



   /*******************************************************************************
    ** Setter for totalCount
    *******************************************************************************/
   public void setTotalCount(Integer totalCount)
   {
      this.totalCount = totalCount;
   }



   /*******************************************************************************
    ** Fluent setter for totalCount - approximate total matches, when the searcher knows it (null otherwise)
    *******************************************************************************/
   public RecordSearchTableSummary withTotalCount(Integer totalCount)
   {
      this.totalCount = totalCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for source
    *******************************************************************************/
   public RecordSearchSource getSource()
   {
      return (this.source);
   }



   /*******************************************************************************
    ** Setter for source
    *******************************************************************************/
   public void setSource(RecordSearchSource source)
   {
      this.source = source;
   }



   /*******************************************************************************
    ** Fluent setter for source - what searched the table
    *******************************************************************************/
   public RecordSearchTableSummary withSource(RecordSearchSource source)
   {
      this.source = source;
      return (this);
   }

}
