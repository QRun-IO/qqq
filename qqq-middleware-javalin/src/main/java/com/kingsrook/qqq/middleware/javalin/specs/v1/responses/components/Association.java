/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2024.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
