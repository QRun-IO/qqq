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
import java.util.Map;
import java.util.TreeMap;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendBuiltInView;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.openapi.model.Schema;
import com.kingsrook.qqq.openapi.model.Type;


/*******************************************************************************
 ** Built-in ownership view. Keep expression objects intact on the wire.
 *******************************************************************************/
public class BuiltInViewV1 implements ToSchema
{
   @OpenAPIExclude()
   private QFrontendBuiltInView wrapped;



   /*******************************************************************************
    ** Constructor for schema publication.
    *******************************************************************************/
   public BuiltInViewV1()
   {
   }



   /*******************************************************************************
    ** Constructor for metadata responses.
    *******************************************************************************/
   public BuiltInViewV1(QFrontendBuiltInView wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Stable view identifier.
    *******************************************************************************/
   public String getName()
   {
      return (wrapped.getName());
   }



   /*******************************************************************************
    ** User-facing view label.
    *******************************************************************************/
   public String getLabel()
   {
      return (wrapped.getLabel());
   }



   /*******************************************************************************
    ** A reusable filter; CurrentUser is displayed as "Me" by clients.
    *******************************************************************************/
   public QQueryFilter getFilter()
   {
      return (wrapped.getFilter());
   }



   /*******************************************************************************
    ** This additive schema describes the built-in ownership filter without
    ** changing the older QueryFilter response's string-only values contract.
    *******************************************************************************/
   @Override
   public Schema toSchema()
   {
      Schema currentUser = new Schema().withType(Type.OBJECT)
         .withProperties(new TreeMap<>(Map.of("type", new Schema().withType(Type.STRING).withEnumValues(List.of("CurrentUser")))))
         .withDescription("The executing session's user id. Display as Me. A missing user matches nothing.");
      Schema criterion = new Schema().withType(Type.OBJECT).withProperties(new TreeMap<>(Map.of(
         "fieldName", new Schema().withType(Type.STRING),
         "operator", new Schema().withType(Type.STRING).withEnumValues(List.of("EQUALS")),
         "values", new Schema().withType(Type.ARRAY).withItems(currentUser))));
      Schema filter = new Schema().withType(Type.OBJECT).withProperties(new TreeMap<>(Map.of(
         "booleanOperator", new Schema().withType(Type.STRING).withEnumValues(List.of("OR")),
         "criteria", new Schema().withType(Type.ARRAY).withItems(criterion))));
      return (new Schema().withType(Type.OBJECT).withProperties(new TreeMap<>(Map.of(
         "name", new Schema().withType(Type.STRING),
         "label", new Schema().withType(Type.STRING),
         "filter", filter))));
   }
}
