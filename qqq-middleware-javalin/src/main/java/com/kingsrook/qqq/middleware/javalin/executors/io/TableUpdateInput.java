/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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

package com.kingsrook.qqq.middleware.javalin.executors.io;


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 **
 *******************************************************************************/
public class TableUpdateInput extends AbstractMiddlewareInput
{
   private String                    tableName;
   private String                    primaryKey;
   private Map<String, Serializable> recordValues;
   private QRecord record;



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
   }



   /*******************************************************************************
    ** Fluent setter for tableName
    **
    *******************************************************************************/
   public TableUpdateInput withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for primaryKey
    **
    *******************************************************************************/
   public String getPrimaryKey()
   {
      return primaryKey;
   }



   /*******************************************************************************
    ** Setter for primaryKey
    **
    *******************************************************************************/
   public void setPrimaryKey(String primaryKey)
   {
      this.primaryKey = primaryKey;
   }



   /*******************************************************************************
    ** Fluent setter for primaryKey
    **
    *******************************************************************************/
   public TableUpdateInput withPrimaryKey(String primaryKey)
   {
      this.primaryKey = primaryKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for recordValues
    **
    *******************************************************************************/
   public Map<String, Serializable> getRecordValues()
   {
      return recordValues;
   }



   /*******************************************************************************
    ** Setter for recordValues
    **
    *******************************************************************************/
   public void setRecordValues(Map<String, Serializable> recordValues)
   {
      this.recordValues = recordValues;
   }



   /*******************************************************************************
    ** Fluent setter for recordValues
    **
    *******************************************************************************/
   public TableUpdateInput withRecordValues(Map<String, Serializable> recordValues)
   {
      this.recordValues = recordValues;
      return (this);
   }



   /*******************************************************************************
    ** Getter for record
    *******************************************************************************/
   public QRecord getRecord()
   {
      return (record);
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
   public TableUpdateInput withRecord(QRecord record)
   {
      this.record = record;
      return (this);
   }

}
