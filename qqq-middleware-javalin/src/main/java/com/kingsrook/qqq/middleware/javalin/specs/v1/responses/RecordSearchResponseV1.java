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
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchProviderStatus;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchResult;
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchTableSummary;
import com.kingsrook.qqq.middleware.javalin.executors.io.RecordSearchOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RecordSearchProviderStatusV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RecordSearchResultV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RecordSearchTableSummaryV1;


/*******************************************************************************
 ** Response of the record search endpoint.
 *******************************************************************************/
public class RecordSearchResponseV1 implements RecordSearchOutputInterface, ToSchema
{
   @OpenAPIDescription("Records whose search fields match the search term, grouped by table (in instance order) and, within each table, in the requested order (RELEVANCE: the search provider's order, or by primary key for the default search; RECENT: most recently modified first).")
   @OpenAPIListItems(value = RecordSearchResultV1.class, useRef = true)
   private List<RecordSearchResultV1> results;

   @OpenAPIDescription("A summary of each searched table, in the order of the results: how many results it returned, whether it has more, and what searched it.")
   @OpenAPIListItems(value = RecordSearchTableSummaryV1.class, useRef = true)
   private List<RecordSearchTableSummaryV1> tables;

   @OpenAPIDescription("How the instance's record search provider fared: when its status is FAILED, the tables it claimed were searched with the default search instead (their source is FALLBACK).")
   private RecordSearchProviderStatusV1 provider;



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



   /*******************************************************************************
    ** Setter for tables
    *******************************************************************************/
   @Override
   public void setTables(List<RecordSearchTableSummary> tables)
   {
      this.tables = tables == null ? null : tables.stream().map(RecordSearchTableSummaryV1::new).toList();
   }



   /*******************************************************************************
    ** Fluent setter for tables
    *******************************************************************************/
   public RecordSearchResponseV1 withTables(List<RecordSearchTableSummary> tables)
   {
      setTables(tables);
      return (this);
   }



   /*******************************************************************************
    ** Getter for tables
    *******************************************************************************/
   public List<RecordSearchTableSummaryV1> getTables()
   {
      return (this.tables);
   }



   /*******************************************************************************
    ** Setter for the provider status
    *******************************************************************************/
   @Override
   public void setProviderStatus(RecordSearchProviderStatus providerStatus)
   {
      this.provider = providerStatus == null ? null : new RecordSearchProviderStatusV1(providerStatus);
   }



   /*******************************************************************************
    ** Fluent setter for the provider status
    *******************************************************************************/
   public RecordSearchResponseV1 withProviderStatus(RecordSearchProviderStatus providerStatus)
   {
      setProviderStatus(providerStatus);
      return (this);
   }



   /*******************************************************************************
    ** Getter for provider
    *******************************************************************************/
   public RecordSearchProviderStatusV1 getProvider()
   {
      return (this.provider);
   }

}
