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


import com.kingsrook.qqq.backend.core.actions.help.HelpSearchResult;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/*******************************************************************************
    ** HelpSearchResultV1 contract.
    *******************************************************************************/
public class HelpSearchResultV1 implements ToSchema
{
   @OpenAPIExclude
   private HelpSearchResult wrapped;



   /*******************************************************************************
    ** Constructor for schema generation.
    *******************************************************************************/
   public HelpSearchResultV1()
   {
   }



   /*******************************************************************************
    ** Wrap the action result.
    *******************************************************************************/
   public HelpSearchResultV1(HelpSearchResult wrapped)
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
    ** Getter for name
    *******************************************************************************/
   @OpenAPIDescription("Target metadata name.")
   public String getName()
   {
      return (wrapped.getName());
   }



   /*******************************************************************************
    ** Getter for table
    *******************************************************************************/
   @OpenAPIDescription("Related table name, when applicable.")
   public String getTable()
   {
      return (wrapped.getTable());
   }



   /*******************************************************************************
    ** Getter for process
    *******************************************************************************/
   @OpenAPIDescription("Related process name, when applicable.")
   public String getProcess()
   {
      return (wrapped.getProcess());
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
    ** Getter for summary
    *******************************************************************************/
   @OpenAPIDescription("Optional short plain-text tooltip.")
   public String getSummary()
   {
      return (wrapped.getSummary());
   }



   /*******************************************************************************
    ** Getter for text
    *******************************************************************************/
   @OpenAPIDescription("Plain-text help excerpt, at most 600 characters.")
   public String getText()
   {
      return (wrapped.getText());
   }



   /*******************************************************************************
    ** Getter for contentAsHtml
    *******************************************************************************/
   @OpenAPIDescription("Rendered Markdown (raw HTML escaped, unsafe links removed). Explicit HTML is unchanged and clients must sanitize it.")
   public String getContentAsHtml()
   {
      return (wrapped.getContentAsHtml());
   }



   /*******************************************************************************
    ** Getter for href
    *******************************************************************************/
   @OpenAPIDescription("Application-relative navigation link, when available.")
   public String getHref()
   {
      return (wrapped.getHref());
   }
}
