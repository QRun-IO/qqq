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

package com.kingsrook.qqq.backend.core.scheduler.quartz;


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.BaseTest;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.quartz.QuartzSchedulerMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.scheduler.QScheduleManager;
import com.kingsrook.qqq.backend.core.scheduler.SchedulerTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.utils.ConnectionProvider;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Tests for QuartzScheduler running on a persistent, clustered JDBC job store
 ** (JobStoreTX with the PostgreSQL delegate and Quartz's own PostgreSQL DDL, on
 ** an in-memory H2 database in PostgreSQL mode).
 **
 ** Covers QRun-IO/qqq#1044: QScheduleManager.start() must create the jobs on a
 ** fresh persistent store, and a later start (a restart, or another clustered
 ** node booting against the same store) must leave existing jobs and triggers
 ** alone, while still adding jobs for schedulables that are new.
 *******************************************************************************/
class QuartzSchedulerPersistentJobStoreTest extends BaseTest
{
   private static final String DATA_SOURCE_NAME = "qqqTestQuartzDataSource";

   private String jdbcUrl;
   private Connection keepAliveConnection;



   /*******************************************************************************
    ** Create a fresh in-memory database with the Quartz tables in it.
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      jdbcUrl = "jdbc:h2:mem:quartz_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=PostgreSQL";
      keepAliveConnection = DriverManager.getConnection(jdbcUrl, "sa", "");

      String ddl;
      try(InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("org/quartz/impl/jdbcjobstore/tables_postgres.sql"))
      {
         assertNotNull(inputStream, "Quartz PostgreSQL DDL should be on the classpath");
         ddl = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      }

      StringBuilder withoutComments = new StringBuilder();
      for(String line : ddl.split("\\R"))
      {
         if(!line.trim().startsWith("--"))
         {
            withoutComments.append(line).append("\n");
         }
      }

      try(Statement statement = keepAliveConnection.createStatement())
      {
         for(String sql : withoutComments.toString().split(";"))
         {
            if(!sql.isBlank() && !sql.trim().equalsIgnoreCase("COMMIT"))
            {
               statement.execute(sql);
            }
         }
      }

      H2ConnectionProvider.url = jdbcUrl;
   }



   /*******************************************************************************
    ** Stop & un-init the schedulers, then drop the in-memory database.
    *******************************************************************************/
   @AfterEach
   void afterEach() throws SQLException
   {
      SchedulerTestUtils.afterEach();
      keepAliveConnection.close();
   }



   /*******************************************************************************
    ** A fresh persistent store gets a job for each schedulable when the schedule
    ** manager starts - without anyone running Reschedule All Jobs.
    *******************************************************************************/
   @Test
   void testStartCreatesJobsOnFreshPersistentStore() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      setupInstance(qInstance, "persistentProcessA");

      assertEquals(0, countRows("QRTZ_JOB_DETAILS"), "Store should start out empty");

      startScheduleManager(qInstance, "node1");

      Map<String, Long> triggerStartTimes = readTriggerStartTimes();
      assertThat(triggerStartTimes).containsKey("process:persistentProcessA");
      assertEquals(countRows("QRTZ_JOB_DETAILS"), countRows("QRTZ_TRIGGERS"), "Each job should have exactly one trigger");
      assertEquals(triggerStartTimes.size(), QuartzScheduler.getInstance().queryQuartz().size());
   }



   /*******************************************************************************
    ** Starting again against the same store (a restart, or a second clustered node)
    ** must not replace or duplicate existing jobs and triggers, but must add jobs
    ** for schedulables that were not in the store yet.
    *******************************************************************************/
   @Test
   void testSecondStartIsIdempotentAndAddsOnlyMissingJobs() throws Exception
   {
      QInstance qInstance = QContext.getQInstance();
      setupInstance(qInstance, "persistentProcessA");

      startScheduleManager(qInstance, "node1");
      Map<String, Long> firstStartTriggers = readTriggerStartTimes();
      assertThat(firstStartTriggers).containsKey("process:persistentProcessA");

      ////////////////////////////////////////////////////////////////////////////////
      // stop this "node", and let some time pass, so a replaced trigger would have //
      // a different start time than the original one                               //
      ////////////////////////////////////////////////////////////////////////////////
      SchedulerTestUtils.afterEach();
      Thread.sleep(20);

      ////////////////////////////////////////////////////////////////////////////
      // boot another node against the same store, with one additional process //
      ////////////////////////////////////////////////////////////////////////////
      qInstance.addProcess(buildProcess("persistentProcessB"));
      startScheduleManager(qInstance, "node2");

      Map<String, Long> secondStartTriggers = readTriggerStartTimes();
      assertThat(secondStartTriggers).containsKey("process:persistentProcessB");
      assertEquals(firstStartTriggers.size() + 1, secondStartTriggers.size());
      assertEquals(countRows("QRTZ_JOB_DETAILS"), countRows("QRTZ_TRIGGERS"), "Each job should have exactly one trigger");

      Map<String, Long> carriedOverTriggers = new HashMap<>(secondStartTriggers);
      carriedOverTriggers.remove("process:persistentProcessB");
      assertEquals(firstStartTriggers, carriedOverTriggers, "Existing triggers should not have been replaced");
   }



   /*******************************************************************************
    ** Point the instance's schedulables at a quartz scheduler that uses the H2
    ** JDBC store, with long initial delays so nothing fires during the test.
    *******************************************************************************/
   private void setupInstance(QInstance qInstance, String processName)
   {
      qInstance.getSchedulers().clear();
      qInstance.addScheduler(new QuartzSchedulerMetaData()
         .withProperties(getPersistentQuartzProperties("unused"))
         .withName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME));

      qInstance.getTables().values().forEach(t ->
      {
         if(t.getAutomationDetails() != null)
         {
            t.getAutomationDetails().getSchedule()
               .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME)
               .withInitialDelayMillis(3_600_000);
         }
      });

      qInstance.getQueues().values()
         .forEach(q -> q.getSchedule()
            .withSchedulerName(QuartzTestUtils.QUARTZ_SCHEDULER_NAME)
            .withInitialDelayMillis(3_600_000));

      qInstance.addProcess(buildProcess(processName));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QProcessMetaData buildProcess(String name)
   {
      QProcessMetaData process = SchedulerTestUtils.buildTestProcess(name, QuartzTestUtils.QUARTZ_SCHEDULER_NAME);
      process.getSchedule()
         .withRepeatMillis(null)
         .withRepeatSeconds(3600)
         .withInitialDelaySeconds(null)
         .withInitialDelayMillis(3_600_000);
      return (process);
   }



   /*******************************************************************************
    ** Init & start the schedule manager as a given clustered node.
    *******************************************************************************/
   private void startScheduleManager(QInstance qInstance, String instanceId) throws Exception
   {
      QuartzSchedulerMetaData schedulerMetaData = (QuartzSchedulerMetaData) qInstance.getSchedulers().get(QuartzTestUtils.QUARTZ_SCHEDULER_NAME);
      schedulerMetaData.setProperties(getPersistentQuartzProperties(instanceId));

      QSession         qSession         = QContext.getQSession();
      QScheduleManager qScheduleManager = QScheduleManager.initInstance(qInstance, () -> qSession);
      qScheduleManager.start();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Properties getPersistentQuartzProperties(String instanceId)
   {
      Properties properties = new Properties();
      properties.put("org.quartz.scheduler.instanceName", QuartzTestUtils.QUARTZ_SCHEDULER_NAME);
      properties.put("org.quartz.scheduler.instanceId", instanceId);
      properties.put("org.quartz.threadPool.threadCount", "2");
      properties.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
      properties.put("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate");
      properties.put("org.quartz.jobStore.tablePrefix", "QRTZ_");
      properties.put("org.quartz.jobStore.isClustered", "true");
      properties.put("org.quartz.jobStore.dataSource", DATA_SOURCE_NAME);
      properties.put("org.quartz.dataSource." + DATA_SOURCE_NAME + ".connectionProvider.class", H2ConnectionProvider.class.getName());
      return (properties);
   }



   /*******************************************************************************
    ** Read trigger start times (by trigger name) for our scheduler, straight from
    ** the database.
    *******************************************************************************/
   private Map<String, Long> readTriggerStartTimes() throws SQLException
   {
      Map<String, Long> rs = new HashMap<>();
      try(PreparedStatement statement = keepAliveConnection.prepareStatement("SELECT TRIGGER_NAME, START_TIME FROM QRTZ_TRIGGERS WHERE SCHED_NAME = ?"))
      {
         statement.setString(1, QuartzTestUtils.QUARTZ_SCHEDULER_NAME);
         try(ResultSet resultSet = statement.executeQuery())
         {
            while(resultSet.next())
            {
               rs.put(resultSet.getString(1), resultSet.getLong(2));
            }
         }
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer countRows(String tableName) throws SQLException
   {
      try(Statement statement = keepAliveConnection.createStatement();
         ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM " + tableName))
      {
         resultSet.next();
         return (resultSet.getInt(1));
      }
   }



   /*******************************************************************************
    ** Minimal (non-pooling) quartz connection provider for the H2 test database -
    ** quartz's own pooling providers need c3p0 or HikariCP, which aren't on the
    ** classpath here.
    *******************************************************************************/
   public static class H2ConnectionProvider implements ConnectionProvider
   {
      private static String url;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Connection getConnection() throws SQLException
      {
         return (DriverManager.getConnection(url, "sa", ""));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void shutdown()
      {
         //////////////////////////////////////////////////
         // noop - connections are closed by their users //
         //////////////////////////////////////////////////
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void initialize()
      {
         //////////////////////////////////////////////
         // noop - the url is set by the test itself //
         //////////////////////////////////////////////
      }
   }

}
