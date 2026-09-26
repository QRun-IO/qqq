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


import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendAssociation;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/*******************************************************************************
 ** A named association and its join, without recursive child table metadata.
 *******************************************************************************/
public class Association implements ToSchema
{
   @OpenAPIExclude()
   private QFrontendAssociation wrapped;



   /*******************************************************************************
    **
    *******************************************************************************/
   public Association(QFrontendAssociation wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public Association()
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Exact name used for this group in associatedRecords")
   public String getName()
   {
      return (wrapped.getName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("Name of the associated table; its permissions remain independent")
   public String getAssociatedTableName()
   {
      return (wrapped.getAssociatedTableName());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @OpenAPIDescription("The named join and field pairs defining this association")
   public QJoinMetaData getJoin()
   {
      return (wrapped.getJoin());
   }
}
