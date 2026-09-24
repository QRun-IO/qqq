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


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.ReplaceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.AbstractTableActionInput;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.InputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.replace.ReplaceOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Staged common Replace probes on canonical FieldLab and native H2 storage.
 ** Owned transaction rollback is an RDBMS assertion, not a provider-wide promise.
 ** Returned phase errors or a QException may communicate rejection; native state
 ** and phase counters independently require no later omission deletion/commit.
 *******************************************************************************/
class SampleReplaceContractProbeTest
{
   private static final String TABLE = "fieldLab";
   private QInstance instance;
   private QTableMetaData canonical;
   private String canonicalJson;
   private QTableMetaData active;
   private String activeJson;



   /*******************************************************************************
    ** Existing NONE behavior makes full native snapshots deterministic; it does
    ** not alter the matching, validation, permissions or transaction contracts.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      canonical = instance.getTable(TABLE);
      canonicalJson = JsonUtils.toJson(canonical);
      active = canonical.clone();
      for(String field : List.of("createDate", "modifyDate", "createdDay", "modifiedDay", "userIdValue"))
      {
         active.getField(field).withBehaviors(new HashSet<>(active.getField(field).getBehaviors()))
            .withBehavior(DynamicDefaultValueBehavior.NONE);
      }
      active.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(PhaseCustomizer.class))
         .withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(PhaseCustomizer.class))
         .withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(PhaseCustomizer.class))
         .withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(PhaseCustomizer.class));
      Map<String, QTableMetaData> tables = new LinkedHashMap<>(instance.getTables());
      tables.put(TABLE, active);
      instance.setTables(tables);
      activeJson = JsonUtils.toJson(active);
      PhaseCustomizer.reset();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(3, statement.executeUpdate("INSERT INTO field_lab(id,name,normalized_key,long_value,text_value) VALUES "
            + "(1,'Matched','MATCH',10,'Private matched'),"
            + "(2,'Omitted','OMIT',10,'Private omitted'),"
            + "(3,'Outside','OUT',20,'Private outside')"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      PhaseCustomizer.reset();
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** A returned INSERT validation error must stop UPDATE and omission DELETE.
    *******************************************************************************/
   @Test
   void testInsertErrorStopsLaterPhasesAndOwnedCommit() throws Exception
   {
      assertOwnedPhaseFailure("insert", 1, 0, 0);
   }



   /*******************************************************************************
    ** The earlier successful INSERT must roll back when UPDATE returns an error.
    *******************************************************************************/
   @Test
   void testUpdateErrorStopsOmissionDeleteAndRollsBackEarlierInsert() throws Exception
   {
      assertOwnedPhaseFailure("update", 1, 1, 0);
   }



   /*******************************************************************************
    ** A returned DELETE error must prevent committing earlier INSERT and UPDATE.
    *******************************************************************************/
   @Test
   void testDeleteErrorRollsBackEarlierInsertAndUpdate() throws Exception
   {
      assertOwnedPhaseFailure("delete", 1, 1, 1);
   }



   /*******************************************************************************
    ** Matching and UPDATE deliberately cross the deletion filter. Only omission
    ** deletion is limited to longValue=10, and committed inserted ids are usable.
    *******************************************************************************/
   @Test
   void testValidReplaceUpdatesOutsideDeletionFilterAndCommits() throws Exception
   {
      List<Map<String, String>> before = rows();
      QRecord outside = record("Outside", "Changed outside");
      ReplaceInput input = input(record("Matched", "Changed matched"), newRecord(), outside);
      QQueryFilter filter = input.getFilter();
      String filterJson = JsonUtils.toJson(filter);
      ReplaceOutput output = new ReplaceAction().execute(input);
      assertSuccess(output, 1, 2, 1);
      int insertedId = output.getInsertOutput().getRecords().get(0).getValueInteger("id");
      List<Map<String, String>> expected = new ArrayList<>();
      expected.add(changed(before.get(0), "TEXT_VALUE", "Changed matched"));
      expected.add(changed(before.get(2), "TEXT_VALUE", "Changed outside"));
      expected.add(insertedRow(before.get(0), insertedId, "New", "NEW", "New text"));
      assertAll(
         () -> assertEquals(expected, rows()),
         () -> assertEquals(3, outside.getValueInteger("id")),
         () -> assertEquals(insertedId, input.getRecords().get(1).getValueInteger("id")),
         () -> assertSame(filter, input.getFilter()),
         () -> assertEquals(filterJson, JsonUtils.toJson(filter)),
         () -> assertNull(input.getTransaction()),
         this::assertMetadataUnchanged);
   }



   /*******************************************************************************
    ** Failure must not commit/rollback/close unrelated caller work. Earlier
    ** INSERT work may remain pending; the caller explicitly rolls everything back.
    *******************************************************************************/
   @Test
   void testFailedReplaceLeavesCallerTransactionOwnedAndOmissionsUntouched() throws Exception
   {
      List<Map<String, String>> before = rows();
      PhaseCustomizer.rejectedPhase = "update";
      try(RDBMSTransaction transaction = new RDBMSTransaction(connection()))
      {
         Connection connection = transaction.getConnection();
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("UPDATE field_lab SET text_value='Caller pending' WHERE id=3"));
         }
         ReplaceInput input = input(record("Matched", "Rejected change"), newRecord()).withTransaction(transaction);
         Attempt attempt = attempt(input);
         List<Map<String, String>> pending = rows(connection);
         List<Map<String, String>> expectedPending = new ArrayList<>(before);
         expectedPending.set(2, changed(before.get(2), "TEXT_VALUE", "Caller pending"));
         if(pending.size() == 4)
         {
            int insertedId = Integer.parseInt(pending.get(3).get("ID"));
            expectedPending.add(insertedRow(before.get(0), insertedId, "New", "NEW", "New text"));
         }
         assertAll(
            () -> assertRejected(attempt),
            () -> assertEquals(expectedPending, pending),
            () -> assertEquals(1, PhaseCustomizer.insertCount),
            () -> assertEquals(1, PhaseCustomizer.updateCount),
            () -> assertEquals(0, PhaseCustomizer.deleteCount),
            () -> assertEquals(before.get(0), row(rows(connection), 1)),
            () -> assertEquals(before.get(1), row(rows(connection), 2)),
            () -> assertEquals(changed(before.get(2), "TEXT_VALUE", "Caller pending"), row(rows(connection), 3)),
            () -> assertEquals(before, rows()),
            () -> assertCallerOwns(input, transaction, connection));
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("UPDATE field_lab SET text_value='Caller still owns' WHERE id=3"));
         }
         transaction.rollback();
         assertAll(
            () -> assertCallerOwns(input, transaction, connection),
            () -> assertEquals(before, rows(connection)),
            () -> assertEquals(before, rows()));
      }
      assertEquals(before, rows());
   }



   /*******************************************************************************
    ** Matching sees the caller's uncommitted row. Replace leaves all work pending
    ** and only the caller's explicit commit makes the exact replacement visible.
    *******************************************************************************/
   @Test
   void testSuccessfulReplaceUsesCallerSnapshotAndLeavesCommitToCaller() throws Exception
   {
      List<Map<String, String>> before = rows();
      try(RDBMSTransaction transaction = new RDBMSTransaction(connection()))
      {
         Connection connection = transaction.getConnection();
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value,text_value) VALUES(99,'Pending',10,'Caller inserted')"));
         }
         QRecord pending = record("Pending", "Changed pending");
         ReplaceInput input = input(record("Matched", "Changed matched"), pending).withTransaction(transaction);
         ReplaceOutput output = new ReplaceAction().execute(input);
         assertSuccess(output, 0, 2, 1);
         List<Map<String, String>> expected = new ArrayList<>();
         expected.add(changed(before.get(0), "TEXT_VALUE", "Changed matched"));
         expected.add(before.get(2));
         Map<String, String> pendingRow = emptyRow(before.get(0));
         pendingRow.put("ID", "99");
         pendingRow.put("NAME", "Pending");
         pendingRow.put("LONG_VALUE", "10");
         pendingRow.put("TEXT_VALUE", "Changed pending");
         expected.add(pendingRow);
         assertAll(
            () -> assertEquals(99, pending.getValueInteger("id")),
            () -> assertEquals(expected, rows(connection)),
            () -> assertEquals(before, rows()),
            () -> assertCallerOwns(input, transaction, connection));
         transaction.commit();
         assertAll(
            () -> assertCallerOwns(input, transaction, connection),
            () -> assertEquals(expected, rows(connection)),
            () -> assertEquals(expected, rows()));
      }
   }



   /*******************************************************************************
    ** USER reaches matching and all three DML phases. A removed field cannot be
    ** inserted or updated, while allowed fields and omission deletion still work.
    *******************************************************************************/
   @Test
   void testUserSourcePreservesPersonalizedWriteFieldsAcrossAllPhases() throws Exception
   {
      assertSource(QInputSource.USER);
   }



   /*******************************************************************************
    ** The same personalizer leaves SYSTEM unrestricted, providing a real control.
    *******************************************************************************/
   @Test
   void testSystemSourceKeepsUnrestrictedInsertAndUpdate() throws Exception
   {
      assertSource(QInputSource.SYSTEM);
   }



   /*******************************************************************************
    ** Explicit opt-in single-column null equality must retain the native owner.
    *******************************************************************************/
   @Test
   void testSingleNullableMatchingKeyRetainsOwnerWhenEnabled() throws Exception
   {
      assertNullableMatch(new UniqueKey("normalizedKey"), true);
   }



   /*******************************************************************************
    ** Default null-distinct matching inserts a new identity and leaves the owner
    ** alone when omission deletion is disabled; this is not a SQL UPSERT promise.
    *******************************************************************************/
   @Test
   void testSingleNullableKeyIsDistinctByDefault() throws Exception
   {
      assertNullableMatch(new UniqueKey("normalizedKey"), false);
   }



   /*******************************************************************************
    ** Existing composite-null matching is a positive control for the one-column
    ** regression; the complete tuple and retained native id are asserted.
    *******************************************************************************/
   @Test
   void testCompositeNullableMatchingKeyRetainsOwnerWhenEnabled() throws Exception
   {
      assertNullableMatch(new UniqueKey("normalizedKey", "longValue"), true);
   }



   /*******************************************************************************
    ** The error comes from a real normal DML customizer, not a fake backend. Every
    ** assertion runs even when current Replace has already committed wrong data.
    *******************************************************************************/
   private void assertOwnedPhaseFailure(String phase, int insertCount, int updateCount, int deleteCount) throws Exception
   {
      List<Map<String, String>> before = rows();
      PhaseCustomizer.rejectedPhase = phase;
      ReplaceInput input = input(record("Matched", "Changed matched"), newRecord());
      Attempt attempt = attempt(input);
      assertAll(
         () -> assertRejected(attempt),
         () -> assertEquals(insertCount, PhaseCustomizer.insertCount),
         () -> assertEquals(updateCount, PhaseCustomizer.updateCount),
         () -> assertEquals(deleteCount, PhaseCustomizer.deleteCount),
         () -> assertEquals(before, rows()),
         () -> assertNull(input.getTransaction()),
         this::assertMetadataUnchanged);
   }



   /*******************************************************************************
    ** Metadata removal is scoped to USER; canonical fields remain registered.
    *******************************************************************************/
   private void assertSource(QInputSource source) throws Exception
   {
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(PrivateText.class));
      List<Map<String, String>> before = rows();
      ReplaceInput input = input(record("Matched", "Attempted private update").withValue("longValue", 11L), newRecord());
      input.setInputSource(source);
      ReplaceOutput output = new ReplaceAction().execute(input);
      assertSuccess(output, 1, 1, 1);
      int insertedId = output.getInsertOutput().getRecords().get(0).getValueInteger("id");
      List<Map<String, String>> expected = new ArrayList<>();
      Map<String, String> matched = changed(before.get(0), "LONG_VALUE", "11");
      if(source == QInputSource.SYSTEM)
      {
         matched.put("TEXT_VALUE", "Attempted private update");
      }
      expected.add(matched);
      expected.add(before.get(2));
      expected.add(insertedRow(before.get(0), insertedId, "New", "NEW", source == QInputSource.USER ? null : "New text"));
      assertAll(
         () -> assertEquals(expected, rows()),
         () -> assertFalse(PhaseCustomizer.querySources.isEmpty()),
         () -> assertTrue(PhaseCustomizer.querySources.stream().allMatch(source::equals), PhaseCustomizer.querySources.toString()),
         () -> assertEquals(List.of(source), PhaseCustomizer.insertSources),
         () -> assertEquals(List.of(source), PhaseCustomizer.updateSources),
         () -> assertEquals(List.of(source), PhaseCustomizer.deleteSources),
         this::assertMetadataUnchanged);
   }



   /*******************************************************************************
    ** A differently named candidate avoids a second name-uniqueness constraint
    ** concealing whether the nullable key matched or inserted a new record.
    *******************************************************************************/
   private void assertNullableMatch(UniqueKey key, boolean equalNulls) throws Exception
   {
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(1, statement.executeUpdate("UPDATE field_lab SET normalized_key=NULL WHERE id=1"));
      }
      List<Map<String, String>> before = rows();
      QRecord candidate = record("Nullable candidate", "Nullable replacement").withValue("normalizedKey", null).withValue("longValue", 10L);
      ReplaceInput input = input(candidate).withKey(key).withPerformDeletes(false);
      if(equalNulls)
      {
         input.setAllowNullKeyValuesToEqual(true);
      }
      else
      {
         assertFalse(input.getAllowNullKeyValuesToEqual());
      }
      ReplaceOutput output = new ReplaceAction().execute(input);
      assertNull(output.getDeleteOutput());
      assertSuccess(output, equalNulls ? 0 : 1, equalNulls ? 1 : 0, null);
      List<Map<String, String>> expected = new ArrayList<>(before);
      if(equalNulls)
      {
         Map<String, String> updated = changed(before.get(0), "NAME", "Nullable candidate");
         updated.put("TEXT_VALUE", "Nullable replacement");
         expected.set(0, updated);
         assertEquals(1, candidate.getValueInteger("id"));
      }
      else
      {
         int insertedId = output.getInsertOutput().getRecords().get(0).getValueInteger("id");
         assertTrue(insertedId > 3);
         assertEquals(insertedId, candidate.getValueInteger("id"));
         expected.add(insertedRow(before.get(0), insertedId, "Nullable candidate", null, "Nullable replacement"));
      }
      assertAll(
         () -> assertEquals(expected, rows()),
         () -> assertEquals(0, PhaseCustomizer.deleteCount),
         this::assertMetadataUnchanged);
   }



   /*******************************************************************************
    ** Error transport has not yet been selected for the correction. Both existing
    ** public mechanisms can reject, but silent success cannot satisfy this probe.
    *******************************************************************************/
   private Attempt attempt(ReplaceInput input)
   {
      try
      {
         return new Attempt(new ReplaceAction().execute(input), null);
      }
      catch(QException e)
      {
         return new Attempt(null, e);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertRejected(Attempt attempt)
   {
      if(attempt.exception() != null)
      {
         return;
      }
      ReplaceOutput output = attempt.output();
      assertNotNull(output);
      boolean insertError = output.getInsertOutput() != null && output.getInsertOutput().getRecords().stream().anyMatch(r -> !r.getErrors().isEmpty());
      boolean updateError = output.getUpdateOutput() != null && output.getUpdateOutput().getRecords().stream().anyMatch(r -> !r.getErrors().isEmpty());
      boolean deleteError = output.getDeleteOutput() != null && CollectionUtils.nullSafeHasContents(output.getDeleteOutput().getRecordsWithErrors());
      assertTrue(insertError || updateError || deleteError, JsonUtils.toJson(output));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertSuccess(ReplaceOutput output, int inserts, int updates, Integer deletes)
   {
      assertAll(
         () -> assertEquals(inserts, output.getInsertOutput().getRecords().size()),
         () -> assertEquals(updates, output.getUpdateOutput().getRecords().size()),
         () -> assertTrue(output.getInsertOutput().getRecords().stream().allMatch(r -> r.getErrors().isEmpty()), JsonUtils.toJson(output)),
         () -> assertTrue(output.getUpdateOutput().getRecords().stream().allMatch(r -> r.getErrors().isEmpty()), JsonUtils.toJson(output)));
      if(deletes != null)
      {
         assertEquals(deletes, output.getDeleteOutput().getDeletedRecordCount());
         assertTrue(CollectionUtils.nullSafeIsEmpty(output.getDeleteOutput().getRecordsWithErrors()), JsonUtils.toJson(output));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ReplaceInput input(QRecord... records)
   {
      ReplaceInput input = new ReplaceInput().withKey(new UniqueKey("name")).withRecords(List.of(records))
         .withFilter(new QQueryFilter(new QFilterCriteria("longValue", QCriteriaOperator.EQUALS, 10L)))
         .withSetPrimaryKeyInInsertedRecords(true);
      input.setTableName(TABLE);
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord record(String name, String text)
   {
      return new QRecord().withValue("name", name).withValue("textValue", text);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord newRecord()
   {
      return record("New", "New text").withValue("normalizedKey", "NEW").withValue("longValue", 10L);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, String> changed(Map<String, String> original, String field, String value)
   {
      Map<String, String> result = new LinkedHashMap<>(original);
      result.put(field, value);
      return result;
   }



   /*******************************************************************************
    ** Native values are not inferred from framework output or another QQQ query.
    *******************************************************************************/
   private Map<String, String> insertedRow(Map<String, String> template, int id, String name, String key, String text)
   {
      Map<String, String> result = emptyRow(template);
      result.put("ID", String.valueOf(id));
      result.put("NAME", name);
      result.put("NORMALIZED_KEY", key);
      result.put("LONG_VALUE", "10");
      result.put("TEXT_VALUE", text);
      result.put("BOOLEAN_VALUE", "TRUE");
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, String> emptyRow(Map<String, String> template)
   {
      Map<String, String> result = new LinkedHashMap<>();
      template.keySet().forEach(key -> result.put(key, null));
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Map<String, String> row(List<Map<String, String>> rows, int id)
   {
      return rows.stream().filter(row -> String.valueOf(id).equals(row.get("ID"))).findFirst().orElseThrow();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Connection connection() throws Exception
   {
      return ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<Map<String, String>> rows() throws Exception
   {
      try(Connection connection = connection())
      {
         return rows(connection);
      }
   }



   /*******************************************************************************
    ** Every physical column is compared; binary values use actual JDBC bytes.
    *******************************************************************************/
   private List<Map<String, String>> rows(Connection connection) throws Exception
   {
      List<Map<String, String>> result = new ArrayList<>();
      try(Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery("SELECT * FROM field_lab ORDER BY id"))
      {
         while(rows.next())
         {
            Map<String, String> row = new LinkedHashMap<>();
            for(int index = 1; index <= rows.getMetaData().getColumnCount(); index++)
            {
               String name = rows.getMetaData().getColumnLabel(index);
               row.put(name, "BLOB_VALUE".equals(name) && rows.getBytes(index) != null
                  ? Base64.getEncoder().encodeToString(rows.getBytes(index)) : rows.getString(index));
            }
            result.add(row);
         }
      }
      return result;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertCallerOwns(ReplaceInput input, RDBMSTransaction transaction, Connection connection) throws Exception
   {
      assertSame(transaction, input.getTransaction());
      assertSame(connection, transaction.getConnection());
      assertFalse(connection.isClosed());
      assertFalse(connection.getAutoCommit());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertMetadataUnchanged() throws Exception
   {
      assertSame(active, instance.getTable(TABLE));
      assertEquals(activeJson, JsonUtils.toJson(active));
      assertEquals(canonicalJson, JsonUtils.toJson(canonical));
   }



   /*******************************************************************************
    ** Only a temporary test result holder; it does not change the action API.
    *******************************************************************************/
   private record Attempt(ReplaceOutput output, QException exception)
   {
   }



   /*******************************************************************************
    ** Normal table hooks observe phase order and return ordinary validation errors.
    *******************************************************************************/
   public static class PhaseCustomizer implements TableCustomizerInterface
   {
      private static String rejectedPhase;
      private static int insertCount;
      private static int updateCount;
      private static int deleteCount;
      private static final List<InputSource> querySources = new ArrayList<>();
      private static final List<InputSource> insertSources = new ArrayList<>();
      private static final List<InputSource> updateSources = new ArrayList<>();
      private static final List<InputSource> deleteSources = new ArrayList<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      private static void reset()
      {
         rejectedPhase = null;
         insertCount = 0;
         updateCount = 0;
         deleteCount = 0;
         querySources.clear();
         insertSources.clear();
         updateSources.clear();
         deleteSources.clear();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         querySources.add(((AbstractTableActionInput) input).getInputSource());
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean isPreview)
      {
         insertCount += records.size();
         if(!records.isEmpty())
         {
            insertSources.add(input.getInputSource());
         }
         reject("insert", records);
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecords)
      {
         updateCount += records.size();
         if(!records.isEmpty())
         {
            updateSources.add(input.getInputSource());
         }
         reject("update", records);
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         deleteCount += records.size();
         if(!records.isEmpty())
         {
            deleteSources.add(input.getInputSource());
         }
         reject("delete", records);
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private void reject(String phase, List<QRecord> records)
      {
         if(phase.equals(rejectedPhase))
         {
            records.forEach(record -> record.addError(new BadInputStatusMessage("Rejected Replace " + phase + " phase")));
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PrivateText implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(!TABLE.equals(input.getTableName()) || input.getInputSource() != QInputSource.USER)
         {
            return input.getTable();
         }
         QTableMetaData result = input.getTable().clone();
         result.getFields().remove("textValue");
         return result;
      }
   }
}
