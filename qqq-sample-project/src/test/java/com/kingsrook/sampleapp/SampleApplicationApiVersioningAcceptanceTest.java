/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import com.kingsrook.qqq.api.actions.ApiImplementation;
import com.kingsrook.qqq.api.actions.GetTableApiFieldsAction;
import com.kingsrook.qqq.api.javalin.QBadRequestException;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.actions.ApiFieldCustomValueMapper;
import com.kingsrook.qqq.api.model.actions.HttpApiResponse;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessCustomizers;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaData;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.processes.ApiProcessObjectOutput;
import com.kingsrook.qqq.api.model.metadata.processes.PreRunApiProcessCustomizer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiAssociationMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QNotFoundException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.openapi.model.HttpMethod;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.h2.tools.RunScript;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Source acceptance for opt-in application APIs over the sample's Person/Pet
 ** schema. Public API actions use owned H2 data; JDBC is the persistence oracle.
 *******************************************************************************/
class SampleApplicationApiVersioningAcceptanceTest
{
   private static final String V1 = "2026.01";
   private static final String V2 = "2026.02";
   private static final String V3 = "2026.03";
   private static final String PROCESS = "readVersionedSamplePerson";
   private static final String PROCESS_IDS = "apiVersioningProcessIds";
   private CapturedContext previousContext;
   private Map<String, Serializable> previousObjects;
   private final ArrayList<String> processIds = new ArrayList<>();
   private Connection anchor;
   private QInstance instance;
   private ApiInstanceMetaData api;
   private String apiName;



   /*******************************************************************************
    ** A unique API name also isolates the module's process-name memoization.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousContext = QContext.capture();
      previousObjects = QContext.getObjects();
      QContext.setObjects(new LinkedHashMap<>());
      QContext.getObjects().put(PROCESS_IDS, processIds);
      ConnectionManager.resetConnectionProviders();
      String jdbcUrl = "jdbc:h2:mem:api_versioning_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=0";
      anchor = DriverManager.getConnection(jdbcUrl, "sa", "");
      try(InputStreamReader reader = new InputStreamReader(SampleMetaDataProvider.class.getResourceAsStream("/prime-test-database.sql"), StandardCharsets.UTF_8))
      {
         RunScript.execute(anchor, reader);
      }
      instance = SampleMetaDataProvider.defineTestInstance();
      RDBMSBackendMetaData backend = SampleMetaDataProvider.defineRdbmsBackend().withName("apiVersioningDatabase").withJdbcUrl(jdbcUrl);
      instance.addBackend(backend);
      for(String tableName : List.of("person", "pet", "petNote"))
      {
         instance.getTable(tableName).setBackendName(backend.getName());
      }
      apiName = "sampleVersioning" + UUID.randomUUID();
      api = new ApiInstanceMetaData().withName(apiName).withPath("/sample-versioning/").withLabel("Sample versioning")
         .withDescription("Owned Person/Pet acceptance fixture").withContactEmail("sample@example.invalid")
         .withCurrentVersion(new APIVersion(V2)).withPastVersions(List.of(new APIVersion(V1)))
         .withFutureVersions(List.of(new APIVersion(V3))).withSupportedVersions(List.of(new APIVersion(V1), new APIVersion(V2)));
      instance.withSupplementalMetaData(new ApiInstanceMetaDataContainer().withApiInstanceMetaData(api));
      exposeTable("person", new ApiTableMetaData().withInitialVersion(V1).withApiTableName("people")
         .withApiAssociationMetaData("pets", new ApiAssociationMetaData().withInitialVersion(V2))
         .withRemovedApiField(new QFieldMetaData("workDays", QFieldType.INTEGER).withSupplementalMetaData(new ApiFieldMetaDataContainer()
            .withApiFieldMetaData(apiName, new ApiFieldMetaData().withInitialVersion(V1).withFinalVersion(V1).withReplacedByFieldName("daysWorked")))));
      exposeTable("pet", new ApiTableMetaData().withInitialVersion(V2).withFinalVersion(V2));
      exposeTable("petNote", new ApiTableMetaData().withInitialVersion(V2));
      for(String field : List.of("id", "firstName", "lastName"))
      {
         exposeField("person", field, new ApiFieldMetaData().withInitialVersion(V1));
      }
      exposeField("person", "firstName", new ApiFieldMetaData().withInitialVersion(V1).withApiFieldName("givenName"));
      exposeField("person", "lastName", new ApiFieldMetaData().withInitialVersion(V1).withCustomValueMapper(new QCodeReference(FamilyNameMapper.class)));
      exposeField("person", "daysWorked", new ApiFieldMetaData().withInitialVersion(V2));
      exposeField("person", "annualSalary", new ApiFieldMetaData().withInitialVersion(V1).withIsExcluded(true));
      for(String field : List.of("id", "name"))
      {
         exposeField("pet", field, new ApiFieldMetaData().withInitialVersion(V2));
      }
      for(String field : List.of("id", "note"))
      {
         exposeField("petNote", field, new ApiFieldMetaData().withInitialVersion(V2));
      }
      instance.addProcess(new QProcessMetaData().withName(PROCESS)
         .withStep(new QBackendStepMetaData().withName("readPerson").withCode(new QCodeReference(ReadPersonStep.class)))
         .withSupplementalMetaData(new ApiProcessMetaDataContainer().withApiProcessMetaData(apiName, new ApiProcessMetaData()
            .withApiProcessName("personName").withInitialVersion(V2).withFinalVersion(V2).withMethod(HttpMethod.GET)
            .withCustomizer(ApiProcessCustomizers.PRE_RUN.getRole(), new QCodeReference(ReadPersonStep.class))
            .withOutput(new ApiProcessObjectOutput().withOutputField(new QFieldMetaData("givenName", QFieldType.STRING))))));
      QContext.init(instance, new QSession());
      ApiImplementation.clearCaches();
      GetTableApiFieldsAction.clearCaches();
      new QInstanceValidator().revalidate(instance);
   }



   /*******************************************************************************
    ** Remove only owned process state, close H2 and restore the caller's context.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         for(String id : processIds)
         {
            RunProcessAction.getStateProvider().remove(new UUIDAndTypeStateKey(UUID.fromString(id), StateType.PROCESS_STATUS));
         }
      }
      finally
      {
         try
         {
            if(anchor != null)
            {
               try(Statement statement = anchor.createStatement())
               {
                  statement.execute("SHUTDOWN");
               }
               finally
               {
                  anchor.close();
               }
            }
         }
         finally
         {
            ApiImplementation.clearCaches();
            GetTableApiFieldsAction.clearCaches();
            ConnectionManager.resetConnectionProviders();
            QContext.clear();
            QContext.init(previousContext);
            QContext.setObjects(previousObjects);
         }
      }
   }



   /*******************************************************************************
    ** Historical names and versioned associations shape actual retrieved records.
    *******************************************************************************/
   @Test
   void testVersionedFieldsAndNestedRecords() throws Exception
   {
      Map<String, Serializable> old = ApiImplementation.get(api, V1, "people", "1");
      assertEquals(Set.of("id", "givenName", "lastName", "workDays"), old.keySet());
      assertEquals("Avery", old.get("givenName"));
      assertEquals("family:Sample", old.get("lastName"));
      assertEquals(1001, old.get("workDays"));
      Map<String, Serializable> current = ApiImplementation.get(api, V2, "people", "1");
      assertEquals(Set.of("id", "givenName", "lastName", "daysWorked", "pets"), current.keySet());
      assertEquals(1001, current.get("daysWorked"));
      JSONObject json = new JSONObject(current);
      assertEquals(4, json.getJSONArray("pets").length());
      Map<Integer, JSONObject> pets = new LinkedHashMap<>();
      json.getJSONArray("pets").forEach(value -> pets.put(((JSONObject) value).getInt("id"), (JSONObject) value));
      assertEquals(Set.of(1, 2, 3, 4), pets.keySet());
      assertEquals("Charlie", pets.get(1).getString("name"));
      assertFalse(pets.get(1).has("personId"));
      assertEquals("Target note", pets.get(1).getJSONArray("notes").getJSONObject(0).getString("note"));
      assertEquals("Charlie", ApiImplementation.get(api, V2, "pet", "1").get("name"));
   }



   /*******************************************************************************
    ** Table enrichment inherits field exposure unless the application excludes
    ** it explicitly. Keep this default visible rather than claiming opt-in only.
    *******************************************************************************/
   @Test
   void testUnconfiguredFieldInheritsTableVersion()
   {
      instance.getTable("person").getField("email").withSupplementalMetaData(new ApiFieldMetaDataContainer());
      GetTableApiFieldsAction.clearCaches();
      assertDoesNotThrow(() -> new QInstanceValidator().revalidate(instance));
      assertEquals("avery@example.invalid", assertDoesNotThrow(() -> ApiImplementation.get(api, V1, "people", "1")).get("email"));
   }



   /*******************************************************************************
    ** Both rename mechanisms and a custom mapper reach the physical columns.
    *******************************************************************************/
   @Test
   void testMappedWritesPersistThroughBothVersions() throws Exception
   {
      ApiImplementation.update(api, V1, "people", "1", """
         {"givenName":"Alex","lastName":"family:VersionOne","workDays":42}
         """);
      assertEquals(List.of("Alex", "VersionOne", "42", "avery@example.invalid"), personValues());
      assertEquals("family:VersionOne", ApiImplementation.get(api, V1, "people", "1").get("lastName"));
      ApiImplementation.update(api, V2, "people", "1", """
         {"givenName":"Taylor","lastName":"family:VersionTwo","daysWorked":43}
         """);
      assertEquals(List.of("Taylor", "VersionTwo", "43", "avery@example.invalid"), personValues());
      assertEquals(43, ApiImplementation.get(api, V1, "people", "1").get("workDays"));
      assertEquals(43, ApiImplementation.get(api, V2, "people", "1").get("daysWorked"));
   }



   /*******************************************************************************
    ** Opt-in and version failures reject requests before changing stored data.
    *******************************************************************************/
   @Test
   void testUnexposedTablesFieldsAndVersionsStayUnavailable() throws Exception
   {
      assertNotNull(instance.getTable("carrier"));
      assertThrows(QNotFoundException.class, () -> ApiImplementation.get(api, V2, "carrier", "1"));
      assertThrows(QNotFoundException.class, () -> ApiImplementation.get(api, V2, "person", "1"));
      assertThrows(QNotFoundException.class, () -> ApiImplementation.get(api, V1, "pet", "1"));
      assertThrows(QNotFoundException.class, () -> ApiImplementation.get(api, V3, "people", "1"));
      api.withSupportedVersions(List.of(new APIVersion(V1), new APIVersion(V2), new APIVersion(V3)));
      assertThrows(QNotFoundException.class, () -> ApiImplementation.get(api, V3, "pet", "1"));
      ApiTableMetaDataContainer.of(instance.getTable("pet")).getApiTableMetaData(apiName).setIsExcluded(true);
      assertThrows(QNotFoundException.class, () -> ApiImplementation.get(api, V2, "pet", "1"));
      List<String> before = personValues();
      for(String field : List.of("email", "annualSalary", "firstName", "workDays"))
      {
         QBadRequestException error = assertThrows(QBadRequestException.class,
            () -> ApiImplementation.update(api, V2, "people", "1", new JSONObject().put(field, "unavailable").toString()));
         assertTrue(error.getMessage().contains("unrecognized field name: " + field));
         assertEquals(before, personValues());
      }
      assertThrows(QBadRequestException.class, () -> ApiImplementation.update(api, V1, "people", "1", "{\"daysWorked\":99}"));
      assertEquals(before, personValues());
   }



   /*******************************************************************************
    ** Execute a registered process that reads real sample data, then prove its
    ** opt-in name, version range, exclusion and permission boundaries.
    *******************************************************************************/
   @Test
   void testOptInProcessAndVersionBoundaries() throws Exception
   {
      HttpApiResponse response = ApiImplementation.runProcess(api, V2, "personName", Map.of());
      assertEquals(200, response.getStatusCode().getCode());
      assertEquals(Map.of("givenName", "Avery"), response.getResponseBodyObject());
      assertEquals(1, processIds.size());
      QProcessMetaData unexposedProcess = instance.getProcess(SampleMetaDataProvider.PROCESS_NAME_GREET);
      assertNotNull(unexposedProcess);
      unexposedProcess.setIsHidden(false);
      assertThrows(QNotFoundException.class, () -> ApiImplementation.runProcess(api, V2, unexposedProcess.getName(), Map.of()));
      assertThrows(QNotFoundException.class, () -> ApiImplementation.runProcess(api, V2, PROCESS, Map.of()));
      assertThrows(QNotFoundException.class, () -> ApiImplementation.runProcess(api, V1, "personName", Map.of()));
      assertThrows(QNotFoundException.class, () -> ApiImplementation.runProcess(api, V3, "personName", Map.of()));
      api.withSupportedVersions(List.of(new APIVersion(V1), new APIVersion(V2), new APIVersion(V3)));
      assertThrows(QNotFoundException.class, () -> ApiImplementation.runProcess(api, V3, "personName", Map.of()));
      ApiProcessMetaData metadata = ApiProcessMetaDataContainer.of(instance.getProcess(PROCESS)).getApiProcessMetaData(apiName);
      metadata.setIsExcluded(true);
      assertThrows(QNotFoundException.class, () -> ApiImplementation.runProcess(api, V2, "personName", Map.of()));
      metadata.setIsExcluded(false);
      instance.getProcess(PROCESS).withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.HAS_ACCESS_PERMISSION));
      assertThrows(QPermissionDeniedException.class, () -> ApiImplementation.runProcess(api, V2, "personName", Map.of()));
      assertEquals(1, processIds.size());
   }



   /*******************************************************************************
    ** Permission rejection and record filtering have independent data controls.
    *******************************************************************************/
   @Test
   void testDeniedDataCannotBeReadOrChanged() throws Exception
   {
      assertEquals("Avery", ApiImplementation.get(api, V2, "people", "1").get("givenName"));
      List<String> before = personValues();
      instance.getTable("person").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      assertThrows(QPermissionDeniedException.class, () -> ApiImplementation.get(api, V2, "people", "1"));
      assertThrows(QPermissionDeniedException.class, () -> ApiImplementation.update(api, V2, "people", "1", "{\"givenName\":\"denied\"}"));
      assertEquals(before, personValues());
      instance.getTable("person").setPermissionRules(null);
      instance.addSecurityKeyType(new QSecurityKeyType().withName("apiPerson"));
      instance.getTable("person").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("apiPerson").withFieldName("id"));
      QContext.setQSession(new QSession().withSecurityKeyValue("apiPerson", 2));
      assertEquals("Blair", ApiImplementation.get(api, V2, "people", "2").get("givenName"));
      assertThrows(QNotFoundException.class, () -> ApiImplementation.get(api, V2, "people", "1"));
      assertThrows(QException.class, () -> ApiImplementation.update(api, V2, "people", "1", "{\"givenName\":\"denied\"}"));
      assertEquals(before, personValues());
      QContext.setQSession(new QSession().withSecurityKeyValue("apiPerson", 1));
      assertEquals("Avery", ApiImplementation.get(api, V2, "people", "1").get("givenName"));
   }



   /*******************************************************************************
    ** Duplicate external names are an invalid mapping rejected by the real API.
    *******************************************************************************/
   @Test
   void testDuplicateFieldMappingIsRejectedBeforeWrite() throws Exception
   {
      exposeField("person", "lastName", new ApiFieldMetaData().withInitialVersion(V1).withApiFieldName("givenName"));
      GetTableApiFieldsAction.clearCaches();
      List<String> before = personValues();
      QException error = assertThrows(QException.class,
         () -> ApiImplementation.update(api, V2, "people", "1", "{\"givenName\":\"ambiguous\"}"));
      assertTrue(error.getMessage().contains("givenName"));
      assertTrue(error.getMessage().contains("more than once"));
      assertEquals(before, personValues());
   }



   /*******************************************************************************
    ** Characterize the gap: nonexistent replacement targets are accepted by
    ** metadata validation and read as null. This is not validation success.
    *******************************************************************************/
   @Test
   void testMissingReplacementTargetRemainsAnExplicitValidationGap() throws Exception
   {
      assertFalse(instance.getTable("person").getFields().containsKey("missingTarget"));
      ApiTableMetaDataContainer.of(instance.getTable("person")).getApiTableMetaData(apiName).getRemovedApiFields().get(0)
         .withSupplementalMetaData(new ApiFieldMetaDataContainer().withApiFieldMetaData(apiName,
            new ApiFieldMetaData().withInitialVersion(V1).withFinalVersion(V1).withReplacedByFieldName("missingTarget")));
      GetTableApiFieldsAction.clearCaches();
      assertDoesNotThrow(() -> new QInstanceValidator().revalidate(instance));
      Map<String, Serializable> record = ApiImplementation.get(api, V1, "people", "1");
      assertTrue(record.containsKey("workDays"));
      assertNull(record.get("workDays"));
      assertEquals("1001", personValues().get(2));
   }



   /*******************************************************************************
    ** Fixture-only metadata, attached to the native sample tables.
    *******************************************************************************/
   private void exposeTable(String tableName, ApiTableMetaData metadata)
   {
      instance.getTable(tableName).withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData(apiName, metadata));
      for(String fieldName : instance.getTable(tableName).getFields().keySet())
      {
         exposeField(tableName, fieldName, new ApiFieldMetaData().withIsExcluded(true));
      }
   }



   /*******************************************************************************
    ** Override the fixture's explicit field exclusions with selected exposure.
    *******************************************************************************/
   private void exposeField(String tableName, String fieldName, ApiFieldMetaData metadata)
   {
      instance.getTable(tableName).getField(fieldName).withSupplementalMetaData(new ApiFieldMetaDataContainer().withApiFieldMetaData(apiName, metadata));
   }



   /*******************************************************************************
    ** Direct JDBC checks avoid using the API mapper as its own persistence oracle.
    *******************************************************************************/
   private List<String> personValues() throws Exception
   {
      try(Statement statement = anchor.createStatement();
         ResultSet result = statement.executeQuery("SELECT first_name,last_name,days_worked,email FROM person WHERE id=1"))
      {
         assertTrue(result.next());
         return List.of(result.getString(1), result.getString(2), result.getString(3), result.getString(4));
      }
   }



   /*******************************************************************************
    ** Application-defined mapping invoked by the module in both directions.
    *******************************************************************************/
   public static class FamilyNameMapper extends ApiFieldCustomValueMapper
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public Serializable produceApiValue(QRecord record, String apiFieldName)
      {
         return "family:" + record.getValueString("lastName");
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void consumeApiValue(QRecord record, Object value, JSONObject fullApiJsonObject, String apiFieldName)
      {
         record.setValue("lastName", value.toString().substring("family:".length()));
      }
   }



   /*******************************************************************************
    ** A normal backend step reads the native sample table; the pre-run hook only
    ** tracks the API-assigned UUID so teardown can remove this test's state.
    *******************************************************************************/
   public static class ReadPersonStep implements BackendStep, PreRunApiProcessCustomizer
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         QRecord person = new GetAction().executeForRecord(new GetInput("person").withPrimaryKey(1));
         output.addValue("givenName", person.getValueString("firstName"));
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      @SuppressWarnings("unchecked")
      public void preApiRun(RunProcessInput input)
      {
         ((List<String>) QContext.getObjects().get(PROCESS_IDS)).add(input.getProcessUUID());
      }
   }
}
