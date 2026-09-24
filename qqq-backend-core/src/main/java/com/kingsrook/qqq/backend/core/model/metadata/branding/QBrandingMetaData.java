/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.branding;


import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.TopLevelMetaDataInterface;


/*******************************************************************************
 ** Meta-Data to define branding in a QQQ instance.
 **
 *******************************************************************************/
public class QBrandingMetaData implements TopLevelMetaDataInterface, Cloneable, Serializable
{
   private String companyName;
   private String companyUrl;
   private String appName;
   private String logo;
   private String icon;
   private String accentColor;
   private String accentColorLight;
   private String gravatarDefault;

   private Map<BannerSlot, Banner> banners;


   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public QBrandingMetaData clone()
   {
      try
      {
         QBrandingMetaData clone = (QBrandingMetaData) super.clone();

         //////////////////////////////////////////////////////////////////////////////////////
         // copy mutable state here, so the clone can't change the internals of the original //
         //////////////////////////////////////////////////////////////////////////////////////
         if(banners != null)
         {
            clone.banners = new LinkedHashMap<>();
            for(Map.Entry<BannerSlot, Banner> entry : this.banners.entrySet())
            {
               clone.banners.put(entry.getKey(), entry.getValue().clone());
            }
         }

         return clone;
      }
      catch(CloneNotSupportedException e)
      {
         throw new AssertionError();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String toString()
   {
      return ("QBrandingMetaData[" + appName + "]");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return "Branding";
   }



   /*******************************************************************************
    ** Getter for companyName
    **
    *******************************************************************************/
   public String getCompanyName()
   {
      return companyName;
   }



   /*******************************************************************************
    ** Setter for companyName
    **
    *******************************************************************************/
   public void setCompanyName(String companyName)
   {
      this.companyName = companyName;
   }



   /*******************************************************************************
    ** Fluent setter for companyName
    **
    *******************************************************************************/
   public QBrandingMetaData withCompanyName(String companyName)
   {
      this.companyName = companyName;
      return this;
   }



   /*******************************************************************************
    ** Getter for logo
    **
    *******************************************************************************/
   public String getLogo()
   {
      return logo;
   }



   /*******************************************************************************
    ** Setter for logo
    **
    *******************************************************************************/
   public void setLogo(String logo)
   {
      this.logo = logo;
   }



   /*******************************************************************************
    ** Fluent setter for logo
    **
    *******************************************************************************/
   public QBrandingMetaData withLogo(String logo)
   {
      this.logo = logo;
      return this;
   }



   /*******************************************************************************
    ** Getter for icon
    **
    *******************************************************************************/
   public String getIcon()
   {
      return icon;
   }



   /*******************************************************************************
    ** Setter for icon
    **
    *******************************************************************************/
   public void setIcon(String icon)
   {
      this.icon = icon;
   }



   /*******************************************************************************
    ** Fluent setter for icon
    **
    *******************************************************************************/
   public QBrandingMetaData withIcon(String icon)
   {
      this.icon = icon;
      return this;
   }



   /*******************************************************************************
    ** Getter for appName
    *******************************************************************************/
   public String getAppName()
   {
      return (this.appName);
   }



   /*******************************************************************************
    ** Setter for appName
    *******************************************************************************/
   public void setAppName(String appName)
   {
      this.appName = appName;
   }



   /*******************************************************************************
    ** Fluent setter for appName
    *******************************************************************************/
   public QBrandingMetaData withAppName(String appName)
   {
      this.appName = appName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for companyUrl
    *******************************************************************************/
   public String getCompanyUrl()
   {
      return (this.companyUrl);
   }



   /*******************************************************************************
    ** Setter for companyUrl
    *******************************************************************************/
   public void setCompanyUrl(String companyUrl)
   {
      this.companyUrl = companyUrl;
   }



   /*******************************************************************************
    ** Fluent setter for companyUrl
    *******************************************************************************/
   public QBrandingMetaData withCompanyUrl(String companyUrl)
   {
      this.companyUrl = companyUrl;
      return (this);
   }



   /*******************************************************************************
    ** Getter for accentColor
    **
    *******************************************************************************/
   public String getAccentColor()
   {
      return accentColor;
   }



   /*******************************************************************************
    ** Setter for accentColor
    **
    *******************************************************************************/
   public void setAccentColor(String accentColor)
   {
      this.accentColor = accentColor;
   }



   /*******************************************************************************
    ** Fluent setter for accentColor
    **
    *******************************************************************************/
   public QBrandingMetaData withAccentColor(String accentColor)
   {
      this.accentColor = accentColor;
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void addSelfToInstance(QInstance qInstance)
   {
      qInstance.setBranding(this);
   }


   /*******************************************************************************
    ** Getter for banners
    *******************************************************************************/
   public Map<BannerSlot, Banner> getBanners()
   {
      return (this.banners);
   }



   /*******************************************************************************
    ** Setter for banners
    *******************************************************************************/
   public void setBanners(Map<BannerSlot, Banner> banners)
   {
      this.banners = banners;
   }



   /*******************************************************************************
    ** Fluent setter for banners
    *******************************************************************************/
   public QBrandingMetaData withBanners(Map<BannerSlot, Banner> banners)
   {
      this.banners = banners;
      return (this);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   public QBrandingMetaData withBanner(BannerSlot slot, Banner banner)
   {
      if(this.banners == null)
      {
         this.banners = new LinkedHashMap<>();
      }
      this.banners.put(slot, banner);

      return (this);
   }



   /*******************************************************************************
    * Getter for accentColorLight
    * @see #withAccentColorLight(String)
    *******************************************************************************/
   public String getAccentColorLight()
   {
      return (this.accentColorLight);
   }



   /*******************************************************************************
    * Setter for accentColorLight
    * @see #withAccentColorLight(String)
    *******************************************************************************/
   public void setAccentColorLight(String accentColorLight)
   {
      this.accentColorLight = accentColorLight;
   }



   /*******************************************************************************
    * Fluent setter for accentColorLight
    *
    * @param accentColorLight
    * A lighter shade of the accent color, use for UI elements in an alternate mode,
    * such as a 'dirty' quick-filter.
    * @return this
    *******************************************************************************/
   public QBrandingMetaData withAccentColorLight(String accentColorLight)
   {
      this.accentColorLight = accentColorLight;
      return (this);
   }



   /*******************************************************************************
    * Getter for gravatarDefault
    * @see #withGravatarDefault(String)
    *******************************************************************************/
   public String getGravatarDefault()
   {
      return (this.gravatarDefault);
   }



   /*******************************************************************************
    * Setter for gravatarDefault
    * @see #withGravatarDefault(String)
    *******************************************************************************/
   public void setGravatarDefault(String gravatarDefault)
   {
      this.gravatarDefault = gravatarDefault;
   }



   /*******************************************************************************
    * Fluent setter for gravatarDefault
    *
    * @param gravatarDefault
    * value to be appended to the gravatar URL as the 'default' parameter.  See
    * See <a href=https://docs.gravatar.com/sdk/images/>Gravatar docs</a> for details.
    * The short version is - can be a url-encoded URL to an image, or a keyword
    * defined by gravatar, like initials, identicon, or blank.
    * @return this
    *******************************************************************************/
   public QBrandingMetaData withGravatarDefault(String gravatarDefault)
   {
      this.gravatarDefault = gravatarDefault;
      return (this);
   }


}
