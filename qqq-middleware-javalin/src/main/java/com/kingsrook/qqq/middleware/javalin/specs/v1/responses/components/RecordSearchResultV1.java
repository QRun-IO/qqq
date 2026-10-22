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


import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


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



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Relevance from the record search provider (higher is better); absent for the default search.")
   public Double getScore()
   {
      return (this.wrapped.getScore());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("When the record was last modified (ISO-8601 instant), when its table has a last-modified field the user may see.")
   public String getUpdatedAt()
   {
      return (this.wrapped.getUpdatedAt() == null ? null : this.wrapped.getUpdatedAt().toString());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Plain-text snippets of the fields (other than those in the record label) that match the search term, with the matched spans.")
   @OpenAPIListItems(value = RecordSearchHighlightV1.class, useRef = true)
   public List<RecordSearchHighlightV1> getHighlights()
   {
      return (this.wrapped.getHighlights() == null ? null : this.wrapped.getHighlights().stream().map(RecordSearchHighlightV1::new).toList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Other search field values (not in the record label, and not highlighted) to show under the record label.")
   @OpenAPIListItems(value = RecordSearchDetailV1.class, useRef = true)
   public List<RecordSearchDetailV1> getDetails()
   {
      return (this.wrapped.getDetails() == null ? null : this.wrapped.getDetails().stream().map(RecordSearchDetailV1::new).toList());
   }

}
