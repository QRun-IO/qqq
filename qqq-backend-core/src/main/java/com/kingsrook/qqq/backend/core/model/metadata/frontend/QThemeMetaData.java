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

   //////////////////////
   // Icon style       //
   //////////////////////
   private String iconStyle;

   ////////////////////////////////
   // Branded Header Bar         //
   ////////////////////////////////
   private Boolean brandedHeaderEnabled;
   private String  brandedHeaderBackgroundColor;
   private String  brandedHeaderTextColor;
   private String  brandedHeaderLogoPath;
   private String  brandedHeaderLogoAltText;
   private String  brandedHeaderHeight;
   private String  brandedHeaderTagline;

   //////////////////////
   // App Bar          //
   //////////////////////
   private String appBarBackgroundColor;
   private String appBarTextColor;

   //////////////////////
   // Sidebar          //
   //////////////////////
   private String sidebarBackgroundColor;
   private String sidebarTextColor;
   private String sidebarIconColor;
   private String sidebarSelectedBackgroundColor;
   private String sidebarSelectedTextColor;
   private String sidebarHoverBackgroundColor;
   private String sidebarDividerColor;

   //////////////////////
   // Tables           //
   //////////////////////
   private String tableHeaderBackgroundColor;
   private String tableHeaderTextColor;
   private String tableRowHoverColor;
   private String tableRowSelectedColor;
   private String tableBorderColor;

   //////////////////////
   // General          //
   //////////////////////
   private String dividerColor;
   private String borderColor;
   private String cardBorderColor;

   ////////////////////////////////
   // Typography - Base          //
   ////////////////////////////////
   private String  monoFontFamily;
   private String  fontSizeBase;
   private Integer fontWeightLight;
   private Integer fontWeightRegular;
   private Integer fontWeightMedium;
   private Integer fontWeightBold;

   ////////////////////////////////
   // Typography - Variants      //
   ////////////////////////////////
   private String  typographyH1FontSize;
   private Integer typographyH1FontWeight;
   private String  typographyH1LineHeight;
   private String  typographyH1LetterSpacing;

   private String  typographyH2FontSize;
   private Integer typographyH2FontWeight;
   private String  typographyH2LineHeight;
   private String  typographyH2LetterSpacing;

   private String  typographyH3FontSize;
   private Integer typographyH3FontWeight;
   private String  typographyH3LineHeight;
   private String  typographyH3LetterSpacing;

   private String  typographyH4FontSize;
   private Integer typographyH4FontWeight;
   private String  typographyH4LineHeight;
   private String  typographyH4LetterSpacing;

   private String  typographyH5FontSize;
   private Integer typographyH5FontWeight;
   private String  typographyH5LineHeight;
   private String  typographyH5LetterSpacing;

   private String  typographyH6FontSize;
   private Integer typographyH6FontWeight;
   private String  typographyH6LineHeight;
   private String  typographyH6LetterSpacing;

   private String  typographyBody1FontSize;
   private Integer typographyBody1FontWeight;
   private String  typographyBody1LineHeight;
   private String  typographyBody1LetterSpacing;

   private String  typographyBody2FontSize;
   private Integer typographyBody2FontWeight;
   private String  typographyBody2LineHeight;
   private String  typographyBody2LetterSpacing;

   private String  typographyButtonFontSize;
   private Integer typographyButtonFontWeight;
   private String  typographyButtonLineHeight;
   private String  typographyButtonLetterSpacing;
   private String  typographyButtonTextTransform;

   private String  typographyCaptionFontSize;
   private Integer typographyCaptionFontWeight;
   private String  typographyCaptionLineHeight;
   private String  typographyCaptionLetterSpacing;



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
      ///////////////////////
      // Core palette      //
      ///////////////////////
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

      ///////////////////////
      // Branded header    //
      ///////////////////////
      validateColor("brandedHeaderBackgroundColor", brandedHeaderBackgroundColor);
      validateColor("brandedHeaderTextColor", brandedHeaderTextColor);

      ///////////////////////
      // App bar           //
      ///////////////////////
      validateColor("appBarBackgroundColor", appBarBackgroundColor);
      validateColor("appBarTextColor", appBarTextColor);

      ///////////////////////
      // Sidebar           //
      ///////////////////////
      validateColor("sidebarBackgroundColor", sidebarBackgroundColor);
      validateColor("sidebarTextColor", sidebarTextColor);
      validateColor("sidebarIconColor", sidebarIconColor);
      validateColor("sidebarSelectedBackgroundColor", sidebarSelectedBackgroundColor);
      validateColor("sidebarSelectedTextColor", sidebarSelectedTextColor);
      validateColor("sidebarHoverBackgroundColor", sidebarHoverBackgroundColor);
      validateColor("sidebarDividerColor", sidebarDividerColor);

      ///////////////////////
      // Tables            //
      ///////////////////////
      validateColor("tableHeaderBackgroundColor", tableHeaderBackgroundColor);
      validateColor("tableHeaderTextColor", tableHeaderTextColor);
      validateColor("tableRowHoverColor", tableRowHoverColor);
      validateColor("tableRowSelectedColor", tableRowSelectedColor);
      validateColor("tableBorderColor", tableBorderColor);

      ///////////////////////
      // General           //
      ///////////////////////
      validateColor("dividerColor", dividerColor);
      validateColor("borderColor", borderColor);
      validateColor("cardBorderColor", cardBorderColor);

      if(density != null && !density.equals("compact") && !density.equals("normal") && !density.equals("comfortable"))
      {
         LOG.warn("Invalid theme density value, should be 'compact', 'normal', or 'comfortable'", logPair("density", density));
      }

      if(iconStyle != null && !iconStyle.equals("filled") && !iconStyle.equals("outlined") && !iconStyle.equals("rounded") && !iconStyle.equals("sharp") && !iconStyle.equals("two-tone"))
      {
         LOG.warn("Invalid theme iconStyle value, should be 'filled', 'outlined', 'rounded', 'sharp', or 'two-tone'", logPair("iconStyle", iconStyle));
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



   /*******************************************************************************
    ** Getter for iconStyle
    *******************************************************************************/
   public String getIconStyle()
   {
      return (this.iconStyle);
   }



   /*******************************************************************************
    ** Setter for iconStyle
    *******************************************************************************/
   public void setIconStyle(String iconStyle)
   {
      this.iconStyle = iconStyle;
   }



   /*******************************************************************************
    ** Fluent setter for iconStyle
    *******************************************************************************/
   public QThemeMetaData withIconStyle(String iconStyle)
   {
      this.iconStyle = iconStyle;
      return (this);
   }



   /*******************************************************************************
    ** Getter for brandedHeaderEnabled
    *******************************************************************************/
   public Boolean getBrandedHeaderEnabled()
   {
      return (this.brandedHeaderEnabled);
   }



   /*******************************************************************************
    ** Setter for brandedHeaderEnabled
    *******************************************************************************/
   public void setBrandedHeaderEnabled(Boolean brandedHeaderEnabled)
   {
      this.brandedHeaderEnabled = brandedHeaderEnabled;
   }



   /*******************************************************************************
    ** Fluent setter for brandedHeaderEnabled
    *******************************************************************************/
   public QThemeMetaData withBrandedHeaderEnabled(Boolean brandedHeaderEnabled)
   {
      this.brandedHeaderEnabled = brandedHeaderEnabled;
      return (this);
   }



   /*******************************************************************************
    ** Getter for brandedHeaderBackgroundColor
    *******************************************************************************/
   public String getBrandedHeaderBackgroundColor()
   {
      return (this.brandedHeaderBackgroundColor);
   }



   /*******************************************************************************
    ** Setter for brandedHeaderBackgroundColor
    *******************************************************************************/
   public void setBrandedHeaderBackgroundColor(String brandedHeaderBackgroundColor)
   {
      this.brandedHeaderBackgroundColor = brandedHeaderBackgroundColor;
   }



   /*******************************************************************************
    ** Fluent setter for brandedHeaderBackgroundColor
    *******************************************************************************/
   public QThemeMetaData withBrandedHeaderBackgroundColor(String brandedHeaderBackgroundColor)
   {
      this.brandedHeaderBackgroundColor = brandedHeaderBackgroundColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for brandedHeaderTextColor
    *******************************************************************************/
   public String getBrandedHeaderTextColor()
   {
      return (this.brandedHeaderTextColor);
   }



   /*******************************************************************************
    ** Setter for brandedHeaderTextColor
    *******************************************************************************/
   public void setBrandedHeaderTextColor(String brandedHeaderTextColor)
   {
      this.brandedHeaderTextColor = brandedHeaderTextColor;
   }



   /*******************************************************************************
    ** Fluent setter for brandedHeaderTextColor
    *******************************************************************************/
   public QThemeMetaData withBrandedHeaderTextColor(String brandedHeaderTextColor)
   {
      this.brandedHeaderTextColor = brandedHeaderTextColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for brandedHeaderLogoPath
    *******************************************************************************/
   public String getBrandedHeaderLogoPath()
   {
      return (this.brandedHeaderLogoPath);
   }



   /*******************************************************************************
    ** Setter for brandedHeaderLogoPath
    *******************************************************************************/
   public void setBrandedHeaderLogoPath(String brandedHeaderLogoPath)
   {
      this.brandedHeaderLogoPath = brandedHeaderLogoPath;
   }



   /*******************************************************************************
    ** Fluent setter for brandedHeaderLogoPath
    *******************************************************************************/
   public QThemeMetaData withBrandedHeaderLogoPath(String brandedHeaderLogoPath)
   {
      this.brandedHeaderLogoPath = brandedHeaderLogoPath;
      return (this);
   }



   /*******************************************************************************
    ** Getter for brandedHeaderLogoAltText
    *******************************************************************************/
   public String getBrandedHeaderLogoAltText()
   {
      return (this.brandedHeaderLogoAltText);
   }



   /*******************************************************************************
    ** Setter for brandedHeaderLogoAltText
    *******************************************************************************/
   public void setBrandedHeaderLogoAltText(String brandedHeaderLogoAltText)
   {
      this.brandedHeaderLogoAltText = brandedHeaderLogoAltText;
   }



   /*******************************************************************************
    ** Fluent setter for brandedHeaderLogoAltText
    *******************************************************************************/
   public QThemeMetaData withBrandedHeaderLogoAltText(String brandedHeaderLogoAltText)
   {
      this.brandedHeaderLogoAltText = brandedHeaderLogoAltText;
      return (this);
   }



   /*******************************************************************************
    ** Getter for brandedHeaderHeight
    *******************************************************************************/
   public String getBrandedHeaderHeight()
   {
      return (this.brandedHeaderHeight);
   }



   /*******************************************************************************
    ** Setter for brandedHeaderHeight
    *******************************************************************************/
   public void setBrandedHeaderHeight(String brandedHeaderHeight)
   {
      this.brandedHeaderHeight = brandedHeaderHeight;
   }



   /*******************************************************************************
    ** Fluent setter for brandedHeaderHeight
    *******************************************************************************/
   public QThemeMetaData withBrandedHeaderHeight(String brandedHeaderHeight)
   {
      this.brandedHeaderHeight = brandedHeaderHeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for brandedHeaderTagline
    *******************************************************************************/
   public String getBrandedHeaderTagline()
   {
      return (this.brandedHeaderTagline);
   }



   /*******************************************************************************
    ** Setter for brandedHeaderTagline
    *******************************************************************************/
   public void setBrandedHeaderTagline(String brandedHeaderTagline)
   {
      this.brandedHeaderTagline = brandedHeaderTagline;
   }



   /*******************************************************************************
    ** Fluent setter for brandedHeaderTagline
    *******************************************************************************/
   public QThemeMetaData withBrandedHeaderTagline(String brandedHeaderTagline)
   {
      this.brandedHeaderTagline = brandedHeaderTagline;
      return (this);
   }



   /*******************************************************************************
    ** Getter for appBarBackgroundColor
    *******************************************************************************/
   public String getAppBarBackgroundColor()
   {
      return (this.appBarBackgroundColor);
   }



   /*******************************************************************************
    ** Setter for appBarBackgroundColor
    *******************************************************************************/
   public void setAppBarBackgroundColor(String appBarBackgroundColor)
   {
      this.appBarBackgroundColor = appBarBackgroundColor;
   }



   /*******************************************************************************
    ** Fluent setter for appBarBackgroundColor
    *******************************************************************************/
   public QThemeMetaData withAppBarBackgroundColor(String appBarBackgroundColor)
   {
      this.appBarBackgroundColor = appBarBackgroundColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for appBarTextColor
    *******************************************************************************/
   public String getAppBarTextColor()
   {
      return (this.appBarTextColor);
   }



   /*******************************************************************************
    ** Setter for appBarTextColor
    *******************************************************************************/
   public void setAppBarTextColor(String appBarTextColor)
   {
      this.appBarTextColor = appBarTextColor;
   }



   /*******************************************************************************
    ** Fluent setter for appBarTextColor
    *******************************************************************************/
   public QThemeMetaData withAppBarTextColor(String appBarTextColor)
   {
      this.appBarTextColor = appBarTextColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sidebarBackgroundColor
    *******************************************************************************/
   public String getSidebarBackgroundColor()
   {
      return (this.sidebarBackgroundColor);
   }



   /*******************************************************************************
    ** Setter for sidebarBackgroundColor
    *******************************************************************************/
   public void setSidebarBackgroundColor(String sidebarBackgroundColor)
   {
      this.sidebarBackgroundColor = sidebarBackgroundColor;
   }



   /*******************************************************************************
    ** Fluent setter for sidebarBackgroundColor
    *******************************************************************************/
   public QThemeMetaData withSidebarBackgroundColor(String sidebarBackgroundColor)
   {
      this.sidebarBackgroundColor = sidebarBackgroundColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sidebarTextColor
    *******************************************************************************/
   public String getSidebarTextColor()
   {
      return (this.sidebarTextColor);
   }



   /*******************************************************************************
    ** Setter for sidebarTextColor
    *******************************************************************************/
   public void setSidebarTextColor(String sidebarTextColor)
   {
      this.sidebarTextColor = sidebarTextColor;
   }



   /*******************************************************************************
    ** Fluent setter for sidebarTextColor
    *******************************************************************************/
   public QThemeMetaData withSidebarTextColor(String sidebarTextColor)
   {
      this.sidebarTextColor = sidebarTextColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sidebarIconColor
    *******************************************************************************/
   public String getSidebarIconColor()
   {
      return (this.sidebarIconColor);
   }



   /*******************************************************************************
    ** Setter for sidebarIconColor
    *******************************************************************************/
   public void setSidebarIconColor(String sidebarIconColor)
   {
      this.sidebarIconColor = sidebarIconColor;
   }



   /*******************************************************************************
    ** Fluent setter for sidebarIconColor
    *******************************************************************************/
   public QThemeMetaData withSidebarIconColor(String sidebarIconColor)
   {
      this.sidebarIconColor = sidebarIconColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sidebarSelectedBackgroundColor
    *******************************************************************************/
   public String getSidebarSelectedBackgroundColor()
   {
      return (this.sidebarSelectedBackgroundColor);
   }



   /*******************************************************************************
    ** Setter for sidebarSelectedBackgroundColor
    *******************************************************************************/
   public void setSidebarSelectedBackgroundColor(String sidebarSelectedBackgroundColor)
   {
      this.sidebarSelectedBackgroundColor = sidebarSelectedBackgroundColor;
   }



   /*******************************************************************************
    ** Fluent setter for sidebarSelectedBackgroundColor
    *******************************************************************************/
   public QThemeMetaData withSidebarSelectedBackgroundColor(String sidebarSelectedBackgroundColor)
   {
      this.sidebarSelectedBackgroundColor = sidebarSelectedBackgroundColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sidebarSelectedTextColor
    *******************************************************************************/
   public String getSidebarSelectedTextColor()
   {
      return (this.sidebarSelectedTextColor);
   }



   /*******************************************************************************
    ** Setter for sidebarSelectedTextColor
    *******************************************************************************/
   public void setSidebarSelectedTextColor(String sidebarSelectedTextColor)
   {
      this.sidebarSelectedTextColor = sidebarSelectedTextColor;
   }



   /*******************************************************************************
    ** Fluent setter for sidebarSelectedTextColor
    *******************************************************************************/
   public QThemeMetaData withSidebarSelectedTextColor(String sidebarSelectedTextColor)
   {
      this.sidebarSelectedTextColor = sidebarSelectedTextColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sidebarHoverBackgroundColor
    *******************************************************************************/
   public String getSidebarHoverBackgroundColor()
   {
      return (this.sidebarHoverBackgroundColor);
   }



   /*******************************************************************************
    ** Setter for sidebarHoverBackgroundColor
    *******************************************************************************/
   public void setSidebarHoverBackgroundColor(String sidebarHoverBackgroundColor)
   {
      this.sidebarHoverBackgroundColor = sidebarHoverBackgroundColor;
   }



   /*******************************************************************************
    ** Fluent setter for sidebarHoverBackgroundColor
    *******************************************************************************/
   public QThemeMetaData withSidebarHoverBackgroundColor(String sidebarHoverBackgroundColor)
   {
      this.sidebarHoverBackgroundColor = sidebarHoverBackgroundColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for sidebarDividerColor
    *******************************************************************************/
   public String getSidebarDividerColor()
   {
      return (this.sidebarDividerColor);
   }



   /*******************************************************************************
    ** Setter for sidebarDividerColor
    *******************************************************************************/
   public void setSidebarDividerColor(String sidebarDividerColor)
   {
      this.sidebarDividerColor = sidebarDividerColor;
   }



   /*******************************************************************************
    ** Fluent setter for sidebarDividerColor
    *******************************************************************************/
   public QThemeMetaData withSidebarDividerColor(String sidebarDividerColor)
   {
      this.sidebarDividerColor = sidebarDividerColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableHeaderBackgroundColor
    *******************************************************************************/
   public String getTableHeaderBackgroundColor()
   {
      return (this.tableHeaderBackgroundColor);
   }



   /*******************************************************************************
    ** Setter for tableHeaderBackgroundColor
    *******************************************************************************/
   public void setTableHeaderBackgroundColor(String tableHeaderBackgroundColor)
   {
      this.tableHeaderBackgroundColor = tableHeaderBackgroundColor;
   }



   /*******************************************************************************
    ** Fluent setter for tableHeaderBackgroundColor
    *******************************************************************************/
   public QThemeMetaData withTableHeaderBackgroundColor(String tableHeaderBackgroundColor)
   {
      this.tableHeaderBackgroundColor = tableHeaderBackgroundColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableHeaderTextColor
    *******************************************************************************/
   public String getTableHeaderTextColor()
   {
      return (this.tableHeaderTextColor);
   }



   /*******************************************************************************
    ** Setter for tableHeaderTextColor
    *******************************************************************************/
   public void setTableHeaderTextColor(String tableHeaderTextColor)
   {
      this.tableHeaderTextColor = tableHeaderTextColor;
   }



   /*******************************************************************************
    ** Fluent setter for tableHeaderTextColor
    *******************************************************************************/
   public QThemeMetaData withTableHeaderTextColor(String tableHeaderTextColor)
   {
      this.tableHeaderTextColor = tableHeaderTextColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableRowHoverColor
    *******************************************************************************/
   public String getTableRowHoverColor()
   {
      return (this.tableRowHoverColor);
   }



   /*******************************************************************************
    ** Setter for tableRowHoverColor
    *******************************************************************************/
   public void setTableRowHoverColor(String tableRowHoverColor)
   {
      this.tableRowHoverColor = tableRowHoverColor;
   }



   /*******************************************************************************
    ** Fluent setter for tableRowHoverColor
    *******************************************************************************/
   public QThemeMetaData withTableRowHoverColor(String tableRowHoverColor)
   {
      this.tableRowHoverColor = tableRowHoverColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableRowSelectedColor
    *******************************************************************************/
   public String getTableRowSelectedColor()
   {
      return (this.tableRowSelectedColor);
   }



   /*******************************************************************************
    ** Setter for tableRowSelectedColor
    *******************************************************************************/
   public void setTableRowSelectedColor(String tableRowSelectedColor)
   {
      this.tableRowSelectedColor = tableRowSelectedColor;
   }



   /*******************************************************************************
    ** Fluent setter for tableRowSelectedColor
    *******************************************************************************/
   public QThemeMetaData withTableRowSelectedColor(String tableRowSelectedColor)
   {
      this.tableRowSelectedColor = tableRowSelectedColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableBorderColor
    *******************************************************************************/
   public String getTableBorderColor()
   {
      return (this.tableBorderColor);
   }



   /*******************************************************************************
    ** Setter for tableBorderColor
    *******************************************************************************/
   public void setTableBorderColor(String tableBorderColor)
   {
      this.tableBorderColor = tableBorderColor;
   }



   /*******************************************************************************
    ** Fluent setter for tableBorderColor
    *******************************************************************************/
   public QThemeMetaData withTableBorderColor(String tableBorderColor)
   {
      this.tableBorderColor = tableBorderColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for dividerColor
    *******************************************************************************/
   public String getDividerColor()
   {
      return (this.dividerColor);
   }



   /*******************************************************************************
    ** Setter for dividerColor
    *******************************************************************************/
   public void setDividerColor(String dividerColor)
   {
      this.dividerColor = dividerColor;
   }



   /*******************************************************************************
    ** Fluent setter for dividerColor
    *******************************************************************************/
   public QThemeMetaData withDividerColor(String dividerColor)
   {
      this.dividerColor = dividerColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for borderColor
    *******************************************************************************/
   public String getBorderColor()
   {
      return (this.borderColor);
   }



   /*******************************************************************************
    ** Setter for borderColor
    *******************************************************************************/
   public void setBorderColor(String borderColor)
   {
      this.borderColor = borderColor;
   }



   /*******************************************************************************
    ** Fluent setter for borderColor
    *******************************************************************************/
   public QThemeMetaData withBorderColor(String borderColor)
   {
      this.borderColor = borderColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for cardBorderColor
    *******************************************************************************/
   public String getCardBorderColor()
   {
      return (this.cardBorderColor);
   }



   /*******************************************************************************
    ** Setter for cardBorderColor
    *******************************************************************************/
   public void setCardBorderColor(String cardBorderColor)
   {
      this.cardBorderColor = cardBorderColor;
   }



   /*******************************************************************************
    ** Fluent setter for cardBorderColor
    *******************************************************************************/
   public QThemeMetaData withCardBorderColor(String cardBorderColor)
   {
      this.cardBorderColor = cardBorderColor;
      return (this);
   }



   /*******************************************************************************
    ** Getter for monoFontFamily
    *******************************************************************************/
   public String getMonoFontFamily()
   {
      return (this.monoFontFamily);
   }



   /*******************************************************************************
    ** Setter for monoFontFamily
    *******************************************************************************/
   public void setMonoFontFamily(String monoFontFamily)
   {
      this.monoFontFamily = monoFontFamily;
   }



   /*******************************************************************************
    ** Fluent setter for monoFontFamily
    *******************************************************************************/
   public QThemeMetaData withMonoFontFamily(String monoFontFamily)
   {
      this.monoFontFamily = monoFontFamily;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fontSizeBase
    *******************************************************************************/
   public String getFontSizeBase()
   {
      return (this.fontSizeBase);
   }



   /*******************************************************************************
    ** Setter for fontSizeBase
    *******************************************************************************/
   public void setFontSizeBase(String fontSizeBase)
   {
      this.fontSizeBase = fontSizeBase;
   }



   /*******************************************************************************
    ** Fluent setter for fontSizeBase
    *******************************************************************************/
   public QThemeMetaData withFontSizeBase(String fontSizeBase)
   {
      this.fontSizeBase = fontSizeBase;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fontWeightLight
    *******************************************************************************/
   public Integer getFontWeightLight()
   {
      return (this.fontWeightLight);
   }



   /*******************************************************************************
    ** Setter for fontWeightLight
    *******************************************************************************/
   public void setFontWeightLight(Integer fontWeightLight)
   {
      this.fontWeightLight = fontWeightLight;
   }



   /*******************************************************************************
    ** Fluent setter for fontWeightLight
    *******************************************************************************/
   public QThemeMetaData withFontWeightLight(Integer fontWeightLight)
   {
      this.fontWeightLight = fontWeightLight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fontWeightRegular
    *******************************************************************************/
   public Integer getFontWeightRegular()
   {
      return (this.fontWeightRegular);
   }



   /*******************************************************************************
    ** Setter for fontWeightRegular
    *******************************************************************************/
   public void setFontWeightRegular(Integer fontWeightRegular)
   {
      this.fontWeightRegular = fontWeightRegular;
   }



   /*******************************************************************************
    ** Fluent setter for fontWeightRegular
    *******************************************************************************/
   public QThemeMetaData withFontWeightRegular(Integer fontWeightRegular)
   {
      this.fontWeightRegular = fontWeightRegular;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fontWeightMedium
    *******************************************************************************/
   public Integer getFontWeightMedium()
   {
      return (this.fontWeightMedium);
   }



   /*******************************************************************************
    ** Setter for fontWeightMedium
    *******************************************************************************/
   public void setFontWeightMedium(Integer fontWeightMedium)
   {
      this.fontWeightMedium = fontWeightMedium;
   }



   /*******************************************************************************
    ** Fluent setter for fontWeightMedium
    *******************************************************************************/
   public QThemeMetaData withFontWeightMedium(Integer fontWeightMedium)
   {
      this.fontWeightMedium = fontWeightMedium;
      return (this);
   }



   /*******************************************************************************
    ** Getter for fontWeightBold
    *******************************************************************************/
   public Integer getFontWeightBold()
   {
      return (this.fontWeightBold);
   }



   /*******************************************************************************
    ** Setter for fontWeightBold
    *******************************************************************************/
   public void setFontWeightBold(Integer fontWeightBold)
   {
      this.fontWeightBold = fontWeightBold;
   }



   /*******************************************************************************
    ** Fluent setter for fontWeightBold
    *******************************************************************************/
   public QThemeMetaData withFontWeightBold(Integer fontWeightBold)
   {
      this.fontWeightBold = fontWeightBold;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH1FontSize
    *******************************************************************************/
   public String getTypographyH1FontSize()
   {
      return (this.typographyH1FontSize);
   }



   /*******************************************************************************
    ** Setter for typographyH1FontSize
    *******************************************************************************/
   public void setTypographyH1FontSize(String typographyH1FontSize)
   {
      this.typographyH1FontSize = typographyH1FontSize;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH1FontSize
    *******************************************************************************/
   public QThemeMetaData withTypographyH1FontSize(String typographyH1FontSize)
   {
      this.typographyH1FontSize = typographyH1FontSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH1FontWeight
    *******************************************************************************/
   public Integer getTypographyH1FontWeight()
   {
      return (this.typographyH1FontWeight);
   }



   /*******************************************************************************
    ** Setter for typographyH1FontWeight
    *******************************************************************************/
   public void setTypographyH1FontWeight(Integer typographyH1FontWeight)
   {
      this.typographyH1FontWeight = typographyH1FontWeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH1FontWeight
    *******************************************************************************/
   public QThemeMetaData withTypographyH1FontWeight(Integer typographyH1FontWeight)
   {
      this.typographyH1FontWeight = typographyH1FontWeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH1LineHeight
    *******************************************************************************/
   public String getTypographyH1LineHeight()
   {
      return (this.typographyH1LineHeight);
   }



   /*******************************************************************************
    ** Setter for typographyH1LineHeight
    *******************************************************************************/
   public void setTypographyH1LineHeight(String typographyH1LineHeight)
   {
      this.typographyH1LineHeight = typographyH1LineHeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH1LineHeight
    *******************************************************************************/
   public QThemeMetaData withTypographyH1LineHeight(String typographyH1LineHeight)
   {
      this.typographyH1LineHeight = typographyH1LineHeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH1LetterSpacing
    *******************************************************************************/
   public String getTypographyH1LetterSpacing()
   {
      return (this.typographyH1LetterSpacing);
   }



   /*******************************************************************************
    ** Setter for typographyH1LetterSpacing
    *******************************************************************************/
   public void setTypographyH1LetterSpacing(String typographyH1LetterSpacing)
   {
      this.typographyH1LetterSpacing = typographyH1LetterSpacing;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH1LetterSpacing
    *******************************************************************************/
   public QThemeMetaData withTypographyH1LetterSpacing(String typographyH1LetterSpacing)
   {
      this.typographyH1LetterSpacing = typographyH1LetterSpacing;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH2FontSize
    *******************************************************************************/
   public String getTypographyH2FontSize()
   {
      return (this.typographyH2FontSize);
   }



   /*******************************************************************************
    ** Setter for typographyH2FontSize
    *******************************************************************************/
   public void setTypographyH2FontSize(String typographyH2FontSize)
   {
      this.typographyH2FontSize = typographyH2FontSize;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH2FontSize
    *******************************************************************************/
   public QThemeMetaData withTypographyH2FontSize(String typographyH2FontSize)
   {
      this.typographyH2FontSize = typographyH2FontSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH2FontWeight
    *******************************************************************************/
   public Integer getTypographyH2FontWeight()
   {
      return (this.typographyH2FontWeight);
   }



   /*******************************************************************************
    ** Setter for typographyH2FontWeight
    *******************************************************************************/
   public void setTypographyH2FontWeight(Integer typographyH2FontWeight)
   {
      this.typographyH2FontWeight = typographyH2FontWeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH2FontWeight
    *******************************************************************************/
   public QThemeMetaData withTypographyH2FontWeight(Integer typographyH2FontWeight)
   {
      this.typographyH2FontWeight = typographyH2FontWeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH2LineHeight
    *******************************************************************************/
   public String getTypographyH2LineHeight()
   {
      return (this.typographyH2LineHeight);
   }



   /*******************************************************************************
    ** Setter for typographyH2LineHeight
    *******************************************************************************/
   public void setTypographyH2LineHeight(String typographyH2LineHeight)
   {
      this.typographyH2LineHeight = typographyH2LineHeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH2LineHeight
    *******************************************************************************/
   public QThemeMetaData withTypographyH2LineHeight(String typographyH2LineHeight)
   {
      this.typographyH2LineHeight = typographyH2LineHeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH2LetterSpacing
    *******************************************************************************/
   public String getTypographyH2LetterSpacing()
   {
      return (this.typographyH2LetterSpacing);
   }



   /*******************************************************************************
    ** Setter for typographyH2LetterSpacing
    *******************************************************************************/
   public void setTypographyH2LetterSpacing(String typographyH2LetterSpacing)
   {
      this.typographyH2LetterSpacing = typographyH2LetterSpacing;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH2LetterSpacing
    *******************************************************************************/
   public QThemeMetaData withTypographyH2LetterSpacing(String typographyH2LetterSpacing)
   {
      this.typographyH2LetterSpacing = typographyH2LetterSpacing;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH3FontSize
    *******************************************************************************/
   public String getTypographyH3FontSize()
   {
      return (this.typographyH3FontSize);
   }



   /*******************************************************************************
    ** Setter for typographyH3FontSize
    *******************************************************************************/
   public void setTypographyH3FontSize(String typographyH3FontSize)
   {
      this.typographyH3FontSize = typographyH3FontSize;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH3FontSize
    *******************************************************************************/
   public QThemeMetaData withTypographyH3FontSize(String typographyH3FontSize)
   {
      this.typographyH3FontSize = typographyH3FontSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH3FontWeight
    *******************************************************************************/
   public Integer getTypographyH3FontWeight()
   {
      return (this.typographyH3FontWeight);
   }



   /*******************************************************************************
    ** Setter for typographyH3FontWeight
    *******************************************************************************/
   public void setTypographyH3FontWeight(Integer typographyH3FontWeight)
   {
      this.typographyH3FontWeight = typographyH3FontWeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH3FontWeight
    *******************************************************************************/
   public QThemeMetaData withTypographyH3FontWeight(Integer typographyH3FontWeight)
   {
      this.typographyH3FontWeight = typographyH3FontWeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH3LineHeight
    *******************************************************************************/
   public String getTypographyH3LineHeight()
   {
      return (this.typographyH3LineHeight);
   }



   /*******************************************************************************
    ** Setter for typographyH3LineHeight
    *******************************************************************************/
   public void setTypographyH3LineHeight(String typographyH3LineHeight)
   {
      this.typographyH3LineHeight = typographyH3LineHeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH3LineHeight
    *******************************************************************************/
   public QThemeMetaData withTypographyH3LineHeight(String typographyH3LineHeight)
   {
      this.typographyH3LineHeight = typographyH3LineHeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH3LetterSpacing
    *******************************************************************************/
   public String getTypographyH3LetterSpacing()
   {
      return (this.typographyH3LetterSpacing);
   }



   /*******************************************************************************
    ** Setter for typographyH3LetterSpacing
    *******************************************************************************/
   public void setTypographyH3LetterSpacing(String typographyH3LetterSpacing)
   {
      this.typographyH3LetterSpacing = typographyH3LetterSpacing;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH3LetterSpacing
    *******************************************************************************/
   public QThemeMetaData withTypographyH3LetterSpacing(String typographyH3LetterSpacing)
   {
      this.typographyH3LetterSpacing = typographyH3LetterSpacing;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH4FontSize
    *******************************************************************************/
   public String getTypographyH4FontSize()
   {
      return (this.typographyH4FontSize);
   }



   /*******************************************************************************
    ** Setter for typographyH4FontSize
    *******************************************************************************/
   public void setTypographyH4FontSize(String typographyH4FontSize)
   {
      this.typographyH4FontSize = typographyH4FontSize;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH4FontSize
    *******************************************************************************/
   public QThemeMetaData withTypographyH4FontSize(String typographyH4FontSize)
   {
      this.typographyH4FontSize = typographyH4FontSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH4FontWeight
    *******************************************************************************/
   public Integer getTypographyH4FontWeight()
   {
      return (this.typographyH4FontWeight);
   }



   /*******************************************************************************
    ** Setter for typographyH4FontWeight
    *******************************************************************************/
   public void setTypographyH4FontWeight(Integer typographyH4FontWeight)
   {
      this.typographyH4FontWeight = typographyH4FontWeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH4FontWeight
    *******************************************************************************/
   public QThemeMetaData withTypographyH4FontWeight(Integer typographyH4FontWeight)
   {
      this.typographyH4FontWeight = typographyH4FontWeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH4LineHeight
    *******************************************************************************/
   public String getTypographyH4LineHeight()
   {
      return (this.typographyH4LineHeight);
   }



   /*******************************************************************************
    ** Setter for typographyH4LineHeight
    *******************************************************************************/
   public void setTypographyH4LineHeight(String typographyH4LineHeight)
   {
      this.typographyH4LineHeight = typographyH4LineHeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH4LineHeight
    *******************************************************************************/
   public QThemeMetaData withTypographyH4LineHeight(String typographyH4LineHeight)
   {
      this.typographyH4LineHeight = typographyH4LineHeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH4LetterSpacing
    *******************************************************************************/
   public String getTypographyH4LetterSpacing()
   {
      return (this.typographyH4LetterSpacing);
   }



   /*******************************************************************************
    ** Setter for typographyH4LetterSpacing
    *******************************************************************************/
   public void setTypographyH4LetterSpacing(String typographyH4LetterSpacing)
   {
      this.typographyH4LetterSpacing = typographyH4LetterSpacing;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH4LetterSpacing
    *******************************************************************************/
   public QThemeMetaData withTypographyH4LetterSpacing(String typographyH4LetterSpacing)
   {
      this.typographyH4LetterSpacing = typographyH4LetterSpacing;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH5FontSize
    *******************************************************************************/
   public String getTypographyH5FontSize()
   {
      return (this.typographyH5FontSize);
   }



   /*******************************************************************************
    ** Setter for typographyH5FontSize
    *******************************************************************************/
   public void setTypographyH5FontSize(String typographyH5FontSize)
   {
      this.typographyH5FontSize = typographyH5FontSize;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH5FontSize
    *******************************************************************************/
   public QThemeMetaData withTypographyH5FontSize(String typographyH5FontSize)
   {
      this.typographyH5FontSize = typographyH5FontSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH5FontWeight
    *******************************************************************************/
   public Integer getTypographyH5FontWeight()
   {
      return (this.typographyH5FontWeight);
   }



   /*******************************************************************************
    ** Setter for typographyH5FontWeight
    *******************************************************************************/
   public void setTypographyH5FontWeight(Integer typographyH5FontWeight)
   {
      this.typographyH5FontWeight = typographyH5FontWeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH5FontWeight
    *******************************************************************************/
   public QThemeMetaData withTypographyH5FontWeight(Integer typographyH5FontWeight)
   {
      this.typographyH5FontWeight = typographyH5FontWeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH5LineHeight
    *******************************************************************************/
   public String getTypographyH5LineHeight()
   {
      return (this.typographyH5LineHeight);
   }



   /*******************************************************************************
    ** Setter for typographyH5LineHeight
    *******************************************************************************/
   public void setTypographyH5LineHeight(String typographyH5LineHeight)
   {
      this.typographyH5LineHeight = typographyH5LineHeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH5LineHeight
    *******************************************************************************/
   public QThemeMetaData withTypographyH5LineHeight(String typographyH5LineHeight)
   {
      this.typographyH5LineHeight = typographyH5LineHeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH5LetterSpacing
    *******************************************************************************/
   public String getTypographyH5LetterSpacing()
   {
      return (this.typographyH5LetterSpacing);
   }



   /*******************************************************************************
    ** Setter for typographyH5LetterSpacing
    *******************************************************************************/
   public void setTypographyH5LetterSpacing(String typographyH5LetterSpacing)
   {
      this.typographyH5LetterSpacing = typographyH5LetterSpacing;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH5LetterSpacing
    *******************************************************************************/
   public QThemeMetaData withTypographyH5LetterSpacing(String typographyH5LetterSpacing)
   {
      this.typographyH5LetterSpacing = typographyH5LetterSpacing;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH6FontSize
    *******************************************************************************/
   public String getTypographyH6FontSize()
   {
      return (this.typographyH6FontSize);
   }



   /*******************************************************************************
    ** Setter for typographyH6FontSize
    *******************************************************************************/
   public void setTypographyH6FontSize(String typographyH6FontSize)
   {
      this.typographyH6FontSize = typographyH6FontSize;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH6FontSize
    *******************************************************************************/
   public QThemeMetaData withTypographyH6FontSize(String typographyH6FontSize)
   {
      this.typographyH6FontSize = typographyH6FontSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH6FontWeight
    *******************************************************************************/
   public Integer getTypographyH6FontWeight()
   {
      return (this.typographyH6FontWeight);
   }



   /*******************************************************************************
    ** Setter for typographyH6FontWeight
    *******************************************************************************/
   public void setTypographyH6FontWeight(Integer typographyH6FontWeight)
   {
      this.typographyH6FontWeight = typographyH6FontWeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH6FontWeight
    *******************************************************************************/
   public QThemeMetaData withTypographyH6FontWeight(Integer typographyH6FontWeight)
   {
      this.typographyH6FontWeight = typographyH6FontWeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH6LineHeight
    *******************************************************************************/
   public String getTypographyH6LineHeight()
   {
      return (this.typographyH6LineHeight);
   }



   /*******************************************************************************
    ** Setter for typographyH6LineHeight
    *******************************************************************************/
   public void setTypographyH6LineHeight(String typographyH6LineHeight)
   {
      this.typographyH6LineHeight = typographyH6LineHeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH6LineHeight
    *******************************************************************************/
   public QThemeMetaData withTypographyH6LineHeight(String typographyH6LineHeight)
   {
      this.typographyH6LineHeight = typographyH6LineHeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyH6LetterSpacing
    *******************************************************************************/
   public String getTypographyH6LetterSpacing()
   {
      return (this.typographyH6LetterSpacing);
   }



   /*******************************************************************************
    ** Setter for typographyH6LetterSpacing
    *******************************************************************************/
   public void setTypographyH6LetterSpacing(String typographyH6LetterSpacing)
   {
      this.typographyH6LetterSpacing = typographyH6LetterSpacing;
   }



   /*******************************************************************************
    ** Fluent setter for typographyH6LetterSpacing
    *******************************************************************************/
   public QThemeMetaData withTypographyH6LetterSpacing(String typographyH6LetterSpacing)
   {
      this.typographyH6LetterSpacing = typographyH6LetterSpacing;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyBody1FontSize
    *******************************************************************************/
   public String getTypographyBody1FontSize()
   {
      return (this.typographyBody1FontSize);
   }



   /*******************************************************************************
    ** Setter for typographyBody1FontSize
    *******************************************************************************/
   public void setTypographyBody1FontSize(String typographyBody1FontSize)
   {
      this.typographyBody1FontSize = typographyBody1FontSize;
   }



   /*******************************************************************************
    ** Fluent setter for typographyBody1FontSize
    *******************************************************************************/
   public QThemeMetaData withTypographyBody1FontSize(String typographyBody1FontSize)
   {
      this.typographyBody1FontSize = typographyBody1FontSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyBody1FontWeight
    *******************************************************************************/
   public Integer getTypographyBody1FontWeight()
   {
      return (this.typographyBody1FontWeight);
   }



   /*******************************************************************************
    ** Setter for typographyBody1FontWeight
    *******************************************************************************/
   public void setTypographyBody1FontWeight(Integer typographyBody1FontWeight)
   {
      this.typographyBody1FontWeight = typographyBody1FontWeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyBody1FontWeight
    *******************************************************************************/
   public QThemeMetaData withTypographyBody1FontWeight(Integer typographyBody1FontWeight)
   {
      this.typographyBody1FontWeight = typographyBody1FontWeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyBody1LineHeight
    *******************************************************************************/
   public String getTypographyBody1LineHeight()
   {
      return (this.typographyBody1LineHeight);
   }



   /*******************************************************************************
    ** Setter for typographyBody1LineHeight
    *******************************************************************************/
   public void setTypographyBody1LineHeight(String typographyBody1LineHeight)
   {
      this.typographyBody1LineHeight = typographyBody1LineHeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyBody1LineHeight
    *******************************************************************************/
   public QThemeMetaData withTypographyBody1LineHeight(String typographyBody1LineHeight)
   {
      this.typographyBody1LineHeight = typographyBody1LineHeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyBody1LetterSpacing
    *******************************************************************************/
   public String getTypographyBody1LetterSpacing()
   {
      return (this.typographyBody1LetterSpacing);
   }



   /*******************************************************************************
    ** Setter for typographyBody1LetterSpacing
    *******************************************************************************/
   public void setTypographyBody1LetterSpacing(String typographyBody1LetterSpacing)
   {
      this.typographyBody1LetterSpacing = typographyBody1LetterSpacing;
   }



   /*******************************************************************************
    ** Fluent setter for typographyBody1LetterSpacing
    *******************************************************************************/
   public QThemeMetaData withTypographyBody1LetterSpacing(String typographyBody1LetterSpacing)
   {
      this.typographyBody1LetterSpacing = typographyBody1LetterSpacing;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyBody2FontSize
    *******************************************************************************/
   public String getTypographyBody2FontSize()
   {
      return (this.typographyBody2FontSize);
   }



   /*******************************************************************************
    ** Setter for typographyBody2FontSize
    *******************************************************************************/
   public void setTypographyBody2FontSize(String typographyBody2FontSize)
   {
      this.typographyBody2FontSize = typographyBody2FontSize;
   }



   /*******************************************************************************
    ** Fluent setter for typographyBody2FontSize
    *******************************************************************************/
   public QThemeMetaData withTypographyBody2FontSize(String typographyBody2FontSize)
   {
      this.typographyBody2FontSize = typographyBody2FontSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyBody2FontWeight
    *******************************************************************************/
   public Integer getTypographyBody2FontWeight()
   {
      return (this.typographyBody2FontWeight);
   }



   /*******************************************************************************
    ** Setter for typographyBody2FontWeight
    *******************************************************************************/
   public void setTypographyBody2FontWeight(Integer typographyBody2FontWeight)
   {
      this.typographyBody2FontWeight = typographyBody2FontWeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyBody2FontWeight
    *******************************************************************************/
   public QThemeMetaData withTypographyBody2FontWeight(Integer typographyBody2FontWeight)
   {
      this.typographyBody2FontWeight = typographyBody2FontWeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyBody2LineHeight
    *******************************************************************************/
   public String getTypographyBody2LineHeight()
   {
      return (this.typographyBody2LineHeight);
   }



   /*******************************************************************************
    ** Setter for typographyBody2LineHeight
    *******************************************************************************/
   public void setTypographyBody2LineHeight(String typographyBody2LineHeight)
   {
      this.typographyBody2LineHeight = typographyBody2LineHeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyBody2LineHeight
    *******************************************************************************/
   public QThemeMetaData withTypographyBody2LineHeight(String typographyBody2LineHeight)
   {
      this.typographyBody2LineHeight = typographyBody2LineHeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyBody2LetterSpacing
    *******************************************************************************/
   public String getTypographyBody2LetterSpacing()
   {
      return (this.typographyBody2LetterSpacing);
   }



   /*******************************************************************************
    ** Setter for typographyBody2LetterSpacing
    *******************************************************************************/
   public void setTypographyBody2LetterSpacing(String typographyBody2LetterSpacing)
   {
      this.typographyBody2LetterSpacing = typographyBody2LetterSpacing;
   }



   /*******************************************************************************
    ** Fluent setter for typographyBody2LetterSpacing
    *******************************************************************************/
   public QThemeMetaData withTypographyBody2LetterSpacing(String typographyBody2LetterSpacing)
   {
      this.typographyBody2LetterSpacing = typographyBody2LetterSpacing;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyButtonFontSize
    *******************************************************************************/
   public String getTypographyButtonFontSize()
   {
      return (this.typographyButtonFontSize);
   }



   /*******************************************************************************
    ** Setter for typographyButtonFontSize
    *******************************************************************************/
   public void setTypographyButtonFontSize(String typographyButtonFontSize)
   {
      this.typographyButtonFontSize = typographyButtonFontSize;
   }



   /*******************************************************************************
    ** Fluent setter for typographyButtonFontSize
    *******************************************************************************/
   public QThemeMetaData withTypographyButtonFontSize(String typographyButtonFontSize)
   {
      this.typographyButtonFontSize = typographyButtonFontSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyButtonFontWeight
    *******************************************************************************/
   public Integer getTypographyButtonFontWeight()
   {
      return (this.typographyButtonFontWeight);
   }



   /*******************************************************************************
    ** Setter for typographyButtonFontWeight
    *******************************************************************************/
   public void setTypographyButtonFontWeight(Integer typographyButtonFontWeight)
   {
      this.typographyButtonFontWeight = typographyButtonFontWeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyButtonFontWeight
    *******************************************************************************/
   public QThemeMetaData withTypographyButtonFontWeight(Integer typographyButtonFontWeight)
   {
      this.typographyButtonFontWeight = typographyButtonFontWeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyButtonLineHeight
    *******************************************************************************/
   public String getTypographyButtonLineHeight()
   {
      return (this.typographyButtonLineHeight);
   }



   /*******************************************************************************
    ** Setter for typographyButtonLineHeight
    *******************************************************************************/
   public void setTypographyButtonLineHeight(String typographyButtonLineHeight)
   {
      this.typographyButtonLineHeight = typographyButtonLineHeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyButtonLineHeight
    *******************************************************************************/
   public QThemeMetaData withTypographyButtonLineHeight(String typographyButtonLineHeight)
   {
      this.typographyButtonLineHeight = typographyButtonLineHeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyButtonLetterSpacing
    *******************************************************************************/
   public String getTypographyButtonLetterSpacing()
   {
      return (this.typographyButtonLetterSpacing);
   }



   /*******************************************************************************
    ** Setter for typographyButtonLetterSpacing
    *******************************************************************************/
   public void setTypographyButtonLetterSpacing(String typographyButtonLetterSpacing)
   {
      this.typographyButtonLetterSpacing = typographyButtonLetterSpacing;
   }



   /*******************************************************************************
    ** Fluent setter for typographyButtonLetterSpacing
    *******************************************************************************/
   public QThemeMetaData withTypographyButtonLetterSpacing(String typographyButtonLetterSpacing)
   {
      this.typographyButtonLetterSpacing = typographyButtonLetterSpacing;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyButtonTextTransform
    *******************************************************************************/
   public String getTypographyButtonTextTransform()
   {
      return (this.typographyButtonTextTransform);
   }



   /*******************************************************************************
    ** Setter for typographyButtonTextTransform
    *******************************************************************************/
   public void setTypographyButtonTextTransform(String typographyButtonTextTransform)
   {
      this.typographyButtonTextTransform = typographyButtonTextTransform;
   }



   /*******************************************************************************
    ** Fluent setter for typographyButtonTextTransform
    *******************************************************************************/
   public QThemeMetaData withTypographyButtonTextTransform(String typographyButtonTextTransform)
   {
      this.typographyButtonTextTransform = typographyButtonTextTransform;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyCaptionFontSize
    *******************************************************************************/
   public String getTypographyCaptionFontSize()
   {
      return (this.typographyCaptionFontSize);
   }



   /*******************************************************************************
    ** Setter for typographyCaptionFontSize
    *******************************************************************************/
   public void setTypographyCaptionFontSize(String typographyCaptionFontSize)
   {
      this.typographyCaptionFontSize = typographyCaptionFontSize;
   }



   /*******************************************************************************
    ** Fluent setter for typographyCaptionFontSize
    *******************************************************************************/
   public QThemeMetaData withTypographyCaptionFontSize(String typographyCaptionFontSize)
   {
      this.typographyCaptionFontSize = typographyCaptionFontSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyCaptionFontWeight
    *******************************************************************************/
   public Integer getTypographyCaptionFontWeight()
   {
      return (this.typographyCaptionFontWeight);
   }



   /*******************************************************************************
    ** Setter for typographyCaptionFontWeight
    *******************************************************************************/
   public void setTypographyCaptionFontWeight(Integer typographyCaptionFontWeight)
   {
      this.typographyCaptionFontWeight = typographyCaptionFontWeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyCaptionFontWeight
    *******************************************************************************/
   public QThemeMetaData withTypographyCaptionFontWeight(Integer typographyCaptionFontWeight)
   {
      this.typographyCaptionFontWeight = typographyCaptionFontWeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyCaptionLineHeight
    *******************************************************************************/
   public String getTypographyCaptionLineHeight()
   {
      return (this.typographyCaptionLineHeight);
   }



   /*******************************************************************************
    ** Setter for typographyCaptionLineHeight
    *******************************************************************************/
   public void setTypographyCaptionLineHeight(String typographyCaptionLineHeight)
   {
      this.typographyCaptionLineHeight = typographyCaptionLineHeight;
   }



   /*******************************************************************************
    ** Fluent setter for typographyCaptionLineHeight
    *******************************************************************************/
   public QThemeMetaData withTypographyCaptionLineHeight(String typographyCaptionLineHeight)
   {
      this.typographyCaptionLineHeight = typographyCaptionLineHeight;
      return (this);
   }



   /*******************************************************************************
    ** Getter for typographyCaptionLetterSpacing
    *******************************************************************************/
   public String getTypographyCaptionLetterSpacing()
   {
      return (this.typographyCaptionLetterSpacing);
   }



   /*******************************************************************************
    ** Setter for typographyCaptionLetterSpacing
    *******************************************************************************/
   public void setTypographyCaptionLetterSpacing(String typographyCaptionLetterSpacing)
   {
      this.typographyCaptionLetterSpacing = typographyCaptionLetterSpacing;
   }



   /*******************************************************************************
    ** Fluent setter for typographyCaptionLetterSpacing
    *******************************************************************************/
   public QThemeMetaData withTypographyCaptionLetterSpacing(String typographyCaptionLetterSpacing)
   {
      this.typographyCaptionLetterSpacing = typographyCaptionLetterSpacing;
      return (this);
   }

}
