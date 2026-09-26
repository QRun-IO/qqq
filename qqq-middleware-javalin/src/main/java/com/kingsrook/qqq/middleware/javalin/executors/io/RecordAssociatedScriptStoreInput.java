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
 ** Input for the v1 route that stores a new revision of a record's associated script.
 *******************************************************************************/
public class RecordAssociatedScriptStoreInput extends AbstractMiddlewareInput
{
   private String tableName;
   private String primaryKey;
   private String fieldName;
   private String contents;
   private String commitMessage;



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
   public RecordAssociatedScriptStoreInput withTableName(String tableName)
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
   public RecordAssociatedScriptStoreInput withPrimaryKey(String primaryKey)
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
   public RecordAssociatedScriptStoreInput withFieldName(String fieldName)
   {
      this.fieldName = fieldName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for contents
    *******************************************************************************/
   public String getContents()
   {
      return (this.contents);
   }



   /*******************************************************************************
    ** Setter for contents
    *******************************************************************************/
   public void setContents(String contents)
   {
      this.contents = contents;
   }



   /*******************************************************************************
    ** Fluent setter for contents
    *******************************************************************************/
   public RecordAssociatedScriptStoreInput withContents(String contents)
   {
      this.contents = contents;
      return (this);
   }



   /*******************************************************************************
    ** Getter for commitMessage
    *******************************************************************************/
   public String getCommitMessage()
   {
      return (this.commitMessage);
   }



   /*******************************************************************************
    ** Setter for commitMessage
    *******************************************************************************/
   public void setCommitMessage(String commitMessage)
   {
      this.commitMessage = commitMessage;
   }



   /*******************************************************************************
    ** Fluent setter for commitMessage
    *******************************************************************************/
   public RecordAssociatedScriptStoreInput withCommitMessage(String commitMessage)
   {
      this.commitMessage = commitMessage;
      return (this);
   }

}
