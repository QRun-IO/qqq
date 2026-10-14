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


import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.assist.BulkLoadAiRoute;


/*******************************************************************************
 ** Input for GET /bulkLoad/uploads/{uploadRef}/profile.
 *******************************************************************************/
public class BulkLoadUploadProfileInput extends AbstractMiddlewareInput
{
   private String uploadRef;
   private String tableName;
   private Integer sampleCount;
   private BulkLoadAiRoute route;



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
   public BulkLoadUploadProfileInput withUploadRef(String uploadRef)
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
   public BulkLoadUploadProfileInput withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sampleCount
    *******************************************************************************/
   public Integer getSampleCount()
   {
      return (this.sampleCount);
   }



   /*******************************************************************************
    ** Setter for sampleCount
    *******************************************************************************/
   public void setSampleCount(Integer sampleCount)
   {
      this.sampleCount = sampleCount;
   }



   /*******************************************************************************
    ** Fluent setter for sampleCount
    *******************************************************************************/
   public BulkLoadUploadProfileInput withSampleCount(Integer sampleCount)
   {
      this.sampleCount = sampleCount;
      return (this);
   }



   /*******************************************************************************
    ** Getter for route
    *******************************************************************************/
   public BulkLoadAiRoute getRoute()
   {
      return (this.route);
   }



   /*******************************************************************************
    ** Setter for route
    *******************************************************************************/
   public void setRoute(BulkLoadAiRoute route)
   {
      this.route = route;
   }



   /*******************************************************************************
    ** Fluent setter for route
    *******************************************************************************/
   public BulkLoadUploadProfileInput withRoute(BulkLoadAiRoute route)
   {
      this.route = route;
      return (this);
   }
}
