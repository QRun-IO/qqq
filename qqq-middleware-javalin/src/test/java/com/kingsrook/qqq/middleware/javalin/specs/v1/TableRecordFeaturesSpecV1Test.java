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

package com.kingsrook.qqq.middleware.javalin.specs.v1;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.MapperFeature;
import com.kingsrook.qqq.backend.core.actions.collaboration.RecordFeatureSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.actions.collaboration.RuntimeMetaDataOverlay;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeatureSummary;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordReference;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.utils.YamlUtils;
import com.kingsrook.qqq.middleware.javalin.specs.AbstractEndpointSpec;
import com.kingsrook.qqq.middleware.javalin.specs.SpecTestBase;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.RecordFeatureSummaryV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.TableMetaDataResponseV1;
import com.kingsrook.qqq.middleware.javalin.specs.v1.responses.components.RecordFeaturesV1;
import kong.unirest.Unirest;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** HTTP feature summary, visibility, frontend publication, and the committed v1 contract.
 *******************************************************************************/
public class TableRecordFeaturesSpecV1Test extends SpecTestBase
{



   /*******************************************************************************
    **
    *******************************************************************************/
   protected AbstractEndpointSpec<?, ?, ?> getSpec()
   {
      return (new TableRecordFeaturesSpecV1());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected String getVersion()
   {
      return ("v1");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected List<AbstractEndpointSpec<?, ?, ?>> getAdditionalSpecs()
   {
      return (List.of(new TableMetaDataSpecV1(), new MetaDataSpecV1()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   protected QInstance defineQInstance() throws QException
   {
      QInstance instance = super.defineQInstance();
      RecordFeaturesMetaData.ofOrWithNew(instance).withSummaryProviders(List.of(new QCodeReference(SummaryProvider.class)));
      RecordFeaturesMetaData.ofOrWithNew(instance.getTable("person")).withEnabled(RecordFeature.COMMENTS, true)
         .withSetting(RecordFeature.COMMENTS, "mode", "plain");
      new QInstanceEnricher(instance).enrich();
      RuntimeMetaDataOverlay.refresh(instance);
      return (instance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void returnsSummaryAndFrontendMetadata()
   {
      var response = Unirest.get(getBaseUrlAndPath() + "/table/person/1/features").asString();
      assertEquals(200, response.getStatus(), response.getBody());
      JSONObject summary = new JSONObject(response.getBody());
      assertEquals("person", summary.getString("tableName"));
      assertEquals("1", summary.getString("primaryKey"));
      assertEquals(List.of("COMMENTS"), summary.getJSONArray("features").toList());
      assertTrue(summary.getJSONObject("capabilities").getBoolean("comment"));
      assertEquals(3, summary.getJSONObject("counts").getInt("comments"));
      assertEquals("ok", summary.getJSONObject("extensions").getJSONObject("test").getString("status"));
      var metadata = Unirest.get(getBaseUrlAndPath() + "/metaData/table/person").asString();
      assertEquals(200, metadata.getStatus(), metadata.getBody());
      JSONObject table = new JSONObject(metadata.getBody());
      assertEquals(1, table.getLong("metaDataGeneration"));
      assertEquals("plain", table.getJSONObject("recordFeatures").getJSONObject("settings").getJSONObject("COMMENTS").getString("mode"));
      assertTrue(table.getJSONObject("recordFeatures").getJSONObject("capabilities").getBoolean("comment"));
      var all = Unirest.get(getBaseUrlAndPath() + "/metaData").asString();
      assertEquals(200, all.getStatus(), all.getBody());
      assertEquals(1, new JSONObject(all.getBody()).getLong("metaDataGeneration"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void invisibleRecordHasSame404AsMissingRecord()
   {
      serverQInstance.addSecurityKeyType(new QSecurityKeyType().withName("lastName"));
      serverQInstance.getTable("person").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("lastName").withFieldName("lastName"));
      var hidden = Unirest.get(getBaseUrlAndPath() + "/table/person/1/features").asString();
      var missing = Unirest.get(getBaseUrlAndPath() + "/table/person/999999/features").asString();
      assertEquals(404, hidden.getStatus(), hidden.getBody());
      assertEquals(404, missing.getStatus(), missing.getBody());
      assertEquals(new JSONObject(missing.getBody()).getString("error"), new JSONObject(hidden.getBody()).getString("error"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void typedSchemasAndCommittedOpenApiMatch() throws Exception
   {
      var schema = new RecordFeatureSummaryV1().toSchema();
      assertTrue(schema.getProperties().get("presence").getNullable());
      assertTrue(schema.getProperties().get("isFavorite").getNullable());
      assertNotNull(schema.getProperties().get("counts").getAdditionalProperties());
      assertNotNull(new RecordFeaturesV1().toSchema().getProperties().get("settings").getAdditionalProperties());
      assertEquals("#/components/schemas/RecordFeaturesV1", new TableMetaDataResponseV1().toSchema().getProperties().get("recordFeatures").getRef());
      var openAPI = new MiddlewareVersionV1().generateOpenAPIModel("qqq");
      String generated = YamlUtils.toYamlCustomized(openAPI, builder -> builder.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true));
      Path committed = Path.of("src/main/resources/openapi/v1/openapi.yaml");
      if(Boolean.getBoolean("qqq.updateOpenApi"))
      {
         Files.writeString(committed, generated);
      }
      Files.writeString(Path.of("target/openapi-generated.yaml"), generated);
      assertTrue(Files.readString(committed).replace("\r\n", "\n").equals(generated.replace("\r\n", "\n")),
         "Committed OpenAPI differs from target/openapi-generated.yaml; review the diff and regenerate with -Dqqq.updateOpenApi=true");
      assertTrue(generated.contains("/qqq/v1/table/{tableName}/{primaryKey}/features:"));
      assertTrue(generated.contains("#/components/schemas/RecordFeaturesV1"));
   }



   /*******************************************************************************
    ** Summary provider only supplies optional values.
    *******************************************************************************/
   public static class SummaryProvider implements RecordFeatureSummaryProviderInterface
   {



      /*******************************************************************************
       **
       *******************************************************************************/
      public void contribute(RecordReference reference, QRecord parent, RecordFeatureSummary summary)
      {
         summary.getCounts().put("comments", 3);
         summary.getExtensions().put("test", new java.util.LinkedHashMap<>(Map.of("status", "ok")));
      }
   }

}
