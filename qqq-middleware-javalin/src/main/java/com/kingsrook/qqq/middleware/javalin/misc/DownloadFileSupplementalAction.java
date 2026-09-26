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

package com.kingsrook.qqq.middleware.javalin.misc;


import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** custom code that can run when user downloads a file.  Set as a code-reference
 ** on a field adornment.
 *******************************************************************************/
public interface DownloadFileSupplementalAction
{

   /***************************************************************************
    **
    ***************************************************************************/
   void run(DownloadFileSupplementalActionInput input, DownloadFileSupplementalActionOutput output) throws QException;


   /***************************************************************************
    **
    ***************************************************************************/
   class DownloadFileSupplementalActionInput
   {
      private String tableName;
      private String primaryKey;
      private String fieldName;
      private String fileName;



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
      public DownloadFileSupplementalActionInput withTableName(String tableName)
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
      public DownloadFileSupplementalActionInput withPrimaryKey(String primaryKey)
      {
         this.primaryKey = primaryKey;
         return (this);
      }



      /*******************************************************************************
       ** Getter for fieldName
       **
       *******************************************************************************/
      public String getFieldName()
      {
         return fieldName;
      }



      /*******************************************************************************
       ** Setter for fieldName
       **
       *******************************************************************************/
      public void setFieldName(String fieldName)
      {
         this.fieldName = fieldName;
      }



      /*******************************************************************************
       ** Fluent setter for fieldName
       **
       *******************************************************************************/
      public DownloadFileSupplementalActionInput withFieldName(String fieldName)
      {
         this.fieldName = fieldName;
         return (this);
      }



      /*******************************************************************************
       ** Getter for fileName
       **
       *******************************************************************************/
      public String getFileName()
      {
         return fileName;
      }



      /*******************************************************************************
       ** Setter for fileName
       **
       *******************************************************************************/
      public void setFileName(String fileName)
      {
         this.fileName = fileName;
      }



      /*******************************************************************************
       ** Fluent setter for fileName
       **
       *******************************************************************************/
      public DownloadFileSupplementalActionInput withFileName(String fileName)
      {
         this.fileName = fileName;
         return (this);
      }

   }



   /***************************************************************************
    **
    ***************************************************************************/
   class DownloadFileSupplementalActionOutput
   {
      /*******************************************************************************
       ** Constructor
       **
       *******************************************************************************/
      public DownloadFileSupplementalActionOutput()
      {
         ////////////////////////////////////////////////////////////////
         // sorry, but here just to get test-coverage on this class... //
         ////////////////////////////////////////////////////////////////
         int i = 0;
      }
   }
}
