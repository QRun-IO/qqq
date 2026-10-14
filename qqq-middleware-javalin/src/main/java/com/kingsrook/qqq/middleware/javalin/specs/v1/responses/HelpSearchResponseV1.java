/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2023.  Kingsrook, LLC
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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.kingsrook.qqq.backend.core.actions.help.HelpSearchResult;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.HelpSearchResultV1;


/*******************************************************************************
    ** HelpSearchResponseV1 contract.
    *******************************************************************************/
public class HelpSearchResponseV1 implements AbstractMiddlewareOutputInterface, ToSchema
{
   @OpenAPIListItems(value = HelpSearchResultV1.class, useRef = true)
   private List<HelpSearchResultV1> results;



   /*******************************************************************************
    ** Getter for results
    *******************************************************************************/
   @JsonInclude(JsonInclude.Include.ALWAYS)
   public List<HelpSearchResultV1> getResults()
   {
      return (results);
   }



   /*******************************************************************************
    ** Set results from the core action.
    *******************************************************************************/
   public void setResults(List<HelpSearchResult> results)
   {
      this.results = results.stream().map(HelpSearchResultV1::new).toList();
   }
}
