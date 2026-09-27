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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import java.util.List;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordSearchOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RecordSearchResultV1;


/*******************************************************************************
 ** Response of the record search endpoint.
 *******************************************************************************/
public class RecordSearchResponseV1 implements RecordSearchOutputInterface, ToSchema
{
   @OpenAPIDescription("Records whose search fields match the search term, grouped by table (in instance order) and ordered by primary key within each table.")
   @OpenAPIListItems(value = RecordSearchResultV1.class, useRef = true)
   private List<RecordSearchResultV1> results;



   /*******************************************************************************
    ** Setter for results
    *******************************************************************************/
   @Override
   public void setResults(List<RecordSearchResult> results)
   {
      this.results = results == null ? null : results.stream().map(RecordSearchResultV1::new).toList();
   }



   /*******************************************************************************
    ** Fluent setter for results
    *******************************************************************************/
   public RecordSearchResponseV1 withResults(List<RecordSearchResult> results)
   {
      setResults(results);
      return (this);
   }



   /*******************************************************************************
    ** Getter for results
    *******************************************************************************/
   public List<RecordSearchResultV1> getResults()
   {
      return (this.results);
   }

}
