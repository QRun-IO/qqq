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


import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.AbstractActionInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;


/*******************************************************************************
 ** Input for {@link com.kingsrook.qqq.backend.core.actions.tables.RecordSearchAction}:
 ** a free-text search term, matched against the search fields of every table
 ** that declares them (optionally only the named tables).
 *******************************************************************************/
public class RecordSearchInput extends AbstractActionInput
{
   private String       searchTerm;
   private List<String> tableNames;
   private Integer      limitPerTable;
   private InputSource  inputSource = QInputSource.SYSTEM;



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
   public RecordSearchInput withSearchTerm(String searchTerm)
   {
      this.searchTerm = searchTerm;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableNames
    *******************************************************************************/
   public List<String> getTableNames()
   {
      return (this.tableNames);
   }



   /*******************************************************************************
    ** Setter for tableNames
    *******************************************************************************/
   public void setTableNames(List<String> tableNames)
   {
      this.tableNames = tableNames;
   }



   /*******************************************************************************
    ** Fluent setter for tableNames - restricts the search to these tables (null
    ** or empty searches every table with search fields).
    *******************************************************************************/
   public RecordSearchInput withTableNames(List<String> tableNames)
   {
      this.tableNames = tableNames;
      return (this);
   }



   /*******************************************************************************
    ** Getter for limitPerTable
    *******************************************************************************/
   public Integer getLimitPerTable()
   {
      return (this.limitPerTable);
   }



   /*******************************************************************************
    ** Setter for limitPerTable
    *******************************************************************************/
   public void setLimitPerTable(Integer limitPerTable)
   {
      this.limitPerTable = limitPerTable;
   }



   /*******************************************************************************
    ** Fluent setter for limitPerTable - the maximum number of records returned
    ** from each table (null uses the default; larger values are capped).
    *******************************************************************************/
   public RecordSearchInput withLimitPerTable(Integer limitPerTable)
   {
      this.limitPerTable = limitPerTable;
      return (this);
   }



   /*******************************************************************************
    ** Getter for inputSource
    *******************************************************************************/
   public InputSource getInputSource()
   {
      return (this.inputSource);
   }



   /*******************************************************************************
    ** Setter for inputSource
    *******************************************************************************/
   public void setInputSource(InputSource inputSource)
   {
      this.inputSource = inputSource;
   }



   /*******************************************************************************
    ** Fluent setter for inputSource
    *******************************************************************************/
   public RecordSearchInput withInputSource(InputSource inputSource)
   {
      this.inputSource = inputSource;
      return (this);
   }

}
