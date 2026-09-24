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

package com.kingsrook.qqq.backend.core.model.metadata.branding;


import java.util.LinkedHashMap;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;


/*******************************************************************************
 ** Unit tests for QBrandingMetaData
 *******************************************************************************/
class QBrandingMetaDataTest
{

   /*******************************************************************************
    ** Local BannerSlot enum for test use only.
    *******************************************************************************/
   private enum TestSlot implements BannerSlot
   {
      HEADER, FOOTER
   }


   /*******************************************************************************
    ** getName should always return the constant "Branding".
    *******************************************************************************/
   @Test
   void testGetName_returnsConstantBranding()
   {
      assertEquals("Branding", new QBrandingMetaData().getName());
   }



   /*******************************************************************************
    ** toString should include appName.
    *******************************************************************************/
   @Test
   void testToString_includesAppName()
   {
      QBrandingMetaData branding = new QBrandingMetaData().withAppName("MyApp");
      assertThat(branding.toString()).contains("MyApp");
   }



   /*******************************************************************************
    ** Fluent builder round-trip: every field set via withX() should be read back.
    *******************************************************************************/
   @Test
   void testFluentBuilder_allFields_roundTrip()
   {
      QBrandingMetaData branding = new QBrandingMetaData()
         .withCompanyName("Acme")
         .withCompanyUrl("https://acme.example.com")
         .withAppName("AcmeApp")
         .withLogo("/images/logo.png")
         .withIcon("/images/icon.png")
         .withAccentColor("#FF5733")
         .withAccentColorLight("#FF9973")
         .withGravatarDefault("identicon");

      assertEquals("Acme", branding.getCompanyName());
      assertEquals("https://acme.example.com", branding.getCompanyUrl());
      assertEquals("AcmeApp", branding.getAppName());
      assertEquals("/images/logo.png", branding.getLogo());
      assertEquals("/images/icon.png", branding.getIcon());
      assertEquals("#FF5733", branding.getAccentColor());
      assertEquals("#FF9973", branding.getAccentColorLight());
      assertEquals("identicon", branding.getGravatarDefault());
   }



   /*******************************************************************************
    ** addSelfToInstance should set branding on the given QInstance.
    *******************************************************************************/
   @Test
   void testAddSelfToInstance_setsBrandingOnInstance()
   {
      QBrandingMetaData branding = new QBrandingMetaData().withAppName("TestApp");
      QInstance         instance = new QInstance();

      branding.addSelfToInstance(instance);

      assertSame(branding, instance.getBranding());
   }



   /*******************************************************************************
    ** withBanner lazy-initialises the map and stores the banner under the given slot.
    *******************************************************************************/
   @Test
   void testWithBanner_lazyInitMap_storesBanner()
   {
      Banner banner = new Banner().withMessageText("Maintenance tonight");

      QBrandingMetaData branding = new QBrandingMetaData()
         .withBanner(TestSlot.HEADER, banner);

      assertNotNull(branding.getBanners());
      assertEquals(1, branding.getBanners().size());
      assertSame(banner, branding.getBanners().get(TestSlot.HEADER));
   }



   /*******************************************************************************
    ** withBanner on a slot that already has a banner should replace it.
    *******************************************************************************/
   @Test
   void testWithBanner_replacesExistingSlot()
   {
      Banner first  = new Banner().withMessageText("old");
      Banner second = new Banner().withMessageText("new");

      QBrandingMetaData branding = new QBrandingMetaData()
         .withBanner(TestSlot.HEADER, first)
         .withBanner(TestSlot.HEADER, second);

      assertEquals("new", branding.getBanners().get(TestSlot.HEADER).getMessageText());
   }



   /*******************************************************************************
    ** clone without banners — the clone is a distinct object with same field values.
    *******************************************************************************/
   @Test
   void testClone_noBanners_distinctObjectSameValues()
   {
      QBrandingMetaData original = new QBrandingMetaData()
         .withAppName("MyApp")
         .withAccentColor("#123456");

      QBrandingMetaData clone = original.clone();

      assertNotSame(original, clone);
      assertEquals("MyApp", clone.getAppName());
      assertEquals("#123456", clone.getAccentColor());
      assertNull(clone.getBanners());
   }



   /*******************************************************************************
    ** clone with banners — mutating a cloned banner must not affect the original.
    *******************************************************************************/
   @Test
   void testClone_withBanners_deepCopyBanners()
   {
      Banner originalBanner = new Banner().withMessageText("hello");
      QBrandingMetaData original = new QBrandingMetaData()
         .withBanner(TestSlot.HEADER, originalBanner);

      QBrandingMetaData clone = original.clone();

      // Maps are distinct
      assertNotSame(original.getBanners(), clone.getBanners());

      // Banner entries are distinct objects (deep copy)
      Banner clonedBanner = clone.getBanners().get(TestSlot.HEADER);
      assertNotSame(originalBanner, clonedBanner);

      // Mutating the clone must not affect the original
      clonedBanner.setMessageText("mutated");
      assertEquals("hello", original.getBanners().get(TestSlot.HEADER).getMessageText());
   }



   /*******************************************************************************
    ** clone with multiple banners — all slots should be present in the clone.
    *******************************************************************************/
   @Test
   void testClone_multipleBanners_allSlotsPresent()
   {
      QBrandingMetaData original = new QBrandingMetaData()
         .withBanner(TestSlot.HEADER, new Banner().withMessageText("top"))
         .withBanner(TestSlot.FOOTER, new Banner().withMessageText("bottom"));

      QBrandingMetaData clone = original.clone();

      assertThat(clone.getBanners()).containsKeys(TestSlot.HEADER, TestSlot.FOOTER);
      assertEquals("top", clone.getBanners().get(TestSlot.HEADER).getMessageText());
      assertEquals("bottom", clone.getBanners().get(TestSlot.FOOTER).getMessageText());
   }



   /*******************************************************************************
    ** setBanners replaces the entire banners map.
    *******************************************************************************/
   @Test
   void testSetBanners_replacesBannersMap()
   {
      QBrandingMetaData branding = new QBrandingMetaData()
         .withBanner(TestSlot.HEADER, new Banner().withMessageText("old"));

      branding.setBanners(new LinkedHashMap<>());

      assertThat(branding.getBanners()).isEmpty();
   }

}
