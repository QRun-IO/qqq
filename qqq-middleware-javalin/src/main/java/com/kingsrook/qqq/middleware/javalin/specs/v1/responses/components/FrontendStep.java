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
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIListItems;


/*******************************************************************************
 **
 *******************************************************************************/
public class FrontendStep implements ToSchema
{
   @OpenAPIExclude()
   private QFrontendStepMetaData wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FrontendStep(QFrontendStepMetaData wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public FrontendStep()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("The unique name for this step within its process")
   public String getName()
   {
      return (this.wrapped.getName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("The user-facing name for this step")
   public String getLabel()
   {
      return (this.wrapped.getLabel());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("An optional indicator of the screen format preferred by the application to be used for this screen.  Different frontends may support different formats, and implement them differently.")
   public String getFormat()
   {
      return (this.wrapped.getFormat());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("The components that make up this screen")
   @OpenAPIListItems(value = FrontendComponent.class, useRef = true)
   public List<FrontendComponent> getComponents()
   {
      return (CollectionUtils.nonNullList(this.wrapped.getComponents()).stream().map(f -> new FrontendComponent(f)).toList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Fields used as form fields (inputs) on this step/screen")
   @OpenAPIListItems(value = FieldMetaData.class, useRef = true)
   public List<FieldMetaData> getFormFields()
   {
      return (CollectionUtils.nonNullList(this.wrapped.getFormFields()).stream().map(f -> new FieldMetaData(f)).toList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Fields used as view-only fields on this step/screen")
   @OpenAPIListItems(value = FieldMetaData.class, useRef = true)
   public List<FieldMetaData> getViewFields()
   {
      return (CollectionUtils.nonNullList(this.wrapped.getViewFields()).stream().map(f -> new FieldMetaData(f)).toList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Fields used in record-lists shown on the step/screen.")
   @OpenAPIListItems(value = FieldMetaData.class, useRef = true)
   public List<FieldMetaData> getRecordListFields()
   {
      return (CollectionUtils.nonNullList(this.wrapped.getRecordListFields()).stream().map(f -> new FieldMetaData(f)).toList());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Name of the step a user can go back to from this step (step with isStepBack=true), when there is one.")
   public String getBackStepName()
   {
      return (this.wrapped.getBackStepName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Help content shown on the step/screen.")
   public List<QHelpContent> getHelpContents()
   {
      return (this.wrapped.getHelpContents());
   }

}
