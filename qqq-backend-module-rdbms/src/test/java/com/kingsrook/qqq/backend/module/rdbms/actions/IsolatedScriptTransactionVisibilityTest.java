/*
 * QQQ - Low-code Application Framework for Engineers.
 * Copyright (C) 2021-2026.  Kingsrook, LLC
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


import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import com.kingsrook.qqq.backend.core.actions.QBackendTransaction;
import com.kingsrook.qqq.backend.core.actions.scripts.ExecuteCodeAction;
import com.kingsrook.qqq.backend.core.actions.scripts.ScriptExecutionIsolation;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedViewsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.scripts.Script;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptExecutionMode;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptRevision;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptType;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.TestUtils;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


/*******************************************************************************
 ** Real two-connection H2 regression test for the isolated-script marker: an
 ** ISOLATED script that is inserted, but not yet committed, in transaction A is
 ** invisible to an independent connection B.  Legacy (in-process) resolution on B
 ** must still refuse it (fail closed - an id that can't be resolved is never
 ** "legacy"), and must keep refusing it once A commits (when it resolves, as
 ** ISOLATED).  A legitimate, visible, unmarked script keeps executing throughout.
 **
 ** Uses the real core APIs (ScriptExecutionIsolation, ExecuteCodeAction, the
 ** standard script tables) over actual JDBC transactions - nothing is mocked.
 *******************************************************************************/
class IsolatedScriptTransactionVisibilityTest
{
   private RDBMSBackendMetaData backend;
   private Integer              scriptTypeId;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      CountingFunction.COUNT.set(0);
      ConnectionManager.closeConnectionProviders();

      ///////////////////////////////////////////////////////////////////////////
      // a new instance, on its own private in-memory h2 database - which has  //
      // the standard script tables (including the executionMode column)       //
      ///////////////////////////////////////////////////////////////////////////
      QInstance instance = TestUtils.defineInstance();
      backend = (RDBMSBackendMetaData) instance.getBackend(TestUtils.DEFAULT_BACKEND_NAME);
      backend.setJdbcUrl("jdbc:h2:mem:isolated_script_tx_" + UUID.randomUUID().toString().replace("-", "") + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE");

      new SavedViewsMetaDataProvider().withIsShareSavedViewEnabled(false).defineAll(instance, TestUtils.DEFAULT_BACKEND_NAME, table ->
      {
         table.setBackendDetails(new RDBMSTableBackendDetails().withTableName(QInstanceEnricher.inferBackendName(table.getName())));
         QInstanceEnricher.setInferredFieldBackendNames(table);
      });
      new ScriptsMetaDataProvider().defineAll(instance, TestUtils.DEFAULT_BACKEND_NAME, table ->
      {
         table.setBackendDetails(new RDBMSTableBackendDetails().withTableName(QInstanceEnricher.inferBackendName(table.getName())));
         QInstanceEnricher.setInferredFieldBackendNames(table);
      });

      QContext.init(instance, new QSession());

      try(Connection connection = ConnectionManager.getConnection(backend); Statement statement = connection.createStatement())
      {
         statement.execute("""
            CREATE TABLE script_type
            (
               id INTEGER AUTO_INCREMENT PRIMARY KEY,
               create_date TIMESTAMP,
               modify_date TIMESTAMP,
               name VARCHAR(100),
               help_text CLOB,
               sample_code CLOB,
               file_mode INTEGER,
               test_script_interface_name VARCHAR(500)
            )""");
         statement.execute("""
            CREATE TABLE script
            (
               id INTEGER AUTO_INCREMENT PRIMARY KEY,
               create_date TIMESTAMP,
               modify_date TIMESTAMP,
               name VARCHAR(250),
               script_type_id INTEGER,
               table_name VARCHAR(100),
               max_batch_size INTEGER,
               current_script_revision_id INTEGER,
               execution_mode VARCHAR(32)
            )""");
         statement.execute("""
            CREATE TABLE script_revision
            (
               id INTEGER AUTO_INCREMENT PRIMARY KEY,
               create_date TIMESTAMP,
               modify_date TIMESTAMP,
               script_id INTEGER,
               api_name VARCHAR(100),
               api_version VARCHAR(100),
               sequence_no INTEGER,
               commit_message VARCHAR(250),
               author VARCHAR(100)
            )""");
      }

      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(ScriptType.TABLE_NAME);
      insertInput.setRecords(List.of(new ScriptType().withName("TestScriptType").toQRecord()));
      scriptTypeId = new InsertAction().execute(insertInput).getRecords().get(0).getValueInteger("id");
      assertNotNull(scriptTypeId);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      try(Connection connection = ConnectionManager.getConnection(backend); Statement statement = connection.createStatement())
      {
         statement.execute("DROP ALL OBJECTS");
      }
      catch(Exception e)
      {
         //////////////////////////////////////////////////////////////
         // best effort - the database is private to this test, and  //
         // goes away with the JVM                                   //
         //////////////////////////////////////////////////////////////
      }
      ConnectionManager.closeConnectionProviders();
      QContext.clear();
   }



   /*******************************************************************************
    ** inserted-but-uncommitted in A: invisible to B (proven with plain jdbc), and
    ** refused by legacy resolution on B - by script id, by revision id, and via
    ** ExecuteCodeAction (the engine is never reached).  After A commits, still
    ** refused - now because it resolves, as ISOLATED.
    *******************************************************************************/
   @Test
   void testUncommittedIsolatedScriptIsRefusedOnIndependentConnectionThenAfterCommit() throws Exception
   {
      try(QBackendTransaction transactionA = QBackendTransaction.openFor(new InsertInput(Script.TABLE_NAME)))
      {
         Integer scriptId   = ScriptExecutionIsolation.insertIsolatedScript(new Script().withName("isolated").withScriptTypeId(scriptTypeId).withTableName(TestUtils.TABLE_NAME_PERSON), transactionA).getValueInteger("id");
         Integer revisionId = insertRevision(scriptId, transactionA);

         //////////////////////////////////////////////////////////////////
         // A sees its own work, as ISOLATED                             //
         //////////////////////////////////////////////////////////////////
         assertEquals(ScriptExecutionMode.ISOLATED, ScriptExecutionIsolation.getExecutionMode(scriptId, transactionA));

         ////////////////////////////////////////////////////////////////////////////////////
         // an independent connection (B) really can't see the uncommitted rows - so this  //
         // is a true two-connection setup, not an in-process model of one                 //
         ////////////////////////////////////////////////////////////////////////////////////
         assertEquals(0, countRows("script", scriptId));
         assertEquals(0, countRows("script_revision", revisionId));

         /////////////////////////////////////////////////////////////////////////////////////////
         // legacy resolution on B (no transaction in the context) - refused, engine not reached //
         /////////////////////////////////////////////////////////////////////////////////////////
         assertLegacyRefused(scriptId, revisionId);

         ///////////////////////////////////////////////////////////////////////////////////
         // commit, and now B can see it - and legacy resolution refuses it as ISOLATED   //
         ///////////////////////////////////////////////////////////////////////////////////
         transactionA.commit();
         assertEquals(1, countRows("script", scriptId));
         assertEquals(1, countRows("script_revision", revisionId));
         assertEquals(ScriptExecutionMode.ISOLATED, ScriptExecutionIsolation.getExecutionMode(scriptId, null));
         assertLegacyRefused(scriptId, revisionId);
      }

      assertEquals(0, CountingFunction.COUNT.get());
   }



   /*******************************************************************************
    ** a legitimate, committed, unmarked script keeps executing - including while
    ** an isolated script is still uncommitted in a transaction that is open on
    ** another connection - and the isolated one is refused throughout.
    *******************************************************************************/
   @Test
   void testVisibleLegacyScriptStillRunsWhileIsolatedScriptIsUncommitted() throws Exception
   {
      Integer legacyScriptId   = insertGenericScript(new Script().withName("legacy").withScriptTypeId(scriptTypeId).withTableName(TestUtils.TABLE_NAME_PERSON));
      Integer legacyRevisionId = insertRevision(legacyScriptId, null);

      try(QBackendTransaction transactionA = QBackendTransaction.openFor(new InsertInput(Script.TABLE_NAME)))
      {
         Integer isolatedScriptId   = ScriptExecutionIsolation.insertIsolatedScript(new Script().withName("isolated").withScriptTypeId(scriptTypeId).withTableName(TestUtils.TABLE_NAME_PERSON), transactionA).getValueInteger("id");
         Integer isolatedRevisionId = insertRevision(isolatedScriptId, transactionA);

         //////////////////////////////////////////////////////////////////////////
         // legacy runs (by script id; by revision id; by both) - isolated doesn't //
         //////////////////////////////////////////////////////////////////////////
         ExecuteCodeOutput output = new ExecuteCodeOutput();
         new ExecuteCodeAction().run(legacyInput().withScriptId(legacyScriptId), output);
         assertEquals(1, output.getOutput());
         new ExecuteCodeAction().run(legacyInput().withScriptRevisionId(legacyRevisionId), output);
         assertEquals(2, output.getOutput());
         new ExecuteCodeAction().run(legacyInput().withScriptId(legacyScriptId).withScriptRevisionId(legacyRevisionId), output);
         assertEquals(3, output.getOutput());

         assertLegacyRefused(isolatedScriptId, isolatedRevisionId);
         assertEquals(3, CountingFunction.COUNT.get());

         transactionA.commit();

         assertLegacyRefused(isolatedScriptId, isolatedRevisionId);
         new ExecuteCodeAction().run(legacyInput().withScriptId(legacyScriptId), output);
         assertEquals(4, output.getOutput());
      }

      assertEquals(4, CountingFunction.COUNT.get());
   }



   /*******************************************************************************
    ** an isolated script whose transaction is rolled back never becomes visible -
    ** and an id that doesn't resolve is still refused (never treated as legacy).
    ** a legitimate script inserted afterwards is unaffected.
    *******************************************************************************/
   @Test
   void testRolledBackIsolatedScriptIsRefusedAndLegacyIsUnaffected() throws Exception
   {
      Integer rolledBackScriptId;
      try(QBackendTransaction transactionA = QBackendTransaction.openFor(new InsertInput(Script.TABLE_NAME)))
      {
         rolledBackScriptId = ScriptExecutionIsolation.insertIsolatedScript(new Script().withName("isolated").withScriptTypeId(scriptTypeId).withTableName(TestUtils.TABLE_NAME_PERSON), transactionA).getValueInteger("id");
         assertLegacyRefused(rolledBackScriptId, null);
         transactionA.rollback();
      }

      assertEquals(0, countRows("script", rolledBackScriptId));
      assertLegacyRefused(rolledBackScriptId, null);
      assertEquals(0, CountingFunction.COUNT.get());

      Integer legacyScriptId = insertGenericScript(new Script().withName("legacy").withScriptTypeId(scriptTypeId).withTableName(TestUtils.TABLE_NAME_PERSON));
      new ExecuteCodeAction().run(legacyInput().withScriptId(legacyScriptId), new ExecuteCodeOutput());
      assertEquals(1, CountingFunction.COUNT.get());
   }



   /*******************************************************************************
    ** legacy resolution - with no transaction in the context (so on a connection
    ** independent of any open one) - refuses each of the ids, and never reaches
    ** the engine.
    *******************************************************************************/
   private void assertLegacyRefused(Integer scriptId, Integer revisionId)
   {
      int before = CountingFunction.COUNT.get();

      assertThatThrownBy(() -> ScriptExecutionIsolation.requireLegacyExecutionAllowed(scriptId)).isInstanceOf(QPermissionDeniedException.class);
      assertThatThrownBy(() -> new ExecuteCodeAction().run(legacyInput().withScriptId(scriptId), new ExecuteCodeOutput())).isInstanceOf(QPermissionDeniedException.class);

      if(revisionId != null)
      {
         assertThatThrownBy(() -> new ExecuteCodeAction().run(legacyInput().withScriptRevisionId(revisionId), new ExecuteCodeOutput())).isInstanceOf(QPermissionDeniedException.class);
         assertThatThrownBy(() -> new ExecuteCodeAction().run(legacyInput().withScriptId(scriptId).withScriptRevisionId(revisionId), new ExecuteCodeOutput())).isInstanceOf(QPermissionDeniedException.class);
      }

      assertEquals(before, CountingFunction.COUNT.get());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ExecuteCodeInput legacyInput()
   {
      return (new ExecuteCodeInput().withCodeReference(new QCodeReference(CountingFunction.class)));
   }



   /*******************************************************************************
    ** insert a (legacy, unmarked) script through the generic action - committed.
    *******************************************************************************/
   private Integer insertGenericScript(Script script) throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(Script.TABLE_NAME);
      insertInput.setRecords(List.of(script.toQRecord()));
      QRecord inserted = new InsertAction().execute(insertInput).getRecords().get(0);
      assertNotNull(inserted.getValueInteger("id"), String.valueOf(inserted.getErrors()));
      return (inserted.getValueInteger("id"));
   }



   /*******************************************************************************
    ** insert a revision, in the given transaction (or, if null, on its own).
    *******************************************************************************/
   private Integer insertRevision(Integer scriptId, QBackendTransaction transaction) throws QException
   {
      InsertInput insertInput = new InsertInput();
      insertInput.setTableName(ScriptRevision.TABLE_NAME);
      insertInput.setTransaction(transaction);
      insertInput.setRecords(List.of(new ScriptRevision().withScriptId(scriptId).withSequenceNo(1).toQRecord()));
      QRecord inserted = new InsertAction().execute(insertInput).getRecords().get(0);
      assertNotNull(inserted.getValueInteger("id"), String.valueOf(inserted.getErrors()));
      return (inserted.getValueInteger("id"));
   }



   /*******************************************************************************
    ** count rows with the given id - using plain jdbc, on a brand new connection
    ** (so, what an independent session can see).
    *******************************************************************************/
   private int countRows(String tableName, Integer id) throws SQLException
   {
      try(Connection connection = ConnectionManager.getConnection(backend); PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName + " WHERE id = ?"))
      {
         statement.setInt(1, id);
         try(ResultSet resultSet = statement.executeQuery())
         {
            resultSet.next();
            return (resultSet.getInt(1));
         }
      }
   }



   /*******************************************************************************
    ** stands in for an engine: counts the times it is (wrongly or rightly) reached.
    *******************************************************************************/
   public static class CountingFunction implements Function<Map<String, Object>, Serializable>
   {
      static final AtomicInteger COUNT = new AtomicInteger(0);



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Serializable apply(Map<String, Object> context)
      {
         return (COUNT.incrementAndGet());
      }
   }

}
