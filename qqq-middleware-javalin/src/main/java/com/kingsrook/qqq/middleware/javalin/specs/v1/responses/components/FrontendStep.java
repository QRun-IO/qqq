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
