/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
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

package com.kingsrook.qqq.backend.core.realtime;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.collaboration.RuntimeMetaDataOverlay;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeature;
import com.kingsrook.qqq.backend.core.model.metadata.collaboration.RecordFeaturesMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.statusmessages.RecordVersionConflictError;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Version preflight works through UpdateAction and preserves stale records.
 *******************************************************************************/
class RecordVersionCheckTest extends BaseTest
{
   /*******************************************************************************
    ** A minimal memory table with a typed version and modify-date default.
    *******************************************************************************/
   @BeforeEach
   void setup() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      instance.addBackend(new QBackendMetaData().withName("realtimeMemory").withBackendType(MemoryBackendModule.class));
      instance.addTable(new QTableMetaData().withName("realtimeRecord").withBackendName("realtimeMemory").withPrimaryKeyField("id")
         .withRecordVersionField("version")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("name", QFieldType.STRING))
         .withField(new QFieldMetaData("version", QFieldType.INTEGER))
         .withField(new QFieldMetaData("modifyDate", QFieldType.DATE_TIME)));
      new QInstanceEnricher(instance).enrich();
      new InsertAction().execute(new InsertInput("realtimeRecord").withRecord(new QRecord().withValue("id", 1).withValue("name", "original").withValue("version", 2)));
   }



   /*******************************************************************************
    ** A stale expected version reports structured details and leaves stored values intact.
    *******************************************************************************/
   @Test
   void staleAndMatchingAndUnconditional() throws Exception
   {
      QRecord stale = new UpdateAction().execute(new UpdateInput("realtimeRecord")
         .withRecord(new QRecord().withValue("id", 1).withValue("name", "stale"))
         .withExpectedVersions(Map.of(1, 1))).getRecords().get(0);
      RecordVersionConflictError conflict = assertInstanceOf(RecordVersionConflictError.class, stale.getErrors().get(0));
      assertEquals(2, conflict.getCurrentVersion());
      assertEquals(List.of("name"), conflict.getChangedFields());
      assertEquals("original", new GetAction().execute(new GetInput("realtimeRecord").withPrimaryKey(1)).getRecord().getValue("name"));
      QRecord matching = new UpdateAction().execute(new UpdateInput("realtimeRecord")
         .withRecord(new QRecord().withValue("id", 1).withValue("name", "matched"))
         .withExpectedVersions(Map.of("1", "2"))).getRecords().get(0);
      assertTrue(matching.getErrors().isEmpty());
      QRecord unconditional = new UpdateAction().execute(new UpdateInput("realtimeRecord")
         .withRecord(new QRecord().withValue("id", 1).withValue("name", "unconditional"))).getRecords().get(0);
      assertTrue(unconditional.getErrors().isEmpty());
   }



   /*******************************************************************************
    ** Enrichment defaults versions from field behavior and respects explicit opt-out.
    *******************************************************************************/
   @Test
   void defaultsAndOptOut() throws Exception
   {
      QTableMetaData table = QContext.getQInstance().getTable("realtimeRecord");
      table.setRecordVersionField(null);
      new QInstanceEnricher(QContext.getQInstance()).enrich();
      assertEquals("modifyDate", table.getRecordVersionField());
      table.setRecordVersionField("");
      new QInstanceEnricher(QContext.getQInstance()).enrich();
      assertEquals("", table.getRecordVersionField());
      assertTrue(new UpdateAction().execute(new UpdateInput("realtimeRecord")
         .withRecord(new QRecord().withValue("id", 1).withValue("name", "allowed"))
         .withExpectedVersions(Map.of(1, "stale"))).getRecords().get(0).getErrors().isEmpty());
   }



   /*******************************************************************************
    ** The listener waits for commit, discards rollback hints and coalesces table changes.
    *******************************************************************************/
   @Test
   void recordChangesAfterCommit() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      RecordFeaturesMetaData.ofOrWithNew(instance).withEnabled(RecordFeature.FOLLOW, true);
      new QInstanceEnricher(instance).enrich();
      List<RealtimeMessage> events = new ArrayList<>();
      try(AutoCloseable subscription = RealtimeBus.get(instance).subscribe(events::add))
      {
         QBackendTransaction transaction = new QBackendTransaction();
         new UpdateAction().execute(new UpdateInput("realtimeRecord").withTransaction(transaction)
            .withRecord(new QRecord().withValue("id", 1).withValue("name", "committed")));
         assertTrue(events.isEmpty());
         transaction.commit();
         assertEquals("record.changed", events.get(0).getEvent());
         assertEquals("record:realtimeRecord:1", events.get(0).getChannel());
         assertEquals(2, events.get(0).getData().get("version"));
         assertTrue(((List<?>) events.get(0).getData().get("fields")).contains("name"));
         assertEquals(1, events.stream().filter(event -> event.getEvent().equals("table.changed")).count());
         QBackendTransaction rolledBack = new QBackendTransaction();
         new UpdateAction().execute(new UpdateInput("realtimeRecord").withTransaction(rolledBack)
            .withRecord(new QRecord().withValue("id", 1).withValue("name", "rolled back")));
         rolledBack.rollback();
         assertEquals(2, events.size());
      }
   }



   /*******************************************************************************
    ** Do not retain test bus executors.
    *******************************************************************************/
   @AfterEach
   void closeBus()
   {
      RealtimeBus.close(QContext.getQInstance());
   }



   /*******************************************************************************
    ** Delete hints carry identifiers; unrelated feature settings do not publish.
    *******************************************************************************/
   @Test
   void deletionAndDisabledFeatures() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      RecordFeaturesMetaData.ofOrWithNew(instance).withEnabled(RecordFeature.FOLLOW, true);
      new QInstanceEnricher(instance).enrich();
      List<RealtimeMessage> events = new ArrayList<>();
      try(AutoCloseable subscription = RealtimeBus.get(instance).subscribe(events::add))
      {
         new DeleteAction().execute(new DeleteInput()
            .withTableName("realtimeRecord").withPrimaryKey(1));
         assertEquals("record.deleted", events.get(0).getEvent());
         assertEquals(Map.of("table", "realtimeRecord", "pk", 1), events.get(0).getData());
         RecordFeaturesMetaData.ofOrWithNew(instance).withEnabled(RecordFeature.FOLLOW, false);
         RuntimeMetaDataOverlay.refresh(instance);
         assertEquals("metadata.changed", events.get(events.size() - 1).getEvent());
         events.clear();
         new InsertAction().execute(new InsertInput("realtimeRecord").withRecord(new QRecord().withValue("id", 2).withValue("name", "quiet")));
         assertTrue(events.isEmpty());
      }
   }



   /*******************************************************************************
    ** ISO timestamp versions from JSON normalize to the stored Instant type.
    *******************************************************************************/
   @Test
   void timestampVersionAndMixedBatch() throws Exception
   {
      QContext.getQInstance().getTable("realtimeRecord").setRecordVersionField("modifyDate");
      QRecord old = new GetAction().execute(new GetInput("realtimeRecord").withPrimaryKey(1)).getRecord();
      var matched = new UpdateAction().execute(new UpdateInput("realtimeRecord")
         .withExpectedVersions(Map.of(1, old.getValue("modifyDate").toString()))
         .withRecord(new QRecord().withValue("id", 1).withValue("name", "timestamp")));
      assertTrue(matched.getRecords().get(0).getErrors().isEmpty());
      QContext.getQInstance().getTable("realtimeRecord").setRecordVersionField("version");
      new InsertAction().execute(new InsertInput("realtimeRecord").withRecord(new QRecord().withValue("id", 2).withValue("name", "second").withValue("version", 3)));
      var batch = new UpdateAction().execute(new UpdateInput("realtimeRecord").withExpectedVersions(Map.of(1, 1, 2, 3))
         .withRecord(new QRecord().withValue("id", 1).withValue("name", "stale"))
         .withRecord(new QRecord().withValue("id", 2).withValue("name", "fresh")));
      assertEquals(1, batch.getRecords().stream().filter(record -> !record.getErrors().isEmpty()).count());
      assertEquals("fresh", new GetAction().execute(new GetInput("realtimeRecord").withPrimaryKey(2)).getRecord().getValue("name"));
   }
}

