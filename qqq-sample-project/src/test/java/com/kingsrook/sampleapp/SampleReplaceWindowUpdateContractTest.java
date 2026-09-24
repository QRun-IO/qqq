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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.AbstractPreInsertCustomizer;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.ReplaceAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
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
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Replace omission windows and UPDATE result integrity on canonical native H2.
 ** Window policy: exclude retained keys, preserve the grouped original predicate,
 ** then apply its root ORDER/SKIP/LIMIT. Matching itself remains unrestricted.
 *******************************************************************************/
class SampleReplaceWindowUpdateContractTest
{
   private static final String TABLE = "fieldLab";
   private QInstance instance;
   private QTableMetaData canonical;
   private String canonicalJson;
   private QTableMetaData active;
   private String activeJson;



   /*******************************************************************************
    ** Six deliberately non-key-ordered rows distinguish each window operation.
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
      active.getField("textValue").setIsRequired(true);
      active.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(UpdateCallbacks.class))
         .withCustomizer(TableCustomizers.POST_UPDATE_RECORD, new QCodeReference(UpdateCallbacks.class))
         .withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(DeleteCounter.class))
         .withCustomizer(TableCustomizers.POST_INSERT_RECORD, new QCodeReference(InsertCounter.class));
      Map<String, QTableMetaData> tables = new LinkedHashMap<>(instance.getTables());
      tables.put(TABLE, active);
      instance.setTables(tables);
      activeJson = JsonUtils.toJson(active);
      seed();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      resetCounters();
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** The kept row occupies the pre-exclusion window. Excluding it first selects
    ** id5; ignoring ORDER or SKIP selects id3, and losing LIMIT deletes more rows.
    ** OR grouping also prevents a retained key from re-entering the delete set.
    *******************************************************************************/
   @Test
   void testDeletionWindowAppliesAfterKeepExclusionAndPreservesOrGrouping() throws Exception
   {
      QQueryFilter filter = new QQueryFilter().withBooleanOperator(QQueryFilter.BooleanOperator.OR)
         .withCriteria(new QFilterCriteria("booleanValue", QCriteriaOperator.EQUALS, true))
         .withCriteria(new QFilterCriteria("name", QCriteriaOperator.EQUALS, "Outside"))
         .withOrderBy(new QFilterOrderBy("longValue", true)).withSkip(1).withLimit(1);
      assertEquals(List.of(5), nativeIds("SELECT id FROM field_lab WHERE id<>1 AND (boolean_value=TRUE OR name='Outside') ORDER BY long_value ASC OFFSET 1 ROWS FETCH NEXT 1 ROWS ONLY"));
      List<Map<String, String>> expected = rows();
      expected.get(0).put("HTML_VALUE", "Matched update");
      expected.remove(4);
      ReplaceInput input = input(new QRecord().withValue("name", "Target").withValue("htmlValue", "Matched update")).withFilter(filter);
      String filterJson = JsonUtils.toJson(filter);
      ReplaceOutput output = new ReplaceAction().execute(input);
      assertAll(
         () -> assertSuccess(output, 0, 1, 1),
         () -> assertEquals(expected, rows()),
         () -> assertEquals(List.of(5), DeleteCounter.ids),
         () -> assertSame(filter, input.getFilter()),
         () -> assertEquals(filterJson, JsonUtils.toJson(filter)),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** Zero limit suppresses omission deletion, not matching or the allowed UPDATE.
    *******************************************************************************/
   @Test
   void testZeroDeletionLimitStillAllowsMatchedUpdate() throws Exception
   {
      List<Map<String, String>> expected = rows();
      expected.get(0).put("HTML_VALUE", "Matched update");
      ReplaceInput input = input(new QRecord().withValue("name", "Target").withValue("htmlValue", "Matched update"))
         .withFilter(segment().withLimit(0));
      ReplaceOutput output = new ReplaceAction().execute(input);
      assertAll(
         () -> assertSuccess(output, 0, 1, 0),
         () -> assertEquals(expected, rows()),
         () -> assertEquals(List.of(), DeleteCounter.ids),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** A negative window must reject and preserve storage, not disappear inside a
    ** nested predicate. No callback-side-effect rollback guarantee is asserted.
    *******************************************************************************/
   @Test
   void testNegativeDeletionWindowsRejectWithoutCommittingChanges()
   {
      assertAll(
         () -> assertInvalidWindow(segment().withSkip(-1)),
         () -> assertInvalidWindow(segment().withLimit(-1)));
   }



   /*******************************************************************************
    ** Required UPDATE failure coexists with an earlier INSERT and valid sibling.
    ** POST cannot erase the error or remove its record to commit a partial result.
    *******************************************************************************/
   @Test
   void testUpdatePostCannotClearOrDropFailedNativeBackedRecord()
   {
      assertAll(
         () -> assertHiddenUpdateFailure("clear"),
         () -> assertHiddenUpdateFailure("drop"));
   }



   /*******************************************************************************
    ** Native H2 VARCHAR overflow throws before POST. Its concrete SQLState and
    ** complete rollback are a control, not a claim of clearable returned SQL errors.
    *******************************************************************************/
   @Test
   void testNativeUpdateExceptionStopsPostAndRollsBackEarlierInsert() throws Exception
   {
      List<Map<String, String>> before = rows();
      UpdateCallbacks.mode = "clear";
      ReplaceInput input = input(newRecord(), new QRecord().withValue("name", "Target").withValue("passThroughValue", "X".repeat(300)));
      Attempt attempt = attempt(input);
      assertAll(
         () ->
         {
            Throwable cause = attempt.exception();
            assertNotNull(cause);
            while(cause != null && !(cause instanceof SQLException))
            {
               cause = cause.getCause();
            }
            assertEquals("22001", assertInstanceOf(SQLException.class, cause).getSQLState());
         },
         () -> assertEquals(1, InsertCounter.count),
         () -> assertEquals(1, UpdateCallbacks.preCount),
         () -> assertEquals(0, UpdateCallbacks.badInputCount),
         () -> assertEquals(0, UpdateCallbacks.postCount),
         () -> assertEquals(List.of(), DeleteCounter.ids),
         () -> assertEquals(before, rows()),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** Valid UPDATE POST clones/reordering/warnings remain supported alongside
    ** earlier INSERT and exact omission deletion; only errors block completion.
    *******************************************************************************/
   @Test
   void testValidUpdatePostClonesReorderAndWarningsStillCommit() throws Exception
   {
      UpdateCallbacks.mode = "warnings";
      List<Map<String, String>> before = rows();
      ReplaceInput input = input(newRecord(), new QRecord().withValue("name", "Target").withValue("textValue", "Valid replacement"),
         new QRecord().withValue("name", "Sibling").withValue("htmlValue", "Sibling replacement"));
      ReplaceOutput output = new ReplaceAction().execute(input);
      assertSuccess(output, 1, 2, 3);
      List<Map<String, String>> expected = new ArrayList<>();
      Map<String, String> target = new LinkedHashMap<>(before.get(0));
      target.put("TEXT_VALUE", "Valid replacement");
      expected.add(target);
      Map<String, String> sibling = new LinkedHashMap<>(before.get(1));
      sibling.put("HTML_VALUE", "Sibling replacement");
      expected.add(sibling);
      expected.add(before.get(5));
      int insertedId = output.getInsertOutput().getRecords().get(0).getValueInteger("id");
      assertTrue(insertedId > 6);
      Map<String, String> added = new LinkedHashMap<>();
      before.get(0).keySet().forEach(field -> added.put(field, null));
      added.put("ID", Integer.toString(insertedId));
      added.put("NAME", "New");
      added.put("LONG_VALUE", "10");
      added.put("TEXT_VALUE", "New text");
      added.put("BOOLEAN_VALUE", "TRUE");
      expected.add(added);
      assertAll(
         () -> assertEquals(expected, rows()),
         () -> assertEquals(List.of("Sibling", "Target"), output.getUpdateOutput().getRecords().stream().map(record -> record.getValueString("name")).toList()),
         () ->
         {
            for(QRecord record : output.getUpdateOutput().getRecords())
            {
               assertEquals(List.of("Reviewed " + record.getValueString("name")), record.getWarnings().stream().map(QWarningMessage::getMessage).toList());
            }
         },
         () -> assertEquals(List.of(3, 4, 5), DeleteCounter.ids.stream().sorted().toList()),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** Inspect a caller-owned transaction before rollback to prove invalid PRE
    ** origin rejection actually precedes native INSERT, not merely owned rollback.
    *******************************************************************************/
   @Test
   void testInvalidPreOriginDoesNotInsertInsideCallerTransaction() throws Exception
   {
      active.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(FreshPre.class));
      activeJson = JsonUtils.toJson(active);
      List<Map<String, String>> before = rows();
      try(RDBMSTransaction transaction = new RDBMSTransaction(connection()))
      {
         Connection connection = transaction.getConnection();
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("UPDATE field_lab SET html_value='Caller pending' WHERE id=6"));
         }
         List<Map<String, String>> pending = rows(connection);
         ReplaceInput input = input(newRecord()).withTransaction(transaction).withSetPrimaryKeyInInsertedRecords(true);
         Attempt attempt = attempt(input);
         assertAll(
            () -> assertRejected(attempt),
            () -> assertEquals(1, FreshPre.count),
            () -> assertEquals(0, InsertCounter.count),
            () -> assertEquals(0, UpdateCallbacks.preCount),
            () -> assertEquals(List.of(), DeleteCounter.ids),
            () -> assertEquals(pending, rows(connection)),
            () -> assertEquals(before, rows()),
            () -> assertCallerOwns(input, transaction, connection));
         transaction.rollback();
         assertAll(
            () -> assertEquals(before, rows(connection)),
            () -> assertEquals(before, rows()),
            () -> assertCallerOwns(input, transaction, connection));
      }
      assertMetaDataUnchanged();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertInvalidWindow(QQueryFilter filter) throws Exception
   {
      seed();
      List<Map<String, String>> before = rows();
      String json = JsonUtils.toJson(filter);
      ReplaceInput input = input(new QRecord().withValue("name", "Target").withValue("htmlValue", "Must roll back")).withFilter(filter);
      Attempt attempt = attempt(input);
      assertAll(
         () -> assertRejected(attempt),
         () -> assertEquals(before, rows()),
         () -> assertSame(filter, input.getFilter()),
         () -> assertEquals(json, JsonUtils.toJson(filter)),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    ** A real required-field error is observed in PRE before native dispatch.
    *******************************************************************************/
   private void assertHiddenUpdateFailure(String mode) throws Exception
   {
      seed();
      UpdateCallbacks.mode = mode;
      List<Map<String, String>> before = rows();
      ReplaceInput input = input(newRecord(), new QRecord().withValue("name", "Target").withValue("textValue", null),
         new QRecord().withValue("name", "Sibling").withValue("htmlValue", "Sibling replacement"));
      Attempt attempt = attempt(input);
      assertAll(
         () -> assertRejected(attempt),
         () -> assertEquals(1, InsertCounter.count),
         () -> assertEquals(2, UpdateCallbacks.preCount),
         () -> assertEquals(1, UpdateCallbacks.badInputCount),
         () -> assertEquals(0, UpdateCallbacks.postCount),
         () -> assertEquals(List.of(), DeleteCounter.ids),
         () -> assertEquals(before, rows()),
         this::assertMetaDataUnchanged);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QQueryFilter segment()
   {
      return new QQueryFilter(new QFilterCriteria("booleanValue", QCriteriaOperator.EQUALS, true));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ReplaceInput input(QRecord... records)
   {
      ReplaceInput input = new ReplaceInput().withRecords(List.of(records)).withKey(new UniqueKey("name")).withFilter(segment());
      input.setTableName(TABLE);
      input.setInputSource(QInputSource.USER);
      return input;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QRecord newRecord()
   {
      return new QRecord().withValue("name", "New").withValue("longValue", 10L).withValue("textValue", "New text");
   }



   /*******************************************************************************
    **
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
   private boolean hasErrors(ReplaceOutput output)
   {
      return output != null && (output.getInsertOutput() != null && output.getInsertOutput().getRecords().stream().anyMatch(record -> !record.getErrors().isEmpty())
         || output.getUpdateOutput() != null && output.getUpdateOutput().getRecords().stream().anyMatch(record -> !record.getErrors().isEmpty())
         || output.getDeleteOutput() != null && CollectionUtils.nullSafeHasContents(output.getDeleteOutput().getRecordsWithErrors()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertRejected(Attempt attempt)
   {
      assertTrue(attempt.exception() != null || hasErrors(attempt.output()), "Replace did not reject the invalid operation");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertSuccess(ReplaceOutput output, int inserted, int updated, int deleted)
   {
      assertNotNull(output);
      assertAll(
         () -> assertEquals(inserted, output.getInsertOutput().getRecords().size()),
         () -> assertEquals(updated, output.getUpdateOutput().getRecords().size()),
         () -> assertEquals(deleted, output.getDeleteOutput().getDeletedRecordCount()),
         () -> assertFalse(hasErrors(output), JsonUtils.toJson(output)));
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
   private void seed() throws Exception
   {
      resetCounters();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         statement.executeUpdate("DELETE FROM field_lab");
         assertEquals(6, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value,boolean_value,text_value,html_value) VALUES "
            + "(1,'Target',15,TRUE,'Original target','Original target HTML'),"
            + "(2,'Sibling',50,TRUE,'Original sibling','Original sibling HTML'),"
            + "(3,'Omission first',10,TRUE,'Original third','Original third HTML'),"
            + "(4,'Omission third',40,TRUE,'Original fourth','Original fourth HTML'),"
            + "(5,'Omission second',20,TRUE,'Original fifth','Original fifth HTML'),"
            + "(6,'Outside',60,FALSE,'Original outside','Original outside HTML')"));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void resetCounters()
   {
      UpdateCallbacks.mode = "none";
      UpdateCallbacks.preCount = 0;
      UpdateCallbacks.badInputCount = 0;
      UpdateCallbacks.postCount = 0;
      DeleteCounter.ids = new ArrayList<>();
      InsertCounter.count = 0;
      FreshPre.count = 0;
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
   private List<Integer> nativeIds(String sql) throws Exception
   {
      List<Integer> result = new ArrayList<>();
      try(Connection connection = connection(); Statement statement = connection.createStatement(); ResultSet rows = statement.executeQuery(sql))
      {
         while(rows.next())
         {
            result.add(rows.getInt(1));
         }
      }
      return result;
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
    ** Exact native snapshots include every column and actual BLOB bytes.
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
   private void assertMetaDataUnchanged()
   {
      assertEquals(activeJson, JsonUtils.toJson(active));
      assertEquals(canonicalJson, JsonUtils.toJson(canonical));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private record Attempt(ReplaceOutput output, QException exception)
   {
   }



   /*******************************************************************************
    ** Ordinary public UPDATE callbacks observe/erase real validation results.
    *******************************************************************************/
   public static class UpdateCallbacks implements TableCustomizerInterface
   {
      private static String mode;
      private static int preCount;
      private static int badInputCount;
      private static int postCount;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecords)
      {
         preCount += records.size();
         badInputCount += (int) records.stream().filter(record -> record.getErrors().stream().anyMatch(BadInputStatusMessage.class::isInstance)).count();
         return records;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postUpdate(UpdateInput input, List<QRecord> records, Optional<List<QRecord>> oldRecords)
      {
         postCount++;
         if("drop".equals(mode))
         {
            return records.stream().filter(record -> record.getErrors().isEmpty()).map(QRecord::new).toList();
         }
         List<QRecord> copies = records.stream().map(QRecord::new).toList();
         if("clear".equals(mode))
         {
            copies.forEach(record -> record.getErrors().clear());
         }
         if("warnings".equals(mode))
         {
            copies.forEach(record -> record.addWarning(new QWarningMessage("Reviewed " + record.getValueString("name"))));
            return List.of(copies.get(1), copies.get(0));
         }
         return copies;
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public static class DeleteCounter implements TableCustomizerInterface
   {
      private static List<Integer> ids = new ArrayList<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         records.forEach(record -> ids.add(record.getValueInteger("id")));
         return records;
      }
   }



   /*******************************************************************************
    ** Count actual successful INSERT outputs before a later UPDATE failure.
    *******************************************************************************/
   public static class InsertCounter implements TableCustomizerInterface
   {
      private static int count;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postInsert(InsertInput input, List<QRecord> records)
      {
         count += records.size();
         return records;
      }
   }



   /*******************************************************************************
    ** A new valid PRE record cannot be correlated with the caller for publication.
    *******************************************************************************/
   public static class FreshPre implements TableCustomizerInterface
   {
      private static int count;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public AbstractPreInsertCustomizer.WhenToRun whenToRunPreInsert(InsertInput input, boolean isPreview)
      {
         return AbstractPreInsertCustomizer.WhenToRun.BEFORE_ALL_VALIDATIONS;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean isPreview)
      {
         count += records.size();
         return List.of(newRecord().withValue("name", "Fresh replacement"));
      }
   }
}
