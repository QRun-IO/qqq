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
 ** A capability implemented by a standard process: run it by name through the process\noperations (initProcess / stepProcess), never by a hard-coded name.  Since UI API 1.6.0.
 *******************************************************************************/
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessCapabilityV1 implements ToSchema
{
   @OpenAPIDescription("True when the process exists and the user may run it.")
   private Boolean available;

   @OpenAPIDescription("The process to run (present only when available).")
   private String  processName;



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
   public ProcessCapabilityV1 withAvailable(Boolean available)
   {
      this.available = available;
      return (this);
   }



   /*******************************************************************************
    ** Getter for processName
    *******************************************************************************/
   public String getProcessName()
   {
      return (this.processName);
   }



   /*******************************************************************************
    ** Fluent setter for processName
    *******************************************************************************/
   public ProcessCapabilityV1 withProcessName(String processName)
   {
      this.processName = processName;
      return (this);
   }
}
