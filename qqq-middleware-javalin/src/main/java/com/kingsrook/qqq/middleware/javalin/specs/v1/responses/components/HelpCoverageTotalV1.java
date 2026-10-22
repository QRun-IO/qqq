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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components;


import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageTotal;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/*******************************************************************************
    ** HelpCoverageTotalV1 contract.
    *******************************************************************************/
public class HelpCoverageTotalV1 implements ToSchema
{
   @OpenAPIExclude
   private HelpCoverageTotal wrapped;



   /*******************************************************************************
    ** Constructor for schema generation.
    *******************************************************************************/
   public HelpCoverageTotalV1()
   {
   }



   /*******************************************************************************
    ** Wrap the action result.
    *******************************************************************************/
   public HelpCoverageTotalV1(HelpCoverageTotal wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Getter for kind
    *******************************************************************************/
   @OpenAPIDescription("Help target kind.")
   public String getKind()
   {
      return (wrapped.getKind());
   }



   /*******************************************************************************
    ** Getter for total
    *******************************************************************************/
   @OpenAPIDescription("Number of targets in the selected kind and parent scope.")
   public Integer getTotal()
   {
      return (wrapped.getTotal());
   }



   /*******************************************************************************
    ** Getter for withHelp
    *******************************************************************************/
   @OpenAPIDescription("Targets with a visible summary or long content.")
   public Integer getWithHelp()
   {
      return (wrapped.getWithHelp());
   }
}
