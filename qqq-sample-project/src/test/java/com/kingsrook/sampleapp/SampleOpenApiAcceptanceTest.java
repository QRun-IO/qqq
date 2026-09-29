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


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.kingsrook.qqq.api.actions.GenerateOpenApiSpecAction;
import com.kingsrook.qqq.api.actions.GetTableApiFieldsAction;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.actions.GenerateOpenApiSpecInput;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.openapi.model.SecurityScheme;
import com.kingsrook.qqq.openapi.model.SecuritySchemeType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Compare served OpenAPI descriptions with an owned, versioned application API.
 *******************************************************************************/
class SampleOpenApiAcceptanceTest
{
   private QInstance instance;
   private SampleOpenApiHttpFixture http;



   /*******************************************************************************
    ** Use real memory records and product routes, without an external identity service.
    *******************************************************************************/
   @BeforeEach
   void start() throws Exception
   {
      MemoryRecordStore.fullReset();
      GetTableApiFieldsAction.clearCaches();
      instance = new QInstance();
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withType(QAuthenticationType.MOCK));
      instance.addBackend(new QBackendMetaData().withName("memory").withBackendType(MemoryBackendModule.class));
      instance.withSupplementalMetaData(new ApiInstanceMetaDataContainer().withApiInstanceMetaData(new ApiInstanceMetaData()
         .withName("owned").withPath("/owned/").withLabel("Owned acceptance API").withDescription("Versioned sample metadata")
         .withContactEmail("owner@example.test").withCurrentVersion(new APIVersion("2026.Q3"))
         .withSupportedVersions(List.of(new APIVersion("2026.Q1"), new APIVersion("2026.Q3")))
         .withFutureVersions(List.of(new APIVersion("2027.Q1")))
         .withSecuritySchemes(Map.of("ownedBearer", new SecurityScheme().withType(SecuritySchemeType.HTTP).withScheme("bearer")))));
      instance.addTable(table("person", new ApiTableMetaData().withInitialVersion("2026.Q1").withApiTableName("people"))
         .withField(new QFieldMetaData("displayName", QFieldType.STRING).withSupplementalMetaData(new ApiFieldMetaDataContainer()
            .withApiFieldMetaData("owned", new ApiFieldMetaData().withApiFieldName("name").withDescription("Public name"))))
         .withField(new QFieldMetaData("code", QFieldType.STRING))
         .withField(new QFieldMetaData("later", QFieldType.INTEGER).withSupplementalMetaData(new ApiFieldMetaDataContainer()
            .withApiFieldMetaData("owned", new ApiFieldMetaData().withInitialVersion("2026.Q3"))))
         .withField(new QFieldMetaData("secret", QFieldType.STRING).withSupplementalMetaData(new ApiFieldMetaDataContainer()
            .withApiFieldMetaData("owned", new ApiFieldMetaData().withIsExcluded(true)))));
      instance.addTable(table("excluded", new ApiTableMetaData().withInitialVersion("2026.Q1").withIsExcluded(true)));
      instance.addTable(table("future", new ApiTableMetaData().withInitialVersion("2027.Q1")));
      instance.addTable(table("unconfigured", null));
      ApiInstanceMetaDataContainer.of(instance).withApiInstanceMetaData(new ApiInstanceMetaData()
         .withName("other").withPath("/other/").withLabel("Other owned API").withDescription("Separate field-name scope")
         .withContactEmail("owner@example.test").withCurrentVersion(new APIVersion("2026.Q3"))
         .withSupportedVersions(List.of(new APIVersion("2026.Q1"), new APIVersion("2026.Q3"))).withSecuritySchemes(Map.of()));
      ApiTableMetaDataContainer.of(instance.getTable("person")).withApiTableMetaData("other",
         new ApiTableMetaData().withInitialVersion("2026.Q1").withApiTableName("people"));
      QContext.init(instance, null);
      try
      {
         new QInstanceEnricher(instance).enrich();
         MemoryRecordStore.getInstance().insert(new InsertInput("person").withRecords(List.of(
            new QRecord().withValue("id", 1).withValue("displayName", "foo").withValue("code", "foo").withValue("later", 42).withValue("secret", "fixture-only"),
            new QRecord().withValue("id", 2).withValue("displayName", "bar").withValue("code", "bar").withValue("later", 7))), true);
      }
      finally
      {
         QContext.clear();
      }
      http = new SampleOpenApiHttpFixture(instance);
   }



   /*******************************************************************************
    ** Keep fixture state and native server lifetimes local to each test.
    *******************************************************************************/
   @AfterEach
   void stop()
   {
      try
      {
         if(http != null)
         {
            http.close();
         }
      }
      finally
      {
         QContext.clear();
         MemoryRecordStore.fullReset();
         GetTableApiFieldsAction.clearCaches();
      }
   }



   /*******************************************************************************
    ** Catch version, external-name, schema type and security-description drift.
    *******************************************************************************/
   @Test
   void testServedVersionsFieldsRoutesAndSecurityMatchRecords() throws Exception
   {
      for(String version : List.of("2026.Q1", "2026.Q3"))
      {
         JsonNode record = getJson("/owned/" + version + "/people/1", 200);
         assertEquals("foo", record.path("name").asText());
         assertEquals(1, record.path("id").asInt());
         Set<String> expectedFields = "2026.Q1".equals(version) ? Set.of("id", "name", "code") : Set.of("id", "name", "code", "later");
         assertEquals(expectedFields, names(record));
         if("2026.Q3".equals(version))
         {
            assertEquals(42, record.path("later").asInt());
         }
         for(String format : List.of("json", "yaml"))
         {
            JsonNode document = spec(version, format);
            assertEquals("3.0.3", document.path("openapi").asText());
            assertEquals("Owned acceptance API", document.at("/info/title").asText());
            assertEquals(version, document.at("/info/version").asText());
            JsonNode properties = document.at("/components/schemas/people/properties");
            assertEquals(expectedFields, names(properties));
            assertEquals("integer", properties.at("/id/type").asText());
            assertEquals("string", properties.at("/name/type").asText());
            assertEquals("Public name", properties.at("/name/description").asText());
            assertEquals("http", document.at("/components/securitySchemes/ownedBearer/type").asText());
            assertEquals("bearer", document.at("/components/securitySchemes/ownedBearer/scheme").asText());
            JsonNode get = document.path("paths").path("/owned/" + version + "/people/{id}").path("get");
            assertEquals("#/components/schemas/people", get.at("/responses/200/content/application~1json/schema/$ref").asText());
            assertEquals(JsonUtils.toObject("[{\"ownedBearer\":[]}]", JsonNode.class), get.path("security"));
            assertFalse(document.path("paths").has("/owned/" + version + "/person/{id}"));
         }
      }
   }



   /*******************************************************************************
    ** Execute a documented filter example against two owned records.
    *******************************************************************************/
   @Test
   void testServedQueryExamplesSelectTheDocumentedRecords() throws Exception
   {
      for(String format : List.of("json", "yaml"))
      {
         JsonNode document = spec("2026.Q3", format);
         JsonNode examples = document.at("/components/examples");
         assertEquals("foo", examples.at("/criteriaStringEquals/value/0").asText());
         assertEquals("BETWEEN 42,47", examples.at("/criteriaNumberBetween/value/0").asText());
         JsonNode query = document.path("paths").path("/owned/2026.Q3/people/query").path("get");
         JsonNode codeParameter = null;
         JsonNode laterParameter = null;
         for(JsonNode parameter : query.path("parameters"))
         {
            if("code".equals(parameter.path("name").asText()))
            {
               codeParameter = parameter;
            }
            if("later".equals(parameter.path("name").asText()))
            {
               laterParameter = parameter;
            }
         }
         assertTrue(codeParameter != null, "Spec must expose the actual filter field");
         assertEquals("#/components/examples/criteriaStringEquals", codeParameter.at("/examples/criteriaStringEquals/$ref").asText());
         assertTrue(laterParameter != null, "Spec must expose the numeric filter field");
         assertEquals("#/components/examples/criteriaNumberBetween", laterParameter.at("/examples/criteriaNumberBetween/$ref").asText());
         for(String filter : List.of(
            "code=" + URLEncoder.encode(examples.at("/criteriaStringEquals/value/0").asText(), StandardCharsets.UTF_8),
            "later=" + URLEncoder.encode(examples.at("/criteriaNumberBetween/value/0").asText(), StandardCharsets.UTF_8)))
         {
            JsonNode result = getJson("/owned/2026.Q3/people/query?" + filter, 200);
            assertEquals(1, result.path("count").asInt());
            assertEquals(1, result.path("records").size());
            assertEquals(1, result.at("/records/0/id").asInt());
            assertEquals("foo", result.at("/records/0/name").asText());
         }
      }
   }



   /*******************************************************************************
    ** Regression #874: the documented alias must select the correct owned record.
    *******************************************************************************/
   @Test
   void testRenamedQueryFieldMatchesDocumentedAlias() throws Exception
   {
      JsonNode document = spec("2026.Q3", "json");
      Set<String> parameters = new HashSet<>();
      for(JsonNode parameter : document.path("paths").path("/owned/2026.Q3/people/query").path("get").path("parameters"))
      {
         parameters.add(parameter.path("name").asText());
      }
      assertTrue(parameters.contains("name"));
      assertFalse(parameters.contains("displayName"));
      for(String version : List.of("2026.Q1", "2026.Q3"))
      {
         JsonNode result = getJson("/owned/" + version + "/people/query?name=foo", 200);
         assertEquals(1, result.path("count").asInt());
         assertEquals(1, result.path("records").size());
         assertEquals(1, result.at("/records/0/id").asInt());
         assertEquals("foo", result.at("/records/0/name").asText());
      }
   }



   /*******************************************************************************
    ** Historical aliases still map through their explicit replacement field.
    *******************************************************************************/
   @Test
   void testHistoricalQueryReplacementAndVersionBoundaries() throws Exception
   {
      QFieldMetaData legacy = new QFieldMetaData("retiredInternalName", QFieldType.STRING)
         .withSupplementalMetaData(new ApiFieldMetaDataContainer().withApiFieldMetaData("owned", new ApiFieldMetaData()
            .withApiFieldName("legacyName").withInitialVersion("2026.Q1").withFinalVersion("2026.Q1").withReplacedByFieldName("displayName")));
      QContext.init(instance, null);
      new QInstanceEnricher(instance).enrichField(legacy);
      ApiTableMetaDataContainer.of(instance.getTable("person")).getApiTableMetaData("owned").withRemovedApiField(legacy);
      QContext.clear();
      JsonNode result = getJson("/owned/2026.Q1/people/query?legacyName=foo", 200);
      assertEquals(1, result.path("count").asInt());
      assertEquals(1, result.path("records").size());
      assertEquals(1, result.at("/records/0/id").asInt());
      assertEquals("foo", result.at("/records/0/legacyName").asText());
      JsonNode error = getJson("/owned/2026.Q3/people/query?legacyName=foo", 400);
      assertEquals("Unrecognized filter criteria field: legacyName", error.path("error").asText());
      assertFalse(error.has("records"));
   }



   /*******************************************************************************
    ** Internal, excluded, unknown and wrong-version/API aliases remain rejected.
    *******************************************************************************/
   @Test
   void testQueryAliasScopeAndInvalidCriteria() throws Exception
   {
      for(String field : List.of("displayName", "secret", "unknown"))
      {
         JsonNode error = getJson("/owned/2026.Q3/people/query?" + field + "=foo", 400);
         assertEquals("Unrecognized filter criteria field: " + field, error.path("error").asText());
         assertFalse(error.has("records"));
      }
      assertEquals("Unrecognized filter criteria field: later", getJson("/owned/2026.Q1/people/query?later=42", 400).path("error").asText());
      assertEquals("Unrecognized filter criteria field: name", getJson("/other/2026.Q3/people/query?name=foo", 400).path("error").asText());
      JsonNode other = getJson("/other/2026.Q3/people/query?displayName=foo", 200);
      assertEquals(1, other.path("count").asInt());
      assertEquals(1, other.path("records").size());
      assertEquals(1, other.at("/records/0/id").asInt());
      assertEquals("foo", other.at("/records/0/displayName").asText());
      JsonNode invalid = getJson("/owned/2026.Q3/people/query?name=" + URLEncoder.encode("BETWEEN foo", StandardCharsets.UTF_8), 400);
      assertTrue(invalid.path("error").asText().contains("for field name requires 2 values"));
      assertFalse(invalid.has("records"));
      assertEquals(0, getJson("/owned/2026.Q3/people/query?name=absent", 200).path("records").size());
   }



   /*******************************************************************************
    ** Excluded, unconfigured and future tables must be absent from docs and API.
    *******************************************************************************/
   @Test
   void testExcludedEndpointsAreNotDocumentedOrServed() throws Exception
   {
      for(String version : List.of("2026.Q1", "2026.Q3"))
      {
         for(String tableName : List.of("excluded", "future", "unconfigured"))
         {
            JsonNode error = getJson("/owned/" + version + "/" + tableName + "/query", 404);
            assertTrue(error.has("error"));
            assertFalse(error.has("records"));
            for(String format : List.of("json", "yaml"))
            {
               JsonNode document = spec(version, format);
               assertFalse(document.path("paths").toString().contains("/" + tableName + "/"));
               assertFalse(document.at("/components/schemas").has(tableName));
            }
         }
      }
   }



   /*******************************************************************************
    ** The generator has explicit required metadata checks, not DTO validation.
    *******************************************************************************/
   @Test
   void testMissingRequiredMetadataRejectsGeneration() throws Exception
   {
      QContext.init(instance, null);
      var apiMetadata = ApiInstanceMetaDataContainer.of(instance);
      instance.getSupplementalMetaData().clear();
      assertEquals("No ApiInstanceMetaDataContainer exists in this instance", assertThrows(QException.class,
         () -> new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput().withApiName("owned").withVersion("2026.Q3"))).getMessage());
      instance.withSupplementalMetaData(apiMetadata);
      assertEquals("Missing required input: apiName", assertThrows(QException.class,
         () -> new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput().withVersion("2026.Q3"))).getMessage());
      assertEquals("Missing required input: version", assertThrows(QException.class,
         () -> new GenerateOpenApiSpecAction().execute(new GenerateOpenApiSpecInput().withApiName("owned"))).getMessage());
      instance.getTable("person").setPrimaryKeyField(null);
      for(String format : List.of("json", "yaml"))
      {
         JsonNode error = getJson("/owned/2026.Q3/openapi." + format, 500);
         assertTrue(error.path("error").asText().contains("because it does not have a primary key"));
         assertFalse(error.has("openapi"));
         assertFalse(error.has("paths"));
      }
   }



   /*******************************************************************************
    ** Resolve local refs and prove the oracle rejects a deliberately dangling schema.
    *******************************************************************************/
   @Test
   void testServedReferencesResolveAndDanglingSchemaIsDetected() throws Exception
   {
      for(String format : List.of("json", "yaml"))
      {
         JsonNode document = spec("2026.Q3", format);
         assertTrue(assertReferences(document, document) > 10);
         ObjectNode broken = document.deepCopy();
         ((ObjectNode) broken.at("/components/schemas")).remove("people");
         assertThrows(AssertionError.class, () -> assertReferences(broken, broken));
      }
   }



   /*******************************************************************************
    ** Check local JSON Pointers throughout the actual document, without a validator dependency.
    *******************************************************************************/
   private int assertReferences(JsonNode document, JsonNode node)
   {
      int count = 0;
      if(node.has("$ref"))
      {
         String reference = node.path("$ref").asText();
         assertTrue(reference.startsWith("#/"), "Expected a local JSON Pointer: " + reference);
         assertFalse(document.at(reference.substring(1)).isMissingNode(), "Dangling reference: " + reference);
         count++;
      }
      for(JsonNode child : node)
      {
         count += assertReferences(document, child);
      }
      return count;
   }



   /*******************************************************************************
    ** Parse each format only after asserting the real endpoint response contract.
    *******************************************************************************/
   private JsonNode spec(String version, String format) throws Exception
   {
      var response = http.get("/owned/" + version + "/openapi." + format);
      assertEquals(200, response.statusCode());
      assertTrue(response.headers().firstValue("Content-Type").orElse("").contains(format));
      return "json".equals(format) ? JsonUtils.toObject(response.body(), JsonNode.class) : new YAMLMapper().readTree(response.body());
   }



   /*******************************************************************************
    ** Assert JSON success/error response status before inspecting its shape.
    *******************************************************************************/
   private JsonNode getJson(String path, int expectedStatus) throws Exception
   {
      var response = http.get(path);
      assertEquals(expectedStatus, response.statusCode(), path + " " + response.body());
      return JsonUtils.toObject(response.body(), JsonNode.class);
   }



   /*******************************************************************************
    ** Compare exact exposed field sets, including absence of private/internal names.
    *******************************************************************************/
   private Set<String> names(JsonNode object)
   {
      Set<String> names = new HashSet<>();
      object.fieldNames().forEachRemaining(names::add);
      return names;
   }



   /*******************************************************************************
    ** Keep the fixture's table declarations small and explicit.
    *******************************************************************************/
   private QTableMetaData table(String name, ApiTableMetaData api)
   {
      QTableMetaData table = new QTableMetaData().withName(name).withBackendName("memory").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
      if(api != null)
      {
         table.withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData("owned", api));
      }
      return table;
   }
}
