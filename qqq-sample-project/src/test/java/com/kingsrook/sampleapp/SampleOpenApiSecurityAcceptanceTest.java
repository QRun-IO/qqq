/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2026.  Kingsrook, LLC
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

package com.kingsrook.sampleapp;


import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.kingsrook.qqq.api.javalin.QJavalinApiHandler;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.openapi.model.OAuth2;
import com.kingsrook.qqq.openapi.model.OAuth2Flow;
import com.kingsrook.qqq.openapi.model.SecurityScheme;
import com.kingsrook.qqq.openapi.model.SecuritySchemeType;
import io.javalin.Javalin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Exercise actual public spec routes with owned metadata and an ephemeral port.
 *******************************************************************************/
class SampleOpenApiSecurityAcceptanceTest
{
   private Javalin server;
   private HttpClient client;



   /*******************************************************************************
    ** Declare security descriptions without calling any external auth provider.
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      QInstance instance = new QInstance();
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withType(QAuthenticationType.MOCK));
      instance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      ApiTableMetaDataContainer tableApis = new ApiTableMetaDataContainer();
      ApiInstanceMetaDataContainer apis = new ApiInstanceMetaDataContainer();
      OAuth2 oauth = new OAuth2().withFlows(Map.of("clientCredentials", new OAuth2Flow()
         .withTokenUrl("https://auth.example.test/token").withScopes(Map.of("person.read", "Read people"))));
      Map<String, Map<String, SecurityScheme>> schemes = Map.of(
         "bearer", Map.of("ownedBearer", new SecurityScheme().withType(SecuritySchemeType.HTTP).withScheme("bearer")),
         "apiKey", Map.of("ownedKey", new SecurityScheme().withType(SecuritySchemeType.API_KEY).withIn("header").withName("x-api-key")),
         "oauth", Map.of("ownedOAuth", oauth),
         "anonymous", Map.of());
      for(var entry : schemes.entrySet())
      {
         String name = entry.getKey();
         tableApis.withApiTableMetaData(name, new ApiTableMetaData().withInitialVersion("2026.Q3"));
         apis.withApiInstanceMetaData(new ApiInstanceMetaData().withName(name).withPath("/" + name + "/")
            .withLabel(name).withDescription("Owned spec acceptance").withContactEmail("owner@example.test")
            .withCurrentVersion(new APIVersion("2026.Q3")).withSupportedVersions(List.of(new APIVersion("2026.Q3")))
            .withSecuritySchemes(entry.getValue()));
      }
      instance.addTable(new QTableMetaData().withName("person").withLabel("Person").withBackendName("memory")
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER)).withSupplementalMetaData(tableApis));
      instance.withSupplementalMetaData(apis);
      QContext.init(instance, null);
      try
      {
         new QInstanceEnricher(instance).enrich();
      }
      finally
      {
         QContext.clear();
      }
      server = Javalin.create(config ->
      {
         config.routes.apiBuilder(new QJavalinApiHandler(instance).getRoutes());
         config.routes.after(context -> QContext.clear());
      });
      server.start("127.0.0.1", 0);
      client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
   }



   /*******************************************************************************
    ** Bound client and server resource lifetimes, including failed assertions.
    *******************************************************************************/
   @AfterEach
   void stop()
   {
      try
      {
         if(client != null)
         {
            client.close();
         }
      }
      finally
      {
         if(server != null)
         {
            server.stop();
         }
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** Bearer auth stays required even though its OpenAPI scope list is empty.
    *******************************************************************************/
   @Test
   void testBearerSpecRetainsEmptyScopeRequirement() throws Exception
   {
      assertSpec("bearer", "[{\"ownedBearer\":[]}]", "ownedBearer", "http");
   }



   /*******************************************************************************
    ** API key requirements use the same non-OAuth empty-array contract.
    *******************************************************************************/
   @Test
   void testApiKeySpecRetainsEmptyScopeRequirement() throws Exception
   {
      assertSpec("apiKey", "[{\"ownedKey\":[]}]", "ownedKey", "apiKey");
   }



   /*******************************************************************************
    ** OAuth permission scopes survive both public response formats.
    *******************************************************************************/
   @Test
   void testOAuthSpecRetainsPermissionScopes() throws Exception
   {
      assertSpec("oauth", "[{\"ownedOAuth\":[\"person.read\"]}]", "ownedOAuth", "oauth2");
   }



   /*******************************************************************************
    ** Anonymous descriptions stay distinguishable from required empty scopes.
    *******************************************************************************/
   @Test
   void testAnonymousSpecRetainsEmptyRequirements() throws Exception
   {
      assertSpec("anonymous", "[]", null, null);
   }



   /*******************************************************************************
    ** Rejected versions must return an error rather than a successful document.
    *******************************************************************************/
   @Test
   void testUnsupportedVersionDoesNotReturnSpec() throws Exception
   {
      for(String format : List.of("json", "yaml"))
      {
         HttpResponse<String> response = get("/bearer/2025.Q1/openapi." + format);
         assertEquals(500, response.statusCode());
         JsonNode body = JsonUtils.toObject(response.body(), JsonNode.class);
         assertTrue(body.get("error").asText().contains("not a supported API Version"));
         assertFalse(body.has("openapi"));
         assertFalse(body.has("paths"));
      }
   }



   /*******************************************************************************
    ** Assert semantic JSON/YAML, including required keys and anonymous boundaries.
    *******************************************************************************/
   private void assertSpec(String api, String expected, String schemeName, String schemeType) throws Exception
   {
      for(String format : List.of("json", "yaml"))
      {
         HttpResponse<String> response = get("/" + api + "/2026.Q3/openapi." + format);
         assertEquals(200, response.statusCode());
         assertTrue(response.headers().firstValue("Content-Type").orElse("").contains(format));
         JsonNode document = "json".equals(format) ? JsonUtils.toObject(response.body(), JsonNode.class) : new YAMLMapper().readTree(response.body());
         assertEquals("3.0.3", document.get("openapi").asText());
         JsonNode security = document.get("paths").get("/" + api + "/2026.Q3/person/query").get("get").get("security");
         assertEquals(JsonUtils.toObject(expected, JsonNode.class), security);
         if(schemeName == null)
         {
            assertEquals(0, document.at("/components/securitySchemes").size());
            assertFalse(document.has("security"));
         }
         else
         {
            assertEquals(schemeType, document.at("/components/securitySchemes/" + schemeName + "/type").asText());
            assertTrue(security.get(0).has(schemeName));
         }
      }
   }



   /*******************************************************************************
    ** Use the bound loopback port and a bounded request timeout.
    *******************************************************************************/
   private HttpResponse<String> get(String path) throws Exception
   {
      return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + server.port() + path))
         .timeout(Duration.ofSeconds(10)).GET().build(), HttpResponse.BodyHandlers.ofString());
   }
}
