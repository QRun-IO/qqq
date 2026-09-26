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


import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.branding.Banner;
import com.kingsrook.qqq.backend.core.model.metadata.branding.BannerSlot;
import com.kingsrook.qqq.backend.core.model.metadata.branding.QBrandingMetaData;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.ToSchema;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIDescription;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIExclude;
import com.kingsrook.qqq.middleware.javalin.schemabuilder.annotations.OpenAPIMapValueType;


/***************************************************************************
 ** Application identity (names, images, accent colors and banners) for a frontend.
 ***************************************************************************/
public class Branding implements ToSchema
{
   @OpenAPIExclude()
   private QBrandingMetaData wrapped;



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Branding(QBrandingMetaData wrapped)
   {
      this.wrapped = wrapped;
   }



   /*******************************************************************************
    ** Constructor
    **
    *******************************************************************************/
   public Branding()
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
   @OpenAPIDescription("URL for the company's web site.")
   public String getCompanyUrl()
   {
      return (this.wrapped.getCompanyUrl());
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



   /***************************************************************************
    **
    ***************************************************************************/
   @OpenAPIDescription("Banners to display, keyed by the slot where each is shown.")
   @OpenAPIMapValueType(value = BrandingBanner.class, useRef = true)
   public Map<String, BrandingBanner> getBanners()
   {
      if(this.wrapped.getBanners() == null)
      {
         return (null);
      }

      Map<String, BrandingBanner> banners = new LinkedHashMap<>();
      for(Map.Entry<BannerSlot, Banner> entry : this.wrapped.getBanners().entrySet())
      {
         banners.put(String.valueOf(entry.getKey()), new BrandingBanner(entry.getValue()));
      }
      return (banners);
   }
}
