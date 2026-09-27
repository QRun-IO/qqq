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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.util.Set;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.branding.Banner;
import com.kingsrook.qqq.backend.core.model.metadata.branding.BannerSlot;
import com.kingsrook.qqq.backend.core.model.metadata.branding.QBrandingMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.TestUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.AuthenticationMetaDataResponseV1;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Unit test for AuthenticationMetaDataSpecV1
 *******************************************************************************/
class AuthenticationMetaDataSpecV1Test extends SpecTestBase
{
   /***************************************************************************
    ** a banner slot for the branding tests
    ***************************************************************************/
   private enum TestSlot implements BannerSlot
   {
      TOP
   }



   /***************************************************************************
    ** the served instance has branding, including a banner
    ***************************************************************************/
   @Override
   protected QInstance defineQInstance() throws QException
   {
      QInstance qInstance = TestUtils.defineInstance();
      qInstance.setBranding(brandingWithBanner());
      return (qInstance);
   }



   /***************************************************************************
    **
    ***************************************************************************/
   private static QBrandingMetaData brandingWithBanner()
   {
      return (new QBrandingMetaData()
         .withAppName("QQQ Sample")
         .withCompanyName("QRun")
         .withCompanyUrl("https://qrun.example.test")
         .withLogo("/samples-logo.png")
         .withIcon("/kr-icon.png")
         .withAccentColor("#1d4ed8")
         .withAccentColorLight("#dbeafe")
         .withBanner(TestSlot.TOP, new Banner().withSeverity(Banner.Severity.WARNING).withMessageText("Signed-in users only")));
   }



   /*******************************************************************************
    ** Before sign-in the endpoint exposes the display branding (QRun-IO/qqq#703),
    ** never banners.
    *******************************************************************************/
   @Test
   void testPublicBranding()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/metaData/authentication").asString();
      assertEquals(200, response.getStatus());
      JSONObject branding = JsonUtils.toJSONObject(response.getBody()).getJSONObject("branding");
      assertEquals(Set.of("appName", "companyName", "logo", "icon", "accentColor", "accentColorLight"), branding.keySet());
      assertEquals("QQQ Sample", branding.getString("appName"));
      assertEquals("QRun", branding.getString("companyName"));
      assertEquals("/samples-logo.png", branding.getString("logo"));
      assertEquals("/kr-icon.png", branding.getString("icon"));
      assertEquals("#1d4ed8", branding.getString("accentColor"));
      assertEquals("#dbeafe", branding.getString("accentColorLight"));
      assertFalse(response.getBody().contains("Signed-in users only"));
   }



   /*******************************************************************************
    ** Partial branding keeps only the fields it declares; no branding, no object.
    *******************************************************************************/
   @Test
   void testPartialAndMissingBranding()
   {
      AuthenticationMetaDataResponseV1 partial = new AuthenticationMetaDataResponseV1();
      partial.setBranding(new QBrandingMetaData().withAppName("Only A Name"));
      JSONObject partialJson = new JSONObject(JsonUtils.toJson(partial));
      assertEquals(Set.of("appName"), partialJson.getJSONObject("branding").keySet());

      AuthenticationMetaDataResponseV1 none = new AuthenticationMetaDataResponseV1();
      none.setBranding(null);
      assertFalse(new JSONObject(JsonUtils.toJson(none)).has("branding"));
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return new AuthenticationMetaDataSpecV1();
   }



   /***************************************************************************
    **
    ***************************************************************************/
   @Override
   protected String getVersion()
   {
      return "v1";
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void test()
   {
      HttpResponse<String> response = Unirest.get(getBaseUrlAndPath() + "/metaData/authentication").asString();
      assertEquals(200, response.getStatus());
      JSONObject jsonObject = JsonUtils.toJSONObject(response.getBody());
      assertNotNull(jsonObject);
      assertTrue(jsonObject.has("name"));
      assertTrue(jsonObject.has("type"));
   }



   /*******************************************************************************
    ** Test that when externalBaseUrl is not set, the response uses baseUrl.
    *******************************************************************************/
   @Test
   void testOAuth2Response_noExternalBaseUrl_usesBaseUrl()
   {
      OAuth2AuthenticationMetaData metaData = new OAuth2AuthenticationMetaData()
         .withBaseUrl("http://internal.auth.local/oauth");
      metaData.setName("test");

      AuthenticationMetaDataResponseV1 response = new AuthenticationMetaDataResponseV1();
      response.setAuthenticationMetaData(metaData);

      AuthenticationMetaDataResponseV1.OAuth2Values values = (AuthenticationMetaDataResponseV1.OAuth2Values) response.getValues();
      assertEquals("http://internal.auth.local/oauth", values.getBaseUrl());
   }



   /*******************************************************************************
    ** Test that when externalBaseUrl is set, the response uses it instead of baseUrl.
    *******************************************************************************/
   @Test
   void testOAuth2Response_withExternalBaseUrl_usesExternalBaseUrl()
   {
      OAuth2AuthenticationMetaData metaData = new OAuth2AuthenticationMetaData()
         .withBaseUrl("http://internal.auth.local/oauth")
         .withExternalBaseUrl("https://auth.example.com/oauth");
      metaData.setName("test");

      AuthenticationMetaDataResponseV1 response = new AuthenticationMetaDataResponseV1();
      response.setAuthenticationMetaData(metaData);

      AuthenticationMetaDataResponseV1.OAuth2Values values = (AuthenticationMetaDataResponseV1.OAuth2Values) response.getValues();
      assertEquals("https://auth.example.com/oauth", values.getBaseUrl());
   }

}