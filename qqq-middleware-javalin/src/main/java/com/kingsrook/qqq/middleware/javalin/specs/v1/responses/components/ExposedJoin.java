/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
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
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendExposedJoin;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/***************************************************************************
 **
 ***************************************************************************/
public class ExposedJoin implements ToSchema
{
   @OpenAPIExclude()
   private QFrontendExposedJoin wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ExposedJoin(QFrontendExposedJoin section)
   {
      this.wrapped = section;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ExposedJoin()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("User-facing label to display for this join")
   public String getLabel()
   {
      return (this.wrapped.getLabel());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Whether or not this join is 'to many' in nature")
   public Boolean getIsMany()
   {
      return (this.wrapped.getIsMany());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("The meta-data for the joined table")
   public TableMetaData getJoinTable()
   {
      return (new TableMetaData(this.wrapped.getJoinTable()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("A list of joins that travel from the base table to the exposed join table")
   @OpenAPIListItems(value = QJoinMetaData.class, useRef = false)
   public List<QJoinMetaData> getJoinPath()
   {
      return (this.wrapped.getJoinPath());
   }

}
