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


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaData;
import com.kingsrook.qqq.backend.core.jobs.DurableJobsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;


/*******************************************************************************
 ** Strict Jobs participation on the actual source connection. Qualification is
 ** repeated before acquiring the shared admission row; no runtime DDL, separate
 ** connection or pooled-session mutation is performed. Trusted administrators
 ** own schema/storage configuration; probes do not contain a hostile DBA.
 *******************************************************************************/
final class RDBMSJobAdmission
{
   private static final String GUARD = "jobs_admission";
   private static final int TIMEOUT_SECONDS = 5;



   /*******************************************************************************
    ** Static backend implementation only.
    *******************************************************************************/
   private RDBMSJobAdmission() {}



   /*******************************************************************************
    ** Qualify settings, expected owner mappings, physical keys and lock admission.
    *******************************************************************************/
   static String lock(Connection connection, QTableMetaData source) throws QException
   {
      try
      {
         if(!DurableJobsMetaData.of(QContext.getQInstance()).isTransactionalAdmissionEnabled())
         {
            throw refused();
         }
         String backend = RDBMSAtomicWrites.require(connection, source);
         if(connection.getTransactionIsolation() != Connection.TRANSACTION_READ_COMMITTED)
         {
            throw refused();
         }
         boolean h2 = "H2".equals(connection.getMetaData().getDatabaseProductName());
         boolean postgres = "PostgreSQL".equals(connection.getMetaData().getDatabaseProductName());
         requireSettings(connection, h2, postgres);
         List<QTableMetaData> expected = expectedTables(backend);
         for(QTableMetaData table : expected)
         {
            QTableMetaData live = QContext.getQInstance().getTable(table.getName());
            requireMapping(table, live);
            RDBMSAtomicWrites.require(connection, live);
         }
         requireGuard(connection, h2, postgres);
         for(QTableMetaData table : expected)
         {
            requireColumns(connection, table);
         }
         requireKeys(connection, expected, h2, postgres);
         try(PreparedStatement statement = prepare(connection, "SELECT id FROM jobs_admission WHERE id=1 FOR UPDATE");
             ResultSet rows = statement.executeQuery())
         {
            if(!rows.next() || rows.getInt(1) != 1 || rows.next())
            {
               throw refused();
            }
         }
         requireExistingMembership(connection, postgres);
         String identity = backend + "\n" + connection.getMetaData().getURL().split("[;?]", 2)[0] + "\n" + connection.getCatalog() + "\n" + connection.getSchema();
         return (HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(identity.getBytes(StandardCharsets.UTF_8))));
      }
      catch(QException error)
      {
         throw (error);
      }
      catch(SQLException | java.security.NoSuchAlgorithmException error)
      {
         throw (new QException("Could not qualify durable Jobs transaction", error));
      }
   }



   /*******************************************************************************
    ** Use authoritative Jobs definitions, not mutable host metadata as the schema.
    *******************************************************************************/
   private static List<QTableMetaData> expectedTables(String backend)
   {
      DurableJobsMetaDataProvider provider = new DurableJobsMetaDataProvider();
      List<QTableMetaData> tables = List.of(provider.defineJobTable(backend), provider.defineChunkTable(backend),
         provider.defineEventTable(backend), provider.defineEffectTable(backend));
      tables.forEach(DurableJobsMetaDataProvider.snakeCaseColumnNames());
      return (tables);
   }



   /*******************************************************************************
    ** Refuse remapped owner columns, weakened primary/unique keys or foreign tables.
    *******************************************************************************/
   private static void requireMapping(QTableMetaData expected, QTableMetaData live) throws QException
   {
      if(live == null || !Objects.equals(expected.getBackendName(), live.getBackendName())
         || !expected.getName().equals(RDBMSAtomicWrites.physicalTable(live)) || !Objects.equals(expected.getPrimaryKeyField(), live.getPrimaryKeyField()))
      {
         throw refused();
      }
      for(QFieldMetaData field : expected.getFields().values())
      {
         QFieldMetaData actual = live.getFields().get(field.getName());
         if(actual == null || actual.getType() != field.getType() || !Objects.equals(actual.getBackendName(), field.getBackendName())
            || !Objects.equals(actual.getIsRequired(), field.getIsRequired()) || !Objects.equals(actual.getMaxLength(), field.getMaxLength()))
         {
            throw refused();
         }
      }
      if(expected.getUniqueKeys() != null)
      {
         for(UniqueKey key : expected.getUniqueKeys())
         {
            if(live.getUniqueKeys() == null || live.getUniqueKeys().stream().noneMatch(actual -> key.getFieldNames().equals(actual.getFieldNames())))
            {
               throw refused();
            }
         }
      }
   }



   /*******************************************************************************
    ** Require synchronous durable storage and bounded locking without changing it.
    *******************************************************************************/
   private static void requireSettings(Connection connection, boolean h2, boolean postgres) throws SQLException, QException
   {
      if(postgres)
      {
         try(PreparedStatement statement = prepare(connection, "SELECT current_setting('server_version_num')::int, current_setting('transaction_read_only'), pg_is_in_recovery(),"
            + " current_setting('synchronous_commit'), current_setting('fsync'), current_setting('full_page_writes'), current_setting('TimeZone'),"
            + " (SELECT setting::bigint FROM pg_settings WHERE name='lock_timeout'), current_setting('transaction_isolation')");
             ResultSet rows = statement.executeQuery())
         {
            if(!rows.next() || rows.getInt(1) / 10000 != 17 || !"off".equals(rows.getString(2)) || rows.getBoolean(3)
               || !Set.of("on", "remote_apply").contains(rows.getString(4)) || !"on".equals(rows.getString(5)) || !"on".equals(rows.getString(6))
               || !Set.of("UTC", "Etc/UTC").contains(rows.getString(7)) || rows.getLong(8) < 1 || rows.getLong(8) > 5000
               || !"read committed".equals(rows.getString(9)))
            {
               throw refused();
            }
         }
      }
      else if(h2)
      {
         String url = connection.getMetaData().getURL();
         if(url == null || !url.startsWith("jdbc:h2:file:"))
         {
            throw refused();
         }
         try(PreparedStatement statement = prepare(connection, "SELECT SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE SETTING_NAME='WRITE_DELAY'");
             ResultSet rows = statement.executeQuery())
         {
            boolean present = false;
            while(rows.next())
            {
               present = true;
               if(!"0".equals(rows.getString(1)))
               {
                  throw refused();
               }
            }
            if(!present)
            {
               throw refused();
            }
         }
         try(PreparedStatement statement = prepare(connection, "SELECT SETTING_NAME, SETTING_VALUE FROM INFORMATION_SCHEMA.SETTINGS WHERE SETTING_NAME IN ('COLLATION','IGNORECASE')");
             ResultSet rows = statement.executeQuery())
         {
            while(rows.next())
            {
               if("COLLATION".equals(rows.getString(1)) ? !"OFF".equals(rows.getString(2)) : !"false".equalsIgnoreCase(rows.getString(2)))
               {
                  throw refused();
               }
            }
         }
         try(PreparedStatement statement = prepare(connection, "CALL LOCK_TIMEOUT()"); ResultSet rows = statement.executeQuery())
         {
            if(!rows.next() || rows.getInt(1) < 1 || rows.getInt(1) > 5000)
            {
               throw refused();
            }
         }
      }
      else
      {
         try(PreparedStatement statement = prepare(connection, "SELECT @@GLOBAL.innodb_flush_log_at_trx_commit, @@GLOBAL.log_bin, @@GLOBAL.sync_binlog, @@GLOBAL.innodb_doublewrite, @@GLOBAL.read_only, @@SESSION.transaction_read_only, @@SESSION.transaction_isolation, @@SESSION.time_zone, @@SESSION.innodb_lock_wait_timeout, @@SESSION.lock_wait_timeout");
             ResultSet rows = statement.executeQuery())
         {
            if(!rows.next() || rows.getInt(1) != 1 || (rows.getBoolean(2) && rows.getInt(3) != 1)
               || !Set.of("ON", "DETECT_AND_RECOVER").contains(rows.getString(4)) || rows.getBoolean(5) || rows.getBoolean(6)
               || !"READ-COMMITTED".equals(rows.getString(7)) || !Set.of("+00:00", "UTC").contains(rows.getString(8))
               || rows.getInt(9) < 1 || rows.getInt(9) > 5 || rows.getInt(10) < 1 || rows.getInt(10) > 5)
            {
               throw refused();
            }
         }
      }
   }



   /*******************************************************************************
    ** The private guard must be a native durable table, never a linked/view target.
    *******************************************************************************/
   private static void requireGuard(Connection connection, boolean h2, boolean postgres) throws SQLException, QException
   {
      if(postgres)
      {
         RDBMSAtomicWrites.requirePostgresTable(connection, GUARD);
         return;
      }
      String sql = h2 ? "SELECT TABLE_TYPE, STORAGE_TYPE FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?"
         : "SELECT TABLE_TYPE, ENGINE FROM information_schema.TABLES WHERE TABLE_SCHEMA=? AND TABLE_NAME=?";
      try(PreparedStatement statement = prepare(connection, sql))
      {
         statement.setString(1, h2 ? connection.getSchema() : connection.getCatalog());
         statement.setString(2, GUARD);
         try(ResultSet rows = statement.executeQuery())
         {
            if(!rows.next() || !"BASE TABLE".equals(rows.getString(1))
               || !(h2 ? Set.of("CACHED", "MEMORY").contains(rows.getString(2)) : "InnoDB".equals(rows.getString(2))) || rows.next())
            {
               throw refused();
            }
         }
      }
   }



   /*******************************************************************************
    ** Check native types/lengths without reading or exposing any stored values.
    *******************************************************************************/
   private static void requireColumns(Connection connection, QTableMetaData table) throws SQLException, QException
   {
      try(PreparedStatement statement = prepare(connection, "SELECT * FROM " + table.getName() + " WHERE 1=0");
          ResultSet rows = statement.executeQuery())
      {
         ResultSetMetaData metadata = rows.getMetaData();
         Map<String, Integer> columns = new HashMap<>();
         for(int index = 1; index <= metadata.getColumnCount(); index++)
         {
            columns.put(metadata.getColumnName(index), index);
         }
         for(QFieldMetaData field : table.getFields().values())
         {
            Integer index = columns.get(field.getBackendName());
            if(index == null || "VARCHAR_IGNORECASE".equals(metadata.getColumnTypeName(index)) || !compatible(field, metadata.getColumnType(index))
               || (field.getMaxLength() != null && metadata.getPrecision(index) < field.getMaxLength())
               || (field.getName().equals(table.getPrimaryKeyField()) && (!metadata.isAutoIncrement(index) || metadata.isNullable(index) != ResultSetMetaData.columnNoNulls))
               || (Boolean.TRUE.equals(field.getIsRequired()) && metadata.isNullable(index) != ResultSetMetaData.columnNoNulls))
            {
               throw refused();
            }
         }
      }
   }



   /*******************************************************************************
    ** Match the native storage types used by the Jobs owner migration.
    *******************************************************************************/
   private static boolean compatible(QFieldMetaData field, int type)
   {
      return (switch(field.getType())
      {
         case INTEGER -> type == Types.INTEGER;
         case LONG -> type == Types.BIGINT;
         case BOOLEAN -> type == Types.BOOLEAN || type == Types.BIT || type == Types.TINYINT;
         case STRING -> type == Types.VARCHAR;
         case TEXT -> type == Types.CLOB || type == Types.LONGVARCHAR || type == Types.VARCHAR;
         case DATE_TIME -> type == Types.TIMESTAMP;
         default -> false;
      });
   }



   /*******************************************************************************
    ** Query all owned primary/unique constraints together; prefix indexes cannot
    ** stand in for full canonical identity uniqueness.
    *******************************************************************************/
   private static void requireKeys(Connection connection, List<QTableMetaData> tables, boolean h2, boolean postgres) throws SQLException, QException
   {
      List<String> names = new ArrayList<>(tables.stream().map(QTableMetaData::getName).toList());
      names.add(GUARD);
      String placeholders = String.join(",", java.util.Collections.nCopies(names.size(), "?"));
      String sql = postgres
         ? "SELECT t.relname, i.relname, a.attname, k.ord, ix.indisprimary FROM pg_index ix JOIN pg_class i ON i.oid=ix.indexrelid JOIN pg_class t ON t.oid=ix.indrelid"
            + " JOIN pg_namespace n ON n.oid=t.relnamespace CROSS JOIN LATERAL unnest(ix.indkey::int2[]) WITH ORDINALITY AS k(attnum, ord)"
            + " JOIN pg_attribute a ON a.attrelid=t.oid AND a.attnum=k.attnum WHERE n.nspname=current_schema() AND ix.indisunique AND ix.indisvalid AND ix.indpred IS NULL"
            + " AND ix.indexprs IS NULL AND k.ord<=ix.indnkeyatts AND t.relname IN (" + placeholders + ")"
         : h2
         ? "SELECT tc.TABLE_NAME, tc.CONSTRAINT_NAME, kc.COLUMN_NAME, kc.ORDINAL_POSITION, tc.CONSTRAINT_TYPE FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kc ON tc.CONSTRAINT_CATALOG=kc.CONSTRAINT_CATALOG AND tc.CONSTRAINT_SCHEMA=kc.CONSTRAINT_SCHEMA AND tc.CONSTRAINT_NAME=kc.CONSTRAINT_NAME WHERE tc.TABLE_SCHEMA=? AND tc.CONSTRAINT_TYPE IN ('PRIMARY KEY','UNIQUE') AND tc.TABLE_NAME IN (" + placeholders + ")"
         : "SELECT TABLE_NAME, INDEX_NAME, COLUMN_NAME, SEQ_IN_INDEX, SUB_PART FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=? AND NON_UNIQUE=0 AND TABLE_NAME IN (" + placeholders + ")";
      Map<String, Map<String, TreeMap<Integer, String>>> indexes = new HashMap<>();
      Map<String, String> primary = new HashMap<>();
      try(PreparedStatement statement = prepare(connection, sql))
      {
         if(!postgres)
         {
            statement.setString(1, h2 ? connection.getSchema() : connection.getCatalog());
         }
         for(int index = 0; index < names.size(); index++)
         {
            statement.setString(index + (postgres ? 1 : 2), names.get(index));
         }
         try(ResultSet rows = statement.executeQuery())
         {
            while(rows.next())
            {
               if(!h2 && !postgres && rows.getObject(5) != null)
               {
                  throw refused();
               }
               String table = rows.getString(1);
               String name = rows.getString(2);
               indexes.computeIfAbsent(table, ignored -> new HashMap<>()).computeIfAbsent(name, ignored -> new TreeMap<>()).put(rows.getInt(4), rows.getString(3));
               if(postgres ? rows.getBoolean(5) : h2 ? "PRIMARY KEY".equals(rows.getString(5)) : "PRIMARY".equals(name))
               {
                  primary.put(table, name);
               }
            }
         }
      }
      for(String name : names)
      {
         Map<String, TreeMap<Integer, String>> keys = indexes.getOrDefault(name, Map.of());
         String primaryName = primary.get(name);
         if(primaryName == null || !List.of("id").equals(new ArrayList<>(keys.getOrDefault(primaryName, new TreeMap<>()).values())))
         {
            throw refused();
         }
      }
      for(QTableMetaData table : tables)
      {
         if(table.getUniqueKeys() != null)
         {
            for(UniqueKey key : table.getUniqueKeys())
            {
               List<String> columns = key.getFieldNames().stream().map(name -> table.getField(name).getBackendName()).toList();
               if(indexes.get(table.getName()).values().stream().noneMatch(index -> columns.equals(new ArrayList<>(index.values()))))
               {
                  throw refused();
               }
            }
         }
      }
      if(postgres)
      {
         requireDeterministicColumns(connection, names);
      }
      else if(!h2)
      {
         requireBinaryColumns(connection, names);
      }
   }



   /*******************************************************************************
    ** PostgreSQL text equality is case-sensitive under the default collation; only a
    ** non-deterministic (for example case-insensitive) collation could weaken the
    ** actor, key and status identities, so any such column is refused.
    *******************************************************************************/
   private static void requireDeterministicColumns(Connection connection, List<String> names) throws SQLException, QException
   {
      String sql = "SELECT a.attname FROM pg_attribute a JOIN pg_class c ON c.oid=a.attrelid JOIN pg_namespace n ON n.oid=c.relnamespace JOIN pg_collation co ON co.oid=a.attcollation"
         + " WHERE n.nspname=current_schema() AND a.attnum>0 AND NOT a.attisdropped AND NOT co.collisdeterministic AND c.relname IN ("
         + String.join(",", java.util.Collections.nCopies(names.size(), "?")) + ")";
      try(PreparedStatement statement = prepare(connection, sql))
      {
         for(int index = 0; index < names.size(); index++)
         {
            statement.setString(index + 1, names.get(index));
         }
         try(ResultSet rows = statement.executeQuery())
         {
            if(rows.next())
            {
               throw refused();
            }
         }
      }
   }



   /*******************************************************************************
    ** Preserve case-sensitive actor, key and status equality in MySQL capacity SQL.
    *******************************************************************************/
   private static void requireBinaryColumns(Connection connection, List<String> names) throws SQLException, QException
   {
      String sql = "SELECT COLLATION_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=? AND TABLE_NAME IN ("
         + String.join(",", java.util.Collections.nCopies(names.size(), "?")) + ") AND COLLATION_NAME IS NOT NULL";
      try(PreparedStatement statement = prepare(connection, sql))
      {
         statement.setString(1, connection.getCatalog());
         for(int index = 0; index < names.size(); index++)
         {
            statement.setString(index + 2, names.get(index));
         }
         try(ResultSet rows = statement.executeQuery())
         {
            while(rows.next())
            {
               if(!"utf8mb4_0900_bin".equals(rows.getString(1)))
               {
                  throw refused();
               }
            }
         }
      }
   }



   /*******************************************************************************
    ** Existing rows must be migrated before all writers enable strict admission.
    ** A bounded database predicate avoids returning hidden row contents or counts.
    *******************************************************************************/
   private static void requireExistingMembership(Connection connection, boolean postgres) throws SQLException, QException
   {
      String active = "('AWAITING_CONFIRMATION','QUEUED','RUNNING','PLANNING','PAUSING','PAUSED','CANCELLING')";
      String channelMismatch = postgres ? "client_channel !~ '^[a-z][a-z0-9_-]*([/:].*)?$'" : "NOT REGEXP_LIKE(client_channel, '^[a-z][a-z0-9_-]*([/:].*)?$')";
      for(String sql : List.of(
         "SELECT COUNT(*) FROM jobs_job WHERE status IN " + active + " AND client_channel IS NOT NULL AND " + channelMismatch,
         "SELECT COUNT(*) FROM jobs_job j WHERE j.status NOT IN ('RUNNING','PLANNING','PAUSING','CANCELLING') AND EXISTS (SELECT 1 FROM jobs_chunk c WHERE c.job_id=j.id AND c.status='RUNNING')"))
      {
         try(PreparedStatement statement = prepare(connection, sql); ResultSet rows = statement.executeQuery())
         {
            if(!rows.next() || rows.getLong(1) != 0)
            {
               throw (new QException("Existing Jobs channel/execution state must be reconciled before strict admission is enabled"));
            }
         }
      }
   }



   /*******************************************************************************
    ** Acquire row locks in the chunk-before-job order used by progress updates.
    *******************************************************************************/
   static void lockControlRows(Connection connection, Integer jobId) throws QException
   {
      try
      {
         for(String sql : List.of("SELECT id FROM jobs_chunk WHERE job_id=? ORDER BY id FOR UPDATE", "SELECT id FROM jobs_job WHERE id=? FOR UPDATE"))
         {
            try(PreparedStatement statement = prepare(connection, sql))
            {
               statement.setInt(1, jobId);
               try(ResultSet rows = statement.executeQuery())
               {
                  int count = 0;
                  while(rows.next())
                  {
                     if(++count > 100000)
                     {
                        throw (new QException("Job control exceeds the qualified chunk bound"));
                     }
                  }
               }
            }
         }
      }
      catch(SQLException error)
      {
         throw (new QException("Could not lock Jobs control rows", error));
      }
   }



   /*******************************************************************************
    ** Read time from this connection rather than a pool checkout.
    *******************************************************************************/
   static Instant now(Connection connection) throws QException
   {
      try(PreparedStatement statement = prepare(connection, "SELECT CURRENT_TIMESTAMP"); ResultSet rows = statement.executeQuery())
      {
         if(!rows.next())
         {
            throw refused();
         }
         return (rows.getTimestamp(1, java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))).toInstant());
      }
      catch(SQLException error)
      {
         throw (new QException("Could not read Jobs transaction time", error));
      }
   }



   /*******************************************************************************
    ** Every probe and lock has a bounded statement deadline.
    *******************************************************************************/
   private static PreparedStatement prepare(Connection connection, String sql) throws SQLException
   {
      PreparedStatement statement = connection.prepareStatement(sql);
      try
      {
         statement.setQueryTimeout(TIMEOUT_SECONDS);
         return (statement);
      }
      catch(SQLException | RuntimeException error)
      {
         try
         {
            statement.close();
         }
         catch(SQLException | RuntimeException closeError)
         {
            if(closeError != error)
            {
               error.addSuppressed(closeError);
            }
         }
         throw (error);
      }
   }



   /*******************************************************************************
    ** Configuration refusal contains no JDBC URL, credentials or row contents.
    *******************************************************************************/
   private static QException refused()
   {
      return (new QException("Durable Jobs requires its qualified native schema, synchronous file H2, MySQL8.4 or PostgreSQL17 storage, READ COMMITTED and bounded lock waits"));
   }
}
