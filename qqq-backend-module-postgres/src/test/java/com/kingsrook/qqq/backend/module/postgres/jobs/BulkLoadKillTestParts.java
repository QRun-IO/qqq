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


package com.kingsrook.qqq.backend.module.postgres.jobs;


import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.alerts.AlertResolution;
import com.kingsrook.qqq.backend.core.alerts.AlertSignal;
import com.kingsrook.qqq.backend.core.alerts.AlertSinkInterface;
import com.kingsrook.qqq.backend.core.alerts.AlertsInstanceMetaData;
import com.kingsrook.qqq.backend.core.alerts.NotificationSignal;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.audits.AuditsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.audits.AuditLevel;
import com.kingsrook.qqq.backend.core.model.metadata.audits.QAuditRules;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.realtime.RealtimeBusInterface;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMessage;
import com.kingsrook.qqq.backend.core.realtime.RealtimeMetaData;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;


/*******************************************************************************
 ** The bulk-load parts of the kill-the-pod tests (spec section 6.1): shared
 ** chunk-object storage (local files here; S3 in production), a target table
 ** with a unique key and slow inserts (so a pod can be killed mid-chunk),
 ** audits with the client channel, and an alert sink and realtime bus that
 ** record what they get into a table every JVM can see.
 *******************************************************************************/
public class BulkLoadKillTestParts
{
   public static final String TABLE_STORAGE = "jobs_storage_fs";
   public static final String TABLE_TARGET  = "kill_test_target";
   public static final String TABLE_SIGNAL  = "kill_test_signal";
   public static final String TABLE_LOAD    = "kill_test_load";
   public static final String BACKEND_FS    = "jobsfs";

   public static final int INSERT_MILLIS_PER_ROW = 3;



   /*******************************************************************************
    **
    *******************************************************************************/
   public static void add(QInstance instance, String backendName, String storageBasePath) throws QException
   {
      instance.addBackend(new FilesystemBackendMetaData().withBasePath(storageBasePath).withName(BACKEND_FS));
      instance.addTable(new QTableMetaData()
         .withName(TABLE_STORAGE)
         .withBackendName(BACKEND_FS)
         .withPrimaryKeyField("fileName")
         .withField(new QFieldMetaData("fileName", QFieldType.STRING))
         .withField(new QFieldMetaData("contents", QFieldType.BLOB))
         .withBackendDetails(new FilesystemTableBackendDetails()
            .withBasePath("jobs")
            .withCardinality(Cardinality.ONE)
            .withFileNameFieldName("fileName")
            .withContentsFieldName("contents")));

      instance.addTable(new QTableMetaData()
         .withName(TABLE_TARGET)
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("code"))
         .withAuditRules(new QAuditRules().withAuditLevel(AuditLevel.RECORD))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("code", QFieldType.STRING).withIsRequired(true))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true))
         .withField(new QFieldMetaData("amount", QFieldType.INTEGER))
         .withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(SlowInsert.class)));

      ////////////////////////////////////////////////////////////////
      // the 1M-row load target: plain inserts, no customizer, no audits //
      ////////////////////////////////////////////////////////////////
      instance.addTable(new QTableMetaData()
         .withName(TABLE_LOAD)
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withUniqueKey(new UniqueKey("code"))
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("code", QFieldType.STRING).withIsRequired(true))
         .withField(new QFieldMetaData("name", QFieldType.STRING).withIsRequired(true))
         .withField(new QFieldMetaData("amount", QFieldType.INTEGER)));

      instance.addTable(new QTableMetaData()
         .withName(TABLE_SIGNAL)
         .withBackendName(backendName)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("kind", QFieldType.STRING))
         .withField(new QFieldMetaData("channel", QFieldType.STRING))
         .withField(new QFieldMetaData("detail", QFieldType.TEXT))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("create_date")));

      new AuditsMetaDataProvider().withClientChannelField(true).defineAll(instance, backendName, table ->
      {
         DurableJobsMetaDataProvider.snakeCaseColumnNames().accept(table);
         table.withBackendDetails(new RDBMSTableBackendDetails().withTableName(table.getName().replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase()));
      });

      AlertsInstanceMetaData.ofOrWithNew(instance).withSink(new QCodeReference(RecordingSink.class));
      instance.withSupplementalMetaData(RealtimeMetaData.of(instance).withBus(new QCodeReference(RecordingBus.class)));
   }



   /*******************************************************************************
    ** Record one signal row (as the system user).
    *******************************************************************************/
   static void record(String kind, String channel, String detail)
   {
      try
      {
         SystemScope.call(() -> new InsertAction().execute(new InsertInput(TABLE_SIGNAL).withRecord(new QRecord()
            .withValue("kind", kind).withValue("channel", channel).withValue("detail", detail).withValue("createDate", Instant.now()))));
      }
      catch(Exception e)
      {
         throw (new RuntimeException("Could not record a test signal", e));
      }
   }



   /*******************************************************************************
    ** Slows inserts so a chunk takes long enough to be killed mid-way.
    *******************************************************************************/
   public static class SlowInsert implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
      {
         if(!isPreview)
         {
            SleepUtils.sleep((long) INSERT_MILLIS_PER_ROW * records.size(), TimeUnit.MILLISECONDS);
         }
         return (records);
      }
   }



   /*******************************************************************************
    ** Records notifications (and failure alerts) into kill_test_signal.
    *******************************************************************************/
   public static class RecordingSink implements AlertSinkInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void raise(AlertSignal signal)
      {
         record("raise", signal.getAlertTypeName(), signal.getTitle());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void resolve(AlertResolution resolution)
      {
         record("resolve", resolution.getAlertTypeName(), resolution.getDedupKey());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void heartbeat(String heartbeatKey, Instant at)
      {
         // not recorded
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void notify(NotificationSignal signal)
      {
         record("notify", signal.getSourceRecordId(), signal.getTitle() + " | " + signal.getBody());
      }
   }



   /*******************************************************************************
    ** Records job.progress and job.status events into kill_test_signal.
    *******************************************************************************/
   public static class RecordingBus implements RealtimeBusInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void publish(RealtimeMessage message)
      {
         if(message.getEvent() != null && message.getEvent().startsWith("job.") && message.getChannel().startsWith("record:"))
         {
            record(message.getEvent(), message.getChannel(), String.valueOf(message.getData()));
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public AutoCloseable subscribe(java.util.function.Consumer<RealtimeMessage> listener)
      {
         return (() ->
         {
         });
      }
   }

}
