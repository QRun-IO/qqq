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

package com.kingsrook.qqq.backend.core.model.developer;


import java.io.Serializable;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** What a {@link DeveloperPanelProviderInterface} builds its panel for: a
 ** table, and (on a record's developer view) the record - already loaded under
 ** the current user's permissions and record security locks.
 *******************************************************************************/
public class DeveloperPanelContext
{
   /** The table. */
   private QTableMetaData table;

   /** The record (null for a table-level developer view). */
   private QRecord record;


   /*******************************************************************************
    ** The table's name.
    *******************************************************************************/
   public String getTableName()
   {
      return (table == null ? null : table.getName());
   }



   /*******************************************************************************
    ** The record's primary key value, or null without a record.
    *******************************************************************************/
   public Serializable getPrimaryKey()
   {
      return (table == null || record == null ? null : record.getValue(table.getPrimaryKeyField()));
   }



   /*******************************************************************************
    ** Getter for table
    *******************************************************************************/
   public QTableMetaData getTable()
   {
      return (this.table);
   }



   /*******************************************************************************
    ** Setter for table
    *******************************************************************************/
   public void setTable(QTableMetaData table)
   {
      this.table = table;
   }



   /*******************************************************************************
    ** Fluent setter for table
    *******************************************************************************/
   public DeveloperPanelContext withTable(QTableMetaData table)
   {
      this.table = table;
      return (this);
   }



   /*******************************************************************************
    ** Getter for record
    *******************************************************************************/
   public QRecord getRecord()
   {
      return (this.record);
   }



   /*******************************************************************************
    ** Setter for record
    *******************************************************************************/
   public void setRecord(QRecord record)
   {
      this.record = record;
   }



   /*******************************************************************************
    ** Fluent setter for record
    *******************************************************************************/
   public DeveloperPanelContext withRecord(QRecord record)
   {
      this.record = record;
      return (this);
   }

}
