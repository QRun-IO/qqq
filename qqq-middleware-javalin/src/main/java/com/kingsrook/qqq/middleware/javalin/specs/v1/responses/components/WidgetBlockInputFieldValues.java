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


import com.kingsrook.qqq.backend.core.model.dashboard.widgets.blocks.inputfield.InputFieldValues;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/*******************************************************************************
 **
 *******************************************************************************/
@OpenAPIDescription("Values used for an INPUT_FIELD type widget block")
public final class WidgetBlockInputFieldValues implements WidgetBlockValues
{
   @OpenAPIExclude()
   private InputFieldValues wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public WidgetBlockInputFieldValues(InputFieldValues textValues)
   {
      this.wrapped = textValues;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public WidgetBlockInputFieldValues()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Metadata to define the field that this block controls")
   public FieldMetaData getFieldMetaData()
   {
      return (new FieldMetaData(this.wrapped.getFieldMetaData()));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Indicate whether this field should auto-focus when it is rendered")
   public Boolean getAutoFocus()
   {
      return (this.wrapped.getAutoFocus());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Indicate whether the form that this field is on should be submitted when Enter is pressed")
   public Boolean getSubmitOnEnter()
   {
      return (this.wrapped.getSubmitOnEnter());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Indicate if the frontend uses a software/on-screen keyboard, if the application should try to hide it (e.g., upon auto-focus).")
   public Boolean getHideSoftKeyboard()
   {
      return (this.wrapped.getHideSoftKeyboard());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Optional placeholder text to display in the input box.")
   public String getPlaceholder()
   {
      return (this.wrapped.getPlaceholder());
   }

}
