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


import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.processes.implementations.mergeduplicates.AbstractMergeDuplicatesTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.mergeduplicates.MergeDuplicatesLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.mergeduplicates.MergeDuplicatesProcess;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.module.rdbms.actions.RDBMSTransaction;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Application-selected Person merges through the registered framework process,
 ** with independent H2 observations of Person, Pet and nested Pet Note records.
 *******************************************************************************/
class SampleMergeAcceptanceTest
{
   private static final String PROCESS = "mergeSamplePeople";
   private static final String CLEANUP_PROCESS = "mergeWithCleanupFailures";
   private CapturedContext previousContext;
   private Map<String, Serializable> previousObjects;
   private final List<UUID> processIds = new ArrayList<>();
   private QInstance instance;
   private QSession session;
   private Connection anchor;
   private String jdbcUrl;



   /*******************************************************************************
    ** Own the database and metadata; no sample server or shared broker is started.
    *******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousContext = QContext.capture();
      previousObjects = QContext.getObjects();
      QContext.setObjects(new LinkedHashMap<>());
      ConnectionManager.resetConnectionProviders();
      jdbcUrl = "jdbc:h2:mem:merge_" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=0";
      anchor = DriverManager.getConnection(jdbcUrl, "sa", "");
      try(InputStreamReader reader = new InputStreamReader(SampleMetaDataProvider.class.getResourceAsStream("/prime-test-database.sql"), StandardCharsets.UTF_8))
      {
         RunScript.execute(anchor, reader);
      }
      instance = SampleMetaDataProvider.defineTestInstance();
      RDBMSBackendMetaData backend = SampleMetaDataProvider.defineRdbmsBackend().withName("mergeDatabase").withJdbcUrl(jdbcUrl);
      instance.addBackend(backend);
      for(String table : List.of("person", "pet", "petNote"))
      {
         instance.getTable(table).setBackendName(backend.getName());
      }
      instance.addProcess(MergeDuplicatesProcess.processMetaDataBuilder().withName(PROCESS).withTableName("person")
         .withMergeDuplicatesTransformStepClass(MergePeople.class)
         .withFields(List.of(new QFieldMetaData("survivorId", QFieldType.INTEGER), new QFieldMetaData("mergedFirstName", QFieldType.STRING)))
         .getProcessMetaData());
      session = new QSession();
      QContext.init(instance, session);
      sql("UPDATE person SET email='duplicate@example.invalid', days_worked=id*10 WHERE id IN (1,2)");
   }



   /*******************************************************************************
    ** Remove only our process state, shut down owned H2, and restore caller context.
    *******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         for(UUID id : processIds)
         {
            RunProcessAction.getStateProvider().remove(new UUIDAndTypeStateKey(id, StateType.PROCESS_STATUS));
         }
      }
      finally
      {
         try
         {
            if(anchor != null)
            {
               try(Statement statement = anchor.createStatement())
               {
                  statement.execute("SHUTDOWN");
               }
               finally
               {
                  anchor.close();
               }
            }
         }
         finally
         {
            ConnectionManager.resetConnectionProviders();
            QContext.clear();
            QContext.init(previousContext);
            QContext.setObjects(previousObjects);
         }
      }
   }



   /*******************************************************************************
    ** Selecting one Person discovers its duplicate; the chosen survivor receives
    ** merged values and all pets, without changing unrelated people or pet notes.
    *******************************************************************************/
   @Test
   void selectedDuplicateMergesFieldsAndPreservesAssociations() throws Exception
   {
      List<List<String>> unrelated = rows("SELECT * FROM person WHERE id>=3 ORDER BY id");
      List<List<String>> notes = rows("SELECT * FROM pet_note ORDER BY id");
      RunProcessOutput output = run(input("1", 1));
      assertTrue(output.getException().isEmpty(), () -> output.getException().toString());
      assertEquals(List.of(List.of("1", "Merged", "30")), rows("SELECT id,first_name,days_worked FROM person WHERE id IN (1,2) ORDER BY id"));
      assertAll(
         () -> assertEquals(List.of(List.of("1", "1"), List.of("2", "1"), List.of("3", "1"), List.of("4", "1"), List.of("5", "1"), List.of("6", "3")), rows("SELECT id,person_id FROM pet ORDER BY id")),
         () -> assertEquals(notes, rows("SELECT * FROM pet_note ORDER BY id")));
      assertEquals(unrelated, rows("SELECT * FROM person WHERE id>=3 ORDER BY id"));
   }



   /*******************************************************************************
    ** The caller may keep the second record, including the first record's nested notes.
    *******************************************************************************/
   @Test
   void choosingSecondSurvivorPreservesAllPetsAndGrandchildren() throws Exception
   {
      List<List<String>> notes = rows("SELECT * FROM pet_note ORDER BY id");
      List<List<String>> unrelated = rows("SELECT * FROM person WHERE id>=3 ORDER BY id");
      run(input("1,2", 2));
      assertEquals(List.of(List.of("2", "Merged", "30")), rows("SELECT id,first_name,days_worked FROM person WHERE id IN (1,2) ORDER BY id"));
      assertEquals(List.of(List.of("1", "2"), List.of("2", "2"), List.of("3", "2"), List.of("4", "2"), List.of("5", "2"), List.of("6", "3")), rows("SELECT id,person_id FROM pet ORDER BY id"));
      assertEquals(notes, rows("SELECT * FROM pet_note ORDER BY id"));
      assertEquals(unrelated, rows("SELECT * FROM person WHERE id>=3 ORDER BY id"));
   }



   /*******************************************************************************
    ** Preview is read-only; the same registered process resumes after review.
    *******************************************************************************/
   @Test
   void previewDoesNotWriteAndConfirmationPersistsMerge() throws Exception
   {
      Map<String, List<List<String>>> before = snapshot();
      RunProcessInput input = input("1", 1);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.BREAK);
      RunProcessOutput preview = run(input);
      assertEquals("review", preview.getProcessState().getNextStepName().orElseThrow());
      assertEquals(before, snapshot());
      input.setStartAfterStep("review");
      run(input);
      assertEquals(List.of(List.of("1", "Merged", "30")), rows("SELECT id,first_name,days_worked FROM person WHERE id IN (1,2) ORDER BY id"));
      assertEquals(List.of(List.of("5", "1")), rows("SELECT id,person_id FROM pet WHERE id=5"));
      assertEquals(before.get("pet_note"), rows("SELECT * FROM pet_note ORDER BY id"));
   }



   /*******************************************************************************
    ** A duplicate without children permits a field-only merge and repeat is a no-op.
    *******************************************************************************/
   @Test
   void selectedSurvivorAndFieldsPersistWithoutAssociationMoves() throws Exception
   {
      sql("DELETE FROM pet_note WHERE pet_id=5");
      sql("DELETE FROM pet WHERE id=5");
      List<List<String>> pets = rows("SELECT * FROM pet ORDER BY id");
      RunProcessOutput output = run(input("2", 1));
      assertTrue(output.getException().isEmpty());
      assertEquals(List.of(List.of("1", "Merged", "30")), rows("SELECT id,first_name,days_worked FROM person WHERE id IN (1,2) ORDER BY id"));
      assertEquals(pets, rows("SELECT * FROM pet ORDER BY id"));
      Map<String, List<List<String>>> merged = snapshot();
      run(input("1", 1));
      assertEquals(merged, snapshot());
   }



   /*******************************************************************************
    ** Distinct keys and a survivor outside the discovered group are not merged.
    *******************************************************************************/
   @Test
   void unrelatedSelectionAndInvalidSurvivorDoNotMutate() throws Exception
   {
      Map<String, List<List<String>>> before = snapshot();
      run(input("3,4,5", 3));
      assertEquals(before, snapshot());
      run(input("1,2", 3));
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Read authorization applies to discovery even when the caller selects denied IDs.
    *******************************************************************************/
   @Test
   void readDeniedDuplicateIsNotMerged() throws Exception
   {
      lockPeople(RecordSecurityLock.LockScope.READ, 1, 3, 4, 5);
      Map<String, List<List<String>>> before = snapshot();
      run(input("1,2", 1));
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** A denied survivor must not consume the other Person or its associations.
    *******************************************************************************/
   @Test
   void writeDeniedSurvivorDoesNotConsumeDuplicate() throws Exception
   {
      lockPeople(RecordSecurityLock.LockScope.WRITE, 2, 3, 4, 5);
      Map<String, List<List<String>>> before = snapshot();
      QException failure = assertThrows(QException.class, () -> run(input("1,2", 1)));
      assertTrue(failure.toString().contains("survivor write"), failure.toString());
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** A native unique constraint and matching QQQ key reject the survivor change.
    *******************************************************************************/
   @Test
   void uniqueConflictDoesNotConsumeDuplicate() throws Exception
   {
      sql("ALTER TABLE person ADD CONSTRAINT merge_first_name_unique UNIQUE(first_name)");
      instance.getTable("person").withUniqueKey(new UniqueKey("firstName"));
      Map<String, List<List<String>>> before = snapshot();
      RunProcessInput input = input("1,2", 1);
      input.addValue("mergedFirstName", "Casey");
      QException failure = assertThrows(QException.class, () -> run(input));
      assertTrue(failure.toString().contains("survivor write"), failure.toString());
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** A real customizer exception after the survivor update rolls its transaction
    ** back. The customizer checks uncommitted native state before throwing.
    *******************************************************************************/
   @Test
   void thrownLoadFailureRollsBackAlreadyUpdatedSurvivor() throws Exception
   {
      instance.getTable("person").withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(FailAfterSurvivorUpdate.class));
      Map<String, List<List<String>>> before = snapshot();
      QException failure = assertThrows(QException.class, () -> run(input("1,2", 1)));
      assertTrue(failure.toString().contains("Owned load failure"), failure.toString());
      assertEquals(Boolean.TRUE, QContext.getObject("sawUncommittedMerge"));
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Reassignment errors must stop deletion and roll back the earlier survivor write.
    *******************************************************************************/
   @Test
   void deniedAssociationReassignmentRollsBackMerge() throws Exception
   {
      sql("INSERT INTO pet(id,name,species_id,person_id) VALUES(7,'Owned second pet',1,2)");
      instance.addSecurityKeyType(new QSecurityKeyType().withName("mergePet"));
      instance.getTable("pet").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("mergePet")
         .withFieldName("id").withLockScope(RecordSecurityLock.LockScope.WRITE));
      session.withSecurityKeyValue("mergePet", 5);
      Map<String, List<List<String>>> before = snapshot();
      for(String level : List.of("process", "page"))
      {
         RunProcessInput input = input("1,2", 1);
         input.addValue("transactionLevel", level);
         QException failure = assertThrows(QException.class, () -> run(input));
         assertTrue(failure.toString().contains("related record update"), failure.toString());
         assertEquals(before, snapshot());
      }

   }



   /*******************************************************************************
    ** A failed duplicate deletion must not leave the survivor or associations changed.
    *******************************************************************************/
   @Test
   void deniedDuplicateDeletionRollsBackMerge() throws Exception
   {
      lockPeople(RecordSecurityLock.LockScope.WRITE, 1, 3, 4, 5);
      Map<String, List<List<String>>> before = snapshot();
      QException failure = assertThrows(QException.class, () -> run(input("1,2", 1)));
      assertTrue(failure.toString().contains("record deletion"), failure.toString());
      assertEquals(before, snapshot());
   }



   /*******************************************************************************
    ** Autocommit cannot undo a prior survivor write, but stops before deleting the
    ** duplicate on reassignment failure. This is not process-level atomicity.
    *******************************************************************************/
   @Test
   void autocommitFailureStopsDeletionWithoutClaimingRollback() throws Exception
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("mergePet"));
      instance.getTable("pet").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("mergePet")
         .withFieldName("id").withLockScope(RecordSecurityLock.LockScope.WRITE));
      session.withSecurityKeyValue("mergePet", 6);
      Map<String, List<List<String>>> before = snapshot();
      RunProcessInput input = input("1,2", 1);
      input.addValue("transactionLevel", "autoCommit");
      QException failure = assertThrows(QException.class, () -> run(input));
      assertTrue(failure.toString().contains("related record update"), failure.toString());
      assertEquals(List.of(List.of("1", "Merged", "30"), List.of("2", "Blair", "20")), rows("SELECT id,first_name,days_worked FROM person WHERE id IN (1,2) ORDER BY id"));
      assertEquals(before.get("person").subList(2, 5), rows("SELECT * FROM person WHERE id>=3 ORDER BY id"));
      assertEquals(before.get("pet"), rows("SELECT * FROM pet ORDER BY id"));
      assertEquals(before.get("pet_note"), rows("SELECT * FROM pet_note ORDER BY id"));
   }



   /*******************************************************************************
    ** Cleanup diagnostics must remain supplemental to a real merge failure; native
    ** rollback/close happen before this fixture raises their synthetic failures.
    *******************************************************************************/
   @Test
   void cleanupFailuresDoNotMaskThrownMergeFailure() throws Exception
   {
      assertCleanupFailurePreservesPrimary(false);
   }



   /*******************************************************************************
    ** Record-level reassignment failure must retain the same precedence as throws.
    *******************************************************************************/
   @Test
   void cleanupFailuresDoNotMaskErrorBearingReassignment() throws Exception
   {
      assertCleanupFailurePreservesPrimary(true);
   }



   /*******************************************************************************
    ** Exercise both existing transaction levels without creating a new provider policy.
    *******************************************************************************/
   private void assertCleanupFailurePreservesPrimary(boolean recordError) throws Exception
   {
      registerCleanupProcess();
      if(recordError)
      {
         instance.addSecurityKeyType(new QSecurityKeyType().withName("mergePet"));
         instance.getTable("pet").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("mergePet")
            .withFieldName("id").withLockScope(RecordSecurityLock.LockScope.WRITE));
         session.withSecurityKeyValue("mergePet", 6);
      }
      else
      {
         instance.getTable("person").withCustomizer(TableCustomizers.PRE_DELETE_RECORD, new QCodeReference(FailAfterSurvivorUpdate.class));
      }
      Map<String, List<List<String>>> before = snapshot();
      for(String level : List.of("process", "page"))
      {
         QContext.setObject("cleanupRollback", null);
         QContext.setObject("cleanupClose", null);
         RunProcessInput input = input("1,2", 1);
         input.setProcessName(CLEANUP_PROCESS);
         input.addValue("transactionLevel", level);
         QException failure = assertThrows(QException.class, () -> run(input));
         String primaryMessage = recordError ? "Merge failed during related record update" : "Owned load failure after native survivor update";
         Throwable primary = failure;
         while(primary != null && !primaryMessage.equals(primary.getMessage()))
         {
            primary = primary.getCause();
         }
         assertNotNull(primary, failure.toString());
         assertSame(QContext.getObject("ownedMergeFailure"), primary);
         assertEquals(List.of("Owned rollback diagnostic", "Owned close diagnostic"),
            Arrays.stream(primary.getSuppressed()).map(Throwable::getMessage).toList());
         assertEquals(Boolean.TRUE, QContext.getObject("cleanupRollback"));
         assertEquals(Boolean.TRUE, QContext.getObject("cleanupClose"));
         assertEquals(before, snapshot());
      }
   }



   /*******************************************************************************
    ** A successful commit followed by failed close reports the cleanup failure.
    ** The persisted merge is not falsely described as rolled back.
    *******************************************************************************/
   @Test
   void successfulMergeSurfacesCloseFailure() throws Exception
   {
      assertCommittedMergeSurfacesCloseFailure("process");
   }



   /*******************************************************************************
    ** Page commit has the same cleanup-error visibility as process commit.
    *******************************************************************************/
   @Test
   void successfulPageMergeSurfacesCloseFailure() throws Exception
   {
      assertCommittedMergeSurfacesCloseFailure("page");
   }



   /*******************************************************************************
    ** Native rows prove the commit actually preceded the cleanup diagnostic.
    *******************************************************************************/
   private void assertCommittedMergeSurfacesCloseFailure(String level) throws Exception
   {
      registerCleanupProcess();
      RunProcessInput input = input("1,2", 1);
      input.setProcessName(CLEANUP_PROCESS);
      input.addValue("transactionLevel", level);
      QException failure = assertThrows(QException.class, () -> run(input));
      assertEquals("Owned close diagnostic", failure.getCause().getMessage());
      assertNull(QContext.getObject("cleanupRollback"));
      assertEquals(Boolean.TRUE, QContext.getObject("cleanupClose"));
      assertEquals(List.of(List.of("1", "Merged", "30")), rows("SELECT id,first_name,days_worked FROM person WHERE id IN (1,2) ORDER BY id"));
      assertEquals(List.of(List.of("5", "1")), rows("SELECT id,person_id FROM pet WHERE id=5"));
      assertEquals(2, rows("SELECT * FROM pet_note").size());
   }



   /*******************************************************************************
    ** Providers may throw the same exception instance during cleanup; do not replace
    ** it with IllegalArgumentException from self-suppression.
    *******************************************************************************/
   @Test
   void repeatedPrimaryInstanceIsNotSuppressedOntoItself() throws Exception
   {
      registerCleanupProcess();
      QContext.setObject("reuseCleanupFailure", Boolean.TRUE);
      Map<String, List<List<String>>> before = snapshot();
      for(String level : List.of("process", "page"))
      {
         RunProcessInput input = input("1,2", 1);
         input.setProcessName(CLEANUP_PROCESS);
         input.addValue("transactionLevel", level);
         QException failure = assertThrows(QException.class, () -> run(input));
         assertSame(QContext.getObject("ownedMergeFailure"), failure.getCause());
         assertEquals(0, failure.getCause().getSuppressed().length);
         assertEquals(before, snapshot());
      }
   }



   /*******************************************************************************
    ** Register the same application transform with the existing custom load seam.
    *******************************************************************************/
   private void registerCleanupProcess()
   {
      instance.addProcess(MergeDuplicatesProcess.processMetaDataBuilder().withName(CLEANUP_PROCESS).withTableName("person")
         .withMergeDuplicatesTransformStepClass(MergePeople.class).withLoadStepClass(CleanupFailureLoad.class)
         .withFields(List.of(new QFieldMetaData("survivorId", QFieldType.INTEGER), new QFieldMetaData("mergedFirstName", QFieldType.STRING)))
         .getProcessMetaData());
   }



   /*******************************************************************************
    ** Install scoped row authorization without changing the shared sample metadata.
    *******************************************************************************/
   private void lockPeople(RecordSecurityLock.LockScope scope, Integer... allowedIds)
   {
      instance.addSecurityKeyType(new QSecurityKeyType().withName("mergePerson"));
      instance.getTable("person").withRecordSecurityLock(new RecordSecurityLock().withSecurityKeyType("mergePerson")
         .withFieldName("id").withLockScope(scope));
      for(Integer id : allowedIds)
      {
         session.withSecurityKeyValue("mergePerson", id);
      }
   }



   /*******************************************************************************
    ** Every native column in all affected tables is part of the no-mutation oracle.
    *******************************************************************************/
   private Map<String, List<List<String>>> snapshot() throws Exception
   {
      Map<String, List<List<String>>> result = new LinkedHashMap<>();
      for(String table : List.of("person", "pet", "pet_note"))
      {
         result.put(table, rows("SELECT * FROM " + table + " ORDER BY id"));
      }
      return result;
   }



   /*******************************************************************************
    ** User input goes through normal process selection and transaction defaults.
    *******************************************************************************/
   private RunProcessInput input(String selectedIds, Integer survivorId)
   {
      UUID id = UUID.randomUUID();
      processIds.add(id);
      RunProcessInput input = new RunProcessInput().withProcessName(PROCESS);
      input.setProcessUUID(id.toString());
      input.setInputSource(QInputSource.USER);
      input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
      input.addValue("recordIds", selectedIds);
      input.addValue("survivorId", survivorId);
      input.addValue("mergedFirstName", "Merged");
      return input;
   }



   /*******************************************************************************
    ** Assert that the framework returns control to this fixture's caller context.
    *******************************************************************************/
   private RunProcessOutput run(RunProcessInput input) throws Exception
   {
      try
      {
         return new RunProcessAction().execute(input);
      }
      finally
      {
         assertSame(instance, QContext.getQInstance());
         assertSame(session, QContext.getQSession());
      }
   }



   /*******************************************************************************
    ** Native setup never depends on the QQQ actions being tested.
    *******************************************************************************/
   private void sql(String sql) throws Exception
   {
      try(Statement statement = anchor.createStatement())
      {
         statement.execute(sql);
      }
   }



   /*******************************************************************************
    ** Separate JDBC connection observes committed persistence, including nulls.
    *******************************************************************************/
   private List<List<String>> rows(String sql) throws Exception
   {
      List<List<String>> rows = new ArrayList<>();
      try(Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
         Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql))
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
    ** Supported application failure seam, after native write but before deletion.
    *******************************************************************************/
   public static class FailAfterSurvivorUpdate implements TableCustomizerInterface
   {
      /*******************************************************************************
       ** Prove a rollback is meaningful rather than merely a pre-write rejection.
       *******************************************************************************/
      @Override
      public List<QRecord> preDelete(DeleteInput input, List<QRecord> records, boolean isPreview) throws QException
      {
         RDBMSTransaction transaction = (RDBMSTransaction) input.getTransaction();
         try(Statement statement = transaction.getConnection().createStatement();
            ResultSet result = statement.executeQuery("SELECT first_name,days_worked FROM person WHERE id=1"))
         {
            assertTrue(result.next());
            assertEquals("Merged", result.getString(1));
            assertEquals(30, result.getInt(2));
            try(ResultSet pet = statement.executeQuery("SELECT person_id FROM pet WHERE id=5"))
            {
               assertTrue(pet.next());
               assertEquals(1, pet.getInt(1));
            }
            QContext.setObject("sawUncommittedMerge", Boolean.TRUE);
         }
         catch(SQLException e)
         {
            throw new QException("Unable to observe owned transaction", e);
         }
         throw new QException("Owned load failure after native survivor update");
      }
   }



   /*******************************************************************************
    ** Existing load extension point supplies a native transaction with owned faults.
    *******************************************************************************/
   public static class CleanupFailureLoad extends MergeDuplicatesLoadStep
   {
      /*******************************************************************************
       ** Retain the exact primary instance independently of framework wrapping.
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         try
         {
            super.runOnePage(input, output);
            if(Boolean.TRUE.equals(QContext.getObject("reuseCleanupFailure")))
            {
               throw new IllegalStateException("Owned runtime merge failure");
            }
         }
         catch(QException | RuntimeException failure)
         {
            QContext.setObject("ownedMergeFailure", failure);
            throw failure;
         }
      }



      /*******************************************************************************
       ** Only this fixture's H2 connection is wrapped; all record actions stay real.
       *******************************************************************************/
      @Override
      public Optional<QBackendTransaction> openTransaction(RunBackendStepInput input) throws QException
      {
         RDBMSBackendMetaData backend = (RDBMSBackendMetaData) QContext.getQInstance().getBackend("mergeDatabase");
         try
         {
            return Optional.of(new CleanupFailureTransaction(DriverManager.getConnection(backend.getJdbcUrl(), "sa", "")));
         }
         catch(SQLException e)
         {
            throw new QException("Unable to open owned transaction", e);
         }
      }
   }



   /*******************************************************************************
    ** Fail only after actual native cleanup, retaining independent teardown ownership.
    *******************************************************************************/
   private static class CleanupFailureTransaction extends RDBMSTransaction
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      private CleanupFailureTransaction(Connection connection) throws SQLException
      {
         super(connection);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void rollback() throws QException
      {
         super.rollback();
         QContext.setObject("cleanupRollback", Boolean.TRUE);
         if(Boolean.TRUE.equals(QContext.getObject("reuseCleanupFailure")))
         {
            throw (RuntimeException) QContext.getObject("ownedMergeFailure");
         }
         throw new QException("Owned rollback diagnostic");
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void close()
      {
         super.close();
         assertDoesNotThrow(() -> assertTrue(getConnection().isClosed()));
         QContext.setObject("cleanupClose", Boolean.TRUE);
         if(Boolean.TRUE.equals(QContext.getObject("reuseCleanupFailure")))
         {
            throw (RuntimeException) QContext.getObject("ownedMergeFailure");
         }
         throw new IllegalStateException("Owned close diagnostic");
      }
   }



   /*******************************************************************************
    ** Application policy uses the existing transform extension point. The framework
    ** owns duplicate discovery, the load, deletes and the process transaction.
    *******************************************************************************/
   public static class MergePeople extends AbstractMergeDuplicatesTransformStep
   {
      /*******************************************************************************
       ** Email identifies the owned duplicate group; automatic audit is out of scope.
       *******************************************************************************/
      @Override
      protected MergeProcessConfig getMergeProcessConfig()
      {
         return new MergeProcessConfig("person", List.of("email"), false);
      }



      /*******************************************************************************
       ** Choose explicitly, sum worked days, and request existing association updates.
       *******************************************************************************/
      @Override
      public QRecord buildRecordToKeep(RunBackendStepInput input, List<QRecord> duplicates) throws QException
      {
         Integer survivorId = input.getValueInteger("survivorId");
         QRecord survivor = duplicates.stream().filter(record -> survivorId.equals(record.getValueInteger("id"))).findFirst()
            .orElseThrow(() -> new SkipTheseRecordsException("Chosen survivor is not in this duplicate group"));
         QRecord merged = new QRecord(survivor).withValue("firstName", input.getValueString("mergedFirstName"))
            .withValue("daysWorked", duplicates.stream().mapToInt(record -> record.getValueInteger("daysWorked")).sum());
         List<Serializable> ids = duplicates.stream().map(record -> record.getValue("id")).toList();
         List<QRecord> pets = new QueryAction().execute(new QueryInput("pet").withFilter(new QQueryFilter()
            .withCriteria(new QFilterCriteria("personId", QCriteriaOperator.IN, ids)))).getRecords();
         addOtherTableRecordsToStore("pet", pets.stream().filter(pet -> !survivorId.equals(pet.getValueInteger("personId"))).map(pet -> new QRecord().withValue("id", pet.getValue("id")).withValue("personId", survivorId)).toList());
         return merged;
      }
   }
}
