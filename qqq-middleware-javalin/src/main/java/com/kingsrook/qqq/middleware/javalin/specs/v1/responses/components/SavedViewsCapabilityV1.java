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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;


/*******************************************************************************
 ** Saved views of a table (the standard saved-view processes).  Since UI API 1.6.0.
 *******************************************************************************/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SavedViewsCapabilityV1 implements ToSchema
{
   @OpenAPIDescription("True when the user may list saved views (queryProcessName can run).")
   private Boolean available;

   @OpenAPIDescription("Lists the views (input tableName; output savedViewList).")
   private String  queryProcessName;

   @OpenAPIDescription("Stores (inserts or updates) a view; absent when the user may not store views.")
   private String  storeProcessName;

   @OpenAPIDescription("Deletes a view; absent when the user may not delete views.")
   private String  deleteProcessName;



   /*******************************************************************************
    ** Getter for available
    *******************************************************************************/
   public Boolean getAvailable()
   {
      return (this.available);
   }



   /*******************************************************************************
    ** Fluent setter for available
    *******************************************************************************/
   public SavedViewsCapabilityV1 withAvailable(Boolean available)
   {
      this.available = available;
      return (this);
   }



   /*******************************************************************************
    ** Getter for queryProcessName
    *******************************************************************************/
   public String getQueryProcessName()
   {
      return (this.queryProcessName);
   }



   /*******************************************************************************
    ** Fluent setter for queryProcessName
    *******************************************************************************/
   public SavedViewsCapabilityV1 withQueryProcessName(String queryProcessName)
   {
      this.queryProcessName = queryProcessName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for storeProcessName
    *******************************************************************************/
   public String getStoreProcessName()
   {
      return (this.storeProcessName);
   }



   /*******************************************************************************
    ** Fluent setter for storeProcessName
    *******************************************************************************/
   public SavedViewsCapabilityV1 withStoreProcessName(String storeProcessName)
   {
      this.storeProcessName = storeProcessName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for deleteProcessName
    *******************************************************************************/
   public String getDeleteProcessName()
   {
      return (this.deleteProcessName);
   }



   /*******************************************************************************
    ** Fluent setter for deleteProcessName
    *******************************************************************************/
   public SavedViewsCapabilityV1 withDeleteProcessName(String deleteProcessName)
   {
      this.deleteProcessName = deleteProcessName;
      return (this);
   }
}
