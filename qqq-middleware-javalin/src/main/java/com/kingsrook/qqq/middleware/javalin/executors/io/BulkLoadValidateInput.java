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
import com.kingsrook.qqq.backend.core.processes.implementations.bulk.insert.model.BulkLoadProfile;


/*******************************************************************************
 ** Input for POST /bulkLoad/{tableName}/validate.
 *******************************************************************************/
public class BulkLoadValidateInput extends AbstractMiddlewareInput
{
   private String uploadRef;
   private String tableName;
   private BulkLoadProfile profile;
   private String mode;
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
   public BulkLoadValidateInput withUploadRef(String uploadRef)
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
   public BulkLoadValidateInput withTableName(String tableName)
   {
      this.tableName = tableName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for profile
    *******************************************************************************/
   public BulkLoadProfile getProfile()
   {
      return (this.profile);
   }



   /*******************************************************************************
    ** Setter for profile
    *******************************************************************************/
   public void setProfile(BulkLoadProfile profile)
   {
      this.profile = profile;
   }



   /*******************************************************************************
    ** Fluent setter for profile
    *******************************************************************************/
   public BulkLoadValidateInput withProfile(BulkLoadProfile profile)
   {
      this.profile = profile;
      return (this);
   }



   /*******************************************************************************
    ** Getter for mode
    *******************************************************************************/
   public String getMode()
   {
      return (this.mode);
   }



   /*******************************************************************************
    ** Setter for mode
    *******************************************************************************/
   public void setMode(String mode)
   {
      this.mode = mode;
   }



   /*******************************************************************************
    ** Fluent setter for mode
    *******************************************************************************/
   public BulkLoadValidateInput withMode(String mode)
   {
      this.mode = mode;
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
   public BulkLoadValidateInput withRoute(BulkLoadAiRoute route)
   {
      this.route = route;
      return (this);
   }
}
