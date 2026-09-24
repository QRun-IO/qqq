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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.PermissionDeniedMessage;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Stored Delete authorization and public snapshots use canonical native rows.
 *******************************************************************************/
class SampleDeleteSecurityTest
{
   private static final String TABLE = "fieldLab";
   private static final String PRIVATE_TEXT = "delete-user-private-sentinel";
   private QInstance instance;



   /*******************************************************************************
    ** Native seeding keeps Insert out of the Delete authorization oracle.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.addSecurityKeyType(new QSecurityKeyType().withName("deleteOwner"));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("deletePassword"));
      QContext.init(instance, new QSession().withSecurityKeyValue("deleteOwner", 1L)
         .withSecurityKeyValue("deletePassword", "allowed-delete-password"));
      CaptureDelete.records = List.of();
      ImmutableDelete.sawPreview = false;
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         assertEquals(3, statement.executeUpdate("INSERT INTO field_lab(id,name,long_value,password_value,text_value) VALUES "
            + "(101,'Forbidden',2,'forbidden-delete-password','" + PRIVATE_TEXT + "'),"
            + "(102,'Allowed',1,'allowed-delete-password','Allowed text'),(103,'Null owner',NULL,NULL,'Null text')"));
      }
   }



   /*******************************************************************************
    ** Static customizer captures and native/context state belong to this fixture.
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      CaptureDelete.records = List.of();
      ImmutableDelete.sawPreview = false;
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** Ordinary visible stored ownership still denies a forbidden Delete.
    *******************************************************************************/
   @Test
   void testVisibleStoredOwnerDeniesDelete() throws Exception
   {
      instance.getTable(TABLE).withRecordSecurityLock(ownerLock());
      assertDeniedUnchanged(input(101));
   }



   /*******************************************************************************
    ** Hidden presentation values cannot turn a stored owner into allowed null.
    *******************************************************************************/
   @Test
   void testHiddenStoredOwnerDeniesDelete() throws Exception
   {
      instance.getTable(TABLE).withRecordSecurityLock(ownerLock());
      instance.getTable(TABLE).getField("longValue").setIsHidden(true);
      assertOwnerNotPublished(assertDeniedUnchanged(input(101)));
   }



   /*******************************************************************************
    ** A heavy-field length is not the stored owner used for authorization.
    *******************************************************************************/
   @Test
   void testHeavyStoredOwnerDeniesDelete() throws Exception
   {
      instance.getTable(TABLE).withRecordSecurityLock(ownerLock());
      instance.getTable(TABLE).getField("longValue").setIsHeavy(true);
      assertOwnerNotPublished(assertDeniedUnchanged(input(101)));
   }



   /*******************************************************************************
    ** Private owner retrieval must retain allowed values and real null policy.
    *******************************************************************************/
   @Test
   void testHiddenOwnerAllowsOwnedAndTrueNullRecords() throws Exception
   {
      assertPrivateOwnerAllowsOwnedAndTrueNullRecords(false);
   }



   /*******************************************************************************
    ** Heavy-field retrieval must retain allowed values and real null policy.
    *******************************************************************************/
   @Test
   void testHeavyOwnerAllowsOwnedAndTrueNullRecords() throws Exception
   {
      assertPrivateOwnerAllowsOwnedAndTrueNullRecords(true);
   }



   /*******************************************************************************
    ** Both presentation omissions leave the same stored-value authorization.
    *******************************************************************************/
   private void assertPrivateOwnerAllowsOwnedAndTrueNullRecords(boolean heavy) throws Exception
   {
      instance.getTable(TABLE).withRecordSecurityLock(ownerLock());
      instance.getTable(TABLE).getField("longValue").setIsHeavy(heavy);
      instance.getTable(TABLE).getField("longValue").setIsHidden(!heavy);
      List<Map<String, String>> before = rows();
      DeleteOutput output = new DeleteAction().execute(input(102).withPrimaryKey(103));
      assertAll(() -> assertEquals(List.of(before.get(0)), rows()),
         () -> assertEquals(2, output.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors()), JsonUtils.toJson(output)));
   }



   /*******************************************************************************
    ** Password masking cannot deny an owner whose stored key is allowed.
    *******************************************************************************/
   @Test
   void testMaskedPasswordAllowsStoredOwner() throws Exception
   {
      instance.getTable(TABLE).withRecordSecurityLock(passwordLock());
      List<Map<String, String>> before = rows();
      DeleteOutput output = new DeleteAction().execute(input(102));
      assertAll(() -> assertEquals(List.of(before.get(0), before.get(2)), rows()),
         () -> assertEquals(1, output.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors()), JsonUtils.toJson(output)),
         () -> assertFalse(JsonUtils.toJson(output).contains("allowed-delete-password")));
   }



   /*******************************************************************************
    ** A forbidden stored password cannot leak through a denial message or value.
    *******************************************************************************/
   @Test
   void testMaskedPasswordDeniesForbiddenOwner() throws Exception
   {
      instance.getTable(TABLE).withRecordSecurityLock(passwordLock());
      assertDeniedUnchanged(input(101));
   }



   /*******************************************************************************
    ** Presentation ownership stays in the callback snapshot, not authorization.
    *******************************************************************************/
   @Test
   void testPresentationOwnerCannotAuthorizeStoredOwner() throws Exception
   {
      instance.getTable(TABLE).withRecordSecurityLock(ownerLock())
         .withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(PresentationOwner.class))
         .withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(CaptureDelete.class));
      assertDeniedUnchanged(input(101));
      assertEquals(1, CaptureDelete.records.size());
      assertEquals(1L, CaptureDelete.records.get(0).getValueLong("longValue"));
      assertFalse(JsonUtils.toJson(CaptureDelete.records).contains("forbidden-delete-password"));
   }



   /*******************************************************************************
    ** Private stored reads must not make an ordinary READ-denied record visible.
    *******************************************************************************/
   @Test
   void testReadDeniedRecordRemainsNotFound() throws Exception
   {
      instance.getTable(TABLE).withRecordSecurityLock(ownerLock())
         .withRecordSecurityLock(new RecordSecurityLock().withFieldName("longValue").withSecurityKeyType("deleteOwner")
            .withLockScope(RecordSecurityLock.LockScope.READ));
      List<Map<String, String>> before = rows();
      DeleteInput input = input(101);
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(() -> assertEquals(before, rows()),
         () -> assertEquals(0, output.getDeletedRecordCount()),
         () -> assertEquals(List.of(101), input.getPrimaryKeys()),
         () -> assertTrue(CollectionUtils.nonNullList(output.getRecordsWithErrors()).stream()
            .anyMatch(record -> record.getErrors().stream().anyMatch(NotFoundStatusMessage.class::isInstance)), JsonUtils.toJson(output)),
         () -> assertFalse(JsonUtils.toJson(output).contains(PRIVATE_TEXT)));
      DeleteOutput allowed = new DeleteAction().execute(input(102));
      assertEquals(1, allowed.getDeletedRecordCount());
      assertTrue(CollectionUtils.nullSafeIsEmpty(allowed.getRecordsWithErrors()), JsonUtils.toJson(allowed));
      assertEquals(List.of(before.get(0), before.get(2)), rows());
   }



   /*******************************************************************************
    ** Stored authorization sees pending owners and leaves rollback to the caller.
    *******************************************************************************/
   @Test
   void testCallerTransactionUsesUncommittedOwnerAndRetainsRollback() throws Exception
   {
      instance.getTable(TABLE).withRecordSecurityLock(ownerLock());
      instance.getTable(TABLE).getField("longValue").setIsHidden(true);
      List<Map<String, String>> original = rows();
      try(RDBMSTransaction transaction = new RDBMSTransaction(connection()))
      {
         Connection connection = transaction.getConnection();
         try(Statement statement = connection.createStatement())
         {
            assertEquals(1, statement.executeUpdate("UPDATE field_lab SET long_value=2 WHERE id=102"));
         }
         List<Map<String, String>> pending = rows(connection);
         DeleteInput input = input(102).withTransaction(transaction);
         DeleteOutput output = new DeleteAction().execute(input);
         assertAll(() -> assertEquals(pending, rows(connection)),
            () -> assertEquals(original, rows()),
            () -> assertEquals(0, output.getDeletedRecordCount()),
            () -> assertTrue(hasPermissionError(output), JsonUtils.toJson(output)),
            () -> assertSame(transaction, input.getTransaction()),
            () -> assertSame(connection, transaction.getConnection()),
            () -> assertFalse(connection.isClosed()),
            () -> assertFalse(connection.getAutoCommit()),
            () -> assertEquals(List.of(102), input.getPrimaryKeys()));
         assertOwnerNotPublished(output);
         transaction.rollback();
         assertFalse(connection.isClosed());
         assertEquals(original, rows(connection));
      }
      assertEquals(original, rows());
   }



   /*******************************************************************************
    ** USER-only removed fields stay absent from denied output and callback data.
    *******************************************************************************/
   @Test
   void testUserRemovedFieldStaysPrivateInDenialAndPreDeleteSnapshot() throws Exception
   {
      configurePrivateText();
      QTableMetaData canonical = instance.getTable(TABLE);
      List<String> originalFields = new ArrayList<>(canonical.getFields().keySet());
      DeleteOutput output = assertDeniedUnchanged(input(101).withInputSource(QInputSource.USER));
      assertAll(() -> assertFalse(JsonUtils.toJson(output).contains(PRIVATE_TEXT)),
         () -> assertTrue(output.getRecordsWithErrors().stream().noneMatch(record -> record.getValues().containsKey("textValue"))),
         () -> assertEquals(1, CaptureDelete.records.size()),
         () -> assertFalse(JsonUtils.toJson(CaptureDelete.records).contains(PRIVATE_TEXT)),
         () -> assertTrue(CaptureDelete.records.stream().noneMatch(record -> record.getValues().containsKey("textValue"))),
         () -> assertSame(canonical, instance.getTable(TABLE)),
         () -> assertEquals(originalFields, new ArrayList<>(canonical.getFields().keySet())));
   }



   /*******************************************************************************
    ** SYSTEM retains its declared fields while the same stored owner still denies.
    *******************************************************************************/
   @Test
   void testSystemSnapshotRetainsFieldRemovedOnlyForUser() throws Exception
   {
      configurePrivateText();
      DeleteOutput output = assertDeniedUnchanged(input(101).withInputSource(QInputSource.SYSTEM));
      assertEquals(1, CaptureDelete.records.size());
      assertEquals(PRIVATE_TEXT, CaptureDelete.records.get(0).getValueString("textValue"));
      assertTrue(output.getRecordsWithErrors().stream().anyMatch(record -> PRIVATE_TEXT.equals(record.getValueString("textValue"))));
   }



   /*******************************************************************************
    ** Replacement lists need not be mutable for an ordinary successful Delete.
    *******************************************************************************/
   @Test
   void testImmutablePreDeleteReplacementPreservesWarningAndDeletes() throws Exception
   {
      instance.getTable(TABLE).withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(ImmutableDelete.class));
      List<Map<String, String>> before = rows();
      DeleteInput input = input(102);
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(() -> assertEquals(List.of(before.get(0), before.get(2)), rows()),
         () -> assertEquals(1, output.getDeletedRecordCount()),
         () -> assertTrue(CollectionUtils.nullSafeIsEmpty(output.getRecordsWithErrors()), JsonUtils.toJson(output)),
         () -> assertEquals(List.of("Reviewed delete warning"), output.getRecordsWithWarnings().get(0).getWarnings().stream().map(QWarningMessage::getMessage).toList()),
         () -> assertEquals(List.of(102), input.getPrimaryKeys()),
         () -> assertFalse(ImmutableDelete.sawPreview));
   }



   /*******************************************************************************
    ** Preview can append a missing-key result without changing the supplied list.
    *******************************************************************************/
   @Test
   void testPublicPreviewAcceptsImmutableRecordsAndMissingKey() throws Exception
   {
      instance.getTable(TABLE).withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(ImmutableDelete.class));
      List<Map<String, String>> before = rows();
      List<QRecord> provided = List.of(new QRecord().withValue("id", 102).withValue("name", "Allowed"));
      String originalRecords = JsonUtils.toJson(provided);
      DeleteInput input = input(102).withPrimaryKey(999);
      List<QRecord> output = new DeleteAction().performValidations(input, Optional.of(provided), true);
      assertAll(() -> assertEquals(before, rows()),
         () -> assertEquals(originalRecords, JsonUtils.toJson(provided)),
         () -> assertEquals(List.of(102, 999), input.getPrimaryKeys()),
         () -> assertEquals(2, output.size()),
         () -> assertTrue(output.stream().anyMatch(record -> Integer.valueOf(999).equals(record.getValueInteger("id"))
            && record.getErrors().stream().anyMatch(NotFoundStatusMessage.class::isInstance))),
         () -> assertTrue(output.stream().anyMatch(record -> Integer.valueOf(102).equals(record.getValueInteger("id"))
            && record.getErrors().isEmpty() && record.getWarnings().stream().anyMatch(warning -> "Reviewed delete warning".equals(warning.getMessage())))),
         () -> assertTrue(ImmutableDelete.sawPreview));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void configurePrivateText()
   {
      instance.getTable(TABLE).withRecordSecurityLock(ownerLock())
         .withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(CaptureDelete.class));
      instance.addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(PrivateText.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RecordSecurityLock ownerLock()
   {
      return new RecordSecurityLock().withFieldName("longValue").withSecurityKeyType("deleteOwner")
         .withLockScope(RecordSecurityLock.LockScope.WRITE).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.ALLOW);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RecordSecurityLock passwordLock()
   {
      return new RecordSecurityLock().withFieldName("passwordValue").withSecurityKeyType("deletePassword")
         .withLockScope(RecordSecurityLock.LockScope.WRITE).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.ALLOW);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private DeleteInput input(int primaryKey)
   {
      return new DeleteInput(TABLE).withPrimaryKey(primaryKey);
   }



   /*******************************************************************************
    ** SQL preservation is asserted before inspecting returned denial details.
    *******************************************************************************/
   private DeleteOutput assertDeniedUnchanged(DeleteInput input) throws Exception
   {
      List<Map<String, String>> before = rows();
      List<?> originalKeys = new ArrayList<>(input.getPrimaryKeys());
      DeleteOutput output = new DeleteAction().execute(input);
      assertAll(() -> assertEquals(before, rows()),
         () -> assertEquals(0, output.getDeletedRecordCount()),
         () -> assertTrue(hasPermissionError(output), JsonUtils.toJson(output)),
         () -> assertEquals(originalKeys, input.getPrimaryKeys()),
         () -> assertFalse(JsonUtils.toJson(output).contains("forbidden-delete-password")));
      return output;
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private boolean hasPermissionError(DeleteOutput output)
   {
      return CollectionUtils.nonNullList(output.getRecordsWithErrors()).stream()
         .anyMatch(record -> record.getErrors().stream().anyMatch(PermissionDeniedMessage.class::isInstance));
   }



   /*******************************************************************************
    ** Private owner values must not be copied into error records or messages.
    *******************************************************************************/
   private void assertOwnerNotPublished(DeleteOutput output)
   {
      assertTrue(CollectionUtils.nonNullList(output.getRecordsWithErrors()).stream().noneMatch(record -> record.getValues().containsKey("longValue")));
      assertFalse(JsonUtils.toJson(output).contains("value of 2"));
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
    ** All native columns and unrelated rows are included; BLOB is SQL NULL here.
    *******************************************************************************/
   private List<Map<String, String>> rows(Connection connection) throws Exception
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
    ** A snapshot records exactly the public records received by PRE_DELETE.
    *******************************************************************************/
   public static class CaptureDelete implements TableCustomizerInterface
   {
      private static List<QRecord> records = List.of();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         CaptureDelete.records = records.stream().map(QRecord::new).toList();
         return records;
      }
   }



   /*******************************************************************************
    ** Query presentation can deliberately replace ownership in a returned clone.
    *******************************************************************************/
   public static class PresentationOwner implements TableCustomizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         return records.stream().map(record -> new QRecord(record).withValue("longValue", 1L)).toList();
      }
   }



   /*******************************************************************************
    ** This existing public customizer API does not require a mutable result list.
    *******************************************************************************/
   public static class ImmutableDelete implements TableCustomizerInterface
   {
      private static boolean sawPreview;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview)
      {
         sawPreview = isPreview;
         return records.stream().map(record -> new QRecord(record).withWarning(new QWarningMessage("Reviewed delete warning"))).toList();
      }
   }



   /*******************************************************************************
    ** Only USER metadata loses the private field; canonical metadata is untouched.
    *******************************************************************************/
   public static class PrivateText implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         if(!TABLE.equals(input.getTableName()) || !QInputSource.USER.equals(input.getInputSource()))
         {
            return input.getTable();
         }
         QTableMetaData table = input.getTable().clone();
         table.getFields().remove("textValue");
         return table;
      }
   }
}
