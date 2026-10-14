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


import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpContentVisibility;
import com.kingsrook.qqq.backend.core.model.metadata.help.HelpFormat;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
 ** Public help contract. Targeting and data provenance are never serialized.
 *******************************************************************************/
public class HelpContentV1 implements ToSchema
{
   @OpenAPIExclude
   private QHelpContent wrapped;



   /*******************************************************************************
    ** Constructor for schema generation.
    *******************************************************************************/
   public HelpContentV1()
   {
   }



   /*******************************************************************************
    ** Wrap visible help.
    *******************************************************************************/
   public HelpContentV1(QHelpContent wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Getter for content
    *******************************************************************************/
   @OpenAPIDescription("Long help in the declared format.")
   public String getContent()
   {
      return (wrapped.getContent());
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
    ** Getter for summary
    *******************************************************************************/
   @OpenAPIDescription("Optional short plain-text tooltip.")
   public String getSummary()
   {
      return (wrapped.getSummary());
   }



   /*******************************************************************************
    ** Getter for format
    *******************************************************************************/
   @OpenAPIDescription("Format of the long help.")
   public HelpFormat getFormat()
   {
      return (wrapped.getFormat());
   }



   /*******************************************************************************
    ** Getter for roles
    *******************************************************************************/
   @OpenAPIListItems(String.class)
   @OpenAPIDescription("Screen roles covered by this entry.")
   public List<String> getRoles()
   {
      return (wrapped.getRoles() == null ? null : wrapped.getRoles().stream().map(Object::toString).sorted().toList());
   }



   /*******************************************************************************
    ** Wrap a list, applying targeting also for endpoints wrapping full metadata.
    *******************************************************************************/
   public static List<HelpContentV1> of(List<QHelpContent> contents)
   {
      return (contents == null ? null : HelpContentVisibility.filter(contents).stream().map(HelpContentV1::new).toList());
   }



   /*******************************************************************************
    ** Wrap a slot map while preserving property names and null shapes.
    *******************************************************************************/
   public static Map<String, List<HelpContentV1>> of(Map<String, List<QHelpContent>> contents)
   {
      if(contents == null)
      {
         return (null);
      }
      Map<String, List<HelpContentV1>> result = new LinkedHashMap<>();
      contents.forEach((slot, values) -> result.put(slot, of(values)));
      return (result);
   }
}
