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


import java.io.Serializable;


/*******************************************************************************
 ** One record matched by record search: its table, primary key and label.
 *******************************************************************************/
public class RecordSearchResult implements Serializable
{
   private static final long serialVersionUID = 1L;

   private String       tableName;
   private String       tableLabel;
   private Serializable recordId;
   private String       recordLabel;



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
   public RecordSearchResult withTableName(String tableName)
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
   public RecordSearchResult withTableLabel(String tableLabel)
   {
      this.tableLabel = tableLabel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordId
    *******************************************************************************/
   public Serializable getRecordId()
   {
      return (this.recordId);
   }



   /*******************************************************************************
    ** Setter for recordId
    *******************************************************************************/
   public void setRecordId(Serializable recordId)
   {
      this.recordId = recordId;
   }



   /*******************************************************************************
    ** Fluent setter for recordId (the record's primary key value)
    *******************************************************************************/
   public RecordSearchResult withRecordId(Serializable recordId)
   {
      this.recordId = recordId;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordLabel
    *******************************************************************************/
   public String getRecordLabel()
   {
      return (this.recordLabel);
   }



   /*******************************************************************************
    ** Setter for recordLabel
    *******************************************************************************/
   public void setRecordLabel(String recordLabel)
   {
      this.recordLabel = recordLabel;
   }



   /*******************************************************************************
    ** Fluent setter for recordLabel
    *******************************************************************************/
   public RecordSearchResult withRecordLabel(String recordLabel)
   {
      this.recordLabel = recordLabel;
      return (this);
   }

}
