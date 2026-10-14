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


import java.util.List;
import com.kingsrook.qqq.backend.core.actions.help.HelpCoverageEntry;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
    ** HelpCoverageEntryV1 contract.
    *******************************************************************************/
public class HelpCoverageEntryV1 implements ToSchema
{
   @OpenAPIExclude
   private HelpCoverageEntry wrapped;



   /*******************************************************************************
    ** Constructor for schema generation.
    *******************************************************************************/
   public HelpCoverageEntryV1()
   {
   }



   /*******************************************************************************
    ** Wrap the action result.
    *******************************************************************************/
   public HelpCoverageEntryV1(HelpCoverageEntry wrapped)
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
    ** Getter for key
    *******************************************************************************/
   @OpenAPIDescription("Canonical helpContent address.")
   public String getKey()
   {
      return (wrapped.getKey());
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   @OpenAPIDescription("User-facing target label.")
   public String getLabel()
   {
      return (wrapped.getLabel());
   }



   /*******************************************************************************
    ** Getter for parentKey
    *******************************************************************************/
   @OpenAPIDescription("Canonical parent address.")
   public String getParentKey()
   {
      return (wrapped.getParentKey());
   }



   /*******************************************************************************
    ** Getter for hidden
    *******************************************************************************/
   @OpenAPIDescription("Whether the target is hidden in metadata.")
   public Boolean getHidden()
   {
      return (wrapped.getHidden());
   }



   /*******************************************************************************
    ** Getter for hasSummary
    *******************************************************************************/
   @OpenAPIDescription("Whether visible help has an explicit summary.")
   public Boolean getHasSummary()
   {
      return (wrapped.getHasSummary());
   }



   /*******************************************************************************
    ** Getter for hasContent
    *******************************************************************************/
   @OpenAPIDescription("Whether visible help has long content.")
   public Boolean getHasContent()
   {
      return (wrapped.getHasContent());
   }



   /*******************************************************************************
    ** Getter for roles
    *******************************************************************************/
   @OpenAPIListItems(String.class)
   @OpenAPIDescription("Screen roles covered by this entry.")
   public List<String> getRoles()
   {
      return (wrapped.getRoles());
   }
}
