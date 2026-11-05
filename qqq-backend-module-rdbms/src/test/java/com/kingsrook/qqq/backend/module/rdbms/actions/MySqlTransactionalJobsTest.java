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

package com.kingsrook.qqq.backend.module.rdbms.actions;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsTables;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/*******************************************************************************
 ** Opt-in real MySQL8.4 provider tests reuse all native transaction/capacity cases.
 ** Only a disposable test-owned pinned container is started, by the explicit gate.
 *******************************************************************************/
@EnabledIfSystemProperty(named = "mysqlTransactionalJobsTests", matches = "true")
class MySqlTransactionalJobsTest extends TransactionalJobsJdbcTest
{
   private static MySQLContainer mysql;
   private static final Map<String, Level> LOG_LEVELS = new HashMap<>();



   /*******************************************************************************
    ** Start a pinned isolated provider with synchronous durable storage settings.
    *******************************************************************************/
   @BeforeAll
   static void start()
   {
      for(String logger : List.of("org.apache.hc", "org.apache.http", "org.testcontainers", "com.github.dockerjava"))
      {
         LOG_LEVELS.put(logger, LogManager.getLogger(logger).getLevel());
         Configurator.setLevel(logger, Level.WARN);
      }
      mysql = new MySQLContainer(DockerImageName.parse("mysql@sha256:a0225d1ea6631c8bc2385dc4c2faba17bbf1834ecf369e1659a9a382bd8d5dd7"))
         .withDatabaseName("transactional_jobs").withUsername("root").withPassword(UUID.randomUUID().toString())
         .withCommand("--innodb-flush-log-at-trx-commit=1", "--sync-binlog=1", "--innodb-lock-wait-timeout=5", "--lock-wait-timeout=5", "--transaction-isolation=READ-COMMITTED");
      mysql.start();
   }



   /*******************************************************************************
    ** Stop only the owned provider and restore logging levels without credentials.
    *******************************************************************************/
   @AfterAll
   static void stop()
   {
      ConnectionManager.closeConnectionProviders();
      if(mysql != null)
      {
         mysql.stop();
      }
      LOG_LEVELS.forEach(Configurator::setLevel);
   }



   /*******************************************************************************
    ** Each native fixture receives its own database in the disposable provider.
    *******************************************************************************/
   @Override
   protected void configureBackend(RDBMSBackendMetaData backend) throws Exception
   {
      String database = "jobs_" + UUID.randomUUID().toString().replace("-", "");
      try(Connection connection = DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword()); Statement statement = connection.createStatement())
      {
         statement.execute("CREATE DATABASE " + database + " CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_bin");
      }
      backend.setVendor("mysql");
      backend.setHostName(mysql.getHost());
      backend.setPort(mysql.getMappedPort(3306));
      backend.setDatabaseName(database);
      backend.setUsername(mysql.getUsername());
      backend.setPassword(mysql.getPassword());
      url = "jdbc:mysql://" + mysql.getHost() + ":" + mysql.getMappedPort(3306) + "/" + database
         + "?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&useSSL=false&allowPublicKeyRetrieval=true";
      backend.setJdbcUrl(url);
   }



   /*******************************************************************************
    ** Independent current provider connection; no shared application database.
    *******************************************************************************/
   @Override
   protected Connection openConnection() throws Exception
   {
      return (DriverManager.getConnection(url, mysql.getUsername(), mysql.getPassword()));
   }



   /*******************************************************************************
    ** Exact native owner identity representation.
    *******************************************************************************/
   @Override
   protected String identityType()
   {
      return ("INT AUTO_INCREMENT PRIMARY KEY");
   }



   /*******************************************************************************
    ** All fixture-owned tables retain transactional case-sensitive identities.
    *******************************************************************************/
   @Override
   protected String tableSuffix()
   {
      return (" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin");
   }



   /*******************************************************************************
    ** MySQL-compatible primitive owner storage types.
    *******************************************************************************/
   @Override
   protected String type(QFieldMetaData field)
   {
      return (switch(field.getType())
      {
         case DATE_TIME -> "DATETIME(6)";
         case TEXT -> "LONGTEXT";
         default -> super.type(field);
      });
   }



   /*******************************************************************************
    ** Refuse equivalent MySQL mapping/collation weakening on real native DDL.
    *******************************************************************************/
   @Override
   @Test
   void refusesWeakenedMetadataAndCaseInsensitiveActorColumn() throws Exception
   {
      QFieldMetaData owner = instance.getTable(DurableJobsTables.TABLE_JOB).getField("userId");
      String original = owner.getBackendName();
      owner.setBackendName("client_channel");
      assertThrows(QException.class, () -> store.insertJob(job("QUEUED"), null));
      owner.setBackendName(original);
      try(Connection connection = openConnection(); Statement statement = connection.createStatement())
      {
         statement.execute("ALTER TABLE jobs_job MODIFY user_id VARCHAR(250) COLLATE utf8mb4_0900_ai_ci");
      }
      assertThrows(QException.class, () -> store.insertJob(job("QUEUED"), null));
      assertEquals(0, count("jobs_job"));
   }



   /*******************************************************************************
    ** Use the shipped MySQL additive migration in the inherited old-row fixture.
    *******************************************************************************/
   @Override
   protected String migrationScript()
   {
      return ("mysql-additive.sql");
   }



   /*******************************************************************************
    ** Actual nonrecovering durability settings refuse admission before any intent.
    *******************************************************************************/
   @Test
   void disabledFlushDurabilityRefusesAdmission() throws Exception
   {
      try(Connection connection = openConnection(); Statement statement = connection.createStatement())
      {
         try
         {
            statement.execute("SET GLOBAL innodb_flush_log_at_trx_commit=2");
            assertThrows(QException.class, () -> store.insertJob(job("QUEUED"), null));
         }
         finally
         {
            statement.execute("SET GLOBAL innodb_flush_log_at_trx_commit=1");
         }
      }
      assertEquals(0, count("jobs_job"));
   }


   /*******************************************************************************
    ** MySQL DATE loses required time precision and must fail strict qualification.
    *******************************************************************************/
   @Override
   protected String incompatibleTemporalStorageSql()
   {
      return ("ALTER TABLE jobs_chunk MODIFY lease_expires_at DATE NULL");
   }

}
