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


import com.kingsrook.qqq.backend.core.model.actions.tables.search.RecordSearchHighlightMatch;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/***************************************************************************
 ** One matched span in a record search highlight: character offsets into its text.
 ***************************************************************************/
public class RecordSearchHighlightMatchV1 implements ToSchema
{
   @OpenAPIExclude()
   private RecordSearchHighlightMatch wrapped;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RecordSearchHighlightMatchV1(RecordSearchHighlightMatch wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public RecordSearchHighlightMatchV1()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Offset of the first matched character in the highlight text.")
   public Integer getStart()
   {
      return (this.wrapped.getStart());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Offset just after the last matched character (exclusive).")
   public Integer getEnd()
   {
      return (this.wrapped.getEnd());
   }

}
