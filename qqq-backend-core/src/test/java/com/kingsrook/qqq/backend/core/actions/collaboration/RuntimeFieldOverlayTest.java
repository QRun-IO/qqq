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

package com.kingsrook.qqq.backend.core.actions.collaboration;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.ai.AiDataClassifications;
import com.kingsrook.qqq.backend.core.actions.ai.AiFieldMetaData;
import com.kingsrook.qqq.backend.core.actions.ai.DataClassification;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordOwnership;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RuntimeMetaDataContribution;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RuntimeTableMetaDataContribution;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QVirtualFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.frontend.QFrontendTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.realtime.InMemoryRealtimeBus;
import com.kingsrook.qqq.backend.core.realtime.RealtimeBus;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMessage;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMetaData;
import com.kingsrook.qqq.backend.core.utils.TestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Runtime custom field metadata and storage.
 *******************************************************************************/
public class RuntimeFieldOverlayTest extends BaseTest
{
   private QInstance instance;
   private QTableMetaData original;



   /*******************************************************************************
    ** Start from validated metadata and an empty provider.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      instance = QContext.getQInstance();
      original = new QTableMetaData().withName("runtimeTest").withBackendName(TestUtils.MEMORY_BACKEND_NAME).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
      instance.addTable(original);
      new QInstanceValidator().validate(instance);
      Provider.contribution = new RuntimeMetaDataContribution();
      RecordFeaturesMetaData.ofOrWithNew(instance).withRuntimeProviders(List.of(new QCodeReference(Provider.class)));
   }



   /*******************************************************************************
    ** Copy-on-write publication, sections, search, frontend metadata and removal.
    *******************************************************************************/
   @Test
   void addsAndRemovesFields() throws Exception
   {
      RuntimeTableMetaDataContribution table = new RuntimeTableMetaDataContribution()
         .withFields(List.of(JsonPathStorageTest.field("custom", QFieldType.STRING)))
         .withSectionPlacements(Map.of("custom", "extra")).withSearchFields(List.of("custom"));
      Provider.contribution.withTables(Map.of("runtimeTest", table));
      RuntimeMetaDataOverlay.refresh(instance);
      QTableMetaData published = instance.getTable("runtimeTest");
      assertNotSame(original, published);
      assertFalse(original.getFields().containsKey("custom"));
      assertEquals("Custom", published.getField("custom").getLabel());
      assertEquals("Custom fields", published.getSection("extra").getLabel());
      assertEquals("extra", published.getSections().get(published.getSections().size() - 1).getName());
      assertEquals(List.of("custom"), published.getSearchFields());
      QFrontendTableMetaData frontend = new QFrontendTableMetaData(new QueryInput(), instance.getBackendForTable("runtimeTest"), published, true, false);
      assertTrue(frontend.getFields().containsKey("custom"));
      assertTrue(instance.getHasBeenValidated());
      assertEquals(1, RuntimeMetaDataOverlay.getGeneration(instance));
      // Repeated refresh must replace rather than accumulate fields and sections.
      RuntimeMetaDataOverlay.refresh(instance);
      assertEquals(2, instance.getTable("runtimeTest").getFields().size());
      Provider.contribution.withTables(Map.of());
      RuntimeMetaDataOverlay.refresh(instance);
      assertEquals(3, RuntimeMetaDataOverlay.getGeneration(instance));
      assertFalse(instance.getTable("runtimeTest").getFields().containsKey("custom"));
      assertNull(instance.getTable("runtimeTest").getSection("extra"));
      assertNull(instance.getTable("runtimeTest").getSearchFields());
      assertTrue(published.getFields().containsKey("custom"));
   }



   /*******************************************************************************
    ** A bad table retains its accepted generation while code fields win clashes.
    *******************************************************************************/
   @Test
   void clashesAndInvalidContributions() throws Exception
   {
      Provider.contribution.withTables(Map.of("runtimeTest", new RuntimeTableMetaDataContribution()
         .withFields(List.of(JsonPathStorageTest.field("id", QFieldType.STRING), JsonPathStorageTest.field("custom", QFieldType.STRING)))));
      RuntimeMetaDataOverlay.refresh(instance);
      QTableMetaData accepted = instance.getTable("runtimeTest");
      assertEquals(QFieldType.INTEGER, accepted.getField("id").getType());
      assertNotNull(accepted.getField("custom"));
      QFieldMetaData invalid = JsonPathStorageTest.field("bad", QFieldType.BLOB);
      Provider.contribution.withTables(Map.of("runtimeTest", new RuntimeTableMetaDataContribution().withFields(List.of(invalid))));
      RuntimeMetaDataOverlay.refresh(instance);
      assertSame(accepted, instance.getTable("runtimeTest"));
      assertEquals(2, RuntimeMetaDataOverlay.getGeneration(instance));
      Provider.contribution.withTables(Map.of());
      RuntimeMetaDataOverlay.refresh(instance);
      assertEquals(List.of("id"), new ArrayList<>(instance.getTable("runtimeTest").getFields().keySet()));
   }



   /*******************************************************************************
    ** Virtual fields are contributed independently of physical form sections.
    *******************************************************************************/
   @Test
   void addsVirtualFieldsAndIsolatesTables() throws Exception
   {
      instance.addTable(new QTableMetaData().withName("otherRuntime").withBackendName(TestUtils.MEMORY_BACKEND_NAME).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER)));
      new QInstanceValidator().revalidate(instance);
      QTableMetaData other = instance.getTable("otherRuntime");
      Provider.contribution.withTables(Map.of(
         "runtimeTest", new RuntimeTableMetaDataContribution().withVirtualFields(List.of(new QVirtualFieldMetaData("computed", QFieldType.STRING))),
         "otherRuntime", new RuntimeTableMetaDataContribution().withFields(List.of(JsonPathStorageTest.field("bad", QFieldType.BLOB)))));
      RuntimeMetaDataOverlay.refresh(instance);
      assertNotNull(instance.getTable("runtimeTest").getVirtualField("computed"));
      assertSame(other, instance.getTable("otherRuntime"));
      Provider.contribution.withTables(Map.of());
      RuntimeMetaDataOverlay.refresh(instance);
      assertNull(instance.getTable("runtimeTest").getVirtualField("computed"));
   }



   /*******************************************************************************
    ** Ownership validates contributed fields and rejects removing a referenced field.
    *******************************************************************************/
   @Test
   void validatesRuntimeOwnershipAndPublishesBuiltInView() throws Exception
   {
      RecordFeaturesMetaData.ofOrWithNew(original).withEnabled(RecordFeature.ASSIGNMENT, true)
         .withOwnership(new RecordOwnership().withOwnerFieldName("cf_owner"));
      Provider.contribution.withTables(Map.of("runtimeTest", new RuntimeTableMetaDataContribution()
         .withFields(List.of(JsonPathStorageTest.field("cf_owner", QFieldType.STRING)))));
      RuntimeMetaDataOverlay.refresh(instance);
      QTableMetaData accepted = instance.getTable("runtimeTest");
      assertNotSame(original, accepted);
      QFrontendTableMetaData frontend = new QFrontendTableMetaData(new QueryInput(), instance.getBackendForTable("runtimeTest"), accepted, true, false);
      assertTrue(frontend.getFields().containsKey("cf_owner"));
      assertEquals("cf_owner", frontend.getBuiltInViews().get(0).getFilter().getCriteria().get(0).getFieldName());
      Provider.contribution.withTables(Map.of());
      RuntimeMetaDataOverlay.refresh(instance);
      assertSame(accepted, instance.getTable("runtimeTest"));
      Provider.contribution.withTables(Map.of("runtimeTest", new RuntimeTableMetaDataContribution()
         .withFields(List.of(JsonPathStorageTest.field("cf_owner", QFieldType.BOOLEAN)))));
      RuntimeMetaDataOverlay.refresh(instance);
      assertSame(accepted, instance.getTable("runtimeTest"));
   }



   /*******************************************************************************
    ** Supplemental classification survives both contribution clones and table enrichment.
    *******************************************************************************/
   @Test
   void preservesContributedDataClassification() throws Exception
   {
      QFieldMetaData field = JsonPathStorageTest.field("cf_x", QFieldType.STRING)
         .withSupplementalMetaData(new AiFieldMetaData().withClassification(DataClassification.PHI).withAiDescription("Sensitive custom field"));
      Provider.contribution.withTables(Map.of("runtimeTest", new RuntimeTableMetaDataContribution().withFields(List.of(field))));
      RuntimeMetaDataOverlay.refresh(instance);
      QTableMetaData published = instance.getTable("runtimeTest");
      assertNotSame(original, published);
      assertNotSame(field, published.getField("cf_x"));
      assertFalse(original.getFields().containsKey("cf_x"));
      assertEquals(DataClassification.PHI, AiDataClassifications.ofField(published, "cf_x"));
      assertEquals("Sensitive custom field", AiFieldMetaData.of(published.getField("cf_x")).getAiDescription());
      RuntimeMetaDataOverlay.refresh(instance);
      assertNotSame(published, instance.getTable("runtimeTest"));
      assertEquals(DataClassification.PHI, AiDataClassifications.ofField(instance.getTable("runtimeTest"), "cf_x"));
      assertEquals(DataClassification.PHI, AiDataClassifications.ofField(published, "cf_x"));
   }



   /*******************************************************************************
    ** Subscribers see the swapped tables and the generation carried by the event.
    *******************************************************************************/
   @Test
   void publishesMetadataAfterSwap() throws Exception
   {
      Provider.contribution.withGeneration(10).withTables(Map.of("runtimeTest", new RuntimeTableMetaDataContribution()
         .withFields(List.of(JsonPathStorageTest.field("custom", QFieldType.STRING)))));
      List<RealtimeMessage> events = new ArrayList<>();
      List<QTableMetaData> observedTables = new ArrayList<>();
      List<Long> observedGenerations = new ArrayList<>();
      try(AutoCloseable subscription = RealtimeBus.get(instance).subscribe(event ->
      {
         events.add(event);
         observedTables.add(instance.getTable("runtimeTest"));
         observedGenerations.add(RuntimeMetaDataOverlay.getGeneration(instance));
      }))
      {
         RuntimeMetaDataOverlay.refresh(instance);
         assertEquals(1, events.size());
         assertEquals("instance", events.get(0).getChannel());
         assertEquals("metadata.changed", events.get(0).getEvent());
         assertEquals(Map.of("generation", 10L), events.get(0).getData());
         assertSame(instance.getTable("runtimeTest"), observedTables.get(0));
         assertNotNull(observedTables.get(0).getField("custom"));
         assertEquals(List.of(10L), observedGenerations);
         RuntimeMetaDataOverlay.refresh(instance);
         assertEquals(2, events.size());
         assertEquals(Map.of("generation", 11L), events.get(1).getData());
      }
   }



   /*******************************************************************************
    ** A failed external bus cannot undo or fail an accepted metadata refresh.
    *******************************************************************************/
   @Test
   void publishFailureDoesNotFailRefresh() throws Exception
   {
      instance.withSupplementalMetaData(new RealtimeMetaData().withBus(new QCodeReference(FailingBus.class)));
      Provider.contribution.withTables(Map.of("runtimeTest", new RuntimeTableMetaDataContribution()
         .withFields(List.of(JsonPathStorageTest.field("custom", QFieldType.STRING)))));
      RuntimeMetaDataOverlay.refresh(instance);
      assertEquals(1, RuntimeMetaDataOverlay.getGeneration(instance));
      assertNotNull(instance.getTable("runtimeTest").getField("custom"));
      assertNotSame(original, instance.getTable("runtimeTest"));
   }



   /*******************************************************************************
    ** Release realtime executors created by refresh.
    *******************************************************************************/
   @AfterEach
   void closeBus()
   {
      RealtimeBus.close(instance);
   }



   /*******************************************************************************
    ** Simulate a transport failure at the bus boundary.
    *******************************************************************************/
   public static class FailingBus extends InMemoryRealtimeBus
   {
      /*******************************************************************************
       ** Refuse publication.
       *******************************************************************************/
      @Override
      public void publish(RealtimeMessage message)
      {
         throw (new IllegalStateException("Test bus unavailable"));
      }
   }



   /*******************************************************************************
    ** Provider used to simulate committed metadata changes.
    *******************************************************************************/
   public static class Provider implements RuntimeMetaDataProviderInterface
   {
      private static RuntimeMetaDataContribution contribution;



      /*******************************************************************************
       ** Return the current contribution.
       *******************************************************************************/
      @Override
      public RuntimeMetaDataContribution getContribution(QInstance instance)
      {
         return (contribution);
      }
   }
}

