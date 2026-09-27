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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.scripts.ExecuteCodeAction;
import com.kingsrook.qqq.backend.core.actions.scripts.logging.BuildScriptLogAndScriptLogLineExecutionLogger;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QCodeException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeType;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.SimpleConnectionProvider;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*******************************************************************************
 ** First-party table customization through the real Nashorn QCodeExecutor path.
 ** Independent JDBC snapshots prove intended writes and failed-write isolation.
 ******************************************************************************/
@Timeout(20)
class SampleJavaScriptAcceptanceTest
{
   private CapturedContext previousContext;
   private Map<String, Serializable> previousObjects;
   private Connection oracle;
   private QInstance instance;
   private QSession session;
   private Invocation invocation;



   /*******************************************************************************
    ** Own a unique native database and table customizer without starting a server.
    ******************************************************************************/
   @BeforeEach
   void setUp() throws Exception
   {
      previousContext = QContext.capture();
      previousObjects = QContext.getObjects();
      QContext.setObjects(new LinkedHashMap<>());
      ConnectionManager.resetConnectionProviders();
      instance = SampleMetaDataProvider.defineTestInstance();
      RDBMSBackendMetaData backend = (RDBMSBackendMetaData) instance.getBackend(SampleMetaDataProvider.RDBMS_BACKEND_NAME);
      backend.setJdbcUrl("jdbc:h2:mem:javascript_" + UUID.randomUUID() + ";MODE=MySQL");
      backend.setConnectionProvider(new QCodeReference(SimpleConnectionProvider.class));
      oracle = DriverManager.getConnection(backend.getJdbcUrl(), "sa", "");
      try(var reader = new InputStreamReader(SampleMetaDataProvider.class.getResourceAsStream("/prime-test-database.sql"), StandardCharsets.UTF_8))
      {
         RunScript.execute(oracle, reader);
      }
      instance.getTable("person").withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, new QCodeReference(ScriptCustomizer.class));
      instance.setDeploymentMode("owned-javascript-fixture");
      session = new QSession();
      QContext.init(instance, session);
      invocation = new Invocation();
      QContext.setObject("javascriptInvocation", invocation);
   }



   /*******************************************************************************
    ** Release native state and restore caller context, including named objects.
    ******************************************************************************/
   @AfterEach
   void tearDown() throws Exception
   {
      try
      {
         if(oracle != null)
         {
            oracle.close();
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



   /*******************************************************************************
    ** Typed inputs, logger calls, return identity and mutation survive real DML.
    ******************************************************************************/
   @Test
   void testTypedContextLoggerAndReturnedRecordPersistOnlyIntendedChange() throws Exception
   {
      Map<Integer, Map<String, String>> before = snapshot();
      invocation.reference = script("success.js");
      QRecord record = updateRecord();
      Instant beforeWrite = Instant.now().minusSeconds(1);
      List<QRecord> output = new UpdateAction().execute(new UpdateInput().withTableName("person").withRecord(record)).getRecords();
      assertEquals(1, output.size());
      assertTrue(output.get(0).getErrors().isEmpty());
      assertSame(record, invocation.result);
      assertEquals("owned:12.50:7", record.getValueString("lastName"));
      Map<Integer, Map<String, String>> expected = new LinkedHashMap<>(before);
      Map<Integer, Map<String, String>> after = snapshot();
      Map<String, String> changed = new LinkedHashMap<>(expected.get(1));
      changed.put("LAST_NAME", "owned:12.50:7");
      String modifiedValue = after.get(1).get("MODIFY_DATE");
      Instant modified = LocalDateTime.parse(modifiedValue.replace(' ', 'T')).toInstant(ZoneOffset.UTC);
      assertFalse(modified.isBefore(beforeWrite));
      assertFalse(modified.isAfter(Instant.now()));
      changed.put("MODIFY_DATE", modifiedValue);
      expected.put(1, changed);
      assertEquals(expected, after);
      assertEquals(List.of("person=1;mode=owned-javascript-fixture"), logLines());
      assertEquals(false, invocation.logger.getScriptLog().getValueBoolean("hadError"));
      assertNotNull(invocation.logger.getScriptLog().getValue("endTimestamp"));
      assertContextAndConnections();
   }



   /*******************************************************************************
    ** Use the original inline reference, including Nashorn's parser diagnostics.
    ******************************************************************************/
   @Test
   void testSyntaxFailureLogsDiagnosticAndPreservesNativeRows() throws Exception
   {
      invocation.reference = script("syntax-error.js");
      QCodeException failure = assertInstanceOf(QCodeException.class, rejectedUpdate());
      assertTrue(failure.getMessage().contains("Script parser exception"));
      assertNotNull(failure.getContext());
      assertTrue(logLines().isEmpty());
      assertLoggedFailure();
   }



   /*******************************************************************************
    ** Mutation before a thrown script error stays in memory, never in native rows.
    ******************************************************************************/
   @Test
   void testRuntimeFailureAfterMutationPreservesNativeRows() throws Exception
   {
      invocation.reference = script("runtime-error.js");
      QCodeException failure = assertInstanceOf(QCodeException.class, rejectedUpdate());
      assertTrue(failure.getMessage().contains("owned script failure"));
      assertEquals("not-persisted", invocation.record.getValueString("lastName"));
      assertEquals(List.of("before-owned-failure"), logLines());
      assertLoggedFailure();
   }



   /*******************************************************************************
    ** Missing references fail through the normal action and execution logger.
    ******************************************************************************/
   @Test
   void testMissingCodeReferenceFailsWithoutNativeWrite() throws Exception
   {
      invocation.reference = null;
      QException failure = rejectedUpdate();
      assertInstanceOf(NullPointerException.class, failure.getCause());
      assertTrue(failure.getMessage().contains("Error executing code"));
      assertLoggedFailure();
   }



   /*******************************************************************************
    ** The executor supports inline code only; an empty reference must not write.
    ******************************************************************************/
   @Test
   void testMissingInlineScriptFailsWithoutNativeWrite() throws Exception
   {
      invocation.reference = new QCodeReference().withCodeType(QCodeType.JAVA_SCRIPT);
      QException failure = rejectedUpdate();
      assertTrue(failure.getCause().getMessage().contains("Only inline code is implemented"));
      assertLoggedFailure();
   }



   /*******************************************************************************
    ** A primitive is valid engine output but invalid for this customizer contract.
    ** The sample rejects it before DML; the engine log truthfully records success.
    ******************************************************************************/
   @Test
   void testInvalidResultTypeIsRejectedBeforeNativeWrite() throws Exception
   {
      invocation.reference = script("invalid-result.js");
      QException failure = rejectedUpdate();
      assertEquals("Script customizer must return a QRecord", failure.getMessage());
      assertEquals("not a record", invocation.result);
      assertEquals("not-persisted", invocation.record.getValueString("lastName"));
      assertEquals(false, invocation.logger.getScriptLog().getValueBoolean("hadError"));
      assertEquals("not a record", invocation.logger.getScriptLog().getValueString("output"));
   }



   /*******************************************************************************
    ** Read owned resources into an unmodified product QCodeReference.
    ******************************************************************************/
   private QCodeReference script(String name) throws Exception
   {
      try(var stream = getClass().getResourceAsStream("/SampleJavaScriptAcceptance/" + name))
      {
         assertNotNull(stream);
         return new QCodeReference().withCodeType(QCodeType.JAVA_SCRIPT).withInlineCode(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
      }
   }



   /*******************************************************************************
    ** A minimal update makes accidental persistence visible in every negative.
    ******************************************************************************/
   private QRecord updateRecord()
   {
      return new QRecord().withValue("id", 1).withValue("lastName", "attempted");
   }



   /*******************************************************************************
    ** Snapshot every persisted person field before and after the rejected update.
    ******************************************************************************/
   private QException rejectedUpdate() throws Exception
   {
      Map<Integer, Map<String, String>> before = snapshot();
      QException failure = assertThrows(QException.class, () -> new UpdateAction().execute(new UpdateInput().withTableName("person").withRecord(updateRecord())));
      assertEquals(before, snapshot());
      assertNull(invocation.logger.getScriptLog().getValue("id"));
      assertContextAndConnections();
      return failure;
   }



   /*******************************************************************************
    ** Capture the actual engine logger; do not replace execution with a mock.
    ******************************************************************************/
   private List<String> logLines()
   {
      return invocation.logger.getScriptLogLines().stream().map(row -> row.getValueString("text")).toList();
   }



   /*******************************************************************************
    ** Failed executions have a terminal error log and no accepted result.
    ******************************************************************************/
   private void assertLoggedFailure()
   {
      assertEquals(true, invocation.logger.getScriptLog().getValueBoolean("hadError"));
      assertNotNull(invocation.logger.getScriptLog().getValue("endTimestamp"));
      assertNotNull(invocation.logger.getScriptLog().getValueString("error"));
      assertNull(invocation.result);
   }



   /*******************************************************************************
    ** All person columns are native readback, independent of the QQQ query path.
    ******************************************************************************/
   private Map<Integer, Map<String, String>> snapshot() throws Exception
   {
      Map<Integer, Map<String, String>> result = new LinkedHashMap<>();
      try(Statement statement = oracle.createStatement(); ResultSet rows = statement.executeQuery("SELECT * FROM person ORDER BY id"))
      {
         while(rows.next())
         {
            Map<String, String> values = new LinkedHashMap<>();
            for(int i = 1; i <= rows.getMetaData().getColumnCount(); i++)
            {
               values.put(rows.getMetaData().getColumnName(i), rows.getString(i));
            }
            result.put(rows.getInt("id"), values);
         }
      }
      assertEquals(5, result.size());
      return result;
   }



   /*******************************************************************************
    ** DML closes its connections and leaves the owned session/context in place.
    ******************************************************************************/
   private void assertContextAndConnections() throws Exception
   {
      assertSame(instance, QContext.getQInstance());
      assertSame(session, QContext.getQSession());
      assertSame(invocation, QContext.getObject("javascriptInvocation"));
      try(Statement statement = oracle.createStatement(); ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.SESSIONS"))
      {
         assertTrue(rows.next());
         assertEquals(1, rows.getInt(1));
         assertFalse(rows.next());
      }
   }



   /*******************************************************************************
    ** Owned execution state passed through the normal named-context facility.
    ******************************************************************************/
   public static class Invocation implements Serializable
   {
      private QCodeReference reference;
      private final BuildScriptLogAndScriptLogLineExecutionLogger logger = new BuildScriptLogAndScriptLogLineExecutionLogger();
      private Serializable result;
      private QRecord record;
   }



   /*******************************************************************************
    ** Application-level adapter: the production loader loads Java customizers,
    ** which invoke ExecuteCodeAction for the configured original script reference.
    ******************************************************************************/
   public static class ScriptCustomizer implements TableCustomizerInterface
   {
      /*******************************************************************************
       ** Reject wrong result types before allowing UpdateAction to persist rows.
       ******************************************************************************/
      @Override
      public List<QRecord> preUpdate(UpdateInput input, List<QRecord> records, boolean preview, Optional<List<QRecord>> oldRecords) throws QException
      {
         Invocation run = (Invocation) QContext.getObject("javascriptInvocation");
         for(QRecord record : records)
         {
            run.record = record;
            ExecuteCodeInput scriptInput = new ExecuteCodeInput().withCodeReference(run.reference)
               .withContext("record", record).withContext("request", new ScriptRequest())
               .withExecutionLogger(run.logger);
            ExecuteCodeOutput scriptOutput = new ExecuteCodeOutput();
            new ExecuteCodeAction().run(scriptInput, scriptOutput);
            run.result = scriptOutput.getOutput();
            if(!(run.result instanceof QRecord))
            {
               throw new QException("Script customizer must return a QRecord");
            }
         }
         return records;
      }
   }



   /*******************************************************************************
    ** Real Java input bean consumed by Nashorn, retaining decimal arithmetic.
    ******************************************************************************/
   public static class ScriptRequest implements Serializable
   {
      /*******************************************************************************
       ** Typed string input.
       ******************************************************************************/
      public String getLabel()
      {
         return "owned";
      }



      /*******************************************************************************
       ** Typed decimal input.
       ******************************************************************************/
      public BigDecimal getAmount()
      {
         return new BigDecimal("10.00");
      }



      /*******************************************************************************
       ** Typed integer input.
       ******************************************************************************/
      public Integer getIncrement()
      {
         return 2;
      }
   }
}
