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
