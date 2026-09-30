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

package com.kingsrook.qqq.api.actions;


import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.actions.GenerateOpenApiSpecInput;
import com.kingsrook.qqq.api.model.actions.GenerateOpenApiSpecOutput;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;


/*******************************************************************************
 ** Security requirements must survive native OpenAPI model and wire generation.
 *******************************************************************************/
class GenerateOpenApiSecurityTest
{
   private static final String QUERY_PATH = "/api/2026.Q3/person/query";



   /*******************************************************************************
    ** Release the fixture's thread context after success or failure.
    *******************************************************************************/
   @AfterEach
   void clearContext()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Non-OAuth scheme requirements retain their names with empty scope arrays.
    *******************************************************************************/
   @Test
   void testNonOAuthRequirementsHaveEmptyScopes() throws Exception
   {
      for(SecurityScheme scheme : List.of(
         new SecurityScheme().withType(SecuritySchemeType.HTTP).withScheme("bearer"),
         new SecurityScheme().withType(SecuritySchemeType.HTTP).withScheme("basic"),
         new SecurityScheme().withType(SecuritySchemeType.API_KEY).withName("x-api-key").withIn("header")))
      {
         GenerateOpenApiSpecOutput output = generate(Map.of("ownedAuth", scheme));
         assertAll(
            () -> assertEquals(List.of(Map.of("ownedAuth", List.of())), output.getOpenAPI().getPaths().get(QUERY_PATH).getGet().getSecurity()),
            () -> assertSecurity(output, "[{\"ownedAuth\":[]}]"));
      }
   }



   /*******************************************************************************
    ** OAuth permission scopes and flow declarations must not be emptied.
    *******************************************************************************/
   @Test
   void testOAuthScopesArePreserved() throws Exception
   {
      OAuth2 oauth = new OAuth2().withFlows(Map.of("clientCredentials", new OAuth2Flow()
         .withTokenUrl("https://auth.example.test/token").withScopes(Map.of("person.read", "Read people"))));
      GenerateOpenApiSpecOutput output = generate(Map.of("ownedOAuth", oauth));
      assertEquals(List.of(Map.of("ownedOAuth", List.of("person.read"))), output.getOpenAPI().getPaths().get(QUERY_PATH).getGet().getSecurity());
      assertSecurity(output, "[{\"ownedOAuth\":[\"person.read\"]}]");
      for(JsonNode document : documents(output))
      {
         assertEquals("Read people", document.at("/components/securitySchemes/ownedOAuth/flows/clientCredentials/scopes/person.read").asText());
         assertEquals("oauth2", document.at("/components/securitySchemes/ownedOAuth/type").asText());
      }
   }



   /*******************************************************************************
    ** Anonymous operations stay explicitly empty instead of gaining a requirement.
    *******************************************************************************/
   @Test
   void testAnonymousRequirementsStayEmpty() throws Exception
   {
      GenerateOpenApiSpecOutput output = generate(Map.of());
      assertEquals(List.of(), output.getOpenAPI().getPaths().get(QUERY_PATH).getGet().getSecurity());
      assertSecurity(output, "[]");
      for(JsonNode document : documents(output))
      {
         assertEquals(0, document.at("/components/securitySchemes").size());
         assertFalse(document.has("security"));
      }
   }



   /*******************************************************************************
    ** Own a minimal metadata fixture without external providers or record I/O.
    *******************************************************************************/
   private GenerateOpenApiSpecOutput generate(Map<String, SecurityScheme> schemes) throws Exception
   {
      QInstance instance = new QInstance();
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withType(QAuthenticationType.MOCK));
      instance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      instance.addTable(new QTableMetaData().withName("person").withLabel("Person").withBackendName("memory")
         .withPrimaryKeyField("id").withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData("ownedApi",
            new ApiTableMetaData().withInitialVersion("2026.Q3"))));
      instance.withSupplementalMetaData(new ApiInstanceMetaDataContainer().withApiInstanceMetaData(new ApiInstanceMetaData()
         .withName("ownedApi").withPath("/api/").withLabel("Owned API").withDescription("Security requirement fixture")
         .withContactEmail("owner@example.test").withCurrentVersion(new APIVersion("2026.Q3"))
         .withSupportedVersions(List.of(new APIVersion("2026.Q3"))).withSecuritySchemes(schemes)));
      QContext.init(instance, null);
      new QInstanceEnricher(instance).enrich();
      return new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput()
         .withApiName("ownedApi").withVersion("2026.Q3").withTableName("person"));
   }



   /*******************************************************************************
    ** JSON and YAML must preserve the same literal security requirements.
    *******************************************************************************/
   private void assertSecurity(GenerateOpenApiSpecOutput output, String expected) throws Exception
   {
      JsonNode expectedSecurity = JsonUtils.toObject(expected, JsonNode.class);
      List<JsonNode> documents = documents(output);
      assertAll(
         () -> assertEquals(expectedSecurity, documents.get(0).get("paths").get(QUERY_PATH).get("get").get("security")),
         () -> assertEquals(expectedSecurity, documents.get(1).get("paths").get(QUERY_PATH).get("get").get("security")));
   }



   /*******************************************************************************
    ** Parse both output formats without applying output inclusion filters.
    *******************************************************************************/
   private List<JsonNode> documents(GenerateOpenApiSpecOutput output) throws Exception
   {
      return List.of(JsonUtils.toObject(output.getJson(), JsonNode.class), new YAMLMapper().readTree(output.getYaml()));
   }
}
