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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.model.actions.tables.QueryOrGetInputInterface;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.security.MultiRecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.statusmessages.NotFoundStatusMessage;
import com.kingsrook.qqq.backend.core.model.statusmessages.PermissionDeniedMessage;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Stored WRITE authorization uses canonical rows and independent native readback.
 *******************************************************************************/
class SampleUpdateSecurityTest
{
   private QInstance instance;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      ConnectionManager.resetConnectionProviders();
      SampleMetaDataProvider.primeTestDatabase("prime-test-database.sql");
      instance = SampleMetaDataProvider.defineTestInstance();
      instance.addSecurityKeyType(new QSecurityKeyType().withName("updateOwner"));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("updatePassword"));
      instance.addSecurityKeyType(new QSecurityKeyType().withName("updateName"));
      QContext.init(instance, new QSession().withSecurityKeyValue("updateOwner", 1L)
         .withSecurityKeyValue("updatePassword", "allowed-private-value").withSecurityKeyValue("updateName", "Name alternative"));
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         statement.executeUpdate("INSERT INTO field_lab(id,name,long_value,password_value) VALUES (101,'Forbidden',2,'forbidden-private-value'),(102,'Allowed',1,'allowed-private-value'),(103,'Null owner',NULL,NULL)");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void tearDown()
   {
      QContext.clear();
      ConnectionManager.resetConnectionProviders();
   }



   /*******************************************************************************
    ** Even a readable old owner cannot be replaced by an allowed owner.
    *******************************************************************************/
   @Test
   void testFlatWriteLockRejectsForbiddenStoredOwner() throws Exception
   {
      instance.getTable("fieldLab").withRecordSecurityLock(ownerLock());
      assertEquals(3, new QueryAction().execute(new QueryInput("fieldLab")).getRecords().size());
      assertDeniedUnchanged(new QRecord().withValue("id", 101).withValue("name", "Taken").withValue("longValue", 1L));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNestedWriteLockRejectsForbiddenStoredOwner() throws Exception
   {
      instance.getTable("fieldLab").withRecordSecurityLock(new MultiRecordSecurityLock()
         .withOperator(MultiRecordSecurityLock.BooleanOperator.AND).withLock(ownerLock()));
      assertEquals(3, new QueryAction().execute(new QueryInput("fieldLab")).getRecords().size());
      assertDeniedUnchanged(new QRecord().withValue("id", 101).withValue("name", "Taken").withValue("longValue", 1L));
   }



   /*******************************************************************************
    ** Omitted private values must not become the lock's allowed-null case.
    *******************************************************************************/
   @Test
   void testHiddenWriteLockRejectsSparseUpdate() throws Exception
   {
      instance.getTable("fieldLab").withRecordSecurityLock(ownerLock());
      instance.getTable("fieldLab").getField("longValue").setIsHidden(true);
      assertTrue(new QueryAction().execute(new QueryInput("fieldLab")).getRecords().stream().noneMatch(record -> record.getValues().containsKey("longValue")));
      assertDeniedUnchanged(new QRecord().withValue("id", 101).withValue("name", "Taken"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHiddenWriteLockRejectsAllowedReplacementOwner() throws Exception
   {
      instance.getTable("fieldLab").withRecordSecurityLock(ownerLock());
      instance.getTable("fieldLab").getField("longValue").setIsHidden(true);
      assertDeniedUnchanged(new QRecord().withValue("id", 101).withValue("name", "Taken").withValue("longValue", 1L));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHeavyWriteLockRejectsSparseUpdate() throws Exception
   {
      instance.getTable("fieldLab").withRecordSecurityLock(ownerLock());
      instance.getTable("fieldLab").getField("longValue").setIsHeavy(true);
      assertDeniedUnchanged(new QRecord().withValue("id", 101).withValue("name", "Taken"));
   }



   /*******************************************************************************
    ** A permitted secret owner stays private in the returned sparse patch.
    *******************************************************************************/
   @Test
   void testPasswordWriteLockUsesStoredValueWithoutPublishingIt() throws Exception
   {
      instance.getTable("fieldLab").withRecordSecurityLock(new RecordSecurityLock().withFieldName("passwordValue")
         .withSecurityKeyType("updatePassword").withLockScope(RecordSecurityLock.LockScope.WRITE).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.ALLOW));
      QRecord output = update(new QRecord().withValue("id", 102).withValue("name", "Allowed changed"));
      assertAll(() -> assertTrue(output.getErrors().isEmpty(), output.getErrorsAsString()),
         () -> assertFalse(output.getValues().containsKey("passwordValue")),
         () -> assertEquals(List.of(List.of("102", "Allowed changed", "1", "allowed-private-value")), rows("SELECT id,name,long_value,password_value FROM field_lab WHERE id=102")));
      assertDeniedUnchanged(new QRecord().withValue("id", 101).withValue("name", "Taken"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testHiddenAllowedAndActuallyNullOwnersRemainWritable() throws Exception
   {
      instance.getTable("fieldLab").withRecordSecurityLock(ownerLock());
      instance.getTable("fieldLab").getField("longValue").setIsHidden(true);
      for(Integer id : List.of(102, 103))
      {
         QRecord output = update(new QRecord().withValue("id", id).withValue("name", "Changed " + id));
         assertTrue(output.getErrors().isEmpty(), output.getErrorsAsString());
         assertFalse(output.getValues().containsKey("longValue"));
      }
      assertEquals(List.of(List.of("101", "Forbidden"), List.of("102", "Changed 102"), List.of("103", "Changed 103")), rows("SELECT id,name FROM field_lab ORDER BY id"));
      assertEquals(List.of(List.of("1")), rows("SELECT long_value FROM field_lab WHERE id=102"));
      assertEquals(1, rows("SELECT id FROM field_lab WHERE id=103 AND long_value IS NULL").size());
   }



   /*******************************************************************************
    ** Filtering WRITE leaves must preserve an authorizing READ_AND_WRITE OR branch.
    *******************************************************************************/
   @Test
   void testMixedWriteTreePreservesAllowedOrAlternative() throws Exception
   {
      instance.getTable("fieldLab").withRecordSecurityLock(new MultiRecordSecurityLock().withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
         .withLock(ownerLock())
         .withLock(new RecordSecurityLock().withFieldName("name").withSecurityKeyType("updateName").withLockScope(RecordSecurityLock.LockScope.READ_AND_WRITE)));
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         statement.executeUpdate("UPDATE field_lab SET name='Name alternative' WHERE id=101");
      }
      QRecord output = update(new QRecord().withValue("id", 101).withValue("name", "Name alternative").withValue("longValue", 3L));
      assertTrue(output.getErrors().isEmpty(), output.getErrorsAsString());
      assertEquals(List.of(List.of("101", "Name alternative", "3")), rows("SELECT id,name,long_value FROM field_lab WHERE id=101"));
   }



   /*******************************************************************************
    ** Private owner checks do not grant access to a row excluded by ordinary READ locks.
    *******************************************************************************/
   @Test
   void testReadDeniedRowRemainsMissingToUpdate() throws Exception
   {
      instance.getTable("fieldLab").withRecordSecurityLock(ownerLock())
         .withRecordSecurityLock(new RecordSecurityLock().withFieldName("longValue").withSecurityKeyType("updateOwner")
            .withLockScope(RecordSecurityLock.LockScope.READ));
      List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
      QRecord output = update(new QRecord().withValue("id", 101).withValue("name", "Taken").withValue("longValue", 1L));
      assertTrue(output.getErrors().stream().anyMatch(NotFoundStatusMessage.class::isInstance), output.getErrorsAsString());
      assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
      QRecord allowed = update(new QRecord().withValue("id", 102).withValue("name", "Allowed changed"));
      assertTrue(allowed.getErrors().isEmpty(), allowed.getErrorsAsString());
      assertEquals(List.of(List.of("101", "Forbidden"), List.of("102", "Allowed changed"), List.of("103", "Null owner")), rows("SELECT id,name FROM field_lab ORDER BY id"));
   }



   /*******************************************************************************
    ** Presentation changes cannot rewrite ownership; the customizer snapshot stays sanitized.
    *******************************************************************************/
   @Test
   void testPresentationOwnerDoesNotAuthorizeStoredOwner() throws Exception
   {
      instance.getTable("fieldLab").withRecordSecurityLock(ownerLock())
         .withCustomizer(TableCustomizers.POST_QUERY_RECORD, new QCodeReference(PresentationOwner.class))
         .withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(PresentationOwner.class));
      PresentationOwner.oldRecords = List.of();
      assertDeniedUnchanged(new QRecord().withValue("id", 101).withValue("name", "Taken"));
      assertEquals(1, PresentationOwner.oldRecords.size());
      QRecord snapshot = PresentationOwner.oldRecords.get(0);
      assertEquals(1L, snapshot.getValueLong("longValue"));
      assertFalse("forbidden-private-value".equals(snapshot.getValue("passwordValue")));
      PresentationOwner.oldRecords = List.of();
   }



   /*******************************************************************************
    ** Empty updates and explicit public validation remain harmless with configured locks.
    *******************************************************************************/
   @Test
   void testNullAndEmptyUpdatesDoNotReadOrMutateRows() throws Exception
   {
      instance.getTable("fieldLab").withRecordSecurityLock(ownerLock());
      List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
      UpdateInput absent = new UpdateInput("fieldLab");
      absent.setRecords(null);
      assertTrue(new UpdateAction().execute(absent).getRecords().isEmpty());
      UpdateInput empty = new UpdateInput("fieldLab").withRecords(List.of());
      assertTrue(new UpdateAction().execute(empty).getRecords().isEmpty());
      new UpdateAction().performValidations(absent, Optional.of(List.of()), false);
      new UpdateAction().performValidations(empty, Optional.of(List.of()), false);
      assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id"));
   }



   /*******************************************************************************
    ** Every failed branch rejects OR, regardless of how many leaf errors it contains.
    *******************************************************************************/
   @Test
   void testNestedOrRejectsWhenEveryBranchFails() throws Exception
   {
      installNestedOwnerTree();
      assertDeniedUnchanged(new QRecord().withValue("id", 101).withValue("longValue", 2L).withValue("name", "Taken"));
   }



   /*******************************************************************************
    ** A successful alternative wins even when its sibling produces multiple errors.
    *******************************************************************************/
   @Test
   void testNestedOrAllowsOneSuccessfulBranch() throws Exception
   {
      installNestedOwnerTree();
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         statement.executeUpdate("UPDATE field_lab SET name='Name alternative' WHERE id=101");
      }
      QRecord output = update(new QRecord().withValue("id", 101).withValue("longValue", 2L).withValue("name", "Name alternative"));
      assertTrue(output.getErrors().isEmpty(), output.getErrorsAsString());
      assertEquals(List.of(List.of("101", "Name alternative", "2")), rows("SELECT id,name,long_value FROM field_lab WHERE id=101"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void installNestedOwnerTree()
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("updateSecondOwner"));
      QContext.getQSession().withSecurityKeyValue("updateSecondOwner", 4L);
      instance.getTable("fieldLab").withRecordSecurityLock(new MultiRecordSecurityLock().withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
         .withLock(new MultiRecordSecurityLock().withOperator(MultiRecordSecurityLock.BooleanOperator.AND)
            .withLock(ownerLock()).withLock(new RecordSecurityLock().withFieldName("longValue").withSecurityKeyType("updateSecondOwner").withLockScope(RecordSecurityLock.LockScope.WRITE)))
         .withLock(new RecordSecurityLock().withFieldName("name").withSecurityKeyType("updateName").withLockScope(RecordSecurityLock.LockScope.WRITE)));
   }



   /*******************************************************************************
    ** A READ-only nested branch disappears from a WRITE tree instead of granting access.
    *******************************************************************************/
   @Test
   void testReadOnlyBranchDoesNotAuthorizeWrites() throws Exception
   {
      instance.getTable("fieldLab").withRecordSecurityLock(new MultiRecordSecurityLock().withOperator(MultiRecordSecurityLock.BooleanOperator.OR)
         .withLock(new MultiRecordSecurityLock().withOperator(MultiRecordSecurityLock.BooleanOperator.AND)
            .withLock(new RecordSecurityLock().withFieldName("name").withSecurityKeyType("updateName").withLockScope(RecordSecurityLock.LockScope.READ)))
         .withLock(ownerLock()));
      try(Connection connection = connection(); Statement statement = connection.createStatement())
      {
         statement.executeUpdate("UPDATE field_lab SET name='Name alternative' WHERE id=101");
      }
      assertEquals(1, new QueryAction().execute(new QueryInput("fieldLab")).getRecords().size());
      assertDeniedUnchanged(new QRecord().withValue("id", 101).withValue("name", "Name alternative").withValue("longValue", 2L));
   }



   /*******************************************************************************
    ** The authorization read sees the caller's uncommitted owner and leaves rollback open.
    *******************************************************************************/
   @Test
   void testPrivateOwnerAuthorizationUsesCallerTransaction() throws Exception
   {
      instance.getTable("fieldLab").withRecordSecurityLock(ownerLock());
      instance.getTable("fieldLab").getField("longValue").setIsHidden(true);
      List<List<String>> original = rows("SELECT * FROM field_lab ORDER BY id");
      try(RDBMSTransaction transaction = new RDBMSTransaction(connection()); Statement statement = transaction.getConnection().createStatement())
      {
         statement.executeUpdate("UPDATE field_lab SET long_value=2 WHERE id=102");
         List<List<String>> before = rows(transaction.getConnection(), "SELECT * FROM field_lab ORDER BY id");
         QRecord output = new UpdateAction().execute(new UpdateInput("fieldLab").withTransaction(transaction)
            .withRecord(new QRecord().withValue("id", 102).withValue("name", "Taken"))).getRecords().get(0);
         assertAll(() -> assertTrue(output.getErrors().stream().anyMatch(PermissionDeniedMessage.class::isInstance), output.getErrorsAsString()),
            () -> assertEquals(before, rows(transaction.getConnection(), "SELECT * FROM field_lab ORDER BY id")),
            () -> assertEquals(original, rows("SELECT * FROM field_lab ORDER BY id")),
            () -> assertFalse(transaction.getConnection().isClosed()));
         transaction.rollback();
      }
      assertEquals(original, rows("SELECT * FROM field_lab ORDER BY id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RecordSecurityLock ownerLock()
   {
      return new RecordSecurityLock().withFieldName("longValue").withSecurityKeyType("updateOwner")
         .withLockScope(RecordSecurityLock.LockScope.WRITE).withNullValueBehavior(RecordSecurityLock.NullValueBehavior.ALLOW);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertDeniedUnchanged(QRecord input) throws Exception
   {
      List<List<String>> before = rows("SELECT * FROM field_lab ORDER BY id");
      boolean suppliedOwner = input.getValues().containsKey("longValue");
      boolean suppliedStoredOwner = Long.valueOf(2L).equals(input.getValue("longValue"));
      QRecord output = update(input);
      assertAll(() -> assertTrue(output.getErrors().stream().anyMatch(PermissionDeniedMessage.class::isInstance), output.getErrorsAsString()),
         () -> assertEquals(before, rows("SELECT * FROM field_lab ORDER BY id")),
         () -> assertFalse(output.getValues().containsKey("passwordValue")),
         () -> assertFalse(output.getErrorsAsString().contains("forbidden-private-value")));
      if(!suppliedOwner)
      {
         assertFalse(input.getValues().containsKey("longValue"));
         assertFalse(output.getValues().containsKey("longValue"));
      }
      if(!suppliedStoredOwner)
      {
         assertFalse(output.getErrorsAsString().contains("value of 2"), output.getErrorsAsString());
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord update(QRecord input) throws Exception
   {
      return new UpdateAction().execute(new UpdateInput("fieldLab").withRecord(input)).getRecords().get(0);
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
   private List<List<String>> rows(String sql) throws Exception
   {
      try(Connection connection = connection())
      {
         return rows(connection, sql);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<List<String>> rows(Connection connection, String sql) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
      {
         while(result.next())
         {
            List<String> row = new ArrayList<>();
            for(int column = 1; column <= result.getMetaData().getColumnCount(); column++)
            {
               row.add(result.getString(column));
            }
            rows.add(row);
         }
      }
      return rows;
   }


   /*******************************************************************************
    **
    *******************************************************************************/
   public static class PresentationOwner implements TableCustomizerInterface
   {
      private static List<QRecord> oldRecords = List.of();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> postQuery(QueryOrGetInputInterface input, List<QRecord> records)
      {
         return records.stream().map(record -> new QRecord(record).withValue("longValue", 1L)).toList();
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList)
      {
         oldRecords = oldRecordList.orElse(List.of()).stream().map(QRecord::new).toList();
         return records;
      }
   }

}
