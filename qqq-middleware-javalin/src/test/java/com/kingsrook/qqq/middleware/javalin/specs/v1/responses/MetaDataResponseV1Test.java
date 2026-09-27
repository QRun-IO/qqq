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

package com.kingsrook.qqq.middleware.javalin.specs.v1.responses;


import com.kingsrook.qqq.backend.core.model.actions.metadata.MetaDataOutput;
import com.kingsrook.qqq.backend.core.model.metadata.branding.Banner;
import com.kingsrook.qqq.backend.core.model.metadata.branding.BannerSlot;
import com.kingsrook.qqq.backend.core.model.metadata.branding.QBrandingMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Unit test for MetaDataResponseV1
 *******************************************************************************/
class MetaDataResponseV1Test
{
   /***************************************************************************
    **
    ***************************************************************************/
   private enum TestSlot implements BannerSlot
   {
      TOP
   }



   /*******************************************************************************
    ** v1 metadata carries the instance branding (QRun-IO/qqq#539).
    *******************************************************************************/
   @Test
   void testBranding()
   {
      MetaDataOutput output = new MetaDataOutput();
      output.setBranding(new QBrandingMetaData()
         .withAppName("QQQ Sample")
         .withCompanyName("QRun")
         .withLogo("/samples-logo.png")
         .withIcon("/kr-icon.png")
         .withAccentColor("#123456")
         .withBanner(TestSlot.TOP, new Banner().withSeverity(Banner.Severity.WARNING).withMessageText("Maintenance tonight")));

      JSONObject json     = new JSONObject(JsonUtils.toJson(new MetaDataResponseV1().withMetaDataOutput(output)));
      JSONObject branding = json.getJSONObject("branding");
      assertEquals("QQQ Sample", branding.getString("appName"));
      assertEquals("QRun", branding.getString("companyName"));
      assertEquals("/samples-logo.png", branding.getString("logo"));
      assertEquals("/kr-icon.png", branding.getString("icon"));
      assertEquals("#123456", branding.getString("accentColor"));
      JSONObject banner = branding.getJSONObject("banners").getJSONObject("TOP");
      assertEquals("WARNING", banner.getString("severity"));
      assertEquals("Maintenance tonight", banner.getString("messageText"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testWithoutBranding()
   {
      JSONObject json = new JSONObject(JsonUtils.toJson(new MetaDataResponseV1().withMetaDataOutput(new MetaDataOutput())));
      assertFalse(json.has("branding") && !json.isNull("branding"));
      assertFalse(json.has("tables"));
   }
}
