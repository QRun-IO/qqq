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
import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageEntry;
import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageTotal;
import com.kingsrook.qqq.middleware.javalin.executors.io.AbstractMiddlewareOutputInterface;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.HelpCoverageEntryV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.HelpCoverageTotalV1;


/*******************************************************************************
    ** HelpCoverageResponseV1 contract.
    *******************************************************************************/
public class HelpCoverageResponseV1 implements AbstractMiddlewareOutputInterface, ToSchema
{
   @OpenAPIListItems(value = HelpCoverageTotalV1.class, useRef = true)
   private List<HelpCoverageTotalV1> totals;
   @OpenAPIListItems(value = HelpCoverageEntryV1.class, useRef = true)
   private List<HelpCoverageEntryV1> entries;



   /*******************************************************************************
    ** Getter for totals
    *******************************************************************************/
   @JsonInclude(JsonInclude.Include.ALWAYS)
   public List<HelpCoverageTotalV1> getTotals()
   {
      return (totals);
   }



   /*******************************************************************************
    ** Set totals from the core action.
    *******************************************************************************/
   public void setTotals(List<HelpCoverageTotal> totals)
   {
      this.totals = totals.stream().map(HelpCoverageTotalV1::new).toList();
   }



   /*******************************************************************************
    ** Getter for entries
    *******************************************************************************/
   @JsonInclude(JsonInclude.Include.ALWAYS)
   public List<HelpCoverageEntryV1> getEntries()
   {
      return (entries);
   }



   /*******************************************************************************
    ** Set entries from the core action.
    *******************************************************************************/
   public void setEntries(List<HelpCoverageEntry> entries)
   {
      this.entries = entries.stream().map(HelpCoverageEntryV1::new).toList();
   }
}
