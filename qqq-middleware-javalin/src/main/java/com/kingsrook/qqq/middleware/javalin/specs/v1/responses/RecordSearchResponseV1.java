/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
