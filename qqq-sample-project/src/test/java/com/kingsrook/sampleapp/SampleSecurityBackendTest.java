/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2022.  Kingsrook, LLC
 * 651 N Broad St Ste 205 # 6917 | Middletown DE 19709 | United States
 * contact@kingsrook.com
 * https://github.com/Kingsrook/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.kingsrook.sampleapp;


import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qqq.backend.core.actions.reporting.ExportAction;
import com.kingsrook.qqq.backend.core.actions.tables.AggregateAction;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ExportInput;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportDestination;
import com.kingsrook.qqq.backend.core.model.actions.reporting.ReportFormat;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.Aggregate;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.aggregate.AggregateOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryJoin;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Owned native security boundaries and the same sample table on Memory.
 *******************************************************************************/
class SampleSecurityBackendTest
{
   private static final String TABLE = "fieldLab";
   private static final String MEMORY = "memoryFieldLab";



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      MemoryRecordStore.fullReset();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      QContext.init(SampleMetaDataProvider.defineTestInstance(), new QSession().withSecurityKeyValue("sampleOwner", 1L));
      QContext.getQInstance().addSecurityKeyType(new QSecurityKeyType().withName("sampleOwner")
         .withAllAccessKeyName("sampleAll").withNullValueBehaviorKeyName("sampleNull"));
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement())
      {
         statement.executeUpdate("INSERT INTO field_lab(id,name,long_value) VALUES (101,'Forbidden',2),(102,'Allowed',1),(103,'Null owner',NULL)");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      MemoryRecordStore.fullReset();
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testTenantIsolationAcrossReadsExportAndWrites() throws Exception
   {
      QContext.getQInstance().getTable(TABLE).withRecordSecurityLock(lock());
      assertEquals(List.of(102), ids(TABLE));
      assertEquals(1, CountAction.execute(TABLE, null));
      assertNull(GetAction.execute(TABLE, 101));
      assertNull(GetAction.execute(TABLE, 103));
      assertEquals("Allowed", GetAction.execute(TABLE, 102).getValueString("name"));
      Aggregate count = new Aggregate("id", AggregateOperator.COUNT);
      Number countValue = (Number) new AggregateAction().execute(new AggregateInput(TABLE).withAggregates(List.of(count))
         .withInputSource(QInputSource.USER)).getResults().get(0).getAggregateValue(count);
      assertEquals(1L, countValue.longValue());
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      ExportInput export = new ExportInput();
      export.setTableName(TABLE);
      export.setInputSource(QInputSource.USER);
      export.setFieldNames(List.of("name"));
      export.setQueryFilter(new QQueryFilter());
      export.setReportDestination(new ReportDestination().withReportFormat(ReportFormat.JSON).withReportOutputStream(bytes));
      assertEquals(1, new ExportAction().execute(export).getRecordCount());
      JSONArray exported = new JSONArray(bytes.toString(StandardCharsets.UTF_8));
      assertEquals(1, exported.length());
      assertTrue(exported.toString().contains("Allowed"));
      assertFalse(exported.toString().contains("Forbidden"));
      assertFalse(insert(TABLE, new QRecord().withValue("name", "Denied insert").withValue("longValue", 2L)).getErrors().isEmpty());
      assertFalse(new UpdateAction().execute(new UpdateInput(TABLE).withRecord(new QRecord().withValue("id", 101).withValue("name", "Taken")))
         .getRecords().get(0).getErrors().isEmpty());
      assertEquals(0, new DeleteAction().execute(new DeleteInput(TABLE).withPrimaryKey(101)).getDeletedRecordCount());
      assertEquals(List.of("101:Forbidden", "102:Allowed", "103:Null owner"), storedRows());
      QContext.setQSession(new QSession());
      assertEquals(0, CountAction.execute(TABLE, null));
      assertEquals(List.of(), ids(TABLE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMultipleKeysAllAccessAndNullOverrides() throws Exception
   {
      QContext.getQInstance().getTable(TABLE).withRecordSecurityLock(lock());
      QContext.setQSession(new QSession().withSecurityKeyValue("sampleOwner", 1L).withSecurityKeyValue("sampleOwner", 2L));
      assertEquals(List.of(101, 102), ids(TABLE));
      QContext.setQSession(new QSession().withSecurityKeyValue("sampleAll", true));
      assertEquals(List.of(101, 102, 103), ids(TABLE));
      QContext.setQSession(new QSession().withSecurityKeyValue("sampleOwner", 1L).withSecurityKeyValue("sampleNull", "allow"));
      assertEquals(List.of(102, 103), ids(TABLE));
      QContext.setQSession(new QSession().withSecurityKeyValue("sampleOwner", 1L).withSecurityKeyValue("sampleNull", "deny"));
      assertEquals(List.of(102), ids(TABLE));
      QContext.setQSession(new QSession().withSecurityKeyValue("sampleOwner", 1L).withSecurityKeyValue("sampleNull", "invalid"));
      assertEquals(List.of(102), ids(TABLE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNullWriteOnlyAllowsInsertWithoutExposingNullRows() throws Exception
   {
      QContext.getQInstance().getTable(TABLE).withRecordSecurityLock(lock().withNullValueBehavior(RecordSecurityLock.NullValueBehavior.ALLOW_WRITE_ONLY));
      assertEquals(List.of(102), ids(TABLE));
      QRecord inserted = insert(TABLE, new QRecord().withValue("name", "Null write"));
      assertTrue(inserted.getErrors().isEmpty(), inserted.getErrorsAsString());
      assertNotNull(inserted.getValueInteger("id"));
      assertNull(GetAction.execute(TABLE, inserted.getValueInteger("id")));
      assertEquals(4, storedRows().size());
      assertEquals(List.of(102), ids(TABLE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMemoryCrudGeneratedKeysAndRejectedInputs() throws Exception
   {
      addMemoryTable();
      QRecord first = insert(MEMORY, new QRecord().withValue("name", "One").withValue("longValue", 1L));
      QRecord second = insert(MEMORY, new QRecord().withValue("name", "Two").withValue("longValue", 2L));
      assertTrue(first.getErrors().isEmpty());
      assertTrue(second.getErrors().isEmpty());
      Integer id = first.getValueInteger("id");
      assertNotNull(id);
      assertNotEquals(id, second.getValueInteger("id"));
      assertEquals(2, CountAction.execute(MEMORY, null));
      assertEquals(2, ids(MEMORY).size());
      assertNull(GetAction.execute(MEMORY, -1));
      QRecord duplicate = insert(MEMORY, new QRecord().withValue("id", id).withValue("name", "Overwrite"));
      assertFalse(duplicate.getErrors().isEmpty());
      assertEquals("One", GetAction.execute(MEMORY, id).getValueString("name"));
      assertThrows(QException.class, () -> QueryAction.execute(MEMORY, new QQueryFilter(new QFilterCriteria("missingField", QCriteriaOperator.EQUALS, 1))));
      assertEquals(2, CountAction.execute(MEMORY, null));
      assertTrue(new UpdateAction().execute(new UpdateInput(MEMORY).withRecord(new QRecord().withValue("id", id).withValue("name", "Changed")))
         .getRecords().get(0).getErrors().isEmpty());
      assertEquals("Changed", GetAction.execute(MEMORY, id).getValueString("name"));
      assertEquals(1, new DeleteAction().execute(new DeleteInput(MEMORY).withPrimaryKey(id)).getDeletedRecordCount());
      assertNull(GetAction.execute(MEMORY, id));
      assertEquals(1, CountAction.execute(MEMORY, null));
      assertEquals(3, storedRows().size());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testMemorySecurityAndResetIsolation() throws Exception
   {
      addMemoryTable();
      QRecord first = insert(MEMORY, new QRecord().withValue("name", "Owned").withValue("longValue", 1L));
      QRecord second = insert(MEMORY, new QRecord().withValue("name", "Other").withValue("longValue", 2L));
      QContext.getQInstance().getTable(MEMORY).withRecordSecurityLock(lock());
      assertEquals(List.of(first.getValueInteger("id")), ids(MEMORY));
      assertNull(GetAction.execute(MEMORY, second.getValueInteger("id")));
      assertFalse(insert(MEMORY, new QRecord().withValue("name", "Denied").withValue("longValue", 2L)).getErrors().isEmpty());
      QContext.getQInstance().getTable(MEMORY).setRecordSecurityLocks(List.of());
      assertEquals(2, CountAction.execute(MEMORY, null));
      MemoryRecordStore.fullReset();
      assertEquals(0, CountAction.execute(MEMORY, null));
      assertTrue(insert(MEMORY, new QRecord().withValue("name", "Fresh run")).getErrors().isEmpty());
      assertEquals(1, CountAction.execute(MEMORY, null));
   }



   /*******************************************************************************
    ** An orphan participates only in the explicit left join, without a fake parent.
    *******************************************************************************/
   @Test
   void testOrphanAndDuplicateJoinPaths() throws Exception
   {
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend()); Statement statement = connection.createStatement())
      {
         assertEquals(1, statement.executeUpdate("INSERT INTO pet(id,name,species_id,person_id) VALUES (107,'Orphan fixture',1,999)"));
      }
      QQueryFilter orphan = new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.EQUALS, 107));
      QueryInput inner = new QueryInput("pet").withFilter(orphan).withQueryJoin(new QueryJoin("person").withType(QueryJoin.Type.INNER).withSelect(true));
      assertEquals(0, new QueryAction().execute(inner).getRecords().size());
      QueryInput left = new QueryInput("pet").withFilter(orphan).withQueryJoin(new QueryJoin("person").withType(QueryJoin.Type.LEFT).withSelect(true));
      List<QRecord> records = new QueryAction().execute(left).getRecords();
      assertEquals(1, records.size());
      assertEquals(107, records.get(0).getValueInteger("id"));
      assertNull(records.get(0).getValue("person.id"));
      QueryInput duplicate = new QueryInput("person").withQueryJoin(new QueryJoin("pet").withAlias("animal"))
         .withQueryJoin(new QueryJoin("pet").withAlias("animal"));
      assertThrows(QException.class, () -> new QueryAction().execute(duplicate));
      assertEquals(7, CountAction.execute("pet", null));
      assertEquals(5, CountAction.execute("person", null));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RecordSecurityLock lock()
   {
      return new RecordSecurityLock().withSecurityKeyType("sampleOwner").withFieldName("longValue")
         .withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void addMemoryTable()
   {
      QContext.getQInstance().addBackend(new QBackendMetaData().withName("sampleMemory").withBackendType(MemoryBackendModule.class));
      QContext.getQInstance().addTable(QContext.getQInstance().getTable(TABLE).clone().withName(MEMORY).withBackendName("sampleMemory"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord insert(String table, QRecord record) throws QException
   {
      return new InsertAction().execute(new InsertInput(table).withRecord(record)).getRecords().get(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Integer> ids(String table) throws QException
   {
      return QueryAction.execute(table, null).stream().map(record -> record.getValueInteger("id")).sorted().toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> storedRows() throws Exception
   {
      ArrayList<String> result = new ArrayList<>();
      try(Connection connection = ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
         Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT id,name FROM field_lab ORDER BY id"))
      {
         while(rows.next())
         {
            result.add(rows.getInt(1) + ":" + rows.getString(2));
         }
      }
      return result;
   }
}
