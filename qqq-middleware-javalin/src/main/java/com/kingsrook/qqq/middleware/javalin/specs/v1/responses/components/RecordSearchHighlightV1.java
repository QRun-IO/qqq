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
import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchHighlight;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/***************************************************************************
 ** A plain-text snippet of a field that matches the search term, with the matched spans.
 ***************************************************************************/
public class RecordSearchHighlightV1 implements ToSchema
{
   @OpenAPIExclude()
   private RecordSearchHighlight wrapped;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RecordSearchHighlightV1(RecordSearchHighlight wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RecordSearchHighlightV1()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Name of the field the snippet is from.")
   public String getFieldName()
   {
      return (this.wrapped.getFieldName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("User-facing label of the field.")
   public String getFieldLabel()
   {
      return (this.wrapped.getFieldLabel());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Plain-text snippet of the field value (never markup; an ellipsis marks trimmed ends).")
   public String getText()
   {
      return (this.wrapped.getText());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Matched spans in the text: sorted, non-overlapping character offsets (start inclusive, end exclusive).")
   @OpenAPIListItems(value = RecordSearchHighlightMatchV1.class, useRef = true)
   public List<RecordSearchHighlightMatchV1> getMatches()
   {
      return (this.wrapped.getMatches() == null ? null : this.wrapped.getMatches().stream().map(RecordSearchHighlightMatchV1::new).toList());
   }

}
