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

package com.kingsrook.sampleapp;


import java.io.InputStreamReader;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import com.kingsrook.qqq.api.model.APIVersion;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaData;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataContainer;
import com.kingsrook.qqq.api.model.metadata.ApiInstanceMetaDataProvider;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.scripts.RecordScriptTestInterface;
import com.kingsrook.qqq.backend.core.actions.scripts.RunAdHocRecordScriptAction;
import com.kingsrook.qqq.backend.core.actions.scripts.RunAssociatedScriptAction;
import com.kingsrook.qqq.backend.core.actions.scripts.StoreAssociatedScriptAction;
import com.kingsrook.qqq.backend.core.actions.scripts.TestScriptActionInterface;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryFilterLink;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.actions.scripts.ExecuteCodeInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAdHocRecordScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAdHocRecordScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAssociatedScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.RunAssociatedScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.StoreAssociatedScriptOutput;
import com.kingsrook.qqq.backend.core.model.actions.scripts.TestScriptInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.code.AdHocScriptCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.AssociatedScriptCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.PermissionLevel;
import com.kingsrook.qqq.backend.core.model.metadata.permissions.QPermissionRules;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.tables.AssociatedScript;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.savedviews.SavedViewsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.scripts.ScriptsMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSTableBackendDetails;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Stored/draft script lifecycle in a real sample instance and a private H2 schema.
 *******************************************************************************/
@Timeout(30)
class SampleAssociatedScriptAcceptanceTest
{
   private static final AtomicInteger NEXT_SCRIPT_ID = new AtomicInteger(1000000);



   /*******************************************************************************
    ** Store advances the current pointer and retains exact historical code/files.
    *******************************************************************************/
   @Test
   void testStoreRevisionsAndRunCurrentAssociatedScript() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         String firstCode = "logger.log('revision-one'); return greeting + ':' + count;";
         StoreAssociatedScriptOutput first = fixture.store(firstCode, null);
         assertEquals(1, first.getScriptRevisionSequenceNo());
         assertEquals(List.of(List.of(String.valueOf(first.getScriptId()))), fixture.rows("SELECT associated_script_id FROM person WHERE id=1"));
         assertEquals(List.of(List.of("1", "Initial version", "Script Tester", firstCode)), fixture.rows("SELECT r.sequence_no,r.commit_message,r.author,f.contents FROM script_revision r JOIN script_revision_file f ON f.script_revision_id=r.id ORDER BY r.sequence_no"));
         RunAssociatedScriptOutput run = fixture.runAssociated(Map.of("greeting", "Hello", "count", 3));
         assertEquals("Hello:3", run.getOutput());
         assertEquals(first.getScriptRevisionId(), run.getScriptRevisionId());
         String secondCode = "return greeting + ':' + (count * 2);";
         StoreAssociatedScriptOutput second = fixture.store(secondCode, "Double count");
         assertEquals(first.getScriptId(), second.getScriptId());
         assertEquals(2, second.getScriptRevisionSequenceNo());
         assertEquals(List.of(List.of(String.valueOf(second.getScriptRevisionId()))), fixture.rows("SELECT current_script_revision_id FROM script"));
         assertEquals(List.of(List.of("1", firstCode), List.of("2", secondCode)), fixture.rows("SELECT r.sequence_no,f.contents FROM script_revision r JOIN script_revision_file f ON f.script_revision_id=r.id ORDER BY r.sequence_no"));
         run = fixture.runAssociated(Map.of("greeting", "Hello", "count", 3));
         assertEquals("Hello:6", run.getOutput());
         assertEquals(second.getScriptRevisionId(), run.getScriptRevisionId());
         assertEquals(List.of(List.of("2")), fixture.rows("SELECT COUNT(*) FROM script_log WHERE had_error=FALSE"));
      }
   }



   /*******************************************************************************
    ** The registered Test process runs draft text without replacing saved code/logs.
    *******************************************************************************/
   @Test
   void testAssociatedDraftReturnsOutputWithoutPersistingRevisionOrLogs() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         StoreAssociatedScriptOutput stored = fixture.store("return 'saved';", "Saved source");
         RunProcessOutput tested = fixture.testDraft(stored.getScriptId(), "logger.log('draft-only'); return greeting + ':' + (count * 2);", Map.of("greeting", "Draft", "count", 4));
         assertEquals("Draft:8", tested.getValues().get("outputObject"));
         List<?> lines = (List<?>) tested.getValues().get("scriptLogLines");
         assertEquals(1, lines.size());
         assertEquals("draft-only", ((QRecord) lines.get(0)).getValueString("text"));
         assertEquals(List.of(List.of("1", "return 'saved';")), fixture.rows("SELECT r.sequence_no,f.contents FROM script_revision r JOIN script_revision_file f ON f.script_revision_id=r.id"));
         assertEquals(List.of(List.of("0")), fixture.rows("SELECT COUNT(*) FROM script_log"));
         assertEquals(List.of(List.of("0")), fixture.rows("SELECT COUNT(*) FROM script_log_line"));
         assertEquals("saved", fixture.runAssociated(Map.of()).getOutput());
      }
   }



   /*******************************************************************************
    ** Draft record mutations are transient; explicit write isolation is open in #849.
    *******************************************************************************/
   @Test
   void testRecordDraftLocalMutationDoesNotPersist() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         Integer scriptId = fixture.storeRecordScript("return 'saved';");
         RunProcessOutput output = fixture.testDraft(scriptId, "records.get(0).setValue('firstName', 'Draft write'); logger.log(records.get(0).getValueString('firstName'));", Map.of("recordPrimaryKeyList", "1"));
         assertEquals("Draft write", ((QRecord) ((List<?>) output.getValues().get("scriptLogLines")).get(0)).getValueString("text"));
         assertEquals(List.of(List.of("0")), fixture.rows("SELECT COUNT(*) FROM script_log"));
         assertEquals(List.of(List.of("1")), fixture.rows("SELECT COUNT(*) FROM script_revision"));
         assertEquals(List.of(List.of("Avery")), fixture.rows("SELECT first_name FROM person WHERE id=1"));
      }
   }



   /*******************************************************************************
    ** Ad-hoc inputs/output and local mutations are distinct from explicit persistence.
    *******************************************************************************/
   @Test
   void testAdHocRecordInputsOutputsAndExplicitPersistence() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         Integer scriptId = fixture.storeRecordScript("records.get(0).setValue('firstName', prefix); return records.size() + ':' + records.get(0).getValueString('firstName');");
         RunAdHocRecordScriptOutput output = fixture.runRecord(scriptId, List.of(1, 2), Map.of("prefix", "Transient"));
         assertTrue(output.getException().isEmpty(), output.getException().toString());
         assertEquals("2:Transient", output.getOutput());
         assertEquals(List.of(List.of("Avery"), List.of("Blair")), fixture.rows("SELECT first_name FROM person WHERE id IN(1,2) ORDER BY id"));
         fixture.store("qqq.update('person', qqq.newRecord().withValue('id', records.get(0).getValueInteger('id')).withValue('firstName', prefix)); return 'written';", "Explicit write");
         output = fixture.runRecord(scriptId, List.of(1), Map.of("prefix", "Persisted"));
         assertTrue(output.getException().isEmpty(), output.getException().toString());
         assertEquals("written", output.getOutput());
         assertEquals(List.of(List.of("Persisted"), List.of("Blair")), fixture.rows("SELECT first_name FROM person WHERE id IN(1,2) ORDER BY id"));
         assertEquals(List.of(List.of("2")), fixture.rows("SELECT COUNT(*) FROM script_log WHERE had_error=FALSE"));
      }
   }



   /*******************************************************************************
    ** The public Run process executes selected rows in stored two-record batches.
    *******************************************************************************/
   @Test
   void testRunRecordProcessPersistsSelectedRecordsAndSummarizesBatches() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         Integer scriptId = fixture.storeRecordScript("for each (var record in records) { qqq.update('person', qqq.newRecord().withValue('id', record.getValueInteger('id')).withValue('lastName', 'Processed')); } logger.log('batch-size=' + records.size()); return records.size();");
         RunProcessOutput output = fixture.process(ScriptsMetaDataProvider.RUN_RECORD_SCRIPT_PROCESS_NAME, Map.of("tableName", "person", "scriptId", scriptId, "recordIds", "1,2,3"));
         assertEquals(List.of(List.of("1", "Processed"), List.of("2", "Processed"), List.of("3", "Processed"), List.of("4", "Sample"), List.of("5", "Sample")), fixture.rows("SELECT id,last_name FROM person ORDER BY id"));
         assertEquals(List.of(List.of("2")), fixture.rows("SELECT COUNT(*) FROM script_log WHERE had_error=FALSE"));
         assertEquals(List.of(List.of("batch-size=1"), List.of("batch-size=2")), fixture.rows("SELECT text FROM script_log_line ORDER BY text"));
         List<?> summary = (List<?>) output.getValues().get("processResults");
         assertTrue(summary.stream().anyMatch(line -> line instanceof ProcessSummaryLine counted && counted.getCount().equals(3) && counted.getMessage().contains("had the script ran")), summary.toString());
         assertTrue(summary.stream().anyMatch(line -> line instanceof ProcessSummaryFilterLink link && link.getStatus() == Status.OK && link.getMessage().equals("Created 2 Successful Script Logs")), summary.toString());
      }
   }



   /*******************************************************************************
    ** Invalid draft/code parameters fail without storing a draft or changing records.
    *******************************************************************************/
   @Test
   void testInvalidDraftCodeParametersAndAssociatedReference() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         Integer scriptId = fixture.storeRecordScript("return 'saved';");
         assertThat(assertThrows(QException.class, () -> fixture.testDraft(scriptId, "return (;", Map.of("recordPrimaryKeyList", "1"))))
            .hasStackTraceContaining("QCodeException");
         assertThat(assertThrows(QException.class, () -> fixture.testDraft(scriptId, "return 'unused';", Map.of())))
            .hasStackTraceContaining("Record primary key list was not given.");
         assertThat(assertThrows(QException.class, () -> fixture.testDraft(scriptId, "return 'unused';", Map.of("recordPrimaryKeyList", "999999"))))
            .hasStackTraceContaining("No records were found by the given primary keys.");
         StoreAssociatedScriptInput invalid = new StoreAssociatedScriptInput();
         invalid.setTableName("person");
         invalid.setRecordPrimaryKey(1);
         invalid.setFieldName("firstName");
         invalid.setCode("return 'unused';");
         assertThat(assertThrows(QException.class, () -> new StoreAssociatedScriptAction().run(invalid, new StoreAssociatedScriptOutput())))
            .hasMessageContaining("not an associated script field");
         assertEquals(List.of(List.of("1")), fixture.rows("SELECT COUNT(*) FROM script_revision"));
         assertEquals(List.of(List.of("0")), fixture.rows("SELECT COUNT(*) FROM script_log"));
         assertEquals(List.of(List.of("Avery")), fixture.rows("SELECT first_name FROM person WHERE id=1"));
      }
   }



   /*******************************************************************************
    ** Script writes use the caller's actual permissions; granting EDIT enables retry.
    *******************************************************************************/
   @Test
   void testRecordScriptPermissionDeniedThenGrantedWrite() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         Integer scriptId = fixture.storeRecordScript("qqq.update('person', qqq.newRecord().withValue('id', 1).withValue('firstName', 'Allowed')); return 'written';");
         QContext.getQInstance().getTable("person").withPermissionRules(new QPermissionRules().withLevel(PermissionLevel.READ_INSERT_EDIT_DELETE_PERMISSIONS));
         QContext.getQSession().withPermissions("person.read");
         RunAdHocRecordScriptOutput denied = fixture.runRecord(scriptId, List.of(1), Map.of());
         assertTrue(denied.getException().isPresent());
         assertThat(denied.getException().get()).hasStackTraceContaining("Permission denied");
         assertEquals(List.of(List.of("Avery")), fixture.rows("SELECT first_name FROM person WHERE id=1"));
         assertEquals(List.of(List.of("1")), fixture.rows("SELECT COUNT(*) FROM script_log WHERE had_error=TRUE"));
         QContext.getQSession().withPermissions("person.read", "person.edit");
         RunAdHocRecordScriptOutput allowed = fixture.runRecord(scriptId, List.of(1), Map.of());
         assertTrue(allowed.getException().isEmpty(), allowed.getException().toString());
         assertEquals("written", allowed.getOutput());
         assertEquals(List.of(List.of("Allowed")), fixture.rows("SELECT first_name FROM person WHERE id=1"));
         assertEquals(List.of(List.of("1")), fixture.rows("SELECT COUNT(*) FROM script_log WHERE had_error=FALSE"));
      }
   }



   /*******************************************************************************
    ** Store checks the existing script's WRITE lock and retains the old revision.
    *******************************************************************************/
   @Test
   void testStoreRevisionDeniedByRecordLockThenAuthorized() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         StoreAssociatedScriptOutput original = fixture.store("return 'original';", "Original");
         QInstance instance = QContext.getQInstance();
         instance.addSecurityKeyType(new QSecurityKeyType().withName("scriptOwner"));
         instance.getTable("script").withRecordSecurityLock(new RecordSecurityLock().withFieldName("id")
            .withSecurityKeyType("scriptOwner").withLockScope(RecordSecurityLock.LockScope.WRITE));
         assertThrows(QPermissionDeniedException.class, () -> fixture.store("return 'denied';", "Denied"));
         assertEquals(List.of(List.of("1", "return 'original';")), fixture.rows("SELECT r.sequence_no,f.contents FROM script_revision r JOIN script_revision_file f ON f.script_revision_id=r.id"));
         assertEquals(List.of(List.of(String.valueOf(original.getScriptRevisionId()))), fixture.rows("SELECT current_script_revision_id FROM script"));
         QContext.getQSession().withSecurityKeyValue("scriptOwner", original.getScriptId());
         StoreAssociatedScriptOutput allowed = fixture.store("return 'allowed';", "Allowed");
         assertEquals(2, allowed.getScriptRevisionSequenceNo());
         assertEquals("allowed", fixture.runAssociated(Map.of()).getOutput());
      }
   }



   /*******************************************************************************
    ** Store accepts source text; Run rejects invalid syntax and logs runtime errors.
    *******************************************************************************/
   @Test
   void testStoredSyntaxAndRuntimeFailuresThenValidRevisionRecovery() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         fixture.store("return (;", "Invalid syntax is stored, not compiled");
         assertThat(assertThrows(QException.class, () -> fixture.runAssociated(Map.of())))
            .hasStackTraceContaining("QCodeException");
         fixture.store("throw new Error('associated-runtime-marker');", "Runtime failure");
         assertThat(assertThrows(QException.class, () -> fixture.runAssociated(Map.of())))
            .hasStackTraceContaining("associated-runtime-marker");
         assertEquals(List.of(List.of("2")), fixture.rows("SELECT COUNT(*) FROM script_log WHERE had_error=TRUE"));
         assertEquals(List.of(List.of("1")), fixture.rows("SELECT COUNT(*) FROM script_log WHERE error LIKE '%associated-runtime-marker%'"));
         StoreAssociatedScriptOutput recovery = fixture.store("return 'recovered';", "Recovered");
         assertEquals(3, recovery.getScriptRevisionSequenceNo());
         assertEquals("recovered", fixture.runAssociated(Map.of()).getOutput());
         assertEquals(List.of(List.of("Avery")), fixture.rows("SELECT first_name FROM person WHERE id=1"));
      }
   }



   /*******************************************************************************
    ** A failed record process exposes ERROR log links, distinct from attempted count.
    *******************************************************************************/
   @Test
   void testRunRecordProcessReportsScriptFailuresWithoutImplicitWrites() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         Integer scriptId = fixture.storeRecordScript("records.get(0).setValue('firstName', 'Transient failure'); throw new Error('record-runtime-marker');");
         RunProcessOutput output = fixture.process(ScriptsMetaDataProvider.RUN_RECORD_SCRIPT_PROCESS_NAME, Map.of("tableName", "person", "scriptId", scriptId, "recordIds", "1,2,3"));
         assertEquals(List.of(List.of("Avery"), List.of("Blair"), List.of("Casey")), fixture.rows("SELECT first_name FROM person WHERE id IN(1,2,3) ORDER BY id"));
         assertEquals(List.of(List.of("2")), fixture.rows("SELECT COUNT(*) FROM script_log WHERE had_error=TRUE AND error LIKE '%record-runtime-marker%'"));
         assertEquals(List.of(List.of("0")), fixture.rows("SELECT COUNT(*) FROM script_log WHERE had_error=FALSE"));
         List<?> summary = (List<?>) output.getValues().get("processResults");
         assertTrue(summary.stream().anyMatch(line -> line instanceof ProcessSummaryFilterLink link && link.getStatus() == Status.ERROR && link.getMessage().equals("Created 2 Script Logs with Errors")), summary.toString());
         assertTrue(summary.stream().noneMatch(line -> line instanceof ProcessSummaryFilterLink link && link.getStatus() == Status.OK), summary.toString());
      }
   }



   /*******************************************************************************
    ** Explicit revision IDs remain pinned; missing/unknown references report failure.
    *******************************************************************************/
   @Test
   void testAdHocPinnedRevisionAndInvalidReferences() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         StoreAssociatedScriptOutput first = fixture.store("return 'historical';", "First");
         fixture.store("return 'current';", "Second");
         RunAdHocRecordScriptOutput current = fixture.runRecord(first.getScriptId(), List.of(1), Map.of());
         assertTrue(current.getException().isEmpty(), current.getException().toString());
         assertEquals("current", current.getOutput());
         RunAdHocRecordScriptOutput historical = fixture.runRecord(new AdHocScriptCodeReference().withScriptRevisionId(first.getScriptRevisionId()), List.of(1), Map.of());
         assertTrue(historical.getException().isEmpty(), historical.getException().toString());
         assertEquals("historical", historical.getOutput());
         RunAdHocRecordScriptOutput missing = fixture.runRecord(new AdHocScriptCodeReference(), List.of(1), Map.of());
         assertTrue(missing.getException().isPresent());
         assertThat(missing.getException().get()).hasMessageContaining("Code reference did not contain");
         RunAdHocRecordScriptOutput unknown = fixture.runRecord(new AdHocScriptCodeReference().withScriptRevisionId(-1), List.of(1), Map.of());
         assertTrue(unknown.getException().isPresent());
         assertThat(unknown.getException().get()).hasMessage("Script revision was not found.");
         assertEquals(List.of(List.of("2")), fixture.rows("SELECT COUNT(*) FROM script_revision"));
         assertEquals(List.of(List.of("2")), fixture.rows("SELECT COUNT(*) FROM script_log"));
      }
   }



   /*******************************************************************************
    ** Application-defined associated-script tester uses the real default executor.
    *******************************************************************************/
   public static class ParameterTester implements TestScriptActionInterface
   {
      /*******************************************************************************
       ** The real Test process serializes user form values; keep them as inputs.
       *******************************************************************************/
      @Override
      public void setupTestScriptInput(TestScriptInput input, ExecuteCodeInput output) throws QException
      {
         output.setInput(input.getInputValues());
      }



      /*******************************************************************************
       ** Form inputs are owned by this sample script type.
       *******************************************************************************/
      @Override
      public List<QFieldMetaData> getTestInputFields()
      {
         return (List.of(new QFieldMetaData("greeting", QFieldType.STRING), new QFieldMetaData("count", QFieldType.INTEGER)));
      }



      /*******************************************************************************
       ** This script type returns a scalar output.
       *******************************************************************************/
      @Override
      public List<QFieldMetaData> getTestOutputFields()
      {
         return (List.of(new QFieldMetaData("result", QFieldType.STRING)));
      }
   }



   /*******************************************************************************
    ** Own every database/context; script IDs avoid the action's shared ID memoization.
    *******************************************************************************/
   private static class Fixture implements AutoCloseable
   {
      private final CapturedContext previousContext = QContext.capture();
      private final Map<String, Serializable> previousObjects = QContext.getObjects();
      private QInstance instance;
      private Connection anchor;



      /*******************************************************************************
       ** Use existing metadata producers and explicit owned SQL, without a launcher.
       *******************************************************************************/
      private Fixture() throws Exception
      {
         try
         {
            ConnectionManager.resetConnectionProviders();
            instance = SampleMetaDataProvider.defineTestInstance();
            RDBMSBackendMetaData backend = (RDBMSBackendMetaData) instance.getBackend("rdbms");
            backend.setDatabaseName("sample_associated_scripts_" + UUID.randomUUID());
            anchor = ConnectionManager.getConnection(backend);
            for(String resource : List.of("/prime-test-database.sql", "/database/associated-scripts-acceptance.sql"))
            {
               try(InputStreamReader reader = new InputStreamReader(Fixture.class.getResourceAsStream(resource), StandardCharsets.UTF_8))
               {
                  RunScript.execute(anchor, reader);
               }
            }
            try(Statement statement = anchor.createStatement())
            {
               statement.execute("ALTER TABLE script ALTER COLUMN id RESTART WITH " + NEXT_SCRIPT_ID.getAndAdd(100));
            }
            new ScriptsMetaDataProvider().defineAll(instance, "rdbms", table ->
            {
               table.setBackendDetails(new RDBMSTableBackendDetails().withTableName(QInstanceEnricher.inferBackendName(table.getName())));
               QInstanceEnricher.setInferredFieldBackendNames(table);
            });
            instance.withSupplementalMetaData(new ApiInstanceMetaDataContainer().withApiInstanceMetaData(new ApiInstanceMetaData()
               .withName("scriptSample").withPath("/script-api/").withLabel("Script Sample")
               .withDescription("Owned script acceptance API metadata").withContactEmail("scripts@example.test")
               .withCurrentVersion(new APIVersion("2026.Q3")).withSupportedVersions(List.of(new APIVersion("2026.Q3")))));
            ApiInstanceMetaDataProvider.definePossibleValueSourcesForApiNameAndVersion(instance);
            new SavedViewsMetaDataProvider().withIsShareSavedViewEnabled(false).defineAll(instance, "rdbms", null);
            QTableMetaData person = instance.getTable("person");
            person.withField(new QFieldMetaData("associatedScriptId", QFieldType.INTEGER).withBackendName("associated_script_id"));
            person.withSection(new QFieldSection().withName("script").withLabel("Script").withTier(Tier.T3).withFieldNames(List.of("associatedScriptId")));
            person.withAssociatedScript(new AssociatedScript().withFieldName("associatedScriptId").withScriptTypeId(1).withScriptTester(new QCodeReference(ParameterTester.class)));
            EsbTableMetaData.of(person).setPublications(List.of());
            QContext.setObjects(null);
            QContext.init(instance, new QSession().withUser(new QUser().withIdReference("script-tester").withFullName("Script Tester")));
            List<QRecord> types = InsertAction.executeForRecords(new InsertInput("scriptType").withRecords(List.of(
               new QRecord().withValue("id", 1).withValue("name", "Associated sample script").withValue("testScriptInterfaceName", ParameterTester.class.getName()),
               new QRecord().withValue("id", 2).withValue("name", "Record Script").withValue("testScriptInterfaceName", RecordScriptTestInterface.class.getName()))));
            for(QRecord type : types)
            {
               assertTrue(type.getErrors().isEmpty(), type.getErrors().toString());
            }
         }
         catch(Exception | Error failure)
         {
            try
            {
               close();
            }
            catch(Exception cleanupFailure)
            {
               failure.addSuppressed(cleanupFailure);
            }
            throw failure;
         }
      }



      /*******************************************************************************
       ** Store through the associated-record action, never via native script inserts.
       *******************************************************************************/
      private StoreAssociatedScriptOutput store(String code, String message) throws QException
      {
         StoreAssociatedScriptInput input = new StoreAssociatedScriptInput();
         input.setTableName("person");
         input.setRecordPrimaryKey(1);
         input.setFieldName("associatedScriptId");
         input.setCode(code);
         input.setCommitMessage(message);
         StoreAssociatedScriptOutput output = new StoreAssociatedScriptOutput();
         new StoreAssociatedScriptAction().run(input, output);
         return (output);
      }



      /*******************************************************************************
       ** Each call resolves the current revision afresh, using the real JS provider.
       *******************************************************************************/
      private RunAssociatedScriptOutput runAssociated(Map<String, Serializable> values) throws QException
      {
         RunAssociatedScriptInput input = new RunAssociatedScriptInput();
         input.setTableName("person");
         input.setInputValues(values);
         input.setCodeReference(new AssociatedScriptCodeReference().withRecordTable("person").withRecordPrimaryKey(1).withFieldName("associatedScriptId"));
         RunAssociatedScriptOutput output = new RunAssociatedScriptOutput();
         new RunAssociatedScriptAction().run(input, output);
         return (output);
      }



      /*******************************************************************************
       ** Store through the normal revision action, then select the native record type.
       *******************************************************************************/
      private Integer storeRecordScript(String code) throws QException
      {
         Integer scriptId = store(code, "Record script").getScriptId();
         QRecord record = new QRecord().withValue("id", scriptId).withValue("scriptTypeId", 2).withValue("tableName", "person").withValue("maxBatchSize", 2);
         new UpdateAction().execute(new UpdateInput("script").withRecords(List.of(record)));
         assertTrue(record.getErrors().isEmpty(), record.getErrors().toString());
         return (scriptId);
      }



      /*******************************************************************************
       ** Exercise current-revision selection plus native primary-key record inputs.
       *******************************************************************************/
      private RunAdHocRecordScriptOutput runRecord(Integer scriptId, List<Serializable> ids, Map<String, Serializable> values) throws QException
      {
         return (runRecord(new AdHocScriptCodeReference().withScriptId(scriptId), ids, values));
      }



      /*******************************************************************************
       ** Allow an explicit revision reference without replacing current-revision state.
       *******************************************************************************/
      private RunAdHocRecordScriptOutput runRecord(AdHocScriptCodeReference reference, List<Serializable> ids, Map<String, Serializable> values) throws QException
      {
         RunAdHocRecordScriptInput input = new RunAdHocRecordScriptInput();
         input.setTableName("person");
         input.setRecordPrimaryKeyList(ids);
         input.setInputValues(values);
         input.setCodeReference(reference);
         RunAdHocRecordScriptOutput output = new RunAdHocRecordScriptOutput();
         new RunAdHocRecordScriptAction().run(input, output);
         return (output);
      }



      /*******************************************************************************
       ** Exercise the registered public process with isolated, removed process state.
       *******************************************************************************/
      private RunProcessOutput process(String name, Map<String, Serializable> values) throws Exception
      {
         UUID id = UUID.randomUUID();
         RunProcessInput input = new RunProcessInput().withProcessName(name);
         input.setProcessUUID(id.toString());
         input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
         input.setValues(new HashMap<>(values));
         try
         {
            return (new RunProcessAction().execute(input));
         }
         finally
         {
            RunProcessAction.getStateProvider().remove(new UUIDAndTypeStateKey(id, StateType.PROCESS_STATUS));
         }
      }



      /*******************************************************************************
       ** Test an unsaved single-file draft through the metadata-registered process.
       *******************************************************************************/
      private RunProcessOutput testDraft(Integer scriptId, String code, Map<String, Serializable> values) throws Exception
      {
         Map<String, Serializable> inputs = new HashMap<>(values);
         inputs.put("scriptId", scriptId);
         inputs.put("fileNames", "script");
         inputs.put("fileContents:script", code);
         return (process(ScriptsMetaDataProvider.TEST_SCRIPT_PROCESS_NAME, inputs));
      }



      /*******************************************************************************
       ** Observe persisted values independently of script execution and QQQ reads.
       *******************************************************************************/
      private List<List<String>> rows(String sql) throws Exception
      {
         List<List<String>> rows = new ArrayList<>();
         try(Statement statement = anchor.createStatement(); ResultSet result = statement.executeQuery(sql))
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
         return (rows);
      }



      /*******************************************************************************
       ** Always restore context and close the owned anchor after database shutdown.
       *******************************************************************************/
      @Override
      public void close() throws Exception
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
            QContext.init(previousContext);
            QContext.setObjects(previousObjects);
            assertSame(previousContext.qInstance(), QContext.capture().qInstance());
            assertSame(previousContext.qSession(), QContext.capture().qSession());
            assertSame(previousObjects, QContext.getObjects());
         }
      }
   }
}
