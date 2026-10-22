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


import java.io.Serializable;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableProcessMetaData;
import com.kingsrook.qqq.backend.core.jobs.SystemScope;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectDispatcher;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectKeys;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectRequest;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectResult;
import com.kingsrook.qqq.backend.core.jobs.effects.EffectTarget;
import com.kingsrook.qqq.backend.core.jobs.effects.JobEffectDeclaration;
import com.kingsrook.qqq.backend.core.jobs.effects.JobEffectType;
import com.kingsrook.qqq.backend.core.jobs.effects.JobEffects;
import com.kingsrook.qqq.backend.core.jobs.effects.ReconcileResult;
import com.kingsrook.qqq.backend.core.jobs.effects.targets.HttpEffectTarget;
import com.kingsrook.qqq.backend.core.jobs.effects.targets.TableUpsertEffectTarget;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.LoadViaUpdateStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.C3P0PooledConnectionProvider;


/*******************************************************************************
 ** The side-effect parts of the fault-injection tests (spec section 2.13): a
 ** process that, per customer, updates the customer in the jobs' own database
 ** (LOCAL_TX) and emits four external effects - an email (non-idempotent), an
 ** S3 PUT (idempotent by deterministic key), an HTTP call (Idempotency-Key)
 ** and an upsert into another database (natural key). The stubs record every
 ** call in tables (or the test JVM's HTTP server) every JVM can see.
 **
 ** Pods read their settings from system properties: dispatcher threads, and a
 ** fault - point:effectName:n - at which the pod halts (kill -9 semantics: no
 ** shutdown hooks, nothing flushed).
 *******************************************************************************/
public class EffectsKillTestParts
{
   public static final String PROCESS        = "effectsKillTest";
   public static final String TABLE_CUSTOMER = "effects_test_customer";
   public static final String TABLE_EMAIL    = "stub_email_sent";
   public static final String TABLE_S3_PUT   = "stub_s3_put";
   public static final String TABLE_S3_OBJ   = "stub_s3_object";
   public static final String TABLE_OTHER_DB = "other_db_customer_flag";
   public static final String BACKEND_OTHER  = "otherdb";

   public static final String PROPERTY_ENABLED     = "qqq.jobs.test.effects";
   public static final String PROPERTY_DISPATCHERS = "qqq.jobs.test.dispatchers";
   public static final String PROPERTY_FAULT       = "qqq.jobs.test.effectFault";
   public static final String PROPERTY_HTTP_STUB   = "qqq.jobs.test.httpStub";

   public static final int EFFECT_LEASE_TTL = 5;
   public static final int CHUNK_SIZE       = 5;

   public static final String EMAIL_DOMAIN = "effects.test";



   /*******************************************************************************
    ** Add the tables, targets, process and effect settings.
    *******************************************************************************/
   public static void add(QInstance instance, String backendName, String host, int port, String database, String user, String password) throws QException
   {
      PostgreSQLBackendMetaData other = new PostgreSQLBackendMetaData().withName(BACKEND_OTHER).withHostName(host).withPort(port)
         .withDatabaseName(database).withUsername(user).withPassword(password);
      other.setConnectionProvider(new QCodeReference(C3P0PooledConnectionProvider.class));
      instance.addBackend(other);

      instance.addTable(new QTableMetaData().withName(TABLE_CUSTOMER).withBackendName(backendName).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("code", QFieldType.STRING))
         .withField(new QFieldMetaData("email", QFieldType.STRING))
         .withField(new QFieldMetaData("visits", QFieldType.INTEGER)));
      instance.addTable(new QTableMetaData().withName(TABLE_EMAIL).withBackendName(backendName).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("messageId", QFieldType.STRING).withBackendName("message_id"))
         .withField(new QFieldMetaData("recipient", QFieldType.STRING))
         .withField(new QFieldMetaData("podId", QFieldType.STRING).withBackendName("pod_id")));
      instance.addTable(new QTableMetaData().withName(TABLE_S3_PUT).withBackendName(backendName).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("objectKey", QFieldType.STRING).withBackendName("object_key"))
         .withField(new QFieldMetaData("podId", QFieldType.STRING).withBackendName("pod_id")));
      instance.addTable(new QTableMetaData().withName(TABLE_S3_OBJ).withBackendName(backendName).withPrimaryKeyField("objectKey")
         .withField(new QFieldMetaData("objectKey", QFieldType.STRING).withBackendName("object_key"))
         .withField(new QFieldMetaData("content", QFieldType.TEXT)));
      instance.addTable(new QTableMetaData().withName(TABLE_OTHER_DB).withBackendName(BACKEND_OTHER).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER).withIsEditable(false))
         .withField(new QFieldMetaData("code", QFieldType.STRING))
         .withField(new QFieldMetaData("flag", QFieldType.STRING)));

      DurableJobsMetaData.of(instance)
         .withEffectTarget("email", new QCodeReference(StubEmailTarget.class))
         .withEffectTarget("s3", new QCodeReference(StubS3Target.class))
         .withEffectTarget("http", new QCodeReference(StubHttpTarget.class))
         .withEffectTarget("otherdb", new QCodeReference(TableUpsertEffectTarget.class))
         .withEffectLeaseTtlSeconds(EFFECT_LEASE_TTL)
         .withEffectBatchSize(20)
         .withEffectRetryBaseSeconds(1)
         .withEffectReconcileAttempts(3)
         .withEffectDispatcherThreads(Integer.parseInt(System.getProperty(PROPERTY_DISPATCHERS, "1")));

      QProcessMetaData process = StreamedETLWithFrontendProcess.processMetaDataBuilder()
         .withName(PROCESS)
         .withTableName(TABLE_CUSTOMER)
         .withSourceTable(TABLE_CUSTOMER)
         .withDestinationTable(TABLE_CUSTOMER)
         .withExtractStepClass(ExtractViaQueryStep.class)
         .withTransformStepClass(VisitAndNotify.class)
         .withLoadStepClass(LoadViaUpdateStep.class)
         .getProcessMetaData();
      process.withSupplementalMetaData(new DurableProcessMetaData().withEligible(true).withChunkSize(CHUNK_SIZE)
         .withEffect(new JobEffectDeclaration("welcomeEmail", null, "email", JobEffectType.NON_IDEMPOTENT_EXTERNAL))
         .withEffect(new JobEffectDeclaration("receiptToS3", null, "s3", JobEffectType.IDEMPOTENT_EXTERNAL))
         .withEffect(new JobEffectDeclaration("notifyPartner", null, "http", JobEffectType.IDEMPOTENT_EXTERNAL))
         .withEffect(new JobEffectDeclaration("flagInOtherDb", null, "otherdb", JobEffectType.IDEMPOTENT_EXTERNAL)));
      instance.addProcess(process);
   }



   /*******************************************************************************
    ** The fault hook from the pod's system property (or none).
    *******************************************************************************/
   public static EffectDispatcher.FaultHook faultHookFromProperty()
   {
      String fault = System.getProperty(PROPERTY_FAULT);
      if(fault == null || fault.isBlank())
      {
         return (EffectDispatcher.FaultHook.NONE);
      }
      String[]      parts  = fault.split(":");
      String        point  = parts[0];
      String        effect = parts[1];
      int           nth    = Integer.parseInt(parts[2]);
      AtomicInteger seen   = new AtomicInteger();
      return ((at, record) ->
      {
         if(at.equals(point) && ("*".equals(effect) || effect.equals(record.getValueString("effectName"))) && seen.incrementAndGet() == nth)
         {
            System.out.println("HALT at " + at + " " + record.getValueString("effectName") + " " + record.getValueString("rowKey"));
            System.out.flush();
            Runtime.getRuntime().halt(137);
         }
      });
   }



   /*******************************************************************************
    ** Per customer: one more visit (LOCAL_TX, in the chunk's transaction) and
    ** four external effects through the outbox.
    *******************************************************************************/
   public static class VisitAndNotify extends AbstractTransformStep
   {
      private final ProcessSummaryLine ok = new ProcessSummaryLine(Status.OK, 0, "visited");



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         for(QRecord customer : input.getRecords())
         {
            String id   = String.valueOf(customer.getValue("id"));
            String code = customer.getValueString("code");
            output.addRecord(new QRecord().withValue("id", customer.getValue("id")).withValue("visits", customer.getValueInteger("visits") + 1));

            JobEffects.emit(input, "welcomeEmail", id, Map.of("to", customer.getValueString("email")));
            JobEffects.emit(input, "receiptToS3", id, Map.of("content", "{\"customer\":\"" + code + "\"}"));
            JobEffects.emit(input, "notifyPartner", id, Map.of("code", code));
            Map<String, Serializable> values = new LinkedHashMap<>();
            values.put("code", code);
            values.put("flag", "visited");
            JobEffects.emit(input, "flagInOtherDb", id, Map.of("tableName", TABLE_OTHER_DB, "keyFields", new ArrayList<>(List.of("code")), "values", new LinkedHashMap<>(values)));
            ok.incrementCountAndAddPrimaryKey(customer.getValue("id"));
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         return (new ArrayList<>(List.of(ok)));
      }
   }



   /*******************************************************************************
    ** An email server stub: every send is logged (it does NOT dedupe - a resend
    ** would show as a second row). Non-idempotent; reconcile looks the fixed
    ** Message-ID up in the log, as a provider's message search would.
    *******************************************************************************/
   public static class StubEmailTarget implements EffectTarget
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public EffectResult deliver(EffectRequest request) throws Exception
      {
         String messageId = EffectKeys.messageId(request.idempotencyKey(), EMAIL_DOMAIN);
         SystemScope.call(() -> new InsertAction().execute(new InsertInput(TABLE_EMAIL).withRecord(new QRecord()
            .withValue("messageId", messageId).withValue("recipient", String.valueOf(request.payload().get("to")))
            .withValue("podId", System.getProperty(KillTestInstance.POD_PROPERTY)))));
         SleepUtils.sleep(150, TimeUnit.MILLISECONDS);
         return (EffectResult.sent(messageId));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ReconcileResult reconcile(EffectRequest request) throws Exception
      {
         String messageId = EffectKeys.messageId(request.idempotencyKey(), EMAIL_DOMAIN);
         List<QRecord> found = SystemScope.call(() -> new QueryAction().execute(new QueryInput(TABLE_EMAIL)
            .withFilter(new QQueryFilter(new QFilterCriteria("messageId", QCriteriaOperator.EQUALS, messageId)))).getRecords());
         return (found.isEmpty() ? ReconcileResult.notFound() : ReconcileResult.found(messageId));
      }
   }



   /*******************************************************************************
    ** An S3 stub: every PUT is logged; the object map keeps one object per key
    ** (a PUT to an existing key overwrites it, as S3 does).
    *******************************************************************************/
   public static class StubS3Target implements EffectTarget
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public EffectResult deliver(EffectRequest request) throws Exception
      {
         String objectKey = EffectKeys.objectName("receipts", request.idempotencyKey(), "json");
         String content   = String.valueOf(request.payload().get("content"));
         SystemScope.call(() ->
         {
            new InsertAction().execute(new InsertInput(TABLE_S3_PUT).withRecord(new QRecord().withValue("objectKey", objectKey)
               .withValue("podId", System.getProperty(KillTestInstance.POD_PROPERTY))));
            List<QRecord> existing = new QueryAction().execute(new QueryInput(TABLE_S3_OBJ)
               .withFilter(new QQueryFilter(new QFilterCriteria("objectKey", QCriteriaOperator.EQUALS, objectKey)))).getRecords();
            if(existing.isEmpty())
            {
               List<QRecord> stored = new InsertAction().execute(new InsertInput(TABLE_S3_OBJ).withRecord(new QRecord().withValue("objectKey", objectKey).withValue("content", content))).getRecords();
               if(stored.get(0).getErrors() != null && !stored.get(0).getErrors().isEmpty())
               {
                  throw (new QException("PUT raced: " + stored.get(0).getErrors()));
               }
            }
            else
            {
               new UpdateAction().execute(new UpdateInput(TABLE_S3_OBJ).withRecord(new QRecord().withValue("objectKey", objectKey).withValue("content", content)));
            }
            return (null);
         });
         return (EffectResult.sent(objectKey));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ReconcileResult reconcile(EffectRequest request) throws Exception
      {
         String objectKey = EffectKeys.objectName("receipts", request.idempotencyKey(), "json");
         List<QRecord> found = SystemScope.call(() -> new QueryAction().execute(new QueryInput(TABLE_S3_OBJ)
            .withFilter(new QQueryFilter(new QFilterCriteria("objectKey", QCriteriaOperator.EQUALS, objectKey)))).getRecords());
         return (found.isEmpty() ? ReconcileResult.notFound() : ReconcileResult.found(objectKey));
      }
   }



   /*******************************************************************************
    ** A partner API that honors Idempotency-Key (the test JVM's stub server).
    *******************************************************************************/
   public static class StubHttpTarget extends HttpEffectTarget
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      protected HttpRequest.Builder buildRequest(EffectRequest request)
      {
         return (HttpRequest.newBuilder(URI.create(System.getProperty(PROPERTY_HTTP_STUB) + "/effects"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.toJson(request.payload()))));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      protected String providerRef(HttpResponse<String> response)
      {
         return (response.body());
      }
   }
}
