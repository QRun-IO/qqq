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
