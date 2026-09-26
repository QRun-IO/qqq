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

package com.kingsrook.qqq.backend.core.model.actions;


import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Base class for input for any qqq action that works against a table.
 **
 *******************************************************************************/
public class AbstractTableActionInput extends AbstractActionInput
{
   private String      tableName;
   private InputSource inputSource = QInputSource.SYSTEM;

   private QTableMetaData tableMetaData;


   /*******************************************************************************
    **
    *******************************************************************************/
   public AbstractTableActionInput()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getActionIdentity()
   {
      return (getClass().getSimpleName() + ":" + getTableName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QBackendMetaData getBackend()
   {
      return (QContext.getQInstance().getBackendForTable(getTableName()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public QTableMetaData getTable()
   {
      if(tableMetaData == null)
      {
         tableMetaData = QContext.getQInstance().getTable(getTableName());
      }

      return (tableMetaData);
   }



   /*******************************************************************************
    ** Setter for tableMetaData
    **
    *******************************************************************************/
   public void setTableMetaData(QTableMetaData tableMetaData)
   {
      this.tableMetaData = tableMetaData;
      this.tableName = tableMetaData == null ? null : tableMetaData.getName();
   }



   /*******************************************************************************
    ** Getter for tableName
    **
    *******************************************************************************/
   public String getTableName()
   {
      return tableName;
   }



   /*******************************************************************************
    ** Setter for tableName
    **
    *******************************************************************************/
   public void setTableName(String tableName)
   {
      this.tableName = tableName;
      this.tableMetaData = QContext.getQInstance().getTable(getTableName());
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    *******************************************************************************/
   public AbstractTableActionInput withTableName(String tableName)
   {
      setTableName(tableName);
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
    * Fluent setter for inputSource
    *
    * @param inputSource
    * Indicator of what the source of the action is.  Default values from QQQ are
    * USER or SYSTEM (where it can be the case that an action that originated from
    * a USER leads to SYSTEM actions (e.g., running a process - the RunProcessAction
    * is USER initiated, but if that process then runs a query, by default, it would
    * be SYSTEM initiated).
    * @return this
    *******************************************************************************/
   public AbstractTableActionInput withInputSource(InputSource inputSource)
   {
      this.inputSource = inputSource;
      return (this);
   }



   /*******************************************************************************
    * Getter for tableMetaData
    * @see #withTableMetaData(QTableMetaData)
    *******************************************************************************/
   public QTableMetaData getTableMetaData()
   {
      return (this.tableMetaData);
   }



   /*******************************************************************************
    * Fluent setter for tableMetaData
    *
    * @param tableMetaData
    * The table (possibly personalized!) that the action is running against.
    * This property and @see {@link #withTableName(String)} are kept in-sync by all
    * 4 setters & fluent setters for either of them.  e.g., setting tableName sets
    * tableMetaData (to the active QInstance's version of the table).
    * @return this
    *******************************************************************************/
   public AbstractTableActionInput withTableMetaData(QTableMetaData tableMetaData)
   {
      setTableMetaData(tableMetaData);
      return (this);
   }


}
