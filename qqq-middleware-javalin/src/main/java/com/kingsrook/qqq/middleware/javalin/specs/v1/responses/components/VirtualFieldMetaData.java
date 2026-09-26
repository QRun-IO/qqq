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


import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendVirtualFieldMetaData;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIIncludeProperties;


/*******************************************************************************
 * API response component representing a virtual field's metadata, extending the
 * base {@link FieldMetaData} with queryability flags that indicate whether the
 * virtual field may be used as a filter criterion and/or included in query output.
 *******************************************************************************/
@OpenAPIIncludeProperties(ancestorClasses = FieldMetaData.class)
public class VirtualFieldMetaData extends FieldMetaData implements ToSchema
{
   @OpenAPIExclude()
   private QVirtualFieldMetaData wrappedFull;

   @OpenAPIExclude()
   private QFrontendVirtualFieldMetaData wrappedFrontend;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public VirtualFieldMetaData(QVirtualFieldMetaData wrapped)
   {
      super(wrapped);
      this.wrappedFull = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public VirtualFieldMetaData(QFrontendVirtualFieldMetaData wrapped)
   {
      super(wrapped);
      this.wrappedFrontend = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public VirtualFieldMetaData()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Indicator of whether or not this virtual field can be included in query outputs (e.g., if it can be selected, in the SQL sense)")
   public Boolean getIsQuerySelectable()
   {
      return (this.wrappedFull != null ? this.wrappedFull.getIsQuerySelectable() : this.wrappedFrontend != null ? this.wrappedFrontend.getIsQuerySelectable() : false);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Indicator of whether or not this virtual field can be included as a query filter criteria")
   public Boolean getIsQueryCriteria()
   {
      return (this.wrappedFull != null ? this.wrappedFull.getIsQueryCriteria() : this.wrappedFrontend != null ? this.wrappedFrontend.getIsQueryCriteria() : false);
   }

}
