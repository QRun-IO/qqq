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


import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchTableSummary;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/***************************************************************************
 ** Summary of the record search of one table.
 ***************************************************************************/
public class RecordSearchTableSummaryV1 implements ToSchema
{
   @OpenAPIExclude()
   private RecordSearchTableSummary wrapped;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RecordSearchTableSummaryV1(RecordSearchTableSummary wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RecordSearchTableSummaryV1()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Name of the table.")
   public String getTableName()
   {
      return (this.wrapped.getTableName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("User-facing label of the table.")
   public String getTableLabel()
   {
      return (this.wrapped.getTableLabel());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Number of results returned from the table.")
   public Integer getResultCount()
   {
      return (this.wrapped.getResultCount());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Whether the table has more matches past these results (request them with offset).")
   public Boolean getHasMore()
   {
      return (this.wrapped.getHasMore());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Approximate total number of matches in the table, when the search knows it (records the user may not read can make it high).")
   public Integer getTotalCount()
   {
      return (this.wrapped.getTotalCount());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("What searched the table: the record search PROVIDER, the DEFAULT search, or the default search as a FALLBACK because the provider failed.")
   public RecordSearchSource getSource()
   {
      return (this.wrapped.getSource());
   }

}
