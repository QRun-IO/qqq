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
 ** Record sharing for a shareable table (the standard sharing processes; see\nshareableTableMetaData for the audience setup).  Since UI API 1.6.0.
 *******************************************************************************/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SharingCapabilityV1 implements ToSchema
{
   @OpenAPIDescription("True when the table is shareable and the user may list shares.")
   private Boolean available;

   @OpenAPIDescription("Lists a record's shares.")
   private String  listProcessName;

   @OpenAPIDescription("Adds a share (the record owner only; the server checks).")
   private String  insertProcessName;

   @OpenAPIDescription("Changes a share's scope.")
   private String  editProcessName;

   @OpenAPIDescription("Removes a share.")
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
   public SharingCapabilityV1 withAvailable(Boolean available)
   {
      this.available = available;
      return (this);
   }



   /*******************************************************************************
    ** Getter for listProcessName
    *******************************************************************************/
   public String getListProcessName()
   {
      return (this.listProcessName);
   }



   /*******************************************************************************
    ** Fluent setter for listProcessName
    *******************************************************************************/
   public SharingCapabilityV1 withListProcessName(String listProcessName)
   {
      this.listProcessName = listProcessName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for insertProcessName
    *******************************************************************************/
   public String getInsertProcessName()
   {
      return (this.insertProcessName);
   }



   /*******************************************************************************
    ** Fluent setter for insertProcessName
    *******************************************************************************/
   public SharingCapabilityV1 withInsertProcessName(String insertProcessName)
   {
      this.insertProcessName = insertProcessName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for editProcessName
    *******************************************************************************/
   public String getEditProcessName()
   {
      return (this.editProcessName);
   }



   /*******************************************************************************
    ** Fluent setter for editProcessName
    *******************************************************************************/
   public SharingCapabilityV1 withEditProcessName(String editProcessName)
   {
      this.editProcessName = editProcessName;
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
   public SharingCapabilityV1 withDeleteProcessName(String deleteProcessName)
   {
      this.deleteProcessName = deleteProcessName;
      return (this);
   }
}
