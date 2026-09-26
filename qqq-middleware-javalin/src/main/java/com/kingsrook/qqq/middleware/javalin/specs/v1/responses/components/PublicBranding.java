/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


import com.kingsrook.qqq.backend.core.model.metadata.branding.QBrandingMetaData;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;


/***************************************************************************
 ** The part of the application's branding that is safe to show before a user
 ** signs in (for example on a login page): names, logo, icon and accent
 ** colors.  Banners are deliberately left out, as they may carry messages
 ** meant for signed-in users only.
 ***************************************************************************/
public class PublicBranding implements ToSchema
{
   @OpenAPIExclude()
   private QBrandingMetaData wrapped;



   /***************************************************************************
    **
    ***************************************************************************/
   public PublicBranding(QBrandingMetaData wrapped)
   {
      this.wrapped = wrapped;
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public PublicBranding()
   {
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Name of the company that operates this application.")
   public String getCompanyName()
   {
      return (this.wrapped.getCompanyName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Name of this application, for display to users.")
   public String getAppName()
   {
      return (this.wrapped.getAppName());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Path or URL of the application's logo image.")
   public String getLogo()
   {
      return (this.wrapped.getLogo());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Path or URL of the application's small icon image.")
   public String getIcon()
   {
      return (this.wrapped.getIcon());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Primary accent color, as a CSS color value.")
   public String getAccentColor()
   {
      return (this.wrapped.getAccentColor());
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Light variant of the accent color, as a CSS color value.")
   public String getAccentColorLight()
   {
      return (this.wrapped.getAccentColorLight());
   }
}
