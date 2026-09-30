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

package com.kingsrook.sampleapp;


import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.messaging.email.EmailMessagingProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.quartz.QuartzSchedulerMetaData;
import com.kingsrook.qqq.backend.core.model.savedreports.RenderedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReport;
import com.kingsrook.qqq.backend.core.model.savedreports.SavedReportsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.savedreports.ScheduledReport;
import com.kingsrook.qqq.backend.core.model.savedreports.ScheduledReportSyncToScheduledJobProcess;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJob;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobParameter;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.customizers.ScheduledJobTableCustomizer;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.core.processes.implementations.savedreports.RenderSavedReportMetaDataProducer;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.SchedulableType;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.runner.SchedulableProcessRunner;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.Cardinality;
import com.kingsrook.qqq.backend.module.filesystem.base.model.metadata.RecordFormat;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemBackendMetaData;
import com.kingsrook.qqq.backend.module.filesystem.local.model.metadata.FilesystemTableBackendDetails;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.quartz.CronTrigger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.listeners.JobListenerSupport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Record actions must keep saved-report schedules and actual Quartz jobs in sync.
 *******************************************************************************/
class SampleScheduledReportLifecycleTest
{
   private static final String OWNER = "owned-scheduled-report-user";
   private static final String CRON = "0 0 0 1 1 ?";

   @TempDir
   Path directory;

   private QInstance instance;
   private Integer reportId;
   private String schedulerName;
   private QScheduleManager manager;
   private Scheduler quartz;
   private SampleScheduledReportContractTest.SmtpSink smtp;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      MemoryRecordStore.getInstance().reset();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QSession session = new QSession().withUser(new QUser().withIdReference(OWNER))
         .withPermissions(Set.of("person.read", "renderSavedReport.hasAccess"));
      QContext.init(instance, session);
      new SavedReportsMetaDataProvider().defineAll(instance, SampleMetaDataProvider.MEMORY_BACKEND_NAME,
         SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME, table ->
         {
            if(SavedReportsMetaDataProvider.REPORT_STORAGE_TABLE_NAME.equals(table.getName()))
            {
               table.setBackendDetails(new FilesystemTableBackendDetails().withBasePath("reports")
                  .withCardinality(Cardinality.MANY).withRecordFormat(RecordFormat.CSV));
            }
         });
      new ScheduledJobsMetaDataProvider().defineAll(instance, SampleMetaDataProvider.MEMORY_BACKEND_NAME, null);
      ((FilesystemBackendMetaData) instance.getBackend(SampleMetaDataProvider.FILESYSTEM_BACKEND_NAME)).setBasePath(directory.toString());
      instance.getAuthentication().setCustomizer(new QCodeReference(SampleScheduledReportContractTest.OwnerPermissions.class));
      instance.addSchedulableType(new SchedulableType().withName("PROCESS").withRunner(new QCodeReference(SchedulableProcessRunner.class)));
      schedulerName = "owned-lifecycle-" + UUID.randomUUID();
      Properties properties = new Properties();
      properties.setProperty("org.quartz.scheduler.instanceName", schedulerName);
      properties.setProperty("org.quartz.threadPool.threadCount", "1");
      instance.getSchedulers().clear();
      instance.addScheduler(new QuartzSchedulerMetaData().withProperties(properties).withName(schedulerName));
      manager = QScheduleManager.initInstance(instance, () -> session);
      manager.start();
      quartz = new StdSchedulerFactory(properties).getScheduler();
      QRecord report = new InsertAction().execute(new InsertInput(SavedReport.TABLE_NAME).withRecordEntity(new SavedReport()
         .withLabel("Lifecycle people").withUserId(OWNER).withTableName("person")
         .withColumnsJson("{\"columns\":[{\"name\":\"firstName\"}]}").withQueryFilterJson("{}"))).getRecords().get(0);
      assertClean(report);
      reportId = report.getValueInteger("id");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(manager != null)
         {
            manager.stop();
            manager.unInit();
         }
         if(smtp != null)
         {
            smtp.close();
         }
      }
      finally
      {
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testConfiguredScheduleLifecycle() throws Exception
   {
      configureScheduler(schedulerName);
      QRecord report = insertSchedule(false);
      assertClean(report);
      QRecord job = onlyJob();
      Integer jobId = job.getValueInteger("id");
      assertFalse(quartz.checkExists(jobKey(job)));
      assertEquals(2, QueryAction.execute(ScheduledJobParameter.TABLE_NAME, null).size());
      assertClean(update(report, "isActive", true));
      assertTrue(quartz.checkExists(jobKey(job)));
      assertEquals(CRON, trigger(job).getCronExpression());
      assertEquals("UTC", trigger(job).getTimeZone().getID());
      String changedCron = "0 30 8 ? * MON-FRI";
      assertClean(new UpdateAction().execute(new UpdateInput(ScheduledReport.TABLE_NAME).withRecord(new QRecord()
         .withValue("id", report.getValue("id")).withValue("cronExpression", changedCron)
         .withValue("cronTimeZoneId", "America/Chicago"))).getRecords().get(0));
      assertEquals(changedCron, trigger(job).getCronExpression());
      assertEquals("America/Chicago", trigger(job).getTimeZone().getID());
      assertEquals(jobId, onlyJob().getValueInteger("id"));
      configureScheduler("unused-new-default");
      assertClean(update(report, "subject", "Updated subject"));
      assertEquals(schedulerName, onlyJob().getValueString("schedulerName"));
      assertEquals(jobId, onlyJob().getValueInteger("id"));
      assertEquals(2, QueryAction.execute(ScheduledJobParameter.TABLE_NAME, null).size());
      assertClean(update(report, "isActive", false));
      assertFalse(quartz.checkExists(jobKey(job)));
      assertFalse(onlyJob().getValueBoolean("isActive"));
      assertClean(update(report, "isActive", true));
      assertTrue(quartz.checkExists(jobKey(job)));
      var deleted = new DeleteAction().execute(new DeleteInput(ScheduledReport.TABLE_NAME).withPrimaryKeys(List.of(report.getValue("id"))));
      assertEquals(1, deleted.getDeletedRecordCount());
      assertTrue(deleted.getRecordsWithErrors().isEmpty());
      assertTrue(deleted.getRecordsWithWarnings().isEmpty());
      assertTrue(QueryAction.execute(ScheduledJob.TABLE_NAME, null).isEmpty());
      assertTrue(QueryAction.execute(ScheduledJobParameter.TABLE_NAME, null).isEmpty());
      assertFalse(quartz.checkExists(jobKey(job)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testPersistedJobDispatchesCurrentReport() throws Exception
   {
      configureScheduler(schedulerName);
      smtp = new SampleScheduledReportContractTest.SmtpSink(false);
      instance.addMessagingProvider(new EmailMessagingProviderMetaData().withSmtpServer("127.0.0.1")
         .withSmtpPort(String.valueOf(smtp.getPort())).withName("owned-lifecycle-mail"));
      var inputs = instance.getProcess("renderSavedReport").getBackendStep("pre").getInputMetaData();
      inputs.getFieldThrowing(RenderSavedReportMetaDataProducer.SES_PROVIDER_NAME).setDefaultValue("owned-lifecycle-mail");
      inputs.getFieldThrowing(RenderSavedReportMetaDataProducer.FROM_EMAIL_ADDRESS).setDefaultValue("sender@example.com");
      assertClean(insertSchedule(true));
      QRecord job = onlyJob();
      CountDownLatch finished = new CountDownLatch(1);
      AtomicReference<JobExecutionException> failure = new AtomicReference<>();
      quartz.getListenerManager().addJobListener(new JobListenerSupport()
      {
         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public String getName()
         {
            return "owned-lifecycle-result";
         }



         /*******************************************************************************
          **
          *******************************************************************************/
         @Override
         public void jobWasExecuted(JobExecutionContext context, JobExecutionException exception)
         {
            failure.set(exception);
            finished.countDown();
         }
      });
      quartz.triggerJob(jobKey(job));
      assertTrue(finished.await(5, TimeUnit.SECONDS));
      assertNull(failure.get(), String.valueOf(failure.get()));
      List<QRecord> history = QueryAction.execute(RenderedReport.TABLE_NAME, null);
      assertEquals(1, history.size());
      String csv = Files.readString(directory.resolve("reports").resolve(history.get(0).getValueString("resultPath")));
      assertEquals(6, csv.lines().count());
      assertTrue(csv.contains("Avery"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testInvalidUpdatePreservesExistingSchedule() throws Exception
   {
      configureScheduler(schedulerName);
      QRecord report = insertSchedule(true);
      QRecord job = onlyJob();
      QRecord result = update(report, "cronExpression", "invalid-owned-cron");
      assertFalse(result.getErrors().isEmpty());
      assertEquals(CRON, onlyJob().getValueString("cronExpression"));
      assertEquals(CRON, trigger(job).getCronExpression());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RejectLinkedJobDelete extends ScheduledJobTableCustomizer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         records.forEach(record -> record.addError(new BadInputStatusMessage("Owned linked-job deletion rejection")));
         return records;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class RejectSecondJob extends ScheduledJobTableCustomizer
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean isPreview) throws QException
      {
         super.preInsert(input, records, isPreview);
         records.stream().filter(record -> "2".equals(record.getValueString("foreignKeyValue")))
            .forEach(record -> record.addError(new BadInputStatusMessage("Owned linked-job rejection")));
         return records;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void configureScheduler(String name) throws Exception
   {
      instance.getProcess(ScheduledReportSyncToScheduledJobProcess.NAME).getBackendStep("preview").getInputMetaData()
         .getFieldThrowing("schedulerName").setDefaultValue(name);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insertSchedule(boolean active) throws Exception
   {
      return new InsertAction().execute(new InsertInput(ScheduledReport.TABLE_NAME).withRecordEntity(newSchedule(active))).getRecords().get(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ScheduledReport newSchedule(boolean active)
   {
      return new ScheduledReport().withSavedReportId(reportId).withUserId(OWNER).withFormat("CSV").withInputValues("{}")
         .withCronExpression(CRON).withCronTimeZoneId("UTC").withIsActive(active)
         .withToAddresses("receiver@example.com").withSubject("Owned lifecycle delivery");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord update(QRecord report, String field, java.io.Serializable value) throws Exception
   {
      return new UpdateAction().execute(new UpdateInput(ScheduledReport.TABLE_NAME).withRecord(new QRecord()
         .withValue("id", report.getValue("id")).withValue(field, value))).getRecords().get(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord onlyJob() throws Exception
   {
      List<QRecord> records = new QueryAction().execute(new QueryInput(ScheduledJob.TABLE_NAME).withIncludeAssociations(true)).getRecords();
      assertEquals(1, records.size());
      return records.get(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private JobKey jobKey(QRecord job)
   {
      return new JobKey("scheduledJob:" + job.getValueInteger("id"), "PROCESS");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CronTrigger trigger(QRecord job) throws Exception
   {
      CronTrigger trigger = (CronTrigger) quartz.getTrigger(new TriggerKey(jobKey(job).getName(), "PROCESS"));
      assertNotNull(trigger);
      return trigger;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertClean(QRecord record)
   {
      assertTrue(record.getErrors() == null || record.getErrors().isEmpty(), String.valueOf(record.getErrors()));
      assertTrue(record.getWarnings() == null || record.getWarnings().isEmpty(), String.valueOf(record.getWarnings()));
   }
}
