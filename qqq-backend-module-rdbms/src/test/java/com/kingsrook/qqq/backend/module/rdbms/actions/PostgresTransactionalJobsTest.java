/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2025.  Kingsrook, LLC
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
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerHelper.RequiredListenerException;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.TransactionalRecordChangeMetaData;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsTables;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.module.postgres.model.metadata.PostgreSQLBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Opt-in real native PostgreSQL 17 (native postgres backend type) run of every
 ** inherited transactional-record and durable-Jobs case: native Insert, Update and
 ** Delete paths with hooked records, same-transaction source/job/chunk/grant/event
 ** intent, rollback, exactly-once, idempotency, capacity concurrency and the
 ** unsupported fail-closed guards. The suite adds PostgreSQL-specific qualification
 ** refusals. Only a disposable loopback container with random synthetic passwords is
 ** used; the application connects as a DML-only role that does not own any table.
 *******************************************************************************/
@EnabledIfSystemProperty(named = "postgresTransactionalJobsTests", matches = "true")
class PostgresTransactionalJobsTest extends TransactionalJobsJdbcTest
{
   private static final boolean BRIDGE_ADDRESS = Boolean.getBoolean("postgresTransactionalJobsTests.bridgeAddress");

   private static final String APP_ROLE = "jobs_app";
   private static final String ROOT_PASSWORD = UUID.randomUUID().toString();
   private static final String APP_PASSWORD = UUID.randomUUID().toString();
   private static final String OPTIONS = "?connectTimeout=5&socketTimeout=30&options=-c%20TimeZone%3DUTC";
   private static GenericContainer<?> postgres;
   private static TimeZone previousZone;
   private static String server;
   private String database;



   /*******************************************************************************
    ** Start the disposable server with explicit synchronous durable settings.
    ** pgjdbc announces the JVM zone as the session TimeZone, so the suite runs UTC.
    *******************************************************************************/
   @BeforeAll
   static void start() throws Exception
   {
      previousZone = TimeZone.getDefault();
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      postgres = new GenericContainer<>("postgres:17")
         .withEnv("POSTGRES_PASSWORD", ROOT_PASSWORD).withEnv("POSTGRES_DB", "bootstrap")
         .withCommand("postgres", "-c", "fsync=on", "-c", "synchronous_commit=on", "-c", "full_page_writes=on")
         .withExposedPorts(5432)
         .withCreateContainerCmdModifier(command ->
         {
            command.getHostConfig().withMemory(768L * 1024 * 1024);
            if(!BRIDGE_ADDRESS)
            {
               command.getHostConfig().withPortBindings(new PortBinding(Ports.Binding.bindIpAndPort("127.0.0.1", 0), new ExposedPort(5432)));
            }
         })
         .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2).withStartupTimeout(Duration.ofMinutes(2)));
      postgres.start();
      ///////////////////////////////////////////////////////////////////////////////////////////
      // default: the published loopback port.  When this JVM runs in a sibling container (the   //
      // disposable Linux gate; no host networking), the server is reached by its bridge address //
      // and no host port is published at all.                                                   //
      ///////////////////////////////////////////////////////////////////////////////////////////
      server = BRIDGE_ADDRESS
         ? "jdbc:postgresql://" + postgres.getContainerInfo().getNetworkSettings().getNetworks().values().iterator().next().getIpAddress() + ":5432/"
         : "jdbc:postgresql://127.0.0.1:" + postgres.getMappedPort(5432) + "/";
      try(Connection connection = connect(server + "bootstrap" + OPTIONS, "postgres", ROOT_PASSWORD); Statement statement = connection.createStatement())
      {
         statement.execute("CREATE ROLE " + APP_ROLE + " LOGIN PASSWORD '" + APP_PASSWORD + "' NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS");
         statement.execute("ALTER ROLE " + APP_ROLE + " SET lock_timeout='5s'");
      }
   }



   /*******************************************************************************
    ** Remove only this exact disposable container and restore the JVM zone.
    *******************************************************************************/
   @AfterAll
   static void stop()
   {
      ConnectionManager.closeConnectionProviders();
      if(postgres != null)
      {
         postgres.close();
      }
      if(previousZone != null)
      {
         TimeZone.setDefault(previousZone);
      }
   }



   /*******************************************************************************
    ** The native postgres module metadata (vendor and backend type "postgres").
    *******************************************************************************/
   @Override
   protected RDBMSBackendMetaData newBackend()
   {
      PostgreSQLBackendMetaData backend = new PostgreSQLBackendMetaData();
      backend.setName(super.newBackend().getName());
      return (backend);
   }



   /*******************************************************************************
    ** A new database per case; the application identity is the DML-only role, the
    ** superuser (used only by openConnection) owns every table.
    *******************************************************************************/
   @Override
   protected void configureBackend(RDBMSBackendMetaData backend) throws Exception
   {
      TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      database = "jobs_" + UUID.randomUUID().toString().replace("-", "");
      try(Connection root = connect(server + "bootstrap" + OPTIONS, "postgres", ROOT_PASSWORD); Statement statement = root.createStatement())
      {
         statement.execute("ALTER ROLE " + APP_ROLE + " SET lock_timeout='5s'");
         statement.execute("ALTER ROLE " + APP_ROLE + " RESET synchronous_commit");
         statement.execute("ALTER ROLE " + APP_ROLE + " RESET default_transaction_isolation");
         statement.execute("ALTER ROLE " + APP_ROLE + " RESET default_transaction_read_only");
         statement.execute("ALTER ROLE " + APP_ROLE + " NOSUPERUSER NOCREATEDB NOCREATEROLE NOREPLICATION NOBYPASSRLS");
         statement.execute("CREATE DATABASE " + database);
         statement.execute("REVOKE ALL ON DATABASE " + database + " FROM PUBLIC");
         statement.execute("GRANT CONNECT ON DATABASE " + database + " TO " + APP_ROLE);
      }
      url = server + database + OPTIONS;
      try(Connection root = openConnection(); Statement statement = root.createStatement())
      {
         statement.execute("GRANT USAGE ON SCHEMA public TO " + APP_ROLE);
         statement.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO " + APP_ROLE);
         statement.execute("ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO " + APP_ROLE);
      }
      backend.setDatabaseName(database);
      backend.setUsername(APP_ROLE);
      backend.setPassword(APP_PASSWORD);
      backend.setJdbcUrl(url);
   }



   /*******************************************************************************
    ** Independent superuser connection for fixture DDL, fault injection and
    ** committed-state observation.
    *******************************************************************************/
   @Override
   protected Connection openConnection() throws Exception
   {
      return (connect(url, "postgres", ROOT_PASSWORD));
   }



   /*******************************************************************************
    ** Standard SQL identifier quoting.
    *******************************************************************************/
   @Override
   protected String quote(String identifier)
   {
      return ("\"" + identifier + "\"");
   }



   /*******************************************************************************
    ** Standard identity owner key.
    *******************************************************************************/
   @Override
   protected String identityType()
   {
      return ("INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY");
   }



   /*******************************************************************************
    ** PostgreSQL storage types for the public owner metadata.
    *******************************************************************************/
   @Override
   protected String type(QFieldMetaData field)
   {
      return (switch(field.getType())
      {
         case DATE_TIME -> "TIMESTAMPTZ";
         case TEXT -> "TEXT";
         default -> super.type(field);
      });
   }



   /*******************************************************************************
    ** The shipped PostgreSQL additive migration in the inherited old-row fixture.
    *******************************************************************************/
   @Override
   protected String migrationScript()
   {
      return ("postgres-additive.sql");
   }



   /*******************************************************************************
    ** A date-only column cannot hold the lease instants.
    *******************************************************************************/
   @Override
   protected String incompatibleTemporalStorageSql()
   {
      return ("ALTER TABLE jobs_chunk ALTER COLUMN lease_expires_at TYPE DATE");
   }



   /*******************************************************************************
    ** Mapping weakening and a non-deterministic (case-insensitive) actor column fail
    ** before insertion.
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
      execute("CREATE COLLATION jobs_case_insensitive (provider = icu, locale = 'und-u-ks-level2', deterministic = false)");
      execute("ALTER TABLE jobs_job ALTER COLUMN user_id TYPE VARCHAR(250) COLLATE jobs_case_insensitive");
      assertThrows(QException.class, () -> store.insertJob(job("QUEUED"), null));
      assertEquals(0, count("jobs_job"));
   }



   /*******************************************************************************
    ** A qualified hooked Delete enlists exactly one durable job in the same
    ** transaction and a failing hook rolls both the delete and the intent back.
    *******************************************************************************/
   @Test
   void nativeDeleteHookEnlistsAtomicallyAndRollsBack() throws Exception
   {
      Integer sourceId = new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "to-delete"))).getRecords().get(0).getValueInteger("id");
      int jobsBefore = count("jobs_job");
      instance.getTable("source_record").withSupplementalMetaData(new TransactionalRecordChangeMetaData(
         new QCodeReference(Listener.class), Set.of(RecordChangeType.DELETE)));
      new DeleteAction().execute(new DeleteInput("source_record").withPrimaryKey(sourceId));
      assertEquals(0, count("source_record"));
      assertEquals(jobsBefore + 1, count("jobs_job"));
      Integer second = insertDirect("kept");
      execute("ALTER TABLE jobs_event ADD CONSTRAINT refuse_any_event CHECK(type = 'NEVER') NOT VALID");
      assertThrows(RequiredListenerException.class, () -> new DeleteAction().execute(new DeleteInput("source_record").withPrimaryKey(second)));
      execute("ALTER TABLE jobs_event DROP CONSTRAINT refuse_any_event");
      assertEquals(1, count("source_record"));
      assertEquals(jobsBefore + 1, count("jobs_job"));
   }



   /*******************************************************************************
    ** A hooked Update takes the same strict path and a refused admission leaves the
    ** row unchanged.
    *******************************************************************************/
   @Test
   void nativeUpdateHookRefusalLeavesSourceUnchanged() throws Exception
   {
      Integer sourceId = insertDirect("before");
      instance.getTable("source_record").withSupplementalMetaData(new TransactionalRecordChangeMetaData(
         new QCodeReference(Listener.class), Set.of(RecordChangeType.UPDATE)));
      execute("ALTER ROLE " + APP_ROLE + " SET synchronous_commit='off'");
      refresh();
      assertThrows(RequiredListenerException.class, () -> new UpdateAction().execute(new UpdateInput("source_record")
         .withRecord(new QRecord().withValue("id", sourceId).withValue("value", "after"))));
      assertEquals("before", observe("SELECT value FROM source_record WHERE id=" + sourceId));
      assertEquals(0, count("jobs_job"));
      execute("ALTER ROLE " + APP_ROLE + " RESET synchronous_commit");
      refresh();
      new UpdateAction().execute(new UpdateInput("source_record").withRecord(new QRecord().withValue("id", sourceId).withValue("value", "after")));
      assertEquals("after", observe("SELECT value FROM source_record WHERE id=" + sourceId));
      assertEquals(1, count("jobs_job"));
   }



   /*******************************************************************************
    ** Only the native postgres type is admitted. The same server under the generic
    ** rdbms type, or any other vendor label, refuses before source mutation.
    *******************************************************************************/
   @Test
   void onlyTheNativePostgresTypeIsAdmitted() throws Exception
   {
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) instance.getBackend(newBackend().getName());
      backend.setBackendType("rdbms");
      refuseInsert();
      backend.setBackendType("postgres");
      backend.setVendor("aurora-postgresql");
      refuseInsert();
      backend.setVendor("postgres");
      acceptInsert();
   }



   /*******************************************************************************
    ** Weak or unbounded session settings refuse admission before any intent, and
    ** each refusal is lifted by restoring the setting (so none is a blanket denial).
    *******************************************************************************/
   @Test
   void unsafeSessionSettingsRefuseAdmission() throws Exception
   {
      for(String[] setting : List.of(
         new String[] { "synchronous_commit='off'", "synchronous_commit" },
         new String[] { "lock_timeout='60s'", "lock_timeout" },
         new String[] { "default_transaction_isolation='repeatable read'", "default_transaction_isolation" },
         new String[] { "default_transaction_isolation='serializable'", "default_transaction_isolation" }))
      {
         execute("ALTER ROLE " + APP_ROLE + " SET " + setting[0]);
         refresh();
         refuseInsert();
         execute("ALTER ROLE " + APP_ROLE + " RESET " + setting[1]);
         execute("ALTER ROLE " + APP_ROLE + " SET lock_timeout='5s'");
      }
      execute("ALTER ROLE " + APP_ROLE + " RESET lock_timeout");
      refresh();
      refuseInsert();
      execute("ALTER ROLE " + APP_ROLE + " SET lock_timeout='5s'");
      refresh();
      acceptInsert();
   }



   /*******************************************************************************
    ** A non-UTC host zone is announced to the session and is refused rather than
    ** silently shifting lease instants.
    *******************************************************************************/
   @Test
   void nonUtcHostZoneRefusesAdmission() throws Exception
   {
      TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"));
      try
      {
         refresh();
         refuseInsert();
      }
      finally
      {
         TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
      }
      refresh();
      acceptInsert();
   }



   /*******************************************************************************
    ** The application role must be data-manipulation only: elevated attributes and
    ** table ownership refuse admission, and the real role cannot alter or drop.
    *******************************************************************************/
   @Test
   void onlyADataManipulationRoleThatOwnsNothingIsAdmitted() throws Exception
   {
      for(String attribute : List.of("CREATEDB", "CREATEROLE", "SUPERUSER"))
      {
         execute("ALTER ROLE " + APP_ROLE + " " + attribute);
         refresh();
         refuseInsert();
         execute("ALTER ROLE " + APP_ROLE + " NO" + attribute);
      }
      execute("ALTER TABLE source_record OWNER TO " + APP_ROLE);
      refresh();
      refuseInsert();
      execute("ALTER TABLE source_record OWNER TO postgres");
      execute("GRANT SELECT, INSERT, UPDATE, DELETE ON source_record TO " + APP_ROLE);
      refresh();
      acceptInsert();
      try(Connection application = connect(url, APP_ROLE, APP_PASSWORD); Statement statement = application.createStatement())
      {
         assertThrows(java.sql.SQLException.class, () -> statement.execute("DROP TABLE jobs_job"));
         assertThrows(java.sql.SQLException.class, () -> statement.execute("ALTER TABLE jobs_job ADD COLUMN injected INTEGER"));
         assertThrows(java.sql.SQLException.class, () -> statement.execute("TRUNCATE jobs_event"));
      }
   }



   /*******************************************************************************
    ** PostgreSQL 17 role-membership authority (pg_has_role semantics).  A login that
    ** can INHERIT, SET ROLE to, or ADMIN (regrant) a privileged, owner or server-level
    ** role is refused - including ADMIN TRUE with INHERIT FALSE and SET FALSE, which
    ** carries no privileges today but lets the login grant itself the rest.  Raw
    ** membership with no inherit, no SET and no ADMIN confers nothing and is NOT
    ** authority.  The genuine narrow runtime DML role stays admitted.
    *******************************************************************************/
   @Test
   void roleMembershipAuthorityIsRefusedButRawMembershipIsNot() throws Exception
   {
      List<String> forbidden = List.of("pg_write_all_data", "pg_read_server_files", "pg_write_server_files", "pg_execute_server_program",
         "pg_signal_backend", "pg_maintain", "pg_checkpoint", "pg_create_subscription");
      List<String[]> attributeRoles = List.of(new String[] { "grp_super", "SUPERUSER" }, new String[] { "grp_createrole", "CREATEROLE" },
         new String[] { "grp_createdb", "CREATEDB" }, new String[] { "grp_bypassrls", "BYPASSRLS" }, new String[] { "grp_replication", "REPLICATION" });
      List<String> created = new java.util.ArrayList<>();
      try
      {
         for(String[] role : attributeRoles)
         {
            execute("CREATE ROLE " + role[0] + " NOLOGIN " + role[1]);
            created.add(role[0]);
         }

         List<String> targets = new java.util.ArrayList<>(forbidden);
         attributeRoles.forEach(role -> targets.add(role[0]));
         for(String target : targets)
         {
            for(String options : List.of("ADMIN TRUE, INHERIT FALSE, SET FALSE", "ADMIN FALSE, INHERIT TRUE, SET FALSE", "ADMIN FALSE, INHERIT FALSE, SET TRUE"))
            {
               execute("GRANT " + target + " TO " + APP_ROLE + " WITH " + options);
               refuseInsert();
               execute("REVOKE " + target + " FROM " + APP_ROLE);
            }

            ////////////////////////////////////////////////////////////////////////////////
            // membership with no inherit, no SET and no ADMIN is not authority: admitted //
            ////////////////////////////////////////////////////////////////////////////////
            execute("GRANT " + target + " TO " + APP_ROLE + " WITH ADMIN FALSE, INHERIT FALSE, SET FALSE");
            acceptInsert();
            execute("REVOKE " + target + " FROM " + APP_ROLE);
         }
      }
      finally
      {
         for(String role : created)
         {
            execute("DROP ROLE IF EXISTS " + role);
         }
      }
   }



   /*******************************************************************************
    ** Inherited authority over the OWNER of the table, the schema or the database
    ** refuses admission (direct and through a chain of inheriting memberships); a
    ** chain whose link confers neither inherit nor SET does not.
    *******************************************************************************/
   @Test
   void inheritedOrSettableOwnerAuthorityIsRefused() throws Exception
   {
      List<String> created = List.of("grp_table_owner", "grp_schema_owner", "grp_db_owner", "grp_mid");
      try
      {
         for(String role : created)
         {
            execute("CREATE ROLE " + role + " NOLOGIN");
         }

         ///////////////////////////////////////////////////////////////////////////////////////
         // the table owner: ADMIN-only, INHERIT-only and SET-only are all authority          //
         ///////////////////////////////////////////////////////////////////////////////////////
         execute("ALTER TABLE source_record OWNER TO grp_table_owner");
         execute("GRANT SELECT, INSERT, UPDATE, DELETE ON source_record TO " + APP_ROLE);
         for(String options : List.of("ADMIN TRUE, INHERIT FALSE, SET FALSE", "ADMIN FALSE, INHERIT TRUE, SET FALSE", "ADMIN FALSE, INHERIT FALSE, SET TRUE"))
         {
            execute("GRANT grp_table_owner TO " + APP_ROLE + " WITH " + options);
            refuseInsert();
            execute("REVOKE grp_table_owner FROM " + APP_ROLE);
         }
         execute("GRANT grp_table_owner TO " + APP_ROLE + " WITH ADMIN FALSE, INHERIT FALSE, SET FALSE");
         acceptInsert();
         execute("REVOKE grp_table_owner FROM " + APP_ROLE);

         //////////////////////////////////////////////////////////////////////////////////////////
         // a chain: app -> grp_mid (INHERIT) -> owner (INHERIT) is inherited authority; if the   //
         // second link confers nothing (INHERIT FALSE, SET FALSE) the chain is not authority     //
         //////////////////////////////////////////////////////////////////////////////////////////
         execute("GRANT grp_mid TO " + APP_ROLE + " WITH ADMIN FALSE, INHERIT TRUE, SET FALSE");
         execute("GRANT grp_table_owner TO grp_mid WITH ADMIN FALSE, INHERIT TRUE, SET FALSE");
         refuseInsert();
         execute("REVOKE grp_table_owner FROM grp_mid");
         execute("GRANT grp_table_owner TO grp_mid WITH ADMIN FALSE, INHERIT FALSE, SET FALSE");
         acceptInsert();
         execute("REVOKE grp_table_owner FROM grp_mid");
         execute("REVOKE grp_mid FROM " + APP_ROLE);
         execute("ALTER TABLE source_record OWNER TO postgres");
         execute("GRANT SELECT, INSERT, UPDATE, DELETE ON source_record TO " + APP_ROLE);

         //////////////////////////////////////////////////////////////////////////////////////////
         // the schema owner and the database owner                                               //
         //////////////////////////////////////////////////////////////////////////////////////////
         execute("ALTER SCHEMA public OWNER TO grp_schema_owner");
         execute("GRANT USAGE ON SCHEMA public TO " + APP_ROLE);
         execute("GRANT grp_schema_owner TO " + APP_ROLE + " WITH ADMIN FALSE, INHERIT TRUE, SET FALSE");
         refuseInsert();
         execute("REVOKE grp_schema_owner FROM " + APP_ROLE);
         execute("GRANT grp_schema_owner TO " + APP_ROLE + " WITH ADMIN FALSE, INHERIT FALSE, SET FALSE");
         acceptInsert();
         execute("REVOKE grp_schema_owner FROM " + APP_ROLE);
         execute("ALTER SCHEMA public OWNER TO pg_database_owner");

         execute("ALTER DATABASE " + database + " OWNER TO grp_db_owner");
         execute("GRANT grp_db_owner TO " + APP_ROLE + " WITH ADMIN FALSE, INHERIT FALSE, SET TRUE");
         refuseInsert();
         execute("REVOKE grp_db_owner FROM " + APP_ROLE);
         execute("ALTER DATABASE " + database + " OWNER TO postgres");
         acceptInsert();
      }
      finally
      {
         execute("ALTER DATABASE " + database + " OWNER TO postgres");
         execute("ALTER SCHEMA public OWNER TO pg_database_owner");
         execute("ALTER TABLE source_record OWNER TO postgres");
         for(String role : created)
         {
            execute("DROP OWNED BY " + role);
            execute("DROP ROLE IF EXISTS " + role);
         }
      }
   }



   /*******************************************************************************
    ** The legitimate runtime shapes stay admitted: the DML-only login, a login that
    ** inherits a narrow DML group role, and read-only monitoring membership
    ** (pg_read_all_stats, pg_monitor), none of which carry write-all, owner or
    ** server-level power.
    *******************************************************************************/
   @Test
   void narrowRuntimeAndReadOnlyMonitoringMembershipStayAdmitted() throws Exception
   {
      try
      {
         execute("CREATE ROLE grp_dml NOLOGIN");
         execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO grp_dml");
         execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO grp_dml");
         acceptInsert();
         execute("GRANT grp_dml TO " + APP_ROLE + " WITH ADMIN FALSE, INHERIT TRUE, SET TRUE");
         acceptInsert();
         for(String monitoring : List.of("pg_read_all_stats", "pg_read_all_settings", "pg_monitor", "pg_stat_scan_tables"))
         {
            execute("GRANT " + monitoring + " TO " + APP_ROLE + " WITH ADMIN FALSE, INHERIT TRUE, SET FALSE");
            acceptInsert();
            execute("REVOKE " + monitoring + " FROM " + APP_ROLE);
         }
         execute("REVOKE grp_dml FROM " + APP_ROLE);
      }
      finally
      {
         execute("REVOKE ALL ON ALL TABLES IN SCHEMA public FROM grp_dml");
         execute("REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM grp_dml");
         execute("DROP ROLE IF EXISTS grp_dml");
      }
   }



   /*******************************************************************************
    ** The table classification itself: views, foreign, partitioned, partition,
    ** unlogged and application-owned tables are refused, while an ordinary logged
    ** table owned by another role is qualified, on a real application connection.
    *******************************************************************************/
   @Test
   void tableClassificationRefusesEverythingButOrdinaryLoggedTables() throws Exception
   {
      execute("CREATE EXTENSION IF NOT EXISTS file_fdw");
      execute("CREATE SERVER IF NOT EXISTS jobs_files FOREIGN DATA WRAPPER file_fdw");
      execute("CREATE TABLE ordinary_t (id INTEGER)");
      execute("CREATE VIEW view_t AS SELECT * FROM ordinary_t");
      execute("CREATE FOREIGN TABLE foreign_t (line TEXT) SERVER jobs_files OPTIONS (filename '/etc/hostname', format 'text')");
      execute("CREATE TABLE partitioned_t (id INTEGER) PARTITION BY RANGE (id)");
      execute("CREATE TABLE partition_t PARTITION OF partitioned_t FOR VALUES FROM (0) TO (10)");
      execute("CREATE UNLOGGED TABLE unlogged_t (id INTEGER)");
      execute("CREATE TABLE app_owned_t (id INTEGER)");
      execute("ALTER TABLE app_owned_t OWNER TO " + APP_ROLE);
      execute("GRANT TEMPORARY ON DATABASE " + database + " TO " + APP_ROLE);
      try(Connection application = connect(url, APP_ROLE, APP_PASSWORD))
      {
         application.setAutoCommit(false);
         RDBMSAtomicWrites.requirePostgresTable(application, "ordinary_t");
         for(String name : List.of("view_t", "foreign_t", "partitioned_t", "partition_t", "unlogged_t", "app_owned_t", "missing_t"))
         {
            assertThrows(QException.class, () -> RDBMSAtomicWrites.requirePostgresTable(application, name), name);
         }
         try(Statement statement = application.createStatement())
         {
            statement.execute("CREATE TEMP TABLE ordinary_t (id INTEGER)");
         }
         assertThrows(QException.class, () -> RDBMSAtomicWrites.requirePostgresTable(application, "ordinary_t"), "temporary shadow");
      }
   }



   /*******************************************************************************
    ** The guard table is qualified like the owner tables and its single row is
    ** required: a missing row, an unlogged guard and a missing guard all refuse.
    *******************************************************************************/
   @Test
   void guardTableDriftRefusesAdmission() throws Exception
   {
      execute("DELETE FROM jobs_admission");
      refuseInsert();
      execute("INSERT INTO jobs_admission(id) VALUES(1)");
      execute("ALTER TABLE jobs_admission SET UNLOGGED");
      refuseInsert();
      execute("ALTER TABLE jobs_admission SET LOGGED");
      execute("ALTER TABLE jobs_admission RENAME TO jobs_admission_moved");
      refuseInsert();
      execute("ALTER TABLE jobs_admission_moved RENAME TO jobs_admission");
      acceptInsert();
   }



   /*******************************************************************************
    ** A held guard row bounds admission at the role lock timeout and rolls the
    ** source record back with the refused intent.
    *******************************************************************************/
   @Test
   void heldGuardRowBoundsAdmissionAndRollsBack() throws Exception
   {
      try(Connection holder = openConnection(); Statement statement = holder.createStatement())
      {
         holder.setAutoCommit(false);
         statement.execute("SELECT id FROM jobs_admission WHERE id=1 FOR UPDATE");
         long started = System.nanoTime();
         assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "blocked"))));
         long millis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
         assertTrue(millis >= 3000 && millis < 30000, "bounded wait was " + millis);
         holder.rollback();
      }
      assertEquals(0, count("source_record"));
      assertEquals(0, count("jobs_job"));
      acceptInsert();
   }



   /*******************************************************************************
    ** Weakened unique identity is refused: a partial index cannot stand in for the
    ** complete enlistment-key uniqueness, and the complete key restores admission.
    *******************************************************************************/
   @Test
   void partialUniqueIdentityRefusesAdmission() throws Exception
   {
      String constraint = observe("SELECT conname FROM pg_constraint WHERE conrelid='jobs_job'::regclass AND contype='u'");
      assertNotNull(constraint);
      execute("ALTER TABLE jobs_job DROP CONSTRAINT " + constraint);
      refuseInsert();
      execute("CREATE UNIQUE INDEX jobs_job_enlistment_partial ON jobs_job(enlistment_key) WHERE enlistment_key IS NOT NULL");
      refuseInsert();
      execute("DROP INDEX jobs_job_enlistment_partial");
      execute("ALTER TABLE jobs_job ADD CONSTRAINT jobs_job_enlistment_unique UNIQUE(enlistment_key)");
      acceptInsert();
   }



   /*******************************************************************************
    ** An owner table that is unlogged is refused, and a logged one is accepted.
    *******************************************************************************/
   @Test
   void unloggedOwnerOrSourceTableRefusesAdmission() throws Exception
   {
      for(String table : List.of("source_record", "jobs_job", "jobs_chunk", "jobs_event"))
      {
         execute("ALTER TABLE " + table + " SET UNLOGGED");
         refuseInsert();
         execute("ALTER TABLE " + table + " SET LOGGED");
      }
      acceptInsert();
   }



   /*******************************************************************************
    ** Unsupported fail-closed guards still refuse: a non-job source table of a
    ** different native backend identity (wrong database for the connection).
    *******************************************************************************/
   @Test
   void wrongDatabaseIdentityRefusesAtomicWrites() throws Exception
   {
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) instance.getBackend(newBackend().getName());
      String original = backend.getDatabaseName();
      backend.setDatabaseName("bootstrap");
      refuseInsert();
      backend.setDatabaseName(original);
      acceptInsert();
   }



   /*******************************************************************************
    ** Open a fixture connection. This suite opens many short observer connections;
    ** on Windows a burst can exhaust the client socket buffers ("No buffer space
    ** available", a client-side connect failure unrelated to the database), so only
    ** that specific condition is retried, a few times, with a short pause.
    *******************************************************************************/
   private static Connection connect(String jdbcUrl, String user, String password) throws java.sql.SQLException
   {
      java.sql.SQLException last = null;
      for(int attempt = 0; attempt < 6; attempt++)
      {
         try
         {
            return (DriverManager.getConnection(jdbcUrl, user, password));
         }
         catch(java.sql.SQLException failure)
         {
            boolean exhausted = false;
            for(Throwable cause = failure; cause != null; cause = cause.getCause())
            {
               exhausted |= cause.getMessage() != null && cause.getMessage().contains("No buffer space available");
            }
            if(!exhausted)
            {
               throw (failure);
            }
            last = failure;
            try
            {
               Thread.sleep(500L * (attempt + 1));
            }
            catch(InterruptedException interrupted)
            {
               Thread.currentThread().interrupt();
               throw (failure);
            }
         }
      }
      throw (last);
   }



   /*******************************************************************************
    ** Drop all pooled sessions so a changed role setting or JVM zone is observed.
    *******************************************************************************/
   private void refresh()
   {
      ConnectionManager.closeConnectionProviders();
   }



   /*******************************************************************************
    ** The required hook refuses and leaves neither the source row nor any intent.
    *******************************************************************************/
   private void refuseInsert() throws Exception
   {
      refresh();
      int sources = count("source_record");
      int jobs = count("jobs_job");
      Exception refusal = assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "refuse"))));
      assertTrue(isGuardRefusal(refusal), "not a qualification refusal: " + refusal);
      assertEquals(sources, count("source_record"));
      assertEquals(jobs, count("jobs_job"));
   }



   /*******************************************************************************
    ** True only when the failure (or a cause) is one of the qualification guards'
    ** own refusals, so an unrelated database or fixture error never passes a
    ** refusal assertion.
    *******************************************************************************/
   private static boolean isGuardRefusal(Throwable failure)
   {
      for(int depth = 0; failure != null && depth < 8; depth++, failure = failure.getCause())
      {
         String message = failure.getMessage();
         if(message != null && (message.contains("Required record") || message.contains("Durable Jobs requires") || message.contains("Required event")))
         {
            return (true);
         }
      }
      return (false);
   }



   /*******************************************************************************
    ** The required hook is accepted: one more source row and exactly one more job.
    *******************************************************************************/
   private void acceptInsert() throws Exception
   {
      refresh();
      int sources = count("source_record");
      int jobs = count("jobs_job");
      new InsertAction().execute(new InsertInput("source_record").withRecord(new QRecord().withValue("value", "accept")));
      assertEquals(sources + 1, count("source_record"));
      assertEquals(jobs + 1, count("jobs_job"));
   }



   /*******************************************************************************
    ** Insert a source row without any hook, through an independent connection.
    *******************************************************************************/
   private Integer insertDirect(String value) throws Exception
   {
      try(Connection connection = openConnection(); Statement statement = connection.createStatement();
         ResultSet rows = statement.executeQuery("INSERT INTO source_record(value) VALUES('" + value + "') RETURNING id"))
      {
         rows.next();
         return (rows.getInt(1));
      }
   }



   /*******************************************************************************
    ** Execute fixed trusted fixture SQL on the superuser connection.
    *******************************************************************************/
   private void execute(String sql) throws Exception
   {
      try(Connection connection = openConnection(); Statement statement = connection.createStatement())
      {
         statement.execute(sql);
      }
   }



   /*******************************************************************************
    ** Read one committed value through an independent connection.
    *******************************************************************************/
   private String observe(String sql) throws Exception
   {
      try(Connection connection = openConnection(); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql))
      {
         return (rows.next() ? rows.getString(1) : null);
      }
   }}
