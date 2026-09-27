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


import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.QWarningMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.SystemErrorStatusMessage;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Common Delete outcomes use canonical FieldLab and independent native rows.
 ** Failed-statement recovery and transaction visibility here are H2 contracts;
 ** no automatic rollback, HTTP authorization or provider-wide claim is made.
 *******************************************************************************/
class SampleDeleteContractTest
{
   private static final String TABLE = "fieldLab";
   private QInstance instance;
   private boolean constraintAdded;



   /*******************************************************************************
    ** Seed directly so Insert validation cannot influence Delete's row oracle.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      QContext.init(instance, new QSession());
      ThrowAfterDelete.seenKeys = List.of();
      MixedBeforeDelete.sawPreview = false;
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(4, statement.executeUpdate("INSERT INTO field_lab(id,name,decimal_value,boolean_value,text_value) VALUES "
            + "(1,'First',1.0000,TRUE,'Original one'),(2,'Second',2.0000,FALSE,'Original two'),"
            + "(3,'Third',3.0000,TRUE,'Original three'),(4,'Unrelated',4.0000,FALSE,'Original four')"));
      }
   }



   /*******************************************************************************
    ** Drop only this fixture's optional constraint, then release local state.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(constraintAdded)
         {
            try(Connection connection = connection(); Statement statement = connection.createStatement())
            {
               statement.executeUpdate("ALTER TABLE field_lab DROP CONSTRAINT sample_delete_restrict");
            }
         }
      }
      finally
      {
         ThrowAfterDelete.seenKeys = List.of();
         MixedBeforeDelete.sawPreview = false;
         QContext.clear();
         ConnectionManager.resetConnectionProviders();
      }
   }



   /*******************************************************************************
    ** Errors exclude a key, warnings permit it, and a repeated key deletes once.
    ** Successful rows are not echoed as an input-aligned result list.
    *******************************************************************************/
   @Test
   void testMixedPreDeleteOutcomesPreserveRejectedAndUnrelatedRows() throws Exception
   {
      instance.getTable(TABLE).withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(MixedBeforeDelete.class));
      List<Map<String, String>> before = rows();
      DeleteInput input = mixedInput();
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(
         () -> assertEquals(List.of(before.get(0), before.get(3)), rows()),
         () -> assertEquals(2, output.getDeletedRecordCount()),
         () -> assertMixedErrors(output),
         () -> assertEquals(List.of(2), keys(output.getRecordsWithWarnings())),
         () -> assertEquals(List.of("Reviewed before deletion"), record(output.getRecordsWithWarnings(), 2)
            .getWarnings().stream().map(QWarningMessage::getMessage).toList()),
         () -> assertEquals(List.of(3, 1, 2, 2, 999), input.getPrimaryKeys()),
         () -> assertNull(input.getQueryFilter()),
         () -> assertFalse(MixedBeforeDelete.sawPreview));
   }



   /*******************************************************************************
    ** A PRE_DELETE exception stops native DML, restores selection, and does not
    ** poison a later permitted invocation in the same context.
    *******************************************************************************/
   @Test
   void testPreDeleteExceptionPreservesFilterAndAllowsRecovery() throws Exception
   {
      instance.getTable(TABLE).withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(ThrowBeforeFirst.class));
      List<Map<String, String>> before = rows();
      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("id", QCriteriaOperator.IN, 1, 2));
      String filterJson = JsonUtils.toJson(filter);
      DeleteInput input = new DeleteInput(TABLE).withQueryFilter(filter);
      assertAll(
         () -> assertEquals("Owned pre-delete rejection", assertThrows(QException.class, () -> new DeleteAction().execute(input)).getMessage()),
         () -> assertEquals(before, rows()),
         () -> assertSame(filter, input.getQueryFilter()),
         () -> assertEquals(filterJson, JsonUtils.toJson(filter)),
         () -> assertNull(input.getPrimaryKeys()));
      DeleteOutput recovered = new DeleteAction().execute(new DeleteInput(TABLE).withPrimaryKey(2));
      assertAll(
         () -> assertEquals(1, recovered.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(recovered.getRecordsWithErrors())),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(recovered.getRecordsWithWarnings())),
         () -> assertEquals(List.of(before.get(0), before.get(2), before.get(3)), rows()));
   }



   /*******************************************************************************
    ** The caller's pending target participates in prefetch and native deletion;
    ** caller rollback restores the committed row and removes the pending insert.
    *******************************************************************************/
   @Test
   void testCallerPendingTargetDeletionCanBeRolledBack() throws Exception
   {
      assertPendingTargetTransaction(false);
   }



   /*******************************************************************************
    ** No deletion is published to independent connections until caller commit.
    *******************************************************************************/
   @Test
   void testCallerCommitPublishesPendingTargetDeletion() throws Exception
   {
      assertPendingTargetTransaction(true);
   }



   /*******************************************************************************
    ** H2 rejects a bulk statement with a referenced key, then the native adapter
    ** deletes the other keys individually. Releasing the reference permits retry.
    *******************************************************************************/
   @Test
   void testNativeRestrictFallbackPreservesBlockedRowAndRecovers() throws Exception
   {
      addNativeDeleteBlocker();
      List<Map<String, String>> before = rows();
      DeleteInput input = new DeleteInput(TABLE).withPrimaryKeys(List.of(1, 2, 3));
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(
         () -> assertEquals(List.of(before.get(1), before.get(3)), rows()),
         () -> assertNativeBlockedKey(output),
         () -> assertEquals(List.of(1, 2, 3), input.getPrimaryKeys()));
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(1, statement.executeUpdate("UPDATE field_lab SET long_value=NULL WHERE id=4"));
      }
      List<Map<String, String>> expected = rows();
      DeleteOutput recovered = new DeleteAction().execute(new DeleteInput(TABLE).withPrimaryKey(2));
      assertAll(
         () -> assertEquals(1, recovered.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(recovered.getRecordsWithErrors())),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(recovered.getRecordsWithWarnings())),
         () -> assertEquals(List.of(expected.get(1)), rows()));
   }



   /*******************************************************************************
    ** H2 retains earlier caller work and permitted fallback deletes after the
    ** failed key. Only explicit rollback restores the complete original rows.
    *******************************************************************************/
   @Test
   void testNativeFailureRetainsCallerOwnershipUntilRollback() throws Exception
   {
      addNativeDeleteBlocker();
      List<Map<String, String>> before = rows();
      try(RDBMSTransaction transaction = new RDBMSTransaction(connection()))
      {
         Connection connection = transaction.getConnection();
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("UPDATE field_lab SET text_value='Earlier caller work' WHERE id=4"));
         }
         List<Map<String, String>> pending = rows(connection);
         DeleteInput input = new DeleteInput(TABLE).withPrimaryKeys(List.of(1, 2, 3)).withTransaction(transaction);
         DeleteOutput output = new DeleteAction().execute(input);
         assertAll(
            () -> assertNativeBlockedKey(output),
            () -> assertEquals(List.of(pending.get(1), pending.get(3)), rows(connection)),
            () -> assertEquals(before, rows()),
            () -> assertCallerOwns(input, transaction, connection),
            () -> assertEquals(List.of(1, 2, 3), input.getPrimaryKeys()));
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("UPDATE field_lab SET text_value='Later caller work' WHERE id=4"));
         }
         pending.get(3).put("TEXT_VALUE", "Later caller work");
         assertEquals(List.of(pending.get(1), pending.get(3)), rows(connection));
         assertEquals(before, rows());
         transaction.rollback();
         assertCallerOwns(input, transaction, connection);
         assertEquals(before, rows(connection));
      }
      assertEquals(before, rows());
   }



   /*******************************************************************************
    ** POST_DELETE exceptions are reported as warnings on successful keys. They
    ** do not reverse DML or run the callback on rejected/missing records.
    *******************************************************************************/
   @Test
   void testPostDeleteExceptionWarnsOnlyForDeletedRows() throws Exception
   {
      instance.getTable(TABLE).withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(MixedBeforeDelete.class))
         .withCustomizer(TableCustomizers.POST_DELETE_RECORD, new QCodeReference(ThrowAfterDelete.class));
      List<Map<String, String>> before = rows();
      DeleteOutput output = new DeleteAction().execute(mixedInput());
      assertAll(
         () -> assertEquals(List.of(before.get(0), before.get(3)), rows()),
         () -> assertEquals(2, output.getDeletedRecordCount()),
         () -> assertMixedErrors(output),
         () -> assertEquals(List.of(2, 3), ThrowAfterDelete.seenKeys),
         () -> assertEquals(List.of(2, 3), keys(output.getRecordsWithWarnings())),
         () -> assertTrue(output.getRecordsWithWarnings().stream().allMatch(record -> record.getWarnings().stream()
            .anyMatch(warning -> "An error occurred after the delete: Owned post-delete failure".equals(warning.getMessage())))),
         () -> assertTrue(record(output.getRecordsWithWarnings(), 2).getWarnings().stream()
            .anyMatch(warning -> "Reviewed before deletion".equals(warning.getMessage()))));
   }



   /*******************************************************************************
    ** A returned POST_DELETE error is an after-write result. Its warning is
    ** suppressed, but its already-deleted row still contributes to the count.
    *******************************************************************************/
   @Test
   void testPostDeleteReturnedErrorsDoNotUndoNativeDeletion() throws Exception
   {
      instance.getTable(TABLE).withCustomizer(TableCustomizers.POST_DELETE_RECORD, new QCodeReference(StatusAfterDelete.class));
      List<Map<String, String>> before = rows();
      DeleteInput input = new DeleteInput(TABLE).withPrimaryKeys(List.of(3, 1, 2));
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(
         () -> assertEquals(List.of(before.get(3)), rows()),
         () -> assertEquals(3, output.getDeletedRecordCount()),
         () -> assertEquals(List.of(1), keys(output.getRecordsWithErrors())),
         () -> assertTrue(record(output.getRecordsWithErrors(), 1).getErrors().stream()
            .anyMatch(error -> error instanceof BadInputStatusMessage && "Reported after deletion".equals(error.getMessage()))),
         () -> assertEquals(List.of(2), keys(output.getRecordsWithWarnings())),
         () -> assertEquals(List.of("Reviewed after deletion"), record(output.getRecordsWithWarnings(), 2)
            .getWarnings().stream().map(QWarningMessage::getMessage).toList()),
         () -> assertEquals(List.of(3, 1, 2), input.getPrimaryKeys()));
   }



   /*******************************************************************************
    ** Native pending inserts expose a missing transaction on either prefetch or
    ** DML. The committed target separately exposes an unexpected commit/rollback.
    *******************************************************************************/
   private void assertPendingTargetTransaction(boolean commit) throws Exception
   {
      List<Map<String, String>> before = rows();
      List<Map<String, String>> survivors = List.of(before.get(1), before.get(2), before.get(3));
      try(RDBMSTransaction transaction = new RDBMSTransaction(connection()))
      {
         Connection connection = transaction.getConnection();
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("INSERT INTO field_lab(id,name,text_value) VALUES(99,'Pending target','Not committed')"));
         }
         assertEquals(5, rows(connection).size());
         assertEquals(before, rows());
         DeleteInput input = new DeleteInput(TABLE).withPrimaryKeys(List.of("99", 1)).withTransaction(transaction);
         DeleteOutput output = new DeleteAction().execute(input);
         assertAll(
            () -> assertEquals(2, output.getDeletedRecordCount()),
            () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors()), JsonUtils.toJson(output)),
            () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithWarnings())),
            () -> assertEquals(survivors, rows(connection)),
            () -> assertEquals(before, rows()),
            () -> assertEquals(List.of("99", 1), input.getPrimaryKeys()),
            () -> assertCallerOwns(input, transaction, connection));
         if(commit)
         {
            transaction.commit();
         }
         else
         {
            transaction.rollback();
         }
         assertCallerOwns(input, transaction, connection);
         assertEquals(commit ? survivors : before, rows(connection));
         assertEquals(commit ? survivors : before, rows());
      }
      assertEquals(commit ? survivors : before, rows());
   }



   /*******************************************************************************
    ** The constraint is a native fixture variation, not a metadata association.
    ** A disposable direct statement proves SQLState 23503 before framework DML.
    *******************************************************************************/
   private void addNativeDeleteBlocker() throws Exception
   {
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(1, statement.executeUpdate("UPDATE field_lab SET long_value=2 WHERE id=4"));
         statement.executeUpdate("ALTER TABLE field_lab ADD CONSTRAINT sample_delete_restrict FOREIGN KEY(long_value) REFERENCES field_lab(id) ON DELETE RESTRICT");
         constraintAdded = true;
      }
      List<Map<String, String>> before = rows();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         SQLException failure = assertThrows(SQLException.class, () -> statement.executeUpdate("DELETE FROM field_lab WHERE id=2"));
         assertEquals("23503", failure.getSQLState());
      }
      assertEquals(before, rows());
   }



   /*******************************************************************************
    ** The native adapter returns per-key SystemError, not necessarily QException.
    ** Exact SQLState text ties the returned error to the proven H2 constraint.
    *******************************************************************************/
   private void assertNativeBlockedKey(DeleteOutput output)
   {
      assertAll(
         () -> assertEquals(2, output.getDeletedRecordCount()),
         () -> assertEquals(List.of(2), keys(output.getRecordsWithErrors())),
         () -> assertTrue(record(output.getRecordsWithErrors(), 2).getErrors().stream()
            .anyMatch(error -> error instanceof SystemErrorStatusMessage && error.getMessage().contains("23503")), JsonUtils.toJson(output)),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithWarnings())));
   }



   /*******************************************************************************
    ** Typed per-key categories stay distinct without assuming output row order.
    *******************************************************************************/
   private void assertMixedErrors(DeleteOutput output)
   {
      assertAll(
         () -> assertEquals(List.of(1, 999), keys(output.getRecordsWithErrors())),
         () -> assertTrue(record(output.getRecordsWithErrors(), 1).getErrors().stream()
            .anyMatch(error -> error instanceof BadInputStatusMessage && "Rejected before deletion".equals(error.getMessage()))),
         () -> assertTrue(record(output.getRecordsWithErrors(), 999).getErrors().stream().anyMatch(NotFoundStatusMessage.class::isInstance)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private DeleteInput mixedInput()
   {
      return new DeleteInput(TABLE).withPrimaryKeys(List.of(3, 1, 2, 2, 999));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertCallerOwns(DeleteInput input, RDBMSTransaction transaction, Connection connection) throws SQLException
   {
      assertSame(transaction, input.getTransaction());
      assertSame(connection, transaction.getConnection());
      assertFalse(connection.isClosed());
      assertFalse(connection.getAutoCommit());
   }



   /*******************************************************************************
    ** Sorting error/warning identities avoids imposing a public result ordering.
    *******************************************************************************/
   private static List<Integer> keys(List<QRecord> records)
   {
      return CollectionUtils.nonNullList(records).stream().map(record -> record.getValueInteger("id")).sorted().toList();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord record(List<QRecord> records, Integer key)
   {
      return CollectionUtils.nonNullList(records).stream().filter(record -> key.equals(record.getValueInteger("id"))).findFirst().orElseThrow();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Connection connection() throws Exception
   {
      return ConnectionManager.getConnection(SampleMetaDataProvider.defineRdbmsBackend());
   }



   /*******************************************************************************
    ** Close only independent connections; caller transaction reads use overload.
    *******************************************************************************/
   private List<Map<String, String>> rows() throws Exception
   {
      try(Connection connection = connection())
      {
         return rows(connection);
      }
   }



   /*******************************************************************************
    ** All canonical native columns and unrelated rows participate. BLOB is NULL.
    *******************************************************************************/
   private List<Map<String, String>> rows(Connection connection) throws SQLException
   {
      List<Map<String, String>> rows = new ArrayList<>();
      try(Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery("SELECT * FROM field_lab ORDER BY id"))
      {
         while(result.next())
         {
            Map<String, String> row = new LinkedHashMap<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               row.put(result.getMetaData().getColumnLabel(column), result.getString(column));
            }
            rows.add(row);
         }
      }
      return rows;
   }



   /*******************************************************************************
    ** Different record statuses exercise common pre-delete partitioning.
    *******************************************************************************/
   public static class MixedBeforeDelete implements TableCustomizerInterface
   {
      private static boolean sawPreview;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         sawPreview = isPreview;
         for(QRecord record : records)
         {
            if(Integer.valueOf(1).equals(record.getValueInteger("id")))
            {
               record.addError(new BadInputStatusMessage("Rejected before deletion"));
            }
            else if(Integer.valueOf(2).equals(record.getValueInteger("id")))
            {
               record.addWarning(new QWarningMessage("Reviewed before deletion"));
            }
         }
         return records;
      }
   }



   /*******************************************************************************
    ** The same registered customizer permits the later id2 recovery request.
    *******************************************************************************/
   public static class ThrowBeforeFirst implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview) throws QException
      {
         if(records.stream().anyMatch(record -> Integer.valueOf(1).equals(record.getValueInteger("id"))))
         {
            throw new QException("Owned pre-delete rejection");
         }
         return records;
      }
   }



   /*******************************************************************************
    ** Capture only the eligible keys before deliberately failing after DML.
    *******************************************************************************/
   public static class ThrowAfterDelete implements TableCustomizerInterface
   {
      private static List<Integer> seenKeys = List.of();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postDelete(DeleteInput input, List<QRecord> records) throws QException
      {
         seenKeys = keys(records);
         throw new QException("Owned post-delete failure");
      }
   }



   /*******************************************************************************
    ** These returned statuses occur after native rows have already been removed.
    *******************************************************************************/
   public static class StatusAfterDelete implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postDelete(DeleteInput input, List<QRecord> records)
      {
         for(QRecord record : records)
         {
            if(Integer.valueOf(1).equals(record.getValueInteger("id")))
            {
               record.addError(new BadInputStatusMessage("Reported after deletion"));
               record.addWarning(new QWarningMessage("Warning superseded by after-delete error"));
            }
            else if(Integer.valueOf(2).equals(record.getValueInteger("id")))
            {
               record.addWarning(new QWarningMessage("Reviewed after deletion"));
            }
         }
         return records;
      }
   }
}
