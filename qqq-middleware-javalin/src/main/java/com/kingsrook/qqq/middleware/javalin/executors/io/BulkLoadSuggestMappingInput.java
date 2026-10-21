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
 ** Input for POST /bulkLoad/{tableName}/suggestMapping.
 *******************************************************************************/
public class BulkLoadSuggestMappingInput extends AbstractMiddlewareInput
{
   private String uploadRef;
   private String tableName;
   private Boolean isBulkEdit;



   /*******************************************************************************
    ** Getter for uploadRef
    *******************************************************************************/
   public String getUploadRef()
   {
      return (this.uploadRef);
   }



   /*******************************************************************************
    ** Setter for uploadRef
    *******************************************************************************/
   public void setUploadRef(String uploadRef)
   {
      this.uploadRef = uploadRef;
   }



   /*******************************************************************************
    ** Fluent setter for uploadRef
    *******************************************************************************/
   public BulkLoadSuggestMappingInput withUploadRef(String uploadRef)
   {
      this.uploadRef = uploadRef;
      return (this);
   }



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
   public BulkLoadSuggestMappingInput withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for isBulkEdit
    *******************************************************************************/
   public Boolean getIsBulkEdit()
   {
      return (this.isBulkEdit);
   }



   /*******************************************************************************
    ** Setter for isBulkEdit
    *******************************************************************************/
   public void setIsBulkEdit(Boolean isBulkEdit)
   {
      this.isBulkEdit = isBulkEdit;
   }



   /*******************************************************************************
    ** Fluent setter for isBulkEdit
    *******************************************************************************/
   public BulkLoadSuggestMappingInput withIsBulkEdit(Boolean isBulkEdit)
   {
      this.isBulkEdit = isBulkEdit;
      return (this);
   }
}
