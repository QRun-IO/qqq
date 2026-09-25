/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
