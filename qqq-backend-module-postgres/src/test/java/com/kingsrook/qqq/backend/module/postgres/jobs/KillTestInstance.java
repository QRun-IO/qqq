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


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.jobs.DurableProcessMetaData;
import com.kingsrook.qqq.backend.core.jobs.model.JobIdempotency;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.AuthScope;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.SleepUtils;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.C3P0PooledConnectionProvider;


/*******************************************************************************
 ** The QInstance the kill-the-pod tests run in every JVM (the test and the
 ** child "pods"): Postgres jobs tables, and a six-step process whose steps are
 ** slow and log which pod ran them.
 *******************************************************************************/
public class KillTestInstance
{
   public static final String BACKEND           = "default";
   public static final String PROCESS_SIX_STEPS = "killTestSteps";
   public static final String TABLE_STEP_LOG    = "kill_test_step_log";
   public static final String POD_PROPERTY      = "qqq.jobs.test.podId";

   public static final int STEP_COUNT  = 6;
   public static final int LEASE_TTL   = 10;
   public static final int REAPER_SECS = 3;



   /*******************************************************************************
    ** Define the instance. workerThreads 0 makes a launch-only JVM.
    *******************************************************************************/
   public static QInstance define(String host, int port, String database, String user, String password, String podId, int workerThreads) throws QException
   {
      return (define(host, port, database, user, password, podId, workerThreads, null));
   }



   /*******************************************************************************
    ** Define the instance; with a storage base path, also the bulk-load parts
    ** (chunk-object storage, the slow-insert target table, audits, and the
    ** recording alert sink and realtime bus).
    *******************************************************************************/
   public static QInstance define(String host, int port, String database, String user, String password, String podId, int workerThreads, String storageBasePath) throws QException
   {
      PostgreSQLBackendMetaData backend = new PostgreSQLBackendMetaData()
         .withName(BACKEND)
         .withHostName(host)
         .withPort(port)
         .withDatabaseName(database)
         .withUsername(user)
         .withPassword(password);
      backend.setConnectionProvider(new QCodeReference(C3P0PooledConnectionProvider.class));

      QInstance instance = new QInstance();
      instance.addBackend(backend);
      instance.registerAuthenticationProvider(AuthScope.instanceDefault(), new QAuthenticationMetaData().withName("mock").withType(QAuthenticationType.MOCK));

      new DurableJobsMetaDataProvider().defineAll(instance, BACKEND, DurableJobsMetaDataProvider.snakeCaseColumnNames());

      instance.addTable(new QTableMetaData()
         .withName(TABLE_STEP_LOG)
         .withBackendName(BACKEND)
         .withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER))
         .withField(new QFieldMetaData("stepName", QFieldType.STRING).withBackendName("step_name"))
         .withField(new QFieldMetaData("podId", QFieldType.STRING).withBackendName("pod_id"))
         .withField(new QFieldMetaData("createDate", QFieldType.DATE_TIME).withBackendName("create_date")));

      List<QStepMetaData> steps = new ArrayList<>();
      for(int i = 1; i <= STEP_COUNT; i++)
      {
         steps.add(new QBackendStepMetaData().withName("step" + i).withCode(new QCodeReference(SlowLoggedStep.class)));
      }
      QProcessMetaData process = new QProcessMetaData().withName(PROCESS_SIX_STEPS).withLabel("Kill Test Steps").withStepList(steps);
      process.withSupplementalMetaData(new DurableProcessMetaData().withEligible(true).withIdempotency(JobIdempotency.NATURAL));
      instance.addProcess(process);

      DurableJobsMetaData settings = new DurableJobsMetaData()
         .withWorkerThreads(workerThreads)
         .withLeaseTtlSeconds(LEASE_TTL)
         .withReaperIntervalSeconds(REAPER_SECS)
         .withPollIntervalMillis(200)
         .withPodId(podId);
      instance.withSupplementalMetaData(settings);

      if(storageBasePath != null)
      {
         BulkLoadKillTestParts.add(instance, BACKEND, storageBasePath);
         settings.withStorageTableName(BulkLoadKillTestParts.TABLE_STORAGE);
      }
      return (instance);
   }



   /*******************************************************************************
    ** A slow step that logs its name and pod (natural idempotency: rerunning it
    ** only adds another log row).
    *******************************************************************************/
   public static class SlowLoggedStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         Integer stepMillis = input.getValueInteger("stepMillis");
         SleepUtils.sleep(stepMillis == null ? 1500 : stepMillis, TimeUnit.MILLISECONDS);
         new InsertAction().execute(new InsertInput(TABLE_STEP_LOG).withRecord(new QRecord()
            .withValue("stepName", input.getStepName())
            .withValue("podId", System.getProperty(POD_PROPERTY))));
         output.addValue("last", input.getStepName());
      }
   }

}
