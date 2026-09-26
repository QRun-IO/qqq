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

package com.kingsrook.qqq.middleware.javalin.executors.io;


/*******************************************************************************
 ** Input for the v1 route that gets the logs of a record's associated script revision.
 *******************************************************************************/
public class RecordAssociatedScriptLogsInput extends AbstractMiddlewareInput
{
   private String tableName;
   private String primaryKey;
   private String fieldName;
   private String scriptRevisionId;



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
   public RecordAssociatedScriptLogsInput withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for primaryKey
    *******************************************************************************/
   public String getPrimaryKey()
   {
      return (this.primaryKey);
   }



   /*******************************************************************************
    ** Setter for primaryKey
    *******************************************************************************/
   public void setPrimaryKey(String primaryKey)
   {
      this.primaryKey = primaryKey;
   }



   /*******************************************************************************
    ** Fluent setter for primaryKey
    *******************************************************************************/
   public RecordAssociatedScriptLogsInput withPrimaryKey(String primaryKey)
   {
      this.primaryKey = primaryKey;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fieldName
    *******************************************************************************/
   public String getFieldName()
   {
      return (this.fieldName);
   }



   /*******************************************************************************
    ** Setter for fieldName
    *******************************************************************************/
   public void setFieldName(String fieldName)
   {
      this.fieldName = fieldName;
   }



   /*******************************************************************************
    ** Fluent setter for fieldName
    *******************************************************************************/
   public RecordAssociatedScriptLogsInput withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for scriptRevisionId
    *******************************************************************************/
   public String getScriptRevisionId()
   {
      return (this.scriptRevisionId);
   }



   /*******************************************************************************
    ** Setter for scriptRevisionId
    *******************************************************************************/
   public void setScriptRevisionId(String scriptRevisionId)
   {
      this.scriptRevisionId = scriptRevisionId;
   }



   /*******************************************************************************
    ** Fluent setter for scriptRevisionId
    *******************************************************************************/
   public RecordAssociatedScriptLogsInput withScriptRevisionId(String scriptRevisionId)
   {
      this.scriptRevisionId = scriptRevisionId;
      return (this);
   }

}
