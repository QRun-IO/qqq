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
import java.util.stream.Collectors;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/***************************************************************************
 **
 ***************************************************************************/
public class FilterCriteria implements ToSchema
{
   @OpenAPIExclude()
   private QFilterCriteria wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FilterCriteria(QFilterCriteria wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FilterCriteria()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Field name that this criteria applies to")
   public String getFieldName()
   {
      return (this.wrapped.getFieldName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Logical operator that applies to this criteria")
   public QCriteriaOperator getOperator()
   {
      return (this.wrapped.getOperator());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Values to apply via the operator to the field")
   @OpenAPIListItems(value = String.class)
   public List<String> getValues()
   {
      return (CollectionUtils.nonNullList(this.wrapped.getValues()).stream().map(String::valueOf).collect(Collectors.toList()));
   }

}
