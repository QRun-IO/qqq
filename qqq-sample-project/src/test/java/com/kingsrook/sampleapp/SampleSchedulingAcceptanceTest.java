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


import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qqq.backend.core.actions.automation.AutomationStatus;
import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationHandlerInterface;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.logging.QCollectingLogger;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.automation.RecordAutomationInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.automation.PollingAutomationProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.VariantRunStrategy;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSPollerSettings;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSQueueMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.queues.SQSQueueProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.quartz.QuartzSchedulerMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.simple.SimpleSchedulerMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.AutomationStatusTracking;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.AutomationStatusTrackingType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.QTableAutomationDetails;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TableAutomationAction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TriggerEvent;
import com.kingsrook.qqq.backend.core.model.metadata.variants.BackendVariantsConfig;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobType;
import com.kingsrook.qqq.backend.core.model.scheduledjobs.ScheduledJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerUtils;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzJobRunner;
import com.kingsrook.qqq.backend.core.scheduler.quartz.QuartzScheduler;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.SchedulableType;
import com.kingsrook.qqq.backend.core.scheduler.schedulable.identity.BasicSchedulableIdentity;
import com.kingsrook.qqq.backend.core.scheduler.simple.SimpleScheduler;
import com.kingsrook.qqq.backend.core.scheduler.simple.StandardScheduledExecutor;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import com.kingsrook.qqq.middleware.javalin.QApplicationLauncher;
import com.kingsrook.qqq.middleware.javalin.QApplicationLauncherConfig;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import com.sun.net.httpserver.HttpServer;
import io.javalin.Javalin;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.listeners.JobListenerSupport;
import org.quartz.utils.ConnectionProvider;
import org.quartz.utils.DBConnectionManager;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** First-party scheduling through QQQ managers/runners, native Quartz and owned H2.
 *******************************************************************************/
class SampleSchedulingAcceptanceTest
{
   private static final String PROCESS = "ownedScheduledPerson";
   private static final String SIMPLE = "ownedSimple";
   private static volatile Observation observation;
   private CapturedContext previousContext;
   private Map<String, Serializable> previousObjects;
   private QInstance instance;
   private QScheduleManager manager;
   private Scheduler quartz;
   private String quartzName;
   private Connection anchor;
   private String jdbcUrl;



   /*******************************************************************************
    ** Own metadata, native data and both singleton schedulers in this test JVM.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousContext = QContext.capture();
      previousObjects = QContext.getObjects();
      QContext.setObjects(new LinkedHashMap<>());
      assertThrows(IllegalStateException.class, QScheduleManager::getInstance);
      ConnectionManager.resetConnectionProviders();
      jdbcUrl = "jdbc:h2:mem:scheduling_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=0";
      anchor = DriverManager.getConnection(jdbcUrl, "sa", "");
      try(InputStreamReader reader = new InputStreamReader(SampleMetaDataProvider.class.getResourceAsStream("/prime-test-database.sql"), StandardCharsets.UTF_8))
      {
         RunScript.execute(anchor, reader);
      }
      instance = SampleMetaDataProvider.defineTestInstance();
      //////////////////////////////////////////////////////////////////////////
      // Keep the owned scheduling database independent of sample ESB brokers. //
      //////////////////////////////////////////////////////////////////////////
      EsbTableMetaData.of(instance.getTable("person")).setPublications(List.of());
      RDBMSBackendMetaData backend = SampleMetaDataProvider.defineRdbmsBackend().withName("schedulingDatabase").withJdbcUrl(jdbcUrl);
      instance.addBackend(backend);
      instance.getTable("person").setBackendName(backend.getName());
      instance.addProcess(new QProcessMetaData().withName(PROCESS).withTableName("person")
         .withStep(new QBackendStepMetaData().withName("writePerson").withCode(new QCodeReference(WritePerson.class))));
      QScheduleManager.defineDefaultSchedulableTypesInInstance(instance);
      instance.getSchedulers().clear();
      quartzName = "owned-quartz-" + UUID.randomUUID();
      Properties properties = new Properties();
      properties.setProperty("org.quartz.scheduler.instanceName", quartzName);
      properties.setProperty("org.quartz.threadPool.threadCount", "3");
      instance.addScheduler(new QuartzSchedulerMetaData().withProperties(properties).withName(quartzName));
      instance.addScheduler(new SimpleSchedulerMetaData().withName(SIMPLE));
      QContext.init(instance, new QSession().withUser(new QUser().withIdReference("caller")));
      observation = new Observation();
      manager = QScheduleManager.initInstance(instance, () -> new QSession().withUser(new QUser().withIdReference("scheduled-user")));
      quartz = new StdSchedulerFactory(properties).getScheduler();
   }



   /*******************************************************************************
    ** Release any owned blocked work before shutdown, then remove state and native data.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(observation != null)
         {
            observation.release.countDown();
         }
         if(manager != null)
         {
            try
            {
               manager.stop();
            }
            finally
            {
               manager.unInit();
            }
         }
         if(observation != null)
         {
            for(UUID id : observation.processIds)
            {
               RunProcessAction.getStateProvider().remove(new UUIDAndTypeStateKey(id, StateType.PROCESS_STATUS));
            }
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
            observation = null;
            ConnectionManager.resetConnectionProviders();
            QContext.clear();
            QContext.init(previousContext);
            QContext.setObjects(previousObjects);
         }
      }
   }



   /*******************************************************************************
    ** Native trigger time must honor the explicit QQQ initial-delay contract.
    *******************************************************************************/
   @Test
   void quartzHonorsInitialDelay() throws Exception
   {
      QuartzScheduler.getInstance().doNotStart();
      long before = System.currentTimeMillis();
      QuartzScheduler.getInstance().setupSchedulable(identity("delay"), processType(), Map.of("processName", PROCESS),
         new QScheduleMetaData().withSchedulerName(quartzName).withRepeatMillis(200).withInitialDelayMillis(10000), true);
      long nativeStart = quartz.getTrigger(new TriggerKey("delay", "PROCESS")).getStartTime().getTime();
      assertTrue(nativeStart >= before + 10000, "Configured 10000ms; native trigger delay was " + (nativeStart - before) + "ms");
   }



   /*******************************************************************************
    ** SimpleScheduler's interval and first delay drive actual QQQ writes to H2.
    *******************************************************************************/
   @Test
   void simpleIntervalAndInitialDelayPersistProcessWrites() throws Exception
   {
      instance.getProcess(PROCESS).setSchedule(new QScheduleMetaData().withSchedulerName(SIMPLE).withRepeatMillis(50).withInitialDelayMillis(250));
      long started = System.nanoTime();
      manager.start();
      Invocation first = next();
      Invocation second = next();
      manager.stop();
      assertTrue(first.atNanos() - started >= TimeUnit.MILLISECONDS.toNanos(250));
      assertTrue(second.atNanos() > first.atNanos());
      assertEquals("scheduled-user", first.user());
      assertEquals(List.of(List.of("Scheduled", String.valueOf(observation.writes.get()))), rows("SELECT first_name,days_worked FROM person WHERE id=1"));
      StandardScheduledExecutor executor = SimpleScheduler.getInstance(instance).getExecutors().get(0);
      assertEquals(StandardScheduledExecutor.RunningState.STOPPED, executor.getRunningState());
      assertEquals("caller", QContext.getQSession().getUser().getIdReference());
   }



   /*******************************************************************************
    ** Real Quartz interval execution and QQQ management share one native identity.
    *******************************************************************************/
   @Test
   void quartzIntervalPauseResumeRescheduleAndUnschedule() throws Exception
   {
      QuartzScheduler scheduler = QuartzScheduler.getInstance();
      scheduler.doNotStart();
      BasicSchedulableIdentity id = identity("managed");
      QScheduleMetaData schedule = new QScheduleMetaData().withSchedulerName(quartzName).withRepeatMillis(50);
      scheduler.setupSchedulable(id, processType(), Map.of("processName", PROCESS), schedule, true);
      TriggerKey key = new TriggerKey("managed", "PROCESS");
      assertEquals(50, ((SimpleTrigger) quartz.getTrigger(key)).getRepeatInterval());
      scheduler.pauseJob("managed", "PROCESS");
      assertEquals(Trigger.TriggerState.PAUSED, quartz.getTriggerState(key));
      scheduler.setupSchedulable(id, processType(), Map.of("processName", PROCESS), schedule.withRepeatMillis(75), true);
      assertEquals(1, quartz.getJobKeys(GroupMatcher.anyJobGroup()).size());
      assertEquals(75, ((SimpleTrigger) quartz.getTrigger(key)).getRepeatInterval());
      assertEquals(Trigger.TriggerState.PAUSED, quartz.getTriggerState(key));
      scheduler.resumeJob("managed", "PROCESS");
      assertEquals(Trigger.TriggerState.NORMAL, quartz.getTriggerState(key));
      scheduler.start();
      next();
      next();
      scheduler.unscheduleSchedulable(id, processType());
      assertFalse(quartz.checkExists(new JobKey("managed", "PROCESS")));
      scheduler.stop();
      assertTrue(observation.writes.get() >= 2);
      assertEquals(List.of(List.of("Scheduled", String.valueOf(observation.writes.get()))), rows("SELECT first_name,days_worked FROM person WHERE id=1"));
   }



   /*******************************************************************************
    ** Cron uses the requested native expression/time zone and dispatches a real process.
    *******************************************************************************/
   @Test
   void cronScheduleDispatchesAndUnscheduleAllRemovesIt() throws Exception
   {
      QuartzScheduler scheduler = QuartzScheduler.getInstance();
      scheduler.doNotStart();
      scheduler.setupSchedulable(identity("cron"), processType(), Map.of("processName", PROCESS),
         new QScheduleMetaData().withSchedulerName(quartzName).withCronExpression("0/1 * * * * ?").withCronTimeZoneId("UTC"), true);
      CronTrigger trigger = (CronTrigger) quartz.getTrigger(new TriggerKey("cron", "PROCESS"));
      assertEquals("0/1 * * * * ?", trigger.getCronExpression());
      assertEquals("UTC", trigger.getTimeZone().getID());
      scheduler.start();
      next();
      manager.unscheduleAll();
      assertTrue(quartz.getJobKeys(GroupMatcher.anyJobGroup()).isEmpty());
      scheduler.stop();
      assertEquals(List.of(List.of("Scheduled")), rows("SELECT first_name FROM person WHERE id=1"));
   }



   /*******************************************************************************
    ** Invalid cron is logged by Quartz setup; an existing job is not replaced and
    ** invalid new jobs do not appear. This method does not promise caller exceptions.
    *******************************************************************************/
   @Test
   void invalidCronPreservesNativeScheduleAndData() throws Exception
   {
      QuartzScheduler scheduler = QuartzScheduler.getInstance();
      scheduler.doNotStart();
      BasicSchedulableIdentity id = identity("valid");
      QScheduleMetaData schedule = new QScheduleMetaData().withSchedulerName(quartzName).withRepeatMillis(100);
      scheduler.setupSchedulable(id, processType(), Map.of("processName", PROCESS), schedule, true);
      Trigger before = quartz.getTrigger(new TriggerKey("valid", "PROCESS"));
      List<List<String>> data = rows("SELECT * FROM person ORDER BY id");
      QScheduleMetaData invalid = new QScheduleMetaData().withSchedulerName(quartzName).withCronExpression("invalid cron").withCronTimeZoneId("UTC");
      scheduler.setupSchedulable(id, processType(), Map.of("processName", PROCESS), invalid, true);
      scheduler.setupSchedulable(identity("invalid"), processType(), Map.of("processName", PROCESS), invalid, true);
      assertEquals(before, quartz.getTrigger(new TriggerKey("valid", "PROCESS")));
      assertFalse(quartz.checkExists(new JobKey("invalid", "PROCESS")));
      assertEquals(data, rows("SELECT * FROM person ORDER BY id"));
      assertFalse(new SimpleSchedulerMetaData().supportsCronSchedules());
      assertFalse(new SimpleSchedulerMetaData().mayUseInScheduledJobsTable());
   }



   /*******************************************************************************
    ** The documented launcher registers and dispatches fresh RAM jobs without a
    ** second setup call, and releases its owned HTTP and scheduler resources.
    *******************************************************************************/
   @Test
   void launcherStartupDispatchesFreshQuartzJob() throws Exception
   {
      instance.getProcess(PROCESS).setSchedule(new QScheduleMetaData().withSchedulerName(quartzName).withRepeatSeconds(60).withInitialDelayMillis(0));
      AtomicReference<Javalin> http = new AtomicReference<>();
      QApplicationLauncher launcher = null;
      try
      {
         launcher = QApplicationLauncher.run(new AbstractQQQApplication()
         {
            /*******************************************************************
             ** Use the fixture's actual application metadata and owned backend.
             *******************************************************************/
            @Override
            public QInstance defineQInstance()
            {
               return (instance);
            }
         }, new QApplicationLauncherConfig().withRegisterShutdownHook(false)
            .withSystemUserSessionSupplier(() -> new QSession().withUser(new QUser().withIdReference("scheduled-user")))
            .withServerCustomizer(server -> server.withPort(0)
               .withServeFrontendMaterialDashboard(false).withServeFrontendNext(false)
               .withJavalinConfigurationCustomizer(http::set)));
         assertTrue(http.get().port() > 0);
         assertTrue(quartz.checkExists(new JobKey("process:" + PROCESS, "PROCESS")), "Launcher omitted configured Quartz process");
         next();
      }
      finally
      {
         if(launcher != null)
         {
            launcher.stop();
         }
      }
      assertTrue(quartz.isShutdown());
      assertTrue(http.get().jettyServer().server().isStopped());
      assertEquals(List.of(List.of("Scheduled")), rows("SELECT first_name FROM person WHERE id=1"));
   }



   /*******************************************************************************
    ** RAM bootstrap leaves existing jobs, pause state and unrelated jobs untouched.
    *******************************************************************************/
   @Test
   void startupPreservesExistingPausedRamJob() throws Exception
   {
      instance.getProcess(PROCESS).setSchedule(new QScheduleMetaData().withSchedulerName(quartzName).withRepeatSeconds(60));
      seedNativePausedJob("process:" + PROCESS);
      seedNativePausedJob("unrelated");
      instance.addProcess(new QProcessMetaData().withName("existingNormal")
         .withStep(new QBackendStepMetaData().withName("writePerson").withCode(new QCodeReference(WritePerson.class)))
         .withSchedule(new QScheduleMetaData().withSchedulerName(quartzName).withRepeatSeconds(60)));
      seedNativePausedJob("process:existingNormal");
      quartz.resumeJob(new JobKey("process:existingNormal", "PROCESS"));
      TriggerKey normalKey = new TriggerKey("process:existingNormal", "PROCESS");
      Trigger normalBefore = quartz.getTrigger(normalKey);
      TriggerKey key = new TriggerKey("process:" + PROCESS, "PROCESS");
      Trigger before = quartz.getTrigger(key);
      manager.start();
      assertEquals(before.getStartTime(), quartz.getTrigger(key).getStartTime());
      assertEquals(TimeUnit.HOURS.toMillis(1), ((SimpleTrigger) quartz.getTrigger(key)).getRepeatInterval());
      assertEquals(Trigger.TriggerState.PAUSED, quartz.getTriggerState(key));
      assertEquals("native-marker", quartz.getJobDetail(new JobKey("process:" + PROCESS, "PROCESS")).getDescription());
      assertTrue(quartz.checkExists(new JobKey("unrelated", "PROCESS")));
      assertEquals(normalBefore.getStartTime(), quartz.getTrigger(normalKey).getStartTime());
      assertEquals(TimeUnit.HOURS.toMillis(1), ((SimpleTrigger) quartz.getTrigger(normalKey)).getRepeatInterval());
      assertEquals(Trigger.TriggerState.NORMAL, quartz.getTriggerState(normalKey));
      assertEquals("native-marker", quartz.getJobDetail(new JobKey("process:existingNormal", "PROCESS")).getDescription());
      assertEquals(3, quartz.getJobKeys(GroupMatcher.anyJobGroup()).size());
      assertEquals(0, observation.writes.get());
   }



   /*******************************************************************************
    ** One manager startup registers both scheduler types before dispatching each.
    *******************************************************************************/
   @Test
   void startupDispatchesMixedSimpleAndQuartzJobs() throws Exception
   {
      instance.getProcess(PROCESS).setSchedule(new QScheduleMetaData().withSchedulerName(quartzName).withRepeatSeconds(3600).withInitialDelayMillis(0));
      instance.addProcess(new QProcessMetaData().withName("simpleStartup").withTableName("person")
         .withStep(new QBackendStepMetaData().withName("writePerson").withCode(new QCodeReference(WritePerson.class)))
         .withSchedule(new QScheduleMetaData().withSchedulerName(SIMPLE).withRepeatSeconds(3600).withInitialDelayMillis(0)));
      manager.start();
      assertTrue(quartz.checkExists(new JobKey("process:" + PROCESS, "PROCESS")));
      next();
      next();
      manager.stop();
      assertEquals(2, observation.writes.get());
      assertEquals(StandardScheduledExecutor.RunningState.STOPPED, SimpleScheduler.getInstance(instance).getExecutors().get(0).getRunningState());
      assertEquals(List.of(List.of("Scheduled")), rows("SELECT first_name FROM person WHERE id=1"));
   }



   /*******************************************************************************
    ** Startup reads already persisted dynamic job and parameter rows, then performs
    ** the registered process through the volatile scheduler without modifying them.
    *******************************************************************************/
   @Test
   void startupRegistersPersistedDynamicJob() throws Exception
   {
      new ScheduledJobsMetaDataProvider().defineAll(instance, "schedulingDatabase", table ->
      {
         table.setBackendDetails(new RDBMSTableBackendDetails().withTableName(QInstanceEnricher.inferBackendName(table.getName())));
         QInstanceEnricher.setInferredFieldBackendNames(table);
      });
      try(Statement statement = anchor.createStatement())
      {
         statement.execute("CREATE TABLE scheduled_job (id INT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, label VARCHAR(100), description VARCHAR(250), scheduler_name VARCHAR(100), cron_expression VARCHAR(100), cron_description VARCHAR(250), cron_time_zone_id VARCHAR(100), repeat_seconds INT, type VARCHAR(100), is_active BOOLEAN, foreign_key_type VARCHAR(100), foreign_key_value VARCHAR(100))");
         statement.execute("CREATE TABLE scheduled_job_parameter (id INT PRIMARY KEY, create_date TIMESTAMP, modify_date TIMESTAMP, scheduled_job_id INT, `key` VARCHAR(250), `value` VARCHAR(250))");
      }
      try(PreparedStatement insert = anchor.prepareStatement("INSERT INTO scheduled_job (id,label,scheduler_name,repeat_seconds,type,is_active) VALUES (1,'Owned dynamic',?,3600,'PROCESS',TRUE)"))
      {
         insert.setString(1, quartzName);
         insert.executeUpdate();
      }
      try(Statement statement = anchor.createStatement())
      {
         statement.execute("INSERT INTO scheduled_job_parameter (id,scheduled_job_id,`key`,`value`) VALUES (1,1,'processName','" + PROCESS + "')");
      }
      List<List<String>> before = rows("SELECT * FROM scheduled_job");
      List<List<String>> parameters = rows("SELECT * FROM scheduled_job_parameter");
      manager.start();
      assertTrue(quartz.checkExists(new JobKey("scheduledJob:1", "PROCESS")));
      next();
      manager.stop();
      assertEquals(before, rows("SELECT * FROM scheduled_job"));
      assertEquals(parameters, rows("SELECT * FROM scheduled_job_parameter"));
      assertEquals(List.of(List.of("Scheduled")), rows("SELECT first_name FROM person WHERE id=1"));
   }



   /*******************************************************************************
    ** A real JDBC Quartz store retains the historical startup no-reconcile guard.
    ** Its preexisting paused row survives while a missing configured job stays absent.
    *******************************************************************************/
   @Test
   void persistentQuartzStartupRetainsGuard() throws Exception
   {
      manager.stop();
      manager.unInit();
      String url = "jdbc:h2:mem:quartz_" + UUID.randomUUID();
      try(Connection database = DriverManager.getConnection(url, "sa", ""))
      {
         try(InputStreamReader schema = new InputStreamReader(StdSchedulerFactory.class.getResourceAsStream("/org/quartz/impl/jdbcjobstore/tables_h2.sql"), StandardCharsets.UTF_8))
         {
            RunScript.execute(database, schema);
         }
         String dataSource = "owned-" + UUID.randomUUID();
         DBConnectionManager.getInstance().addConnectionProvider(dataSource, new ConnectionProvider()
         {
            /*******************************************************************
             ** Each native Quartz request receives its own JDBC connection.
             *******************************************************************/
            @Override
            public Connection getConnection() throws SQLException
            {
               return (DriverManager.getConnection(url, "sa", ""));
            }



            /*******************************************************************
             ** The outer fixture owns the database anchor; no pool is retained.
             *******************************************************************/
            @Override
            public void shutdown()
            {
            }



            /*******************************************************************
             ** Connections are opened only when requested by Quartz.
             *******************************************************************/
            @Override
            public void initialize()
            {
            }
         });
         Properties properties = new Properties();
         properties.setProperty("org.quartz.scheduler.instanceName", "jdbc-" + quartzName);
         properties.setProperty("org.quartz.threadPool.threadCount", "1");
         properties.setProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
         properties.setProperty("org.quartz.jobStore.dataSource", dataSource);
         instance.getSchedulers().clear();
         instance.addScheduler(new QuartzSchedulerMetaData().withProperties(properties).withName(quartzName));
         instance.getProcess(PROCESS).setSchedule(new QScheduleMetaData().withSchedulerName(quartzName).withRepeatSeconds(60));
         instance.addProcess(new QProcessMetaData().withName("missingPersistentJob")
            .withStep(new QBackendStepMetaData().withName("writePerson").withCode(new QCodeReference(WritePerson.class)))
            .withSchedule(new QScheduleMetaData().withSchedulerName(quartzName).withRepeatSeconds(60)));
         manager = QScheduleManager.initInstance(instance, () -> new QSession());
         quartz = new StdSchedulerFactory(properties).getScheduler();
         try
         {
            assertTrue(quartz.getMetaData().isJobStoreSupportsPersistence());
            seedNativePausedJob("process:" + PROCESS);
            TriggerKey key = new TriggerKey("process:" + PROCESS, "PROCESS");
            Trigger before = quartz.getTrigger(key);
            manager.start();
            assertEquals(before.getStartTime(), quartz.getTrigger(key).getStartTime());
            assertEquals(TimeUnit.HOURS.toMillis(1), ((SimpleTrigger) quartz.getTrigger(key)).getRepeatInterval());
            assertEquals("native-marker", quartz.getJobDetail(new JobKey("process:" + PROCESS, "PROCESS")).getDescription());
            assertEquals(Trigger.TriggerState.PAUSED, quartz.getTriggerState(key));
            assertFalse(quartz.checkExists(new JobKey("process:missingPersistentJob", "PROCESS")));
            try(Statement statement = database.createStatement(); ResultSet result = statement.executeQuery("SELECT COUNT(*) FROM QRTZ_JOB_DETAILS"))
            {
               assertTrue(result.next());
               assertEquals(1, result.getInt(1));
            }
            assertEquals(0, observation.writes.get());
         }
         finally
         {
            manager.stop();
            manager.unInit();
            manager = null;
         }
         assertTrue(quartz.isShutdown());
      }
   }



   /*******************************************************************************
    ** Seed native state independently of QQQ registration and its startup guard.
    *******************************************************************************/
   private void seedNativePausedJob(String name) throws Exception
   {
      JobDetail job = JobBuilder.newJob(QuartzJobRunner.class).withIdentity(name, "PROCESS")
         .withDescription("native-marker").storeDurably().build();
      Trigger trigger = TriggerBuilder.newTrigger().withIdentity(name, "PROCESS").forJob(job)
         .withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInHours(1).repeatForever())
         .startAt(new Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1))).build();
      quartz.scheduleJob(job, trigger);
      quartz.pauseJob(job.getKey());
   }



   /*******************************************************************************
    ** Native dispatch and persisted QQQ writes honor delay, beyond trigger metadata.
    *******************************************************************************/
   @Test
   void quartzDelayPrecedesActualProcessWrite() throws Exception
   {
      QuartzScheduler scheduler = QuartzScheduler.getInstance();
      scheduler.doNotStart();
      long started = System.nanoTime();
      scheduler.setupSchedulable(identity("delayedWrite"), processType(), Map.of("processName", PROCESS),
         new QScheduleMetaData().withRepeatSeconds(60).withInitialDelayMillis(250), true);
      scheduler.start();
      Invocation invocation = next();
      manager.stop();
      assertTrue(invocation.atNanos() - started >= TimeUnit.MILLISECONDS.toNanos(250));
      assertEquals(List.of(List.of("Scheduled")), rows("SELECT first_name FROM person WHERE id=1"));
   }



   /*******************************************************************************
    ** SERIAL variants execute in one job with no concurrent application steps.
    *******************************************************************************/
   @Test
   void serialVariantsPersistDistinctRowsWithoutOverlap() throws Exception
   {
      assertVariants(VariantRunStrategy.SERIAL);
   }



   /*******************************************************************************
    ** PARALLEL variants have separate identities and overlap under native Quartz.
    *******************************************************************************/
   @Test
   void parallelVariantsOverlapWithIsolatedSessionsAndRows() throws Exception
   {
      assertVariants(VariantRunStrategy.PARALLEL);
   }



   /*******************************************************************************
    ** Latches hold the first application step so concurrency is observed, not guessed.
    *******************************************************************************/
   private void assertVariants(VariantRunStrategy strategy) throws Exception
   {
      instance.addBackend(new QBackendMetaData().withName("ownedVariants").withBackendType(MemoryBackendModule.class).withUsesVariants(true)
         .withBackendVariantsConfig(new BackendVariantsConfig().withVariantTypeKey("ownedVariant").withOptionsTableName("person")
            .withOptionsFilter(new QQueryFilter().withCriteria(new QFilterCriteria("id", QCriteriaOperator.IN, List.of(1, 2))))));
      instance.addProcess(new QProcessMetaData().withName("writeVariant").withTableName("person")
         .withVariantBackend("ownedVariants").withVariantRunStrategy(strategy)
         .withStep(new QBackendStepMetaData().withName("write").withCode(new QCodeReference(WriteVariant.class)))
         .withSchedule(new QScheduleMetaData().withSchedulerName(quartzName).withRepeatSeconds(60).withInitialDelayMillis(0)));
      List<List<String>> unrelated = rows("SELECT * FROM person WHERE id>=3 ORDER BY id");
      manager.start();
      assertEquals(strategy == VariantRunStrategy.SERIAL ? 1 : 2, quartz.getJobKeys(GroupMatcher.anyJobGroup()).size());
      VariantInvocation first = observation.variantsEntered.poll(5, TimeUnit.SECONDS);
      assertNotNull(first);
      VariantInvocation second = null;
      if(strategy == VariantRunStrategy.PARALLEL)
      {
         second = observation.variantsEntered.poll(5, TimeUnit.SECONDS);
         assertNotNull(second);
         assertEquals(2, observation.maxActive.get());
      }
      observation.release.countDown();
      if(second == null)
      {
         second = observation.variantsEntered.poll(5, TimeUnit.SECONDS);
         assertNotNull(second);
      }
      assertNotNull(observation.variantsCompleted.poll(5, TimeUnit.SECONDS));
      assertNotNull(observation.variantsCompleted.poll(5, TimeUnit.SECONDS));
      manager.stop();
      assertEquals(Set.of(1, 2), Set.of(first.id(), second.id()));
      assertEquals("scheduled-user", first.user());
      assertEquals("scheduled-user", second.user());
      assertEquals(strategy == VariantRunStrategy.SERIAL, first.thread() == second.thread());
      assertEquals(strategy == VariantRunStrategy.SERIAL ? 1 : 2, observation.maxActive.get());
      assertEquals(List.of(List.of("1", "Variant1"), List.of("2", "Variant2")), rows("SELECT id,first_name FROM person WHERE id IN (1,2) ORDER BY id"));
      assertEquals(unrelated, rows("SELECT * FROM person WHERE id>=3 ORDER BY id"));
      assertEquals("caller", QContext.getQSession().getUser().getIdReference());
   }



   /*******************************************************************************
    ** Job failures are logged by the existing runner; the next interval still writes.
    ** This does not claim Quartz receives an exception swallowed by SchedulerUtils.
    *******************************************************************************/
   @Test
   void failedProcessDoesNotPreventNextInterval() throws Exception
   {
      instance.getProcess(PROCESS).getBackendStep("writePerson").setCode(new QCodeReference(FailFirstWrite.class));
      QCollectingLogger logger = QLogger.activateCollectingLoggerForClass(SchedulerUtils.class);
      try
      {
         QuartzScheduler scheduler = QuartzScheduler.getInstance();
         scheduler.doNotStart();
         scheduler.setupSchedulable(identity("retryInterval"), processType(), Map.of("processName", PROCESS),
            new QScheduleMetaData().withRepeatMillis(50).withInitialDelayMillis(0), true);
         scheduler.start();
         next();
         manager.stop();
         assertTrue(observation.attempts.get() >= 2);
         assertTrue(logger.getCollectedMessages().stream().anyMatch(message -> message.getMessage().contains("Owned scheduled failure")));
         assertEquals(List.of(List.of("Scheduled")), rows("SELECT first_name FROM person WHERE id=1"));
      }
      finally
      {
         manager.stop();
         QLogger.deactivateCollectingLoggerForClass(SchedulerUtils.class);
      }
   }



   /*******************************************************************************
    ** An observer timeout does not cancel application work. Native shutdown waits
    ** for our owned blocked step; release proves completion and resource cleanup.
    *******************************************************************************/
   @Test
   void observerTimeoutDoesNotCancelAndShutdownWaitsForOwnedWork() throws Exception
   {
      instance.getProcess(PROCESS).getBackendStep("writePerson").setCode(new QCodeReference(BlockedWrite.class));
      QuartzScheduler scheduler = QuartzScheduler.getInstance();
      scheduler.doNotStart();
      scheduler.setupSchedulable(identity("blocked"), processType(), Map.of("processName", PROCESS),
         new QScheduleMetaData().withRepeatSeconds(60).withInitialDelayMillis(0), true);
      scheduler.start();
      assertTrue(observation.entered.await(5, TimeUnit.SECONDS));
      assertEquals(1, quartz.getCurrentlyExecutingJobs().size());
      assertEquals(0, observation.writes.get());
      CompletableFuture<Void> stopped = CompletableFuture.runAsync(manager::stop);
      try
      {
         assertThrows(TimeoutException.class, () -> stopped.get(100, TimeUnit.MILLISECONDS));
         assertEquals(0, observation.writes.get());
      }
      finally
      {
         observation.release.countDown();
         stopped.get(5, TimeUnit.SECONDS);
      }
      next();
      assertTrue(quartz.isShutdown());
      assertEquals(List.of(List.of("Scheduled")), rows("SELECT first_name FROM person WHERE id=1"));
   }



   /*******************************************************************************
    ** The actual automation runner consumes pending native rows and marks them OK.
    *******************************************************************************/
   @Test
   void automationRunnerPersistsHandlerResultAndStatus() throws Exception
   {
      try(Statement statement = anchor.createStatement())
      {
         statement.execute("ALTER TABLE person ADD owned_status INTEGER DEFAULT 7");
         statement.execute("UPDATE person SET owned_status=1 WHERE id=1");
      }
      instance.addAutomationProvider(new PollingAutomationProviderMetaData().withName("ownedPolling"));
      instance.getTable("person").withField(new QFieldMetaData("ownedStatus", QFieldType.INTEGER).withBackendName("owned_status"))
         .withAutomationDetails(new QTableAutomationDetails().withProviderName("ownedPolling")
            .withStatusTracking(new AutomationStatusTracking().withType(AutomationStatusTrackingType.FIELD_IN_TABLE).withFieldName("ownedStatus"))
            .withAction(new TableAutomationAction().withName("ownedPersonAutomation").withTriggerEvent(TriggerEvent.POST_INSERT)
               .withCodeReference(new QCodeReference(AutomatePerson.class))));
      List<List<String>> unrelated = rows("SELECT * FROM person WHERE id>=2 ORDER BY id");
      QuartzScheduler scheduler = QuartzScheduler.getInstance();
      scheduler.doNotStart();
      scheduler.setupSchedulable(identity("automation"), instance.getSchedulableType(ScheduledJobType.TABLE_AUTOMATIONS.name()),
         Map.of("tableName", "person", "automationStatus", AutomationStatus.PENDING_INSERT_AUTOMATIONS.name()),
         new QScheduleMetaData().withRepeatSeconds(60).withInitialDelayMillis(0), true);
      scheduler.start();
      next();
      manager.stop();
      assertEquals(List.of(List.of("Automated", "7")), rows("SELECT first_name,owned_status FROM person WHERE id=1"));
      assertEquals(unrelated, rows("SELECT * FROM person WHERE id>=2 ORDER BY id"));
   }



   /*******************************************************************************
    ** Real legacy SDK/runner dispatches the received body and acknowledges success.
    *******************************************************************************/
   @Test
   void queueRunnerPersistsBodyBeforeAcknowledging() throws Exception
   {
      assertQueueRunner(false);
   }



   /*******************************************************************************
    ** Failed application work leaves the owned protocol fixture's message unacked.
    *******************************************************************************/
   @Test
   void queueRunnerDoesNotAcknowledgeFailedProcess() throws Exception
   {
      assertQueueRunner(true);
   }



   /*******************************************************************************
    ** A loopback, in-memory protocol fixture exercises the existing runner without
    ** any SQS account. Its receipt state is an independent acknowledgement oracle,
    ** not evidence of AWS delivery/IAM semantics or SDK-client lifecycle ownership.
    *******************************************************************************/
   private void assertQueueRunner(boolean fail) throws Exception
   {
      observation.failQueue = fail;
      AtomicInteger receives = new AtomicInteger();
      BlockingQueue<String> deletes = new LinkedBlockingQueue<>();
      String body = "Queued";
      String digest = HexFormat.of().formatHex(MessageDigest.getInstance("MD5").digest(body.getBytes(StandardCharsets.UTF_8)));
      HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/owned", exchange ->
      {
         String request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
         String target = exchange.getRequestHeaders().getFirst("X-Amz-Target");
         String response;
         if("AmazonSQS.ReceiveMessage".equals(target))
         {
            receives.incrementAndGet();
            response = JsonUtils.toJson(Map.of("Messages", List.of(Map.of("MessageId", "owned-message", "ReceiptHandle", "owned-receipt", "MD5OfBody", digest, "Body", body))));
         }
         else if("AmazonSQS.DeleteMessageBatch".equals(target))
         {
            deletes.add(request);
            response = JsonUtils.toJson(Map.of("Successful", List.of(Map.of("Id", "0")), "Failed", List.of()));
         }
         else
         {
            response = "{}";
         }
         byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
         exchange.getResponseHeaders().add("Content-Type", "application/x-amz-json-1.0");
         exchange.sendResponseHeaders(200, bytes.length);
         try(exchange)
         {
            exchange.getResponseBody().write(bytes);
         }
      });
      server.start();
      try
      {
         instance.addQueueProvider(new SQSQueueProviderMetaData().withName("ownedQueueProvider")
            .withAccessKey("owned-fixture-key").withSecretKey("owned-fixture-secret").withRegion("us-east-1")
            .withBaseURL("http://127.0.0.1:" + server.getAddress().getPort())
            .withPollerSettings(new SQSPollerSettings().withMaxLoops(1).withMaxNumberOfMessages(1).withWaitTimeSeconds(0)));
         instance.addQueue(new SQSQueueMetaData().withName("ownedQueue").withProviderName("ownedQueueProvider").withQueueName("owned").withProcessName("writeQueuedPerson"));
         instance.addProcess(new QProcessMetaData().withName("writeQueuedPerson").withTableName("person")
            .withStep(new QBackendStepMetaData().withName("write").withCode(new QCodeReference(WriteQueuedPerson.class))));
         CountDownLatch finished = new CountDownLatch(1);
         quartz.getListenerManager().addJobListener(new JobListenerSupport()
         {
            /*******************************************************************************
             **
             *******************************************************************************/
            @Override
            public String getName()
            {
               return "ownedQueueCompletion";
            }



            /*******************************************************************************
             ** Signal after the actual queue runner has finished.
             *******************************************************************************/
            @Override
            public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException)
            {
               finished.countDown();
            }
         });
         List<List<String>> before = rows("SELECT * FROM person ORDER BY id");
         QuartzScheduler scheduler = QuartzScheduler.getInstance();
         scheduler.doNotStart();
         scheduler.setupSchedulable(identity("queue"), instance.getSchedulableType(ScheduledJobType.QUEUE_PROCESSOR.name()),
            Map.of("queueName", "ownedQueue"), new QScheduleMetaData().withRepeatSeconds(60).withInitialDelayMillis(0), true);
         scheduler.start();
         assertTrue(finished.await(10, TimeUnit.SECONDS), "Owned queue runner did not finish");
         manager.stop();
         assertEquals(1, receives.get());
         assertEquals(List.of(body), observation.queueBodies);
         if(fail)
         {
            assertTrue(deletes.isEmpty());
            assertEquals(before, rows("SELECT * FROM person ORDER BY id"));
         }
         else
         {
            assertEquals(1, deletes.size());
            assertEquals("owned-receipt", JsonUtils.toJSONObject(deletes.remove()).getJSONArray("Entries").getJSONObject(0).getString("ReceiptHandle"));
            assertEquals(List.of(List.of(body)), rows("SELECT first_name FROM person WHERE id=1"));
            assertEquals(before.subList(1, before.size()), rows("SELECT * FROM person WHERE id>=2 ORDER BY id"));
         }
      }
      finally
      {
         server.stop(0);
      }
   }



   /*******************************************************************************
    ** Native Quartz isolates named objects and restores the exact prior worker state.
    *******************************************************************************/
   @Test
   void quartzContextSuccessIsolatedAndRestored() throws Exception
   {
      assertWorkerContext(true, false);
   }



   /*******************************************************************************
    ** A thrown application exception restores the worker before the next firing.
    *******************************************************************************/
   @Test
   void quartzContextFailureRestoresAndNextRunRecovers() throws Exception
   {
      assertWorkerContext(true, true);
   }



   /*******************************************************************************
    ** The registered SimpleJobRunner executes on a real StandardScheduledExecutor.
    *******************************************************************************/
   @Test
   void simpleContextSuccessIsolatedAndRestored() throws Exception
   {
      assertWorkerContext(false, false);
   }



   /*******************************************************************************
    ** Simple's real application error path restores state and allows another run.
    *******************************************************************************/
   @Test
   void simpleContextFailureRestoresAndNextRunRecovers() throws Exception
   {
      assertWorkerContext(false, true);
   }



   /*******************************************************************************
    ** Acceptance-only probe of the real five-minute Simple stop boundary. The IT
    ** owns invocation so the ordinary focused suite does not incur this wait.
    *******************************************************************************/
   void assertActualSimpleStopTimeout() throws Exception
   {
      QInstance workerInstance = SampleMetaDataProvider.defineTestInstance();
      QSession workerSession = new QSession().withUser(new QUser().withIdReference("worker-owner"));
      Map<String, Serializable> originalObjects = new LinkedHashMap<>(Map.of("workerMarker", "worker-only"));
      instance.getProcess(PROCESS).getBackendStep("writePerson").setCode(new QCodeReference(RetainedContextWrite.class));
      SimpleScheduler scheduler = SimpleScheduler.getInstance(instance);
      scheduler.setupSchedulable(identity("retained"), processType(), Map.of("processName", PROCESS),
         new QScheduleMetaData().withRepeatSeconds(3600).withInitialDelayMillis(0), true);
      Runnable registeredRunner = scheduler.getExecutors().get(0).getRunnable();
      AtomicReference<Thread> workerThread = new AtomicReference<>();
      CountDownLatch workerFinished = new CountDownLatch(1);
      StandardScheduledExecutor ownedWorker = new StandardScheduledExecutor(() ->
      {
         workerThread.set(Thread.currentThread());
         seedWorker(workerInstance, workerSession, originalObjects);
         try
         {
            registeredRunner.run();
         }
         finally
         {
            observeWorker();
            workerFinished.countDown();
         }
      });
      ownedWorker.setName("ownedStopTimeoutWorker");
      ownedWorker.setInitialDelayMillis(0);
      ownedWorker.setDelayMillis(3600000);
      CompletableFuture<Boolean> stopResult = new CompletableFuture<>();
      Thread stopper = new Thread(() ->
      {
         try
         {
            stopResult.complete(ownedWorker.stop());
         }
         catch(Throwable error)
         {
            stopResult.completeExceptionally(error);
         }
      }, "ownedStopTimeoutCaller");
      List<List<String>> before = rows("SELECT * FROM person ORDER BY id");
      try
      {
         assertTrue(ownedWorker.start());
         assertTrue(observation.entered.await(5, TimeUnit.SECONDS), "Owned application step did not enter");
         long started = System.nanoTime();
         stopper.start();
         Boolean stopped = stopResult.get(330, TimeUnit.SECONDS);
         long elapsed = System.nanoTime() - started;
         assertFalse(stopped, "Expected the actual stop result, not an observer timeout");
         assertTrue(elapsed >= TimeUnit.SECONDS.toNanos(300), "Stop returned before its existing termination wait elapsed");
         assertEquals(StandardScheduledExecutor.RunningState.STOPPING, ownedWorker.getRunningState());
         assertTrue(workerThread.get().isAlive());
         assertEquals(1, workerFinished.getCount());
         assertEquals(1, observation.release.getCount());
         assertEquals(0, observation.writes.get());
         assertEquals(before, rows("SELECT * FROM person ORDER BY id"));
         assertSame(instance, observation.insideInstance);
         assertEquals("scheduled-user", observation.insideUser);
         assertNull(observation.insideMarker);
         observation.release.countDown();
         assertTrue(workerFinished.await(5, TimeUnit.SECONDS));
         workerThread.get().join(5000);
         assertFalse(workerThread.get().isAlive(), "Owned native worker must terminate after retained work finishes");
         ContextResult result = observation.contextResults.poll(5, TimeUnit.SECONDS);
         assertNotNull(result);
         assertAll(
            () -> assertSame(workerInstance, result.restoredInstance()),
            () -> assertSame(workerSession, result.restoredSession()),
            () -> assertSame(originalObjects, result.restoredObjects()),
            () -> assertEquals(Map.of("workerMarker", "worker-only"), originalObjects),
            () -> assertEquals(result.beforeThreadName(), result.afterThreadName()));
         assertEquals(1, observation.writes.get());
         assertEquals(List.of(List.of("Scheduled", "1")), rows("SELECT first_name,days_worked FROM person WHERE id=1"));
         assertEquals(before.subList(1, before.size()), rows("SELECT * FROM person WHERE id>=2 ORDER BY id"));
         assertEquals("caller", QContext.getQSession().getUser().getIdReference());
         assertEquals(StandardScheduledExecutor.RunningState.STOPPING, ownedWorker.getRunningState(),
            "The existing API retains STOPPING after timeout even once its worker has exited");
      }
      finally
      {
         observation.release.countDown();
         if(ownedWorker.getRunningState() == StandardScheduledExecutor.RunningState.RUNNING)
         {
            ownedWorker.stop();
         }
         stopper.join(5000);
         if(workerThread.get() != null)
         {
            workerThread.get().join(5000);
            assertFalse(workerThread.get().isAlive(), "Fixture left its owned worker running");
         }
         assertFalse(stopper.isAlive(), "Fixture left its owned stop caller running");
      }
   }



   /*******************************************************************************
    ** Quartz offers listener hooks. Simple exposes its registered Runnable; wrap
    ** that unchanged runner with native StandardScheduledExecutor hooks to seed and
    ** inspect a pre-existing worker context. Neither path calls application code
    ** directly or substitutes its runner. Timer behavior is covered separately.
    *******************************************************************************/
   private void assertWorkerContext(boolean useQuartz, boolean failFirst) throws Exception
   {
      QInstance workerInstance = SampleMetaDataProvider.defineTestInstance();
      QSession workerSession = new QSession().withUser(new QUser().withIdReference("worker-owner"));
      Map<String, Serializable> originalObjects = new LinkedHashMap<>(Map.of("workerMarker", "worker-only"));
      observation.failContextFirst = failFirst;
      instance.getProcess(PROCESS).getBackendStep("writePerson").setCode(new QCodeReference(InspectContextWrite.class));
      BasicSchedulableIdentity id = identity("context");
      QScheduleMetaData schedule = new QScheduleMetaData().withRepeatSeconds(60).withInitialDelayMillis(0);
      StandardScheduledExecutor ownedWorker;
      if(useQuartz)
      {
         ownedWorker = null;
         quartz.getListenerManager().addJobListener(new JobListenerSupport()
         {
            /*******************************************************************************
             **
             *******************************************************************************/
            @Override
            public String getName()
            {
               return "ownedContextProbe";
            }



            /*******************************************************************************
             ** Seed independently of the scheduled application's instance and session.
             *******************************************************************************/
            @Override
            public void jobToBeExecuted(JobExecutionContext context)
            {
               seedWorker(workerInstance, workerSession, originalObjects);
            }



            /*******************************************************************************
             ** Observe after QuartzJobRunner's finally block has executed.
             *******************************************************************************/
            @Override
            public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException)
            {
               observeWorker();
            }
         });
         QuartzScheduler scheduler = QuartzScheduler.getInstance();
         scheduler.doNotStart();
         scheduler.setupSchedulable(id, processType(), Map.of("processName", PROCESS), schedule, true);
      }
      else
      {
         SimpleScheduler scheduler = SimpleScheduler.getInstance(instance);
         scheduler.setupSchedulable(id, processType(), Map.of("processName", PROCESS), schedule, true);
         Runnable registeredRunner = scheduler.getExecutors().get(0).getRunnable();
         ownedWorker = new StandardScheduledExecutor(() ->
         {
            seedWorker(workerInstance, workerSession, originalObjects);
            try
            {
               registeredRunner.run();
            }
            finally
            {
               observeWorker();
            }
         });
         ownedWorker.setName("ownedContextWorker");
         ownedWorker.setInitialDelayMillis(0);
         ownedWorker.setDelayMillis(60000);
      }
      List<List<String>> before = rows("SELECT * FROM person ORDER BY id");
      try
      {
         for(int attempt = 0; attempt < 2; attempt++)
         {
            if(useQuartz)
            {
               if(attempt == 0)
               {
                  QuartzScheduler.getInstance().start();
               }
               else
               {
                  quartz.triggerJob(new JobKey("context", "PROCESS"));
               }
            }
            else
            {
               assertTrue(ownedWorker.start());
            }
            ContextResult result = observation.contextResults.poll(5, TimeUnit.SECONDS);
            assertNotNull(result, "Owned worker context observation timed out");
            if(!useQuartz)
            {
               assertTrue(ownedWorker.stop());
            }
            assertAll(
               () -> assertSame(instance, result.insideInstance()),
               () -> assertEquals("scheduled-user", result.insideUser()),
               () -> assertNull(result.insideMarker(), "Job must not inherit worker-only objects"),
               () -> assertSame(workerInstance, result.restoredInstance()),
               () -> assertSame(workerSession, result.restoredSession()),
               () -> assertEquals("worker-owner", result.restoredSession().getUser().getIdReference()),
               () -> assertSame(originalObjects, result.restoredObjects(), "Restore the exact prior map"),
               () -> assertEquals(Map.of("workerMarker", "worker-only"), originalObjects),
               () -> assertEquals(result.beforeThreadName(), result.afterThreadName()));
            if(failFirst && attempt == 0)
            {
               assertEquals(0, observation.writes.get());
               assertEquals(before, rows("SELECT * FROM person ORDER BY id"));
            }
            else
            {
               assertEquals(List.of(List.of("Scheduled")), rows("SELECT first_name FROM person WHERE id=1"));
            }
         }
         assertEquals(failFirst ? 1 : 2, observation.writes.get());
         assertEquals("caller", QContext.getQSession().getUser().getIdReference());
      }
      finally
      {
         if(ownedWorker != null && ownedWorker.getRunningState() == StandardScheduledExecutor.RunningState.RUNNING)
         {
            ownedWorker.stop();
         }
         manager.stop();
      }
   }



   /*******************************************************************************
    ** Synthetic worker ownership is established outside the code being tested.
    *******************************************************************************/
   private void seedWorker(QInstance workerInstance, QSession workerSession, Map<String, Serializable> objects)
   {
      QContext.init(workerInstance, workerSession);
      QContext.setObjects(objects);
      observation.workerThreadName = Thread.currentThread().getName();
   }



   /*******************************************************************************
    ** Snapshot references before clearing only this fixture's worker context.
    *******************************************************************************/
   private void observeWorker()
   {
      observation.contextResults.add(new ContextResult(observation.insideInstance, observation.insideUser, observation.insideMarker,
         QContext.getQInstance(), QContext.getQSession(), QContext.getObjects(), observation.workerThreadName, Thread.currentThread().getName()));
      QContext.clear();
   }



   /*******************************************************************************
    ** The existing global disable switch leaves both native dispatchers unstarted.
    *******************************************************************************/
   @Test
   void disabledManagerDoesNotRegisterOrDispatch() throws Exception
   {
      String previous = System.getProperty("qqq.scheduleManager.enabled");
      try
      {
         System.setProperty("qqq.scheduleManager.enabled", "false");
         instance.getProcess(PROCESS).setSchedule(new QScheduleMetaData().withSchedulerName(quartzName).withRepeatMillis(50).withInitialDelayMillis(0));
         manager.start();
         assertFalse(quartz.isStarted());
         assertTrue(quartz.getJobKeys(GroupMatcher.anyJobGroup()).isEmpty());
         assertTrue(SimpleScheduler.getInstance(instance).getExecutors().isEmpty());
         assertEquals(0, observation.writes.get());
      }
      finally
      {
         if(previous == null)
         {
            System.clearProperty("qqq.scheduleManager.enabled");
         }
         else
         {
            System.setProperty("qqq.scheduleManager.enabled", previous);
         }
      }
   }



   /*******************************************************************************
    ** Await completion signals with a fixed budget, without polling/sleeping.
    *******************************************************************************/
   private Invocation next() throws Exception
   {
      Invocation result = observation.completed.poll(5, TimeUnit.SECONDS);
      assertNotNull(result, "Owned scheduled job did not complete within five seconds");
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private BasicSchedulableIdentity identity(String name)
   {
      return new BasicSchedulableIdentity(name, "Owned sample " + name);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private SchedulableType processType()
   {
      return instance.getSchedulableType(ScheduledJobType.PROCESS.name());
   }



   /*******************************************************************************
    ** Independent JDBC observes committed rows outside scheduler worker threads.
    *******************************************************************************/
   private List<List<String>> rows(String sql) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
         Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               row.add(result.getString(column));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Owned synchronization and observations; never used to replace scheduler behavior.
    *******************************************************************************/
   private static class Observation
   {
      private final BlockingQueue<Invocation> completed = new LinkedBlockingQueue<>();
      private final Set<UUID> processIds = ConcurrentHashMap.newKeySet();
      private final BlockingQueue<ContextResult> contextResults = new LinkedBlockingQueue<>();
      private volatile QInstance insideInstance;
      private volatile boolean failContextFirst;
      private final AtomicInteger contextAttempts = new AtomicInteger();
      private volatile String workerThreadName;
      private volatile String insideUser;
      private volatile Serializable insideMarker;
      private volatile boolean failQueue;
      private volatile Object queueBodies;
      private final AtomicInteger writes = new AtomicInteger();
      private final AtomicInteger attempts = new AtomicInteger();
      private final AtomicInteger active = new AtomicInteger();
      private final AtomicInteger maxActive = new AtomicInteger();
      private final BlockingQueue<VariantInvocation> variantsEntered = new LinkedBlockingQueue<>();
      private final BlockingQueue<VariantInvocation> variantsCompleted = new LinkedBlockingQueue<>();
      private final CountDownLatch entered = new CountDownLatch(1);
      private final CountDownLatch release = new CountDownLatch(1);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private record Invocation(long atNanos, String user)
   {
   }



   /*******************************************************************************
    ** Preserve references, not copies, to verify the existing worker's exact identity.
    *******************************************************************************/
   private record ContextResult(QInstance insideInstance, String insideUser, Serializable insideMarker,
                                QInstance restoredInstance, QSession restoredSession, Map<String, Serializable> restoredObjects,
                                String beforeThreadName, String afterThreadName)
   {
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private record VariantInvocation(int id, long thread, String user)
   {
   }



   /*******************************************************************************
    ** Real application step, blocked only by owned test synchronization.
    *******************************************************************************/
   public static class WriteVariant implements BackendStep
   {
      /*******************************************************************************
       ** Every worker reads its own selected variant and updates that native row.
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         rememberProcess();
         int id = (Integer) QContext.getQSession().getBackendVariants().get("ownedVariant");
         VariantInvocation invocation = new VariantInvocation(id, Thread.currentThread().threadId(), QContext.getQSession().getUser().getIdReference());
         int active = observation.active.incrementAndGet();
         observation.maxActive.accumulateAndGet(active, Math::max);
         observation.variantsEntered.add(invocation);
         try
         {
            awaitRelease();
            QRecord record = new UpdateAction().execute(new UpdateInput("person").withRecord(new QRecord().withValue("id", id)
               .withValue("firstName", "Variant" + id))).getRecords().get(0);
            if(!record.getErrors().isEmpty())
            {
               throw new QException("Owned variant write failed");
            }
         }
         finally
         {
            observation.active.decrementAndGet();
         }
         observation.variantsCompleted.add(invocation);
      }
   }



   /*******************************************************************************
    ** The real scheduler pushes its process input; remember only those owned UUIDs.
    *******************************************************************************/
   private static void rememberProcess()
   {
      for(var action : QContext.getActionStack())
      {
         if(action instanceof RunProcessInput process && process.getProcessUUID() != null)
         {
            observation.processIds.add(UUID.fromString(process.getProcessUUID()));
         }
      }
   }



   /*******************************************************************************
    ** Bounded latch wait prevents an assertion failure from stranding worker threads.
    *******************************************************************************/
   private static void awaitRelease() throws QException
   {
      try
      {
         if(!observation.release.await(10, TimeUnit.SECONDS))
         {
            throw new QException("Owned scheduling fixture release timed out");
         }
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
         throw new QException("Owned scheduling fixture interrupted", e);
      }
   }



   /*******************************************************************************
    ** First invocation fails before DML; later native firings execute the normal step.
    *******************************************************************************/
   public static class FailFirstWrite extends WritePerson
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         rememberProcess();
         if(observation.attempts.incrementAndGet() == 1)
         {
            throw new QException("Owned scheduled failure");
         }
         super.run(input, output);
      }
   }



   /*******************************************************************************
    ** Retain a real process inside its job context until the acceptance test releases
    ** it. The application adds no execution deadline and does not force cancellation.
    *******************************************************************************/
   public static class RetainedContextWrite extends WritePerson
   {
      /*******************************************************************************
       ** Native timeout must leave work entered and uncommitted until explicit release.
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         rememberProcess();
         observation.insideInstance = QContext.getQInstance();
         observation.insideUser = QContext.getQSession().getUser().getIdReference();
         observation.insideMarker = QContext.getObject("workerMarker");
         QContext.setObject("jobMarker", "job-only");
         observation.entered.countDown();
         try
         {
            observation.release.await();
         }
         catch(InterruptedException e)
         {
            Thread.currentThread().interrupt();
            throw new QException("Owned retained application work was interrupted", e);
         }
         super.run(input, output);
      }
   }



   /*******************************************************************************
    ** Controlled application wait distinguishes shutdown waiting from cancellation.
    *******************************************************************************/
   public static class BlockedWrite extends WritePerson
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         rememberProcess();
         observation.entered.countDown();
         awaitRelease();
         super.run(input, output);
      }
   }



   /*******************************************************************************
    ** The real automation handler updates a selected row using the QQQ action API.
    *******************************************************************************/
   public static class AutomatePerson implements RecordAutomationHandlerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void execute(RecordAutomationInput input) throws QException
      {
         for(QRecord record : input.getRecordList())
         {
            QRecord result = new UpdateAction().execute(new UpdateInput("person").withOmitTriggeringAutomations(true)
               .withRecord(new QRecord().withValue("id", record.getValue("id")).withValue("firstName", "Automated"))).getRecords().get(0);
            if(!result.getErrors().isEmpty())
            {
               throw new QException("Owned automation update failed");
            }
         }
         observation.completed.add(new Invocation(System.nanoTime(), QContext.getQSession().getUser().getIdReference()));
      }
   }



   /*******************************************************************************
    ** The body delivered by the unmodified queue runner determines persisted data.
    *******************************************************************************/
   public static class WriteQueuedPerson implements BackendStep
   {
      /*******************************************************************************
       ** Fail before DML to prove failed work does not acknowledge the receipt.
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         rememberProcess();
         observation.queueBodies = input.getValue("bodies");
         if(observation.failQueue)
         {
            throw new QException("Owned queued process failure");
         }
         @SuppressWarnings("unchecked")
         List<String> bodies = (List<String>) input.getValue("bodies");
         QRecord result = new UpdateAction().execute(new UpdateInput("person").withRecord(new QRecord().withValue("id", 1)
            .withValue("firstName", bodies.get(0)))).getRecords().get(0);
         if(!result.getErrors().isEmpty())
         {
            throw new QException("Owned queued write failed");
         }
      }
   }



   /*******************************************************************************
    ** Observe the real scheduled context before executing normal application DML.
    *******************************************************************************/
   public static class InspectContextWrite extends WritePerson
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         rememberProcess();
         observation.insideInstance = QContext.getQInstance();
         observation.insideUser = QContext.getQSession().getUser().getIdReference();
         observation.insideMarker = QContext.getObject("workerMarker");
         QContext.setObject("jobMarker", "job-only");
         if(observation.contextAttempts.incrementAndGet() == 1 && observation.failContextFirst)
         {
            throw new QException("Owned context failure");
         }
         super.run(input, output);
      }
   }



   /*******************************************************************************
    ** First-party application step executes real QQQ DML in the supplied job context.
    *******************************************************************************/
   public static class WritePerson implements BackendStep
   {
      /*******************************************************************************
       ** Completion is published only after the normal backend write succeeds.
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         rememberProcess();
         int count = observation.writes.incrementAndGet();
         QRecord result = new UpdateAction().execute(new UpdateInput("person")
            .withRecord(new QRecord().withValue("id", 1).withValue("firstName", "Scheduled").withValue("daysWorked", count))).getRecords().get(0);
         if(!result.getErrors().isEmpty())
         {
            throw new QException("Owned scheduled write failed: " + result.getErrorsAsString());
         }
         observation.completed.add(new Invocation(System.nanoTime(), QContext.getQSession().getUser().getIdReference()));
      }
   }
}
