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


import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;


/*******************************************************************************
 ** Unit test for QThemeMetaData
 *******************************************************************************/
class QThemeMetaDataTest extends BaseTest
{
   private QCollectingLogger collectingLogger;



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      if(collectingLogger != null)
      {
         collectingLogger.clear();
      }
   }



   /*******************************************************************************
    ** Test that of() returns null when no theme is set on the instance.
    *******************************************************************************/
   @Test
   void testOf_returnsNull_whenNotSet()
   {
      QInstance qInstance = QContext.getQInstance();
      assertNull(QThemeMetaData.of(qInstance));
   }



   /*******************************************************************************
    ** Test that ofOrWithNew() creates and attaches a new theme to the instance.
    *******************************************************************************/
   @Test
   void testOfOrWithNew_createsAndAttaches()
   {
      QInstance qInstance = QContext.getQInstance();

      QThemeMetaData theme = QThemeMetaData.ofOrWithNew(qInstance);
      assertThat(theme).isNotNull();

      //////////////////////////////////////////////////////////
      // Verify it was attached - of() should now return it   //
      //////////////////////////////////////////////////////////
      assertSame(theme, QThemeMetaData.of(qInstance));
   }



   /*******************************************************************************
    ** Test that ofOrWithNew() returns existing theme if already set.
    *******************************************************************************/
   @Test
   void testOfOrWithNew_returnsExisting()
   {
      QInstance qInstance = QContext.getQInstance();

      QThemeMetaData theme1 = QThemeMetaData.ofOrWithNew(qInstance);
      theme1.withPrimaryColor("#FF0000");

      QThemeMetaData theme2 = QThemeMetaData.ofOrWithNew(qInstance);

      assertSame(theme1, theme2);
      assertThat(theme2.getPrimaryColor()).isEqualTo("#FF0000");
   }



   /*******************************************************************************
    ** Test validation passes for valid hex colors.
    *******************************************************************************/
   @Test
   void testValidation_validColors_noWarnings()
   {
      collectingLogger = QLogger.activateCollectingLoggerForClass(QThemeMetaData.class);

      QThemeMetaData theme = new QThemeMetaData()
         .withPrimaryColor("#FF0000")
         .withSecondaryColor("#00FF00")
         .withBackgroundColor("#0000FF")
         .withErrorColor("#F44")
         .withWarningColor("#AABBCCDD");

      theme.validate(QContext.getQInstance(), new QInstanceValidator());

      assertThat(collectingLogger.getCollectedMessages()).isEmpty();
   }



   /*******************************************************************************
    ** Test validation logs warning for invalid hex color.
    *******************************************************************************/
   @Test
   void testValidation_invalidColor_logsWarning()
   {
      collectingLogger = QLogger.activateCollectingLoggerForClass(QThemeMetaData.class);

      QThemeMetaData theme = new QThemeMetaData()
         .withPrimaryColor("not-a-color");

      theme.validate(QContext.getQInstance(), new QInstanceValidator());

      assertThat(collectingLogger.getCollectedMessages()).hasSize(1);
      assertThat(collectingLogger.getCollectedMessages().get(0).getMessage())
         .contains("Invalid theme color value");
   }



   /*******************************************************************************
    ** Test validation logs warning for multiple invalid colors.
    *******************************************************************************/
   @Test
   void testValidation_multipleInvalidColors_logsMultipleWarnings()
   {
      collectingLogger = QLogger.activateCollectingLoggerForClass(QThemeMetaData.class);

      QThemeMetaData theme = new QThemeMetaData()
         .withPrimaryColor("red")
         .withSecondaryColor("blue")
         .withBackgroundColor("invalid");

      theme.validate(QContext.getQInstance(), new QInstanceValidator());

      assertThat(collectingLogger.getCollectedMessages()).hasSize(3);
   }



   /*******************************************************************************
    ** Test validation passes for valid density values.
    *******************************************************************************/
   @Test
   void testValidation_validDensity_noWarnings()
   {
      collectingLogger = QLogger.activateCollectingLoggerForClass(QThemeMetaData.class);

      for(String density : new String[] {"compact", "normal", "comfortable"})
      {
         collectingLogger.clear();
         QThemeMetaData theme = new QThemeMetaData().withDensity(density);
         theme.validate(QContext.getQInstance(), new QInstanceValidator());
         assertThat(collectingLogger.getCollectedMessages()).isEmpty();
      }
   }



   /*******************************************************************************
    ** Test validation logs warning for invalid density.
    *******************************************************************************/
   @Test
   void testValidation_invalidDensity_logsWarning()
   {
      collectingLogger = QLogger.activateCollectingLoggerForClass(QThemeMetaData.class);

      QThemeMetaData theme = new QThemeMetaData().withDensity("invalid-density");
      theme.validate(QContext.getQInstance(), new QInstanceValidator());

      assertThat(collectingLogger.getCollectedMessages()).hasSize(1);
      assertThat(collectingLogger.getCollectedMessages().get(0).getMessage())
         .contains("Invalid theme density value");
   }



   /*******************************************************************************
    ** Test validation passes for valid iconStyle values.
    *******************************************************************************/
   @Test
   void testValidation_validIconStyle_noWarnings()
   {
      collectingLogger = QLogger.activateCollectingLoggerForClass(QThemeMetaData.class);

      for(String iconStyle : new String[] {"filled", "outlined", "rounded", "sharp", "two-tone"})
      {
         collectingLogger.clear();
         QThemeMetaData theme = new QThemeMetaData().withIconStyle(iconStyle);
         theme.validate(QContext.getQInstance(), new QInstanceValidator());
         assertThat(collectingLogger.getCollectedMessages()).isEmpty();
      }
   }



   /*******************************************************************************
    ** Test validation logs warning for invalid iconStyle.
    *******************************************************************************/
   @Test
   void testValidation_invalidIconStyle_logsWarning()
   {
      collectingLogger = QLogger.activateCollectingLoggerForClass(QThemeMetaData.class);

      QThemeMetaData theme = new QThemeMetaData().withIconStyle("invalid-style");
      theme.validate(QContext.getQInstance(), new QInstanceValidator());

      assertThat(collectingLogger.getCollectedMessages()).hasSize(1);
      assertThat(collectingLogger.getCollectedMessages().get(0).getMessage())
         .contains("Invalid theme iconStyle value");
   }



   /*******************************************************************************
    ** Test that fluent setters return this for chaining.
    *******************************************************************************/
   @Test
   void testFluentSetters_returnThis()
   {
      QThemeMetaData theme = new QThemeMetaData();

      //////////////////////////
      // Core palette         //
      //////////////////////////
      assertSame(theme, theme.withPrimaryColor("#000"));
      assertSame(theme, theme.withSecondaryColor("#111"));
      assertSame(theme, theme.withBackgroundColor("#222"));
      assertSame(theme, theme.withSurfaceColor("#333"));
      assertSame(theme, theme.withTextPrimary("#444"));
      assertSame(theme, theme.withTextSecondary("#555"));
      assertSame(theme, theme.withErrorColor("#666"));
      assertSame(theme, theme.withWarningColor("#777"));
      assertSame(theme, theme.withSuccessColor("#888"));
      assertSame(theme, theme.withInfoColor("#999"));
      assertSame(theme, theme.withFontFamily("Arial"));
      assertSame(theme, theme.withHeaderFontFamily("Helvetica"));
      assertSame(theme, theme.withBorderRadius("8px"));
      assertSame(theme, theme.withDensity("normal"));
      assertSame(theme, theme.withLogoPath("/logo.png"));
      assertSame(theme, theme.withIconPath("/icon.png"));
      assertSame(theme, theme.withFaviconPath("/favicon.ico"));
      assertSame(theme, theme.withCustomCss(".custom { color: red; }"));
      assertSame(theme, theme.withIconStyle("outlined"));

      //////////////////////////
      // Branded header       //
      //////////////////////////
      assertSame(theme, theme.withBrandedHeaderEnabled(true));
      assertSame(theme, theme.withBrandedHeaderBackgroundColor("#6BD6D0"));
      assertSame(theme, theme.withBrandedHeaderTextColor("#FFFFFF"));
      assertSame(theme, theme.withBrandedHeaderLogoPath("/logo.png"));
      assertSame(theme, theme.withBrandedHeaderLogoAltText("Logo"));
      assertSame(theme, theme.withBrandedHeaderHeight("48px"));
      assertSame(theme, theme.withBrandedHeaderTagline("Tagline"));

      //////////////////////////
      // App bar              //
      //////////////////////////
      assertSame(theme, theme.withAppBarBackgroundColor("#FFFFFF"));
      assertSame(theme, theme.withAppBarTextColor("#333333"));

      //////////////////////////
      // Sidebar              //
      //////////////////////////
      assertSame(theme, theme.withSidebarBackgroundColor("#F5F3F0"));
      assertSame(theme, theme.withSidebarTextColor("#3D3C3C"));
      assertSame(theme, theme.withSidebarIconColor("#3D3C3C"));
      assertSame(theme, theme.withSidebarSelectedBackgroundColor("#4B9995"));
      assertSame(theme, theme.withSidebarSelectedTextColor("#FFFFFF"));
      assertSame(theme, theme.withSidebarHoverBackgroundColor("#E0E0E0"));
      assertSame(theme, theme.withSidebarDividerColor("#CCCCCC"));

      //////////////////////////
      // Tables               //
      //////////////////////////
      assertSame(theme, theme.withTableHeaderBackgroundColor("#F5F5F5"));
      assertSame(theme, theme.withTableHeaderTextColor("#333333"));
      assertSame(theme, theme.withTableRowHoverColor("#F0F0F0"));
      assertSame(theme, theme.withTableRowSelectedColor("#E3F2FD"));
      assertSame(theme, theme.withTableBorderColor("#E0E0E0"));

      //////////////////////////
      // General              //
      //////////////////////////
      assertSame(theme, theme.withDividerColor("#E0E0E0"));
      assertSame(theme, theme.withBorderColor("#CCCCCC"));
      assertSame(theme, theme.withCardBorderColor("#E0E0E0"));

      //////////////////////////
      // Typography base      //
      //////////////////////////
      assertSame(theme, theme.withMonoFontFamily("monospace"));
      assertSame(theme, theme.withFontSizeBase("14px"));
      assertSame(theme, theme.withFontWeightLight(300));
      assertSame(theme, theme.withFontWeightRegular(400));
      assertSame(theme, theme.withFontWeightMedium(500));
      assertSame(theme, theme.withFontWeightBold(700));

      //////////////////////////
      // Typography variants  //
      //////////////////////////
      assertSame(theme, theme.withTypographyH1FontSize("2.5rem"));
      assertSame(theme, theme.withTypographyH1FontWeight(700));
      assertSame(theme, theme.withTypographyH1LineHeight("1.2"));
      assertSame(theme, theme.withTypographyH1LetterSpacing("-0.02em"));
   }



   /*******************************************************************************
    ** Test that all properties are settable and gettable.
    *******************************************************************************/
   @Test
   void testAllPropertiesSettableAndGettable()
   {
      QThemeMetaData theme = new QThemeMetaData()
         //////////////////////////
         // Core palette         //
         //////////////////////////
         .withPrimaryColor("#FF0000")
         .withSecondaryColor("#00FF00")
         .withBackgroundColor("#0000FF")
         .withSurfaceColor("#FFFFFF")
         .withTextPrimary("#000000")
         .withTextSecondary("#666666")
         .withErrorColor("#FF0000")
         .withWarningColor("#FFA500")
         .withSuccessColor("#00FF00")
         .withInfoColor("#0000FF")
         .withFontFamily("Roboto")
         .withHeaderFontFamily("Open Sans")
         .withBorderRadius("4px")
         .withDensity("compact")
         .withLogoPath("/assets/logo.svg")
         .withIconPath("/assets/icon.svg")
         .withFaviconPath("/assets/favicon.ico")
         .withCustomCss("body { margin: 0; }")
         .withIconStyle("rounded")
         //////////////////////////
         // Branded header       //
         //////////////////////////
         .withBrandedHeaderEnabled(true)
         .withBrandedHeaderBackgroundColor("#6BD6D0")
         .withBrandedHeaderTextColor("#FFFFFF")
         .withBrandedHeaderLogoPath("/header-logo.png")
         .withBrandedHeaderLogoAltText("Brand Logo")
         .withBrandedHeaderHeight("48px")
         .withBrandedHeaderTagline("Your Health Partner")
         //////////////////////////
         // App bar              //
         //////////////////////////
         .withAppBarBackgroundColor("#FAFAFA")
         .withAppBarTextColor("#212121")
         //////////////////////////
         // Sidebar              //
         //////////////////////////
         .withSidebarBackgroundColor("#F5F3F0")
         .withSidebarTextColor("#3D3C3C")
         .withSidebarIconColor("#5A5A5A")
         .withSidebarSelectedBackgroundColor("#4B9995")
         .withSidebarSelectedTextColor("#FFFFFF")
         .withSidebarHoverBackgroundColor("#EEEEEE")
         .withSidebarDividerColor("#DDDDDD")
         //////////////////////////
         // Tables               //
         //////////////////////////
         .withTableHeaderBackgroundColor("#F5F5F5")
         .withTableHeaderTextColor("#424242")
         .withTableRowHoverColor("#FAFAFA")
         .withTableRowSelectedColor("#E3F2FD")
         .withTableBorderColor("#E0E0E0")
         //////////////////////////
         // General              //
         //////////////////////////
         .withDividerColor("#BDBDBD")
         .withBorderColor("#E0E0E0")
         .withCardBorderColor("#EEEEEE")
         //////////////////////////
         // Typography base      //
         //////////////////////////
         .withMonoFontFamily("Fira Code, monospace")
         .withFontSizeBase("14px")
         .withFontWeightLight(300)
         .withFontWeightRegular(400)
         .withFontWeightMedium(500)
         .withFontWeightBold(700)
         //////////////////////////
         // Typography variants  //
         //////////////////////////
         .withTypographyH1FontSize("2.5rem")
         .withTypographyH1FontWeight(700)
         .withTypographyH1LineHeight("1.2")
         .withTypographyH1LetterSpacing("-0.02em")
         .withTypographyH2FontSize("2rem")
         .withTypographyH2FontWeight(600)
         .withTypographyH2LineHeight("1.3")
         .withTypographyH2LetterSpacing("-0.01em")
         .withTypographyBody1FontSize("1rem")
         .withTypographyBody1FontWeight(400)
         .withTypographyBody1LineHeight("1.5")
         .withTypographyBody1LetterSpacing("0.01em")
         .withTypographyButtonFontSize("0.875rem")
         .withTypographyButtonFontWeight(500)
         .withTypographyButtonLineHeight("1.75")
         .withTypographyButtonLetterSpacing("0.02em")
         .withTypographyButtonTextTransform("uppercase");

      //////////////////////////
      // Core palette asserts //
      //////////////////////////
      assertThat(theme.getPrimaryColor()).isEqualTo("#FF0000");
      assertThat(theme.getSecondaryColor()).isEqualTo("#00FF00");
      assertThat(theme.getBackgroundColor()).isEqualTo("#0000FF");
      assertThat(theme.getSurfaceColor()).isEqualTo("#FFFFFF");
      assertThat(theme.getTextPrimary()).isEqualTo("#000000");
      assertThat(theme.getTextSecondary()).isEqualTo("#666666");
      assertThat(theme.getErrorColor()).isEqualTo("#FF0000");
      assertThat(theme.getWarningColor()).isEqualTo("#FFA500");
      assertThat(theme.getSuccessColor()).isEqualTo("#00FF00");
      assertThat(theme.getInfoColor()).isEqualTo("#0000FF");
      assertThat(theme.getFontFamily()).isEqualTo("Roboto");
      assertThat(theme.getHeaderFontFamily()).isEqualTo("Open Sans");
      assertThat(theme.getBorderRadius()).isEqualTo("4px");
      assertThat(theme.getDensity()).isEqualTo("compact");
      assertThat(theme.getLogoPath()).isEqualTo("/assets/logo.svg");
      assertThat(theme.getIconPath()).isEqualTo("/assets/icon.svg");
      assertThat(theme.getFaviconPath()).isEqualTo("/assets/favicon.ico");
      assertThat(theme.getCustomCss()).isEqualTo("body { margin: 0; }");
      assertThat(theme.getIconStyle()).isEqualTo("rounded");

      ////////////////////////////
      // Branded header asserts //
      ////////////////////////////
      assertThat(theme.getBrandedHeaderEnabled()).isTrue();
      assertThat(theme.getBrandedHeaderBackgroundColor()).isEqualTo("#6BD6D0");
      assertThat(theme.getBrandedHeaderTextColor()).isEqualTo("#FFFFFF");
      assertThat(theme.getBrandedHeaderLogoPath()).isEqualTo("/header-logo.png");
      assertThat(theme.getBrandedHeaderLogoAltText()).isEqualTo("Brand Logo");
      assertThat(theme.getBrandedHeaderHeight()).isEqualTo("48px");
      assertThat(theme.getBrandedHeaderTagline()).isEqualTo("Your Health Partner");

      ///////////////////////
      // App bar asserts   //
      ///////////////////////
      assertThat(theme.getAppBarBackgroundColor()).isEqualTo("#FAFAFA");
      assertThat(theme.getAppBarTextColor()).isEqualTo("#212121");

      ///////////////////////
      // Sidebar asserts   //
      ///////////////////////
      assertThat(theme.getSidebarBackgroundColor()).isEqualTo("#F5F3F0");
      assertThat(theme.getSidebarTextColor()).isEqualTo("#3D3C3C");
      assertThat(theme.getSidebarIconColor()).isEqualTo("#5A5A5A");
      assertThat(theme.getSidebarSelectedBackgroundColor()).isEqualTo("#4B9995");
      assertThat(theme.getSidebarSelectedTextColor()).isEqualTo("#FFFFFF");
      assertThat(theme.getSidebarHoverBackgroundColor()).isEqualTo("#EEEEEE");
      assertThat(theme.getSidebarDividerColor()).isEqualTo("#DDDDDD");

      ///////////////////////
      // Tables asserts    //
      ///////////////////////
      assertThat(theme.getTableHeaderBackgroundColor()).isEqualTo("#F5F5F5");
      assertThat(theme.getTableHeaderTextColor()).isEqualTo("#424242");
      assertThat(theme.getTableRowHoverColor()).isEqualTo("#FAFAFA");
      assertThat(theme.getTableRowSelectedColor()).isEqualTo("#E3F2FD");
      assertThat(theme.getTableBorderColor()).isEqualTo("#E0E0E0");

      ///////////////////////
      // General asserts   //
      ///////////////////////
      assertThat(theme.getDividerColor()).isEqualTo("#BDBDBD");
      assertThat(theme.getBorderColor()).isEqualTo("#E0E0E0");
      assertThat(theme.getCardBorderColor()).isEqualTo("#EEEEEE");

      ////////////////////////////
      // Typography base asserts//
      ////////////////////////////
      assertThat(theme.getMonoFontFamily()).isEqualTo("Fira Code, monospace");
      assertThat(theme.getFontSizeBase()).isEqualTo("14px");
      assertThat(theme.getFontWeightLight()).isEqualTo(300);
      assertThat(theme.getFontWeightRegular()).isEqualTo(400);
      assertThat(theme.getFontWeightMedium()).isEqualTo(500);
      assertThat(theme.getFontWeightBold()).isEqualTo(700);

      ///////////////////////////////
      // Typography variants asserts//
      ///////////////////////////////
      assertThat(theme.getTypographyH1FontSize()).isEqualTo("2.5rem");
      assertThat(theme.getTypographyH1FontWeight()).isEqualTo(700);
      assertThat(theme.getTypographyH1LineHeight()).isEqualTo("1.2");
      assertThat(theme.getTypographyH1LetterSpacing()).isEqualTo("-0.02em");
      assertThat(theme.getTypographyH2FontSize()).isEqualTo("2rem");
      assertThat(theme.getTypographyH2FontWeight()).isEqualTo(600);
      assertThat(theme.getTypographyH2LineHeight()).isEqualTo("1.3");
      assertThat(theme.getTypographyH2LetterSpacing()).isEqualTo("-0.01em");
      assertThat(theme.getTypographyBody1FontSize()).isEqualTo("1rem");
      assertThat(theme.getTypographyBody1FontWeight()).isEqualTo(400);
      assertThat(theme.getTypographyBody1LineHeight()).isEqualTo("1.5");
      assertThat(theme.getTypographyBody1LetterSpacing()).isEqualTo("0.01em");
      assertThat(theme.getTypographyButtonFontSize()).isEqualTo("0.875rem");
      assertThat(theme.getTypographyButtonFontWeight()).isEqualTo(500);
      assertThat(theme.getTypographyButtonLineHeight()).isEqualTo("1.75");
      assertThat(theme.getTypographyButtonLetterSpacing()).isEqualTo("0.02em");
      assertThat(theme.getTypographyButtonTextTransform()).isEqualTo("uppercase");
   }



   /*******************************************************************************
    ** Test that getName() returns the expected constant.
    *******************************************************************************/
   @Test
   void testGetName()
   {
      QThemeMetaData theme = new QThemeMetaData();
      assertThat(theme.getName()).isEqualTo(QThemeMetaData.NAME);
      assertThat(theme.getName()).isEqualTo("theme");
   }



   /*******************************************************************************
    ** Test validation of extended color properties (branded header, sidebar, etc).
    *******************************************************************************/
   @Test
   void testValidation_extendedColors_validHex_noWarnings()
   {
      collectingLogger = QLogger.activateCollectingLoggerForClass(QThemeMetaData.class);

      QThemeMetaData theme = new QThemeMetaData()
         .withBrandedHeaderBackgroundColor("#6BD6D0")
         .withBrandedHeaderTextColor("#FFFFFF")
         .withAppBarBackgroundColor("#FAFAFA")
         .withAppBarTextColor("#212121")
         .withSidebarBackgroundColor("#F5F3F0")
         .withSidebarTextColor("#3D3C3C")
         .withSidebarIconColor("#5A5A5A")
         .withSidebarSelectedBackgroundColor("#4B9995")
         .withSidebarSelectedTextColor("#FFFFFF")
         .withSidebarHoverBackgroundColor("#EEE")
         .withSidebarDividerColor("#DDD")
         .withTableHeaderBackgroundColor("#F5F5F5")
         .withTableHeaderTextColor("#424242")
         .withTableRowHoverColor("#FAFAFA")
         .withTableRowSelectedColor("#E3F2FD")
         .withTableBorderColor("#E0E0E0")
         .withDividerColor("#BDBDBD")
         .withBorderColor("#E0E0E0")
         .withCardBorderColor("#EEEEEE");

      theme.validate(QContext.getQInstance(), new QInstanceValidator());

      assertThat(collectingLogger.getCollectedMessages()).isEmpty();
   }



   /*******************************************************************************
    ** Test validation logs warnings for invalid extended color properties.
    *******************************************************************************/
   @Test
   void testValidation_extendedColors_invalidHex_logsWarnings()
   {
      collectingLogger = QLogger.activateCollectingLoggerForClass(QThemeMetaData.class);

      QThemeMetaData theme = new QThemeMetaData()
         .withBrandedHeaderBackgroundColor("turquoise")
         .withSidebarBackgroundColor("cream")
         .withTableHeaderBackgroundColor("lightgray");

      theme.validate(QContext.getQInstance(), new QInstanceValidator());

      assertThat(collectingLogger.getCollectedMessages()).hasSize(3);
      assertThat(collectingLogger.getCollectedMessages().get(0).getMessage())
         .contains("Invalid theme color value");
   }
}
