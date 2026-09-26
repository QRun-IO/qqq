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
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendProcessMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIIncludeProperties;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/***************************************************************************
 **
 ***************************************************************************/
@OpenAPIIncludeProperties(ancestorClasses = ProcessMetaDataLight.class)
public class ProcessMetaData extends ProcessMetaDataLight implements ToSchema
{


   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ProcessMetaData(QFrontendProcessMetaData wrapped)
   {
      super(wrapped);
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public ProcessMetaData()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Frontend steps (aka, Screens) for this process.")
   @OpenAPIListItems(value = FrontendStep.class, useRef = true)
   public List<FrontendStep> getFrontendSteps()
   {
      return (CollectionUtils.nonNullList(this.wrapped.getFrontendSteps()).stream().map(f -> new FrontendStep(f)).toList());
   }


}
