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


import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/***************************************************************************
 **
 ***************************************************************************/
public class QueryJoin implements ToSchema
{
   @OpenAPIExclude()
   private com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QueryJoin(com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public QueryJoin()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Table being joined into the query by this QueryJoin")
   public String getJoinTable()
   {
      return (this.wrapped.getJoinTable());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Base table (or an alias) that this QueryJoin is joined against")
   public String getBaseTableOrAlias()
   {
      return (this.wrapped.getBaseTableOrAlias());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Name of a join to use in case the baseTable and joinTable have more than one")
   public String getJoinName()
   {
      return (this.wrapped.getJoinMetaData() == null ? null : this.wrapped.getJoinMetaData().getName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Alias to apply to this table in the join query")
   public String getAlias()
   {
      return (this.wrapped.getAlias());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Whether or not to select values from the join table")
   public Boolean getSelect()
   {
      return (this.wrapped.getSelect());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Type of join being performed (SQL semantics)")
   public com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin.Type getType()
   {
      return (this.wrapped.getType());
   }

}
