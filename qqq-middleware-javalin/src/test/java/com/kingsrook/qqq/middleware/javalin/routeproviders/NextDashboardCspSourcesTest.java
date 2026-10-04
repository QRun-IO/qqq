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

package com.kingsrook.qqq.middleware.javalin.routeproviders;


import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.OAuth2AuthenticationMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.AuthenticationMetaDataResponseV1;
import com.kingsrook.qqq.openapi.model.Schema;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Public policy additions must stay separate from complete document policies.
 *******************************************************************************/
class NextDashboardCspSourcesTest
{
   /*******************************************************************************
    ** An unconfigured instance grants no extra sources, but keeps the fixed shape.
    *******************************************************************************/
   @Test
   void testNoFeaturesAndImmutableSnapshot()
   {
      QInstance instance = new QInstance();
      NextDashboardCspSources sources = NextDashboardCspSources.fromInstance(instance);
      JSONObject json = new JSONObject(JsonUtils.toJson(sources));
      assertEquals(Set.of("connectSrc", "scriptSrc", "frameSrc", "styleSrc"), json.keySet());
      for(String key : json.keySet())
      {
         assertEquals(0, json.getJSONArray(key).length());
      }
      assertThrows(UnsupportedOperationException.class, () -> sources.getScriptSrc().add("https://unconfigured.example"));
      instance.setEnvironmentValues(Map.of("GOOGLE_ANALYTICS_ENABLED", "false", "GOOGLE_ANALYTICS_TRACKING_ID", "G-IGNORED"));
      assertEquals(json.toString(), new JSONObject(JsonUtils.toJson(NextDashboardCspSources.fromInstance(instance))).toString());
   }



   /*******************************************************************************
    ** The schema must describe exactly four lists of strings, without constants.
    *******************************************************************************/
   @Test
   void testSchemaAndOptionalResponseField()
   {
      Schema schema = new AuthenticationMetaDataResponseV1().toSchema().getProperties().get("dashboardCspSources");
      assertEquals(Set.of("connectSrc", "scriptSrc", "frameSrc", "styleSrc"), schema.getProperties().keySet());
      for(Schema source : schema.getProperties().values())
      {
         assertEquals("array", source.getType());
         assertEquals("string", source.getItems().getType());
      }
      assertFalse(new JSONObject(JsonUtils.toJson(new AuthenticationMetaDataResponseV1())).has("dashboardCspSources"));
   }

   /*******************************************************************************
    ** Preserve Java's origin spelling while excluding URL credentials and paths.
    *******************************************************************************/
   @Test
   void testOriginSpellings()
   {
      for(String origin : List.of("https://cdn.example.test:443", "http://cdn.example.test:80", "http://[0:0:0:0:0:0:0:1]:80", "https://[2001:db8:0:0::1]:443"))
      {
         QInstance instance = new QInstance();
         instance.withInstanceDefaultAuthentication(new OAuth2AuthenticationMetaData().withBaseUrl(origin + "/private?secret=ORIGIN_SECRET").withName("oidc"));
         assertEquals(List.of(origin), NextDashboardCspSources.fromInstance(instance).getConnectSrc());
      }
   }
}
