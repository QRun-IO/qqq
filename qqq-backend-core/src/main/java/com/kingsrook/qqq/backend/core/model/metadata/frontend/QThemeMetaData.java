/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.model.metadata.frontend;


import java.util.regex.Pattern;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Supplemental instance metadata for configuring frontend theme properties.
 **
 ** Provides color palette, typography, sizing, and asset path configuration
 ** that gets sent to the frontend for runtime theme customization.
 *******************************************************************************/
public class QThemeMetaData implements QSupplementalInstanceMetaData
{
   private static final QLogger LOG = QLogger.getLogger(QThemeMetaData.class);

   public static final String NAME = "theme";

   private static final Pattern HEX_COLOR_PATTERN = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$");

   //////////////////////
   // Color palette    //
   //////////////////////
   private String primaryColor;
   private String secondaryColor;
   private String backgroundColor;
   private String surfaceColor;
   private String textPrimary;
   private String textSecondary;
   private String errorColor;
   private String warningColor;
   private String successColor;
   private String infoColor;

   //////////////////////
   // Typography       //
   //////////////////////
   private String fontFamily;
   private String headerFontFamily;

   //////////////////////
   // Sizing           //
   //////////////////////
   private String borderRadius;
   private String density;

   //////////////////////
   // Asset paths      //
   //////////////////////
   private String logoPath;
   private String iconPath;
   private String faviconPath;

   //////////////////////
   // Custom CSS       //
   //////////////////////
   private String customCss;



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** Retrieve the QThemeMetaData from a QInstance.
    *******************************************************************************/
   public static QThemeMetaData of(QInstance qInstance)
   {
      return QSupplementalInstanceMetaData.of(qInstance, NAME);
   }



   /*******************************************************************************
    ** Retrieve or create a new QThemeMetaData for a QInstance.
    *******************************************************************************/
   public static QThemeMetaData ofOrWithNew(QInstance qInstance)
   {
      return QSupplementalInstanceMetaData.ofOrWithNew(qInstance, NAME, QThemeMetaData::new);
   }



   /***************************************************************************
    ** Validate theme configuration.
    ** Logs warnings for invalid values but does not prevent startup.
    ***************************************************************************/
   @Override
   public void validate(QInstance qInstance, QInstanceValidator validator)
   {
      validateColor("primaryColor", primaryColor);
      validateColor("secondaryColor", secondaryColor);
      validateColor("backgroundColor", backgroundColor);
      validateColor("surfaceColor", surfaceColor);
      validateColor("textPrimary", textPrimary);
      validateColor("textSecondary", textSecondary);
      validateColor("errorColor", errorColor);
      validateColor("warningColor", warningColor);
      validateColor("successColor", successColor);
      validateColor("infoColor", infoColor);

      if(density != null && !density.equals("compact") && !density.equals("normal") && !density.equals("comfortable"))
      {
         LOG.warn("Invalid theme density value, should be 'compact', 'normal', or 'comfortable'", logPair("density", density));
      }
   }



   /***************************************************************************
    ** Validate a color value is a valid hex code.
    ***************************************************************************/
   private void validateColor(String fieldName, String value)
   {
      if(value != null && !HEX_COLOR_PATTERN.matcher(value).matches())
      {
         LOG.warn("Invalid theme color value, expected hex format (#RGB, #RRGGBB, or #RRGGBBAA)", logPair("field", fieldName), logPair("value", value));
      }
   }



   /*******************************************************************************
    ** Getter for primaryColor
    *******************************************************************************/
   public String getPrimaryColor()
   {
      return (this.primaryColor);
   }



   /*******************************************************************************
    ** Setter for primaryColor
    *******************************************************************************/
   public void setPrimaryColor(String primaryColor)
   {
      this.primaryColor = primaryColor;
   }



   /*******************************************************************************
    ** Fluent setter for primaryColor
    *******************************************************************************/
   public QThemeMetaData withPrimaryColor(String primaryColor)
   {
      this.primaryColor = primaryColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for secondaryColor
    *******************************************************************************/
   public String getSecondaryColor()
   {
      return (this.secondaryColor);
   }



   /*******************************************************************************
    ** Setter for secondaryColor
    *******************************************************************************/
   public void setSecondaryColor(String secondaryColor)
   {
      this.secondaryColor = secondaryColor;
   }



   /*******************************************************************************
    ** Fluent setter for secondaryColor
    *******************************************************************************/
   public QThemeMetaData withSecondaryColor(String secondaryColor)
   {
      this.secondaryColor = secondaryColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for backgroundColor
    *******************************************************************************/
   public String getBackgroundColor()
   {
      return (this.backgroundColor);
   }



   /*******************************************************************************
    ** Setter for backgroundColor
    *******************************************************************************/
   public void setBackgroundColor(String backgroundColor)
   {
      this.backgroundColor = backgroundColor;
   }



   /*******************************************************************************
    ** Fluent setter for backgroundColor
    *******************************************************************************/
   public QThemeMetaData withBackgroundColor(String backgroundColor)
   {
      this.backgroundColor = backgroundColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for surfaceColor
    *******************************************************************************/
   public String getSurfaceColor()
   {
      return (this.surfaceColor);
   }



   /*******************************************************************************
    ** Setter for surfaceColor
    *******************************************************************************/
   public void setSurfaceColor(String surfaceColor)
   {
      this.surfaceColor = surfaceColor;
   }



   /*******************************************************************************
    ** Fluent setter for surfaceColor
    *******************************************************************************/
   public QThemeMetaData withSurfaceColor(String surfaceColor)
   {
      this.surfaceColor = surfaceColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for textPrimary
    *******************************************************************************/
   public String getTextPrimary()
   {
      return (this.textPrimary);
   }



   /*******************************************************************************
    ** Setter for textPrimary
    *******************************************************************************/
   public void setTextPrimary(String textPrimary)
   {
      this.textPrimary = textPrimary;
   }



   /*******************************************************************************
    ** Fluent setter for textPrimary
    *******************************************************************************/
   public QThemeMetaData withTextPrimary(String textPrimary)
   {
      this.textPrimary = textPrimary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for textSecondary
    *******************************************************************************/
   public String getTextSecondary()
   {
      return (this.textSecondary);
   }



   /*******************************************************************************
    ** Setter for textSecondary
    *******************************************************************************/
   public void setTextSecondary(String textSecondary)
   {
      this.textSecondary = textSecondary;
   }



   /*******************************************************************************
    ** Fluent setter for textSecondary
    *******************************************************************************/
   public QThemeMetaData withTextSecondary(String textSecondary)
   {
      this.textSecondary = textSecondary;
      return (this);
   }



   /*******************************************************************************
    ** Getter for errorColor
    *******************************************************************************/
   public String getErrorColor()
   {
      return (this.errorColor);
   }



   /*******************************************************************************
    ** Setter for errorColor
    *******************************************************************************/
   public void setErrorColor(String errorColor)
   {
      this.errorColor = errorColor;
   }



   /*******************************************************************************
    ** Fluent setter for errorColor
    *******************************************************************************/
   public QThemeMetaData withErrorColor(String errorColor)
   {
      this.errorColor = errorColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for warningColor
    *******************************************************************************/
   public String getWarningColor()
   {
      return (this.warningColor);
   }



   /*******************************************************************************
    ** Setter for warningColor
    *******************************************************************************/
   public void setWarningColor(String warningColor)
   {
      this.warningColor = warningColor;
   }



   /*******************************************************************************
    ** Fluent setter for warningColor
    *******************************************************************************/
   public QThemeMetaData withWarningColor(String warningColor)
   {
      this.warningColor = warningColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for successColor
    *******************************************************************************/
   public String getSuccessColor()
   {
      return (this.successColor);
   }



   /*******************************************************************************
    ** Setter for successColor
    *******************************************************************************/
   public void setSuccessColor(String successColor)
   {
      this.successColor = successColor;
   }



   /*******************************************************************************
    ** Fluent setter for successColor
    *******************************************************************************/
   public QThemeMetaData withSuccessColor(String successColor)
   {
      this.successColor = successColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for infoColor
    *******************************************************************************/
   public String getInfoColor()
   {
      return (this.infoColor);
   }



   /*******************************************************************************
    ** Setter for infoColor
    *******************************************************************************/
   public void setInfoColor(String infoColor)
   {
      this.infoColor = infoColor;
   }



   /*******************************************************************************
    ** Fluent setter for infoColor
    *******************************************************************************/
   public QThemeMetaData withInfoColor(String infoColor)
   {
      this.infoColor = infoColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fontFamily
    *******************************************************************************/
   public String getFontFamily()
   {
      return (this.fontFamily);
   }



   /*******************************************************************************
    ** Setter for fontFamily
    *******************************************************************************/
   public void setFontFamily(String fontFamily)
   {
      this.fontFamily = fontFamily;
   }



   /*******************************************************************************
    ** Fluent setter for fontFamily
    *******************************************************************************/
   public QThemeMetaData withFontFamily(String fontFamily)
   {
      this.fontFamily = fontFamily;
      return (this);
   }



   /*******************************************************************************
    ** Getter for headerFontFamily
    *******************************************************************************/
   public String getHeaderFontFamily()
   {
      return (this.headerFontFamily);
   }



   /*******************************************************************************
    ** Setter for headerFontFamily
    *******************************************************************************/
   public void setHeaderFontFamily(String headerFontFamily)
   {
      this.headerFontFamily = headerFontFamily;
   }



   /*******************************************************************************
    ** Fluent setter for headerFontFamily
    *******************************************************************************/
   public QThemeMetaData withHeaderFontFamily(String headerFontFamily)
   {
      this.headerFontFamily = headerFontFamily;
      return (this);
   }



   /*******************************************************************************
    ** Getter for borderRadius
    *******************************************************************************/
   public String getBorderRadius()
   {
      return (this.borderRadius);
   }



   /*******************************************************************************
    ** Setter for borderRadius
    *******************************************************************************/
   public void setBorderRadius(String borderRadius)
   {
      this.borderRadius = borderRadius;
   }



   /*******************************************************************************
    ** Fluent setter for borderRadius
    *******************************************************************************/
   public QThemeMetaData withBorderRadius(String borderRadius)
   {
      this.borderRadius = borderRadius;
      return (this);
   }



   /*******************************************************************************
    ** Getter for density
    *******************************************************************************/
   public String getDensity()
   {
      return (this.density);
   }



   /*******************************************************************************
    ** Setter for density
    *******************************************************************************/
   public void setDensity(String density)
   {
      this.density = density;
   }



   /*******************************************************************************
    ** Fluent setter for density
    *******************************************************************************/
   public QThemeMetaData withDensity(String density)
   {
      this.density = density;
      return (this);
   }



   /*******************************************************************************
    ** Getter for logoPath
    *******************************************************************************/
   public String getLogoPath()
   {
      return (this.logoPath);
   }



   /*******************************************************************************
    ** Setter for logoPath
    *******************************************************************************/
   public void setLogoPath(String logoPath)
   {
      this.logoPath = logoPath;
   }



   /*******************************************************************************
    ** Fluent setter for logoPath
    *******************************************************************************/
   public QThemeMetaData withLogoPath(String logoPath)
   {
      this.logoPath = logoPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for iconPath
    *******************************************************************************/
   public String getIconPath()
   {
      return (this.iconPath);
   }



   /*******************************************************************************
    ** Setter for iconPath
    *******************************************************************************/
   public void setIconPath(String iconPath)
   {
      this.iconPath = iconPath;
   }



   /*******************************************************************************
    ** Fluent setter for iconPath
    *******************************************************************************/
   public QThemeMetaData withIconPath(String iconPath)
   {
      this.iconPath = iconPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for faviconPath
    *******************************************************************************/
   public String getFaviconPath()
   {
      return (this.faviconPath);
   }



   /*******************************************************************************
    ** Setter for faviconPath
    *******************************************************************************/
   public void setFaviconPath(String faviconPath)
   {
      this.faviconPath = faviconPath;
   }



   /*******************************************************************************
    ** Fluent setter for faviconPath
    *******************************************************************************/
   public QThemeMetaData withFaviconPath(String faviconPath)
   {
      this.faviconPath = faviconPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for customCss
    *******************************************************************************/
   public String getCustomCss()
   {
      return (this.customCss);
   }



   /*******************************************************************************
    ** Setter for customCss
    *******************************************************************************/
   public void setCustomCss(String customCss)
   {
      this.customCss = customCss;
   }



   /*******************************************************************************
    ** Fluent setter for customCss
    *******************************************************************************/
   public QThemeMetaData withCustomCss(String customCss)
   {
      this.customCss = customCss;
      return (this);
   }

}
