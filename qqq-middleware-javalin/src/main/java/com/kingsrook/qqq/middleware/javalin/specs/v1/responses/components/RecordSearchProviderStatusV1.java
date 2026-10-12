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


import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchProviderState;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchProviderStatus;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/***************************************************************************
 ** How the instance's record search provider fared in this search.
 ***************************************************************************/
public class RecordSearchProviderStatusV1 implements ToSchema
{
   @OpenAPIExclude()
   private RecordSearchProviderStatus wrapped;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RecordSearchProviderStatusV1(RecordSearchProviderStatus wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RecordSearchProviderStatusV1()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("OK: the provider searched the tables it claimed.  FAILED: it failed, and those tables were searched with the default search.  NONE: there is no provider, or it claimed none of the searched tables.")
   public RecordSearchProviderState getStatus()
   {
      return (this.wrapped.getStatus());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("When the provider failed: the simple class name of the root cause (e.g., ConnectException).")
   public String getFailureClass()
   {
      return (this.wrapped.getFailureClass());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("When the provider failed with a user-facing error: its message (one line).")
   public String getMessage()
   {
      return (this.wrapped.getMessage());
   }

}
