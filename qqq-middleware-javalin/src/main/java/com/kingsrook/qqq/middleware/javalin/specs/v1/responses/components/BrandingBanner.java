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


import java.io.Serializable;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.branding.Banner;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/***************************************************************************
 ** A banner message shown in one of the frontend's banner slots.
 ***************************************************************************/
public class BrandingBanner implements ToSchema
{
   @OpenAPIExclude()
   private Banner wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BrandingBanner(Banner wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public BrandingBanner()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Severity of the message: INFO, WARNING, ERROR or SUCCESS.")
   public String getSeverity()
   {
      return (this.wrapped.getSeverity() == null ? null : this.wrapped.getSeverity().name());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Text color, as a CSS color value.")
   public String getTextColor()
   {
      return (this.wrapped.getTextColor());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Background color, as a CSS color value.")
   public String getBackgroundColor()
   {
      return (this.wrapped.getBackgroundColor());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Plain-text message.")
   public String getMessageText()
   {
      return (this.wrapped.getMessageText());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("HTML message, used instead of the plain-text message when set.")
   public String getMessageHTML()
   {
      return (this.wrapped.getMessageHTML());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Additional CSS style properties for the banner.")
   public Map<String, Serializable> getAdditionalStyles()
   {
      return (this.wrapped.getAdditionalStyles());
   }
}
