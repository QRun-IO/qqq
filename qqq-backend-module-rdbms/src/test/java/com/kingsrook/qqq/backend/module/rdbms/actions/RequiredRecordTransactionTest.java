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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.metadata.personalization.TableMetaDataPersonalizerInterface;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeEvent;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeListenerInterface;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.RecordChangeType;
import com.kingsrook.qqq.backend.core.actions.tables.listeners.TransactionalRecordChangeMetaData;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.metadata.personalization.TableMetaDataPersonalizerInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.module.rdbms.BaseTest;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.QueryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Real native H2 mutations demonstrate required-listener transaction ownership.
 ** These atomicity tests do not qualify memory H2 for durable Jobs storage.
 *******************************************************************************/
class RequiredRecordTransactionTest extends BaseTest
{
   private static final List<RecordChangeEvent> EVENTS = new ArrayList<>();
   private static boolean fail;
   private static boolean applies;
   private static boolean failApplies;
   private static boolean addCallback;
   private static boolean nestedPreview;
   private static boolean previewWrites;
   private static boolean previewChangesFlag;
   private static boolean changesTable;



   /*******************************************************************************
    ** Fresh native fixture and host-declared listener for each case.
    *******************************************************************************/
   @BeforeEach
   void prepare() throws Exception
   {
      TestUtils.primeTestDatabase("prime-test-database.sql");
      EVENTS.clear();
      fail = false;
      applies = true;
      failApplies = false;
      addCallback = false;
      nestedPreview = false;
      previewWrites = false;
      previewChangesFlag = false;
      changesTable = false;
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).withSupplementalMetaData(new TransactionalRecordChangeMetaData(
         new QCodeReference(Listener.class), Set.of(RecordChangeType.INSERT, RecordChangeType.UPDATE, RecordChangeType.DELETE)));
   }



   /*******************************************************************************
    ** Owned actions commit each complete mutation and restore exact input/context.
    *******************************************************************************/
   @Test
   void ownsInsertUpdateDeleteAndSuppressionDoesNotSkipRequiredListener() throws Exception
   {
      InsertInput input = new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person()).withOmitRecordChangeEvents(true);
      int id = new InsertAction().execute(input).getRecords().get(0).getValueInteger("id");
      assertNull(input.getTransaction());
      assertNull(QContext.getQBackendTransaction());
      assertEquals(1, count(id));
      assertTrue(EVENTS.get(0).getNotificationsSuppressed());
      new UpdateAction().execute(new UpdateInput(TestUtils.TABLE_NAME_PERSON).withRecord(new QRecord().withValue("id", id).withValue("firstName", "Updated")));
      assertEquals("Required", EVENTS.get(1).getOldRecords().get(0).getValueString("firstName"));
      new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_PERSON).withPrimaryKey(id));
      assertEquals(List.of(RecordChangeType.INSERT, RecordChangeType.UPDATE, RecordChangeType.DELETE), EVENTS.stream().map(RecordChangeEvent::getType).toList());
      assertEquals(0, count(id));
   }



   /*******************************************************************************
    ** Success on a joined transaction remains invisible until its caller commits.
    *******************************************************************************/
   @Test
   void joinedSuccessDoesNotCommitCallerTransaction() throws Exception
   {
      try(RDBMSTransaction transaction = transaction())
      {
         InsertInput input = new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person()).withTransaction(transaction);
         int id = new InsertAction().execute(input).getRecords().get(0).getValueInteger("id");
         assertSame(transaction, input.getTransaction());
         assertSame(transaction, EVENTS.get(0).getTransaction());
         assertNull(QContext.getQBackendTransaction());
         assertEquals(0, count(id));
         transaction.commit();
         assertEquals(1, count(id));
      }
   }



   /*******************************************************************************
    ** Swallowing a listener exception cannot commit its already-issued SQL.
    *******************************************************************************/
   @Test
   void requiredFailurePoisonsJoinedTransaction() throws Exception
   {
      try(RDBMSTransaction transaction = transaction())
      {
         fail = true;
         InsertInput input = new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person()).withTransaction(transaction);
         assertThrows(Exception.class, () -> new InsertAction().execute(input));
         assertTrue(transaction.isRollbackOnly());
         assertSame(transaction, input.getTransaction());
         assertNull(QContext.getQBackendTransaction());
         assertThrows(QException.class, transaction::commit);
         transaction.rollback();
         assertEquals(0, namedCount());
      }
   }



   /*******************************************************************************
    ** Listener loading and appliesTo refusal happen before business mutation.
    *******************************************************************************/
   @Test
   void missingAndNonapplyingListenersFailBeforeDml() throws Exception
   {
      applies = false;
      assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person())));
      failApplies = true;
      assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person())));
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).withSupplementalMetaData(new TransactionalRecordChangeMetaData(
         new QCodeReference(String.class), Set.of(RecordChangeType.INSERT)));
      assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person())));
      assertEquals(0, namedCount());
      assertTrue(EVENTS.isEmpty());
      assertNull(QContext.getQBackendTransaction());
   }



   /*******************************************************************************
    ** A no-op transaction and conflicting ambient connection cannot be admitted.
    *******************************************************************************/
   @Test
   void refusesNoOpAndMismatchedTransactions() throws Exception
   {
      QBackendTransaction noOp = new QBackendTransaction();
      assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person()).withTransaction(noOp)));
      assertTrue(noOp.isRollbackOnly());
      try(RDBMSTransaction explicit = transaction(); RDBMSTransaction ambient = transaction())
      {
         QContext.setTransaction(ambient);
         assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person()).withTransaction(explicit)));
         assertSame(ambient, QContext.getQBackendTransaction());
         assertTrue(explicit.isRollbackOnly());
         assertTrue(ambient.isRollbackOnly());
         explicit.rollback();
         ambient.rollback();
         QContext.clearTransaction();
      }
      assertEquals(0, namedCount());
   }



   /*******************************************************************************
    ** Preview and no selected delete records legitimately produce no event.
    *******************************************************************************/
   @Test
   void previewAndEmptyDeleteDoNotEnlist() throws Exception
   {
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person()).withDryRun(true));
      new DeleteAction().execute(new DeleteInput(TestUtils.TABLE_NAME_PERSON).withPrimaryKeys(List.of()));
      assertTrue(EVENTS.isEmpty());
      assertEquals(0, namedCount());
   }



   /*******************************************************************************
    ** Existing post-commit cleanup can perform independent native writes.
    *******************************************************************************/
   @Test
   void afterCommitCallbackStartsFreshNativeTransaction() throws Exception
   {
      addCallback = true;
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person()));
      assertEquals(2, namedCount());
      assertEquals(2, EVENTS.size());
      assertFalse(EVENTS.get(0).getTransaction() == EVENTS.get(1).getTransaction());
      assertNull(QContext.getQBackendTransaction());
   }



   /*******************************************************************************
    ** Failed source mutations never dispatch their post-commit callback.
    *******************************************************************************/
   @Test
   void failedRequiredListenerDiscardsCallback() throws Exception
   {
      addCallback = true;
      fail = true;
      assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person())));
      assertEquals(0, namedCount());
      assertEquals(1, EVENTS.size());
   }



   /*******************************************************************************
    ** Caller metadata cannot strip the authoritative obligation or redirect DML.
    *******************************************************************************/
   @Test
   void refusesCallerPhysicalTableRedirection() throws Exception
   {
      var redirected = TestUtils.defineTablePerson();
      redirected.setBackendDetails(new com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails().withTableName("other_person"));
      InsertInput input = new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person());
      input.setTableMetaData(redirected);
      assertThrows(QException.class, () -> new InsertAction().execute(input));
      assertTrue(EVENTS.isEmpty());
      assertEquals(0, namedCount());
   }



   /*******************************************************************************
    ** A linked H2 table must not write through an independent remote connection.
    *******************************************************************************/
   @Test
   void refusesLinkedTableBeforeRemoteWrites() throws Exception
   {
      try(Connection remote = DriverManager.getConnection("jdbc:h2:mem:required_remote", "sa", "");
         Connection local = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         QueryManager.executeUpdate(remote, "CREATE TABLE remote_person (id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, first_name VARCHAR(100))");
         QueryManager.executeUpdate(local, "CREATE LINKED TABLE linked_person('org.h2.Driver','jdbc:h2:mem:required_remote','sa','','REMOTE_PERSON')");
         try
         {
            QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).setBackendDetails(
               new com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails().withTableName("linked_person"));
            assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person())));
            assertEquals(0, QueryManager.executeStatementForSingleValue(remote, Integer.class, "SELECT COUNT(*) FROM remote_person"));
            assertTrue(EVENTS.isEmpty());
         }
         finally
         {
            QueryManager.executeUpdate(local, "DROP TABLE linked_person");
         }
      }
   }



   /*******************************************************************************
    ** A nested native preview can read its parent's transaction without writing.
    *******************************************************************************/
   @Test
   void nestedPreviewDoesNotPoisonSuccessfulParent() throws Exception
   {
      nestedPreview = true;
      new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person()));
      assertEquals(1, namedCount());
      assertEquals(1, EVENTS.size());
   }



   /*******************************************************************************
    ** Catching real DML attempted inside a strict preview cannot save its parent.
    *******************************************************************************/
   @Test
   void nestedPreviewCannotHideRealWrite() throws Exception
   {
      nestedPreview = true;
      previewWrites = true;
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(Probe.class));
      assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person())));
      assertEquals(0, namedCount());
   }



   /*******************************************************************************
    ** Host validation cannot switch a nested preview into an actual insert.
    *******************************************************************************/
   @Test
   void nestedPreviewCannotClearItsDryRunFlag() throws Exception
   {
      nestedPreview = true;
      previewChangesFlag = true;
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(Probe.class));
      assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person())));
      assertEquals(0, namedCount());
   }



   /*******************************************************************************
    ** A caught permission refusal preceding scope creation poisons earlier SQL.
    *******************************************************************************/
   @Test
   void earlyPermissionRefusalPreventsCallerCommit() throws Exception
   {
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      try(RDBMSTransaction transaction = transaction())
      {
         QueryManager.executeUpdate(transaction.getConnection(), "INSERT INTO person (first_name,last_name,email) VALUES ('Required','Earlier','fixture@example.invalid')");
         InsertInput input = new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person()).withTransaction(transaction);
         input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         assertThrows(QException.class, () -> new InsertAction().execute(input));
         assertTrue(transaction.isRollbackOnly());
         assertThrows(QException.class, transaction::commit);
         transaction.rollback();
         assertEquals(0, namedCount());
      }
   }



   /*******************************************************************************
    ** Standalone preview permission refusal leaves its caller's transaction alone.
    *******************************************************************************/
   @Test
   void standalonePreviewRefusalDoesNotPoisonCaller() throws Exception
   {
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).setPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
      try(RDBMSTransaction transaction = transaction())
      {
         InsertInput input = new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person()).withTransaction(transaction).withDryRun(true);
         input.setPermissionEnforcement(PermissionEnforcement.ENFORCE);
         assertThrows(QException.class, () -> new InsertAction().execute(input));
         assertFalse(transaction.isRollbackOnly());
      }
   }



   /*******************************************************************************
    ** A standalone required preview cannot clear its flag and bypass the listener.
    *******************************************************************************/
   @Test
   void standalonePreviewCannotBecomeWriteWithOrWithoutCallerTransaction() throws Exception
   {
      previewChangesFlag = true;
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(Probe.class));
      assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person()).withDryRun(true)));
      try(RDBMSTransaction transaction = transaction())
      {
         assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person()).withDryRun(true).withTransaction(transaction)));
         assertTrue(transaction.isRollbackOnly());
         assertThrows(QException.class, transaction::commit);
         transaction.rollback();
      }
      assertEquals(0, namedCount());
      assertTrue(EVENTS.isEmpty());
      assertNull(QContext.getQBackendTransaction());
   }



   /*******************************************************************************
    ** A personalizer redirect is requalified before any native write.
    *******************************************************************************/
   @Test
   void refusesHostPersonalizerMappingDrift() throws Exception
   {
      QContext.getQInstance().addSupplementalCustomizer(TableMetaDataPersonalizerInterface.CUSTOMIZER_TYPE, new QCodeReference(Redirect.class));
      assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person())));
      assertTrue(EVENTS.isEmpty());
      assertEquals(0, namedCount());
   }



   /*******************************************************************************
    ** The admitted logical source cannot change after listener admission.
    *******************************************************************************/
   @Test
   void refusesSourceNameDriftDuringValidation() throws Exception
   {
      changesTable = true;
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).withCustomizer(TableCustomizers.PRE_INSERT_RECORD, new QCodeReference(Probe.class));
      assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person())));
      assertEquals(0, namedCount());
      assertTrue(EVENTS.isEmpty());
   }



   /*******************************************************************************
    ** A mixed batch emits only its actual successful native records.
    *******************************************************************************/
   @Test
   void partialBatchNeverEnlistsRejectedRecords() throws Exception
   {
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_PERSON).getField("lastName").setIsRequired(true);
      var output = new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecords(List.of(person(), new QRecord().withValue("firstName", "Rejected"))));
      assertEquals(2, output.getRecords().size());
      assertEquals(1, EVENTS.size());
      assertEquals(1, EVENTS.get(0).getRecords().size());
      assertEquals("Required", EVENTS.get(0).getRecords().get(0).getValueString("firstName"));
      assertEquals(1, namedCount());
   }



   /*******************************************************************************
    ** Parent and nested association writes roll back together on required failure.
    *******************************************************************************/
   @Test
   void requiredParentFailureRollsBackAssociations() throws Exception
   {
      QContext.getQSession().withSecurityKeyValue(TestUtils.TABLE_NAME_STORE, 1);
      int orders = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER).size();
      int lines = TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER_LINE).size();
      QContext.getQInstance().getTable(TestUtils.TABLE_NAME_ORDER).withSupplementalMetaData(new TransactionalRecordChangeMetaData(
         new QCodeReference(Listener.class), Set.of(RecordChangeType.INSERT)));
      fail = true;
      QRecord order = new QRecord().withValue("storeId", 1).withValue("billToPersonId", 100).withValue("shipToPersonId", 200)
         .withAssociatedRecord("orderLine", new QRecord().withValue("storeId", 1).withValue("sku", "BASIC1").withValue("quantity", 1));
      assertThrows(Exception.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_ORDER).withRecord(order)));
      assertEquals(orders, TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER).size());
      assertEquals(lines, TestUtils.queryTable(TestUtils.TABLE_NAME_ORDER_LINE).size());
      assertEquals(1, EVENTS.size());
   }



   /*******************************************************************************
    ** Trusted fixture models an erroneous host metadata personalizer.
    *******************************************************************************/
   public static class Redirect implements TableMetaDataPersonalizerInterface
   {
      /*******************************************************************************
       ** Return a clone aimed at a different physical table.
       *******************************************************************************/
      @Override
      public QTableMetaData execute(TableMetaDataPersonalizerInput input)
      {
         QTableMetaData table = input.getTable().clone();
         table.setBackendDetails(new com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails().withTableName("other_person"));
         return (table);
      }
   }



   /*******************************************************************************
    ** Native validation fixture for nested preview and source-identity tests.
    *******************************************************************************/
   public static class Probe implements TableCustomizerInterface
   {
      /*******************************************************************************
       ** Attempt forbidden writes or target drift only when requested by a test.
       *******************************************************************************/
      @Override
      public List<QRecord> preInsert(InsertInput input, List<QRecord> records, boolean isPreview) throws QException
      {
         if(isPreview && previewWrites)
         {
            assertThrows(QException.class, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person())));
         }
         if(isPreview && previewChangesFlag)
         {
            input.setDryRun(false);
         }
         if(changesTable)
         {
            input.setTableName(TestUtils.TABLE_NAME_PERSONAL_ID_CARD);
         }
         return (records);
      }
   }



   /*******************************************************************************
    ** Host-declared fixture listener only records native event identity.
    *******************************************************************************/
   public static class Listener implements RecordChangeListenerInterface
   {
      /*******************************************************************************
       ** Admission refuses host errors as well as a false appliesTo result.
       *******************************************************************************/
      @Override
      public boolean appliesTo(String tableName, RecordChangeType type)
      {
         if(failApplies)
         {
            throw (new IllegalStateException("fixture appliesTo refusal"));
         }
         return (applies);
      }



      /*******************************************************************************
       ** Verify the actual transaction is propagated and inject required failure.
       *******************************************************************************/
      @Override
      public void onRecordsChanged(RecordChangeEvent event) throws QException
      {
         assertSame(QContext.getQBackendTransaction(), event.getTransaction());
         assertFalse(event.getRecords().isEmpty());
         EVENTS.add(event);
         if(nestedPreview)
         {
            nestedPreview = false;
            try
            {
               new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person()).withDryRun(true));
            }
            catch(QException error)
            {
               if(!previewWrites && !previewChangesFlag)
               {
                  throw (error);
               }
            }
         }
         if(addCallback)
         {
            addCallback = false;
            CapturedContext captured = new CapturedContext(QContext.getQInstance(), QContext.getQSession());
            event.getTransaction().addAfterCommitCallback(() ->
            {
               try
               {
                  QContext.withTemporaryContext(captured, () -> new InsertAction().execute(new InsertInput(TestUtils.TABLE_NAME_PERSON).withRecord(person())));
               }
               catch(QException error)
               {
                  throw (new IllegalStateException(error));
               }
            });
         }
         if(fail)
         {
            throw (new QException("fixture required listener refusal"));
         }
      }
   }



   /*******************************************************************************
    ** Native writable connection, owned by the individual test.
    *******************************************************************************/
   private static RDBMSTransaction transaction() throws Exception
   {
      return (new RDBMSTransaction(ConnectionManager.getConnection(TestUtils.defineBackend())));
   }



   /*******************************************************************************
    ** Bounded synthetic business record.
    *******************************************************************************/
   private static QRecord person()
   {
      return (new QRecord().withValue("firstName", "Required").withValue("lastName", "Listener").withValue("email", "fixture@example.invalid"));
   }



   /*******************************************************************************
    ** Query through a separate connection to observe only committed writes.
    *******************************************************************************/
   private static int count(int id) throws Exception
   {
      try(Connection observer = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         return (QueryManager.executeStatementForSingleValue(observer, Integer.class, "SELECT COUNT(*) FROM person WHERE id=?", id));
      }
   }



   /*******************************************************************************
    ** Confirm admission refusals did not leave synthetic business rows behind.
    *******************************************************************************/
   private static int namedCount() throws Exception
   {
      try(Connection observer = ConnectionManager.getConnection(TestUtils.defineBackend()))
      {
         return (QueryManager.executeStatementForSingleValue(observer, Integer.class, "SELECT COUNT(*) FROM person WHERE first_name='Required'"));
      }
   }
}
