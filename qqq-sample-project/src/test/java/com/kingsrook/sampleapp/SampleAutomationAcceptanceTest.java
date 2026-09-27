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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.kingsrook.qqq.backend.core.actions.automation.RecordAutomationHandlerInterface;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.CapturedContext;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QInstanceValidationException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.automation.RecordAutomationInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.automation.PollingAutomationProviderMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.DynamicDefaultValueBehavior;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.TablesPossibleValueSourceMetaDataProvider;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.AutomationStatusTracking;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.AutomationStatusTrackingType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.QTableAutomationDetails;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TableAutomationAction;
import com.kingsrook.qqq.backend.core.model.metadata.tables.automation.TriggerEvent;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.processes.implementations.automation.HealBadRecordAutomationStatusesProcessStep;
import com.kingsrook.qqq.backend.core.processes.implementations.automation.RunTableAutomationsProcessStep;
import com.kingsrook.qqq.backend.core.state.StateType;
import com.kingsrook.qqq.backend.core.state.UUIDAndTypeStateKey;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.esb.model.EsbTableMetaData;
import com.kingsrook.sampleapp.metadata.SampleMetaDataProvider;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


/*******************************************************************************
 ** Public sample record/process automation with independent persisted SQL oracles.
 *******************************************************************************/
@Timeout(30)
class SampleAutomationAcceptanceTest
{
   private static final String PROVIDER = "sampleAcceptancePolling";
   private static final String PROCESS = "sampleAutomationTrace";
   private static final String OWNED_PROCESS_IDS = "sampleAutomationProcessIds";



   /*******************************************************************************
    ** User writes schedule work; handler writes must not recursively reschedule it.
    *******************************************************************************/
   @Test
   void testInsertUpdateAndCompletedWorkIsNotRepeated() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         fixture.actions(code("insert", TriggerEvent.POST_INSERT, "I"),
            code("update", TriggerEvent.POST_UPDATE, "U"));
         int id = fixture.insert("Eligible");
         assertEquals(List.of(List.of("1", "", "0")), fixture.rows("SELECT automation_status,automation_trace,automation_attempts FROM person WHERE id=" + id));
         fixture.update(id, "Before insert poll");
         assertEquals(List.of(List.of("1")), fixture.rows("SELECT automation_status FROM person WHERE id=" + id));
         fixture.poll();
         assertEquals(List.of(List.of("7", "I", "1")), fixture.rows("SELECT automation_status,automation_trace,automation_attempts FROM person WHERE id=" + id));
         List<List<String>> completed = fixture.rows("SELECT * FROM person ORDER BY id");
         fixture.poll();
         assertEquals(completed, fixture.rows("SELECT * FROM person ORDER BY id"));
         fixture.update(id, "After insert poll");
         assertEquals(List.of(List.of("4")), fixture.rows("SELECT automation_status FROM person WHERE id=" + id));
         fixture.poll();
         assertEquals(List.of(List.of("7", "IU", "2")), fixture.rows("SELECT automation_status,automation_trace,automation_attempts FROM person WHERE id=" + id));
         completed = fixture.rows("SELECT * FROM person ORDER BY id");
         fixture.poll();
         assertEquals(completed, fixture.rows("SELECT * FROM person ORDER BY id"));
      }
   }



   /*******************************************************************************
    ** Characterize the known false-success boundary; this is not provider rejection.
    *******************************************************************************/
   @Test
   void testUnknownRuntimeProviderLeavesPendingWorkDespiteSuccess() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         fixture.actions(code("insert", TriggerEvent.POST_INSERT, "I"));
         int id = fixture.insert("Eligible");
         RunProcessOutput output = fixture.process(RunTableAutomationsProcessStep.NAME,
            Map.of("tableName", "person", "automationProviderName", "missingProvider"));
         assertEquals(List.of(List.of("1", "", "0")), fixture.rows("SELECT automation_status,automation_trace,automation_attempts FROM person WHERE id=" + id));
         assertTrue(output.getException().isEmpty());
         assertEquals("true", output.getValueString("ok"));
         fixture.poll();
         assertEquals(List.of(List.of("7", "I", "1")), fixture.rows("SELECT automation_status,automation_trace,automation_attempts FROM person WHERE id=" + id));
      }
   }



   /*******************************************************************************
    ** Reverse declaration order, persisted re-filtering and actual Person/Pet joins.
    *******************************************************************************/
   @Test
   void testPriorityFiltersAndAssociationLoading() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         fixture.actions(code("second", TriggerEvent.POST_UPDATE, "B").withPriority(20)
               .withFilter(new QQueryFilter().withCriteria(new QFilterCriteria("automationTrace", QCriteriaOperator.EQUALS, "A")))
               .withIncludeRecordAssociations(true),
            code("first", TriggerEvent.POST_UPDATE, "A").withPriority(10)
               .withFilter(new QQueryFilter().withCriteria(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Eligible"))));
         fixture.update(1, "Eligible");
         fixture.update(2, "Excluded");
         assertEquals(List.of(List.of("1", "4"), List.of("2", "4")), fixture.rows("SELECT id,automation_status FROM person WHERE id IN(1,2) ORDER BY id"));
         assertEquals(List.of(List.of("4")), fixture.rows("SELECT COUNT(*) FROM pet WHERE person_id=1"));
         fixture.poll();
         assertEquals(List.of(List.of("1", "7", "AB", "2", "4"), List.of("2", "7", "", "0", "-1")),
            fixture.rows("SELECT id,automation_status,automation_trace,automation_attempts,automation_pet_count FROM person WHERE id IN(1,2) ORDER BY id"));
         assertEquals(List.of(List.of("3")), fixture.rows("SELECT COUNT(*) FROM person WHERE id IN(3,4,5) AND automation_attempts=0 AND automation_status=7"));
      }
   }



   /*******************************************************************************
    ** The real child process callback must select only this page of pending rows.
    *******************************************************************************/
   @Test
   void testProcessHandlerRespectsBatchSizeAndSelectedRecords() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         fixture.actions(new TableAutomationAction().withName("process").withTriggerEvent(TriggerEvent.POST_INSERT).withProcessName(PROCESS));
         for(int i = 0; i < 5; i++)
         {
            fixture.insert("Eligible");
         }
         assertEquals(List.of(List.of("5")), fixture.rows("SELECT COUNT(*) FROM person WHERE automation_status=1"));
         fixture.poll();
         assertEquals(List.of(List.of("1", "1"), List.of("2", "4")),
            fixture.rows("SELECT automation_batch_size,COUNT(*) FROM person WHERE automation_trace='P' GROUP BY automation_batch_size ORDER BY automation_batch_size"));
         assertEquals(List.of(List.of("5")), fixture.rows("SELECT COUNT(*) FROM person WHERE automation_status=7 AND automation_trace='P' AND automation_attempts=1 AND automation_observed_status=2"));
         assertEquals(List.of(List.of("5")), fixture.rows("SELECT COUNT(*) FROM person WHERE id<=5 AND automation_trace='' AND automation_attempts=0"));
         List<List<String>> completed = fixture.rows("SELECT * FROM person ORDER BY id");
         fixture.poll();
         assertEquals(completed, fixture.rows("SELECT * FROM person ORDER BY id"));
      }
   }



   /*******************************************************************************
    ** Failures are page-wide and partial writes are not rolled back or exactly-once.
    *******************************************************************************/
   @Test
   void testFailedInsertBatchRequiresRecoveryAndRepeatsPartialWork() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         TableAutomationAction failing = code("failure", TriggerEvent.POST_INSERT, "F").withPriority(10)
            .withValues(Map.of("marker", "F", "failAfterWrite", true))
            .withFilter(new QQueryFilter().withCriteria(new QFilterCriteria("lastName", QCriteriaOperator.EQUALS, "Fail target")));
         fixture.actions(failing, code("later", TriggerEvent.POST_INSERT, "L").withPriority(20));
         fixture.insert("Fail target");
         fixture.insert("Eligible");
         fixture.poll();
         assertEquals(List.of(List.of("3", "FL", "2", "2"), List.of("3", "L", "1", "2")),
            fixture.rows("SELECT automation_status,automation_trace,automation_attempts,automation_observed_status FROM person WHERE id>5 ORDER BY id"));
         List<List<String>> failed = fixture.rows("SELECT * FROM person ORDER BY id");
         fixture.poll();
         assertEquals(failed, fixture.rows("SELECT * FROM person ORDER BY id"));
         RunBackendStepInput previewInput = new RunBackendStepInput();
         previewInput.setStepName("preview");
         previewInput.addValue("tableName", "person");
         RunBackendStepOutput preview = new RunBackendStepOutput();
         new HealBadRecordAutomationStatusesProcessStep().run(previewInput, preview);
         assertEquals(2, preview.getValueInteger("totalRecordsToUpdate"));
         assertEquals(failed, fixture.rows("SELECT * FROM person ORDER BY id"));
         fixture.heal(2);
         assertEquals(List.of(List.of("1", "FL", "2"), List.of("1", "L", "1")),
            fixture.rows("SELECT automation_status,automation_trace,automation_attempts FROM person WHERE id>5 ORDER BY id"));
         failing.setValues(Map.of("marker", "F"));
         fixture.poll();
         assertEquals(List.of(List.of("7", "FLFL", "4"), List.of("7", "LL", "2")),
            fixture.rows("SELECT automation_status,automation_trace,automation_attempts FROM person WHERE id>5 ORDER BY id"));
         List<List<String>> recovered = fixture.rows("SELECT * FROM person ORDER BY id");
         fixture.heal(0);
         fixture.poll();
         assertEquals(recovered, fixture.rows("SELECT * FROM person ORDER BY id"));
      }
   }



   /*******************************************************************************
    ** Child-process exceptions become FAILED_UPDATE; healing preserves the event.
    *******************************************************************************/
   @Test
   void testFailedUpdateProcessRecoversWithoutImplicitRetry() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         fixture.actions(new TableAutomationAction().withName("process").withTriggerEvent(TriggerEvent.POST_UPDATE).withProcessName(PROCESS));
         fixture.update(1, "Fail after write");
         fixture.poll();
         assertEquals(List.of(List.of("6", "P", "1", "5")),
            fixture.rows("SELECT automation_status,automation_trace,automation_attempts,automation_observed_status FROM person WHERE id=1"));
         List<List<String>> failed = fixture.rows("SELECT * FROM person ORDER BY id");
         fixture.poll();
         assertEquals(failed, fixture.rows("SELECT * FROM person ORDER BY id"));
         fixture.sql("UPDATE person SET last_name='Repaired' WHERE id=1");
         fixture.heal(1);
         assertEquals(List.of(List.of("4", "P", "1")), fixture.rows("SELECT automation_status,automation_trace,automation_attempts FROM person WHERE id=1"));
         fixture.poll();
         assertEquals(List.of(List.of("7", "PP", "2", "5")),
            fixture.rows("SELECT automation_status,automation_trace,automation_attempts,automation_observed_status FROM person WHERE id=1"));
         assertEquals(List.of(List.of("4")), fixture.rows("SELECT COUNT(*) FROM person WHERE id>1 AND automation_trace='' AND automation_attempts=0 AND automation_status=7"));
      }
   }



   /*******************************************************************************
    ** Recover stale running and failed states, preserving recent/unknown/null/OK rows.
    *******************************************************************************/
   @Test
   void testRecoveryStaleRunningAndInvalidPersistedStatuses() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         fixture.actions(code("insert", TriggerEvent.POST_INSERT, "I"), code("update", TriggerEvent.POST_UPDATE, "U"));
         for(int id = 6; id <= 9; id++)
         {
            assertEquals(id, fixture.insert("Recovery control"));
         }
         fixture.sql("UPDATE person SET automation_status=CASE id WHEN 1 THEN 3 WHEN 2 THEN 6 WHEN 3 THEN 2 WHEN 4 THEN 5 WHEN 5 THEN 2 WHEN 6 THEN 5 WHEN 7 THEN 999 WHEN 8 THEN NULL ELSE 7 END");
         fixture.sql("UPDATE person SET create_date=CURRENT_TIMESTAMP,modify_date=CURRENT_TIMESTAMP");
         fixture.sql("UPDATE person SET create_date=TIMESTAMP '2000-01-01 00:00:00' WHERE id=3");
         fixture.sql("UPDATE person SET modify_date=TIMESTAMP '2000-01-01 00:00:00' WHERE id=4");
         List<List<String>> protectedRows = fixture.rows("SELECT * FROM person WHERE id>=5 ORDER BY id");
         fixture.heal(4);
         assertEquals(List.of(List.of("1", "1"), List.of("2", "4"), List.of("3", "1"), List.of("4", "4")),
            fixture.rows("SELECT id,automation_status FROM person WHERE id<=4 ORDER BY id"));
         assertEquals(protectedRows, fixture.rows("SELECT * FROM person WHERE id>=5 ORDER BY id"));
         fixture.poll();
         assertEquals(List.of(List.of("1", "7", "I", "1"), List.of("2", "7", "U", "1"), List.of("3", "7", "I", "1"), List.of("4", "7", "U", "1")),
            fixture.rows("SELECT id,automation_status,automation_trace,automation_attempts FROM person WHERE id<=4 ORDER BY id"));
         assertEquals(protectedRows, fixture.rows("SELECT * FROM person WHERE id>=5 ORDER BY id"));
         fixture.heal(0);
         assertEquals(protectedRows, fixture.rows("SELECT * FROM person WHERE id>=5 ORDER BY id"));
      }
   }



   /*******************************************************************************
    ** Startup validation rejects broken provider linkage and an absent status field.
    *******************************************************************************/
   @Test
   void testInvalidProviderAndStatusMetadataRejectedWithoutWrites() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         fixture.actions(code("insert", TriggerEvent.POST_INSERT, "I"));
         int id = fixture.insert("Eligible");
         List<List<String>> before = fixture.rows("SELECT * FROM person ORDER BY id");
         QTableAutomationDetails details = fixture.instance.getTable("person").getAutomationDetails();
         details.setProviderName("missingProvider");
         QInstanceValidationException providerFailure = assertThrows(QInstanceValidationException.class, fixture::validate);
         assertTrue(providerFailure.getMessage().contains("unrecognized providerName: missingProvider"), providerFailure.getMessage());
         assertEquals(before, fixture.rows("SELECT * FROM person ORDER BY id"));
         details.setProviderName(PROVIDER);
         details.getStatusTracking().setFieldName("absentStatusField");
         QInstanceValidationException statusFailure = assertThrows(QInstanceValidationException.class, fixture::validate);
         assertTrue(statusFailure.getMessage().contains("statusTracking field is not a defined field on this table"), statusFailure.getMessage());
         assertEquals(before, fixture.rows("SELECT * FROM person ORDER BY id"));
         details.getStatusTracking().setFieldName("automationStatus");
         fixture.validate();
         fixture.poll();
         assertEquals(List.of(List.of("7", "I", "1")), fixture.rows("SELECT automation_status,automation_trace,automation_attempts FROM person WHERE id=" + id));
      }
   }



   /*******************************************************************************
    ** Missing/unknown table and ambiguous implicit provider fail without work.
    *******************************************************************************/
   @Test
   void testInvalidManualProcessInputsPreservePendingWork() throws Exception
   {
      try(Fixture fixture = new Fixture())
      {
         fixture.actions(code("insert", TriggerEvent.POST_INSERT, "I"));
         int id = fixture.insert("Eligible");
         List<List<String>> before = fixture.rows("SELECT * FROM person ORDER BY id");
         QException missingTable = assertThrows(QException.class, () -> fixture.process(RunTableAutomationsProcessStep.NAME, Map.of()));
         assertTrue(missingTable.getMessage().contains("Missing required input value: tableName"), missingTable.getMessage());
         QException unknownTable = assertThrows(QException.class, () -> fixture.process(RunTableAutomationsProcessStep.NAME, Map.of("tableName", "missingTable")));
         assertTrue(unknownTable.getMessage().contains("Unrecognized table name: missingTable"), unknownTable.getMessage());
         fixture.instance.addAutomationProvider(new PollingAutomationProviderMetaData().withName("otherProvider"));
         fixture.validate();
         QException ambiguousProvider = assertThrows(QException.class, () -> fixture.process(RunTableAutomationsProcessStep.NAME, Map.of("tableName", "person")));
         assertTrue(ambiguousProvider.getMessage().contains("Missing required input value: automationProviderName"), ambiguousProvider.getMessage());
         assertEquals(before, fixture.rows("SELECT * FROM person ORDER BY id"));
         fixture.poll();
         assertEquals(List.of(List.of("7", "I", "1")), fixture.rows("SELECT automation_status,automation_trace,automation_attempts FROM person WHERE id=" + id));
      }
   }



   /*******************************************************************************
    ** Configure an actual code handler with caller-owned marker parameters.
    *******************************************************************************/
   private static TableAutomationAction code(String name, TriggerEvent event, String marker)
   {
      return (new TableAutomationAction().withName(name).withTriggerEvent(event)
         .withCodeReference(new QCodeReference(TraceHandler.class)).withValues(Map.of("marker", marker)));
   }



   /*******************************************************************************
    ** Handler effects use public writes; SQL assertions never reuse this logic.
    *******************************************************************************/
   public static class TraceHandler implements RecordAutomationHandlerInterface
   {
      /*******************************************************************************
       ** Each execution appends an observable marker and increments its attempt count.
       *******************************************************************************/
      @Override
      public void execute(RecordAutomationInput input) throws QException
      {
         List<QRecord> updates = new ArrayList<>();
         for(QRecord record : input.getRecordList())
         {
            updates.add(new QRecord().withValue("id", record.getValue("id"))
               .withValue("automationTrace", record.getValueString("automationTrace") + input.getAction().getValues().get("marker"))
               .withValue("automationAttempts", record.getValueInteger("automationAttempts") + 1)
               .withValue("automationObservedStatus", record.getValueInteger("automationStatus"))
               .withValue("automationBatchSize", input.getRecordList().size())
               .withValue("automationPetCount", record.getAssociatedRecords().containsKey("pets") ? record.getAssociatedRecords().get("pets").size() : -1));
         }
         List<QRecord> results = UpdateAction.executeForRecords(new UpdateInput("person").withRecords(updates));
         for(QRecord result : results)
         {
            if(!result.getErrors().isEmpty())
            {
               throw (new QException("Automation fixture write failed: " + result.getErrors()));
            }
         }
         if(Boolean.TRUE.equals(input.getAction().getValues().get("failAfterWrite")))
         {
            throw (new QException("Owned automation failure after persisted writes"));
         }
      }
   }



   /*******************************************************************************
    ** Test process uses the actual automation callback rather than all table rows.
    *******************************************************************************/
   public static class TraceProcess implements BackendStep
   {
      /*******************************************************************************
       ** Preserve generated child UUIDs and run normal record APIs for the callback.
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         ProcessIds processIds = QContext.getObject(OWNED_PROCESS_IDS, ProcessIds.class).orElseThrow();
         QContext.getActionStack().stream().filter(RunProcessInput.class::isInstance).map(RunProcessInput.class::cast)
            .forEach(process -> processIds.values.add(UUID.fromString(process.getProcessUUID())));
         RecordAutomationInput handlerInput = new RecordAutomationInput();
         handlerInput.setTableName("person");
         handlerInput.setAction(code("process", TriggerEvent.POST_INSERT, "P"));
         handlerInput.setRecordList(new QueryAction().execute(new QueryInput("person").withFilter(input.getCallback().getQueryFilter())).getRecords());
         new TraceHandler().execute(handlerInput);
         if(handlerInput.getRecordList().stream().anyMatch(record -> "Fail after write".equals(record.getValueString("lastName"))))
         {
            throw (new QException("Owned child-process failure after persisted writes"));
         }
      }
   }



   /*******************************************************************************
    ** Context-local ownership list, including UUIDs allocated by child processes.
    *******************************************************************************/
   private static class ProcessIds implements Serializable
   {
      private final List<UUID> values = new ArrayList<>();
   }



   /*******************************************************************************
    ** Owns a unique H2 database, context and process state. Never starts a scheduler.
    *******************************************************************************/
   private static class Fixture implements AutoCloseable
   {
      private final CapturedContext previousContext = QContext.capture();
      private final Map<String, Serializable> previousObjects = QContext.getObjects();
      private final ProcessIds processIds = new ProcessIds();
      private QInstance instance;
      private Connection anchor;



      /*******************************************************************************
       ** Extend only this sample instance and its private copy of the sample schema.
       *******************************************************************************/
      private Fixture() throws Exception
      {
         try
         {
            ConnectionManager.resetConnectionProviders();
            instance = SampleMetaDataProvider.defineTestInstance();
            RDBMSBackendMetaData backend = (RDBMSBackendMetaData) instance.getBackend("rdbms");
            backend.setDatabaseName("sample_automation_" + UUID.randomUUID());
            anchor = ConnectionManager.getConnection(backend);
            try(InputStreamReader reader = new InputStreamReader(Fixture.class.getResourceAsStream("/prime-test-database.sql"), StandardCharsets.UTF_8))
            {
               RunScript.execute(anchor, reader);
            }
            try(Statement statement = anchor.createStatement())
            {
               statement.execute("ALTER TABLE person ADD automation_status INT DEFAULT 7");
               statement.execute("ALTER TABLE person ADD automation_trace VARCHAR(200) DEFAULT ''");
               statement.execute("ALTER TABLE person ADD automation_attempts INT DEFAULT 0");
               statement.execute("ALTER TABLE person ADD automation_batch_size INT DEFAULT 0");
               statement.execute("ALTER TABLE person ADD automation_pet_count INT DEFAULT -1");
               statement.execute("ALTER TABLE person ADD automation_observed_status INT DEFAULT 0");
            }
            QTableMetaData person = instance.getTable("person");
            person.withField(new QFieldMetaData("automationStatus", QFieldType.INTEGER).withBackendName("automation_status").withDefaultValue(7));
            person.withField(new QFieldMetaData("automationTrace", QFieldType.STRING).withBackendName("automation_trace").withDefaultValue(""));
            person.withField(new QFieldMetaData("automationAttempts", QFieldType.INTEGER).withBackendName("automation_attempts").withDefaultValue(0));
            person.withField(new QFieldMetaData("automationBatchSize", QFieldType.INTEGER).withBackendName("automation_batch_size").withDefaultValue(0));
            person.withField(new QFieldMetaData("automationPetCount", QFieldType.INTEGER).withBackendName("automation_pet_count").withDefaultValue(-1));
            person.withSection(new QFieldSection().withName("automationEvidence").withLabel("Automation evidence").withTier(Tier.T3)
               .withFieldNames(List.of("automationStatus", "automationTrace", "automationAttempts", "automationBatchSize", "automationPetCount", "automationObservedStatus")));
            person.withField(new QFieldMetaData("automationObservedStatus", QFieldType.INTEGER).withBackendName("automation_observed_status").withDefaultValue(0));
            person.getField("createDate").withBehavior(DynamicDefaultValueBehavior.CREATE_DATE);
            person.getField("modifyDate").withBehavior(DynamicDefaultValueBehavior.MODIFY_DATE);
            EsbTableMetaData.of(person).setPublications(List.of());
            instance.addAutomationProvider(new PollingAutomationProviderMetaData().withName(PROVIDER));
            person.withAutomationDetails(new QTableAutomationDetails().withProviderName(PROVIDER).withOverrideBatchSize(2)
               .withStatusTracking(new AutomationStatusTracking().withType(AutomationStatusTrackingType.FIELD_IN_TABLE).withFieldName("automationStatus")));
            instance.addPossibleValueSource(TablesPossibleValueSourceMetaDataProvider.defineTablesPossibleValueSource(instance));
            instance.addProcess(new RunTableAutomationsProcessStep().produce(instance));
            instance.addProcess(new HealBadRecordAutomationStatusesProcessStep().produce(instance));
            instance.addProcess(new QProcessMetaData().withName(PROCESS).withTableName("person")
               .withStep(new QBackendStepMetaData().withName("trace").withCode(new QCodeReference(TraceProcess.class))));
            QContext.setObjects(null);
            QContext.setObject(OWNED_PROCESS_IDS, processIds);
            QContext.init(instance, new QSession());
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
       ** Vary the owned action list without changing any shared sample metadata.
       *******************************************************************************/
      private void actions(TableAutomationAction... actions) throws QException
      {
         instance.getTable("person").getAutomationDetails().setActions(List.of(actions));
         validate();
      }



      /*******************************************************************************
       ** Exercise the same enricher/validator used by application initialization.
       *******************************************************************************/
      private void validate() throws QInstanceValidationException
      {
         instance.setHasBeenValidated(null);
         new QInstanceValidator().validate(instance);
      }



      /*******************************************************************************
       ** Create a real sample person and fail on record-level validation errors.
       *******************************************************************************/
      private int insert(String lastName) throws Exception
      {
         QRecord record = InsertAction.executeForRecords(new InsertInput("person").withRecord(new QRecord()
            .withValue("firstName", "Automation").withValue("lastName", lastName).withValue("email", "automation@example.invalid"))).get(0);
         assertTrue(record.getErrors().isEmpty(), record.getErrors().toString());
         return (record.getValueInteger("id"));
      }



      /*******************************************************************************
       ** An ordinary user update, without suppressing automation tracking.
       *******************************************************************************/
      private void update(int id, String lastName) throws Exception
      {
         QRecord record = UpdateAction.executeForRecords(new UpdateInput("person")
            .withRecord(new QRecord().withValue("id", id).withValue("lastName", lastName))).get(0);
         assertTrue(record.getErrors().isEmpty(), record.getErrors().toString());
      }



      /*******************************************************************************
       ** Run the registered manual process synchronously through its public API.
       *******************************************************************************/
      private void poll() throws Exception
      {
         RunProcessOutput output = process(RunTableAutomationsProcessStep.NAME, Map.of("tableName", "person", "automationProviderName", PROVIDER));
         assertTrue(output.getException().isEmpty(), output.getException().toString());
         assertEquals("true", output.getValueString("ok"));
      }



      /*******************************************************************************
       ** Recover through the registered process, asserting its actual result count.
       *******************************************************************************/
      private void heal(int expectedCount) throws Exception
      {
         RunProcessOutput output = process(HealBadRecordAutomationStatusesProcessStep.NAME, Map.of("tableName", "person", "minutesOldLimit", 60));
         assertTrue(output.getException().isEmpty(), output.getException().toString());
         assertEquals(expectedCount, output.getValueInteger("totalRecordsUpdated"));
         assertEquals(0, output.getValueInteger("warningCount"));
      }



      /*******************************************************************************
       ** Keep every owned process UUID for teardown even if execution throws.
       *******************************************************************************/
      private RunProcessOutput process(String name, Map<String, Serializable> values) throws Exception
      {
         UUID processId = UUID.randomUUID();
         processIds.values.add(processId);
         RunProcessInput input = new RunProcessInput().withProcessName(name);
         input.setProcessUUID(processId.toString());
         input.setFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP);
         values.forEach(input::addValue);
         return (new RunProcessAction().execute(input));
      }



      /*******************************************************************************
       ** Native fixture corruption/repair bypasses automation intentionally.
       *******************************************************************************/
      private void sql(String sql) throws Exception
      {
         try(Statement statement = anchor.createStatement())
         {
            statement.executeUpdate(sql);
         }
      }



      /*******************************************************************************
       ** Native SQL observes physical state independently of QQQ query/status code.
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
       ** Always restore context and close the anchor, even when shutdown fails.
       *******************************************************************************/
      @Override
      public void close() throws Exception
      {
         try
         {
            for(UUID processId : processIds.values)
            {
               RunProcessAction.getStateProvider().remove(new UUIDAndTypeStateKey(processId, StateType.PROCESS_STATUS));
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
               QContext.init(previousContext);
               QContext.setObjects(previousObjects);
            }
         }
      }
   }
}
