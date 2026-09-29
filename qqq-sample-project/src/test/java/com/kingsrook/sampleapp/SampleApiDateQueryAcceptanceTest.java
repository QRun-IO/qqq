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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.JsonNode;
import com.kingsrook.qqq.api.actions.GetTableApiFieldsAction;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.actions.ApiFieldCustomValueMapper;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaData;
import com.kingsrook.qqq.api.model.metadata.fields.ApiFieldMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaData;
import com.kingsrook.qqq.api.model.metadata.tables.ApiTableMetaDataContainer;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrCountInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Real application API routes must convert DATE criteria before native queries.
 *******************************************************************************/
class SampleApiDateQueryAcceptanceTest
{
   private QInstance instance;
   private SampleOpenApiHttpFixture http;



   /*******************************************************************************
    ** Normal insertion stores typed dates; historical metadata replaces a STRING field.
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
         .withName("dates").withPath("/dates/").withLabel("Owned date API").withDescription("Date query acceptance")
         .withContactEmail("owner@example.test").withCurrentVersion(new APIVersion("2026.Q3"))
         .withSupportedVersions(List.of(new APIVersion("2026.Q1"), new APIVersion("2026.Q3"))).withSecuritySchemes(Map.of())));
      QFieldMetaData oldDate = new QFieldMetaData("oldDate", QFieldType.STRING).withSupplementalMetaData(new ApiFieldMetaDataContainer()
         .withApiFieldMetaData("dates", new ApiFieldMetaData().withApiFieldName("oldDay").withInitialVersion("2026.Q1")
            .withFinalVersion("2026.Q1").withReplacedByFieldName("dueDate")));
      instance.addTable(new QTableMetaData().withName("datedRecord").withBackendName("memory").withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("dueDate", QFieldType.DATE).withSupplementalMetaData(new ApiFieldMetaDataContainer()
            .withApiFieldMetaData("dates", new ApiFieldMetaData().withApiFieldName("day").withInitialVersion("2026.Q3"))))
         .withField(new QFieldMetaData("dateText", QFieldType.STRING))
         .withSupplementalMetaData(new ApiTableMetaDataContainer().withApiTableMetaData("dates", new ApiTableMetaData()
            .withApiTableName("records").withInitialVersion("2026.Q1").withRemovedApiField(oldDate))));
      QContext.init(instance, new QSession());
      try
      {
         new QInstanceEnricher(instance).enrich();
         new InsertAction().execute(new InsertInput("datedRecord").withRecords(List.of(
            new QRecord().withValue("id", 1).withValue("dueDate", LocalDate.of(2000, 1, 1)).withValue("dateText", "2000-01-01"),
            new QRecord().withValue("id", 2).withValue("dueDate", LocalDate.of(2001, 2, 2)).withValue("dateText", "second"),
            new QRecord().withValue("id", 3).withValue("dateText", "undated"))));
      }
      finally
      {
         QContext.clear();
      }
      http = new SampleOpenApiHttpFixture(instance);
   }



   /*******************************************************************************
    ** Release the owned server and all fixture metadata state.
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
    ** Both public versions must select exactly the record represented by the date.
    *******************************************************************************/
   @Test
   void testCurrentAndHistoricalDateEquality() throws Exception
   {
      for(String value : List.of("2000-01-01", "1/1/2000", "20000101"))
      {
         JsonNode current = query("2026.Q3", "day", value, 200);
         assertIds(current, List.of(1));
         assertEquals("2000-01-01", current.at("/records/0/day").asText());
         JsonNode historical = query("2026.Q1", "oldDay", value, 200);
         assertIds(historical, List.of(1));
         assertEquals("2000-01-01", historical.at("/records/0/oldDay").asText());
      }
      assertIds(query("2026.Q3", "day", "1999-01-01", 200), List.of());
      assertIds(query("2026.Q3", "day", "IN 2000-01-01,2001-02-02", 200), List.of(2, 1));
      assertIds(query("2026.Q3", "day", "BETWEEN 2000-01-01,2000-12-31", 200), List.of(1));
   }



   /*******************************************************************************
    ** Existing blank and STRING criteria retain their meanings and version scope.
    *******************************************************************************/
   @Test
   void testBlankStringAndVersionControls() throws Exception
   {
      assertIds(query("2026.Q3", "day", "EMPTY", 200), List.of(3));
      assertIds(query("2026.Q3", "day", "!EMPTY", 200), List.of(2, 1));
      assertIds(query("2026.Q3", "dateText", "2000-01-01", 200), List.of(1));
      assertIds(query("2026.Q3", "dateText", "LIKE 2000%", 200), List.of(1));
      for(String field : List.of("oldDay", "dueDate", "unknown"))
      {
         assertTrue(query("2026.Q3", field, "2000-01-01", 400).path("error").asText().contains(field));
      }
      assertTrue(query("2026.Q1", "day", "2000-01-01", 400).path("error").asText().contains("day"));
   }



   /*******************************************************************************
    ** Conversion failures use the served bad-request response, never an uncaught 500.
    *******************************************************************************/
   @Test
   void testInvalidDateValuesReturnBadRequest() throws Exception
   {
      for(String version : List.of("2026.Q1", "2026.Q3"))
      {
         String field = "2026.Q1".equals(version) ? "oldDay" : "day";
         for(String value : List.of("not-a-date", "IN 2000-01-01,not-a-date", "BETWEEN 2000-01-01,not-a-date"))
         {
            JsonNode error = query(version, field, value, 400);
            assertTrue(error.path("error").asText().contains("not-a-date"));
            assertFalse(error.has("records"));
         }
      }
   }



   /*******************************************************************************
    ** A DATE custom mapper can continue accepting its own non-date input language.
    *******************************************************************************/
   @Test
   void testCustomMapperRetainsRawInput() throws Exception
   {
      ApiFieldMetaDataContainer.of(instance.getTable("datedRecord").getField("dueDate")).getApiFieldMetaData("dates")
         .withCustomValueMapper(new QCodeReference(DateTokenMapper.class));
      assertIds(query("2026.Q3", "day", "owned-token", 200), List.of(2));
   }



   /*******************************************************************************
    ** Use the shared real HTTP transport with encoded query values.
    *******************************************************************************/
   private JsonNode query(String version, String field, String value, Integer status) throws Exception
   {
      var response = http.get("/dates/" + version + "/records/query?" + field + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8));
      assertEquals(status.intValue(), response.statusCode());
      return JsonUtils.toObject(response.body(), JsonNode.class);
   }



   /*******************************************************************************
    ** Check count, cardinality and ordered identity, including empty result arrays.
    *******************************************************************************/
   private void assertIds(JsonNode result, List<Integer> ids)
   {
      assertEquals(ids.size(), result.path("count").asInt());
      assertTrue(result.path("records").isArray());
      assertEquals(ids.size(), result.path("records").size());
      for(int i = 0; i < ids.size(); i++)
      {
         assertEquals(ids.get(i).intValue(), result.path("records").get(i).path("id").asInt());
      }
   }



   /*******************************************************************************
    ** A raw token maps to a different native field before query execution.
    *******************************************************************************/
   public static class DateTokenMapper extends ApiFieldCustomValueMapper
   {
      /*******************************************************************************
       ** Assert the mapper's public alias and raw value contract directly.
       *******************************************************************************/
      @Override
      public void customizeFilterCriteriaForQueryOrCount(QueryOrCountInputInterface input, QQueryFilter filter, QFilterCriteria criteria, String apiFieldName, ApiFieldMetaData metadata)
      {
         assertEquals("day", apiFieldName);
         assertEquals("day", criteria.getFieldName());
         assertEquals(List.of("owned-token"), criteria.getValues());
         criteria.setFieldName("id");
         criteria.setValues(List.of(2));
      }
   }
}
