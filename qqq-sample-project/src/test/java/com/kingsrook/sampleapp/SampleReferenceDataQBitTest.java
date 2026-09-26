/*
 * Copyright 2026 QRun-IO.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.kingsrook.sampleapp;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qbits.reference.ReferenceDataQBit;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.DirectoryResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.InputSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** A first-party generated data QBit exercised through the sample host. */
class SampleReferenceDataQBitTest
{
   private ReferenceDataQBit qbit;

   @TempDir
   private Path directory;

   /** Start each scenario with the real host and an empty QQQ memory backend. */
   @BeforeEach
   void setUp() throws Exception
   {
      MemoryRecordStore.getInstance().reset();
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      qbit = new ReferenceDataQBit("sample", SampleMetaDataProvider.MEMORY_BACKEND_NAME);
   }

   /** Clear shared QQQ state after the scenario. */
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      MemoryRecordStore.getInstance().reset();
      ConnectionManager.resetConnectionProviders();
   }

   /** The host registers the QBit, table, PVS, and an isolated second prefix. */
   @Test
   void testHostMetadataAndPrefixIsolation() throws Exception
   {
      QInstance instance = QContext.getQInstance();
      assertNotNull(instance.getTable(qbit.tableName()));
      assertNotNull(instance.getPossibleValueSource(qbit.tableName()));
      assertNotNull(instance.getQBits().get("com.kingsrook.qbits:qqq-sample-data-qbit:sample"));
      ReferenceDataQBit other = new ReferenceDataQBit("other", SampleMetaDataProvider.MEMORY_BACKEND_NAME);
      other.produce(instance);
      assertNotNull(instance.getTable(other.tableName()));
      qbit.sync(List.of(new ReferenceDataQBit.Source("A", "Sample")));
      other.sync(List.of(new ReferenceDataQBit.Source("A", "Other")));
      assertEquals("Sample", qbit.records().get(0).getValueString("name"));
      assertEquals("Other", other.records().get(0).getValueString("name"));
      assertEquals("Sample", new SearchPossibleValueSourceAction().execute(
         new SearchPossibleValueSourceInput().withPossibleValueSourceName(qbit.tableName()))
         .getResults().get(0).getLabel());
   }

   /** Sync preserves IDs, deactivates missing rows, and skips unchanged rows. */
   @Test
   void testNaturalKeySyncLifecycle() throws Exception
   {
      assertEquals(new ReferenceDataQBit.SyncResult(2, 0, 0, 0), qbit.sync(List.of(
         new ReferenceDataQBit.Source("A", "Alpha"), new ReferenceDataQBit.Source("B", "Beta"))));
      Object id = qbit.records().get(0).getValue("id");
      assertEquals(new ReferenceDataQBit.SyncResult(1, 1, 0, 1), qbit.sync(List.of(
         new ReferenceDataQBit.Source("A", "Alpha updated"), new ReferenceDataQBit.Source("C", "Gamma"))));
      assertEquals(id, qbit.records().stream().filter(r -> "A".equals(r.getValueString("code"))).findFirst().orElseThrow().getValue("id"));
      assertEquals(false, qbit.records().stream().filter(r -> "B".equals(r.getValueString("code"))).findFirst().orElseThrow().getValueBoolean("isActive"));
      assertEquals(new ReferenceDataQBit.SyncResult(0, 0, 2, 0), qbit.sync(List.of(
         new ReferenceDataQBit.Source("A", "Alpha updated"), new ReferenceDataQBit.Source("C", "Gamma"))));
      assertEquals(3, qbit.records().size());
      assertEquals(new ReferenceDataQBit.SyncResult(0, 1, 2, 0), qbit.sync(List.of(
         new ReferenceDataQBit.Source("A", "Alpha updated"), new ReferenceDataQBit.Source("B", "Beta"),
         new ReferenceDataQBit.Source("C", "Gamma"))));
      assertEquals(true, qbit.records().stream().filter(r -> "B".equals(r.getValueString("code"))).findFirst().orElseThrow().getValueBoolean("isActive"));
   }

   /** Invalid source keys are rejected before the first write. */
   @Test
   void testBadSourceRejectedBeforeWrites() throws Exception
   {
      qbit.sync(List.of(new ReferenceDataQBit.Source("A", "Alpha")));
      assertThrows(IllegalArgumentException.class, () -> qbit.sync(List.of(
         new ReferenceDataQBit.Source("B", "Beta"), new ReferenceDataQBit.Source("B", "Duplicate"))));
      assertThrows(IllegalArgumentException.class, () -> qbit.sync(List.of(new ReferenceDataQBit.Source(" ", "Bad"))));
      assertEquals(1, qbit.records().size());
      assertThrows(IllegalArgumentException.class, () -> new ReferenceDataQBit("bad-prefix!", "memory"));
   }

   /** The generated XML names the host table and failed input leaves it empty. */
   @Test
   void testLiquibaseGenerationAndFailedSyncBoundary() throws Exception
   {
      var parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
      var xml = parser.parse(new InputSource(new StringReader(qbit.liquibaseChangelog())));
      assertEquals(qbit.tableName(), xml.getElementsByTagName("createTable").item(0).getAttributes()
         .getNamedItem("tableName").getNodeValue());
      assertEquals(4, xml.getElementsByTagName("column").getLength());
      assertThrows(IllegalArgumentException.class, () -> qbit.sync(List.of(
         new ReferenceDataQBit.Source("A", "Alpha"), new ReferenceDataQBit.Source("B", " "))));
      assertThrows(IllegalArgumentException.class, () -> qbit.sync(List.of(
         new ReferenceDataQBit.Source("A", "Alpha"), new ReferenceDataQBit.Source("B", "x".repeat(101)))));
      assertEquals(0, qbit.records().size());
   }

   /** The host process reads the shipped JSON fixture and persists its records. */
   @Test
   void testBundledSyncProcess() throws Exception
   {
      RunProcessInput input = new RunProcessInput();
      input.setProcessName("sample_syncReferenceCategories");
      new RunProcessAction().execute(input);
      assertEquals(2, qbit.records().size());
      new RunProcessAction().execute(input);
      assertEquals(2, qbit.records().size());
   }

   /** A real Liquibase migration creates an H2 table that QQQ actions can sync. */
   @Test
   void testLiquibaseMigrationOnRealHost() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      QInstance instance = SampleMetaDataProvider.defineTestInstance();
      ReferenceDataQBit databaseQBit = new ReferenceDataQBit("db", SampleMetaDataProvider.RDBMS_BACKEND_NAME);
      databaseQBit.produce(instance);
      Path changelog = directory.resolve("reference-data.xml");
      Files.writeString(changelog, databaseQBit.liquibaseChangelog());
      try(var connection = ConnectionManager.getConnection((RDBMSBackendMetaData) instance.getBackend(SampleMetaDataProvider.RDBMS_BACKEND_NAME));
         var liquibase = new Liquibase(changelog.getFileName().toString(), new DirectoryResourceAccessor(directory),
            new JdbcConnection(connection)))
      {
         liquibase.update();
      }
      QContext.init(instance, new QSession());
      assertEquals(new ReferenceDataQBit.SyncResult(1, 0, 0, 0), databaseQBit.sync(List.of(
         new ReferenceDataQBit.Source("A", "Alpha"))));
      assertEquals("Alpha", databaseQBit.records().get(0).getValueString("name"));
   }
}
