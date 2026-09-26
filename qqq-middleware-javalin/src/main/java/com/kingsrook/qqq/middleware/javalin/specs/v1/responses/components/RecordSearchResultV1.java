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


import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/***************************************************************************
 ** One record matched by record search.
 ***************************************************************************/
public class RecordSearchResultV1 implements ToSchema
{
   @OpenAPIExclude()
   private RecordSearchResult wrapped;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RecordSearchResultV1(RecordSearchResult wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RecordSearchResultV1()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Name of the table that the record is from.")
   public String getTableName()
   {
      return (this.wrapped.getTableName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("User-facing label of the table that the record is from.")
   public String getTableLabel()
   {
      return (this.wrapped.getTableLabel());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Primary key of the record, as a string.")
   public String getRecordId()
   {
      return (ValueUtils.getValueAsString(this.wrapped.getRecordId()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Label to identify the record to a user.")
   public String getRecordLabel()
   {
      return (this.wrapped.getRecordLabel());
   }

}
